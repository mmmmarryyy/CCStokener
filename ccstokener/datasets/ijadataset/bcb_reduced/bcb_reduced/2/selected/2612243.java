package com.rapidminer.operator.features.construction;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.text.io.filereader.FileReader;
import com.rapidminer.operator.text.io.filereader.XMLFileReader;
import com.rapidminer.operator.text.tools.queries.Match;
import com.rapidminer.operator.text.tools.queries.Query;
import com.rapidminer.operator.text.tools.queries.QueryService;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.TextType;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.NumberParser;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.io.Encoding;

/**
 * This operator allows to extract additional attributes from structured or unstructured web service results or simpler
 * HTTP requests using regular expression, XPath or simple string matching. The input texts are requested from the
 * specified URL. For flexibly including information from each example into the request, every <%attribute name%> is
 * replaced by the current examples value of the attribute with the given name. Since some webservice provider restrict
 * the number of requests per second, a delay might be used to obey this restriction.
 * 
 * The query type for extracting information from the response might be either XPath for XML documents, or regular
 * expressions for less structured texts. The XPath expression specifies directly which part of the XML document is
 * retrieved and this is used as value for the new attribute. If you use regular expressions, the first matching group
 * is used as value. For example an expression like "Name:\s*(.*)\n" on a text "Name:
 * Paul" followed by a line break will yield "Paul" as new value in the attribute.
 * 
 * String matching is a fast and easy to use replacement for regular expressions, but less powerful. You just have to
 * specify a start and an end string. Everything between the two strings is extracted. For example if the start string
 * would be "Name:" and the end string a linebreak, then the result of the above text would be "  Paul".
 * 
 * The response might contain a separated list of results, for example a XML tag like this:
 * <languages>en,de,fr,sp</languages> Then it is possible to enter the a query yielding "en,de,fr,sp" multiple times,
 * using different attribute names. If the separator parameter contains the ",", then the first attribute will be filled
 * with "en" the second with "de" and so on. This might be used to get only the first enumerated value, too. But be
 * careful with this feature, since other results might be splitted, too, even if you don't enter a query twice. You
 * might avoid this, by inserting a second operator, where you don't specify a separator.
 * 
 * @author Sebastian Land
 */
public class WebserviceBasedAttributeConstruction extends AbstractQueryBasedOperator {

    private static final String CODE_END = "%>";

    private static final String CODE_START = "<%";

    public static final String PARAMETER_HTTP_METHOD = "request_method";

    public static final String PARAMETER_WEB_SERVICE_METHOD = "service_method";

    public static final String PARAMETER_HTTP_BODY = "body";

    public static final String PARAMETER_URL = "url";

    public static final String PARAMETER_REQUEST_PROPERTIES = "request_properties";

    public static final String PARAMETER_REQUEST_PROPERTY = "property";

    public static final String PARAMETER_REQUEST_PROPERTY_VALUE = "value";

    public static final String PARAMETER_SEPARATOR = "separator";

    public static final String PARAMETER_DELAY = "delay";

    public static final String[] REQUEST_METHODS = new String[] { "GET", "POST" };

    public static final int REQUEST_METHOD_GET = 0;

    public static final int REQUEST_METHOD_POST = 1;

    private final InputPort exampleSetInput = getInputPorts().createPort("Example Set", ExampleSet.class);

    private final OutputPort exampleSetOutput = getOutputPorts().createPort("ExampleSet");

    public WebserviceBasedAttributeConstruction(OperatorDescription description) {
        super(description);
        getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

            @Override
            public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
                Pair<List<AttributeMetaData>, SetRelation> pair = QueryService.getAttributeMetaDataList(WebserviceBasedAttributeConstruction.this);
                metaData.addAllAttributes(pair.getFirst());
                metaData.mergeSetRelation(pair.getSecond());
                return metaData;
            }
        });
    }

    @Override
    public void doWork() throws OperatorException {
        ExampleSet exampleSet = exampleSetInput.getData();
        Attributes attributes = exampleSet.getAttributes();
        List<Pair<String, Attribute>> urlPiecesList = generatePiecesList(attributes, getParameterAsString(PARAMETER_URL));
        int requestMethod = REQUEST_METHOD_GET;
        List<Pair<String, Attribute>> bodyPiecesList = null;
        if (REQUEST_METHODS[REQUEST_METHOD_POST].equals(getParameterAsString(PARAMETER_HTTP_METHOD)) && isParameterSet(PARAMETER_HTTP_BODY)) {
            bodyPiecesList = generatePiecesList(attributes, getParameterAsString(PARAMETER_HTTP_BODY));
            requestMethod = REQUEST_METHOD_POST;
        }
        boolean useSeparator = false;
        String separator = null;
        if (isParameterSet(PARAMETER_SEPARATOR)) {
            useSeparator = true;
            separator = getParameterAsString(PARAMETER_SEPARATOR);
        }
        int delay = getParameterAsInt(PARAMETER_DELAY);
        Charset encoding = Encoding.getEncoding(this);
        Map<String, Pair<Attribute, Query>> attributeQueryMap = QueryService.getAttributeQueryMap(this);
        addAttributesToExampleSet(exampleSet, attributeQueryMap);
        HashMap<String, String> properties = new LinkedHashMap<String, String>();
        properties.put("Content-Type", "text/xml;charset=" + encoding.displayName());
        if (requestMethod == REQUEST_METHOD_POST && isParameterSet(PARAMETER_WEB_SERVICE_METHOD)) {
            properties.put("SOAPAction", getParameterAsString(PARAMETER_WEB_SERVICE_METHOD));
        }
        List<String[]> propertiesList = getParameterList(PARAMETER_REQUEST_PROPERTIES);
        for (String[] property : propertiesList) {
            properties.put(property[0], property[1]);
        }
        FileReader reader = new XMLFileReader();
        boolean firstExample = true;
        for (Example example : exampleSet) {
            checkForStop();
            if (!firstExample) {
                if (delay > 0) try {
                    Thread.sleep(delay);
                } catch (InterruptedException e2) {
                }
            }
            firstExample = false;
            try {
                URL url = new URL(gluePiecesList(urlPiecesList, example));
                URLConnection connection = url.openConnection();
                if (connection instanceof HttpURLConnection) {
                    HttpURLConnection httpConnection = (HttpURLConnection) connection;
                    if (requestMethod == REQUEST_METHOD_POST) {
                        httpConnection.setRequestMethod("POST");
                        httpConnection.setDoOutput(true);
                        for (Entry<String, String> entry : properties.entrySet()) {
                            httpConnection.addRequestProperty(entry.getKey(), entry.getValue());
                        }
                        String body = gluePiecesList(bodyPiecesList, example);
                        byte[] bodyBytes = body.getBytes(encoding);
                        httpConnection.addRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
                        OutputStream out = httpConnection.getOutputStream();
                        out.write(bodyBytes);
                        out.close();
                    }
                } else {
                    throw new UserError(this, "enrich_data_by_webservice.wrong_protocol");
                }
                String response = reader.readStream(connection.getInputStream(), true, encoding);
                Map<Query, StringTokenizer> queryTokenizerMap = new HashMap<Query, StringTokenizer>();
                for (String attributeName : attributeQueryMap.keySet()) {
                    Pair<Attribute, Query> pair = attributeQueryMap.get(attributeName);
                    Attribute attribute = pair.getFirst();
                    Query query = pair.getSecond();
                    Match queryResult = query.getFirstMatch(response);
                    String value;
                    if (queryResult != null) {
                        if (useSeparator) {
                            if (!queryTokenizerMap.containsKey(query)) {
                                queryTokenizerMap.put(query, new StringTokenizer(queryResult.getMatch(), separator));
                            }
                            StringTokenizer tokenizer = queryTokenizerMap.get(query);
                            if (tokenizer.hasMoreElements()) value = tokenizer.nextToken(); else value = null;
                        } else {
                            value = queryResult.getMatch();
                        }
                    } else {
                        value = null;
                    }
                    if (value != null) {
                        if (attribute.isNumerical()) {
                            try {
                                example.setValue(attribute, NumberParser.parse(value));
                            } catch (NumberFormatException e1) {
                                example.setValue(attribute, Double.NaN);
                            }
                        } else {
                            example.setValue(attribute, attribute.getMapping().mapString(value));
                        }
                    } else {
                        example.setValue(attribute, Double.NaN);
                    }
                }
            } catch (MalformedURLException e) {
                throw new UserError(this, 313);
            } catch (IOException e) {
                throw new UserError(this, e, 314, gluePiecesList(urlPiecesList, example));
            }
        }
        exampleSetOutput.deliver(exampleSet);
    }

    /**
     * This method will take the string cut it into pairs of text + attribute. So that the
     * resulting text can be combined by concatenating all subsequent text + attribute value pairs.
     * The attribute part of the pairs might be null. In this case, this part should simply be ignored.
     */
    private ArrayList<Pair<String, Attribute>> generatePiecesList(Attributes attributes, String string) {
        ArrayList<Pair<String, Attribute>> pieces = new ArrayList<Pair<String, Attribute>>();
        int lastEnd = 0;
        int start = string.indexOf(CODE_START, 0);
        while (start >= 0) {
            int end = string.indexOf(CODE_END, start);
            if (end > 0) {
                String attributeName = string.substring(start + CODE_START.length(), end);
                Attribute attribute = attributes.get(attributeName);
                if (attribute != null) {
                    pieces.add(new Pair<String, Attribute>(string.substring(lastEnd, start), attribute));
                    lastEnd = end + CODE_END.length();
                }
                start = string.indexOf(CODE_START, end);
            }
        }
        pieces.add(new Pair<String, Attribute>(string.substring(lastEnd, string.length()), null));
        return pieces;
    }

    private String gluePiecesList(List<Pair<String, Attribute>> piecesList, Example example) {
        StringBuilder builder = new StringBuilder();
        for (Pair<String, Attribute> piece : piecesList) {
            builder.append(piece.getFirst());
            if (piece.getSecond() != null) {
                builder.append(example.getValueAsString(piece.getSecond()));
            }
        }
        return builder.toString();
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeCategory(PARAMETER_HTTP_METHOD, "The method of the HTTP protocoll used for this webservice.", REQUEST_METHODS, REQUEST_METHOD_GET, true));
        ParameterType type = new ParameterTypeString(PARAMETER_WEB_SERVICE_METHOD, "This is the method of the webservice as defined by the respective soapAction attribute of the operation element of the WSDL file.", false);
        type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_HTTP_METHOD, REQUEST_METHODS, false, REQUEST_METHOD_POST));
        types.add(type);
        type = new ParameterTypeText(PARAMETER_HTTP_BODY, "This is the body of the request sent to the webservice. This parameter can be used for SOAP Webservices to specify the request and it's values. As in the URL, <%attributeName%> will be replaced by the attribute's value for each exapmle.", TextType.XML);
        type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_HTTP_METHOD, REQUEST_METHODS, false, REQUEST_METHOD_POST));
        types.add(type);
        types.add(new ParameterTypeString(PARAMETER_URL, "The url of the HTTP GET based service. This URL may contain terms of the form <%attributeName%>, including the braces, that are replaced by the value of the corresonding attribute before invoking the query.", false));
        types.add(new ParameterTypeString(PARAMETER_SEPARATOR, "Characters used to separate entries in the result field obtained by XPath or regular expression."));
        types.add(new ParameterTypeInt(PARAMETER_DELAY, "Amount of milliseconds to wait between requests", 0, (int) Double.POSITIVE_INFINITY, 0));
        types.add(new ParameterTypeList(PARAMETER_REQUEST_PROPERTIES, "With this parameter you can define all properties that are sent with the HTTP request to match the needs of your webservice.", new ParameterTypeString(PARAMETER_REQUEST_PROPERTY, "The name of the request property."), new ParameterTypeString(PARAMETER_REQUEST_PROPERTY_VALUE, "The value of the request property.")));
        types.addAll(Encoding.getParameterTypes(this));
        return types;
    }
}
