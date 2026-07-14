# 高并发票务预订系统

##### Language: 中文 | [English](README.md)

一个基于 **Java、Spring Boot、PostgreSQL、Redis、Kafka 和 Spring Security** 构建的后端票务预订系统。
该项目模拟高并发抢票场景，重点关注库存一致性、防止超卖、异步订单处理，以及基于 JWT 的身份认证。

## 功能特性

* 基于 JWT 的用户注册与登录
* 活动（Event）与票种（Ticket Type）管理
* 结合已认证用户身份创建购票订单
* 使用 Redis + Lua 脚本进行库存预扣减
* 通过原子化校验与扣减避免超卖
* 基于 Kafka 的异步订单确认流程
* 订单状态流转：`PENDING`、`CONFIRMED`、`FAILED`
* 使用 PostgreSQL 悲观锁保证最终库存一致性
* 针对长时间停留在 `PENDING` 的订单提供定时补偿任务
* Kafka 消费端具备幂等处理，避免重复扣减库存
* 使用 JUnit 与 Mockito 为订单处理逻辑编写单元测试
* 支持通过 Docker Compose 一键启动本地基础设施

## 技术栈

| 分层 | 技术 |
| --- | --- |
| 后端 | Java, Spring Boot |
| 安全认证 | Spring Security, JWT |
| 数据库 | PostgreSQL, Spring Data JPA |
| 缓存 / 库存控制 | Redis, Lua Script |
| 消息队列 | Kafka |
| 测试 | JUnit, Mockito, Postman |
| DevOps | Docker, Docker Compose |

## 系统设计概览

系统通过 Redis 和 Kafka 来更安全、高效地处理高并发抢票请求。

当用户提交订单时，系统会先在 Redis 中使用 Lua 脚本检查并扣减库存。这样可以保证“校验库存”和“扣减库存”两个步骤是原子执行的，从请求入口处降低超卖风险。

当 Redis 预扣减成功后，系统会先在 PostgreSQL 中创建一条 `PENDING` 状态的订单；待数据库事务提交后，再向 Kafka 发布订单创建事件。随后由 Kafka 消费者异步处理订单，在数据库层使用悲观锁扣减最终库存，并将订单状态更新为 `CONFIRMED` 或 `FAILED`。

## 订单处理流程

```text
1. 用户登录并获取 JWT Token。
2. 用户提交下单请求。
3. Redis Lua 脚本原子化检查并扣减库存。
4. 系统在 PostgreSQL 中创建一条 PENDING 订单。
5. 事务提交后，系统向 Kafka 发送订单创建事件。
6. Kafka 消费者异步处理该订单。
7. 消费者在 PostgreSQL 中锁定对应票种记录。
8. 如果数据库库存充足：
   - 扣减数据库库存
   - 将订单状态更新为 CONFIRMED
9. 如果数据库库存不足或处理失败：
   - 在适用场景下将订单状态更新为 FAILED
   - 如有需要，对 Redis 库存进行补偿
10. 定时任务会重试长时间处于 PENDING 的订单。
```

## 为什么使用 Redis？

Redis 作为库存控制的第一道关口。

在高并发抢票场景下，许多用户可能会同时购买同一种票。如果每次请求都直接访问 PostgreSQL 做库存校验和扣减，数据库压力会很大，也更容易出现并发超卖问题。

本项目通过 Redis + Lua 脚本，将以下操作以原子方式执行：

```text
校验库存 -> 扣减库存 -> 返回结果
```

这样可以避免多个用户在并发情况下成功购买超过实际可售数量的票。

## 为什么使用 Kafka？

Kafka 用于将“提交订单”和“最终确认订单”两个阶段解耦。

系统不会在用户请求线程中同步完成所有数据库更新，而是先创建 `PENDING` 订单并发送 Kafka 事件，再由消费者异步确认订单。

这样的设计有几个好处：

* 降低接口响应延迟
* 吸收流量峰值
* 减轻数据库瞬时压力
* 为后续扩展支付、通知、分析、超时取消等异步流程预留空间

## 为什么还需要 PostgreSQL 锁？

Redis 负责在请求入口处拦截超卖，但 PostgreSQL 仍然是订单和库存持久化数据的最终真实来源。

Kafka 消费者在更新 PostgreSQL 库存时使用悲观锁，避免并发修改导致最终库存不一致，并帮助维持 Redis 库存、数据库库存与订单状态三者之间的一致性。

## 可靠性设计

### 幂等消费者

Kafka 消息存在重复投递的可能。为了避免重复消费造成库存被多次扣减，消费者会先检查订单状态：

```text
只有 PENDING 状态的订单允许继续处理。
CONFIRMED 或 FAILED 状态的订单会被跳过。
```

这样即使同一条消息被重复消费，也不会多次扣减库存。

### PENDING 订单补偿

如果某个订单长时间停留在 `PENDING`，通常意味着 Kafka 消息发送失败、消费者处理异常，或者应用在处理中途重启。

项目中提供了一个定时补偿任务，会扫描这些过期的 `PENDING` 订单，并重新向 Kafka 发布订单创建消息以触发重试。

## 主要 API 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 注册新用户 |
| POST | `/api/auth/login` | 登录并获取 JWT Token |
| POST | `/api/events` | 创建活动 |
| GET | `/api/events` | 获取全部活动 |
| GET | `/api/events/{id}` | 获取活动详情 |
| POST | `/api/ticket-types` | 创建票种 |
| GET | `/api/events/{eventId}/ticket-types` | 获取某活动下的票种列表 |
| POST | `/api/orders` | 创建购票订单 |
| GET | `/api/orders/{id}` | 查询订单状态 |

## 本地运行

### 前置要求

如果使用 Docker Compose：

* Docker Desktop
* Git

如果直接在本机运行 Spring Boot 应用：

* Java 17+
* Maven 或 Maven Wrapper
* PostgreSQL、Redis、Kafka

可选工具：

* Postman，用于接口测试

### 使用 Docker Compose 启动完整环境

```bash
docker compose up --build
```

该命令会启动完整的本地开发环境：

* Spring Boot 后端：`http://localhost:8080`
* PostgreSQL：`localhost:5433`
* Redis：`localhost:6379`
* Kafka：`localhost:9092`
* Kafka UI：`http://localhost:8081`

当你希望后端应用和所有依赖基础设施都运行在容器中时，推荐使用这种方式。

### 使用接近生产环境的 Compose 配置

先基于模板创建本地生产环境变量文件：

```bash
cp .env.prod.example .env.prod
```

然后将 `.env.prod` 中的 `DB_PASSWORD` 和 `JWT_SECRET` 改成真实值，再执行：

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up --build -d
```

这个生产风格的 Compose 配置只会暴露后端 API 端口，PostgreSQL、Redis 和 Kafka 都只保留在 Docker 内部网络中。

### 手动构建后端镜像

```bash
docker build -t ticket-booking-backend:latest .
```

如果你准备把镜像发布到 Docker Hub 或其他镜像仓库，可以把 Compose 文件中的本地构建配置替换成 `image:`，例如：

```yaml
app:
  image: your-dockerhub-username/ticket-booking-backend:0.1.0
```

这样其他人就可以直接拉取并运行镜像，而无需在本地重新构建。

### 仅启动基础设施

如果你想在 IDE 中或通过 Maven 直接运行 Spring Boot 应用，也可以只启动依赖服务：

```bash
docker compose up -d postgres redis kafka kafka-ui
```

### 启动 Spring Boot 应用

```bash
./mvnw spring-boot:run
```

在 Windows PowerShell 中：

```bash
.\mvnw spring-boot:run
```

### 运行测试

```bash
./mvnw test
```

在 Windows PowerShell 中：

```bash
.\mvnw test
```

## 测试场景

项目已通过 Postman 验证以下流程：

* 用户注册与登录
* 创建活动
* 创建票种
* 初始化 Redis 库存
* 携带 JWT Token 提交购票订单
* 验证订单状态从 `PENDING` 变为 `CONFIRMED`
* 验证 Redis 库存扣减
* 验证 PostgreSQL 库存扣减
* 验证 Kafka 消息重复消费时的幂等处理逻辑
* 验证数据库库存不足时的 Redis 库存补偿

## 后续可改进方向

* 构建统一的票务主数据模型，完善活动、场馆、场次、票种、渠道、库存池等核心对象的标准化管理、审计与同步能力
* 扩展票种全生命周期管理能力，支持上架、下架、停售、多渠道同步，以及更完整的状态流转管理
* 建设可配置的策略引擎，支持限购、风控、渠道配额、动态定价等规则的统一配置与自动执行
* 完善消息驱动架构的可靠性，包括 Kafka 幂等消费、消息重试、死信队列、补偿机制，以及 Transactional Outbox Pattern
* 推动系统向微服务架构演进，拆分订单、库存、活动、策略等领域服务，并引入 Gateway、OpenFeign、Resilience4j 强化服务治理
* 增加数据分析与业务赋能能力，沉淀运营指标、报表、告警与趋势分析，支撑业务决策
* 探索 LLM、RAG、Agent 在票务运营场景中的应用，例如智能问答、异常分析、运营建议与自动化问题处理
* 提升工程化成熟度，包括基于 Docker Compose 的多服务编排、CI/CD、可观测性建设，以及 Testcontainers 集成测试

## 项目亮点

这个项目主要体现了以下后端工程能力：

* 高并发库存控制
* 分布式系统可靠性设计
* 异步事件驱动处理
* 数据库事务处理
* 缓存与数据库一致性保障
* JWT 身份认证
* 单元测试与后端接口验证
* 基于 Docker Compose 的应用容器化与本地环境编排
