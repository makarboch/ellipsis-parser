package ru.spbpu.ellipsis.ellipsis;

import ru.spbpu.ellipsis.model.EllipticalSentence;
import ru.spbpu.ellipsis.model.Sentence;
import ru.spbpu.ellipsis.model.SyntaxBlock;
import ru.spbpu.ellipsis.nlp.model.SyntaxToken;
import ru.spbpu.ellipsis.nlp.model.SyntaxTree;

import java.util.ArrayList;
import java.util.List;

/**
 * Детектор эллипсиса Типа 1.
 *
 * Структура: ИГ1_подл ГГр1_полная, [ИГi_подл ГГрi_неполная, i=2..n]
 *
 * Признаки (AND):
 *   — 2+ подлежащих (nsubj / nsubj:pass)
 *   — предикат: VERB или ADJ-root («перпендикулярна», «равны»)
 *   — orphan-блок
 *   — маркер: «, а» или тире
 *
 * Пример: «диагональ AC перпендикулярна CD, а диагональ DB – AB»
 */
public class Type1Detector {

    public boolean matches(SyntaxTree tree, List<SyntaxBlock> orphans) {
        if (orphans.isEmpty()) return false;
        long subjects    = tree.getSubjects().size();
        boolean hasPred  = !tree.getVerbs().isEmpty() || hasAdjPredicate(tree);
        String  text     = tree.getSentence();
        boolean hasDash  = text.contains("–") || text.contains("—");
        boolean hasConjA = text.contains(", а ") || text.contains(",а ");
        return subjects >= 2 && hasPred && (hasDash || hasConjA);
    }

    private boolean hasAdjPredicate(SyntaxTree tree) {
        SyntaxToken root = tree.getRoot();
        return root != null && "ADJ".equals(root.getUpos());
    }

    public EllipticalSentence build(Sentence sentence, SyntaxTree tree,
                                    List<SyntaxBlock> orphans) {
        List<String> markers = new ArrayList<>();
        String text = tree.getSentence();
        if (text.contains("–") || text.contains("—")) markers.add("тире");
        if (text.contains(", а "))                    markers.add("союз «а»");
        return new EllipticalSentence(sentence, EllipsisType.TYPE_1_MULTIPLE_SUBJECTS,
                                      orphans, markers);
    }
}
