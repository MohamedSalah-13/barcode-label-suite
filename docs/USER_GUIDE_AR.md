# تشغيل البرنامج

الإصدار الحالي هو `0.6.1-SNAPSHOT`. كل الموديولات تُبنى من ملف `pom.xml` الرئيسي بأمر واحد.

لبناء المشروع وتشغيل الاختبارات ثم فتح البرنامج:

```powershell
cd <مسار المشروع>\barcode-label-suite
.\start.cmd
```

على Linux أو macOS استخدم `./start.sh`.

للبناء والاختبار فقط بدون فتح الواجهة:

```powershell
.\mvnw.cmd clean install
```

بعد نجاح البناء، أنشئ تطبيق Windows مستقلًا يحتوي على Java runtime خاص به:

```powershell
.\package.ps1 -SkipBuild
```

ستجد الملف التنفيذي في `dist\BarcodeLabelSuite\BarcodeLabelSuite.exe`. لا تستخدم
`-SkipBuild` إلا بعد نجاح `mvnw.cmd clean install` على النسخة الحالية من الكود.

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
