package ru.spbpu.ellipsis.ellipsis;

import ru.spbpu.ellipsis.model.EllipticalSentence;
import ru.spbpu.ellipsis.model.Sentence;
import ru.spbpu.ellipsis.model.SyntaxBlock;
import ru.spbpu.ellipsis.nlp.model.SyntaxToken;
import ru.spbpu.ellipsis.nlp.model.SyntaxTree;

import java.util.List;

/**
 * Детектор эллипсиса Типа 2.
 *
 * Структура: ИГ1_подл ГГр1_полная, [ГГрi_неполная, i=2..n]
 *
 * Признаки (AND):
 *   — ровно 1 подлежащее
 *   — предикат: VERB или ADJ-root
 *   — тире
 *   — orphan-блок
 *   — НЕТ причастного оборота (acl+Part) → иначе это Тип 3
 *
 * Пример: «Окружность касается AB, а сторону BC – в точке E»
 */
public class Type2Detector {

    public boolean matches(SyntaxTree tree, List<SyntaxBlock> orphans) {
        if (orphans.isEmpty()) return false;
        long subjects   = tree.getSubjects().size();
        boolean hasPred = !tree.getVerbs().isEmpty() || hasAdjPredicate(tree);
        boolean hasDash = tree.getSentence().contains("–") || tree.getSentence().contains("—");
        return subjects == 1 && hasPred && hasDash && !hasParticipleClause(tree);
    }

    private boolean hasAdjPredicate(SyntaxTree tree) {
        SyntaxToken root = tree.getRoot();
        return root != null && "ADJ".equals(root.getUpos());
    }

    private boolean hasParticipleClause(SyntaxTree tree) {
        for (SyntaxToken t : tree.getTokens())
            if ("acl".equals(t.getDeprel()) && t.isParticiple()) return true;
        return false;
    }

    public EllipticalSentence build(Sentence sentence, SyntaxTree tree,
                                    List<SyntaxBlock> orphans) {
        return new EllipticalSentence(sentence, EllipsisType.TYPE_2_SINGLE_SUBJECT,
                                      orphans, List.of("тире"));
    }
}
