package ru.spbpu.ellipsis;

import ru.spbpu.ellipsis.ellipsis.EllipsisDetector;
import ru.spbpu.ellipsis.export.CsvExporter;
import ru.spbpu.ellipsis.export.TxtExporter;
import ru.spbpu.ellipsis.model.EllipticalSentence;
import ru.spbpu.ellipsis.model.ProcessingStats;
import ru.spbpu.ellipsis.parser.*;
import ru.spbpu.ellipsis.storage.SqliteSentenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Точка входа — парсер для поиска предложений с глагольным эллипсисом.
 *
 * Использование:
 *   java -jar ellipsis-parser.jar <url-или-путь-к-файлу>
 *
 * Поддерживаемые источники:
 *   http(s)://...   — HTML-страница (Jsoup)
 *   *.txt           — текстовый файл
 *   *.docx          — документ Word (Apache POI)
 *   *.pdf           — документ PDF (PDFBox)
 *
 * Конвейер:
 *   1. Парсинг источника → сырой текст
 *   2. Разбиение на предложения
 *   3. Предфильтр (тире/запятая, русский текст) — дёшево
 *   4. Синтаксический анализ UDPipe — только для кандидатов
 *   5. Поиск orphan-блоков → классификация типа эллипсиса
 *   6. Сохранение уникальных предложений в SQLite
 *   7. Экспорт в output/results.txt и output/results.csv
 *
 * Результат включает статистику по каждому этапу (кол-во предложений,
 * время), что позволяет оценить эффект предфильтрации.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        if (args.length == 0) {
            log.error("Использование: java -jar ellipsis-parser.jar <url-или-путь>");
            log.error("Примеры:");
            log.error("  java -jar ellipsis-parser.jar https://example.com/article");
            log.error("  java -jar ellipsis-parser.jar data/input/sample.txt");
            System.exit(1);
        }

        String source = args[0];
        log.info("Запуск анализа: {}", source);

        // ── 1. Выбор и запуск парсера ─
        DocumentParser parser = selectParser(source);
        String rawText = parser.parse(source);
        if (rawText.isBlank()) {
            log.error("Не удалось получить текст из источника: {}", source);
            System.exit(1);
        }

        // ── 2. Двухэтапная детекция со сбором статистики ──
        ProcessingStats stats   = new ProcessingStats(source);
        EllipsisDetector detector = new EllipsisDetector();
        List<EllipticalSentence> found = detector.detect(rawText, source, stats);

        // ── 3. Сохранение в SQLite ──
        new File("output").mkdirs();
        SqliteSentenceRepository repo = new SqliteSentenceRepository("output/ellipsis.db");
        int saved = repo.saveAll(found);
        stats.setSavedNew(saved);

        // ── 4. Экспорт ──
        new TxtExporter().export(found, "output/results.txt", stats);
        new CsvExporter().export(found, "output/results.csv");

        // ── 5. Вывод итогов ──
        log.info("{}", stats);
        log.info("Всего в банке (БД): {} предложений", repo.count());
        log.info("Файлы: output/results.txt, output/results.csv, output/ellipsis.db");
    }

    private static DocumentParser selectParser(String source) {
        if (source.startsWith("http://") || source.startsWith("https://"))
            return new HtmlPageParser();
        if (source.endsWith(".txt"))  return new TxtDocumentParser();
        if (source.endsWith(".docx")) return new DocxDocumentParser();
        if (source.endsWith(".pdf"))  return new PdfDocumentParser();
        throw new IllegalArgumentException("Неподдерживаемый тип источника: " + source);
    }
}
