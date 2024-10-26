package grame.midishare.midifile;

import java.io.*;
import java.net.*;
import grame.midishare.*;
import grame.midishare.tools.*;

/**
 A class to load a MIDIfile an convert it in a MidiShare sequence or to save a MidiShare
 sequence in a MIDIfile. The most important methods are Load and Save. 
 */
public class MidiFileStream {

    static final short codeTbl[] = { 0x90, 0x90, 0x80, 0xa0, 0xb0, 0xc0, 0xd0, 0xe0, 0xf2, 0xf3, 0xf8, 0xfa, 0xfb, 0xfc, 0xf6, 0xfe, 0xff, 0xf0, 0xf7 };

    static final short typeTbl[] = { 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 17, 130, 8, 9, 0, 0, 14, 18, 10, 0, 11, 12, 13, 0, 15, 16 };

    static final short metaCodeTbl[] = { 0xf1, 0xb0, 0xb0, 0xb0, 0, 1, 2, 3, 4, 5, 6, 7, 0x20, 0x2f, 0x51, 0x54, 0x58, 0x59, 0x7f, 0x21 };

    static final byte metaTypeTbl[] = { 1, 2, 3, 4, 5, 6, 7, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 11, 0, 0, 12, 0, 0, 0, 13, 14, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15 };

    static final MfEvent ReadEvTbl[] = { new Data2Ev(), new Data2Ev(), new Data2Ev(), new Data2Ev(), new Data1Ev(), new Data1Ev(), new Data2Ev(), new sysEx(), new Data1Ev(), new Data2Ev(), new Data1Ev(), new undef(), new undef(), new Data0Ev(), new stream(), new Data0Ev(), new undef(), new Data0Ev(), new Data0Ev(), new Data0Ev(), new undef(), new Data0Ev(), new undef() };

    static final MfEvent ReadExtTbl[] = { new ignoreEv(), new seqNum(), new text(), new text(), new text(), new text(), new text(), new text(), new text(), new chanPref(), new endTrack(), new tempo(), new smpte(), new timeSign(), new keySign(), new text(), new portPref() };

    final MfEvent WriteEvTbl[] = { new Note(), new Data2Ev(), new Data2Ev(), new Data2Ev(), new Data2Ev(), new Data1Ev(), new Data1Ev(), new Data2Ev(), new Data2Ev(), new Data1Ev(), new Data0Ev(), new Data0Ev(), new Data0Ev(), new Data0Ev(), new Data0Ev(), new Data0Ev(), new dont_write(), new sysEx(), new stream() };

    final MfEvent WriteExtTbl[] = { new Data1Ev(), new Ctrl14b(), new NRegP(), new RegP(), new seqNum(), new text(), new text(), new text(), new text(), new text(), new text(), new text(), new chanPref(), new endTrack(), new tempo(), new smpte(), new timeSign(), new keySign(), new text(), new portPref() };

    static final short META = 0xFF;

    static final byte MDF_NumSeq = 0;

    static final byte MDF_Texte = 1;

    static final byte MDF_Copyright = 2;

    static final byte MDF_SeqName = 3;

    static final byte MDF_InstrName = 4;

    static final byte MDF_Lyric = 5;

    static final byte MDF_Mark = 6;

    static final byte MDF_CuePt = 7;

    static final byte MDF_ChanPref = 0x20;

    static final byte MDF_EndTrk = 0x2F;

    static final byte MDF_Tempo = 0x51;

    static final byte MDF_Offset = 0x54;

    static final byte MDF_Meas = 0x58;

    static final byte MDF_Ton = 0x59;

    static final byte MDF_Extra = 0x7F;

    static final byte MDF_PortPref = 0x21;

    static final short MDF_NumSeqLen = 2;

    static final short MDF_ChanPrefLen = 1;

    static final short MDF_PortPrefLen = 1;

    static final short MDF_EndTrkLen = 0;

    static final short MDF_TempoLen = 3;

    static final short MDF_OffsetLen = 5;

    static final short MDF_MeasLen = 4;

    static final short MDF_TonLen = 2;

    static final String MDF_MThd = "MThd";

    static final String MDF_MTrk = "MTrk";

    static final String Player = "PLAYER ref:";

    static final short maxTrack = 256;

    int trackListe[];

    short format;

    short ntrks = 0;

    short time;

    DataInputStream input;

    DataOutputStream output;

    SeekOutputStream seekoutput;

    int countpos;

    int lenpos;

    int trkHeadOffset;

    int _cnt;

    int keyOff;

    int curDate;

    boolean opened;

    private short status = 0;

    final short offset_ntrks = 10;

    final short offset_trkLen = 4;

    final boolean isTrackOpen() {
        return opened;
    }

    final boolean IsTempoMap(int t) {
        return (t == Midi.typeCopyright) || (t == Midi.typeMarker) || ((t >= Midi.typeTempo) && (t <= Midi.typeKeySign));
    }

    final short smpte() {
        return (short) (time & 0x8000);
    }

    final short frame_par_sec() {
        return (short) ((time & 0x8000) >> 8);
    }

    final short ticks_par_frame() {
        return (short) (time & 0xFF);
    }

    final short ticks_par_quarterNote() {
        return time;
    }

    public MidiFileStream() {
        trackListe = new int[MidiFileStream.maxTrack];
    }

    /**
 	Open an existing MIDI file. 
 	*@param file is the file to be opened.
 	*@exception Exception If the file can not be opened. 
  	*/
    public final void Open(File file) throws Exception {
        input = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        MdfHeader mdfheader = new MdfHeader();
        mdfheader.read(input);
        format = mdfheader.format;
        ntrks = mdfheader.ntrks;
        time = mdfheader.time;
    }

    public final void Open(InputStream inputstream) throws Exception {
        input = new DataInputStream(new BufferedInputStream(inputstream));
        MdfHeader mdfheader = new MdfHeader();
        mdfheader.read(input);
        format = mdfheader.format;
        ntrks = mdfheader.ntrks;
        time = mdfheader.time;
    }

    /**
 	Open an existing MIDI file. 
 	*@param url is the url to be opened.
 	*@exception Exception If the file can not be opened. 
 	*/
    public final void Open(URL url) throws Exception {
        input = new DataInputStream(new BufferedInputStream(url.openStream()));
        MdfHeader mdfheader = new MdfHeader();
        InitRead();
        mdfheader.read(input);
        format = mdfheader.format;
        ntrks = mdfheader.ntrks;
        time = mdfheader.time;
    }

    /**
 	Create a MIDIFile format file. The function parameters are as follow:
    *@param format	is the  MIDIFile format of the file, it can takes the following values:
	<BR>
	- midifile0 : format 0 (one track)
	<BR>
	- midifile1 : format 1 (several tracks, to read according
				       	      to the tempo map contained in the track 0)
	<BR>
	- midifile2 : format 2 (several independant patterns one per track, every track contains its own tempo map)
	

    *@param timeDef	is the time representation, it can takes the following values:
    <br>
    - TicksPerQuarterNote 	: MIDI measured time.
    <br>
    - Smpte24            	: smpte time 24 frame/sec.
    <br>
    - Smpte25             	: smpte time 25 frame/sec.
    <br>
    - Smpte29             	: smpte time 29 frame/sec.
    <br>
    - Smpte30             	: smpte time 30 frame/sec.
    *@param  ticks    	: for MIDI time: represents the ticks count per quarter note.
	for smpte time: represents the ticks count per frame.
			  		
	*@exception Exception If the file can not be created. 
	*/
    public final void Create(short format, short ntrks, short timeDef, short ticks) throws Exception {
        int time;
        seekoutput = new SeekOutputStream();
        output = new DataOutputStream(seekoutput);
        MdfHeader mdfheader = new MdfHeader();
        this.ntrks = 0;
        this.format = format;
        if (timeDef == MidiFileInfos.TicksPerQuarterNote) {
            time = ((short) ticks & 0x7FFF);
        } else {
            time = (timeDef | 0x80) << 8;
            time |= ticks;
        }
        InitWrite();
        countpos = mdfheader.write(output, ntrks, format, (short) time);
    }

    public final void Close() throws Exception {
        if (isTrackOpen()) {
            CloseTrack();
        }
        if (keyOff != 0) {
            Midi.FreeSeq(keyOff);
        }
        output.close();
    }

    final void InitRead() throws Exception {
        opened = false;
        curDate = 0;
        keyOff = 0;
    }

    final void InitWrite() throws Exception {
        opened = false;
        curDate = 0;
        keyOff = Midi.NewSeq();
        if (keyOff == 0) throw new MidiException("No more MidiShare event");
    }

    /**
 	ReadEv returns the next event within the current track. The track must be opened using 
	OpenTrack before reading an event. When you reach the end of the current track, it is 
	automaticaly closed  and the function returns 0.
	*@exception Exception If a  MidiShare error occurs.
 	*/
    public final int ReadEv() throws Exception {
        int ev = 0;
        if (isTrackOpen()) {
            if (_cnt > 0) {
                mdf_GetDate();
                ev = ReadEvAux();
                if (ev != 0) Midi.SetDate(ev, curDate);
            } else {
                opened = false;
            }
        } else throw new IOException("Error track closed");
        return ev;
    }

    /**
 	The function reads the current track from the file and returns the result in a MidiShare 
 	sequence.  ReadTrack automaticaly opens and closes the track to read.
	*@Exception Exception If the function returns nil when the track is still opened.
 	*/
    public final int ReadTrack() throws Exception {
        int ev, seq = 0;
        OpenTrack();
        if ((seq = Midi.NewSeq()) != 0) {
            try {
                while (isTrackOpen()) {
                    if ((ev = ReadEv()) != 0) {
                        if (Midi.GetFirstEv(seq) == 0) {
                            Midi.SetFirstEv(seq, ev);
                            Midi.SetLastEv(seq, ev);
                        } else {
                            Midi.SetLink(Midi.GetLastEv(seq), ev);
                            Midi.SetLastEv(seq, ev);
                        }
                    } else {
                    }
                }
                return seq;
            } catch (MidiException e) {
                Midi.FreeSeq(seq);
                throw e;
            } catch (Exception e) {
                throw e;
            }
        } else throw new MidiException("No more MidiShare event");
    }

    /**
 	Open the track if the file is opened for reading.The purpose of this 
	function consists essentially in data initialization to facilitate the track handling.
	*@exception Exception If the track is already opened.
 	*/
    public final void OpenTrack() throws Exception {
        TrkHeader trkheader = new TrkHeader();
        if (isTrackOpen()) throw new IOException("Track already opened");
        trkheader.read(input);
        _cnt = trkheader.len;
        curDate = 0;
        trkHeadOffset = 0;
        opened = true;
    }

    final int ReadEvAux() throws Exception {
        int ev = 0;
        short c;
        try {
            input.mark(0xffff);
            c = (short) input.read();
            _cnt--;
            if (c == META) {
                ev = mdf_read_meta();
                c = status = 0;
            } else if ((c & 0x80) != 0) status = c; else if (status != 0) {
                input.reset();
                _cnt++;
                c = status;
            } else {
                c = 0;
                throw new IOException("Midi File error format");
            }
            if (c != 0) {
                if ((ev = ReadEvTbl[(c < Midi.SysEx) ? (c & 0x7F) / 16 : c - Midi.SysEx + 7].read(this, c)) != 0) {
                    if (c < Midi.SysEx) Midi.SetChan(ev, c % 16);
                } else {
                }
            }
            return ev;
        } catch (Exception e) {
            if (ev != 0) Midi.FreeEv(ev);
            throw e;
        }
    }

    final int mdf_read_meta() throws Exception {
        short type;
        int len;
        type = (short) input.read();
        _cnt--;
        len = ReadVarLen();
        if ((type & 0x80) != 0) {
            type = 0;
        } else {
            type = MidiFileStream.metaTypeTbl[type];
        }
        return MidiFileStream.ReadExtTbl[type].read(this, len, type);
    }

    final int ReadVarLen() throws Exception {
        int val;
        short c;
        if (((val = input.read()) & 0x80) != 0) {
            val &= 0x7F;
            do {
                val = (val << 7) + ((c = (short) input.read()) & 0x7F);
                _cnt--;
            } while ((c & 0x80) != 0);
        }
        _cnt--;
        return (int) val;
    }

    final int ReadVarLen1(int ev) throws Exception {
        int val;
        short c;
        val = input.read();
        Midi.AddField(ev, val);
        if ((val & 0x80) != 0) {
            val &= 0x7F;
            do {
                val = (val << 7) + ((c = (short) input.read()) & 0x7F);
                Midi.AddField(ev, c);
                _cnt--;
            } while ((c & 0x80) != 0);
        }
        _cnt--;
        return (int) val;
    }

    final void WriteVarLen(int val) throws Exception {
        long buf, val1;
        if (val < 0) throw new IllegalArgumentException("WriteVarLen : arg < 0 !!");
        val1 = (long) val;
        buf = val1 & 0x7F;
        while ((val1 >>= 7) != 0) {
            buf <<= 8;
            buf |= 0x80;
            buf += (val1 & 0x7F);
        }
        while (true) {
            output.write((int) buf);
            if ((buf & 0x80) != 0) {
                buf >>= 8;
            } else break;
        }
    }

    static final int GetVarLen(int val) {
        long buf;
        int res = 0;
        buf = val & 0x7F;
        while ((val >>= 7) != 0) {
            buf <<= 8;
            buf |= 0x80;
            buf += (val & 0x7F);
        }
        while (true) {
            res++;
            if ((buf & 0x80) != 0) buf >>= 8; else break;
        }
        return res;
    }

    final void write_param(short num, short val, short type) throws Exception {
        output.write(type);
        output.write(num >> 7);
        WriteVarLen(0);
        output.write(type - 1);
        output.write(num & 0x7F);
        WriteVarLen(0);
        output.write(6);
        output.write(val >> 7);
        WriteVarLen(0);
        output.write(38);
        output.write(val & 0x7F);
    }

    final void WriteEvAux(int ev) throws Exception {
        int type;
        type = Midi.GetType(ev);
        if (type < Midi.typePrivate) WriteEvTbl[type].write(this, ev, (short) (type < Midi.typeSongPos ? codeTbl[type] + Midi.GetChan(ev) : codeTbl[type])); else if (type >= Midi.typeQFrame && type < Midi.typeReserved) {
            type -= Midi.typeQFrame;
            WriteExtTbl[type].write(this, ev, metaCodeTbl[type]);
        }
    }

    final void FlushKeyOff() throws Exception {
        int seq, ev, date;
        seq = keyOff;
        ev = Midi.GetFirstEv(seq);
        date = curDate;
        while (ev != 0) {
            Midi.SetFirstEv(seq, Midi.GetLink(ev));
            WriteVarLen(Midi.GetDate(ev) - date);
            WriteEvAux(ev);
            date = Midi.GetDate(ev);
            Midi.FreeEv(ev);
            ev = Midi.GetFirstEv(seq);
        }
        Midi.SetLastEv(seq, 0);
        curDate = date;
    }

    /**
 	NewTrack adds a new track header at the end of the file and open the corresponding track. 
 	You can use  this function only if the file is opened for writing. 
	 A previously opened track will first been closed.
 	*@exception Exception 
  	*/
    public final void NewTrack() throws Exception {
        TrkHeader trkheader = new TrkHeader();
        if (isTrackOpen()) CloseTrack();
        lenpos = trkheader.write(output);
        curDate = 0;
        opened = true;
    }

    /**
 	Close a track previously opened with OpenTrack or created with NewTrack.
 		<br> 
	- If the file is opened for reading, CloseTrack locate the file pointer at the beginning of the next 
	track.
 		<br> 
	- If the file is opened for writing, CloseTrack flush the KeyOff sequence (coming from typeNote 
	events), update the track header and the file header.
	The function does nothing  if the track is still closed.

 	*@exception Exception If the track is already closed.
  	*/
    public final void CloseTrack() throws Exception {
        if (isTrackOpen()) {
            FlushKeyOff();
            int length = output.size() - lenpos - 4;
            seekoutput.writeInt(length, lenpos);
            ntrks++;
            seekoutput.writeShort(ntrks, countpos);
            opened = false;
        } else throw new IOException("Track already closed");
    }

    public final void WriteEv(int event) throws Exception {
        int seq, off, date;
        if (!isTrackOpen()) throw new IOException("Track  closed");
        date = curDate;
        seq = keyOff;
        off = Midi.GetFirstEv(seq);
        while ((off != 0) && (Midi.GetDate(event) >= Midi.GetDate(off))) {
            WriteVarLen(Midi.GetDate(off) - date);
            WriteEvAux(off);
            date = Midi.GetDate(off);
            Midi.SetFirstEv(seq, Midi.GetLink(off));
            Midi.FreeEv(off);
            if ((off = Midi.GetFirstEv(seq)) == 0) Midi.SetLastEv(seq, 0);
        }
        WriteVarLen(Midi.GetDate(event) - date);
        WriteEvAux(event);
        curDate = Midi.GetDate(event);
    }

    /**
 	Writes in order all the events of the sequence seq to the file. WriteTrack automatically 
	create and close the written track. 
	*@exception Exception If a IO error or a MidiShare error occurs.
 	*/
    public final void WriteTrack(int seq) throws Exception {
        int ev;
        NewTrack();
        ev = Midi.GetFirstEv(seq);
        while (ev != 0) {
            WriteEv(ev);
            ev = Midi.GetLink(ev);
        }
        CloseTrack();
    }

    public final void WriteTrack1(int seq) throws Exception {
        int ev;
        int port = 0;
        int lastWrite = 0;
        NewTrack();
        ev = Midi.GetFirstEv(seq);
        while (ev != 0) {
            if (Midi.GetPort(ev) != port) {
                port = Midi.GetPort(ev);
                WritePortPrefix(lastWrite, port);
            }
            WriteEv(ev);
            lastWrite = ev;
            ev = Midi.GetLink(ev);
        }
        CloseTrack();
    }

    final void mdf_GetDate() throws Exception {
        int offset;
        offset = ReadVarLen();
        curDate += offset;
    }

    /**
	Load a MIDIfile and convert it in a MidiShare sequence. For a multi-tracks MIDIfile,
	the tracks are mixed in a single MidiShare sequence but are distinguished by the 
	reference number of their events.
	*@param url is the location of the MIDIFile.
	*@param seq is a pointer to the MidiShare sequence to be filled with the MIDIFile.
	*@param info is a MidiFileInfos object  to be filled with various informations read in the MIDIFile.
 	*@exception Exception If the MIDIfile cannot be read of if a MidiShare error occurs. 	
 	*/
    public final void Load(URL url, int seq, MidiFileInfos info) throws Exception {
        Open(url);
        if (seq == 0) throw new MidiException("No more MidiShare event");
        Midi.SetFirstEv(seq, 0);
        Midi.SetLastEv(seq, 0);
        try {
            ReadTracks(seq);
            MidiSequence.DelEndTrack(seq);
            info.format = format;
            ReturnTimeInfos(info);
            SetLoadDates(info, seq);
            info.tracks = ntrks;
            input.close();
        } catch (IOException e) {
            Midi.ClearSeq(seq);
            throw e;
        }
    }

    /**
	Load a MIDIfile and convert it in a MidiShare sequence. For a multi-tracks MIDIfile,
	the tracks are mixed in a single MidiShare sequence but are distinguished by the 
	reference number of their events.
	*@param mf is the location of the MIDIFile.
	*@param seq is a pointer to the MidiShare sequence to be filled with the MIDIFile.
	*@param info is a MidiFileInfos object  to be filled with various informations read in the MIDIFile.
 	*@exception Exception If the MIDIfile cannot be read of if a MidiShare error occurs. 	
 	*/
    public final void Load(File mf, int seq, MidiFileInfos info) throws Exception {
        int i, tmp, tmp1;
        Open(mf);
        if (seq == 0) throw new MidiException("No more MidiShare event");
        Midi.SetFirstEv(seq, 0);
        Midi.SetLastEv(seq, 0);
        try {
            ReadTracks(seq);
            MidiSequence.DelEndTrack(seq);
            info.format = format;
            ReturnTimeInfos(info);
            SetLoadDates(info, seq);
            info.tracks = ntrks;
            input.close();
        } catch (IOException e) {
            Midi.ClearSeq(seq);
            throw e;
        }
    }

    /**
	Load a MIDIfile and convert it in a MidiShare sequence. For a multi-tracks MIDIfile,
	the tracks are mixed in a single MidiShare sequence but are distinguished by the 
	reference number of their events.
	*@param inputstream is the location of the MIDIFile. The stream will be closed by the function.
	*@param seq is a pointer to the MidiShare sequence to be filled with the MIDIFile.
	*@param info is a MidiFileInfos object  to be filled with various informations read in the MIDIFile.
 	*@exception Exception If the MIDIfile cannot be read of if a MidiShare error occurs. 	
 	*/
    public final void Load(InputStream inputstream, int seq, MidiFileInfos info) throws Exception {
        int i, tmp, tmp1;
        Open(inputstream);
        if (seq == 0) throw new MidiException("No more MidiShare event");
        Midi.SetFirstEv(seq, 0);
        Midi.SetLastEv(seq, 0);
        try {
            ReadTracks(seq);
            MidiSequence.DelEndTrack(seq);
            info.format = format;
            ReturnTimeInfos(info);
            SetLoadDates(info, seq);
            info.tracks = ntrks;
            input.close();
        } catch (IOException e) {
            Midi.ClearSeq(seq);
            throw e;
        }
    }

    final void ReturnTimeInfos(MidiFileInfos info) {
        int t;
        if ((time & 0x8000) != 0) {
            t = time & 0x7fff;
            info.timedef = t >> 8;
            info.clicks = t & 0xff;
        } else {
            info.timedef = 0;
            info.clicks = time;
        }
    }

    final void SaveAux(int seq, MidiFileInfos info) throws Exception {
        if (info.format < 0 || info.format > 2) throw new IOException("MidiFileFormat error");
        InitTrackListe();
        Create((short) info.format, (short) info.tracks, (short) info.timedef, (short) info.clicks);
        if (info.format != 0) {
            AnalyseSeq(seq);
            WriteTracks(seq);
        } else {
            WriteTrack1(seq);
        }
    }

    /**
	Save a MidiShare sequence as a MIDIfile.
	Each track in a multi-tracks sequence must be distinguished by the 
	reference number of their events.
	*@param file is the location of the MIDIFile.
	*@param seq is a pointer to the MidiShare sequence to be saved.
	*@param info is a MidiFileInfos object and contains informations about the sequence.
	*@exception Exception If the MIDIfile cannot be saved of if a MidiShare error occurs. 	
 	*/
    public final void Save(File file, int seq, MidiFileInfos info) throws Exception {
        SaveAux(seq, info);
        BufferedOutputStream tmp = new BufferedOutputStream(new FileOutputStream(file), 1000);
        seekoutput.writeTo(tmp);
        tmp.close();
        Close();
    }

    /**
	Save a MidiShare sequence as a MIDIfile. 
	Each track in a multi-tracks sequence must be distinguished by the 
	reference number of their events.
	*@param url is the location of the MIDIFile.
	*@param seq is a pointer to the MidiShare sequence to be saved.
	*@param info is a MidiFileInfos object and contains informations about the sequence.
	*@exception Exception If the MIDIfile cannot be saved of if a MidiShare error occurs. 	
 	*/
    public final void Save(URL url, int seq, MidiFileInfos info) throws Exception {
        SaveAux(seq, info);
        URLConnection connection = url.openConnection();
        BufferedOutputStream tmp = new BufferedOutputStream(connection.getOutputStream(), 1000);
        seekoutput.writeTo(tmp);
        tmp.close();
        Close();
    }

    /**
	Save a MidiShare sequence as a MIDIfile.
	Each track in a multi-tracks sequence must be distinguished by the 
	reference number of their events.
	*@param outstream is the location of the MIDIFile. The stream will be closed by the function.
	*@param seq is a pointer to the MidiShare sequence to be saved.
	*@param info is a MidiFileInfos object and contains informations about the sequence.
	*@exception Exception If the MIDIfile cannot be saved of if a MidiShare error occurs. 	
 	*/
    public final void Save(OutputStream outstream, int seq, MidiFileInfos info) throws Exception {
        SaveAux(seq, info);
        BufferedOutputStream tmp = new BufferedOutputStream(outstream, 1000);
        seekoutput.writeTo(tmp);
        tmp.close();
        Close();
    }

    final void SetLoadDates(MidiFileInfos info, int s) {
        int e;
        if (info.timedef != 0) {
            if ((e = Midi.NewEv(Midi.typeTempo)) != 0) {
                Midi.SetField(e, 0, info.timedef * info.clicks * 2000);
                Midi.SetDate(e, 0);
                Midi.AddSeq(s, e);
                info.clicks = 500;
                info.timedef = 0;
            }
        }
    }

    final void WriteTracks(int seq) throws Exception {
        short i = 0, numPiste = 0;
        if (format == MidiFileInfos.midifile1) {
            NewTrack();
            WriteTempoMap(seq);
            CloseTrack();
            numPiste++;
            i++;
        }
        for (i = 0; i < MidiFileStream.maxTrack; i++) {
            if (trackListe[i] > 0) {
                NewTrack();
                if (format == MidiFileInfos.midifile1) {
                    WriteTrackFormat1(seq, i, numPiste);
                } else {
                    WriteTrackFormat2(seq, i, numPiste);
                }
                numPiste++;
                CloseTrack();
            }
        }
    }

    final void AnalyseSeq(int seq) {
        int ev, type;
        ev = Midi.GetFirstEv(seq);
        if (format == MidiFileInfos.midifile1) {
            while (ev != 0) {
                type = Midi.GetType(ev);
                if (!IsTempoMap(type)) trackListe[Midi.GetRefnum(ev)] = 1;
                if (ev == Midi.GetLink(ev)) {
                    ev = 0;
                } else {
                    ev = Midi.GetLink(ev);
                }
            }
        } else {
            while (ev != 0) {
                trackListe[Midi.GetRefnum(ev)] = 1;
                ev = Midi.GetLink(ev);
            }
        }
    }

    final void WriteTempoMap(int seq) throws Exception {
        int type, ev, lastWrite = 0;
        ev = Midi.GetFirstEv(seq);
        while (ev != 0) {
            type = Midi.GetType(ev);
            if (IsTempoMap(type)) {
                WriteEv(ev);
                lastWrite = ev;
            } else if (Midi.GetRefnum(ev) == 0) {
                WriteEv(ev);
                lastWrite = ev;
            }
            ev = Midi.GetLink(ev);
        }
        if ((lastWrite == 0) || (Midi.GetType(lastWrite) != Midi.typeEndTrack)) WriteEndTrack(lastWrite);
    }

    final void WriteTrackFormat1(int seq, short ref, short numPiste) throws Exception {
        boolean firstName = true;
        int type, ev, lastWrite = 0;
        int port = 0;
        ev = Midi.GetFirstEv(seq);
        while (ev != 0) {
            if (Midi.GetRefnum(ev) == ref) {
                if (Midi.GetPort(ev) != port) {
                    port = Midi.GetPort(ev);
                    WritePortPrefix(lastWrite, port);
                }
                type = Midi.GetType(ev);
                if (!IsTempoMap(type)) {
                    if (firstName && Midi.GetType(ev) == Midi.typeSeqName) {
                        WriteSeqName(ev, ref, numPiste);
                        firstName = false;
                    } else {
                        WriteEv(ev);
                    }
                    lastWrite = ev;
                }
            }
            ev = Midi.GetLink(ev);
        }
        if ((lastWrite == 0) || (Midi.GetType(lastWrite) != Midi.typeEndTrack)) {
            WriteEndTrack(lastWrite);
        }
    }

    final void WriteTrackFormat2(int seq, short ref, short numPiste) throws Exception {
        boolean firstName = true;
        int type, ev, lastWrite = 0;
        int port = 0;
        ev = Midi.GetFirstEv(seq);
        while (ev != 0) {
            if (Midi.GetRefnum(ev) == ref) {
                if (Midi.GetPort(ev) != port) {
                    port = Midi.GetPort(ev);
                    WritePortPrefix(lastWrite, port);
                }
                if (firstName && Midi.GetType(ev) == Midi.typeSeqName) {
                    WriteSeqName(ev, ref, numPiste);
                    firstName = false;
                } else {
                    WriteEv(ev);
                }
                lastWrite = ev;
            }
            ev = Midi.GetLink(ev);
        }
        if ((lastWrite == 0) || (Midi.GetType(lastWrite) != Midi.typeEndTrack)) WriteEndTrack(lastWrite);
    }

    final void WriteSeqName(int ev, short ref, short numPiste) throws Exception {
        int name;
        if (ref == numPiste) {
            WriteEv(ev);
        } else if ((name = Midi.CopyEv(ev)) != 0) {
            Midi.SetText(name, Midi.GetText(name) + " " + MidiFileStream.Player + ref);
            WriteEv(name);
            Midi.FreeEv(name);
        } else throw new IOException("No more MidiShare events");
    }

    final void WriteEndTrack(int prev) throws Exception {
        int ev, seq;
        if ((ev = Midi.NewEv(Midi.typeEndTrack)) != 0) {
            Midi.SetLink(ev, 0);
            if (((seq = keyOff) != 0) && (Midi.GetFirstEv(seq) != 0)) {
                Midi.SetDate(ev, Midi.GetDate(Midi.GetLastEv(seq)));
                Midi.SetLink(Midi.GetLastEv(seq), ev);
                Midi.SetLastEv(seq, ev);
            } else {
                if (prev != 0) {
                    Midi.SetDate(ev, Midi.GetDate(prev));
                    if (Midi.GetType(prev) == Midi.typeNote) Midi.SetDate(ev, Midi.GetDate(ev) + Midi.GetField(prev, 2));
                } else Midi.SetDate(ev, 0);
                WriteEv(ev);
                Midi.FreeEv(ev);
            }
        } else throw new IOException("No more MidiShare events");
    }

    final void WritePortPrefix(int prev, int port) throws Exception {
        int ev;
        int seq;
        if (Midi.GetVersion() < 185) {
            return;
        } else if ((ev = Midi.NewEv(Midi.typePortPrefix)) != 0) {
            Midi.SetLink(ev, 0);
            Midi.SetField(ev, 0, port);
            if (prev != 0) Midi.SetDate(ev, Midi.GetDate(prev)); else Midi.SetDate(ev, 0);
            WriteEv(ev);
            Midi.FreeEv(ev);
        } else throw new IOException("No more MidiShare events");
    }

    final void InitTrackListe() {
        int i;
        for (i = 0; i < MidiFileStream.maxTrack; i++) {
            trackListe[i] = 0;
        }
    }

    final void UseTrack(int seq, int dest, int i) {
        SetSeqRef(seq, GetSeqRef(seq, i));
        SetSeqPort(seq);
        MidiSequence.Mix(seq, dest);
        Midi.SetFirstEv(seq, 0);
        Midi.SetLastEv(seq, 0);
        Midi.FreeSeq(seq);
    }

    final void TryToReadTrack(int dest, int i) throws Exception {
        int seq;
        seq = ReadTrack();
        UseTrack(seq, dest, i);
    }

    final void ReadTracks(int dest) throws Exception {
        int i;
        for (i = 0; i < ntrks; i++) {
            TryToReadTrack(dest, i);
        }
    }

    final void SetSeqRef(int seq, int refNum) {
        int ev;
        ev = Midi.GetFirstEv(seq);
        while (ev != 0) {
            Midi.SetRefnum(ev, refNum);
            ev = Midi.GetLink(ev);
        }
    }

    final void SetSeqPort(int seq) {
        int ev, prev, tmp;
        int port = 0;
        prev = 0;
        ev = Midi.GetFirstEv(seq);
        while (ev != 0) {
            if ((Midi.GetType(ev) == Midi.typePortPrefix) && (Midi.GetLink(ev) != 0)) {
                port = Midi.GetField(ev, 0);
                if (prev != 0) Midi.SetLink(prev, Midi.GetLink(ev)); else Midi.SetFirstEv(seq, Midi.GetLink(ev));
                tmp = Midi.GetLink(ev);
                Midi.FreeEv(ev);
                ev = tmp;
            } else {
                Midi.SetPort(ev, port);
                prev = ev;
                ev = Midi.GetLink(ev);
            }
        }
    }

    final int GetBeginKey(String buff) {
        int i, l = buff.length();
        int len = MidiFileStream.Player.length();
        for (i = 0; i < l; i++) {
            if (buff.regionMatches(i, MidiFileStream.Player, 0, len)) break;
        }
        return i;
    }

    final int RestoreSeqName(String buff) {
        int ev = 0;
        if ((ev = Midi.NewEv(Midi.typeSeqName)) != 0) {
            Midi.SetText(ev, buff.substring(0, GetBeginKey(buff)));
        }
        return ev;
    }

    final int GetTrackName(int seq) {
        int ev, prev = 0;
        ev = Midi.GetFirstEv(seq);
        while (ev != 0) {
            if (Midi.GetType(ev) == Midi.typeSeqName) {
                break;
            }
            prev = ev;
            ev = Midi.GetLink(ev);
        }
        if (prev == 0) {
            return ev;
        } else if (ev != 0) {
            return prev;
        } else return ev;
    }

    final int GetEvRef(String buff, int keyLen, int numPiste) {
        int index;
        String strRef;
        index = GetBeginKey(buff);
        if (index == buff.length()) {
            return numPiste;
        } else {
            return Integer.parseInt(buff.substring(index + keyLen));
        }
    }

    final int GetSeqRef(int seq, int numPiste) {
        int ev, ori, refNum;
        int prec = 0;
        int i, n, l;
        String buff;
        refNum = numPiste;
        if ((ev = GetTrackName(seq)) != 0) {
            if (Midi.GetType(ev) != Midi.typeSeqName) {
                prec = ev;
                ev = Midi.GetLink(ev);
            } else {
                prec = 0;
            }
            n = Midi.CountFields(ev);
            l = MidiFileStream.Player.length();
            if (n < 511) {
                buff = Midi.GetText(ev);
                if (buff.regionMatches(0, MidiFileStream.Player, 0, l) && (prec == 0)) {
                    Midi.SetFirstEv(seq, Midi.GetLink(ev));
                    if (Midi.GetFirstEv(seq) == 0) Midi.SetLastEv(seq, 0);
                    Midi.FreeEv(ev);
                } else if ((ori = RestoreSeqName(buff)) != 0) {
                    Midi.SetDate(ori, Midi.GetDate(ev));
                    Midi.SetLink(ori, Midi.GetLink(ev));
                    if (prec != 0) {
                        Midi.SetLink(prec, ori);
                    } else {
                        Midi.SetFirstEv(seq, ori);
                    }
                    Midi.FreeEv(ev);
                }
                refNum = GetEvRef(buff, l, numPiste);
            }
        }
        return refNum;
    }
}

final class MdfHeader {

    byte id[];

    int len;

    short format;

    short ntrks;

    short time;

    MdfHeader() {
        id = new byte[4];
    }

    final void read(DataInputStream input) throws IOException {
        id = new byte[4];
        input.readFully(id);
        String str = new String(id);
        if (!(str.equals(MidiFileStream.MDF_MThd))) throw new IOException("MidiFile format error");
        len = input.readInt();
        if (len != 6) throw new IOException("MidiFile format error");
        format = input.readShort();
        ntrks = input.readShort();
        time = input.readShort();
    }

    final int write(DataOutputStream output, short tracknum, short format, short time) throws Exception {
        int countpos;
        output.writeBytes(MidiFileStream.MDF_MThd);
        output.writeInt(6);
        output.writeShort(format);
        countpos = output.size();
        output.writeShort(tracknum);
        output.writeShort(time);
        return countpos;
    }
}

final class TrkHeader {

    byte id[];

    int len;

    TrkHeader() {
        id = new byte[4];
    }

    final void read(DataInputStream input) throws IOException {
        input.readFully(id);
        String str = new String(id);
        if (!(str.equals(MidiFileStream.MDF_MTrk))) throw new IOException("MidiFile format error");
        len = input.readInt();
        if (Midi.FreeSpace() < (len / 2)) {
            Midi.GrowSpace(len / 2);
        }
    }

    final int write(DataOutputStream output) throws IOException {
        int lenpos;
        output.writeBytes(MidiFileStream.MDF_MTrk);
        lenpos = output.size();
        output.writeInt(0);
        return lenpos;
    }
}

class MfEvent {

    int read(MidiFileStream mfile, int len, short status) throws Exception {
        return 0;
    }

    int read(MidiFileStream mfile, short status) throws Exception {
        return 0;
    }

    void write(MidiFileStream mfile, int event, short status) throws Exception {
    }
}

final class sysEx extends MfEvent {

    final int read(MidiFileStream mfile, short status) throws Exception {
        int ev1, ev2;
        int len;
        int c = 0;
        ev1 = Midi.NewEv(Midi.typeSysEx);
        ev2 = Midi.NewEv(Midi.typeStream);
        if ((ev1 != 0) && (ev2 != 0)) {
            Midi.AddField(ev2, status);
            len = mfile.ReadVarLen1(ev2);
            while (len-- > 0) {
                c = mfile.input.read();
                mfile._cnt--;
                Midi.AddField(ev2, c);
                if (c != 0xF7) Midi.AddField(ev1, c);
            }
            if (c == 0xF7) {
                Midi.FreeEv(ev2);
                return ev1;
            } else {
                Midi.FreeEv(ev1);
                return ev2;
            }
        } else throw new MidiException("No more MidiShare event");
    }

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        int i, count;
        count = Midi.CountFields(ev);
        mfile.output.write(0xF0);
        mfile.WriteVarLen(count + 1);
        for (i = 0; i < count; i++) {
            mfile.output.write(Midi.GetField(ev, i));
        }
        mfile.output.write(0xF7);
    }
}

final class stream extends MfEvent {

    final int read(MidiFileStream mfile, short status) throws Exception {
        int ev;
        int len;
        int c;
        if ((ev = Midi.NewEv(Midi.typeStream)) != 0) {
            Midi.AddField(ev, status);
            len = mfile.ReadVarLen1(ev);
            while (len-- > 0) {
                c = mfile.input.read();
                mfile._cnt--;
                Midi.AddField(ev, c);
            }
        } else throw new MidiException("No more MidiShare event");
        return ev;
    }

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        int i, count;
        count = Midi.CountFields(ev);
        for (i = 0; i < count; i++) {
            mfile.output.write(Midi.GetField(ev, i));
        }
    }
}

final class Note extends MfEvent {

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        int c;
        if ((c = Midi.CopyEv(ev)) != 0) {
            Midi.SetType(c, Midi.typeKeyOff);
            Midi.SetField(c, 1, 0);
            Midi.SetDate(c, Midi.GetDate(ev) + Midi.GetField(ev, 2));
            Midi.SetField(c, 2, 0);
            Midi.AddSeq(mfile.keyOff, c);
            mfile.output.write(status);
            mfile.output.write(Midi.GetData0(ev));
            mfile.output.write(Midi.GetData1(ev));
        } else {
            throw new MidiException("No more MidiShare event");
        }
    }
}

final class dont_write extends MfEvent {

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
    }
}

final class NRegP extends MfEvent {

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        short num, val;
        num = (short) Midi.GetField(ev, 0);
        val = (short) Midi.GetField(ev, 1);
        mfile.output.write(Midi.ControlChg + Midi.GetChan(ev));
        mfile.write_param(num, val, (short) 99);
    }
}

final class RegP extends MfEvent {

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        short num, val;
        num = (short) Midi.GetField(ev, 0);
        val = (short) Midi.GetField(ev, 1);
        mfile.output.write(Midi.ControlChg + Midi.GetChan(ev));
        mfile.write_param(num, val, (short) 101);
    }
}

final class Ctrl14b extends MfEvent {

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        short num, val;
        num = (short) Midi.GetField(ev, 0);
        val = (short) Midi.GetField(ev, 1);
        mfile.output.write(Midi.ControlChg + Midi.GetChan(ev));
        mfile.output.write(num);
        mfile.output.write(val >> 7);
        mfile.WriteVarLen(0);
        mfile.output.write(num + 32);
        mfile.output.write(val & 0x7F);
    }
}

final class Data2Ev extends MfEvent {

    final int read(MidiFileStream mfile, short status) throws Exception {
        int ev = Midi.NewEv(MidiFileStream.typeTbl[status & 0x7F]);
        if (ev != 0) {
            Midi.SetData0(ev, mfile.input.read());
            Midi.SetData1(ev, mfile.input.read());
            mfile._cnt -= 2;
        } else throw new MidiException("No more MidiShare event");
        return ev;
    }

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        mfile.output.write(status);
        mfile.output.write(Midi.GetData0(ev));
        mfile.output.write(Midi.GetData1(ev));
    }
}

final class Data1Ev extends MfEvent {

    final int read(MidiFileStream mfile, short status) throws Exception {
        int ev = Midi.NewEv(MidiFileStream.typeTbl[status & 0x7F]);
        if (ev != 0) {
            Midi.SetData0(ev, mfile.input.read());
            mfile._cnt--;
        } else throw new MidiException("No more MidiShare event");
        return ev;
    }

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        mfile.output.write(status);
        mfile.output.write(Midi.GetData0(ev));
    }
}

final class Data0Ev extends MfEvent {

    final int read(MidiFileStream mfile, short status) throws Exception {
        int ev = Midi.NewEv(MidiFileStream.typeTbl[status & 0x7F]);
        if (ev == 0) throw new MidiException("No more MidiShare event");
        return ev;
    }

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        mfile.output.write(status);
    }
}

final class text extends MfEvent {

    final int read(MidiFileStream mfile, int len, short type) throws Exception {
        int ev = 0;
        if ((ev = Midi.NewEv(type + 133)) != 0) {
            mfile._cnt -= len;
            while (len-- > 0) Midi.AddField(ev, (int) mfile.input.read());
        } else throw new MidiException("No more MidiShare event");
        return ev;
    }

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        int len, i;
        mfile.output.write(MidiFileStream.META);
        mfile.output.write(status);
        len = Midi.CountFields(ev);
        mfile.WriteVarLen(len);
        for (i = 0; i < len; i++) {
            mfile.output.write((int) Midi.GetField(ev, i));
        }
    }
}

final class endTrack extends MfEvent {

    final int read(MidiFileStream mfile, int len, short unused1) throws Exception {
        int ev = 0;
        if (len != MidiFileStream.MDF_EndTrkLen) {
            return mfile.ReadExtTbl[0].read(mfile, len, (short) 0);
        } else if ((ev = Midi.NewEv(Midi.typeEndTrack)) == 0) {
            throw new MidiException("No more MidiShare event");
        }
        return ev;
    }

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        mfile.output.write(MidiFileStream.META);
        mfile.output.write(MidiFileStream.MDF_EndTrk);
        mfile.output.write(MidiFileStream.MDF_EndTrkLen);
    }
}

final class tempo extends MfEvent {

    final int read(MidiFileStream mfile, int len, short unused1) throws Exception {
        int ev = 0;
        int tempo;
        if (len != MidiFileStream.MDF_TempoLen) {
            return mfile.ReadExtTbl[0].read(mfile, len, (short) 0);
        } else if ((ev = Midi.NewEv(Midi.typeTempo)) != 0) {
            tempo = (int) mfile.input.read();
            tempo <<= 8;
            tempo |= mfile.input.read();
            tempo <<= 8;
            tempo |= mfile.input.read();
            Midi.SetField(ev, 0, tempo);
            mfile._cnt -= len;
        } else throw new MidiException("No more MidiShare event");
        return ev;
    }

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        int l;
        short s;
        mfile.output.write(MidiFileStream.META);
        mfile.output.write(MidiFileStream.MDF_Tempo);
        mfile.output.write(MidiFileStream.MDF_TempoLen);
        l = Midi.GetField(ev, 0);
        s = (short) l;
        mfile.output.write((int) (l >> 16));
        mfile.output.write(s >> 8);
        mfile.output.write(s & 0xFF);
    }
}

final class keySign extends MfEvent {

    final int read(MidiFileStream mfile, int len, short unused1) throws Exception {
        int ev = 0;
        if (len != MidiFileStream.MDF_TonLen) return mfile.ReadExtTbl[0].read(mfile, len, (short) 0); else if ((ev = Midi.NewEv(Midi.typeKeySign)) != 0) {
            Midi.SetField(ev, 0, mfile.input.read());
            Midi.SetField(ev, 1, mfile.input.read());
            mfile._cnt -= len;
        } else throw new MidiException("No more MidiShare event");
        return ev;
    }

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        mfile.output.write(MidiFileStream.META);
        mfile.output.write(MidiFileStream.MDF_Ton);
        mfile.output.write(MidiFileStream.MDF_TonLen);
        mfile.output.write(Midi.GetField(ev, 0));
        mfile.output.write(Midi.GetField(ev, 1));
    }
}

final class timeSign extends MfEvent {

    final int read(MidiFileStream mfile, int len, short unused1) throws Exception {
        int ev = 0;
        if (len != MidiFileStream.MDF_MeasLen) return mfile.ReadExtTbl[0].read(mfile, len, (short) 0); else if ((ev = Midi.NewEv(Midi.typeTimeSign)) != 0) {
            Midi.SetField(ev, 0, mfile.input.read());
            Midi.SetField(ev, 1, mfile.input.read());
            Midi.SetField(ev, 2, mfile.input.read());
            Midi.SetField(ev, 3, mfile.input.read());
            mfile._cnt -= len;
        } else throw new MidiException("No more MidiShare event");
        return ev;
    }

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        mfile.output.write(MidiFileStream.META);
        mfile.output.write(MidiFileStream.MDF_Meas);
        mfile.output.write(MidiFileStream.MDF_MeasLen);
        mfile.output.write(Midi.GetField(ev, 0));
        mfile.output.write(Midi.GetField(ev, 1));
        mfile.output.write(Midi.GetField(ev, 2));
        mfile.output.write(Midi.GetField(ev, 3));
    }
}

final class seqNum extends MfEvent {

    final int read(MidiFileStream mfile, int len, short unused1) throws Exception {
        int ev = 0;
        int num;
        if (len != MidiFileStream.MDF_NumSeqLen) return mfile.ReadExtTbl[0].read(mfile, len, (short) 0); else if ((ev = Midi.NewEv(Midi.typeSeqNum)) != 0) {
            num = mfile.input.read();
            num <<= 8;
            num |= mfile.input.read();
            Midi.SetField(ev, 0, num);
            mfile._cnt -= len;
        } else throw new MidiException("No more MidiShare event");
        return ev;
    }

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        short s;
        mfile.output.write(MidiFileStream.META);
        mfile.output.write(MidiFileStream.MDF_NumSeq);
        mfile.output.write(MidiFileStream.MDF_NumSeqLen);
        s = (short) Midi.GetField(ev, 0);
        mfile.output.write(s >> 8);
        mfile.output.write(s & 0xF);
    }
}

final class chanPref extends MfEvent {

    final int read(MidiFileStream mfile, int len, short unused1) throws Exception {
        int ev = 0;
        if (len != MidiFileStream.MDF_ChanPrefLen) return mfile.ReadExtTbl[0].read(mfile, len, (short) 0); else if ((ev = Midi.NewEv(Midi.typeChanPrefix)) != 0) {
            Midi.SetField(ev, 0, mfile.input.read());
            mfile._cnt -= len;
        } else throw new MidiException("No more MidiShare event");
        return ev;
    }

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        mfile.output.write(MidiFileStream.META);
        mfile.output.write(MidiFileStream.MDF_ChanPref);
        mfile.output.write(MidiFileStream.MDF_ChanPrefLen);
        mfile.output.write(Midi.GetField(ev, 0));
    }
}

final class portPref extends MfEvent {

    final int read(MidiFileStream mfile, int len, short unused1) throws Exception {
        int ev = 0;
        if ((Midi.GetVersion() < 185) || (len != MidiFileStream.MDF_PortPrefLen)) return mfile.ReadExtTbl[0].read(mfile, len, (short) 0); else if ((ev = Midi.NewEv(Midi.typePortPrefix)) != 0) {
            Midi.SetField(ev, 0, mfile.input.read());
            mfile._cnt -= len;
        } else throw new MidiException("No more MidiShare event");
        return ev;
    }

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        mfile.output.write(MidiFileStream.META);
        mfile.output.write(MidiFileStream.MDF_PortPref);
        mfile.output.write(MidiFileStream.MDF_PortPrefLen);
        mfile.output.write(Midi.GetField(ev, 0));
    }
}

final class smpte extends MfEvent {

    final int read(MidiFileStream mfile, int len, short unused1) throws Exception {
        int ev = 0;
        int tmp;
        if (len != MidiFileStream.MDF_OffsetLen) return mfile.ReadExtTbl[0].read(mfile, len, (short) 0); else if ((ev = Midi.NewEv(Midi.typeSMPTEOffset)) != 0) {
            tmp = mfile.input.read() * 3600;
            tmp = tmp + mfile.input.read() * 30;
            tmp += mfile.input.read();
            Midi.AddField(ev, tmp);
            tmp = mfile.input.read() * 100;
            tmp += mfile.input.read();
            Midi.AddField(ev, tmp);
            mfile._cnt -= len;
        } else throw new MidiException("No more MidiShare event");
        return ev;
    }

    final void write(MidiFileStream mfile, int ev, short status) throws Exception {
        int l;
        mfile.output.write(MidiFileStream.META);
        mfile.output.write(MidiFileStream.MDF_Offset);
        mfile.output.write(MidiFileStream.MDF_OffsetLen);
        l = (int) Midi.GetField(ev, 0);
        mfile.output.write(l / 3600);
        l %= 3600;
        mfile.output.write(l / 60);
        mfile.output.write(l % 60);
        l = (int) Midi.GetField(ev, 1);
        mfile.output.write(l / 100);
        mfile.output.write(l % 100);
    }
}

final class undef extends MfEvent {

    final int read(MidiFileStream mfile, short status) throws Exception {
        throw new IOException("Midifile error unknow");
    }

    final void write(MidiFileStream input, int event, short status) throws Exception {
    }
}

final class ignoreEv extends MfEvent {

    final int read(MidiFileStream mfile, int len, short status) throws Exception {
        mfile._cnt -= len;
        while (len-- > 0) {
            mfile.input.read();
        }
        return 0;
    }
}
