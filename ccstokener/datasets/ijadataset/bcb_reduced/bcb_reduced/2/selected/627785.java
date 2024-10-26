package net.sf.webwarp.base.i18n.impl;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.sf.webwarp.base.i18n.Message;
import net.sf.webwarp.base.i18n.MessageBundle;
import net.sf.webwarp.base.i18n.MessageLocale;
import net.sf.webwarp.base.i18n.MutableMessageProvider;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.springframework.util.StringUtils;

/**
 * Helper class for importing/exporting properties/messages to and from Excel 97.
 * 
 * @author atr: Extracted from DAO message provider implementation.
 */
public class ExcelHelper {

    private static final Logger log = Logger.getLogger(ExcelHelper.class);

    /**
     * Singleton constructor
     */
    private ExcelHelper() {
    }

    public static List<HSSFWorkbook> extractWorkbooks(Collection<String> paths) {
        List<HSSFWorkbook> workbooks = new ArrayList<HSSFWorkbook>();
        for (String path : paths) {
            if (path.endsWith(".xls")) {
                try {
                    URL url = new URL(path);
                    workbooks.add(new HSSFWorkbook(new POIFSFileSystem(url.openStream())));
                } catch (IOException e) {
                    log.warn("could not install the file: " + path, e);
                }
            }
        }
        return workbooks;
    }
}
