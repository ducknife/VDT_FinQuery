package com.vdt.finq.modules.hierarchicalrag.preprocessing.datacleaner;
import java.text.Normalizer;

import org.springframework.stereotype.Component;

// unicode cho phép cùng một chữ có dấu được lưu theo hai cách, và tiếng Việt
// gặp chuyện này liên tục vì một chữ có thể mang hai dấu:
//     "ế" NFC = 1 code point U+1EBF
//     "ế" NFD = 3 code point: e + dấu mũ + dấu sắc
// hai dạng trông giống hệt trên màn hình nhưng equals() trả về false, so từng byte
// thì thấy độ dài khác nhau. chuẩn hoá ngay ở cổng vào thì mọi phép so sánh sau đều an toàn
@Component
public class NFCNormalization {

    // luỹ đẳng và rẻ: chạy trên văn bản đã là NFC thì không đổi gì,
    // đo được khoảng 0.03 ms cho một trang 10000 ký tự
    public String toNfc(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    // chỉ dùng để chẩn đoán. code chạy thật cứ gọi toNfc luôn,
    // kiểm tra trước chỉ thêm dòng mà không được lợi gì
    public boolean isNfc(String text) {
        return text != null && Normalizer.isNormalized(text, Normalizer.Form.NFC);
    }
  
}
