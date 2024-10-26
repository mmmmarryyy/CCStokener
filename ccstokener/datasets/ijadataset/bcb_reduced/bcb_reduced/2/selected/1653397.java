package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.zip.Deflater;
import org.xml.sax.InputSource;
import org.openexi.proc.EXIDecoder;
import org.openexi.proc.HeaderOptionsOutputType;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIEvent;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.events.EXIEventAttributeByRef;
import org.openexi.proc.events.EXIEventWildcardAttributeByRef;
import org.openexi.proc.events.EXIEventSchemaNil;
import org.openexi.proc.events.EXIEventSchemaType;
import org.openexi.proc.grammars.EventTypeSchema;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.io.compression.ChannellingScanner;
import org.openexi.sax.Transmogrifier;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.TestBase;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import junit.framework.Assert;

public class CompressionTest extends TestBase {

    public CompressionTest(String name) {
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
   * EXI compression changes the order in which values are read and
   * written to and from an EXI stream.
   */
    public void testValueOrder_01() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema((String) null, getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
        for (AlignmentType alignment : Alignments) {
            for (boolean preserveWhitespaces : new boolean[] { true, false }) {
                Transmogrifier encoder = new Transmogrifier();
                EXIDecoder decoder = new EXIDecoder(31);
                Scanner scanner;
                InputSource inputSource;
                encoder.setAlignmentType(alignment);
                decoder.setAlignmentType(alignment);
                encoder.setEXISchema(grammarCache);
                encoder.setPreserveWhitespaces(preserveWhitespaces);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                encoder.setOutputStream(baos);
                URL url = resolveSystemIdAsURL("/compression/valueOrder-01.xml");
                inputSource = new InputSource(url.toString());
                inputSource.setByteStream(url.openStream());
                byte[] bts;
                int n_events;
                encoder.encode(inputSource);
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
                Assert.assertEquals(preserveWhitespaces ? 461 : 346, n_events);
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
                if (preserveWhitespaces) {
                    exiEvent = exiEventList.get(pos++);
                    Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
                    Assert.assertEquals("\n   ", exiEvent.getCharacters().makeString());
                }
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
                Assert.assertEquals("a", exiEvent.getName());
                Assert.assertEquals("", exiEvent.getURI());
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
                Assert.assertEquals("XXX", exiEvent.getCharacters().makeString());
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
                if (preserveWhitespaces) {
                    exiEvent = exiEventList.get(pos++);
                    Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
                    Assert.assertEquals("\n   ", exiEvent.getCharacters().makeString());
                }
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
                Assert.assertEquals("b", exiEvent.getName());
                Assert.assertEquals("", exiEvent.getURI());
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
                Assert.assertEquals("bla", exiEvent.getCharacters().makeString());
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
                if (preserveWhitespaces) {
                    exiEvent = exiEventList.get(pos++);
                    Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
                    Assert.assertEquals("\n   ", exiEvent.getCharacters().makeString());
                }
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
                Assert.assertEquals("c", exiEvent.getName());
                Assert.assertEquals("", exiEvent.getURI());
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
                Assert.assertEquals("foo", exiEvent.getCharacters().makeString());
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
                if (preserveWhitespaces) {
                    exiEvent = exiEventList.get(pos++);
                    Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
                    Assert.assertEquals("\n   ", exiEvent.getCharacters().makeString());
                }
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
                Assert.assertEquals("b", exiEvent.getName());
                Assert.assertEquals("", exiEvent.getURI());
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
                Assert.assertEquals("XXX", exiEvent.getCharacters().makeString());
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
                for (int i = 0; i < 110; i++) {
                    if (preserveWhitespaces) {
                        exiEvent = exiEventList.get(pos++);
                        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
                        Assert.assertEquals("\n   ", exiEvent.getCharacters().makeString());
                    }
                    exiEvent = exiEventList.get(pos++);
                    Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
                    Assert.assertEquals("a", exiEvent.getName());
                    Assert.assertEquals("", exiEvent.getURI());
                    exiEvent = exiEventList.get(pos++);
                    Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
                    Assert.assertEquals(Integer.toString(i + 1), exiEvent.getCharacters().makeString());
                    exiEvent = exiEventList.get(pos++);
                    Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
                }
                if (preserveWhitespaces) {
                    exiEvent = exiEventList.get(pos++);
                    Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
                    Assert.assertEquals("\n", exiEvent.getCharacters().makeString());
                }
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
            }
        }
    }

    /**
   * Values of xsi:nil attributes matching AT(xsi:nil) in schema-informed 
   * grammars are stored in structure channels whereas those that occur
   * in the context of built-in grammars are stored in value channels. 
   */
    public void testXsiNil_01() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema((String) null, getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
        final String xmlString = "<A xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:nil='true'>" + "  <B xmlns:xsd='http://www.w3.org/2001/XMLSchema' xsi:type='xsd:boolean' xsi:nil='true' />" + "  <A xsi:nil='true' />" + "</A>\n";
        AlignmentType[] alignments = new AlignmentType[] { AlignmentType.preCompress, AlignmentType.compress };
        for (AlignmentType alignment : alignments) {
            for (boolean preserveWhitespaces : new boolean[] { true, false }) {
                Transmogrifier encoder = new Transmogrifier();
                EXIDecoder decoder = new EXIDecoder(31);
                Scanner scanner;
                encoder.setAlignmentType(alignment);
                decoder.setAlignmentType(alignment);
                encoder.setEXISchema(grammarCache);
                encoder.setPreserveWhitespaces(preserveWhitespaces);
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
                Assert.assertEquals(preserveWhitespaces ? 14 : 12, n_events);
                int pos = 0;
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
                Assert.assertEquals("A", exiEvent.getName());
                Assert.assertEquals("", exiEvent.getURI());
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
                Assert.assertEquals("nil", exiEvent.getName());
                Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", exiEvent.getURI());
                Assert.assertEquals("true", exiEvent.getCharacters().makeString());
                Assert.assertTrue(exiEvent instanceof EXIEventWildcardAttributeByRef);
                if (preserveWhitespaces) {
                    exiEvent = exiEventList.get(pos++);
                    Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
                    Assert.assertEquals("  ", exiEvent.getCharacters().makeString());
                }
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
                Assert.assertEquals("B", exiEvent.getName());
                Assert.assertEquals("", exiEvent.getURI());
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
                Assert.assertEquals("type", ((EXIEventSchemaType) exiEvent).getName());
                Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", ((EXIEventSchemaType) exiEvent).getURI());
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
                Assert.assertTrue(((EXIEventSchemaNil) exiEvent).isNilled());
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
                if (preserveWhitespaces) {
                    exiEvent = exiEventList.get(pos++);
                    Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
                    Assert.assertEquals("  ", exiEvent.getCharacters().makeString());
                }
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
                Assert.assertEquals("A", exiEvent.getName());
                Assert.assertEquals("", exiEvent.getURI());
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
                Assert.assertEquals("nil", exiEvent.getName());
                Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", exiEvent.getURI());
                Assert.assertEquals("true", exiEvent.getCharacters().makeString());
                Assert.assertTrue(exiEvent instanceof EXIEventAttributeByRef);
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
                exiEvent = exiEventList.get(pos++);
                Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
            }
        }
    }

    /**
   * EXI test cases of National Library of Medicine (NLM) XML formats.
   */
    public void testNLM_default_01() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/NLM/nlmcatalogrecord_060101.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
        for (AlignmentType alignment : Alignments) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder(31);
            Scanner scanner;
            InputSource inputSource;
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            encoder.setEXISchema(grammarCache);
            encoder.setPreserveWhitespaces(true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.setOutputStream(baos);
            URL url = resolveSystemIdAsURL("/NLM/catplussamp2006.xml");
            inputSource = new InputSource(url.toString());
            inputSource.setByteStream(url.openStream());
            byte[] bts;
            int n_events;
            encoder.encode(inputSource);
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
            Assert.assertEquals(50071, n_events);
            exiEvent = exiEventList.get(47024);
            Assert.assertEquals("Interdisciplinary Studies", exiEvent.getCharacters().makeString());
        }
    }

    /**
   * EXI test cases of National Library of Medicine (NLM) XML formats.
   */
    public void testNLM_strict_01() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/NLM/nlmcatalogrecord_060101.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
        for (AlignmentType alignment : Alignments) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder(31);
            Scanner scanner;
            InputSource inputSource;
            encoder.setAlignmentType(alignment);
            decoder.setAlignmentType(alignment);
            encoder.setEXISchema(grammarCache);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.setOutputStream(baos);
            URL url = resolveSystemIdAsURL("/NLM/catplussamp2006.xml");
            inputSource = new InputSource(url.toString());
            inputSource.setByteStream(url.openStream());
            byte[] bts;
            int n_events;
            encoder.encode(inputSource);
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
        AlignmentType[] alignments = new AlignmentType[] { AlignmentType.preCompress, AlignmentType.compress };
        int[] strategies = { Deflater.DEFAULT_STRATEGY, Deflater.FILTERED, Deflater.HUFFMAN_ONLY };
        for (AlignmentType alignment : alignments) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder(31);
            Scanner scanner;
            InputSource inputSource;
            encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);
            encoder.setAlignmentType(alignment);
            encoder.setDeflateLevel(java.util.zip.Deflater.BEST_COMPRESSION);
            final boolean isCompress = alignment == AlignmentType.compress;
            byte[][] resultBytes = isCompress ? new byte[3][] : null;
            for (int i = 0; i < strategies.length; i++) {
                encoder.setDeflateStrategy(strategies[i]);
                encoder.setEXISchema(grammarCache);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                encoder.setOutputStream(baos);
                URL url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/sequence-01.xml");
                inputSource = new InputSource(url.toString());
                inputSource.setByteStream(url.openStream());
                byte[] bts;
                int n_events;
                encoder.encode(inputSource);
                bts = baos.toByteArray();
                if (isCompress) resultBytes[i] = bts;
                decoder.setEXISchema(grammarCache);
                decoder.setInputStream(new ByteArrayInputStream(bts));
                scanner = decoder.processHeader();
                Assert.assertEquals(alignment, scanner.getHeaderOptions().getAlignmentType());
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
            if (isCompress) {
                Assert.assertTrue(resultBytes[0].length < resultBytes[1].length);
                Assert.assertTrue(resultBytes[1].length < resultBytes[2].length);
            }
        }
    }

    /**
   * EXI test cases of Joint Theater Logistics Management format.
   */
    public void testJTLM_publish911() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/JTLM/schemas/TLMComposite.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
        AlignmentType alignment = AlignmentType.compress;
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        InputSource inputSource;
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
        encoder.setEXISchema(grammarCache);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        URL url = resolveSystemIdAsURL("/JTLM/publish911.xml");
        inputSource = new InputSource(url.toString());
        inputSource.setByteStream(url.openStream());
        byte[] bts;
        int n_events, n_texts;
        encoder.encode(inputSource);
        bts = baos.toByteArray();
        decoder.setEXISchema(grammarCache);
        decoder.setInputStream(new ByteArrayInputStream(bts));
        scanner = decoder.processHeader();
        ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
        EXIEvent exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
                if (exiEvent.getCharacters().length() == 0) {
                    --n_events;
                    continue;
                }
                if (n_texts % 100 == 0) {
                    final int n = n_texts / 100;
                    Assert.assertEquals(JTLMTest.publish911_centennials[n], exiEvent.getCharacters().makeString());
                }
                ++n_texts;
            }
            exiEventList.add(exiEvent);
        }
        Assert.assertEquals(96576, n_events);
    }

    /**
   * EXI test cases of Joint Theater Logistics Management format.
   */
    public void testJTLM_publish100_blockSize() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/JTLM/schemas/TLMComposite.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
        AlignmentType[] alignments = new AlignmentType[] { AlignmentType.preCompress, AlignmentType.compress };
        int[] blockSizes = { 1, 100, 101 };
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder(999);
        encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);
        encoder.setEXISchema(grammarCache);
        decoder.setEXISchema(grammarCache);
        for (AlignmentType alignment : alignments) {
            for (int i = 0; i < blockSizes.length; i++) {
                Scanner scanner;
                InputSource inputSource;
                encoder.setAlignmentType(alignment);
                encoder.setBlockSize(blockSizes[i]);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                encoder.setOutputStream(baos);
                URL url = resolveSystemIdAsURL("/JTLM/publish100.xml");
                inputSource = new InputSource(url.toString());
                inputSource.setByteStream(url.openStream());
                byte[] bts;
                int n_events, n_texts;
                encoder.encode(inputSource);
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
                        if (exiEvent.getCharacters().length() == 0) {
                            --n_events;
                            continue;
                        }
                        if (n_texts % 100 == 0) {
                            final int n = n_texts / 100;
                            Assert.assertEquals(JTLMTest.publish100_centennials[n], exiEvent.getCharacters().makeString());
                        }
                        ++n_texts;
                    }
                    exiEventList.add(exiEvent);
                }
                Assert.assertEquals(10610, n_events);
            }
        }
    }

    /**
   */
    public void testEmptyBlock_01() throws Exception {
        EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/compression/emptyBlock_01.xsd", getClass(), m_compilerErrors);
        Assert.assertEquals(0, m_compilerErrors.getTotalCount());
        GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder(31);
        Scanner scanner;
        InputSource inputSource;
        encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);
        encoder.setAlignmentType(AlignmentType.compress);
        encoder.setBlockSize(1);
        encoder.setEXISchema(grammarCache);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        URL url = resolveSystemIdAsURL("/compression/emptyBlock_01.xml");
        inputSource = new InputSource(url.toString());
        inputSource.setByteStream(url.openStream());
        byte[] bts;
        int n_events;
        encoder.encode(inputSource);
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
}
