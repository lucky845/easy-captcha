package io.github.lucky845.basic.captcha;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import java.util.logging.Logger;
import java.util.stream.IntStream;


@SpringBootTest(classes = {
        CaptchaAutoconfiguration.class
})
class CaptchaServiceTest {

    private final Logger logger = Logger.getLogger(CaptchaServiceTest.class.getName());

    @Autowired
    private CaptchaService captchaService;

    @Test
    void verificationCodeTest() {

        int count = 10000;
        StopWatch stopWatch = new StopWatch();

        stopWatch.start("task1");
        logger.info("开始生成[" + count + "]个验证码");
        IntStream.range(0, count).forEach(i -> {
            captchaService.generateCaptchaBase64();
            String verificationCode = captchaService.getCaptcha();
            Assertions.assertNotNull(verificationCode);
        });
        stopWatch.stop();
        logger.info("生成[" + count + "]个验证码，耗时: [" + stopWatch.lastTaskInfo().getTimeMillis() + "]ms");

    }

}
