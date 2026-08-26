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
