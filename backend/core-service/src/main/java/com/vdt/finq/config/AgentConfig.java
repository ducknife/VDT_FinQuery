package com.vdt.finq.config;

import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vdt.finq.modules.agent.tools.DeclineOutOfScope;
import com.vdt.finq.modules.agent.tools.SearchDocuments;
import com.vdt.finq.modules.dbquery.ComputeFinancialRatio;
import com.vdt.finq.modules.dbquery.GetFinancialFigures;
import com.vdt.finq.modules.dbquery.RelateTwoFigures;

// danh sách tool gửi LLM và bộ chạy tool; vòng lặp viết tay như VDT_AIOps
@Configuration
public class AgentConfig {

    @Bean
    public ToolCallingManager toolCallingManager() {
        return DefaultToolCallingManager.builder().build();
    }

    // nhánh tài liệu (search_documents), nhánh số liệu (3 tool SQL mẫu trong modules/dbquery), lối thoát
    @Bean
    public ToolCallback[] agentTools(SearchDocuments searchDocuments, GetFinancialFigures getFinancialFigures,
            RelateTwoFigures relateTwoFigures, ComputeFinancialRatio computeFinancialRatio,
            DeclineOutOfScope declineOutOfScope) {
        return ToolCallbacks.from(getFinancialFigures, relateTwoFigures, computeFinancialRatio, searchDocuments,
                declineOutOfScope);
    }
}
