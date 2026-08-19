# دمج Barcode Label Suite في مشروع JavaFX

## 1. تثبيت المكتبة محليًا

من مجلد المشروع الرئيسي:

```powershell
mvn clean install
```

الإصدار الحالي للتطوير هو `0.1.0-SNAPSHOT`.

## 2. إضافة الوحدات إلى `pom.xml`

لإدراج عنصر المعاينة/المصمم داخل مشروع JavaFX:

```xml
<dependency>
    <groupId>com.mohamed.barcodelabel</groupId>
    <artifactId>barcode-label-fx</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

للحفظ والتحميل وربط حقول البيانات:

```xml
<dependency>
    <groupId>com.mohamed.barcodelabel</groupId>
    <artifactId>barcode-label-data</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

وللطباعة وتخطيط صفحات الملصقات:

```xml
<dependency>
    <groupId>com.mohamed.barcodelabel</groupId>
    <artifactId>barcode-label-print</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## 3. إعداد `module-info.java`

```java
module my.application {
    requires com.mohamed.barcodelabel.fx;
    requires com.mohamed.barcodelabel.data;
    requires com.mohamed.barcodelabel.print;
}
```

`barcode-label-fx` يوفّر `barcode-label-model` وJavaFX Controls بشكل transitive، فلا تحتاج لتكرارهما إلا إذا كان مشروعك يستخدمهما مباشرة لأسباب أخرى.

## 4. إدراج المعاينة في الواجهة

```java
LabelDesigner designer = new LabelDesigner();
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

