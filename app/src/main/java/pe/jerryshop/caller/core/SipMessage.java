package pe.jerryshop.caller.core;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Mensaje SIP (petición o respuesta) con lectura tolerante de cabeceras. */
public final class SipMessage {
    public String method;      // si es petición
    public String uri;         // si es petición
    public int code;           // si es respuesta
    public String reason = "";
    public final Map<String, List<String>> headers = new LinkedHashMap<>();
    public String body = "";

    private static final Map<String, String> COMPACT = new LinkedHashMap<>();
    static {
        COMPACT.put("v", "via"); COMPACT.put("f", "from"); COMPACT.put("t", "to"); COMPACT.put("i", "call-id");
        COMPACT.put("m", "contact"); COMPACT.put("l", "content-length"); COMPACT.put("c", "content-type");
        COMPACT.put("k", "supported");
    }

    public boolean isRequest() { return method != null; }

    public static SipMessage parse(byte[] data, int len) {
        String s = new String(data, 0, len, StandardCharsets.UTF_8);
        int sep = s.indexOf("\r\n\r\n");
        String head = sep >= 0 ? s.substring(0, sep) : s;
        SipMessage m = new SipMessage();
        m.body = sep >= 0 ? s.substring(sep + 4) : "";
        String[] lines = head.split("\r\n");
        if (lines.length == 0 || lines[0].trim().isEmpty()) return null;
        String first = lines[0];
        if (first.startsWith("SIP/2.0 ")) {
            String[] p = first.split(" ", 3);
            try { m.code = Integer.parseInt(p[1].trim()); } catch (Exception e) { return null; }
            m.reason = p.length > 2 ? p[2] : "";
        } else {
            String[] p = first.split(" ");
            if (p.length < 3 || !p[2].startsWith("SIP/")) return null;
            m.method = p[0].toUpperCase(Locale.ROOT);
            m.uri = p[1];
        }
        String last = null;
        for (int i = 1; i < lines.length; i++) {
            String l = lines[i];
            if ((l.startsWith(" ") || l.startsWith("\t")) && last != null) { // cabecera multilínea
                List<String> vs = m.headers.get(last);
                vs.set(vs.size() - 1, vs.get(vs.size() - 1) + " " + l.trim());
                continue;
            }
            int c = l.indexOf(':');
            if (c <= 0) continue;
            String name = l.substring(0, c).trim().toLowerCase(Locale.ROOT);
            if (COMPACT.containsKey(name)) name = COMPACT.get(name);
            String val = l.substring(c + 1).trim();
            List<String> vs = m.headers.computeIfAbsent(name, k -> new ArrayList<>());
            if (name.equals("via") || name.equals("record-route") || name.equals("route")) {
                for (String part : splitComma(val)) vs.add(part.trim());
            } else vs.add(val);
            last = name;
        }
        return m;
    }

    /** Separa por comas fuera de <> y comillas. */
    static List<String> splitComma(String v) {
        List<String> out = new ArrayList<>();
        int depth = 0; boolean q = false; int start = 0;
        for (int i = 0; i < v.length(); i++) {
            char ch = v.charAt(i);
            if (ch == '"') q = !q;
            else if (!q && ch == '<') depth++;
            else if (!q && ch == '>') depth--;
            else if (!q && depth == 0 && ch == ',') { out.add(v.substring(start, i)); start = i + 1; }
        }
        out.add(v.substring(start));
        return out;
    }

    public String h(String name) {
        List<String> v = headers.get(name.toLowerCase(Locale.ROOT));
        return v == null || v.isEmpty() ? "" : v.get(0);
    }

    public List<String> all(String name) {
        List<String> v = headers.get(name.toLowerCase(Locale.ROOT));
        return v == null ? new ArrayList<>() : v;
    }

    public String callId() { return h("call-id"); }

    public int cseqNum() {
        try { return Integer.parseInt(h("cseq").trim().split("\\s+")[0]); } catch (Exception e) { return 0; }
    }

    public String cseqMethod() {
        String[] p = h("cseq").trim().split("\\s+");
        return p.length > 1 ? p[1].toUpperCase(Locale.ROOT) : "";
    }

    /** Parámetro de una cabecera, p. ej. param(h("via"), "branch"). */
    public static String param(String header, String name) {
        Matcher m = Pattern.compile(";\\s*" + Pattern.quote(name) + "(?:=([^;,>\\s]*))?", Pattern.CASE_INSENSITIVE)
                .matcher(stripUri(header));
        if (!m.find()) return null;
        return m.group(1) == null ? "" : m.group(1);
    }

    /** Quita el <uri> para que los parámetros de la URI no se confundan con los de la cabecera. */
    private static String stripUri(String h) {
        int a = h.indexOf('<'), b = h.indexOf('>');
        return a >= 0 && b > a ? h.substring(0, a) + h.substring(b + 1) : h;
    }

    public static String tag(String fromOrTo) {
        String t = param(fromOrTo, "tag");
        return t == null ? "" : t;
    }

    /** Extrae la URI sip:... de una cabecera tipo From/To/Contact. */
    public static String uriOf(String header) {
        int a = header.indexOf('<'), b = header.indexOf('>');
        if (a >= 0 && b > a) return header.substring(a + 1, b);
        String s = header.trim();
        int sc = s.indexOf(';');
        return sc > 0 ? s.substring(0, sc) : s;
    }

    /** Lee el valor de un parámetro de autenticación Digest (realm, nonce, qop, opaque...). */
    public static String authParam(String header, String name) {
        Matcher m = Pattern.compile("(?i)\\b" + Pattern.quote(name) + "\\s*=\\s*(\"([^\"]*)\"|([^,\\s]+))").matcher(header);
        if (!m.find()) return null;
        return m.group(2) != null ? m.group(2) : m.group(3);
    }

    public String summary() {
        return isRequest() ? method + " " + uri : code + " " + reason;
    }
}
