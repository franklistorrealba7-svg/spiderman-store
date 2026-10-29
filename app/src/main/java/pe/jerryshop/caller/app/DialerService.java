package pe.jerryshop.caller.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

/**
 * Servicio en primer plano: mantiene vivas las llamadas con la pantalla apagada
 * (wake lock + Wi-Fi en alto rendimiento) y muestra el avance en la notificación.
 */
public final class DialerService extends Service {

    private static final String CANAL = "campana";
    private static final int ID = 7;
    private static final String DETENER = "detener";

    private PowerManager.WakeLock wake;
    private WifiManager.WifiLock wifi;
    private ConnectivityManager.NetworkCallback redCb;
    private final Handler h = new Handler(Looper.getMainLooper());
    private Network ultimaRed;
    private long ultimoGuardado;
    private static volatile boolean activo;

    /** Arranca el servicio si hay campaña/prueba en curso; lo detiene si no. */
    public static void actualizar(Context c) {
        App app = App.get();
        if (app == null) return;
        if (activo || !app.engine.corriendo()) return; // ya está corriendo: no volver a arrancarlo desde segundo plano
        Intent i = new Intent(c, DialerService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
            activo = true;
        } catch (Exception e) {
            app.engine.log("⚠ Android no dejó iniciar el modo segundo plano: abre la app y vuelve a tocar Iniciar.");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && DETENER.equals(intent.getAction())) {
            App.get().engine.detener();
        }
        activo = true;
        crearCanal();
        Notification n = notificacion();
        if (Build.VERSION.SDK_INT >= 29) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else startForeground(ID, n);
        if (wake == null) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jerrycaller:llamadas");
            wake.setReferenceCounted(false);
            wake.acquire(12 * 60 * 60 * 1000L);
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm != null) {
                int modo = Build.VERSION.SDK_INT >= 29 ? WifiManager.WIFI_MODE_FULL_LOW_LATENCY : WifiManager.WIFI_MODE_FULL_HIGH_PERF;
                wifi = wm.createWifiLock(modo, "jerrycaller:wifi");
                wifi.setReferenceCounted(false);
                wifi.acquire();
            }
            vigilarRed();
            h.post(tick);
        }
        return START_NOT_STICKY;
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            App app = App.get();
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.notify(ID, notificacion());
            if (System.currentTimeMillis() - ultimoGuardado > 10_000) { app.guardarClientes(); ultimoGuardado = System.currentTimeMillis(); }
            if (!app.engine.corriendo()) {
                app.guardarClientes();
                stopForeground(true);
                stopSelf();
                return;
            }
            h.postDelayed(this, 3000);
        }
    };

    /** Si el celular pasa de Wi-Fi a datos (o al revés), re-registrar con la nueva IP. */
    private void vigilarRed() {
        if (Build.VERSION.SDK_INT < 24) return;
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        redCb = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network network) {
                if (ultimaRed != null && !network.equals(ultimaRed)) App.get().engine.redCambio();
                ultimaRed = network;
            }
            @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
                if (ultimaRed == null) ultimaRed = network;
            }
        };
        try { cm.registerDefaultNetworkCallback(redCb); } catch (Exception ignored) { redCb = null; }
    }

    private void crearCanal() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CANAL, "Campaña de llamadas", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    private Notification notificacion() {
        App app = App.get();
        String titulo = app.engine.esPrueba() ? "Llamada de prueba en curso" : "Campaña de llamadas en curso";
        if (!app.engine.conectado()) titulo += " · conectando…";
        int flags = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent abrir = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), flags);
        PendingIntent parar = PendingIntent.getService(this, 1, new Intent(this, DialerService.class).setAction(DETENER), flags);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CANAL) : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.stat_sys_phone_call)
         .setContentTitle(titulo)
         .setContentText(app.engine.resumen())
         .setStyle(new Notification.BigTextStyle().bigText(app.engine.resumen()))
         .setOngoing(true)
         .setOnlyAlertOnce(true)
         .setContentIntent(abrir)
         .addAction(new Notification.Action.Builder(null, "Detener", parar).build());
        return b.build();
    }

    @Override
    public void onDestroy() {
        activo = false;
        h.removeCallbacks(tick);
        if (wake != null && wake.isHeld()) wake.release();
        if (wifi != null && wifi.isHeld()) wifi.release();
        if (redCb != null) {
            try { ((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE)).unregisterNetworkCallback(redCb); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
