# Validation Report: CAND-002

## Finding

Article feed renders author-controlled titles and summaries as raw HTML.

## Rubric

- [x] Attacker can control stored content rendered to other users.
- [x] The backend path does not HTML-escape or sanitize that specific title/summary output.
- [x] The frontend sink interprets the content as HTML.
- [x] A sanitizer exists elsewhere but is not on this path.
- [x] Sensitive client-side material is available to injected JavaScript.

## Evidence

`ArticleAppService.createArticle` calls `sanitizeArticleContent` at lines 555-560, and `sanitizeArticleContent` at lines 1084-1099 performs sensitive-word detection/masking only. It does not HTML-escape titles or summaries.

`ArticleFeed.vue` line 133 defines `highlightHtml`; line 135 returns `text` unchanged when no highlight keyword exists, and line 137 injects a `<mark>` wrapper without escaping the original text. Lines 346-347 render `article.title` and `article.summary` through `v-html`. The shared feed is used by home, search, user profile, following, category, tag, topic, and column views.

`frontend/src/utils/markdown.js` uses DOMPurify for markdown bodies, but `ArticleFeed.vue` does not call it. `frontend/src/stores/session.js` and `frontend/src/api/http.js` store/read the session token from localStorage, so injected JavaScript can access bearer tokens.

## Tested

No browser PoC was executed. Static validation is direct: a title such as an image tag with an event handler would be stored and later inserted with `v-html` by feed viewers. DOMPurify is not in the feed title/summary path.

## Uncertainty

Exact browser event-handler behavior depends on the payload and Vue DOM insertion behavior, but arbitrary HTML insertion via `v-html` is enough to preserve the candidate. Dynamic browser validation would raise confidence from source-proven to runtime-proven.

## Minimal Next Step

Render title and summary with moustache/text binding, or make `highlightHtml` escape all text before wrapping highlight marks. Add an e2e/unit regression that feeds `<img src=x onerror=...>` and asserts it appears as text, not markup.
