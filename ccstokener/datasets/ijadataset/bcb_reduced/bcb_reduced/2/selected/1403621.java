package eduburner.crawler.http;

import java.io.IOException;
import org.apache.http.client.HttpClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

public class CommonsClientHttpRequest extends AbstractClientHttpRequest {

    private HttpClient client;

    public CommonsClientHttpRequest(HttpClient client) {
        this.client = client;
    }

    @Override
    protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
        return null;
    }

    @Override
    public HttpMethod getMethod() {
        return null;
    }
}
