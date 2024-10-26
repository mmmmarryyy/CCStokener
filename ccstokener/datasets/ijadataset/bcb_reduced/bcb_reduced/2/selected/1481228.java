package gcr.mmm.ui;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import javax.swing.JComboBox;

/**
 * Applet for drawing multiple tagged boxes onto an image.
 * 
 * @author Benjamin Hill
 * 
 */
public class ImageAnnotate extends Applet implements MouseMotionListener, MouseListener, ActionListener {

    /**
     * Container for an individual annotation.
     * 
     * @author Benjamin
     * 
     */
    public class Annotation extends Rectangle {

        /**
         * Comment for <code>serialVersionUID</code>
         */
        private static final long serialVersionUID = -7697736472623464096L;

        private String annotation = null;

        /**
         * @return Returns the annotation.
         */
        public String getAnnotation() {
            return this.annotation;
        }

        /**
         * @param anno
         *            The annotation to set.
         */
        public void setAnnotation(String anno) {
            this.annotation = anno;
        }

        /**
         * @return String representation of this markup.
         * @see java.awt.Rectangle#toString()
         */
        public String toString() {
            return this.annotation + "(" + super.toString() + ")";
        }

        /**
         * @return URL parameter version of this annotation
         */
        public String toUrl() {
            return "annotation=" + this.annotation + "&x=" + this.x + "&y=" + this.y + "&width=" + this.width + "&height=" + this.height;
        }
    }

    private static final String ANNO_PART_SPLIT = ",";

    private static final String ANNO_SPLIT = ":";

    private static final int CLEAR_BOX_SIZE = 15;

    private static final String PARAM_ANNO = "annotations";

    private static final String PARAM_EXISTING = "existing";

    private static final String PARAM_IMAGE = "image";

    private static final String PARAM_IMAGEID = "photoID";

    private static final String PARAM_URL = "postURL";

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3690419210531196801L;

    final LinkedList annotations = new LinkedList();

    Image backbuffer, photo;

    Graphics backg;

    final JComboBox combo = new JComboBox();

    boolean isDragging = false;

    int width, height;

    double wscale, hscale;

    /**
     * @param e
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().startsWith("comboBoxChanged")) {
            String name = (String) this.combo.getSelectedItem();
            Annotation anno = (Annotation) this.annotations.getLast();
            anno.setAnnotation(name);
            this.combo.setEnabled(false);
            repaint();
            System.out.println("Set annotation:" + anno.toUrl());
            try {
                submitAnnotations();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else {
            System.out.println("Ignored Event:" + e);
        }
    }

    /**
     * 
     */
    private void downloadImage() {
        MediaTracker mt = new MediaTracker(this);
        this.photo = this.getImage(this.getDocumentBase(), this.getParameter(PARAM_IMAGE));
        mt.addImage(this.photo, 1);
        try {
            mt.waitForAll();
        } catch (InterruptedException e) {
            System.err.println(e);
        }
        System.out.println("Downloaded image:" + this.getParameter(PARAM_IMAGE));
        if (this.photo == null) {
            System.err.println("Unknown problem when downloading image.");
        }
        System.out.println(this.photo.getWidth(this) + "," + this.photo.getHeight(this));
    }

    /**
     * Downloads a CSV list of annotations.
     */
    public void getAnnotationList() {
        String anno = this.getParameter(PARAM_ANNO);
        String annos[] = anno.split(ANNO_SPLIT);
        final TreeSet items = new TreeSet();
        for (int i = 0; i < annos.length; i++) {
            String str = annos[i];
            str = str.trim();
            str = str.replaceAll(" +", " ");
            items.add(str);
        }
        this.combo.addItem("[unknown]");
        Iterator itr = items.iterator();
        while (itr.hasNext()) {
            this.combo.addItem(itr.next());
        }
        System.out.println("Parsed " + annos.length + " annotation options.");
    }

    /**
     * @return applet rules
     * @see java.applet.Applet#getAppletInfo()
     */
    public String getAppletInfo() {
        return "MMM Image Annotation applet.";
    }

    /**
     * Parse format: Benjmain Hill,1,1,20,20:John
     * Smith,40,2,100,25...String,x,y,w,h
     */
    public void getExistingAnnotations() {
        if (this.getParameter(PARAM_EXISTING) != null && this.getParameter(PARAM_EXISTING).length() > 0) {
            String existing[] = this.getParameter(PARAM_EXISTING).split(ANNO_SPLIT);
            int restoredCount = 0;
            for (int i = 0; i < existing.length; i++) {
                System.out.println("Restoring:" + existing[i]);
                String parts[] = existing[i].split(ANNO_PART_SPLIT);
                if (parts.length == 5) {
                    Annotation anno = new Annotation();
                    anno.setAnnotation(parts[0]);
                    anno.x = (int) (Integer.parseInt(parts[1]) * this.wscale);
                    anno.y = (int) (Integer.parseInt(parts[2]) * this.hscale);
                    anno.width = (int) (Integer.parseInt(parts[3]) * this.wscale);
                    anno.height = (int) (Integer.parseInt(parts[4]) * this.hscale);
                    System.out.println(anno.toString());
                    this.annotations.addLast(anno);
                    restoredCount++;
                } else {
                    System.err.println("Unable to restore:" + existing[i]);
                }
            }
            System.out.println("Restored " + restoredCount + " annotation boxes.");
        } else {
            System.out.println("No existing annotations to restore.");
        }
    }

    /**
     * 
     * @see java.applet.Applet#init()
     */
    public void init() {
        this.width = getSize().width;
        this.height = getSize().height;
        downloadImage();
        this.wscale = ((double) this.width / (double) this.photo.getWidth(this));
        this.hscale = ((double) this.height / (double) this.photo.getHeight(this));
        getAnnotationList();
        getExistingAnnotations();
        this.backbuffer = createImage(this.width, this.height);
        this.backg = this.backbuffer.getGraphics();
        this.backg.setFont(new Font("Arial", Font.BOLD, 12));
        this.combo.setEditable(true);
        this.combo.setEnabled(true);
        this.addMouseMotionListener(this);
        this.addMouseListener(this);
        this.combo.addActionListener(this);
        this.add(this.combo);
        this.doLayout();
        this.repaint();
    }

    /**
     * @param arg0
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent arg0) {
    }

    /**
     * @param e
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    public void mouseDragged(MouseEvent e) {
        if (!this.isDragging) {
            if (this.annotations.size() == 0) {
                Annotation anno = new Annotation();
                this.annotations.addLast(anno);
            }
            Annotation anno = (Annotation) this.annotations.getLast();
            if (anno.getAnnotation() != null) {
                anno = new Annotation();
                this.annotations.addLast(anno);
                System.out.println("Started new annotation.");
            }
            anno.x = e.getX();
            anno.y = e.getY();
            this.isDragging = true;
        } else {
            Annotation anno = (Annotation) this.annotations.getLast();
            anno.width = e.getX() - anno.x;
            anno.height = e.getY() - anno.y;
        }
        repaint();
        e.consume();
    }

    /**
     * @param arg0
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent arg0) {
    }

    /**
     * @param arg0
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent arg0) {
    }

    /**
     * @param e
     * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
     */
    public void mouseMoved(MouseEvent e) {
        this.isDragging = false;
        this.combo.setEnabled(true);
    }

    /**
     * @param e
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e) {
        LinkedList annoCopy = new LinkedList();
        annoCopy.addAll(this.annotations);
        Iterator itr = annoCopy.iterator();
        while (itr.hasNext()) {
            Annotation anno = (Annotation) itr.next();
            if (anno.getAnnotation() != null) {
                if (e.getX() < anno.x + CLEAR_BOX_SIZE && e.getX() > anno.x && e.getY() < anno.y + CLEAR_BOX_SIZE && e.getY() > anno.y) {
                    this.annotations.remove(anno);
                    System.out.println("Annotation removed.");
                }
            }
        }
        this.repaint();
    }

    /**
     * @param arg0
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent arg0) {
    }

    /**
     * @param g
     * @see java.awt.Container#paint(java.awt.Graphics)
     */
    public void paint(Graphics g) {
        update(g);
    }

    private void submitAnnotations() throws IOException {
        final URL url = new URL(this.getDocumentBase(), this.getParameter(PARAM_URL));
        System.out.println(url);
        System.out.flush();
        StringBuffer outBuff = new StringBuffer();
        outBuff.append(PARAM_IMAGEID).append("=").append(this.getParameter(PARAM_IMAGEID)).append("&");
        Iterator annos = this.annotations.iterator();
        while (annos.hasNext()) {
            Annotation anno = (Annotation) annos.next();
            outBuff.append("annotation=");
            outBuff.append(anno.getAnnotation() + ANNO_PART_SPLIT);
            outBuff.append((int) (anno.x / this.wscale) + ANNO_PART_SPLIT);
            outBuff.append((int) (anno.y / this.hscale) + ANNO_PART_SPLIT);
            outBuff.append((int) (anno.width / this.wscale) + ANNO_PART_SPLIT);
            outBuff.append((int) (anno.height / this.hscale));
            outBuff.append("&");
        }
        System.out.println("Posting to " + url.toString());
        System.out.println(outBuff.toString());
        final HttpURLConnection huc = (HttpURLConnection) url.openConnection();
        huc.setRequestMethod("POST");
        huc.setDoOutput(true);
        huc.setDoInput(true);
        huc.setUseCaches(false);
        Writer out = new BufferedWriter(new OutputStreamWriter(huc.getOutputStream()));
        out.write(outBuff.toString());
        out.flush();
        out.close();
        BufferedReader inbuf = new BufferedReader(new InputStreamReader(huc.getInputStream()));
        String inputLine;
        String sContent = "";
        while ((inputLine = inbuf.readLine()) != null) {
            if (inputLine.trim().length() > 0) sContent += inputLine + "\n";
        }
        inbuf.close();
        System.out.println("Returned:" + sContent);
    }

    /**
     * @param g
     * @see java.awt.Container#update(java.awt.Graphics)
     */
    public void update(Graphics g) {
        this.backg.setPaintMode();
        this.backg.setColor(Color.white);
        this.backg.fillRect(0, 0, this.width, this.height);
        this.backg.drawImage(this.photo, 0, 0, this.width, this.height, 0, 0, this.photo.getWidth(this), this.photo.getHeight(this), Color.white, this);
        this.backg.setXORMode(Color.white);
        Iterator annos = this.annotations.iterator();
        while (annos.hasNext()) {
            Annotation anno = (Annotation) annos.next();
            this.backg.setColor(Color.black);
            this.backg.drawRect(anno.x, anno.y, anno.width, anno.height);
            this.backg.drawRect(anno.x + 1, anno.y + 1, anno.width - 2, anno.height - 2);
            if (anno.getAnnotation() != null) {
                this.backg.setColor(Color.red);
                this.backg.fillRect(anno.x, anno.y, CLEAR_BOX_SIZE, CLEAR_BOX_SIZE);
                this.backg.drawString(anno.getAnnotation(), anno.x, anno.y + anno.height + 10);
            }
        }
        g.drawImage(this.backbuffer, 0, 0, this);
    }
}
