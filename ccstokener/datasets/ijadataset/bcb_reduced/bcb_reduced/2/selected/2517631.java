package parsers;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import beans.CotacoesHtml;

public class XLSParser {

    String path;

    /**
     * Método que baixa cotações do site da bovespa.
     * @param paramBusca - Códigos das ações cujas cotações se deseja, separados
     * por um sinal de "+".
     * @param caminhoArquivo - String com o caminho obtido no contexto da aplicação.
     */
    public void xlsLoader(String paramBusca, String caminhoArquivo) {
        try {
            path = caminhoArquivo;
            OutputStream out = new FileOutputStream(path + "/Cotacoes.txt", false);
            URL url = new URL("http://br.finance.yahoo.com/d/quotes.csv?s=" + paramBusca + "&f=sl1d1t1c1ohgv&e=.csv");
            URLConnection conn = url.openConnection();
            InputStream in = conn.getInputStream();
            int i = 0;
            while ((i = in.read()) != -1) {
                out.write(i);
            }
            in.close();
            out.close();
            System.out.println("XLS baixado com sucesso!");
        } catch (Exception e) {
            System.out.println("Erro ao baixar o xls!");
        }
    }

    public ArrayList<CotacoesHtml> doXLSParse() {
        String[] columns;
        ArrayList<CotacoesHtml> arrayCotacoes = new ArrayList<CotacoesHtml>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path + "/Cotacoes.txt"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                columns = line.split("\\;");
                CotacoesHtml cot = new CotacoesHtml();
                cot.setCodAcao(columns[0]);
                cot.setDia(columns[2]);
                cot.setHora(columns[3]);
                cot.setAbertura(new BigDecimal(columns[1]));
                cot.setMinimo(new BigDecimal(columns[7]));
                cot.setMaximo(new BigDecimal(columns[6]));
                cot.setAtual(new BigDecimal(columns[5]));
                cot.setVariacao(new BigDecimal(columns[4]));
                cot.setQuantidade(Integer.parseInt(columns[8]));
                arrayCotacoes.add(cot);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arrayCotacoes;
    }
}
