package org.fudaa.fudaa.tr;

import java.awt.Frame;
import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.ListModel;
import com.memoire.bu.*;
import com.memoire.fu.Fu;
import com.memoire.fu.FuLib;
import com.memoire.fu.FuLog;
import org.fudaa.ctulu.CtuluLib;
import org.fudaa.ctulu.CtuluLibString;
import org.fudaa.ctulu.gui.CtuluLibDialog;
import org.fudaa.ctulu.gui.CtuluOptionPane;
import org.fudaa.fudaa.commun.FudaaBrowserControl;
import org.fudaa.fudaa.commun.FudaaLib;
import org.fudaa.fudaa.tr.common.TrLib;
import org.fudaa.fudaa.tr.common.TrResource;

/**
 * @author fred deniger
 * @version $Id: TrSoftUpdater.java,v 1.12 2007-05-04 14:01:52 deniger Exp $
 */
public class TrSoftUpdater {

    BuCommonImplementation impl_;

    final BuInformationsSoftware infos_;

    boolean isConnected_;

    boolean isUptoDate_;

    int nbTry_ = 7;

    final String updateURL_;

    int wait_ = 1000;

    public TrSoftUpdater(final BuInformationsSoftware _infos) {
        super();
        infos_ = _infos;
        updateURL_ = TrLib.getMajURl(infos_);
        BuPreferences.BU.applyNetwork();
    }

    void afficheDialogueForUpdate(final String _version) {
        final String title = TrResource.getS("Mise � jour de Fudaa-Prepro:") + CtuluLibString.ESPACE + _version;
        final String message = "<html><body><p>" + TrResource.getS("Une nouvelle version est disponible:") + "<b>&nbsp;" + _version + "</b>.</p><p>" + TrResource.getS("Cliquer {0} pour voir les modifications apport�es � la derni�re version.", "<a href=\"GO_UPDATE\">" + TrResource.getS("ici") + "</a>") + "</p><p>" + TrResource.getS("Pour utiliser la nouvelle version, vous devrez fermer toutes les applications de Fudaa-Prepro.") + "<br>" + TrResource.getS("Pour cela, vous pouvez utiliser directement l'option ci-dessous <br> ou le menu 'Fichier>Fermer toutes les applications'.") + "<p><b>" + TrResource.getS("Mettre � jour en arri�re-plan:") + "</b><br>" + TrResource.getS("La nouvelle version sera t�l�charg�e et install�e en tache de fond. Elle sera utilis�e au prochain red�marrage de Fudaa Prepro.") + "</p>";
        final boolean isJnlp = TrLib.isJnlp();
        final String closeAll = TrResource.getS("Fermer toutes les applications et mettre � jour");
        final String ignore = FudaaLib.getS("Ignorer");
        final String background = TrResource.getS("Mettre � jour en arri�re-plan");
        final String ignoreAndForget = TrResource.getS("Ignorer et ne plus demander");
        final String[] options = isJnlp ? new String[] { closeAll, background, ignoreAndForget, ignore } : new String[] { closeAll, ignoreAndForget, ignore };
        final CtuluOptionPane pane = new CtuluOptionPane(message, JOptionPane.INFORMATION_MESSAGE) {

            @Override
            protected void linkActivated(final String _url) {
                if (_url == null) {
                    new Thread() {

                        @Override
                        public void run() {
                            showUpdateModification(_version);
                        }
                    }.start();
                } else {
                    super.linkActivated(_url);
                }
            }
        };
        pane.setOptions(options);
        final JDialog d = pane.createDialog(getFrame(), title);
        d.setResizable(true);
        d.setModal(true);
        d.pack();
        TrLauncherDefault.updateDial_ = d;
        d.show();
        d.dispose();
        TrLauncherDefault.updateDial_ = null;
        final Object o = pane.getValue();
        final String preproJnlp = "http://fudaa.fr/install/prepro/prepro.jnlp";
        final boolean isJava16 = "1.6".compareTo(FuLib.getJavaVersion()) <= 0;
        if (options[0].equals(o)) {
            Object tmp = null;
            final boolean onlySup = isOnlySupervisor();
            if (!onlySup) {
                tmp = new Object();
                BuRegistry.register(tmp);
                closeAll();
            }
            if (onlySup || (tmp != null && BuRegistry.getModel().getSize() == 1 && tmp.equals(BuRegistry.getModel().get(0)))) {
                if (TrLib.isJnlp()) {
                    try {
                        String[] strings = new String[] { TrLib.getJavaws(), "-online", preproJnlp };
                        if (isJava16) {
                            strings = new String[] { TrLib.getJavaws(), preproJnlp };
                        }
                        Runtime.getRuntime().exec(strings);
                    } catch (final IOException _evt) {
                        FudaaBrowserControl.displayURL(updateURL_);
                        FuLog.error(_evt);
                    }
                }
                System.exit(0);
            } else {
                BuRegistry.unregister(tmp);
                FudaaBrowserControl.displayURL(updateURL_);
            }
        } else if (isJnlp && options[1].equals(o)) {
            if (TrLib.isJnlp()) {
                try {
                    Runtime.getRuntime().exec(new String[] { TrLib.getJavaws(), "-import", "-silent", preproJnlp });
                } catch (final IOException _evt) {
                    FudaaBrowserControl.displayURL(updateURL_);
                    FuLog.error(_evt);
                }
            } else {
                FudaaBrowserControl.displayURL(updateURL_);
            }
        } else if (options[options.length - 2].equals(o)) {
            BuPreferences.BU.putBooleanProperty("check.update", false);
            BuPreferences.BU.writeIniFile();
            CtuluLibDialog.showMessage(getFrame(), TrResource.getS("V�rification des mises � jour au d�marrage"), TrResource.getS("Pour activer � nouveau cette option, aller dans les pr�f�rences (menu Edition) et\n choisir l'onglet 'Syst�me' et 'D�marrage/sortie'"));
        }
        if (!options[0].equals(o)) {
            TrLauncherDefault.feedback();
        }
    }

    private Frame getFrame() {
        return impl_ == null ? BuLib.HELPER : impl_.getFrame();
    }

    protected void showUpdateModification(final String _version) {
        InputStream in = null;
        LineNumberReader reader = null;
        try {
            final String lang = CtuluLib.isFrenchLanguageSelected() ? "fr" : "en";
            final URL url = new URL("http://www.fudaa.fr/prepro/inc.last-changelog." + lang + ".html");
            final URLConnection connection = url.openConnection();
            for (int i = 0; i < nbTry_ && in == null; i++) {
                connection.connect();
                in = url.openStream();
                try {
                    if (in == null) {
                        Thread.sleep(wait_);
                    }
                } catch (final InterruptedException _evt) {
                }
            }
            if (in == null) {
                BuBrowserControl.displayURL(updateURL_);
            } else {
                final StringBuffer buf = new StringBuffer(300);
                buf.append("<html><body>");
                reader = new LineNumberReader(new InputStreamReader(in));
                String line = reader.readLine();
                while (line != null) {
                    buf.append(line);
                    line = reader.readLine();
                }
                buf.append("</body></html>");
                CtuluLibDialog.showMessage(getFrame(), TrResource.getS("Modifications apport�es � la version {0}", _version), buf.toString());
            }
        } catch (final IOException _evt) {
        } finally {
            FuLib.safeClose(in);
            FuLib.safeClose(reader);
        }
    }

    public void closeAll() {
        if (impl_ != null) {
            impl_.exit();
        }
        final ListModel model = BuRegistry.getModel();
        final int nb = model.getSize() - 1;
        for (int i = 0; i < nb; i++) {
            final Object o = model.getElementAt(i);
            if (o instanceof BuApplication) {
                ((BuApplication) o).exit();
            } else if (o instanceof Window) {
                ((Window) o).hide();
                ((Window) o).dispose();
            }
        }
    }

    public BuCommonImplementation getImpl() {
        return impl_;
    }

    public int getNbTry() {
        return nbTry_;
    }

    public int getWait() {
        return wait_;
    }

    public boolean isConnected() {
        return isConnected_;
    }

    public boolean isOnlySupervisor() {
        if (BuRegistry.getModel().getSize() == 0) {
            return true;
        }
        if (BuRegistry.getModel().getSize() == 1) {
            final Object o = BuRegistry.getModel().get(0);
            return (o instanceof BuApplication) && ((((BuApplication) o).getImplementation()) instanceof TrSupervisorImplementation);
        }
        return false;
    }

    public boolean isUptoDate() {
        return isUptoDate_;
    }

    public void setImpl(final BuCommonImplementation _impl) {
        impl_ = _impl;
    }

    public void setNbTry(final int _nbTry) {
        nbTry_ = _nbTry;
    }

    public void setWait(final int _millsec) {
        wait_ = _millsec;
    }

    public void update() {
        if (Fu.DEBUG && FuLog.isDebug()) {
            FuLog.debug("FTR: update start load properties");
        }
        InputStream in = null;
        try {
            final URL url = new URL("http://www.fudaa.fr/prepro/prepro.properties");
            final URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            for (int i = 0; i < nbTry_ && in == null; i++) {
                connection.connect();
                in = url.openStream();
                try {
                    if (in == null) {
                        Thread.sleep(wait_);
                    }
                } catch (final InterruptedException _evt) {
                }
            }
            if (in == null) {
                isConnected_ = false;
            } else {
                isConnected_ = true;
                final Properties prop = new Properties();
                prop.load(in);
                final String version = prop.getProperty("@version@");
                if (Fu.DEBUG && FuLog.isDebug()) {
                    FuLog.debug("FTR: version read from site " + version);
                }
                if (version != null && version.compareTo(infos_.version) > 0) {
                    isUptoDate_ = false;
                    BuLib.invokeLater(new Runnable() {

                        public void run() {
                            afficheDialogueForUpdate(version);
                        }
                    });
                } else {
                    isUptoDate_ = true;
                }
            }
        } catch (final IOException _evt) {
        } finally {
            FuLib.safeClose(in);
        }
    }
}
