package org.moviereport.core.ofdbxmlgw;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.moviereport.core.Configuration;
import org.moviereport.core.MDFAssertion;
import org.moviereport.core.MDFException;
import org.moviereport.core.MovieInternetSearch;
import org.moviereport.core.MDFException.ERROR_TYPE;
import org.moviereport.core.model.MovieDescription;
import org.moviereport.core.model.SearchDetails;
import org.moviereport.core.model.SearchResult;
import org.moviereport.core.ofdbxmlgw.model.FassungOfdbGw;
import org.moviereport.core.ofdbxmlgw.model.FassungResultat;
import org.moviereport.core.ofdbxmlgw.model.MovieOfdbGw;
import org.moviereport.core.ofdbxmlgw.model.MovieResultat;
import org.moviereport.core.ofdbxmlgw.model.SearchOfdbGw;
import org.moviereport.core.ofdbxmlgw.model.SearchResultat;
import org.moviereport.core.ofdbxmlgw.model.MovieResultat.Fassungen;
import org.moviereport.core.ofdbxmlgw.model.MovieResultat.Titel;
import org.moviereport.core.ofdbxmlgw.model.SearchResultat.Eintrag;
import org.moviereport.core.utils.CollectionUtilsExt;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class OfdbXmlGwMDFetcher implements MovieInternetSearch {

    private Logger logger = Logger.getLogger(OfdbXmlGwMDFetcher.class.getName());

    private Configuration configuration = new Configuration();

    private static final String searchURL = "http://xml.n4rf.net/ofdbgw/search/";

    private static final String getMovieDescURL = "http://xml.n4rf.net/ofdbgw/movie/";

    private static final String getFassungURL = "http://xml.n4rf.net/ofdbgw/fassung/";

    private static final String nullImageURL = "http://img.ofdb.de/film/na.gif";

    public MovieDescription getMovieDescription(String movieId) throws MDFException {
        MovieDescription movieDescription = null;
        Fassungen fassungen = null;
        try {
            URL url = new URL(getMovieDescURL + movieId);
            JAXBContext context = JAXBContext.newInstance(MovieOfdbGw.class);
            Unmarshaller um = context.createUnmarshaller();
            um.setEventHandler(new ValidationEventHandler() {

                public boolean handleEvent(ValidationEvent event) {
                    logger.log(Level.WARNING, event.getMessage());
                    return true;
                }
            });
            Object o = um.unmarshal(url);
            MovieOfdbGw movieOfdbGw = (MovieOfdbGw) o;
            MovieResultat movieResultat = movieOfdbGw.getResultat();
            if (movieResultat != null && StringUtils.isNotBlank(movieResultat.getTitel())) {
                movieDescription = new MovieDescription();
                movieDescription.setDescription(movieResultat.getBeschreibung());
                movieDescription.setId(movieId);
                movieDescription.setTitle(movieResultat.getTitel());
                if (movieResultat.getProduktionsland() != null) {
                    movieDescription.setCountry(movieResultat.getProduktionsland().getName());
                }
                if (movieResultat.getGenre() != null && !movieResultat.getGenre().getTitel().isEmpty()) {
                    movieDescription.setGenres(movieResultat.getGenre().getTitel());
                }
                movieDescription.setReleaseYear(movieResultat.getJahr());
                String imageUrlString = movieResultat.getBildURL();
                if (!StringUtils.isBlank(imageUrlString) && !nullImageURL.equals(imageUrlString)) {
                    movieDescription.setMovieImageUrl(imageUrlString);
                }
                movieDescription.setDirector(movieResultat.getRegie());
                movieDescription.setActors(movieResultat.getBesetzung());
                fassungen = movieResultat.getFassungen();
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Error retrieving the Movie description", exception);
            throw new MDFException(ERROR_TYPE.UNDEFINED, exception);
        }
        List<String> selectedFassungIds = new ArrayList<String>();
        List<String> allFassungIds = new ArrayList<String>();
        if (fassungen != null && !CollectionUtilsExt.isEmpty(fassungen.getTitel())) {
            String runtime = "";
            for (Titel titel : fassungen.getTitel()) {
                if (titel.getName().contains("Free-TV")) {
                    selectedFassungIds.add(titel.getId());
                }
                allFassungIds.add(titel.getId());
            }
            runtime = getRuntimeFromFassungen(selectedFassungIds);
            if (StringUtils.isEmpty(runtime)) {
                runtime = getRuntimeFromFassungen(allFassungIds);
            }
            movieDescription.setRuntime(runtime);
        }
        return movieDescription;
    }

    private void saveResponseToFile(URL url, File tempFile, Proxy proxy) throws Exception {
        URLConnection connection = null;
        if (proxy == null) {
            connection = url.openConnection();
        } else {
            connection = url.openConnection(proxy);
        }
        InputStream inputStream = connection.getInputStream();
        FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
        byte[] buffer = new byte[8];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, len);
        }
        fileOutputStream.flush();
        fileOutputStream.close();
        inputStream.close();
    }

    public List<SearchResult> searchMovieDescription(SearchDetails searchDetails) {
        List<SearchResult> searchResults = new ArrayList<SearchResult>();
        try {
            String processedSearchString = processSearchString(searchDetails.getSearchString());
            URL url = new URL(searchURL + processedSearchString);
            JAXBContext context = JAXBContext.newInstance(SearchOfdbGw.class);
            Unmarshaller um = context.createUnmarshaller();
            um.setEventHandler(new ValidationEventHandler() {

                public boolean handleEvent(ValidationEvent event) {
                    logger.log(Level.WARNING, event.getMessage());
                    return true;
                }
            });
            Object o = um.unmarshal(url);
            SearchOfdbGw searchOfdbGw = (SearchOfdbGw) o;
            SearchResultat searchResultat = searchOfdbGw.getResultat();
            if (searchResultat != null && searchResultat.getEintraege() != null) {
                idSet = new HashSet<String>();
                for (Eintrag eintrag : searchResultat.getEintraege()) {
                    SearchResult searchResult = new SearchResult(eintrag.getId(), eintrag.getTitle());
                    if (isSearchResultValid(searchDetails, searchResult)) {
                        searchResults.add(searchResult);
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return searchResults;
    }

    private Set<String> idSet = new HashSet<String>();

    private boolean isSearchResultValid(SearchDetails searchDetails, SearchResult searchResult) {
        if (idSet.contains(searchResult.getId())) {
            return false;
        }
        idSet.add(searchResult.getId());
        String[] splittedSearchString = searchDetails.getSearchString().split(" ");
        for (int i = 0; i < splittedSearchString.length; i++) {
            if (!searchResult.getTitel().toLowerCase().contains(splittedSearchString[i].toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    private void logResponse(File tempFile) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tempFile)));
            String line = null;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String processSearchString(String searchString) {
        String newSearchString = searchString.trim();
        return newSearchString;
    }

    private Document loadDocument(File file) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(file);
        return document;
    }

    public void configure(Configuration configuration) {
        MDFAssertion.preConditionNotNull(configuration);
        this.configuration = configuration;
    }

    private String getRuntimeFromFassungen(Collection<String> fassungIds) throws MDFException {
        String runtime = "";
        for (String fassungId : fassungIds) {
            if (StringUtils.isNotBlank(fassungId)) {
                try {
                    URL url = new URL(getFassungURL + fassungId);
                    JAXBContext context = JAXBContext.newInstance(FassungOfdbGw.class);
                    Unmarshaller um = context.createUnmarshaller();
                    um.setEventHandler(new ValidationEventHandler() {

                        public boolean handleEvent(ValidationEvent event) {
                            logger.log(Level.WARNING, event.getMessage());
                            return true;
                        }
                    });
                    Object o = um.unmarshal(url);
                    FassungOfdbGw fassungOfdbGw = (FassungOfdbGw) o;
                    FassungResultat fassungResultat = fassungOfdbGw.getResultat();
                    if (fassungResultat != null) {
                        String laufzeit = fassungResultat.getLaufzeit();
                        if (StringUtils.isNotBlank(laufzeit)) {
                            String token = "Min";
                            String correctedLaufzeit = laufzeit;
                            if (laufzeit.contains(token)) {
                                correctedLaufzeit = laufzeit.substring(0, laufzeit.indexOf(token) + token.length());
                            }
                            runtime = correctedLaufzeit;
                            break;
                        }
                    }
                } catch (Exception exception) {
                    logger.log(Level.SEVERE, "Error retrieving the Movie description", exception);
                    throw new MDFException(ERROR_TYPE.UNDEFINED, exception);
                }
            }
        }
        return runtime;
    }
}
