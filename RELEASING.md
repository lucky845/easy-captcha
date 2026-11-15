# 发布到 Maven Central

本文记录把 `easy-captcha` 发到 Maven Central 的完整流程，以及在 Central 收紧发布配额之后必须注意的几条硬限制。

## 前置条件

- **JDK 21 / Maven 3.9+**
- **GPG 密钥**：私钥已生成且公钥已上传到 `keyserver.ubuntu.com`（或其他 Central 能查到的 keyserver）。
  - 推荐使用**无口令**的 ed25519 密钥；若有口令，用环境变量 `MAVEN_GPG_PASSPHRASE` 传入。
  - 日常校验可用 `gpg --list-secret-keys --keyid-format=long`，`pom.xml` 里 `<gpg.keyname>` 填的是**密钥指纹**，不是末 8 位短 ID（短 ID 不是口令，填错会报「损坏的密码」）。
- **Central 凭据**：`~/.m2/settings.xml` 中存在 `<server>` id 为 `central` 的条目，用户名/密码即 Central Portal 生成的 token。
- 本地 `gpg` 需要开启 `--pinentry-mode loopback`，否则在 CI / 非交互 shell 里会报 `Inappropriate ioctl for device`。

## 一次发布的完整流程

```bash
# 1. 改版本号：父 pom + 三个子 pom 的 <version>，以及 README 徽章
grep -n "<version>1.0.4</version>" pom.xml easy-captcha-*/pom.xml

# 2. 补 CHANGELOG.md（新增/修复/依赖升级分节）

# 3. 本地必须全绿，别指望 CI 兜底
mvn clean verify -B

# 4. 提交并推送，打 tag
git add -A && git commit -m "chore: 发布 1.0.5"
git tag -a v1.0.5 -m "1.0.5" && git push origin master --tags

# 5. 发布（-P release 才会带上 gpg 签名与 central-publishing 插件）
mvn clean deploy -P release -DskipTests

# 6. 若 autoPublish=false，需要到 Central Portal 手动点 Publish：
#    https://central.sonatype.com/publishing/deployments

# 7. 只有这一步通过，才算真的发成功
curl -s https://repo1.maven.org/maven2/io/github/lucky845/easy-captcha-core/maven-metadata.xml | grep latest
```

**`deploy` 成功不等于已发布。** `central-publishing-maven-plugin` 的 `waitUntil=validated` 只表示 bundle 通过校验并上传待发布；`autoPublish=false` 时仍需人工确认。曾经的误判就是看到 `BUILD SUCCESS` 就以为发布完成，实际还要点一下 Publish。

## 每月发布配额（务必先看）

Maven Central 从 2026 年起按**组织**、按**自然月**统计三个指标，超过阈值不再免费无限制：

| 指标 | 免费阈值（约为全体发布者的 90 分位） |
| :--- | ---: |
| 文件数 | 约 1,167 个 / 月 |
| 发布体积 | 约 78 MB / 月 |
| 发布次数 | 约 7 次 / 月 |

规则要点（详见 [Maven Central Publishing Limits](https://central.sonatype.org/publish/maven-central-publishing-limits/)）：

- **超限不立刻封**，而是进入 **30 天宽限期**。宽限期内照常可以发布。
- 宽限期到期时如果**仍然是超限状态**，才会拒绝「会让该指标继续超标」的新发布；且只针对超标的那个指标和范围，不是整个组织一刀切封停。
- **每月重新计算**，1 号归零；连续两个月回到限额内，宽限期也会重置。
- 已发布的产物**永远不受影响**，限额只影响新的发布行为。

对我们的实际含义：**一个月内最多发 7 个版本**。同一版本号重复发布、为改一个 README 就重发一版，都会占用配额。

### 撞上限额怎么办

1. **合并发布**：把几个小修复攒进一个版本，不要一天发三次。
2. **错峰**：紧急修复尽量排进配额，非紧急的挪到下个月。
3. **申请豁免**：社区开源项目可以申请提高限额。发邮件到 `central-support@sonatype.com`，附上 namespace、组织名、项目说明、发布频率和申请理由。
4. 在宽限期结束前一周就动手，别等到被拒才处理。

## 已知坑

- **`maven.deploy.skip=true` 对 `central-publishing-maven-plugin` 无效**。要排除 demo 模块，必须用插件自己的配置：

  ```xml
  <excludeArtifacts>
    <artifact>easy-captcha-demo</artifact>
  </excludeArtifacts>
  ```

- **依赖版本覆盖不能靠 BOM import**。`spring-boot-dependencies` 先 import 了 `jackson-bom`，我们再 import 一个同名 BOM 优先级更低、不生效，必须逐个 artifact 在 `dependencyManagement` 里写死版本。
- **`jackson-annotations` 版本线和 `jackson-databind` 不同步**，强行统一会解析失败；只覆盖 databind 与 core，annotations 交给 Maven 调解。
- 发布前确认 `gpg --list-keys` 里的 uid 邮箱与 `pom.xml` 的 `developers` 一致，否则 Central 校验可能不通过。
