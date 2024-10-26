package liugangc.appspot.example;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

@SuppressWarnings("serial")
public class GaeExampleServlet extends HttpServlet {

    static Log log = LogFactory.getLog(GaeExampleServlet.class);

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String urlStr = "http://www.csindex.com.cn/sseportal/ps/zhs/hqjt/csi/indexinfo.xls";
        PersistenceManager pm = PMF.get().getPersistenceManager();
        URL url = new URL(urlStr);
        InputStream is = url.openStream();
        POIFSFileSystem fs = new POIFSFileSystem(is);
        HSSFWorkbook wb = new HSSFWorkbook(fs);
        HSSFSheet sheet = wb.getSheetAt(0);
        String indexDate = sheet.getRow(1).getCell(3).getRichStringCellValue().getString();
        indexDate = indexDate.substring(1, indexDate.length() - 1);
        Query query = pm.newQuery("select from liugangc.appspot.example.StIndex where indexDate == indexDateParam parameters String indexDateParam");
        List<StIndex> results = (List<StIndex>) query.execute(indexDate);
        log.debug("=============" + results.size());
        if (results.size() > 0) {
            return;
        }
        for (int i = 4; i < 11; i++) {
            HSSFRow row = sheet.getRow(i);
            StIndex index = new StIndex();
            index.setPubDate(new Date());
            index.setIndexDate(indexDate);
            index.setName(row.getCell(0).getRichStringCellValue().getString());
            index.setClosing(row.getCell(1).getNumericCellValue());
            index.setUpDown(row.getCell(2).getNumericCellValue());
            index.setUpDownPercent(row.getCell(3).getNumericCellValue());
            index.setYearUpDown(row.getCell(4).getNumericCellValue());
            index.setYearUpDownPercent(row.getCell(5).getNumericCellValue());
            index.setTurnOverUpDown(row.getCell(6).getNumericCellValue());
            index.setTurnOverUpDownPercent(row.getCell(7).getNumericCellValue());
            index.setStaticPE(row.getCell(8).getNumericCellValue());
            index.setDynaPE(row.getCell(9).getNumericCellValue());
            index.setPB(row.getCell(10).getNumericCellValue());
            index.setLastYearStaticPE(row.getCell(11).getNumericCellValue());
            index.setLastYearDynaPE(row.getCell(12).getNumericCellValue());
            index.setLastYearPB(row.getCell(13).getNumericCellValue());
            pm.makePersistent(index);
        }
        is.close();
    }
}
