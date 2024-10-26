package org.ita.capes.pegarIefs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import org.ccil.cowan.tagsoup.Parser;
import org.ita.capes.CurriculumCapesSAXHandler;
import org.ita.capes.ListaProfessoresSAXHandler;
import org.ita.capes.Professor;
import org.ita.capes.Publicacao;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class LeitorIef {

    private Parser parser = new Parser();

    public List<Professor> getProfessores(InputStream is) throws IOException, SAXException {
        ListaProfessoresSAXHandler handler = new ListaProfessoresSAXHandler();
        parser.setContentHandler(handler);
        InputSource input = new InputSource(is);
        parser.parse(input);
        return handler.getLista();
    }

    public List<Publicacao> lerPublicacoes(InputStream is) throws IOException, SAXException {
        CurriculumCapesSAXHandler handler = new CurriculumCapesSAXHandler();
        parser.setContentHandler(handler);
        InputSource input = new InputSource(is);
        parser.parse(input);
        return handler.getPublicacoes();
    }

    public List<Publicacao> lerPublicacoes(String urlLates) throws IOException, SAXException {
        URL url = new URL(urlLates);
        InputStream in = url.openStream();
        return lerPublicacoes(in);
    }
}
