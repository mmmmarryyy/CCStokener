import java.applet.Applet;
import javax.swing.JLabel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;

public class AppletGraph extends Applet {

    private ChartPanel chartPanel;

    private TimeSeries free;

    public void init() {
        setBackground(new Color(204, 204, 204));
        this.free = new TimeSeries("Free", Millisecond.class);
        this.free.setMaximumItemCount(10);
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(free);
        DateAxis domain = new DateAxis("Time");
        NumberAxis range = new NumberAxis("Memory");
        XYItemRenderer renderer = new DefaultXYItemRenderer();
        renderer.setSeriesPaint(0, Color.red);
        renderer.setSeriesPaint(1, Color.green);
        renderer.setBaseStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        XYPlot xyplot = new XYPlot(dataset, domain, range, renderer);
        xyplot.setBackgroundPaint(Color.black);
        domain.setAutoRange(true);
        domain.setLowerMargin(0.0);
        domain.setUpperMargin(0.0);
        domain.setTickLabelsVisible(true);
        range.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, xyplot, true);
        chart.setBackgroundPaint(new Color(204, 204, 204));
        chartPanel = new ChartPanel(chart);
        add(chartPanel);
    }

    class DataGenerator extends javax.swing.Timer implements ActionListener {

        DataGenerator() {
            super(2000, null);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent event) {
            long result = Long.parseLong(connectToServlet());
            free.add(new Millisecond(), result);
        }
    }

    public void start() {
        this.new DataGenerator().start();
    }

    public String connectToServlet() {
        URL urlStory = null;
        BufferedReader brStory;
        String result = "";
        try {
            urlStory = new URL(getCodeBase(), "http://localhost:8080/javawebconsole/ToApplet");
        } catch (MalformedURLException MUE) {
            MUE.printStackTrace();
        }
        try {
            brStory = new BufferedReader(new InputStreamReader(urlStory.openStream()));
            while (brStory.ready()) {
                result += brStory.readLine();
            }
        } catch (IOException IOE) {
            IOE.printStackTrace();
        }
        return result;
    }
}
