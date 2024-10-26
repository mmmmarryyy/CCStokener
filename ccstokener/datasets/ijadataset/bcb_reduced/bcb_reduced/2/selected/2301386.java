package freestyleLearning.homeCore.usersManager;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import java.net.*;
import javax.xml.bind.*;
import freestyleLearning.homeCore.mainFrame.*;
import freestyleLearning.homeCore.programConfigurationManager.event.*;
import freestyleLearning.homeCore.usersManager.data.xmlBinding.*;
import freestyleLearning.homeCore.usersManager.data.xmlBindingSubclasses.*;
import freestyleLearning.homeCore.usersManager.event.*;
import freestyleLearningGroup.independent.gui.*;
import freestyleLearningGroup.independent.util.*;

public class FSLUsersManager implements FSLUserEventGenerator, FSLProgramConfigurationListener {

    private static final String LEARNER_ROLE = "learner";

    private static final String AUTHOR_ROLE = "author";

    private FLGInternationalization internationalization;

    private File usersDirectory;

    private File usersDescriptorFile;

    private FSLUsersDescriptor usersDescriptor;

    private FSLUserDescriptor currentUserDescriptor;

    private FSLUserLoginDialog userLoginDialog;

    private FSLUsersManagementDialog usersManagementDialog;

    private FSLProgramConfigurationEventGenerator configurationEventGenerator;

    private FSLUserPassEncrypter userPassEncrypter;

    private Vector userListeners;

    private JMenu userMenu;

    private Hashtable actions;

    private Hashtable menus;

    private Hashtable menuItems;

    private Color[] colors;

    private String[] fontSizes;

    private JFrame mainFrame;

    private FLGHtmlPane displayResponsePane;

    private URL url = null;

    private String response;

    public void init(FSLProgramConfigurationEventGenerator configurationEventGenerator, FSLUserListener userListener, File usersDirectory) {
        this.mainFrame = FLGUIUtilities.getMainFrame();
        this.configurationEventGenerator = configurationEventGenerator;
        this.usersDirectory = usersDirectory;
        configurationEventGenerator.addProgramConfigurationListener(this);
        internationalization = new FLGInternationalization("freestyleLearning.homeCore.usersManager.internationalization", FSLUsersManager.class.getClassLoader());
        this.usersDescriptor = loadUsersDescriptor(new File(usersDirectory, "usersDescriptor.xml"));
        this.userLoginDialog = new FSLUserLoginDialog();
        this.usersManagementDialog = new FSLUsersManagementDialog();
        this.userListeners = new Vector();
        userPassEncrypter = new FSLUserPassEncrypter();
        addUserListener(userListener);
        buildIndependentUI();
        buildDependentUI();
    }

    public boolean getDisplayWelcomeScreen() {
        return currentUserDescriptor.getAutomaticSelectionEnabled();
    }

    public boolean getAutomaticSelectionEnabled() {
        return currentUserDescriptor.getAutomaticSelectionEnabled();
    }

    public boolean getRememberFrameStatusEnabled() {
        return currentUserDescriptor.getRememberFrameStatusEnabled();
    }

    public void saveUserFrameConfiguration() {
        if (currentUserDescriptor != null && currentUserDescriptor.getRememberFrameStatusEnabled()) {
            FSLUserFrameDescriptor frameDescriptor = new FSLUserFrameDescriptor();
            frameDescriptor.setFrameMaximized(mainFrame.getExtendedState() == JFrame.MAXIMIZED_BOTH);
            frameDescriptor.setFrameHeight(mainFrame.getHeight());
            frameDescriptor.setFrameWidth(mainFrame.getWidth());
            frameDescriptor.setFrameLocationX(mainFrame.getLocation().x);
            frameDescriptor.setFrameLocationY(mainFrame.getLocation().y);
            if (!currentUserDescriptor.getUserFrameOptions().isEmpty()) currentUserDescriptor.getUserFrameOptions().clear();
            currentUserDescriptor.getUserFrameOptions().add(frameDescriptor);
            saveUsersDescriptor();
        }
    }

    private String[] getUserFontSizesFromDescriptor(FSLUserDescriptor userDescriptor) {
        String[] userFontSizes = new String[2];
        java.util.List userFontSizesList = userDescriptor.getUserFonts();
        if (!userFontSizesList.isEmpty()) {
            for (int i = 0; i < userFontSizesList.size(); i++) {
                UserFontDescriptor fontDescriptor = (UserFontDescriptor) userFontSizesList.get(i);
                userFontSizes[0] = fontDescriptor.getStructureTreeFontSize();
                userFontSizes[1] = fontDescriptor.getElementsContentsPanelFontSize();
            }
        } else {
            userFontSizes[0] = "" + UIManager.get("FSLLearningUnitViewElementsStructurePanel.BaseFontSize");
            userFontSizes[1] = "" + UIManager.get("FSLLearningUnitViewElementsStructurePanel.BaseFontSize");
        }
        return userFontSizes;
    }

    private Color[] getUserColorsFromDescriptor(FSLUserDescriptor userDescriptor) {
        Color[] userColors = new Color[FSLMainFrame.NO_MAINFRAMECOLORS];
        java.util.List userColorsList = userDescriptor.getUserColors();
        if (!userColorsList.isEmpty()) {
            for (int i = 0; i < userColorsList.size(); i++) {
                UserColorDescriptor colorDescriptor = (UserColorDescriptor) userColorsList.get(i);
                int red = Integer.parseInt(colorDescriptor.getRed());
                int green = Integer.parseInt(colorDescriptor.getGreen());
                int blue = Integer.parseInt(colorDescriptor.getBlue());
                userColors[i] = new Color(red, green, blue);
            }
        } else {
            for (int i = 0; i < userColors.length; i++) {
                userColors[i] = (Color) UIManager.get("FSLMainFrameDefaultColor" + (i + 1));
            }
        }
        return userColors;
    }

    public void configurationChanged(FSLProgramConfigurationEvent event) {
        if (event.getEventType() == FSLProgramConfigurationEvent.AUTOMATIC_LOGIN) {
            FSLUserDescriptor userDescriptor = findUserDescriptorByUserName(event.getAutomaticLoginUserName());
            if (userDescriptor != null) {
                currentUserDescriptor = userDescriptor;
                colors = getUserColorsFromDescriptor(currentUserDescriptor);
                fontSizes = getUserFontSizesFromDescriptor(currentUserDescriptor);
                FSLUserEvent userEvent;
                boolean automaticSelectionEnabled = false;
                boolean displayWelcomeScreenEnabled = true;
                boolean rememberFrameStatusEnabled = true;
                try {
                    automaticSelectionEnabled = currentUserDescriptor.getAutomaticSelectionEnabled();
                    displayWelcomeScreenEnabled = currentUserDescriptor.getDisplayWelcomeScreen();
                    rememberFrameStatusEnabled = currentUserDescriptor.getRememberFrameStatusEnabled();
                } catch (javax.xml.bind.NoValueException nve) {
                    currentUserDescriptor.setAutomaticSelectionEnabled(automaticSelectionEnabled);
                    currentUserDescriptor.setDisplayWelcomeScreen(displayWelcomeScreenEnabled);
                    currentUserDescriptor.setRememberFrameStatusEnabled(rememberFrameStatusEnabled);
                    saveUsersDescriptor();
                }
                Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
                int frameWidth = screenDim.width;
                int frameHeight = screenDim.height;
                int frameLocationX = 0;
                int frameLocationY = 0;
                boolean frameMaximized = true;
                try {
                    UserFrameDescriptor frameDescriptor = (UserFrameDescriptor) currentUserDescriptor.getUserFrameOptions().get(0);
                    frameMaximized = frameDescriptor.getFrameMaximized();
                    if (!frameMaximized) {
                        frameWidth = frameDescriptor.getFrameWidth();
                        frameHeight = frameDescriptor.getFrameHeight();
                        frameLocationX = frameDescriptor.getFrameLocationX();
                        frameLocationY = frameDescriptor.getFrameLocationY();
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
                userEvent = freestyleLearning.homeCore.usersManager.event.FSLUserEvent.createUserChangedEvent(new File(usersDirectory, currentUserDescriptor.getLearningUnitsUserDataDirectoryName()), currentUserDescriptor.getUserName(), currentUserDescriptor.getUserPassword(), currentUserDescriptor.getCurrentUserRole(), colors, fontSizes, true, automaticSelectionEnabled, displayWelcomeScreenEnabled, rememberFrameStatusEnabled, frameWidth, frameHeight, frameLocationX, frameLocationY, frameMaximized);
                fireUserEvent(userEvent);
            } else {
                FLGOptionPane.showMessageDialog(internationalization.getString("message.userUnknown"), internationalization.getString("message.loginFailure"), FLGOptionPane.ERROR_MESSAGE);
            }
        }
        if (event.getEventType() == FSLProgramConfigurationEvent.COLOR_CHANGED) {
            if (currentUserDescriptor != null) {
                currentUserDescriptor.emptyUserColors();
                java.util.List userColors = currentUserDescriptor.getUserColors();
                colors = event.getColors();
                for (int i = 0; i < colors.length; i++) {
                    FSLUserColorDescriptor colorDescriptor = new FSLUserColorDescriptor();
                    colorDescriptor.setRed("" + colors[i].getRed());
                    colorDescriptor.setGreen("" + colors[i].getGreen());
                    colorDescriptor.setBlue("" + colors[i].getBlue());
                    currentUserDescriptor.getUserColors().add(colorDescriptor);
                }
                saveUsersDescriptor();
            }
        }
        if (event.getEventType() == FSLProgramConfigurationEvent.FONT_CHANGED) {
            if (currentUserDescriptor != null) {
                currentUserDescriptor.emptyUserFonts();
                java.util.List userFontSizes = currentUserDescriptor.getUserFonts();
                fontSizes = event.getFontSizes();
                FSLUserFontDescriptor fontDescriptor = new FSLUserFontDescriptor();
                fontDescriptor.setElementsContentsPanelFontSize("" + UIManager.get("FSLLearningUnitViewElementsContentsPanel.BaseFontSize"));
                fontDescriptor.setStructureTreeFontSize("" + UIManager.get("FSLLearningUnitViewElementsStructurePanel.BaseFontSize"));
                currentUserDescriptor.getUserFonts().add(fontDescriptor);
                saveUsersDescriptor();
            }
        }
        if (event.getEventType() == FSLProgramConfigurationEvent.FSL_CONFIGURATION_CHANGED) {
            if (currentUserDescriptor != null) {
                currentUserDescriptor.setAutomaticSelectionEnabled(event.isAutomaticSelectionEnabled());
                currentUserDescriptor.setDisplayWelcomeScreen(event.isDisplayWelcomeScreenEnabled());
                currentUserDescriptor.setRememberFrameStatusEnabled(event.isRememberFrameStatusEnabled());
                saveUsersDescriptor();
            }
        }
    }

    public void addUserListener(FSLUserListener listener) {
        userListeners.add(listener);
    }

    public void removeUserListener(FSLUserListener listener) {
        userListeners.remove(listener);
    }

    public JMenu getUserMenu() {
        return userMenu;
    }

    private String getCurrentUserLearningUnitDataDirectoryName() {
        return currentUserDescriptor.getLearningUnitsUserDataDirectoryName();
    }

    private void buildIndependentUI() {
        actions = new Hashtable();
        menuItems = new Hashtable();
        menus = new Hashtable();
        userMenu = new JMenu(internationalization.getString("menu.user.title"));
        userMenu.setMnemonic(internationalization.getString("menu.user.mnemonic").charAt(0));
        JMenuItem menuItem_newUser = createMenuItem("menu.user.new.title");
        char newUserChar = internationalization.getString("menu.user.new.mnemonic").charAt(0);
        menuItem_newUser.setMnemonic(newUserChar);
        userMenu.add(menuItem_newUser);
        JMenuItem menuItem_login = createMenuItem("menu.user.login.title");
        char loginChar = internationalization.getString("menu.user.login.mnemonic").charAt(0);
        menuItem_login.setMnemonic(loginChar);
        menuItem_login.setAccelerator(KeyStroke.getKeyStroke(loginChar, KeyEvent.CTRL_MASK));
        userMenu.add(menuItem_login);
        JMenu subMenu = new JMenu(internationalization.getString("menu.user.changeUserRole.title"));
        subMenu.setMnemonic(internationalization.getString("menu.user.changeUserRole.mnemonic").charAt(0));
        menus.put("menu.user.changeUserRole.title", subMenu);
        userMenu.add(subMenu);
        JMenuItem menuItem_delete = createMenuItem("menu.user.delete.title");
        userMenu.add(menuItem_delete);
        ButtonGroup buttonGroup = new ButtonGroup();
        JMenuItem menuItem = createMenuItem("menu.user.changeUserRole.learner.title", true);
        subMenu.add(menuItem);
        buttonGroup.add(menuItem);
        menuItem = createMenuItem("menu.user.changeUserRole.author.title", true);
        subMenu.add(menuItem);
        buttonGroup.add(menuItem);
        getAction("menu.user.login.title").setEnabled(true);
        getMenu("menu.user.changeUserRole.title").setEnabled(true);
    }

    private void buildDependentUI() {
        if (currentUserDescriptor != null) {
            String currentUserRole = currentUserDescriptor.getCurrentUserRole();
            if (currentUserDescriptor.getAllowedUserRoles().size() > 1) {
                getMenu("menu.user.changeUserRole.title").setEnabled(true);
                getAction("menu.user.changeUserRole.learner.title").setEnabled(userRoleIsAllowed(LEARNER_ROLE));
                getAction("menu.user.changeUserRole.author.title").setEnabled(userRoleIsAllowed(AUTHOR_ROLE));
                ((JCheckBoxMenuItem) getMenuItem("menu.user.changeUserRole.learner.title")).setSelected(currentUserRole.equals(LEARNER_ROLE));
                ((JCheckBoxMenuItem) getMenuItem("menu.user.changeUserRole.author.title")).setSelected(currentUserRole.equals(AUTHOR_ROLE));
            } else {
                getMenu("menu.user.changeUserRole.title").setEnabled(false);
            }
        } else {
            getMenu("menu.user.changeUserRole.title").setEnabled(false);
        }
        ;
        getAction("menu.user.delete.title").setEnabled(currentUserDescriptor != null);
    }

    private boolean userRoleIsAllowed(String userRole) {
        java.util.List allowedUserRoles = currentUserDescriptor.getAllowedUserRoles();
        for (int i = 0; i < allowedUserRoles.size(); i++) {
            String allowedUserRole = (String) allowedUserRoles.get(i);
            if (allowedUserRole.equals(userRole)) return true;
        }
        return false;
    }

    private Action getAction(String id) {
        return (Action) actions.get(id);
    }

    private JMenuItem getMenuItem(String id) {
        return (JMenuItem) menuItems.get(id);
    }

    private JMenu getMenu(String id) {
        return (JMenu) menus.get(id);
    }

    private JMenuItem createMenuItem(String menuItemId) {
        return createMenuItem(menuItemId, false);
    }

    private JMenuItem createMenuItem(String menuItemId, boolean asCheckBoxMenuItem) {
        Action action = new AbstractAction(internationalization.getString(menuItemId)) {

            public void actionPerformed(ActionEvent e) {
                menuItemSelected(((JMenuItem) e.getSource()).getActionCommand());
            }
        };
        actions.put(menuItemId, action);
        JMenuItem menuItem;
        if (asCheckBoxMenuItem) {
            menuItem = new JCheckBoxMenuItem(action);
        } else {
            menuItem = new JMenuItem(action);
        }
        menuItem.setActionCommand(menuItemId);
        menuItems.put(menuItemId, menuItem);
        return menuItem;
    }

    private void askUserForLogin() {
        saveUserFrameConfiguration();
        FSLUserVetoableEvent vetoableEvent = FSLUserVetoableEvent.createUserChangingEvent();
        fireUserEvent(vetoableEvent);
        if (!vetoableEvent.isVeto()) {
            boolean loginSuccessful = false;
            if (userLoginDialog.showDialog()) {
                FSLUserDescriptor userDescriptor = findUserDescriptorByUserName(userLoginDialog.getUserName());
                if (userDescriptor != null) {
                    boolean password_hash = false;
                    try {
                        if (userDescriptor.getUserPassword().equals("author") || userDescriptor.getUserPassword().equals("learner") || userDescriptor.getUserPassword().equals("admin")) {
                            password_hash = true;
                        } else {
                            String loginPassword = userLoginDialog.getUserPassword();
                            String hash = userPassEncrypter.encrypt(loginPassword);
                            if (hash.equals(userDescriptor.getUserPassword())) {
                                password_hash = true;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (password_hash) {
                        loginSuccessful = true;
                        currentUserDescriptor = userDescriptor;
                    } else {
                        FLGOptionPane.showMessageDialog(internationalization.getString("message.passwordIncorrect"), internationalization.getString("message.loginFailure"), FLGOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    FLGOptionPane.showMessageDialog(internationalization.getString("message.userUnknown"), internationalization.getString("message.loginFailure"), FLGOptionPane.ERROR_MESSAGE);
                }
                if (loginSuccessful) {
                    performUserLogin(userDescriptor);
                }
            }
        }
    }

    private void performUserLogin(FSLUserDescriptor userDescriptor) {
        FSLUserEvent userEvent;
        currentUserDescriptor = userDescriptor;
        boolean automaticSelectionEnabled = false;
        boolean displayWelcomeScreenEnabled = true;
        boolean rememberFrameStatusEnabled = true;
        colors = getUserColorsFromDescriptor(userDescriptor);
        fontSizes = getUserFontSizesFromDescriptor(userDescriptor);
        try {
            automaticSelectionEnabled = currentUserDescriptor.getAutomaticSelectionEnabled();
            displayWelcomeScreenEnabled = currentUserDescriptor.getDisplayWelcomeScreen();
            rememberFrameStatusEnabled = currentUserDescriptor.getRememberFrameStatusEnabled();
        } catch (javax.xml.bind.NoValueException nve) {
            currentUserDescriptor.setAutomaticSelectionEnabled(automaticSelectionEnabled);
            currentUserDescriptor.setPlayWelcomeVideosEnabled(displayWelcomeScreenEnabled);
            currentUserDescriptor.setRememberFrameStatusEnabled(rememberFrameStatusEnabled);
            saveUsersDescriptor();
        }
        Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
        int frameWidth = screenDim.width;
        int frameHeight = screenDim.height;
        int frameLocationX = 0;
        int frameLocationY = 0;
        boolean frameMaximized = true;
        try {
            UserFrameDescriptor frameDescriptor = (UserFrameDescriptor) currentUserDescriptor.getUserFrameOptions().get(0);
            frameMaximized = frameDescriptor.getFrameMaximized();
            if (!frameMaximized) {
                frameWidth = frameDescriptor.getFrameWidth();
                frameHeight = frameDescriptor.getFrameHeight();
                frameLocationX = frameDescriptor.getFrameLocationX();
                frameLocationY = frameDescriptor.getFrameLocationY();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        userEvent = freestyleLearning.homeCore.usersManager.event.FSLUserEvent.createUserChangedEvent(new File(usersDirectory, currentUserDescriptor.getLearningUnitsUserDataDirectoryName()), currentUserDescriptor.getUserName(), currentUserDescriptor.getUserPassword(), currentUserDescriptor.getCurrentUserRole(), colors, fontSizes, userLoginDialog.isAutomaticLoginEnabled(), automaticSelectionEnabled, displayWelcomeScreenEnabled, rememberFrameStatusEnabled, frameWidth, frameHeight, frameLocationX, frameLocationY, frameMaximized);
        fireUserEvent(userEvent);
    }

    private void showNewUserDialog() {
        FSLNewUserDialog newUserDialog = new FSLNewUserDialog();
        if (newUserDialog.showDialog()) {
            if (openUssUserCheckPerformed(newUserDialog)) {
                if (newUserDialog.getUserName() != null && newUserDialog.getUserName().length() > 0 && newUserDialog.getPassword() != null && newUserDialog.getPassword().length() > 0 && newUserDialog.isPasswordConfirmed()) {
                    String userName = newUserDialog.getUserName();
                    if (!checkUserExisting(userName)) {
                        String password = newUserDialog.getPassword();
                        try {
                            password = userPassEncrypter.encrypt(password);
                        } catch (Exception exp) {
                            exp.printStackTrace();
                        }
                        FSLUserDescriptor newUserDescriptor = FSLUserDescriptor.createFSL_UserDescriptor(createUserId(userName), userName, password, newUserDialog.getUserRoles());
                        usersDescriptor.getUsersDescriptors().add(newUserDescriptor);
                        saveUsersDescriptor();
                        performUserLogin(newUserDescriptor);
                    } else {
                        FLGOptionPane.showMessageDialog(internationalization.getString("message.userExists.text"), internationalization.getString("message.userExists.title"), FLGOptionPane.WARNING_MESSAGE);
                    }
                }
            } else {
                FLGOptionPane.showMessageDialog(internationalization.getString("message.failed.text"), internationalization.getString("message.failed.title"), FLGOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void showDeleteUserDialog() {
        if (currentUserDescriptor != null) {
            if (FLGOptionPane.showConfirmDialog(internationalization.getString("dialog.deleteUser.message1") + " \"" + currentUserDescriptor.getUserName() + "\"\n" + internationalization.getString("dialog.deleteUser.message2"), internationalization.getString("dialog.deleteUser.title"), FLGOptionPane.YES_NO_OPTION, FLGOptionPane.WARNING_MESSAGE) == FLGOptionPane.YES_OPTION) {
                usersDescriptor.getUsersDescriptors().remove(currentUserDescriptor);
                currentUserDescriptor = null;
                saveUsersDescriptor();
                fireUserEvent(FSLUserEvent.createUserLogoutEvent());
            }
        }
    }

    private boolean checkUserExisting(String userName) {
        java.util.List usersList = usersDescriptor.getUsersDescriptors();
        for (int i = 0; i < usersList.size(); i++) {
            FSLUserDescriptor currentUser = (FSLUserDescriptor) usersList.get(i);
            String currentUserName = currentUser.getUserName();
            if (userName.equalsIgnoreCase(currentUserName)) return true;
        }
        return false;
    }

    private boolean openUssUserCheckPerformed(FSLNewUserDialog dialog) {
        if (!(dialog.getUserName() != null && dialog.getUserName().length() > 0 && dialog.getPassword() != null && dialog.getPassword().length() > 0 && dialog.isPasswordConfirmed())) {
            return false;
        }
        if (!dialog.isOpenUssUserRequested()) return true;
        String openUssServerName = dialog.openUssServerName;
        String openUssUserRole = dialog.userType;
        StringBuffer urlBuffer = new StringBuffer("http://" + openUssServerName + "/extension/directaccess/registration/RegistrationAction.po" + "?Usertype=" + dialog.userType + "&Code=" + dialog.code + "&FirstName=" + dialog.firstName + "&LastName=" + dialog.lastName + "&Username=" + dialog.userName + "&Password=" + dialog.password + "&RetypePassword=" + dialog.retypePassword + "&EMail=" + dialog.email + "&LanguageId=" + dialog.language_id);
        if (dialog.year.length() > 0) urlBuffer.append("&year=" + dialog.year);
        if (dialog.subject.length() > 0) urlBuffer.append("&subject=" + dialog.subject);
        if (dialog.personalID.length() > 0) urlBuffer.append("&personalID=" + dialog.personalID);
        if (dialog.title.length() > 0) urlBuffer.append("&title=" + dialog.title);
        if (dialog.address.length() > 0) urlBuffer.append("&address=" + dialog.address);
        if (dialog.city.length() > 0) urlBuffer.append("&city=" + dialog.city);
        if (dialog.postcode.length() > 0) urlBuffer.append("&postcode=" + dialog.postcode);
        if (dialog.land.length() > 0) urlBuffer.append("&land=" + dialog.land);
        if (dialog.telephonenumber.length() > 0) urlBuffer.append("&telephonenumber=" + dialog.telephonenumber);
        if (dialog.function.length() > 0) urlBuffer.append("&function=" + dialog.function);
        if (dialog.faculty_id.length() > 0) urlBuffer.append("&faculty_ID=" + dialog.faculty_id);
        if (dialog.language_id.length() > 0) urlBuffer.append("&language_ID=" + dialog.language_id);
        String urlString = new String(urlBuffer);
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
        }
        response = readData(url);
        if (response != null && FLGUtilities.contains(response, "OK: Success")) {
            FLGOptionPane.showMessageDialog(internationalization.getString("message.success.text"), internationalization.getString("message.success.title"), FLGOptionPane.OK_OPTION);
            return true;
        }
        return false;
    }

    private String readData(URL url) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer responseBuffer = new StringBuffer();
            String line;
            while ((line = in.readLine()) != null) {
                responseBuffer.append(line);
            }
            in.close();
            return new String(responseBuffer);
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    private String createUserId(String userId) {
        String newUserId = userId;
        int ix = 1;
        while (isUserIdExisting(newUserId)) {
            newUserId = userId + ix++;
        }
        return newUserId;
    }

    private boolean isUserIdExisting(String userId) {
        for (int i = 0; i < usersDescriptor.getUsersDescriptors().size(); i++) {
            FSLUserDescriptor userDescriptor = (FSLUserDescriptor) usersDescriptor.getUsersDescriptors().get(i);
            if (userId.equalsIgnoreCase(userDescriptor.getId())) return true;
        }
        return false;
    }

    private FSLUserDescriptor findUserDescriptorByUserName(String username) {
        java.util.List userDescriptorList = usersDescriptor.getUsersDescriptors();
        for (int i = 0; i < userDescriptorList.size(); i++) {
            FSLUserDescriptor userDescriptor = (FSLUserDescriptor) userDescriptorList.get(i);
            if (userDescriptor.getUserName().equals(username)) return userDescriptor;
        }
        return null;
    }

    private void saveUsersDescriptor() {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(usersDescriptorFile);
            usersDescriptor.validate();
            usersDescriptor.marshal(fileOutputStream);
            fileOutputStream.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private FSLUsersDescriptor loadUsersDescriptor(File usersDescriptorFile) {
        this.usersDescriptorFile = usersDescriptorFile;
        FSLUsersDescriptor usersDescriptor = null;
        Dispatcher dispatcher = UsersDescriptor.newDispatcher();
        dispatcher.register(UsersDescriptor.class, FSLUsersDescriptor.class);
        dispatcher.register(UserDescriptor.class, FSLUserDescriptor.class);
        FileInputStream usersDescriptorFileInputStream = null;
        try {
            usersDescriptorFileInputStream = new FileInputStream(usersDescriptorFile);
            usersDescriptor = (FSLUsersDescriptor) dispatcher.unmarshal(usersDescriptorFileInputStream);
            usersDescriptorFileInputStream.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return usersDescriptor;
    }

    private void fireUserEvent(FSLUserEvent event) {
        for (int i = 0; i < userListeners.size(); i++) {
            FSLUserListener userListener = (FSLUserListener) userListeners.get(i);
            switch(event.getEventType()) {
                case FSLUserEvent.USER_CHANGED:
                    userListener.userChanged(event);
                    break;
                case FSLUserEvent.USER_LOGOUT:
                    userListener.userLogout(event);
                    break;
                case FSLUserEvent.USER_ROLE_CHANGED:
                    userListener.userRoleChanged(event);
                    break;
                case FSLUserVetoableEvent.USER_CHANGING:
                    if (userListener instanceof FSLUserVetoableListener) {
                        FSLUserVetoableEvent vetoableEvent = (FSLUserVetoableEvent) event;
                        ((FSLUserVetoableListener) userListener).userChanging(vetoableEvent);
                        if (vetoableEvent.isVeto()) return;
                    }
                    break;
            }
        }
        buildDependentUI();
    }

    private void setCurrentUserRole(String currentUserRole) {
        currentUserDescriptor.setCurrentUserRole(currentUserRole);
        FSLUserEvent event = FSLUserEvent.createUserRoleChangedEvent(currentUserDescriptor.getUserName(), currentUserDescriptor.getCurrentUserRole());
        fireUserEvent(event);
    }

    private void menuItemSelected(String menuItemId) {
        if (menuItemId == "menu.user.login.title") askUserForLogin();
        if (menuItemId == "menu.user.new.title") showNewUserDialog();
        if (menuItemId == "menu.user.delete.title") showDeleteUserDialog();
        if (menuItemId == "menu.user.changeUserRole.learner.title") setCurrentUserRole(LEARNER_ROLE);
        if (menuItemId == "menu.user.changeUserRole.author.title") setCurrentUserRole(AUTHOR_ROLE);
    }
}
