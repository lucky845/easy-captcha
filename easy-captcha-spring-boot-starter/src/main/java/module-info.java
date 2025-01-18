module easy.captcha.spring.boot.starter {
    requires easy.captcha.core;
    requires static lombok;
    requires java.desktop;
    requires spring.boot;
    requires spring.context;
    requires transmittable.thread.local;
    requires spring.boot.autoconfigure;
    requires jakarta.servlet;
    requires java.logging;
    requires jakarta.annotation;

    exports io.github.lucky845.basic.captcha;
    exports io.github.lucky845.basic.captcha.config;
    exports io.github.lucky845.basic.captcha.generator;
    exports io.github.lucky845.basic.captcha.proterties;
}
