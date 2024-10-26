package parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import beans.Acoes;
import beans.Companhias;
import dao.ConexaoBD;
import java.util.Iterator;
import java.util.Set;

public class TitulosNegociaveis {

    public void ObtemArquivoTxt() {
        try {
            String userDir = System.getProperty("user.dir");
            File file = new File(userDir + "/build/web/WEB-INF/classes/teste/arquivosBaixados/Titulos_Negociaveis.txt");
            OutputStream out = new FileOutputStream(file, false);
            URL url = new URL("http://www.bovespa.com.br/suplemento/ExecutaAcaoDownload.asp?arquivo=Titulos_Negociaveis.txt");
            URLConnection conn = url.openConnection();
            InputStream in = conn.getInputStream();
            int i = 0;
            while ((i = in.read()) != -1) {
                out.write(i);
            }
            in.close();
            out.close();
            System.out.println("Download efetuado com sucesso");
        } catch (FileNotFoundException e) {
            System.out.println("Arquivo não encontrado. Causa: " + e.getMessage());
        } catch (MalformedURLException e) {
            System.out.println("Erro na formatação da URL. Causa: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erro de entrada/saida de dados. Causa: " + e.getMessage());
        }
    }

    public void ParseTitulosNegociaveis(HashMap<String, Companhias> listaDeCompanhias, HashMap<String, ArrayList<Acoes>> hashListaDeAcoes) {
        try {
            String userDir = System.getProperty("user.dir");
            FileReader file = new FileReader(userDir + "/build/web/WEB-INF/classes/teste/arquivosBaixados/Titulos_Negociaveis.txt");
            BufferedReader in = new BufferedReader(file);
            while (in.ready()) {
                String registro = in.readLine();
                if (registro.length() == 220) {
                    String headerStr = registro.substring(0, 2);
                    Integer header = Integer.parseInt(headerStr);
                    switch(header) {
                        case 0:
                            String dataDoPregao = registro.substring(30, 40);
                            break;
                        case 1:
                            String codCompanhia = registro.substring(02, 06);
                            listaDeCompanhias.put(codCompanhia, new Companhias());
                            listaDeCompanhias.get(codCompanhia).setCodCompanhia(codCompanhia);
                            String nomeCompanhia = registro.substring(06, 66);
                            listaDeCompanhias.get(codCompanhia).setNomeCompanhia(nomeCompanhia);
                            String nomeReduzido = registro.substring(66, 78);
                            listaDeCompanhias.get(codCompanhia).setNomeReduzido(nomeReduzido);
                            break;
                        case 2:
                            String codigoDoMercado = registro.substring(108, 111);
                            if (codigoDoMercado.compareTo("010") == 0) {
                                String codigoDaEmpresa2 = registro.substring(14, 18);
                                ArrayList<Acoes> listaDeAcoesPorCompanhia = hashListaDeAcoes.get(codigoDaEmpresa2);
                                if (listaDeAcoesPorCompanhia == null) {
                                    listaDeAcoesPorCompanhia = new ArrayList<Acoes>();
                                    hashListaDeAcoes.put(codigoDaEmpresa2, listaDeAcoesPorCompanhia);
                                    if (listaDeCompanhias.get(codigoDaEmpresa2) != null) {
                                        listaDeCompanhias.get(codigoDaEmpresa2).setAcoesList(listaDeAcoesPorCompanhia);
                                    }
                                }
                                hashListaDeAcoes.get(codigoDaEmpresa2).add(new Acoes());
                                String codAcao = registro.substring(2, 14);
                                hashListaDeAcoes.get(codigoDaEmpresa2).get(hashListaDeAcoes.get(codigoDaEmpresa2).size() - 1).setCodAcao(codAcao);
                                String tipo = registro.substring(133, 136);
                                hashListaDeAcoes.get(codigoDaEmpresa2).get(hashListaDeAcoes.get(codigoDaEmpresa2).size() - 1).setTipo(tipo);
                                hashListaDeAcoes.get(codigoDaEmpresa2).get(hashListaDeAcoes.get(codigoDaEmpresa2).size() - 1).setCodCompanhia(listaDeCompanhias.get(codigoDaEmpresa2));
                            }
                            break;
                        case 9:
                            break;
                        default:
                            System.out.println("Header inexistente ");
                            break;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Arquivo não encontrado. Causa: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Erro de entrada/saida de dados. Causa: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void imprimeBeans(HashMap<String, Companhias> listaDeCompanhias, HashMap<String, ArrayList<Acoes>> hashListaDeAcoes) {
        System.out.println(listaDeCompanhias.values());
        System.out.println(hashListaDeAcoes.values());
    }

    public void insereCompanhiasNoBanco(HashMap<String, Companhias> listaDeCompanhias) {
        Set<String> setChaves = listaDeCompanhias.keySet();
        Iterator<String> cursor = setChaves.iterator();
        ConexaoBD conexaoBD = new ConexaoBD();
        while (cursor.hasNext()) {
            String cod = cursor.next();
            conexaoBD.inserirCompanhia(listaDeCompanhias.get(cod));
        }
    }
}
