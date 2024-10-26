package com.infocetera.util;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.*;

/**
 * Class GuiUtils _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class GuiUtils {

    /** _more_ */
    public static final int PT_NONE = 0;

    /** _more_ */
    public static final int PT_NW = 1;

    /** _more_ */
    public static final int PT_N = 2;

    /** _more_ */
    public static final int PT_NE = 3;

    /** _more_ */
    public static final int PT_W = 4;

    /** _more_ */
    public static final int PT_C = 5;

    /** _more_ */
    public static final int PT_E = 6;

    /** _more_ */
    public static final int PT_SW = 7;

    /** _more_ */
    public static final int PT_S = 8;

    /** _more_ */
    public static final int PT_SE = 9;

    /** _more_ */
    public static final String[] PTS = { "none", "nw", "n", "ne", "w", "c", "e", "sw", "s", "se" };

    /** _more_ */
    public static final int BORDER_EMPTY = 0;

    /** _more_ */
    public static final int BORDER_RAISED = 1;

    /** _more_ */
    public static final int BORDER_SUNKEN = 2;

    /** _more_ */
    public static final int BORDER_ETCHED = 3;

    /** _more_ */
    public static final int BORDER_MATTE = 4;

    /** _more_ */
    public static final String[] BORDERS = { "empty", "raised", "sunken", "etched", "matte" };

    /** _more_ */
    public static final Insets borderInsets = new Insets(1, 1, 1, 1);

    /** _more_ */
    public static final Color[] borderColors = { null, null, null, null };

    /** _more_ */
    private static Insets dfltInsets = new Insets(0, 0, 0, 0);

    /** _more_ */
    public static Insets tmpInsets;

    /** _more_ */
    private static final int DFLT_ANCHOR = GridBagConstraints.WEST;

    /** _more_ */
    public static int tmpAnchor = -1;

    /** _more_ */
    public static int tmpFill = -1;

    /** _more_ */
    public static final Insets ZERO_INSETS = new Insets(0, 0, 0, 0);

    /** _more_ */
    public static final double[] DS_Y = { 1 };

    /** _more_ */
    public static final double[] DS_YY = { 1, 1 };

    /** _more_ */
    public static final double[] DS_YYY = { 1, 1, 1 };

    /** _more_ */
    public static final double[] DS_N = { 0 };

    /** _more_ */
    public static final double[] DS_NN = { 0 };

    /** _more_ */
    public static final double[] DS_NNN = { 0 };

    /** _more_ */
    public static final double[] DS_NNNY = { 0, 0, 0, 1 };

    /** _more_ */
    public static final double[] DS_NNNNY = { 0, 0, 0, 0, 0, 1 };

    /** _more_ */
    public static final double[] DS_YN = { 1, 0 };

    /** _more_ */
    public static final double[] DS_NY = { 0, 1 };

    /** _more_ */
    public static final double[] DS_NYN = { 0, 1, 0 };

    /** _more_ */
    public static final double[] DS_YNY = { 1, 0, 1 };

    /** _more_ */
    public static final double[] DS_YNN = { 1, 0, 0 };

    /** _more_ */
    public static final double[] DS_NNY = { 0, 0, 1 };

    /** _more_ */
    public static final double[] DS_NNYN = { 0, 0, 1, 0 };

    /** _more_ */
    public static final double[] DS_NNYNY = { 0, 0, 1, 0, 1 };

    /** _more_ */
    public static final double[] DS_NNYNYNY = { 0, 0, 1, 0, 1, 0, 1 };

    /** _more_ */
    public static final double[] DS_NYNY = { 0, 1, 0, 1 };

    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     */
    public static Container wrap(Component c) {
        return doLayout(new Component[] { c }, 1, DS_N, DS_N);
    }

    /**
     * _more_
     *
     * @param c _more_
     * @param h _more_
     * @param v _more_
     *
     * @return _more_
     */
    public static Container inset(Component c, int h, int v) {
        tmpInsets = new Insets(v, h, v, h);
        return doLayout(new Component[] { c }, 1, DS_Y, DS_Y);
    }

    /**
     * _more_
     *
     * @param c _more_
     * @param borderType _more_
     * @param hGap _more_
     * @param vGap _more_
     *
     * @return _more_
     */
    public static Container border(Component c, int borderType, int hGap, int vGap) {
        tmpInsets = new Insets(vGap, hGap, vGap, hGap);
        JPanel p = new BorderPanel(borderType, new Insets(vGap, hGap, vGap, hGap), borderColors, null);
        doLayout(p, new Component[] { c }, 1, DS_Y, DS_N);
        return p;
    }

    /**
     * _more_
     *
     * @param commands _more_
     *
     * @return _more_
     */
    public static Vector parseCommands(String commands) {
        StringTokenizer tok = new StringTokenizer(commands, ";");
        Vector v = new Vector();
        while (tok.hasMoreTokens()) {
            String cmd = tok.nextToken().trim();
            int index = cmd.indexOf("(");
            if ((index >= 0) && cmd.endsWith(")")) {
                String funcName = cmd.substring(0, index);
                String params = cmd.substring(index + 1).trim();
                params = params.substring(0, params.length() - 1);
                v.addElement(new String[] { funcName, params });
            } else {
                v.addElement(new String[] { cmd, "" });
            }
        }
        return v;
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static double[] parseDoubles(String s) {
        if (s == null) {
            return null;
        }
        StringTokenizer tok = new StringTokenizer(s, ",");
        double[] cw = new double[tok.countTokens()];
        int cnt = 0;
        while (tok.hasMoreTokens()) {
            cw[cnt++] = new Double(tok.nextToken()).doubleValue();
        }
        return cw;
    }

    /**
     * _more_
     *
     * @param l _more_
     * @param c _more_
     * @param r _more_
     *
     * @return _more_
     */
    public static JPanel leftCenterRight(Component l, Component c, Component r) {
        JPanel cont = new JPanel(new BorderLayout());
        if (l != null) {
            cont.add("West", l);
        }
        if (c != null) {
            cont.add("Center", c);
        }
        if (r != null) {
            cont.add("East", r);
        }
        return cont;
    }

    /**
     * _more_
     *
     * @param t _more_
     * @param c _more_
     * @param b _more_
     *
     * @return _more_
     */
    public static JPanel topCenterBottom(Component t, Component c, Component b) {
        JPanel cont = new JPanel(new BorderLayout());
        if (t != null) {
            cont.add("North", t);
        }
        if (c != null) {
            cont.add("Center", c);
        }
        if (b != null) {
            cont.add("South", b);
        }
        return cont;
    }

    /**
     * _more_
     *
     * @param comps _more_
     * @param hgap _more_
     *
     * @return _more_
     */
    public static Container flow(Component[] comps, int hgap) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, hgap, 0));
        for (int i = 0; i < comps.length; i++) {
            p.add(comps[i]);
        }
        return p;
    }

    /**
     * _more_
     *
     * @param comps _more_
     * @param cols _more_
     * @param wx _more_
     * @param wy _more_
     *
     * @return _more_
     */
    public static Container doLayout(Component[] comps, int cols, double wx, double wy) {
        JPanel c = new JPanel();
        double[] wxa = { wx };
        double[] wya = { wy };
        return doLayout(c, comps, cols, wxa, wya);
    }

    /**
     * _more_
     *
     * @param comps _more_
     * @param cols _more_
     * @param weightsX _more_
     * @param weightsY _more_
     *
     * @return _more_
     */
    public static Container doLayout(Component[] comps, int cols, double[] weightsX, double[] weightsY) {
        return doLayout(new JPanel(), comps, cols, weightsX, weightsY);
    }

    /**
     * _more_
     *
     * @param c _more_
     * @param comps _more_
     * @param cols _more_
     * @param weightsX _more_
     * @param weightsY _more_
     *
     * @return _more_
     */
    public static Container doLayout(Container c, Component[] comps, int cols, double[] weightsX, double[] weightsY) {
        return doLayout(c, comps, cols, weightsX, weightsY, null, null);
    }

    /**
     * _more_
     *
     * @param c _more_
     * @param comps _more_
     * @param cols _more_
     * @param weightsX _more_
     * @param weightsY _more_
     * @param anchors _more_
     * @param fills _more_
     *
     * @return _more_
     */
    public static Container doLayout(Container c, Component[] comps, int cols, double[] weightsX, double[] weightsY, Hashtable anchors, Hashtable fills) {
        GridBagLayout l = new GridBagLayout();
        c.setLayout(l);
        GridBagConstraints consts = new GridBagConstraints();
        consts.insets = dfltInsets;
        if (tmpInsets != null) {
            consts.insets = tmpInsets;
        } else {
            consts.insets = dfltInsets;
        }
        tmpInsets = null;
        int dfltAnchor = ((tmpAnchor == -1) ? DFLT_ANCHOR : tmpAnchor);
        tmpAnchor = -1;
        int dfltFill = ((tmpFill >= 0) ? tmpFill : GridBagConstraints.BOTH);
        tmpFill = -1;
        int col = 0;
        int row = 0;
        double weightX = 1.0;
        double weightY = 0.0;
        for (int i = 0; i < comps.length; i++) {
            consts.anchor = dfltAnchor;
            consts.fill = dfltFill;
            if ((weightsX != null) && (col < weightsX.length)) {
                weightX = weightsX[col];
            }
            if ((weightsY != null) && (row < weightsY.length)) {
                weightY = weightsY[row];
            }
            boolean lastCol = false;
            if (col == (cols - 1)) {
                lastCol = true;
                consts.gridwidth = GridBagConstraints.REMAINDER;
            } else {
                col++;
            }
            consts.weightx = weightX;
            consts.weighty = weightY;
            Component comp = comps[i];
            if ((anchors != null) && (comp != null)) {
                Integer anchor = (Integer) anchors.get(comp);
                if (anchor != null) {
                    consts.anchor = anchor.intValue();
                }
            }
            if ((fills != null) && (comp != null)) {
                Integer fill = (Integer) fills.get(comp);
                if (fill != null) {
                    consts.fill = fill.intValue();
                }
            }
            if (comp != null) {
                l.setConstraints(comp, consts);
                c.add(comp);
            }
            if (lastCol) {
                col = 0;
                row++;
            }
            consts.gridwidth = 1;
        }
        return c;
    }

    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public static Component[] getCompArray(Vector v) {
        Component[] compArray = new Component[v.size()];
        for (int i = 0; i < v.size(); i++) {
            compArray[i] = (Component) v.elementAt(i);
        }
        return compArray;
    }

    /**
     * _more_
     *
     * @param g _more_
     * @param xs _more_
     * @param ys _more_
     */
    public static void drawPolyLine(Graphics g, int[] xs, int[] ys) {
        int i;
        for (i = 1; i < xs.length; i++) {
            g.drawLine(xs[i - 1], ys[i - 1], xs[i], ys[i]);
        }
        g.drawLine(xs[i - 1], ys[i - 1], xs[0], ys[0]);
    }

    /**
     * _more_
     *
     * @param c _more_
     * @param top _more_
     *
     * @return _more_
     */
    public static Point getScreenLocation(Component c, Component top) {
        Point p = c.getLocation();
        Component parent = c.getParent();
        if (parent == null) {
            return p;
        }
        if (parent == top) {
            return p;
        }
        Point pp = getScreenLocation(parent, top);
        pp.x += p.x;
        pp.y += p.y;
        return pp;
    }

    /**
     * _more_
     *
     * @param g _more_
     * @param borderType _more_
     * @param width _more_
     * @param height _more_
     */
    public static void paintBorder(Graphics g, int borderType, int width, int height) {
        paintBorder(g, borderType, borderInsets, borderColors, width, height);
    }

    /**
     * _more_
     *
     * @param g _more_
     * @param borderType _more_
     * @param insets _more_
     * @param colors _more_
     * @param width _more_
     * @param height _more_
     */
    public static void paintBorder(Graphics g, int borderType, Insets insets, Color[] colors, int width, int height) {
        paintBorder(g, borderType, insets, colors, 0, 0, width, height);
    }

    /**
     * _more_
     *
     * @param g _more_
     * @param borderType _more_
     * @param insets _more_
     * @param colors _more_
     * @param x _more_
     * @param y _more_
     * @param width _more_
     * @param height _more_
     */
    public static void paintBorder(Graphics g, int borderType, Insets insets, Color[] colors, int x, int y, int width, int height) {
        width--;
        height--;
        if (borderType == BORDER_RAISED) {
            g.setColor(Color.darkGray);
            g.drawLine(x, y + height, x + width - 1, y + height);
            g.drawLine(x + width, y + 0, x + width, y + height);
            g.setColor(Color.gray);
            g.drawLine(x + 1, y + height - 1, x + width - 2, y + height - 1);
            g.drawLine(x + width - 1, y + 1, x + width - 1, y + height - 1);
            g.setColor(Color.white);
            g.drawLine(x, y, x, y + height - 1);
            g.drawLine(x, y, x + width - 1, y);
        } else if (borderType == BORDER_SUNKEN) {
            g.setColor(Color.gray);
            g.drawLine(x, y, x + width - 1, y);
            g.drawLine(x, y, x, y + height - 1);
            g.setColor(Color.black);
            g.drawLine(x + 1, y + 1, x + width - 2, y + 1);
            g.drawLine(x + 1, y + 1, x + 1, y + height - 2);
            g.setColor(Color.white);
            g.drawLine(x, y + height, x + width, y + height);
            g.drawLine(width, y, x + width, y + height);
        } else if (borderType == BORDER_MATTE) {
            width += 1;
            height += 1;
            if (insets.top != 0) {
                g.setColor(((colors[0] != null) ? colors[0] : Color.black));
                g.fillRect(x, y, width, insets.top);
            }
            if (insets.bottom != 0) {
                g.setColor(((colors[2] != null) ? colors[2] : Color.black));
                g.fillRect(x, y + height - insets.bottom, width, insets.bottom);
            }
            if (insets.left != 0) {
                g.setColor(((colors[1] != null) ? colors[1] : Color.black));
                g.fillRect(x, y, insets.left, height);
            }
            if (insets.right != 0) {
                g.setColor(((colors[3] != null) ? colors[3] : Color.black));
                g.fillRect(x + width - insets.right, y, insets.right, height);
            }
        }
    }

    /**
     * _more_
     *
     * @param p _more_
     * @param r _more_
     *
     * @return _more_
     */
    public static double distance(Point p, Rectangle r) {
        if (r.contains(p)) {
            return 0.0;
        }
        double tmp = distance(p.x, p.y, r.x, r.y);
        tmp = Math.min(tmp, distance(p.x, p.y, r.x, r.y + r.height));
        tmp = Math.min(tmp, distance(p.x, p.y, r.x + r.width, r.y));
        tmp = Math.min(tmp, distance(p.x, p.y, r.x + r.width, r.y + r.height));
        return tmp;
    }

    /**
     * _more_
     *
     * @param x1 _more_
     * @param y1 _more_
     * @param x2 _more_
     * @param y2 _more_
     *
     * @return _more_
     */
    public static double distance(int x1, int y1, int x2, int y2) {
        return (Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)));
    }

    /**
     * _more_
     *
     * @param p1 _more_
     * @param p2 _more_
     *
     * @return _more_
     */
    public static double pointAngle(Point p1, Point p2) {
        Point b = new Point(p2.x - p1.x, p2.y - p1.y);
        if (b.x == 0) {
            if (b.y >= 0) {
                return toRadian(90.0);
            } else {
                return toRadian(270.0);
            }
        }
        double angle = Math.atan(((double) (b.y)) / b.x);
        if ((b.x < 0) && (b.y <= 0)) {
            return Math.PI + angle;
        } else if (b.x < 0) {
            return Math.PI + angle;
        } else if (b.y < 0) {
            return 2.0 * Math.PI + angle;
        }
        return angle;
    }

    /**
     * _more_
     *
     * @param degree _more_
     *
     * @return _more_
     */
    public static double toRadian(double degree) {
        return degree * 2.0 * Math.PI / 360.0;
    }

    /**
     * _more_
     *
     * @param target _more_
     * @param origin _more_
     * @param theta _more_
     *
     * @return _more_
     */
    public static Point rotatePoint(Point target, Point origin, double theta) {
        Point ret = rotatePoint(new Point(target.x - origin.x, target.y - origin.y), theta);
        ret.x += origin.x;
        ret.y += origin.y;
        return ret;
    }

    /**
     * _more_
     *
     * @param target _more_
     * @param theta _more_
     *
     * @return _more_
     */
    public static Point rotatePoint(Point target, double theta) {
        return (new Point((int) (target.x * Math.cos(theta) - target.y * Math.sin(theta) + 0.5), (int) (target.x * Math.sin(theta) + target.y * Math.cos(theta) + 0.5)));
    }

    /**
     * _more_
     *
     * @param which _more_
     * @param r _more_
     *
     * @return _more_
     */
    public static Point getPointOnRect(int which, Rectangle r) {
        switch(which) {
            case PT_N:
                return new Point(r.x + r.width / 2, r.y);
            case PT_NE:
                return new Point(r.x + r.width, r.y);
            case PT_W:
                return new Point(r.x, r.y + r.height / 2);
            case PT_C:
                return new Point(r.x + r.width / 2, r.y + r.height / 2);
            case PT_E:
                return new Point(r.x + r.width, r.y + r.height / 2);
            case PT_SW:
                return new Point(r.x, r.y + r.height);
            case PT_S:
                return new Point(r.x + r.width / 2, r.y + r.height);
            case PT_SE:
                return new Point(r.x + r.width, r.y + r.height);
            case PT_NW:
            default:
                return new Point(r.x, r.y);
        }
    }

    /**
     * _more_
     *
     * @param g _more_
     * @param t _more_
     * @param x _more_
     * @param y _more_
     * @param where _more_
     */
    public static void drawStringAt(Graphics g, String t, int x, int y, int where) {
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(t);
        int h = fm.getMaxAscent() + fm.getMaxDescent();
        switch(where) {
            case PT_NW:
                g.drawString(t, x, y + h);
                break;
            case PT_NE:
                g.drawString(t, x - w, y + h);
                break;
            case PT_SW:
                g.drawString(t, x, y);
                break;
            case PT_SE:
                g.drawString(t, x - w, y);
                break;
        }
    }

    /**
     * _more_
     *
     * @param g _more_
     * @param t _more_
     * @param x _more_
     * @param y _more_
     * @param width _more_
     */
    public static void drawClippedString(Graphics g, String t, int x, int y, int width) {
        g.drawString(clipString(g, t, width), x, y);
    }

    /**
     * _more_
     *
     * @param g _more_
     * @param t _more_
     * @param width _more_
     *
     * @return _more_
     */
    public static String clipString(Graphics g, String t, int width) {
        if (width <= 0) {
            return "";
        }
        FontMetrics fm = g.getFontMetrics();
        int length = t.length();
        int maxAdvance = fm.getMaxAdvance();
        if (length * maxAdvance < width) {
            return t;
        } else {
            if (fm.stringWidth(t) < width) {
                return t;
            } else {
                int runningWidth = 0;
                int i;
                for (i = 0; (i < length) && (runningWidth < width); i++) {
                    runningWidth += fm.charWidth(t.charAt(i));
                }
                i--;
                return t.substring(0, i);
            }
        }
    }

    /**
     * _more_
     *
     * @param g _more_
     * @param left _more_
     * @param top _more_
     * @param width _more_
     * @param height _more_
     * @param startAngle _more_
     * @param deltaAngle _more_
     * @param lineWidth _more_
     */
    public static void drawArc(Graphics g, int left, int top, int width, int height, int startAngle, int deltaAngle, int lineWidth) {
        left = left - lineWidth / 2;
        top = top - lineWidth / 2;
        width = width + lineWidth;
        height = height + lineWidth;
        for (int i = 0; i < lineWidth; i++) {
            g.drawArc(left, top, width, height, startAngle, deltaAngle);
            if ((i + 1) < lineWidth) {
                g.drawArc(left, top, width - 1, height - 1, startAngle, deltaAngle);
                g.drawArc(left + 1, top, width - 1, height - 1, startAngle, deltaAngle);
                g.drawArc(left, top + 1, width - 1, height - 1, startAngle, deltaAngle);
                g.drawArc(left + 1, top + 1, width - 1, height - 1, startAngle, deltaAngle);
                left = left + 1;
                top = top + 1;
                width = width - 2;
                height = height - 2;
            }
        }
    }

    /**
     * _more_
     *
     * @param g _more_
     * @param x1 _more_
     * @param y1 _more_
     * @param x2 _more_
     * @param y2 _more_
     * @param lineWidth _more_
     */
    public static void drawLine(Graphics g, int x1, int y1, int x2, int y2, int lineWidth) {
        if (lineWidth == 1) {
            g.drawLine(x1, y1, x2, y2);
        } else {
            g.drawLine(x1, y1, x2, y2);
            double halfWidth = ((double) lineWidth) / 2.0;
            double deltaX = (double) (x2 - x1);
            double deltaY = (double) (y2 - y1);
            double angle = ((x1 == x2) ? Math.PI : Math.atan(deltaY / deltaX) + Math.PI / 2);
            int xOffset = (int) (halfWidth * Math.cos(angle));
            int yOffset = (int) (halfWidth * Math.sin(angle));
            int[] xCorners = { x1 - xOffset, x2 - xOffset + 1, x2 + xOffset + 1, x1 + xOffset };
            int[] yCorners = { y1 - yOffset, y2 - yOffset, y2 + yOffset + 1, y1 + yOffset + 1 };
            g.fillPolygon(xCorners, yCorners, 4);
            Color c = g.getColor();
            g.setColor(Color.red);
            int hw = (int) halfWidth;
            int dw = hw * 2;
            g.setColor(c);
        }
    }

    /**
     * _more_
     *
     * @param g _more_
     * @param left _more_
     * @param top _more_
     * @param width _more_
     * @param height _more_
     * @param lineWidth _more_
     */
    public static void drawRect(Graphics g, int left, int top, int width, int height, int lineWidth) {
        left = left - lineWidth / 2;
        top = top - lineWidth / 2;
        width = width + lineWidth;
        height = height + lineWidth;
        for (int i = 0; i < lineWidth; i++) {
            g.drawRect(left, top, width, height);
            left = left + 1;
            top = top + 1;
            width = width - 2;
            height = height - 2;
        }
    }

    /**
     * _more_
     *
     * @param g _more_
     * @param left _more_
     * @param top _more_
     * @param width _more_
     * @param height _more_
     * @param arcWidth _more_
     * @param arcHeight _more_
     * @param lineWidth _more_
     */
    public static void drawRoundRect(Graphics g, int left, int top, int width, int height, int arcWidth, int arcHeight, int lineWidth) {
        left = left - lineWidth / 2;
        top = top - lineWidth / 2;
        width = width + lineWidth;
        height = height + lineWidth;
        for (int i = 0; i < lineWidth; i++) {
            g.drawRoundRect(left, top, width, height, arcWidth, arcHeight);
            if ((i + 1) < lineWidth) {
                g.drawRoundRect(left, top, width - 1, height - 1, arcWidth, arcHeight);
                g.drawRoundRect(left + 1, top, width - 1, height - 1, arcWidth, arcHeight);
                g.drawRoundRect(left, top + 1, width - 1, height - 1, arcWidth, arcHeight);
                g.drawRoundRect(left + 1, top + 1, width - 1, height - 1, arcWidth, arcHeight);
                left = left + 1;
                top = top + 1;
                width = width - 2;
                height = height - 2;
            }
        }
    }

    /** _more_ */
    private static Hashtable images = new Hashtable();

    /**
     * _more_
     *
     * @param gif _more_
     * @param c _more_
     *
     * @return _more_
     */
    public static Image getImageResource(String gif, Class c) {
        byte[] bytes = readResource(gif, c);
        if (bytes != null) {
            return Toolkit.getDefaultToolkit().createImage(bytes);
        }
        return null;
    }

    /**
     * _more_
     *
     * @param resource _more_
     * @param c _more_
     *
     * @return _more_
     */
    public static byte[] readResource(String resource, Class c) {
        return readResource(resource, c, false);
    }

    /**
     * _more_
     *
     * @param resource _more_
     * @param c _more_
     * @param printError _more_
     *
     * @return _more_
     */
    public static byte[] readResource(String resource, Class c, boolean printError) {
        try {
            InputStream inputStream = null;
            System.err.println("read resource:" + resource);
            if (!resource.startsWith("http:")) {
                inputStream = c.getResourceAsStream(resource);
            }
            if (inputStream == null) {
                URL url = new URL(resource);
                System.err.println("read url:" + url);
                URLConnection connection = url.openConnection();
                inputStream = connection.getInputStream();
            }
            byte[] buffer = new byte[100000];
            byte[] bytes = new byte[10];
            int totalBytes = 0;
            while (true) {
                int howMany = inputStream.read(buffer);
                if (howMany < 0) {
                    break;
                }
                while ((howMany + totalBytes) > bytes.length) {
                    byte[] tmp = bytes;
                    bytes = new byte[tmp.length * 2];
                    System.arraycopy(tmp, 0, bytes, 0, totalBytes);
                }
                System.arraycopy(buffer, 0, bytes, totalBytes, howMany);
                totalBytes += howMany;
            }
            byte[] finalBytes = new byte[totalBytes];
            System.arraycopy(bytes, 0, finalBytes, 0, totalBytes);
            return finalBytes;
        } catch (Exception exc) {
            if (printError) {
                System.err.println("Error reading resource:" + resource + " " + exc);
                exc.printStackTrace();
            }
        }
        return null;
    }

    /**
     * _more_
     *
     * @param source _more_
     * @param patterns _more_
     *
     * @return _more_
     */
    public static int indexOf(String source, String[] patterns) {
        int idx = -1;
        for (int i = 0; i < patterns.length; i++) {
            int tmpIdx = source.indexOf(patterns[i]);
            if ((idx == -1) || ((tmpIdx >= 0) && (tmpIdx < idx))) {
                idx = tmpIdx;
            }
        }
        return idx;
    }

    /**
     * _more_
     *
     * @param source _more_
     * @param patterns _more_
     *
     * @return _more_
     */
    public static Vector split(String source, String[] patterns) {
        Vector tokens = new Vector();
        if (source == null) {
            return tokens;
        }
        int idx;
        while ((idx = indexOf(source, patterns)) >= 0) {
            tokens.addElement(source.substring(0, idx));
            source = source.substring(idx + 1);
        }
        tokens.addElement(source);
        return tokens;
    }

    /**
     * _more_
     *
     * @param source _more_
     * @param pattern _more_
     * @param to _more_
     *
     * @return _more_
     */
    public static String replace(String source, String pattern, String to) {
        return replace(source, pattern, to, true);
    }

    /**
     * _more_
     *
     * @param source _more_
     * @param pattern _more_
     * @param to _more_
     * @param caseSensitive _more_
     *
     * @return _more_
     */
    public static String replace(String source, String pattern, String to, boolean caseSensitive) {
        String workSource = (caseSensitive ? source : source.toLowerCase());
        pattern = (caseSensitive ? pattern : pattern.toLowerCase());
        int idx = workSource.indexOf(pattern);
        int length = pattern.length();
        StringBuffer dest = new StringBuffer("");
        while (idx >= 0) {
            dest.append(source.substring(0, idx));
            dest.append(to);
            workSource = workSource.substring(idx + length);
            if (!caseSensitive) {
                source = source.substring(idx + length);
            } else {
                source = workSource;
            }
            idx = workSource.indexOf(pattern);
        }
        dest.append(source);
        return dest.toString();
    }

    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     */
    public static Color getColor(String value) {
        return getColor(value, Color.black);
    }

    /**
     * _more_
     *
     * @param value _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static Color getColor(String value, Color dflt) {
        if (value == null) {
            return dflt;
        }
        value = value.trim();
        if (value.equals("null")) {
            return null;
        }
        String s = value;
        String lookFor = ",";
        int i1 = s.indexOf(lookFor);
        if (i1 < 0) {
            lookFor = " ";
            i1 = s.indexOf(lookFor);
        }
        if (i1 > 0) {
            String red = s.substring(0, i1);
            s = s.substring(i1 + 1).trim();
            int i2 = s.indexOf(lookFor);
            if (i2 > 0) {
                String green = s.substring(0, i2);
                String blue = s.substring(i2 + 1);
                try {
                    return new Color(Integer.decode(red).intValue(), Integer.decode(green).intValue(), Integer.decode(blue).intValue());
                } catch (Exception exc) {
                    System.err.println("Bad color:" + value);
                }
            }
        }
        try {
            return new Color(Integer.decode(s).intValue());
        } catch (Exception e) {
            s = s.toLowerCase();
            if (s.equals("blue")) {
                return Color.blue;
            }
            if (s.equals("black")) {
                return Color.black;
            }
            if (s.equals("red")) {
                return Color.red;
            }
            if (s.equals("gray")) {
                return Color.gray;
            }
            if (s.equals("lightgray")) {
                return Color.lightGray;
            }
            if (s.equals("white")) {
                return Color.white;
            }
            if (s.equals("green")) {
                return Color.green;
            }
            if (s.equals("orange")) {
                return Color.orange;
            }
            return dflt;
        }
    }

    /**
     * _more_
     *
     * @param comp _more_
     */
    public static void relayout(Component comp) {
        Container parent = comp.getParent();
        while (parent != null) {
            if (parent.getParent() == null) {
                parent.invalidate();
                parent.validate();
                return;
            }
            parent = parent.getParent();
        }
    }

    /**
     *  Utility for reading the contents of a url
     *
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String readUrl(String url) throws Exception {
        byte[] bytes = readResource(url, GuiUtils.class);
        if (bytes == null) {
            return null;
        }
        return new String(bytes);
    }

    /**
     * _more_
     *
     * @param component _more_
     *
     * @return _more_
     */
    public static JFrame getFrame(Component component) {
        if (component == null) {
            return null;
        }
        Component parent = component.getParent();
        while (parent != null) {
            if (JFrame.class.isAssignableFrom(parent.getClass())) {
                return (JFrame) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * _more_
     *
     * @param to _more_
     * @param from _more_
     */
    public static void addAll(Vector to, Vector from) {
        for (int i = 0; i < from.size(); i++) {
            to.addElement(from.elementAt(i));
        }
    }

    /**
     * _more_
     *
     * @param initial _more_
     *
     * @return _more_
     */
    public static Vector vector(Object initial) {
        Vector v = new Vector();
        v.addElement(initial);
        return v;
    }

    /**
     * _more_
     *
     * @param a _more_
     * @param values _more_
     * @param s _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static int getIndex(String[] a, int[] values, String s, int dflt) {
        if (s == null) {
            return dflt;
        }
        for (int i = 0; i < a.length; i++) {
            if (s.equals(a[i])) {
                return ((values != null) ? values[i] : i);
            }
        }
        return dflt;
    }
}
