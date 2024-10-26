package org.lm2a.client;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;
import org.lm2a.exceptions.CoverlessException;

public class XResumeControlHTTP {

    private static final int PUERTO = 56789;

    private static final int BUFFER = 512;

    private URL url;

    private FileOutputStream fout;

    private String theFile = null;

    private URLConnection urlConnection;

    private boolean conectado;

    private ServerSocket conex;

    private int longitudArchivo = 0;

    int offset = 0;

    int bytesread = 0;

    byte[] b = null;

    private InputStream archivoInputStream;

    int puntoInterrupcion = 0;

    int x = 15550;

    public XResumeControlHTTP(String[] argumentos) {
        if ((argumentos.length != 1)) {
            throw new IllegalArgumentException("N�mero equivocado de argumentos");
        }
        try {
            url = new URL(argumentos[0]);
            String protocol = url.getProtocol();
            if (!protocol.equals("http")) {
                throw new IllegalArgumentException("URL debe usar el protocolo 'http:'");
            }
        } catch (MalformedURLException e) {
            error(url + " no es una buena URL.");
            e.printStackTrace();
        }
        urlParse(url);
    }

    public void urlParse(URL u) {
        theFile = u.getFile();
        ver("Archivo: " + theFile);
        theFile = theFile.substring(theFile.lastIndexOf('/') + 1);
        try {
            fout = new FileOutputStream(theFile);
        } catch (FileNotFoundException fnfe) {
            error("ERROR #RC3: Problemas al crear el archivo de salida");
            fnfe.printStackTrace();
        }
    }

    public void proceso() {
        try {
            abreConeccion();
        } catch (IOException e) {
            e.printStackTrace();
        }
        b = new byte[longitudArchivo];
        try {
            leeDesde(x);
        } catch (CoverlessException cle) {
            setConexion(false);
            urlConnection = null;
            error("Error: Palmo cuando intente leer la posicion " + offset + " bytes");
            try {
                archivoInputStream.close();
            } catch (IOException e) {
                ver("Excepcion al hacer el close");
                e.printStackTrace();
            }
            tryConnection(offset);
            cle.printStackTrace();
        }
    }

    public URLConnection abreConeccion() throws IOException {
        ver("Intentando crear el socket");
        urlConnection = url.openConnection();
        urlConnection.setDoOutput(true);
        urlConnection.setAllowUserInteraction(true);
        if (urlConnection != null) {
            setConexion(true);
            archivoInputStream = requestHTTP(url, urlConnection);
            longitudArchivo = leerLongitudArchivo();
        }
        return urlConnection;
    }

    public int leerLongitudArchivo() {
        return urlConnection.getContentLength();
    }

    public void escribirArchivo(byte[] b, int off, int len) {
        try {
            fout.write(b, off, len);
        } catch (IOException ioe) {
            error("ERROR #RC4: Problemas al escribir el archivo de salida");
            ioe.printStackTrace();
        } catch (IndexOutOfBoundsException iobe) {
            error("Error de overflow para off:  " + off + "len: " + len);
        }
    }

    public void abrirServerSocket() {
        try {
            conex = new ServerSocket(PUERTO);
        } catch (IOException e) {
            error("ERROR #RC6: Problemas al abrir el socket de servicio");
            e.printStackTrace();
        }
    }

    public void tryConnection(int d) {
        int delay = 5000;
        int period = 15000;
        final Timer timer = new Timer();
        ver("ver d: " + d);
        timer.scheduleAtFixedRate(new TimerTask() {

            public void run() {
                try {
                    urlConnection = abreConeccion();
                    urlConnection.getInputStream();
                    ver("Re-Conectado!!!");
                    if (conectado) {
                        procesoReConexion();
                        timer.cancel();
                    }
                } catch (IOException e) {
                    error("Intento de coneccion fallido");
                }
            }
        }, delay, period);
    }

    public void procesoReConexion() {
        puntoInterrupcion = offset;
        verArray(b, puntoInterrupcion, puntoInterrupcion + 10);
        ver("re-conexi�n + d: " + offset);
        try {
            bytesread = 0;
            ver("descartando " + (offset));
            try {
                requestRangedHTTP(url, urlConnection, offset + "", longitudArchivo + "");
            } catch (IOException e) {
                error("Problema con el requestRangedHTTP");
                e.printStackTrace();
            }
            leeDesde(offset);
        } catch (CoverlessException e) {
            error("Problema: CoverlessException");
            e.printStackTrace();
        }
    }

    public void leeDesde(int posicion) throws CoverlessException {
        offset = posicion;
        ver("LEYENDO");
        ver("bytesread" + bytesread);
        ver("offset" + offset);
        int chunk = BUFFER;
        while (bytesread >= 0) {
            if ((longitudArchivo - offset) < BUFFER) {
                chunk = longitudArchivo - offset;
            }
            try {
                bytesread = archivoInputStream.read(b, offset, chunk);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (bytesread == -1) {
                ver("Archivo leido");
                break;
            }
            if (bytesread == 0) {
                ver("Archivo leido 0");
                break;
            }
            ver("escribiendo en offset: " + offset + " | chunk: " + chunk + " | bytesread: " + bytesread);
            escribirArchivo(b, offset, bytesread);
            offset += bytesread;
        }
        if (offset != longitudArchivo) {
            throw new CoverlessException("ERROR #RC7: Problemas de cobertura" + "\n longitud de archivo le�da = " + longitudArchivo + "\n Offset = " + offset);
        }
    }

    public void verArray(byte[] b, int desde, int cuanto) {
        error(" Desde: " + desde + "  hasta: " + cuanto);
        String x = new String(b);
        String y = x.substring(desde, cuanto);
        error(y);
    }

    public void setConexion(boolean valor) {
        conectado = valor;
    }

    public boolean getConexion() {
        return conectado;
    }

    public void error(String mje) {
        System.err.println(mje);
    }

    public void ver(String mje) {
        System.out.println(mje);
    }

    public InputStream requestHTTP(URL _url, URLConnection _urlConnection) throws IOException {
        PrintWriter to_server = new PrintWriter(new OutputStreamWriter(_urlConnection.getOutputStream()));
        InputStream from_server = _urlConnection.getInputStream();
        String filename = _url.getFile();
        to_server.println("GET " + filename + " HTTP/1.1");
        to_server.println("Host: " + _url.getHost());
        to_server.flush();
        return from_server;
    }

    public InputStream requestRangedHTTP(URL _url, URLConnection _urlConnection, String desde, String hasta) throws IOException {
        PrintWriter to_server = new PrintWriter(new OutputStreamWriter(_urlConnection.getOutputStream()));
        InputStream from_server = _urlConnection.getInputStream();
        String filename = _url.getFile();
        to_server.println("GET " + filename + " HTTP/1.1");
        to_server.println("Accept: */*");
        to_server.println("Range: bytes=" + desde + "-" + hasta);
        to_server.println("Host: " + _url.getHost());
        to_server.flush();
        return from_server;
    }
}
