package pe.jerryshop.caller.core;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Convierte lo que dijo el cliente ("nueve ochenta y tres, quinientos uno...") en dígitos para verificarlo. */
public final class NumerosEs {
    private NumerosEs() {}

    private static final Map<String, Integer> UNI = new HashMap<>(), ESP = new HashMap<>(), DEC = new HashMap<>(), CEN = new HashMap<>();
    static {
        String[] u = {"cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve"};
        for (int i = 0; i < u.length; i++) UNI.put(u[i], i);
        UNI.put("un", 1); UNI.put("una", 1);
        String[] e = {"diez", "once", "doce", "trece", "catorce", "quince", "dieciseis", "diecisiete", "dieciocho", "diecinueve",
                "veinte", "veintiuno", "veintidos", "veintitres", "veinticuatro", "veinticinco", "veintiseis", "veintisiete",
                "veintiocho", "veintinueve"};
        for (int i = 0; i < e.length; i++) ESP.put(e[i], 10 + i);
        ESP.put("veintiun", 21);
        String[] d = {"treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"};
        for (int i = 0; i < d.length; i++) DEC.put(d[i], 30 + 10 * i);
        String[] c = {"doscientos", "trescientos", "cuatrocientos", "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos"};
        for (int i = 0; i < c.length; i++) {
            CEN.put(c[i], 200 + 100 * i);
            CEN.put(c[i].replace("ientos", "ientas").replace("ntos", "ntas"), 200 + 100 * i);
        }
        CEN.put("cien", 100); CEN.put("ciento", 100);
    }

    private static List<String> tokens(String texto) {
        String t = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("\\d+|[a-zñ]+").matcher(t);
        while (m.find()) out.add(m.group());
        return out;
    }

    public static String aDigitos(String texto) {
        List<String> tk = tokens(texto);
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < tk.size()) {
            String t = tk.get(i);
            if (t.matches("\\d+")) { out.append(t); i++; continue; }
            if (t.equals("doble") && i + 1 < tk.size() && UNI.containsKey(tk.get(i + 1))) {
                int d = UNI.get(tk.get(i + 1)); out.append(d).append(d); i += 2; continue;
            }
            if (UNI.containsKey(t)) { out.append(UNI.get(t)); i++; continue; }
            if (CEN.containsKey(t) || DEC.containsKey(t) || ESP.containsKey(t)) {
                int valor = 0, cifras = 0;
                if (CEN.containsKey(t)) {
                    valor = CEN.get(t); cifras = 3; i++;
                    t = i < tk.size() ? tk.get(i) : "";
                }
                if (ESP.containsKey(t)) {
                    valor += ESP.get(t); if (cifras == 0) cifras = 2; i++;
                } else if (DEC.containsKey(t)) {
                    valor += DEC.get(t); if (cifras == 0) cifras = 2; i++;
                    if (i + 1 < tk.size() && tk.get(i).equals("y") && UNI.containsKey(tk.get(i + 1))) {
                        valor += UNI.get(tk.get(i + 1)); i += 2;
                    }
                } else if (cifras == 3 && UNI.containsKey(t)) {
                    valor += UNI.get(t); i++;
                }
                String v = String.valueOf(valor);
                while (v.length() < cifras) v = "0" + v;
                out.append(v);
                continue;
            }
            i++;
        }
        return out.toString();
    }

    /** "correcto", "incorrecto" o "sin_numero". [0]=estado, [1]=dígitos oídos */
    public static String[] verificar(String texto, String correcto) {
        String d = aDigitos(texto);
        if (d.length() < 6) return new String[]{"sin_numero", d};
        if (d.contains(correcto)) return new String[]{"correcto", correcto};
        return new String[]{"incorrecto", d};
    }

    public static String detallarError(String oido, String correcto) {
        String[] nombres = {"el primer grupo", "el segundo grupo", "el último grupo"};
        List<String> mal = new ArrayList<>();
        for (int k = 0; k < 3; k++) {
            String a = oido.length() >= 3 * k + 3 ? oido.substring(3 * k, 3 * k + 3) : (oido.length() > 3 * k ? oido.substring(3 * k) : "");
            if (!a.equals(correcto.substring(3 * k, 3 * k + 3))) mal.add(nombres[k]);
        }
        return mal.isEmpty() ? "algún dígito" : String.join(", ", mal);
    }

    public static String espaciado(String d) {
        StringBuilder sb = new StringBuilder();
        for (char c : d.toCharArray()) { if (sb.length() > 0) sb.append(' '); sb.append(c); }
        return sb.toString();
    }
}
