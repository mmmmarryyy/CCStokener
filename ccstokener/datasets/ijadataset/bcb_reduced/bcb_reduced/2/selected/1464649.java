package com.alquilacosas.mbean;

import com.visural.common.IOUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.primefaces.json.JSONObject;

/**
 *
 * @author damiancardozo
 */
@ManagedBean(name = "facebookAccess")
@RequestScoped
public class FacebookAccessMBean {

    @ManagedProperty(value = "#{login}")
    private ManejadorUsuarioMBean login;

    /** Creates a new instance of FacebookAccessMBean */
    public FacebookAccessMBean() {
    }

    @PostConstruct
    public void init() {
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        HttpServletRequest req = (HttpServletRequest) context.getRequest();
        HttpServletResponse resp = (HttpServletResponse) context.getResponse();
        String code = req.getParameter("code");
        if (code != null && !code.equals("")) {
            String authURL = ManejadorUsuarioMBean.getAuthURL(code);
            try {
                URL url = new URL(authURL);
                String result = readURL(url);
                String accessToken = null;
                Integer expires = null;
                String[] pairs = result.split("&");
                for (String pair : pairs) {
                    String[] kv = pair.split("=");
                    if (kv.length != 2) {
                        throw new RuntimeException("Unexpected auth response");
                    } else {
                        if (kv[0].equals("access_token")) {
                            accessToken = kv[1];
                        }
                        if (kv[0].equals("expires")) {
                            expires = Integer.valueOf(kv[1]);
                        }
                    }
                }
                if (accessToken != null && expires != null) {
                    try {
                        String res = IOUtil.urlToString(new URL("https://graph.facebook.com/me?access_token=" + accessToken));
                        JSONObject json = new JSONObject(res);
                        String id = json.getString("id");
                        String nombre = json.getString("first_name");
                        String apellido = json.getString("last_name");
                        String email = json.getString("email");
                        boolean loggedIn = login.completeFbLogin(email, id);
                        if (!loggedIn) {
                            login.registrarFb(nombre, apellido, email, id);
                        }
                        System.out.println("Hello " + nombre + " " + apellido + ", email: " + email);
                    } catch (Exception e) {
                        Logger.getLogger(FacebookAccessMBean.class).error("Exception reading response from facebook: " + e + ": " + e.getMessage());
                        System.out.println("Exception getting son object!" + e + ": " + e.getMessage());
                    }
                    String redirect = (String) req.getSession(true).getAttribute("redirectUrl");
                    req.getSession(true).removeAttribute("redirectUrl");
                    if (redirect != null) {
                        redirect = req.getContextPath() + redirect;
                    } else {
                        redirect = req.getContextPath() + "/vistas/inicio.jsf";
                    }
                    context.redirect(redirect);
                    FacesContext.getCurrentInstance().responseComplete();
                } else {
                    System.out.println("access token no recibido");
                    context.redirect(req.getContextPath() + "/vistas/inicio.jsf");
                    FacesContext.getCurrentInstance().responseComplete();
                }
            } catch (IOException e) {
                try {
                    context.redirect(req.getContextPath() + "/vistas/inicio.jsf");
                    FacesContext.getCurrentInstance().responseComplete();
                } catch (IOException ex) {
                }
            }
        }
    }

    private String readURL(URL url) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = url.openStream();
        int r;
        while ((r = is.read()) != -1) {
            baos.write(r);
        }
        return new String(baos.toByteArray());
    }

    public String getInit() {
        return "";
    }

    public ManejadorUsuarioMBean getLogin() {
        return login;
    }

    public void setLogin(ManejadorUsuarioMBean login) {
        this.login = login;
    }
}
