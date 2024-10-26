package net.sf.ngrease;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import net.sf.ngrease.core.ast.Element;
import net.sf.ngrease.core.ast.ElementIterator;
import net.sf.ngrease.core.ast.ElementParser;
import net.sf.ngrease.core.ast.ElementPrettyPrinter;
import net.sf.ngrease.core.ast.ElementUtil;
import net.sf.ngrease.core.ast.InputStreamUtil;
import net.sf.ngrease.core.ast.MetaParser;
import net.sf.ngrease.core.ast.NgreaseException;
import net.sf.ngrease.core.metalanguage.BooleanUtils;
import net.sf.ngrease.core.metalanguage.Ngrease;
import net.sf.ngrease.core.metalanguage.StringConverter;

public class NgreaseAcceptanceTest extends TestCase {

    private static void assertFeatureEvaluatesToResult(Element expectedResult, Element feature, boolean shouldPass) {
        boolean areEqual = ElementUtil.equals(expectedResult, feature, true);
        if (shouldPass) {
            if (!areEqual) {
                fail("Expected and got:\n" + printSideBySide(ElementPrettyPrinter.prettyPrint(expectedResult), ElementPrettyPrinter.prettyPrint(feature), 30));
            }
        } else {
            if (areEqual) {
                fail("A happy failure: it's time to" + " require success for this feature :)");
            }
        }
    }

    private static void assertAssertionStatus(String resourcePath, boolean shouldPass) {
        Element expectedResult = BooleanUtils.TRUE;
        Element feature = null;
        try {
            feature = evaluate(resourcePath + ".ngr");
        } catch (Exception e) {
            if (shouldPass) {
                e.printStackTrace();
                throw new NgreaseException(e);
            }
            return;
        }
        assertFeatureEvaluatesToResult(expectedResult, feature, shouldPass);
    }

    private static void assertFeatureStatus(String resourcePath, boolean shouldPass) {
        Element expectedResult = null;
        Element feature = null;
        try {
            expectedResult = parse(resourcePath + "-result.ngr");
            feature = evaluate(resourcePath + ".ngr");
        } catch (Exception e) {
            if (shouldPass) {
                e.printStackTrace();
                throw new NgreaseException(e);
            }
            return;
        }
        assertFeatureEvaluatesToResult(expectedResult, feature, shouldPass);
    }

    private static void assertStringGenerator(String path) {
        Element element = evaluate(path + ".ngr");
        InputStream resultStream = NgreaseAcceptanceTest.class.getResourceAsStream(path + "-result.txt");
        String expectedResult;
        try {
            expectedResult = InputStreamUtil.getStringContent(resultStream);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AssertionFailedError("" + e);
        }
        String actualResult = StringConverter.elementToString(element);
        assertEquals(expectedResult, actualResult);
    }

    private static String printSideBySide(String s1, String s2, int width) {
        StringBuffer b = new StringBuffer();
        BufferedReader reader1 = new BufferedReader(new StringReader(s1));
        BufferedReader reader2 = new BufferedReader(new StringReader(s2));
        while (true) {
            try {
                String line1 = reader1.readLine();
                String line2 = reader2.readLine();
                if (line1 == null && line2 == null) {
                    return b.toString();
                }
                b.append(pad(line1, width));
                b.append("|");
                b.append(pad(line2, width));
                b.append("\n");
            } catch (IOException e) {
                throw new IllegalStateException("IOException with strings!?");
            }
        }
    }

    private static String pad(String line, int width) {
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < width; i++) {
            if (line == null || i >= line.length()) {
                b.append(" ");
            } else {
                b.append(line.charAt(i));
            }
        }
        return b.toString();
    }

    private static Element parse(String resourcePath) throws IOException {
        URL url = NgreaseAcceptanceTest.class.getResource(resourcePath);
        ElementParser parser = new MetaParser();
        InputStream stream = url.openStream();
        Element result = parser.parse(stream);
        return result;
    }

    private static Element evaluate(String resourcePath) {
        URL url = NgreaseAcceptanceTest.class.getResource(resourcePath);
        Element result = Ngrease.getInstance().evaluate(url);
        return result;
    }

    public void testEvaluateVerySimple() {
        URL url = getClass().getResource("/net/sf/ngrease/very-simple.ngr");
        Element result = Ngrease.getInstance().evaluate(url);
        assertEquals("(AnElement ())", ElementUtil.elementToLispString(result));
    }

    public void testDefaultContext() {
        Element result = evaluate("/net/sf/ngrease/acceptancetests/metaevaluation/default-context.ngr");
        Element defaultContextElement = Ngrease.defaultContextElement;
        assertEquals(2, result.getChildren().size());
        for (ElementIterator iter = result.getChildren().elementIterator(); iter.hasNext(); ) {
            Element child = iter.nextElement();
            assertSame(defaultContextElement, child);
        }
    }

    public void testArithmetic() {
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/arithmetic/greaterThan", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/arithmetic/hex-bytes", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/arithmetic/plus", true);
    }

    public void testElementModifications() {
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/elementmodifications/append-child", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/elementmodifications/append-children", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/elementmodifications/append-symbols", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/elementmodifications/child", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/elementmodifications/flatten", true);
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/elementmodifications/identity", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/elementmodifications/keep-children", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/elementmodifications/pretty-print", true);
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/elementmodifications/symbol-of", true);
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/elementmodifications/tree-append", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/elementmodifications/replace-children", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/elementmodifications/separate-children", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/elementmodifications/set-symbol", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/elementmodifications/split-symbol", true);
    }

    public void testElements() {
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/elements/child-count", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/elements/equals", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/elements/child-exists", true);
    }

    public void testJava2Ngr() {
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/java2ngr/dir2javasrcmodule", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/java2ngr/fullround-java", true);
    }

    public void testLogic() {
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/logic/and", true);
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/logic/if", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/logic/not", true);
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/logic/or", true);
    }

    public void testMetaEvaluation() {
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/metaevaluation/compose", true);
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/metaevaluation/context-overriding", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/metaevaluation/defined", true);
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/metaevaluation/double-evaluate", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/metaevaluation/evaluate", true);
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/metaevaluation/escaped-evaluate", true);
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/metaevaluation/quote", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/metaevaluation/splicing", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/metaevaluation/with", true);
    }

    public void testOntology() {
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/ontology/is-a", true);
    }

    public void testReferences() {
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/references/root", true);
    }

    public void testSyntacticSugar() {
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/syntacticsugar/colon", true);
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/syntacticsugar/commented-element", true);
    }

    public void testTransformationDemos() {
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/transformationdemos/identity", false);
    }

    public void testTransformations() {
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/transformations/constant_transformer", true);
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/transformations/hyperlink/root", false);
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/transformations/nonconstant_transformer", true);
        assertAssertionStatus("/net/sf/ngrease/acceptancetests/transformations/transform-test", true);
        assertFeatureStatus("/net/sf/ngrease/acceptancetests/transformations/undefined_transformation", true);
    }

    public void testLanguageDemos() {
        assertStringGenerator("/net/sf/ngrease/languagedemos/article-as-html-source");
        assertStringGenerator("/net/sf/ngrease/languagedemos/simple-java-as-directory");
        assertStringGenerator("/net/sf/ngrease/languagedemos/java-class-with-property-as-directory");
    }
}
