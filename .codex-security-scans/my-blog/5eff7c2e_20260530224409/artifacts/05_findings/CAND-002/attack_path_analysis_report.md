# Attack Path Analysis: CAND-002

## Decision

Reportable, high severity.

## Attack Path

1. A normal authenticated author creates or edits an article with HTML/script-capable payloads in the title or summary.
2. Backend article sanitization performs sensitive-word masking only and stores the title/summary.
3. Any visitor who views a feed route using `ArticleFeed.vue` receives the malicious article.
4. `v-html` inserts the payload into the DOM.
5. Injected JavaScript can read `localStorage` session data and act as the victim through API calls.

## Counterevidence

Markdown bodies are sanitized through DOMPurify, but the vulnerable feed title/summary path bypasses that renderer. Vue moustache bindings elsewhere are safe siblings, not counterevidence for `v-html` here.

## Severity

High. The affected component is reused across public and authenticated feeds, and the application stores bearer tokens in localStorage. A self-service author can therefore attack readers and potentially take over accounts. Dynamic browser proof would determine whether specific payloads execute immediately, but raw HTML insertion is already a serious broken control.
