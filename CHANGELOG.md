# Changelog

All notable changes to Barcode Label Suite are listed here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/).

## [0.7.0] - 2026-11-17

### Fixed
- **Designer elements were invisible.** Element views on the label canvas were never sized, so their content, selection border and resize handle did not show, and rubber-band selection only caught an element's top-left corner.
- **The designer showed the label mirrored** inside the right-to-left UI: elements appeared on the wrong side, dragging and the arrow keys moved them the wrong way, and the resize handle sat bottom-left. The label surface is now left to right, matching the printout, and sits on the right of the workspace.
- **"Apply properties" applied only position and size.** Text, rotation, font, colour, alignment, lock, visibility and image fit were silently dropped, and every click added several empty undo steps.
- **Ctrl+Shift+Z undid instead of redoing.**
- **A stale batch stayed printable.** After importing another CSV or changing the mapping, without validating again, the print tab still held and would print the old labels.
- **Manual column choices were lost** when the template was edited and the CSV tab reopened.
- The CSV summary kept saying "ready" after a new file was imported.
- The data preview titled "first 8 rows" showed three.
- **A template that looked right could fail to print:** the printer accepted only `#RRGGBB` colours while the designer also accepted names such as `red`.
- The print confirmation ignored copies and page ranges, so it under-reported the job.
- The custom paper dialog showed the model's English error messages, and asked for a "whole number" (رقمًا صحيحًا) in fields that take decimals such as 63.5.
- English left in the Arabic UI: the undo and redo buttons ("تراجع: Move elements"), the names of new elements and of the empty template ("Rectangle", "New label"), the paper type and image fit lists (`SHEET`, `CONTAIN`), file type names and the OK/Cancel buttons of dialogs.
- Property names in the designer, the Browse button and the print pane's page-range captions were cut short ("الع…", "…") when the panels were narrow.

### Added
- **An English interface.** Every screen is in Arabic (right to left) or English (left to right), chosen from a language menu at the end of the tab bar and remembered between runs. Switching keeps the label being designed; the untouched starting label switches language too. Embedding applications pass a `UiLanguage` to the panes' constructors; without one they stay Arabic. The texts live in `messages_ar.properties`/`messages_en.properties` per module, and the tests check that both languages have every text and that the English screens show no Arabic.
- `README.ar.md`, an Arabic README, and `sample-products-quantities-en.csv`, the sample data in English.
- **Automatic CSV column suggestions** from column names: exact names, English and Arabic synonyms (`Name`/`الصنف`, `Code`/`Barcode`/`الكود`, `Retail Price`/`السعر`…) and shared words. The quantity column is picked too (`Qty`, `الكمية`, `عدد النسخ`…), and the screen asks the user to review the suggestion.
- Colours accept every form the designer does: the 148 colour names, `#RGB`, `#RRGGBB`, `#RRGGBBAA`, `rgb()`, `rgba()`, `hsl()`, `hsla()` (`com.mohamed.barcodelabel.model.color.LabelColors`). An invalid colour shows and prints as black.
- The print confirmation states the labels and pages that will print, the copies and the page range.
- `CHANGELOG.md`, an MIT `LICENSE`, and a release workflow that builds, tests, packages and publishes the Windows application when a `v*` tag is pushed.
- CI on Linux as well as Windows.
- Screenshots of the designer, the CSV screen and the print preview in the README (English) and `README.ar.md` (Arabic), and a walkthrough with the sample CSV.

### Changed
- **One build:** every module builds and tests from the root `pom.xml` with `mvnw clean install`, at one shared version. `start.cmd`/`start.sh` build and start the app; `package.ps1` makes the Windows app image and takes its version from `pom.xml`.
- The application is `barcode-label-app`, main class `com.mohamed.barcodelabel.app.BarcodeLabelSuiteLauncher`, and the window is titled "Barcode Label Suite".
- The print screen numbers sheet slots from 1 instead of 0.
- Validation messages are in Arabic, configuration problems no longer claim to be in "row 1", and counts use Arabic number agreement ("10 ملصقات من 4 صفوف").
- 8-digit hex colours are read as `#RRGGBBAA` in print too, as the designer reads them (print used `#AARRGGBB`).
- `BatchValidationIssue` carries a `BatchIssueKind` and the offending value; the four-argument constructor still works.
- The model's value checks share one internal `Checks` class; their messages are uniform and name the value.
- `docs/INTEGRATION_AR.md` is rewritten for the current modules; the Arabic user guide moves to `docs/USER_GUIDE_AR.md`.
- Test coverage: 227 tests (from 57), and JaCoCo requires 70% line coverage in every module.

### Removed
- The milestone-1 `barcode-label-fx` and `barcode-label-demo` modules (use `barcode-label-fx-designer`'s `VisualLabelDesigner`), and the unused `barcode-label-fx-preview` and `barcode-label-fx-print` modules.
- The per-phase parent POMs, build folders and scripts, and `sample-products.csv`.

### بالعربية
- **أخطاء أُصلحت:** عناصر المصمّم لم تكن تظهر؛ المصمّم كان يعرض الملصق معكوسًا ويحرّك العناصر بالاتجاه الخطأ؛ "تطبيق الخصائص" كان يطبّق الموقع والحجم فقط؛ دفعة CSV قديمة كانت تبقى قابلة للطباعة بعد تغيير الملف أو المطابقة؛ المطابقة اليدوية كانت تضيع؛ قالب سليم في المصمّم قد يفشل في الطباعة بسبب أسماء الألوان.
- **جديد:** واجهة إنجليزية بجانب العربية، تختارها من قائمة اللغة ويتذكرها البرنامج؛ ملف README بالعربية؛ اقتراح تلقائي لربط أعمدة CSV بالحقول، بالعربي والإنجليزي، مع عمود الكمية؛ قبول كل صيغ الألوان في الطباعة؛ رسالة تأكيد طباعة تذكر عدد الملصقات والصفحات والنسخ؛ ترقيم الخانات من 1.
- **تغييرات:** بناء المشروع كله بأمر واحد؛ رسائل التحقق بالعربية وبصياغة أعداد صحيحة؛ حذف الموديولات غير المستخدمة؛ 227 اختبارًا وحد تغطية 70%.

## [0.6.1] - 2026-11-17

The phase-6 batch-production application (CSV import, column mapping, per-row quantities, batch validation and printing) at the version set before 0.7.0's changes.
