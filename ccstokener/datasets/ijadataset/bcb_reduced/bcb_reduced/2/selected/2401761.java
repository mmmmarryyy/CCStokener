package net.etherstorm.jopenrpg.swing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import net.etherstorm.jopenrpg.ReferenceManager;
import net.etherstorm.jopenrpg.SharedDataManager;
import net.etherstorm.jopenrpg.event.ConnectionStateEvent;
import net.etherstorm.jopenrpg.event.GroupInfoEvent;
import net.etherstorm.jopenrpg.event.GroupInfoListener;
import net.etherstorm.jopenrpg.net.CreateGroupMessage;
import net.etherstorm.jopenrpg.swing.actions.ConnectionAwareAction;
import net.etherstorm.jopenrpg.swing.actions.DefaultAction;
import net.etherstorm.jopenrpg.swing.nodehandlers.PreferencesNode;
import net.etherstorm.jopenrpg.util.ExceptionHandler;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 * 
 * 
 * @author $Author: tedberg $
 * @version $Revision: 1.24 $
 * $Date: 2004/04/01 03:19:36 $
 */
public class JServerBrowserPanel extends JPanel implements ListSelectionListener {

    protected JTextField addressEdit;

    protected JTable serverList;

    protected ServerTableModel serverModel;

    protected JTable groupTable;

    protected GroupTableModel groupModel;

    protected JPasswordField roomPasswd;

    protected JPasswordField myRoomPasswd;

    protected JPasswordField kickRoomPasswd;

    protected JTextField roomName;

    protected JMenuBar mybar;

    protected ReferenceManager referenceManager = ReferenceManager.getInstance();

    protected SharedDataManager sdm = referenceManager.getSharedDataManager();

    protected static Logger logger = Logger.getLogger(JServerBrowserPanel.class);

    protected PreferencesNode prefs = new PreferencesNode();

    public void connect() {
        if (addressEdit.getText().trim().equals("")) {
            JOptionPane.showMessageDialog(JServerBrowserPanel.this, "You must indicate which server to connect to", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        referenceManager.getCore().connectTo(addressEdit.getText());
        referenceManager.getSharedDataManager().setServerName(getSelectedServerName());
    }

    /**
	 *
	 */
    public JServerBrowserPanel() {
        super(new BorderLayout());
        try {
            groupModel = new GroupTableModel();
            groupTable = new JTable(groupModel);
            serverModel = new ServerTableModel();
            addressEdit = new JTextField();
            addressEdit.addKeyListener(new KeyListener() {

                public void keyPressed(KeyEvent e) {
                }

                public void keyReleased(KeyEvent e) {
                }

                public void keyTyped(KeyEvent e) {
                    if (e.getKeyChar() == '\n') {
                        if (!referenceManager.getCore().isConnected()) connect();
                    }
                }
            });
            serverList = new JTable(serverModel);
            serverList.getSelectionModel().addListSelectionListener(this);
            serverList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            serverList.getColumn("Players").sizeWidthToFit();
            groupTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            roomPasswd = new JPasswordField();
            myRoomPasswd = new JPasswordField();
            kickRoomPasswd = new JPasswordField();
            roomName = new JTextField();
            roomName.setText(prefs.getDefaultRoomName());
            Box left = Box.createVerticalBox();
            Box top = Box.createVerticalBox();
            Box center = Box.createVerticalBox();
            Box b = Box.createHorizontalBox();
            JLabel l = new JLabel("Server address");
            l.setLabelFor(addressEdit);
            l.setDisplayedMnemonic('s');
            b.add(l);
            b.add(Box.createHorizontalStrut(8));
            b.add(addressEdit);
            top.add(b);
            l = new JLabel("Servers");
            l.setDisplayedMnemonic('v');
            l.setLabelFor(left);
            left.add(l);
            left.add(new JScrollPane(serverList));
            l = new JLabel("Rooms");
            l.setDisplayedMnemonic('o');
            l.setLabelFor(groupTable);
            center.add(l);
            center.add(new JScrollPane(groupTable));
            JPanel p = new JPanel(new BorderLayout());
            p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Room Info"));
            b = Box.createVerticalBox();
            Box bb = Box.createHorizontalBox();
            l = new JLabel("Room Name");
            l.setDisplayedMnemonic('r');
            l.setLabelFor(roomName);
            bb.add(l);
            bb.add(Box.createHorizontalStrut(8));
            bb.add(roomName);
            b.add(bb);
            bb = Box.createHorizontalBox();
            l = new JLabel("Room Password");
            l.setDisplayedMnemonic('a');
            l.setLabelFor(myRoomPasswd);
            bb.add(l);
            bb.add(Box.createHorizontalStrut(8));
            bb.add(myRoomPasswd);
            b.add(bb);
            bb = Box.createHorizontalBox();
            l = new JLabel("Kick Password");
            l.setDisplayedMnemonic('k');
            l.setLabelFor(kickRoomPasswd);
            bb.add(l);
            bb.add(Box.createHorizontalStrut(8));
            bb.add(kickRoomPasswd);
            b.add(bb);
            p.add(b);
            center.add(p);
            JPanel buttons = new JPanel();
            buttons.add(new JButton(new RefreshServersAction()));
            buttons.add(new JButton(new ConnectAction()));
            buttons.add(new JButton(new JoinRoomAction()));
            buttons.add(new JButton(new CreateRoomAction()));
            buttons.add(new JButton(new DisconnectAction()));
            add(buttons, BorderLayout.SOUTH);
            add(top, BorderLayout.NORTH);
            JSplitPane sp = new JSplitPane();
            sp.setLeftComponent(left);
            sp.setRightComponent(center);
            sp.setDividerLocation(200);
            sp.setOneTouchExpandable(true);
            add(sp);
            mybar = new JMenuBar();
            JMenu menu = new JMenu("Server Commands");
            menu.add(new RefreshServersAction());
            menu.addSeparator();
            menu.add(new ConnectAction());
            menu.add(new DisconnectAction());
            menu.addSeparator();
            menu.add(new JoinRoomAction());
            menu.add(new CreateRoomAction());
            mybar.add(menu);
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 *
	 */
    public JMenuBar getJMenuBar() {
        return mybar;
    }

    public String getSelectedServerName() {
        try {
            return serverModel.getServer(serverList.getSelectedRow()).getName();
        } catch (Exception ex) {
        }
        return "<Err>";
    }

    /**
	 *
	 */
    public void fetchServerList() {
        try {
            serverList.clearSelection();
            serverModel.clear();
            class ReportThread implements Runnable {

                Document doc;

                public ReportThread(Document d) {
                    doc = d;
                }

                public void run() {
                    Iterator iter = doc.getRootElement().getChildren("server").iterator();
                    while (iter.hasNext()) {
                        Element e = (Element) iter.next();
                        server_info info = new server_info(e.getAttributeValue("name"), e.getAttributeValue("address"), e.getAttributeValue("failed_count"), e.getAttributeValue("port"));
                        info.setPlayerCount(e.getAttributeValue("num_users"));
                        serverModel.addServer(info);
                    }
                }
            }
            class QueryThread implements Runnable {

                public void run() {
                    try {
                        String meta = prefs.getMetaServer().toString();
                        URL url = new URL(meta + "?version=" + referenceManager.getSettingAttribute("client", "protocol", "version", "0.9.2"));
                        try {
                            SAXBuilder sax = new SAXBuilder();
                            Document doc = sax.build(url.openStream());
                            SwingUtilities.invokeLater(new ReportThread(doc));
                            XMLOutputter xout = new XMLOutputter();
                            xout.setNewlines(true);
                            xout.setIndent("	");
                            xout.setTextNormalize(true);
                            System.out.println(xout.outputString(doc));
                            logger.debug(xout.outputString(doc));
                        } catch (Exception ex) {
                            ExceptionHandler.handleException(ex);
                        }
                    } catch (Exception ex) {
                    }
                }
            }
            Thread t = new Thread(new QueryThread());
            t.start();
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 *
	 */
    public void valueChanged(ListSelectionEvent evt) {
        try {
            server_info server = serverModel.getServer(serverList.getSelectedRow());
            addressEdit.setText(server.getServer());
        } catch (NullPointerException npe) {
        }
    }

    /**
	 *
	 */
    class RefreshServersAction extends DefaultAction {

        /**
		 *
		 */
        public RefreshServersAction() {
            super();
            initProperties(getClass().getName());
        }

        /**
		 *
		 */
        public void actionPerformed(ActionEvent evt) {
            super.actionPerformed(evt);
            fetchServerList();
        }
    }

    /**
	 *
	 */
    class ConnectAction extends ConnectionAwareAction {

        /**
		 *
		 */
        public ConnectAction() {
            super();
            initProperties(getClass().getName());
            setEnabled(!referenceManager.getCore().isConnected());
        }

        /**
		 *
		 */
        public void actionPerformed(ActionEvent evt) {
            super.actionPerformed(evt);
            connect();
        }

        /**
		 *
		 */
        public void connectionStateChanged(ConnectionStateEvent evt) {
            super.connectionStateChanged(evt);
            setEnabled(!referenceManager.getCore().isConnected());
        }
    }

    /**
	 *
	 */
    class DisconnectAction extends ConnectionAwareAction {

        /**
		 *
		 */
        public DisconnectAction() {
            super();
            initProperties(getClass().getName());
            setEnabled(referenceManager.getCore().isConnected());
        }

        /**
		 *
		 */
        public void actionPerformed(ActionEvent evt) {
            super.actionPerformed(evt);
            referenceManager.getCore().disconnect();
        }

        /**
		 *
		 */
        public void connectionStateChanged(ConnectionStateEvent evt) {
            super.connectionStateChanged(evt);
            setEnabled(referenceManager.getCore().isConnected());
        }
    }

    /**
	 *
	 */
    class JoinRoomAction extends ConnectionAwareAction {

        /**
		 *
		 */
        public JoinRoomAction() {
            super();
            initProperties(getClass().getName());
            setEnabled(referenceManager.getCore().isConnected());
        }

        /**
		 *
		 */
        public void actionPerformed(ActionEvent evt) {
            super.actionPerformed(evt);
            int index = groupTable.getSelectedRow();
            if (index == -1) {
                JOptionPane.showMessageDialog(JServerBrowserPanel.this, "You must select a room to join", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String id = groupModel.getGroupID(index);
            String passwd = "";
            if (sdm.isGroupPassworded(id)) {
                passwd = JOptionPane.showInputDialog(referenceManager.getMainFrame().desktop, "Please enter the room's password");
                passwd = (passwd == null) ? "" : passwd;
            }
            referenceManager.getCore().joinGroup(id, passwd);
        }

        /**
		 *
		 */
        public void connectionStateChanged(ConnectionStateEvent evt) {
            super.connectionStateChanged(evt);
            setEnabled(referenceManager.getCore().isConnected());
        }
    }

    /**
	 *
	 */
    class CreateRoomAction extends ConnectionAwareAction {

        /**
		 *
		 */
        public CreateRoomAction() {
            super();
            initProperties(getClass().getName());
            setEnabled(referenceManager.getCore().isConnected());
        }

        /**
		 *
		 */
        public void actionPerformed(ActionEvent evt) {
            super.actionPerformed(evt);
            CreateGroupMessage cgm = new CreateGroupMessage();
            cgm.setName(roomName.getText());
            cgm.setPwd(new String(myRoomPasswd.getPassword()));
            cgm.setBootPwd(new String(kickRoomPasswd.getPassword()));
            prefs.setDefaultRoomName(roomName.getText());
            logger.debug(cgm);
            cgm.send();
        }

        /**
		 *
		 */
        public void connectionStateChanged(ConnectionStateEvent evt) {
            super.connectionStateChanged(evt);
            setEnabled(referenceManager.getCore().isConnected());
        }
    }

    /**
	 *
	 */
    class GroupTableModel implements TableModel, GroupInfoListener {

        Vector listeners;

        /**
		 *
		 */
        public GroupTableModel() {
            listeners = new Vector();
            groupInfoChanged(null);
            sdm.addGroupInfoListener(this);
        }

        /**
		 *
		 */
        public String getGroupID(int index) {
            return sdm.getGroupIdAt(index);
        }

        /**
		 *
		 */
        public void groupInfoChanged(GroupInfoEvent evt) {
            fireTableChanged();
        }

        /**
		 *
		 */
        public void fireTableChanged() {
            TableModelEvent evt = new TableModelEvent(this);
            Iterator iter = listeners.iterator();
            while (iter.hasNext()) ((TableModelListener) iter.next()).tableChanged(evt);
        }

        /**
		 *
		 */
        public void addTableModelListener(TableModelListener l) {
            listeners.add(l);
        }

        /**
		 *
		 */
        public Class getColumnClass(int columnIndex) {
            return String.class;
        }

        /**
		 *
		 */
        public int getColumnCount() {
            return 3;
        }

        /**
		 *
		 */
        public String getColumnName(int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return "Room Name";
                case 1:
                    return "Player Count";
                case 2:
                    return "Private";
                default:
                    return "<error in JServerBrowserPanel.GroupTableModel>";
            }
        }

        /**
		 *
		 */
        public int getRowCount() {
            return sdm.getGroupCount();
        }

        /**
		 *
		 */
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return sdm.getGroupNameAt(rowIndex);
                case 1:
                    return sdm.getGroupPlayerCountAt(rowIndex);
                case 2:
                    return (sdm.getGroupIsPasswordedAt(rowIndex) ? "yes" : "no");
                default:
                    return "<error in JServerBrowserPanel.GroupTableModel>";
            }
        }

        /**
		 *
		 */
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        /**
		 *
		 */
        public void removeTableModelListener(TableModelListener l) {
            listeners.remove(l);
        }

        /**
		 *
		 */
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }
    }
}

class ServerTableModel implements TableModel {

    public ServerTableModel() {
    }

    protected ArrayList _servers;

    public void setServers(ArrayList val) {
        _servers = val;
    }

    public ArrayList getServers() {
        if (_servers == null) setServers(new ArrayList());
        return _servers;
    }

    public void clear() {
        getServers().clear();
        fireTableChanged(new TableModelEvent(this));
    }

    public void addServer(server_info foo) {
        getServers().add(foo);
        fireTableChanged(new TableModelEvent(this));
    }

    protected ArrayList _listeners;

    public void setListeners(ArrayList val) {
        _listeners = val;
    }

    public ArrayList getListeners() {
        if (_listeners == null) setListeners(new ArrayList());
        return _listeners;
    }

    protected server_info getServer(int index) {
        try {
            return (server_info) getServers().get(index);
        } catch (IndexOutOfBoundsException ioobe) {
            return null;
        }
    }

    public void addTableModelListener(TableModelListener l) {
        getListeners().add(l);
    }

    public Class getColumnClass(int columnIndex) {
        switch(columnIndex) {
            case 0:
                return String.class;
            case 1:
                return String.class;
            default:
                return String.class;
        }
    }

    public int getColumnCount() {
        return 2;
    }

    public String getColumnName(int columnIndex) {
        switch(columnIndex) {
            case 0:
                return "Server Name";
            case 1:
                return "Players";
            default:
                return "<err>";
        }
    }

    public int getRowCount() {
        return getServers().size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        switch(columnIndex) {
            case 0:
                return getServer(rowIndex).getName();
            case 1:
                return getServer(rowIndex).getPlayerCount();
            default:
                return "<err>";
        }
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void removeTableModelListener(TableModelListener l) {
        getListeners().remove(l);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }

    protected void fireTableChanged(TableModelEvent tme) {
        Iterator iter = getListeners().iterator();
        while (iter.hasNext()) {
            ((TableModelListener) iter.next()).tableChanged(tme);
        }
    }
}

/**
 *
 */
class server_info {

    /**
	 *
	 */
    public server_info(String name, String address, String failed_count, String port) {
        setName(name);
        setAddress(address);
        try {
            setCount(Integer.parseInt(failed_count));
        } catch (NumberFormatException nfe) {
            setCount(0);
        }
        try {
            setPort(Integer.parseInt(port));
        } catch (NumberFormatException nfe) {
            setPort(0);
        }
    }

    public String toString() {
        return getName();
    }

    protected String _name;

    public static final String PROPERTY_NAME = "name";

    public void setName(String val) {
        try {
            if (val.equals(_name)) return;
        } catch (Exception ex) {
            return;
        }
        String oldval = _name;
        _name = val;
    }

    public String getName() {
        if (_name == null) setName(new String());
        return _name;
    }

    protected String _address;

    public static final String PROPERTY_ADDRESS = "address";

    public void setAddress(String val) {
        try {
            if (val.equals(_address)) return;
        } catch (Exception ex) {
            return;
        }
        String oldval = _address;
        _address = val;
    }

    public String getAddress() {
        if (_address == null) setAddress(new String());
        return _address;
    }

    protected int _count;

    public static final String PROPERTY_COUNT = "count";

    public void setCount(int val) {
        if (val == _count) return;
        int oldval = _count;
        _count = val;
    }

    public int getCount() {
        return _count;
    }

    protected int _port;

    public static final String PROPERTY_PORT = "port";

    public void setPort(int val) {
        if (val == _port) return;
        int oldval = _port;
        _port = val;
    }

    public int getPort() {
        return _port;
    }

    protected String _playerCount;

    public static final String PROPERTY_PLAYERCOUNT = "playerCount";

    public void setPlayerCount(String val) {
        try {
            if (val.equals(_playerCount)) return;
        } catch (Exception ex) {
            return;
        }
        String oldval = _playerCount;
        _playerCount = val;
    }

    public String getPlayerCount() {
        if (_playerCount == null) setPlayerCount(new String());
        return _playerCount;
    }

    public String getServer() {
        return getAddress() + ":" + String.valueOf(getPort());
    }
}
