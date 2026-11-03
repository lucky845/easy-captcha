# Changelog

本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [1.0.4] - 2025-11-03

### 新增

- **Redis 存储**。新增 `RedisCaptchaStore`，集群部署时多实例共享验证码。
  类路径存在 `spring-boot-starter-data-redis` 且容器里有 `StringRedisTemplate` 时**自动启用**，无需额外配置；
  自定义 `CaptchaStore` Bean 始终优先。
  - 一次性消费用 `GETDEL` 原子完成，并发下同一张验证码只会通过一次。
  - 值序列化用长度前缀自描述格式，不经 JDK 反序列化，无 gadget 风险。
  - `clear()` 用 `SCAN` 而非 `KEYS`，不阻塞 Redis。
- 新增配置项 `easy-captcha.redis-key-prefix`（默认 `easy-captcha:`）。

### 修复

- 升级传递依赖，消除 19 个安全告警中的 17 个：
  `tomcat-embed-core` → 10.1.59、`jackson-databind` / `jackson-core` → 2.21.7、`log4j-api` → 2.25.5。
  剩余 2 个 `spring-webmvc` 告警需等 Spring Framework 6.2.20 发布，当前上游尚未释出。
- `CaptchaService` 类型推断兼容字符串形式的类型标记，自定义存储或跨版本缓存不再被误判成普通验证码。

## [1.0.3] - 2025-11-03

### 修复

- `easy-captcha-spring-boot-starter` 在 `dependencyManagement` 中被错误锁定为 `provided`，
  导致下游引入后拿不到 `spring-boot-autoconfigure`、自动配置静默失效。已移除。
- 发布时 `easy-captcha-demo` 被一并推到 Central。改用 `excludeArtifacts` 排除，
  发布物现在只含 `easy-captcha`、`easy-captcha-core`、`easy-captcha-spring-boot-starter`。

### 其他

- 构建：GPG 口令改用 `MAVEN_GPG_PASSPHRASE` 环境变量，不再通过 `-Dgpg.passphrase` 泄漏到进程列表。

## [1.0.2] - 2025-10

### 修复

- 打包白名单误排除 `AutoConfiguration.imports` 与内置字体，导致自动配置不生效、字体加载失败。

### 新增

- 行为验证码：滑块拼图（`SLIDER_CAPTCHA`）与文字点选（`CLICK_WORD_CAPTCHA`）。
- 轨迹校验：方向单调性、采样点数、耗时、弯曲度，阈值全部可配置，默认宽容档。
- 二次校验票据：`validateAndIssueToken` / `verifyToken`，票据一次性消费。
- 接口限流：按 key 的固定窗口计数。

## [1.0.0] - 2025-09

首个可用版本。

- 5 种静态验证码：PNG、GIF、中文 PNG、中文 GIF、算术。
- 生成器无状态，`CaptchaConfig` → `CaptchaResult` 纯函数。
- Spring Boot 自动配置，零外部依赖。
