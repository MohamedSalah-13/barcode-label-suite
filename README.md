# Barcode Label Suite

A reusable, modular Java 21 / JavaFX library for designing and printing barcode labels.

## Current release

The actively developed application is phase 6 final (`0.6.1-SNAPSHOT`). It provides:

- An immutable label-template model using millimetres.
- Sheet, single-label and roll page definitions.
- Text, barcode, image and shape elements.
- JSON save/load with a schema version.
- `{{field}}` data binding.
- A reusable JavaFX label designer and high-quality preview.
- Java2D barcode rendering and printing for sheets, rolls and single labels.
- CSV import, field mapping, quantity expansion and batch validation.
- Page-range printing, cancellation and typed print failures.
- A batch-production desktop workbench.

## Requirements

- JDK 21 (`JAVA_HOME` must point to it).
- Windows for the supplied launch script and desktop printing workflow.

The repository includes Maven Wrapper 3.9.9, so a separate Maven installation is optional.

## Build and run phase 6

```powershell
.\start-phase6-v2.cmd
```

This installs the required phase artifacts in dependency order, runs the tests, and starts the final demo. Do not use `start-phase6.cmd`; it targets the earlier phase-6 reactor.

To verify without starting the desktop application, run the same Maven stages shown in [the CI workflow](.github/workflows/build.yml). The base milestone alone can still be built with:

```powershell
.\mvnw.cmd clean verify
```

Arabic integration instructions are available in [`docs/INTEGRATION_AR.md`](docs/INTEGRATION_AR.md).

## Main modules

- `barcode-label-model`: UI-independent document model.
- `barcode-label-data`: JSON persistence and field binding.
- `barcode-label-render`: renderer contracts.
- `barcode-label-print`: sheet planning and print contracts.
- `barcode-label-render-java2d`: ZXing/Barcode4J rendering implementation.
- `barcode-label-print-java2d`: shared Java2D printing implementation.
- `barcode-label-workflow`: CSV import and universal page planning.
- `barcode-label-batch`: mapping, validation and batch preparation.
- `barcode-label-fx-designer`, `barcode-label-fx-workbench`, `barcode-label-fx-batch`: reusable JavaFX UI layers.

The root `pom.xml` covers the original milestone modules. Later phases currently retain standalone parent POMs for compatibility; the launch script and CI workflow encode their required build order.
