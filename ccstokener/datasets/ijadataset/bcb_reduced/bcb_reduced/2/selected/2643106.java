package de.byteholder.geoclipse.weather;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.ViewPart;
import de.byteholder.geoclipse.map.MapView;
import de.byteholder.gpx.GeoPosition;

/**
 * this view displays weather information
 * 
 * @author s2y
 *
 */
public class WeatherViewPart extends ViewPart {

    WeatherProvider weatherProvider;

    String query = "";

    private Label message;

    private Label city;

    private Label date;

    private Label dateTime;

    private Label wind;

    private Label humidity;

    private Label currentCondition;

    private Label temperature;

    private Label currentIcon;

    private Label high;

    private Label low;

    private Label condition;

    private Label icon;

    private Label dayOfWeek;

    private List<WeatherForecastConditions> weatherForecasts;

    private WeatherForecastInformation weatherForecastInformation;

    private WeatherCurrentCondition weatherCurrentCondition;

    private Composite parent;

    public WeatherViewPart() {
    }

    @Override
    public void createPartControl(Composite parent) {
        this.parent = parent;
        init();
        GridLayout gridLayout = new GridLayout(5, true);
        parent.setLayout(gridLayout);
        if (query == null) {
            return;
        }
        message = new Label(parent, SWT.BORDER);
        if (weatherForecasts.size() == 0) {
            message.setText("information not available");
            return;
        }
        city = new Label(parent, SWT.BORDER);
        date = new Label(parent, SWT.BORDER);
        wind = new Label(parent, SWT.BORDER);
        currentCondition = new Label(parent, SWT.BORDER);
        currentIcon = new Label(parent, SWT.BORDER);
        paintUi(parent);
    }

    /**
	 * Get the actual location from the map and query weather information.
	 */
    public void init() {
        query = readMap();
        if (query != null) {
            weatherProvider = new WeatherProvider(query);
            weatherForecasts = weatherProvider.getWeatherDataForecasts();
            weatherForecastInformation = weatherProvider.getWeatherForecastInformation();
            weatherCurrentCondition = weatherProvider.getWeatherCurrentCondition();
        }
    }

    private String readMap() {
        String result = null;
        IWorkbenchPage workbenchPage = getSite().getWorkbenchWindow().getActivePage();
        if (workbenchPage == null) {
            return null;
        } else {
            Object object = workbenchPage.findView(MapView.ID);
            if (object != null) {
                MapView mapView = (MapView) object;
                GeoPosition geoPosition = mapView.getMap().getCenterPosition();
                double latitude = geoPosition.getLatitude();
                double longtitude = geoPosition.getLongitude();
                result = GoogleGeocoder.getCityForLocation(latitude, longtitude);
            }
        }
        return result;
    }

    /**
	 * draw the controls and initialize them.
	 * 
	 * @param parent
	 */
    private void paintUi(Composite parent) {
        city.setText(weatherForecastInformation.getCity());
        date.setText(weatherForecastInformation.getCurrentDateTime().toString());
        wind.setText(weatherCurrentCondition.getWindCondition());
        currentCondition.setText(weatherCurrentCondition.getCondition());
        Image imgIcon = getImage(WeatherProvider.baseUrlImages + weatherCurrentCondition.getIcon());
        currentIcon.setImage(imgIcon);
        for (WeatherForecastConditions weatherDataForecast : weatherForecasts) {
            high = new Label(parent, SWT.BORDER);
            low = new Label(parent, SWT.BORDER);
            dayOfWeek = new Label(parent, SWT.BORDER);
            condition = new Label(parent, SWT.BORDER);
            icon = new Label(parent, SWT.BORDER);
            high.setToolTipText("high");
            high.setText(weatherDataForecast.getHigh());
            low.setToolTipText("low");
            low.setText(weatherDataForecast.getLow());
            dayOfWeek.setToolTipText("day of week");
            dayOfWeek.setText(weatherDataForecast.getDayOfWeek());
            condition.setToolTipText("condition");
            condition.setText(weatherDataForecast.getCondition());
            icon.setImage(getImage(weatherDataForecast.getIcon()));
        }
    }

    private Image getImage(String url) {
        Image img = null;
        try {
            img = new Image(getSite().getShell().getDisplay(), new URL(url).openStream());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return img;
    }

    @Override
    public void setFocus() {
    }

    public Composite getParent() {
        return parent;
    }
}
