package ru.spbpu.ellipsis.ellipsis;

import ru.spbpu.ellipsis.model.EllipticalSentence;
import ru.spbpu.ellipsis.model.ProcessingStats;
import ru.spbpu.ellipsis.model.Sentence;
import ru.spbpu.ellipsis.model.SyntaxBlock;
import ru.spbpu.ellipsis.nlp.SentenceSplitter;
import ru.spbpu.ellipsis.nlp.SyntaxAnalyzer;
import ru.spbpu.ellipsis.nlp.UDPipeAnalyzer;
import ru.spbpu.ellipsis.nlp.model.SyntaxTree;
import ru.spbpu.ellipsis.nlp.model.SyntaxToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

import java.util.ArrayList;
import java.util.List;


/**
 * Двухэтапный конвейер обнаружения глагольного эллипсиса.
 *
 * ═══ Этап 1 — дешёвый предфильтр (~мкс/предложение) ═══════════════════
 *   SentenceSplitter.filterCandidates():
 *     • русский текст
 *     • длина > 10 символов
 *     • содержит тире «–»/«—» или запятую + союз/тире
 *   Предложения, не прошедшие фильтр, до UDPipe НЕ доходят.
 *
 * ═══ Этап 2 — синтаксический анализ (~100–500 мс/предложение) ══════════
 *   Только для кандидатов после этапа 1:
 *     • UDPipe строит дерево зависимостей (CoNLL-U)
 *     • BlockExtractor ищет orphan-блоки (фрагменты без управляющего глагола)
 *     • Type1/2/3Detector классифицирует тип эллипсиса
 *
 * По замечанию руководителя: на текстах без эллипсисов этап 1 отсекает
 * большинство предложений, и время работы существенно меньше, чем при
 * анализе всех предложений через UDPipe.
 *
 * ProcessingStats фиксирует количество предложений и время на каждом этапе.
 */
public class EllipsisDetector {

    private static final Logger log = LoggerFactory.getLogger(EllipsisDetector.class);

    private final SyntaxAnalyzer analyzer;
    private final BlockExtractor blockExtractor;
    private final Type1Detector  type1;
    private final Type2Detector  type2;
    private final Type3Detector  type3;

    public EllipsisDetector() {
        this.analyzer       = new UDPipeAnalyzer();
        this.blockExtractor = new BlockExtractor();
        this.type1          = new Type1Detector();
        this.type2          = new Type2Detector();
        this.type3          = new Type3Detector();
    }

    /** Конструктор для тестирования (с mock-анализатором) */
    public EllipsisDetector(SyntaxAnalyzer analyzer) {
        this.analyzer       = analyzer;
        this.blockExtractor = new BlockExtractor();
        this.type1          = new Type1Detector();
        this.type2          = new Type2Detector();
        this.type3          = new Type3Detector();
    }

    /**
     * Основной метод. Возвращает найденные предложения и заполняет статистику.
     *
     * @param rawText  текст из парсера документа/страницы
     * @param source   источник (URL или путь) — для метаданных и stats
     * @param stats    объект статистики — заполняется в процессе работы
     */
    public List<EllipticalSentence> detect(String rawText, String source,
                                           ProcessingStats stats) {
        List<EllipticalSentence> result = new ArrayList<>();
        if (rawText == null || rawText.isBlank()) return result;

        long totalStart = System.currentTimeMillis();

        // ── Этап 1: разбиение + дешёвый предфильтр ────────────────────────
        long preStart = System.currentTimeMillis();
        SentenceSplitter splitter = new SentenceSplitter(source);
        List<Sentence> all        = splitter.split(rawText);
        List<Sentence> candidates = splitter.filterCandidates(all);
        stats.setTotalSentences(all.size());
        stats.setAfterPreFilter(candidates.size());
        stats.setPreFilterMs(System.currentTimeMillis() - preStart);

        log.info("[{}] Предложений: {}, после предфильтра: {}",
                 source, all.size(), candidates.size());

        Runtime rt = Runtime.getRuntime();
        long memBefore = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        OperatingSystemMXBean os =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        // ── Этап 2: синтаксический анализ (UDPipe) — только для кандидатов ─
        long syntaxStart = System.currentTimeMillis();
        int withOrphans = 0;

        for (Sentence candidate : candidates) {
            SyntaxTree      tree    = analyzer.analyze(candidate.getText());
            List<SyntaxBlock> orphans = blockExtractor.getOrphanBlocks(tree);

            if (orphans.isEmpty()) {
                log.debug("Orphan-блоков нет: {}", candidate.getText());
                continue;
            }
            withOrphans++;

            EllipticalSentence es = classify(candidate, tree, orphans);
            result.add(es);
        }

        stats.setAfterSyntax(withOrphans);
        stats.setSyntaxMs(System.currentTimeMillis() - syntaxStart);
        long memAfter = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        stats.setMemoryBeforeMb(memBefore);
        stats.setMemoryAfterMb(memAfter);
        stats.setPeakCpuPercent(os.getProcessCpuLoad() * 100);
        stats.setTotalMs(System.currentTimeMillis() - totalStart);

        log.info("[{}] Найдено с эллипсисом: {}, время: {} мс",
                 source, result.size(), stats.getTotalMs());
        return result;
    }

    /**
     * Упрощённый вызов без статистики (для тестов и CLI-режима без stats).
     */
    public List<EllipticalSentence> detect(String rawText) {
        return detect(rawText, "unknown", new ProcessingStats("unknown"));
    }

    /**
     * Классификация по типу: Type1 → Type3 → Type2 → UNKNOWN.
     * Type3 проверяется ДО Type2 — причастные обороты специфичнее.
     */
    private EllipticalSentence classify(Sentence sentence, SyntaxTree tree,
                                        List<SyntaxBlock> orphans) {
        if (type1.matches(tree, orphans))
            return type1.build(sentence, tree, orphans);
        if (type3.matches(tree, orphans))
            return type3.build(sentence, tree, orphans);
        if (type2.matches(tree, orphans))
            return type2.build(sentence, tree, orphans);

        log.debug("Тип не определён (UNKNOWN): {}", sentence.getText());
        return new EllipticalSentence(sentence, EllipsisType.UNKNOWN,
                                      orphans, List.of());
    }
}
