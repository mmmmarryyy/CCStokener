package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import junit.framework.Assert;
import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIEvent;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.EXISchema;
import org.openexi.schema.TestBase;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

public class GrammarStrictAllTest extends TestBase {

    public GrammarStrictAllTest(String name) {
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
   * <C>
   *   <AB/><AC/>
   * </C>
   */
    public void testAcceptanceForC_01() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
        final String xmlString = "<C xmlns='urn:foo'>\n" + "  <AB/><AC/>\n" + "</C>\n";
        for (AlignmentType alignment : Alignments) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            encoder.setEXISchema(grammarCache);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.setOutputStream(baos);
            encoder.encode(new InputSource(new StringReader(xmlString)));
            byte[] bts = baos.toByteArray();
            decoder.setEXISchema(grammarCache);
            decoder.setInputStream(new ByteArrayInputStream(bts));
            scanner = decoder.processHeader();
            ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
            EXIEvent exiEvent;
            int n_events = 0;
            while ((exiEvent = scanner.nextEvent()) != null) {
                ++n_events;
                exiEventList.add(exiEvent);
            }
            Assert.assertEquals(10, n_events);
            EventType eventType;
            EventTypeList eventTypeList;
            exiEvent = exiEventList.get(0);
            Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertSame(exiEvent, eventType);
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertNull(eventTypeList.getEE());
            exiEvent = exiEventList.get(1);
            Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
            Assert.assertEquals("C", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertNull(eventTypeList.getEE());
            exiEvent = exiEventList.get(2);
            Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
            Assert.assertEquals("AB", exiEvent.getName());
            Assert.assertEquals("urn:foo", exiEvent.getURI());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
            Assert.assertEquals("AB", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(3, eventTypeList.getLength());
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
            Assert.assertEquals("AC", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            exiEvent = exiEventList.get(3);
            Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
            Assert.assertEquals("", exiEvent.getCharacters().makeString());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
            Assert.assertEquals(1, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertNull(eventTypeList.getEE());
            Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
            exiEvent = exiEventList.get(4);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(1, eventTypeList.getLength());
            exiEvent = exiEventList.get(5);
            Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
            Assert.assertEquals("AC", exiEvent.getName());
            Assert.assertEquals("urn:foo", exiEvent.getURI());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
            Assert.assertEquals("AC", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals(1, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(3, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
            Assert.assertEquals("AB", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            exiEvent = exiEventList.get(6);
            Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
            Assert.assertEquals("", exiEvent.getCharacters().makeString());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
            Assert.assertEquals(1, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertNull(eventTypeList.getEE());
            Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
            exiEvent = exiEventList.get(7);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(1, eventTypeList.getLength());
            exiEvent = exiEventList.get(8);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            Assert.assertEquals(2, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(3, eventTypeList.getLength());
            Assert.assertNotNull(eventTypeList.getEE());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
            Assert.assertEquals("AB", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
            Assert.assertEquals("AC", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            exiEvent = exiEventList.get(9);
            Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertSame(exiEvent, eventType);
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertNull(eventTypeList.getEE());
        }
    }

    /**
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
   * <C>
   *   <AC/><AB/><!-- reverse order -->  
   * </C>
   * where C has "all" group that consists of AC and AB.
   */
    public void testAcceptanceForC_02() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
        final String xmlString = "<C xmlns='urn:foo'>\n" + "  <AC/><AB/>\n" + "</C>\n";
        for (AlignmentType alignment : Alignments) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            encoder.setEXISchema(grammarCache);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.setOutputStream(baos);
            encoder.encode(new InputSource(new StringReader(xmlString)));
            byte[] bts = baos.toByteArray();
            decoder.setEXISchema(grammarCache);
            decoder.setInputStream(new ByteArrayInputStream(bts));
            scanner = decoder.processHeader();
            ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
            EXIEvent exiEvent;
            int n_events = 0;
            while ((exiEvent = scanner.nextEvent()) != null) {
                ++n_events;
                exiEventList.add(exiEvent);
            }
            Assert.assertEquals(10, n_events);
            EventType eventType;
            EventTypeList eventTypeList;
            exiEvent = exiEventList.get(0);
            Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertSame(exiEvent, eventType);
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertNull(eventTypeList.getEE());
            exiEvent = exiEventList.get(1);
            Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
            Assert.assertEquals("C", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertNull(eventTypeList.getEE());
            exiEvent = exiEventList.get(2);
            Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
            Assert.assertEquals("AC", exiEvent.getName());
            Assert.assertEquals("urn:foo", exiEvent.getURI());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
            Assert.assertEquals("AC", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals(1, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(3, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
            Assert.assertEquals("AB", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            exiEvent = exiEventList.get(3);
            Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
            Assert.assertEquals("", exiEvent.getCharacters().makeString());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
            Assert.assertEquals(1, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertNull(eventTypeList.getEE());
            Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
            exiEvent = exiEventList.get(4);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(1, eventTypeList.getLength());
            exiEvent = exiEventList.get(5);
            Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
            Assert.assertEquals("AB", exiEvent.getName());
            Assert.assertEquals("urn:foo", exiEvent.getURI());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
            Assert.assertEquals("AB", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(3, eventTypeList.getLength());
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
            Assert.assertEquals("AC", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            exiEvent = exiEventList.get(6);
            Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
            Assert.assertEquals("", exiEvent.getCharacters().makeString());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
            Assert.assertEquals(1, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertNull(eventTypeList.getEE());
            Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
            exiEvent = exiEventList.get(7);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(1, eventTypeList.getLength());
            exiEvent = exiEventList.get(8);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            Assert.assertEquals(2, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(3, eventTypeList.getLength());
            Assert.assertNotNull(eventTypeList.getEE());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
            Assert.assertEquals("AB", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
            Assert.assertEquals("AC", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            exiEvent = exiEventList.get(9);
            Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertSame(exiEvent, eventType);
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertNull(eventTypeList.getEE());
        }
    }

    /**
   * Schema:
   * <xsd:complexType name="C">
   *   <xsd:all>
   *     <xsd:element ref="foo:AB" minOccurs="0" />
   *     <xsd:element ref="foo:AC" />
   *   </xsd:all>
   * </xsd:complexType>
   * 
   * <xsd:element name="C" type="foo:C"/>
   *
   * <C>
   *   <AC/><AB/><AC/>  
   * </C>
   */
    public void testDecodeAll_03() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/interop/schemaInformedGrammar/acceptance.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        decoder.setAlignmentType(AlignmentType.bitPacked);
        URL url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/all-03.bitPacked");
        decoder.setEXISchema(grammarCache);
        decoder.setInputStream(url.openStream());
        scanner = decoder.processHeader();
        ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
        EXIEvent exiEvent;
        int n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            exiEventList.add(exiEvent);
        }
        Assert.assertEquals(13, n_events);
        EventType eventType;
        exiEvent = exiEventList.get(0);
        Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        exiEvent = exiEventList.get(1);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("C", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        exiEvent = exiEventList.get(2);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("AC", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AC", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        exiEvent = exiEventList.get(3);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals("", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        exiEvent = exiEventList.get(4);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        exiEvent = exiEventList.get(5);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("AB", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AB", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        exiEvent = exiEventList.get(6);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals("", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        exiEvent = exiEventList.get(7);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        exiEvent = exiEventList.get(8);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("AC", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AC", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        exiEvent = exiEventList.get(9);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals("", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        exiEvent = exiEventList.get(10);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        exiEvent = exiEventList.get(11);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        exiEvent = exiEventList.get(12);
        Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
    }
}
