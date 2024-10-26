package edu.lehigh.mab305.swproj.GUI;

import edu.lehigh.mab305.swproj.ConferenceModel.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Observable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import edu.lehigh.mab305.swproj.ConferenceModel.Conference;
import edu.lehigh.mab305.swproj.ConferenceModel.OWL2ConfException;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.vocabulary.*;

public class SeriesPaneController extends Observable implements Runnable {

    public static String CONSTITUENT_CONF_URI = Conference.confURI + "seriesConferenceInstance";

    public static String OCCURSIN_CONF_URI = Conference.confURI + "seriesOccursIn";

    public static String SUBMISSIONSDUEIN_CONF_URI = Conference.confURI + "seriesSubmissionsDueIn";

    protected ConfAnnounceController controller;

    protected SeriesPane pane;

    protected String titleBuffer = null, baseURI = null;

    protected String ontNameBuffer = null, occursInBuffer = null, submissionsDueInBuffer = null;

    protected ArrayList<String> constConfBuffer = null;

    protected Model seriesModelBuffer = null;

    public SeriesPaneController(ConfAnnounceController controller) {
        super();
        this.controller = controller;
    }

    public void loadSeriesOntology(String ontology) {
        if (ontology != null && ontology != "") {
            this.ontNameBuffer = ontology;
            (new Thread(this)).start();
        }
    }

    public void notifyObservers(Object e) {
        this.setChanged();
        super.notifyObservers(e);
    }

    public void run() {
        Model seriesModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        try {
            URL onturl = new URL(this.ontNameBuffer);
            URLConnection urlc = onturl.openConnection();
            InputStream i = urlc.getInputStream();
            seriesModel.read(i, null);
            this.seriesModelBuffer = seriesModel;
            this.controller.getWindow().getSShell().getDisplay().asyncExec(new Runnable() {

                public void run() {
                    try {
                        readSeriesOntology();
                    } catch (OWL2ConfException e) {
                    }
                }
            });
        } catch (MalformedURLException me) {
            this.notifyObservers(me);
        } catch (IOException ie) {
            this.notifyObservers(ie);
        }
    }

    protected void readSeriesOntology() throws OWL2ConfException {
        readSeriesOntology(this.seriesModelBuffer);
    }

    public ArrayList<String> getTopicAreas() {
        return this.pane.getTopicAreas();
    }

    public void setTopicAreas(ArrayList<String> topicAreas) {
        this.pane.setTopicAreas(topicAreas);
    }

    protected void readSeriesOntology(Model model) throws OWL2ConfException {
        ResIterator res = model.listSubjectsWithProperty(RDF.type, ResourceFactory.createResource(ConferenceSeries.SERIES_TYPE_URI));
        NodeIterator niter;
        Resource series;
        ArrayList<String> constituents = new ArrayList<String>();
        Display disp = this.pane.getShell().getDisplay();
        if (!res.hasNext()) {
            throw new OWL2ConfException("Ontology document does not contain a ConferenceSeries");
        }
        this.pane.setEditable(false);
        series = res.nextResource();
        niter = model.listObjectsOfProperty(series, ResourceFactory.createProperty(Conference.CONFERENCE_TITLE_URI));
        if (niter.hasNext()) {
            this.titleBuffer = niter.nextNode().toString();
            disp.asyncExec(new Runnable() {

                public void run() {
                    setSeriesTitle(titleBuffer);
                }
            });
        }
        niter = model.listObjectsOfProperty(series, ResourceFactory.createProperty(CONSTITUENT_CONF_URI));
        if (niter.hasNext()) {
            while (niter.hasNext()) {
                constituents.add(niter.nextNode().toString());
            }
            this.constConfBuffer = constituents;
            disp.asyncExec(new Runnable() {

                public void run() {
                    setConstituentConfs(constConfBuffer);
                }
            });
        }
        niter = model.listObjectsOfProperty(series, ResourceFactory.createProperty(OCCURSIN_CONF_URI));
        if (niter.hasNext()) {
            this.occursInBuffer = niter.nextNode().toString();
            disp.asyncExec(new Runnable() {

                public void run() {
                    setOccursIn(occursInBuffer);
                }
            });
        }
        niter = model.listObjectsOfProperty(series, ResourceFactory.createProperty(SUBMISSIONSDUEIN_CONF_URI));
        if (niter.hasNext()) {
            this.submissionsDueInBuffer = niter.nextNode().toString();
            disp.asyncExec(new Runnable() {

                public void run() {
                    setSubmissionsDueIn(submissionsDueInBuffer);
                }
            });
        }
    }

    /**
	 * @return the conf
	 */
    public Conference getConf() {
        return this.controller.getConf();
    }

    /**
	 * @param conf the conf to set
	 */
    public void setController(ConfAnnounceController controller) {
        this.controller = controller;
    }

    public void setPane(SeriesPane window) {
        this.pane = window;
    }

    public void setSeriesTitle(String title) {
        if (title != null) {
            this.pane.setSeriesTitle(title);
        }
    }

    public String getSeriesTitle() {
        return this.pane.getSeriesTitle();
    }

    public void setConstituentConfs(ArrayList<String> confs) {
        this.pane.setConstituentConfs(confs);
    }

    public ArrayList<String> getConstituentConfs() {
        return this.pane.getConstituentConfs();
    }

    public void setOccursIn(String occursIn) {
        if (occursIn != null) {
            String s = occursIn.substring(occursIn.lastIndexOf("#") + 1);
            this.pane.setMonthStarts(s);
        }
    }

    public String getOccursIn() {
        return this.pane.getMonthStarts();
    }

    public void setSubmissionsDueIn(String submissionsDueIn) {
        if (submissionsDueIn != null) {
            String s = submissionsDueIn.substring(submissionsDueIn.lastIndexOf("#") + 1);
            this.pane.setMonthSubmissions(s);
        }
    }

    public String getSubmissionsDueIn() {
        return this.pane.getMonthSubmissions();
    }

    public String getSeriesBaseURI() {
        String ret = null;
        if (this.pane.getSeriesBaseURI() != null) {
            ret = this.pane.getSeriesBaseURI();
        }
        return ret;
    }

    public ArrayList<String> getConferenceTopicAreas() {
        return this.controller.getConferenceTopicAreas();
    }

    public ArrayList<String> getConferenceTopicAreaLabels() {
        return this.controller.getConferenceTopicAreaLabels();
    }

    public Model getCompositeTopicModel() {
        return this.controller.getCompositeTopicModel();
    }

    public String getSeriesURI() {
        String base = this.controller.getSeriesBaseURI();
        String ret = "";
        if (base != null && base.length() > 0) {
            if (base.contains("#")) {
                if (base.charAt(base.length() - 1) == '#') {
                    ret = base + Conference.makeURIFriendly(this.pane.getSeriesTitle());
                } else {
                    ret = base;
                }
            } else {
                ret = base + "#" + Conference.makeURIFriendly(this.pane.getSeriesTitle());
            }
        }
        return ret;
    }

    public void setSeriesURI(String uri) {
        this.pane.setSeriesBaseURI(uri);
    }

    public boolean isEditable() {
        return this.pane.isEditable();
    }

    public void setEditable(boolean editable) {
        this.pane.setEditable(editable);
    }

    public Tree getSeriesTree() {
        return this.pane.getTree();
    }

    public void refreshNew() {
        this.pane.resetInterface();
    }

    public String getWebsite() {
        return this.pane.getSeriesWebsite();
    }

    public void setWebsite(String website) {
        this.pane.setSeriesWebsite(website);
    }

    /**
	 * @return the controller
	 */
    public ConfAnnounceController getController() {
        return controller;
    }

    public void clearTree() {
        this.pane.clearTree();
    }
}
