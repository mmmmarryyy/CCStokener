package sk.sigp.tetras.test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.GregorianCalendar;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import sk.sigp.tetras.dao.BinaryObjectDao;
import sk.sigp.tetras.dao.FirmaDao;
import sk.sigp.tetras.dao.VyhladavaciAlgoritmusDao;
import sk.sigp.tetras.dao.criteria.FirmaSearchCriteria;
import sk.sigp.tetras.dao.websearch.SearchCategoryDao;
import sk.sigp.tetras.dao.websearch.SearchPrefsDao;
import sk.sigp.tetras.entity.Firma;
import sk.sigp.tetras.entity.FirmaInfo;
import sk.sigp.tetras.entity.VyhladavaciAlgoritmus;
import sk.sigp.tetras.entity.websearch.SearchCategory;
import sk.sigp.tetras.entity.websearch.SearchPrefs;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/dbcoreContext.xml" })
public class ShitTest {

    @Autowired
    FirmaDao dao;

    @Autowired
    BinaryObjectDao dao2;

    @Autowired
    SearchCategoryDao scDao;

    @Autowired
    VyhladavaciAlgoritmusDao vaDao;

    @Autowired
    SearchPrefsDao spDao;

    @SuppressWarnings("unused")
    private static Firma makef(String a, String n, String f1, String f2) {
        Firma f = new Firma();
        FirmaInfo info = new FirmaInfo();
        info.setAddress(a);
        info.setName(n);
        info.setFax(f1);
        f.setAlternativeFax1(f1);
        f.setFirmaData(info);
        return f;
    }

    private static boolean isFirmaAlr(List<Firma> lx, Firma fx) {
        for (Firma x : lx) {
            if (x.getFirmaData().getName().equals(fx.getFirmaData().getName())) if (fx != x) return true;
        }
        return false;
    }

    protected String buildLinkWithOffsetNo(String type, String country, long offsetNo) {
        String result = "http://www.bfr.de/bfrsearch.php?cmd=search&branche=" + type;
        result += "&ap=" + (offsetNo - 1);
        return result;
    }

    protected String httpToStringStupid(String url, String encoding) throws IllegalStateException, IOException, HttpException, InterruptedException, URISyntaxException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY);
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String pageDump = IOUtils.toString(entity.getContent(), encoding);
        return pageDump;
    }

    @Test
    public void makeShit() {
        BufferedReader in;
        VyhladavaciAlgoritmus va = vaDao.findById(2800849l);
        try {
            List<SearchCategory> scl = scDao.findAll(va);
            int index = 0;
            for (SearchCategory sc : scl) {
                index++;
                if (index <= 6606) continue;
                System.out.println(sc.getName());
                if (sc.getCode().startsWith("Ojo6O")) {
                    System.out.println("skipped");
                    continue;
                }
                String code = sc.getCode();
                String uri = buildLinkWithOffsetNo(code, null, 1);
                try {
                    String html = httpToStringStupid(uri, "iso-8859-1");
                    String codex = "bfrsearch.php?cmd=mysearch&amp;bfr=";
                    int tmpIndex = html.indexOf(codex) + codex.length();
                    String categ = html.substring(tmpIndex, html.indexOf("&amp;", tmpIndex));
                    System.out.println(index + " : " + categ);
                    sc.setCode(categ);
                    scDao.update(sc);
                    System.out.println(sc.getNoSpaceName() + " was update to above line");
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (HttpException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void test() {
        FirmaSearchCriteria c = new FirmaSearchCriteria();
        List<Firma> lx = dao.findByCriteria(c, 0, 100);
        for (Firma x : lx) {
            x.setCreated(new GregorianCalendar());
            if (isFirmaAlr(lx, x)) dao.delete(x);
        }
        System.out.println(lx.size());
    }
}
