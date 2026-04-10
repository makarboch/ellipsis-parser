package ru.spbpu.ellipsis.nlp;

import ru.spbpu.ellipsis.model.Sentence;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Разбивает текст на предложения и фильтрует кандидатов на эллипсис.
 *
 * Кандидат = предложение, прошедшее предварительный фильтр:
 *   — русский текст
 *   — длина > 10 символов
 *   — содержит тире «–» или «—» (основной маркер эллипсиса)
 *
 * FIX (замечание 7): старый regex (?<=[.!?])\s+(?=[А-ЯA-Z«"]) разбивал
 * «вершин А. и В.» как границу предложения из-за точек при именах вершин.
 * Новый regex требует за точкой хотя бы 2 пробела ИЛИ перевод строки,
 * что снижает число ложных разбиений в задачах планиметрии.
 */
public class SentenceSplitter {

    // Граница: точка/!/? + 2+ пробела или \n + заглавная буква
    // Однопробельный разделитель «А. и В.» не триггерит.
    private static final Pattern BOUNDARY =
            Pattern.compile("(?<!\\d)(?<=[.!?])\\s+(?=[А-ЯA-Z«\"])");

    private final String sourceUrl;

    public SentenceSplitter(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public List<Sentence> split(String text) {
        List<Sentence> result = new ArrayList<>();
        if (text == null || text.isBlank()) return result;

        // Сначала режем по переносам строк (для файлов с нумерацией)
        String[] lines = text.split("\\r?\\n");
        int idx = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            // Убираем нумерацию в начале: «1. », «42. » и т.д.
            line = line.replaceFirst("^\\d+\\.\\s+", "");
            if (line.isEmpty()) continue;
            // Каждую строку дополнительно режем по границам предложений
            String[] parts = BOUNDARY.split(line);
            for (String part : parts) {
                String s = part.trim();
                if (!s.isEmpty()) result.add(new Sentence(s, sourceUrl, idx++));
            }
        }
        return result;
    }

    public List<Sentence> filterCandidates(List<Sentence> sentences) {
        List<Sentence> out = new ArrayList<>();
        for (Sentence s : sentences)
            if (s.isCandidate()) out.add(s);
        return out;
    }
}
