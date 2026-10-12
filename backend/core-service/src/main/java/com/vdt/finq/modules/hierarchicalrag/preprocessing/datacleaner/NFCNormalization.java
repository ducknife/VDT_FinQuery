package com.vdt.finq.modules.hierarchicalrag.preprocessing.datacleaner;
import java.text.Normalizer;

import org.springframework.stereotype.Component;

// một chữ có dấu có hai cách lưu (NFC/NFD) trông giống nhau mà equals() khác; chuẩn hoá ở cổng vào
@Component
public class NFCNormalization {

    // luỹ đẳng và rẻ (~0,03 ms/trang)
    public String toNfc(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    // chỉ để chẩn đoán; code thật cứ gọi toNfc
    public boolean isNfc(String text) {
        return text != null && Normalizer.isNormalized(text, Normalizer.Form.NFC);
    }
  
}
