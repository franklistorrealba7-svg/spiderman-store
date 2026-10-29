import pe.jerryshop.caller.core.*;
import java.util.*;
public class T5 {
  public static void main(String[] a) throws Exception {
    Config cfg=new Config(Config.memoria());
    cfg.set("sip_server","127.0.0.1"); cfg.set("sip_port",a[0]); cfg.set("sip_pass","secreto"); cfg.set("eleven_key","x"); cfg.set("eleven_voz","v");
    cfg.set("ia_activa","false"); cfg.set("reintentos","0"); cfg.set("seg_timbrando","2"); cfg.set("hora_inicio","03:00"); cfg.set("hora_fin","03:01");
    Ai ai=new Ai(cfg,new java.io.File("/tmp/jcache")){ public byte[] voz(String t, boolean r, Trozo tr) throws Exception { byte[] b=new byte[1600]; Arrays.fill(b,(byte)0xFF); return b; } };
    Clientes.Historial hist=new Clientes.Historial(new java.io.File("/tmp/jhist5.csv")); hist.borrar();
    Engine e=new Engine(cfg,ai,hist,new Engine.Host(){ public void log(String l){ System.out.println(l);} public void clienteCambio(Clientes.Cliente c){} public void estadoCambio(){} });
    List<Clientes.Cliente> l=new ArrayList<>(); l.add(new Clientes.Cliente("Ana","900000003")); l.add(new Clientes.Cliente("Beto","900000013"));
    e.setClientes(l);
    System.out.println("MANUAL1 -> "+e.llamarManual("Rosa","+51 900 000 001"));   // fuera de horario: igual llama
    while(e.corriendo()) Thread.sleep(200);
    System.out.println("LISTA "+lista(e)+" HIST "+hist.numeros());
    cfg.set("ignorar_horario","true");
    e.iniciarCampana(); Thread.sleep(500);
    System.out.println("MANUAL2 -> "+e.llamarManual("Luis","900000021"));
    while(e.corriendo()) Thread.sleep(200);
    System.out.println("LISTA "+lista(e)+" HIST "+hist.numeros());
  }
  static String lista(Engine e){ StringBuilder sb=new StringBuilder(); for(Clientes.Cliente c:e.clientes()) sb.append(c.nombre+"/"+c.telefono+"="+c.estado+"; "); return sb.toString(); }
}
