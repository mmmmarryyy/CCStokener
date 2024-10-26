package tirateima.main;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import tirateima.gui.Principal;
import tirateima.gui.arquivos.AbstractArquivo;

/**
           Exemplo de tag para se colocar no HTML:

<applet code="br.unb.cic.algostep.main.Applet" archive="tirateima.jar" height="600" width="800">

<param name="arq_fonte" value="[nome do arquivo fonte]">
<param name="arq_texto" value="[nome do arquivo txt]">
<param name="modo" value="['janela' ou 'applet']">

</applet>
 */
@SuppressWarnings("serial")
public class Applet extends java.applet.Applet {

    /** Um cache com as entradas que nós já vimos até agora. Salvamos
	 *  a URL do arquivo de passos como chave. */
    private HashMap<URL, Principal> entradas = new HashMap<URL, Principal>();

    /** Um cache de janelas (visíveis ou não). Usado apenas pelo applet. */
    private HashMap<URL, Programa> janelas = null;

    /**
	 * Normaliza uma URL. 
	 */
    public URL normalizarURL(String url) throws MalformedURLException {
        if (!url.toLowerCase().startsWith("http://")) return new URL(getCodeBase() + url); else return new URL(url);
    }

    /**
	 * Retorna um Reader correspondente a uma URL (relativa ou absoluta).
	 */
    public Reader getArquivo(URL url) throws Exception {
        return new InputStreamReader(url.openConnection().getInputStream());
    }

    /**
	 * Prepara em background uma entrada. Isso significa baixar todos os
	 * arquivos e interpretá-los.
	 */
    public void prepararEntrada(String str_fonte, String str_texto) throws MalformedURLException {
        prepararEntrada(normalizarURL(str_fonte), normalizarURL(str_texto));
    }

    /**
	 * Prepara em background uma entrada. Isso significa baixar todos os
	 * arquivos e interpretá-los.
	 */
    private void prepararEntrada(final URL url_fonte, final URL url_texto) {
        new Thread(new Runnable() {

            public void run() {
                Principal p;
                try {
                    p = new Principal(getArquivo(url_fonte), getArquivo(url_texto));
                } catch (Exception e) {
                    e.printStackTrace();
                    p = null;
                }
                synchronized (entradas) {
                    if (!entradas.containsKey(url_texto)) entradas.put(url_texto, p);
                    entradas.notifyAll();
                }
                ;
            }
        }).start();
    }

    /**
	 * Retorna uma entrada que foi preparada anteriormente. Caso a preparação
	 * em background ainda não tenha sido concluída, este método bloqueia.
	 * 
	 * Retorna 'null' quando houver um erro na preparação da entrada.
	 * Se a url não tiver sido preparada, ele não retorna *nunca*.
	 */
    private Principal getEntrada(URL url_texto) {
        while (true) {
            synchronized (entradas) {
                if (entradas.containsKey(url_texto)) return entradas.get(url_texto); else try {
                    entradas.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
    }

    /**
	 * Abre uma janela com uma entrada previamente preparada.
	 * Pode ser chamado quantas vezes quantas forem desejadas após
	 * a entrada ter sido preparada.
	 */
    public void abrirEntrada(String str_fonte, String str_texto) throws MalformedURLException {
        if (str_fonte != str_fonte) return;
        abrirEntrada(normalizarURL(str_texto));
    }

    /**
	 * Abre uma janela com uma entrada previamente preparada.
	 */
    private void abrirEntrada(final URL url_texto) {
        Principal principal = getEntrada(url_texto);
        Programa p;
        synchronized (janelas) {
            if (janelas.containsKey(url_texto)) p = janelas.get(url_texto); else {
                p = new Programa(principal);
                p.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                janelas.put(url_texto, p);
            }
        }
        p.setVisible(true);
        p.toFront();
    }

    @Override
    public void init() {
        try {
            AbstractArquivo.url_base = normalizarURL("").toString();
            String modo = getParameter("modo").toLowerCase();
            if (modo.equals("janela") || modo.equals("applet")) {
                URL url_fonte = normalizarURL(getParameter("arq_fonte"));
                URL url_texto = normalizarURL(getParameter("arq_texto"));
                prepararEntrada(url_fonte, url_texto);
                Principal principal = getEntrada(url_texto);
                if (principal == null) throw new Exception("Erro na preparação, veja o Console Java");
                if (modo.equals("janela")) {
                    int largura, altura;
                    try {
                        String l_temp = getParameter("largura");
                        largura = l_temp != null ? Integer.parseInt(l_temp) : 800;
                        String a_temp = getParameter("altura");
                        altura = l_temp != null ? Integer.parseInt(a_temp) : 800;
                    } catch (NumberFormatException e) {
                        largura = 800;
                        altura = 600;
                    }
                    Programa p = new Programa(principal);
                    p.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    p.setSize(largura, altura);
                    p.setVisible(true);
                } else {
                    GridBagConstraints gbc = new GridBagConstraints();
                    setLayout(new GridBagLayout());
                    gbc.anchor = GridBagConstraints.CENTER;
                    gbc.fill = GridBagConstraints.BOTH;
                    gbc.weightx = gbc.weighty = 1.0;
                    add(principal);
                }
            } else if (modo.equals("escondido")) {
                if (getParameter("arq_fonte") != null || getParameter("arq_texto") != null) throw new Exception("No modo \"escondido\" não devem ser" + "especificados os parâmetros \"arq_fonte\" e" + "\"arq_texto\".");
                janelas = new HashMap<URL, Programa>();
            } else {
                throw new Exception("Modo \"" + modo + "\" desconhecido.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e, "Tira-Teima", JOptionPane.ERROR_MESSAGE);
        }
    }
}
