package testes.jfreechart;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xml.DatasetReader;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/**
 * A simple demonstration application showing how to create a bar chart using data from an
 * XML data file.
 *
 */
public class XMLBarChartDemo extends ApplicationFrame {

    /**
     * Default constructor.
     *
     * @param title  the frame title.
     */
    public XMLBarChartDemo(final String title) {
        super(title);
        CategoryDataset dataset = null;
        final URL url = getClass().getResource("/org/jfree/chart/demo/categorydata.xml");
        try {
            final InputStream in = url.openStream();
            dataset = DatasetReader.readCategoryDatasetFromXML(in);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
        final JFreeChart chart = ChartFactory.createBarChart("Bar Chart", "Domain", "Range", dataset, PlotOrientation.VERTICAL, true, true, false);
        chart.setBackgroundPaint(Color.yellow);
        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        setContentPane(chartPanel);
    }

    /**
     * Starting point for the demonstration application.
     *
     * @param args  ignored.
     */
    public static void main(final String[] args) {
        final XMLBarChartDemo demo = new XMLBarChartDemo("XML Bar Chart Demo");
        demo.pack();
        RefineryUtilities.centerFrameOnScreen(demo);
        demo.setVisible(true);
    }
}
