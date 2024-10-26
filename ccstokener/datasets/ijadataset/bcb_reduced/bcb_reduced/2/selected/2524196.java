package br.com.linkcom.neo.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * @author fabricio
 */
public class IncludeTag extends TagSupport {

    private static final long serialVersionUID = 1L;

    protected String url;

    public int doEndTag() throws JspException {
        JspWriter saida = pageContext.getOut();
        HttpURLConnection urlConnection = null;
        try {
            URL requisicao = new URL(((HttpServletRequest) pageContext.getRequest()).getRequestURL().toString());
            URL link = new URL(requisicao, url);
            urlConnection = (HttpURLConnection) link.openConnection();
            BufferedReader entrada = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "ISO-8859-1"));
            String linha = entrada.readLine();
            while (linha != null) {
                saida.write(linha + "\n");
                linha = entrada.readLine();
            }
            entrada.close();
        } catch (Exception e) {
            try {
                saida.write("Erro ao incluir o conte�do da URL \"" + url + "\"");
            } catch (IOException e1) {
            }
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return super.doEndTag();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String string) {
        url = string;
    }
}
