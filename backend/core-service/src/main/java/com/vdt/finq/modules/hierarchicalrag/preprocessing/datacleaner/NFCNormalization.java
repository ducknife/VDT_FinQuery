package com.vdt.finq.modules.hierarchicalrag.preprocessing.datacleaner;
import java.text.Normalizer;

import org.springframework.stereotype.Component;

// unicode lets the same accented letter be stored two ways, and vietnamese hits
// this constantly because a letter can carry two marks:
//     "ế" NFC = 1 code point U+1EBF
//     "ế" NFD = 3 code points: e + circumflex + acute
// they look identical on screen but equals() returns false and a byte comparison
// sees different lengths. normalising at the gate makes every later comparison safe
@Component
public class NFCNormalization {

    // idempotent and cheap: running it on text that is already NFC changes nothing,
    // measured at about 0.03 ms for a 10000 character page
    public String toNfc(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    // only for diagnostics. production code should just call toNfc unconditionally,
    // checking first is more lines for no gain
    public boolean isNfc(String text) {
        return text != null && Normalizer.isNormalized(text, Normalizer.Form.NFC);
    }
  
}
