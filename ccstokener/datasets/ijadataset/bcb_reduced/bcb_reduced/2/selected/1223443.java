package rs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/** Trata uma página do rapidshare.com
 *
 * @author Samir <samirfor@gmail.com>
 */
public class Html {

    private URI uri;

    private String body, link;

    public Html() {
    }

    /**
     * Cria a pagina com o host já alterado para o endereço IP.
     * @param link - endereço completo do link.
     */
    public Html(String link) {
        modificaHost(link);
        this.body = getHTTPBody();
        this.link = "http://" + uri.getHost() + uri.getPath();
    }

    /**
     * Troca o hostname pelo endereço IP correspondente.
     * Ex: rapidshare.com => 195.122.131.2
     * 
     * Os dados são guardados no atributo URI uri.
     * @param link - endereço completo do link.
     * @return
     */
    public boolean modificaHost(String link) {
        try {
            uri = new URI(link);
            Socket socket;
            try {
                socket = new Socket(uri.getHost(), 80);
                link = "http://" + socket.getInetAddress().getHostAddress() + uri.getPath();
                uri = new URI(link);
                return true;
            } catch (UnknownHostException ex) {
                Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (URISyntaxException ex) {
            Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * Conecta-se ao site para obter o BODY da página.
     * @return string - body da página.
     */
    public String getHTTPBody() {
        StringBuilder stream = new StringBuilder();
        try {
            URLConnection conexao = uri.toURL().openConnection();
            InputStream entrada = conexao.getInputStream();
            byte[] buffer = new byte[1024];
            while (entrada.read(buffer) != -1) {
                stream.append(new String(buffer));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stream.toString();
    }

    /**
     * Procura uma ocorrência de um padrão numa string.
     * @return - a posição da string em que a substring está.
     * - retorna -1 se não encontrar
     */
    public int buscaString(String padrao) {
        int indice;
        indice = body.indexOf(padrao);
        return indice;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getHost() {
        return uri.getHost();
    }

    public String getPath() {
        return uri.getPath();
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        modificaHost(link);
        this.link = "http://" + uri.getHost() + uri.getPath();
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    /**
     * Escreve o atributo body numa arquivo.
     * @param path - caminho do arquivo
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void toFile(String path) throws FileNotFoundException, IOException {
        File arq = new File(path);
        FileOutputStream stream = new FileOutputStream(arq);
        stream.write(getBody().getBytes());
    }

    public String submit(String action, HashMap<String, String> dado) throws Exception {
        StringBuilder stream = new StringBuilder();
        URL url = new URL(action);
        HttpURLConnection conexao = (HttpURLConnection) url.openConnection();
        conexao.setRequestMethod("POST");
        conexao.setDoOutput(true);
        conexao.setDoInput(true);
        DataOutputStream saida = new DataOutputStream(conexao.getOutputStream());
        Set chaves = dado.keySet();
        Iterator chaveIterador = chaves.iterator();
        String conteudo = "";
        for (int i = 0; chaveIterador.hasNext(); i++) {
            Object key = chaveIterador.next();
            if (i != 0) {
                conteudo += "&";
            }
            conteudo += key + "=" + URLEncoder.encode(dado.get(key), "UTF-8");
        }
        saida.writeBytes(conteudo);
        saida.flush();
        saida.close();
        InputStream entrada = conexao.getInputStream();
        byte[] buffer = new byte[1024];
        while (entrada.read(buffer) != -1) {
            stream.append(new String(buffer));
        }
        entrada.close();
        return stream.toString();
    }

    public void substituirTudo(String padrao, String troca) {
        body = body.replaceAll(padrao, troca);
    }
}
