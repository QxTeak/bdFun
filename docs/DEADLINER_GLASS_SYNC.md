# Deadliner glass and adaptive layout sync

Source: AritxOnly/Deadliner local working tree, based on `3b561656e1ddca43220a39763f12aaa0c67fdaba`, captured 2026-11-10. This includes the author's uncommitted wide/fold header, readable-list width and glass button changes. Source file hashes are recorded in `deadliner-source-sha256.json`. Remote HEAD `d17b21e0c3483cb563421f83a6d8b249eaca77c7` was checked; the compared remote changes do not replace the glass implementation used here.

## Material compatibility

The full optical stack, shader, sampling padding, edge helpers, surface/icon/button/FAB, shadow host, and progressive header are ported into TeakUI. Package names and theme/backdrop adapters use the existing Teak contracts. The published MIUIX 0.9.4 dependency remains unchanged.

bdFun's existing `TeakMaterialTuning`, `TeakMaterialFineTuning`, `TeakModalMaterialTuning`, recipe coefficients and light/dark shadow radius/spread/alpha remain unchanged. The progressive host also uses bdFun's existing top-bar blur radius (16), tint alpha (0.66), gradient start (0.62), end (1) and curve (1.25). The existing base wash is retained, now inside materialAlpha so it disappears together with the glass. The new rim-width field uses Deadliner's 0.6 dp default. The official MIUIX glass dependency closure is vendored with its Apache-2.0 notices; the optional official-material path defaults off to preserve bdFun's material settings. Non-shader and no-backdrop fallbacks remain available.

The new general button and FAB are reusable library components; existing app controls retain their callbacks. The app's navigation-specific badges and classic style remain in the existing tab component. No unrelated Deadliner business screens or assets are imported. Deadliner MIT license is retained under `TeakUI/third_party/deadliner`.

## Layout and scroll ownership

- Actual list/grid position drives smoothstep glass reveal over 4–28 dp, independent of large-title collapse. Returning to the first item clears the material. Home and dynamic tabs have independent state; Activity pages provide scoped state.
- One backdrop records content only. The progressive background and button shadow foreground sit outside that recorder. Header buttons share the same animated material progress.
- Fixed wide/fold headers use Deadliner's window policy: width >= 400 dp and height/width <= 1.65. The main navigation moves to a rail for these windows at width >= 600 dp, preserving the same pager selection.
- Home uses adaptive video columns on wide viewports; dynamic uses an author sidebar and bounded feed at >= 840 dp; Mine uses two card columns at >= 600 dp.
- Secondary titled pages center content using Deadliner's policy: at >= 600 dp use min(70% of viewport, 680 dp). Registered lists can be scrolled from the gutters. Search and terms content also use bounded columns. Video fullscreen behavior remains owned by the player.
- Main/terms Activity portrait locks are removed so window rotation reaches the adaptive layout.

## Validation

Run `:app:assembleDebug :TeakUI:testDebugUnitTest`. Host tests cover wide/fold boundaries, density-independent reveal, top reset and independent tab states. Emulator checks and any limitations are recorded in the PR.
