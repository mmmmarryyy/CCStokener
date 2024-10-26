package com.googlecode.phugushop.drawing;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class SpreadsheetUtils {

    public HSSFWorkbook getWorkbookFromClassPath(String name) throws IOException {
        InputStream is = null;
        try {
            URL url = this.getClass().getClassLoader().getResource(name);
            is = url.openStream();
            POIFSFileSystem fs = new POIFSFileSystem(is);
            return new HSSFWorkbook(fs);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public HSSFWorkbook getWorkbookFromFile(File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            POIFSFileSystem fs = new POIFSFileSystem(fis);
            return new HSSFWorkbook(fs);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    public void saveWorkbookToFile(HSSFWorkbook wb, File file) throws FileNotFoundException, IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            wb.write(fos);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    public HSSFWorkbook copyWorkbook(HSSFWorkbook in) {
        return null;
    }

    /**
     * Write a new value into a cell
     * 
     * @param value The value to write into the cell
     * @param sheet The sheet the cell is on
     * @param columnNum starting from 1 ie A=1, B=2
     * @param rowNum starting from 1
     */
    public void writeCell(String value, HSSFSheet sheet, short columnNum, int rowNum) {
        HSSFCell cell = findCell(sheet, true, columnNum, rowNum);
        cell.setCellValue(value);
    }

    private HSSFCell findCell(HSSFSheet sheet, boolean create, short columnNum, int rowNum) {
        int rowNo = rowNum - 1;
        short columnNo = (short) (columnNum - 1);
        HSSFRow row = sheet.getRow(rowNo);
        if (row == null) {
            row = sheet.createRow(rowNo);
        }
        HSSFCell cell = row.getCell(columnNo);
        if (cell == null) {
            cell = row.createCell(columnNo);
        }
        return cell;
    }

    /**
     * Read value from a text cell
     * 
     * @param sheet The sheet the cell is on
     * @param columnNum starting from 1 ie A=1, B=2
     * @param rowNum starting from 1
     * @return The contents of the cell
     */
    public String readCell(HSSFSheet sheet, short columnNum, int rowNum) {
        HSSFCell cell = findCell(sheet, false, columnNum, rowNum);
        if (cell == null) {
            return null;
        }
        return cell.getStringCellValue();
    }

    public double readNumericCell(HSSFSheet sheet, short columnNum, int rowNum) {
        HSSFCell cell = findCell(sheet, false, columnNum, rowNum);
        if (cell == null) {
            return -1;
        }
        return cell.getNumericCellValue();
    }

    class Location {

        int row;

        short column;
    }
}
