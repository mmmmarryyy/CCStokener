package actualizacion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.URL;

class HttpReaderException extends RuntimeException {

    private static final long serialVersionUID = 364698936409845078L;

    public HttpReaderException(String msg) {
        super(msg);
    }
}

class HttpResponseHeader {

    String status;

    String date;

    String server;

    String modified;

    String contentLength;

    String contentType;

    public String toString() {
        return ("Status: " + status) + "\n" + ("Date: " + date) + "\n" + ("Server: " + server) + "\n" + ("Modified: " + modified) + "\n" + ("Content-Length: " + contentLength) + "\n" + ("Content-Type: " + contentType) + "\n";
    }
}

class BandwidthManager extends Thread {

    int promedio;

    long descarga;

    long leche;

    int kbps;

    private long velocidades[];

    public BandwidthManager(int kbps) {
        this.kbps = kbps;
        velocidades = new long[5];
        setPriority(MAX_PRIORITY);
        start();
    }

    public void setKBps(int kbps) {
        this.kbps = kbps;
    }

    public void run() {
        int i = 0;
        while (true) {
            try {
                Thread.sleep(1000);
                synchronized (this) {
                    leche = kbps * 1024;
                    notifyAll();
                }
                if (i >= velocidades.length) i = 0;
                velocidades[i++] = HttpReader.descarga;
                descarga += HttpReader.descarga;
                HttpReader.descarga = 0;
                promedio = 0;
                for (int x = 0; x < velocidades.length; x++) promedio += velocidades[x];
                promedio /= velocidades.length;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class URLParts {

    String host;

    int port;

    String get;

    public static URLParts parseURL(String url) {
        String host = "";
        String get = "";
        String puerto = "";
        if (url.startsWith("http://")) {
            int x;
            for (x = 7; x < url.length() && url.charAt(x) != '/' && url.charAt(x) != ':'; x++) {
                host += url.charAt(x);
            }
            if (host.equals("")) throw new HttpReaderException("URL Inv�lida: " + url);
            if (x < url.length() && url.charAt(x) == ':') {
                x++;
                while (x < url.length() && url.charAt(x) != '/') {
                    puerto += url.charAt(x);
                    x++;
                }
            }
            if (puerto.equals("")) puerto = "80";
            while (x < url.length()) {
                get += url.charAt(x);
                x++;
            }
            if (get.equals("")) get += "/";
            URLParts partes = new URLParts();
            partes.host = host;
            partes.port = Integer.parseInt(puerto);
            partes.get = get;
            return partes;
        } else throw new HttpReaderException("URL Inv�lida: " + url);
    }
}

public class HttpReader {

    static long descarga;

    static BandwidthManager bwm = new BandwidthManager(150);

    static int bsize = 1024;

    static int timeOut = 60000 * 5;

    byte buffer[];

    int pos;

    int res;

    int leido;

    Socket skt;

    SocketAddress direccion;

    private IMostrarMensaje mMesg;

    public HttpReader(IMostrarMensaje mMesg) {
        buffer = new byte[1024 * 1024];
        direccion = null;
        this.mMesg = mMesg;
    }

    protected void mostrar(String msg) {
        if (mMesg != null) mMesg.mostrar(msg);
    }

    public synchronized HttpResponseHeader getHeader(String url) throws IOException {
        HttpResponseHeader head = new HttpResponseHeader();
        URLParts ul = URLParts.parseURL(url);
        if (skt != null && !skt.isClosed()) skt.close();
        skt = new Socket();
        skt.setSoLinger(true, 0);
        skt.setSoTimeout(timeOut);
        skt.setReuseAddress(true);
        if (direccion != null) skt.bind(direccion);
        skt.connect(new InetSocketAddress(ul.host, ul.port), timeOut);
        if (direccion == null) direccion = skt.getLocalSocketAddress();
        skt.setReceiveBufferSize(bsize);
        String peticion;
        peticion = "HEAD " + ul.get + " HTTP/1.1\n";
        peticion += "Host: " + ul.host + "\n";
        peticion += "Connection: close\n";
        peticion += "Accept: text/html\n";
        peticion += "User-Agent: RieztraBot/0.5\n\n";
        int x;
        for (x = 0; x < peticion.length(); x++) {
            buffer[x] = (byte) peticion.charAt(x);
        }
        skt.getOutputStream().write(buffer, 0, x);
        skt.getOutputStream().flush();
        BufferedReader br = new BufferedReader(new InputStreamReader(skt.getInputStream()));
        String linea;
        head.status = br.readLine();
        if (head.status != null) descarga += head.status.length();
        while ((linea = br.readLine()) != null) {
            descarga += linea.length();
            if (linea.startsWith("Server:")) {
                head.server = linea.substring(linea.indexOf(":") + 1).trim();
            } else if (linea.startsWith("Date:")) {
                head.date = linea.substring(linea.indexOf(":") + 1).trim();
            } else if (linea.startsWith("Last-Modified:")) {
                head.modified = linea.substring(linea.indexOf(":") + 1).trim();
            } else if (linea.startsWith("Content-Length:")) {
                head.contentLength = linea.substring(linea.indexOf(":") + 1).trim();
            } else if (linea.startsWith("Content-Type:")) {
                head.contentType = linea.substring(linea.indexOf(":") + 1).trim();
            }
        }
        br.close();
        return head;
    }

    public synchronized String get(String url) throws IOException {
        URLParts ul = URLParts.parseURL(url);
        if (skt != null && !skt.isClosed()) skt.close();
        skt = new Socket();
        skt.setSoLinger(true, 0);
        skt.setSoTimeout(timeOut);
        skt.setReuseAddress(true);
        if (direccion != null) skt.bind(direccion);
        skt.connect(new InetSocketAddress(ul.host, ul.port), timeOut);
        if (direccion == null) direccion = skt.getLocalSocketAddress();
        skt.setReceiveBufferSize(bsize);
        String peticion;
        peticion = "GET " + ul.get + " HTTP/1.1\n";
        peticion += "Host: " + ul.host + "\n";
        peticion += "Connection: close\n";
        peticion += "Accept: text/html\n";
        peticion += "User-Agent: RieztraBot/0.5\n\n";
        int x;
        for (x = 0; x < peticion.length(); x++) {
            buffer[x] = (byte) peticion.charAt(x);
        }
        skt.getOutputStream().write(buffer, 0, x);
        skt.getOutputStream().flush();
        InputStream is = skt.getInputStream();
        int res;
        pos = 0;
        try {
            do {
                synchronized (bwm) {
                    while (bwm.leche < bsize) {
                        try {
                            bwm.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    bwm.leche -= bsize;
                }
                res = is.read(buffer, pos, bsize);
                if (res > -1) {
                    pos += res;
                    descarga += res;
                }
            } while (res > -1);
        } catch (SocketTimeoutException e) {
            skt.close();
            throw e;
        }
        skt.close();
        return new String(buffer, 0, pos);
    }

    private String readLine(int ini) throws IOException {
        String tmp = "";
        if (ini >= pos) {
            res = skt.getInputStream().read(buffer, pos, bsize);
            pos += res;
        }
        while (ini < pos && buffer[ini] != 13 && buffer[ini] != 10) {
            ini++;
            tmp += (char) buffer[ini - 1];
            if (ini >= pos) {
                res = skt.getInputStream().read(buffer, pos, bsize);
                pos += res;
            }
        }
        ini++;
        if (buffer[ini] == 10) ini++;
        if (ini < pos) leido = ini; else return null;
        return tmp;
    }

    public synchronized byte[] getFile(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        int size = conn.getContentLength();
        InputStream is = conn.getInputStream();
        pos = 0;
        leido = 0;
        int ini = leido;
        try {
            do {
                synchronized (bwm) {
                    while (bwm.leche < bsize) {
                        try {
                            bwm.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    bwm.leche -= bsize;
                }
                res = is.read(buffer, pos, bsize);
                if (res > -1) {
                    pos += res;
                    descarga += res;
                }
                mostrar("Descargando (" + (pos - ini) / 1024 + " de " + size / 1024 + " KB) | " + (pos - ini) * 100 / size + "% | [ " + (double) ((int) ((double) bwm.promedio / 1024 * 100)) / 100 + " KB/s ]");
            } while (res > -1);
        } catch (SocketTimeoutException e) {
            skt.close();
            throw e;
        }
        is.close();
        byte bin[] = new byte[pos - ini];
        for (int x = ini; x < pos; x++) {
            bin[x - ini] = buffer[x];
        }
        return bin;
    }

    public static void main(String[] args) throws Exception {
    }
}
