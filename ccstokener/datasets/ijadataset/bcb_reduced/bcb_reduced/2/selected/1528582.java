package com.chuidiang.descargador;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clase encargada de la descarga de ficheros y de analizar el contenido de un
 * html buscando enlaces a ficheros.<br>
 * Posiblemente esta clase deba separarse en dos, una para el analis de html y
 * otra para la gestion de descargas.<br>
 * 
 * @author chuidiang
 */
public class Descargador {

    /**
	 * Datos para descargar un fichero: su url y el path donde debe guardarse.
	 * 
	 * @author chuidiang
	 * 
	 */
    public class ASalvar {

        public URL url;

        public String fichero;
    }

    /**
	 * Clase con los datos de una url descompuesto en trozos: protocolo y host,
	 * path y fichero.
	 * 
	 * @author chuidiang
	 * 
	 */
    public class URLDescompuesta {

        /** Protocolo mas host */
        public String protocoloHost = null;

        /**
		 * Parte del path de la url, quitando protocolo, host y el fichero.
		 */
        public String path = null;

        /** Nombre del fichero en la url */
        public String fichero = null;

        /**
		 * Devuelve un String con todas las partes de la url concatenadas.
		 */
        public String toString() {
            return protocoloHost + " " + path + " " + fichero;
        }
    }

    /**
	 * Observadores del proceso de descarga.
	 */
    private LinkedList<IfzObservadorDescarga> observadores = new LinkedList<IfzObservadorDescarga>();

    /**
	 * Configuracion en la que esta para cada extension de fichero, donde debe
	 * almacenarse.
	 */
    private Hashtable<String, String> configuracion;

    /**
	 * Lista de ficheros a descargar.
	 */
    private LinkedList<ASalvar> lista = new LinkedList<ASalvar>();

    /**
	 * Constructor, arranca un hilo de descargas.
	 */
    public Descargador() {
        new Thread() {

            public void run() {
                arrancaHiloDescargas();
            }
        }.start();
    }

    /**
	 * Registra un observador para avisarle de cambios en la lista de descargas.
	 * 
	 * @param o
	 */
    public void addObservador(IfzObservadorDescarga o) {
        observadores.add(o);
    }

    /**
	 * Anade un fichero a la lista de descargas.
	 * 
	 * @param a
	 */
    public void anhadeASalvar(ASalvar a) {
        synchronized (lista) {
            lista.add(a);
            avisaAnhadidoListaDescargas(a.url.toString());
            lista.notify();
        }
    }

    /**
	 * Convierte un href relativo en absoluto.<br>
	 * Se le pasa una urlDescompuesta con los datos del Host de donde se esta
	 * intentando descargar los ficheros y un String con un href extraido
	 * directamente de la pagina html.<br>
	 * Revisa si href es ya una url absoluta (empieza por http) y si no lo es,
	 * le anade la parte de protocolo y host de urlDescompuesta. Devuelve una
	 * href absoluta.
	 * 
	 * @param urlDescompuesta
	 *            URL de la pagina html de la que se intentan descargar los
	 *            ficheros.
	 * @param href
	 *            un enlace extraido directamente del html.
	 * @return el href convertido a url absoluta.
	 */
    private String convierteHrefRelativaEnAbsoluta(URLDescompuesta urlDescompuesta, String href) {
        if (href.toLowerCase().startsWith("http")) return href;
        if (href.startsWith("/")) return urlDescompuesta.protocoloHost + href;
        return urlDescompuesta.protocoloHost + urlDescompuesta.path + "/" + href;
    }

    /**
	 * Comienza el hilo de descargas.<br>
	 * El hilo es un bucle infinito que est� a la espera de que se anada algo al
	 * hilo de descargas. Una vez anadido, comienza a descargarlo en el
	 * directorio que indice configuracion para la extension concreta del
	 * fichero.
	 */
    public void arrancaHiloDescargas() {
        while (true) {
            ASalvar a = null;
            try {
                synchronized (lista) {
                    if (lista.size() == 0) lista.wait();
                    a = lista.get(0);
                    lista.remove(0);
                }
                avisaComienzaDescarga(a.url.toString());
                a.fichero = getPathYNombreNoExistente(new File(a.fichero)).getAbsolutePath();
                System.out.println(a.url.toString() + " -> " + a.fichero);
                URLConnection urlcon = a.url.openConnection();
                urlcon.connect();
                InputStream is = urlcon.getInputStream();
                byte[] buffer = new byte[1000];
                int leido = is.read(buffer);
                FileOutputStream fw = new FileOutputStream(a.fichero);
                while (leido > 0) {
                    fw.write(buffer, 0, leido);
                    leido = is.read(buffer);
                }
                fw.close();
                is.close();
                avisaTerminadaDescarga(a.url.toString(), true, null);
            } catch (Exception e) {
                if (null != a.url) {
                    avisaTerminadaDescarga(a.url.toString(), false, e);
                }
                e.printStackTrace();
            }
        }
    }

    /**
	 * Avisa a los observadores que se ha anadido un nuevo fichero a la lista de
	 * descargas.
	 * 
	 * @param fichero
	 */
    private void avisaAnhadidoListaDescargas(String fichero) {
        for (IfzObservadorDescarga o : observadores) o.anhadidoAListaDescargas(fichero);
    }

    /**
	 * Avisa a los observadores que empieza una descarga.
	 * 
	 * @param fichero
	 */
    private void avisaComienzaDescarga(String fichero) {
        for (IfzObservadorDescarga o : observadores) o.comienzaDescarga(fichero);
    }

    /**
	 * Avis a los observadores que ha finalizado una descarga.
	 * 
	 * @param fichero
	 * @param exito
	 *            <code>true</code> si se ha finalizado con exito,
	 *            <code>false</code> en caso contrario.
	 * @param e
	 *            <code>Exception</code> en caso de error.
	 */
    private void avisaTerminadaDescarga(String fichero, boolean exito, Exception e) {
        for (IfzObservadorDescarga o : observadores) o.descargaTerminada(fichero, exito, e);
    }

    /**
	 * Se pasa una url de una pagina html y se analiza para anadir todos los
	 * ficheros que enlaza esta pagina a la lista de descargas.
	 * 
	 * @param textoUrl
	 */
    public void descarga(String textoUrl) {
        URL url;
        try {
            url = new URL(textoUrl);
            String texto = getTextoUrl(url);
            List<String> enlaces = getEnlaces(url, texto);
            List<ASalvar> ficheros = getFicheros(enlaces);
            for (ASalvar a : ficheros) {
                anhadeASalvar(a);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Devuelve la configuracion.
	 * 
	 * @return El Hashtable con la configuracion
	 */
    public Hashtable<String, String> getConfiguracion() {
        return configuracion;
    }

    /**
	 * Devuelve el directorio de descarga adecuado en funcion de la extension
	 * del fichero. Usa la configuracion para determinar este directorio.
	 * 
	 * @param fichero
	 * @return
	 */
    private String getDirectorioDescarga(String fichero) {
        Enumeration<String> extensiones = configuracion.keys();
        while (extensiones.hasMoreElements()) {
            String extension = extensiones.nextElement();
            if (fichero.endsWith(extension)) return configuracion.get(extension);
        }
        return null;
    }

    /**
	 * Devuelve la lista de href dentro de la pagina html que se le pasa. Se le
	 * pasa la direccion de la pagina original de forma que pueda devolver todos
	 * los enlaces como referencias absolutas, anadiendo la url original a los
	 * enlaces relativos.
	 * 
	 * @param url
	 *            Direccion de la pagina original
	 * @param textoHtml
	 *            Texto html de la pagina.
	 * @return Lista de enlaces absolutos.
	 */
    public List<String> getEnlaces(URL url, String textoHtml) {
        LinkedList<String> enlaces = new LinkedList<String>();
        URLDescompuesta urlDescompuesta = getURLDescompuesta(url);
        Pattern pBase = Pattern.compile("< *[Bb][Aa][Ss][Ee] *[hH][rR][eE][fF] *= *[\"']?([^ \"'>])[\"'>]");
        Matcher mBase = pBase.matcher(textoHtml);
        if (mBase.find()) {
            System.out.println(mBase.group(1));
        }
        Pattern pHref = Pattern.compile("[hH][rR][eE][fF] *= *[\"']?([^ \"'>]*)[\"'>]");
        Matcher mHref = pHref.matcher(textoHtml);
        while (mHref.find()) {
            String href = convierteHrefRelativaEnAbsoluta(urlDescompuesta, mHref.group(1));
            enlaces.add(href.replace("\\", "/"));
        }
        return enlaces;
    }

    /**
	 * Se le pasa una lista de enlaces a ficheros y devuelve aquellos cuya
	 * extension esta contemplada en el fichero de configuracion, junto con su
	 * path de descarga asociado.
	 * 
	 * @param hrefs
	 *            Lista de enlaces de ficheros.
	 * @return Lista de enlaces cuya extension es conocida por configuracion
	 */
    public List<ASalvar> getFicheros(List<String> hrefs) {
        LinkedList<ASalvar> aSalvar = new LinkedList<ASalvar>();
        for (String href : hrefs) {
            String pathGuardar = getDirectorioDescarga(href);
            if (null != pathGuardar) {
                String nombreFichero;
                if (-1 != href.lastIndexOf("/")) {
                    nombreFichero = href.substring(href.lastIndexOf("/") + 1);
                    pathGuardar = pathGuardar + nombreFichero;
                    try {
                        ASalvar a = new ASalvar();
                        a.url = new URL(href);
                        a.fichero = pathGuardar;
                        aSalvar.add(a);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return aSalvar;
    }

    /**
	 * Busca un nombre de fichero que no exista para hacer la descarga.<br>
	 * Cuando se intenta descargar un fichero, se intenta guardar con el mismo
	 * nombre en el directorio de descargas. Si ya hay un fichero con ese
	 * nombre, para no machacarlo, se busca otro nombre. Para ello, se va
	 * anadiendo (1), (2), (3)... al nombre del fichero hasta encontrar uno que
	 * no exista. Ejemplo, fichero.txt, fichero(1).txt, fichero(2).txt, etc.
	 * 
	 * @param f
	 *            Fichero que se quiere guardar
	 * @return Fichero que no existe.
	 */
    private File getPathYNombreNoExistente(File f) {
        int contador = 1;
        String[] nombreExt = f.getName().split("[.]");
        while (f.exists()) {
            f = new File(f.getParent() + "/" + nombreExt[0] + "(" + contador + ")." + nombreExt[1]);
            contador++;
        }
        return f;
    }

    /**
	 * Se le pasa una url y devuelve un String con todo el texto html leido de
	 * esa url.
	 * 
	 * @param url
	 *            Una url de una pagina html (o con contenido de texto).
	 * @return El texto completo leido de la url.
	 */
    public String getTextoUrl(URL url) {
        String texto = null;
        try {
            URLConnection con = url.openConnection();
            InputStream contenido = con.getInputStream();
            BufferedReader isr = new BufferedReader(new InputStreamReader(contenido));
            String linea = isr.readLine();
            while (null != linea) {
                texto += linea;
                linea = isr.readLine();
            }
            contenido.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return texto;
    }

    /**
	 * Se le pasa una URL y la devuelve descompuesta en tres cachos: protocolo y
	 * host + path del fichero + nombre del fichero. Considera que hay fichero
	 * si tiene extension. Si no tiene extension, entonces supone que es un
	 * directorio.
	 * 
	 * @param url
	 * @return
	 */
    private URLDescompuesta getURLDescompuesta(URL url) {
        URLDescompuesta temp = new URLDescompuesta();
        temp.protocoloHost = url.getProtocol() + "://" + url.getHost();
        File f = new File(url.getPath());
        temp.path = f.getParent();
        temp.fichero = f.getName();
        if (-1 == temp.fichero.indexOf(".")) {
            temp.path += "/" + temp.fichero;
            temp.fichero = null;
        }
        return temp;
    }

    /**
	 * Elimina al observador de la lista de observadores.
	 * 
	 * @param o
	 */
    public void removeObservador(IfzObservadorDescarga o) {
        observadores.remove(o);
    }

    /**
	 * Se le pasa la lista de extension y paths asociados de descarga. Las
	 * claves del Hashtable son las extensiones, los valores son los paths.
	 * 
	 * @param configuracion
	 */
    public void setConfiguracion(Hashtable<String, String> configuracion) {
        this.configuracion = configuracion;
    }
}
