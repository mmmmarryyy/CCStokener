package rene.zirkel;

import java.awt.Label;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import rene.util.xml.XmlReader;
import rene.util.xml.XmlTag;
import rene.util.xml.XmlTagPI;
import rene.util.xml.XmlTagText;
import rene.util.xml.XmlTree;
import rene.zirkel.construction.Construction;
import rene.zirkel.construction.ConstructionException;
import rene.zirkel.objects.ConstructionObject;
import rene.zirkel.objects.PointObject;
import rene.zirkel.objects.PrimitiveCircleObject;
import rene.zirkel.objects.PrimitiveLineObject;
import rene.zirkel.objects.SegmentObject;
import rene.zirkel.tools.AnimatorTool;
import rene.zirkel.tools.ObjectTracker;
import rene.zirkel.tools.Tracker;

public class DemoRunner implements Runnable, MouseListener {

    ZirkelCanvas ZC;

    ZirkelApplet ZA;

    boolean Stopped = false, Continue = false, Hold = false;

    int delay = 10;

    Label L;

    XmlTree Tree;

    public DemoRunner(final ZirkelCanvas zc, final ZirkelApplet za, final String filename, final Label label) {
        ZC = zc;
        ZA = za;
        L = label;
        try {
            URL url;
            if (filename.toUpperCase().startsWith("HTTP")) url = new URL(filename); else url = new URL(ZA.getCodeBase(), filename);
            final InputStream in = url.openStream();
            final XmlReader xml = new XmlReader();
            xml.init(in);
            XmlTree tree = xml.scan();
            Enumeration e = tree.getContent();
            while (e.hasMoreElements()) {
                tree = (XmlTree) e.nextElement();
                if (tree.getTag() instanceof XmlTagPI) continue;
                if (!tree.getTag().name().equals("Demo")) throw new ConstructionException("Demo tag not found"); else {
                    final XmlTag tag = tree.getTag();
                    if (tag.hasParam("delay")) {
                        try {
                            delay = Integer.parseInt(tag.getValue("delay"));
                        } catch (final Exception ex) {
                        }
                    }
                    break;
                }
            }
            Tree = tree;
            e = tree.getContent();
            if (!e.hasMoreElements()) return;
            while (e.hasMoreElements()) {
                tree = (XmlTree) e.nextElement();
                if (!tree.getTag().name().equals("File")) throw new ConstructionException("Illegal tag " + tree.getTag().name());
            }
            in.close();
        } catch (final ConstructionException e) {
            label.setText(e.toString());
        } catch (final Exception e) {
            label.setText("Error loading " + filename);
        }
        zc.addMouseListener(this);
        new Thread(this).start();
    }

    public void run() {
        Enumeration e = Tree.getContent();
        ZC.setFrozen(true);
        while (true) {
            Continue = false;
            final int D = delay;
            if (e.hasMoreElements()) {
                try {
                    XmlTree tree = (XmlTree) e.nextElement();
                    final XmlTag tag = tree.getTag();
                    final String filename = tag.getValue("name");
                    URL url;
                    if (filename.toUpperCase().startsWith("HTTP")) url = new URL(filename); else url = new URL(ZA.getCodeBase(), filename);
                    final InputStream in = url.openStream();
                    ZC.clearMacros();
                    ZC.load(in);
                    in.close();
                    ZC.recompute();
                    if (tag.hasParam("delay")) {
                        try {
                            delay = Integer.parseInt(tag.getValue("delay"));
                        } catch (final Exception ex) {
                        }
                    }
                    final Enumeration en = tree.getContent();
                    while (en.hasMoreElements()) {
                        tree = (XmlTree) en.nextElement();
                        if (tree.getTag() instanceof XmlTagText) {
                            L.setText(((XmlTagText) tree.getTag()).getContent());
                        }
                    }
                    startZC();
                } catch (final Exception ex) {
                    L.setText("Error loading file!");
                }
                try {
                    for (int i = 0; i < delay * 2 || Hold; i++) {
                        Thread.sleep(500);
                        if (i == 0) {
                            ZC.setFrozen(false);
                            ZC.repaint();
                        }
                        if (Stopped) return;
                        if (Continue) {
                            Hold = false;
                            break;
                        }
                    }
                    ZC.setFrozen(true);
                } catch (final Exception ex) {
                }
                delay = D;
            } else {
                e = Tree.getContent();
            }
        }
    }

    AnimatorTool A;

    public void startZC() {
        final Construction C = ZC.getConstruction();
        ZC.setInteractive(false);
        if (C.TrackP != null) {
            try {
                final ConstructionObject P = C.find(C.TrackP);
                if (!((P instanceof PointObject) || (P instanceof PrimitiveLineObject))) throw new ConstructionException("");
                final ConstructionObject po[] = new ConstructionObject[C.TrackPO.size()];
                for (int i = 0; i < po.length; i++) {
                    final ConstructionObject o = C.find((String) C.TrackPO.elementAt(i));
                    if (o == null || !((o instanceof PointObject) || (o instanceof PrimitiveLineObject) || (o instanceof PointObject))) throw new ConstructionException("");
                    po[i] = o;
                }
                final PointObject PM = (PointObject) C.find(C.TrackPM);
                if (C.TrackO != null) {
                    final ConstructionObject O = C.find(C.TrackO);
                    if (P == null || PM == null || O == null) throw new ConstructionException("");
                    final ObjectTracker ot = new ObjectTracker(P, PM, O, ZC, C.Animate, C.Paint, po);
                    if (C.Animate) ot.Interactive = false;
                    ot.setOmit(C.Omit);
                    ZC.setTool(ot);
                    ZC.allowRightMouse(false);
                    ZC.validate();
                    ZC.repaint();
                } else {
                    if (P == null) throw new ConstructionException("");
                    ZC.setTool(new Tracker(P, po));
                    if (PM != null) PM.setSelected(true);
                    ZC.validate();
                    ZC.repaint();
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        } else if (C.AnimateP != null) {
            try {
                final PointObject P = (PointObject) C.find(C.AnimateP);
                if (P == null || !P.moveable()) throw new ConstructionException("");
                final Enumeration e = C.AnimateV.elements();
                while (e.hasMoreElements()) {
                    final String s = (String) e.nextElement();
                    final ConstructionObject o = C.find(s);
                    if (o == null || !(o instanceof SegmentObject || o instanceof PrimitiveCircleObject || o instanceof PointObject)) throw new ConstructionException("");
                }
                ZC.setTool(A = new AnimatorTool(P, C.AnimateV, ZC, C.AnimateNegative, C.AnimateOriginal, C.AnimateDelay));
                ZC.allowRightMouse(false);
                A.setInteractive(false);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        ZC.repaint();
    }

    public void stop() {
        Stopped = true;
    }

    public void mousePressed(final MouseEvent e) {
    }

    public void mouseReleased(final MouseEvent e) {
    }

    public void mouseEntered(final MouseEvent e) {
    }

    public void mouseExited(final MouseEvent e) {
    }

    public void mouseClicked(final MouseEvent e) {
        if (e.isMetaDown()) Hold = true; else Continue = true;
    }
}
