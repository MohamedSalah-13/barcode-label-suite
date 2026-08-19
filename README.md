# Barcode Label Suite

A reusable, modular Java 21 / JavaFX library for designing and printing barcode labels.

## Current milestone

The first milestone provides:

- An immutable label-template model using millimetres.
- Sheet, single-label and roll page definitions.
- Text, barcode, image and shape elements.
- JSON save/load with a schema version.
- `{{field}}` data binding.
- A reusable JavaFX `LabelDesigner` preview control.
- A demo application and unit tests.
- Rendering and printing extension contracts for the next milestones.

## Build

```powershell
mvn clean verify
```

## Run the demo

```powershell
mvn -pl barcode-label-demo -am javafx:run
```

Arabic integration instructions are available in [`docs/INTEGRATION_AR.md`](docs/INTEGRATION_AR.md).

## Modules

- `barcode-label-model`: UI-independent document model.
- `barcode-label-data`: JSON persistence and field binding.
- `barcode-label-render`: renderer contracts.
- `barcode-label-print`: sheet planning and print contracts.
- `barcode-label-fx`: embeddable JavaFX designer/preview control.
- `barcode-label-demo`: example desktop application.
