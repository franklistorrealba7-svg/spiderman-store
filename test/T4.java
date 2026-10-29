import pe.jerryshop.caller.core.*;
import java.util.*;
public class T4 { public static void main(String[] a) throws Exception {
  for(String f:a){
    Clientes.Resultado r=Clientes.cargar(f,new java.io.FileInputStream(f),new HashSet<>(Arrays.asList("972552326")));
    System.out.println(f.substring(f.lastIndexOf('/')+1)+": "+r.clientes.size()+" inval="+r.invalidos+" dup="+r.duplicados+" ya="+r.yaContestaron+" ej="+r.clientes.get(0).nombre+"/"+r.clientes.get(0).telefono+" "+r.clientes.get(1).primerNombre()+"|"+r.clientes.get(1).telefono);
  }
  System.out.println(NumerosEs.aDigitos("novecientos ochenta y tres, quinientos uno, seiscientos setenta"));
}}
