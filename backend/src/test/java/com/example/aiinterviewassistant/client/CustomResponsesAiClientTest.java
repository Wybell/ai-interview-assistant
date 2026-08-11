package com.example.aiinterviewassistant.client;

import com.example.aiinterviewassistant.config.AiProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CustomResponsesAiClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldSendResponsesCompatibleQuestionRequest() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/responses", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"output_text\":\"generated question\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        AiProperties properties = new AiProperties();
        properties.setCustomApiKey("test-key");
        properties.setCustomEndpoint("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/responses");
        properties.setCustomReasoningEffort("high");
        properties.setCustomDisableResponseStorage(true);
        CustomResponsesAiClient client = new CustomResponsesAiClient(
                properties,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                HttpClient.newHttpClient());

        String result = client.generate("gpt-5.6-luna", "system instruction", "user input");

        assertThat(result).isEqualTo("generated question");
        assertThat(requestBody.get())
                .contains("\"model\":\"gpt-5.6-luna\"")
                .contains("\"instructions\":\"system instruction\"")
                .contains("\"input\":\"user input\"")
                .contains("\"store\":false")
                .contains("\"reasoning\":{\"effort\":\"high\"}");
    }
}
