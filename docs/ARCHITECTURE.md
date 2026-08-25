# SkyCast 技术架构

## 1. 架构原则

- 起步采用模块化单体，避免为作品版引入微服务复杂度；
- 数据接入、领域决策和展示相互隔离；
- 数值模型产生结论，LLM只解释结构化结论；
- C++只进入性能敏感且可测量的路径；
- 所有实时能力必须支持历史重放，便于演示和回归测试。

## 2. 逻辑架构

```text
数据提供方
  ├─ 天气/实况/官方预警
  ├─ 雷达
  ├─ FY-4卫星
  ├─ DEM/土地覆盖
  └─ 地图与道路
          │
          ▼
Provider Adapters → Quality Control → Normalized Observations
          │                              │
          │                              ├─ Object Storage（栅格）
          │                              ├─ PostGIS（时空对象）
          │                              └─ Redis（热点缓存）
          ▼
Decision Engine
  ├─ Scene Scoring
  ├─ Radar Nowcasting
  ├─ Site Ranking
  └─ Safety Policy
          │
          ▼
API / WebSocket / Push
          │
          ▼
Android App
  ├─ Compose UI
  ├─ Room Offline Cache
  ├─ Map/Timeline
  └─ C++ meteocore via JNI
```

## 3. 客户端模块

```text
android/app              应用壳、导航、依赖装配
android/feature-home     城市首页
android/feature-scene    场景评估
android/feature-radar    雷达与追溯时间轴
android/feature-route    安全地点与路线
android/core-network     API、鉴权、重试
android/core-database    Room与离线读取
android/core-model       Kotlin领域模型
core/                    C++20共享核心
```

Android首版采用单 Activity、Compose、ViewModel、Repository。读取以 Room 为单一事实来源：网络同步成功后写入 Room，UI 只观察本地数据。

## 4. C++ 核心边界

适合放入 C++：

- 雷达色标转换和栅格解码；
- 动画帧缓存、插值和 OpenGL ES 渲染；
- 轮廓提取、地形视线与空间几何；
- 可在桌面和 Android 共用的领域校验。

不放入 C++：

- 页面状态和Android生命周期；
- 普通HTTP请求、数据库业务代码；
- 产品策略文案；
- 需要频繁改动的推荐编排逻辑。

JNI规则：

- Kotlin层持有一个显式 native handle；
- 批量数据使用 DirectByteBuffer 或扁平数组；
- 禁止逐像素JNI调用；
- Native异常不能越过JNI边界；
- 每个Native API有桌面单元测试和Android集成测试。

## 5. 后端边界

建议从一个 FastAPI 模块化单体开始：

```text
server/app/providers     外部数据适配器
server/app/ingestion     调度、质控、标准化
server/app/domain        领域对象与策略
server/app/models        数值模型与评估
server/app/api           REST/WebSocket
server/tests             单元、契约、历史回放测试
```

首批API：

```text
GET /v1/cities/{cityId}/opportunities
GET /v1/scenes/{sceneId}/assessment
GET /v1/radar/frames
GET /v1/nowcasts/{location}
POST /v1/feedback
GET /v1/events/{eventId}/timeline
```

## 6. AI职责边界

| 能力 | 建议技术 | LLM角色 |
|---|---|---|
| 朝晚霞概率 | 规则基线 + LightGBM/校准模型 | 解释影响因素 |
| 雷达外推 | persistence + 光流 + STEPS | 不参与数值预测 |
| 地点排序 | 硬约束 + 排序模型 | 总结地点优缺点 |
| 用户问答 | 检索结构化事实 | 组织语言并引用来源 |

LLM只能接收已经校验过的结构化事实，输出必须保留来源和有效时间。它不能生成新的官方预警或改变安全等级。

## 7. 首条纵向切片

第一个端到端功能选择“城市晚霞机会卡”：

```text
固定历史数据 → 后端SceneAssessment → API → Room → Compose机会卡 → 用户反馈
```

原因：数据规模小、用户价值直观、可快速建立产品反馈闭环。雷达实时链路作为第二条纵向切片。
