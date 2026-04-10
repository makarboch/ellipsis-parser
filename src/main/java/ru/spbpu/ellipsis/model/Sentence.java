package ru.spbpu.ellipsis.model;

/**
 * Одно предложение, извлечённое из текста.
 *
 * Метод isCandidate() реализует предфильтрацию:
 * — Текст на русском языке
 * — Длина > 10 символов
 * — Содержит запятую, тире «–» или «—» (формальный признак эллипсиса)
 */
public class Sentence {

    private final String text;
    private final String sourceUrl;
    private final int index;

    public Sentence(String text, String sourceUrl, int index) {
        this.text = text;
        this.sourceUrl = sourceUrl;
        this.index = index;
    }

    /**
     * Предварительный фильтр кандидатов на глагольный эллипсис.
     * Проверяет формальные признаки без синтаксического анализа.
     */
    public boolean isCandidate() {
        return text != null
                && text.length() > 10
                && text.matches(".*[а-яА-ЯёЁ]+.*")        // русский текст
                && (text.contains(",")
                    || text.contains("–")
                    || text.contains("—"));
    }

    public String getText()      { return text; }
    public String getSourceUrl() { return sourceUrl; }
    public int getIndex()        { return index; }

    @Override
    public String toString() { return "[" + index + "] " + text; }
}
