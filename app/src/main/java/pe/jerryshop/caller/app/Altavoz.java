package pe.jerryshop.caller.app;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import pe.jerryshop.caller.core.Rtp;

/** Escuchar la llamada en vivo por el parlante o audífonos del celular (mezcla Vanessa + cliente). */
public final class Altavoz implements Rtp.AudioSink {

    private static final int MAX = 8000; // 1 s: si se acumula más, se descarta para seguir "en vivo"
    private final AudioTrack track;
    private final short[] cli = new short[MAX * 2], van = new short[MAX * 2];
    private int nCli, nVan;
    private volatile boolean activo = true;

    public Altavoz() {
        int min = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        track = new AudioTrack(
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
                new AudioFormat.Builder().setSampleRate(8000).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                Math.max(min, 3200), AudioTrack.MODE_STREAM, 0);
        track.play();
        Thread t = new Thread(this::bucle, "altavoz");
        t.start();
    }

    private synchronized void agregar(short[] dst, boolean esCliente, short[] pcm) {
        int n = esCliente ? nCli : nVan;
        if (n + pcm.length > dst.length) { // demasiado retraso: quedarse con lo último
            int quitar = n + pcm.length - MAX;
            System.arraycopy(dst, quitar, dst, 0, n - quitar);
            n -= quitar;
        }
        System.arraycopy(pcm, 0, dst, n, pcm.length);
        n += pcm.length;
        if (esCliente) nCli = n; else nVan = n;
        notifyAll();
    }

    @Override public void cliente(short[] pcm) { agregar(cli, true, pcm); }
    @Override public void vanessa(short[] pcm) { agregar(van, false, pcm); }

    private void bucle() {
        short[] mezcla = new short[160];
        while (activo) {
            synchronized (this) {
                while (activo && nCli < 160 && nVan < 160) {
                    try { wait(40); } catch (InterruptedException e) { return; }
                    if (nCli > 0 || nVan > 0) break;
                }
                if (!activo) break;
                for (int i = 0; i < 160; i++) {
                    int a = i < nCli ? cli[i] : 0, b = i < nVan ? van[i] : 0;
                    int m = (int) (a * 0.75 + b * 0.75);
                    mezcla[i] = (short) Math.max(-32768, Math.min(32767, m));
                }
                int qc = Math.min(160, nCli), qv = Math.min(160, nVan);
                System.arraycopy(cli, qc, cli, 0, nCli - qc); nCli -= qc;
                System.arraycopy(van, qv, van, 0, nVan - qv); nVan -= qv;
            }
            track.write(mezcla, 0, 160);
        }
    }

    public void cerrar() {
        activo = false;
        synchronized (this) { notifyAll(); }
        try { track.stop(); track.release(); } catch (Exception ignored) {}
    }
}
