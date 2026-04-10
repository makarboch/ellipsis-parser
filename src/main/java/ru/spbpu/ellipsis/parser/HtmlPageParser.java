package ru.spbpu.ellipsis.parser;

import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Парсер HTML-страниц.
 * Скачивает страницу через OkHttp, очищает разметку через Jsoup.
 */
public class HtmlPageParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(HtmlPageParser.class);
    private final OkHttpClient http = new OkHttpClient();

    @Override
    public String parse(String url) {
        log.info("Скачивание: {}", url);
        try {
            Request req = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "EllipsisParser/1.0")
                    .build();
            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null)
                    throw new IOException("HTTP " + resp.code());
                String html = resp.body().string();
                Document doc = Jsoup.parse(html, url);
                // Убрать скрипты, стили, навигацию
                doc.select("script, style, nav, header, footer, .menu").remove();
                // Добавляем переносы строк после блочных элементов перед извлечением текста
                for (org.jsoup.nodes.Element el : doc.select("p, li, h1, h2, h3, h4, div.problem, div.task")) {
                    el.after("\n");
                }
                String text = doc.body().text();
                return text.replaceAll("[ \\t]+", " ").trim();
            }
        } catch (IOException e) {
            log.error("HTML parse error {}: {}", url, e.getMessage());
            return "";
        }
    }

    @Override
    public boolean supports(String source) {
        return source.startsWith("http://") || source.startsWith("https://");
    }
}
