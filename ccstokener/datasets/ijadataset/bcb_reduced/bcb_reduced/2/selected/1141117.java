package br.com.invest.action;

import br.com.invest.model.Acao;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Scope;

@Scope(ScopeType.SESSION)
public class BuscarCotacao_OLD {

    private String cotacao;

    @org.jboss.seam.annotations.datamodel.DataModel("cotacoesDM")
    private List<Properties> cotacoes;

    private List<String> nomesCotacoes;

    @In
    private Session investDB;

    @Factory("cotacoesDM")
    public String initCotacao() {
        gerarListaCotacoes();
        return null;
    }

    public void init() {
        List<Acao> listaAcoes = investDB.createCriteria(Acao.class).addOrder(Order.asc("cod_acao")).list();
        if (listaAcoes != null) {
            for (Acao a : listaAcoes) {
                gerarCotacao(a.getPrefixo());
            }
        }
    }

    private Properties gerarCotacao(String nome) {
        Properties result = new Properties();
        try {
            String[] siglaNome = nome.split("-");
            URL url = new URL("http://br.finance.yahoo.com/d/quotes.csv?s=" + siglaNome[0] + "&f=sl1d1t1c1ohgv&e=.csv");
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
            result.setProperty("Sigla", siglaNome[0]);
            result.setProperty("Nome", siglaNome[1]);
            result.setProperty("Acao", s[0]);
            result.setProperty("Ultima transacao", s[1]);
            result.setProperty("Data", s[2]);
            result.setProperty("Hora", s[3]);
            if (s[4].charAt(0) == '+') {
                result.setProperty("Percentual", "<span style='color:blue'>" + s[4] + "</span>");
            } else if (s[4].charAt(0) == '-') {
                result.setProperty("Percentual", "<span style='color:red'>" + s[4] + "</span>");
            } else {
                result.setProperty("Percentual", s[4]);
            }
            result.setProperty("Valor de abertura", s[5]);
            result.setProperty("Máximo de venda", s[6]);
            result.setProperty("Minimo de venda", s[7]);
            result.setProperty("Volume", s[8]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private void gerarListaCotacoes() {
        nomesCotacoes = new ArrayList<String>(0);
        nomesCotacoes.add("ALLL11.SA-ALL AMER LAT");
        nomesCotacoes.add("AMBV4.SA-AMBEV PN");
        nomesCotacoes.add("ARCZ6.SA-ARACRUZ PNB N1");
        nomesCotacoes.add("BTOW3.SA-B2W VAREJO ON NM");
        nomesCotacoes.add("BVMF3.SA-BMF BOVESPA ON EDJ NM");
        nomesCotacoes.add("BBDC4.SA-BRADESCO PN N1");
        cotacoes = new ArrayList<Properties>(0);
        for (String nc : nomesCotacoes) {
            cotacoes.add(gerarCotacao(nc));
        }
    }

    public String getCotacao() {
        return cotacao;
    }

    public void setCotacao(String cotacao) {
        this.cotacao = cotacao;
    }

    public List<Properties> getCotacoes() {
        return cotacoes;
    }

    public void setCotacoes(List<Properties> cotacoes) {
        this.cotacoes = cotacoes;
    }

    public List<String> getNomesCotacoes() {
        return nomesCotacoes;
    }

    public void setNomesCotacoes(List<String> nomesCotacoes) {
        this.nomesCotacoes = nomesCotacoes;
    }
}
