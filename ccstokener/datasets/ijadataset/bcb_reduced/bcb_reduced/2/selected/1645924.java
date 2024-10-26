package gov.sandia.ccaffeine.dc.user_iface.applet;

import javax.swing.JApplet;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;

/**
Retrieve and plot the data from ServletPlot.
 */
public class AppletFileSearch extends JApplet {

    protected boolean testing = false;

    /**
   * Constructor
   */
    public AppletFileSearch() {
    }

    /**
     * Initialize the applet
     */
    public void init() {
        try {
            System.out.print("getting name of host node...");
            URL url = this.getCodeBase();
            String hostName = url.getHost();
            System.out.println("done");
            System.out.println(hostName);
            String urlString = "http://" + hostName + ":8080/ccaffeine/servlet/ServletFileSearch";
            System.out.println(urlString);
            System.out.flush();
            System.out.println("creating FileSearch object...");
            FileSearch fileSearch = new FileSearch();
            if (!testing) fileSearch.setFolderName("/home/tomcat4/cca/dccafe"); else fileSearch.setFolderName("c:/Edward");
            if (!testing) fileSearch.setFilenameExtensions(new String[] { ".bld" }); else fileSearch.setFilenameExtensions(new String[] { ".txt" });
            System.out.println("done");
            System.out.flush();
            System.out.println(fileSearch.toString());
            fileSearch = (FileSearch) sendRequestToServletAndRetreiveResponse(urlString, fileSearch);
            System.out.println(fileSearch.toString());
            System.out.flush();
            textArea = new JTextArea(30, 80);
            textArea = new JTextArea();
            String filenames[] = fileSearch.getFoundFilenames();
            int numberOfFilenames = filenames.length;
            for (int i = 0; i < numberOfFilenames; i++) textArea.append(filenames[i] + "\n");
            getContentPane().setBackground(Color.white);
            getContentPane().setLayout(new GridLayout(1, 1));
            getContentPane().add(textArea);
            validate();
        } catch (java.lang.Throwable e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    JScrollPane scrollPane;

    JTextArea textArea;

    /**
    * Stupid JVM.  The textArea is not refreshing.
    * I using paint to manually refresh it.
    * @param g The graphics object that is connected to the terminal.
    */
    public void paint(Graphics g) {
        textArea.setText(textArea.getText());
    }

    /**
     * Send a request to the servlet and
     * retrieve the servlet's response.
     * @param urlString The url of the servlet.
     * @param object The request we are sending the servlet.
     * @return The response from the servlet
     * @throws IOException
     */
    protected FileSearch sendRequestToServletAndRetreiveResponse(String urlString, FileSearch fileSearch) throws IOException {
        System.out.print("creating URL...");
        URL urlOfServlet = null;
        try {
            urlOfServlet = new URL(urlString);
        } catch (java.net.MalformedURLException e) {
            throw new IOException(urlString + " is not a valid URL");
        }
        System.out.println("done");
        System.out.flush();
        System.out.print("connecting to servlet...");
        java.net.URLConnection connectionToServlet = urlOfServlet.openConnection();
        System.out.println("done");
        System.out.flush();
        System.out.print("setting up reading & writing...");
        connectionToServlet.setDoInput(true);
        connectionToServlet.setDoOutput(true);
        System.out.println("done");
        System.out.flush();
        System.out.print("turning off cache...");
        connectionToServlet.setUseCaches(false);
        connectionToServlet.setDefaultUseCaches(false);
        System.out.println("done");
        System.out.flush();
        System.out.println("setting up for binary data");
        connectionToServlet.setRequestProperty("Content-Type", "application/octet-stream");
        System.out.println("done");
        System.out.flush();
        System.out.print("getting output stream...");
        ObjectOutputStream outputStreamToServlet = new ObjectOutputStream(new BufferedOutputStream(connectionToServlet.getOutputStream()));
        System.out.println("done");
        System.out.flush();
        System.out.print("writing fileSearch object...");
        outputStreamToServlet.writeObject((java.io.Serializable) fileSearch);
        outputStreamToServlet.flush();
        System.out.println("done");
        System.out.flush();
        System.out.print("closing output stream...");
        outputStreamToServlet.close();
        System.out.println("done");
        System.out.flush();
        System.out.print("setting up input stream...");
        ObjectInputStream inputStreamFromServlet = new ObjectInputStream(new BufferedInputStream(connectionToServlet.getInputStream()));
        System.out.println("done");
        System.out.flush();
        System.out.print("reading FileSearch object...");
        try {
            fileSearch = (FileSearch) inputStreamFromServlet.readObject();
        } catch (java.lang.ClassNotFoundException e) {
            throw new IOException("Error.  Servlet did not send back a response.");
        }
        System.out.println("done");
        System.out.flush();
        System.out.print("closing input stream...");
        inputStreamFromServlet.close();
        System.out.println("done");
        System.out.flush();
        return (fileSearch);
    }

    public static void main(String args[]) {
        AppletFileSearch x = new AppletFileSearch();
        x.init();
    }
}
