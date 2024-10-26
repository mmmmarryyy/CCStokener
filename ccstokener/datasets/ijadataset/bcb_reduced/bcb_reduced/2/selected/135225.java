package juploader.httpclient;

import juploader.httpclient.exceptions.RequestCancelledException;
import juploader.httpclient.exceptions.TimeoutException;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.ExecutionContext;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Abstrakcyjne żądanie HTTP grupujący wspólne elementy.
 *
 * @author Adam Pawelec
 */
public abstract class AbstractHttpRequest {

    /** Adres URL, na który wykonane ma zostać żądanie. */
    protected URL url;

    /** Klient HTTP, przy pomocy którego wykonane będzie żądanie. */
    protected final org.apache.http.impl.client.DefaultHttpClient httpClient;

    /** Kontekst HTTP (magazyn ciasteczek etc.) */
    protected final org.apache.http.protocol.HttpContext context;

    /** Lista parametrów dołączonych do żądania. */
    protected final List<NVPair> parameters;

    /** Lista ciasteczek dołączonych do żądania. */
    protected final List<NVPair> cookies;

    /** Lista nagłówków dołączonych do żądania. */
    protected final List<NVPair> headers;

    /** Słuchacz postępu wykonania żądania. */
    protected RequestProgressListener progressListener;

    /** Flaga określająca zakończenie żądania. */
    protected volatile boolean abortFlag = false;

    /** Określa, czy przekierowania mają być obsługiwane. */
    protected boolean handleRedirects = true;

    /** Licznik przekierowań. */
    private int redirectsCount = 0;

    /**
     * Maksymalna liczba przekierować, po osiągnięciu której dalsze nie będą
     * wykonywane, co ma zapobiec ew. zapętleniu.
     */
    private static final int MAX_REDIRECTS = 5;

    /** Aktualnie wykonywane żądanie Apache HttpClient. */
    private HttpUriRequest request;

    /**
     * Tworzy nowe żądanie które ma być realizowane na kliencie podanym jako
     * argument. Konstruktor ten ma widoczność pakietową, gdyż przeznaczony jest
     * do użycia tylko przez klasę <i>HttpClient</i>, która zapewnia
     * współdzielenie klienta między różnymi żądaniami.
     */
    public AbstractHttpRequest() {
        this(new DefaultHttpClient(), null);
    }

    /**
     * Tworzy żądanie które ma być realizowane na nowym kliencie HTTP - aby
     * współdzielić stan między żądaniami trzeba odczytać ciasteczka z
     * otrzymanej odpowiedzi i dołączyć do innego żądania. W większości sytuacji
     * bardziej zalecanym sposobem postępowania jest utworzenie instancji klasy
     * <b>HttpClient</b> i skorzystanie z jej metod fabrykujących do tworzenia
     * żądań, dzięki czemu wszystkie będą współdzieliły magazyn ciasteczek.
     *
     * @param httpClient klient HTTP w ramach którego wykonywane ma być żądanie
     * @param context    kontekst
     * @see juploader.httpclient.HttpClient
     */
    AbstractHttpRequest(org.apache.http.impl.client.DefaultHttpClient httpClient, org.apache.http.protocol.HttpContext context) {
        this.httpClient = httpClient;
        this.context = context;
        this.parameters = new ArrayList<NVPair>();
        this.cookies = new ArrayList<NVPair>();
        this.headers = new ArrayList<NVPair>();
    }

    /**
     * Wykonuje żądanie i zwraca odpowiedź.
     *
     * @return obiekt reprezentujący odpowiedź, z ciałem, kodem, statusem etc.
     *         Niektóre implementacje, dla których obiekt ten nie
     *         reprezentowałby użytecznej wartości (np. zapisujące dane
     *         bezpośrednio do pliku) mogą zwracać null.
     * @throws IllegalStateException     jeżeli próbowano wykonać żądanie przed
     *                                   dokonaniem niezbędnych ustawień np.
     *                                   określeniem URLa
     * @throws IOException               w przypadku błędu IO, np. błędzie
     *                                   połączenia lub po podaniu nieprawidłowego
     *                                   adresu
     * @throws RequestCancelledException jeżeli w trakcie wykonywania żądania
     *                                   nastąpiło jego anulowanie
     * @throws TimeoutException          w przypadku przeterminowania
     */
    public HttpResponse makeRequest() throws IOException, TimeoutException, RequestCancelledException {
        checkState();
        try {
            request = createRequest();
            org.apache.http.HttpResponse response = httpClient.execute(request, context);
            if (checkAbortFlag()) {
                throw new RequestCancelledException();
            }
            return handleRedirectIfNeeded(response);
        } catch (SocketTimeoutException ex) {
            throw new TimeoutException();
        } catch (ConnectTimeoutException ex) {
            throw new TimeoutException();
        } catch (InterruptedIOException ex) {
            throw new RequestCancelledException();
        } catch (SocketException ex) {
            throw new RequestCancelledException();
        } catch (URISyntaxException ex) {
            throw new IOException("Invalid URL");
        }
    }

    /**
     * Metoda zwracająca żądanie do wykonania. Implementacje, o ile nie
     * nadpisują metody <i>makeRequest()</i>, tworzą w niej żądanie, które
     * zostanie wykonane na kliencie HTTP i jest to jedyna rzecz, którą muszą
     * zrobić.
     *
     * @return żądanie do wykonania
     * @throws URISyntaxException w przypadku nieprawidłowego URLa
     */
    abstract HttpUriRequest createRequest() throws URISyntaxException;

    /**
     * Ustawia adres URL, będący celem żądania.
     *
     * @param url adres URL
     */
    public void setUrl(URL url) {
        this.url = url;
    }

    /**
     * Ustawia adres URL, będący celem żądania. Wyjątek związany z ewentualnym
     * niepoprawnym adresem URL jest ignorowany, gdyż zostanie on rzucony przy
     * próbie wykonania żądania.
     *
     * @param url adres URL w postaci stringa
     */
    public void setUrl(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException ignored) {
        }
    }

    /**
     * Określenie, czy przekierowania mają być obsługiwane. Jeżeli nie, zwrócony
     * obiekt odpowiedzi zawierać będzie status <i>30x</i>. Jeżeli tak, to po
     * wykonaniu pierwszego żądania i stwierdzeniu wystąpienia przekierowania
     * zostanie wykonane drugie żądanie na adres przekierowania i obiekt
     * odpowiedzi będzie zawierał wyniki tego żądania. W przypadku logowania
     * obsługiwanie przekierowań nie jest najczęściej konieczne, gdyż ważne są
     * ciasteczka (zapisane do klienta HTTP na rzecz którego zostaje wykonane
     * żądanie) a nie sama odpowiedź.
     *
     * @param handleRedirects true, jeżeli przekierowania mają być obsługiwane,
     *                        false jeżeli nie
     */
    public void setHandleRedirects(boolean handleRedirects) {
        this.handleRedirects = handleRedirects;
    }

    /**
     * Metoda obsługująca przekierowanie lub, jeżeli nie zostało ono ustawione
     * albo nie jest potrzebne, zwracająca bezpośrednio obiekt odpowiedzi.
     *
     * @param responseToRedirect żądanie do przekierowania
     * @return obiekt odpowiedzi
     * @throws IOException w przypadku błędu IO
     */
    protected HttpResponse handleRedirectIfNeeded(org.apache.http.HttpResponse responseToRedirect) throws IOException {
        Header locationHeader = responseToRedirect.getFirstHeader("Location");
        if (!handleRedirects || redirectsCount > MAX_REDIRECTS || locationHeader == null) {
            return new HttpResponse(responseToRedirect);
        } else {
            String location = locationHeader.getValue();
            String host = ((HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST)).toURI();
            String redirectedUrl = getRedirectLocation(host, location);
            responseToRedirect.getEntity().consumeContent();
            HttpGet get = new HttpGet(redirectedUrl);
            org.apache.http.HttpResponse response = httpClient.execute(get, context);
            redirectsCount++;
            return handleRedirectIfNeeded(response);
        }
    }

    /**
     * Pomocnicza funkcja dokonująca złączenia hosta i adresu przekierowania w
     * sposób, który zwraca poprawny URL. Niektóre serwery mogą w Location
     * zwracać URL bewzwględny i wówczas on jest zwracaną wartością, a inne w
     * sposób względny (co jest niezgodne ze standardem HTTP ale niestety czasem
     * się zdarza) i wówczas następuje połączenie.
     *
     * @param host     adres hosta
     * @param location adres przekierowania
     * @return string zawierający kompletny URL przekierowania
     */
    protected String getRedirectLocation(String host, String location) {
        if (location.startsWith("http://")) {
            return location;
        } else {
            return String.format("%s/%s", host, location);
        }
    }

    /**
     * Dodaje parametr do żądania.
     *
     * @param name  nazwa parametru
     * @param value wartość parametru
     */
    public void addParameter(String name, String value) {
        parameters.add(new NVPair(name, value));
    }

    /**
     * Dodaje parametry do żądania.
     *
     * @param parameters kolekcja parametrów, zapisanych w obiektach {@link
     *                   NVPair}
     */
    public void addParameters(Collection<NVPair> parameters) {
        this.parameters.addAll(parameters);
    }

    /**
     * Dodaje ciasteczko do żądania.
     *
     * @param name  nazwa ciasteczka
     * @param value wartość ciasteczka
     */
    public void addCookie(String name, String value) {
        cookies.add(new NVPair(name, value));
    }

    /**
     * Dodaje ciasteczka do żądania.
     *
     * @param cookies kolekcja ciasteczek, zapisanych w obiektach {@link
     *                NVPair}
     */
    public void addCookies(Collection<NVPair> cookies) {
        this.cookies.addAll(cookies);
    }

    /**
     * Dodaje nagłówek do żądania.
     *
     * @param name  nazwa nagłówka
     * @param value wartość nagłówka
     */
    public void addHeader(String name, String value) {
        headers.add(new NVPair(name, value));
    }

    /**
     * Dodaje nagłówki do żądania.
     *
     * @param headers kolekcja nagłówków, zapisanych w obiektach {@link NVPair}
     */
    public void addHeaders(Collection<NVPair> headers) {
        this.headers.addAll(headers);
    }

    /**
     * Sprawdza, czy żądanie jest gotowe do realizacji. Implementacje mogą (a
     * nawet powinny) nadpisywać tę metodę, z wywołaniem
     * <i>super.checkState()</i> i specyfikować w niej dodatkowe wymagane
     * ustawienia przed wykonaniem żądania (np. określenie pliku do wysłania).
     *
     * @throws IllegalStateException jeżeli żądanie nie jest gotowe do
     *                               realizacji
     */
    protected void checkState() throws IllegalStateException {
        if (url == null) {
            throw new IllegalStateException("URL was not set or was not set properly.");
        }
    }

    /**
     * Pomocnicza funkcja dokonująca konwersji z postaci listy {@link NVPair} na
     * akceptowalną przez bibliotekę Apache HttpClient.
     *
     * @param pairs lista par - obiektów {@link NVPair}
     * @return lista prt - obiektów {@link NameValuePair}
     */
    protected List<NameValuePair> nvPairsListToApache(List<NVPair> pairs) {
        List<NameValuePair> result = new ArrayList<NameValuePair>();
        for (NVPair pair : pairs) {
            result.add(new BasicNameValuePair(pair.getName(), pair.getValue()));
        }
        return result;
    }

    /**
     * Sprawdza flagę anulowania realizacji żądania.
     *
     * @return true jeżeli ustawiono flagę
     */
    public boolean checkAbortFlag() {
        return abortFlag;
    }

    /** Przerywa wykonanie żądania. */
    public void abort() {
        abortFlag = true;
        try {
            Thread.sleep(760);
        } catch (InterruptedException ignored) {
        }
        request.abort();
    }

    /**
     * Rejestruje słuchacza postępu realizacji żądania.
     *
     * @param progressListener słuchacz
     */
    public void setProgressListener(RequestProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    protected void started(long fileSize) {
        if (progressListener != null) {
            progressListener.started(fileSize);
        }
    }

    protected void cancelled() {
        if (progressListener != null) {
            progressListener.cancelled();
        }
    }

    protected void finished() {
        if (progressListener != null) {
            progressListener.finished();
        }
    }

    protected void progress(long transferred) {
        if (progressListener != null) {
            progressListener.progress(transferred);
        }
    }
}
