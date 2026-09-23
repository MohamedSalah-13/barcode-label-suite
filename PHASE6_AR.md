# المرحلة السادسة — مطابقة CSV والكميات

أضيفت طبقة إنتاج جماعي فوق Workbench المصحح، بدون تغيير محرك التصميم أو الطباعة المستقر.

## الوظائف الجديدة

- اكتشاف الحقول المستخدمة فعليًا داخل النصوص والباركود، مثل `{{product_name}}`.
- مطابقة كل حقل مع أي عمود في CSV حتى لو اختلف اسمه.
- مطابقة تلقائية تتجاهل حالة الأحرف والمسافات و`_` و`-`.
- اختيار عمود للكمية، أو استخدام كمية افتراضية لكل صف.
- توسيع الصف إلى عدد الملصقات المطلوب قبل المعاينة.
- إيقاف الدفعة بالكامل عند وجود حقل غير مربوط أو كمية غير صالحة؛ لا توجد طباعة جزئية صامتة.
- حد أقصى 999 نسخة للصف و100,000 ملصق للدفعة كحد أمان افتراضي.
- حفظ مطابقة الأعمدة في ملف `.properties` وفتحها لاحقًا.
- تنفيذ فتح CSV وملفات المطابقة خارج JavaFX Application Thread.

## التشغيل

```powershell
cd <مسار المشروع>\barcode-label-suite
.\start-phase6-v2.cmd
```

> لا تستخدم `start-phase6.cmd`؛ فهو يشير إلى Reactor أولي غير مصحح. الإصدار
> النهائي الحالي هو `0.6.1-SNAPSHOT` ويُبنى عبر `start-phase6-v2.cmd`.

للتحقق الكامل بدون تشغيل الواجهة:

```powershell
.\verify-phase6.ps1
```

ولإنشاء حزمة Windows مستقلة بعد نجاح التحقق:

```powershell
.\package-phase6.ps1
```

استخدم الملف التجريبي `sample-products-quantities.csv`، ثم اربط الحقول كالتالي:

| حقل القالب | عمود CSV |
|---|---|
| `product_name` | `Name` |
| `sku` | `Code` |
| `price` | `Retail Price` |

واختر `Qty` كعمود الكمية، ثم اضغط **تحقق وجهّز الطباعة**. عند نجاح التحقق تُرسل السجلات الموسعة تلقائيًا إلى Workbench في تبويب **تصميم ومعاينة وطباعة**.

## إضافة الوحدة إلى مشروع آخر

```xml
<dependency>
    <groupId>com.mohamed.barcodelabel</groupId>
    <artifactId>barcode-label-fx-batch</artifactId>
    <version>0.6.1-SNAPSHOT</version>
</dependency>
```

```java
BatchProductionWorkbench workbench = new BatchProductionWorkbench(template);
root.setCenter(workbench);
```

لاستخدام المحرك بدون JavaFX:

```xml
<dependency>
    <groupId>com.mohamed.barcodelabel</groupId>
    <artifactId>barcode-label-batch</artifactId>
    <version>0.6.1-SNAPSHOT</version>
</dependency>
```
