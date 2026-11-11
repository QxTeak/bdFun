# Fold chrome and relation lists

- Compact and wide space lists own separate saved scroll positions because only compact lists contain the profile and Segment anchor. The selected pager page owns the top material; changing the window width remounts list geometry without changing the selected tab. Compact tabs follow their list anchor and stop below the navigation row; wide tabs stay in the right header.
- Space status-bar contrast uses the resolved Activity (including ContextWrapper), follows animated header icon contrast, and is reapplied after focus/configuration changes. Usernames use the remaining row width beside the message/follow actions instead of inserting line breaks.
- Dynamic details use the canonical large-title component, which yields to the fixed wide top bar. This avoids two simultaneous titles.
- Only home controls/search and segmented selectors opt into a flat MIUIX `surfaceContainer.copy(alpha = 0.9)` background before glass reveals. Generic top-bar back/actions, including the Home search and search-result back/clear buttons, remain transparent when glass is hidden. Scroll-to-top reserves the navigation inset and at least 20dp above the actual bottom bar, with extra lift on ordinary pages. Tint, refraction and blur tuning are unchanged. The wide player uses one progressive host beginning at window y=0, with status padding inside it. Only the visible introduction/comment page drives progress; the previous wide gradient is removed.
- Space header candidates use collection artwork, custom web `top_photo`, then App day/night defaults. The public App response for UID 283375530 returned default dressing without a collection; the web response returned a built-in default as a relative `bfs/space/...` path. Relative paths are normalized to the image CDN and the built-in web default is placed after App artwork. The authenticated custom header still needs a device check. Collection crop coordinates follow HyperPiliPlus's `TopImage`. Failed bitmap requests try the remaining candidates and error drawables do not count as successful images.

## Following and fans

References: HyperPiliPlus `lib/http/follow.dart`, `lib/http/fan.dart`, `lib/models_new/follow`, and the batch relation query in `lib/pages/video/introduction/ugc/controller.dart`.

- GET `/x/relation/followings` or `/x/relation/fans`: `vmid`, `pn`, `ps=20`, `order=desc`; following supports `order_type=""` (recent) or `attention` (frequent).
- GET `/x/relation/relations?fids=...` resolves the logged-in user's relationships. Attributes from someone else's following list are deliberately not reused. If batch status fails, a row offers a status retry before any mutation.
- Existing `/x/relation/modify` handles follow/unfollow. UI changes only after success. Logged-out mutations show a login hint. API privacy and visibility restrictions remain visible as errors/partial results.
- Space and Mine statistics open the lists. Avatars/names open spaces. Both lists use the existing large-title/scroll glass scaffold, settings-style grouped cards and single-column width limit. Following offers recent/frequent sorting; refresh is a top-bar action.
- Paging keeps loaded rows on error, retries the failed page, deduplicates overlapping results and stops repeated pages. Refresh commits only after success.

Validation: Kotlin compilation, APK builds and unit tests; device appearance is left to the user's physical foldable (no simulator run).
