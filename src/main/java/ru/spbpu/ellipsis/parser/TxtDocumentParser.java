package ru.spbpu.ellipsis.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class TxtDocumentParser implements DocumentParser {
    private static final Logger log = LoggerFactory.getLogger(TxtDocumentParser.class);

    @Override
    public String parse(String filePath) {
        try { return Files.readString(Path.of(filePath), StandardCharsets.UTF_8); }
        catch (IOException e) { log.error("TXT: {}", e.getMessage()); return ""; }
    }

    @Override public boolean supports(String source) { return source.endsWith(".txt"); }
}
