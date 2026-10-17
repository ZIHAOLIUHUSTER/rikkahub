RikkaHub 学习版第一阶段正式版，版本 **2.5.6-study.3（194）**。

- 移除应用更新检测、侧栏更新卡片、设置中的更新提醒与暂停选项，以及更新包下载逻辑；不再请求上游更新服务。
- 保留已验证的 LaTeX 长按复制、整段选区公式复制与 Markdown/纯文本复制。
- 保留 HTML 自动预览、自适应高度、源码切换与全屏；思考内容和仍在生成的回答不预览，离屏时释放 WebView。
- 使用经过代码优化的非调试 Release 构建与固定发布签名，无需 Google 服务或 google-services.json。

**安装**：普通安卓手机选 arm64-v8a，不确定时选 universal。最低 Android 8.0。

正式版包名为 `me.rerere.rikkahub.study`，名称为「RikkaHub 学习版」。它与原版以及 `.study.debug` 测试版独立安装，不覆盖其数据。请在旧版通过备份功能导出数据，再导入正式版。今后正式版更新需继续使用本次发布密钥签名。

本地运行 UI 单元测试和 HTML 预览策略单元测试、Release APK 构建、APK 签名及包名/版本检查。公开发布前在 Android 15 模拟器安装并启动实际 Release APK。之前的 Android WebView 回归测试已覆盖 SVG、Canvas、滑块、按钮、文档替换和 WebView 释放；本次不修改渲染逻辑。真实设备上的最终体验仍以实机测试为准。

SHA256SUMS.txt 提供 APK 校验值。签名证书 SHA-256：
`4b15d75da0e4461dc24506fb53e2aaa156b72c34b59a3fec3d56356525842909`
