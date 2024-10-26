package DN2;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

class mainGUI extends JFrame {

    JPanel BottomPanel;

    private JTable table;

    private BottomPanel bottompanel;

    private SetPanel setpanel;

    public mainGUI() {
        setLayout(null);
        setResizable(false);
        Image frameimage = Toolkit.getDefaultToolkit().getImage("frame.gif");
        setIconImage(frameimage);
        setTray(this);
        int x, y, w, h, gap;
        x = 10;
        y = 10;
        h = 30;
        gap = 10;
        setpanel = new SetPanel();
        setpanel.setBounds(x = 0, y = 0, w = 600, h = 180);
        add(setpanel);
        DefaultTableCellRenderer cr2 = new DefaultTableCellRenderer() {

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JProgressBar bar = new JProgressBar(0, 100);
                if (value instanceof String) {
                    String amount = value.toString();
                    int bar_value = Integer.parseInt(amount);
                    bar.setValue(bar_value);
                    bar.setStringPainted(true);
                }
                return bar;
            }
        };
        String[] columnNames = { "�̸�", "���� ũ��", "���� ��", "�����", "���� �ð�", "���� �ð�", "�ӵ�", "���� ��", "��õ� Ƚ��", "�ּ�" };
        MyTableModel mtm = new MyTableModel(columnNames, 10);
        table = new JTable(mtm);
        setpanel.setTable(table);
        setpanel.setMtm(mtm);
        table.setAutoResizeMode(table.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(3).setCellRenderer(cr2);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(x, y += h + gap, w = 600, h = 150);
        add(scrollPane);
        bottompanel = new BottomPanel();
        bottompanel.setBounds(x, y += h + gap, w = 600, h = 30);
        add(bottompanel);
        bottompanel.setString("���� ����â");
    }

    public void setTray(final JFrame frame) {
        ActionListener listener = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("quit")) {
                    System.exit(0);
                } else if (e.getActionCommand().equals("hide")) {
                    frame.setVisible(false);
                } else if (e.getActionCommand().equals("show")) {
                    frame.setVisible(true);
                }
            }
        };
        SystemTray tray = SystemTray.getSystemTray();
        Image trayimage = Toolkit.getDefaultToolkit().getImage("frame.gif");
        PopupMenu popup = new PopupMenu();
        MenuItem show = new MenuItem("���̱�");
        show.setActionCommand("show");
        popup.add(show);
        show.addActionListener(listener);
        MenuItem hide = new MenuItem("����");
        popup.add(hide);
        hide.setActionCommand("hide");
        popup.add(hide);
        hide.addActionListener(listener);
        popup.addSeparator();
        MenuItem quit = new MenuItem("����");
        popup.add(quit);
        quit.setActionCommand("quit");
        popup.add(quit);
        quit.addActionListener(listener);
        TrayIcon trayIcon = new TrayIcon(trayimage, "��Ƽ �ٿ�δ�", popup);
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }
}

/**
 * 
 * ���� �гκ�
 *
 */
class SetPanel extends JPanel implements ActionListener, MouseListener, KeyListener {

    private JLabel Laddr;

    private JLabel Lfolder;

    private JLabel Lsplit;

    private JLabel Lfilename;

    private JTextField Taddr;

    private JTextField Tfolder;

    private JTextField Tfilename;

    private JSpinner Tsplit;

    private SpinnerNumberModel SNM;

    private JButton Bdown;

    private JButton Bbrowse;

    private JTable table;

    private MyTableModel mymodel;

    private int rowNum;

    private DownloadManager[] downloadmanager;

    private JButton Stop;

    private JButton Start;

    private JButton Remove;

    private JFileChooser chooser;

    public SetPanel() {
        setLayout(null);
        InitPanel();
        downloadmanager = new DownloadManager[10];
    }

    public void InitPanel() {
        Laddr = new JLabel("�ּ�");
        Lfolder = new JLabel("���� ��ġ");
        Lsplit = new JLabel("���� ����");
        Lfilename = new JLabel("���� �̸�");
        Taddr = new JTextField("");
        Tfolder = new JTextField();
        Tfilename = new JTextField();
        SNM = new SpinnerNumberModel(1, 1, 10, 1);
        Tsplit = new JSpinner(SNM);
        Bdown = new JButton("�޾� ����");
        Bbrowse = new JButton("���� ��ġ");
        chooser = new JFileChooser();
        chooser.setFileSelectionMode(chooser.DIRECTORIES_ONLY);
        Stop = new JButton(new ImageIcon(Toolkit.getDefaultToolkit().getImage("stop20.gif")));
        Start = new JButton(new ImageIcon(Toolkit.getDefaultToolkit().getImage("start20.gif")));
        Remove = new JButton(new ImageIcon(Toolkit.getDefaultToolkit().getImage("remove20.gif")));
        Stop.setActionCommand("stop");
        Stop.addActionListener(this);
        Stop.setToolTipText("���� ���õ� �ٿ�ε��� ������ ���� �մϴ�.");
        Start.setActionCommand("start");
        Start.addActionListener(this);
        Start.setToolTipText("���� ���õ� ���ڵ��� �ּҷ� �̾�ޱ� �մϴ�.");
        Remove.setActionCommand("remove");
        Remove.addActionListener(this);
        Remove.setToolTipText("���� ���õ� ���ڵ带 ���� �մϴ�.");
        Bdown.setActionCommand("down");
        Bdown.addActionListener(this);
        Bbrowse.setActionCommand("browse");
        Bbrowse.addActionListener(this);
        Taddr.addKeyListener(this);
        Taddr.addMouseListener(this);
        addComponents();
    }

    public String getAddr() {
        return Taddr.getText();
    }

    public String getFolder() {
        return Tfolder.getText();
    }

    public String getFilename() {
        return Tfilename.getText();
    }

    public int getSplit() {
        return Integer.parseInt(Tsplit.getValue().toString());
    }

    public void setTable(JTable table) {
        this.table = table;
    }

    public void setMtm(MyTableModel mtm) {
        this.mymodel = mtm;
    }

    private void addComponents() {
        int x, y, w, h, gap;
        x = 10;
        y = 10;
        h = 30;
        gap = 10;
        Laddr.setBounds(x, y, w = 60, h);
        add(Laddr);
        Taddr.setBounds(x += w + gap, y, w = 400, h);
        add(Taddr);
        Bdown.setBounds(x + w + gap, y, w = 100, h);
        add(Bdown);
        Lfolder.setBounds(x = 10, y += h + gap, w = 60, h);
        add(Lfolder);
        Tfolder.setBounds(x += w + gap, y, w = 400, h);
        add(Tfolder);
        Bbrowse.setBounds(x + w + gap, y, w = 100, h);
        add(Bbrowse);
        Lsplit.setBounds(x = 10, y += h + gap, w = 60, h);
        add(Lsplit);
        Tsplit.setBounds(x += w + gap, y, w = 60, h);
        add(Tsplit);
        Lfilename.setBounds(x = 10, y += h + gap, w = 60, h);
        add(Lfilename);
        Tfilename.setBounds(x += w + gap, y, w = 300, h);
        add(Tfilename);
        Remove.setBounds(x = 470, y += h, w = 20, h = 20);
        add(Remove);
        Stop.setBounds(x += w + gap, y, w, h);
        add(Stop);
        Start.setBounds(x += w + gap, y, w, h);
        add(Start);
    }

    public void SetFileName() {
        String addr = Taddr.getText();
        String http = "http://";
        int httplen = http.length();
        String filename = "";
        if (addr.length() > httplen) {
            String t = addr.substring(0, "http://".length());
            if (!t.equals(http)) {
            } else {
                try {
                    URL url = new URL(addr);
                    t = url.getPath();
                    int p = t.lastIndexOf("/");
                    t = t.substring(p + 1, t.length());
                    if (t.equals("")) {
                        filename = "index.htm";
                    } else {
                        filename = t;
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
        Tfilename.setText(filename);
    }

    public void actionPerformed(ActionEvent e) {
        if ("browse".equals(e.getActionCommand())) {
            int retval = chooser.showDialog(null, "����");
            if (retval == chooser.APPROVE_OPTION) {
                Tfolder.setText(chooser.getSelectedFile().getPath());
            }
        } else if ("down".equals(e.getActionCommand())) {
            if (Taddr.getText().equals("")) {
                MessageDialog.CreateDialog(this, "�ּҸ� �Է��ϼ���", "�Է°� ����");
            } else if (Tfolder.getText().equals("")) {
                MessageDialog.CreateDialog(this, "������ġ�� �����ϼ���", "�Է°� ����");
            } else if (Tfilename.getText().equals("")) {
                MessageDialog.CreateDialog(this, "�����̸��� �Է��ϼ���", "�Է°�����");
            } else {
                File file = new File(getFolder() + "\\" + getFilename());
                if (file.exists()) {
                    int r;
                    if ((r = CheckTable()) != -1) {
                        if (MessageDialog.ContinueTrans(this.getParent()) == 1) {
                            if (downloadmanager[r].remove()) mymodel.removeRow(r);
                            downStart();
                        } else {
                            String filename = table.getValueAt(r, 0) + ".dn";
                            File bkfile = new File(filename);
                            if (bkfile.exists()) {
                                downRestart(bkfile, r);
                            } else {
                                MessageDialog.CreateDialog(this.getParent(), "���� ����", "��� ������ ���� �Ǿ���ϴ�.");
                            }
                        }
                    } else {
                        MessageDialog md = new MessageDialog();
                        if (md.FileExists(this.getParent()) == 1) {
                        } else {
                            downStart();
                        }
                    }
                } else {
                    downStart();
                }
            }
        } else if ("stop".equals((e.getActionCommand()))) {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) return;
            if (table.getValueAt(selectedRow, 0) != null) {
                if (!downloadmanager[selectedRow].getStop()) downloadmanager[selectedRow].setStop();
            }
        } else if ("start".equals((e.getActionCommand()))) {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) return;
            if (table.getValueAt(selectedRow, 0) != null) {
                String filename = table.getValueAt(selectedRow, 0) + ".dn";
                File bkfile = new File(filename);
                if (bkfile.exists()) {
                    downRestart(bkfile, selectedRow);
                } else {
                    MessageDialog.CreateDialog(this.getParent(), "���� ����", "��� ������ ���� �Ǿ���ϴ�.");
                }
            }
        } else if ("remove".equals((e.getActionCommand()))) {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) return;
            if (table.getValueAt(selectedRow, 0) != null) {
                String bkfilename = table.getValueAt(selectedRow, 0) + ".dn";
                String filename = table.getValueAt(selectedRow, 0) + "";
                File file = new File(filename);
                File bkfile = new File(bkfilename);
                if (!downloadmanager[selectedRow].getStop()) {
                    if (downloadmanager[selectedRow].remove()) mymodel.removeRow(selectedRow);
                } else {
                    mymodel.removeRow(selectedRow);
                }
                if (file.exists()) file.delete();
                if (bkfile.exists()) bkfile.delete();
            }
        }
    }

    public URL CheckHost(String addr) {
        int port;
        URL url;
        try {
            url = new URL(addr);
            port = (url.getPort() != -1) ? url.getPort() : 80;
            Socket soc = new Socket(url.getHost(), port);
            soc.close();
        } catch (UnknownHostException te) {
            MessageDialog.CreateDialog(this.getParent(), "HOST ����", "������ ���� �� �� ����ϴ�!!");
            url = null;
        } catch (MalformedURLException e) {
            MessageDialog.CreateDialog(this.getParent(), "HOST ����", "�Է��Ͻ� �ּҿ� ������ �ֽ��ϴ�.");
            url = null;
        } catch (IOException e) {
            MessageDialog.CreateDialog(this.getParent(), "SOCKET ����", "SOCKET�� ���µ� ���� �߽��ϴ�.");
            url = null;
        }
        return url;
    }

    public void downRestart(File file, int selectedRow) {
        Information info = new DNFileManager().getDNfileContent(file);
        URL url = CheckHost(table.getValueAt(selectedRow, 9) + "");
        if (url == null) return;
        info.setRestart(true);
        info.SetTable(table);
        info.SetRowNum(selectedRow);
        SocketManager sm = new SocketManager(info);
        HttpHeader hheader = new HttpHeader();
        downloadmanager[selectedRow] = new DownloadManager(info, hheader, sm);
        downloadmanager[selectedRow].start();
    }

    public int CheckTable() {
        int retval = -1;
        for (int i = 0; i < 10; i++) {
            if ((table.getValueAt(i, 0)) != null) {
                if (table.getValueAt(i, 9).equals(getAddr())) {
                    retval = i;
                    break;
                }
            }
        }
        return retval;
    }

    public void downStart() {
        try {
            int port;
            URL url = CheckHost(getAddr());
            if (url == null) return;
            if (DNutil.RedirectCheck(url)) {
                String addr = DNutil.GetRedirectLocation(url);
                url = new URL(addr);
                String t = url.getPath().substring(url.getPath().lastIndexOf("/") + 1, url.getPath().length());
                Taddr.setText(addr);
                Tfilename.setText(t);
            }
            port = (url.getPort() != -1) ? url.getPort() : 80;
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            rowNum = mymodel.getEmptyRow();
            File file = new File(getFolder() + "\\" + getFilename());
            File bkfile = new File(getFolder() + "\\" + getFilename() + ".dn");
            table.setValueAt(getFolder() + "\\" + getFilename(), rowNum, 0);
            table.setValueAt(con.getContentLength() + " Byte", rowNum, 1);
            table.setValueAt(0 + "", rowNum, 2);
            table.setValueAt(getSplit() + "", rowNum, 7);
            table.setValueAt(getAddr(), rowNum, 9);
            Information info = new Information(url.getHost(), url.getFile(), port, file, con.getContentLength(), getSplit(), bkfile);
            info.SetTable(table);
            info.SetRowNum(rowNum);
            SocketManager sm = new SocketManager(info);
            HttpHeader hheader = new HttpHeader();
            downloadmanager[rowNum] = new DownloadManager(info, hheader, sm);
            con.disconnect();
            downloadmanager[rowNum].start();
        } catch (MalformedURLException te) {
            te.printStackTrace();
        } catch (IOException te) {
            te.printStackTrace();
        }
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
        if (e.getSource() == Taddr) {
            SetFileName();
        }
    }

    public void keyTyped(KeyEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
}

/**
 * ���̺� �� 
 */
class MyTableModel extends DefaultTableModel {

    public MyTableModel(Object[] columnNames, int row) {
        super(columnNames, row);
    }

    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public int getEmptyRow() {
        int retval = -1;
        int row = getRowCount();
        for (int i = 0; i < row; i++) {
            if (this.getValueAt(i, 0) == null) {
                retval = i;
                break;
            }
        }
        return retval;
    }

    public void removeRow(int row) {
        int col = getColumnCount();
        for (int i = 0; i < col; i++) {
            setValueAt(null, row, i);
        }
    }
}

/**
 * ����â �г�
 */
class StatPanel extends JScrollPane {

    private JTable table;

    public StatPanel(JTable table) {
        this.table = table;
        setLayout(new BorderLayout());
        addComponent();
    }

    private void addComponent() {
        add(table.getTableHeader(), BorderLayout.PAGE_START);
        add(table, BorderLayout.CENTER);
    }
}

/**
 * ���� �г�
 */
class BottomPanel extends JPanel {

    private JLabel Lstatus;

    public BottomPanel() {
        setLayout(null);
        Lstatus = new JLabel("");
        Lstatus.setBounds(50, 0, 550, 30);
        add(Lstatus);
    }

    public void setString(String status) {
        Lstatus.setText(status);
    }
}

class MessageDialog {

    public MessageDialog() {
    }

    public int FileExists(Container frame) {
        int t;
        JOptionPane optionPane = new JOptionPane();
        t = optionPane.showConfirmDialog(frame, "������ ���� �մϴ�.\n" + "���� ���ðڽ��ϱ�?", "File exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return t;
    }

    public static int ContinueTrans(Container frame) {
        int t;
        JOptionPane optionPane = new JOptionPane();
        t = optionPane.showConfirmDialog(frame, "�ش� �ּҷ� �޴� ������ ���̺? ���� �մϴ�.\n" + "�̾�ޱ⸦ �Ͻðڽ��ϱ�?", "�̾� �ޱ�", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return t;
    }

    public static void CreateDialog(Component frame, String value, String message) {
        JOptionPane optionPane = new JOptionPane();
        optionPane.showConfirmDialog(frame, value, message, JOptionPane.CLOSED_OPTION, JOptionPane.INFORMATION_MESSAGE);
    }
}

public class msdmGUI {

    public static void main(String args[]) {
        mainGUI mg = new mainGUI();
        mg.setTitle("���� ���� �ٿ�δ�");
        mg.setSize(600, 410);
        mg.setDefaultCloseOperation(mg.EXIT_ON_CLOSE);
        mg.setVisible(true);
    }
}
