package com.jdiv;

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.Timer;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import com.jdiv.extensions.JFlag;
import com.jdiv.extensions.JFnt;
import com.jdiv.extensions.JFontImage;
import com.jdiv.extensions.JFpg;
import com.jdiv.extensions.JFpgImage;
import com.jdiv.input.JMouse;
import com.jdiv.input.JWrite;
import com.jdiv.util.JNumber;
import com.jdiv.util.MaskColorImage;
import com.jdiv.util.composite.BlendComposite;
import com.jdiv.util.composite.GraphicsUtil;

/**
 * @author  Joyal
 */
public class JCore extends Canvas implements JConst, KeyListener {

    private static final long serialVersionUID = -2283943149256912923L;

    /**
	 * @uml.property  name="process"
	 */
    private ArrayList<JProcess> process = new ArrayList<JProcess>();

    /**
	 * @uml.property  name="fpgs"
	 */
    private ArrayList<JFpg> fpgs = new ArrayList<JFpg>();

    /**
	 * @uml.property  name="images"
	 */
    private ArrayList<JFpgImage> images = new ArrayList<JFpgImage>();

    /**
	 * @uml.property  name="fnts"
	 */
    private ArrayList<JFnt> fnts = new ArrayList<JFnt>();

    /**
	 * @uml.property  name="writes"
	 */
    private ArrayList<JWrite> writes = new ArrayList<JWrite>();

    /**
	 * @uml.property  name="regions"
	 */
    private ArrayList<JRegion> regions = new ArrayList<JRegion>();

    /**
	 * @uml.property  name="keyStatus"
	 */
    private boolean keyStatus[] = new boolean[522];

    private BufferStrategy bStrategy;

    private String title;

    private BufferedImage background = null;

    /**
	 * @uml.property  name="window"
	 * @uml.associationEnd  
	 */
    public JWindow window;

    private int windowWidth;

    private int windowHeight;

    private Image backbuffer;

    private BufferedImage screenImg;

    private BufferedImage clearImg;

    private BufferedImage blendImg;

    /**
	 * @uml.property  name="jUpdate"
	 * @uml.associationEnd  
	 */
    private JUpdate jUpdate;

    /**
	 * @uml.property  name="jLoop"
	 * @uml.associationEnd  
	 */
    private JLoop jLoop;

    private Timer drawTimer;

    /**
	 * @uml.property  name="scroll"
	 * @uml.associationEnd  multiplicity="(0 -1)"
	 */
    public JScroll scroll[] = new JScroll[10];

    private int mFpsCount = 0;

    private int mFps = 0;

    private boolean mIsShowFps = true;

    private long mInitFps = -1;

    public long elapsedTime = 0;

    private int scaleMode = 0;

    int init = 0;

    private int bX = 0;

    private int bY = 0;

    /**
	 * @uml.property  name="mouse"
	 * @uml.associationEnd  
	 */
    public JMouse mouse = new JMouse();

    public JCore() {
    }

    public void appletInit(int resolution) {
        setResolution(resolution);
        coreInit();
    }

    public void appletInit(int width, int height) {
        setResolution(width, height);
        coreInit();
    }

    private BufferedImage canvasToImage() {
        int w = getWidth();
        int h = getHeight();
        int type = BufferedImage.TYPE_INT_RGB;
        BufferedImage image = new BufferedImage(w, h, type);
        Image img = this.createImage(windowWidth, windowHeight);
        JFrame frame = new JFrame("Canvas");
        frame.setSize(w, h);
        frame.getContentPane().add(new JLabel(new ImageIcon(img)));
        frame.setVisible(true);
        return image;
    }

    private void cleanRegion() {
        for (int i = 0; i < regions.size(); i++) {
            Graphics gRegion = regions.get(i).getImgRegion().getGraphics();
            gRegion.setColor(Color.black);
            gRegion.clearRect(0, 0, getWidth(), getHeight());
        }
    }

    public void coreInit() {
        System.setProperty("sun.java2d.translaccel", "true");
        System.setProperty("sun.java2d.ddforcevram", "true");
        regions.add(new JRegion(0, 0, 0, getCanvasWidth(), getCanvasHeight()));
        setIgnoreRepaint(true);
        addKeyListener(this);
        setMouseListener(mouse);
        setMouseMotionListener(mouse);
        createBufferStrategy(2);
        bStrategy = getBufferStrategy();
        clearImg = new BufferedImage(getWidth() + 30, getHeight() + 30, BufferedImage.TYPE_INT_RGB);
        clearImg.createGraphics();
        Graphics g = clearImg.getGraphics();
        g.setColor(Color.black);
        g.fillRect(0, 0, getWidth(), getHeight());
        for (int i = 0; i < scroll.length; i++) scroll[i] = new JScroll();
        jUpdate = new JUpdate();
        jLoop = new JLoop();
        jUpdate.start();
        jLoop.start();
    }

    public void define_region(int id, int x0, int y0, int x1, int y1) {
    }

    public void delay(int speed) {
        try {
            Thread.sleep(speed);
        } catch (InterruptedException e) {
        }
    }

    public void delay(long speed) {
        try {
            Thread.sleep(speed);
        } catch (InterruptedException e) {
        }
    }

    public void delete_text(int text) {
    }

    private void fps(Graphics2D g) {
        if (mIsShowFps == true) {
            if (mInitFps == -1) mInitFps = System.currentTimeMillis();
            g.setColor(Color.white);
            g.drawString("Fps: " + mFps, 3, 12);
            long mTimeFps = System.currentTimeMillis() - mInitFps;
            if (mTimeFps >= 1000) {
                mInitFps = System.currentTimeMillis();
                mFps = mFpsCount;
                mFpsCount = 0;
            }
        }
    }

    private void processCount(Graphics2D g) {
        g.setColor(Color.white);
        g.drawString("Process: " + process.size(), 3, 30);
    }

    public int get_pixel(int x, int y) {
        return canvasToImage().getRGB(x, y);
    }

    public void screenShot() {
        BufferedImage bufImage = new BufferedImage(windowWidth, windowHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bufImage.createGraphics();
        updatePaint(g);
        g.dispose();
        try {
            ImageIO.write(bufImage, "png", new File("D:/prueba.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BufferStrategy getBStrategy() {
        return bStrategy;
    }

    public Canvas getCanvas() {
        return this;
    }

    /**
 * @return
 * @uml.property  name="fnts"
 */
    public ArrayList<JFnt> getFnts() {
        return fnts;
    }

    /**
 * @return
 * @uml.property  name="fpgs"
 */
    public ArrayList<JFpg> getFpgs() {
        return fpgs;
    }

    public int getCanvasHeight() {
        return windowHeight;
    }

    /**
 * @return
 * @uml.property  name="images"
 */
    public ArrayList<JFpgImage> getImages() {
        return images;
    }

    public Graphics2D getJDivGraphics() {
        return (Graphics2D) bStrategy.getDrawGraphics();
    }

    /**
 * @return
 * @uml.property  name="keyStatus"
 */
    public boolean[] getKeyStatus() {
        return keyStatus;
    }

    /**
 * @return
 * @uml.property  name="process"
 */
    public ArrayList<JProcess> getProcess() {
        return process;
    }

    private JRegion getRegion(int id) {
        for (int i = 0; i < regions.size(); i++) {
            if (regions.get(i).getId() == id) return regions.get(i);
        }
        return null;
    }

    /**
 * @return
 * @uml.property  name="regions"
 */
    public ArrayList<JRegion> getRegions() {
        return regions;
    }

    public int getCanvasWidth() {
        return windowWidth;
    }

    /**
 * @return
 * @uml.property  name="writes"
 */
    public ArrayList<JWrite> getWrites() {
        return writes;
    }

    public boolean key(int KeyCode) {
        return keyStatus[KeyCode];
    }

    public void keyPressed(KeyEvent e) {
        keyStatus[e.getKeyCode()] = true;
    }

    public void keyReleased(KeyEvent e) {
        keyStatus[e.getKeyCode()] = false;
    }

    public void keyTyped(KeyEvent e) {
    }

    public int load_fnt(String archivo) {
        JFnt fnt = new JFnt(archivo);
        fnts.add(fnt);
        return fnts.size() - 1;
    }

    public int load_fnt(URL archivo) {
        JFnt fnt = new JFnt(archivo);
        fnts.add(fnt);
        return fnts.size() - 1;
    }

    public int load_fpg(String archivo) {
        JFpg fpg = new JFpg(archivo);
        fpgs.add(fpg);
        return fpgs.size() - 1;
    }

    public int load_fpg(URL url) {
        JFpg fpg = new JFpg(url);
        fpgs.add(fpg);
        return fpgs.size() - 1;
    }

    public int map_get_pixel(int fpg, int graph, int x, int y) {
        BufferedImage img = fpgs.get(fpg).getImage(graph);
        return img.getRGB(x, y);
    }

    public int load_image(String nombre) {
        String ext = nombre.substring(nombre.length() - 3, nombre.length());
        BufferedImage image = null;
        try {
            FileInputStream file = new FileInputStream(nombre);
            if (ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("gif")) {
                image = ImageIO.read(file);
            } else {
                image = ImageIO.read(file);
                image = GraphicsUtil.toCompatibleImage(MaskColorImage.maskImage(image));
            }
            JFpgImage jfpgi = new JFpgImage(image, 100 + images.size(), image.getHeight() * image.getWidth(), "Imagen " + images.size(), nombre, image.getWidth(), image.getHeight(), 0, new ArrayList<JFlag>());
            images.add(jfpgi);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return MAX_FPG_IMAGE + (images.size() - 1);
    }

    public int load_image(URL url) {
        String nombre = url.getFile();
        String ext = nombre.substring(nombre.length() - 3, nombre.length());
        BufferedImage image = null;
        try {
            InputStream fin = url.openStream();
            DataInputStream file = new DataInputStream(new BufferedInputStream(fin));
            if (ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("gif")) {
                image = ImageIO.read(file);
            } else {
                image = ImageIO.read(file);
                image = GraphicsUtil.toCompatibleImage(MaskColorImage.maskImage(image));
            }
            JFpgImage jfpgi = new JFpgImage(image, 100 + images.size(), image.getHeight() * image.getWidth(), "Imagen " + images.size(), nombre, image.getWidth(), image.getHeight(), 0, new ArrayList<JFlag>());
            images.add(jfpgi);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return MAX_FPG_IMAGE + (images.size() - 1);
    }

    public boolean out_region(JProcess pro, int idRegion) {
        if (pro != null && pro.image != null) {
            if (pro.x > getWidth() + pro.image.getWidth() || pro.x < -pro.image.getWidth()) return true;
            if (pro.y > getHeight() + pro.image.getHeight() || pro.y < -pro.image.getHeight()) return true;
        }
        return false;
    }

    public void paint(Graphics g) {
        updatePaint((Graphics2D) g);
    }

    public void paintBackground(Graphics2D g) {
        if (background != null) {
            g.drawImage(background, 0, 0, this);
        }
    }

    public void addScroll(int id, int fpg, int graph, int background, int region, int flags) {
        scroll[id] = new JScroll(id, fpg, graph, background, region, flags);
    }

    private void paintScroll(Graphics2D g) {
        for (int i = 0; i < 10; i++) {
            if (scroll[i] != null && scroll[i].run) {
                if (!scroll[i].init) {
                    if (scroll[i].graph < MAX_FPG_IMAGE && scroll[i].graph > 0) scroll[i].setImage(fpgs.get(scroll[i].file).getImage(scroll[i].graph)); else if (scroll[i].graph >= MAX_FPG_IMAGE && scroll[i].graph > 0) scroll[i].setImage(images.get((scroll[i].graph - MAX_FPG_IMAGE)).getImagen());
                    if (scroll[i].graph < MAX_FPG_IMAGE && scroll[i].background > 0) scroll[i].setBackgroundImage(fpgs.get(scroll[i].file).getImage(scroll[i].background)); else if (scroll[i].graph >= MAX_FPG_IMAGE && scroll[i].background > 0) scroll[i].setBackgroundImage(images.get((scroll[i].background - MAX_FPG_IMAGE)).getImagen());
                    scroll[i].init = true;
                }
                if (scroll[i].getBackgroundImage() != null) g.drawImage(scroll[i].getBackgroundImage(), scroll[i].x1, scroll[i].y1, null);
                if (scroll[i].getImage() != null) g.drawImage(scroll[i].getImage(), scroll[i].x0, scroll[i].y0, null);
            }
        }
    }

    public void moveScroll(int cnumber, int vel, int direction) {
        int i = cnumber;
        int endX = windowWidth - scroll[i].getImage().getWidth();
        switch(scroll[i].flags) {
            case 0:
                JProcess camara = null;
                if (process.size() > 0) camara = process.get(scroll[i].camera);
                if (scroll[i].camera > -1 && camara != null && camara.getImage() != null && camara.ctype == C_SCROLL) {
                    if (scroll[i].getImage() != null) {
                        switch(direction) {
                            case 0:
                                scroll[i].x0 += vel;
                                break;
                            case 1:
                                scroll[i].x0 -= vel;
                                break;
                        }
                        if (scroll[i].x0 >= 0 && direction == 0) {
                            scroll[i].x0 = 0;
                        } else if (scroll[i].x0 <= endX && direction == 1) {
                            scroll[i].x0 = endX;
                        }
                        if (camara.sY < windowHeight / 2) {
                        } else if (camara.sY > windowHeight / 2) {
                            switch(direction) {
                                case 2:
                                    scroll[i].y0 -= vel;
                                    break;
                                case 3:
                                    scroll[i].y0 += vel;
                                    break;
                            }
                        }
                    }
                }
                break;
            case 1:
                if (scroll[i].getImage() != null) {
                    int sX = -(scroll[i].x0);
                    if (Math.abs(sX) > scroll[i].getImage().getWidth()) scroll[i].x0 = 0;
                }
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            case 5:
                break;
            case 6:
                break;
            case 7:
                break;
            case 8:
                break;
            default:
                break;
        }
    }

    private void paintProcess(Graphics2D g) {
        for (int i = 0; i < process.size(); i++) {
            if (i <= process.size() && process.get(i) != null) {
                JProcess proces = process.get(i);
                if (proces.status == S_WAKEUP && proces.graph < MAX_FPG_IMAGE && proces.graph > 0 && proces.graph != proces.old_graph) {
                    proces.setImage(fpgs.get(proces.file).getImage(proces.graph));
                    proces.setBackupImage(fpgs.get(proces.file).getImage(proces.graph));
                } else if (proces.status == S_WAKEUP && proces.graph >= MAX_FPG_IMAGE && proces.graph > 0 && proces.graph != proces.old_graph) {
                    proces.setImage(images.get((proces.graph - MAX_FPG_IMAGE)).getImagen());
                    proces.setBackupImage(images.get((proces.graph - MAX_FPG_IMAGE)).getImagen());
                }
                if (proces.isPrimitive()) paintPrimitives(g, proces);
                updateProcess(proces);
                switch(proces.ctype) {
                    case C_SCREEN:
                        if (proces.getImage() != null) {
                            proces.width = proces.getImage().getWidth();
                            proces.height = proces.getImage().getHeight();
                            g.drawImage(proces.getImage(), proces.x - (proces.width / 2), proces.y - (proces.height / 2), null);
                            if (proces.showBounds) {
                                g.setColor(Color.RED);
                                g.drawRect(proces.x - (proces.width / 2), proces.y - (proces.height / 2), proces.width, proces.height);
                            }
                        }
                        break;
                    case C_BLEND:
                        paintBlend(g, proces);
                        break;
                    case C_SCROLL:
                        if (proces.getImage() != null) paintProcessScroll(g, proces);
                        break;
                }
                if (mouse.graph > 0 && mouse.image != null) g.drawImage(mouse.image, mouse.x - (mouse.image.getWidth() / 2), mouse.y - (mouse.image.getHeight() / 2), null); else if (mouse.graph > 0 && mouse.image == null) {
                    if (mouse.graph >= MAX_FPG_IMAGE) mouse.image = images.get((mouse.graph - MAX_FPG_IMAGE)).getImagen(); else mouse.image = fpgs.get(0).getImage(mouse.graph);
                }
                proces.old_graph = proces.graph;
            }
        }
    }

    private void paintPrimitives(Graphics2D g, JProcess proces) {
        if (!proces.rectangles.isEmpty()) {
            for (int i = 0; i < proces.rectangles.size(); i++) {
                g.drawRect(proces.rectangles.get(i).x, proces.rectangles.get(i).y, proces.rectangles.get(i).width, proces.rectangles.get(i).height);
            }
        }
        if (!proces.boxes.isEmpty()) {
            g.setColor(proces.color);
            for (int i = 0; i < proces.boxes.size(); i++) {
                g.fillRect(proces.boxes.get(i).x, proces.boxes.get(i).y, proces.boxes.get(i).width, proces.boxes.get(i).height);
            }
        }
    }

    private void oldPaint() {
        JProcess proces = null;
        Graphics2D g = null;
        if (proces.getImage() != null && proces.blend == 0) {
            g.drawImage(proces.getImage(), proces.x - (proces.getImage().getWidth() / 2), proces.y - (proces.getImage().getHeight() / 2), null);
        }
        if (proces.getImage() != null && blendImg != null && proces.blend > 0) {
            BufferedImage image0 = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image0.createGraphics();
            g2.drawImage(blendImg, bX, bY, null);
            g2.setComposite(BlendComposite.Add);
            g2.drawImage(GraphicsUtil.toCompatibleImage(proces.getImage()), proces.x - (proces.getImage().getWidth() / 2), proces.y - (proces.getImage().getHeight() / 2), null);
            g2.dispose();
            g.drawImage(image0, 0, 0, null);
            blendImg = image0;
            bX = 0;
            bY = 0;
        } else if (proces.getImage() != null && proces.blend > 0) {
            blendImg = new BufferedImage(proces.getImage().getWidth(), proces.getImage().getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g4 = blendImg.createGraphics();
            g4.drawImage(proces.getImage(), 0, 0, null);
            bX = proces.x - (proces.getImage().getWidth() / 2);
            bY = proces.y - (proces.getImage().getHeight() / 2);
        }
    }

    private void paintBlend(Graphics2D g, JProcess proces) {
        for (int i = 0; i < process.size(); i++) {
            if (i <= process.size() && process.get(i) != null) proces = process.get(i);
            if (proces.getImage() != null && blendImg != null && proces.blend > 0) {
                BufferedImage image0 = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = image0.createGraphics();
                g2.drawImage(GraphicsUtil.toCompatibleImage(blendImg), bX, bY, null);
                g2.setComposite(BlendComposite.Add);
                g2.drawImage(GraphicsUtil.toCompatibleImage(proces.getImage()), proces.x - (proces.getImage().getWidth() / 2), proces.y - (proces.getImage().getHeight() / 2), null);
                g2.dispose();
                g.drawImage(image0, 0, 0, null);
                blendImg = image0;
                bX = 0;
                bY = 0;
            } else if (proces.getImage() != null && proces.blend > 0) {
                blendImg = new BufferedImage(proces.getImage().getWidth(), proces.getImage().getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g4 = blendImg.createGraphics();
                g4.drawImage(proces.getImage(), 0, 0, null);
                bX = proces.x - (proces.getImage().getWidth() / 2);
                bY = proces.y - (proces.getImage().getHeight() / 2);
            }
        }
        blendImg = null;
    }

    private void paintProcessScroll(Graphics2D g, JProcess proces) {
        if (scroll[proces.cnumber].camera == proces.id) {
            int limitX = (scroll[proces.cnumber].getImage().getWidth() - windowWidth) + windowWidth / 2;
            int limitY = (scroll[proces.cnumber].getImage().getHeight() - windowHeight) + windowHeight / 2;
            int scrollWidth = scroll[proces.cnumber].getImage().getWidth();
            int vel = Math.abs(proces.old_x - proces.x);
            if (proces.x > proces.old_x && proces.x > windowWidth / 2) {
                moveScroll(proces.cnumber, vel, 1);
            } else if (proces.x < proces.old_x && proces.x < limitX) {
                moveScroll(proces.cnumber, vel, 0);
            }
            if ((scroll[proces.cnumber].x0 == 0 && proces.x <= windowWidth / 2) || (proces.x >= limitX)) {
                if (proces.x > proces.old_x) proces.sX += vel; else if (proces.x < proces.old_x) proces.sX -= vel;
            } else {
                proces.sX = windowWidth / 2;
            }
            g.drawImage(proces.getImage(), proces.sX - (proces.getImage().getWidth() / 2), proces.y - (proces.getImage().getHeight() / 2), null);
            proces.old_x = proces.x;
            proces.old_sY = proces.sY;
        } else {
            JProcess cam = null;
            for (int i = 0; i < process.size(); i++) {
                if (scroll[proces.cnumber].camera == process.get(i).id) cam = process.get(i);
            }
            if (cam != null) {
                if (cam.sX >= windowHeight / 2 && scroll[proces.cnumber].x0 < scroll[proces.cnumber].old_x0) {
                    proces.x -= Math.abs(scroll[proces.cnumber].x0 - scroll[proces.cnumber].old_x0);
                } else if (cam.sX >= windowHeight / 2 && scroll[proces.cnumber].x0 > scroll[proces.cnumber].old_x0) {
                    proces.x += Math.abs(scroll[proces.cnumber].x0 - scroll[proces.cnumber].old_x0);
                }
                g.drawImage(proces.getImage(), proces.x - (proces.getImage().getWidth() / 2), proces.y - (proces.getImage().getHeight() / 2), null);
                proces.old_x = proces.x;
                scroll[proces.cnumber].old_x0 = scroll[proces.cnumber].x0;
            }
        }
    }

    private void paintWrites(Graphics2D g) {
        BufferedImage imgFont = null;
        for (int i = 0; i < writes.size(); i++) {
            String texto = writes.get(i).getText();
            if (writes.get(i).getFile() != 0) {
                int anchoTemp = 0;
                BufferedImage imgTmp = new BufferedImage(writes.get(i).lenght(), writes.get(i).getHeight() + writes.get(i).maxVOffset(), BufferedImage.TYPE_INT_ARGB);
                for (int j = 0; j < texto.length(); j++) {
                    JFontImage font = fnts.get(writes.get(i).getFile() - 1).getFont(texto.charAt(j));
                    imgFont = font.getImgFont();
                    Graphics2D g2 = imgTmp.createGraphics();
                    g2.drawImage(imgFont, null, anchoTemp, font.getVoffset());
                    anchoTemp += font.getAncho() + 2;
                }
                imgFont = imgTmp;
            }
            int x = 0, y = 0;
            switch(writes.get(i).getAlign()) {
                case 0:
                    x = writes.get(i).getX();
                    y = writes.get(i).getY();
                    break;
                case 1:
                    x = writes.get(i).getX() - (writes.get(i).lenght() / 2);
                    y = writes.get(i).getY();
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    break;
                case 6:
                    break;
                case 7:
                    break;
                case 8:
                    break;
            }
            if (writes.get(i).getFile() != 0) {
                g.drawImage(imgFont, x, y, this);
            } else {
                g.setColor(Color.WHITE);
                g.drawString(texto, x, y + 10);
            }
        }
    }

    public void put_screen(int file, int graph) {
        if (graph >= MAX_FPG_IMAGE) background = images.get((graph - MAX_FPG_IMAGE)).getImagen(); else background = fpgs.get(file - 1).getImage(graph);
    }

    public void setMouseListener(MouseListener mouse) {
        addMouseListener(mouse);
    }

    public int rand(int n1, int n2) {
        int menos, result;
        menos = Math.abs(n1) - Math.abs(n2);
        result = (int) (Math.random() * menos) + Math.abs(n1);
        if ((n1 < 0 && n2 < 0) || (n1 < 0 && n2 > 0)) result = -1 * result;
        return result;
    }

    private BufferedImage rotateImage(BufferedImage img, int angle) {
        if (img == null) return null;
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.rotate(Math.toRadians(angle), w / 2, h / 2);
        g2.drawImage(img, null, 0, 0);
        return image;
    }

    private BufferedImage alphaImage(BufferedImage img, int alpha) {
        if (img == null) return null;
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        float a = (float) alpha / 100;
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a);
        g2.setComposite(ac);
        g2.drawImage(img, null, 0, 0);
        return image;
    }

    private BufferedImage sizeImage(BufferedImage img, int scale) {
        if (img == null) return null;
        int w = scale * img.getWidth() / 100;
        int h = scale * img.getHeight() / 100;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double scalex = (double) image.getWidth() / img.getWidth();
        double scaley = (double) image.getHeight() / img.getHeight();
        AffineTransform xform = AffineTransform.getScaleInstance(scalex, scaley);
        g2.drawRenderedImage(img, xform);
        g2.dispose();
        return image;
    }

    private BufferedImage flipImage(BufferedImage img, int flag) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        switch(flag) {
            case 0:
                tx.translate(0, 0);
                break;
            case 1:
                tx.translate(-w, 0);
                break;
            default:
                break;
        }
        g2.drawImage(img, tx, null);
        g2.dispose();
        return image;
    }

    public void set_mode(int resolution) {
        setResolution(resolution);
        window = new JWindow(title, this, windowWidth, windowHeight);
        window.addKeyListener(this);
        window.setVisible(true);
        coreInit();
    }

    public void set_mode(int w, int h) {
        this.windowWidth = w;
        this.windowHeight = h;
        window = new JWindow(title, this, w, h);
        window.addKeyListener(this);
        window.setVisible(true);
        coreInit();
    }

    public void set_title(String title) {
        this.title = title;
        if (window != null) window.setTitle(title);
    }

    /**
 * @param images
 * @uml.property  name="images"
 */
    public void setImages(ArrayList<JFpgImage> images) {
        this.images = images;
    }

    /**
 * @param regions
 * @uml.property  name="regions"
 */
    public void setRegions(ArrayList<JRegion> regions) {
        this.regions = regions;
    }

    private void setResolution(int resolution) {
        int width = 0, height = 0;
        switch(resolution) {
            case 0:
                width = 320;
                height = 200;
                break;
            case 1:
                width = 320;
                height = 240;
                break;
            case 2:
                width = 320;
                height = 400;
                break;
            case 3:
                width = 360;
                height = 240;
                break;
            case 4:
                width = 360;
                height = 260;
                break;
            case 5:
                width = 376;
                height = 282;
                break;
            case 6:
                width = 400;
                height = 300;
                break;
            case 7:
                width = 640;
                height = 400;
                break;
            case 8:
                width = 640;
                height = 480;
                break;
            case 9:
                width = 800;
                height = 600;
                break;
            case 10:
                width = 1024;
                height = 768;
                break;
            case 11:
                width = 1280;
                height = 720;
                break;
        }
        this.windowWidth = width;
        this.windowHeight = height;
    }

    private void setResolution(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
    }

    public void setMouseMotionListener(MouseMotionListener mouse) {
        addMouseMotionListener(mouse);
    }

    public void scale_mode(int scale) {
        int border = 5;
        this.scaleMode = scale;
        setSize(new Dimension((this.windowWidth * scale) - border, (this.windowHeight * scale) - border));
    }

    private void scaleCanvas(Graphics2D g, int scale) {
        AffineTransform transformer = new AffineTransform();
        transformer.scale(scale, scale);
        g.setTransform(transformer);
    }

    public void fullScreen(boolean fullscreen) {
        window.setFullScreen(fullscreen);
    }

    public void signal(JProcess pro, int signal) {
        pro.status = signal;
    }

    public void start_scroll(int id, int fpg, int graph, int background, int region, int flags) {
    }

    public synchronized void update() {
        long initTime = System.currentTimeMillis();
        if (bStrategy != null) {
            Graphics2D g = (Graphics2D) bStrategy.getDrawGraphics();
            if (scaleMode > 0) scaleCanvas(g, scaleMode);
            updatePaint(g);
            if (bStrategy != null) bStrategy.show();
            mFpsCount++;
        }
        elapsedTime = initTime - System.currentTimeMillis();
    }

    public void close() {
        bStrategy = null;
        jUpdate = null;
        jLoop = null;
    }

    public void update(Graphics2D graphics) {
        updatePaint(graphics);
        fps(graphics);
        processCount(graphics);
        graphics.dispose();
    }

    public void update(Graphics g) {
        if (bStrategy != null) bStrategy.show();
    }

    public void updatePaint(Graphics2D g) {
        g.setColor(Color.black);
        g.fillRect(0, 0, getWidth(), getHeight());
        paintBackground(g);
        paintScroll(g);
        paintProcess(g);
        paintWrites(g);
    }

    private void updateProcess(JProcess process) {
        if (process != null && process.getImage() != null && process.angle != process.old_angle) {
            int ang = -process.angle / ANGLE_OFFSET;
            ang = (ang < 0) ? (ang % 360) + 360 : ang % 360;
            process.setImage(rotateImage(process.getBackupImage(), ang));
            process.old_angle = process.angle;
        }
        if (process != null && process.getImage() != null && process.flags > 0 && process.graph != process.old_graph) {
            process.setImage(flipImage(process.getImage(), process.flags));
            process.old_flags = process.flags;
        }
        if (process != null && process.getImage() != null && process.size != process.old_size) {
            process.setImage(sizeImage(process.getImage(), process.size));
            process.old_size = process.size;
        }
        if (process != null && process.getImage() != null && process.graph != process.old_graph && process.alpha < 100) {
            process.setImage(alphaImage(process.getImage(), process.alpha));
            process.old_alpha = process.alpha;
        }
        if (process != null && process.getImage() != null && process.alpha != process.old_alpha) {
            process.setImage(alphaImage(process.getImage(), process.alpha));
            process.old_alpha = process.alpha;
        }
        if (process != null && process.getImage() != null && process.spin != process.old_spin) {
            int ang = -process.spin / ANGLE_OFFSET;
            ang = (ang < 0) ? (ang % 360) + 360 : ang % 360;
            process.setImage(rotateImage(process.getImage(), ang));
            process.old_spin = process.spin;
        }
    }

    public void write(int fnt, int x, int y, int align, JNumber num) {
        boolean existe = false;
        for (int i = 0; i < writes.size(); i++) {
            JWrite t = writes.get(i);
            if (fnt == t.getFile() && x == t.getX() && y == t.getY()) {
                t.setNum(num);
                existe = true;
                break;
            }
        }
        if (!existe) writes.add(new JWrite(fnt, x, y, align, num));
    }

    public void write(int fnt, int x, int y, int align, String text) {
        boolean existe = false;
        for (int i = 0; i < writes.size(); i++) {
            JWrite t = writes.get(i);
            if (fnt == t.getFile() && x == t.getX() && y == t.getY()) {
                t.setText(text);
                existe = true;
                break;
            }
        }
        if (!existe) writes.add(new JWrite(fnt, x, y, align, text));
    }

    public int fget_angle(int x1, int y1, int x2, int y2) {
        return x1 * x2 + y1 * y2;
    }

    public Panel getPanel() {
        return window.getPanel();
    }
}
