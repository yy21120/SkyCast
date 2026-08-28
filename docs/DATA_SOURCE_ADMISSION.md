# SkyCast 数据源准入与公开演示边界

## 1. 目的

本文件是数据接入前的产品、合规和工程门禁。它回答的不是“网页上能否看到数据”，
而是“SkyCast 能否程序化获取、缓存、加工、公开展示和用于未来生产服务”。

结论核验日期为 **2026-08-28**，不构成法律意见。条款变化、产品迁移或超过目录设定的
复核周期后，必须重新核验官方依据。

机器可读事实源位于 `data/catalog/data_sources.v1.json`。本文解释决策，不能替代目录。

## 2. 准入状态

| 状态 | 工程含义 | 是否可以新增 Provider |
|---|---|---|
| `approved` | 当前用途、署名和降级条件均已明确 | 可以，但必须遵守目录约束 |
| `restricted` | 需要申请、账号、合同或书面确认 | 不可以，完成条件后重新评审 |
| `blocked` | 官方声明禁止当前获取或使用方式 | 不可以，只能使用自有或获授权替代数据 |

任何未登记来源默认视为 `blocked`。开发便利、浏览器可访问、没有登录或技术上可下载，
都不能替代授权依据。

## 3. 首批准入结论

| source_id | 数据 | 状态 | 作品集边界 | 生产边界 |
|---|---|---:|---|---|
| `skycast:synthetic-radar:wuhan-v1` | 武汉合成强对流雷达回放 | `approved` | 必须标注历史合成、非实时官方雷达 | 禁止用于生产和防灾决策 |
| `open-meteo:forecast` | 云量、降水、能见度等预报 | `approved` | 非商业演示可用，必须署名和链接 | 商业发布前订阅商业方案 |
| `cma:nmc-radar-mosaic:web` | 国家气象中心雷达拼图网页 | `blocked` | 禁止抓取、缓存和嵌入 | 获书面授权前禁止 |
| `cma:data-service:radar-composite-reflectivity` | 中国气象数据服务雷达拼图 | `restricted` | 实名注册并确认展示边界 | 依书面授权执行 |
| `cma:smart-weather-api:official-alerts` | 官方预警 API | `blocked` | 公布的申请邮箱失效，等待新通道 | 等待官方确认 |
| `cma:nsmc:fy4b-agri-l1` | FY-4B AGRI L1 | `restricted` | 注册下载后仍需确认公开再分发权 | 依书面授权执行 |

### 3.1 Open-Meteo

Open-Meteo 的 API 数据采用 CC BY 4.0，页面展示数据时必须提供来源署名和链接。免费 API
限非商业用途，并有每日、每小时和每分钟调用上限；商业产品需要订阅相应方案。

当前武汉晚霞纵向切片属于无广告、无订阅的非商业作品版，因此准入。客户端后续必须补齐
可点击的 Open-Meteo 署名；在此之前，服务端返回的来源信息仍需完整保留。

官方依据：

- [Open-Meteo Terms](https://open-meteo.com/en/terms)
- [Open-Meteo Licence](https://open-meteo.com/en/license)
- [Model Updates and Data Availability](https://open-meteo.com/en/docs/model-updates)

### 3.2 国家气象中心雷达网页

国家气象中心雷达页面展示的产品时次通常相隔约 6 分钟，但页面同时声明，本站信息和数据
未经授权禁止下载使用。因此该网页只能作为人工查看入口，不能作为 App 的隐式图片接口，
也不能通过分析网络请求、拼接图片地址或批量缓存来规避授权。

官方依据：

- [国家气象中心雷达图](https://www.nmc.cn/publish/radar.html)

雷达纵向切片在获得许可前使用仓库内自有合成事件包，并在 UI 明确标注“历史合成回放，
非实时官方雷达”。

### 3.3 中国气象数据服务雷达产品

中国气象数据服务目录提供天气雷达组网基本反射率和组合反射率图像产品，官方页面标明
实时制作、近 24 小时、约 6 分钟更新，并允许个人实名或单位实名注册用户访问。这是研究
和历史事件准备应优先申请的正规入口。

2026-08-28，中国气象数据网客服进一步确认：天气雷达气象观测产品目录用于查看具体产品
及其共享级别，程序化数据服务应从官方数据接口市场查询和申请。该答复确认了申请路径，
但没有确认某个雷达接口已向本项目开放，也没有授予公开展示、缓存或再分发权限。因此目录
状态仍为 `restricted`，不能把网页地址、登录后下载地址或接口市场入口直接写成生产 Provider。

官方依据：

- [天气雷达气象观测产品目录](https://data.cma.cn/data-service/category/7/70102)
- [中国气象数据网数据接口市场](https://data.cma.cn/cmaOld/Market/MarketList.html)
- [天气雷达组网组合反射率图像产品](https://k.data.cma.cn/mekb/?dataCode=J.0019.0010.S001&r=data%2Fdetail)
- [天气雷达组网基本反射率图像产品](https://k.data.cma.cn/mekb/?dataCode=J.0017.0010.S001&r=data%2Fdetail)

实名账号解决的是访问资格，不自动授予在 GitHub、APK 或演示视频中公开再分发雷达图片的
权利。进入真实 Provider 开发前，必须拿到并记录以下信息：

1. 可调用的具体产品或接口编号，以及武汉区域是否覆盖；
2. 鉴权方式、调用频率、配额、费用、时间分辨率和典型延迟；
3. 缓存期限、公开展示、裁剪着色、派生动画和作品集使用边界；
4. 数据源署名、原始时次、获取时次及异常/缺帧处理要求。

若接口市场中找不到对应产品，或页面权限说明仍不清晰，使用客服提供的
`010-68407499` 在工作时间确认。仓库不保存账号、Cookie、密钥、原始受限雷达文件或邮件
中的个人信息。

### 3.4 SmartWeatherAPI 官方预警

中国天气网将 SmartWeatherAPI 描述为面向第三方的官方气象服务接口，覆盖预警、雷达、
云图等信息，历史申请说明要求提交申请表并等待审核。但项目于 2026-08-27 向页面公布的
专用邮箱发送申请时收到 SMTP 550 退信，因此当前没有经过验证的可用申请通道。版权声明
同时禁止未经书面许可复制、展示、利用或镜像网站内容与服务。

官方依据：

- [SmartWeatherAPI 开放平台](https://www.weather.com.cn/wzfw/smart/weatherapi.shtml)
- [中国天气网版权声明](https://www.weather.com.cn/wzfw/banquan.shtml)
- [中国天气网当前联系方式](https://www.weather.com.cn/wzfw/contact.shtml)

下一步通过 `service@weather.com.cn` 或 400-6000-121 请求新的申请渠道。在官方确认并完成
授权前，SkyCast 不抓取网页预警。获批后也必须原样保留发布单位、发布时间、有效期、等级
和原文，并把官方预警与 AI 短临研判分层展示。

### 3.5 FY-4B AGRI

风云卫星遥感数据服务网要求在线下载用户实名注册并经过审核。注册解决访问身份问题，
不自动证明拥有把原始文件、快视图或加工图公开再分发的权利。因此本阶段只记录产品和
访问流程，不把账号、Cookie、下载链接或原始受限数据提交到 Git。

官方依据：

- [风云卫星数据服务使用帮助](https://satellite.nsmc.org.cn/DataPortal/cn/support/faq.html)
- [FY-4 数据检索入口](https://satellite.nsmc.org.cn/DataPortal/cn/data/structure.html)

## 4. 频率、延迟与过期

目录区分三个概念：

- `expected_interval_minutes`：预期相邻产品时次，不是可用性承诺；
- `typical_latency_minutes`：产品时次到 SkyCast 可获取之间的典型延迟，未知时必须为 `null`；
- `expires_after_minutes`：SkyCast 自己的安全降级阈值。

不得为了让表格完整而编造延迟。事件驱动的官方预警使用其 `issuedAt` 与 `expiresAt`，不设置
虚假的固定发布周期。定时栅格在连续三帧未更新后默认判为过期；具体值记录在目录中。

## 5. Provider 接入流程

1. 以稳定 `source_id` 新增目录记录，并附官方 HTTPS 依据和核验日期；
2. 运行 `python tools/validate_data_sources.py`；
3. 只有 `approved` 来源才能实现或启用 Provider；
4. Provider 输出必须保留 `source_id`、观测时间、获取时间和来源链接；
5. 客户端按目录要求署名，并展示在线、过期、离线缓存或合成回放状态；
6. 条款变化、用途变化或复核到期时重新评审。

CI 和本地门禁命令：

```powershell
server\.venv\Scripts\python.exe tools\validate_data_sources.py
server\.venv\Scripts\python.exe -m pytest tools\tests\test_validate_data_sources.py
```

## 6. 下一步决策

E1-04 已使用**不包含受限官方原始资料**的武汉强对流合成事件包，定义雷达元数据、缺帧、
乱序和时间轴回放契约。下一步 E1-06 将基于该契约实现强对流单体识别与短时路径基线。
真实 CMA Provider 继续保持阻塞，直到具体接口获批并取得可审计的使用边界；届时只需实现
既有 `RadarProvider`，不改动上层领域模型。
