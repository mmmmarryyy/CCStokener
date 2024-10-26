package ar.com.khronos.core.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

/**
 * Representacion Java de una plantilla para crear
 * un mail en formato HTML.
 * <p>
 * Una plantilla HTML no es mas que una pagina HTML
 * con ciertos valores variables. Toda secuencia
 * de caracteres encontrada dentro dentro de un par
 * de llaves ({}), precedidas por un caracter peso ($) sera
 * tomado como una variable del template y sera reemplazada
 * luego por el valor especificado.
 * <p>
 * Ejemplo de template html:
 * <pre>
 * &lt;html&gt;
 * 	&lt;body&gt;
 * 		&lt;b&gt;Hola, mi nombre es ${nombre}&lt;/b&gt;
 * 	&lt;/body&gt;
 * &lt;/html&gt;
 * </pre>
 * Ejemplo de uso del template
 * <pre>
 * 		MailTemplate template = new MailTemplate("path/to/template.html");
 * 		Map<String, String> params = new HashMap<String, String>();
 * 		params.put("nombre", "Harry Potter");
 * 		String html = template.build(params);
 * </pre>
 * 
 * 
 * @author <a href="mailto:tezequiel@gmail.com">Ezequiel Turovetzky</a>
 *
 */
public class MailTemplate {

    /** Path al archivo a usar como plantilla */
    private String templateFile;

    /**
     * Crea una nueva instancia de esta clase.
     */
    public MailTemplate() {
    }

    /**
     * Crea una nueva instancia de esta clase.
     * 
     * @param templateFile Archivo a ser usado como plantilla
     */
    public MailTemplate(String templateFile) {
        this.templateFile = templateFile;
    }

    /**
     * Devuelve el path hacia la plantilla.
     * 
     * @return El path hacia la plantilla
     */
    public String getTemplateFile() {
        return templateFile;
    }

    /**
     * Establece el path hacia la plantilla.
     * 
     * @param templateFile El path hacia la plantilla
     */
    public void setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
    }

    /**
     * Constuye un objeto {@link String} a partir del HTML
     * de la plantilla, reemplazando las variables de la plantilla
     * con los parametros especificados en el mapa de parametros
     * 
     * @param params Parametros a insertar en la plantilla
     */
    public String build(Map<String, String> params) {
        try {
            URL url = getClass().getClassLoader().getResource(templateFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            StringBuffer buff = new StringBuffer("");
            while ((line = reader.readLine()) != null) {
                buff.append(line);
            }
            reader.close();
            String htmlText = buff.toString();
            if (params != null) {
                for (String key : params.keySet()) {
                    String pattern = "\\$\\{" + key + "\\}";
                    htmlText = htmlText.replaceAll(pattern, params.get(key));
                }
            }
            return htmlText;
        } catch (IOException e) {
            throw new UnexpectedException("Error leyendo plantilla de mail", e);
        }
    }
}
