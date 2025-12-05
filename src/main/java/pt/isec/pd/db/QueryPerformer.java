package pt.isec.pd.db;

import pt.isec.pd.client.ClientAPI;
import pt.isec.pd.common.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.time.format.DateTimeFormatter;

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
            values = String.format("'%s', '%s', '%s', '%s'",
                    user.getEmail(), user.getName(), user.getPassword(), s.getIdNumber());
        } else if (user instanceof Teacher) {
            if (!validateTeacherCode(user.getExtra())) {
                System.err.println("[DB] Registo docente: Código de validação inválido.");
                return false;
            }
            tableName = "docente";
            values = String.format("'%s', '%s', '%s', '%s'",
                    user.getEmail(), user.getName(), user.getPassword(), user.getExtra());
        } else {
            return false;
        }

        // A query final deve ser replicável
        finalSql = String.format("INSERT INTO %s VALUES (%s)", tableName, values);

        // [CORREÇÃO] Usa o método que executa, incrementa e notifica o cluster
        if (dbManager.executeUpdateByClient(finalSql)) {
            System.out.println("[DB] Utilizador registado (via update): " + user.getEmail());
            return true;
        }
        return false;
    }

    public boolean validateTeacherCode(String teacherCode) {
        String sql = "SELECT teacher_hash FROM config WHERE id = 1";

        dbManager.getReadLock().lock();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) return false;

            String storedHash = rs.getString("teacher_hash");
            if (storedHash == null || storedHash.isEmpty()) return false;

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
        // ... (lógica inalterada de leitura) ...
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
        return null;
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
                q.getId(),
                q.getQuestion().replace("'", "''"),
                q.getStartTime() != null ? "'" + q.getStartTime().format(DATETIME_FORMATTER) + "'" : "NULL",
                q.getEndTime() != null ? "'" + q.getEndTime().format(DATETIME_FORMATTER) + "'" : "NULL",
                q.getTeacherId() != null ? q.getTeacherId() : "unknown@isec.pt"
        );
        String finalReplicaSql = "";

        dbManager.getWriteLock().lock();
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false); // Inicia a transação

            int questionId = -1;

            // 1. Inserção da Pergunta
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sqlQuestion, Statement.RETURN_GENERATED_KEYS);

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) questionId = rs.getInt(1);
            }

            if (questionId == -1) throw new SQLException("Falha ao gerar ID da pergunta.");

            // 2. Inserção das Opções (usando transação e batch)
            String sqlOption = "INSERT INTO opcao (text, is_correct, pergunta_id) VALUES (?, ?, ?)";
            StringBuilder replicaBuilder = new StringBuilder();

            // Inicia a string replicável com a query da pergunta
            replicaBuilder.append(sqlQuestion).append(";");

            try (PreparedStatement pstmt = conn.prepareStatement(sqlOption)) {
                for (String optText : q.getOptions()) {
                    pstmt.setString(1, optText);
                    pstmt.setBoolean(2, optText.equals(q.getCorrectOption()));
                    pstmt.setLong(3, questionId);
                    pstmt.addBatch();

                    // Adiciona a query da opção à string replicável
                    // Usa subconsulta (SELECT MAX(id) FROM pergunta) para o secundário encontrar o ID gerado
                    String optReplica = String.format(
                            "INSERT INTO opcao (text, is_correct, pergunta_id) VALUES ('%s', %s, (SELECT MAX(id) FROM pergunta WHERE code='%s'))",
                            optText.replace("'", "''"),
                            optText.equals(q.getCorrectOption()) ? "1" : "0",
                            q.getId()
                    );
                    replicaBuilder.append(optReplica).append(";");
                }
                pstmt.executeBatch();
            }

            // 3. Commit da Transação
            conn.commit();
            finalReplicaSql = replicaBuilder.toString();

            // 4. [CORREÇÃO] Notifica o Heartbeat Manager
            dbManager.notifyUpdateAfterTransaction(finalReplicaSql);

            System.out.println("[DB] Pergunta gravada e notificada. ID: " + questionId);
            return true;

        } catch (SQLException e) {
            // Lógica de Rollback
            try (Connection conn = dbManager.getConnection()) {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackE) {
                System.err.println("[DB] Erro ao fazer rollback: " + rollbackE.getMessage());
            }
            System.err.println("[DB] Erro ao gravar pergunta: " + e.getMessage());
            return false;
        } finally {
            dbManager.getWriteLock().unlock();
        }
    }

    public boolean submitAnswer(String studentEmail, String code, int optionIndex) {
        String sqlGetId = "SELECT id FROM pergunta WHERE code = ?";
        String sqlGetOptions = "SELECT id, is_correct FROM opcao WHERE pergunta_id = ? ORDER BY id ASC";
        String replicaSql = null;
        boolean isCorrect = false;

        dbManager.getWriteLock().lock();
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false); // Inicia a transação

            // ... (lógica para obter questionId e optionId) ...
            long questionId = -1;
            long optionId = -1;

            // Obter ID da Pergunta
            try (PreparedStatement pstmt = conn.prepareStatement(sqlGetId)) {
                pstmt.setString(1, code);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) questionId = rs.getLong("id");
            }
            if (questionId == -1) { conn.rollback(); return false; }

            // Obter ID da Opção e Verificar se é Correta
            try (PreparedStatement pstmt = conn.prepareStatement(sqlGetOptions)) {
                pstmt.setLong(1, questionId);
                ResultSet rs = pstmt.executeQuery();
                int currentIdx = 0;
                while (rs.next()) {
                    if (currentIdx == optionIndex) {
                        optionId = rs.getLong("id");
                        isCorrect = rs.getBoolean("is_correct");
                        break;
                    }
                    currentIdx++;
                }
            }
            if (optionId == -1) { conn.rollback(); return false; }


            // 1. Execução da Inserção (dentro da transação)
            String insertSql = "INSERT OR IGNORE INTO resposta (estudante_email, pergunta_id, opcao_id) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, studentEmail);
                pstmt.setLong(2, questionId);
                pstmt.setLong(3, optionId);
                pstmt.executeUpdate();
            }

            // 2. Commit da transação
            conn.commit();

            // 3. Constrói a Query Replicável (Após o commit)
            replicaSql = String.format("INSERT OR IGNORE INTO resposta (estudante_email, pergunta_id, opcao_id) VALUES ('%s', %d, %d)",
                    studentEmail, questionId, optionId);

            // 4. [CORREÇÃO] Notifica o Heartbeat Manager
            dbManager.notifyUpdateAfterTransaction(replicaSql);

            System.out.println("[DB] Resposta submetida e notificada de " + studentEmail);
            return isCorrect;

        } catch (SQLException e) {
            // Lógica de Rollback
            try (Connection conn = dbManager.getConnection()) {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackE) {
                System.err.println("[DB] Erro ao fazer rollback: " + rollbackE.getMessage());
            }
            System.err.println("[DB] Erro submitAnswer: " + e.getMessage());
            return false;
        } finally {
            dbManager.getWriteLock().unlock();
        }
    }

    public boolean editQuestion(Question q) {
        String startStr = q.getStartTime() != null ? q.getStartTime().format(DATETIME_FORMATTER) : null;
        String endStr = q.getEndTime() != null ? q.getEndTime().format(DATETIME_FORMATTER) : null;

        String sqlUpdateReplica = String.format(
                "UPDATE pergunta SET text='%s', start_time='%s', end_time='%s' WHERE code='%s'",
                q.getQuestion().replace("'", "''"),
                startStr, endStr, q.getId()
        );

        Connection conn = null;
        dbManager.getWriteLock().lock();

        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            long internalId = -1;
            try(PreparedStatement ps = conn.prepareStatement("SELECT id FROM pergunta WHERE code = ?")) {
                ps.setString(1, q.getId());
                ResultSet rs = ps.executeQuery();
                if(rs.next()) internalId = rs.getLong("id");
            }
            if (internalId == -1) { conn.rollback(); return false; }

            String sqlUpdateLocal = "UPDATE pergunta SET text=?, start_time=?, end_time=? WHERE id=?";
            try(PreparedStatement ps = conn.prepareStatement(sqlUpdateLocal)) {
                ps.setString(1, q.getQuestion());
                ps.setString(2, startStr);
                ps.setString(3, endStr);
                ps.setLong(4, internalId);
                ps.executeUpdate();
            }

            try(PreparedStatement ps = conn.prepareStatement("DELETE FROM opcao WHERE pergunta_id = ?")) {
                ps.setLong(1, internalId);
                ps.executeUpdate();
            }

            String sqlInsertOpt = "INSERT INTO opcao (text, is_correct, pergunta_id) VALUES (?, ?, ?)";
            try(PreparedStatement ps = conn.prepareStatement(sqlInsertOpt)) {
                for (String optText : q.getOptions()) {
                    ps.setString(1, optText);
                    ps.setBoolean(2, optText.equals(q.getCorrectOption()));
                    ps.setLong(3, internalId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();

            dbManager.notifyUpdateAfterTransaction(sqlUpdateReplica);

            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ex) {}
            }
            dbManager.getWriteLock().unlock();
        }
    }

    // Histórico do Aluno (Leitura)
    public List<ClientAPI.HistoryItem> getStudentHistory(String email) {
        // ... (código inalterado) ...
        List<ClientAPI.HistoryItem> history = new ArrayList<>();
        String sql = """
            SELECT p.text, p.end_time, o.is_correct
            FROM resposta r
            JOIN pergunta p ON r.pergunta_id = p.id
            JOIN opcao o ON r.opcao_id = o.id
            WHERE r.estudante_email = ?
            ORDER BY p.end_time DESC
        """;

        dbManager.getReadLock().lock();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String qText = rs.getString("text");
                String dateStr = rs.getString("end_time");
                boolean correct = rs.getBoolean("is_correct");

                LocalDateTime dateTime = null;
                if (dateStr != null) {
                    dateTime = LocalDateTime.parse(dateStr, DATETIME_FORMATTER);
                }

                history.add(new ClientAPI.HistoryItem(qText, dateTime != null ? dateTime.toLocalDate() : null, correct));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erro history: " + e.getMessage());
        } finally {
            dbManager.getReadLock().unlock();
        }
        return history;
    }

    public TeacherResultsData getQuestionResults(String questionCode) {
        // ... (código inalterado) ...
        Question q = getQuestionByCode(questionCode);
        if (q == null) return null;

        List<StudentAnswerInfo> studentAnswers = new ArrayList<>();
        String sql = """
            SELECT e.name, e.email, o.id as opcao_id
            FROM resposta r
            JOIN estudante e ON r.estudante_email = e.email
            JOIN pergunta p ON r.pergunta_id = p.id
            JOIN opcao o ON r.opcao_id = o.id
            WHERE p.code = ?
        """;

        List<Long> orderedOptionIds = new ArrayList<>();
        String sqlOpts = "SELECT id FROM opcao WHERE pergunta_id = (SELECT id FROM pergunta WHERE code = ?) ORDER BY id ASC";

        dbManager.getReadLock().lock();
        try (Connection conn = dbManager.getConnection()) {

            // Mapeamento IDs
            try(PreparedStatement ps = conn.prepareStatement(sqlOpts)) {
                ps.setString(1, questionCode);
                ResultSet rs = ps.executeQuery();
                while(rs.next()) orderedOptionIds.add(rs.getLong("id"));
            }

            // Buscar Respostas
            try(PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, questionCode);
                ResultSet rs = ps.executeQuery();
                while(rs.next()) {
                    String name = rs.getString("name");
                    String email = rs.getString("email");
                    long optId = rs.getLong("opcao_id");

                    // Calcular letra
                    int idx = orderedOptionIds.indexOf(optId);
                    String letter = (idx != -1) ? String.valueOf((char)('a' + idx)) : "?";
                    boolean isCorrect = letter.equalsIgnoreCase(q.getCorrectOption());

                    studentAnswers.add(new StudentAnswerInfo(name, email, letter, isCorrect));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            dbManager.getReadLock().unlock();
        }

        String dateStr = "N/A";
        if (q.getStartTime() != null) {
            dateStr = q.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }

        return new TeacherResultsData(
                q.getQuestion(),
                List.of(q.getOptions()),
                q.getCorrectOption(),
                dateStr,
                studentAnswers.size(),
                studentAnswers
        );
    }

    // VALIDAR CÓDIGO DA PERGUNTA (Leitura)
    public String validateQuestionCode(String code) {
        // ... (código inalterado) ...
        String sql = "SELECT start_time, end_time FROM pergunta WHERE code = ?";

        dbManager.getReadLock().lock();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, code);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) return "INVALID";

            String startStr = rs.getString("start_time");
            String endStr = rs.getString("end_time");

            if (startStr == null || endStr == null) return "VALID";

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

    // OBTER PERGUNTA PELO CÓDIGO (Leitura)
    public Question getQuestionByCode(String code) {
        // ... (código inalterado) ...
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

            if (questionId == -1) return null;

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
                        correctOption = String.valueOf((char) ('a' + index));
                    }
                    index++;
                }
            }

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

    public List<Question> getTeacherQuestions(String teacherEmail) {
        // ... (código inalterado) ...
        List<Question> list = new ArrayList<>();

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
                String code = rs.getString("code");
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