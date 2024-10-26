package org.zkoss.zss.model.impl;

import org.zkoss.util.logging.Log;
import org.zkoss.zss.model.Book;
import org.zkoss.zss.model.Format;
import org.zkoss.zss.model.Image;
import org.zkoss.zss.model.Importer;
import org.zkoss.zss.model.ModelException;
import org.zkoss.zss.model.Range;
import org.zkoss.zss.model.Sheet;
import org.zkoss.zss.engine.xel.SSError;
import org.zkoss.lang.Objects;
import common.LengthUnit;
import jxl.format.CellFormat;
import jxl.CellType;
import jxl.SheetSettings;
import jxl.Workbook;
import jxl.biff.EmptyCell;
import jxl.biff.drawing.Drawing;
import jxl.biff.drawing.DrawingGroup;
import jxl.biff.drawing.ZssDrawing;
import jxl.biff.drawing.MsoDrawingRecord;
import jxl.biff.drawing.Origin;
import jxl.biff.formula.FormulaException;
import jxl.biff.formula.ZssTokenFormulaParser;
import jxl.read.biff.BiffException;
import jxl.read.biff.ColumnInfoRecord;
import jxl.read.biff.RowRecord;
import jxl.read.biff.ZssWorkbookParser;
import jxl.write.WritableImage;
import jxl.format.Border;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Currency;
import java.util.List;

/**
 * Imports an Excel file into {@link Book} object.
 * @author henrichen
 *
 */
public class ExcelImporter implements Importer {

    private Log log = Log.lookup(ExcelImporter.class);

    public Book imports(Object obj) {
        if (obj instanceof String) {
            return importFromFile(new File((String) obj));
        } else if (obj instanceof File) {
            return importFromFile((File) obj);
        } else if (obj instanceof URL) {
            return importFromURL((URL) obj);
        }
        throw new IllegalArgumentException("Unsupported object :" + obj);
    }

    /**
	 * Import data from an InputStream to zk spreadsheet book, the InputStream must provide a excel format data 
	 * 
	 * @param is the excel data InputStream
	 * @param name the book name
	 * @return A {@link Book} with imported cells
	 * @throws BiffException
	 * @throws IOException
	 * @throws FormulaException
	 */
    public Book importFromStream(InputStream is, String name) {
        Workbook wb = null;
        try {
            wb = ZssWorkbookParser.getWorkbook(is);
            BookImpl book = new BookImpl(name);
            jxl.Sheet[] sheets = wb.getSheets();
            for (int i = 0; i < sheets.length; i++) {
                jxl.Sheet jsheet = sheets[i];
                Sheet sh = book.addSheet(jsheet.getName(), jsheet.getColumns(), jsheet.getRows());
            }
            importNamedRanges(wb, book);
            final List zksheets = book.getSheets();
            for (int i = 0; i < sheets.length; i++) {
                jxl.Sheet jsheet = sheets[i];
                importEachSheet(wb, (SheetImpl) zksheets.get(i), jsheet);
            }
            return book;
        } catch (Exception ex) {
            throw ModelException.Aide.wrap(ex);
        } finally {
            if (wb != null) {
                wb.close();
            }
        }
    }

    /**
	 * Import data from an excel file to zk spreadsheet book
	 * 
	 * @param file the target file
	 * @return A {@link Book} with imported cells
	 * @throws BiffException
	 * @throws IOException
	 * @throws FormulaException
	 */
    public Book importFromFile(File file) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            return importFromStream(is, file.getName());
        } catch (Exception ex) {
            throw ModelException.Aide.wrap(ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    throw ModelException.Aide.wrap(ex);
                }
            }
        }
    }

    /**
	 * Import data from an url to zk spreadsheet book
	 * 
	 * @param url the target url
	 * @return A {@link Book} with imported cells
	 * @throws BiffException
	 * @throws IOException
	 * @throws FormulaException
	 */
    public Book importFromURL(URL url) {
        InputStream is = null;
        try {
            is = url.openStream();
            return importFromStream(is, url.toString());
        } catch (Exception ex) {
            throw ModelException.Aide.wrap(ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    throw ModelException.Aide.wrap(ex);
                }
            }
        }
    }

    private void importEachSheet(Workbook wb, SheetImpl zkSheet, jxl.Sheet sheet) throws FormulaException {
        importMergedCells(zkSheet, sheet);
        importSheetFormat(zkSheet, sheet);
        for (int i = 0; i < sheet.getRows(); i++) {
            importEachRow(wb, zkSheet, i, sheet.getRow(i));
        }
        importSheetImages(zkSheet, sheet);
    }

    private void importEachRow(Workbook wb, SheetImpl zkSheet, int row, jxl.Cell[] rowcells) throws FormulaException {
        for (int i = 0; i < rowcells.length; i++) {
            importEachCell(wb, zkSheet, row, i, rowcells[i]);
        }
    }

    private void importEachCell(Workbook wb, SheetImpl zkSheet, int row, int col, jxl.Cell cell) throws FormulaException {
        if (!(cell instanceof EmptyCell)) {
            Object _cell = importCell(wb, cell);
            zkSheet.setCellValue(row, col, _cell);
            zkSheet.getCell(row, col).setFormat(importCellStyle(cell, row, col));
        }
    }

    private Object importCell(Workbook wb, jxl.Cell cell) throws FormulaException {
        CellType type = cell.getType();
        if (cell instanceof jxl.biff.FormulaData) {
            return new FormulaImpl(getFormula(wb, (jxl.biff.FormulaData) cell));
        } else {
            if (CellType.DATE.equals(type)) {
                jxl.DateCell dc = (jxl.DateCell) cell;
                return dc.getDate();
            } else if (CellType.NUMBER.equals(type)) {
                jxl.NumberCell nc = (jxl.NumberCell) cell;
                return new Double(nc.getValue());
            } else if (CellType.BOOLEAN.equals(type)) {
                return Boolean.valueOf(((jxl.BooleanCell) cell).getValue());
            } else if (CellType.FORMULA_ERROR.equals(type)) {
                return cell.getContents();
            } else if (CellType.ERROR.equals(type)) {
                return importErrorCell(((jxl.ErrorCell) cell).getErrorCode());
            } else if (CellType.EMPTY.equals(type)) {
                return null;
            } else {
                return cell.getContents();
            }
        }
    }

    private String getFormula(Workbook wb, jxl.biff.FormulaData cell) throws FormulaException {
        final byte[] data = cell.getFormulaData();
        final byte[] tokens = new byte[data.length - 16];
        System.arraycopy(data, 16, tokens, 0, tokens.length);
        final ZssTokenFormulaParser fp = new ZssTokenFormulaParser(tokens, (jxl.Cell) cell, (jxl.biff.formula.ExternalSheet) wb, (jxl.biff.WorkbookMethods) wb, ((ZssWorkbookParser) wb).getSettings());
        fp.parse();
        return fp.getFormula();
    }

    private Object importErrorCell(int errorCode) {
        switch(errorCode) {
            case 7:
                return SSError.DIV0;
            case 42:
                return SSError.NA;
            case 36:
                return SSError.NUM;
            case 29:
                return SSError.NAME;
            case 23:
                return SSError.REF;
            case 15:
                return SSError.VALUE;
            case 0:
                return SSError.NULL;
        }
        return SSError.EX;
    }

    private FormatImpl importCellStyle(jxl.Cell cell, int row, int col) {
        FormatImpl zkFormat = new FormatImpl();
        CellFormat format = cell.getCellFormat();
        zkFormat.setFormatCodes(format.getFormat().getFormatString());
        zkFormat.setTextVAlign(ExcelFormatHelper.getTextVAlign(format.getVerticalAlignment()));
        zkFormat.setTextHAlign(ExcelFormatHelper.getTextHAlign(format.getAlignment()));
        zkFormat.setTextIndent(format.getIndentation());
        zkFormat.setTextAngle(format.getOrientation().getValue());
        zkFormat.setTextWrap(format.getWrap());
        zkFormat.setTextShrinkToFit(format.isShrinkToFit());
        jxl.format.Font font = format.getFont();
        zkFormat.setFontType(font.getName());
        zkFormat.setFontStyle(ExcelFormatHelper.getFontStyle(font));
        zkFormat.setFontSize(font.getPointSize());
        zkFormat.setFontUnderline(ExcelFormatHelper.getFontUnlineStyle(font.getUnderlineStyle()));
        zkFormat.setFontColor(ExcelFormatHelper.jxlColourToHex(font.getColour()));
        zkFormat.setFontStrikethrough(font.isStruckout());
        zkFormat.setFontSuperscript(ExcelFormatHelper.isSuperscript(font.getScriptStyle()));
        zkFormat.setFontSubscript(ExcelFormatHelper.isSubscript(font.getScriptStyle()));
        if (format.hasBorders()) {
            zkFormat.setBorderBottom(ExcelFormatHelper.getBorderStyle(format.getBorderColour(Border.BOTTOM), format.getBorderLine(Border.BOTTOM)));
            zkFormat.setBorderLeft(ExcelFormatHelper.getBorderStyle(format.getBorderColour(Border.LEFT), format.getBorderLine(Border.LEFT)));
            zkFormat.setBorderRight(ExcelFormatHelper.getBorderStyle(format.getBorderColour(Border.RIGHT), format.getBorderLine(Border.RIGHT)));
            zkFormat.setBorderTop(ExcelFormatHelper.getBorderStyle(format.getBorderColour(Border.TOP), format.getBorderLine(Border.TOP)));
        }
        zkFormat.setFillColor(ExcelFormatHelper.jxlColourToHex(format.getBackgroundColour()));
        zkFormat.setPatternStyle(ExcelFormatHelper.getPatternStyle(format.getPattern()));
        zkFormat.setLocked(format.isLocked());
        zkFormat.setHidden(cell.isHidden());
        return zkFormat;
    }

    private FormatImpl importNumberCellFormat(FormatImpl zkFormat, jxl.NumberCell cell, int row, int col) {
        zkFormat.setFormatCodes(cell.getCellFormat().getFormat().getFormatString());
        return zkFormat;
    }

    private FormatImpl importDateTimeCelFormat(FormatImpl zkFormat, jxl.DateCell cell, int row, int col) {
        CellFormat cf = cell.getCellFormat();
        zkFormat.setFormatCodes(cf.getFormat().getFormatString());
        return zkFormat;
    }

    private void importMergedCells(SheetImpl zkSheet, jxl.Sheet sheet) {
        jxl.Range ranges[] = sheet.getMergedCells();
        for (int i = 0, l = ranges.length; i < l; i++) {
            jxl.Cell topLeft = ranges[i].getTopLeft();
            jxl.Cell bottomRight = ranges[i].getBottomRight();
            zkSheet.mergeCells(topLeft.getColumn(), topLeft.getRow(), bottomRight.getColumn(), bottomRight.getRow());
        }
    }

    private void importSheetFormat(SheetImpl zkSheet, jxl.Sheet sheet) {
        final jxl.read.biff.SheetImpl jsheet = (jxl.read.biff.SheetImpl) sheet;
        SheetSettings settings = sheet.getSettings();
        final int colWidthInChar = settings.getDefaultColumnWidth();
        final int rowHeightInPt = settings.getDefaultRowHeight() / 20;
        zkSheet.setDefaultColumnWidth(ExcelFormatHelper.charToPx(colWidthInChar));
        zkSheet.setDefaultRowHeight(ExcelFormatHelper.ptToPx(rowHeightInPt));
        final RowRecord[] rinfos = jsheet.getRowProperties();
        final int rowsz = rinfos.length;
        for (int j = 0; j < rowsz; j++) {
            final RowRecord rinfo = rinfos[j];
            if (!ZssWorkbookParser.isDefaultHeight(rinfo)) {
                final int rowHeight = rinfo.getRowHeight() / 20;
                final int ri = rinfo.getRowNumber();
                zkSheet.setRowHeight(ri, ExcelFormatHelper.ptToPx(rowHeight));
            }
        }
        final ColumnInfoRecord[] cinfos = jsheet.getColumnInfos();
        final int colsz = cinfos.length;
        for (int j = 0; j < colsz; j++) {
            final ColumnInfoRecord cinfo = cinfos[j];
            final int sci = cinfo.getStartColumn();
            final int eci = cinfo.getEndColumn();
            final int colWidth = cinfo.getWidth() / 256;
            if (colWidth != colWidthInChar) {
                for (int k = sci; k <= eci; ++k) {
                    zkSheet.setColumnWidth(k, ExcelFormatHelper.charToPx(colWidth));
                }
            }
        }
        int hFreeze = settings.getHorizontalFreeze();
        int vFreeze = settings.getVerticalFreeze();
        zkSheet.setRowFreeze(vFreeze - 1);
        zkSheet.setColumnFreeze(hFreeze - 1);
    }

    private void importSheetImages(SheetImpl zkSheet, jxl.Sheet sheet) {
        final int sz = sheet.getNumberOfImages();
        for (int i = 0; i < sz; i++) {
            Drawing drawing = (Drawing) sheet.getDrawing(i);
            ZssDrawing zd = new ZssDrawing(drawing);
            try {
                final Image image = new ImageImpl(zd.getName(), zd.getAlt(), zd.getImageData(), zd.getTop(), zd.getTopFraction() / 256d, zd.getLeft(), zd.getLeftFraction() / 1024d, getWidthInPx(zkSheet, zd), getHeightInPx(zkSheet, zd));
                zkSheet.addImage(image);
                if (log.debugable()) {
                    StringBuffer sb = new StringBuffer();
                    sb.append(zd.getName());
                    sb.append("\n\tProperties: " + zd.getProperties());
                    sb.append("\n\tLeft: " + zd.getLeft());
                    sb.append("\n\tLeftFraction: " + zd.getLeftFraction());
                    sb.append("\n\tTop: " + zd.getTop());
                    sb.append("\n\tTopFraction: " + zd.getTopFraction());
                    sb.append("\n\tRight: " + zd.getRight());
                    sb.append("\n\tRightFraction: " + zd.getRightFraction());
                    sb.append("\n\tBottom: " + zd.getBottom());
                    sb.append("\n\tBottomFraction: " + zd.getBottomFraction());
                    sb.append("\n\timage.getAlt(): " + image.getAlt());
                    sb.append("\n\timage.getColumn(): " + image.getColumn());
                    sb.append("\n\timage.getColumnFraction(): " + image.getColumnFraction());
                    sb.append("\n\timage.getRow(): " + image.getRow());
                    sb.append("\n\timage.getRowFaction(): " + image.getRowFraction());
                    sb.append("\n\timage.getFrameWidth(): " + image.getFrameWidth());
                    sb.append("\n\timage.getFrameHeight():" + image.getFrameHeight());
                    log.debug(sb.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int getWidthInPx(Sheet zkSheet, ZssDrawing zd) {
        final int l = zd.getLeft();
        final int lfrc = zd.getLeftFraction();
        final int lw = getWidthAny(zkSheet, l);
        final int wFirst = lfrc >= 1024 ? 0 : (lw - lw * lfrc / 1024);
        final int r = zd.getRight();
        int wLast = 0;
        if (l != r) {
            final int rfrc = zd.getRightFraction();
            final int rw = getWidthAny(zkSheet, r);
            wLast = rw * rfrc / 1024;
        }
        int width = wFirst + wLast;
        for (int j = l + 1; j < r; ++j) {
            width += getWidthAny(zkSheet, j);
        }
        return width;
    }

    private int getHeightInPx(Sheet zkSheet, ZssDrawing zd) {
        final int t = zd.getTop();
        final int tfrc = zd.getTopFraction();
        final int th = getHeightAny(zkSheet, t);
        final int hFirst = tfrc >= 256 ? 0 : (th - th * tfrc / 256);
        final int b = zd.getBottom();
        int hLast = 0;
        if (t != b) {
            final int bfrc = zd.getBottomFraction();
            final int bh = getHeightAny(zkSheet, b);
            hLast = bh * bfrc / 256;
        }
        int height = hFirst + hLast;
        for (int j = t + 1; j < b; ++j) {
            height += getHeightAny(zkSheet, j);
        }
        return height;
    }

    private int getWidthAny(Sheet zkSheet, int col) {
        int w = zkSheet.getColumnWidth(col);
        if (w == -1) return zkSheet.getDefaultColumnWidth();
        return w;
    }

    private int getHeightAny(Sheet zkSheet, int row) {
        int h = zkSheet.getRowHeight(row);
        if (h == -1) return zkSheet.getDefaultRowHeight();
        return h;
    }

    private void importNamedRanges(Workbook workbook, BookImpl book) {
        String[] rangeNames = workbook.getRangeNames();
        for (int i = 0; i < rangeNames.length; i++) {
            jxl.Range[] range = workbook.findByName(rangeNames[i]);
            if (range.length > 0) {
                int sheetNumber = range[0].getFirstSheetIndex();
                book.addNameRange(rangeNames[i], (SheetImpl) book.getSheets().get(sheetNumber), range[0].getTopLeft().getColumn(), range[0].getTopLeft().getRow(), range[0].getBottomRight().getColumn(), range[0].getBottomRight().getRow());
            }
        }
    }
}
