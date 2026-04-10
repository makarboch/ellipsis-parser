package ru.spbpu.ellipsis.nlp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Синтаксическое дерево одного предложения.
 * Строится из CoNLL-U вывода UDPipe.
 */
public class SyntaxTree {

    private final String sentence;
    private final List<SyntaxToken> tokens;

    public SyntaxTree(String sentence, List<SyntaxToken> tokens) {
        this.sentence = sentence;
        this.tokens   = tokens;
    }

    /** Все глагольные токены (VERB, AUX) */
    public List<SyntaxToken> getVerbs() {
        return tokens.stream().filter(SyntaxToken::isVerb).collect(Collectors.toList());
    }

    /** Все подлежащие (deprel = nsubj или nsubj:pass) */
    public List<SyntaxToken> getSubjects() {
        return tokens.stream().filter(SyntaxToken::isSubject).collect(Collectors.toList());
    }

    /** Все причастия */
    public List<SyntaxToken> getParticiples() {
        return tokens.stream().filter(SyntaxToken::isParticiple).collect(Collectors.toList());
    }

    /** Прямые потомки токена с данным ID */
    public List<SyntaxToken> getChildren(int parentId) {
        return tokens.stream()
                .filter(t -> t.getHead() == parentId)
                .collect(Collectors.toList());
    }

    /** Корень дерева (head == 0) */
    public SyntaxToken getRoot() {
        return tokens.stream().filter(SyntaxToken::isRoot).findFirst().orElse(null);
    }

    /** Токен по его ID */
    public SyntaxToken getById(int id) {
        return tokens.stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }

    public String getSentence()          { return sentence; }
    public List<SyntaxToken> getTokens() { return tokens; }
}
