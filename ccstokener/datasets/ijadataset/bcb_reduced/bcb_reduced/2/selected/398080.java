package com.daffodilwoods.daffodildb.utils.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.List;
import java.io.*;
import java.net.URL;
import com.daffodilwoods.database.resource.*;
import com.daffodilwoods.database.utility.P;
import com.daffodilwoods.daffodildb.utils.DBStack;
import java.net.*;
import java.util.TreeMap;

/**
 * This class is used for making ProductionRules Hierarchy by reading coding.txt file
 * Rules made by this class is used for parsing.
 */
public class ProductionRuleParser implements ProductionRuleParserConstants {

    /**
    * HashMap contains entries of definition of rules read from file.
    */
    private HashMap fileEntries;

    /**
    * HashMap contains entries of all rules.
    * Key being the nameOfRule and Value being ProductionRules
    */
    private HashMap rulea;

    /**
    * HashMap containing entries of all Token Rules.
    * Key being the nameOfRule and Value being ProductionRules
    * This hashMap is used for making TokenProductionRules.
    */
    HashMap allRules;

    /**
    * Stack held to set recurssive flag in ProductionRules
    */
    DBStack recursion;

    /**
    * Boolean Flag For Token, If True then token ProductionRules are being made
    */
    boolean tokenFlag;

    /**
    * Vector used to hold rules withcomparablewithhashmap
    */
    protected List withHashMapWithBinary;

    /**
    * Stack containing HashMap Rules , Rules having multiple parents.
    * This rules are read from file withHashMapRules.
    */
    private ArrayList withHashMapRules;

    private List nonHashMap;

    /**
    * Vector for NonReserved Word and Reserved Word
    */
    List reservedWord;

    List nonReservedWord;

    private String reserved = "SRESERVEDWORD1206543922";

    private String nonReserved = "SNONRESERVEDWORD136444255";

    URL url;

    ClassLoader classLoader;

    public static final TokenGenerator tk = new TokenGenerator();

    public ProductionRuleParser(ClassLoader classLoader0) throws DException {
        this(null, classLoader0);
    }

    public ProductionRuleParser(URL urlPath, ClassLoader classLoader0) throws DException {
        classLoader = classLoader0;
        url = urlPath;
        initialize();
        initializeTokenGeneratorMaps();
    }

    private void initializeTokenGeneratorMaps() throws DException {
        tk.reserveWords = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        String obj[] = new String[] { "ABSOLUTE", "ADD", "ADMIN", "AFTER", "AGGREGATE", "ALL", "ALLOCATE", "ALTER", "AND", "ANY", "ARE", "ARRAY", "AS", "ASC", "ASSERTION", "AT", "AUTHORIZATION", "BEFORE", "BEGIN", "BIGINT", "BINARY", "BIT", "BLOB", "BOOLEAN", "BOTH", "BREADTH", "BY", "BYTE", "CALL", "CASCADE", "CASCADED", "CASE", "CAST", "CATALOG", "CHAR", "CHARACTER", "CHECK", "CLASS", "CLOB", "CLOSE", "COLLATE", "COLLATION", "COLUMN", "COMMIT", "COMPLETION", "CONNECT", "CONNECTION", "CONSTRAINT", "CONSTRAINTS", "CONSTRUCTOR", "CONTINUE", "CORRESPONDING", "CREATE", "CROSS", "CUBE", "CURRENT", "CURRENT_DATABASE", "CURRENT_DATE", "CURRENT_PATH", "CURRENT_ROLE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURRENTVAL", "CURSOR", "CYCLE", "DATE", "DAY", "DEALLOCATE", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DEFERRABLE", "DEFERRED", "DELETE", "DEPTH", "DEREF", "DESC", "DESCRIBE", "DESCRIPTOR", "DESTROY", "DESTRUCTOR", "DETERMINISTIC", "DICTIONARY", "DIAGNOSTICS", "DISCONNECT", "DISTINCT", "DOMAIN", "DOUBLE", "DROP", "DYNAMIC", "EACH", "ELSE", "END", "EQUALS", "ESCAPE", "EVERY", "EXCEPT", "EXCEPTION", "EXEC", "EXECUTE", "EXTERNAL", "FALSE", "FETCH", "FIRST", "FLOAT", "FOR", "FOREIGN", "FOUND", "FROM", "FREE", "FULL", "FUNCTION", "GENERAL", "GET", "GLOBAL", "GO", "GOTO", "GRANT", "GROUP", "GROUPING", "HASRECORD", "HAVING", "HOST", "HOUR", "IDENTITY", "IGNORE", "IMMEDIATE", "IN", "INDICATOR", "INITIALIZE", "INITIALLY", "INNER", "INOUT", "INPUT", "INSERT", "INT", "INTEGER", "INTERSECT", "INTERVAL", "INTO", "IS", "ISOLATION", "ITERATE", "JOIN", "KEY", "LANGUAGE", "LARGE", "LAST", "LATERAL", "LEFT", "LESS", "LEVEL", "LIKE", "LOCAL", "LOCALTIME", "LOCALTIMESTAMP", "LOCATOR", "LONG", "LONGINT", "MAP", "MATCH", "MINUTE", "MODIFIES", "MODIFY", "MODULE", "MONTH", "NAMES", "NATIONAL", "NATURAL", "NCHAR", "NCLOB", "NEW", "NEXTVAL", "NO", "NONE", "NOT", "NULL", "NUMBER", "NUMERIC", "OF", "OFF", "OLD", "ON", "ONLY", "OPEN", "OR", "ORDER", "ORDINALITY", "OUT", "OUTER", "OUTPUT", "PAD", "PARAMETER", "PARTIAL", "PATH", "POSTFIX", "PRECISION", "PREORDER", "PREPARE", "PRESERVE", "PRIMARY", "PRIOR", "PRIVILEGES", "PROCEDURE", "PUBLIC", "READ", "READS", "REAL", "RECURSIVE", "REF", "REFERENCES", "REFERENCING", "RELATIVE", "RELEASE", "RESTRICT", "RETURN", "RETURNS", "REVOKE", "RIGHT", "ROLLBACK", "ROLLUP", "ROUTINE", "ROW", "ROWNUM", "ROWS", "SAVEPOINT", "SCHEMA", "SCROLL", "SCOPE", "SEARCH", "SECOND", "SECTION", "SELECT", "SEQUENCE", "SESSION", "SESSION_USER", "SET", "SETS", "SIZE", "SMALLINT", "SOME", "SPACE", "SPECIFIC", "SPECIFICTYPE", "SQL", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "START", "STATEMENT", "STATIC", "STRUCTURE", "SYSTEM_USER", "TABLE", "TEMPORARY", "TERMINATE", "THAN", "THEN", "TIME", "TIMESTAMP", "TIMEZONE_HOUR", "TIMEZONE_MINUTE", "TINYINT", "TO", "TRAILING", "TRANSACTION", "TRANSLATION", "TREAT", "TRIGGER", "TRUE", "UNDER", "UNION", "UNIQUE", "UNKNOWN", "UNNEST", "UPDATE", "USAGE", "USER", "USING", "VALUES", "VARBINARY", "VARCHAR", "VARCHAR2", "VARIABLE", "VARYING", "VIEW", "WHENEVER", "WHERE", "WITH", "WITHOUT", "WORK", "WRITE", "ZONE", "CONDITION", "DO", "ELSEIF", "EXIT", "HANDLER", "IF", "ITERATE", "LEAVE", "LOOP", "REDO", "RESIGNAL", "SIGNAL", "UNDO", "UNTIL", "WHILE", "JAVAPARAMETER", "STRING", "SYSDATE" };
        for (int i = 0; i < obj.length; i++) tk.reserveWords.put(obj[i], obj[i]);
        obj = new String[] { "NEXT", "OBJECT", "ROLE", "ACTION", "COUNT", "AVG", "LIMIT", "MAX", "OPERATION", "PREFIX", "RESULT", "SUM", "WHEN", "MIN", "REPEAT", "CONTAINS", "DATA", "ABS", "ACOS", "ADA", "ALIAS", "ASENSITIVE", "ASCII", "ASSIGNMENT", "ASENSITIVE", "ASIN", "ASYMMETRIC", "ATAN", "ATAN2", "ATOMIC", "ATTRIBUTE", "AUTOINCREMENT", "B", "BETWEEN", "BIT_LENGTH", "BITVAR", "C", "CALLED", "CARDINALITY", "CATALOG_NAME", "CEILING", "CHAIN", "CHAR_LENGTH", "CHARACTER_LENGTH", "CHARACTER_SET_CATALOG", "CHARACTER_SET_NAME", "CHARACTERISTICS", "CHARACTER_SET_SCHEMA", "CHECKED", "CLASS_ORIGIN", "COALESCE", "COBOL", "COLLATION_CATALOG", "COLLATION_NAME", "COLLATION_SCHEMA", "COLUMN_NAME", "COMMAND_FUNCTION", "COMMAND_FUNCTION_CODE", "COMMITTED", "CONCAT", "CONDITION_NUMBER", "CONNECTION_NAME", "CONSTRAINT_CATALOG", "CONSTRAINT_NAME", "CONSTRAINT_SCHEMA", "CONVERT", "COS", "COT", "COUNTRY", "CURDATE", "CURSOR_NAME", "CURTIME", "CURTIMESTAMP", "DATABASE", "DATESPAN", "DATETIME_INTERVAL_CODE", "DATETIME_INTERVAL_PRECISION", "DAYNAME", "DAYOFMONTH", "DAYOFWEEK", "DAYOFYEAR", "DEFINED", "DEFINER", "DEGREES", "DERIVED", "DIFFERENCE", "DISPATCH", "DYNAMIC_FUNCTION", "DYNAMIC_FUNCTION_CODE", "EXISTING", "EXISTS", "EXP", "EXTRACT", "FILESIZE", "FILEGROWTH", "FINAL", "FLOOR", "FORTRAN", "FULLTEXT", "G", "GENERATED", "GRANTED", "HIERARCHY", "HOLD", "IFNULL", "IMPLEMENTATION", "INCREMENT", "INDEX", "INFIX", "INSENSITIVE", "INSTANCE", "INSTANTIABLE", "INVOKER", "K", "KEY_MEMBER", "KEY_TYPE", "LCASE", "LENGTH", "LOCATE", "LOG", "LOWER", "LTRIM", "M", "MATERIALIZED", "MAXVALUE", "MESSAGE_LENGTH", "MESSAGE_OCTET_LENGTH", "MESSAGE_TEXT", "METHOD", "MINVALUE", "MOD", "MONTHNAME", "MORE", "MOUNT", "MUMPS", "NAME", "NOMAXVALUE", "NOW", "NOCYCLE", "NOMINVALUE", "NOORDER", "NULLABLE", "NULLIF", "OCTET_LENGTH", "OPTIONS", "ORDERING", "OVERLAPS", "OVERLAY", "OVERRIDING", "PASCAL", "PARAMETER_MODE", "OPTION", "PARAMETERS", "PARAMETER_NAME", "PARAMETER_ORDINAL_POSITION", "PARAMETER_SPECIFIC_CATALOG", "PARAMETER_SPECIFIC_NAME", "PARAMETER_SPECIFIC_SCHEMA", "PASSWORD", "PI", "PLI", "PLACING", "POSITION", "POWER", "RADIANS", "RAND", "REPEATABLE", "REPEAT", "REPLACE", "RETURNED_LENGTH", " RETURNED_OCTET_LENGTH", "RETURNED_SQLSTATE", "ROUND", "ROUTINE_CATALOG", "ROUTINE_NAME", "ROUTINE_SCHEMA", "ROW_COUNT", "RTRIM", "SCALE", "SCHEMA_NAME", "SECURITY", "SELF", "SENSITIVE", "SERIALIZABLE", "SERVER_NAME", "SIMPLE", "SIGN", "SIN", "SOUNDEX", "SOURCE", "SPECIFIC_NAME", "SIMILAR", "SQL_TSI_DAY", "SQL_TSI_FRAC_SECOND", "SQL_TSI_HOUR", "SQL_TSI_MINUTE", "SQL_TSI_MONTH", "SQL_TSI_QUARTER", "SQL_TSI_SECOND", "SQL_TSI_WEEK", "SQL_TSI_YEAR", "SQRT", "SUBLIST", "SUBSTRING", "SUPPORT", "STATE", "STYLE", "SUBCLASS_ORIGIN", "SYMMETRIC", "SYSTEM", "TABLE_NAME", "TAN", "THROW", "TIMESTAMPADD", "TIMESTAMPDIFF", "TOP", "TRANSACTIONS_COMMITTED", "TRANSACTIONS_ROLLED_BACK", "TRANSACTION_ACTIVE", "TRANSFORM", "TRANSFORMS", "TRANSLATE", "TRIGGER_CATALOG", "TRIGGER_SCHEMA", "TRIGGER_NAME", "TRIM", "TRUNCATE", "TYPE", "UCASE", "UNICODE", "UNCOMMITTED", "UNNAMED", "UPPER", "USER_DEFINED_TYPE_CATALOG", "USER_DEFINED_TYPE_NAME", "USER_DEFINED_TYPE_SCHEMA", "VALUE", "WEEK", "X", "YEAR", "CONDITION_IDENTIFIER", "ISOPEN", "NOTFOUND", "A", "C", "D", "E", "F", "a", "b", "c", "d", "e", "f", "x", "TRUNC", "INITCAP", "DECODE", "DUMP", "SYS_CONTEXT", "LEADING", "TRAILING", "BOTH" };
        tk.nonReserveWords = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < obj.length; i++) tk.nonReserveWords.put(obj[i], obj[i]);
    }

    private void initialize() throws DException {
        fileEntries = new HashMap();
        rulea = new HashMap();
        allRules = new HashMap();
        recursion = new DBStack();
        withHashMapRules = new ArrayList();
        withHashMapWithBinary = new DBStack();
        nonHashMap = vectorForBestAndNonBest.getRulesWithHashMapToBeIncludedAsNonHashMap();
        readFromFile1();
        readFileForWithHashMapRules1();
        testRetrievalOfFiles();
    }

    /**
    *  For reading HashMapRules from file in to an Stack withHashMapRules.
    */
    private void readFileForWithHashMapRules1() throws DException {
        URL url1 = null;
        if (url == null) {
            url1 = getClass().getResource("/com/daffodilwoods/daffodildb/utils/parser/withHashMapRules.obj");
            if (url1 == null) {
                throw new DException("DSE0", new Object[] { "withHashMapRules.obj file is missing in classpath" });
            }
        } else {
            try {
                url1 = new URL(url.getProtocol() + ":" + url.getPath() + "/withHashMapRules.obj");
            } catch (MalformedURLException ex) {
                throw new DException("DSE0", new Object[] { ex });
            }
        }
        try {
            ObjectInputStream ooin = new ObjectInputStream(new BufferedInputStream(url1.openStream()));
            withHashMapRules = (ArrayList) ooin.readObject();
            ooin.close();
        } catch (ClassNotFoundException ex1) {
            throw new DException("DSE0", new Object[] { ex1 });
        } catch (IOException ex1) {
            throw new DException("DSE0", new Object[] { ex1 });
        }
    }

    /**
    *  For reading definition from coding file in an hashMap fileEntries.
    *  This definition does not uses RandomAccessFile.
    */
    private void readFromFile1() throws DException {
        URL url1 = null;
        if (url == null) {
            url = getClass().getResource("/com/daffodilwoods/daffodildb/utils/parser/parser.schema");
            try {
                url = new URL(url.getProtocol() + ":" + url.getPath().substring(0, url.getPath().indexOf("/parser.schema")));
            } catch (MalformedURLException ex2) {
                ex2.printStackTrace();
                throw new DException("DSE0", new Object[] { ex2 });
            }
            try {
                url1 = new URL(url.getProtocol() + ":" + url.getPath() + "/parser.schema");
            } catch (MalformedURLException ex) {
                throw new DException("DSE0", new Object[] { ex });
            }
            if (url1 == null) {
                throw new DException("DSE0", new Object[] { "Parser.schema file is missing in classpath." });
            }
        } else {
            try {
                url1 = new URL(url.getProtocol() + ":" + url.getPath() + "/parser.schema");
            } catch (MalformedURLException ex) {
                throw new DException("DSE0", new Object[] { ex });
            }
        }
        ArrayList arr1 = null;
        StringBuffer rule = null;
        try {
            LineNumberReader raf = new LineNumberReader(new BufferedReader(new InputStreamReader(url1.openStream())));
            arr1 = new ArrayList();
            rule = new StringBuffer("");
            while (true) {
                String str1 = raf.readLine();
                if (str1 == null) {
                    break;
                }
                String str = str1.trim();
                if (str.length() == 0) {
                    if (rule.length() > 0) {
                        arr1.add(rule.toString());
                    }
                    rule = new StringBuffer("");
                } else {
                    rule.append(" ").append(str);
                }
            }
            raf.close();
        } catch (IOException ex1) {
            ex1.printStackTrace();
            throw new DException("DSE0", new Object[] { ex1 });
        }
        if (rule.length() > 0) arr1.add(rule.toString());
        for (int i = 0; i < arr1.size(); i++) {
            String str = (String) arr1.get(i);
            int index = str.indexOf("::=");
            if (index == -1) {
                P.pln("Error " + str);
                throw new DException("DSE0", new Object[] { "Rule is missing from parser.schema" });
            }
            String key = str.substring(0, index).trim();
            String value = str.substring(index + 3).trim();
            Object o = fileEntries.put(key, value);
            if (o != null) {
                new Exception("Duplicate Defination for Rule [" + key + "] Value [" + value + "] Is Replaced By  [" + o + "]").printStackTrace();
            }
        }
    }

    /**
    * This Method is used for Making Entry in HashMap rulea of all rules
    * This method is also used to load all rules classes.
    */
    private void makeEntryInHashMap(ProductionRules production, String className, String nameOfRule) {
        if (production instanceof OptionalProductionRules || production instanceof OptionalProductionRulesWithHashMap) return;
        if (!(production instanceof RepetitiveProductionRules || production instanceof OrProductionRules || production instanceof RepetitiveProductionRulesWithHashMap || production instanceof OrProductionRulesWithHashMap) && tokenFlag) allRules.put(nameOfRule, production);
    }

    /**
    * This Function Initialize the name Of rule.
    * for example : rulename abc-ss is changed as sql.abc_ss
    * Initialised name of rule is used to load class into memory using reflection.
    */
    String initailizeNameOfClass(String nameRule) {
        return updateNameOfRule(nameRule);
    }

    /**
    * This method updates the nameOfRule to a desired format.
    * For example : rulename abc-ss is changed as abc_ss
    */
    private String updateNameOfRule(String nameRule) {
        char[] value = nameRule.toCharArray();
        StringBuffer str = new StringBuffer();
        for (int i = 0, count = value.length; i < count; i++) if (value[i] == '-') str.append('_'); else if (!(value[i] == ' ' || value[i] == ':' || value[i] == '(' || value[i] == ')' || value[i] == '\\' || value[i] == '\'')) str.append(value[i]);
        return str.toString();
    }

    /**
    * This method is used for making ProductionRules of ruleName passed.
    * RuleNamed "charater string literal" is made as CharacterStringLiteralProductionRules
    * Rules in KeyWord rule are made as KeyWordProductionRules.
    * 0 type is set for StringProductionRules
    * 1 type is set for KeyWordProductionRules
    * 2 type is set for StringProductionRulesWithoutSpace
    */
    public ProductionRules getProductionRule(String productionRuleName) throws DException {
        ProductionRules pr = getProductionRule2(productionRuleName);
        modifyProductionRuleName(pr);
        return pr;
    }

    private ProductionRules getProductionRule2(String productionRuleName) throws DException {
        productionRuleName = productionRuleName.trim();
        if (allRules != null) {
            Object obj = allRules.get(productionRuleName);
            if (obj != null) return (ProductionRules) obj;
        }
        String temp = productionRuleName;
        ProductionRules production = (ProductionRules) rulea.get(productionRuleName);
        if (production == null) {
            production = getProductionRuleObjectType(productionRuleName);
            recursion.push(productionRuleName);
            productionRuleName = getNameOfRule(production, productionRuleName, -1);
            production.setProductionRuleName(productionRuleName);
            production.ruleKey = production.nameOfRule;
            rulea.put(productionRuleName, production);
            makeEntryInHashMap(production, production.className, production.getProductionRuleName());
            if (productionRuleName.equalsIgnoreCase(REGULARIDENTIFIER)) {
                makeRangeProductionRules(production);
            } else if (productionRuleName.equalsIgnoreCase(CHARACTERSTRINGLITERAL)) {
                makeCharacterStringLiteralProductionRules('\'', production);
            } else if (productionRuleName.equalsIgnoreCase(BITSTRINGLITERAL)) {
                makeStringLiteralProductionRules(BinaryStringLiteralProductionRules.BITSTRINGTYPE, production);
            } else if (productionRuleName.equalsIgnoreCase(HEXSTRINGLITERAL)) {
                makeStringLiteralProductionRules(BinaryStringLiteralProductionRules.HEXSTRINGTYPE, production);
            } else if (productionRuleName.equalsIgnoreCase(DELIMITEDIDENTIFIER)) {
                makeCharacterStringLiteralProductionRules('"', production);
            } else {
                if (ifRuleForKeyWordProductionRule(temp)) getProductionRuleObject(production, temp, 1); else getProductionRuleObject(production, temp, 0);
            }
            recursion.pop();
        }
        if (!tokenFlag && !allRules.containsKey(temp)) CheckRecursive(temp, production);
        return production;
    }

    /**
    * RangeProductionRules
    */
    private void makeRangeProductionRules(ProductionRules production) {
        ArrayList arr = new ArrayList(1);
        RangeProductionRules rule = new RangeProductionRules(classLoader);
        rule.setProductionRuleName("SpecialHandlingRule");
        rule.ruleKey = "SpecialHandlingRule";
        arr.add(rule);
        production.setProductionRules(arr);
    }

    /**
   * Function for getting characterstringliteralproductionrules
   * c == ''' FOR CHARACTERSTRINGLITERAL
   * c == '"' FOR DELIMITEDIDENTIFIER
   */
    private void makeCharacterStringLiteralProductionRules(char c, ProductionRules production) {
        ArrayList arr = new ArrayList(1);
        CharacterStringLiteralProductionRules rule = new CharacterStringLiteralProductionRules(c, classLoader);
        rule.setProductionRuleName("SpecialHandlingRule");
        rule.ruleKey = "SpecialHandlingRule";
        arr.add(rule);
        production.setProductionRules(arr);
    }

    private void makeStringLiteralProductionRules(int flag, ProductionRules production) {
        ArrayList arr = new ArrayList(1);
        BinaryStringLiteralProductionRules rule = new BinaryStringLiteralProductionRules(classLoader);
        rule.setProductionRuleName("StringLiteralHandlingRule");
        rule.ruleKey = "StringLiteralHandlingRule";
        arr.add(rule);
        production.setProductionRules(arr);
        rule.setClassName(flag);
    }

    /**
    * This method is used to set recursive flag true for rules which are recursive.
    */
    private void CheckRecursive(String productionRuleName, ProductionRules production) {
        int index = recursion.indexOf(productionRuleName);
        if (index == -1) return;
        int size = recursion.size() - 1;
        while (index <= size) {
            ProductionRules productionRule = (ProductionRules) rulea.get(recursion.get(index));
            if (!productionRule.recursiveflag) productionRule.recursiveflag = true;
            ++index;
        }
    }

    /**
    * This method is used to get Type of ProductionRule of ruleName passed.
    */
    private ProductionRules getProductionRuleObjectType(String productionRuleName) throws DException {
        return makingProductionRuleObject(makeStack(getProductionRuleString(productionRuleName)), productionRuleName);
    }

    /**
    * This Method is used for getting ProductionRule String from HashMap of ruleName passed.
    */
    private String getProductionRuleString(String name) throws DException {
        name = name.trim();
        if (name.length() == 0) throw new DException("DSE885", new Object[] { name });
        StringBuffer str = new StringBuffer("<");
        str.append(name).append(">");
        String toFollow = (String) fileEntries.get(str.toString());
        if (toFollow == null) throw new DException("DSE324", new Object[] { name });
        return toFollow;
    }

    /**
    * This method is used for getting ProductionRules for an object passed with it's ruleName.
    * Type passed is used to differentiate types of StringProductionRules.
    */
    private ProductionRules getProductionRuleObject(ProductionRules production, String productionRuleName, int type) throws DException {
        char queryArray[] = getProductionRuleString(productionRuleName).toCharArray();
        makingProductionRules(production, queryArray, type);
        production.setProductionRuleName(productionRuleName);
        return production;
    }

    /**
    * Assembling on basis of OrProductionRule,UnionProductionRule,OptionalProductionRule,RepititiveProductionRule
    *  1. If keywords occur or words with capital letters then object of StringProductionRules
    *  2. If string ocuur starting with < and ending with > then object of ProductionRules
    *  3. Assembling of these above two occur on the basis of
    *     a) if","symbol occurs then type is setted as OrProductionRule
    *     b) if no","symbol occurs then type is setted as UnionProductionRule.
    *           NOW , till string ends add all objects of any type in arrayList
    *           and then set arrayList acc to type.
    *  Now if [ occurs then productionRule inside [] are added as OptionalproductionRule
    *  Now if ... occurs then object of repetitive production rules.
    *  StringProductionRules is made of different types.
    *  Argument passed is ProductionRules Object for which we are going on.
    *  Character array of definition read from file for rule.
    *  Type used for differentiating types of StringProductionRules.
    *  Return Type is ProductionRules With it's arrayList initialised.
    *  This method is also used for getting ComparableRules for OrRules when tokenFlag is true only.
    */
    private ProductionRules makingProductionRules(ProductionRules productionRule, char productionRuleArray[], int flag) throws DException {
        ArrayList rules = new ArrayList();
        DBStack stack = new DBStack();
        StringBuffer buffer = new StringBuffer(100);
        int length = productionRuleArray.length;
        for (int index = 0; index < length; ++index) {
            switch(productionRuleArray[index]) {
                case '<':
                    if (length > 2) {
                        handlingForStringProductionRules(null, rules, buffer, stack, flag);
                        Object object[] = processingForAngularBracket(productionRuleArray, index);
                        index = object[1].hashCode();
                        if (!tokenFlag && ((ProductionRules) object[0]).nameOfRule.equalsIgnoreCase("boolean value expression") && productionRule.nameOfRule.equalsIgnoreCase("SQL 99")) {
                            stack.pop();
                            break;
                        }
                        if (!tokenFlag && ((ProductionRules) object[0]).nameOfRule.equalsIgnoreCase("token")) {
                            while (index < length && productionRuleArray[index] != '<') ++index;
                            --index;
                        } else {
                            rules.add((ProductionRules) object[0]);
                            stack.push(new Integer(rules.size() - 1));
                        }
                    } else buffer.append(productionRuleArray[index]);
                    break;
                case '|':
                    if (length > 2) {
                        handlingForStringProductionRules(null, rules, buffer, stack, flag);
                        rules.add(makingUnionProductionRuleObject(stack, rules));
                        stack.push(new Integer(rules.size() - 1));
                        stack.push("|");
                    } else buffer.append(productionRuleArray[index]);
                    break;
                case '[':
                    if (length > 1) {
                        handlingForStringProductionRules(null, rules, buffer, stack, flag);
                        stack.push("[");
                    } else buffer.append(productionRuleArray[index]);
                    break;
                case ']':
                    if (length > 1) {
                        handlingForStringProductionRules(null, rules, buffer, stack, flag);
                        rules.add(EmptyingStack("]", stack, rules));
                        stack.push(new Integer(rules.size() - 1));
                    } else buffer.append(productionRuleArray[index]);
                    break;
                case '{':
                    if (length > 1) {
                        handlingForStringProductionRules(null, rules, buffer, stack, flag);
                        stack.push("{");
                    } else buffer.append(productionRuleArray[index]);
                    break;
                case '}':
                    if (length > 1) {
                        handlingForStringProductionRules(null, rules, buffer, stack, flag);
                        ProductionRules productionRule2 = EmptyingStack("}", stack, rules);
                        rules.add(productionRule2);
                        stack.push(new Integer(rules.size() - 1));
                    } else buffer.append(productionRuleArray[index]);
                    break;
                case '.':
                    if ((index + 2) < length) {
                        if (productionRuleArray[index + 1] == '.' && productionRuleArray[index + 2] == '.') {
                            rules.add(handlingForRepetitiveGroup(rules));
                            index += 2;
                            break;
                        }
                    }
                default:
                    if (productionRuleArray[index] == ' ') handlingForStringProductionRules(null, rules, buffer, stack, flag); else buffer.append(productionRuleArray[index]);
            }
        }
        handlingForStringProductionRules(productionRule, rules, buffer, stack, flag);
        ProductionRules productionrule = gettingProductionRule(productionRule, rules, stack);
        return productionrule;
    }

    /**
    * This method is used for checking for productionrule in HashMap, If Exist then returned from
    * HashMap otherwise making ProductionRules object acc to type passed.
    * RuleKey is the key for making entries in hashmap
    */
    private ProductionRules checkingForHashMap(String nameOfRule, ArrayList list, int type, String ruleKey) {
        String ruleName = nameOfRule;
        nameOfRule = getNameOfRule(null, nameOfRule, type);
        ruleKey = getNameOfRule(null, ruleKey, type);
        if (allRules != null) {
            Object obj = allRules.get(nameOfRule);
            if (obj != null) {
                return (ProductionRules) obj;
            }
        }
        ProductionRules productionRules = (ProductionRules) rulea.get(ruleKey);
        if (productionRules == null) {
            productionRules = makingProductionRuleObject(type, ruleKey);
            productionRules.setProductionRuleName(nameOfRule);
            productionRules.setProductionRules(list);
            productionRules.ruleKey = ruleKey;
            rulea.put(ruleKey, productionRules);
            makeEntryInHashMap(productionRules, productionRules.className, productionRules.nameOfRule);
        }
        return productionRules;
    }

    /**
    * This method is used for making RepetitiveProductionRules for rules passed.
    */
    private ProductionRules handlingForRepetitiveGroup(ArrayList rules) {
        ArrayList array = new ArrayList();
        ProductionRules productionRules = (ProductionRules) rules.remove(rules.size() - 1);
        array.add(productionRules);
        String nameOfRule = productionRules.nameOfRule;
        return checkingForHashMap(nameOfRule, array, REPETITIVEPRODUCTIONRULE, nameOfRule);
    }

    /**
    * This method is used for making ProductionRules having an index maintained in Stack.
    * ProductionRules passed is rule for which we are going on.
    * ArrayList Rules is setted as rules arrayList for rule
    * Stack contains index of rules in an arrayList.
    */
    public ProductionRules gettingProductionRule(ProductionRules productionRules, ArrayList rules, DBStack stack) {
        int max = stack.size();
        if (max == 0) return productionRules;
        ArrayList list = new ArrayList();
        while (max >= 1) {
            Object temporary = stack.pop();
            if (temporary instanceof Integer) list.add(rules.remove(temporary.hashCode())); else if (temporary.equals("|")) {
                ProductionRules production = processingForRestElementsInStack(stack, list, UNIONPRODUCTIONRULE);
                if (production != null) {
                    list = new ArrayList();
                    list.add(production);
                }
                poppingOutAllOr(stack);
                max = stack.size() + 1;
            }
            --max;
        }
        productionRules.setProductionRules(list);
        return productionRules;
    }

    /**
    * This method is used for processing Rest of Elements in Stack.
    */
    private ProductionRules processingForRestElementsInStack(DBStack stack, ArrayList list, int type) {
        if (list.size() > 1) {
            String[] array = gettingNames(list, type);
            return checkingForHashMap(array[0], list, type, array[1]);
        }
        return null;
    }

    /**
    *  For Getting productionruleObject ruleName read between < and >
    *  Retruns Object array for rule name to be made.
    *  Object[0] for ProductionRules
    *  Object[1] for index of query character array.
    */
    private Object[] processingForAngularBracket(char productionRuleArray[], int index) throws DException {
        StringBuffer buffer = new StringBuffer(100);
        while (productionRuleArray[++index] != '>') buffer.append(productionRuleArray[index]);
        ProductionRules productionRules = getProductionRule2(new String(buffer));
        return new Object[] { productionRules, new Integer(index) };
    }

    /**
    *  This method is used for making StringProductionRules.
    */
    private void handlingForStringProductionRules(ProductionRules productionRules, ArrayList rules, StringBuffer buffer, DBStack stack, int flag) {
        if (buffer.length() == 0) return;
        String temporary = new String(buffer);
        if (!temporary.trim().equals("")) {
            if (stack.size() == 0 && productionRules != null) {
                StringProductionRules strp = (StringProductionRules) productionRules;
                strp.setString(temporary, null);
                strp.ruleKey = strp.keyWord;
                return;
            }
            StringProductionRules rule = flag == 1 ? new KeyWordProductionRules(classLoader) : new StringProductionRules(classLoader);
            rule.setProductionRuleName(temporary);
            String temporary1 = updateNameOfRule(rule.nameOfRule);
            if ((nonReservedWord.contains(temporary1) || temporary.equalsIgnoreCase("DATA")) && !temporary.equalsIgnoreCase("ROWNUM")) {
                rule.className = initailizeNameOfClass(nonReserved);
                rule.setString(temporary, nonReserved);
            } else if ((reservedWord.contains(temporary1) || temporary.equalsIgnoreCase("ROWNUM"))) {
                rule.className = initailizeNameOfClass(reserved);
                rule.setString(temporary, reserved);
            } else rule.setString(temporary, null);
            rule.ruleKey = rule.nameOfRule;
            rules.add(rule);
            stack.push(new Integer(rules.size() - 1));
            allRules.put(rule.nameOfRule, rule);
            rulea.put(rule.nameOfRule, rule);
        }
        buffer.setLength(0);
    }

    /**
    *  This method is used for making Rules lying between []
    *  This method is also used for making Rules lying between {}
    */
    private ProductionRules EmptyingStack(String oper, DBStack stack, ArrayList rules) {
        int type = -1;
        ArrayList list = new ArrayList();
        ProductionRules productionRules = null;
        if (oper.equals("}")) oper = "{";
        if (oper.equals("]")) {
            oper = "[";
            type = OPTIONALPRODUCTIONRULE;
        }
        while (true) {
            Object temporary = stack.pop();
            if (temporary instanceof Integer) list.add(rules.remove(temporary.hashCode())); else if (temporary.equals("|")) {
                ProductionRules production = processingForRestElementsInStack(stack, list, UNIONPRODUCTIONRULE);
                if (production != null) {
                    list = new ArrayList();
                    list.add(production);
                }
                poppingOutAllOr(stack);
                productionRules = makingProductionRuleObject(ORPRODUCTIONRULE, "");
            }
            if (oper.equals(temporary)) break;
        }
        ProductionRules rr = gettingObject(list, productionRules, type);
        return rr;
    }

    /**
    *  This method is used for popping out all or's from Stack.
    */
    private void poppingOutAllOr(DBStack stack) {
        int i = stack.size() - 1;
        while (i >= 0) {
            Object oper = stack.get(i);
            if (oper.equals("{") || oper.equals("[")) break; else if (oper.equals("|")) stack.remove(i);
            --i;
        }
    }

    private ProductionRules gettingObject(ArrayList list, ProductionRules productionRules, int type) {
        ArrayList array1;
        if (list.size() == 1 && type != -1) {
            String array[] = gettingNames(list, type);
            return checkingForHashMap(array[0], list, type, array[1]);
        }
        if (productionRules != null) {
            String[] array = gettingNames(list, type);
            String nameOfRule = array[0];
            String ruleKey = array[1];
            if (rulea.containsKey(ruleKey)) productionRules = (ProductionRules) rulea.get(ruleKey); else {
                productionRules = makingProductionRuleObject(ORPRODUCTIONRULE, ruleKey);
                productionRules.setProductionRuleName(nameOfRule);
                productionRules.ruleKey = ruleKey;
                productionRules.setProductionRules(list);
                rulea.put(ruleKey, productionRules);
                makeEntryInHashMap(productionRules, productionRules.className, productionRules.nameOfRule);
            }
            array1 = new ArrayList();
            array1.add(productionRules);
            list = array1;
        }
        if (list.size() > 1) {
            String[] array = gettingNames(list, type);
            String nameOfRule = array[0];
            String ruleKey = array[1];
            productionRules = checkingForHashMap(nameOfRule, list, UNIONPRODUCTIONRULE, ruleKey);
            array1 = new ArrayList();
            array1.add(productionRules);
            list = array1;
        }
        if (type == -1) {
            if (productionRules == null) return (ProductionRules) list.remove(list.size() - 1);
            return productionRules;
        }
        String nameOfRule = ((ProductionRules) list.get(0)).nameOfRule;
        String ruleKey = ((ProductionRules) list.get(0)).ruleKey;
        return checkingForHashMap(nameOfRule, list, type, ruleKey);
    }

    /**
    * For making NameOfRule from arrayLIst passed to it
    */
    private String[] gettingNames(ArrayList list, int type) {
        StringBuffer bufferForgettingNames = new StringBuffer(100);
        StringBuffer bufferForRuleKey = new StringBuffer(100);
        int size = list.size() - 1;
        for (int i = size; i >= 0; i--) {
            ProductionRules rule = (ProductionRules) list.get(i);
            if (rule instanceof StringProductionRules) {
                bufferForgettingNames.append(((StringProductionRules) rule).keyWord);
                bufferForRuleKey.append(((StringProductionRules) rule).nameOfRule);
            } else {
                bufferForgettingNames.append(rule.nameOfRule);
                bufferForRuleKey.append(rule.nameOfRule);
            }
        }
        return new String[] { bufferForgettingNames.toString(), bufferForRuleKey.toString() };
    }

    /**
    * This method is used for making ProductionRules WithHashMap if nameOfRule passed has an entry in withHashMapRules Stack.
    * ProductionRules object is made for type passed.
    */
    private ProductionRules makingProductionRuleObject(int type, String nameOfRule) {
        switch(type) {
            case UNIONPRODUCTIONRULE:
                if ((withHashMapRules.contains(nameOfRule) || withHashMapWithBinary.contains(nameOfRule)) && !nonHashMap.contains(nameOfRule)) return new UnionProductionRulesWithHashMap(classLoader);
                return new UnionProductionRules(classLoader);
            case OPTIONALPRODUCTIONRULE:
                if ((withHashMapRules.contains(nameOfRule) || withHashMapWithBinary.contains(nameOfRule)) && !nonHashMap.contains(nameOfRule)) return new OptionalProductionRulesWithHashMap(classLoader);
                return new OptionalProductionRules(classLoader);
            case ORPRODUCTIONRULE:
                if ((withHashMapRules.contains(nameOfRule) || withHashMapWithBinary.contains(nameOfRule)) && !nonHashMap.contains(nameOfRule)) return new OrProductionRulesWithHashMap(classLoader);
                return new OrProductionRules(classLoader);
            case REPETITIVEPRODUCTIONRULE:
                if ((withHashMapRules.contains(nameOfRule) || withHashMapWithBinary.contains(nameOfRule)) && !nonHashMap.contains(nameOfRule)) return new RepetitiveProductionRulesWithHashMap(classLoader);
                return new RepetitiveProductionRules(classLoader);
            case STRINGPRODUCTIONRULE:
                return new StringProductionRules(classLoader);
            case SIMPLEPRODUCTIONRULE:
                if ((withHashMapRules.contains(nameOfRule) || withHashMapWithBinary.contains(nameOfRule)) && !nonHashMap.contains(nameOfRule)) return new SimpleProductionRulesWithHashMap(classLoader);
                return new SimpleProductionRules(classLoader);
        }
        return null;
    }

    /**
    * This Method is used for makingUnionProductionRules object.
    */
    private ProductionRules makingUnionProductionRuleObject(DBStack stack, ArrayList rules) {
        int max = stack.size();
        ArrayList array = new ArrayList();
        while (max >= 1) {
            Object temporary = stack.peek();
            if (temporary instanceof Integer) {
                temporary = stack.pop();
                array.add(rules.remove(temporary.hashCode()));
            } else if (temporary == null || temporary.getClass() == String.class) break;
            --max;
        }
        if (array.size() == 1) return (ProductionRules) array.get(0);
        String[] array1 = gettingNames(array, UNIONPRODUCTIONRULE);
        String nameOfRule = array1[0];
        String ruleKey = array1[1];
        return checkingForHashMap(nameOfRule, array, UNIONPRODUCTIONRULE, ruleKey);
    }

    /**
    * This Method is used for getting NameOfRule for Production Rule passed
    */
    private String getNameOfRule(ProductionRules production, String str, int type) {
        StringBuffer strbuf = null;
        if (type == REPETITIVEPRODUCTIONRULE || production instanceof RepetitiveProductionRules || production instanceof RepetitiveProductionRulesWithHashMap) {
            strbuf = new StringBuffer("Rep");
            strbuf.append(str);
            return strbuf.toString();
        }
        if (type == OPTIONALPRODUCTIONRULE || production instanceof OptionalProductionRules || production instanceof OptionalProductionRulesWithHashMap) {
            strbuf = new StringBuffer("Opt");
            strbuf.append(str);
            return strbuf.toString();
        }
        return str;
    }

    /**
    * This method is used for making Stack for getting type of productionrule without evaluating.
    */
    private int makeStack(String name) {
        char[] array = name.trim().toCharArray();
        DBStack stack = new DBStack();
        int length = array.length;
        for (int i = 0; i < length; ++i) {
            switch(array[i]) {
                case '<':
                    if (length > 2) stack.push("<"); else stack.push(new Character('<'));
                    break;
                case '>':
                    if (length > 2) EmptyingStackTill(stack, "<"); else stack.push(new Character('>'));
                    break;
                case '|':
                    if (length > 2) stack.push("|"); else stack.push(new Character('|'));
                    break;
                case '[':
                    if (length > 1) stack.push("["); else stack.push(new Character('['));
                    break;
                case ']':
                    if (length > 1) EmptyingStackTill(stack, "["); else stack.push(new Character(']'));
                    break;
                case '{':
                    if (length > 1) stack.push("{"); else stack.push(new Character('{'));
                    break;
                case '}':
                    if (length > 1) EmptyingStackTill(stack, "{"); else stack.push(new Character('}'));
                    break;
                case '.':
                    if ((i + 2) < array.length) {
                        if (array[i + 1] == '.' && array[i + 2] == '.') {
                            stack.pop();
                            stack.push("%R");
                            i = i + 2;
                            break;
                        }
                    }
                default:
                    if (array[i] != ' ' && i < array.length) stack.push(new Character(array[i])); else checkingForString(stack);
            }
        }
        return returningType(stack);
    }

    private void checkingForString(DBStack stack) {
        int j = stack.size() - 1;
        while (j >= 0 && stack.get(j) instanceof Character) {
            char ch = ((Character) stack.get(j)).charValue();
            if (Character.isUpperCase(ch)) {
                stack.pop();
                --j;
                if (j < 0) stack.push("&O");
            } else {
                stack.push("&O");
                break;
            }
        }
    }

    /**
    * Returning Type Of Object.
    */
    private int returningType(DBStack stack) {
        int max = stack.size();
        if (stack.indexOf("|") != -1) {
            if (max <= 2) return STRINGPRODUCTIONRULE;
            return ORPRODUCTIONRULE;
        }
        if (max >= 1) return checkingForUnion(stack);
        return STRINGPRODUCTIONRULE;
    }

    /**
    * CheckingFor Union Rule in Stack.
    */
    private int checkingForUnion(DBStack stack) {
        int i = 0;
        int type = STRINGPRODUCTIONRULE;
        int max = stack.size();
        while (i < max) {
            if (stack.get(i) instanceof String) {
                String str = (String) stack.get(i);
                if (max == 1) return SIMPLEPRODUCTIONRULE;
                if (str.equals("&O") || str.equals("^C") || str.equals("%A") || str.equals("%R")) type = UNIONPRODUCTIONRULE;
            }
            ++i;
        }
        return type;
    }

    /**
    *  Emtying Stack for string s passed.
    */
    private void EmptyingStackTill(DBStack stack, String s) {
        String str;
        if (s.equals("[") && stack.size() > 1) str = "&O"; else if (s.equals("[") && stack.size() == 1) str = "%S"; else if (s.equals("{")) str = "^C"; else {
            str = "%A";
            if (stack.size() == 1) {
                stack.push("%S");
                return;
            } else if (stack.empty()) {
                stack.push(">");
                return;
            }
        }
        while (true) {
            Object temp = stack.pop();
            if (temp.equals(s)) break;
        }
        stack.push(str);
    }

    private boolean ifRuleForKeyWordProductionRule(String temp) {
        if (temp.equalsIgnoreCase(DATELITERAL) || temp.equalsIgnoreCase(TIMELITERAL) || temp.equalsIgnoreCase(TIMESTAMPLITERAL) || temp.equalsIgnoreCase(RESERVEDWORD) || temp.equalsIgnoreCase(NONRESERVEDWORD) || temp.equalsIgnoreCase(NATIONALCHARACTERSTRINGLITERAL) || temp.equalsIgnoreCase(BITSTRINGLITERAL) || temp.equalsIgnoreCase(HEXSTRINGLITERAL) || temp.equalsIgnoreCase(BINARYSTRINGLITERAL) || temp.equalsIgnoreCase(INTERVALLITERAL) || temp.equalsIgnoreCase(BOOLEANLITERAL) || temp.equalsIgnoreCase("multiplier")) return true;
        return false;
    }

    private void testRetrievalOfFiles() throws DException {
        Class cls = getClass();
        URL url1 = null;
        if (url == null) {
            url1 = cls.getResource("/com/daffodilwoods/daffodildb/utils/parser/reservedWord.obj");
            if (url1 == null) throw new DException("DSE0", new Object[] { "Reserveword file is missing in classpath." });
        } else {
            try {
                url1 = new URL(url.getProtocol() + ":" + url.getPath() + "/reservedWord.obj");
            } catch (MalformedURLException ex) {
                throw new DException("DSE0", new Object[] { ex });
            }
        }
        try {
            ObjectInputStream ooin = new ObjectInputStream(new BufferedInputStream(url1.openStream()));
            reservedWord = new ArrayList((Vector) ooin.readObject());
            ooin.close();
        } catch (ClassNotFoundException ex1) {
            ex1.printStackTrace();
            throw new DException("DSE0", new Object[] { ex1 });
        } catch (IOException ex1) {
            ex1.printStackTrace();
            throw new DException("DSE0", new Object[] { ex1 });
        }
        if (url == null) {
            url1 = cls.getResource("/com/daffodilwoods/daffodildb/utils/parser/nonReservedWord.obj");
            if (url1 == null) throw new DException("DSE0", new Object[] { "NonReserveword file is missing in classpath." });
        } else {
            try {
                url1 = new URL(url.getProtocol() + ":" + url.getPath() + "/nonReservedWord.obj");
            } catch (MalformedURLException ex1) {
                ex1.printStackTrace();
                throw new DException("DSE0", new Object[] { ex1 });
            }
        }
        try {
            ObjectInputStream ooin = new ObjectInputStream(new BufferedInputStream(url1.openStream()));
            nonReservedWord = new ArrayList((Vector) ooin.readObject());
            ooin.close();
        } catch (ClassNotFoundException ex1) {
            ex1.printStackTrace();
            throw new DException("DSE0", new Object[] { ex1 });
        } catch (IOException ex1) {
            ex1.printStackTrace();
            throw new DException("DSE0", new Object[] { ex1 });
        }
    }

    ArrayList awe = new ArrayList();

    private void modifyProductionRuleName(ProductionRules pr1) {
        if (awe.contains(pr1.nameOfRule)) {
            return;
        }
        awe.add(pr1.nameOfRule);
        Object[] asd = pr1.rules;
        if (asd == null) {
            return;
        }
        for (int i = 0; i < asd.length; i++) {
            if (asd[i] instanceof ProductionRules) modifyProductionRuleName((ProductionRules) asd[i]);
        }
        if (pr1.nameOfRule.equalsIgnoreCase("SComma94843605character string literal")) return;
        ProductionRules pr = pr1;
        if (pr instanceof UnionProductionRules || pr instanceof UnionProductionRulesWithHashMap) {
            Object[] updatedRules = pr.rules;
            ArrayList productionRules = new ArrayList(Arrays.asList(updatedRules));
            ArrayList productionRules1 = new ArrayList(Arrays.asList(updatedRules));
            int size1 = updatedRules.length - 1;
            boolean ff = false, ff1 = true;
            if (size1 > 0) {
                if (updatedRules[size1].toString().equalsIgnoreCase("[Scomma94843605] SPR [,]")) {
                    Object o = productionRules.remove(size1);
                    productionRules1.set(size1, null);
                    ff = true;
                }
                if (updatedRules[size1].toString().equalsIgnoreCase("[Sleft paren653880241] SPR [(]") && updatedRules[0].toString().equalsIgnoreCase("[Sright paren-1874859514] SPR [)]")) {
                    Object o = productionRules.remove(size1);
                    o = productionRules.remove(0);
                    productionRules1.set(size1, null);
                    productionRules1.set(0, null);
                    ff1 = false;
                }
            }
            Object[] temp = pr.rules;
            pr.rules = null;
            boolean b = updateArrayList(pr.nameOfRule, productionRules);
            if (b) {
                Object o = productionRules1.set(1, Boolean.FALSE);
                pr.setRepUnionType(true);
            }
            if (ff && !pr.angularType) {
                pr.setProductionRuleName(gettingNames(productionRules));
                pr.toMakeClass = false;
            }
            boolean ss = pr.toMakeClass;
            pr.toMakeClass = ff1;
            pr.setProductionRules(productionRules);
            pr.toMakeClass = ss;
            pr.rules = temp;
            pr.updatedRules = productionRules1.toArray();
        }
    }

    private boolean updateArrayList(String asd, ArrayList pr) {
        if (pr.size() == 2) {
            ProductionRules pr0 = (ProductionRules) pr.get(0);
            ProductionRules pr1 = (ProductionRules) pr.get(1);
            if (pr0 instanceof OptionalProductionRules || pr0 instanceof OptionalProductionRulesWithHashMap) {
                pr0 = (ProductionRules) pr0.rules[0];
                if (pr0 instanceof RepetitiveProductionRules || pr0 instanceof RepetitiveProductionRulesWithHashMap) {
                    pr0 = (ProductionRules) pr0.rules[0];
                }
            }
            if (pr0.nameOfRule.equalsIgnoreCase(pr1.nameOfRule)) {
                ProductionRules o = (ProductionRules) pr.remove(1);
                ProductionRules o1 = (ProductionRules) pr.get(0);
                o1.nameOfRule = asd;
                return true;
            }
        }
        return false;
    }

    private String gettingNames(ArrayList list) {
        StringBuffer bufferForgettingNames = new StringBuffer(100);
        int size = list.size() - 1;
        for (int i = size; i >= 0; i--) {
            ProductionRules rule = (ProductionRules) list.get(i);
            if (rule instanceof StringProductionRules) {
                bufferForgettingNames.append(((StringProductionRules) rule).keyWord);
            } else {
                bufferForgettingNames.append(rule.nameOfRule);
            }
        }
        return bufferForgettingNames.toString();
    }
}
