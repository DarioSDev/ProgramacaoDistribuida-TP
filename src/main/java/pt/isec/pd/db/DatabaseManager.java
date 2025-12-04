package pt.isec.pd.db;

import pt.isec.pd.server.HeartbeatManager;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.sql.*;
import java.util.Base64;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DatabaseManager {
    private String dbPath;
    private final ReentrantReadWriteLock dbLock;
    private final Lock readLock;
    private final Lock writeLock;
    private volatile boolean schemaReady = false;
    private HeartbeatManager heartbeatManager;

    public DatabaseManager(String dbDirectory, String dbName) {
        this.dbPath = new File(dbDirectory, dbName).getAbsolutePath();
        this.dbLock = new ReentrantReadWriteLock();
        this.readLock = dbLock.readLock();
        this.writeLock = dbLock.writeLock();
    }

    public void setHeartbeatManager(HeartbeatManager heartbeatManager) {
        this.heartbeatManager = heartbeatManager;
    }

    public void setDbFile(File dbFile) {
        this.dbPath = dbFile.getAbsolutePath();
    }

    public File getDbFile() {
        return new File(dbPath);
    }

    public Lock getReadLock() { return readLock; }
    public Lock getWriteLock() { return writeLock; }

    public boolean isSchemaReady() {
        return schemaReady;
    }

    Connection getConnection() throws SQLException {
        // Garante que o driver SQLite é carregado
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver SQLite não encontrado: " + e.getMessage());
        }
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }


    public void createSchema() {
        writeLock.lock();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Criação das tabelas (mantida a estrutura original)
            stmt.execute("CREATE TABLE IF NOT EXISTS config (" +
                    "id INTEGER PRIMARY KEY CHECK (id = 1), " +
                    "version INTEGER NOT NULL DEFAULT 0, " +
                    "teacher_hash TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS docente (" +
                    "email TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "password TEXT NOT NULL, " +
                    "extra TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS estudante (" +
                    "email TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "password TEXT NOT NULL, " +
                    "student_number TEXT UNIQUE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS pergunta (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "code TEXT UNIQUE, " +
                    "text TEXT NOT NULL, " +
                    "start_time TEXT, " +
                    "end_time TEXT, " +
                    "docente_email TEXT, " +
                    "FOREIGN KEY(docente_email) REFERENCES docente(email))");

            stmt.execute("CREATE TABLE IF NOT EXISTS opcao (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "text TEXT NOT NULL, " +
                    "is_correct BOOLEAN NOT NULL DEFAULT 0, " +
                    "pergunta_id INTEGER, " +
                    "FOREIGN KEY(pergunta_id) REFERENCES pergunta(id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS resposta (" +
                    "estudante_email TEXT, " +
                    "pergunta_id INTEGER, " +
                    "opcao_id INTEGER, " +
                    "PRIMARY KEY (estudante_email, pergunta_id), " +
                    "FOREIGN KEY(estudante_email) REFERENCES estudante(email), " +
                    "FOREIGN KEY(pergunta_id) REFERENCES pergunta(id), " +
                    "FOREIGN KEY(opcao_id) REFERENCES opcao(id))");

            // Inicialização da versão e teacher_hash
            stmt.execute("INSERT OR IGNORE INTO config (id, version, teacher_hash) VALUES (1, 0, NULL)");

            PreparedStatement ps = conn.prepareStatement("SELECT teacher_hash FROM config WHERE id = 1");
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String current = rs.getString("teacher_hash");
                if (current == null || current.isEmpty()) {
                    String teacherCode = "p4Ssw0!3d"; // Código Hardcoded para o primeiro arranque
                    String hashed = hashCode(teacherCode);

                    PreparedStatement upd = conn.prepareStatement(
                            "UPDATE config SET teacher_hash = ? WHERE id = 1"
                    );
                    upd.setString(1, hashed);
                    upd.executeUpdate();
                }
            }

            schemaReady = true;
            System.out.println("[DatabaseManager] Schema verificado e inicializado em: " + dbPath);

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Erro ao criar schema: " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }

    public int getDbVersion() {
        readLock.lock();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version FROM config WHERE id = 1")) {

            if (rs.next()) return rs.getInt("version");
            return 0;

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Erro ao ler versão: " + e.getMessage());
            return -1;
        } finally {
            readLock.unlock();
        }
    }

    // Método auxiliar para incrementar a versão *apenas* localmente (usado por executeUpdateBySync)
    private void incrementDbVersionLocalOnly() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("UPDATE config SET version = version + 1 WHERE id = 1");
            // System.out.println("[DatabaseManager] Versão incrementada (LOCAL).");

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Erro ao incrementar versão (LOCAL): " + e.getMessage());
        }
    }

    /**
     * Usado pelo Primary (Servidor Principal) para executar uma query iniciada por um cliente.
     * Executa a query, incrementa a versão e notifica o cluster via Heartbeat com a query.
     */
    public boolean executeUpdateByClient(String sql) {
        writeLock.lock();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(sql);

            // 1. O Primary envia o heartbeat com a query e a nova versão
            // Nota: O getDbVersion() ainda retorna a versão antiga, mas o HeartbeatManager
            // deve enviar a versão que será aplicada APÓS o incremento.
            heartbeatManager.sendHeartbeat(sql);

            // 2. Incrementa a versão na BD
            incrementDbVersionLocalOnly();

            System.out.println("[DatabaseManager] Query CLIENTE executada e notificada. SQL: " + sql);
            return true;

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Erro SQL (CLIENTE): " + sql);
            System.err.println("[DatabaseManager] Erro SQL (CLIENTE): " + e.getMessage());
            return false;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Usado pelo Servidor Secundário ao receber uma query de sincronização via Multicast.
     * Apenas executa a query e incrementa a versão localmente (sem notificar).
     */
    public boolean executeUpdateBySync(String sql) {
        writeLock.lock();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Executa a query recebida do Primary
            stmt.executeUpdate(sql);

            // Incrementa a versão localmente
            incrementDbVersionLocalOnly();

            System.out.println("[DatabaseManager] Query SYNC executada. SQL: " + sql);
            return true;

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Erro SQL (SYNC): " + sql);
            System.err.println("[DatabaseManager] Erro SQL (SYNC): " + e.getMessage());
            return false;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Usado pelo Primary após uma transação (multi-query com commit/rollback) ser concluída.
     * Envia o Heartbeat com a query replicável (já commitada) e incrementa a versão.
     */
    public void notifyUpdateAfterTransaction(String replicableSql) {
        writeLock.lock();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. O Primary envia a query replicável
            heartbeatManager.sendHeartbeat(replicableSql);

            // 2. Incrementa a versão localmente
            incrementDbVersionLocalOnly();

            System.out.println("[DatabaseManager] Transação notificada e versão incrementada.");

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Erro ao incrementar versão (APÓS TRANSAÇÃO): " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }

    // Método mantido inalterado
    public static String hashCode(String code) {
        try {
            byte[] salt = "TEACHER_CODE_SALT".getBytes();
            PBEKeySpec spec = new PBEKeySpec(code.toCharArray(), salt, 65536, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao gerar hash", e);
        }
    }
}