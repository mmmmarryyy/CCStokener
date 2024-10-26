package org.reprap.scanning.FileIO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;
import org.reprap.scanning.Geometry.LineSegment2D;
import org.reprap.scanning.Geometry.Point2d;

public class InitialLineSegmentForTextureMatching {

    public LineSegment2D initialline;

    public int referenceimage;

    private String filename;

    public InitialLineSegmentForTextureMatching(String file) {
        initialline = new LineSegment2D(new Point2d(), new Point2d());
        filename = file;
        referenceimage = -1;
    }

    public void loadProperties() throws IOException {
        File file = new File(filename);
        URL url = file.toURI().toURL();
        Properties temp = new Properties();
        temp.load(url.openStream());
        Point2d start = new Point2d();
        Point2d end = new Point2d();
        if (temp.getProperty("StartX") != null) try {
            start.x = Double.valueOf(temp.getProperty("StartX"));
        } catch (Exception e) {
            System.out.println("Error loading StartX - leaving as default: " + e);
        }
        if (temp.getProperty("StartY") != null) try {
            start.y = Double.valueOf(temp.getProperty("StartY"));
        } catch (Exception e) {
            System.out.println("Error loading StartY - leaving as default: " + e);
        }
        if (temp.getProperty("EndX") != null) try {
            end.x = Double.valueOf(temp.getProperty("EndX"));
        } catch (Exception e) {
            System.out.println("Error loading EndX - leaving as default: " + e);
        }
        if (temp.getProperty("EndY") != null) try {
            end.y = Double.valueOf(temp.getProperty("EndY"));
        } catch (Exception e) {
            System.out.println("Error loading EndY - leaving as default: " + e);
        }
        initialline = new LineSegment2D(start, end);
        if (temp.getProperty("ReferenceImage") != null) try {
            referenceimage = Integer.valueOf(temp.getProperty("ReferenceImage"));
        } catch (Exception e) {
            System.out.println("Error loading ReferenceImage - leaving as default: " + e);
        }
    }

    public void saveProperties() throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            File p = new File(file.getParent());
            if (!p.isDirectory()) {
                p.mkdirs();
            }
        }
        OutputStream output = new FileOutputStream(file);
        Properties temp = new Properties();
        temp.setProperty("StartX", String.valueOf(initialline.start.x));
        temp.setProperty("StartY", String.valueOf(initialline.start.y));
        temp.setProperty("EndX", String.valueOf(initialline.end.x));
        temp.setProperty("EndY", String.valueOf(initialline.end.y));
        temp.setProperty("ReferenceImage", String.valueOf(referenceimage));
        String comments = "Carapace Copier initial line segment for texture matching properties http://sourceforge.net/projects/carapace-copier/ - can be edited by hand but not recommended as elements may be reordered by the program\n";
        temp.store(output, comments);
    }
}
