package com.cosylab.vdct.model.db.db;

import com.cosylab.logging.DebugLogger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import com.cosylab.vdct.model.db.Constants;
import com.cosylab.vdct.model.db.DBModule;
import com.cosylab.vdct.utils.EnhancedStreamTokenizer;
import com.cosylab.vdct.utils.PathSpecification;
import com.cosylab.vdct.utils.StringUtils;
import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This type was created in VisualAge.
 */
public class DBResolver {

    public static final Logger debug = DebugLogger.getLogger(DBModule.class.getName(), Level.ALL);

    private static final String errorString = "Invalid VisualDCT visual data...";

    private static final String nullString = "";

    public static final String FIELD = "field";

    public static final String RECORD = "record";

    public static final String GRECORD = "grecord";

    public static final String INCLUDE = "include";

    public static final String PATH = "path";

    public static final String ADDPATH = "addpath";

    public static final String TEMPLATE = "template";

    public static final String PORT = "port";

    public static final String EXPAND = "expand";

    public static final String MACRO = "macro";

    private static final String ENDSTR = "}";

    private static final String NL = "\n";

    public static final String VDCTSKIP = "SKIP";

    public static final String VDCTVIEW = "View";

    public static final String VDCTSPREADSHEET_VIEW = "SpreadsheetView";

    public static final String VDCTSPREADSHEET_COL = "Col";

    public static final String VDCTSPREADSHEET_COLUMNORDER = "ColumnOrder";

    public static final String VDCTSPREADSHEET_SHOWALLROWS = "ShowAllRows";

    public static final String VDCTSPREADSHEET_GROUPCOLUMNSBYGUIGROUP = "GroupColumnsByGuiGroup";

    public static final String VDCTSPREADSHEET_BACKGROUNDCOLOR = "BackgroundColor";

    public static final String VDCTSPREADSHEET_COLUMN = "Column";

    public static final String VDCTSPREADSHEET_WIDTH = "Width";

    public static final String VDCTSPREADSHEET_HIDDENROW = "HiddenRow";

    public static final String VDCTSPREADSHEET_ROWORDER = "RowOrder";

    public static final String VDCTSPREADSHEET_SPLITCOLUMN = "SplitCol";

    public static final String VDCTSPREADSHEET_RECENTSPLIT = "RecentSplit";

    public static final String VDCTRECORD = "Record";

    public static final String VDCTGROUP = "Group";

    public static final String VDCTFIELD = "Field";

    public static final String VDCTLINK = "Link";

    public static final String VDCTVISIBILITY = "Visibility";

    public static final String VDCTCONNECTOR = "Connector";

    public static final String VDCT_CONSTANT_PORT = "ConstantPort";

    public static final String VDCT_INPUT_PORT = "InputPort";

    public static final String VDCT_OUTPUT_PORT = "OutputPort";

    public static final String VDCT_INPUT_MACRO = "InputMacro";

    public static final String VDCT_OUTPUT_MACRO = "OutputMacro";

    public static final String VDCTLINE = "Line";

    public static final String VDCTBOX = "Box";

    public static final String VDCTOVAL = "Oval";

    public static final String VDCTTEXTBOX = "TextBox";

    private static final String DBD_START_STR = "DBDSTART";

    private static final String DBD_ENTRY_STR = "DBD";

    private static final String DBD_END_STR = "DBDEND";

    public static final String DBD_START = "#! " + DBD_START_STR + "\n";

    public static final String DBD_ENTRY = "#! " + DBD_ENTRY_STR + "(\"";

    public static final String DBD_END = "#! " + DBD_END_STR + "\n";

    public static final String TEMPLATE_INSTANCE = "TemplateInstance";

    public static final String TEMPLATE_FIELD = "TemplateField";

    public static EnhancedStreamTokenizer getEnhancedStreamTokenizer(String fileName) {
        FileInputStream fi = null;
        EnhancedStreamTokenizer tokenizer = null;
        try {
            fi = new FileInputStream(fileName);
            tokenizer = new EnhancedStreamTokenizer(new BufferedReader(new InputStreamReader(fi)));
            initializeTokenizer(tokenizer);
        } catch (IOException e) {
            debug.finer("\no) Error occured while opening file '" + fileName + "'");
        }
        return tokenizer;
    }

    /**
	 * 
	 * @return EnhancedStreamTokenizer
	 * @param fileName String
	 */
    public static EnhancedStreamTokenizer getEnhancedStreamTokenizer(InputStream is) {
        EnhancedStreamTokenizer tokenizer = null;
        try {
            tokenizer = new EnhancedStreamTokenizer(new BufferedReader(new InputStreamReader(is)));
            initializeTokenizer(tokenizer);
        } catch (Throwable e) {
            debug.finer("\no) Error occured while opening stream '" + is + "'");
            debug.finer(e.toString());
        }
        return tokenizer;
    }

    /**
	 * 
	 * @param st jEnhancedStreamTokenizer
	 */
    public static void initializeTokenizer(EnhancedStreamTokenizer tokenizer) {
        tokenizer.setParseEscapeSequences(false);
        tokenizer.resetSyntax();
        tokenizer.whitespaceChars(0, 32);
        tokenizer.wordChars(33, 255);
        tokenizer.eolIsSignificant(true);
        tokenizer.parseNumbers();
        tokenizer.quoteChar(DBConstants.quoteChar);
        tokenizer.whitespaceChars(',', ',');
        tokenizer.whitespaceChars('{', '{');
        tokenizer.whitespaceChars('(', '(');
        tokenizer.whitespaceChars(')', ')');
    }

    private static String loadTemplate(Object dsId, DBData data, String templateFile, String referencedFromFile, PathSpecification paths, Stack loadStack, ArrayList loadList) throws Exception {
        File file = paths.search4File(templateFile);
        String templateToResolve = file.getAbsolutePath();
        boolean alreadyLoaded = false;
        alreadyLoaded = loadList.contains(templateToResolve);
        if (alreadyLoaded) {
            return file.getName();
        }
        debug.finer("Loading template \"" + templateFile + "\"...");
        DBData templateData = resolveDB(dsId, templateToResolve, loadStack, loadList);
        if (templateData == null) throw new DBException("Failed to load template: '" + templateFile + "'");
        templateData.getTemplateData().setData(templateData);
        data.addTemplate(templateData.getTemplateData());
        loadList.add(templateToResolve);
        debug.finer("Template \"" + templateFile + "\" loaded.");
        return templateData.getTemplateData().getId();
    }

    /**
	 * VisualDCT layout data is also processed here
	 * @param rootData com.cosylab.vdct.db.DBData
	 * @param tokenizer java.io.EnhancedStreamTokenizer
	 */
    public static String processComment(Object dsId, DBData data, EnhancedStreamTokenizer tokenizer, String fileName) throws Exception {
        if ((data == null) || !tokenizer.sval.equals(DBConstants.layoutDataString)) {
            String comment = tokenizer.sval;
            tokenizer.resetSyntax();
            tokenizer.whitespaceChars(0, 31);
            tokenizer.wordChars(32, 255);
            tokenizer.wordChars('\t', '\t');
            tokenizer.eolIsSignificant(true);
            while ((tokenizer.nextToken() != EnhancedStreamTokenizer.TT_EOL) && (tokenizer.ttype != EnhancedStreamTokenizer.TT_EOF)) if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) {
                comment = comment + tokenizer.nval;
            } else {
                comment = comment + tokenizer.sval;
            }
            initializeTokenizer(tokenizer);
            return comment + NL;
        } else {
            DBRecordData rd;
            DBFieldData fd;
            String str, str2, desc;
            int t, tx, tx2, ty, ty2, t2, t3;
            while ((tokenizer.nextToken() != EnhancedStreamTokenizer.TT_EOL) && (tokenizer.ttype != EnhancedStreamTokenizer.TT_EOF)) if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) if (tokenizer.sval.equalsIgnoreCase(VDCTRECORD)) {
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                rd = (DBRecordData) (data.getRecords().get(str));
                if (rd != null) {
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) rd.setX((int) tokenizer.nval); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) rd.setY((int) tokenizer.nval); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) rd.setColor(StringUtils.int2color((int) tokenizer.nval)); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) rd.setRotated(((int) tokenizer.nval) != 0); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) rd.setDescription(tokenizer.sval); else throw (new DBGParseException(errorString, tokenizer, fileName));
                }
            } else if (tokenizer.sval.equalsIgnoreCase(VDCTFIELD)) {
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                int pos = str.lastIndexOf(Constants.FIELD_SEPARATOR);
                str2 = str.substring(pos + 1);
                str = str.substring(0, pos);
                rd = (DBRecordData) data.getRecords().get(str);
                if (rd != null) {
                    fd = (DBFieldData) rd.getFields().get(str2);
                    if (fd == null) {
                        fd = new DBFieldData(str2, null);
                        rd.addField(fd);
                    }
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) fd.setColor(StringUtils.int2color((int) tokenizer.nval)); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) fd.setRotated(((int) tokenizer.nval) != 0); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) fd.setDescription(tokenizer.sval); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    rd.addVisualField(fd);
                }
            } else if (tokenizer.sval.equalsIgnoreCase(VDCTLINK)) {
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str2 = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                data.addLink(new DBLinkData(str, str2));
            } else if (tokenizer.sval.equalsIgnoreCase(VDCTVISIBILITY)) {
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                int pos = str.lastIndexOf(Constants.FIELD_SEPARATOR);
                str2 = str.substring(pos + 1);
                str = str.substring(0, pos);
                rd = (DBRecordData) data.getRecords().get(str);
                if (rd != null) {
                    fd = (DBFieldData) rd.getFields().get(str2);
                    if (fd == null) {
                        fd = new DBFieldData(str2, null);
                        rd.addField(fd);
                    }
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) fd.setVisibility((int) tokenizer.nval); else throw (new DBGParseException(errorString, tokenizer, fileName));
                }
            } else if (tokenizer.sval.equalsIgnoreCase(VDCTCONNECTOR)) {
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str2 = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) tx = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) ty = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) t = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) desc = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                int mode = 0;
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) mode = (int) tokenizer.nval; else tokenizer.pushBack();
                data.addConnector(new DBConnectorData(str, str2, tx, ty, StringUtils.int2color(t), desc, mode));
            } else if (tokenizer.sval.equalsIgnoreCase(VDCTGROUP)) {
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) tx = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) ty = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) t = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) desc = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                data.addGroup(new DBGroupData(str, tx, ty, StringUtils.int2color(t), desc));
            } else if (tokenizer.sval.equalsIgnoreCase(TEMPLATE_INSTANCE)) {
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) tx = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) ty = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) t = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) desc = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                DBTemplateInstance ti = (DBTemplateInstance) data.getTemplateInstances().get(str);
                if (ti != null) {
                    ti.setX(tx);
                    ti.setY(ty);
                    ti.setColor(StringUtils.int2color(t));
                    ti.setDescription(desc);
                }
            } else if (tokenizer.sval.equalsIgnoreCase(TEMPLATE_FIELD)) {
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str2 = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) t = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                boolean isRight = false;
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) isRight = ((int) tokenizer.nval) != 0; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) t2 = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                DBTemplateInstance ti = (DBTemplateInstance) data.getTemplateInstances().get(str);
                if (ti != null) ti.getTemplateFields().addElement(new DBTemplateField(str2, StringUtils.int2color(t), isRight, t2));
            } else if (tokenizer.sval.equalsIgnoreCase(VDCTLINE)) {
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) tx = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) ty = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) tx2 = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) ty2 = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                boolean dashed = false;
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) dashed = ((int) tokenizer.nval) != 0; else throw (new DBGParseException(errorString, tokenizer, fileName));
                boolean startArrow = false;
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) startArrow = ((int) tokenizer.nval) != 0; else throw (new DBGParseException(errorString, tokenizer, fileName));
                boolean endArrow = false;
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) endArrow = ((int) tokenizer.nval) != 0; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) t = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                String parentBorderID = null;
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) parentBorderID = tokenizer.sval;
                data.addLine(new DBLine(str, tx, ty, tx2, ty2, dashed, startArrow, endArrow, StringUtils.int2color(t), parentBorderID));
            } else if (tokenizer.sval.equalsIgnoreCase(VDCTOVAL)) {
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) tx = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) ty = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) tx2 = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) ty2 = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                boolean dashed = false;
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) dashed = ((int) tokenizer.nval) != 0; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) t = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                String parentBorderID = null;
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) parentBorderID = tokenizer.sval;
                boolean filled = false;
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) filled = ((int) tokenizer.nval) != 0; else throw (new DBGParseException(errorString, tokenizer, fileName));
                int background = 0;
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) background = (int) tokenizer.nval;
                data.addOval(new DBOval(str, tx, ty, tx2, ty2, dashed, StringUtils.int2color(t), parentBorderID, filled, StringUtils.int2color(background)));
            } else if (tokenizer.sval.equalsIgnoreCase(VDCTBOX)) {
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) tx = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) ty = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) tx2 = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) ty2 = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                boolean dashed = false;
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) dashed = ((int) tokenizer.nval) != 0; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) t = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                String parentBorderID = null;
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) parentBorderID = tokenizer.sval;
                boolean filled = false;
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) filled = ((int) tokenizer.nval) != 0;
                int background = Color.WHITE.getRGB();
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) background = (int) tokenizer.nval;
                data.addBox(new DBBox(str, tx, ty, tx2, ty2, dashed, StringUtils.int2color(t), parentBorderID, filled, StringUtils.int2color(background)));
            } else if (tokenizer.sval.equalsIgnoreCase(VDCTTEXTBOX)) {
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) tx = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) ty = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) tx2 = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) ty2 = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                boolean border = false;
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) border = ((int) tokenizer.nval) != 0; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str2 = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) t2 = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) t3 = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) t = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                try {
                    tokenizer.setParseEscapeSequences(true);
                    tokenizer.nextToken();
                } finally {
                    tokenizer.setParseEscapeSequences(false);
                }
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) desc = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                String parentBorderID = null;
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) parentBorderID = tokenizer.sval;
                data.addTextBox(new DBTextBox(str, tx, ty, tx2, ty2, border, str2, t2, t3, StringUtils.int2color(t), desc, parentBorderID));
            } else if (tokenizer.sval.equalsIgnoreCase(VDCTVIEW)) {
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) t = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) t2 = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                double scale = 1.0;
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) scale = tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                data.setView(new DBView(t, t2, scale));
            } else if (tokenizer.sval.equalsIgnoreCase(VDCTSPREADSHEET_VIEW)) {
                String type = null;
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                    type = tokenizer.sval;
                } else {
                    throw (new DBGParseException(errorString, tokenizer, fileName));
                }
                String name = null;
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                    name = tokenizer.sval;
                } else {
                    throw (new DBGParseException(errorString, tokenizer, fileName));
                }
                String modeName = null;
                Boolean showAllRows = null;
                Boolean groupColumnsByGuiGroup = null;
                Integer backgroundColor = null;
                DBSheetRowOrder rowOrder = null;
                Map columnsMap = new HashMap();
                Vector splitColumnsVector = new Vector();
                Vector hiddenRowsVector = new Vector();
                Vector recentSplitsVector = new Vector();
                int columnPropertyIndex = 0;
                int namelessPropertyIndex = 0;
                tokenizer.nextToken();
                while (tokenizer.ttype != EnhancedStreamTokenizer.TT_EOL && tokenizer.ttype != EnhancedStreamTokenizer.TT_EOF) {
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                        if (tokenizer.sval.equalsIgnoreCase(VDCTSPREADSHEET_COLUMNORDER)) {
                            tokenizer.nextToken();
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                                modeName = tokenizer.sval;
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                        } else if (tokenizer.sval.equalsIgnoreCase(VDCTSPREADSHEET_SHOWALLROWS)) {
                            tokenizer.nextToken();
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                                showAllRows = Boolean.valueOf(tokenizer.sval.equals(String.valueOf(true)));
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                        } else if (tokenizer.sval.equalsIgnoreCase(VDCTSPREADSHEET_GROUPCOLUMNSBYGUIGROUP)) {
                            tokenizer.nextToken();
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                                groupColumnsByGuiGroup = Boolean.valueOf(tokenizer.sval.equals(String.valueOf(true)));
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                        } else if (tokenizer.sval.equalsIgnoreCase(VDCTSPREADSHEET_BACKGROUNDCOLOR)) {
                            tokenizer.nextToken();
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) {
                                backgroundColor = new Integer((int) tokenizer.nval);
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                        } else if (tokenizer.sval.equalsIgnoreCase(VDCTSPREADSHEET_COLUMN)) {
                            tokenizer.nextToken();
                            String columnName = null;
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                                columnName = tokenizer.sval;
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                            tokenizer.nextToken();
                            boolean hidden = false;
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                                hidden = tokenizer.sval.equals(String.valueOf(true));
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                            tokenizer.nextToken();
                            int sortIndex = 0;
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) {
                                sortIndex = (int) tokenizer.nval;
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                            Vector widthData = new Vector();
                            tokenizer.nextToken();
                            while (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD && tokenizer.sval.equalsIgnoreCase(VDCTSPREADSHEET_WIDTH)) {
                                tokenizer.nextToken();
                                int splitIndex = 0;
                                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) {
                                    splitIndex = (int) tokenizer.nval;
                                } else {
                                    throw (new DBGParseException(errorString, tokenizer, fileName));
                                }
                                tokenizer.nextToken();
                                int width = 0;
                                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) {
                                    width = (int) tokenizer.nval;
                                } else {
                                    throw (new DBGParseException(errorString, tokenizer, fileName));
                                }
                                widthData.add(new DBSheetColWidth(splitIndex, width));
                                tokenizer.nextToken();
                            }
                            DBSheetColWidth[] splitPartWidthData = new DBSheetColWidth[widthData.size()];
                            widthData.copyInto(splitPartWidthData);
                            columnsMap.put(columnName, new DBSheetColumn(columnName, hidden, sortIndex, splitPartWidthData));
                            continue;
                        } else if (tokenizer.sval.equalsIgnoreCase(VDCTSPREADSHEET_ROWORDER)) {
                            tokenizer.nextToken();
                            String columnName = null;
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                                columnName = tokenizer.sval;
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                            tokenizer.nextToken();
                            int splitIndex = 0;
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) {
                                splitIndex = (int) tokenizer.nval;
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                            tokenizer.nextToken();
                            String ascOrder = null;
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                                ascOrder = tokenizer.sval;
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                            rowOrder = new DBSheetRowOrder(columnName, splitIndex, ascOrder);
                        } else if (tokenizer.sval.equalsIgnoreCase(VDCTSPREADSHEET_HIDDENROW)) {
                            tokenizer.nextToken();
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                                hiddenRowsVector.add(tokenizer.sval);
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                        } else if (tokenizer.sval.equalsIgnoreCase(VDCTSPREADSHEET_SPLITCOLUMN)) {
                            tokenizer.nextToken();
                            String columnName = null;
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                                columnName = tokenizer.sval;
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                            tokenizer.nextToken();
                            String delimiterTypeString = null;
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                                delimiterTypeString = tokenizer.sval;
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                            tokenizer.nextToken();
                            String pattern = null;
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                                pattern = tokenizer.sval;
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                            splitColumnsVector.add(new DBSheetSplitCol(columnName, delimiterTypeString, pattern));
                        } else if (tokenizer.sval.equalsIgnoreCase(VDCTSPREADSHEET_RECENTSPLIT)) {
                            tokenizer.nextToken();
                            String delimiterTypeString = null;
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                                delimiterTypeString = tokenizer.sval;
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                            tokenizer.nextToken();
                            String pattern = null;
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                                pattern = tokenizer.sval;
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                            recentSplitsVector.add(new DBSheetSplitCol(delimiterTypeString, pattern));
                        } else if (tokenizer.sval.equalsIgnoreCase(VDCTSPREADSHEET_COL)) {
                            tokenizer.nextToken();
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                                columnsMap.put(tokenizer.sval, new DBSheetColumn(tokenizer.sval, false, columnPropertyIndex));
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                            columnPropertyIndex++;
                        } else if (namelessPropertyIndex == 0) {
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                                modeName = tokenizer.sval;
                                namelessPropertyIndex++;
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                        } else if (namelessPropertyIndex == 1) {
                            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD || tokenizer.ttype == DBConstants.quoteChar) {
                                showAllRows = Boolean.valueOf(tokenizer.sval.equals(String.valueOf(true)));
                                namelessPropertyIndex++;
                            } else {
                                throw (new DBGParseException(errorString, tokenizer, fileName));
                            }
                        }
                    } else if (namelessPropertyIndex == 2) {
                        if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) {
                            backgroundColor = new Integer((int) tokenizer.nval);
                            namelessPropertyIndex++;
                        } else {
                            throw (new DBGParseException(errorString, tokenizer, fileName));
                        }
                    } else {
                        throw (new DBGParseException(errorString, tokenizer, fileName));
                    }
                    tokenizer.nextToken();
                }
                DBSheetSplitCol[] splitColumns = new DBSheetSplitCol[splitColumnsVector.size()];
                splitColumnsVector.copyInto(splitColumns);
                String[] hiddenRows = new String[hiddenRowsVector.size()];
                hiddenRowsVector.copyInto(hiddenRows);
                DBSheetSplitCol[] recentSplits = new DBSheetSplitCol[recentSplitsVector.size()];
                recentSplitsVector.copyInto(recentSplits);
                DBSheetView viewRecord = new DBSheetView(type, name);
                viewRecord.setModeName(modeName);
                viewRecord.setShowAllRows(showAllRows);
                viewRecord.setGroupColumnsByGuiGroup(groupColumnsByGuiGroup);
                viewRecord.setBackgroundColor(backgroundColor);
                viewRecord.setRowOrder(rowOrder);
                viewRecord.setColumns(columnsMap);
                viewRecord.setSplitColumns(splitColumns);
                viewRecord.setHiddenRows(hiddenRows);
                viewRecord.setRecentSplits(recentSplits);
                DBSheetData.getInstance(dsId).add(viewRecord);
            } else if (tokenizer.sval.equalsIgnoreCase(VDCTSKIP)) {
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) tx = (int) tokenizer.nval; else {
                    tx = 1;
                    tokenizer.pushBack();
                }
                skipLines(tx + 1, tokenizer, fileName);
            } else if (tokenizer.sval.equalsIgnoreCase("VDCTRecordPos")) {
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                rd = (DBRecordData) (data.records.get(str));
                if (rd != null) {
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) rd.setX((int) tokenizer.nval); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) rd.setY((int) tokenizer.nval); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) rd.setRotated(((int) tokenizer.nval) != 0); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    rd.setColor(java.awt.Color.black);
                }
            } else if (tokenizer.sval.equalsIgnoreCase("VDCTGroupPos")) {
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) tx = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) ty = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                data.addGroup(new DBGroupData(str, tx, ty, java.awt.Color.black, nullString));
            } else if (tokenizer.sval.equalsIgnoreCase("VDCTLinkData")) {
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) desc = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) tx = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) tx2 = (int) tokenizer.nval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                tokenizer.nextToken();
                tokenizer.nextToken();
            }
            return nullString;
        }
    }

    /**
	 * VisualDCT layout data is also processed here
	 * @param rootData com.cosylab.vdct.db.DBData
	 * @param tokenizer java.io.EnhancedStreamTokenizer
	 */
    public static String processTemplateComment(DBTemplate template, EnhancedStreamTokenizer tokenizer, String fileName) throws Exception {
        if ((template == null) || !tokenizer.sval.equals(DBConstants.layoutDataString)) {
            String comment = tokenizer.sval;
            tokenizer.resetSyntax();
            tokenizer.whitespaceChars(0, 31);
            tokenizer.wordChars(32, 255);
            tokenizer.wordChars('\t', '\t');
            tokenizer.eolIsSignificant(true);
            while ((tokenizer.nextToken() != EnhancedStreamTokenizer.TT_EOL) && (tokenizer.ttype != EnhancedStreamTokenizer.TT_EOF)) if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) {
                comment = comment + tokenizer.nval;
            } else {
                comment = comment + tokenizer.sval;
            }
            initializeTokenizer(tokenizer);
            return comment + NL;
        } else {
            String str;
            while ((tokenizer.nextToken() != EnhancedStreamTokenizer.TT_EOL) && (tokenizer.ttype != EnhancedStreamTokenizer.TT_EOF)) if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) if (tokenizer.sval.equalsIgnoreCase(VDCT_CONSTANT_PORT) || tokenizer.sval.equalsIgnoreCase(VDCT_INPUT_PORT) || tokenizer.sval.equalsIgnoreCase(VDCT_OUTPUT_PORT)) {
                int mode;
                if (tokenizer.sval.equalsIgnoreCase(VDCT_CONSTANT_PORT)) mode = OutLink.CONSTANT_PORT_MODE; else if (tokenizer.sval.equalsIgnoreCase(VDCT_INPUT_PORT)) mode = OutLink.INPUT_PORT_MODE; else mode = OutLink.OUTPUT_PORT_MODE;
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                DBPort port = (DBPort) (template.getPorts().get(str));
                if (port != null) {
                    port.setMode(mode);
                    tokenizer.nextToken();
                    if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) port.setInLinkID(tokenizer.sval); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) port.setX((int) tokenizer.nval); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) port.setY((int) tokenizer.nval); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) port.setColor(StringUtils.int2color((int) tokenizer.nval)); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) port.setDefaultVisibility((int) tokenizer.nval); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_EOL) {
                        tokenizer.pushBack();
                        port.setNamePositionNorth(true);
                    } else if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) {
                        port.setNamePositionNorth(new Boolean(tokenizer.sval).booleanValue());
                    } else throw (new DBGParseException(errorString, tokenizer, fileName));
                    port.setHasVisual(true);
                }
            } else if (tokenizer.sval.equalsIgnoreCase(VDCT_INPUT_MACRO) || tokenizer.sval.equalsIgnoreCase(VDCT_OUTPUT_MACRO)) {
                int mode;
                if (tokenizer.sval.equalsIgnoreCase(VDCT_INPUT_MACRO)) mode = InLink.INPUT_MACRO_MODE; else mode = InLink.OUTPUT_MACRO_MODE;
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str = tokenizer.sval; else throw (new DBGParseException(errorString, tokenizer, fileName));
                DBMacro macro = (DBMacro) (template.getMacros().get(str));
                if (macro == null) {
                    macro = new DBMacro(str);
                    template.addMacro(macro);
                    macro.setMode(mode);
                    tokenizer.nextToken();
                    if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) macro.setDescription(tokenizer.sval); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) macro.setX((int) tokenizer.nval); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) macro.setY((int) tokenizer.nval); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) macro.setColor(StringUtils.int2color((int) tokenizer.nval)); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_NUMBER) macro.setDefaultVisibility((int) tokenizer.nval); else throw (new DBGParseException(errorString, tokenizer, fileName));
                    tokenizer.nextToken();
                    if (tokenizer.ttype == EnhancedStreamTokenizer.TT_EOL) {
                        tokenizer.pushBack();
                        macro.setNamePositionNorth(true);
                    } else if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) {
                        macro.setNamePositionNorth(new Boolean(tokenizer.sval).booleanValue());
                    } else throw (new DBGParseException(errorString, tokenizer, fileName));
                    macro.setHasVisual(true);
                }
            }
            return nullString;
        }
    }

    /**
	 * VisualDCT layout data is also processed here
	 * @param tokenizer java.io.EnhancedStreamTokenizer
	 */
    public static void skipLines(int linesToSkip, EnhancedStreamTokenizer tokenizer, String fileName) throws Exception {
        int lines = 0;
        while (lines < linesToSkip) {
            tokenizer.nextToken();
            if (tokenizer.ttype == EnhancedStreamTokenizer.TT_EOF) return; else if (tokenizer.ttype == EnhancedStreamTokenizer.TT_EOL) lines++;
        }
    }

    public static void readVdctData(Object dsId, DBData data, String vdctData, String source) {
        StringReader reader = new StringReader(vdctData);
        EnhancedStreamTokenizer tokenizer = new EnhancedStreamTokenizer(reader);
        initializeTokenizer(tokenizer);
        try {
            while (tokenizer.nextToken() != EnhancedStreamTokenizer.TT_EOF) {
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) && (tokenizer.sval.startsWith(DBConstants.commentString))) {
                    processComment(dsId, data, tokenizer, source);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
	 * This method was created in VisualAge.
	 * @param rootData com.cosylab.vdct.db.DBData
	 * @param tokenizer java.io.EnhancedStreamTokenizer
	 */
    public static void processDB(Object dsId, DBData data, EnhancedStreamTokenizer tokenizer, String fileName, PathSpecification paths, Stack loadStack, ArrayList loadList) throws Exception {
        String comment = nullString;
        String str, str2;
        String include_filename;
        EnhancedStreamTokenizer inctokenizer = null;
        if (data != null) {
            if (loadStack.contains(fileName)) {
                StringBuffer buf = new StringBuffer();
                buf.append("Cyclic reference detected when trying to load '");
                buf.append(fileName);
                buf.append("'.\nLoad stack trace:\n");
                for (int i = loadStack.size() - 1; i >= 0; i--) {
                    buf.append("\t");
                    buf.append(loadStack.elementAt(i));
                    buf.append("\n");
                }
                throw new DBException(buf.toString());
            } else {
                loadStack.push(fileName);
            }
            try {
                while (tokenizer.nextToken() != EnhancedStreamTokenizer.TT_EOF) if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) if (tokenizer.sval.startsWith(DBConstants.commentString)) comment += processComment(dsId, data, tokenizer, fileName); else if (tokenizer.sval.equalsIgnoreCase(RECORD) || tokenizer.sval.equalsIgnoreCase(GRECORD)) {
                    DBRecordData rd = new DBRecordData();
                    tokenizer.nextToken();
                    if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) rd.setRecord_type(tokenizer.sval); else throw (new DBParseException("Invalid record type...", tokenizer, fileName));
                    tokenizer.nextToken();
                    if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) rd.setName(tokenizer.sval); else throw (new DBParseException("Invalid record name...", tokenizer, fileName));
                    rd.setComment(comment);
                    comment = nullString;
                    processFields(dsId, rd, tokenizer, fileName, paths);
                    data.addRecord(rd);
                } else if (tokenizer.sval.equalsIgnoreCase(TEMPLATE)) {
                    str = null;
                    tokenizer.nextToken();
                    if (tokenizer.ttype == DBConstants.quoteChar) str = tokenizer.sval; else tokenizer.pushBack();
                    DBTemplate templateData = data.getTemplateData();
                    if (!templateData.isInitialized()) {
                        templateData.setInitialized(true);
                        templateData.setComment(comment);
                        comment = nullString;
                        templateData.setDescription(str);
                        DBTemplateEntry entry = new DBTemplateEntry();
                        data.addEntry(entry);
                    } else {
                        comment = nullString;
                    }
                    processPorts(templateData, tokenizer, fileName, paths);
                } else if (tokenizer.sval.equalsIgnoreCase(EXPAND)) {
                    tokenizer.nextToken();
                    if (tokenizer.ttype == DBConstants.quoteChar) str = tokenizer.sval; else throw (new DBParseException("Invalid expand file...", tokenizer, fileName));
                    tokenizer.nextToken();
                    if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) str2 = tokenizer.sval; else throw (new DBParseException("Invalid expand template instance name...", tokenizer, fileName));
                    String loadedTemplateId = loadTemplate(dsId, data, str, fileName, paths, loadStack, loadList);
                    DBTemplateInstance ti = new DBTemplateInstance(str2, loadedTemplateId);
                    ti.setComment(comment);
                    comment = nullString;
                    processMacros(dsId, ti, tokenizer, fileName, paths);
                    data.addTemplateInstance(ti);
                } else if (tokenizer.sval.equalsIgnoreCase(INCLUDE)) {
                    tokenizer.nextToken();
                    if (tokenizer.ttype == DBConstants.quoteChar) include_filename = tokenizer.sval; else throw (new DBParseException("Invalid include filename...", tokenizer, fileName));
                    DBDataEntry entry = new DBDataEntry(INCLUDE + " \"" + include_filename + "\"");
                    entry.setComment(comment);
                    comment = nullString;
                    data.addEntry(entry);
                    File file = paths.search4File(include_filename);
                    inctokenizer = getEnhancedStreamTokenizer(file.getAbsolutePath());
                    if (inctokenizer != null) processDB(dsId, data, inctokenizer, include_filename, new PathSpecification(file.getParentFile().getAbsolutePath(), paths), loadStack, loadList);
                } else if (tokenizer.sval.equalsIgnoreCase(PATH)) {
                    tokenizer.nextToken();
                    if (tokenizer.ttype == DBConstants.quoteChar) str = tokenizer.sval; else throw (new DBParseException("Invalid path...", tokenizer, fileName));
                    DBDataEntry entry = new DBDataEntry(PATH + " \"" + str + "\"");
                    entry.setComment(comment);
                    comment = nullString;
                    data.addEntry(entry);
                    paths.setPath(str);
                } else if (tokenizer.sval.equalsIgnoreCase(ADDPATH)) {
                    tokenizer.nextToken();
                    if (tokenizer.ttype == DBConstants.quoteChar) str = tokenizer.sval; else throw (new DBParseException("Invalid addpath...", tokenizer, fileName));
                    DBDataEntry entry = new DBDataEntry(ADDPATH + " \"" + str + "\"");
                    entry.setComment(comment);
                    comment = nullString;
                    data.addEntry(entry);
                    paths.addAddPath(str);
                }
            } catch (Exception e) {
                throw e;
            } finally {
                loadStack.pop();
            }
        }
    }

    /**
	 * This method was created in VisualAge.
	 * @param rd com.cosylab.vdct.db.DBRecordData
	 * @param tokenizer java.io.EnhancedStreamTokenizer
	 * @exception java.lang.Exception The exception description.
	 */
    public static void processMacros(Object dsId, DBTemplateInstance templateInstance, EnhancedStreamTokenizer tokenizer, String fileName, PathSpecification paths) throws Exception {
        String name;
        String value;
        String include_filename;
        EnhancedStreamTokenizer inctokenizer = null;
        if (templateInstance != null) while (tokenizer.nextToken() != EnhancedStreamTokenizer.TT_EOF) if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) if (tokenizer.sval.equals(ENDSTR)) break; else if (tokenizer.sval.startsWith(DBConstants.commentString)) processComment(dsId, null, tokenizer, fileName); else if (tokenizer.sval.equalsIgnoreCase(MACRO)) {
            tokenizer.nextToken();
            if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) name = tokenizer.sval; else throw (new DBParseException("Invalid macro name...", tokenizer, fileName));
            tokenizer.nextToken();
            if (tokenizer.ttype == DBConstants.quoteChar) value = tokenizer.sval; else throw (new DBParseException("Invalid macro value...", tokenizer, fileName));
            templateInstance.addProperty(name, value);
        } else if (tokenizer.sval.equalsIgnoreCase(INCLUDE)) {
            tokenizer.nextToken();
            if (tokenizer.ttype == DBConstants.quoteChar) include_filename = tokenizer.sval; else throw (new DBParseException("Invalid include filename...", tokenizer, fileName));
            File file = paths.search4File(include_filename);
            inctokenizer = getEnhancedStreamTokenizer(file.getAbsolutePath());
            if (inctokenizer != null) processMacros(dsId, templateInstance, inctokenizer, include_filename, new PathSpecification(file.getParentFile().getAbsolutePath(), paths));
        }
    }

    /**
	 * This method was created in VisualAge.
	 * @param rd com.cosylab.vdct.db.DBRecordData
	 * @param tokenizer java.io.EnhancedStreamTokenizer
	 * @exception java.lang.Exception The exception description.
	 */
    public static void processPorts(DBTemplate template, EnhancedStreamTokenizer tokenizer, String fileName, PathSpecification paths) throws Exception {
        String name;
        String value;
        String description;
        String include_filename;
        String comment = nullString;
        EnhancedStreamTokenizer inctokenizer = null;
        if (template != null) while (tokenizer.nextToken() != EnhancedStreamTokenizer.TT_EOF) if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) if (tokenizer.sval.equals(ENDSTR)) break; else if (tokenizer.sval.startsWith(DBConstants.commentString)) comment += processTemplateComment(template, tokenizer, fileName); else if (tokenizer.sval.equalsIgnoreCase(PORT)) {
            tokenizer.nextToken();
            if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) name = tokenizer.sval; else throw (new DBParseException("Invalid port name...", tokenizer, fileName));
            tokenizer.nextToken();
            if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) value = tokenizer.sval; else throw (new DBParseException("Invalid port value...", tokenizer, fileName));
            description = null;
            tokenizer.nextToken();
            if (tokenizer.ttype == DBConstants.quoteChar) description = tokenizer.sval; else tokenizer.pushBack();
            DBPort port = new DBPort(name, value);
            port.setComment(comment);
            comment = nullString;
            port.setDescription(description);
            template.addPort(port);
        } else if (tokenizer.sval.equalsIgnoreCase(INCLUDE)) {
            tokenizer.nextToken();
            if (tokenizer.ttype == DBConstants.quoteChar) include_filename = tokenizer.sval; else throw (new DBParseException("Invalid include filename...", tokenizer, fileName));
            File file = paths.search4File(include_filename);
            inctokenizer = getEnhancedStreamTokenizer(file.getAbsolutePath());
            if (inctokenizer != null) processPorts(template, inctokenizer, include_filename, new PathSpecification(file.getParentFile().getAbsolutePath(), paths));
        }
    }

    /**
	 * This method was created in VisualAge.
	 * @param rd com.cosylab.vdct.db.DBRecordData
	 * @param tokenizer java.io.EnhancedStreamTokenizer
	 * @exception java.lang.Exception The exception description.
	 */
    public static void processFields(Object dsId, DBRecordData rd, EnhancedStreamTokenizer tokenizer, String fileName, PathSpecification paths) throws Exception {
        String name;
        String value;
        String comment = nullString;
        String include_filename;
        EnhancedStreamTokenizer inctokenizer = null;
        if (rd != null) while (tokenizer.nextToken() != EnhancedStreamTokenizer.TT_EOF) if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) if (tokenizer.sval.equals(ENDSTR)) break; else if (tokenizer.sval.startsWith(DBConstants.commentString)) comment += processComment(dsId, null, tokenizer, fileName); else if (tokenizer.sval.equalsIgnoreCase(FIELD)) {
            tokenizer.nextToken();
            if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) name = tokenizer.sval; else throw (new DBParseException("Invalid field name...", tokenizer, fileName));
            tokenizer.nextToken();
            if (tokenizer.ttype == DBConstants.quoteChar) value = tokenizer.sval; else throw (new DBParseException("Invalid field value...", tokenizer, fileName));
            DBFieldData fd = new DBFieldData(name, value);
            fd.setComment(comment);
            comment = nullString;
            rd.addField(fd);
        } else if (tokenizer.sval.equalsIgnoreCase(INCLUDE)) {
            tokenizer.nextToken();
            if (tokenizer.ttype == DBConstants.quoteChar) include_filename = tokenizer.sval; else throw (new DBParseException("Invalid include filename...", tokenizer, fileName));
            File file = paths.search4File(include_filename);
            inctokenizer = getEnhancedStreamTokenizer(file.getAbsolutePath());
            if (inctokenizer != null) processFields(dsId, rd, inctokenizer, include_filename, new PathSpecification(file.getParentFile().getAbsolutePath(), paths));
        }
    }

    /**
	 * This method was created in VisualAge.
	 * @return Vector
	 * @param fileName java.lang.String
	 */
    public static String[] resolveIncodedDBDs(String fileName) throws IOException {
        EnhancedStreamTokenizer tokenizer = getEnhancedStreamTokenizer(fileName);
        if (tokenizer == null) return null;
        String[] dbds = null;
        Vector vec = null;
        while (tokenizer.nextToken() != EnhancedStreamTokenizer.TT_EOF) if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) if (tokenizer.sval.startsWith(DBConstants.layoutDataString)) {
            while ((tokenizer.nextToken() != EnhancedStreamTokenizer.TT_EOL) && (tokenizer.ttype != EnhancedStreamTokenizer.TT_EOF)) if (tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) if (tokenizer.sval.equalsIgnoreCase(DBD_START_STR)) vec = new Vector(); else if (tokenizer.sval.equalsIgnoreCase(DBD_ENTRY_STR)) {
                if (vec == null) {
                    vec = new Vector();
                    debug.finer("Warning: error found in file '" + fileName + "', line " + tokenizer.lineno() + " near token '" + tokenizer.sval + "':\n\t'" + DBD_ENTRY_STR + "' before '" + DBD_END_STR + "'...");
                }
                tokenizer.nextToken();
                if ((tokenizer.ttype == EnhancedStreamTokenizer.TT_WORD) || (tokenizer.ttype == DBConstants.quoteChar)) vec.addElement(tokenizer.sval); else debug.finer("Warning: error found in file '" + fileName + "', line " + tokenizer.lineno() + " near token '" + tokenizer.sval + "':\n\tinvalid '" + DBD_ENTRY_STR + "' entry. Quoted DBD filename expected...");
            } else if (tokenizer.sval.equalsIgnoreCase(DBD_END_STR)) break;
        }
        if (vec != null) {
            dbds = new String[vec.size()];
            vec.toArray(dbds);
        }
        return dbds;
    }

    /**
	 * This method was created in VisualAge.
	 * @return Vector
	 * @param fileName java.lang.String
	 */
    public static DBData resolveDB(Object dsId, String fileName, Stack loadStack, ArrayList loadList) throws Exception {
        DBData data = null;
        EnhancedStreamTokenizer tokenizer = getEnhancedStreamTokenizer(fileName);
        if (tokenizer != null) {
            File file = new File(fileName);
            PathSpecification paths = new PathSpecification(file.getParentFile().getAbsolutePath());
            data = new DBData(file.getName(), file.getAbsolutePath());
            data.getTemplateData().setModificationTime(file.lastModified());
            processDB(dsId, data, tokenizer, fileName, paths, loadStack, loadList);
            System.gc();
        }
        return data;
    }

    /**
	 * This method was created in VisualAge.
	 * @return Vector
	 * @param fileName java.lang.String
	 */
    public static DBData resolveDB(Object dsId, InputStream is, Stack loadStack, ArrayList loadList) {
        DBData data = null;
        EnhancedStreamTokenizer tokenizer = getEnhancedStreamTokenizer(is);
        if (tokenizer != null) {
            try {
                PathSpecification paths = new PathSpecification(Settings.getDefaultDir());
                data = new DBData("System Clipboard", "System Clipboard");
                processDB(dsId, data, tokenizer, "System Clipboard", paths, loadStack, loadList);
            } catch (Exception e) {
                debug.finer(e.toString());
                data = null;
            } finally {
                System.gc();
            }
        }
        return data;
    }

    /**
	 * This method was created in VisualAge.
	 * @return Vector
	 * @param fileName java.lang.String
	 */
    public static DBData resolveDB(Object dsId, String fileName) throws Exception {
        Stack loadStack = new Stack();
        ArrayList loadList = new ArrayList();
        return resolveDB(dsId, fileName, loadStack, loadList);
    }

    /**
	 * This method was created in VisualAge.
	 * @return Vector
	 * @param fileName java.lang.String
	 */
    public static DBData resolveDB(Object dsId, InputStream is) throws Exception {
        Stack loadStack = new Stack();
        ArrayList loadList = new ArrayList();
        return resolveDB(dsId, is, loadStack, loadList);
    }

    /**
	 * This method was created in VisualAge.
	 * @return Vector
	 * @param fileName java.lang.String
	 */
    public static DBData resolveDBasURL(java.net.URL url) throws Exception {
        DBData data = null;
        InputStream fi = null;
        EnhancedStreamTokenizer tokenizer = null;
        try {
            fi = url.openStream();
            tokenizer = new EnhancedStreamTokenizer(new BufferedReader(new InputStreamReader(fi)));
            initializeTokenizer(tokenizer);
        } catch (Exception e) {
            debug.finer("\nError occured while opening URL '" + url.toString() + "'");
            debug.finer(e.toString());
            return null;
        }
        if (tokenizer != null) {
            try {
            } finally {
                System.gc();
            }
        }
        return data;
    }
}
