package com.example.aiinterviewassistant.service;

import com.example.aiinterviewassistant.client.AiClientRegistry;
import com.example.aiinterviewassistant.client.AiTextDeltaConsumer;
import com.example.aiinterviewassistant.dto.AiScoreResult;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.model.EffectiveAiModel;
import com.example.aiinterviewassistant.model.KnowledgeContext;
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
            + "\"suggestion\": \"改进建议\"}"
            + " Return exactly one JSON object. Do not use Markdown code fences or add any other text.";

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

    public String generateQuestion(
            EffectiveAiModel aiModel,
            String direction,
            String language,
            String tag) {
        return generate(
                aiModel,
                "你是一位严格的" + language + " " + direction + "技术面试官，只输出一道面试题。",
                "面试方向：" + direction + "\n语言或技术栈：" + language + "\n知识点：" + tag
        );
    }

    public String generateQuestion(
            EffectiveAiModel aiModel,
            String direction,
            String language,
            String tag,
            KnowledgeContext knowledgeContext) {
        String sourceInstruction = knowledgeContext == null
                ? "Generate one question for the requested topic."
                : "Generate one question grounded only in the supplied knowledge-base material. Do not claim facts absent from it.";
        String userContent = "Interview direction: " + direction
                + "\nLanguage or technology: " + language
                + "\nTopic: " + tag;
        if (knowledgeContext != null) {
            userContent += "\nKnowledge-base topic: " + knowledgeContext.title()
                    + "\nKnowledge-base material:\n" + knowledgeContext.content();
        }
        return generate(
                aiModel,
                "You are a rigorous " + language + " " + direction
                        + " technical interviewer. " + sourceInstruction + " Output only the question.",
                userContent
        );
    }

    public String generateMockInterviewQuestion(
            EffectiveAiModel aiModel,
            String resumeContent,
            String targetPosition,
            String interviewRound,
            String previousTranscript) {
        String roundFocus = switch (interviewRound) {
            case "FIRST" -> "Verify resume experience, role ownership, core fundamentals, and communication.";
            case "SECOND" -> "Probe project details, technical trade-offs, debugging, and the candidate's depth.";
            case "THIRD" -> "Probe system design, business judgment, collaboration, ownership, and decision making.";
            default -> throw new BusinessException(400, "Interview round is invalid");
        };
        String content = "Target position: " + targetPosition
                + "\nInterview round: " + interviewRound
                + "\nResume:\n" + limitText(resumeContent, 12_000)
                + "\nPrevious turns:\n" + limitText(previousTranscript, 8_000);
        return generate(
                aiModel,
                "You are an experienced Chinese technical interviewer. " + roundFocus
                        + " Ask exactly one concise question in Chinese. Use only facts stated in the resume; "
                        + "you may ask general role-relevant questions but must not invent experience. Output only the question.",
                content
        );
    }

    public String generateMockInterviewSummary(
            EffectiveAiModel aiModel,
            String targetPosition,
            String interviewRound,
            String transcript) {
        return generate(
                aiModel,
                "You are an experienced Chinese technical interviewer. Write a concise Chinese interview report "
                        + "with average performance, strengths, gaps, project follow-up points, and three preparation suggestions. "
                        + "Do not invent details absent from the interview transcript.",
                "Target position: " + targetPosition + "\nInterview round: " + interviewRound
                        + "\nInterview transcript:\n" + limitText(transcript, 16_000)
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
            JsonNode scoreNode = objectMapper.readTree(extractFirstJsonObject(resultText));
            validateScoreResult(scoreNode);

            int score = scoreNode.get("score").intValue();
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

    private String limitText(String content, int maxLength) {
        if (content == null || content.isBlank()) {
            return "(none)";
        }
        String normalized = content.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private void validateScoreResult(JsonNode scoreNode) {
        if (scoreNode == null
                || !scoreNode.isObject()
                || !scoreNode.has("score")
                || !scoreNode.has("correct_answer")
                || !scoreNode.has("suggestion")) {
            throw new BusinessException(502, "AI 评分结果格式错误");
        }

        JsonNode score = scoreNode.get("score");
        JsonNode correctAnswer = scoreNode.get("correct_answer");
        JsonNode suggestion = scoreNode.get("suggestion");
        if (!score.isIntegralNumber()
                || !score.canConvertToInt()
                || !correctAnswer.isTextual()
                || !suggestion.isTextual()) {
            throw new BusinessException(502, "AI 评分结果格式错误");
        }
    }

    private String extractFirstJsonObject(String resultText) {
        if (resultText == null || resultText.isBlank()) {
            throw new BusinessException(502, "AI 评分结果解析失败");
        }

        int objectStart = resultText.indexOf('{');
        if (objectStart < 0) {
            throw new BusinessException(502, "AI 评分结果解析失败");
        }

        boolean inString = false;
        boolean escaped = false;
        int objectDepth = 0;
        for (int index = objectStart; index < resultText.length(); index++) {
            char currentCharacter = resultText.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (currentCharacter == '\\') {
                    escaped = true;
                } else if (currentCharacter == '"') {
                    inString = false;
                }
                continue;
            }

            if (currentCharacter == '"') {
                inString = true;
            } else if (currentCharacter == '{') {
                objectDepth++;
            } else if (currentCharacter == '}') {
                objectDepth--;
                if (objectDepth == 0) {
                    return resultText.substring(objectStart, index + 1);
                }
            }
        }

        throw new BusinessException(502, "AI 评分结果解析失败");
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
