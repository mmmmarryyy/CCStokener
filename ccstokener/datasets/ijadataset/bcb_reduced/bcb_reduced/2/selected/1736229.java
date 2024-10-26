package jf.exam.paint;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Locale;
import java.util.Vector;
import jf.exam.paint.graphics.obj.CircleRect;
import jf.exam.paint.graphics.obj.Clear;
import jf.exam.paint.graphics.obj.CurveLine;
import jf.exam.paint.graphics.obj.DragArea;
import jf.exam.paint.graphics.obj.HText;
import jf.exam.paint.graphics.obj.IGraphicsObject;
import jf.exam.paint.graphics.obj.Line;
import jf.exam.paint.graphics.obj.ManualLine;
import jf.exam.paint.graphics.obj.NetFill;
import jf.exam.paint.graphics.obj.Oval;
import jf.exam.paint.graphics.obj.Point;
import jf.exam.paint.graphics.obj.Rectangle;
import jf.exam.paint.graphics.obj.Sharp;
import jf.exam.paint.graphics.obj.SignLeft;
import jf.exam.paint.graphics.obj.SignRight;
import jf.exam.paint.graphics.obj.Smooth;
import jf.exam.paint.graphics.obj.Sprayer;
import jf.exam.paint.graphics.obj.SolidFill;
import jf.exam.paint.graphics.obj.TemplImg;
import jf.exam.paint.graphics.obj.VText;
import jf.exam.paint.graphics.tools.MessageBox;
import jf.exam.paint.graphics.tools.MessageEvent;
import jf.exam.paint.graphics.tools.MessageListener;
import jf.exam.paint.panel.ListPanel;
import jf.exam.paint.panel.ListPanelFactory;
import jf.exam.paint.panel.ListSaveAreaPanel;
import jf.exam.paint.util.Config;
import jf.exam.paint.util.GifEncoder;
import jf.exam.paint.util.Language;
import jf.exam.paint.util.SJpegEncoder;
import com.edgenius.registry.IObservable;
import com.edgenius.registry.IRegistrable;
import com.edgenius.registry.IRegistryNode;
import com.edgenius.registry.RootRegistryNode;

/**
 * This class fulfill all funciton in Canvas of PaintApplet
 * Creation date: (2001-11-12 9:39:03)
 * @author: Steve Ni
 */
public class PaintCanvas extends java.awt.Canvas implements IRegistrable, IObservable {

    private IRegistryNode registerNode;

    private int drawLineStatus = Config.EndDraw;

    private IGraphicsObject gObj = null;

    private Image oldImage = null;

    private Graphics oldG = null;

    private Image currentImage = null;

    private Graphics currentG = null;

    private boolean noActionFlag = true;

    private ListSaveAreaPanel listSaveAreap = null;

    private boolean existFunc = false;

    private boolean isCurrImgDirty = false;

    public int lastPenType = 0;

    private int buttLabelIndex = 1;

    private int messBoxW = 200;

    private int messBoxH = 120;

    private int buttW = 50;

    private int buttH = 30;

    private int saveStatus = 0;

    private URL url = null;

    private byte[] arrByte = new byte[1];

    private StringBuffer saveTypeFlag;

    private MessageBox msgBox;

    private Vector mouseListeners = new Vector();

    private Vector mouseMoveListeners = new Vector();

    private GObjTrace gObjContainer = new GObjTrace();

    CanvasEventHandler caEventHandler = new CanvasEventHandler();

    MessBoxEventHandler mbEventHandler = new MessBoxEventHandler();

    DragAreaEventHandler daEventHandler = new DragAreaEventHandler();

    class GObjTrace extends Vector {

        private IGraphicsObject[] arrGObj;

        private Image[] arrImg;

        private int unPoint = 0;

        private int foot = 0;

        private int head = foot;

        private int cursor = foot;

        private int PAGE = 10;

        private int MAX_UNDO = 50;

        private int MAX_DIMENSION = MAX_UNDO + PAGE;

        public void push(IGraphicsObject gObj, Image img) {
            head = cursor;
            if (head == foot + MAX_UNDO) foot++;
            cursor = ++head;
            unPoint++;
            if (unPoint == MAX_DIMENSION) unPoint = 0;
            if ((unPoint % PAGE) == 0) getArrImg()[unPoint / PAGE].getGraphics().drawImage(img, 0, 0, PaintCanvas.this); else getArrGObj()[unPoint] = gObj;
        }

        public void undo(Image currentImg) {
            if (unPoint == -1 && getArrGObj()[MAX_DIMENSION - 1] == null) {
                unPoint = 0;
                return;
            }
            if (cursor == foot) return;
            cursor--;
            unPoint--;
            if (unPoint == -1) unPoint = MAX_DIMENSION - 1;
            currentImg.getGraphics().drawImage(getArrImg()[unPoint / PAGE], 0, 0, PaintCanvas.this);
            for (int i = unPoint / PAGE * PAGE + 1; i <= unPoint; i++) {
                getArrGObj()[i].draw(currentImg.getGraphics());
            }
            return;
        }

        public void redo(Image currentImg) {
            if (cursor == head) return;
            cursor++;
            unPoint++;
            if (unPoint == MAX_DIMENSION) unPoint = 0;
            currentImg.getGraphics().drawImage(getArrImg()[unPoint / PAGE], 0, 0, PaintCanvas.this);
            for (int i = unPoint / PAGE * PAGE + 1; i <= unPoint; i++) {
                getArrGObj()[i].draw(currentImg.getGraphics());
            }
            return;
        }

        private IGraphicsObject[] getArrGObj() {
            if (arrGObj == null) return arrGObj = new IGraphicsObject[MAX_DIMENSION]; else return arrGObj;
        }

        private Image[] getArrImg() {
            if (arrImg == null) {
                arrImg = new Image[MAX_DIMENSION / PAGE];
                for (int i = 0; i < MAX_DIMENSION / PAGE; i++) arrImg[i] = PaintCanvas.this.createImage(Config.CanvasW, Config.CanvasH);
            }
            return arrImg;
        }
    }

    class CanvasEventHandler implements java.awt.event.MouseListener, java.awt.event.MouseMotionListener {

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent e) {
            if (getRootNode().getEvent().isLock()) return;
            if (getRootNode().getResource().getPenType() != Config.PenNone) {
                constructGobj(e, Config.Drawing);
                repaint();
            }
            if (gObj != null && gObj instanceof TemplImg && !((TemplImg) gObj).isFixed()) {
                ((TemplImg) gObj).handleEvent(Config.TemplMouseDragged, e);
                repaint();
            }
        }

        public void mouseEntered(MouseEvent e) {
            if (getRootNode().getEvent().isLock()) return;
            if (getRootNode().getResource().getPenType() != Config.PenNone) setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        }

        public void mouseExited(MouseEvent e) {
            if (getRootNode().getEvent().isLock()) return;
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }

        public void mouseMoved(MouseEvent e) {
            if (getRootNode().getEvent().isLock()) return;
            if (gObj != null && gObj instanceof TemplImg && !((TemplImg) gObj).isFixed()) {
                ((TemplImg) gObj).handleEvent(Config.TemplMouseMoved, e);
                repaint();
                return;
            }
        }

        public void mousePressed(MouseEvent e) {
            if (getRootNode().getEvent().isLock()) return;
            if (getRootNode().getResource().getPenType() != Config.PenNone) {
                if (getRootNode().getResource().getPenType() == Config.PenTemplImg) {
                    completeDraw();
                    if (gObj != null && gObj instanceof TemplImg) ((TemplImg) gObj).fixTempl();
                }
                constructGobj(e, Config.StartDraw);
            }
            if (gObj != null && gObj instanceof TemplImg && !((TemplImg) gObj).isFixed()) ((TemplImg) gObj).handleEvent(Config.TemplMousePressed, e);
        }

        public void mouseReleased(MouseEvent e) {
            if (getRootNode().getEvent().isLock()) return;
            if (getRootNode().getResource().getPenType() != Config.PenNone) {
                getRootNode().getEvent().setDirty();
                constructGobj(e, Config.EndDraw);
            }
            if (gObj != null && gObj instanceof TemplImg && !((TemplImg) gObj).isFixed()) ((TemplImg) gObj).handleEvent(Config.TemplMouseReleased, e);
            repaint();
        }
    }

    class MessBoxEventHandler implements MessageListener {

        public void buttonChosen(MessageEvent e) {
            MessageBox mb = (MessageBox) e.getSource();
            switch(mb.getID()) {
                case Config.PSaveBegin:
                    getMsgBox().dispose();
                    getRootNode().getEvent().setLock(Config.LOCK_None);
                    if (e.getIndex() == 1) if (saveImage(1)) {
                        getMsgBox().display(Config.PSaveSuccess, Language.getString("SAVE_SUCCESS"), Language.getString("YES"), MessageBox.CENTER);
                        getRootNode().getEvent().setLock(Config.LOCK_MessageBox);
                    } else {
                        getMsgBox().display(Config.PSaveFail, Language.getString("FAIL_TRY_AGAIN"), Language.getString("YES_CANCEL"), MessageBox.CENTER);
                        getRootNode().getEvent().setLock(Config.LOCK_MessageBox);
                    }
                    if (e.getIndex() == 2) restoreScreen();
                    break;
                case Config.PSaveFail:
                    getMsgBox().dispose();
                    getRootNode().getEvent().setLock(Config.LOCK_None);
                    if (e.getIndex() == 1) if (saveImage(2)) {
                        getMsgBox().display(Config.PSaveSuccess, Language.getString("SAVE_SUCCESS"), Language.getString("YES"), MessageBox.CENTER);
                        getRootNode().getEvent().setLock(Config.LOCK_MessageBox);
                    } else {
                        getMsgBox().display(Config.PSaveFailAgain, Language.getString("FAIL_NEXT"), Language.getString("YES"), MessageBox.CENTER);
                        getRootNode().getEvent().setLock(Config.LOCK_MessageBox);
                        if (getRootNode().getResource().getPenType() != Config.PenNone) PaintCanvas.this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
                    }
                    if (e.getIndex() == 2) restoreScreen();
                    break;
                case Config.PSaveFailAgain:
                    getMsgBox().dispose();
                    getRootNode().getEvent().setLock(Config.LOCK_None);
                    restoreScreen();
                    gotoURL();
                    break;
                case Config.PSaveSuccess:
                    getMsgBox().dispose();
                    getRootNode().getEvent().setLock(Config.LOCK_None);
                    restoreScreen();
                    gotoURL();
                    break;
                default:
                    break;
            }
        }
    }

    ;

    class DragAreaEventHandler implements java.awt.event.MouseListener, java.awt.event.MouseMotionListener {

        public void mouseClicked(java.awt.event.MouseEvent e) {
        }

        ;

        public void mouseEntered(java.awt.event.MouseEvent e) {
        }

        ;

        public void mouseExited(java.awt.event.MouseEvent e) {
        }

        ;

        public synchronized void mousePressed(java.awt.event.MouseEvent e) {
            if (getRootNode().getEvent().processEvent() && getRootNode().getEvent().getLock() == Config.LOCK_MessageBox) return;
            if (isCurrImgDirty) {
                currentG.drawImage(oldImage, 0, 0, PaintCanvas.this);
            }
            constructGobj(e, Config.StartDraw);
            isCurrImgDirty = true;
        }

        ;

        public synchronized void mouseReleased(java.awt.event.MouseEvent e) {
            constructGobj(e, Config.EndDraw);
            repaint();
        }

        ;

        public void mouseMoved(java.awt.event.MouseEvent e) {
        }

        ;

        public synchronized void mouseDragged(java.awt.event.MouseEvent e) {
            constructGobj(e, Config.Drawing);
            repaint();
        }

        ;
    }

    ;

    /**
	 * Test1Canvas constructor comment.
	 */
    public PaintCanvas() {
        super();
        init();
    }

    /**
	 * Test1Canvas constructor comment.
	 * @param config java.awt.GraphicsConfiguration
	 */
    public PaintCanvas(java.awt.GraphicsConfiguration config) {
        super(config);
        init();
    }

    /**
	 * This method for these graphics object that own over 2 draw number and
	 * status is not "keep" ( now only Curve Line is )
	 * When this graphics object doesn't complete, but other event occurs this
	 * time, such as Function from Pen is changed to Text, then this method will
	 * invoke for complete current graphis object drawing.
	 * Creation date: (2001-12-12 10:27:53)
	 */
    public void completeDraw() {
        if (drawLineStatus == Config.EndDraw) return;
        drawLineStatus = Config.EndDraw;
        gObj.setNumOfDraw(gObj.getMaxNumOfDraw());
        gObjContainer.push(gObj, getCanvasImage());
        oldG.drawImage(currentImage, 0, 0, this);
    }

    /**
	 * There are three type flag:
	 * startDraw , Drawing and EndDraw
	 * this method set value of GraphicsObject depend on flag.
	 * Creation date: (2001-11-12 11:07:52)
	 * @return boolean
	 * @param flag int
	 */
    private boolean constructGobj(MouseEvent e, int flag) {
        if (drawLineStatus == Config.EndDraw && flag != Config.StartDraw) {
            noActionFlag = true;
            return false;
        }
        noActionFlag = false;
        if (flag == Config.StartDraw) {
            produceGobj();
            if (gObj.getNumOfDraw() == 0) drawLineStatus = flag; else drawLineStatus = Config.Drawing;
            gObj.setNumOfDraw(gObj.getNumOfDraw() + 1);
            gObj.setStartP(new Point(e.getX(), e.getY()));
        }
        if (flag == Config.Drawing) {
            drawLineStatus = flag;
            gObj.setMiddleP(new Point(e.getX(), e.getY()));
        }
        if (flag == Config.EndDraw) {
            if (gObj.getNumOfDraw() == gObj.getMaxNumOfDraw()) drawLineStatus = flag; else drawLineStatus = Config.Drawing;
            gObj.setEndP(new Point(e.getX(), e.getY()));
        }
        return true;
    }

    /**
	* return current Image
	* Creation date: (2001-12-5 17:06:08)
	* @return java.awt.Image
	*/
    public Image getCanvasImage() {
        return currentImage;
    }

    /**
	 * return old Image
	 * Creation date: (2001-12-5 17:06:08)
	 * @return java.awt.Image
	 */
    public Image getOldImage() {
        return oldImage;
    }

    public GObjTrace getGObjContainer() {
        return this.gObjContainer;
    }

    /**
	 * Initializes the applet.
	 */
    public void init() {
        this.addMouseListener(caEventHandler);
        this.addMouseMotionListener(caEventHandler);
    }

    /**
	 * This method will complete any draw function in Canvas
	 * Creation date: (2001-11-12 9:40:00)
	 * @param g java.awt.Graphics
	 */
    public void paint(Graphics g) {
        if (existFunc) {
            int eventType = getRootNode().getEvent().getEventType();
            existFunc = false;
            if (eventType == Config.EventCtlPanelUnDo) {
                gObjContainer.undo(currentImage);
                oldG.drawImage(currentImage, 0, 0, this);
            }
            if (eventType == Config.EventCtlPanelReDo) {
                gObjContainer.redo(currentImage);
                oldG.drawImage(currentImage, 0, 0, this);
            }
            if (eventType == Config.EventCtlPanelClear) {
                Clear clearScr = new Clear();
                getRootNode().register((IRegistrable) clearScr);
                clearScr.draw(g);
                clearScr.draw(currentG);
                gObjContainer.push(clearScr, currentImage);
                clearScr.draw(oldG);
            }
        }
        if (noActionFlag) {
            g.drawImage(currentImage, 0, 0, this);
            return;
        }
        if (oldImage != null) currentG.drawImage(oldImage, 0, 0, this);
        if (gObj != null) {
            gObj.draw(currentG);
            if (drawLineStatus == Config.EndDraw) {
                gObjContainer.push(gObj, getCanvasImage());
                gObj.draw(oldG);
            }
        }
        g.drawImage(currentImage, 0, 0, this);
        noActionFlag = true;
    }

    /**
	* Creation date: (2001-12-13 9:38:39)
	*/
    public void processSave() {
        if (getRootNode().getEvent().getEventType() == Config.EventSaveFile || getRootNode().getEvent().getEventType() == Config.EventSaveTemplate) {
            getMsgBox().display(Config.PSaveBegin, Language.getString("SAVE_IMAGE"), Language.getString("YES_CANCEL"), MessageBox.CENTER);
            getRootNode().getEvent().setLock(Config.LOCK_MessageBox);
        }
        if (getRootNode().getEvent().getEventType() == Config.EventSaveAreaFile || getRootNode().getEvent().getEventType() == Config.EventSaveAreaTemplate) {
            ((ListPanel) getRootNode().lookup(Config.REG_ListPanel)).hidePanel();
            ListPanelFactory.factory(Config.EventCtlPanelArea).display(this.getParent());
            lastPenType = getRootNode().getResource().getPenType();
            getRootNode().getResource().setPenType(Config.PenDragArea);
            getRootNode().getEvent().saveX1 = 0;
            getRootNode().getEvent().saveY1 = 0;
            getRootNode().getEvent().saveX2 = Config.CanvasW;
            getRootNode().getEvent().saveY2 = Config.CanvasH;
            this.removeMouseListener(caEventHandler);
            this.removeMouseMotionListener(caEventHandler);
            this.addMouseListener(daEventHandler);
            this.addMouseMotionListener(daEventHandler);
            getRootNode().getEvent().setLock(Config.LOCK_AreaSaveButton);
        }
        if (getRootNode().getEvent().getEventType() == Config.EventSaveAreaYes) {
            if (saveImage(1)) {
                getMsgBox().display(Config.PSaveSuccess, Language.getString("SAVE_SUCCESS"), Language.getString("YES"), MessageBox.CENTER);
                getRootNode().getEvent().setLock(Config.LOCK_MessageBox);
            } else {
                getMsgBox().display(Config.PSaveFail, Language.getString("FAIL_TRY_AGAIN"), Language.getString("YES_CANCEL"), MessageBox.CENTER);
                getRootNode().getEvent().setLock(Config.LOCK_MessageBox);
            }
        }
        if (getRootNode().getEvent().getEventType() == Config.EventSaveAreaCancel) {
            ((ListPanel) getRootNode().lookup(Config.REG_ListPanel)).hidePanel();
            ListPanelFactory.factory(Config.EventCtlPanelSave).display(this.getParent());
            this.removeMouseListener(daEventHandler);
            this.removeMouseMotionListener(daEventHandler);
            this.addMouseListener(caEventHandler);
            this.addMouseMotionListener(caEventHandler);
            getRootNode().getResource().setPenType(lastPenType);
            restoreScreen();
            getRootNode().getEvent().setLock(Config.LOCK_None);
        }
    }

    /**
	 * This is factory of all GraphicsObj
	 * ,it will instantiate GraphicsObj depend on PenType 
	 * Creation date: (2001-11-14 11:35:35)
	 */
    private void produceGobj() {
        switch(getRootNode().getResource().getPenType()) {
            case Config.PenStraightLine:
                gObj = new Line();
                break;
            case Config.PenCurveLine:
                if (gObj != null && gObj.getNumOfDraw() < gObj.getMaxNumOfDraw()) return;
                gObj = new CurveLine();
                break;
            case Config.PenManualLine:
                gObj = new ManualLine();
                break;
            case Config.PenRectangle:
                gObj = new Rectangle();
                break;
            case Config.PenCircleRectangle:
                gObj = new CircleRect();
                break;
            case Config.PenCircle:
                gObj = new Oval();
                break;
            case Config.PenHorizonText:
                gObj = new HText();
                break;
            case Config.PenVerticalText:
                gObj = new VText();
                break;
            case Config.PenFillSolid:
                gObj = new SolidFill();
                break;
            case Config.PenFillNet:
                gObj = new NetFill();
                break;
            case Config.PenDragArea:
                gObj = new DragArea();
                break;
            case Config.PenTemplImg:
                gObj = new TemplImg();
                break;
            case Config.PenSharp:
                gObj = new Sharp();
                break;
            case Config.PenSmooth:
                gObj = new Smooth();
                break;
            case Config.PenSignLeft:
                gObj = new SignLeft();
                break;
            case Config.PenSignRight:
                gObj = new SignRight();
                break;
            case Config.PenSprayer:
                gObj = new Sprayer();
                break;
            default:
                break;
        }
        if (gObj != null && gObj.getMaxNumOfDraw() == gObj.getNumOfDraw()) gObj.setNumOfDraw(0);
        getRootNode().register((IRegistrable) gObj);
    }

    private void restoreScreen() {
        if (isCurrImgDirty) {
            isCurrImgDirty = false;
            currentG.drawImage(oldImage, 0, 0, this);
            repaint();
        }
    }

    /**
	* All save funciton will call this method to execute save.
	* This method will generate a flag, named saveTypeFlag, which inlcude three char:
	*		first char is "a" or "w" : area save or whole screen save
	*		second char is "g" or "j": gif type or jpeg type
	*		third char is "f" or "t" " file save or template save
	* Creation date: (2001-12-11 14:19:17)
	* 
	*/
    private synchronized boolean saveImage(int saveTime) {
        if (saveTime == 1) {
            saveTypeFlag = new StringBuffer();
            url = null;
            try {
                url = new URL(((PaintApplet) this.getParent()).getCodeBase(), "servlet/jf.exam.paint.servlet.MainServlet");
            } catch (java.net.MalformedURLException e) {
            }
            Image saveImg = getCanvasImage();
            if (getRootNode().getEvent().getEventType() == Config.EventSaveAreaFile || getRootNode().getEvent().getEventType() == Config.EventSaveAreaTemplate) {
                saveImg = this.createImage(Config.CanvasW, Config.CanvasH);
                Graphics saveG = saveImg.getGraphics();
                saveG.drawImage(getCanvasImage(), 0, 0, this);
                if (getRootNode().getEvent().saveX1 < 0) getRootNode().getEvent().saveX1 = 0;
                if (getRootNode().getEvent().saveX1 > Config.CanvasW) getRootNode().getEvent().saveX1 = Config.CanvasW;
                if (getRootNode().getEvent().saveY1 < 0) getRootNode().getEvent().saveY1 = 0;
                if (getRootNode().getEvent().saveY1 > Config.CanvasH) getRootNode().getEvent().saveY1 = Config.CanvasH;
                if (getRootNode().getEvent().saveX2 < 0) getRootNode().getEvent().saveX2 = 0;
                if (getRootNode().getEvent().saveX2 > Config.CanvasW) getRootNode().getEvent().saveX2 = Config.CanvasW;
                if (getRootNode().getEvent().saveY2 < 0) getRootNode().getEvent().saveY2 = 0;
                if (getRootNode().getEvent().saveY2 > Config.CanvasH) getRootNode().getEvent().saveY2 = Config.CanvasH;
                int offdisX, offdisY;
                if (getRootNode().getEvent().saveX1 < getRootNode().getEvent().saveX2) offdisX = getRootNode().getEvent().saveX1; else offdisX = getRootNode().getEvent().saveX2;
                if (getRootNode().getEvent().saveY1 < getRootNode().getEvent().saveY2) offdisY = getRootNode().getEvent().saveY1; else offdisY = getRootNode().getEvent().saveY2;
                saveG.copyArea(offdisX, offdisY, Math.abs(getRootNode().getEvent().saveX1 - getRootNode().getEvent().saveX2), Math.abs(getRootNode().getEvent().saveY1 - getRootNode().getEvent().saveY2), -offdisX, -offdisY);
                Image tempImg = this.createImage(Math.abs(getRootNode().getEvent().saveX1 - getRootNode().getEvent().saveX2), Math.abs(getRootNode().getEvent().saveY1 - getRootNode().getEvent().saveY2));
                Graphics tempG = tempImg.getGraphics();
                tempG.drawImage(saveImg, 0, 0, this);
                saveImg = tempImg;
                saveTypeFlag.append("a");
                saveG.dispose();
            } else {
                saveImg = getCanvasImage();
                saveTypeFlag.append("w");
            }
            ByteArrayOutputStream byteArrOS = null;
            char imageType = 'g';
            try {
                byteArrOS = new ByteArrayOutputStream();
                GifEncoder genc = new GifEncoder(saveImg, byteArrOS);
                genc.encode();
                byteArrOS.flush();
                arrByte = byteArrOS.toByteArray();
                if (Config.DEBUG) System.out.println("before save arrbyte length : " + arrByte.length);
            } catch (java.io.IOException e) {
                if (e.getMessage().equals("too many colors for a GIF")) {
                    try {
                        imageType = 'j';
                        byteArrOS = new ByteArrayOutputStream();
                        SJpegEncoder sjpeg = new SJpegEncoder();
                        sjpeg.encode(byteArrOS, saveImg);
                        byteArrOS.flush();
                        arrByte = byteArrOS.toByteArray();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else e.printStackTrace();
            } finally {
                try {
                    if (byteArrOS != null) byteArrOS.close();
                } catch (IOException exp) {
                    exp.printStackTrace();
                }
            }
            saveTypeFlag.append(imageType);
            if (getRootNode().getEvent().getEventType() == Config.EventSaveAreaFile || getRootNode().getEvent().getEventType() == Config.EventSaveFile) saveTypeFlag.append("f"); else saveTypeFlag.append("t");
            if (!saveS(url, saveTypeFlag.toString(), arrByte)) {
                return false;
            }
        } else {
            if (!saveU(url, saveTypeFlag.toString(), arrByte)) return false;
        }
        return true;
    }

    /**
	 * Save Image as Socket method
	 * Creation date: (2001-12-7 13:40:35)
	 * @param param java.net.URL
	 */
    private synchronized boolean saveS(URL url, String typeFlag, byte[] arrByte) {
        PaintApplet parent = (PaintApplet) this.getParent();
        Socket socket = null;
        boolean flag = true;
        StringBuffer stringBuff = new StringBuffer();
        BufferedReader bufferedreader = null;
        BufferedOutputStream bufferedoutputstream = null;
        char flagChar = '0';
        if (Config.DEBUG) System.out.println("enter SaveS: ");
        try {
            stringBuff.append("POST " + url.getFile() + " HTTP/1.0\r\nAccept-Language: ");
            stringBuff.append(Locale.getDefault().getLanguage() + "\r\n");
            stringBuff.append("Content-type");
            stringBuff.append(": ");
            stringBuff.append("application/octet-stream");
            stringBuff.append("\r\nReferer: ");
            stringBuff.append(parent.getDocumentBase().toExternalForm());
            stringBuff.append("\r\nUser-Agent: PaintApplet/1.x (" + System.getProperty("os.name") + ';' + System.getProperty("os.version") + ")\r\nHost: ");
            stringBuff.append(url.getHost());
            stringBuff.append("\r\nConnection: close\r\nContent-Length: ");
            stringBuff.append(arrByte.length);
            if (Config.DEBUG) System.out.println("arrByte length (Image size): " + arrByte.length);
            stringBuff.append("\r\n\r\n");
            stringBuff.append(typeFlag);
            int currPort = url.getPort();
            socket = new Socket(url.getHost(), currPort <= 0 ? 80 : currPort);
            bufferedoutputstream = new BufferedOutputStream(socket.getOutputStream());
            bufferedoutputstream.write(stringBuff.toString().getBytes("UTF8"));
            bufferedoutputstream.flush();
            stringBuff = null;
            int transLen = 0;
            int fileLen = arrByte.length;
            for (int startPos = 0; startPos < fileLen; ) {
                transLen = Math.min(fileLen - startPos, 5000);
                bufferedoutputstream.write(arrByte, startPos, transLen);
                bufferedoutputstream.flush();
                startPos += transLen;
            }
            if (Config.DEBUG) System.out.println("Applet output file successfully! ");
            stringBuff = new StringBuffer();
            if (Config.DEBUG) System.out.println("Applet socket getInputStream " + socket.getInputStream());
            bufferedreader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            if (Config.DEBUG) System.out.println("Applet socket getInputStream successfully");
            String serReturnMess = bufferedreader.readLine();
            if (Config.DEBUG) System.out.println("Applet check status : " + serReturnMess);
            flagChar = '2';
            if (serReturnMess != null) {
                stringBuff.append(serReturnMess);
                serReturnMess = serReturnMess.substring(serReturnMess.indexOf(32)).trim() + '2';
                flagChar = serReturnMess.charAt(0);
            }
            while ((serReturnMess = bufferedreader.readLine()) != null) {
                if (serReturnMess.length() <= 0) break;
            }
        } catch (Throwable exp) {
            exp.printStackTrace();
            return false;
        } finally {
            try {
                if (bufferedoutputstream != null) bufferedoutputstream.close();
                if (bufferedreader != null) bufferedreader.close();
                if (socket != null) socket.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            if (flagChar == '2' || flagChar == '3') flag = true; else flag = false;
        }
        return flag;
    }

    /**
	 * Save Image as URLConnection method
	 * Creation date: (2001-12-7 13:40:35)
	 * @param param java.net.URL
	 */
    private synchronized boolean saveU(URL url, String typeFlag, byte[] arrByte) {
        BufferedReader buffReader = null;
        BufferedOutputStream buffOS = null;
        URLConnection urlconnection = null;
        char flagChar = '0';
        boolean flag = true;
        try {
            urlconnection = url.openConnection();
            urlconnection.setDoOutput(true);
            urlconnection.setDoInput(true);
            urlconnection.setUseCaches(false);
            urlconnection.setRequestProperty("Content-type", "application/octet-stream");
            buffOS = new BufferedOutputStream(urlconnection.getOutputStream());
            buffOS.write((byte[]) typeFlag.getBytes());
            buffOS.write(arrByte);
            buffOS.flush();
            if (Config.DEBUG) System.out.println("Applet output file successfully! ");
            buffReader = new BufferedReader(new InputStreamReader(urlconnection.getInputStream()));
            StringBuffer stringBuff = new StringBuffer();
            String serReturnMess = buffReader.readLine();
            if (Config.DEBUG) System.out.println("Applet check status successfully! " + serReturnMess);
            flagChar = '2';
            if (serReturnMess != null) {
                stringBuff.append(serReturnMess);
                serReturnMess = serReturnMess.substring(serReturnMess.indexOf(32)).trim() + '2';
                flagChar = serReturnMess.charAt(0);
            }
            while ((serReturnMess = buffReader.readLine()) != null) {
                if (serReturnMess.length() <= 0) break;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (buffOS != null) buffOS.close();
                if (buffReader != null) buffReader.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            if (flagChar == '2' || flagChar == '3') flag = true; else flag = false;
        }
        return flag;
    }

    /**
	 * This method will execute when you initialize PaintCavas in PaintApplet
	 * Creation date: (2001-11-12 14:38:59)
	 */
    public void start() {
        oldImage = this.createImage(Config.CanvasW, Config.CanvasH);
        oldG = oldImage.getGraphics();
        currentImage = this.createImage(Config.CanvasW, Config.CanvasH);
        currentG = currentImage.getGraphics();
    }

    /**
	 * Update Canvas screen
	 * Creation date: (2001-12-17 15:06:56)
	 * @param g java.awt.Graphics
	 * @see #paint
	 # @see #repaint
	 */
    public void update(Graphics g) {
        paint(g);
    }

    public String getRegisterName() {
        return getName();
    }

    public IRegistryNode getRegisterNode() {
        return registerNode;
    }

    public RootRegistryNode getRootNode() {
        IRegistryNode node = getRegisterNode();
        while (!node.isRoot()) {
            node = (node).getParentNode();
        }
        return (RootRegistryNode) node;
    }

    public MessageBox getMsgBox() {
        if (msgBox == null) {
            msgBox = new MessageBox(this, getCanvasImage());
            msgBox.addMessageListener(mbEventHandler);
        }
        return msgBox;
    }

    public void setRegisterNode(IRegistryNode regNode) {
        registerNode = regNode;
    }

    public void update(IRegistryNode o, Object arg) {
        if (gObj != null && gObj instanceof TemplImg) {
            ((TemplImg) gObj).fixTempl();
            repaint();
        }
        if (getRootNode().getResource().getPenType() == Config.PenCurveLine) completeDraw();
        if (arg == null) return;
        int eventType = 0;
        try {
            eventType = Integer.parseInt((String) arg);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (eventType == Config.EventCtlPanelClear) {
            if (getRootNode().getEvent().getEventType() != Config.EventCtlPanelClear || getRootNode().getEvent().isDirty()) {
                existFunc = true;
                repaint();
            }
        }
        if (eventType == Config.EventCtlPanelUnDo || eventType == Config.EventCtlPanelReDo) {
            existFunc = true;
            repaint();
        }
    }

    /**
	 * This method is for JDK version under 1.3, which doesn't support
	 * getListener() method. So each time, add Mouse Listener will save
	 * Listener to a vector.
	 * @see Component#addMouseListener(MouseListener)
	 */
    public synchronized void addMouseListener(MouseListener l) {
        super.addMouseListener(l);
        mouseListeners.add(l);
    }

    /**
	 * This method is for JDK version under 1.3, which doesn't support
	 * getListener() method. So each time, add Mouse Listener will save
	 * Listener to a vector.
	 * 
	 * @see Component#addMouseMotionListener(MouseMotionListener)
	 */
    public synchronized void addMouseMotionListener(MouseMotionListener l) {
        super.addMouseMotionListener(l);
        mouseMoveListeners.add(l);
    }

    /**
	 * If JDK version is under 1.3, this method won't be supported!
	 * So, new method getListeners11() instead of Component.getListeners()
	 * @see Component#getListeners(Class)
	 */
    public EventListener[] getListeners11(Class listenerType) {
        if (listenerType == MouseListener.class) {
            MouseListener[] eventLis = new MouseListener[mouseListeners.size()];
            for (int i = 0; i < mouseListeners.size(); i++) eventLis[i] = (MouseListener) mouseListeners.get(i);
            return eventLis;
        }
        if (listenerType == MouseMotionListener.class) {
            MouseMotionListener[] eventLis = new MouseMotionListener[mouseMoveListeners.size()];
            for (int i = 0; i < mouseMoveListeners.size(); i++) eventLis[i] = (MouseMotionListener) mouseMoveListeners.get(i);
            return eventLis;
        }
        return null;
    }

    /**
	 * @see Component#removeMouseListener(MouseListener)
	 */
    public synchronized void removeMouseListener(MouseListener l) {
        super.removeMouseListener(l);
        mouseListeners.removeElement(l);
    }

    /**
	 * @see Component#removeMouseMotionListener(MouseMotionListener)
	 */
    public synchronized void removeMouseMotionListener(MouseMotionListener l) {
        super.removeMouseMotionListener(l);
        mouseMoveListeners.removeElement(l);
    }

    public void gotoURL() {
        try {
            getRootNode().getApplet().getAppletContext().showDocument(new URL("http://news.sina.com.cn"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
