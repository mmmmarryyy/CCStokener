package ru.ksu.niimm.cll.mocassin.crawl.parser.gate.util;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import ru.ksu.niimm.cll.mocassin.util.IOUtil;
import com.google.common.base.Predicate;

public class StopWordLoaderImpl implements StopWordLoader {

    private static final String STOP_LIST_FILENAME = "stop_list.properties";

    private final Set<String> stopWords;

    private final NonStopWordPredicate nonStopWordPredicate = new NonStopWordPredicate();

    public StopWordLoaderImpl() throws IOException {
        ClassLoader loader = StopWordLoaderImpl.class.getClassLoader();
        URL url = loader.getResource(STOP_LIST_FILENAME);
        this.stopWords = IOUtil.readLineSet(url.openStream());
    }

    @Override
    public Set<String> getStopWords() {
        return this.stopWords;
    }

    public NonStopWordPredicate getNonStopWordPredicate() {
        return nonStopWordPredicate;
    }

    @Override
    public boolean isStopWord(String word) {
        return !nonStopWordPredicate.apply(word);
    }

    public class NonStopWordPredicate implements Predicate<String> {

        @Override
        public boolean apply(String input) {
            return !getStopWords().contains(input.toLowerCase());
        }
    }
}
