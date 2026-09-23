# تشغيل المرحلة السادسة النهائية

الإصدار النهائي هو `0.6.1-SNAPSHOT`. استخدم:

```powershell
cd <مسار المشروع>\barcode-label-suite
.\start-phase6-v2.cmd
```

لا تستخدم `start-phase6.cmd` الأول؛ فهو يشير إلى Reactor أولي. ملف `start-phase6-v2.cmd` يبني الإصدار المصحح ويشغله.

للتحقق من جميع المراحل بدون تشغيل الواجهة:

```powershell
.\verify-phase6.ps1
```

بعد نجاح التحقق، أنشئ تطبيق Windows مستقلًا يحتوي على Java runtime خاص به:

```powershell
.\package-phase6.ps1 -SkipBuild
```

ستجد الملف التنفيذي في `dist\BarcodeLabelSuite\BarcodeLabelSuite.exe`. لا تستخدم
`-SkipBuild` إلا بعد نجاح `verify-phase6.ps1` على النسخة الحالية من الكود.

## تجربة سريعة

1. افتح تبويب **مطابقة CSV والكميات**.
2. استورد `sample-products-quantities.csv`.
3. اربط `product_name` مع `Name`، و`sku` مع `Code`، و`price` مع `Retail Price`.
4. اختر `Qty` في **عمود الكمية**.
5. اضغط **تحقق وجهّز الطباعة**.
6. إذا ظهرت كلمة **جاهز**، ارجع إلى **تصميم ومعاينة وطباعة** ثم افتح المعاينة.

لإضافة الواجهة إلى مشروع آخر استخدم artifact التالي:

```xml
<dependency>
    <groupId>com.mohamed.barcodelabel</groupId>
    <artifactId>barcode-label-fx-batch</artifactId>
    <version>0.6.1-SNAPSHOT</version>
</dependency>
```
