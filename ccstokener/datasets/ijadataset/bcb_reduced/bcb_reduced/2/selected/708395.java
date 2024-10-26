package org.wikiup.modules.testcase.util;

import java.net.HttpURLConnection;
import java.net.URL;
import org.wikiup.core.inf.Document;
import org.wikiup.core.inf.Getter;
import org.wikiup.core.util.Assert;
import org.wikiup.core.util.Documents;
import org.wikiup.core.util.StringUtil;

public class TestCases {

    public static boolean doTest(Getter<?> context, Document node) {
        try {
            URL url = new URL(StringUtil.evaluateEL(Documents.getDocumentValue(node, "url"), context));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            Assert.isTrue(conn.getResponseCode() < 400);
            conn.disconnect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
