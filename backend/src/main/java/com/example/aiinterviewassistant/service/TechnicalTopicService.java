package com.example.aiinterviewassistant.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TechnicalTopicService {

    private static final Map<String, List<String>> TOPICS = Map.ofEntries(
            Map.entry("frontend:JavaScript", List.of("JavaScript 基础", "异步编程", "DOM 与事件", "浏览器原理")),
            Map.entry("frontend:TypeScript", List.of("类型系统", "泛型", "类型体操", "工程配置")),
            Map.entry("frontend:Vue", List.of("组件通信", "Composition API", "响应式原理", "Vue Router")),
            Map.entry("frontend:React", List.of("组件设计", "Hooks", "状态管理", "React 性能优化")),
            Map.entry("backend:Java", List.of("集合框架", "并发编程", "JVM", "Spring", "MySQL", "Redis")),
            Map.entry("backend:Python", List.of("Python 基础", "异步编程", "FastAPI", "Django")),
            Map.entry("backend:Go", List.of("Goroutine", "Channel", "Gin", "服务并发")),
            Map.entry("backend:C#", List.of("C# 基础", ".NET", "ASP.NET Core", "Entity Framework")),
            Map.entry("backend:Node.js", List.of("事件循环", "Express", "NestJS", "Node.js 性能")),
            Map.entry("backend:TypeScript", List.of("Node.js 类型开发", "NestJS", "异步编程", "服务架构"))
    );

    public List<String> getTopics(String direction, String language) {
        return TOPICS.getOrDefault(key(direction, language), List.of());
    }

    public boolean isSupported(String direction, String language, String topic) {
        return topic != null && getTopics(direction, language).contains(topic.trim());
    }

    private String key(String direction, String language) {
        return direction + ":" + language;
    }
}
