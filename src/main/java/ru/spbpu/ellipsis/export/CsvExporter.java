package ru.spbpu.ellipsis.export;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import ru.spbpu.ellipsis.model.EllipticalSentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/**
 * Экспорт банка предложений в CSV (разделитель «;», кодировка UTF-8).
 * Колонки: id; type; text; source_url; markers; orphan_blocks_count
 */
public class CsvExporter {

    private static final Logger log = LoggerFactory.getLogger(CsvExporter.class);

    public void export(List<EllipticalSentence> sentences, String outputPath) {
        try {
            Files.createDirectories(Path.of(outputPath).getParent());
            try (ICSVWriter w = new CSVWriterBuilder(
                    new FileWriter(outputPath, StandardCharsets.UTF_8))
                    .withSeparator(';').build()) {

                w.writeNext(new String[]{
                    "id", "type", "text", "source_url", "markers", "orphan_blocks"
                });
                for (int i = 0; i < sentences.size(); i++) {
                    EllipticalSentence s = sentences.get(i);
                    w.writeNext(new String[]{
                        String.valueOf(i + 1),
                        s.getType().name(),
                        s.getOriginal().getText(),
                        s.getOriginal().getSourceUrl(),
                        String.join("|", s.getFormalMarkers()),
                        String.valueOf(s.getOrphanBlocks().size())
                    });
                }
            }
            log.info("CSV: {} строк → {}", sentences.size(), outputPath);
        } catch (IOException e) {
            log.error("CsvExporter: {}", e.getMessage());
        }
    }
}
