# Changelog

All notable changes to Barcode Label Suite are listed here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/).

## [0.8.0] - 2026-11-17

### Added
- **An English interface.** Every screen is in Arabic (right to left) or English (left to right), chosen from a language menu at the end of the tab bar and remembered between runs. Switching keeps the label being designed; the untouched starting label switches language too. Embedding applications pass a `UiLanguage` to the panes' constructors; without one they stay Arabic. The texts live in `messages_ar.properties`/`messages_en.properties` per module, and the tests check that both languages have every text and that the English screens show no Arabic.
- **Print one label without a CSV file.** The print tab shows a field for each `{{field}}` of the template; the typed values make one label, and the number of copies is the number of labels. Printing stops and names any field left blank.
- In the designer, Enter in a property field applies it, and a double click on an element jumps to its text or barcode data. The content field's caption names what it holds: text, barcode data or image path.
- **Resizable, hideable designer panels.** The elements and properties panels sit beside the label behind dividers that resize them, and two check boxes in the designer's status bar hide or show each one. A hidden panel comes back at its width; a double click on an element shows the properties panel again. The properties panel scrolls when the window is short.
- **Named custom paper sizes.** The custom size dialog takes a name (or makes one from the label size). The size is applied, added to the paper list and, in the application, kept in the user's preferences, so it is in the list after a restart. Choosing a saved size and opening the dialog again changes it; "Delete size" removes it. Embedding applications pass a `CustomPaperStore` to `BarcodeLabelWorkbench.setCustomPaperStore`; without one, the sizes last while the application runs.
- **The app opens the last design.** On closing, the application keeps the label being designed, with its paper size, in `%APPDATA%\Barcode Label Suite\last-design.label.json` (`~/.barcode-label-suite/` elsewhere) and opens it on the next run. The untouched starting label is not kept, and a file that cannot be read is ignored.
- **An application icon:** a barcode label on a blue tile, in the window's title bar and taskbar and on the packaged application. `scripts/AppIcon.java` draws every size from code, with simpler, pixel-aligned artwork for the small ones; `package.ps1` passes the `.ico` to jpackage.
- `README.ar.md`, an Arabic README, and `sample-products-quantities-en.csv`, the sample data in English. The README screenshots are in English, and `README.ar.md` has Arabic ones.

### Fixed
- Nothing could be typed into the designer's property fields in the application: after every mouse press, the designer gave the focus back to the selected element, so the field lost it before the first key. A press in a field or box one types into now leaves it the focus.
- English left in the Arabic UI: the undo and redo buttons ("تراجع: Move elements"), the names of new elements and of the empty template ("Rectangle", "New label"), the paper type and image fit lists (`SHEET`, `CONTAIN`), file type names and the OK/Cancel buttons of dialogs.
- Property names in the designer, the Browse button and the print pane's page-range captions were cut short ("الع…", "…") when the panels were narrow.

### بالعربية
- **جديد:** واجهة إنجليزية بجانب العربية، تختارها من قائمة اللغة ويتذكرها البرنامج. طباعة ملصق واحد بدون ملف CSV بكتابة بياناته في تبويب الطباعة، وعدد النسخ هو عدد الملصقات. في المصمّم، Enter يطبّق الخاصية، والنقر المزدوج على العنصر ينقلك لخانة نصه أو بيانات الباركود. ملف README بالعربية. لوحتا العناصر والخصائص يتغير عرضهما بالسحب ويمكن إخفاؤهما من شريط الحالة. المقاس المخصص يُحفظ باسم في قائمة المقاسات ويبقى بعد إغلاق البرنامج، ويمكن تعديله أو حذفه. البرنامج يفتح آخر تصميم كنت تعمل عليه، بمقاس ورقه، عند تشغيله من جديد. أيقونة للبرنامج: ملصق باركود على مربع أزرق، في شريط العنوان وشريط المهام وعلى البرنامج المثبّت.
- **أخطاء أُصلحت:** لم يكن ممكنًا الكتابة في خانات الخصائص في المصمّم لأن التركيز كان يعود للعنصر المحدد بعد كل نقرة. نصوص إنجليزية كانت تظهر في الواجهة العربية، مثل "تراجع: Move elements" وأسماء العناصر الجديدة وقوائم نوع الورق ونمط الصورة وأزرار الحوارات. وأسماء الخصائص التي كانت تُقص عند ضيق اللوحة.

## [0.7.1] - 2026-11-17

### Fixed
- **Designer text was smaller than it prints**, by about 29% at every zoom, and did not grow when zooming in: the canvas used the point size as a pixel size. Text is now scaled like the rest of the label, so it takes the same room on the canvas as on paper. Like the printout, it stays on one line, placed by its alignment and cut off at its frame, instead of wrapping or ending in "…": text too long for its frame shows its start, middle or end as on paper.
- **Text the label's font has no glyphs for printed as boxes**, for example Arabic in a Latin-only font such as Liberation Sans, while the designer showed it correctly in a substitute font. Those characters now print in a font that has them (Java's `Dialog` font), and the rest of the text keeps the chosen font.

### بالعربية
- **أخطاء أُصلحت:** النص في المصمّم كان أصغر من المطبوع بنحو 29% ولا يكبر مع التكبير، وأصبح بحجمه المطبوع. النص الأطول من إطاره يظهر الآن على سطر واحد ويُقص عند الإطار كما في الطباعة، فيظهر أوله أو وسطه أو آخره حسب المحاذاة. والحروف التي لا يدعمها الخط المختار، مثل العربية بخط لاتيني فقط، كانت تُطبع مربعات فارغة، وأصبحت تُطبع بخط بديل يدعمها.

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

### Added
- **Automatic CSV column suggestions** from column names: exact names, English and Arabic synonyms (`Name`/`الصنف`, `Code`/`Barcode`/`الكود`, `Retail Price`/`السعر`…) and shared words. The quantity column is picked too (`Qty`, `الكمية`, `عدد النسخ`…), and the screen asks the user to review the suggestion.
- Colours accept every form the designer does: the 148 colour names, `#RGB`, `#RRGGBB`, `#RRGGBBAA`, `rgb()`, `rgba()`, `hsl()`, `hsla()` (`com.mohamed.barcodelabel.model.color.LabelColors`). An invalid colour shows and prints as black.
- The print confirmation states the labels and pages that will print, the copies and the page range.
- `CHANGELOG.md`, an MIT `LICENSE`, and a release workflow that builds, tests, packages and publishes the Windows application when a `v*` tag is pushed.
- CI on Linux as well as Windows.
- Screenshots of the designer, the CSV screen and the print preview in the README, and a walkthrough with the sample CSV.

### Changed
- **One build:** every module builds and tests from the root `pom.xml` with `mvnw clean install`, at one shared version. `start.cmd`/`start.sh` build and start the app; `package.ps1` makes the Windows app image and takes its version from `pom.xml`.
- The application is `barcode-label-app`, main class `com.mohamed.barcodelabel.app.BarcodeLabelSuiteLauncher`, and the window is titled "Barcode Label Suite".
- The print screen numbers sheet slots from 1 instead of 0.
- Validation messages are in Arabic, configuration problems no longer claim to be in "row 1", and counts use Arabic number agreement ("10 ملصقات من 4 صفوف").
- 8-digit hex colours are read as `#RRGGBBAA` in print too, as the designer reads them (print used `#AARRGGBB`).
- `BatchValidationIssue` carries a `BatchIssueKind` and the offending value; the four-argument constructor still works.
- The model's value checks share one internal `Checks` class; their messages are uniform and name the value.
- `docs/INTEGRATION_AR.md` is rewritten for the current modules; the Arabic user guide moves to `docs/USER_GUIDE_AR.md`.
- Test coverage: 206 tests (from 57), and JaCoCo requires 70% line coverage in every module.

### Removed
- The milestone-1 `barcode-label-fx` and `barcode-label-demo` modules (use `barcode-label-fx-designer`'s `VisualLabelDesigner`), and the unused `barcode-label-fx-preview` and `barcode-label-fx-print` modules.
- The per-phase parent POMs, build folders and scripts, and `sample-products.csv`.

### بالعربية
- **أخطاء أُصلحت:** عناصر المصمّم لم تكن تظهر؛ المصمّم كان يعرض الملصق معكوسًا ويحرّك العناصر بالاتجاه الخطأ؛ "تطبيق الخصائص" كان يطبّق الموقع والحجم فقط؛ دفعة CSV قديمة كانت تبقى قابلة للطباعة بعد تغيير الملف أو المطابقة؛ المطابقة اليدوية كانت تضيع؛ قالب سليم في المصمّم قد يفشل في الطباعة بسبب أسماء الألوان.
- **جديد:** اقتراح تلقائي لربط أعمدة CSV بالحقول، بالعربي والإنجليزي، مع عمود الكمية؛ قبول كل صيغ الألوان في الطباعة؛ رسالة تأكيد طباعة تذكر عدد الملصقات والصفحات والنسخ؛ ترقيم الخانات من 1.
- **تغييرات:** بناء المشروع كله بأمر واحد؛ رسائل التحقق بالعربية وبصياغة أعداد صحيحة؛ حذف الموديولات غير المستخدمة؛ 206 اختبارات وحد تغطية 70%.

## [0.6.1] - 2026-11-17

The phase-6 batch-production application (CSV import, column mapping, per-row quantities, batch validation and printing) at the version set before 0.7.0's changes.
