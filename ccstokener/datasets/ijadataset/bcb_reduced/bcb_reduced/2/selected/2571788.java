package net.mjrz.fm.utils;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import net.mjrz.fm.Version;
import net.mjrz.fm.ui.dialogs.AboutDialog;
import net.mjrz.fm.ui.utils.UIDefaults;
import com.ifreebudget.web.service.response.UpdateCheckResponse;
import static net.mjrz.fm.utils.Messages.tr;

public class UpdateCheck {

    public static boolean updateCheck() throws Exception {
        String v = Version.getVersion().getVersionId();
        v = v.replaceAll("\\.", "_");
        URL url = new URL(UIDefaults.API_URL + "version/" + v);
        BufferedReader in = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            int status = conn.getResponseCode();
            if (status == 200) {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder str = new StringBuilder();
                while (true) {
                    String line = in.readLine();
                    if (line == null) break;
                    str.append(line);
                }
                System.out.println(str);
                JAXBContext jc = JAXBContext.newInstance("com.ifreebudget.web.service.response");
                Unmarshaller um = jc.createUnmarshaller();
                JAXBElement<UpdateCheckResponse> elem = (JAXBElement<UpdateCheckResponse>) um.unmarshal(new StringReader(str.toString()));
                UpdateCheckResponse resp = elem.getValue();
                if (resp.isUpdateRequired()) {
                    return true;
                } else {
                    return false;
                }
            } else {
                throw new RuntimeException(tr("Unable to get latest version, server unavailable."));
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
