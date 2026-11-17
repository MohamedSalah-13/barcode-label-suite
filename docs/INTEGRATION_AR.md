# دمج Barcode Label Suite في مشروع JavaFX

## 1. تثبيت المكتبة محليًا

من مجلد المشروع الرئيسي:

```powershell
mvn clean install
```

الإصدار الحالي هو `0.6.1`.

## 2. إضافة الوحدات إلى `pom.xml`

لإدراج مصمم الملصقات داخل مشروع JavaFX:

```xml
<dependency>
    <groupId>com.mohamed.barcodelabel</groupId>
    <artifactId>barcode-label-fx-designer</artifactId>
    <version>0.6.1</version>
</dependency>
```

للحفظ والتحميل وربط حقول البيانات:

```xml
<dependency>
    <groupId>com.mohamed.barcodelabel</groupId>
    <artifactId>barcode-label-data</artifactId>
    <version>0.6.1</version>
</dependency>
```

وللطباعة وتخطيط صفحات الملصقات:

```xml
<dependency>
    <groupId>com.mohamed.barcodelabel</groupId>
    <artifactId>barcode-label-print</artifactId>
    <version>0.6.1</version>
</dependency>
```

## 3. إعداد `module-info.java`

```java
module my.application {
    requires com.mohamed.barcodelabel.fx.designer;
    requires com.mohamed.barcodelabel.data;
    requires com.mohamed.barcodelabel.print;
}
```

`barcode-label-fx-designer` يجلب `barcode-label-model` و`barcode-label-data` وJavaFX Controls كاعتماديات Maven بشكل transitive، فلا تحتاج لإضافتها إلى `pom.xml` إلا إذا كان مشروعك يستخدمها مباشرة لأسباب أخرى.

## 4. إدراج المصمم في الواجهة

```java
VisualLabelDesigner designer = new VisualLabelDesigner();
designer.setTemplate(template);
mainPane.setCenter(designer);
```

## 5. ربط بيانات المنتج

```java
Map<String, Object> product = Map.of(
    "product_name", "قهوة عربية",
    "sku", "6221234567890",
    "price", "125.50"
);

LabelTemplate resolved = new TemplateFieldResolver().resolve(
    template,
    product,
    MissingFieldPolicy.FAIL
);
```

القالب يمكنه استخدام الحقول التالية مثلًا:

```text
{{product_name}}
{{sku}}
السعر: {{price}} ج.م
```

## 6. حفظ القالب واستعادته

```java
TemplateJsonService json = new TemplateJsonService();
json.write(Path.of("product-label.json"), template);

LabelTemplate restored = json.read(Path.of("product-label.json"));
```

## ملاحظة المرحلة الحالية

المعاينة، نموذج القالب، ربط البيانات وتخطيط الورقة جاهزة. توليد صورة الباركود الفعلية والطباعة عبر `Graphics2D` سيضافان في المرحلة التالية خلف العقود الموجودة في وحدتي `render` و`print`.

