package juploader.upload.plugin;

import juploader.httpclient.AbstractHttpRequest;
import juploader.httpclient.FileUploadRequest;
import juploader.httpclient.HttpClient;
import juploader.httpclient.HttpResponse;
import juploader.httpclient.NVPair;
import juploader.httpclient.RequestProgressListener;
import juploader.httpclient.exceptions.RequestCancelledException;
import juploader.httpclient.exceptions.TimeoutException;
import juploader.plugin.AbstractPlugin;
import juploader.plugin.Plugin;
import juploader.upload.UploadResult;
import juploader.upload.exceptions.ChangesOnServerException;
import juploader.upload.exceptions.ConnectionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Abstrakcyjna klasa przeznaczona do rozszerzania przez autorów pluginów
 * <strong>(nie należy implementować interfejsu {@link UploadProvider}
 * bezpośrednio)</strong>. Implementuje ona część metod tego interfejsu,
 * pozostawiając do implementacji te, które zależą już od konkretnych serwisów
 * hostingowych i muszą zostać zdefiniowane we wtyczce.<br/><br/> Oto metody,
 * które muszą zostać zdefiniowane we wtyczkach: <ul> <li>upload(...) - kod
 * związany z wysyłaniem pliku do serwisu, na początku należy utworzyć obiekt
 * żądania korzystając z metody <em>createFileUploadRequest()</em>, pamiętając o
 * konieczności zamknięcia otrzymanej odpowiedzi</li> <li>getMaxFileSize() -
 * maksymalny rozmiar pliku obsługiwany przez dany serwis (w bajtach) lub -1
 * jeżeli brak ograniczeń</li> <li>getSupportedExtensions() - tablica
 * obsługiwanych rozszerzeń (bez kropki)</li> <li>getUploadOptionsPanel() -
 * metoda zwracająca JPanel służący do wprowadzania dodatkowych opcji uploadu,
 * jeżeli serwis takich nie posiada wtedy null</li> <li>getPluginInfo() - metoda
 * zwracająca obiekt zawierający informacje o wtyczce (wersje, autora etc.).
 * <strong>Proszę pamiętać szczególnie o aktualizowaniu wersji wtyczki po
 * wprowadzeniu zmian</strong></li> </ul> Wykonanie żądania w metodzie upload()
 * winno wyglądać w skrócie tak: <ol> <li>utworzenie żądania przy pomocy
 * createFileUploadRequest(parameters)</li> <li>konfiguracja otrzymanego obiektu
 * żądania (podanie parametrów)</li> <li>próba wykonania żądania przez klienta
 * HTTP, które otrzymujemy metodą getHttpClient()</li> <li>jeżeli na tym etapie
 * wystąpi wyjątek do rzucenie {@link ConnectionException} i koniec</li>
 * <li>jeżeli udało się wykonać żądanie i serwer zwrócił odpowiedź to:
 * wyciągnięcie linków z odpowiedzi i zapisanie ich do obiektu {@link
 * UploadResult}</li> <li>jeżeli w trakcie parsowania wystąpił bład to
 * zamknięcie obiektu odpowiedzi i rzucenie wyjątku {@link
 * ChangesOnServerException}</li> <li>jeżeli udało się sparsować linki to
 * zamknięcie obiektu odpowiedzi i zwrócenie wyników</li> </ol> Należy pamiętać
 * o prawidłowej obsłudze wyjątków - wyjątki klienta HTTP rzucone w trakcie
 * wykonywania żądania powinny zostać wyłapane  w metodzie upload i z tamtąd
 * wyrzucone jako {@link ConnectionException}, natomiast wszelkie błędy
 * wyjścia/wejścia, które wystąpią na etapie parsowania, zakładając poprawnie
 * wykonane połączenie i zwrócenie prawidłowej odpowiedzi, powinny skutkować
 * rzuceniem wyjątku {@link ChangesOnServerException}, który oznacza
 * prawdopodobne zmiany skryptu na serwerze i konieczność dopasowania do nich
 * wtyczki. Należy przy tym pisać wtyczki tak, aby jak najbardziej uniezależnić
 * implementację od tych zmian, głównie poprzez korzystanie z wyrażeń
 * regularnych, jednak całkowicie zapobiec im się nie da. <strong>Prawidłowa
 * obsługa wyjątków jest kluczowa - manager uploadu wykorzystuje je do
 * informowania użytkownika o tym, co poszło nie tak.</strong><br/><br/>
 * Wszelkie napisy zwracane użytkownikowi (przede wszystkim opisy linków/kodów
 * zwracanych przez serwisy) powinny znaleźć się w plikach .properties, tak by
 * umożliwić ich łatwą internacjonalizację.</br><br/>
 *
 * @author Adam Pawelec
 */
public abstract class AbstractUploadProvider extends AbstractPlugin implements UploadProvider {

    /** Klient HTTP wysyłający żądanie. */
    private HttpClient httpClient;

    /** Aktualnie realizowane żądanie wysyłania pliku. */
    private FileUploadRequest request;

    /** Słuchacz postępu wykonania żądania. */
    private RequestProgressListener listener;

    /** Odczytany maksymalny rozmiar pliku z adnotacji. */
    private long maxFileSize = -2;

    /** Odczytana lista obługiwanych plików z adnotacji. */
    private String[] supportedExtensions = null;

    public void init() {
        httpClient = HttpClient.createInstance();
    }

    public void finish() {
        try {
            httpClient.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void setProgressListener(RequestProgressListener listener) {
        this.listener = listener;
    }

    public void cancel() {
        if (request != null) {
            request.abort();
        }
    }

    /**
     * Metoda wykonująca żądanie i rzucająca odpowiednie wyjątki.
     *
     * @param request żądanie do wykonania
     * @return obiekt odpowiedzi
     * @throws ConnectionException       w przypadku błędu połączenia
     * @throws RequestCancelledException w przypadku przerwania uploadu
     */
    protected HttpResponse executeRequest(AbstractHttpRequest request) throws ConnectionException, RequestCancelledException {
        try {
            HttpResponse response = getHttpClient().execute(request);
            if (!response.is2xxSuccess()) {
                throw new ConnectionException();
            }
            return response;
        } catch (IOException ex) {
            throw new ConnectionException();
        } catch (TimeoutException ex) {
            throw new ConnectionException();
        }
    }

    /**
     * Zamyka obiekt odpowiedzi.
     *
     * @param response obiekt odpowiedzi
     */
    protected void closeReponse(HttpResponse response) {
        try {
            response.close();
        } catch (IOException ex) {
        }
    }

    /**
     * Zwraca odpowiedni przygotowane żądanie służące do wysyłania pliku na
     * serwer.
     *
     * @return żądanie
     */
    protected FileUploadRequest createFileUploadRequest(List<NVPair> parameters) {
        if (httpClient == null) {
            throw new IllegalStateException("HttpClient was not been created");
        }
        request = httpClient.createFileUploadRequest();
        request.setProgressListener(listener);
        if (parameters != null) {
            request.addParameters(parameters);
        }
        return request;
    }

    /**
     * Zwraca klienta HTTP wykonującego żądania.
     *
     * @return klient HTTP
     */
    protected HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Implementacja metody korzystająca z metod zwracających maksymalny rozmiar
     * pliku oraz tablicę rozszerzeń (zadeklarowanych oddzielnie celem łatwego
     * wyświetlania informacji o wtyczce w interfejsie użytkownika) w celu
     * sprawdzenia, czy dany plik jest obsługiwany.
     *
     * @param file plik do sprawdzenia
     * @return true, jeżeli jest obsługiwany
     */
    public boolean isFileSupported(File file) {
        if (file.length() == 0 || getMaxFileSize() != -1 && file.length() >= getMaxFileSize()) {
            return false;
        } else {
            boolean supported = false;
            if (getSupportedExtensions().length == 0) {
                supported = true;
            } else {
                for (String ext : getSupportedExtensions()) {
                    String pattern = String.format(".*\\.%s$", ext.toLowerCase());
                    if (file.getName().toLowerCase().matches(pattern)) {
                        supported = true;
                        break;
                    }
                }
            }
            return supported;
        }
    }

    public long getMaxFileSize() {
        if (maxFileSize == -2) {
            maxFileSize = readMaxFileSizeFromAnnotation();
        }
        return maxFileSize;
    }

    public String[] getSupportedExtensions() {
        if (supportedExtensions == null) {
            supportedExtensions = readSupportedExtensionsFromAnnotation();
        }
        return supportedExtensions;
    }

    /** Odczytuje tablicę obsługiwanych rozszerzeń z adnotacji. */
    private String[] readSupportedExtensionsFromAnnotation() {
        if (this.getClass().isAnnotationPresent(Plugin.class)) {
            Plugin ann = this.getClass().getAnnotation(Plugin.class);
            return ann.supportedExtensions();
        } else {
            throw new RuntimeException("Class not marked by annotation");
        }
    }

    /** Odczytuje maksymalny rozmiar pliku z adnotacji. */
    private long readMaxFileSizeFromAnnotation() {
        if (this.getClass().isAnnotationPresent(Plugin.class)) {
            Plugin ann = this.getClass().getAnnotation(Plugin.class);
            return ann.maxFileSize();
        } else {
            throw new RuntimeException("Class not marked by annotation");
        }
    }

    protected UploadResult executeRequestAndParseResponse(FileUploadRequest request, File uploadedFile) throws ConnectionException, RequestCancelledException, ChangesOnServerException {
        HttpResponse response = executeRequest(request);
        UploadResult uploadResult = parseResponseBody(response.getResponseBody(), uploadedFile);
        closeReponse(response);
        return uploadResult;
    }

    protected abstract UploadResult parseResponseBody(InputStream responseBody, File file) throws ChangesOnServerException;
}
