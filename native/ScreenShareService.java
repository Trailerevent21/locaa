package __PKG__;

import android.app.*;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.*;

public class ScreenShareService extends Service {
  @Override public IBinder onBind(Intent i) { return null; }
  @Override public int onStartCommand(Intent i, int f, int id) {
    String ch = "screen_share";
    NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel(ch, "Ekran paylaşımı", NotificationManager.IMPORTANCE_LOW));
    Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, ch) : new Notification.Builder(this);
    Notification n = b.setContentTitle("Loca").setContentText("Ekranın paylaşılıyor").setSmallIcon(android.R.drawable.ic_menu_share).setOngoing(true).build();
    if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
    else startForeground(1, n);
    return START_NOT_STICKY;
  }
}
