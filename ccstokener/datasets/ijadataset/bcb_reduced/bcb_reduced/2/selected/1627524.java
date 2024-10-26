package cxtable.registry;

import cxtable.core_comm.*;
import cxtable.gui.*;
import cxtable.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;

public class xRegistryServer extends Thread implements xValidListener, xServerListener {

    private xRegistry xreggy = new xRegistry();

    private String site, user, pw, dir, file;

    private boolean gui;

    private String pigb = "";

    private xEnableServer xes;

    private Frame f;

    private Button b;

    private Button bench = new Button("Request benchmarks");

    xPanel reporter = new xPanel("Reports from Server:", "{}");

    private boolean as_ftp = true;

    private String httpdat = "";

    private String phpservname = "";

    public xRegistryServer(String s, String u, String p, String d, String f, boolean g) {
        site = s;
        user = u;
        pw = p;
        dir = d;
        file = f;
        gui = g;
    }

    public xRegistryServer(String http, String pserb) {
        httpdat = http;
        phpservname = pserb;
        as_ftp = false;
    }

    public xRegistryServer(String http, String pserb, boolean g) {
        gui = g;
        httpdat = http;
        phpservname = pserb;
        as_ftp = false;
    }

    public void run() {
        xreggy.setReport(reporter);
        reporter.setSize(6, 30, true);
        new Thread(new xReDirect("xRegistryLog.cgt")).start();
        if (gui == true) {
            f = new Frame("Registry Server GUI");
            f.addWindowListener(new WindowAdapter() {

                public void windowClosing(WindowEvent we) {
                    dispose();
                }
            });
            b = new Button("Press to bring down server gracefully");
            f.add(b);
            b.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    dispose();
                }
            });
            f.pack();
            f.setVisible(true);
        }
        if (as_ftp) {
            new Thread(new xServSocket(site, user, pw, dir, file, this)).start();
        } else {
            new Thread(new xServSocket(httpdat, phpservname, this)).start();
        }
        new Thread(new xRegistryBroadcaster(xreggy, 60000)).start();
    }

    public void add(Socket s) {
        xClientConn x = new xClientConn(s);
        xValidator xv = new xValidator(x, this);
        xv.setReport(reporter);
        new Thread(x).start();
        new Thread(xv).start();
    }

    public void validated(xClientConn x) {
        new Thread(new xRegistryDispatch(xreggy, x, pigb)).start();
        new Thread(new xAutoProcessor(x, reporter)).start();
    }

    public void regserver(String s, String t) {
        System.out.println("Bringing up xEnableServer");
        xes = new xEnableServer(xreggy, this);
        new Thread(xes).start();
        f.setVisible(false);
        f = null;
        f = new Frame("xRegistry Server..");
        f.setLayout(new BorderLayout());
        f.add(xes.getRef(), BorderLayout.EAST);
        f.add(reporter.create(), BorderLayout.WEST);
        Panel p = new Panel();
        p.setLayout(new BorderLayout());
        p.add(b, BorderLayout.SOUTH);
        p.add(bench, BorderLayout.NORTH);
        bench.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                bench();
            }
        });
        f.add(p, BorderLayout.SOUTH);
        f.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent we) {
                dispose();
            }
        });
        f.pack();
        f.show();
    }

    public void dispose() {
        if (f != null) {
            f.setVisible(false);
        }
        f = null;
        System.out.print("Erasing http-site presence...");
        if (as_ftp) {
            xLinlyn xlin = new xLinlyn(site, user, pw, dir);
            try {
                xlin.erase(file);
                System.out.println("..Success!");
            } catch (Exception e) {
                System.out.println("...failed!");
            }
            System.out.println("Done");
        } else {
            try {
                URL url = new URL(httpdat + "REM:" + phpservname);
                BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                String s = "";
                System.out.println("Removing php-entry for:" + phpservname + "\nRESPONSE\n==================\n");
                while ((s = br.readLine()) != null) {
                    System.out.println(s);
                }
                br.close();
            } catch (Exception ez) {
                System.out.println("Error at performing remove-->failed");
            }
            System.out.println("Done w/ php");
        }
        System.exit(0);
    }

    public void setPiggyBack(String s) {
        pigb = s;
    }

    private void bench() {
        xClientConn[] cli = xreggy.on();
        if (cli.length < 1) {
            reporter.post("No peers to benchmark");
            System.out.println("Bench called on xReg w/ no peers");
            return;
        }
        try {
            for (int i = 0; i < cli.length; i++) {
                try {
                    long l = System.currentTimeMillis();
                    cli[i].send("<BENCHF>Benchmarking message... for return trip measurement<START>" + l + "</START></BENCHF>");
                } catch (Exception ee) {
                    String m = "Benchmark to " + cli[i].getRegID() + " failed.";
                    reporter.post(m);
                    System.out.println(m);
                }
            }
        } catch (Exception e) {
            System.out.println("Error in benchmarking, unrecoverable");
        }
        return;
    }

    public static void main(String[] args) {
        new Thread(new xRegistryServer("http://cxtable.sourceforge.net/registry.php?", "Mr_Bubbs", true)).start();
    }
}
