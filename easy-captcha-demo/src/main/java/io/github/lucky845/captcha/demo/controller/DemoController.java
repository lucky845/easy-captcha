package io.github.lucky845.captcha.demo.controller;


import io.github.lucky845.basic.captcha.CaptchaService;
import io.github.lucky845.basic.captcha.generator.AbstractCaptchaGeneratorStrategy;
import io.github.lucky845.basic.captcha.generator.CaptchaGeneratorFactory;
import io.github.lucky845.basic.captcha.proterties.CaptchaProperties;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author created by lucky845 on 2025-01-17
 */
@CrossOrigin(origins = "*")
@RequestMapping("/")
@RestController
public class DemoController {

    private final Logger logger = Logger.getLogger(DemoController.class.getName());

    private final String REDIS_KEY_PREFIX = "captcha:";

    private final CaptchaProperties captchaProperties;
    private final CaptchaService captchaService;
    private final RedisTemplate<String, Object> redisTemplate;

    public DemoController(CaptchaProperties captchaProperties,
                          CaptchaService captchaService,
                          RedisTemplate<String, Object> redisTemplate) {
        this.captchaProperties = captchaProperties;
        this.captchaService = captchaService;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("captcha/{num}")
    public void captcha(@PathVariable("num") Integer num, HttpServletResponse response) throws IOException, FontFormatException {
        ServletOutputStream outputStream = response.getOutputStream();
        AbstractCaptchaGeneratorStrategy strategy = CaptchaGeneratorFactory.getGenerator(captchaProperties.getCaptchaType());
        strategy.generateCaptcha(outputStream);
        // 此处仅打印验证码，实际使用请保存至Redis
        String captcha = strategy.getCaptcha();
        logger.info(captcha);
        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + num, captcha, 60, TimeUnit.SECONDS);
        outputStream.flush();
        outputStream.close();
    }

    @PostMapping("validateCaptcha")
    public ResponseEntity<Map<String, Object>> validateCaptcha(@RequestBody Map<String, String> requestData) {
        String userCaptcha = requestData.get("captcha");
        String num = requestData.get("num");
        String redisCaptcha = (String) redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + num);
        redisTemplate.delete(REDIS_KEY_PREFIX + num);

        logger.info("user: " + userCaptcha + ", redis: " + redisCaptcha);

        Map<String, Object> response = new HashMap<>();
        if (userCaptcha != null && userCaptcha.equals(redisCaptcha)) {
            response.put("valid", true);
        } else {
            response.put("valid", false);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("captchaBase64")
    public String captchaBase64() throws IOException, FontFormatException {
        AbstractCaptchaGeneratorStrategy strategy = CaptchaGeneratorFactory.getGenerator(captchaProperties.getCaptchaType());
        String base64Captcha = strategy.generateCaptchaBase64();
        // 此处仅打印验证码，实际使用请保存至Redis
        logger.info(strategy.getCaptcha());
        return base64Captcha;
    }

    @GetMapping("captchaNew")
    public void captchaNew(HttpServletResponse response) {
        captchaService.generateCaptcha(response);
        // 此处仅打印验证码，实际使用请保存至Redis
        logger.info(captchaService.getCaptcha());
    }

    @GetMapping("captchaBase64New")
    public String captchaBase64New() {
        String base64Captcha = captchaService.generateCaptchaBase64();
        // 此处仅打印验证码，实际使用请保存至Redis
        logger.info(captchaService.getCaptcha());
        return base64Captcha;
    }


}
