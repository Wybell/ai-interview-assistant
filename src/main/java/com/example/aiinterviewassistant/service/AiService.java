package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.client.AiClientRegistry;
import com.example.aiinterviewassistant.client.AiTextDeltaConsumer;
import com.example.aiinterviewassistant.dto.AiScoreResult;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.model.EffectiveAiModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private static final String QUESTION_SYSTEM_PROMPT = "你是一位严格的 Java 后端技术面试官。"
            + "请根据用户指定的知识点生成一道面试题。"
            + "只输出题目，不要输出答案或解释。";

    private static final String SCORE_SYSTEM_PROMPT = "你是一位 Java 面试官，请对候选人的回答进行 0 到 10 分评分，"
            + "并给出标准答案要点和改进建议。"
            + "必须严格返回 JSON，格式如下："
            + "{\"score\": 8, "
            + "\"correct_answer\": \"答案要点\", "
            + "\"suggestion\": \"改进建议\"}";

    private final AiClientRegistry aiClientRegistry;
    private final ObjectMapper objectMapper;

    public AiService(AiClientRegistry aiClientRegistry, ObjectMapper objectMapper) {
        this.aiClientRegistry = aiClientRegistry;
        this.objectMapper = objectMapper;
    }

    public String generateQuestion(EffectiveAiModel aiModel, String tag) {
        return generate(
                aiModel,
                QUESTION_SYSTEM_PROMPT,
                "知识点：" + tag
        );
    }

    public AiScoreResult scoreAnswer(EffectiveAiModel aiModel, String question, String answer) {
        String resultText = generate(
                aiModel,
                SCORE_SYSTEM_PROMPT,
                "面试问题：" + question + "\n候选人回答：" + answer
        );

        return parseScoreResult(resultText);
    }

    public AiScoreResult streamScoreAnswer(
            EffectiveAiModel aiModel,
            String question,
            String answer,
            AiTextDeltaConsumer deltaConsumer) {
        String resultText = generateStream(
                aiModel,
                SCORE_SYSTEM_PROMPT,
                "面试问题：" + question + "\n候选人回答：" + answer,
                deltaConsumer
        );

        return parseScoreResult(resultText);
    }

    private AiScoreResult parseScoreResult(String resultText) {
        try {
            JsonNode scoreNode = objectMapper.readTree(resultText);
            if (!scoreNode.has("score")
                    || !scoreNode.has("correct_answer")
                    || !scoreNode.has("suggestion")) {
                throw new BusinessException(502, "AI 评分结果格式错误");
            }

            int score = scoreNode.get("score").asInt();
            if (score < 0 || score > 10) {
                throw new BusinessException(502, "AI 评分结果超出范围");
            }

            return new AiScoreResult(
                    score,
                    scoreNode.get("correct_answer").asText(),
                    scoreNode.get("suggestion").asText()
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(502, "AI 评分结果解析失败");
        }
    }

    private String generate(
            EffectiveAiModel aiModel,
            String systemPrompt,
            String userContent) {
        return aiClientRegistry.generate(
                aiModel.provider(),
                aiModel.modelCode(),
                systemPrompt,
                userContent
        );
    }

    private String generateStream(
            EffectiveAiModel aiModel,
            String systemPrompt,
            String userContent,
            AiTextDeltaConsumer deltaConsumer) {
        return aiClientRegistry.generateStream(
                aiModel.provider(),
                aiModel.modelCode(),
                systemPrompt,
                userContent,
                deltaConsumer
        );
    }
}
