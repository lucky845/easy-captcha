# Easy Captcha Spring Boot Starter

## 项目简介

`easy-captcha-spring-boot-starter` 是一个简单易用的验证码生成器，基于 Spring Boot
进行自动配置，支持多种验证码类型。它可以帮助开发者快速集成验证码功能，提升应用的安全性。

## 特性

- 支持多种验证码类型（数字、字符、图形等）。
- 与 Spring Boot 无缝集成，开箱即用。
- 配置简单，支持自定义设置。
- 支持 Base64 编码的验证码，便于前端展示。

## 安装

### Maven

将以下依赖添加到 `pom.xml` 中：

```xml

<dependency>
    <groupId>io.github.lucky845</groupId>
    <artifactId>easy-captcha-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 配置

在 `application.properties` 或 `application.yml` 文件中，配置验证码相关的属性：

```properties
captcha.enable=true
captcha.width=200
captcha.height=50
```

## 使用

1. 在 Spring Boot 项目中启用自动配置。

```java
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CaptchaProperties.class)
public class CaptchaConfiguration {
    // 配置你的验证码逻辑
}
```

2. 使用 `CaptchaService` 类来生成验证码。

```java
import io.github.lucky845.basic.captcha.CaptchaService;

@Autowired
private CaptchaService captchaService;

public void generateCaptcha() {
    String captcha = captchaService.generateCaptcha();
    // 处理生成的验证码
}
```

## 贡献

欢迎提交您的贡献！请遵循以下步骤：

1. Fork 仓库。
2. 创建一个新分支：`git checkout -b feature-branch`
3. 提交您的更改：`git commit -am 'Add new feature'`
4. 推送到分支：`git push origin feature-branch`
5. 创建 Pull Request。

如果您发现任何漏洞或问题，请通过以下方式报告：

- 发送邮件至 [1447545564@qq.com](mailto:1447545564@qq.com)
- 创建 GitHub Issue（请标记为 "Security"）

## 安全政策

每月发布一次安全更新，包含最新的漏洞修复。紧急修复将在 24 小时内发布。请确保您使用的是最新版本，确保系统安全。

## 许可证

该项目采用 [Apache-2.0 许可证](LICENSE) 进行许可。
