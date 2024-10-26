package org.authsum.stitches.wicket.nearbys;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.authsum.stitches.services.*;
import org.authsum.stitches.wicket.*;
import org.authsum.stitches.main.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.io.Streams;
import org.apache.wicket.validation.validator.StringValidator;
import org.authsum.stitches.external.*;
import org.authsum.stitches.wicket.BaseLandingPanel;
import org.authsum.stitches.wicket.content.table.ContentTable;
import org.authsum.stitches.wicket.contentdef.ContentDefsPanel;
import org.authsum.stitches.external.*;
import wicket.contrib.gmap.*;
import wicket.contrib.gmap.api.*;
import wicket.contrib.gmap.util.Geocoder;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import wicket.contrib.gmap.api.GMarkerOptions;

public class GeoSearchPanel extends BaseLandingPanel {

    GeoSearchRequest geoSearchRequest = new GeoSearchRequest();

    public DistanceChoice selectedDistanceChoice = null;

    public DropDownChoice getDistanceDropDownChoice() {
        List<DistanceChoice> list = new ArrayList<DistanceChoice>();
        list.add(new DistanceChoice("1 Mile", 1.0D));
        list.add(new DistanceChoice("5 Miles", 5.0D));
        list.add(new DistanceChoice("10 Miles", 10.0D));
        list.add(new DistanceChoice("25 Miles", 25.0D));
        DropDownChoice ddc = new DropDownChoice("mileRadius", new PropertyModel(this, "selectedDistanceChoice"), list);
        return ddc;
    }

    public DropDownChoice getContentDefDropDownChoice() {
        ContentDefRequest cdrequest = new ContentDefRequest();
        AttributeValuesRequest request = new AttributeValuesRequest();
        ContentDefResponse cdr = getStitchesService().findContentDefs(cdrequest);
        final List list = cdr.getContentDefHolders().getContentDefHolder();
        System.out.println("returned " + list.size() + " contentdefs");
        DropDownChoice ddc = new DropDownChoice("contentDefIds", new PropertyModel(this, "selectedContentDefHolder"), list, new IChoiceRenderer() {

            public String getDisplayValue(Object object) {
                ContentDefHolder cdh = (ContentDefHolder) object;
                return cdh.getName();
            }

            public String getIdValue(Object object, int index) {
                return new Integer(index).toString();
            }
        });
        return ddc;
    }

    public ContentDefHolder selectedContentDefHolder = null;

    protected void createGeoCodeForm() {
        SearchParms sp = new SearchParms();
        Form geocodeForm = new Form("geocodeForm", new CompoundPropertyModel(sp));
        add(geocodeForm);
        geocodeForm.add(new RequiredTextField("geocodeAddress").add(StringValidator.maximumLength(255)));
        Label longitude = new Label("longitude", longModel);
        geocodeForm.add(longitude);
        Label latitude = new Label("latitude", latModel);
        geocodeForm.add(latitude);
        IndicatingAjaxButton geocodeButton = new IndicatingAjaxButton("geocodeButton", geocodeForm) {

            private static final long serialVersionUID = 1L;

            SearchParms searchParms = (SearchParms) searchForm.getModel().getObject();

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                logger.debug("ajax search onError");
                target.addComponent(feedbackPanel);
                searchForm.setEnabled(false);
            }

            protected void onSubmit(AjaxRequestTarget target, Form form) {
                try {
                    SearchParms formModel = (SearchParms) form.getModel().getObject();
                    GLatLng thelatlong = geoCodeAddress(formModel);
                    latLng = thelatlong;
                    gmap.addOverlay(new GMarker(latLng));
                    searchForm.setEnabled(true);
                    update(target);
                } catch (IOException e) {
                    e.printStackTrace();
                    target.appendJavascript("Unable to geocode (" + e.getMessage() + ")");
                }
            }
        };
        geocodeButton.setDefaultFormProcessing(true);
        geocodeForm.add(geocodeButton);
    }

    protected GLatLng latLng = null;

    private void update(AjaxRequestTarget target) {
        target.addComponent(this);
        geoSearchRequest.setLatitude(latLng.getLat());
        geoSearchRequest.setLongitude(latLng.getLng());
        gmap.setCenter(latLng);
        gmap.addOverlay(new GMarker(latLng));
        target.addComponent(feedbackPanel);
    }

    protected void createSearchForm() {
        searchForm = new Form("searchForm", new CompoundPropertyModel(geoSearchRequest));
        add(searchForm);
        searchForm.add(new TextField("keywords").add(StringValidator.maximumLength(255)));
        searchForm.add(getDistanceDropDownChoice());
        searchForm.add(getContentDefDropDownChoice());
        IndicatingAjaxButton search = new IndicatingAjaxButton("search", searchForm) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(feedbackPanel);
            }

            protected void onSubmit(AjaxRequestTarget target, Form form) {
                if (!validateSearch()) {
                    target.addComponent(feedbackPanel);
                    return;
                }
                performSearch(target, form);
            }

            private boolean validateSearch() {
                List<String> errors = new ArrayList();
                if (!errors.isEmpty()) {
                    Iterator<String> iterator = errors.iterator();
                    while (iterator.hasNext()) {
                        error(iterator.next());
                    }
                    return false;
                }
                return true;
            }
        };
        search.setDefaultFormProcessing(true);
        searchForm.add(search);
    }

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    GMap2 gmap = null;

    public Model latModel = new Model() {

        public Serializable getObject() {
            return geoSearchRequest.getLatitude();
        }
    };

    public Model longModel = new Model() {

        public Serializable getObject() {
            return geoSearchRequest.getLongitude();
        }
    };

    public GLatLng geoCodeAddress(SearchParms searchParms) throws IOException {
        logger.debug(searchParms.getGeocodeAddress());
        URL url = new URL(geocoder.encode(searchParms.getGeocodeAddress()));
        URLConnection connection = url.openConnection();
        String content = Streams.readString(connection.getInputStream());
        return geocoder.decode(content);
    }

    private StitchesPropertyHolder getStitchesPropertyHolder() {
        return ((WicketApplication) getApplication()).getStitchesPropertyHolder();
    }

    private Geocoder geocoder;

    public GeoSearchPanel(String id) {
        super(id);
        gmap = new GMap2("gmap", getStitchesPropertyHolder().getGmapKey());
        this.geocoder = new Geocoder(getStitchesPropertyHolder().getGmapKey());
        logger.debug("adding overlay, valid long lat");
        GLatLng latLng = new GLatLng(40.438167, -79.921142);
        gmap.setCenter(latLng);
        gmap.addOverlay(new GMarker(latLng));
        gmap.addControl(GControl.GSmallMapControl);
        detailPanel = new Panel("detailPanel");
        updateContainer.add(gmap);
        createGeoCodeForm();
        postInit();
        searchForm.setEnabled(false);
        createResultsList(new ArrayList());
    }

    protected void initTable(List list) {
        resultsTable = new Panel("resultsTable");
        updateContainer.addOrReplace(resultsTable);
    }

    private static Log logger = LogFactory.getLog(ContentDefsPanel.class);

    protected void refreshTable(AjaxRequestTarget target, GeoSearchResponse geoSearchResponse) {
        logger.debug("refreshTable");
        gmap = new GMap2("gmap", getStitchesPropertyHolder().getGmapKey());
        gmap.setCenter(latLng);
        gmap.addOverlay(new GMarker(latLng));
        updateContainer.addOrReplace(gmap);
        Iterator<GeoSearchResult> iterator = geoSearchResponse.getGeoSearchResults().getGeoSearchResult().iterator();
        int i = 1;
        while (iterator.hasNext()) {
            final GeoSearchResult gsr = iterator.next();
            final GLatLng latLng = new GLatLng(gsr.getLatitude(), gsr.getLongitude());
            GMarkerOptions gmarkOptions = new GMarkerOptions("" + i);
            GMarker gmarker = new GMarker(latLng, gmarkOptions);
            gmap.addOverlay(gmarker);
            i++;
        }
        ContentDefRequest cdr = new ContentDefRequest();
        createResultsList(geoSearchResponse.getGeoSearchResults().getGeoSearchResult());
        target.addComponent(this);
    }

    private ListView geosearchResultsList = null;

    private void createResultsList(List results) {
        geosearchResultsList = new ListView("geosearchResultsList", results) {

            protected void populateItem(ListItem item) {
                GeoSearchResult searchResult = (GeoSearchResult) item.getModel().getObject();
                item.add(new Label("resultNumber", "" + (item.getIndex() + 1)));
                item.add(new Label("name", searchResult.getContentName()));
                item.add(new Label("distance", searchResult.getDistance().toString() + " miles"));
                item.add(new Label("cityName", getCityName(searchResult)));
            }
        };
        addOrReplace(geosearchResultsList);
    }

    private String getCityName(GeoSearchResult geoSearchResult) {
        System.out.println("getCityName");
        System.out.println("there are attr values " + geoSearchResult.getAttributeValueHolders().getAttributeValueHolder().size());
        Iterator<AttributeValueHolder> iterator = geoSearchResult.getAttributeValueHolders().getAttributeValueHolder().iterator();
        while (iterator.hasNext()) {
            AttributeValueHolder avh = iterator.next();
            System.out.println("avh name=" + avh.getAttributeName());
            if (avh.getAttributeName().equals("cityName")) {
                return avh.getValueAsString();
            }
        }
        return "City name not found";
    }

    @Override
    public void createDetailObject() {
    }

    @Override
    public void performSearch(AjaxRequestTarget target, Form form) {
        if (selectedContentDefHolder != null) {
            geoSearchRequest.setContentDefIds(new ArrayOfLong());
            geoSearchRequest.getContentDefIds().getLong().add(selectedContentDefHolder.getId());
        }
        if (selectedDistanceChoice == null) {
            error("Please selected a mile radius.");
            return;
        }
        geoSearchRequest.setMileRadius(selectedDistanceChoice.distanceValue);
        geoSearchRequest.setAttributeNames(new ArrayOfString());
        geoSearchRequest.getAttributeNames().getString().add("cityName");
        System.out.println("doing a longlat search with radius of " + geoSearchRequest.getMileRadius());
        GeoSearchResponse geoSearchResponse = getStitchesService().geosearch(geoSearchRequest);
        System.out.println("there are " + geoSearchResponse.getGeoSearchResults().getGeoSearchResult().size());
        refreshTable(target, geoSearchResponse);
        searchTips.setVisible(geoSearchResponse.getGeoSearchResults().getGeoSearchResult().isEmpty());
    }

    class ResultInfo extends Panel {

        ResultInfo(String id, GeoSearchResult searchResult) {
            super(id);
        }
    }
}
