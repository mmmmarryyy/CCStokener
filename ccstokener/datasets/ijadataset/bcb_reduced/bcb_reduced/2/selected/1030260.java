package glaureano.uteis.arquivos;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;

public class FileUtil {

    public static InputStream download(String endereco, ProxyConfig proxy) {
        if (proxy != null) {
            System.getProperties().put("proxySet", "true");
            System.getProperties().put("proxyPort", proxy.getPorta());
            System.getProperties().put("proxyHost", proxy.getHost());
            Authenticator.setDefault(new ProxyAuthenticator(proxy.getUsuario(), proxy.getSenha()));
        }
        try {
            URL url = new URL(endereco);
            ;
            URLConnection connection = url.openConnection();
            InputStream bis = new BufferedInputStream(connection.getInputStream());
            return bis;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static InputStream download(String endereco) {
        return download(endereco, (ProxyConfig) null);
    }

    public static boolean download(String endereco, String arquivo) {
        return gravar(download(endereco), arquivo);
    }

    public static boolean download(String endereco, String arquivo, ProxyConfig proxy) {
        return gravar(download(endereco, proxy), arquivo);
    }

    public static boolean gravar(InputStream inputStream, String arquivo) {
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(arquivo));
            int i;
            while ((i = inputStream.read()) != -1) {
                bos.write(i);
            }
            inputStream.close();
            bos.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    static class ProxyAuthenticator extends Authenticator {

        private String user, password;

        public ProxyAuthenticator(String user, String password) {
            this.user = user;
            this.password = password;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(user, password.toCharArray());
        }
    }
}
