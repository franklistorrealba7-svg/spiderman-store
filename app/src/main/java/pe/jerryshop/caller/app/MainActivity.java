package pe.jerryshop.caller.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pe.jerryshop.caller.core.Clientes;
import pe.jerryshop.caller.core.Clientes.Cliente;
import pe.jerryshop.caller.core.Engine;

/** Pantalla principal: campaña, clientes, mensaje e IA, configuración y registro. */
public final class MainActivity extends Activity {

    // Colores
    private static final int VERDE = 0xFF0F7A4F, VERDE_OSC = 0xFF0B5E3C, FONDO = 0xFFF2F5F3, TARJETA = 0xFFFFFFFF,
            TEXTO = 0xFF17201B, GRIS = 0xFF5D6A62, LINEA = 0xFFD5DDD7, ROJO = 0xFFB3261E;

    private static final int ABRIR = 1, EXPORTAR = 2;

    private App app;
    private final Handler h = new Handler(Looper.getMainLooper());
    private FrameLayout contenido;
    private final List<TextView> pestanas = new ArrayList<>();
    private final List<View> paneles = new ArrayList<>();
    private int pestanaActual;
    private long ultimoCambio = -1;

    // Campaña
    private TextView estado, resumen, vivoTitulo, logCorto;
    private Button btnIniciar, btnDetener, btnEscuchar, btnControl;
    private EditText desde, hasta, numPrueba, decir;
    private CheckBox cualquierHora;
    // Clientes
    private final List<Cliente> lista = new ArrayList<>();
    private BaseAdapter adaptador;
    private TextView resumenClientes;
    // Registro
    private TextView logTexto;
    private ScrollView logScroll;
    // Config / Mensaje
    private final Map<String, EditText> campos = new LinkedHashMap<>();
    private CheckBox iaActiva;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        app = App.get();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (Build.VERSION.SDK_INT >= 21) getWindow().setStatusBarColor(VERDE_OSC);

        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setBackgroundColor(FONDO);

        TextView cab = new TextView(this);
        cab.setText("JerryCaller · JERRY SHOP PERÚ");
        cab.setTextColor(Color.WHITE);
        cab.setTypeface(Typeface.DEFAULT_BOLD);
        cab.setTextSize(18);
        cab.setBackgroundColor(VERDE_OSC);
        cab.setPadding(dp(16), dp(14), dp(16), dp(10));
        raiz.addView(cab);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setBackgroundColor(VERDE_OSC);
        LinearLayout barra = new LinearLayout(this);
        hs.addView(barra);
        raiz.addView(hs);

        contenido = new FrameLayout(this);
        raiz.addView(contenido, new LinearLayout.LayoutParams(-1, 0, 1));

        String[] nombres = {"Campaña", "Clientes", "Mensaje e IA", "Configuración", "Registro"};
        View[] vistas = {panelCampana(), panelClientes(), panelMensaje(), panelConfig(), panelRegistro()};
        for (int i = 0; i < nombres.length; i++) {
            final int idx = i;
            TextView t = new TextView(this);
            t.setText(nombres[i]);
            t.setPadding(dp(14), dp(10), dp(14), dp(12));
            t.setTextSize(14);
            t.setOnClickListener(v -> mostrar(idx));
            barra.addView(t);
            pestanas.add(t);
            paneles.add(vistas[i]);
            contenido.addView(vistas[i], new FrameLayout.LayoutParams(-1, -1));
        }
        setContentView(raiz);
        mostrar(0);

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5);
        }
        if (app.cfg.s("sip_pass").isEmpty() || app.cfg.s("eleven_key").isEmpty()) {
            mostrar(3);
            toast("Completa la contraseña SIP y tus claves de ElevenLabs y OpenAI");
        }
    }

    private void mostrar(int idx) {
        pestanaActual = idx;
        for (int i = 0; i < paneles.size(); i++) {
            paneles.get(i).setVisibility(i == idx ? View.VISIBLE : View.GONE);
            TextView t = pestanas.get(i);
            t.setTextColor(i == idx ? Color.WHITE : 0xB3FFFFFF);
            t.setTypeface(i == idx ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            t.setBackgroundColor(i == idx ? VERDE : Color.TRANSPARENT);
        }
        ultimoCambio = -1;
    }

    @Override protected void onResume() { super.onResume(); h.post(refresco); }
    @Override protected void onPause() { super.onPause(); h.removeCallbacks(refresco); app.guardarClientes(); }

    private final Runnable refresco = new Runnable() {
        @Override public void run() {
            long c = app.cambios();
            if (c != ultimoCambio) { ultimoCambio = c; refrescar(); }
            h.postDelayed(this, 600);
        }
    };

    // ================= Campaña =================

    private View panelCampana() {
        LinearLayout col = columna();

        LinearLayout t1 = tarjeta(col);
        estado = texto("", 17, TEXTO, true);
        resumen = texto("", 14, GRIS, false);
        t1.addView(estado);
        t1.addView(resumen);
        LinearLayout f = fila(t1);
        btnIniciar = boton("▶  Iniciar campaña", true, v -> iniciar());
        btnDetener = boton("■  Detener", false, v -> app.engine.detener());
        f.addView(btnIniciar, peso());
        f.addView(btnDetener, peso());

        LinearLayout t2 = tarjeta(col);
        t2.addView(subtitulo("Horario (hora de Perú, 24 h)"));
        LinearLayout fh = fila(t2);
        desde = entrada(app.cfg.s("hora_inicio"), InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        hasta = entrada(app.cfg.s("hora_fin"), InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        fh.addView(texto("Desde", 14, GRIS, false));
        fh.addView(desde, peso());
        fh.addView(texto("hasta", 14, GRIS, false));
        fh.addView(hasta, peso());
        cualquierHora = new CheckBox(this);
        cualquierHora.setText("Llamar a cualquier hora");
        cualquierHora.setChecked(app.cfg.b("ignorar_horario"));
        t2.addView(cualquierHora);

        LinearLayout t3 = tarjeta(col);
        t3.addView(subtitulo("Llamada de prueba"));
        LinearLayout fp = fila(t3);
        numPrueba = entrada("", InputType.TYPE_CLASS_PHONE);
        numPrueba.setHint("Tu celular, ej. 987654321");
        fp.addView(numPrueba, peso());
        fp.addView(boton("📞 Probar", false, v -> probar()));

        LinearLayout t4 = tarjeta(col);
        t4.addView(subtitulo("Base de clientes"));
        LinearLayout fb = fila(t4);
        fb.addView(boton("📂 Cargar", false, v -> abrirBase()), peso());
        fb.addView(boton("💾 Exportar", false, v -> exportar()), peso());
        fb.addView(boton("🗑 Historial", false, v -> borrarHistorial()), peso());

        LinearLayout t5 = tarjeta(col);
        t5.addView(subtitulo("🎧 En vivo: escuchar y corregir"));
        vivoTitulo = texto("Sin llamada en curso", 14, GRIS, false);
        t5.addView(vivoTitulo);
        LinearLayout fv = fila(t5);
        btnEscuchar = boton("🎧 Escuchar", false, v -> alternarEscucha());
        btnControl = boton("🙋 Tomar control", false, v -> alternarControl());
        fv.addView(btnEscuchar, peso());
        fv.addView(btnControl, peso());
        LinearLayout fd = fila(t5);
        decir = entrada("", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        decir.setHint("Que Vanessa diga…");
        fd.addView(decir, peso());
        fd.addView(boton("🗣 Decir", true, v -> decir(null)));
        LinearLayout fr = fila(t5);
        fr.addView(boton("🔁 Dictar", false, v -> decir(app.cfg.s("frase_dictado"))), peso());
        fr.addView(boton("⏳ Te espero", false, v -> decir("Claro, tranquilo, aquí te espero.")), peso());
        Button colgar = boton("☎ Colgar", false, v -> app.engine.ordenar(null, "colgar", ""));
        colgar.setTextColor(ROJO);
        fr.addView(colgar, peso());

        LinearLayout t6 = tarjeta(col);
        t6.addView(subtitulo("Últimos eventos"));
        logCorto = texto("", 12, GRIS, false);
        logCorto.setTypeface(Typeface.MONOSPACE);
        t6.addView(logCorto);

        return scroll(col);
    }

    private void guardarHorario() {
        app.cfg.set("hora_inicio", desde.getText().toString().trim());
        app.cfg.set("hora_fin", hasta.getText().toString().trim());
        app.cfg.set("ignorar_horario", String.valueOf(cualquierHora.isChecked()));
    }

    private boolean faltanDatos() {
        List<String> f = new ArrayList<>();
        if (app.cfg.s("sip_user").isEmpty()) f.add("Usuario SIP");
        if (app.cfg.s("sip_pass").isEmpty()) f.add("Contraseña SIP");
        if (app.cfg.s("eleven_key").isEmpty()) f.add("API key de ElevenLabs");
        if (app.cfg.s("eleven_voz").isEmpty()) f.add("ID de la voz");
        if (app.cfg.b("ia_activa") && app.cfg.s("openai_key").isEmpty()) f.add("API key de OpenAI (o desactiva la IA)");
        if (f.isEmpty()) return false;
        new AlertDialog.Builder(this).setTitle("Faltan datos").setMessage("Completa en Configuración:\n• " + String.join("\n• ", f))
                .setPositiveButton("Ir", (d, w) -> mostrar(3)).show();
        return true;
    }

    private void iniciar() {
        guardarHorario();
        if (faltanDatos()) return;
        List<Cliente> l = app.engine.clientes();
        int pend = 0;
        for (Cliente c : l) if (!"Contestó".equals(c.estado)) pend++;
        if (pend == 0) { toast("Primero carga tu base de clientes"); return; }
        String cuando = app.cfg.b("ignorar_horario") ? "ahora mismo (sin horario)" : "de " + app.cfg.s("hora_inicio") + " a " + app.cfg.s("hora_fin");
        String extra = !app.cfg.b("ignorar_horario") && !app.engine.enHorario()
                ? "\n\nAhora está fuera de ese horario: empezará sola a las " + app.cfg.s("hora_inicio") + ". Deja el celular cargando." : "";
        new AlertDialog.Builder(this).setTitle("Iniciar campaña")
                .setMessage("Se llamará a " + pend + " clientes " + cuando + "." + extra)
                .setPositiveButton("Iniciar", (d, w) -> { app.engine.iniciar(false); DialerService.actualizar(this); pedirBateria(); })
                .setNegativeButton("Cancelar", null).show();
    }

    private void probar() {
        guardarHorario();
        if (faltanDatos()) return;
        if (app.engine.corriendo()) { toast("Hay una campaña en curso: deténla primero"); return; }
        if (!app.engine.probar(numPrueba.getText().toString())) { toast("Escribe un celular de 9 dígitos, ej. 987654321"); return; }
        DialerService.actualizar(this);
        mostrar(4);
    }

    private void alternarEscucha() {
        if (app.engine.escuchando()) {
            app.engine.escuchar(null, false);
            if (app.altavoz != null) { app.altavoz.cerrar(); app.altavoz = null; }
        } else {
            try {
                app.altavoz = new Altavoz();
                app.engine.escuchar(app.altavoz, true);
                toast("Escuchando por el parlante (usa audífonos para mejor sonido)");
            } catch (Exception e) {
                toast("No se pudo abrir el audio: " + e.getMessage());
            }
        }
        refrescar();
    }

    private void alternarControl() {
        Engine.Activa a = app.engine.ultimaActiva();
        if (a == null) { toast("No hay ninguna llamada contestada"); return; }
        app.engine.ordenar(a.clave, a.manual ? "auto" : "manual", "");
    }

    private void decir(String texto) {
        String t = texto != null ? texto : decir.getText().toString().trim();
        if (t.isEmpty()) return;
        if (app.engine.ordenar(null, "decir", t) && texto == null) decir.setText("");
        else if (app.engine.ultimaActiva() == null) toast("No hay ninguna llamada contestada");
    }

    private void pedirBateria() {
        if (Build.VERSION.SDK_INT < 23) return;
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm.isIgnoringBatteryOptimizations(getPackageName())) return;
        new AlertDialog.Builder(this).setTitle("Para que no se corte en segundo plano")
                .setMessage("Permite que JerryCaller funcione sin restricciones de batería. Así el celular no cortará la campaña con la pantalla apagada.")
                .setPositiveButton("Permitir", (d, w) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())));
                    } catch (Exception e) {
                        startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                    }
                }).setNegativeButton("Ahora no", null).show();
    }

    // ================= Base de clientes =================

    private void abrirBase() {
        if (app.engine.corriendo()) { toast("Detén la campaña antes de cargar otra base"); return; }
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/csv",
                "text/comma-separated-values", "text/plain", "application/octet-stream"});
        startActivityForResult(i, ABRIR);
    }

    private void exportar() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/csv");
        i.putExtra(Intent.EXTRA_TITLE, "reporte_llamadas.csv");
        startActivityForResult(i, EXPORTAR);
    }

    private void borrarHistorial() {
        int n = app.historial.numeros().size();
        if (n == 0) { toast("Aún no hay clientes que hayan contestado"); return; }
        new AlertDialog.Builder(this).setTitle("Borrar historial")
                .setMessage(n + " clientes ya contestaron y se omiten al cargar la base. ¿Borrar el historial para poder llamarlos otra vez?")
                .setPositiveButton("Borrar", (d, w) -> { app.historial.borrar(); toast("Historial borrado. Vuelve a cargar la base."); })
                .setNegativeButton("Cancelar", null).show();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (req == ABRIR) {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                Clientes.Resultado r = Clientes.cargar(nombreDe(uri), in, app.historial.numeros());
                app.reemplazarClientes(r.clientes);
                StringBuilder sb = new StringBuilder(r.clientes.size() + " clientes por llamar.");
                if (r.yaContestaron > 0) sb.append("\n\n").append(r.yaContestaron).append(" se omitieron porque YA CONTESTARON antes.");
                if (r.duplicados > 0) sb.append("\n").append(r.duplicados).append(" números repetidos quitados.");
                if (r.invalidos > 0) sb.append("\n").append(r.invalidos).append(" filas sin celular peruano válido omitidas.");
                new AlertDialog.Builder(this).setTitle("Base cargada").setMessage(sb).setPositiveButton("OK", null).show();
                mostrar(1);
            } catch (Exception e) {
                new AlertDialog.Builder(this).setTitle("No se pudo leer").setMessage(String.valueOf(e.getMessage())).setPositiveButton("OK", null).show();
            }
        } else if (req == EXPORTAR) {
            try (OutputStream o = getContentResolver().openOutputStream(uri)) {
                o.write(Clientes.reporteCsv(app.engine.clientes()).getBytes(StandardCharsets.UTF_8));
                toast("Reporte guardado");
            } catch (Exception e) {
                toast("No se pudo guardar: " + e.getMessage());
            }
        }
    }

    private String nombreDe(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return c.getString(i);
            }
        } catch (Exception ignored) {}
        return uri.getLastPathSegment() == null ? "base" : uri.getLastPathSegment();
    }

    // ================= Clientes =================

    private View panelClientes() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(12), dp(10), dp(12), 0);
        resumenClientes = texto("", 13, GRIS, false);
        resumenClientes.setPadding(dp(4), 0, dp(4), dp(8));
        col.addView(resumenClientes);
        ListView lv = new ListView(this);
        lv.setDividerHeight(dp(6));
        lv.setDivider(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        adaptador = new BaseAdapter() {
            public int getCount() { return lista.size(); }
            public Object getItem(int p) { return lista.get(p); }
            public long getItemId(int p) { return p; }
            public View getView(int p, View v, ViewGroup parent) {
                TextView t = v instanceof TextView ? (TextView) v : new TextView(MainActivity.this);
                Cliente c = lista.get(p);
                String nom = c.nombre.isEmpty() ? "(sin nombre)" : c.nombre;
                StringBuilder sb = new StringBuilder(nom).append("  ·  ").append(c.telefono).append("\n").append(c.estado);
                if (c.intentos > 0) sb.append("  ·  intentos ").append(c.intentos);
                if (!c.anoto.isEmpty()) sb.append("  ·  anotó ").append(c.anoto).append(" · guardó ").append(c.guardo);
                if (c.duracion > 0) sb.append("  ·  ").append(c.duracion).append(" s");
                t.setText(sb);
                t.setTextColor(TEXTO);
                t.setTextSize(14);
                t.setPadding(dp(14), dp(10), dp(14), dp(10));
                t.setBackground(fondo(colorEstado(c.estado), dp(10)));
                return t;
            }
        };
        lv.setAdapter(adaptador);
        lv.setOnItemClickListener((parent, view, pos, id) -> {
            Cliente c = lista.get(pos);
            String conv = c.conversacion.isEmpty() ? "Sin conversación." : c.conversacion.replace(" | ", "\n\n");
            if (!c.detalle.isEmpty()) conv += "\n\nDetalle: " + c.detalle;
            new AlertDialog.Builder(this).setTitle((c.nombre.isEmpty() ? "Cliente" : c.nombre) + " · " + c.telefono)
                    .setMessage(conv).setPositiveButton("OK", null).show();
        });
        col.addView(lv, new LinearLayout.LayoutParams(-1, 0, 1));
        return col;
    }

    private static int colorEstado(String e) {
        switch (e) {
            case "Contestó": return 0xFFD8F3E4;
            case "No contestó": return 0xFFFDE8D7;
            case "Error": return 0xFFF9D6D6;
            case "Llamando": return 0xFFFFF4C2;
            case "Reintentar": return 0xFFEEE6FF;
            default: return TARJETA;
        }
    }

    // ================= Mensaje e IA =================

    private View panelMensaje() {
        LinearLayout col = columna();
        LinearLayout t = tarjeta(col);
        iaActiva = new CheckBox(this);
        iaActiva.setText("Usar IA: dicta el número, pide que lo repita, verifica y confirma que lo guardó");
        iaActiva.setChecked(app.cfg.b("ia_activa"));
        t.addView(iaActiva);
        campo(t, "Saludo con IA (usa {nombre})", "mensaje_ia", 4, false);
        campo(t, "Mensaje sin IA (debe decir el número completo)", "mensaje", 4, false);
        campo(t, "Si el cliente se queda callado", "repregunta", 2, false);
        campo(t, "Frases mientras espera (una por línea)", "frases_espera", 3, false);
        campo(t, "Dictado del número", "frase_dictado", 2, false);
        campo(t, "Despedida", "despedida", 2, false);
        campo(t, "Instrucciones para la IA", "prompt_ia", 10, false);
        LinearLayout f = fila(t);
        f.addView(boton("Restaurar recomendados", false, v -> {
            app.cfg.restaurarTextos();
            for (String k : new String[]{"mensaje_ia", "mensaje", "repregunta", "frases_espera", "frase_dictado", "despedida", "prompt_ia"})
                campos.get(k).setText(app.cfg.s(k));
            toast("Textos recomendados restaurados");
        }), peso());
        f.addView(boton("Guardar", true, v -> guardarCampos()), peso());
        return scroll(col);
    }

    // ================= Configuración =================

    private View panelConfig() {
        LinearLayout col = columna();
        LinearLayout t1 = tarjeta(col);
        t1.addView(subtitulo("SIP (MagnusBilling)"));
        campo(t1, "Servidor (IP)", "sip_server", 1, false);
        campo(t1, "Puerto", "sip_port", 1, false);
        campo(t1, "Usuario SIP", "sip_user", 1, false);
        campo(t1, "Contraseña SIP", "sip_pass", 1, true);
        campo(t1, "Prefijo de marcado", "prefijo", 1, false);

        LinearLayout t2 = tarjeta(col);
        t2.addView(subtitulo("ElevenLabs (voz)"));
        campo(t2, "API key", "eleven_key", 1, true);
        campo(t2, "ID de la voz", "eleven_voz", 1, false);
        campo(t2, "Modelo (mensajes fijos)", "eleven_modelo", 1, false);
        campo(t2, "Modelo rápido (respuestas)", "eleven_modelo_rapido", 1, false);

        LinearLayout t3 = tarjeta(col);
        t3.addView(subtitulo("ChatGPT (OpenAI)"));
        campo(t3, "API key", "openai_key", 1, true);
        campo(t3, "Modelo", "openai_modelo", 1, false);
        campo(t3, "Modelo para entender la voz", "openai_transcripcion", 1, false);

        LinearLayout t4 = tarjeta(col);
        t4.addView(subtitulo("Campaña"));
        campo(t4, "Llamadas simultáneas (1 recomendado en celular)", "max_simultaneas", 1, false);
        campo(t4, "Reintentos si no contesta", "reintentos", 1, false);
        campo(t4, "Minutos entre reintentos", "min_entre_reintentos", 1, false);
        campo(t4, "Segundos timbrando", "seg_timbrando", 1, false);
        campo(t4, "Segundos de silencio para responder", "seg_silencio_fin", 1, false);
        campo(t4, "Sensibilidad de voz (300-800)", "umbral_voz", 1, false);
        campo(t4, "Máx. turnos de conversación", "max_turnos", 1, false);

        LinearLayout f = fila(col);
        f.addView(boton("Probar claves", false, v -> probarClaves()), peso());
        f.addView(boton("Guardar", true, v -> guardarCampos()), peso());
        LinearLayout f2 = fila(col);
        f2.addView(boton("🔋 Funcionar sin restricciones de batería", false, v -> pedirBateria()), peso());
        TextView nota = texto("Tus claves se guardan solo en este celular.", 12, GRIS, false);
        nota.setPadding(dp(4), dp(8), dp(4), dp(24));
        col.addView(nota);
        return scroll(col);
    }

    private void campo(LinearLayout padre, String etiqueta, String clave, int lineas, boolean oculto) {
        TextView l = texto(etiqueta, 13, GRIS, false);
        l.setPadding(0, dp(8), 0, dp(2));
        padre.addView(l);
        int tipo = lineas > 1 ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                : oculto ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT;
        EditText e = entrada(app.cfg.s(clave), tipo);
        if (lineas > 1) { e.setMinLines(lineas); e.setGravity(Gravity.TOP); }
        padre.addView(e);
        campos.put(clave, e);
    }

    private void guardarCampos() {
        for (Map.Entry<String, EditText> e : campos.entrySet()) {
            String v = e.getValue().getText().toString();
            if (!e.getKey().equals("prompt_ia") && !e.getKey().equals("frases_espera")) v = v.trim();
            app.cfg.set(e.getKey(), v);
        }
        app.cfg.set("ia_activa", String.valueOf(iaActiva.isChecked()));
        toast("Guardado ✔" + (app.engine.corriendo() ? " (se aplica en la próxima llamada)" : ""));
    }

    private void probarClaves() {
        guardarCampos();
        toast("Probando…");
        new Thread(() -> {
            String r = app.ai.probarClaves();
            h.post(() -> new AlertDialog.Builder(this).setTitle("Prueba de claves").setMessage(r).setPositiveButton("OK", null).show());
        }).start();
    }

    // ================= Registro =================

    private View panelRegistro() {
        logScroll = new ScrollView(this);
        logTexto = new TextView(this);
        logTexto.setTypeface(Typeface.MONOSPACE);
        logTexto.setTextSize(11);
        logTexto.setTextColor(TEXTO);
        logTexto.setTextIsSelectable(true);
        logTexto.setPadding(dp(12), dp(12), dp(12), dp(24));
        logScroll.addView(logTexto);
        return logScroll;
    }

    // ================= refresco =================

    private void refrescar() {
        Engine e = app.engine;
        boolean corre = e.corriendo();
        String st;
        if (!corre) st = "⏸  Detenida";
        else if (!e.conectado()) st = "🔄  Conectando con el SIP…";
        else if (!e.enHorario()) st = "🕘  Esperando horario (" + app.cfg.s("hora_inicio") + ")";
        else st = e.esPrueba() ? "📞  Llamada de prueba en curso" : "🟢  Campaña en curso";
        estado.setText(st);
        resumen.setText(e.resumen());
        btnIniciar.setEnabled(!corre);
        btnDetener.setEnabled(corre);
        btnIniciar.setAlpha(corre ? 0.5f : 1f);
        btnDetener.setAlpha(corre ? 1f : 0.5f);
        Engine.Activa a = e.ultimaActiva();
        vivoTitulo.setText(a == null ? "Sin llamada contestada en este momento" : "En llamada: " + a.clave + (a.manual ? "  (control manual)" : ""));
        btnEscuchar.setText(e.escuchando() ? "🔇 Dejar de escuchar" : "🎧 Escuchar");
        btnControl.setText(a != null && a.manual ? "🤖 Devolver a la IA" : "🙋 Tomar control");

        List<String> log = e.registro();
        StringBuilder corto = new StringBuilder();
        for (int i = Math.max(0, log.size() - 6); i < log.size(); i++) corto.append(log.get(i)).append('\n');
        logCorto.setText(corto.toString().trim());

        if (pestanaActual == 1) {
            lista.clear();
            lista.addAll(e.clientes());
            adaptador.notifyDataSetChanged();
            resumenClientes.setText(e.resumen() + "   ·   toca un cliente para ver la conversación");
        }
        if (pestanaActual == 4) {
            StringBuilder sb = new StringBuilder();
            for (int i = Math.max(0, log.size() - 400); i < log.size(); i++) sb.append(log.get(i)).append('\n');
            logTexto.setText(sb);
            logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
        }
    }

    // ================= ayudas de interfaz =================

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private GradientDrawable fondo(int color, int radio) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radio);
        return g;
    }

    private LinearLayout columna() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(12), dp(10), dp(12), dp(24));
        return l;
    }

    private ScrollView scroll(View v) {
        ScrollView s = new ScrollView(this);
        s.addView(v);
        return s;
    }

    private LinearLayout tarjeta(LinearLayout padre) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable g = fondo(TARJETA, dp(12));
        g.setStroke(1, LINEA);
        t.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(10);
        padre.addView(t, lp);
        return t;
    }

    private LinearLayout fila(LinearLayout padre) {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.HORIZONTAL);
        f.setGravity(Gravity.CENTER_VERTICAL);
        f.setPadding(0, dp(6), 0, 0);
        padre.addView(f, new LinearLayout.LayoutParams(-1, -2));
        return f;
    }

    private LinearLayout.LayoutParams peso() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.rightMargin = dp(6);
        return lp;
    }

    private TextView texto(String s, int sp, int color, boolean negrita) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (negrita) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(dp(2), dp(2), dp(6), dp(2));
        return t;
    }

    private TextView subtitulo(String s) {
        TextView t = texto(s, 15, VERDE_OSC, true);
        t.setPadding(0, 0, 0, dp(4));
        return t;
    }

    private EditText entrada(String valor, int tipo) {
        EditText e = new EditText(this);
        e.setText(valor);
        e.setInputType(tipo);
        e.setTextSize(15);
        e.setTextColor(TEXTO);
        return e;
    }

    private Button boton(String s, boolean principal, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(principal ? Color.WHITE : VERDE_OSC);
        GradientDrawable g = fondo(principal ? VERDE : 0xFFE6F1EB, dp(10));
        b.setBackground(g);
        b.setPadding(dp(10), dp(8), dp(10), dp(8));
        b.setOnClickListener(l);
        return b;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
