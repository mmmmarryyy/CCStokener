package gpsxml.gui;

import gpsxml.Archive;
import gpsxml.TagTreeNode;
import gpsxml.TagTreeNodeInterface;
import gpsxml.io.InitDataSource;
import gpsxml.io.OtherServers;
import gpsxml.xml.Source;
import java.awt.event.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.log4j.Logger;

/**
 *  Allows users to subscribe to other servers.
 * @author mizumoto
 */
public class RegisterOtherServersFrame extends JInternalFrame {

    static Logger logger = Logger.getLogger(gpsxml.gui.RegisterOtherServersFrame.class.getName());

    private JButton okButton = new JButton("  OK  ");

    private JButton cancelButton = new JButton("Cancel");

    private JButton registerButton = new JButton("Register new server");

    private InitDataSource initDataSource;

    private MainWindow mainWindow;

    private RegisterNewServer newServerFrame;

    private ServerUsageFrame usageFrame;

    private JScrollPane parentServersScrolPane;

    private JList parentServersList;

    private String[] parentServersArray;

    private JButton deleteButton = new JButton("Delete");

    private JButton deleteAllButton = new JButton("Delete All");

    private JButton usageButton = new JButton("Usage");

    public RegisterOtherServersFrame(String title, RegisterNewServer newServerFrame, ServerUsageFrame serverUsageFrame, MainWindow mainWindow) {
        super(title, true, true, true, true);
        this.newServerFrame = newServerFrame;
        this.usageFrame = serverUsageFrame;
        this.mainWindow = mainWindow;
        initComponents();
        initListeners();
    }

    private void initComponents() {
        Box box = Box.createVerticalBox();
        JComponent parentServerPanel = getParenetServerPanel();
        JComponent buttonPanel = Box.createHorizontalBox();
        buttonPanel.add(Box.createHorizontalStrut(8));
        buttonPanel.add(okButton);
        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createHorizontalGlue());
        box.add(parentServerPanel);
        box.add(Box.createVerticalStrut(10));
        box.add(buttonPanel);
        box.add(Box.createVerticalStrut(5));
        add(box);
    }

    public void addOkButtonActionListener(ActionListener listner) {
        okButton.addActionListener(listner);
    }

    private void initListeners() {
        parentServersList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                int[] selectedIndexes = parentServersList.getSelectedIndices();
                for (int i = 0; i < selectedIndexes.length; i++) {
                    if (findServerDependency(parentServersArray[selectedIndexes[i]])) {
                        usageButton.setEnabled(true);
                        deleteButton.setEnabled(false);
                        deleteAllButton.setEnabled(false);
                    } else {
                        usageButton.setEnabled(false);
                        deleteButton.setEnabled(true);
                        deleteAllButton.setEnabled(true);
                    }
                }
            }
        });
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                newServerFrame.setVisible(false);
            }
        });
        deleteButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (parentServersList.getSelectedIndex() != -1 && initDataSource != null) {
                    int[] selectedIndexes = parentServersList.getSelectedIndices();
                    String selectedServerNames[] = new String[selectedIndexes.length];
                    for (int i = 0; i < selectedServerNames.length; i++) selectedServerNames[i] = parentServersArray[selectedIndexes[i]];
                    if (findServerDependency(selectedServerNames)) {
                        JOptionPane.showMessageDialog(((JButton) e.getSource()).getParent(), "Unable to delete Servers due to dependency");
                        return;
                    }
                    for (int i = 0; i < selectedIndexes.length; i++) parentServersArray[selectedIndexes[i]] = null;
                    Vector<String> vector = new Vector<String>();
                    for (int i = 0; i < parentServersArray.length; i++) {
                        if (parentServersArray[i] != null) vector.add(parentServersArray[i]);
                    }
                    parentServersArray = new String[vector.size()];
                    if (vector.size() > 0) {
                        for (int i = 0; i < vector.size(); i++) parentServersArray[i] = vector.elementAt(i);
                    }
                    refresh();
                }
            }
        });
        deleteAllButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (findServerDependency()) {
                    JOptionPane.showMessageDialog(((JButton) e.getSource()).getParent(), "Unable to delete Servers due to dependency");
                    return;
                }
                parentServersArray = new String[0];
                refresh();
            }
        });
        usageButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                HashMap<String, String> serverNamesMap = (new OtherServers(parentServersArray)).getParentsMap();
                usageFrame.setOtherServersName(serverNamesMap);
                usageFrame.add();
                usageFrame.setVisible(true);
            }
        });
        registerButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                newServerFrame.clean();
                newServerFrame.setVisible(true);
            }
        });
        newServerFrame.addRegisterButtonActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (newServerFrame.getText() != null && newServerFrame.getText() != "" && initDataSource != null) {
                    OtherServers otherServers = new OtherServers(parentServersArray);
                    HashMap<String, String> parentsMap = otherServers.getParentsMap();
                    if (parentsMap.containsKey(newServerFrame.getText())) {
                        DialogPane.showMessageDialog(RegisterOtherServersFrame.this, "There is already a subscription to this server", "Warning", JOptionPane.WARNING_MESSAGE);
                    } else if (initDataSource.getServerName().equals("http://" + newServerFrame.getText())) {
                        DialogPane.showMessageDialog(RegisterOtherServersFrame.this, "This is a local server!", "Warning", JOptionPane.WARNING_MESSAGE);
                    } else if (checkOmicBrowse(newServerFrame.getText())) {
                        newServerFrame.setVisible(false);
                        String[] newArray;
                        if (parentServersArray != null) {
                            newArray = new String[parentServersArray.length + 1];
                            for (int i = 0; i < parentServersArray.length; i++) newArray[i] = parentServersArray[i];
                        } else newArray = new String[1];
                        newArray[newArray.length - 1] = newServerFrame.getText();
                        parentServersArray = newArray;
                        refresh();
                        newServerFrame.clean();
                    }
                }
            }
        });
    }

    public void setInit(InitDataSource initDataSource) {
        this.initDataSource = initDataSource;
    }

    private JComponent getParenetServerPanel() {
        parentServersList = new JList();
        parentServersScrolPane = new JScrollPane(parentServersList);
        JComponent parentServerPanel = Box.createVerticalBox();
        parentServerPanel.setBorder(new TitledBorder("List of Servers"));
        JPanel newParentServerPanel = new JPanel();
        JComponent buttonBox = Box.createHorizontalBox();
        usageButton.setEnabled(false);
        deleteButton.setEnabled(false);
        deleteAllButton.setEnabled(false);
        buttonBox.add(deleteButton);
        buttonBox.add(deleteAllButton);
        buttonBox.add(usageButton);
        buttonBox.add(Box.createHorizontalGlue());
        parentServerPanel.add(Box.createVerticalStrut(10));
        parentServerPanel.add(parentServersScrolPane);
        parentServerPanel.add(Box.createVerticalStrut(10));
        parentServerPanel.add(buttonBox);
        return parentServerPanel;
    }

    private String[] getParentServers() {
        String[] parentServersArray = null;
        if (initDataSource != null) parentServersArray = initDataSource.getParentsServerName();
        return parentServersArray;
    }

    private String[] changeStringToUrl(String[] strings) {
        String[] newStrings = new String[strings.length];
        for (int i = 0; i < strings.length; i++) {
            newStrings[i] = "http://" + strings[i];
            try {
                URL url = new URL(newStrings[i]);
            } catch (MalformedURLException e) {
                newStrings[i] = strings[i];
            }
        }
        return newStrings;
    }

    private boolean checkOmicBrowse(String server) {
        URL url = null;
        HttpURLConnection connection;
        if (server.indexOf("/gps") == -1) server = server + "/gps";
        try {
            url = new URL(server);
        } catch (MalformedURLException e) {
            try {
                url = new URL("http://" + server);
            } catch (MalformedURLException me) {
                DialogPane.showMessageDialog(RegisterOtherServersFrame.this, "Invalid Server Name", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        try {
            if (url != null) {
                connection = (HttpURLConnection) url.openConnection();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    DialogPane.showMessageDialog(RegisterOtherServersFrame.this, "The server has been found successfully.", "Register a New Server", JOptionPane.INFORMATION_MESSAGE);
                    return true;
                }
            }
        } catch (IOException e) {
            logger.debug("The server was not found");
        }
        return DialogPane.showConfirmDialog(RegisterOtherServersFrame.this, "<html>The server was not found.<br>Do you really want to register this server?</html>", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public void update() {
        parentServersArray = getParentServers();
        if (parentServersArray == null) parentServersArray = new String[0];
        refresh();
    }

    public void updateInit() {
        if (initDataSource != null) {
            initDataSource.setHostProperty(initDataSource.getServerName(), initDataSource.getAliasName(), changeStringToUrl(parentServersArray));
        }
    }

    /**
      * finds the server dependencies of all the current servers
      *@return true if any one is dependent
      */
    private boolean findServerDependency() {
        return findServerDependency(parentServersArray);
    }

    /**
      * find server dependencies of specified server
      *@param String serverName The name of the server to check the dependency
      *@return true if the server has dependency with the DataSourceGroups; false otherwise
      */
    private boolean findServerDependency(String serverName) {
        Archive currentArchive = mainWindow.getArchiveHandler().getCurrentArchive();
        HashMap<String, String> serverNamesMap = (new OtherServers(parentServersArray)).getParentsMap();
        if (currentArchive == null) return false;
        Iterator<TagTreeNodeInterface> it = currentArchive.getArchiveData().getGroupMap().values().iterator();
        while (it.hasNext()) {
            List<TagTreeNodeInterface> sourceList = it.next().getChildList();
            for (TagTreeNodeInterface sources : sourceList) {
                String server = ((Source) sources.getTagInterface()).getServer();
                if ((server != null) && (!server.equals("")) && server.equalsIgnoreCase(serverNamesMap.get(serverName))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
      * find the dependecies of the array servers 
      *@param String[] serverNames The array of names of server to check the dependency
      *@return true if anyone has dependency
      *@note this method can be used also for getting all the dependent server names
      */
    private boolean findServerDependency(String serverNames[]) {
        String dependentServerNames = null;
        for (int i = 0; i < serverNames.length; i++) if (findServerDependency(serverNames[i])) dependentServerNames = serverNames[i] + ":";
        return dependentServerNames != null;
    }

    /**
      * find the dependecies of the array servers 
      *@param List<String> serverNames The java list object containing string representation of names of server to check the dependency
      *@return true if anyone has dependency
      */
    private boolean findServerDependency(List<String> serverNames) {
        return findServerDependency((String[]) serverNames.toArray());
    }

    private void refresh() {
        parentServersList.setListData(parentServersArray);
        parentServersList.repaint();
        parentServersScrolPane.repaint();
        usageButton.setEnabled(false);
        deleteButton.setEnabled(false);
        deleteAllButton.setEnabled(false);
    }
}
