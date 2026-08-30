# 万象无人机云控平台（WanXiang）

参考 DJI 上云 API（Cloud API）实现的无人机 / 机场后台管理平台：机场与无人机通过 MQTT 接入平台，平台提供多租户的设备管理、实时遥测监控、健康告警、固件升级、远程日志等能力，并通过 WebSocket 向前端实时推送设备数据。

> 生态位：对应 DJI 官方「上云 API」方案中的云端服务端（DJI Cloud API Demo 的自研替代），配合 DJI Pilot 2 / 机场（Dock）使用。

## 核心功能

| 功能 | 说明 |
| --- | --- |
| 多租户机构管理 | 机构（租户）+ 绑定码，现场在 Pilot 中填绑定码即可把设备绑入机构 |
| 用户与角色权限 | 手机号验证码登录（Sa-Token），4 个内置角色 + 权限点体系 |
| 设备管理 | 机场-无人机-负载设备树、列表/详情/重命名/解绑、在线状态 |
| 实时遥测（OSD） | 设备位置、电量、航向等遥测实时入库 Redis 并推送前端 |
| 设备状态与属性设置 | 设备增量状态落库，支持远程设置机场属性（如静音模式） |
| 健康告警（HMS） | 设备告警码翻译为中文文案入库，支持分页筛选查询 |
| 空域安全（AirSense） | 机场 ADS-B 检测到周边民航飞机时告警入库（危险等级 0-4），支持按设备/等级筛选 |
| 直播 | 下发开始/停止/清晰度/镜头/相机切换指令，回执实时推送前端；码流设备直推流媒体服务器（RTMP/GB28181/WHIP），不经平台 |
| 媒体管理 | 机场任务媒体直传对象存储（凭证按需下发），平台维护元数据索引：列表/删除/任务关联/拍摄位置，新文件实时推送 |
| 配置更新 | 响应设备 config 请求下发上云凭据（AppId/AppKey/AppLicense/NTP），校验配置格式与范围 |
| 自定义飞行区 | 作业区/限飞区文件管理与下发同步，飞行器绕行/限界；文件存平台库经 HTTP 下载，进度与飞行告警实时推送 |
| 航线管理 | KMZ 航线文件上传/管理（WPML 标准），设备经任务资源获取拉取，文件存平台库经 HTTP 下载 |
| 飞行任务 | 立即/定时任务创建下发（prepare→execute）、进度/断点/媒体数量实时跟踪、取消；DB 驱动调度器重启不丢任务 |
| 固件升级 | 下发 OTA 升级任务，实时接收升级进度并推送前端 |
| 远程日志 | 拉取设备日志文件列表、下发上传（凭证直传 OSS/MinIO）、取消上传 |

## 整体架构

```
DJI 机场 / Pilot 2 ──MQTT──▶ MQTT Broker(EMQX 等) ──▶ 万象后台(Spring Boot)
                                                          │
                        前端 ◀──WebSocket(STOMP)──────────┤
                        前端 ◀──HTTP REST────────────────┤
                                                          ▼
                                              MySQL(业务数据) + Redis(会话/遥测/心跳)
```

设备侧只与 MQTT Broker 交互，后台作为唯一的服务端完成协议处理、业务落库和前端推送。

## 技术栈

| 类别 | 选型 |
| --- | --- |
| 语言 / 框架 | Java 21、Spring Boot 3.2.5 |
| 设备接入 | Spring Integration MQTT（Paho）、MQTT QoS1 |
| 前端推送 | Spring WebSocket（STOMP），端点 `/ws` |
| ORM | MyBatis-Plus 3.5.7 |
| 认证鉴权 | Sa-Token 1.39.0（会话存 Redis） |
| 存储 | MySQL 8、Redis 7 |
| 工具库 | Hutool、Guava、Fastjson2、Lombok |
| 部署 | Docker 多阶段构建 + docker-compose（MySQL / Redis） |

## 模块结构

```
WanXiang（父工程）
├── wanxiang-common   通用基建：统一返回/错误码/异常、日志切面与链路追踪、MyBatis-Plus 与 Sa-Token 配置
├── wanxiang-user     用户域：机构、用户、角色权限、登录认证（多租户隔离的收口在 UserContext）
├── wanxiang-device   设备域：MQTT 接入与消息分发、设备绑定/拓扑/OSD/状态/告警/固件/远程日志、WebSocket 推送
└── wanxiang-app      启动模块：WanXiangApplication、application.yml、db/schema.sql、logback 配置
```

依赖方向：`app → user/device → common`。业务代码按「业务域 → 功能包」两级分包（如 `device` 下的 `alarm`/`bind`/`firmware`/`status`…）。

## 快速开始

1. **起依赖**（MySQL 映射宿主机 3307，Redis 6379）：

   ```bash
   docker-compose up -d
   ```

2. **配置**：按需修改 `wanxiang-app/src/main/resources/application.yml`：
   - 数据库 / Redis 连接（默认对准 docker-compose 起的实例）；
   - `wanxiang.mqtt.*`：MQTT broker 地址与账号（对应 DJI 开发者平台「上云」配置页的网关信息）；**不配 broker-url 也能启动**，只是不接入设备；
   - `wanxiang.dji.*`：上云 API 应用凭据（AppId / AppKey / AppLicense / NTP），设备 License 校验时下发；
   - `wanxiang.oss.*`：远程日志上传用的对象存储凭证（阿里云 OSS / AWS S3 / MinIO）。

3. **启动**：建表与种子数据由 `schema.sql` 在启动时自动执行（幂等）。运行 `wanxiang-app` 的 `WanXiangApplication`，或：

   ```bash
   mvn clean package -DskipTests -pl wanxiang-app -am
   java -jar wanxiang-app/target/wanxiang-app.jar
   ```

4. **登录验证**：验证码为 mock 固定值 `123456`，预置账号：

   | 手机号 | 角色 | 机构 |
   | --- | --- | --- |
   | 13800000000 | 平台超管 SUPER_ADMIN | 无（跨机构） |
   | 13900000001 | 机构管理员 ADMIN | 演示机构 |
   | 13900000002 | 机构操作员 OPERATOR | 演示机构 |

服务端口 `8080`，WebSocket 端点 `ws://host:8080/ws?satoken={登录token}`。

## 文档导航

全部文档位于 `docs/`，按受众分为两个目录，均可直接在 IDE / Git 平台预览（图表使用 Mermaid）：

**产品文档（`docs/product/`）——面向管理层与产品人员，不需要编程背景：**

- [产品功能设计](docs/product/产品功能设计.md)：平台解决什么问题、每个功能怎么设计的、业务流程图、角色权限矩阵与后续规划。

**技术文档（`docs/tech/`）——面向开发人员：**

- [技术架构设计](docs/tech/技术架构设计.md)：总体架构、模块划分、技术选型理由、关键技术决策（为什么这么设计）、配置项与部署、日志体系。
- [核心流程与时序图](docs/tech/核心流程与时序图.md)：MQTT 主题总表 + 每条核心链路的时序图（绑定、上下线、OSD、固件、告警……），图上标注代码入口。
- [设备属性设计](docs/tech/设备属性设计.md)：DJI 属性机制（osd/state/property set 三条链路）与平台的处理设计、Dock 3 属性分组概览与建模取舍。
- [直播功能设计](docs/tech/直播功能设计.md)：控制面/数据面分工、开始到停止的全链路时序图、推流地址生成规则、SRS 本地联调。
- [媒体管理设计](docs/tech/媒体管理设计.md)：凭证下发 → 设备直传 → 上传结果入库的全链路时序图、去重与机构隔离、MinIO 本地联调。
- [自定义飞行区设计](docs/tech/自定义飞行区设计.md)：作业区/限飞区同步链路时序图、文件存库+平台下载的设计取舍、机构级文件隔离。
- [航线与任务设计](docs/tech/航线与任务设计.md)：任务生命周期状态机、调度器设计（24h/2min 窗口）、prepare 字段对照、与媒体模块的联动。
- [数据库设计](docs/tech/数据库设计.md)：ER 图、每张表的业务含义与设计要点、种子数据说明。
- [开发指南](docs/tech/开发指南.md)：环境搭建、代码分层约定、错误码/日志/权限的使用方式、新增接口/消息类型/数据表的步骤、本地模拟设备联调与问题排查。

> REST API 文档不在本仓库维护（另有存放位置）。
