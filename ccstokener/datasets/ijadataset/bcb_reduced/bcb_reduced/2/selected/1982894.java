package org.openexi.proc;

import java.net.URL;
import java.util.ArrayList;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIEvent;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.EventTypeSchema;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.io.compression.ChannellingScanner;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.TestBase;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import junit.framework.Assert;

public class DecodeStrictTest extends TestBase {

    public DecodeStrictTest(String name) {
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
   * Decode EXI-encoded NLM data.
   */
    public void testNLM_strict_01() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/NLM/nlmcatalogrecord_060101.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
        String[] exiFiles = { "/NLM/catplussamp2006.bitPacked", "/NLM/catplussamp2006.byteAligned", "/NLM/catplussamp2006.preCompress", "/NLM/catplussamp2006.compress" };
        for (int i = 0; i < Alignments.length; i++) {
            AlignmentType alignment = Alignments[i];
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;
            decoder.setAlignmentType(alignment);
            URL url = resolveSystemIdAsURL(exiFiles[i]);
            int n_events;
            decoder.setEXISchema(grammarCache);
            decoder.setInputStream(url.openStream());
            scanner = decoder.processHeader();
            ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
            EXIEvent exiEvent;
            n_events = 0;
            while ((exiEvent = scanner.nextEvent()) != null) {
                ++n_events;
                exiEventList.add(exiEvent);
            }
            Assert.assertEquals(35176, n_events);
            exiEvent = exiEventList.get(33009);
            Assert.assertEquals("Interdisciplinary Studies", exiEvent.getCharacters().makeString());
        }
    }

    /**
   * Only a handful of values in a stream.
   */
    public void testSequence_01() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/interop/schemaInformedGrammar/acceptance.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
        String[] exiFiles = { "/interop/schemaInformedGrammar/declaredProductions/sequence-01.bitPacked", "/interop/schemaInformedGrammar/declaredProductions/sequence-01.byteAligned", "/interop/schemaInformedGrammar/declaredProductions/sequence-01.preCompress", "/interop/schemaInformedGrammar/declaredProductions/sequence-01.compress" };
        for (int i = 0; i < Alignments.length; i++) {
            AlignmentType alignment = Alignments[i];
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;
            decoder.setAlignmentType(alignment);
            URL url = resolveSystemIdAsURL(exiFiles[i]);
            int n_events;
            decoder.setEXISchema(grammarCache);
            decoder.setInputStream(url.openStream());
            scanner = decoder.processHeader();
            ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
            EXIEvent exiEvent;
            n_events = 0;
            while ((exiEvent = scanner.nextEvent()) != null) {
                ++n_events;
                exiEventList.add(exiEvent);
            }
            Assert.assertEquals(19, n_events);
            EventType eventType;
            EventTypeList eventTypeList;
            int pos = 0;
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertSame(exiEvent, eventType);
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(1, eventTypeList.getLength());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
            Assert.assertEquals("A", exiEvent.getName());
            Assert.assertEquals("urn:foo", exiEvent.getURI());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
            Assert.assertEquals("AB", exiEvent.getName());
            Assert.assertEquals("urn:foo", exiEvent.getURI());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
            Assert.assertEquals("", exiEvent.getCharacters().makeString());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
            Assert.assertEquals("AC", exiEvent.getName());
            Assert.assertEquals("urn:foo", exiEvent.getURI());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
            Assert.assertEquals("", exiEvent.getCharacters().makeString());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
            Assert.assertEquals("AC", exiEvent.getName());
            Assert.assertEquals("urn:foo", exiEvent.getURI());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
            Assert.assertEquals("", exiEvent.getCharacters().makeString());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
            Assert.assertEquals("AD", exiEvent.getName());
            Assert.assertEquals("urn:foo", exiEvent.getURI());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
            Assert.assertEquals("", exiEvent.getCharacters().makeString());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
            Assert.assertEquals("AE", exiEvent.getName());
            Assert.assertEquals("urn:foo", exiEvent.getURI());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
            Assert.assertEquals("", exiEvent.getCharacters().makeString());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            exiEvent = exiEventList.get(pos++);
            Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
        }
    }

    /**
   */
    public void testHeaderOptionsAlignment_01() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/optionsSchema.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
        String[] exiFiles = { "/encoding/headerOptions-01.bitPacked", "/encoding/headerOptions-01.byteAligned", "/encoding/headerOptions-01.preCompress", "/encoding/headerOptions-01.compress" };
        AlignmentType[] alignments = { AlignmentType.bitPacked, AlignmentType.byteAligned, AlignmentType.preCompress, AlignmentType.compress };
        for (int i = 0; i < alignments.length; i++) {
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;
            URL url = resolveSystemIdAsURL(exiFiles[i]);
            int n_events;
            final AlignmentType falseAlignmentType;
            falseAlignmentType = alignments[i] == AlignmentType.compress ? AlignmentType.bitPacked : AlignmentType.compress;
            decoder.setAlignmentType(falseAlignmentType);
            decoder.setEXISchema(grammarCache);
            decoder.setInputStream(url.openStream());
            scanner = decoder.processHeader();
            Assert.assertEquals(alignments[i], scanner.getAlignmentType());
            ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
            EXIEvent exiEvent;
            n_events = 0;
            while ((exiEvent = scanner.nextEvent()) != null) {
                ++n_events;
                exiEventList.add(exiEvent);
            }
            Assert.assertEquals(6, n_events);
            EventType eventType;
            exiEvent = exiEventList.get(0);
            Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
            exiEvent = exiEventList.get(1);
            Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
            Assert.assertEquals("header", exiEvent.getName());
            Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
            exiEvent = exiEventList.get(2);
            Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
            Assert.assertEquals("strict", exiEvent.getName());
            Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
            exiEvent = exiEventList.get(3);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            exiEvent = exiEventList.get(4);
            Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
            exiEvent = exiEventList.get(5);
            Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
            eventType = exiEvent.getEventType();
            Assert.assertSame(exiEvent, eventType);
            Assert.assertEquals(0, eventType.getIndex());
        }
    }

    /**
   */
    public void testEmptyBlock_01() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/compression/emptyBlock_01.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        decoder.setAlignmentType(AlignmentType.compress);
        decoder.setBlockSize(1);
        URL url = resolveSystemIdAsURL("/compression/emptyBlock_01.compress");
        int n_events;
        decoder.setEXISchema(grammarCache);
        decoder.setInputStream(url.openStream());
        scanner = decoder.processHeader();
        ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
        EXIEvent exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            exiEventList.add(exiEvent);
        }
        Assert.assertEquals(11, n_events);
        Assert.assertEquals(1, ((ChannellingScanner) scanner).getBlockCount());
        EventType eventType;
        EventTypeList eventTypeList;
        int pos = 0;
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("root", exiEvent.getName());
        Assert.assertEquals("", eventType.getURI());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("parent", exiEvent.getName());
        Assert.assertEquals("", eventType.getURI());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("child", exiEvent.getName());
        Assert.assertEquals("", eventType.getURI());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals("42", exiEvent.getCharacters().makeString());
        int tp = ((EventTypeSchema) exiEvent.getEventType()).getSchemaSubstance();
        Assert.assertEquals(EXISchemaConst.UNSIGNED_BYTE_TYPE, corpus.getSerialOfType(tp));
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("adjunct", exiEvent.getName());
        Assert.assertEquals("", exiEvent.getURI());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
    }

    /**
   * Enumeration of union
   */
    public void testEnmueration_04() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/interop/datatypes/enumeration/enumeration.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
        String exiFiles = "/interop/datatypes/enumeration/enumeration-valid-04.byteAligned";
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        decoder.setAlignmentType(AlignmentType.byteAligned);
        URL url = resolveSystemIdAsURL(exiFiles);
        int n_events;
        decoder.setEXISchema(grammarCache);
        decoder.setInputStream(url.openStream());
        scanner = decoder.processHeader();
        ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
        EXIEvent exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            exiEventList.add(exiEvent);
        }
        Assert.assertEquals(10, n_events);
        EventType eventType;
        EventTypeList eventTypeList;
        int pos = 0;
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(1, eventTypeList.getLength());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("root", exiEvent.getName());
        Assert.assertEquals("", exiEvent.getURI());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("union", exiEvent.getName());
        Assert.assertEquals("", exiEvent.getURI());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals("+10", exiEvent.getCharacters().makeString());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("union", exiEvent.getName());
        Assert.assertEquals("", exiEvent.getURI());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals("12:32:00", exiEvent.getCharacters().makeString());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        exiEvent = exiEventList.get(pos++);
        Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
    }
}
