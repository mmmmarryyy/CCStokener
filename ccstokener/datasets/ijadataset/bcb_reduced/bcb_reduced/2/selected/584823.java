package com.bryanbibat.wave;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.wave.api.AbstractRobotServlet;
import com.google.wave.api.Blip;
import com.google.wave.api.Event;
import com.google.wave.api.EventType;
import com.google.wave.api.Image;
import com.google.wave.api.Range;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.api.TextView;
import com.google.wave.api.Wavelet;

@SuppressWarnings("serial")
public class WatexyJavaServlet extends AbstractRobotServlet {

    private static final String WELCOME_MESSAGE = "Hi. My name is Watexy and I'm here to help you presenting LaTeX in waves. " + "Just put the latex between $$ and $$, e.g. $$2+2=5$$. This robot uses the " + "http://www.forkosh.dreamhost.com/source_mathtex.html#webservice service. \n\n" + "(Note that your text/formulas will get all wonky when submit multiple formulas " + "in one go because of a bug described here: " + "http://code.google.com/p/google-wave-resources/issues/detail?id=176.)";

    private static final Logger log = Logger.getLogger(WatexyJavaServlet.class.getName());

    @Override
    public void processEvents(RobotMessageBundle bundle) {
        Wavelet wavelet = bundle.getWavelet();
        if (bundle.wasSelfAdded()) {
            Blip blip = wavelet.appendBlip();
            TextView textView = blip.getDocument();
            textView.append(WELCOME_MESSAGE);
        }
        log.info("Event processed");
        for (Event e : bundle.getEvents()) {
            if (e.getType() == EventType.BLIP_SUBMITTED) {
                Blip blip = e.getBlip();
                TextView textView = blip.getDocument();
                Pattern latex_regex = Pattern.compile("\\$\\$(.+?)\\$\\$");
                String text = textView.getText().replaceAll("[\\s]", " ");
                Matcher matcher = latex_regex.matcher(text);
                boolean hasMatch = matcher.find();
                log.info(text);
                while (hasMatch) {
                    textView.replace(new Range(matcher.start(), matcher.start() + 1), "x");
                    textView.replace(new Range(matcher.end() - 1, matcher.end()), "x");
                    String latex = "http://www.forkosh.dreamhost.com/mathtex.cgi?" + matcher.group(1);
                    String imgUrl = latex.length() > 60 ? getShortUrl(latex) : latex;
                    if (imgUrl.length() > 0) {
                        Image image = new Image();
                        image.setUrl(imgUrl);
                        textView.insertElement(matcher.end(), image);
                    }
                    textView.setAnnotation(new Range(matcher.start(), matcher.end()), "style/color", "rgb(204, 204, 204)");
                    textView.setAnnotation(new Range(matcher.start(), matcher.end()), "style/fontSize", "0.80em");
                    log.info(imgUrl);
                    text = textView.getText().replaceAll("[\\s]", " ");
                    matcher = latex_regex.matcher(text);
                    hasMatch = matcher.find();
                    log.info(text);
                }
                log.info(textView.getText());
            }
            log.info("end");
        }
    }

    public String getShortUrl(String urlName) {
        try {
            URL url = new URL("http://is.gd/api.php?longurl=" + URLEncoder.encode(urlName, "UTF-8"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                return reader.readLine();
            } else {
                return "";
            }
        } catch (Exception e) {
            log.severe("Exception encountered: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }
}
