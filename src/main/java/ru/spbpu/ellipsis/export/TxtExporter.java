package ru.spbpu.ellipsis.export;

import ru.spbpu.ellipsis.model.EllipticalSentence;
import ru.spbpu.ellipsis.model.ProcessingStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Экспорт банка предложений в TXT.
 *
 * Формат:
 *   === Предложение #N (TYPE_1) ===
 *   Источник: <url>
 *   Текст:    <текст>
 *   Маркеры:  тире, союз «а»
 *   Блоков без глагола: N
 *   ----------------------------------
 *
 * В конце — сводная статистика прогона (если передана).
 */
public class TxtExporter {

    private static final Logger log = LoggerFactory.getLogger(TxtExporter.class);
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public void export(List<EllipticalSentence> sentences, String outputPath) {
        export(sentences, outputPath, null);
    }

    public void export(List<EllipticalSentence> sentences, String outputPath,
                       ProcessingStats stats) {
        try {
            Files.createDirectories(Path.of(outputPath).getParent());
            try (PrintWriter w = new PrintWriter(outputPath, StandardCharsets.UTF_8)) {
                w.println("Банк предложений с глагольным эллипсисом");
                w.println("Сформирован: " + LocalDateTime.now().format(DT));
                w.println("Всего предложений: " + sentences.size());
                w.println("=".repeat(60));

                for (int i = 0; i < sentences.size(); i++) {
                    EllipticalSentence s = sentences.get(i);
                    w.printf("%n=== Предложение #%d (%s) ===%n", i + 1, s.getType());
                    w.println("Источник: " + s.getOriginal().getSourceUrl());
                    w.println("Текст:    " + s.getOriginal().getText());
                    w.println("Маркеры:  " + String.join(", ", s.getFormalMarkers()));
                    w.println("Блоков без глагола: " + s.getOrphanBlocks().size());
                    w.println("-".repeat(60));
                }

                if (stats != null) {
                    w.println();
                    w.println("=".repeat(60));
                    w.println("СТАТИСТИКА ПРОГОНА");
                    w.println(stats.toString());
                }
            }
            log.info("TXT: {} предложений → {}", sentences.size(), outputPath);
        } catch (IOException e) {
            log.error("TxtExporter: {}", e.getMessage());
        }
    }
}
