# Vinilogs — Design Direction

**Status:** locked for v1.0. Changing anything in §2–§6 requires an ADR (`02-ARCHITECTURE.md` §7).
**Applies to:** every screen, component and piece of UI copy in the app.
**Reference:** [Nomad Coffee](https://nomadcoffee.es/en/) — minimal monochrome shell, full-colour product photography, generous whitespace, text-link CTAs, no ornament.

---

## 1. The one-line rule

> **The interface is black and white. The records are the colour.**

Every pixel of chrome — bars, buttons, type, dividers, icons, empty states — is drawn from a neutral grey ramp. The only saturated colour on screen is album artwork. This is the whole design system in one sentence; if a decision is ambiguous, the option that keeps chrome monochrome and lets sleeves carry the colour is the right one.

This is not a stylistic preference alone. It serves the product principles:

- **Respect the collector's knowledge** — a sleeve is the thing collectors recognise. Nothing should compete with it.
- **Speed over completeness** — no gradients, shadows or tint layers means less overdraw and fewer states to design.
- **The collection is sacred** — a neutral shell makes the user's own content the subject, not our branding.

---

## 2. Colour

One neutral ramp, two themes, exactly one chromatic token. All values are opaque; alpha is never used to fake a tint.

### 2.1 Neutral ramp

| Token | Light | Dark | Use | Floor |
|---|---|---|---|---|
| `Ink900` | `#0A0A0A` | `#F5F5F5` | Primary text, primary button fill, active icons | 4.5:1 |
| `Ink700` | `#3D3D3D` | `#C7C7C7` | Secondary text, subtitles | 4.5:1 |
| `Ink500` | `#686868` | `#9A9A9A` | Meta text, placeholders, inactive icons, catalogue numbers | 4.5:1 |
| `Ink400` | `#858585` | `#6E6E6E` | **Control outlines** — resting text-field rule, unselected chip | 3:1 |
| `Ink300` | `#A3A3A3` | `#5C5C5C` | Disabled text and icons | exempt |
| `Ink200` | `#D4D4D4` | `#2E2E2E` | **Decorative hairlines** — list dividers, scrolled app-bar rule | exempt |
| `Ink100` | `#EDEDED` | `#1F1F1F` | Surface variant, skeletons, sleeve placeholders | — |
| `Surface` | `#FFFFFF` | `#141414` | Cards, sheets, dialogs | — |
| `Background` | `#FAFAFA` | `#0A0A0A` | Screen background | — |

Note the ramp **inverts** between themes — `Ink900` is the near-black in light and the near-white in dark. Name tokens by role, never by literal colour, so the same call site works in both.

**`Ink400` and `Ink200` are not interchangeable.** `Ink400` is the boundary of an interactive control, so WCAG 1.4.11 requires it to clear 3:1 — it is what tells a user the text field is there. `Ink200` is a divider that carries no information; a divider dark enough to pass 3:1 reads as a heavy border and wrecks the lightness of the whole system, so it deliberately sits below the floor. Reaching for `Ink200` on a text field is the most likely mistake in this palette.

Every combination above is verified in §7.

### 2.2 The single chromatic token

| Token | Light | Dark | Use |
|---|---|---|---|
| `Alert` | `#B3261E` | `#F2B8B5` | **Only** destructive confirmation and form validation errors |

**Tradeoff, stated:** a purely monochrome system cannot signal danger by colour alone, and "the collection is sacred" requires that deleting a record be unmistakable. Rather than weaken that, we spend exactly one colour on it. `Alert` never appears for anything else — not for badges, not for unread counts, not for emphasis. If you are reaching for it and the user is not about to lose data or has not made a mistake, the answer is weight or a hairline, not colour.

Everything else that would conventionally be coloured is expressed monochromatically:

- **Unread chat** — a filled `Ink900` dot, not a red badge.
- **Selected state** — inverted fill (`Ink900` background, `Background` text), not a tint.
- **Success** — no colour. A checkmark and the copy.
- **Condition grades** — outlined chips, never a red-to-green scale, never stars (see glossary in `00-README.md`).

### 2.3 Forbidden

No gradients. No drop shadows. No coloured overlays on artwork. No brand accent beyond the ramp. Never desaturate a sleeve to "fit the theme" — that inverts the entire premise of §1.

---

## 3. Typography

**Family:** Inter (variable). One font file, subset to Latin + Latin-Ext. Ships in `core/designsystem/src/main/res/font/`.

**Tradeoff, stated:** the system default (Roboto) would cost zero APK bytes, but Inter's tighter neo-grotesque forms and its `tnum` (tabular figures) feature are what make catalogue numbers and condition grades read as data rather than prose. A subset variable Inter is ~250 KB — acceptable for the single strongest carrier of the reference's character. If APK size becomes a release blocker, fall back to Roboto and keep the scale below unchanged.

### 3.1 Scale

Mapped onto Material 3 slot names so it drops straight into a Compose `Typography`. Sizes in `sp`, line heights in `sp`, tracking in `sp`.

| Slot | Size / Line | Weight | Tracking | Use |
|---|---|---|---|---|
| `headlineLarge` | 32 / 40 | 600 | −0.5 | Screen titles on large-title screens |
| `headlineMedium` | 28 / 36 | 600 | −0.4 | Record detail: album title |
| `headlineSmall` | 24 / 32 | 600 | −0.3 | Section headers, empty-state headline |
| `titleLarge` | 22 / 28 | 600 | −0.2 | Top app bar title |
| `titleMedium` | 16 / 24 | 600 | −0.1 | Card titles, list item primary |
| `titleSmall` | 14 / 20 | 600 | 0 | Dense list primary |
| `bodyLarge` | 16 / 24 | 400 | 0 | Body copy, chat messages |
| `bodyMedium` | 14 / 20 | 400 | 0 | Secondary body, list item supporting |
| `bodySmall` | 12 / 16 | 400 | +0.1 | Grid captions under sleeves |
| `labelLarge` | 14 / 20 | 600 | +0.4 | Button labels |
| `labelMedium` | 12 / 16 | 600 | +0.8 | **Uppercase.** Section eyebrows, nav labels |
| `labelSmall` | 11 / 16 | 500 | +1.0 | **Uppercase.** Chips, condition grades, timestamps |

### 3.2 Rules

- **Uppercase is structural, not decorative.** Only `labelMedium` and `labelSmall` are uppercased, and only for eyebrows, nav, and chips. Never uppercase a headline, an artist name or an album title — collectors read those as proper nouns.
- **Negative tracking on headlines, positive on labels.** This is the single most recognisable trait of the reference; do not normalise it to 0.
- **Catalogue numbers, condition grades and any figure in a column use tabular figures** (`FontFeature("tnum")`). `PCS 7027` must align down a list.
- **As few type sizes per screen as the hierarchy genuinely needs.** The shelf grid needs three (count, artist, caption). A metadata-dense screen like Record Detail legitimately needs more — headline, subtitle, chip label, meta key, meta value are five different jobs, and collapsing them loses the hierarchy rather than simplifying it. The discipline is not a hard ceiling; it is that every size on screen must map to a named slot in §3.1 and a distinct job — never a one-off size invented for a single element.
- Never fake a weight by scaling. Never use italic.

---

## 4. Space, shape and elevation

### 4.1 Spacing — 4dp base grid

| Token | dp | Typical use |
|---|---|---|
| `xxs` | 2 | Icon-to-label optical nudge |
| `xs` | 4 | Inside chips |
| `sm` | 8 | Between related lines of text |
| `md` | 12 | List item vertical padding |
| `lg` | 16 | Grid gutter, standard block gap |
| `xl` | 24 | Between sections |
| `xxl` | 32 | Above a section header |
| `xxxl` | 48 | Around empty states |

**Screen horizontal gutter is `20.dp`** — deliberately wider than Android's default 16, because whitespace is the reference's primary tool. The shelf grid is the one exception: it goes full-bleed to `16.dp` so a third column stays readable.

### 4.2 Shape — sharp

| Slot | Radius | Rationale |
|---|---|---|
| `extraSmall` | 0 dp | Chips, inputs |
| `small` | 0 dp | Buttons |
| `medium` | 2 dp | Cards |
| `large` | 4 dp | Dialogs |
| `extraLarge` | 16 dp | Bottom sheets (top corners only) |

**Album sleeves are always `0.dp` and always square (1:1).** A sleeve is a square object; rounding it or cropping it to any other ratio is wrong regardless of what the layout wants.

### 4.3 Elevation — none

`0.dp` everywhere. No shadows, no tonal elevation overlays. Surfaces separate by **a 1dp `Ink200` hairline** or by the `Background` / `Surface` tone difference. A top app bar gains its hairline only once content scrolls beneath it; it has none at rest.

---

## 5. Components

**Buttons** — 48dp tall, rectangular, `labelLarge`.

- *Primary:* `Ink900` fill, `Background` label. Full-width on forms, intrinsic elsewhere.
- *Secondary:* transparent, 1dp `Ink900` outline, `Ink900` label.
- *Tertiary:* label only with a 1dp underline — the reference's text-link CTA. Use for anything non-committal ("See all pressings").
- *Destructive:* `Alert` outline and label. Filled `Alert` only inside a confirmation dialog.

**Top app bar** — transparent, no elevation, `titleLarge`, hairline on scroll only.

**Bottom navigation** — 1dp `Ink200` top hairline. Outline icons; active is filled `Ink900` with its label, inactive is `Ink500`. No pill indicator.

**Shelf grid** — square sleeves, 3 columns portrait / 5 landscape, `lg` gutter. Caption below each: artist in `bodySmall` `Ink900`, title in `bodySmall` `Ink500`, both single-line with ellipsis. No card and no shadow around a sleeve — only the 1dp inset edge described in §6.

**Sleeve placeholder** — `Ink100` square containing the catalogue number in `labelSmall` `Ink500`, centred. Never a generic music-note icon; the catalogue number is the useful thing.

**List rows** — `md` vertical padding, full-bleed 1dp `Ink200` divider between rows, none after the last.

**Metadata row (record detail key/value table)** — a distinct, denser component: `sm` vertical padding rather than a generic list row's `md`, because a collector wants the full spec sheet on screen without excess scrolling. Same full-bleed `Ink200` divider rule, none after the last row. `meta-k` in `labelMedium` uppercase `Ink500`, `meta-v` in `bodyLarge` `Ink900`, baseline-aligned.

**Condition chip** — 1dp `Ink400` outline, 0dp radius, `labelSmall` uppercase tabular, `Ink700`. Renders the grade abbreviation exactly as in the glossary (`M`, `NM`, `VG+`, `VG`, `G`, `F`, `P`).

**Chat bubbles** — own messages: `Ink900` fill, `Background` text. Theirs: `Ink100` fill, `Ink900` text. 2dp radius. A record shared into a thread renders as a square sleeve with a two-line caption — the only colour in the conversation.

**Text fields** — no filled container. 1dp `Ink400` bottom rule that becomes 2dp `Ink900` on focus and 2dp `Alert` on error. Label above in `labelMedium` uppercase.

**Empty states** — `headlineSmall` line, one `bodyMedium` line, one tertiary button. No illustration.

---

## 6. Motion and imagery

**Motion** — 150ms for state changes, 200ms for transitions, standard easing. No bounce, no overshoot, no spring. Optimistic UI means the result is drawn before the network confirms; motion should never imply waiting that isn't happening.

**Imagery** — sleeve artwork is full colour, unmodified, always. `ContentScale.Crop` on a 1:1 box, `FilterQuality.Medium`. Crossfade in over 150ms from the `Ink100` placeholder; never a spinner over a sleeve. User avatars are 1:1 circles at `Ink100` when absent, showing initials in `labelMedium`.

**Every sleeve carries a 1dp inset `Ink200` edge.** This is the one place the system needs a border it would otherwise refuse. Sleeve art regularly runs to the edge of the square in near-black or near-white — a black-sleeve pressing dissolves into `Background` in dark theme, a plain white sleeve dissolves into it in light. The inset rule is invisible against the other 95% of artwork and is the only thing keeping those two cases from looking like a rendering failure. Inset, not outset, so the sleeve stays exactly 1:1 and the grid does not shift.

---

## 7. Accessibility floor — verified

- Body and label text meets **WCAG AA 4.5:1** against its background. `Ink500` is the lightest token permitted for text anywhere.
- Control outlines meet **WCAG 1.4.11, 3:1**. `Ink400` is the token; `Ink200` is not.
- `Ink300` (disabled) and `Ink200` (decorative dividers) are exempt: neither ever carries information.
- Every interactive target is **≥48×48dp** regardless of its drawn size.
- No state is signalled by colour alone — `Alert` always accompanies an icon and explicit copy.
- The app supports system font scaling to 200%; never set a fixed `dp` height on a container holding text.

### 7.1 Measured ratios

Every text token against every background it can legally sit on, both themes:

| Token | on `Background` | on `Surface` | on `Ink100` |
|---|---|---|---|
| `Ink900` light | 18.97 | 19.80 | 16.91 |
| `Ink700` light | 10.41 | 10.86 | 9.28 |
| `Ink500` light | 5.34 | 5.57 | **4.76** |
| `Alert` light | 6.26 | 6.54 | 5.58 |
| `Ink900` dark | 18.16 | 16.90 | 15.12 |
| `Ink700` dark | 11.71 | 10.90 | 9.75 |
| `Ink500` dark | 7.04 | 6.55 | 5.86 |
| `Alert` dark | 11.60 | 10.79 | 9.65 |

Control outlines (3:1 floor): `Ink400` light 3.54 / 3.69 / 3.15 · dark 3.88 / 3.61 / 3.23.
Inverted fills: primary button 18.97 light, 18.16 dark · `onError` on `Alert` 6.54 light, 7.66 dark.

**The tight one is `Ink500` on `Ink100` at 4.76** — that is the catalogue number inside a sleeve placeholder, and it is the pair that constrains the whole light ramp. If you ever lighten `Ink500` or darken `Ink100`, that combination breaks first. Re-run the check before changing either.

---

## 8. Checklist for any new screen

1. Is every colour on screen from the ramp, except album artwork?
2. Are there three or fewer type sizes?
3. Is every gap a spacing token?
4. Are all sleeves square and unrounded?
5. Is there any shadow? (There must not be.)
6. Does it read at 200% font scale and in dark theme?
7. Does it survive with no network, showing cached data rather than a spinner?
