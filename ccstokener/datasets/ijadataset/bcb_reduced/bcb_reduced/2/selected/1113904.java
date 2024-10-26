package com.volantis.mcs.runtime.debug;

import com.volantis.mcs.css.version.CSSVersion;
import com.volantis.mcs.css.version.DefaultCSSVersion;
import com.volantis.devrep.repository.api.devices.DevicePolicyConstants;
import com.volantis.mcs.dom.DOMFactory;
import com.volantis.mcs.dom.WalkingDOMVisitorStub;
import com.volantis.mcs.dom.DOMWalker;
import com.volantis.mcs.dom.Document;
import com.volantis.mcs.dom.Element;
import com.volantis.mcs.dom.debug.DOMUtilities;
import com.volantis.mcs.dom.debug.DocumentStyler;
import com.volantis.mcs.dom.debug.StyledDocumentWriter;
import com.volantis.mcs.dom.debug.DebugCharacterEncoder;
import com.volantis.mcs.dom.output.DOMDocumentOutputter;
import com.volantis.mcs.dom2theme.AssetResolver;
import com.volantis.mcs.dom2theme.ExtractorContext;
import com.volantis.mcs.dom2theme.StyledDOMThemeExtractorFactory;
import com.volantis.mcs.dom2theme.StyledDocumentOptimizer;
import com.volantis.mcs.dom2theme.extractor.ExtractorConfiguration;
import com.volantis.mcs.dom2theme.extractor.ExtractorConfigurationBuilder;
import com.volantis.mcs.dom2theme.extractor.PropertyDetailsSetHelper;
import com.volantis.mcs.expression.PolicyExpression;
import com.volantis.mcs.policies.PolicyReference;
import com.volantis.mcs.policies.variants.metadata.EncodingCollection;
import com.volantis.mcs.protocols.DOMTransformer;
import com.volantis.mcs.protocols.DeferredInheritTransformer;
import com.volantis.mcs.protocols.trans.NullRemovingDOMTransformer;
import com.volantis.mcs.runtime.styling.CSSCompilerBuilder;
import com.volantis.mcs.runtime.styling.StylingFunctions;
import com.volantis.mcs.runtime.themes.ThemeStyleSheetCompilerFactory;
import com.volantis.mcs.themes.MutableShorthandSet;
import com.volantis.mcs.themes.ShorthandSet;
import com.volantis.mcs.themes.StylePropertyDetails;
import com.volantis.mcs.xdime.XDIMESchemata;
import com.volantis.shared.iteration.IterationAction;
import com.volantis.shared.throwable.ExtendedRuntimeException;
import com.volantis.styling.StylingFactory;
import com.volantis.styling.compiler.CSSCompiler;
import com.volantis.styling.compiler.InlineStyleSheetCompilerFactory;
import com.volantis.styling.compiler.StyleSheetCompilerFactory;
import com.volantis.styling.device.DeviceOutlook;
import com.volantis.styling.engine.StylingEngine;
import com.volantis.styling.properties.PropertyDetailsSet;
import com.volantis.styling.properties.StyleProperty;
import com.volantis.styling.properties.StylePropertyDefinitions;
import com.volantis.styling.properties.StylePropertyIteratee;
import com.volantis.styling.sheet.CompiledStyleSheet;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to allow easy parsing and rendering of styled DOMs.
 * <p>
 * Useful for integration testing code which uses styled DOMs.
 */
public class StrictStyledDOMHelper {

    private final EntityResolver resolver;

    private StyledDocumentOptimizer optimizer;

    private final String styleAttributeName;

    /**
     * Construct a new StrictStyledDOMHelper with a null EntityResolver
     */
    public StrictStyledDOMHelper() {
        this(null, "style");
    }

    /**
     * Constructs a new StrictStyledDOMHelper with specified entity resolver.
     *
     * @param resolver entity resolver
     */
    public StrictStyledDOMHelper(EntityResolver resolver) {
        this(resolver, "style");
    }

    /**
     * Constructs a new StrictStyledDOMHelper with specified entity resolver,
     * and name of the style attribute.
     *
     * @param resolver entity resolver
     * @param styleAttributeName style attribute name
     */
    public StrictStyledDOMHelper(EntityResolver resolver, String styleAttributeName) {
        this.resolver = resolver;
        this.styleAttributeName = styleAttributeName;
        StyledDOMThemeExtractorFactory extractorFactory = StyledDOMThemeExtractorFactory.getDefaultInstance();
        final AssetResolver assetResolver = new AssetResolver() {

            public PolicyReference evaluateExpression(PolicyExpression expression) {
                return null;
            }

            public String resolveImage(PolicyReference reference) {
                return null;
            }

            public String resolveTranscodableImage(String transcodableUrl) {
                return null;
            }

            public String resolveVideo(PolicyReference reference) {
                return null;
            }

            public String resolveText(PolicyReference reference, EncodingCollection requiredEncodings) {
                return null;
            }
        };
        ExtractorContext context = new ExtractorContext() {

            public AssetResolver getAssetResolver() {
                return assetResolver;
            }

            public boolean generateTypeRules() {
                return true;
            }

            public CSSVersion getCSSVersion() {
                DefaultCSSVersion cssVersion = new DefaultCSSVersion("testVersion");
                cssVersion.markImmutable();
                return cssVersion;
            }
        };
        StylePropertyDefinitions definitions = StylePropertyDetails.getDefinitions();
        final List standardProperties = new ArrayList();
        definitions.iterateStyleProperties(new StylePropertyIteratee() {

            public IterationAction next(StyleProperty property) {
                String name = property.getName();
                if (property == StylePropertyDetails.MCS_INPUT_FORMAT || !name.startsWith("mcs-")) {
                    standardProperties.add(property);
                }
                return IterationAction.CONTINUE;
            }
        });
        PropertyDetailsSet detailsSet = PropertyDetailsSetHelper.getDetailsSet(standardProperties);
        ShorthandSet supportedShorthands = new MutableShorthandSet();
        ExtractorConfigurationBuilder extractorBuilder = extractorFactory.createConfigurationBuilder();
        extractorBuilder.setDetailsSet(detailsSet);
        extractorBuilder.setSupportedShorthands(supportedShorthands);
        CSSCompiler compiler = StylingFactory.getDefaultInstance().createDeviceCSSCompiler(DeviceOutlook.OPTIMISTIC);
        CompiledStyleSheet deviceStyleSheet = compiler.compile(new StringReader(DevicePolicyConstants.DEFAULT_DISPLAY_CSS), null);
        extractorBuilder.setDeviceStyleSheet(deviceStyleSheet);
        ExtractorConfiguration configuration = extractorBuilder.buildConfiguration();
        optimizer = extractorFactory.createOptimizer(configuration, context);
    }

    /**
     * Return a styled <code>Document</code> based on the the specified
     * <code>InputStream</code>.
     *
     * @param inputStream
     * @return a Document
     * @throws RuntimeException if there was a problem.
     */
    public Document parse(InputStream inputStream) {
        Document document;
        try {
            XMLReader reader = DOMUtilities.getReader();
            if (resolver != null) {
                reader.setEntityResolver(resolver);
            }
            document = DOMUtilities.read(reader, inputStream);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return parse(document);
    }

    /**
     * Return a <code>Document</code> based on the specified XML fragment.
     *
     * @param fragment
     * @return a Document
     * @throws RuntimeException if there was a problem.
     */
    public Document parse(String fragment) {
        Document document;
        try {
            document = DOMUtilities.read(fragment);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return parse(document);
    }

    /**
     * Return a styled version of the Document, using the default styles
     * described in the resource
     * <code>com/volantis/mcs/runtime/default.css</code>.
     *
     * @param document
     * @return a styled Document
     */
    public Document parse(Document document) {
        CSSCompilerBuilder compilerBuilder = new CSSCompilerBuilder();
        StyleSheetCompilerFactory compilerFactory = getStyleSheetCompilerFactory();
        compilerBuilder.setStyleSheetCompilerFactory(compilerFactory);
        CSSCompiler cssCompiler = compilerBuilder.getCSSCompiler();
        CompiledStyleSheet defaultCompiledStyleSheet;
        try {
            URL url = getClass().getResource("/com/volantis/mcs/runtime/default.css");
            InputStream stream = url.openStream();
            defaultCompiledStyleSheet = cssCompiler.compile(new InputStreamReader(stream), null);
        } catch (IOException e) {
            throw new ExtendedRuntimeException(e);
        }
        StylingFactory stylingFactory = StylingFactory.getDefaultInstance();
        StylingEngine stylingEngine = stylingFactory.createStylingEngine(new InlineStyleSheetCompilerFactory(StylingFunctions.getResolver()));
        stylingEngine.pushStyleSheet(defaultCompiledStyleSheet);
        DocumentStyler styler = new DocumentStyler(stylingEngine, XDIMESchemata.CDM_NAMESPACE);
        styler.style(document);
        DOMWalker walker = new DOMWalker(new WalkingDOMVisitorStub() {

            public void visit(Element element) {
                if (element.getStyles() == null) {
                    throw new IllegalArgumentException("element " + element.getName() + " has no styles");
                }
            }
        });
        walker.walk(document);
        DOMTransformer transformer = new DeferredInheritTransformer();
        document = transformer.transform(null, document);
        return document;
    }

    /**
     * Get the StyleSheetCompilerFactory.
     */
    protected StyleSheetCompilerFactory getStyleSheetCompilerFactory() {
        return ThemeStyleSheetCompilerFactory.getDefaultInstance();
    }

    /**
     * Return a <code>String</code> based on the specified
     * <code>Element</code>.
     *
     * @param element
     * @return an XML fragment string
     */
    public String render(Element element) {
        final Document document = DOMFactory.getDefaultInstance().createDocument();
        document.addNode(element);
        return render(document);
    }

    /**
     * Return a <code>String</code> based on the specified
     * <code>Document</code>.
     *
     * @param document
     * @return an XML string
     */
    public String render(Document document) {
        DOMTransformer transformer = new NullRemovingDOMTransformer();
        document = transformer.transform(null, document);
        transformer = new DeferredInheritTransformer();
        document = transformer.transform(null, document);
        optimizer.optimizeDocument(document);
        StringWriter writer = new StringWriter();
        StyledDocumentWriter documentWriter = new StyledDocumentWriter(writer, styleAttributeName);
        DOMDocumentOutputter outputter = new DOMDocumentOutputter(documentWriter, new DebugCharacterEncoder());
        try {
            outputter.output(document);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }

    /**
     * Normalise a string XML fragment by parsing it into a styled DOM and then
     * rendering it back to a string.
     *
     * @param fragment
     * @return
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    public String normalize(String fragment) throws IOException, SAXException {
        final Document expectedDOM = parse(fragment);
        return render(expectedDOM);
    }
}
