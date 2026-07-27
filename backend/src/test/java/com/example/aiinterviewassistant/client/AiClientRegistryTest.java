package com.example.aiinterviewassistant.client;

import com.example.aiinterviewassistant.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AiClientRegistryTest {

    @Mock
    private AiClient deepSeekClient;

    @Mock
    private AiClient change2ProClient;

    private AiClientRegistry aiClientRegistry;

    @BeforeEach
    void setUp() {
        when(deepSeekClient.provider()).thenReturn("deepseek");
        when(change2ProClient.provider()).thenReturn("change2proapi");
        aiClientRegistry = new AiClientRegistry(List.of(deepSeekClient, change2ProClient));
        clearInvocations(deepSeekClient, change2ProClient);
    }

    @Test
    void shouldRouteRequestedProviderAndModelCode() {
        when(deepSeekClient.isConfigured()).thenReturn(true);
        when(deepSeekClient.generate("deepseek-v4-flash", "system prompt", "user content"))
                .thenReturn("generated text");

        String actual = aiClientRegistry.generate(
                "deepseek",
                "deepseek-v4-flash",
                "system prompt",
                "user content"
        );

        assertThat(actual).isEqualTo("generated text");
        verify(deepSeekClient).generate("deepseek-v4-flash", "system prompt", "user content");
    }

    @Test
    void shouldNormalizeProviderAndTrimModelCode() {
        when(change2ProClient.isConfigured()).thenReturn(true);
        when(change2ProClient.generate("gpt-5.6-luna", "system prompt", "user content"))
                .thenReturn("generated text");

        String actual = aiClientRegistry.generate(
                " CHANGE2PROAPI ",
                " gpt-5.6-luna ",
                "system prompt",
                "user content"
        );

        assertThat(actual).isEqualTo("generated text");
        verify(change2ProClient).generate(
                "gpt-5.6-luna",
                "system prompt",
                "user content"
        );
    }

    @Test
    void shouldRouteStreamingRequestToRequestedProviderAndModelCode() {
        List<String> deltas = new ArrayList<>();
        when(deepSeekClient.isConfigured()).thenReturn(true);
        when(deepSeekClient.generateStream(
                org.mockito.ArgumentMatchers.eq("deepseek-v4-flash"),
                org.mockito.ArgumentMatchers.eq("system prompt"),
                org.mockito.ArgumentMatchers.eq("user content"),
                any(AiTextDeltaConsumer.class)
        )).thenAnswer(invocation -> {
            AiTextDeltaConsumer deltaConsumer = invocation.getArgument(3);
            deltaConsumer.onDelta("generated ");
            deltaConsumer.onDelta("text");
            return "generated text";
        });

        String actual = aiClientRegistry.generateStream(
                "deepseek",
                "deepseek-v4-flash",
                "system prompt",
                "user content",
                deltas::add
        );

        assertThat(actual).isEqualTo("generated text");
        assertThat(deltas).containsExactly("generated ", "text");
        verify(deepSeekClient).generateStream(
                org.mockito.ArgumentMatchers.eq("deepseek-v4-flash"),
                org.mockito.ArgumentMatchers.eq("system prompt"),
                org.mockito.ArgumentMatchers.eq("user content"),
                any(AiTextDeltaConsumer.class)
        );
    }

    @Test
    void shouldRejectUnknownProviderWithoutCallingAClient() {
        assertThatThrownBy(() -> aiClientRegistry.generate(
                "unknown",
                "unknown-model",
                "system prompt",
                "user content"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(503);

        verifyNoInteractions(deepSeekClient, change2ProClient);
    }

    @Test
    void shouldRejectBlankModelCodeWithoutCallingAClient() {
        assertThatThrownBy(() -> aiClientRegistry.generate(
                "deepseek",
                " ",
                "system prompt",
                "user content"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(503);

        verifyNoInteractions(deepSeekClient, change2ProClient);
    }

    @Test
    void shouldRejectProviderWithIncompleteRuntimeConfiguration() {
        assertThat(aiClientRegistry.isModelAvailable("deepseek", "deepseek-v4-flash")).isFalse();
        assertThatThrownBy(() -> aiClientRegistry.generate(
                "deepseek",
                "deepseek-v4-flash",
                "system prompt",
                "user content"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(503);

        verify(deepSeekClient, never()).generate("deepseek-v4-flash", "system prompt", "user content");
    }

    @Test
    void shouldHideProviderConfigurationErrors() {
        when(deepSeekClient.isConfigured()).thenReturn(true);
        when(deepSeekClient.generate("deepseek-v4-flash", "system prompt", "user content"))
                .thenThrow(new BusinessException(500, "provider configuration missing"));

        assertThatThrownBy(() -> aiClientRegistry.generate(
                "deepseek",
                "deepseek-v4-flash",
                "system prompt",
                "user content"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(503);
    }

    @Test
    void shouldRejectDuplicateProviderClientsAtStartup() {
        AiClient duplicateClient = org.mockito.Mockito.mock(AiClient.class);
        when(duplicateClient.provider()).thenReturn("deepseek");

        assertThatThrownBy(() -> new AiClientRegistry(List.of(deepSeekClient, duplicateClient)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Duplicate AI client provider: deepseek");
    }
}
