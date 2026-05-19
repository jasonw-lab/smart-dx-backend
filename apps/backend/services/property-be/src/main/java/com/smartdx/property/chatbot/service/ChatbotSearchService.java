package com.smartdx.property.chatbot.service;

import com.smartdx.property.chatbot.model.req.ChatbotSearchReq;
import com.smartdx.property.chatbot.model.vo.ChatbotSearchVO;

/**
 * Chatbot検索サービスインターフェース
 */
public interface ChatbotSearchService {

    /**
     * Chatbot検索を実行
     *
     * @param req 検索リクエスト
     * @return 検索結果
     */
    ChatbotSearchVO search(ChatbotSearchReq req);
}
