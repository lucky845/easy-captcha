module easy.captcha.spring.boot.starter {
    requires easy.captcha.core;
    requires jakarta.annotation;
    requires spring.beans;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires java.desktop;

    exports com.lucky845.basic.captcha;
    exports com.lucky845.basic.captcha.config;
    exports com.lucky845.basic.captcha.generator;
    exports com.lucky845.basic.captcha.proterties;
}
