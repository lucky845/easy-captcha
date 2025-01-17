module easy.captcha.spring.boot.starter {
    requires easy.captcha.core;
    requires spring.beans;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires org.apache.tomcat.embed.core;
    requires java.desktop;

    exports io.github.lucky845.basic.captcha;
    exports io.github.lucky845.basic.captcha.config;
    exports io.github.lucky845.basic.captcha.generator;
    exports io.github.lucky845.basic.captcha.proterties;
}
