package com.example.aiinterviewassistant.client;

public class AiStreamCancelledException extends RuntimeException {

    public AiStreamCancelledException() {
        super("AI stream was cancelled");
    }

    public AiStreamCancelledException(Throwable cause) {
        super("AI stream was cancelled", cause);
    }
}
