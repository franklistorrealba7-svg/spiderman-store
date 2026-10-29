package pe.jerryshop.caller.core;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.Random;

/** Audio de una llamada (RTP, G.711 8 kHz, paquetes de 20 ms). */
public final class Rtp {

    /** Para escuchar la llamada en vivo. */
    public interface AudioSink {
        void cliente(short[] pcm);
        void vanessa(short[] pcm);
    }

    public static final int FRAME = 160; // 20 ms a 8 kHz

    private final DatagramSocket sock;
    private volatile InetSocketAddress remote;
    private volatile int pt = 0; // 0 = PCMU, 8 = PCMA
    private volatile boolean running;
    private volatile AudioSink monitor;
    private volatile boolean latched;

    // salida (μ-law)
    private final Object outLock = new Object();
    private byte[] out = new byte[8000 * 10];
    private int outR, outW;

    // entrada (PCM 16 bits por cuadros de 20 ms)
    private final ArrayDeque<short[]> in = new ArrayDeque<>();
    private long recibidos;

    private Thread tx, rx;

    public Rtp() throws IOException {
        DatagramSocket s = null;
        Random r = new Random();
        for (int i = 0; i < 40 && s == null; i++) { // puerto par entre 10000 y 20000
            try { s = new DatagramSocket(10000 + 2 * r.nextInt(5000)); } catch (IOException ignored) {}
        }
        sock = s != null ? s : new DatagramSocket();
        sock.setSoTimeout(200);
    }

    public int localPort() { return sock.getLocalPort(); }

    public void setRemote(String host, int port, int payloadType) {
        if (!latched) remote = new InetSocketAddress(host, port);
        pt = payloadType == 8 ? 8 : 0;
    }

    public int payloadType() { return pt; }
    public long paquetesRecibidos() { return recibidos; }

    public void setMonitor(AudioSink m) { monitor = m; }

    public void start() {
        if (running) return;
        running = true;
        tx = new Thread(this::enviar, "rtp-tx");
        rx = new Thread(this::recibir, "rtp-rx");
        tx.setPriority(Thread.MAX_PRIORITY);
        tx.start();
        rx.start();
    }

    public void stop() {
        running = false;
        sock.close();
    }

    // ---------------- salida ----------------
    public void write(byte[] ulaw) { write(ulaw, 0, ulaw.length); }

    public void write(byte[] ulaw, int off, int len) {
        synchronized (outLock) {
            int pend = outW - outR;
            if (outW + len > out.length) {
                if (pend + len > out.length) {
                    byte[] n = new byte[Math.max(out.length * 2, pend + len)];
                    System.arraycopy(out, outR, n, 0, pend);
                    out = n;
                } else {
                    System.arraycopy(out, outR, out, 0, pend);
                }
                outR = 0;
                outW = pend;
            }
            System.arraycopy(ulaw, off, out, outW, len);
            outW += len;
        }
    }

    /** Bytes de audio que aún no salieron (8000 = 1 segundo). */
    public int pendiente() { synchronized (outLock) { return outW - outR; } }

    public void limpiarSalida() { synchronized (outLock) { outR = outW = 0; } }

    private void enviar() {
        byte[] pkt = new byte[12 + FRAME];
        byte[] frame = new byte[FRAME];
        byte[] alaw = new byte[FRAME];
        Random r = new Random();
        int seq = r.nextInt(65536);
        long ts = r.nextInt(1 << 30);
        int ssrc = r.nextInt();
        boolean primero = true;
        long siguiente = System.nanoTime();
        while (running) {
            int n;
            synchronized (outLock) {
                n = Math.min(FRAME, outW - outR);
                System.arraycopy(out, outR, frame, 0, n);
                outR += n;
            }
            for (int i = n; i < FRAME; i++) frame[i] = G711.ULAW_SILENCE;
            AudioSink m = monitor;
            if (m != null && n > 0) m.vanessa(G711.decode(frame, 0, FRAME, 0));
            if (pt == 8) G711.ulawToAlaw(frame, alaw, FRAME);
            int p = pt;
            pkt[0] = (byte) 0x80;
            pkt[1] = (byte) (p | (primero ? 0x80 : 0));
            pkt[2] = (byte) (seq >> 8); pkt[3] = (byte) seq;
            pkt[4] = (byte) (ts >> 24); pkt[5] = (byte) (ts >> 16); pkt[6] = (byte) (ts >> 8); pkt[7] = (byte) ts;
            pkt[8] = (byte) (ssrc >> 24); pkt[9] = (byte) (ssrc >> 16); pkt[10] = (byte) (ssrc >> 8); pkt[11] = (byte) ssrc;
            System.arraycopy(p == 8 ? alaw : frame, 0, pkt, 12, FRAME);
            InetSocketAddress dst = remote;
            if (dst != null) {
                try { sock.send(new DatagramPacket(pkt, pkt.length, dst)); } catch (IOException ignored) {}
            }
            primero = false;
            seq = (seq + 1) & 0xFFFF;
            ts = (ts + FRAME) & 0xFFFFFFFFL;
            siguiente += 20_000_000L;
            long espera = siguiente - System.nanoTime();
            if (espera > 0) {
                try { Thread.sleep(espera / 1_000_000L, (int) (espera % 1_000_000L)); } catch (InterruptedException e) { break; }
            } else if (espera < -200_000_000L) {
                siguiente = System.nanoTime(); // se atrasó mucho: no recuperar de golpe
            }
        }
    }

    // ---------------- entrada ----------------
    private void recibir() {
        byte[] buf = new byte[2048];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        while (running) {
            try {
                sock.receive(dp);
            } catch (SocketTimeoutException e) {
                continue;
            } catch (IOException e) {
                break;
            }
            int len = dp.getLength();
            if (len < 12 || (buf[0] & 0xC0) != 0x80) continue;
            int p = buf[1] & 0x7F;
            if (p != 0 && p != 8) continue; // DTMF, ruido de confort, etc.
            int off = 12 + 4 * (buf[0] & 0x0F);
            if ((buf[0] & 0x10) != 0 && len >= off + 4) off += 4 + 4 * (((buf[off + 2] & 0xFF) << 8) | (buf[off + 3] & 0xFF));
            int padding = (buf[0] & 0x20) != 0 ? buf[len - 1] & 0xFF : 0;
            int n = len - off - padding;
            if (n <= 0) continue;
            // RTP simétrico: responder a la dirección real de donde llega el audio (atraviesa NAT)
            InetSocketAddress src = (InetSocketAddress) dp.getSocketAddress();
            if (!latched || !src.equals(remote)) { remote = src; latched = true; }
            short[] pcm = G711.decode(buf, off, n, p);
            recibidos++;
            AudioSink m = monitor;
            if (m != null) m.cliente(pcm);
            synchronized (in) {
                in.addLast(pcm);
                while (in.size() > 500) in.removeFirst(); // máx. 10 s
                in.notifyAll();
            }
        }
    }

    /** Siguiente cuadro recibido (≈20 ms) o null si no llegó nada en timeoutMs. */
    public short[] leer(long timeoutMs) throws InterruptedException {
        long fin = System.currentTimeMillis() + timeoutMs;
        synchronized (in) {
            while (in.isEmpty()) {
                long w = fin - System.currentTimeMillis();
                if (w <= 0) return null;
                in.wait(w);
            }
            return in.removeFirst();
        }
    }

    public void limpiarEntrada() { synchronized (in) { in.clear(); } }
}
