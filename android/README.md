# SkyCast Android

首个 Android 纵向切片展示武汉未来三天晚霞机会卡和可追溯评估详情。

点击任意机会卡可进入详情页，查看：

- 染色开始、日落和染色结束时刻（统一转换为武汉时区）；
- 完整评分因子、观测值、单位、贡献和解释；
- 未校准规则基线值、概率状态、置信度和模型版本；
- 数据来源、采样时间、获取时间和可用的公开来源链接；
- 当前数据来自在线请求、离线缓存还是过期缓存。

`baseline_probability` 在客户端固定标注为“未校准规则基线值”，仅用于规则排序，不能解释为晚霞发生的真实概率。来源按钮只接受合法的 `http` 或 `https` 地址。

## 实况结果反馈

详情页底部提供“记录实况”入口。用户可以选择明显晚霞、普通晚霞或未出现，填写 1～5 级拍摄质量，并添加最多 200 字的可选备注。

- 每次反馈使用随机 `client_feedback_id`，网络失败后的原样重试复用同一请求；
- 失败时保留选择、质量和备注，用户修改内容后会生成新的幂等键；
- 提交中禁止重复点击，成功后展示明确确认状态；
- 客户端不主动采集姓名、手机号、精确位置或设备唯一标识；
- 页面提醒用户不要在备注中填写个人敏感信息。

反馈当前需要在线提交。离线反馈队列、账号体系、图片上传和公开评论不属于此版本。

## 本地接口

Debug 构建默认连接 Android 模拟器宿主机的 FastAPI：

```text
http://10.0.2.2:8000
```

先在仓库根目录启动服务：

```powershell
cd server
.\.venv\Scripts\python.exe -m uvicorn app.main:app --reload
```

再用 Android Studio 打开 `android` 目录，运行 `app`。

当前页面明确标注“规则评分基线·非官方天气预报”。Release 构建不会允许明文 HTTP，正式部署时必须替换为 HTTPS API。

Windows 下请从纯英文路径打开和构建 Android 工程。Android Gradle Plugin、JUnit 进程以及后续 NDK/JNI 工具链可能无法正确处理非 ASCII 工程路径。

## 离线缓存

客户端使用 Room 保存武汉晚霞最近一次成功响应：

- 在线请求成功后先写入 `opportunity_cache`，再展示在线状态；
- 请求失败且存在缓存时继续展示机会卡，并标记“离线缓存”；
- 缓存超过 6 小时后标记“缓存已过期，仅供参考”；
- 缓存页面可通过“重新获取”主动恢复在线数据；
- 从缓存进入详情页时继续保留离线或过期状态及“重新获取”入口；
- 损坏或无法解析的缓存会被删除，不会导致 App 崩溃。

缓存表首版以 `城市:场景:天数` 为键保存 JSON 快照和本地缓存时间。Room Schema 保存在 `app/schemas`，数据库升级时必须提交新的 Schema 和迁移测试。

## Android 质量门禁

从纯英文映射路径执行：

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
.\gradlew.bat connectedDebugAndroidTest
```

第二条命令需要已启动的 Android 模拟器或已连接设备，用于运行真实 Room 数据库、机会卡导航、详情页和反馈表单 Compose 测试。
