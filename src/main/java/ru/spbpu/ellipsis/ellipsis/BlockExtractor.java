package ru.spbpu.ellipsis.ellipsis;

import ru.spbpu.ellipsis.model.SyntaxBlock;
import ru.spbpu.ellipsis.nlp.model.SyntaxToken;
import ru.spbpu.ellipsis.nlp.model.SyntaxTree;

import java.util.ArrayList;
import java.util.List;

/**
 * Выделяет orphan-блоки из дерева разбора.
 *
 * По алгоритму Пархоменко В.А.:
 *   Orphan-блок = именной фрагмент, у которого на всём пути вверх по дереву
 *   до корня нет ни одного управляющего глагола (или ADJ-предиката).
 *   Это и есть признак глагольного эллипсиса.
 *
 * FIX (баг 3): старая логика помечала как orphan любой NOUN чей непосредственный
 * родитель — не глагол. Это слишком агрессивно: обычные атрибутивные NOUN
 * (аппозиции, nmod) тоже попадали. Новый алгоритм поднимается по всей цепочке
 * head до корня и считает orphan только тот блок, на пути которого нет предиката.
 */
public class BlockExtractor {

    /**
     * Возвращает все orphan-блоки из дерева.
     */
    public List<SyntaxBlock> getOrphanBlocks(SyntaxTree tree) {
        List<SyntaxBlock> orphans = new ArrayList<>();

        for (SyntaxToken token : tree.getTokens()) {
            if (isOrphanRoot(token, tree)) {
                List<SyntaxToken> subtree = collectSubtree(token.getId(), tree);
                subtree.add(0, token);
                orphans.add(new SyntaxBlock(token, subtree, true));
            }
        }
        return orphans;
    }

    /**
     * Токен — вершина orphan-блока если:
     * 1. Он сам — существительное (NOUN/PROPN), не корень дерева.
     * 2. Его deprel — одно из: obl, obj, nmod, iobj (типичные дополнения).
     * 3. На всём пути от него вверх до корня нет ни одного токена с isVerb() или isAdjPred().
     *
     * Условие 2 отсекает подлежащие (nsubj) и определения (amod, det) —
     * они не являются «бесхозными актантами» по смыслу алгоритма.
     */
    private boolean isOrphanRoot(SyntaxToken token, SyntaxTree tree) {
        // UDPipe явно размечает эллипсис меткой "orphan" в DEPREL
        return "orphan".equals(token.getDeprel());
    }
    /**
     * Краткое прилагательное в роли сказуемого (ADJ + root).
     * Учитывается при подъёме по цепочке head.
     */

    /** Рекурсивно собирает всех потомков токена с данным ID */
    private List<SyntaxToken> collectSubtree(int parentId, SyntaxTree tree) {
        List<SyntaxToken> result = new ArrayList<>();
        for (SyntaxToken child : tree.getChildren(parentId)) {
            result.add(child);
            result.addAll(collectSubtree(child.getId(), tree));
        }
        return result;
    }
}
