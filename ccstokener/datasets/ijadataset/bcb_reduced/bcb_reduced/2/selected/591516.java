package sequime.IO.DatabaseReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "DatabaseReader" Node. reads chosen
 * sequence files from common database sites
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Fabian
 */
public class DatabaseReaderNodeDialog extends DefaultNodeSettingsPane {

    DialogComponentNumber accessionIndex;

    /**
	 * New pane for configuring DatabaseReader node dialog. This is just a
	 * suggestion to demonstrate possible default dialog components.
	 */
    protected DatabaseReaderNodeDialog() {
        super();
        final SettingsModelString database = DatabaseReaderNodeModel.createSettingsModelDatabase();
        final SettingsModelString filename = DatabaseReaderNodeModel.createSettingsModelFilename();
        DialogComponentStringSelection source = new DialogComponentStringSelection(database, "Choose source", "EBI - eukaryota", "EBI - bacteria", "EBI - archaea", "EBI - virus", "EBI - phage", "EBI - viroid", "EBI - plasmid", "EBI - organelle", "UNIPROT");
        final Vector<String> descs = new Vector<String>();
        final Vector<String> links = new Vector<String>();
        reademi(descs, links, "http://www.ebi.ac.uk/genomes/eukaryota.html", "http://www.ebi.ac.uk/genomes/eukaryota.details.txt");
        final DialogComponentStringSelection file = new DialogComponentStringSelection(filename, "Choose file", descs);
        file.setSizeComponents(450, 22);
        filename.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                if (database.getStringValue().startsWith("EBI")) {
                    DatabaseReaderNodeModel.m_link = links.get(descs.indexOf(filename.getStringValue()));
                }
            }
        });
        database.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                if (database.getStringValue().startsWith("EBI")) {
                    descs.clear();
                    links.clear();
                    reademi(descs, links, "http://www.ebi.ac.uk/genomes/" + database.getStringValue().substring(6) + ".html", "http://www.ebi.ac.uk/genomes/" + database.getStringValue().substring(6) + ".details.txt");
                    file.replaceListItems(descs, null);
                } else if (database.getStringValue() == "UNIPROT") {
                    descs.clear();
                    links.clear();
                    readUNI(descs, "http://www.uniprot.org/taxonomy/?query=complete%3ayes&force=yes&format=tab");
                    file.replaceListItems(descs, null);
                } else {
                }
            }
        });
        addDialogComponent(source);
        addDialogComponent(file);
    }

    /**
	 * reads tab delimited list of complete genomes from ebi
	 * and assigns the given Vectors information about the genome names
	 * and the download links for the genomes.
	 * This method accesses 2 different files, a tab delimited txt file(idmap) in order
	 * to get accession numbers and a html version of the genome list to get
	 * the genome download links belonging to the accession numbers.
	 * @param  descriptions  String Vector where the genome names and ids are stored
	 * @param  links String Vector where the download links for the genomes are stored
	 * @param  linkaddress address to the location of the html version of the genome list
     * @param  idmap address to the tab delimited version of the genome list
	 */
    public void reademi(Vector<String> descriptions, Vector<String> links, String linkaddress, String idmap) {
        InputStream is = null;
        URL url;
        ArrayList<String> keys = new ArrayList<String>();
        ArrayList<String> names = new ArrayList<String>();
        try {
            url = new URL(idmap);
            is = url.openStream();
            Scanner scanner = new Scanner(is);
            scanner.nextLine();
            String line = "";
            String id = "";
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                Scanner linescanner = new Scanner(line);
                linescanner.useDelimiter("\t");
                id = linescanner.next();
                id = id.substring(0, id.length() - 2);
                keys.add(id);
                linescanner.next();
                linescanner.next();
                linescanner.next();
                linescanner.useDelimiter("\n");
                names.add(linescanner.next());
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(linkaddress).openStream()));
            String link = "";
            String key = "";
            String name = "";
            int counter = 0;
            while ((line = reader.readLine()) != null) {
                if (line.indexOf("style=raw") != -1) {
                    int linkstart = line.indexOf("http://www.ebi.ac.uk/cgi-bin/dbfetch?db");
                    int idstart = line.indexOf("id=") + 3;
                    int linkend = line.substring(linkstart).indexOf("\"") + linkstart;
                    link = line.substring(linkstart, linkend);
                    key = line.substring(idstart, linkend);
                    if (keys.indexOf(key) != -1) {
                        name = names.get(keys.indexOf(key));
                        counter++;
                        descriptions.add(counter + " " + key + " " + name);
                        links.add(link);
                    }
                }
            }
        } catch (MalformedURLException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * reads tab delimited list of complete genomes from uniprot
	 * and assigns the given Vector information about the genome names
	 * @param  descriptions  String Vector where the genome names and ids are stored
	 * @param  source String of the address of the tab delimited list
	*/
    public void readUNI(Vector<String> descriptions, String source) {
        InputStream is = null;
        try {
            URL url = new URL(source);
            is = url.openStream();
            Scanner scanner = new Scanner(is);
            scanner.nextLine();
            String line = "";
            String id = "";
            String desc = "";
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                Scanner linescanner = new Scanner(line);
                linescanner.useDelimiter("\t");
                id = linescanner.next();
                linescanner.next();
                desc = linescanner.next();
                linescanner.useDelimiter("\n");
                linescanner.next();
                descriptions.add(id + " " + desc);
            }
        } catch (MalformedURLException e) {
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException e) {
            }
        }
    }
}
