package org.uguess.birt.report.engine.emitter.xls;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlHelper;
import org.apache.commons.jexl.parser.ASTAddNode;
import org.apache.commons.jexl.parser.ASTAndNode;
import org.apache.commons.jexl.parser.ASTArrayAccess;
import org.apache.commons.jexl.parser.ASTAssignment;
import org.apache.commons.jexl.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl.parser.ASTBlock;
import org.apache.commons.jexl.parser.ASTDivNode;
import org.apache.commons.jexl.parser.ASTEQNode;
import org.apache.commons.jexl.parser.ASTEmptyFunction;
import org.apache.commons.jexl.parser.ASTExpression;
import org.apache.commons.jexl.parser.ASTExpressionExpression;
import org.apache.commons.jexl.parser.ASTFalseNode;
import org.apache.commons.jexl.parser.ASTFloatLiteral;
import org.apache.commons.jexl.parser.ASTForeachStatement;
import org.apache.commons.jexl.parser.ASTGENode;
import org.apache.commons.jexl.parser.ASTGTNode;
import org.apache.commons.jexl.parser.ASTIdentifier;
import org.apache.commons.jexl.parser.ASTIfStatement;
import org.apache.commons.jexl.parser.ASTIntegerLiteral;
import org.apache.commons.jexl.parser.ASTJexlScript;
import org.apache.commons.jexl.parser.ASTLENode;
import org.apache.commons.jexl.parser.ASTLTNode;
import org.apache.commons.jexl.parser.ASTMethod;
import org.apache.commons.jexl.parser.ASTModNode;
import org.apache.commons.jexl.parser.ASTMulNode;
import org.apache.commons.jexl.parser.ASTNENode;
import org.apache.commons.jexl.parser.ASTNotNode;
import org.apache.commons.jexl.parser.ASTNullLiteral;
import org.apache.commons.jexl.parser.ASTOrNode;
import org.apache.commons.jexl.parser.ASTReference;
import org.apache.commons.jexl.parser.ASTReferenceExpression;
import org.apache.commons.jexl.parser.ASTSizeFunction;
import org.apache.commons.jexl.parser.ASTSizeMethod;
import org.apache.commons.jexl.parser.ASTStatementExpression;
import org.apache.commons.jexl.parser.ASTStringLiteral;
import org.apache.commons.jexl.parser.ASTSubtractNode;
import org.apache.commons.jexl.parser.ASTTrueNode;
import org.apache.commons.jexl.parser.ASTUnaryMinusNode;
import org.apache.commons.jexl.parser.ASTWhileStatement;
import org.apache.commons.jexl.parser.Parser;
import org.apache.commons.jexl.parser.ParserVisitor;
import org.apache.commons.jexl.parser.SimpleNode;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFComment;
import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.eclipse.birt.report.engine.api.IHTMLActionHandler;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.birt.report.engine.api.impl.Action;
import org.eclipse.birt.report.engine.content.IAutoTextContent;
import org.eclipse.birt.report.engine.content.IContent;
import org.eclipse.birt.report.engine.content.IHyperlinkAction;
import org.eclipse.birt.report.engine.content.IPageContent;
import org.eclipse.birt.report.engine.content.IReportContent;
import org.eclipse.birt.report.engine.content.impl.PageContent;
import org.eclipse.birt.report.engine.emitter.IEmitterServices;
import org.eclipse.birt.report.engine.ir.ReportElementDesign;
import org.eclipse.birt.report.engine.nLayout.area.IArea;
import org.eclipse.birt.report.engine.nLayout.area.IAreaVisitor;
import org.eclipse.birt.report.engine.nLayout.area.IContainerArea;
import org.eclipse.birt.report.engine.nLayout.area.IImageArea;
import org.eclipse.birt.report.engine.nLayout.area.ITemplateArea;
import org.eclipse.birt.report.engine.nLayout.area.ITextArea;
import org.eclipse.birt.report.engine.nLayout.area.impl.PageArea;
import org.eclipse.birt.report.model.api.ReportElementHandle;
import org.eclipse.birt.report.model.api.elements.DesignChoiceConstants;
import org.uguess.birt.report.engine.layout.wrapper.Frame;
import org.uguess.birt.report.engine.layout.wrapper.impl.AggregateFrame;
import org.uguess.birt.report.engine.layout.wrapper.impl.AreaFrame;
import org.uguess.birt.report.engine.spreadsheet.model.Cell;
import org.uguess.birt.report.engine.spreadsheet.model.MergeBlock;
import org.uguess.birt.report.engine.spreadsheet.model.Sheet;
import org.uguess.birt.report.engine.spreadsheet.wrapper.Transformer;
import org.uguess.birt.report.engine.util.EngineUtil;
import org.uguess.birt.report.engine.util.ImageUtil;
import org.uguess.birt.report.engine.util.OptionParser;
import org.uguess.birt.report.engine.util.PageRangeChecker;

/**
 * XlsRenderer
 */
public class XlsRenderer implements IAreaVisitor {

    public static final String DEFAULT_FILE_NAME = "report.xls";

    protected static final String XLS_IDENTIFIER = "xls";

    protected static final int COMMENTS_WIDTH_IN_COLUMN = 3;

    protected static final int COMMENTS_HEIGHT_IN_ROW = 5;

    private static final String ATTR_LANDSCAPE = "landscape";

    private static final boolean DEBUG;

    static {
        DEBUG = System.getProperty("debug") != null;
    }

    protected boolean removeEmptyRow = false;

    protected boolean showGridLines = false;

    protected boolean exportBodyOnly = false;

    protected boolean suppressUnknownImage = false;

    protected boolean exportSingleSheet = false;

    protected boolean enableComments = true;

    protected Expression sheetNameExpr;

    protected int fixedColumnWidth = -1;

    private int dpi;

    private double baseCharWidth;

    private AggregateFrame singleContainer;

    private PageRangeChecker rangeChecker;

    private int currentPageIndex;

    private int commentsAnchorColumnIndex, commentsAnchorRowIndex;

    private boolean closeStream;

    private OutputStream output = null;

    private HSSFWorkbook workbook;

    private XlsStyleProcessor processor;

    protected Stack<Frame> frameStack;

    protected Set<HSSFCell> totalCells;

    protected Set<IArea> totalPageAreas;

    protected IEmitterServices services;

    private long timeCounter;

    Map<IArea, IContent> contentCache;

    public void initialize(IEmitterServices services) {
        rangeChecker = new PageRangeChecker(null);
        removeEmptyRow = false;
        showGridLines = false;
        fixedColumnWidth = -1;
        exportBodyOnly = false;
        exportSingleSheet = false;
        dpi = 96;
        suppressUnknownImage = false;
        enableComments = true;
        sheetNameExpr = null;
        closeStream = false;
        this.services = services;
        Object option = services.getEmitterConfig().get(XLS_IDENTIFIER);
        if (option instanceof Map) {
            parseRendererOptions((Map) option);
        } else if (option instanceof IRenderOption) {
            parseRendererOptions(((IRenderOption) option).getOptions());
        }
        parseRendererOptions(services.getRenderOption().getOptions());
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

    protected void parseRendererOptions(Map xlsOption) {
        if (xlsOption == null) {
            return;
        }
        Object value;
        value = xlsOption.get("dpi");
        if (value != null) {
            int pdpi = OptionParser.parseInt(value);
            if (pdpi != -1) {
                dpi = pdpi;
            }
        }
        value = xlsOption.get(XlsEmitterConfig.KEY_PAGE_RANGE);
        if (value != null) {
            rangeChecker = new PageRangeChecker(OptionParser.parseString(value));
        }
        value = xlsOption.get(XlsEmitterConfig.KEY_REMOVE_EMPTY_ROW);
        if (value != null) {
            removeEmptyRow = OptionParser.parseBoolean(value);
        }
        value = xlsOption.get(XlsEmitterConfig.KEY_SHOW_GRID_LINES);
        if (value != null) {
            showGridLines = OptionParser.parseBoolean(value);
        }
        value = xlsOption.get(XlsEmitterConfig.KEY_FIXED_COLUMN_WIDTH);
        if (value != null) {
            fixedColumnWidth = OptionParser.parseInt(value);
        }
        value = xlsOption.get(XlsEmitterConfig.KEY_EXPORT_BODY_ONLY);
        if (value != null) {
            exportBodyOnly = OptionParser.parseBoolean(value);
        }
        value = xlsOption.get(XlsEmitterConfig.KEY_EXPORT_SINGLE_SHEET);
        if (value != null) {
            exportSingleSheet = OptionParser.parseBoolean(value);
        }
        value = xlsOption.get(XlsEmitterConfig.KEY_SUPPRESS_UNKNOWN_IMAGE);
        if (value != null) {
            suppressUnknownImage = OptionParser.parseBoolean(value);
        }
        value = xlsOption.get(XlsEmitterConfig.KEY_ENABLE_COMMENTS);
        if (value != null) {
            enableComments = OptionParser.parseBoolean(value);
        }
        value = xlsOption.get(XlsEmitterConfig.KEY_SHEET_NAME_EXPR);
        if (value != null) {
            sheetNameExpr = parseSheetNameExpression(OptionParser.parseString(value));
        }
        value = xlsOption.get(IRenderOption.CLOSE_OUTPUTSTREAM_ON_EXIT);
        if (value != null) {
            closeStream = OptionParser.parseBoolean(value);
        }
    }

    private Expression parseSheetNameExpression(String expr) {
        Expression expression = null;
        if (expr != null) {
            try {
                expr = expr.trim();
                if (!expr.endsWith(";")) {
                    expr += ";";
                }
                Parser parser = new Parser(new StringReader(";"));
                SimpleNode tree = parser.parse(new StringReader(expr));
                SimpleNode node = (SimpleNode) tree.jjtGetChild(0);
                node.jjtAccept(new ParserVisitor() {

                    public Object visit(ASTReference node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTSizeMethod node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTArrayAccess node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTMethod node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTForeachStatement node, Object data) {
                        throw new UnsupportedOperationException("Loop function is not allowed.");
                    }

                    public Object visit(ASTWhileStatement node, Object data) {
                        throw new UnsupportedOperationException("Loop function is not allowed.");
                    }

                    public Object visit(ASTIfStatement node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTReferenceExpression node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTStatementExpression node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTExpressionExpression node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTStringLiteral node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTFloatLiteral node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTIntegerLiteral node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTFalseNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTTrueNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTNullLiteral node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTNotNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTBitwiseComplNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTUnaryMinusNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTModNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTDivNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTMulNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTSubtractNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTAddNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTGENode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTLENode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTGTNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTLTNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTNENode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTEQNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTBitwiseAndNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTBitwiseXorNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTBitwiseOrNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTAndNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTOrNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTAssignment node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTExpression node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTIdentifier node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTSizeFunction node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTEmptyFunction node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTBlock node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(ASTJexlScript node, Object data) {
                        return node.childrenAccept(this, data);
                    }

                    public Object visit(SimpleNode node, Object data) {
                        return node.childrenAccept(this, data);
                    }
                }, null);
                expression = ExpressionFactory.createExpression(expr);
            } catch (Exception e) {
                expression = null;
                e.printStackTrace();
            }
        }
        return expression;
    }

    public void start(IReportContent rc) {
        if (DEBUG) {
            timeCounter = System.currentTimeMillis();
        }
        this.frameStack = new Stack<Frame>();
        workbook = new HSSFWorkbook();
        currentPageIndex = 1;
        baseCharWidth = 7 * 72d / dpi;
        processor = new XlsStyleProcessor(workbook);
    }

    public void end(IReportContent rc) {
        if (exportSingleSheet && singleContainer != null) {
            long span = System.currentTimeMillis();
            Sheet modelSheet = Transformer.toSheet(singleContainer, 1000, fixedColumnWidth * 1000);
            exportSheet(modelSheet, Boolean.TRUE == singleContainer.getAttribute(ATTR_LANDSCAPE));
            if (DEBUG) {
                System.out.println("------------export total single sheet using " + (System.currentTimeMillis() - span) + " ms");
            }
        }
        try {
            processor.optimize();
            workbook.write(output);
            if (closeStream) {
                output.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        frameStack.clear();
        processor.dispose();
        processor = null;
        workbook = null;
        frameStack = null;
        singleContainer = null;
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
            exportFrame(currentFrame);
        }
    }

    protected void exportFrame(Frame currentFrame) {
        long span = System.currentTimeMillis();
        if (rangeChecker.checkRange(currentPageIndex)) {
            PageArea pa = (PageArea) getTranslatedElementValue(currentFrame.getData());
            IPageContent content = (PageContent) pa.getContent();
            boolean landscape = DesignChoiceConstants.PAGE_ORIENTATION_LANDSCAPE.equals(content.getOrientation());
            if (exportBodyOnly) {
                IContainerArea body = pa.getBody();
                Frame bodyFrame = locateChildFrame(currentFrame, body);
                if (bodyFrame instanceof AreaFrame) {
                    ((AreaFrame) bodyFrame).setXOffset(-bodyFrame.getLeft());
                    ((AreaFrame) bodyFrame).setYOffset(-bodyFrame.getTop());
                }
                if (exportSingleSheet) {
                    if (singleContainer == null) {
                        singleContainer = new AggregateFrame();
                        singleContainer.setRight(bodyFrame.getRight() - bodyFrame.getLeft());
                        singleContainer.setBottom(bodyFrame.getBottom() - bodyFrame.getTop());
                        if (landscape) {
                            singleContainer.putAttribute(ATTR_LANDSCAPE, Boolean.TRUE);
                        }
                    } else {
                        int yOff = singleContainer.getBottom();
                        singleContainer.setBottom(yOff + bodyFrame.getBottom() - bodyFrame.getTop());
                        if (bodyFrame instanceof AreaFrame) {
                            ((AreaFrame) bodyFrame).setYOffset(yOff + ((AreaFrame) bodyFrame).getYOffset());
                        }
                    }
                    singleContainer.addChild(bodyFrame);
                    if (bodyFrame instanceof AreaFrame) {
                        ((AreaFrame) bodyFrame).setParent(singleContainer);
                    }
                    if (DEBUG) {
                        System.out.println("------------aggregate sheet[" + (currentPageIndex) + "] using " + (System.currentTimeMillis() - span) + " ms");
                    }
                } else {
                    Sheet modelSheet = Transformer.toSheet(bodyFrame, 1000, fixedColumnWidth * 1000);
                    exportSheet(modelSheet, landscape);
                    if (DEBUG) {
                        System.out.println("------------export sheet[" + (currentPageIndex) + "] using " + (System.currentTimeMillis() - span) + " ms");
                    }
                }
            } else {
                if (exportSingleSheet) {
                    if (singleContainer == null) {
                        singleContainer = new AggregateFrame();
                        singleContainer.setRight(currentFrame.getRight() - currentFrame.getLeft());
                        singleContainer.setBottom(currentFrame.getBottom() - currentFrame.getTop());
                        if (landscape) {
                            singleContainer.putAttribute(ATTR_LANDSCAPE, Boolean.TRUE);
                        }
                    } else {
                        int yOff = singleContainer.getBottom();
                        singleContainer.setBottom(yOff + currentFrame.getBottom() - currentFrame.getTop());
                        if (currentFrame instanceof AreaFrame) {
                            ((AreaFrame) currentFrame).setYOffset(yOff);
                        }
                    }
                    singleContainer.addChild(currentFrame);
                    if (currentFrame instanceof AreaFrame) {
                        ((AreaFrame) currentFrame).setParent(singleContainer);
                    }
                    if (DEBUG) {
                        System.out.println("------------aggregate sheet[" + (currentPageIndex) + "] using " + (System.currentTimeMillis() - span) + " ms");
                    }
                } else {
                    Sheet modelSheet = Transformer.toSheet(currentFrame, 1000, fixedColumnWidth * 1000);
                    exportSheet(modelSheet, landscape);
                    if (DEBUG) {
                        System.out.println("------------export sheet[" + (currentPageIndex) + "] using " + (System.currentTimeMillis() - span) + " ms");
                    }
                }
            }
        }
        currentPageIndex++;
    }

    /**
	 * Find child frame by specific data object(use "==" to compare)
	 * 
	 * @param parentFrame
	 * @param data
	 * @return
	 */
    protected Frame locateChildFrame(Frame parentFrame, Object data) {
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

    protected boolean isEffectiveCell(Cell cell) {
        if (cell.isEmpty()) {
            return false;
        }
        return true;
    }

    private String getSheetName() {
        if (sheetNameExpr != null) {
            try {
                JexlContext context = JexlHelper.createContext();
                context.getVars().put("pageIndex", currentPageIndex);
                context.getVars().put("sheetIndex", workbook.getNumberOfSheets());
                context.getVars().put("rptContext", new ManagedReportContext(services.getReportContext()));
                Object result = sheetNameExpr.evaluate(context);
                if (result != null) {
                    return result.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "Sheet" + workbook.getNumberOfSheets();
    }

    protected final void exportSheet(Sheet modelSheet, boolean landscape) {
        HSSFSheet xlsSheet = workbook.createSheet();
        HSSFPatriarch patriarch = xlsSheet.createDrawingPatriarch();
        xlsSheet.setDisplayGridlines(showGridLines);
        if (landscape) {
            xlsSheet.getPrintSetup().setLandscape(true);
        }
        try {
            workbook.setSheetName(workbook.getNumberOfSheets() - 1, getSheetName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        short columnCount = (short) (Math.min(modelSheet.getActiveColumnRange(), 254) + 1);
        int rowCount = modelSheet.getActiveRowRange() + 1;
        if (columnCount + COMMENTS_WIDTH_IN_COLUMN < 255) {
            commentsAnchorColumnIndex = columnCount;
        } else {
            commentsAnchorColumnIndex = 255 - COMMENTS_WIDTH_IN_COLUMN;
        }
        if (rowCount + COMMENTS_HEIGHT_IN_ROW < 65535) {
            commentsAnchorRowIndex = rowCount;
        } else {
            commentsAnchorRowIndex = 65535 - COMMENTS_HEIGHT_IN_ROW;
        }
        boolean[] nonBlankRow = new boolean[rowCount];
        if (removeEmptyRow) {
            for (int i = 0; i < rowCount; i++) {
                for (int j = 0; j < columnCount; j++) {
                    Cell cell = modelSheet.getCell(i, j, false);
                    if (cell != null && isEffectiveCell(cell)) {
                        nonBlankRow[i] = true;
                        break;
                    }
                }
            }
        }
        double width = 0;
        for (short i = 1; i < columnCount + 1; i++) {
            width = modelSheet.getColumnWidth(i - 1) / (1000 * baseCharWidth);
            xlsSheet.setColumnWidth((short) (i - 1), (short) (width * 256));
        }
        HSSFCellStyle emptyCellStyle = processor.getEmptyCellStyle();
        HSSFCell emptyCell = null;
        for (short y = 0; y < rowCount; y++) {
            if (!removeEmptyRow || nonBlankRow[y]) {
                HSSFRow xlsRow = xlsSheet.createRow(y);
                double height = modelSheet.getRowHeight(y) / 1000d;
                xlsRow.setHeightInPoints((float) height);
                for (short x = 0; x < columnCount; x++) {
                    emptyCell = xlsRow.createCell(x);
                    emptyCell.setCellStyle(emptyCellStyle);
                    Cell element = modelSheet.getCell(y, x, false);
                    if (element != null) {
                        exportCell(element, x, y, xlsSheet, patriarch, xlsRow, modelSheet);
                    } else {
                        emptyCell = xlsRow.createCell(x);
                        emptyCell.setCellStyle(emptyCellStyle);
                    }
                }
            } else {
                HSSFRow row = xlsSheet.createRow(y);
                row.setHeight((short) 0);
                for (short x = 0; x < columnCount; x++) {
                    emptyCell = row.createCell(x);
                    emptyCell.setCellStyle(emptyCellStyle);
                }
            }
        }
    }

    protected void exportCell(Cell element, short x, short y, HSSFSheet xlsSheet, HSSFPatriarch patriarch, HSSFRow xlsRow, Sheet modelSheet) {
        if (element.isEmpty()) {
            return;
        }
        short x2 = x;
        short y2 = y;
        if (element.isMerged()) {
            Iterator<MergeBlock> it = modelSheet.mergesIterator();
            while (it.hasNext()) {
                MergeBlock merge = it.next();
                if (merge.include(y, x)) {
                    if (x == merge.getStartColumn() && y == merge.getStartRow()) {
                        x2 = (short) (x + merge.getColumnSpan());
                        y2 = (short) (y + merge.getRowSpan());
                        xlsSheet.addMergedRegion(new CellRangeAddress(y, y2, x, x2));
                        break;
                    }
                }
            }
        }
        HSSFCell cell = xlsRow.createCell(x);
        Object cellValue = getTranslatedElementValue(element.getValue());
        boolean useHyperLinkStyle = false;
        if (cellValue instanceof ITextArea) {
            useHyperLinkStyle = handleHyperLink((IArea) cellValue, cell);
        }
        HSSFCellStyle cellStyle = processor.getHssfCellStyle(element.getStyle(), useHyperLinkStyle);
        cell.setCellStyle(cellStyle);
        exportCellData(element, x, y, x2, y2, cell, patriarch);
    }

    protected Object getTranslatedElementValue(Object value) {
        return value;
    }

    protected void exportCellData(Cell element, short x, short y, short x2, short y2, HSSFCell cell, HSSFPatriarch patriarch) {
        if (isTotalPageArea(element.getValue())) {
            if (totalCells == null) {
                totalCells = new HashSet<HSSFCell>();
            }
            totalCells.add(cell);
        }
        Object cellValue = getTranslatedElementValue(element.getValue());
        if (cellValue instanceof IImageArea) {
            exportImage((IImageArea) cellValue, cell, x, y, x2, y2, patriarch);
        } else if (cellValue instanceof ITextArea) {
            exportText((ITextArea) cellValue, cell);
        }
        if (enableComments && cellValue instanceof IArea) {
            handleComments((IArea) cellValue, cell, patriarch);
        }
    }

    protected void exportImage(IImageArea image, HSSFCell cell, short x, short y, short x2, short y2, HSSFPatriarch patriarch) {
        int idx = loadPicture(image);
        if (idx != -1) {
            HSSFClientAnchor anchor = new HSSFClientAnchor(0, 0, 0, 0, x, y, (short) (x2 + 1), y2 + 1);
            anchor.setAnchorType(2);
            patriarch.createPicture(anchor, idx);
        } else if (!suppressUnknownImage) {
            cell.setCellValue(new HSSFRichTextString("<<Unsupported Image>>"));
        }
    }

    protected void exportText(ITextArea text, HSSFCell cell) {
        exportText(text.getText(), cell);
    }

    protected void exportText(String csCellText, HSSFCell cell) {
        Double csNumberValue = null;
        try {
            csNumberValue = Double.parseDouble(csCellText);
        } catch (NumberFormatException e) {
            csNumberValue = null;
        }
        if (csNumberValue == null || csNumberValue.isNaN()) {
            cell.setCellValue(new HSSFRichTextString(csCellText));
        } else {
            cell.setCellValue(csNumberValue);
        }
    }

    protected boolean handleHyperLink(IArea area, HSSFCell cell) {
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
                    HSSFHyperlink hssflink = new HSSFHyperlink(HSSFHyperlink.LINK_URL);
                    hssflink.setAddress(link);
                    if (tooltip != null) {
                        hssflink.setLabel(tooltip);
                    }
                    cell.setHyperlink(hssflink);
                    return true;
                }
            } catch (Exception e) {
                System.out.println("create hyperlink failed.");
                e.printStackTrace();
            }
        }
        return false;
    }

    protected boolean handleComments(IArea area, HSSFCell cell, HSSFPatriarch patriarch) {
        IContent content = contentCache.get(area);
        if (content != null) {
            Object sourceObj = content.getGenerateBy();
            if (sourceObj instanceof ReportElementDesign) {
                sourceObj = ((ReportElementDesign) sourceObj).getHandle();
                if (sourceObj instanceof ReportElementHandle) {
                    String comments = ((ReportElementHandle) sourceObj).getComments();
                    if (comments != null && comments.length() > 0) {
                        HSSFComment hssfcomment = patriarch.createComment(new HSSFClientAnchor(0, 0, 0, 0, (short) commentsAnchorColumnIndex, commentsAnchorRowIndex, (short) (commentsAnchorColumnIndex + COMMENTS_WIDTH_IN_COLUMN), commentsAnchorRowIndex + COMMENTS_HEIGHT_IN_ROW));
                        hssfcomment.setString(new HSSFRichTextString(comments));
                        cell.setCellComment(hssfcomment);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected int loadPicture(IImageArea imageArea) {
        byte[] data = imageArea.getImageData();
        if (data == null) {
            String url = imageArea.getImageUrl();
            data = loadPictureData(url);
        }
        if (data != null) {
            int type = processor.getHssfPictureType(data);
            if (type != -1) {
                return workbook.addPicture(data, type);
            } else {
                try {
                    data = ImageUtil.convertImage(data, "png");
                    return workbook.addPicture(data, HSSFWorkbook.PICTURE_TYPE_PNG);
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

    protected boolean isTotalPageArea(Object element) {
        if (element != null && totalPageAreas != null) {
            return totalPageAreas.contains(element);
        }
        return false;
    }

    public void setTotalPage(ITextArea totalPage) {
        if (totalPage != null && totalCells != null) {
            String text = totalPage.getText();
            for (Iterator<HSSFCell> itr = totalCells.iterator(); itr.hasNext(); ) {
                HSSFCell cell = itr.next();
                exportText(text, cell);
            }
        }
    }

    protected void buildFrame(IArea area, boolean isContainer) {
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
