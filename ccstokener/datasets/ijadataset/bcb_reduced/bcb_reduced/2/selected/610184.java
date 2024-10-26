package jaxe.elements;

import org.apache.log4j.Logger;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.security.AccessControlException;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import jaxe.JaxeResourceBundle;
import org.w3c.dom.Element;

/**
 * Affiche un dialogue permettant de choisir une image parmis les fichiers du dossier symboles de Jaxe.
 * Dans le cas d'une applet, utilise un fichier liste.txt dans le dossier symboles pour trouver
 * la liste des images (le dossier symboles devant se trouver dans le classpath).
 */
public class DialogueSymbole extends JDialog implements ActionListener {

    /**
     * Logger for this class
     */
    private static final Logger LOG = Logger.getLogger(DialogueSymbole.class);

    Element el;

    JFrame jframe;

    boolean valide = false;

    File[] liste;

    JLabel[] labels;

    int ichoix = -1;

    public DialogueSymbole(final JFrame jframe, final Element el, final String srcAttr) {
        super(jframe, JaxeResourceBundle.getRB().getString("symbole.Insertion"), true);
        this.jframe = jframe;
        this.el = el;
        final String nomf = el.getAttribute(srcAttr);
        boolean applet = false;
        try {
            final File dossierSymboles = new File("symboles");
            if (!dossierSymboles.exists()) {
                JOptionPane.showMessageDialog(jframe, JaxeResourceBundle.getRB().getString("erreur.SymbolesNonTrouve"), JaxeResourceBundle.getRB().getString("erreur.Erreur"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            liste = chercherImages(dossierSymboles);
        } catch (AccessControlException ex) {
            applet = true;
            try {
                final URL urlListe = DialogueSymbole.class.getClassLoader().getResource("symboles/liste.txt");
                BufferedReader in = new BufferedReader(new InputStreamReader(urlListe.openStream()));
                final ArrayList<File> listeImages = new ArrayList<File>();
                String ligne = null;
                while ((ligne = in.readLine()) != null) {
                    if (!"".equals(ligne.trim())) listeImages.add(new File("symboles/" + ligne.trim()));
                }
                liste = listeImages.toArray(new File[listeImages.size()]);
            } catch (IOException ex2) {
                LOG.error(ex2);
            }
        }
        final JPanel cpane = new JPanel(new BorderLayout());
        setContentPane(cpane);
        final GridLayout grille = new GridLayout((int) Math.ceil(liste.length / 13.0), 13, 10, 10);
        final JPanel spane = new JPanel(grille);
        cpane.add(spane, BorderLayout.CENTER);
        ichoix = 0;
        final MyMouseListener ecouteur = new MyMouseListener();
        labels = new JLabel[liste.length];
        for (int i = 0; i < liste.length; i++) {
            if (nomf != null && !"".equals(nomf) && nomf.equals(liste[i].getPath())) ichoix = i;
            URL urlIcone;
            try {
                if (applet) {
                    final URL urlListe = DialogueSymbole.class.getClassLoader().getResource("symboles/liste.txt");
                    final String baseURL = urlListe.toString().substring(0, urlListe.toString().indexOf("symboles/liste.txt"));
                    urlIcone = new URL(baseURL + liste[i].getPath());
                } else urlIcone = liste[i].toURL();
            } catch (MalformedURLException ex) {
                LOG.error(ex);
                break;
            }
            final Icon ic = new ImageIcon(urlIcone);
            final JLabel label = new JLabel(ic);
            label.addMouseListener(ecouteur);
            labels[i] = label;
            spane.add(label);
        }
        final JPanel bpane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        final JButton boutonAnnuler = new JButton(JaxeResourceBundle.getRB().getString("bouton.Annuler"));
        boutonAnnuler.addActionListener(this);
        boutonAnnuler.setActionCommand("Annuler");
        bpane.add(boutonAnnuler);
        final JButton boutonOK = new JButton(JaxeResourceBundle.getRB().getString("bouton.OK"));
        boutonOK.addActionListener(this);
        boutonOK.setActionCommand("OK");
        bpane.add(boutonOK);
        cpane.add(bpane, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(boutonOK);
        choix(ichoix);
        pack();
        if (jframe != null) {
            final Rectangle r = jframe.getBounds();
            setLocation(r.x + r.width / 4, r.y + r.height / 4);
        } else {
            final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation((screen.width - getSize().width) / 3, (screen.height - getSize().height) / 3);
        }
    }

    public static File[] chercherImages(final File dossier) {
        final File[] liste = dossier.listFiles();
        final LinkedHashSet<File> res = new LinkedHashSet<File>();
        for (final File f : liste) if (f.isDirectory()) res.addAll(Arrays.asList(chercherImages(f))); else if (f.isFile()) {
            final String nomf = f.getName();
            final int ip = nomf.lastIndexOf('.');
            if (ip != -1) {
                final String ext = nomf.substring(ip + 1).toLowerCase();
                if ("png".equals(ext)) res.add(f); else if ("gif".equals(ext)) {
                    final String nomfpng = nomf.substring(0, ip) + ".png";
                    boolean trouv = false;
                    for (int j = 0; j < liste.length && !trouv; j++) if (nomfpng.equals(liste[j].getName())) trouv = true;
                    if (!trouv) res.add(f);
                }
            }
        }
        return res.toArray(new File[res.size()]);
    }

    public boolean afficher() {
        if (ichoix == -1) return false;
        setVisible(true);
        return valide;
    }

    public String fichierChoisi() {
        String chemin = liste[ichoix].getPath();
        if (File.separatorChar != '/') chemin = chemin.replace(File.separatorChar, '/');
        return chemin;
    }

    public void actionPerformed(final ActionEvent e) {
        final String cmd = e.getActionCommand();
        if ("OK".equals(cmd)) {
            valide = true;
            setVisible(false);
        } else if ("Annuler".equals(cmd)) {
            valide = false;
            setVisible(false);
        }
    }

    protected void choix(final int ich) {
        if (ichoix != -1) {
            final JLabel label = labels[ichoix];
            label.setBorder(null);
        }
        ichoix = ich;
        final JLabel label = labels[ichoix];
        label.setBorder(BorderFactory.createLineBorder(Color.darkGray));
    }

    class MyMouseListener extends MouseAdapter {

        public MyMouseListener() {
            super();
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            final Component c = e.getComponent();
            for (int i = 0; i < labels.length; i++) if (labels[i] == c) choix(i);
        }
    }
}
