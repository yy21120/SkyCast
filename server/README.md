# SkyCast Server

当前纵向切片提供武汉晚霞机会评估和实况结果反馈：

```text
天气 Provider → 日落时刻特征 → 可解释规则评分 → FastAPI → 客户端机会卡
```

雷达开发基线通过独立 Provider 读取仓库自有的武汉合成强对流事件包，尚未暴露公共 API。

## 本地启动

在 `server` 目录执行：

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -e ".[dev]"
python -m pytest
python -m uvicorn app.main:app --reload
```

访问：

- `http://127.0.0.1:8000/docs`
- `http://127.0.0.1:8000/v1/cities/wuhan/opportunities?mode=replay&days=3`
- `http://127.0.0.1:8000/v1/cities/wuhan/opportunities?mode=live&days=3`

## 晚霞实况反馈

提交反馈：

```http
POST /v1/feedback/sunset
Content-Type: application/json
```

```json
{
  "client_feedback_id": "5215a6f3-bace-45df-ae86-9de854f6fc64",
  "scene_id": "wuhan-sunset-2026-08-27",
  "outcome": "vivid",
  "shooting_quality": 5,
  "notes": "东湖边可以看到明显染色。",
  "submitted_at": "2026-08-27T19:20:00+08:00"
}
```

`outcome` 支持 `vivid`、`visible` 和 `not_visible`。`shooting_quality` 必须为 1～5，备注可省略且最多 200 字。

开发环境默认保存到 `server/var/skycast.db`。此目录不会提交到 Git。相同 `client_feedback_id` 和相同内容可安全重试；同一 ID 对应不同内容时返回 HTTP 409。

反馈接口不要求或存储姓名、手机号、精确位置和设备唯一标识。备注属于用户主动输入，产品界面应提醒用户不要填写个人敏感信息。

## 数据与结论边界

- `replay` 是合成测试样例，只用于确定性开发和自动化测试，不代表真实天气。
- `live` 使用 Open-Meteo 预报数据，客户端必须展示来源链接和更新时间。
- 当前概率是未校准的规则基线，不得显示为官方预报或官方预警。
- Open-Meteo 数据按 CC BY 4.0 使用，展示数据时必须署名并链接来源。
- 雷达事件是合成回放，必须标注非实时、非官方且不可用于防灾或实际追风。

## 合成雷达事件

在仓库根目录重新生成并校验：

```powershell
server\.venv\Scripts\python.exe tools\build_synthetic_radar_event.py
server\.venv\Scripts\python.exe tools\validate_radar_event.py
```

事件包含 12 个 80×80 的 `uint8-dbz-v1` 帧，间隔 6 分钟。完整格式、checksum 算法和
真实雷达 Provider 的扩展约束见 `docs/RADAR_REPLAY_CONTRACT.md`。
