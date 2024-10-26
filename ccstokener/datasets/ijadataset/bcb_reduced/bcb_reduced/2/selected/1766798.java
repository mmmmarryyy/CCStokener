package com.hitao.codegen.configs.dao;

import static com.hitao.codegen.constent.StringConstants.BLANK_LINE;
import static com.hitao.codegen.constent.StringConstants.CODEGEN_DAO_DO_SUFFIX;
import static com.hitao.codegen.util.StringUtils.EMPTY;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.velocity.app.VelocityEngine;
import com.hitao.codegen.configs.DaoServiceConfigurationManager;
import com.hitao.codegen.configs.basic.IConfigObject;
import com.hitao.codegen.configs.common.AbstractCodeGenConfig;
import com.hitao.codegen.configs.dao.mapping.IColumnInfo;
import com.hitao.codegen.util.ClassPathUtils;

/***
 * The SQL map configuration of the DAO.
 * 
 * @author zhangjun.ht
 * @created 2011-2-19
 * @version $Id: SqlMapXmlConfig.java 59 2011-05-23 05:47:35Z guest $
 */
public class SqlMapXmlConfig extends AbstractCodeGenConfig {

    private static final long serialVersionUID = 7111008159277154321L;

    private static final String TABLE = "table";

    private static final String DAO_NAME = "daoName";

    private static final String DO_CLASS_NAME = "doClassName";

    public static final String DEFAULT_TABLE_NAME_ALIAS = "t";

    private String table_ = EMPTY;

    private String daoName_ = EMPTY;

    private String doClassName_ = EMPTY;

    private String insertStatementId_ = EMPTY;

    private String updateStatementId_ = EMPTY;

    private String paginStatementId_ = EMPTY;

    private String selectByIdStatementId_ = EMPTY;

    public static String INSERT_STATEMENT_RETURN_TYPE = "Long";

    public static String UPDATE_STATEMENT_RETURN_TYPE = "Integer";

    public static String SELECT_PAGIN_RETURN_COLLECTION_TYPE = "com.skyjoo.common.Paginable";

    private List<IColumnInfo> columnList = new ArrayList<IColumnInfo>();

    private static final String XML_HEAD = "<?xml version=\"1.0\" encoding=\"GB2312\"?>";

    private static final String XML_TDD = "<!DOCTYPE sqlMap PUBLIC \"-//iBATIS.com//DTD SQL Map 2.0//EN\" \"http://www.ibatis.com/dtd/sql-map-2.dtd\">";

    public static final String VELOCITY_PROPS_FILE = "configs/velocity.properties";

    /**
     * VelocityEngine
     */
    protected VelocityEngine velocityEngine = null;

    public SqlMapXmlConfig() {
        velocityEngine = newVelocityEngine();
    }

    private VelocityEngine newVelocityEngine() {
        VelocityEngine velocityEngine = null;
        InputStream is = null;
        try {
            URL url = ClassPathUtils.getResource(VELOCITY_PROPS_FILE);
            is = url.openStream();
            Properties props = new Properties();
            props.load(is);
            velocityEngine = new VelocityEngine(props);
            velocityEngine.init();
        } catch (Exception e) {
            throw new RuntimeException("can not find velocity props file, file=" + VELOCITY_PROPS_FILE, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return velocityEngine;
    }

    @Override
    public void setConfigObject(String argKey, IConfigObject argValue) {
        if (TABLE.equalsIgnoreCase(argKey)) {
            setTable(argValue.toString());
        } else if (DAO_NAME.equals(argKey)) {
            setDaoName(argValue.toString());
        } else if (DO_CLASS_NAME.equals(argKey)) {
            setDoClassName(argValue.toString());
        }
    }

    @Override
    public String getName() {
        String name = getTable().toUpperCase() + "_SqlMap";
        return name;
    }

    @Override
    public String getStatement() {
        StringBuffer statement = new StringBuffer();
        statement.append(XML_HEAD);
        statement.append(BLANK_LINE);
        statement.append(XML_TDD);
        statement.append(BLANK_LINE);
        String rootStart = "<sqlMap namespace=\"" + getNameSpaceName() + "\">";
        statement.append(rootStart);
        statement.append(BLANK_LINE);
        statement.append(BLANK_LINE);
        getTypeAlias(statement);
        statement.append(BLANK_LINE);
        statement.append(BLANK_LINE);
        getResultMap(statement);
        statement.append(BLANK_LINE);
        statement.append(BLANK_LINE);
        getColumnAlias(statement);
        statement.append(BLANK_LINE);
        statement.append(BLANK_LINE);
        getColumns(statement);
        statement.append(BLANK_LINE);
        statement.append(BLANK_LINE);
        getInsert(statement);
        statement.append(BLANK_LINE);
        statement.append(BLANK_LINE);
        getUpdate(statement);
        statement.append(BLANK_LINE);
        statement.append(BLANK_LINE);
        getSelect(statement);
        statement.append(BLANK_LINE);
        statement.append(BLANK_LINE);
        getSelectById(statement);
        statement.append(BLANK_LINE);
        statement.append(BLANK_LINE);
        getSelectForPagination(statement);
        statement.append(BLANK_LINE);
        statement.append(BLANK_LINE);
        getCountForPagination(statement);
        statement.append(BLANK_LINE);
        statement.append(BLANK_LINE);
        statement.append("</sqlMap>");
        return statement.toString();
    }

    /***
     * <typeAlias> elements in SQL map configuration file.
     * 
     * @param argStringBuffer
     */
    protected void getTypeAlias(StringBuffer argStringBuffer) {
        argStringBuffer.append("	<typeAlias alias=\"" + getAliasName() + "\" type=\"" + getDoClassName() + "\" />");
    }

    /**
     * <resultMap> elements in SQL map configuration file. the mapping between DO's fields and the columns in table.
     * 
     * @param argStringBuffer
     */
    protected void getResultMap(StringBuffer argStringBuffer) {
        argStringBuffer.append("	<resultMap id=\"" + getResultMapId() + "\" class=\"" + getAliasName() + "\">");
        argStringBuffer.append(BLANK_LINE);
        for (IColumnInfo column : getColumnList()) {
            argStringBuffer.append("		<result column=\"" + column.getColumnName() + "\" property=\"" + column.getFieldName() + "\" />");
            argStringBuffer.append(BLANK_LINE);
        }
        argStringBuffer.append("	</resultMap>");
    }

    protected void getColumnAlias(StringBuffer argStringBuffer) {
        argStringBuffer.append("	<sql id=\"" + getSqlId() + "\">");
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("		");
        argStringBuffer.append(getColumnsWithoutId());
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("	</sql>");
    }

    /***
     * <sql> elements in SQL map configuration file. The generated columns doesn't contain the "id" column.
     * 
     * @param argStringBuffer
     */
    protected void getColumns(StringBuffer argStringBuffer) {
        argStringBuffer.append("	<sql id=\"" + getSqlResult() + "\">");
        argStringBuffer.append(BLANK_LINE);
        int i = 0;
        int length = getColumnList().size();
        List<IColumnInfo> list = getColumnList();
        if (list != null) {
            IColumnInfo column = null;
            for (; i < length; i++) {
                column = list.get(i);
                argStringBuffer.append("		" + column.getColumnName() + " as " + column.getFieldName() + " ,");
                if (i == length - 1) {
                    argStringBuffer.setLength(argStringBuffer.length() - 1);
                }
                argStringBuffer.append(BLANK_LINE);
            }
        }
        argStringBuffer.append("	</sql>");
    }

    private StringBuffer columnsWithoutId_ = null;

    /***
     * Get all String columns without id.
     * 
     * @return all String columns without id
     */
    protected String getColumnsWithoutId() {
        if (columnsWithoutId_ == null) {
            columnsWithoutId_ = new StringBuffer();
            for (IColumnInfo column : getColumnList()) {
                if (!"id".equals(column.getColumnName())) {
                    columnsWithoutId_.append(column.getColumnName() + ", ");
                }
            }
            if (columnsWithoutId_.length() > 2) {
                columnsWithoutId_.setLength(columnsWithoutId_.length() - 2);
            }
            return columnsWithoutId_.toString();
        } else {
            return columnsWithoutId_.toString();
        }
    }

    private StringBuffer columnsWithoutIdWithAlias_ = null;

    /***
     * Get all String columns which contains table's alias name.
     * 
     * @return all String columns which contains table's alias name.
     */
    protected String getAllColumnsWithTableAlais() {
        if (columnsWithoutIdWithAlias_ == null) {
            columnsWithoutIdWithAlias_ = new StringBuffer();
            for (IColumnInfo column : getColumnList()) {
                columnsWithoutIdWithAlias_.append(DEFAULT_TABLE_NAME_ALIAS + "." + column.getColumnName() + ", ");
            }
            if (columnsWithoutIdWithAlias_.length() > 2) {
                columnsWithoutIdWithAlias_.setLength(columnsWithoutIdWithAlias_.length() - 2);
            }
            return columnsWithoutIdWithAlias_.toString();
        } else {
            return columnsWithoutIdWithAlias_.toString();
        }
    }

    /***
     * <insert> elements in SQL map configuration file.
     * 
     * @param argStringBuffer
     */
    protected void getInsert(StringBuffer argStringBuffer) {
        insertStatementId_ = getNameSpaceName() + ".insert" + getBusinessName();
        argStringBuffer.append("	<insert id=\"" + insertStatementId_ + "\"  parameterClass=\"" + getAliasName() + "\">");
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("		INSERT INTO " + getTable() + "(<include refid=\"" + getSqlId() + "\"/>)");
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("		VALUES(");
        for (IColumnInfo column : getColumnList()) {
            if (!"id".equals(column.getColumnName())) {
                argStringBuffer.append("#" + column.getFieldName() + "#, ");
            }
        }
        argStringBuffer.setLength(argStringBuffer.length() - 2);
        argStringBuffer.append(")");
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("		<selectKey resultClass=\"long\" keyProperty=\"id\">" + BLANK_LINE);
        argStringBuffer.append("			SELECT LAST_INSERT_ID()" + BLANK_LINE);
        argStringBuffer.append("		</selectKey>" + BLANK_LINE);
        argStringBuffer.append("	</insert>");
    }

    /***
     * <update> elements in SQL map configuration file.
     * 
     * @param argStringBuffer
     */
    protected void getUpdate(StringBuffer argStringBuffer) {
        updateStatementId_ = getNameSpaceName() + ".update" + getBusinessName();
        argStringBuffer.append("	<update id=\"" + updateStatementId_ + "\"  parameterClass=\"" + getAliasName() + "\">");
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("		UPDATE " + getTable() + BLANK_LINE);
        argStringBuffer.append("		   SET");
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("		   <dynamic prepend=\" \">" + BLANK_LINE);
        argStringBuffer.append("				<isParameterPresent>" + BLANK_LINE);
        boolean isFirst = true;
        for (IColumnInfo column : getColumnList()) {
            if (!"id".equals(column.getColumnName())) {
                if (isFirst) {
                    isFirst = false;
                    argStringBuffer.append("			   		<isNotNull  property=\"" + column.getFieldName() + "\" prepend=\" \">" + column.getColumnName() + " = #" + column.getFieldName() + "#</isNotNull>");
                } else {
                    argStringBuffer.append("			   		<isNotNull  property=\"" + column.getFieldName() + "\" prepend=\", \">" + column.getColumnName() + " = #" + column.getFieldName() + "#</isNotNull>");
                }
                argStringBuffer.append(BLANK_LINE);
            }
        }
        argStringBuffer.append("				</isParameterPresent>" + BLANK_LINE);
        argStringBuffer.append("		   </dynamic>" + BLANK_LINE);
        argStringBuffer.append("	     WHERE id = #id#");
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("	</update>");
    }

    /***
     * <select> elements in SQL map configuration file.
     * 
     * @param argStringBuffer sql statement.
     */
    protected void getSelect(StringBuffer argStringBuffer) {
        String id = getNameSpaceName() + ".select" + getBusinessName();
        genernateSelect(argStringBuffer, id, false, "java.util.HashMap");
    }

    /***
     * <select> elements in SQL map configuration file.
     * 
     * @param argStringBuffer sql statement.
     */
    protected void getSelectById(StringBuffer argStringBuffer) {
        selectByIdStatementId_ = getNameSpaceName() + ".select" + getBusinessName() + "ById";
        genernateSelect(argStringBuffer, selectByIdStatementId_, true, "java.lang.Long");
    }

    /***
     * Genernate the select statement.
     * 
     * @param argStringBuffer sql statement.
     * @param argSelectId the ID of the select element.
     * @param argHasId whether contains the id column.
     */
    private void genernateSelect(StringBuffer argStringBuffer, String argSelectId, boolean argHasId, String argParameterType) {
        argStringBuffer.append("  	<select id=\"" + argSelectId + "\" resultClass=\"" + getAliasName() + "\" parameterClass=\"" + argParameterType + "\">");
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("		SELECT " + BLANK_LINE);
        argStringBuffer.append("		   id, <include refid=\"" + getSqlId() + "\"/> ");
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("		FROM " + getTable());
        argStringBuffer.append(BLANK_LINE);
        if (!argHasId) {
            argStringBuffer.append("				<dynamic prepend=\"WHERE\">");
            argStringBuffer.append(BLANK_LINE);
            argStringBuffer.append("					<isParameterPresent>");
            argStringBuffer.append(BLANK_LINE);
        }
        for (IColumnInfo column : getColumnList()) {
            if ("id".equals(column.getColumnName()) && argHasId) {
                argStringBuffer.append("		WHERE");
                argStringBuffer.append(BLANK_LINE);
                argStringBuffer.append("			" + column.getColumnName() + " = #" + column.getFieldName() + "#");
                argStringBuffer.append(BLANK_LINE);
                break;
            } else {
                if (argHasId) {
                    continue;
                } else {
                    argStringBuffer.append("						<isNotNull  property=\"" + column.getFieldName() + "\" prepend=\"AND \">" + column.getColumnName() + " = #" + column.getFieldName() + "#</isNotNull>");
                    argStringBuffer.append(BLANK_LINE);
                }
            }
        }
        if (!argHasId) {
            argStringBuffer.append("					</isParameterPresent>");
            argStringBuffer.append(BLANK_LINE);
            argStringBuffer.append("				</dynamic>");
            argStringBuffer.append(BLANK_LINE);
        }
        argStringBuffer.append("  	</select>");
    }

    /***
     * <select> elements in SQL map configuration file for pagination.
     * 
     * @param argStringBuffer
     */
    protected void getSelectForPagination(StringBuffer argStringBuffer) {
        paginStatementId_ = getNameSpaceName() + ".select" + getBusinessName() + "ForPagin";
        argStringBuffer.append("    <select id=\"" + paginStatementId_ + "\" resultClass=\"" + getAliasName() + "\" parameterClass=\"java.util.HashMap\">");
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("        SELECT " + BLANK_LINE);
        argStringBuffer.append("           " + getAllColumnsWithTableAlais());
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("        FROM " + BLANK_LINE);
        argStringBuffer.append("             " + "(SELECT id FROM " + getTable());
        argStringBuffer.append(BLANK_LINE);
        appendDynamicConditions(argStringBuffer);
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("        ORDER BY" + BLANK_LINE);
        argStringBuffer.append("                    gmt_create DESC" + BLANK_LINE);
        argStringBuffer.append("        LIMIT  #startRow#, #pageSize#) AS a, " + getTable() + " AS " + DEFAULT_TABLE_NAME_ALIAS + BLANK_LINE);
        argStringBuffer.append("        FORCE INDEX(PRIMARY)" + BLANK_LINE);
        argStringBuffer.append("        WHERE a.id = t.id" + BLANK_LINE);
        argStringBuffer.append("    </select>");
    }

    protected String getDynamicConditions() {
        StringBuffer sb = new StringBuffer();
        appendDynamicConditions(sb);
        return sb.toString();
    }

    protected void appendDynamicConditions(StringBuffer argStringBuffer) {
        argStringBuffer.append("                    <dynamic prepend=\"WHERE\">");
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("                        <isParameterPresent>");
        argStringBuffer.append(BLANK_LINE);
        for (IColumnInfo column : getColumnList()) {
            if (!"id".equals(column.getColumnName())) {
                argStringBuffer.append("                            <isNotNull  property=\"" + column.getFieldName() + "\" prepend=\"AND \">" + column.getColumnName() + " = #" + column.getFieldName() + "#</isNotNull>");
                argStringBuffer.append(BLANK_LINE);
            }
        }
        argStringBuffer.append("                        </isParameterPresent>");
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("                    </dynamic>");
    }

    /***
     * Get total count for pagination.
     * 
     * @param argStringBuffer
     */
    protected void getCountForPagination(StringBuffer argStringBuffer) {
        String sqlId = getNameSpaceName() + ".select" + getBusinessName() + "ForPagin" + "TotalCount";
        argStringBuffer.append("  	<select id=\"" + sqlId + "\" resultClass=\"java.lang.Integer\" parameterClass=\"java.util.HashMap\">");
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("		SELECT " + BLANK_LINE);
        argStringBuffer.append("				count(1) " + BLANK_LINE);
        argStringBuffer.append("		FROM " + getTable() + BLANK_LINE);
        argStringBuffer.append("					<dynamic prepend=\"WHERE\">");
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("						<isParameterPresent>");
        argStringBuffer.append(BLANK_LINE);
        for (IColumnInfo column : getColumnList()) {
            argStringBuffer.append("							<isNotNull  property=\"" + column.getFieldName() + "\" prepend=\"AND \">" + column.getColumnName() + " = #" + column.getFieldName() + "#</isNotNull>");
            argStringBuffer.append(BLANK_LINE);
        }
        argStringBuffer.append("						</isParameterPresent>");
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("					</dynamic>");
        argStringBuffer.append(BLANK_LINE);
        argStringBuffer.append("  	</select>");
    }

    private String businessName = null;

    /***
     * Get business name.
     * 
     * @return business name.
     */
    protected String getBusinessName() {
        if (businessName != null) {
            return businessName;
        }
        businessName = doClassName_;
        int index = businessName.lastIndexOf(".");
        if (index >= 0) {
            businessName = businessName.substring(index + 1);
        }
        String doSuffix = DaoServiceConfigurationManager.getProperty(CODEGEN_DAO_DO_SUFFIX);
        if (businessName.endsWith(doSuffix)) {
            businessName = businessName.substring(0, businessName.lastIndexOf(doSuffix));
        }
        return businessName;
    }

    protected String getNameSpaceName() {
        return getDaoName();
    }

    /***
     * Get alias name for the DO configuration object. <typeAlias alias="**Alias">
     * 
     * @return alias name of the DO.
     */
    protected String getAliasName() {
        return getBusinessName() + "Alias";
    }

    /***
     * Get ResultMap name for the DO configuration object. <resultMap id="**ResultMap"/>
     * 
     * @return ResultMap name of the DO.
     */
    protected String getResultMapId() {
        return getBusinessName() + "ResultMap";
    }

    /***
     * Get SQL id for the DO configuration object.
     * 
     * @return SQL id for the DO.
     */
    protected String getSqlId() {
        return getBusinessName() + "-columns-without-id";
    }

    protected String getSqlResult() {
        return getBusinessName() + "-result";
    }

    public String getDaoName() {
        return daoName_;
    }

    public void setDaoName(String daoName) {
        this.daoName_ = daoName;
    }

    public String getDoClassName() {
        return doClassName_;
    }

    public void setDoClassName(String doClassName) {
        this.doClassName_ = doClassName;
    }

    public List<IColumnInfo> getColumnList() {
        return columnList;
    }

    public void setColumnList(List<IColumnInfo> columnList) {
        this.columnList = columnList;
    }

    public String getTable() {
        return table_;
    }

    public void setTable(String table) {
        this.table_ = table;
    }

    @Override
    public String getFileType() {
        return ".xml";
    }

    @Override
    public String getEndElement() {
        return "</sqlMap>";
    }

    public String getInsertStatementId() {
        return insertStatementId_;
    }

    public String getUpdateStatementId() {
        return updateStatementId_;
    }

    public String getPaginStatementId() {
        return paginStatementId_;
    }

    public String getSelectByIdStatementId() {
        return selectByIdStatementId_;
    }

    public void setPaginStatementId(String paginStatementId) {
        this.paginStatementId_ = paginStatementId;
    }

    public VelocityEngine getVelocityEngine() {
        return velocityEngine;
    }

    public void setVelocityEngine(VelocityEngine velocityEngine) {
        this.velocityEngine = velocityEngine;
    }

    public void setInsertStatementId(String insertStatementId) {
        this.insertStatementId_ = insertStatementId;
    }
}
