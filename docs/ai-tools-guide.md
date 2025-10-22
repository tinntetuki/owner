# AI编程工具使用指南

## 目录
- [一、工具概览](#一工具概览)
- [二、GitHub Copilot](#二github-copilot)
- [三、ChatGPT/Claude](#三chatgptclaude)
- [四、Cursor](#四cursor)
- [五、最佳实践](#五最佳实践)

## 一、工具概览

### 1.1 主流AI编程工具

| 工具 | 类型 | 特点 | 适用场景 |
|------|------|------|---------|
| GitHub Copilot | 代码补全 | IDE集成，实时建议 | 日常编码 |
| ChatGPT/Claude | 对话助手 | 强大理解能力 | 代码审查、架构设计 |
| Cursor | AI-first IDE | 集成IDE | 全流程开发 |
| Amazon CodeWhisperer | 代码补全 | AWS集成 | AWS项目 |
| JetBrains AI | IDE集成 | JetBrains产品 | IDEA用户 |

### 1.2 使用场景

- **代码生成**：函数实现、单元测试
- **代码优化**：性能优化、代码重构
- **Bug修复**：定位问题、提供修复方案
- **文档生成**：注释、README、API文档
- **学习辅助**：代码解释、最佳实践

## 二、GitHub Copilot

### 2.1 基本使用

**代码补全**：
```java
// 输入注释，Copilot自动生成代码
// 实现二分查找
public int binarySearch(int[] arr, int target) {
    // Copilot会生成完整实现
}
```

**单元测试**：
```java
// 输入测试方法名
@Test
public void testBinarySearch() {
    // Copilot自动生成测试用例
}
```

### 2.2 高级技巧

**1. 提供上下文**：
```java
// 已有代码：
public class UserService {
    private UserRepository userRepository;
    
    // 输入方法签名，Copilot根据上下文生成
    public User getUserById(Long id) {
        // 自动生成repository调用
    }
}
```

**2. 使用示例**：
```java
// 示例：计算阶乘
// factorial(5) -> 120
public int factorial(int n) {
    // Copilot根据示例生成正确实现
}
```

**3. 多个建议**：
- Windows/Linux: `Ctrl + Enter`
- Mac: `Option + ]`

## 三、ChatGPT/Claude

### 3.1 代码审查

**Prompt**：
```
请审查以下Java代码，指出潜在问题和改进建议：

[粘贴代码]

重点关注：
1. 性能问题
2. 线程安全
3. 异常处理
4. 代码规范
```

### 3.2 架构设计

**Prompt**：
```
我需要设计一个秒杀系统，要求：
- 支持10万+ QPS
- 防止超卖
- 防止恶意刷单

请给出架构设计方案，包括：
1. 整体架构图
2. 核心流程
3. 技术选型
4. 关键代码示例
```

### 3.3 Debug辅助

**Prompt**：
```
以下代码抛出NullPointerException，请帮我定位问题：

[粘贴代码和错误堆栈]

并给出修复方案。
```

### 3.4 学习新技术

**Prompt**：
```
请教我Flink的Window机制，要求：
1. 基本概念
2. 各种Window类型
3. 代码示例
4. 最佳实践
```

## 四、Cursor

### 4.1 特色功能

**1. AI Chat**：
- 在IDE内对话
- 代码上下文感知

**2. AI Edit**：
- 选中代码，AI修改
- 自然语言指令

**3. Generate**：
- 从注释生成代码
- 从需求生成代码

### 4.2 使用技巧

**1. 项目级别理解**：
```
Ctrl + K → "这个项目是做什么的？"
```

**2. 快速重构**：
```
选中代码 → Ctrl + K → "将这个方法拆分为多个小方法"
```

**3. 生成测试**：
```
选中类 → Ctrl + K → "为这个类生成完整的单元测试"
```

## 五、最佳实践

### 5.1 Prompt Engineering

**1. 明确具体**：
```
❌ "写一个排序算法"
✅ "用Java实现快速排序，要求：
   - 支持泛型
   - 有详细注释
   - 包含单元测试"
```

**2. 提供上下文**：
```
我正在开发一个Spring Boot项目，使用MySQL数据库。
请帮我实现一个用户注册功能，包括：
- Controller
- Service
- Repository
- Entity
- 参数校验
- 异常处理
```

**3. 分步骤**：
```
第一步：设计数据库表结构
第二步：实现CRUD接口
第三步：添加缓存
第四步：编写测试用例
```

### 5.2 安全注意事项

**不要泄露敏感信息**：
```
❌ 包含真实密码、API Key
✅ 使用占位符：password=xxx, apiKey=your_api_key
```

**验证生成的代码**：
- AI可能生成有bug的代码
- 必须经过测试
- 注意安全漏洞

### 5.3 效率提升建议

**1. 快速原型**：
- AI快速生成基础代码
- 人工精细调整

**2. 学习加速**：
- 让AI解释陌生代码
- 询问最佳实践

**3. 文档生成**：
- 自动生成注释
- 生成README
- API文档

### 5.4 组合使用

**工作流示例**：
```
1. Cursor: 快速搭建项目框架
2. GitHub Copilot: 日常编码辅助
3. ChatGPT: 架构设计、代码审查
4. 人工: 核心逻辑、优化调整
```

### 5.5 常见陷阱

**1. 过度依赖**：
- AI是辅助工具
- 核心逻辑需人工把控

**2. 不加验证**：
- 生成的代码可能有误
- 必须测试验证

**3. 忽视性能**：
- AI生成的代码可能性能不佳
- 需要性能测试和优化

---

**关键字**：AI编程、GitHub Copilot、ChatGPT、Cursor、Prompt Engineering

