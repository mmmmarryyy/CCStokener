package com.dreamfabric.jac64;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.DataInputStream;
import java.net.URL;
import java.util.*;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 *
 *
 * @author  Joakim Eriksson (joakime@sics.se)
 * @version $Revision: 1.5 $, $Date: 2006/05/01 14:57:57 $
 */
public class C64Reader {

    public static final int NONE = 0;

    public static final int TAPE = 1;

    public static final int DISK = 2;

    private int[] memory;

    private String label = "";

    private ArrayList dirNames = new ArrayList();

    private Hashtable dirEntries = new Hashtable();

    private DiskListener listener;

    private int type = 0;

    private int diskSize = 0;

    private byte sectors[][] = new byte[800][256];

    private int noBytes;

    public void setCPU(CPU cpu) {
        this.memory = cpu.getMemory();
    }

    public void setDiskListener(DiskListener l) {
        listener = l;
    }

    public int getLoadedType() {
        return type;
    }

    public ArrayList getDirNames() {
        return dirNames;
    }

    public DirEntry getDirEntry(String name) {
        return (DirEntry) dirEntries.get(name);
    }

    public boolean readDisk(InputStream stream) {
        int sector = 0;
        int sectrak = 0;
        int trak = 1;
        int numRead = 0;
        DataInputStream reader = new DataInputStream(stream);
        dirNames.clear();
        dirEntries.clear();
        type = DISK;
        try {
            while ((numRead = reader.read(sectors[sector])) > 0) {
                if (numRead < 256) {
                    reader.readFully(sectors[sector], numRead, 256 - numRead);
                }
                if (trak == 18) {
                    readDir(sectors[sector], trak, sectrak);
                }
                sector++;
                sectrak++;
                if ((trak < 18 && sectrak == 21) || (trak >= 18 && trak < 25 && sectrak == 19) || (trak >= 25 && trak < 31 && sectrak == 18) || (trak >= 31 && trak < 41 && sectrak == 17)) {
                    sectrak = 0;
                    trak++;
                }
            }
            System.out.println("Read " + sector + " sectors");
            diskSize = sector;
        } catch (Exception e) {
            System.out.println("Error reading sectors");
            System.out.println("Track: " + trak + " sec: " + sector);
            e.printStackTrace();
            return false;
        } finally {
            try {
                reader.close();
            } catch (Exception x) {
            }
        }
        if (listener != null) {
            listener.diskChanged();
        }
        return true;
    }

    public byte[] getSector(int sector) {
        return sectors[sector];
    }

    public static int getSectorCount(int track) {
        if (track > 30) return 17;
        if (track > 24) return 18;
        if (track > 17) return 19;
        return 21;
    }

    public byte[] getSector(int trak, int sectrak) {
        int sector = getSecTrack(trak);
        sector = sector + sectrak;
        return sectors[sector];
    }

    private int getSecTrack(int trak) {
        int sector = 0;
        if (trak < 18) sector = (trak - 1) * 21; else if ((trak >= 18) && (trak < 25)) sector = (trak - 1) * 19 + 17 * 2; else if ((trak >= 25) && (trak < 31)) sector = (trak - 1) * 18 + 17 * 3 + 7; else sector = (trak - 1) * 17 + 17 * 4 + 7 * 2 + 6;
        return sector;
    }

    public void writeDisk(OutputStream stream) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(stream);
        for (int i = 0, n = diskSize; i < n; i++) {
            out.write(sectors[i], 0, 256);
        }
        out.close();
    }

    public void setSector(int track, int sectrack, byte[] newSector) {
        int sector = getSecTrack(track);
        sector = sector + sectrack;
        for (int i = 0, n = 255; i < n; i++) {
            sectors[sector][i] = newSector[i];
        }
    }

    private boolean lastEntry;

    private int nextSector;

    private void readDir(byte[] data, int trak, int sec) {
        if (sec == 0) {
            label = "";
            for (int i = 0; i < 16; i++) if (data[0x90 + i] != (byte) 0xa0) {
                label += (char) data[0x90 + i];
            }
            System.out.println("Directory listing of '" + label + "'");
            lastEntry = false;
            nextSector = trak;
            nextSector = 1;
        } else {
            if (!lastEntry) {
                if ((nextSector == sec) && data[0] == 0) {
                    lastEntry = true;
                } else {
                    nextSector = data[1];
                }
                int start = 0;
                while ((start < 0xff) && (data[start + 2] != 0)) {
                    int tp = data[start + 2] & 0xff;
                    String name = "";
                    int size;
                    for (int i = 0; i < 16; i++) if (data[start + 5 + i] != (byte) 0xa0) name = name + (char) data[start + 5 + i]; else name = name + " ";
                    size = data[start + 0x1e] & 0xff + data[start + 0x1f] * 256;
                    DirEntry entry = new DirEntry(name, data[start + 3], data[start + 4], size, tp);
                    dirNames.add(entry);
                    dirEntries.put(name, entry);
                    start = start + 0x20;
                }
                if (lastEntry) System.out.println("No more files.");
            }
        }
    }

    public String readFile(String str) {
        return readFile(str, -1);
    }

    public String readFile(String str, int adr) {
        return readFile(str, adr, null);
    }

    private void printDirListing(OutputStream out) {
        byte[] dir = new byte[4096];
        int p = 0;
        int adr = 0x801;
        dir[p++] = (byte) (adr & 0xff);
        dir[p++] = (byte) (adr >> 8);
        adr += label.length() + 5;
        dir[p++] = (byte) (adr & 0xff);
        dir[p++] = (byte) (adr >> 8);
        dir[p++] = 0;
        dir[p++] = 0;
        dir[p++] = 0x12;
        dir[p++] = '"';
        for (int i = 0, n = label.length(); i < n; i++) {
            dir[p++] = (byte) label.charAt(i);
        }
        dir[p++] = '"';
        dir[p++] = 0;
        for (int i = 0, n = dirNames.size(); i < n; i++) {
            DirEntry dire = (DirEntry) dirNames.get(i);
            adr += 26;
            int fill = 1;
            if (dire.size < 10) fill = 3; else if (dire.size < 100) fill = 2;
            adr += fill;
            dir[p++] = (byte) (adr & 0xff);
            dir[p++] = (byte) (adr >> 8);
            dir[p++] = (byte) (dire.size & 0xff);
            dir[p++] = (byte) (dire.size >> 8);
            for (int j = 0, m = fill; j < m; j++) {
                dir[p++] = ' ';
            }
            for (int j = 0, m = dire.name.length(); j < m; j++) {
                dir[p++] = (byte) dire.name.charAt(j);
            }
            for (int j = 0, m = 18 - dire.name.length(); j < m; j++) {
                dir[p++] = ' ';
            }
            String type = dire.getTypeString();
            for (int j = 0, m = type.length(); j < m; j++) {
                dir[p++] = (byte) type.charAt(j);
            }
            dir[p++] = 0;
        }
        dir[p++] = 0;
        dir[p++] = 0;
        try {
            out.write(dir, 0, p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String readFile(String filename, int adr, OutputStream out) {
        int sindex = 0;
        DirEntry dire = null;
        System.out.println("Loading: '" + filename + "' at " + adr);
        if ((sindex = filename.indexOf('*')) >= 0) {
            String match = filename.substring(0, sindex);
            System.out.println("Matcher: " + match);
            Enumeration names = dirEntries.keys();
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                if (name.startsWith(match)) {
                    System.out.println("Found: " + name);
                    dire = (DirEntry) dirEntries.get(name);
                }
            }
        } else if (filename.equals("$")) {
            printDirListing(out);
            return filename;
        } else {
            dire = (DirEntry) dirEntries.get(filename);
            if (dire == null) {
                for (int i = filename.length(), n = 16; i < n; i++) {
                    filename += ' ';
                }
                dire = (DirEntry) dirEntries.get(filename);
            }
        }
        if (dire == null) return null;
        if (type == TAPE) {
            return readTapeFile(dire);
        }
        return readDiskFile(dire, filename, adr, out);
    }

    public String readDiskFile(DirEntry dire, String str, int adr, OutputStream out) {
        byte[] sec = getSector(dire.trk, dire.sec);
        int address = (sec[2] & 0xff) + (sec[3] & 0xff) * 256;
        System.out.println("*** Reading DISK file at " + Integer.toString(address, 16));
        if (out != null) {
            try {
                out.write(sec[2] & 0xff);
                out.write(sec[3] & 0xff);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (adr != -1) {
            System.out.println("Address override: " + address + " -> " + adr);
            address = adr;
        }
        int nextSector = sec[1] & 0xff;
        int nextTrak = sec[0] & 0xff;
        try {
            for (int i = 0; i < 252; i++) {
                if (out != null) out.write(sec[i + 4] & 0xff); else memory[i + address] = sec[i + 4] & 0xff;
            }
        } catch (Exception e) {
            System.out.println("Could not write to output stream");
            e.printStackTrace();
        }
        address = address + 252;
        boolean reading = nextTrak != 0;
        while (reading) {
            sec = getSector(nextTrak, nextSector);
            nextSector = sec[1] & 0xff;
            nextTrak = sec[0] & 0xff;
            reading = nextTrak != 0;
            try {
                for (int i = 0; i < 254; i++) {
                    if (out != null) out.write(sec[i + 2] & 0xff); else memory[i + address] = sec[i + 2] & 0xff;
                }
            } catch (Exception e) {
                System.out.println("Could not write to output stream");
                e.printStackTrace();
            }
            address = address + 254;
        }
        address = address - 255 + nextSector;
        if (out == null) setAddress(address);
        System.out.println("*** File loaded - end at: " + Integer.toString(address, 16));
        return dire.name;
    }

    public String saveFile() {
        int startAdr = memory[43] + (memory[44] << 8);
        int lastAdr = memory[45] + (memory[46] << 8);
        System.out.println("Dumping mem from: " + startAdr + " to " + lastAdr);
        StringBuffer sb = new StringBuffer();
        if ((startAdr & 0xff) < 16) sb.append('0');
        sb.append(Integer.toString(startAdr & 0xff));
        if ((startAdr >> 8) < 16) sb.append('0');
        sb.append(Integer.toString(startAdr >> 8));
        for (int i = startAdr; i < lastAdr; i++) {
            int m = memory[i];
            if (m < 16) sb.append('0');
            sb.append(Integer.toString(memory[i], 16));
        }
        return sb.toString();
    }

    private void setAddress(int address) {
        if (address > 0x9f00) address = 0x9f00;
        memory[45] = address & 0xff;
        memory[46] = (address & 0xff00) >> 8;
        memory[47] = address & 0xff;
        memory[48] = (address & 0xff00) >> 8;
        memory[49] = address & 0xff;
        memory[50] = (address & 0xff00) >> 8;
    }

    private String readTapeFile(DirEntry dire) {
        int address = dire.sec;
        int offset = dire.trk;
        System.out.println("Reading from " + offset);
        System.out.println("Storing at: " + address);
        System.out.println("Size: " + dire.size);
        for (int i = 0; i < dire.size; i++) {
            memory[address++] = sectors[(i + offset) >> 8][(i + offset) & 0xff] & 0xff;
        }
        setAddress(address);
        return dire.name;
    }

    private boolean readPGM(InputStream stream, int address) {
        byte[] start = new byte[2];
        try {
            DataInputStream reader = new DataInputStream(stream);
            reader.readFully(start);
            int sector = 0;
            int numRead = 0;
            int noBytes = 0;
            while ((numRead = readSector(reader, sector)) == 256) sector++;
            noBytes = sector * 256 + numRead;
            System.out.println("Read " + noBytes + " program data");
            if (address == -1) address = (start[0] + (start[1] * 256));
            System.out.println("Storing at: " + address);
            for (int i = 0; i < noBytes; i++) memory[address++] = sectors[(i) >> 8][i & 0xff] & 0xff;
            setAddress(address);
            return true;
        } catch (Exception e) {
            System.out.println("Error while reading pgm file " + e);
        }
        return false;
    }

    private boolean readTape(InputStream stream) {
        byte[] start = new byte[32];
        dirNames.clear();
        dirEntries.clear();
        type = TAPE;
        try {
            DataInputStream reader = new DataInputStream(stream);
            reader.readFully(start);
            String st = new String(start);
            if (st.startsWith("C64")) {
                System.out.println("Tape Archive found:");
                reader.readFully(start);
                int max = start[3] * 256 + start[2];
                int used = start[5] * 256 + start[4];
                if (used == 0) used = 1;
                String name = "";
                System.out.println("Type: " + start[0] * 256 + start[1]);
                System.out.println("Max Entries: " + max);
                System.out.println("Used Entries: " + used);
                for (int i = 8; i < 32; i++) name = name + (char) start[i];
                System.out.println("Name: " + name);
                int startAdr, endAdr, offset;
                for (int i = 0; i < used; i++) {
                    reader.readFully(start);
                    startAdr = (start[2] & 0xff) + (start[3] & 0xff) * 256;
                    endAdr = (start[4] & 0xff) + (start[5] & 0xff) * 256;
                    offset = (start[8] & 0xff) + (start[9] & 0xff) * 256;
                    System.out.println("---------------------");
                    System.out.println("Entry: " + i);
                    System.out.println("File Type: " + start[0]);
                    System.out.println("1541 Type: " + start[1]);
                    System.out.println("Start Adr: " + startAdr);
                    System.out.println("End Adr: " + endAdr + " -> size = " + (endAdr - startAdr));
                    System.out.println("Offset: " + offset);
                    name = "";
                    for (int j = 16; j < 32; j++) name = name + (char) start[j];
                    System.out.println("File Name: " + name);
                    DirEntry entry = new DirEntry(name, offset - 32 * (max + 2), startAdr, endAdr - startAdr, start[1]);
                    dirNames.add(entry);
                    dirEntries.put(name, entry);
                }
                for (int i = used; i < max; i++) {
                    reader.readFully(start);
                }
                int sector = 0;
                int numRead;
                while ((numRead = readSector(reader, sector)) == 256) sector++;
                noBytes = sector * 256 + numRead;
                System.out.println("Read " + noBytes + " program data");
            }
            return true;
        } catch (Exception e) {
            System.out.println("Error while reading tape");
        }
        return false;
    }

    private boolean readSID(InputStream stream) {
        byte[] start = new byte[0x16];
        try {
            DataInputStream reader = new DataInputStream(stream);
            reader.readFully(start);
            String st = new String(start);
            if (st.startsWith("PSID")) {
                int version = start[4] * 256 + start[5];
                int addr = start[8] * 256 + start[9];
                int iaddr = start[10] * 256 + start[11];
                int paddr = start[12] * 256 + start[13];
                int songs = start[14] * 256 + start[15];
                int startsong = start[16] * 256 + start[17];
                long speed = (start[18] << 24) + (start[19] << 16) + (start[20] << 8) + start[21];
                System.out.println("FOUND SID TUNE!");
                System.out.println("Version: " + version);
                System.out.println("LAddr: " + addr);
                System.out.println("IAddr: " + iaddr);
                System.out.println("PAddr: " + paddr);
                System.out.println("Songs: " + songs);
                System.out.println("StartSong: " + startsong);
                System.out.println("Speed: " + speed);
                byte[] str = new byte[0x20];
                for (int i = 0; i < songs; i++) {
                    reader.readFully(str);
                    System.out.println("Song " + (i + 1));
                    System.out.println("Name      :" + new String(str));
                    reader.readFully(str);
                    System.out.println("Author    :" + new String(str));
                    reader.readFully(str);
                    System.out.println("Copyright :" + new String(str));
                }
                if (version == 2) {
                    byte[] garbage = new byte[6];
                    reader.readFully(garbage);
                }
                byte[] adr = new byte[2];
                reader.readFully(adr);
            }
        } catch (Exception e) {
            System.out.println("Error while reading SID");
        }
        return false;
    }

    private int readSector(DataInputStream reader, int sector) {
        int no, numRead = 0;
        try {
            no = numRead = reader.read(sectors[sector]);
            while (numRead < 256) {
                no = reader.read(sectors[sector], numRead, 256 - numRead);
                if (no == -1) return numRead;
                numRead += no;
            }
        } catch (Exception e) {
            System.out.println("Exception while reading file... " + e);
        }
        return numRead;
    }

    public boolean readDiskFromFile(String name) {
        try {
            System.out.println("Loading " + name);
            FileInputStream reader = new FileInputStream(name);
            return readDisk(reader);
        } catch (Exception e) {
            System.out.println("Error while opening file " + name + "  " + e);
        }
        return false;
    }

    public boolean readPGM(URL url, int address) {
        try {
            return readPGM(url.openConnection().getInputStream(), address);
        } catch (Exception e) {
            System.out.println("Error when opening url " + url + "  " + e);
        }
        return false;
    }

    public boolean readPGM(String file, int address) {
        try {
            return readPGM(new FileInputStream(file), address);
        } catch (Exception e) {
            System.out.println("Error when opening file " + file + "  " + e);
        }
        return false;
    }

    public boolean readDiskFromURL(URL url) {
        try {
            System.out.println("Loading from " + url);
            InputStream reader = url.openConnection().getInputStream();
            return readDisk(reader);
        } catch (Exception e) {
            System.out.println("Error when opening url " + url + "  " + e);
        }
        return false;
    }

    public boolean readTapeFromFile(String name) {
        try {
            System.out.println("Loading " + name);
            FileInputStream reader = new FileInputStream(name);
            return readTape(reader);
        } catch (Exception e) {
            System.out.println("Error while opening file " + name + "  " + e);
        }
        return false;
    }

    public boolean readTapeFromURL(URL url) {
        try {
            System.out.println("Loading from " + url);
            InputStream reader = url.openConnection().getInputStream();
            return readTape(reader);
        } catch (Exception e) {
            System.out.println("Error when opening url " + url + "  " + e);
        }
        return false;
    }

    public boolean readSIDFromFile(String name) {
        try {
            System.out.println("Loading SID " + name);
            FileInputStream reader = new FileInputStream(name);
            return readSID(reader);
        } catch (Exception e) {
            System.out.println("Error while opening file " + name + "  " + e);
        }
        return false;
    }

    public static void main(String[] args) {
        C64Reader cr = new C64Reader();
        cr.readDiskFromFile(args[0]);
        if (args.length > 1) {
            try {
                FileOutputStream fio = new FileOutputStream(args[2]);
                cr.readFile(args[1], 0, fio);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            cr.printDirListing(System.out);
        }
    }
}
