package ru.spbpu.ellipsis.model;

import ru.spbpu.ellipsis.ellipsis.EllipsisType;
import java.util.List;

/**
 * Предложение с обнаруженным глагольным эллипсисом.
 *
 * Хранит:
 *   — исходное предложение (original)
 *   — тип эллипсиса (TYPE_1, TYPE_2, TYPE_3, UNKNOWN)
 *   — orphan-блоки: фрагменты без управляющего глагола
 *   — формальные маркеры: тире, союз «а» и др.
 *
 * Восстановление пропущенного глагола не входит в задачи данного НИР.
 * Цель — найти и сохранить предложения-кандидаты в банк.
 */
public class EllipticalSentence {

    private final Sentence         original;
    private final EllipsisType     type;
    private final List<SyntaxBlock> orphanBlocks;
    private final List<String>     formalMarkers;

    public EllipticalSentence(Sentence original,
                               EllipsisType type,
                               List<SyntaxBlock> orphanBlocks,
                               List<String> formalMarkers) {
        this.original      = original;
        this.type          = type;
        this.orphanBlocks  = orphanBlocks;
        this.formalMarkers = formalMarkers;
    }

    public Sentence              getOriginal()      { return original; }
    public EllipsisType          getType()          { return type; }
    public List<SyntaxBlock>     getOrphanBlocks()  { return orphanBlocks; }
    public List<String>          getFormalMarkers() { return formalMarkers; }

    @Override
    public String toString() {
        return String.format("[%s] %s | маркеры: %s | orphan-блоков: %d",
            type, original.getText(),
            String.join(", ", formalMarkers),
            orphanBlocks.size());
    }
}
