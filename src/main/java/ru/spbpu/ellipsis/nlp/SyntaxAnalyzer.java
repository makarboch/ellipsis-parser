package ru.spbpu.ellipsis.nlp;

import ru.spbpu.ellipsis.nlp.model.SyntaxTree;
import java.util.List;

/**
 * Интерфейс синтаксического анализатора.
 * Реализация: UDPipeAnalyzer (REST API).
 */
public interface SyntaxAnalyzer {

    /** Анализирует одно предложение, возвращает дерево зависимостей */
    SyntaxTree analyze(String sentence);

    /** Batch-анализ списка предложений */
    List<SyntaxTree> analyzeBatch(List<String> sentences);
}
