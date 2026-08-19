# تشغيل المرحلة السادسة النهائية

الإصدار النهائي هو `0.6.1-SNAPSHOT`. استخدم:

```powershell
cd C:\Users\Mohamed\Documents\Codex\barcode-label-suite
.\start-phase6-v2.cmd
```

لا تستخدم `start-phase6.cmd` الأول؛ فهو يشير إلى Reactor أولي. ملف `start-phase6-v2.cmd` يبني الإصدار المصحح ويشغله.

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
