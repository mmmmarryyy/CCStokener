package com.abich.eve.evecalc;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.abich.eve.evecalc.alloys.Items;
import com.abich.eve.evecalc.alloys.PriceList;
import com.abich.eve.evecalc.alloys.Region;

public class EVECalcControllerImpl implements EVECalcController {

    private static final String BASEURL = "http://www.abich.com/evecalc";

    private static final String REGION_PROPERTIES = "/com/abich/eve/evecalc/regions.properties";

    private static final String REGIONS_URL = BASEURL + "/regions.properties";

    private static final String SHIPS_URL = BASEURL + "/ships.xml?";

    private EVECalcView view;

    private Properties properties;

    private Items alloys = Items.getInstance();

    private PriceEditor priceEditor;

    private JFrame frame;

    public JFrame getFrame() {
        if (frame == null) {
            frame = new JFrame();
        }
        return frame;
    }

    public EVECalcControllerImpl(EVECalcView gui) {
        this.view = gui;
        properties = new Properties();
        try {
            InputStream resStream;
            resStream = getClass().getResourceAsStream(REGION_PROPERTIES);
            if (resStream == null) {
                System.out.println("Loading for needed Properties files failed.");
                URL url = new URL(REGIONS_URL);
                try {
                    resStream = url.openStream();
                    properties.load(resStream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                properties.load(resStream);
            }
        } catch (IOException e) {
        }
    }

    public void showPriceEditor() {
        if (priceEditor == null) {
            priceEditor = new PriceEditor(getFrame(), this);
        }
        priceEditor.setVisible(true);
    }

    public void calcFuelUse() {
        JTextField massField = view.getMass();
        JTextField fuelField = view.getLiquidOzone();
        JTextField distanceField = view.getDistance();
        if (massField == null || fuelField == null || distanceField == null) {
            throw new InitializationException();
        }
        String massString = massField.getText() == null ? "0" : massField.getText();
        Double mass;
        try {
            mass = new Double(massString);
        } catch (NumberFormatException e) {
            mass = new Double(0);
        }
        double distance;
        try {
            distance = new Double(distanceField.getText() == null ? "5" : distanceField.getText());
        } catch (NumberFormatException e) {
            distance = 5;
        }
        view.getDistance().setText(new Long(Math.round(distance)).toString());
        double fuelNeeded = mass / 1000000000 * 500 * (distance == 0 ? 5 : distance);
        fuelField.setText(new Long(Math.round(fuelNeeded)).toString());
    }

    public void createNodes(DefaultMutableTreeNode top) {
        createNodesByXML(top);
    }

    public void createNodesByXML(DefaultMutableTreeNode top) {
        Thread task = new Task(top);
        task.start();
    }

    private void createTree(DefaultMutableTreeNode top) throws MalformedURLException, ParserConfigurationException, SAXException, IOException {
        InputStream stream;
        URL url = new URL(SHIPS_URL + view.getBaseurl());
        try {
            stream = url.openStream();
        } catch (Exception e) {
            stream = getClass().getResourceAsStream("ships.xml");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document doc = parser.parse(stream);
        NodeList races = doc.getElementsByTagName("race");
        for (int i = 0; i < races.getLength(); i++) {
            Element race = (Element) races.item(i);
            top.add(buildRaceTree(race));
        }
        top.setUserObject("Ships");
        view.getShipTree().repaint();
        view.getShipTree().expandRow(0);
    }

    private DefaultMutableTreeNode buildRaceTree(Element race) {
        DefaultMutableTreeNode raceNode = new DefaultMutableTreeNode(race.getAttribute("name"));
        NodeList categories = race.getElementsByTagName("category");
        for (int j = 0; j < categories.getLength(); j++) {
            Element category = (Element) categories.item(j);
            raceNode.add(buildCategoryTree(category));
        }
        return raceNode;
    }

    private DefaultMutableTreeNode buildCategoryTree(Element category) {
        DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(category.getAttribute("name"));
        NodeList ships = category.getElementsByTagName("ship");
        for (int k = 0; k < ships.getLength(); k++) {
            Element ship = (Element) ships.item(k);
            DefaultMutableTreeNode shipNode = buildShip(ship);
            categoryNode.add(shipNode);
        }
        return categoryNode;
    }

    private DefaultMutableTreeNode buildShip(Element ship) {
        TreeNodeEntry treeNodeEntry = new TreeNodeEntry(ship.getAttribute("name"), new Long(ship.getAttribute("mass")));
        DefaultMutableTreeNode shipNode = new DefaultMutableTreeNode(treeNodeEntry);
        return shipNode;
    }

    public void setMassByTree(TreeNodeEntry entry) {
        JTextField massField = view.getMass();
        JTree tree = view.getShipTree();
        if (massField == null || tree == null) {
            throw new InitializationException();
        }
        massField.setText(entry.getMass().toString());
    }

    private class Task extends Thread implements Runnable {

        private DefaultMutableTreeNode tree;

        public Task(DefaultMutableTreeNode tree) {
            this.tree = tree;
        }

        public void run() {
            try {
                System.out.println("Starting Thread");
                createTree(tree);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Thread finished");
        }
    }

    public ListModel getRegions() {
        DefaultListModel list = new DefaultListModel();
        Vector<String> regions = splitProperty();
        for (Iterator iter = regions.iterator(); iter.hasNext(); ) {
            String region = (String) iter.next();
            list.addElement(new Region(properties.getProperty("region." + region + ".name", ""), properties.getProperty("region." + region + ".id", "")));
        }
        return list;
    }

    private Vector<String> splitProperty() {
        String propRegions = properties.getProperty("regions");
        Vector<String> regs = propSplit(propRegions);
        return regs;
    }

    /**
	 * @param propRegions
	 * @return
	 */
    private Vector<String> propSplit(String propRegions) {
        Vector<String> regs = new Vector<String>();
        if (propRegions != null) {
            int comma = propRegions.indexOf(",");
            if (comma > -1) {
                String reg = propRegions.substring(0, comma);
                String rest = propRegions.substring(comma + 1);
                regs.add(reg);
                if (rest.length() > 0) {
                    regs.addAll(propSplit(rest));
                }
            } else {
                regs.add(propRegions);
            }
        }
        return regs;
    }

    public void loadRegionPrices(Region region) {
        alloys.loadRegionPrices(region);
    }

    public PriceList getPriceList(Region region) {
        return alloys.getPriceList(region);
    }

    public EVECalcView getMainView() {
        return view;
    }
}
