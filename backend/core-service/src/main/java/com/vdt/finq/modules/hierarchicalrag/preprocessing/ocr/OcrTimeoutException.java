package com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr;

import java.io.IOException;

/**
 * The model stopped sending data before the page was finished.
 *
 * Kept separate from a plain IOException because the retry ladder reacts to it
 * differently: a timeout usually means the network is struggling, so the next
 * attempt lowers the DPI to send a smaller image.
 */
public class OcrTimeoutException extends IOException {

    public OcrTimeoutException(String message) {
        super(message);
    }
}
