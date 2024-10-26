package rene.zirkel;

import java.applet.AppletContext;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.MediaTracker;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import javax.swing.JPanel;
import rene.dialogs.Warning;
import rene.gui.DoActionListener;
import rene.gui.Global;
import rene.gui.HistoryTextField;
import rene.gui.IconBar;
import rene.gui.IconBarListener;
import rene.gui.Panel3D;
import rene.util.FileName;
import rene.util.parser.StringParser;
import rene.zirkel.construction.Construction;
import rene.zirkel.construction.ConstructionException;
import rene.zirkel.construction.Count;
import rene.zirkel.constructors.AngleConstructor;
import rene.zirkel.constructors.AreaConstructor;
import rene.zirkel.constructors.BoundedPointConstructor;
import rene.zirkel.constructors.Circle3Constructor;
import rene.zirkel.constructors.CircleConstructor;
import rene.zirkel.constructors.ExpressionConstructor;
import rene.zirkel.constructors.ImageConstructor;
import rene.zirkel.constructors.IntersectionConstructor;
import rene.zirkel.constructors.LineConstructor;
import rene.zirkel.constructors.MidpointConstructor;
import rene.zirkel.constructors.ObjectConstructor;
import rene.zirkel.constructors.ParallelConstructor;
import rene.zirkel.constructors.PlumbConstructor;
import rene.zirkel.constructors.PointConstructor;
import rene.zirkel.constructors.QuadricConstructor;
import rene.zirkel.constructors.RayConstructor;
import rene.zirkel.constructors.SegmentConstructor;
import rene.zirkel.constructors.TextConstructor;
import rene.zirkel.dialogs.CommentDialog;
import rene.zirkel.dialogs.Replay;
import rene.zirkel.listener.DoneListener;
import rene.zirkel.listener.StatusListener;
import rene.zirkel.macro.Macro;
import rene.zirkel.macro.MacroBar;
import rene.zirkel.macro.MacroItem;
import rene.zirkel.macro.MacroRunner;
import rene.zirkel.objects.ConstructionObject;
import rene.zirkel.objects.ExpressionObject;
import rene.zirkel.objects.PointObject;
import rene.zirkel.objects.PrimitiveCircleObject;
import rene.zirkel.objects.PrimitiveLineObject;
import rene.zirkel.objects.SegmentObject;
import rene.zirkel.objects.TextObject;
import rene.zirkel.tools.AnimatorTool;
import rene.zirkel.tools.BreakpointAnimator;
import rene.zirkel.tools.DeleteTool;
import rene.zirkel.tools.DrawerTool;
import rene.zirkel.tools.EditTool;
import rene.zirkel.tools.HiderTool;
import rene.zirkel.tools.MoverTool;
import rene.zirkel.tools.ObjectTracker;
import rene.zirkel.tools.RenamerTool;
import rene.zirkel.tools.ReorderTool;
import rene.zirkel.tools.SaveJob;
import rene.zirkel.tools.SetParameterTool;
import rene.zirkel.tools.SetTargetsTool;
import rene.zirkel.tools.Tracker;
import rene.zirkel.tools.ZoomerTool;
import eric.JLocusObjectTracker;

public class ZirkelApplet extends javax.swing.JApplet implements IconBarListener, StatusListener, KeyListener, DoneListener, DoActionListener, ZirkelCanvasInterface {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    ZirkelCanvas ZC;

    IconBar IA, IB, IC;

    MacroBar IM;

    String filename = "test.zir";

    Label Status;

    Color C, CC;

    Frame F;

    String FirstConstructor = "point", Tools = "all", Options = "";

    int CurrentTool = 0;

    HistoryTextField Input;

    ConstructionObject Last = null;

    boolean edit;

    boolean jumptostart;

    public String JScontrol() {
        String exp = "";
        try {
            exp = String.valueOf(ZC.getConstruction().find("jscode").getValue());
        } catch (final Exception ex) {
        }
        return exp;
    }

    public void JSsend(final String s) {
        if (s.startsWith("<")) {
            putConstruction(s);
        } else {
            ZC.JSsend(s);
            repaint();
        }
    }

    public String JSreceive(final String s) {
        if (s.equals("construction")) {
            return getConstruction();
        } else {
            return ZC.JSreceive(s);
        }
    }

    @Override
    public void paint(final Graphics gc) {
        super.paint(gc);
        ZC.JCM.readXmlTags();
        ZC.dovalidate();
        ZC.repaint();
    }

    public static final String DefaultIcons = " point line segment ray circle fixedcircle" + " parallel plumb circle3 midpoint angle fixedangle" + " move tracker objecttracker hide expression area text quadric" + " runmacro animate ";

    public static final String DefaultOptions = " back undo delete color type thickness" + " hidden showcolor macro grid " + " draw twolines indicate ";

    @Override
    public void init() {
        if (getParameter("language") != null) {
            try {
                Locale.setDefault(new Locale(getParameter("language"), ""));
            } catch (final RuntimeException e) {
            }
        }
        Global.initBundle("rene/zirkel/docs/ZirkelProperties");
        Count.resetAll();
        CC = C = Global.Background;
        initLightColors();
        initObjectKeys();
        final Dimension dscreen = getToolkit().getScreenSize();
        F = new Frame();
        F.setSize(dscreen);
        if (getParameter("oldicons") == null) {
            Global.setParameter("iconpath", "/eric/icons/palette/");
            Global.setParameter("iconsize", getParameter("smallicons") == null ? 32 : 24);
            Global.setParameter("icontype", "png");
        } else {
            Global.setParameter("iconpath", "//eric/icons/palette/");
            Global.setParameter("iconsize", 20);
        }
        String color = getParameter("color");
        if (color != null) {
            final StringParser p = new StringParser(getParameter("color"));
            p.replace(',', ' ');
            int red, green, blue;
            red = p.parseint();
            green = p.parseint();
            blue = p.parseint();
            C = new Color(red, green, blue);
            Global.Background = C;
            Global.ControlBackground = C;
            CC = C;
        }
        color = getParameter("colorbackground");
        if (color != null) {
            Global.setParameter("colorbackground", color);
        } else {
            Global.removeParameter("colorbackground");
        }
        color = getParameter("colorselect");
        if (color != null) {
            Global.setParameter("colorselect", color);
        } else {
            Global.removeParameter("colorselect");
        }
        ZirkelFrame.SelectColor = Global.getParameter("colorselect", Color.red);
        color = getParameter("colortarget");
        if (color != null) {
            Global.setParameter("colortarget", color);
        } else {
            Global.removeParameter("colortarget");
        }
        ZirkelFrame.TargetColor = Global.getParameter("colorselect", Color.pink);
        ZirkelFrame.initLightColors(Color.white);
        final String font = getParameter("font");
        if (font != null) {
            if (font.indexOf("bold") >= 0) {
                Global.setParameter("font.bold", true);
            }
            if (font.indexOf("large") >= 0) {
                Global.setParameter("font.large", true);
            }
        }
        if (getParameter("demo") != null) {
            initDemo();
            return;
        }
        String style = getParameter("style");
        if (style == null) {
            if ((getParameter("tools") != null) || (getParameter("options") != null)) {
                style = "icons";
            } else {
                style = "plain";
            }
        }
        final boolean simple = (style.equals("plain") || style.equals("3D") || style.equals("breaks"));
        edit = !simple;
        final boolean status = (style.equals("full") || style.equals("status"));
        final boolean icons = (style.equals("full") || style.equals("nonvisual") || style.equals("icons"));
        final boolean breaks = (style.equals("breaks"));
        if (getParameter("edit") != null) {
            edit = true;
        }
        for (int i = 0; i < ZirkelFrame.Colors.length; i++) {
            color = getParameter("color" + i);
            if (color != null) {
                Global.setParameter("color" + i, color);
            } else {
                Global.removeParameter("color" + i);
            }
        }
        ZirkelFrame.initLightColors(Color.white);
        getContentPane().setLayout(new BorderLayout());
        Tools = getParameter("tools");
        if (Tools == null || Tools.equals("defaults")) {
            Tools = DefaultIcons;
        }
        Options = getParameter("options");
        if (Options == null || Options.equals("defaults")) {
            Options = DefaultOptions;
        }
        Global.setParameter("macrobar", Options.indexOf("macrobar") >= 0);
        if (icons) {
            IB = new IconBar(F);
            IB.setBackground(CC);
            IB.setIconBarListener(this);
            StringTokenizer t = new StringTokenizer(Tools);
            int count = 0;
            while (t.hasMoreTokens()) {
                t.nextToken();
                count++;
            }
            final String a[] = new String[count];
            t = new StringTokenizer(Tools);
            count = 0;
            while (t.hasMoreTokens()) {
                a[count++] = t.nextToken();
            }
            if (count > 0) {
                FirstConstructor = a[0];
            }
            IB.addToggleGroupLeft(a);
            if (Options.indexOf("twolines") >= 0 || Options.indexOf("defaults") >= 0) {
                IA = new IconBar(F);
                IA.setBackground(CC);
                IA.setIconBarListener(this);
                setIA(IA, Options);
                final JPanel north = new JPanel();
                north.setBackground(CC);
                north.setLayout(new GridLayout(0, 1));
                north.add(IA);
                north.add(IB);
                if (IM != null) {
                    north.add(IM);
                }
                getContentPane().add("North", new Panel3D(north, CC));
            } else {
                IA = IB;
                setIA(IB, Options);
                if (IM != null) {
                    final JPanel north = new JPanel();
                    north.setBackground(CC);
                    north.setLayout(new GridLayout(0, 1));
                    north.add(IA);
                    if (IM != null) {
                        north.add(IM);
                    }
                    getContentPane().add("North", new Panel3D(north, CC));
                } else {
                    getContentPane().add("North", new Panel3D(IB, CC));
                }
            }
        } else {
            IA = IB = null;
        }
        rene.zirkel.Zirkel.IsApplet = true;
        Global.setParameter("options.choice", true);
        Global.setParameter("options.indicate", true);
        Global.setParameter("options.indicate.simple", false);
        Global.setParameter("options.intersection", false);
        Global.setParameter("options.pointon", false);
        eric.JMacrosTools.initObjectsProperties();
        if (getParameter("selectionsize") != null) {
            try {
                final double x = new Double(getParameter("selectionsize")).doubleValue();
                Global.setParameter("selectionsize", x);
            } catch (final Exception e) {
            }
        }
        ZC = new ZirkelCanvas(!edit, !breaks, !breaks);
        ZC.addMouseListener(ZC);
        ZC.addMouseMotionListener(ZC);
        ZC.setBackground(Global.getParameter("colorbackground", C));
        ZC.setFrame(F);
        ZC.setZirkelCanvasListener(this);
        if (getParameter("showhidden") != null) {
            ZC.setShowHidden(true);
        }
        if (style.equals("plain")) {
            getContentPane().add("Center", ZC);
        } else {
            getContentPane().add("Center", new Panel3D(ZC, ZC.getBackground()));
        }
        ZC.addStatusListener(this);
        ZC.addKeyListener(this);
        setShowNames(false);
        if (status) {
            Status = new Label("");
            Status.setBackground(CC);
            getContentPane().add("South", new Panel3D(Status, Status.getBackground()));
        } else if (style.equals("nonvisual")) {
            Input = new HistoryTextField(this, "Input");
            ZC.setTextField(Input);
            ZC.Visual = false;
            setShowNames(true);
            getContentPane().add("South", new Panel3D(Input));
        }
        try {
            Global.setParameter("digits.edit", Integer.parseInt(getParameter("editdigits")));
        } catch (final Exception e) {
        }
        try {
            Global.setParameter("digits.lengths", Integer.parseInt(getParameter("displaydigits")));
        } catch (final Exception e) {
        }
        try {
            Global.setParameter("digits.angles", Integer.parseInt(getParameter("angledigits")));
        } catch (final Exception e) {
        }
        setOption("movename");
        setOption("movefixname");
        ZC.updateDigits();
        setOption("nopopupmenu");
        setOption("nomousezoom");
        try {
            Global.setParameter("minpointsize", new Double(getParameter("minpointsize")).doubleValue());
        } catch (final Exception e) {
        }
        try {
            Global.setParameter("minlinesize", new Double(getParameter("minlinesize")).doubleValue());
        } catch (final Exception e) {
        }
        try {
            Global.setParameter("minfontsize", new Double(getParameter("minfontsize")).doubleValue());
        } catch (final Exception e) {
        }
        try {
            Global.setParameter("arrowsize", new Double(getParameter("arrowsize")).doubleValue());
        } catch (final Exception e) {
        }
        try {
            final String grid = getParameter("grid");
            ZC.ShowGrid = !grid.equals("none");
            Global.setParameter("grid.fine", grid.equals("coordinates"));
            if (getParameter("snap").equals("left")) {
                Global.setParameter("grid.leftsnap", true);
            }
        } catch (final Exception e) {
        }
        if (getParameter("interactive") != null && getParameter("interactive").equals("false")) {
            ZC.setInteractive(false);
        }
        boolean job = false;
        ZC.IW = getSize().width;
        ZC.IH = getSize().height;
        if (getParameter("germanpoints") != null && getParameter("germanpoints").equals("true")) {
            Global.setParameter("options.germanpoints", true);
        }
        try {
            final InputStream o = getClass().getResourceAsStream("/builtin.mcr");
            ZC.ProtectMacros = true;
            ZC.load(o, false, true);
            ZC.ProtectMacros = false;
            o.close();
            ZC.PM.removeAll();
        } catch (final Exception e) {
        }
        filename = getParameter("file");
        if (filename == null) {
            filename = getParameter("job");
            job = true;
        }
        if (filename != null) {
            boolean firsttry = true;
            while (true) {
                try {
                    URL url;
                    if (filename.toUpperCase().startsWith("HTTP")) {
                        url = new URL(firsttry ? FileName.toURL(filename) : filename);
                    } else {
                        url = new URL(getCodeBase(), firsttry ? FileName.toURL(filename) : filename);
                    }
                    ZC.clear();
                    InputStream in = url.openStream();
                    if (ZirkelFrame.isCompressed(filename)) {
                        in = new GZIPInputStream(in);
                    }
                    showStatus(Global.name("loading"));
                    ZC.load(in);
                    toggleGrid(ZC.ShowGrid);
                    if (job) {
                        ZC.displayJob(true);
                        ZC.setDoneListener(this);
                    }
                    if (icons) {
                        iconPressed(FirstConstructor);
                    }
                    in.close();
                    if (getParameter("background") != null) {
                        final Image i = getToolkit().getImage(new URL(getCodeBase(), getParameter("background")));
                        final MediaTracker mt = new MediaTracker(this);
                        mt.addImage(i, 0);
                        mt.waitForID(0);
                        if (mt.checkID(0) && !mt.isErrorAny()) {
                            ZC.setBackground(i);
                        }
                    }
                    ZC.repaint();
                } catch (final Exception e) {
                    if (firsttry) {
                        firsttry = false;
                        continue;
                    }
                    final Warning w = new Warning(F, FileName.chop(32, "" + e, 64), Zirkel.name("message"), true);
                    w.center(F);
                    w.setVisible(true);
                    showStatus("" + e);
                    e.printStackTrace();
                    System.out.println(e);
                }
                break;
            }
        }
        if (breaks) {
            IC = new IconBar(F);
            IC.setBackground(CC);
            IC.setIconBarListener(this);
            IC.addLeft("allback");
            if (haveBreaks()) {
                IC.addLeft("nextbreak");
            } else {
                IC.addLeft("oneforward");
            }
            IC.addLeft("allforward");
            final javax.swing.JPanel pic = new Panel3D(IC);
            getContentPane().add("South", pic);
            IC.setEnabled("nextbreak", false);
            IC.setEnabled("oneforward", false);
            IC.setEnabled("allforward", false);
            ZC.getConstruction().setOriginalOrder(true);
            jumptostart = (getParameter("jumptostart") != null);
        }
        if (getParameter("restrictedmove") != null) {
            Global.setParameter("restrictedmove", true);
        }
        if (getParameter("noconfirmation") != null) {
            Global.setParameter("confirmation", false);
        }
        if (IA != null) {
            settype(2);
        }
        ZC.setMacroBar(IM);
        ZC.updateMacroBar();
        ZC.recompute();
        ZC.setSize(ZC.getSize().width + 1, ZC.getSize().height + 1);
        ZC.setSize(ZC.getSize().width - 1, ZC.getSize().height - 1);
        ZC.JCM.hideHandles(null);
    }

    public void setOption(final String option) {
        try {
            Global.setParameter("options." + option, getParameter(option).equals("true"));
        } catch (final Exception e) {
        }
    }

    DemoRunner DR = null;

    void initDemo() {
        getContentPane().setLayout(new BorderLayout());
        ZC = new ZirkelCanvas(false, false, false);
        ZC.setBackground(Global.getParameter("colorbackground", C));
        ZC.setFrame(F);
        ZC.setZirkelCanvasListener(this);
        if (getParameter("showhidden") != null) {
            ZC.setShowHidden(true);
        }
        getContentPane().add("Center", new Panel3D(ZC, ZC.getBackground()));
        Status = new Label("", Label.CENTER);
        Status.setBackground(C);
        getContentPane().add("South", new Panel3D(Status, Status.getBackground()));
    }

    void setIA(final IconBar IA, final String set) {
        if (set.indexOf("rename") >= 0) {
            IA.addToggleLeft("rename");
        }
        if (set.indexOf("back") >= 0) {
            IA.addLeft("back");
        }
        if (set.indexOf("delete") >= 0) {
            IA.addToggleLeft("delete");
        }
        if (set.indexOf("undo") >= 0) {
            IA.addLeft("undo");
        }
        if (set.indexOf("hidden") >= 0) {
            IA.addOnOffLeft("hidden");
        }
        if (set.indexOf("showcolor") >= 0) {
            IA.addMultipleIconLeft("showcolor", ZirkelFrame.Colors.length);
        }
        if (set.indexOf("color") >= 0) {
            IA.addMultipleIconLeft("color", 6);
        }
        if (set.indexOf("type") >= 0) {
            IA.addMultipleIconLeft("type", 6);
        }
        if (set.indexOf("thickness") >= 0) {
            IA.addMultipleIconLeft("thickness", 3);
        }
        if (set.indexOf("partial") >= 0) {
            IA.addOnOffLeft("partial");
        }
        if (set.indexOf("plines") >= 0) {
            IA.addOnOffLeft("plines");
        }
        if (set.indexOf("arrow") >= 0) {
            IA.addOnOffLeft("arrow");
        }
        if (set.indexOf("showname") >= 0) {
            IA.addOnOffLeft("showname");
        }
        if (set.indexOf("showvalue") >= 0) {
            IA.addOnOffLeft("showvalue");
        }
        if (set.indexOf("edit") >= 0) {
            IA.addToggleLeft("edit");
        }
        if (set.indexOf("obtuse") >= 0) {
            IA.addOnOffLeft("obtuse");
        }
        if (set.indexOf("solid") >= 0) {
            IA.addOnOffLeft("solid");
        }
        if (set.indexOf("grid") >= 0) {
            IA.addOnOffLeft("grid");
        }
        if (set.indexOf("macro") >= 0) {
            IA.addMultipleToggleIconLeft("macro", 3);
        }
        if (set.indexOf("replay") >= 0) {
            IA.addLeft("replay");
        }
        if (set.indexOf("zoom") >= 0) {
            IA.addToggleLeft("zoom");
        }
        if (set.indexOf("comment") >= 0) {
            IA.addLeft("comment");
        }
        if (set.indexOf("function") >= 0) {
            IA.addLeft("function");
        }
        if (set.indexOf("draw") >= 0) {
            IA.addToggleLeft("draw");
        }
    }

    public void makeMacroBar() {
        if (Global.getParameter("macrobar", true)) {
            IM = new MacroBar(F);
            IM.addKeyListener(this);
            IM.setIconBarListener(this);
            IM.setBackground(CC);
        }
    }

    public void updateMacroBar() {
        ZC.updateMacroBar();
    }

    Replay RD = null;

    public void iconPressed(final String o) {
        if (RD != null) {
            RD.doclose();
        }
        ZC.requestFocus();
        if (IA != null && IA.isControlPressed()) {
            if (o.equals("edit")) {
                if (CurrentTool != ZirkelFrame.NEdit) {
                    IA.setState("edit", false);
                }
                ZC.editLast();
                ZC.repaint();
            }
            IA.clearShiftControl();
            return;
        }
        for (int i = 0; i < ZirkelFrame.ObjectStrings.length; i++) {
            if (o.equals(ZirkelFrame.ObjectStrings[i])) {
                if (i == ZirkelFrame.NMacro) {
                    runmacro(IB.isShiftPressed());
                } else {
                    settool(i);
                }
                return;
            }
        }
        if (IM != null) {
            final Macro m = IM.find(o);
            if (m != null) {
                runMacro(m);
                return;
            }
        }
        if (o.equals("hidden")) {
            ZC.setShowHidden(IA.getState("hidden"));
        } else if (o.equals("partial")) {
            ZC.setPartial(IA.getState("partial"));
        } else if (o.equals("plines")) {
            ZC.setPartialLines(IA.getState("plines"));
        } else if (o.equals("arrow")) {
            ZC.setVectors(IA.getState("arrow"));
        } else if (o.equals("color")) {
            final int n = IA.getMultipleState("color");
            if (n >= 0) {
                setcolor(n);
            }
        } else if (o.equals("showcolor")) {
            final int n = IA.getMultipleState("showcolor");
            if (n >= 0) {
                showcolor(n);
            }
        } else if (o.equals("showname")) {
            setShowNames(IA.getState("showname"));
        } else if (o.equals("obtuse")) {
            setObtuse(IA.getState("obtuse"));
        } else if (o.equals("solid")) {
            setSolid(IA.getState("solid"));
        } else if (o.equals("showvalue")) {
            setShowValues(IA.getState("showvalue"));
        } else if (o.equals("longnames")) {
            setLongNames(IA.getState("longnames"));
        } else if (o.equals("grid")) {
            toggleGrid(IA.getState("grid"));
        } else if (o.equals("comment")) {
            showcomment();
        } else if (o.equals("function")) {
            if (IA.isControlPressed()) {
                ZC.createFunction();
            } else {
                ZC.createCurve();
            }
        } else if (o.equals("type")) {
            final int n = IA.getMultipleState("type");
            if (n >= 0) {
                settype(n);
            }
        } else if (o.equals("thickness")) {
            final int n = IA.getMultipleState("thickness");
            if (n >= 0) {
                setcolortype(n);
            }
        } else if (o.equals("edit")) {
            settool(ZirkelFrame.NEdit);
        } else if (o.equals("back")) {
            ZC.back();
            ZC.repaint();
        } else if (o.equals("undo")) {
            ZC.undo();
            ZC.repaint();
        } else if (o.equals("replay")) {
            replay();
        } else if (o.equals("macro")) {
            final int n = IA.getMultipleState("macro");
            switch(n) {
                case 1:
                    settool(ZirkelFrame.NParameters);
                    break;
                case 2:
                    settool(ZirkelFrame.NTargets);
                    break;
                case 0:
                    definemacro();
                    break;
            }
            IA.setState("macro", true);
        } else if (o.equals("allback")) {
            if (Last != null && Last instanceof TextObject) {
                ((TextObject) Last).setDoShow(false);
            }
            final Enumeration e = ZC.getConstruction().elements();
            if (e.hasMoreElements()) {
                Last = (ConstructionObject) e.nextElement();
                ZC.paintUntil(Last);
                if ((Last instanceof TextObject) && Last.valid() && !Last.isSuperHidden()) {
                    ((TextObject) Last).setDoShow(true);
                }
            }
            IC.setEnabled("allforward", true);
            IC.setEnabled("nextbreak", true);
            IC.setEnabled("oneforward", true);
            IC.setEnabled("allback", false);
            if (haveBreaks() && !Last.isBreak()) {
                iconPressed("nextbreak");
            }
        } else if (o.equals("allforward")) {
            if (Last != null && Last instanceof TextObject) {
                ((TextObject) Last).setDoShow(false);
            }
            ZC.paintUntil(null);
            Last = null;
            IC.setEnabled("allforward", false);
            IC.setEnabled("nextbreak", false);
            IC.setEnabled("oneforward", false);
            IC.setEnabled("allback", true);
        } else if (o.equals("nextbreak")) {
            if (Last != null && Last instanceof TextObject) {
                ((TextObject) Last).setDoShow(false);
            }
            final Enumeration e = ZC.getConstruction().elements();
            outer: while (e.hasMoreElements()) {
                final ConstructionObject next = (ConstructionObject) e.nextElement();
                if (next == Last) {
                    while (e.hasMoreElements()) {
                        Last = (ConstructionObject) e.nextElement();
                        if (Last == null || Last.isBreak()) {
                            break outer;
                        }
                    }
                }
            }
            IC.setEnabled("allback", true);
            if ((Last instanceof TextObject) && Last.valid() && !Last.isSuperHidden()) {
                ((TextObject) Last).setDoShow(true);
            }
            ZC.paintUntil(Last);
            IC.setEnabled("allback", true);
            IC.setEnabled("nextbreak", e.hasMoreElements());
            IC.setEnabled("allforward", e.hasMoreElements());
        } else if (o.equals("oneforward")) {
            if (Last != null && Last instanceof TextObject) {
                ((TextObject) Last).setDoShow(false);
            }
            final Enumeration e = ZC.getConstruction().elements();
            outer: while (e.hasMoreElements()) {
                final ConstructionObject next = (ConstructionObject) e.nextElement();
                if (next == Last) {
                    while (e.hasMoreElements()) {
                        Last = (ConstructionObject) e.nextElement();
                        if (Last == null || !Last.isHidden()) {
                            break outer;
                        }
                        if ((Last instanceof TextObject) && Last.valid() && !Last.isSuperHidden()) {
                            ((TextObject) Last).setDoShow(true);
                            break outer;
                        }
                    }
                }
            }
            IC.setEnabled("allback", true);
            ZC.paintUntil(Last);
            if (!e.hasMoreElements()) {
                ZC.paintUntil(null);
                IC.setEnabled("allforward", false);
                IC.setEnabled("oneforward", false);
                IC.setEnabled("allback", true);
            }
        } else if (o.startsWith("bi_")) {
            eric.JGlobals.runmacro(ZC, this, "@builtin@/" + o.substring(3));
        }
        IA.clearShiftControl();
        IB.clearShiftControl();
    }

    public ObjectConstructor ObjectConstructors[] = { new PointConstructor(), new BoundedPointConstructor(), new IntersectionConstructor(), new LineConstructor(), new RayConstructor(), new SegmentConstructor(), new SegmentConstructor(true), new CircleConstructor(), new Circle3Constructor(), new CircleConstructor(true), new ParallelConstructor(), new PlumbConstructor(), new MidpointConstructor(), new AngleConstructor(), new AngleConstructor(true), new MoverTool(), new Tracker(), new ObjectTracker(), new AnimatorTool(), new ExpressionConstructor(), new AreaConstructor(), new QuadricConstructor(), new ImageConstructor(), new TextConstructor(), new HiderTool(), new JLocusObjectTracker(), new MacroRunner(), new EditTool(), new SetParameterTool(), new SetTargetsTool(), new SaveJob(), new DeleteTool(), new ReorderTool(), new DrawerTool(), new RenamerTool(), new ZoomerTool(), new BreakpointAnimator() };

    public void settool(final int i) {
        if (IM != null) {
            IM.deselectAll();
        }
        ZC.setTool(ObjectConstructors[i]);
        CurrentTool = i;
        if (i < ZirkelFrame.IconNumber && IB.have(ZirkelFrame.ObjectStrings[i])) {
            IB.toggle(ZirkelFrame.ObjectStrings[i]);
        } else {
            IB.unselect(FirstConstructor);
        }
        ObjectConstructors[i].resetFirstTime(ZC);
        if (i == ZirkelFrame.NTargets) {
            IA.setMultipleState("macro", 2);
        } else if (i == ZirkelFrame.NParameters) {
            IA.setMultipleState("macro", 1);
        } else {
            IA.setMultipleState("macro", 0);
            IA.setState("macro", false);
        }
        IA.setState("delete", i == ZirkelFrame.NDelete);
        IA.setState("edit", i == ZirkelFrame.NEdit);
        IA.setState("draw", i == ZirkelFrame.NDraw);
        IA.setState("rename", i == ZirkelFrame.NRename);
        IA.setState("zoom", i == ZirkelFrame.NZoom);
    }

    public void setcolor(final int c) {
        IA.setMultipleState("color", c);
        ZC.setDefaultColor(c);
    }

    public void settype(final int c) {
        IA.setMultipleState("type", c);
        ZC.setDefaultType(c);
    }

    public void setcolortype(final int c) {
        IA.setMultipleState("thickness", c);
        ZC.setDefaultColorType(c);
    }

    @Override
    public void showStatus(final String s) {
        super.showStatus(s);
        if (Status != null) {
            Status.setText(s);
        }
    }

    public void keyPressed(final KeyEvent e) {
    }

    public void keyReleased(final KeyEvent e) {
        final int code = e.getKeyCode();
        int i;
        final boolean Shift = e.isShiftDown(), Control = e.isControlDown(), Alt = e.isAltDown();
        if (Control) {
            if (Options.indexOf("type") > 0) {
                for (i = 0; i < ZirkelFrame.PointKeys.length; i++) {
                    if (ZirkelFrame.PointKeys[i] == code) {
                        settype(i);
                        return;
                    }
                }
            }
            if (Options.indexOf("color") > 0) {
                for (i = 0; i < ZirkelFrame.ColorKeys.length; i++) {
                    if (ZirkelFrame.ColorKeys[i] == code) {
                        setcolor(i);
                        return;
                    }
                }
            }
        } else if (Alt) {
            if (Options.indexOf("showcolor") > 0) {
                for (i = 0; i < ZirkelFrame.ColorKeys.length; i++) {
                    if (ZirkelFrame.ColorKeys[i] == code) {
                        showcolor(i);
                        return;
                    }
                }
            }
            if (Options.indexOf("thickness") > 0) {
                for (i = 0; i < ZirkelFrame.ColorTypeKeys.length; i++) {
                    if (ZirkelFrame.ColorTypeKeys[i] == code) {
                        setcolortype(i);
                        return;
                    }
                }
            }
        } else {
            switch(code) {
                case KeyEvent.VK_ESCAPE:
                    if (ZC.getCurrentTool() instanceof DrawerTool) {
                        ZC.clearDrawings();
                    } else {
                        ZC.reset();
                    }
                    break;
            }
        }
        if (!e.isActionKey()) {
            return;
        }
        switch(code) {
            case KeyEvent.VK_F1:
                showVersion();
                break;
            case KeyEvent.VK_F7:
                if (Shift || Control) {
                    setShowNames(!IA.getState("showname"));
                }
                break;
            case KeyEvent.VK_F8:
                if (Shift || Control) {
                    setLongNames(!IA.getState("longnames"));
                }
                break;
            case KeyEvent.VK_F9:
                if (Shift || Control) {
                    IA.setState("partial", !IA.getState("partial"));
                    ZC.setPartial(IA.getState("partial"));
                } else {
                    if (Options.indexOf("hidden") < 0) {
                        break;
                    }
                    IA.setState("hidden", !IA.getState("hidden"));
                    ZC.setShowHidden(IA.getState("hidden"));
                }
                break;
            case KeyEvent.VK_F10:
                if (Shift || Control) {
                    IA.setState("plines", !IA.getState("plines"));
                    ZC.setPartial(IA.getState("plines"));
                } else {
                    showcomment();
                }
                break;
            case KeyEvent.VK_F11:
                if (Shift || Control) {
                    IA.setState("arrow", !IA.getState("arrow"));
                    ZC.setPartial(IA.getState("arrow"));
                } else {
                    showconstruction();
                }
                break;
            case KeyEvent.VK_F12:
                if (Shift || Control) {
                    IA.setState("obtuse", !IA.getState("obtuse"));
                    ZC.setObtuse(IA.getState("obtuse"));
                } else {
                    toggleGrid();
                }
                break;
            case KeyEvent.VK_LEFT:
                if (Shift && ZC.getCurrentTool() instanceof ObjectTracker) {
                    ((ObjectTracker) ZC.getCurrentTool()).increaseOmit();
                } else if (Shift && ZC.getCurrentTool() instanceof BreakpointAnimator) {
                    ((BreakpointAnimator) ZC.getCurrentTool()).decreaseSpeed();
                } else if (Shift && ZC.getCurrentTool() instanceof AnimatorTool) {
                    ((AnimatorTool) ZC.getCurrentTool()).decreaseSpeed();
                } else {
                    ZC.shift(-0.2, 0);
                }
                break;
            case KeyEvent.VK_RIGHT:
                if (Shift && ZC.getCurrentTool() instanceof ObjectTracker) {
                    ((ObjectTracker) ZC.getCurrentTool()).decreaseOmit();
                } else if (Shift && ZC.getCurrentTool() instanceof BreakpointAnimator) {
                    ((BreakpointAnimator) ZC.getCurrentTool()).increaseSpeed();
                } else if (Shift && ZC.getCurrentTool() instanceof AnimatorTool) {
                    ((AnimatorTool) ZC.getCurrentTool()).increaseSpeed();
                } else {
                    ZC.shift(0.2, 0);
                }
                break;
            case KeyEvent.VK_UP:
                ZC.shift(0, 0.2);
                break;
            case KeyEvent.VK_DOWN:
                ZC.shift(0, -0.2);
                break;
        }
    }

    public void keyTyped(final KeyEvent e) {
        final char c = e.getKeyChar();
        if (e.isControlDown() || e.isAltDown()) {
            return;
        }
        int i;
        for (i = 0; i < ZirkelFrame.ObjectKeys.length; i++) {
            if (c == ZirkelFrame.ObjectKeys[i]) {
                if (Tools.indexOf(ZirkelFrame.ObjectStrings[i]) >= 0) {
                    settool(i);
                }
                return;
            }
        }
        switch(c) {
            case KeyEvent.VK_BACK_SPACE:
                ZC.undo();
                ZC.repaint();
                return;
            case '+':
                ZC.magnify(1 / Math.sqrt(Math.sqrt(2)));
                break;
            case '-':
                ZC.magnify(Math.sqrt(Math.sqrt(2)));
                break;
        }
    }

    public void initLightColors() {
        final int n = ZirkelFrame.Colors.length;
        ZirkelFrame.LightColors = new Color[n];
        final Color back = C;
        final int red = back.getRed(), green = back.getGreen(), blue = back.getBlue();
        final double lambda = 0.4;
        for (int i = 0; i < n; i++) {
            final int r = (int) (red * (1 - lambda) + ZirkelFrame.Colors[i].getRed() * lambda);
            final int g = (int) (green * (1 - lambda) + ZirkelFrame.Colors[i].getGreen() * lambda);
            final int b = (int) (blue * (1 - lambda) + ZirkelFrame.Colors[i].getBlue() * lambda);
            ZirkelFrame.LightColors[i] = new Color(r, g, b);
        }
    }

    public void initObjectKeys() {
        ZirkelFrame.ObjectKeys = new char[ZirkelFrame.ObjectStrings.length];
        for (int i = 0; i < ZirkelFrame.ObjectStrings.length; i++) {
            final String shortcut = Zirkel.name("shortcuts." + ZirkelFrame.ObjectStrings[i]);
            if (shortcut.length() > 0) {
                ZirkelFrame.ObjectKeys[i] = shortcut.charAt(0);
            }
        }
    }

    public void notifyDone() {
        repaint();
        try {
            Thread.sleep(500);
        } catch (final Exception e) {
        }
        if (Global.getParameter("confirmation", true)) {
            final Warning w = new Warning(F, Zirkel.name("done"), Zirkel.name("message"), true);
            w.center(F);
            w.setVisible(true);
        }
        final String sol = getParameter("solution");
        if (sol != null) {
            try {
                final AppletContext ac = getAppletContext();
                ac.showDocument(new URL(getCodeBase(), FileName.toURL(sol)));
            } catch (final Exception e) {
                System.out.println(e);
                e.printStackTrace();
            }
        }
    }

    public void showcolor(final int c) {
        ZC.setShowColor(c);
    }

    public void showcomment() {
        if (F == null) {
            return;
        }
        final CommentDialog d = new CommentDialog(F, ZC.getComment(), Zirkel.name("comment.title"), ZC.displayJob());
        ZC.setComment(d.getText());
    }

    public void showconstruction() {
        if (F == null) {
            return;
        }
    }

    public void toggleGrid() {
        ZC.toggleShowGrid();
    }

    void definemacro() {
        if (!ZC.defineMacro()) {
            return;
        }
        if (ZC.getOC() instanceof SetTargetsTool) {
            settool(ZirkelFrame.NParameters);
        } else {
            ZC.getOC().reset(ZC);
        }
    }

    String OldMacro = null;

    public void runmacro(final boolean shift) {
        Macro m = null;
        if (OldMacro == null && ZC.getMacros().size() == 1) {
            m = ((MacroItem) ZC.getMacros().elementAt(0)).M;
        } else {
            m = ZC.chooseMacro(OldMacro);
            if (!shift || m == null) {
                m = ZC.chooseMacro();
            }
        }
        if (m == null) {
            settool(CurrentTool);
            return;
        }
        runMacro(m);
    }

    public void runMacro(final Macro m) {
        ((MacroRunner) ObjectConstructors[ZirkelFrame.NMacro]).setMacro(m, ZC);
        settool(ZirkelFrame.NMacro);
        if (IM != null) {
            IM.select(m);
        }
        IB.setMultipleState("macro", 0);
        OldMacro = m.getName();
    }

    public void setShowNames(final boolean flag) {
        if (IA != null) {
            IA.setState("showname", flag);
        }
        Global.setParameter("options.point.shownames", flag);
        ZC.setShowNames(flag);
    }

    public void setObtuse(final boolean flag) {
        IA.setState("obtuse", flag);
        Global.setParameter("options.obtuse", flag);
        ZC.setObtuse(flag);
    }

    public void setSolid(final boolean flag) {
        IA.setState("solid", flag);
        Global.setParameter("options.solid", flag);
        ZC.setSolid(flag);
    }

    public void setShowValues(final boolean flag) {
        IA.setState("showvalue", flag);
        Global.setParameter("options.showvalue", flag);
        ZC.setShowValues(flag);
    }

    public void setLongNames(final boolean flag) {
        IA.setState("longnames", flag);
        Global.setParameter("options.longnames", flag);
        ZC.setLongNames(flag);
    }

    public void toggleGrid(final boolean grid) {
        ZC.setShowGrid(grid);
        if (IA != null) {
            IA.setState("grid", grid);
        }
    }

    public void loadsettings() {
        setcolor(ZC.getDefaultColor());
        settype(ZC.getDefaultType());
        setcolortype(ZC.getDefaultColorType());
        IA.setState("partial", ZC.getPartial());
        IA.setState("plines", ZC.getPartialLines());
        IA.setState("arrow", ZC.getVectors());
        ZC.setHidden(false);
    }

    public void doAction(final String o) {
        if (o.equals("Input")) {
            try {
                ZC.getConstruction().interpret(ZC, Input.getText(), "");
                Input.remember();
                Input.setText("");
                loadsettings();
                ZC.validate();
                ZC.getConstruction().updateCircleDep();
            } catch (final ConstructionException e) {
                ZC.warning(e.getDescription());
            }
        }
    }

    public void itemAction(final String o, final boolean flag) {
    }

    AnimatorTool A = null;

    @Override
    public void start() {
        if (getParameter("demo") != null) {
            DR = new DemoRunner(ZC, this, getParameter("demo"), Status);
            return;
        }
        final Construction C = ZC.getConstruction();
        ZC.paint(ZC.getGraphics());
        ZC.allowRightMouse(true);
        if (C.TrackP != null) {
            try {
                final ConstructionObject P = C.find(C.TrackP);
                if (!((P instanceof PointObject) || (P instanceof PrimitiveLineObject))) {
                    throw new ConstructionException("");
                }
                final ConstructionObject po[] = new ConstructionObject[C.TrackPO.size()];
                for (int i = 0; i < po.length; i++) {
                    final ConstructionObject o = C.find((String) C.TrackPO.elementAt(i));
                    if (o == null || !((o instanceof PointObject) || (o instanceof PrimitiveLineObject))) {
                        throw new ConstructionException("");
                    }
                    po[i] = o;
                }
                PointObject PM = null;
                if (C.TrackPM != null) {
                    PM = (PointObject) C.find(C.TrackPM);
                }
                if (C.TrackO != null) {
                    final ConstructionObject O = C.find(C.TrackO);
                    if (P == null || (PM == null && (O instanceof ExpressionObject)) || O == null) {
                        throw new ConstructionException("");
                    }
                    final ObjectTracker ot = new ObjectTracker(P, PM, O, ZC, C.Animate, C.Paint, po);
                    if (C.Animate) {
                        ot.Interactive = false;
                    }
                    ot.setOmit(C.Omit);
                    ZC.setTool(ot);
                    if (!edit) {
                        ZC.allowRightMouse(false);
                    }
                    ZC.validate();
                    ZC.repaint();
                } else {
                    if (P == null) {
                        throw new ConstructionException("");
                    }
                    ZC.setTool(new Tracker(P, po));
                    if (PM != null) {
                        PM.setSelected(true);
                    }
                    ZC.validate();
                    ZC.repaint();
                }
            } catch (final Exception e) {
            }
        } else if (C.AnimateP != null) {
            try {
                final PointObject P = (PointObject) C.find(C.AnimateP);
                if (P == null || !P.moveable()) {
                    throw new ConstructionException("");
                }
                final Enumeration e = C.AnimateV.elements();
                while (e.hasMoreElements()) {
                    final String s = (String) e.nextElement();
                    final ConstructionObject o = C.find(s);
                    if (o == null || !(o instanceof SegmentObject || o instanceof PrimitiveCircleObject || o instanceof PointObject)) {
                        throw new ConstructionException("");
                    }
                }
                ZC.setTool(A = new AnimatorTool(P, C.AnimateV, ZC, C.AnimateNegative, C.AnimateOriginal, C.AnimateDelay));
                ZC.allowRightMouse(false);
                A.setInteractive(false);
            } catch (final Exception e) {
            }
        } else if (C.AnimateBreakpoints) {
            final BreakpointAnimator bp = new BreakpointAnimator();
            bp.setLoop(C.AnimateLoop);
            bp.setSpeed(C.AnimateTime);
            ZC.allowRightMouse(false);
            ZC.setTool(bp);
            bp.reset(ZC);
        } else if (jumptostart) {
            System.out.println("here");
            iconPressed("allback");
        }
    }

    public void showVersion() {
        showStatus(Zirkel.name("program.name") + " " + Zirkel.name("program.version") + " " + Zirkel.name("program.date"));
    }

    @Override
    public void stop() {
        ZC.invalidate();
        if (DR != null) {
            DR.stop();
        }
    }

    @Override
    public void destroy() {
        ZC.invalidate();
        if (DR != null) {
            DR.stop();
        }
    }

    public void replay() {
        if (RD != null) {
            return;
        }
        RD = new Replay(F, ZC) {

            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void doclose() {
                RD = null;
                super.doclose();
            }
        };
        final Dimension d = getToolkit().getScreenSize();
        RD.setLocation(d.width - 100 - RD.getSize().width, 100);
        ZC.OC.invalidate(ZC);
        RD.setVisible(true);
    }

    public void replayChosen() {
        replay();
    }

    public boolean enabled(final String function) {
        if (Tools.indexOf("all") >= 0) {
            return true;
        }
        return Tools.indexOf(function) >= 0;
    }

    public boolean haveBreaks() {
        final Enumeration e = ZC.getConstruction().elements();
        while (e.hasMoreElements()) {
            if (((ConstructionObject) e.nextElement()).isBreak()) {
                return true;
            }
        }
        return false;
    }

    public String loadImage() {
        return null;
    }

    public Image doLoadImage(final String filename) {
        Image i;
        try {
            i = getToolkit().getImage(new URL(getCodeBase(), filename));
            final MediaTracker mt = new MediaTracker(this);
            mt.addImage(i, 0);
            mt.waitForID(0);
            if (mt.checkID(0) && !mt.isErrorAny()) {
                return i;
            }
        } catch (final Exception e) {
            showStatus(e.toString());
        }
        return null;
    }

    /**
     * For external scripting: Interprets a command s using the internal
     * construction language. (Expl: "A=point()")
     *
     * @param s
     * @return Command executed or not
     */
    public boolean interpret(final String s) {
        try {
            ZC.getConstruction().interpret(ZC, s);
            ZC.repaint();
            return true;
        } catch (final ConstructionException e) {
            return false;
        }
    }

    /**
     * For external scripting: Gets the construction as an XML stream contained
     * in a string.
     *
     * @return Construction.
     */
    public String getConstruction() {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            ZC.save(out, true, false, false, ZC.getMacros(), "");
            return out.toString("utf-8");
        } catch (Exception ex) {
            return "Error";
        }
    }

    /**
     * For external scripting: Receives the construction as an XML stream,
     * contained in a string.
     *
     * @param s
     * @return Success
     */
    public boolean putConstruction(final String s) {
        try {
            final ByteArrayOutputStream bout = new ByteArrayOutputStream();
            final PrintWriter out = new PrintWriter(new OutputStreamWriter(bout, "utf-8"));
            out.print(s);
            out.close();
            final byte b[] = bout.toByteArray();
            final InputStream in = new ByteArrayInputStream(b);
            ZC.load(in, true, true);
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
