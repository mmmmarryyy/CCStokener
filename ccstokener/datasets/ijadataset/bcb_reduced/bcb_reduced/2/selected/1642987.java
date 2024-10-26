package org.bdgp.swing;

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.awt.*;

public class XMLLayout {

    public static class LayoutItem {

        Color background = null;

        Color foreground = null;

        int width = -1;

        int height = -1;

        Font font = null;
    }

    public static class BoxElement extends LayoutItem {

        public static final int VERTICAL = 0;

        public static final int HORIZONTAL = 1;

        protected int orientation;

        Vector components;

        public BoxElement(int orientation) {
            components = new Vector();
            this.orientation = orientation;
        }

        public void addItem(LayoutItem item) {
            components.addElement(item);
        }

        public Vector getItems() {
            return components;
        }

        public int getOrientation() {
            return orientation;
        }
    }

    public static class TabRecord extends LayoutItem {

        String name;

        LayoutItem item;

        Color bgColor;

        boolean selected = false;

        public String getName() {
            return name;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public void setName(String name) {
            this.name = name;
        }

        public LayoutItem getItem() {
            return item;
        }

        public void setItem(LayoutItem item) {
            this.item = item;
        }

        public Color getColor() {
            return bgColor;
        }

        public void setColor(Color bgColor) {
            this.bgColor = bgColor;
        }
    }

    public static class TabElement extends LayoutItem {

        Vector components;

        public TabElement() {
            components = new Vector();
        }

        public void addItem(TabRecord record) {
            components.add(record);
        }

        public Vector getItems() {
            return components;
        }
    }

    public static class ScrollerElement extends LayoutItem {

        public static final int ALWAYS = 0;

        public static final int AS_NEEDED = 1;

        public static final int NEVER = 2;

        protected LayoutItem item;

        protected int horiz = AS_NEEDED;

        protected int vert = NEVER;

        public ScrollerElement(int horiz, int vert) {
            this.horiz = horiz;
            this.vert = vert;
        }

        public void setItem(LayoutItem item) {
            this.item = item;
        }

        public ScrollerElement(LayoutItem item, int horiz, int vert) {
            this.item = item;
            this.horiz = horiz;
            this.vert = vert;
        }
    }

    public static class DividerElement extends LayoutItem {

        public static final int VERTICAL = 0;

        public static final int HORIZONTAL = 1;

        protected int width = 3;

        protected int orientation;

        protected LayoutItem first;

        protected LayoutItem second;

        public DividerElement(LayoutItem first, LayoutItem second, int width, int orientation) {
            this.first = first;
            this.second = second;
            this.width = width;
            this.orientation = orientation;
        }

        public DividerElement(int width, int orientation) {
            this.width = width;
            this.orientation = orientation;
        }

        public void setFirst(LayoutItem first) {
            this.first = first;
        }

        public void setSecond(LayoutItem second) {
            this.second = second;
        }

        public LayoutItem getFirst() {
            return first;
        }

        public LayoutItem getSecond() {
            return second;
        }

        public int getOrientation() {
            return orientation;
        }

        public int getWidth() {
            return width;
        }
    }

    public static class ComponentElement extends LayoutItem {

        protected String id;

        protected Properties props;

        public ComponentElement(String id, Properties props) {
            this.id = id;
            this.props = props;
        }

        public String getID() {
            return id;
        }

        public Properties getProperties() {
            return props;
        }
    }

    public static class PanelElement extends LayoutItem {

        protected LayoutItem north;

        protected LayoutItem south;

        protected LayoutItem east;

        protected LayoutItem west;

        protected LayoutItem center;

        public PanelElement() {
        }

        public PanelElement(LayoutItem north, LayoutItem south, LayoutItem east, LayoutItem west, LayoutItem center) {
            this.north = north;
            this.south = south;
            this.east = east;
            this.west = west;
            this.center = center;
        }
    }

    protected LayoutItem root;

    public XMLLayout(LayoutItem root) {
        this.root = root;
    }

    public LayoutItem getRoot() {
        return root;
    }

    public static XMLLayout getLayout(String layout) throws SAXException, IOException {
        return getLayout(new InputSource(new StringReader(layout)));
    }

    public static XMLLayout getLayout(URL url) throws SAXException, IOException {
        return getLayout(url.openStream());
    }

    public static XMLLayout getLayout(File file) throws SAXException, IOException {
        return getLayout(file.toURL());
    }

    public static XMLLayout getLayout(InputStream stream) throws SAXException, IOException {
        return getLayout(new InputSource(stream));
    }

    public static XMLLayout getLayout(InputSource document) throws SAXException, IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setValidating(false);
        XMLReader xmlReader = null;
        SAXParser saxParser;
        try {
            saxParser = spf.newSAXParser();
        } catch (ParserConfigurationException e) {
            throw new IOException("Couldn't load parser");
        }
        xmlReader = saxParser.getXMLReader();
        LayoutBuilder layoutBuilder = new LayoutBuilder();
        xmlReader.setContentHandler(layoutBuilder);
        xmlReader.parse(document);
        return new XMLLayout(layoutBuilder.getRoot());
    }

    protected class LayoutNode {

        protected String tagName;

        protected Attributes attributes;

        protected LayoutItem item;
    }

    protected static class LayoutBuilder extends DefaultHandler {

        private static String[] componentTags = { "scroller", "panel", "component", "divider", "tabs" };

        private Vector stack;

        private LayoutItem root;

        public boolean isComponentTag(String tag) {
            for (int i = 0; i < componentTags.length; i++) if (componentTags[i].equalsIgnoreCase(tag)) return true;
            return false;
        }

        public LayoutItem getRoot() {
            return root;
        }

        public void startDocument() throws SAXException {
            stack = new Vector();
        }

        public void startElement(String namespaceURI, String localName, String rawName, Attributes atts) throws SAXException {
            LayoutItem item = null;
            localName = rawName;
            if (localName.equalsIgnoreCase("component")) {
                Properties props = new Properties();
                for (int i = 0; i < atts.getLength(); i++) {
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);
                    if (!name.equals("id")) props.put(name, value);
                }
                item = new ComponentElement(atts.getValue("id"), props);
                stack.insertElementAt(item, 0);
            } else if (localName.equalsIgnoreCase("divider")) {
                String widthStr = atts.getValue("dividerSize");
                int width = 5;
                if (widthStr != null) {
                    try {
                        width = Integer.parseInt(widthStr);
                    } catch (Exception e) {
                    }
                }
                int orientation = DividerElement.VERTICAL;
                if (atts.getValue("orientation") != null && atts.getValue("orientation").equalsIgnoreCase("horz")) orientation = DividerElement.HORIZONTAL;
                item = new DividerElement(width, orientation);
                stack.insertElementAt(item, 0);
            } else if (localName.equalsIgnoreCase("panel")) {
                item = new PanelElement();
                stack.insertElementAt(item, 0);
            } else if (localName.equalsIgnoreCase("tabs")) {
                item = new TabElement();
                stack.insertElementAt(item, 0);
            } else if (localName.equalsIgnoreCase("tab")) {
                TabElement pitem = (TabElement) stack.elementAt(0);
                TabRecord record = new TabRecord();
                String colorStr = atts.getValue("color");
                Color color = null;
                if (colorStr != null) {
                    color = Color.getColor(colorStr);
                    if (color == null) {
                        try {
                            color = Color.decode(colorStr);
                        } catch (Exception e) {
                        }
                    }
                }
                record.setSelected(atts.getValue("selected") != null && atts.getValue("selected").equals("true"));
                record.setColor(color);
                record.setName(atts.getValue("name"));
                pitem.addItem(record);
            } else if (localName.equalsIgnoreCase("box")) {
                int orientation = 0;
                if (atts.getValue("orientation") != null && atts.getValue("orientation").equalsIgnoreCase("horz")) orientation = 1;
                item = new BoxElement(orientation);
                stack.insertElementAt(item, 0);
            } else if (localName.equalsIgnoreCase("scroller")) {
                item = new ScrollerElement(convertScrollString(atts.getValue("horz")), convertScrollString(atts.getValue("vert")));
                stack.insertElementAt(item, 0);
            }
            if (isComponentTag(localName)) {
                configureComponent(item, atts);
            }
            if (root == null && item != null) root = item;
        }

        private void configureComponent(LayoutItem item, Attributes atts) {
            String fontName = atts.getValue("font");
            String foregroundStr = atts.getValue("foreground");
            String backgroundStr = atts.getValue("background");
            int height = -1;
            int width = -1;
            try {
                height = Integer.parseInt(atts.getValue("height"));
            } catch (Exception e) {
            }
            try {
                width = Integer.parseInt(atts.getValue("width"));
            } catch (Exception e) {
            }
            Font font = null;
            if (fontName != null) {
                font = Font.getFont(fontName);
                if (font == null) font = Font.decode(fontName);
            }
            Color foreground = null;
            if (foregroundStr != null) {
                foreground = Color.getColor(foregroundStr);
                if (foreground == null) {
                    try {
                        foreground = Color.decode(foregroundStr);
                    } catch (Exception e) {
                    }
                }
            }
            Color background = null;
            if (backgroundStr != null) {
                background = Color.getColor(backgroundStr);
                if (background == null) {
                    try {
                        background = Color.decode(backgroundStr);
                    } catch (Exception e) {
                    }
                }
            }
            item.foreground = foreground;
            item.background = background;
            item.font = font;
            item.width = width;
            item.height = height;
        }

        private static int convertScrollString(String in) {
            if (in == null) return ScrollerElement.AS_NEEDED;
            if (in.equalsIgnoreCase("ALWAYS")) return ScrollerElement.ALWAYS; else if (in.equalsIgnoreCase("NEVER")) return ScrollerElement.NEVER; else return ScrollerElement.AS_NEEDED;
        }

        public void endElement(java.lang.String uri, java.lang.String localName, java.lang.String qName) {
            localName = qName;
            if (localName.equalsIgnoreCase("first")) {
                LayoutItem item = (LayoutItem) stack.elementAt(0);
                LayoutItem parent = (LayoutItem) stack.elementAt(1);
                ((DividerElement) parent).setFirst(item);
                stack.removeElementAt(0);
            } else if (localName.equalsIgnoreCase("second")) {
                LayoutItem item = (LayoutItem) stack.elementAt(0);
                LayoutItem parent = (LayoutItem) stack.elementAt(1);
                ((DividerElement) parent).setSecond(item);
                stack.removeElementAt(0);
            } else if (localName.equalsIgnoreCase("tab")) {
                LayoutItem item = (LayoutItem) stack.elementAt(0);
                LayoutItem parent = (LayoutItem) stack.elementAt(1);
                TabRecord record = (TabRecord) ((TabElement) parent).getItems().get(((TabElement) parent).getItems().size() - 1);
                record.setItem(item);
                stack.removeElementAt(0);
            } else if (localName.equalsIgnoreCase("north")) {
                LayoutItem item = (LayoutItem) stack.elementAt(0);
                LayoutItem parent = (LayoutItem) stack.elementAt(1);
                ((PanelElement) parent).north = item;
                stack.removeElementAt(0);
            } else if (localName.equalsIgnoreCase("south")) {
                LayoutItem item = (LayoutItem) stack.elementAt(0);
                LayoutItem parent = (LayoutItem) stack.elementAt(1);
                ((PanelElement) parent).south = item;
                stack.removeElementAt(0);
            } else if (localName.equalsIgnoreCase("east")) {
                LayoutItem item = (LayoutItem) stack.elementAt(0);
                LayoutItem parent = (LayoutItem) stack.elementAt(1);
                ((PanelElement) parent).east = item;
                stack.removeElementAt(0);
            } else if (localName.equalsIgnoreCase("west")) {
                LayoutItem item = (LayoutItem) stack.elementAt(0);
                LayoutItem parent = (LayoutItem) stack.elementAt(1);
                ((PanelElement) parent).west = item;
                stack.removeElementAt(0);
            } else if (localName.equalsIgnoreCase("center")) {
                LayoutItem item = (LayoutItem) stack.elementAt(0);
                LayoutItem parent = (LayoutItem) stack.elementAt(1);
                ((PanelElement) parent).center = item;
                stack.removeElementAt(0);
            } else {
                if (stack.size() >= 2) {
                    LayoutItem item = (LayoutItem) stack.elementAt(0);
                    LayoutItem parent = (LayoutItem) stack.elementAt(1);
                    if (parent instanceof ScrollerElement) {
                        ((ScrollerElement) parent).setItem(item);
                        stack.removeElementAt(0);
                    } else if (parent instanceof BoxElement) {
                        ((BoxElement) parent).addItem(item);
                        stack.removeElementAt(0);
                    }
                }
            }
        }

        public void endDocument() throws SAXException {
        }
    }
}
