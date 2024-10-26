package edu.chop.bic.cnv.ui;

import edu.chop.bic.cnv.database.PositionQuery;
import edu.chop.bic.cnv.domain.*;
import edu.chop.bic.cnv.session.MySession;
import edu.chop.bic.cnv.session.User;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: ohara
 * Date: Jul 22, 2008
 * Time: 10:56:06 AM
 * To change this template use File | Settings | File Templates.
 */
public class Gbrowse extends Panel implements Serializable {

    private User user = ((MySession) getSession()).getUser();

    @SpringBean
    private PositionQuery positionQuery;

    @SpringBean
    private CustomParameters customParameters;

    private String position = "";

    private Position positionObj = new Position();

    IModel positionModel = new Model() {

        public Object getObject() {
            return positionOptions;
        }
    };

    private String positionSelection = "";

    private List positionOptions = new ArrayList<String>();

    private List<Position> positionList = new ArrayList<Position>();

    private PositionHandler positionHandler = null;

    private String[] tracks = null;

    private String legendText = "Show legend";

    private boolean legendVisible = false;

    private String gbrowseHideShowText = user.isImageViewer() ? "Hide image" : "Show image";

    private boolean gbrowseVisible = user.isImageViewer() ? true : false;

    private String width = "";

    private PageParameters pageParameters;

    private Integer maximum = new Integer(0);

    public ExternalLink linkedMapItLink;

    public IModel linkedMapItLinkBackingStore;

    public Search linkedSearchPage;

    private String sync = "smart";

    private boolean showSyncBox = user.isShowSyncBox();

    private String syncSelection = "Smart";

    private String oldSyncSelection = "Smart";

    private final String[] syncButtonOptions = new String[] { "On", "Off", "Smart" };

    public Gbrowse(String id, String[] tracks, final PageParameters pageParameters, PositionHandler positionHandler) {
        super(id);
        setPosition(pageParameters.getString("position"));
        setTracks(tracks);
        setWidth(pageParameters.getString("width"));
        setPageParameters(pageParameters);
        this.positionHandler = positionHandler;
        populatePositionOptions();
        sync = pageParameters.getString("sync");
        final WebMarkupContainer gbrowseWrapper = new WebMarkupContainer("gbrowseWrapper");
        gbrowseWrapper.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
        add(gbrowseWrapper);
        final DropDownChoice positions = new DropDownChoice("positions", new PropertyModel(this, "positionSelection"), positionModel) {

            @Override
            protected CharSequence getDefaultChoice(Object selected) {
                if (!getPositionHandler().getSelectedPosition().equals("") && !positionOptions.contains(getPositionHandler().getSelectedPosition())) {
                    return "<option value=\"\">Current: " + getPositionHandler().getSelectedPosition() + "</option>";
                }
                return "";
            }
        };
        positions.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            protected void onUpdate(AjaxRequestTarget target) {
                try {
                    getPositionHandler().setSelectedPosition(getPositionSelection());
                    positionObj = new Position(getPositionHandler().getSelectedPosition());
                    setMaximum(positionQuery.getSize(positionObj.getChr()));
                    HashMap<String, String> params = getPageParameters();
                    params.put("selectedPosition", getPositionHandler().getSelectedPosition());
                    if (syncShouldBeOn()) {
                        setResponsePage(Search.class, new PageParameters(params));
                    }
                } catch (CnvException e) {
                    System.err.println("There was a problem creating the Position: " + e.getMessage());
                }
                linkedMapItLinkBackingStore.setObject(getPositionHandler().getSelectedPosition());
                target.addComponent(gbrowseWrapper);
                target.addComponent(linkedMapItLink);
            }
        });
        gbrowseWrapper.add(positions);
        final WebMarkupContainer syncBox = new WebMarkupContainer("syncBox") {

            public boolean isVisible() {
                return showSyncBox;
            }
        };
        syncBox.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
        final AjaxLink syncLink = new AjaxLink("syncLink") {

            public void onClick(AjaxRequestTarget target) {
                toggleSyncBox();
                target.addComponent(syncBox);
                target.addComponent(this);
            }

            public boolean isVisible() {
                String saved = pageParameters.getString("saved");
                String sample = pageParameters.getString("sample");
                if (saved != null && saved.equals("true")) {
                    return false;
                }
                if (sample != null && !sample.equals("")) {
                    return false;
                }
                return true;
            }
        };
        if (pageParameters.containsKey("sync")) {
            if (pageParameters.getString("sync").equals("true")) {
                syncSelection = "On";
            } else {
                if (pageParameters.getString("sync").equals("false")) syncSelection = "Off"; else syncSelection = "Smart";
            }
        }
        final RadioChoice syncButtons = (RadioChoice) new RadioChoice("syncButtons", new PropertyModel(this, "syncSelection"), new Model(new ArrayList(Arrays.asList(syncButtonOptions)))).add(new AjaxFormChoiceComponentUpdatingBehavior() {

            protected void onUpdate(AjaxRequestTarget target) {
                try {
                    CnvInvalidParameterException.checkParameter(oldSyncSelection, syncButtonOptions);
                    if (!syncShouldBeOn() && (!oldSyncSelection.equals("On") || (oldSyncSelection.equals("Smart") && smartSyncDefaultOn()))) {
                        HashMap<String, String> params = getPageParameters();
                        assignSyncParameterValue();
                        params.put("selectedPosition", "");
                        params.put("sync", sync);
                        assignSyncParameterValue();
                        setResponsePage(Search.class, new PageParameters(params));
                    }
                    if (syncShouldBeOn() && (!oldSyncSelection.equals("Off") || (oldSyncSelection.equals("Smart") && !smartSyncDefaultOn()))) {
                        assignSyncParameterValue();
                        HashMap<String, String> params = getPageParameters();
                        params.put("selectedPosition", getPositionHandler().getSelectedPosition());
                        params.put("sync", sync);
                        setResponsePage(Search.class, new PageParameters(params));
                    }
                } catch (InternalCnvException e) {
                    e.printStackTrace();
                }
                oldSyncSelection = syncSelection;
            }
        });
        syncBox.add(syncButtons);
        AjaxLink shiftLeft = new AjaxLink("shiftLeft") {

            String newPosition = "";

            public boolean isVisible() {
                boolean visible = true;
                int interval = positionObj.getEnd() - positionObj.getStart();
                int newEnd = positionObj.getStart();
                int newStart = newEnd - interval;
                if (newStart < 0 || newEnd > getMaximum()) {
                    visible = false;
                } else {
                    visible = true;
                    newPosition = "chr" + positionObj.getChr() + ":" + newStart + "-" + newEnd;
                }
                return visible;
            }

            public void onClick(AjaxRequestTarget target) {
                try {
                    if (syncShouldBeOn()) {
                        assignSyncParameterValue();
                        HashMap<String, String> params = getPageParameters();
                        params.put("selectedPosition", newPosition);
                        params.put("sync", sync);
                        setResponsePage(Search.class, new PageParameters(params));
                    } else {
                        getPositionHandler().setSelectedPosition(newPosition);
                        setPositionSelection(newPosition);
                        positionObj = new Position(newPosition);
                        target.addComponent(gbrowseWrapper);
                    }
                } catch (CnvException e) {
                    System.err.println(e.getMessage());
                }
            }
        };
        gbrowseWrapper.add(shiftLeft);
        AjaxLink zoomIn = new AjaxLink("zoomIn") {

            String newPosition = "";

            public boolean isVisible() {
                boolean visible = true;
                int interval = positionObj.getEnd() - positionObj.getStart();
                int newEnd = positionObj.getEnd() - interval / 4;
                int newStart = positionObj.getStart() + interval / 4;
                if (newStart == newEnd || newEnd > getMaximum()) {
                    visible = false;
                } else {
                    visible = true;
                    newPosition = "chr" + positionObj.getChr() + ":" + newStart + "-" + newEnd;
                }
                return visible;
            }

            public void onClick(AjaxRequestTarget target) {
                try {
                    if (syncShouldBeOn()) {
                        assignSyncParameterValue();
                        HashMap<String, String> params = getPageParameters();
                        params.put("selectedPosition", newPosition);
                        params.put("sync", sync);
                        setResponsePage(Search.class, new PageParameters(params));
                    } else {
                        assignSyncParameterValue();
                        getPositionHandler().setSelectedPosition(newPosition);
                        setPositionSelection(newPosition);
                        positionObj = new Position(newPosition);
                        target.addComponent(gbrowseWrapper);
                    }
                } catch (CnvException e) {
                    System.err.println(e.getMessage());
                }
            }
        };
        gbrowseWrapper.add(zoomIn);
        AjaxLink zoomOut = new AjaxLink("zoomOut") {

            String newPosition = "";

            public boolean isVisible() {
                boolean visible = true;
                int interval = positionObj.getEnd() - positionObj.getStart();
                int newEnd = positionObj.getEnd() + interval / 4;
                int newStart = positionObj.getStart() - interval / 4;
                if (newStart < 0 || newEnd > getMaximum()) {
                    visible = false;
                } else {
                    visible = true;
                    newPosition = "chr" + positionObj.getChr() + ":" + newStart + "-" + newEnd;
                }
                return visible;
            }

            public void onClick(AjaxRequestTarget target) {
                try {
                    if (syncShouldBeOn()) {
                        assignSyncParameterValue();
                        HashMap<String, String> params = getPageParameters();
                        params.put("selectedPosition", newPosition);
                        params.put("sync", sync);
                        setResponsePage(Search.class, new PageParameters(params));
                    } else {
                        assignSyncParameterValue();
                        getPositionHandler().setSelectedPosition(newPosition);
                        setPositionSelection(newPosition);
                        positionObj = new Position(newPosition);
                        target.addComponent(gbrowseWrapper);
                    }
                } catch (CnvException e) {
                    System.err.println(e.getMessage());
                }
            }
        };
        gbrowseWrapper.add(zoomOut);
        AjaxLink shiftRight = new AjaxLink("shiftRight") {

            String newPosition = "";

            public boolean isVisible() {
                boolean visible = true;
                int interval = positionObj.getEnd() - positionObj.getStart();
                int newEnd = positionObj.getEnd() + interval;
                int newStart = positionObj.getEnd();
                if (newEnd > getMaximum()) {
                    visible = false;
                } else {
                    visible = true;
                    newPosition = "chr" + positionObj.getChr() + ":" + newStart + "-" + newEnd;
                }
                return visible;
            }

            public void onClick(AjaxRequestTarget target) {
                try {
                    if (syncShouldBeOn()) {
                        assignSyncParameterValue();
                        HashMap<String, String> params = getPageParameters();
                        params.put("selectedPosition", newPosition);
                        params.put("sync", sync);
                        setResponsePage(Search.class, new PageParameters(params));
                    } else {
                        assignSyncParameterValue();
                        getPositionHandler().setSelectedPosition(newPosition);
                        setPositionSelection(newPosition);
                        positionObj = new Position(newPosition);
                        target.addComponent(gbrowseWrapper);
                    }
                } catch (CnvException e) {
                    System.err.println(e.getMessage());
                }
            }
        };
        gbrowseWrapper.add(shiftRight);
        Label label = new Label("gbrowse", new LoadableDetachableModel() {

            protected Object load() {
                String gbrowse = "";
                if (positionObj.getEnd() - positionObj.getStart() > getMaximum() || getPositionSelection().equals("")) {
                    gbrowse = "Ranges larger than 264,000,000 can not be displayed.";
                } else {
                    gbrowse = getGbrowse(getPositionHandler().getSelectedPosition(), getTracks());
                }
                return gbrowse;
            }
        });
        label.setEscapeModelStrings(false);
        label.setOutputMarkupId(true);
        gbrowseWrapper.add(label);
        final WebMarkupContainer legend = new WebMarkupContainer("legend") {

            public boolean isVisible() {
                return legendVisible;
            }
        };
        legend.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
        gbrowseWrapper.add(legend);
        gbrowseWrapper.add(syncBox);
        gbrowseWrapper.add(syncLink);
        legend.add(new WebMarkupContainer("cnvLegend"));
        WebMarkupContainer ucscLegend = new WebMarkupContainer("ucscLegend") {

            public boolean isVisible() {
                return legendVisible && customParameters.getCustomColor().equals("on") && !((MySession) getSession()).getUser().getUsername().equals("");
            }
        };
        ucscLegend.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
        legend.add(ucscLegend);
        final Label legendLabel = new Label("legendLabel", new LoadableDetachableModel() {

            protected Object load() {
                return legendText;
            }
        });
        legendLabel.setOutputMarkupId(true);
        AjaxLink legendShowHide = new AjaxLink("legendShowHide") {

            public void onClick(AjaxRequestTarget target) {
                if (legendText.equals("Show legend")) {
                    legendVisible = true;
                    legendText = "Hide legend";
                } else {
                    legendVisible = false;
                    legendText = "Show legend";
                }
                target.addComponent(legendLabel);
                target.addComponent(legend);
            }
        };
        legendShowHide.add(legendLabel);
        gbrowseWrapper.add(legendShowHide);
    }

    public String getGbrowse(String position, String[] tracks) {
        Calendar cal1 = Calendar.getInstance();
        StringBuilder gbrowseHtml = new StringBuilder();
        String urlString = customParameters.getGbrowseImgUrl();
        urlString += "/cnv_gbrowse_hg";
        urlString += customParameters.getBuild();
        urlString += "/?name=";
        urlString += position;
        if (tracks.length > 0) {
            urlString += "&type=";
        }
        for (int i = 0; i < tracks.length - 1; i++) {
            urlString += tracks[i] + "+";
        }
        urlString += tracks[tracks.length - 1];
        urlString += "&embed=1";
        urlString += "&width=" + getWidth();
        URL url = null;
        try {
            url = new URL(urlString);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = "";
            while ((line = in.readLine()) != null) {
                line = line.replaceAll("hmap\"", "hmap" + position + "\"");
                if (line.startsWith("<area shape")) {
                    if (line.indexOf("href=\"../..") < 0) {
                        gbrowseHtml.append(customizeHrefHtml(line));
                    }
                } else if (line.indexOf("<img") >= 0) {
                    gbrowseHtml.append(line.replace("src=\"", "src=\"" + customParameters.getGbrowseHost()));
                }
            }
            gbrowseHtml.append("</map>");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Calendar cal2 = Calendar.getInstance();
        String gbrowseHtmlString = gbrowseHtml.toString();
        gbrowseHtmlString = gbrowseHtmlString.replaceAll("alt=\"[^\"]*\"", "");
        gbrowseHtmlString = gbrowseHtmlString.replaceAll("(\\s)+", " ");
        return gbrowseHtmlString;
    }

    protected String customizeHrefHtml(String line) {
        String newLine = "";
        String oldLink = line.substring(line.indexOf("href="), line.indexOf("title=") - 1);
        String originalPosition = line.substring(line.indexOf("href=") + 6, line.indexOf("title=") - 2);
        try {
            String newLink = "href=\"";
            if (line.contains("DGV CNV")) {
                newLink += "http://projects.tcag.ca/variation/cgi-bin/gbrowse/hg18?name=" + originalPosition;
            } else if (line.contains("GAD Phenotype")) {
                originalPosition = originalPosition.replace("%3A", ":");
                CharSequence inputStr = originalPosition;
                String patternStr = "chr([X|Y|0-9]+):([0-9]+)-([0-9]+)";
                Pattern pattern = Pattern.compile(patternStr);
                Matcher matcher = pattern.matcher(inputStr);
                boolean matchFound = matcher.find();
                String chr = "";
                String start = "";
                String end = "";
                if (matcher.groupCount() > 2) {
                    chr = matcher.group(1);
                    start = matcher.group(2);
                    end = matcher.group(3);
                }
                newLink += "http://geneticassociationdb.nih.gov/cgi-bin/tableview.cgi?table=allview&cond=CHR='" + chr + "' AND upper(DNA_START) like '%25" + start + "%' AND upper(DNA_END) like '%25" + end + "%'";
            } else if (line.contains("UCSC Gene") || line.contains("UCSC Band")) {
                newLink += "http://genome.ucsc.edu/cgi-bin/hgTracks?position=" + originalPosition + "&pix=610&db=hg18&Submit=Submit";
            } else {
                newLink += "?";
                Iterator iter = getPageParameters().entrySet().iterator();
                while (iter.hasNext()) {
                    Object key = iter.next();
                    try {
                        if (!key.toString().contains("null")) {
                            if (key.toString().startsWith("position")) {
                                newLink += "position=" + originalPosition + "&";
                            } else if (key.toString().startsWith("source")) {
                                if (line.contains("CHOP CNV")) {
                                    newLink += "source=Normals_CHOP&";
                                } else {
                                    newLink += key + "&";
                                }
                            } else if (key.toString().startsWith("displayPosition") && syncShouldBeOn()) {
                                newLink += "displayPosition=" + originalPosition + "&";
                            } else if (key.toString().startsWith("selectedPosition") && syncShouldBeOn()) {
                                newLink += "selectedPosition=" + originalPosition + "&";
                            } else if (key.toString().startsWith("searchMode")) {
                                newLink += "searchMode=Basic / annotated&";
                            } else {
                                if (getPageParameters().getString(key.toString()) != null && !getPageParameters().getString(key.toString()).equals("")) {
                                    newLink += key.toString();
                                    newLink += "&";
                                }
                            }
                        }
                    } catch (Exception E) {
                        E.printStackTrace();
                    }
                }
                if (getPageParameters().getString("submit") != null && !getPageParameters().getString("submit").equals("")) {
                    newLink += "submit=" + getPageParameters().getString("submit");
                }
            }
            newLink += "\"";
            newLine = line.replace(oldLink, newLink);
            newLine = newLine.replace("target=\"_top\"", "target=\"_blank\"");
        } catch (Exception e) {
            System.out.println("Error in customizeHrefHtml: " + e.getMessage());
        }
        return newLine;
    }

    public boolean smartSyncDefaultOn() {
        return positionOptions.size() == 1;
    }

    public boolean syncShouldBeOn() throws CnvInvalidParameterException {
        CnvInvalidParameterException.checkParameter(syncSelection, syncButtonOptions);
        return syncSelection.equalsIgnoreCase("On") || ((syncSelection.equalsIgnoreCase("Smart")) && smartSyncDefaultOn());
    }

    private void assignSyncParameterValue() throws CnvInvalidParameterException {
        CnvInvalidParameterException.checkParameter(syncSelection, syncButtonOptions);
        sync = (new String[] { "true", "false", "smart" })[new ArrayList(Arrays.asList(syncButtonOptions)).indexOf(syncSelection)];
    }

    public void populatePositionOptions() {
        positionOptions.clear();
        positionList = positionHandler.getPositionList();
        try {
            if (positionSelection.equals("")) {
                setPositionSelection(positionHandler.getSelectedPosition());
            }
            boolean first = true;
            for (Position p : positionList) {
                if (first) {
                    positionObj = new Position(getPositionSelection());
                    setMaximum(positionQuery.getSize(positionObj.getChr()));
                    first = false;
                }
                positionOptions.add(p.toStringNoComma());
            }
        } catch (CnvException e) {
            System.err.println("There was a problem creating Position.  " + e.getMessage());
        }
    }

    public void updatePositionDelegates() {
        setPositionSelection(positionHandler.getSelectedPosition());
        linkedMapItLinkBackingStore.setObject(positionHandler.getSelectedPosition());
    }

    private void toggleSyncBox() {
        showSyncBox = !showSyncBox;
        user.setShowSyncBox(showSyncBox);
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String[] getTracks() {
        return tracks;
    }

    public void setTracks(String[] tracks) {
        this.tracks = tracks;
    }

    public String getPositionSelection() {
        return positionSelection;
    }

    public void setPositionSelection(String positionSelection) {
        this.positionSelection = (positionSelection == null ? "" : positionSelection);
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public PageParameters getPageParameters() {
        return pageParameters;
    }

    public void setPageParameters(PageParameters pageParameters) {
        this.pageParameters = pageParameters;
    }

    public Integer getMaximum() {
        return maximum;
    }

    public void setMaximum(Integer maximum) {
        if (maximum != null) this.maximum = maximum;
    }

    public CustomParameters getCustomParameters() {
        return customParameters;
    }

    public void setCustomParameters(CustomParameters customParameters) {
        this.customParameters = customParameters;
    }

    public PositionHandler getPositionHandler() {
        return positionHandler;
    }

    public void setPositionHandler(PositionHandler positionHandler) {
        this.positionHandler = positionHandler;
    }
}
