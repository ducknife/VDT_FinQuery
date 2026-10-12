package com.vdt.finq.modules.hierarchicalrag.retrieval;

import static com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.vectordb.QdrantConstants.KY;
import static com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.vectordb.QdrantConstants.LOAI;
import static com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.vectordb.QdrantConstants.MA_CONG_TY;
import static com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.vectordb.QdrantConstants.NAM;
import static com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.vectordb.QdrantConstants.PHAM_VI;
import static io.qdrant.client.ConditionFactory.filter;
import static io.qdrant.client.ConditionFactory.isEmpty;
import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.ConditionFactory.matchKeywords;
import static io.qdrant.client.ConditionFactory.matchValues;

import java.util.List;

import com.vdt.finq.modules.document.DocumentEnums.Ky;
import com.vdt.finq.modules.document.DocumentEnums.LoaiTaiLieu;
import com.vdt.finq.modules.hierarchicalrag.preretrieval.filter.QueryFilter;

import io.qdrant.client.grpc.Points.Condition;
import io.qdrant.client.grpc.Points.Filter;

// QueryFilter -> Filter của Qdrant. mọi trường ở đây đều có payload index (QdrantIndexWriter)
public final class QdrantFilters {

    private QdrantFilters() {
    }

    // lọc chặt: công ty AND năm AND (kỳ OR không có kỳ) AND (phạm vi OR không có phạm vi) AND loại.
    // năm: (năm báo cáo định kỳ) OR (không có kỳ AND năm văn bản sự kiện); hai danh sách do FilterExtractor tính
    public static Filter strict(QueryFilter q) {
        Filter.Builder b = Filter.newBuilder().addMust(matchKeyword(MA_CONG_TY, q.getMaCongTy()));
        if (!q.getNams().isEmpty()) {
            Filter eventDocs = Filter.newBuilder().addMust(isEmpty(KY))
                    .addMust(matchValues(NAM, longs(q.getEventNams()))).build();
            b.addMust(filter(Filter.newBuilder()
                    .addShould(matchValues(NAM, longs(q.getPeriodNams())))
                    .addShould(filter(eventDocs))
                    .build()));
        }
        if (!q.getKys().isEmpty()) {
            b.addMust(orMissing(KY, matchKeywords(KY, q.getKys().stream().map(Ky::getValue).toList())));
        }
        if (q.getPhamVi() != null) {
            b.addMust(orMissing(PHAM_VI, matchKeyword(PHAM_VI, q.getPhamVi().getValue())));
        }
        if (!q.getLoais().isEmpty()) {
            b.addMust(matchKeywords(LOAI, q.getLoais().stream().map(LoaiTaiLieu::getValue).toList()));
        }
        return b.build();
    }

    // lọc nới: chỉ giữ công ty, dùng khi lọc chặt ra quá ít
    public static Filter relaxed(QueryFilter q) {
        return Filter.newBuilder().addMust(matchKeyword(MA_CONG_TY, q.getMaCongTy())).build();
    }

    private static List<Long> longs(List<Integer> values) {
        return values.stream().map(Integer::longValue).toList();
    }

    // khớp điều kiện HOẶC tài liệu không có trường này (nghị quyết không có kỳ, bctn không có phạm vi)
    private static Condition orMissing(String field, Condition condition) {
        return filter(Filter.newBuilder().addAllShould(List.of(condition, isEmpty(field))).build());
    }
}
