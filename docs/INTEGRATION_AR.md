# دمج Barcode Label Suite في برنامج JavaFX

## 1. تثبيت المكتبة محليًا

من مجلد المشروع الرئيسي:

```bash
./mvnw clean install
```

على Windows استخدم `mvnw.cmd`. اختبارات الواجهة تحتاج شاشة؛ على Linux بدون شاشة شغّل الأمر عبر `xvfb-run -a`، أو أضف `-DskipTests`.

الإصدار الحالي هو `0.6.1`، وكل الموديولات تحمل نفس الإصدار.

## 2. اختر الموديول المناسب

| ما تريده | الموديول | الكلاس |
|---|---|---|
| الشاشة الكاملة: تصميم ومعاينة وطباعة ومطابقة CSV والكميات | `barcode-label-fx-batch` | `BatchProductionWorkbench` |
| التصميم والمعاينة والطباعة بدون شاشة مطابقة CSV | `barcode-label-fx-workbench` | `BarcodeLabelWorkbench` |
| معاينة وطباعة سجلات جاهزة فقط | `barcode-label-fx-workbench` | `UniversalPrintPane` |
| مصمم الملصق فقط | `barcode-label-fx-designer` | `VisualLabelDesigner` |
| حفظ وتحميل القوالب وربط الحقول، بدون واجهة | `barcode-label-data` | `TemplateJsonService`، `TemplateFieldResolver` |

كل موديول يجلب ما يحتاجه من الموديولات الأخرى تلقائيًا، فيكفي أن تضيف واحدًا منها. مثلًا للشاشة الكاملة:

```xml
<dependency>
    <groupId>com.mohamed.barcodelabel</groupId>
    <artifactId>barcode-label-fx-batch</artifactId>
    <version>0.6.1</version>
</dependency>
```

## 3. classpath أو module-info

موديولات الواجهة تعمل على الـ classpath، وهذا أبسط طريق. إذا كان برنامجك يستخدم `module-info.java`، فلكل موديول اسم ثابت في ملف الـ manifest، مثل:

```java
module my.application {
    requires com.mohamed.barcodelabel.fx.batch;
}
```

وأسماء الموديولات الأخرى: `com.mohamed.barcodelabel.fx.workbench` و`com.mohamed.barcodelabel.fx.designer` و`com.mohamed.barcodelabel.data`.

## 4. إدراج الواجهة

الشاشة الكاملة كما في البرنامج نفسه:

```java
BatchProductionWorkbench workbench = new BatchProductionWorkbench(template);
stage.setScene(new Scene(workbench, 1480, 940));
```

المصمم فقط، مع متابعة التعديلات وحفظ القالب:

```java
VisualLabelDesigner designer = new VisualLabelDesigner(template);
designer.templateProperty().addListener((obs, oldTemplate, edited) -> System.out.println(edited.name()));
mainPane.setCenter(designer);

designer.save(Path.of("product-label.json"));
```

معاينة وطباعة سجلات من برنامجك مباشرة، بدون CSV:

```java
UniversalPrintPane printPane = new UniversalPrintPane();
printPane.setTemplate(template);
printPane.setRecords(List.of(
        Map.of("product_name", "قلم أزرق", "sku", "SKU-000001", "price", "12.50"),
        Map.of("product_name", "دفتر 100 ورقة", "sku", "SKU-000002", "price", "35.00")));
mainPane.setCenter(printPane);
```

الواجهات مصممة بالعربية من اليمين لليسار، أما سطح الملصق في المصمم فيبقى من اليسار لليمين حتى يطابق الطباعة. البرنامج يستخدم مظهر AtlantaFX؛ لنفس الشكل أضف قبل إنشاء النافذة:

```java
Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
```

## 5. إنشاء قالب من الكود

```java
PageDefinition page = new PaperPresetRegistry().find("a4-3x7-63.5x38.1").orElseThrow().page();
TextElement name = new TextElement(ElementFrame.create("Product", new RectMm(4, 3, 55, 7), 0),
        "{{product_name}}", "Arial", 11, "#111827", true, false, TextAlignment.CENTER);
BarcodeElement barcode = new BarcodeElement(ElementFrame.create("Barcode", new RectMm(4, 11, 55.5, 19), 1),
        "{{sku}}", Symbology.CODE_128, 0.25, 2.5, true);
LabelTemplate template = LabelTemplate.create("قالب المنتجات", page, List.of(name, barcode));
```

القياسات كلها بالمليمتر. الحقول بين `{{` و`}}` تُملأ من بيانات كل منتج عند المعاينة والطباعة.

## 6. ربط بيانات منتج واحد بدون واجهة

```java
Map<String, Object> product = Map.of(
    "product_name", "قهوة عربية",
    "sku", "6221234567890",
    "price", "125.50"
);

LabelTemplate resolved = new TemplateFieldResolver().resolve(template, product, MissingFieldPolicy.FAIL);
```

## 7. حفظ القالب واستعادته

```java
TemplateJsonService json = new TemplateJsonService();
json.write(Path.of("product-label.json"), template);

LabelTemplate restored = json.read(Path.of("product-label.json"));
```
