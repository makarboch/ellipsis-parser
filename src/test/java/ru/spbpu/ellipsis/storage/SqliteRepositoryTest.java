package ru.spbpu.ellipsis.storage;

import ru.spbpu.ellipsis.ellipsis.EllipsisType;
import ru.spbpu.ellipsis.model.EllipticalSentence;
import ru.spbpu.ellipsis.model.Sentence;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqliteRepositoryTest {

    private static final String DB = "test_ellipsis_temp.db";
    private SqliteSentenceRepository repo;

    @BeforeEach void setUp()    { repo = new SqliteSentenceRepository(DB); }
    @AfterEach  void tearDown() { repo.clear(); new File(DB).delete(); }

    @Test
    void save_новоеПредложение_returnsTrue() {
        assertTrue(repo.save(make("Тест 1")));
    }

    @Test
    void save_дубликат_returnsFalse() {
        repo.save(make("Дубликат"));
        assertFalse(repo.save(make("Дубликат")));
    }

    @Test
    void saveAll_считаетТолькоУникальные() {
        int saved = repo.saveAll(List.of(make("A"), make("B"), make("A")));
        assertEquals(2, saved);
    }

    @Test
    void exists_послеСохранения() {
        repo.save(make("Существует"));
        assertTrue(repo.exists("Существует"));
        assertFalse(repo.exists("Не существует"));
    }

    @Test
    void findAll_возвращаетВсеСохранённые() {
        repo.save(make("P1")); repo.save(make("P2"));
        assertEquals(2, repo.findAll().size());
    }

    @Test
    void count_совпадаетСFindAll() {
        repo.save(make("X")); repo.save(make("Y"));
        assertEquals(repo.findAll().size(), repo.count());
    }

    private EllipticalSentence make(String text) {
        Sentence s = new Sentence(text, "http://test.ru", 1);
        return new EllipticalSentence(s, EllipsisType.TYPE_1_MULTIPLE_SUBJECTS,
                List.of(), List.of("тире"));
    }
}
