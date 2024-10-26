package com.dreamfabric.jac64;

import java.io.BufferedReader;
import java.net.URL;
import java.io.DataInputStream;
import java.io.InputStreamReader;

/**
 * Describe class C1541Emu here.
 *
 *
 * Created: Tue Aug 01 12:29:57 2006
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public class C1541Emu extends MOS6510Core {

    public static final boolean DEBUG = false;

    public static final boolean IODEBUG = false;

    public static final int C1541ROM = 0xc000;

    public static final int RESET_VECTOR = 0xfffc;

    public C1541Chips chips;

    public C1541Emu(IMonitor m, String cb) {
        super(m, cb);
        memory = new int[0x10000];
        chips = new C1541Chips(this);
        init(chips);
        loadDebug("c1541dbg.txt");
        debug = false;
    }

    public String getName() {
        return "C1541 CPU";
    }

    public void setReader(C64Reader reader) {
        chips.setReader(reader);
    }

    protected final int fetchByte(int adr) {
        cycles++;
        if (adr < 0x800 || adr >= 0xc000) return memory[adr];
        int c = adr & 0xff00;
        if (c == 0x1800 || c == 0x1c00) {
            int data = chips.performRead(adr, cycles);
            if (IODEBUG) System.out.println("C1541: Reading from " + Integer.toHexString(adr) + " PC = " + Integer.toHexString(pc) + " => " + Integer.toHexString(data));
            return data;
        }
        return 0;
    }

    protected final void writeByte(int adr, int data) {
        cycles++;
        if (adr < 0x800) {
            memory[adr] = data;
        }
        int c = adr & 0xff00;
        if (c == 0x1800 || c == 0x1c00) chips.performWrite(adr, data, cycles);
    }

    public void reset() {
        super.reset();
        pc = memory[RESET_VECTOR] | (memory[RESET_VECTOR + 1] << 8);
        System.out.println("C1541: Reset to " + Integer.toHexString(pc));
    }

    boolean byteReady = false;

    void triggerByteReady() {
        byteReady = true;
    }

    public void tick(long c64Cycles) {
        while (cycles < c64Cycles) {
            if (byteReady && chips.byteReadyOverflow) {
                overflow = true;
                byteReady = false;
            }
            if (DEBUG) {
                String msg;
                if ((msg = getDebug(pc)) != null) {
                    System.out.println("C1541: " + Integer.toHexString(pc) + "****** " + msg + " Data: " + Integer.toHexString(memory[0x85]) + " => '" + (char) memory[0x85] + '\'');
                }
            }
            if (DEBUG && (monitor.isEnabled() || interruptInExec > 0)) {
                monitor.disAssemble(memory, pc, acc, x, y, (byte) getStatusByte(), interruptInExec, lastInterrupt);
            }
            emulateOp();
            if (chips.nextCheck < cycles) {
                chips.clock(cycles);
            }
        }
    }

    public void patchROM(PatchListener list) {
    }

    public void loadDebug(String resource) {
        try {
            URL url = getClass().getResource(resource);
            monitor.info("Loading debug from URL: " + url);
            if (url == null) url = new URL(codebase + resource);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                int adr = -1;
                try {
                    adr = Integer.parseInt(parts[0].trim(), 16);
                } catch (Exception e) {
                }
                if (adr != -1) {
                    setDebug(adr, parts[1].trim());
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to load debug text: " + resource);
        }
    }

    protected void readROM(String resource, int startMem, int len) {
        try {
            URL url = getClass().getResource(resource);
            monitor.info("URL: " + url);
            monitor.info("Read ROM " + resource);
            if (url == null) url = new URL(codebase + resource);
            loadROM(new DataInputStream(url.openConnection().getInputStream()), startMem, len);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void installROMS() {
        readROM("/roms/c1541.rom", C1541ROM, 0x4000);
    }
}
