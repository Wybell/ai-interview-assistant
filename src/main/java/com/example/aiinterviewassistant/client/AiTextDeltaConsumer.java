package com.example.aiinterviewassistant.client;

@FunctionalInterface
public interface AiTextDeltaConsumer {

    void onDelta(String text);
}
