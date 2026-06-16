# YesMaam — Design System

A small, durable reference for the look and feel of YesMaam. The aesthetic is a
**quiet paper register**: warm serif type, soft pastels, generous whitespace,
flat surfaces edged with hairline borders instead of heavy shadows. Everything
here maps directly onto Jetpack Compose + Material 3 tokens.

> Companion to the spec: `docs/superpowers/specs/2026-06-17-yesmaam-attendance-design.md`.
> Visual source of truth: `docs/mockups/index.html`.

---

## 1. Principles

1. **Paper, not glass.** Surfaces are matte and flat. Depth comes from hairline
   borders (`outline`) and one soft long shadow reserved for genuinely floating
   things (FAB-like buttons, bottom sheets). No Material drop-shadow stacks.
2. **Serif everywhere.** Fraunces for anything that announces (titles, numbers,
   the brand); Lora for everything you read (rows, captions, body).
3. **Colour carries meaning, never alone.** Present/Absent/Late/Holiday each have
   a colour *and* a letter/glyph. Status is never communicated by hue only.
4. **Calm by default.** One accent action per screen. Pastels stay in the
   background; ink-on-cream is the resting state.
5. **Thumb-friendly.** Every interactive element has a ≥48dp touch target even
   when it looks smaller.

---

## 2. Colour

### 2.1 Raw palette (the only hex values that exist)

| Token            | Hex       | Use                                   |
|------------------|-----------|---------------------------------------|
| `cream`          | `#FBF7F0` | App canvas / background               |
| `paper`          | `#FFFDFA` | Cards, sheets, fields                 |
| `ink`            | `#3E3A35` | Primary text                          |
| `inkSoft`        | `#9A9086` | Secondary text, captions              |
| `line`           | `#ECE3D6` | Hairline borders, dividers            |
| `sage`           | `#B7CBB0` | Present (fill)                        |
| `sageDeep`       | `#6E8C66` | **Primary / brand**, present text     |
| `sageTint`       | `#EAF1E7` | Present container, "taken" calendar    |
| `blush`          | `#E9BFC1` | Absent (fill)                         |
| `blushDeep`      | `#B9777B` | Absent text/accent                    |
| `blushTint`      | `#FBEDED` | Absent container                      |
| `peach`          | `#F2D2A9` | Late + Holiday (fill)                 |
| `peachDeep`      | `#C68E4F` | Late/Holiday text/accent              |
| `peachTint`      | `#FBF0E0` | Late/Holiday container                |
| `lavender`       | `#D3CBE4` | Secondary accent (fill)               |
| `lavenderDeep`   | `#8C7FAE` | Secondary accent text                 |
| `lavenderTint`   | `#F1EEF7` | Secondary container, "pending" chips   |
| `errorRed`       | `#B0413E` | Destructive only (delete)             |

### 2.2 Semantic roles

| Role            | Token         |
|-----------------|---------------|
| background      | `cream`       |
| surface         | `paper`       |
| onSurface       | `ink`         |
| onSurfaceVariant| `inkSoft`     |
| outline         | `line`        |
| primary / brand | `sageDeep`    |
| accent          | `lavenderDeep`|
| present         | `sage` / `sageDeep` / `sageTint` |
| absent          | `blush` / `blushDeep` / `blushTint` |
| late            | `peach` / `peachDeep` / `peachTint` |
| holiday         | `peach` / `peachDeep` / `peachTint` (paired with ☂ glyph) |

Late and Holiday intentionally share peach — they never appear in the same slot
(a status cell is one or the other), and the glyph/letter disambiguates.

### 2.3 Material 3 `ColorScheme` mapping (light)

```
primary            = sageDeep      onPrimary           = #FFFFFF
primaryContainer   = sageTint      onPrimaryContainer  = #39532F
secondary          = lavenderDeep  secondaryContainer  = lavenderTint
tertiary           = blushDeep     tertiaryContainer   = blushTint
error              = errorRed      onError             = #FFFFFF
background         = cream         onBackground        = ink
surface            = paper         onSurface           = ink
surfaceVariant     = #F2EADF       onSurfaceVariant    = inkSoft
outline            = line          outlineVariant      = #F0E8DC
```

Material 3 has no roles for present/absent/late/holiday, so they live in a
companion object exposed through a `CompositionLocal` (see §8). **Dynamic colour
(Material You) is disabled** — the pastel identity is fixed.

Dark mode is **out of scope for v1** (single light theme). The token structure
leaves room to add a dark scheme later without touching call sites.

---

## 3. Typography

Two families, bundled as assets (`res/font`) so the look is identical on every
device and inside generated PDFs.

- **Fraunces** (`opsz` optical, weights 400–700) — display, headlines, titles,
  big numbers.
- **Lora** (400/500, + italic) — body, labels, captions.

| Style (M3 slot)   | Family            | Size / Line | Use                              |
|-------------------|-------------------|-------------|----------------------------------|
| `displayLarge`    | Fraunces 600      | 54 / 56     | Brand splash, app title          |
| `headlineMedium`  | Fraunces 600      | 24 / 28     | Screen titles ("Today's Register")|
| `titleLarge`      | Fraunces 600      | 19 / 24     | Section / panel headings          |
| `titleMedium`     | Fraunces 600      | 16 / 20     | Class names, stat values          |
| `headlineSmall`*  | Fraunces 600      | 23 / 26     | Stat "big number"                 |
| `bodyLarge`       | Lora 400          | 16 / 24     | Primary reading text             |
| `bodyMedium`      | Lora 400          | 14.5 / 21   | List rows, secondary text        |
| `labelLarge`      | Lora 500          | 14.5 / 18   | Buttons                          |
| `labelMedium`     | Lora 500          | 12.5 / 16   | Pills, chips, tab labels         |
| `labelSmall`      | Lora 400 italic   | 11.5 / 15   | Captions, the "good morning" hello|

\* mapped onto an existing M3 slot in code; listed separately for clarity.

Letter-spacing: Fraunces titles `-0.01em`; everything else default. Italics are
a deliberate motif (captions, the day's date) — they give the handwritten-ledger
warmth.

---

## 4. Spacing, shape, elevation

**Spacing** — 4dp base scale: `4, 8, 12, 16, 20, 24, 32`.
- Screen horizontal padding: **18dp**.
- Card inner padding: **12–14dp**.
- Gap between stacked cards/rows: **10dp**.
- Section spacing: **20–24dp**.

**Corner radius**
| Element                       | Radius |
|-------------------------------|--------|
| Pills / chips                 | full (999) |
| Status toggle buttons         | 9dp    |
| Calendar cell                 | 10dp   |
| Fields                        | 13dp   |
| Primary/ghost buttons         | 14dp   |
| Stat cards, selectors         | 14dp   |
| Class emoji tile              | 14dp   |
| List/student cards            | 16dp   |
| Bottom sheets, large panels   | 20dp   |

Maps to M3 `Shapes`: `small = 9`, `medium = 14`, `large = 16`, `extraLarge = 20`.

**Elevation**
- Default surfaces: **0 elevation**, `1dp` `line` border instead.
- The one soft shadow, for floating elements only:
  `shadow: 0 18px 40px -18px rgba(62,58,53,0.30)` → in Compose, a `Modifier.shadow`
  with low alpha + large blur, or simply reserve it for the primary button.
- Primary button: subtle sage glow `0 10px 20px -10px rgba(110,140,102,0.8)`.

---

## 5. Components

**Primary button** — `sageDeep` fill, white `labelLarge`, radius 14, full-width,
soft sage glow. **Ghost button** — `paper` fill, `line` border, `ink` text, no
shadow. One primary per screen.

**Card / list row** — `paper`, `line` border, radius 16, padding 12. Student row:
circular 34dp avatar (pastel fill, Fraunces initial, white glyph) · name
(`bodyMedium` 500) + sub-line (`labelSmall`, `inkSoft`) · trailing control.

**Status toggle (segmented P / A / L)** — three 30dp rounded squares (visual),
48dp touch target. Unselected: `paper` + `line` + `inkSoft` letter. Selected:
Present → `sage`/`sageDeep`, Absent → `blush`/`blushDeep`, Late → `peach`/`peachDeep`.
Letter in Fraunces 600. Selection animates as a 150ms colour crossfade.

**Summary chip** — full-radius, tinted container + a 8dp status dot + count text
(`labelMedium`). Present→sageTint, Absent→blushTint, Late→peachTint.

**Class card (home)** — `paper`/`line`/radius 18; 46dp emoji tile (status tint
bg) · name (`titleMedium`) + info (`labelSmall`) · trailing state pill
(done=sageTint, not-yet=lavenderTint, holiday=peachTint, italic `labelSmall`).

**Class pill row** — horizontal scroll of full-radius pills; selected =
`sageDeep`/white, rest = `paper`/`line`/`inkSoft`.

**Field** — `paper`/`line`/radius 13; italic `labelSmall` label above a
`bodyLarge` value; placeholder in `#C9C0B4`.

**Stat card** — `paper`/`line`/radius 14, centered; Fraunces big number +
italic `labelSmall` label.

**Calendar cell** — 38dp, radius 10. States (data-driven, **no weekend logic**):
- *Taken*: `sageTint` bg, `#D6E4D0` border, ink-green text, small `sageDeep` dot.
- *Holiday*: `peachTint` bg, peach border, `peachDeep` ☂ glyph.
- *Today*: 2dp `sageDeep` outline, bold number.
- *Empty / future*: `paper`, muted `#C7BDAE` number.
A legend (taken · ☂ holiday · today) sits below the grid. Tapping a day opens a
small action area (Take attendance / Mark holiday).

**Bottom navigation** — `paper`, top `line` border, 60dp; glyph + `labelMedium`;
active = `sageDeep`, rest = `inkSoft`. In-class tabs: Today · Calendar · Students
· Reports. Top-level tabs: Classes · Settings.

**Report grid (PDF/Excel)** — Fraunces header row on `#FAF6EF`; cells `8.5pt`,
`line` borders; P = sageDeep bold, A = blushDeep bold, H = peachDeep on peachTint;
totals column in Fraunces.

---

## 6. Iconography & imagery

Simple line glyphs only (`✓ ▦ ❖ ▤ ⚙ ☂ › ＋`); no filled icon sets to keep the
APK and the visual noise small. **Emoji** are used deliberately as class
identity (📕 🏺 🎨 🎵 ⚽…) — a curated picker, not freeform. No photos, no
illustrations.

---

## 7. Motion & accessibility

- **Motion**: 150–200ms ease-in-out. Toggle = colour crossfade; navigation =
  default M3 transitions; calendar month change = horizontal slide. Nothing
  bouncy.
- **Contrast**: body text is `ink` on `cream`/`paper` (passes AA). Status text
  uses the *deep* variant on its *tint* container; verify each pairing ≥ 4.5:1
  for the letter glyphs.
- **Non-colour cues**: every status shows a letter or glyph, never colour alone.
- **Touch targets**: ≥48dp on all controls, including the 30dp-looking toggles.
- **Type scaling**: respect system font scale; layouts use wrap/min-height, not
  fixed text heights.

---

## 8. Compose implementation map

```
ui/theme/
  Color.kt        // raw palette as val Color(...) constants
  YesMaamColors.kt// data class of semantic extras (present/absent/late/holiday
                  //   tints) + LocalYesMaamColors CompositionLocal
  Type.kt         // Fraunces + Lora FontFamily, Typography{} per §3
  Shape.kt        // Shapes(small=9, medium=14, large=16, extraLarge=20)
  Theme.kt        // YesMaamTheme: lightColorScheme(...) per §2.3,
                  //   dynamicColor=false, provides LocalYesMaamColors
res/font/         // fraunces_*.ttf, lora_*.ttf (also loaded as Typeface for PDF)
```

Usage rules:
- Call sites read colour from `MaterialTheme.colorScheme.*` for standard roles
  and `LocalYesMaamColors.current.*` for status colours — **never hard-code hex**
  outside `Color.kt`.
- The PDF/Excel exporters reuse the same `Color.kt` constants and the bundled
  font files so on-screen and on-paper match exactly.
