package zonasoft.Utilidades;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

public class HTTPRequestPoster {

    public static boolean estaMuerto = false;

    public static boolean estaOcupado = false;

    public static boolean estaSuspendido = false;

    public static boolean noEstaEnElCenso = false;

    public static Map<String, String> datosFinales = new HashMap<String, String>();

    /**
     * Sends an HTTP GET request to a url
     *
     * @param endpoint - The URL of the server. (Example: " http://www.yahoo.com/search")
     * @param requestParameters - all the request parameters (Example: "param1=val1&param2=val2"). Note: This method will add the question mark (?) to the request - DO NOT add it yourself
     * @return - The response from the end point
     */
    public static String sendGetRequest(String endpoint, String requestParameters) {
        String result = null;
        if (endpoint.startsWith("http://")) {
            try {
                StringBuffer data = new StringBuffer();
                String urlStr = endpoint;
                if (requestParameters != null && requestParameters.length() > 0) {
                    urlStr += "?" + requestParameters;
                }
                URL url = new URL(urlStr);
                URLConnection conn = url.openConnection();
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuffer sb = new StringBuffer();
                String line, interesante = "";
                String[] datos = {};
                while ((line = rd.readLine()) != null) {
                    if (line.contains("intente") && line.contains("Ocupado")) {
                        estaOcupado = true;
                        break;
                    }
                    if (line.contains("Suspension") && line.contains("Derechos")) {
                        estaSuspendido = true;
                        break;
                    }
                    if (line.contains("Muerte")) {
                        estaMuerto = true;
                        break;
                    }
                    if (line.contains("No se encuentra en el censo ")) {
                        noEstaEnElCenso = true;
                    }
                    if (!line.isEmpty() && line.contains("<td")) {
                        interesante += sinAtributos(line);
                    }
                    sb.append(line + "\n");
                }
                datos = interesante.split("</td>");
                datosFinales = datosDelZonificado(datos);
                rd.close();
                result = sb.toString();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Verifique su conexion a internet");
            }
        }
        return result;
    }

    /**
     * Reads data from the data reader and posts it to a server via POST request.
     * data - The data you want to send
     * endpoint - The server's address
     * output - writes the server's response to output
     * @throws Exception
     */
    public static void postData(Reader data, URL endpoint, Writer output) throws Exception {
        HttpURLConnection urlc = null;
        try {
            urlc = (HttpURLConnection) endpoint.openConnection();
            try {
                urlc.setRequestMethod("POST");
            } catch (ProtocolException e) {
                throw new Exception("Shouldn't happen: HttpURLConnection doesn't support POST??", e);
            }
            urlc.setDoOutput(true);
            urlc.setDoInput(true);
            urlc.setUseCaches(false);
            urlc.setAllowUserInteraction(false);
            urlc.setRequestProperty("Content-type", "text/xml; charset=" + "UTF-8");
            OutputStream out = urlc.getOutputStream();
            try {
                Writer writer = new OutputStreamWriter(out, "UTF-8");
                pipe(data, writer);
                writer.close();
            } catch (IOException e) {
                throw new Exception("IOException while posting data", e);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            InputStream in = urlc.getInputStream();
            try {
                Reader reader = new InputStreamReader(in);
                pipe(reader, output);
                reader.close();
            } catch (IOException e) {
                throw new Exception("IOException while reading response", e);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            throw new Exception("Connection error (is server running at " + endpoint + " ?): " + e);
        } finally {
            if (urlc != null) {
                urlc.disconnect();
            }
        }
    }

    /**
     * Pipes everything from the reader to the writer via a buffer
     */
    private static void pipe(Reader reader, Writer writer) throws IOException {
        char[] buf = new char[1024];
        int read = 0;
        while ((read = reader.read(buf)) >= 0) {
            writer.write(buf, 0, read);
        }
        writer.flush();
    }

    public static String sinAtributos(String linea) {
        String pattern = "<td.*?>";
        String pattern2 = "<div.*?>";
        String replace = "";
        String replace2 = "</strong>";
        String replace3 = "<strong>";
        if (linea.contains("<div.*?>")) {
            Pattern p1 = Pattern.compile(pattern2, Pattern.CASE_INSENSITIVE);
            Matcher matcher2 = p1.matcher(linea);
            linea = matcher2.replaceAll(replace);
        }
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = p.matcher(linea);
        linea = matcher.replaceAll(replace);
        linea = linea.replace(replace2, "");
        linea = linea.replace(replace3, "");
        return linea;
    }

    public static Map<String, String> datosDelZonificado(String[] datos) {
        Map<String, String> result = new HashMap<String, String>();
        String pattern1 = "<div.*?>";
        String replace = "";
        for (int i = 0; i < datos.length; i++) {
            String valor = datos[i];
            if (valor.contains("Departamen")) {
                result.put("departamento", datos[i + 1].trim());
            }
            if (valor.contains("Municip")) {
                result.put("municipio", datos[i + 1].trim());
            }
            if (valor.contains("Direcci") && valor.contains("Puest")) {
                String linea = datos[i + 1].trim();
                Pattern p1 = Pattern.compile(pattern1, Pattern.CASE_INSENSITIVE);
                Matcher matcher2 = p1.matcher(linea);
                linea = matcher2.replaceAll(replace);
                linea = linea.replace("</div>", " ");
                result.put("direccion", linea);
            }
            if (valor.contains("Mesa")) {
                result.put("mesa", datos[i + 1].trim());
            }
            if (valor.contains("Puesto:") && !valor.contains("Direcci")) {
                result.put("puesto", datos[i + 1].trim());
            }
        }
        return result;
    }

    public static void consultarDatos(String cedula) {
        HTTPRequestPoster.sendGetRequest("http://www3.registraduria.gov.co/censo/_censoresultado.php", "nCedula=" + cedula);
    }
}
