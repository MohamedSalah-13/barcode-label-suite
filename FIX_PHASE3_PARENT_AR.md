# إصلاح اعتماد الأب في المرحلة الثالثة

سبب الخطأ أن حزمة `barcode-label-fx-print` ترث إعداداتها من:

```text
com.mohamed.barcodelabel:barcode-label-phase3:0.3.0-SNAPSHOT
```

وكان مشغل المرحلة يثبت الحزم الفرعية دون تثبيت ملف POM الأب في Maven Local.

استخدم المشغل المصحح:

```powershell
.\start-phase3-v2.cmd
```

ينفذ المشغل ثلاث خطوات بالترتيب:

1. تثبيت POM الأب فقط باستخدام `mvn -N install`.
2. بناء وتثبيت مكتبات المرحلة الثالثة.
3. تشغيل تطبيق JavaFX التجريبي.

