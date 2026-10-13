package com.vdt.finq.modules.conversation;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.finq.common.exception.AppException;
import com.vdt.finq.common.exception.InvalidRequestException;
import com.vdt.finq.common.security.CurrentUser;
import com.vdt.finq.modules.conversation.ConversationRepository.FeedbackRow;
import com.vdt.finq.modules.rag.preprocessing.indexing.embedder.EmbeddingClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// feedback 3 mức (đúng / sai một phần / sai toàn bộ, sai thì bắt buộc lý do).
// trước khi trả lời: tìm câu đã từng bị chấm sai mà giống câu hỏi mới, đưa lý do sai vào làm bài học
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    public static final Set<String> LEVELS = Set.of("dung", "sai_mot_phan", "sai_toan_bo");
    private static final double MIN_SIMILARITY = 0.80;
    private static final int MAX_LESSONS = 3;

    private final ConversationRepository repository;
    private final EmbeddingClient embeddingClient;
    private final ObjectMapper json;

    public void save(CurrentUser user, long messageId, String level, String reason) {
        if (!LEVELS.contains(level)) {
            throw new InvalidRequestException("mức đánh giá phải là dung, sai_mot_phan hoặc sai_toan_bo");
        }
        String r = reason == null ? "" : reason.strip();
        if (!level.equals("dung") && r.isEmpty()) {
            throw new InvalidRequestException("câu trả lời sai thì phải ghi lý do");
        }
        String question = repository.questionOfOwnMessage(messageId, user.id())
                .orElseThrow(() -> new AppException(404, "không có câu trả lời " + messageId));
        String embedding = null;
        try {
            embedding = json.writeValueAsString(embeddingClient.embed(List.of(question)).get(0));
        } catch (IOException e) {
            // không có vector thì vẫn lưu feedback, chỉ không dùng được để tìm câu tương tự
            log.warn("embed câu hỏi của feedback lỗi: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        repository.upsertFeedback(messageId, user.id(), level, r.isEmpty() ? null : r, question, embedding);
    }

    // bài học từ feedback của câu tương tự (cosine >= 0,80), rỗng nếu không có
    public String lessonsFor(String question) {
        List<FeedbackRow> negatives = repository.negativeFeedback();
        if (negatives.isEmpty()) {
            return "";
        }
        float[] q;
        try {
            q = embeddingClient.embed(List.of(question)).get(0);
        } catch (IOException e) {
            log.warn("embed câu hỏi để tìm bài học lỗi: {}", e.getMessage());
            return "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
        record Scored(FeedbackRow row, double sim) {
        }
        return negatives.stream().map(f -> new Scored(f, cosine(q, vector(f.embedding()))))
                .filter(s -> s.sim() >= MIN_SIMILARITY)
                .sorted(Comparator.comparingDouble(Scored::sim).reversed()).limit(MAX_LESSONS)
                .map(s -> "- Câu tương tự \"" + s.row().cauHoi() + "\" từng bị đánh giá "
                        + (s.row().muc().equals("sai_toan_bo") ? "SAI TOÀN BỘ" : "SAI MỘT PHẦN") + "; lý do: "
                        + s.row().lyDo())
                .collect(Collectors.joining("\n"));
    }

    private float[] vector(String text) {
        try {
            return json.readValue(text, float[].class);
        } catch (IOException e) {
            return new float[0];
        }
    }

    private static double cosine(float[] a, float[] b) {
        if (a.length == 0 || a.length != b.length) {
            return 0;
        }
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return na == 0 || nb == 0 ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
