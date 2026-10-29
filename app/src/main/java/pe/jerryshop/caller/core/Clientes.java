package pe.jerryshop.caller.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Base de clientes: leer Excel/CSV, limpiar números e historial de quienes ya contestaron. */
public final class Clientes {

    public static final class Cliente {
        public final String nombre;
        public final String telefono;
        public volatile String estado = "Pendiente";
        public volatile int intentos;
        public volatile long proximo;
        public volatile String anoto = "";
        public volatile String guardo = "";
        public volatile int duracion;
        public volatile String conversacion = "";
        public volatile String detalle = "";
        public volatile String inicio = "";

        public Cliente(String nombre, String telefono) {
            this.nombre = nombre == null ? "" : nombre.trim();
            this.telefono = telefono;
        }

        public String primerNombre() {
            String[] p = nombre.trim().split("\\s+");
            if (p.length == 0 || p[0].isEmpty()) return "";
            String s = p[0];
            return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT);
        }
    }

    public static final class Resultado {
        public final List<Cliente> clientes = new ArrayList<>();
        public int invalidos, duplicados, yaContestaron;
    }

    /** Celular peruano de 9 dígitos o null. */
    public static String limpiarNumero(String crudo) {
        if (crudo == null) return null;
        String s = crudo.trim();
        if (s.matches("\\d+\\.0")) s = s.substring(0, s.length() - 2);
        if (s.matches("\\d(\\.\\d+)?[eE]\\+?\\d+")) { // Excel en notación científica
            try { s = new java.math.BigDecimal(s).toPlainString(); } catch (Exception ignored) {}
        }
        String d = s.replaceAll("\\D", "");
        if (d.startsWith("0051")) d = d.substring(4);
        else if (d.startsWith("51") && d.length() == 11) d = d.substring(2);
        return d.length() == 9 && d.startsWith("9") ? d : null;
    }

    public static Resultado cargar(String nombreArchivo, InputStream in, Set<String> yaContestaron) throws Exception {
        byte[] datos = leerTodo(in);
        List<List<String>> filas = nombreArchivo.toLowerCase(Locale.ROOT).endsWith(".xlsx")
                || (datos.length > 2 && datos[0] == 'P' && datos[1] == 'K') ? leerXlsx(datos) : leerCsv(datos);
        Resultado r = new Resultado();
        if (filas.isEmpty()) return r;
        int colNom = -1, colTel = -1;
        List<String> enc = filas.get(0);
        for (int i = 0; i < enc.size(); i++) {
            String h = enc.get(i).toLowerCase(Locale.ROOT).trim();
            if (colNom < 0 && (h.contains("nombre") || h.contains("cliente"))) colNom = i;
            if (colTel < 0 && (h.contains("tel") || h.contains("cel") || h.contains("num") || h.contains("whats") || h.contains("movil") || h.contains("móvil"))) colTel = i;
        }
        int desde = 1;
        if (colTel < 0) { // sin encabezado
            desde = 0;
            for (int i = 0; i < enc.size(); i++) if (limpiarNumero(enc.get(i)) != null) { colTel = i; break; }
            if (colTel < 0) colTel = enc.size() > 1 ? 1 : 0;
            colNom = colTel != 0 && enc.size() > 1 ? 0 : -1;
        }
        Set<String> vistos = new HashSet<>();
        for (int f = desde; f < filas.size(); f++) {
            List<String> fila = filas.get(f);
            boolean vacia = true;
            for (String v : fila) if (!v.trim().isEmpty()) { vacia = false; break; }
            if (vacia) continue;
            String tel = limpiarNumero(colTel < fila.size() ? fila.get(colTel) : "");
            String nom = colNom >= 0 && colNom < fila.size() ? fila.get(colNom) : "";
            if (tel == null) { r.invalidos++; continue; }
            if (!vistos.add(tel)) { r.duplicados++; continue; }
            if (yaContestaron != null && yaContestaron.contains(tel)) { r.yaContestaron++; continue; }
            r.clientes.add(new Cliente(nom, tel));
        }
        return r;
    }

    public static byte[] leerArchivo(File f) throws IOException {
        try (InputStream in = new java.io.FileInputStream(f)) { return leerTodo(in); }
    }

    private static byte[] leerTodo(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
        return b.toByteArray();
    }

    private static List<List<String>> leerCsv(byte[] datos) {
        String s = new String(datos, StandardCharsets.UTF_8);
        if (s.startsWith("﻿")) s = s.substring(1);
        String[] lineas = s.split("\r?\n");
        char sep = ',';
        if (lineas.length > 0) {
            String l = lineas[0];
            int comas = l.length() - l.replace(",", "").length();
            int pyc = l.length() - l.replace(";", "").length();
            int tabs = l.length() - l.replace("\t", "").length();
            if (pyc > comas && pyc >= tabs) sep = ';';
            else if (tabs > comas) sep = '\t';
        }
        List<List<String>> out = new ArrayList<>();
        for (String l : lineas) {
            List<String> campos = new ArrayList<>();
            StringBuilder cur = new StringBuilder();
            boolean q = false;
            for (int i = 0; i < l.length(); i++) {
                char c = l.charAt(i);
                if (c == '"') {
                    if (q && i + 1 < l.length() && l.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else q = !q;
                } else if (c == sep && !q) { campos.add(cur.toString()); cur.setLength(0); }
                else cur.append(c);
            }
            campos.add(cur.toString());
            out.add(campos);
        }
        return out;
    }

    /** Lee la primera hoja de un .xlsx (es un zip con XML) sin librerías. */
    private static List<List<String>> leerXlsx(byte[] datos) throws Exception {
        Map<String, byte[]> partes = new HashMap<>();
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(datos))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                String n = e.getName();
                if (n.equals("xl/sharedStrings.xml") || n.startsWith("xl/worksheets/sheet") || n.equals("xl/workbook.xml")
                        || n.equals("xl/_rels/workbook.xml.rels")) partes.put(n, leerTodo(z));
            }
        }
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(false);
        List<String> compartidos = new ArrayList<>();
        if (partes.containsKey("xl/sharedStrings.xml")) {
            Document d = f.newDocumentBuilder().parse(new ByteArrayInputStream(partes.get("xl/sharedStrings.xml")));
            NodeList sis = d.getElementsByTagName("si");
            for (int i = 0; i < sis.getLength(); i++) {
                NodeList ts = ((Element) sis.item(i)).getElementsByTagName("t");
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < ts.getLength(); k++) sb.append(ts.item(k).getTextContent());
                compartidos.add(sb.toString());
            }
        }
        String hoja = primeraHoja(f, partes);
        byte[] xml = partes.get(hoja);
        if (xml == null) throw new IOException("El Excel no tiene hojas legibles");
        Document d = f.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        NodeList rows = d.getElementsByTagName("row");
        List<List<String>> out = new ArrayList<>();
        for (int r = 0; r < rows.getLength(); r++) {
            NodeList cs = ((Element) rows.item(r)).getElementsByTagName("c");
            List<String> fila = new ArrayList<>();
            for (int k = 0; k < cs.getLength(); k++) {
                Element c = (Element) cs.item(k);
                int col = columna(c.getAttribute("r"));
                if (col < 0) col = fila.size();
                while (fila.size() < col) fila.add("");
                String t = c.getAttribute("t");
                String v;
                if ("inlineStr".equals(t)) {
                    NodeList ts = c.getElementsByTagName("t");
                    StringBuilder sb = new StringBuilder();
                    for (int x = 0; x < ts.getLength(); x++) sb.append(ts.item(x).getTextContent());
                    v = sb.toString();
                } else {
                    NodeList vs = c.getElementsByTagName("v");
                    v = vs.getLength() > 0 ? vs.item(0).getTextContent() : "";
                    if ("s".equals(t)) {
                        try { v = compartidos.get(Integer.parseInt(v.trim())); } catch (Exception ignored) { v = ""; }
                    }
                }
                if (fila.size() == col) fila.add(v); else fila.set(col, v);
            }
            out.add(fila);
        }
        return out;
    }

    private static String primeraHoja(DocumentBuilderFactory f, Map<String, byte[]> partes) {
        try {
            Document wb = f.newDocumentBuilder().parse(new ByteArrayInputStream(partes.get("xl/workbook.xml")));
            Element sheet = (Element) wb.getElementsByTagName("sheet").item(0);
            String rid = sheet.getAttribute("r:id");
            Document rels = f.newDocumentBuilder().parse(new ByteArrayInputStream(partes.get("xl/_rels/workbook.xml.rels")));
            NodeList rs = rels.getElementsByTagName("Relationship");
            for (int i = 0; i < rs.getLength(); i++) {
                Element e = (Element) rs.item(i);
                if (rid.equals(e.getAttribute("Id"))) {
                    String t = e.getAttribute("Target");
                    if (t.startsWith("/")) return t.substring(1);
                    return "xl/" + t;
                }
            }
        } catch (Exception ignored) {}
        return "xl/worksheets/sheet1.xml";
    }

    private static int columna(String ref) {
        if (ref == null || ref.isEmpty()) return -1;
        int n = 0, i = 0;
        while (i < ref.length() && Character.isLetter(ref.charAt(i))) { n = n * 26 + (Character.toUpperCase(ref.charAt(i)) - 'A' + 1); i++; }
        return n - 1;
    }

    // ================= historial =================

    public static final class Historial {
        private final File archivo;

        public Historial(File archivo) { this.archivo = archivo; }

        public synchronized Set<String> numeros() {
            Set<String> s = new LinkedHashSet<>();
            if (!archivo.exists()) return s;
            try {
                for (String l : new String(leerArchivo(archivo), StandardCharsets.UTF_8).split("\n")) {
                    String t = l.split(";", 2)[0].replace("﻿", "").trim();
                    if (t.matches("\\d{9}")) s.add(t);
                }
            } catch (IOException ignored) {}
            return s;
        }

        public synchronized void registrar(Cliente c, String fecha) {
            if (numeros().contains(c.telefono)) return;
            boolean nuevo = !archivo.exists();
            try (Writer w = new OutputStreamWriter(new FileOutputStream(archivo, true), StandardCharsets.UTF_8)) {
                if (nuevo) w.write("﻿telefono;nombre;fecha;segundos;anoto;guardo;conversacion\n");
                w.write(c.telefono + ";" + limpio(c.nombre) + ";" + fecha + ";" + c.duracion + ";" + c.anoto + ";"
                        + c.guardo + ";" + limpio(c.conversacion) + "\n");
            } catch (IOException ignored) {}
        }

        public synchronized void borrar() { archivo.delete(); }

        public File archivo() { return archivo; }

        private static String limpio(String s) { return s == null ? "" : s.replace(";", ",").replace("\n", " "); }
    }

    /** Guarda la lista y su avance (para no perderlo si se cierra la app). */
    public static void guardarEstado(List<Cliente> lista, File f) {
        StringBuilder sb = new StringBuilder();
        for (Cliente c : lista) {
            String est = "Llamando".equals(c.estado) ? "Pendiente" : c.estado;
            sb.append(c.telefono).append('\t').append(t(c.nombre)).append('\t').append(est).append('\t').append(c.intentos)
              .append('\t').append(c.anoto).append('\t').append(c.guardo).append('\t').append(c.duracion)
              .append('\t').append(t(c.conversacion)).append('\t').append(t(c.detalle)).append('\t').append(t(c.inicio)).append('\n');
        }
        try {
            File tmp = new File(f.getPath() + ".tmp");
            try (FileOutputStream o = new FileOutputStream(tmp)) { o.write(sb.toString().getBytes(StandardCharsets.UTF_8)); }
            tmp.renameTo(f);
        } catch (IOException ignored) {}
    }

    public static List<Cliente> leerEstado(File f) {
        List<Cliente> out = new ArrayList<>();
        if (!f.exists()) return out;
        try {
            for (String l : new String(leerArchivo(f), StandardCharsets.UTF_8).split("\n")) {
                String[] p = l.split("\t", -1);
                if (p.length < 10) continue;
                Cliente c = new Cliente(p[1], p[0]);
                c.estado = p[2];
                try { c.intentos = Integer.parseInt(p[3]); c.duracion = Integer.parseInt(p[6]); } catch (NumberFormatException ignored) {}
                c.anoto = p[4]; c.guardo = p[5]; c.conversacion = p[7]; c.detalle = p[8]; c.inicio = p[9];
                out.add(c);
            }
        } catch (IOException ignored) {}
        return out;
    }

    private static String t(String s) { return s == null ? "" : s.replace('\t', ' ').replace('\n', ' '); }

    /** Reporte CSV (se abre en Excel) con el resultado de cada cliente. */
    public static String reporteCsv(List<Cliente> lista) {
        StringBuilder sb = new StringBuilder("﻿nombre;telefono;estado;anoto_numero;guardo_numero;intentos;ultimo_intento;segundos;conversacion;detalle\n");
        for (Cliente c : lista) {
            sb.append(c.nombre.replace(";", ",")).append(';').append(c.telefono).append(';').append(c.estado).append(';')
              .append(c.anoto).append(';').append(c.guardo).append(';').append(c.intentos).append(';').append(c.inicio).append(';')
              .append(c.duracion).append(';').append(c.conversacion.replace(";", ",").replace("\n", " ")).append(';')
              .append(c.detalle.replace(";", ",")).append('\n');
        }
        return sb.toString();
    }
}
