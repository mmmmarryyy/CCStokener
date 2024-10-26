package guineu.modules.mylly.filter.pubChem.GolmIdentification;

import guineu.data.Dataset;
import guineu.data.DatasetType;
import guineu.data.PeakListRow;
import guineu.data.GCGCColumnName;
import guineu.data.impl.datasets.SimpleGCGCDataset;
import guineu.taskcontrol.AbstractTask;
import guineu.taskcontrol.TaskStatus;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author scsandra
 */
public class GetGolmIDsTask extends AbstractTask {

    private Dataset dataset;

    private double progress = 0.0;

    public GetGolmIDsTask(Dataset dataset) {
        this.dataset = dataset;
    }

    public String getTaskDescription() {
        return "Filtering files with Name Identifiacion Filter... ";
    }

    public double getFinishedPercentage() {
        return progress;
    }

    public void cancel() {
        setStatus(TaskStatus.CANCELED);
    }

    public void run() {
        setStatus(TaskStatus.PROCESSING);
        if (dataset.getType() != DatasetType.GCGCTOF) {
            setStatus(TaskStatus.ERROR);
            errorMessage = "Wrong data set type. This module is for the ID identification in GCxGC-MS data";
            return;
        } else {
            try {
                actualMap((SimpleGCGCDataset) dataset);
                setStatus(TaskStatus.FINISHED);
            } catch (Exception ex) {
                Logger.getLogger(GetGolmIDsTask.class.getName()).log(Level.SEVERE, null, ex);
                setStatus(TaskStatus.ERROR);
            }
        }
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        synchronized (in) {
            synchronized (out) {
                byte[] buffer = new byte[256];
                while (true) {
                    int bytesRead = in.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    protected void actualMap(SimpleGCGCDataset input) throws Exception {
        int numRows = input.getNumberRows();
        int count = 0;
        for (PeakListRow row : input.getAlignment()) {
            if (getStatus() == TaskStatus.CANCELED) {
                break;
            }
            if (row.getName().contains("Unknown")) {
                count++;
                continue;
            }
            try {
                String name = (String) row.getVar(GCGCColumnName.CAS.getGetFunctionName());
                int score = 0;
                if (!name.contains("0-00-0") && name.length() > 0) {
                    score = addIDs(row, name);
                }
                String pubChemID = (String) row.getVar(GCGCColumnName.PUBCHEM.getGetFunctionName());
                if (score < 998) {
                    row.setVar(GCGCColumnName.PUBCHEM.getSetFunctionName(), "");
                    row.setVar(GCGCColumnName.ChEBI.getSetFunctionName(), "");
                    row.setVar(GCGCColumnName.CAS2.getSetFunctionName(), "");
                    row.setVar(GCGCColumnName.KEGG.getSetFunctionName(), "");
                    row.setVar(GCGCColumnName.SYNONYM.getSetFunctionName(), "");
                    row.setVar(GCGCColumnName.MOLWEIGHT.getSetFunctionName(), "0.0");
                }
                if (pubChemID.length() == 0) {
                    name = row.getName();
                    name = name.replaceAll(" ", "+");
                    name = name.replaceAll(",", "%2c");
                    if (name.length() > 0) {
                        addIDs(row, name);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            count++;
            progress = (double) count / numRows;
        }
    }

    private int addIDs(PeakListRow row, String name) {
        {
            BufferedReader in = null;
            try {
                String urlName = "http://gmd.mpimp-golm.mpg.de/search.aspx?query=" + name;
                URL url = new URL(urlName);
                in = new BufferedReader(new InputStreamReader(url.openStream()));
                String inputLine, score = "0";
                while ((inputLine = in.readLine()) != null) {
                    String metaboliteID = "";
                    if (inputLine.contains("href=\"Metabolites/")) {
                        String[] dataScore = inputLine.split("</td><td>");
                        score = dataScore[0].substring(dataScore[0].indexOf("<td>") + 4);
                        metaboliteID = inputLine.substring(inputLine.indexOf("href=\"Metabolites/") + 18, inputLine.indexOf("aspx\">") + 4);
                        urlName = "http://gmd.mpimp-golm.mpg.de/Metabolites/" + metaboliteID;
                        inputLine = in.readLine();
                        inputLine = in.readLine();
                        String[] data = inputLine.split("</td><td>");
                        String molecularWeight = data[data.length - 1].replaceAll("&nbsp;", "");
                        row.setVar(GCGCColumnName.MOLWEIGHT.getSetFunctionName(), molecularWeight);
                        break;
                    } else if (inputLine.contains("href=\"Analytes/")) {
                        String[] dataScore = inputLine.split("</td><td>");
                        score = dataScore[0].substring(dataScore[0].indexOf("<td>") + 4);
                        metaboliteID = inputLine.substring(inputLine.indexOf("href=\"Analytes/") + 15, inputLine.indexOf("aspx\">") + 4);
                        urlName = "http://gmd.mpimp-golm.mpg.de/Analytes/" + metaboliteID;
                        inputLine = in.readLine();
                        inputLine = in.readLine();
                        String[] data = inputLine.split("</td><td>");
                        String molecularWeight = data[data.length - 1].replaceAll("&nbsp;", "");
                        row.setVar(GCGCColumnName.MOLWEIGHT.getSetFunctionName(), molecularWeight);
                        break;
                    } else if (inputLine.contains("href=\"ReferenceSubstances/")) {
                        String[] dataScore = inputLine.split("</td><td>");
                        score = dataScore[0].substring(dataScore[0].indexOf("<td>") + 4);
                        metaboliteID = inputLine.substring(inputLine.indexOf("href=\"ReferenceSubstances/") + 26, inputLine.indexOf("aspx\">") + 4);
                        urlName = "http://gmd.mpimp-golm.mpg.de/ReferenceSubstances/" + metaboliteID;
                        inputLine = in.readLine();
                        inputLine = in.readLine();
                        String[] data = inputLine.split("</td><td>");
                        String molecularWeight = data[data.length - 1].replaceAll("&nbsp;", "");
                        row.setVar(GCGCColumnName.MOLWEIGHT.getSetFunctionName(), molecularWeight);
                        break;
                    }
                }
                in.close();
                urlName = searchMetabolite(urlName);
                if (urlName != null && urlName.contains(".aspx")) {
                    url = new URL(urlName);
                    in = new BufferedReader(new InputStreamReader(url.openStream()));
                    while ((inputLine = in.readLine()) != null) {
                        if (inputLine.contains("<meta http-equiv=\"keywords\" content=")) {
                            String line = inputLine.substring(inputLine.indexOf("<meta http-equiv=\"keywords\" content=") + 37, inputLine.indexOf("\" /></head>"));
                            String[] names = line.split(", ");
                            for (String id : names) {
                                if (id.contains("PubChem")) {
                                    id = id.substring(id.indexOf("PubChem") + 8);
                                    String pubChem = (String) row.getVar(GCGCColumnName.PUBCHEM.getGetFunctionName());
                                    if (pubChem.length() == 0) {
                                        pubChem += id;
                                    } else {
                                        pubChem += ", " + id;
                                    }
                                    row.setVar(GCGCColumnName.PUBCHEM.getSetFunctionName(), pubChem);
                                } else if (id.contains("ChEBI")) {
                                    id = id.substring(id.indexOf("ChEBI:") + 6);
                                    row.setVar(GCGCColumnName.ChEBI.getSetFunctionName(), id);
                                } else if (id.contains("KEGG")) {
                                    id = id.substring(id.indexOf("KEGG:") + 6);
                                    row.setVar(GCGCColumnName.KEGG.getSetFunctionName(), id);
                                } else if (id.contains("CAS")) {
                                    id = id.substring(id.indexOf("CAS:") + 5);
                                    row.setVar(GCGCColumnName.CAS2.getSetFunctionName(), id);
                                } else if (id.contains("ChemSpider") || id.contains("MAPMAN") || id.contains("Beilstein:")) {
                                } else {
                                    String synonym = (String) row.getVar(GCGCColumnName.SYNONYM.getGetFunctionName());
                                    if (synonym.length() == 0) {
                                        synonym += id;
                                    } else {
                                        synonym += " // " + id;
                                    }
                                    synonym = synonym.replaceAll("&amp;#39;", "'");
                                    row.setVar(GCGCColumnName.SYNONYM.getSetFunctionName(), synonym);
                                }
                            }
                            break;
                        }
                    }
                    in.close();
                }
                return Integer.parseInt(score);
            } catch (IOException ex) {
                Logger.getLogger(GetGolmIDsTask.class.getName()).log(Level.SEVERE, null, ex);
                return 0;
            }
        }
    }

    public String getName() {
        return "Filter IDs Identification";
    }

    private String searchMetabolite(String name) {
        {
            BufferedReader in = null;
            try {
                String urlName = name;
                URL url = new URL(urlName);
                in = new BufferedReader(new InputStreamReader(url.openStream()));
                String inputLine;
                Boolean isMetabolite = false;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.contains("Metabolite</h1>")) {
                        isMetabolite = true;
                    }
                    if (inputLine.contains("<td><a href=\"/Metabolites/") && isMetabolite) {
                        String metName = inputLine.substring(inputLine.indexOf("/Metabolites/") + 13, inputLine.indexOf("aspx\" target") + 4);
                        return "http://gmd.mpimp-golm.mpg.de/Metabolites/" + metName;
                    }
                }
                in.close();
                return name;
            } catch (IOException ex) {
                Logger.getLogger(GetGolmIDsTask.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
    }
}
