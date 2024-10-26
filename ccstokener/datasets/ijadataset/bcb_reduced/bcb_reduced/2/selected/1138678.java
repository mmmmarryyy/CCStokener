package com.ladydinde;

import java.net.URL;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import android.view.MotionEvent;
import com.turboconnard.android.dialogs.AlertDialog;
import com.turboconnard.core.Agd;
import com.turboconnard.data.Library;
import com.turboconnard.display.Plane;
import com.turboconnard.display.Ribbon;
import com.turboconnard.display.Sprite;
import com.turboconnard.events.AccelerometerEvent;
import com.turboconnard.events.Event;
import com.turboconnard.events.MenuEvent;
import com.turboconnard.geom.Point3D;
import com.turboconnard.hardware.Accelerometer;
import com.turboconnard.menu.AndroidMenu;
import com.turboconnard.menu.AndroidMenuItem;
import com.turboconnard.net.GMLHandler;
import com.turboconnard.net.GMLObject;

public class LadyDindeAGD extends Agd {

    private final int PLAYBACK = 0;

    private final int RECORD = 1;

    private int MODE = 0;

    private boolean first = true;

    private float _x;

    private float _y;

    private float _z;

    private Sprite s;

    private Ribbon c;

    private boolean deuz;

    private Plane c1;

    private Plane c2;

    private float speedx;

    private float speedy;

    private float ralenti = 0.70f;

    private float cool = 0.30f;

    private Point3D point;

    private float vitX;

    private float vitY;

    private float cibleX;

    private float cibleY;

    private long start;

    private int _currentPoint;

    private GMLObject gml;

    public LadyDindeAGD() {
        super();
        Accelerometer.getInstance().addEventListener(AccelerometerEvent.ON_CHANGE, this);
        AndroidMenu am = new AndroidMenu();
        AndroidMenuItem edit = new AndroidMenuItem(Library.getInstance().getText(R.string.editMode), android.R.drawable.btn_minus);
        AndroidMenuItem play = new AndroidMenuItem(Library.getInstance().getText(R.string.playbackMode), android.R.drawable.ic_media_play);
        AndroidMenuItem about = new AndroidMenuItem(Library.getInstance().getText(R.string.about), android.R.drawable.ic_menu_info_details);
        edit.addEventListener(MenuEvent.SELECT, this);
        am.addItem(edit);
        am.addItem(play);
        am.addItem(about);
        menu.currentMenu = am;
        init();
    }

    public void loadRandomTag() {
        try {
            URL url = new URL("http://000000book.com/data/random.gml");
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            GMLHandler myExampleHandler = new GMLHandler();
            xr.setContentHandler(myExampleHandler);
            xr.parse(new InputSource(url.openStream()));
            gml = myExampleHandler.getGML();
        } catch (Exception e) {
        }
    }

    public void init() {
        stage.removeAllChilds();
        stage.clear();
        if (MODE == PLAYBACK) {
            loadRandomTag();
            start = System.currentTimeMillis();
            c = new Ribbon(10);
            c.addPoints(gml.points);
            s = new Sprite();
            _currentPoint = 0;
            s.x = 0;
            s.rotationZ = 90;
            s.y = stage.height;
            s.x = (stage.width - gml.screenBoundsX) / 2;
            stage.addChild(s);
            s.addChild(c);
        } else {
            deuz = false;
            first = true;
            _y = _x = _z = 0;
            c = new Ribbon(10);
            s = new Sprite();
            s.x = stage.width / 2;
            s.y = stage.height / 2;
            stage.addChild(s);
            s.addChild(c);
        }
    }

    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            init();
            if (first) {
                first = false;
                point = new Point3D(e.getX() - stage.width / 2, e.getY() - stage.height / 2, 0);
                speedx = 0;
                speedy = 0;
            }
            if (e.getX() < 100 && e.getY() > (stage.height - 50)) {
                init();
            }
        }
        return true;
    }

    public void update() {
        if (MODE == PLAYBACK) {
            cibleX += (vitX - cibleX) / 5;
            cibleY += (vitY - cibleY) / 5;
        } else {
            if (!first) {
                _z += 1;
                if (Math.abs(speedx) > 2 || Math.abs(speedy) > 2) {
                    c.addPoint(point.x, point.y, _z);
                }
                speedx = speedx * ralenti + (_x - stage.width / 2 - point.x) * cool;
                speedy = speedy * ralenti + (_y - stage.height / 2 - point.y) * cool;
                point.x += speedx;
                point.y += speedy;
                s.z -= 1;
            } else if (deuz) {
                cibleX += (vitX - cibleX) / 5;
                cibleY += (vitY - cibleY) / 5;
            }
        }
    }

    public void event(Event e) {
        if (e.name == MenuEvent.SELECT) {
            AlertDialog a = new AlertDialog();
            a.show();
            Plane c = new Plane(50, 50);
            c.x = stage.width / 2;
            c.y = stage.height / 2;
            stage.addChild(c);
        }
        if (e.name == AccelerometerEvent.ON_CHANGE) {
            Point3D p = (Point3D) e.param;
            vitX = p.y / 2;
            vitY = p.x / 2;
        }
    }
}
