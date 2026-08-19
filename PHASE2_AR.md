# المرحلة الثانية: الرسم الحقيقي للباركود

تضيف هذه المرحلة ثلاث وحدات فوق الأساس العامل:

- `barcode-label-render-java2d`: رسم Java2D عالي الدقة وتوليد الباركود.
- `barcode-label-fx-preview`: عنصر معاينة JavaFX قابل للدمج.
- `barcode-label-demo-v2`: تطبيق تجريبي يعرض باركودًا حقيقيًا.

## التشغيل

```powershell
.\start-phase2-demo.cmd
```

## البناء والاختبار

```powershell
mvn -f .\phase2-pom.xml clean verify
```

تستخدم الباركودات الخطية Barcode4J، بينما تستخدم QR وData Matrix وPDF417 وAztec مكتبة ZXing.
تُنشأ المعاينة في Virtual Thread حتى لا تتجمد واجهة JavaFX.

