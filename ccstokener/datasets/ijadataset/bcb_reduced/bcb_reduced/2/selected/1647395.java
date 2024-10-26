package de.fmf.deployhelper.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import de.fmf.deployhelper.Server;
import de.fmf.deployhelper.ServerMaster;
import de.fmf.deployhelper.Tomcat;
import de.fmf.deployhelper.XmlReader;
import de.fmf.error.Info;
import de.fmf.network.ErrorReporter;
import de.fmf.network.InfoServer;
import de.fmf.ui.swt.GuiStatePers;

public class DeployHelper {

    private static final String APP_TITLE = "DeployHelper";

    private static GuiStatePers guiPer;

    private static DeployHelper thisClass;

    private static Display display;

    private static ServerMaster serverMaster;

    private static org.eclipse.swt.widgets.Shell sShell = null;

    private Image icon;

    private Composite c0;

    private static ArrayList<Label> checkCats = new ArrayList<Label>();

    static ArrayList<Text> checkCatsInfo = new ArrayList<Text>();

    ThreadGroup tg = new ThreadGroup("tomcatCheck");

    ArrayList<Check> at = new ArrayList<Check>();

    private static String userProfileDir = (System.getenv().get("APPDATA") != null) ? System.getenv().get("APPDATA") : "";

    private static File userSettingsDir = new File(userProfileDir, "fma" + File.separator + "deployhelper");

    public static void main(String[] args) {
        userSettingsDir.mkdirs();
        guiPer = new GuiStatePers(new File(userSettingsDir, "guiState"));
        display = org.eclipse.swt.widgets.Display.getDefault();
        thisClass = new DeployHelper();
        thisClass.createSShell();
        thisClass.sShell.open();
        setSize("on");
        checkTomcats();
        while (!thisClass.sShell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
        display.dispose();
    }

    private static void setSize(String action) {
        if (action.equals("on")) {
            Integer xpos = new Integer(guiPer.getGuiStateI(GuiStatePers.XPOS));
            Integer ypos = new Integer(guiPer.getGuiStateI(GuiStatePers.YPOS));
            Integer width = new Integer(guiPer.getGuiStateI(GuiStatePers.WIDTH));
            Integer height = new Integer(guiPer.getGuiStateI(GuiStatePers.HEIGHT));
            sShell.setBounds(xpos, ypos, 0, 0);
            boolean break1 = false;
            boolean break2 = false;
            int i = 2;
            while (!(break1 && break2)) {
                if (!(sShell.getSize().x >= width)) sShell.setSize(sShell.getSize().x + i, sShell.getSize().y); else break1 = true;
                if (!(sShell.getSize().y >= height)) sShell.setSize(sShell.getSize().x, sShell.getSize().y + i); else break2 = true;
                i = i + 5;
                if (i > 10000) {
                    break1 = true;
                    break2 = true;
                }
            }
            sShell.setSize(width, height);
        }
        if (action.equals("off")) {
            boolean break1 = false;
            boolean break2 = false;
            int i = 2;
            while (!(break1 && break2)) {
                if (!(sShell.getSize().x < 200)) sShell.setSize(sShell.getSize().x - i, sShell.getSize().y); else break1 = true;
                if (!(sShell.getSize().y < 200)) sShell.setSize(sShell.getSize().x, sShell.getSize().y - i); else break2 = true;
                i = i + 5;
                if (i > 10000) {
                    break1 = true;
                    break2 = true;
                }
            }
        }
    }

    private static void readServers() {
        File utTestFile = new File("servers.xml");
        XmlReader xx = new XmlReader(utTestFile);
        serverMaster = xx.readXml(new ServerMaster());
    }

    private void createSShell() {
        sShell = new org.eclipse.swt.widgets.Shell();
        sShell.setBounds(0, 0, 0, 0);
        FillLayout sShellLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
        sShell.setLayout(sShellLayout);
        sShell.setText(APP_TITLE);
        icon = new Image(display, getClass().getResourceAsStream("/fm/unitrans.ico"));
        sShell.setImage(icon);
        {
            c0 = new Composite(sShell, SWT.NONE);
            GridLayout c0Layout = new GridLayout();
            c0Layout.numColumns = 7;
            c0.setLayout(c0Layout);
            readServers();
            createControls();
            makeControlThreads();
            c0.layout();
        }
        sShell.addShellListener(new ShellAdapter() {

            public void shellClosed(ShellEvent evt) {
                try {
                    for (Iterator<Check> iterator = at.iterator(); iterator.hasNext(); ) {
                        Check tx = iterator.next();
                        tx.endComm();
                    }
                    tg.interrupt();
                    guiPer.addItem(GuiStatePers.HEIGHT, sShell.getBounds().height + "");
                    guiPer.addItem(GuiStatePers.WIDTH, sShell.getBounds().width + "");
                    guiPer.addItem(GuiStatePers.XPOS, sShell.getBounds().x + "");
                    guiPer.addItem(GuiStatePers.YPOS, sShell.getBounds().y + "");
                    setSize("off");
                    guiPer.writeGuiStateToFile();
                    Info.reportInfo("DEPLOYHELPER FINISHED#" + InetAddress.getLocalHost().getHostName());
                } catch (Exception e) {
                    Info.reportError(e, this.getClass().getName());
                }
            }
        });
        try {
            Info.reportInfo("DEPLOYHELPER STARTET#" + InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {
            Info.reportError(e, this.getClass().getName());
        }
    }

    private void makeControlThreads() {
        for (Iterator<Label> iterator = checkCats.iterator(); iterator.hasNext(); ) {
            Label lbl = iterator.next();
            Server server = (Server) lbl.getData(ServerMaster.SERVER);
            Tomcat tomcat = (Tomcat) lbl.getData(ServerMaster.TOMCAT);
            Check c = new Check(server, tomcat);
            Thread t = new Thread(tg, c);
            at.add(c);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        }
    }

    private static void checkTomcats() {
        Runnable timer = new Runnable() {

            public void run() {
                for (Iterator<Label> iterator = checkCats.iterator(); iterator.hasNext(); ) {
                    Label lbl = iterator.next();
                    Server server = (Server) lbl.getData(ServerMaster.SERVER);
                    Tomcat tomcat = (Tomcat) lbl.getData(ServerMaster.TOMCAT);
                    boolean isAlive = tomcat.getIsServerAlive();
                    if (isAlive) lbl.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN)); else lbl.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
                }
                for (Iterator<Text> iterator2 = checkCatsInfo.iterator(); iterator2.hasNext(); ) {
                    Text txt = iterator2.next();
                    Server server = (Server) txt.getData(ServerMaster.SERVER);
                    Tomcat tomcat = (Tomcat) txt.getData(ServerMaster.TOMCAT);
                    String info = tomcat.getDetailInfo();
                    txt.setText(info);
                }
                sShell.getDisplay().timerExec(5000, this);
            }
        };
        sShell.getDisplay().timerExec(1000, timer);
    }

    private void sendInterrupts(int watitBeforeSendTime) {
        Runnable timer = new Runnable() {

            public void run() {
                tg.interrupt();
            }
        };
        sShell.getDisplay().timerExec(watitBeforeSendTime, timer);
    }

    private void createControls() {
        GridData gd = new GridData();
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        HashMap<String, Server> servers = serverMaster.getServers();
        Set<String> servs = servers.keySet();
        int controlSetCounter = 0;
        for (Iterator<String> iterator = servs.iterator(); iterator.hasNext(); ) {
            String sss = iterator.next();
            Server server = servers.get(sss);
            HashMap<String, Tomcat> tomcats = server.getTomcats();
            Set<String> toms = tomcats.keySet();
            for (Iterator<String> iterator2 = toms.iterator(); iterator2.hasNext(); ) {
                String tx = iterator2.next();
                Tomcat tomcat = tomcats.get(tx);
                Label lbl = new Label(c0, SWT.NONE);
                lbl.setText(tomcat.getName() + " - " + server.getIp());
                lbl.setFont(new Font(null, "Arial", 9, SWT.BOLD));
                lbl.setData(ServerMaster.SERVER, server);
                lbl.setData(ServerMaster.TOMCAT, tomcat);
                checkCats.add(lbl);
                Button btnClean = new Button(c0, SWT.PUSH | SWT.CENTER);
                btnClean.setText("clean");
                btnClean.setData(ServerMaster.SERVER, server);
                btnClean.setData(ServerMaster.TOMCAT, tomcat);
                btnClean.setLayoutData(gd);
                btnClean.setToolTipText(tomcat.getName());
                btnClean.addSelectionListener(new SelectionAdapter() {

                    public void widgetSelected(SelectionEvent evt) {
                        Button src = (Button) evt.getSource();
                        serverMaster.cleanTomcat((Server) src.getData(ServerMaster.SERVER), (Tomcat) src.getData(ServerMaster.TOMCAT));
                        sendInterrupts(5000);
                    }
                });
                Button button2 = new Button(c0, SWT.PUSH | SWT.CENTER);
                button2.setText("start");
                button2.setData(ServerMaster.SERVER, server);
                button2.setData(ServerMaster.TOMCAT, tomcat);
                button2.setLayoutData(gd);
                button2.setToolTipText(tomcat.getName());
                button2.addSelectionListener(new SelectionAdapter() {

                    public void widgetSelected(SelectionEvent evt) {
                        Button src = (Button) evt.getSource();
                        serverMaster.startTomcat((Server) src.getData(ServerMaster.SERVER), (Tomcat) src.getData(ServerMaster.TOMCAT));
                        sendInterrupts(10000);
                    }
                });
                Button button3 = new Button(c0, SWT.PUSH | SWT.CENTER);
                button3.setText("stop");
                button3.setData(ServerMaster.SERVER, server);
                button3.setData(ServerMaster.TOMCAT, tomcat);
                button3.setLayoutData(gd);
                button3.setToolTipText(tomcat.getName());
                button3.addSelectionListener(new SelectionAdapter() {

                    public void widgetSelected(SelectionEvent evt) {
                        Button src = (Button) evt.getSource();
                        serverMaster.stopTomcat((Server) src.getData(ServerMaster.SERVER), (Tomcat) src.getData(ServerMaster.TOMCAT));
                        sendInterrupts(5000);
                    }
                });
                Button button4 = new Button(c0, SWT.PUSH | SWT.CENTER);
                button4.setText("restart");
                button4.setData(ServerMaster.SERVER, server);
                button4.setData(ServerMaster.TOMCAT, tomcat);
                button4.setLayoutData(gd);
                button4.setToolTipText(tomcat.getName());
                button4.addSelectionListener(new SelectionAdapter() {

                    public void widgetSelected(SelectionEvent evt) {
                        Button src = (Button) evt.getSource();
                        serverMaster.restartTomcat((Server) src.getData(ServerMaster.SERVER), (Tomcat) src.getData(ServerMaster.TOMCAT));
                        sendInterrupts(10000);
                    }
                });
                Button button5 = new Button(c0, SWT.PUSH | SWT.CENTER);
                button5.setText("test");
                button5.setData(ServerMaster.SERVER, server);
                button5.setData(ServerMaster.TOMCAT, tomcat);
                button5.setLayoutData(gd);
                button5.setToolTipText(tomcat.getName());
                button5.addSelectionListener(new SelectionAdapter() {

                    public void widgetSelected(SelectionEvent evt) {
                        Button src = (Button) evt.getSource();
                        serverMaster.testTomcat((Server) src.getData(ServerMaster.SERVER), (Tomcat) src.getData(ServerMaster.TOMCAT));
                    }
                });
                Text text1 = new Text(c0, SWT.NONE);
                text1.setData(ServerMaster.SERVER, server);
                text1.setData(ServerMaster.TOMCAT, tomcat);
                text1.setData("cs_counter", controlSetCounter);
                checkCatsInfo.add(text1);
                GridData text1xLData = new GridData();
                text1xLData.horizontalAlignment = GridData.FILL;
                text1xLData.widthHint = 180;
                text1.setLayoutData(text1xLData);
                text1.setText("no info");
                String col = guiPer.getGuiStateI("txt_color_" + tomcat.getName() + " - " + server.getIp());
                if (col != null && !col.equals("")) {
                    try {
                        String[] cols = col.split(":");
                        RGB rgb = new RGB(Integer.parseInt(cols[0]), Integer.parseInt(cols[1]), Integer.parseInt(cols[2]));
                        text1.setBackground(new Color(Display.getCurrent(), rgb));
                    } catch (Exception e) {
                        Info.reportError(e, this.getClass().getName());
                    }
                }
                text1.addMouseListener(new org.eclipse.swt.events.MouseListener() {

                    public void mouseDown(org.eclipse.swt.events.MouseEvent e) {
                    }

                    public void mouseDoubleClick(org.eclipse.swt.events.MouseEvent e) {
                        Text t = (Text) e.getSource();
                        ColorDialog cd = new ColorDialog(sShell);
                        cd.setText(APP_TITLE);
                        cd.setRGB(new RGB(255, 255, 255));
                        RGB newColor = cd.open();
                        if (newColor == null) {
                            return;
                        }
                        t.setBackground(new Color(Display.getCurrent(), newColor));
                        int blue = t.getBackground().getBlue();
                        int green = t.getBackground().getGreen();
                        int red = t.getBackground().getRed();
                        guiPer.addItem("txt_color_" + ((Tomcat) t.getData("tomcat")).getName() + " - " + ((Server) t.getData("server")).getIp(), red + ":" + green + ":" + blue);
                    }

                    public void mouseUp(org.eclipse.swt.events.MouseEvent e) {
                    }
                });
                controlSetCounter++;
            }
        }
    }
}

class Check implements Runnable {

    private Tomcat tomcat;

    private Server server;

    private boolean run;

    public Check(Server server, Tomcat tomcat) {
        this.server = server;
        this.tomcat = tomcat;
        this.run = true;
    }

    public void endComm() {
        this.run = false;
    }

    @Override
    public void run() {
        while (run) {
            try {
                URL url = new URL("http://" + server.getIp() + "/" + tomcat.getName() + "/ui/pva/version.jsp?RT=" + System.currentTimeMillis());
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), Charset.forName("UTF-8")));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.contains("currentversion")) {
                        String s = inputLine.substring(inputLine.indexOf("=") + 1, inputLine.length());
                        tomcat.setDetailInfo(s.trim());
                    }
                }
                in.close();
                tomcat.setIsAlive(true);
            } catch (Exception e) {
                tomcat.setIsAlive(false);
            }
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
            }
        }
    }
}
