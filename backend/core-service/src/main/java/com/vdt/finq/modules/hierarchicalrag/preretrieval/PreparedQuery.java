package com.vdt.finq.modules.hierarchicalrag.preretrieval;

import java.util.List;

import com.vdt.finq.modules.hierarchicalrag.preretrieval.filter.QueryFilter;
import com.vdt.finq.modules.hierarchicalrag.preretrieval.normalizer.NormalizedQuery;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

// kết quả tiền truy xuất: Q' = chuẩn hoá ∪ từ khoá, kèm bộ lọc
@Getter
@ToString
@AllArgsConstructor
public class PreparedQuery {
    private final NormalizedQuery normalized;
    private final List<String> keywords;
    // Q': câu đem embed và đem rerank
    private final String text;
    private final QueryFilter filter;
}
