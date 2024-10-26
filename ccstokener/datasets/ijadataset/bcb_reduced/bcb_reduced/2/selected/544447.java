package it.pep.EsamiGWT.server;

import it.pep.EsamiGWT.client.dbBrowse.HtmlCodeForPrint;
import it.pep.EsamiGWT.client.hibernate.*;
import it.pep.EsamiGWT.server.DAO.EsamiDAO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.text.DateFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;

/**
 *
 * @author Francesco
 */
public class GestioneStampaAppello {

    /** crea una tabella con i dati dell'elaborato e gli spazi per 
     *  scrivere i dati del candidato
     */
    private static void addDatiElaboratoHTML(Appelli appello, Elaborati el, StringBuffer retVal, boolean primaVolta, String url, boolean anonimo) {
        final String spaziVuoti = "&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp";
        retVal.append(HtmlCodeForPrint.getInitTableEsami());
        retVal.append(HtmlCodeForPrint.getInitBody());
        String col1 = null;
        String col2 = null;
        String col3 = null;
        String col4 = null;
        col1 = HtmlCodeForPrint.creaColonna("Elaborato n. " + HtmlCodeForPrint.creaBarcode("" + appello.getID() + "-" + el.getID(), url));
        retVal.append(HtmlCodeForPrint.creaRiga(col1));
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        String dataAppello = df.format(appello.getDataAppello());
        col1 = HtmlCodeForPrint.creaColonna("Appello del " + dataAppello);
        retVal.append(HtmlCodeForPrint.creaRiga(col1));
        if (!anonimo) {
            col1 = HtmlCodeForPrint.creaColonna("<br>Cognome<br>");
            col2 = HtmlCodeForPrint.creaColonna(spaziVuoti);
            col3 = HtmlCodeForPrint.creaColonna("<br>Nome<br>");
            col4 = HtmlCodeForPrint.creaColonna(spaziVuoti);
            retVal.append(HtmlCodeForPrint.creaRiga(col1 + col2 + col3 + col4));
            col1 = HtmlCodeForPrint.creaColonna("<br>Matricola<br>");
            col2 = HtmlCodeForPrint.creaColonna(spaziVuoti);
            retVal.append(HtmlCodeForPrint.creaRiga(col1 + col2));
        }
        retVal.append(HtmlCodeForPrint.getEndBody());
        retVal.append(HtmlCodeForPrint.getEndTable());
        retVal.append("<hr><p>");
    }

    private static void addDomandeHTML(Elaborati el, StringBuffer retVal, int numColonne) {
        List<DomandeElaborati> listaDomandeElaborati = EsamiDAO.trovaDomandeElaborato(el.getID());
        retVal.append(HtmlCodeForPrint.getInitBody());
        int numDomanda = 0;
        int totaleDomande = listaDomandeElaborati.size();
        StringBuffer tmpRetVal = null;
        String colDom1 = null;
        for (DomandeElaborati de : listaDomandeElaborati) {
            String col1 = null;
            String col2 = null;
            String col3 = null;
            tmpRetVal = new StringBuffer();
            numDomanda++;
            if ((numColonne == 1) || (numColonne == 2 && numDomanda % 2 == 1)) {
                colDom1 = "";
            }
            col1 = HtmlCodeForPrint.creaColonna("" + numDomanda);
            col2 = HtmlCodeForPrint.creaColonna("&nbsp");
            col3 = HtmlCodeForPrint.creaColonna(de.getDomanda());
            tmpRetVal.append(HtmlCodeForPrint.creaRiga(col1 + col2 + col3));
            col1 = HtmlCodeForPrint.creaColonna("&nbsp");
            col2 = HtmlCodeForPrint.creaColonnaConBordo("1");
            col3 = HtmlCodeForPrint.creaColonna(de.getRisp1());
            tmpRetVal.append(HtmlCodeForPrint.creaRiga(col1 + col2 + col3));
            col1 = HtmlCodeForPrint.creaColonna("&nbsp");
            col2 = HtmlCodeForPrint.creaColonnaConBordo("2");
            col3 = HtmlCodeForPrint.creaColonna(de.getRisp2());
            tmpRetVal.append(HtmlCodeForPrint.creaRiga(col1 + col2 + col3));
            col1 = HtmlCodeForPrint.creaColonna("&nbsp");
            col2 = HtmlCodeForPrint.creaColonnaConBordo("3");
            col3 = HtmlCodeForPrint.creaColonna(de.getRisp3());
            tmpRetVal.append(HtmlCodeForPrint.creaRiga(col1 + col2 + col3));
            col1 = HtmlCodeForPrint.creaColonna("&nbsp");
            col2 = HtmlCodeForPrint.creaColonnaConBordo("4");
            col3 = HtmlCodeForPrint.creaColonna(de.getRisp4());
            tmpRetVal.append(HtmlCodeForPrint.creaRiga(col1 + col2 + col3));
            col1 = HtmlCodeForPrint.creaColonna("&nbsp");
            col2 = HtmlCodeForPrint.creaColonnaConBordo("5");
            col3 = HtmlCodeForPrint.creaColonna(de.getRisp5());
            tmpRetVal.append(HtmlCodeForPrint.creaRiga(col1 + col2 + col3));
            if (numColonne == 1) {
                retVal.append(tmpRetVal);
            } else if (numColonne == 2) {
                if (numDomanda % 2 == 0 || numDomanda > totaleDomande) {
                    retVal.append(HtmlCodeForPrint.creaRiga(HtmlCodeForPrint.creaColonna(HtmlCodeForPrint.creaTabellaEsami(colDom1)) + HtmlCodeForPrint.creaColonna(HtmlCodeForPrint.creaTabellaEsami(tmpRetVal.toString()))));
                } else {
                    colDom1 = tmpRetVal.toString();
                }
            }
        }
        retVal.append(HtmlCodeForPrint.getEndBody());
    }

    private static void addFooterHTML(Elaborati el, StringBuffer retVal, int numColonne) {
        String col1 = null;
        String col2 = null;
        String col3 = null;
        retVal.append(HtmlCodeForPrint.getInitFooter());
        col1 = HtmlCodeForPrint.creaColonna("&nbsp");
        col2 = HtmlCodeForPrint.creaColonna("&nbsp");
        col3 = HtmlCodeForPrint.creaColonna("<hr>Elaborato n. " + el.getID());
        if (numColonne == 1) {
            retVal.append(HtmlCodeForPrint.creaRiga(col1 + col2 + col3));
        } else {
            retVal.append(HtmlCodeForPrint.creaRiga(col3));
        }
        retVal.append(HtmlCodeForPrint.getEndFooter());
    }

    public static String creaStampa(HttpSession httpSess, Appelli appello, String url, boolean stampaDomande, boolean stampaRisposte, boolean anonimo, int numColonne) {
        List<Elaborati> listaElaborati = EsamiDAO.trovaElaboratiPerAppello(appello.getID());
        StringBuffer retVal = new StringBuffer(HtmlCodeForPrint.getInitHtml());
        boolean primaVolta = true;
        for (Elaborati el : listaElaborati) {
            if (stampaRisposte) {
                stampaFoglioRisposte(httpSess, appello, el, retVal, primaVolta, url, anonimo);
            }
            if (stampaDomande) {
                stampaFoglioDomande(appello, el, retVal, primaVolta, url, anonimo, numColonne);
            }
            primaVolta = false;
        }
        retVal.append(HtmlCodeForPrint.getEndHtml());
        LoggerApache.debug(retVal.toString());
        return retVal.toString();
    }

    private static void stampaFoglioDomande(Appelli appello, Elaborati el, StringBuffer retVal, boolean primaVolta, String url, boolean anonimo, int numColonne) {
        addDatiElaboratoHTML(appello, el, retVal, primaVolta, url, anonimo);
        retVal.append(HtmlCodeForPrint.getInitTableEsami());
        addFooterHTML(el, retVal, numColonne);
        addDomandeHTML(el, retVal, numColonne);
        retVal.append(HtmlCodeForPrint.getEndTable());
        retVal.append("<p STYLE=\"page-break-after: always\">&nbsp</p><p>");
    }

    private static void stampaFoglioRisposte(HttpSession httpSess, Appelli appello, Elaborati el, StringBuffer retVal, boolean primaVolta, String url, boolean anonimo) {
        InputStream is = null;
        String html = null;
        final int MAX_RIGHE_PER_PAGINA = 25;
        long totaleDomande = EsamiDAO.trovaQuanteDomandeElaborato(el.getID());
        long numPagine = 0, totalePagine = (long) Math.ceil(totaleDomande / 50.0);
        String urlBarcode = null;
        while (numPagine < totalePagine) {
            try {
                urlBarcode = URLEncoder.encode(HtmlCodeForPrint.creaBarcode("" + appello.getID() + "-" + el.getID() + "-" + (numPagine + 1), url), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(GestioneStampaAppello.class.getName()).log(Level.SEVERE, null, ex);
            }
            String jsp = url + "jsp/StampaRisposte.jsp?base=" + (numPagine * MAX_RIGHE_PER_PAGINA) + "&urlbarcode=" + urlBarcode;
            try {
                URL urlJSP = new URL(jsp);
                is = urlJSP.openStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int letto = is.read();
                while (letto != -1) {
                    baos.write(letto);
                    letto = is.read();
                }
                html = baos.toString();
            } catch (IOException ex) {
                Logger.getLogger(GestioneStampaAppello.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException ex) {
                    Logger.getLogger(GestioneStampaAppello.class.getName()).log(Level.SEVERE, null, ex);
                }
                numPagine++;
            }
        }
        retVal.append(html);
    }
}
