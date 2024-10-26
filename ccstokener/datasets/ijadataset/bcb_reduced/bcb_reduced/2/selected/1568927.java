package tcbnet.ui;

import tcbnet.TCBNet;
import tcbnet.control.Configuration;
import tcbnet.corba.Header;
import tcbnet.corba.Node;
import tcbnet.corba.NodeImpl;
import tcbnet.corba.TcbnetOrb;
import tcbnet.gui.MainWindow;
import tcbnet.treecast.Treecast;
import java.io.*;
import java.util.*;
import java.net.*;

/**
 * Fila de arquivos esperando o push, implementando um TableModel para poder
 * ser utilizada pela interface (e.g.: JTable).
 *
 * @author $Author: dseki $
 * @version $Revision: 1.17 $
 */
public class UIController {

    private Configuration configuration;

    private NodeImpl nodeImpl;

    private PushQueue pushQueue;

    private TransferTableModel transferTableModel;

    /**
     * Construtor da classe de controle de interface. Recebe como parametro
     * um NodeImpl, o meio de comunicacao com a camada responsavel pela mani-
     * pulacao dos arquivos (o nucleo do programa em si).
     *
     * @param nodeImpl no de comunicacao
     */
    public UIController(Configuration configuration, NodeImpl nodeImpl) {
        this.nodeImpl = nodeImpl;
        this.configuration = configuration;
        this.pushQueue = configuration.getUIConfiguration().getPushQueue();
        this.pushQueue.setUIController(this);
        this.transferTableModel = new TransferTableModel(this);
    }

    /**
     * Inicia a interface com o usuario, seja ela na linha de comando ou
     * grafica.
     */
    public synchronized void uiStart() {
        MainWindow ui = new MainWindow(this.configuration, this);
        ui.show();
    }

    /**
     * Atualiza na interface os dados que foram alterados.
     */
    public synchronized void refreshUI() {
        this.pushQueue.refresh();
        this.transferTableModel.refresh();
    }

    /**
     * Notifica o no' sobre mudanca nas configuracoes e requisita que os
     * novos parametros entrem em efeito.
     */
    public synchronized void refreshNodeConfigurations() {
        this.nodeImpl.update();
    }

    /**
     * Chama o node para criar um cabecalho com as informacoes fornecidas.
     * Retorna um vetor de tamanho dois contendo o cabecalho do tipo Header
     * na primeira posicao e um String com o caminho do arquivo na segunda.
     *
     * @param filename arquivo, com caminho completo
     * @param description descricao do arquivo
     * @return vetor com o header e o caminho do arquivo
     */
    public synchronized Object[] createHeader(String filename, String description) throws FileNotFoundException, IOException {
        return this.nodeImpl.prepareFile(filename, description);
    }

    /**
     * Devolve a fila de push.
     *
     * @return conteudo da fila de push
     */
    public synchronized PushQueue getPushQueue() {
        return this.pushQueue;
    }

    /**
     * Adiciona um ou mais nodes `a lista de nodes para se tentar conectar.
     * Este e' um metodo de conexao padrao, utilizado quando nao se quer espe-
     * cificar o metodo, apenas desejando que a adicao funcione.
     */
    public synchronized void connect() {
        try {
            this.connectURL("http://www.linux.ime.usp.br/" + "~dseki/disc/sod/tcbnet/ior/node.ior");
        } catch (IllegalArgumentException e) {
            System.out.println("Pagina nao eh do TCBNet");
        } catch (MalformedURLException e) {
            System.out.println("URL invalida");
        } catch (Exception e) {
            System.out.println("Problema de conexao (pagina nao encontrada)");
        }
    }

    /**
     * Adiciona o node fornecido `a lista de nodes para se tentar conectar.
     *
     * @param node Node da rede
     */
    public synchronized void connect(Node node) {
        this.nodeImpl.tryConnect(node);
    }

    /**
     * Adiciona todos os nodes fornecidos `a lista de nodes para se tentar
     * conectar.
     *
     * @param nodes array de Nodes conhecidos da rede em questao
     */
    public synchronized void connect(Node[] nodes) {
        this.nodeImpl.tryConnect(nodes);
    }

    /**
     * Adiciona o node da ior fornecida `a lista de nodes para se tentar
     * conectar.
     *
     * @param ior "representacao na forma de texto de um Node"
     */
    public synchronized void connect(String ior) {
        this.connect(TcbnetOrb.getInstance().getNode(ior));
    }

    /**
     * Adiciona todos os Nodes cujas IORs estao em um arquivo de nome fornecido
     * `a lista de nodes para se tentar conectar.
     *
     * @param filename nome do arquivo com o caminho (relativo ou absoluto)
     * @throws FileNotFoundException se nao encontrar ou conseguir abrir o arq
     * @throws IOException se ocorrer um erro de Entrada/Saida
     */
    public synchronized void connectFile(String filename) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        ArrayList nodesAL = new ArrayList();
        String ior;
        while ((ior = br.readLine()) != null) {
            if (ior.trim().equals("")) continue;
            nodesAL.add(ior);
        }
        Object[] nodesOA = nodesAL.toArray();
        Node[] nodes = new Node[nodesOA.length];
        for (int i = 0; i < nodesOA.length; i++) nodes[i] = TcbnetOrb.getInstance().getNode((String) nodesOA[i]);
        this.connect(nodes);
    }

    /**
     * Adiciona todos os Nodes cujas IORs forem fornecidas por uma pagina na
     * internet `a lista de nodes para se tentar conectar.
     * A pagina deve fornecer um texto com a palavra "TCBNet IOR" na primeira
     * linha e um IOR por linha depois disso.
     *
     * @param url endereco da pagina que deve fornecer os nodes
     */
    public synchronized void connectURL(String url) throws IllegalArgumentException, IOException, MalformedURLException {
        URL myurl = new URL(url);
        InputStream in = myurl.openStream();
        BufferedReader page = new BufferedReader(new InputStreamReader(in));
        String ior = null;
        ArrayList nodesAL = new ArrayList();
        while ((ior = page.readLine()) != null) {
            if (ior.trim().equals("")) continue;
            nodesAL.add(ior);
        }
        in.close();
        Object[] nodesOA = nodesAL.toArray();
        Node[] nodes = new Node[nodesOA.length];
        for (int i = 0; i < nodesOA.length; i++) nodes[i] = TcbnetOrb.getInstance().getNode((String) nodesOA[i]);
        this.connect(nodes);
    }

    /**
     * Procedimentos de disconexao.
     */
    public synchronized void disconnect() {
    }

    /**
     * Inicia o encerramento do programa.
     */
    public synchronized void exit() {
        TCBNet.exit();
    }

    /**
     * Retorna informacoes sobre downloads.
     *
     * @param i i-esimo download.
     * @param j j-esima informacao de download.
     * @return j-esima dentre "Filename", "Transfered", "Filesize", "Speed",
     *         "Source", "Description", "Progress" do i-esimo download.
     */
    public synchronized Object getDownloadInfo(int i, int j) {
        return this.nodeImpl.getDownloadInfo(i, j);
    }

    /**
     * @return o numero de downloads ativos
     */
    public synchronized int getDownloadCount() {
        return this.nodeImpl.getDownloadCount();
    }

    /**
     * Devolve as configuracoes.
     *
     * @return configuracoes do programa
     */
    public synchronized Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * @return transferTableModel
     */
    public synchronized TransferTableModel getTransferTableModel() {
        return this.transferTableModel;
    }

    /**
     * Ordena ao no' o envio de um arquivo.
     *
     * @param h cabecalho do arquivo
     * @param path caminho completo do arquivo
     * @return treecast criado
     */
    public synchronized Treecast broadcast(Header h, String path) {
        return this.nodeImpl.pushHeader(h, path);
    }

    /**
     * Acaba definitivamente com um treecast.
     *
     * @param treecast treecast no corredor da morte
     */
    public synchronized void killTreecast(Treecast treecast) {
        this.nodeImpl.kill(treecast);
    }
}
