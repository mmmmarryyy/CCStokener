package br.com.insight.consultoria.pojos.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 
 */
public class UtilString {

    /**
     * Preenche com Zeros a esquerda, de acordo com a quantidade informada
     * 
     * @param value
     * @param qtd
     * @return
     */
    public static String leftZeros(String value, int qtd) {
        StringBuffer buf = new StringBuffer(qtd);
        if ((value == null) || (value.equals("null"))) {
            for (int i = 0; i < qtd; i++) {
                buf.append("0");
            }
        } else {
            for (int i = 0; i < qtd - value.length(); i++) {
                buf.append("0");
            }
            buf.append(value);
        }
        return buf.toString();
    }

    public static String rigthEspacos(String value, int qtd) {
        StringBuffer buf = new StringBuffer(qtd);
        if (value == null) {
            for (int i = 0; i < qtd; i++) {
                buf.append(" ");
            }
        } else {
            buf.append(value);
            for (int i = 0; i < qtd - value.length(); i++) {
                buf.append(" ");
            }
        }
        return buf.toString();
    }

    /**
     * M�todo usado para trocar ponto por v�rgula e v�rgula por ponto
     * @param valor
     * @return
     */
    public static String trocaVirgulaPonto(String valor) {
        valor.replace(',', '@');
        valor.replace('.', '.');
        valor.replace('@', ',');
        return valor;
    }

    public static boolean ehVazioOuNaoExiste(String valor) {
        return valor == null || valor.trim().equals("");
    }

    /***
     * M�todo utilizado para retirar o caracter  
     * da String e retirar os espa�os entre os caracteres restantes 
     * */
    public static String remove(String str, char oldChar) {
        String sp[] = str.replace(oldChar, ' ').split(" ");
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < sp.length; i++) {
            buffer.append(sp[i]);
        }
        return buffer.toString();
    }

    /**
    * Converte o valor double em uma string com n(casas decimais)
    * @param value 
    * @return String formatada
    */
    public static String doubleToString(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return "--";
        }
        if (value == 0) {
            return "--";
        }
        Locale locale = new Locale("pt", "BR");
        DecimalFormat nft = (DecimalFormat) NumberFormat.getInstance(locale);
        nft.applyPattern("#,##0.00");
        return nft.format(value);
    }

    public static String removePonto(String valor) {
        if (valor != null) {
            return valor.replaceAll("\\p{Punct}", "");
        } else {
            return "0";
        }
    }

    public static String getBarraTitulo() {
        String barraNavegacao = "http://www.caixa.gov.br";
        HttpURLConnection connection;
        try {
            URL url = new URL(barraNavegacao);
            connection = (HttpURLConnection) (url.openConnection());
            connection.getInputStream();
            return barraNavegacao;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return barraNavegacao;
    }

    public static void main(String[] args) {
        System.out.println(UtilString.getBarraTitulo());
    }
}
