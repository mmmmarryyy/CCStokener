package org.librebiz.pureport.reportfile;

import java.awt.Color;
import java.awt.font.TextAttribute;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.librebiz.pureport.definition.Box;
import org.librebiz.pureport.definition.ImageElement;
import org.librebiz.pureport.definition.StrokeInfo;
import org.librebiz.pureport.definition.TextWrapper;
import org.librebiz.pureport.definition.Band;
import org.librebiz.pureport.definition.Block;
import org.librebiz.pureport.definition.Cell;
import org.librebiz.pureport.definition.Choose;
import org.librebiz.pureport.definition.Column;
import org.librebiz.pureport.definition.Expression;
import org.librebiz.pureport.definition.Iteration;
import org.librebiz.pureport.definition.MacroCall;
import org.librebiz.pureport.definition.MacroDefinition;
import org.librebiz.pureport.definition.PageLayout;
import org.librebiz.pureport.definition.Report;
import org.librebiz.pureport.definition.Row;
import org.librebiz.pureport.definition.Script;
import org.librebiz.pureport.definition.SectionContainer;
import org.librebiz.pureport.definition.TextContainer;
import org.librebiz.pureport.definition.TextString;
import org.librebiz.pureport.definition.TileExpression;
import org.librebiz.pureport.definition.When;
import org.librebiz.pureport.quantity.Quantity;
import org.librebiz.pureport.quantity.Unit;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.DefaultHandler;

public class ReportReader extends DefaultHandler {

    private static final boolean DEBUG = true;

    private static final Map<String, Color> COLOR_MAP = new HashMap<String, Color>();

    private static final Map<String, Integer> IMAGE_ALIGNMENT_MAP = new HashMap<String, Integer>();

    private Unit defaultUnits = Unit.DEFAULT_UNIT;

    private Report report;

    private Band band;

    private int row;

    private int column;

    private Cell cell;

    private TextContainer container;

    private List containerStack = new LinkedList();

    private int imageAlignment;

    private Quantity imageOriginX;

    private Quantity imageOriginY;

    private Quantity imageWidth;

    private Quantity imageHeight;

    private boolean pass2;

    private TileExpression tileExpr;

    private Box box;

    private TextHandler textHandler;

    private SectionContainer sectionContainer;

    private List<SectionContainer> sectionContainerStack = new ArrayList<SectionContainer>();

    private MacroCall call;

    private String argName;

    private String argValue;

    private Choose choose;

    static {
        COLOR_MAP.put("white", Color.WHITE);
        COLOR_MAP.put("light-gray", Color.LIGHT_GRAY);
        COLOR_MAP.put("gray", Color.GRAY);
        COLOR_MAP.put("dark-gray", Color.DARK_GRAY);
        COLOR_MAP.put("black", Color.BLACK);
        COLOR_MAP.put("red", Color.RED);
        COLOR_MAP.put("pink", Color.PINK);
        COLOR_MAP.put("orange", Color.ORANGE);
        COLOR_MAP.put("yellow", Color.YELLOW);
        COLOR_MAP.put("green", Color.GREEN);
        COLOR_MAP.put("magenta", Color.MAGENTA);
        COLOR_MAP.put("cyan", Color.CYAN);
        COLOR_MAP.put("blue", Color.BLUE);
    }

    static {
        IMAGE_ALIGNMENT_MAP.put("top", ImageElement.TOP_ALIGNMENT);
        IMAGE_ALIGNMENT_MAP.put("bottom", ImageElement.BOTTOM_ALIGNMENT);
        IMAGE_ALIGNMENT_MAP.put("hanging", ImageElement.HANGING_BASELINE);
        IMAGE_ALIGNMENT_MAP.put("center", ImageElement.CENTER_BASELINE);
        IMAGE_ALIGNMENT_MAP.put("roman", ImageElement.ROMAN_BASELINE);
    }

    private ReportReader() {
    }

    public static Report load(InputStream in) throws IOException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature("http://xml.org/sax/features/validation", true);
            SAXParser parser = factory.newSAXParser();
            ReportReader handler = new ReportReader();
            parser.parse(new InputSource(in), handler);
            return handler.report;
        } catch (ParserConfigurationException ex) {
            throw new IOException(ex.getMessage());
        } catch (SAXNotSupportedException ex) {
            throw new IOException(ex.getMessage());
        } catch (SAXException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    public static Report load(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            return load(in);
        } finally {
            in.close();
        }
    }

    public static Report load(String fileName) throws IOException {
        return load(new File(fileName));
    }

    public static Report load(URL url) throws IOException {
        InputStream in = url.openStream();
        try {
            return load(in);
        } finally {
            in.close();
        }
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
        if ("-//librebiz//DTD pureport 1.0//EN".equals(publicId) || "report.dtd".equals(systemId)) {
            return new InputSource(getClass().getResourceAsStream("report.dtd"));
        }
        return null;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs) {
        if (DEBUG) {
            dumpStart(qName, attrs);
        }
        if (qName.equals("report")) {
            startReport(attrs);
        } else if (qName.equals("script")) {
            startScript();
        } else if (qName.equals("before")) {
            startBefore();
        } else if (qName.equals("after")) {
            startAfter();
        } else if (qName.equals("condition")) {
            startCondition(attrs);
        } else if (qName.equals("page-layout")) {
            startPageLayout(attrs);
        } else if (qName.equals("report-header")) {
            startBand(attrs);
            report.setReportHeader(band);
        } else if (qName.equals("report-footer")) {
            startBand(attrs);
            report.setReportFooter(band);
        } else if (qName.equals("page-header")) {
            startBand(attrs);
            report.setPageHeader(band);
        } else if (qName.equals("page-footer")) {
            startBand(attrs);
            report.setPageFooter(band);
        } else if (qName.equals("band")) {
            startBand(attrs);
            sectionContainer.addContent(band);
        } else if (qName.equals("iterate")) {
            startIterate(attrs);
        } else if (qName.equals("row")) {
            startRow(attrs);
        } else if (qName.equals("column")) {
            startColumn(attrs);
        } else if (qName.equals("cell")) {
            startCell(attrs);
        } else if (qName.equals("block")) {
            startBlock(attrs);
        } else if (qName.equals("tile")) {
            startTile(attrs);
        } else if (qName.equals("wrapper")) {
            startWrapper(attrs);
        } else if (qName.equals("expression")) {
            startExpression(attrs);
        } else if (qName.equals("image")) {
            startImage(attrs);
        } else if (qName.equals("macro")) {
            startMacroDefinition(attrs);
        } else if (qName.equals("call")) {
            startMacroCall(attrs);
        } else if (qName.equals("argument")) {
            startArgument(attrs);
        } else if (qName.equals("choose")) {
            startChoose(attrs);
        } else if (qName.equals("when")) {
            startWhen(attrs);
        } else if (qName.equals("otherwise")) {
            startOtherwise(attrs);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (DEBUG) {
            dumpEnd(qName);
        }
        if (qName.equals("report")) {
            endReport();
        } else if (qName.equals("script")) {
            endScript();
        } else if (qName.equals("before")) {
            endBefore();
        } else if (qName.equals("after")) {
            endAfter();
        } else if (qName.equals("condition")) {
            endCondition();
        } else if (qName.equals("page-layout")) {
            endPageLayout();
        } else if (qName.equals("report-header")) {
            endBand();
        } else if (qName.equals("report-footer")) {
            endBand();
        } else if (qName.equals("page-header")) {
            endBand();
        } else if (qName.equals("page-footer")) {
            endBand();
        } else if (qName.equals("band")) {
            endBand();
        } else if (qName.equals("iterate")) {
            endIterate();
        } else if (qName.equals("row")) {
            endRow();
        } else if (qName.equals("column")) {
            endColumn();
        } else if (qName.equals("cell")) {
            endCell();
        } else if (qName.equals("block")) {
            endBlock();
        } else if (qName.equals("tile")) {
            endTile();
        } else if (qName.equals("wrapper")) {
            endWrapper();
        } else if (qName.equals("expression")) {
            endExpression();
        } else if (qName.equals("image")) {
            endImage();
        } else if (qName.equals("macro")) {
            endMacroDefinition();
        } else if (qName.equals("call")) {
            endMacroCall();
        } else if (qName.equals("argument")) {
            endArgument();
        } else if (qName.equals("choose")) {
            endChoose();
        } else if (qName.equals("when")) {
            endWhen();
        } else if (qName.equals("otherwise")) {
            endOtherwise();
        }
    }

    private void dumpStart(String element, Attributes attrs) {
        StringBuilder sb = new StringBuilder();
        sb.append('<');
        sb.append(element);
        int n = attrs.getLength();
        for (int i = 0; i < n; ++i) {
            sb.append(' ');
            sb.append(attrs.getLocalName(i));
            sb.append("=\"");
            sb.append(attrs.getValue(i));
            sb.append('"');
        }
        sb.append('>');
        System.out.println(sb.toString());
    }

    private void dumpEnd(String element) {
        System.out.println("<" + element + "/>");
    }

    @Override
    public void characters(char chars[], int start, int length) {
        if (textHandler != null) {
            int end = start + length;
            for (int i = start; i < end; ++i) {
                textHandler.handleChar(chars[i]);
            }
        }
    }

    private Quantity zero() {
        return new Quantity(0, defaultUnits);
    }

    private Quantity toQuantity(String value) {
        return toQuantity(value, null);
    }

    private Quantity toQuantity(String value, Quantity defValue) {
        return value == null ? defValue : Quantity.parse(value, defaultUnits);
    }

    private int toInt(String value, int defValue) {
        return value == null ? defValue : Integer.parseInt(value);
    }

    private Color toColor(String s) {
        if (s == null) {
            return null;
        } else {
            Color col = (Color) COLOR_MAP.get(s.toLowerCase());
            if (col != null) {
                return col;
            } else {
                int rgb = (int) Long.parseLong(s, 16);
                return new Color(rgb, s.length() == 8);
            }
        }
    }

    private StrokeInfo toStrokeInfo(String s) {
        if (s == null) {
            return null;
        } else {
            StringTokenizer tokenizer = new StringTokenizer(s, " \t");
            if (tokenizer.countTokens() != 3) {
                throw new ReportReaderException("Invalid border info " + s);
            }
            StrokeInfo si = new StrokeInfo();
            si.setWidth(toQuantity(tokenizer.nextToken()));
            tokenizer.nextToken();
            si.setColor(toColor(tokenizer.nextToken()));
            return si;
        }
    }

    private void setBoxAttributes(Box box, Attributes attrs) {
        box.setCondition(attrs.getValue("condition"));
        box.setTopMargin(toQuantity(attrs.getValue("top-margin")));
        box.setBottomMargin(toQuantity(attrs.getValue("bottom-margin")));
        box.setLeftMargin(toQuantity(attrs.getValue("left-margin")));
        box.setRightMargin(toQuantity(attrs.getValue("right-margin")));
        box.setTopPadding(toQuantity(attrs.getValue("top-padding")));
        box.setBottomPadding(toQuantity(attrs.getValue("bottom-padding")));
        box.setLeftPadding(toQuantity(attrs.getValue("left-padding")));
        box.setRightPadding(toQuantity(attrs.getValue("right-padding")));
        box.setBackground(toColor(attrs.getValue("background")));
        String s = attrs.getValue("vertical-align");
        if ("bottom".equals(s)) {
            box.setVerticalAlignment(Cell.ALIGN_BOTTOM);
        } else if ("center".equals(s)) {
            box.setVerticalAlignment(Cell.ALIGN_CENTER);
        } else {
            box.setVerticalAlignment(Cell.ALIGN_TOP);
        }
        s = attrs.getValue("align");
        if ("right".equals(s)) {
            box.setAlignment(Block.ALIGN_RIGHT);
        } else if ("center".equals(s)) {
            box.setAlignment(Block.ALIGN_CENTER);
        } else if ("justify".equals(s)) {
            box.setAlignment(Block.ALIGN_JUSTIFY);
        } else {
            box.setAlignment(Block.ALIGN_LEFT);
        }
        StrokeInfo si = toStrokeInfo(attrs.getValue("border"));
        if (si != null) {
            box.setLeftBorder(si);
            box.setRightBorder(si);
            box.setTopBorder(si);
            box.setBottomBorder(si);
        } else {
            box.setLeftBorder(toStrokeInfo(attrs.getValue("border-left")));
            box.setRightBorder(toStrokeInfo(attrs.getValue("border-right")));
            box.setTopBorder(toStrokeInfo(attrs.getValue("border-top")));
            box.setBottomBorder(toStrokeInfo(attrs.getValue("border-bottom")));
        }
        this.box = box;
    }

    private void setTextAttributes(TextWrapper at, Attributes attrs) {
        at.setAttribute(TextAttribute.FAMILY, attrs.getValue("font-family"));
        String s = attrs.getValue("font-style");
        if (s != null) {
            StringTokenizer tokenizer = new StringTokenizer(s, " \t,;");
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                if (token.equals("italic")) {
                    at.setAttribute(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
                }
            }
        }
        Quantity q = toQuantity(attrs.getValue("font-size"));
        if (q != null) {
            float size = (float) q.getValue(Unit.PT);
            at.setAttribute(TextAttribute.SIZE, new Float(size));
        }
        Color col = toColor(attrs.getValue("foreground"));
        if (col != null) {
            at.setAttribute(TextAttribute.FOREGROUND, col);
        }
        col = toColor(attrs.getValue("background"));
        if (col != null) {
            at.setAttribute(TextAttribute.BACKGROUND, col);
        }
    }

    private void startReport(Attributes attrs) {
        report = new Report();
        String s = attrs.getValue("default-units");
        if (s != null) {
            Unit u = Unit.get(s);
            if (u != null) {
                defaultUnits = u;
            }
        }
        sectionContainer = report;
    }

    private void endReport() {
    }

    public void startBefore() {
        new ExpressionTextHandler().start();
    }

    public void endBefore() {
        String s = textHandler.end();
        if (s != null && s.length() > 0) {
            if (band == null) {
                report.setBefore(s);
            } else {
                band.setBefore(s);
            }
        }
    }

    public void startAfter() {
        new ExpressionTextHandler().start();
    }

    public void endAfter() {
        String s = textHandler.end();
        if (s != null && s.length() > 0) {
            if (band == null) {
                report.setAfter(s);
            } else {
                band.setAfter(s);
            }
        }
    }

    private void startCondition(Attributes attrs) {
        new ExpressionTextHandler().start();
    }

    private void endCondition() {
        String s = textHandler.end();
        if (s != null && s.length() > 0) {
            if (box != null) {
                box.setCondition(s);
            }
        }
    }

    private void startPageLayout(Attributes attrs) {
        PageLayout layout = new PageLayout();
        layout.setPaperWidth(toQuantity(attrs.getValue("paper-width"), new Quantity(21, Unit.CM)));
        layout.setPaperHeight(toQuantity(attrs.getValue("paper-height"), new Quantity(29.7, Unit.CM)));
        layout.setTopMargin(toQuantity(attrs.getValue("top-margin")));
        layout.setBottomMargin(toQuantity(attrs.getValue("bottom-margin")));
        layout.setLeftMargin(toQuantity(attrs.getValue("left-margin")));
        layout.setRightMargin(toQuantity(attrs.getValue("right-margin")));
        String orientation = attrs.getValue("orientation");
        if ("portrait".equals(orientation)) {
            layout.setOrientation(PageLayout.PORTRAIT);
        } else if ("landscape".equals(orientation)) {
            layout.setOrientation(PageLayout.LANDSCAPE);
        } else if ("reverse-landscape".equals(orientation)) {
            layout.setOrientation(PageLayout.REVERSE_LANDSCAPE);
        }
        report.setPageLayout(layout);
    }

    private void endPageLayout() {
    }

    private void startBand(Attributes attrs) {
        band = new Band();
        setBoxAttributes(band, attrs);
        row = 0;
        column = 0;
    }

    private void endBand() {
        band = null;
    }

    private void startRow(Attributes attrs) {
        Row r = new Row();
        r.setHeight(toQuantity(attrs.getValue("height")));
        r.setTopMargin(toQuantity(attrs.getValue("top-margin"), zero()));
        r.setBottomMargin(toQuantity(attrs.getValue("bottom-margin"), zero()));
        band.addRow(r);
    }

    private void endRow() {
    }

    private void startColumn(Attributes attrs) {
        Column c = new Column();
        c.setWidth(toQuantity(attrs.getValue("width")));
        c.setLeftMargin(toQuantity(attrs.getValue("left-margin"), zero()));
        c.setRightMargin(toQuantity(attrs.getValue("right-margin"), zero()));
        band.addColumn(c);
    }

    private void endColumn() {
    }

    private void startCell(Attributes attrs) {
        cell = new Cell();
        setBoxAttributes(cell, attrs);
        cell.setRow(row);
        cell.setColumn(column);
        cell.setRowSpan(toInt(attrs.getValue("row-span"), 1));
        cell.setColumnSpan(toInt(attrs.getValue("column-span"), 1));
        band.addCell(cell);
    }

    private void endCell() {
        while (true) {
            ++column;
            if (column >= band.getColumnCount()) {
                column = 0;
                ++row;
            }
            if (band.getCellAt(row, column) == null) {
                break;
            }
        }
        cell = null;
    }

    private void startBlock(Attributes attrs) {
        Block block = new Block();
        container = block.getContent();
        setBoxAttributes(block, attrs);
        setTextAttributes(block.getContent(), attrs);
        cell.addBox(block);
        new ContentTextHandler().start();
    }

    private void endBlock() {
        String s = textHandler.end();
        if (s.length() > 0) {
            container.add(new TextString(s));
        }
        container = null;
    }

    private void startTile(Attributes attrs) {
        tileExpr = new TileExpression();
        setBoxAttributes(tileExpr, attrs);
        tileExpr.setExpression(attrs.getValue("value"));
        cell.addBox(tileExpr);
        new ContentTextHandler().start();
    }

    private void endTile() {
        String s = textHandler.end();
        if (s.length() > 0) {
            tileExpr.setExpression(s);
        }
        tileExpr = null;
    }

    private void startWrapper(Attributes attrs) {
        String s = textHandler.text();
        if (s.length() > 0) {
            container.add(new TextString(s));
        }
        TextWrapper at = new TextWrapper();
        setTextAttributes(at, attrs);
        container.add(at);
        containerStack.add(0, container);
        container = at;
        new ContentTextHandler().start();
    }

    private void endWrapper() {
        String s = textHandler.end();
        if (s.length() > 0) {
            container.add(new TextString(s));
        }
        container = (TextContainer) containerStack.remove(0);
    }

    private void startExpression(Attributes attrs) {
        String s = textHandler.text();
        if (s.length() > 0) {
            container.add(new TextString(s));
        }
        s = attrs.getValue("pass2");
        pass2 = s == null ? false : "true".equalsIgnoreCase(s);
        s = attrs.getValue("value");
        if (s != null && s.length() > 0) {
            container.add(new Expression(s, pass2));
        }
        new ContentTextHandler().start();
    }

    private void endExpression() {
        String s = textHandler.end();
        if (s.length() > 0) {
            container.add(new Expression(s, pass2));
        }
    }

    private void startImage(Attributes attrs) {
        String s = textHandler.text();
        if (s.length() > 0) {
            container.add(new TextString(s));
        }
        s = attrs.getValue("alignment");
        Integer a = (Integer) IMAGE_ALIGNMENT_MAP.get(s);
        imageAlignment = a == null ? ImageElement.TOP_ALIGNMENT : a.intValue();
        imageWidth = toQuantity(attrs.getValue("width"));
        imageHeight = toQuantity(attrs.getValue("height"));
        imageOriginX = toQuantity(attrs.getValue("origin-x"), Quantity.ZERO);
        imageOriginY = toQuantity(attrs.getValue("origin-y"), Quantity.ZERO);
        s = attrs.getValue("value");
        if (s != null && s.length() > 0) {
            container.add(new ImageElement(s, imageAlignment, imageWidth, imageHeight, imageOriginX, imageOriginY));
        }
        new ExpressionTextHandler().start();
    }

    private void endImage() {
        String s = textHandler.end();
        if (s.length() > 0) {
            container.add(new ImageElement(s, imageAlignment, imageWidth, imageHeight, imageOriginX, imageOriginY));
        }
    }

    private void startSectionContainer(SectionContainer sc) {
        sectionContainerStack.add(0, sectionContainer);
        sectionContainer = sc;
    }

    private void endSectionContainer() {
        sectionContainer = sectionContainerStack.remove(0);
    }

    private void startIterate(Attributes attrs) {
        Iteration iteration = new Iteration();
        iteration.setId(attrs.getValue("id"));
        iteration.setIndexId(attrs.getValue("index-id"));
        iteration.setCollection(attrs.getValue("collection"));
        sectionContainer.addContent(iteration);
        startSectionContainer(iteration);
    }

    private void endIterate() {
        endSectionContainer();
    }

    private void startMacroDefinition(Attributes attrs) {
        MacroDefinition macro = new MacroDefinition();
        report.addMacro(attrs.getValue("name"), macro);
        String attrList = attrs.getValue("arguments");
        if (attrList != null) {
            String names[] = attrList.split("[,; ]");
            for (String name : names) {
                macro.addArgument(name.trim());
            }
        }
        startSectionContainer(macro);
    }

    private void endMacroDefinition() {
        endSectionContainer();
    }

    private void startMacroCall(Attributes attrs) {
        call = new MacroCall();
        call.setName(attrs.getValue("name"));
        sectionContainer.addContent(call);
    }

    private void endMacroCall() {
        call = null;
    }

    private void startArgument(Attributes attrs) {
        argName = attrs.getValue("name");
        argValue = attrs.getValue("value");
        new ExpressionTextHandler().start();
    }

    private void endArgument() {
        String s = textHandler.end();
        if (s.length() > 0) {
            call.addArgument(argName, s);
        } else {
            call.addArgument(argName, argValue);
        }
    }

    public void startScript() {
        new ExpressionTextHandler().start();
    }

    public void endScript() {
        String s = textHandler.end();
        if (s != null && s.length() > 0) {
            sectionContainer.addContent(new Script(s));
        }
    }

    private void startChoose(Attributes attrs) {
        choose = new Choose();
        sectionContainer.addContent(choose);
    }

    private void endChoose() {
        choose = null;
    }

    private void startWhen(Attributes attrs) {
        When branch = new When();
        choose.addBranch(branch);
        branch.setCondition(attrs.getValue("test"));
        startSectionContainer(branch);
    }

    private void endWhen() {
        endSectionContainer();
    }

    private void startOtherwise(Attributes attrs) {
        When branch = new When();
        choose.addBranch(branch);
        startSectionContainer(branch);
    }

    private void endOtherwise() {
        endSectionContainer();
    }

    private abstract class TextHandler {

        private TextHandler link;

        private StringBuffer buf = new StringBuffer();

        void start() {
            link = textHandler;
            textHandler = this;
        }

        String text() {
            String result = buf.toString();
            buf.setLength(0);
            return result;
        }

        String end() {
            textHandler = link;
            return buf.toString();
        }

        void putChar(char c) {
            buf.append(c);
        }

        boolean hasChars() {
            return buf.length() > 0;
        }

        void handleChar(char c) {
            putChar(c);
        }
    }

    private class ExpressionTextHandler extends TextHandler {
    }

    private class ContentTextHandler extends TextHandler {

        boolean wasBlank;

        boolean keepBlanks;

        ContentTextHandler() {
        }

        @Override
        String text() {
            if (wasBlank) {
                putChar(' ');
                wasBlank = false;
            }
            keepBlanks = true;
            return super.text();
        }

        @Override
        void handleChar(char c) {
            if (Character.isWhitespace(c)) {
                wasBlank = keepBlanks;
            } else {
                if (wasBlank) {
                    putChar(' ');
                    wasBlank = false;
                }
                keepBlanks = true;
                putChar(c);
            }
        }
    }
}
