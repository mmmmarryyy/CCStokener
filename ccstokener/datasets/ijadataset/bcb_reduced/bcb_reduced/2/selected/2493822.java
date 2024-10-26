package de.mbenning.weather.wunderground.impl.services;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Scanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import de.mbenning.weather.wunderground.api.domain.HttpProxy;
import de.mbenning.weather.wunderground.api.domain.WeatherStation;
import de.mbenning.weather.wunderground.api.services.IWeatherStationService;

/**
 * @author Martin.Benning
 *
 */
@Service
@Scope("prototype")
@Qualifier("weatherStationService")
public class WeatherStationService implements IWeatherStationService {

    private String url = ResourceBundle.getBundle("wunderground-core").getString("wunderground.core.http.stations.url");

    @Autowired
    @Qualifier("httpProxy")
    private HttpProxy httpProxy;

    public List<WeatherStation> findAllWeatherStationsByCountry(String country) {
        List<WeatherStation> stations = parseStations(this.loadSource(country), country);
        return stations;
    }

    protected Scanner loadSource(String country) {
        if (httpProxy != null && httpProxy.isEnabled()) {
            System.setProperty("proxySet", "true");
            System.setProperty("http.proxyHost", httpProxy.getUrl());
            System.setProperty("http.proxyPort", Integer.toString(httpProxy.getPort()));
        }
        try {
            URL url = new URL(this.url + country);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            return new Scanner(connection.getInputStream());
        } catch (MalformedURLException e) {
            return null;
        } catch (ProtocolException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    protected List<WeatherStation> parseStations(Scanner sourceScanner, String country) {
        List<WeatherStation> stations = new ArrayList<WeatherStation>();
        if (sourceScanner != null) {
            boolean isAdding = false;
            while (sourceScanner.hasNextLine()) {
                String line = sourceScanner.nextLine();
                if (line != null) {
                    WeatherStation weatherStation = null;
                    if (line.contains("/weatherstation/WXDailyHistory.asp?ID=")) {
                        String[] lineParts1 = line.split("<td><a href=");
                        String[] lineParts2 = lineParts1[1].split("\">");
                        String[] idParts = lineParts2[1].split("</a></td>");
                        if (idParts[0] != null) {
                            weatherStation = new WeatherStation(idParts[0]);
                            weatherStation.setCountry(country);
                            isAdding = true;
                        }
                        if (isAdding) {
                            String neighLine = sourceScanner.nextLine();
                            String[] neighParts1 = neighLine.split("<td>");
                            String[] neighParts2 = neighParts1[1].split("&nbsp;</td>");
                            if (neighParts2 != null && neighParts2.length > 0 && neighParts2[0] != null && weatherStation != null) {
                                weatherStation.setNeighborhood(neighParts2[0].trim());
                            }
                        }
                        if (isAdding) {
                            String cityLine = sourceScanner.nextLine();
                            String[] cityParts1 = cityLine.split("<td>");
                            String[] cityParts2 = cityParts1[1].split("&nbsp;</td>");
                            if (cityParts2 != null && cityParts2.length > 0 && cityParts2[0] != null && weatherStation != null) {
                                weatherStation.setCity(cityParts2[0].trim());
                            }
                        }
                        if (isAdding) {
                            String stationTypeLine = sourceScanner.nextLine();
                            String[] stationTypeParts1 = stationTypeLine.split("<td>");
                            String[] stationTypeParts2 = stationTypeParts1[1].split("&nbsp;</td>");
                            if (stationTypeParts2 != null && stationTypeParts2.length > 0 && stationTypeParts2[0] != null && weatherStation != null) {
                                weatherStation.setStationType(stationTypeParts2[0].trim());
                            }
                        }
                        if (isAdding) {
                            String siteLine = sourceScanner.nextLine();
                            if (!siteLine.contains("<td>&nbsp;</td>")) {
                                String[] siteParts1 = siteLine.split("<a href=\"");
                                String[] siteParts2 = siteParts1[1].split("\"></a></td>");
                                if (siteParts2 != null && siteParts2.length > 0 && siteParts2[0] != null && weatherStation != null) {
                                    weatherStation.setSite(siteParts2[0].trim());
                                }
                            }
                        }
                    }
                    if (weatherStation != null) {
                        stations.add(weatherStation);
                    }
                    isAdding = false;
                }
            }
        }
        return stations;
    }

    public WeatherStation getWeatherStation(String country, String id) {
        if (country != null && id != null) {
            List<WeatherStation> stations = this.findAllWeatherStationsByCountry(country);
            for (WeatherStation weatherStation : stations) {
                if (weatherStation.getStationId().equals(id)) {
                    return weatherStation;
                }
            }
        }
        return null;
    }

    public HttpProxy getHttpProxy() {
        return httpProxy;
    }

    public void setHttpProxy(HttpProxy httpProxy) {
        this.httpProxy = httpProxy;
    }
}
