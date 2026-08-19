# المرحلة الرابعة: المصمم المرئي

## الخصائص المنفذة

- إضافة نص وCode 128 وQR ومستطيل.
- عرض باركود حقيقي داخل المصمم.
- تحديد مفرد ومتعدد باستخدام `Ctrl + Click`.
- تحريك العناصر بالماوس.
- تغيير الحجم من المقبض السفلي الأيمن.
- الالتصاق بشبكة 1 مم.
- تحريك دقيق بالأسهم: 0.1 مم، ومع Shift: 1 مم.
- محاذاة العناصر أفقيًا ورأسيًا.
- تكرار وحذف العناصر.
- تعديل X وY والعرض والارتفاع والمحتوى.
- قفل وإخفاء العناصر.
- Undo/Redo مع `Ctrl+Z` و`Ctrl+Y`.
- تكبير وتصغير مساحة التصميم.
- انتقال مباشر من التصميم إلى معاينة A4 والطباعة.

## التشغيل

```powershell
.\start-phase4.cmd
```

## دمج المكتبة

```xml
<dependency>
    <groupId>com.mohamed.barcodelabel</groupId>
    <artifactId>barcode-label-fx-designer</artifactId>
    <version>0.4.0-SNAPSHOT</version>
</dependency>
```

```java
LabelDesignerPane designer = new LabelDesignerPane(template);
root.setCenter(designer);
LabelTemplate changedTemplate = designer.getTemplate();
```

