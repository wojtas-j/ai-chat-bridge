package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.entity.MessageEntity;

public interface OpenAIService {
    MessageEntity sendMessageToOpenAI(MessageEntity message);
}
