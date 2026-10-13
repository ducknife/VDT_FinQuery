package com.vdt.finq.modules.rag.preprocessing.indexing.embedder;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

import org.junit.jupiter.api.Test;

// test giải mã vector base64, không gọi mạng
class EmbeddingClientTest {

    // chuẩn OpenAI: mảng float32 xếp theo little-endian rồi mã hoá base64
    @Test
    void decodesLittleEndianFloat32() {
        float[] original = {0.0354f, -0.0152f, 1.0f, -0.0003f};
        ByteBuffer buffer = ByteBuffer.allocate(original.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float x : original) {
            buffer.putFloat(x);
        }
        String base64 = Base64.getEncoder().encodeToString(buffer.array());

        assertThat(EmbeddingClient.decode(base64)).containsExactly(original);
    }

    // đọc nhầm big-endian thì ra số rác, test này giữ cho thứ tự byte không bị đổi
    @Test
    void bigEndianWouldGiveWrongValues() {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(1.0f);
        String base64 = Base64.getEncoder().encodeToString(buffer.array());

        assertThat(EmbeddingClient.decode(base64)[0]).isNotEqualTo(1.0f);
    }
}
