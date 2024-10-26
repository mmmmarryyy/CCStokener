package jitt64.asm;

import jitt64.Song;
import jitt64.Track;
import jitt64.Pattern;
import jitt64.Instrument;
import jitt64.table.InstrTable;
import jitt64.table.ObjTable;
import java.io.File;
import java.io.InputStream;
import java.io.FileWriter;
import java.net.URL;
import java.lang.reflect.Method;

/**
 * This is the packer for pack and relocate the tune.
 * It uses the player.s and dasm compiler to achieve this
 * 
 * @author ice
 */
public class Packer {

    /** Type of file is SID */
    public static final int TYPE_SID = 0;

    /** Type of file is PRG */
    public static final int TYPE_PRG = 1;

    /** Type of file is BIN */
    public static final int TYPE_BIN = 2;

    /**
   * Get the asm description of the passed object table
   * 
   * @param objTable the object table to describe as asm
   * @return the asm source declaration
   */
    private StringBuffer getAsmFullTable(ObjTable objTable) {
        StringBuffer res = new StringBuffer();
        int type = objTable.getType();
        int max = objTable.getMaxIndex();
        int[] values = objTable.getValues();
        for (int i = 0; i <= max; i++) {
            switch(type) {
                case ObjTable.TYPE_DELAY:
                case ObjTable.TYPE_REPEAT:
                    res.append(" .byte " + values[i] + "\n");
                    break;
                case ObjTable.TYPE_FILTER_FIXED_FREQ:
                case ObjTable.TYPE_FILTER_REL_FREQ:
                    res.append(" .byte " + (values[i] & 0x7) + ", " + (values[i] >> 3) + "\n");
                    break;
                default:
                    res.append(" .byte " + (values[i] & 0xFF) + ", " + (values[i] >> 8) + "\n");
            }
        }
        return res;
    }

    /**
   * Get the asm description of the passed table
   * 
   * @param insTable the table to describe as asm
   * @return the asm source declaration
   */
    private StringBuffer getAsmFullTable(InstrTable insTable) {
        StringBuffer res = new StringBuffer();
        int maxDim = insTable.getDimension();
        if (maxDim == 0) maxDim = 1; else maxDim = maxDim * 2 + 3;
        for (int i = 0; i < maxDim; i++) {
            res.append(" .byte " + insTable.getInternalValue(i) + "\n");
        }
        return res;
    }

    /**
   * Get the asm string for this instrument without optimization
   * 
   * @param ins the instrument to code
   * @param index the index of this instrument
   * @return the asm string for the player
   */
    private StringBuffer getAsmFullInstrument(Instrument ins, int index) {
        StringBuffer buf = new StringBuffer();
        int hr = ((ins.isFlagHR()) ? 0x80 : 0) + ((ins.isFlagGateOff()) ? 0x40 : 0) + ((ins.isFlagOrder()) ? 0x20 : 0) + ins.getValueNTicks();
        buf.append("INSTR_HR_" + index + " = " + hr + "\n");
        buf.append("INSTR_AD_" + index + " = " + ins.getValueADHR() + "\n");
        buf.append("INSTR_SR_" + index + " = " + ins.getValueSRHR() + "\n");
        buf.append("INSTR_CTRL1_" + index + " = " + ins.getValueCtrl1HR() + "\n");
        buf.append("INSTR_CTRL2_" + index + " = " + ins.getValueCtrl2HR() + "\n");
        buf.append("instrAD_" + index + ":\n" + getAsmFullTable(ins.getInstrTableAD()));
        buf.append("instrSR_" + index + ":\n" + getAsmFullTable(ins.getInstrTableSR()));
        buf.append("instrWave_" + index + ":\n" + getAsmFullTable(ins.getInstrTableWave()));
        buf.append("instrFreq_" + index + ":\n" + getAsmFullTable(ins.getInstrTableFreq()));
        buf.append("instrPulse_" + index + ":\n" + getAsmFullTable(ins.getInstrTablePulse()));
        buf.append("instrFilter_" + index + ":\n" + getAsmFullTable(ins.getInstrTableFilter()));
        buf.append("instrRes_" + index + ":\n" + getAsmFullTable(ins.getInstrTableFilterRes()));
        buf.append("instrType_" + index + ":\n" + getAsmFullTable(ins.getInstrTableFilterType()));
        buf.append("instrFixFreq_" + index + ":\n" + getAsmFullTable(ins.getObjTableFixedFreq()));
        buf.append("instrRelFreq_" + index + ":\n" + getAsmFullTable(ins.getObjTableRelFreq()));
        buf.append("instrFixPulse_" + index + ":\n" + getAsmFullTable(ins.getObjTableFixedPulse()));
        buf.append("instrRelPulse_" + index + ":\n" + getAsmFullTable(ins.getObjTableRelPulse()));
        buf.append("instrRelFilter_" + index + ":\n" + getAsmFullTable(ins.getObjTableFilterRelFreq()));
        buf.append("instrFixFilter_" + index + ":\n" + getAsmFullTable(ins.getObjTableFilterFixedFreq()));
        buf.append("instrDelay_" + index + ":\n" + getAsmFullTable(ins.getObjTableDelay()));
        buf.append("instrRepeat_" + index + ":\n" + getAsmFullTable(ins.getObjTableRepeat()));
        return buf;
    }

    /**
   * Get the asm string for this instrument without optimization 
   * 
   * @param track the track to write
   * @param tune the number of this tune (1..256)
   * @param voice the voice of this track (1..3)
   * @return the asm string
   */
    private StringBuffer getAsmFullTrack(Track track, int tune, int voice) {
        StringBuffer buf = new StringBuffer();
        buf.append("tune" + tune + "_track" + voice);
        int size = track.getSize();
        for (int i = 0; i < size; i++) {
            buf.append("  .byte " + track.getValueAt(i) + "\n");
        }
        return buf;
    }

    /**
   * Get the asm string for all the used tracks
   * 
   * @param song the song to use
   * @return tre asm string
   */
    private StringBuffer getAsmFullTracks(Song song) {
        StringBuffer buf = new StringBuffer();
        Track[][] tracks = song.getTracks();
        int tunes = song.getNumberOfTunes();
        for (int i = 0; i < tunes; i++) {
            buf.append(getAsmFullTrack(tracks[i][0], i + 1, 1));
            buf.append(getAsmFullTrack(tracks[i][1], i + 1, 2));
            buf.append(getAsmFullTrack(tracks[i][2], i + 1, 3));
        }
        return buf;
    }

    /**
   * Get the asm string for the pattern
   * 
   * @param pattern the pattern to use
   * @param index the index of the pattern
   * @param nByte number of byte
   * @return
   */
    private StringBuffer getAsmFullPattern(Pattern pattern, int index, int nByte) {
        StringBuffer buf = new StringBuffer();
        buf.append("pat" + index + "_b" + nByte);
        int size = pattern.getSize();
        buf.append("  .byte " + pattern.getTempo() + "\n");
        for (int i = 0; i < size; i++) {
            switch(nByte) {
                case 1:
                    buf.append("  .byte " + pattern.getPatNotes()[i] + "\n");
                    break;
                case 2:
                    buf.append("  .byte " + pattern.getPatInstr()[i] + "\n");
                    break;
                case 3:
                    buf.append("  .byte " + pattern.getPatCommand()[i] + "\n");
                    break;
                case 4:
                    buf.append("  .byte " + pattern.getPatParams()[i] + "\n");
                    break;
            }
        }
        if (nByte == 1) buf.append("  .byte " + Pattern.VAL_END + "\n");
        return buf;
    }

    /**
   * Get the asm string for all the used patterns
   * 
   * @param song the song to use
   * @return the asm string
   */
    private StringBuffer getAsmFullPatterns(Song song) {
        StringBuffer buf = new StringBuffer();
        Pattern[] patterns = song.getPatterns();
        int maxPat = song.getMaxPattern();
        for (int i = 0; i <= maxPat; i++) {
            buf.append(getAsmFullPattern(patterns[i], i, 1));
            buf.append(getAsmFullPattern(patterns[i], i, 2));
            buf.append(getAsmFullPattern(patterns[i], i, 3));
            buf.append(getAsmFullPattern(patterns[i], i, 4));
        }
        return buf;
    }

    /**
   * Get the string information for the PSID creation
   * The code add even the string for comment, not actually a part of PSID
   * The string is so located at the end of the palyer
   * 
   * @param song the song to use
   * @return the string to use in source
   */
    private String getPSIDStrings(Song song) {
        String name = song.getSidName().replace('"', ' ');
        String author = song.getSidAuthor().replace('"', ' ');
        String copyright = song.getSidCopyright().replace('"', ' ');
        String comment = song.getPlayerComment().replace('"', ' ');
        if ("".equals(comment)) comment = " ";
        comment = comment.substring(0, Math.min(215, comment.length()));
        for (int i = name.length() + 1; i <= 32; i++) name += " ";
        for (int i = author.length() + 1; i <= 32; i++) author += " ";
        for (int i = copyright.length() + 1; i <= 32; i++) copyright += " ";
        int tune = song.getNumberOfTunes();
        int pTune;
        if (tune == 256) pTune = 1; else pTune = tune * 256;
        return "PSID_NAME EQM \"" + name + "\"\n" + "PSID_AUTHOR EQM \"" + author + "\"\n" + "PSID_COPYRIGHT EQM \"" + copyright + "\"\n" + "PSID_TUNE EQU " + pTune + "\n" + "PSID_COMMENT EQM \"" + comment + "\"\n" + " .byte PSID_COMMENT\n";
    }

    /**
   * Generate the output file of the given song
   * The extension is automatically added to the name according to the type
   * 
   * @param type the type of file to generate
   * @param song the song to pack
   * @param sourceName the name of the source name to use
   * @param path the path of the output file
   * @param name the name of the output file
   * @param tmpPath path for tmp directory to use
   * @param addr address of relocation
   * @param debug true if output debug can be generated
   * @param f4Table the frequency table to use
   * @return true if all is ok
   */
    public boolean generate(int type, Song song, String sourceName, String path, String name, String tmpPath, int addr, boolean debug, int f4Table) {
        int maxInstr;
        String outFileName;
        String inFileName;
        String res;
        String extension = "";
        String exDefine = "";
        String exFormat = "";
        switch(type) {
            case TYPE_SID:
                extension = ".sid";
                exDefine = "-DEX_SID=1";
                exFormat = "-f3";
                break;
            case TYPE_PRG:
                extension = ".prg";
                exDefine = "-DEX_SID=2";
                exFormat = "-f1";
                break;
            case TYPE_BIN:
                extension = ".bin";
                exDefine = "-DEX_SID=3";
                exFormat = "-f3";
                break;
        }
        String exDefineTune = "-DNUM_TUNE=" + song.getNumberOfTunes();
        String exDefinePat = "-DNUM_PAT=" + song.getMaxPattern();
        maxInstr = song.getMaxInstrument();
        String exDefineInstr = "-DNUM_INSTR=" + maxInstr;
        outFileName = path + File.separator + name + extension;
        inFileName = tmpPath + File.separator + sourceName;
        try {
            URL url = getClass().getResource("/jitt64/asm/player.s");
            File outputFile = new File(inFileName);
            InputStream in = url.openStream();
            FileWriter out = new FileWriter(outputFile);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            for (int i = 1; i <= maxInstr; i++) {
                res = getAsmFullInstrument(song.getInstruments()[i - 1], i).toString();
                out.write(res);
            }
            res = getAsmFullTracks(song).toString();
            out.write(res);
            res = getAsmFullPatterns(song).toString();
            out.write(res);
            res = getPSIDStrings(song);
            out.write(res);
            in.close();
            out.close();
        } catch (Exception e) {
            System.err.println(e);
            return false;
        }
        String[] args;
        if (debug) args = new String[37]; else args = new String[36];
        args[0] = " ";
        args[1] = inFileName;
        args[2] = "-o" + outFileName;
        args[3] = exDefine;
        args[4] = exDefineTune;
        args[5] = exDefinePat;
        args[6] = exDefineInstr;
        args[7] = exFormat;
        args[8] = "-DUSE_INSTR_AD=" + song.getUseInstrTable(Song.INSTR_AD);
        args[9] = "-DUSE_INSTR_SR=" + song.getUseInstrTable(Song.INSTR_SR);
        args[10] = "-DUSE_INSTR_WAVE=" + song.getUseInstrTable(Song.INSTR_WAVE);
        args[11] = "-DUSE_INSTR_FREQ=" + song.getUseInstrTable(Song.INSTR_FREQ);
        args[12] = "-DUSE_INSTR_PULSE=" + song.getUseInstrTable(Song.INSTR_PULSE);
        args[13] = "-DUSE_INSTR_FILTER=" + song.getUseInstrTable(Song.INSTR_FILTER);
        args[14] = "-DUSE_INSTR_RES=" + song.getUseInstrTable(Song.INSTR_RES);
        args[15] = "-DUSE_INSTR_TYPE=" + song.getUseInstrTable(Song.INSTR_TYPE);
        args[16] = "-DA4_FREQ=" + f4Table;
        args[17] = "-DEX_BASE=" + addr;
        args[18] = "-DEX_SPEED=" + song.getIntSpeed();
        args[19] = "-DEX_CHIP=" + song.getIntChip();
        args[20] = "-DUSE_CMD_TEMPO=" + song.getUsePatternCmd(Song.CMD_TEMPO);
        args[21] = "-DUSE_CMD_AD=" + song.getUsePatternCmd(Song.CMD_AD);
        args[22] = "-DUSE_CMD_SR=" + song.getUsePatternCmd(Song.CMD_SR);
        args[23] = "-DUSE_CMD_VOLUME=" + song.getUsePatternCmd(Song.CMD_VOLUME);
        args[24] = "-DUSE_CMD_ARP=" + song.getUsePatternCmd(Song.CMD_ARP);
        args[25] = "-DUSE_CMD_PUP=" + song.getUsePatternCmd(Song.CMD_PUP);
        args[26] = "-DUSE_CMD_PDN=" + song.getUsePatternCmd(Song.CMD_PDN);
        args[27] = "-DUSE_CMD_TPO=" + song.getUsePatternCmd(Song.CMD_TPO);
        args[28] = "-DUSE_CMD_VIB=" + song.getUsePatternCmd(Song.CMD_VIB);
        args[29] = "-DUSE_CMD_SUP=" + song.getUsePatternCmd(Song.CMD_SUP);
        args[30] = "-DUSE_CMD_SDN=" + song.getUsePatternCmd(Song.CMD_SDN);
        args[31] = "-DUSE_CMD_FOUT=" + song.getUsePatternCmd(Song.CMD_FOUT);
        args[32] = "-DUSE_CMD_FTY=" + song.getUsePatternCmd(Song.CMD_FTY);
        args[33] = "-DUSE_CMD_FRS=" + song.getUsePatternCmd(Song.CMD_FRS);
        args[34] = "-DUSE_CMD_FCU=" + song.getUsePatternCmd(Song.CMD_FCU);
        args[35] = "-DUSE_CMD_GSR=" + song.getUsePatternCmd(Song.CMD_GSR);
        if (debug) args[36] = "-L" + tmpPath + File.separator + "out.txt";
        try {
            Class cl = Class.forName("jitt64.asm.Main");
            Method mMain = cl.getMethod("run", new Class[] { String[].class });
            mMain.invoke(cl.newInstance(), new Object[] { args });
        } catch (Exception e) {
            System.err.println(e);
            return false;
        }
        return true;
    }
}
