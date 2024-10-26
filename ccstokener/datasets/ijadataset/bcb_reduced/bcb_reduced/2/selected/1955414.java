package net.sf.bacchus.integration.spreadsheet;

import java.beans.IntrospectionException;
import java.beans.PropertyEditor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.sf.bacchus.CompanyBatch;
import net.sf.bacchus.File;
import net.sf.bacchus.InvalidRecordException;
import net.sf.bacchus.Record;
import net.sf.bacchus.naming.IntrospectionNamingStrategy;
import net.sf.bacchus.naming.NamingStrategy;
import net.sf.bacchus.record.CompanyNamingStrategy;
import net.sf.bacchus.record.FileOriginNamingStrategy;
import net.sf.bacchus.record.process.TypedRecordDispatcher;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.core.io.Resource;

/**
 * Generates spreadsheets from ACH files. Each file produces a workbook, each
 * company/batch produces a sheet in the workbook, and each entry (detail +
 * addenda) produces a row in the sheet.
 */
public class SpreadsheetCreator<D, A> extends TypedRecordDispatcher<File, CompanyBatch, D, A, Object, Object, Object> {

    /** the font height used in the cell styles. */
    public static final short FONT_HEIGHT_IN_POINTS = 10;

    /** the format for integer cells. */
    public static final String INTEGER_FORMAT = "0";

    /** the format for decimal cells, which are assumed to be amounts. */
    public static final String AMOUNT_FORMAT = "$#,##0.00;[Red]($#,##0.00)";

    /** the format for {@link Date} and {@link Calendar} cells. */
    public static final String DATE_FORMAT = "m/d/yy";

    /** the source of new workbooks. */
    private WorkbookFactory workbookFactory;

    /** the naming strategy for workbooks. */
    private NamingStrategy<? super File> workbookNamingStrategy;

    /** the naming strategy for sheets in the workbook. */
    private NamingStrategy<? super CompanyBatch> sheetNamingStrategy;

    /** the naming strategy for display headers, keyed by property names. */
    private NamingStrategy<String> headerNamingStrategy;

    /** the properties of detail records to convert into into columns. */
    private String[] detailColumns = new String[0];

    /** the properties of addenda records to convert into into columns. */
    private String[] addendaColumns = new String[0];

    /**
     * cache of styles for property value types
     * @see #getStyleFor(PropertyAccessor, String)
     */
    private Map<Class<?>, CellStyle> styles = new HashMap<Class<?>, CellStyle>();

    /**
     * the custom property editors to apply to records before writing to
     * spreadsheet.
     */
    private PropertyEditorRegistrar propertyEditorRegistrar;

    /** the base location for workbook output streams. */
    private Resource output;

    /** the current workbook. */
    private Workbook currentWorkbook;

    /** the name for the current workbook. */
    private String currentWorkbookName;

    /**
     * the cell style for strings in the current workbook. It is "current"
     * because the cell styles have to be re-defined for each new workbook. This
     * style is also used as the basis for the other cell styles.
     */
    private CellStyle currentStringStyle;

    /** the cell style for integers in the current workbook. */
    private CellStyle currentIntegerStyle;

    /**
     * the cell style for amounts (all decimal and floating point types) in the
     * current workbook.
     */
    private CellStyle currentAmountStyle;

    /** the cell style for dates in the current workbook. */
    private CellStyle currentDateStyle;

    /** the cell style for header cells in the current workbook. */
    private CellStyle currentHeaderStyle;

    /**
     * Creates a spreadsheet creator with introspected column headers, a default
     * workbook factory, workbook naming strategy and sheet naming strategy.
     * @param detail the expected type for a {@link Record#DETAIL_RECORD}.
     * @param addenda the expected type for a {@link Record#ADDENDA_RECORD}.
     * @throws IntrospectionException if an exception is thrown while
     * introspecting the detail and addenda classes for display names.
     * @see HSSFFactory
     * @see FileOriginNamingStrategy
     * @see CompanyNamingStrategy
     */
    public SpreadsheetCreator(final Class<D> detail, Class<A> addenda) throws IntrospectionException {
        this(new HSSFFactory(), new FileOriginNamingStrategy(), new CompanyNamingStrategy(), new IntrospectionNamingStrategy(new Class<?>[] { detail, addenda }), detail, addenda);
    }

    /**
     * Creates a spreadsheet creator.
     * @param workbookFactory the source of new workbooks.
     * @param workbookNamingStrategy the naming strategy for workbook files.
     * @param sheetNamingStrategy the naming strategy for sheets in the
     * workbook.
     * @param headerNamingStrategy the naming strategy for column headers. If
     * this value is {@code null}, no columns headers will be generated.
     * @param detail the expected type for a {@link Record#DETAIL_RECORD}.
     * @param addenda the expected type for a {@link Record#ADDENDA_RECORD}.
     */
    public SpreadsheetCreator(final WorkbookFactory workbookFactory, final NamingStrategy<? super File> workbookNamingStrategy, final NamingStrategy<? super CompanyBatch> sheetNamingStrategy, final NamingStrategy<String> headerNamingStrategy, final Class<D> detail, final Class<A> addenda) {
        super(File.class, CompanyBatch.class, detail, addenda, Object.class, Object.class, Object.class, true);
        this.workbookFactory = workbookFactory;
        this.workbookNamingStrategy = workbookNamingStrategy;
        this.sheetNamingStrategy = sheetNamingStrategy;
        this.headerNamingStrategy = headerNamingStrategy;
    }

    /**
     * Duplicates the entries in a source array to a destination array. If
     * {@code source} is {@code null}, returns a zero-length array.
     * @param source the source to copy.
     * @return a duplicate of the source array.
     */
    private static final String[] copy(final String[] source) {
        final String[] destination;
        if (source == null || source.length == 0) destination = new String[0]; else {
            destination = new String[source.length];
            System.arraycopy(source, 0, destination, 0, source.length);
        }
        return destination;
    }

    /**
     * Sets the properties of detail records to convert into into columns.
     * @param columns the properties of detail records to convert into into
     * columns.
     */
    public void setDetailColumns(final String... columns) {
        this.detailColumns = copy(columns);
    }

    /**
     * Sets the properties of addenda records to convert into into columns.
     * @param columns the properties of addenda records to convert into into
     * columns.
     */
    public void setAddendaColumns(final String... columns) {
        this.addendaColumns = copy(columns);
    }

    /**
     * Sets the source of new workbooks.
     * @param workbookFactory the source of new workbooks.
     */
    public void setWorkbookFactory(final WorkbookFactory workbookFactory) {
        this.workbookFactory = workbookFactory;
    }

    /**
     * Sets the naming strategy for workbook output streams. If this value is
     * {@code null}, the output resource is used directly.
     * @param workbookNamingStrategy the naming strategy for workbook files.
     */
    public void setWorkbookNamingStrategy(final NamingStrategy<? super File> workbookNamingStrategy) {
        this.workbookNamingStrategy = workbookNamingStrategy;
    }

    /**
     * Sets the naming strategy for sheets in the workbook.
     * @param sheetNamingStrategy the naming strategy for sheets in the
     * workbook.
     */
    public void setSheetNamingStrategy(final NamingStrategy<? super CompanyBatch> sheetNamingStrategy) {
        this.sheetNamingStrategy = sheetNamingStrategy;
    }

    /**
     * Sets the naming strategy for column headers.
     * @param headerNamingStrategy the naming strategy for column headers.
     */
    public void setHeaderNamingStrategy(final NamingStrategy<String> headerNamingStrategy) {
        this.headerNamingStrategy = headerNamingStrategy;
    }

    /**
     * Sets the base location for workbook output streams.
     * @param output the base location for workbook output streams.
     */
    public void setOutput(final Resource output) {
        this.output = output;
    }

    /**
     * Sets custom property editors to apply to records before writing to
     * spreadsheet.
     * @param propertyEditorRegistrar the source of custom property editors.
     */
    public void setPropertyEditorRegistrar(final PropertyEditorRegistrar propertyEditorRegistrar) {
        this.propertyEditorRegistrar = propertyEditorRegistrar;
    }

    /**
     * Starts a new workbook.
     * @param header {@inheritDoc}
     */
    @Override
    protected void handleFileHeader(final File header) throws InvalidRecordException {
        startWorkbook();
        this.currentWorkbookName = this.workbookNamingStrategy == null ? null : this.workbookNamingStrategy.name(header);
    }

    /**
     * Starts a new sheet in the current workbook and makes it the active sheet.
     * @param header {@inheritDoc}
     */
    @Override
    protected void handleCompanyBatchHeader(final CompanyBatch header) throws InvalidRecordException {
        if (this.currentWorkbook == null) startWorkbook();
        final Sheet sheet = this.currentWorkbook.createSheet(this.sheetNamingStrategy.name(header));
        this.currentWorkbook.setActiveSheet(this.currentWorkbook.getSheetIndex(sheet));
    }

    /**
     * Adds a detail row to the active sheet. If this is the first row of the
     * sheet and headers are defined, adds the header row. If necessary, a new
     * sheet or a new workbook will be created.
     * @param detail {@inheritDoc}
     */
    @Override
    protected void handleDetail(final D detail) throws InvalidRecordException {
        final Sheet sheet = startSheetIfNecessary();
        final int last = sheet.getLastRowNum();
        final int rownum = last == 0 && sheet.getRow(0) == null ? 0 : last + 1;
        final Row row = sheet.createRow(rownum);
        addCellsToRow(row, 0, detail, this.detailColumns);
    }

    /**
     * Appends addenda data to the last row of the current sheet.
     * @param addenda {@inheritDoc}
     */
    @Override
    protected void handleAddenda(final A addenda) throws InvalidRecordException {
        final Sheet sheet = startSheetIfNecessary();
        final int rownum = sheet.getLastRowNum();
        Row row = sheet.getRow(rownum);
        if (rownum == 0) {
            if (row == null) row = sheet.createRow(0); else if (this.headerNamingStrategy != null) row = sheet.createRow(1);
        }
        final int startColumn = this.detailColumns == null ? 0 : this.detailColumns.length;
        addCellsToRow(row, startColumn, addenda, this.addendaColumns);
    }

    /**
     * Writes the current workbook to a resource, if one has been defined.
     * @param control {@inheritDoc}
     */
    @Override
    protected void handleFileControl(final Object control) throws InvalidRecordException {
        if (this.output != null) try {
            final Resource resource = this.currentWorkbookName == null ? this.output : this.output.createRelative(this.currentWorkbookName);
            final OutputStream target = outputTo(resource);
            this.currentWorkbook.write(target);
            target.close();
        } catch (final IOException e) {
            throw new RuntimeException("Unable to write workbook", e);
        }
        this.currentWorkbook = null;
    }

    /**
     * Gets a cell style applicable to a cell that represents the property. The
     * default implementation chooses solely based on the type of property.
     * @param accessor accessor for the bean being processed.
     * @param name the name of the property.
     * @return the cell style for the property.
     */
    protected CellStyle getStyleFor(final PropertyAccessor accessor, final String name) {
        final Class<?> type = accessor.getPropertyType(name);
        CellStyle style = this.styles.get(type);
        if (style == null) {
            if (BigDecimal.class.isAssignableFrom(type) || Double.TYPE.isAssignableFrom(type) || Double.class.isAssignableFrom(type) || Float.TYPE.isAssignableFrom(type) || Float.class.isAssignableFrom(type)) style = this.currentAmountStyle; else if (Number.class.isAssignableFrom(type) || Byte.TYPE.isAssignableFrom(type) || Integer.TYPE.isAssignableFrom(type) || Long.TYPE.isAssignableFrom(type)) style = this.currentIntegerStyle; else if (Date.class.isAssignableFrom(type) || Calendar.class.isAssignableFrom(type)) style = this.currentDateStyle; else style = this.currentStringStyle;
            if (style != null) this.styles.put(type, style);
        }
        return style;
    }

    /**
     * Gets a property accessor for a record. The default implementation
     * delegates to
     * {@link PropertyAccessorFactory#forBeanPropertyAccess(Object)}.
     * @param record the record to access.
     * @return a property accessor for the input record.
     */
    protected ConfigurablePropertyAccessor getAccessorFor(final Object record) {
        return PropertyAccessorFactory.forBeanPropertyAccess(record);
    }

    /**
     * Sets the type, value and style of a data cell for each property value.
     * @param row the row to populate.
     * @param firstColumn the column number to start at.
     * @param record the record to populate into the row.
     * @param properties the names of the properties to populate into cells.
     */
    protected void addCellsToRow(final Row row, final int firstColumn, final Object record, final String[] properties) {
        final ConfigurablePropertyAccessor wrapper = getAccessorFor(record);
        boolean registered = this.propertyEditorRegistrar == null;
        for (int i = 0; i < properties.length; i++) {
            final Cell cell = row.createCell(firstColumn + i);
            cell.setCellStyle(getStyleFor(wrapper, properties[i]));
            final Object value = wrapper.getPropertyValue(properties[i]);
            if (value == null) cell.setCellType(Cell.CELL_TYPE_BLANK); else if (value instanceof Number) cell.setCellValue(((Number) value).doubleValue()); else if (value instanceof Date) cell.setCellValue((Date) value); else if (value instanceof Calendar) cell.setCellValue((Calendar) value); else if (value instanceof Boolean) cell.setCellValue((Boolean) value); else if (value instanceof RichTextString) cell.setCellValue((RichTextString) value); else {
                if (!registered) {
                    this.propertyEditorRegistrar.registerCustomEditors(wrapper);
                    registered = true;
                }
                final PropertyEditor editor = wrapper.findCustomEditor(wrapper.getPropertyType(properties[i]), properties[i]);
                if (editor == null) cell.setCellValue(value.toString()); else {
                    editor.setValue(value);
                    cell.setCellValue(editor.getAsText());
                }
            }
        }
    }

    /**
     * Produces an output stream that writes to a {@link Resource}.
     * @param target the target resource.
     * @return an output stream to the resource, or {@code null} if one cannot
     * be created.
     */
    protected OutputStream outputTo(final Resource target) {
        try {
            final java.io.File file = target.getFile();
            if (file != null) return new FileOutputStream(file);
        } catch (final IOException e) {
        }
        try {
            final URL url = target.getURL();
            if (url != null) {
                final URLConnection connection = url.openConnection();
                connection.setDoOutput(true);
                return connection.getOutputStream();
            }
        } catch (final IOException e) {
        }
        return null;
    }

    /**
     * Appends header columns to a row.
     * @param row the header row.
     * @param offset the first column.
     * @param columns the headers to append.
     */
    private void appendHeaders(final Row row, final int offset, final String... columns) {
        if (columns == null || columns.length == 0) return;
        for (int i = 0; i < columns.length; i++) {
            final Cell header = row.createCell(i + offset);
            header.setCellValue(this.headerNamingStrategy.name(columns[i]));
            header.setCellStyle(this.currentHeaderStyle);
        }
    }

    /**
     * Gets the current sheet of the current workbook. If necessary, a new
     * workbook will be started.
     * @return the current sheet of the current workbook
     * @see #startWorkbook()
     */
    private Sheet startSheetIfNecessary() {
        if (this.currentWorkbook == null) startWorkbook();
        Sheet sheet = this.currentWorkbook.getNumberOfSheets() == 0 ? null : this.currentWorkbook.getSheetAt(this.currentWorkbook.getActiveSheetIndex());
        if (sheet == null) sheet = this.currentWorkbook.createSheet(this.sheetNamingStrategy.name(null));
        if (this.headerNamingStrategy != null && sheet.getLastRowNum() == 0 && sheet.getRow(0) == null) {
            sheet.setDisplayRowColHeadings(true);
            final Row headerRow = sheet.createRow(0);
            appendHeaders(headerRow, 0, this.detailColumns);
            appendHeaders(headerRow, this.detailColumns == null ? 0 : this.detailColumns.length, this.addendaColumns);
        }
        return sheet;
    }

    /**
     * Starts a new workbook by creating one using the {@link #workbookFactory}
     * and adding cell style definitions.
     * @see #currentWorkbook
     * @see #currentStringStyle
     * @see #currentIntegerStyle
     * @see #currentAmountStyle
     * @see #currentDateStyle
     * @see #currentHeaderStyle
     */
    private void startWorkbook() {
        this.currentWorkbook = this.workbookFactory.create();
        this.styles.clear();
        final Font font = this.currentWorkbook.createFont();
        final Font headerFont = this.currentWorkbook.createFont();
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        this.currentStringStyle = this.currentWorkbook.createCellStyle();
        this.currentStringStyle.setFont(font);
        this.currentStringStyle.setAlignment(CellStyle.ALIGN_GENERAL);
        this.currentHeaderStyle = this.currentWorkbook.createCellStyle();
        this.currentHeaderStyle.cloneStyleFrom(this.currentStringStyle);
        this.currentHeaderStyle.setFont(headerFont);
        this.currentHeaderStyle.setAlignment(CellStyle.ALIGN_CENTER);
        this.currentHeaderStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        this.currentIntegerStyle = this.currentWorkbook.createCellStyle();
        this.currentIntegerStyle.cloneStyleFrom(this.currentStringStyle);
        this.currentIntegerStyle.setAlignment(CellStyle.ALIGN_RIGHT);
        this.currentIntegerStyle.setDataFormat(this.currentWorkbook.createDataFormat().getFormat(INTEGER_FORMAT));
        this.currentAmountStyle = this.currentWorkbook.createCellStyle();
        this.currentAmountStyle.cloneStyleFrom(this.currentIntegerStyle);
        this.currentAmountStyle.setDataFormat(this.currentWorkbook.createDataFormat().getFormat(AMOUNT_FORMAT));
        this.currentDateStyle = this.currentWorkbook.createCellStyle();
        this.currentDateStyle.cloneStyleFrom(this.currentIntegerStyle);
        this.currentDateStyle.setDataFormat(this.currentWorkbook.createDataFormat().getFormat(DATE_FORMAT));
    }
}
