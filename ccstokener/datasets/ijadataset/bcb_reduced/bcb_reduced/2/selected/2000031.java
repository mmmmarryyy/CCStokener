package jp.riken.omicspace.osml.impl;

import java.util.*;
import java.io.*;
import java.net.*;
import java.util.regex.Pattern;

/**
 *  Clex??
 *  cluster, experiment?
 */
public class ClexDocument {

    private static final Pattern tabPattern = Pattern.compile("\t");

    private static final Pattern commaPattern = Pattern.compile(",");

    private ArrayList vOmicElement = new ArrayList();

    private ArrayList vColumn = new ArrayList();

    public ClexDocument() {
    }

    /** doesn't just set the container.
     *  This is a faster version than the original, but hasn't been fully checked
     *  Uses getMapValues to reduce # of calls toUpperCase and Split, but a space
     *  is inserted before some elements, this should be the same action as occurs
     *  with the previous split method anyway.
     */
    public void setOsmlContainer(OsmlContainer osml) {
        System.out.println("ClexDocument.setOsmlContainer");
        OsmlContainer clex = getOsmlContainer();
        ArrayList<OmicElementContainer> clexElementArray = clex.getAllOmicElementContainers();
        ArrayList<OmicElementContainer> omicElementArray = osml.getAllOmicElementContainers();
        Hashtable<String, OmicElementContainer> clexElementHashTable = new Hashtable<String, OmicElementContainer>();
        for (OmicElementContainer clexElement : clexElementArray) {
            String name = clexElement.getName();
            if (name.length() == 0) continue;
            String names[] = commaPattern.split(name);
            for (int j = 0; j < names.length; j++) {
                if (names[j].length() == 0) {
                    continue;
                }
                clexElementHashTable.put(names[j], clexElement);
            }
        }
        LinkedHashSet<OmicElementContainer> omicElementLinkedSet = new LinkedHashSet<OmicElementContainer>();
        omicElementLinkedSet.addAll(omicElementArray);
        int nCount = 0;
        for (OmicElementContainer osmlElement : omicElementLinkedSet) {
            String name = osmlElement.getName();
            if (name.length() == 0) continue;
            String apdx = osmlElement.getAppendix();
            String names[] = commaPattern.split(name);
            String apdxs[] = commaPattern.split(apdx);
            ArrayList<String> keys = new ArrayList<String>();
            ArrayList<String> values = new ArrayList<String>();
            getMapValues(keys, values, name, osmlElement.getNote());
            nCount = copyElements(nCount, osmlElement, clexElementHashTable, names, keys, values);
            nCount = copyElements(nCount, osmlElement, clexElementHashTable, apdxs, keys, values);
        }
        System.out.println("match cdtx " + nCount + " items");
        DatasetContainer dataset = clex.getDatasetContainer(0);
        Collection<DatasetContainer> osmlDatasets = osml.getAllDatasetContainers();
        if (dataset != null) {
            Collection<ExperimentContainer> experiments = dataset.getAllExperimentContainers();
            for (DatasetContainer datasetContainer : osmlDatasets) {
                for (ExperimentContainer experimentContainer : experiments) {
                    datasetContainer.addExperimentContainer(experimentContainer);
                }
            }
        }
    }

    /** newer version, hopefully faster
     */
    private int copyElements(int nCount, OmicElementContainer osmlElement, Hashtable<String, OmicElementContainer> clexElementHashTable, String[] array, ArrayList<String> keys, ArrayList<String> values) {
        for (int j = 0; j < array.length; j++) {
            OmicElementContainer clexElement = (OmicElementContainer) clexElementHashTable.get(array[j]);
            if (clexElement != null) {
                for (int k = 0; k < clexElement.getAmountContainerSize(); k++) {
                    AmountContainer amount = clexElement.getAmountContainer(k);
                    osmlElement.addAmountContainer(amount);
                }
                for (int k = 0; k < clexElement.getPropertyContainerSize(); k++) {
                    PropertyContainer property = clexElement.getPropertyContainer(k);
                    osmlElement.addPropertyContainer(property);
                }
                clexElement.addRelatedInfo(keys, values);
                nCount++;
            }
        }
        return nCount;
    }

    private int copyElements(int nCount, OmicElementContainer osmlElement, Hashtable<String, OmicElementContainer> clexElementHashTable, String[] array) {
        for (int j = 0; j < array.length; j++) {
            OmicElementContainer clexElement = (OmicElementContainer) clexElementHashTable.get(array[j]);
            if (clexElement != null) {
                for (int k = 0; k < clexElement.getAmountContainerSize(); k++) {
                    AmountContainer amount = clexElement.getAmountContainer(k);
                    osmlElement.addAmountContainer(amount);
                }
                for (int k = 0; k < clexElement.getPropertyContainerSize(); k++) {
                    PropertyContainer property = clexElement.getPropertyContainer(k);
                    osmlElement.addPropertyContainer(property);
                }
                clexElement.addRelatedInfo(osmlElement.getName());
                clexElement.addRelatedInfo(osmlElement.getNote());
                nCount++;
            }
        }
        return nCount;
    }

    /**
     *  Can't be sure whether having uppercase keys is necessary or whether ok
     *  to have names in all uppercase, so this method creates an array of uppercase
     *  keys and an array of original case values.
     */
    public static void getMapValues(ArrayList<String> keys, ArrayList<String> values, String... input) {
        int firstIndex, length;
        int index;
        for (String inputString : input) {
            String upper = inputString.toUpperCase();
            firstIndex = 0;
            length = inputString.length();
            for (index = 0; index < length; index++) {
                if (inputString.charAt(index) == ',') {
                    if (index > firstIndex) {
                        values.add(inputString.substring(firstIndex, index));
                        keys.add(upper.substring(firstIndex, index));
                        firstIndex = index + 1;
                    }
                }
            }
            if (index == length) {
                values.add(inputString.substring(firstIndex, index));
                keys.add(upper.substring(firstIndex, index));
            }
        }
    }

    /**
     *  createOSMLSOMContainer? copy?
     */
    public OsmlContainer getOsmlContainer() {
        System.out.println("ClexDocument.getOsmlContainer");
        OsmlContainer osmlContainer = new OsmlContainer();
        DatasetContainer dataset = (DatasetContainer) DatasetContainer.createContainer();
        dataset.setName("SOM Cluster");
        dataset.setNote("");
        osmlContainer.addDatasetContainer(dataset);
        for (int j = 0; j < vColumn.size(); j++) {
            String name = (String) vColumn.get(j);
            ExperimentContainer exp = (ExperimentContainer) ExperimentContainer.createContainer();
            exp.setName(name);
            exp.setNote("");
            exp.setId("ex" + j);
            dataset.addExperimentContainer(exp);
        }
        Hashtable hCluster = new Hashtable(vOmicElement.size());
        for (int i = 0; i < vOmicElement.size(); i++) {
            OmicElementContainer element = (OmicElementContainer) vOmicElement.get(i);
            String sx = element.getAttribute("clusterX");
            String sy = element.getAttribute("clusterY");
            String key = sx + "-" + sy;
            if (!hCluster.containsKey(key)) {
                FunctionalClassContainer function = (FunctionalClassContainer) FunctionalClassContainer.createContainer();
                function.setName("Cluster:" + key);
                function.setId("Clst" + key);
                hCluster.put(key, function);
                dataset.addFunctionalClassContainer(function);
            }
            FunctionalClassContainer function = (FunctionalClassContainer) hCluster.get(key);
            function.addOmicElementContainer(element);
        }
        return osmlContainer;
    }

    public boolean loadFile(String inpfile) {
        vColumn.clear();
        Hashtable hData = new Hashtable();
        String colGreater = ">COLUMN=";
        String delimiters = "=\n\r\t";
        char greater = '>';
        char tab = '\t';
        String ex = "ex";
        String emptyString = "";
        try {
            BufferedReader in = null;
            if (inpfile.indexOf("http://") >= 0) {
                URL url = null;
                url = new URL(inpfile);
                URLConnection conn = url.openConnection();
                conn.setUseCaches(false);
                InputStreamReader is = new InputStreamReader(conn.getInputStream());
                in = new BufferedReader(is);
            } else {
                in = new BufferedReader(new FileReader(inpfile));
            }
            String pline = null;
            while ((pline = in.readLine()) != null) {
                int nLen = pline.length();
                if (nLen == 0) continue;
                if (pline.charAt(0) == greater) {
                    if (pline.indexOf(colGreater) >= 0) {
                        StringTokenizer stk = new StringTokenizer(pline, delimiters);
                        int n = stk.countTokens();
                        if (n >= 2) {
                            String s0 = stk.nextToken();
                            String s1 = stk.nextToken();
                            vColumn.add(s1);
                        }
                    }
                } else {
                    int nColm = vColumn.size();
                    if (nColm <= 0) {
                        System.out.println("less than 0 nColm");
                        continue;
                    }
                    if (nLen == 0) continue;
                    String[] tokens = tabPattern.split(pline);
                    if (tokens.length < 5) {
                        System.out.println("less than 5 vTok");
                        continue;
                    }
                    OmicElementContainer gene = new OmicElementContainer();
                    gene.setId(gene.getNewId());
                    if (tokens[0].length() > 0) {
                        gene.setName(tokens[0]);
                    }
                    if (tokens[1].length() > 0) {
                        gene.setNote(tokens[1]);
                    }
                    if (tokens[2].length() > 0) {
                        gene.setAttribute("clusterX", tokens[2]);
                    }
                    if (tokens[3].length() > 0) {
                        gene.setAttribute("clusterY", tokens[3]);
                    }
                    for (int i = 0; i < nColm; i++) {
                        if (tokens[i + 5].length() > 0) {
                            AmountContainer amount = new AmountContainer();
                            amount.setRefId(ex + i);
                            float value = Float.parseFloat(tokens[i + 5]);
                            amount.setValue(Float.toString(value));
                            gene.addAmountContainer(amount);
                        }
                    }
                    if (tokens[0].length() > 0 && !hData.containsKey(tokens[0])) {
                        vOmicElement.add(gene);
                        hData.put(tokens[0], gene);
                    }
                }
            }
            in.close();
        } catch (MalformedURLException mfe) {
            System.out.println("MalformedURLException");
            return false;
        } catch (IOException ioe) {
            System.out.println("IOException");
            return false;
        }
        return true;
    }

    public void clear() {
    }
}
