package wsl.mdn.licence;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import javax.help.HelpSetException;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;
import wsl.fw.datasource.RemoteDataManagerServant;
import wsl.fw.gui.GuiConst;
import wsl.fw.gui.WslButton;
import wsl.fw.help.HelpId;
import wsl.fw.help.HelpListener;
import wsl.fw.help.HelpManager;
import wsl.fw.remote.RmiServer;
import wsl.fw.remote.TerminatorServant;
import wsl.fw.resource.ResId;
import wsl.fw.resource.ResourceManager;
import wsl.fw.util.Config;
import wsl.fw.util.Log;
import wsl.fw.util.Util;
import wsl.licence.ActivationKey;
import wsl.licence.LicenceKey;
import wsl.licence.RegisterAppPanel;
import wsl.mdn.admin.MdnAdminHelpManager;
import wsl.mdn.common.MdnAdminConst;
import wsl.mdn.common.MdnResourceManager;
import wsl.mdn.dataview.ResultWrapper;
import wsl.mdn.server.LicenseManager;
import wsl.mdn.server.MdnServer;
import wsl.mdn.server.RemoteLicenseManagerServant;

/**
 *
 */
public class MdnLicenceManager {

    private static MdnStore _store = null;

    public static final ResId BUTTON_HELP = new ResId("OkPanel.button.Help");

    public static final ResId TEXT_ACTIVATION_TITLE = new ResId("MdnLicenceManager.activation.title");

    public static final ResId TEXT_EULA_ACCEPT = new ResId("MdnLicenceManager.eula.accept");

    public static final ResId TEXT_EULA_DECLINE = new ResId("MdnLicenceManager.eula.decline");

    public static final ResId TEXT_EULA_TITLE = new ResId("MdnLicenceManager.eula.title");

    public static final ResId ERROR_EULA_FILE = new ResId("MdnLicenceManager.error.eulaFile");

    public static final HelpId HID_LICENCE = new HelpId("mdn.licence.MdnLicenceManager");

    public static final String EULA_FILE = "EULA.txt";

    /**
     * Product code constants
     */
    public static int PROD_CODE_MDN_SERVER = ((0x01 << 16) | (0x01 << 8) | (0x00));

    /**
     * Display the MDN licence screen. ResourceManager and config must be set
     * before calling.
     * @return false on a fatal error.
     */
    public static boolean showLicenceManager() {
        LicenceKey lKey = null;
        ActivationKey aKey = null;
        try {
            lKey = getLicenceKey(PROD_CODE_MDN_SERVER);
            if (lKey == null) lKey = makeLicenceKey(PROD_CODE_MDN_SERVER); else if (!isValidLicenceKey(lKey)) {
                JOptionPane.showMessageDialog((Component) null, "Fatal Error: Invalid registration key");
                return false;
            }
            aKey = getActivationKey(lKey);
            showActivationPanel(lKey, aKey);
            return true;
        } catch (Exception e) {
            Log.error("Error:", e);
            return false;
        }
    }

    /**
     * Main entrypoint.
     */
    public static void main(String[] args) {
        ResourceManager.set(new MdnResourceManager());
        Log.log(MdnServer.TEXT_STARTING.getText() + " " + MdnServer.TEXT_VERSION.getText() + " " + MdnServer.VERSION_NUMBER);
        Config.setSingleton(MdnAdminConst.MDN_CONFIG_FILE, true);
        String registrationEmail = null;
        if (args.length == 1) {
            registrationEmail = args[0].replaceAll("\"", "");
            consoleRegistration(registrationEmail, null);
            System.exit(0);
        } else if (args.length == 2) {
            registrationEmail = args[0].replaceAll("\"", "");
            String reinstallRef = args[1];
            consoleRegistration(registrationEmail, reinstallRef);
            System.exit(0);
        }
        if (showLicenceManager()) System.exit(0); else System.exit(1);
    }

    public static void consoleRegistration(String emailAddress, String reinstallRef) {
        try {
            LicenceKey licenceKey = getLicenceKey(PROD_CODE_MDN_SERVER);
            if (licenceKey == null) {
                licenceKey = makeLicenceKey(PROD_CODE_MDN_SERVER);
            } else if (!isValidLicenceKey(licenceKey)) {
                System.err.println("Invalid registration key");
                System.exit(1);
            }
            ActivationKey activationKey = getActivationKey(licenceKey);
            ResultWrapper result = LicenseRemoteCallManager.validateUser(emailAddress, licenceKey.toString(), reinstallRef);
            if (result == null || result.getObject() == null) {
                System.out.println("There was a problem activating your account. Please make sure you have registered at http://www.mobiledatanow.com");
                System.out.println("Error: " + (result != null ? result.getErrorMsg() : ""));
                System.exit(1);
            }
            activationKey = (ActivationKey) result.getObject();
            MdnStore store = getStore();
            store.setActivationKey(licenceKey, activationKey);
            store.setRegisteredEmailAddress(result.getRegisteredEmailAddress());
            store.setPublicGroup(result.getPublicGroupBoolean());
            store.setAvailablePublicMessages(result.getAvailablePublicMessages());
            store.setInstallationReferenceNumber(result.getInstallationReferenceNumber());
            store.store();
        } catch (Exception e) {
            Log.error("Error: ", e);
        }
        System.out.println("Your account has been sucessfully activated");
    }

    /**
     * @return the MdnStore singleton.
     */
    public static MdnStore getStore() throws Exception {
        if (_store == null) {
            _store = new MdnStore();
            _store.load();
        }
        return _store;
    }

    /**
     * Show the EULA.
     * @return true if the EULA was accepted.
     */
    public static boolean showEula() {
        try {
            URL url = ClassLoader.getSystemResource(EULA_FILE);
            if (url == null) {
                Log.error(ERROR_EULA_FILE.getText() + EULA_FILE);
                return false;
            }
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            StringBuffer eulaText = new StringBuffer(1000);
            int ch;
            while ((ch = isr.read()) != -1) eulaText.append((char) ch);
            isr.close();
            is.close();
            String optionButtons[] = { TEXT_EULA_ACCEPT.getText(), TEXT_EULA_DECLINE.getText() };
            JTextArea textArea = new JTextArea(eulaText.toString());
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setBorder(new BevelBorder(BevelBorder.LOWERED));
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            scrollPane.setPreferredSize(new Dimension(600, 400));
            int rv = JOptionPane.showOptionDialog(null, scrollPane, TEXT_EULA_TITLE.getText(), JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, optionButtons, optionButtons[0]);
            return (rv == 0);
        } catch (Exception e) {
            Log.error("MdnLicenceManager.showEula: ", e);
            return false;
        }
    }

    /**
     * show the licence panel
     */
    public static void showActivationPanel(LicenceKey lKey, ActivationKey aKey) {
        Frame frame = new Frame();
        frame.setVisible(false);
        JDialog dlg = new JDialog(frame, TEXT_ACTIVATION_TITLE.getText());
        HelpListener listener = new HelpListener(HID_LICENCE, dlg);
        WslButton helpButton = new WslButton(BUTTON_HELP.getText(), listener);
        helpButton.setIcon(Util.resourceIcon(GuiConst.FW_IMAGE_PATH + "help.gif"));
        RegisterAppPanel panel = new RegisterAppPanel(dlg, lKey, aKey);
        dlg.getContentPane().setLayout(new BorderLayout());
        dlg.getContentPane().add(panel, BorderLayout.CENTER);
        dlg.pack();
        dlg.setModal(true);
        dlg.setVisible(true);
        aKey = panel.getActivationKey();
        String registeredEmailAddress = panel.getRegisterdEmailAddress();
        Boolean publicGroup = panel.getPublicGroup();
        int availableMessages = panel.getAvailablePublicMessages();
        if (aKey != null) {
            try {
                MdnStore store = getStore();
                store.setActivationKey(lKey, aKey);
                store.setRegisteredEmailAddress(registeredEmailAddress);
                store.setPublicGroup(publicGroup);
                store.setAvailablePublicMessages(availableMessages);
                store.setInstallationReferenceNumber(panel.getInstallationReferenceNumber());
                store.store();
            } catch (Exception e) {
                System.out.println("Error activating application.");
                System.out.println(e.toString());
            }
        }
    }

    /**
     *
     */
    public static ActivationKey getActivationKey(LicenceKey lKey) throws Exception {
        MdnStore store = getStore();
        if (store != null) return store.getActivationKey(lKey);
        return null;
    }

    /**
     * Returns true if the node locked licence key is not correct for the current node.
     */
    public static boolean isValidLicenceKey(LicenceKey lKey) throws Exception {
        LicenceKey lTmp = makeLicenceKey(lKey.getProductCode());
        return lTmp.equals(lKey);
    }

    /**
     *
     */
    public static LicenceKey getLicenceKey(int productID) throws Exception {
        MdnStore store = getStore();
        if (store != null) return store.getLicenceKey(productID);
        return null;
    }

    public static String getRegisterdEmailAddress() throws Exception {
        MdnStore store = getStore();
        if (store != null) return store.getRegisteredEmailAddress();
        return null;
    }

    public static Boolean getPublicGroup() throws Exception {
        MdnStore store = getStore();
        if (store != null) return store.getPublicGroup();
        return null;
    }

    public static int getAvailablePublicMessages() throws Exception {
        MdnStore store = getStore();
        if (store != null) return store.getAvailablePublicMessages();
        return 0;
    }

    public static int getInstallationReferenceNumber() throws Exception {
        MdnStore store = getStore();
        if (store != null) return store.getInstallationReferenceNumber();
        return 0;
    }

    /**
     * Create a non-nodelocked LicenceKey
     */
    public static LicenceKey makeLicenceKey(int productID) throws Exception {
        MdnStore store = getStore();
        String path = store.getPath();
        InetAddress addr = InetAddress.getLocalHost();
        String host = addr.getHostName();
        return makeLicenceKey(productID, path, host);
    }

    /**
     * Create a nodelocked LicenceKey, using the productID and the two String arguments.
     */
    public static LicenceKey makeLicenceKey(int productID, String str1, String str2) {
        int hash = (str1 + str2).hashCode();
        return new LicenceKey(productID, hash);
    }
}
