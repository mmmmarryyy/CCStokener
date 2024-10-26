package net.juantxu.pentaho.launcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;
import org.apache.log4j.Logger;

public class BuscaCTools {

    static Logger log = Logger.getLogger(BuscaCTools.class);

    public String buscaCDA() {
        Properties prop = new CargaProperties().Carga();
        URL url;
        BufferedReader in;
        String inputLine;
        String miLinea = null;
        try {
            url = new URL(prop.getProperty("CDA"));
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains("cda-TRUNK-")) {
                    miLinea = inputLine;
                    miLinea = miLinea.substring(miLinea.indexOf("lastSuccessfulBuild/artifact/dist/cda-TRUNK"));
                    miLinea = miLinea.substring(0, miLinea.indexOf("\">"));
                    miLinea = url + miLinea;
                }
            }
        } catch (Throwable t) {
        }
        log.debug("Detetectado last build CDA: " + miLinea);
        return miLinea;
    }

    public String buscaCDE() {
        URL url;
        Properties prop = new CargaProperties().Carga();
        BufferedReader in;
        String inputLine;
        String miLinea = null;
        try {
            url = new URL(prop.getProperty("CDE"));
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains("lastSuccessfulBuild/artifact/server/plugin/dist/pentaho-cdf-dd-TRUNK")) {
                    miLinea = inputLine;
                    miLinea = miLinea.substring(miLinea.indexOf("lastSuccessfulBuild/artifact/server/plugin/dist/pentaho-cdf-dd-TRUNK"));
                    miLinea = miLinea.substring(0, miLinea.indexOf("\">"));
                    miLinea = url + miLinea;
                }
            }
        } catch (Throwable t) {
        }
        log.debug("Detetectado last build CDE: " + miLinea);
        return miLinea;
    }

    public String buscaSAIKU() {
        URL url;
        Properties prop = new CargaProperties().Carga();
        BufferedReader in;
        String inputLine;
        String miLinea = null;
        try {
            url = new URL(prop.getProperty("SAIKU"));
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains("lastSuccessfulBuild/artifact/saiku-bi-platform-plugin/target")) {
                    miLinea = inputLine;
                    log.debug(miLinea);
                    miLinea = miLinea.substring(miLinea.indexOf("lastSuccessfulBuild/artifact/saiku-bi-platform-plugin/target"));
                    miLinea = miLinea.substring(0, miLinea.indexOf("\">"));
                    miLinea = url + miLinea;
                }
            }
        } catch (Throwable t) {
        }
        log.debug("Detetectado last build SAIKU: " + miLinea);
        return miLinea;
    }
}
