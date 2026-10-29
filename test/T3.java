import pe.jerryshop.caller.core.*;
import java.util.*;
public class T3 {
  public static void main(String[] a) throws Exception {
    Config.Store st=Config.memoria(); Config cfg=new Config(st);
    cfg.set("sip_server","127.0.0.1"); cfg.set("sip_port",a[0]); cfg.set("sip_pass","secreto"); cfg.set("openai_key","x"); cfg.set("eleven_key","x"); cfg.set("eleven_voz","v");
    cfg.set("ignorar_horario","true"); cfg.set("reintentos","0"); cfg.set("seg_timbrando","3"); cfg.set("seg_espera_cliente","2");
    Iterator<String> dichos=Arrays.asList("No tengo, espérame un momento que busco un lapicero","Ya, listo, dime","nueve ocho tres cinco cero uno seis siete ocho","nueve ochenta y tres, quinientos uno, seiscientos setenta","ya lo guardé").iterator();
    Ai ai=new Ai(cfg,new java.io.File("/tmp/jcache")){
      public byte[] voz(String t, boolean r, Trozo tr) throws Exception { byte[] b=new byte[1600]; Arrays.fill(b,(byte)0xFF); if(tr!=null) tr.recibir(b,b.length); return b; }
      public String transcribir(short[] p){ return dichos.hasNext()?dichos.next():"chau"; }
      public String responder(List<Map<String,Object>> h){
        String last=(String)h.get(h.size()-1).get("content");
        if(last.contains("INCORRECTO")) return "Casi, el último grupo es seis, siete, cero. ¿Me lo repites todo?";
        if(last.contains("CORRECTO")) return "¡Perfecto, está correcto! Guárdalo como JERRY SHOP PERÚ. ¿Ya lo guardaste? [ANOTADO]";
        if(last.contains("guardé")) return "¡Genial! Escríbenos un hola por WhatsApp. ¡Gracias! [GUARDADO] [FIN]";
        if(last.contains("busco")) return "Claro, te espero, tranquilo. [ESPERAR]";
        return "¡Listo! [DICTAR]";
      }};
    Clientes.Historial hist=new Clientes.Historial(new java.io.File("/tmp/jhist.csv")); hist.borrar();
    Engine[] e=new Engine[1];
    e[0]=new Engine(cfg,ai,hist,new Engine.Host(){
      public void log(String l){ System.out.println(l); }
      public void clienteCambio(Clientes.Cliente c){}
      public void estadoCambio(){}
    });
    List<Clientes.Cliente> l=new ArrayList<>();
    l.add(new Clientes.Cliente("maría lópez","900000001")); l.add(new Clientes.Cliente("","900000002")); l.add(new Clientes.Cliente("Luis","900000003"));
    e[0].setClientes(l); e[0].iniciar(false);
    long t=System.currentTimeMillis(); while(e[0].corriendo() && System.currentTimeMillis()-t<90000) Thread.sleep(300);
    Thread.sleep(800);
    for(Clientes.Cliente c:l) System.out.println("RES "+c.telefono+" "+c.estado+" anoto="+c.anoto+" guardo="+c.guardo+" "+c.detalle+" | "+c.conversacion);
    System.out.println("HIST "+hist.numeros());
  }
}
