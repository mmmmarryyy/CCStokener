package model.admin;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import model.TypeRequest;

/**
 * Clase abstracta que realiza las peticiones al applet servidor solicitado
 */
public abstract class Admin {

    private static String server;

    /**
	 * Realiza la peticion con los parametros params al Servlet sevlet recibido
	 * como parametro y retorna la respuesta resultante.
	 * 
	 * @param params
	 * @param servlet
	 * @return Respuesta del Servlet sobre una Hashtable<nombreParametro,contenido>.
	 */
    protected final Hashtable<String, Object> processRequest(Hashtable<String, Object> params, String servlet) {
        Hashtable<String, Object> response = null;
        try {
            URL url = new URL(Admin.server + servlet);
            URLConnection conn = url.openConnection();
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            this.doRequest(params, conn);
            response = this.processResponse(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
	 * Realiza la peticion al Servlet parametro.
	 * 
	 * @param servlet
	 * @return Respuesta del Servlet sobre una Hashtable<nombreParametro,contenido>.
	 */
    protected final Hashtable<String, Object> processRequest(String servlet) {
        return this.processRequest(null, servlet);
    }

    /**
	 * Realiza la peticion al Servlet servlet del tipo dado por typeRequest con
	 * el parametro (nameParam, param).
	 * 
	 * @param nameParam
	 * @param param
	 * @param typeRequest
	 * @param servlet
	 * @return Respuesta por defecto de los servlet (result - un solo
	 *         resultado).
	 */
    protected final Object requestWhit(String nameParam, Object param, TypeRequest typeRequest, String servlet) {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        params.put(nameParam, param);
        if (typeRequest != null) params.put("type", typeRequest);
        Hashtable<String, Object> response = this.processRequest(params, servlet);
        return response.get("result");
    }

    /**
	 * Como requestWhit pero con dos parametros.
	 * 
	 * @param nameParam
	 * @param param
	 * @param nameParam2
	 * @param param2
	 * @param typeRequest
	 * @param servlet
	 * @return Respuesta por defecto de los servlet (result - un solo
	 *         resultado).
	 */
    protected final Object requestWhitWhit(String nameParam, Object param, String nameParam2, Object param2, TypeRequest typeRequest, String servlet) {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        params.put(nameParam, param);
        params.put(nameParam2, param2);
        if (typeRequest != null) params.put("type", typeRequest);
        Hashtable<String, Object> response = this.processRequest(params, servlet);
        return response.get("result");
    }

    /**
	 * Como requestWhit pero con tres parametros.
	 * 
	 * @param nameParam
	 * @param param
	 * @param nameParam2
	 * @param param2
	 * @param nameParam3
	 * @param param3
	 * @param typeRequest
	 * @param servlet
	 * @return Respuesta por defecto de los servlet (result - un solo
	 *         resultado).
	 */
    protected final Object requestWhitWhitWhit(String nameParam, Object param, String nameParam2, Object param2, String nameParam3, Object param3, TypeRequest typeRequest, String servlet) {
        Hashtable<String, Object> params = new Hashtable<String, Object>();
        params.put(nameParam, param);
        params.put(nameParam2, param2);
        params.put(nameParam3, param3);
        if (typeRequest != null) params.put("type", typeRequest);
        Hashtable<String, Object> response = this.processRequest(params, servlet);
        return response.get("result");
    }

    private void doRequest(Hashtable<String, Object> params, URLConnection conn) throws IOException {
        ObjectOutputStream objOutStream = new ObjectOutputStream(conn.getOutputStream());
        objOutStream.writeObject(params);
    }

    private Hashtable<String, Object> processResponse(URLConnection con) throws IOException, ClassNotFoundException {
        InputStream in = con.getInputStream();
        ObjectInputStream objStream = new ObjectInputStream(in);
        return (Hashtable<String, Object>) objStream.readObject();
    }

    public static String getServer() {
        return server;
    }

    public static void setServer(String server) {
        Admin.server = server;
    }
}
