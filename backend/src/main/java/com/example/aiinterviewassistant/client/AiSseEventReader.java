package com.example.aiinterviewassistant.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class AiSseEventReader {

    private AiSseEventReader() {
    }

    static void read(InputStream inputStream, EventHandler eventHandler) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String eventName = null;
            StringBuilder data = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                ensureNotInterrupted();

                if (line.isEmpty()) {
                    dispatch(eventHandler, eventName, data);
                    eventName = null;
                    continue;
                }

                if (line.startsWith("event:")) {
                    eventName = removeOptionalLeadingSpace(line.substring("event:".length()));
                } else if (line.startsWith("data:")) {
                    if (data.length() > 0) {
                        data.append('\n');
                    }
                    data.append(removeOptionalLeadingSpace(line.substring("data:".length())));
                }
            }

            dispatch(eventHandler, eventName, data);
        }
    }

    private static void dispatch(
            EventHandler eventHandler,
            String eventName,
            StringBuilder data) throws IOException {
        if (data.length() == 0) {
            return;
        }

        eventHandler.onEvent(eventName, data.toString());
        data.setLength(0);
    }

    private static String removeOptionalLeadingSpace(String value) {
        return value.startsWith(" ") ? value.substring(1) : value;
    }

    private static void ensureNotInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new AiStreamCancelledException();
        }
    }

    @FunctionalInterface
    interface EventHandler {

        void onEvent(String eventName, String data) throws IOException;
    }
}
