# PawHome · 城市流浪动物救助站领养与物资管理平台

基于 Spring Cloud Alibaba 的微服务实践项目，覆盖动物建档、领养申请与审核、物资库存、爱心捐赠、志愿排班五个业务域。

7 个业务服务 + 1 个 API 网关，每个服务独占一个 MySQL 库，共 7 库 17 表，对外提供 58 个接口。

## 技术栈

| 类别 | 技术 |
| --- | --- |
| 语言 / 框架 | Java 17、Spring Boot 3.2.5、Spring Cloud 2023.0.1 |
| 微服务套件 | Spring Cloud Alibaba 2023.0.1.0、Nacos 2.3、Sentinel 1.8.7 |
| 服务注册 | Spring Cloud Netflix Eureka（8761）+ Nacos（配置中心） |
| 配置中心 | Spring Cloud Config Server（8888，native 模式） |
| 网关 | Spring Cloud Gateway（路由 / JWT 鉴权 / 角色校验 / 限流） |
| 数据访问 | MyBatis-Plus 3.5.5、MySQL 8.0 |
| 缓存与锁 | Redis 7.x、Redisson 3.27.2 |
| 服务调用 | OpenFeign + Spring Cloud LoadBalancer |
| 前端 | Vue 3 + Element Plus（静态页，由网关托管） |
| 文档 | Knife4j 4.5.0 |

## 模块结构

```
pawhome/
├── pawhome-eureka       8761   Eureka 服务注册中心（教材 11.4.2）
├── pawhome-config       8888   Config 配置中心（教材 11.4.6）
├── pawhome-gateway      8080   API 网关：路由、鉴权、权限、限流、静态资源
├── pawhome-auth         8081   认证与用户管理
├── pawhome-animal       8082   动物档案与封面图上传
├── pawhome-adoption     8083   领养申请与审核
├── pawhome-supply       8084   物资库存与出入库流水
├── pawhome-donation     8085   爱心捐赠与入库补偿
├── pawhome-volunteer    8086   志愿排班与打卡
├── pawhome-notify       8087   站内消息
├── pawhome-common             公共模块：拦截器、工具、异常、统一返回
├── pawhome-api                Feign 契约与 DTO
└── run/                       一键启动脚本与配置
```

## 架构要点

- **注册与配置分离**：服务注册用 Eureka，网关通过 `lb://` 从 Eureka 拉取实例；Nacos 保留配置中心能力（SupplyProperties 动态刷新）。
- **网关是唯一入口**：所有请求经 8080 进入，按 `/api/{服务}` 前缀路由，`StripPrefix=1` 去掉前缀。JWT 在网关解析，身份以请求头传给下游。
- **数据库按服务隔离**：禁止跨库 JOIN，跨域数据一律走 Feign。
- **并发安全**：库存扣减用 Redisson 分布式锁 + 数据库 CAS 双重保护。
- **最终一致**：捐赠入库失败写入补偿表，定时任务按捐赠单号幂等重试。
- **旁路隔离**：消息通知失败只记日志，不影响主流程。

## 快速开始

### 1. 环境要求

- JDK 17
- Maven 3.8+
- MySQL 5.7 / 8.0
- Redis 7.x
- Nacos 2.3.x（单机模式）
- Sentinel Dashboard 1.8.7（可选）

### 2. 初始化数据库

按编号顺序执行 `05-环境与部署/sql/` 下的 8 个脚本（00 为建库脚本，必须最先执行）：

```bash
mysql -uroot -p < 00-create-databases.sql
mysql -uroot -p < 01-pawhome_auth.sql
# ... 依次执行至 07
```

### 3. 修改配置

编辑 `04-项目源码/pawhome/run/config.bat`，填写本机路径与数据库密码：

```bat
set "JDK17=C:\Program Files\Java\jdk-17"
set "REDIS_DIR=D:\redis"
set "NACOS_DIR=D:\nacos"
set "SENTINEL_DIR=D:\sentinel"
set "MYSQL_USERNAME=root"
set "MYSQL_PASSWORD=你的密码"
set "NACOS_USERNAME=nacos"
set "NACOS_PASSWORD=nacos"
```

### 4. 编译与启动

```bat
cd 04-项目源码\pawhome\run
1-build.bat     :: 编译，首次约 5-15 分钟
2-start.bat     :: 一键启动 11 个进程，约 90 秒
3-stop.bat      :: 停止全部
```

启动完成后：

| 入口 | 地址 | 账号 |
| --- | --- | --- |
| 领养门户 | http://localhost:8080/ | user01 / 123456 |
| 管理后台 | http://localhost:8080/admin.html | admin / 123456 |
| Eureka 面板 | http://localhost:8761 | — |
| Config 配置 | http://localhost:8888/pawhome-supply/default | — |
| Nacos 控制台 | http://localhost:8848/nacos | nacos / nacos |
| Sentinel 控制台 | http://localhost:8858 | sentinel / sentinel |

初始账号：`admin`（管理员）、`staff01`（员工）、`vol01`（志愿者）、`user01`（领养人），密码均为 `123456`。

## 业务演示路径

1. **领养流程**：门户浏览待领养动物 → 提交申请 → 后台审核通过 → 动物状态变更 → 申请人收到站内消息
2. **物资流程**：后台物资入库 / 出库 → 库存变化并生成流水 → 跌破预警值自动通知管理员
3. **捐赠流程**：门户登记捐赠 → 支付 → 物资捐赠自动触发入库（失败进补偿表重试）
4. **并发验证**：两个窗口同时对同一物资出库，第二个提示「该物资正在被其他人操作」

## 已知限制

- 网关为单实例，生产环境需前置 Nginx 并部署多副本
- 图片存本地磁盘，生产环境应接入对象存储
- 支付为模拟实现
- 未引入消息队列，跨服务一致性用补偿表 + 定时任务保证

## 文档

仓库根目录包含需求分析、设计文档、测试报告、使用手册、技术架构说明、答辩讲稿等配套文档（.docx）。

## License

本项目为课程设计作品，仅供学习参考。
