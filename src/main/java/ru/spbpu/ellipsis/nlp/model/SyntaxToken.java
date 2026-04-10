package ru.spbpu.ellipsis.nlp.model;

/**
 * Один токен из CoNLL-U разбора (UDPipe).
 *
 * CoNLL-U колонки:
 *   ID  FORM  LEMMA  UPOS  XPOS  FEATS  HEAD  DEPREL  DEPS  MISC
 *
 * Пример строки:
 *   3  перпендикулярна  перпендикулярный  ADJ  ...  0  root  ..
 *
 * FIX: isVerb() дополнен проверкой на причастие (VerbForm=Part),
 * isParticiple() теперь не требует isVerb() — UDPipe иногда помечает
 * причастия как VERB, иногда как ADJ (краткие причастия).
 */
public class SyntaxToken {

    private final int    id;
    private final String form;
    private final String lemma;
    private final String upos;
    private final String feats;
    private final int    head;
    private final String deprel;

    public SyntaxToken(int id, String form, String lemma,
                       String upos, String feats, int head, String deprel) {
        this.id     = id;
        this.form   = form;
        this.lemma  = lemma;
        this.upos   = upos;
        this.feats  = feats != null ? feats : "";
        this.head   = head;
        this.deprel = deprel != null ? deprel : "";
    }

    public boolean isVerb()    { return "VERB".equals(upos) || "AUX".equals(upos); }
    public boolean isNoun()    { return "NOUN".equals(upos) || "PROPN".equals(upos); }
    public boolean isSubject() { return "nsubj".equals(deprel) || "nsubj:pass".equals(deprel); }
    public boolean isRoot()    { return head == 0; }

    /**
     * Является ли причастием.
     * UDPipe: причастия чаще VERB+VerbForm=Part, но краткие страдательные
     * могут быть ADJ+VerbForm=Part. Проверяем feats независимо от upos.
     */
    public boolean isParticiple() {
        return feats.contains("VerbForm=Part");
    }

    public int    getId()     { return id; }
    public String getForm()   { return form; }
    public String getLemma()  { return lemma; }
    public String getUpos()   { return upos; }
    public String getFeats()  { return feats; }
    public int    getHead()   { return head; }
    public String getDeprel() { return deprel; }

    @Override
    public String toString() {
        return id + "\t" + form + "\t" + lemma + "\t" + upos + "\t" + head + "\t" + deprel;
    }
}
