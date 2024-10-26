package edu.chop.bic.cnv.ui;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.commons.dbcp.BasicDataSource;
import java.io.Serializable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import edu.chop.bic.cnv.session.MySession;
import edu.chop.bic.cnv.domain.CustomParameters;

public class Ploidy extends Panel implements Serializable {

    private String source = "";

    @SpringBean
    private CustomParameters customParameters;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Ploidy(String id, String source) {
        super(id);
        setSource(source);
        Label label = new Label("ploidy", new LoadableDetachableModel() {

            @Override
            protected Object load() {
                return getPloidy(getSource());
            }
        });
        label.setEscapeModelStrings(false);
        add(label);
    }

    public String getPloidy(String source) {
        StringBuilder ploidyHtml = new StringBuilder();
        String hyperdiploidyUrl = customParameters.getHyperdiploidyUrl();
        String urlString = hyperdiploidyUrl + "?source=" + source;
        URL url = null;
        try {
            url = new URL(urlString);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = "";
            while ((line = in.readLine()) != null) {
                ploidyHtml.append(line);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ploidyHtml.toString();
    }

    public CustomParameters getCustomParameters() {
        return customParameters;
    }

    public void setCustomParameters(CustomParameters customParameters) {
        this.customParameters = customParameters;
    }
}
