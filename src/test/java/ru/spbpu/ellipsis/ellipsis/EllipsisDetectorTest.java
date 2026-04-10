package ru.spbpu.ellipsis.ellipsis;

import ru.spbpu.ellipsis.model.EllipticalSentence;
import ru.spbpu.ellipsis.model.ProcessingStats;
import ru.spbpu.ellipsis.nlp.SyntaxAnalyzer;
import ru.spbpu.ellipsis.nlp.model.SyntaxToken;
import ru.spbpu.ellipsis.nlp.model.SyntaxTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Тесты детектора глагольного эллипсиса.
 *
 * Тест-кейсы взяты из примеров руководителя (записки Пархоменко В.А.).
 * SyntaxAnalyzer замокирован, чтобы не зависеть от сети/UDPipe.
 *
 * Включает тест производительности предфильтра:
 * проверяет, что на тексте без маркеров эллипсиса UDPipe не вызывается
 * (анализатор не получает ни одного вызова analyze()).
 */
class EllipsisDetectorTest {

    private EllipsisDetector detector;
    private SyntaxAnalyzer   mockAnalyzer;

    @BeforeEach
    void setUp() {
        mockAnalyzer = mock(SyntaxAnalyzer.class);
        detector     = new EllipsisDetector(mockAnalyzer);
    }

    // ── Тип 1 ──

    @Test
    void detectType1_диагональАС() {
        String sentence = "В трапеции ABCD диагональ AC перпендикулярна боковой стороне CD, "
                + "а диагональ DB – боковой стороне AB.";
        when(mockAnalyzer.analyze(anyString())).thenReturn(buildTree_Type1(sentence));

        List<EllipticalSentence> found = detector.detect(sentence);

        assertFalse(found.isEmpty(), "Должен обнаружить эллипсис Типа 1");
        assertEquals(EllipsisType.TYPE_1_MULTIPLE_SUBJECTS, found.get(0).getType());
        assertFalse(found.get(0).getOrphanBlocks().isEmpty());
        assertFalse(found.get(0).getFormalMarkers().isEmpty());
    }

    // ── Тип 2 ─────────────────────────────────────────────────────────────

    @Test
    void detectType2_окружностьКасается() {
        String sentence = "Окружность касается сторон AB и AD и пересекает сторону DC, "
                + "а сторону BC – в точке E.";
        when(mockAnalyzer.analyze(anyString())).thenReturn(buildTree_Type2(sentence));

        List<EllipticalSentence> found = detector.detect(sentence);

        assertFalse(found.isEmpty());
        assertEquals(EllipsisType.TYPE_2_SINGLE_SUBJECT, found.get(0).getType());
    }

    // ── Тип 3 ─────────────────────────────────────────────────────────────

    @Test
    void detectType3_причастныйОборот() {
        String sentence = "В треугольник ABC вписана окружность, касающаяся стороны AC в точке D, "
                + "стороны AB – в точке E и стороны BC – в точке F.";
        when(mockAnalyzer.analyze(anyString())).thenReturn(buildTree_Type3(sentence));

        List<EllipticalSentence> found = detector.detect(sentence);

        assertFalse(found.isEmpty());
        assertEquals(EllipsisType.TYPE_3_OTHER, found.get(0).getType());
    }

    // ── Негативные тесты ──────────────────────────────────────────────────

    @Test
    void noEllipsis_простоеПредложение() {
        // Нет тире → не проходит предфильтр → UDPipe не вызывается
        String sentence = "Треугольник ABC является прямоугольным.";
        List<EllipticalSentence> found = detector.detect(sentence);
        assertTrue(found.isEmpty(), "Простое предложение без тире: эллипсиса нет");
        verify(mockAnalyzer, never()).analyze(anyString());
    }

    @Test
    void noEllipsis_пустойТекст() {
        assertTrue(detector.detect("").isEmpty());
        verify(mockAnalyzer, never()).analyze(anyString());
    }

    // ── Тест производительности предфильтра ───────────────────────────────

    /**
     * По замечанию руководителя: текст без потенциальных эллипсисов
     * (без тире/союза «а») должен обрабатываться быстро — UDPipe не вызывается.
     *
     * Проверяем через статистику:
     *   afterPreFilter == 0 → syntaxMs должно быть ~0 (нет вызовов UDPipe)
     */
    @Test
    void preFilter_textWithoutEllipsisMarkers_udpipeNotCalled() {
        // Текст из простых предложений без тире и союза «а» после запятой
        String text = "Треугольник ABC является прямоугольным. " +
                      "Стороны AB и BC перпендикулярны. " +
                      "Площадь треугольника равна половине произведения катетов. " +
                      "Гипотенуза является наибольшей стороной.";

        ProcessingStats stats = new ProcessingStats("test");
        List<EllipticalSentence> found = detector.detect(text, "test", stats);

        assertTrue(found.isEmpty());
        assertEquals(0, stats.getAfterPreFilter(),
            "Предфильтр должен отсеять все предложения без маркеров эллипсиса");
        verify(mockAnalyzer, never()).analyze(anyString());
        System.out.printf("Предфильтр: %d пр. → %d кандидатов за %d мс (UDPipe: 0 вызовов)%n",
            stats.getTotalSentences(), stats.getAfterPreFilter(), stats.getPreFilterMs());
    }

    /**
     * Текст С маркерами эллипсиса: UDPipe вызывается только для кандидатов.
     * Показывает разницу между totalSentences и afterPreFilter.
     */
    @Test
    void preFilter_mixedText_onlyCandidatesSentToUDPipe() {
        String text = "Треугольник ABC является прямоугольным. " +     // нет маркера
                      "В трапеции ABCD диагональ AC перпендикулярна боковой стороне CD, " +
                      "а диагональ DB – боковой стороне AB. " +          // есть «, а» и «–»
                      "Гипотенуза является наибольшей стороной.";        // нет маркера

        when(mockAnalyzer.analyze(anyString())).thenReturn(buildTree_Type1(
            "В трапеции ABCD диагональ AC перпендикулярна боковой стороне CD, " +
            "а диагональ DB – боковой стороне AB."));

        ProcessingStats stats = new ProcessingStats("test");
        detector.detect(text, "test", stats);

        assertTrue(stats.getTotalSentences() > stats.getAfterPreFilter(),
            "Предфильтр должен сократить число предложений для UDPipe");
        // UDPipe вызван только для 1 кандидата, а не для всех 3 предложений
        verify(mockAnalyzer, times(stats.getAfterPreFilter())).analyze(anyString());
        System.out.printf("Предфильтр: %d пр. → %d кандидатов (%.1f%% отсеяно)%n",
            stats.getTotalSentences(), stats.getAfterPreFilter(),
            stats.preFilterRejectRate());
    }

    // ── Mock-деревья ──────────────────────────────────────────────────────

    private SyntaxTree buildTree_Type1(String sentence) {
        return new SyntaxTree(sentence, List.of(
            new SyntaxToken(1,  "диагональ",       "диагональ",       "NOUN", "",  3,  "nsubj"),
            new SyntaxToken(2,  "AC",              "ac",              "PROPN","",  1,  "flat"),
            new SyntaxToken(3,  "перпендикулярна", "перпендикулярный","ADJ",  "",  0,  "root"),
            new SyntaxToken(4,  "боковой",         "боковой",         "ADJ",  "",  5,  "amod"),
            new SyntaxToken(5,  "стороне",         "сторона",         "NOUN", "",  3,  "obl"),
            new SyntaxToken(6,  "CD",              "cd",              "PROPN","",  5,  "flat"),
            new SyntaxToken(7,  "диагональ",       "диагональ",       "NOUN", "",  3,  "nsubj"),
            new SyntaxToken(8,  "DB",              "db",              "PROPN","",  7,  "flat"),
            new SyntaxToken(9,  "боковой",         "боковой",         "ADJ",  "",  10, "amod"),
            new SyntaxToken(10, "стороне",         "сторона",         "NOUN", "",  3,  "obl"),
            new SyntaxToken(11, "AB",              "ab",              "PROPN","",  10, "flat")
        ));
    }

    private SyntaxTree buildTree_Type2(String sentence) {
        return new SyntaxTree(sentence, List.of(
            new SyntaxToken(1, "Окружность", "окружность", "NOUN", "", 2, "nsubj"),
            new SyntaxToken(2, "касается",   "касаться",   "VERB", "", 0, "root"),
            new SyntaxToken(3, "сторону",    "сторона",    "NOUN", "", 2, "obl"),
            new SyntaxToken(4, "BC",         "bc",         "PROPN","", 3, "flat"),
            new SyntaxToken(5, "в",          "в",          "ADP",  "", 6, "case"),
            new SyntaxToken(6, "точке",      "точка",      "NOUN", "", 3, "nmod")
        ));
    }

    private SyntaxTree buildTree_Type3(String sentence) {
        return new SyntaxTree(sentence, List.of(
            new SyntaxToken(1, "вписана",    "вписать",    "VERB", "VerbForm=Part", 0, "root"),
            new SyntaxToken(2, "окружность", "окружность", "NOUN", "",              1, "nsubj:pass"),
            new SyntaxToken(3, "касающаяся", "касаться",   "VERB", "VerbForm=Part", 2, "acl"),
            new SyntaxToken(4, "стороны",    "сторона",    "NOUN", "",              3, "obl"),
            new SyntaxToken(5, "AC",         "ac",         "PROPN","",              4, "flat"),
            new SyntaxToken(6, "стороны",    "сторона",    "NOUN", "",              2, "obl"),
            new SyntaxToken(7, "AB",         "ab",         "PROPN","",              6, "flat")
        ));
    }
}
