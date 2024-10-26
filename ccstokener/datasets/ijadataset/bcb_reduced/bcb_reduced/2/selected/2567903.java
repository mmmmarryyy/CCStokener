package br.org.reconcavotecnologia.update19.network;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

/**
 * Classe responsável por realizar o update da aplicação. Faz download do
 * arquivo necessário e atualiza as bibliotecas.
 * 
 * @author Cássio Oliveira
 * 
 * TODO Adicionar Gerenciamento de Download
 * TODO Informar porcentagem de progresso do download
 * TODO Utilizar uma Thread no método Download start
 */
public class Download {

    /** Variáveis para indicar o estado do download */
    public static final int COMPLETED = 0, IN_PROGRESS = 1, NOT_STARTED = 2, ERROR = 3;

    /** Arquivo a ser baixado */
    private String fileFromServer = null;

    /** Endereço local de destino */
    private String localFile = null;

    /** indica o status do download */
    private int status;

    /** Arquivo baixado */
    private File file = null;

    /** Método construtor */
    public Download(String fileFromServer, String localFile) {
        this.fileFromServer = fileFromServer;
        this.localFile = localFile;
        status = NOT_STARTED;
    }

    /** Retorna o status do download definido com as constantes estáticas desta classe */
    public int getStatus() {
        return status;
    }

    /** Retorna o arquivo baixado */
    public File getFile() {
        return file;
    }

    /** Aguarda até que o download seja concluído (com sucesso ou não) */
    public int waitFor() {
        while (this.status == IN_PROGRESS) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return this.status;
    }

    /** Invoca o método de start sem proxy */
    public void start() throws Exception {
        this.start(null);
    }

    /** Faz download de arquivo */
    public void start(Proxy proxy) throws Exception {
        InputStream in = null;
        URLConnection conn = null;
        OutputStream out = null;
        System.out.println("Download.start() " + this.fileFromServer);
        try {
            URL url = new URL(this.fileFromServer);
            out = new BufferedOutputStream(new FileOutputStream(this.localFile));
            if (proxy != null) {
                conn = url.openConnection(proxy);
            } else {
                conn = url.openConnection();
            }
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            this.status = IN_PROGRESS;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
            System.out.println(this.localFile + "\t" + numWritten);
            file = new File(this.localFile);
            this.status = COMPLETED;
        } catch (Exception exception) {
            this.status = ERROR;
            throw exception;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
