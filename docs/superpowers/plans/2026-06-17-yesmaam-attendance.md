# YesMaam Attendance Manager — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an offline Android app for a teacher to track attendance across arbitrary classes and export monthly reports as Excel and PDF.

**Architecture:** Light MVVM in a single `:app` module. Compose + Material 3 UI, Room persistence, manual DI via an `AppContainer`. A pure (Android-free) `ReportBuilder` produces a `MonthlyReport` consumed by pluggable `ReportExporter`s (Excel via a hand-rolled OOXML writer, PDF via `android.graphics.pdf.PdfDocument`).

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room (KSP), navigation-compose, `java.time` (core-library desugaring), `FileProvider` sharing. No Hilt, no Apache POI.

**Companion docs:** spec `docs/superpowers/specs/2026-06-17-yesmaam-attendance-design.md`; design system `docs/design-system.md`; mockups `docs/mockups/index.html`.

---

## File Structure

```
com.example.yesmaam/
  MainActivity.kt              MainActivity hosting NavGraph in YesMaamTheme
  YesMaamApp.kt                Application; builds AppContainer
  di/AppContainer.kt           manual DI: db, repository, settings, exporters
  di/Vm.kt                     appContainer extension + viewModel factory helper

  data/db/Converters.kt        Room TypeConverters (AttendanceStatus)
  data/db/Entities.kt          ClassEntity, StudentEntity, AttendanceEntity, HolidayEntity
  data/db/ClassDao.kt
  data/db/StudentDao.kt
  data/db/AttendanceDao.kt
  data/db/HolidayDao.kt
  data/db/AppDatabase.kt
  data/AttendanceRepository.kt
  data/SettingsStore.kt        SharedPreferences (teacher name, institution)

  domain/model/Models.kt       AttendanceStatus, ClassInfo, StudentRef, AttendanceRecord
  domain/report/MonthlyReport.kt
  domain/report/ReportBuilder.kt   pure: ReportInputs -> MonthlyReport

  export/ExportFormat.kt
  export/ReportExporter.kt
  export/ooxml/XlsxWriter.kt
  export/XlsxReportExporter.kt
  export/PdfReportExporter.kt
  export/ExportDelivery.kt

  ui/theme/Color.kt  YesMaamColors.kt  Type.kt  Shape.kt  Theme.kt
  ui/nav/Routes.kt  NavGraph.kt
  ui/components/Components.kt   PrimaryButton, GhostButton, Field, SummaryChip, StatCard
  ui/components/StatusToggle.kt
  ui/components/StudentRow.kt
  ui/components/ClassCard.kt
  ui/components/CalendarGrid.kt
  ui/classes/ClassesScreen.kt  ClassesViewModel.kt
  ui/classes/ClassEditorScreen.kt  ClassEditorViewModel.kt
  ui/classroom/ClassHomeScreen.kt
  ui/classroom/today/TodayScreen.kt  TodayViewModel.kt
  ui/classroom/calendar/CalendarScreen.kt  CalendarViewModel.kt
  ui/classroom/students/StudentsScreen.kt  StudentsViewModel.kt
  ui/classroom/students/StudentEditorScreen.kt  StudentEditorViewModel.kt
  ui/classroom/reports/ReportsScreen.kt  ReportsViewModel.kt  PdfPreview.kt
  ui/settings/SettingsScreen.kt  SettingsViewModel.kt

res/font/                       fraunces_*.ttf, lora_*.ttf
res/xml/file_paths.xml          FileProvider paths
```

Test files live under `app/src/test/java/com/example/yesmaam/` (JVM) and
`app/src/androidTest/java/com/example/yesmaam/` (instrumented), mirroring package paths.

**Commands used throughout:**
- Build: `./gradlew :app:assembleDebug`
- JVM tests: `./gradlew :app:testDebugUnitTest`
- Instrumented tests (needs emulator/device): `./gradlew :app:connectedDebugAndroidTest`

---

## Phase 1 — Foundation

### Task 1: Initialize git and add dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Initialize the repository**

```bash
cd /Users/kanishkdebnath/Developer/YesMaam
git init
git add -A
git commit -m "chore: initial scaffold + design docs"
```

- [ ] **Step 2: Add version-catalog entries**

In `gradle/libs.versions.toml`, add under `[versions]`:

```toml
room = "2.7.1"
ksp = "2.2.10-2.0.2"
navigationCompose = "2.9.0"
lifecycleViewmodelCompose = "2.10.0"
desugarJdkLibs = "2.1.5"
```

Add under `[libraries]`:

```toml
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }
desugar-jdk-libs = { group = "com.android.tools", name = "desugar_jdk_libs", version.ref = "desugarJdkLibs" }
```

Add under `[plugins]`:

```toml
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 3: Apply plugins, desugaring, and dependencies**

In `app/build.gradle.kts`, change the `plugins { }` block to:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}
```

In `android { compileOptions { } }`, add core-library desugaring:

```kotlin
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
```

In `dependencies { }`, add:

```kotlin
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
```

- [ ] **Step 4: Verify the build syncs**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (dependencies resolve; app still the default template).

> If `ksp`/`room`/`navigationCompose` versions fail to resolve, bump to the latest published versions Gradle reports and re-run. KSP version must be `<kotlinVersion>-<kspVersion>`.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add Room, navigation, viewmodel-compose, desugaring"
```

---

### Task 2: Bundle the serif fonts

**Files:**
- Create: `app/src/main/res/font/fraunces_regular.ttf`
- Create: `app/src/main/res/font/fraunces_medium.ttf`
- Create: `app/src/main/res/font/fraunces_semibold.ttf`
- Create: `app/src/main/res/font/fraunces_bold.ttf`
- Create: `app/src/main/res/font/lora_regular.ttf`
- Create: `app/src/main/res/font/lora_medium.ttf`
- Create: `app/src/main/res/font/lora_italic.ttf`

- [ ] **Step 1: Download static TTFs into res/font**

```bash
mkdir -p app/src/main/res/font
BASE=https://github.com/google/fonts/raw/main/ofl
curl -L -o app/src/main/res/font/fraunces_regular.ttf  "$BASE/fraunces/static/Fraunces_9pt-Regular.ttf"
curl -L -o app/src/main/res/font/fraunces_medium.ttf   "$BASE/fraunces/static/Fraunces_9pt-Medium.ttf"
curl -L -o app/src/main/res/font/fraunces_semibold.ttf "$BASE/fraunces/static/Fraunces_9pt-SemiBold.ttf"
curl -L -o app/src/main/res/font/fraunces_bold.ttf     "$BASE/fraunces/static/Fraunces_9pt-Bold.ttf"
curl -L -o app/src/main/res/font/lora_regular.ttf      "$BASE/lora/static/Lora-Regular.ttf"
curl -L -o app/src/main/res/font/lora_medium.ttf       "$BASE/lora/static/Lora-Medium.ttf"
curl -L -o app/src/main/res/font/lora_italic.ttf       "$BASE/lora/static/Lora-Italic.ttf"
```

- [ ] **Step 2: Verify the files are real TTFs (not HTML error pages)**

Run: `file app/src/main/res/font/*.ttf`
Expected: each line reports `TrueType Font data` (or `OpenType`). If any reports HTML/ASCII, the static path moved — grab the static `.ttf` from https://fonts.google.com/specimen/Fraunces and https://fonts.google.com/specimen/Lora and place them with the exact filenames above. Filenames MUST be lowercase `[a-z0-9_]` starting with a letter.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/font
git commit -m "assets: bundle Fraunces and Lora fonts"
```

---

### Task 3: Theme — colours, type, shapes

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/ui/theme/Color.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/theme/YesMaamColors.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/theme/Type.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/theme/Shape.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/theme/Theme.kt`
- Delete (template leftovers): `ui/theme/Color.kt` content replaced; existing `Theme.kt` and `Type.kt` overwritten.

- [ ] **Step 1: Replace `Color.kt` with the raw palette**

```kotlin
package com.example.yesmaam.ui.theme

import androidx.compose.ui.graphics.Color

val Cream = Color(0xFFFBF7F0)
val Paper = Color(0xFFFFFDFA)
val Ink = Color(0xFF3E3A35)
val InkSoft = Color(0xFF9A9086)
val Line = Color(0xFFECE3D6)

val Sage = Color(0xFFB7CBB0)
val SageDeep = Color(0xFF6E8C66)
val SageTint = Color(0xFFEAF1E7)

val Blush = Color(0xFFE9BFC1)
val BlushDeep = Color(0xFFB9777B)
val BlushTint = Color(0xFFFBEDED)

val Peach = Color(0xFFF2D2A9)
val PeachDeep = Color(0xFFC68E4F)
val PeachTint = Color(0xFFFBF0E0)

val Lavender = Color(0xFFD3CBE4)
val LavenderDeep = Color(0xFF8C7FAE)
val LavenderTint = Color(0xFFF1EEF7)

val ErrorRed = Color(0xFFB0413E)
val SurfaceVariantWarm = Color(0xFFF2EADF)
val OutlineVariantWarm = Color(0xFFF0E8DC)
val PresentOnContainer = Color(0xFF39532F)
```

- [ ] **Step 2: Create `YesMaamColors.kt` (status roles M3 lacks)**

```kotlin
package com.example.yesmaam.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class YesMaamColors(
    val present: Color, val presentTint: Color, val onPresent: Color,
    val absent: Color, val absentTint: Color, val onAbsent: Color,
    val late: Color, val lateTint: Color, val onLate: Color,
    val holiday: Color, val holidayTint: Color, val onHoliday: Color,
)

val LightYesMaamColors = YesMaamColors(
    present = Sage, presentTint = SageTint, onPresent = PresentOnContainer,
    absent = Blush, absentTint = BlushTint, onAbsent = Color(0xFF6F2F33),
    late = Peach, lateTint = PeachTint, onLate = Color(0xFF7A4F17),
    holiday = Peach, holidayTint = PeachTint, onHoliday = PeachDeep,
)

val LocalYesMaamColors = staticCompositionLocalOf { LightYesMaamColors }
```

- [ ] **Step 3: Create `Type.kt`**

```kotlin
package com.example.yesmaam.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.yesmaam.R

val Fraunces = FontFamily(
    Font(R.font.fraunces_regular, FontWeight.Normal),
    Font(R.font.fraunces_medium, FontWeight.Medium),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
    Font(R.font.fraunces_bold, FontWeight.Bold),
)

val Lora = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_medium, FontWeight.Medium),
    Font(R.font.lora_italic, FontWeight.Normal, FontStyle.Italic),
)

private fun fraunces(size: Int, line: Int, weight: FontWeight = FontWeight.SemiBold) =
    TextStyle(fontFamily = Fraunces, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private fun lora(size: Double, line: Int, weight: FontWeight = FontWeight.Normal, italic: Boolean = false) =
    TextStyle(
        fontFamily = Lora, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    )

val YesMaamTypography = Typography(
    displayLarge = fraunces(54, 56),
    headlineMedium = fraunces(24, 28),
    headlineSmall = fraunces(23, 26),
    titleLarge = fraunces(19, 24),
    titleMedium = fraunces(16, 20),
    bodyLarge = lora(16.0, 24),
    bodyMedium = lora(14.5, 21),
    labelLarge = lora(14.5, 18, FontWeight.Medium),
    labelMedium = lora(12.5, 16, FontWeight.Medium),
    labelSmall = lora(11.5, 15, italic = true),
)
```

- [ ] **Step 4: Create `Shape.kt`**

```kotlin
package com.example.yesmaam.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val YesMaamShapes = Shapes(
    small = RoundedCornerShape(9.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)
```

- [ ] **Step 5: Replace `Theme.kt`**

```kotlin
package com.example.yesmaam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = SageDeep, onPrimary = Color.White,
    primaryContainer = SageTint, onPrimaryContainer = PresentOnContainer,
    secondary = LavenderDeep, secondaryContainer = LavenderTint,
    tertiary = BlushDeep, tertiaryContainer = BlushTint,
    error = ErrorRed, onError = Color.White,
    background = Cream, onBackground = Ink,
    surface = Paper, onSurface = Ink,
    surfaceVariant = SurfaceVariantWarm, onSurfaceVariant = InkSoft,
    outline = Line, outlineVariant = OutlineVariantWarm,
)

@Composable
fun YesMaamTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // v1 is light-only by design (design system §2.3).
    CompositionLocalProvider(LocalYesMaamColors provides LightYesMaamColors) {
        MaterialTheme(
            colorScheme = LightScheme,
            typography = YesMaamTypography,
            shapes = YesMaamShapes,
            content = content,
        )
    }
}
```

- [ ] **Step 6: Verify the build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. (If `R.font.*` is unresolved, re-check Task 2 filenames.)

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/ui/theme
git commit -m "feat(ui): pastel serif Material 3 theme"
```

---

### Task 4: App scaffold, manual DI, navigation skeleton

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/YesMaamApp.kt`
- Create: `app/src/main/java/com/example/yesmaam/di/AppContainer.kt`
- Create: `app/src/main/java/com/example/yesmaam/di/Vm.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/nav/Routes.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/nav/NavGraph.kt`
- Modify: `app/src/main/java/com/example/yesmaam/MainActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

> `AppContainer` is created with a placeholder here and filled in Task 10 once the
> DB/repository exist. It compiles now with empty members.

- [ ] **Step 1: Create a placeholder `AppContainer`**

```kotlin
package com.example.yesmaam.di

import android.content.Context

// Filled with db/repository/settings/exporters in Task 10.
class AppContainer(@Suppress("unused") private val context: Context)
```

- [ ] **Step 2: Create the `Application`**

```kotlin
package com.example.yesmaam

import android.app.Application
import com.example.yesmaam.di.AppContainer

class YesMaamApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

- [ ] **Step 3: Create the DI helper `Vm.kt`**

```kotlin
package com.example.yesmaam.di

import android.content.Context
import com.example.yesmaam.YesMaamApp

val Context.appContainer: AppContainer
    get() = (applicationContext as YesMaamApp).container
```

- [ ] **Step 4: Create `Routes.kt`**

```kotlin
package com.example.yesmaam.ui.nav

object Routes {
    const val CLASSES = "classes"
    const val SETTINGS = "settings"
    fun classEditor(classId: Long?) = "classEditor?classId=${classId ?: -1L}"
    const val CLASS_EDITOR = "classEditor?classId={classId}"
    fun classHome(classId: Long) = "class/$classId"
    const val CLASS_HOME = "class/{classId}"
    fun studentEditor(classId: Long, studentId: Long?) =
        "studentEditor?classId=$classId&studentId=${studentId ?: -1L}"
    const val STUDENT_EDITOR = "studentEditor?classId={classId}&studentId={studentId}"
}
```

- [ ] **Step 5: Create a skeleton `NavGraph.kt` (placeholder screens)**

```kotlin
package com.example.yesmaam.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun YesMaamNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.CLASSES) {
        composable(Routes.CLASSES) { Placeholder("Classes") }
        composable(Routes.SETTINGS) { Placeholder("Settings") }
    }
}

@Composable
private fun Placeholder(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(label) }
}
```

- [ ] **Step 6: Replace `MainActivity.kt`**

```kotlin
package com.example.yesmaam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.yesmaam.ui.nav.YesMaamNavGraph
import com.example.yesmaam.ui.theme.YesMaamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YesMaamTheme {
                Surface(Modifier.fillMaxSize()) { YesMaamNavGraph() }
            }
        }
    }
}
```

- [ ] **Step 7: Register the Application in the manifest**

In `app/src/main/AndroidManifest.xml`, add `android:name=".YesMaamApp"` to the
`<application>` tag (keep all existing attributes):

```xml
    <application
        android:name=".YesMaamApp"
        ... existing attributes ... >
```

- [ ] **Step 8: Build and run**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Installing on a device shows a screen reading "Classes".

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/example/yesmaam app/src/main/AndroidManifest.xml
git commit -m "feat: app scaffold, manual DI, nav skeleton"
```

---

## Phase 2 — Data layer

### Task 5: Domain models, entities, converters

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/domain/model/Models.kt`
- Create: `app/src/main/java/com/example/yesmaam/data/db/Converters.kt`
- Create: `app/src/main/java/com/example/yesmaam/data/db/Entities.kt`

- [ ] **Step 1: Create `Models.kt`**

```kotlin
package com.example.yesmaam.domain.model

import java.time.LocalDate

enum class AttendanceStatus { PRESENT, ABSENT, LATE }

data class ClassInfo(
    val id: Long,
    val name: String,
    val note: String?,
    val emoji: String,
    val colorKey: String,
)

data class StudentRef(
    val id: Long,
    val name: String,
    val rollNumber: String,
    val guardianPhone: String?,
)

data class AttendanceRecord(
    val studentId: Long,
    val date: LocalDate,
    val status: AttendanceStatus,
)
```

- [ ] **Step 2: Create `Converters.kt`**

```kotlin
package com.example.yesmaam.data.db

import androidx.room.TypeConverter
import com.example.yesmaam.domain.model.AttendanceStatus

class Converters {
    @TypeConverter fun statusToString(s: AttendanceStatus): String = s.name
    @TypeConverter fun stringToStatus(s: String): AttendanceStatus = AttendanceStatus.valueOf(s)
}
```

- [ ] **Step 3: Create `Entities.kt`**

```kotlin
package com.example.yesmaam.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.yesmaam.domain.model.AttendanceStatus

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val note: String? = null,
    val emoji: String,
    val colorKey: String,
    val createdAt: Long,
    val archived: Boolean = false,
)

@Entity(
    tableName = "students",
    foreignKeys = [ForeignKey(
        entity = ClassEntity::class, parentColumns = ["id"], childColumns = ["classId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("classId")],
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val name: String,
    val rollNumber: String,
    val guardianPhone: String? = null,
    val createdAt: Long,
)

@Entity(
    tableName = "attendance",
    foreignKeys = [ForeignKey(
        entity = StudentEntity::class, parentColumns = ["id"], childColumns = ["studentId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["studentId", "date"], unique = true)],
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val date: Long, // epoch day
    val status: AttendanceStatus,
)

@Entity(
    tableName = "holidays",
    foreignKeys = [ForeignKey(
        entity = ClassEntity::class, parentColumns = ["id"], childColumns = ["classId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["classId", "date"], unique = true)],
)
data class HolidayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val date: Long, // epoch day
    val note: String? = null,
)
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/domain app/src/main/java/com/example/yesmaam/data/db
git commit -m "feat(data): domain models, Room entities, converters"
```

---

### Task 6: DAOs

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/data/db/ClassDao.kt`
- Create: `app/src/main/java/com/example/yesmaam/data/db/StudentDao.kt`
- Create: `app/src/main/java/com/example/yesmaam/data/db/AttendanceDao.kt`
- Create: `app/src/main/java/com/example/yesmaam/data/db/HolidayDao.kt`

- [ ] **Step 1: Create `ClassDao.kt`**

```kotlin
package com.example.yesmaam.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes WHERE archived = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes WHERE id = :id")
    fun observe(id: Long): Flow<ClassEntity?>

    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun get(id: Long): ClassEntity?

    @Insert suspend fun insert(c: ClassEntity): Long
    @Update suspend fun update(c: ClassEntity)
    @Delete suspend fun delete(c: ClassEntity)
}
```

- [ ] **Step 2: Create `StudentDao.kt`**

```kotlin
package com.example.yesmaam.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY CAST(rollNumber AS INTEGER), name")
    fun observeByClass(classId: Long): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY CAST(rollNumber AS INTEGER), name")
    suspend fun getByClass(classId: Long): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun get(id: Long): StudentEntity?

    @Insert suspend fun insert(s: StudentEntity): Long
    @Update suspend fun update(s: StudentEntity)
    @Delete suspend fun delete(s: StudentEntity)
}
```

- [ ] **Step 3: Create `AttendanceDao.kt`**

```kotlin
package com.example.yesmaam.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<AttendanceEntity>)

    /** All attendance for a class within an (inclusive) epoch-day range, via the student join. */
    @Query(
        """
        SELECT a.* FROM attendance a
        INNER JOIN students s ON s.id = a.studentId
        WHERE s.classId = :classId AND a.date BETWEEN :startDay AND :endDay
        """
    )
    fun observeForClassRange(classId: Long, startDay: Long, endDay: Long): Flow<List<AttendanceEntity>>

    @Query(
        """
        SELECT a.* FROM attendance a
        INNER JOIN students s ON s.id = a.studentId
        WHERE s.classId = :classId AND a.date BETWEEN :startDay AND :endDay
        """
    )
    suspend fun getForClassRange(classId: Long, startDay: Long, endDay: Long): List<AttendanceEntity>

    @Query(
        """
        SELECT a.* FROM attendance a
        INNER JOIN students s ON s.id = a.studentId
        WHERE s.classId = :classId AND a.date = :day
        """
    )
    fun observeForClassDay(classId: Long, day: Long): Flow<List<AttendanceEntity>>
}
```

- [ ] **Step 4: Create `HolidayDao.kt`**

```kotlin
package com.example.yesmaam.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(h: HolidayEntity)

    @Query("DELETE FROM holidays WHERE classId = :classId AND date = :day")
    suspend fun remove(classId: Long, day: Long)

    @Query("SELECT * FROM holidays WHERE classId = :classId AND date BETWEEN :startDay AND :endDay")
    fun observeForClassRange(classId: Long, startDay: Long, endDay: Long): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays WHERE classId = :classId AND date BETWEEN :startDay AND :endDay")
    suspend fun getForClassRange(classId: Long, startDay: Long, endDay: Long): List<HolidayEntity>

    @Query("SELECT COUNT(*) FROM holidays WHERE classId = :classId AND date = :day")
    fun observeIsHoliday(classId: Long, day: Long): Flow<Int>
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/data/db
git commit -m "feat(data): DAOs for class, student, attendance, holiday"
```

---

### Task 7: AppDatabase

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/data/db/AppDatabase.kt`

- [ ] **Step 1: Create `AppDatabase.kt`**

```kotlin
package com.example.yesmaam.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ClassEntity::class, StudentEntity::class, AttendanceEntity::class, HolidayEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun classDao(): ClassDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun holidayDao(): HolidayDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "yesmaam.db").build()
    }
}
```

- [ ] **Step 2: Verify Room codegen compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (KSP generates DAO/db impls). Foreign-key/index warnings are acceptable; errors are not.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/data/db/AppDatabase.kt
git commit -m "feat(data): Room AppDatabase"
```

---

### Task 8: Repository

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/data/AttendanceRepository.kt`

> Epoch-day conversion is centralised here: `LocalDate.toEpochDay()` and
> `LocalDate.ofEpochDay()`. The repository speaks `LocalDate`; the DB stores `Long`.

- [ ] **Step 1: Create `AttendanceRepository.kt`**

```kotlin
package com.example.yesmaam.data

import com.example.yesmaam.data.db.AppDatabase
import com.example.yesmaam.data.db.AttendanceEntity
import com.example.yesmaam.data.db.ClassEntity
import com.example.yesmaam.data.db.HolidayEntity
import com.example.yesmaam.data.db.StudentEntity
import com.example.yesmaam.domain.model.AttendanceStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

class AttendanceRepository(private val db: AppDatabase) {

    // ----- classes -----
    fun observeClasses(): Flow<List<ClassEntity>> = db.classDao().observeAll()
    fun observeClass(id: Long): Flow<ClassEntity?> = db.classDao().observe(id)
    suspend fun getClass(id: Long): ClassEntity? = db.classDao().get(id)
    suspend fun createClass(c: ClassEntity): Long = db.classDao().insert(c)
    suspend fun updateClass(c: ClassEntity) = db.classDao().update(c)
    suspend fun deleteClass(c: ClassEntity) = db.classDao().delete(c)

    // ----- students -----
    fun observeStudents(classId: Long): Flow<List<StudentEntity>> =
        db.studentDao().observeByClass(classId)
    suspend fun getStudents(classId: Long): List<StudentEntity> =
        db.studentDao().getByClass(classId)
    suspend fun getStudent(id: Long): StudentEntity? = db.studentDao().get(id)
    suspend fun addStudent(s: StudentEntity): Long = db.studentDao().insert(s)
    suspend fun updateStudent(s: StudentEntity) = db.studentDao().update(s)
    suspend fun deleteStudent(s: StudentEntity) = db.studentDao().delete(s)

    // ----- attendance -----
    fun observeDay(classId: Long, date: LocalDate): Flow<List<AttendanceEntity>> =
        db.attendanceDao().observeForClassDay(classId, date.toEpochDay())

    fun observeMonth(classId: Long, month: YearMonth): Flow<List<AttendanceEntity>> {
        val (start, end) = month.range()
        return db.attendanceDao().observeForClassRange(classId, start, end)
    }

    suspend fun getMonthAttendance(classId: Long, month: YearMonth): List<AttendanceEntity> {
        val (start, end) = month.range()
        return db.attendanceDao().getForClassRange(classId, start, end)
    }

    /** Persist the whole day's statuses for a class (creates/updates the session). */
    suspend fun saveDay(date: LocalDate, statuses: Map<Long, AttendanceStatus>) {
        val day = date.toEpochDay()
        db.attendanceDao().upsertAll(
            statuses.map { (studentId, status) -> AttendanceEntity(studentId = studentId, date = day, status = status) }
        )
    }

    // ----- holidays -----
    fun observeHolidays(classId: Long, month: YearMonth): Flow<List<HolidayEntity>> {
        val (start, end) = month.range()
        return db.holidayDao().observeForClassRange(classId, start, end)
    }

    suspend fun getHolidays(classId: Long, month: YearMonth): List<HolidayEntity> {
        val (start, end) = month.range()
        return db.holidayDao().getForClassRange(classId, start, end)
    }

    fun observeIsHoliday(classId: Long, date: LocalDate): Flow<Int> =
        db.holidayDao().observeIsHoliday(classId, date.toEpochDay())

    suspend fun markHoliday(classId: Long, date: LocalDate, note: String? = null) =
        db.holidayDao().insert(HolidayEntity(classId = classId, date = date.toEpochDay(), note = note))

    suspend fun removeHoliday(classId: Long, date: LocalDate) =
        db.holidayDao().remove(classId, date.toEpochDay())

    private fun YearMonth.range(): Pair<Long, Long> =
        atDay(1).toEpochDay() to atEndOfMonth().toEpochDay()
}
```

- [ ] **Step 2: Verify the build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/data/AttendanceRepository.kt
git commit -m "feat(data): AttendanceRepository"
```

---

### Task 9: SettingsStore

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/data/SettingsStore.kt`

- [ ] **Step 1: Create `SettingsStore.kt`**

```kotlin
package com.example.yesmaam.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class TeacherSettings(val teacherName: String = "", val institution: String = "")

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("yesmaam_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(
        TeacherSettings(
            teacherName = prefs.getString(KEY_NAME, "") ?: "",
            institution = prefs.getString(KEY_INSTITUTION, "") ?: "",
        )
    )
    val settings: StateFlow<TeacherSettings> = _settings

    fun update(name: String, institution: String) {
        prefs.edit().putString(KEY_NAME, name).putString(KEY_INSTITUTION, institution).apply()
        _settings.update { it.copy(teacherName = name, institution = institution) }
    }

    companion object {
        private const val KEY_NAME = "teacher_name"
        private const val KEY_INSTITUTION = "institution"
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/data/SettingsStore.kt
git commit -m "feat(data): SettingsStore"
```

---

### Task 10: Wire up AppContainer

**Files:**
- Modify: `app/src/main/java/com/example/yesmaam/di/AppContainer.kt`
- Modify: `app/src/main/java/com/example/yesmaam/di/Vm.kt`

- [ ] **Step 1: Fill in `AppContainer.kt`**

```kotlin
package com.example.yesmaam.di

import android.content.Context
import com.example.yesmaam.data.AttendanceRepository
import com.example.yesmaam.data.SettingsStore
import com.example.yesmaam.data.db.AppDatabase

class AppContainer(context: Context) {
    private val db = AppDatabase.build(context)
    val repository = AttendanceRepository(db)
    val settings = SettingsStore(context)
}
```

- [ ] **Step 2: Confirm `Vm.kt` exposes only the `appContainer` accessor**

`Vm.kt` stays exactly as created in Task 4 (the `Context.appContainer` extension).
Screens build their ViewModels with an inline factory, e.g.:

```kotlin
val container = LocalContext.current.appContainer
val vm: TodayViewModel = viewModel(
    factory = viewModelFactory { initializer { TodayViewModel(container, classId, date) } }
)
```

with imports `androidx.lifecycle.viewmodel.compose.viewModel`,
`androidx.lifecycle.viewmodel.initializer`, `androidx.lifecycle.viewmodel.viewModelFactory`.
This pattern is repeated verbatim in every screen task below.

- [ ] **Step 3: Verify the build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/di
git commit -m "feat(di): wire AppContainer with db, repository, settings"
```

---

## Phase 3 — Report core (pure, TDD)

### Task 11: MonthlyReport model + ReportBuilder

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/domain/report/MonthlyReport.kt`
- Create: `app/src/main/java/com/example/yesmaam/domain/report/ReportBuilder.kt`
- Test: `app/src/test/java/com/example/yesmaam/domain/report/ReportBuilderTest.kt`

> This is the correctness heart of the app. It has no Android imports and is
> tested on the JVM. Rules implemented (from spec §5): a **session** is a
> non-holiday date with ≥1 attendance record; **percentage = (present + late) /
> sessionCount**; a student with **no record on a session date counts as absent**;
> **column dates = sorted(sessions ∪ holidays)**.

- [ ] **Step 1: Create the model `MonthlyReport.kt`**

```kotlin
package com.example.yesmaam.domain.report

import com.example.yesmaam.domain.model.AttendanceRecord
import com.example.yesmaam.domain.model.AttendanceStatus
import com.example.yesmaam.domain.model.ClassInfo
import com.example.yesmaam.domain.model.StudentRef
import java.time.LocalDate
import java.time.YearMonth

data class ReportInputs(
    val classInfo: ClassInfo,
    val students: List<StudentRef>,
    val attendance: List<AttendanceRecord>,
    val holidays: List<LocalDate>,
    val month: YearMonth,
    val teacherName: String? = null,
    val institution: String? = null,
)

data class StudentReportRow(
    val student: StudentRef,
    val statusByDate: Map<LocalDate, AttendanceStatus>,
    val presentCount: Int,
    val lateCount: Int,
    val absentCount: Int,
    val attendedCount: Int,
    val percentage: Int,
)

data class ReportSummary(
    val studentCount: Int,
    val sessionCount: Int,
    val holidayCount: Int,
    val averagePercentage: Int,
)

data class MonthlyReport(
    val classInfo: ClassInfo,
    val month: YearMonth,
    val teacherName: String?,
    val institution: String?,
    val sessionDates: List<LocalDate>,
    val holidayDates: List<LocalDate>,
    val columnDates: List<LocalDate>,
    val rows: List<StudentReportRow>,
    val summary: ReportSummary,
)
```

- [ ] **Step 2: Write the failing test `ReportBuilderTest.kt`**

```kotlin
package com.example.yesmaam.domain.report

import com.example.yesmaam.domain.model.AttendanceRecord
import com.example.yesmaam.domain.model.AttendanceStatus.ABSENT
import com.example.yesmaam.domain.model.AttendanceStatus.LATE
import com.example.yesmaam.domain.model.AttendanceStatus.PRESENT
import com.example.yesmaam.domain.model.ClassInfo
import com.example.yesmaam.domain.model.StudentRef
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ReportBuilderTest {
    private val cls = ClassInfo(1, "VI-A", null, "📕", "sage")
    private val ann = StudentRef(1, "Ann", "01", null)
    private val bob = StudentRef(2, "Bob", "02", null)
    private fun d(day: Int) = LocalDate.of(2026, 6, day)

    private fun inputs(
        students: List<StudentRef>,
        attendance: List<AttendanceRecord>,
        holidays: List<LocalDate> = emptyList(),
    ) = ReportInputs(cls, students, attendance, holidays, YearMonth.of(2026, 6))

    @Test fun `sessions are dates with records excluding holidays`() {
        val att = listOf(
            AttendanceRecord(1, d(3), PRESENT),
            AttendanceRecord(1, d(4), PRESENT),
            AttendanceRecord(1, d(5), PRESENT), // 5th is a holiday -> not a session
        )
        val report = buildMonthlyReport(inputs(listOf(ann), att, holidays = listOf(d(5))))
        assertEquals(listOf(d(3), d(4)), report.sessionDates)
        assertEquals(listOf(d(5)), report.holidayDates)
        assertEquals(listOf(d(3), d(4), d(5)), report.columnDates)
        assertEquals(2, report.summary.sessionCount)
        assertEquals(1, report.summary.holidayCount)
    }

    @Test fun `percentage counts present and late over sessions`() {
        val att = listOf(
            AttendanceRecord(1, d(1), PRESENT),
            AttendanceRecord(1, d(2), LATE),
            AttendanceRecord(1, d(3), ABSENT),
            AttendanceRecord(1, d(4), PRESENT),
        )
        val report = buildMonthlyReport(inputs(listOf(ann), att))
        val row = report.rows.single()
        assertEquals(2, row.presentCount)
        assertEquals(1, row.lateCount)
        assertEquals(1, row.absentCount)
        assertEquals(3, row.attendedCount)
        assertEquals(75, row.percentage) // 3 of 4
    }

    @Test fun `missing record on a session date counts as absent`() {
        // Bob has no record on the 1st, which is a session because Ann was marked.
        val att = listOf(
            AttendanceRecord(1, d(1), PRESENT),
            AttendanceRecord(2, d(2), PRESENT),
        )
        val report = buildMonthlyReport(inputs(listOf(ann, bob), att))
        assertEquals(listOf(d(1), d(2)), report.sessionDates)
        val bobRow = report.rows.single { it.student.id == 2L }
        assertEquals(1, bobRow.presentCount)
        assertEquals(1, bobRow.absentCount) // missing on the 1st
        assertEquals(50, bobRow.percentage)
    }

    @Test fun `empty month yields zero percentages`() {
        val report = buildMonthlyReport(inputs(listOf(ann), emptyList()))
        assertEquals(0, report.summary.sessionCount)
        assertEquals(0, report.rows.single().percentage)
        assertEquals(0, report.summary.averagePercentage)
    }

    @Test fun `average percentage is the mean of student percentages`() {
        val att = listOf(
            AttendanceRecord(1, d(1), PRESENT), AttendanceRecord(1, d(2), PRESENT), // Ann 100
            AttendanceRecord(2, d(1), PRESENT), AttendanceRecord(2, d(2), ABSENT),  // Bob 50
        )
        val report = buildMonthlyReport(inputs(listOf(ann, bob), att))
        assertEquals(75, report.summary.averagePercentage)
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.yesmaam.domain.report.ReportBuilderTest"`
Expected: FAIL — `buildMonthlyReport` unresolved.

- [ ] **Step 4: Implement `ReportBuilder.kt`**

```kotlin
package com.example.yesmaam.domain.report

import com.example.yesmaam.domain.model.AttendanceStatus
import java.time.LocalDate
import kotlin.math.roundToInt

fun buildMonthlyReport(inputs: ReportInputs): MonthlyReport {
    val month = inputs.month
    fun inMonth(date: LocalDate) = YearMonthMatches(date, month)

    val holidaySet = inputs.holidays.filter { inMonth(it) }.toSet()

    val recordedDates = inputs.attendance.map { it.date }.filter { inMonth(it) }.toSet()
    val sessionDates = (recordedDates - holidaySet).sorted()
    val holidayDates = holidaySet.sorted()
    val columnDates = (sessionDates + holidayDates).distinct().sorted()

    val byStudent = inputs.attendance.groupBy { it.studentId }

    val rows = inputs.students.map { student ->
        val statusByDate = byStudent[student.id].orEmpty()
            .filter { it.date in sessionDates }
            .associate { it.date to it.status }
        var present = 0; var late = 0; var absent = 0
        for (date in sessionDates) {
            when (statusByDate[date]) {
                AttendanceStatus.PRESENT -> present++
                AttendanceStatus.LATE -> late++
                AttendanceStatus.ABSENT, null -> absent++
            }
        }
        val attended = present + late
        val pct = if (sessionDates.isEmpty()) 0 else (attended * 100.0 / sessionDates.size).roundToInt()
        StudentReportRow(student, statusByDate, present, late, absent, attended, pct)
    }

    val avg = if (rows.isEmpty()) 0 else (rows.sumOf { it.percentage }.toDouble() / rows.size).roundToInt()

    return MonthlyReport(
        classInfo = inputs.classInfo,
        month = month,
        teacherName = inputs.teacherName,
        institution = inputs.institution,
        sessionDates = sessionDates,
        holidayDates = holidayDates,
        columnDates = columnDates,
        rows = rows,
        summary = ReportSummary(
            studentCount = inputs.students.size,
            sessionCount = sessionDates.size,
            holidayCount = holidayDates.size,
            averagePercentage = avg,
        ),
    )
}

private fun YearMonthMatches(date: LocalDate, month: java.time.YearMonth): Boolean =
    date.year == month.year && date.monthValue == month.monthValue
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.yesmaam.domain.report.ReportBuilderTest"`
Expected: PASS (5 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/domain/report app/src/test/java/com/example/yesmaam/domain/report
git commit -m "feat(report): pure MonthlyReport builder with tests"
```

---

### Task 12: XlsxWriter (hand-rolled OOXML, TDD)

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/export/ooxml/XlsxWriter.kt`
- Test: `app/src/test/java/com/example/yesmaam/export/ooxml/XlsxWriterTest.kt`

> Produces a valid single-sheet `.xlsx` (a ZIP of OOXML parts) using only
> `java.util.zip`. Style ids: `0` default, `1` header (bold + cream fill),
> `2` present fill, `3` absent fill, `4` late/holiday fill, `5` bold totals.

- [ ] **Step 1: Write the failing test `XlsxWriterTest.kt`**

```kotlin
package com.example.yesmaam.export.ooxml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class XlsxWriterTest {
    private fun entries(bytes: ByteArray): Map<String, String> {
        val out = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                out[e.name] = zis.readBytes().toString(Charsets.UTF_8)
                e = zis.nextEntry
            }
        }
        return out
    }

    @Test fun `writes the required ooxml parts`() {
        val sheet = XlsxSheet("Report", listOf(listOf(XlsxCell.Text("Name", 1), XlsxCell.Number(96.0))))
        val bos = ByteArrayOutputStream()
        XlsxWriter.write(sheet, bos)
        val parts = entries(bos.toByteArray())
        assertTrue("[Content_Types].xml" in parts)
        assertTrue("_rels/.rels" in parts)
        assertTrue("xl/workbook.xml" in parts)
        assertTrue("xl/_rels/workbook.xml.rels" in parts)
        assertTrue("xl/styles.xml" in parts)
        assertTrue("xl/worksheets/sheet1.xml" in parts)
    }

    @Test fun `sheet contains cell values and styles`() {
        val sheet = XlsxSheet("Report", listOf(listOf(XlsxCell.Text("Ann & co", 1), XlsxCell.Number(75.0, 5))))
        val bos = ByteArrayOutputStream()
        XlsxWriter.write(sheet, bos)
        val xml = entries(bos.toByteArray()).getValue("xl/worksheets/sheet1.xml")
        assertTrue(xml.contains("<c r=\"A1\" t=\"inlineStr\" s=\"1\">"))
        assertTrue(xml.contains("<t>Ann &amp; co</t>"))      // escaped
        assertTrue(xml.contains("<c r=\"B1\" s=\"5\"><v>75</v></c>"))
    }

    @Test fun `column names roll over past Z`() {
        assertEquals("A", colName(0))
        assertEquals("Z", colName(25))
        assertEquals("AA", colName(26))
        assertEquals("AB", colName(27))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.yesmaam.export.ooxml.XlsxWriterTest"`
Expected: FAIL — `XlsxSheet`/`XlsxCell`/`XlsxWriter`/`colName` unresolved.

- [ ] **Step 3: Implement `XlsxWriter.kt`**

```kotlin
package com.example.yesmaam.export.ooxml

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

sealed interface XlsxCell {
    data class Text(val value: String, val styleId: Int = 0) : XlsxCell
    data class Number(val value: Double, val styleId: Int = 0) : XlsxCell
}

class XlsxSheet(val name: String, val rows: List<List<XlsxCell>>)

/** Excel column letters: 0 -> A, 25 -> Z, 26 -> AA. */
fun colName(index: Int): String {
    var i = index
    val sb = StringBuilder()
    while (i >= 0) {
        sb.insert(0, ('A' + (i % 26)))
        i = i / 26 - 1
    }
    return sb.toString()
}

private fun String.xmlEscape(): String = buildString {
    for (c in this@xmlEscape) when (c) {
        '&' -> append("&amp;"); '<' -> append("&lt;"); '>' -> append("&gt;")
        '"' -> append("&quot;"); '\'' -> append("&apos;"); else -> append(c)
    }
}

object XlsxWriter {
    fun write(sheet: XlsxSheet, out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            zip.put("[Content_Types].xml", CONTENT_TYPES)
            zip.put("_rels/.rels", ROOT_RELS)
            zip.put("xl/workbook.xml", workbook(sheet.name))
            zip.put("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            zip.put("xl/styles.xml", STYLES)
            zip.put("xl/worksheets/sheet1.xml", sheetXml(sheet))
        }
    }

    private fun ZipOutputStream.put(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun sheetXml(sheet: XlsxSheet): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        sheet.rows.forEachIndexed { r, row ->
            append("""<row r="${r + 1}">""")
            row.forEachIndexed { c, cell ->
                val ref = "${colName(c)}${r + 1}"
                when (cell) {
                    is XlsxCell.Text ->
                        append("""<c r="$ref" t="inlineStr" s="${cell.styleId}"><is><t>${cell.value.xmlEscape()}</t></is></c>""")
                    is XlsxCell.Number -> {
                        val v = if (cell.value == cell.value.toLong().toDouble()) cell.value.toLong().toString() else cell.value.toString()
                        append("""<c r="$ref" s="${cell.styleId}"><v>$v</v></c>""")
                    }
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    private fun workbook(sheetName: String) =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """ +
        """xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""" +
        """<sheets><sheet name="${sheetName.xmlEscape()}" sheetId="1" r:id="rId1"/></sheets></workbook>"""

    private const val CONTENT_TYPES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
        """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
        """<Default Extension="xml" ContentType="application/xml"/>""" +
        """<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
        """<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" +
        """<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""" +
        """</Types>"""

    private const val ROOT_RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
        """</Relationships>"""

    private const val WORKBOOK_RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>""" +
        """<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""" +
        """</Relationships>"""

    private const val STYLES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
        """<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>""" +
        """<fills count="6">""" +
        """<fill><patternFill patternType="none"/></fill>""" +
        """<fill><patternFill patternType="gray125"/></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="FFEAF1E7"/></patternFill></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="FFFBEDED"/></patternFill></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="FFFBF0E0"/></patternFill></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="FFFAF6EF"/></patternFill></fill>""" +
        """</fills>""" +
        """<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""" +
        """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
        """<cellXfs count="6">""" +
        """<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
        """<xf numFmtId="0" fontId="1" fillId="5" borderId="0" xfId="0" applyFont="1" applyFill="1"/>""" +
        """<xf numFmtId="0" fontId="0" fillId="2" borderId="0" xfId="0" applyFill="1"/>""" +
        """<xf numFmtId="0" fontId="0" fillId="3" borderId="0" xfId="0" applyFill="1"/>""" +
        """<xf numFmtId="0" fontId="0" fillId="4" borderId="0" xfId="0" applyFill="1"/>""" +
        """<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>""" +
        """</cellXfs></styleSheet>"""
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.yesmaam.export.ooxml.XlsxWriterTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Manually confirm the file opens (one-time sanity check)**

Write a tiny throwaway main or reuse the test to dump bytes to `/tmp/t.xlsx`, then
open it in Numbers/Excel/Google Sheets. Expected: a one-row sheet with "Name" bold
and 96. (No code committed for this step.)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/export/ooxml app/src/test/java/com/example/yesmaam/export/ooxml
git commit -m "feat(export): hand-rolled XLSX writer with tests"
```

---

## Phase 4 — Export subsystem

### Task 13: ReportExporter interface + Excel exporter

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/export/ExportFormat.kt`
- Create: `app/src/main/java/com/example/yesmaam/export/ReportExporter.kt`
- Create: `app/src/main/java/com/example/yesmaam/export/XlsxReportExporter.kt`
- Test: `app/src/test/java/com/example/yesmaam/export/XlsxReportExporterTest.kt`

- [ ] **Step 1: Create `ExportFormat.kt`**

```kotlin
package com.example.yesmaam.export

enum class ExportFormat(val label: String, val mime: String, val ext: String) {
    EXCEL("Excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    PDF("PDF", "application/pdf", "pdf"),
}
```

- [ ] **Step 2: Create `ReportExporter.kt`**

```kotlin
package com.example.yesmaam.export

import com.example.yesmaam.domain.report.MonthlyReport
import java.io.OutputStream

interface ReportExporter {
    val format: ExportFormat
    fun write(report: MonthlyReport, out: OutputStream)
}
```

- [ ] **Step 3: Write the failing test `XlsxReportExporterTest.kt`**

```kotlin
package com.example.yesmaam.export

import com.example.yesmaam.domain.model.AttendanceRecord
import com.example.yesmaam.domain.model.AttendanceStatus.PRESENT
import com.example.yesmaam.domain.model.ClassInfo
import com.example.yesmaam.domain.model.StudentRef
import com.example.yesmaam.domain.report.ReportInputs
import com.example.yesmaam.domain.report.buildMonthlyReport
import com.example.yesmaam.export.ooxml.XlsxCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class XlsxReportExporterTest {
    @Test fun `sheet has header and a holiday column shows H`() {
        val ann = StudentRef(1, "Ann", "01", null)
        val report = buildMonthlyReport(
            ReportInputs(
                classInfo = ClassInfo(1, "VI-A", null, "📕", "sage"),
                students = listOf(ann),
                attendance = listOf(
                    AttendanceRecord(1, LocalDate.of(2026, 6, 3), PRESENT),
                    AttendanceRecord(1, LocalDate.of(2026, 6, 4), PRESENT),
                ),
                holidays = listOf(LocalDate.of(2026, 6, 5)),
                month = YearMonth.of(2026, 6),
            )
        )
        val sheet = XlsxReportExporter().buildSheet(report)
        val header = sheet.rows.first().filterIsInstance<XlsxCell.Text>().map { it.value }
        assertTrue("Name" in header)
        assertTrue("%" in header)
        // body row: holiday column (5) renders "H"
        val body = sheet.rows[1].filterIsInstance<XlsxCell.Text>().map { it.value }
        assertTrue("H" in body)
        assertTrue("P" in body)
        assertEquals("Ann", body.first())
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.yesmaam.export.XlsxReportExporterTest"`
Expected: FAIL — `XlsxReportExporter` unresolved.

- [ ] **Step 5: Implement `XlsxReportExporter.kt`**

```kotlin
package com.example.yesmaam.export

import com.example.yesmaam.domain.model.AttendanceStatus
import com.example.yesmaam.domain.report.MonthlyReport
import com.example.yesmaam.export.ooxml.XlsxCell
import com.example.yesmaam.export.ooxml.XlsxSheet
import com.example.yesmaam.export.ooxml.XlsxWriter
import java.io.OutputStream

class XlsxReportExporter : ReportExporter {
    override val format = ExportFormat.EXCEL

    // Style ids from XlsxWriter: 1 header, 2 present, 3 absent, 4 late/holiday, 5 bold totals.
    fun buildSheet(report: MonthlyReport): XlsxSheet {
        val holidaySet = report.holidayDates.toSet()
        val header = buildList {
            add(XlsxCell.Text("Name", 1)); add(XlsxCell.Text("Roll", 1))
            report.columnDates.forEach { add(XlsxCell.Text(it.dayOfMonth.toString(), 1)) }
            add(XlsxCell.Text("P", 1)); add(XlsxCell.Text("A", 1))
            add(XlsxCell.Text("L", 1)); add(XlsxCell.Text("%", 1))
        }
        val body = report.rows.map { row ->
            buildList<XlsxCell> {
                add(XlsxCell.Text(row.student.name)); add(XlsxCell.Text(row.student.rollNumber))
                for (date in report.columnDates) {
                    if (date in holidaySet) { add(XlsxCell.Text("H", 4)); continue }
                    when (row.statusByDate[date]) {
                        AttendanceStatus.PRESENT -> add(XlsxCell.Text("P", 2))
                        AttendanceStatus.LATE -> add(XlsxCell.Text("L", 4))
                        AttendanceStatus.ABSENT, null -> add(XlsxCell.Text("A", 3))
                    }
                }
                add(XlsxCell.Number(row.presentCount.toDouble(), 5))
                add(XlsxCell.Number(row.absentCount.toDouble(), 5))
                add(XlsxCell.Number(row.lateCount.toDouble(), 5))
                add(XlsxCell.Number(row.percentage.toDouble(), 5))
            }
        }
        return XlsxSheet(report.classInfo.name.take(31), listOf(header) + body)
    }

    override fun write(report: MonthlyReport, out: OutputStream) =
        XlsxWriter.write(buildSheet(report), out)
}
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.yesmaam.export.XlsxReportExporterTest"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/export app/src/test/java/com/example/yesmaam/export
git commit -m "feat(export): ReportExporter interface + Excel exporter"
```

---

### Task 14: PDF exporter

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/export/PdfReportExporter.kt`
- Test: `app/src/androidTest/java/com/example/yesmaam/export/PdfReportExporterTest.kt`

> PDF needs Android (`PdfDocument`, `Canvas`, fonts), so it takes a `Context` and
> is verified with an instrumented test. Landscape A4 @72dpi = 842×595 pt; rows
> paginate vertically; columns are fixed-width.

- [ ] **Step 1: Implement `PdfReportExporter.kt`**

```kotlin
package com.example.yesmaam.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.example.yesmaam.R
import com.example.yesmaam.domain.model.AttendanceStatus
import com.example.yesmaam.domain.report.MonthlyReport
import java.io.OutputStream
import java.time.format.DateTimeFormatter

class PdfReportExporter(private val context: Context) : ReportExporter {
    override val format = ExportFormat.PDF

    private val pageW = 842; private val pageH = 595
    private val margin = 36f
    private val rowH = 20f
    private val nameW = 150f; private val rollW = 40f; private val dayW = 22f; private val totW = 34f

    private val fraunces = ResourcesCompat.getFont(context, R.font.fraunces_semibold)
    private val lora = ResourcesCompat.getFont(context, R.font.lora_regular)

    override fun write(report: MonthlyReport, out: OutputStream) {
        val doc = PdfDocument()
        val title = Paint().apply { typeface = fraunces; textSize = 16f; color = 0xFF3E3A35.toInt() }
        val meta = Paint().apply { typeface = lora; textSize = 9f; color = 0xFF9A9086.toInt() }
        val head = Paint().apply { typeface = fraunces; textSize = 8f; color = 0xFF3E3A35.toInt(); textAlign = Paint.Align.CENTER }
        val cell = Paint().apply { typeface = lora; textSize = 8f; color = 0xFF3E3A35.toInt(); textAlign = Paint.Align.CENTER }
        val left = Paint().apply { typeface = lora; textSize = 8f; color = 0xFF3E3A35.toInt() }
        val line = Paint().apply { color = 0xFFECE3D6.toInt(); strokeWidth = 0.5f }

        val rowsPerPage = ((pageH - margin * 2 - 60) / rowH).toInt()
        val pages = report.rows.chunked(rowsPerPage).ifEmpty { listOf(emptyList()) }
        val holidaySet = report.holidayDates.toSet()
        val monthLabel = report.month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

        pages.forEachIndexed { pageIndex, pageRows ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageIndex + 1).create())
            val c = page.canvas
            var y = margin + 14f
            c.drawText("Attendance Register", margin, y, title)
            y += 14f
            val sub = buildString {
                append(report.classInfo.name); append(" · ").append(monthLabel)
                report.teacherName?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
            }
            c.drawText(sub, margin, y, meta)
            y += 16f

            // header row
            var x = margin
            c.drawText("Name", x + 2f, y, left.alignLeftHead())
            x += nameW; c.drawText("Roll", x + rollW / 2, y, head)
            x += rollW
            for (date in report.columnDates) { c.drawText(date.dayOfMonth.toString(), x + dayW / 2, y, head); x += dayW }
            c.drawText("P", x + totW / 2, y, head); x += totW
            c.drawText("%", x + totW / 2, y, head)
            y += 6f
            c.drawLine(margin, y, x + totW, y, line)
            y += 12f

            // body
            for (row in pageRows) {
                x = margin
                c.drawText(row.student.name.take(24), x + 2f, y, left)
                x += nameW; c.drawText(row.student.rollNumber, x + rollW / 2, y, cell)
                x += rollW
                for (date in report.columnDates) {
                    val (letter, color) = when {
                        date in holidaySet -> "H" to 0xFFC68E4F.toInt()
                        row.statusByDate[date] == AttendanceStatus.PRESENT -> "P" to 0xFF6E8C66.toInt()
                        row.statusByDate[date] == AttendanceStatus.LATE -> "L" to 0xFFC68E4F.toInt()
                        else -> "A" to 0xFFB9777B.toInt()
                    }
                    cell.color = color; c.drawText(letter, x + dayW / 2, y, cell); cell.color = 0xFF3E3A35.toInt()
                    x += dayW
                }
                c.drawText(row.attendedCount.toString(), x + totW / 2, y, cell); x += totW
                c.drawText("${row.percentage}", x + totW / 2, y, cell)
                y += rowH
            }
            doc.finishPage(page)
        }
        doc.writeTo(out)
        doc.close()
    }

    private fun Paint.alignLeftHead(): Paint =
        Paint(this).apply { typeface = fraunces; textSize = 8f; textAlign = Paint.Align.LEFT; color = 0xFF3E3A35.toInt() }

    @Suppress("unused") private val keepColor = Color.BLACK // ensures Color import retained
}
```

- [ ] **Step 2: Write the instrumented test `PdfReportExporterTest.kt`**

```kotlin
package com.example.yesmaam.export

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.yesmaam.domain.model.AttendanceRecord
import com.example.yesmaam.domain.model.AttendanceStatus.PRESENT
import com.example.yesmaam.domain.model.ClassInfo
import com.example.yesmaam.domain.model.StudentRef
import com.example.yesmaam.domain.report.ReportInputs
import com.example.yesmaam.domain.report.buildMonthlyReport
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class PdfReportExporterTest {
    @Test fun producesNonEmptyPdf() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val report = buildMonthlyReport(
            ReportInputs(
                classInfo = ClassInfo(1, "VI-A", null, "📕", "sage"),
                students = listOf(StudentRef(1, "Ann", "01", null)),
                attendance = listOf(AttendanceRecord(1, LocalDate.of(2026, 6, 3), PRESENT)),
                holidays = emptyList(),
                month = YearMonth.of(2026, 6),
            )
        )
        val bos = ByteArrayOutputStream()
        PdfReportExporter(ctx).write(report, bos)
        val bytes = bos.toByteArray()
        assertTrue(bytes.size > 100)
        assertTrue(String(bytes, 0, 5, Charsets.US_ASCII) == "%PDF-")
    }
}
```

- [ ] **Step 3: Run the instrumented test (emulator/device required)**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "com.example.yesmaam.export.PdfReportExporterTest"`
Expected: PASS. (Skip if no device; verify manually via the Reports screen in Task 23.)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/export/PdfReportExporter.kt app/src/androidTest/java/com/example/yesmaam/export/PdfReportExporterTest.kt
git commit -m "feat(export): PDF report exporter"
```

---

### Task 15: Share delivery + FileProvider

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/export/ExportDelivery.kt`
- Create: `app/src/main/res/xml/file_paths.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create `file_paths.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="exports" path="exports/" />
</paths>
```

- [ ] **Step 2: Add the FileProvider to the manifest**

Inside `<application>…</application>` in `app/src/main/AndroidManifest.xml`:

```xml
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
```

- [ ] **Step 3: Create `ExportDelivery.kt`**

```kotlin
package com.example.yesmaam.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.yesmaam.domain.report.MonthlyReport
import java.io.File
import java.io.FileOutputStream

object ExportDelivery {
    fun shareReport(context: Context, exporter: ReportExporter, report: MonthlyReport) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = "${report.classInfo.name}-${report.month}"
            .replace(Regex("[^A-Za-z0-9\\-_]"), "_")
        val file = File(dir, "$safeName.${exporter.format.ext}")
        FileOutputStream(file).use { exporter.write(report, it) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = exporter.format.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, "Share ${exporter.format.label} report")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
```

- [ ] **Step 4: Verify the build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/export/ExportDelivery.kt app/src/main/res/xml/file_paths.xml app/src/main/AndroidManifest.xml
git commit -m "feat(export): share delivery via FileProvider"
```

---

## Phase 5 — UI components

### Task 16: Reusable components

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/ui/components/ClassColors.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/components/Components.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/components/StatusToggle.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/components/StudentRow.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/components/ClassCard.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/components/CalendarGrid.kt`

- [ ] **Step 1: Create `ClassColors.kt` (colorKey → palette)**

```kotlin
package com.example.yesmaam.ui.components

import androidx.compose.ui.graphics.Color
import com.example.yesmaam.ui.theme.Blush
import com.example.yesmaam.ui.theme.BlushTint
import com.example.yesmaam.ui.theme.Lavender
import com.example.yesmaam.ui.theme.LavenderTint
import com.example.yesmaam.ui.theme.Peach
import com.example.yesmaam.ui.theme.PeachTint
import com.example.yesmaam.ui.theme.Sage
import com.example.yesmaam.ui.theme.SageTint

data class ClassPalette(val key: String, val color: Color, val tint: Color)

val ClassPalettes = listOf(
    ClassPalette("sage", Sage, SageTint),
    ClassPalette("peach", Peach, PeachTint),
    ClassPalette("blush", Blush, BlushTint),
    ClassPalette("lavender", Lavender, LavenderTint),
)

fun paletteFor(colorKey: String): ClassPalette =
    ClassPalettes.firstOrNull { it.key == colorKey } ?: ClassPalettes.first()

val ClassEmojis = listOf("📕", "📗", "🏺", "🎨", "🎵", "⚽", "✏️", "🔬", "💻", "🩰")
```

- [ ] **Step 2: Create `Components.kt`**

```kotlin
package com.example.yesmaam.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth(),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface) }
}

@Composable
fun Field(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = true, shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) { Box(Modifier.padding(14.dp)) { content() } }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    SectionCard(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun Avatar(initial: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(34.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(34.dp).then(Modifier).background(color, CircleShape))
        Text(initial, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

// small import shim for background used above
private fun Modifier.background(color: Color, shape: androidx.compose.ui.graphics.Shape) =
    this.then(androidx.compose.foundation.background(color, shape))
```

> Note: the `Avatar` background shim avoids an extra import line; if your linter
> prefers it, replace with a direct `Modifier.background(color, CircleShape)` and
> import `androidx.compose.foundation.background`.

- [ ] **Step 3: Create `StatusToggle.kt`**

```kotlin
package com.example.yesmaam.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.yesmaam.domain.model.AttendanceStatus
import com.example.yesmaam.ui.theme.LocalYesMaamColors

@Composable
fun StatusToggle(selected: AttendanceStatus, onSelect: (AttendanceStatus) -> Unit) {
    val c = LocalYesMaamColors.current
    Row {
        Seg("P", selected == AttendanceStatus.PRESENT, c.present, c.onPresent) { onSelect(AttendanceStatus.PRESENT) }
        Seg("A", selected == AttendanceStatus.ABSENT, c.absent, c.onAbsent) { onSelect(AttendanceStatus.ABSENT) }
        Seg("L", selected == AttendanceStatus.LATE, c.late, c.onLate) { onSelect(AttendanceStatus.LATE) }
    }
}

@Composable
private fun Seg(letter: String, on: Boolean, fill: Color, onColor: Color, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) fill else Color.Transparent, label = "seg")
    Surface(
        onClick = onClick,
        color = bg,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline),
        modifier = Modifier.size(44.dp).padding(2.dp()),
    ) {
        androidx.compose.foundation.layout.Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Text(
                letter,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = if (on) onColor else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Int.dp() = this.dp
```

> If `Modifier.size(44.dp).padding(2.dp())` triggers lints, simplify to
> `Modifier.padding(2.dp).size(44.dp)` with a normal `import ….dp`. The behaviour
> is identical: a 44dp tappable square with a hairline border.

- [ ] **Step 4: Create `StudentRow.kt`**

```kotlin
package com.example.yesmaam.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StudentRow(
    name: String,
    subtitle: String,
    avatarColor: Color,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(name.take(1).uppercase(), avatarColor)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(name, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Modifier.width(8.dp)
            trailing()
        }
    }
}
```

- [ ] **Step 5: Create `ClassCard.kt`**

```kotlin
package com.example.yesmaam.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ClassDayState(val label: String) { DONE("✓ done"), PENDING("not yet"), HOLIDAY("☂ holiday") }

@Composable
fun ClassCard(name: String, info: String, emoji: String, tint: Color, state: ClassDayState, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = tint, shape = MaterialTheme.shapes.medium, modifier = Modifier.size(46.dp)) {
                Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) { Text(emoji, style = MaterialTheme.typography.titleLarge) }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(info, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(state.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

- [ ] **Step 6: Create `CalendarGrid.kt`**

```kotlin
package com.example.yesmaam.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.yesmaam.ui.theme.LocalYesMaamColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

enum class DayMark { NONE, TAKEN, HOLIDAY }

@Composable
fun CalendarGrid(
    month: YearMonth,
    today: LocalDate,
    marks: Map<LocalDate, DayMark>,
    onDayClick: (LocalDate) -> Unit,
) {
    val c = LocalYesMaamColors.current
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val first = month.atDay(1)
        val lead = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
        val cells = buildList<LocalDate?> {
            repeat(lead) { add(null) }
            (1..month.lengthOfMonth()).forEach { add(month.atDay(it)) }
            while (size % 7 != 0) add(null)
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            val mark = marks[date] ?: DayMark.NONE
                            val bg = when (mark) {
                                DayMark.TAKEN -> c.presentTint; DayMark.HOLIDAY -> c.holidayTint
                                DayMark.NONE -> MaterialTheme.colorScheme.surface
                            }
                            val border = if (date == today) BorderStroke(2.dp, c.present)
                            else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(1f)
                                    .background(bg, MaterialTheme.shapes.small)
                                    .border(border, MaterialTheme.shapes.small)
                                    .clickable { onDayClick(date) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (mark == DayMark.HOLIDAY) "☂" else date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (mark == DayMark.HOLIDAY) c.onHoliday else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 7: Verify the build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Fix any import nits flagged in the inline notes for
`Avatar`/`StatusToggle`.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/ui/components
git commit -m "feat(ui): reusable components (toggle, rows, cards, calendar)"
```

---

## Phase 6 — Screens

> **ViewModel pattern (used by every screen):** build with an inline factory:
> ```kotlin
> val container = LocalContext.current.appContainer
> val vm: XViewModel = viewModel(factory = viewModelFactory { initializer { XViewModel(container, /*args*/) } })
> ```
> Imports: `androidx.lifecycle.viewmodel.compose.viewModel`,
> `androidx.lifecycle.viewmodel.initializer`, `androidx.lifecycle.viewmodel.viewModelFactory`,
> `androidx.compose.ui.platform.LocalContext`, `com.example.yesmaam.di.appContainer`.
> Collect state with `val state by vm.state.collectAsStateWithLifecycle()`
> (import `androidx.lifecycle.compose.collectAsStateWithLifecycle`).

### Task 17: Classes home screen

**Files:**
- Modify: `app/src/main/java/com/example/yesmaam/data/db/StudentDao.kt` (add counts query)
- Modify: `app/src/main/java/com/example/yesmaam/data/db/AttendanceDao.kt` (classes-with-attendance-today)
- Modify: `app/src/main/java/com/example/yesmaam/data/db/HolidayDao.kt` (classes-on-holiday-today)
- Modify: `app/src/main/java/com/example/yesmaam/data/AttendanceRepository.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/classes/ClassesViewModel.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/classes/ClassesScreen.kt`
- Modify: `app/src/main/java/com/example/yesmaam/ui/nav/NavGraph.kt`

- [ ] **Step 1: Add DAO queries**

In `StudentDao.kt` add (and a small projection):

```kotlin
data class ClassCount(val classId: Long, val n: Int)

@Query("SELECT classId, COUNT(*) AS n FROM students GROUP BY classId")
fun observeCounts(): kotlinx.coroutines.flow.Flow<List<ClassCount>>
```

In `AttendanceDao.kt` add:

```kotlin
@Query(
    """
    SELECT DISTINCT s.classId FROM attendance a
    INNER JOIN students s ON s.id = a.studentId WHERE a.date = :day
    """
)
fun observeClassIdsWithAttendance(day: Long): kotlinx.coroutines.flow.Flow<List<Long>>
```

In `HolidayDao.kt` add:

```kotlin
@Query("SELECT classId FROM holidays WHERE date = :day")
fun observeClassIdsOnHoliday(day: Long): kotlinx.coroutines.flow.Flow<List<Long>>
```

- [ ] **Step 2: Add repository pass-throughs**

In `AttendanceRepository.kt` add:

```kotlin
fun observeStudentCounts() = db.studentDao().observeCounts()
fun observeClassIdsWithAttendance(date: java.time.LocalDate) =
    db.attendanceDao().observeClassIdsWithAttendance(date.toEpochDay())
fun observeClassIdsOnHoliday(date: java.time.LocalDate) =
    db.holidayDao().observeClassIdsOnHoliday(date.toEpochDay())
```

- [ ] **Step 3: Create `ClassesViewModel.kt`**

```kotlin
package com.example.yesmaam.ui.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yesmaam.data.db.ClassEntity
import com.example.yesmaam.di.AppContainer
import com.example.yesmaam.ui.components.ClassDayState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class ClassRowUi(val entity: ClassEntity, val studentCount: Int, val state: ClassDayState)

class ClassesViewModel(container: AppContainer) : ViewModel() {
    private val repo = container.repository
    private val today = LocalDate.now()

    val state: StateFlow<List<ClassRowUi>> = combine(
        repo.observeClasses(),
        repo.observeStudentCounts(),
        repo.observeClassIdsWithAttendance(today),
        repo.observeClassIdsOnHoliday(today),
    ) { classes, counts, takenIds, holidayIds ->
        val countMap = counts.associate { it.classId to it.n }
        classes.map { c ->
            val st = when {
                c.id in holidayIds -> ClassDayState.HOLIDAY
                c.id in takenIds -> ClassDayState.DONE
                else -> ClassDayState.PENDING
            }
            ClassRowUi(c, countMap[c.id] ?: 0, st)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

- [ ] **Step 4: Create `ClassesScreen.kt`**

```kotlin
package com.example.yesmaam.ui.classes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.yesmaam.di.appContainer
import com.example.yesmaam.ui.components.ClassCard
import com.example.yesmaam.ui.components.PrimaryButton
import com.example.yesmaam.ui.components.paletteFor

@Composable
fun ClassesScreen(onOpenClass: (Long) -> Unit, onNewClass: () -> Unit) {
    val container = LocalContext.current.appContainer
    val vm: ClassesViewModel = viewModel(factory = viewModelFactory { initializer { ClassesViewModel(container) } })
    val rows by vm.state.collectAsStateWithLifecycle()

    Scaffold { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 18.dp)) {
            Text("Your Classes", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(rows, key = { it.entity.id }) { row ->
                    val p = paletteFor(row.entity.colorKey)
                    ClassCard(
                        name = row.entity.name,
                        info = "${row.studentCount} students" + (row.entity.note?.let { " · $it" } ?: ""),
                        emoji = row.entity.emoji,
                        tint = p.tint,
                        state = row.state,
                        onClick = { onOpenClass(row.entity.id) },
                    )
                }
            }
            PrimaryButton("＋  New class", onClick = onNewClass, modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}
```

- [ ] **Step 5: Wire `NavGraph.kt`**

Replace the body of `YesMaamNavGraph` with:

```kotlin
@Composable
fun YesMaamNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.CLASSES) {
        composable(Routes.CLASSES) {
            com.example.yesmaam.ui.classes.ClassesScreen(
                onOpenClass = { id -> nav.navigate(Routes.classHome(id)) },
                onNewClass = { nav.navigate(Routes.classEditor(null)) },
            )
        }
        // class editor, class home, settings composables are added in later tasks.
    }
}
```

Keep existing imports; remove the now-unused `Placeholder` only if no other route uses it.

- [ ] **Step 6: Build & run**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. The app launches to an empty "Your Classes" list with a "New class" button (tapping it will be wired in Task 18).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/data app/src/main/java/com/example/yesmaam/ui/classes app/src/main/java/com/example/yesmaam/ui/nav
git commit -m "feat(ui): classes home screen"
```

---

### Task 18: Class editor screen

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/ui/classes/ClassEditorViewModel.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/classes/ClassEditorScreen.kt`
- Modify: `app/src/main/java/com/example/yesmaam/ui/nav/NavGraph.kt`

- [ ] **Step 1: Create `ClassEditorViewModel.kt`**

```kotlin
package com.example.yesmaam.ui.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yesmaam.data.db.ClassEntity
import com.example.yesmaam.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClassEditorUi(
    val name: String = "",
    val note: String = "",
    val emoji: String = "📕",
    val colorKey: String = "sage",
    val loaded: Boolean = false,
)

class ClassEditorViewModel(private val container: AppContainer, private val classId: Long?) : ViewModel() {
    private val repo = container.repository
    private val _ui = MutableStateFlow(ClassEditorUi())
    val ui: StateFlow<ClassEditorUi> = _ui

    init {
        if (classId != null) viewModelScope.launch {
            repo.getClass(classId)?.let { c ->
                _ui.value = ClassEditorUi(c.name, c.note ?: "", c.emoji, c.colorKey, true)
            }
        } else _ui.update { it.copy(loaded = true) }
    }

    fun onName(v: String) = _ui.update { it.copy(name = v) }
    fun onNote(v: String) = _ui.update { it.copy(note = v) }
    fun onEmoji(v: String) = _ui.update { it.copy(emoji = v) }
    fun onColor(v: String) = _ui.update { it.copy(colorKey = v) }

    val canSave get() = _ui.value.name.isNotBlank()

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val s = _ui.value
        if (classId == null) {
            repo.createClass(ClassEntity(
                name = s.name.trim(), note = s.note.ifBlank { null }, emoji = s.emoji,
                colorKey = s.colorKey, createdAt = System.currentTimeMillis(),
            ))
        } else {
            repo.getClass(classId)?.let {
                repo.updateClass(it.copy(name = s.name.trim(), note = s.note.ifBlank { null }, emoji = s.emoji, colorKey = s.colorKey))
            }
        }
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        if (classId != null) repo.getClass(classId)?.let { repo.deleteClass(it) }
        onDone()
    }
}
```

- [ ] **Step 2: Create `ClassEditorScreen.kt`**

```kotlin
package com.example.yesmaam.ui.classes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.yesmaam.di.appContainer
import com.example.yesmaam.ui.components.ClassEmojis
import com.example.yesmaam.ui.components.ClassPalettes
import com.example.yesmaam.ui.components.Field
import com.example.yesmaam.ui.components.GhostButton
import com.example.yesmaam.ui.components.PrimaryButton

@Composable
fun ClassEditorScreen(classId: Long?, onDone: () -> Unit) {
    val container = LocalContext.current.appContainer
    val vm: ClassEditorViewModel = viewModel(factory = viewModelFactory { initializer { ClassEditorViewModel(container, classId) } })
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(18.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (classId == null) "New Class" else "Edit Class", style = MaterialTheme.typography.headlineMedium)
            Field("Class name", ui.name, vm::onName)
            Field("Note (optional)", ui.note, vm::onNote)

            Text("Emoji", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ClassEmojis.take(6).forEach { e ->
                    Box(
                        Modifier.size(40.dp)
                            .border(if (e == ui.emoji) 2.dp else 1.dp, if (e == ui.emoji) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                            .clickable { vm.onEmoji(e) },
                        contentAlignment = Alignment.Center,
                    ) { Text(e, style = MaterialTheme.typography.titleMedium) }
                }
            }

            Text("Colour", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ClassPalettes.forEach { p ->
                    Box(
                        Modifier.size(30.dp).background(p.color, CircleShape)
                            .border(if (p.key == ui.colorKey) 2.dp else 0.dp, if (p.key == ui.colorKey) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                            .clickable { vm.onColor(p.key) },
                    )
                }
            }

            PrimaryButton(if (classId == null) "Create class" else "Save", onClick = { if (vm.canSave) vm.save(onDone) })
            if (classId != null) GhostButton("Delete class", onClick = { vm.delete(onDone) })
        }
    }
}
```

- [ ] **Step 3: Add the route to `NavGraph.kt`**

Inside the `NavHost { }`, add:

```kotlin
        composable(
            Routes.CLASS_EDITOR,
            arguments = listOf(navArgument("classId") { type = NavType.LongType; defaultValue = -1L }),
        ) { entry ->
            val id = entry.arguments?.getLong("classId")?.takeIf { it >= 0 }
            com.example.yesmaam.ui.classes.ClassEditorScreen(classId = id, onDone = { nav.popBackStack() })
        }
```

Add imports `androidx.navigation.NavType` and `androidx.navigation.navArgument`.

- [ ] **Step 4: Build & run**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. "New class" now opens the editor; saving returns to a
populated home list with the correct emoji/colour/state.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/ui/classes app/src/main/java/com/example/yesmaam/ui/nav
git commit -m "feat(ui): class editor (create/edit/delete)"
```

---

### Task 19: In-class scaffold with bottom navigation

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/ui/classroom/ClassHomeScreen.kt`
- Modify: `app/src/main/java/com/example/yesmaam/ui/nav/NavGraph.kt`

> `ClassHomeScreen` owns the four in-class tabs with local tab state (no nested
> NavHost). Each tab composable is added in Tasks 20–23; here they are stubs so
> the scaffold builds and runs.

- [ ] **Step 1: Create `ClassHomeScreen.kt`**

```kotlin
package com.example.yesmaam.ui.classroom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.List

private enum class Tab(val label: String) { TODAY("Today"), CALENDAR("Calendar"), STUDENTS("Students"), REPORTS("Reports") }

@Composable
fun ClassHomeScreen(classId: Long, onEditClass: () -> Unit, onEditStudent: (Long?) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = {
                            Icon(
                                when (t) {
                                    Tab.TODAY -> Icons.Filled.CheckCircle
                                    Tab.CALENDAR -> Icons.Filled.DateRange
                                    Tab.STUDENTS -> Icons.Filled.Person
                                    Tab.REPORTS -> Icons.Filled.List
                                }, contentDescription = t.label,
                            )
                        },
                        label = { Text(t.label, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (Tab.entries[tab]) {
                Tab.TODAY -> TodayTabStub()        // replaced in Task 20
                Tab.CALENDAR -> CalendarTabStub()  // replaced in Task 21
                Tab.STUDENTS -> StudentsTabStub()  // replaced in Task 22
                Tab.REPORTS -> ReportsTabStub()    // replaced in Task 23
            }
        }
    }
}

@Composable private fun TodayTabStub() = Center("Today")
@Composable private fun CalendarTabStub() = Center("Calendar")
@Composable private fun StudentsTabStub() = Center("Students")
@Composable private fun ReportsTabStub() = Center("Reports")
@Composable private fun Center(s: String) = Box(Modifier.fillMaxSize(), Alignment.Center) { Text(s) }
```

- [ ] **Step 2: Add the route to `NavGraph.kt`**

```kotlin
        composable(
            Routes.CLASS_HOME,
            arguments = listOf(navArgument("classId") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments!!.getLong("classId")
            com.example.yesmaam.ui.classroom.ClassHomeScreen(
                classId = id,
                onEditClass = { nav.navigate(Routes.classEditor(id)) },
                onEditStudent = { sid -> nav.navigate(Routes.studentEditor(id, sid)) },
            )
        }
```

- [ ] **Step 3: Add the `material-icons` dependency if missing**

`material3` ships core filled icons via `androidx.compose.material:material-icons-core`,
which the BoM includes transitively. If `Icons.Filled.*` is unresolved, add to
`app/build.gradle.kts` dependencies: `implementation("androidx.compose.material:material-icons-core")`
(version from the Compose BoM).

- [ ] **Step 4: Build & run**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Opening a class shows four working bottom-nav tabs with
placeholder content.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/ui/classroom app/src/main/java/com/example/yesmaam/ui/nav app/build.gradle.kts
git commit -m "feat(ui): in-class scaffold with bottom navigation"
```

---

### Task 20: Today (mark attendance) screen

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/ui/classroom/today/TodayViewModel.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/classroom/today/TodayScreen.kt`
- Modify: `app/src/main/java/com/example/yesmaam/ui/classroom/ClassHomeScreen.kt`

- [ ] **Step 1: Create `TodayViewModel.kt`**

```kotlin
package com.example.yesmaam.ui.classroom.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yesmaam.data.db.StudentEntity
import com.example.yesmaam.di.AppContainer
import com.example.yesmaam.domain.model.AttendanceStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TodayViewModel(private val container: AppContainer, private val classId: Long) : ViewModel() {
    private val repo = container.repository
    val date: LocalDate = LocalDate.now()

    data class Row(val student: StudentEntity, val status: AttendanceStatus)
    data class Ui(val isHoliday: Boolean = false, val rows: List<Row> = emptyList(),
                  val present: Int = 0, val absent: Int = 0, val late: Int = 0)

    val ui: StateFlow<Ui> = combine(
        repo.observeStudents(classId),
        repo.observeDay(classId, date),
        repo.observeIsHoliday(classId, date),
    ) { students, records, holidayCount ->
        val byId = records.associate { it.studentId to it.status }
        val rows = students.map { Row(it, byId[it.id] ?: AttendanceStatus.PRESENT) }
        Ui(
            isHoliday = holidayCount > 0,
            rows = rows,
            present = rows.count { it.status == AttendanceStatus.PRESENT },
            absent = rows.count { it.status == AttendanceStatus.ABSENT },
            late = rows.count { it.status == AttendanceStatus.LATE },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Ui())

    fun toggle(studentId: Long, status: AttendanceStatus) = viewModelScope.launch {
        val current = ui.value.rows.associate { it.student.id to it.status }.toMutableMap()
        current[studentId] = status
        repo.saveDay(date, current) // persists the whole roster -> creates/updates the session
    }

    fun markHoliday() = viewModelScope.launch { repo.markHoliday(classId, date) }
    fun removeHoliday() = viewModelScope.launch { repo.removeHoliday(classId, date) }
}
```

- [ ] **Step 2: Create `TodayScreen.kt`**

```kotlin
package com.example.yesmaam.ui.classroom.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.yesmaam.di.appContainer
import com.example.yesmaam.ui.components.StudentRow
import com.example.yesmaam.ui.components.StatusToggle
import com.example.yesmaam.ui.components.paletteFor
import com.example.yesmaam.ui.theme.LocalYesMaamColors
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(classId: Long, colorKey: String) {
    val container = LocalContext.current.appContainer
    val vm: TodayViewModel = viewModel(factory = viewModelFactory { initializer { TodayViewModel(container, classId) } })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val c = LocalYesMaamColors.current
    val avatar = paletteFor(colorKey).color

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text(vm.date.format(DateTimeFormatter.ofPattern("EEEE · d MMM yyyy")),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp))
        Text("Today's Register", style = MaterialTheme.typography.headlineMedium)

        if (ui.isHoliday) {
            Surface(
                color = c.holidayTint, shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable { vm.removeHoliday() },
            ) { Text("☂ Marked as holiday — tap to remove", color = c.onHoliday,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp)) }
        } else {
            Surface(
                color = c.holidayTint, shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable { vm.markHoliday() },
            ) { Text("☂ No class today? Mark holiday", color = c.onHoliday,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp)) }

            Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("${ui.present} present", c.presentTint, c.onPresent)
                Chip("${ui.absent} absent", c.absentTint, c.onAbsent)
                Chip("${ui.late} late", c.lateTint, c.onLate)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ui.rows, key = { it.student.id }) { row ->
                    StudentRow(
                        name = row.student.name,
                        subtitle = "Roll ${row.student.rollNumber}",
                        avatarColor = avatar,
                    ) {
                        StatusToggle(selected = row.status, onSelect = { vm.toggle(row.student.id, it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, bg: Color, fg: Color) {
    Surface(color = bg, shape = CircleShape) {
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp))
    }
}
```

- [ ] **Step 3: Replace the Today stub in `ClassHomeScreen.kt`**

Pass the class colour to tabs: load the class once. At the top of `ClassHomeScreen`,
collect the class entity:

```kotlin
    val container = androidx.compose.ui.platform.LocalContext.current.appContainer
    val classEntity by container.repository.observeClass(classId)
        .collectAsStateWithLifecycle(initialValue = null)
    val colorKey = classEntity?.colorKey ?: "sage"
```

(imports: `com.example.yesmaam.di.appContainer`,
`androidx.lifecycle.compose.collectAsStateWithLifecycle`, `androidx.compose.runtime.getValue`)

Replace the `Tab.TODAY -> TodayTabStub()` branch with:

```kotlin
                Tab.TODAY -> com.example.yesmaam.ui.classroom.today.TodayScreen(classId, colorKey)
```

Delete the `TodayTabStub` function.

- [ ] **Step 4: Build & run**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Add a class + students (students come in Task 22; for
now verify the holiday bar toggles and the empty register renders). Tapping a
toggle persists the day.

> Manual check deferred to Task 22 once students can be added. The screen builds now.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/ui/classroom
git commit -m "feat(ui): Today attendance screen with auto-save"
```

---

### Task 21: Calendar screen + holidays

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/ui/classroom/calendar/CalendarViewModel.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/classroom/calendar/CalendarScreen.kt`
- Modify: `app/src/main/java/com/example/yesmaam/ui/classroom/ClassHomeScreen.kt`

- [ ] **Step 1: Create `CalendarViewModel.kt`**

```kotlin
package com.example.yesmaam.ui.classroom.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yesmaam.di.AppContainer
import com.example.yesmaam.ui.components.DayMark
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(private val container: AppContainer, private val classId: Long) : ViewModel() {
    private val repo = container.repository
    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month
    val today: LocalDate = LocalDate.now()

    data class Selected(val date: LocalDate, val isHoliday: Boolean, val hasSession: Boolean)
    private val _selected = MutableStateFlow<Selected?>(null)
    val selected: StateFlow<Selected?> = _selected

    val marks: StateFlow<Map<LocalDate, DayMark>> = _month.flatMapLatest { m ->
        combine(repo.observeMonth(classId, m), repo.observeHolidays(classId, m)) { attendance, holidays ->
            val holidaySet = holidays.map { LocalDate.ofEpochDay(it.date) }.toSet()
            val sessionSet = attendance.map { LocalDate.ofEpochDay(it.date) }.toSet() - holidaySet
            buildMap {
                sessionSet.forEach { put(it, DayMark.TAKEN) }
                holidaySet.forEach { put(it, DayMark.HOLIDAY) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun prevMonth() { _month.value = _month.value.minusMonths(1) }
    fun nextMonth() { _month.value = _month.value.plusMonths(1) }

    fun selectDay(date: LocalDate) {
        val m = marks.value[date]
        _selected.value = Selected(date, m == DayMark.HOLIDAY, m == DayMark.TAKEN)
    }

    fun markHoliday(date: LocalDate) = viewModelScope.launch {
        repo.markHoliday(classId, date); selectDay(date)
    }
    fun removeHoliday(date: LocalDate) = viewModelScope.launch {
        repo.removeHoliday(classId, date); selectDay(date)
    }
}
```

- [ ] **Step 2: Create `CalendarScreen.kt`**

```kotlin
package com.example.yesmaam.ui.classroom.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.yesmaam.di.appContainer
import com.example.yesmaam.ui.components.CalendarGrid
import com.example.yesmaam.ui.components.GhostButton
import com.example.yesmaam.ui.components.PrimaryButton
import com.example.yesmaam.ui.components.SectionCard
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(classId: Long) {
    val container = LocalContext.current.appContainer
    val vm: CalendarViewModel = viewModel(factory = viewModelFactory { initializer { CalendarViewModel(container, classId) } })
    val month by vm.month.collectAsStateWithLifecycle()
    val marks by vm.marks.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = vm::prevMonth) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            TextButton(onClick = vm::nextMonth) { Text("›", style = MaterialTheme.typography.headlineMedium) }
        }
        CalendarGrid(month = month, today = vm.today, marks = marks, onDayClick = vm::selectDay)

        selected?.let { sel ->
            SectionCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column {
                    Text(sel.date.format(DateTimeFormatter.ofPattern("EEEE · d MMMM")) +
                        if (sel.isHoliday) " — Holiday" else "", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (sel.isHoliday) {
                            PrimaryButton("Remove holiday", onClick = { vm.removeHoliday(sel.date) }, modifier = Modifier.weight(1f))
                        } else {
                            GhostButton("Mark holiday", onClick = { vm.markHoliday(sel.date) }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
```

> Editing a past day's attendance from the calendar reuses the Today flow against a
> chosen date. For v1 the calendar marks/unmarks holidays and shows session state;
> back-dated attendance editing is a noted follow-up (the `saveDay(date, …)` repo
> method already accepts any date, so wiring a date-parameterised register later is
> small).

- [ ] **Step 3: Replace the Calendar stub in `ClassHomeScreen.kt`**

Replace `Tab.CALENDAR -> CalendarTabStub()` with:

```kotlin
                Tab.CALENDAR -> com.example.yesmaam.ui.classroom.calendar.CalendarScreen(classId)
```

Delete `CalendarTabStub`.

- [ ] **Step 4: Build & run**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. The Calendar tab shows the month grid; tapping a day
reveals the action card; marking a holiday paints the cell peach.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/ui/classroom
git commit -m "feat(ui): calendar with holiday marking"
```

---

### Task 22: Students roster + student editor

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/domain/report/Mappers.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/classroom/students/StudentsViewModel.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/classroom/students/StudentsScreen.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/classroom/students/StudentEditorViewModel.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/classroom/students/StudentEditorScreen.kt`
- Modify: `app/src/main/java/com/example/yesmaam/ui/classroom/ClassHomeScreen.kt`
- Modify: `app/src/main/java/com/example/yesmaam/ui/nav/NavGraph.kt`

- [ ] **Step 1: Create `Mappers.kt` (entity → domain)**

```kotlin
package com.example.yesmaam.domain.report

import com.example.yesmaam.data.db.AttendanceEntity
import com.example.yesmaam.data.db.ClassEntity
import com.example.yesmaam.data.db.HolidayEntity
import com.example.yesmaam.data.db.StudentEntity
import com.example.yesmaam.domain.model.AttendanceRecord
import com.example.yesmaam.domain.model.ClassInfo
import com.example.yesmaam.domain.model.StudentRef
import java.time.LocalDate

fun ClassEntity.toInfo() = ClassInfo(id, name, note, emoji, colorKey)
fun StudentEntity.toRef() = StudentRef(id, name, rollNumber, guardianPhone)
fun AttendanceEntity.toRecord() = AttendanceRecord(studentId, LocalDate.ofEpochDay(date), status)
fun HolidayEntity.toDate(): LocalDate = LocalDate.ofEpochDay(date)
```

- [ ] **Step 2: Create `StudentsViewModel.kt`**

```kotlin
package com.example.yesmaam.ui.classroom.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yesmaam.data.db.StudentEntity
import com.example.yesmaam.di.AppContainer
import com.example.yesmaam.domain.model.ClassInfo
import com.example.yesmaam.domain.report.ReportInputs
import com.example.yesmaam.domain.report.buildMonthlyReport
import com.example.yesmaam.domain.report.toDate
import com.example.yesmaam.domain.report.toRecord
import com.example.yesmaam.domain.report.toRef
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

class StudentsViewModel(container: AppContainer, classId: Long) : ViewModel() {
    private val repo = container.repository
    private val month = YearMonth.now()

    data class RowUi(val student: StudentEntity, val percentage: Int)

    val rows: StateFlow<List<RowUi>> = combine(
        repo.observeStudents(classId),
        repo.observeMonth(classId, month),
        repo.observeHolidays(classId, month),
    ) { students, attendance, holidays ->
        val report = buildMonthlyReport(
            ReportInputs(
                classInfo = ClassInfo(classId, "", null, "", "sage"),
                students = students.map { it.toRef() },
                attendance = attendance.map { it.toRecord() },
                holidays = holidays.map { it.toDate() },
                month = month,
            )
        )
        val pct = report.rows.associate { it.student.id to it.percentage }
        students.map { RowUi(it, pct[it.id] ?: 0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

- [ ] **Step 3: Create `StudentsScreen.kt`**

```kotlin
package com.example.yesmaam.ui.classroom.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.yesmaam.di.appContainer
import com.example.yesmaam.ui.components.PrimaryButton
import com.example.yesmaam.ui.components.StudentRow
import com.example.yesmaam.ui.components.paletteFor

@Composable
fun StudentsScreen(classId: Long, colorKey: String, onEditStudent: (Long?) -> Unit) {
    val container = LocalContext.current.appContainer
    val vm: StudentsViewModel = viewModel(factory = viewModelFactory { initializer { StudentsViewModel(container, classId) } })
    val rows by vm.rows.collectAsStateWithLifecycle()
    val avatar = paletteFor(colorKey).color

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text("Students", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            items(rows, key = { it.student.id }) { r ->
                val phone = r.student.guardianPhone?.let { "☎ guardian · " } ?: ""
                StudentRow(
                    name = r.student.name,
                    subtitle = "Roll ${r.student.rollNumber} · $phone${r.percentage}%",
                    avatarColor = avatar,
                    modifier = Modifier.padding(0.dp),
                    trailing = { Text("›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                )
                Column(Modifier.padding(0.dp).clickableRow { onEditStudent(r.student.id) }) {}
            }
        }
        PrimaryButton("＋  Add a student", onClick = { onEditStudent(null) }, modifier = Modifier.padding(bottom = 16.dp))
    }
}

// Make the whole row tappable by wrapping StudentRow in a clickable; simplest is to
// add Modifier.clickable on StudentRow's modifier:
private fun Modifier.clickableRow(onClick: () -> Unit) = this
```

> Simplify the row tap: delete the trailing `Column(...).clickableRow{}` line and
> instead pass `modifier = Modifier.clickable { onEditStudent(r.student.id) }` to
> `StudentRow` (import `androidx.compose.foundation.clickable`). The placeholder
> `clickableRow` helper exists only so the file compiles if pasted verbatim; prefer
> the `clickable` modifier on `StudentRow`.

- [ ] **Step 4: Create `StudentEditorViewModel.kt`**

```kotlin
package com.example.yesmaam.ui.classroom.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yesmaam.data.db.StudentEntity
import com.example.yesmaam.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentEditorUi(val name: String = "", val roll: String = "", val phone: String = "")

class StudentEditorViewModel(
    private val container: AppContainer,
    private val classId: Long,
    private val studentId: Long?,
) : ViewModel() {
    private val repo = container.repository
    private val _ui = MutableStateFlow(StudentEditorUi())
    val ui: StateFlow<StudentEditorUi> = _ui

    init {
        if (studentId != null) viewModelScope.launch {
            repo.getStudent(studentId)?.let { _ui.value = StudentEditorUi(it.name, it.rollNumber, it.guardianPhone ?: "") }
        }
    }

    fun onName(v: String) = _ui.update { it.copy(name = v) }
    fun onRoll(v: String) = _ui.update { it.copy(roll = v) }
    fun onPhone(v: String) = _ui.update { it.copy(phone = v) }
    val canSave get() = _ui.value.name.isNotBlank() && _ui.value.roll.isNotBlank()

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val s = _ui.value
        if (studentId == null) {
            repo.addStudent(StudentEntity(
                classId = classId, name = s.name.trim(), rollNumber = s.roll.trim(),
                guardianPhone = s.phone.ifBlank { null }, createdAt = System.currentTimeMillis(),
            ))
        } else {
            repo.getStudent(studentId)?.let {
                repo.updateStudent(it.copy(name = s.name.trim(), rollNumber = s.roll.trim(), guardianPhone = s.phone.ifBlank { null }))
            }
        }
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        if (studentId != null) repo.getStudent(studentId)?.let { repo.deleteStudent(it) }
        onDone()
    }
}
```

- [ ] **Step 5: Create `StudentEditorScreen.kt`**

```kotlin
package com.example.yesmaam.ui.classroom.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.yesmaam.di.appContainer
import com.example.yesmaam.ui.components.Field
import com.example.yesmaam.ui.components.GhostButton
import com.example.yesmaam.ui.components.PrimaryButton

@Composable
fun StudentEditorScreen(classId: Long, studentId: Long?, onDone: () -> Unit) {
    val container = LocalContext.current.appContainer
    val vm: StudentEditorViewModel = viewModel(factory = viewModelFactory { initializer { StudentEditorViewModel(container, classId, studentId) } })
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (studentId == null) "Add a Student" else "Edit Student", style = MaterialTheme.typography.headlineMedium)
            Field("Full name", ui.name, vm::onName)
            Field("Roll number", ui.roll, vm::onRoll)
            Field("Guardian phone (optional)", ui.phone, vm::onPhone)
            PrimaryButton("Save student", onClick = { if (vm.canSave) vm.save(onDone) })
            if (studentId != null) GhostButton("Delete student", onClick = { vm.delete(onDone) })
        }
    }
}
```

- [ ] **Step 6: Wire the Students tab and the student-editor route**

In `ClassHomeScreen.kt`, replace `Tab.STUDENTS -> StudentsTabStub()` with:

```kotlin
                Tab.STUDENTS -> com.example.yesmaam.ui.classroom.students.StudentsScreen(classId, colorKey, onEditStudent)
```

Delete `StudentsTabStub`.

In `NavGraph.kt` add:

```kotlin
        composable(
            Routes.STUDENT_EDITOR,
            arguments = listOf(
                navArgument("classId") { type = NavType.LongType },
                navArgument("studentId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val cid = entry.arguments!!.getLong("classId")
            val sid = entry.arguments?.getLong("studentId")?.takeIf { it >= 0 }
            com.example.yesmaam.ui.classroom.students.StudentEditorScreen(cid, sid, onDone = { nav.popBackStack() })
        }
```

- [ ] **Step 7: Build & run — full attendance flow now testable**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Manual check: create a class → add 3 students → Today
tab shows them defaulting to Present → toggle one to Absent (auto-saves) → reopen
the class, the Absent persists → Students tab shows percentages.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/yesmaam
git commit -m "feat(ui): students roster + student editor"
```

---

### Task 23: Reports screen + export

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/ui/classroom/reports/ReportsViewModel.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/classroom/reports/PdfPreview.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/classroom/reports/ReportsScreen.kt`
- Modify: `app/src/main/java/com/example/yesmaam/ui/classroom/ClassHomeScreen.kt`

- [ ] **Step 1: Create `ReportsViewModel.kt`**

```kotlin
package com.example.yesmaam.ui.classroom.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yesmaam.di.AppContainer
import com.example.yesmaam.domain.report.MonthlyReport
import com.example.yesmaam.domain.report.ReportInputs
import com.example.yesmaam.domain.report.buildMonthlyReport
import com.example.yesmaam.domain.report.toDate
import com.example.yesmaam.domain.report.toInfo
import com.example.yesmaam.domain.report.toRecord
import com.example.yesmaam.domain.report.toRef
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(container: AppContainer, private val classId: Long) : ViewModel() {
    private val repo = container.repository
    private val settings = container.settings
    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month

    val report: StateFlow<MonthlyReport?> = _month.flatMapLatest { m ->
        combine(
            repo.observeClass(classId),
            repo.observeStudents(classId),
            repo.observeMonth(classId, m),
            repo.observeHolidays(classId, m),
            settings.settings,
        ) { cls, students, attendance, holidays, sett ->
            if (cls == null) null else buildMonthlyReport(
                ReportInputs(
                    classInfo = cls.toInfo(),
                    students = students.map { it.toRef() },
                    attendance = attendance.map { it.toRecord() },
                    holidays = holidays.map { it.toDate() },
                    month = m,
                    teacherName = sett.teacherName.ifBlank { null },
                    institution = sett.institution.ifBlank { null },
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun prevMonth() { _month.value = _month.value.minusMonths(1) }
    fun nextMonth() { _month.value = _month.value.plusMonths(1) }
}
```

- [ ] **Step 2: Create `PdfPreview.kt` (on-screen register preview)**

```kotlin
package com.example.yesmaam.ui.classroom.reports

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.yesmaam.domain.model.AttendanceStatus
import com.example.yesmaam.domain.report.MonthlyReport
import com.example.yesmaam.ui.theme.LocalYesMaamColors

@Composable
fun ReportPreview(report: MonthlyReport) {
    val c = LocalYesMaamColors.current
    val holidaySet = report.holidayDates.toSet()
    Column(Modifier.horizontalScroll(rememberScrollState())) {
        Row {
            Text("Name", Modifier.width(96.dp), style = MaterialTheme.typography.labelMedium)
            report.columnDates.forEach {
                Text(it.dayOfMonth.toString(), Modifier.width(20.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
            Text("%", Modifier.width(34.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
        }
        report.rows.forEach { row ->
            Row(Modifier.padding(top = 4.dp)) {
                Text(row.student.name.take(12), Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium)
                report.columnDates.forEach { date ->
                    val (letter, color) = when {
                        date in holidaySet -> "H" to c.onHoliday
                        row.statusByDate[date] == AttendanceStatus.PRESENT -> "P" to c.onPresent
                        row.statusByDate[date] == AttendanceStatus.LATE -> "L" to c.onLate
                        else -> "A" to c.onAbsent
                    }
                    Text(letter, Modifier.width(20.dp), textAlign = TextAlign.Center, color = color, style = MaterialTheme.typography.labelSmall)
                }
                Text("${row.percentage}", Modifier.width(34.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
```

- [ ] **Step 3: Create `ReportsScreen.kt`**

```kotlin
package com.example.yesmaam.ui.classroom.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.yesmaam.di.appContainer
import com.example.yesmaam.export.ExportDelivery
import com.example.yesmaam.export.PdfReportExporter
import com.example.yesmaam.export.XlsxReportExporter
import com.example.yesmaam.ui.components.PrimaryButton
import com.example.yesmaam.ui.components.GhostButton
import com.example.yesmaam.ui.components.SectionCard
import com.example.yesmaam.ui.components.StatCard
import java.time.format.DateTimeFormatter

@Composable
fun ReportsScreen(classId: Long) {
    val context = LocalContext.current
    val container = context.appContainer
    val vm: ReportsViewModel = viewModel(factory = viewModelFactory { initializer { ReportsViewModel(container, classId) } })
    val month by vm.month.collectAsStateWithLifecycle()
    val report by vm.report.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).verticalScroll(rememberScrollState())) {
        Text("Monthly Report", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = vm::prevMonth) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            TextButton(onClick = vm::nextMonth) { Text("›", style = MaterialTheme.typography.headlineMedium) }
        }

        report?.let { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatCard("${r.summary.sessionCount}", "school days", Modifier.weight(1f))
                StatCard("${r.summary.averagePercentage}%", "avg present", Modifier.weight(1f))
                StatCard("${r.summary.holidayCount}", "holidays", Modifier.weight(1f))
            }
            SectionCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) { ReportPreview(r) }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Export Excel", onClick = {
                    ExportDelivery.shareReport(context, XlsxReportExporter(), r)
                }, modifier = Modifier.weight(1f))
                GhostButton("Export PDF", onClick = {
                    ExportDelivery.shareReport(context, PdfReportExporter(context), r)
                }, modifier = Modifier.weight(1f))
            }
        }
    }
}
```

- [ ] **Step 4: Wire the Reports tab in `ClassHomeScreen.kt`**

Replace `Tab.REPORTS -> ReportsTabStub()` with:

```kotlin
                Tab.REPORTS -> com.example.yesmaam.ui.classroom.reports.ReportsScreen(classId)
```

Delete `ReportsTabStub`. (`onEditClass` remains available; wire it to a top-app-bar
edit action if desired — optional for v1.)

- [ ] **Step 5: Build, run, and export end-to-end**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Manual: mark a few days, open Reports, tap **Export
Excel** → share sheet → save/open the `.xlsx` (opens in Sheets/Excel). Tap **Export
PDF** → share → the register PDF renders with serif fonts and pastel marks.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/ui/classroom
git commit -m "feat(ui): reports screen with Excel and PDF export"
```

---

### Task 24: Settings screen

**Files:**
- Create: `app/src/main/java/com/example/yesmaam/ui/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/example/yesmaam/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/example/yesmaam/ui/classes/ClassesScreen.kt`
- Modify: `app/src/main/java/com/example/yesmaam/ui/nav/NavGraph.kt`

- [ ] **Step 1: Create `SettingsViewModel.kt`**

```kotlin
package com.example.yesmaam.ui.settings

import androidx.lifecycle.ViewModel
import com.example.yesmaam.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val store = container.settings
    private val _ui = MutableStateFlow(
        store.settings.value.let { Pair(it.teacherName, it.institution) }
    )
    val ui: StateFlow<Pair<String, String>> = _ui

    fun onName(v: String) = _ui.update { it.copy(first = v) }
    fun onInstitution(v: String) = _ui.update { it.copy(second = v) }
    fun save(onDone: () -> Unit) { store.update(_ui.value.first.trim(), _ui.value.second.trim()); onDone() }
}
```

- [ ] **Step 2: Create `SettingsScreen.kt`**

```kotlin
package com.example.yesmaam.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.yesmaam.di.appContainer
import com.example.yesmaam.ui.components.Field
import com.example.yesmaam.ui.components.PrimaryButton

@Composable
fun SettingsScreen(onDone: () -> Unit) {
    val container = LocalContext.current.appContainer
    val vm: SettingsViewModel = viewModel(factory = viewModelFactory { initializer { SettingsViewModel(container) } })
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Text("Shown on the header of exported reports.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Field("Your name", ui.first, vm::onName)
            Field("Institution (optional)", ui.second, vm::onInstitution)
            PrimaryButton("Save", onClick = { vm.save(onDone) })
        }
    }
}
```

- [ ] **Step 3: Add a Settings entry point on the Classes header**

In `ClassesScreen.kt`, change the signature to add `onOpenSettings: () -> Unit` and
replace the title `Text("Your Classes", …)` with a header row:

```kotlin
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.TextButton

// inside Column, replacing the single title Text:
Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
    Text("Your Classes", style = MaterialTheme.typography.headlineMedium)
    TextButton(onClick = onOpenSettings) { Text("⚙", style = MaterialTheme.typography.headlineMedium) }
}
```

- [ ] **Step 4: Wire the Settings route and the new callback in `NavGraph.kt`**

Update the `Routes.CLASSES` composable call to pass
`onOpenSettings = { nav.navigate(Routes.SETTINGS) }`, and add:

```kotlin
        composable(Routes.SETTINGS) {
            com.example.yesmaam.ui.settings.SettingsScreen(onDone = { nav.popBackStack() })
        }
```

- [ ] **Step 5: Build & run**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. The ⚙ on the home header opens Settings; saving a name
makes it appear in the next exported report header.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/yesmaam/ui
git commit -m "feat(ui): settings (teacher name + institution)"
```

---

## Phase 7 — Integration & verification

### Task 25: DAO integration test, final build, acceptance pass

**Files:**
- Create: `app/src/androidTest/java/com/example/yesmaam/data/db/DaoTest.kt`

- [ ] **Step 1: Write the instrumented DAO test**

```kotlin
package com.example.yesmaam.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.yesmaam.domain.model.AttendanceStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: AppDatabase

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun teardown() = db.close()

    @Test fun deletingClassCascadesStudentsAndAttendance() = runBlocking {
        val classId = db.classDao().insert(ClassEntity(name = "VI-A", emoji = "📕", colorKey = "sage", createdAt = 0))
        val sId = db.studentDao().insert(StudentEntity(classId = classId, name = "Ann", rollNumber = "01", createdAt = 0))
        db.attendanceDao().upsertAll(listOf(AttendanceEntity(studentId = sId, date = 100, status = AttendanceStatus.PRESENT)))

        db.classDao().get(classId)?.let { db.classDao().delete(it) }

        assertEquals(emptyList<StudentEntity>(), db.studentDao().getByClass(classId))
        assertEquals(emptyList<AttendanceEntity>(), db.attendanceDao().getForClassRange(classId, 0, 1000))
    }

    @Test fun upsertReplacesSameStudentAndDate() = runBlocking {
        val classId = db.classDao().insert(ClassEntity(name = "C", emoji = "📕", colorKey = "sage", createdAt = 0))
        val sId = db.studentDao().insert(StudentEntity(classId = classId, name = "Ann", rollNumber = "01", createdAt = 0))
        db.attendanceDao().upsertAll(listOf(AttendanceEntity(studentId = sId, date = 100, status = AttendanceStatus.PRESENT)))
        db.attendanceDao().upsertAll(listOf(AttendanceEntity(studentId = sId, date = 100, status = AttendanceStatus.ABSENT)))

        val rows = db.attendanceDao().observeForClassRange(classId, 0, 1000).first()
        assertEquals(1, rows.size)
        assertEquals(AttendanceStatus.ABSENT, rows.single().status)
    }
}
```

- [ ] **Step 2: Run all JVM unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS — `ReportBuilderTest`, `XlsxWriterTest`, `XlsxReportExporterTest`.

- [ ] **Step 3: Run instrumented tests (emulator/device)**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: PASS — `DaoTest`, `PdfReportExporterTest`.

- [ ] **Step 4: Full debug build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Manual acceptance checklist (on device)**

Walk the whole flow and confirm each:
- [ ] Create a class with an emoji + colour; it appears on the home shelf.
- [ ] Open it; add 3 students with roll numbers (one with a guardian phone).
- [ ] Today tab: all default to Present; toggle one Absent, one Late — reopening the class shows them persisted (auto-save).
- [ ] Today tab: "Mark holiday" paints the day; the holiday clears with one tap.
- [ ] Calendar tab: taken days are sage, the holiday is peach ☂, today is ringed; marking/removing a holiday updates the grid.
- [ ] Students tab: each student shows this month's %.
- [ ] Reports tab: stat cards show school days / avg % / holidays; the preview grid matches; **Export Excel** opens a valid `.xlsx`; **Export PDF** opens a serif register PDF.
- [ ] Settings: name entered shows up in the exported report header.

- [ ] **Step 6: Commit**

```bash
git add app/src/androidTest/java/com/example/yesmaam/data/db/DaoTest.kt
git commit -m "test(data): DAO cascade + upsert integration tests"
```

---

## Spec coverage check

| Spec area | Implemented by |
|-----------|----------------|
| Pastel serif design system | Tasks 2–3, 16 (+ `docs/design-system.md`) |
| Generic classes (CRUD, emoji, colour) | Tasks 17–18 |
| Students roster (roll, guardian phone) | Task 22 |
| Daily attendance, Present/Absent/Late, auto-save | Task 20 |
| Session-day rule + percentages (pure, tested) | Task 11 |
| Holidays per class + calendar painting | Tasks 21, 6 (DAO), 8 (repo) |
| Excel export (hand-rolled OOXML) | Tasks 12–13 |
| PDF export (PdfDocument + serif) | Task 14 |
| Extensible exporter seam | Task 13 (`ReportExporter`/`ExportFormat`) |
| Share delivery (FileProvider) | Task 15 |
| Settings (teacher/institution in report header) | Tasks 9, 24 |
| Offline, on-device, manual DI | Tasks 4, 7, 10 |
| Testing strategy (unit + instrumented) | Tasks 11, 12, 13, 14, 25 |

**Deferred (spec non-goals, untouched):** accounts/sync, multi-teacher, dark mode,
bulk import, CSV export, save-to-Downloads, back-dated attendance editing from the
calendar (repo already supports it; UI wiring is a small follow-up noted in Task 21).

