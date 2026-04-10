package ru.spbpu.ellipsis.ellipsis;

import ru.spbpu.ellipsis.model.EllipticalSentence;
import ru.spbpu.ellipsis.model.Sentence;
import ru.spbpu.ellipsis.model.SyntaxBlock;
import ru.spbpu.ellipsis.nlp.model.SyntaxToken;
import ru.spbpu.ellipsis.nlp.model.SyntaxTree;

import java.util.List;

/**
 * Детектор эллипсиса Типа 3.
 *
 * Охватывает побудительные, неопределённо-личные конструкции
 * и причастные/деепричастные обороты.
 *
 * Признаки:
 *   — тире + orphan-блок
 *   — И причастный оборот (acl+VerbForm=Part) ИЛИ нет подлежащего
 *
 * Примеры:
 *   «окружность, касающаяся стороны AC в точке D, стороны AB – в точке E»
 *   «Примите вершину C за начало координат, а ось проекций – за ось абсцисс»
 */
public class Type3Detector {

    public boolean matches(SyntaxTree tree, List<SyntaxBlock> orphans) {
        if (orphans.isEmpty()) return false;
        boolean hasDash      = tree.getSentence().contains("–") || tree.getSentence().contains("—");
        boolean hasPartClause = hasParticipleClause(tree);
        boolean noSubject    = tree.getSubjects().isEmpty();
        return hasDash && (hasPartClause || noSubject);
    }

    private boolean hasParticipleClause(SyntaxTree tree) {
        for (SyntaxToken t : tree.getTokens())
            if ("acl".equals(t.getDeprel()) && t.isParticiple()) return true;
        return false;
    }

    public EllipticalSentence build(Sentence sentence, SyntaxTree tree,
                                    List<SyntaxBlock> orphans) {
        return new EllipticalSentence(sentence, EllipsisType.TYPE_3_OTHER,
                                      orphans, List.of("тире"));
    }
}
