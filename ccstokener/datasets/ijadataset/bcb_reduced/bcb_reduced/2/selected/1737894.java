package uk.ac.lkl.migen.mockup.shapebuilder;

import java.lang.reflect.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.lkl.migen.mockup.shapebuilder.ui.ExpressionPalette;
import uk.ac.lkl.migen.mockup.shapebuilder.ui.ShapePlotter;
import uk.ac.lkl.migen.mockup.shapebuilder.model.shape.ShapeMaker;
import uk.ac.lkl.migen.mockup.shapebuilder.ui.tool.expression.ExpressionTool;
import uk.ac.lkl.migen.mockup.shapebuilder.ui.tool.shape.ShapeTool;

public class Configuration {

    private List<Constructor<?>> expressionToolConstructors;

    private List<Constructor<?>> shapeToolConstructors;

    private List<ShapeMaker> shapeMakers;

    private HashMap<String, String> parameterMap;

    private static Configuration configuration;

    public static Configuration getConfiguration() {
        if (configuration == null) throw new RuntimeException("Configuration not set");
        return configuration;
    }

    private Configuration() {
    }

    public static Configuration readConfiguration(URL url) {
        try {
            InputStream stream = url.openStream();
            return readConfiguration(stream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't open stream to URL");
        }
    }

    public static Configuration readConfiguration(File file) {
        try {
            InputStream stream = new FileInputStream(file);
            return readConfiguration(stream);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Can't find file");
        }
    }

    public static Configuration readConfiguration(InputStream stream) {
        configuration = new Configuration();
        configuration.processStream(stream);
        return configuration;
    }

    private void processStream(InputStream stream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(stream);
            NodeList list = document.getElementsByTagName("ExpressionTools");
            if (list.getLength() != 1) throw new IllegalArgumentException("Configuration should have a single ExpressionTools block");
            Node expressionToolsNode = list.item(0);
            processExpressionTools((Element) expressionToolsNode);
            list = document.getElementsByTagName("ShapeTools");
            if (list.getLength() != 1) throw new IllegalArgumentException("Configuration should have a single ShapeTools block");
            Node shapeToolsNode = list.item(0);
            processShapeTools((Element) shapeToolsNode);
            list = document.getElementsByTagName("ShapeMakers");
            if (list.getLength() != 1) throw new IllegalArgumentException("Configuration should have a single ShapeMakers block");
            Node shapeMakersNode = list.item(0);
            processShapeMakers((Element) shapeMakersNode);
            list = document.getElementsByTagName("Parameters");
            if (list.getLength() != 1) throw new IllegalArgumentException("Configuration should have a single Parameters block");
            Node parametersNode = list.item(0);
            processParameters((Element) parametersNode);
        } catch (ParserConfigurationException e) {
            throw new IllegalArgumentException("Unable to create factory");
        } catch (SAXException e) {
            throw new IllegalArgumentException("Unable to parse xml");
        } catch (IOException e) {
            throw new IllegalArgumentException("Problem reading configuration");
        }
    }

    private void processShapeMakers(Element element) {
        this.shapeMakers = new ArrayList<ShapeMaker>();
        String packageName = getClass().getPackage().getName();
        NodeList list = element.getElementsByTagName("ShapeMaker");
        int numChildren = list.getLength();
        for (int i = 0; i < numChildren; i++) {
            Node child = list.item(i);
            processShapeMaker(packageName, (Element) child);
        }
    }

    private void processShapeMaker(String packageName, Element element) {
        String shapeMakerName = element.getAttribute("name");
        String className = packageName + ".model.shape.maker." + shapeMakerName + "ShapeMaker";
        try {
            Class<?> shapeMakerClass = Class.forName(className);
            ShapeMaker shapeMaker = (ShapeMaker) shapeMakerClass.newInstance();
            shapeMakers.add(shapeMaker);
        } catch (ClassNotFoundException e) {
            System.out.println(e);
        } catch (InstantiationException e) {
            System.out.println(e);
        } catch (IllegalAccessException e) {
            System.out.println(e);
        }
    }

    private void processShapeTools(Element element) {
        this.shapeToolConstructors = new ArrayList<Constructor<?>>();
        String packageName = getClass().getPackage().getName();
        NodeList list = element.getElementsByTagName("ShapeTool");
        int numChildren = list.getLength();
        for (int i = 0; i < numChildren; i++) {
            Node child = list.item(i);
            processShapeTool(packageName, (Element) child);
        }
    }

    private void processShapeTool(String packageName, Element element) {
        String toolName = element.getAttribute("name");
        String className = packageName + ".ui.tool.shape." + toolName + "ShapeTool";
        try {
            Class<?> toolClass = Class.forName(className);
            Constructor<?> constructor = toolClass.getConstructor(ShapePlotter.class);
            shapeToolConstructors.add(constructor);
        } catch (ClassNotFoundException e) {
            System.out.println(e);
        } catch (NoSuchMethodException e) {
            System.out.println(e);
        }
    }

    private void processExpressionTools(Element element) {
        this.expressionToolConstructors = new ArrayList<Constructor<?>>();
        String packageName = getClass().getPackage().getName();
        NodeList list = element.getElementsByTagName("ExpressionTool");
        int numChildren = list.getLength();
        for (int i = 0; i < numChildren; i++) {
            Node child = list.item(i);
            processExpressionTool(packageName, (Element) child);
        }
    }

    private void processExpressionTool(String packageName, Element element) {
        String toolName = element.getAttribute("name");
        String className = packageName + ".ui.tool.expression." + toolName + "Tool";
        try {
            Class<?> toolClass = Class.forName(className);
            Constructor<?> constructor = toolClass.getConstructor(ExpressionPalette.class);
            expressionToolConstructors.add(constructor);
        } catch (ClassNotFoundException e) {
            System.out.println(e);
        } catch (NoSuchMethodException e) {
            System.out.println(e);
        }
    }

    private void processParameters(Element element) {
        this.parameterMap = new HashMap<String, String>();
        NodeList list = element.getElementsByTagName("Parameter");
        int numChildren = list.getLength();
        for (int i = 0; i < numChildren; i++) {
            Node child = list.item(i);
            processParameter((Element) child);
        }
    }

    private void processParameter(Element element) {
        String name = element.getAttribute("name");
        String value = element.getAttribute("value");
        parameterMap.put(name, value);
    }

    public String getStringParameter(String name) {
        return parameterMap.get(name);
    }

    public boolean getBooleanParameter(String name) {
        return Boolean.parseBoolean(getStringParameter(name));
    }

    public ArrayList<ExpressionTool> createExpressionTools(ExpressionPalette palette) {
        ArrayList<ExpressionTool> expressionTools = new ArrayList<ExpressionTool>();
        for (Constructor<?> expressionToolConstructor : expressionToolConstructors) {
            try {
                Object expressionTool = expressionToolConstructor.newInstance(palette);
                expressionTools.add((ExpressionTool) expressionTool);
            } catch (InstantiationException e) {
                System.out.println(e);
            } catch (IllegalAccessException e) {
                System.out.println(e);
            } catch (InvocationTargetException e) {
                System.out.println(e);
            }
        }
        return expressionTools;
    }

    public ArrayList<ShapeTool> createShapeTools(ShapePlotter shapePlotter) {
        ArrayList<ShapeTool> shapeTools = new ArrayList<ShapeTool>();
        for (Constructor<?> shapeToolConstructor : shapeToolConstructors) {
            try {
                Object shapeTool = shapeToolConstructor.newInstance(shapePlotter);
                shapeTools.add((ShapeTool) shapeTool);
            } catch (InstantiationException e) {
                System.out.println(e);
            } catch (IllegalAccessException e) {
                System.out.println(e);
            } catch (InvocationTargetException e) {
                System.out.println(e);
            }
        }
        return shapeTools;
    }

    public List<ShapeMaker> getShapeMakers() {
        return Collections.unmodifiableList(shapeMakers);
    }
}
