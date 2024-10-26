import javax.swing.*;
import java.applet.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import netscape.javascript.*;
import java.awt.event.*;

public class PassPoints extends JApplet {

    private int[][] clickPoints;

    private int clickCount;

    private String system;

    private String username;

    private String mode;

    private int numClicks;

    private String imageDir;

    private String imageName;

    private JSObject mainWindow;

    private JTextField entryField;

    private JButton continueButton;

    private JTextArea debugLabel;

    private boolean debug = false;

    public void init() {
        getContentPane().setBackground(Color.white);
        getParams();
        clickPoints = new int[numClicks][2];
        clickCount = 0;
        mainWindow = JSObject.getWindow(this);
        mainWindow.call("resizeTo", new String[] { "" + (this.getWidth() + 40), "" + (this.getHeight() + 95) });
        runScript("../logdata.php?system=" + system + "&user=" + username + "&scheme=passpoints" + "&mode=" + mode + "&event=appletLoaded&data=image:" + imageName);
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    createImageGUI();
                }
            });
        } catch (Exception e) {
            System.err.println("createGUI didn't successfully complete");
        }
    }

    private void getParams() {
        system = getParameter("system");
        username = getParameter("user");
        mode = getParameter("mode");
        imageName = getParameter("imageName");
        imageDir = getParameter("imageDir");
        numClicks = Integer.parseInt(getParameter("numClicks"));
    }

    private void createImageGUI() {
        getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        JLabel imageLabel = new JLabel();
        ImageIcon displayIcon = new ImageIcon(getImage(getCodeBase(), "../" + imageDir + imageName));
        imageLabel.setIcon(displayIcon);
        imageLabel.setSize(displayIcon.getIconWidth(), displayIcon.getIconHeight());
        imageLabel.addMouseListener(clickListener);
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 3;
        c.gridheight = 1;
        c.ipady = 2;
        getContentPane().add(imageLabel, c);
        if (debug) {
            JLabel systemLabel = new JLabel("System: " + system);
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.ipadx = 10;
            getContentPane().add(systemLabel, c);
            JLabel modeLabel = new JLabel("Mode: " + mode);
            c.gridx = 1;
            c.gridy = 0;
            c.gridwidth = 1;
            c.gridheight = 1;
            getContentPane().add(modeLabel, c);
            JLabel usernameLabel = new JLabel("Username: " + username);
            c.gridx = 2;
            c.gridy = 0;
            c.gridwidth = 1;
            c.gridheight = 1;
            getContentPane().add(usernameLabel, c);
            debugLabel = new JTextArea("../" + imageDir + imageName);
            c.gridx = 0;
            c.gridy = 3;
            c.gridwidth = 3;
            c.gridheight = 1;
            c.fill = GridBagConstraints.BOTH;
            getContentPane().add(debugLabel, c);
        }
        JButton clearButton = new JButton("Reset Clicks");
        clearButton.addActionListener(clearListener);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.ipadx = 10;
        c.fill = GridBagConstraints.HORIZONTAL;
        getContentPane().add(clearButton, c);
    }

    private MouseListener clickListener = new MouseListener() {

        public void mouseMoved(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseClicked(MouseEvent e) {
            if (clickCount < numClicks) {
                clickPoints[clickCount][0] = e.getX();
                clickPoints[clickCount][1] = e.getY();
                runScript("../logdata.php?system=" + system + "&user=" + username + "&scheme=passpoints" + "&mode=" + mode + "&event=click&data=x:" + e.getX() + ",y:" + e.getY());
                if (mode.equals("create")) {
                    Graphics g = e.getComponent().getGraphics();
                    g.setColor(new Color(255, 255, 255, 128));
                    g.fillRect(e.getX() - 10, e.getY() - 10, 20, 20);
                    g.setColor(Color.BLACK);
                    g.drawString((clickCount + 1) + "", e.getX() - 4, e.getY() + 5);
                }
                clickCount++;
                if (clickCount >= numClicks) submitClicks();
            }
        }
    };

    private ActionListener clearListener = new ActionListener() {

        public void actionPerformed(ActionEvent e) {
            clickPoints = new int[numClicks][2];
            clickCount = 0;
            continueButton.setEnabled(false);
            repaint();
            runScript("../logdata.php?system=" + system + "&user=" + username + "&scheme=passpoints" + "&mode=" + mode + "&event=reset");
        }
    };

    private void submitData(String data) {
        String[] submitArgs = new String[2];
        submitArgs[0] = data;
        submitArgs[1] = mode;
        mainWindow.call("passresult", submitArgs);
    }

    private void submitClicks() {
        String data = runScript("submitclicks.php" + "?clicks=" + getClicksString() + "&mode=" + mode + "&username=" + username + "&system=" + system + "&imageName=" + imageName);
        debugLabel.setText(data);
        runScript("../logdata.php?system=" + system + "&user=" + username + "&scheme=passpoints" + "&mode=" + mode + "&event=passwordSubmitted&data=pw:" + data);
        submitData(data);
    }

    private String getClicksString() {
        String temp = "";
        for (int i = 0; i < clickPoints.length; i++) {
            temp += clickPoints[i][0];
            temp += ",";
            temp += clickPoints[i][1];
            if (i < clickPoints.length - 1) temp += ",";
        }
        return temp;
    }

    String runScript(String scriptName) {
        String data = "";
        try {
            URL url = new URL(getCodeBase().toString() + scriptName);
            InputStream in = url.openStream();
            BufferedInputStream buffIn = new BufferedInputStream(in);
            do {
                int temp = buffIn.read();
                if (temp == -1) break;
                data = data + (char) temp;
            } while (true);
        } catch (Exception e) {
            data = "error!";
        }
        return data;
    }
}
