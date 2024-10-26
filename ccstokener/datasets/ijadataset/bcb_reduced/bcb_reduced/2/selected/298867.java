package net.sf.metaprint2d.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.sf.metaprint2d.Fingerprint;
import net.sf.metaprint2d.FingerprintData;
import net.sf.metaprint2d.Fingerprint.FPLevel;
import net.sf.metaprint2d.data.BinFile.BinDataHandler;

public class BinFileDataSource implements FingerprintDataSource {

    private URL url;

    ;

    public BinFileDataSource(URL url) {
        if (url == null) {
            throw new NullPointerException("Null url");
        }
        this.url = url;
    }

    public BinFileDataSource(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("Null file");
        }
        if (!file.isFile()) {
            throw new FileNotFoundException("File not found: " + file);
        }
        this.url = file.toURL();
    }

    public Map<Fingerprint, List<FingerprintData>> getFingerprintData(List<Fingerprint> fps, int matchLevels) {
        final HashSet<FPLevel> filter = new HashSet<FPLevel>();
        for (Fingerprint fp : fps) {
            for (int i = 0; i < matchLevels; i++) {
                filter.add(fp.getLevel(i));
            }
        }
        final int x = matchLevels;
        final FPLevel[] last = new FPLevel[x];
        final ArrayList<FingerprintData> list = new ArrayList<FingerprintData>();
        try {
            BinFile.parse(url.openStream(), new BinDataHandler() {

                boolean match;

                @Override
                public void numFingerprints(int numFingerprints) {
                }

                @Override
                public void entry(FPLevel[] levels, int rcount, int scount) {
                    for (int i = x - 1; i >= 0; i--) {
                        if (levels[i] == last[i]) {
                            continue;
                        }
                        last[i] = levels[i];
                        if (!filter.contains(levels[i])) {
                            match = false;
                            break;
                        } else {
                            match = true;
                        }
                    }
                    if (match) {
                        list.add(new FingerprintData(0, levels, rcount, scount));
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        MemoryDataSource memdata = new MemoryDataSource(list);
        return memdata.getFingerprintData(fps, matchLevels);
    }
}
