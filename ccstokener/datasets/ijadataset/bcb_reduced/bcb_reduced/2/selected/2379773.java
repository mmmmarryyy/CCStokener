package com.volantis.mcs.css.impl.parser;

import com.volantis.mcs.css.parser.AggregatingDiagnosticListener;
import com.volantis.mcs.css.parser.CSSParser;
import com.volantis.mcs.css.renderer.CSSStyleSheetRenderer;
import com.volantis.mcs.css.renderer.RendererContext;
import com.volantis.mcs.css.renderer.StyleSheetRenderer;
import com.volantis.mcs.themes.StylePropertyDetails;
import com.volantis.mcs.themes.StyleSheet;
import com.volantis.mcs.themes.MutableStyleProperties;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;

/**
 * Tests to ensure that the CSS Parser can recover from syntax errors and parse
 * errors.
 */
public class CSSParserErrorRecoveryTestCase extends ParsingTestCaseAbstract {

    private AggregatingDiagnosticListener diagnosticListener;

    private String followingProperty = "width:100%";

    protected void setUp() throws Exception {
        super.setUp();
        diagnosticListener = new AggregatingDiagnosticListenerImpl();
    }

    protected CSSParser createParser() {
        return CSS_PARSER_FACTORY.createParser(getStyleSheetFactory(), diagnosticListener);
    }

    /**
     * Ensure that the parser can recover from an invalid color.
     */
    public void testInvalidColor() throws Exception {
        MutableStyleProperties properties = parseDeclarations("color:grey;" + followingProperty);
        checkFollowingPropertyAndMessages(properties, getNotAllowableMessage(1, 7, "color", "grey"));
    }

    /**
     * Ensure that the parser can recover from an invalid keyword.
     */
    public void testInvalidKeyword() throws Exception {
        MutableStyleProperties properties = parseDeclarations("float:flubber;" + followingProperty);
        checkFollowingPropertyAndMessages(properties, getNotAllowableMessage(1, 7, "float", "flubber"));
    }

    /**
     * Check that the following property, i.e. the one after the failing one is
     * still parsed and the message was expected.
     *
     * @param properties The properties to check.
     * @param message    The message to check.
     */
    private void checkFollowingPropertyAndMessages(MutableStyleProperties properties, final String message) {
        assertEquals(PERCENTAGE_100, properties.getStyleValue(StylePropertyDetails.WIDTH));
        assertEquals(message, diagnosticListener.getResults());
    }

    /**
     * Get the message when a token is not allowed for a specific property.
     *
     * @param line     The line.
     * @param column   The column.
     * @param property The property.
     * @param value    The value.
     * @return The message.
     */
    private String getNotAllowableMessage(final int line, final int column, final String property, final String value) {
        return getMessageLine(line, column, "'" + value + "' is not an allowable value for property '" + property + "'");
    }

    /**
     * Ensure that a missing value can be recovered from.
     */
    public void testMissingValue() throws Exception {
        MutableStyleProperties properties = parseDeclarations("color:;" + followingProperty);
        checkFollowingPropertyAndMessages(properties, getExpectedTokenMessage(1, 7, "<value>", null, ";"));
    }

    /**
     * Ensure that the parser can recover from a missing colon after the
     * property name.
     */
    public void testMissingColonAfterPropertyName() throws Exception {
        MutableStyleProperties properties = parseDeclarations("color;" + followingProperty);
        checkFollowingPropertyAndMessages(properties, getExpectedTokenMessage(1, 6, ":", null, ";"));
    }

    /**
     * Get the message when one token is expected but a different one is found.
     *
     * @param line     The line.
     * @param column   The column.
     * @param expected The expected token.
     * @param type     The type of the token.
     * @param actual   The actual token.
     * @return The message.
     */
    private String getExpectedTokenMessage(final int line, final int column, String expected, String type, final String actual) {
        if (!expected.startsWith("<")) {
            expected = '"' + expected + '"';
        }
        return getMessageLine(line, column, "Expected " + expected + " found " + (type == null ? "" : type + " ") + "\"" + actual + "\"");
    }

    /**
     * Ensure that the parser can recover from a missing colon within a
     * property definition.
     */
    public void testMissingColonInProperty() throws Exception {
        MutableStyleProperties properties = parseDeclarations("color red 1234 \"fred\";" + followingProperty);
        checkFollowingPropertyAndMessages(properties, getExpectedTokenMessage(1, 7, ":", "<identifier>", "red"));
    }

    /**
     * Ensure that the parser can recover from an invalid property.
     */
    public void testInvalidProperty() throws Exception {
        MutableStyleProperties properties = parseDeclarations("blah: red;" + followingProperty);
        checkFollowingPropertyAndMessages(properties, getMessageLine(1, 1, "Unknown property 'blah'"));
    }

    /**
     * Ensure that the parser can handle a large block of invalid CSS.
     *
     * <p>The CSS tested here comes from the W3C CSS 2.1 specification.</p>
     */
    public void testInvalidCSS() throws Exception {
        URL url = getClass().getResource("w3c.css");
        Reader reader = new InputStreamReader(url.openStream());
        StyleSheet sheet = parseStyleSheet(reader, "w3c.css");
        StyleSheetRenderer renderer = CSSStyleSheetRenderer.getSingleton();
        StringWriter writer = new StringWriter();
        RendererContext context = new RendererContext(writer, renderer);
        renderer.renderStyleSheet(sheet, context);
        String css = writer.toString();
        String results = diagnosticListener.getResults();
        System.out.println(results);
        assertEquals("a{color:red}\n" + "b{float:left}\n" + "f{color:green}\n" + "g{color:green}\n" + "h{color:green}\n" + "i{color:green}\n" + "j{color:green}\n" + "k{color:green}\n" + "l{color:green}", css);
    }

    /**
     * Get the message line.
     *
     * @param line    The line.
     * @param column  The column.
     * @param message The message.
     * @return The whole line.
     */
    private String getMessageLine(int line, int column, String message) {
        return line + ":" + column + ": ERROR - " + message + "\n";
    }
}
