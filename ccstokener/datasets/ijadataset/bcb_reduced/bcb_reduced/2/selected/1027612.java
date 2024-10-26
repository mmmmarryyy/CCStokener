package at.dotti.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JWindow;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.ini4j.Ini;
import org.ini4j.Ini.Section;
import org.ini4j.InvalidIniFormatException;
import at.dotti.sf.files.LatestFilesMain;
import at.dotti.tools.lc.LC;

public class XExternalEditor extends MouseAdapter implements MouseMotionListener {

    private static final Logger _logger = Logger.getLogger(XExternalEditor.class);

    private static final DecimalFormat nf = new DecimalFormat("###.0");

    private static final byte[] BOM = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        new XExternalEditor(args);
    }

    private JProgressBar bar = null;

    private HttpClient client;

    private final String cookie = "";

    private String extension = "";

    private JFrame frame;

    private boolean mouseDragged = false;

    private String script = "";

    private String special = "";

    private JTextField text = null;

    private File tmpFile = null;

    private String type = "";

    private JButton uploadBtn = null;

    private JButton previewBtn = null;

    private JButton enterUserPwd = null;

    private JButton closeBtn = null;

    private final JWindow w = new JWindow();

    private int x, y;

    private String time;

    private String starttime;

    private String pagetitle;

    private String token;

    /**
	 * 
	 */
    public XExternalEditor(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        Timer tt = new Timer("sfnet-updater", true);
        tt.schedule(new TimerTask() {

            @Override
            public void run() {
                try {
                    LatestFilesMain l = new LatestFilesMain();
                    if (l.update()) {
                        URL url = l.getLatest();
                        if (JOptionPane.showConfirmDialog(null, LC.tr("A new version of XExternalEditor is available!\nDo you wan't to get it now?"), LC.tr("New Version"), JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE) == JOptionPane.YES_OPTION) {
                            Desktop.getDesktop().browse(url.toURI());
                        }
                    }
                } catch (Throwable e) {
                    XExternalEditor._logger.error("", e);
                }
            }
        }, 100, 1000 * 60 * 60 * 24);
        if (args.length == 0) {
            JFileChooser fc = new JFileChooser(new File(System.getProperty("user.home")));
            fc.setFileFilter(new FileFilter() {

                @Override
                public boolean accept(File f) {
                    return f.getName().endsWith(".ini") || f.isDirectory();
                }

                @Override
                public String getDescription() {
                    return LC.tr("ini files");
                }
            });
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                args = new String[] { fc.getSelectedFile().getAbsolutePath() };
            } else {
                System.exit(0);
            }
        }
        if (new File(args[0]).exists()) {
            this.iniFileFromServer = new File(args[0]);
            try {
                this.loadIniFile();
                this.loadSettings();
                this.showGui();
                if (!this.processFile()) {
                    this.close();
                }
            } catch (IOException e) {
                XExternalEditor._logger.error("", e);
                JOptionPane.showMessageDialog(this.frame, e.getClass().getSimpleName() + ": " + e.getMessage(), LC.tr("Error"), JOptionPane.ERROR_MESSAGE);
            } catch (NotSupportedException e) {
                XExternalEditor._logger.error("", e);
                JOptionPane.showMessageDialog(this.frame, LC.tr("XExternalEditor >>\nA feature you wan't is currently not supported!").concat("\n\nInfo: <" + e.getMessage() + ">"), LC.tr("Error"), JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(new JFrame(), LC.tr("The given parameter is no ini file!\nXExternalEditor will be closed."), LC.tr("Error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
	 * 
	 */
    private void close() {
        this.frame.dispose();
        this.w.dispose();
        this.iniFile.setProperty("x", String.valueOf(this.frame.getLocation().x));
        this.iniFile.setProperty("y", String.valueOf(this.frame.getLocation().y));
        try {
            this.iniFile.store(new FileOutputStream(this.getGuiIniFile()), "Gui Properties");
        } catch (FileNotFoundException e) {
            XExternalEditor._logger.error(LC.tr("The GUI properties cannot be stored"), e);
        } catch (IOException e) {
            XExternalEditor._logger.error(LC.tr("The GUI properties cannot be stored"), e);
        }
    }

    /**
	 * @return
	 * @throws IOException
	 */
    private File getGuiIniFile() throws IOException {
        if (this.url != null) {
            URL url = new URL(this.url);
            String customFile = url.getHost().concat(this.path).replace(".", "_").replace("/", "_");
            return new File(System.getProperty("user.home") + File.separator + "XExternalEditor" + File.separator + customFile + ".ini");
        }
        throw new IOException("no ini file saved yet!");
    }

    /**
	 * @param url
	 * @return
	 * @throws NotSupportedException
	 */
    private String getFileName(URL url) throws NotSupportedException {
        if (this.type.equalsIgnoreCase("Edit file")) {
            return url.getFile();
        } else if (this.type.equalsIgnoreCase("Edit text")) {
            String file = url.getFile();
            if (file.indexOf("title") != -1) {
                Pattern p = Pattern.compile(".*title=([^&]*)&?.*");
                Matcher m = p.matcher(file);
                if (m.find()) {
                    this.pagetitle = m.group(1);
                    String filename = this.pagetitle.replace(':', '_');
                    filename = filename.replace('.', '_');
                    try {
                        return URLDecoder.decode(filename, "UTF-8").concat(".").concat(this.extension);
                    } catch (UnsupportedEncodingException e) {
                        XExternalEditor._logger.error("", e);
                        return this.pagetitle.concat(".").concat(this.extension);
                    }
                }
            }
            throw new NotSupportedException("url = " + url);
        }
        throw new NotSupportedException("type = " + this.type);
    }

    /**
	 * @param url
	 * @return downloaded file
	 * @throws IOException
	 * @throws NotSupportedException
	 */
    private File download(String urlString) throws IOException, NotSupportedException {
        OutputStreamWriter osw = null;
        InputStream is = null;
        OutputStream fos = null;
        URL url = new URL(urlString);
        File file = new File(System.getProperty("user.home") + File.separator + "XExternalEditor" + File.separator + "tmp" + File.separator + new File(this.getFileName(url)).getName());
        if (file.exists()) {
            file.delete();
        } else if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        file.createNewFile();
        file.deleteOnExit();
        try {
            if (this.type.equalsIgnoreCase("edit file")) {
                this.text.setText(LC.tr("Establishing the connection"));
                this.bar.setIndeterminate(true);
                URLConnection con = url.openConnection();
                con.setUseCaches(false);
                con.addRequestProperty("User-Agent", "X-External-Editor");
                con.setRequestProperty("Cookie", this.cookie);
                con.connect();
                is = con.getInputStream();
                fos = new FileOutputStream(file);
                byte[] buf = new byte[1024 * 8];
                double len = -1, sumlen = 0, nano = System.nanoTime();
                double contentLength = con.getContentLength();
                this.text.setText(LC.tr("Downloading the file"));
                this.bar.setIndeterminate(false);
                this.bar.setValue(0);
                while ((len = is.read(buf)) != -1) {
                    fos.write(buf, 0, (int) len);
                    sumlen += len;
                    int percent = (int) ((sumlen / contentLength) * 100);
                    double sec = (System.nanoTime() - nano) / sumlen;
                    String secString = (int) sec + " B";
                    if (sec > 1024) {
                        secString = XExternalEditor.nf.format(sec / 1024) + " KB";
                    }
                    this.text.setText(LC.tr("Downloading the file").concat(" [" + percent + " %] [" + secString + "/s]"));
                    this.bar.setValue(percent);
                }
                this.bar.setValue(100);
                this.text.setText(LC.tr("File downloaded"));
            } else if (this.type.equalsIgnoreCase("edit text")) {
                this.text.setText(LC.tr("Downloading the wiki text"));
                GetMethod gethtmlpage = new GetMethod(urlString);
                int status = this.client.executeMethod(gethtmlpage);
                if (status != 200) {
                    this.showHTMLFile(gethtmlpage.getResponseBodyAsString());
                    throw new RuntimeException(LC.tr("Wiki page could not be loaded!"));
                }
                this.text.setText(LC.tr("Analyzing the wiki text"));
                String html = gethtmlpage.getResponseBodyAsString();
                Pattern pText = Pattern.compile(".*<textarea[^>]+name=\"wpTextbox1\"[^>]*>(.*)</textarea>.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                Matcher mText = pText.matcher(html);
                String text = null;
                if (mText.find()) {
                    text = mText.group(1);
                    text = StringEscapeUtils.unescapeHtml(text);
                    if (!text.contains(System.getProperty("line.separator"))) {
                        _logger.warn("changing line ends to " + System.getProperty("line.separator"));
                        text = text.replace("\n", System.getProperty("line.separator"));
                    }
                } else {
                    throw new RuntimeException(LC.tr("Wiki text not found in HTML document!"));
                }
                Pattern pTime = Pattern.compile(".*<input type='hidden' value=\"(.*)\" name=\"wpEdittime\" />.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                Matcher mTime = pTime.matcher(html);
                if (mTime.find()) {
                    this.time = mTime.group(1);
                } else {
                    throw new RuntimeException(LC.tr("Edit time not found in HTML document!"));
                }
                Pattern pStarttime = Pattern.compile(".*<input type='hidden' value=\"(.*)\" name=\"wpStarttime\" />.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                Matcher mStarttime = pStarttime.matcher(html);
                if (mStarttime.find()) {
                    this.starttime = mStarttime.group(1);
                } else {
                    throw new RuntimeException(LC.tr("Start time not found in HTML document!"));
                }
                Pattern pToken = Pattern.compile(".*<input type=[\"']hidden[\"'] value=\"([^\"]*)\" name=\"wpEditToken\" />.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                Matcher mToken = pToken.matcher(html);
                if (mToken.find()) {
                    this.token = mToken.group(1);
                } else {
                    throw new RuntimeException(LC.tr("Edit token not found in HTML document!"));
                }
                fos = new FileOutputStream(file);
                osw = new OutputStreamWriter(fos, this.responseCharSet);
                if (this.responseCharSet.equals("UTF-8")) {
                    if (!text.startsWith(new String(BOM))) {
                        fos.write(BOM);
                    }
                }
                osw.append(text);
                this.text.setText(LC.tr("Wiki text downloaded!"));
            } else {
                throw new NotSupportedException("type = " + this.type);
            }
        } finally {
            if (osw != null) {
                osw.close();
            }
            if (fos != null) {
                fos.close();
            }
            if (is != null) {
                is.close();
            }
        }
        return file;
    }

    private void setProxy() {
        if (this.iniFile.getProperty("useProxy") != null && this.iniFile.getProperty("useProxy").equals("true") && this.iniFile.getProperty("proxyHost") != null && this.iniFile.getProperty("proxyPort") != null) {
            try {
                this.client.getHostConfiguration().setProxy(this.iniFile.getProperty("proxyHost"), Integer.parseInt(this.iniFile.getProperty("proxyPort")));
            } catch (Exception e) {
                XExternalEditor._logger.error("", e);
                JOptionPane.showMessageDialog(this.frame, LC.tr("Couldn't set the Proxy.\nCheck the settings.") + "\n\n" + e.getLocalizedMessage(), LC.tr("Error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
	 * <p>
	 * Resolves the real script path (http redirect)
	 * 
	 * @throws IOException
	 * @throws HttpException
	 */
    private void resolveScript() throws HttpException, IOException {
        GetMethod getLoginPage = new GetMethod(new URL(this.script).toString());
        getLoginPage.setFollowRedirects(false);
        int status = this.client.executeMethod(getLoginPage);
        Header location = getLoginPage.getResponseHeader("location");
        this.responseCharSet = getLoginPage.getResponseCharSet();
        if (status == HttpStatus.SC_NOT_FOUND) {
            throw new RuntimeException(LC.tr("The URL from the ini file isn't correct").concat(": ") + this.script);
        }
        if (location != null) {
            this.script = resolveLocation(location, status).getValue();
            if (!this.script.endsWith("/")) {
                _logger.warn("url trancated from " + this.script);
                this.script = this.script.substring(0, this.script.lastIndexOf("/") + 1);
                _logger.warn("                to " + this.script);
            }
        }
    }

    /**
	 * <p>
	 * Handle redirects
	 * 
	 * @param location
	 * @param status
	 * @throws IOException
	 * @throws HttpException
	 */
    private Header resolveLocation(Header location, int status) throws HttpException, IOException {
        GetMethod get = new GetMethod(new URL(this.script).toString());
        get.setFollowRedirects(false);
        if (status == HttpStatus.SC_MOVED_TEMPORARILY || status == HttpStatus.SC_MOVED_PERMANENTLY) {
            int i = 0;
            while ((status == HttpStatus.SC_MOVED_TEMPORARILY || status == HttpStatus.SC_MOVED_PERMANENTLY) && location != null) {
                if (++i > 3) {
                    throw new RuntimeException(LC.tr("Too many redirects - operation canceled"));
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                get = new GetMethod(location.getValue());
                status = this.client.executeMethod(get);
                if (get.getResponseHeader("location") == null) {
                    return location;
                }
                location = get.getResponseHeader("location");
            }
            if (status != 200) {
                this.showHTMLFile(get.getResponseBodyAsString());
                throw new RuntimeException(LC.tr("Error resolving the real script path").concat(": ") + this.script);
            }
        } else if (status != 200) {
            this.showHTMLFile(get.getResponseBodyAsString());
            throw new RuntimeException(LC.tr("Error resolving the real script path").concat(": ") + this.script);
        }
        return location;
    }

    private final Properties iniFile = new Properties();

    private String responseCharSet = "UTF-8";

    private JToggleButton uploadOnSaveButton;

    private File iniFileFromServer;

    private String engine;

    private String server;

    private String path;

    private Section file;

    private String url;

    private Section process;

    /**
	 * @throws IOException
	 */
    private void saveSettings() throws IOException {
        OutputStream os = new FileOutputStream(this.getGuiIniFile());
        this.iniFile.store(os, "Gui Properties");
        os.close();
    }

    /**
	 * @param iniFileFromServer
	 */
    private void loadSettings() {
        try {
            this.iniFile.load(new FileInputStream(this.getGuiIniFile()));
        } catch (IOException e) {
            XExternalEditor._logger.error("", e);
        }
    }

    /**
	 * @param forceDialog
	 * @return
	 * @throws IOException
	 */
    private boolean login(boolean forceDialog) throws IOException {
        this.loadSettings();
        String username = "";
        String password = "";
        if (forceDialog || this.iniFile.getProperty("username") == null) {
            UserPwdProxyDialog d = new UserPwdProxyDialog(this.frame, this.iniFile.getProperty("username"), this.iniFile.getProperty("proxyHost"), this.iniFile.getProperty("proxyPort"), this.iniFile.getProperty("useProxy") != null && this.iniFile.getProperty("useProxy").equals("true"));
            d.setVisible(true);
            if (d.getStatus() == JOptionPane.OK_OPTION) {
                this.iniFile.setProperty("username", d.getUsername());
                this.iniFile.setProperty("password", d.getPassword());
                this.iniFile.setProperty("useProxy", String.valueOf(d.useProxy()));
                if (d.useProxy()) {
                    this.iniFile.setProperty("proxyHost", d.getProxyHost());
                    this.iniFile.setProperty("proxyPort", d.getProxyPort());
                } else {
                    this.iniFile.remove("proxyHost");
                    this.iniFile.remove("proxyPort");
                }
                this.saveSettings();
            } else {
                XExternalEditor._logger.error(LC.tr("Login canceled - program aborted"));
                return false;
            }
        }
        this.client = new HttpClient();
        this.client.getParams().setParameter("http.useragent", "X-External-Editor");
        GetMethod getLoginPage = new GetMethod(new URL(this.script + "?title=" + this.special + ":UserLogin").toString());
        int status = this.client.executeMethod(getLoginPage);
        Header location = getLoginPage.getResponseHeader("location");
        this.responseCharSet = getLoginPage.getResponseCharSet();
        if (status == HttpStatus.SC_NOT_FOUND) {
            throw new RuntimeException(LC.tr("The URL from the ini file isn't correct").concat(": ") + this.script);
        }
        if (status == HttpStatus.SC_MOVED_TEMPORARILY) {
            int i = 3;
            while (status == HttpStatus.SC_MOVED_TEMPORARILY && location != null) {
                if (++i > 3) {
                    throw new RuntimeException(LC.tr("Too many redirects - operation canceled"));
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                getLoginPage = new GetMethod(location.getValue());
                status = this.client.executeMethod(getLoginPage);
                location = getLoginPage.getResponseHeader("location");
            }
            if (status != 200) {
                this.showHTMLFile(getLoginPage.getResponseBodyAsString());
                return false;
            }
        } else if (status != 200) {
            this.showHTMLFile(getLoginPage.getResponseBodyAsString());
            return false;
        }
        URL login_url = new URL(this.script + "?title=" + this.special + ":Userlogin&action=submitlogin");
        _logger.debug(login_url);
        PostMethod post = new PostMethod(login_url.toString());
        this.setProxy();
        String loginToken = this.parseLoginToken(getLoginPage.getResponseBody());
        username = this.iniFile.getProperty("username");
        password = this.iniFile.getProperty("password");
        post.addParameter("wpLoginToken", loginToken);
        post.addParameter("wpName", username);
        post.addParameter("wpPassword", password);
        post.addParameter("wpRemember", "1");
        post.addParameter("wpLoginAttempt", "Log%20in");
        status = this.client.executeMethod(post);
        location = post.getResponseHeader("location");
        if (status == HttpStatus.SC_MOVED_TEMPORARILY) {
            int i = 0;
            while (status == HttpStatus.SC_MOVED_TEMPORARILY && location != null) {
                if (++i > 3) {
                    throw new RuntimeException(LC.tr("Too many redirects - operation canceled"));
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                PostMethod post2 = new PostMethod(location.getValue());
                post2.addParameters(post.getParameters());
                status = this.client.executeMethod(post2);
                location = post2.getResponseHeader("location");
            }
            if (status != 200) {
                this.showHTMLFile(post.getResponseBodyAsString());
                return false;
            }
        } else if (status != 200) {
            this.showHTMLFile(post.getResponseBodyAsString());
            return false;
        }
        return true;
    }

    /**
	 * @param responseBody
	 * @return
	 */
    private String parseLoginToken(byte[] responseBody) {
        String body = new String(responseBody);
        Pattern P = Pattern.compile("name=\"wpLoginToken\" value=\"([a-z0-9]+)\"");
        Matcher m = P.matcher(body);
        if (m.find()) {
            return m.group(1);
        }
        throw new RuntimeException(LC.tr("no login token found"));
    }

    @Override
    public void mouseDragged(final MouseEvent e) {
        if (!this.mouseDragged) {
            this.x = e.getX();
            this.y = e.getY();
            this.w.setSize(this.frame.getSize());
            this.w.setVisible(true);
            this.w.getContentPane().setBackground(Color.BLACK);
        }
        this.w.setLocation((int) (this.frame.getLocation().getX() + (e.getX() - this.x)), (int) (this.frame.getLocationOnScreen().getY() + (e.getY() - this.y)));
        this.mouseDragged = true;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (this.mouseDragged) {
            int ya = (int) (this.frame.getLocationOnScreen().getY() + (e.getY() - this.y));
            if (ya < 10) {
                ya = 0;
            }
            if (ya > Toolkit.getDefaultToolkit().getScreenSize().getHeight() - this.frame.getHeight() - 10) {
                ya = (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() - this.frame.getHeight());
            }
            int xa = (int) (this.frame.getLocation().getX() + (e.getX() - this.x));
            if (xa < 10) {
                xa = 0;
            }
            if (xa > Toolkit.getDefaultToolkit().getScreenSize().getWidth() - this.frame.getWidth() - 10) {
                xa = (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - this.frame.getWidth());
            }
            this.frame.setLocation(xa, ya);
            this.mouseDragged = false;
            this.w.setVisible(false);
        }
    }

    /**
	 * @param iniFile
	 */
    private boolean processFile() {
        if (this.url != null) {
            try {
                boolean login = true, forceDialog = false;
                while (login) {
                    try {
                        login = false;
                        if (!this.login(forceDialog)) {
                            JOptionPane.showMessageDialog(this.frame, LC.tr("The application was not able to login at the wiki"));
                            boolean openDialog = true;
                            boolean loggedin = false;
                            while (openDialog && (loggedin = this.login(true)) == false) {
                                openDialog = JOptionPane.showConfirmDialog(this.frame, LC.tr("re-enter Benutzername/Passwort?"), LC.tr("Question"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                            }
                            if (!loggedin) {
                                return false;
                            }
                        }
                    } catch (Exception e) {
                        XExternalEditor._logger.error("", e);
                        JOptionPane.showMessageDialog(this.frame, e.getClass().getSimpleName() + ": " + e.getMessage(), LC.tr("Error at login"), JOptionPane.ERROR_MESSAGE);
                        if (JOptionPane.showConfirmDialog(this.frame, LC.tr("Do you wan't to change username, password and/or proxy?"), LC.tr("Change settings?"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                            forceDialog = true;
                            login = true;
                        }
                    }
                }
                URL tempUrl = new URL(this.url);
                String sTitleDocumentName = new File(this.getFileName(tempUrl)).getName();
                JPanel jPane = (JPanel) this.frame.getContentPane();
                jPane.setBorder(BorderFactory.createTitledBorder(sTitleDocumentName));
                this.tmpFile = this.download(this.url);
                if (this.frame == null || !this.frame.isVisible()) {
                    return false;
                }
                if (Desktop.isDesktopSupported()) {
                    this.text.setText(LC.tr("Opening file in external program"));
                    this.listenOnChanges(this.tmpFile);
                    try {
                        Desktop.getDesktop().open(this.tmpFile);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(this.frame, LC.tr("The file has no application registered to launch it with!\nPleaser register an application for the file extension '" + this.extension + "'."));
                    }
                    this.text.setText(LC.tr("Ready. You can now edit the file ..."));
                    this.bar.setVisible(false);
                    this.uploadBtn.setEnabled(!this.getUploadOnSaveButton().isSelected());
                    this.previewBtn.setEnabled(this.type.equalsIgnoreCase("edit text"));
                    return true;
                } else {
                    JOptionPane.showMessageDialog(this.frame, LC.tr("JAVA Desktop functions are not allowed!\nI have to stop the program!"));
                }
            } catch (IOException e) {
                XExternalEditor._logger.error("", e);
                JOptionPane.showMessageDialog(this.frame, e.getClass().getSimpleName() + ": " + e.getMessage(), LC.tr("Error"), JOptionPane.ERROR_MESSAGE);
            } catch (NotSupportedException e) {
                XExternalEditor._logger.error("", e);
                JOptionPane.showMessageDialog(this.frame, e.getClass().getSimpleName() + ": " + e.getMessage(), LC.tr("Error"), JOptionPane.ERROR_MESSAGE);
            } catch (Throwable e) {
                XExternalEditor._logger.error("", e);
                JOptionPane.showMessageDialog(this.frame, e.getClass().getSimpleName() + ": " + e.getMessage(), LC.tr("Error"), JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }

    /**
	 * @param iniFile
	 * @throws InvalidIniFormatException
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NotSupportedException
	 */
    private void loadIniFile() throws InvalidIniFormatException, FileNotFoundException, IOException, NotSupportedException {
        Ini ini4jFile = new Ini();
        ini4jFile.load(new FileInputStream(this.iniFileFromServer));
        this.process = ini4jFile.get("Process");
        this.special = this.process.get("Special namespace");
        this.type = this.process.get("Type");
        this.engine = this.process.get("Engine");
        this.script = this.process.get("Script");
        this.server = this.process.get("Server");
        this.path = this.process.get("Path");
        this.special = "Special";
        if (!this.type.equalsIgnoreCase("Edit file") && !this.type.equalsIgnoreCase("Edit text")) {
            throw new NotSupportedException("type = " + this.type);
        }
        if (!this.engine.equalsIgnoreCase("MediaWiki")) {
            throw new NotSupportedException("engine = " + this.engine);
        }
        this.file = ini4jFile.get("File");
        this.extension = this.file.get("Extension");
        this.url = this.file.get("URL");
    }

    /**
	 * @param file
	 */
    private void listenOnChanges(final File file) {
        final AtomicLong originalLastModified = new AtomicLong(file.lastModified());
        Timer changeTimer = new Timer(true);
        changeTimer.schedule(new TimerTask() {

            private boolean checking = false;

            @Override
            public void run() {
                if (this.checking) {
                    return;
                }
                this.checking = true;
                if (file.exists() && originalLastModified.get() < file.lastModified()) {
                    if (XExternalEditor.this.getUploadOnSaveButton().isSelected()) {
                        new Thread(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    XExternalEditor.this.upload(file, false);
                                    originalLastModified.set(file.lastModified());
                                } catch (Exception e) {
                                    XExternalEditor._logger.error("", e);
                                    JOptionPane.showMessageDialog(XExternalEditor.this.frame, e.getMessage(), LC.tr("Error"), JOptionPane.ERROR_MESSAGE);
                                } finally {
                                    checking = false;
                                }
                            }
                        }, "upload").start();
                    } else {
                        this.checking = false;
                    }
                } else {
                    this.checking = false;
                }
            }
        }, 100, 1000);
    }

    /**
	 * @throws IOException
	 * @throws HeadlessException
	 * 
	 */
    private void showGui() throws HeadlessException, IOException {
        this.w.setVisible(false);
        this.w.setBackground(Color.BLACK);
        this.frame = new JFrame("XExternalEditor");
        this.frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                XExternalEditor.this.close();
            }
        });
        this.frame.setResizable(false);
        this.frame.setAlwaysOnTop(true);
        this.frame.setUndecorated(true);
        this.frame.setIconImage(Toolkit.getDefaultToolkit().getImage("resources/icons/logo.png"));
        JPanel cPane = new JPanel();
        String sTitleDocumentName = "XExternalEditor-UploadProxy";
        cPane.setBorder(BorderFactory.createTitledBorder(sTitleDocumentName));
        cPane.setLayout(new BorderLayout());
        this.frame.setContentPane(cPane);
        JPanel msgPane = new JPanel();
        msgPane.setLayout(new BoxLayout(msgPane, BoxLayout.Y_AXIS));
        JToolBar btnPane = new JToolBar();
        btnPane.setFloatable(false);
        this.text = new JTextField();
        this.text.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        this.text.setColumns(30);
        this.text.setEditable(false);
        msgPane.add(this.text);
        this.bar = new JProgressBar();
        this.bar.setPreferredSize(new Dimension(140, 8));
        msgPane.add(this.bar);
        this.uploadBtn = new JButton(new AbstractAction(LC.tr("Save"), new ImageIcon(Toolkit.getDefaultToolkit().getImage("resources/icons/save.png"))) {

            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            XExternalEditor.this.upload(XExternalEditor.this.tmpFile, false);
                        } catch (Exception e1) {
                            XExternalEditor._logger.error("", e1);
                            JOptionPane.showMessageDialog(XExternalEditor.this.frame, e1.getMessage(), LC.tr("Error"), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }).start();
            }
        });
        this.uploadBtn.setEnabled(!this.getUploadOnSaveButton().isSelected());
        this.previewBtn = new JButton(new AbstractAction(LC.tr("Preview"), new ImageIcon(Toolkit.getDefaultToolkit().getImage("resources/icons/preview.png"))) {

            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            XExternalEditor.this.upload(XExternalEditor.this.tmpFile, true);
                        } catch (Exception e1) {
                            XExternalEditor._logger.error("", e1);
                            JOptionPane.showMessageDialog(XExternalEditor.this.frame, e1.getMessage(), LC.tr("Error"), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }).start();
            }
        });
        this.previewBtn.setEnabled(this.type.equalsIgnoreCase("edit text"));
        this.enterUserPwd = new JButton(new AbstractAction(LC.tr("Settings"), new ImageIcon(Toolkit.getDefaultToolkit().getImage("resources/icons/userpwd.png"))) {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                try {
                    XExternalEditor.this.login(true);
                } catch (IOException e) {
                    XExternalEditor._logger.error("", e);
                    JOptionPane.showMessageDialog(XExternalEditor.this.frame, e.getClass().getSimpleName() + ": " + e.getMessage(), LC.tr("Error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        this.enterUserPwd.setEnabled(true);
        this.closeBtn = new JButton(new AbstractAction(LC.tr("Close"), new ImageIcon(Toolkit.getDefaultToolkit().getImage("resources/icons/close.png"))) {

            @Override
            public void actionPerformed(ActionEvent e) {
                XExternalEditor.this.close();
            }
        });
        this.closeBtn.setEnabled(true);
        btnPane.add(this.getUploadOnSaveButton());
        btnPane.add(this.uploadBtn);
        btnPane.add(this.previewBtn);
        btnPane.add(this.enterUserPwd);
        btnPane.add(this.closeBtn);
        final JLabel move = new JLabel(new ImageIcon(Toolkit.getDefaultToolkit().getImage("resources/icons/move.png")));
        move.addMouseMotionListener(this);
        move.addMouseListener(this);
        this.text.addMouseMotionListener(this);
        this.text.addMouseListener(this);
        msgPane.addMouseMotionListener(this);
        msgPane.addMouseListener(this);
        this.frame.addMouseListener(this);
        this.frame.addMouseMotionListener(this);
        this.frame.getContentPane().add(msgPane, BorderLayout.CENTER);
        this.frame.getContentPane().add(btnPane, BorderLayout.EAST);
        this.frame.pack();
        if (this.getGuiIniFile().exists()) {
            Properties props = new Properties();
            try {
                props.load(new FileInputStream(this.getGuiIniFile()));
                this.frame.setLocation(Integer.parseInt(props.getProperty("x")), Integer.parseInt(props.getProperty("y")));
            } catch (Exception e1) {
                this.frame.setLocation((int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - this.frame.getSize().getWidth()), 0);
            }
        } else {
            this.frame.setLocation((int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - this.frame.getSize().getWidth()), 0);
        }
        this.frame.setVisible(true);
    }

    /**
	 * @return
	 */
    private JToggleButton getUploadOnSaveButton() {
        if (this.uploadOnSaveButton == null) {
            this.uploadOnSaveButton = new JToggleButton(LC.tr("Upload On Save"), new ImageIcon(Toolkit.getDefaultToolkit().getImage("resources/icons/uploadonsave.png")));
            this.uploadOnSaveButton.addItemListener(new ItemListener() {

                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (XExternalEditor.this.getUploadOnSaveButton().isSelected()) {
                        XExternalEditor.this.uploadBtn.setEnabled(false);
                    } else {
                        XExternalEditor.this.uploadBtn.setEnabled(true);
                    }
                    XExternalEditor.this.iniFile.setProperty("uploadOnSave", Boolean.toString(XExternalEditor.this.getUploadOnSaveButton().isSelected()));
                    try {
                        XExternalEditor.this.saveSettings();
                    } catch (IOException e1) {
                        XExternalEditor._logger.error("settings not saved!", e1);
                    }
                }
            });
            this.uploadOnSaveButton.setSelected(this.iniFile.getProperty("uploadOnSave") != null ? Boolean.parseBoolean(this.iniFile.getProperty("uploadOnSave")) : false);
        }
        return this.uploadOnSaveButton;
    }

    /**
	 * @param file
	 * @param preview
	 * @throws IOException
	 * @throws NotSupportedException
	 */
    private void upload(File file, boolean preview) throws IOException, NotSupportedException {
        try {
            this.uploadBtn.setEnabled(false);
            this.previewBtn.setEnabled(false);
            URL upload_url = null;
            if (this.type.equalsIgnoreCase("edit file")) {
                upload_url = new URL(this.script + "?title=" + this.special + ":Upload");
            } else if (this.type.equalsIgnoreCase("edit text")) {
                upload_url = new URL(this.script + "?title=" + this.pagetitle + "&action=submit");
            } else {
                throw new NotSupportedException("type = " + this.type);
            }
            this.text.setText(LC.tr("Uploading the file"));
            this.bar.setVisible(true);
            this.bar.setIndeterminate(true);
            PostMethod filePost = new PostMethod(upload_url.toString());
            filePost.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
            FilePart filePart = new FilePart("wpUploadFile", file);
            StringPart wpPreview = null;
            if (preview) {
                wpPreview = new StringPart("wpPreview", "true");
            } else {
                wpPreview = new StringPart("wpSave", "true");
            }
            this.frame.requestFocusInWindow();
            String comment = JOptionPane.showInputDialog(this.frame, LC.tr("Comment for this file"));
            if (comment == null) {
                this.text.setText(LC.tr("Upload cancled by the user"));
                return;
            } else if (comment.trim().length() == 0) {
                comment = "uploaded with x-external-editor";
            }
            Part[] parts = null;
            if (this.type.equalsIgnoreCase("edit file")) {
                parts = new Part[] { new StringPart("wpUploadDescription", new String(comment.getBytes(this.responseCharSet)), "UTF-8"), new StringPart("wpUploadAffirm", "1"), new StringPart("wpUpload", "Upload file"), new StringPart("wpIgnoreWarning", "1"), new StringPart("wpDestFile", URLDecoder.decode(file.getName(), "UTF-8")), wpPreview, filePart };
            } else if (this.type.equalsIgnoreCase("edit text")) {
                String wikiText = this.readFile(file);
                if (wikiText.startsWith(new String(BOM))) {
                    wikiText = StringUtils.stripStart(wikiText, new String(BOM));
                }
                parts = new Part[] { new StringPart("wpTextbox1", wikiText, "UTF-8"), new StringPart("wpSummary", comment, "UTF-8"), new StringPart("wpEdittime", this.time), new StringPart("wpStarttime", this.starttime), new StringPart("wpEditToken", this.token), wpPreview };
            } else {
                throw new NotSupportedException("type = " + this.type);
            }
            filePost.setRequestEntity(new MultipartRequestEntity(parts, filePost.getParams()));
            int status = this.client.executeMethod(filePost);
            if (status == HttpStatus.SC_OK) {
                this.showHTMLFile(filePost.getResponseBodyAsString());
                if (this.type.equalsIgnoreCase("edit file")) {
                    this.text.setText(LC.tr("Finished uploading the file"));
                } else if (this.type.equalsIgnoreCase("edit text")) {
                    this.text.setText(LC.tr("Finished uploading the text"));
                } else {
                    throw new NotSupportedException("type = " + this.type);
                }
            } else if (status == HttpStatus.SC_MOVED_TEMPORARILY) {
                Header locationHeader = filePost.getResponseHeader("location");
                if (locationHeader != null) {
                    String redirectLocation = locationHeader.getValue();
                    GetMethod fileResult = new GetMethod(redirectLocation);
                    fileResult.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
                    status = this.client.executeMethod(fileResult);
                    if (status == HttpStatus.SC_OK) {
                        this.showHTMLFile(fileResult.getResponseBodyAsString());
                        this.text.setText(LC.tr("Finished uploading the file(redirect)"));
                    } else {
                        this.text.setText(LC.tr("Error") + " '" + status + "' " + LC.tr("from the server (redirect)"));
                    }
                } else {
                    this.text.setText(LC.tr("Error resolving the destination-host (redirect)"));
                }
            } else {
                this.text.setText(LC.tr("Error") + " '" + status + "' " + LC.tr("from the server"));
            }
        } finally {
            this.uploadBtn.setEnabled(!this.getUploadOnSaveButton().isSelected());
            this.previewBtn.setEnabled(this.type.equalsIgnoreCase("edit text"));
            this.bar.setVisible(false);
            this.bar.setIndeterminate(false);
        }
    }

    /**
	 * @param file
	 * @return
	 * @throws IOException
	 */
    private String readFile(File file) throws IOException {
        FileInputStream fin = new FileInputStream(file);
        BufferedReader fr = new BufferedReader(new InputStreamReader(fin, this.responseCharSet));
        StringBuilder sb = new StringBuilder();
        try {
            if (this.responseCharSet.equals("UTF-8")) {
                byte[] myBom = new byte[3];
                fin.read(myBom);
                if (myBom.equals(BOM)) {
                    throw new IllegalArgumentException("I am sorry! This should not happend. The file read has no BOM but it should have.");
                }
            }
            String line = null;
            while ((line = fr.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } finally {
            if (fr != null) {
                fr.close();
            }
        }
        return sb.toString();
    }

    /**
	 * @param content
	 * @throws IOException
	 */
    private void showHTMLFile(String content) throws IOException {
        File tmpFile = File.createTempFile("preview", ".html");
        tmpFile.deleteOnExit();
        BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), this.responseCharSet));
        String $preview = content;
        URL url = new URL(this.script);
        String server = url.getProtocol() + "://" + url.getHost();
        String path = url.getPath().replace("index.php", "");
        $preview = $preview.replaceAll("<head>", "<head><base href=\"" + server + "" + path + "\" />");
        fw.append($preview);
        fw.close();
        try {
            Desktop.getDesktop().open(tmpFile);
        } catch (IOException e) {
            this.text.setText(LC.tr("Cannot open the HTML file"));
            XExternalEditor._logger.error("", e);
            JOptionPane.showMessageDialog(this.frame, e.getMessage(), LC.tr("Error"), JOptionPane.ERROR_MESSAGE);
        }
    }
}
