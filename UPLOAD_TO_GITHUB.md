# 上传到 GitHub（Windows PowerShell）

## 推荐：先创建 Private 仓库

课程Scaffold的图片/地图公开授权尚未核实，建议先把GitHub仓库设为 **Private**。之后确认课程代码公开规则及素材再分发权限，再决定是否改为 Public。

1. 将压缩包解压，打开 `java-tanks-game` 文件夹。
2. 在 GitHub 网站创建一个空仓库，例如 `java-tanks-game`；**不要**勾选自动创建 README、.gitignore 或 LICENSE（这些文件已准备好）。
3. 在项目根目录执行下面命令，把 `<YOUR_GITHUB_USERNAME>` 改成你的GitHub用户名：

```powershell
git init
git add .
git commit -m "Initial release: Java Tanks game with tests and docs"
git branch -M main
git remote add origin https://github.com/<YOUR_GITHUB_USERNAME>/java-tanks-game.git
git push -u origin main
```

GitHub可能要求浏览器登录、凭据管理器或者已有SSH授权。不要在聊天中提供密码或访问令牌。

## 核查GitHub上传内容

- `src/main/java/Tanks/App.java`
- `src/test/java/Tanks/`（9个测试类）
- `src/main/resources/Tanks/`（11个图片资源）
- `config.json` 和三个 `.txt` 地图
- `build.gradle` / `settings.gradle`
- `README.md`, `.gitignore`, `.github/workflows/ci.yml`, `docs/`

在仓库的 **Actions** 页面查看首次构建是否通过。`docs/VERIFICATION.md`中的147项测试证据来自之前的本地验收，不能代替新工作流的执行结果。

## 本地复核

```powershell
gradle clean test jacocoTestReport javadoc
gradle run
```

本项目不附带Gradle Wrapper，需预先安装Gradle。已验证过的本地组合为JDK 17 + Gradle 8.14.6（Java 8代码目标）。
