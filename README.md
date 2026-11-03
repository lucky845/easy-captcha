# Easy Captcha

一个零依赖、线程安全的 Java 验证码库，附带 Spring Boot Starter。覆盖市面上主流验证码方案：静态图片、**行为验证码**、**轨迹校验**、**二次校验令牌**、**限流防刷**。

[![Java](https://img.shields.io/badge/Java-21-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)]()
[![License](https://img.shields.io/badge/license-Apache%202.0-green)](LICENSE)

---

## 特性

### 验证码类型（7 种）

| 类型 | 枚举值 | 交互 | 说明 |
| --- | --- | --- | --- |
| PNG 字符 | `PNG_CAPTCHA` | 输入 | 随机字符 + 旋转 + 干扰线 |
| GIF 动图 | `GIF_CAPTCHA` | 输入 | 逐帧正弦波动 |
| 中文 PNG | `CHINESE_CAPTCHA` | 输入 | 常用汉字 |
| 中文 GIF | `CHINESE_GIF_CAPTCHA` | 输入 | 汉字 + 动画 |
| 算术题 | `ARITHMETIC_CAPTCHA` | 输入 | 4 档难度，答案按真实优先级计算 |
| **滑块拼图** | `SLIDER_CAPTCHA` | **拖动** | 拼图轮廓挖孔，答案不落前端 |
| **文字点选** | `CLICK_WORD_CAPTCHA` | **点击** | 按提示顺序点字，坐标即答案 |

### 核心能力

- **无状态生成器**：每次调用产生全新结果，同一实例可被任意线程并发复用，不存在验证码串号
- **行为轨迹校验**：不只判断"点得对不对"，还判断"是不是人点的"——耗时、轨迹点数、方向、弯曲度四维过滤脚本
- **防答案探测**：轨迹不像人时优先返回行为原因，攻击者无法通过返回值差异二分搜索出正确答案
- **一次性校验**：答案与图片绑定返回，校验后立即失效
- **二次校验令牌**：校验通过换发短命票据，业务接口（登录/注册/发短信）凭票放行一次，防跳过、防重放
- **限流防刷**：按 IP 限制签发频率，超限返回 429
- **密码学安全随机**：`SecureRandom` 每线程独立实例
- **开箱即用**：Spring Boot 自动配置，不依赖 Redis 等外部服务
- **配置校验**：非法宽高、长度、字号在生成时立即报错并指明字段

## 模块

| 模块 | 说明 |
| --- | --- |
| `easy-captcha-core` | 纯 Java 生成器 + 轨迹校验，仅依赖 `java.desktop` |
| `easy-captcha-spring-boot-starter` | 自动配置、`CaptchaService`、内存存储、限流 |
| `easy-captcha-demo` | 可运行的示例工程，含完整交互前端 |

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.lucky845</groupId>
    <artifactId>easy-captcha-spring-boot-starter</artifactId>
    <version>1.0.3-SNAPSHOT</version>
</dependency>
```

### 2. 配置（可选）

```yaml
easy-captcha:
  enabled: true
  captcha-type: png_captcha      # 默认类型
  width: 200
  height: 50
  captcha-length: 4
  char-type: default
  font: actionj
  difficulty: easy

  ttl: 2m                        # 验证码有效期
  max-entries: 10000             # 内存最大条数

  rate-limit-permits: 60         # 窗口内允许签发次数，<=0 表示不限流
  rate-limit-window: 1m

  # ---- 滑块拼图 ----
  slider-width: 300              # 滑块专用宽度（比其他类型大，块才看得清）
  slider-height: 150             # 滑块专用高度
  slider-tolerance: 8            # 偏移量允许误差（px）
  # slider-background: captcha/bg.jpg   # 自定义底图（classpath 路径），留空用内置

  # ---- 行为验证阈值（全部可调，默认宽容档）----
  click-tolerance: 18            # 点选每次点击允许误差（px）
  behavior:
    drag:                        # 拖动型（滑块）
      min-duration-ms: 0         # 0 = 不设下限（推荐，见下方说明）
      max-duration-ms: 300000    # 5 分钟，防挂机重放
      min-points: 1              # 1 = 不限制轨迹点数
      monotonic-tolerance: 60    # 允许拖过头 60px 再拉回
      check-curvature: false     # 弯曲度检查，默认关闭
      min-curvature: 1.005       # 仅在 check-curvature=true 时生效
    click:                       # 点击型（点选）
      min-duration-ms: 0
      max-duration-ms: 300000
```

### 阈值怎么调

所有阈值都能在配置文件里改，也可以用启动参数覆盖：

```bash
java -jar app.jar --easy-captcha.slider-tolerance=2
```

| 想达到的效果 | 改什么 | 推荐值 |
| --- | --- | --- |
| 放宽滑块对准精度 | `slider-tolerance` | 12 ~ 15 |
| 放宽容错点击位置 | `click-tolerance` | 25 ~ 30 |
| 允许用户拖过头更远 | `behavior.drag.monotonic-tolerance` | 80 ~ 120 |
| 收紧、拦截来回试探 | `behavior.drag.monotonic-tolerance` | 20 ~ 30 |
| 高风险场景要求非直线轨迹 | `behavior.drag.check-curvature` | `true` |
| 要求更多轨迹采样 | `behavior.drag.min-points` | 3 ~ 5 |
| 防挂机重放 | `behavior.drag.max-duration-ms` | 120000 |
| 拦截接口批量刷取 | `rate-limit-permits` | 10 ~ 30 |

> **优先调限流，而不是收紧轨迹。** 接口被刷时，把 `rate-limit-permits` 降下来
> 比收紧轨迹判定有效得多，而且完全不会误杀真人。

完整属性清单见 `META-INF/spring-configuration-metadata.json`，IDE 里可自动补全。

### 3. 静态验证码

```java
@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    /** 返回 { id, image } */
    @GetMapping("/new")
    public Map<String, Object> create() {
        CaptchaService.CaptchaIssue issue = captchaService.issue();
        return Map.of("id", issue.id(), "image", issue.result().toBase64());
    }

    /** 校验一次性答案 */
    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestBody VerifyRequest request) {
        return Map.of("valid", captchaService.validate(request.id(), request.answer()));
    }

    public record VerifyRequest(String id, String answer) { }
}
```

### 4. 行为验证码（滑块 / 点选）

```java
/** 1. 下发：把 slider 渲染数据一起返回，前端据此画出拼图和滑块条 */
@GetMapping("/slider")
public Map<String, Object> slider() {
    CaptchaService.CaptchaIssue issue = captchaService.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("id", issue.id());
    body.putAll(issue.result().extra());   // 含 slider.background / block / blockY
    return body;
}

/** 2. 校验：把前端采集的轨迹一起提交 */
@PostMapping("/slider/verify")
public Map<String, Object> sliderVerify(@RequestBody BehaviorRequest request) {
    BehaviorAnswer answer = new BehaviorAnswer(
            request.answer(),
            request.points().stream()
                    .map(p -> new BehaviorAnswer.Point(p.x(), p.y(), p.t()))
                    .toList(),
            request.duration());
    BehaviorVerdict verdict = captchaService.validateBehavior(request.id(), answer);
    return Map.of("valid", verdict.passed(), "reason", verdict.reason(), "detail", verdict.detail());
}
```

前端需要采集的轨迹数据（每次 `pointermove` 记录一条）：

```javascript
handle.addEventListener('pointermove', e => {
    points.push({ x: e.clientX - startX, y: e.clientY - startY, t: Date.now() - startTime });
});
```

### 5. 二次校验（推荐用于登录/注册）

```java
/** 第一次：验证码验过，换一张一次性票据 */
@PostMapping("/verify/ticket")
public Map<String, Object> verifyWithTicket(@RequestBody VerifyRequest request) {
    return captchaService.validateAndIssueToken(request.id(), request.answer())
            .map(ticket -> Map.of("valid", true, "ticket", ticket))
            .orElseGet(() -> Map.of("valid", false));
}

/** 第二次：业务接口凭票放行，票据用后即焚 */
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    if (!captchaService.verifyToken(request.ticket())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("ok", false));
    }
    // 真正的登录逻辑
    return ResponseEntity.ok(Map.of("ok", true));
}
```

## 核心 API

### `CaptchaService`

| 方法 | 说明 |
| --- | --- |
| `generate()` / `generate(type)` | 生成验证码，不落库 |
| `issue()` / `issue(type)` | 生成并存入 `CaptchaStore`，返回带 id 的结果 |
| `issue(type, key)` | 带限流的签发，超限抛 `RateLimitExceededException` |
| `validate(id, answer)` | 一次性校验静态答案 |
| `checkText(id, answer)` | 同上，但返回 `BehaviorVerdict` 便于区分失败原因 |
| `validateBehavior(id, answer)` | 行为验证：答案 + 轨迹一起判定 |
| `validateAndIssueToken(id, answer)` | 校验通过换发票据 |
| `verifyToken(ticket)` | 验票，有效即消费 |
| `pendingCount()` | 当前未消费的验证码数量 |
| `remainingQuota(key)` | 剩余配额 |

### `CaptchaResult`

| 方法 | 说明 |
| --- | --- |
| `code()` | 答案（算术为结果；滑块为偏移量；点选为坐标串） |
| `image()` | 图片字节 |
| `extra()` | 前端渲染用的公开数据，**不含答案** |
| `mimeType()` | 如 `image/png` |
| `toBase64()` | `data:image/png;base64,...` |

### 行为校验裁决

`BehaviorVerdict.Reason` 可选值：

| 原因 | 含义 |
| --- | --- |
| `OK` | 通过 |
| `ANSWER_MISMATCH` | 答案不匹配 |
| `TOO_FAST` | 操作过快，疑似脚本 |
| `TIMEOUT` | 操作超时 |
| `TOO_FEW_POINTS` | 轨迹点太少 |
| `ABNORMAL_DIRECTION` | 轨迹方向异常 |
| `TOO_STRAIGHT` | 轨迹过于笔直 |
| `INVALID_INPUT` | 输入为空或格式非法 |

### `CaptchaGenerator`（core 层）

| 方法 | 说明 |
| --- | --- |
| `captchaType()` | 验证码类型 |
| `contentType()` | 输出 MIME 类型 |
| `generate(CaptchaConfig)` | 生成一份新验证码 |

内置实现：`PngCaptchaGenerator`、`GifCaptchaGenerator`、`ChineseCaptchaGenerator`、`ChineseGifCaptchaGenerator`、`ArithmeticCaptchaGenerator`、`SliderCaptchaGenerator`、`ClickWordCaptchaGenerator`。

## 安全设计要点

### 为什么不设耗时下限（重要）

这个库最初用"操作耗时"判断机器人，设过 500ms、150ms、100ms 三档阈值，**每一档都在真实场景里误杀用户**：

| 被误杀的场景 | 原因 |
| --- | --- |
| 手快的用户 | 3 次点击总共 400ms，低于 500ms 阈值 |
| 低采样率设备 | 浏览器只上报 3 个轨迹点，低于"最少 3 点"刚好卡住 |
| 拖直线的用户 | 弯曲度算出来正好 1.0，低于 1.02 被判"轨迹笔直" |
| 内网 / CDN 回源 | 签发到提交只经过 50ms，低于任何下限都要被拒 |

根因是：**"从签发到提交"的时间主要由网络决定，与用户是不是机器人无关**。拿它做下限，本质上是在猜网络速度。

现在的设计原则是**宁可漏放，不可误杀**：

- **不设耗时下限**（`behavior-min-duration-ms: 0`）
- 只保留**上限**，用于拦截"拿到验证码挂机很久再提交"的重放
- 点数、弯曲度、方向这些几何规则**只在严格模式下才启用**
- 真正可靠的机器特征是**答案错误**与**轨迹大幅横跳**，这两条已经覆盖暴力破解和脚本拖拽

误杀真人的代价远大于偶尔漏放一个脚本——被挡住的用户会直接流失，而漏放的脚本还有限流、一次性销毁、二次票据三层兜底。

### 滑块拼图的设计要点

**形状必须严格落在画布内。** 拼图块是"按形状裁出来的一小块图"。如果形状的包围盒超出画布，
凸出的部分会被画布裁掉，最终得到的是一个方块——而缺口仍按完整形状绘制，
两者对不上，用户永远对不准。这也是早期版本"怎么拖都偏差"的根因。
现在 `PuzzleShape` 会先把基础方块内缩一个凸舌尺寸，保证凸舌正好顶到画布边缘。

**必须有假缺口。** 只有真缺口时，脚本做一次模板匹配就能定位目标。加上 2 个假缺口后，
候选位置变多，必须结合形状朝向才能辨别，而真人一眼就能看出哪个缺口配了滑块。

**滑块要单独配尺寸。** 200x50 的图上块只有 21px、凸舌仅 3px，肉眼分辨不出形状差异。
因此滑块默认使用 `slider-width: 300` / `slider-height: 150`，块约 63px、凸舌约 10px。
如果配得太小，生成器会打一条警告日志提示调整。

**支持自定义底图。** 内置底图是程序生成的渐变纹理；业务方想用自己的素材时：

```yaml
easy-captcha:
  slider-background: captcha/bg.jpg   # 放在 src/main/resources 下
```

图片会按 `slider-width` × `slider-height` 缩放并居中裁剪，不会变形。
也可以编程方式设置：

```java
SliderCaptchaGenerator.setCustomBackground(inputStream);
```

### 前端集成的三个坑（都踩过）

如果自己写前端，这三处最容易出错，表现都是"滑块和缺口对不上"：

**1. 缩放方向写反。** 图片被 CSS 拉伸到 422px 显示、而原图 300px 时，
「原图→显示」的系数是 `422/300 = 1.407`。写成 `300/422 = 0.711` 会让拼图块缩小一半、
位置也偏小。**只保留一个方向的系数**（统一用 `displayScale = 显示宽度/原图宽度`），
所有换算都乘它，不要同时存在正反两个变量。

**2. 图片与拼图块不在同一坐标系。** 拼图块是 `position:absolute`，它的 `left/top`
相对最近的定位祖先。如果图片被 flex 居中，就会与块差开一段距离。
本库的做法是让图片铺满容器左缘、`display:block`，**不用 flex 居中**。

**3. data URI 的 onload 可能不触发。** 图片是 base64 内联的，浏览器常直接命中缓存，
`img.onload` 不再回调，`setupSlider` 不执行、页面上残留上一张的拼图块。
需要额外兜一次检查：`requestAnimationFrame(() => { if (img.complete) run(); })`。

**推荐保持图片 1:1 显示**（不设置 `width:100%`）。这样整个坐标换算退化成纯平移，
出错概率最低；容器太窄时用 `max-width:100%` 等比缩小，再由 JS 按实际显示宽度算比例。

### 为什么不先比对答案

滑块校验**先验轨迹、后验答案**。如果反过来，攻击者反复提交不同偏移量并观察返回的是"答案错"还是"轨迹错"，就能二分搜索出正确位置。当前实现中，轨迹异常时一律返回行为原因，答案对错完全不泄露。

### 耗时只信服务端时钟

客户端上报的 `duration` 和轨迹点的 `t` 都可以被脚本任意伪造，因此**绝不参与判定**。只有"服务端签发到提交之间真实经过多久"是不可伪造的（`CaptchaStore` 在保存时记录 `issuedAt`）。

### 答案永远不会出现在下发数据里

行为验证码的 `extra` 只包含背景图、拼图块、尺寸、提示文字等**可以公开**的渲染信息。测试中有专门断言防止答案泄露进前端 payload。

### 一次性的三层保证

1. 验证码 id 取出即删除，无法重放
2. 二次校验票据用后即焚
3. 限流限制单位时间内可签发数量

## 示例工程

```bash
mvn clean install
java -jar easy-captcha-demo/target/easy-captcha-demo.jar
```

打开 <http://localhost:8080/> 可体验全部 7 种类型，滑块与点选是真实可交互的。

| 接口 | 说明 |
| --- | --- |
| `GET /captcha/image?type=PNG_CAPTCHA` | 直接返回图片 |
| `GET /captcha/base64?type=SLIDER_CAPTCHA` | 返回 id + 图片 + 行为渲染数据 |
| `POST /captcha/validate` | 校验静态验证码 |
| `POST /captcha/validate/behavior` | 校验行为验证码（含轨迹） |
| `POST /captcha/validate/ticket` | 校验并换票据 |
| `POST /captcha/business/login` | 凭票调用业务接口 |
| `GET /captcha/types` | 列出类型，标注是否行为验证码 |
| `GET /captcha/config` | 当前配置与剩余配额 |

## 自定义扩展

### 自定义生成器

实现 `CaptchaGenerator` 并注册为 Spring Bean，自动覆盖同类型内置实现：

```java
@Component
public class MyCaptchaGenerator extends AbstractCaptchaGenerator {

    @Override
    public CaptchaTypeEnum captchaType() {
        return CaptchaTypeEnum.PNG_CAPTCHA;
    }

    @Override
    public ContentTypeEnum contentType() {
        return ContentTypeEnum.PNG;
    }

    @Override
    public CaptchaResult generate(CaptchaConfig config) {
        String code = generateRandomText(config);
        // ... 自行绘制
        return new CaptchaResult(code, pngBytes, contentType());
    }
}
```

### 自定义存储

默认 `InMemoryCaptchaStore`。集群部署时提供 `CaptchaStore` Bean 即可：

```java
@Bean
public CaptchaStore captchaStore(RedisTemplate<String, CaptchaResult> redis) {
    return new RedisCaptchaStore(redis);
}
```

## 构建与测试

```bash
mvn clean test       # 127 个测试
mvn clean install    # 构建并安装
```

覆盖范围：

| 测试类 | 数量 | 覆盖点 |
| --- | --- | --- |
| `CaptchaGeneratorTest` | 17 | 生成正确性、答案不冻结、全字体、边界尺寸 |
| `SliderCaptchaTest` | 14 | 形状完整、凸舌可见、假缺口、自定义底图 |
| `CaptchaConcurrencyTest` | 3 | 32 线程并发无串号 |
| `TrajectoryValidatorTest` | 34 | 真人全场景、机器特征、防探测、度量 |
| `CaptchaServiceTest` | 11 | 自动配置、一次性校验、并发 |
| `BehaviorCaptchaTest` | 15 | 滑块/点选端到端、限流、票据、误杀回归 |
| `BehaviorConfigurationTest` | 11 | 阈值配置化、默认宽容档、非法值夹取 |
| `InMemoryCaptchaStoreTest` | 7 | TTL、容量、边界 |
| `DemoControllerTest` | 15 | HTTP 全链路、答案不泄露、快速提交 |

## 要求

- Java 21+
- Spring Boot 3.x（仅 starter 模块需要）

## 关于依赖告警

GitHub Dependabot 会在这个仓库报出若干依赖漏洞告警。需要说明的是：

- **告警全部来自 `easy-captcha-demo` 模块的传递依赖**（`tomcat-embed-core`、
  `spring-webmvc`、`jackson-databind`、`netty` 等），因为 demo 是一个完整的
  Spring Boot Web 示例工程。
- **`easy-captcha-core` 与 `easy-captcha-spring-boot-starter` 本身依赖很干净**：
  core 只依赖 `java.desktop`，starter 只依赖 `spring-boot-autoconfigure`。
- 引入本库不会把 demo 的依赖带进你的项目——`easy-captcha-demo` 已设置
  `maven.deploy.skip=true`，不参与发布。

如果你需要部署 demo，请自行把 Spring Boot 升到最新补丁版，
或直接用 `mvn dependency:tree` 排查后再上线。

## 许可证

[Apache License 2.0](LICENSE)
