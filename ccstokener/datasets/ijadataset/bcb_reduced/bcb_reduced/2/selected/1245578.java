package org.reprap.scanning.FileIO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;
import java.util.ArrayList;
import org.reprap.scanning.Geometry.Ellipse;
import org.reprap.scanning.Geometry.Point2d;

public class CalibrationSheetProperties {

    public int width;

    public int height;

    public double circleradius;

    public Ellipse[] calibrationcircles;

    private String filename;

    public CalibrationSheetProperties(String file) {
        width = 0;
        height = 0;
        circleradius = 0;
        calibrationcircles = new Ellipse[0];
        filename = file;
    }

    public void load() throws IOException {
        File file = new File(filename);
        URL url = file.toURI().toURL();
        Properties temp = new Properties();
        temp.load(url.openStream());
        if (temp.getProperty("Width") != null) try {
            width = Integer.valueOf(temp.getProperty("Width"));
        } catch (Exception e) {
            System.out.println("Error loading Calibration Sheet Width - leaving as default: " + e);
        }
        if (temp.getProperty("Height") != null) try {
            height = Integer.valueOf(temp.getProperty("Height"));
        } catch (Exception e) {
            System.out.println("Error loading Calibration Sheet Height - leaving as default: " + e);
        }
        if (temp.getProperty("CircleRadius") != null) try {
            circleradius = Double.valueOf(temp.getProperty("CircleRadius"));
        } catch (Exception e) {
            System.out.println("Error loading Calibration Sheet Circle Radius - leaving as default: " + e);
        }
        ArrayList<Ellipse> calibrationcircleslist = new ArrayList<Ellipse>();
        int i = 0;
        while ((temp.getProperty("Circle" + i + "CenterX") != null) && (temp.getProperty("Circle" + i + "CenterY") != null)) {
            Point2d circlecenter = new Point2d(0, 0);
            circlecenter.x = Double.valueOf(temp.getProperty("Circle" + i + "CenterX"));
            circlecenter.y = Double.valueOf(temp.getProperty("Circle" + i + "CenterY"));
            Ellipse element = new Ellipse(circlecenter, circleradius, circleradius, 0);
            calibrationcircleslist.add(element);
            i++;
        }
        calibrationcircles = new Ellipse[0];
        calibrationcircles = calibrationcircleslist.toArray(calibrationcircles);
    }

    public void save() throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            File p = new File(file.getParent());
            if (!p.isDirectory()) {
                p.mkdirs();
            }
        }
        OutputStream output = new FileOutputStream(file);
        Properties temp = new Properties();
        temp.setProperty("Width", String.valueOf(width));
        temp.setProperty("Height", String.valueOf(height));
        temp.setProperty("CircleRadius", String.valueOf(circleradius));
        for (int i = 0; i < calibrationcircles.length; i++) {
            Point2d center = calibrationcircles[i].GetCenter();
            temp.setProperty("Circle" + i + "CenterX", String.valueOf(center.x));
            temp.setProperty("Circle" + i + "CenterY", String.valueOf(center.y));
        }
        String comments = "Carapace-Copier Calibration Sheet properties http://sourceforge.net/projects/carapace-copier/ - can be edited by hand but not recommended as elements may be reordered by the program\n";
        temp.store(output, comments);
    }
}
