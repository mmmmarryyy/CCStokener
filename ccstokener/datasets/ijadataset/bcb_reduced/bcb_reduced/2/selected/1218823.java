package net.etherstorm.jOpenRPG;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.net.*;
import java.io.*;
import java.util.*;
import net.etherstorm.jOpenRPG.utils.ExceptionHandler;
import net.etherstorm.jOpenRPG.event.*;
import net.etherstorm.jOpenRPG.actions.ConnectionAwareAction;
import net.etherstorm.jOpenRPG.actions.DefaultAction;
import net.etherstorm.jOpenRPG.commlib.CreateGroupMessage;

/**
 * 
 * 
 * @author $Author: tedberg $
 * @version $Revision: 352 $
 * $Date: 2002-02-01 02:32:11 -0500 (Fri, 01 Feb 2002) $
 */
public class JServerBrowserPanel extends JPanel implements ListSelectionListener {

    Vector servers;

    JTextField addressEdit;

    JList serverList;

    DefaultListModel servermodel;

    JProgressBar flakyFactor;

    JTable groupTable;

    GroupTableModel groupModel;

    JPasswordField roomPasswd;

    JPasswordField myRoomPasswd;

    JPasswordField kickRoomPasswd;

    JTextField roomName;

    JMenuBar mybar;

    ReferenceManager referenceManager = ReferenceManager.getInstance();

    SharedDataManager sdm = referenceManager.getSharedDataManager();

    /**
	 *
	 */
    public JServerBrowserPanel() {
        super(new BorderLayout());
        try {
            servers = new Vector();
            groupModel = new GroupTableModel();
            groupTable = new JTable(groupModel);
            servermodel = new DefaultListModel();
            addressEdit = new JTextField();
            serverList = new JList(servermodel);
            serverList.addListSelectionListener(this);
            flakyFactor = new JProgressBar();
            flakyFactor.setString("Reliability");
            flakyFactor.setMinimum(0);
            flakyFactor.setMaximum(5);
            flakyFactor.setStringPainted(true);
            roomPasswd = new JPasswordField();
            myRoomPasswd = new JPasswordField();
            kickRoomPasswd = new JPasswordField();
            roomName = new JTextField();
            roomName.setText(referenceManager.getPreference("default.room.name", ""));
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
            top.add(flakyFactor);
            top.add(Box.createVerticalStrut(8));
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

    /**
	 *
	 */
    public void fetchServerList() {
        try {
            serverList.clearSelection();
            servermodel.clear();
            servers.clear();
            String meta = referenceManager.getPreference("meta.server", "http://www.openrpg.com/openrpg_servers.php");
            URL url = new URL(meta + "?version=" + referenceManager.getSettingAttribute("client", "protocol", "version", "0.9.2"));
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(url.openStream());
            NodeList nl = doc.getElementsByTagName("server");
            for (int loop = 0; loop < nl.getLength(); loop++) {
                Element e = (Element) nl.item(loop);
                String name = e.getAttribute("name");
                String address = e.getAttribute("address");
                String reliability = e.getAttribute("failed_count");
                server_info info = new server_info(name, address, reliability);
                servers.add(info);
            }
            for (int loop = 0; loop < servers.size(); loop++) {
                servermodel.addElement((server_info) servers.get(loop));
            }
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 *
	 */
    public void valueChanged(ListSelectionEvent evt) {
        try {
            server_info server = (server_info) serverList.getSelectedValue();
            addressEdit.setText(server.getAddress());
            flakyFactor.setValue(5 - server.getCount());
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
            referenceManager.getCore().connectTo(addressEdit.getText());
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
            referenceManager.setPreference("default.room.name", roomName.getText());
            System.out.println(cgm);
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
    class server_info {

        String name;

        String address;

        int count;

        /**
		 *
		 */
        public server_info(String name, String address, String failed_count) {
            this.name = name;
            this.address = address;
            count = Integer.parseInt(failed_count);
        }

        /**
		 *
		 */
        public String getName() {
            return name;
        }

        /**
		 *
		 */
        public String getAddress() {
            return address;
        }

        /**
		 *
		 */
        public int getCount() {
            return count;
        }

        /**
		 *
		 */
        public String toString() {
            return name;
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
