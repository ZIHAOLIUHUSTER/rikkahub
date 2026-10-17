本次修复 HTML 预览显示 `net::ERR_HTTP_RESPONSE_CODE_FAILURE` 的回归：此前对所有加载返回 403，可能误拦主文档；现在用独立预览域名直接返回原始 HTML，拦截只针对其他请求。不再通过 Jsoup 重写模型的 HTML/SVG，不再注入主题 CSS 或 viewport，恢复原有缩放设置。

思考内容与仍在生成的回答只显示源码；完成的回答才自动预览。保留自适应高度、源码切换、全屏和随滚动可见区域创建/释放 WebView 的行为。公式复制功能不变。

此版本显示名称为 **RikkaHub 学习版**，使用新的独立包名 `me.rerere.rikkahub.study.debug`，版本为 `2.5.6-study.2`。无需卸载原 RikkaHub；它们使用独立的数据目录，原应用和原数据保留。旧的定制预览包使用 `me.rerere.rikkahub.debug`，本次也与它分开安装。

此预览版包含以下改进：

- 长按数学公式复制完整 LaTeX；整段选择复制保留行内及独立公式块的 LaTeX。
- 复制面板支持原始 Markdown 与纯文本。
- 完整 `html` 代码块在回答生成结束后自动预览，可切换源码、展开及全屏显示。
- 预览自适应高度，限制同时活跃的 WebView，并隔离 Android 接口、文件、设备及外部网络访问。
- 移除 Firebase 使用统计与崩溃上报，无需 `google-services.json`。

安装：一般安卓手机请选择 `arm64-v8a` APK，不确定架构时选择 `universal`。这是使用 Android 调试证书签名的可安装测试包，包名为 `me.rerere.rikkahub.study.debug`，可与正式版共存。它不使用正式版签名；后续预览包的签名可能不同，升级前请先备份应用数据。

发布流程执行 UI 单元测试、前端类型检查、Debug APK 构建及 Android 15 WebView 渲染回归测试，并校验 APK 签名；这些检查通过后才发布附件。尚未进行真实设备上的长按、跨段落拖选、WebView 控件和自适应布局验收。外部 CDN、远程图片和网络请求会被 HTML 预览的隔离策略阻止。离屏预览重新创建时，临时表单状态会重置。

附件提供 SHA-256 校验值。详细验收步骤见仓库 `docs/development/phase-one-rich-content.md`。
