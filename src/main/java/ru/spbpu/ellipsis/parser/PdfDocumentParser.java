package ru.spbpu.ellipsis.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class PdfDocumentParser implements DocumentParser {
    private static final Logger log = LoggerFactory.getLogger(PdfDocumentParser.class);

    @Override
    public String parse(String filePath) {
        try (PDDocument doc = Loader.loadPDF(new File(filePath))) {
            return new PDFTextStripper().getText(doc);
        } catch (IOException e) { log.error("PDF: {}", e.getMessage()); return ""; }
    }

    @Override public boolean supports(String source) { return source.endsWith(".pdf"); }
}
