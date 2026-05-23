package com.smartdx.property.chatbot.controller;

import com.smartdx.core.result.Result;
import com.smartdx.property.chatbot.model.req.ChatbotSearchReq;
import com.smartdx.property.chatbot.model.vo.ChatbotSearchVO;
import com.smartdx.property.chatbot.service.ChatbotSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chatbot検索コントローラー
 *
 * POST /api/v1/properties/chatbot/search
 */
@Tag(name = "Chatbot検索")
@RestController
@RequestMapping("/api/v1/properties/chatbot")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ChatbotSearchController {

    private final ChatbotSearchService chatbotSearchService;

    @Operation(
            summary = "Chatbot検索API",
            description = "自然言語入力から検索条件を抽出し、物件検索を実行する"
    )
    @PostMapping("/search")
    public Result<ChatbotSearchVO> search(@Valid @RequestBody ChatbotSearchReq req) {
        log.info("API call start: POST /api/v1/properties/chatbot/search. messageLength={}",
                req.getMessage().length());

        ChatbotSearchVO result = chatbotSearchService.search(req);

        log.info("API call end: POST /api/v1/properties/chatbot/search. method={}, llmUsed={}, resultCount={}",
                result.getExtractionMethod(),
                result.isLlmUsed(),
                result.getSearchResults() != null ? result.getSearchResults().getTotal() : 0);

        return Result.success(result);
    }
}
