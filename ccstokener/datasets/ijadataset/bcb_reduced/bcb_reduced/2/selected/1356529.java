package peertrust.applet;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import org.protune.api.FilteredPolicy;
import org.protune.core.ProtuneMessage;
import org.protune.net.DispatcherMessage;
import org.protune.net.DispatcherStartNegotiationMessage;
import org.protune.net.EndNegotiationMessage;
import org.protune.net.NegotiationMessage;
import org.protune.net.NegotiationResult;
import org.protune.net.Pointer;
import org.protune.net.Service;
import org.protune.net.ServiceAvailableMessage;
import org.protune.net.ServiceRequestMessage;
import org.protune.net.StartNegotiationMessage;
import org.protune.net.WrongMessageTypeException;
import peertrust.common.pointer.HttpServerPointer;
import peertrust.common.pointer.HttpSuccessfulNegotiationResult;
import peertrust.common.pointer.HttpUnsuccessfulNegotiationResult;
import peertrust.service.HttpNegotiationService;
import netscape.javascript.JSObject;

/**
 * Applet that acts on the client side and performs the trust negotiation with the
 * server. If the user clicks a hyperlink, a JavaScript-script (see link.js in
 * the projekt for the server part) will forward the link to the askForURL-
 * method (the most important part in this class) in which all the trust
 * negotiation will be performed. 
 * @author Sebastian Wittler
 */
public class PeertrustApplet extends Applet implements IAddToLog {

    private static final long serialVersionUID = -8472160483364062011L;

    public String CREDENTIALS[];

    public static final String SERVICE = "SuchAService";

    private JList list;

    private JTextArea textLog;

    private JSObject jsobject;

    private JScrollPane scrollpane;

    /**
	 * @see Applet.init
	 */
    public void init() {
        super.init();
        List<String> liste = new LinkedList<String>();
        int i = 1;
        String str;
        while ((str = getParameter("cred" + (i++))) != null) liste.add(str);
        CREDENTIALS = (String[]) liste.toArray(new String[0]);
        setBackground(new Color(186, 193, 255));
        setLayout(new BorderLayout());
        Font font = Font.decode("Arial").deriveFont(Font.BOLD);
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Available Credentials");
        label.setFont(font);
        panel.add(label, BorderLayout.NORTH);
        Border border = BorderFactory.createLineBorder(new Color(0, 0, 0));
        list = new JList();
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setBorder(border);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        add(panel, BorderLayout.NORTH);
        list.setListData(CREDENTIALS);
        panel = new JPanel(new BorderLayout());
        label = new JLabel("Log");
        label.setFont(font);
        panel.add(label, BorderLayout.NORTH);
        textLog = new JTextArea();
        textLog.setWrapStyleWord(true);
        textLog.setLineWrap(true);
        textLog.setEditable(false);
        scrollpane = new JScrollPane(textLog);
        scrollpane.setBorder(border);
        panel.add(scrollpane, BorderLayout.CENTER);
        add(panel, BorderLayout.CENTER);
        jsobject = JSObject.getWindow(this);
    }

    /**
	 * Initializes and returns a connection to a URL to perform a http post request. 
	 * @param _url The URL to which an http post message should be sent.
	 * @return The Connection to the URL.
	 * @throws IOException
	 */
    private HttpURLConnection getHttpPostConnection(String _url) throws IOException {
        URL url = new URL(_url);
        HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
        urlcon.setUseCaches(false);
        urlcon.setDefaultUseCaches(false);
        urlcon.setDoOutput(true);
        urlcon.setDoInput(true);
        urlcon.setRequestProperty("Content-type", "application/octet-stream");
        urlcon.setAllowUserInteraction(false);
        HttpURLConnection.setDefaultAllowUserInteraction(false);
        urlcon.setRequestMethod("POST");
        return urlcon;
    }

    /**
	 * Sends an object to an URL (via http post).
	 * @param urlcon The connection to the URL.
	 * @param obj The Object that should be sent (must be serializable).
	 * @throws IOException
	 */
    private void writeObject(HttpURLConnection urlcon, Object obj) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(urlcon.getOutputStream());
        oos.writeObject(obj);
        oos.flush();
        oos.close();
    }

    /**
	 * Reads an object from an URL (http response).
	 * @param urlcon The connection to the URL.
	 * @return The Object returned by the URL.
	 * @throws IOException
	 */
    private Object readObject(HttpURLConnection urlcon) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(urlcon.getInputStream());
        Object obj = ois.readObject();
        ois.close();
        return obj;
    }

    /**
	 * Performs trust negotiation with the server if a resource (URL) can be accessed.
	 * If it was successful, the resource will be shown in the browser.
	 * The most important method of thhis class (also called from JavaScript script
	 * file: if a user clicks a link, this method will be called).
	 * @param _url The URL of the resource that should be accessed.
	 * @param target The target where the resource should be shown.
	 * @param url_waitpage The URL of the wait page that is shown while the
	 * 						negotiation is performed.
	 */
    public void askForURL(String _url, String target, String url_waitpage) {
        String strSessionID = null;
        try {
            addToLog("Request for " + _url);
            HttpURLConnection urlcon = getHttpPostConnection(_url + "?applet_access=true");
            writeObject(urlcon, new ServiceRequestMessage(SERVICE));
            Object obj = readObject(urlcon);
            if (!(obj instanceof ServiceAvailableMessage)) {
                addToLog("Negotiation service not available");
                return;
            }
            urlcon = getHttpPostConnection(_url + "?applet_access=true");
            writeObject(urlcon, new DispatcherStartNegotiationMessage(null, SERVICE));
            obj = readObject(urlcon);
            if (!(obj instanceof StartNegotiationMessage)) {
                addToLog("Server can't start negotiation");
                return;
            }
            StartNegotiationMessage snm = (StartNegotiationMessage) obj;
            Pointer pointer = snm.getPeerPointer();
            if (!(pointer instanceof HttpServerPointer)) {
                addToLog("Not a known pointer from server");
                return;
            }
            long nServiceID = ((HttpServerPointer) pointer).getServiceID();
            Service service = createService(nServiceID, pointer);
            if (service == null) {
                addToLog("Can't create service");
            }
            boolean bFirst = true;
            while (true) {
                if (bFirst) {
                    urlcon = getHttpPostConnection(_url + "?applet_access=true");
                    writeObject(urlcon, new DispatcherMessage(nServiceID, new ProtuneMessage(new FilteredPolicy[] { new FilteredPolicy(_url) }, null)));
                    bFirst = false;
                } else urlcon = ((HttpServerPointer) pointer).getURLConnection();
                obj = readObject(urlcon);
                if (!(obj instanceof DispatcherMessage)) {
                    addToLog("Not an DispatcherMessage");
                    return;
                }
                DispatcherMessage dm = (DispatcherMessage) obj;
                NegotiationMessage nm = dm.getNegotiationMessage();
                if (dm.getServiceID() != nServiceID) {
                    addToLog("Wrong service ID");
                    return;
                }
                if (nm instanceof ProtuneMessage) {
                    ProtuneMessage onm = (ProtuneMessage) nm;
                    try {
                        NegotiationMessage nmsg = service.perform(onm);
                        if (nmsg instanceof EndNegotiationMessage) break;
                    } catch (IOException ioe) {
                        return;
                    } catch (WrongMessageTypeException wmte) {
                        return;
                    }
                } else if (nm instanceof EndNegotiationMessage) {
                    EndNegotiationMessage enm = (EndNegotiationMessage) nm;
                    NegotiationResult nr = enm.getNegotiationResult();
                    if (nr instanceof HttpSuccessfulNegotiationResult) {
                        addToLog("Negotiation successful");
                        strSessionID = ((HttpSuccessfulNegotiationResult) nr).getSessionID();
                    } else if (nr instanceof HttpUnsuccessfulNegotiationResult) {
                        addToLog("Negotiation failed");
                        strSessionID = ((HttpUnsuccessfulNegotiationResult) nr).getSessionID();
                    }
                    break;
                }
            }
            addToLog("------------------------");
            if (strSessionID != null) {
                getAppletContext().showDocument(new URL(_url + "?session=" + strSessionID), target);
            }
        } catch (MalformedURLException e) {
            addToLog("" + e);
        } catch (IOException e) {
            addToLog("" + e);
        } catch (Exception e) {
            addToLog("" + e);
        }
    }

    /**
 * Creates and returns the client negotiation service. 
 * @param service_id The service ID of the servicee (must be the same as the server's)
 * @param pointerOtherPeer The Pointer (communication part) of the server.
 * @return The client negotiation service.
 */
    private Service createService(long service_id, Pointer pointerOtherPeer) {
        List<String> listKeys = new LinkedList<String>();
        List<String> listValues = new LinkedList<String>();
        int ids[] = list.getSelectedIndices();
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == 0) {
                listKeys.add(CREDENTIALS[0]);
                listValues.add("http://localhost:8081/MyServlet/trusted_company");
            } else if (ids[i] == 1) {
                listKeys.add(CREDENTIALS[1]);
                listValues.add("");
            } else if (ids[i] == 2) {
                listKeys.add(CREDENTIALS[2]);
                listValues.add("");
            } else if (ids[i] == 3) {
                listKeys.add(CREDENTIALS[3]);
                listValues.add("");
            }
        }
        Service service = new HttpNegotiationService((String[]) listKeys.toArray(new String[0]), (String[]) listValues.toArray(new String[0]), pointerOtherPeer, this);
        return service;
    }

    public void addToLog(String text) {
        textLog.setText(textLog.getText() + text + "\n");
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
        JScrollBar scrollbar = scrollpane.getVerticalScrollBar();
        scrollbar.setValue(scrollbar.getMaximum());
        scrollpane.repaint();
    }
}
