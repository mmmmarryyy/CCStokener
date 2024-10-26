package jp.joogoo.web.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.slim3.datastore.Datastore;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import com.google.api.client.http.HttpTransport;
import com.google.appengine.repackaged.org.json.JSONException;
import com.google.appengine.repackaged.org.json.JSONObject;
import jp.joogoo.web.meta.T_newMeta;
import jp.joogoo.web.model.M_joogoo;
import jp.joogoo.web.model.M_service;
import jp.joogoo.web.model.T_new;

public class ContentsService {

    private static final T_newMeta meta = new T_newMeta();

    private static final Logger logger = Logger.getLogger(ContentsService.class.getName());

    /**
     * アカウントに関する新着情報を処理します。 アカウントの認証ヘッダーを生成し、サービスプロバイダーに HTTP リクエストを投げます。
     * レスポンスはサービスプロバイダーに合わせたパーサーで処理します。
     * 
     * @param account
     * @throws IOException
     * 
     * @see <a
     *      href="http://javadoc.google-api-java-client.googlecode.com/hg/1.2.0-alpha/com/google/api/client/googleapis/xml/atom/package-summary.html">Google
     *      API Client Library for Java</a>
     */
    public void fetch(M_joogoo account) throws IOException {
        if (account.getToken() == null) {
            throw new IOException("user token is null.");
        }
        M_service service = account.getMServiceRef().getModel();
        OAuthConsumer consumer = service.getConsumer();
        try {
            JSONObject token = new JSONObject(account.getToken());
            String key = token.getString("key");
            String secret = token.getString("secret");
            consumer.setTokenWithSecret(key, secret);
        } catch (JSONException e) {
            throw new RuntimeException("invalid state, userId=" + account.getUserId(), e);
        }
        String targetUrl = service.getTargetUrl("NOTIFICATION");
        URL url = new URL(targetUrl);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        try {
            consumer.sign(c);
        } catch (OAuthMessageSignerException e) {
            throw new IOException(e.getMessage());
        } catch (OAuthExpectationFailedException e) {
            throw new IOException(e.getMessage());
        } catch (OAuthCommunicationException e) {
            throw new IOException(e.getMessage());
        }
        HttpTransport transport = new HttpTransport();
        transport.defaultHeaders.authorization = c.getRequestProperty("Authorization");
        FetcherService fetcher = FetcherServiceFactory.createFetcher(service);
        fetcher.setUpTransport(transport);
        List<T_new> news = fetcher.executeGet(transport, targetUrl);
        for (T_new n : news) {
            if (Datastore.getOrNull(T_new.class, n.getKey()) == null) {
                n.setUserId(account.getUserId());
                n.getMJoogooRef().setModel(account);
                Datastore.put(n);
            }
        }
    }

    /**
     * データストアから、アカウント情報に紐づくサービスプロバイダーにおけるユーザーの新着情報を取得します。
     * 認証済みアカウント情報が存在しない場合には空の配列を返します。
     * 
     * @param account
     * @param lastRequestSec
     * @return valid list not null.
     */
    public List<T_new> retrieve(M_joogoo account, long lastRequestSec) {
        if (account == null) {
            return Collections.emptyList();
        }
        Date since;
        if (lastRequestSec > 0) {
            since = new Date(lastRequestSec);
        } else {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE, -1);
            since = c.getTime();
        }
        List<T_new> news = account.getNewListRef().query().filter(meta.createdAt.greaterThan(since)).getModelList();
        if (news == null) {
            return Collections.emptyList();
        }
        logger.info(String.format("Retrieved %d news for %s since %s.", news.size(), account.getUserId(), since));
        return news;
    }

    /**
     * データストアから、１ヶ月より前の新着情報を削除します。
     */
    public void clear() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        Datastore.deleteWithoutTx(Datastore.query(meta).filter(meta.createdAt.lessThan(calendar.getTime())).asKeyList());
        logger.info("Clear old news items before " + calendar.getTime());
    }
}
