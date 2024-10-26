package net.sourceforge.pinyinlookup.engine.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.GZIPInputStream;
import net.sourceforge.pinyinlookup.dict.Dict;
import net.sourceforge.pinyinlookup.dict.DictList;
import net.sourceforge.pinyinlookup.dict.cedict.CeDictLoader;
import net.sourceforge.pinyinlookup.engine.IPhoneticEngine;
import net.sourceforge.pinyinlookup.pinyin.PinyinSequence;
import net.sourceforge.pinyinlookup.pinyin.PinyinString;

/**
 * Pinyin engine class.
 * Parses and convert pinyin strings.
 * 
 * @author Vincent Petry <PVince81@users.sourceforge.net>
 */
public class PinyinEngine implements IPhoneticEngine {

    private DictList dictList;

    private PinyinConverter sentenceConverter;

    private PinyinParser parser;

    private PinyinMap pinyinMap;

    public PinyinEngine(String dataPath, String mapFile, String[] dictFiles, boolean useToneSandhi) throws IOException {
        dictList = new DictList();
        if (dictFiles.length == 0) {
            throw new IllegalArgumentException("No dictionary files specified");
        }
        loadDefaultDicts(dataPath, dictFiles);
        sentenceConverter = new PinyinConverter(dictList, useToneSandhi);
        pinyinMap = new PinyinMap(dataPath + "/" + mapFile);
        parser = new PinyinParser(pinyinMap);
    }

    public PinyinSequence convert(String text) {
        return sentenceConverter.convert(text);
    }

    public PinyinSequence groupWords(PinyinSequence sequence) {
        return sentenceConverter.groupWords(sequence);
    }

    public PinyinString parse(String hanzi, String pinyin) {
        return parser.parse(hanzi, pinyin);
    }

    /**
	 * Load default dictionaries.
	 * @param dictPath dictionary path
	 * @throws IOException 
	 * @throws URISyntaxException 
	 */
    public void loadDefaultDicts(String dictPath, String[] dictNames) throws IOException {
        for (int i = 0; i < dictNames.length; i++) {
            loadDict(dictPath + "/" + dictNames[i]);
        }
    }

    /**
	 * Loading dictionary file name.
	 * @param dictFile
	 * @throws IOException
	 */
    public void loadDict(String dictFile) throws IOException {
        if (!dictFile.startsWith("file:/")) {
            loadDict((new File(dictFile)).toURI());
        } else {
            loadDict(URI.create(dictFile));
        }
    }

    /**
	 * Load dictionary.
	 * @param dictUrl dictionary URL
	 * @throws IOException
	 */
    public void loadDict(URI url) throws IOException {
        InputStream stream = null;
        try {
            stream = url.toURL().openStream();
            if (stream != null) {
                stream = new GZIPInputStream(stream);
            }
            Dict dict = (new CeDictLoader(stream)).load();
            dictList.addDict(dict);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    public PinyinMap getPinyinMap() {
        return pinyinMap;
    }
}
