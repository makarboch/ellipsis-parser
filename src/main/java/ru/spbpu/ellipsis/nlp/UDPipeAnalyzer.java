package ru.spbpu.ellipsis.nlp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import ru.spbpu.ellipsis.nlp.model.SyntaxToken;
import ru.spbpu.ellipsis.nlp.model.SyntaxTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Синтаксический анализатор через UDPipe REST API.
 *
 * Публичный endpoint (бесплатный):
 *   POST https://lindat.mff.cuni.cz/services/udpipe/api/process
 *   Form params: model, tokenizer, tagger, parser, data=<текст>
 *   Ответ: {"result": "<CoNLL-U текст>"}
 *
 * Модель для русского: russian-syntagrus-ud-2.12-230717
 *
 * Пример CoNLL-U строки:
 *   1  Диагональ  диагональ  NOUN  ...  3  nsubj  ...
 *   3  перпендикулярна  перпендикулярный  ADJ  ...  0  root  ...
 */
public class UDPipeAnalyzer implements SyntaxAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(UDPipeAnalyzer.class);

    private static final String API_URL = "https://lindat.mff.cuni.cz/services/udpipe/api/process";
    private static final String MODEL   = "russian-syntagrus-ud-2.12-230717";

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public SyntaxTree analyze(String sentence) {
        try {
            RequestBody body = new FormBody.Builder()
                    .add("model",     MODEL)
                    .add("tokenizer", "")
                    .add("tagger",    "")
                    .add("parser",    "")
                    .add("data",      sentence)
                    .build();

            Request request = new Request.Builder().url(API_URL).post(body).build();

            try (Response response = http.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.error("UDPipe HTTP {}", response.code());
                    return empty(sentence);
                }
                String json = response.body().string();
                JsonNode root = mapper.readTree(json);
                String conllu = root.path("result").asText("");
                // конец отладки
                return new SyntaxTree(sentence, parseConllu(conllu));
            }
        } catch (IOException e) {
            log.error("UDPipe error: {}", e.getMessage());
            return empty(sentence);
        }
    }

    @Override
    public List<SyntaxTree> analyzeBatch(List<String> sentences) {
        // Реализовано как последовательный вызов analyze() для каждого предложения.
        // Оптимизация через пакетный запрос — направление дальнейшего развития.
        List<SyntaxTree> out = new ArrayList<>();
        for (String s : sentences) out.add(analyze(s));
        return out;
    }

    /** Парсит CoNLL-U текст в список токенов */
    private List<SyntaxToken> parseConllu(String conllu) {
        List<SyntaxToken> tokens = new ArrayList<>();
        for (String line : conllu.split("\n")) {
            if (line.startsWith("#") || line.isBlank()) continue;
            String[] f = line.split("\t");
            if (f.length >= 8) {
                try {
                    int id = Integer.parseInt(f[0]);
                    tokens.add(new SyntaxToken(id, f[1], f[2], f[3], f[5],
                            Integer.parseInt(f[6]), f[7]));
                } catch (NumberFormatException ignored) {
                    // Пропускаем многословные токены (1-2, 1.1 и т.д.) — они не являются числами
                }
            }
        }
        return tokens;
    }

    private SyntaxTree empty(String sentence) {
        return new SyntaxTree(sentence, List.of());
    }
}
