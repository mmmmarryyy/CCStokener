package org.lm2a.client;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import org.lm2a.exceptions.CoverlessException;

public class YResumeControlHTTP {

    private static final int PUERTO = 56789;

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

    /**
	 * Aqui se internalizan los argumentos, es decir la URL, se valida que sea
	 * del protocolo HTTP y que este bien formada.
	 * Ademas se invoca al m�todo parse que extrae del objeto URL la informaci�n
	 * relevante.
	 * <p>
	 */
    public YResumeControlHTTP(String[] argumentos) {
        if ((argumentos.length != 1)) {
            throw new IllegalArgumentException("ResumeControlHTTP(String[] argumentos): N�mero equivocado de argumentos");
        }
        try {
            url = new URL(argumentos[0]);
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

    /**
	 * Aqui se parsea la URL ingresada y se extrae el nombre del archivo que se desea
	 * acceder. Con esa informaci�n se crea el flujo de salida (FileOutputStream).
	 * <p>
	 */
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

    /**
	 * En este m�todo se plantea todo el proceso a seguir y los cursos de acci�n
	 * frente a ca�das de cobertura. La ca�da de cobertura se catchea con una excepci�n
	 * propia (CoverlessException).
	 * <p>
	 */
    public void proceso() {
        try {
            abreConeccion();
        } catch (IOException e) {
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

    /**
	 * En este m�todo abre la conexi�n con el servidor y permite obtener los headers
	 * que el servidor env�a.
	 * Devuelve un objeto HttpURLConnection que luego se la pasa a los m�todos que realizan
	 * el pedido en concordancia con el protocolo HTTP.
	 * <p>
	 */
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

    /**
	 * En este m�todo re-abre la conexi�n con el servidor y permite obtener los headers
	 * que el servidor env�a. Difiere del anterior en que utiliza un versi�n rangueada
	 * del GET (requestRangedHTTP). Esta versi�n rangueada solicita al servidor la informaci�n
	 * restante para terminar el download. De esta forma no se recarga la parte que el cliente ya recibi�.
	 * Tambi�n devuelve un objeto HttpURLConnection que luego se la pasa a los m�todos que realizan
	 * el pedido en concordancia con el protocolo HTTP.
	 * <p>
	 */
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

    /**
	 * Lee la longitud del archivo a bajar. Se podr�a tambi�n obtener por HTTP.
	 * <p>
	 */
    public int leerLongitudArchivo() {
        return httpURLConnection.getContentLength();
    }

    /**
	 * En este m�todo se escribe en el archivo de salida (se utiliza una versi�n bufferizada con un array byte)
	 * a medida que se lee.
	 * <p>
	 */
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

    /**
	 * En este m�todo se abre el servidor local que el cliente utilizar� para obtener
	 * el archivo que requiere. 
	 * <p>
	 */
    public void abrirServerSocket() {
        try {
            conex = new ServerSocket(PUERTO);
        } catch (IOException e) {
            error("abrirServerSocket(): ERROR #RC6--> Problemas al abrir el socket de servicio");
            e.printStackTrace();
        }
    }

    /**
	 * En este m�todo se reintenta la conexi�n utilizando un Timer para hacerlo con determinada
	 * frecuencia. 
	 * En caso de reconexi�n se sigue el proceso pertinente (procesoReConexion()).
	 * <p>
	 */
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

    /**
	 * Proceso a seguir en caso de reconexi�n.
	 * <p>
	 */
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

    /**
	 * En este m�todo se lee el archivo. Se utiliza un bloque de decisi�n para el caso de que
	 * el �ltimo tramo a leer sea inferior al BUFFER previsto. Y la lectura al igual
	 * que la escritura tambi�n esta bufferizada con un array de bytes.
	 * Este es el m�todo donde b�sicamente hay mayor probabilidad de procesar una 
	 * excepci�n por problemas de cobertura. Y es el m�todo que llama al de escritura.
	 * <p>
	 */
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
            ver("leeDesde(int posicion): Escribiendo en offset " + offset + " | chunk: " + chunk + " | bytesread: " + bytesread);
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

    /**
	 * En este m�todo se define el estado de la conexi�n.
	 * <p>
	 */
    public void setConexion(boolean valor) {
        conectado = valor;
    }

    /**
	 * Este m�todo devuelve el estado de la conexi�n.
	 * <p>
	 */
    public boolean getConexion() {
        return conectado;
    }

    /**
	 * M�todo para imprimir un mensaje por el est�ndar error (consola en rojo).
	 * <p>
	 */
    public void error(String mje) {
        System.err.println(mje);
    }

    /**
	 * M�todo para imprimir un mensaje por la salida est�ndar (consola).
	 * <p>
	 */
    public void ver(String mje) {
        System.out.println(mje);
    }

    /**
	 * En este m�todo se pasa la informaci�n HTTP al servidor y se obtiene el InputStream que
	 * se usar� para el m�todo leeDesde().
	 * <p>
	 */
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

    /**
  	 * En este m�todo se pasa la informaci�n HTTP al servidor, pero se le solicita un 
  	 * GET rangueado para bajar solo lo que restaba desde la interrupci�n hasta el final
  	 * del archivo.
  	 * <p>
  	 */
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
}
