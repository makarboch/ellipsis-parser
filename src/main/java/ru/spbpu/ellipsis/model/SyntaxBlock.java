package ru.spbpu.ellipsis.model;

import ru.spbpu.ellipsis.nlp.model.SyntaxToken;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Синтаксический блок — связное поддерево токенов.
 *
 * Реализует концепцию «блока» из алгоритма руководителя (Пархоменко В.А.):
 *   Блок = совокупность слов, где каждое слово подчиняется какому-либо другому
 *   слову или подчиняет его согласно правилам управления русского языка.
 *   Если вершина блока не связана ни с одним другим блоком → эллипсис (orphan=true).
 */
public class SyntaxBlock {

    /** Вершина блока (управляющее слово) */
    private final SyntaxToken rootToken;

    /** Все токены блока (поддерево) */
    private final List<SyntaxToken> tokens;

    /**
     * true = блок «бесхозный»: его вершина не управляется никаким глаголом.
     * Это главный признак глагольного эллипсиса.
     */
    private final boolean orphan;

    public SyntaxBlock(SyntaxToken rootToken, List<SyntaxToken> tokens, boolean orphan) {
        this.rootToken = rootToken;
        this.tokens = tokens;
        this.orphan = orphan;
    }

    /**
     * Проверяет синтаксическую эквивалентность двух блоков.
     * Эквивалентность = одинаковая роль в предложении (deprel).
     * Используется для поиска «образца» при восстановлении эллипсиса.
     */
    public boolean isSyntacticallyEquivalentTo(SyntaxBlock other) {
        if (this.rootToken == null || other.rootToken == null) return false;
        return this.rootToken.getDeprel().equals(other.rootToken.getDeprel());
    }

    /** Текст блока (все токены через пробел) */
    public String getText() {
        return tokens.stream()
                .map(SyntaxToken::getForm)
                .collect(Collectors.joining(" "));
    }

    public SyntaxToken getRootToken()   { return rootToken; }
    public List<SyntaxToken> getTokens() { return tokens; }
    public boolean isOrphan()           { return orphan; }
}
