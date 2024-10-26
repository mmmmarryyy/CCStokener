package gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;
import java.util.StringTokenizer;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import commonResources.Database;
import commonResources.GlobalResources;
import commonResources.LangCenter;
import commonResources.MenuPopup;

/**
 * ����� ������ ��� ������ �������, � ������� ����� ����������� ����������,
 * ��������� � �������� ���� ������. ������ ������ ���������� �� �������: PanelSearch, PanelTestcase
 * 
 * @author Igor Starodubtsev
 */
public class DialogEditTestcase extends JDialog implements ActionListener, ItemListener, MouseListener {

    private static final long serialVersionUID = 1L;

    private StringTokenizer st;

    private int selectID;

    private String resourceCall = null;

    private JPanel jContentPane = null;

    private JLabel l_TestcaseName = null;

    private JLabel l_TestcaseKeyword = null;

    private JLabel l_TestcaseInformation = null;

    private JLabel l_TestcaseCIU = null;

    private JLabel l_Image1 = null;

    private JLabel l_Image2 = null;

    private JLabel l_Image3 = null;

    private JLabel l_TestcaseTestBody = null;

    private JLabel l_TestcaseOptionalTest = null;

    private JLabel l_Status = null;

    private JTextField tf_TestcaseName = null;

    private JTextField tf_TestcaseKeyword = null;

    private JTextField tf_Image1 = null;

    private JTextField tf_Image2 = null;

    private JTextField tf_Image3 = null;

    private JTextPane tp_TestcaseInformation = null;

    private JTextPane tp_TestcaseCIU = null;

    private JTextPane tp_TestcaseTestBody = null;

    private JTextPane tp_TestcaseOptionalTest = null;

    private JScrollPane scrollTestBody = null;

    private JScrollPane scrollOptionalTest = null;

    private JCheckBox chb_EnableOptionalTest = null;

    private JButton b_AddKeyword = null;

    private JButton b_Edit = null;

    private JButton b_AddImage = null;

    private JButton b_Ok = null;

    private JButton b_Cancel = null;

    String url[] = { "", "", "" };

    JDialog dialogLoadPicture = null;

    String query = "";

    /**
	 * ����������� ������. ������ ���������, ������ � ��������� �������. ������ ��������� ����.
	 * ��������� ��������� �������� ���� ������������.
	 * 
	 * @param ID ������������� ���� �����. ���� ���� ���� �����������, �� ID=-1,
	 * ���� �� �� ��������������� ��� ����������, �� ID=����������� ���� ����� � �������.
	 * @param sourcer ��� �������: ���������� ("Add"), ��������� ("Edit") ��� �������� ("View").
	 * @see gui.PanelTestcases
	 * @see gui.PanelSearch
	 */
    public DialogEditTestcase(int ID, String sourcer) {
        selectID = ID;
        resourceCall = sourcer;
        if (resourceCall.equals("Edit")) {
            Database.executed_query("UPDATE testcase " + "SET Locked='0' " + "WHERE ID='" + selectID + "';", "short");
        }
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setIconImage(((ImageIcon) GlobalResources.main_icon_little_icon).getImage());
        this.setModal(true);
        this.setSize(600, 750);
        this.setLocationByPlatform(true);
        this.setResizable(false);
        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent we) {
                if (resourceCall.equals("Edit")) {
                    Database.executed_query("UPDATE testcase " + "SET Locked='0' " + "WHERE ID='" + selectID + "';", "short");
                }
                dispose();
            }
        });
        this.setContentPane(getJContentPane());
    }

    /**
	 * � ������ �����������, ����������� � ����������� ���������� ����������� ����.
	 * ����� ��������� ��������� �������.
	 * 
	 * @return	���������� ������, �� ������� ����������� ����������� ����������.
	 */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(null);
            l_TestcaseName = new JLabel(LangCenter.getString("Title") + ": ");
            l_TestcaseName.setBounds(new Rectangle(10, 10, 80, 20));
            jContentPane.add(l_TestcaseName, null);
            tf_TestcaseName = new JTextField();
            tf_TestcaseName.setBounds(new Rectangle(110, 10, 450, 20));
            new MenuPopup(tf_TestcaseName);
            tf_TestcaseName.addMouseListener(new commonResources.PopupListener(MenuPopup.popup));
            jContentPane.add(tf_TestcaseName, null);
            l_TestcaseKeyword = new JLabel(LangCenter.getString("Keywords"));
            l_TestcaseKeyword.setBounds(new Rectangle(10, 40, 100, 20));
            jContentPane.add(l_TestcaseKeyword, null);
            tf_TestcaseKeyword = new JTextField();
            tf_TestcaseKeyword.setEditable(false);
            tf_TestcaseKeyword.setBounds(new Rectangle(110, 40, 450, 20));
            new MenuPopup(tf_TestcaseKeyword);
            tf_TestcaseKeyword.addMouseListener(new commonResources.PopupListener(MenuPopup.popup));
            jContentPane.add(tf_TestcaseKeyword, null);
            b_AddKeyword = new JButton(GlobalResources.add_icon);
            b_AddKeyword.setFocusPainted(false);
            b_AddKeyword.setVerticalTextPosition(AbstractButton.CENTER);
            b_AddKeyword.setHorizontalTextPosition(AbstractButton.CENTER);
            b_AddKeyword.setMargin(new Insets(0, 0, 0, 0));
            b_AddKeyword.setActionCommand("AddKeyword");
            b_AddKeyword.setBounds(new Rectangle(560, 40, 20, 20));
            b_AddKeyword.addActionListener(this);
            jContentPane.add(b_AddKeyword, null);
            l_TestcaseInformation = new JLabel(LangCenter.getString("Information") + ": ");
            l_TestcaseInformation.setBounds(new Rectangle(10, 70, 80, 20));
            jContentPane.add(l_TestcaseInformation, null);
            tp_TestcaseInformation = new JTextPane();
            new MenuPopup(tp_TestcaseInformation);
            tp_TestcaseInformation.addMouseListener(new commonResources.PopupListener(MenuPopup.popup));
            JScrollPane scrollInformation = new JScrollPane(tp_TestcaseInformation);
            scrollInformation.setBounds(new Rectangle(10, 90, 280, 100));
            jContentPane.add(scrollInformation, null);
            l_TestcaseCIU = new JLabel(LangCenter.getString("ConfigurationInUse") + ": ");
            l_TestcaseCIU.setBounds(new Rectangle(300, 70, 170, 20));
            jContentPane.add(l_TestcaseCIU, null);
            tp_TestcaseCIU = new JTextPane();
            new MenuPopup(tp_TestcaseCIU);
            tp_TestcaseCIU.addMouseListener(new commonResources.PopupListener(MenuPopup.popup));
            JScrollPane scrollCIU = new JScrollPane(tp_TestcaseCIU);
            scrollCIU.setBounds(new Rectangle(300, 90, 280, 100));
            jContentPane.add(scrollCIU, null);
            l_Image1 = new JLabel();
            jContentPane.add(l_Image1, null);
            tf_Image1 = new JTextField();
            tf_Image1.setBounds(new Rectangle(110, 200, 450, 20));
            new MenuPopup(tf_Image1);
            tf_Image1.addMouseListener(new commonResources.PopupListener(MenuPopup.popup));
            jContentPane.add(tf_Image1, null);
            b_AddImage = new JButton(GlobalResources.add_icon);
            b_AddImage.setFocusPainted(false);
            b_AddImage.setVerticalTextPosition(AbstractButton.CENTER);
            b_AddImage.setHorizontalTextPosition(AbstractButton.CENTER);
            b_AddImage.setActionCommand("Add_image");
            b_AddImage.setToolTipText(LangCenter.getString("AddNewImage"));
            b_AddImage.setBounds(new Rectangle(560, 200, 20, 20));
            b_AddImage.addActionListener(this);
            jContentPane.add(b_AddImage, null);
            l_Image2 = new JLabel();
            jContentPane.add(l_Image2, null);
            tf_Image2 = new JTextField();
            tf_Image2.setBounds(new Rectangle(110, 220, 450, 20));
            new MenuPopup(tf_Image2);
            tf_Image2.addMouseListener(new commonResources.PopupListener(MenuPopup.popup));
            jContentPane.add(tf_Image2, null);
            l_Image3 = new JLabel();
            jContentPane.add(l_Image3, null);
            tf_Image3 = new JTextField();
            tf_Image3.setBounds(new Rectangle(110, 240, 450, 20));
            new MenuPopup(tf_Image3);
            tf_Image3.addMouseListener(new commonResources.PopupListener(MenuPopup.popup));
            jContentPane.add(tf_Image3, null);
            l_TestcaseTestBody = new JLabel(LangCenter.getString("TestBody") + ": ");
            l_TestcaseTestBody.setBounds(new Rectangle(10, 260, 200, 40));
            jContentPane.add(l_TestcaseTestBody, null);
            tp_TestcaseTestBody = new JTextPane();
            new MenuPopup(tp_TestcaseTestBody);
            tp_TestcaseTestBody.addMouseListener(new commonResources.PopupListener(MenuPopup.popup));
            scrollTestBody = new JScrollPane(tp_TestcaseTestBody);
            scrollTestBody.setBounds(new Rectangle(10, 290, 570, 270));
            jContentPane.add(scrollTestBody, null);
            l_TestcaseOptionalTest = new JLabel(LangCenter.getString("OptionalTest") + ": ");
            l_TestcaseOptionalTest.setBounds(new Rectangle(10, 570, 200, 20));
            jContentPane.add(l_TestcaseOptionalTest, null);
            tp_TestcaseOptionalTest = new JTextPane();
            new MenuPopup(tp_TestcaseOptionalTest);
            tp_TestcaseOptionalTest.addMouseListener(new commonResources.PopupListener(MenuPopup.popup));
            scrollOptionalTest = new JScrollPane(tp_TestcaseOptionalTest);
            scrollOptionalTest.setBounds(new Rectangle(10, 590, 570, 80));
            jContentPane.add(scrollOptionalTest, null);
            chb_EnableOptionalTest = new JCheckBox(LangCenter.getString("EnableOptionalTest"), null, false);
            chb_EnableOptionalTest.setBounds(new Rectangle(10, 690, 200, 20));
            chb_EnableOptionalTest.addItemListener(this);
            jContentPane.add(chb_EnableOptionalTest, null);
            b_Ok = new JButton(LangCenter.getString("Ok"));
            b_Ok.setActionCommand("Ok");
            b_Ok.setBounds(new Rectangle(400, 690, 70, 20));
            jContentPane.add(b_Ok, null);
            b_Ok.addActionListener(this);
            b_Cancel = new JButton(LangCenter.getString("Cancel"));
            b_Cancel.setActionCommand("Cancel");
            b_Cancel.setBounds(new Rectangle(490, 690, 70, 20));
            jContentPane.add(b_Cancel, null);
            b_Cancel.addActionListener(this);
            b_Edit = new JButton(LangCenter.getString("Edit"));
            b_Edit.setActionCommand("Edit");
            b_Edit.setVisible(false);
            b_Edit.setBounds(new Rectangle(490, 70, 90, 20));
            b_Edit.addActionListener(this);
            jContentPane.add(b_Edit, null);
            l_Status = new JLabel();
            l_Status.setBounds(new Rectangle(20, 690, 150, 20));
            jContentPane.add(l_Status, null);
            update_field(resourceCall);
        }
        return jContentPane;
    }

    private void update_field(String action) {
        query = "";
        this.setTitle(LangCenter.getString(action) + " " + LangCenter.getString("Testcase"));
        if (action.equals("Add")) {
            tf_TestcaseName.setBackground(Color.WHITE);
            tf_TestcaseName.setEditable(true);
            tf_TestcaseKeyword.setText("");
            tf_TestcaseKeyword.setBackground(new Color(230, 230, 230));
            b_AddKeyword.setVisible(true);
            tp_TestcaseInformation.setText("");
            tp_TestcaseInformation.setBackground(Color.WHITE);
            tp_TestcaseInformation.setEditable(true);
            tp_TestcaseInformation.setCaretPosition(0);
            tp_TestcaseCIU.setText("");
            tp_TestcaseCIU.setBackground(Color.WHITE);
            tp_TestcaseCIU.setEditable(true);
            tp_TestcaseCIU.setCaretPosition(0);
            l_Image1.setText(LangCenter.getString("Image") + " 1:");
            l_Image1.setIcon(null);
            l_Image1.setBorder(null);
            l_Image1.setForeground(Color.BLACK);
            l_Image1.setVisible(true);
            l_Image1.setBounds(new Rectangle(10, 200, 100, 20));
            tf_Image1.setVisible(true);
            tf_Image1.setText("");
            l_Image2.setText(LangCenter.getString("Image") + " 2:");
            l_Image2.setIcon(null);
            l_Image2.setBorder(null);
            l_Image2.setForeground(Color.BLACK);
            l_Image2.setVisible(false);
            l_Image2.setBounds(new Rectangle(10, 220, 100, 20));
            tf_Image2.setVisible(false);
            tf_Image2.setText("");
            l_Image3.setText(LangCenter.getString("Image") + " 3:");
            l_Image3.setIcon(null);
            l_Image3.setBorder(null);
            l_Image3.setForeground(Color.BLACK);
            l_Image3.setVisible(false);
            l_Image3.setBounds(new Rectangle(10, 240, 100, 20));
            tf_Image3.setVisible(false);
            tf_Image3.setText("");
            b_AddImage.setVisible(true);
            tp_TestcaseTestBody.setBackground(Color.WHITE);
            tp_TestcaseTestBody.setEditable(true);
            tp_TestcaseTestBody.setText("");
            scrollTestBody.setBounds(new Rectangle(10, 290, 570, 270));
            tp_TestcaseTestBody.setCaretPosition(0);
            l_TestcaseOptionalTest.setVisible(false);
            tp_TestcaseOptionalTest.setEnabled(false);
            scrollOptionalTest.setVisible(false);
            chb_EnableOptionalTest.setVisible(true);
            tp_TestcaseOptionalTest.setBackground(Color.WHITE);
            tp_TestcaseOptionalTest.setCaretPosition(0);
            b_Edit.setVisible(false);
            l_Status.setVisible(false);
        } else if (action.equals("View")) {
            query = Database.executed_query("SELECT Name " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short");
            tf_TestcaseName.setText(query);
            tf_TestcaseName.setBackground(new Color(240, 240, 240));
            tf_TestcaseName.setEditable(false);
            query = Database.executed_query("SELECT Keyword " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short");
            String keywords = "";
            st = new StringTokenizer(query, ";");
            while (st.countTokens() != 0) {
                keywords += Database.executed_query("SELECT Name " + "FROM keywords " + "WHERE ID='" + st.nextToken() + "';", "short") + "; ";
            }
            tf_TestcaseKeyword.setText(keywords);
            tf_TestcaseKeyword.setBackground(new Color(200, 255, 200));
            b_AddKeyword.setVisible(false);
            query = Database.executed_query("SELECT Information " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short");
            tp_TestcaseInformation.setText(query);
            tp_TestcaseInformation.setBackground(new Color(240, 240, 240));
            tp_TestcaseInformation.setEditable(false);
            tp_TestcaseInformation.setCaretPosition(0);
            query = Database.executed_query("SELECT Configuration_in_use " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short");
            tp_TestcaseCIU.setText(query);
            tp_TestcaseCIU.setBackground(new Color(240, 240, 240));
            tp_TestcaseCIU.setEditable(false);
            tp_TestcaseCIU.setCaretPosition(0);
            l_Image1.setText("");
            l_Image1.setForeground(Color.BLACK);
            tf_Image1.setVisible(false);
            l_Image2.setText("");
            l_Image2.setForeground(Color.BLACK);
            tf_Image2.setVisible(false);
            l_Image3.setText("");
            l_Image3.setForeground(Color.BLACK);
            tf_Image3.setVisible(false);
            b_AddImage.setVisible(false);
            query = Database.executed_query("SELECT Image " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short");
            st = new StringTokenizer(query, "|");
            int count = st.countTokens();
            for (int i = 0; i < count; i++) {
                url[i] = st.nextToken();
            }
            if (!url[0].equals("")) {
                try {
                    (new URL(url[0])).openConnection().getContent();
                    l_Image1.setIcon(GlobalResources.CreateLittleImage(url[0], l_Image1, "1"));
                    l_Image1.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                    l_Image1.addMouseListener(this);
                } catch (IOException e) {
                    l_Image1.setText(" " + LangCenter.getString("Error"));
                    l_Image1.setBorder(BorderFactory.createLineBorder(Color.RED));
                    l_Image1.setForeground(Color.RED);
                }
                l_Image1.setToolTipText(url[0]);
                l_Image1.setName("Image1");
                l_Image1.setBounds(new Rectangle(10, 200, 70, 70));
            }
            if (!url[1].equals("")) {
                try {
                    (new URL(url[1])).openConnection().getContent();
                    l_Image2.setIcon(GlobalResources.CreateLittleImage(url[1], l_Image2, "2"));
                    l_Image2.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                    l_Image2.addMouseListener(this);
                } catch (IOException e) {
                    l_Image2.setText(" " + LangCenter.getString("Error"));
                    l_Image2.setBorder(BorderFactory.createLineBorder(Color.RED));
                    l_Image2.setForeground(Color.RED);
                }
                l_Image2.setToolTipText(url[1]);
                l_Image2.setName("Image2");
                l_Image2.setBounds(new Rectangle(110, 200, 70, 70));
            }
            if (!url[2].equals("")) {
                try {
                    (new URL(url[2])).openConnection().getContent();
                    l_Image3.setIcon(GlobalResources.CreateLittleImage(url[2], l_Image3, "3"));
                    l_Image3.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                    l_Image3.addMouseListener(this);
                } catch (IOException e) {
                    l_Image3.setText(" " + LangCenter.getString("Error"));
                    l_Image3.setBorder(BorderFactory.createLineBorder(Color.RED));
                    l_Image3.setForeground(Color.RED);
                }
                l_Image3.setToolTipText(url[2]);
                l_Image3.setName("Image3");
                l_Image3.setBounds(new Rectangle(210, 200, 70, 70));
            }
            query = Database.executed_query("SELECT Test_body " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short");
            tp_TestcaseTestBody.setText(query);
            tp_TestcaseTestBody.setBackground(new Color(240, 240, 240));
            scrollTestBody.setBounds(new Rectangle(10, 290, 570, 370));
            tp_TestcaseTestBody.setEditable(false);
            tp_TestcaseTestBody.setCaretPosition(0);
            l_TestcaseOptionalTest.setVisible(false);
            tp_TestcaseOptionalTest.setEnabled(false);
            scrollOptionalTest.setVisible(false);
            chb_EnableOptionalTest.setVisible(false);
            query = Database.executed_query("SELECT Optional_test " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short");
            if (!query.equals("")) {
                l_TestcaseOptionalTest.setVisible(true);
                scrollTestBody.setBounds(new Rectangle(10, 290, 570, 270));
                tp_TestcaseOptionalTest.setEnabled(true);
                scrollOptionalTest.setVisible(true);
                tp_TestcaseOptionalTest.setEditable(false);
                tp_TestcaseOptionalTest.setBackground(new Color(240, 240, 240));
                tp_TestcaseOptionalTest.setText(query);
                tp_TestcaseOptionalTest.setCaretPosition(0);
            }
            if (GlobalResources.Add_testcase.equals("1")) {
                b_Edit.setVisible(true);
            }
            l_Status.setVisible(true);
            String Status = "";
            int StatusID = Integer.parseInt(Database.executed_query("SELECT Status " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short"));
            if (StatusID == 0) {
                Status = LangCenter.getString("Disabled");
            } else if (StatusID == 1) {
                Status = LangCenter.getString("Active");
            }
            l_Status.setText(LangCenter.getString("Status") + ": " + Status);
        } else if (action.equals("Edit")) {
            query = Database.executed_query("SELECT Name " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short");
            tf_TestcaseName.setText(query);
            tf_TestcaseName.setBackground(Color.WHITE);
            tf_TestcaseName.setEditable(true);
            query = Database.executed_query("SELECT Keyword " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short");
            String keywords = "";
            st = new StringTokenizer(query, ";");
            while (st.countTokens() != 0) {
                keywords += Database.executed_query("SELECT Name " + "FROM keywords " + "WHERE ID='" + st.nextToken() + "';", "short") + "; ";
            }
            tf_TestcaseKeyword.setText(keywords);
            tf_TestcaseKeyword.setBackground(new Color(230, 230, 230));
            b_AddKeyword.setVisible(true);
            query = Database.executed_query("SELECT Information " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short");
            tp_TestcaseInformation.setText(query);
            tp_TestcaseInformation.setBackground(Color.WHITE);
            tp_TestcaseInformation.setEditable(true);
            tp_TestcaseInformation.setCaretPosition(0);
            query = Database.executed_query("SELECT Configuration_in_use " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short");
            tp_TestcaseCIU.setText(query);
            tp_TestcaseCIU.setBackground(Color.WHITE);
            tp_TestcaseCIU.setEditable(true);
            tp_TestcaseCIU.setCaretPosition(0);
            b_AddImage.setVisible(false);
            query = Database.executed_query("SELECT Image " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short");
            st = new StringTokenizer(query, "|");
            int count = st.countTokens();
            for (int i = 0; i < count; i++) {
                url[i] = st.nextToken();
            }
            l_Image1.setText(LangCenter.getString("Image") + " 1:");
            l_Image1.setIcon(null);
            l_Image1.setBorder(null);
            l_Image1.setForeground(Color.BLACK);
            l_Image1.setBounds(new Rectangle(10, 200, 100, 20));
            l_Image1.setVisible(true);
            tf_Image1.setText(url[0]);
            tf_Image1.setVisible(true);
            l_Image2.setText(LangCenter.getString("Image") + " 2:");
            l_Image2.setIcon(null);
            l_Image2.setBorder(null);
            l_Image2.setForeground(Color.BLACK);
            l_Image2.setBounds(new Rectangle(10, 220, 100, 20));
            l_Image2.setVisible(true);
            tf_Image2.setText(url[1]);
            tf_Image2.setVisible(true);
            l_Image3.setText(LangCenter.getString("Image") + " 3:");
            l_Image3.setIcon(null);
            l_Image3.setBorder(null);
            l_Image3.setForeground(Color.BLACK);
            l_Image3.setBounds(new Rectangle(10, 240, 100, 20));
            l_Image3.setVisible(true);
            tf_Image3.setText(url[2]);
            tf_Image3.setVisible(true);
            query = Database.executed_query("SELECT Test_body " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short");
            tp_TestcaseTestBody.setBackground(Color.WHITE);
            tp_TestcaseTestBody.setEditable(true);
            scrollTestBody.setBounds(new Rectangle(10, 290, 570, 270));
            tp_TestcaseTestBody.setText(query);
            tp_TestcaseTestBody.setCaretPosition(0);
            chb_EnableOptionalTest.setVisible(false);
            query = Database.executed_query("SELECT Optional_test " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short");
            l_TestcaseOptionalTest.setVisible(true);
            tp_TestcaseOptionalTest.setEnabled(true);
            tp_TestcaseOptionalTest.setBackground(Color.WHITE);
            tp_TestcaseOptionalTest.setEditable(true);
            tp_TestcaseOptionalTest.setText(query);
            tp_TestcaseOptionalTest.setCaretPosition(0);
            scrollOptionalTest.setVisible(true);
            b_Edit.setVisible(false);
            l_Status.setVisible(true);
            String Status = "";
            int StatusID = Integer.parseInt(Database.executed_query("SELECT Status " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short"));
            if (StatusID == 0) {
                Status = LangCenter.getString("Disabled");
            } else if (StatusID == 1) {
                Status = LangCenter.getString("Active");
            }
            l_Status.setText(LangCenter.getString("Status") + ": " + Status);
        }
    }

    /**
	 * � ������ �������������� ������� �� ������ ��, Cancel, Edit, Add_image.
	 * �� "��" ����� ��� ���������� ������ ����������� � ���� ������
	 * �� "Cancel" - ���������� �������� ������� (dispose()).
	 * �� "Edit" - ������ ��������� � ����� �������������� ���������� - 
	 * �� ���� ������ ������ ����������� ������ � resourceCall = "Edit" � ��� �� ID ���������.
	 * ������ Add_image ������������ ����� ������ � �������, �� ������� ��������� ���� �� ������� "+"
	 * �� "Add_image" ����������� ����� ���� ��� ����� url ��� �����������.
	 */
    public void actionPerformed(ActionEvent ae) {
        String str = ae.getActionCommand();
        query = "";
        if (str.equals("AddKeyword")) {
            String keywords = "";
            this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            JDialog dialog = new DialogSelection("keywords", tf_TestcaseKeyword.getText().replaceAll("; ", ";"));
            dialog.setVisible(true);
            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            if (DialogSelection.is_ok_pressed) {
                for (int i = 0; i < DialogSelection.sortOldQuery.length; i++) {
                    keywords += DialogSelection.sortOldQuery[i] + "; ";
                }
                tf_TestcaseKeyword.setText(keywords);
            }
        }
        if (str.equals("Cancel")) {
            if (resourceCall.equals("Edit")) {
                Database.executed_query("UPDATE testcase " + "SET Locked='0' " + "WHERE ID='" + selectID + "';", "short");
            }
            this.dispose();
        }
        if (str.equals("Ok")) {
            if (resourceCall.equals("Add")) {
                if (tf_TestcaseName.getText().equals("") | tp_TestcaseTestBody.getText().equals("")) {
                    JOptionPane.showMessageDialog(this, LangCenter.getString("FillTheAllField"), LangCenter.getString("Error"), JOptionPane.ERROR_MESSAGE);
                    if (tf_TestcaseName.getText().equals("")) {
                        l_TestcaseName.setForeground(Color.RED);
                        tf_TestcaseName.setBackground(new Color(255, 200, 200));
                    } else {
                        l_TestcaseName.setForeground(Color.BLACK);
                        tf_TestcaseName.setBackground(Color.WHITE);
                    }
                    if (tp_TestcaseTestBody.getText().equals("")) {
                        l_TestcaseTestBody.setForeground(Color.RED);
                        tp_TestcaseTestBody.setBackground(new Color(255, 200, 200));
                    } else {
                        l_TestcaseTestBody.setForeground(Color.BLACK);
                        tp_TestcaseTestBody.setBackground(Color.WHITE);
                    }
                } else {
                    String status = Database.executed_query("SELECT COUNT(Name) " + "FROM testcase " + "WHERE Name='" + tf_TestcaseName.getText() + "';", "short");
                    if (!status.equals("0")) {
                        JOptionPane.showMessageDialog(this, LangCenter.getString("InputTestcaseExists"), LangCenter.getString("Error"), JOptionPane.ERROR_MESSAGE);
                    } else {
                        String User_id = Database.executed_query("SELECT ID " + "FROM users " + "WHERE Name='" + GlobalResources.USER_NAME + "';", "short");
                        String keywords_id = "";
                        st = new StringTokenizer(tf_TestcaseKeyword.getText().replaceAll("; ", ";"), ";");
                        while (st.countTokens() != 0) {
                            keywords_id += Database.executed_query("SELECT ID " + "FROM keywords " + "WHERE Name='" + st.nextToken() + "';", "short") + ";";
                        }
                        String Image = "";
                        if (!tf_Image1.getText().equals("")) {
                            Image = tf_Image1.getText();
                        }
                        if (!tf_Image2.getText().equals("")) {
                            Image = Image + "|" + tf_Image2.getText();
                        }
                        if (!tf_Image3.getText().equals("")) {
                            Image = Image + "|" + tf_Image3.getText();
                        }
                        Database.executed_query("INSERT INTO testcase(ID, User_id, Created, Name, " + "Keyword, Information, Image, Configuration_in_use, Test_body, " + "Optional_test, Locked, Status) " + "VALUES (null," + "'" + User_id + "'," + "'" + GlobalResources.date() + "'," + "'" + GlobalResources.prepare(tf_TestcaseName.getText()) + "'," + "'" + keywords_id + "'," + "'" + GlobalResources.prepare(tp_TestcaseInformation.getText()) + "'," + "'" + GlobalResources.prepare(Image) + "'," + "'" + GlobalResources.prepare(tp_TestcaseCIU.getText()) + "'," + "'" + GlobalResources.prepare(tp_TestcaseTestBody.getText()) + "'," + "'" + GlobalResources.prepare(tp_TestcaseOptionalTest.getText()) + "'," + "'0','1');", "short");
                        Database.executed_query("UPDATE testcase " + "SET Locked='0' " + "WHERE ID='" + selectID + "';", "short");
                        this.dispose();
                        PanelTestcases.CellAdd("1", tf_TestcaseName.getText());
                        JOptionPane.showMessageDialog(this, LangCenter.getString("InputValueAdded"), LangCenter.getString("Success"), JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
            if (resourceCall.equals("Edit")) {
                if (tf_TestcaseName.getText().equals("") | tp_TestcaseTestBody.getText().equals("")) {
                    JOptionPane.showMessageDialog(this, LangCenter.getString("FillTheAllField"), LangCenter.getString("Error"), JOptionPane.ERROR_MESSAGE);
                    if (tf_TestcaseName.getText().equals("")) {
                        l_TestcaseName.setForeground(Color.RED);
                        tf_TestcaseName.setBackground(new Color(255, 200, 200));
                    } else {
                        l_TestcaseName.setForeground(Color.BLACK);
                        tf_TestcaseName.setBackground(Color.WHITE);
                    }
                    if (tp_TestcaseTestBody.getText().equals("")) {
                        l_TestcaseTestBody.setForeground(Color.RED);
                        tp_TestcaseTestBody.setBackground(new Color(255, 200, 200));
                    } else {
                        l_TestcaseTestBody.setForeground(Color.BLACK);
                        tp_TestcaseTestBody.setBackground(Color.WHITE);
                    }
                } else {
                    String User_id = Database.executed_query("SELECT ID " + "FROM users " + "WHERE Name='" + GlobalResources.USER_NAME + "';", "short");
                    String Image = "";
                    if (!tf_Image1.getText().equals("")) {
                        Image = tf_Image1.getText();
                    }
                    if (!tf_Image2.getText().equals("")) {
                        Image = Image + "|" + tf_Image2.getText();
                    }
                    if (!tf_Image3.getText().equals("")) {
                        Image = Image + "|" + tf_Image3.getText();
                    }
                    String Status = Database.executed_query("SELECT Status " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short");
                    String keywords_id = "";
                    st = new StringTokenizer(tf_TestcaseKeyword.getText().replaceAll("; ", ";"), ";");
                    while (st.countTokens() != 0) {
                        keywords_id += Database.executed_query("SELECT ID " + "FROM keywords " + "WHERE Name='" + st.nextToken() + "';", "short") + ";";
                    }
                    Database.executed_query("UPDATE testcase " + "SET User_id='" + User_id + "', " + "Created='" + GlobalResources.date() + "', " + "Name='" + GlobalResources.prepare(tf_TestcaseName.getText()) + "', " + "Keyword='" + keywords_id + "', " + "Information='" + GlobalResources.prepare(tp_TestcaseInformation.getText()) + "', " + "Image='" + GlobalResources.prepare(Image) + "', " + "Configuration_in_use='" + GlobalResources.prepare(tp_TestcaseCIU.getText()) + "', " + "Test_body='" + GlobalResources.prepare(tp_TestcaseTestBody.getText()) + "', " + "Optional_test='" + GlobalResources.prepare(tp_TestcaseOptionalTest.getText()) + "', " + "Locked='0', " + "Status='" + Status + "' " + "WHERE ID='" + selectID + "';", "short");
                    Database.executed_query("UPDATE testcase " + "SET Locked='0' " + "WHERE ID='" + selectID + "';", "short");
                    PanelTestcases.CellUpdate(Status, tf_TestcaseName.getText());
                    this.dispose();
                    JOptionPane.showMessageDialog(this, LangCenter.getString("InputValueChanged"), LangCenter.getString("Success"), JOptionPane.INFORMATION_MESSAGE);
                }
            }
            if (resourceCall.equals("View")) {
                this.dispose();
            }
        }
        if (str.equals("Add_image")) {
            if (!l_Image2.isVisible()) {
                l_Image2.setVisible(true);
                tf_Image2.setVisible(true);
            } else if (!l_Image3.isVisible()) {
                l_Image3.setVisible(true);
                tf_Image3.setVisible(true);
            }
        }
        if (str.equals("Edit")) {
            boolean is_locked = false;
            query = Database.executed_query("SELECT Locked " + "FROM testcase " + "WHERE ID='" + selectID + "';", "short");
            if (query.equals("1")) {
                is_locked = true;
                int answer = JOptionPane.showConfirmDialog(this, LangCenter.getString("RecordLock"), LangCenter.getString("Lock"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (answer == JOptionPane.YES_OPTION) {
                    is_locked = false;
                }
            }
            if (!is_locked) {
                Database.executed_query("UPDATE testcase " + "SET Locked='1' " + "WHERE ID='" + selectID + "';", "short");
                this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                resourceCall = "Edit";
                update_field(resourceCall);
                this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    /**
	 * � ���� ������ ���������� ���� ��� ����� �������������� ������ (Optional Test).
	 * �� ��������� ��� ���� ���������. ���������� ������� "Enable Optional Test"
	 */
    public void itemStateChanged(ItemEvent ie) {
        if (resourceCall.equals("Add")) {
            if (chb_EnableOptionalTest.isSelected()) {
                l_TestcaseOptionalTest.setVisible(true);
                scrollOptionalTest.setVisible(true);
                tp_TestcaseOptionalTest.setEnabled(true);
            } else {
                tp_TestcaseOptionalTest.setEnabled(false);
                scrollOptionalTest.setVisible(false);
                l_TestcaseOptionalTest.setVisible(false);
            }
        }
    }

    public void mouseClicked(MouseEvent me) {
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    public void mouseReleased(MouseEvent me) {
    }

    /**
	 * ���� ����� ��������� ������ ������ ������ ��������.
	 * ��� ����������� ������ �������� ���������� �������� �� ����������� ����� �����������.
	 */
    public void mousePressed(MouseEvent me) {
        String whoIs = me.getComponent().getName();
        if (whoIs.equals("Image1")) {
            this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            dialogLoadPicture = new DialogLoadPicture(url[0]);
            dialogLoadPicture.setVisible(true);
            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        } else if (whoIs.equals("Image2")) {
            this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            dialogLoadPicture = new DialogLoadPicture(url[1]);
            dialogLoadPicture.setVisible(true);
            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        } else if (whoIs.equals("Image3")) {
            this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            dialogLoadPicture = new DialogLoadPicture(url[2]);
            dialogLoadPicture.setVisible(true);
            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
	 * ����� ���������� �������� ���� ��������� �������.
	 * @author Stigor
	 */
    class DisposeHandler implements ActionListener {

        public void actionPerformed(ActionEvent evt) {
            if (resourceCall.equals("View")) dispose();
        }
    }

    /**
	 * ����� ������������ ������� �� ������� Esc � ��������� ������ � ������.
	 */
    protected JRootPane createRootPane() {
        JRootPane rootPane = super.createRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        rootPane.registerKeyboardAction(new DisposeHandler(), stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        return rootPane;
    }
}
