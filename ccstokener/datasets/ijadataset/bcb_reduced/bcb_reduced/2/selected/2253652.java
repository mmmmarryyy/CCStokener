package org.ofbiz.datafile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Stack;

/**
 *  Record Iterator for reading large files
 *
 */
public class RecordIterator {

    public static final String module = RecordIterator.class.getName();

    protected BufferedReader br;

    protected ModelDataFile modelDataFile;

    protected InputStream dataFileStream;

    protected boolean closed = false;

    protected String locationInfo;

    protected int nextLineNum = 0;

    protected String curLine = null;

    protected Record curRecord = null;

    protected String nextLine = null;

    protected Record nextRecord = null;

    public RecordIterator(URL fileUrl, ModelDataFile modelDataFile) throws DataFileException {
        this.modelDataFile = modelDataFile;
        InputStream urlStream = null;
        try {
            urlStream = fileUrl.openStream();
        } catch (IOException e) {
            throw new DataFileException("Error open URL: " + fileUrl.toString(), e);
        }
        this.setupStream(urlStream, fileUrl.toString());
    }

    public RecordIterator(InputStream dataFileStream, ModelDataFile modelDataFile, String locationInfo) throws DataFileException {
        this.modelDataFile = modelDataFile;
        this.setupStream(dataFileStream, locationInfo);
    }

    protected void setupStream(InputStream dataFileStream, String locationInfo) throws DataFileException {
        this.locationInfo = locationInfo;
        this.dataFileStream = dataFileStream;
        try {
            this.br = new BufferedReader(new InputStreamReader(dataFileStream, "UTF-8"));
        } catch (Exception e) {
            throw new DataFileException("UTF-8 is not supported");
        }
        this.getNextLine();
    }

    protected boolean getNextLine() throws DataFileException {
        this.nextLine = null;
        this.nextRecord = null;
        boolean isFixedRecord = ModelDataFile.SEP_FIXED_RECORD.equals(modelDataFile.separatorStyle);
        boolean isFixedLength = ModelDataFile.SEP_FIXED_LENGTH.equals(modelDataFile.separatorStyle);
        boolean isDelimited = ModelDataFile.SEP_DELIMITED.equals(modelDataFile.separatorStyle);
        if (isFixedRecord) {
            if (modelDataFile.recordLength <= 0) {
                throw new DataFileException("Cannot read a fixed record length file if no record length is specified");
            }
            try {
                char[] charData = new char[modelDataFile.recordLength + 1];
                if (br.read(charData, 0, modelDataFile.recordLength) == -1) {
                    nextLine = null;
                } else {
                    nextLine = new String(charData);
                }
            } catch (IOException e) {
                throw new DataFileException("Error reading line #" + nextLineNum + " (index " + (nextLineNum - 1) * modelDataFile.recordLength + " length " + modelDataFile.recordLength + ") from location: " + locationInfo, e);
            }
        } else {
            try {
                nextLine = br.readLine();
            } catch (IOException e) {
                throw new DataFileException("Error reading line #" + nextLineNum + " from location: " + locationInfo, e);
            }
        }
        if (nextLine != null) {
            nextLineNum++;
            ModelRecord modelRecord = findModelForLine(nextLine, nextLineNum, modelDataFile);
            if (isDelimited) {
                this.nextRecord = Record.createDelimitedRecord(nextLine, nextLineNum, modelRecord, modelDataFile.delimiter);
            } else {
                this.nextRecord = Record.createRecord(nextLine, nextLineNum, modelRecord);
            }
            return true;
        } else {
            this.close();
            return false;
        }
    }

    public int getCurrentLineNumber() {
        return this.nextLineNum - 1;
    }

    public boolean hasNext() {
        return nextLine != null;
    }

    public Record next() throws DataFileException {
        if (!hasNext()) {
            return null;
        }
        if (ModelDataFile.SEP_DELIMITED.equals(modelDataFile.separatorStyle) || ModelDataFile.SEP_FIXED_RECORD.equals(modelDataFile.separatorStyle) || ModelDataFile.SEP_FIXED_LENGTH.equals(modelDataFile.separatorStyle)) {
            boolean isFixedRecord = ModelDataFile.SEP_FIXED_RECORD.equals(modelDataFile.separatorStyle);
            this.curLine = this.nextLine;
            this.curRecord = this.nextRecord;
            this.getNextLine();
            if (!isFixedRecord && modelDataFile.recordLength > 0 && curLine.length() != modelDataFile.recordLength) {
                throw new DataFileException("Line number " + this.getCurrentLineNumber() + " was not the expected length; expected: " + modelDataFile.recordLength + ", got: " + curLine.length());
            }
            if (this.curRecord.getModelRecord().childRecords.size() > 0) {
                Stack parentStack = new Stack();
                parentStack.push(curRecord);
                while (this.nextRecord != null && this.nextRecord.getModelRecord().parentRecord != null) {
                    Record parentRecord = null;
                    while (parentStack.size() > 0) {
                        parentRecord = (Record) parentStack.peek();
                        if (parentRecord.recordName.equals(this.nextRecord.getModelRecord().parentName)) {
                            break;
                        } else {
                            parentStack.pop();
                            parentRecord = null;
                        }
                    }
                    if (parentRecord == null) {
                        throw new DataFileException("Expected Parent Record not found for line " + this.getCurrentLineNumber() + "; record name of expected parent is " + this.nextRecord.getModelRecord().parentName);
                    }
                    parentRecord.addChildRecord(this.nextRecord);
                    if (this.nextRecord.getModelRecord().childRecords.size() > 0) {
                        parentStack.push(this.nextRecord);
                    }
                    this.getNextLine();
                }
            }
        } else {
            throw new DataFileException("Separator style " + modelDataFile.separatorStyle + " not recognized.");
        }
        return curRecord;
    }

    public void close() throws DataFileException {
        if (this.closed) {
            return;
        }
        try {
            this.br.close();
            this.closed = true;
        } catch (IOException e) {
            throw new DataFileException("Error closing data file input stream", e);
        }
    }

    /** Searches through the record models to find one with a matching type-code, if no type-code exists that model will always be used if it gets to it
     * @param line
     * @param lineNum
     * @param modelDataFile
     * @throws DataFileException Exception thown for various errors, generally has a nested exception
     * @return
     */
    protected static ModelRecord findModelForLine(String line, int lineNum, ModelDataFile modelDataFile) throws DataFileException {
        ModelRecord modelRecord = null;
        for (int i = 0; i < modelDataFile.records.size(); i++) {
            ModelRecord curModelRecord = (ModelRecord) modelDataFile.records.get(i);
            if (curModelRecord.tcPosition < 0) {
                modelRecord = curModelRecord;
                break;
            }
            String typeCode = line.substring(curModelRecord.tcPosition, curModelRecord.tcPosition + curModelRecord.tcLength);
            if (curModelRecord.typeCode.length() > 0) {
                if (typeCode != null && typeCode.equals(curModelRecord.typeCode)) {
                    modelRecord = curModelRecord;
                    break;
                }
            } else if (curModelRecord.tcMin.length() > 0 || curModelRecord.tcMax.length() > 0) {
                if (curModelRecord.tcIsNum) {
                    long typeCodeNum = Long.parseLong(typeCode);
                    if ((curModelRecord.tcMinNum < 0 || typeCodeNum >= curModelRecord.tcMinNum) && (curModelRecord.tcMaxNum < 0 || typeCodeNum <= curModelRecord.tcMaxNum)) {
                        modelRecord = curModelRecord;
                        break;
                    }
                } else {
                    if ((typeCode.compareTo(curModelRecord.tcMin) >= 0) && (typeCode.compareTo(curModelRecord.tcMax) <= 0)) {
                        modelRecord = curModelRecord;
                        break;
                    }
                }
            }
        }
        if (modelRecord == null) {
            throw new DataFileException("Could not find record definition for line " + lineNum + "; first bytes: " + line.substring(0, (line.length() > 5) ? 5 : line.length()));
        }
        return modelRecord;
    }
}
