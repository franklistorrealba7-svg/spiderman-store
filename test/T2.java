import pe.jerryshop.caller.core.*;
public class T2 { public static void main(String[] a) throws Exception {
  Sip sip=new Sip("127.0.0.1",Integer.parseInt(a[0]),"20060108",a[1],"JERRY SHOP PERU",s->System.out.println("LOG "+s));
  System.out.println("start="+sip.start());
  if(!sip.registrado()) return;
  Thread.sleep(2500);
  // 1) contesta
  Sip.Call c=sip.llamar("51900000001"); long t=System.currentTimeMillis();
  while(c.estado!=Sip.Estado.CONTESTADA && c.estado!=Sip.Estado.TERMINADA && System.currentTimeMillis()-t<8000) Thread.sleep(50);
  System.out.println("call1 estado="+c.estado+" pt="+c.rtp.payloadType());
  byte[] tono=new byte[8000]; for(int i=0;i<tono.length;i++) tono[i]=G711.linearToUlaw((int)(8000*Math.sin(i/3.0)));
  c.rtp.write(tono); Thread.sleep(1500);
  System.out.println("pendiente="+c.rtp.pendiente()+" recibidos="+c.rtp.paquetesRecibidos());
  short[] f=c.rtp.leer(500); System.out.println("frame="+(f==null?null:f.length));
  c.colgar(); System.out.println("call1 fin="+c.estado+" "+c.motivoFin);
  // 2) ocupado
  Sip.Call c2=sip.llamar("51900000002"); t=System.currentTimeMillis();
  while(c2.estado!=Sip.Estado.TERMINADA && System.currentTimeMillis()-t<5000) Thread.sleep(50);
  System.out.println("call2 "+c2.estado+" "+c2.codigoFin+" "+c2.motivoFin);
  // 3) timbra y se cancela
  Sip.Call c3=sip.llamar("51900000003"); t=System.currentTimeMillis();
  while(c3.estado!=Sip.Estado.TIMBRANDO && System.currentTimeMillis()-t<5000) Thread.sleep(50);
  System.out.println("call3 "+c3.estado); Thread.sleep(1000); c3.colgar(); Thread.sleep(800);
  System.out.println("call3 "+c3.estado+" "+c3.codigoFin);
  System.out.println("registrado="+sip.registrado());
  sip.stop(); Thread.sleep(500);
}}
