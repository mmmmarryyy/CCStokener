package juploader.httpclient;

import juploader.httpclient.exceptions.RequestCancelledException;
import juploader.httpclient.exceptions.TimeoutException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Klient HTTP. Udostępnia metody fabrykujące służące do tworzenia żądań, które
 * będą wykonane w ramach jednego i tego samego klienta, co skutkuje m.in.
 * współdzieleniem stanu zapisanego w ciasteczach. Po zakończeniu korzystania z
 * klienta należy go zamknąć metodą <i>close</i>.
 *
 * @author Adam Pawelec
 */
public class HttpClient implements Closeable {

    /** Wykorzystywany "pod maską" klient HTTP z biblioteki Apache. */
    private final DefaultHttpClient httpClient;

    /** Magazyn ciasteczek, współdzielony przez wszystkie żądania. */
    private final CookieStore cookieStore;

    /** Kontekst przekazywany do każdego żądania. */
    private final HttpContext context;

    /** Ustawienia klienta. */
    private final HttpParams httpParams;

    private HttpClient() {
        httpParams = new BasicHttpParams();
        httpClient = new DefaultHttpClient(httpParams);
        cookieStore = new BasicCookieStore();
        context = new BasicHttpContext();
        context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    /**
     * Tworzy nowego klienta HTTP.
     *
     * @return klient HTTP
     */
    public static HttpClient createInstance() {
        return new HttpClient();
    }

    /**
     * Tworzy nowy obiekt do realizacji żądania wysłania formularza.
     *
     * @return obiekt FormSubmitRequest
     */
    public FormSubmitRequest createFormSubmitRequest() {
        return new FormSubmitRequest(httpClient, context);
    }

    /**
     * Tworzy nowy obiekt do realizacji pobierania pliku.
     *
     * @return obiekt FileDownloadRequest
     */
    public FileDownloadRequest createFileDownloadRequest() {
        return new FileDownloadRequest();
    }

    /**
     * Tworzy nowy obiekt do realizacji wysyłania pliku.
     *
     * @return obiekt FileUploadRequest
     */
    public FileUploadRequest createFileUploadRequest() {
        return new FileUploadRequest(httpClient, context);
    }

    /**
     * Tworzy nowy obiekt do pobierania zawartości stron.
     *
     * @return obiekt GetPageRequest
     */
    public GetPageRequest createGetPageRequest() {
        return new GetPageRequest(httpClient, context);
    }

    /**
     * Wykonuje podane żądanie <i>request</i>. Robi dokładnie to samo co
     * <i>request.makeRequest()</i>, jednak użycie tej funkcji pozwala uwypuklić
     * fakt, iż żądania są wykonywane przez jednego klienta.
     *
     * @param request żądanie do wykonania
     * @return obiekt odpowiedzi
     */
    public HttpResponse execute(AbstractHttpRequest request) throws IOException, RequestCancelledException, TimeoutException {
        return request.makeRequest();
    }

    /**
     * Ustawienie limitu czasu połączenia.
     *
     * @param connectionTimeoutMillis limit czasu połązenia w ms
     */
    public void setConnectionTimeout(int connectionTimeoutMillis) {
        HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeoutMillis);
    }

    /**
     * Ustawienie limitu czasu otrzymywania danych.
     *
     * @param socketTimeoutMillis limit czasu otrzymywania danych w ms
     */
    public void setSocketTimeout(int socketTimeoutMillis) {
        HttpConnectionParams.setSoTimeout(httpParams, socketTimeoutMillis);
    }

    /**
     * Zwraca listę ciasteczek klienta, które otrzymał od serwerów w ramach
     * kolejnych żądań. Metoda przeznaczona tylko do testów.
     *
     * @return lista ciasteczek
     */
    protected List<NVPair> getClientCookies() {
        List<Cookie> storeCookies = cookieStore.getCookies();
        List<NVPair> cookies = new ArrayList<NVPair>(storeCookies.size());
        for (Cookie cookie : storeCookies) {
            cookies.add(new NVPair(cookie.getName(), cookie.getName()));
        }
        return cookies;
    }

    /**
     * Zamyka klienta HTTP i zwalnia wszystkie połączenia, również te aktywne.
     * Metoda ta powinna zostać wywołana po zakończeniu pracy z klientem,
     * niezależnie od tego czy nastąpiła ona na skutek normalnego zakończnia
     * żądań czy też jakiegoś błędu.
     */
    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.getConnectionManager().shutdown();
        }
    }
}
