package com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.vdt.finq.common.exception.AppException;
import com.vdt.finq.modules.document.Document;
import com.vdt.finq.modules.document.DocumentRepository;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.DocumentPageReader;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.chunker.Chunk;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.chunker.Chunking;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.embedder.EmbeddedChunk;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.embedder.Embedding;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.vectordb.DocumentMetadata;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.vectordb.QdrantIndexWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// nạp một tài liệu đã OCR vào Qdrant: đọc trang -> làm sạch -> chunk -> embed -> ghi Qdrant -> ghi MySQL.
//
// bất biến cần giữ: ngay_embed khác NULL thì Qdrant chắc chắn có đủ point của tài liệu.
// mọi thứ tự trong indexDocument đều để giữ điều này khi lỗi xảy ra ở bất kỳ bước nào
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIndexingService {

    private final DocumentRepository documentRepository;
    private final DocumentPageReader pageReader;
    private final Chunking chunking;
    private final Embedding embedding;
    private final QdrantIndexWriter qdrantIndexWriter;

    // nạp lại một tài liệu (kể cả đã nạp rồi); trả về số chunk đã ghi
    public int indexDocument(String taiLieuId) throws IOException, InterruptedException {
        Document document = documentRepository.findById(taiLieuId)
                .orElseThrow(() -> new AppException(404, "không có tài liệu " + taiLieuId));
        if (document.getNgayOcr() == null) {
            throw new AppException(409, "tài liệu " + taiLieuId + " chưa OCR xong");
        }
        qdrantIndexWriter.ensureCollection();
        return index(document);
    }

    // nạp mọi tài liệu đã OCR mà chưa embed; một tài liệu lỗi không dừng cả lượt
    public IndexingSummary indexPending() {
        List<Document> pending = documentRepository.findByNgayOcrIsNotNullAndNgayEmbedIsNullOrderByTaiLieuId();
        log.info("nạp {} tài liệu chưa embed", pending.size());
        qdrantIndexWriter.ensureCollection();

        int succeeded = 0;
        int chunks = 0;
        List<String> failed = new ArrayList<>();
        for (int i = 0; i < pending.size(); i++) {
            Document document = pending.get(i);
            try {
                chunks += index(document);
                succeeded++;
            } catch (InterruptedException e) {
                // bị dừng thì thôi, các tài liệu chưa làm vẫn NULL, lần sau làm tiếp
                Thread.currentThread().interrupt();
                log.warn("bị ngắt ở tài liệu {}/{}", i + 1, pending.size());
                break;
            } catch (Exception e) {
                log.error("nạp {} lỗi: {}", document.getTaiLieuId(), e.getMessage(), e);
                failed.add(document.getTaiLieuId());
            }
            log.info("tiến độ {}/{}", i + 1, pending.size());
        }

        IndexingSummary summary = new IndexingSummary(pending.size(), succeeded, chunks, failed);
        log.info("xong lượt nạp: {}", summary);
        return summary;
    }

    private int index(Document document) throws IOException, InterruptedException {
        String taiLieuId = document.getTaiLieuId();
        long start = System.nanoTime();

        // 1. phần chậm và dễ lỗi (đọc file, gọi API) làm TRƯỚC, khi Qdrant còn nguyên bản cũ.
        //    embed lỗi thì dừng ở đây, tài liệu vẫn tìm được như trước
        List<String> pages = pageReader.readCleanPages(document);
        List<Chunk> chunks = chunking.chunk(taiLieuId, pages);
        if (chunks.isEmpty()) {
            throw new IllegalStateException(taiLieuId + " không có trang nào có chữ");
        }
        List<EmbeddedChunk> embedded = embedding.embedChunks(chunks, document.getTenHienThi());

        // 2. hạ cờ trước khi đụng Qdrant: nếu chết giữa lúc xoá và ghi,
        //    MySQL không còn nói "đã nạp" trong khi Qdrant đang thiếu
        documentRepository.updateEmbedState(taiLieuId, null, null);

        // 3. xoá point cũ (dọn phần thừa nếu số chunk giảm) rồi ghi mới, cả hai đều chờ xong
        qdrantIndexWriter.deleteDocument(taiLieuId);
        int written = qdrantIndexWriter.upsert(DocumentMetadata.from(document), embedded);

        // 4. đếm lại cho chắc rồi mới dựng cờ; lệch thì để NULL cho lần sau làm lại
        long counted = qdrantIndexWriter.countDocument(taiLieuId);
        if (counted != written) {
            throw new IllegalStateException(taiLieuId + ": ghi " + written + " point nhưng Qdrant có " + counted);
        }
        documentRepository.updateEmbedState(taiLieuId, LocalDateTime.now(), written);

        log.info("{}: {} trang, {} chunk, {} ms", taiLieuId, pages.size(), written,
                (System.nanoTime() - start) / 1_000_000);
        return written;
    }
}
