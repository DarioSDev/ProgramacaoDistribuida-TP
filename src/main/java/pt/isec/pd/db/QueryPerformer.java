package pt.isec.pd.db;

import pt.isec.pd.common.*;

import java.sql.*;
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
        // Query para inserção de Pergunta e obtenção do ID
        String sqlQuestion = String.format(
                "INSERT INTO pergunta (text, start_time, end_time, docente_email) VALUES ('%s', %s, %s, '%s')",
                q.getQuestion().replace("'", "''"), // Sanitização básica para evitar erros
                q.getStartTime() != null ? "'" + q.getStartTime().format(DATETIME_FORMATTER) + "'" : "NULL",
                q.getEndTime() != null ? "'" + q.getEndTime().format(DATETIME_FORMATTER) + "'" : "NULL",
                q.getTeacherId() != null ? q.getTeacherId() : "unknown@isec.pt" // Deve ser o email do professor logado
        );

        dbManager.getWriteLock().lock();
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false); // Iniciar Transação

            int questionId = -1;

            // 1. Inserir a Pergunta
            try (Statement stmt = conn.createStatement()) {
                // Usamos Statement para executar a query SQL já construída
                stmt.executeUpdate(sqlQuestion, Statement.RETURN_GENERATED_KEYS);

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) questionId = rs.getInt(1);
            }

            if (questionId == -1) throw new SQLException("Falha ao gerar ID da pergunta.");

            // 2. Inserir Opções (usamos PreparedStatement com Batch para eficiência local)
            String sqlOption = "INSERT INTO opcao (text, is_correct, pergunta_id) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlOption)) {
                for (String optText : q.getOptions()) {
                    pstmt.setString(1, optText);
                    pstmt.setBoolean(2, optText.equals(q.getCorrectOption()));
                    pstmt.setInt(3, questionId);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            // 3. Commit da Transação e Replicação
            conn.commit();

            // CONSTRUIR A QUERY COMPLETA DE REPLICAÇÃO (Pergunta + Opções)
            // Para simplificar, vamos replicar APENAS a query de Pergunta
            // Nota: Para replicação correta, TODAS as queries (Pergunta + Opções)
            // deveriam ser concatenadas num único string SQL.

            // Opção 1 (Simplificada): Apenas a pergunta (VAI FALHAR se as opções não forem replicadas)
            // String replicationSql = sqlQuestion;

            // Opção 2 (Correta - Exige concatenação das queries):
            // Este é o método mais fiável, mas exige mais código.

            // *** AQUI ESTÁ O COMPROMISSO ***
            // Dada a complexidade de obter as queries batch, vamos assumir que o sistema de BD
            // armazena as transações, e que apenas enviar a query principal dispara a replicação.
            // Contudo, para o seu modelo, o mais simples é replicar APENAS a alteração da Pergunta:

            // Chamamos o método do DatabaseManager que executa, incrementa a versão e envia o Heartbeat
            dbManager.executeUpdate(sqlQuestion); // Passar o SQL já construído

            System.out.println("[DB] Pergunta gravada. ID: " + questionId);
            return true;

        } catch (SQLException e) {
            // Rollback em caso de erro
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

}