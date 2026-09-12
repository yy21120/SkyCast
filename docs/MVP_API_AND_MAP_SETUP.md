# SkyCast MVP：DeepSeek 与高德地图接入

## 1. 架构边界

```text
Android App ──HTTPS──> SkyCast FastAPI ──HTTPS──> DeepSeek API
      │
      ├──高德 Android 地图 SDK：显示、缩放、点击候选机位
      └──高德 URI：跳转高德地图 App，开始驾车或步行导航
```

- `DEEPSEEK_API_KEY` 只能部署到服务端环境变量，绝不能写进 APK、Git 或聊天记录。
- `AMAP_API_KEY` 是 Android SDK Key，会进入 APK，但仍必须在高德控制台绑定包名和 SHA1。
- 晚霞分数、时间与天气因子来自 SkyCast 结构化评估；DeepSeek 只解释和生成摄影建议。
- 地图上的点是候选机位，不是空间化晚霞概率。没有可靠网格预报前，不展示伪热力图。

## 2. 部署 DeepSeek 服务

### 2.1 获取密钥

在 DeepSeek 开放平台创建 API Key。不要把真实 Key 发到 Issue、PR、截图或提交记录中。

### 2.2 本地验证

在 `server` 目录启动 PowerShell：

```powershell
$env:DEEPSEEK_API_KEY = "你的密钥"
$env:DEEPSEEK_MODEL = "deepseek-flash"
$env:DEEPSEEK_BASE_URL = "https://api.deepseek.com"
.\.venv\Scripts\python.exe -m uvicorn app.main:app --reload
```

另开终端验证：

```powershell
$body = @{
  city_id = "wuhan"
  message = "今天几点去东湖拍？"
  selected_spot_id = "east-lake-lingbo-gate"
  history = @()
  mode = "live"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://127.0.0.1:8000/v1/agent/chat" `
  -ContentType "application/json" `
  -Body $body
```

返回的 `provider` 为 `deepseek` 代表模型调用成功；`fallback: true` 代表安全降级为规则建议。

### 2.3 部署到云端

仓库中的 `render.yaml` 和 `server/Dockerfile` 可用于 Render 或其他支持 Docker 的云平台：

1. 连接 GitHub 仓库，并保持服务根目录为仓库根目录 `.`。
2. 构建方式选择 Dockerfile，路径使用 `server/Dockerfile`，构建上下文使用仓库根目录。
3. 添加私密环境变量 `DEEPSEEK_API_KEY`、`DEEPSEEK_MODEL` 和 `DEEPSEEK_BASE_URL`。
4. 健康检查路径填写 `/ready`，容器端口由平台的 `PORT` 环境变量提供。
5. 部署完成后确认 `https://你的域名/ready`、`/health` 和 `/docs` 可以访问；`/health` 中的 `agent_provider` 应为 `deepseek`。
6. 不要把密钥配置成 Docker build argument；它必须是运行时 Secret。

Render 可以直接识别仓库根目录的 `render.yaml`。部署完成后，只有当云端 `/health` 返回 `status=ok`、Agent 返回 `provider=deepseek` 时，才应用该 HTTPS 域名构建手机 APK。Cloudflare Quick Tunnel 地址只用于临时联调，不能用于独立安装包。

构建连接正式服务的 APK：

```powershell
cd android
.\gradlew.bat --% assembleDebug -PSKYCAST_API_BASE_URL=https://你的服务域名
```

正式包使用：

```powershell
.\gradlew.bat --% assembleRelease -PSKYCAST_RELEASE_API_BASE_URL=https://你的服务域名
```

## 3. 接入高德地图

### 3.1 创建 Android Key

在高德开放平台创建应用和 Android Key。当前工程信息：

```text
包名 / applicationId：com.yy21120.skycast
```

获取本机 Debug SHA1：

```powershell
cd android
.\gradlew.bat signingReport
```

把输出中的 Debug `SHA1` 与包名一起填入高德控制台。Release 包需要使用正式签名证书的 SHA1 再创建或更新 Key；Debug 和 Release SHA1 通常不同。

### 3.2 在本机保存 Key

编辑 `%USERPROFILE%\.gradle\gradle.properties`，添加：

```properties
AMAP_API_KEY=你的AndroidKey
```

该文件位于用户目录，不会进入仓库。也可以只对一次构建传入：

```powershell
.\gradlew.bat --% assembleDebug -PAMAP_API_KEY=你的AndroidKey
```

重新安装后，在 Agent 中点击“推荐去哪拍？”即可出现地图模块。首次加载真实地图时，App 会先请求高德地图隐私同意。用户不同意时不会构造 `MapView`。

当前高德 10.x 地图 SDK 仅支持 `armeabi-v7a` 和 `arm64-v8a`。普通 x86/x86_64 模拟器会继续显示本地机位预览；真实高德地图请在 ARM 模拟器或 Android 实体手机上验收。
本地预览中的机位圆点同样可点击，可以在 x86 模拟器完成机位选择和导航入口交互验收。

### 3.3 导航行为

- 点击机位切换目的地。
- “驾车导航”或“步行导航”优先调起高德地图 App。
- 未安装高德地图时，自动打开高德 URI 网页。
- SkyCast 不请求精确定位；导航起点由高德地图在获得用户授权后使用当前位置。

## 4. 验收清单

- 未配置 DeepSeek Key：聊天仍返回明确标记的规则建议。
- 已配置 DeepSeek Key：`provider=deepseek`，分数和时间仍与结构化评估一致。
- 未配置高德 Key：首页正常运行，显示本地机位示意图。
- 已配置高德 Key：同意隐私说明后地图可缩放、点击标记。
- 点击导航：能调起高德 App或 URI 网页，终点名称和坐标准确。
- 首页初始只显示概览与对话；询问详细数据或地点后再出现相应模块。
- APK 中不得出现 `DEEPSEEK_API_KEY`。

## 5. 官方参考

- DeepSeek Chat Completions：<https://api-docs.deepseek.com/api/create-chat-completion/>
- 高德 Android SDK 工程配置：<https://lbs.amap.com/api/android-sdk/guide/create-project/android-studio-create-project>
- 高德 Android Key：<https://lbs.amap.com/api/android-sdk/guide/create-project/get-key>
- 高德地图隐私合规：<https://lbs.amap.com/api/maps-sdk-for-android/guide/create-project/dev-attention>
- 高德 App 路径规划 URI：<https://lbs.amap.com/api/amap-mobile/guide/android/route>
