# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Java 21 / JavaFX library and desktop app for designing and printing barcode labels (`com.mohamed.barcodelabel`), built as a Maven multi-module project. The domain model works in millimetres; labels are laid out on sheets/rolls/single pages and rendered/printed via pluggable contracts.

## Build & run — this repo has two build trees, not one

The root `pom.xml` is a Maven reactor covering only the original milestone-1 modules:
`barcode-label-model`, `barcode-label-data`, `barcode-label-render`, `barcode-label-print`, `barcode-label-fx`, `barcode-label-demo`.

```powershell
mvn clean verify                              # builds/tests milestone-1 modules only
mvn -pl barcode-label-demo -am javafx:run     # runs the milestone-1 demo
```

Everything built after milestone 1 (phases 2–6: `render-java2d`, `print-java2d`, `fx-designer*`, `fx-workbench`, `fx-preview`, `fx-print`, `workflow`, `batch`, `fx-batch`, `demo-v6`) is **not** in the root reactor. The required parent POMs are `phase2-pom.xml`, `phase3-pom.xml`, `phase3-build-pom.xml`, `phase3-final-pom.xml`, `phase4-final-pom.xml`, `phase5-pom.xml`, `phase5-fixed-pom.xml`, and `phase6-final-pom.xml`. Later phases depend on artifacts installed to the local repo by earlier ones (`relativePath` + `${base.version}`/`${phaseN.version}` properties). To build a later phase, its dependency chain must already be `mvn install`ed. Read `verify-phase6.ps1` for the install order.

The current/most complete state is phase 6 final (`0.6.1-SNAPSHOT`, batch CSV production workflow). To build and run it:

```powershell
.\start-phase6-v2.cmd
```

[`PHASE6_FINAL_AR.md`](PHASE6_FINAL_AR.md) documents the current app in Arabic; `docs/INTEGRATION_AR.md` documents consuming `barcode-label-fx`/`barcode-label-data`/`barcode-label-print` as a library from another JavaFX app.

Run a single test module: `mvn -f <phaseN-pom.xml> -pl <module-name> test` (or plain `mvn test` inside a milestone-1 module, or from the root reactor).

## Architecture

**Module dependency direction** (lower layers know nothing about upper ones):
`model` → `data` (JSON persistence, `{{field}}` binding) / `render` (rasterization contracts) / `print` (sheet layout contracts) → `render-java2d` / `print-java2d` (Java2D implementations) → `workflow` (CSV import + universal page planning, phase 5) → `batch` (CSV-to-print validation/preparation, phase 6) → the `fx-*` UI modules, which compose into demo apps.

**Two module systems coexist.** Milestone-1 modules (`model`, `data`, `render`, `print`, `fx`, `demo`) use real JPMS (`module-info.java`, `requires`/`exports`). Everything added in phases 2–6 builds on the classpath instead, using `Automatic-Module-Name` manifest entries (set via `maven-jar-plugin`) rather than `module-info.java`. Don't assume a phase-2+ module has a module descriptor — check before adding a `requires`.

**Domain model (`barcode-label-model`)** is the immutable core: `LabelTemplate` (schema-versioned, holds a `PageDefinition` and a list of `LabelElement`s — barcode/text/image/shape, each anchored by an `ElementFrame`), geometry types (`DimensionsMm`, `InsetsMm`, `RectMm`) all in millimetres, and page/sheet layout (`PageDefinition`, `PageMode`, `SheetLayout`). Validation lives in compact record constructors; each geometry/element record currently re-implements its own `requireNonNegative`/`requirePositive` helpers rather than sharing one.

**Data flow for the batch/CSV workflow (phase 5–6, the actively-developed path):** `CsvImportService`/`CsvDataset` (workflow) parse a CSV → `ColumnMapping` maps CSV columns to template `{{field}}`s (batch) → `BatchPreparationService` validates rows against the mapping and produces `BatchPreparationResult`/`BatchValidationIssue` → `UniversalPagePlanner` (workflow) lays out `PlannedPage`/`PlannedPlacement` against a `PaperPreset` → `barcode-label-print-java2d` renders/prints. The FX layer (`BatchProductionWorkbench`, `BatchDataMappingPane` in `barcode-label-fx-batch`) drives this end to end; it's built via an ant-copy + source-patch step in `phase6-build/fx/pom.xml` rather than compiling `barcode-label-fx-batch` directly — check that plugin config before editing files under `barcode-label-fx-batch/src`, since a source patch there is reapplied at build time.

**Rendering/printing contracts vs. implementations**: `barcode-label-render`/`barcode-label-print` define renderer/printer interfaces and value types (label-independent); `barcode-label-render-java2d` (ZXing + Barcode4J rasterizers) and `barcode-label-print-java2d` (`Graphics2D`-based sheet rendering/printing, `PrinterCatalog`) are the concrete Java2D implementations phases 3+ build against.

**Persistence**: `barcode-label-data`'s `TemplateJsonService` (de)serializes `LabelTemplate` to/from JSON (Jackson) with `schemaVersion`; `TemplateFieldResolver` resolves `{{field}}` placeholders against a data map under a `MissingFieldPolicy` (e.g. `FAIL`).
