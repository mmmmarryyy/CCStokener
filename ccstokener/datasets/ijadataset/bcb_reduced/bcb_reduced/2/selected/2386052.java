package de.tudarmstadt.ito.schemas.converters;

import de.tudarmstadt.ito.schemas.dtd.Attribute;
import de.tudarmstadt.ito.schemas.dtd.DTD;
import de.tudarmstadt.ito.schemas.dtd.DTDConst;
import de.tudarmstadt.ito.schemas.dtd.ElementType;
import de.tudarmstadt.ito.schemas.dtd.DTDException;
import de.tudarmstadt.ito.schemas.dtd.Entity;
import de.tudarmstadt.ito.schemas.dtd.Group;
import de.tudarmstadt.ito.schemas.dtd.Notation;
import de.tudarmstadt.ito.schemas.dtd.ParameterEntity;
import de.tudarmstadt.ito.schemas.dtd.ParsedGeneralEntity;
import de.tudarmstadt.ito.schemas.dtd.Particle;
import de.tudarmstadt.ito.schemas.dtd.Reference;
import de.tudarmstadt.ito.schemas.dtd.UnparsedEntity;
import de.tudarmstadt.ito.schemas.dtd.Factory;
import de.tudarmstadt.ito.utils.NSName;
import de.tudarmstadt.ito.utils.TokenList;
import java.io.EOFException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import org.xml.sax.InputSource;

/**
 * Converts an external DTD or the DTD in an XML document into a DTD object.
 *
 * <p>Note that while SubsetToDTD checks for most syntactic errors in the DTD
 * it does not check for all of them. Thus, results are undetermined if the
 * DTD is not syntactically correct.</p>
 *
 * @author Ronald Bourret, Technical University of Darmstadt
 * @version 1.01
 */
public class SubsetToDTD {

    static final int READER_READER = 0, READER_STRING = 1, READER_URL = 2;

    static final int STATE_OUTSIDEDTD = 0, STATE_DTD = 1, STATE_ATTVALUE = 2, STATE_ENTITYVALUE = 3, STATE_COMMENT = 4, STATE_IGNORE = 5;

    static final int BUFSIZE = 8096, LITBUFSIZE = 1024, NAMEBUFSIZE = 1024;

    DTD dtd;

    Hashtable namespaceURIs, predefinedEntities = new Hashtable();

    TokenList dtdTokens;

    Reader reader;

    int readerType, bufferPos, bufferLen, literalPos, namePos, entityState, line, column;

    Stack readerStack;

    StringBuffer literalStr, nameStr;

    char[] buffer, literalBuffer = new char[LITBUFSIZE], nameBuffer = new char[NAMEBUFSIZE];

    boolean ignoreQuote, ignoreMarkup;

    URL readerURL;

    /** Create a new SubsetToDTD object. */
    public SubsetToDTD() {
        dtdTokens = new TokenList(DTDConst.KEYWDS, DTDConst.KEYWD_TOKENS, DTDConst.KEYWD_TOKEN_UNKNOWN);
        initPredefinedEntities();
    }

    /**
    * Convert the DTD in an XML document containing an internal subset,
    * reference to an external subset, or both, into a DTD object.
    *
    * @param src A SAX InputSource for the XML document.
    * @param namespaceURIs A Hashtable mapping prefixes used in the DTD to
    *  namespace URIs. May be null.
    * @return The DTD object.
    * @exception DTDException Thrown if a DTD error is found.
    * @exception EOFException Thrown if EOF is reached prematurely.
    * @exception MalformedURLException Thrown if a system ID is malformed.
    * @exception IOException Thrown if an I/O error occurs.
    */
    public DTD convertDocument(InputSource src, Hashtable namespaceURIs) throws DTDException, MalformedURLException, IOException, EOFException {
        initGlobals();
        this.namespaceURIs = namespaceURIs;
        openInputSource(src);
        parseDocument();
        postProcessDTD();
        return dtd;
    }

    /**
    * Convert the DTD in an external subset into a DTD object.
    *
    * @param src A SAX InputSource for DTD (external subset).
    * @param namespaceURIs A Hashtable mapping prefixes used in the DTD to
    *  namespace URIs. May be null.
    * @return The DTD object.
    * @exception DTDException Thrown if a DTD error is found.
    * @exception EOFException Thrown if EOF is reached prematurely.
    * @exception MalformedURLException Thrown if a system ID is malformed.
    * @exception IOException Thrown if an I/O error occurs.
    */
    public DTD convertExternalSubset(InputSource src, Hashtable namespaceURIs) throws DTDException, MalformedURLException, IOException, EOFException {
        initGlobals();
        this.namespaceURIs = namespaceURIs;
        openInputSource(src);
        parseExternalSubset(true);
        postProcessDTD();
        return dtd;
    }

    void parseAttlistDecl() throws DTDException, MalformedURLException, IOException, EOFException {
        ElementType elementType;
        requireWhitespace();
        elementType = getElementType();
        while (!isChar('>')) {
            requireWhitespace();
            if (isChar('>')) break;
            getAttDef(elementType);
        }
    }

    void parseComment() throws DTDException, MalformedURLException, IOException, EOFException {
        int saveEntityState;
        saveEntityState = entityState;
        entityState = STATE_COMMENT;
        discardUntil("--");
        requireChar('>');
        entityState = saveEntityState;
    }

    boolean parseConditional() throws DTDException, MalformedURLException, IOException, EOFException {
        int saveEntityState;
        boolean condFound = true;
        if (isString("<![")) {
            discardWhitespace();
            if (isString("INCLUDE")) {
                parseInclude();
            } else if (isString("IGNORE")) {
                entityState = STATE_IGNORE;
                parseIgnoreSect();
                entityState = STATE_DTD;
            } else {
                throwDTDException("Invalid conditional section.");
            }
        } else {
            condFound = false;
        }
        return condFound;
    }

    void parseDocTypeDecl() throws DTDException, MalformedURLException, IOException, EOFException {
        String root, systemID = null;
        if (!isString("<!DOCTYPE")) return;
        requireWhitespace();
        root = getName();
        if (root == null) throwDTDException("Invalid root element type name.");
        if (isWhitespace()) {
            discardWhitespace();
            if (isString("SYSTEM")) {
                systemID = parseSystemLiteral();
                discardWhitespace();
            } else if (isString("PUBLIC")) {
                parsePublicID();
                systemID = parseSystemLiteral();
                discardWhitespace();
            }
        }
        if (isChar('[')) {
            parseInternalSubset();
            requireChar(']');
        }
        if (systemID != null) {
            pushCurrentReader();
            createURLReader(new URL(readerURL, systemID));
            parseExternalSubset(false);
        }
        discardWhitespace();
        requireChar('>');
    }

    void parseDocument() throws DTDException, MalformedURLException, IOException, EOFException {
        if (isString("<?xml")) {
            parseXMLDecl();
        }
        parseMisc();
        parseDocTypeDecl();
    }

    void parseElementDecl() throws DTDException, MalformedURLException, IOException, EOFException {
        ElementType elementType;
        requireWhitespace();
        elementType = addElementType();
        requireWhitespace();
        getContentModel(elementType);
        discardWhitespace();
        requireChar('>');
    }

    void parseEncodingDecl() throws DTDException, MalformedURLException, IOException, EOFException {
        parseEquals();
        getEncName();
    }

    void parseEntityDecl() throws DTDException, MalformedURLException, IOException, EOFException {
        Entity entity;
        boolean isPE = false;
        String name, notation, value = null, systemID = null, publicID = null;
        requireWhitespace();
        if (isChar('%')) {
            isPE = true;
            requireWhitespace();
        }
        name = getName();
        requireWhitespace();
        if (isString("PUBLIC")) {
            publicID = parsePublicID();
            systemID = parseSystemLiteral();
        } else if (isString("SYSTEM")) {
            systemID = parseSystemLiteral();
        } else {
            value = getEntityValue();
        }
        if (isPE) {
            entity = Factory.newParameterEntity(name);
            entity.systemID = systemID;
            entity.publicID = publicID;
            ((ParameterEntity) entity).value = value;
            if (!dtd.parameterEntities.containsKey(name)) {
                dtd.parameterEntities.put(name, entity);
            }
        } else if (isString("NDATA")) {
            requireWhitespace();
            notation = getName();
            entity = Factory.newUnparsedEntity(name);
            entity.systemID = systemID;
            entity.publicID = publicID;
            ((UnparsedEntity) entity).notation = notation;
            if (!dtd.unparsedEntities.containsKey(name) && !dtd.parsedGeneralEntities.containsKey(name)) {
                dtd.unparsedEntities.put(name, entity);
            }
        } else {
            entity = Factory.newParsedGeneralEntity(name);
            entity.systemID = systemID;
            entity.publicID = publicID;
            ((ParsedGeneralEntity) entity).value = value;
            if (!dtd.unparsedEntities.containsKey(name) && !dtd.parsedGeneralEntities.containsKey(name)) {
                dtd.parsedGeneralEntities.put(name, entity);
            }
        }
        discardWhitespace();
        requireChar('>');
    }

    void parseEquals() throws DTDException, MalformedURLException, IOException, EOFException {
        discardWhitespace();
        requireChar('=');
        discardWhitespace();
    }

    void parseExternalSubset(boolean eofOK) throws DTDException, MalformedURLException, IOException, EOFException {
        entityState = STATE_DTD;
        if (isString("<?xml")) {
            parseTextDecl();
        }
        parseExternalSubsetDecl(eofOK);
        entityState = STATE_OUTSIDEDTD;
    }

    void parseExternalSubsetDecl(boolean eofOK) throws DTDException, MalformedURLException, IOException, EOFException {
        boolean declFound = true;
        while (declFound) {
            try {
                discardWhitespace();
            } catch (EOFException eof) {
                if (eofOK) return;
                throw eof;
            }
            declFound = parseMarkupDecl();
            if (!declFound) {
                declFound = parseConditional();
            }
        }
    }

    boolean parseIgnore() throws DTDException, MalformedURLException, IOException, EOFException {
        char c;
        int state = 0;
        while (true) {
            c = nextChar();
            switch(state) {
                case 0:
                    if (c == '<') state = 1; else if (c == ']') state = 3;
                    break;
                case 1:
                    state = (c == '!') ? 2 : 0;
                    break;
                case 2:
                    if (c == '[') return false;
                    state = 0;
                    break;
                case 3:
                    state = (c == ']') ? 4 : 0;
                    break;
                case 4:
                    if (c == ']') return true;
                    state = 0;
                    break;
            }
        }
    }

    void parseIgnoreSect() throws DTDException, MalformedURLException, IOException, EOFException {
        discardWhitespace();
        requireChar('[');
        parseIgnoreSectContents();
    }

    void parseIgnoreSectContents() throws DTDException, MalformedURLException, IOException, EOFException {
        int open = 1;
        while (open > 0) {
            open = (parseIgnore()) ? open - 1 : open + 1;
        }
    }

    void parseInclude() throws DTDException, MalformedURLException, IOException, EOFException {
        discardWhitespace();
        requireChar('[');
        parseExternalSubsetDecl(false);
        requireString("]]>");
    }

    void parseInternalSubset() throws DTDException, MalformedURLException, IOException, EOFException {
        boolean declFound = true;
        entityState = STATE_DTD;
        while (declFound) {
            discardWhitespace();
            declFound = parseMarkupDecl();
        }
        entityState = STATE_OUTSIDEDTD;
    }

    boolean parseMarkupDecl() throws DTDException, MalformedURLException, IOException, EOFException {
        String name;
        if (!isChar('<')) return false;
        if (isString("!--")) {
            parseComment();
        } else if (isChar('!')) {
            name = getName();
            switch(dtdTokens.getToken(name)) {
                case DTDConst.KEYWD_TOKEN_ELEMENT:
                    parseElementDecl();
                    break;
                case DTDConst.KEYWD_TOKEN_ATTLIST:
                    parseAttlistDecl();
                    break;
                case DTDConst.KEYWD_TOKEN_ENTITY:
                    parseEntityDecl();
                    break;
                case DTDConst.KEYWD_TOKEN_NOTATION:
                    parseNotationDecl();
                    break;
                default:
                    throwDTDException("Invalid markup declaration: <!" + name + ".");
            }
        } else if (isChar('?')) {
            parsePI();
        } else {
            return false;
        }
        return true;
    }

    void parseMisc() throws DTDException, MalformedURLException, IOException, EOFException {
        boolean miscFound = true;
        while (miscFound) {
            discardWhitespace();
            if (isString("<!--")) {
                parseComment();
            } else if (isString("<?")) {
                parsePI();
            } else {
                miscFound = false;
            }
        }
    }

    void parseNotationDecl() throws DTDException, MalformedURLException, IOException, EOFException {
        Notation notation;
        String keywd;
        notation = Factory.newNotation();
        requireWhitespace();
        notation.name = getName();
        requireWhitespace();
        keywd = getName();
        switch(dtdTokens.getToken(keywd)) {
            case DTDConst.KEYWD_TOKEN_SYSTEM:
                notation.systemID = parseSystemLiteral();
                discardWhitespace();
                requireChar('>');
                break;
            case DTDConst.KEYWD_TOKEN_PUBLIC:
                notation.publicID = parsePublicID();
                if (!isChar('>')) {
                    requireWhitespace();
                    if (!isChar('>')) {
                        notation.systemID = getSystemLiteral();
                        discardWhitespace();
                        requireChar('>');
                    }
                }
                break;
            default:
                throwDTDException("Invalid keyword in notation declaration: " + keywd + ".");
        }
        dtd.notations.put(notation.name, notation);
    }

    void parsePI() throws DTDException, MalformedURLException, IOException, EOFException {
        discardUntil("?>");
    }

    String parsePublicID() throws DTDException, MalformedURLException, IOException, EOFException {
        requireWhitespace();
        return getPubidLiteral();
    }

    void parseStandalone() throws DTDException, MalformedURLException, IOException, EOFException {
        String yesno;
        parseEquals();
        getYesNo();
    }

    String parseSystemLiteral() throws DTDException, MalformedURLException, IOException, EOFException {
        requireWhitespace();
        return getSystemLiteral();
    }

    void parseTextDecl() throws DTDException, MalformedURLException, IOException, EOFException {
        requireWhitespace();
        if (isString("version")) {
            parseVersion();
            requireWhitespace();
        }
        requireString("encoding");
        parseEncodingDecl();
        discardWhitespace();
        requireString("?>");
    }

    void parseVersion() throws DTDException, MalformedURLException, IOException, EOFException {
        char quote;
        parseEquals();
        quote = getQuote();
        requireString("1.0");
        requireChar(quote);
    }

    void parseXMLDecl() throws DTDException, MalformedURLException, IOException, EOFException {
        if (!isWhitespace()) {
            parsePI();
            return;
        }
        discardWhitespace();
        requireString("version");
        parseVersion();
        if (isWhitespace()) {
            discardWhitespace();
            if (isString("encoding")) {
                parseEncodingDecl();
                if (!isWhitespace()) return;
                discardWhitespace();
            }
            if (isString("standalone")) {
                parseStandalone();
                discardWhitespace();
            }
        }
        requireString("?>");
    }

    ElementType addElementType() throws DTDException, MalformedURLException, IOException, EOFException {
        NSName name;
        name = getNSName();
        return dtd.addElementType(name);
    }

    void getAttDef(ElementType elementType) throws DTDException, MalformedURLException, IOException, EOFException {
        Attribute attribute;
        attribute = getAttribute(elementType);
        requireWhitespace();
        getAttributeType(attribute);
        requireWhitespace();
        getAttributeRequired(attribute);
    }

    Attribute getAttribute(ElementType elementType) throws DTDException, MalformedURLException, IOException, EOFException {
        NSName name;
        Attribute attribute;
        name = getNSName();
        attribute = Factory.newAttribute(name);
        if (!elementType.attributes.containsKey(name.qualified)) {
            elementType.attributes.put(name.qualified, attribute);
        }
        return attribute;
    }

    void getAttributeDefault(Attribute attribute) throws DTDException, MalformedURLException, IOException, EOFException {
        attribute.defaultValue = getAttValue();
    }

    void getAttributeRequired(Attribute attribute) throws DTDException, MalformedURLException, IOException, EOFException {
        String name;
        if (isChar('#')) {
            name = getName();
            switch(dtdTokens.getToken(name)) {
                case DTDConst.KEYWD_TOKEN_REQUIRED:
                    attribute.required = Attribute.REQUIRED_REQUIRED;
                    break;
                case DTDConst.KEYWD_TOKEN_IMPLIED:
                    attribute.required = Attribute.REQUIRED_OPTIONAL;
                    break;
                case DTDConst.KEYWD_TOKEN_FIXED:
                    attribute.required = Attribute.REQUIRED_FIXED;
                    requireWhitespace();
                    getAttributeDefault(attribute);
                    break;
                default:
                    throwDTDException("Invalid attribute default: " + name + ".");
            }
        } else {
            attribute.required = Attribute.REQUIRED_DEFAULT;
            getAttributeDefault(attribute);
        }
    }

    void getAttributeType(Attribute attribute) throws DTDException, MalformedURLException, IOException, EOFException {
        String name;
        if (isChar('(')) {
            attribute.type = Attribute.TYPE_ENUMERATED;
            getEnumeration(attribute, false);
            return;
        }
        name = getName();
        switch(dtdTokens.getToken(name)) {
            case DTDConst.KEYWD_TOKEN_CDATA:
                attribute.type = Attribute.TYPE_CDATA;
                break;
            case DTDConst.KEYWD_TOKEN_ID:
                attribute.type = Attribute.TYPE_ID;
                break;
            case DTDConst.KEYWD_TOKEN_IDREF:
                attribute.type = Attribute.TYPE_IDREF;
                break;
            case DTDConst.KEYWD_TOKEN_IDREFS:
                attribute.type = Attribute.TYPE_IDREFS;
                break;
            case DTDConst.KEYWD_TOKEN_ENTITY:
                attribute.type = Attribute.TYPE_ENTITY;
                break;
            case DTDConst.KEYWD_TOKEN_ENTITIES:
                attribute.type = Attribute.TYPE_ENTITIES;
                break;
            case DTDConst.KEYWD_TOKEN_NMTOKEN:
                attribute.type = Attribute.TYPE_NMTOKEN;
                break;
            case DTDConst.KEYWD_TOKEN_NMTOKENS:
                attribute.type = Attribute.TYPE_NMTOKENS;
                break;
            case DTDConst.KEYWD_TOKEN_NOTATION:
                attribute.type = Attribute.TYPE_NOTATION;
                requireWhitespace();
                requireChar('(');
                getEnumeration(attribute, true);
                break;
            default:
                throwDTDException("Invalid attribute type: " + name + ".");
        }
    }

    void getContentModel(ElementType elementType) throws DTDException, MalformedURLException, IOException, EOFException {
        if (isChar('(')) {
            discardWhitespace();
            if (isChar('#')) {
                getMixedContent(elementType);
            } else {
                getElementContent(elementType);
            }
        } else if (isString("EMPTY")) {
            elementType.contentType = ElementType.CONTENT_EMPTY;
        } else if (isString("ANY")) {
            elementType.contentType = ElementType.CONTENT_ANY;
        } else throwDTDException("Invalid element type declaration.");
    }

    void getContentParticle(Group group, ElementType parent) throws DTDException, MalformedURLException, IOException, EOFException {
        Group childGroup;
        Reference ref;
        if (isChar('(')) {
            childGroup = Factory.newGroup();
            group.members.addElement(childGroup);
            getGroup(childGroup, parent);
        } else {
            ref = getReference(group, parent, false);
            getFrequency(ref);
        }
    }

    void getElementContent(ElementType elementType) throws DTDException, MalformedURLException, IOException, EOFException {
        elementType.content = Factory.newGroup();
        elementType.contentType = ElementType.CONTENT_ELEMENT;
        getGroup(elementType.content, elementType);
    }

    ElementType getElementType() throws DTDException, MalformedURLException, IOException, EOFException {
        NSName name;
        name = getNSName();
        return dtd.getElementType(name);
    }

    void getEnumeratedValue(Attribute attribute, boolean useNames, Hashtable enums) throws DTDException, MalformedURLException, IOException, EOFException {
        String name;
        discardWhitespace();
        name = useNames ? getName() : getNmtoken();
        if (enums.containsKey(name)) throwDTDException("Enumerated values must be unique: " + name + ".");
        attribute.enums.addElement(name);
        discardWhitespace();
    }

    void getEnumeration(Attribute attribute, boolean useNames) throws DTDException, MalformedURLException, IOException, EOFException {
        String name;
        Hashtable enums = new Hashtable();
        attribute.enums = new Vector();
        getEnumeratedValue(attribute, useNames, enums);
        while (!isChar(')')) {
            requireChar('|');
            getEnumeratedValue(attribute, useNames, enums);
        }
    }

    void getFrequency(Particle particle) throws DTDException, MalformedURLException, IOException, EOFException {
        if (isChar('?')) {
            particle.isRequired = false;
            particle.isRepeatable = false;
        } else if (isChar('+')) {
            particle.isRequired = true;
            particle.isRepeatable = true;
        } else if (isChar('*')) {
            particle.isRequired = false;
            particle.isRepeatable = true;
        } else {
            particle.isRequired = true;
            particle.isRepeatable = false;
        }
    }

    void getGroup(Group group, ElementType parent) throws DTDException, MalformedURLException, IOException, EOFException {
        boolean moreCPs = true;
        while (moreCPs) {
            discardWhitespace();
            getContentParticle(group, parent);
            discardWhitespace();
            if (isChar('|')) {
                if (group.type == Particle.PARTICLE_UNKNOWN) {
                    group.type = Particle.PARTICLE_CHOICE;
                } else if (group.type == Particle.PARTICLE_SEQUENCE) {
                    throwDTDException("Invalid mixture of ',' and '|' in content model.");
                }
            } else if (isChar(',')) {
                if (group.type == Particle.PARTICLE_UNKNOWN) {
                    group.type = Particle.PARTICLE_SEQUENCE;
                } else if (group.type == Particle.PARTICLE_CHOICE) {
                    throwDTDException("Invalid mixture of ',' and '|' in content model.");
                }
            } else if (isChar(')')) {
                moreCPs = false;
                getFrequency(group);
                if (group.type == Particle.PARTICLE_UNKNOWN) {
                    group.type = Particle.PARTICLE_SEQUENCE;
                }
            }
        }
    }

    void getMixedContent(ElementType parent) throws DTDException, MalformedURLException, IOException, EOFException {
        boolean moreNames = true;
        discardWhitespace();
        requireString("PCDATA");
        discardWhitespace();
        if (isChar('|')) {
            parent.contentType = ElementType.CONTENT_MIXED;
            parent.content = Factory.newGroup();
            parent.content.type = Particle.PARTICLE_CHOICE;
            parent.content.isRequired = false;
            parent.content.isRepeatable = true;
            while (moreNames) {
                discardWhitespace();
                getReference(parent.content, parent, true);
                discardWhitespace();
                moreNames = isChar('|');
            }
            requireString(")*");
        } else {
            parent.contentType = ElementType.CONTENT_PCDATA;
            requireChar(')');
            isChar('*');
        }
    }

    NSName getNSName() throws DTDException, MalformedURLException, IOException, EOFException {
        String prefixedName;
        prefixedName = getName();
        return NSName.getNSName(prefixedName, namespaceURIs);
    }

    Reference getReference(Group group, ElementType parent, boolean mixed) throws DTDException, MalformedURLException, IOException, EOFException {
        NSName name;
        ElementType child;
        Reference ref;
        child = getElementType();
        if (mixed) {
            if (parent.children.containsKey(child.name.qualified)) throwDTDException("The element type " + child.name.qualified + " appeared more than once in the declaration of mixed content for the element type " + child.name.qualified + ".");
        }
        parent.children.put(child.name.qualified, child);
        child.parents.put(parent.name.qualified, parent);
        ref = Factory.newReference(child);
        group.members.addElement(ref);
        return ref;
    }

    void initGlobals() throws MalformedURLException {
        dtd = Factory.newDTD();
        entityState = STATE_OUTSIDEDTD;
        readerStack = new Stack();
        initReaderGlobals();
    }

    void initPredefinedEntities() {
        ParsedGeneralEntity entity;
        entity = Factory.newParsedGeneralEntity("lt");
        entity.value = "<";
        predefinedEntities.put(entity.name, entity);
        entity = Factory.newParsedGeneralEntity("gt");
        entity.value = ">";
        predefinedEntities.put(entity.name, entity);
        entity = Factory.newParsedGeneralEntity("amp");
        entity.value = "&";
        predefinedEntities.put(entity.name, entity);
        entity = Factory.newParsedGeneralEntity("apos");
        entity.value = "'";
        predefinedEntities.put(entity.name, entity);
        entity = Factory.newParsedGeneralEntity("quot");
        entity.value = "\"";
        predefinedEntities.put(entity.name, entity);
    }

    void postProcessDTD() throws DTDException {
        if (dtd != null) {
            dtd.updateANYParents();
            dtd.checkElementTypeReferences();
            dtd.checkNotationReferences();
        }
    }

    void throwDTDException(String s) throws DTDException {
        throw Factory.newDTDException(s + " Line: " + line + " Column: " + column);
    }

    boolean isWhitespace() throws DTDException, MalformedURLException, IOException, EOFException {
        if (isWhitespace(nextChar())) return true;
        restore();
        return false;
    }

    void requireWhitespace() throws DTDException, MalformedURLException, IOException, EOFException {
        if (!isWhitespace()) throwDTDException("Whitespace required.");
        discardWhitespace();
    }

    void discardWhitespace() throws DTDException, MalformedURLException, IOException, EOFException {
        while (isWhitespace()) ;
    }

    void discardUntil(String s) throws DTDException, MalformedURLException, IOException, EOFException {
        char[] chars = s.toCharArray();
        char c;
        int pos = 0;
        while (pos < chars.length) {
            c = nextChar();
            pos = (c == chars[pos]) ? pos + 1 : 0;
        }
    }

    boolean isString(String s) throws DTDException, MalformedURLException, IOException, EOFException {
        char[] chars = s.toCharArray();
        char c;
        int pos = 0;
        for (int i = 0; i < chars.length; i++) {
            if ((c = nextChar()) != chars[i]) {
                chars[i] = c;
                restore(new String(chars, 0, i + 1));
                return false;
            }
        }
        return true;
    }

    boolean isChar(char c) throws DTDException, MalformedURLException, IOException, EOFException {
        if (nextChar() == c) return true;
        restore();
        return false;
    }

    void requireString(String s) throws DTDException, MalformedURLException, IOException, EOFException {
        if (!isString(s)) throwDTDException("String required: " + s + ".");
    }

    void requireChar(char c) throws DTDException, MalformedURLException, IOException, EOFException {
        if (!isChar(c)) throwDTDException("Character required: " + c + ".");
    }

    String getAttValue() throws DTDException, MalformedURLException, IOException, EOFException {
        char quote, c;
        entityState = STATE_ATTVALUE;
        quote = getQuote();
        resetLiteralBuffer();
        c = nextChar();
        while ((c != quote) || ignoreQuote) {
            if ((c == '<') || (c == '&')) {
                if (!ignoreMarkup) throwDTDException("Markup character '" + c + "' not allowed in default attribute value.");
            }
            appendLiteralBuffer(c);
            c = nextChar();
        }
        entityState = STATE_DTD;
        return getLiteralBuffer();
    }

    String getEncName() throws DTDException, MalformedURLException, IOException, EOFException {
        char quote, c;
        quote = getQuote();
        resetLiteralBuffer();
        c = nextChar();
        if (!isLatinLetter(c)) throwDTDException("Invalid starting character in encoding name: " + c + ".");
        while ((c = nextChar()) != quote) {
            if (!isLatinLetter(c) && !isLatinDigit(c) && (c != '.') && (c != '_') && (c != '-')) throwDTDException("Invalid character in encoding name: " + c + ".");
            appendLiteralBuffer(c);
        }
        return getLiteralBuffer();
    }

    String getEntityValue() throws DTDException, MalformedURLException, IOException, EOFException {
        char quote, c;
        entityState = STATE_ENTITYVALUE;
        quote = getQuote();
        resetLiteralBuffer();
        c = nextChar();
        while ((c != quote) || ignoreQuote) {
            if ((c == '<') || (c == '%')) {
                if (!ignoreMarkup) throwDTDException("Markup character '" + c + "' not allowed in entity value.");
            }
            appendLiteralBuffer(c);
            c = nextChar();
        }
        entityState = STATE_DTD;
        return getLiteralBuffer();
    }

    String getName() throws DTDException, MalformedURLException, IOException, EOFException {
        char c;
        resetLiteralBuffer();
        c = nextChar();
        if (!isLetter(c) && (c != '_') && (c != ':')) throwDTDException("Invalid name start character: " + c + ".");
        while (isNameChar(c)) {
            appendLiteralBuffer(c);
            c = nextChar();
        }
        restore();
        return getLiteralBuffer();
    }

    String getNmtoken() throws DTDException, MalformedURLException, IOException, EOFException {
        char c;
        resetLiteralBuffer();
        c = nextChar();
        if (!isNameChar(c)) throwDTDException("Invalid Nmtoken start character: " + c + ".");
        while (isNameChar(c)) {
            appendLiteralBuffer(c);
            c = nextChar();
        }
        restore();
        return getLiteralBuffer();
    }

    String getPubidLiteral() throws DTDException, MalformedURLException, IOException, EOFException {
        char quote, c;
        quote = getQuote();
        resetLiteralBuffer();
        while ((c = nextChar()) != quote) {
            if (!isPubidChar(c)) throwDTDException("Invalid character in public identifier: " + c + ".");
            appendLiteralBuffer(c);
        }
        return getLiteralBuffer();
    }

    char getQuote() throws DTDException, MalformedURLException, IOException, EOFException {
        char quote;
        quote = nextChar();
        if ((quote != '\'') && (quote != '"')) throwDTDException("Quote character required.");
        return quote;
    }

    String getSystemLiteral() throws DTDException, MalformedURLException, IOException, EOFException {
        char quote, c;
        quote = getQuote();
        resetLiteralBuffer();
        while ((c = nextChar()) != quote) {
            appendLiteralBuffer(c);
        }
        return getLiteralBuffer();
    }

    String getYesNo() throws DTDException, MalformedURLException, IOException, EOFException {
        char quote;
        boolean no = true;
        quote = getQuote();
        if (!isString("no")) {
            requireString("yes");
            no = false;
        }
        requireChar(quote);
        return ((no) ? "no" : "yes");
    }

    void resetLiteralBuffer() {
        literalPos = -1;
        literalStr = null;
    }

    void appendLiteralBuffer(char c) {
        literalPos++;
        if (literalPos >= LITBUFSIZE) {
            if (literalStr == null) {
                literalStr = new StringBuffer();
            }
            literalStr.append(literalBuffer);
            literalPos = 0;
        }
        literalBuffer[literalPos] = c;
    }

    String getLiteralBuffer() {
        if (literalStr == null) {
            return new String(literalBuffer, 0, literalPos + 1);
        } else {
            literalStr.append(literalBuffer, 0, literalPos + 1);
            return literalStr.toString();
        }
    }

    boolean isWhitespace(char c) {
        switch(c) {
            case 0x20:
            case 0x09:
            case 0x0a:
            case 0x0d:
                return true;
            default:
                return false;
        }
    }

    boolean isLatinLetter(char c) {
        return (((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z')));
    }

    boolean isLatinDigit(char c) {
        return ((c >= '0') && (c <= '9'));
    }

    boolean isPubidChar(char c) {
        switch(c) {
            case '-':
            case '\'':
            case '(':
            case ')':
            case '+':
            case ',':
            case '.':
            case '/':
            case ':':
            case '=':
            case '?':
            case ';':
            case '!':
            case '*':
            case '#':
            case '@':
            case '$':
            case '_':
            case '%':
            case 0x20:
            case 0xD:
            case 0xA:
                return true;
            default:
                return (isLatinLetter(c) || isLatinDigit(c));
        }
    }

    boolean isNameChar(char c) {
        if (isLatinLetter(c)) return true;
        if (isLatinDigit(c)) return true;
        if ((c == '.') || (c == '-') || (c == '_') || (c == ':')) return true;
        if (isLetter(c)) return true;
        if (isDigit(c)) return true;
        if (isCombiningChar(c)) return true;
        if (isExtender(c)) return true;
        return false;
    }

    boolean isLetter(char c) {
        switch(c >> 8) {
            case 0x00:
                if ((c >= 0x0041) && (c <= 0x005A)) return true;
                if ((c >= 0x0061) && (c <= 0x007A)) return true;
                if ((c >= 0x00C0) && (c <= 0x00D6)) return true;
                if ((c >= 0x00D8) && (c <= 0x00F6)) return true;
                if ((c >= 0x00F8) && (c <= 0x00FF)) return true;
                return false;
            case 0x01:
                if ((c >= 0x0100) && (c <= 0x0131)) return true;
                if ((c >= 0x0134) && (c <= 0x013E)) return true;
                if ((c >= 0x0141) && (c <= 0x0148)) return true;
                if ((c >= 0x014A) && (c <= 0x017E)) return true;
                if ((c >= 0x0180) && (c <= 0x01C3)) return true;
                if ((c >= 0x01CD) && (c <= 0x01F0)) return true;
                if ((c >= 0x01F4) && (c <= 0x01F5)) return true;
                if ((c >= 0x01FA) && (c <= 0x01FF)) return true;
                return false;
            case 0x02:
                if ((c >= 0x0200) && (c <= 0x0217)) return true;
                if ((c >= 0x0250) && (c <= 0x02A8)) return true;
                if ((c >= 0x02BB) && (c <= 0x02C1)) return true;
                return false;
            case 0x03:
                if ((c >= 0x0388) && (c <= 0x038A)) return true;
                if ((c >= 0x038E) && (c <= 0x03A1)) return true;
                if ((c >= 0x03A3) && (c <= 0x03CE)) return true;
                if ((c >= 0x03D0) && (c <= 0x03D6)) return true;
                if ((c >= 0x03E2) && (c <= 0x03F3)) return true;
                if ((c == 0x0386) || (c == 0x038C) || (c == 0x03DA) || (c == 0x03DC) || (c == 0x03DE) || (c == 0x03E0)) return true;
                return false;
            case 0x04:
                if ((c >= 0x0401) && (c <= 0x040C)) return true;
                if ((c >= 0x040E) && (c <= 0x044F)) return true;
                if ((c >= 0x0451) && (c <= 0x045C)) return true;
                if ((c >= 0x045E) && (c <= 0x0481)) return true;
                if ((c >= 0x0490) && (c <= 0x04C4)) return true;
                if ((c >= 0x04C7) && (c <= 0x04C8)) return true;
                if ((c >= 0x04CB) && (c <= 0x04CC)) return true;
                if ((c >= 0x04D0) && (c <= 0x04EB)) return true;
                if ((c >= 0x04EE) && (c <= 0x04F5)) return true;
                if ((c >= 0x04F8) && (c <= 0x04F9)) return true;
                return false;
            case 0x05:
                if ((c >= 0x0531) && (c <= 0x0556)) return true;
                if ((c >= 0x0561) && (c <= 0x0586)) return true;
                if ((c >= 0x05D0) && (c <= 0x05EA)) return true;
                if ((c >= 0x05F0) && (c <= 0x05F2)) return true;
                if (c == 0x0559) return true;
                return false;
            case 0x06:
                if ((c >= 0x0621) && (c <= 0x063A)) return true;
                if ((c >= 0x0641) && (c <= 0x064A)) return true;
                if ((c >= 0x0671) && (c <= 0x06B7)) return true;
                if ((c >= 0x06BA) && (c <= 0x06BE)) return true;
                if ((c >= 0x06C0) && (c <= 0x06CE)) return true;
                if ((c >= 0x06D0) && (c <= 0x06D3)) return true;
                if ((c >= 0x06E5) && (c <= 0x06E6)) return true;
                if (c == 0x06D5) return true;
                return false;
            case 0x09:
                if ((c >= 0x0905) && (c <= 0x0939)) return true;
                if ((c >= 0x0958) && (c <= 0x0961)) return true;
                if ((c >= 0x0985) && (c <= 0x098C)) return true;
                if ((c >= 0x098F) && (c <= 0x0990)) return true;
                if ((c >= 0x0993) && (c <= 0x09A8)) return true;
                if ((c >= 0x09AA) && (c <= 0x09B0)) return true;
                if ((c >= 0x09B6) && (c <= 0x09B9)) return true;
                if ((c >= 0x09DC) && (c <= 0x09DD)) return true;
                if ((c >= 0x09DF) && (c <= 0x09E1)) return true;
                if ((c >= 0x09F0) && (c <= 0x09F1)) return true;
                if ((c == 0x093D) || (c == 0x09B2)) return true;
                return false;
            case 0x0A:
                if ((c >= 0x0A05) && (c <= 0x0A0A)) return true;
                if ((c >= 0x0A0F) && (c <= 0x0A10)) return true;
                if ((c >= 0x0A13) && (c <= 0x0A28)) return true;
                if ((c >= 0x0A2A) && (c <= 0x0A30)) return true;
                if ((c >= 0x0A32) && (c <= 0x0A33)) return true;
                if ((c >= 0x0A35) && (c <= 0x0A36)) return true;
                if ((c >= 0x0A38) && (c <= 0x0A39)) return true;
                if ((c >= 0x0A59) && (c <= 0x0A5C)) return true;
                if ((c >= 0x0A72) && (c <= 0x0A74)) return true;
                if ((c >= 0x0A85) && (c <= 0x0A8B)) return true;
                if ((c >= 0x0A8F) && (c <= 0x0A91)) return true;
                if ((c >= 0x0A93) && (c <= 0x0AA8)) return true;
                if ((c >= 0x0AAA) && (c <= 0x0AB0)) return true;
                if ((c >= 0x0AB2) && (c <= 0x0AB3)) return true;
                if ((c >= 0x0AB5) && (c <= 0x0AB9)) return true;
                if ((c == 0x0A5E) || (c == 0x0A8D) || (c == 0x0ABD) || (c == 0x0AE0)) return true;
                return false;
            case 0x0B:
                if ((c >= 0x0B05) && (c <= 0x0B0C)) return true;
                if ((c >= 0x0B0F) && (c <= 0x0B10)) return true;
                if ((c >= 0x0B13) && (c <= 0x0B28)) return true;
                if ((c >= 0x0B2A) && (c <= 0x0B30)) return true;
                if ((c >= 0x0B32) && (c <= 0x0B33)) return true;
                if ((c >= 0x0B36) && (c <= 0x0B39)) return true;
                if ((c >= 0x0B5C) && (c <= 0x0B5D)) return true;
                if ((c >= 0x0B5F) && (c <= 0x0B61)) return true;
                if ((c >= 0x0B85) && (c <= 0x0B8A)) return true;
                if ((c >= 0x0B8E) && (c <= 0x0B90)) return true;
                if ((c >= 0x0B92) && (c <= 0x0B95)) return true;
                if ((c >= 0x0B99) && (c <= 0x0B9A)) return true;
                if ((c >= 0x0B9E) && (c <= 0x0B9F)) return true;
                if ((c >= 0x0BA3) && (c <= 0x0BA4)) return true;
                if ((c >= 0x0BA8) && (c <= 0x0BAA)) return true;
                if ((c >= 0x0BAE) && (c <= 0x0BB5)) return true;
                if ((c >= 0x0BB7) && (c <= 0x0BB9)) return true;
                if ((c == 0x0B3D) || (c == 0x0B9C)) return true;
                return false;
            case 0x0C:
                if ((c >= 0x0C05) && (c <= 0x0C0C)) return true;
                if ((c >= 0x0C0E) && (c <= 0x0C10)) return true;
                if ((c >= 0x0C12) && (c <= 0x0C28)) return true;
                if ((c >= 0x0C2A) && (c <= 0x0C33)) return true;
                if ((c >= 0x0C35) && (c <= 0x0C39)) return true;
                if ((c >= 0x0C60) && (c <= 0x0C61)) return true;
                if ((c >= 0x0C85) && (c <= 0x0C8C)) return true;
                if ((c >= 0x0C8E) && (c <= 0x0C90)) return true;
                if ((c >= 0x0C92) && (c <= 0x0CA8)) return true;
                if ((c >= 0x0CAA) && (c <= 0x0CB3)) return true;
                if ((c >= 0x0CB5) && (c <= 0x0CB9)) return true;
                if ((c >= 0x0CE0) && (c <= 0x0CE1)) return true;
                if (c == 0x0CDE) return true;
                return false;
            case 0x0D:
                if ((c >= 0x0D05) && (c <= 0x0D0C)) return true;
                if ((c >= 0x0D0E) && (c <= 0x0D10)) return true;
                if ((c >= 0x0D12) && (c <= 0x0D28)) return true;
                if ((c >= 0x0D2A) && (c <= 0x0D39)) return true;
                if ((c >= 0x0D60) && (c <= 0x0D61)) return true;
                return false;
            case 0x0E:
                if ((c >= 0x0E01) && (c <= 0x0E2E)) return true;
                if ((c >= 0x0E32) && (c <= 0x0E33)) return true;
                if ((c >= 0x0E40) && (c <= 0x0E45)) return true;
                if ((c >= 0x0E81) && (c <= 0x0E82)) return true;
                if ((c >= 0x0E87) && (c <= 0x0E88)) return true;
                if ((c >= 0x0E94) && (c <= 0x0E97)) return true;
                if ((c >= 0x0E99) && (c <= 0x0E9F)) return true;
                if ((c >= 0x0EA1) && (c <= 0x0EA3)) return true;
                if ((c >= 0x0EAA) && (c <= 0x0EAB)) return true;
                if ((c >= 0x0EAD) && (c <= 0x0EAE)) return true;
                if ((c >= 0x0EB2) && (c <= 0x0EB3)) return true;
                if ((c >= 0x0EC0) && (c <= 0x0EC4)) return true;
                if ((c == 0x0E30) || (c == 0x0E84) || (c == 0x0E8A) || (c == 0x0E8D) || (c == 0x0EA5) || (c == 0x0EA7) || (c == 0x0EB0) || (c == 0x0EBD)) return true;
                return false;
            case 0x0F:
                if ((c >= 0x0F40) && (c <= 0x0F47)) return true;
                if ((c >= 0x0F49) && (c <= 0x0F69)) return true;
                return false;
            case 0x10:
                if ((c >= 0x10A0) && (c <= 0x10C5)) return true;
                if ((c >= 0x10D0) && (c <= 0x10F6)) return true;
                return false;
            case 0x11:
                if ((c >= 0x1102) && (c <= 0x1103)) return true;
                if ((c >= 0x1105) && (c <= 0x1107)) return true;
                if ((c >= 0x110B) && (c <= 0x110C)) return true;
                if ((c >= 0x110E) && (c <= 0x1112)) return true;
                if ((c >= 0x1154) && (c <= 0x1155)) return true;
                if ((c >= 0x115F) && (c <= 0x1161)) return true;
                if ((c >= 0x116D) && (c <= 0x116E)) return true;
                if ((c >= 0x1172) && (c <= 0x1173)) return true;
                if ((c >= 0x11AE) && (c <= 0x11AF)) return true;
                if ((c >= 0x11B7) && (c <= 0x11B8)) return true;
                if ((c >= 0x11BC) && (c <= 0x11C2)) return true;
                if ((c == 0x1100) || (c == 0x1109) || (c == 0x113C) || (c == 0x113E) || (c == 0x1140) || (c == 0x114C) || (c == 0x114E) || (c == 0x1150) || (c == 0x1159) || (c == 0x1163) || (c == 0x1165) || (c == 0x1167) || (c == 0x1169) || (c == 0x1175) || (c == 0x119E) || (c == 0x11A8) || (c == 0x11AB) || (c == 0x11BA) || (c == 0x11EB) || (c == 0x11F0) || (c == 0x11F9)) return true;
                return false;
            case 0x1E:
                if ((c >= 0x1E00) && (c <= 0x1E9B)) return true;
                if ((c >= 0x1EA0) && (c <= 0x1EF9)) return true;
                return false;
            case 0x1F:
                if ((c >= 0x1F00) && (c <= 0x1F15)) return true;
                if ((c >= 0x1F18) && (c <= 0x1F1D)) return true;
                if ((c >= 0x1F20) && (c <= 0x1F45)) return true;
                if ((c >= 0x1F48) && (c <= 0x1F4D)) return true;
                if ((c >= 0x1F50) && (c <= 0x1F57)) return true;
                if ((c >= 0x1F5F) && (c <= 0x1F7D)) return true;
                if ((c >= 0x1F80) && (c <= 0x1FB4)) return true;
                if ((c >= 0x1FB6) && (c <= 0x1FBC)) return true;
                if ((c >= 0x1FC2) && (c <= 0x1FC4)) return true;
                if ((c >= 0x1FC6) && (c <= 0x1FCC)) return true;
                if ((c >= 0x1FD0) && (c <= 0x1FD3)) return true;
                if ((c >= 0x1FD6) && (c <= 0x1FDB)) return true;
                if ((c >= 0x1FE0) && (c <= 0x1FEC)) return true;
                if ((c >= 0x1FF2) && (c <= 0x1FF4)) return true;
                if ((c >= 0x1FF6) && (c <= 0x1FFC)) return true;
                if ((c == 0x1F59) || (c == 0x1F5B) || (c == 0x1F5D) || (c == 0x1FBE)) return true;
                return false;
            case 0x21:
                if ((c >= 0x212A) && (c <= 0x212B)) return true;
                if ((c >= 0x2180) && (c <= 0x2182)) return true;
                if ((c == 0x2126) || (c == 0x212E)) return true;
                return false;
            case 0x20:
                if ((c >= 0x3041) && (c <= 0x3094)) return true;
                if ((c >= 0x30A1) && (c <= 0x30FA)) return true;
                if ((c >= 0x3021) && (c <= 0x3029)) return true;
                if (c == 0x3007) return true;
                return false;
            case 0x31:
                if ((c >= 0x3105) && (c <= 0x312C)) return true;
                return false;
            default:
                if ((c >= 0xAC00) && (c <= 0xD7A3)) return true;
                if ((c >= 0x4E00) && (c <= 0x9FA5)) return true;
                return false;
        }
    }

    boolean isDigit(char c) {
        if (!Character.isDigit(c)) return false;
        return (c > 0xF29);
    }

    boolean isCombiningChar(char c) {
        switch(c >> 8) {
            case 0x03:
                if ((c >= 0x0300) && (c <= 0x0345)) return true;
                if ((c >= 0x0360) && (c <= 0x0361)) return true;
                return false;
            case 0x04:
                if ((c >= 0x0483) && (c <= 0x0486)) return true;
                return false;
            case 0x05:
                if ((c >= 0x0591) && (c <= 0x05A1)) return true;
                if ((c >= 0x05A3) && (c <= 0x05B9)) return true;
                if ((c >= 0x05BB) && (c <= 0x05BD)) return true;
                if ((c >= 0x05C1) && (c <= 0x05C2)) return true;
                if ((c == 0x05BF) || (c == 0x05C4)) return true;
                return false;
            case 0x06:
                if ((c >= 0x064B) && (c <= 0x0652)) return true;
                if ((c >= 0x06D6) && (c <= 0x06DC)) return true;
                if ((c >= 0x06DD) && (c <= 0x06DF)) return true;
                if ((c >= 0x06E0) && (c <= 0x06E4)) return true;
                if ((c >= 0x06E7) && (c <= 0x06E8)) return true;
                if ((c >= 0x06EA) && (c <= 0x06ED)) return true;
                if (c == 0x0670) return true;
                return false;
            case 0x09:
                if ((c >= 0x0901) && (c <= 0x0903)) return true;
                if ((c >= 0x093E) && (c <= 0x094C)) return true;
                if ((c >= 0x0951) && (c <= 0x0954)) return true;
                if ((c >= 0x0962) && (c <= 0x0963)) return true;
                if ((c >= 0x0981) && (c <= 0x0983)) return true;
                if ((c >= 0x09C0) && (c <= 0x09C4)) return true;
                if ((c >= 0x09C7) && (c <= 0x09C8)) return true;
                if ((c >= 0x09CB) && (c <= 0x09CD)) return true;
                if ((c >= 0x09E2) && (c <= 0x09E3)) return true;
                if ((c == 0x093C) || (c == 0x094D) || (c == 0x09BC) || (c == 0x09BE) || (c == 0x09BF) || (c == 0x09D7)) return true;
                return false;
            case 0x0A:
                if ((c >= 0x0A40) && (c <= 0x0A42)) return true;
                if ((c >= 0x0A47) && (c <= 0x0A48)) return true;
                if ((c >= 0x0A4B) && (c <= 0x0A4D)) return true;
                if ((c >= 0x0A70) && (c <= 0x0A71)) return true;
                if ((c >= 0x0A81) && (c <= 0x0A83)) return true;
                if ((c >= 0x0ABE) && (c <= 0x0AC5)) return true;
                if ((c >= 0x0AC7) && (c <= 0x0AC9)) return true;
                if ((c >= 0x0ACB) && (c <= 0x0ACD)) return true;
                if ((c == 0x0A02) || (c == 0x0A3C) || (c == 0x0A3E) || (c == 0x0A3F) || (c == 0x0ABC)) return true;
                return false;
            case 0x0B:
                if ((c >= 0x0B01) && (c <= 0x0B03)) return true;
                if ((c >= 0x0B3E) && (c <= 0x0B43)) return true;
                if ((c >= 0x0B47) && (c <= 0x0B48)) return true;
                if ((c >= 0x0B4B) && (c <= 0x0B4D)) return true;
                if ((c >= 0x0B56) && (c <= 0x0B57)) return true;
                if ((c >= 0x0B82) && (c <= 0x0B83)) return true;
                if ((c >= 0x0BBE) && (c <= 0x0BC2)) return true;
                if ((c >= 0x0BC6) && (c <= 0x0BC8)) return true;
                if ((c >= 0x0BCA) && (c <= 0x0BCD)) return true;
                if ((c == 0x0B3C) || (c == 0x0BD7)) return true;
                return false;
            case 0x0C:
                if ((c >= 0x0C01) && (c <= 0x0C03)) return true;
                if ((c >= 0x0C3E) && (c <= 0x0C44)) return true;
                if ((c >= 0x0C46) && (c <= 0x0C48)) return true;
                if ((c >= 0x0C4A) && (c <= 0x0C4D)) return true;
                if ((c >= 0x0C55) && (c <= 0x0C56)) return true;
                if ((c >= 0x0C82) && (c <= 0x0C83)) return true;
                if ((c >= 0x0CBE) && (c <= 0x0CC4)) return true;
                if ((c >= 0x0CC6) && (c <= 0x0CC8)) return true;
                if ((c >= 0x0CCA) && (c <= 0x0CCD)) return true;
                if ((c >= 0x0CD5) && (c <= 0x0CD6)) return true;
                return false;
            case 0x0D:
                if ((c >= 0x0D02) && (c <= 0x0D03)) return true;
                if ((c >= 0x0D3E) && (c <= 0x0D43)) return true;
                if ((c >= 0x0D46) && (c <= 0x0D48)) return true;
                if ((c >= 0x0D4A) && (c <= 0x0D4D)) return true;
                if (c == 0x0D57) return true;
                return false;
            case 0x0E:
                if ((c >= 0x0E34) && (c <= 0x0E3A)) return true;
                if ((c >= 0x0E47) && (c <= 0x0E4E)) return true;
                if ((c >= 0x0EB4) && (c <= 0x0EB9)) return true;
                if ((c == 0x0EBB) && (c <= 0x0EBC)) return true;
                if ((c >= 0x0EC8) && (c <= 0x0ECD)) return true;
                if ((c == 0x0E31) || (c == 0x0EB1)) return true;
                return false;
            case 0x0F:
                if ((c >= 0x0F18) && (c <= 0x0F19)) return true;
                if ((c >= 0x0F71) && (c <= 0x0F84)) return true;
                if ((c >= 0x0F86) && (c <= 0x0F8B)) return true;
                if ((c >= 0x0F90) && (c <= 0x0F95)) return true;
                if ((c >= 0x0F99) && (c <= 0x0FAD)) return true;
                if ((c >= 0x0FB1) && (c <= 0x0FB7)) return true;
                if ((c == 0x0F35) || (c == 0x0F37) || (c == 0x0F39) || (c == 0x0F3E) || (c == 0x0F3F) || (c == 0x0F97) || (c == 0x0FB9)) return true;
                return false;
            case 0x20:
                if ((c >= 0x20D0) && (c <= 0x20DC)) return true;
                if (c == 0x20E1) return true;
                return false;
            case 0x30:
                if ((c >= 0x302A) && (c <= 0x302F)) return true;
                if ((c == 0x3099) || (c == 0x309A)) return true;
                return false;
            default:
                return false;
        }
    }

    boolean isExtender(char c) {
        switch(c) {
            case 0x00B7:
            case 0x02D0:
            case 0x02D1:
            case 0x0387:
            case 0x0640:
            case 0x0E46:
            case 0x0EC6:
            case 0x3005:
                return true;
            default:
                if ((c >= 0x3031) && (c <= 0x3035)) return true;
                if ((c >= 0x309D) && (c <= 0x309E)) return true;
                if ((c >= 0x30FC) && (c <= 0x30FE)) return true;
                return false;
        }
    }

    char nextChar() throws DTDException, MalformedURLException, IOException, EOFException {
        char c;
        c = getChar();
        switch(c) {
            case '&':
                c = processAmpersand();
                break;
            case '%':
                c = processPercent();
                break;
            default:
                break;
        }
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    char processAmpersand() throws DTDException, IOException, EOFException {
        char c;
        switch(entityState) {
            case STATE_DTD:
                throwDTDException("Invalid general entity reference or character reference.");
            case STATE_ATTVALUE:
                if (getChar() == '#') {
                    getCharRef();
                } else {
                    restore();
                    getGeneralEntityRef();
                }
                return nextChar();
            case STATE_ENTITYVALUE:
                if (getChar() == '#') {
                    getCharRef();
                    return nextChar();
                } else {
                    restore();
                    return '&';
                }
            case STATE_OUTSIDEDTD:
            case STATE_COMMENT:
            case STATE_IGNORE:
                return '&';
            default:
                throw new IllegalStateException("Internal error: invalid entity state: " + entityState);
        }
    }

    char processPercent() throws DTDException, MalformedURLException, IOException, EOFException {
        char c;
        switch(entityState) {
            case STATE_DTD:
                c = getChar();
                restore();
                if (isWhitespace(c)) return '%';
                getParameterEntityRef();
                return nextChar();
            case STATE_ATTVALUE:
                return '%';
            case STATE_ENTITYVALUE:
                getParameterEntityRef();
                return nextChar();
            case STATE_OUTSIDEDTD:
            case STATE_COMMENT:
            case STATE_IGNORE:
                return '%';
            default:
                throw new IllegalStateException("Internal error: invalid entity state: " + entityState);
        }
    }

    void getCharRef() throws DTDException, IOException, EOFException {
        boolean hex = false;
        char c;
        char[] chars = new char[1];
        int value = 0;
        c = getChar();
        if (c == 'x') {
            hex = true;
            c = getChar();
        }
        while (c != ';') {
            if (hex) {
                c = Character.toUpperCase(c);
                if ((c < '0') || (c > 'F') || ((c > '9') && (c < 'A'))) throwDTDException("Invalid character in character reference: " + c + ".");
                value *= 16;
                value += (c < 'A') ? c - '0' : c - 'A' + 10;
            } else {
                if ((c < '0') || (c > '9')) throwDTDException("Invalid character in character reference: " + c + ".");
                value *= 10;
                value += c - '0';
            }
            c = getChar();
        }
        if (value > Character.MAX_VALUE) throwDTDException("Invalid character reference: " + value + ".");
        pushCurrentReader();
        chars[0] = (char) value;
        createStringReader(new String(chars));
        ignoreQuote = true;
        ignoreMarkup = true;
    }

    void getGeneralEntityRef() throws DTDException, IOException, EOFException {
        char c;
        int size;
        String entityName;
        ParsedGeneralEntity entity;
        resetNameBuffer();
        while ((c = getChar()) != ';') {
            appendNameBuffer(c);
        }
        entityName = getNameBuffer();
        pushCurrentReader();
        entity = (ParsedGeneralEntity) dtd.parsedGeneralEntities.get(entityName);
        if (entity == null) {
            entity = (ParsedGeneralEntity) predefinedEntities.get(entityName);
            if (entity == null) throwDTDException("Reference to undefined parsed general entity: " + entityName + ".");
        }
        if (entity.value == null) throwDTDException("Reference to external parsed general entity in attribute value: " + entityName + ".");
        createStringReader(entity.value);
        ignoreQuote = true;
        ignoreMarkup = false;
    }

    void getParameterEntityRef() throws DTDException, MalformedURLException, IOException, EOFException {
        char c;
        String entityName;
        ParameterEntity entity;
        resetNameBuffer();
        while ((c = getChar()) != ';') {
            appendNameBuffer(c);
        }
        entityName = getNameBuffer();
        pushCurrentReader();
        entity = (ParameterEntity) dtd.parameterEntities.get(entityName);
        if (entity == null) throwDTDException("Reference to undefined parameter entity: " + entityName + ".");
        pushStringReader(" ", false, false);
        if (entity.value != null) {
            pushStringReader(entity.value, (entityState == STATE_ENTITYVALUE), false);
        } else {
            pushURLReader(entity.systemID, (entityState == STATE_ENTITYVALUE), false);
        }
        createStringReader(" ");
        ignoreQuote = false;
        ignoreMarkup = false;
    }

    void createStringReader(String s) {
        int size;
        reader = new StringReader(s);
        readerType = READER_READER;
        size = (s.length() > BUFSIZE) ? BUFSIZE : s.length();
        buffer = new char[size];
        bufferPos = BUFSIZE + 1;
        bufferLen = -1;
        line = 1;
        column = 1;
    }

    void createURLReader(URL url) throws IOException {
        reader = new InputStreamReader(url.openStream());
        readerType = READER_READER;
        readerURL = url;
        buffer = new char[BUFSIZE];
        bufferPos = BUFSIZE + 1;
        bufferLen = 0;
        line = 1;
        column = 1;
    }

    void pushCurrentReader() {
        readerStack.push(new ReaderInfo(reader, buffer, readerURL, null, readerType, bufferPos, bufferLen, line, column, ignoreQuote, ignoreMarkup));
    }

    void pushStringReader(String s, boolean ignoreQuote, boolean ignoreMarkup) {
        readerStack.push(new ReaderInfo(null, null, null, s, READER_STRING, 0, 0, 1, 1, ignoreQuote, ignoreMarkup));
    }

    void pushURLReader(String urlString, boolean ignoreQuote, boolean ignoreMarkup) throws MalformedURLException {
        URL url;
        url = new URL(readerURL, urlString);
        readerStack.push(new ReaderInfo(null, null, url, null, READER_URL, 0, 0, 1, 1, ignoreQuote, ignoreMarkup));
    }

    void popReader() throws DTDException, IOException, EOFException {
        ReaderInfo readerInfo;
        if (readerStack.empty()) throw new EOFException("End of file reached while parsing.");
        readerInfo = (ReaderInfo) readerStack.pop();
        switch(readerInfo.type) {
            case READER_READER:
                reader = readerInfo.reader;
                readerType = readerInfo.type;
                readerURL = readerInfo.url;
                buffer = readerInfo.buffer;
                bufferPos = readerInfo.bufferPos;
                bufferLen = readerInfo.bufferLen;
                line = readerInfo.line;
                column = readerInfo.column;
                break;
            case READER_STRING:
                createStringReader(readerInfo.str);
                break;
            case READER_URL:
                createURLReader(readerInfo.url);
                break;
        }
        ignoreQuote = readerInfo.ignoreQuote;
        ignoreMarkup = readerInfo.ignoreMarkup;
    }

    void resetNameBuffer() {
        namePos = -1;
        nameStr = null;
    }

    void appendNameBuffer(char c) {
        namePos++;
        if (namePos >= LITBUFSIZE) {
            if (nameStr == null) {
                nameStr = new StringBuffer();
            }
            nameStr.append(nameBuffer);
            namePos = 0;
        }
        nameBuffer[namePos] = c;
    }

    String getNameBuffer() {
        if (nameStr == null) {
            return new String(nameBuffer, 0, namePos + 1);
        } else {
            nameStr.append(nameBuffer, 0, namePos + 1);
            return nameStr.toString();
        }
    }

    void initReaderGlobals() {
        reader = null;
        readerURL = null;
        line = 1;
        column = 1;
        ignoreQuote = false;
        ignoreMarkup = false;
    }

    char getChar() throws DTDException, IOException, EOFException {
        char c;
        if (bufferPos >= bufferLen) {
            bufferLen = reader.read(buffer, 0, buffer.length);
            if (bufferLen == -1) {
                popReader();
                return getChar();
            } else {
                bufferPos = 0;
            }
        }
        return buffer[bufferPos++];
    }

    void restore() {
        bufferPos--;
    }

    void restore(String s) {
        pushCurrentReader();
        createStringReader(s);
    }

    void openInputSource(InputSource src) throws DTDException, IOException {
        String srcURL;
        Reader srcReader;
        InputStream srcStream;
        srcURL = src.getSystemId();
        try {
            readerURL = new URL(srcURL);
        } catch (MalformedURLException e) {
            readerURL = null;
        }
        if ((srcReader = src.getCharacterStream()) != null) {
            reader = srcReader;
            readerType = READER_READER;
            buffer = new char[BUFSIZE];
            bufferPos = BUFSIZE + 1;
            bufferLen = 0;
        } else if ((srcStream = src.getByteStream()) != null) {
            reader = new InputStreamReader(srcStream);
            readerType = READER_READER;
            buffer = new char[BUFSIZE];
            bufferPos = BUFSIZE + 1;
            bufferLen = 0;
        } else if (readerURL != null) {
            createURLReader(readerURL);
        } else {
            throwDTDException("InputSource does not have a character stream, byte stream, or system ID.");
        }
    }

    class ReaderInfo {

        Reader reader;

        char[] buffer;

        URL url;

        String str;

        int type, bufferPos, bufferLen, line, column;

        boolean ignoreQuote, ignoreMarkup;

        ReaderInfo(Reader reader, char[] buffer, URL url, String str, int type, int bufferPos, int bufferLen, int line, int column, boolean ignoreQuote, boolean ignoreMarkup) {
            this.reader = reader;
            this.buffer = buffer;
            this.url = url;
            this.str = str;
            this.type = type;
            this.bufferPos = bufferPos;
            this.bufferLen = bufferLen;
            this.line = line;
            this.column = column;
            this.ignoreQuote = ignoreQuote;
            this.ignoreMarkup = ignoreMarkup;
        }
    }
}
