package tw.idv.cut7man.cuttle.core.schedule;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.TimerTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import tw.idv.cut7man.cuttle.core.util.CompositePageUtil;
import tw.idv.cut7man.cuttle.vo.XMLAction;
import tw.idv.cut7man.cuttle.vo.XMLCacheBuilder;

public class GeneralCacheTask extends TimerTask {

    private Log logger = LogFactory.getLog("[CacheTask]");

    public void run() {
        buildCache();
    }

    public void buildCache() {
        XMLCacheBuilder cacheBuilder = CompositePageUtil.getCacheBuilder();
        String postFix = "";
        if (cacheBuilder.getPostFix() != null && !cacheBuilder.getPostFix().equals("")) {
            postFix = "." + cacheBuilder.getPostFix();
        }
        String basePath = cacheBuilder.getBasePath();
        List actions = CompositePageUtil.getXMLActions();
        for (int i = 0; i < actions.size(); i++) {
            try {
                XMLAction action = (XMLAction) actions.get(i);
                if (action.getEscapeCacheBuilder() != null && action.getEscapeCacheBuilder().equals("true")) continue;
                String actionUrl = basePath + action.getName() + postFix;
                URL url = new URL(actionUrl);
                HttpURLConnection huc = (HttpURLConnection) url.openConnection();
                huc.setDoInput(true);
                huc.setDoOutput(true);
                huc.setUseCaches(false);
                huc.setRequestProperty("Content-Type", "text/html");
                DataOutputStream dos = new DataOutputStream(huc.getOutputStream());
                dos.flush();
                dos.close();
                huc.disconnect();
            } catch (MalformedURLException e) {
                logger.error(e);
                e.printStackTrace();
            } catch (IOException e) {
                logger.equals(e);
                e.printStackTrace();
            }
        }
    }
}
