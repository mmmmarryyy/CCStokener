package br.org.acessobrasil.processoacessibilidade.bo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import br.org.acessobrasil.ases.regras.RegrasHardCodedEmag;
import br.org.acessobrasil.io.G_File;
import br.org.acessobrasil.padrao.G_StringUtils;
import br.org.acessobrasil.padrao.PadraoNumerico;
import br.org.acessobrasil.portal.modelo.Arquivo;
import br.org.acessobrasil.portal.modelo.Pagina;
import br.org.acessobrasil.portal.persistencia.ArquivoDaoSpring;
import br.org.acessobrasil.processoacessibilidade.dao.ArquivoDao;
import br.org.acessobrasil.processoacessibilidade.dao.PaginaDao;
import br.org.acessobrasil.processoacessibilidade.vo.ArquivoPro;
import br.org.acessobrasil.processoacessibilidade.vo.JobPro;
import br.org.acessobrasil.processoacessibilidade.vo.PaginaPro;

/**
 * O Arquivista salva HTML dos htm
 * @author Fabio Issamu Oshiro, Jonatas Pacheco Ribeiro
 *
 */
public class Arquivista {

    private static Logger logger = Logger.getLogger(Arquivista.class);

    public static String pastaDeArquivosDoSitio = "../sitio/";

    public static String pastaDePaginasERelatoriosDoSitio = "../sitio/";

    public static String pastaDeArquivosTemplates = "../sitio/";

    public static int arrumarLinkPara;

    public static final int LINK_PARA_PAGINA_LOCAL = 1;

    public static final int LINK_PARA_PAGINA_CMS = 2;

    private FiltroDeUrl filtroDeUrl = null;

    public static Pattern PAT_A = Pattern.compile("<a\\s[^>]*>", Pattern.CASE_INSENSITIVE);

    private static Pattern patImg = Pattern.compile("<img[^>]*>", Pattern.CASE_INSENSITIVE);

    private static Pattern patCss = Pattern.compile("<link[^>]*>", Pattern.CASE_INSENSITIVE);

    private static Pattern patJavaScript = Pattern.compile("<script[^>]*>", Pattern.CASE_INSENSITIVE);

    private static Pattern patSwf = Pattern.compile("<param[^>]*>", Pattern.CASE_INSENSITIVE);

    private static Pattern patObject = Pattern.compile("<object[^>]*>", Pattern.CASE_INSENSITIVE);

    private static Pattern patApplet = Pattern.compile("<object[^>]*>", Pattern.CASE_INSENSITIVE);

    private static Pattern patUrlCss = Pattern.compile(".*?url[^;]*;", Pattern.CASE_INSENSITIVE);

    /**
	 * Pattern para pegar o @ import sem o url na frente
	 */
    private static Pattern patUrlCss2 = Pattern.compile("@import (\"|')(.*?)\\1");

    private static RegrasHardCodedEmag regras = new RegrasHardCodedEmag();

    private static ArquivoDao arquivoDao;

    private static PaginaDao paginaDao;

    private boolean enabledBaixarArquivos = true;

    private boolean enabledBaixarImagens = true;

    private boolean enabledBaixarCss = true;

    private boolean enabledBaixarJavaScript = true;

    private boolean enabledBaixarSwf = true;

    private boolean enabledBaixarObject = true;

    private boolean enabledBaixarApplet = true;

    private boolean enabledCriarPagina = true;

    private boolean enabledBaixarPdf = true;

    private boolean enabledCadastrarImagensCms = false;

    private Coordenador coordenador;

    public Arquivista(Coordenador coordenador) {
        this.coordenador = coordenador;
    }

    public static PaginaDao getPaginaDao() {
        return paginaDao;
    }

    public static void setPaginaDao(PaginaDao paginaDao) {
        Arquivista.paginaDao = paginaDao;
    }

    private static String extensoesPaginas[] = { ".htm", ".html", ".jsp", ".asp", ".aspx", ".action", ".do", ".dhtml", ".php", ".cfm" };

    private static String extensoesArquivos[] = { ".doc", ".odt", ".xls", ".pdf", ".exe", ".zip", ".wav", ".mp3", ".ogg", ".jpg", ".jpeg", ".gif", ".png", ".swf", ".js", ".tar", ".gz", ".rar", ".txt", ".xml", ".mpeg", ".avi", ".mpg", ".wmv", ".flv" };

    private static boolean isPagina(String url) {
        String extensao = "." + G_File.getExtensao(url);
        for (int i = 0; i < extensoesPaginas.length; i++) {
            if (extensao.toLowerCase().equals(extensoesPaginas[i])) {
                return true;
            }
        }
        return false;
    }

    /**
	 * Bugs conhecidos, pode ser um arquivo, porem baixado de uma pagina
	 * @param url
	 * @return true caso seja uma extensao de arquivo
	 */
    public static boolean isArquivo(String url) {
        String extensao = "." + G_File.getExtensao(url);
        for (int i = 0; i < extensoesArquivos.length; i++) {
            if (extensao.toLowerCase().equals(extensoesArquivos[i])) {
                return true;
            }
        }
        return false;
    }

    /**
	 * 
	 * @param address
	 * @param localFileName
	 */
    private void download(String address, String localFileName) throws UrlNotFoundException, Exception {
        String ext = G_File.getExtensao(address);
        if (ext.equals("jsp")) {
            throw new Exception("Erro ao baixar pagina JSP, tipo negado." + address);
        }
        File temp = new File(localFileName + ".tmp");
        if (temp.exists()) temp.delete();
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            try {
                URL url = new URL(address);
                conn = url.openConnection();
                in = conn.getInputStream();
            } catch (FileNotFoundException e2) {
                throw new UrlNotFoundException();
            }
            out = new BufferedOutputStream(new FileOutputStream(temp));
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
        } catch (UrlNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw exception;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
        File oldArq = new File(localFileName);
        if (oldArq.exists()) {
            oldArq.delete();
        }
        oldArq = null;
        File nomeFinal = new File(localFileName);
        temp.renameTo(nomeFinal);
    }

    /**
	 * Baixa os arquivos necessarios para montar a pagina
	 * @param pagina
	 * @param job
	 */
    public void baixarArquivos(PaginaPro pagina, JobPro job) {
        if (!enabledBaixarArquivos) return;
        logger.info("baixando arquivos...");
        baixarArquivosLinkados(pagina, job);
        if (enabledBaixarImagens) {
            baixarImagens(pagina, job);
        }
        if (enabledBaixarCss) {
            logger.debug("baixando arquivos css...");
            baixarCss(pagina, job);
        }
        if (enabledBaixarJavaScript) {
            baixarJavaScript(pagina, job);
        }
        if (enabledBaixarSwf) {
            baixarSwf(pagina, job);
        }
        if (enabledBaixarObject) {
            baixarObject(pagina, job);
        }
        if (enabledBaixarApplet) {
            baixarApplet(pagina, job);
        }
    }

    /**
	 * Quando mudado para false ele nao baixa os arquivos
	 * @param valor true ou false
	 */
    public void setBaixarArquivos(boolean valor) {
        enabledBaixarArquivos = valor;
    }

    public void setBaixarImagens(boolean valor) {
        enabledBaixarImagens = valor;
    }

    public void setBaixarCss(boolean valor) {
        enabledBaixarCss = valor;
    }

    public void setBaixarJavaScript(boolean valor) {
        enabledBaixarJavaScript = valor;
    }

    public void setBaixarSwf(boolean valor) {
        enabledBaixarSwf = valor;
    }

    public void setBaixarObject(boolean valor) {
        enabledBaixarObject = valor;
    }

    public void setBaixarApplet(boolean valor) {
        enabledBaixarApplet = valor;
    }

    /**
	 * Normalmente salva a pagina HTML no HD valor padrao = true
	 * @param valor false = nao cria a pagina html no HD
	 */
    public void setCriarPagina(boolean valor) {
        enabledCriarPagina = valor;
    }

    /**
	 * Ex.:
	 * &lt;applet code="components/TumbleItem.class" width="600" height="95"&gt;
	 * @param pagina
	 * @param job
	 */
    private void baixarApplet(PaginaPro pagina, JobPro job) {
        Matcher mat = patApplet.matcher(pagina.getCodOriginal());
        while (mat.find()) {
            String tag = mat.group();
            try {
                String code = regras.getAtributo(tag, "code");
                if (!code.equals("")) {
                    ArquivoPro arquivo = baixarCadastrarArquivo(normalizarUrl(pagina.getUrl(), code), "applet", job, pagina);
                    if (arquivo != null) arrumarLink(pagina, code, arquivo.getPathLocal());
                }
            } catch (Exception e) {
                logger.error("Erro ao baixar " + tag, e);
            }
        }
    }

    /**
	 * Ex.:
	 * &lt;object type="application/x-shockwave-flash" data="arquivos/swf/flash3.swf" &gt;
	 * @param pagina
	 * @param job
	 */
    private void baixarObject(PaginaPro pagina, JobPro job) {
        Matcher mat = patObject.matcher(pagina.getCodOriginal());
        while (mat.find()) {
            String tag = mat.group();
            try {
                String data = regras.getAtributo(tag, "data");
                if (!data.equals("")) {
                    String dataFull = normalizarUrl(pagina.getUrl(), data);
                    ArquivoPro arquivo;
                    if (G_File.getExtensao(dataFull).equals("swf")) {
                        arquivo = baixarCadastrarArquivo(dataFull, "swf", job, pagina);
                    } else {
                        arquivo = baixarCadastrarArquivo(dataFull, "object", job, pagina);
                    }
                    if (arquivo != null) arrumarLink(pagina, data, arquivo.getPathLocal());
                }
            } catch (Exception e) {
                logger.error("Erro ao baixar " + tag, e);
            }
        }
    }

    /**
	 * Ex.: &lt;param name="movie" value="arquivos/swf/flash3.swf"  /&gt;
	 * @param pagina
	 * @param job
	 */
    private void baixarSwf(PaginaPro pagina, JobPro job) {
        Matcher mat = patSwf.matcher(pagina.getCodOriginal());
        while (mat.find()) {
            String tag = mat.group();
            try {
                String name = regras.getAtributo(tag, "name");
                if (name.toLowerCase().equals("movie")) {
                    String value = regras.getAtributo(tag, "value");
                    ArquivoPro arquivo = baixarCadastrarArquivo(normalizarUrl(pagina.getUrl(), value), "swf", job, pagina);
                    if (arquivo != null) {
                        arrumarLink(pagina, value, arquivo.getPathLocal());
                    }
                }
            } catch (Exception e) {
                logger.error("Erro ao baixar swf " + tag, e);
            }
        }
    }

    /**
	 * Ex.: &lt;script src="extras/acessibilidade.js" type="text/javascript"&gt;&lt;/script&gt;
	 * @param pagina
	 * @param job
	 */
    private void baixarJavaScript(PaginaPro pagina, JobPro job) {
        Matcher mat = patJavaScript.matcher(pagina.getCodOriginal());
        while (mat.find()) {
            String tag = mat.group();
            try {
                String src = regras.getAtributo(tag, "src");
                if (!src.equals("")) {
                    ArquivoPro arquivo = baixarCadastrarArquivo(normalizarUrl(pagina.getUrl(), src), "javascript", job, pagina);
                    if (arquivo != null) arrumarLink(pagina, src, arquivo.getPathLocal());
                }
            } catch (Exception e) {
                logger.error("Erro ao baixar " + tag, e);
            }
        }
    }

    /**
	 * Ex.:
	 * &lt;link href="css/basico.css" rel="stylesheet" type="text/css" media="screen" /&gt;
	 * @param pagina
	 * @param job
	 */
    private void baixarCss(PaginaPro pagina, JobPro job) {
        Matcher mat = patCss.matcher(pagina.getCodOriginal());
        while (mat.find()) {
            String tag = mat.group();
            try {
                String type = regras.getAtributo(tag, "type");
                if (type.toLowerCase().equals("text/css")) {
                    String href = regras.getAtributo(tag, "href");
                    ArquivoPro arquivo = baixarCadastrarArquivo(normalizarUrl(pagina.getUrl(), href), "css", job, pagina);
                    if (arquivo != null && !href.equals("")) {
                        arrumarLink(pagina, href, "templates" + File.separatorChar + coordenador.getSitioCms().getNuSitio() + File.separatorChar + "css" + File.separatorChar + arquivo.getIdArquivo() + "_a_" + G_File.getName(arquivo.getPathLocal()));
                        baixarArquivoCss(arquivo, job, pagina);
                    }
                }
            } catch (Exception e) {
                logger.error("Erro ao baixar " + tag, e);
            }
        }
        mat = patUrlCss.matcher(pagina.getCodOriginal());
        while (mat.find()) {
            String tag = mat.group();
            try {
                int ini = tag.indexOf("(") + 1;
                int fim = tag.indexOf(")");
                if (fim == -1) continue;
                String href = tag.subSequence(ini, fim).toString().trim();
                if (href.endsWith(".css")) {
                    ArquivoPro arquivo = baixarCadastrarArquivo(normalizarUrl(pagina.getUrl(), href), "css", job, pagina);
                    if (arquivo != null) {
                        arrumarLink(pagina, href, "templates" + File.separatorChar + coordenador.getSitioCms().getNuSitio() + File.separatorChar + "css" + File.separatorChar + arquivo.getIdArquivo() + "_a_" + G_File.getName(arquivo.getUrl()));
                        baixarArquivoCss(arquivo, job, pagina);
                    }
                }
            } catch (Exception e) {
                logger.error("Erro ao baixar " + tag, e);
            }
        }
        mat = patUrlCss2.matcher(pagina.getCodOriginal());
        while (mat.find()) {
            String tag = mat.group();
            try {
                String href = mat.group(2).trim();
                ArquivoPro arquivo = baixarCadastrarArquivo(normalizarUrl(pagina.getUrl(), href), "css", job, pagina);
                if (arquivo != null && !href.equals("")) {
                    arrumarLink(pagina, href, "css/" + arquivo.getIdArquivo() + "_a_" + G_File.getName(arquivo.getUrl()));
                    baixarArquivoCss(arquivo, job, pagina);
                }
            } catch (Exception e) {
                logger.error("Erro ao baixar " + tag, e);
            }
        }
    }

    /**
	 * Retorna a url path relativa a host
	 * @param host referencia da url
	 * @param path url relativa ou fixa
	 * @return url path em relacao a host
	 * @throws Exception caso a url seja invalida
	 */
    private String normalizarUrl(String host, String path) throws Exception {
        path = tratarEncodedPath(path);
        URL url = new URL(new URL(host), path);
        return url.toString().replace("/../", "/");
    }

    private String tratarEncodedPath(String path) {
        path = G_StringUtils.urlDecode(path);
        path = G_StringUtils.htmlDecode(path);
        path = G_StringUtils.urlEncode(path);
        path = path.replace("%2F", "/");
        path = path.replace("%3A", ":");
        path = path.replace("+", "%20");
        path = path.replace("%0D", "");
        path = path.replace("%0A", "");
        path = path.replace("%23", "#");
        path = path.replace("%3F", "?");
        path = path.replace("%3D", "=");
        path = path.replace("%26", "&");
        return path;
    }

    private void baixarImagens(PaginaPro pagina, JobPro job) {
        Matcher mat = patImg.matcher(pagina.getCodOriginal());
        while (mat.find()) {
            String tag = mat.group();
            String src = regras.getAtributo(tag, "src");
            try {
                String alt = regras.getAtributo(tag, "alt");
                ArquivoPro arquivo = baixarCadastrarArquivo(normalizarUrl(pagina.getUrl(), src), alt, job, pagina);
                if (arquivo != null && !src.equals("")) {
                    logger.debug("arrumando o link de " + src + " para " + arquivo.getPathLocal() + "\n\ttag = " + tag);
                    arrumarLink(pagina, src, arquivo.getPathLocal());
                    DesignerTemplate.getInstance(job).descolorirImagem(arquivo, this);
                    if (enabledCadastrarImagensCms) {
                        cadatrarImagemCms(arquivo.getPathLocal(), alt);
                    }
                }
            } catch (Exception e) {
                logger.error("Erro ao baixar " + tag, e);
            }
        }
    }

    /**
	 * Cadastra uma imagem baixada no cms associado. 
	 * (Ha outro metodo que lista o diretorio de arquivos do cms e 
	 *  cadastra os arquivos que ainda nao estao no banco)
	 * @param src
	 * @param alt
	 */
    private void cadatrarImagemCms(String src, String alt) {
        cadastrarImagemCmsImp(src, alt);
        int indexPonto = src.lastIndexOf(".");
        cadastrarImagemCmsImp(src.substring(0, indexPonto) + "_pb" + src.substring(indexPonto), alt);
    }

    private void cadastrarImagemCmsImp(String src, String alt) {
        ArquivoDaoSpring arquivoDaoCMS = Coordenador.getArquivoDaoCMS();
        if (arquivoDaoCMS.buscarPorNomeExato(src, coordenador.getSitioCms()) == null) {
            Arquivo arquivo = new Arquivo();
            if (!alt.trim().equals("")) arquivo.setDeArquivo(alt); else arquivo.setDeArquivo("Inserido Automaticamente");
            arquivo.setNoArquivo(src);
            arquivo.setNuUsuario(2L);
            arquivo.setNuSetor(1L);
            arquivo.setSitio(coordenador.getSitioCms());
            try {
                arquivoDaoCMS.create(arquivo);
                logger.debug("cadastrou a imagem " + src + " " + arquivo.getNuArquivo() + " no cms.");
            } catch (Exception e) {
                logger.error("Ocorreu um erro ao cadastrar a imagem " + src + " no cms. ", e);
            }
        }
    }

    /**
	 * Substitui o linkOriginal por outro linkLocal
	 * @param cod codigo original
	 * @param linkOriginal link a ser substituido
	 * @param linkLocal link a ser colocado no lugar de linkOriginal
	 * @return codigo alterado
	 */
    private String arrumarLink(String cod, String linkOriginal, String linkLocal) {
        if (linkOriginal == null || linkOriginal.trim().equals("")) return cod;
        if (linkOriginal.contains("#")) {
            linkOriginal = linkOriginal.substring(0, linkOriginal.indexOf('#'));
        }
        if (linkOriginal.trim().equals("")) return cod;
        if (linkOriginal.length() < 3) {
            logger.warn("linkOriginal='" + linkOriginal + "' linkLocal = '" + linkLocal + "'");
        }
        cod = cod.replace("\"" + linkOriginal + "\"", "\"" + linkLocal + "\"").replace("\'" + linkOriginal + "\'", "\"" + linkLocal + "\"").replace("(" + linkOriginal + ")", "(" + linkLocal + ")").replace("\"" + linkOriginal + "#", "\"" + linkLocal + "#").replace("'" + linkOriginal + "#", "'" + linkLocal + "#");
        return cod;
    }

    /**
	 * Altera o atributo codTrabalho da pagina e altera o link para apontar localmente
	 * @param pagina pagina com o codTrabalho a ser alterado
	 * @param linkOriginal link a ser substituido
	 * @param linkLocal link a ser colocado no lugar de linkOriginal
	 */
    private void arrumarLink(PaginaPro pagina, String linkOriginal, String linkLocal) {
        String cod = pagina.getCodTrabalho();
        String codArquivo = pagina.getCodArquivo();
        String res = "";
        if (linkLocal.endsWith(".html")) {
            long id = Long.parseLong(linkLocal.substring(0, linkLocal.indexOf(".")));
            PaginaPro pagAux = paginaDao.find(id);
            if (pagAux.getIdPaginaCMS() == null) {
                Pagina paginaCms = new Pagina();
                Coordenador.getPaginaDaoCMS().create(paginaCms);
                pagAux.setIdPaginaCMS(paginaCms.getNuPagina());
            }
            res = arrumarLink(cod, linkOriginal, "?nu_pagina=" + pagAux.getIdPaginaCMS());
        } else {
            res = arrumarLink(cod, linkOriginal, linkLocal);
        }
        linkLocal = "../../workspace/CMS08/web/" + linkLocal;
        String resArquivo = arrumarLink(codArquivo, linkOriginal, linkLocal);
        pagina.setCodTrabalho(res);
        pagina.setCodArquivo(resArquivo);
    }

    /**
	 * 
	 * @param url http://www.qqcoisa.com
	 * @param job
	 * @return true caso pertence
	 */
    public boolean verificarPaginaPertenceSitio(String url, JobPro job) {
        if (filtroDeUrl != null) {
            return filtroDeUrl.pertence(url);
        }
        return coordenador.verificarPaginaPertenceSitio(url, job);
    }

    private synchronized ArquivoPro baixarCadastrarArquivo(String url, String alt, JobPro job, PaginaPro pagina) throws Exception {
        logger.debug("baixando " + url);
        if (!verificarPaginaPertenceSitio(url, job)) {
            logger.info("A url nao casa com o site '" + url + "'");
            return null;
        }
        ArquivoPro arquivo;
        arquivo = arquivoDao.find(url);
        if (arquivo == null) {
            arquivo = new ArquivoPro();
            arquivo.setAlt(alt);
            arquivo.setUrl(url);
            arquivoDao.create(arquivo);
        }
        if (arquivo.getErro() != null) return arquivo;
        String extensao = G_File.getExtensao(arquivo.getUrl());
        if (extensao == null || extensao.equals("")) {
            logger.warn("AVISO: Arquivista.baixarCadastrarArquivo():\n\tSem extensao:" + arquivo.toString().replace("\n", "\n\t\t"));
            extensao = "jpg";
        }
        extensao = extensao.toLowerCase();
        String fileName = G_File.getName(arquivo.getUrl());
        String basePath;
        String localRelativo;
        if (extensao.equals("css") || extensao.equals("js")) {
            localRelativo = extensao + "/" + arquivo.getIdArquivo() + "_" + fileName;
            File dir = new File(pastaDeArquivosTemplates + extensao);
            basePath = pastaDeArquivosTemplates;
            if (!dir.exists()) dir.mkdirs();
        } else {
            if (extensao.equals("br")) {
                throw new Exception("No 'br' allowed!");
            }
            localRelativo = extensao + "/" + arquivo.getIdArquivo() + "_" + fileName;
            File dir = new File(pastaDeArquivosDoSitio + extensao);
            basePath = pastaDeArquivosDoSitio;
            if (!dir.exists()) dir.mkdirs();
        }
        localRelativo = G_StringUtils.urlDecode(localRelativo);
        localRelativo = G_StringUtils.removeAcentos(localRelativo);
        localRelativo = localRelativo.replace("%20", "_");
        localRelativo = localRelativo.replace(" ", "_");
        String localFileName = basePath + localRelativo;
        if (arquivo.getPathLocal() == null || !localFileName.equals(arquivo.getPathLocal())) {
            if (extensao.equals("css") || extensao.equals("js")) {
                arquivo.setPathLocal("templates/" + coordenador.getSitioCms().getNuSitio() + "/" + localRelativo);
            } else {
                arquivo.setPathLocal("arquivos/" + coordenador.getSitioCms().getNuSitio() + "/" + localRelativo);
            }
            arquivoDao.update(arquivo);
        }
        File arqLocal = new File(localFileName);
        if (!arqLocal.exists()) {
            try {
                logger.debug("baixando o arquivo " + arquivo.getUrl() + "\n\tpagina: " + pagina.getUrl());
                download(arquivo.getUrl(), localFileName);
            } catch (UrlNotFoundException e) {
                arquivo.setErro("Erro 404 " + pagina.getUrl());
                arquivoDao.update(arquivo);
            }
        } else {
            logger.debug("Arquivo existente, pulando " + localFileName);
        }
        pagina = paginaDao.find(pagina.getIdPagina());
        if (pagina.getListaArquivo() == null) {
            pagina.setListaArquivo(new ArrayList<ArquivoPro>());
        }
        if (!pagina.getListaArquivo().contains(arquivo)) {
            pagina.getListaArquivo().add(arquivo);
            paginaDao.update(pagina);
        }
        return arquivo;
    }

    /**
	 * verifica a existencia de arquivos dentro das css's
	 */
    public void baixarArquivoCss(ArquivoPro arquivo2, JobPro job, PaginaPro pagina) throws Exception {
        ArrayList<ArquivoPro> arrUrlCss = new ArrayList<ArquivoPro>();
        arrUrlCss.add(arquivo2);
        int i = 0;
        while (i < arrUrlCss.size()) {
            ArquivoPro arquivo = arrUrlCss.get(i);
            if (arquivo == null) return;
            String basePath = pastaDeArquivosTemplates;
            try {
                G_File alterado = new G_File(basePath + "css/" + arquivo.getIdArquivo() + "_a_" + G_File.getName(arquivo.getPathLocal()));
                if (alterado.exists()) {
                    i++;
                    continue;
                }
                G_File css = new G_File(Coordenador.PASTA_BASE_ARQUIVOS_CMS + arquivo.getPathLocal());
                String conteudo = css.read();
                String conteudoAlterado = conteudo;
                Matcher mat = patUrlCss.matcher(conteudo);
                while (mat.find()) {
                    String tag = mat.group();
                    String urlImport = tag.subSequence(tag.indexOf("(") + 1, tag.indexOf(")")).toString().trim();
                    if (urlImport.endsWith(".css")) {
                        if (arrUrlCss.contains(urlImport)) continue;
                        ArquivoPro arquivo3 = baixarCadastrarArquivo(normalizarUrl(arquivo.getUrl(), urlImport), "css", job, pagina);
                        arrUrlCss.add(arquivo3);
                        conteudoAlterado = arrumarLink(conteudoAlterado, urlImport, arquivo3.getIdArquivo() + "_a_" + G_File.getName(arquivo3.getPathLocal()));
                    } else {
                        if ((urlImport.endsWith("\"") && urlImport.startsWith("\"")) || (urlImport.endsWith("'") && urlImport.startsWith("'"))) {
                            urlImport = urlImport.substring(1, urlImport.length() - 1);
                        }
                        ArquivoPro arquivo3 = baixarCadastrarArquivo(normalizarUrl(arquivo.getUrl(), urlImport), "arquivoCss", job, pagina);
                        conteudoAlterado = arrumarLink(conteudoAlterado, urlImport, "../../../" + arquivo3.getPathLocal());
                    }
                }
                alterado.write(conteudoAlterado);
            } catch (Exception e) {
                logger.error("Erro ao baixar import no css " + arquivo.getUrl());
            }
            i++;
        }
    }

    /**
	 * Baixa todos os arquivos de a href
	 * com final de extensoesArquivos
	 * @param pagina
	 * @param job
	 */
    private void baixarArquivosLinkados(PaginaPro pagina, JobPro job) {
        Matcher mat = PAT_A.matcher(pagina.getCodOriginal());
        while (mat.find()) {
            String tag = mat.group();
            String href = regras.getAtributo(tag, "href");
            try {
                boolean baixou = false;
                if (!isPagina(href)) {
                    if (isArquivo(href) && baixarPdf(href)) {
                        String alt = regras.getAtributo(tag, "alt");
                        ArquivoPro arquivo = baixarCadastrarArquivo(normalizarUrl(pagina.getUrl(), href), alt, job, pagina);
                        if (arquivo != null && href != null && !href.trim().equals("")) {
                            arrumarLink(pagina, href, arquivo.getPathLocal());
                            baixou = true;
                        }
                    } else {
                        logger.debug("Nao e uma pagina nem arquivo " + href);
                    }
                }
                if (!baixou) {
                    try {
                        String fullUrl = normalizarUrl(pagina.getUrl(), href);
                        if (fullUrl.matches(job.getSitio().getUrlRegex())) {
                            PaginaPro pag = paginaDao.find(fullUrl);
                            if (pag != null) {
                                arrumarLink(pagina, href, PadraoNumerico.completarZeros(pag.getIdPagina(), 5) + ".html");
                            } else {
                                logger.debug("PaginaPro ausente no banco '" + fullUrl + "'");
                            }
                        }
                    } catch (MalformedURLException ex) {
                        logger.info("Erro ao arrumar o link " + href);
                    }
                }
            } catch (Exception e) {
                logger.error("Erro ao baixar " + href);
            }
        }
    }

    private boolean baixarPdf(String href) {
        if (enabledBaixarPdf) {
            return true;
        } else if (!href.toLowerCase().endsWith(".pdf")) {
            return true;
        }
        return false;
    }

    /**
	 * Verifica se existe a pasta do site, senao houver a criara
	 * @param job
	 * @throws Exception
	 */
    public void criarPastaSitio(JobPro job) throws Exception {
        if (!new File(pastaDeArquivosDoSitio).exists()) {
            File pastaSitioFile = new File(pastaDeArquivosDoSitio);
            pastaSitioFile.mkdirs();
        }
        if (!new File(pastaDePaginasERelatoriosDoSitio).exists()) {
            File pastaSitioFile = new File(pastaDePaginasERelatoriosDoSitio);
            pastaSitioFile.mkdirs();
        }
        if (!new File(pastaDeArquivosTemplates).exists()) {
            File pastaSitioFile = new File(pastaDeArquivosTemplates);
            pastaSitioFile.mkdirs();
        }
    }

    /**
	 * Cria a pagina para ser alterada
	 * @param pagina
	 * @param job
	 * @return path do arquivo criado pela pagina
	 * @throws Exception
	 */
    public String criarPaginaTrabalho(PaginaPro pagina, JobPro job) throws Exception {
        if (!enabledCriarPagina) {
            return "";
        }
        String path = null;
        if (pagina == null) {
            throw new Exception("Erro pagina = null");
        }
        if (job == null) {
            throw new Exception("Erro job = null");
        }
        if (job.getSitio() == null) {
            throw new Exception("Erro job.getSitio() = null");
        }
        if (job.getSitio().getNomePasta() == null) {
            throw new Exception("Erro job.getSitio().getNomePasta() = null");
        }
        path = pastaDePaginasERelatoriosDoSitio + PadraoNumerico.completarZeros(pagina.getIdPagina(), 5) + ".html";
        G_File paginaFile = new G_File(path);
        paginaFile.write(pagina.getCodArquivo());
        return path;
    }

    /**
	 * Cria a pagina de backup no HD, sendo este o codigo original<br>
	 * o .htm
	 * @param pagina
	 * @param job
	 * @return path do arquivo criado
	 * @throws Exception
	 */
    public String criarPagina(PaginaPro pagina, JobPro job) throws Exception {
        if (!enabledCriarPagina) {
            return "";
        }
        logger.info("Criando pagina " + pagina.getIdPagina() + " no HD");
        String path = pastaDePaginasERelatoriosDoSitio + PadraoNumerico.completarZeros(pagina.getIdPagina(), 5) + ".htm";
        G_File paginaFile = new G_File(path);
        paginaFile.write(pagina.getCodOriginal());
        return path;
    }

    public static ArquivoDao getArquivoDao() {
        return arquivoDao;
    }

    public static void setArquivoDao(ArquivoDao arquivoDao) {
        Arquivista.arquivoDao = arquivoDao;
    }

    /**
	 * Tenta baixar novamente arquivos com erro
	 */
    public void tentarBaixarArquivosErro(JobPro job) {
        List<ArquivoPro> arquivos = arquivoDao.selectErros(job.getSitio());
        int tot = arquivos.size();
        int atual = 0;
        for (ArquivoPro arquivo : arquivos) {
            atual++;
            String urlOriginal = arquivo.getUrl();
            try {
                arquivo.setUrl(tratarEncodedPath(arquivo.getUrl()));
                baixarNovamente(arquivo, job);
                logger.debug(atual + " de " + tot + "\n\tBaixado com sucesso " + arquivo.getUrl());
            } catch (Exception e) {
                logger.debug(atual + " de " + tot + "\n\tErro ao baixar " + arquivo.getUrl());
                arquivo.setErro(e.getMessage());
                arquivo.setUrl(urlOriginal);
                arquivoDao.update(arquivo);
            }
        }
    }

    /**
	 * Tenta baixar novamente um arquivo
	 * @param arquivo
	 * @param job
	 * @throws Exception
	 */
    public void baixarNovamente(ArquivoPro arquivo, JobPro job) throws Exception {
        download(arquivo.getUrl(), arquivo.getPathLocal());
    }

    public void setFiltroDeUrl(FiltroDeUrl filtroDeUrl) {
        this.filtroDeUrl = filtroDeUrl;
    }

    /**
	 * Path onde ser�o salvos os arquivos deste s�tio
	 * @param pathSitio
	 */
    public void setPastaDeArquivosDoSitio(String pastaDeArquivosDoSitio) {
        Arquivista.pastaDeArquivosDoSitio = pastaDeArquivosDoSitio;
    }

    /**
	 * Path onde ser�o salvos os relat�rios e p�ginas deste s�tio
	 * @param pastaDePaginasERelatoriosDoSitio
	 */
    public void setPastaDePaginasERelatoriosDoSitio(String pastaDePaginasERelatoriosDoSitio) {
        Arquivista.pastaDePaginasERelatoriosDoSitio = pastaDePaginasERelatoriosDoSitio;
    }

    public void setPastaDeArquivosTemplates(String pastaDeArquivosTemplates) {
        Arquivista.pastaDeArquivosTemplates = pastaDeArquivosTemplates;
    }

    /**
	 * Determina se os pdfs serao baixados ou ignorados (padrao = true).
	 * @param enabledBaixarPdf
	 */
    public void setBaixarPdf(boolean enabledBaixarPdf) {
        this.enabledBaixarPdf = enabledBaixarPdf;
    }

    public void setCadastrarImagensCms(boolean enabledCadastrarImagensCms) {
        this.enabledCadastrarImagensCms = enabledCadastrarImagensCms;
    }
}
