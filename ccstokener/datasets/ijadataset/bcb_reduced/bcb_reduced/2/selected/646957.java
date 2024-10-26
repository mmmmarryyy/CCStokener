package org.openexi.fujitsu.sax;

import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import junit.framework.Assert;
import org.w3c.exi.ttf.Event;
import org.w3c.exi.ttf.sax.SAXRecorder;
import org.openexi.fujitsu.proc.common.AlignmentType;
import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.proc.grammars.GrammarCache;
import org.openexi.fujitsu.proc.util.URIConst;
import org.openexi.fujitsu.sax.Transmogrifier;
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.EmptySchema;
import org.openexi.fujitsu.schema.TestBase;
import org.openexi.fujitsu.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.fujitsu.scomp.EXISchemaFactoryTestUtil;

public class EXIReaderTest extends TestBase {

    public EXIReaderTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        m_compilerErrors = new EXISchemaFactoryErrorMonitor();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        m_compilerErrors.clear();
    }

    private EXISchemaFactoryErrorMonitor m_compilerErrors;

    private static final AlignmentType[] Alignments = new AlignmentType[] { AlignmentType.bitPacked, AlignmentType.byteAligned, AlignmentType.preCompress, AlignmentType.compress };

    /**
   * Make use of ITEM_SCHEMA_NS that belongs to an ElementGrammar and ElementTagGrammar.
   * Note that the ITEM_SCHEMA_NS event in ElementTagGrammar cannot be exercised since
   * it never matches an namespace declaration instance. 
   * 
   * Schema:
   * <xsd:complexType name="F">
   *   <xsd:sequence>
   *   ...
   *   </xsd:sequence>
   *   <xsd:attribute ref="foo:aA" use="required"/>
   *   ...
   * </xsd:complexType>
   * 
   * <xsd:element name="F" type="foo:F" nillable="true"/>
   */
    public void testNamespaceDeclaration_01() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
        final String xmlString = "<F xsi:type='F' xmlns='urn:foo' xmlns:foo='urn:foo' " + "   xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + "   foo:aA='abc'>" + "</F>\n";
        for (AlignmentType alignment : Alignments) {
            for (boolean preserveLexicalValues : new boolean[] { true, false }) {
                Transmogrifier encoder = new Transmogrifier();
                EXIReader decoder = new EXIReader();
                encoder.setAlignmentType(alignment);
                decoder.setAlignmentType(alignment);
                encoder.setPreserveLexicalValues(preserveLexicalValues);
                decoder.setPreserveLexicalValues(preserveLexicalValues);
                encoder.setEXISchema(grammarCache);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                encoder.setOutputStream(baos);
                byte[] bts;
                encoder.encode(new InputSource(new StringReader(xmlString)));
                bts = baos.toByteArray();
                decoder.setEXISchema(grammarCache);
                ArrayList<Event> exiEventList = new ArrayList<Event>();
                SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
                decoder.setContentHandler(saxRecorder);
                decoder.setLexicalHandler(saxRecorder);
                decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
                Assert.assertEquals(10, exiEventList.size());
                Event saxEvent;
                saxEvent = exiEventList.get(0);
                Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
                Assert.assertEquals("urn:foo", saxEvent.namespace);
                Assert.assertEquals("", saxEvent.name);
                saxEvent = exiEventList.get(1);
                Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
                Assert.assertEquals("urn:foo", saxEvent.namespace);
                Assert.assertEquals("foo", saxEvent.name);
                saxEvent = exiEventList.get(2);
                Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
                Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", saxEvent.namespace);
                Assert.assertEquals("xsi", saxEvent.name);
                saxEvent = exiEventList.get(3);
                Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
                Assert.assertEquals("urn:foo", saxEvent.namespace);
                Assert.assertEquals("F", saxEvent.localName);
                Assert.assertEquals("F", saxEvent.name);
                saxEvent = exiEventList.get(4);
                Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
                Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", saxEvent.namespace);
                Assert.assertEquals("type", saxEvent.localName);
                Assert.assertEquals("xsi:type", saxEvent.name);
                Assert.assertEquals("F", saxEvent.stringValue);
                saxEvent = exiEventList.get(5);
                Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
                Assert.assertEquals("urn:foo", saxEvent.namespace);
                Assert.assertEquals("aA", saxEvent.localName);
                Assert.assertEquals("foo:aA", saxEvent.name);
                Assert.assertEquals("abc", saxEvent.stringValue);
                saxEvent = exiEventList.get(6);
                Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
                Assert.assertEquals("urn:foo", saxEvent.namespace);
                Assert.assertEquals("F", saxEvent.localName);
                Assert.assertEquals("F", saxEvent.name);
                saxEvent = exiEventList.get(7);
                Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
                Assert.assertEquals("xsi", saxEvent.name);
                saxEvent = exiEventList.get(8);
                Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
                Assert.assertEquals("foo", saxEvent.name);
                saxEvent = exiEventList.get(9);
                Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
                Assert.assertEquals("", saxEvent.name);
            }
        }
    }

    /**
   * Schema:
   * <xsd:element name="AB" type="xsd:anySimpleType"/>
   *
   * Instance:
   * <AB xmlns="urn:foo" xsi:type="xsd:string" foo:aA="abc">xyz</AB>
   */
    public void testUndeclaredAttrWildcardAnyOfElementTagGrammar_withNS() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
        final String xmlString = "<foo:AB xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n" + "  xmlns:xsd='http://www.w3.org/2001/XMLSchema' \n" + "  xmlns:foo='urn:foo' xsi:type='xsd:string' foo:aA='abc'>" + "xyz</foo:AB>";
        for (AlignmentType alignment : Alignments) {
            for (boolean preserveLexicalValues : new boolean[] { true, false }) {
                Transmogrifier encoder = new Transmogrifier();
                EXIReader decoder = new EXIReader();
                encoder.setAlignmentType(alignment);
                decoder.setAlignmentType(alignment);
                encoder.setPreserveLexicalValues(preserveLexicalValues);
                decoder.setPreserveLexicalValues(preserveLexicalValues);
                encoder.setEXISchema(grammarCache);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                encoder.setOutputStream(baos);
                byte[] bts;
                encoder.encode(new InputSource(new StringReader(xmlString)));
                bts = baos.toByteArray();
                decoder.setEXISchema(grammarCache);
                ArrayList<Event> exiEventList = new ArrayList<Event>();
                SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
                decoder.setContentHandler(saxRecorder);
                decoder.setLexicalHandler(saxRecorder);
                decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
                Assert.assertEquals(11, exiEventList.size());
                Event saxEvent;
                saxEvent = exiEventList.get(0);
                Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
                Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", saxEvent.namespace);
                Assert.assertEquals("xsi", saxEvent.name);
                saxEvent = exiEventList.get(1);
                Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
                Assert.assertEquals("http://www.w3.org/2001/XMLSchema", saxEvent.namespace);
                Assert.assertEquals("xsd", saxEvent.name);
                saxEvent = exiEventList.get(2);
                Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
                Assert.assertEquals("urn:foo", saxEvent.namespace);
                Assert.assertEquals("foo", saxEvent.name);
                saxEvent = exiEventList.get(3);
                Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
                Assert.assertEquals("urn:foo", saxEvent.namespace);
                Assert.assertEquals("AB", saxEvent.localName);
                Assert.assertEquals("foo:AB", saxEvent.name);
                saxEvent = exiEventList.get(4);
                Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
                Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", saxEvent.namespace);
                Assert.assertEquals("type", saxEvent.localName);
                Assert.assertEquals("xsi:type", saxEvent.name);
                Assert.assertEquals("xsd:string", saxEvent.stringValue);
                saxEvent = exiEventList.get(5);
                Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
                Assert.assertEquals("urn:foo", saxEvent.namespace);
                Assert.assertEquals("aA", saxEvent.localName);
                Assert.assertEquals("foo:aA", saxEvent.name);
                Assert.assertEquals("abc", saxEvent.stringValue);
                saxEvent = exiEventList.get(6);
                Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
                Assert.assertEquals("xyz", new String(saxEvent.charValue));
                saxEvent = exiEventList.get(7);
                Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
                Assert.assertEquals("urn:foo", saxEvent.namespace);
                Assert.assertEquals("AB", saxEvent.localName);
                Assert.assertEquals("foo:AB", saxEvent.name);
                saxEvent = exiEventList.get(8);
                Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
                Assert.assertEquals("foo", saxEvent.name);
                saxEvent = exiEventList.get(9);
                Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
                Assert.assertEquals("xsd", saxEvent.name);
                saxEvent = exiEventList.get(10);
                Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
                Assert.assertEquals("xsi", saxEvent.name);
            }
        }
    }

    /**
   * Schema:
   * <xsd:element name="AB" type="xsd:anySimpleType"/>
   *
   * Instance:
   * <AB xmlns="urn:foo" xsi:type="xsd:string" foo:aA="abc">xyz</AB>
   */
    public void testUndeclaredAttrWildcardAnyOfElementTagGrammar_withoutNS() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
        final String xmlString = "<foo:AB xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n" + "  xmlns:xsd='http://www.w3.org/2001/XMLSchema' \n" + "  xmlns:foo='urn:foo' xsi:type='xsd:string' foo:aA='abc'>" + "xyz</foo:AB>";
        for (AlignmentType alignment : Alignments) {
            for (boolean preserveLexicalValues : new boolean[] { true, false }) {
                Transmogrifier encoder = new Transmogrifier();
                EXIReader decoder = new EXIReader();
                encoder.setAlignmentType(alignment);
                decoder.setAlignmentType(alignment);
                encoder.setPreserveLexicalValues(preserveLexicalValues);
                decoder.setPreserveLexicalValues(preserveLexicalValues);
                encoder.setEXISchema(grammarCache);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                encoder.setOutputStream(baos);
                byte[] bts;
                encoder.encode(new InputSource(new StringReader(xmlString)));
                bts = baos.toByteArray();
                decoder.setEXISchema(grammarCache);
                ArrayList<Event> exiEventList = new ArrayList<Event>();
                SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
                decoder.setContentHandler(saxRecorder);
                decoder.setLexicalHandler(saxRecorder);
                decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
                Assert.assertEquals(preserveLexicalValues ? 9 : 11, exiEventList.size());
                Event saxEvent;
                int n = 0;
                saxEvent = exiEventList.get(n++);
                Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
                Assert.assertEquals("urn:foo", saxEvent.namespace);
                Assert.assertEquals("p0", saxEvent.name);
                saxEvent = exiEventList.get(n++);
                Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
                Assert.assertEquals(URIConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.namespace);
                Assert.assertEquals("p1", saxEvent.name);
                if (!preserveLexicalValues) {
                    saxEvent = exiEventList.get(n++);
                    Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
                    Assert.assertEquals("http://www.w3.org/2001/XMLSchema", saxEvent.namespace);
                    Assert.assertEquals("p2", saxEvent.name);
                }
                saxEvent = exiEventList.get(n++);
                Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
                Assert.assertEquals("urn:foo", saxEvent.namespace);
                Assert.assertEquals("AB", saxEvent.localName);
                Assert.assertEquals("p0:AB", saxEvent.name);
                saxEvent = exiEventList.get(n++);
                Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
                Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", saxEvent.namespace);
                Assert.assertEquals("type", saxEvent.localName);
                Assert.assertNull(saxEvent.name);
                Assert.assertEquals(preserveLexicalValues ? "xsd:string" : "p2:string", saxEvent.stringValue);
                saxEvent = exiEventList.get(n++);
                Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
                Assert.assertEquals("urn:foo", saxEvent.namespace);
                Assert.assertEquals("aA", saxEvent.localName);
                Assert.assertEquals("p0:aA", saxEvent.name);
                Assert.assertEquals("abc", saxEvent.stringValue);
                saxEvent = exiEventList.get(n++);
                Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
                Assert.assertEquals("xyz", new String(saxEvent.charValue));
                saxEvent = exiEventList.get(n++);
                Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
                Assert.assertEquals("urn:foo", saxEvent.namespace);
                Assert.assertEquals("AB", saxEvent.localName);
                Assert.assertEquals("p0:AB", saxEvent.name);
                if (!preserveLexicalValues) {
                    saxEvent = exiEventList.get(n++);
                    Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
                    Assert.assertEquals("p2", saxEvent.name);
                }
                saxEvent = exiEventList.get(n++);
                Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
                Assert.assertEquals("p1", saxEvent.name);
                saxEvent = exiEventList.get(n++);
                Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
                Assert.assertEquals("p0", saxEvent.name);
            }
        }
    }

    /**
   * Schema: 
   * <xsd:complexType name="restricted_B">
   *   <xsd:complexContent>
   *     <xsd:restriction base="foo:B">
   *       <xsd:sequence>
   *         <xsd:element ref="foo:AB"/>
   *         <xsd:element ref="foo:AC" minOccurs="0"/>
   *         <xsd:element ref="foo:AD" minOccurs="0"/>
   *       </xsd:sequence>
   *     </xsd:restriction>
   *   </xsd:complexContent>
   * </xsd:complexType>
   *
   * <xsd:element name="nillable_B" type="foo:B" nillable="true" />
   * 
   * Instance:
   * <nillable_B xmlns='urn:foo' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'/>
   */
    public void testAcceptanceForNillableB() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
        final String xmlString = "<foo:nillable_B xmlns:foo='urn:foo' xsi:nil='  true   ' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'/>";
        for (AlignmentType alignment : Alignments) {
            for (boolean preserveLexicalValues : new boolean[] { true, false }) {
                Transmogrifier encoder = new Transmogrifier();
                EXIReader decoder = new EXIReader();
                encoder.setAlignmentType(alignment);
                decoder.setAlignmentType(alignment);
                encoder.setPreserveLexicalValues(preserveLexicalValues);
                decoder.setPreserveLexicalValues(preserveLexicalValues);
                encoder.setEXISchema(grammarCache);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                encoder.setOutputStream(baos);
                byte[] bts;
                encoder.encode(new InputSource(new StringReader(xmlString)));
                bts = baos.toByteArray();
                decoder.setEXISchema(grammarCache);
                ArrayList<Event> exiEventList = new ArrayList<Event>();
                SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
                decoder.setContentHandler(saxRecorder);
                decoder.setLexicalHandler(saxRecorder);
                decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
                Assert.assertEquals(7, exiEventList.size());
                Event saxEvent;
                saxEvent = exiEventList.get(0);
                Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
                Assert.assertEquals("urn:foo", saxEvent.namespace);
                Assert.assertEquals("foo", saxEvent.name);
                saxEvent = exiEventList.get(1);
                Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
                Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", saxEvent.namespace);
                Assert.assertEquals("xsi", saxEvent.name);
                saxEvent = exiEventList.get(2);
                Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
                Assert.assertEquals("urn:foo", saxEvent.namespace);
                Assert.assertEquals("nillable_B", saxEvent.localName);
                Assert.assertEquals("foo:nillable_B", saxEvent.name);
                saxEvent = exiEventList.get(3);
                Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
                Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", saxEvent.namespace);
                Assert.assertEquals("nil", saxEvent.localName);
                Assert.assertEquals("xsi:nil", saxEvent.name);
                Assert.assertEquals(preserveLexicalValues ? "  true   " : "true", saxEvent.stringValue);
                saxEvent = exiEventList.get(4);
                Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
                Assert.assertEquals("urn:foo", saxEvent.namespace);
                Assert.assertEquals("nillable_B", saxEvent.localName);
                Assert.assertEquals("foo:nillable_B", saxEvent.name);
                saxEvent = exiEventList.get(5);
                Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
                Assert.assertEquals("xsi", saxEvent.name);
                saxEvent = exiEventList.get(6);
                Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
                Assert.assertEquals("foo", saxEvent.name);
            }
        }
    }

    /**
   * Exercise CM and PI in "all" group.
   * 
   * Schema:
   * <xsd:element name="C">
   *   <xsd:complexType>
   *     <xsd:all>
   *       <xsd:element ref="foo:AB" minOccurs="0" />
   *       <xsd:element ref="foo:AC" minOccurs="0" />
   *     </xsd:all>
   *   </xsd:complexType>
   * </xsd:element>
   *
   * Instance:
   * <C><AC/><!-- Good? --><?eg Good! ?></C><?eg Good? ?><!-- Good! -->
   */
    public void testCommentPI_01() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        short options = GrammarOptions.DEFAULT_OPTIONS;
        options = GrammarOptions.addCM(options);
        options = GrammarOptions.addPI(options);
        GrammarCache grammarCache = new GrammarCache(corpus, options);
        final String xmlString = "<C xmlns='urn:foo'><AC/><!-- Good? --><?eg Good! ?></C><?eg Good? ?><!-- Good! -->";
        for (AlignmentType alignment : Alignments) {
            Transmogrifier encoder = new Transmogrifier();
            EXIReader decoder = new EXIReader();
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            encoder.setEXISchema(grammarCache);
            decoder.setEXISchema(grammarCache);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.setOutputStream(baos);
            encoder.encode(new InputSource(new StringReader(xmlString)));
            byte[] bts = baos.toByteArray();
            ArrayList<Event> exiEventList = new ArrayList<Event>();
            SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
            decoder.setContentHandler(saxRecorder);
            decoder.setLexicalHandler(saxRecorder);
            decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
            Assert.assertEquals(10, exiEventList.size());
            Event saxEvent;
            saxEvent = exiEventList.get(0);
            Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
            Assert.assertEquals("urn:foo", saxEvent.namespace);
            Assert.assertEquals("p0", saxEvent.name);
            saxEvent = exiEventList.get(1);
            Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
            Assert.assertEquals("urn:foo", saxEvent.namespace);
            Assert.assertEquals("C", saxEvent.localName);
            Assert.assertEquals("p0:C", saxEvent.name);
            saxEvent = exiEventList.get(2);
            Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
            Assert.assertEquals("urn:foo", saxEvent.namespace);
            Assert.assertEquals("AC", saxEvent.localName);
            Assert.assertEquals("p0:AC", saxEvent.name);
            saxEvent = exiEventList.get(3);
            Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
            Assert.assertEquals("urn:foo", saxEvent.namespace);
            Assert.assertEquals("AC", saxEvent.localName);
            Assert.assertEquals("p0:AC", saxEvent.name);
            saxEvent = exiEventList.get(4);
            Assert.assertEquals(Event.COMMENT, saxEvent.type);
            Assert.assertEquals(" Good? ", new String(saxEvent.charValue));
            saxEvent = exiEventList.get(5);
            Assert.assertEquals(Event.PROCESSING_INSTRUCTION, saxEvent.type);
            Assert.assertEquals("eg", saxEvent.name);
            Assert.assertEquals("Good! ", saxEvent.stringValue);
            saxEvent = exiEventList.get(6);
            Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
            Assert.assertEquals("urn:foo", saxEvent.namespace);
            Assert.assertEquals("C", saxEvent.localName);
            Assert.assertEquals("p0:C", saxEvent.name);
            saxEvent = exiEventList.get(7);
            Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
            Assert.assertEquals("p0", saxEvent.name);
            saxEvent = exiEventList.get(8);
            Assert.assertEquals(Event.PROCESSING_INSTRUCTION, saxEvent.type);
            Assert.assertEquals("eg", saxEvent.name);
            Assert.assertEquals("Good? ", saxEvent.stringValue);
            saxEvent = exiEventList.get(9);
            Assert.assertEquals(Event.COMMENT, saxEvent.type);
            Assert.assertEquals(" Good! ", new String(saxEvent.charValue));
        }
    }

    /**
   * Schema:
   * None available
   * 
   * Instance:
   * <None>&abc;&def;</None>
   */
    public void testBuiltinEntityRef() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addDTD(GrammarOptions.DEFAULT_OPTIONS));
        final String xmlString;
        byte[] bts;
        xmlString = "<!DOCTYPE None [ <!ENTITY ent SYSTEM 'er-entity.xml'> ]><None xmlns='urn:foo'>&ent;&ent;</None>\n";
        for (AlignmentType alignment : Alignments) {
            Transmogrifier encoder = new Transmogrifier();
            encoder.setResolveExternalGeneralEntities(false);
            EXIReader decoder = new EXIReader();
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            encoder.setEXISchema(grammarCache);
            decoder.setEXISchema(grammarCache);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.setOutputStream(baos);
            encoder.encode(new InputSource(new StringReader(xmlString)));
            bts = baos.toByteArray();
            ArrayList<Event> exiEventList = new ArrayList<Event>();
            SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
            decoder.setContentHandler(saxRecorder);
            decoder.setLexicalHandler(saxRecorder);
            decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
            Assert.assertEquals(8, exiEventList.size());
            Event saxEvent;
            saxEvent = exiEventList.get(0);
            Assert.assertEquals(Event.DOCTYPE, saxEvent.type);
            Assert.assertEquals("None", saxEvent.name);
            saxEvent = exiEventList.get(1);
            Assert.assertEquals(Event.END_DTD, saxEvent.type);
            saxEvent = exiEventList.get(2);
            Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
            Assert.assertEquals("urn:foo", saxEvent.namespace);
            Assert.assertEquals("p0", saxEvent.name);
            saxEvent = exiEventList.get(3);
            Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
            Assert.assertEquals("urn:foo", saxEvent.namespace);
            Assert.assertEquals("None", saxEvent.localName);
            Assert.assertEquals("p0:None", saxEvent.name);
            saxEvent = exiEventList.get(4);
            Assert.assertEquals(Event.UNEXPANDED_ENTITY, saxEvent.type);
            Assert.assertEquals("ent", saxEvent.name);
            saxEvent = exiEventList.get(5);
            Assert.assertEquals(Event.UNEXPANDED_ENTITY, saxEvent.type);
            Assert.assertEquals("ent", saxEvent.name);
            saxEvent = exiEventList.get(6);
            Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
            Assert.assertEquals("urn:foo", saxEvent.namespace);
            Assert.assertEquals("None", saxEvent.localName);
            Assert.assertEquals("p0:None", saxEvent.name);
            saxEvent = exiEventList.get(7);
            Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
            Assert.assertEquals("p0", saxEvent.name);
        }
    }

    /**
   */
    public void testBlockSize_01() throws Exception {
        GrammarCache grammarCache = new GrammarCache((EXISchema) null, GrammarOptions.DEFAULT_OPTIONS);
        for (AlignmentType alignment : new AlignmentType[] { AlignmentType.preCompress, AlignmentType.compress }) {
            Transmogrifier encoder = new Transmogrifier();
            EXIReader decoder = new EXIReader();
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            encoder.setBlockSize(1);
            encoder.setEXISchema(grammarCache);
            decoder.setEXISchema(grammarCache);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.setOutputStream(baos);
            URL url = resolveSystemIdAsURL("/interop/datatypes/string/indexed-10.xml");
            InputSource inputSource = new InputSource(url.toString());
            inputSource.setByteStream(url.openStream());
            encoder.encode(inputSource);
            byte[] bts = baos.toByteArray();
            ArrayList<Event> exiEventList = new ArrayList<Event>();
            SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
            decoder.setContentHandler(saxRecorder);
            try {
                decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
            } catch (Exception e) {
                continue;
            }
            Assert.fail();
        }
        for (AlignmentType alignment : new AlignmentType[] { AlignmentType.preCompress, AlignmentType.compress }) {
            Transmogrifier encoder = new Transmogrifier();
            EXIReader decoder = new EXIReader();
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            encoder.setBlockSize(1);
            decoder.setBlockSize(1);
            encoder.setEXISchema(grammarCache);
            decoder.setEXISchema(grammarCache);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.setOutputStream(baos);
            URL url = resolveSystemIdAsURL("/interop/datatypes/string/indexed-10.xml");
            InputSource inputSource = new InputSource(url.toString());
            inputSource.setByteStream(url.openStream());
            encoder.encode(inputSource);
            byte[] bts = baos.toByteArray();
            ArrayList<Event> exiEventList = new ArrayList<Event>();
            SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
            decoder.setContentHandler(saxRecorder);
            decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
            Assert.assertEquals(302, exiEventList.size());
            Event saxEvent;
            int n = 0;
            saxEvent = exiEventList.get(n++);
            Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
            Assert.assertEquals("", saxEvent.namespace);
            Assert.assertEquals("root", saxEvent.localName);
            Assert.assertEquals("root", saxEvent.name);
            for (int i = 0; i < 100; i++) {
                saxEvent = exiEventList.get(n++);
                Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
                Assert.assertEquals("", saxEvent.namespace);
                Assert.assertEquals("a", saxEvent.localName);
                Assert.assertEquals("a", saxEvent.name);
                saxEvent = exiEventList.get(n++);
                Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
                Assert.assertEquals(String.format("test%1$02d", i), new String(saxEvent.charValue));
                saxEvent = exiEventList.get(n++);
                Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
                Assert.assertEquals("", saxEvent.namespace);
                Assert.assertEquals("a", saxEvent.localName);
                Assert.assertEquals("a", saxEvent.name);
            }
            saxEvent = exiEventList.get(n++);
            Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
            Assert.assertEquals("", saxEvent.namespace);
            Assert.assertEquals("root", saxEvent.localName);
            Assert.assertEquals("root", saxEvent.name);
        }
    }

    /**
   * Use JAXP transformer to serialize SAX events into text XML.
   */
    public void testXMLSerialization_01() throws Exception {
        GrammarCache grammarCache = new GrammarCache(EmptySchema.getEXISchema(), GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS));
        final String xmlString = "<foo:A xmlns:foo='urn:foo' xmlns:goo='urn:goo' goo:a='Good Bye!'>Hello <!-- evil --> World!</foo:A>\n";
        SAXTransformerFactory saxTransformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        for (AlignmentType alignment : Alignments) {
            Transmogrifier encoder = new Transmogrifier();
            EXIReader decoder = new EXIReader();
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            encoder.setEXISchema(grammarCache);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.setOutputStream(baos);
            encoder.encode(new InputSource(new StringReader(xmlString)));
            byte[] bts = baos.toByteArray();
            decoder.setEXISchema(grammarCache);
            TransformerHandler transformerHandler = saxTransformerFactory.newTransformerHandler();
            StringWriter stringWriter = new StringWriter();
            transformerHandler.setResult(new StreamResult(stringWriter));
            decoder.setContentHandler(transformerHandler);
            decoder.setLexicalHandler(transformerHandler);
            decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
            final String reconstitutedString;
            reconstitutedString = stringWriter.getBuffer().toString();
            ArrayList<Event> exiEventList = new ArrayList<Event>();
            SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
            XMLReader xmlReader = saxParserFactory.newSAXParser().getXMLReader();
            xmlReader.setContentHandler(saxRecorder);
            xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", saxRecorder);
            xmlReader.parse(new InputSource(new StringReader(reconstitutedString)));
            Assert.assertEquals(10, exiEventList.size());
            Event saxEvent;
            saxEvent = exiEventList.get(0);
            Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
            Assert.assertEquals("urn:foo", saxEvent.namespace);
            Assert.assertEquals("p0", saxEvent.name);
            saxEvent = exiEventList.get(1);
            Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
            Assert.assertEquals("urn:goo", saxEvent.namespace);
            Assert.assertEquals("p1", saxEvent.name);
            saxEvent = exiEventList.get(2);
            Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
            Assert.assertEquals("urn:foo", saxEvent.namespace);
            Assert.assertEquals("A", saxEvent.localName);
            Assert.assertEquals("p0:A", saxEvent.name);
            saxEvent = exiEventList.get(3);
            Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
            Assert.assertEquals("urn:goo", saxEvent.namespace);
            Assert.assertEquals("a", saxEvent.localName);
            Assert.assertEquals("p1:a", saxEvent.name);
            Assert.assertEquals("Good Bye!", saxEvent.stringValue);
            saxEvent = exiEventList.get(4);
            Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
            Assert.assertEquals("Hello ", new String(saxEvent.charValue));
            saxEvent = exiEventList.get(5);
            Assert.assertEquals(Event.COMMENT, saxEvent.type);
            Assert.assertEquals(" evil ", new String(saxEvent.charValue));
            saxEvent = exiEventList.get(6);
            Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
            Assert.assertEquals(" World!", new String(saxEvent.charValue));
            saxEvent = exiEventList.get(7);
            Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
            Assert.assertEquals("urn:foo", saxEvent.namespace);
            Assert.assertEquals("A", saxEvent.localName);
            Assert.assertEquals("p0:A", saxEvent.name);
            saxEvent = exiEventList.get(8);
            Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
            Assert.assertEquals("p0", saxEvent.name);
            saxEvent = exiEventList.get(9);
            Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
            Assert.assertEquals("p1", saxEvent.name);
        }
    }
}
