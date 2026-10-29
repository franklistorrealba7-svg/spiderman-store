package pe.jerryshop.caller.core;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Cliente SIP liviano sobre UDP pensado para MagnusBilling/Asterisk:
 * registro con autenticación Digest, NAT (rport/received + RTP simétrico + keep-alive),
 * llamadas salientes con ACK/BYE/CANCEL correctos y respuesta a OPTIONS.
 */
public final class Sip {

    public interface Log { void log(String s); }

    public final String server;
    public final int port;
    public final String user;
    private final String pass;
    private final String displayName;
    private final Log log;

    private DatagramSocket sock;
    private InetSocketAddress srv;
    private String localIp;
    private int localPort;
    private volatile String contactHost;
    private volatile int contactPort;
    private volatile boolean running;
    private volatile boolean registered;
    private volatile String ultimoError = "";
    private volatile long ultimoMensaje;
    private int regCseq = 1;
    private final String regCallId;
    private final String regTag;
    private int expires = 300;

    private final Random rnd = new Random();
    private final Map<String, BlockingQueue<SipMessage>> transacciones = new ConcurrentHashMap<>();
    private final Map<String, Call> calls = new ConcurrentHashMap<>();
    private Thread rx, mantenimiento;

    public Sip(String server, int port, String user, String pass, String displayName, Log log) {
        this.server = server.trim();
        this.port = port;
        this.user = user.trim();
        this.pass = pass;
        this.displayName = displayName == null ? "" : displayName.replace("\"", "");
        this.log = log;
        this.regCallId = rand(16) + "@jerrycaller";
        this.regTag = rand(10);
    }

    // ================= ciclo de vida =================

    /** Abre la conexión y se registra. Devuelve true si quedó registrado. */
    public synchronized boolean start() {
        try {
            srv = new InetSocketAddress(InetAddress.getByName(server), port);
            sock = new DatagramSocket();
            sock.setSoTimeout(500);
            localPort = sock.getLocalPort();
            localIp = ipLocal();
            contactHost = localIp;
            contactPort = localPort;
            running = true;
            rx = new Thread(this::recibir, "sip-rx");
            rx.start();
        } catch (IOException e) {
            ultimoError = "No se pudo abrir la conexión: " + e.getMessage();
            log.log(ultimoError);
            return false;
        }
        boolean ok = registrar(expires);
        if (ok) {
            mantenimiento = new Thread(this::mantener, "sip-keepalive");
            mantenimiento.start();
        }
        return ok;
    }

    public synchronized void stop() {
        if (!running) return;
        for (Call c : new ArrayList<>(calls.values())) c.colgar();
        try { if (registered) registrar(0); } catch (Exception ignored) {}
        running = false;
        registered = false;
        if (sock != null) sock.close();
        if (mantenimiento != null) mantenimiento.interrupt();
    }

    public boolean registrado() { return running && registered; }
    public String ultimoError() { return ultimoError; }

    private String ipLocal() {
        try (DatagramSocket s = new DatagramSocket()) {
            s.connect(srv);
            return s.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }

    /** Re-registro periódico + keep-alive para que el NAT del router/operador no cierre el puerto. */
    private void mantener() {
        long proximoRegistro = System.currentTimeMillis() + Math.max(30, expires * 8 / 10) * 1000L;
        long proximoPing = System.currentTimeMillis() + 20_000;
        while (running) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { return; }
            long ahora = System.currentTimeMillis();
            if (ahora >= proximoPing) {
                enviar("\r\n\r\n");
                proximoPing = ahora + 20_000;
            }
            if (ahora >= proximoRegistro) {
                boolean ok = registrar(expires);
                if (!ok) {
                    try { Thread.sleep(3000); } catch (InterruptedException e) { return; }
                    ok = registrar(expires);
                }
                proximoRegistro = System.currentTimeMillis() + (ok ? Math.max(30, expires * 8 / 10) : 15) * 1000L;
            }
        }
    }

    // ================= registro =================

    private boolean registrar(int exp) {
        String uri = "sip:" + hostPort();
        String auth = null;
        for (int intento = 0; intento < 3; intento++) {
            String branch = branch();
            int cseq;
            synchronized (this) { cseq = regCseq++; }
            StringBuilder m = new StringBuilder();
            m.append("REGISTER ").append(uri).append(" SIP/2.0\r\n");
            m.append(via(branch));
            m.append("Max-Forwards: 70\r\n");
            m.append("From: <sip:").append(user).append('@').append(hostPort()).append(">;tag=").append(regTag).append("\r\n");
            m.append("To: <sip:").append(user).append('@').append(hostPort()).append(">\r\n");
            m.append("Call-ID: ").append(regCallId).append("\r\n");
            m.append("CSeq: ").append(cseq).append(" REGISTER\r\n");
            m.append("Contact: ").append(contact()).append("\r\n");
            m.append("Expires: ").append(exp).append("\r\n");
            if (auth != null) m.append(auth);
            m.append("Allow: INVITE, ACK, CANCEL, BYE, OPTIONS, NOTIFY, UPDATE, INFO\r\n");
            m.append("User-Agent: JerryCaller/1.0\r\n");
            m.append("Content-Length: 0\r\n\r\n");
            SipMessage r = peticion(m.toString(), branch, 10_000);
            if (r == null) {
                registered = false;
                ultimoError = "El servidor SIP " + server + " no responde (revisa IP e internet)";
                log.log(ultimoError);
                return false;
            }
            actualizarNat(r);
            if ((r.code == 401 || r.code == 407) && intento < 2) {
                String ch = r.h(r.code == 401 ? "www-authenticate" : "proxy-authenticate");
                auth = autorizacion(r.code, ch, "REGISTER", uri);
                if (intento == 1) { // ya enviamos credenciales y las rechazó
                    registered = false;
                    ultimoError = "Usuario o contraseña SIP incorrectos";
                    log.log(ultimoError);
                    return false;
                }
                continue;
            }
            if (r.code >= 200 && r.code < 300) {
                if (exp == 0) { registered = false; return true; }
                String e = SipMessage.param(r.h("contact"), "expires");
                try {
                    int v = e != null ? Integer.parseInt(e) : Integer.parseInt(r.h("expires").trim());
                    if (v > 0) expires = Math.min(v, 600);
                } catch (Exception ignored) {}
                boolean primera = !registered;
                registered = true;
                ultimoError = "";
                if (primera) log.log("Registrado en el SIP ✔ (" + user + "@" + server + ", IP pública " + contactHost + ")");
                return true;
            }
            registered = false;
            ultimoError = "Registro rechazado: " + r.code + " " + r.reason;
            log.log(ultimoError);
            return false;
        }
        return false;
    }

    /** Usa la IP/puerto públicos que ve el servidor (received/rport) en el Contact. */
    private void actualizarNat(SipMessage r) {
        String v = r.h("via");
        String rec = SipMessage.param(v, "received");
        String rp = SipMessage.param(v, "rport");
        if (rec != null && !rec.isEmpty()) contactHost = rec;
        if (rp != null && !rp.isEmpty()) {
            try { contactPort = Integer.parseInt(rp); } catch (NumberFormatException ignored) {}
        }
    }

    // ================= llamadas =================

    public Call llamar(String numero) throws IOException {
        if (!registrado()) throw new IOException("sin registro en el SIP");
        Call c = new Call(numero);
        calls.put(c.callId, c);
        c.invitar(null);
        return c;
    }

    public enum Estado { MARCANDO, TIMBRANDO, CONTESTADA, TERMINADA }

    public final class Call {
        public final String numero;
        public final String callId = rand(20) + "@jerrycaller";
        public final Rtp rtp;
        public volatile Estado estado = Estado.MARCANDO;
        public volatile int codigoFin;
        public volatile String motivoFin = "";
        public volatile boolean colgoCliente;
        private final String localTag = rand(10);
        private volatile String remoteTag = "";
        private volatile String remoteTarget;
        private volatile List<String> rutas = new ArrayList<>();
        private int cseq = 1;
        private volatile int inviteCseq;
        private volatile String inviteBranch;
        private volatile String inviteTexto;
        private volatile boolean provisional;
        private volatile boolean autenticado;
        private volatile boolean cancelada;
        private final String requestUri;
        private final long sesion = Math.abs(rnd.nextLong() % 1_000_000_000L);

        Call(String numero) throws IOException {
            this.numero = numero;
            this.requestUri = "sip:" + numero + "@" + hostPort();
            this.rtp = new Rtp();
            this.remoteTarget = requestUri;
        }

        private String from() {
            String dn = displayName.isEmpty() ? "" : "\"" + displayName + "\" ";
            return dn + "<sip:" + user + "@" + hostPort() + ">;tag=" + localTag;
        }

        private String to() {
            return "<" + requestUri + ">" + (remoteTag.isEmpty() ? "" : ";tag=" + remoteTag);
        }

        private String sdp() {
            String ip = contactHost;
            return "v=0\r\n" +
                    "o=- " + sesion + " " + sesion + " IN IP4 " + ip + "\r\n" +
                    "s=JerryCaller\r\n" +
                    "c=IN IP4 " + ip + "\r\n" +
                    "t=0 0\r\n" +
                    "m=audio " + rtp.localPort() + " RTP/AVP 0 8 101\r\n" +
                    "a=rtpmap:0 PCMU/8000\r\n" +
                    "a=rtpmap:8 PCMA/8000\r\n" +
                    "a=rtpmap:101 telephone-event/8000\r\n" +
                    "a=fmtp:101 0-16\r\n" +
                    "a=ptime:20\r\n" +
                    "a=sendrecv\r\n";
        }

        synchronized void invitar(String auth) {
            inviteBranch = branch();
            inviteCseq = cseq++;
            String body = sdp();
            StringBuilder m = new StringBuilder();
            m.append("INVITE ").append(requestUri).append(" SIP/2.0\r\n");
            m.append(via(inviteBranch));
            m.append("Max-Forwards: 70\r\n");
            m.append("From: ").append(from()).append("\r\n");
            m.append("To: <").append(requestUri).append(">\r\n");
            m.append("Call-ID: ").append(callId).append("\r\n");
            m.append("CSeq: ").append(inviteCseq).append(" INVITE\r\n");
            m.append("Contact: ").append(contact()).append("\r\n");
            if (auth != null) m.append(auth);
            m.append("Allow: INVITE, ACK, CANCEL, BYE, OPTIONS, NOTIFY, UPDATE, INFO\r\n");
            m.append("User-Agent: JerryCaller/1.0\r\n");
            m.append("Content-Type: application/sdp\r\n");
            m.append("Content-Length: ").append(body.getBytes(StandardCharsets.UTF_8).length).append("\r\n\r\n");
            m.append(body);
            inviteTexto = m.toString();
            provisional = false;
            enviar(inviteTexto);
            final String b = inviteBranch;
            new Thread(() -> retransmitirInvite(b), "sip-invite-rt").start();
        }

        /** UDP puede perder paquetes: reenviar el INVITE hasta tener respuesta. */
        private void retransmitirInvite(String b) {
            long t = 500;
            long total = 0;
            while (running && !provisional && estado == Estado.MARCANDO && b.equals(inviteBranch)) {
                try { Thread.sleep(t); } catch (InterruptedException e) { return; }
                total += t;
                if (provisional || estado != Estado.MARCANDO || !b.equals(inviteBranch)) return;
                if (total >= 32_000) { terminar(408, "Sin respuesta del servidor"); return; }
                enviar(inviteTexto);
                t = Math.min(t * 2, 4000);
            }
        }

        void alResponder(SipMessage r) {
            String metodo = r.cseqMethod();
            if (!"INVITE".equals(metodo)) return; // respuestas a BYE/CANCEL: nada que hacer
            if (r.cseqNum() != inviteCseq) {
                if (r.code >= 200 && r.code < 300) ack(r, true); // 200 tardío de un INVITE anterior
                return;
            }
            if (r.code < 200) {
                provisional = true;
                if ((r.code == 180 || r.code == 183) && estado == Estado.MARCANDO) estado = Estado.TIMBRANDO;
                return;
            }
            provisional = true;
            String tt = SipMessage.tag(r.h("to"));
            if (r.code < 300) {
                boolean nueva = estado != Estado.CONTESTADA;
                if (nueva) {
                    remoteTag = tt;
                    String ct = r.h("contact");
                    if (!ct.isEmpty()) remoteTarget = SipMessage.uriOf(ct);
                    List<String> rr = new ArrayList<>(r.all("record-route"));
                    Collections.reverse(rr);
                    rutas = rr;
                }
                ack(r, true);
                if (nueva) {
                    if (cancelada || estado == Estado.TERMINADA) { bye(); return; }
                    aplicarSdp(r.body);
                    rtp.start();
                    estado = Estado.CONTESTADA;
                }
                return;
            }
            // respuesta final de error: hay que confirmarla con ACK
            ack(r, false);
            if ((r.code == 401 || r.code == 407) && !autenticado) {
                autenticado = true;
                String ch = r.h(r.code == 401 ? "www-authenticate" : "proxy-authenticate");
                invitar(autorizacion(r.code, ch, "INVITE", requestUri));
                return;
            }
            terminar(r.code, r.reason);
        }

        private void aplicarSdp(String body) {
            String ip = null; int puerto = 0; int pt = 0;
            for (String l : body.split("\r?\n")) {
                l = l.trim();
                if (l.startsWith("c=IN IP4 ")) ip = l.substring(9).trim();
                else if (l.startsWith("m=audio ")) {
                    String[] p = l.split("\\s+");
                    try { puerto = Integer.parseInt(p[1]); } catch (Exception ignored) {}
                    for (int i = 3; i < p.length; i++) {
                        if (p[i].equals("0") || p[i].equals("8")) { pt = Integer.parseInt(p[i]); break; }
                    }
                }
            }
            if (ip == null || ip.equals("0.0.0.0")) ip = srv.getAddress().getHostAddress();
            if (puerto > 0) rtp.setRemote(ip, puerto, pt);
        }

        private void ack(SipMessage r, boolean exito) {
            StringBuilder m = new StringBuilder();
            String uri = exito ? remoteTarget : requestUri;
            m.append("ACK ").append(uri).append(" SIP/2.0\r\n");
            m.append(via(exito ? branch() : inviteBranch));
            m.append("Max-Forwards: 70\r\n");
            if (exito) for (String ruta : rutas) m.append("Route: ").append(ruta).append("\r\n");
            m.append("From: ").append(from()).append("\r\n");
            m.append("To: ").append(r.h("to")).append("\r\n");
            m.append("Call-ID: ").append(callId).append("\r\n");
            m.append("CSeq: ").append(r.cseqNum()).append(" ACK\r\n");
            m.append("User-Agent: JerryCaller/1.0\r\n");
            m.append("Content-Length: 0\r\n\r\n");
            enviar(m.toString());
        }

        private void bye() {
            int n;
            synchronized (this) { n = cseq++; }
            String b = branch();
            StringBuilder m = new StringBuilder();
            m.append("BYE ").append(remoteTarget).append(" SIP/2.0\r\n");
            m.append(via(b));
            m.append("Max-Forwards: 70\r\n");
            for (String ruta : rutas) m.append("Route: ").append(ruta).append("\r\n");
            m.append("From: ").append(from()).append("\r\n");
            m.append("To: ").append(to()).append("\r\n");
            m.append("Call-ID: ").append(callId).append("\r\n");
            m.append("CSeq: ").append(n).append(" BYE\r\n");
            m.append("User-Agent: JerryCaller/1.0\r\n");
            m.append("Content-Length: 0\r\n\r\n");
            String texto = m.toString();
            new Thread(() -> peticion(texto, b, 4000), "sip-bye").start();
        }

        private void cancelar() {
            StringBuilder m = new StringBuilder();
            m.append("CANCEL ").append(requestUri).append(" SIP/2.0\r\n");
            m.append(via(inviteBranch));
            m.append("Max-Forwards: 70\r\n");
            m.append("From: ").append(from()).append("\r\n");
            m.append("To: <").append(requestUri).append(">\r\n");
            m.append("Call-ID: ").append(callId).append("\r\n");
            m.append("CSeq: ").append(inviteCseq).append(" CANCEL\r\n");
            m.append("User-Agent: JerryCaller/1.0\r\n");
            m.append("Content-Length: 0\r\n\r\n");
            enviar(m.toString());
        }

        /** Corta la llamada en cualquier estado (BYE si contestó, CANCEL si aún timbra). */
        public void colgar() {
            Estado e = estado;
            if (e == Estado.TERMINADA) return;
            if (e == Estado.CONTESTADA) {
                bye();
                terminar(0, "Colgada por el programa");
            } else {
                cancelada = true;
                if (provisional) cancelar();
                terminar(487, "Cancelada (no contestó a tiempo)");
            }
        }

        void alColgarRemoto() {
            if (estado == Estado.TERMINADA) return;
            colgoCliente = true;
            terminar(0, "El cliente colgó");
        }

        void terminar(int code, String motivo) {
            if (estado == Estado.TERMINADA) return;
            codigoFin = code;
            motivoFin = motivo == null ? "" : motivo;
            estado = Estado.TERMINADA;
            rtp.stop();
            // se deja un rato en el mapa por si llegan retransmisiones del servidor
            new Thread(() -> {
                try { Thread.sleep(32_000); } catch (InterruptedException ignored) {}
                calls.remove(callId);
            }, "sip-limpieza").start();
        }

        String sdpRespuesta() { return sdp(); }

        void reinvite(SipMessage r) {
            aplicarSdp(r.body);
        }
    }

    // ================= recepción =================

    private void recibir() {
        byte[] buf = new byte[65535];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        while (running) {
            try {
                sock.receive(dp);
            } catch (SocketTimeoutException e) {
                continue;
            } catch (IOException e) {
                if (running) { registered = false; ultimoError = "Conexión cerrada: " + e.getMessage(); }
                break;
            }
            ultimoMensaje = System.currentTimeMillis();
            if (dp.getLength() < 10) continue; // keep-alive
            SipMessage m = SipMessage.parse(buf, dp.getLength());
            if (m == null) continue;
            try {
                if (m.isRequest()) peticionEntrante(m);
                else respuestaEntrante(m);
            } catch (Exception e) {
                log.log("SIP: error procesando " + m.summary() + ": " + e);
            }
        }
    }

    private void respuestaEntrante(SipMessage r) {
        String b = SipMessage.param(r.h("via"), "branch");
        if ("INVITE".equals(r.cseqMethod())) {
            Call c = calls.get(r.callId());
            if (c != null) c.alResponder(r);
            return;
        }
        if (b != null) {
            BlockingQueue<SipMessage> q = transacciones.get(b);
            if (q != null) q.offer(r);
        }
    }

    private void peticionEntrante(SipMessage m) {
        Call c = calls.get(m.callId());
        switch (m.method) {
            case "ACK":
                return;
            case "BYE":
                responder(m, 200, "OK", null);
                if (c != null) c.alColgarRemoto();
                return;
            case "INVITE":
                if (c != null) { // re-INVITE (refresco de sesión o cambio de audio)
                    c.reinvite(m);
                    responder(m, 200, "OK", c.sdpRespuesta());
                } else {
                    responder(m, 486, "Busy Here", null); // no atendemos llamadas entrantes
                }
                return;
            case "CANCEL":
                responder(m, 200, "OK", null);
                return;
            default: // OPTIONS (¿sigues ahí?), NOTIFY, INFO, UPDATE, MESSAGE...
                responder(m, 200, "OK", null);
        }
    }

    private void responder(SipMessage req, int code, String reason, String sdp) {
        StringBuilder m = new StringBuilder();
        m.append("SIP/2.0 ").append(code).append(' ').append(reason).append("\r\n");
        for (String v : req.all("via")) m.append("Via: ").append(v).append("\r\n");
        for (String rr : req.all("record-route")) m.append("Record-Route: ").append(rr).append("\r\n");
        m.append("From: ").append(req.h("from")).append("\r\n");
        String to = req.h("to");
        if (SipMessage.tag(to).isEmpty()) {
            Call c = calls.get(req.callId());
            to = to + ";tag=" + (c != null ? c.localTag : rand(10));
        }
        m.append("To: ").append(to).append("\r\n");
        m.append("Call-ID: ").append(req.callId()).append("\r\n");
        m.append("CSeq: ").append(req.h("cseq")).append("\r\n");
        if (sdp != null) m.append("Contact: ").append(contact()).append("\r\n");
        m.append("Allow: INVITE, ACK, CANCEL, BYE, OPTIONS, NOTIFY, UPDATE, INFO\r\n");
        m.append("User-Agent: JerryCaller/1.0\r\n");
        if (sdp != null) {
            m.append("Content-Type: application/sdp\r\n");
            m.append("Content-Length: ").append(sdp.getBytes(StandardCharsets.UTF_8).length).append("\r\n\r\n").append(sdp);
        } else {
            m.append("Content-Length: 0\r\n\r\n");
        }
        enviar(m.toString());
    }

    // ================= utilidades =================

    /** Envía una petición no-INVITE con reintentos UDP y espera la respuesta final. */
    private SipMessage peticion(String texto, String branch, long timeoutMs) {
        BlockingQueue<SipMessage> q = new LinkedBlockingQueue<>();
        transacciones.put(branch, q);
        try {
            long fin = System.currentTimeMillis() + timeoutMs;
            long t = 500;
            enviar(texto);
            while (System.currentTimeMillis() < fin) {
                SipMessage r = q.poll(t, TimeUnit.MILLISECONDS);
                if (r != null) {
                    if (r.code >= 200) return r;
                    t = 4000; // provisional: dejar de reenviar tan seguido
                    continue;
                }
                enviar(texto);
                t = Math.min(t * 2, 4000);
            }
            return null;
        } catch (InterruptedException e) {
            return null;
        } finally {
            transacciones.remove(branch);
        }
    }

    private void enviar(String texto) {
        DatagramSocket s = sock;
        if (s == null) return;
        byte[] b = texto.getBytes(StandardCharsets.UTF_8);
        try { s.send(new DatagramPacket(b, b.length, srv)); } catch (IOException ignored) {}
    }

    private String hostPort() { return port == 5060 ? server : server + ":" + port; }

    private String via(String branch) {
        return "Via: SIP/2.0/UDP " + localIp + ":" + localPort + ";branch=" + branch + ";rport\r\n";
    }

    private String contact() {
        return "<sip:" + user + "@" + contactHost + ":" + contactPort + ";transport=udp>";
    }

    private String branch() { return "z9hG4bK" + rand(16); }

    private String rand(int n) {
        String a = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(a.charAt(rnd.nextInt(a.length())));
        return sb.toString();
    }

    private String autorizacion(int code, String challenge, String method, String uri) {
        String realm = SipMessage.authParam(challenge, "realm");
        String nonce = SipMessage.authParam(challenge, "nonce");
        String opaque = SipMessage.authParam(challenge, "opaque");
        String qop = SipMessage.authParam(challenge, "qop");
        if (realm == null) realm = server;
        if (nonce == null) nonce = "";
        String ha1 = md5(user + ":" + realm + ":" + pass);
        String ha2 = md5(method + ":" + uri);
        String nc = "00000001", cnonce = rand(12), resp;
        boolean conQop = qop != null && qop.toLowerCase(Locale.ROOT).contains("auth");
        resp = conQop ? md5(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":auth:" + ha2) : md5(ha1 + ":" + nonce + ":" + ha2);
        StringBuilder a = new StringBuilder(code == 407 ? "Proxy-Authorization: " : "Authorization: ");
        a.append("Digest username=\"").append(user).append("\", realm=\"").append(realm)
         .append("\", nonce=\"").append(nonce).append("\", uri=\"").append(uri)
         .append("\", response=\"").append(resp).append("\", algorithm=MD5");
        if (conQop) a.append(", qop=auth, nc=").append(nc).append(", cnonce=\"").append(cnonce).append('"');
        if (opaque != null) a.append(", opaque=\"").append(opaque).append('"');
        return a.append("\r\n").toString();
    }

    static String md5(String s) {
        try {
            byte[] d = MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : d) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
