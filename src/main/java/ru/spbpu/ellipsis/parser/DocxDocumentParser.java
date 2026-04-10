package ru.spbpu.ellipsis.parser;

import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.StringJoiner;

public class DocxDocumentParser implements DocumentParser {
    private static final Logger log = LoggerFactory.getLogger(DocxDocumentParser.class);

    @Override
    public String parse(String filePath) {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(filePath))) {
            StringJoiner sj = new StringJoiner("\n");
            for (XWPFParagraph p : doc.getParagraphs()) {
                String t = p.getText().trim();
                if (!t.isEmpty()) sj.add(t);
            }
            return sj.toString();
        } catch (IOException e) { log.error("DOCX: {}", e.getMessage()); return ""; }
    }

    @Override public boolean supports(String source) { return source.endsWith(".docx"); }
}
