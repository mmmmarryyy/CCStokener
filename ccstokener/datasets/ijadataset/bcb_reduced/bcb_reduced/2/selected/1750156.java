package au.edu.ausstage.mapping;

import java.io.*;
import java.sql.*;
import java.net.*;

/**
 * A class to manage all aspects of the Geocoding process
 */
public class GeocodeManager {

    DataManager dataManager;

    /**
	 * Constructor for this class
	 *
	 * @param manager the DataManager object used to access the database
	 */
    public GeocodeManager(DataManager manager) {
        dataManager = manager;
    }

    /**
	 * A method to undertake a database search
	 *
	 * @param type           the type of search to undertake
	 * @param queryParameter the query parameter to use in the search
	 *
	 * @return               the results of the search
	 */
    public String doSearch(String type, String queryParameter) throws javax.servlet.ServletException {
        StringBuilder results = new StringBuilder();
        String sqlQuery = "";
        int recordCount = 0;
        this.dataManager.connect();
        if (type.equals("org_search")) {
            sqlQuery = "SELECT organisationid, name, COUNT (have_coords), COUNT (no_coords) " + "FROM (SELECT DISTINCT search_organisation.organisationid, " + "      search_organisation.name, " + "      venue.venueid, " + "      DECODE(venue.longitude, null, null ,'y') as have_coords, " + "      DECODE(venue.longitude, null, 'y', null) as no_coords " + "      FROM search_organisation, " + "           orgevlink, " + "           events, " + "           venue " + "      WHERE CONTAINS(search_organisation.combined_all, ?, 1) > 0 " + "      AND search_organisation.organisationid = orgevlink.organisationid " + "      AND events.eventid = orgevlink.eventid " + "      AND venue.venueid = events.venueid) " + "GROUP BY organisationid, name";
            results.append("<table class=\"searchResults\"><thead><tr><th>Organisation</th><th>Venues with Coordinates</th><th>Venues without Coordinates</th></tr></thead>");
            results.append("<tfoot><tr><td colspan=\"3\">");
            results.append("<ul><li>Click the name of the organisation to view a list of venues with incomplete coordinate information</li>");
            results.append("</ul></tfoot>");
        } else if (type.equals("venue_search")) {
            sqlQuery = "SELECT search_venue.venueid, search_venue.venue_name, search_venue.suburb, states.state, " + "DECODE(venue.longitude, null, 'No' ,'Yes') as have_coords " + "FROM search_venue, states, venue " + "WHERE CONTAINS(search_venue.combined_all, ?, 1) > 0 " + "AND states.stateid = search_venue.state " + "AND search_venue.venueid = venue.venueid " + "ORDER BY venue_name, state";
            results.append("<table class=\"searchResults\"><thead><tr><th>Venue Name</th><th>Suburb / State </th><th>Has Coordinates</th></tr></thead>");
            results.append("<tfoot><tr><td colspan=\"3\">");
            results.append("<ul><li>Click the name of the venue to attempt geocoding for the venue</li>");
            results.append("</ul></tfoot>");
        } else if (type.equals("org_venue_search")) {
            sqlQuery = "SELECT DISTINCT venue.venueid, venue.venue_name, venue.suburb, states.state, " + "DECODE(venue.longitude, null, 'No' ,'Yes') as have_coords " + "FROM orgevlink, " + "events, " + "venue, " + "states " + "WHERE organisationid = ? " + "AND orgevlink.eventid = events.eventid " + "AND venue.venueid = events.venueid " + "AND states.stateid = venue.state " + "ORDER BY venue_name, state";
            results.append("<table class=\"searchResults\"><thead><tr><th>Venue Name</th><th>Suburb / State </th><th>Has Coordinates</th></tr></thead>");
            results.append("<tfoot><tr><td colspan=\"3\">");
            results.append("<ul><li>Click the name of the venue to attempt geocoding for the venue</li>");
            results.append("</ul></tfoot>");
        }
        String[] parameters = new String[1];
        parameters[0] = queryParameter;
        ResultSet resultSet = this.dataManager.executePreparedStatement(sqlQuery, parameters);
        results.append("<tbody>");
        try {
            int j = 2;
            while (resultSet.next() && recordCount < 16) {
                if (recordCount % j == 1) {
                    results.append("<tr class=\"odd\">");
                } else {
                    results.append("<tr>");
                }
                if (type.equals("org_search")) {
                    if (Integer.parseInt(resultSet.getString(4)) > 0) {
                        results.append("<td><a href=\"#\" onclick=\"return showVenues('" + resultSet.getString(1) + "'); return false;\">" + resultSet.getString(2) + "</a></td>");
                    } else {
                        results.append("<td>" + resultSet.getString(2) + "</td>");
                    }
                    results.append("<td>" + resultSet.getString(3) + "</td>");
                    results.append("<td>" + resultSet.getString(4) + "</td></tr>");
                } else if (type.equals("venue_search")) {
                    results.append("<td><a href=\"#\" onclick=\"showVenue('" + resultSet.getString(1) + "'); return false; \">" + resultSet.getString(2) + "</a></td>");
                    if (resultSet.getString(3) != null) {
                        results.append("<td>" + resultSet.getString(3) + " / " + resultSet.getString(4) + "</td>");
                        results.append("<td>" + resultSet.getString(5) + "</td></tr>");
                    } else {
                        results.append("<td>" + " / " + resultSet.getString(4) + "</td>");
                        results.append("<td>" + resultSet.getString(5) + "</td></tr>");
                    }
                } else if (type.equals("org_venue_search")) {
                    results.append("<td><a href=\"#\" onclick=\"showVenue('" + resultSet.getString(1) + "'); return false; \">" + resultSet.getString(2) + "</a></td>");
                    if (resultSet.getString(3) != null) {
                        results.append("<td>" + resultSet.getString(3) + " / " + resultSet.getString(4) + "</td>");
                        results.append("<td>" + resultSet.getString(5) + "</td></tr>");
                    } else {
                        results.append("<td>" + " / " + resultSet.getString(4) + "</td>");
                        results.append("<td>" + resultSet.getString(5) + "</td></tr>");
                    }
                    if (recordCount == 15) {
                        recordCount = -1;
                    }
                }
                recordCount++;
            }
            if (recordCount == 16) {
                results.append("<tr><td colspan=\"3\"><strong>Note: </strong>Record limit of 15 records reached, please adjust your search terms to be more specific</td></tr>");
            }
            resultSet.close();
            this.dataManager.closeStatement();
        } catch (java.sql.SQLException ex) {
            throw new javax.servlet.ServletException("Unable to get search results", ex);
        }
        results.append("</tbody></table>");
        return results.toString();
    }

    /**
	  * A method to lookup information about a venue
	  *
	  * @param id the id number of the venue
	  *
	  * @return  information on the venue as a HTML string
	  */
    public String doVenueLookup(String id) throws javax.servlet.ServletException {
        StringBuilder results = new StringBuilder();
        StringBuilder address = new StringBuilder();
        this.dataManager.connect();
        String sqlQuery = "SELECT venueid, venue_name, other_names1, other_names2, other_names3, " + "       street, suburb, states.state, postcode, country.countryname, longitude, latitude " + "FROM venue, " + "     states, " + "     country " + "WHERE venueid = ? " + "AND venue.state = states.stateid " + "AND venue.countryid = country.countryid ";
        String[] parameters = new String[1];
        parameters[0] = id;
        ResultSet resultSet = this.dataManager.executePreparedStatement(sqlQuery, parameters);
        results.append("<table class=\"searchResults\">");
        results.append("<tfoot><tr><td colspan=\"3\">");
        results.append("<ul><li>Use the standard AusStage Admin interface to update the Geographic information where required</li>");
        results.append("</ul></tfoot>");
        results.append("<tbody>");
        try {
            resultSet.next();
            results.append("<tr><th scope=\"row\">Venue ID</th><td>" + resultSet.getString(1) + "</td></tr>");
            results.append("<tr><th scope=\"row\">Venue Name</th><td>" + resultSet.getString(2) + "</td></tr>");
            if (resultSet.getString(3) != null) {
                results.append("<tr><th scope=\"row\">Other Names</th><td><ul><li>" + resultSet.getString(3) + "</li>");
                if (resultSet.getString(4) != null) {
                    results.append("<li>" + resultSet.getString(4) + "</li>");
                }
                if (resultSet.getString(5) != null) {
                    results.append("<li>" + resultSet.getString(5) + "</li>");
                }
                results.append("</ul></td></tr>");
            } else {
                results.append("<tr><th scope=\"row\">Other Names</th><td>&nbsp;</td></tr>");
            }
            if (resultSet.getString(6) != null) {
                address.append(resultSet.getString(6));
            }
            if (resultSet.getString(7) != null) {
                if (resultSet.getString(6) != null) {
                    address.append(", " + resultSet.getString(7));
                } else {
                    address.append(resultSet.getString(7));
                }
            }
            if (resultSet.getString(8) != null && !resultSet.getString(8).equals("O/S")) {
                address.append(", " + resultSet.getString(8));
            }
            if (resultSet.getString(9) != null) {
                address.append(", " + resultSet.getString(9));
            }
            if (resultSet.getString(10) != null) {
                address.append(", " + resultSet.getString(10));
            }
            results.append("<tr><th scope=\"row\">Address</th><td>" + address.toString() + "</td></tr>");
            if (resultSet.getString(11) != null) {
                results.append("<tr><th scope=\"row\">Longitude (in AusStage)</th><td>" + resultSet.getString(11) + "</td></tr>");
                results.append("<tr><th scope=\"row\">Latitude (in AusStage)</th><td>" + resultSet.getString(12) + "</td></tr>");
                results.append("<tr><th scope=\"row\">View in Google Maps</th><td><a href=\"http://maps.google.com/?q=" + resultSet.getString(12) + "," + resultSet.getString(11) + "\" title=\"View Coordinates on Google Maps\" target=\"blank\">View Map</a></li>");
            } else {
                results.append("<tr><th scope=\"row\">Longitude (in AusStage)</th><td>Not Available</td></tr>");
                results.append("<tr><th scope=\"row\">Latitude (in AusStage)</th><td>Not Available</td></tr>");
                String geocodeUrl = dataManager.getContextParam("googleGeocodeUrl");
                String encodedAddress = URLEncoder.encode(resultSet.getString(2) + ", " + address.toString(), "utf-8");
                String geocodeFromGoogle = this.fetchURL(geocodeUrl.replace("[address]", encodedAddress));
                if (geocodeFromGoogle != null) {
                    String[] geocode = geocodeFromGoogle.split(",");
                    if (geocode[0].equals("200")) {
                        int accuracy = Integer.parseInt(geocode[1]);
                        results.append("<tr><th scope=\"row\">Information from Google Maps</th><td><ul>");
                        switch(accuracy) {
                            case 6:
                                results.append("<li>Accurate to Street level</li>");
                                break;
                            case 7:
                                results.append("<li>Accurate to intersection</li>");
                                break;
                            case 8:
                                results.append("<li>Accurate to address</li>");
                                break;
                            case 9:
                                results.append("<li>Accurate to the building</li>");
                                break;
                            default:
                                results.append("<li>Results not accurate enough</li>");
                        }
                        if (accuracy > 5) {
                            results.append("<li>Latitude: " + geocode[2] + "</li>");
                            results.append("<li>Longitude: " + geocode[3] + "</li>");
                            results.append("<li><a href=\"http://maps.google.com/?q=" + geocode[2] + "," + geocode[3] + "\" title=\"View Coordinates on Google Maps\" target=\"blank\">View Map</a></li>");
                        }
                        results.append("</ul></td></tr>");
                    } else {
                        results.append("<tr><th scope=\"row\">Information from Google Maps</th><td>Google Maps reported an error: " + geocode[0] + "</td></tr>");
                    }
                } else {
                    results.append("<tr><th scope=\"row\">Information from Google Maps</th><td>Error occured during lookup</td></tr>");
                }
            }
            results.append("</tbody></table>");
            resultSet.close();
            this.dataManager.closeStatement();
            return results.toString();
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new javax.servlet.ServletException("Unable to lookup venue details", ex);
        } catch (java.sql.SQLException ex) {
            return "<p><strong>Error: </strong><br/>Unable to retrieve venue details, perhaps an incomplete record eg. missing country information.<br/>Check the record in AusStage and try again.</p>";
        }
    }

    /**
	  * A method to take a URL and return the response
	  *
	  * @param url the url to fetch
	  *
	  * @returns   the content returned
	  */
    private String fetchURL(String url) {
        StringBuilder content = new StringBuilder();
        String line;
        BufferedReader input = null;
        try {
            URL urlToFetch = new URL(url);
            input = new BufferedReader(new InputStreamReader(urlToFetch.openStream()));
            while ((line = input.readLine()) != null) {
                content.append(line);
            }
            input.close();
            return content.toString();
        } catch (java.io.IOException ex) {
            return null;
        }
    }
}
