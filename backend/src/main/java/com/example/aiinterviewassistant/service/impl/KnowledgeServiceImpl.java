package com.example.aiinterviewassistant.service.impl;

import com.example.aiinterviewassistant.dto.KnowledgeTopicResponse;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.service.KnowledgeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private final List<KnowledgeTopicResponse> topics = List.of(
            topic(1L, "backend", "Java", "JVM", "JVM 运行时内存区域",
                    "理解程序运行期间各内存区域的职责，是分析 Java 性能和内存问题的基础。",
                    List.of("程序计数器记录当前线程执行位置。", "虚拟机栈保存方法调用和局部变量。", "堆是对象实例和数组的主要分配区域。"),
                    "JVM 的堆和栈有什么区别？",
                    "堆用于存放对象实例，生命周期通常由垃圾回收器管理；栈以线程为单位保存方法调用、局部变量和操作数栈，方法结束后对应栈帧会被释放。"),
            topic(2L, "backend", "Java", "Spring", "Spring Bean 生命周期",
                    "掌握 Bean 的创建、依赖注入、初始化和销毁过程。",
                    List.of("实例化后进行属性注入。", "执行 Aware 接口和 BeanPostProcessor 回调。", "容器关闭时执行销毁回调。"),
                    "BeanPostProcessor 的作用是什么？",
                    "它允许在 Bean 初始化前后对实例进行扩展处理，AOP 代理、自动注入等能力都可以基于这类扩展点实现。"),
            topic(3L, "backend", "Python", "FastAPI", "FastAPI 依赖注入",
                    "依赖注入用于复用认证、数据库连接和公共业务逻辑。",
                    List.of("使用 Depends 声明依赖。", "依赖可以继续依赖其他依赖。", "依赖结果会按请求范围复用。"),
                    "FastAPI 依赖注入适合解决什么问题？",
                    "它适合统一处理认证、权限、数据库会话和参数预处理，让路由函数只关注当前业务。"),
            topic(4L, "backend", "Go", "并发编程", "Goroutine 与 Channel",
                    "Go 使用轻量级 Goroutine 和 Channel 组织并发任务。",
                    List.of("Goroutine 由运行时调度。", "Channel 用于协程之间通信。", "WaitGroup 可等待一组任务完成。"),
                    "为什么不能只依赖共享变量实现并发通信？",
                    "共享变量容易产生竞态条件；使用 Channel 传递数据或使用互斥锁保护临界区，可以明确并发访问边界。"),
            topic(5L, "frontend", "TypeScript", "类型系统", "TypeScript 类型收窄",
                    "类型收窄让联合类型在分支中获得更准确的类型信息。",
                    List.of("typeof 可识别基础类型。", "in 可判断对象是否包含属性。", "自定义类型守卫可以复用复杂判断。"),
                    "类型收窄解决了什么问题？",
                    "它把宽泛的联合类型缩小到当前分支真实可用的类型，从而减少断言并让编译器发现更多错误。"),
            topic(6L, "frontend", "Vue", "Composition API", "Vue 响应式原理",
                    "Vue 通过响应式系统追踪状态读取和更新。",
                    List.of("ref 适合包装单值。", "reactive 适合响应式对象。", "computed 用于派生状态，watch 用于响应变化执行副作用。"),
                    "computed 和 watch 应该如何选择？",
                    "需要得到派生值时使用 computed；需要在变化后执行请求、日志或其他副作用时使用 watch。"),
            topic(7L, "frontend", "React", "Hooks", "React Hooks 使用原则",
                    "Hooks 让函数组件能够使用状态和生命周期能力。",
                    List.of("Hooks 只能在组件或自定义 Hook 顶层调用。", "依赖数组需要反映闭包使用的外部值。", "复杂逻辑应提取为自定义 Hook。"),
                    "为什么 Hooks 不能放在条件分支中？",
                    "React 依赖 Hooks 的调用顺序匹配状态。如果调用顺序在不同渲染之间变化，状态就会对应错误。")
    );

    @Override
    public List<KnowledgeTopicResponse> getTopics(String direction, String language) {
        return topics.stream()
                .filter(topic -> topic.direction().equals(direction) && topic.language().equals(language))
                .toList();
    }

    @Override
    public KnowledgeTopicResponse getTopic(Long topicId) {
        return topics.stream()
                .filter(topic -> topic.id().equals(topicId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(404, "知识专题不存在"));
    }

    private static KnowledgeTopicResponse topic(
            Long id, String direction, String language, String category, String title,
            String summary, List<String> keyPoints, String question, String answer) {
        return new KnowledgeTopicResponse(
                id, direction, language, category, title, summary, keyPoints,
                List.of(new KnowledgeTopicResponse.KnowledgeQuestion(question, answer))
        );
    }
}
