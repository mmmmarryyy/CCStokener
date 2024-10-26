package com.rapidminer.operator;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.extraction.AttributeQueryMap;
import com.rapidminer.operator.extraction.ExtractionException;
import com.rapidminer.operator.extraction.TextExtractionWrapper;
import com.rapidminer.operator.extraction.TextExtractor;
import com.rapidminer.operator.extraction.util.FeatureExtractionUtil;
import com.rapidminer.operator.extraction.util.NumberParser;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;

/**
 * Operator that enriches an example set with attributes gathered from a web resource. The web resource must be accessible via HTTP GET and return an XML tree as
 * result. The user specifies a generic URL of the resource (that may contain example dependent values) and a list of attribute names 
 * with a corresponding xpath expression. The attributes are then assigned the value to which the corresponding expression is evaluated. To support comma or space 
 * separated values in the XML tree it is allowed to use the same query more than once. The first attribute in the list
 * of attributes sharing the same query is assigned the first value, the second attribute the second and so on. Separators can be specified explicitely.
 *
 * @author Michael Wurst
 * @version $Id: MashupOperator.java,v 1.9 2009-03-14 08:48:56 ingomierswa Exp $
 *
 */
public class MashupOperator extends Operator {

    public static final String PARAMETER_URL = "url";

    public static final String PARAMETER_SEPARATORS = "separators";

    public static final String PARAMETER_DELAY = "delay";

    public MashupOperator(OperatorDescription description) {
        super(description);
    }

    public InputDescription getInputDescription(Class<?> cls) {
        if (ExampleSet.class.isAssignableFrom(cls)) {
            return new InputDescription(cls, true, false);
        } else {
            return super.getInputDescription(cls);
        }
    }

    public IOObject[] apply() throws OperatorException {
        String genericURL = getParameterAsString(PARAMETER_URL);
        String separators = getParameterAsString(PARAMETER_SEPARATORS);
        int delay = getParameterAsInt(PARAMETER_DELAY);
        ExampleSet es = getInput(ExampleSet.class);
        AttributeQueryMap aqMap = null;
        try {
            aqMap = FeatureExtractionUtil.getAttributeQueryMap(getParameters());
        } catch (ExtractionException e3) {
            UserError error = e3.getUserError();
            error.setOperator(this);
            throw error;
        }
        es.getExampleTable().addAttributes(aqMap.getAttributes());
        for (Attribute attribute : aqMap.getAttributes()) es.getAttributes().addRegular(attribute);
        for (Example e : es) {
            String urlStr = null;
            try {
                urlStr = rewriteURL(genericURL, e);
            } catch (URISyntaxException e3) {
                throw new UserError(this, 212, new Object[] { urlStr, e3 });
            }
            TextExtractionWrapper wrapper = null;
            try {
                URL url = new URL(urlStr);
                URLConnection connection = url.openConnection();
                wrapper = new TextExtractionWrapper(connection.getInputStream(), TextExtractionWrapper.CONTENT_TYPE_XML, false);
            } catch (ExtractionException e2) {
                UserError error = e2.getUserError();
                error.setOperator(this);
                throw error;
            } catch (MalformedURLException e2) {
                throw new UserError(this, 212, new Object[] { urlStr, e2 });
            } catch (IOException e2) {
                throw new UserError(this, 302, new Object[] { urlStr, e2 });
            }
            if (wrapper != null) {
                Map<Object, StringTokenizer> queryMap = new HashMap<Object, StringTokenizer>();
                for (Attribute att : aqMap.getAttributes()) {
                    TextExtractor query = aqMap.getQuery(att);
                    StringTokenizer tokenizer = queryMap.get(query);
                    if (tokenizer == null) {
                        Iterator<String> values = null;
                        try {
                            values = wrapper.getValues(query);
                        } catch (ExtractionException e1) {
                            logWarning("Could not extract values from xml:\n" + e1);
                        }
                        if ((values != null) && values.hasNext()) {
                            String result = values.next();
                            if (result != null) {
                                tokenizer = new StringTokenizer(result, separators);
                                queryMap.put(query, tokenizer);
                            }
                        }
                    }
                    if ((tokenizer != null) && tokenizer.hasMoreElements()) {
                        String value = tokenizer.nextToken();
                        if (!att.isNominal()) {
                            double numericalValue;
                            try {
                                numericalValue = NumberParser.parse(value);
                            } catch (NumberFormatException e1) {
                                numericalValue = Double.NaN;
                            }
                            e.setValue(att, numericalValue);
                        } else {
                            e.setValue(att, att.getMapping().mapString(value));
                        }
                    } else {
                        e.setValue(att, Double.NaN);
                    }
                }
            }
            try {
                if (delay > 0) Thread.sleep(delay);
            } catch (InterruptedException e2) {
            }
        }
        return new IOObject[0];
    }

    private String rewriteURL(String genericURLStr, Example e) throws URISyntaxException {
        String result = null;
        result = genericURLStr;
        Iterator<Attribute> i = e.getAttributes().allAttributes();
        while (i.hasNext()) {
            Attribute attribute = i.next();
            String value = e.getValueAsString(attribute);
            result = result.replaceAll("<" + attribute.getName() + ">", (new URI(null, value, null)).toASCIIString());
        }
        return result;
    }

    public Class<?>[] getInputClasses() {
        return new Class[] { ExampleSet.class };
    }

    public Class<?>[] getOutputClasses() {
        return new Class[0];
    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterType attributesParam = FeatureExtractionUtil.createQueryParameter();
        attributesParam.setExpert(false);
        types.add(attributesParam);
        ParameterType namespaceParam = FeatureExtractionUtil.createNamespaceParameter();
        namespaceParam.setExpert(false);
        types.add(namespaceParam);
        ParameterType urlParam = new ParameterTypeString(PARAMETER_URL, "The url of the HTTP GET based service. This URL may contain terms of the form <attributeName> that are replaced by the value of the corresonding attribute before invoking the query.");
        types.add(urlParam);
        types.add(new ParameterTypeString(PARAMETER_SEPARATORS, "Characters used to separate entries in the result field obtained by XPath or regular expression."));
        types.add(new ParameterTypeInt(PARAMETER_DELAY, "Amount of milliseconds to wait between requests", 0, (int) Double.POSITIVE_INFINITY, 0));
        return types;
    }
}
