package __PKG__;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Base64;
import android.util.DisplayMetrics;
import androidx.activity.result.ActivityResult;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.ByteArrayOutputStream;

@CapacitorPlugin(name = "ScreenShare")
public class ScreenSharePlugin extends Plugin {
  private MediaProjection mp;
  private VirtualDisplay vd;
  private ImageReader ir;
  private HandlerThread ht;
  private long last = 0;

  @PluginMethod
  public void start(PluginCall call) {
    MediaProjectionManager m = (MediaProjectionManager) getContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    startActivityForResult(call, m.createScreenCaptureIntent(), "onPerm");
  }

  @ActivityCallback
  private void onPerm(PluginCall call, ActivityResult r) {
    if (call == null) return;
    if (r.getResultCode() != Activity.RESULT_OK || r.getData() == null) { call.reject("denied"); return; }
    final Context c = getContext();
    ContextCompat.startForegroundService(c, new Intent(c, ScreenShareService.class));
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
      try {
        MediaProjectionManager m = (MediaProjectionManager) c.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mp = m.getMediaProjection(r.getResultCode(), r.getData());
        mp.registerCallback(new MediaProjection.Callback() {
          @Override public void onStop() { cleanup(); notifyListeners("stopped", new JSObject()); }
        }, new Handler(Looper.getMainLooper()));
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        float sc = Math.min(1f, 960f / Math.max(dm.widthPixels, dm.heightPixels));
        final int w = Math.max(2, (int) (dm.widthPixels * sc)), h = Math.max(2, (int) (dm.heightPixels * sc));
        ht = new HandlerThread("loca-ss"); ht.start();
        ir = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2);
        vd = mp.createVirtualDisplay("loca", w, h, dm.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, ir.getSurface(), null, null);
        ir.setOnImageAvailableListener(rd -> {
          Image im = rd.acquireLatestImage();
          if (im == null) return;
          try {
            long now = SystemClock.uptimeMillis();
            if (now - last < 110) return;
            last = now;
            Image.Plane p = im.getPlanes()[0];
            int rs = p.getRowStride(), ps = p.getPixelStride();
            Bitmap b = Bitmap.createBitmap(rs / ps, h, Bitmap.Config.ARGB_8888);
            b.copyPixelsFromBuffer(p.getBuffer());
            Bitmap crop = Bitmap.createBitmap(b, 0, 0, w, h);
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            crop.compress(Bitmap.CompressFormat.JPEG, 55, o);
            JSObject js = new JSObject();
            js.put("d", Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP));
            js.put("w", w); js.put("h", h);
            notifyListeners("frame", js);
          } finally { im.close(); }
        }, new Handler(ht.getLooper()));
        call.resolve();
      } catch (Exception e) { call.reject(String.valueOf(e.getMessage())); }
    }, 400);
  }

  private void cleanup() {
    try { if (vd != null) vd.release(); } catch (Exception e) {}
    try { if (ir != null) ir.close(); } catch (Exception e) {}
    try { if (ht != null) ht.quitSafely(); } catch (Exception e) {}
    vd = null; ir = null; ht = null;
    MediaProjection p = mp; mp = null;
    try { if (p != null) p.stop(); } catch (Exception e) {}
    Context c = getContext();
    c.stopService(new Intent(c, ScreenShareService.class));
  }

  @PluginMethod
  public void stop(PluginCall call) { cleanup(); call.resolve(); }
}
