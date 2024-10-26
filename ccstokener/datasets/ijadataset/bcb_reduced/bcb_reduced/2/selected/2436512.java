package com.trapezium.chisel;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import com.trapezium.util.ProgramYek;
import com.trapezium.chisel.gui.UnlockDialog;
import com.trapezium.chisel.gui.AboutDialog;
import com.trapezium.chisel.gui.AboutPanel;

/** Access manager for Chisel.
 *
 *  There are three kinds of access:
 *
 *       REFUSED    Evaluation copy, evaluation has expired
 *
 *
 *
 *
 *  @author          Michael St. Hippolyte
 *  @version         1.0, 30 Jan 1999
 *
 *  @since           1.0
 */
public class ChiselAccess {

    public static final int ACCESS_ALLOWED = -1;

    public static final int ACCESS_REFUSED = 0;

    public static final int ACCESS_EVALUATION = 1;

    private String appname;

    private int access;

    private int days;

    private String username;

    private String email;

    private String key;

    private String checkFilename;

    private String keyFilename;

    public ChiselAccess(String appname, String keyDirectory) {
        this.appname = appname;
        if (keyDirectory == null) {
            keyDirectory = "";
        }
        checkFilename = keyDirectory + appname + ".key";
        keyFilename = keyDirectory + ProgramYek.getVidSys();
        ProgramYek yek = getYek(keyFilename);
        if (yek != null && yek.isValid()) {
            initFields(yek);
        } else {
            days = -1;
            access = ACCESS_REFUSED;
            username = "";
            email = "";
            key = "";
        }
    }

    public int daysLeft() {
        return days;
    }

    public int getAccess() {
        return access;
    }

    public String getUserName() {
        return username;
    }

    public String getUserEmail() {
        return email;
    }

    public String getKey() {
        return key;
    }

    public boolean accessAllowed() {
        return (access != ACCESS_REFUSED);
    }

    public boolean fullAccessAllowed() {
        return (access == ACCESS_ALLOWED);
    }

    public boolean evaluationAccess() {
        return (access == ACCESS_EVALUATION);
    }

    public boolean setFields(String name, String email, String key) {
        if (email == null || email.length() < 5) {
            return false;
        }
        int n = email.indexOf('@');
        if (n == -1 || email.indexOf('.', n) == -1) {
            return false;
        }
        if (name == null || name.length() < 1) {
            return false;
        }
        if (key == null || key.length() != ProgramYek.KEYLEN) {
            return false;
        }
        ProgramYek newYek = new ProgramYek(appname, name, email);
        if (newYek.isValid() && newYek.isKey(key)) {
            initFields(newYek);
            File f = new File(keyFilename);
            try {
                FileOutputStream fos = new FileOutputStream(f);
                newYek.write(fos);
                fos.close();
            } catch (Exception e) {
                System.out.println("** Exception writing key: " + e);
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    private void initFields(ProgramYek yek) {
        days = yek.daysLeft();
        if (days > 0) {
            access = ACCESS_EVALUATION;
        } else if (days == 0) {
            access = ACCESS_REFUSED;
        } else {
            access = ACCESS_ALLOWED;
        }
        username = yek.getUserName();
        email = yek.getUserEmail();
        key = yek.toString();
    }

    private ProgramYek getYek(String keyFilename) {
        File f = new File(keyFilename);
        InputStream is = null;
        try {
            is = new FileInputStream(f);
        } catch (java.io.FileNotFoundException ee) {
        } catch (Exception e) {
            System.out.println("** Exception reading key: " + e);
        }
        if (is == null) {
            try {
                URL url = ChiselResources.getResourceByName(ProgramYek.getVidSys(), ChiselResources.LOADFROMCLASSPATH);
                if (url == null) {
                } else {
                    is = url.openStream();
                }
            } catch (Exception e) {
                System.out.println("** Exception reading key: " + e);
            }
        }
        ProgramYek y = null;
        if (is != null) {
            try {
                y = ProgramYek.read(is);
            } catch (Exception e) {
                System.out.println("** Exception reading key: " + e);
            }
        } else {
            File chk = new File(checkFilename);
            if (chk.exists()) {
                System.out.println("This is the evaluation version of " + appname);
                y = new ProgramYek(appname, "Evaluation", "", 15);
                ProgramYek.serialize(y, keyFilename);
            }
        }
        return y;
    }
}
