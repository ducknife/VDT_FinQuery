package com.vdt.finq.modules.rag.preretrieval.normalizer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.vdt.finq.common.text.VietnameseText;

import lombok.AllArgsConstructor;
import lombok.Getter;

// dữ liệu bộ chuẩn hoá, đọc từ resources/preretrieval/*.yml
@Getter
public final class NormalizationDictionary {

    @Getter
    @AllArgsConstructor
    public static class Abbreviation {
        // các cách viết, ví dụ ĐHĐCĐ và DHDCD
        private final List<String> forms;
        private final String fullForm;
        // từ ngữ cảnh (không dấu, chữ thường); rỗng nghĩa là luôn mở rộng
        private final List<String> context;
    }

    private final List<Abbreviation> abbreviations;
    // thuật ngữ có dấu, sắp dài trước ngắn sau để khớp cụm dài nhất
    private final List<String> terms;
    // mã công ty -> các cách gọi (không dấu)
    private final Map<String, List<String>> companies;
    private final String defaultCompany;

    private NormalizationDictionary(List<Abbreviation> abbreviations, List<String> terms,
            Map<String, List<String>> companies, String defaultCompany) {
        this.abbreviations = abbreviations;
        this.terms = terms;
        this.companies = companies;
        this.defaultCompany = defaultCompany;
    }

    public static NormalizationDictionary fromClasspath() {
        try (InputStream abbr = resource("preretrieval/abbreviations.yml");
                InputStream term = resource("preretrieval/terms.yml")) {
            return load(abbr, term);
        } catch (IOException e) {
            throw new IllegalStateException("không đọc được từ điển chuẩn hoá", e);
        }
    }

    @SuppressWarnings("unchecked")
    static NormalizationDictionary load(InputStream abbreviationYaml, InputStream termYaml) {
        Yaml yaml = new Yaml();
        Map<String, Object> abbr = yaml.load(abbreviationYaml);
        Map<String, Object> term = yaml.load(termYaml);

        List<Abbreviation> abbreviations = new ArrayList<>();
        for (Map<String, Object> e : (List<Map<String, Object>>) abbr.get("abbreviations")) {
            List<String> forms = ((List<Object>) e.get("forms")).stream().map(String::valueOf).toList();
            List<String> context = e.containsKey("context")
                    ? ((List<Object>) e.get("context")).stream().map(String::valueOf).toList()
                    : List.of();
            abbreviations.add(new Abbreviation(forms, String.valueOf(e.get("expansion")), context));
        }

        List<String> terms = new ArrayList<>(((List<Object>) term.get("terms")).stream().map(String::valueOf).toList());
        // dạng đầy đủ của viết tắt cũng là thuật ngữ để thêm dấu
        abbreviations.forEach(a -> terms.add(a.getFullForm().toLowerCase()));
        terms.sort(Comparator.comparingInt((String t) -> VietnameseText.fold(t).length()).reversed());

        Map<String, List<String>> companies = new LinkedHashMap<>();
        ((Map<String, List<Object>>) term.get("companies")).forEach((code, names) ->
                companies.put(code, names.stream().map(String::valueOf).toList()));
        return new NormalizationDictionary(List.copyOf(abbreviations), List.copyOf(terms.stream().distinct().toList()),
                companies, String.valueOf(term.get("default_company")));
    }

    private static InputStream resource(String path) {
        InputStream in = NormalizationDictionary.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("thiếu file " + path);
        }
        return in;
    }
}
