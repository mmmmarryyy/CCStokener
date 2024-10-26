package it.unica.citmusei.jepi.scheda;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.AbstractAction;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JApplet;
import javax.swing.SwingConstants;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import it.unica.citmusei.jepi.schedaxml.SchedaXml;
import it.unica.citmusei.jepi.uischedaxml.UISchedaXml;

public class SchedaJApplet extends JApplet {

    final UISchedaXml uiSchedaXml = new UISchedaXml();

    String cata;

    String server;

    /**
       Create ToolBar
    */
    private JToolBar newJToolBar() {
        final JToolBar jToolBar = new JToolBar();
        final Action saveAction = new AbstractAction("Salva") {

            public void actionPerformed(ActionEvent event) {
                writeCard();
            }

            ;
        };
        jToolBar.add(saveAction);
        return jToolBar;
    }

    /**
     * Create a URLString for a Query on exist db through the REST Get interface.
     */
    private String createURLStringExistRESTGetXQuery(String XQuery) {
        return "http://" + server + "/exist/rest/db/?" + "_query=declare default element namespace " + "\"http://citmusei.unica.it/jEpi/scheda\";" + XQuery;
    }

    /**
     * Retrive from exist db an xml file trough the REST interface.
     */
    private void readCard() {
        try {
            final String urlString = createURLStringExistRESTGetXQuery("//scheda[cata = \"" + cata + "\"]");
            InputStream inputStream = new URL(urlString).openStream();
            uiSchedaXml.read(inputStream);
            inputStream.close();
        } catch (MalformedURLException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    /**
     * Update the card current edited trough the REST interface.
     */
    private void writeCard() {
        try {
            new URL(createURLStringExistRESTGetXQuery("update value //scheda[cata = \"" + cata + "\"] with " + "\"replaced from /schede/scheda-... by jEpi-Scheda-Applet\"")).openStream().close();
            String urlString = "http://" + server + "/exist/rest/db/schede/" + "scheda-" + cata + ".xml";
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(urlString).openConnection();
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestMethod("PUT");
            OutputStream outputStream = httpURLConnection.getOutputStream();
            uiSchedaXml.write(outputStream);
            outputStream.close();
            httpURLConnection.getInputStream().close();
            httpURLConnection.disconnect();
        } catch (MalformedURLException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void init() {
        cata = getParameter("cata");
        server = getParameter("server");
        SchedaXml.INSTANCE.readSchedaXml();
        final Container contentPane = getContentPane();
        contentPane.add(newJToolBar(), BorderLayout.NORTH);
        contentPane.add(new SchedaJTabbedPane(uiSchedaXml, -1));
        readCard();
    }
}
