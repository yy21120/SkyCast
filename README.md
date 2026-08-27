# SkyCast（逐光）

SkyCast 是一款面向风光、天文与强天气摄影场景的 AI 气象决策助手。它不只展示天气，而是回答：值不值得出发、何时拍摄、去哪里拍、风险是否可接受，以及判断依据是什么。

## 当前阶段

项目进入 `M1 — 武汉晚霞纵向切片`：

- 产品全链路和指标已经定义；
- 4 周 MVP 的范围与验收标准已经定义；
- Android、后端和 C++ 核心模块的边界已经定义；
- C++ 核心模块已建立第一组领域模型与测试；
- FastAPI 后端已建立武汉晚霞回放与实时 Provider 边界；
- Android 已支持机会卡、可追溯详情、离线缓存和实况结果反馈；
- 服务端已支持 SQLite 反馈持久化和客户端幂等重试；
- Git 仓库采用 GitHub Flow 和 Conventional Commits。

## 文档入口

- [产品全链路总纲](docs/PRODUCT_BLUEPRINT.md)
- [MVP PRD](docs/PRD_MVP.md)
- [技术架构](docs/ARCHITECTURE.md)
- [Git 实战工作流](docs/GIT_WORKFLOW.md)
- [产品待办](docs/BACKLOG.md)

## 仓库结构

```text
SkyCast/
├─ docs/                 产品、架构、迭代与 Git 文档
├─ core/                 可复用 C++20 领域与计算核心
├─ android/              Android Compose 客户端
├─ server/               FastAPI 数据接入与评估服务
├─ data/sample/wuhan/    可公开回放的武汉样例
└─ .github/              Issue 与 Pull Request 模板
```

## 当前可运行内容

```powershell
cmake -S core -B build/core -G Ninja
cmake --build build/core
ctest --test-dir build/core --output-on-failure
```

武汉晚霞 API 和反馈服务的运行方式见 [server/README.md](server/README.md)，客户端运行与质量门禁见 [android/README.md](android/README.md)。

## 产品原则

1. 官方预警与 AI 研判严格分层。
2. 每个结论都展示来源、观测时间、有效期、置信度和模型版本。
3. LLM 负责解释，不替代数值模型作出气象结论。
4. 安全约束优先于景观评分和路线效率。
5. 每个版本必须有可重复的测试和量化验收结果。
