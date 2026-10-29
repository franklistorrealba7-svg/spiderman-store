package pe.jerryshop.caller.core;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pe.jerryshop.caller.core.Clientes.Cliente;

/** Campaña de llamadas: marca, habla con la voz de ElevenLabs y conversa con ChatGPT. */
public final class Engine {

    public interface Host {
        void log(String linea);
        void clienteCambio(Cliente c);
        void estadoCambio();
    }

    public static final TimeZone LIMA = TimeZone.getTimeZone("America/Lima");

    private final Config cfg;
    private final Ai ai;
    private final Clientes.Historial historial;
    private final Host host;

    private volatile Sip sip;
    private final Object lockConexion = new Object();
    private volatile boolean corriendo;
    private volatile boolean esPrueba;
    private volatile boolean forzarReconexion;
    private int fallasSeguidas;
    private final List<Thread> lineas = new ArrayList<>();
    private final List<Cliente> clientes = Collections.synchronizedList(new ArrayList<>());
    private final List<Cliente> listaPrueba = Collections.synchronizedList(new ArrayList<>());
    private final List<String> registro = Collections.synchronizedList(new ArrayList<>());

    // en vivo
    public static final class Activa {
        public final String clave;
        public final Sip.Call call;
        public final Cliente cliente;
        public volatile boolean manual;
        final BlockingQueue<String[]> ordenes = new LinkedBlockingQueue<>();
        Activa(String clave, Sip.Call call, Cliente c) { this.clave = clave; this.call = call; this.cliente = c; }
    }

    private final Map<String, Activa> activas = Collections.synchronizedMap(new LinkedHashMap<>());
    private volatile Rtp.AudioSink monitor;
    private volatile boolean escucharAuto;
    private volatile String escuchando;

    public Engine(Config cfg, Ai ai, Clientes.Historial historial, Host host) {
        this.cfg = cfg;
        this.ai = ai;
        this.historial = historial;
        this.host = host;
    }

    // ================= estado =================

    public boolean corriendo() { return corriendo; }
    public boolean esPrueba() { return esPrueba; }
    public boolean conectado() { Sip s = sip; return !forzarReconexion && s != null && s.registrado(); }
    public List<Cliente> clientes() { synchronized (clientes) { return new ArrayList<>(clientes); } }
    public List<String> registro() { synchronized (registro) { return new ArrayList<>(registro); } }
    public List<Activa> activas() { synchronized (activas) { return new ArrayList<>(activas.values()); } }

    public void setClientes(List<Cliente> lista) {
        synchronized (clientes) { clientes.clear(); clientes.addAll(lista); }
    }

    public void log(String s) {
        SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);
        f.setTimeZone(LIMA);
        String l = "[" + f.format(new Date()) + "] " + s;
        synchronized (registro) {
            registro.add(l);
            while (registro.size() > 800) registro.remove(0);
        }
        host.log(l);
    }

    private static String ahoraTexto() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
        f.setTimeZone(LIMA);
        return f.format(new Date());
    }

    // ================= horario =================

    static int aMinutos(String v) {
        String s = v.trim().replace('.', ':');
        try {
            if (s.contains(":")) {
                String[] p = s.split(":");
                return Integer.parseInt(p[0].trim()) * 60 + (p.length > 1 && !p[1].trim().isEmpty() ? Integer.parseInt(p[1].trim()) : 0);
            }
            return Integer.parseInt(s) * 60;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean enHorario() {
        if (esPrueba || cfg.b("ignorar_horario")) return true;
        Calendar c = Calendar.getInstance(LIMA);
        int m = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        int ini = aMinutos(cfg.s("hora_inicio")), fin = aMinutos(cfg.s("hora_fin"));
        return ini <= fin ? (m >= ini && m < fin) : (m >= ini || m < fin);
    }

    // ================= conexión =================

    private boolean asegurarConexion() {
        if (conectado()) return true;
        synchronized (lockConexion) {
            if (conectado()) return true;
            forzarReconexion = false;
            Sip viejo = sip;
            if (viejo != null) { log("⚠ Se perdió la conexión con el SIP. Reconectando..."); viejo.stop(); }
            else log("Conectando a " + cfg.s("sip_server") + " como " + cfg.s("sip_user") + "...");
            for (int intento = 1; corriendo; intento++) {
                Sip s = new Sip(cfg.s("sip_server"), cfg.i("sip_port"), cfg.s("sip_user"), cfg.s("sip_pass"),
                        cfg.s("caller_name"), this::log);
                if (s.start()) { sip = s; host.estadoCambio(); return true; }
                s.stop();
                if (s.ultimoError().contains("incorrectos")) { log("Revisa el usuario y la contraseña SIP en Configuración."); detener(); return false; }
                int espera = Math.min(60, 10 * intento);
                log("   Reintentando conexión en " + espera + " s (las llamadas quedan en pausa)...");
                dormir(espera * 1000L);
            }
            return false;
        }
    }

    private void dormir(long ms) {
        long fin = System.currentTimeMillis() + ms;
        while (corriendo && System.currentTimeMillis() < fin) {
            try { Thread.sleep(Math.min(500, fin - System.currentTimeMillis())); } catch (InterruptedException e) { return; }
        }
    }

    // ================= campaña =================

    /** Llamada de prueba a un número (no toca la lista de clientes ni el historial). */
    public synchronized boolean probar(String numero) {
        String n = Clientes.limpiarNumero(numero);
        if (n == null || corriendo) return false;
        synchronized (listaPrueba) { listaPrueba.clear(); listaPrueba.add(new Cliente("Prueba", n)); }
        iniciar(true);
        return true;
    }

    /** Cambió la red (Wi-Fi ↔ datos): re-registrar con la nueva IP. */
    public void redCambio() {
        if (corriendo && sip != null) { forzarReconexion = true; log("Cambió la red: reconectando con la nueva conexión..."); }
    }

    private List<Cliente> fuente() { return esPrueba ? listaPrueba : clientes; }

    public synchronized void iniciar(boolean prueba) {
        if (corriendo) return;
        esPrueba = prueba;
        corriendo = true;
        List<Cliente> origen;
        synchronized (fuente()) { origen = new ArrayList<>(fuente()); }
        for (Cliente c : origen) {
            if (!"Contestó".equals(c.estado)) { c.estado = "Pendiente"; c.intentos = 0; c.proximo = 0; }
        }
        lineas.clear();
        Thread pre = new Thread(this::precalentar, "precalentar");
        pre.setPriority(Thread.MIN_PRIORITY);
        pre.start();
        int n = prueba ? 1 : Math.max(1, Math.min(4, cfg.i("max_simultaneas")));
        for (int k = 0; k < n; k++) {
            final int num = k + 1;
            Thread t = new Thread(() -> trabajador(num), "linea" + num);
            lineas.add(t);
            t.start();
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        }
        new Thread(this::esperarFin, "fin-campana").start();
        host.estadoCambio();
    }

    public void detener() {
        if (!corriendo) return;
        corriendo = false;
        log("Deteniendo: se termina la llamada en curso y no se marcan más.");
        host.estadoCambio();
    }

    private void esperarFin() {
        for (Thread t : new ArrayList<>(lineas)) {
            try { t.join(); } catch (InterruptedException ignored) {}
        }
        corriendo = false;
        Sip s = sip;
        sip = null;
        if (s != null) s.stop();
        log(esPrueba ? "Prueba terminada." : "Campaña terminada.");
        host.estadoCambio();
    }

    private void precalentar() {
        if (!cfg.b("ia_activa")) return;
        List<String> fijas = new ArrayList<>();
        fijas.add(cfg.s("repregunta"));
        fijas.add(cfg.s("despedida"));
        fijas.add(cfg.s("frase_dictado"));
        for (String f : cfg.s("frases_espera").split("\n")) if (!f.trim().isEmpty()) fijas.add(f.trim());
        for (String f : fijas) {
            try { ai.voz(f, false, null); } catch (Exception ignored) {}
        }
    }

    private Cliente siguiente(boolean[] quedan) {
        long ahora = System.currentTimeMillis();
        List<Cliente> lista = fuente();
        synchronized (lista) {
            quedan[0] = false;
            for (Cliente c : lista) {
                if ("Pendiente".equals(c.estado) || ("Reintentar".equals(c.estado) && c.proximo <= ahora)) {
                    c.estado = "Llamando";
                    return c;
                }
                if ("Reintentar".equals(c.estado) || "Llamando".equals(c.estado)) quedan[0] = true;
            }
        }
        return null;
    }

    private void trabajador(int num) {
        boolean avisoHorario = false;
        while (corriendo) {
            if (!enHorario()) {
                if (!avisoHorario) {
                    avisoHorario = true;
                    log("Fuera de horario (" + cfg.s("hora_inicio") + " a " + cfg.s("hora_fin") + "). Esperando la hora de inicio...");
                }
                dormir(30_000);
                continue;
            }
            avisoHorario = false;
            if (!asegurarConexion()) break;
            boolean[] quedan = new boolean[1];
            Cliente c = siguiente(quedan);
            if (c == null) {
                if (!quedan[0]) break;
                dormir(5000);
                continue;
            }
            procesar(c);
        }
        log("Línea " + num + " terminó.");
    }

    private void procesar(Cliente c) {
        c.intentos++;
        c.inicio = ahoraTexto();
        host.clienteCambio(c);
        String resultado;
        try {
            resultado = llamar(c);
        } catch (SinConexion e) {
            c.intentos--;
            c.estado = "Pendiente";
            host.clienteCambio(c);
            log("   " + c.telefono + " vuelve a la cola (falla de conexión: " + e.getMessage() + ")");
            if (++fallasSeguidas >= 3 || !conectado()) { fallasSeguidas = 0; forzarReconexion = true; }
            dormir(3000);
            return;
        } catch (Exception e) {
            resultado = "Error";
            c.detalle = recortar(String.valueOf(e.getMessage()));
            log("Error con " + c.telefono + ": " + e.getMessage());
        }
        if ("No contestó".equals(resultado) && c.intentos <= cfg.i("reintentos") && !esPrueba) {
            c.estado = "Reintentar";
            c.proximo = System.currentTimeMillis() + cfg.i("min_entre_reintentos") * 60_000L;
        } else {
            c.estado = resultado;
        }
        if ("Contestó".equals(resultado) && !esPrueba) historial.registrar(c, ahoraTexto());
        host.clienteCambio(c);
    }

    // ================= una llamada =================

    static final class SinConexion extends Exception {
        SinConexion(String m) { super(m); }
    }

    static final class ClienteColgo extends Exception {
        ClienteColgo() { super("colgó"); }
    }

    private boolean iaActiva() {
        return cfg.b("ia_activa") && !cfg.s("openai_key").trim().isEmpty();
    }

    String textoSaludo(String nombre) {
        String base = iaActiva() ? cfg.s("mensaje_ia") : cfg.s("mensaje");
        return base.replace("{nombre}", nombre).replace(" ,", ",").replace("  ", " ");
    }

    private String llamar(Cliente c) throws Exception {
        String numero = cfg.s("prefijo").trim() + c.telefono;
        String nombre = c.primerNombre();
        byte[] saludo;
        try {
            saludo = ai.voz(textoSaludo(nombre), false, null); // listo antes de marcar: sin silencio al contestar
        } catch (java.net.UnknownHostException | java.net.ConnectException | java.net.SocketTimeoutException e) {
            throw new SinConexion("sin internet (" + e.getMessage() + ")");
        }
        Sip s = sip;
        if (s == null || !s.registrado()) throw new SinConexion("sin registro en el SIP");
        log("Llamando a " + (nombre.isEmpty() ? "(sin nombre)" : nombre) + " " + numero + "...");
        Sip.Call call;
        try {
            call = s.llamar(numero);
        } catch (IOException e) {
            throw new SinConexion(e.getMessage());
        }
        fallasSeguidas = 0;
        long t0 = System.currentTimeMillis();
        long limite = cfg.i("seg_timbrando") * 1000L;
        while (call.estado != Sip.Estado.CONTESTADA) {
            if (call.estado == Sip.Estado.TERMINADA) {
                String motivo = call.codigoFin > 0 ? call.codigoFin + " " + call.motivoFin : call.motivoFin;
                c.detalle = motivo;
                log(numero + ": no contestó (" + motivo + " — " + explicar(call.codigoFin) + ")");
                if (call.codigoFin == 408 && !s.registrado()) throw new SinConexion("el servidor no respondió");
                int code = call.codigoFin;
                if (code == 403 || code == 402 || code == 484 || code == 488 || code == 503 || code == 404) return "Error";
                return "No contestó";
            }
            if (System.currentTimeMillis() - t0 > limite || !corriendo) {
                call.colgar();
                log(numero + ": no contestó (timbró " + (System.currentTimeMillis() - t0) / 1000 + " s)");
                return "No contestó";
            }
            Thread.sleep(100);
        }

        long inicio = System.currentTimeMillis();
        List<String> conversacion = new ArrayList<>();
        String clave = new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date()) + " · "
                + (nombre.isEmpty() ? "Cliente" : nombre) + " " + c.telefono;
        Activa a = new Activa(clave, call, c);
        activas.put(clave, a);
        if (escucharAuto || (escuchando != null && !activas.containsKey(escuchando))) {
            call.rtp.setMonitor(monitor);
            escuchando = clave;
        }
        host.estadoCambio();
        try {
            Thread.sleep(300);
            reproducir(call, saludo, "mensaje");
            if (iaActiva()) conversacion = conversar(a);
            else Thread.sleep(500);
        } catch (ClienteColgo e) {
            // el cliente colgó
        } catch (Exception e) {
            log("   ✖ Error durante la llamada: " + e.getMessage());
            c.detalle = recortar(String.valueOf(e.getMessage()));
        } finally {
            if (call.estado == Sip.Estado.CONTESTADA) log("   Colgando (fin normal)");
            else if (call.colgoCliente) log("   El cliente colgó.");
            call.rtp.setMonitor(null);
            call.colgar();
            activas.remove(clave);
            host.estadoCambio();
        }
        if (call.rtp.paquetesRecibidos() == 0) {
            log("   ⚠ No llegó audio del cliente. Si el cliente no escuchó nada, activa NAT=yes en tu usuario de MagnusBilling.");
        }
        c.duracion = (int) ((System.currentTimeMillis() - inicio) / 1000);
        c.conversacion = String.join(" | ", conversacion);
        log(numero + ": contestó (" + c.duracion + " s)");
        return "Contestó";
    }

    static String explicar(int code) {
        switch (code) {
            case 403: return "prohibido: revisa saldo, permisos o el prefijo";
            case 404: return "número no encontrado: revisa el prefijo";
            case 408: return "el operador no respondió a tiempo";
            case 480: return "celular apagado o sin señal";
            case 484: return "número incompleto: revisa el prefijo";
            case 486: return "ocupado";
            case 487: return "cancelada";
            case 488: return "códec no aceptado";
            case 402: return "sin saldo";
            case 503: return "servicio no disponible / sin rutas";
            case 603: return "el cliente rechazó la llamada";
            default: return code >= 500 ? "falla del operador" : "ver código SIP";
        }
    }

    private void verificar(Sip.Call call) throws ClienteColgo {
        if (call.estado != Sip.Estado.CONTESTADA) throw new ClienteColgo();
    }

    private void esperarQueSuene(Sip.Call call, long t0, String nombre) throws Exception {
        while (call.rtp.pendiente() > 0) {
            verificar(call);
            Thread.sleep(40);
        }
        Thread.sleep(250); // margen para que termine de sonar en el teléfono
        verificar(call);
        log("   ✔ " + nombre + " (" + (System.currentTimeMillis() - t0) / 100 / 10.0 + " s)");
    }

    private void reproducir(Sip.Call call, byte[] audio, String nombre) throws Exception {
        verificar(call);
        long t0 = System.currentTimeMillis();
        call.rtp.write(audio);
        try {
            esperarQueSuene(call, t0, nombre);
        } catch (ClienteColgo e) {
            log("   ✖ La llamada se cortó a los " + (System.currentTimeMillis() - t0) / 1000 + " s (" + nombre + ")");
            throw e;
        }
    }

    /** Voz en streaming: empieza a sonar mientras ElevenLabs la genera. */
    private void hablar(Sip.Call call, String texto, boolean rapido, String nombre) throws Exception {
        verificar(call);
        long t0 = System.currentTimeMillis();
        final boolean[] primero = {true};
        ai.voz(texto, rapido, (b, n) -> {
            if (call.estado != Sip.Estado.CONTESTADA) throw new ClienteColgo();
            if (primero[0]) { primero[0] = false; log("   ⏱ la voz empezó a sonar en " + (System.currentTimeMillis() - t0) / 100 / 10.0 + " s"); }
            call.rtp.write(b, 0, n);
        });
        esperarQueSuene(call, t0, nombre);
    }

    private static final Object ORDEN = new Object();

    /** Graba lo que dice el cliente. Devuelve short[] (PCM 8 kHz), null si no habló, o ORDEN si el operador escribió algo. */
    private Object escuchar(Activa a, double espera, double silencioFin, double maximo) throws Exception {
        Sip.Call call = a.call;
        int umbral = cfg.i("umbral_voz");
        call.rtp.limpiarEntrada();
        List<short[]> grabado = new ArrayList<>();
        boolean hablando = false;
        long t0 = System.currentTimeMillis(), ultVoz = 0, tHabla = 0;
        while (true) {
            verificar(call);
            short[] f = call.rtp.leer(60);
            long ahora = System.currentTimeMillis();
            int nivel = f == null ? 0 : G711.rms(f, 0, f.length);
            if (nivel > umbral) {
                if (!hablando) { hablando = true; tHabla = ahora; }
                ultVoz = ahora;
            }
            if (hablando) {
                if (f != null) grabado.add(f);
                if (ahora - ultVoz > silencioFin * 1000 || ahora - tHabla > maximo * 1000) break;
            } else if (ahora - t0 > espera * 1000) {
                return null;
            } else if (!a.ordenes.isEmpty()) {
                return ORDEN;
            }
        }
        int total = 0;
        for (short[] g : grabado) total += g.length;
        if (total < 4000) return null; // menos de medio segundo = ruido
        short[] out = new short[total];
        int p = 0;
        for (short[] g : grabado) { System.arraycopy(g, 0, out, p, g.length); p += g.length; }
        return out;
    }

    private static final Pattern PIDE_ESPERAR = Pattern.compile(
            "\\b(espera|esperame|espérame|esperate|un momento|un momentito|momentito|un segundo|un segundito|"
            + "un ratito|dame un|déjame|dejame|aguanta|aguántame|voy a buscar|busco|ahorita|no tengo|"
            + "deja que|permiteme|permíteme|ya vengo|un toque)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern LISTO = Pattern.compile("\\b(ya tengo|listo|lista|ya está|ya esta|dime|dale)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    static boolean pideEsperar(String texto) {
        String t = texto.toLowerCase(Locale.ROOT);
        if (LISTO.matcher(t).find() && !t.matches("(?s).*\\bno tengo\\b.*")) return false;
        return PIDE_ESPERAR.matcher(t).find();
    }

    private static Map<String, Object> msg(String rol, String texto) {
        return Json.obj("role", rol, "content", texto);
    }

    private List<String> conversar(Activa a) throws Exception {
        Sip.Call call = a.call;
        Cliente c = a.cliente;
        String nombre = c.primerNombre();
        List<Map<String, Object>> hist = new ArrayList<>();
        hist.add(msg("system", cfg.s("prompt_ia") + (nombre.isEmpty() ? "" : "\nEl cliente se llama " + nombre + ".")));
        hist.add(msg("assistant", textoSaludo(nombre)));
        List<String> reg = new ArrayList<>();
        c.anoto = "No";
        c.guardo = "No";
        int silencios = 0, turnos = 0, avisosEspera = 0;
        boolean esperando = false;
        long inicio = System.currentTimeMillis();
        List<String> frases = new ArrayList<>();
        for (String f : cfg.s("frases_espera").split("\n")) if (!f.trim().isEmpty()) frases.add(f.trim());
        if (frases.isEmpty()) frases.add("Aquí te espero.");
        double finVoz = cfg.d("seg_silencio_fin");
        String correcto = Config.NUMERO_NUEVO;

        while ((turnos < cfg.i("max_turnos") && System.currentTimeMillis() - inicio < 300_000) || a.manual) {
            if (atenderOrdenes(a, hist, reg)) return reg;
            if (a.manual) {
                Object au = escuchar(a, 30, finVoz, 20);
                if (au instanceof short[]) {
                    String t = ai.transcribir((short[]) au);
                    if (!t.isEmpty()) {
                        reg.add("Cliente: " + t);
                        hist.add(msg("user", t));
                        log("   " + c.telefono + " dice: " + t + "   (modo manual: responde tú)");
                    }
                }
                continue;
            }
            Object au;
            if (esperando) {
                au = escuchar(a, cfg.d("seg_espera_cliente"), finVoz, 20);
                if (au == ORDEN) continue;
                if (au == null) {
                    if (avisosEspera >= frases.size()) { esperando = false; log("   (terminó la espera sin respuesta)"); continue; }
                    String frase = frases.get(avisosEspera++);
                    log("   (esperando al cliente... aviso " + avisosEspera + ")");
                    hablar(call, frase, false, "aviso de espera");
                    hist.add(msg("assistant", frase));
                    continue;
                }
                esperando = false;
                avisosEspera = 0;
            } else {
                au = escuchar(a, 9, finVoz, 20);
                if (au == ORDEN) continue;
                if (au == null) {
                    silencios++;
                    log("   (el cliente no habló)");
                    if (silencios >= 2) break;
                    hablar(call, cfg.s("repregunta"), false, "repregunta");
                    hist.add(msg("assistant", cfg.s("repregunta")));
                    continue;
                }
            }
            silencios = 0;
            turnos++;
            long tTr = System.currentTimeMillis();
            String texto = ai.transcribir((short[]) au);
            tTr = System.currentTimeMillis() - tTr;
            if (texto.isEmpty()) continue;
            reg.add("Cliente: " + texto);
            log("   " + c.telefono + " dice: " + texto);
            hist.add(msg("user", texto));

            String[] v = NumerosEs.verificar(texto, correcto);
            String nota = null;
            if ("correcto".equals(v[0])) {
                nota = "[VERIFICACIÓN] CORRECTO: el cliente repitió bien el número 983 501 670.";
                c.anoto = "Sí";
            } else if ("incorrecto".equals(v[0])) {
                nota = "[VERIFICACIÓN] INCORRECTO: el cliente dijo " + NumerosEs.espaciado(v[1]) + ". Está mal "
                        + NumerosEs.detallarError(v[1], correcto) + ". El correcto es 9 8 3 5 0 1 6 7 0.";
            }
            if (nota != null) { log("   " + nota); hist.add(msg("system", nota)); }

            long tIa = System.currentTimeMillis();
            String resp = ai.responder(hist);
            log("   ⏱ entender " + tTr / 100 / 10.0 + " s · pensar " + (System.currentTimeMillis() - tIa) / 100 / 10.0 + " s");
            boolean fin = resp.contains("[FIN]");
            if (resp.contains("[ESPERAR]") || (pideEsperar(texto) && !fin)) {
                esperando = true;
                avisosEspera = 0;
                log("   (el cliente pidió esperar: modo espera)");
            }
            if (resp.contains("[ANOTADO]")) c.anoto = "Sí";
            if (resp.contains("[GUARDADO]")) c.guardo = "Sí";
            boolean dictar = resp.contains("[DICTAR]");
            String limpio = resp.replaceAll("\\[(FIN|ANOTADO|GUARDADO|ESPERAR|DICTAR)\\]", "").trim();
            hist.add(msg("assistant", resp));
            reg.add("IA: " + limpio);
            log("   IA responde: " + limpio);
            host.clienteCambio(c);
            if (!limpio.isEmpty()) hablar(call, limpio, true, "respuesta IA");
            if (dictar) {
                hablar(call, cfg.s("frase_dictado"), false, "dictado del número");
                Map<String, Object> ult = hist.get(hist.size() - 1);
                ult.put("content", ult.get("content") + " " + cfg.s("frase_dictado"));
            }
            if (fin && !a.manual) return reg;
        }
        hablar(call, cfg.s("despedida"), false, "despedida");
        return reg;
    }

    // ================= control en vivo =================

    /** tipo: "decir" (Vanessa dice el texto), "manual", "auto" o "colgar". clave null = última llamada. */
    public boolean ordenar(String clave, String tipo, String texto) {
        Activa a = clave != null ? activas.get(clave) : ultimaActiva();
        if (a == null) { log("No hay ninguna llamada contestada en este momento."); return false; }
        switch (tipo) {
            case "manual":
                a.manual = true;
                log("🙋 Tomaste el control de " + a.clave + ": la IA ya no responde, escribe lo que debe decir Vanessa.");
                break;
            case "auto":
                a.manual = false;
                log("🤖 La IA vuelve a responder en " + a.clave + ".");
                break;
            default:
                a.ordenes.offer(new String[]{tipo, texto == null ? "" : texto});
        }
        host.estadoCambio();
        return true;
    }

    public Activa ultimaActiva() {
        synchronized (activas) {
            Activa u = null;
            for (Activa a : activas.values()) u = a;
            return u;
        }
    }

    private boolean atenderOrdenes(Activa a, List<Map<String, Object>> hist, List<String> reg) throws Exception {
        String[] o;
        while ((o = a.ordenes.poll()) != null) {
            if ("colgar".equals(o[0])) { log("   ☎ Colgado por el operador"); return true; }
            if ("decir".equals(o[0]) && !o[1].trim().isEmpty()) {
                log("   ✍ Operador: Vanessa dice: " + o[1]);
                reg.add("Operador: " + o[1]);
                hist.add(msg("assistant", o[1]));
                hablar(a.call, o[1], true, "frase del operador");
            }
        }
        return false;
    }

    /** Escuchar en vivo: sink != null activa (auto = seguir cada llamada nueva), null desactiva. */
    public void escuchar(Rtp.AudioSink sink, boolean auto) {
        monitor = sink;
        escucharAuto = sink != null && auto;
        synchronized (activas) {
            for (Activa a : activas.values()) a.call.rtp.setMonitor(null);
        }
        escuchando = null;
        if (sink != null) {
            Activa u = ultimaActiva();
            if (u != null) { u.call.rtp.setMonitor(sink); escuchando = u.clave; }
        }
        host.estadoCambio();
    }

    public boolean escuchando() { return monitor != null; }

    private static String recortar(String s) { return s.length() > 200 ? s.substring(0, 200) : s; }

    /** Para la pantalla: resumen de estados. */
    public String resumen() {
        Map<String, Integer> cuenta = new LinkedHashMap<>();
        List<Cliente> l = clientes();
        for (Cliente c : l) cuenta.merge(c.estado, 1, Integer::sum);
        StringBuilder sb = new StringBuilder("Total: " + l.size());
        for (Map.Entry<String, Integer> e : cuenta.entrySet()) sb.append(" · ").append(e.getKey()).append(": ").append(e.getValue());
        return sb.toString();
    }

    static String grupos(String d) {
        Matcher m = Pattern.compile("(\\d{3})(\\d{3})(\\d{3})").matcher(d);
        return m.matches() ? m.group(1) + " " + m.group(2) + " " + m.group(3) : d;
    }
}
