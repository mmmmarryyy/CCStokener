package org.xmlcml.cml.converters.marker.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nu.xom.Element;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlcml.cml.base.CMLConstants;
import org.xmlcml.cml.base.CMLUtil;
import org.xmlcml.cml.converters.AbstractConverterTestBase;
import org.xmlcml.cml.element.CMLModule;
import org.xmlcml.euclid.Util;
import org.xmlcml.util.TestUtils;

public class TextParserConverterTest extends AbstractConverterTestBase {

    private static Logger LOG = Logger.getLogger(TextParserConverterTest.class);

    public final String getLocalDirName() {
        return "marker/text";
    }

    public final String getInputSuffix() {
        return "txt";
    }

    public final String getOutputSuffix() {
        return "cml";
    }

    public final String getAuxiliaryFileName() {
        return "src/main/resources/org/xmlcml/cml/converters/marker/text/templateList.xml";
    }

    @Test
    public void testConverter() throws IOException {
        testQuiet();
    }

    @Test
    public void testParser() {
        TextParserConverter textParser = new TextParserConverter();
        String text = "" + "start junk\n" + "Hello world\n" + "    I read it in a book\n" + "more junk\n" + "end junk\n" + "";
        List<String> lines = Arrays.asList(text.split(CMLConstants.S_NEWLINE));
        textParser.readLinesIntoScalars(lines);
        Element templateListElement = CMLUtil.parseXML("" + "<templateList prefix='hello'>" + "<template dictRef='foo:bar'>" + "<regex>Hello world</regex>" + "<regex>    I read it in a book</regex>" + "</template>" + "</templateList>" + "");
        textParser.generateTemplateList(templateListElement);
        textParser.parseModule();
        CMLModule module = textParser.getCmlModule();
        Element expectedParsedModule = CMLUtil.parseXML("" + "<module xmlns='http://www.xml-cml.org/schema'>" + "<scalar>start junk</scalar>" + "<module dictRef='hello:bar'/>" + "<scalar>more junk</scalar>" + "<scalar>end junk</scalar>" + "</module>");
        TestUtils.assertEqualsCanonically("parsedModule", expectedParsedModule, module, true);
    }

    @Test
    public void testParser1() {
        TextParserConverter textParser = new TextParserConverter();
        String text = "" + "start junk\n" + "Hello world\n" + "    I read it in a book\n" + "more junk\n" + "end junk\n" + "Hello world\n" + "    I read it in a book\n" + "Hello world\n" + "    I read it in a book\n" + "start junk\n" + "Hello world\n" + "    I read it in a book\n" + "more junk\n" + "end junk\n" + "";
        List<String> lines = Arrays.asList(text.split(CMLConstants.S_NEWLINE));
        textParser.readLinesIntoScalars(lines);
        Element templateListElement = CMLUtil.parseXML("" + "<templateList prefix='hello'>" + "<template dictRef='foo:bar'>" + "<regex>Hello world</regex>" + "<regex>    I read it in a book</regex>" + "</template>" + "</templateList>" + "");
        textParser.generateTemplateList(templateListElement);
        textParser.parseModule();
        CMLModule module = textParser.getCmlModule();
        Element expectedParsedModule = CMLUtil.parseXML("" + "<module xmlns='http://www.xml-cml.org/schema'>" + "<scalar>start junk</scalar>" + "<module dictRef='hello:bar'/>" + "<scalar>more junk</scalar>" + "<scalar>end junk</scalar>" + "<module dictRef='hello:bar'/>" + "<module dictRef='hello:bar'/>" + "<scalar>start junk</scalar>" + "<module dictRef='hello:bar'/>" + "<scalar>more junk</scalar>" + "<scalar>end junk</scalar>" + "</module>");
        TestUtils.assertEqualsCanonically("parsedModule", expectedParsedModule, module, true);
    }

    @Test
    public void testParser2() {
        TextParserConverter textParser = new TextParserConverter();
        String text = "" + "start junk\n" + "Hello world\n" + "    I read it in a book\n" + "more junk\n" + "end junk\n" + "To be or not to be, that is the question\n" + "    Whether 'tis nobler in the heart\n" + "Hello world\n" + "    I read it in a book\n" + "start junk\n" + "To be or not to be, that is the question\n" + "    Whether 'tis nobler in the heart\n" + "more junk\n" + "end junk\n" + "";
        List<String> lines = Arrays.asList(text.split(CMLConstants.S_NEWLINE));
        textParser.readLinesIntoScalars(lines);
        Element templateListElement = CMLUtil.parseXML("" + "<templateList prefix='hello'>" + "<template dictRef='foo:bar'>" + "<regex>Hello world</regex>" + "<regex>    I read it in a book</regex>" + "</template>" + "<template dictRef='foo:hamlet'>" + "<regex>To be or not to be, that is the question</regex>" + "<regex>    Whether 'tis nobler in the heart</regex>" + "</template>" + "</templateList>" + "");
        textParser.generateTemplateList(templateListElement);
        textParser.parseModule();
        CMLModule module = textParser.getCmlModule();
        Element expectedParsedModule = CMLUtil.parseXML("" + "<module xmlns='http://www.xml-cml.org/schema'>" + "<scalar>start junk</scalar>" + "<module dictRef='hello:bar'/>" + "<scalar>more junk</scalar>" + "<scalar>end junk</scalar>" + "<module dictRef='hello:hamlet'/>" + "<module dictRef='hello:bar'/>" + "<scalar>start junk</scalar>" + "<module dictRef='hello:hamlet'/>" + "<scalar>more junk</scalar>" + "<scalar>end junk</scalar>" + "</module>");
        TestUtils.assertEqualsCanonically("parsedModule", expectedParsedModule, module, true);
    }

    @Test
    @Ignore
    public void testParser3() {
        TextParserConverter textParser = new TextParserConverter();
        Element templateListElement = readTemplateListElement("org/xmlcml/cml/converters/marker/text/templateList.xml");
        List<String> lines = readLines("org/xmlcml/cml/converters/marker/text/getopt.txt");
        textParser.readLinesIntoScalars(lines);
        textParser.generateTemplateList(templateListElement);
        textParser.parseModule();
        CMLModule module = textParser.getCmlModule();
        Element expectedParsedModule = readTemplateListElement("org/xmlcml/cml/converters/marker/text/getopt0.cml");
        TestUtils.assertEqualsCanonically("parsedModule", expectedParsedModule, module, true);
    }

    @Test
    @Ignore
    public void testParser4() {
        TextParserConverter textParser = new TextParserConverter();
        Element templateListElement = readTemplateListElement("org/xmlcml/cml/converters/marker/text/templateList0.xml");
        List<String> lines = readLines("org/xmlcml/cml/converters/marker/text/getopt.txt");
        LOG.debug("XX");
        textParser.readLinesIntoScalars(lines);
        textParser.generateTemplateList(templateListElement);
        LOG.debug("YY");
        textParser.parseModule();
        LOG.debug("ZZ");
        CMLModule module = textParser.getCmlModule();
        Element expectedParsedModule = readTemplateListElement("org/xmlcml/cml/converters/marker/text/getopt.cml");
        TestUtils.assertEqualsCanonically("parsedModule", expectedParsedModule, module, true);
    }

    private Element readTemplateListElement(String filename) {
        Element templateListElement = null;
        try {
            templateListElement = CMLUtil.getXMLResource(filename).getRootElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return templateListElement;
    }

    private List<String> readLines(String filename) {
        List<String> lines = new ArrayList<String>();
        URL url = Util.getResource(filename);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                lines.add(line);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return lines;
    }
}
