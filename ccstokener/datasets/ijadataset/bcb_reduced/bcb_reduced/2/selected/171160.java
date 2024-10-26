package org.openexi.fujitsu.sax;

import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;
import org.xml.sax.InputSource;
import junit.framework.Assert;
import org.openexi.fujitsu.proc.EXIDecoder;
import org.openexi.fujitsu.proc.HeaderOptionsOutputType;
import org.openexi.fujitsu.proc.common.AlignmentType;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.proc.common.EXIEvent;
import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.proc.grammars.GrammarCache;
import org.openexi.fujitsu.proc.io.Scanner;
import org.openexi.fujitsu.sax.Transmogrifier;
import org.openexi.fujitsu.sax.TransmogrifierException;
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.TestBase;
import org.openexi.fujitsu.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.fujitsu.scomp.EXISchemaFactoryTestUtil;

public class BooleanValueEncodingTest extends TestBase {

    public BooleanValueEncodingTest(String name) {
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
   * A valid boolean value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:boolean.
   */
    public void testValidBoolean_01() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
        final String[] xmlStrings;
        final String[] values = { " \t\r  true\n", "false", " \n 1  \t\r", "0" };
        final String[] resultValues = { "true", "false", "true", "false" };
        int i;
        xmlStrings = new String[values.length];
        for (i = 0; i < values.length; i++) {
            xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
        }
        ;
        for (AlignmentType alignment : Alignments) {
            for (i = 0; i < xmlStrings.length; i++) {
                Transmogrifier encoder = new Transmogrifier();
                EXIDecoder decoder = new EXIDecoder();
                Scanner scanner;
                encoder.setAlignmentType(alignment);
                decoder.setAlignmentType(alignment);
                encoder.setEXISchema(grammarCache);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                encoder.setOutputStream(baos);
                byte[] bts;
                int n_events;
                encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
                bts = baos.toByteArray();
                decoder.setEXISchema(grammarCache);
                decoder.setInputStream(new ByteArrayInputStream(bts));
                scanner = decoder.processHeader();
                ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
                EXIEvent exiEvent;
                n_events = 0;
                while ((exiEvent = scanner.nextEvent()) != null) {
                    ++n_events;
                    exiEventList.add(exiEvent);
                }
                Assert.assertEquals(5, n_events);
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
                Assert.assertEquals("A", eventType.getName());
                Assert.assertEquals("urn:foo", eventType.getURI());
                eventTypeList = eventType.getEventTypeList();
                Assert.assertNull(eventTypeList.getEE());
                exiEvent = exiEventList.get(2);
                Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
                Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
                eventType = exiEvent.getEventType();
                Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
                Assert.assertEquals(2, eventType.getIndex());
                eventTypeList = eventType.getEventTypeList();
                Assert.assertEquals(8, eventTypeList.getLength());
                eventType = eventTypeList.item(0);
                Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
                eventType = eventTypeList.item(1);
                Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
                eventType = eventTypeList.item(3);
                Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
                Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
                eventType = eventTypeList.item(4);
                Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
                Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
                eventType = eventTypeList.item(5);
                Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
                Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
                eventType = eventTypeList.item(6);
                Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
                eventType = eventTypeList.item(7);
                Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
                exiEvent = exiEventList.get(3);
                Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
                eventType = exiEvent.getEventType();
                Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
                Assert.assertEquals(0, eventType.getIndex());
                eventTypeList = eventType.getEventTypeList();
                Assert.assertEquals(3, eventTypeList.getLength());
                eventType = eventTypeList.item(1);
                Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
                eventType = eventTypeList.item(2);
                Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
                exiEvent = exiEventList.get(4);
                Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
                eventType = exiEvent.getEventType();
                Assert.assertSame(exiEvent, eventType);
                Assert.assertEquals(0, eventType.getIndex());
                eventTypeList = eventType.getEventTypeList();
                Assert.assertEquals(1, eventTypeList.getLength());
            }
        }
    }

    /**
   * An invalid boolean value matching ITEM_CH instead of ITEM_SCHEMA_CH.
   */
    public void testInvalidBoolean_01() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
        final String xmlString = "<foo:A xmlns:foo='urn:foo'>tree</foo:A>\n";
        for (AlignmentType alignment : Alignments) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            encoder.setEXISchema(grammarCache);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.setOutputStream(baos);
            byte[] bts;
            int n_events;
            encoder.encode(new InputSource(new StringReader(xmlString)));
            bts = baos.toByteArray();
            decoder.setEXISchema(grammarCache);
            decoder.setInputStream(new ByteArrayInputStream(bts));
            scanner = decoder.processHeader();
            ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
            EXIEvent exiEvent;
            n_events = 0;
            while ((exiEvent = scanner.nextEvent()) != null) {
                ++n_events;
                exiEventList.add(exiEvent);
            }
            Assert.assertEquals(5, n_events);
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
            Assert.assertEquals("A", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertNull(eventTypeList.getEE());
            exiEvent = exiEventList.get(2);
            Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
            Assert.assertEquals("tree", exiEvent.getCharacters().makeString());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            Assert.assertEquals(7, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(8, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(4);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(5);
            Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
            eventType = eventTypeList.item(6);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(7);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            exiEvent = exiEventList.get(3);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            Assert.assertEquals(1, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(4, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            exiEvent = exiEventList.get(4);
            Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertSame(exiEvent, eventType);
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(1, eventTypeList.getLength());
        }
    }

    /**
   * An attribute with a valid boolean value matching ITEM_SCHEMA_AT where 
   * the associated datatype is xsd:boolean.
   */
    public void testValidBoolean_02() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
        final String xmlString = "<foo:B xmlns:foo='urn:foo' foo:aA='false' />\n";
        for (AlignmentType alignment : Alignments) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            encoder.setEXISchema(grammarCache);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.setOutputStream(baos);
            byte[] bts;
            int n_events;
            encoder.encode(new InputSource(new StringReader(xmlString)));
            bts = baos.toByteArray();
            decoder.setEXISchema(grammarCache);
            decoder.setInputStream(new ByteArrayInputStream(bts));
            scanner = decoder.processHeader();
            ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
            EXIEvent exiEvent;
            n_events = 0;
            while ((exiEvent = scanner.nextEvent()) != null) {
                ++n_events;
                exiEventList.add(exiEvent);
            }
            Assert.assertEquals(5, n_events);
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
            Assert.assertEquals("B", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertNull(eventTypeList.getEE());
            exiEvent = exiEventList.get(2);
            Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
            Assert.assertEquals("false", exiEvent.getCharacters().makeString());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
            Assert.assertEquals(2, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(10, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventType = eventTypeList.item(4);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(6);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            eventType = eventTypeList.item(7);
            Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
            eventType = eventTypeList.item(8);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(9);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            exiEvent = exiEventList.get(3);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            Assert.assertEquals(1, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(6, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
            eventType = eventTypeList.item(4);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            exiEvent = exiEventList.get(4);
            Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertSame(exiEvent, eventType);
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(1, eventTypeList.getLength());
        }
    }

    /**
   * An attribute with an invalid boolean value matching 
   * ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE instead of ITEM_SCHEMA_AT.
   */
    public void testInvalidBoolean_02() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
        final String xmlString = "<foo:B xmlns:foo='urn:foo' foo:aA='faith' />\n";
        for (AlignmentType alignment : Alignments) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            encoder.setEXISchema(grammarCache);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.setOutputStream(baos);
            byte[] bts;
            int n_events;
            encoder.encode(new InputSource(new StringReader(xmlString)));
            bts = baos.toByteArray();
            decoder.setEXISchema(grammarCache);
            decoder.setInputStream(new ByteArrayInputStream(bts));
            scanner = decoder.processHeader();
            ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
            EXIEvent exiEvent;
            n_events = 0;
            while ((exiEvent = scanner.nextEvent()) != null) {
                ++n_events;
                exiEventList.add(exiEvent);
            }
            Assert.assertEquals(5, n_events);
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
            Assert.assertEquals("B", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertNull(eventTypeList.getEE());
            exiEvent = exiEventList.get(2);
            Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
            Assert.assertEquals("faith", exiEvent.getCharacters().makeString());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            Assert.assertEquals(6, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(10, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventType = eventTypeList.item(4);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(7);
            Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
            eventType = eventTypeList.item(8);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(9);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            exiEvent = exiEventList.get(3);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            Assert.assertEquals(1, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(6, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
            eventType = eventTypeList.item(4);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            exiEvent = exiEventList.get(4);
            Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertSame(exiEvent, eventType);
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(1, eventTypeList.getLength());
        }
    }

    /**
   * An attribute with a valid boolean value matching ITEM_SCHEMA_AT_WC_NS 
   * where there is a global attribute declaration given for the attribute with
   * datatype xsd:boolean. 
   */
    public void testValidBoolean_03() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
        final String xmlString = "<foo:B xmlns:foo='urn:foo' foo:aB='true' />\n";
        for (AlignmentType alignment : Alignments) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            encoder.setEXISchema(grammarCache);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.setOutputStream(baos);
            byte[] bts;
            int n_events;
            encoder.encode(new InputSource(new StringReader(xmlString)));
            bts = baos.toByteArray();
            decoder.setEXISchema(grammarCache);
            decoder.setInputStream(new ByteArrayInputStream(bts));
            scanner = decoder.processHeader();
            ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
            EXIEvent exiEvent;
            n_events = 0;
            while ((exiEvent = scanner.nextEvent()) != null) {
                ++n_events;
                exiEventList.add(exiEvent);
            }
            Assert.assertEquals(5, n_events);
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
            Assert.assertEquals("B", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertNull(eventTypeList.getEE());
            exiEvent = exiEventList.get(2);
            Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
            Assert.assertEquals("true", exiEvent.getCharacters().makeString());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals(3, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(10, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            eventType = eventTypeList.item(4);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(6);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            eventType = eventTypeList.item(7);
            Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
            eventType = eventTypeList.item(8);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(9);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            exiEvent = exiEventList.get(3);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            Assert.assertEquals(4, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(10, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventType = eventTypeList.item(5);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(6);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            eventType = eventTypeList.item(7);
            Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
            eventType = eventTypeList.item(8);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(9);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            exiEvent = exiEventList.get(4);
            Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertSame(exiEvent, eventType);
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(1, eventTypeList.getLength());
        }
    }

    /**
   * An attribute with an invalid boolean value matching ITEM_AT_WC_ANY 
   * where there is a global attribute declaration given for the attribute with
   * datatype xsd:boolean. 
   */
    public void testInvalidBoolean_03() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
        final String xmlString = "<foo:B xmlns:foo='urn:foo' foo:aB='tree' />\n";
        for (AlignmentType alignment : Alignments) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            encoder.setEXISchema(grammarCache);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.setOutputStream(baos);
            byte[] bts;
            int n_events;
            encoder.encode(new InputSource(new StringReader(xmlString)));
            bts = baos.toByteArray();
            decoder.setEXISchema(grammarCache);
            decoder.setInputStream(new ByteArrayInputStream(bts));
            scanner = decoder.processHeader();
            ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
            EXIEvent exiEvent;
            n_events = 0;
            while ((exiEvent = scanner.nextEvent()) != null) {
                ++n_events;
                exiEventList.add(exiEvent);
            }
            Assert.assertEquals(5, n_events);
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
            Assert.assertEquals("B", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertNull(eventTypeList.getEE());
            exiEvent = exiEventList.get(2);
            Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
            Assert.assertEquals("tree", exiEvent.getCharacters().makeString());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
            Assert.assertEquals(7, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(10, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventType = eventTypeList.item(4);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(6);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            eventType = eventTypeList.item(7);
            Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
            eventType = eventTypeList.item(8);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(9);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            exiEvent = exiEventList.get(3);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            Assert.assertEquals(4, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(10, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventType = eventTypeList.item(5);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(6);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            eventType = eventTypeList.item(7);
            Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
            eventType = eventTypeList.item(8);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(9);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            exiEvent = exiEventList.get(4);
            Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertSame(exiEvent, eventType);
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(1, eventTypeList.getLength());
        }
    }

    /**
   * An attribute with a valid boolean value matching undeclared ITEM_SCHEMA_AT_WC_ANY 
   * where there is a global attribute declaration given for the attribute with
   * datatype xsd:boolean. 
   */
    public void testValidBoolean_04() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
        final String xmlString = "<foo:B xmlns:foo='urn:foo' xmlns:goo='urn:goo' goo:aX='true' />\n";
        for (AlignmentType alignment : Alignments) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            encoder.setEXISchema(grammarCache);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.setOutputStream(baos);
            byte[] bts;
            int n_events;
            encoder.encode(new InputSource(new StringReader(xmlString)));
            bts = baos.toByteArray();
            decoder.setEXISchema(grammarCache);
            decoder.setInputStream(new ByteArrayInputStream(bts));
            scanner = decoder.processHeader();
            ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
            EXIEvent exiEvent;
            n_events = 0;
            while ((exiEvent = scanner.nextEvent()) != null) {
                ++n_events;
                exiEventList.add(exiEvent);
            }
            Assert.assertEquals(5, n_events);
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
            Assert.assertEquals("B", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertNull(eventTypeList.getEE());
            exiEvent = exiEventList.get(2);
            Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
            Assert.assertEquals("true", exiEvent.getCharacters().makeString());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            Assert.assertEquals(5, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(10, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventType = eventTypeList.item(4);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            eventType = eventTypeList.item(6);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            eventType = eventTypeList.item(7);
            Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
            eventType = eventTypeList.item(8);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(9);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            exiEvent = exiEventList.get(3);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            Assert.assertEquals(4, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(10, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventType = eventTypeList.item(5);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(6);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            eventType = eventTypeList.item(7);
            Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
            eventType = eventTypeList.item(8);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(9);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            exiEvent = exiEventList.get(4);
            Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertSame(exiEvent, eventType);
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(1, eventTypeList.getLength());
        }
    }

    /**
   * An attribute with an invalid boolean value matching ITEM_AT_WC_ANY 
   * where there is a global attribute declaration given for the attribute with
   * datatype xsd:boolean. 
   */
    public void testInvalidBoolean_04() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
        final String xmlString = "<foo:B xmlns:foo='urn:foo' xmlns:goo='urn:goo' goo:aX='tree' />\n";
        for (AlignmentType alignment : Alignments) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            encoder.setEXISchema(grammarCache);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.setOutputStream(baos);
            byte[] bts;
            int n_events;
            encoder.encode(new InputSource(new StringReader(xmlString)));
            bts = baos.toByteArray();
            decoder.setEXISchema(grammarCache);
            decoder.setInputStream(new ByteArrayInputStream(bts));
            scanner = decoder.processHeader();
            ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
            EXIEvent exiEvent;
            n_events = 0;
            while ((exiEvent = scanner.nextEvent()) != null) {
                ++n_events;
                exiEventList.add(exiEvent);
            }
            Assert.assertEquals(5, n_events);
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
            Assert.assertEquals("B", eventType.getName());
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertNull(eventTypeList.getEE());
            exiEvent = exiEventList.get(2);
            Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
            Assert.assertEquals("tree", exiEvent.getCharacters().makeString());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
            Assert.assertEquals(7, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(10, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventType = eventTypeList.item(4);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(6);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            eventType = eventTypeList.item(8);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(9);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            exiEvent = exiEventList.get(3);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
            Assert.assertEquals(4, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(10, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            eventType = eventTypeList.item(5);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(6);
            Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("aA", eventType.getName());
            eventType = eventTypeList.item(7);
            Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
            eventType = eventTypeList.item(8);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(9);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            exiEvent = exiEventList.get(4);
            Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertSame(exiEvent, eventType);
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(1, eventTypeList.getLength());
        }
    }

    /**
   * Boolean representation takes 2 bits to distinguish lexical values
   * when there is an associated pattern. 
   */
    public void testPatternedBoolean() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
        final String[] xmlStrings;
        final String[] originalValues = { " \t\r true\n", " \t\r false\n", " \t\r 1\n", " \t\r 0\n" };
        final String[] resultValues = { "true", "false", "1", "0" };
        final String startTag = "<foo:C xmlns:foo='urn:foo'>";
        final String endTag = "</foo:C>\n";
        int i;
        xmlStrings = new String[originalValues.length];
        for (i = 0; i < originalValues.length; i++) {
            xmlStrings[i] = startTag + originalValues[i] + endTag;
        }
        ;
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        encoder.setEXISchema(grammarCache);
        decoder.setEXISchema(grammarCache);
        for (AlignmentType alignment : Alignments) {
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            for (i = 0; i < xmlStrings.length; i++) {
                Scanner scanner;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                encoder.setOutputStream(baos);
                byte[] bts;
                int n_events, n_texts;
                encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
                bts = baos.toByteArray();
                decoder.setInputStream(new ByteArrayInputStream(bts));
                scanner = decoder.processHeader();
                ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
                EXIEvent exiEvent;
                n_events = 0;
                n_texts = 0;
                while ((exiEvent = scanner.nextEvent()) != null) {
                    ++n_events;
                    if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
                        String stringValue = exiEvent.getCharacters().makeString();
                        Assert.assertEquals(resultValues[i], stringValue);
                        Assert.assertTrue(exiEvent.getEventType().isSchemaInformed());
                        ++n_texts;
                    }
                    exiEventList.add(exiEvent);
                }
                Assert.assertEquals(1, n_texts);
                Assert.assertEquals(5, n_events);
            }
        }
        encoder.setAlignmentType(AlignmentType.bitPacked);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        try {
            encoder.encode(new InputSource(new StringReader(startTag + "a" + endTag)));
        } catch (TransmogrifierException eee) {
            Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, eee.getCode());
            return;
        }
        Assert.fail();
    }

    /**
   * Preserve lexical boolean values by turning on Preserve.lexicalValues.
   */
    public void testBooleanRCS() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
        final String[] xmlStrings;
        final String[] originalValues = { " \t\r *t*r*u*e*\n" };
        final String[] parsedOriginalValues = { " \t\n *t*r*u*e*\n" };
        int i;
        xmlStrings = new String[originalValues.length];
        for (i = 0; i < originalValues.length; i++) {
            xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:A>\n";
        }
        ;
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        encoder.setEXISchema(grammarCache);
        decoder.setEXISchema(grammarCache);
        encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);
        encoder.setPreserveLexicalValues(true);
        for (AlignmentType alignment : Alignments) {
            encoder.setAlignmentType(alignment);
            for (i = 0; i < xmlStrings.length; i++) {
                Scanner scanner;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                encoder.setOutputStream(baos);
                byte[] bts;
                int n_events, n_texts;
                encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
                bts = baos.toByteArray();
                decoder.setInputStream(new ByteArrayInputStream(bts));
                scanner = decoder.processHeader();
                ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
                EXIEvent exiEvent;
                n_events = 0;
                n_texts = 0;
                while ((exiEvent = scanner.nextEvent()) != null) {
                    ++n_events;
                    if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
                        String stringValue = exiEvent.getCharacters().makeString();
                        Assert.assertEquals(parsedOriginalValues[i], stringValue);
                        Assert.assertTrue(exiEvent.getEventType().isSchemaInformed());
                        ++n_texts;
                    }
                    exiEventList.add(exiEvent);
                }
                Assert.assertEquals(1, n_texts);
                Assert.assertEquals(5, n_events);
            }
        }
    }

    /**
   */
    public void test4BooleanStore() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/DataStore/DataStore.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
        String[] booleanValues4 = { "true", "false", "0", "1" };
        for (AlignmentType alignment : Alignments) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder(999);
            Scanner scanner;
            InputSource inputSource;
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            encoder.setEXISchema(grammarCache);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.setOutputStream(baos);
            URL url = resolveSystemIdAsURL("/DataStore/instance/4BooleanStore.xml");
            inputSource = new InputSource(url.toString());
            inputSource.setByteStream(url.openStream());
            byte[] bts;
            int n_texts;
            encoder.encode(inputSource);
            bts = baos.toByteArray();
            decoder.setEXISchema(grammarCache);
            decoder.setInputStream(new ByteArrayInputStream(bts));
            scanner = decoder.processHeader();
            ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
            EXIEvent exiEvent;
            n_texts = 0;
            while ((exiEvent = scanner.nextEvent()) != null) {
                if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
                    String expected = booleanValues4[n_texts];
                    String val = exiEvent.getCharacters().makeString();
                    if ("true".equals(val)) {
                        Assert.assertTrue("true".equals(expected) || "1".equals(expected));
                    } else {
                        Assert.assertEquals("false", val);
                        Assert.assertTrue("false".equals(expected) || "0".equals(expected));
                    }
                    ++n_texts;
                }
                exiEventList.add(exiEvent);
            }
            Assert.assertEquals(4, n_texts);
        }
    }

    /**
   * Decode 1000BooleanStore.bitPacked
   */
    public void testDecode1000BooleanStore_BitPacked() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/DataStore/DataStore.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
        String[] booleanValues100 = { "false", "true", "false", "true", "0", "0", "1", "1", "1", "1" };
        AlignmentType alignment = AlignmentType.bitPacked;
        Scanner scanner;
        int n_texts;
        EXIDecoder decoder = new EXIDecoder(999);
        decoder.setEXISchema(grammarCache);
        decoder.setAlignmentType(alignment);
        URL url = resolveSystemIdAsURL("/DataStore/instance/1000BooleanStore.bitPacked");
        decoder.setInputStream(url.openStream());
        scanner = decoder.processHeader();
        EXIEvent exiEvent;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
            if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
                if (++n_texts % 100 == 0) {
                    String expected = booleanValues100[(n_texts / 100) - 1];
                    String val = exiEvent.getCharacters().makeString();
                    if ("true".equals(val)) {
                        Assert.assertTrue("true".equals(expected) || "1".equals(expected));
                    } else {
                        Assert.assertEquals("false", val);
                        Assert.assertTrue("false".equals(expected) || "0".equals(expected));
                    }
                }
            }
        }
        Assert.assertEquals(1000, n_texts);
    }
}
