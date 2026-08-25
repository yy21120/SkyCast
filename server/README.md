# SkyCast Server

第一条纵向切片提供武汉晚霞机会评估：

```text
天气 Provider → 日落时刻特征 → 可解释规则评分 → FastAPI → 客户端机会卡
```

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

## 数据与结论边界

- `replay` 是合成测试样例，只用于确定性开发和自动化测试，不代表真实天气。
- `live` 使用 Open-Meteo 预报数据，客户端必须展示来源链接和更新时间。
- 当前概率是未校准的规则基线，不得显示为官方预报或官方预警。
- Open-Meteo 数据按 CC BY 4.0 使用，展示数据时必须署名并链接来源。
