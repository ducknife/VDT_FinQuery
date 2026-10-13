package com.vdt.finq.modules.agent.evidence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.vdt.finq.modules.rag.retrieval.RetrievedChunk;

// kho bằng chứng của một câu hỏi: gom kết quả mọi tool, gán mã, bỏ trùng
public class EvidenceStore {

    private final Map<String, Evidence> byId = new LinkedHashMap<>();
    // chunk đã có (tài liệu|trang|phần) -> mã, để hai lần tìm trùng chunk không bị đếm hai lần
    private final Map<String, String> chunkIds = new LinkedHashMap<>();
    // số liệu DB đã có (mã|phạm vi|từ|đến) -> mã D#
    private final Map<String, String> figureIds = new LinkedHashMap<>();
    private int chunkSeq;
    private int figureSeq;

    // thêm các chunk sau rerank (giữ thứ hạng), trả về mã đã gán theo đúng thứ tự
    public List<Evidence> addChunks(List<RetrievedChunk> chunks) {
        List<Evidence> added = new ArrayList<>();
        for (RetrievedChunk c : chunks) {
            String key = c.getTaiLieuId() + "|" + c.getTrang() + "|" + c.getPhan();
            String existing = chunkIds.get(key);
            if (existing != null) {
                added.add(byId.get(existing));
                continue;
            }
            String id = "C" + (++chunkSeq);
            Double score = c.getTier2Score() != null ? c.getTier2Score() : c.getTier1Score();
            Evidence e = new Evidence(id, Evidence.Kind.CHUNK, c.getTaiLieuId(), c.getTenHienThi(), c.getTrang(),
                    c.getPhan(), c.textForModel(), score);
            byId.put(id, e);
            chunkIds.put(key, id);
            added.add(e);
        }
        return added;
    }

    public Evidence addFigure(String taiLieuId, String tenHienThi, int trang, String content) {
        String id = "D" + (++figureSeq);
        Evidence e = new Evidence(id, Evidence.Kind.FIGURE, taiLieuId, tenHienThi, trang, 0, content, null);
        byId.put(id, e);
        return e;
    }

    // số đã tra trong câu hỏi này (khoá: mã|phạm vi|từ|đến): tra lại thì dùng lại mã cũ, không sinh D# mới
    public Evidence figure(String key) {
        String id = figureIds.get(key);
        return id == null ? null : byId.get(id);
    }

    public Evidence addFigure(String key, String taiLieuId, String tenHienThi, int trang, String content) {
        Evidence e = addFigure(taiLieuId, tenHienThi, trang, content);
        figureIds.put(key, e.getId());
        return e;
    }

    // kết quả Java tính: không có trang nguồn, nội dung ghi rõ các D# đầu vào
    public Evidence addComputed(String content) {
        String id = "D" + (++figureSeq);
        Evidence e = new Evidence(id, Evidence.Kind.FIGURE, null, null, 0, 0, content, null);
        byId.put(id, e);
        return e;
    }

    public Evidence get(String id) {
        return byId.get(id);
    }

    // số liệu DB trước, rồi chunk theo thứ tự gán (thứ hạng rerank)
    public List<Evidence> all() {
        List<Evidence> figures = new ArrayList<>();
        List<Evidence> chunks = new ArrayList<>();
        byId.values().forEach(e -> (e.getKind() == Evidence.Kind.FIGURE ? figures : chunks).add(e));
        figures.addAll(chunks);
        return figures;
    }

    public boolean isEmpty() {
        return byId.isEmpty();
    }
}
