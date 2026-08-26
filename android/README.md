# SkyCast Android

首个 Android 纵向切片展示武汉未来三天晚霞机会卡。

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
- 损坏或无法解析的缓存会被删除，不会导致 App 崩溃。

缓存表首版以 `城市:场景:天数` 为键保存 JSON 快照和本地缓存时间。Room Schema 保存在 `app/schemas`，数据库升级时必须提交新的 Schema 和迁移测试。

## Android 质量门禁

从纯英文映射路径执行：

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
.\gradlew.bat connectedDebugAndroidTest
```

第二条命令需要已启动的 Android 模拟器或已连接设备，用于运行真实 Room 数据库测试。
