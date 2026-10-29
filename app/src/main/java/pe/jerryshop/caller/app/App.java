package pe.jerryshop.caller.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.List;

import pe.jerryshop.caller.core.Ai;
import pe.jerryshop.caller.core.Clientes;
import pe.jerryshop.caller.core.Config;
import pe.jerryshop.caller.core.Engine;

/** Mantiene un único motor de llamadas vivo mientras la app (o su servicio) exista. */
public final class App extends Application {

    private static App instancia;

    public Config cfg;
    public Ai ai;
    public Clientes.Historial historial;
    public Engine engine;
    public Altavoz altavoz;
    private File archivoClientes;
    private volatile long cambios;
    private long guardado;

    @Override
    public void onCreate() {
        super.onCreate();
        instancia = this;
        final SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        cfg = new Config(new Config.Store() {
            public String get(String k) { return sp.getString(k, null); }
            public void put(String k, String v) {
                if (v == null) sp.edit().remove(k).apply(); else sp.edit().putString(k, v).apply();
            }
        });
        ai = new Ai(cfg, new File(getCacheDir(), "voz"));
        historial = new Clientes.Historial(new File(getFilesDir(), "ya_llamados.csv"));
        archivoClientes = new File(getFilesDir(), "clientes.tsv");
        engine = new Engine(cfg, ai, historial, new Engine.Host() {
            public void log(String linea) { cambios++; }
            public void clienteCambio(Clientes.Cliente c) { cambios++; }
            public void estadoCambio() { cambios++; DialerService.actualizar(App.this); }
        });
        engine.setClientes(Clientes.leerEstado(archivoClientes));
    }

    public static App get() { return instancia; }

    /** Contador que sube cuando algo cambia (la pantalla solo se redibuja si cambió). */
    public long cambios() { return cambios; }

    public void guardarClientes() {
        if (guardado == cambios) return;
        guardado = cambios;
        Clientes.guardarEstado(engine.clientes(), archivoClientes);
    }

    public void reemplazarClientes(List<Clientes.Cliente> lista) {
        engine.setClientes(lista);
        cambios++;
        Clientes.guardarEstado(lista, archivoClientes);
    }
}
