package pt.isec.pd.db;

import java.io.File;
import java.sql.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DatabaseManager {
    private String dbPath;
    private final ReentrantReadWriteLock dbLock;
    private final Lock readLock;
    private final Lock writeLock;

    public DatabaseManager(String dbDirectory, String dbName) {
        this.dbPath = new File(dbDirectory, dbName).getAbsolutePath();
        this.dbLock = new ReentrantReadWriteLock();
        this.readLock = dbLock.readLock();
        this.writeLock = dbLock.writeLock();
    }

    public void setDbFile(File dbFile) {
        this.dbPath = dbFile.getAbsolutePath();
    }

    public File getDbFile() {
        return new File(dbPath);
    }

    public Lock getReadLock() { return readLock; }
    public Lock getWriteLock() { return writeLock; }

    Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    public void createSchema() {
        writeLock.lock();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS config (" +
                    "id INTEGER PRIMARY KEY CHECK (id = 1), " +
                    "version INTEGER NOT NULL DEFAULT 0, " +
                    "teacher_hash TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS docente (" +
                    "email TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "password TEXT NOT NULL, " +
                    "id_uuid TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS estudante (" +
                    "email TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "password TEXT NOT NULL, " +
                    "student_number TEXT UNIQUE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS pergunta (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "text TEXT NOT NULL, " +
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

            stmt.execute("INSERT OR IGNORE INTO config (id, version, teacher_hash) VALUES (1, 0, '')");

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

    public void incrementDbVersion() {
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
            incrementDbVersion();
            return true;

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Erro SQL: " + e.getMessage());
            return false;
        } finally {
            writeLock.unlock();
        }
    }


}