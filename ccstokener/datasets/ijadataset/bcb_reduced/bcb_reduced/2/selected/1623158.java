package controler;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import view.ScanPanel;
import model.ApplicationSignature;
import model.ChallengeFaviconMd5;
import model.ChallengeContainedLinks;
import view.WarssInputDialog;
import model.utils.Pair;

/**
 * Runnable unit that perform a complete scan of
 * a given target.
 */
public class Scan implements Runnable {

    private WarssIO wioControler;

    private SigManager sigManager;

    private URL urlTarget;

    private ScanPanel panel;

    private ApplicationSignature appSig;

    /**
     * Constructor
     *
     * @param wio controler to access the GUI elements
     * @param sm signature manager to access the signatures database
     * @param scanPanel panel to write results and get user inputs if required
     */
    public Scan(WarssIO wio, SigManager sm, ScanPanel scanPanel) {
        wioControler = wio;
        sigManager = sm;
        urlTarget = wio.getTargetURL();
        panel = scanPanel;
        appSig = new ApplicationSignature();
    }

    /**
     * Scan logic implementation
     * Craft a signature for the targetted application
     * compare it with already known ones and print results.
     * Update signatures database if requested
     */
    public void run() {
        if (wioControler == null || sigManager == null || urlTarget == null) {
            panel.writeLine("[!] Bad initialization, QUITTING!!");
            return;
        }
        panel.writeLine("[+] Running...");
        try {
            HttpURLConnection conn = (HttpURLConnection) urlTarget.openConnection();
            conn.connect();
        } catch (ConnectException e) {
            panel.writeLine("[!] Can't open connection (" + e.getMessage() + ")");
            panel.writeLine("[!] Aborted!");
            return;
        } catch (IOException e) {
            panel.writeLine("[!] Unknown target (" + e.getMessage() + ")");
            panel.writeLine("[!] Aborted!");
            return;
        }
        appSig.addChallenge(new ChallengeFaviconMd5(urlTarget), 50);
        appSig.addChallenge(new ChallengeContainedLinks(urlTarget), 50);
        appSig.performTests();
        panel.writeLine("====== SIGNATURE ======");
        panel.writeLine(appSig.computeResult());
        panel.writeLine("=======================");
        Pair<String, String> baseSig = sigManager.getClosestSignature(appSig);
        if (baseSig != null) {
            panel.writeLine("[+] The application is " + baseSig.getA() + ", version :" + baseSig.getB());
        } else {
            String appVersion = null;
            String appName = WarssInputDialog.getWarssInputNameVersion("Application name :");
            if (appName != null) {
                appVersion = WarssInputDialog.getWarssInputNameVersion("Application version :");
            }
            sigManager.addSig(appSig, appName, appVersion);
            sigManager.saveFile(sigManager.getFilename());
            panel.writeLine("[+] The application is " + appName + ", version :" + appVersion);
        }
        panel.writeLine("[+] DEBUG : scan terminated");
    }

    /**
     * accessor
     *
     * @param url targetted url
     */
    public void setURL(URL url) {
        urlTarget = url;
    }

    /**
     * accessor
     *
     * @param as new application signature to use
     */
    public void setAppSignature(ApplicationSignature as) {
        appSig = as;
    }

    /**
     * accessor
     *
     * @return the application signature generated (or being generated!)
     */
    public ApplicationSignature getAppSignature() {
        return appSig;
    }
}
