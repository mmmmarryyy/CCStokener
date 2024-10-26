package org.lm2a.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import org.lm2a.exceptions.CoverlessException;
import player.PlayerListener;

public class ResumeControlHTTP implements PlayerListener {

    private static final int PUERTO = 5678;

    private static final int BUFFER = 512;

    private URL url;

    private FileOutputStream fout;

    private String theFile = null;

    private HttpURLConnection httpURLConnection;

    private boolean conectado;

    private ServerSocket conex;

    private int longitudArchivo = 0;

    int offset = 0;

    int bytesread = 0;

    byte[] b = null;

    private InputStream archivoInputStream;

    int puntoInterrupcion = 0;

    boolean reconnected = false;

    int estadoPlayer = 0;

    private static final int STOP = 1;

    private static final int PLAY = 2;

    private static final int PAUSE = 3;

    public void irURL(String argumento) {
        try {
            url = new URL(argumento);
            String protocol = url.getProtocol();
            if (!protocol.equals("http")) {
                throw new IllegalArgumentException("URL debe usar el protocolo 'http:'");
            }
        } catch (MalformedURLException e) {
            error("ResumeControlHTTP(String[] argumentos): " + url + " no es una buena URL.");
            e.printStackTrace();
        }
        urlParse(url);
    }

    public void urlParse(URL u) {
        theFile = u.getFile();
        ver("urlParse(URL u): Archivo = " + theFile);
        theFile = theFile.substring(theFile.lastIndexOf('/') + 1);
        try {
            fout = new FileOutputStream(theFile);
        } catch (FileNotFoundException fnfe) {
            error("urlParse(URL u): ERROR #RC3--> Problemas al crear el archivo de salida");
            fnfe.printStackTrace();
        }
    }

    public void proceso() {
        try {
            abreConeccion();
            abrirServerSocket();
        } catch (IOException e) {
            error("abrirServerSocket(): ERROR #RC6--> Problemas al abrir conexiones");
            e.printStackTrace();
        }
        b = new byte[longitudArchivo];
        try {
            leeDesde(0);
        } catch (CoverlessException cle) {
            setConexion(false);
            httpURLConnection = null;
            longitudArchivo = longitudArchivo - offset;
            error("proceso(): Error--> Palmo cuando intente leer la posicion " + offset + " bytes");
            try {
                archivoInputStream.close();
            } catch (IOException e) {
                ver("proceso(): Excepcion al hacer el close");
                e.printStackTrace();
            }
            tryConnection(offset);
            cle.printStackTrace();
        }
    }

    public HttpURLConnection abreConeccion() throws IOException {
        ver("abreConeccion(): Intentando crear el socket");
        httpURLConnection = (HttpURLConnection) url.openConnection();
        if (httpURLConnection != null) {
            setConexion(true);
            archivoInputStream = requestHTTP(url, httpURLConnection);
            longitudArchivo = leerLongitudArchivo();
            for (int i = 0; ; i++) {
                String name = httpURLConnection.getHeaderFieldKey(i);
                String value = httpURLConnection.getHeaderField(i);
                if (name == null && value == null) {
                    break;
                }
                if (name == null) {
                    System.out.println("\nServer HTTP version, Response code:");
                    System.out.println(value);
                    System.out.print("\n");
                } else {
                    System.out.println(name + "=" + value + "\n");
                }
            }
        }
        return httpURLConnection;
    }

    public HttpURLConnection reAbreConeccion() throws IOException {
        ver("reAbreConeccion(): Intentando crear el socket");
        httpURLConnection = (HttpURLConnection) url.openConnection();
        if (httpURLConnection != null) {
            setConexion(true);
            reconnected = true;
            ver("reAbreConeccion(): " + " Offset = " + offset + " LongitudArchivo = " + longitudArchivo);
            archivoInputStream = requestRangedHTTP(url, httpURLConnection, offset + "", longitudArchivo + "");
            for (int i = 0; ; i++) {
                String name = httpURLConnection.getHeaderFieldKey(i);
                String value = httpURLConnection.getHeaderField(i);
                if (name == null && value == null) {
                    break;
                }
                if (name == null) {
                    System.out.println("\nServer HTTP version, Response code:");
                    System.out.println(value);
                    System.out.print("\n");
                } else {
                    System.out.println(name + "=" + value + "\n");
                }
            }
        }
        return httpURLConnection;
    }

    public int leerLongitudArchivo() {
        return httpURLConnection.getContentLength();
    }

    public void escribirArchivo(byte[] b, int off, int len) {
        try {
            fout.write(b, off, len);
            if (reconnected) {
                ver("escribirArchivo(byte[] b, int off, int len): ");
                verArray(b, off, len);
                reconnected = false;
            }
        } catch (IOException ioe) {
            error("escribirArchivo(byte[] b, int off, int len): ERROR #RC4: Problemas al escribir el archivo de salida");
            ioe.printStackTrace();
        } catch (IndexOutOfBoundsException iobe) {
            error("escribirArchivo(byte[] b, int off, int len): Error de overflow para longitud b = " + b.length + " con off = " + off + " y len = " + len);
        }
    }

    public void abrirServerSocket() throws IOException {
        new CreaServer(PUERTO).start();
        ver("abrirServerSocket(): Disparado el thread servidor en el puerto " + PUERTO);
    }

    public void tryConnection(int d) {
        int delay = 5000;
        int period = 15000;
        final Timer timer = new Timer();
        ver("tryConnection(int d): d= " + d);
        timer.scheduleAtFixedRate(new TimerTask() {

            public void run() {
                try {
                    httpURLConnection = reAbreConeccion();
                    ver("tryConnection(int d): Re-Conectado-->" + " Offset: " + offset + " Hasta: " + longitudArchivo);
                    if (conectado) {
                        procesoReConexion();
                        timer.cancel();
                    }
                } catch (IOException e) {
                    error("tryConnection(int d): Intento de coneccion fallido");
                }
            }
        }, delay, period);
    }

    public void procesoReConexion() {
        puntoInterrupcion = offset;
        ver("procesoReConexion():");
        verArray(b, puntoInterrupcion, puntoInterrupcion + 10);
        ver("procesoReConexion(): d = " + offset);
        try {
            bytesread = 0;
            leeDesde(0);
        } catch (CoverlessException e) {
            error("procesoReConexion() problema: CoverlessException");
            e.printStackTrace();
        }
    }

    public void leeDesde(int posicion) throws CoverlessException {
        offset = posicion;
        ver("leeDesde(int posicion): bytesread = " + bytesread);
        ver("leeDesde(int posicion): offset = " + offset);
        int chunk = BUFFER;
        while (bytesread >= 0) {
            if ((longitudArchivo - offset) < BUFFER) {
                chunk = longitudArchivo - offset;
            }
            try {
                bytesread = archivoInputStream.read(b, offset, chunk);
            } catch (IOException e) {
                error("leeDesde(int posicion): IOException mientras leia " + offset + " con chunk = " + chunk);
                throw new CoverlessException("leeDesde(int posicion): ERROR #RC7--> Problemas de cobertura" + "\n longitud de archivo le�da = " + longitudArchivo + "\n Offset = " + offset);
            }
            if (bytesread == -1) {
                ver("leeDesde(int posicion): Archivo leido");
                break;
            }
            if (bytesread == 0) {
                ver("leeDesde(int posicion): Archivo leido = 0");
                break;
            }
            escribirArchivo(b, offset, bytesread);
            offset += bytesread;
        }
        if (offset != longitudArchivo) {
            throw new CoverlessException("leeDesde(int posicion): ERROR #RC7--> Problemas de cobertura" + "\n longitud de archivo le�da = " + longitudArchivo + "\n Offset = " + offset);
        }
    }

    public void verArray(byte[] b, int desde, int cuanto) {
        ver("verArray(byte[] b, int desde, int cuanto): Desde = " + desde + "  Hasta = " + cuanto);
        String x = new String(b);
        String y = x.substring(desde, cuanto);
        ver("verArray(byte[] b, int desde, int cuanto): " + y);
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

    public InputStream requestHTTP(URL _url, HttpURLConnection _httpURLConnection) throws IOException {
        _httpURLConnection.setRequestMethod("GET");
        _httpURLConnection.connect();
        InputStream is = _httpURLConnection.getInputStream();
        int code = _httpURLConnection.getResponseCode();
        if (code == HttpURLConnection.HTTP_OK) {
            ver("requestHTTP(URL _url, HttpURLConnection _httpURLConnection): Codigo devuelto = " + code);
        }
        return is;
    }

    public InputStream requestRangedHTTP(URL _url, HttpURLConnection _httpURLConnection, String desde, String hasta) throws IOException {
        _httpURLConnection.setRequestMethod("GET");
        _httpURLConnection.setRequestProperty("Accept-Ranges", "bytes");
        _httpURLConnection.setRequestProperty("Range", "bytes=" + desde + "-");
        _httpURLConnection.connect();
        InputStream is = _httpURLConnection.getInputStream();
        int code = _httpURLConnection.getResponseCode();
        if (code == HttpURLConnection.HTTP_OK) {
            ver("requestHTTP(URL _url, HttpURLConnection _httpURLConnection): Codigo devuelto = " + code);
        }
        return is;
    }

    class CreaServer extends Thread {

        int puerto;

        CreaServer(int _puerto) {
            this.puerto = _puerto;
        }

        public void run() {
            try {
                conex = new ServerSocket(PUERTO);
                ver("CreaServer:run(): Atendiendo pedidos en el puerto " + PUERTO);
                while (true) {
                    new HttpServerConexion(conex.accept()).start();
                    error("CreaServer:run(): Llego un pedido");
                }
            } catch (IOException e) {
                ver("CreaServer:run(): IOExcepcion al crear el socket y/o disparar el server thread");
                e.printStackTrace();
            }
        }
    }

    class HttpServerConexion extends Thread {

        private Socket cliente;

        private String contentType = "liculape/rebebaira";

        HttpServerConexion(Socket _cliente) throws SocketException {
            this.cliente = _cliente;
            cliente.setSoTimeout(300000);
            setPriority(NORM_PRIORITY - 1);
        }

        public void run() {
            ver("HttpServerConexion:run(): Arrancando el servicio del thread servidor");
            try {
                OutputStream out = cliente.getOutputStream();
                PrintStream outStream = new PrintStream(out);
                outStream.print("HTTP/1.0 200 OK\r\n");
                Date now = new Date();
                outStream.print("Date: " + now + "\r\n");
                outStream.print("Server: [lm2a] Server 1.0\r\n");
                outStream.print("Content-length: " + longitudArchivo + "\r\n");
                outStream.print("Content-type: " + contentType + "\r\n\r\n");
                servir(out);
                cliente.close();
            } catch (IOException e) {
                error("HttpServerConexion:run():I/O error " + e);
                e.printStackTrace();
            }
        }

        public void servir(OutputStream _out) throws IOException {
            int outOffset = 0;
            int totalEscribir = outOffset + BUFFER;
            int quantum = BUFFER;
            ver("HttpServerConexion:servir(OutputStream _out): Intento escribir " + totalEscribir + " bytes de un total de Longitud del archivo = " + longitudArchivo);
            while (outOffset < longitudArchivo) {
                if ((longitudArchivo - outOffset) < BUFFER) {
                    quantum = longitudArchivo - outOffset;
                }
                if (totalEscribir < longitudArchivo) {
                    _out.write(b, outOffset, quantum);
                    outOffset += quantum;
                    ver("HttpServerConexion:servir(OutputStream _out): Escribiendo en salida en posicion " + outOffset + " la cantidad de " + quantum + " bytes");
                }
            }
            error("|----> servir(OutputStream _out)");
            _out.flush();
            _out.close();
        }
    }

    public void actualFPS(float arg0) {
    }

    public void changedState(int arg0) {
        this.estadoPlayer = arg0;
        if (estadoPlayer == STOP) {
            ver("changedState(int arg0: Player STOPPED");
        }
        if (estadoPlayer == PLAY) {
            ver("changedState(int arg0: Player PLAYING");
        }
        if (estadoPlayer == PAUSE) {
            ver("changedState(int arg0: Player PAUSED");
        }
    }

    public void fpsChanged(float arg0) {
    }

    public void handleAnchor(String arg0) {
    }

    public void muteChanged(boolean arg0) {
    }

    public void playRequestWhenClosed() {
    }

    public void playerSize(int arg0, int arg1) {
    }

    public void playerTime(long arg0) {
    }

    public void preferSize(int arg0, int arg1) {
    }

    public void scalingChanged(boolean arg0) {
    }

    public void serverNTPTimeOrigin(long arg0) {
    }

    public void speedChanged(double arg0) {
    }

    public void speedScalingChanged(boolean arg0) {
    }

    public void urlChanged(String arg0) {
        ver("urlChanged(String arg0): El player intenta conectar con " + arg0);
    }

    public void usingMetrics(boolean arg0) {
    }

    public void volumeChanged(double arg0) {
    }
}
