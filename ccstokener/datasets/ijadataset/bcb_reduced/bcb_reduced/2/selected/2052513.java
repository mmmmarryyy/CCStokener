package openadmin.util.territoryload;

import openadmin.model.territory.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

/** Clase ConversorCoordenadas
 * Contiene las dos funciones estaticas relacionadas con el catastro
 * @author Francisco Martinez
 * @version 1.0
 */
public class ConversorCoordenadas {

    private static String baseURL[] = { "http://ovc.catastro.meh.es/ovcservweb/OVCSWLocalizacionRC/OVCCoordenadas.asmx/Consulta_RCCOOR?SRS=" + "<SRS>" + "&Coordenada_X=" + "<coordX>" + "&Coordenada_Y=" + "<coordY>", "http://ovc.catastro.meh.es/ovcservweb/OVCSWLocalizacionRC/OVCCoordenadas.asmx/Consulta_CPMRC?Provincia=&Municipio=&SRS=EPSG:4326&RC=" + "<RC>" };

    /** Metodo getRefCatastral
	  * Sirve para obtener la referencia catastral a partir de las coordenadas X e Y dadas,
	  * se conecta a la direccion generica del Catastro, 
	  * es estatica dado que no necesita crear ningun tipo de objeto para utilizarse.
	  * @param Double x latitud
	  * @param Double y longitud
	  * @return String referencia catastral obtenida
	  */
    public static String getRefCatastral(Double x, Double y) {
        return getRefCatastral(baseURL[0].replace("<SRS>", "EPSG:4326").replace("<coordX>", Double.toString(x)).replace("<coordY>", Double.toString(y)));
    }

    /** Metodo getRefCatastral
	  * Sirve para obtener la referencia catastral a partir de las coordenadas X e Y dadas,
	  * se conecta a la direccion generica del Catastro, 
	  * es estatica dado que no necesita crear ningun tipo de objeto para utilizarse.
	  * @param Coordenadas c coordenadas de las que queremos saber la referencia catastral
	  * @return String referencia catastral obtenida
	  */
    public static String getRefCatastral(Coordinate c) {
        return getRefCatastral(baseURL[0].replace("<SRS>", "EPSG:4326").replace("<coordX>", Double.toString(c.getLongitude())).replace("<coordY>", Double.toString(c.getLatitude())));
    }

    /** Metodo getRefCatastral
	  * Sirve para obtener la referencia catastral a partir de las coordenadas X e Y dadas, 
	  * es estatica dado que no necesita crear ningun tipo de objeto para utilizarse.
	  * @param String pURL url del servicio al que se accede
	  * @return String referencia catastral obtenida
	  */
    public static String getRefCatastral(String pURL) {
        String result = new String();
        String iniPC1 = "<pc1>";
        String iniPC2 = "<pc2>";
        String finPC1 = "</pc1>";
        String finPC2 = "</pc2>";
        String iniCuerr = "<cuerr>";
        String finCuerr = "</cuerr>";
        String iniDesErr = "<des>";
        String finDesErr = "</des>";
        boolean error = false;
        int ini, fin;
        try {
            URL url = new URL(pURL);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            while ((str = in.readLine()) != null) {
                if (str.contains(iniCuerr)) {
                    ini = str.indexOf(iniCuerr) + iniCuerr.length();
                    fin = str.indexOf(finCuerr);
                    if (Integer.parseInt(str.substring(ini, fin)) > 0) error = true;
                }
                if (error) {
                    if (str.contains(iniDesErr)) {
                        ini = str.indexOf(iniDesErr) + iniDesErr.length();
                        fin = str.indexOf(finDesErr);
                        throw (new Exception(str.substring(ini, fin)));
                    }
                } else {
                    if (str.contains(iniPC1)) {
                        ini = str.indexOf(iniPC1) + iniPC1.length();
                        fin = str.indexOf(finPC1);
                        result = str.substring(ini, fin);
                    }
                    if (str.contains(iniPC2)) {
                        ini = str.indexOf(iniPC2) + iniPC2.length();
                        fin = str.indexOf(finPC2);
                        result = result.concat(str.substring(ini, fin));
                    }
                }
            }
            in.close();
        } catch (Exception e) {
            System.err.println(e);
        }
        return result;
    }

    /** Metodo getCoordenadas
	  * Sirve para obtener las coordenadas X e Y a partir de una referencia catastral dada, 
	  * es estatica dado que no necesita crear ningun tipo de objeto para utilizarse.
	  * @param String RCoURL puede ser la referencia catastral de la que queremos saber las coordenadas a partir
	  * de la direccion generica del Catastro o bien la url del servicio al que debe acceder en lugar de la del Catastro
	  * @return Coordenadas c coordenadas obtenidas
	  */
    public static Coordinate getCoordenadas(String RCoURL) {
        Coordinate coord = new Coordinate();
        String pURL;
        String iniPC1 = "<pc1>";
        String iniPC2 = "<pc2>";
        String finPC1 = "</pc1>";
        String finPC2 = "</pc2>";
        String iniX = "<xcen>";
        String iniY = "<ycen>";
        String finX = "</xcen>";
        String finY = "</ycen>";
        String iniCuerr = "<cuerr>";
        String finCuerr = "</cuerr>";
        String iniDesErr = "<des>";
        String finDesErr = "</des>";
        boolean error = false;
        int ini, fin;
        if (RCoURL.contains("/") || RCoURL.contains("\\") || RCoURL.contains(".")) pURL = RCoURL; else {
            if (RCoURL.length() > 14) pURL = baseURL[1].replace("<RC>", RCoURL.substring(0, 14)); else pURL = baseURL[1].replace("<RC>", RCoURL);
        }
        try {
            URL url = new URL(pURL);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            while ((str = in.readLine()) != null) {
                if (str.contains(iniCuerr)) {
                    ini = str.indexOf(iniCuerr) + iniCuerr.length();
                    fin = str.indexOf(finCuerr);
                    if (Integer.parseInt(str.substring(ini, fin)) > 0) error = true;
                }
                if (error) {
                    if (str.contains(iniDesErr)) {
                        ini = str.indexOf(iniDesErr) + iniDesErr.length();
                        fin = str.indexOf(finDesErr);
                        throw (new Exception(str.substring(ini, fin)));
                    }
                } else {
                    if (str.contains(iniPC1)) {
                        ini = str.indexOf(iniPC1) + iniPC1.length();
                        fin = str.indexOf(finPC1);
                        coord.setDescription(str.substring(ini, fin));
                    }
                    if (str.contains(iniPC2)) {
                        ini = str.indexOf(iniPC2) + iniPC2.length();
                        fin = str.indexOf(finPC2);
                        coord.setDescription(coord.getDescription().concat(str.substring(ini, fin)));
                    }
                    if (str.contains(iniX)) {
                        ini = str.indexOf(iniX) + iniX.length();
                        fin = str.indexOf(finX);
                        coord.setLongitude(Double.parseDouble(str.substring(ini, fin)));
                    }
                    if (str.contains(iniY)) {
                        ini = str.indexOf(iniY) + iniY.length();
                        fin = str.indexOf(finY);
                        coord.setLatitude(Double.parseDouble(str.substring(ini, fin)));
                    }
                }
            }
            in.close();
        } catch (Exception e) {
            System.err.println(e);
        }
        return coord;
    }
}
