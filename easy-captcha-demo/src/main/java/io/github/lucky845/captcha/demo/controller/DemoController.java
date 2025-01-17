package io.github.lucky845.captcha.demo.controller;


import io.github.lucky845.basic.captcha.CaptchaService;
import io.github.lucky845.basic.captcha.generator.AbstractCaptchaGeneratorStrategy;
import io.github.lucky845.basic.captcha.generator.CaptchaGeneratorFactory;
import io.github.lucky845.basic.captcha.proterties.CaptchaProperties;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.*;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author created by lucky845 on 2025-01-17
 */
@RequestMapping("/")
@RestController
public class DemoController {

    private final Logger logger = Logger.getLogger(DemoController.class.getName());

    private final CaptchaProperties captchaProperties;
    private final CaptchaService captchaService;

    public DemoController(CaptchaProperties captchaProperties, CaptchaService captchaService) {
        this.captchaProperties = captchaProperties;
        this.captchaService = captchaService;
    }

    @GetMapping("captcha")
    public void captcha(HttpServletResponse response) throws IOException, FontFormatException {
        ServletOutputStream outputStream = response.getOutputStream();
        AbstractCaptchaGeneratorStrategy strategy = CaptchaGeneratorFactory.getGenerator(captchaProperties.getCaptchaType());
        strategy.generateCaptcha(outputStream);
        // 此处仅打印验证码，实际使用请保存至Redis
        logger.info(strategy.getCaptcha());
        outputStream.flush();
        outputStream.close();
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
    public void captchaNew(HttpServletResponse response) throws IOException, FontFormatException {
        captchaService.generateCaptcha(response);
        // 此处仅打印验证码，实际使用请保存至Redis
        logger.info(captchaService.getCaptcha());
    }

    @GetMapping("captchaBase64New")
    public String captchaBase64New() throws IOException, FontFormatException {
        String base64Captcha = captchaService.generateCaptchaBase64();
        // 此处仅打印验证码，实际使用请保存至Redis
        logger.info(captchaService.getCaptcha());
        return base64Captcha;
    }


}
