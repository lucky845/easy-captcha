package io.github.lucky845.captcha.demo.controller;

import io.github.lucky845.basic.captcha.CaptchaService;
import io.github.lucky845.basic.captcha.behavior.BehaviorAnswer;
import io.github.lucky845.basic.captcha.properties.CaptchaProperties;
import io.github.lucky845.basic.core.behavior.BehaviorVerdict;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.exception.RateLimitExceededException;
import io.github.lucky845.basic.core.result.CaptchaResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 验证码示例接口。
 *
 * <p>覆盖市面上主流验证码库的完整能力：
 * <ul>
 *     <li>静态图片：PNG / GIF / 中文 / 算术；</li>
 *     <li>行为验证码：滑块拼图、文字点选；</li>
 *     <li>二次校验：校验通过后换发 token，业务接口凭 token 放行；</li>
 *     <li>限流：按客户端 IP 限制签发频率。</li>
 * </ul>
 *
 * @author created by lucky845 on 2025-01-17
 */
@RestController
@RequestMapping("/captcha")
public class DemoController {

    private static final Logger LOG = Logger.getLogger(DemoController.class.getName());

    private final CaptchaService captchaService;

    private final CaptchaProperties properties;

    public DemoController(CaptchaService captchaService, CaptchaProperties properties) {
        this.captchaService = captchaService;
        this.properties = properties;
    }

    /**
     * 直接返回图片，浏览器会缓存 URL，所以必须显式禁用缓存。
     */
    @GetMapping("/image")
    public void image(@RequestParam(value = "type", required = false) String type,
                      HttpServletRequest request,
                      HttpServletResponse response) throws IOException {
        CaptchaResult result = captchaService.issue(resolveType(type), clientKey(request)).result();
        writeImage(result, response);
    }

    /**
     * 返回 Base64 图片 + 可校验的 id，行为验证码还会带上前端渲染所需的图形数据。
     */
    @GetMapping("/base64")
    public Map<String, Object> base64(@RequestParam(value = "type", required = false) String type,
                                      HttpServletRequest request) {
        CaptchaTypeEnum resolved = resolveType(type);
        CaptchaService.CaptchaIssue issue = captchaService.issue(resolved, clientKey(request));
        CaptchaResult result = issue.result();

        LOG.info(() -> "生成验证码 id=" + issue.id() + ", 类型=" + resolved + ", 答案=" + result.code());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", issue.id());
        body.put("image", result.toBase64());
        body.put("type", resolved.name());
        result.extra().forEach(body::putIfAbsent);
        return body;
    }

    /**
     * Plain Text 形式的 Base64。
     */
    @GetMapping(value = "/base64/plain", produces = MediaType.TEXT_PLAIN_VALUE)
    public String base64Plain(@RequestParam(value = "type", required = false) String type,
                              HttpServletRequest request) {
        return captchaService.issue(resolveType(type), clientKey(request)).result().toBase64();
    }

    /**
     * 校验静态验证码。
     *
     * @param request 包含 id 与 answer
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody ValidateRequest request) {
        BehaviorVerdict verdict = captchaService.checkText(request.id(), request.answer());
        return ResponseEntity.ok(verdictBody(verdict));
    }

    /**
     * 校验静态验证码并通过后签发二次校验令牌。
     */
    @PostMapping("/validate/ticket")
    public ResponseEntity<Map<String, Object>> validateWithTicket(@RequestBody ValidateRequest request) {
        return captchaService.validateAndIssueToken(request.id(), request.answer())
                .map(token -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("valid", true);
                    body.put("ticket", token);
                    return ResponseEntity.ok(body);
                })
                .orElseGet(() -> ResponseEntity.ok(Map.of("valid", false, "reason", "ANSWER_MISMATCH")));
    }

    /**
     * 行为验证码校验（滑块 / 点选），答案与轨迹一起判定。
     */
    @PostMapping("/validate/behavior")
    public ResponseEntity<Map<String, Object>> validateBehavior(@RequestBody BehaviorRequest request) {
        BehaviorAnswer answer = new BehaviorAnswer(
                request.answer(),
                request.points() == null ? List.of() : request.points().stream()
                        .map(p -> new BehaviorAnswer.Point(p.x(), p.y(), p.t()))
                        .toList(),
                request.duration());
        BehaviorVerdict verdict = captchaService.validateBehavior(request.id(), answer);
        return ResponseEntity.ok(verdictBody(verdict));
    }

    /**
     * 业务接口示例：凭二次校验令牌放行。
     *
     * <p>真实业务里这里会是登录、注册、发短信等动作，必须先过验证码才能进入。
     */
    @PostMapping("/business/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        if (!captchaService.verifyToken(request.ticket())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("ok", false, "message", "验证码票据无效或已过期"));
        }
        // 票据是一次性的，这里已经消费掉
        return ResponseEntity.ok(Map.of("ok", true, "message", "登录成功（示例）"));
    }

    /**
     * 列出可用的验证码类型及是否为行为验证码。
     */
    @GetMapping("/types")
    public List<Map<String, Object>> types() {
        return List.of(CaptchaTypeEnum.values()).stream()
                .map(t -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("type", t.name());
                    item.put("behavior", t.isBehavior());
                    return item;
                })
                .toList();
    }

    /**
     * 当前生效的配置。
     */
    @GetMapping("/config")
    public Map<String, Object> config(HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("captchaType", properties.getCaptchaType());
        body.put("width", properties.getWidth());
        body.put("height", properties.getHeight());
        body.put("captchaLength", properties.getCaptchaLength());
        body.put("charType", properties.getCharType());
        body.put("font", properties.getFont());
        body.put("difficulty", properties.getDifficulty());
        body.put("ttl", properties.getTtl().toString());
        body.put("sliderTolerance", properties.getSliderTolerance());
        body.put("clickTolerance", properties.getClickTolerance());
        body.put("remainingQuota", captchaService.remainingQuota(clientKey(request)));
        return body;
    }

    /**
     * 限流触发时返回 429，而不是 500。
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> onRateLimited(RateLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("ok", false, "message", e.getMessage()));
    }

    private Map<String, Object> verdictBody(BehaviorVerdict verdict) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("valid", verdict.passed());
        body.put("reason", verdict.reason().name());
        body.put("detail", verdict.detail());
        return body;
    }

    private void writeImage(CaptchaResult result, HttpServletResponse response) throws IOException {
        response.setContentType(result.mimeType());
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentLength(result.image().length);
        response.getOutputStream().write(result.image());
        response.getOutputStream().flush();
    }

    private CaptchaTypeEnum resolveType(String type) {
        if (type == null || type.isBlank()) {
            return properties.getCaptchaType();
        }
        try {
            return CaptchaTypeEnum.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return properties.getCaptchaType();
        }
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 静态验证码校验请求
     *
     * @param id     验证码 id
     * @param answer 用户答案
     */
    public record ValidateRequest(String id, String answer) {
    }

    /**
     * 行为验证码校验请求
     *
     * @param id       验证码 id
     * @param answer   静态答案（滑块偏移量 / 点选坐标串）
     * @param points   轨迹点
     * @param duration 耗时毫秒
     */
    public record BehaviorRequest(String id, String answer, List<PointDto> points, long duration) {
    }

    /**
     * 轨迹点
     *
     * @param x 横坐标
     * @param y 纵坐标
     * @param t 时间偏移
     */
    public record PointDto(int x, int y, long t) {
    }

    /**
     * 登录请求
     *
     * @param ticket 二次校验令牌
     */
    public record LoginRequest(String ticket) {
    }
}
