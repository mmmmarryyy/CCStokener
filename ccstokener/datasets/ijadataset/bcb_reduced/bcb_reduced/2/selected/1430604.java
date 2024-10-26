package org.reprap.scanning.FileIO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;
import org.reprap.scanning.Geometry.Point2d;
import Jama.Matrix;

public class ProcessedImageProperties {

    public Point2d originofimagecoordinates;

    public Matrix WorldToImageTransform;

    public boolean skipprocessing;

    private String filename;

    public ProcessedImageProperties(String file) {
        originofimagecoordinates = new Point2d(0, 0);
        WorldToImageTransform = new Matrix(3, 4);
        filename = file;
        skipprocessing = true;
    }

    public void loadProperties() throws IOException {
        File file = new File(filename);
        URL url = file.toURI().toURL();
        Properties temp = new Properties();
        temp.load(url.openStream());
        if (temp.getProperty("OriginOfImageCoordinatesX") != null) try {
            originofimagecoordinates.x = Double.valueOf(temp.getProperty("OriginOfImageCoordinatesX"));
        } catch (Exception e) {
            System.out.println("Error loading OriginOfImageCoordinatesX - leaving as default: " + e);
        }
        if (temp.getProperty("OriginOfImageCoordinatesY") != null) try {
            originofimagecoordinates.y = Double.valueOf(temp.getProperty("OriginOfImageCoordinatesY"));
        } catch (Exception e) {
            System.out.println("Error loading OriginOfImageCoordinatesY - leaving as default: " + e);
        }
        if (temp.getProperty("SkipProcessing") != null) skipprocessing = temp.getProperty("SkipProcessing").equals("true");
        for (int i = 0; i < 3; i++) for (int j = 0; j < 4; j++) {
            try {
                WorldToImageTransform.set(i, j, Double.valueOf(temp.getProperty("WorldToImageTransformMatrixRow" + i + "Column" + j)));
            } catch (Exception e) {
                System.out.println("Error loading WorldToImageTransformMatrixRow" + i + "Column" + j + " - leaving as default: " + e);
            }
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
        temp.setProperty("OriginOfImageCoordinatesX", String.valueOf(originofimagecoordinates.x));
        temp.setProperty("OriginOfImageCoordinatesY", String.valueOf(originofimagecoordinates.y));
        temp.setProperty("SkipProcessing", String.valueOf(skipprocessing));
        for (int i = 0; i < 3; i++) for (int j = 0; j < 4; j++) {
            temp.setProperty("WorldToImageTransformMatrixRow" + i + "Column" + j + "", String.valueOf(WorldToImageTransform.get(i, j)));
        }
        String comments = "Carapace Copier processed image properties http://sourceforge.net/projects/carapace-copier/ - can be edited by hand but not recommended as elements may be reordered by the program\n";
        temp.store(output, comments);
    }
}
