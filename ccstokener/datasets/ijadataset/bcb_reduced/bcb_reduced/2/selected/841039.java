package uk.me.jstott.jweatherstation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.TooManyListenersException;
import javax.comm.CommPortIdentifier;
import javax.comm.CommPortOwnershipListener;
import javax.comm.NoSuchPortException;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.SerialPortEvent;
import javax.comm.SerialPortEventListener;
import javax.comm.UnsupportedCommOperationException;
import org.apache.log4j.Logger;
import uk.me.jstott.jweatherstation.sql.SQL;
import uk.me.jstott.jweatherstation.util.CRC;
import uk.me.jstott.jweatherstation.util.Process;
import uk.me.jstott.jweatherstation.util.UnsignedByte;

/**
 * 
 * 
 * @author Jonathan Stott
 * @version 1.1
 * @since 1.0
 */
public class Station {

    private static final int RECORD_SIZE = 52;

    private static final Logger LOGGER = Logger.getLogger(Station.class);

    private static final int BUFFER_SIZE = 266;

    private UnsignedByte[] data = new UnsignedByte[BUFFER_SIZE];

    private static final int LOOP_SIZE = 99;

    private ArrayList<DmpRecord> dmpRecords = new ArrayList<DmpRecord>();

    private static final int ACK = 6;

    private static final byte LF = '\n';

    private static final byte CR = '\r';

    private InputStream inputStream = null;

    private Calendar lastDate = Calendar.getInstance();

    private OutputStream outputStream = null;

    private SerialPort port = null;

    private CommPortIdentifier portID = null;

    private String portName;

    private CRC crc = new CRC();

    public Station(String portName) throws PortInUseException, NoSuchPortException {
        this.portName = portName;
        portID = getPortID(portName);
        openPort();
    }

    private CommPortIdentifier getPortID(String portName) throws NoSuchPortException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("main: Listing ports: ");
            Enumeration pList = CommPortIdentifier.getPortIdentifiers();
            while (pList.hasMoreElements()) {
                CommPortIdentifier cpi = (CommPortIdentifier) pList.nextElement();
                LOGGER.debug(cpi.getName());
            }
            LOGGER.debug("Done listing ports");
        }
        return CommPortIdentifier.getPortIdentifier(portName);
    }

    /**
     * Open and configure the serial port ready for use.
     * 
     * @return true if the serial port was successfully opened - false
     *         otherwise.
     * @throws PortInUseException
     *             if the serial port is already in use and we are unable to get
     *             a lock on it.
     * @since 1.0
     */
    private boolean openPort() throws PortInUseException {
        try {
            port = (SerialPort) portID.open("Davis Station", 2000);
            if (port == null) {
                LOGGER.error("Error opening port " + portID.getName());
                return false;
            }
            LOGGER.info("Opening port: " + port);
            try {
                inputStream = port.getInputStream();
            } catch (IOException e) {
                LOGGER.error("Cannot open input stream", e);
            }
            try {
                outputStream = port.getOutputStream();
            } catch (IOException e) {
                LOGGER.error("Cannot open output stream");
            }
            try {
                port.setSerialPortParams(19200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            } catch (UnsupportedCommOperationException e) {
                LOGGER.error(e);
            }
        } catch (PortInUseException e) {
            LOGGER.info("Queueing open for " + portID.getName() + ": port in use by " + e.currentOwner);
        }
        return true;
    }

    /**
     * Close the serial port.
     * 
     * @since 1.0
     */
    public void shutdown() {
        port.close();
        port = null;
    }

    /**
     * 
     * 
     * @param pageOffset 
     * @since 1.0
     */
    private void processDmpAftPacket(byte[] page, int pageOffset) {
        for (int i = pageOffset; i < 5; i++) {
            byte[] rawData = new byte[RECORD_SIZE];
            int byteOffset = RECORD_SIZE * i;
            for (int k = 0, j = byteOffset; j < byteOffset + RECORD_SIZE; j++, k++) {
                rawData[k] = page[j];
            }
            DmpRecord dmpRecord = new DmpRecord(rawData);
            if (dmpRecord.getDate().after(getLastDate().getTime())) {
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("DMP Record: " + dmpRecord.getDate());
            }
            if (dmpRecord.getDate().after(getLastDate().getTime())) {
                dmpRecords.add(dmpRecord);
                setLastDate(getLastDate());
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(dmpRecord.getDate() + " older than " + getLastDate().getTime());
                }
            }
        }
    }

    /**
     * 
     * 
     * @since 1.0
     */
    private void clearDmpRecords() {
        dmpRecords = new ArrayList<DmpRecord>();
    }

    /**
     * 
     * 
     * @since 1.0
     */
    private void printDmpRecords() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("st: Printing " + dmpRecords.size() + " DmpAft packets");
            for (int i = 0; i < dmpRecords.size(); i++) {
                DmpRecord record = (DmpRecord) dmpRecords.get(i);
                if (record.isValid()) {
                    LOGGER.debug("st: Record " + i + ": " + record.toString());
                }
            }
            LOGGER.debug("st: Done printing " + dmpRecords.size() + " DmpAft packets");
        }
    }

    /**
     * 
     * 
     * @since 1.0
     */
    private void sqlDmpRecords() {
        LOGGER.info("st: SQL updating " + dmpRecords.size() + " DmpAft packets");
        SQL sql = new SQL(Main.DB_DATABASE, Main.DB_TABLE, Main.DB_USERNAME, Main.DB_PASSWORD);
        sql.connect();
        for (int i = 0; i < dmpRecords.size(); i++) {
            DmpRecord record = (DmpRecord) dmpRecords.get(i);
            if (record.isValid()) {
                sql.update(record.toSQLUpdate());
            }
        }
        sql.disconnect();
        LOGGER.info("st: Done sql updating " + dmpRecords.size() + " DmpAft packets");
    }

    /**
     * 
     * 
     * @since 1.1
     */
    private void writeDmpRecords() {
        LOGGER.info("st: writing " + dmpRecords.size() + " DmpAft packets to " + Main.DATA_FILE);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(Main.DATA_FILE), true));
            for (int i = 0; i < dmpRecords.size(); i++) {
                DmpRecord record = (DmpRecord) dmpRecords.get(i);
                if (record.isValid()) {
                    bw.write(record.toDataFile());
                    bw.newLine();
                }
            }
            bw.close();
        } catch (Exception e) {
            LOGGER.error(e);
        }
        LOGGER.info("st: Done writing " + dmpRecords.size() + " DmpAft packets to " + Main.DATA_FILE);
    }

    /**
     * 
     * 
     * @since 1.1
     */
    private void writeSQLLogDmpRecords() {
        LOGGER.info("st: writing SQL log for " + dmpRecords.size() + " DmpAft packets to " + Main.SQL_LOG);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(Main.SQL_LOG), true));
            for (int i = 0; i < dmpRecords.size(); i++) {
                DmpRecord record = (DmpRecord) dmpRecords.get(i);
                if (record.isValid()) {
                    bw.write(record.toSQLUpdate());
                    bw.newLine();
                }
            }
            bw.close();
        } catch (Exception e) {
            LOGGER.error(e);
        }
        LOGGER.info("st: Done writing SQL log for " + dmpRecords.size() + " DmpAft packets to " + Main.SQL_LOG);
    }

    /**
     * 
     * 
     * @since 1.1
     */
    private void postDmpRecords() {
        LOGGER.info("st: POST updating " + dmpRecords.size() + " DmpAft packets");
        for (int i = 0; i < dmpRecords.size(); i++) {
            DmpRecord record = (DmpRecord) dmpRecords.get(i);
            if (record.isValid()) {
                try {
                    URL url = new URL("http://www.canterburyweather.co.uk/post.php");
                    URLConnection urlConn = url.openConnection();
                    urlConn.setDoOutput(true);
                    OutputStreamWriter out = new OutputStreamWriter(urlConn.getOutputStream());
                    String content = record.toPostRequest();
                    System.out.println(content);
                    out.write(content);
                    out.flush();
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(line);
                        }
                    }
                    in.close();
                    out.close();
                } catch (Exception e) {
                    LOGGER.error(e);
                }
            }
        }
        LOGGER.info("st: Done POST updating " + dmpRecords.size() + " DmpAft packets");
    }

    /**
     * 
     * 
     * @since 1.0
     */
    private void uploadDmpRecords() {
        LOGGER.info("st: Uploading " + dmpRecords.size() + " DmpAft packets");
        try {
            URL url = new URL(Main.UPLOAD_URL);
            for (int i = 0; i < dmpRecords.size(); i++) {
                DmpRecord record = (DmpRecord) dmpRecords.get(i);
                if (record.isValid()) {
                    LOGGER.info("st: Uploading record " + i + " to " + url.toString());
                    URLConnection conn = url.openConnection();
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                    wr.writeBytes(record.toPostRequest());
                    wr.flush();
                    wr.close();
                }
            }
        } catch (MalformedURLException mue) {
            LOGGER.error(mue);
        } catch (IOException ioe) {
            LOGGER.error(ioe);
        }
        LOGGER.info("st: Done uploading " + dmpRecords.size() + " DmpAft packets");
    }

    /**
     * 
     * 
     * @return
     * @since 1.0
     */
    private Timestamp sqlLastDmpRecordTimestamp() {
        LOGGER.info("st: SQL getting date of last DMP record - " + Main.DB_TABLE);
        SQL sql = new SQL(Main.DB_DATABASE, Main.DB_TABLE, Main.DB_USERNAME, Main.DB_PASSWORD);
        sql.connect();
        ResultSet rs = sql.query("SELECT * FROM dmprecords ORDER BY date DESC LIMIT 1");
        Timestamp date = null;
        try {
            if (!rs.first()) {
                date = new Timestamp(new Date().getTime());
            } else {
                date = rs.getTimestamp("date");
            }
        } catch (SQLException sqle) {
            LOGGER.error(sqle);
        } catch (NullPointerException npe) {
            LOGGER.info("st: SQL failed to connect");
            return null;
        }
        sql.disconnect();
        LOGGER.info("st: SQL got date of last DMP record - " + date.toString());
        return date;
    }

    /**
     * 
     * 
     * @return
     * @since 1.1
     */
    private Timestamp postLastDmpRecordTimestamp() {
        LOGGER.info("st: POST getting date of last DMP record");
        Timestamp date = null;
        LOGGER.info("st: POST got date of last DMP record - " + date.toString());
        return date;
    }

    /**
     * 
     * 
     * @return
     * @since 1.0
     */
    private UnsignedByte[] getData() {
        return data;
    }

    /**
     * 
     * 
     * @return
     * @since 1.0
     */
    private InputStream getInputStream() {
        return inputStream;
    }

    /**
     * 
     * 
     * @return
     * @since 1.0
     */
    private OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * 
     * 
     * @return
     * @since 1.0
     */
    private SerialPort getPort() {
        return port;
    }

    private void flushOutputBuffer() throws IOException {
        this.getOutputStream().flush();
    }

    private void clearInputBuffer() throws IOException {
        int bytesAvailable = this.getInputStream().available();
        for (int i = 0; i < bytesAvailable; i++) {
            this.getInputStream().read();
        }
        if (LOGGER.isDebugEnabled()) {
            if (bytesAvailable > 0) {
                LOGGER.debug("cleared: " + bytesAvailable + " bytes from the input stream");
            }
        }
    }

    private void sendString(String str) throws IOException {
        wakeup();
        sendBytes(str.getBytes());
    }

    private void sendByte(byte b) throws IOException {
        sendBytes(new byte[] { b });
    }

    private void delay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    private void wakeup() throws IOException {
        clearInputBuffer();
        for (int i = 1; i <= 3; i++) {
            sendByte((byte) 0x0d);
            delay(1500);
            int bytes = getInputStream().available();
            if (bytes == 2) {
                byte[] crlf = new byte[2];
                int bytesRead = getInputStream().read(crlf);
                if (bytesRead == 2 && crlf[0] == LF && crlf[1] == CR) {
                    return;
                }
            } else {
            }
        }
        LOGGER.error("Station failed to wake up");
    }

    private boolean getAck() throws IOException {
        delay(500);
        int ack = getInputStream().read();
        if (ack == ACK) {
            if (LOGGER.isDebugEnabled()) {
            }
            return true;
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("missed ack: " + ack);
            }
            return false;
        }
    }

    public boolean test() throws IOException {
        sendString("TEST\n");
        boolean ok = false;
        byte tmp[] = new byte[10];
        int bytesRead = this.getInputStream().read(tmp);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytesRead; i++) {
            if (tmp[i] != 10 && tmp[i] != 13) {
                sb.append((char) tmp[i]);
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("station responded: " + sb.toString());
        }
        int index = sb.indexOf("TEST");
        if (index != -1) {
            ok = true;
        }
        return ok;
    }

    protected void sendLoopCommand(int i) throws IOException {
        sendString("LOOP " + i + "\n");
        getAck();
    }

    protected LoopPacket readLoopData() {
        LoopPacket packet = null;
        int bytes = 0;
        byte[] localBuffer = new byte[LOOP_SIZE];
        try {
            sendLoopCommand(1);
            bytes = getInputStream().available();
            if (bytes == LOOP_SIZE) {
                bytes = getInputStream().read(localBuffer);
                packet = new LoopPacket(localBuffer);
            } else {
                LOGGER.warn("unexpected buffer size of: " + bytes);
            }
        } catch (IOException ex) {
            LOGGER.info(getPort().getName() + ": Cannot read input stream");
        }
        return packet;
    }

    /**
     * 
     * 
     * @return
     * @throws IOException
     * @since 1.0
     */
    public boolean dmpaft() throws IOException {
        UnsignedByte[] datetime = null;
        if (Main.DO_SQL) {
            Timestamp dbLastDate = null;
            dbLastDate = this.sqlLastDmpRecordTimestamp();
            getLastDate().setTime(dbLastDate);
            if (dbLastDate == null) {
                LOGGER.info("Aborting dmpaft");
                return false;
            }
            datetime = Process.dmpTimeStamp(dbLastDate.getDate(), dbLastDate.getMonth() + 1, dbLastDate.getYear() + 1900, dbLastDate.getHours(), dbLastDate.getMinutes());
        } else {
            if (getLastDate() == null) {
                datetime = Process.dmpTimeStamp(29, 8, 2005, 1, 20);
            } else {
                datetime = Process.dmpTimeStamp(getLastDate().get(Calendar.DAY_OF_MONTH), getLastDate().get(Calendar.MONTH) + 1, getLastDate().get(Calendar.YEAR), getLastDate().get(Calendar.HOUR_OF_DAY), getLastDate().get(Calendar.MINUTE));
            }
        }
        sendString("DMPAFT\n");
        getAck();
        crc.reset();
        LOGGER.info("Sending date/time " + Process.printUnsignedByteArray(datetime));
        sendUnsignedBytes(datetime);
        UnsignedByte[] check = crc.getUnsignedBytes();
        LOGGER.info("sending CRC " + Process.printUnsignedByteArray(check));
        sendUnsignedBytes(check);
        if (!getAck()) {
            LOGGER.error("Aborting dmpaft");
            return false;
        }
        int bytes = getInputStream().available();
        byte[] header = new byte[bytes];
        int bytesRead = getInputStream().read(header);
        UnsignedByte[] data = UnsignedByte.getUnsignedBytes(header);
        LOGGER.info("tx: Data: " + Process.printUnsignedByteArray(data));
        int pages = (data[1].getByte() << 8) | data[0].getByte();
        int startRecord = (data[3].getByte() << 8) | data[2].getByte();
        LOGGER.info("tx: Expecting " + pages + " pages; first record: " + startRecord);
        for (int i = 0; i < pages; i++) {
            sendBytes(new byte[] { (byte) 0x06 });
            int sequenceNumber = getInputStream().read();
            LOGGER.info("processing page sequence number: " + sequenceNumber);
            if (i == 0) {
                readDmpData(startRecord);
            } else {
                readDmpData();
            }
        }
        if (Main.DO_SQL) {
            sqlDmpRecords();
        }
        if (Main.DO_DATA_FILE) {
            writeDmpRecords();
        }
        if (Main.DO_SQL_LOG) {
            writeSQLLogDmpRecords();
        }
        if (Main.DO_POST) {
            postDmpRecords();
        }
        clearDmpRecords();
        return true;
    }

    private void readDmpData() {
        readDmpData(0);
    }

    /**
     * 
     * 
     * @since 1.0
     */
    private void readDmpData(int offset) {
        byte[] localBuffer = new byte[BUFFER_SIZE];
        try {
            int available = getInputStream().available();
            if (available > 0) {
                int bytes = getInputStream().read(localBuffer);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("retrieved: " + bytes + " bytes");
                }
                processDmpAftPacket(localBuffer, offset);
            }
            delay(500);
        } catch (IOException ex) {
            LOGGER.error(getPort().getName() + ": Cannot read input stream", ex);
        }
    }

    /**
     * 
     * 
     * @param bytes
     * @throws IOException
     * @since 1.0
     */
    private void sendUnsignedBytes(UnsignedByte[] bytes) throws IOException {
        byte[] bs = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            bs[i] = (byte) (bytes[i].getByte() & 0xFF);
            if (LOGGER.isDebugEnabled()) {
            }
        }
        sendBytes(bs);
    }

    private void sendBytes(byte[] bytes) {
        int count;
        count = bytes.length;
        if (count > 0) {
            try {
                for (int i = 0; i < count; i++) {
                    if (LOGGER.isDebugEnabled()) {
                    }
                    getOutputStream().write(bytes[i] & 0xFF);
                    crc.updateCRC(bytes[i]);
                }
            } catch (IOException ex) {
                LOGGER.error(ex);
            }
        }
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public String getPortName() {
        return portName;
    }

    private void setLastDate(Calendar lastDate) {
        this.lastDate = lastDate;
    }

    private Calendar getLastDate() {
        return lastDate;
    }
}
