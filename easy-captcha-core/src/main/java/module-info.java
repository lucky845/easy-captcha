module easy.captcha.core {
    requires java.desktop;
    requires java.logging;
    requires static lombok;

    exports io.github.lucky845.basic.core.behavior;
    exports io.github.lucky845.basic.core.captcha;
    exports io.github.lucky845.basic.core.captcha.impl;
    exports io.github.lucky845.basic.core.config;
    exports io.github.lucky845.basic.core.constants;
    exports io.github.lucky845.basic.core.encoder;
    exports io.github.lucky845.basic.core.enums;
    exports io.github.lucky845.basic.core.exception;
    exports io.github.lucky845.basic.core.result;
    exports io.github.lucky845.basic.core.utils;
}
