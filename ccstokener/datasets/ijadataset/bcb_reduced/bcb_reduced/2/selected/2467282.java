package r2q2.processing;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPException;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import r2q2.processing.exception.ConversionException;
import r2q2.util.properties.*;
import r2q2.NoAttachmentException;
import r2q2.SpecificationViolationException;
import r2q2.processing.expression.Expression;
import r2q2.processing.expression.ExpressionFactory;
import r2q2.processing.expression.InvalidArgumentsException;
import r2q2.variable.AreaMapEntry;
import r2q2.variable.AreaMapping;
import r2q2.variable.CorrectResponse;
import r2q2.variable.DirectedPair;
import r2q2.util.streams.StreamCopier;
import r2q2.util.xml.DocumentBuilder;
import r2q2.variable.ItemVariable;
import r2q2.variable.MapEntry;
import r2q2.variable.Mapping;
import r2q2.variable.OutcomeDeclaraton;
import r2q2.variable.Pair;
import r2q2.variable.ResponseDeclaration;
import r2q2.variable.TemplateDeclaration;
import r2q2.variable.ItemVariable.BaseType;
import r2q2.variable.ItemVariable.Cardinality;
import r2q2.variable.ItemVariable.Shape;
import r2q2.processing.expression.operators.Inside;

public class ProcessingEngine {

    public static final Namespace[] ns = { Namespace.getNamespace("http://www.imsglobal.org/xsd/imsqti_v2p0"), Namespace.getNamespace("http://www.imsglobal.org/xsd/imsqti_v2p1") };

    public HashMap[] processTemplate(HashMap[] vars) throws NoAttachmentException, ProcessingException, SpecificationViolationException {
        try {
            Element root = getRootNode(MessageContext.getCurrentContext().getRequestMessage());
            Element templateProcessing = root.getChild("templateProcessing", ns[0]);
            if (templateProcessing == null) templateProcessing = root.getChild("templateProcessing", ns[1]);
            if (templateProcessing == null) return vars;
            HashMap<String, ItemVariable> objectVars = null;
            try {
                objectVars = populateObjectMaps(vars);
            } catch (ConversionException ce) {
                throw new ProcessingException(ce);
            }
            HashMap<String, ItemVariable> responses = null;
            try {
                Iterator it = templateProcessing.getChildren().iterator();
                while (it.hasNext()) responses = processTemplateElement((Element) it.next(), objectVars);
            } catch (InvalidArgumentsException iae) {
                iae.printStackTrace();
                throw new ProcessingException(iae);
            }
            try {
                return rePopulateHashArrays(responses);
            } catch (ConversionException ce) {
                throw new ProcessingException("Error: could not repopulate portable legacy objects correctly, aborting", ce);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public HashMap[] processResponse(HashMap[] vars) throws NoAttachmentException, ProcessingException, SpecificationViolationException {
        try {
            Element root = getRootNode(MessageContext.getCurrentContext().getRequestMessage());
            Element responseProcessing = root.getChild("responseProcessing", ns[0]);
            if (responseProcessing == null) responseProcessing = root.getChild("responseProcessing", ns[1]);
            if (responseProcessing == null) throw new SpecificationViolationException("Couldn't locate responseProcessing node!");
            if (responseProcessing.getChildren().size() == 0) {
                byte[] xml = null;
                String templateLocation = responseProcessing.getAttributeValue("templateLocation");
                if (templateLocation != null) {
                    try {
                        URL url = new URL(templateLocation);
                        xml = StreamCopier.copyToByteArray(url.openStream());
                    } catch (MalformedURLException e) {
                        throw new SpecificationViolationException("Invalid response processing template location URL", e);
                    } catch (IOException e) {
                        throw new SpecificationViolationException("Error reading from remote response processing template", e);
                    }
                } else {
                    String template = responseProcessing.getAttributeValue("template");
                    if (template != null) {
                        String filename = null;
                        try {
                            if (template.matches("http://www.imsglobal.org/question/qti_v2p[01]/rptemplates/match_correct")) {
                                filename = PropertiesLoader.getProperty("r2q2.processing.responsetemplates.matchCorrect");
                            } else if (template.matches("http://www.imsglobal.org/question/qti_v2p[01]/rptemplates/map_response")) {
                                filename = PropertiesLoader.getProperty("r2q2.processing.responsetemplates.mapResponse");
                            } else if (template.matches("http://www.imsglobal.org/question/qti_v2p[01]/rptemplates/map_response_point")) {
                                filename = PropertiesLoader.getProperty("r2q2.processing.responsetemplates.mapResponsePoint");
                            } else {
                                throw new ProcessingException("Unsupported response processing template");
                            }
                        } catch (IOException e) {
                            throw new ProcessingException("Couldn't read properties file", e);
                        } catch (NoSuchPropertyException e) {
                            throw new ProcessingException("Couldn't find template location property", e);
                        }
                        try {
                            xml = StreamCopier.copyToByteArray(new FileInputStream(new File(filename)));
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            throw new ProcessingException("Couldn't find template", e);
                        } catch (IOException e) {
                            e.printStackTrace();
                            throw new ProcessingException("Couldn't read template", e);
                        }
                    } else throw new SpecificationViolationException("No response processing rules!");
                }
                try {
                    responseProcessing = DocumentBuilder.getRootElement(xml);
                    if (!responseProcessing.getName().equals("responseProcessing")) throw new SpecificationViolationException("Response template not valid!");
                    if (responseProcessing.getChildren().size() < 1) throw new ProcessingException("No content in template - either a spec violation or an unsupported nested template");
                } catch (JDOMException e) {
                    throw new SpecificationViolationException("Error building DOM from response processing template", e);
                } catch (NoSuchPropertyException e) {
                    throw new ProcessingException(e);
                } catch (IOException e) {
                    throw new SpecificationViolationException("Error reading from response processing template", e);
                }
            }
            HashMap<String, ItemVariable> objectVars = null;
            try {
                objectVars = populateObjectMaps(vars);
            } catch (ConversionException ce) {
                throw new ProcessingException(ce);
            }
            HashMap<String, ItemVariable> responses = null;
            try {
                Iterator it = responseProcessing.getChildren().iterator();
                while (it.hasNext()) responses = processResponseElement((Element) it.next(), objectVars);
            } catch (InvalidArgumentsException iae) {
                iae.printStackTrace();
                throw new ProcessingException(iae);
            }
            try {
                Iterator it = responses.keySet().iterator();
                while (it.hasNext()) {
                    String key = (String) it.next();
                    Object test = responses.get(key);
                    if (test instanceof ResponseDeclaration) {
                        ResponseDeclaration rd = (ResponseDeclaration) test;
                        boolean correct = true;
                        if (rd.value != null && rd.correctResponse != null) {
                            Vector<Object> candResps;
                            Vector<Object> corrResps;
                            if (rd.card == Cardinality.single) {
                                Object candResp = rd.value;
                                Object corrResp = rd.correctResponse.value;
                                candResps = new Vector();
                                candResps.add(candResp);
                                corrResps = new Vector();
                                corrResps.add(corrResp);
                            } else {
                                candResps = (Vector<Object>) rd.value;
                                corrResps = (Vector<Object>) rd.correctResponse.value;
                                corrResps = (Vector<Object>) corrResps.clone();
                            }
                            if (rd.varType == BaseType.point) {
                                corrResps = new Vector();
                                corrResps = (Vector) rd.areaMapping.areaMapEntries;
                            }
                            if (candResps.size() == corrResps.size()) {
                                if (rd.varType == BaseType.point) {
                                    for (int i = 0; i < candResps.size(); i++) {
                                        Point point = (Point) candResps.get(i);
                                        boolean found = false;
                                        for (int j = 0; j < corrResps.size(); j++) {
                                            AreaMapEntry corr = (AreaMapEntry) corrResps.get(j);
                                            Double[] coordwrap = corr.coords;
                                            double[] coord = new double[coordwrap.length];
                                            for (int x = 0; x < coordwrap.length; x++) {
                                                coord[x] = coordwrap[x].doubleValue();
                                            }
                                            switch(corr.shape) {
                                                case circle:
                                                    found = Inside.insideCircle((Point2D) point, coord);
                                                    break;
                                                case ellipse:
                                                    found = Inside.insideEllipse((Point2D) point, coord);
                                                    break;
                                                case poly:
                                                    found = Inside.insidePolygon((Point2D) point, coord);
                                                    break;
                                                case rect:
                                                    found = Inside.insideRect((Point2D) point, coord);
                                                    break;
                                            }
                                            if (found == true) {
                                                corrResps.remove(j);
                                                break;
                                            }
                                        }
                                        if (!found) {
                                            correct = false;
                                        }
                                    }
                                } else {
                                    for (int i = 0; i < candResps.size(); i++) {
                                        Object cand = candResps.get(i);
                                        boolean found = false;
                                        if (rd.card == Cardinality.ordered) {
                                            Object corr = corrResps.get(i);
                                            if (cand.equals(corr)) {
                                                found = true;
                                            }
                                        } else {
                                            for (int j = 0; j < corrResps.size(); j++) {
                                                Object corr = corrResps.get(j);
                                                if (cand.equals(corr)) {
                                                    found = true;
                                                    corrResps.remove(j);
                                                    break;
                                                }
                                            }
                                        }
                                        if (!found) {
                                            correct = false;
                                        }
                                    }
                                }
                            } else {
                                correct = false;
                            }
                        } else {
                            correct = false;
                        }
                        rd.correct = correct;
                    }
                }
                return rePopulateHashArrays(responses);
            } catch (ConversionException ce) {
                throw new ProcessingException("Error: could not repopulate portable legacy objects correctly, aborting", ce);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private Element getRootNode(Message _request) throws NoAttachmentException, ProcessingException {
        if (_request.countAttachments() == 0) {
            throw new NoAttachmentException("No supplied XML");
        }
        Iterator attachmentIter = _request.getAttachments();
        AttachmentPart ap = (AttachmentPart) attachmentIter.next();
        byte[] xml;
        try {
            xml = StreamCopier.copyToByteArray(ap.getDataHandler().getInputStream());
        } catch (IOException e) {
            throw new ProcessingException("Couldn't process attachment", e);
        } catch (SOAPException e) {
            throw new ProcessingException("Couldn't process attachment", e);
        }
        try {
            return DocumentBuilder.getRootElement(xml);
        } catch (JDOMException e) {
            e.printStackTrace();
            throw new ProcessingException("Couldn't build document", e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ProcessingException("Couldn't build document", e);
        } catch (NoSuchPropertyException e) {
            e.printStackTrace();
            throw new ProcessingException("Couldn't build document", e);
        }
    }

    private HashMap<String, ItemVariable> processResponseElement(Element _e, HashMap<String, ItemVariable> vars) throws InvalidArgumentsException {
        String element = _e.getName();
        if (element.equals("exitResponse")) {
            return vars;
        } else if (element.equals("responseCondition")) {
            Iterator components = _e.getChildren().iterator();
            while (components.hasNext()) {
                Element ifChild = (Element) components.next();
                Iterator ifChildItr = ifChild.getChildren().iterator();
                if (ifChild.getName().equals("responseElse")) {
                    while (ifChildItr.hasNext()) processResponseElement((Element) ifChildItr.next(), vars);
                    return vars;
                } else {
                    Expression exp = ExpressionFactory.getInstance().makeExpression((Element) ifChildItr.next());
                    ItemVariable eval = exp.eval(vars);
                    if (eval == null) continue;
                    if (eval.card != Cardinality.single || eval.varType != BaseType.bool) throw new InvalidArgumentsException("Error: responseIf can only be passed an expression that evaluates to a single cardinality boolean value, aborting");
                    if (((Boolean) eval.value).booleanValue()) {
                        while (ifChildItr.hasNext()) processResponseElement((Element) ifChildItr.next(), vars);
                        return vars;
                    }
                }
            }
        } else if (element.equals("setOutcomeValue")) {
            String ident = _e.getAttribute("identifier").getValue();
            ItemVariable setVar = vars.get(ident);
            int size = _e.getChildren().size();
            if (size < 1 || size > 1) throw new InvalidArgumentsException("Error: setOutComeValue can only be called with a single contained expression, aborting");
            Expression exp = ExpressionFactory.getInstance().makeExpression((Element) _e.getChildren().iterator().next());
            ItemVariable iv = exp.eval(vars);
            if (iv.card == setVar.card && iv.varType == setVar.varType) {
                setVar.value = iv.value;
            } else if (iv.card == setVar.card && (iv.varType == BaseType.afloat || iv.varType == BaseType.integer) && (setVar.varType == BaseType.afloat || setVar.varType == BaseType.integer)) {
                if (setVar.varType == BaseType.afloat) {
                    setVar.value = new Double(((Integer) iv.value).doubleValue());
                } else {
                    setVar.value = new Integer(((Double) iv.value).intValue());
                }
            } else {
                throw new InvalidArgumentsException("Error: assignment of outcome variables must be of same cardinality and baseType as the variable definition, aborting");
            }
        } else {
            throw new InvalidArgumentsException("Error: unacceptable responseRule provided to response processing, aborting");
        }
        return vars;
    }

    private HashMap<String, ItemVariable> processTemplateElement(Element _e, HashMap<String, ItemVariable> vars) throws InvalidArgumentsException {
        String element = _e.getName();
        if (element.equals("exitTemplate")) {
            return vars;
        } else if (element.equals("templateCondition")) {
            Iterator components = _e.getChildren().iterator();
            int i = 1;
            while (components.hasNext()) {
                Element ifChild = (Element) components.next();
                Iterator ifChildItr = ifChild.getChildren().iterator();
                if (ifChild.getName().equals("templateElse")) {
                    while (ifChildItr.hasNext()) processTemplateElement((Element) ifChildItr.next(), vars);
                    return vars;
                } else {
                    Expression exp = ExpressionFactory.getInstance().makeExpression((Element) ifChildItr.next());
                    ItemVariable eval = exp.eval(vars);
                    if (eval.card != Cardinality.single || eval.varType != BaseType.bool) throw new InvalidArgumentsException("Error: responseIf can only be passed an expression that evaluates to a single cardinality boolean value, aborting");
                    if (((Boolean) eval.value).booleanValue()) {
                        while (ifChildItr.hasNext()) processTemplateElement((Element) ifChildItr.next(), vars);
                        return vars;
                    }
                }
                i++;
            }
        } else if (element.equals("setTemplateValue")) {
            String ident = _e.getAttribute("identifier").getValue();
            ItemVariable setVar = vars.get(ident);
            int size = _e.getChildren().size();
            if (size < 1 || size > 1) throw new InvalidArgumentsException("Error: setTemplateValue can only be called with a single contained expression, aborting");
            Expression exp = ExpressionFactory.getInstance().makeExpression((Element) _e.getChildren().iterator().next());
            ItemVariable iv = exp.eval(vars);
            if (iv.card == setVar.card && iv.varType == setVar.varType) {
                setVar.value = iv.value;
            } else if (iv.card == setVar.card && (iv.varType == BaseType.afloat || iv.varType == BaseType.integer) && (setVar.varType == BaseType.afloat || setVar.varType == BaseType.integer)) {
                if (setVar.varType == BaseType.afloat) {
                    setVar.value = new Double(((Integer) iv.value).doubleValue());
                } else {
                    setVar.value = new Integer(((Double) iv.value).intValue());
                }
            } else throw new InvalidArgumentsException("Error: assignment of template variables must be of same cardinality and baseType as the variable definition, aborting");
        } else if (element.equals("setCorrectResponse")) {
            String ident = _e.getAttribute("identifier").getValue();
            ItemVariable setVar = vars.get(ident);
            if (setVar instanceof ResponseDeclaration) {
                ResponseDeclaration rd = (ResponseDeclaration) setVar;
                int size = _e.getChildren().size();
                if (size < 1 || size > 1) throw new InvalidArgumentsException("Error: setCorrectResponse can only be called with a single contained expression, aborting");
                Expression exp = ExpressionFactory.getInstance().makeExpression((Element) _e.getChildren().iterator().next());
                ItemVariable iv = exp.eval(vars);
                if (iv.card == setVar.card && iv.varType == setVar.varType) {
                    if (rd.correctResponse == null) {
                        rd.correctResponse = new CorrectResponse();
                        rd.correctResponse.value = iv.value;
                    }
                } else if (iv.card == setVar.card && (iv.varType == BaseType.afloat || iv.varType == BaseType.integer) && (setVar.varType == BaseType.afloat || setVar.varType == BaseType.integer)) {
                    if (setVar.varType == BaseType.afloat) {
                        if (rd.correctResponse == null) {
                            rd.correctResponse = new CorrectResponse();
                            rd.correctResponse.value = new Double(((Integer) iv.value).doubleValue());
                        }
                    } else {
                        if (rd.correctResponse == null) {
                            rd.correctResponse = new CorrectResponse();
                            rd.correctResponse.value = new Integer(((Double) iv.value).intValue());
                        }
                    }
                }
            } else throw new InvalidArgumentsException("Error: can only set the correct response on a responseVariable, aborting");
        } else if (element.equals("setDefaultValue")) {
            String ident = _e.getAttribute("identifier").getValue();
            ItemVariable setVar = vars.get(ident);
            if (setVar instanceof ResponseDeclaration || setVar instanceof OutcomeDeclaraton) {
                int size = _e.getChildren().size();
                if (size < 1 || size > 1) throw new InvalidArgumentsException("Error: setCorrectResponse can only be called with a single contained expression, aborting");
                Expression exp = ExpressionFactory.getInstance().makeExpression((Element) _e.getChildren().iterator().next());
                ItemVariable iv = exp.eval(vars);
                if (iv.card == setVar.card && iv.varType == setVar.varType) {
                    setVar.defaultValue = iv.value;
                }
            } else throw new InvalidArgumentsException("Error: can only set the correct response on a responseVariable, aborting");
        } else throw new InvalidArgumentsException("Error: unacceptable responseRule provided to response processing, aborting");
        return vars;
    }

    private static HashMap<String, ItemVariable> populateObjectMaps(HashMap[] vars) throws ConversionException {
        HashMap<String, ItemVariable> ret = new HashMap<String, ItemVariable>();
        Iterator keys = vars[0].keySet().iterator();
        while (keys.hasNext()) {
            String identifier = (String) keys.next();
            HashMap retrieved = (HashMap) vars[0].get(identifier);
            ResponseDeclaration rd = new ResponseDeclaration();
            Iterator components = retrieved.keySet().iterator();
            while (components.hasNext()) {
                String fieldKey = (String) components.next();
                rd.identifier = identifier;
                if (fieldKey.equals("identifier")) {
                } else if (fieldKey.equals("cardinality")) {
                    String card = (String) retrieved.get(fieldKey);
                    if (card.equals("single")) rd.card = Cardinality.single; else if (card.equals("multiple")) rd.card = Cardinality.multiple; else if (card.equals("ordered")) rd.card = Cardinality.ordered; else if (card.equals("record")) rd.card = Cardinality.record; else throw new ConversionException("Error: unknown cardinality type in conversion, aborting");
                } else if (fieldKey.equals("baseType")) {
                    String baseType = (String) retrieved.get(fieldKey);
                    if (baseType.equals("float")) rd.varType = BaseType.afloat; else if (baseType.equals("integer")) rd.varType = BaseType.integer; else if (baseType.equals("identifier")) rd.varType = BaseType.identifier; else if (baseType.equals("boolean")) rd.varType = BaseType.bool; else if (baseType.equals("directedPair")) rd.varType = BaseType.directedPair; else if (baseType.equals("duration")) rd.varType = BaseType.duration; else if (baseType.equals("point")) rd.varType = BaseType.point; else if (baseType.equals("pair")) rd.varType = BaseType.pair; else if (baseType.equals("string")) rd.varType = BaseType.string; else if (baseType.equals("file")) rd.varType = BaseType.file; else if (baseType.equals("uri")) rd.varType = BaseType.uri; else throw new ConversionException("Error: unknown cardinality type in conversion, aborting");
                } else if (fieldKey.equals("candidateValues")) {
                    Object retObj = retrieved.get(fieldKey);
                    rd.value = retObj;
                } else if (fieldKey.equals("defaultValue")) {
                    Object defaultValue = retrieved.get(fieldKey);
                    rd.defaultValue = defaultValue;
                } else if (fieldKey.equals("defaultValueInterpretation")) {
                    String defaultValueInterpretation = (String) retrieved.get(fieldKey);
                    rd.defaultValueInterpretation = defaultValueInterpretation;
                } else if (fieldKey.equals("correctResponse")) {
                    Object correctResp = retrieved.get(fieldKey);
                    rd.correctResponse = new CorrectResponse();
                    rd.correctResponse.value = correctResp;
                } else if (fieldKey.equals("mappingDefaultValue")) {
                    if (rd.mapping == null) rd.mapping = new Mapping();
                    if (retrieved.get(fieldKey) == null) rd.mapping.defaultValue = null; else rd.mapping.defaultValue = Double.parseDouble((String) retrieved.get(fieldKey));
                } else if (fieldKey.equals("mappingLowerBound")) {
                    if (rd.mapping == null) rd.mapping = new Mapping();
                    if (retrieved.get(fieldKey) == null) rd.mapping.lowerBound = null; else rd.mapping.lowerBound = Double.parseDouble((String) retrieved.get(fieldKey));
                } else if (fieldKey.equals("mappingUpperBound")) {
                    if (rd.mapping == null) rd.mapping = new Mapping();
                    if (retrieved.get(fieldKey) == null) rd.mapping.upperBound = null; else rd.mapping.upperBound = Double.parseDouble((String) retrieved.get(fieldKey));
                } else if (fieldKey.equals("mapEntry")) {
                    if (rd.mapping == null) rd.mapping = new Mapping();
                    if (rd.mapping.mapEntries == null) rd.mapping.mapEntries = new Vector<MapEntry>();
                    if (retrieved.get(fieldKey) != null) {
                        HashMap mapHash = (HashMap) retrieved.get(fieldKey);
                        Iterator mapKeys = mapHash.keySet().iterator();
                        while (mapKeys.hasNext()) {
                            String key = (String) mapKeys.next();
                            Double value = Double.parseDouble((String) mapHash.get(key));
                            rd.mapping.mapEntries.add(new MapEntry(key, value));
                        }
                    }
                } else if (fieldKey.equals("areaMappingDefaultValue")) {
                    if (rd.areaMapping == null) rd.areaMapping = new AreaMapping();
                    if (retrieved.get(fieldKey) == null) rd.areaMapping.defaultValue = null; else rd.areaMapping.defaultValue = Double.parseDouble((String) retrieved.get(fieldKey));
                } else if (fieldKey.equals("areaMappingLowerBound")) {
                    if (rd.areaMapping == null) rd.areaMapping = new AreaMapping();
                    if (retrieved.get(fieldKey) == null) rd.areaMapping.lowerBound = null; else rd.areaMapping.lowerBound = Double.parseDouble((String) retrieved.get(fieldKey));
                } else if (fieldKey.equals("areaMappingUpperBound")) {
                    if (rd.areaMapping == null) rd.areaMapping = new AreaMapping();
                    if (retrieved.get(fieldKey) == null) rd.areaMapping.upperBound = null; else rd.areaMapping.upperBound = Double.parseDouble((String) retrieved.get(fieldKey));
                } else if (fieldKey.equals("areaMapEntry")) {
                    if (rd.areaMapping == null) rd.areaMapping = new AreaMapping();
                    if (rd.areaMapping.areaMapEntries == null) rd.areaMapping.areaMapEntries = new Vector<AreaMapEntry>();
                    if (retrieved.get(fieldKey) != null) {
                        Object[] areaMapList = (Object[]) retrieved.get(fieldKey);
                        for (int i = 0; i < areaMapList.length; i++) {
                            HashMap<String, String> element = (HashMap<String, String>) areaMapList[i];
                            AreaMapEntry ame = new AreaMapEntry();
                            String shapeType = element.get("shape");
                            if (shapeType == null) throw new ConversionException("Error: no shape provided in the legacy format, aborting");
                            if (shapeType.equals("circle")) ame.shape = Shape.circle; else if (shapeType.equals("rect")) ame.shape = Shape.rect; else if (shapeType.equals("poly")) ame.shape = Shape.poly; else if (shapeType.equals("ellipse")) ame.shape = Shape.ellipse; else throw new ConversionException("Error: default or other shapes not acceptable in this implementation, aborting");
                            String coords = element.get("coords");
                            if (coords == null) throw new ConversionException("Error: no coords provided in legacy format, aborting");
                            StringTokenizer st = new StringTokenizer(coords, ",");
                            ame.coords = new Double[st.countTokens()];
                            for (int j = 0; j < ame.coords.length; j++) ame.coords[j] = new Double(st.nextToken());
                            if (element.get("mappedValue") == null) throw new ConversionException("Error: a mapping must contain a value in the legacy format, aborting");
                            ame.mappedValue = new Double(element.get("mappedValue"));
                            rd.areaMapping.areaMapEntries.add(ame);
                        }
                    }
                }
            }
            if (rd.correctResponse != null) rd.correctResponse.value = unpack(rd.correctResponse.value, rd.varType);
            if (rd.mapping != null) {
                rd.mapping.mapEntries = unpackMapping(rd.mapping.mapEntries, rd.varType);
            }
            rd.defaultValue = unpack(rd.defaultValue, rd.varType);
            rd.value = unpack(rd.value, rd.varType);
            ret.put(identifier, rd);
        }
        keys = vars[1].keySet().iterator();
        while (keys.hasNext()) {
            String identifier = (String) keys.next();
            HashMap retrieved = (HashMap) vars[1].get(identifier);
            OutcomeDeclaraton od = new OutcomeDeclaraton();
            Iterator components = retrieved.keySet().iterator();
            while (components.hasNext()) {
                String fieldKey = (String) components.next();
                if (fieldKey.equals(identifier)) {
                    od.value = retrieved.get(identifier);
                } else if (fieldKey.equals("identifier")) {
                    Object retObj = retrieved.get(fieldKey);
                    if (retObj != null) {
                        od.identifier = (String) retObj;
                    }
                } else if (fieldKey.equals("cardinality")) {
                    String card = (String) retrieved.get(fieldKey);
                    if (card.equals("single")) od.card = Cardinality.single; else if (card.equals("multiple")) od.card = Cardinality.multiple; else if (card.equals("ordered")) od.card = Cardinality.ordered; else if (card.equals("record")) od.card = Cardinality.record; else throw new ConversionException("Error: unknown cardinality type in conversion, aborting");
                } else if (fieldKey.equals("baseType")) {
                    String baseType = (String) retrieved.get(fieldKey);
                    if (baseType.equals("float")) od.varType = BaseType.afloat; else if (baseType.equals("integer")) od.varType = BaseType.integer; else if (baseType.equals("identifier")) od.varType = BaseType.identifier; else if (baseType.equals("boolean")) od.varType = BaseType.bool; else if (baseType.equals("directedPair")) od.varType = BaseType.directedPair; else if (baseType.equals("duration")) od.varType = BaseType.duration; else if (baseType.equals("point")) od.varType = BaseType.point; else if (baseType.equals("pair")) od.varType = BaseType.pair; else if (baseType.equals("string")) od.varType = BaseType.string; else if (baseType.equals("file")) od.varType = BaseType.file; else if (baseType.equals("uri")) od.varType = BaseType.uri; else throw new ConversionException("Error: unknown cardinality type in conversion, aborting");
                } else if (fieldKey.equals("defaultValue")) {
                    Object defaultValue = retrieved.get(fieldKey);
                    od.defaultValue = defaultValue;
                } else if (fieldKey.equals("defaultValueInterpretation")) {
                    String defaultValueInterpretation = (String) retrieved.get(fieldKey);
                    od.defaultValueInterpretation = defaultValueInterpretation;
                } else if (fieldKey.equals("interpretation")) {
                    String interpretation = (String) retrieved.get(fieldKey);
                    od.interpretation = interpretation;
                } else if (fieldKey.equals("longInterpretation")) {
                    String longInterpretation = (String) retrieved.get(fieldKey);
                    od.longInterpretation = longInterpretation;
                } else if (fieldKey.equals("normalMaximum")) {
                    if (retrieved.get(fieldKey) == null) od.normalMaximum = null; else od.normalMaximum = new Double((String) retrieved.get(fieldKey));
                } else if (fieldKey.equals("normalMinimum")) {
                    if (retrieved.get(fieldKey) == null) od.normalMinimum = null; else od.normalMinimum = new Double((String) retrieved.get(fieldKey));
                }
            }
            od.defaultValue = unpack(od.defaultValue, od.varType);
            od.value = unpack(od.value, od.varType);
            ret.put(identifier, od);
        }
        keys = vars[2].keySet().iterator();
        while (keys.hasNext()) {
            String identifier = (String) keys.next();
            HashMap retrieved = (HashMap) vars[2].get(identifier);
            TemplateDeclaration td = new TemplateDeclaration();
            Iterator components = retrieved.keySet().iterator();
            while (components.hasNext()) {
                String fieldKey = (String) components.next();
                if (fieldKey.equals(identifier)) {
                    td.value = retrieved.get(identifier);
                } else if (fieldKey.equals("identifier")) {
                    Object retObj = retrieved.get(fieldKey);
                    if (retObj != null) {
                        td.identifier = (String) retObj;
                    }
                } else if (fieldKey.equals("cardinality")) {
                    String card = (String) retrieved.get(fieldKey);
                    if (card.equals("single")) td.card = Cardinality.single; else if (card.equals("multiple")) td.card = Cardinality.multiple; else if (card.equals("ordered")) td.card = Cardinality.ordered; else if (card.equals("record")) td.card = Cardinality.record; else throw new ConversionException("Error: unknown cardinality type in conversion, aborting");
                } else if (fieldKey.equals("baseType")) {
                    String baseType = (String) retrieved.get(fieldKey);
                    if (baseType.equals("float")) td.varType = BaseType.afloat; else if (baseType.equals("integer")) td.varType = BaseType.integer; else if (baseType.equals("identifier")) td.varType = BaseType.identifier; else if (baseType.equals("boolean")) td.varType = BaseType.bool; else if (baseType.equals("directedPair")) td.varType = BaseType.directedPair; else if (baseType.equals("duration")) td.varType = BaseType.duration; else if (baseType.equals("point")) td.varType = BaseType.point; else if (baseType.equals("pair")) td.varType = BaseType.pair; else if (baseType.equals("string")) td.varType = BaseType.string; else if (baseType.equals("file")) td.varType = BaseType.file; else if (baseType.equals("uri")) td.varType = BaseType.uri; else throw new ConversionException("Error: unknown cardinality type in conversion, aborting");
                } else if (fieldKey.equals("defaultValue")) {
                    Object defaultValue = retrieved.get(fieldKey);
                    td.defaultValue = defaultValue;
                } else if (fieldKey.equals("defaultValueInterpretation")) {
                    String defaultValueInterpretation = (String) retrieved.get(fieldKey);
                    td.defaultValueInterpretation = defaultValueInterpretation;
                } else if (fieldKey.equals("paramVariable")) {
                    if (retrieved.get(fieldKey) == null) td.paramVariable = null; else td.paramVariable = new Boolean((String) retrieved.get(fieldKey));
                } else if (fieldKey.equals("mathVariable")) {
                    if (retrieved.get(fieldKey) == null) td.mathVariable = null; else td.mathVariable = new Boolean((String) retrieved.get(fieldKey));
                }
            }
            td.defaultValue = unpack(td.defaultValue, td.varType);
            td.value = unpack(td.value, td.varType);
            ret.put(identifier, td);
        }
        return ret;
    }

    private static HashMap[] rePopulateHashArrays(HashMap<String, ItemVariable> vars) throws ConversionException {
        HashMap[] ret = new HashMap[3];
        for (int i = 0; i < 3; i++) ret[i] = new HashMap();
        Iterator it = vars.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            Object test = vars.get(key);
            if (test instanceof ResponseDeclaration) {
                ret[0].put(key, ((ResponseDeclaration) test).toLegacyFormat());
            } else if (test instanceof OutcomeDeclaraton) {
                ret[1].put(key, ((OutcomeDeclaraton) test).toLegacyFormat());
            } else if (test instanceof TemplateDeclaration) {
                ret[2].put(key, ((TemplateDeclaration) test).toLegacyFormat());
            } else throw new ConversionException("Error: in converting the arrays to legacy format an unknown variable type has appreared, aborting");
        }
        return ret;
    }

    private static Vector<MapEntry> unpackMapping(Vector<MapEntry> entries, BaseType type) {
        Vector<MapEntry> rets = new Vector<MapEntry>();
        for (int i = 0; i < entries.size(); i++) {
            String key = (String) entries.elementAt(i).mapKey;
            Double value = entries.elementAt(i).mappedValue;
            if (type == BaseType.integer) {
                rets.add(new MapEntry(new Integer(key), value));
            } else if (type == BaseType.afloat) {
                rets.add(new MapEntry(new Double(key), value));
            } else if (type == BaseType.bool) {
                rets.add(new MapEntry(new Boolean(key), value));
            } else if (type == BaseType.directedPair) {
                StringTokenizer st = new StringTokenizer(key);
                rets.add(new MapEntry(new DirectedPair(st.nextToken(), st.nextToken()), value));
            } else if (type == BaseType.pair) {
                StringTokenizer st = new StringTokenizer(key);
                rets.add(new MapEntry(new Pair(st.nextToken(), st.nextToken()), value));
            } else if (type == BaseType.point) {
                StringTokenizer st = new StringTokenizer(key);
                rets.add(new MapEntry(new Point(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken())), value));
            } else rets.add(entries.elementAt(i));
        }
        return rets;
    }

    private static Object unpack(Object target, BaseType type) {
        Vector<Object> tmp = new Vector<Object>();
        Vector<Object> ret = new Vector<Object>();
        if (target == null) return null;
        if (target instanceof Vector) {
            tmp = (Vector) target;
        } else {
            tmp.add(target);
        }
        for (int i = 0; i < tmp.size(); i++) {
            if (type == BaseType.integer) {
                try {
                    ret.add(new Integer((String) tmp.elementAt(i)));
                } catch (NumberFormatException e) {
                    ret.add(null);
                }
            } else if (type == BaseType.afloat) {
                try {
                    ret.add(new Double((String) tmp.elementAt(i)));
                } catch (NumberFormatException e) {
                    ret.add(null);
                }
            } else if (type == BaseType.bool) {
                try {
                    ret.add(new Boolean((String) tmp.elementAt(i)));
                } catch (NumberFormatException e) {
                    ret.add(null);
                }
            } else if (type == BaseType.directedPair) {
                StringTokenizer st = new StringTokenizer((String) tmp.elementAt(i));
                ret.add(new DirectedPair(st.nextToken(), st.nextToken()));
            } else if (type == BaseType.pair) {
                StringTokenizer st = new StringTokenizer((String) tmp.elementAt(i));
                ret.add(new Pair(st.nextToken(), st.nextToken()));
            } else if (type == BaseType.point) {
                StringTokenizer st = new StringTokenizer((String) tmp.elementAt(i));
                ret.add(new Point(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken())));
            } else if (target instanceof Vector) {
                ret.add(tmp.elementAt(i));
            } else ret.add(target);
        }
        return ret;
    }
}
