package org.uguess.birt.report.engine.emitter.ppt;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import org.apache.poi.ddf.EscherComplexProperty;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.ddf.EscherProperty;
import org.apache.poi.hslf.model.AutoShape;
import org.apache.poi.hslf.model.Hyperlink;
import org.apache.poi.hslf.model.Line;
import org.apache.poi.hslf.model.Picture;
import org.apache.poi.hslf.model.Shape;
import org.apache.poi.hslf.model.ShapeTypes;
import org.apache.poi.hslf.model.SimpleShape;
import org.apache.poi.hslf.model.Slide;
import org.apache.poi.hslf.model.TextBox;
import org.apache.poi.hslf.model.TextShape;
import org.apache.poi.hslf.usermodel.RichTextRun;
import org.apache.poi.hslf.usermodel.SlideShow;
import org.eclipse.birt.report.engine.api.IHTMLActionHandler;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.birt.report.engine.api.impl.Action;
import org.eclipse.birt.report.engine.content.IAutoTextContent;
import org.eclipse.birt.report.engine.content.IHyperlinkAction;
import org.eclipse.birt.report.engine.content.IReportContent;
import org.eclipse.birt.report.engine.css.engine.value.css.CSSConstants;
import org.eclipse.birt.report.engine.emitter.IEmitterServices;
import org.eclipse.birt.report.engine.nLayout.area.IArea;
import org.eclipse.birt.report.engine.nLayout.area.IAreaVisitor;
import org.eclipse.birt.report.engine.nLayout.area.IContainerArea;
import org.eclipse.birt.report.engine.nLayout.area.IImageArea;
import org.eclipse.birt.report.engine.nLayout.area.ITemplateArea;
import org.eclipse.birt.report.engine.nLayout.area.ITextArea;
import org.eclipse.birt.report.engine.nLayout.area.impl.PageArea;
import org.eclipse.birt.report.model.api.IResourceLocator;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.uguess.birt.report.engine.layout.wrapper.Frame;
import org.uguess.birt.report.engine.layout.wrapper.Style;
import org.uguess.birt.report.engine.layout.wrapper.impl.AreaFrame;
import org.uguess.birt.report.engine.util.EngineUtil;
import org.uguess.birt.report.engine.util.ImageUtil;
import org.uguess.birt.report.engine.util.OptionParser;
import org.uguess.birt.report.engine.util.PageRangeChecker;

/**
 * PptRenderer
 */
public class PptRenderer implements IAreaVisitor {

    public static final String DEFAULT_FILE_NAME = "report.ppt";

    private static final String DEFAULT_ALTER_TEXT = "<<Unsupported Image>>";

    private static final String PPT_IDENTIFIER = "ppt";

    private static final boolean DEBUG;

    static {
        DEBUG = System.getProperty("debug") != null;
    }

    private boolean exportBodyOnly = false;

    private boolean mimicBorder = true;

    private boolean suppressUnknownImage = false;

    private int currentPageIndex;

    private PageRangeChecker rangeChecker;

    private boolean closeStream;

    private OutputStream output = null;

    private SlideShow slideShow;

    private Stack<Frame> frameStack;

    private Set<TextBox> totalShapes;

    private Set<IArea> totalPageAreas;

    private long timeCounter;

    private ReportDesignHandle reportDesign;

    private IEmitterServices services;

    public String getOutputFormat() {
        return PPT_IDENTIFIER;
    }

    public void initialize(IEmitterServices services) {
        rangeChecker = new PageRangeChecker(null);
        exportBodyOnly = false;
        mimicBorder = true;
        suppressUnknownImage = false;
        closeStream = false;
        this.services = services;
        Object option = services.getEmitterConfig().get(PPT_IDENTIFIER);
        if (option instanceof Map) {
            parseRendererOptions((Map) option);
        } else if (option instanceof IRenderOption) {
            parseRendererOptions(((IRenderOption) option).getOptions());
        }
        parseRendererOptions(services.getRenderOption().getOptions());
        IReportRunnable reportRunnable = services.getReportRunnable();
        if (reportRunnable != null) {
            reportDesign = (ReportDesignHandle) reportRunnable.getDesignHandle();
        }
        Object fd = services.getOption(IRenderOption.OUTPUT_FILE_NAME);
        File file = null;
        try {
            if (fd != null) {
                file = new File(fd.toString());
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                output = new FileOutputStream(file);
            }
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }
        if (output == null) {
            Object value = services.getOption(IRenderOption.OUTPUT_STREAM);
            if (value != null && value instanceof OutputStream) {
                output = (OutputStream) value;
            } else {
                try {
                    file = new File(DEFAULT_FILE_NAME);
                    output = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void parseRendererOptions(Map pptOption) {
        if (pptOption == null) {
            return;
        }
        Object value;
        value = pptOption.get(PptEmitterConfig.KEY_PAGE_RANGE);
        if (value != null) {
            rangeChecker = new PageRangeChecker(OptionParser.parseString(value));
        }
        value = pptOption.get(PptEmitterConfig.KEY_EXPORT_BODY_ONLY);
        if (value != null) {
            exportBodyOnly = OptionParser.parseBoolean(value);
        }
        value = pptOption.get(PptEmitterConfig.KEY_MIMIC_BORDER);
        if (value != null) {
            mimicBorder = OptionParser.parseBoolean(value);
        }
        value = pptOption.get(PptEmitterConfig.KEY_SUPPRESS_UNKNOWN_IMAGE);
        if (value != null) {
            suppressUnknownImage = OptionParser.parseBoolean(value);
        }
        value = pptOption.get(IRenderOption.CLOSE_OUTPUTSTREAM_ON_EXIT);
        if (value != null) {
            closeStream = OptionParser.parseBoolean(value);
        }
    }

    public void start(IReportContent rc) {
        if (DEBUG) {
            timeCounter = System.currentTimeMillis();
        }
        this.frameStack = new Stack<Frame>();
        currentPageIndex = 1;
        slideShow = new SlideShow();
    }

    public void end(IReportContent rc) {
        try {
            slideShow.write(output);
            if (closeStream) {
                output.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        slideShow = null;
        frameStack.clear();
        frameStack = null;
        if (DEBUG) {
            System.out.println("------------total exporting time using: " + (System.currentTimeMillis() - timeCounter) + " ms");
        }
    }

    public void startContainer(IContainerArea container) {
        buildFrame(container, true);
    }

    public void endContainer(IContainerArea containerArea) {
        Frame currentFrame = frameStack.pop();
        if (currentFrame.getData() instanceof PageArea) {
            long span = System.currentTimeMillis();
            if (rangeChecker.checkRange(currentPageIndex)) {
                if (exportBodyOnly) {
                    PageArea pa = (PageArea) currentFrame.getData();
                    IContainerArea body = pa.getBody();
                    Frame bodyFrame = locateChildFrame(currentFrame, body);
                    if (bodyFrame instanceof AreaFrame) {
                        ((AreaFrame) bodyFrame).setXOffset(-bodyFrame.getLeft());
                        ((AreaFrame) bodyFrame).setYOffset(-bodyFrame.getTop());
                    }
                    exportSlide(bodyFrame);
                } else {
                    exportSlide(currentFrame);
                }
                if (DEBUG) {
                    System.out.println("------------export slide[" + (currentPageIndex) + "] using " + (System.currentTimeMillis() - span) + " ms");
                }
            }
            currentPageIndex++;
        }
    }

    /**
	 * Find child frame by specific data object(use "==" to compare)
	 * 
	 * @param parentFrame
	 * @param data
	 * @return
	 */
    private Frame locateChildFrame(Frame parentFrame, Object data) {
        for (Iterator<Frame> itr = parentFrame.iterator(); itr.hasNext(); ) {
            Frame fr = itr.next();
            if (fr.getData() == data) {
                return fr;
            }
            Frame cfr = locateChildFrame(fr, data);
            if (cfr != null) {
                return cfr;
            }
        }
        return null;
    }

    private void exportSlide(Frame pageFrame) {
        Rectangle rct = getAnchor(pageFrame);
        slideShow.setPageSize(new Dimension(rct.width, rct.height));
        Slide slide = slideShow.createSlide();
        exportShape(pageFrame, slide);
    }

    private void exportShape(Frame shapeFrame, Slide slide) {
        Object area = shapeFrame.getData();
        if (area instanceof IImageArea) {
            int idx = loadPicture((IImageArea) area);
            if (idx != -1) {
                Picture pic = new Picture(idx);
                pic.setAnchor(getAnchor(shapeFrame));
                applyStyle(slide, shapeFrame, pic, shapeFrame.getStyle());
                handleHyperLink((IImageArea) area, pic);
                slide.addShape(pic);
            } else if (!suppressUnknownImage) {
                TextBox text = new TextBox();
                text.setText(DEFAULT_ALTER_TEXT);
                text.setAnchor(getAnchor(shapeFrame));
                text.setVerticalAlignment(TextBox.AnchorMiddleCentered);
                applyStyle(slide, shapeFrame, text, shapeFrame.getStyle());
                slide.addShape(text);
            }
        } else if (area instanceof ITextArea) {
            TextBox text = new TextBox();
            text.setText(((ITextArea) area).getText());
            text.setAnchor(getAnchor(shapeFrame));
            applyStyle(slide, shapeFrame, text, shapeFrame.getStyle());
            handleHyperLink((ITextArea) area, text);
            slide.addShape(text);
            if (isTotalPageArea(area)) {
                if (totalShapes == null) {
                    totalShapes = new HashSet<TextBox>();
                }
                totalShapes.add(text);
            }
        } else {
            Style style = shapeFrame.getStyle();
            AutoShape autoShape = new AutoShape(ShapeTypes.Rectangle);
            if (applyStyle(slide, shapeFrame, autoShape, style)) {
                autoShape.setAnchor(getAnchor(shapeFrame));
                slide.addShape(autoShape);
            }
        }
        for (Iterator<Frame> itr = shapeFrame.iterator(); itr.hasNext(); ) {
            Frame childFrame = itr.next();
            exportShape(childFrame, slide);
        }
    }

    private boolean handleHyperLink(IArea area, SimpleShape shape) {
        IHyperlinkAction hlAction = area.getAction();
        if (hlAction != null) {
            try {
                IReportRunnable runnable = services.getReportRunnable();
                String systemId = runnable == null ? null : runnable.getReportName();
                Action act = new Action(systemId, hlAction);
                String link = null;
                String tooltip = EngineUtil.getActionTooltip(hlAction);
                Object ac = services.getOption(RenderOption.ACTION_HANDLER);
                if (ac instanceof IHTMLActionHandler) {
                    link = ((IHTMLActionHandler) ac).getURL(act, services.getReportContext());
                } else {
                    link = hlAction.getHyperlink();
                }
                if (link != null) {
                    Hyperlink hslflink = new Hyperlink();
                    hslflink.setType(Hyperlink.LINK_URL);
                    hslflink.setAddress(link);
                    hslflink.setTitle(tooltip == null ? link : tooltip);
                    if (area instanceof ITextArea && shape instanceof TextShape) {
                        String txt = ((ITextArea) area).getText();
                        if (txt != null) {
                            int linkId = slideShow.addHyperlink(hslflink);
                            ((TextShape) shape).setHyperlink(linkId, 0, txt.length());
                        }
                    } else {
                        slideShow.addHyperlink(hslflink);
                        shape.setHyperlink(hslflink);
                    }
                    return true;
                }
            } catch (Exception e) {
                System.out.println("create hyperlink failed.");
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean applyStyle(Slide slide, Frame shapeFrame, Shape shape, Style style) {
        if (style == null || style.isEmpty()) {
            return false;
        }
        boolean applied = false;
        if (shape instanceof TextBox) {
            String fontName = style.getFontFamily();
            short fontSize = (short) (style.getFontSize() / 1000d);
            Color color = style.getColor();
            boolean underline = style.isTextUnderline();
            boolean strikethrough = style.isTextLineThrough();
            boolean bold = CSSConstants.CSS_BOLD_VALUE.equals(style.getFontWeight());
            boolean italic = (CSSConstants.CSS_OBLIQUE_VALUE.equals(style.getFontStyle()) || CSSConstants.CSS_ITALIC_VALUE.equals(style.getFontStyle()));
            TextBox text = (TextBox) shape;
            text.setMarginLeft(0);
            text.setMarginRight(0);
            text.setMarginBottom(0);
            text.setMarginTop(0);
            text.setWordWrap(TextBox.WrapNone);
            RichTextRun[] rtra = text.getTextRun().getRichTextRuns();
            if (fontName != null) {
                for (int i = 0; i < rtra.length; i++) {
                    rtra[i].setFontName(fontName);
                }
            }
            if (fontSize > 0) {
                for (int i = 0; i < rtra.length; i++) {
                    rtra[i].setFontSize(fontSize);
                }
            }
            if (color != null) {
                for (int i = 0; i < rtra.length; i++) {
                    rtra[i].setFontColor(color);
                }
            }
            if (underline) {
                for (int i = 0; i < rtra.length; i++) {
                    rtra[i].setUnderlined(underline);
                }
            }
            if (strikethrough) {
                for (int i = 0; i < rtra.length; i++) {
                    rtra[i].setStrikethrough(strikethrough);
                }
            }
            if (italic) {
                for (int i = 0; i < rtra.length; i++) {
                    rtra[i].setItalic(italic);
                }
            }
            if (bold) {
                for (int i = 0; i < rtra.length; i++) {
                    rtra[i].setBold(bold);
                }
            }
            int align = getTextAlign(style);
            if (align != -1) {
                text.setVerticalAlignment(align);
            }
            if (!applyBackgroundImage(text, style)) {
                color = style.getBackgroundColor();
                if (color != null) {
                    text.setFillColor(color);
                }
            }
            applied = true;
        } else if (shape instanceof AutoShape) {
            AutoShape auto = (AutoShape) shape;
            if (!applyBackgroundImage(auto, style)) {
                Color color = style.getBackgroundColor();
                if (color != null) {
                    auto.setFillColor(color);
                    applied = true;
                } else {
                    resetFill(auto);
                }
            } else {
                applied = true;
            }
        }
        if (shape instanceof SimpleShape) {
            SimpleShape simple = (SimpleShape) shape;
            if (applyBorder(slide, shapeFrame, simple, style)) {
                applied = true;
            } else {
                resetBorder(simple);
            }
        }
        return applied;
    }

    private void resetBorder(Shape shape) {
        EscherOptRecord opt = (EscherOptRecord) Shape.getEscherChild(shape.getSpContainer(), EscherOptRecord.RECORD_ID);
        Shape.setEscherProperty(opt, EscherProperties.LINESTYLE__NOLINEDRAWDASH, 0x00080000);
    }

    private void resetFill(Shape shape) {
        EscherOptRecord opt = (EscherOptRecord) Shape.getEscherChild(shape.getSpContainer(), EscherOptRecord.RECORD_ID);
        Shape.setEscherProperty(opt, EscherProperties.FILL__NOFILLHITTEST, 0x00100000);
    }

    private boolean applyBackgroundImage(Shape shape, Style style) {
        String imageURI = style.getBackgroundImage();
        if (imageURI == null) {
            return false;
        }
        if (reportDesign != null) {
            URL url = reportDesign.findResource(imageURI, IResourceLocator.IMAGE);
            if (url != null) {
                imageURI = url.toExternalForm();
            }
        }
        if (imageURI == null || imageURI.length() == 0) {
            return false;
        }
        byte[] imageData = loadPictureData(imageURI);
        int picID = -1;
        if (imageData != null) {
            int type = getPictureType(imageData);
            if (type != -1) {
                try {
                    picID = slideShow.addPicture(imageData, type);
                } catch (IOException e) {
                }
            }
        }
        if (picID < 1) {
            return false;
        }
        String repeat = style.getBackgroundRepeat();
        if (repeat == null) {
            repeat = "repeat";
        }
        EscherOptRecord opt = (EscherOptRecord) Shape.getEscherChild(shape.getSpContainer(), EscherOptRecord.RECORD_ID);
        Shape.setEscherProperty(opt, EscherProperties.FILL__NOFILLHITTEST, 0x00100010);
        Shape.setEscherProperty(opt, EscherProperties.FILL__FILLTYPE, "no-repeat".equals(repeat) ? 3 : 2);
        Shape.setEscherProperty(opt, EscherProperties.BLIP__PICTUREACTIVE, 0x00060000);
        Shape.setEscherProperty(opt, (short) (EscherProperties.FILL__PATTERNTEXTURE + 0x4000), picID);
        setComplexProperty(opt, new EscherComplexProperty((short) (EscherProperties.BLIP__BLIPFILENAME + 0x4000 + 0x8000), new byte[] { 0, 0 }));
        return true;
    }

    private void setComplexProperty(EscherOptRecord opt, EscherComplexProperty cprop) {
        List props = opt.getEscherProperties();
        short propID = cprop.getId();
        for (Iterator iterator = props.iterator(); iterator.hasNext(); ) {
            EscherProperty prop = (EscherProperty) iterator.next();
            if (prop.getId() == propID) {
                iterator.remove();
            }
        }
        opt.addEscherProperty(cprop);
        opt.sortProperties();
    }

    private boolean applyBorder(Slide slide, Frame shapeFrame, SimpleShape shape, Style style) {
        boolean needMimic = false;
        String leftBorderStyle = style.getLeftBorderStyle();
        String rightBorderStyle = style.getRightBorderStyle();
        String topBorderStyle = style.getTopBorderStyle();
        String bottomBorderStyle = style.getBottomBorderStyle();
        boolean hasLeftBorder = leftBorderStyle != null && !"none".equalsIgnoreCase(leftBorderStyle);
        boolean hasRightBorder = rightBorderStyle != null && !"none".equalsIgnoreCase(rightBorderStyle);
        boolean hasTopBorder = topBorderStyle != null && !"none".equalsIgnoreCase(topBorderStyle);
        boolean hasBottomBorder = bottomBorderStyle != null && !"none".equalsIgnoreCase(bottomBorderStyle);
        if (!hasLeftBorder && !hasRightBorder && !hasTopBorder && !hasBottomBorder) {
            return false;
        }
        if (mimicBorder && !needMimic && !(hasLeftBorder && hasRightBorder && hasTopBorder && hasBottomBorder)) {
            needMimic = true;
        }
        double lineLeftWidth = hasLeftBorder ? scaleD(style.getLeftBorderWidth()) : 0;
        double lineRightWidth = hasRightBorder ? scaleD(style.getRightBorderWidth()) : 0;
        double lineTopWidth = hasTopBorder ? scaleD(style.getTopBorderWidth()) : 0;
        double lineBottomWidth = hasBottomBorder ? scaleD(style.getBottomBorderWidth()) : 0;
        if (lineLeftWidth <= 0 && lineRightWidth <= 0 && lineTopWidth <= 0 && lineBottomWidth <= 0) {
            return false;
        }
        if (mimicBorder && !needMimic) {
            double avgWidth = (lineLeftWidth + lineRightWidth + lineTopWidth + lineBottomWidth) / 4;
            if (lineLeftWidth != avgWidth || lineRightWidth != avgWidth || lineTopWidth != avgWidth || lineBottomWidth != avgWidth) {
                needMimic = true;
            }
        }
        int lineLeftPen = hasLeftBorder ? getHslfLinePen(leftBorderStyle) : -1;
        int lineRightPen = hasRightBorder ? getHslfLinePen(rightBorderStyle) : -1;
        int lineTopPen = hasTopBorder ? getHslfLinePen(topBorderStyle) : -1;
        int lineBottomPen = hasBottomBorder ? getHslfLinePen(bottomBorderStyle) : -1;
        if (lineLeftPen == -1 && lineRightPen == -1 && lineTopPen == -1 && lineBottomPen == -1) {
            return false;
        }
        if (mimicBorder && !needMimic) {
            int avgStyle = (lineLeftPen + lineRightPen + lineTopPen + lineBottomPen) / 4;
            if (lineLeftPen != avgStyle || lineRightPen != avgStyle || lineTopPen != avgStyle || lineBottomPen != avgStyle) {
                needMimic = true;
            }
        }
        Color lineLeftColor = hasLeftBorder ? style.getLeftBorderColor() : null;
        Color lineRightColor = hasRightBorder ? style.getRightBorderColor() : null;
        Color lineTopColor = hasTopBorder ? style.getTopBorderColor() : null;
        Color lineBottomColor = hasBottomBorder ? style.getBottomBorderColor() : null;
        if (lineLeftColor == null && lineRightColor == null && lineTopColor == null && lineBottomColor == null) {
            return false;
        }
        if (mimicBorder && !needMimic) {
            int avgColor = ((lineLeftColor != null ? lineLeftColor.getRGB() : -1) + (lineRightColor != null ? lineRightColor.getRGB() : -1) + (lineTopColor != null ? lineTopColor.getRGB() : -1) + (lineBottomColor != null ? lineBottomColor.getRGB() : -1)) / 4;
            if ((lineLeftColor != null ? lineLeftColor.getRGB() : -1) != avgColor || (lineRightColor != null ? lineRightColor.getRGB() : -1) != avgColor || (lineTopColor != null ? lineTopColor.getRGB() : -1) != avgColor || (lineBottomColor != null ? lineBottomColor.getRGB() : -1) != avgColor) {
                needMimic = true;
            }
        }
        int lineLeftStyle = getHslfLineStyle(leftBorderStyle);
        int lineRightStyle = getHslfLineStyle(rightBorderStyle);
        int lineTopStyle = getHslfLineStyle(topBorderStyle);
        int lineBottomStyle = getHslfLineStyle(bottomBorderStyle);
        if (needMimic) {
            resetBorder(shape);
            Rectangle rct = getAnchor(shapeFrame);
            if (hasLeftBorder && lineLeftWidth > 0 && lineLeftPen != -1 && lineLeftColor != null) {
                AutoShape autoShape = new AutoShape(ShapeTypes.Rectangle);
                autoShape.setLineColor(lineLeftColor);
                autoShape.setLineStyle(lineLeftStyle);
                autoShape.setLineDashing(lineLeftPen);
                autoShape.setLineWidth(lineLeftWidth);
                autoShape.setAnchor(new Rectangle(rct.x, rct.y, 0, rct.height));
                slide.addShape(autoShape);
                resetFill(autoShape);
            }
            if (hasRightBorder && lineRightWidth > 0 && lineRightPen != -1 && lineRightColor != null) {
                AutoShape autoShape = new AutoShape(ShapeTypes.Rectangle);
                autoShape.setLineColor(lineRightColor);
                autoShape.setLineStyle(lineRightStyle);
                autoShape.setLineDashing(lineRightPen);
                autoShape.setLineWidth(lineRightWidth);
                autoShape.setAnchor(new Rectangle(rct.x + rct.width, rct.y, 0, rct.height));
                slide.addShape(autoShape);
                resetFill(autoShape);
            }
            if (hasTopBorder && lineTopWidth > 0 && lineTopPen != -1 && lineTopColor != null) {
                AutoShape autoShape = new AutoShape(ShapeTypes.Rectangle);
                autoShape.setLineColor(lineTopColor);
                autoShape.setLineStyle(lineTopStyle);
                autoShape.setLineDashing(lineTopPen);
                autoShape.setLineWidth(lineTopWidth);
                autoShape.setAnchor(new Rectangle(rct.x, rct.y, rct.width, 0));
                slide.addShape(autoShape);
                resetFill(autoShape);
            }
            if (hasBottomBorder && lineBottomWidth > 0 && lineBottomPen != -1 && lineBottomColor != null) {
                AutoShape autoShape = new AutoShape(ShapeTypes.Rectangle);
                autoShape.setLineColor(lineBottomColor);
                autoShape.setLineStyle(lineBottomStyle);
                autoShape.setLineDashing(lineBottomPen);
                autoShape.setLineWidth(lineBottomWidth);
                autoShape.setAnchor(new Rectangle(rct.x, rct.y + rct.height, rct.width, 0));
                slide.addShape(autoShape);
                resetFill(autoShape);
            }
        } else {
            double lineWidth = Math.max(Math.max(lineLeftWidth, lineRightWidth), Math.max(lineTopWidth, lineBottomWidth));
            int linePen = Math.max(Math.max(lineLeftPen, lineRightPen), Math.max(lineTopPen, lineBottomPen));
            int lineStyle = Math.max(Math.max(lineLeftStyle, lineRightStyle), Math.max(lineTopStyle, lineBottomStyle));
            shape.setLineStyle(lineStyle);
            shape.setLineDashing(linePen);
            shape.setLineWidth(lineWidth);
            Color lineColor = lineLeftColor != null ? lineLeftColor : (lineRightColor != null ? lineRightColor : (lineTopColor != null ? lineTopColor : lineBottomColor));
            if (lineColor != null) {
                shape.setLineColor(lineColor);
            }
        }
        return true;
    }

    private int getTextAlign(Style style) {
        int align = -1;
        String textAlign = style.getTextAlign();
        String verticalAlign = style.getVerticalAlign();
        if ("top".equals(verticalAlign)) {
            align = TextBox.AnchorTop;
        } else if ("center".equals(verticalAlign) || "middle".equals(verticalAlign)) {
            align = TextBox.AnchorMiddle;
        } else if ("bottom".equals(verticalAlign)) {
            align = TextBox.AnchorBottom;
        }
        if ("center".equals(textAlign) || "middle".equals(textAlign)) {
            if (align == -1) {
                align = 9;
            } else {
                align += 3;
            }
        }
        return align;
    }

    private int getHslfLineStyle(String style) {
        if ("double".equals(style)) {
            return Line.LINE_DOUBLE;
        }
        return Line.LINE_SIMPLE;
    }

    private int getHslfLinePen(String style) {
        if ("double".equals(style)) {
            return Line.PEN_SOLID;
        }
        if ("solid".equals(style)) {
            return Line.PEN_SOLID;
        }
        if ("dashed".equals(style)) {
            return Line.PEN_DASH;
        }
        if ("dotted".equals(style)) {
            return Line.PEN_DOT;
        }
        return -1;
    }

    private Rectangle getAnchor(Frame frame) {
        Rectangle rct = new Rectangle(scale(frame.getLeft()), scale(frame.getTop()), scale(frame.getRight() - frame.getLeft()), scale(frame.getBottom() - frame.getTop()));
        return rct;
    }

    private int loadPicture(IImageArea imageArea) {
        byte[] data = imageArea.getImageData();
        if (data == null) {
            String url = imageArea.getImageUrl();
            data = loadPictureData(url);
        }
        if (data != null) {
            int type = getPictureType(data);
            if (type != -1) {
                try {
                    return slideShow.addPicture(data, type);
                } catch (IOException e) {
                }
            } else {
                try {
                    data = ImageUtil.convertImage(data, "png");
                    return slideShow.addPicture(data, Picture.PNG);
                } catch (IOException e) {
                }
            }
        }
        return -1;
    }

    private byte[] loadPictureData(String url) {
        if (url != null) {
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(new URL(url).openStream());
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int count = bis.read(buf);
                while (count != -1) {
                    bos.write(buf, 0, count);
                    count = bis.read(buf);
                }
                return bos.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    private int getPictureType(byte[] data) {
        int type = ImageUtil.getImageType(data);
        switch(type) {
            case ImageUtil.TYPE_DIB:
                return Picture.DIB;
            case ImageUtil.TYPE_PNG:
                return Picture.PNG;
            case ImageUtil.TYPE_JPEG:
                return Picture.JPEG;
        }
        return -1;
    }

    private int scale(int val) {
        return Math.round(val / 1000f);
    }

    private double scaleD(int val) {
        return val / 1000d;
    }

    private boolean isTotalPageArea(Object element) {
        if (element != null && totalPageAreas != null) {
            return totalPageAreas.contains(element);
        }
        return false;
    }

    public void visitText(ITextArea textArea) {
        buildFrame(textArea, false);
    }

    public void visitImage(IImageArea imageArea) {
        buildFrame(imageArea, false);
    }

    public void visitAutoText(ITemplateArea templateArea) {
        buildFrame(templateArea, false);
        int type = EngineUtil.getTemplateType(templateArea);
        if (type == IAutoTextContent.TOTAL_PAGE || type == IAutoTextContent.UNFILTERED_TOTAL_PAGE) {
            if (totalPageAreas == null) {
                totalPageAreas = new HashSet<IArea>();
            }
            totalPageAreas.add(templateArea);
        }
    }

    public void visitContainer(IContainerArea containerArea) {
        startContainer(containerArea);
        Iterator iter = containerArea.getChildren();
        while (iter.hasNext()) {
            IArea child = (IArea) iter.next();
            child.accept(this);
        }
        endContainer(containerArea);
    }

    public void setTotalPage(ITextArea totalPage) {
        if (totalPage != null && totalShapes != null) {
            String text = totalPage.getText();
            for (Iterator<TextBox> itr = totalShapes.iterator(); itr.hasNext(); ) {
                TextBox textShape = itr.next();
                textShape.setText(text);
            }
        }
    }

    private void buildFrame(IArea area, boolean isContainer) {
        AreaFrame frame = new AreaFrame(area);
        AreaFrame parentFrame = frameStack.isEmpty() ? null : (AreaFrame) frameStack.peek();
        frame.setParent(parentFrame);
        if (parentFrame != null) {
            parentFrame.addChild(frame);
        }
        if (isContainer) {
            frameStack.push(frame);
        }
    }
}
