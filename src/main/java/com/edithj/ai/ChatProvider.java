package com.edithj.ai;

public interface ChatProvider {

    String providerName();

    String generateReply(String prompt);
}
