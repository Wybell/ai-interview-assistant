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

import java.util.List;

@Service
public class AiService {

    private static final String CHINESE_OUTPUT_RULE = "始终使用简体中文输出。技术名词、类名、方法名、命令和代码可以保留英文；"
            + "除这些必要术语外，不要使用英文完整句子。";
    private static final String SINGLE_QUESTION_RULE = "只输出一道面试题，不能输出题号列表、第二道题、答案、解析、前言或结尾说明。";

    private static final String QUESTION_SYSTEM_PROMPT = "你是一位严格的技术面试官。"
            + CHINESE_OUTPUT_RULE
            + "请根据用户指定的知识点生成一道面试题。"
            + "只输出题目，不要输出答案或解释。";

    private static final String SCORE_SYSTEM_PROMPT = "你是一位技术面试官，请对候选人的回答进行 0 到 10 分评分，"
            + CHINESE_OUTPUT_RULE
            + "并给出标准答案要点和改进建议。"
            + "必须严格返回 JSON，格式如下："
            + "{\"score\": 8, "
            + "\"correct_answer\": \"答案要点\", "
            + "\"suggestion\": \"改进建议\"}"
            + "只返回一个 JSON 对象，不要使用 Markdown 代码围栏或添加其他文本。";

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
                "你是一位严格的" + language + " " + direction + "技术面试官。"
                        + CHINESE_OUTPUT_RULE + "只输出一道面试题。",
                "面试方向：" + direction + "\n语言或技术栈：" + language + "\n知识点：" + tag
        );
    }

    public String generateQuestion(
            EffectiveAiModel aiModel,
            String direction,
            String language,
            String tag,
            KnowledgeContext knowledgeContext) {
        return generateQuestion(aiModel, direction, language, tag, knowledgeContext, List.of());
    }

    public String generateQuestion(
            EffectiveAiModel aiModel,
            String direction,
            String language,
            String tag,
            KnowledgeContext knowledgeContext,
            List<String> previousQuestions) {
        String sourceInstruction = knowledgeContext == null
                ? "根据指定知识点生成一道技术面试题。"
                : "只能依据提供的知识库专题文档生成题目，不得编造文档中没有的事实。";
        String userContent = "面试方向：" + direction
                + "\n语言或技术栈：" + language
                + "\n知识点或专题：" + tag;
        if (knowledgeContext != null) {
            userContent += "\n知识库专题：" + knowledgeContext.title()
                    + "\n知识库文档内容：\n" + knowledgeContext.content();
            String previousQuestionText = formatPreviousQuestions(previousQuestions);
            if (!previousQuestionText.isBlank()) {
                userContent += "\n该专题已经生成过的题目（请优先生成未出现过的新题；只有确实无法继续提出合理新题时才允许重复）：\n"
                        + previousQuestionText;
            }
        }
        String questionText = generate(
                aiModel,
                "你是一位严格的" + language + " " + direction + "技术面试官。"
                        + CHINESE_OUTPUT_RULE + sourceInstruction + SINGLE_QUESTION_RULE,
                userContent
        );
        return knowledgeContext == null ? questionText : normalizeKnowledgeQuestion(questionText);
    }

    private String formatPreviousQuestions(List<String> previousQuestions) {
        if (previousQuestions == null || previousQuestions.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        int questionNumber = 1;
        for (String question : previousQuestions) {
            if (question == null || question.isBlank()) {
                continue;
            }
            String normalized = question.trim().replaceAll("\\s+", " ");
            if (normalized.length() > 300) {
                normalized = normalized.substring(0, 300);
            }
            result.append(questionNumber++).append(". ").append(normalized).append('\n');
            if (result.length() >= 6_000) {
                break;
            }
        }
        return result.toString().trim();
    }

    public String generateMockInterviewQuestion(
            EffectiveAiModel aiModel,
            String resumeContent,
            String targetPosition,
            String targetCompany,
            String interviewRound,
            String previousTranscript) {
        String roundFocus = switch (interviewRound) {
            case "FIRST" -> "重点确认简历经历、职责边界、基础能力和沟通表达。";
            case "SECOND" -> "重点追问项目细节、技术取舍、问题排查和技术深度。";
            case "THIRD" -> "重点考察系统设计、业务判断、协作、责任意识和决策能力。";
            default -> throw new BusinessException(400, "Interview round is invalid");
        };
        String content = "Target position: " + targetPosition
                + "\nTarget company: " + companyContext(targetCompany)
                + "\nInterview round: " + interviewRound
                + "\nResume:\n" + limitText(resumeContent, 12_000)
                + "\nPrevious turns:\n" + limitText(previousTranscript, 8_000);
        return generate(
                aiModel,
                "你是一位经验丰富的中文技术面试官。" + CHINESE_OUTPUT_RULE + roundFocus
                        + "请只提出一个简洁的中文问题。只能使用简历中明确写出的事实；"
                        + "可以提出通用岗位相关问题，但不得编造候选人的经历。只输出问题。",
                content
        );
    }

    public String generateMockInterviewSummary(
            EffectiveAiModel aiModel,
            String targetPosition,
            String targetCompany,
            String interviewRound,
            String transcript) {
        return generate(
                aiModel,
                "你是一位经验丰富的中文技术面试官。" + CHINESE_OUTPUT_RULE + "请用简洁中文写一份面试报告，"
                        + "包含整体表现、优点、薄弱点、项目追问点和三条准备建议。"
                        + "不得编造面试记录中没有的细节。",
                "Target position: " + targetPosition + "\nTarget company: " + companyContext(targetCompany)
                        + "\nInterview round: " + interviewRound
                        + "\nInterview transcript:\n" + limitText(transcript, 16_000)
        );
    }

    private String companyContext(String targetCompany) {
        if (targetCompany == null || targetCompany.isBlank()) {
            return "未提供；按通用岗位面试流程进行。";
        }
        return targetCompany.trim()
                + "。仅作为公司风格模拟语境，可参考公开、常见的招聘侧重点；"
                + "不得声称掌握该公司的真实题库、真实面试流程或内部信息。";
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

    private String normalizeKnowledgeQuestion(String questionText) {
        if (questionText == null || questionText.isBlank()) {
            return questionText;
        }

        String normalized = questionText.replace("```", "").trim()
                .replaceFirst("^(题目|问题|Question)\\s*[:：]\\s*", "");
        StringBuilder firstQuestion = new StringBuilder();
        for (String line : normalized.split("\\R")) {
            String currentLine = line.trim();
            if (currentLine.isEmpty()) {
                continue;
            }
            if (firstQuestion.length() > 0 && currentLine.matches(
                    "^(第?[二三四五六七八九十]?[题问]|[2-9][.、)]|答案|解析)[:：.、)]?.*")) {
                break;
            }
            currentLine = currentLine.replaceFirst("^1[.、)]\\s*", "");
            if (firstQuestion.length() > 0) {
                firstQuestion.append(' ');
            }
            firstQuestion.append(currentLine);
        }

        String result = firstQuestion.length() > 0 ? firstQuestion.toString().trim() : normalized;
        int chineseQuestionMark = result.indexOf('？');
        int englishQuestionMark = result.indexOf('?');
        int questionMark = chineseQuestionMark >= 0 && englishQuestionMark >= 0
                ? Math.min(chineseQuestionMark, englishQuestionMark)
                : Math.max(chineseQuestionMark, englishQuestionMark);
        return questionMark > 0 ? result.substring(0, questionMark + 1).trim() : result;
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
