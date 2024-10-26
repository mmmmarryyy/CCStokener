package de.hpi.eworld.extensions;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import com.trolltech.qt.core.Qt;
import com.trolltech.qt.gui.QAction;
import com.trolltech.qt.gui.QDialog;
import com.trolltech.qt.gui.QGridLayout;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QLineEdit;
import com.trolltech.qt.gui.QMenu;
import com.trolltech.qt.gui.QMessageBox;
import com.trolltech.qt.gui.QPixmap;
import com.trolltech.qt.gui.QPushButton;
import com.trolltech.qt.gui.QWidget;
import de.hpi.eworld.gui.eventlisteners.EventListener;
import de.hpi.eworld.gui.eventlisteners.MainApp;
import de.hpi.eworld.osm2model.Osm2Model;

/**
 * @author Christian Holz
 *
 */
public class WebImport extends QDialog implements EventListener {

    private QLineEdit minLat;

    private QLineEdit maxLat;

    private QLineEdit minLong;

    private QLineEdit maxLong;

    private QLineEdit osmURL;

    private QWidget dataWidget;

    private QWidget downloadWidget;

    private QGridLayout mylayout;

    public WebImport(QWidget parent) {
        super(parent);
        setWindowTitle("Import data from OpenStreetmap.org");
        resize(400, 200);
        mylayout = new QGridLayout(this);
        setLayout(mylayout);
        dataWidget = new QWidget(this);
        QGridLayout layout = new QGridLayout(dataWidget);
        layout.setMargin(5);
        mylayout.setMargin(15);
        dataWidget.setLayout(layout);
        layout.addWidget(new QLabel("Enter coordinates:"), 0, 0, 1, 4);
        layout.addWidget(new QLabel("min lat:"), 1, 0);
        layout.addWidget(new QLabel("max lat:"), 2, 0);
        layout.addWidget(new QLabel("min long:"), 1, 2);
        layout.addWidget(new QLabel("max long:"), 2, 2);
        layout.addWidget(new QLabel("Alternatively, paste OpenStreetmap.org link:"), 4, 0, 1, 4);
        minLat = new QLineEdit(this);
        maxLat = new QLineEdit(this);
        minLong = new QLineEdit(this);
        maxLong = new QLineEdit(this);
        layout.addWidget(minLat, 1, 1);
        layout.addWidget(maxLat, 2, 1);
        layout.addWidget(minLong, 1, 3);
        layout.addWidget(maxLong, 2, 3);
        minLat.textEdited.connect(this, "coordinatesChanged()");
        maxLat.textEdited.connect(this, "coordinatesChanged()");
        minLong.textEdited.connect(this, "coordinatesChanged()");
        maxLong.textEdited.connect(this, "coordinatesChanged()");
        layout.setColumnStretch(1, 1);
        layout.setColumnStretch(3, 1);
        layout.setRowStretch(3, 1);
        osmURL = new QLineEdit(this);
        layout.addWidget(osmURL, 5, 0, 1, 4);
        osmURL.textEdited.connect(this, "urlChanged()");
        mylayout.addWidget(dataWidget, 0, 0, 1, 4);
        QPushButton bOk = new QPushButton("Download", this);
        QPushButton bCancel = new QPushButton("Cancel", this);
        bOk.setDefault(true);
        bOk.setMinimumWidth(100);
        bCancel.setMinimumWidth(100);
        mylayout.setRowStretch(1, 1);
        mylayout.setRowMinimumHeight(3, 5);
        mylayout.addWidget(bOk, 2, 0, 1, 2, Qt.AlignmentFlag.AlignRight);
        mylayout.addWidget(bCancel, 2, 2, 1, 2, Qt.AlignmentFlag.AlignLeft);
        bOk.clicked.connect(this, "downloadClicked()");
        bCancel.clicked.connect(this, "cancelClicked()");
        downloadWidget = new QWidget(this);
        downloadWidget.hide();
        QGridLayout dlayout = new QGridLayout(downloadWidget);
        downloadWidget.setLayout(dlayout);
        dlayout.addWidget(new QLabel("downloading map data from OSM"), 0, 0, 1, 1, Qt.AlignmentFlag.AlignCenter);
    }

    public void eventTriggered(EventType type, Map<String, Object> data) {
    }

    private MainApp mainapp;

    public void startUp(MainApp app) {
        mainapp = app;
        QMenu menu = app.addMenu("OSM");
        QAction action = new QAction(menu.tr("Import from OpenStreetmap.org"), (QWidget) app);
        menu.addAction(action);
        action.triggered.connect(this, "importFromOSM()");
    }

    public void importFromOSM() {
        minLat.clear();
        maxLat.clear();
        minLong.clear();
        maxLong.clear();
        osmURL.clear();
        osmURL.setFocus();
        show();
    }

    public void downloadClicked() {
        String s_url;
        try {
            double minlat = Double.parseDouble(minLat.text());
            double maxlat = Double.parseDouble(maxLat.text());
            double minlong = Double.parseDouble(minLong.text());
            double maxlong = Double.parseDouble(maxLong.text());
            s_url = "http://www.openstreetmap.org/api/0.5/map?bbox=" + minlong + "," + minlat + "," + maxlong + "," + maxlat;
        } catch (Exception e) {
            QMessageBox.critical(this, "Coordinates Error", "Please check the coordinates entered. Make sure to use proper float values.");
            return;
        }
        try {
            mylayout.removeWidget(dataWidget);
            dataWidget.hide();
            mylayout.addWidget(downloadWidget, 0, 0, 1, 4);
            downloadWidget.show();
            repaint();
            update();
            URL url = new URL(s_url);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            new Osm2Model(con.getInputStream());
            mainapp.setStatusbarText("OSM data successful imported", 1000);
            mainapp.activateMapDisplay();
            hide();
        } catch (MalformedURLException e) {
            QMessageBox.critical(this, "OSM import failed", "Data could not be retrieved as download URL is erroneos.");
        } catch (IOException e) {
            QMessageBox.critical(this, "OSM import failed", "I/O error, aborting.");
        }
        mylayout.removeWidget(downloadWidget);
        downloadWidget.hide();
        mylayout.addWidget(dataWidget, 0, 0, 1, 4);
        dataWidget.show();
    }

    public void cancelClicked() {
        hide();
    }

    public void urlChanged() {
        String url = osmURL.text();
        int i = url.indexOf("?");
        if (i == -1) {
            return;
        }
        String[] args = url.substring(i + 1).split("&");
        HashMap<String, String> map = new HashMap<String, String>();
        int eq;
        for (String arg : args) {
            eq = arg.indexOf("=");
            if (eq != -1) {
                map.put(arg.substring(0, eq), arg.substring(eq + 1));
            }
        }
        try {
            if (map.containsKey("bbox")) {
                String bbox[] = map.get("bbox").split(",");
                minLat.setText(bbox[1]);
                minLong.setText(bbox[0]);
                maxLat.setText(bbox[3]);
                maxLong.setText(bbox[2]);
            } else {
                double size = 180.0 / Math.pow(2, Integer.parseInt(map.get("zoom")));
                minLat.setText(Double.toString(parseDouble(map, "lat") - size / 2));
                minLong.setText(Double.toString(parseDouble(map, "lon") - size));
                maxLat.setText(Double.toString(parseDouble(map, "lat") + size / 2));
                maxLong.setText(Double.toString(parseDouble(map, "lon") + size));
            }
        } catch (NumberFormatException x) {
        } catch (NullPointerException x) {
        }
    }

    public void coordinatesChanged() {
        double minlat;
        double maxlat;
        double minlong;
        double maxlong;
        try {
            minlat = Double.parseDouble(minLat.text());
            maxlat = Double.parseDouble(maxLat.text());
            minlong = Double.parseDouble(minLong.text());
            maxlong = Double.parseDouble(maxLong.text());
            double lat = (minlat + maxlat) / 2;
            double lon = (minlong + maxlong) / 2;
            double latMin = Math.log(Math.tan(Math.PI / 4.0 + minlat / 180.0 * Math.PI / 2.0)) * 180.0 / Math.PI;
            double latMax = Math.log(Math.tan(Math.PI / 4.0 + maxlat / 180.0 * Math.PI / 2.0)) * 180.0 / Math.PI;
            double size = Math.max(Math.abs(latMax - latMin), Math.abs(maxlong - minlong));
            int zoom = 0;
            while (zoom <= 20) {
                if (size >= 180) {
                    break;
                }
                size *= 2;
                zoom++;
            }
            osmURL.setText("http://www.openstreetmap.org/index.html?mlat=" + lat + "&mlon=" + lon + "&zoom=" + zoom);
        } catch (Exception e) {
        }
    }

    private static double parseDouble(HashMap<String, String> map, String key) {
        if (map.containsKey(key)) {
            return Double.parseDouble(map.get(key));
        }
        return Double.parseDouble(map.get("m" + key));
    }
}
