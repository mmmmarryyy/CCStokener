package egovframework.com.utl.fcc.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

public class EgovEhgtCalcUtil {

    static final char EGHT_USD = 'U';

    static final char EGHT_JPY = 'J';

    static final char EGHT_EUR = 'E';

    static final char EGHT_CNY = 'C';

    static final char EGHT_KWR = 'K';

    static StringBuffer sb = new StringBuffer();

    /**
     * 대한민국(KRW), 미국(USD), 유럽연합(EUR), 일본(JPY), 중국원화(CNY) 사이의 환율을 계산하는 기능이다
	 * 환율표 - 매매기준율 => 미국(USD) - 1485.00(USD), 일본-100(JPY) - 1596.26(JPY)
	 * 계산법: 대한민원(KRW) - 1,000원 -> 미국(USD)로 변환 시 => 1,000(원)/1485(매매기준율) = 0.67(URS)
	 * 계산법: 일본(JPY) - 100,000원 -> 대한민국(KRW) 변환 시 => (100,000(원) * 1596.26(매매기준율)) / 100(100엔당 기준표이므로) = 1,596,260.00 (KRW)
	 * 계산법: 일본(JPY) - 100,000원 -> 미국(USD) 변환 시     => (
	 * (100,000(원) * 1596.26(매매기준율)) / 100(100엔당 기준표이므로) = 1,596,260.00 (KRW))  / 1,485.00 = 1,074.92 (USD)
     * @param srcType 			- 환율기준
     * @param srcAmount 		- 금액
     * @param cnvrType 			- 변환환율
     * @return 환율금액
     * @exception MyException
     * @see
     */
    public void readHtmlParsing(String str) {
        HttpURLConnection con = null;
        InputStreamReader reader = null;
        try {
            URL url = new URL(str);
            con = (HttpURLConnection) url.openConnection();
            reader = new InputStreamReader(con.getInputStream(), "euc-kr");
            new ParserDelegator().parse(reader, new CallbackHandler(), true);
            con.disconnect();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (con != null) {
                con.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private class CallbackHandler extends HTMLEditorKit.ParserCallback {

        public void handleText(char[] data, int pos) {
            String srcStr = new String(data);
            srcStr = EgovStringUtil.strip(srcStr, " ");
            sb.append(srcStr).append("/");
        }
    }

    public static String getEhgtCalc(String srcType, long srcAmount, String cnvrType) throws Exception {
        String rtnStr = null;
        String[] eghtStdrRt = null;
        double srcStdrRt = 0.00;
        double cnvrStdrRt = 0.00;
        double cnvrAmount = 0.00;
        String sCnvrAmount = null;
        String srcStr = null;
        String cnvrStr = null;
        String srcTypeCnvr = srcType.toUpperCase();
        String cnvrTypeCnvr = cnvrType.toUpperCase();
        try {
            EgovEhgtCalcUtil parser = new EgovEhgtCalcUtil();
            parser.readHtmlParsing("http://community.fxkeb.com/fxportal/jsp/RS/DEPLOY_EXRATE/4176_0.html");
            if (sb == null) {
                throw new RuntimeException("StringBuffer is null!!");
            }
            eghtStdrRt = EgovStringUtil.split(sb.toString(), "/");
        } catch (Exception e) {
            System.out.println(e);
        }
        if (eghtStdrRt == null || (eghtStdrRt.length == 0)) throw new java.lang.Exception("String Split Error!");
        char srcChr = srcTypeCnvr.charAt(0);
        char cnvrChr = cnvrTypeCnvr.charAt(0);
        switch(srcChr) {
            case EGHT_USD:
                srcStr = "USD";
                break;
            case EGHT_JPY:
                srcStr = "JPY";
                break;
            case EGHT_EUR:
                srcStr = "EUR";
                break;
            case EGHT_CNY:
                srcStr = "CNY";
                break;
            default:
                srcStr = "USD";
                break;
        }
        switch(cnvrChr) {
            case EGHT_USD:
                cnvrStr = "USD";
                break;
            case EGHT_JPY:
                cnvrStr = "JPY";
                break;
            case EGHT_EUR:
                cnvrStr = "EUR";
                break;
            case EGHT_CNY:
                cnvrStr = "CNY";
                break;
            default:
                cnvrStr = "KRW";
                break;
        }
        try {
            for (int i = 0; i < eghtStdrRt.length; i++) {
                if (eghtStdrRt[i].equals(srcStr)) {
                    srcStdrRt = Double.parseDouble(eghtStdrRt[i + 1]);
                    if (i == (eghtStdrRt.length - 1)) break;
                }
                if (eghtStdrRt[i].equals(cnvrStr)) {
                    cnvrStdrRt = Double.parseDouble(eghtStdrRt[i + 1]);
                    if (i == (eghtStdrRt.length - 1)) break;
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        BigDecimal bSrcAmount = new BigDecimal(String.valueOf(srcAmount));
        BigDecimal bSrcStdrRt = new BigDecimal(String.valueOf(srcStdrRt));
        BigDecimal bCnvrStdrRt = new BigDecimal(String.valueOf(cnvrStdrRt));
        BigDecimal bStdr = new BigDecimal("100");
        try {
            switch(srcChr) {
                case EGHT_KWR:
                    if (cnvrChr == 'K') sCnvrAmount = bSrcAmount.toString(); else if (cnvrChr == 'J') sCnvrAmount = (bSrcAmount.divide(bCnvrStdrRt, 4, 4)).multiply(bStdr).setScale(2, 4).toString(); else sCnvrAmount = bSrcAmount.divide(bCnvrStdrRt, 2, 4).toString();
                    break;
                case EGHT_USD:
                    if (cnvrChr == 'U') sCnvrAmount = bSrcAmount.toString(); else if (cnvrChr == 'K') sCnvrAmount = bSrcAmount.multiply(bSrcStdrRt).setScale(2, 4).toString(); else if (cnvrChr == 'J') sCnvrAmount = ((bSrcAmount.multiply(bSrcStdrRt).setScale(4, 4)).divide(bCnvrStdrRt, 2, 4)).multiply(bStdr).setScale(2, 4).toString(); else sCnvrAmount = (bSrcAmount.multiply(bSrcStdrRt).setScale(4, 4)).divide(bCnvrStdrRt, 2, 4).toString();
                    break;
                case EGHT_EUR:
                    if (cnvrChr == 'E') sCnvrAmount = bSrcAmount.toString(); else if (cnvrChr == 'K') sCnvrAmount = bSrcAmount.multiply(bSrcStdrRt).setScale(2, 4).toString(); else if (cnvrChr == 'J') sCnvrAmount = ((bSrcAmount.multiply(bSrcStdrRt).setScale(4, 4)).divide(bCnvrStdrRt, 2, 4)).multiply(bStdr).setScale(2, 4).toString(); else sCnvrAmount = (bSrcAmount.multiply(bSrcStdrRt).setScale(4, 4)).divide(bCnvrStdrRt, 2, 4).toString();
                    break;
                case EGHT_JPY:
                    if (cnvrChr == 'J') sCnvrAmount = bSrcAmount.toString(); else if (cnvrChr == 'K') sCnvrAmount = (bSrcAmount.multiply(bSrcStdrRt).setScale(4, 4)).divide(bStdr, 2, 4).toString(); else sCnvrAmount = ((bSrcAmount.multiply(bSrcStdrRt).setScale(4, 4)).divide(bStdr, 2, 4)).divide(bCnvrStdrRt, 2, 4).toString();
                    break;
                case EGHT_CNY:
                    if (cnvrChr == 'C') sCnvrAmount = bSrcAmount.toString(); else if (cnvrChr == 'K') sCnvrAmount = bSrcAmount.multiply(bSrcStdrRt).setScale(2, 4).toString(); else if (cnvrChr == 'J') sCnvrAmount = ((bSrcAmount.multiply(bSrcStdrRt).setScale(4, 4)).divide(bCnvrStdrRt, 2, 4)).multiply(bStdr).setScale(2, 4).toString(); else sCnvrAmount = (bSrcAmount.multiply(bSrcStdrRt).setScale(4, 4)).divide(bCnvrStdrRt, 2, 4).toString();
                    break;
                default:
                    sCnvrAmount = bSrcAmount.divide(bCnvrStdrRt, 2, 4).toString();
                    break;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        rtnStr = sCnvrAmount + "  " + cnvrStr;
        return rtnStr;
    }
}
