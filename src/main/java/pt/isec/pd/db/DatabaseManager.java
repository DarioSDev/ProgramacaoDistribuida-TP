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
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        return conn;
    }


    public void createSchema() {
        writeLock.lock();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS config (" +
                    "id INTEGER PRIMARY KEY CHECK (id = 1), " +
                    "version INTEGER NOT NULL DEFAULT 0, " +
                    "teacher_hash TEXT)");


            System.out.println("[DB][SCHEMA] Tabela criada/verificada: <config>");


            stmt.execute("CREATE TABLE IF NOT EXISTS docente (" +
                    "email TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "password TEXT NOT NULL, " +
                    "extra TEXT)");

            System.out.println("[DB][SCHEMA] Tabela criada/verificada: <docente>");


            stmt.execute("CREATE TABLE IF NOT EXISTS estudante (" +
                    "email TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "password TEXT NOT NULL, " +
                    "student_number TEXT UNIQUE)");

            System.out.println("[DB][SCHEMA] Tabela criada/verificada: <estudante>");

            stmt.execute("CREATE TABLE IF NOT EXISTS pergunta (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "text TEXT NOT NULL, " +
                    "start_time TEXT, " +  // NOVO
                    "end_time TEXT, " +    // NOVO
                    "docente_email TEXT, " +
                    "FOREIGN KEY(docente_email) REFERENCES docente(email))");

            System.out.println("[DB][SCHEMA] Tabela criada/verificada: <pergunta>");


            stmt.execute("CREATE TABLE IF NOT EXISTS opcao (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "text TEXT NOT NULL, " +
                    "is_correct BOOLEAN NOT NULL DEFAULT 0, " +
                    "pergunta_id INTEGER, " +
                    "FOREIGN KEY(pergunta_id) REFERENCES pergunta(id) ON DELETE CASCADE)");

            System.out.println("[DB][SCHEMA] Tabela criada/verificada: <opcao>");


            stmt.execute("CREATE TABLE IF NOT EXISTS resposta (" +
                    "estudante_email TEXT, " +
                    "pergunta_id INTEGER, " +
                    "opcao_id INTEGER, " +
                    "PRIMARY KEY (estudante_email, pergunta_id), " +
                    "FOREIGN KEY(estudante_email) REFERENCES estudante(email), " +
                    "FOREIGN KEY(pergunta_id) REFERENCES pergunta(id), " +
                    "FOREIGN KEY(opcao_id) REFERENCES opcao(id))");

            System.out.println("[DB][SCHEMA] Tabela criada/verificada: <resposta>");


            stmt.execute("INSERT OR IGNORE INTO config (id, version, teacher_hash) VALUES (1, 0, NULL)");

            PreparedStatement ps = conn.prepareStatement("SELECT teacher_hash FROM config WHERE id = 1");
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("[DB][SCHEMA] Inicializando teacher_hash...");

                String current = rs.getString("teacher_hash");
                if (current == null || current.isEmpty()) {

//                    String teacherCode = "p4Ssw0!3d";
                    String teacherCode = "...";
                    String hashed = hashCode(teacherCode);

                    PreparedStatement upd = conn.prepareStatement(
                            "UPDATE config SET teacher_hash = ? WHERE id = 1"
                    );
                    upd.setString(1, hashed);
                    upd.executeUpdate();

                    System.out.println("[DB][SCHEMA] teacher_hash gravado com sucesso!");


                    System.out.println("[DatabaseManager] Código único dos docentes inicializado.");
                }
            }

            schemaReady = true;
            System.out.println("[DatabaseManager] Schema verificado em: " + dbPath);

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

    public void incrementDbVersion(String query) {
        heartbeatManager.sendHeartbeat(query);
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("UPDATE config SET version = version + 1 WHERE id = 1");
            System.out.println("[DatabaseManager] Versão incrementada.");

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Erro ao incrementar versão: " + e.getMessage());
        }
    }

    public boolean executeUpdate(String sql) {
        writeLock.lock();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(sql);
            incrementDbVersion(sql);
            return true;

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Erro SQL: " + e.getMessage());
            return false;
        } finally {
            writeLock.unlock();
        }
    }

    public static String hashCode(String code) {
        System.out.println("Antes do try: " + code);
        try {
            byte[] salt = "TEACHER_CODE_SALT".getBytes();
            System.out.println("Debug 1");
            PBEKeySpec spec = new PBEKeySpec(code.toCharArray(), salt, 65536, 256);
            System.out.println("Debug 2");
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            System.out.println("Debug 3");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            System.out.println("Debug 4");
            System.out.println("DEbug 5 " + Base64.getEncoder().encodeToString(hash));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao gerar hash", e);
        }
    }

}