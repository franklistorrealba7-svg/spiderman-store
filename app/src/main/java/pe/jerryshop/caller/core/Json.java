package pe.jerryshop.caller.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON mínimo (sin librerías) para hablar con OpenAI y ElevenLabs y guardar datos. */
public final class Json {
    private Json() {}

    public static String quote(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    @SuppressWarnings("unchecked")
    public static String write(Object o) {
        if (o == null) return "null";
        if (o instanceof String) return quote((String) o);
        if (o instanceof Number || o instanceof Boolean) return o.toString();
        if (o instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> e : ((Map<String, Object>) o).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(quote(e.getKey())).append(':').append(write(e.getValue()));
            }
            return sb.append('}').toString();
        }
        if (o instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object x : (List<Object>) o) {
                if (!first) sb.append(',');
                first = false;
                sb.append(write(x));
            }
            return sb.append(']').toString();
        }
        return quote(o.toString());
    }

    public static Object parse(String s) {
        int[] pos = {0};
        Object v = value(s, pos);
        return v;
    }

    private static void ws(String s, int[] p) {
        while (p[0] < s.length() && Character.isWhitespace(s.charAt(p[0]))) p[0]++;
    }

    private static Object value(String s, int[] p) {
        ws(s, p);
        if (p[0] >= s.length()) return null;
        char c = s.charAt(p[0]);
        if (c == '{') {
            Map<String, Object> m = new LinkedHashMap<>();
            p[0]++;
            ws(s, p);
            if (s.charAt(p[0]) == '}') { p[0]++; return m; }
            while (true) {
                ws(s, p);
                String k = (String) value(s, p);
                ws(s, p);
                p[0]++; // :
                m.put(k, value(s, p));
                ws(s, p);
                char d = s.charAt(p[0]++);
                if (d == '}') return m;
            }
        }
        if (c == '[') {
            List<Object> l = new ArrayList<>();
            p[0]++;
            ws(s, p);
            if (s.charAt(p[0]) == ']') { p[0]++; return l; }
            while (true) {
                l.add(value(s, p));
                ws(s, p);
                char d = s.charAt(p[0]++);
                if (d == ']') return l;
            }
        }
        if (c == '"') {
            StringBuilder sb = new StringBuilder();
            p[0]++;
            while (p[0] < s.length()) {
                char ch = s.charAt(p[0]++);
                if (ch == '"') break;
                if (ch == '\\') {
                    char e = s.charAt(p[0]++);
                    switch (e) {
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u': sb.append((char) Integer.parseInt(s.substring(p[0], p[0] + 4), 16)); p[0] += 4; break;
                        default: sb.append(e);
                    }
                } else sb.append(ch);
            }
            return sb.toString();
        }
        if (s.startsWith("true", p[0])) { p[0] += 4; return Boolean.TRUE; }
        if (s.startsWith("false", p[0])) { p[0] += 5; return Boolean.FALSE; }
        if (s.startsWith("null", p[0])) { p[0] += 4; return null; }
        int st = p[0];
        while (p[0] < s.length() && "+-0123456789.eE".indexOf(s.charAt(p[0])) >= 0) p[0]++;
        String num = s.substring(st, p[0]);
        try {
            if (num.contains(".") || num.contains("e") || num.contains("E")) return Double.parseDouble(num);
            return Long.parseLong(num);
        } catch (NumberFormatException e) {
            p[0] = st + 1;
            return null;
        }
    }

    /** Navega un JSON: get(obj, "choices", 0, "message", "content"). */
    @SuppressWarnings("unchecked")
    public static Object get(Object o, Object... path) {
        for (Object k : path) {
            if (o == null) return null;
            if (k instanceof Integer) {
                if (!(o instanceof List)) return null;
                List<Object> l = (List<Object>) o;
                int i = (Integer) k;
                o = i < l.size() ? l.get(i) : null;
            } else {
                if (!(o instanceof Map)) return null;
                o = ((Map<String, Object>) o).get((String) k);
            }
        }
        return o;
    }

    public static Map<String, Object> obj(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
}
