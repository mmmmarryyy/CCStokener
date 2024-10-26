package com.quikj.application.web.talk.client;

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import com.quikj.ace.messages.vo.talk.CallPartyElement;
import com.quikj.ace.messages.vo.talk.CallingNameElement;
import com.quikj.ace.messages.vo.talk.ChangePasswordRequestMessage;
import com.quikj.ace.messages.vo.talk.DndRequestMessage;
import com.quikj.ace.messages.vo.talk.GroupList;
import com.quikj.ace.messages.vo.talk.GroupMemberElement;
import com.quikj.ace.messages.vo.talk.JoinRequestMessage;
import com.quikj.ace.messages.vo.talk.MediaElements;
import com.quikj.ace.messages.vo.talk.RegistrationRequestMessage;
import com.quikj.client.beans.ChangePasswordDialog;
import com.quikj.client.beans.ImageButton;
import com.quikj.client.beans.InformationDialog;
import com.quikj.client.beans.LoginDialog;
import com.quikj.client.beans.TextFieldDialog;
import com.quikj.client.framework.HTTPCommunicationErrorInterface;
import com.quikj.client.framework.HTTPMessageListenerInterface;
import com.quikj.client.framework.HTTPRspMessage;
import com.quikj.client.framework.ServerCommunications;
import com.quikj.client.framework.URLUtils;

/**
 * 
 * @author amit
 */
public class TalkFrame extends java.awt.Frame implements HTTPCommunicationErrorInterface {

    class ApplicationsMenuListener implements ActionListener {

        private String className;

        public ApplicationsMenuListener(String class_name) {
            className = class_name;
        }

        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            NetworkApplicationInterface n = (NetworkApplicationInterface) applicationList.get(className);
            n.menuItemClicked();
        }
    }

    class ChangePasswordResponseListener implements HTTPMessageListenerInterface {

        public void messageReceived(int req_id, int status, String content_type, int http_status, String reason, String message) {
            synchronized (messageEventLock) {
                if (status == HTTPMessageListenerInterface.TIMEOUT) {
                    disconnect();
                    doLogin(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Time-out_receiving_response_from_server"));
                } else if (status == HTTPMessageListenerInterface.RECEIVED) {
                    if (http_status == HTTPRspMessage.OK) {
                    } else {
                        if (reason == null) {
                            reason = ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("No_reason_given");
                        }
                        new InformationDialog(TalkFrame.this, ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Change_password"), ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Change_password_failed_") + ' ' + reason, ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("_Close_"), true);
                    }
                } else {
                    disconnect();
                    doLogin(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Unknown_status_") + ' ' + status + ' ' + ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("received_from_the_server"));
                }
            }
        }
    }

    class DndResponseListener implements HTTPMessageListenerInterface {

        public void messageReceived(int req_id, int status, String content_type, int http_status, String reason, String message) {
            synchronized (messageEventLock) {
                if (status == HTTPMessageListenerInterface.TIMEOUT) {
                    disconnect();
                    doLogin(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Time-out_receiving_response_from_server"));
                } else if (status == HTTPMessageListenerInterface.RECEIVED) {
                    if (http_status == HTTPRspMessage.OK) {
                        if (allowInboundCalls == true) {
                            allowInboundCalls = false;
                        } else {
                            allowInboundCalls = true;
                        }
                        setOptionsMenu();
                    } else {
                        if (reason == null) {
                            reason = ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("No_reason_given");
                        }
                        new InformationDialog(TalkFrame.this, ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Error"), ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("failed_to_set_DND") + ' ' + reason, ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("_Close_"), true);
                    }
                } else {
                    disconnect();
                    doLogin(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Unknown_status_") + ' ' + status + ' ' + ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("received_from_the_server"));
                }
            }
        }
    }

    class JoinResponseListener implements HTTPMessageListenerInterface {

        public void messageReceived(int req_id, int status, String content_type, int http_status, String reason, String message) {
            synchronized (messageEventLock) {
                if (status == HTTPMessageListenerInterface.TIMEOUT) {
                    disconnect();
                    doLogin(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Time-out_receiving_response_from_server"));
                } else if (status == HTTPMessageListenerInterface.RECEIVED) {
                    if (http_status == HTTPRspMessage.OK) {
                    } else {
                        if (reason == null) {
                            reason = ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("No_reason_given");
                        }
                        new InformationDialog(TalkFrame.this, ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Join"), ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Join_failed_") + ' ' + reason, ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("_Close_"), true);
                    }
                } else {
                    disconnect();
                    doLogin(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Unknown_status_") + ' ' + status + ' ' + ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("received_from_the_server"));
                }
            }
        }
    }

    class LoginDialogThread extends Thread {

        private String message;

        public LoginDialogThread(String message) {
            this.message = message;
        }

        public void run() {
            performLogin(message);
        }
    }

    class LoginResponseListener implements HTTPMessageListenerInterface {

        public void messageReceived(int req_id, int status, String content_type, int http_status, String reason, String message) {
            synchronized (messageEventLock) {
                if (status == HTTPMessageListenerInterface.TIMEOUT) {
                    disconnect();
                    doLogin(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Time-out_receiving_response_from_server"));
                } else if (status == HTTPMessageListenerInterface.RECEIVED) {
                    if (http_status == HTTPRspMessage.OK) {
                    } else {
                        disconnect();
                        if (reason != null) {
                            doLogin(reason + " (" + http_status + ")");
                        } else {
                            doLogin(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Server_returned_error_") + ' ' + http_status);
                        }
                    }
                } else {
                    disconnect();
                    doLogin(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Unknown_status_") + ' ' + status + ' ' + ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("received_from_the_server"));
                }
            }
        }
    }

    class RequestListener implements HTTPMessageListenerInterface {

        public void messageReceived(int req_id, int status, String content_type, int http_status, String reason, String message) {
        }
    }

    private java.awt.MenuItem aboutMenuItem;

    private java.awt.MenuItem allowIncomingMenu;

    private java.awt.MenuItem audioControlMenu;

    private java.awt.MenuItem autoAnswerMenu;

    private java.awt.Label captionLabel;

    private java.awt.MenuItem changePasswordMenuItem;

    private java.awt.MenuItem copyMenuItem;

    private java.awt.Menu editMenu;

    private java.awt.MenuItem emailMenuItem;

    private java.awt.MenuItem exitMenuItem;

    private java.awt.Menu fileMenu;

    private java.awt.List groupMembers;

    private java.awt.Menu helpMenu;

    private java.awt.MenuItem joinMenu;

    private java.awt.Label label1;

    private java.awt.Label label2;

    private java.awt.ScrollPane logoPane;

    private java.awt.MenuItem logoutMenuItem;

    private java.awt.MenuBar menuBar1;

    private java.awt.Menu operationsMenu;

    private java.awt.Menu optionMenu;

    private java.awt.Panel panel1;

    private java.awt.MenuItem pasteMenuItem;

    private java.awt.List sessionList;

    private java.awt.TextField statusBar;

    private java.awt.MenuItem talktoMenuItem;

    private String title;

    private String host;

    private int port;

    private int applicationId;

    private Applet applet;

    private boolean unregistered = false;

    private String unregisteredCalled;

    private ServerCommunications com = null;

    private boolean allowInboundCalls = true;

    private boolean autoAnswer = false;

    private Vector groupList = new Vector();

    private AudioClip ringTone = null;

    private AudioClip buzzTone = null;

    private AudioClip chimeTone = null;

    private AudioClip loginTone = null;

    private AudioClip logoutTone = null;

    private static TalkFrame instance = null;

    private static final String ALLOW = "Cancel_Do_Not_Disturb";

    private static final String DISALLOW = "Set_Do_Not_Disturb";

    private static final String AUTO_ANSWER = "Set_Auto_Answer";

    private static final String DONT_AUTO_ANSWER = "Cancel_Auto_Answer";

    private static final long RETRY_INTERVAL = 20 * 1000L;

    private static final int RETRY_COUNT = 3;

    private static final long MIN_CONNECTION_CLOSE_INERVAL = 30 * 1000L;

    private static int missedCalls = 0;

    private Object missedCallsLock = new Object();

    private Object messageEventLock = new Object();

    private String language = "English";

    private Locale locale = new Locale("en", "US");

    private CallPartyElement userInformation = new CallPartyElement();

    private Hashtable callList = new Hashtable();

    private Vector sessionListVector = new Vector();

    private Object sessionLock = new Object();

    private Vector applicationMediaElements = new Vector();

    private boolean emailEnabled = true;

    private Vector frameList = new Vector();

    private String caption = null;

    private Image logo = null;

    private AutoEmailTranscriptInfo autoEmailInfo;

    public boolean endSession = false;

    /** Holds value of property displaySessionInfo. */
    private boolean displaySessionInfo = true;

    /** Holds value of property bringToFront. */
    private boolean bringToFront = false;

    private Hashtable applicationList = new Hashtable();

    /** Holds value of property cannedMessageList. */
    private Vector cannedMessageList;

    /** Holds value of property timeAdjustment. */
    private long timeAdjustment = 0L;

    private boolean allowSendBuzz = true;

    private boolean allowSendWebPage = true;

    private boolean trace = false;

    private Date[] closedConnectionTs = new Date[3];

    /** Creates new form NewTalkFrame */
    public TalkFrame(String title, String host, int port, int appl_id, Applet applet, boolean unregistered, String unregistered_called, CallingNameElement element, String caption, Image logo) throws TalkClientException {
        super();
        String parm = applet.getParameter("trace");
        if (parm != null) {
            if (parm.equals("true")) {
                trace = true;
            } else if (parm.equals("false")) {
                trace = false;
            }
        }
        this.title = title;
        this.host = host;
        this.port = port;
        applicationId = appl_id;
        this.applet = applet;
        this.unregistered = unregistered;
        unregisteredCalled = unregistered_called;
        this.caption = caption;
        this.logo = logo;
        setLanguage();
        initApplicationMediaElements();
        autoEmailInfo = new AutoEmailTranscriptInfo(applet);
        if (isRegistered() == true) {
            initComponents();
            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
            setSize(dim.width < 600 ? dim.width : 600, dim.height < 600 ? dim.height : 600);
            Rectangle bounds = getBounds();
            Point mid = new Point(0 + (dim.width / 2), 0 + (dim.height / 2));
            int x = mid.x - (bounds.width / 2);
            int y = mid.y - (bounds.height / 2);
            if (x < 0) x = 0;
            if (y < 0) y = 0;
            setBounds(x, y, bounds.width, bounds.height);
            if (caption != null) {
                captionLabel.setText(caption);
            }
            if (logo != null) {
                ImageButton i_button = new ImageButton();
                i_button.setImage(logo);
                logoPane.add(i_button);
                i_button.invalidate();
            }
            String email_enabled = applet.getParameter("email");
            if (email_enabled != null) {
                if (email_enabled.equals("no") == true) {
                    setEmailEnabled(false);
                    emailMenuItem.setEnabled(false);
                } else {
                    setEmailEnabled(true);
                    emailMenuItem.setEnabled(true);
                }
            }
            setOptionsMenu();
        }
        String display_session_info = applet.getParameter("display-session-info");
        if (display_session_info != null) {
            if (display_session_info.equals("no") == true) {
                setDisplaySessionInfo(false);
            } else if (display_session_info.equals("yes") == true) {
                setDisplaySessionInfo(true);
            }
        }
        String bring_to_front = applet.getParameter("bring-to-front");
        if (bring_to_front != null) {
            if (bring_to_front.equals("no") == true) {
                setBringToFront(false);
            } else if (bring_to_front.equals("yes") == true) {
                setBringToFront(true);
            }
        }
        String allow_buzz = applet.getParameter("allow-send-buzz");
        if (allow_buzz != null) {
            if (allow_buzz.equals("no") == true) {
                setAllowSendBuzz(false);
            } else if (allow_buzz.equals("yes") == true) {
                setAllowSendBuzz(true);
            }
        }
        String allow_webpage = applet.getParameter("allow-send-webpage");
        if (allow_webpage != null) {
            if (allow_webpage.equals("no") == true) {
                setAllowSendWebPage(false);
            } else if (allow_webpage.equals("yes") == true) {
                setAllowSendWebPage(true);
            }
        }
        userInformation.setLanguage(language);
        instance = this;
        initUserToUserElements();
        if (isRegistered() == false) {
            if (element != null) {
                userInformation = element.getCallParty();
                userInformation.setLanguage(language);
            } else {
                userInformation.setName("unregistered");
                long date = (new Date()).getTime() & 0xFFFF;
                userInformation.setFullName(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Visitor") + " " + date);
            }
            userInformation.setEnvironment(getEnvironmentInfo());
            downloadClips();
            if (connect(null) == true) {
                com.setRequestListener(new RequestListener());
                TalkSessionInterface session = TalkSessionFactory.getInstance().createSession();
                if (session.makeCall(unregistered_called) == false) {
                    throw new TalkClientException(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Could_not_make_call"));
                }
            } else {
                throw new TalkClientException(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Could_not_connect_to_the_server"));
            }
        } else {
            show();
            doLogin();
        }
    }

    public static Locale getLocale(String language) {
        Locale locale = new Locale("en", "US");
        if (language != null) {
            if (language.equals("local") == true) {
                locale = Locale.getDefault();
            } else if (language.equals("French") == true) {
                locale = new Locale("fr", "FR");
            } else if (language.equals("German") == true) {
                locale = new Locale("de", "DE");
            } else if (language.equals("Spanish") == true) {
                locale = new Locale("es", "ES");
            } else if (language.equals("Dutch") == true) {
                locale = new Locale("nl", "BE");
            } else if (language.equals("Italian") == true) {
                locale = new Locale("it", "IT");
            } else if (language.equals("Croatian") == true) {
                locale = new Locale("hr", "HR");
            }
        }
        return locale;
    }

    public static TalkFrame Instance() {
        return instance;
    }

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        new AboutAceOperatorFrame(this, locale, applet);
    }

    public void addToFrameList(Frame frame) {
        frameList.addElement(frame);
    }

    private void allowIncomingMenuActionPerformed(java.awt.event.ActionEvent evt) {
        DndRequestMessage message = new DndRequestMessage();
        message.setEnable(allowInboundCalls);
    }

    private void audioControlMenuActionPerformed(java.awt.event.ActionEvent evt) {
        new AudioControlDialog(this, locale);
    }

    private void autoAnswerMenuActionPerformed(java.awt.event.ActionEvent evt) {
        if (autoAnswer == true) {
            autoAnswer = false;
        } else {
            autoAnswer = true;
        }
        setOptionsMenu();
    }

    private void changePasswordMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        ChangePasswordDialog cpd = new ChangePasswordDialog(this, locale);
        if (cpd.isChangeSelected() == true) {
            String error = ChangePasswordDialog.validPasswordSyntax(cpd.getNewPassword(), locale);
            if (error != null) {
                new InformationDialog(this, ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Password_selection_error"), error, ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("_Close_"), true);
                return;
            }
            ChangePasswordRequestMessage message = new ChangePasswordRequestMessage();
            message.setNewPassword(cpd.getNewPassword());
            message.setOldPassword(cpd.getOldPassword());
            message.setUserName(userInformation.getUserName());
        }
    }

    private boolean connect(String sessionId) {
        if (com != null) {
            disconnect();
        }
        try {
            com = new ServerCommunications(host, port, applicationId, sessionId, trace);
        } catch (UnknownHostException ex) {
            return false;
        }
        if (isRegistered() == true) {
            String message = ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Connecting_to_") + " " + host + ':' + port + " .....";
            if (isRegistered() == true) {
                statusBar.setText(message);
            }
        }
        if (com.connect() == false) {
            if (isRegistered() == true) {
                String message = ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Connection_to_server_") + " " + host + ':' + port + " " + ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("failed");
                statusBar.setText(message);
            }
            return false;
        }
        if (isRegistered() == true) {
            String message = ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Connected_to_") + " " + host + ':' + port;
            statusBar.setText(message);
        }
        com.setClosedListener(this);
        return true;
    }

    public void communicationError(String host, int port, int appl_id, int cause) {
        String message = null;
        InformationDialog info = null;
        if (endSession) {
            return;
        }
        disconnect();
        message = ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Lost_connection_with_the_server_all_calls_will_be_dropped");
        info = new InformationDialog(TalkFrame.this, ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Server_connection_lost"), message, ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("_Close_"), true);
        info.setVisible(true);
        Toolkit.getDefaultToolkit().beep();
        Enumeration elements = callList.elements();
        while (elements.hasMoreElements() == true) {
            TalkSessionInterface session = (TalkSessionInterface) elements.nextElement();
            session.dispose();
        }
        callList.clear();
        if (isRegistered() == true) {
            groupMembers.removeAll();
        }
        groupList.removeAllElements();
        if (unregistered == false) {
            doLogin();
        } else {
            dispose();
        }
    }

    private void disconnect() {
        if (com != null) {
            if (com.isConnected() == true) {
                com.disconnect();
            }
        }
        if (isRegistered() == true) {
            statusBar.setText(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Disconnected"));
        }
    }

    public void dispose() {
        disconnect();
        for (int i = 0; i < frameList.size(); i++) {
            Frame frame = (Frame) frameList.elementAt(i);
            frame.dispose();
        }
        frameList.removeAllElements();
        Enumeration elements = callList.elements();
        while (elements.hasMoreElements() == true) {
            TalkSessionInterface session = (TalkSessionInterface) elements.nextElement();
            session.dispose();
        }
        callList.clear();
        if (isRegistered() == true) {
            groupMembers.removeAll();
        }
        groupList.removeAllElements();
        if (isRegistered() == true) {
            super.dispose();
        }
        instance = null;
    }

    private void doLogin() {
        doLogin("");
    }

    private void doLogin(String message) {
        LoginDialogThread t = new TalkFrame.LoginDialogThread(message);
        t.start();
    }

    public MediaElements downloadCannedMessage(int index) {
        if (cannedMessageList == null) {
            return null;
        }
        CannedElement element = (CannedElement) cannedMessageList.elementAt(index);
        if (element.getMessage() != null) {
            return element.getMessage();
        }
        String url_string = applet.getParameter("canned-message");
        if (url_string == null) {
            return null;
        }
        URL url = URLUtils.formatURL(applet, url_string);
        StringBuffer buffer = new StringBuffer(url.toString() + "?action=query");
        buffer.append("&group=" + URLEncoder.encode(element.getGroup()));
        buffer.append("&id=" + URLEncoder.encode(element.getId()));
        try {
            url = new URL(buffer.toString());
            URLConnection c = url.openConnection();
            c.setUseCaches(false);
            StringBuffer output = new StringBuffer();
            byte[] array = new byte[1000];
            InputStream is = c.getInputStream();
            int count = 0;
            while ((count = is.read(array)) > 0) {
                output.append(new String(array, 0, count));
            }
            CannedMessageParser cp = new CannedMessageParser();
            return cp.parseQueryMessage(output.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private void downloadCannedMessageList(GroupList list) {
        if (list == null) {
            return;
        }
        int num_groups = list.numElements();
        if (num_groups == 0) {
            return;
        }
        String url_string = applet.getParameter("canned-message");
        if (url_string == null) {
            return;
        }
        statusBar.setText(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Downloading_pre-defined_messages") + "...");
        URL url = URLUtils.formatURL(applet, url_string);
        StringBuffer buffer = new StringBuffer(url.toString() + "?action=list&groups=all");
        for (int i = 0; i < num_groups; i++) {
            buffer.append(URLEncoder.encode("," + list.getElementAt(i)));
        }
        try {
            url = new URL(buffer.toString());
            URLConnection c = url.openConnection();
            c.setUseCaches(false);
            StringBuffer output = new StringBuffer();
            byte[] array = new byte[1000];
            InputStream is = c.getInputStream();
            int count = 0;
            while ((count = is.read(array)) > 0) {
                output.append(new String(array, 0, count));
            }
            CannedMessageParser cp = new CannedMessageParser();
            cannedMessageList = cp.parseListMessage(output.toString());
        } catch (Exception ex) {
            return;
        }
    }

    private void downloadClips() {
        String message = ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Downloading_audio_clips_dot_dot_dot");
        if (isRegistered() == true) {
            statusBar.setText(message);
        }
        String ring_url = applet.getParameter("ring");
        if (ring_url != null) {
            ringTone = getAudioClip(ring_url);
        }
        String buzz_url = applet.getParameter("buzz");
        if (buzz_url != null) {
            buzzTone = getAudioClip(buzz_url);
        }
        String chime_url = applet.getParameter("chime");
        if (chime_url != null) {
            chimeTone = getAudioClip(chime_url);
        }
        String logout_url = applet.getParameter("logout");
        if (logout_url != null) {
            logoutTone = getAudioClip(logout_url);
        }
        String login_url = applet.getParameter("login");
        if (login_url != null) {
            loginTone = getAudioClip(login_url);
        }
        message = ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Done_downloading_audio_clips");
        if (isRegistered() == true) {
            statusBar.setText(message);
        }
    }

    private void emailMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        Vector addresses = new Vector();
        EmailFrame email = new EmailFrame();
        email.setFrom(userInformation.getEmail());
        if (userInformation.getEmail() != null) {
            if (addresses.contains(userInformation.getEmail()) == false) {
                addresses.addElement(userInformation.getEmail());
            }
        }
        Enumeration elements = callList.elements();
        while (elements.hasMoreElements() == true) {
            TalkSessionInterface session = (TalkSessionInterface) elements.nextElement();
            CallInfo cinfo = session.getCallInfo();
            Vector v = cinfo.getOtherParties();
            for (int i = 0; i < v.size(); i++) {
                CallPartyElement element = (CallPartyElement) v.elementAt(i);
                if (element.getEmail() != null) {
                    if (addresses.contains(element.getEmail()) == false) {
                        addresses.addElement(element.getEmail());
                    }
                }
            }
        }
        if (addresses.size() > 0) {
            String[] abook = new String[addresses.size()];
            for (int i = 0; i < abook.length; i++) {
                abook[i] = (String) addresses.elementAt(i);
            }
            email.setAddressBook(abook);
        }
    }

    /** Exit the Application */
    private void exitForm(java.awt.event.WindowEvent evt) {
        AudioControlDialog.reset();
        resetOptions();
        dispose();
    }

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        closedConnectionTs[0] = closedConnectionTs[1] = closedConnectionTs[2] = null;
        AudioControlDialog.reset();
        resetOptions();
        dispose();
    }

    private TalkSessionInterface findCallInfo(long session) {
        return (TalkSessionInterface) callList.get(new Long(session));
    }

    public Applet getApplet() {
        return applet;
    }

    /**
	 * Getter for property applicationMediaElements.
	 * 
	 * @return Value of property applicationMediaElements.
	 * 
	 */
    public java.util.Vector getApplicationMediaElements() {
        return applicationMediaElements;
    }

    private AudioClip getAudioClip(String url_string) {
        URL url = URLUtils.formatURL(applet, url_string);
        if (url == null) return null;
        return applet.getAudioClip(url);
    }

    /**
	 * Getter for property autoEmailInfo.
	 * 
	 * @return Value of property autoEmailInfo.
	 * 
	 */
    public com.quikj.application.web.talk.client.AutoEmailTranscriptInfo getAutoEmailInfo() {
        return autoEmailInfo;
    }

    public AudioClip getBuzzTone() {
        return buzzTone;
    }

    private String getCallDescriptionText(TalkSessionInterface session) {
        CallInfo cinfo = session.getCallInfo();
        StringBuffer strbuf = new StringBuffer();
        if (cinfo.isCallingParty() == true) {
            strbuf.append(" <<<<  ");
        } else {
            strbuf.append(" >>>>  ");
        }
        Vector v = cinfo.getOtherParties();
        int size = v.size();
        if (size == 0) {
            strbuf.append(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Unknown"));
        } else if (size == 1) {
            CallPartyElement element = (CallPartyElement) v.elementAt(0);
            strbuf.append(element.getUserName());
            String full_name = element.getFullName();
            if (full_name != null) {
                strbuf.append(" (" + full_name + ") ");
            }
        } else {
            strbuf.append(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Conference__") + " " + (size + 1) + "  " + ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("parties"));
        }
        if (cinfo.isConnected() == true) {
            strbuf.append("  -   " + ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("connected") + " ");
        } else {
            strbuf.append("  -    " + ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("not_connected"));
        }
        if (cinfo.isCallingParty() == true) {
            strbuf.append(" >>>>");
        } else {
            strbuf.append(" <<<<");
        }
        return strbuf.toString();
    }

    /**
	 * Getter for property cannedMessageList.
	 * 
	 * @return Value of property cannedMessageList.
	 * 
	 */
    public Vector getCannedMessageList() {
        return this.cannedMessageList;
    }

    /**
	 * Getter for property caption.
	 * 
	 * @return Value of property caption.
	 * 
	 */
    public java.lang.String getCaption() {
        return caption;
    }

    public AudioClip getChimeTone() {
        return chimeTone;
    }

    public ServerCommunications getCom() {
        return com;
    }

    private String getEnvironmentInfo() {
        StringBuffer buffer = new StringBuffer("(");
        buffer.append("java.class.version=");
        buffer.append(System.getProperty("java.class.version"));
        buffer.append("; java.vendor=");
        buffer.append(System.getProperty("java.vendor"));
        buffer.append("; java.version=");
        buffer.append(System.getProperty("java.version"));
        buffer.append("; os.arch=");
        buffer.append(System.getProperty("os.arch"));
        buffer.append("; os.name=");
        buffer.append(System.getProperty("os.name"));
        buffer.append(")");
        return buffer.toString();
    }

    public Locale getLocale() {
        return locale;
    }

    /**
	 * Getter for property logo.
	 * 
	 * @return Value of property logo.
	 * 
	 */
    public java.awt.Image getLogo() {
        return logo;
    }

    public AudioClip getRingTone() {
        return ringTone;
    }

    public String getSelectedMember() {
        int selected_index = groupMembers.getSelectedIndex();
        if (selected_index < 0) {
            return null;
        }
        return ((ListGroupElement) groupList.elementAt(selected_index)).getName();
    }

    /**
	 * Getter for property timeAdjustment.
	 * 
	 * @return Value of property timeAdjustment.
	 * 
	 */
    public long getTimeAdjustment() {
        return timeAdjustment;
    }

    public CallPartyElement getUserInformation() {
        return userInformation;
    }

    public void incrementMissedCallCount() {
        synchronized (missedCallsLock) {
            statusBar.setText(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("You_missed_") + ' ' + (++missedCalls) + ' ' + ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("calls"));
        }
    }

    private void initApplicationMediaElements() {
        String plugins = applet.getParameter("media");
        if (plugins != null) {
            StringTokenizer tokens = new StringTokenizer(plugins, ":");
            int num_tokens = tokens.countTokens();
            for (int i = 0; i < num_tokens; i++) {
                String class_name = tokens.nextToken();
                applicationMediaElements.addElement(class_name);
            }
        }
    }

    /**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        panel1 = new java.awt.Panel();
        captionLabel = new java.awt.Label();
        logoPane = new ScrollPane(ScrollPane.SCROLLBARS_NEVER);
        logoPane.setSize(75, 75);
        label1 = new java.awt.Label();
        sessionList = new java.awt.List();
        label2 = new java.awt.Label();
        groupMembers = new java.awt.List();
        statusBar = new java.awt.TextField();
        menuBar1 = new java.awt.MenuBar();
        fileMenu = new java.awt.Menu();
        changePasswordMenuItem = new java.awt.MenuItem();
        logoutMenuItem = new java.awt.MenuItem();
        exitMenuItem = new java.awt.MenuItem();
        editMenu = new java.awt.Menu();
        copyMenuItem = new java.awt.MenuItem();
        pasteMenuItem = new java.awt.MenuItem();
        optionMenu = new java.awt.Menu();
        allowIncomingMenu = new java.awt.MenuItem();
        autoAnswerMenu = new java.awt.MenuItem();
        audioControlMenu = new java.awt.MenuItem();
        operationsMenu = new java.awt.Menu();
        talktoMenuItem = new java.awt.MenuItem();
        joinMenu = new java.awt.MenuItem();
        emailMenuItem = new java.awt.MenuItem();
        helpMenu = new java.awt.Menu();
        aboutMenuItem = new java.awt.MenuItem();
        setLayout(new java.awt.GridBagLayout());
        setBackground(java.awt.Color.white);
        setTitle(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Talk"));
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });
        panel1.setLayout(new java.awt.GridBagLayout());
        panel1.setBackground(new java.awt.Color(255, 255, 255));
        captionLabel.setFont(new java.awt.Font("Dialog", 0, 18));
        captionLabel.setText(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Web_Talk_Virtual_Call_Center"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 100.0;
        panel1.add(captionLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panel1.add(logoPane, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(panel1, gridBagConstraints);
        label1.setText(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Active_Sessions"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 100.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        add(label1, gridBagConstraints);
        sessionList.setMultipleMode(true);
        sessionList.setBackground(new java.awt.Color(255, 255, 255));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 100.0;
        gridBagConstraints.weighty = 20.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        add(sessionList, gridBagConstraints);
        label2.setText(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Friends_List"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        add(label2, gridBagConstraints);
        groupMembers.setFont(new java.awt.Font("Monospaced", 0, 12));
        groupMembers.setBackground(new java.awt.Color(255, 255, 255));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 100.0;
        gridBagConstraints.weighty = 80.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        add(groupMembers, gridBagConstraints);
        statusBar.setBackground(new java.awt.Color(255, 255, 255));
        statusBar.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(statusBar, gridBagConstraints);
        fileMenu.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("File"));
        changePasswordMenuItem.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Change_password"));
        changePasswordMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changePasswordMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(changePasswordMenuItem);
        fileMenu.addSeparator();
        logoutMenuItem.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Logout"));
        logoutMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logoutMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(logoutMenuItem);
        exitMenuItem.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Exit"));
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);
        menuBar1.add(fileMenu);
        editMenu.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Edit"));
        copyMenuItem.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Copy"));
        editMenu.add(copyMenuItem);
        pasteMenuItem.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Paste"));
        editMenu.add(pasteMenuItem);
        menuBar1.add(editMenu);
        optionMenu.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Options"));
        allowIncomingMenu.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Set_Do_Not_Disturb_quot"));
        allowIncomingMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                allowIncomingMenuActionPerformed(evt);
            }
        });
        optionMenu.add(allowIncomingMenu);
        autoAnswerMenu.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Set_Auto_Answer_______") + "       ");
        autoAnswerMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoAnswerMenuActionPerformed(evt);
            }
        });
        optionMenu.add(autoAnswerMenu);
        audioControlMenu.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Audio_Control"));
        audioControlMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                audioControlMenuActionPerformed(evt);
            }
        });
        optionMenu.add(audioControlMenu);
        menuBar1.add(optionMenu);
        operationsMenu.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Operations"));
        talktoMenuItem.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Talk_to_") + " ");
        talktoMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                talktoMenuItemActionPerformed(evt);
            }
        });
        operationsMenu.add(talktoMenuItem);
        joinMenu.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Join_selected_sessions"));
        joinMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                joinMenuActionPerformed(evt);
            }
        });
        operationsMenu.add(joinMenu);
        emailMenuItem.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Send_E-mail"));
        emailMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                emailMenuItemActionPerformed(evt);
            }
        });
        operationsMenu.add(emailMenuItem);
        menuBar1.add(operationsMenu);
        helpMenu.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Help"));
        aboutMenuItem.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("About"));
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);
        menuBar1.add(helpMenu);
        setMenuBar(menuBar1);
        pack();
    }

    private void initUserToUserElements() {
        String applications = applet.getParameter("application");
        if (applications == null) {
            return;
        }
        StringTokenizer tokens = new StringTokenizer(applications, ":");
        int num_tokens = tokens.countTokens();
        boolean sep_placed = false;
        for (int i = 0; i < num_tokens; i++) {
            String class_info = tokens.nextToken();
            StringTokenizer ptokens = new StringTokenizer(class_info, "?");
            int num_ptokens = ptokens.countTokens();
            String class_name = ptokens.nextToken();
            Hashtable params = new Hashtable();
            if (num_ptokens > 1) {
                String param_values = ptokens.nextToken();
                StringTokenizer pvtokens = new StringTokenizer(param_values, "&");
                int num_pvtokens = pvtokens.countTokens();
                for (int j = 0; j < num_pvtokens; j++) {
                    StringTokenizer pvtoken = new StringTokenizer(pvtokens.nextToken(), "=");
                    int num = pvtoken.countTokens();
                    if (num == 2) {
                        params.put(pvtoken.nextToken(), pvtoken.nextToken());
                    }
                }
            }
            try {
                Class c = Class.forName(class_name);
                NetworkApplicationInterface n = (NetworkApplicationInterface) c.newInstance();
                n.init(params);
                applicationList.put(class_name, n);
                if (n.menuItemName() != null) {
                    if (sep_placed == false) {
                        operationsMenu.addSeparator();
                        sep_placed = true;
                    }
                    if (isRegistered() == true) {
                        MenuItem mi = new MenuItem(n.menuItemName());
                        mi.addActionListener(new ApplicationsMenuListener(class_name));
                        operationsMenu.add(mi);
                    }
                }
            } catch (Exception ex) {
                System.out.println(ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace();
                continue;
            }
        }
    }

    /**
	 * @return Returns the allowSendBuzz.
	 */
    public boolean isAllowSendBuzz() {
        return allowSendBuzz;
    }

    /**
	 * @return Returns the allowSendWebPage.
	 */
    public boolean isAllowSendWebPage() {
        return allowSendWebPage;
    }

    public boolean isAutoAnswer() {
        return autoAnswer;
    }

    /**
	 * Getter for property displaySessionInfo.
	 * 
	 * @return Value of property displaySessionInfo.
	 * 
	 */
    public boolean isDisplaySessionInfo() {
        return this.displaySessionInfo;
    }

    /**
	 * Getter for property bringToFront.
	 * 
	 * @return Value of property bringToFront.
	 * 
	 */
    public boolean isBringToFront() {
        return this.bringToFront;
    }

    /**
	 * Getter for property emailEnabled.
	 * 
	 * @return Value of property emailEnabled.
	 * 
	 */
    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public boolean isRegistered() {
        return !unregistered;
    }

    private void joinMenuActionPerformed(java.awt.event.ActionEvent evt) {
        int[] selected_items = sessionList.getSelectedIndexes();
        if (selected_items.length < 2) {
            new InformationDialog(TalkFrame.this, ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Action_not_permitted"), ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("You_should_select_at_least_two_sessions"), ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("_Close_"), true);
            return;
        }
        JoinRequestMessage message = new JoinRequestMessage();
        for (int i = 0; i < selected_items.length; i++) {
            TalkSessionInterface session = (TalkSessionInterface) sessionListVector.elementAt(i);
            CallInfo call_info = session.getCallInfo();
            if (call_info.isConnected() == false) {
                new InformationDialog(TalkFrame.this, ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Action_not_permitted"), ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("All_sessions_must_be_in_the_connected_state"), ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("_Close_"), true);
                return;
            }
            message.addSession(call_info.getSessionId());
        }
    }

    private boolean login(String user, String password) {
        RegistrationRequestMessage message = new RegistrationRequestMessage();
        message.setUserName(user);
        message.setPassword(password);
        message.setLanguage(language);
        return true;
    }

    private void logoutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        setTitle(title + ' ' + ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("_not_logged_in"));
        disconnect();
        closedConnectionTs[0] = closedConnectionTs[1] = closedConnectionTs[2] = null;
        Enumeration elements = callList.elements();
        while (elements.hasMoreElements() == true) {
            TalkSessionInterface session = (TalkSessionInterface) elements.nextElement();
            session.dispose();
        }
        groupMembers.removeAll();
        groupList.removeAllElements();
        sessionList.removeAll();
        callList.clear();
        AudioControlDialog.reset();
        resetOptions();
        disconnect();
        doLogin();
    }

    private void performLogin(String message) {
        while (true) {
            LoginDialog login_dialog = new LoginDialog(TalkFrame.this, message, locale);
            if (login_dialog.isButtonClicked() == false) {
                login_dialog.dispose();
                continue;
            }
            if (login_dialog.isLoginClicked() == true) {
                String user = login_dialog.getUser();
                String password = login_dialog.getPassword();
                if (user.length() == 0) {
                    continue;
                }
                if (connect(null) == false) {
                    continue;
                }
                if (login(user, password) == true) {
                    userInformation.setName(user);
                    break;
                }
            } else {
                dispose();
                break;
            }
        }
    }

    public void popup() {
        show();
    }

    private void processGroupElement(GroupMemberElement element) {
        switch(element.getOperation()) {
            case GroupMemberElement.OPERATION_ADD_LIST:
                groupList.addElement(new ListGroupElement(element.getUser(), element.getFullName(), element.getCallCount()));
                StringBuffer buffer = new StringBuffer(element.getUser() + " ");
                if (element.getFullName() != null) {
                    buffer.append("\"" + element.getFullName() + "\" ");
                } else {
                    buffer.append(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("_quot_Unknown_quot_") + ' ');
                }
                int len = buffer.length();
                for (int i = 0; i < 40 - len; i++) {
                    buffer.append(' ');
                }
                buffer.append(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Active_sessions_") + ' ' + element.getCallCount());
                groupMembers.add(buffer.toString());
                if (loginTone != null) {
                    if (AudioControlDialog.getMuteLogNotification() == false) {
                        loginTone.play();
                    } else {
                        Toolkit.getDefaultToolkit().beep();
                    }
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
                Date d = new Date((new Date()).getTime() + timeAdjustment);
                statusBar.setText(element.getUser() + ' ' + ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("has_logged_in_at") + ' ' + DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM, locale).format(d));
                break;
            case GroupMemberElement.OPERATION_REM_LIST:
                int size = groupList.size();
                for (int i = 0; i < size; i++) {
                    ListGroupElement group = (ListGroupElement) groupList.elementAt(i);
                    if (group.getName().equals(element.getUser()) == true) {
                        groupList.removeElement(group);
                        groupMembers.remove(i);
                        break;
                    }
                }
                if (logoutTone != null) {
                    if (AudioControlDialog.getMuteLogNotification() == false) {
                        logoutTone.play();
                    } else {
                        Toolkit.getDefaultToolkit().beep();
                    }
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
                d = new Date((new Date()).getTime() + timeAdjustment);
                statusBar.setText(element.getUser() + ' ' + ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("has_logged_out_at") + ' ' + DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM, locale).format(d));
                break;
            case GroupMemberElement.OPERATION_MOD_LIST:
                size = groupList.size();
                for (int i = 0; i < size; i++) {
                    ListGroupElement group = (ListGroupElement) groupList.elementAt(i);
                    if (group.getName().equals(element.getUser()) == true) {
                        buffer = new StringBuffer(element.getUser() + " ");
                        if (element.getFullName() != null) {
                            buffer.append("\"" + element.getFullName() + "\" ");
                            group.setFullName(element.getFullName());
                        } else {
                            if (group.getFullName() != null) {
                                buffer.append("\"" + group.getFullName() + "\" ");
                            } else {
                                buffer.append(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("quot_Unknown_quot_") + ' ');
                            }
                        }
                        len = buffer.length();
                        for (int j = 0; j < 40 - len; j++) {
                            buffer.append(' ');
                        }
                        buffer.append(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Active_sessions_") + ' ' + element.getCallCount());
                        groupMembers.replaceItem(buffer.toString(), i);
                        break;
                    }
                }
                break;
        }
    }

    public void removeFromFrameList(Frame frame) {
        frameList.removeElement(frame);
    }

    public void resetOptions() {
        autoAnswer = false;
        allowInboundCalls = true;
        setOptionsMenu();
    }

    public void sessionAdded(TalkSessionInterface session) {
        synchronized (sessionLock) {
            callList.put(new Long(session.getCallInfo().getSessionId()), session);
            if (isRegistered() == true) {
                sessionList.add(getCallDescriptionText(session));
            }
            sessionListVector.addElement(session);
        }
    }

    public void sessionInformationChanged(TalkSessionInterface session) {
        synchronized (sessionLock) {
            for (int i = 0; i < sessionListVector.size(); i++) {
                TalkSessionInterface session_i = (TalkSessionInterface) sessionListVector.elementAt(i);
                if (session_i == session) {
                    if (isRegistered() == true) {
                        sessionList.replaceItem(getCallDescriptionText(session), i);
                    }
                    break;
                }
            }
        }
    }

    public void sessionRemoved(TalkSessionInterface session) {
        sessionRemoved(session, true);
    }

    public void sessionRemoved(TalkSessionInterface session, boolean dispose) {
        synchronized (sessionLock) {
            callList.remove(new Long(session.getCallInfo().getSessionId()));
            for (int i = 0; i < sessionListVector.size(); i++) {
                TalkSessionInterface session_i = (TalkSessionInterface) sessionListVector.elementAt(i);
                if (session_i == session) {
                    sessionListVector.removeElementAt(i);
                    if (isRegistered() == true) {
                        sessionList.remove(i);
                    }
                    break;
                }
            }
        }
        if (dispose == true) {
            if (unregistered == true) {
                dispose();
            }
        }
    }

    /**
	 * @param allowSendBuzz
	 *            The allowSendBuzz to set.
	 */
    public void setAllowSendBuzz(boolean allowSendBuzz) {
        this.allowSendBuzz = allowSendBuzz;
    }

    /**
	 * @param allowSendWebPage
	 *            The allowSendWebPage to set.
	 */
    public void setAllowSendWebPage(boolean allowSendWebPage) {
        this.allowSendWebPage = allowSendWebPage;
    }

    /**
	 * Setter for property applicationMediaElements.
	 * 
	 * @param applicationMediaElements
	 *            New value of property applicationMediaElements.
	 * 
	 */
    public void setApplicationMediaElements(java.util.Vector applicationMediaElements) {
        this.applicationMediaElements = applicationMediaElements;
    }

    /**
	 * Setter for property autoEmailInfo.
	 * 
	 * @param autoEmailInfo
	 *            New value of property autoEmailInfo.
	 * 
	 */
    public void setAutoEmailInfo(com.quikj.application.web.talk.client.AutoEmailTranscriptInfo autoEmailInfo) {
        this.autoEmailInfo = autoEmailInfo;
    }

    /**
	 * Setter for property cannedMessageList.
	 * 
	 * @param cannedMessageList
	 *            New value of property cannedMessageList.
	 * 
	 */
    public void setCannedMessageList(Vector cannedMessageList) {
        this.cannedMessageList = cannedMessageList;
    }

    /**
	 * Setter for property caption.
	 * 
	 * @param caption
	 *            New value of property caption.
	 * 
	 */
    public void setCaption(java.lang.String caption) {
        this.caption = caption;
    }

    /**
	 * Setter for property displaySessionInfo.
	 * 
	 * @param displaySessionInfo
	 *            New value of property displaySessionInfo.
	 * 
	 */
    public void setDisplaySessionInfo(boolean displaySessionInfo) {
        this.displaySessionInfo = displaySessionInfo;
    }

    /**
	 * Setter for property bringToFront.
	 * 
	 * @param bringToFront
	 *            New value of property bringToFront.
	 * 
	 */
    public void setBringToFront(boolean bringToFront) {
        this.bringToFront = bringToFront;
    }

    /**
	 * Setter for property emailEnabled.
	 * 
	 * @param emailEnabled
	 *            New value of property emailEnabled.
	 * 
	 */
    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    private void setLanguage() {
        language = applet.getParameter("language");
        locale = getLocale(language);
        String lang = locale.getLanguage();
        if (lang.equals("fr") == true) {
            language = "French";
        } else if (lang.equals("de") == true) {
            language = "German";
        } else if (lang.equals("es") == true) {
            language = "Spanish";
        } else if (lang.equals("nl") == true) {
            language = "Dutch";
        } else if (lang.equals("it") == true) {
            language = "Italian";
        } else if (lang.equals("hr") == true) {
            language = "Croatian";
        } else {
            language = "English";
        }
    }

    /**
	 * Setter for property logo.
	 * 
	 * @param logo
	 *            New value of property logo.
	 * 
	 */
    public void setLogo(java.awt.Image logo) {
        this.logo = logo;
    }

    private void setOptionsMenu() {
        if (allowInboundCalls == false) {
            allowIncomingMenu.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString(ALLOW));
        } else {
            allowIncomingMenu.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString(DISALLOW));
        }
        if (autoAnswer == true) {
            autoAnswerMenu.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString(DONT_AUTO_ANSWER));
        } else {
            autoAnswerMenu.setLabel(ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString(AUTO_ANSWER));
        }
    }

    /**
	 * Setter for property timeAdjustment.
	 * 
	 * @param timeAdjustment
	 *            New value of property timeAdjustment.
	 * 
	 */
    public void setTimeAdjustment(long timeAdjustment) {
        this.timeAdjustment = timeAdjustment;
    }

    private void talktoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        String name = getSelectedMember();
        TextFieldDialog make_call = new TextFieldDialog(TalkFrame.this, ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Call_") + " ", ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Call"), ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Cancel"), true, name);
        if (make_call.isOkSelected() == true) {
            String called_name = make_call.getText().trim();
            if (called_name.length() > 0) {
                if (called_name.equals(userInformation.getUserName()) == true) {
                    new InformationDialog(TalkFrame.this, ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("Call_status"), ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("You_cannot_call_yourself"), ResourceBundle.getBundle("com.quikj.application.web.talk.client.language", locale).getString("_Close_"), 5000L);
                    return;
                }
                TalkSessionInterface session = TalkSessionFactory.getInstance().createSession();
                session.makeCall(called_name);
            }
        }
    }
}
