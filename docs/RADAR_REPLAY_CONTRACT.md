# 武汉合成雷达回放契约

## 1. 定位

该事件包用于在真实中国雷达数据尚未完成授权时，继续开发 SkyCast 的雷达时间轴、异常帧
处理、短临算法和客户端渲染。事件完全由仓库代码生成，不对应武汉任何一次真实天气过程。

所有展示必须保留：

> 历史合成回放，非实时官方雷达，不用于防灾决策或实际追风导航。

## 2. 事件内容

| 字段 | 当前值 |
|---|---|
| event_id | `wuhan-synthetic-convection-v1` |
| source_id | `skycast:synthetic-radar:wuhan-v1` |
| 帧数 | 12 |
| 帧间隔 | 6 分钟 |
| 网格 | 80 × 80 |
| 范围 | 113.2–115.2°E，29.6–31.6°N |
| CRS | `EPSG:4326` |
| 产品 | `composite_reflectivity` |
| 单位 | `dBZ` |
| 固定种子 | 21120 |
| 整包 checksum | `bf8d3d11cca27b2e0b5ca49e39d5e832a2e5b0692b5f3fb3b1f765d61be16412` |

主对流单体从区域西南侧向东北移动，强度在过程中先增强后减弱。该可预测轨迹只用于后续
persistence baseline、光流和路径可视化的确定性测试。

## 3. 文件格式

```text
data/sample/wuhan/radar_synthetic_v1/
├─ manifest.json
└─ frames/
   ├─ frame-000.bin
   ├─ ...
   └─ frame-011.bin
```

`manifest.json` 保存事件与帧元数据。每个 `.bin` 是 `uint8-dbz-v1` 栅格：

- 每像素 1 字节；
- 行优先；
- 第一行位于 bounding box 北侧；
- 0–75 直接表示 dBZ；
- 255 保留为缺测；
- 76–254 非法；
- 当前每帧 80 × 80 = 6,400 字节。

每帧保存 SHA-256。整包 checksum 是以下 UTF-8 文本的 SHA-256：

```text
frame-000:<frame checksum>
frame-001:<frame checksum>
...
frame-011:<frame checksum>
```

## 4. 时间与质量规则

- `observed_at`、`ingested_at` 和 `expires_at` 均以带时区 UTC 时间保存；
- UI 使用 `Asia/Shanghai` 显示；
- `observed_at <= ingested_at < expires_at`；
- 帧时间必须严格按声明的 6 分钟间隔递增；
- 当前基线包所有帧使用 `quality_flags: ["complete"]`；
- `synthetic: true`、`official: false` 必须在事件和每帧同时保留。

缺帧、重复帧和乱序帧不会污染这份干净基线，而是在 E1-06 中从基线派生测试序列。

## 5. 生成与验证

在仓库根目录执行：

```powershell
server\.venv\Scripts\python.exe tools\build_synthetic_radar_event.py
server\.venv\Scripts\python.exe tools\validate_radar_event.py
```

生成器使用固定种子；相同版本重复生成后，manifest 和所有帧的 checksum 必须完全一致。

校验器拒绝：

- manifest 领域字段不合法；
- 时间不递增或间隔不一致；
- 文件缺失或尺寸错误；
- 帧 checksum 或整包 checksum 错误；
- 反射率超出 0–75 dBZ；
- 元数据最大反射率与栅格不一致；
- 数据路径逃逸事件目录；
- 被标记为官方数据的 fixture。

## 6. Provider 扩展边界

`RadarProvider` 只暴露三个稳定操作：

```text
event(event_id) -> RadarEvent
frames(event_id) -> list[RadarFrame]
frame_data(event_id, frame_id) -> RadarFrameData
```

当前由 `SyntheticReplayRadarProvider` 实现。真实数据到位后新增 CMA Provider，但必须满足：

1. 对应来源在准入目录中由 `restricted` 更新为 `approved`；
2. 凭据只存在服务端环境或密钥系统，不进入 Git 和 Android APK；
3. 输出同一个 RadarEvent/RadarFrame 契约；
4. 保留官方来源、观测时间、获取时间、有效期和授权署名；
5. 官方预警与 AI 研判继续分层；
6. Provider 不可用时降级到明确标注的合成回放，不能伪装实时。
