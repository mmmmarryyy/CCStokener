package wotlas.client.screen;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import wotlas.client.ClientDirector;
import wotlas.client.DataManager;
import wotlas.client.PlayerImpl;
import wotlas.client.screen.plugin.MacroPlugIn;
import wotlas.common.Player;
import wotlas.common.PlayerState;
import wotlas.common.chat.ChatMessageHistory;
import wotlas.common.chat.ChatRoom;
import wotlas.common.message.chat.ChatRoomCreationMessage;
import wotlas.common.message.chat.RemPlayerFromChatRoomMessage;
import wotlas.common.message.chat.SendTextMessage;
import wotlas.common.universe.WotlasLocation;
import wotlas.libs.aswing.ALabel;
import wotlas.utils.Debug;

/** JPanel to show the chat engine
 *
 * @author Petrus, MasterBob
 */
public class JChatPanel extends JPanel implements MouseListener, ActionListener {

    private ImageIcon iconUp = ClientDirector.getResourceManager().getImageIcon("pin.gif");

    /** Our tabbedPane
     */
    private JTabbedPane tabbedPane;

    /** Button to create a new chatRoom
     */
    private JButton b_createChatRoom;

    /** Button to leave the chatRoom
     */
    private JButton b_leaveChatRoom;

    /** Button to get help on chat commands
     */
    private JButton b_helpChat;

    /** Button to insert an image in the chat.
     */
    private JButton b_imageChat;

    /** TextField where player writes messages
     */
    private JTextField inputBox;

    /** Voice Sound Level
     */
    private JSlider chatVoiceLevel;

    /** Primary key of current ChatRoom
     */
    private String currentPrimaryKey;

    /** Last messages in input box
     */
    private ChatMessageHistory messageHistory;

    /** To iterate the players hashtable
     */
    private Enumeration<PlayerState> e = null;

    /** the name we want to autocomplete
     */
    private String autoName = "";

    /** content of textField
     */
    private String input;

    /** Constructor.
     */
    public JChatPanel() {
        super();
        this.messageHistory = new ChatMessageHistory();
        this.tabbedPane = new JTabbedPane();
        JToolBar chatToolbar = new JToolBar();
        chatToolbar.setFloatable(false);
        this.b_createChatRoom = new JButton(ClientDirector.getResourceManager().getImageIcon("chat-new.gif"));
        this.b_createChatRoom.setActionCommand("createChatRoom");
        this.b_createChatRoom.addActionListener(this);
        this.b_createChatRoom.setToolTipText("Create a new chat room");
        chatToolbar.add(this.b_createChatRoom);
        this.b_leaveChatRoom = new JButton(ClientDirector.getResourceManager().getImageIcon("chat-leave.gif"));
        this.b_leaveChatRoom.setActionCommand("leaveChatRoom");
        this.b_leaveChatRoom.addActionListener(this);
        this.b_leaveChatRoom.setToolTipText("Leave the current chat room");
        chatToolbar.add(this.b_leaveChatRoom);
        chatToolbar.add(new JToolBar.Separator(new Dimension(30, 14)));
        this.b_helpChat = new JButton(ClientDirector.getResourceManager().getImageIcon("chat-help.gif"));
        this.b_helpChat.setActionCommand("helpChat");
        this.b_helpChat.addActionListener(this);
        this.b_helpChat.setToolTipText("To display the available chat commands");
        chatToolbar.add(this.b_helpChat);
        this.b_imageChat = new JButton(ClientDirector.getResourceManager().getImageIcon("chat-image.gif"));
        this.b_imageChat.setActionCommand("imageChat");
        this.b_imageChat.addActionListener(this);
        this.b_imageChat.setToolTipText("To insert an image in the chat");
        chatToolbar.add(this.b_imageChat);
        JPanel bottomChat = new JPanel(false);
        bottomChat.setLayout(new BoxLayout(bottomChat, BoxLayout.X_AXIS));
        this.inputBox = new NoFocusJTextField();
        this.inputBox.getCaret().setVisible(true);
        this.inputBox.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if ((keyEvent.getKeyCode() == KeyEvent.VK_TAB) || (keyEvent.getKeyChar() == '\t')) {
                    JChatPanel.this.inputBox.setText(nameCompletion());
                    keyEvent.consume();
                    return;
                }
                if (keyEvent.getKeyCode() == KeyEvent.VK_UP) {
                    JChatPanel.this.inputBox.setText(JChatPanel.this.messageHistory.getPrevious(JChatPanel.this.inputBox.getText()));
                    return;
                }
                if (keyEvent.getKeyCode() == KeyEvent.VK_DOWN) {
                    JChatPanel.this.inputBox.setText(JChatPanel.this.messageHistory.getNext(JChatPanel.this.inputBox.getText()));
                    return;
                }
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    okAction();
                    return;
                }
                JChatPanel.this.e = null;
            }
        });
        this.chatVoiceLevel = new JSlider(SwingConstants.HORIZONTAL, 0, 2, ChatRoom.NORMAL_VOICE_LEVEL);
        this.chatVoiceLevel.setMajorTickSpacing(1);
        this.chatVoiceLevel.setMinorTickSpacing(1);
        this.chatVoiceLevel.setSnapToTicks(true);
        this.chatVoiceLevel.setPaintTicks(true);
        this.chatVoiceLevel.setMaximumSize(new Dimension(80, 30));
        this.chatVoiceLevel.setMinimumSize(new Dimension(80, 30));
        this.chatVoiceLevel.setPreferredSize(new Dimension(80, 30));
        bottomChat.add(new ALabel(ClientDirector.getResourceManager().getImageIcon("chat-sound-level.gif")));
        bottomChat.add(this.chatVoiceLevel);
        bottomChat.add(this.inputBox);
        setLayout(new BorderLayout());
        add("North", chatToolbar);
        add("Center", this.tabbedPane);
        add("South", bottomChat);
        ChatRoom mainChat = new ChatRoom();
        mainChat.setPrimaryKey(ChatRoom.DEFAULT_CHAT);
        mainChat.setName("");
        JChatRoom jchatRoom = addJChatRoom(mainChat);
        this.currentPrimaryKey = ChatRoom.DEFAULT_CHAT;
    }

    /** To get current ChatRoom primaryKey
     */
    public String getMyCurrentChatPrimaryKey() {
        return this.currentPrimaryKey;
    }

    /** To set input box text
     *
     * @param text the new text of input box
     */
    public void setInputBoxText(String text) {
        this.inputBox.setText(text);
    }

    /** To set the input box text and send the message
     *
     * @param text the new text of input box
     */
    public void sendChatMessage(String text) {
        this.inputBox.setText(text);
        okAction();
    }

    /** To enable/disable a chatRoom
     *
     * @param primaryKey the ChatRoom primary key
     * @param value true to enable/false to disable
     */
    public void setEnabledAt(String primaryKey, boolean value) {
        for (int i = 0; i < this.tabbedPane.getTabCount(); i++) {
            if (this.tabbedPane.getComponentAt(i).getName().equals(primaryKey)) {
                this.tabbedPane.setEnabledAt(i, value);
                return;
            }
        }
    }

    /** To reset the state of the JChatPanel
     */
    public void reset() {
        this.tabbedPane.setEnabledAt(0, true);
        this.tabbedPane.setSelectedIndex(0);
        this.currentPrimaryKey = ChatRoom.DEFAULT_CHAT;
        this.b_createChatRoom.setEnabled(true);
        if (DataManager.SHOW_DEBUG) {
            System.out.println("TAB number:" + this.tabbedPane.getTabCount());
        }
        for (int i = this.tabbedPane.getTabCount() - 1; i >= 0; i--) {
            if (DataManager.SHOW_DEBUG) {
                System.out.println("" + this.tabbedPane.getComponentAt(i).getName());
            }
            if (!this.tabbedPane.getComponentAt(i).getName().equals(ChatRoom.DEFAULT_CHAT)) {
                this.tabbedPane.remove(i);
                if (DataManager.SHOW_DEBUG) {
                    System.out.println("tab removed");
                }
            } else {
                ((JChatRoom) this.tabbedPane.getComponentAt(i)).removeAllPlayers();
                if (DataManager.SHOW_DEBUG) {
                    System.out.println("DEFAULT CHAT player list reseted");
                }
            }
        }
    }

    /** To set the current ChatRoom.
     */
    public boolean setCurrentJChatRoom(String primaryKey) {
        boolean found = false;
        if (primaryKey.equals(ChatRoom.DEFAULT_CHAT)) {
            this.tabbedPane.setEnabledAt(0, true);
            this.tabbedPane.setSelectedIndex(0);
            this.currentPrimaryKey = primaryKey;
            found = true;
        } else {
            this.tabbedPane.setEnabledAt(0, false);
        }
        for (int i = 1; i < this.tabbedPane.getTabCount(); i++) {
            if (this.tabbedPane.getComponentAt(i).getName().equals(primaryKey)) {
                this.tabbedPane.setEnabledAt(i, true);
                this.tabbedPane.setSelectedIndex(i);
                this.currentPrimaryKey = primaryKey;
                found = true;
            } else {
                this.tabbedPane.setEnabledAt(i, false);
                JChatRoom jchatRoom = (JChatRoom) this.tabbedPane.getComponentAt(i);
                jchatRoom.removeAllPlayers();
            }
        }
        return found;
    }

    /** To add a JChatRoom.<br>
     * called by wotlas.client.message.chat.ChatRoomCreatedMessage
     */
    public JChatRoom addJChatRoom(ChatRoom chatRoom) {
        JChatRoom jchatRoom = new JChatRoom(chatRoom);
        if (DataManager.SHOW_DEBUG) {
            System.out.println("JChatRoom::addJChatRoom " + jchatRoom.getName() + " created !");
        }
        if (DataManager.SHOW_DEBUG) {
            System.out.println("\tcreatorPrimaryKey = " + chatRoom.getCreatorPrimaryKey());
        }
        this.tabbedPane.addTab(chatRoom.getName(), this.iconUp, jchatRoom, chatRoom.getName() + " channel");
        if (this.tabbedPane.getTabCount() >= ChatRoom.MAXIMUM_CHATROOMS_PER_ROOM) {
            this.b_createChatRoom.setEnabled(false);
        } else {
            this.b_createChatRoom.setEnabled(true);
        }
        return jchatRoom;
    }

    /** To remove a JChatRoom.
     *
     * @param primaryKey ChatRoom primary key
     */
    public boolean removeJChatRoom(String primaryKey) {
        for (int i = 1; i < this.tabbedPane.getTabCount(); i++) {
            if (this.tabbedPane.getComponentAt(i).getName().equals(primaryKey)) {
                if (DataManager.SHOW_DEBUG) {
                    System.out.println("removeChatRoom");
                }
                this.tabbedPane.remove(i);
                if (primaryKey.equals(this.currentPrimaryKey)) {
                    setCurrentJChatRoom(ChatRoom.DEFAULT_CHAT);
                }
                if (this.tabbedPane.getTabCount() >= ChatRoom.MAXIMUM_CHATROOMS_PER_ROOM) {
                    this.b_createChatRoom.setEnabled(false);
                } else {
                    this.b_createChatRoom.setEnabled(true);
                }
                return true;
            }
        }
        if (DataManager.SHOW_DEBUG) {
            System.out.println("ERROR : Couldn't removeJChatRoom");
        }
        return false;
    }

    /** To change the title of main JChatRoom associated to the room (first index)
     */
    public void changeMainJChatRoom(String roomName) {
        this.tabbedPane.setTitleAt(0, roomName);
        JChatRoom jchatRoom = (JChatRoom) this.tabbedPane.getComponentAt(0);
        jchatRoom.removeAllPlayers();
    }

    /** To get a JChatRoom.
     *
     * @param primaryKey primary key of JChatRoom we want to get
     */
    public JChatRoom getJChatRoom(String primaryKey) {
        for (int i = 0; i < this.tabbedPane.getTabCount(); i++) {
            if (this.tabbedPane.getComponentAt(i).getName().equals(primaryKey)) {
                if (DataManager.SHOW_DEBUG) {
                    System.out.println("getJChatRoom");
                }
                return (JChatRoom) this.tabbedPane.getComponentAt(i);
            }
        }
        if (DataManager.SHOW_DEBUG) {
            System.out.println("ERROR : Couldn't getJChatRoom");
        }
        return null;
    }

    /** To get current JChatRoom.
     */
    public JChatRoom getCurrentJChatRoom() {
        return getJChatRoom(this.currentPrimaryKey);
    }

    /** To add a player to a JChatRoom.
     *
     * @param primaryKey primary key of ChatRoom to modify
     * @param player player to add
     */
    public boolean addPlayer(String primaryKey, PlayerImpl player) {
        for (int i = 0; i < this.tabbedPane.getTabCount(); i++) {
            if (this.tabbedPane.getComponentAt(i).getName().equals(primaryKey)) {
                JChatRoom jchatRoom = (JChatRoom) this.tabbedPane.getComponentAt(i);
                jchatRoom.addPlayer(player.getPrimaryKey(), player.getFullPlayerName());
                return true;
            }
        }
        if (DataManager.SHOW_DEBUG) {
            System.out.println("ERROR : Couldn't addPlayer");
        }
        return false;
    }

    /** To remove a player from a ChatRoom.
     *
     * @param primaryKey primary key of ChatRoom to modify
     * @param player player to remove
     */
    public boolean removePlayer(String primaryKey, PlayerImpl player) {
        for (int i = 0; i < this.tabbedPane.getTabCount(); i++) {
            if (this.tabbedPane.getComponentAt(i).getName().equals(primaryKey)) {
                JChatRoom jchatRoom = (JChatRoom) this.tabbedPane.getComponentAt(i);
                jchatRoom.removePlayer(player.getPrimaryKey());
                return true;
            }
        }
        if (DataManager.SHOW_DEBUG) {
            System.out.println("ERROR : Couldn't removePlayer");
        }
        return false;
    }

    /** To remove a player from all the ChatRooms.
     *
     * @param primaryKey primary key of Player to remove
     */
    public void removePlayerFromAllchatRooms(String primaryKey) {
        for (int i = 0; i < this.tabbedPane.getTabCount(); i++) {
            JChatRoom jchatRoom = (JChatRoom) this.tabbedPane.getComponentAt(i);
            jchatRoom.removePlayer(primaryKey);
        }
    }

    /** To get the list of players of a ChatRoom
     *
     * @param primaryKey primary key of the ChatRoom
     */
    public Hashtable<String, PlayerState> getPlayers(String primaryKey) {
        for (int i = 0; i < this.tabbedPane.getTabCount(); i++) {
            if (this.tabbedPane.getComponentAt(i).getName().equals(primaryKey)) {
                JChatRoom jchatRoom = (JChatRoom) this.tabbedPane.getComponentAt(i);
                return jchatRoom.getPlayers();
            }
        }
        if (DataManager.SHOW_DEBUG) {
            System.out.println("ERROR : Couldn't get players");
        }
        return null;
    }

    /** action when the user wants to send a message
     */
    private void okAction() {
        String message;
        message = this.inputBox.getText();
        if (message.length() == 0) {
            return;
        }
        this.messageHistory.add(message);
        MacroPlugIn macroPlugIn = (MacroPlugIn) ClientDirector.getDataManager().getClientScreen().getPlayerPanel().getPlugIn("Macro");
        if (macroPlugIn != null) {
            message = macroPlugIn.processMacros(message);
        }
        if (message.length() > ChatRoom.MAXIMUM_MESSAGE_SIZE) {
            message = message.substring(0, ChatRoom.MAXIMUM_MESSAGE_SIZE - 4) + "...";
        }
        if (message.startsWith("/whisper")) {
            this.chatVoiceLevel.setValue(ChatRoom.WHISPERING_VOICE_LEVEL);
            message = message.substring(8);
            this.inputBox.setText(message);
            if (message.length() == 0) {
                return;
            }
        } else if (message.startsWith("/shout")) {
            this.chatVoiceLevel.setValue(ChatRoom.SHOUTING_VOICE_LEVEL);
            message = message.substring(6);
            this.inputBox.setText(message);
            if (message.length() == 0) {
                return;
            }
        }
        DataManager dManager = ClientDirector.getDataManager();
        dManager.sendMessage(new SendTextMessage(dManager.getMyPlayer().getPrimaryKey(), dManager.getMyPlayer().getPlayerName(), getMyCurrentChatPrimaryKey(), message, (byte) this.chatVoiceLevel.getValue()));
        this.inputBox.setText("");
        this.chatVoiceLevel.setValue(ChatRoom.NORMAL_VOICE_LEVEL);
    }

    /** To auto complete the name of a player
     */
    private String nameCompletion() {
        if (this.e == null) {
            this.input = this.inputBox.getText();
            if (this.input.length() == 0) {
                return this.input;
            }
            int lastIndex = this.input.lastIndexOf(' ');
            this.autoName = this.input.substring(lastIndex + 1, this.input.length());
            this.input = this.input.substring(0, lastIndex + 1);
            this.e = getPlayers(this.currentPrimaryKey).elements();
        }
        String playerKey;
        for (; this.e.hasMoreElements(); ) {
            playerKey = (this.e.nextElement()).fullName;
            if (playerKey.startsWith(this.autoName)) {
                return this.input + playerKey;
            }
        }
        this.e = getPlayers(this.currentPrimaryKey).elements();
        return this.input + this.autoName;
    }

    /** Called when an action is performed.
     */
    public void actionPerformed(ActionEvent ae) {
        String actionCommand = ae.getActionCommand();
        if (actionCommand == null) {
            return;
        }
        if (DataManager.SHOW_DEBUG) {
            System.out.println("Action command : " + actionCommand);
        }
        DataManager dataManager = ClientDirector.getDataManager();
        PlayerImpl myPlayer = dataManager.getMyPlayer();
        if (!myPlayer.getLocation().isRoom() && actionCommand.equals("createChatRoom")) {
            JOptionPane.showMessageDialog(null, "Sorry, but you can not create/leave chat channels\n" + "on World/Town Maps.", "INFORMATION", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (actionCommand.equals("createChatRoom")) {
            WotlasLocation chatRoomLocation = myPlayer.getLocation();
            String chatRoomName = JOptionPane.showInputDialog("Please enter a Name:");
            if (chatRoomName == null || chatRoomName.length() == 0) {
                return;
            }
            if (this.tabbedPane.getTabCount() >= ChatRoom.MAXIMUM_CHATROOMS_PER_ROOM - 1) {
                this.b_createChatRoom.setEnabled(false);
            } else {
                this.b_createChatRoom.setEnabled(true);
            }
            myPlayer.sendMessage(new ChatRoomCreationMessage(chatRoomName, myPlayer.getPrimaryKey()));
        } else if (actionCommand.equals("leaveChatRoom")) {
            if (!this.currentPrimaryKey.equals(ChatRoom.DEFAULT_CHAT)) {
                myPlayer.sendMessage(new RemPlayerFromChatRoomMessage(myPlayer.getPrimaryKey(), this.currentPrimaryKey));
            }
        } else if (actionCommand.equals("helpChat")) {
            DataManager dManager = ClientDirector.getDataManager();
            dManager.sendMessage(new SendTextMessage(dManager.getMyPlayer().getPrimaryKey(), dManager.getMyPlayer().getPlayerName(), getMyCurrentChatPrimaryKey(), "/help", ChatRoom.NORMAL_VOICE_LEVEL));
        } else if (actionCommand.equals("imageChat")) {
            String imageURL = JOptionPane.showInputDialog("Please enter your image's URL:\nExample: http://wotlas.sf.net/images/wotlas.gif");
            if (imageURL == null || imageURL.length() == 0) {
                return;
            }
            try {
                URL url = new URL(imageURL);
                URLConnection urlC = url.openConnection();
                urlC.connect();
                String ctype = urlC.getContentType();
                if (!ctype.startsWith("image/")) {
                    JOptionPane.showMessageDialog(null, "The specified URL does not refer to an image !", "Information", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                if (urlC.getContentLength() > 50 * 1024) {
                    JOptionPane.showMessageDialog(null, "The specified image is too big (above 50kB).", "Information", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            } catch (Exception ex) {
                Debug.signal(Debug.ERROR, this, "Failed to get image: " + ex);
                JOptionPane.showMessageDialog(null, "Failed to get the specified image...\nCheck your URL.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            DataManager dManager = ClientDirector.getDataManager();
            dManager.sendMessage(new SendTextMessage(dManager.getMyPlayer().getPrimaryKey(), dManager.getMyPlayer().getPlayerName(), getMyCurrentChatPrimaryKey(), "<img src='" + imageURL + "'>", ChatRoom.NORMAL_VOICE_LEVEL));
        } else {
            if (DataManager.SHOW_DEBUG) {
                System.out.println("Err : unknown actionCommand");
                System.out.println("No action command found!");
            }
        }
    }

    /** To update players lists of all chat rooms
     *
     * @param searchedPlayer player we want to update the state
     */
    public void updateAllChatRooms(Player searchedPlayer) {
        for (int i = 0; i < this.tabbedPane.getTabCount(); i++) {
            JChatRoom jchatRoom = (JChatRoom) this.tabbedPane.getComponentAt(i);
            jchatRoom.updatePlayer(searchedPlayer.getPrimaryKey(), searchedPlayer.getPlayerState().value);
        }
    }

    /**
     * Invoked when the mouse button is clicked
     */
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * Invoked when the mouse enters a component
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * Invoked when the mouse exits a component
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * Invoked when a mouse button has been pressed on a component
     */
    public void mousePressed(MouseEvent e) {
    }

    /**
     * Invoked when a mouse button has been released on a component
     */
    public void mouseReleased(MouseEvent e) {
    }
}

/**
 * Private class to prevent loss of focus of a JComponent
 */
class NoFocusInputVerifier extends InputVerifier {

    /** Checks whether the JComponent's input is valid.
     */
    @Override
    public boolean verify(JComponent input) {
        return false;
    }
}

/**
 * Private class to prevent loss of focus of a JComponent
 */
class NoFocusJTextField extends JTextField {

    /** Override this method and return true if your JComponent manages focus
     */
    @Override
    public boolean isManagingFocus() {
        return true;
    }
}
