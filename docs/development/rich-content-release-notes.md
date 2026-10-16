此预览版包含以下改进：

- 长按数学公式复制完整 LaTeX；整段选择复制保留行内及独立公式块的 LaTeX。
- 复制面板支持原始 Markdown 与纯文本。
- 完整 `html` 代码块在回答生成结束后自动预览，可切换源码、展开及全屏显示。
- 预览自适应高度，限制同时活跃的 WebView，并隔离 Android 接口、文件、设备及外部网络访问。
- 移除 Firebase 使用统计与崩溃上报，无需 `google-services.json`。

安装：一般安卓手机请选择 `arm64-v8a` APK，不确定架构时选择 `universal`。这是使用 Android 调试证书签名的可安装测试包，包名为 `me.rerere.rikkahub.debug`，可与正式版共存。它不使用正式版签名；后续预览包的签名可能不同，升级前请先备份应用数据。

发布流程执行 UI 单元测试、前端类型检查及 Debug APK 构建，并校验 APK 签名；这些检查通过后才发布附件。尚未进行真实设备上的长按、跨段落拖选、WebView 控件和自适应布局验收。外部 CDN、远程图片和网络请求会被 HTML 预览的隔离策略阻止。离屏预览重新创建时，临时表单状态会重置。

附件提供 SHA-256 校验值。详细验收步骤见仓库 `docs/development/phase-one-rich-content.md`。
