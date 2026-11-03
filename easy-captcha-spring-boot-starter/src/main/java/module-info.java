module easy.captcha.spring.boot.starter {
    requires easy.captcha.core;
    requires static lombok;
    requires java.desktop;
    requires java.logging;
    requires spring.boot;
    requires spring.context;
    requires spring.beans;
    requires spring.boot.autoconfigure;
    requires spring.data.redis;

    exports io.github.lucky845.basic.captcha;
    exports io.github.lucky845.basic.captcha.generator;
    exports io.github.lucky845.basic.captcha.properties;
    exports io.github.lucky845.basic.captcha.store;
}
