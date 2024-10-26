package apps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import met.Metodos;

public class Fotolog {

    public void mainFotolog(Properties props, String cfg, String load, String apps) {
        String debug = props.getProperty("fotolog_debug");
        String verbose = props.getProperty("fotolog_verbose");
        String showURL = props.getProperty("fotolog_showURL");
        String fotoindv = props.getProperty("fotolog_fotoindv");
        String updateFlog = props.getProperty("fotolog_update");
        String fotologtxt = props.getProperty("fotolog_txt");
        System.out.println("");
        System.out.println("--------------------------");
        System.out.println("Inicio SubPrograma Fotolog");
        System.out.println("--------------------------");
        int cantFlogs = 100;
        String arrayFlogs[] = new String[cantFlogs];
        int cont = 0;
        this.cargaFotologstxt(arrayFlogs, cfg, load, apps, fotologtxt, cantFlogs, verbose, debug);
        while (arrayFlogs[cont] != null) {
            this.downFotolog(arrayFlogs, cont, cfg, load, apps, fotoindv, updateFlog, verbose, debug, showURL);
            cont++;
        }
        if (arrayFlogs[0] == null) {
            System.out.println("Nada para hacer. Continuando con el siguiente modulo.");
        }
    }

    private void cargaFotologstxt(String[] arrayFlogs, String cfg, String load, String apps, String fotologtxt, int cantFlogs, String verbose, String debug) {
        int cont = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(load + '/' + fotologtxt));
            String linea = "";
            while (((linea = reader.readLine()) != null) && (cont < cantFlogs)) {
                arrayFlogs[cont] = linea;
                cont++;
            }
            reader.close();
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        cont = 0;
        if (debug.equalsIgnoreCase("1")) {
            System.out.println("Se cargaron los siguientes Fotologs:");
            while (arrayFlogs[cont] != null) {
                System.out.print(arrayFlogs[cont]);
                cont++;
                if (arrayFlogs[cont] == null) System.out.print(","); else System.out.print(".");
            }
        }
        System.out.println("");
    }

    private void downFotolog(String[] arrayFlogs, int cont, String cfg, String load, String apps, String fotoindv, String updateFlog, String verbose, String debug, String showURL) {
        Metodos met = new Metodos();
        System.out.println("Descargando Fotolog.com/" + arrayFlogs[cont]);
        File file = new File(apps + "/Fotologs/" + arrayFlogs[cont]);
        if (file.exists()) {
            if (verbose.equalsIgnoreCase("1")) System.out.println("El Directorio ya existe. Utilizando el mismo directorio.");
        } else {
            boolean success = file.mkdir();
            if (success) {
                if (verbose.equalsIgnoreCase("1")) System.out.println("Directorio " + arrayFlogs[cont] + " Creado");
            } else {
                if (verbose.equalsIgnoreCase("1")) System.out.println("Ocurri� un problema creando el directorio " + arrayFlogs[cont]);
            }
        }
        URL url = null;
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader r = null;
        String str = null;
        int prevNum = -1;
        String fotoURL = null;
        try {
            int flag = 0;
            if (debug.equalsIgnoreCase("1")) System.out.println("Flag Before: " + flag);
            url = new URL("http://www.fotolog.com/" + arrayFlogs[cont]);
            if (showURL.equalsIgnoreCase("1")) {
                is = url.openStream();
                isr = new InputStreamReader(is);
                r = new BufferedReader(isr);
                do {
                    str = r.readLine();
                    if (str != null) System.out.println(str);
                } while (str != null);
            }
            int flagUpdateFlog = 0;
            String fotoURLfinal = null;
            String prevURLfinal = null;
            String prevURL = null;
            do {
                if (debug.equalsIgnoreCase("1")) System.out.println("Flag After: " + flag);
                if (flag != 0) {
                    if (debug.equalsIgnoreCase("1")) {
                        System.out.println("prevURLfinal es " + prevURLfinal);
                        System.out.println("URL Foto Anterior Recortada es " + prevURLfinal.substring(1, prevURLfinal.length() - 1));
                    }
                    url = new URL(prevURLfinal.substring(1, prevURLfinal.length() - 1));
                }
                is = url.openStream();
                isr = new InputStreamReader(is);
                r = new BufferedReader(isr);
                if (showURL.equalsIgnoreCase("1")) {
                    is = url.openStream();
                    isr = new InputStreamReader(is);
                    r = new BufferedReader(isr);
                    do {
                        str = r.readLine();
                        if (str != null) System.out.println(str);
                    } while (str != null);
                }
                do {
                    str = r.readLine();
                    if (str != null) {
                        prevNum = str.indexOf("<div id=\"centerCol\">");
                    }
                    if (prevNum != -1) {
                        for (int i = 0; i < 3; i++) {
                            str = r.readLine();
                        }
                        prevURL = str;
                        for (int i = 0; i < 3; i++) {
                            str = r.readLine();
                        }
                        fotoURL = str;
                        str = null;
                    }
                } while (str != null);
                Pattern p = Pattern.compile("(\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\")|(  +)");
                Matcher m = p.matcher(prevURL);
                while (m.find()) {
                    prevURLfinal = m.group(1);
                }
                p = Pattern.compile("(\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\")|(  +)");
                m = p.matcher(fotoURL);
                while (m.find()) {
                    fotoURLfinal = m.group(1);
                }
                String filename = null;
                filename = fotoURLfinal.substring(fotoURLfinal.lastIndexOf('/') + 1);
                filename = filename.substring(0, filename.length() - 1);
                File fotoexist = new File(apps + "/Fotologs/" + arrayFlogs[cont] + '/' + filename);
                if (!fotoexist.exists()) {
                    if (debug.equalsIgnoreCase("1")) {
                        System.out.println("Seteando flagUpdateFlog");
                    }
                    flagUpdateFlog = 0;
                    String fotoJpeg = fotoURLfinal.substring(1, fotoURLfinal.length() - 1);
                    byte[] data = met.getURL(fotoJpeg);
                    if (fotoindv.equalsIgnoreCase("1")) {
                        System.out.println("Grabando " + filename);
                    }
                    met.saveData(apps, "Fotologs", arrayFlogs[cont], filename, data);
                } else {
                    if (updateFlog.equalsIgnoreCase("1")) {
                        System.out.println("Fotolog actualizado. Continuando con el proximo.");
                        flagUpdateFlog = 1;
                    } else {
                        if (fotoindv.equalsIgnoreCase("1")) {
                            System.out.println("Foto " + filename + " ya existe. Skipping");
                        }
                    }
                }
                flag = 1;
                if (debug.equalsIgnoreCase("1")) {
                    System.out.println("prevURLfinal es " + prevURLfinal);
                    System.out.println("prevURL es " + prevURL);
                }
            } while ((prevURL.length() > 20) && (flagUpdateFlog == 0));
        } catch (MalformedURLException e) {
            System.out.println("URL Invalida");
        } catch (IOException e) {
            System.out.println("No me pude conectar a la URL");
        }
        System.out.println("Descarga de Fotolog: " + arrayFlogs[cont] + " Finalizada.");
        System.out.println("");
    }
}
