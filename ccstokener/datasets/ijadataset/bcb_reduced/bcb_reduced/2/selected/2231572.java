package net.woodstock.nettool4j.test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import net.woodstock.nettool4j.socket.OutputSocket;
import net.woodstock.nettool4j.socket.ProxyInfo;
import net.woodstock.nettool4j.socket.RedirectInfo;
import net.woodstock.nettool4j.socket.TCPRedirect;

public class TCPRedirectTest {

    public void test1() {
        OutputSocket outputSocket = new OutputSocket(8888);
        Thread thread1 = new Thread(outputSocket);
        thread1.start();
        TCPRedirect tcpRedirect = new TCPRedirect(new RedirectInfo(9999, "localhost", 8888));
        Thread thread2 = new Thread(tcpRedirect);
        thread2.start();
    }

    public void test2() {
        TCPRedirect tcpRedirect = new TCPRedirect(new RedirectInfo(9999, "127.0.0.1", 3306));
        Thread thread2 = new Thread(tcpRedirect);
        thread2.start();
    }

    public void test3() {
        ProxyInfo proxy = new ProxyInfo("127.0.0.1", 8080);
        RedirectInfo redirect = new RedirectInfo(9999, "woodstock.net.br", 8443);
        TCPRedirect tcpRedirect = new TCPRedirect(redirect, proxy);
        Thread thread2 = new Thread(tcpRedirect);
        thread2.start();
    }

    public void test4() {
        try {
            SocketAddress proxyAddress = new InetSocketAddress("127.0.0.1", 8080);
            Proxy proxy = new Proxy(Type.HTTP, proxyAddress);
            SocketAddress address = new InetSocketAddress("woodstock.net.br", 80);
            Socket socket = new Socket(proxy);
            socket.setKeepAlive(true);
            socket.connect(address, 15000);
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();
            outputStream.write("GET / HTTP/1.1\nhost: woodstock.net.br\n".getBytes());
            int i = -1;
            do {
                if (!socket.isInputShutdown()) {
                    i = inputStream.read();
                    if (i != -1) {
                        System.out.println((byte) i);
                    }
                } else {
                    break;
                }
            } while (i != -1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void test5() {
        try {
            SocketAddress proxyAddress = new InetSocketAddress("127.0.0.1", 8080);
            Proxy proxy = new Proxy(Type.HTTP, proxyAddress);
            URL url = new URL("http://woodstock.net.br:8443");
            URLConnection connection = url.openConnection(proxy);
            InputStream inputStream = connection.getInputStream();
            Scanner scanner = new Scanner(inputStream);
            while (scanner.hasNextLine()) {
                System.out.println(scanner.nextLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(final String[] args) {
        new TCPRedirectTest().test5();
    }
}
