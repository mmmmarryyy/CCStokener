package ghm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import algutil.fichier.ActionsFichiers;
import algutil.fichier.exception.CreationDossierException;
import algutil.fichier.exception.DossierNExistePasException;

public class GHMWebSite {

    public static final String URL_START_PAGE = "http://guesshermuff.blogspot.com";

    public static final String URL_PAGE_30_LAST = "http://guesshermuff.blogspot.com/search?max-results=30";

    public static String URL_GUEST_AUTH = "";

    public static File IMAGES_PATH = new File("E:\\data\\perso\\synchro_photos_iphone");

    private boolean activerProxyGL = false;

    private static final Logger log = Logger.getLogger(GHMWebSite.class);

    public GHMWebSite() throws DossierNExistePasException, IOException {
        if (!IMAGES_PATH.exists()) {
            throw new DossierNExistePasException("Le repertoire destination n'existe pas : " + IMAGES_PATH);
        }
    }

    public GHMWebSite(File destination, boolean activerProxyGL) throws DossierNExistePasException, IOException {
        this.activerProxyGL = activerProxyGL;
        IMAGES_PATH = destination;
        if (!IMAGES_PATH.exists()) {
            throw new DossierNExistePasException("Le repertoire destination n'existe pas : " + IMAGES_PATH);
        }
    }

    public List<GHMBean> parse(String url) throws Exception {
        List<GHMBean> l = new ArrayList<GHMBean>();
        GHMBean currentBean = null;
        HttpHost proxy = new HttpHost("172.16.1.138", 3128, "http");
        HttpClient httpclient = new DefaultHttpClient();
        if (activerProxyGL) {
            httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String[] lignes = EntityUtils.toString(entity).split("\n");
        String ligne;
        String date = null;
        String girl = null;
        log.info("Parse html...");
        for (int j = 0; j < lignes.length; j++) {
            ligne = lignes[j];
            log.debug(ligne);
            try {
                if (ligne.indexOf("<h2 class='date-header'") != -1) {
                    date = ligne.substring(ligne.indexOf("'>") + 2, ligne.indexOf("</"));
                    log.debug("Date : " + date);
                } else if (ligne.indexOf(".jpg") != -1 || ligne.indexOf(".JPG") != -1 || ligne.indexOf(".jpeg") != -1) {
                    String[] tab = ligne.split("\"");
                    for (int i = 0; i < tab.length; i++) {
                        if (tab[i].trim().startsWith("http") && (tab[i].trim().endsWith(".jpg") || tab[i].trim().endsWith(".JPG") || tab[i].trim().endsWith(".jpeg"))) {
                            log.debug(tab[i]);
                            if (tab[i].indexOf("s1600") != -1) {
                                if (currentBean.getImg1() == null) {
                                    currentBean.setImg1(tab[i]);
                                } else {
                                    currentBean.addReponse(tab[i]);
                                }
                            }
                        }
                    }
                } else if (ligne.indexOf("Girl #") != -1 && !ligne.startsWith("<li>")) {
                    currentBean = new GHMBean();
                    l.add(currentBean);
                    log.debug(ligne);
                    girl = ligne.substring(ligne.indexOf("'>") + 2, ligne.indexOf("</"));
                    log.info("Post : " + girl + " du " + date);
                    currentBean.setLibelle(girl + " du " + date);
                }
            } catch (Exception e) {
                log.error("ERREUR : ...");
            }
        }
        return l;
    }

    private String recupererVraiURL(String url) throws Exception {
        String ui = null;
        try {
            BufferedReader br = null;
            InputStream httpStream = null;
            log.info("URL : " + url);
            URL fileURL = new URL(url);
            URLConnection urlConnection = fileURL.openConnection();
            httpStream = urlConnection.getInputStream();
            br = new BufferedReader(new InputStreamReader(httpStream, "UTF-8"));
            String ligne;
            while ((ligne = br.readLine()) != null) {
                if (ligne.indexOf("<img") != -1) {
                    ui = ligne.substring(ligne.indexOf("src=\"") + 5, ligne.indexOf("\"", ligne.indexOf("src=\"") + 10));
                    log.debug("Vrai URL = " + ui);
                }
            }
            br.close();
            if (httpStream != null) {
                httpStream.close();
            }
            if (ui == null) {
                return url;
            }
        } catch (MalformedURLException e) {
            log.error("MalformedURL : " + url);
            throw e;
        }
        return ui;
    }

    public void recupererImages(GHMBean bean) throws CreationDossierException {
        String num = bean.getLibelle().substring(6, bean.getLibelle().indexOf(" du"));
        log.debug("Tentative pour : " + num + " -> " + bean.getLibelle());
        String moisS = bean.getLibelle().substring(bean.getLibelle().indexOf("du ") + 3, bean.getLibelle().indexOf("/"));
        if (moisS.length() == 1) {
            moisS = "0" + moisS;
        }
        String anneeS2 = bean.getLibelle().substring(bean.getLibelle().lastIndexOf("/") + 1);
        File repDest = new File(IMAGES_PATH.getPath() + File.separator + "ghm_20" + anneeS2 + "-" + moisS);
        if (!repDest.exists()) {
            ActionsFichiers.creerDossier(repDest);
        }
        try {
            File f = new File(repDest.getPath() + File.separator + "g" + num + "Q.jpg");
            if (!f.exists()) {
                if (log.isDebugEnabled()) {
                    log.debug("URL BD   : " + bean.getImg1());
                    log.debug("URL VRAI : " + recupererVraiURL(bean.getImg1()));
                }
                log.info("DL des img pour " + bean.getLibelle());
                ActionsFichiers.copierFichier(new URL(recupererVraiURL(bean.getImg1())), f);
            }
            f = new File(repDest.getPath() + File.separator + "g" + num + "R1.jpg");
            if (!f.exists()) {
                ActionsFichiers.copierFichier(new URL(recupererVraiURL(bean.getImg2())), f);
            }
            f = new File(repDest.getPath() + File.separator + "g" + num + "R2.jpg");
            if (bean.getImg3() != null && !f.exists()) {
                ActionsFichiers.copierFichier(new URL(recupererVraiURL(bean.getImg3())), f);
            }
            f = new File(repDest.getPath() + File.separator + "g" + num + "R3.jpg");
            if (bean.getImg4() != null && !f.exists()) {
                ActionsFichiers.copierFichier(new URL(recupererVraiURL(bean.getImg4())), f);
            }
            f = new File(repDest.getPath() + File.separator + "g" + num + "R4.jpg");
            if (bean.getImg5() != null && !f.exists()) {
                ActionsFichiers.copierFichier(new URL(recupererVraiURL(bean.getImg5())), f);
            }
            f = new File(repDest.getPath() + File.separator + "g" + num + "R5.jpg");
            if (bean.getImg6() != null && !f.exists()) {
                ActionsFichiers.copierFichier(new URL(recupererVraiURL(bean.getImg6())), f);
            }
        } catch (Exception e) {
            log.error("Impossible de recuperer le post : " + bean.getLibelle(), e);
        }
    }

    private String rechercherURLConnexionGuestAuth() throws IOException {
        String ui = null;
        BufferedReader br = null;
        InputStream httpStream = null;
        log.debug("Recherche dans : " + URL_START_PAGE);
        HttpHost proxy = new HttpHost("172.16.1.138", 3128, "http");
        HttpClient httpclient = new DefaultHttpClient();
        if (activerProxyGL) {
            httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
        HttpGet httpget = new HttpGet(URL_START_PAGE);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String[] lignes = EntityUtils.toString(entity).split("\n");
        String ligne;
        for (int i = 0; i < lignes.length; i++) {
            log.debug(lignes[i]);
            if (lignes[i].indexOf("href=\"http://guesshermuff.blogspot.com/") != -1) {
                ui = lignes[i].substring(lignes[i].indexOf("href=\"") + 6, lignes[i].indexOf("\"", lignes[i].indexOf("href=\"") + 10));
                log.debug("URL Guest Auth = " + ui);
            }
        }
        log.debug("URL trouv� : " + ui);
        URL_GUEST_AUTH = ui.substring(ui.indexOf("guestAuth="));
        log.debug("GUEST_AUTH=" + URL_GUEST_AUTH);
        return ui;
    }

    public static void main(String[] args) throws Exception {
        try {
            DOMConfigurator.configure("conf/log4j.xml");
        } catch (Exception e) {
        }
        GHMWebSite mf = new GHMWebSite(IMAGES_PATH, true);
        List<GHMBean> l = mf.parse(URL_PAGE_30_LAST);
        for (int i = 0; i < l.size(); i++) {
            mf.recupererImages(l.get(i));
        }
    }
}
