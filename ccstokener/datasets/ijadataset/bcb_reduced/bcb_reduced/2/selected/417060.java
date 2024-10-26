package net.sf.hdkp.data.io;

import static org.junit.Assert.*;
import java.io.*;
import java.net.URL;
import org.junit.Test;
import net.sf.hdkp.data.*;

public class LuaReaderTest {

    public static void main(String... args) throws Exception {
        readTestData("http://www.hydra-guild.co.uk/dkp/getdkp.php");
    }

    public static void readTestData(String getDkpUrl) throws Exception {
        final URL url = new URL(getDkpUrl);
        final InputStream is = url.openStream();
        try {
            final LineNumberReader rd = new LineNumberReader(new BufferedReader(new InputStreamReader(is)));
            String line = rd.readLine();
            while (line != null) {
                System.out.println(line);
                line = rd.readLine();
            }
        } finally {
            is.close();
        }
    }

    private LineNumberReader createTestReader() throws IOException {
        final InputStream is = getClass().getResourceAsStream("dkp_list.lua");
        final InputStreamReader rd = new InputStreamReader(is, "UTF-8");
        return new LineNumberReader(rd);
    }

    @Test
    public void testReadData() throws IOException {
        final LuaReader rd = new LuaReader(createTestReader());
        final Data data = rd.readData();
        assertEquals(7, data.getMaxRaids());
        assertEquals(2577, data.getMaxDkp());
        final String[] expectedNames = new String[] { "Agadash", "Anathea", "Angeliza", "Annisa", "Belárion", "Beriadwen", "Dasiy", "Davore", "Eddcom", "Engra", "Epicuros", "Gadgette", "Gorrka", "Hrador", "Ikaros", "Iladri", "Kaanack", "Kaltina", "Kazlas", "Kelidrath", "Kirino", "Logrash", "Maldorai", "Marianná", "Mjinkan", "Molaidon", "Morbis", "Muchin", "Racoonu", "Runtskab", "Saila", "Salblaidd", "Shamit", "Shunran", "Snapvine", "Sylmarien", "Szayel", "Taevyn", "Talesin", "Thumok", "Uktena", "Valerica", "Yalldae", "Zosso" };
        final Toon[] toons = data.getToons();
        final String[] actualNames = new String[toons.length];
        int i = 0;
        for (Toon toon : toons) {
            actualNames[i++] = toon.getName();
        }
        assertArrayEquals(expectedNames, actualNames);
    }
}
