package ru.spbpu.ellipsis.model;

/**
 * Статистика одного прогона анализа.
 *
 * Фиксирует количество предложений на каждом этапе конвейера и время,
 * затраченное на каждый этап. Используется для демонстрации эффекта
 * предварительной фильтрации:
 *
 *   Этап 1 — дешёвый фильтр (строковые проверки, ~мкс/предложение):
 *     наличие тире/запятой, русский текст, минимальная длина.
 *
 *   Этап 2 — дорогой синтаксический анализ через UDPipe REST API
 *     (~100–500 мс/предложение): только для прошедших этап 1.
 *
 * Ожидаемый эффект (по замечанию руководителя): на текстах без эллипсисов
 * этап 1 отсекает почти все предложения, и UDPipe вызывается редко.
 * На текстах с эллипсисами прирост меньше, но фильтр всё равно существенно
 * сокращает число дорогих запросов.
 */
public class ProcessingStats {

    private final String source;

    private int totalSentences;
    private int afterPreFilter;   // прошли дешёвый фильтр (этап 1)
    private int afterSyntax;      // найден хотя бы 1 orphan-блок (этап 2)
    private int savedNew;         // сохранены как новые уникальные

    private long preFilterMs;     // мс на этап 1
    private long syntaxMs;        // мс на этап 2 (UDPipe)
    private long totalMs;

    private long memoryBeforeMb;
    private long memoryAfterMb;
    private double peakCpuPercent;

    public void setMemoryBeforeMb(long mb) { this.memoryBeforeMb = mb; }
    public void setMemoryAfterMb(long mb)  { this.memoryAfterMb = mb; }
    public void setPeakCpuPercent(double p){ this.peakCpuPercent = p; }
    public long getMemoryBeforeMb()        { return memoryBeforeMb; }
    public long getMemoryAfterMb()         { return memoryAfterMb; }
    public double getPeakCpuPercent()      { return peakCpuPercent; }

    public ProcessingStats(String source) {
        this.source = source;
    }

    // ── Setters ──────────────────────────────────────────────────────────

    public void setTotalSentences(int n)  { this.totalSentences = n; }
    public void setAfterPreFilter(int n)  { this.afterPreFilter = n; }
    public void setAfterSyntax(int n)     { this.afterSyntax = n; }
    public void setSavedNew(int n)        { this.savedNew = n; }
    public void setPreFilterMs(long ms)   { this.preFilterMs = ms; }
    public void setSyntaxMs(long ms)      { this.syntaxMs = ms; }
    public void setTotalMs(long ms)       { this.totalMs = ms; }

    // ── Getters ──────────────────────────────────────────────────────────

    public String getSource()         { return source; }
    public int getTotalSentences()    { return totalSentences; }
    public int getAfterPreFilter()    { return afterPreFilter; }
    public int getAfterSyntax()       { return afterSyntax; }
    public int getSavedNew()          { return savedNew; }
    public long getPreFilterMs()      { return preFilterMs; }
    public long getSyntaxMs()         { return syntaxMs; }
    public long getTotalMs()          { return totalMs; }

    /** Доля предложений, отсеянных дешёвым фильтром (не дошли до UDPipe) */
    public double preFilterRejectRate() {
        if (totalSentences == 0) return 0.0;
        return 100.0 * (totalSentences - afterPreFilter) / totalSentences;
    }

    @Override
    public String toString() {
        return String.format(
                "Источник: %s%n" +
                        "  Всего предложений:          %4d%n" +
                        "  После предфильтра (этап 1): %4d  (отсеяно %.1f%%, время %d мс)%n" +
                        "  После синт. анализа (этап 2):%4d  (время %d мс)%n" +
                        "  Сохранено уникальных:       %4d%n" +
                        "  ОЗУ (этап 2): до %d МБ → после %d МБ%n" +
                        "  Пик CPU (этап 2): %.1f%%%n" +
                        "  Итого: %d мс",
                source, totalSentences,
                afterPreFilter, preFilterRejectRate(), preFilterMs,
                afterSyntax, syntaxMs,
                savedNew,
                memoryBeforeMb, memoryAfterMb,
                peakCpuPercent,
                totalMs
        );
    }
}
