package pt.isec.pd.db;

import pt.isec.pd.common.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.time.format.DateTimeFormatter; // Novo import necessário para formatação da data/hora

public class QueryPerformer {
    private final DatabaseManager dbManager;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public QueryPerformer(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public boolean registerUser(User user) {
        String tableName;
        String values;
        String finalSql;

        if (user instanceof Student s) {
            tableName = "estudante";
            // Nota: SQL construído diretamente, assumindo que user.getPassword() é o password final.
            values = String.format("'%s', '%s', '%s', '%s'",
                    user.getEmail(), user.getName(), user.getPassword(), s.getIdNumber());
        } else if (user instanceof Teacher t) {
            if (!validateTeacherCode(user.getExtra())) {
                System.err.println("[DB] Registo docente: Código de validação inválido.");
                return false;
            }
            tableName = "docente";
            // Usa o user.getExtra() que foi validado como valor para a coluna 'extra'.
            values = String.format("'%s', '%s', '%s', '%s'",
                    user.getEmail(), user.getName(), user.getPassword(), user.getExtra());
        } else {
            return false;
        }

        // A query final deve ser replicável diretamente
        finalSql = String.format("INSERT INTO %s VALUES (%s)", tableName, values);

        dbManager.getWriteLock().lock();
        try {
            // Chamamos o método do DatabaseManager que executa, incrementa a versão e envia o Heartbeat
            if (dbManager.executeUpdate(finalSql)) {
                System.out.println("[DB] Utilizador registado (via update): " + user.getEmail());
                return true;
            }
            return false;
        } finally {
            dbManager.getWriteLock().unlock();
        }
    }

    public boolean validateTeacherCode(String teacherCode) {
        String sql = "SELECT teacher_hash FROM config WHERE id = 1";

        dbManager.getReadLock().lock();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                System.err.println("[DB] ERRO: config.id=1 não encontrado.");
                return false;
            }

            String storedHash = rs.getString("teacher_hash");
            if (storedHash == null || storedHash.isEmpty()) {
                System.err.println("[DB] ERRO: teacher_hash não está definido na BD.");
                return false;
            }

            // Gerar hash do código introduzido
            // Nota: hashCode deve ser refatorado para ser estático e não depender da instância dbManager.
            String inputHash = DatabaseManager.hashCode(teacherCode);

            return inputHash.equals(storedHash);

        } catch (SQLException e) {
            System.err.println("[DB] Erro ao validar teacherCode: " + e.getMessage());
            return false;
        } finally {
            dbManager.getReadLock().unlock();
        }
    }

    public User getUser(String email) {
        // Tenta procurar nas duas tabelas
        String sqlStudent = "SELECT email, name, password, student_number FROM estudante WHERE email = ?";
        String sqlTeacher = "SELECT email, name, password, extra FROM docente WHERE email = ?";

        dbManager.getReadLock().lock();
        try (Connection conn = dbManager.getConnection()) {

            // 1. Verifica Estudante
            try (PreparedStatement pstmt = conn.prepareStatement(sqlStudent)) {
                pstmt.setString(1, email);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return new Student(
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("student_number")
                    );
                }
            }

            // 2. Verifica Docente
            try (PreparedStatement pstmt = conn.prepareStatement(sqlTeacher)) {
                pstmt.setString(1, email);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    // Mapeia o campo 'extra' para o ID/código do Teacher, dependendo da sua classe Teacher
                    return new Teacher(
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("extra")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("[DB] Erro ao obter user: " + e.getMessage());
        } finally {
            dbManager.getReadLock().unlock();
        }
        return null; // Não encontrado
    }

    public RoleType authenticate(String email, String password) {
        User u = getUser(email);
        if (u != null && u.getPassword().equals(password)) {
            return RoleType.fromClass(u);
        }
        return null;
    }

    // --- GESTÃO DE PERGUNTAS ---

    public boolean saveQuestion(Question q) {
        String sqlQuestion = String.format(
                "INSERT INTO pergunta (code, text, start_time, end_time, docente_email) VALUES ('%s', '%s', %s, %s, '%s')",
                q.getId(), // <--- O código de 6 dígitos
                q.getQuestion().replace("'", "''"),
                q.getStartTime() != null ? "'" + q.getStartTime().format(DATETIME_FORMATTER) + "'" : "NULL",
                q.getEndTime() != null ? "'" + q.getEndTime().format(DATETIME_FORMATTER) + "'" : "NULL",
                q.getTeacherId() != null ? q.getTeacherId() : "unknown@isec.pt"
        );

        dbManager.getWriteLock().lock();
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            int questionId = -1;

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sqlQuestion, Statement.RETURN_GENERATED_KEYS);

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) questionId = rs.getInt(1);
            }

            if (questionId == -1) throw new SQLException("Falha ao gerar ID da pergunta.");

            String sqlOption = "INSERT INTO opcao (text, is_correct, pergunta_id) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlOption)) {
                for (String optText : q.getOptions()) {
                    pstmt.setString(1, optText);
                    pstmt.setBoolean(2, optText.equals(q.getCorrectOption()));
                    pstmt.setLong(3, questionId);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            conn.commit();
            dbManager.incrementDbVersion(sqlQuestion);
            System.out.println("[DB] Pergunta gravada. ID: " + questionId);
            return true;

        } catch (SQLException e) {
            try (Connection conn = dbManager.getConnection()) {
                conn.rollback();
            } catch (SQLException rollbackE) {
                System.err.println("[DB] Erro ao fazer rollback: " + rollbackE.getMessage());
            }
            System.err.println("[DB] Erro ao gravar pergunta: " + e.getMessage());
            return false;
        } finally {
            dbManager.getWriteLock().unlock();
        }
    }

    public boolean submitAnswer(String studentEmail, int questionId, int optionIndex) {

        String sqlGetOptions = "SELECT id FROM opcao WHERE pergunta_id = ? ORDER BY id ASC";

        dbManager.getWriteLock().lock();
        try (Connection conn = dbManager.getConnection()) {

            int realOptionId = -1;

            // 1. Obter o ID real da Opção
            try (PreparedStatement pstmt = conn.prepareStatement(sqlGetOptions)) {
                pstmt.setInt(1, questionId);
                ResultSet rs = pstmt.executeQuery();

                int currentIndex = 0;
                while (rs.next()) {
                    if (currentIndex == optionIndex) {
                        realOptionId = rs.getInt("id");
                        break;
                    }
                    currentIndex++;
                }
            }

            if (realOptionId == -1) return false;

            // 2. Construir a query SQL final (replicação)
            String sqlInsertAnswer = String.format(
                    "INSERT INTO resposta (estudante_email, pergunta_id, opcao_id) VALUES ('%s', %d, %d)",
                    studentEmail, questionId, realOptionId);

            // Chamamos o método do DatabaseManager que executa, incrementa a versão e envia o Heartbeat
            if (dbManager.executeUpdate(sqlInsertAnswer)) {
                System.out.println("[DB] Resposta submetida (via update): " + studentEmail);
                return true;
            }

            return false;

        } catch (SQLException e) {
            System.err.println("[DB] Erro submitAnswer: " + e.getMessage());
            return false;
        } finally {
            dbManager.getWriteLock().unlock();
        }
    }

    // VALIDAR CÓDIGO DA PERGUNTA
    public String validateQuestionCode(String code) {
        String sql = "SELECT start_time, end_time FROM pergunta WHERE code = ?";

        dbManager.getReadLock().lock();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, code);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                return "INVALID"; // Código não existe
            }

            String startStr = rs.getString("start_time");
            String endStr = rs.getString("end_time");

            if (startStr == null || endStr == null) return "VALID"; // Assumir válido se sem datas (ou tratar como erro)

            LocalDateTime start = LocalDateTime.parse(startStr, DATETIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endStr, DATETIME_FORMATTER);
            LocalDateTime now = LocalDateTime.now();

            if (now.isBefore(start)) return "NOT_STARTED";
            if (now.isAfter(end)) return "EXPIRED";

            return "VALID";

        } catch (SQLException e) {
            System.err.println("[DB] Erro ao validar código: " + e.getMessage());
            return "ERROR";
        } finally {
            dbManager.getReadLock().unlock();
        }
    }

    // OBTER PERGUNTA PELO CÓDIGO (Para o Aluno ver)
    public Question getQuestionByCode(String code) {
        String sqlQuestion = "SELECT id, text, start_time, end_time, docente_email FROM pergunta WHERE code = ?";
        String sqlOptions = "SELECT text, is_correct FROM opcao WHERE pergunta_id = ? ORDER BY id ASC";

        dbManager.getReadLock().lock();
        try (Connection conn = dbManager.getConnection()) {

            // Passo A: Buscar dados da Pergunta
            long questionId = -1;
            String text = null;
            LocalDateTime start = null, end = null;
            String teacherEmail = null;

            try (PreparedStatement pstmt = conn.prepareStatement(sqlQuestion)) {
                pstmt.setString(1, code);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    questionId = rs.getLong("id");
                    text = rs.getString("text");
                    teacherEmail = rs.getString("docente_email");

                    String sTime = rs.getString("start_time");
                    String eTime = rs.getString("end_time");
                    if (sTime != null) start = LocalDateTime.parse(sTime, DATETIME_FORMATTER);
                    if (eTime != null) end = LocalDateTime.parse(eTime, DATETIME_FORMATTER);
                }
            }

            if (questionId == -1) return null; // Não encontrou

            // Passo B: Buscar Opções
            List<String> optionsList = new ArrayList<>();
            String correctOption = null;
            int index = 0;

            try (PreparedStatement pstmt = conn.prepareStatement(sqlOptions)) {
                pstmt.setLong(1, questionId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    optionsList.add(rs.getString("text"));
                    boolean isCorrect = rs.getBoolean("is_correct");
                    if (isCorrect) {
                        // Converte índice 0->a, 1->b, etc.
                        correctOption = String.valueOf((char) ('a' + index));
                    }
                    index++;
                }
            }

            // Construir o objeto (Nota: usamos um array vazio se null)
            return new Question(
                    text,
                    correctOption,
                    optionsList.toArray(new String[0]),
                    start,
                    end,
                    teacherEmail
            );

        } catch (SQLException e) {
            System.err.println("[DB] Erro getQuestionByCode: " + e.getMessage());
            return null;
        } finally {
            dbManager.getReadLock().unlock();
        }
    }

    // SUBMETER RESPOSTA
    public boolean submitAnswer(String studentEmail, String code, int optionIndex) {
        String sqlGetId = "SELECT id FROM pergunta WHERE code = ?";
        String sqlGetOptions = "SELECT id FROM opcao WHERE pergunta_id = ? ORDER BY id ASC";
        // A query de inserção completa para replicação
        String sqlInsertReplica;

        dbManager.getWriteLock().lock();
        try (Connection conn = dbManager.getConnection()) {

            // A. Descobrir ID da Pergunta pelo Code
            long questionId = -1;
            try(PreparedStatement pstmt = conn.prepareStatement(sqlGetId)){
                pstmt.setString(1, code);
                ResultSet rs = pstmt.executeQuery();
                if(rs.next()) questionId = rs.getLong("id");
            }
            if (questionId == -1) return false;

            // B. Descobrir ID da Opção pelo Índice (0, 1, 2...)
            long optionId = -1;
            try (PreparedStatement pstmt = conn.prepareStatement(sqlGetOptions)) {
                pstmt.setLong(1, questionId);
                ResultSet rs = pstmt.executeQuery();
                int currentIdx = 0;
                while (rs.next()) {
                    if (currentIdx == optionIndex) {
                        optionId = rs.getLong("id");
                        break;
                    }
                    currentIdx++;
                }
            }
            if (optionId == -1) return false;

            // C. Verificar se já respondeu (opcional, mas boa prática)
            // ... (podes adicionar um SELECT aqui para evitar duplicados)

            // D. Inserir
            String insertSql = "INSERT INTO resposta (estudante_email, pergunta_id, opcao_id) VALUES (?, ?, ?)";
            try(PreparedStatement pstmt = conn.prepareStatement(insertSql)){
                pstmt.setString(1, studentEmail);
                pstmt.setLong(2, questionId);
                pstmt.setLong(3, optionId);
                pstmt.executeUpdate();
            }

            // E. Replicar
            sqlInsertReplica = String.format("INSERT INTO resposta (estudante_email, pergunta_id, opcao_id) VALUES ('%s', %d, %d)",
                    studentEmail, questionId, optionId);
            dbManager.incrementDbVersion(sqlInsertReplica);

            System.out.println("[DB] Resposta recebida: " + studentEmail + " -> Questao " + code);
            return true;

        } catch (SQLException e) {
            System.err.println("[DB] Erro submitAnswer: " + e.getMessage());
            return false;
        } finally {
            dbManager.getWriteLock().unlock();
        }
    }

    public List<Question> getTeacherQuestions(String teacherEmail) {
        List<Question> list = new ArrayList<>();

        // Query: Seleciona a pergunta e conta as respostas associadas
        String sql = """
            SELECT p.id, p.code, p.text, p.start_time, p.end_time, p.docente_email, 
                   (SELECT COUNT(*) FROM resposta r WHERE r.pergunta_id = p.id) as total_respostas
            FROM pergunta p 
            WHERE p.docente_email = ? 
            ORDER BY p.id DESC
        """;

        String sqlOptions = "SELECT text, is_correct FROM opcao WHERE pergunta_id = ? ORDER BY id ASC";

        dbManager.getReadLock().lock();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, teacherEmail);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                long idDb = rs.getLong("id");
                String code = rs.getString("code"); // O ID visível (6 dígitos)
                String text = rs.getString("text");
                String sTime = rs.getString("start_time");
                String eTime = rs.getString("end_time");
                int totalAnswers = rs.getInt("total_respostas");

                LocalDateTime start = (sTime != null) ? LocalDateTime.parse(sTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
                LocalDateTime end = (eTime != null) ? LocalDateTime.parse(eTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;

                // Recuperar opções
                List<String> opts = new ArrayList<>();
                String correct = "";
                try (PreparedStatement psOpt = conn.prepareStatement(sqlOptions)) {
                    psOpt.setLong(1, idDb);
                    ResultSet rsOpt = psOpt.executeQuery();
                    int idx = 0;
                    while(rsOpt.next()) {
                        opts.add(rsOpt.getString("text"));
                        if(rsOpt.getBoolean("is_correct")) correct = String.valueOf((char)('a' + idx));
                        idx++;
                    }
                }

                Question q = new Question(text, correct, opts.toArray(new String[0]), start, end, teacherEmail);
                q.setId(code);
                q.setTotalAnswers(totalAnswers);

                list.add(q);
            }

        } catch (SQLException e) {
            System.err.println("[DB] Erro getTeacherQuestions: " + e.getMessage());
        } finally {
            dbManager.getReadLock().unlock();
        }

        return list;
    }

}