# Stage 9 最终交付与验收清单

## 已有真实证据

- 2026-10-19 用户上传的 Stage8_QA_Result.txt：Gradle PASS，JUnit 147/147，失败0，跳过0。
- JaCoCo Instructions 96.45%，Branches 89.94%，平均 93.20%，满足原作业的平均>90%目标。
- Stage 9 在Stage 8源代码上只添加Javadoc文字说明、严格Java8编译选项及文档；未改变游戏算法。

## 运行一键脚本后的检查

1. `GRADLE: PASS` 并且147项测试全通过
2. 平均 JaCoCo >90%
3. `JAVADOC: PASS` 并生成 `build/docs/javadoc/index.html`
4. `JAVA 8 BYTECODE: PASS (major=52)`：此项检查的是编译字节码版本，并非真实JDK8运行
5. 游戏界面实际运行 `gradle run`，窗口完整，控制/图片无异常（需要Windows真人或GUI自动化）
6. 在真正JDK8环境验证能运行，若原作业要求必须如此

## 作品集材料

- `README.md` 使用说明、快捷键、技术栈及限制
- `docs/Tanks_UML.pdf` 当前实现结构图；`docs/Tanks_Class_Diagram.dot` 编辑源文件
- `docs/Design_Report_EN.md` <=500词设计与扩展说明；`docs/Design_Report_EN.pdf` 阅读版
- `docs/OOP_Audit_CN.md` OOP优缺点；`docs/Stage8_QA_Result_Provided.txt` 既有实测基线

## 交付注意

如果用于作业提交，应按其要求提交根目录的 `build.gradle`、`src/`、`config.json`、布局文件；不需要提交 `build/`，报告/UML按系统要求另交。过去的2024年作业不代表当前仍可提交。该整理包用于复盘和作品集。
