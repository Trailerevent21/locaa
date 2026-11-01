#!/bin/bash
# "npx cap add android" komutundan SONRA calistir (depo kokunden): bash native/apply.sh
set -e
PKG=$(grep -o '"appId"[^,]*' capacitor.config.json | sed 's/.*: *"\(.*\)"/\1/')
DIR=android/app/src/main/java/$(echo "$PKG" | tr . /)
mkdir -p "$DIR"
for f in ScreenSharePlugin ScreenShareService; do sed "s/__PKG__/$PKG/g" native/$f.java > "$DIR/$f.java"; done
cat > "$DIR/MainActivity.java" <<JAVA
package $PKG;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;
public class MainActivity extends BridgeActivity {
  @Override public void onCreate(Bundle b) { registerPlugin(ScreenSharePlugin.class); super.onCreate(b); }
}
JAVA
M=android/app/src/main/AndroidManifest.xml
if ! grep -q ScreenShareService "$M"; then
  sed -i 's#<application#<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/><uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION"/><uses-permission android:name="android.permission.POST_NOTIFICATIONS"/><application#' "$M"
  sed -i 's#</application>#<service android:name=".ScreenShareService" android:exported="false" android:foregroundServiceType="mediaProjection"/></application>#' "$M"
fi
echo "Ekran paylasimi eklentisi eklendi: $PKG"
