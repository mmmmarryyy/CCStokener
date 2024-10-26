package br.com.invest.action;

import br.com.invest.model.Acao;
import br.com.invest.model.Cotacao;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;

/**
 *
 * @author ceatis
 */
@Name("CotacaoController")
@Scope(ScopeType.SESSION)
public class BuscarCotacaoNOVO {

    @DataModel("cotacoesDM")
    private List<String> listaAcoes;

    private Acao acao;

    @In
    private Session investDB;

    private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

    private SimpleDateFormat sdfHora = new SimpleDateFormat("h:mma");

    @Factory("cotacoesDM")
    public String buscarCotacao() {
        List<Acao> la = investDB.createCriteria(Acao.class).addOrder(Order.asc("prefixo")).list();
        listaAcoes = new ArrayList<String>(0);
        for (Acao a : la) {
            listaAcoes.add(a.getPrefixo());
            try {
                URL url = new URL("http://br.finance.yahoo.com/d/quotes.csv?s=" + a.getPrefixo() + "&f=sl1d1t1c1ohgv&e=.csv");
                System.out.println("ACAO: " + a.getPrefixo());
                URLConnection con = url.openConnection();
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine = null;
                inputLine = br.readLine();
                br.close();
                String s[] = null;
                if (inputLine.contains(",")) {
                    s = inputLine.split(",");
                } else {
                    s = inputLine.split(";");
                }
                Cotacao cotacoes = new Cotacao();
                Calendar calendario = new GregorianCalendar();
                Date data = new Date();
                calendario.setTime(data);
                cotacoes.setDataCotacao(calendario.getTime());
                cotacoes.setHora(sdfHora.parse(s[3]));
                cotacoes.setMaximo(Double.parseDouble(s[6]));
                cotacoes.setMinimo(Double.parseDouble(s[7]));
                cotacoes.setPercentual(Float.parseFloat(s[4]));
                cotacoes.setUltimaCotacao(Double.parseDouble(s[1]));
                cotacoes.setValorAbertura(Double.parseDouble(s[5]));
                cotacoes.setValorAbertura(Double.parseDouble(s[8]));
                a.addCotacao(cotacoes);
                investDB.save(a);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String getCotacoes() {
        return null;
    }

    public Acao getAcao() {
        return acao;
    }

    public void setAcao(Acao acao) {
        this.acao = acao;
    }

    public List<String> getListaAcoes() {
        return listaAcoes;
    }

    public void setListaAcoes(List<String> listaAcoes) {
        this.listaAcoes = listaAcoes;
    }
}
