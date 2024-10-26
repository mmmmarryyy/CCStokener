package org.ibit.avanthotel.end.web.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Category;

/**
 * Aquest servlet simula el TPV
 *
 * @created 7 de octubre de 2003
 * @web:servlet name="SimulaPagament"
 *    description="simula el TPV"
 * @web:servlet-mapping url-pattern="/simulaPagament"
 */
public class SimulaPagament extends HttpServlet {

    private Category logger = Category.getInstance(SimulaPagament.class);

    /**
    * @exception ServletException
    */
    public void init() throws ServletException {
        logger.info("init()");
    }

    /** */
    public void destroy() {
        logger.info("destroy()");
    }

    /**
    * @param request
    * @param response
    * @exception ServletException
    * @exception IOException
    */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    /**
    * @param request
    * @param response
    * @exception ServletException
    * @exception IOException
    */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String nextUrl;
        boolean operationOK = request.getParameter("operationOK") != null;
        String operationId = request.getParameter("operationId");
        String idioma = request.getParameter("Idioma");
        String codigo_pedido = request.getParameter("Codigo_pedido");
        String merchantId = request.getParameter("MerchantID");
        String url = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        if (operationOK) {
            boolean reservaOK = isReservaOK(url + "/confirmaReserva", operationId, idioma, codigo_pedido, merchantId);
            if (reservaOK) {
                nextUrl = request.getParameter("URL_OK");
            } else {
                nextUrl = request.getParameter("URL_NOK");
            }
        } else {
            isReservaOK(url + "/anulaReserva", operationId, idioma, codigo_pedido, merchantId);
            nextUrl = request.getParameter("URL_NOK");
        }
        response.sendRedirect(nextUrl);
    }

    /**
    * retorna el valor per la propietat reservaOK
    *
    * @param urlAddress
    * @param operationId
    * @param idioma
    * @param codigo_pedido
    * @param merchantId
    * @return valor de reservaOK
    * @exception ServletException
    */
    private boolean isReservaOK(String urlAddress, String operationId, String idioma, String codigo_pedido, String merchantId) throws ServletException {
        StringBuffer buf = new StringBuffer();
        try {
            URL url = new URL(urlAddress + "?Num_operacion=" + operationId + "&Idioma=" + idioma + "&Codigo_pedido=" + codigo_pedido + "&MerchantID=" + merchantId);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            while ((str = in.readLine()) != null) {
                buf.append(str);
            }
            in.close();
        } catch (IOException e) {
            throw new ServletException(e);
        }
        return buf.indexOf("$*$OKY$*$") != -1;
    }
}
