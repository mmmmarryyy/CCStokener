package eu.jacquet80.rds.input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import eu.jacquet80.rds.input.group.FrequencyChangeEvent;
import eu.jacquet80.rds.input.group.GroupEvent;
import eu.jacquet80.rds.input.group.GroupReaderEvent;
import eu.jacquet80.rds.log.RealTime;
import eu.jacquet80.rds.log.SequentialTime;
import eu.jacquet80.rds.log.RDSTime;

public class HexFileGroupReader extends GroupReader {

    private static final Pattern FIRST_NUMBER = Pattern.compile(".*\\D(\\d+)");

    private static final Pattern RDS_SPY_DATE_FORMAT = Pattern.compile(".*@(\\d{4})/(\\d{2})/(\\d{2})\\s+(\\d{2}):(\\d{2}):(\\d{2}).(\\d{2,4})$");

    private final BufferedReader br;

    private int groupTime = 0;

    private static final Pattern SPACE = Pattern.compile("\\s+");

    public HexFileGroupReader(BufferedReader br) {
        this.br = br;
    }

    public HexFileGroupReader(URL url) throws IOException {
        this(new BufferedReader(new InputStreamReader(url.openStream())));
    }

    public HexFileGroupReader(File file) throws FileNotFoundException {
        this(new BufferedReader(new FileReader(file)));
    }

    public GroupReaderEvent getGroup() throws IOException, EndOfStream {
        GroupReaderEvent event;
        do {
            String line = br.readLine();
            if (line == null) throw new EndOfStream();
            event = parseHexLine(line, new SequentialTime(groupTime));
            groupTime++;
        } while (event == null);
        return event;
    }

    static GroupReaderEvent parseHexLine(String line, RDSTime time) throws IOException {
        line = line.trim();
        if (line.length() == 0) return null;
        if (line.startsWith("%")) {
            if (line.startsWith("% Freq")) {
                Matcher m = FIRST_NUMBER.matcher(line);
                int f = 0;
                if (m.matches()) f = Integer.parseInt(m.group(1));
                return new FrequencyChangeEvent(time, f);
            }
            return null;
        }
        if (line.startsWith("<")) return null;
        String[] components = SPACE.split(line);
        if (components.length < 4) return null;
        int[] res = new int[4];
        for (int i = 0; i < 4; i++) {
            String s = components[i];
            if ("----".equals(s)) res[i] = -1; else res[i] = Integer.parseInt(s, 16);
        }
        Matcher m = RDS_SPY_DATE_FORMAT.matcher(line);
        if (m.matches()) {
            GregorianCalendar c = new GregorianCalendar(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) - 1, Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6)));
            c.set(Calendar.MILLISECOND, (int) (Float.parseFloat("0." + m.group(7)) * 1000));
            time = new RealTime(c.getTime());
        }
        return new GroupEvent(time, res, false);
    }
}
