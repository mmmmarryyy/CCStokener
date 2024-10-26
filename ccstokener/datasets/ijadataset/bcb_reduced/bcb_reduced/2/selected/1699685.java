package net.cryff.exe;

import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import net.cryff.net.NetPacket;
import net.cryff.security.Algorithm;
import net.cryff.settings.ConfigLoader;
import net.cryff.test.TestMethod;
import net.cryff.utils.FileUtil;
import net.cryff.utils.JARPatcher;

public class Launcher extends JFrame {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private JLabel logoLabel = new JLabel();

    private ImageIcon logo = null;

    private JLabel version = new JLabel();

    private String[] dummy = new String[1];

    private JComboBox serverBox = null;

    private String[] serverlist = null;

    private String[] ports = null;

    private JTextField username = new JTextField();

    private JPasswordField password = new JPasswordField();

    private JButton login = new JButton("login");

    private JButton update = new JButton("update");

    private JCheckBox save_name = new JCheckBox();

    private String ver_nr = null;

    private String no_server_list = "Error getting serverlist, please check internet connection";

    private ConfigLoader config = null;

    private JLabel explanation = new JLabel("save username");

    private int current_version = 0;

    private int server_version = -1;

    private boolean saveUsername = false;

    private Launcher launch = this;

    public static boolean loggedIn = false;

    void increaseCurrentVersion() {
        current_version++;
    }

    private Point getStartPosition() {
        java.awt.Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (dim.width - 550) / 2;
        int y = (dim.height - 500) / 2;
        return new Point(x, y);
    }

    private void setSettings() {
        try {
            String home = System.getProperty("user.home");
            config = new ConfigLoader(home + "/.cff/launcher.cnf");
            saveUsername = Boolean.parseBoolean(config.getValue("save_name"));
            current_version = Integer.parseInt(config.getValue("version"));
            ver_nr = "Build " + current_version + "";
            if (saveUsername) {
                username.setText(config.getValue("username"));
                save_name.setSelected(true);
            }
        } catch (Exception e) {
            String home = System.getProperty("user.home");
            File f = new File(home + "/.cff/");
            try {
                f.mkdir();
                LinkedList<String> temp = new LinkedList<String>();
                temp.add("save_name false");
                temp.add("username  username");
                temp.add("version " + current_version);
                FileUtil.write(temp, home + "/.cff/launcher.cnf");
            } catch (Exception e2) {
                e2.printStackTrace();
                System.err.println("no writing priviliges");
            }
        }
    }

    private void getServerlist() {
        try {
            LinkedList<String> version = FileUtil.read(new URL("http://www.sharpner.de/cff/launch/version.sdf"));
            if (server_version > current_version) {
                login.setEnabled(false);
                update.setEnabled(true);
                JOptionPane.showMessageDialog(this, "Please run the updater to receive the new Version of Cry For Freedom, otherwise you cannot continue playing.", "New Version available!", 1);
            } else {
                update.setEnabled(false);
                login.setEnabled(true);
            }
            server_version = Integer.parseInt(version.get(0));
            LinkedList<String> serverlist = FileUtil.read(new URL("http://www.sharpner.de/cff/launch/list.sdf"));
            String[] servers = new String[serverlist.size()];
            String[] ports = new String[serverlist.size()];
            String[] names = new String[serverlist.size()];
            for (int i = 0; i < serverlist.size(); i++) {
                String[] temp = serverlist.get(i).split(" ");
                if (temp.length >= 3) {
                    servers[i] = temp[0];
                    ports[i] = temp[1];
                    names[i] = temp[2];
                }
            }
            this.serverlist = servers;
            this.ports = ports;
            serverBox.removeAllItems();
            for (int i = 0; i < servers.length; i++) {
                serverBox.addItem(names[i]);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, no_server_list, "Error", 0);
        }
    }

    private int uid = -1;

    public void setUID(int i) {
        this.uid = i;
    }

    private void login() {
        String user = username.getText();
        String pw = new String(password.getPassword());
        pw = Algorithm.toMD5(pw);
        System.out.println(pw);
        Receiver r = null;
        if (saveUsername) {
            try {
                LinkedList<String> temp = new LinkedList<String>();
                temp.add("save_name true");
                temp.add("username " + user);
                temp.add("version " + current_version);
                String home = System.getProperty("user.home");
                FileUtil.write(temp, home + "/.cff/launcher.cnf");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        int j = serverBox.getSelectedIndex();
        try {
            int port = Integer.parseInt(ports[j]);
            Socket s = new Socket(serverlist[j].trim(), port);
            r = new Receiver(s, this);
            r.start();
            NetPacket autho = new NetPacket(user, pw);
            Launcher.sendPacket(s, autho);
            try {
                String crc = Algorithm.checkSum("game.jar");
                NetPacket np = new NetPacket(crc);
                Launcher.sendPacket(s, np);
            } catch (Exception e) {
            }
            if (Launcher.loggedIn) {
                System.out.println("logged");
                r.stopReceiver();
                Thread t = new Thread(new TestMethod(null, s));
                this.setVisible(false);
                t.start();
                this.dispose();
            }
        } catch (ConnectException e) {
            JOptionPane.showMessageDialog(this, "Server Offline! Please try again later", "Error", 0);
        } catch (EOFException e) {
            JOptionPane.showMessageDialog(this, "Connection to the server lost!", "Error", 0);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unknown Error! " + e.getLocalizedMessage(), "Error", 0);
            e.printStackTrace();
        }
    }

    private void createLayout() {
        logoLabel.setBounds(15, 10, 500, 300);
        logoLabel.setIcon(logo);
        version.setBounds(10, 445, 100, 15);
        version.setText(ver_nr);
        serverBox.setBounds(200, 440, 150, 25);
        username.setBounds(200, 340, 150, 25);
        password.setBounds(200, 390, 150, 25);
        login.setBounds(366, 390, 150, 25);
        update.setBounds(366, 340, 150, 25);
        save_name.setBounds(10, 340, 25, 25);
        explanation.setBounds(35, 340, 150, 25);
        login.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                login();
            }
        });
        update.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                Updater up = new Updater(server_version, launch);
                up.setVisible(true);
            }
        });
        save_name.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                saveUsername = !saveUsername;
                writeChanges();
            }
        });
        this.add(explanation);
        this.add(save_name);
        this.add(login);
        this.add(serverBox);
        this.add(update);
        this.add(username);
        this.add(password);
        this.add(version);
        this.add(logoLabel);
    }

    void writeChanges() {
        if (saveUsername) {
            try {
                LinkedList<String> temp = new LinkedList<String>();
                temp.add("save_name true");
                temp.add("username " + username.getText());
                temp.add("version " + current_version);
                String home = System.getProperty("user.home");
                FileUtil.write(temp, home + "/.cff/launcher.cnf");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                LinkedList<String> temp = new LinkedList<String>();
                temp.add("save_name false");
                temp.add("version " + current_version);
                String home = System.getProperty("user.home");
                FileUtil.write(temp, home + "/.cff/launcher.cnf");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadImages() {
        logo = new ImageIcon(getClass().getResource("ressource/launcher-logo.jpg"));
    }

    public Launcher() {
        super("Cry For Freedom Launcher");
        dummy[0] = "getting Serverlist...";
        serverBox = new JComboBox(dummy);
        loadImages();
        this.setSettings();
        Point p = this.getStartPosition();
        this.setBounds((int) p.getX(), (int) p.getY(), 550, 500);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(null);
        this.createLayout();
        this.setVisible(true);
    }

    public static void main(String[] args) {
        Launcher launcher = new Launcher();
        System.out.println(Algorithm.toMD5("fisch"));
        launcher.getServerlist();
    }

    public static void sendPacket(Socket s, NetPacket p) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
        oos.writeObject(p);
        oos.flush();
    }

    public static NetPacket readPacket(Socket s) throws IOException, ClassNotFoundException {
        ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
        Object o = oin.readObject();
        if (o != null && o instanceof NetPacket) {
            return (NetPacket) o;
        }
        return null;
    }
}

class Receiver extends Thread {

    private Socket sock = null;

    private Launcher l = null;

    public Receiver(Socket s, Launcher l) {
        sock = s;
        this.l = l;
    }

    private int uid = -1;

    public int getUid() {
        return uid;
    }

    private boolean isRunning = true;

    public void stopReceiver() {
        isRunning = false;
    }

    public void run() {
        while (isRunning) {
            try {
                ObjectInputStream oin = new ObjectInputStream(sock.getInputStream());
                Object o = oin.readObject();
                if (o != null && o instanceof NetPacket) {
                    NetPacket p = (NetPacket) o;
                    if (p.getType() == NetPacket.TYPE_INFORMATION) {
                        uid = p.getUID();
                        l.setUID(uid);
                    }
                    String msg = p.getString();
                    System.out.println(msg);
                    if (msg.equals("login_denied")) {
                        JOptionPane.showMessageDialog(null, "Wrong Username and/or Password!", "Error", 0);
                    } else if (msg.equals("con_est")) {
                        System.out.println("Connection to the server sucessful...");
                        Launcher.loggedIn = true;
                    } else if (msg.equals("srv_full")) {
                        JOptionPane.showMessageDialog(null, "Sorry, the server is currently full. Please try again later", "Error", 0);
                    } else if (msg.trim().equals("wrong_client")) {
                        JOptionPane.showMessageDialog(null, "Wrong client version, either update, or download the latest client from the page!", "Error", 0);
                    } else if (msg.equals("login_success")) {
                        System.out.println("Login sucessfull... start game...");
                    }
                }
            } catch (SocketException se) {
            } catch (Exception e) {
            }
        }
        System.out.println("Receiver stopped");
    }
}

class Updater extends JFrame implements Runnable {

    private JProgressBar progress = null;

    private JButton button = new JButton("start");

    private int version = -1;

    private long max = 1;

    private JLabel status = new JLabel();

    private long current = 0;

    private Thread t = new Thread(this);

    private Updater up = null;

    public Launcher launch = null;

    public Updater(int i, Launcher l) {
        super("Update to Build " + i);
        this.launch = l;
        this.setLayout(null);
        up = this;
        Point p = getStartPosition();
        this.setBounds((int) p.getX(), (int) p.getY(), 300, 150);
        progress = new JProgressBar(0, 100);
        progress.setBounds(7, 5, 270, 30);
        button.setBounds(60, 80, 160, 25);
        status.setBounds(10, 45, 200, 25);
        this.add(status);
        this.add(progress);
        this.add(button);
        version = i;
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                if (button.getText().equals("apply patch!")) {
                    up.setVisible(false);
                    try {
                        JARPatcher.patch("updates/" + version + ".zip", "game.jar");
                        JOptionPane.showMessageDialog(null, "patched!", "Success!", 2);
                        launch.increaseCurrentVersion();
                        launch.writeChanges();
                        JOptionPane.showMessageDialog(null, "Please restart Launcher now!", "Success!", 2);
                        System.exit(0);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "Error patching, please try again!", "Error!", 0);
                    }
                } else {
                    button.setText("cancel");
                    t.start();
                    button.setEnabled(false);
                }
            }
        });
    }

    public void run() {
        startDownload("http://www.sharpner.de/cff/updates/" + version + ".zip", "updates/" + version + ".zip");
        button.setText("apply patch!");
        button.setEnabled(true);
    }

    public void startDownload(String source, String path) {
        File f = new File(path);
        if (!f.exists()) {
            File f2 = new File(f.getParent());
            f2.mkdir();
        }
        OutputStream out = null;
        URLConnection urlc = null;
        InputStream in = null;
        try {
            URL url = new URL(source);
            out = new BufferedOutputStream(new FileOutputStream(path));
            urlc = url.openConnection();
            max = urlc.getContentLength();
            in = urlc.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
                current = numWritten;
                if (max != 0) {
                    float percent = (current * 100 / max);
                    status.setText(current + "/" + max + " bytes");
                    if (current == max) {
                        status.setText("complete!");
                    }
                    progress.setValue((int) percent);
                    progress.repaint();
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private Point getStartPosition() {
        java.awt.Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (dim.width - 300) / 2;
        int y = (dim.height - 150) / 2;
        return new Point(x, y);
    }
}
