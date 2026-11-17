# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Java 21 / JavaFX library and desktop app for designing and printing barcode labels (`com.mohamed.barcodelabel`), built as a Maven multi-module project. The domain model works in millimetres; labels are laid out on sheets/rolls/single pages and rendered/printed via pluggable contracts.

## Build & run

The root `pom.xml` is a single Maven reactor covering every module, in dependency order, with one shared version (`0.6.1-SNAPSHOT`). Internal dependencies use `${project.version}`; third-party and plugin versions live in the root `dependencyManagement`/`pluginManagement`.

```powershell
.\mvnw.cmd clean install                          # build and test everything
.\start.cmd                                       # build, test and start the app (start.sh on Linux/macOS)
.\mvnw.cmd -pl barcode-label-app javafx:run       # start the app after an install
.\package.ps1                                     # Windows app image via jpackage
```

The JavaFX UI tests (`barcode-label-fx-workbench`, `barcode-label-fx-batch`) start the JavaFX toolkit and need a display; on headless Linux run Maven under `xvfb-run -a`.

Run a single module's tests: `mvnw -pl <module-name> -am test`.

The application is `barcode-label-app` (main class `com.mohamed.barcodelabel.demo.v6.Phase6Launcher`). [`PHASE6_FINAL_AR.md`](PHASE6_FINAL_AR.md) documents it in Arabic; `docs/INTEGRATION_AR.md` documents consuming the modules as a library from another JavaFX app.

JaCoCo enforces a 25% line-coverage baseline on every module with tests, except `barcode-label-fx-designer`, which sets `jacoco.haltOnFailure=false` until its coverage is raised.

## Architecture

**Module dependency direction** (lower layers know nothing about upper ones):
`model` → `data` (JSON persistence, `{{field}}` binding) / `render` (rasterization contracts) / `print` (sheet layout contracts) → `render-java2d` / `print-java2d` (Java2D implementations) → `workflow` (CSV import + universal page planning) → `batch` (CSV-to-print validation/preparation) → the `fx-*` UI modules, which compose into `barcode-label-app`.

**Two module systems coexist.** Milestone-1 modules (`model`, `data`, `render`, `print`, `fx`, `demo`) use real JPMS (`module-info.java`, `requires`/`exports`). Every later module (`render-java2d`, `print-java2d`, `workflow`, `batch`, the other `fx-*` modules and `app`) builds on the classpath instead, using `Automatic-Module-Name` manifest entries (set via `maven-jar-plugin`) rather than `module-info.java`. Don't assume one of these modules has a module descriptor — check before adding a `requires`.

**Domain model (`barcode-label-model`)** is the immutable core: `LabelTemplate` (schema-versioned, holds a `PageDefinition` and a list of `LabelElement`s — barcode/text/image/shape, each anchored by an `ElementFrame`), geometry types (`DimensionsMm`, `InsetsMm`, `RectMm`) all in millimetres, and page/sheet layout (`PageDefinition`, `PageMode`, `SheetLayout`). Validation lives in compact record constructors; each geometry/element record currently re-implements its own `requireNonNegative`/`requirePositive` helpers rather than sharing one.

**Data flow for the batch/CSV workflow (the actively-developed path):** `CsvImportService`/`CsvDataset` (workflow) parse a CSV → `ColumnMapping` maps CSV columns to template `{{field}}`s (batch) → `BatchPreparationService` validates rows against the mapping and produces `BatchPreparationResult`/`BatchValidationIssue` → `UniversalPagePlanner` (workflow) lays out `PlannedPage`/`PlannedPlacement` against a `PaperPreset` → `barcode-label-print-java2d` renders/prints. The FX layer (`BatchProductionWorkbench`, `BatchDataMappingPane` in `barcode-label-fx-batch`) drives this end to end.

**Rendering/printing contracts vs. implementations**: `barcode-label-render`/`barcode-label-print` define renderer/printer interfaces and value types (label-independent); `barcode-label-render-java2d` (ZXing + Barcode4J rasterizers) and `barcode-label-print-java2d` (`Graphics2D`-based sheet rendering/printing, `PrinterCatalog`) are the concrete Java2D implementations the UI modules build against.

**Persistence**: `barcode-label-data`'s `TemplateJsonService` (de)serializes `LabelTemplate` to/from JSON (Jackson) with `schemaVersion`; `TemplateFieldResolver` resolves `{{field}}` placeholders against a data map under a `MissingFieldPolicy` (e.g. `FAIL`).
