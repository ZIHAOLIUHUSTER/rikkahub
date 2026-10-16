# Markdown copying and HTML previews

The changes are intentionally limited to the native Android rich-content renderer.

## Copying

Long-press an inline or display formula to copy LaTeX, including `$…$` or `$$…$$` delimiters. Formula rendering and escaping never feed the clipboard. Long inline formulas scroll horizontally in one bounded placeholder; this avoids copying the complete formula once per visual fragment.

The existing select-and-copy sheet defaults to the original message Markdown, unchanged. Switch to Plain text to select or copy text generated from the Markdown syntax tree; code and LaTeX remain intact. Existing whole-message copy and export actions remain available.

Rendered message selection now carries a formula source map. A formula is one selectable placeholder, restored to its complete LaTeX when copying; this includes display formulas drawn on Canvas and multiline expressions. Visual fragments do not duplicate the source or insert zero-width spaces. Ordinary literal text such as `[Latex]` is never guessed or rewritten. The wrapper supports both Compose clipboard APIs. Long-press still copies the complete formula; selecting individual symbols within the rendered formula is intentionally out of scope.

Arbitrary rendered selections do not map back to exact Markdown formatting spans; use the Markdown tab for exact source selection. Formula delimiter styles are canonicalized on formula-level copy; the Markdown tab preserves the original delimiters and whitespace.

## HTML previews

Preferences → code display settings → Automatically preview HTML controls the default. Only closed fences explicitly labelled `html` automatically preview, after message streaming finishes. SVG retains its manual preview behavior. Toolbar buttons still copy/save source, toggle source/preview, and open fullscreen.

Inline height follows measured page content, rounded to 4dp with a 96dp minimum, a 360dp collapsed limit and a 720dp expanded limit. Larger pages scroll inside the preview. Measurements run every 500ms only while the preview is visible and the activity resumed. At most two visible inline WebViews exist; excess previews show a paused placeholder and remain accessible fullscreen. Leaving the viewport or backgrounding destroys the inline WebView, so its transient JS/form state is reset when it returns. Fullscreen pauses/resumes with the activity.

Generated HTML uses a separate origin and a restrictive CSP before authored scripts. Inline JavaScript, Canvas, SVG, data/blob images and local form interactions work; external scripts/styles/images, network requests, frames, workers, navigation, form submissions, file/content access, downloads, autofill, geolocation, camera/microphone permissions and Android JS interfaces are blocked. Pages requiring CDN libraries or remote resources need to be made self-contained. Existing trusted Mermaid and Markdown-export WebViews retain their asset support.

## Validation

Unit coverage includes Markdown parsing/copying, fenced HTML completion/streaming/setting gates, preserving local controls and scripts, and enforcing CSP order. Cloud validation logs are under `/workspace/.setup`; the final task report distinguishes Gradle checks, direct JUnit execution, and device checks.

On a device, verify:

1. Render inline `$\frac{a}{b} + \alpha$` and a display matrix with `\\` row separators. Long-press each and paste into a plain text editor. Select surrounding prose and check that a whole formula appears once. In the copy sheet compare Markdown source character-for-character, then try Plain text. Check existing message copy and export.
2. Stream the HTML fixture below: the preview must wait for message completion. Test the slider, button, Canvas and SVG; switch to source and fullscreen. Disable automatic HTML previews and confirm source is the default. An unclosed fence or an `xml`/`svg` fence must not auto execute.
3. Try a 1200px-tall body and dynamic DOM insertion: verify capped height, expand/collapse, scrolling, orientation and both themes. Scroll a long conversation with many HTML fences; confirm only visible previews are active. Check the documented reset of transient state on return.
4. Confirm remote fetches/images, `file://`/`content://`, file pickers, permissions and links to app/local origins fail while local controls continue to work.

```html
<!doctype html>
<html><head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
<body>
<label>Value <input type="range" min="0" max="100" value="20"
  oninput="document.querySelector('output').value=this.value"></label>
<output>20</output>
<button onclick="this.textContent='Clicked';document.querySelector('section').style.height='600px'">Click</button>
<canvas id="plot" width="100" height="60"></canvas>
<svg width="60" height="60"><circle cx="30" cy="30" r="20" fill="teal"/></svg>
<section>Dynamic height</section>
<script>document.querySelector('canvas').getContext('2d').fillRect(10,10,50,30)</script>
</body></html>
```
