package alertaboletin.crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import org.apache.log4j.Level;
import alertaboletin.indexer.BOEPDFDocument;
import alertaboletin.indexer.GenericDocument;
import alertaboletin.util.UtilConfiguracion;
import alertaboletin.util.UtilRegexp;

public class SearcherBOECrawler extends GenericCrawler {

    private String regexpURLsInteriores;

    private String prefijoInteriores;

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            System.out.println("Sintaxis: java SearcherBOECrawler enviarMails [startAt]");
            System.out.println("   enviarMails: 1/0 si se quieren enviar correos o no");
            System.out.println("   startAt: Número de resultado por el que se quiere empezar");
            System.exit(-1);
        }
        boolean bEnviarMails = args[0].equals("1") ? true : false;
        int startAt = -1;
        boolean bCrear = true;
        if (args.length == 2) {
            startAt = Integer.parseInt(args[1]);
        }
        SearcherBOECrawler instancia = new SearcherBOECrawler();
        instancia.init(startAt);
        instancia.go(bCrear, bEnviarMails);
    }

    public void init(int startAt) {
        indexDir = new File(UtilConfiguracion.getValor("BOE_INDEX_DIR"));
        inputFileName = UtilConfiguracion.getValor("BOE_SEARCHER_FILE");
        inputFileDir = UtilConfiguracion.getValor("BOE_SEARCHER_FEED_DIR");
        inputFeedURL = UtilConfiguracion.getValor("BOE_SEARCHER_FEED_URL");
        downloadDir = UtilConfiguracion.getValor("BOE_DOWNLOAD_DIR");
        inicioNombreCampo = UtilConfiguracion.getValor("BOE_ID_FIELD_START");
        finNombreCampo = UtilConfiguracion.getValor("BOE_ID_FIELD_END");
        inicioNombreFichero = UtilConfiguracion.getValor("BOE_ID_FILE_START");
        finNombreFichero = UtilConfiguracion.getValor("BOE_ID_FILE_END");
        regexpURLs = UtilConfiguracion.getValor("BOE_REGEXP_URLS");
        prefijoDireccion = UtilConfiguracion.getValor("BOE_PREFIJO_URL");
        postfijoDireccion = UtilConfiguracion.getValor("BOE_POSTFIJO_URL");
        regexpURLsInteriores = UtilConfiguracion.getValor("BOE_REGEXP_URLS_INTERIOR");
        prefijoURL = UtilConfiguracion.getValor("BOE_URL_DOCUMENT_START");
        postfijoURL = UtilConfiguracion.getValor("BOE_URL_DOCUMENT_END");
        prefijoInteriores = UtilConfiguracion.getValor("BOE_PREFIJO_URL_INTERIOR");
        tipo = BOEPDFDocument.TIPO_BOE;
        if (startAt > 0) {
            String strTmp = UtilConfiguracion.getValor("BOE_SEARCHER_FEED_URL_STARTAT");
            inputFeedURL += strTmp;
            inputFeedURL += startAt;
        }
    }

    public String obtenerURL(String direccion) throws Exception {
        InputStream is = null;
        try {
            URL url = new URL(direccion);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            if (connection.getResponseCode() / 100 != 2) {
                throw new Exception("Código de respuesta inválido: " + connection.getResponseCode() + ", URL: " + direccion);
            }
            is = connection.getInputStream();
            BufferedReader reader;
            reader = new BufferedReader(new InputStreamReader(is));
            String linea;
            while ((linea = reader.readLine()) != null) {
                logger.log(Level.DEBUG, "Linea original: " + linea);
                linea = convertir(linea);
                logger.log(Level.DEBUG, "Llamada a extraer: " + linea);
                ArrayList<String> direcciones = UtilRegexp.extraeURLPDFs(linea, regexpURLsInteriores, 1);
                logger.log(Level.DEBUG, "Fin de extraer");
                if (direcciones.size() > 0) {
                    return prefijoInteriores + direcciones.get(0);
                }
            }
            throw new Exception("No encontrada URL al PDF del BOE");
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public GenericDocument buildDocument(String directorio, String nombre, String url) {
        return new BOEPDFDocument(directorio, nombre, url);
    }
}
