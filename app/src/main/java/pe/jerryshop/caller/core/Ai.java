package pe.jerryshop.caller.core;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Voz (ElevenLabs) y cerebro (OpenAI): todo por HTTPS directo, sin librerías. */
public class Ai {

    public interface Trozo { void recibir(byte[] b, int n) throws Exception; }

    private final Config cfg;
    private final File cache;
    private volatile String modeloTr;

    public Ai(Config cfg, File cacheDir) {
        this.cfg = cfg;
        this.cache = cacheDir;
        cacheDir.mkdirs();
        this.modeloTr = cfg.s("openai_transcripcion");
    }

    // ================= ElevenLabs =================

    private File archivoCache(String texto, String modelo) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-1").digest((cfg.s("eleven_voz") + "|" + modelo + "|v2|" + texto)
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : d) sb.append(String.format("%02x", x));
            return new File(cache, sb + ".ulaw");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Audio μ-law 8 kHz. Si 'trozo' no es null, lo va entregando mientras se genera (empieza a sonar antes). */
    public byte[] voz(String texto, boolean rapido, Trozo trozo) throws Exception {
        String modelo = rapido ? cfg.s("eleven_modelo_rapido") : cfg.s("eleven_modelo");
        if (modelo.isEmpty()) modelo = "eleven_multilingual_v2";
        File f = archivoCache(texto, modelo);
        if (f.exists() && f.length() > 0) {
            byte[] b = Clientes.leerArchivo(f);
            if (trozo != null) trozo.recibir(b, b.length);
            return b;
        }
        Map<String, Object> cuerpo = Json.obj(
                "text", texto,
                "model_id", modelo,
                "voice_settings", Json.obj("stability", 0.45, "similarity_boost", 0.8, "style", 0.15,
                        "use_speaker_boost", true, "speed", 1.0));
        HttpURLConnection c = (HttpURLConnection) new URL("https://api.elevenlabs.io/v1/text-to-speech/"
                + cfg.s("eleven_voz") + "/stream?output_format=ulaw_8000&optimize_streaming_latency=2").openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(10_000);
        c.setReadTimeout(30_000);
        c.setDoOutput(true);
        c.setRequestProperty("xi-api-key", cfg.s("eleven_key"));
        c.setRequestProperty("Content-Type", "application/json");
        try (OutputStream os = c.getOutputStream()) { os.write(Json.write(cuerpo).getBytes(StandardCharsets.UTF_8)); }
        int code = c.getResponseCode();
        if (code != 200) {
            String err = leerTexto(c.getErrorStream());
            if (rapido && !modelo.equals(cfg.s("eleven_modelo"))) return voz(texto, false, trozo); // modelo rápido no disponible
            throw new IOException("ElevenLabs " + code + ": " + recortar(err));
        }
        ByteArrayOutputStream todo = new ByteArrayOutputStream();
        try (InputStream in = c.getInputStream()) {
            byte[] buf = new byte[800]; // 0,1 s de audio
            int n;
            while ((n = in.read(buf)) > 0) {
                todo.write(buf, 0, n);
                if (trozo != null) trozo.recibir(buf, n);
            }
        }
        byte[] audio = todo.toByteArray();
        if (audio.length > 0) {
            try (FileOutputStream fo = new FileOutputStream(f)) { fo.write(audio); }
        }
        return audio;
    }

    // ================= OpenAI =================

    public String transcribir(short[] pcm) throws Exception {
        String modelo = modeloTr == null || modeloTr.isEmpty() ? "whisper-1" : modeloTr;
        String limite = "----jerry" + System.nanoTime();
        HttpURLConnection c = (HttpURLConnection) new URL("https://api.openai.com/v1/audio/transcriptions").openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(10_000);
        c.setReadTimeout(30_000);
        c.setDoOutput(true);
        c.setRequestProperty("Authorization", "Bearer " + cfg.s("openai_key"));
        c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + limite);
        try (DataOutputStream o = new DataOutputStream(c.getOutputStream())) {
            campo(o, limite, "model", modelo);
            campo(o, limite, "language", "es");
            o.writeBytes("--" + limite + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"voz.wav\"\r\n"
                    + "Content-Type: audio/wav\r\n\r\n");
            o.write(G711.wav(pcm, 8000));
            o.writeBytes("\r\n--" + limite + "--\r\n");
        }
        int code = c.getResponseCode();
        if (code != 200) {
            String err = leerTexto(c.getErrorStream());
            if (!"whisper-1".equals(modelo)) { modeloTr = "whisper-1"; return transcribir(pcm); }
            throw new IOException("OpenAI transcripción " + code + ": " + recortar(err));
        }
        Object j = Json.parse(leerTexto(c.getInputStream()));
        Object t = Json.get(j, "text");
        return filtrarAlucinacion(t == null ? "" : t.toString().trim());
    }

    public String responder(List<Map<String, Object>> mensajes) throws Exception {
        Map<String, Object> cuerpo = Json.obj("model", cfg.s("openai_modelo"), "messages", mensajes,
                "max_tokens", 110, "temperature", 0.4);
        HttpURLConnection c = (HttpURLConnection) new URL("https://api.openai.com/v1/chat/completions").openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(10_000);
        c.setReadTimeout(30_000);
        c.setDoOutput(true);
        c.setRequestProperty("Authorization", "Bearer " + cfg.s("openai_key"));
        c.setRequestProperty("Content-Type", "application/json");
        try (OutputStream os = c.getOutputStream()) { os.write(Json.write(cuerpo).getBytes(StandardCharsets.UTF_8)); }
        int code = c.getResponseCode();
        if (code != 200) throw new IOException("OpenAI chat " + code + ": " + recortar(leerTexto(c.getErrorStream())));
        Object j = Json.parse(leerTexto(c.getInputStream()));
        Object t = Json.get(j, "choices", 0, "message", "content");
        return t == null ? "" : t.toString().trim();
    }

    /** Comprueba las claves sin gastar casi nada. */
    public String probarClaves() {
        StringBuilder sb = new StringBuilder();
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("https://api.elevenlabs.io/v1/voices/" + cfg.s("eleven_voz")).openConnection();
            c.setRequestProperty("xi-api-key", cfg.s("eleven_key"));
            c.setConnectTimeout(10_000);
            int code = c.getResponseCode();
            if (code == 200) {
                Object n = Json.get(Json.parse(leerTexto(c.getInputStream())), "name");
                sb.append("✔ ElevenLabs: voz \"").append(n).append("\"\n");
            } else sb.append("✖ ElevenLabs: error ").append(code).append(code == 401 ? " (API key incorrecta)" : code == 404 ? " (ID de voz no existe)" : "").append('\n');
        } catch (Exception e) { sb.append("✖ ElevenLabs: ").append(e.getMessage()).append('\n'); }
        if (cfg.b("ia_activa")) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL("https://api.openai.com/v1/models").openConnection();
                c.setRequestProperty("Authorization", "Bearer " + cfg.s("openai_key"));
                c.setConnectTimeout(10_000);
                int code = c.getResponseCode();
                sb.append(code == 200 ? "✔ ChatGPT: OK" : "✖ ChatGPT: error " + code + (code == 401 ? " (API key incorrecta)" : ""));
            } catch (Exception e) { sb.append("✖ ChatGPT: ").append(e.getMessage()); }
        }
        return sb.toString().trim();
    }

    // ================= utilidades =================

    private static final String[] ALUCINACIONES = {
            "el cliente repite un número", "llamada de jerry shop", "subtítulos realizados", "subtitulos realizados",
            "gracias por ver", "suscríbete", "suscribete", "amara.org", "subtítulos por", "música", "www."};

    public static String filtrarAlucinacion(String texto) {
        String t = texto.toLowerCase(Locale.ROOT).replaceAll("^[\\s.¡!¿?]+|[\\s.¡!¿?]+$", "");
        if (t.length() < 2) return "";
        for (String a : ALUCINACIONES) if (t.contains(a)) return "";
        return texto;
    }

    private static void campo(DataOutputStream o, String limite, String nombre, String valor) throws IOException {
        o.writeBytes("--" + limite + "\r\nContent-Disposition: form-data; name=\"" + nombre + "\"\r\n\r\n");
        o.write(valor.getBytes(StandardCharsets.UTF_8));
        o.writeBytes("\r\n");
    }

    static String leerTexto(InputStream in) throws IOException {
        if (in == null) return "";
        try (InputStream i = in) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = i.read(buf)) > 0) b.write(buf, 0, n);
            return b.toString("UTF-8");
        }
    }

    private static String recortar(String s) { return s.length() > 200 ? s.substring(0, 200) : s; }
}
