package fi.vtt.noen.mfw.probes.http;

import fi.vtt.noen.mfw.bundle.common.ProbeConfiguration;
import fi.vtt.noen.mfw.bundle.probe.shared.BaseMeasure;
import fi.vtt.noen.mfw.bundle.probe.shared.BaseProbeAgent;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Grabs a base measure from a HTTP request posted at the address http://<address>/{bm-name}.
 * The base measure name is the part in the url and the content is the body of the http request.
 *
 * @author Teemu Kanstren
 */
public class HTTPProbeAgent extends BaseProbeAgent {

    public BaseMeasure measure() {
        return BaseMeasureFilter.values.get(getInformation().getBmName());
    }

    public void startProbe() {
    }

    public void stopProbe() {
    }

    public void setConfiguration(Map<String, String> configuration) {
    }

    public List<ProbeConfiguration> getConfigurationParameters() {
        return null;
    }

    public static void main(String[] args) throws Exception {
        URL target = new URL("http://localhost:8081/mfw/bm/os_version");
        HttpURLConnection url = (HttpURLConnection) target.openConnection();
        url.setDoInput(true);
        url.setDoOutput(true);
        url.setUseCaches(false);
        url.setRequestProperty("Content-Type", "text/plain");
        url.setRequestMethod("POST");
        DataOutputStream printout = new DataOutputStream(url.getOutputStream());
        String content = "A value has been observed.";
        printout.writeBytes(content);
        printout.flush();
        printout.close();
        BufferedReader br = new BufferedReader(new InputStreamReader(url.getInputStream()));
        String str;
        while (null != ((str = br.readLine()))) {
            System.out.println(str);
        }
        br.close();
    }
}
