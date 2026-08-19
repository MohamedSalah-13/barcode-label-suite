# المرحلة الخامسة — Barcode Label Workbench 0.5

هذه المرحلة تجمع الأدوات السابقة في مكوّن JavaFX واحد قابل للإضافة لأي برنامج:

- تصميم الملصق بصريًا وتحريك العناصر وتغيير أحجامها.
- فتح القوالب من JSON وحفظها وحفظ نسخة جديدة.
- حفظ آمن باستعمال ملف مؤقت ثم استبدال الملف النهائي.
- 7 مقاسات جاهزة لأوراق A4 والرولات الحرارية.
- إنشاء مقاس مخصص بالمليمتر (Sheet أو Roll أو Single).
- استيراد CSV وTSV بترميز UTF-8، ودعم العربية والقيم المقتبسة والأسطر داخل الحقول.
- عرض البيانات في جدول قبل الطباعة.
- معاينة وطباعة A4 والرول والملصق المفرد.
- تنفيذ قراءة الملفات والرسم والطباعة في خيوط خلفية حتى لا تتوقف الواجهة.

## التشغيل

من PowerShell داخل مجلد المشروع:

```powershell
cd C:\Users\Mohamed\Documents\Codex\barcode-label-suite
.\start-phase5.cmd
```

بعد فتح البرنامج:

1. عدّل القالب من تبويب **تصميم الملصق**.
2. اختر مقاسًا جاهزًا واضغط **تطبيق المقاس**، أو افتح **مقاس مخصص**.
3. اضغط **استيراد CSV** واختر `sample-products.csv`.
4. راجع السجلات في تبويب **بيانات الطباعة**.
5. افتح **المعاينة والطباعة**، اختر الطابعة ثم اطبع.

## إضافة المكتبة إلى مشروع JavaFX آخر

بعد تشغيل `mvn -f phase5-pom.xml install` مرة واحدة، أضف:

```xml
<dependency>
    <groupId>com.mohamed.barcodelabel</groupId>
    <artifactId>barcode-label-fx-workbench</artifactId>
    <version>0.5.0-SNAPSHOT</version>
</dependency>
```

ثم أنشئ المكوّن وأضفه إلى أي `Pane`:

```java
LabelTemplate template = createYourTemplate();
BarcodeLabelWorkbench workbench = new BarcodeLabelWorkbench(template);
root.setCenter(workbench);
```

للوصول إلى الأجزاء منفردة:

```java
LabelTemplate current = workbench.getTemplate();
VisualLabelDesigner designer = workbench.designer();
UniversalPrintPane printing = workbench.printPane();
PaperPresetRegistry presets = workbench.paperPresets();
```

يمكن استخدام وحدة الخدمات بدون JavaFX أيضًا:

```xml
<dependency>
    <groupId>com.mohamed.barcodelabel</groupId>
    <artifactId>barcode-label-workflow</artifactId>
    <version>0.5.0-SNAPSHOT</version>
</dependency>
```

وتحتوي على `CsvImportService` و`TemplateDocuments` و`PaperPresetRegistry` و`UniversalPagePlanner`.

## ملاحظات مهمة للطباعة

- يجب تعريف مقاس الرول نفسه في إعدادات تعريف الطابعة على Windows؛ يرسل البرنامج مقاس الصفحة المطلوب، لكن بعض تعريفات الطابعات لا تقبل مقاسًا غير مسجل لديها.
- اختبر المعاينة والطباعة على ورقة عادية أولًا، ثم عاير الهوامش والفجوات حسب الطابعة الفعلية.
- لا يفترض البرنامج أن كل ملفات CSV تستخدم الفاصلة الإنجليزية؛ يكتشف الفاصلة أو الفاصلة المنقوطة أو Tab تلقائيًا.
- أسماء الأعمدة في CSV يجب أن تطابق الحقول داخل القالب، مثل `{{product_name}}` و`{{sku}}` و`{{price}}`.
