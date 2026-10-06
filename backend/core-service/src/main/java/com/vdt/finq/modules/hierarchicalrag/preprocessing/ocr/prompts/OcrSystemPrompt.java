package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.prompts;

public class OcrSystemPrompt {
    public static final String OCR_SYSTEM_PROMPT = String.join("\n",
            "Extract the entire content of this page into Markdown format.",
            "- Every table must be formatted as a Markdown table, with each column separated correctly.",
            "- Retain all index codes, note numbers, and the EXACT full digits of every single number.",
            "- Keep parenthesized formats for negative numbers exactly as they are.",
            "- Leave empty cells blank; do not fill them with zeros.",
            "- Only return the page content. Do not include any explanations or introductory text.");
}
