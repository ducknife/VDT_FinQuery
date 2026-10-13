package com.vdt.finq.modules.agent.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// diễn đạt một lần gọi tool thành câu tiếng Việt cho giao diện
public final class ToolCallText {

    private ToolCallText() {
    }

    public static String describe(String name, String argsJson, ObjectMapper json) {
        JsonNode a;
        try {
            a = json.readTree(argsJson == null || argsJson.isBlank() ? "{}" : argsJson);
        } catch (Exception e) {
            return name + " " + argsJson;
        }
        return switch (name) {
            case "search_documents" -> "Tìm trong tài liệu: " + a.path("query").asText();
            case "get_financial_figures" -> "Tra số liệu: " + list(a.path("indicators")) + " · " + periods(a.path("periods"))
                    + " · " + (a.path("scopes").isMissingNode() ? "[CONSOLIDATED]" : list(a.path("scopes")));
            case "relate_two_figures" -> "Đối chiếu hai số (" + a.path("relation").asText() + "): " + ref(a.path("a")) + " với "
                    + ref(a.path("b"));
            case "compute_financial_ratio" -> "Tính tỷ số " + a.path("ratio").asText() + " · " + a.path("period").asText() + "/"
                    + a.path("year").asText() + " · " + a.path("scope").asText("CONSOLIDATED");
            case "decline_out_of_scope" -> "Từ chối: " + a.path("reason").asText();
            case "execute_sql" -> "Chạy SQL: " + a.path("query").asText();
            default -> name + " " + argsJson;
        };
    }

    private static String list(JsonNode arr) {
        StringBuilder s = new StringBuilder("[");
        for (JsonNode n : arr) {
            s.append(s.length() > 1 ? ", " : "").append(n.asText());
        }
        return s.append(']').toString();
    }

    private static String periods(JsonNode arr) {
        StringBuilder s = new StringBuilder();
        for (JsonNode p : arr) {
            s.append(s.length() > 0 ? ", " : "").append(p.path("period").asText()).append('/').append(p.path("year").asText());
        }
        return s.toString();
    }

    private static String ref(JsonNode r) {
        return r.path("indicator").asText() + " " + r.path("period").asText() + "/" + r.path("year").asText() + " "
                + r.path("scope").asText("CONSOLIDATED");
    }
}
