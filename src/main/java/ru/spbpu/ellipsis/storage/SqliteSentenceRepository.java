package ru.spbpu.ellipsis.storage;

import ru.spbpu.ellipsis.ellipsis.EllipsisType;
import ru.spbpu.ellipsis.model.EllipticalSentence;
import ru.spbpu.ellipsis.model.Sentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.sql.Statement;

/**
 * Хранилище уникальных эллиптических предложений (SQLite).
 *
 * Схема:
 *   elliptical_sentences(id, text UNIQUE, source_url, type, markers, created_at)
 *
 * Уникальность: INSERT OR IGNORE + TEXT UNIQUE гарантируют,
 * что одно и то же предложение не попадёт в банк дважды.
 */
public class SqliteSentenceRepository implements SentenceRepository {

    private static final Logger log = LoggerFactory.getLogger(SqliteSentenceRepository.class);

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS elliptical_sentences (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                text       TEXT UNIQUE NOT NULL,
                source_url TEXT,
                type       TEXT NOT NULL,
                markers    TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )""";

    private final Connection conn;

    public SqliteSentenceRepository(String dbPath) {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            try (Statement st = conn.createStatement()) { st.execute(DDL); }
            log.info("SQLite: {}", dbPath);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("SQLite JDBC драйвер не найден", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка инициализации SQLite: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean save(EllipticalSentence s) {
        String sql = "INSERT OR IGNORE INTO elliptical_sentences" +
                     "(text,source_url,type,markers) VALUES(?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getOriginal().getText());
            ps.setString(2, s.getOriginal().getSourceUrl());
            ps.setString(3, s.getType().name());
            ps.setString(4, String.join("|", s.getFormalMarkers()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("save error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public int saveAll(List<EllipticalSentence> sentences) {
        int count = 0;
        for (EllipticalSentence s : sentences) if (save(s)) count++;
        return count;
    }

    @Override
    public List<EllipticalSentence> findAll() {
        List<EllipticalSentence> result = new ArrayList<>();
        try (ResultSet rs = conn.createStatement()
                .executeQuery("SELECT id, text, source_url, type, markers FROM elliptical_sentences ORDER BY id")) {
            while (rs.next()) {
                Sentence s = new Sentence(rs.getString("text"),
                                          rs.getString("source_url"),
                                          rs.getInt("id"));
                result.add(new EllipticalSentence(s,
                        EllipsisType.valueOf(rs.getString("type")),
                        List.of(), List.of()));
            }
        } catch (SQLException e) {
            log.error("findAll error: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public List<EllipticalSentence> findByType(String type) {
        return findAll().stream()
                .filter(s -> s.getType().name().equals(type))
                .toList();
    }

    @Override
    public boolean exists(String text) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM elliptical_sentences WHERE text=?")) {
            ps.setString(1, text);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) { return false; }
    }

    @Override
    public void clear() {
        try (Statement st = conn.createStatement()) {
            st.execute("DELETE FROM elliptical_sentences");
        } catch (SQLException e) {
            log.error("clear error: {}", e.getMessage());
        }
    }

    public int count() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM elliptical_sentences")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { return 0; }
    }
}
