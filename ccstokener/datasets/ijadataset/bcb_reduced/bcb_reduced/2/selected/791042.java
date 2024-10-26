package dmedv.aurora;

import dmedv.grids.*;
import dmedv.tiles.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuroraTileServer extends HttpServlet {

    public static Double[] values = new Double[] { 0d, 1d, 1.5d, 2d, 3d, 3.25d };

    public static Integer[] colors = new Integer[] { 0, 0x0000FF, 0x00FF00, 0xFFFF00, 0xFF0000, 0xFFFFFF };

    public int GetColorFromValue(Double value) {
        int n = Arrays.binarySearch(values, value);
        int i1 = 0, i2 = 0;
        if (n >= 0) {
            return colors[n];
        } else {
            n = ~n;
            if (n >= values.length) {
                return colors[values.length - 1];
            } else if (n == 0) {
                return colors[0];
            } else {
                i2 = n;
                i1 = n - 1;
            }
        }
        Color c1 = new Color(colors[i1], false);
        Color c2 = new Color(colors[i2], false);
        double x = value;
        double x1 = values[i1];
        double x2 = values[i2];
        {
            int r = (int) (((x2 - x) * c1.getRed() + (x - x1) * c2.getRed()) / (x2 - x1));
            int g = (int) (((x2 - x) * c1.getGreen() + (x - x1) * c2.getGreen()) / (x2 - x1));
            int b = (int) (((x2 - x) * c1.getBlue() + (x - x1) * c2.getBlue()) / (x2 - x1));
            return new Color(r, g, b).getRGB();
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            String baseUrl = getInitParameter("OvationPath");
            res.setContentType("image/jpg");
            OutputStream out = res.getOutputStream();
            String s = req.getPathInfo();
            StringTokenizer st = new StringTokenizer(s, "/");
            String filename = st.nextToken();
            String year = filename.substring(0, 4);
            String month = filename.substring(4, 6);
            String day = filename.substring(6, 8);
            filename = baseUrl + "/" + year + "/" + month + "/" + day + "/" + filename;
            double alpha = Double.parseDouble(st.nextToken());
            int z = Integer.parseInt(st.nextToken());
            int x = Integer.parseInt(st.nextToken());
            int y = Integer.parseInt(st.nextToken());
            BufferedImage image = GetTileImage(filename, alpha, x, y, z);
            ImageIO.write(image, "png", out);
            out.close();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private BufferedImage GetTileImage(String filename, double alpha, int x, int y, int z) throws Exception {
        double[] bounds = new double[4];
        PolarTransform.XYZToXYRect(x, y, z, bounds);
        double[] minmax = new double[4];
        double[][][] points = new double[256][256][2];
        PolarTransform.CoordsForImageGeo(alpha, bounds[0], bounds[1], bounds[2], bounds[3], 256, 256, points, minmax);
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        RgbDataGrid dataGrid = GetGridFromFile(filename);
        for (int j = 0; j < 256; j++) {
            for (int i = 0; i < 256; i++) {
                double lat = points[i][j][0];
                double lon = points[i][j][1];
                if (lon < 0) lon = 360 + lon;
                int newcolor = dataGrid.GetInterpolated(lon, lat) & 0xA0FFFFFF;
                image.setRGB(i, j, newcolor);
            }
        }
        return image;
    }

    private RgbDataGrid GetGridFromFile(String filename) throws Exception {
        URL url = new URL(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(url.openStream())));
        float maxValue = 0;
        String s = reader.readLine();
        ArrayList<Double> xList = new ArrayList<Double>();
        ArrayList<Double> yList = new ArrayList<Double>();
        ArrayList<Float> data = new ArrayList<Float>();
        while (!(s = reader.readLine()).startsWith("Totals")) {
            StringTokenizer st = new StringTokenizer(s, " ");
            float hr = Float.parseFloat(st.nextToken());
            float lat = Float.parseFloat(st.nextToken());
            float value = Float.parseFloat(st.nextToken());
            if (value > maxValue) maxValue = value;
            float lon = hr / 24 * 360;
            if (xList.indexOf(new Double(lon)) < 0) xList.add(new Double(lon));
            if (yList.indexOf(new Double(lat)) < 0) yList.add(new Double(lat));
            data.add(new Float(value));
        }
        reader.close();
        xList.add(new Double(360));
        Double[] lons = xList.toArray(new Double[0]);
        Double[] lats = yList.toArray(new Double[0]);
        for (int i = 0; i < lats.length; i++) {
            data.add(data.get(i));
        }
        RgbDataGrid grid = new RgbDataGrid(lons, lats);
        for (int i = 0; i < data.size(); i++) {
            Color color = new Color(GetColorFromValue(new Double(data.get(i))));
            grid.Data.add(new Integer(color.getRGB()));
        }
        grid.Transpose = true;
        grid.ReverseY = false;
        return grid;
    }
}
