package ebadat;

import ebadat.swing.JFrameIngreso;
import ebadat.swing.JFrame_Centro;
import com.l2fprod.gui.plaf.skin.Skin;
import com.l2fprod.gui.plaf.skin.SkinLookAndFeel;
import ebadat.domain.Centro;
import ebadat.services.CentrosService;
import java.awt.Frame;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author DSILVA
 */
public class Main {

    static final String UPDATE_URL = "http://ebadat.sourceforge.net/files/update.xml";

    /** Creates a new instance of Main */
    public Main() throws SQLException {
        verificarVersion();
        List<Centro> centros;
        CentrosService centrosSrv = CentrosService.getInstance();
        while ((centros = centrosSrv.getAll()).isEmpty()) {
            int rpta = JOptionPane.showConfirmDialog(null, "Es la primera vez que ejecutar� esta aplicaci�n. Por favor, registre los datos del Centro", "Registrar Centro", JOptionPane.YES_NO_OPTION);
            if (rpta == JOptionPane.NO_OPTION) {
                System.exit(0);
            }
            new JFrame_Centro().setVisible(true);
        }
        if (centros.size() > 1) {
            CentroSelectDialog dialog = new CentroSelectDialog(new JFrame(), true);
            dialog.setVisible(true);
            centrosSrv.activar(dialog.getCentro());
        } else {
            centrosSrv.activar(centros.get(0));
        }
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                JFrame VentanaI = new JFrameIngreso();
                VentanaI.setTitle("EBADAT");
                VentanaI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                VentanaI.setVisible(true);
            }
        });
    }

    public static void main(String[] args) throws SQLException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            Skin theSkinToUse = SkinLookAndFeel.loadThemePack("skin-ebadat.zip");
            SkinLookAndFeel.setSkin(theSkinToUse);
            UIManager.setLookAndFeel(new SkinLookAndFeel());
        } catch (Exception ex) {
            logger.warn(ex, ex);
        }
        new Main();
    }

    Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Calendar cal = Calendar.getInstance();
            String version = String.format("%1$04d.%2$02d.%3$02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
            String remoteUrl = null;
            String remoteVersion = "0000.00.00";
            String path = System.getProperties().getProperty("user.dir");
            File propsFile = new File(path, ".ebadat.properties");
            Properties props = new Properties();
            try {
                FileInputStream fis = new FileInputStream(propsFile);
                props.load(fis);
                fis.close();
            } catch (FileNotFoundException ex) {
                logger.warn(ex, ex);
            } catch (IOException ex) {
                logger.warn(ex, ex);
            }
            if (!props.containsKey("ebadat.version")) {
                props.put("ebadat.version", version);
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            boolean proxyAuth = false;
            try {
                builder = factory.newDocumentBuilder();
                URL url = new URL(UPDATE_URL);
                URLConnection conn = url.openConnection(findProxy(new URI(UPDATE_URL)));
                conn.connect();
                Document doc = builder.parse(conn.getInputStream());
                NodeList nodeList = doc.getElementsByTagName("update");
                if (nodeList.getLength() > 0) {
                    Element updateTag = (Element) nodeList.item(0);
                    NodeList children = updateTag.getChildNodes();
                    Node node = updateTag.getFirstChild();
                    while (node != null) {
                        if (node instanceof Element) {
                            Element tag = (Element) node;
                            if (tag.getTagName().equals("version")) {
                                remoteVersion = tag.getTextContent();
                            } else if (tag.getTagName().equals("url")) {
                                remoteUrl = tag.getTextContent();
                            }
                        }
                        node = node.getNextSibling();
                    }
                    remoteUrl += "/" + remoteVersion + ".zip";
                }
                if (remoteVersion.compareTo(version) > 0) {
                    logger.info("descargando ultima version");
                    URL urlDownload = new URL(remoteUrl);
                    URLConnection connDownload = urlDownload.openConnection();
                    connDownload.connect();
                    DataInputStream dis = new DataInputStream(connDownload.getInputStream());
                    File downloadTemp = File.createTempFile("ebadat", "update");
                    downloadTemp.deleteOnExit();
                    FileOutputStream fos = new FileOutputStream(downloadTemp);
                    byte[] buffer = new byte[1024 * 512];
                    int cuenta = 0;
                    while ((cuenta = dis.read(buffer, 0, buffer.length)) >= 0) {
                        fos.write(buffer, 0, cuenta);
                    }
                    dis.close();
                    fos.close();
                    logger.info("descomprimiendo ultima version");
                    File targetDir = new File(path, "lib-update");
                    targetDir.mkdirs();
                    ZipInputStream zis = new ZipInputStream(new FileInputStream(downloadTemp));
                    ZipEntry zipEntry;
                    ZipFile zipFile = new ZipFile(downloadTemp);
                    while ((zipEntry = zis.getNextEntry()) != null) {
                        InputStream is = zipFile.getInputStream(zipEntry);
                        FileOutputStream fosx = new FileOutputStream(new File(targetDir, zipEntry.getName()));
                        while ((cuenta = is.read(buffer, 0, buffer.length)) >= 0) {
                            fosx.write(buffer, 0, cuenta);
                        }
                        fosx.close();
                        is.close();
                    }
                    zis.close();
                    props.put("ebadat.version", remoteVersion);
                }
            } catch (Exception ex) {
                logger.warn(ex);
            }
            if (proxyAuth) {
                try {
                    Proxy proxyUrl = findProxy(new URI(UPDATE_URL));
                    ProxySettingDialog dialog = new ProxySettingDialog(new Frame(), true);
                    InetSocketAddress addr = (InetSocketAddress) proxyUrl.address();
                    dialog.txtHttpProxy.setText(addr.getHostName());
                    dialog.txtHttpPuertoProxy.setText("" + addr.getPort());
                } catch (URISyntaxException ex) {
                    ex.printStackTrace();
                }
            }
            try {
                FileOutputStream fos = new FileOutputStream(propsFile);
                props.store(fos, "Version del Ebadat");
                fos.close();
            } catch (FileNotFoundException ex) {
                logger.warn(ex, ex);
            } catch (IOException ex) {
                logger.warn(ex, ex);
            }
            if (proxyAuth) {
                verificarVersion();
            }
        }
    };

    private void verificarVersion() {
        Thread update = new Thread(updateRunnable);
        update.start();
    }

    private Proxy findProxy(URI uri) {
        try {
            System.setProperty("java.net.useSystemProxies", "true");
            ProxySelector selector = ProxySelector.getDefault();
            List<Proxy> proxyList = selector.select(uri);
            if (proxyList.size() > 0) {
                return proxyList.get(0);
            }
        } catch (IllegalArgumentException e) {
            logger.warn(e, e);
        }
        return Proxy.NO_PROXY;
    }

    static final Logger logger = Logger.getLogger(Main.class);
}
