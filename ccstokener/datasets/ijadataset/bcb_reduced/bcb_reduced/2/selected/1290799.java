package org.phylowidget.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.andrewberman.ui.Color;
import org.andrewberman.ui.FontLoader;
import org.andrewberman.ui.unsorted.MethodAndFieldSetter;
import org.phylowidget.PWContext;
import org.phylowidget.PWPlatform;
import org.phylowidget.PhyloTree;
import org.phylowidget.tree.PhyloNode;
import org.phylowidget.tree.RootedTree;

public class PhyloConfig {

    public boolean debug = false;

    public String remoteConfig = "";

    public String tree = DEFAULT_TREE;

    public String clipboard = "";

    public String search = "";

    public String layout = "Rectangular";

    public String menus = "context.xml;dock.xml;toolbar-new.xml;callbacks.xml";

    public String backgroundColor = "(255,255,255)";

    public String textColor = "(0,0,0)";

    public String nodeColor = "(0,0,0)";

    public String branchColor = "(0,0,0)";

    public String alignmentColor = "(140,190,50)";

    public String nodeShape = "circle";

    public String angleHandling = "level";

    public String lineStyle = "square";

    public String font = "Bitstream Vera Sans";

    public float textRotation = 0f;

    public float textScaling = .8f;

    public float imageSize = 0.95f;

    public float lineWidth = 1f;

    public float nodeSize = 2f;

    public float innerNodeRatio = 1f;

    public float renderThreshold = 500f;

    public float minTextSize = 8;

    public float branchScaling = 1f;

    public float cigarScaling = 10f;

    public float layoutAngle = 0f;

    public float animationFrames = 15f;

    public float viewportX = 0.0f;

    public float viewportY = 0.0f;

    public float viewportZoom = 0.8f;

    public boolean showScaleBar = false;

    public boolean showCladeLabels = false;

    public boolean useBranchLengths = false;

    public boolean showAllLabels = false;

    public boolean hideAllLabels = false;

    public boolean showAllLeafNodes = false;

    public boolean treatNodesAsLabels = false;

    public boolean prioritizeDistantLabels = false;

    public boolean alignLabels = false;

    public boolean useDoubleBuffering = false;

    public boolean antialias = false;

    public boolean outputAllInnerNodes = false;

    public boolean enforceUniqueLabels = false;

    public boolean scrapeNaughtyChars = false;

    public boolean outputFullSizeImages = false;

    public boolean useAnimations = true;

    public boolean animateNewTree = false;

    public boolean suppressMessages = false;

    public boolean colorHoveredBranch = false;

    public boolean respondToMouseWheel = true;

    public boolean ignoreAnnotations = false;

    public boolean showBootstrapValues = false;

    public boolean colorSpecies = true;

    public boolean colorDuplications = true;

    public boolean colorBootstrap = true;

    private PWContext context;

    public PhyloConfig() {
        super();
        context = PWPlatform.getInstance().getThisAppContext();
    }

    private Color backgroundC = Color.parseColor(backgroundColor);

    public void setBackgroundColor(String s) {
        backgroundC = Color.parseColor(s);
        backgroundColor = s;
    }

    public Color getBackgroundColor() {
        return Color.parseColor(backgroundColor);
    }

    private Color textC = Color.parseColor(textColor);

    public void setTextColor(String s) {
        textC = Color.parseColor(s);
        textColor = s;
    }

    public Color getTextColor() {
        if (textC != null) return textC; else return Color.parseColor(textColor);
    }

    private Color nodeC = Color.parseColor(nodeColor);

    public void setNodeColor(String s) {
        nodeC = Color.parseColor(s);
        nodeColor = s;
    }

    public Color getNodeColor() {
        if (nodeC != null) return nodeC; else return Color.parseColor(nodeColor);
    }

    private Color branchC = Color.parseColor(branchColor);

    public void setBranchColor(String s) {
        branchC = Color.parseColor(s);
        branchColor = s;
    }

    public Color getBranchColor() {
        if (branchC != null) return branchC; else return Color.parseColor(branchColor);
    }

    private Color alignmentC = Color.parseColor(alignmentColor);

    public void setAlignmentColor(String s) {
        alignmentC = Color.parseColor(s);
        alignmentColor = s;
    }

    public Color getAlignmentColor() {
        if (alignmentC != null) return alignmentC; else return Color.parseColor(alignmentColor);
    }

    public void setRespondToMouseWheel(boolean respond) {
        if (!respond) {
            context.trees().camera.makeUnresponsive();
        } else {
            context.trees().camera.makeResponsive();
        }
    }

    public void setTree(final String s) {
        new Thread() {

            public void run() {
                context.trees().setTree(s);
                tree = s;
            }
        }.start();
    }

    public void setClipboard(final String s) {
        new Thread() {

            public void run() {
                context.ui().clipboard.setClipFromJS(s);
            }
        }.start();
    }

    public void setUseBranchLengths(boolean useEm) {
        useBranchLengths = useEm;
        context.ui().layout();
    }

    public void setSearch(String s) {
        this.search = s;
        context.ui().search();
    }

    public void setEnforceUniqueLabels(boolean b) {
        enforceUniqueLabels = b;
        RootedTree t = context.trees().getTree();
        if (t != null) t.setEnforceUniqueLabels(b);
    }

    public void setLayout(String s) {
        if (!layout.equals(s)) layout = s;
        s = s.toLowerCase();
        if (s.equals("diagonal")) {
            context.trees().diagonalRender();
        } else if (s.equals("circular")) {
            context.trees().circleRender();
        } else if (s.equals("unrooted")) {
            context.trees().unrootedRender();
        } else {
            context.trees().rectangleRender();
        }
    }

    public void setRemoteConfig(String s) {
        try {
            HashMap<String, String> map = new HashMap<String, String>();
            URL url = new URL(s);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("#")) continue;
                String[] split = line.split("=");
                if (split.length >= 2) {
                    map.put(split[0], split[1]);
                }
            }
            MethodAndFieldSetter.setMethodsAndFields(this, map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setMenus(String menus) {
        this.menus = menus;
        new Thread("Menu Loader") {

            @Override
            public void run() {
                context.ui().setMenus();
            }
        }.start();
    }

    public void setShowAllLabels(boolean showAllLabels) {
        this.showAllLabels = showAllLabels;
    }

    public void setPrioritizeDistantLabels(boolean prioritizeDistanceLabels) {
        this.prioritizeDistantLabels = prioritizeDistanceLabels;
        context.ui().layout();
    }

    public void destroy() {
        tree = null;
    }

    public void setTextSize(float textSize) {
        this.textScaling = textSize;
    }

    public void setLayoutAngle(float layoutAngle) {
        this.layoutAngle = layoutAngle;
        context.ui().forceLayout();
    }

    public void setViewportX(float newX) {
        context.trees().camera.nudgeTo(-newX, context.trees().camera.getY());
    }

    public void setViewportY(float newY) {
        context.trees().camera.nudgeTo(context.trees().camera.getX(), -newY);
    }

    public void setViewportZoom(float newZoom) {
        context.trees().camera.zoomTo(newZoom);
    }

    public void setBranchScaling(float newBranchScaling) {
        this.branchScaling = newBranchScaling;
        context.ui().forceLayout();
    }

    public void setShowScaleBar(boolean show) {
        if (show) {
            context.trees().showScaleBar();
        } else {
            context.trees().hideScaleBar();
        }
    }

    public static final String DEFAULT_TREE = "PhyloWidget";

    public void setFont(String newFont) {
        FontLoader fl = context.trees().getRenderer().getFontLoader();
        fl.setFont(newFont);
        this.font = fl.getFontName();
    }

    public static Map<String, String> getChangedFields(Object a, Object b) {
        Class aClass = a.getClass();
        Class bClass = b.getClass();
        if (!aClass.equals(bClass)) {
            System.out.println("Classes a and b not equal!");
        }
        HashMap<String, String> changedFields = new HashMap<String, String>();
        Field[] fields = aClass.getFields();
        for (Field f : fields) {
            try {
                if (f.get(a).equals(f.get(b))) {
                } else {
                    changedFields.put(f.getName(), f.get(a).toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return changedFields;
    }

    public void setIgnoreAnnotations(boolean ignore) {
        this.ignoreAnnotations = ignore;
        context.ui().layout();
    }

    public static Map<String, String> getConfigSnapshot(PhyloConfig currentConfig) {
        Map<String, String> changed = getChangedFields(currentConfig, new PhyloConfig());
        changed.remove("viewportX");
        changed.remove("viewportY");
        changed.remove("viewportZ");
        changed.remove("menus");
        return changed;
    }
}
