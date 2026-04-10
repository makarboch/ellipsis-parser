package ru.spbpu.ellipsis.storage;

import ru.spbpu.ellipsis.model.EllipticalSentence;
import java.util.List;

/**
 * Репозиторий уникальных эллиптических предложений.
 * Реализация: SqliteSentenceRepository.
 */
public interface SentenceRepository {

    /** Сохранить. Возвращает true если предложение новое (не дубликат) */
    boolean save(EllipticalSentence sentence);

    /** Сохранить список. Возвращает количество реально добавленных */
    int saveAll(List<EllipticalSentence> sentences);

    List<EllipticalSentence> findAll();
    List<EllipticalSentence> findByType(String type);
    boolean exists(String text);
    void clear();
}
