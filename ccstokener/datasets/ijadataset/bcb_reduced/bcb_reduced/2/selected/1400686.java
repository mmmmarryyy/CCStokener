package com.bbn.vessel.author.workspace;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import org.apache.log4j.Logger;
import org.jdom.DocType;
import org.jdom.Element;
import org.jdom.JDOMException;
import com.bbn.vessel.author.startup.AuthorApplet;
import com.bbn.vessel.author.util.IOHelper;
import com.bbn.vessel.core.util.XMLHelper;

/**
 * an implementation of the file manager that talkes to a servlet over http
 *
 * @author jostwald
 *
 */
public class ServletFileManager implements FileManager {

    String dataSource;

    String protocol = AuthorApplet.getApplet().getCodeBase().getProtocol();

    String host = AuthorApplet.getApplet().getCodeBase().getHost();

    int port = AuthorApplet.getApplet().getCodeBase().getPort();

    private final Logger logger = Logger.getLogger(ServletFileManager.class);

    /**
     * make a get connection to the specified path
     *
     * @param path
     *            the path
     * @return the connection
     * @throws IOException
     *             if connection fails
     */
    public HttpURLConnection makeGetConnection(String path) throws IOException {
        return makeHttpsConnection(path, "GET");
    }

    /**
     * make a head connection to the specified path
     *
     * @param path
     *            the path
     * @return the connection
     * @throws IOException
     *             if connection fails
     */
    public HttpURLConnection makeHeadConnection(String path) throws IOException {
        return makeHttpsConnection(path, "HEAD");
    }

    /**
     * make a delete connection to the specified path
     *
     * @param path
     *            the path
     * @return the connection
     * @throws IOException
     *             if connection fails
     */
    private HttpURLConnection makeDeleteConnection(String path) throws IOException {
        return makeHttpsConnection(path, "DELETE");
    }

    /**
     * make a put connection to the specified path
     *
     * @param path
     *            the path
     * @return the connection
     * @throws IOException
     *             if connection fails
     */
    public HttpURLConnection makePutConnection(String path) throws IOException {
        return makeHttpsConnection(path, "PUT");
    }

    /**
     * @param path
     *            the path relative to the /vessel-servlet/ folder
     * @param method
     *            GET or DELETE or HEAD
     */
    private HttpURLConnection makeHttpsConnection(String path, String method) throws IOException {
        final StringBuffer urlSb = new StringBuffer();
        urlSb.append(protocol).append("://").append(host);
        if (port > 0) {
            urlSb.append(":").append(port);
        }
        urlSb.append("/vessel-servlet/");
        urlSb.append(path);
        if (logger.isDebugEnabled()) {
            logger.debug("making " + method + " connection to " + urlSb);
        }
        final URL url = new URL(urlSb.toString());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setUseCaches(false);
        con.setRequestMethod(method);
        con.setRequestProperty("CONTENT_TYPE", "application/octet-stream");
        if (method.equals("PUT")) {
            con.setDoOutput(true);
        }
        con.setDoInput(true);
        return con;
    }

    @Override
    public boolean clearDataSourceWithUserConfirm(Component dialogParent) throws IOException {
        int overwriteOption = JOptionPane.showConfirmDialog(dialogParent, "File " + dataSource + " exists.  Overwrite?", "Overwrite", JOptionPane.YES_NO_OPTION);
        if (overwriteOption == JOptionPane.NO_OPTION) {
            return false;
        }
        for (String fileName : listFiles()) {
            if ((fileName.equals("vessel-readme.txt")) || (fileName.equals(".svn"))) {
                continue;
            }
            HttpURLConnection con = makeDeleteConnection("data/" + dataSource + '/' + fileName);
            if (con.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT) {
                throw new IOException(con.getResponseCode() + ": couldn't delete " + dataSource);
            } else {
                con.getInputStream().close();
            }
        }
        return true;
    }

    @Override
    public boolean dataSourceExists() throws IOException {
        return fileExists("data/" + dataSource);
    }

    private boolean fileExists(String path) throws IOException {
        HttpURLConnection con = makeHeadConnection(path);
        boolean bool = false;
        if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
            bool = true;
            con.getInputStream().close();
        }
        return bool;
    }

    @Override
    public String getDataSourcePrettyName() throws IOException {
        return dataSource;
    }

    @Override
    public boolean hasDataSource() {
        return dataSource != null;
    }

    @Override
    public void importDefault(String fileName) throws IOException {
        if (!fileName.endsWith(".xml")) {
            fileName += ".xml";
        }
        HttpURLConnection getCon = makeGetConnection("data/defaults/" + fileName);
        if (getCon.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new FileNotFoundException("couldn't load " + fileName + " from defaults");
        }
        HttpURLConnection putCon = makePutConnection("data/" + dataSource + "/" + fileName);
        InputStream inputStream = getCon.getInputStream();
        OutputStream outputStream = putCon.getOutputStream();
        IOHelper.pipeStreams(inputStream, outputStream);
        inputStream.close();
        outputStream.close();
        if ((putCon.getResponseCode() != HttpURLConnection.HTTP_CREATED) && (putCon.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT)) {
            throw new IOException(putCon.getResponseCode() + ": couldn't write " + fileName + " to " + dataSource);
        }
        putCon.getInputStream().close();
    }

    @Override
    public boolean letUserSelectDataSource(Component dialogParent, String promptText) throws IOException {
        JPanel selectionPanel = new JPanel();
        selectionPanel.setLayout(new BorderLayout());
        HttpURLConnection con = makeGetConnection("data/");
        if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("couldn't get datasource listing");
        }
        InputStream inputStream = con.getInputStream();
        List<String> dataSources = parseDirectoryContents(inputStream);
        inputStream.close();
        dataSources = cullDirectoryList(dataSources);
        Collections.sort(dataSources);
        final JList dataSourceList = new JList(new DefaultListModel());
        dataSourceList.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        dataSourceList.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    ((AbstractButton) ((Container) SwingUtilities.getAncestorOfClass(JOptionPane.class, ((Component) e.getSource())).getComponent(1)).getComponent(0)).doClick();
                }
            }
        });
        for (String dataSource : dataSources) {
            ((DefaultListModel) dataSourceList.getModel()).addElement(dataSource);
        }
        dataSourceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionPanel.add(dataSourceList, BorderLayout.CENTER);
        JTextField newDataSourceField = new JTextField();
        newDataSourceField.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        newDataSourceField.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(FocusEvent e) {
                dataSourceList.setSelectedIndices(new int[] {});
            }
        });
        selectionPanel.add(newDataSourceField, BorderLayout.SOUTH);
        int option = JOptionPane.showConfirmDialog(null, selectionPanel, promptText, JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) {
            return false;
        }
        if (dataSourceList.getSelectedIndex() != -1) {
            dataSource = (String) dataSourceList.getSelectedValue();
            return true;
        } else {
            String text = newDataSourceField.getText();
            if ((text != null) && (text.length() > 0)) {
                dataSource = text.replace(" ", "_");
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public List<String> listFiles() throws IOException {
        HttpURLConnection con = makeGetConnection("data/" + dataSource + '/');
        if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException(con.getResponseCode() + ": couldn't get listing for " + dataSource);
        }
        InputStream inputStream = con.getInputStream();
        List<String> directoryContents = parseDirectoryContents(inputStream);
        inputStream.close();
        return directoryContents;
    }

    /**
     * remove any entries which are not directories containing a file called
     *
     * @param directoryContents
     * @return
     * @throws IOException
     */
    private List<String> cullDirectoryList(List<String> directoryContents) throws IOException {
        ArrayList<String> ret = new ArrayList<String>();
        for (String dir : directoryContents) {
            if (fileExists("data/" + dir + "/vessel-readme.txt")) {
                ret.add(dir);
            }
        }
        return ret;
    }

    @Override
    public Element load(String fileName, DocType... docTypes) throws JDOMException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("loading " + fileName + " from datasource " + dataSource);
        }
        return loadInternal(fileName, dataSource);
    }

    @Override
    public Element loadDefault(String fileName, DocType... docTypes) throws IOException, JDOMException {
        return loadInternal(fileName, "defaults", docTypes);
    }

    private Element loadInternal(String fileName, String dataSource2, DocType... docTypes) throws IOException, JDOMException {
        if (!fileName.endsWith(".xml")) {
            fileName += ".xml";
        }
        HttpURLConnection con = makeGetConnection("data/" + dataSource2 + "/" + fileName);
        if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new FileNotFoundException("couldn't load " + fileName + " from " + dataSource2);
        }
        InputStream inputStream = con.getInputStream();
        Element rootElement = XMLHelper.getRoot(inputStream, docTypes);
        inputStream.close();
        return rootElement;
    }

    @Override
    public void save(String fileName, Element content, DocType docType) throws IOException {
        if (!fileName.endsWith(".xml")) {
            fileName += ".xml";
        }
        ensureDataSourceExists();
        String path = "data/" + dataSource + "/" + fileName;
        HttpURLConnection con = makePutConnection(path);
        OutputStream outputStream = con.getOutputStream();
        XMLHelper.write(content, outputStream, null);
        outputStream.close();
        if ((con.getResponseCode() != HttpURLConnection.HTTP_CREATED) && (con.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT)) {
            throw new IOException(con.getResponseCode() + ": couldn't write " + fileName + " to " + dataSource);
        }
        con.getInputStream().close();
        if (!fileExists(path)) {
            throw new IOException("newly created file " + path + " isn't recognized by server.  perhaps caching is on." + "  see https://vessel.bbn.com/trac/wiki/SettingUpAuthorServlet" + " for configuration instructions");
        }
    }

    private void ensureDataSourceExists() throws IOException {
        if (!dataSourceExists()) {
            String path = "mkdirServlet?dir=data/" + dataSource;
            HttpURLConnection con = makeGetConnection(path);
            if (con.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
                throw new IOException("couldn't create " + dataSource + " got " + con.getResponseCode());
            }
            con.getInputStream().close();
            if (!dataSourceExists()) {
                throw new IOException("newly created folder " + dataSource + " isn't recognized by server.  perhaps caching is on." + "  see https://vessel.bbn.com/trac/wiki/SettingUpAuthorServlet" + " for configuration instructions");
            }
            String readmePath = "data/" + dataSource + "/vessel-readme.txt";
            HttpURLConnection con2 = makePutConnection(readmePath);
            OutputStream outputStream = con2.getOutputStream();
            outputStream.close();
            if ((con2.getResponseCode() != HttpURLConnection.HTTP_CREATED) && (con2.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT)) {
                throw new IOException(con2.getResponseCode() + ": couldn't write readme file to " + dataSource);
            }
            con2.getInputStream().close();
        }
    }

    @Override
    public void setDataSource(String dataSource) throws IOException {
        this.dataSource = dataSource;
    }

    @Override
    public void unSetDataSource() {
        this.dataSource = null;
    }

    private Element getRoot(InputStream is, DocType... docTypes) throws JDOMException, IOException {
        Element rootElement = XMLHelper.getRoot(is, docTypes);
        return rootElement;
    }

    private static List<String> parseDirectoryContents(InputStream inputStream) throws IOException {
        ArrayList<String> contents = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            Pattern p = Pattern.compile("<a href=.*><tt>(.*?)/?</tt>.*");
            Matcher m = p.matcher(line);
            if (m.matches()) {
                String fileName = m.group(1);
                if (fileName != null) {
                    contents.add(fileName.trim());
                }
            }
        }
        return contents;
    }

    /**
     *
     * @return url which is serving as the datasource
     */
    public String getDataSourceURL() {
        return dataSource;
    }

    @Override
    public boolean canRead(String fileName) {
        return true;
    }
}
