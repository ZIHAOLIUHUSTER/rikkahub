# Native role thought cards

Only assistant text parts, after the existing visual rules, pass through the display-only role-thought parser. User messages, native Reasoning, `<think>` processing, stored `UIMessage` parts and request/history assembly are unchanged. This feature never writes transformed text back to a message.

The UI-only `RoleThoughtDocument` splits exact `<心声>…</心声>` pairs into text and thought spans, with original offsets and original source. Concatenating all span sources reconstructs the input exactly. It uses the existing Markdown grammar to exclude fenced, indented and inline code, plus raw HTML blocks. It scans tags without a replacement regex or XML parser. Nested, unmatched and malformed tags remain ordinary source; a later independent valid block still works.

During streaming, unfinished tags remain visible as original Markdown. Each completed pair immediately becomes a collapsed native card; no message-completion wait is needed. Parsing updates are conflated on a background dispatcher. Expansion is kept at the message/text-part level so adding the final selection container does not collapse an already-open card. Different message IDs reset expansion; changing a thought's content also resets its expansion.

Cards reuse generic `ChainOfThought` visuals and controlled expansion with an independent thought span type. Their expanded body uses native Markdown; code previews (HTML/SVG/Mermaid) are disabled there, so the feature creates no WebViews. Long content uses natural chat-list scrolling, without an inner height cap. Markdown structures are interpreted per segment; keep Markdown constructs and reference definitions within the same segment rather than spanning across a card boundary.

The card copy button copies the exact inner Markdown, including whitespace, code and LaTeX. Expanded body text participates in the existing message selection/LaTeX source map; card headers do not become selected answer text. Existing whole-message copy, Markdown copy sheet and export keep the raw tags and content, even while a card is collapsed.

Preferences → interface → 自动折叠角色心声 controls the feature (default on). Turning it off uses the original Markdown display. Disable any old rule that hides or replaces role thoughts: visual rules run before this renderer, and a renderer cannot recover already-replaced text. No existing regex settings are automatically edited.

Validation: parser tests cover examples A–D, multiple positions, nested/broken tags, code examples, escaped text, every prefix of a stream, long formulas/whitespace and randomized lossless round trips. App tests check unchanged message serialization and old/new settings. Android Compose tests check collapse/expand, independent cards, exact clipboard content, streaming/selection continuity and absence of WebViews for code inside cards.
