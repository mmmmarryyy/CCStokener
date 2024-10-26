package cxtable.peer;

import cxtable.*;
import cxtable.core_comm.*;
import cxtable.setup_boot.*;
import cxtable.gui.*;
import java.net.*;
import java.io.*;

public class xClientLogManager extends Thread implements xListener, xNameListen, xBootListener, xSetupBootListener {

    private static final String[] key = new String[] { "<COMMAND>" };

    private String where, name, ipad, port;

    private boolean debug = false;

    private xClientConn xcc;

    private xRegistry xrego, for_serv;

    private xLineSplit xls = new xLineSplit();

    private String mssg;

    private xNameListen xnl;

    private int fail = 0;

    String[] servers;

    private xReadSorter xres;

    private String reg_id;

    private xMiniServer xmin;

    private long read_delay = 400;

    private xPanel rept;

    private boolean from_file;

    private String err_catch = "";

    public xClientLogManager(xRegistry for_clients, xRegistry servers, String w, String ip, String p, xReadSorter xreads, xMiniServer xmins, xNameListen x_nl, boolean b) {
        xrego = for_clients;
        xres = xreads;
        for_serv = servers;
        where = w;
        ipad = ip;
        port = p;
        xmin = xmins;
        xnl = x_nl;
        from_file = b;
    }

    public void setReport(xPanel x) {
        rept = x;
    }

    public void run() {
        if (from_file == true) {
            new Thread(new xSetupBootPopup(false, this)).start();
        } else {
            new Thread(new xSocketLogin(this)).start();
        }
    }

    public String who() {
        return "xClientLogManager";
    }

    public void login_name(String s) {
        name = s;
        long l = (long) (Math.random() * 100000);
        l = l + 10000;
        reg_id = new String(name + l);
        xmin.setLabel(reg_id);
        xmin.set_Name(name);
        xnl.login_name(s);
        mssg = "<LOGIN><NAME>" + name + "</NAME><IP>" + ipad + "</IP><PORT>" + port + "</PORT><REG_ID>" + reg_id + "</REG_ID></LOGIN>";
        run_two();
    }

    public void setClientDepository(xRegistry xr) {
        xrego = xr;
    }

    public xRegistry getClientDepository() {
        return xrego;
    }

    public void run_two() {
        System.out.println("Waiting to see if server has logged in");
        if (rept != null) {
            rept.post("Checking if RegServer is online..");
        }
        try {
            boolean waiting = true;
            while (waiting) {
                String con = "";
                String s;
                URL url = new URL(where);
                BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                System.out.print("Checking for xRegistry..");
                if (rept != null) {
                    rept.post("Searching for xRegistryServer..");
                }
                while ((s = br.readLine()) != null) {
                    con = con + s;
                }
                err_catch = con;
                try {
                    br.close();
                } catch (IOException ioe) {
                    System.out.print("..b_r wouldn't close..");
                }
                servers = xls.split("SERVER", con);
                if (servers.length < 1) {
                    System.out.println("-->Server not available yet.");
                    if (rept != null) {
                        rept.post("-->Server currently unavailable..");
                    }
                    try {
                        Thread.sleep(read_delay);
                    } catch (Exception inter) {
                    }
                    if (read_delay < 20000) {
                        read_delay = read_delay * 2;
                        if (debug == true) {
                            System.out.println("Set read_delay to:" + read_delay);
                        }
                    } else {
                        read_delay = read_delay + 3000;
                    }
                    if (read_delay > 90000) {
                        waiting = false;
                    }
                }
                if (servers.length > 0) {
                    waiting = false;
                }
            }
            String[] regip = xls.split("IP", servers[0]);
            String[] regport = xls.split("PORT", servers[0]);
            int rp = 0;
            try {
                rp = Integer.parseInt(regport[0]);
            } catch (NumberFormatException nfe) {
                rp = 0;
            }
            System.out.println("Trying for socket on " + regip[0] + ", port:" + rp);
            if (rept != null) {
                rept.post("Connecting to RegServer");
            }
            int f = 0;
            Socket client = new Socket(InetAddress.getByName(regip[0]), rp);
            System.out.println("Socket connected to xRegistry");
            if (rept != null) {
                rept.post("Connected to RegServer");
            }
            xcc = new xClientConn(client);
            xcc.set_IP(ipad);
            xcc.set_Port(port);
            xcc.setListen(this);
            new Thread(xcc).start();
        } catch (Exception e) {
            System.out.println("Failed client connection to registry, 'Java' errorcodes:\n" + e.toString() + "\n\nMessage from server?:\n" + err_catch);
            if (rept != null) {
                rept.post("Failed at registry connect!");
            }
            if (debug == true) {
                e.printStackTrace();
            }
            term();
        }
    }

    public void term() {
        if (rept != null) {
            rept.post("Preparing to close program");
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
            }
        }
        System.err.println("Failure, sending terminate");
        System.out.println("Terminating main thread.");
        System.exit(1);
    }

    public void read(String s) {
        String[] cmmds = xls.split("COMMAND", s);
        if (cmmds.length < 1) {
            return;
        }
        for (int i = 0; i < cmmds.length; i++) {
            System.out.println("Command:" + cmmds[i]);
            if (cmmds[i].equalsIgnoreCase("LOGIN") == true) {
                xcc.send(mssg);
            }
            if (cmmds[i].equalsIgnoreCase("LOGGED") == true) {
                xClientContinual xco = new xClientContinual(xcc, xrego, for_serv, xres, name, reg_id);
                xco.setReport(rept);
                xco.setIP_Port(ipad, port);
                new Thread(xco).start();
                xmin.setContinual(xco);
                xcc.offListen(this);
                xPeerEnableListen xpeel = new xPeerEnableListen(reg_id, name, for_serv);
                xpeel.setReport(rept);
                xres.addxRead(xpeel);
            }
            if (cmmds[i].equalsIgnoreCase("FAIL") == true) {
                fail++;
                if (fail > 6) {
                    System.out.println("Failed at login");
                    System.exit(0);
                }
                xcc.send(mssg);
            }
        }
    }

    public String[] readKeys() {
        return key;
    }

    public boolean readAll() {
        return false;
    }

    public void boot(xFTPData x) {
    }

    public void boot(xHTTPData x) {
        where = x.getHTTP();
        login_name(x.getUser());
    }

    public void boot(xSetupData x) {
        if (x.getDataType() != 1) {
            System.out.println("Bad or corrupt setup data for PHP/HTTP 3-5-02 version");
            System.exit(0);
        }
        String[] elems = x.getElements();
        where = elems[0];
        login_name(elems[1]);
    }
}
