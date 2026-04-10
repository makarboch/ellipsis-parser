package ru.spbpu.ellipsis.parser;

/** Интерфейс парсера документов. */
public interface DocumentParser {
    String parse(String source);
    boolean supports(String source);
}
