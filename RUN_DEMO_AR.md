# تشغيل التطبيق التجريبي

الطريقة الأسهل من PowerShell داخل مجلد المشروع:

```powershell
.\run-demo.ps1
```

أو شغّل Maven يدويًا:

```powershell
mvn install -DskipTests
mvn -f .\barcode-label-demo\pom.xml org.openjfx:javafx-maven-plugin:0.0.8:run
```

لا تستخدم الأمر التالي من ملف POM الرئيسي:

```powershell
mvn -pl barcode-label-demo -am javafx:run
```

لأن اختصار الإضافة `javafx` معرّف في وحدة `barcode-label-demo` وليس في ملف POM التجميعي الرئيسي.

