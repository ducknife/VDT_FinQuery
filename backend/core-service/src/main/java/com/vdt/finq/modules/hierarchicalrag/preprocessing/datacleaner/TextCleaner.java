package com.vdt.finq.modules.hierarchicalrag.preprocessing.datacleaner;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

// cleans a page before it is chunked or parsed.
// this NEVER rewrites the .md on disk: the raw file cost a day of network to make,
// so cleaning is a transformation applied while reading. change a rule and you only
// rerun the chunking, not the ocr

// 1. fence ```
// 2. &nbsp;
// 3. space at the end of line
// 4. 2 or more space consecutive
@Component
@RequiredArgsConstructor
public class TextCleaner {

    private static final String FENCE = "```"; // markdown guard

    // only collapse runs that follow a non space character. leading indentation is
    // left alone: 1003 lines in the corpus start with four or more spaces and in
    // markdown that means a code block
    private static final Pattern INNER_RUN = Pattern.compile("(?<=\\S)[ \\t]{2,}");

    // trailing whitespace, per line
    private static final Pattern TRAILING = Pattern.compile("[ \\t]+$", Pattern.MULTILINE);

    private final NFCNormalization nfcNormalization;

    public String clean(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        // order matters: normalise first so every later rule sees one encoding,
        // and &nbsp; must become spaces before the runs are collapsed
        String text = nfcNormalization.toNfc(raw);
        text = stripCodeFence(text);
        text = text.replace("&nbsp;", " ");
        text = INNER_RUN.matcher(text).replaceAll(" ");
        text = TRAILING.matcher(text).replaceAll("");
        return text.strip();
    }

    // the model sometimes wraps the whole page in ``` or ```markdown.
    // 76 pages in the corpus still carry it, the old node tool missed the labelled form
    private static String stripCodeFence(String text) {
        String trimmed = text.strip();
        if (!trimmed.startsWith(FENCE)) {
            return trimmed;
        }
        int firstNewline = trimmed.indexOf('\n');
        if (firstNewline < 0) {
            // a bare fence with no content
            return "";
        }
        // drop the opening line, it may be ``` or ```markdown
        String body = trimmed.substring(firstNewline + 1).stripTrailing();
        if (body.endsWith(FENCE)) {
            body = body.substring(0, body.length() - FENCE.length());
        }
        return body.strip();
    }
}
