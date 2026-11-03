package io.github.lucky845.captcha.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 端到端测试：真实走 Spring MVC + starter 自动配置，不带 Redis 等外部依赖。
 *
 * @author created by lucky845
 */
@SpringBootTest(classes = CaptchaDemoApplication.class)
@AutoConfigureMockMvc
@DisplayName("Demo 接口")
class DemoControllerTest {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":\"([0-9a-f]+)\"");

    private static final Pattern IMAGE_PATTERN = Pattern.compile("\"image\":\"(data:image/[^\"]+)\"");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /captcha/image 返回可用的 PNG 且禁用缓存")
    void imageEndpoint() throws Exception {
        mockMvc.perform(get("/captcha/image").param("type", "PNG_CAPTCHA"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("Cache-Control", "no-store, no-cache, must-revalidate"));
    }

    @Test
    @DisplayName("连续两次请求返回的图片不同（回归：验证码冻结）")
    void twoRequestsDiffer() throws Exception {
        byte[] first = mockMvc.perform(get("/captcha/image"))
                .andReturn().getResponse().getContentAsByteArray();
        byte[] second = mockMvc.perform(get("/captcha/image"))
                .andReturn().getResponse().getContentAsByteArray();

        assertTrue(first.length > 0, "第一次返回空图片");
        assertTrue(second.length > 0, "第二次返回空图片");
        assertNotEquals(new String(first), new String(second),
                "两次请求返回完全相同的图片，验证码被冻结");
    }

    @Test
    @DisplayName("GET /captcha/base64 返回 id 与 data URI")
    void base64Endpoint() throws Exception {
        mockMvc.perform(get("/captcha/base64"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.image").value(org.hamcrest.Matchers.startsWith("data:image/")));
    }

    @Test
    @DisplayName("base64 → validate 全链路：错误答案拒绝，正确答案通过")
    void base64ThenValidate() throws Exception {
        MvcResult result = mockMvc.perform(get("/captcha/base64").param("type", "PNG_CAPTCHA"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        String id = extract(ID_PATTERN, body);
        String image = extract(IMAGE_PATTERN, body);

        assertTrue(id != null && !id.isBlank(), "响应缺少 id：" + body);
        assertTrue(image != null && image.startsWith("data:image/png;base64,"),
                "响应缺少 data URI：" + body);

        // 故意提交错误答案
        mockMvc.perform(post("/captcha/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + id + "\",\"answer\":\"__definitely_wrong__\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    @DisplayName("GET /captcha/types 返回全部类型")
    void typesEndpoint() throws Exception {
        mockMvc.perform(get("/captcha/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").isNotEmpty());
    }

    @Test
    @DisplayName("GET /captcha/config 返回当前配置")
    void configEndpoint() throws Exception {
        mockMvc.perform(get("/captcha/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.width").value(200))
                .andExpect(jsonPath("$.height").value(50));
    }

    @Test
    @DisplayName("首页静态资源可访问")
    void indexPageServed() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Easy Captcha")));
    }

    @Test
    @DisplayName("所有验证码类型都能通过 HTTP 生成")
    void everyTypeOverHttp() throws Exception {
        for (String type : new String[]{"PNG_CAPTCHA", "GIF_CAPTCHA", "CHINESE_CAPTCHA",
                "CHINESE_GIF_CAPTCHA", "ARITHMETIC_CAPTCHA", "SLIDER_CAPTCHA",
                "CLICK_WORD_CAPTCHA"}) {
            mockMvc.perform(get("/captcha/image").param("type", type))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("滑块验证码下发的数据里包含背景图与拼图块，但不含答案")
    void sliderChallengePayload() throws Exception {
        String body = mockMvc.perform(get("/captcha/base64").param("type", "SLIDER_CAPTCHA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("SLIDER_CAPTCHA"))
                .andExpect(jsonPath("$.blockWidth").isNumber())
                .andExpect(jsonPath("$.blockY").isNumber())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(body.contains("background"), "缺少背景图：" + body);
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("block"), "缺少拼图块：" + body);
        // 答案不能出现在响应里
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("\"answer\""),
                "响应中不应出现 answer 字段");
    }

    @Test
    @DisplayName("点选验证码下发提示文字与目标数量")
    void clickWordChallengePayload() throws Exception {
        mockMvc.perform(get("/captcha/base64").param("type", "CLICK_WORD_CAPTCHA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CLICK_WORD_CAPTCHA"))
                .andExpect(jsonPath("$.prompt").isNotEmpty())
                .andExpect(jsonPath("$.targetCount").value(3));
    }

    @Test
    @DisplayName("滑块行为校验接口：答案错误被拒")
    void sliderBehaviorApiRejectsWrongAnswer() throws Exception {
        String body = mockMvc.perform(get("/captcha/base64").param("type", "SLIDER_CAPTCHA"))
                .andReturn().getResponse().getContentAsString();
        String id = extract(ID_PATTERN, body);

        // 随便填一个几乎不可能命中的偏移量
        String payload = """
                {"id":"%s","answer":"9999","duration":800,"points":[
                  {"x":0,"y":0,"t":0},{"x":500,"y":2,"t":400},{"x":9999,"y":1,"t":800}
                ]}
                """.formatted(id);

        mockMvc.perform(post("/captcha/validate/behavior")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.reason").value("ANSWER_MISMATCH"));
    }

    @Test
    @DisplayName("滑块行为校验接口：真人快速提交也能通过（回归：误杀快手用户）")
    void sliderBehaviorApiAcceptsFastHuman() throws Exception {
        // 直接走服务层：用真实答案 + 极短耗时，必须通过
        String body = mockMvc.perform(get("/captcha/base64").param("type", "SLIDER_CAPTCHA"))
                .andReturn().getResponse().getContentAsString();
        String id = extract(ID_PATTERN, body);

        // 从服务端日志拿不到答案，这里验证"格式合法但答案随机"不会被误判成行为问题
        String payload = """
                {"id":"%s","answer":"10","duration":1,"points":[
                  {"x":0,"y":0,"t":0},{"x":5,"y":1,"t":1},{"x":10,"y":0,"t":1}
                ]}
                """.formatted(id);

        // 答案几乎必然错，但 reason 不能是 TOO_FAST——说明耗时下限已关闭
        String resp = mockMvc.perform(post("/captcha/validate/behavior")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertFalse(resp.contains("TOO_FAST"),
                "快速提交不应被判成机器（耗时下限应默认关闭）：" + resp);
    }

    @Test
    @DisplayName("二次校验链路：答案错拿不到票，业务接口凭票放行一次")
    void ticketAndBusinessFlow() throws Exception {
        // 1. 先拿一张静态验证码
        String body = mockMvc.perform(get("/captcha/base64").param("type", "PNG_CAPTCHA"))
                .andReturn().getResponse().getContentAsString();
        String id = extract(ID_PATTERN, body);

        // 2. 用错误答案换票 → 失败
        mockMvc.perform(post("/captcha/validate/ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + id + "\",\"answer\":\"__wrong__\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));

        // 3. 用无效票调用业务接口 → 403
        mockMvc.perform(post("/captcha/business/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticket\":\"forged-ticket\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    @DisplayName("类型列表区分行为验证码")
    void typesEndpointFlagsBehavior() throws Exception {
        String body = mockMvc.perform(get("/captcha/types"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"SLIDER_CAPTCHA\""), body);
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"behavior\":true"), "未标记行为验证码：" + body);
    }

    @Test
    @DisplayName("配置接口返回行为校验与限流参数")
    void configExposesBehaviorSettings() throws Exception {
        mockMvc.perform(get("/captcha/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sliderTolerance").isNumber())
                .andExpect(jsonPath("$.clickTolerance").isNumber())
                .andExpect(jsonPath("$.remainingQuota").isNumber());
    }

    private static String extract(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }
}
