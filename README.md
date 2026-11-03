<div align="center">

# Easy Captcha

**Java 验证码库 · 7 种类型 · 行为轨迹校验 · 二次校验 · 零运行时依赖**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.lucky845/easy-captcha-spring-boot-starter?color=blue)](https://central.sonatype.com/artifact/io.github.lucky845/easy-captcha-spring-boot-starter)
[![Java](https://img.shields.io/badge/Java-21+-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/tests-143%20passed-brightgreen)]()

[快速开始](#快速开始) · [验证码类型](#验证码类型) · [配置](#配置) · [核心 API](#核心-api) · [更新日志](CHANGELOG.md) · [示例](#运行示例)

</div>

---

## 为什么选这个

- **依赖少。** `easy-captcha-core` 只依赖 `java.desktop`，运行时零第三方库；starter 只依赖 `spring-boot-autoconfigure`。
- **生成器无状态。** `CaptchaConfig` → `CaptchaResult` 是纯函数，同一个实例可被任意线程并发调用，不会出现"全站共用一个验证码"。
- **不误杀真人。** 行为校验默认宽容档，只拦"答案错"和"轨迹大幅横跳"。耗时下限默认关闭——被挡住的用户会直接流失。
- **答案不泄漏给前端。** 下发给浏览器的只有背景图、拼图块、尺寸、提示文字，答案始终留在服务端。
- **开箱即用。** Spring Boot 自动配置，零配置即可跑；单机用内存存储，项目里已有 Redis 时自动切换，多副本部署不会串。

## 验证码类型

| 类型 | 枚举 | 交互 | 说明 |
| :--- | :--- | :---: | :--- |
| PNG 字符 | `PNG_CAPTCHA` | 输入 | 随机字符 + 旋转 + 干扰线 |
| GIF 动图 | `GIF_CAPTCHA` | 输入 | 逐帧正弦波动 |
| 中文 PNG | `CHINESE_CAPTCHA` | 输入 | 常用汉字 |
| 中文 GIF | `CHINESE_GIF_CAPTCHA` | 输入 | 汉字 + 动画 |
| 算术题 | `ARITHMETIC_CAPTCHA` | 输入 | 4 档难度，答案按真实运算优先级计算 |
| **滑块拼图** | `SLIDER_CAPTCHA` | **拖动** | 拼图轮廓挖孔，含假缺口干扰 |
| **文字点选** | `CLICK_WORD_CAPTCHA` | **点击** | 按提示顺序点字，坐标即答案 |

> 前 5 种是静态图片；后 2 种是**行为验证码**，答案是几何信息，由前端回传轨迹、后端按容差判定，OCR 打码对它无效。

## 快速开始

### 引入依赖

```xml
<dependency>
    <groupId>io.github.lucky845</groupId>
    <artifactId>easy-captcha-spring-boot-starter</artifactId>
    <version>1.0.4</version>
</dependency>
```

<details>
<summary>Gradle</summary>

```groovy
implementation 'io.github.lucky845:easy-captcha-spring-boot-starter:1.0.4'
```

</details>

### 生成验证码

```java
@RestController
@RequestMapping("/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @GetMapping("/new")
    public Map<String, Object> create() {
        CaptchaService.CaptchaIssue issue = captchaService.issue();
        return Map.of("id", issue.id(), "image", issue.result().toBase64());
    }
}
```

响应：

```json
{
  "id": "7b5855c9c689442a8b55fb19859ec3d8",
  "image": "data:image/png;base64,iVBORw0KGgo..."
}
```

前端 `<img src="data:image/png;base64,...">` 即可显示。

### 校验

```java
@PostMapping("/verify")
public Map<String, Object> verify(@RequestBody VerifyRequest request) {
    return Map.of("valid", captchaService.validate(request.id(), request.answer()));
}

public record VerifyRequest(String id, String answer) { }
```

验证码**一次性**：校验后立即失效，无法重放。

## 行为验证码

滑块与点选需要前端回传轨迹。完整实现见 [`easy-captcha-demo`](easy-captcha-demo)。

**1. 后端下发**（`extra` 里带前端渲染所需的数据）

```java
@GetMapping("/slider")
public Map<String, Object> slider() {
    CaptchaService.CaptchaIssue issue = captchaService.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("id", issue.id());
    body.putAll(issue.result().extra());   // slider.background / slider.block / blockY ...
    return body;
}
```

**2. 前端渲染拖动**（关键：坐标必须换算到原图）

```javascript
handle.addEventListener('pointermove', e => {
  const travel = e.clientX - startX;            // 手柄在滑块条上的位移
  const imageDelta = travel / maxTravel * (imageWidth - blockWidth);
  points.push({ x: imageDelta, y: e.clientY, t: Date.now() - startTime });
});
```

**3. 后端校验**

```java
@PostMapping("/slider/verify")
public Map<String, Object> verify(@RequestBody BehaviorRequest req) {
    BehaviorAnswer answer = new BehaviorAnswer(req.answer(), req.points(), req.duration());
    BehaviorVerdict verdict = captchaService.validateBehavior(req.id(), answer);
    return Map.of("valid", verdict.passed(), "reason", verdict.reason());
}
```

<details>
<summary>前端集成的三个坑</summary>

1. **缩放方向写反** — 原图 300px 显示成 422px 时，系数是 `422/300 = 1.407`。写成 `0.711` 会让拼图块缩小一半。只保留一个方向的变量。
2. **图片与拼图块不在同一坐标系** — 拼图块是 `position:absolute`，如果图片被 flex 居中就会错位。让图片贴左、`display:block`。
3. **data URI 的 `onload` 可能不触发** — base64 内联图常直接命中缓存，需要 `requestAnimationFrame` 兜底检查。

推荐保持图片 1:1 显示，整个换算退化成纯平移。

</details>

## 二次校验

用于登录、注册、发短信等高风险动作。第一次校验换发票据，业务接口凭票放行一次：

```java
// 1. 验证码通过 → 换票
@PostMapping("/verify/ticket")
public Map<String, Object> withTicket(@RequestBody VerifyRequest req) {
    return captchaService.validateAndIssueToken(req.id(), req.answer())
            .<Map<String, Object>>map(t -> Map.of("valid", true, "ticket", t))
            .orElse(Map.of("valid", false));
}

// 2. 业务接口凭票放行（票据用后即焚）
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest req) {
    if (!captchaService.verifyToken(req.ticket())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("ok", false));
    }
    // ... 真正的登录逻辑
    return ResponseEntity.ok(Map.of("ok", true));
}
```

## 配置

所有配置项都有默认值，**全部可省略**。

```yaml
easy-captcha:
  enabled: true
  captcha-type: png_captcha        # 默认类型
  width: 200
  height: 50
  captcha-length: 4
  char-type: default
  font: actionj
  difficulty: easy

  ttl: 2m                          # 有效期
  max-entries: 10000               # 内存上限
  redis-key-prefix: easy-captcha:  # 仅在启用 Redis 存储时生效
  rate-limit-permits: 60           # 窗口内签发次数，<=0 不限流
  rate-limit-window: 1m

  # 滑块拼图（尺寸比其他类型大，块与凸舌才看得清）
  slider-width: 420
  slider-height: 210
  slider-tolerance: 8              # 偏移量容差 px
  click-tolerance: 18              # 点选每次容差 px
  # slider-background: captcha/bg.jpg   # 自定义底图，留空用内置

  # 行为轨迹阈值（默认宽容档）
  behavior:
    drag:
      min-duration-ms: 0           # 0 = 不设下限
      max-duration-ms: 300000
      min-points: 1                # 1 = 不限制
      monotonic-tolerance: 60
      check-curvature: false
    click:
      min-duration-ms: 0
      max-duration-ms: 300000
```

<details>
<summary>阈值怎么调</summary>

| 目标 | 配置项 | 推荐值 |
| :--- | :--- | :--- |
| 放宽滑块对准精度 | `slider-tolerance` | 12 ~ 15 |
| 放宽容错点击位置 | `click-tolerance` | 25 ~ 30 |
| 允许拖过头更远 | `behavior.drag.monotonic-tolerance` | 80 ~ 120 |
| 收紧、拦截来回试探 | `behavior.drag.monotonic-tolerance` | 20 ~ 30 |
| 高风险场景要求非直线轨迹 | `behavior.drag.check-curvature` | `true` |
| 要求更多轨迹采样 | `behavior.drag.min-points` | 3 ~ 5 |
| 防挂机重放 | `behavior.drag.max-duration-ms` | 120000 |
| 拦截接口批量刷取 | `rate-limit-permits` | 10 ~ 30 |

> **优先调限流，而不是收紧轨迹。** 接口被刷时降低 `rate-limit-permits` 更有效，且完全不会误杀真人。

也可用启动参数覆盖：`--easy-captcha.slider-tolerance=2`

</details>

## 核心 API

### `CaptchaService`

| 方法 | 说明 |
| :--- | :--- |
| `generate()` / `generate(type)` | 生成验证码，不落库 |
| `issue()` / `issue(type)` | 生成并存入 `CaptchaStore`，返回带 id 的结果 |
| `issue(type, key)` | 带限流的签发，超限抛 `RateLimitExceededException` |
| `validate(id, answer)` | 一次性校验 |
| `checkText(id, answer)` | 同上，返回 `BehaviorVerdict` 便于区分失败原因 |
| `validateBehavior(id, answer)` | 行为验证：答案 + 轨迹一起判定 |
| `validateAndIssueToken(id, answer)` | 校验通过换发票据 |
| `verifyToken(ticket)` | 验票，有效即消费 |
| `pendingCount()` / `remainingQuota(key)` | 未消费数量 / 剩余配额 |

### `CaptchaResult`

| 方法 | 说明 |
| :--- | :--- |
| `code()` | 答案（算术为结果；滑块为偏移量；点选为坐标串） |
| `image()` | 图片字节 |
| `toBase64()` | `data:image/png;base64,...` |
| `extra()` | 前端渲染用的公开数据，**不含答案** |
| `mimeType()` / `size()` | MIME 类型 / 字节数 |

### 行为校验裁决

`BehaviorVerdict.Reason`：`OK`、`ANSWER_MISMATCH`、`TOO_FAST`、`TIMEOUT`、`TOO_FEW_POINTS`、`ABNORMAL_DIRECTION`、`TOO_STRAIGHT`、`INVALID_INPUT`

## 扩展

### 自定义生成器

实现 `CaptchaGenerator` 并注册为 Spring Bean，同类型会覆盖内置实现：

```java
@Component
public class MyCaptchaGenerator extends AbstractCaptchaGenerator {

    @Override
    public CaptchaTypeEnum captchaType() { return CaptchaTypeEnum.PNG_CAPTCHA; }

    @Override
    public ContentTypeEnum contentType() { return ContentTypeEnum.PNG; }

    @Override
    public CaptchaResult generate(CaptchaConfig config) {
        String code = generateRandomText(config);
        // ... 自行绘制
        return new CaptchaResult(code, pngBytes, contentType());
    }
}
```

### 存储

`CaptchaStore` 负责保存已签发但未校验的验证码。内置两个实现，**按类路径自动选择**：

| 实现 | 生效条件 | 适用场景 |
| :--- | :--- | :--- |
| `InMemoryCaptchaStore` | 默认 | 单机、本地开发 |
| `RedisCaptchaStore` | 类路径有 `spring-boot-starter-data-redis` **且** 容器里有 `StringRedisTemplate` | 多副本 / 集群部署 |

也就是说：**只要你的项目本来就用了 Redis，什么都不用配**，多实例之间会自动共享验证码。

Redis 实现做的事：

- 一次性消费用 `GETDEL`（`getAndDelete`）原子完成，并发请求不会重复通过同一张验证码。
- key 自带 TTL，过期由 Redis 自己回收，不依赖应用清理。
- 值用长度前缀自描述格式序列化，**不用 JDK 反序列化**，不存在反序列化 gadget 风险。
- `clear()` 用 `SCAN` 遍历而不是 `KEYS`，不会阻塞 Redis 主线程。

自定义实现只需实现接口并注册为 Bean：

```java
@Bean
public CaptchaStore captchaStore() {
    return new MyCaptchaStore();
}
```

> 集群部署却仍然用内存存储，是这套组件最容易踩的坑：A 实例签发的验证码，被负载均衡打到 B 实例校验时必然失败。

## 运行示例

```bash
git clone https://github.com/lucky845/easy-captcha.git
cd easy-captcha
mvn clean install
java -jar easy-captcha-demo/target/easy-captcha-demo.jar
```

打开 <http://localhost:8080/> 可体验全部 7 种类型，滑块与点选是真实可交互的。

| 接口 | 说明 |
| :--- | :--- |
| `GET /captcha/image?type=PNG_CAPTCHA` | 直接返回图片 |
| `GET /captcha/base64?type=SLIDER_CAPTCHA` | 返回 id + 图片 + 行为渲染数据 |
| `POST /captcha/validate` | 校验静态验证码 |
| `POST /captcha/validate/behavior` | 校验行为验证码（含轨迹） |
| `POST /captcha/validate/ticket` | 校验并换票据 |
| `POST /captcha/business/login` | 凭票调用业务接口 |
| `GET /captcha/types` / `GET /captcha/config` | 类型列表 / 当前配置 |

## 构建与测试

```bash
mvn clean test       # 127 个测试
mvn clean install    # 安装到本地仓库
```

| 测试类 | 数量 | 覆盖点 |
| :--- | ---: | :--- |
| `CaptchaGeneratorTest` | 17 | 生成正确性、答案不冻结、全字体、边界尺寸 |
| `CaptchaConcurrencyTest` | 3 | 32 线程并发无串号 |
| `SliderCaptchaTest` | 14 | 形状完整、凸舌可见、假缺口、自定义底图 |
| `TrajectoryValidatorTest` | 34 | 真人全场景、机器特征、防探测 |
| `CaptchaServiceTest` | 11 | 自动配置、一次性校验、并发 |
| `BehaviorCaptchaTest` | 15 | 滑块/点选端到端、限流、票据、误杀回归 |
| `BehaviorConfigurationTest` | 11 | 阈值配置生效、默认宽容档 |
| `InMemoryCaptchaStoreTest` | 7 | TTL、容量、边界 |
| `RedisCaptchaStoreTest` | 11 | 序列化往返、GETDEL 一次性、TTL、损坏数据 |
| `CaptchaStoreAutoconfigurationTest` | 5 | 有/无 Redis 时的存储选择、自定义覆盖 |
| `DemoControllerTest` | 15 | HTTP 全链路、答案不泄露 |

## 环境要求

- Java 21+
- Spring Boot 3.x（仅 starter 模块需要）

## 关于依赖告警

GitHub Dependabot 会报出若干告警，需要说明的是：

- 告警**全部来自 `easy-captcha-demo` 模块的传递依赖**（tomcat、spring-webmvc、jackson、netty 等），因为 demo 是一个完整的 Spring Boot Web 示例工程。
- `easy-captcha-core` 与 `easy-captcha-spring-boot-starter` 本身依赖很干净：core 只依赖 `java.desktop`，starter 只依赖 `spring-boot-autoconfigure`。
- 引入本库**不会**把 demo 的依赖带进你的项目——发布时通过 `central-publishing-maven-plugin` 的 `excludeArtifacts` 排除了 demo 模块。

## 贡献

欢迎 Issue 与 Pull Request。提交前请确认 `mvn clean test` 全绿。

## 许可证

[Apache License 2.0](LICENSE)

<div align="center">
<sub>如果这个项目对你有帮助，欢迎点个 ⭐ Star</sub>
</div>
