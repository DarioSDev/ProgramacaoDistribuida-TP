package pt.isec.pd.db;

import pt.isec.pd.common.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class QueryPerformer {
    private final DatabaseManager dbManager;

    public QueryPerformer(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public boolean registerUser(User user) {
        String sql;
        if (user instanceof Student) {
            sql = "INSERT INTO estudante (email, name, password, student_number) VALUES (?, ?, ?, ?)";
        } else if (user instanceof Teacher) {
            if (validateTeacherCode(user.getExtra())) {
                sql = "INSERT INTO docente (email, name, password, extra) VALUES (?, ?, ?, ?)";
            } else {
                return false;
            }
        } else {
            return false;
        }

        dbManager.getWriteLock().lock();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getName());
            pstmt.setString(3, user.getPassword()); // Nota: Num caso real, usar hash (BCrypt/Argon2)

            if (user instanceof Student s) {
                pstmt.setString(4, s.getIdNumber());
            } else if (user instanceof Teacher t) {
                pstmt.setString(4, t.getTeacherId());
            }

            pstmt.executeUpdate();
            dbManager.incrementDbVersion(); // ⚠️ Importante: Atualiza versão após escrita

            System.out.println("[DB] Utilizador registado: " + user.getEmail());
            return true;

        } catch (SQLException e) {
            System.err.println("[DB] Erro no registo: " + e.getMessage());
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
            String inputHash = dbManager.hashCode(teacherCode);

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
        String sqlStudent = "SELECT * FROM estudante WHERE email = ?";
        String sqlTeacher = "SELECT * FROM docente WHERE email = ?";

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
                    // Nota: O construtor do Teacher gera novo ID, aqui terias de ajustar o construtor
                    // ou usar setters para repor o ID original da BD.
                    Teacher t = new Teacher(
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("extra")
                    );
                    // t.setTeacherId(rs.getString("id_uuid")); // Se implementares este setter
                    return t;
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
        String sqlQuestion = "INSERT INTO pergunta (text, docente_email) VALUES (?, ?)";
        String sqlOption = "INSERT INTO opcao (text, is_correct, pergunta_id) VALUES (?, ?, ?)";

        dbManager.getWriteLock().lock();
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false); // Transação Atómica

            int questionId = -1;

            // 1. Inserir Pergunta
            try (PreparedStatement pstmt = conn.prepareStatement(sqlQuestion, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, q.getQuestion());
                pstmt.setString(2, "TODO_DOCENTE_EMAIL"); // Precisas de passar quem criou a pergunta
                pstmt.executeUpdate();

                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) questionId = rs.getInt(1);
            }

            if (questionId == -1) throw new SQLException("Falha ao gerar ID da pergunta.");

            // 2. Inserir Opções
            try (PreparedStatement pstmt = conn.prepareStatement(sqlOption)) {
                for (String optText : q.getOptions()) {
                    pstmt.setString(1, optText);
                    pstmt.setBoolean(2, optText.equals(q.getCorrectOption()));
                    pstmt.setInt(3, questionId);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            conn.commit();
            dbManager.incrementDbVersion();
            System.out.println("[DB] Pergunta gravada. ID: " + questionId);
            return true;

        } catch (SQLException e) {
            System.err.println("[DB] Erro ao gravar pergunta: " + e.getMessage());
            return false;
        } finally {
            dbManager.getWriteLock().unlock();
        }
    }
}