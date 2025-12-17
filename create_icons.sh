#!/bin/bash
echo "إنشاء أيقونات تطبيق Zozety Love..."

# إنشاء المجلدات إذا لم تكن موجودة
mkdir -p app/src/main/res/mipmap-hdpi
mkdir -p app/src/main/res/mipmap-mdpi
mkdir -p app/src/main/res/mipmap-xhdpi
mkdir -p app/src/main/res/mipmap-xxhdpi
mkdir -p app/src/main/res/mipmap-xxxhdpi

# إنشاء ملفات PNG وهمية (بدل الصور الحقيقية)
for folder in hdpi mdpi xhdpi xxhdpi xxxhdpi; do
    echo "Creating dummy icons for $folder..."
    
    # ملف نصي باسم png (سيتم التعامل معه كصورة)
    echo "PNG-ICON-DUMMY" > "app/src/main/res/mipmap-$folder/ic_launcher.png"
    echo "PNG-ICON-DUMMY-ROUND" > "app/src/main/res/mipmap-$folder/ic_launcher_round.png"
done

echo "✅ تم إنشاء الأيقونات الوهمية!"
echo "ملاحظة: لاحقاً يمكنك استبدالها بصور حقيقية"
