package com.example.aiinterviewassistant.service.impl;

import com.example.aiinterviewassistant.dto.KnowledgeTopicResponse;
import com.example.aiinterviewassistant.exception.BusinessException;
import com.example.aiinterviewassistant.service.KnowledgeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private final List<KnowledgeTopicResponse> topics = buildTopics();

    @Override
    public List<KnowledgeTopicResponse> getTopics(String direction, String language) {
        return topics.stream()
                .filter(topic -> topic.direction().equals(direction) && topic.language().equals(language))
                .toList();
    }

    @Override
    public KnowledgeTopicResponse getTopic(Long topicId) {
        return topics.stream().filter(topic -> topic.id().equals(topicId)).findFirst()
                .orElseThrow(() -> new BusinessException(404, "知识专题不存在"));
    }

    private static List<KnowledgeTopicResponse> buildTopics() {
        List<KnowledgeTopicResponse> result = new ArrayList<>();
        long[] id = {1};
        addLanguage(result, id, "backend", "Java", new String[][]{
                {"Java 基础与面向对象", "覆盖对象模型、String、异常和泛型，是排查基础题与设计题的必备知识。", "封装、继承、多态和组合的适用边界", "String 不可变性与常量池", "泛型擦除和异常处理", "为什么推荐组合优于继承？", "组合通过持有协作对象复用能力，耦合更低、替换更容易；继承适合稳定的 is-a 关系，但会暴露父类实现并扩大变更影响。", "Java 泛型为什么在运行期看不到具体类型？", "Java 泛型主要通过类型擦除兼容旧字节码。编译器会插入强制类型转换，因此泛型能提升编译期安全性，但不能直接用于运行期判断，也不能创建泛型数组。"},
                {"集合与并发", "重点掌握集合实现、线程安全和并发容器的性能取舍。", "HashMap 扩容与哈希冲突", "ConcurrentHashMap 的并发控制", "锁、CAS 与内存可见性", "HashMap 为什么需要扩容？", "容量不足会增加冲突，扩容后重新分布桶中的元素。生产代码应预估容量，避免高峰期扩容；并发场景不能把 HashMap 当作线程安全容器。", "ConcurrentHashMap 如何保证并发安全？", "JDK 8 主要通过 CAS、桶级 synchronized 和 volatile 保证更新安全，读操作尽量无锁。它适合高并发读写，但复合操作仍要使用 compute、merge 等原子方法。"},
                {"JVM 与垃圾回收", "从运行时内存、类加载到 GC 调优，建立定位内存问题的完整路径。", "堆、栈、元空间职责", "分代回收与可达性分析", "OOM、GC 日志和堆转储", "对象为什么会被垃圾回收？", "GC 以 GC Roots 为起点进行可达性分析，不可达对象才具备回收资格。局部变量、线程栈和静态引用可能改变对象的可达性，不能只看引用计数。", "如何处理线上频繁 Full GC？", "先通过监控确认堆占用、晋升失败和分配速率，再结合 GC 日志和堆转储定位大对象、缓存泄漏或元空间增长；不能只盲目增大堆内存。"},
                {"Spring 核心与事务", "掌握 IoC、AOP、Bean 生命周期和事务代理，能够解释常见失效场景。", "Bean 创建与后置处理器", "代理对象与切面织入", "事务传播、隔离级别和回滚", "Spring 事务为什么在同类方法调用时可能失效？", "事务通常通过代理拦截外部调用，同一个对象内部调用不会经过代理，因此注解可能不生效。应通过拆分 Bean、注入自身代理或调整调用边界解决。", "REQUIRED 和 REQUIRES_NEW 有什么区别？", "REQUIRED 复用当前事务，没有则新建；REQUIRES_NEW 会挂起外部事务并创建独立事务。后者适合独立记录日志，但要注意连接池容量和异常传播。"},
                {"MySQL 事务与索引", "覆盖 B+Tree、MVCC、锁和 SQL 优化，是后端面试中的核心数据存储专题。", "联合索引最左匹配", "MVCC 与隔离级别", "行锁、间隙锁和死锁", "为什么索引通常使用 B+Tree？", "B+Tree 非叶子节点只存索引，叶子节点按序连接，树高较低且适合范围扫描。联合索引还要根据等值、范围和排序条件设计列顺序。", "如何定位一条慢 SQL？", "先用 EXPLAIN 看访问类型、扫描行数、索引和排序，再结合慢查询日志确认频率；修复可能包括索引、SQL 改写、减少返回列和拆分大事务。"},
                {"Redis 与缓存设计", "理解数据结构、缓存一致性、穿透击穿和分布式锁的边界。", "String、Hash、ZSet 的场景", "缓存更新与过期策略", "分布式锁续期和误删", "缓存与数据库如何保证最终一致？", "常见做法是先更新数据库再删除缓存，配合重试或延迟双删降低并发窗口；强一致需求不应只依赖缓存，而应把数据库或消息队列作为最终依据。", "Redis 分布式锁有哪些风险？", "要使用带唯一值的 SET NX EX，并在释放时通过 Lua 校验并删除。还需考虑锁过期、业务执行超时和主从切换，不能把它当作严格的事务锁。"},
                {"消息队列与分布式系统", "覆盖可靠投递、幂等、重试、限流和高可用系统设计。", "生产确认与消费确认", "幂等键和重复消费", "熔断、限流与降级", "如何保证消息不丢失？", "生产端要确认发送结果，Broker 要持久化，消费端处理成功后再确认；仍可能出现重复投递，因此业务必须用唯一键、状态机或幂等表抵御重复。", "如何设计一个高并发接口？", "先明确容量和一致性目标，再采用缓存、无状态扩容、限流、异步削峰、数据库索引和降级策略，并用监控、超时和重试边界保证故障可控。"}
        });
        addLanguage(result, id, "backend", "Python", new String[][]{
                {"Python 语言基础", "掌握对象模型、可变性、迭代器和异常，是 Python 服务稳定性的基础。", "可变对象与不可变对象", "深浅拷贝", "迭代器与生成器", "Python 参数默认值为什么不能使用可变对象？", "默认参数在函数定义时只创建一次，使用列表等可变对象会让多次调用共享状态。应使用 None 作为默认值，在函数内部创建新对象。", "生成器适合什么场景？", "生成器按需产生数据，降低内存峰值，适合大文件、分页和数据管道；但它只能顺序消费，需要随机访问时应改用列表或其他持久化结构。"},
                {"FastAPI 与异步服务", "覆盖依赖注入、Pydantic 校验、asyncio 和服务部署。", "路由与依赖注入", "请求模型和响应模型", "异步 IO 与阻塞调用", "async def 不会自动让阻塞代码变快，为什么？", "事件循环只能在协作式让出执行权时调度其他任务。同步数据库或 CPU 密集代码会阻塞事件循环，应使用异步驱动、线程池或进程池。", "FastAPI 如何做参数校验？", "通过 Pydantic 模型声明字段类型、范围和嵌套结构，框架在进入业务函数前完成校验，并统一返回 422；业务层仍需校验权限和业务规则。"}
        });
        addLanguage(result, id, "backend", "Go", new String[][]{
                {"Go 语言基础", "掌握接口、切片、指针和错误处理，避免服务中的隐式行为。", "接口的隐式实现", "切片底层数组与扩容", "error 与 panic 的边界", "Go 接口为什么可能出现 nil 陷阱？", "接口由动态类型和动态值组成，底层指针为 nil 时接口整体仍可能非 nil。判断错误时要明确返回约定，避免把带 nil 指针的接口当成空值。", "切片 append 为什么可能影响原切片？", "切片包含指针、长度和容量。容量足够时 append 会复用底层数组，可能修改共享数据；需要隔离时使用 copy 或重新分配。"},
                {"并发与服务工程", "覆盖 goroutine、channel、context 和 Go 服务治理。", "channel 关闭规则", "context 取消与超时", "竞态检测与数据竞争", "如何避免 goroutine 泄漏？", "每个 goroutine 都应有退出条件，通常监听 context.Done 或可关闭 channel；调用方要负责取消，服务关闭时要等待关键任务收敛。", "Mutex 和 channel 如何选择？", "共享状态的短临界区适合 Mutex，任务传递和所有权转移适合 channel。选择应看数据流和可读性，而不是把 channel 当作所有同步问题的默认答案。"}
        });
        addLanguage(result, id, "backend", "C#", new String[][]{
                {"C# 基础与类型系统", "掌握值类型、引用类型、委托和 LINQ，建立 .NET 面试基础。", "装箱拆箱", "委托、事件和泛型", "LINQ 延迟执行", "值类型和引用类型有什么区别？", "值类型通常直接保存数据，赋值会复制值；引用类型保存对象引用。装箱会把值类型包装到堆对象中，频繁装箱可能带来分配和性能开销。", "LINQ 延迟执行有什么影响？", "多数查询在枚举时才执行，底层集合变化或数据库连接关闭都可能影响结果。需要固定结果时调用 ToList，但要注意额外内存和查询时机。"},
                {"ASP.NET Core 与异步", "覆盖依赖注入、中间件、EF Core 和异步请求链。", "中间件顺序", "Scoped、Singleton、Transient 生命周期", "CancellationToken 与异步数据库访问", "为什么不能在 Singleton 中直接注入 Scoped 服务？", "Singleton 生命周期更长，直接持有 Scoped 对象会造成作用域错用和数据污染。应注入 IServiceScopeFactory，或重新设计依赖生命周期。", "ASP.NET Core 中间件顺序为什么重要？", "请求按注册顺序进入、按反向顺序返回。认证、异常处理、路由和授权位置不当会导致无法识别用户或异常无法统一处理。"}
        });
        addLanguage(result, id, "backend", "Node.js", new String[][]{
                {"Node.js 运行时", "理解事件循环、模块系统和流，避免把单线程误解成低吞吐。", "事件循环阶段", "CommonJS 与 ESM", "Buffer 和 Stream", "Node.js 单线程如何处理高并发？", "JS 代码主要在事件循环线程执行，网络和文件 IO 交给系统或线程池，完成后回调继续执行。因此它适合 IO 密集场景，但 CPU 长任务会阻塞所有请求。", "什么时候使用 worker_threads？", "CPU 密集任务或需要隔离事件循环时使用 worker_threads；任务之间要明确传递数据，避免共享复杂可变状态，同时关注线程数量和内存。"},
                {"Express 与服务安全", "覆盖中间件、错误处理、认证和输入安全。", "路由与中间件链", "统一错误处理", "JWT、限流和参数校验", "Express 错误中间件为什么必须有四个参数？", "框架通过四参数签名识别错误处理器。它应集中记录 requestId 和返回安全错误信息，不能把堆栈或密钥暴露给客户端。", "Node.js API 如何防止常见攻击？", "校验输入、限制请求体大小、设置安全响应头、密码哈希、JWT 过期、限流并避免 SQL/命令拼接；认证成功不等于授权成功。"}
        });
        addLanguage(result, id, "backend", "TypeScript", new String[][]{
                {"TypeScript 类型设计", "覆盖泛型、联合类型、结构化类型和运行时校验。", "unknown 与 any", "泛型约束", "判别联合与类型守卫", "unknown 为什么比 any 更安全？", "unknown 接收任意值，但使用前必须经过类型收窄；any 会关闭检查并把错误推迟到运行期。外部接口数据应先运行时校验，再转为可信类型。", "接口和类型别名如何选择？", "两者都能描述对象结构；接口适合可扩展的对象契约，类型别名更适合联合、交叉和元组。团队应保持风格一致并以表达能力为依据。"},
                {"TypeScript 工程化", "掌握模块、编译配置和前后端契约，减少大型项目回归。", "strict 模式", "类型声明与模块边界", "API 响应类型和运行时校验", "strictNullChecks 解决了什么问题？", "它要求显式处理 null 和 undefined，避免把缺失数据当成完整对象使用。配合可选链、默认值和明确分支，能显著减少线上空值异常。", "为什么类型定义不能替代运行时校验？", "类型只存在于编译阶段，JSON、用户输入和网络响应进入程序时没有类型信息。边界处仍应校验字段和范围，校验后再建立领域对象。"}
        });
        addLanguage(result, id, "frontend", "JavaScript", new String[][]{
                {"JavaScript 核心机制", "覆盖原型、闭包、this 和异步，是前端面试最重要的基础。", "原型链", "词法作用域与闭包", "this 绑定规则", "闭包有什么实际用途和风险？", "闭包保存外层作用域，可用于模块私有状态、缓存和回调；如果长期引用大对象或 DOM，可能阻止回收，应及时解绑监听器。", "Promise 和 async/await 如何处理异常？", "await 抛出 rejected Promise 的原因，需要 try/catch 或让上层统一处理；并行无依赖任务应使用 Promise.all，同时考虑其中一个失败后的取消和补偿。"},
                {"浏览器与性能", "覆盖渲染流程、缓存、网络和性能优化。", "事件循环与任务队列", "重排、重绘与合成", "HTTP 缓存和懒加载", "如何定位页面卡顿？", "先用 Performance 面板看长任务、布局和脚本耗时，再定位组件更新或大计算。优化包括减少主线程工作、批量 DOM 修改、虚拟列表和 Web Worker。", "防抖和节流有什么区别？", "防抖在连续触发结束后执行，适合搜索；节流按固定间隔执行，适合滚动和拖拽。两者都不能替代后端限流和请求取消。"}
        });
        addLanguage(result, id, "frontend", "TypeScript", new String[][]{
                {"前端类型建模", "用类型约束组件、表单和 API 边界，提升重构安全性。", "Props 与 Emits 类型", "联合类型表达状态机", "unknown 与类型守卫", "前端 API 响应为什么要区分业务数据和传输包装？", "统一 ApiResponse 能让客户端集中处理 code、message 和 data，业务类型只描述 data。这样认证失效、错误提示和正常数据不会散落在页面代码中。", "如何为异步页面建模状态？", "用 discriminated union 表达 idle、loading、success、error，避免多个 boolean 组合出非法状态；每个状态都应有明确的 UI。"},
                {"类型安全工程实践", "覆盖严格配置、组件泛型和边界校验。", "strict 模式", "可复用泛型组件", "运行时数据校验", "为什么不能滥用类型断言？", "断言只是告诉编译器相信开发者，不会改变运行时数据。过度断言会隐藏接口变更，应优先使用类型守卫、校验器和窄化函数。", "如何避免前端类型和后端接口漂移？", "以 OpenAPI 或共享契约生成类型，并在请求客户端集中适配响应；后端字段变更时让编译失败尽早暴露问题。"}
        });
        addLanguage(result, id, "frontend", "Vue", new String[][]{
                {"Vue 3 响应式与组件", "掌握 ref、reactive、computed、watch 和组件通信。", "依赖追踪与触发更新", "computed 缓存", "Props、Emits 与插槽", "computed 和 watch 如何选择？", "computed 用于声明式派生值并具备缓存；watch 用于监听变化并执行请求、日志等副作用。不要用 watch 计算本可以直接派生的展示值。", "Vue 响应式对象有哪些常见坑？", "解构 reactive 可能丢失响应式，需使用 toRefs；异步更新要理解 nextTick；列表渲染必须使用稳定 key，避免复用错误的组件状态。"},
                {"Vue 工程化", "覆盖 Composition API、路由、状态管理和性能。", "组合式函数边界", "Pinia 状态归属", "路由懒加载与组件更新", "什么时候应该抽取 composable？", "当多个组件共享一段有状态逻辑、生命周期和副作用时抽取；单纯的无状态格式化函数应放到普通工具模块，避免过度抽象。", "Vue 页面如何减少无效更新？", "缩小响应式范围、拆分稳定组件、合理使用 computed、懒加载路由，并避免在模板中创建新对象或执行重计算函数。"}
        });
        addLanguage(result, id, "frontend", "React", new String[][]{
                {"React 核心与 Hooks", "掌握渲染、状态、Effect 和组件设计，理解 Hooks 规则。", "状态更新与批处理", "Effect 依赖数组", "受控组件", "为什么 Hooks 不能放在条件分支中？", "React 依靠每次渲染中稳定的 Hook 调用顺序对应状态槽位。条件调用会改变顺序，使状态错位，因此应把条件放进 Hook 内部或拆分组件。", "useEffect 应该处理什么？", "它适合与外部系统同步，例如订阅、请求和 DOM API；纯计算应使用普通表达式或 useMemo。Effect 还要清理订阅、定时器和过期请求。"},
                {"React 性能与架构", "覆盖不可变更新、组件边界、缓存和异步数据。", "key 与列表状态", "useMemo/useCallback 的成本", "服务端状态和客户端状态分离", "为什么 key 不推荐使用数组下标？", "列表插入、删除或排序时，下标会对应到不同数据，导致组件状态复用错误。应使用业务稳定 ID，确保 React 正确识别元素。", "React 性能优化应从哪里开始？", "先用 Profiler 找到实际慢点，再减少不必要状态提升、拆分组件、优化列表和请求缓存；盲目添加 memo 也会增加比较和维护成本。"}
        });
        return List.copyOf(result);
    }

    private static void addLanguage(List<KnowledgeTopicResponse> result, long[] id, String direction,
                                    String language, String[][] definitions) {
        for (String[] item : definitions) {
            result.add(topic(id[0]++, direction, language, item[0], item[0], item[1],
                    List.of(item[2], item[3], item[4]),
                    new KnowledgeTopicResponse.KnowledgeQuestion(item[5], item[6]),
                    new KnowledgeTopicResponse.KnowledgeQuestion(item[7], item[8])));
        }
    }

    private static KnowledgeTopicResponse topic(Long id, String direction, String language, String category,
                                                String title, String summary, List<String> keyPoints,
                                                KnowledgeTopicResponse.KnowledgeQuestion... questions) {
        return new KnowledgeTopicResponse(id, direction, language, category, title, summary,
                keyPoints, List.of(questions));
    }
}
