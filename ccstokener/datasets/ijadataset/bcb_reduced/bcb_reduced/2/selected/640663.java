package jaxe.elements;

import org.apache.log4j.Logger;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.text.Position;
import jaxe.JaxeDocument;
import jaxe.JaxeElement;
import jaxe.JaxeResourceBundle;
import jaxe.JaxeUndoableEdit;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Fichier d'image. L'image est affichée dans le texte si elle est trouvée, sinon un message d'erreur
 * est affiché dans le texte à la place de l'image.
 * Type d'élément Jaxe: 'fichier'
 * paramètre: srcAtt: le nom de l'attribut donnant le nom du fichier
 */
public class JEFichier extends JaxeElement {

    /**
     * Logger for this class
     */
    private static final Logger LOG = Logger.getLogger(JEFichier.class);

    static String newline = "\n";

    public static final String defaultSrcAttr = "nom";

    public String srcAttr = defaultSrcAttr;

    JLabel label = null;

    public float alignementY = 1;

    public static int taillemax = 300;

    public static boolean reduction = true;

    private JEFichierMouseListener listener;

    public JEFichier(final JaxeDocument doc) {
        this.doc = doc;
    }

    @Override
    public void init(final Position pos, final Node noeud) {
        final Element el = (Element) noeud;
        srcAttr = doc.cfg.valeurParametreElement(refElement, "srcAtt", defaultSrcAttr);
        affichageLabel();
        label.setAlignmentY(alignementY);
        listener = new JEFichierMouseListener(this, doc.jframe);
        label.addMouseListener(listener);
        final Position newpos = insertComponent(pos, label);
        creerEnfants(newpos);
    }

    /**
     * Création du label ou mise à jour de son affichage
     */
    protected void affichageLabel() {
        URL urlimg = null;
        File fimg = null;
        final Element el = (Element) noeud;
        final String nomf = el.getAttribute(srcAttr);
        if (doc.fsave == null) {
            try {
                if (doc.furl != null) {
                    urlimg = new URL(doc.furl, nomf);
                    if (nomf.startsWith("symboles")) {
                        try {
                            InputStream stream = urlimg.openStream();
                            stream.close();
                        } catch (IOException ex) {
                            urlimg = JEFichier.class.getClassLoader().getResource(nomf);
                        }
                    }
                } else {
                    fimg = new File(nomf);
                    urlimg = fimg.toURI().toURL();
                }
            } catch (final MalformedURLException ex) {
                LOG.error("affichageLabel()", ex);
            }
        } else {
            fimg = new File(nomf);
            if (!fimg.isAbsolute()) fimg = new File(doc.fsave.getParent() + File.separatorChar + nomf);
            if (!fimg.exists() && nomf.startsWith("symboles")) fimg = new File(nomf);
            try {
                urlimg = fimg.toURI().toURL();
            } catch (final MalformedURLException ex) {
                LOG.error("affichageLabel()", ex);
            }
        }
        if (label == null) label = new JLabel();
        if (urlimg == null || (fimg != null && (!fimg.exists() || !fimg.isFile()))) {
            label.setText(getString("erreur.FichierNonTrouve") + ": " + nomf);
            label.setIcon(null);
            label.setBorder(BorderFactory.createLineBorder(Color.darkGray));
        } else {
            Image img = Toolkit.getDefaultToolkit().createImage(urlimg);
            boolean erreur = false;
            if (img == null || !chargerImage(img)) erreur = true;
            if (!erreur && reduction) img = reduireImage(img);
            if (!erreur) {
                final ImageIcon icon = new ImageIcon(img);
                if (icon == null || icon.getImageLoadStatus() == MediaTracker.ABORTED || icon.getImageLoadStatus() == MediaTracker.ERRORED) erreur = true; else {
                    label.setText(null);
                    label.setIcon(icon);
                    label.setBorder(null);
                }
            }
            if (erreur) {
                label.setText(getString("erreur.AffichageImage") + ": " + nomf);
                label.setIcon(null);
                label.setBorder(BorderFactory.createLineBorder(Color.darkGray));
            }
        }
    }

    protected boolean chargerImage(final Image img) {
        if (img == null) return false;
        final MediaTracker tracker = new MediaTracker(doc.jframe);
        tracker.addImage(img, 0);
        try {
            tracker.waitForAll();
        } catch (final InterruptedException e) {
            return false;
        }
        return !tracker.isErrorAny();
    }

    protected static Image reduireImage(final Image img) {
        if (img == null) return null;
        int width = img.getWidth(null);
        int height = img.getHeight(null);
        if (width == -1 || height == -1) {
            return null;
        } else if (width > taillemax || height > taillemax) {
            if (width > height) {
                final double scale = taillemax * 1.0 / width;
                width = taillemax;
                height = (int) (height * scale);
            } else {
                final double scale = taillemax * 1.0 / height;
                height = taillemax;
                width = (int) (width * scale);
            }
            final Image img2 = img.getScaledInstance(width, height, Image.SCALE_FAST);
            img.flush();
            return img2;
        }
        return img;
    }

    @Override
    public Node nouvelElement(final Element refElement) {
        final String nombalise = doc.cfg.nomElement(refElement);
        final Element newel = nouvelElementDOM(doc, refElement);
        if (newel == null) return null;
        final String srcAttr = doc.cfg.valeurParametreElement(refElement, "srcAtt", defaultSrcAttr);
        final DialogueFichier dlg = new DialogueFichier(doc.jframe, doc, JaxeResourceBundle.getRB().getString("zone.NouvelleBalise") + " " + nombalise, refElement, newel, srcAttr);
        if (!dlg.afficher()) return null;
        try {
            dlg.enregistrerReponses();
        } catch (final Exception ex) {
            LOG.error("nouvelElement(Element)", ex);
            return null;
        }
        if (doc.fsave != null) {
            final File f = new File(doc.fsave.getParent() + File.separatorChar + newel.getAttribute(srcAttr));
            if (!f.exists()) {
                JOptionPane.showMessageDialog(doc.jframe, JaxeResourceBundle.getRB().getString("erreur.FichierNonTrouve"), JaxeResourceBundle.getRB().getString("zone.NouvelleBalise") + " " + nombalise, JOptionPane.ERROR_MESSAGE);
            }
        }
        return newel;
    }

    @Override
    public void afficherDialogue(final JFrame jframe) {
        final Element el = (Element) noeud;
        final ArrayList<Element> latt = doc.cfg.listeAttributs(refElement);
        if (latt != null && latt.size() > 0) {
            final DialogueFichier dlg = new DialogueFichier(doc.jframe, doc, "Fichier", refElement, el, srcAttr);
            if (!dlg.afficher()) return;
            dlg.enregistrerReponses();
            majAffichage();
        }
    }

    @Override
    public void majAffichage() {
        affichageLabel();
        doc.imageChanged(label);
    }

    public static void collerImage(final Image img, final JaxeDocument doc, final Position pos, final Element refElement) {
        if (doc.fsave == null) return;
        File imgFile = null;
        final String baseNom = "coller";
        String nouveauNom = null;
        final File dossier = new File(doc.fsave.getParent() + File.separator + "images-collees");
        if (!dossier.exists()) if (!dossier.mkdir()) {
            LOG.error("collerImage(Image, JaxeDocument, Position, Element) -" + " Erreur à la création du dossier des images collées");
            return;
        }
        int i = 1;
        while (imgFile == null || imgFile.exists()) {
            nouveauNom = baseNom + i + ".png";
            imgFile = new File(dossier.getPath() + File.separator + nouveauNom);
            i++;
        }
        final BufferedImage buffimg;
        if (img instanceof BufferedImage) buffimg = (BufferedImage) img; else {
            buffimg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            final Graphics g = buffimg.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
        }
        try {
            ImageIO.write(buffimg, "PNG", imgFile);
        } catch (final IOException ex) {
            LOG.error("collerImage(Image, JaxeDocument, Position, Element) - IOException", ex);
            JOptionPane.showMessageDialog(doc.jframe, JaxeResourceBundle.getRB().getString("erreur.Enregistrement") + ": " + ex.getMessage(), JaxeResourceBundle.getRB().getString("erreur.Erreur"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        final JEFichier newje = new JEFichier(doc);
        final Element newel = JaxeElement.nouvelElementDOM(doc, refElement);
        if (newel == null) return;
        final String srcAttr = doc.cfg.valeurParametreElement(refElement, "srcAtt", defaultSrcAttr);
        final ArrayList<Element> attributs = doc.cfg.listeAttributs(refElement);
        for (final Element attdef : attributs) {
            if (srcAttr.equals(doc.cfg.nomAttribut(attdef))) newel.setAttributeNS(doc.cfg.espaceAttribut(attdef), srcAttr, dossier.getName() + File.separator + nouveauNom);
        }
        newje.inserer(pos, newel);
        doc.textPane.addEdit(new JaxeUndoableEdit(JaxeUndoableEdit.AJOUTER, newje));
        doc.setModif(true);
    }

    @Override
    public void selection(final boolean select) {
        super.selection(select);
        label.setEnabled(!select);
    }

    @Override
    public void effacer() {
        super.effacer();
        if (listener != null) {
            label.removeMouseListener(listener);
            listener = null;
        }
    }

    class JEFichierMouseListener extends MouseAdapter {

        JEFichier jei;

        JFrame jframe;

        public JEFichierMouseListener(final JEFichier obj, final JFrame jframe) {
            super();
            jei = obj;
            this.jframe = jframe;
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            jei.afficherDialogue(jframe);
        }
    }
}
