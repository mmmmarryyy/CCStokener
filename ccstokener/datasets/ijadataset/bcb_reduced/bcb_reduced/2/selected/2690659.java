package ch.ethz.mxquery.query.parser;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import ch.ethz.mxquery.model.Wildcard;
import java.util.Vector;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import ch.ethz.mxquery.contextConfig.Context;
import ch.ethz.mxquery.contextConfig.XQStaticContext;
import ch.ethz.mxquery.datamodel.Namespace;
import ch.ethz.mxquery.datamodel.QName;
import ch.ethz.mxquery.datamodel.XQName;
import ch.ethz.mxquery.datamodel.types.Type;
import ch.ethz.mxquery.datamodel.types.TypeDictionary;
import ch.ethz.mxquery.datamodel.types.TypeInfo;
import ch.ethz.mxquery.exceptions.ErrorCodes;
import ch.ethz.mxquery.exceptions.MXQueryException;
import ch.ethz.mxquery.exceptions.StaticException;
import ch.ethz.mxquery.exceptions.TypeException;
import ch.ethz.mxquery.functions.fn.Doc;
import ch.ethz.mxquery.iterators.SequenceTypeIterator;
import ch.ethz.mxquery.iterators.ValidateIterator;
import ch.ethz.mxquery.iterators.forseq.ForseqIterator;
import ch.ethz.mxquery.iterators.forseq.ForseqWindowNaiveIterator;
import ch.ethz.mxquery.model.WindowVariable;
import ch.ethz.mxquery.model.XDMIterator;

/**
 * Extended version of the parser that enables features and optimizations that
 * are only available on J2SE 1.4 and higher
 * 
 * @author Peter M. Fischer
 * 
 */
public class SEParser extends Parser {

    protected TypeInfo SchemaElementTest() throws MXQueryException {
        int oldIndex = index;
        TypeInfo stepData;
        if (parseKeyword("schema-element")) {
            if (!co.isSchemaAwareness()) generateStaticError(ErrorCodes.A0002_EC_NOT_SUPPORTED, "Schema support disabled: schema-element() not available. Either enable it, or switch to a version of MXQuery that supports it");
            stepData = new TypeInfo();
            if (!parseString("(", true, false)) {
                index = oldIndex;
                return null;
            }
            QName q = EQName();
            if (q == null) {
                generateStaticError(ErrorCodes.E0003_STATIC_NOT_A_VALID_GRAMMAR_ELEMENT, "Error while parsing: 'QName' expected!");
            }
            if (!parseString(")", true, false)) {
                generateStaticError(ErrorCodes.E0003_STATIC_NOT_A_VALID_GRAMMAR_ELEMENT, "Error while parsing: ')' expected!");
            }
            if (!co.isSchemaAwareness()) generateStaticError(ErrorCodes.A0002_EC_NOT_SUPPORTED, "SchemaElementTest not supported yet!");
            q = (QName) q.resolveQNameNamespace(getCurrentContext(), true);
            q = rewriteUDTQNameWithResolvedPrefix(q);
            String namespaceUri = q.getNamespaceURI();
            String localPart = q.getLocalPart();
            if (Context.getDictionary().lookUpByName("(" + namespaceUri + ")" + localPart) == null) generateStaticError(ErrorCodes.E0008_STATIC_NAME_OR_PREFIX_NOT_DEFINED, "Global Element " + q.toString() + " has not been declared");
            stepData.setXQName(q);
            stepData.setType(Type.TYPE_NK_SCHEMA_ELEM_TEST);
            return stepData;
        }
        index = oldIndex;
        return null;
    }

    protected TypeInfo SchemaAttrTest() throws MXQueryException {
        int oldIndex = index;
        TypeInfo stepData;
        if (parseKeyword("schema-attribute")) {
            if (!co.isSchemaAwareness()) generateStaticError(ErrorCodes.A0002_EC_NOT_SUPPORTED, "Schema support disabled: schema-attribute() not available. Either enable it, or switch to a version of MXQuery that supports it");
            stepData = new TypeInfo();
            if (!parseString("(", true, false)) {
                index = oldIndex;
                return null;
            }
            QName q = EQName();
            if (q == null) {
                generateStaticError(ErrorCodes.E0003_STATIC_NOT_A_VALID_GRAMMAR_ELEMENT, "Error while parsing: 'QName' expected!");
            }
            if (!parseString(")", true, false)) {
                generateStaticError(ErrorCodes.E0003_STATIC_NOT_A_VALID_GRAMMAR_ELEMENT, "Error while parsing: ')' expected!");
            }
            if (!co.isSchemaAwareness()) generateStaticError(ErrorCodes.A0002_EC_NOT_SUPPORTED, "SchemaAttributeTest not supported yet!");
            q = (QName) q.resolveQNameNamespace(getCurrentContext(), true);
            q = rewriteUDTQNameWithResolvedPrefix(q);
            String namespaceUri = q.getNamespaceURI();
            String localPart = q.getLocalPart();
            if (Context.getDictionary().lookUpByName("[" + namespaceUri + "]" + localPart) == null) generateStaticError(ErrorCodes.E0008_STATIC_NAME_OR_PREFIX_NOT_DEFINED, "Global Attribute " + q.toString() + " has not been declared");
            stepData.setXQName(q);
            stepData.setType(Type.TYPE_NK_SCHEMA_ATTR_TEST);
            return stepData;
        }
        index = oldIndex;
        return null;
    }

    protected TypeInfo ElementTest() throws MXQueryException {
        int oldIndex = index;
        TypeInfo stepData;
        int type = 0;
        if (parseString("element", true, false) && parseString("(", true, false)) {
            XQName qname;
            stepData = new TypeInfo();
            if (parseString("*", true, false)) {
                stepData.setXQName(new Wildcard("*", false));
                if (parseString(",", true, false)) {
                    QName q = EQName();
                    if (q == null) {
                        generateStaticError(ErrorCodes.E0003_STATIC_NOT_A_VALID_GRAMMAR_ELEMENT, "Error while parsing: 'QName' expected!");
                    } else {
                        try {
                            q = (QName) q.resolveQNameNamespace(getCurrentContext(), true);
                            q = rewriteUDTQNameWithResolvedPrefix(q);
                            type = Type.getTypeFootprint(q, Context.getDictionary());
                        } catch (MXQueryException me) {
                            throw new TypeException(ErrorCodes.E0008_STATIC_NAME_OR_PREFIX_NOT_DEFINED, "Schema Type not available for element check", getCurrentLoc());
                        }
                    }
                    if (parseString("?", true, false)) {
                        type = Type.setIsNilled(type);
                    }
                }
            } else if ((qname = EQName()) != null) {
                qname = ((QName) qname).resolveQNameNamespace(getCurrentContext(), true);
                stepData.setXQName(qname);
                if (parseString(",", true, false)) {
                    QName q = EQName();
                    if (q == null) {
                        generateStaticError(ErrorCodes.E0003_STATIC_NOT_A_VALID_GRAMMAR_ELEMENT, "Error while parsing: 'QName' expected!");
                    } else {
                        try {
                            q = (QName) q.resolveQNameNamespace(getCurrentContext(), true);
                            q = rewriteUDTQNameWithResolvedPrefix(q);
                            type = Type.getTypeFootprint(q, Context.getDictionary());
                        } catch (MXQueryException me) {
                            if (me.getErrorCode().equals(ErrorCodes.E0051_STATIC_QNAME_AS_ATOMICTYPE_NOT_DEFINED_AS_ATOMIC)) throw new TypeException(ErrorCodes.E0008_STATIC_NAME_OR_PREFIX_NOT_DEFINED, "Type not available for element check", getCurrentLoc()); else throw me;
                        }
                        if (parseString("?", true, false)) {
                            type = Type.setIsNilled(type);
                        }
                    }
                }
            }
            if (!parseString(")", true, false)) {
                generateStaticError(ErrorCodes.E0003_STATIC_NOT_A_VALID_GRAMMAR_ELEMENT, "Error while parsing: ')' expected!");
            }
            stepData.setType(Type.START_TAG | type);
            return stepData;
        }
        index = oldIndex;
        return null;
    }

    protected TypeInfo AttributeTest() throws MXQueryException {
        int oldIndex = index;
        TypeInfo stepData;
        int type = 0;
        if (parseString("attribute", true, false) && parseString("(", true, false)) {
            QName qname;
            stepData = new TypeInfo();
            if (parseString("*", true, false)) {
                stepData.setXQName(new Wildcard("*", false));
                if (parseString(",", true, false)) {
                    QName q = EQName();
                    if (q == null) {
                        generateStaticError(ErrorCodes.E0003_STATIC_NOT_A_VALID_GRAMMAR_ELEMENT, "Error while parsing: 'QName' expected!");
                    } else {
                        try {
                            q = (QName) q.resolveQNameNamespace(getCurrentContext(), true);
                            q = rewriteUDTQNameWithResolvedPrefix(q);
                            type = Type.getTypeFootprint(q, Context.getDictionary());
                        } catch (MXQueryException me) {
                            if (me.getErrorCode().equals(ErrorCodes.E0051_STATIC_QNAME_AS_ATOMICTYPE_NOT_DEFINED_AS_ATOMIC)) throw new TypeException(ErrorCodes.E0008_STATIC_NAME_OR_PREFIX_NOT_DEFINED, "Type not available for element check", getCurrentLoc()); else throw me;
                        }
                    }
                }
            } else if ((qname = EQName()) != null) {
                qname = (QName) qname.resolveQNameNamespace(getCurrentContext(), false);
                stepData.setXQName(qname);
                if (parseString(",", true, false)) {
                    QName q = EQName();
                    if (q == null) {
                        generateStaticError(ErrorCodes.E0003_STATIC_NOT_A_VALID_GRAMMAR_ELEMENT, "Error while parsing: 'QName' expected!");
                    } else {
                        try {
                            q = (QName) q.resolveQNameNamespace(getCurrentContext(), true);
                            q = rewriteUDTQNameWithResolvedPrefix(q);
                            type = Type.getTypeFootprint(q, Context.getDictionary());
                        } catch (MXQueryException me) {
                            if (me.getErrorCode().equals(ErrorCodes.E0051_STATIC_QNAME_AS_ATOMICTYPE_NOT_DEFINED_AS_ATOMIC)) throw new TypeException(ErrorCodes.E0008_STATIC_NAME_OR_PREFIX_NOT_DEFINED, "Type not available for element check", getCurrentLoc()); else throw me;
                        }
                        if (parseString("?", true, false)) {
                            type = Type.setIsNilled(type);
                        }
                    }
                }
            }
            if (!parseString(")", true, false)) {
                generateStaticError(ErrorCodes.E0003_STATIC_NOT_A_VALID_GRAMMAR_ELEMENT, "Error while parsing: ')' expected!");
            }
            stepData.setType(Type.TYPE_NK_ATTR_TEST | type);
            return stepData;
        }
        index = oldIndex;
        return null;
    }

    protected void processSchemas(Vector schema_import, String ns_uri, String prefix, boolean defaultElementNamespace) throws MXQueryException {
        if (!co.isSchemaAwareness()) generateStaticError(ErrorCodes.E0009_STATIC_SCHEMA_IMPORTS_NOT_SUPPORTED, "Schema Imports are disabled. Either enable it, or switch to a version of MXQuery that supports it");
        Context ctx = getCurrentContext();
        Namespace ns = ctx.getNamespace(prefix);
        TypeDictionary dict = Context.initDictionary();
        boolean preloadedSchema = false;
        if (dict.containsDefinitions4Namespace(ns_uri)) preloadedSchema = true;
        if (ns != null && (prefix != null) && !(ns.getURI().equals(XQStaticContext.URI_XS)) || ((prefix != null) && prefix.equals(XQStaticContext.NS_XS))) {
            generateStaticError(ErrorCodes.E0033_STATIC_MODULE_MULTIPLE_BINDINGS_FOR_SAME_PREFIX, "Multiple declarations of Namespace " + prefix);
        }
        if (ctx.addTargetNamespace(ns_uri) && (prefix != null)) {
            ctx.addNamespace(new Namespace(prefix, ns_uri));
        } else if (prefix != null) generateStaticError(ErrorCodes.E0058_STATIC_SCHEMA_IMPORTS_SPECIFY_SAME_TARGET_NAMESPACE, "Multiple declaration of TargetNameSpace {" + ns_uri + "}");
        if ((((schema_import.size() == 0) && !defaultElementNamespace && !dict.containsDefinitions4Namespace(ns_uri)) || !exists(schema_import, ns_uri)) && !dict.containsDefinitions4Namespace(ns_uri) && !ns_uri.equals(XQStaticContext.URI_XS)) throw new StaticException(ErrorCodes.E0059_STATIC_UNABLE_TO_PROCESS_SCHEMA_OR_MODULE_IMPORT, "Unable to locate schema for namespace: " + ns_uri, getCurrentLoc()); else if (!ns_uri.equals(XQStaticContext.URI_XS)) {
            Object[] schemas = new Object[schema_import.size()];
            schema_import.copyInto(schemas);
            String[] uriList = new String[schemas.length];
            for (int j = 0; j < schemas.length; j++) {
                uriList[j] = (String) schema_import.elementAt(j);
                try {
                    SchemaParser.parseSchema(ctx, uriList[j], ns_uri, dict, getCurrentLoc());
                } catch (StaticException e) {
                    if (j == schemas.length - 1) {
                        throw e;
                    }
                }
            }
            if (preloadedSchema) addUDTFunctions(ns_uri, prefix);
        }
    }

    private void addUDTFunctions(String ns_uri, String prefix) throws MXQueryException {
        TypeDictionary dict = Context.getDictionary();
        Object list = Context.getDictionary().getUDTFunctionsList(ns_uri);
        if (list != null) {
            for (java.util.Iterator iter = ((Vector) list).iterator(); iter.hasNext(); ) {
                XSSimpleTypeDefinition typeDef = (XSSimpleTypeDefinition) iter.next();
                SchemaParser.addFunction(getCurrentContext(), typeDef, dict);
            }
        }
    }

    private boolean exists(Vector schema_import, String ns_uri) {
        Object[] schemas = new Object[schema_import.size()];
        schema_import.copyInto(schemas);
        String[] uriList = new String[schemas.length];
        for (int i = 0; i < uriList.length; i++) {
            uriList[i] = (String) schema_import.elementAt(i);
            File f = new File(uriList[i]);
            if (f.exists()) return true;
            try {
                URL url = new URL(uriList[i]);
                URLConnection con = url.openConnection();
                con.getInputStream();
                return true;
            } catch (MalformedURLException e) {
            } catch (IOException e) {
            }
        }
        if (getCurrentContext().getLocationOfSchema(ns_uri) != null) {
            schema_import.addElement(getCurrentContext().getLocationOfSchema(ns_uri));
            return true;
        }
        return false;
    }

    protected ForseqIterator generateForseqIterator(QName varQName, TypeInfo qType, XDMIterator seq, int windowType, Context outerContext, WindowVariable[] startVars, XDMIterator startExpr, boolean forceEnd, WindowVariable[] endVars, boolean onNewStart, XDMIterator endExpr) throws MXQueryException {
        ForseqIterator res;
        res = new ForseqWindowNaiveIterator(outerContext, windowType, varQName, qType, seq, startVars, startExpr, endVars, endExpr, forceEnd, onNewStart, ForseqIterator.ORDER_MODE_END, getCurrentLoc());
        return res;
    }

    protected XDMIterator validate(XDMIterator exprIterator, int mode, QName type) throws MXQueryException, StaticException {
        if (!co.isSchemaAwareness()) generateStaticError(ErrorCodes.E0075_STATIC_VALIDATION_NOT_SUPPORTED, "ValidateExpr is disabled. Either enable it, or switch to a version of MXQuery that supports it");
        if (exprIterator instanceof Doc) {
            Doc docIt = (Doc) exprIterator;
            docIt.setInValidateExpression(true);
            return docIt;
        }
        XDMIterator ret = new ValidateIterator(mode, getCurrentContext(), new XDMIterator[] { exprIterator }, getCurrentLoc());
        if (type != null) {
            try {
                type = (QName) type.resolveQNameNamespace(getCurrentContext(), true);
                type = rewriteUDTQNameWithResolvedPrefix(type);
            } catch (StaticException se) {
                if (se.getErrorCode().equals(ErrorCodes.E0008_STATIC_NAME_OR_PREFIX_NOT_DEFINED)) {
                    throw new StaticException(ErrorCodes.E0104_STATIC_TYPE_VALIDATE_UNKNOWN, "Type '" + type + "' used in 'validate' is not known", getCurrentLoc());
                } else throw se;
            }
            int typeToCheck = Type.getTypeFootprint(type, Context.getDictionary());
            TypeInfo typeInfo = new TypeInfo();
            typeInfo.setType(typeToCheck);
            ret = new SequenceTypeIterator(typeInfo, true, false, getCurrentContext(), getCurrentLoc(), false);
        }
        return ret;
    }
}
