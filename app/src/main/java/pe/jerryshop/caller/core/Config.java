package pe.jerryshop.caller.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** Configuración con valores recomendados. La app la guarda en el celular (no se sube a ningún lado). */
public final class Config {

    public interface Store {
        String get(String k);
        void put(String k, String v);
    }

    public static final Map<String, String> DEFAULT = new LinkedHashMap<>();
    public static final int VERSION_TEXTOS = 4;
    public static final String NUMERO_NUEVO = "983501670";

    static {
        // SIP
        DEFAULT.put("sip_server", "51.79.109.199");
        DEFAULT.put("sip_port", "5060");
        DEFAULT.put("sip_user", "20060108");
        DEFAULT.put("sip_pass", "");
        DEFAULT.put("caller_name", "JERRY SHOP PERU");
        DEFAULT.put("prefijo", "51");
        // Voz
        DEFAULT.put("eleven_key", "");
        DEFAULT.put("eleven_voz", "");
        DEFAULT.put("eleven_modelo", "eleven_multilingual_v2");
        DEFAULT.put("eleven_modelo_rapido", "eleven_flash_v2_5");
        // IA
        DEFAULT.put("ia_activa", "true");
        DEFAULT.put("openai_key", "");
        DEFAULT.put("openai_modelo", "gpt-4o-mini");
        DEFAULT.put("openai_transcripcion", "gpt-4o-mini-transcribe");
        DEFAULT.put("max_turnos", "14");
        DEFAULT.put("seg_silencio_fin", "0.7");
        DEFAULT.put("umbral_voz", "450");
        // Campaña
        DEFAULT.put("max_simultaneas", "1");
        DEFAULT.put("hora_inicio", "09:00");
        DEFAULT.put("hora_fin", "20:00");
        DEFAULT.put("ignorar_horario", "false");
        DEFAULT.put("reintentos", "2");
        DEFAULT.put("min_entre_reintentos", "15");
        DEFAULT.put("seg_timbrando", "40");
        DEFAULT.put("seg_espera_cliente", "15");
        // Textos
        DEFAULT.put("version_textos", String.valueOf(VERSION_TEXTOS));
        DEFAULT.put("mensaje_ia",
                "Hola {nombre}, soy Vanessa, del equipo de soporte de JERRY SHOP PERÚ. "
                + "Te llamo para avisarte que este número desde el que te llamo, el nueve, uno, seis... tres, ocho, cinco... "
                + "cero, dos, ocho, quedó bloqueado para WhatsApp, y ahora tenemos un nuevo número oficial. "
                + "¿Tienes a la mano dónde anotarlo? Te lo dicto despacito.");
        DEFAULT.put("mensaje",
                "Hola {nombre}, soy Vanessa, del equipo de soporte de JERRY SHOP PERÚ. "
                + "Te comento que este número desde el que te llamo, el nueve, uno, seis... tres, ocho, cinco... "
                + "cero, dos, ocho, quedó bloqueado para WhatsApp. Nuestro nuevo número oficial es el: "
                + "nueve, ocho, tres... cinco, cero, uno... seis, siete, cero. "
                + "Te lo repito para que lo anotes: nueve, ocho, tres... cinco, cero, uno... seis, siete, cero. "
                + "Por favor, guárdalo ahora en tus contactos como JERRY SHOP PERÚ, y escríbenos un hola por WhatsApp "
                + "apenas termine esta llamada, así te seguimos atendiendo sin problemas.");
        DEFAULT.put("repregunta", "¿Sigues ahí? Tómate tu tiempo. Cuando tengas dónde anotar, me dices y te dicto el número.");
        DEFAULT.put("frases_espera", "Tranquilo, aquí sigo. Me avisas cuando puedas anotar.\n"
                + "Sin apuro, aquí te espero.\n"
                + "Aquí sigo en línea. Si quieres, también puedes anotarlo directo en tu celular.");
        DEFAULT.put("frase_dictado", "Anota por favor: nueve, ocho, tres... cinco, cero, uno... seis, siete, cero. "
                + "¿Me lo repites para confirmar que lo anotaste bien?");
        DEFAULT.put("despedida", "Te lo dejo una vez más: nueve, ocho, tres... cinco, cero, uno... seis, siete, cero. "
                + "Guárdalo como JERRY SHOP PERÚ y escríbenos un hola por WhatsApp. ¡Gracias y que tengas un buen día!");
        DEFAULT.put("prompt_ia",
                "Eres Vanessa, asistente telefónica automática de JERRY SHOP PERÚ (Perú). Estás en una LLAMADA telefónica.\n"
                + "Datos: el número antiguo 916 385 028 quedó bloqueado para WhatsApp. El nuevo número oficial de WhatsApp "
                + "es el 983 501 670.\n\n"
                + "TU MISIÓN: que el cliente ANOTE el nuevo número correctamente, lo GUARDE en sus contactos como "
                + "'JERRY SHOP PERÚ' y nos escriba un 'hola' por WhatsApp. Sigue estos pasos en orden:\n"
                + "1) LISTO PARA ANOTAR: ya le preguntaste si tiene dónde anotar. Escucha bien su respuesta:\n"
                + "   - Si dice que sí o 'dime', dicta (paso 2).\n"
                + "   - Si dice 'espera', 'un momento', 'un segundito', 'déjame buscar', 'ahorita' o similar: responde SOLO algo "
                + "corto como 'Claro, te espero, tranquilo.' y agrega la etiqueta [ESPERAR]. No dictes todavía.\n"
                + "   - Si dice 'no tengo' (papel o lapicero): dile que puede anotarlo directo en los contactos de su celular, "
                + "o que lo esperas mientras busca dónde anotar, y pregúntale qué prefiere. Si va a buscar, agrega [ESPERAR].\n"
                + "   - Si vuelve después de esperar ('ya', 'listo', 'ya tengo'), dicta (paso 2).\n"
                + "   - Si dice que no puede anotar ahora (manejando, ocupado), dicta igual una vez y pídele que lo guarde apenas pueda.\n"
                + "2) DICTAR: para dictar el número completo responde SOLO con la etiqueta [DICTAR] (puedes poner antes "
                + "una palabra corta como '¡Perfecto!'). El sistema dicta el número despacio y le pide que lo repita.\n"
                + "3) VERIFICAR: el sistema te avisará con una nota [VERIFICACIÓN] si lo que repitió el cliente es correcto o no. "
                + "Si es INCORRECTO, dile con amabilidad qué parte está mal y dicta solo esa parte otra vez, y pide que repita "
                + "todo de nuevo. Si es CORRECTO, felicítalo ('¡Perfecto, está correcto!') y agrega la etiqueta [ANOTADO].\n"
                + "4) GUARDAR: pídele que lo guarde ahora en sus contactos como 'JERRY SHOP PERÚ' y pregunta: '¿Ya lo guardaste?'. "
                + "Si dice que lo está guardando, dile que lo esperas. Cuando confirme que lo guardó, agrega la etiqueta [GUARDADO].\n"
                + "5) CIERRE: pídele que nos escriba un 'hola' por WhatsApp a ese número apenas cuelgue, agradece, "
                + "despídete y termina con [FIN].\n\n"
                + "REGLAS:\n"
                + "- Respuestas MUY cortas (1 o 2 frases), cálidas, en español peruano, tuteando. Nunca leas listas.\n"
                + "- Escribe SIEMPRE los números con palabras, dígito por dígito, en grupos de tres separados por '...'.\n"
                + "- Escucha lo que dice el cliente y responde a eso. Si pregunta algo (precios, pedidos, envíos, stock), "
                + "no inventes: dile que con gusto lo atienden al escribir al nuevo WhatsApp, y vuelve al paso en el que estabas.\n"
                + "- Si pregunta si es estafa o desconfía, explícale que puede verificarlo en las redes oficiales de JERRY SHOP PERÚ, "
                + "y que nunca le pediremos datos ni pagos por llamada.\n"
                + "- Si pregunta si eres una persona o un robot, di con honestidad que eres una asistente automática de la tienda.\n"
                + "- No insistas más de dos veces en el mismo paso. Si el cliente está ocupado, molesto o pide que no lo llamen, "
                + "discúlpate, deja el número una sola vez y termina con [FIN].\n"
                + "- Si en cualquier momento el cliente pide que esperes (por ejemplo mientras guarda el contacto), di algo corto "
                + "como 'Claro, te espero' y agrega [ESPERAR].\n"
                + "- Cada vez que haya que dictar el número completo otra vez, usa [DICTAR] en lugar de escribirlo.\n"
                + "- Etiquetas posibles: [DICTAR], [ESPERAR], [ANOTADO], [GUARDADO], [FIN]. El cliente no las escucha.");
    }

    private static final String[] TEXTOS = {"mensaje_ia", "mensaje", "repregunta", "frases_espera", "frase_dictado",
            "despedida", "prompt_ia", "max_turnos"};

    private final Store store;

    public Config(Store store) {
        this.store = store;
        int v;
        try { v = Integer.parseInt(nz(store.get("version_textos"), "0")); } catch (NumberFormatException e) { v = 0; }
        if (v < VERSION_TEXTOS) {
            for (String k : TEXTOS) store.put(k, null);
            store.put("version_textos", String.valueOf(VERSION_TEXTOS));
        }
    }

    private static String nz(String a, String b) { return a == null ? b : a; }

    public String s(String k) {
        String v = store.get(k);
        if (v == null) v = DEFAULT.get(k);
        return v == null ? "" : v;
    }

    public int i(String k) {
        try { return Integer.parseInt(s(k).trim()); } catch (NumberFormatException e) {
            try { return Integer.parseInt(DEFAULT.get(k)); } catch (Exception x) { return 0; }
        }
    }

    public double d(String k) {
        try { return Double.parseDouble(s(k).trim().replace(',', '.')); } catch (NumberFormatException e) {
            try { return Double.parseDouble(DEFAULT.get(k)); } catch (Exception x) { return 0; }
        }
    }

    public boolean b(String k) { return "true".equalsIgnoreCase(s(k).trim()); }

    public void set(String k, String v) { store.put(k, v); }

    public void restaurarTextos() { for (String k : TEXTOS) store.put(k, null); }

    /** Memoria simple (para pruebas). */
    public static Store memoria() {
        Map<String, String> m = new LinkedHashMap<>();
        return new Store() {
            public String get(String k) { return m.get(k); }
            public void put(String k, String v) { if (v == null) m.remove(k); else m.put(k, v); }
        };
    }
}
