# YesMaam — Attendance Manager · Design Spec

**Date:** 2026-06-17
**Status:** Approved direction, pending spec review
**Design system:** `docs/design-system.md`
**Mockups:** `docs/mockups/index.html`

---

## 1. Overview

YesMaam is a single-user Android app for a teacher to track attendance for any
number of classes — a school section, a pottery course, a weekend workshop — and
hand out a tidy **monthly report as Excel (.xlsx) and PDF**. It works fully
offline, stores everything on-device, and is built to stay small in both code
and footprint. The aesthetic is a quiet paper register: serif type, soft
pastels, extremely simple flows (see the design system).

### Goals
- Mark daily attendance for a class in a couple of taps; it auto-saves.
- Manage multiple, arbitrary classes, each with its own roster and calendar.
- Mark holidays per class; show them clearly on a month calendar.
- Export a monthly report to **Excel** and **PDF**, via an engine that makes
  adding a new format (e.g. CSV) a small, isolated change.
- Stay minimal: few dependencies, small surface area, readable code.

### Non-goals (v1)
- No accounts, login, cloud sync, or multi-device.
- No multi-teacher / role management.
- No notifications, reminders, or analytics dashboards.
- No dark theme (structure allows it later).
- No bulk import (CSV import of students) — noted as an easy later add.

---

## 2. Users & key decisions (locked)

Single user: the teacher, on their own phone. Decisions confirmed during
brainstorming:

| Decision | Resolution |
|----------|------------|
| Class type | **Generic** "named group of people you take attendance for" (school, course, club). |
| Statuses | **Present · Absent · Late.** |
| Saving | **Auto-save on tap** — no explicit save button. |
| Roster fields | **Name, roll number, optional guardian phone.** |
| Holidays | **Per-class**, markable, shown on the calendar. |
| Weekends | **No special-casing** (see §5.3 session rule). |
| Person in two classes | **Separate roster entry per class.** |
| App name | **YesMaam.** |

---

## 3. Tech stack

- **Language/UI:** Kotlin + Jetpack Compose, **Material 3** (existing scaffold).
- **Min SDK 24, target 36** (existing).
- **Persistence:** **Room** (entities/DAOs/DB) — relational data with monthly
  aggregation queries; rolling our own would be more code.
- **Settings:** Android **SharedPreferences** (two strings) — zero extra dep.
- **Navigation:** `navigation-compose`.
- **State:** `androidx.lifecycle` `ViewModel` + `viewModelScope` + Kotlin
  `Flow`; `lifecycle-viewmodel-compose`.
- **Dates:** `java.time` (`LocalDate`, `YearMonth`) via **core library
  desugaring** (required on minSdk 24).
- **DI:** **Manual** — a tiny `AppContainer` held by the `Application`. No Hilt
  (keeps it minimal).
- **Excel export:** a **hand-rolled minimal OOXML writer** (`java.util.zip`) —
  Apache POI is too heavy and `fastexcel` needs StAX (`javax.xml.stream`), which
  Android lacks. ~150 lines, no dependency, fully controlled.
- **PDF export:** Android's built-in `android.graphics.pdf.PdfDocument` + Canvas,
  drawn with the bundled serif fonts — no dependency.
- **Sharing:** `FileProvider` + `Intent.ACTION_SEND` (covers WhatsApp, mail,
  Drive, Files). No runtime permissions needed.

New dependencies (to add to `gradle/libs.versions.toml` + `app/build.gradle.kts`):
Room (`room-runtime`, `room-ktx`, `room-compiler` via **KSP**), the KSP plugin,
`navigation-compose`, `lifecycle-viewmodel-compose`, and
`coreLibraryDesugaring(desugar_jdk_libs)`.

---

## 4. Architecture

Light MVVM in a single `:app` module. Each unit has one job and a clear
interface; the report core is pure (no Android types) so it is trivially
testable.

```
com.example.yesmaam/
  MainActivity.kt              // hosts NavGraph in YesMaamTheme
  YesMaamApp.kt                // Application; builds AppContainer (DB, repo, prefs)
  di/AppContainer.kt           // manual DI: exposes repository, settings, exporters

  data/
    db/AppDatabase.kt
    db/entities.kt             // ClassEntity, StudentEntity, AttendanceEntity, HolidayEntity
    db/ClassDao.kt  StudentDao.kt  AttendanceDao.kt  HolidayDao.kt
    AttendanceRepository.kt     // single repository over the DAOs
    SettingsStore.kt            // SharedPreferences (teacher name, institution)

  domain/
    model/Models.kt            // ClassInfo, Student, AttendanceStatus, DayCell...
    report/MonthlyReport.kt    // pure data model of a month's report
    report/ReportBuilder.kt    // pure: (class, students, attendance, holidays, month) -> MonthlyReport

  export/
    ReportExporter.kt          // interface { format; fun write(report, OutputStream) }
    ExportFormat.kt            // enum EXCEL, PDF (extensible)
    XlsxReportExporter.kt
    PdfReportExporter.kt
    ooxml/XlsxWriter.kt        // low-level: build a valid .xlsx zip
    ExportDelivery.kt          // write to cache, share via FileProvider

  ui/
    theme/                     // Color, YesMaamColors, Type, Shape, Theme (per design system)
    nav/NavGraph.kt  Routes.kt
    components/                // StudentRow, StatusToggle, SummaryChip, ClassCard,
                               //   CalendarGrid, StatCard, PrimaryButton, Field, ClassPillRow
    classes/ClassesScreen.kt + ClassesViewModel.kt
    classes/ClassEditorScreen.kt + ClassEditorViewModel.kt
    classroom/ClassHomeScreen.kt          // hosts in-class bottom nav + 4 tabs, scoped to classId
    classroom/today/TodayScreen.kt + TodayViewModel.kt
    classroom/calendar/CalendarScreen.kt + CalendarViewModel.kt
    classroom/students/StudentsScreen.kt + StudentsViewModel.kt
    classroom/students/StudentEditorScreen.kt + StudentEditorViewModel.kt
    classroom/reports/ReportsScreen.kt + ReportsViewModel.kt
    classroom/reports/PdfPreview.kt
    settings/SettingsScreen.kt + SettingsViewModel.kt
```

**Data flow:** Screen → ViewModel → Repository → DAO (Room) → Flow back up.
Report export: ViewModel asks `ReportBuilder` (pure) for a `MonthlyReport`, hands
it to the chosen `ReportExporter`, then `ExportDelivery` shares the file.

---

## 5. Data model & rules

### 5.1 Entities

- **ClassEntity** — `id`, `name`, `note?`, `emoji`, `colorKey` (palette key, e.g.
  "sage"), `createdAt`, `archived` (default false).
- **StudentEntity** — `id`, `classId` (FK→Class, cascade delete), `name`,
  `rollNumber` (string), `guardianPhone?`, `createdAt`. Sorted by roll then name.
- **AttendanceEntity** — `id`, `studentId` (FK→Student, cascade), `date`
  (epoch-day Long), `status` (`PRESENT`/`ABSENT`/`LATE`). **Unique(studentId,
  date).** Index on `(studentId, date)` and a derived class+date query path.
- **HolidayEntity** — `id`, `classId` (FK→Class, cascade), `date` (epoch-day),
  `note?`. **Unique(classId, date).**

Relationships: a Class has many Students; a Student has many Attendance records;
a Class has many Holidays. A real person enrolled in two classes is two separate
StudentEntity rows (locked decision).

### 5.2 Status semantics
- **Present / Late** both count as "attended" for the percentage; **Late** is
  recorded and reported distinctly (own column/count).
- **Absent** counts in the denominator, not the numerator.
- A student with **no record** on a session day is treated as **Absent** for
  totals (a blank in a register means absent). In practice this rarely happens
  because of the session rule below.

### 5.3 Session-day rule (the heart of the model)
A date is a **session** for a class iff it is **not** a holiday **and** at least
one attendance record exists for a student of that class on that date.

- Opening **Today** (or a calendar day) shows every student defaulted to
  **Present**. The **first tap** persists the whole roster's current statuses for
  that date → the day becomes a session. Open-and-leave (no tap) creates nothing.
- **Monthly percentage (per student)** = `(present + late) / sessionCount × 100`,
  where `sessionCount` = number of session dates in the month.
- This makes weekends irrelevant: you only get session days where you actually
  took attendance. Saturday classes count; un-taught Saturdays never appear.

### 5.4 Holidays
Marking a day a holiday (from Today's "☂ Mark holiday" bar, or the calendar
day action) writes a HolidayEntity and excludes that date from sessions. Holiday
overrides attendance: you cannot take attendance on a holiday; un-marking it
restores a normal empty day. Holidays appear as a peach `H` column in reports.

---

## 6. Screens & flows

All screens follow the design system. Mirrors `docs/mockups/index.html`.

**A1 · Classes (home).** A list of class cards (emoji, name, info, today's
state: done / not-yet / holiday) + "New class". Top-level bottom nav: Classes ·
Settings. Tap a card → ClassHome.

**A2 · Class editor (new/edit).** Fields: name, optional note, emoji picker
(curated), colour picker (palette). Save → returns to home. Edit reachable from
within a class. Delete (with confirm) cascades the class's data.

**ClassHome.** Scoped to a `classId`; hosts the in-class bottom nav with four
tabs, app bar showing "‹ {class name}".

**B1 · Today.** Date = today. A "☂ no class today? Mark holiday" bar. Summary
chips (present/absent/late counts). Student rows each with a P/A/L toggle
defaulting to Present. Tapping a toggle auto-saves (and creates the session on
first tap). If today is a holiday, the list is replaced by a holiday state with
"Remove holiday".

**B2 · Calendar.** Month grid (Mon–Sun columns, no weekend special-casing in
logic). Cell states: taken (sage), holiday (peach ☂), today (ring), empty/future
(muted). Month `‹ / ›` navigation. Legend below. Tapping a day:
- empty past/today → "Mark holiday" option;
- holiday → shows holiday + "Remove holiday".

> **v1 status (amended 2026-06-17):** the calendar marks/removes holidays and
> shows session state. Back-dated attendance editing (opening a non-today day's
> register from the calendar) is deferred — `repo.saveDay(date, …)` already accepts
> any date, so wiring a date-parameterised register later is small. Today's
> attendance is taken from the **Today** tab.

**C1 · Students.** Roster for this class: avatar, name, roll · ☎ guardian ·
this-month %. "Add a student". Tap a row → Student editor.

**C2 · Student editor.** Name, roll number, class (fixed to current), optional
guardian phone. Save / Delete (confirm; cascades the student's attendance).

**C3 · Reports.** Month `‹ / ›` selector. Stat cards: **school days** (session
count), **avg present %**, **holidays**. Per-student % rows. Two export buttons:
**Excel** and **PDF** → builds the report, generates the file, opens the share
sheet. (A PDF preview screen shows the rendered register before sharing.)

**Settings.** Teacher display name + optional institution name (used in the
report header). Stored in SharedPreferences.

### Navigation graph
```
classes ──▶ classEditor?classId={id|null}
   │
   └▶ class/{classId} (ClassHome: Today | Calendar | Students | Reports)
                         Students ──▶ studentEditor?classId={id}&studentId={id|null}
                         Reports  ──▶ pdfPreview (in-tab)
classes ──▶ settings
```

---

## 7. Export subsystem (extensibility point)

The single extension seam. Adding CSV or a new layout = one new
`ReportExporter`; nothing else changes.

```kotlin
enum class ExportFormat(val mime: String, val ext: String) {
  EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
  PDF("application/pdf", "pdf")
}

interface ReportExporter {
  val format: ExportFormat
  fun write(report: MonthlyReport, out: OutputStream)
}
```

- **MonthlyReport** (pure) — class info, `YearMonth`, sorted `sessionDates`,
  `holidayDates`, the **column dates** = `sorted(sessionDates ∪ holidayDates)`,
  and per-student rows (`statusByDate`, presentCount, lateCount, absentCount,
  attendedCount, percentage), plus summary (students, sessionCount, holidayCount,
  avg%). Built by `ReportBuilder` from DAO data — no Android imports.
- **XlsxReportExporter** → uses `XlsxWriter` to emit a valid one-sheet workbook:
  header (Name, Roll, day columns, P, A, L, %), body letters (P/A/L/H), light
  cell fills mirroring the palette via a minimal `styles.xml`.
- **PdfReportExporter** → `PdfDocument` landscape page(s), Fraunces/Lora
  `Typeface` from bundled fonts, the register grid drawn with Canvas, paginating
  students vertically; header carries teacher/institution from settings.
- **ExportDelivery** → write to `cacheDir/exports/`, expose via `FileProvider`
  (`${applicationId}.fileprovider`, `res/xml/file_paths.xml`), launch
  `ACTION_SEND`. (Save-to-Downloads via MediaStore is a noted easy follow-up.)

`AndroidManifest.xml` gains the `FileProvider` `<provider>`; no new permissions.

---

## 8. Testing strategy

- **Unit (JVM, no Android):** `ReportBuilder` — session detection, percentage
  math, late handling, holiday exclusion, empty months, missing-record-as-absent.
  This is the correctness core and gets the most coverage (TDD).
- **Unit (JVM):** `XlsxWriter` — output is a valid zip containing the expected
  OOXML parts; cell values/escaping correct.
- **Instrumented:** Room DAOs (in-memory DB) — uniqueness constraints, cascade
  deletes, class+date queries; `PdfReportExporter` produces a non-empty,
  openable PDF.
- **Compose UI (smoke):** Today toggle auto-saves; calendar paints holiday/taken
  states; Reports export buttons fire.

---

## 9. Build, milestones, risks

### Phasing (each independently runnable)
1. **Foundation** — deps, KSP, desugaring, theme (design system), fonts, nav
   skeleton with empty screens.
2. **Data** — Room entities/DAOs/DB, repository, manual `AppContainer`.
3. **Classes** — home + class editor (CRUD).
4. **Attendance** — Today screen, status toggle, auto-save, session rule.
5. **Calendar & holidays** — month grid, day actions, holiday marking.
6. **Students** — roster + student editor.
7. **Reports core** — `ReportBuilder` + `MonthlyReport` (heavily unit-tested).
8. **Export** — `XlsxWriter`/exporters + PDF + share delivery; Reports screen
   wiring + PDF preview.
9. **Settings** + polish pass.

### Risks / mitigations
- **Hand-rolled XLSX correctness** → strict adherence to the minimal OOXML parts;
  validate by unzip + open in Excel/Sheets; unit tests on structure.
- **PDF table pagination** (wide month × many students) → landscape page, fixed
  column widths, vertical pagination; cap day columns to the month's column dates
  (already excludes non-session days).
- **java.time on minSdk 24** → core library desugaring (standard, low risk).
- **Scope creep** → bulk import, save-to-Downloads, dark mode, CSV are explicitly
  deferred; the exporter interface keeps them cheap later.

### Out of scope (v1)
Accounts/sync, multi-teacher, notifications, dark mode, student-in-multiple-
classes as one record, bulk import, CSV export, analytics.
