package com.amwebexpert.tags.workbook.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.poi.ss.format.CellFormat;
import org.apache.poi.ss.format.CellFormatResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;

/**
 * Wrapper class for the Apache POI Workbook. Adds usefull service methods for
 * simplifying workbook handling...
 * 
 * @author amwebexpert@gmail.comgmail.com
 */
public abstract class InMemoryWorkbook implements Externalizable {

    private static final long serialVersionUID = 1L;

    /** POI Workbook instance */
    protected transient Workbook workbook;

    private transient FormulaEvaluator evaluator;

    /**
	 * Display formulas instead of result
	 */
    private boolean showFormula;

    /** Original name of Excel file */
    private String originalFilename;

    private transient DataFormatterUtil util;

    /**
	 * Constructor
	 */
    public InMemoryWorkbook() {
    }

    /**
	 * Constructor
	 * 
	 * @param filename
	 *            The Microsoft Excel workbook filename. Exemple:
	 *            workbooks/test.xls
	 * @throws IOException
	 */
    public InMemoryWorkbook(String filename) throws IOException {
        this.originalFilename = filename;
        loadWorkbook(acquireInputStream(filename));
    }

    /**
	 * Constructor
	 * 
	 * @param in
	 *            Input stream to read the binary Excel file from
	 * @param filename
	 *            The Microsoft Excel workbook filename. Exemple:
	 *            workbooks/test.xls
	 * @throws IOException
	 */
    public InMemoryWorkbook(InputStream in, String filename) throws IOException {
        this.originalFilename = filename;
        loadWorkbook(in);
    }

    /**
	 * Create the input stream to read the binary Excel file from
	 * 
	 * @param filename
	 *            The Microsoft Excel workbook filename. Exemple:
	 *            workbooks/test.xls
	 * @return The input stream to read the binary Excel file from
	 * @throws IOException
	 */
    protected InputStream acquireInputStream(String filename) throws IOException {
        Validate.notEmpty(filename);
        File f = new File(filename);
        if (f.exists()) {
            this.originalFilename = f.getName();
            return new FileInputStream(f);
        }
        URL url = getClass().getClassLoader().getResource(filename);
        if (url == null) {
            if (!filename.startsWith("/")) {
                url = getClass().getClassLoader().getResource("/" + filename);
                if (url == null) {
                    throw new IllegalArgumentException("File [" + filename + "] not found in classpath via " + getClass().getClassLoader().getClass());
                }
            }
        }
        this.originalFilename = filename;
        return url.openStream();
    }

    /**
	 * Excel file loading into POI workbook object
	 * 
	 * @param fullFilename
	 *            The Microsoft Excel workbook full filename. Exemple:
	 *            c:/temp/workbooks/test.xls
	 * @throws IOException
	 */
    protected void loadWorkbook(String fullFilename) throws IOException {
        InputStream in = new FileInputStream(fullFilename);
        loadWorkbook(in);
    }

    /**
	 * Excel file loading into POI workbook object
	 * 
	 * @param in
	 *            Input stream to read the binary Excel file from
	 * @throws IOException
	 */
    protected void loadWorkbook(InputStream in) throws IOException {
        BufferedInputStream bufferedIn = new BufferedInputStream(in);
        Workbook w = instanciateWorkbook(bufferedIn);
        bufferedIn.close();
        setWorkbook(w);
    }

    /**
	 * Load the POI workbook and return the concrete instance
	 * 
	 * @param in
	 *            The input stream for reading the entire workbook
	 * @return The instanciated POI workbook
	 * @throws IOException
	 */
    protected abstract Workbook instanciateWorkbook(InputStream in) throws IOException;

    /**
	 * Return the maximum column index having data
	 * 
	 * @param sheet
	 *            The virtual worksheet
	 * @return The maximum column index having data
	 */
    public int getMaxColIndex(Sheet sheet) {
        int colIdx = 0;
        for (Row row : sheet) {
            if (row != null) {
                for (Cell cell : row) {
                    if (cell != null && cell.getColumnIndex() > colIdx) {
                        colIdx = cell.getColumnIndex();
                    }
                }
            }
        }
        return colIdx;
    }

    /**
	 * Write a value into into a specific location of a worksheet
	 * 
	 * @param sheet
	 *            The POI virtual worksheet
	 * @param rowNumber
	 *            The row index (zero base)
	 * @param colNumber
	 *            The column index (zero base)
	 * @param value
	 *            The new cell value
	 */
    public void writeCellValue(Sheet sheet, int rowNumber, int colNumber, double value) {
        Cell cell = getCellAt(sheet, rowNumber, colNumber);
        cell.setCellValue(value);
    }

    /**
	 * Return the POI cell object corresponding to specific row and column
	 * 
	 * @param sheet
	 *            The POI virtual worksheet
	 * @param rowNumber
	 *            The row index (zero base)
	 * @param colNumber
	 *            The column index (zero base)
	 * @return The POI cell object corresponding to specific row and column
	 */
    public Cell getCellAt(Sheet sheet, int rowNumber, int colNumber) {
        Row row = sheet.getRow(rowNumber);
        Cell cell = row.getCell(colNumber);
        return cell;
    }

    /**
	 * Returns true if the given cell is a "Check mark"
	 * 
	 * @param cell
	 *            The cell to analyse
	 * @return True if the given cell is a "Check mark"
	 */
    public boolean isCheckMark(Cell cell) {
        if (cell.getCellStyle() == null) {
            return false;
        }
        String format = cell.getCellStyle().getDataFormatString();
        return "[=1]\"�\";\"\"".equals(format);
    }

    /**
	 * This can be useful for reports and GUI presentations when you need to
	 * display data exactly as it appears in Excel. Supported formats include
	 * currency, SSN, percentages, decimals, dates, phone numbers, zip codes,
	 * etc.
	 * 
	 * @param cell
	 *            The POI cell instance to extract the value of
	 * @return The formatted data representation of cell value
	 */
    public String getCellValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellStyle().getHidden()) {
            return "";
        }
        String result = null;
        int cellType = cell.getCellType();
        switch(cellType) {
            case Cell.CELL_TYPE_BLANK:
                result = "";
                break;
            case Cell.CELL_TYPE_BOOLEAN:
                result = cell.getBooleanCellValue() ? "true" : "false";
                break;
            case Cell.CELL_TYPE_ERROR:
                result = "ERROR: " + cell.getErrorCellValue();
                break;
            case Cell.CELL_TYPE_FORMULA:
                if (isShowFormula()) {
                    result = cell.getCellFormula();
                } else {
                    result = getCachedCellFormulaResult(cell);
                }
                break;
            case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Format fmt = getNonNullCellFormat(cell);
                    result = fmt.format(cell.getDateCellValue());
                } else {
                    Format fmt = getNonNullCellFormat(cell);
                    result = fmt.format(cell.getNumericCellValue());
                }
                break;
            case Cell.CELL_TYPE_STRING:
                result = cell.getStringCellValue();
                break;
            default:
                break;
        }
        return result;
    }

    /**
	 * Use the CellFormat class for rendering cell value
	 * 
	 * @param cell
	 *            The cell we have to format value of
	 * @return The rendered value
	 */
    protected String getCellValueUsingPOIcellFormat(Cell cell) throws Exception {
        String result = "";
        CellFormat cf = CellFormat.getInstance(cell.getCellStyle().getDataFormatString());
        CellFormatResult cellFmtResult = cf.apply(cell);
        result = cellFmtResult.text;
        return result;
    }

    /**
	 * This can be useful for GUI editors when you need to edit data
	 * 
	 * @param cell
	 *            The POI cell instance to extract the value of
	 * @return The rough data representation of cell value
	 */
    public String getCellValueForEditMode(Cell cell) {
        if (cell == null) return "";
        String result = "";
        int cellType = cell.getCellType();
        switch(cellType) {
            case Cell.CELL_TYPE_BLANK:
                result = "";
                break;
            case Cell.CELL_TYPE_BOOLEAN:
            case Cell.CELL_TYPE_ERROR:
            case Cell.CELL_TYPE_FORMULA:
                result = cell.getCellFormula();
                break;
            case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    result = fmt.format(cell.getDateCellValue());
                    if (result.endsWith(" 00:00:00")) {
                        result = StringUtils.replace(result, " 00:00:00", "");
                    }
                } else {
                    DecimalFormatSymbols dfs = new DecimalFormatSymbols();
                    dfs.setDecimalSeparator('.');
                    NumberFormat fmt = new DecimalFormat("0.####################", dfs);
                    result = fmt.format(cell.getNumericCellValue());
                }
                break;
            case Cell.CELL_TYPE_STRING:
                result = cell.getStringCellValue();
                break;
            default:
                break;
        }
        return result;
    }

    /**
	 * This can be useful for reports and GUI presentations when you need to
	 * display data exactly as it appears in Excel. Supported formats include
	 * currency, SSN, percentages, decimals, dates, phone numbers, zip codes,
	 * etc.
	 * 
	 * @param cell
	 *            The POI cell instance to extract the value of
	 * @return The formatted data representation of cell value
	 */
    private String getCachedCellFormulaResult(Cell cell) {
        String result = null;
        try {
            switch(cell.getCachedFormulaResultType()) {
                case Cell.CELL_TYPE_BOOLEAN:
                    result = String.valueOf(cell.getBooleanCellValue());
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    Format fmt = getNonNullCellFormat(cell);
                    if (DateUtil.isCellDateFormatted(cell)) {
                        result = fmt.format(cell.getDateCellValue());
                    } else {
                        result = fmt.format(cell.getNumericCellValue());
                    }
                    break;
                case Cell.CELL_TYPE_STRING:
                    result = cell.getStringCellValue();
                    break;
                case Cell.CELL_TYPE_ERROR:
                    result = "#ERROR#";
                    break;
            }
        } catch (Exception e) {
            try {
                result = cell.getStringCellValue();
            } catch (IllegalStateException ex) {
                Format fmt = getNonNullCellFormat(cell);
                if (DateUtil.isCellDateFormatted(cell)) {
                    result = fmt.format(cell.getDateCellValue());
                } else {
                    result = fmt.format(cell.getNumericCellValue());
                }
            }
        }
        return result;
    }

    /**
	 * Return the POI cell object corresponding to a CellReference instance
	 * 
	 * @param cellRef
	 *            CellReference instance encapsulating cell location
	 * @return The POI cell object corresponding to a CellReference instance
	 */
    public Cell getCellAt(CellReference cellRef) {
        Sheet sheet = workbook.getSheet(cellRef.getSheetName());
        return getCellAt(sheet, cellRef.getRow(), cellRef.getCol());
    }

    /**
	 * Re-compute formula for the entire workbook
	 * 
	 * @param workbook
	 *            The POI workbook instance
	 * @return The errors map
	 */
    public Map<String, String> reEvaluateFormula(Workbook workbook) {
        evaluator.clearAllCachedResultValues();
        Map<String, String> errors = new HashMap<String, String>();
        for (int sheetNum = 0; sheetNum < workbook.getNumberOfSheets(); sheetNum++) {
            Sheet sheet = workbook.getSheetAt(sheetNum);
            for (Row r : sheet) {
                for (Cell c : r) {
                    if (c.getCellType() == Cell.CELL_TYPE_FORMULA) {
                        try {
                            evaluator.evaluateFormulaCell(c);
                        } catch (Exception e) {
                            errors.put(getCellId(c), c.getCellFormula() + " --> " + e.getClass().getSimpleName());
                        }
                    }
                }
            }
        }
        return errors;
    }

    public void removeEmptyWorksheets() {
        Set<Sheet> sheetsToRemove = new HashSet<Sheet>();
        for (int sheetNum = 0; sheetNum < workbook.getNumberOfSheets(); sheetNum++) {
            Sheet sheet = workbook.getSheetAt(sheetNum);
            try {
                sheet.getDefaultRowHeight();
                sheet.getDefaultColumnWidth();
            } catch (NullPointerException e) {
                sheetsToRemove.add(sheet);
                break;
            }
            boolean hasAtLeastOneCell = false;
            for (Row r : sheet) {
                if (!isEmptyRow(r)) {
                    hasAtLeastOneCell = true;
                    break;
                }
            }
            if (!hasAtLeastOneCell) {
                sheetsToRemove.add(sheet);
            }
        }
        try {
            for (Sheet sheetToRemove : sheetsToRemove) {
                workbook.removeSheetAt(workbook.getSheetIndex(sheetToRemove));
            }
        } catch (ClassCastException e) {
        }
    }

    /**
	 * Indicates if the given column is empty
	 * 
	 * @param sheet
	 *            The worksheet
	 * @param columnIdx
	 *            The column to scan
	 * @return True if all column cells are empty
	 */
    public boolean isEmptyColumn(Sheet sheet, int columnIdx) {
        int firstRowNum = sheet.getFirstRowNum();
        int lastRowNum = sheet.getLastRowNum();
        for (int i = firstRowNum; i <= lastRowNum; i++) {
            Row r = sheet.getRow(i);
            if (r == null) {
                continue;
            }
            Cell c = r.getCell(columnIdx);
            if (c != null) {
                if (StringUtils.isNotBlank(getCellValue(c))) {
                    return false;
                }
                CellStyle cellStyle = c.getCellStyle();
                if (cellStyle != null && !cellStyle.getHidden()) {
                    if (cellStyle.getBorderBottom() != CellStyle.BORDER_NONE) {
                        return false;
                    }
                    if (cellStyle.getBorderLeft() != CellStyle.BORDER_NONE) {
                        return false;
                    }
                    if (cellStyle.getBorderRight() != CellStyle.BORDER_NONE) {
                        return false;
                    }
                    if (cellStyle.getBorderTop() != CellStyle.BORDER_NONE) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
	 * Re-compute formula for the entire POI workbook
	 */
    public Map<String, String> reEvaluateFormula() {
        return reEvaluateFormula(workbook);
    }

    /**
	 * Output the workbook into a *.xls binary file
	 * 
	 * @param outputStream
	 *            The binary output stream (ServletOutputStream,
	 *            FileOutputStream, etc.)
	 * @throws IOException
	 */
    public void write(OutputStream outputStream) throws IOException {
        workbook.write(outputStream);
    }

    /**
	 * Excel binary content as ByteArrayOutputStream instance. Usefull if the
	 * bytes size must be known in advance (method
	 * ByteArrayOutputStream.size()). When a servlet return custom result to the
	 * browser we must know the content length:
	 * HttpServletResponse.setContentLength(baos.size());
	 * 
	 * @return The Excel binary content as ByteArrayOutputStream instance
	 * @throws IOException
	 */
    public ByteArrayOutputStream asOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        return baos;
    }

    /**
	 * For Apache POI direct API access
	 * 
	 * @return The POI workbook instance
	 */
    public Workbook getWorkbook() {
        return workbook;
    }

    /**
	 * Return the cell located at columnIndex - 1
	 * 
	 * @param cell
	 *            The POI cell
	 * @param createIfNone
	 *            If cell located at columnIndex - 1 doesn't exist and if this
	 *            flag is set to true then the cell is created
	 * @return The cell located at columnIndex - 1
	 */
    public Cell getCellAtLeft(Cell cell, boolean createIfNone) {
        if (cell.getColumnIndex() == 0) {
            return null;
        }
        Sheet sheet = cell.getSheet();
        Row row = sheet.getRow(cell.getRowIndex());
        Cell c = row.getCell(cell.getColumnIndex() - 1);
        if (createIfNone && c == null) {
            c = row.createCell(cell.getColumnIndex() - 1);
        }
        return c;
    }

    /**
	 * Return the cell located at columnIndex + 1
	 * 
	 * @param cell
	 *            The POI cell
	 * @param createIfNone
	 *            If cell located at columnIndex + 1 doesn't exist and if this
	 *            flag is set to true then the cell is created
	 * @return The cell located at columnIndex + 1
	 */
    public Cell getCellAtRight(Cell cell, boolean createIfNone) {
        Sheet sheet = cell.getSheet();
        Cell c = getCellAt(sheet, cell.getRowIndex(), cell.getColumnIndex() + 1);
        if (createIfNone && c == null) {
            c = cell.getRow().createCell(cell.getColumnIndex() + 1);
        }
        return c;
    }

    /**
	 * Return true if the cell located at columnIndex - 1 has a border line on
	 * it't right side
	 * 
	 * @param cell
	 *            The POI cell
	 * @return True if the cell located at columnIndex - 1 has a border line at
	 *         on it't right side
	 */
    public boolean isCellAtLeftHasRightBorder(Cell cell) {
        if (cell.getColumnIndex() == 0) {
            return false;
        }
        Sheet sheet = cell.getSheet();
        Row row = sheet.getRow(cell.getRowIndex());
        Cell cellAtLeft = row.getCell(cell.getColumnIndex() - 1);
        if (cellAtLeft == null) {
            return false;
        }
        CellStyle cellStyle = cellAtLeft.getCellStyle();
        if (cellStyle.getBorderRight() != CellStyle.BORDER_NONE) {
            return true;
        }
        return false;
    }

    /**
	 * Return true if the cell located at columnIndex + 1 has value
	 * 
	 * @param cell
	 *            The POI cell instance
	 * @return True if the cell located at columnIndex + 1 has value
	 */
    public boolean isCellAtRightHasValue(Cell cell) {
        Cell cellAtRight = getCellAtRight(cell, false);
        if (cellAtRight == null) {
            return false;
        } else {
            if (cellAtRight.getCellStyle().getHidden()) {
                return false;
            }
            if (cellAtRight.getCellType() == Cell.CELL_TYPE_BLANK) {
                return false;
            }
            return true;
        }
    }

    /**
	 * Return true if the cell located at columnIndex + 1 has a border on it's
	 * left side
	 * 
	 * @param cell
	 *            The POI cell instance
	 * @return True if the cell located at columnIndex + 1 has a border on it's
	 *         left side
	 */
    public boolean isCellAtRightHasBorder(Cell cell) {
        Cell cellAtRight = getCellAtRight(cell, false);
        if (cellAtRight == null) {
            return false;
        } else {
            if (cellAtRight.getCellStyle().getBorderLeft() != CellStyle.BORDER_NONE) {
                return true;
            }
            return false;
        }
    }

    /**
	 * Return the cell width in twips
	 * 
	 * @param cell
	 *            The POI cell instance
	 * @return The cell width in twips
	 */
    public int getWidthInTwips(Cell cell) {
        Sheet sheet = cell.getSheet();
        return sheet.getColumnWidth(cell.getColumnIndex());
    }

    /**
	 * Compute the width of the cell content (cell content may be larger than
	 * the cell width)
	 * 
	 * @param cell
	 *            The POI cell instance
	 * @return The width of the cell content (cell content may be larger than
	 *         the cell width)
	 * @throws Exception
	 */
    public int getContentWidthInTwips(Cell cell) throws Exception {
        if (cell.getCellStyle().getWrapText()) {
            return getWidthInTwips(cell);
        }
        CellStyle style = cell.getCellStyle();
        int cellType = cell.getCellType();
        if (Cell.CELL_TYPE_BLANK == cellType || Cell.CELL_TYPE_ERROR == cellType) {
            return 0;
        }
        String cellValue = getCellValue(cell);
        if (StringUtils.isEmpty(cellValue)) {
            return 0;
        }
        final Font font = getWorkbook().getFontAt(style.getFontIndex());
        int fontHeightInPt = font.getFontHeightInPoints();
        double fontHeightInTwips = ((double) fontHeightInPt / (double) 72 * (double) 1440);
        double contentWidthInTwips = fontHeightInTwips * (double) cellValue.trim().length();
        double hyphenationWidthInTwips = 0d;
        return (int) Math.round(contentWidthInTwips + hyphenationWidthInTwips);
    }

    /**
	 * Compute the width of the cell content (cell content may be larger than
	 * the cell width)
	 * 
	 * @param cell
	 *            The POI cell instance
	 * @return The width of the cell content (cell content may be larger than
	 *         the cell width)
	 * @throws Exception
	 */
    public int getContentWidthInTwipsComplex(Cell cell) throws Exception {
        if (cell.getCellStyle().getWrapText()) {
            return getWidthInTwips(cell);
        }
        Sheet sheet = cell.getRow().getSheet();
        int colIndex = 255;
        Row r = cell.getRow();
        if (r.getCell(colIndex) != null) {
            throw new IllegalStateException("Can't proceed for determining cell content width because cell at row " + r.getRowNum() + " and col " + colIndex + " already exists !");
        }
        Cell newCell = r.createCell(colIndex);
        newCell.setCellValue(getCellValue(cell));
        newCell.setCellStyle(cell.getCellStyle());
        int columnWidthInTwips = sheet.getColumnWidth(colIndex);
        sheet.autoSizeColumn(colIndex);
        int newColumnWidthInTwips = sheet.getColumnWidth(colIndex);
        sheet.setColumnWidth(colIndex, columnWidthInTwips);
        r.removeCell(newCell);
        return newColumnWidthInTwips;
    }

    /**
	 * Indicates if the cell located at rowIndex - 1 has a bottom border line
	 * 
	 * @param cell
	 *            The POI cell instance
	 * @return True if the cell located at rowIndex - 1 has a bottom border line
	 */
    public boolean isCellAboveHasBottomBorder(Cell cell) {
        if (cell.getRowIndex() == 0) {
            return false;
        }
        Sheet sheet = cell.getSheet();
        Row rowAbove = sheet.getRow(cell.getRowIndex() - 1);
        if (rowAbove == null) {
            return false;
        }
        Cell cellAbove = rowAbove.getCell(cell.getColumnIndex());
        if (cellAbove == null) {
            return false;
        }
        CellStyle cellStyle = cellAbove.getCellStyle();
        if (cellStyle.getBorderBottom() != CellStyle.BORDER_NONE) {
            return true;
        }
        return false;
    }

    /**
	 * Return true if the color of a cell has been specified
	 * 
	 * @param cell
	 *            The POI cell
	 * @return True if the color of the cell has been specified
	 */
    public abstract boolean isColorModified(Cell cell);

    /**
	 * Update cell value
	 * 
	 * @param cell
	 *            The POI cell instace
	 * @param valueAsString
	 *            The new value
	 * @throws ParseException
	 */
    public void setCellValue(Cell cell, String valueAsString) throws ParseException {
        if (cell == null) return;
        int cellType = cell.getCellType();
        switch(cellType) {
            case Cell.CELL_TYPE_BLANK:
                if (StringUtils.isNumeric(valueAsString)) {
                    cell.setCellValue(Double.parseDouble(valueAsString));
                } else {
                    cell.setCellValue(valueAsString);
                }
                break;
            case Cell.CELL_TYPE_BOOLEAN:
                Boolean value = Boolean.parseBoolean(valueAsString);
                cell.setCellValue(value);
                break;
            case Cell.CELL_TYPE_ERROR:
                break;
            case Cell.CELL_TYPE_FORMULA:
                cell.setCellFormula(valueAsString);
                break;
            case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    DateFormat fmt = (DateFormat) getNonNullCellFormat(cell);
                    Date dt = fmt.parse(valueAsString);
                    cell.setCellValue(dt);
                } else {
                    if (StringUtils.isBlank(valueAsString)) {
                        cell.setCellValue("");
                    } else {
                        if (isCheckMark(cell)) {
                            cell.setCellValue(Double.parseDouble(valueAsString));
                        } else {
                            DecimalFormat fmt = (DecimalFormat) getNonNullCellFormat(cell);
                            cell.setCellValue(fmt.parse(valueAsString).doubleValue());
                        }
                    }
                }
                break;
            case Cell.CELL_TYPE_STRING:
                if (StringUtils.isNumeric(valueAsString)) {
                    cell.setCellValue(Double.parseDouble(valueAsString));
                } else {
                    cell.setCellValue(valueAsString);
                }
                break;
            default:
                break;
        }
    }

    /**
	 * Return the ID of a cell. Ex: "0_A_6" = cell A6 of sheet 0 (first
	 * worksheet of the workbook)
	 * 
	 * @param cell
	 *            The POI cell instance
	 * @return The ID of a cell. Ex: "0_A_6" = cell A6 of sheet 0 (first
	 *         worksheet of the workbook)
	 */
    public String getCellId(Cell cell) {
        CellReference cellRef = new CellReference(cell.getRowIndex(), cell.getColumnIndex());
        String[] parts = cellRef.getCellRefParts();
        return getSheetIndex(cell) + "_" + parts[2] + "_" + parts[1];
    }

    /**
	 * Return the sheet index of the cell
	 * 
	 * @param cell
	 *            The POI cell instance
	 * @return The sheet index of the cell
	 */
    public int getSheetIndex(Cell cell) {
        return workbook.getSheetIndex(cell.getSheet());
    }

    /**
	 * Determine if a given cell has a numeric value
	 * 
	 * @param cell
	 *            The POI cell instance
	 * @return True if the given cell has a numeric value
	 */
    public boolean isNumericCell(Cell cell) {
        int cellType = cell.getCellType();
        switch(cellType) {
            case Cell.CELL_TYPE_BLANK:
                return false;
            case Cell.CELL_TYPE_BOOLEAN:
                return false;
            case Cell.CELL_TYPE_ERROR:
                return true;
            case Cell.CELL_TYPE_FORMULA:
                try {
                    cell.getStringCellValue();
                    return false;
                } catch (IllegalStateException e) {
                    return true;
                }
            case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return false;
                } else {
                    return true;
                }
            case Cell.CELL_TYPE_STRING:
                return false;
            default:
                return false;
        }
    }

    /**
	 * Return the number of rows and columns span if the given cell is the top
	 * left corner of a merged region, orherwise return null
	 * 
	 * @param cell
	 *            The worksheet cell
	 * @return Return the number of rows and columns span if the given cell is
	 *         the top left corner of a merged region, orherwise return null
	 */
    public CellSpanInfos getSpanInfosOfMergedRegion(Cell cell) {
        Sheet sheet = cell.getSheet();
        int nbMergedRegions = sheet.getNumMergedRegions();
        for (int i = 0; i < nbMergedRegions; i++) {
            CellRangeAddress cellRangeAddr = sheet.getMergedRegion(i);
            if (cellRangeAddr.getFirstColumn() == cell.getColumnIndex()) {
                if (cellRangeAddr.getFirstRow() == cell.getRowIndex()) {
                    int colspan = cellRangeAddr.getLastColumn() - cellRangeAddr.getFirstColumn() + 1;
                    int rowspan = cellRangeAddr.getLastRow() - cellRangeAddr.getFirstRow() + 1;
                    return new CellSpanInfos(colspan, rowspan);
                }
            }
        }
        return null;
    }

    /**
	 * Tell if a given cell is included inside a merged region
	 * 
	 * @param c
	 *            The worksheet cell column index
	 * @param r
	 *            The worksheet cell row index
	 * @param sheet
	 *            The worksheet
	 * @return True if the given cell is included inside a merged region
	 */
    public boolean isPartOfMergedRegion(int c, int r, Sheet sheet) {
        return getMergedRegion(c, r, sheet) != null;
    }

    /**
	 * Get the corresponding merged region of a given cell (if any)
	 * 
	 * @param c
	 *            The worksheet cell column index
	 * @param r
	 *            The worksheet cell row index
	 * @param sheet
	 *            The worksheet
	 * @return The corresponding merged region of a given cell (if any)
	 */
    public CellRangeAddress getMergedRegion(int c, int r, Sheet sheet) {
        int nbMergedRegions = sheet.getNumMergedRegions();
        for (int i = 0; i < nbMergedRegions; i++) {
            CellRangeAddress cellRangeAddr = sheet.getMergedRegion(i);
            if (c >= cellRangeAddr.getFirstColumn() && c <= cellRangeAddr.getLastColumn()) {
                if (r >= cellRangeAddr.getFirstRow() && r <= cellRangeAddr.getLastRow()) {
                    return cellRangeAddr;
                }
            }
        }
        return null;
    }

    /**
	 * Tell if a given cell is included inside a merged region
	 * 
	 * @param cell
	 *            The worksheet cell to analyse
	 * @return True if the given cell is included inside a merged region
	 */
    public boolean isPartOfMergedRegion(Cell cell) {
        int c = cell.getColumnIndex();
        int r = cell.getRowIndex();
        Sheet sheet = cell.getSheet();
        return isPartOfMergedRegion(c, r, sheet);
    }

    /**
	 * Retrieve the bottom left cell of the merged region (if any)
	 * 
	 * @param cell
	 *            The worksheet cell to analyse
	 * @return The bottom left cell of the merged region (if any)
	 */
    public Cell getBottomLeftCornerOfMergedRegion(Cell cell) {
        Sheet sheet = cell.getSheet();
        int c = cell.getColumnIndex();
        int r = cell.getRowIndex();
        CellRangeAddress cellRangeAddr = getMergedRegion(c, r, sheet);
        if (cellRangeAddr == null) {
            return null;
        }
        int colNumber = cellRangeAddr.getFirstColumn();
        int rowNumber = cellRangeAddr.getLastRow();
        return getCellAt(sheet, rowNumber, colNumber);
    }

    /**
	 * Retrieve the top right cell of the merged region (if any)
	 * 
	 * @param cell
	 *            The worksheet cell to analyse
	 * @return The top right cell of the merged region (if any)
	 */
    public Cell getTopRightCornerOfMergedRegion(Cell cell) {
        Sheet sheet = cell.getSheet();
        int c = cell.getColumnIndex();
        int r = cell.getRowIndex();
        CellRangeAddress cellRangeAddr = getMergedRegion(c, r, sheet);
        if (cellRangeAddr == null) {
            return null;
        }
        int colNumber = cellRangeAddr.getLastColumn();
        int rowNumber = cellRangeAddr.getFirstRow();
        return getCellAt(sheet, rowNumber, colNumber);
    }

    /**
	 * Tell if a given cell can be ignored, being internal part of a merged
	 * cells region
	 * 
	 * @param cell
	 *            The worksheet cell to analyse
	 * @return True if a given cell can be ignored, being internal part of a
	 *         merged cells region
	 */
    public boolean isForgettable(Cell cell) {
        int c = cell.getColumnIndex();
        int r = cell.getRowIndex();
        Sheet sheet = cell.getSheet();
        return isForgettable(c, r, sheet);
    }

    /**
	 * Tell if a given cell can be ignored, being internal part of a merged
	 * cells region
	 * 
	 * @param c
	 *            The worksheet cell column index
	 * @param r
	 *            The worksheet cell row index
	 * @param sheet
	 *            The worksheet
	 * @return True if a given cell can be ignored, being internal part of a
	 *         merged cells region
	 */
    public boolean isForgettable(int c, int r, Sheet sheet) {
        int nbMergedRegions = sheet.getNumMergedRegions();
        for (int i = 0; i < nbMergedRegions; i++) {
            CellRangeAddress cellRangeAddr = sheet.getMergedRegion(i);
            if (cellRangeAddr.isInRange(r, c)) {
                if (c != cellRangeAddr.getFirstColumn() || r != cellRangeAddr.getFirstRow()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isShowFormula() {
        return showFormula;
    }

    public void setShowFormula(boolean showFormula) {
        this.showFormula = showFormula;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    /**
	 * Return the cell format. Note: format configurations are defined inside
	 * WorkbookTag <b><i>workbook-tag-number-formats.properties</i></b> and
	 * <b><i>workbook-tag-date-formats.properties</i></b> files in order to
	 * match Excel formats to Java formats
	 * 
	 * @param cell
	 *            The POI cell instance
	 * @return The cell format (never null)
	 */
    public Format getNonNullCellFormat(Cell cell) {
        Format fmt = util.getFormat(cell);
        if (fmt == null) {
            if (DateUtil.isCellDateFormatted(cell)) {
                fmt = new SimpleDateFormat("yyyy-MM-dd");
            } else {
                DecimalFormatSymbols decSymbol = new DecimalFormatSymbols();
                decSymbol.setDecimalSeparator('.');
                fmt = new DecimalFormat("#.##", decSymbol);
            }
        }
        return fmt;
    }

    /**
	 * Setter for the POI workbook object. Usefull for unit tests.
	 * 
	 * @param workbook
	 *            The POI workbook
	 */
    protected void setWorkbook(Workbook w) {
        this.workbook = w;
        this.evaluator = w.getCreationHelper().createFormulaEvaluator();
        this.util = new DataFormatterUtil(false);
    }

    /**
	 * Get the POI workbook object as bytes array
	 * 
	 * @return The bytes of the POI workbook
	 * @throws IOException
	 */
    private byte[] getWorkbookAsBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        out.flush();
        out.close();
        return out.toByteArray();
    }

    /**
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeBoolean(showFormula);
        out.writeObject(originalFilename);
        byte[] wkbBytes = getWorkbookAsBytes();
        out.writeInt(wkbBytes.length);
        out.write(wkbBytes);
    }

    /**
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        showFormula = in.readBoolean();
        originalFilename = (String) in.readObject();
        byte[] bytes = new byte[in.readInt()];
        in.readFully(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        loadWorkbook(bais);
    }

    /**
	 * Indicates if the given sheet has conditional formating
	 * 
	 * @param sheet
	 *            The sheet to analyse
	 * @return True if the given sheet has conditional formating
	 */
    public abstract boolean hasConditionnalFormatting(Sheet sheet);

    public abstract int conditionnalFormattingBackgroundIdx(Cell cell);

    /**
	 * Return the color of the given entity
	 * 
	 * @return The color of the given entity
	 */
    public abstract RGBColorInfo getColor(CellStyle cellStyle, EntityEnum entity);

    /**
	 * Return the color of the given entity
	 * 
	 * @return The color of the given entity
	 */
    public abstract RGBColorInfo getColor(Font font);

    /**
	 * Get the editable cells signature. Allows to determine if changes occured.
	 */
    public String getUnlockedCellsSignature() {
        StringBuilder sb = new StringBuilder();
        for (int sheetNum = 0; sheetNum < workbook.getNumberOfSheets(); sheetNum++) {
            Sheet sheet = workbook.getSheetAt(sheetNum);
            for (Row r : sheet) {
                for (Cell c : r) {
                    if (!c.getCellStyle().getLocked()) {
                        String id = getCellId(c);
                        String val = getCellValue(c);
                        sb.append(id).append("=").append(val).append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
	 * From the cell ID return the corresponding CellReference instance
	 * 
	 * @param cellId
	 *            Identifier 'sheetIndex_ColumnLetter_RowNumber'. Ex: "0_H_18" =
	 *            cell H18 of sheet 0 (first worksheet of the workbook, zero
	 *            base)
	 * @return The CellReference instance corresponding to the identifier
	 */
    public CellReference getCellReferenceFromFieldName(String cellId) {
        String[] parts = StringUtils.split(cellId, "_");
        if (parts.length != 3) {
            return null;
        }
        int sheetIndex = Integer.parseInt(parts[0]);
        String sheetName = workbook.getSheetName(sheetIndex);
        CellReference cellRef = new CellReference(sheetName + "!" + parts[1] + parts[2]);
        return cellRef;
    }

    /**
	 * Write the POI workbook into the specified output stream
	 * 
	 * @param out
	 *            The output stream
	 * @throws IOException
	 */
    public void writeTo(OutputStream out) throws IOException {
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(out);
        workbook.write(bufferedOutputStream);
        bufferedOutputStream.flush();
        bufferedOutputStream.close();
    }

    /**
	 * Write the POI workbook into the specified file
	 * 
	 * @param f
	 *            The output file
	 * @throws IOException
	 */
    public void writeTo(File f) throws IOException {
        writeTo(new FileOutputStream(f));
    }

    /**
	 * Tell if a given row is empty or not
	 * 
	 * @param r
	 *            The row to scan for a value
	 * @return True if the entire row is empty
	 */
    public boolean isEmptyRow(Row r) {
        if (r == null) {
            return true;
        }
        for (Cell c : r) {
            if (c != null) {
                if (StringUtils.isNotBlank(getCellValue(c))) {
                    return false;
                }
                CellStyle cellStyle = c.getCellStyle();
                if (cellStyle != null && !cellStyle.getHidden()) {
                    if (cellStyle.getBorderBottom() != CellStyle.BORDER_NONE) {
                        return false;
                    }
                    if (cellStyle.getBorderLeft() != CellStyle.BORDER_NONE) {
                        return false;
                    }
                    if (cellStyle.getBorderRight() != CellStyle.BORDER_NONE) {
                        return false;
                    }
                    if (cellStyle.getBorderTop() != CellStyle.BORDER_NONE) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public Cell getTopLeftCorner(CellRangeAddress range, int sheetNo) {
        Sheet sheet = workbook.getSheetAt(sheetNo);
        int minCol = Math.min(range.getFirstColumn(), range.getLastColumn());
        int minRow = Math.min(range.getFirstRow(), range.getLastRow());
        return getCellAt(sheet, minRow, minCol);
    }

    public Cell getBottomRightCorner(CellRangeAddress range, int sheetNo) {
        Sheet sheet = workbook.getSheetAt(sheetNo);
        int maxCol = Math.max(range.getFirstColumn(), range.getLastColumn());
        int maxRow = Math.max(range.getFirstRow(), range.getLastRow());
        return getCellAt(sheet, maxRow, maxCol);
    }

    /**
	 * Add rows at the very end of the worksheet
	 * 
	 * @param sheetNo
	 *            The sheer number
	 * @param nbRows
	 *            Number of rows to add
	 */
    public void addRows(int sheetNo, int nbRows) {
        Sheet sheet = workbook.getSheetAt(sheetNo);
        for (int i = 0; i < nbRows; i++) {
            sheet.createRow(sheet.getLastRowNum() + 1);
        }
    }

    /**
	 * Add columns at the very end of the worksheet
	 * 
	 * @param sheetNo
	 *            The sheer number
	 * @param nbCols
	 *            Number of columns to add
	 */
    public void addCols(int sheetNo, int nbCols) {
        Sheet sheet = workbook.getSheetAt(sheetNo);
        Row row = sheet.getRow(sheet.getFirstRowNum());
        for (int i = 0; i < nbCols; i++) {
            row.createCell(row.getLastCellNum());
        }
    }

    /**
	 * Add columns inside the worksheet
	 * 
	 * @param sheetNo
	 *            The sheet number
	 * @param nbCols
	 *            Number of columns to add
	 * @param startCol
	 *            Column index to start at
	 */
    public abstract void insertCols(int sheetNo, int nbCols, int startCol);

    /**
	 * Add rows at the very end of the worksheet
	 * 
	 * @param sheetNo
	 *            The sheer number
	 * @param nbRows
	 *            Number of rows to add
	 */
    public void insertRows(int sheetNo, int nbRows, int startRow) {
        Sheet sheet = workbook.getSheetAt(sheetNo);
        sheet.shiftRows(startRow, sheet.getLastRowNum(), nbRows);
        sheet.createRow(startRow);
    }

    public abstract void updateFormulasAfterRowCopy(Sheet sheet, int rowNum, int nbRows);
}
