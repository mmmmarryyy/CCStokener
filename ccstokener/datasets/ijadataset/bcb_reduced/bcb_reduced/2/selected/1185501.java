package org.leo.oglexplorer.model.engine;

import java.awt.Container;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.leo.oglexplorer.model.result.SearchResult;
import org.leo.oglexplorer.model.result.SearchType;
import org.leo.oglexplorer.resources.APIKeyNotFoundException;
import org.leo.oglexplorer.ui.task.CancelMonitor;
import org.leo.oglexplorer.util.CustomRunnable;

/**
 * SearchEngine $Id: SearchEngine.java 147 2011-08-16 08:06:22Z leolewis $
 * 
 * <pre>
 * Abstract Search Engine, that provide generic mechanism of research, and need to be implemented by 
 * specific search procedures.
 * Basically we keep all the results in memory (a list of URL in the end), but we'll dispose the thumbnail
 * regularly (because this it what take a lot of memory).
 * </pre>
 * 
 * @author Leo Lewis
 */
public abstract class SearchEngine {

    /** Criteria of the current search request */
    private String _words;

    /** Current search results */
    private List<SearchResult> _searchResults;

    /** Current index */
    private volatile int _index;

    /** Current search type */
    private SearchType _currentSearchType;

    /** Used to equilibrate the next/previous algo */
    private boolean _previousWasNext;

    /** Number of entries searched */
    protected int _count;

    /** Number of results returned */
    protected int _size;

    /**
	 * Constructor
	 * 
	 * @param parentPanel parent panel
	 */
    public SearchEngine() {
        _currentSearchType = SearchType.WEB;
        _searchResults = new CopyOnWriteArrayList<SearchResult>();
    }

    /**
	 * Search Images
	 * 
	 * @param words words
	 * @param number number of results
	 * @param offset search offset
	 * @param type type of search
	 * @param cancelMonitor cancel monitor
	 * @return List of results
	 */
    protected abstract List<? extends SearchResult> searchImpl(String words, int number, int offset, SearchType type, CancelMonitor cancelMonitor) throws APIKeyNotFoundException;

    /**
	 * Name of the engine search
	 * 
	 * @return name
	 */
    public abstract String getName();

    /**
	 * Available search types
	 * 
	 * @return array of search types
	 */
    public abstract SearchType[] availableSearchTypes();

    /**
	 * Get additional component used by this search engine
	 * 
	 * @return the extra component (default is null)
	 */
    public JComponent additionalSearchComponent(JPanel parent) {
        return null;
    }

    /**
	 * Add some custom components that will be visible in the ConfigDialog
	 * 
	 * @return the code to be run when apply button is pressed (default is null)
	 */
    public CustomRunnable configPanel(Container parent) {
        return null;
    }

    /**
	 * The logo of the search engine
	 * 
	 * @return the Icon
	 */
    public abstract Icon getLogo();

    /**
	 * Initialize a search with the given words
	 * 
	 * @param words the criteria
	 */
    public void initSearch(String words) {
        synchronized (this) {
            if (_searchResults != null) {
                final List<SearchResult> toDispose = new ArrayList<SearchResult>(_searchResults);
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        for (SearchResult result : toDispose) {
                            result.dispose();
                        }
                    }
                }).start();
                _searchResults.clear();
            }
            _index = 0;
            try {
                _words = URLEncoder.encode(words, "UTF-8");
            } catch (Exception e) {
                _words = words;
            }
            _previousWasNext = true;
            _count = 10;
        }
    }

    /**
	 * Populate a search with a given number of elements
	 * 
	 * @param count the number of elements
	 * @param cancelMonitor monitor used by the algorithm to see if its needs to
	 *            stop because the user canceled the process
	 * @return results of the search
	 * @throws APIKeyNotFoundException
	 */
    public List<? extends SearchResult> populateSearch(int count, CancelMonitor cancelMonitor) throws APIKeyNotFoundException {
        SearchType searchType;
        synchronized (_currentSearchType) {
            searchType = _currentSearchType;
        }
        synchronized (this) {
            if (_words == null || searchType == null) {
                return null;
            }
            _count = count;
            List<? extends SearchResult> results = searchImpl(_words, count, _searchResults.size(), searchType, cancelMonitor);
            if (results == null) {
                return null;
            }
            _searchResults.addAll(results);
            _index += count;
            return results;
        }
    }

    /**
	 * Get the next result (relative to the current cursor value) of the current
	 * search
	 * 
	 * @param dimension dimension of the display component
	 * @return the previous result
	 * @throws APIKeyNotFoundException
	 */
    public SearchResult next(CancelMonitor cancelMonitor) throws APIKeyNotFoundException {
        SearchType searchType;
        synchronized (_currentSearchType) {
            searchType = _currentSearchType;
        }
        synchronized (this) {
            if (_words == null) {
                return null;
            }
            if (!_previousWasNext && _index > 0) {
                _index++;
                _previousWasNext = true;
            }
            if (_index > _searchResults.size() - 5) {
                List<? extends SearchResult> newPage = searchImpl(_words, _count, _searchResults.size(), searchType, cancelMonitor);
                if (newPage != null && !newPage.isEmpty()) {
                    _searchResults.addAll(newPage);
                }
            }
            if (_index > 2 * _count) {
                _searchResults.get(_index - 2 * _count).dispose();
            }
            if (_index < _searchResults.size()) {
                return _searchResults.get(_index++);
            }
            return null;
        }
    }

    /**
	 * Get the previous result (relative to the current cursor value) of the
	 * current search
	 * 
	 * @param dimension dimension of the display component
	 * @return the previous result
	 */
    public SearchResult previous(CancelMonitor cancelMonitor) {
        synchronized (this) {
            if (_words == null) {
                return null;
            }
            if (_index - _count <= 0) {
                return null;
            }
            if (_index + 3 * _count < _searchResults.size()) {
                _searchResults.get(_searchResults.size() - 1).dispose();
            }
            if (_index - _count - 1 > 0) {
                _searchResults.get(_index - _count - 1).preload();
            }
            return _searchResults.get((--_index) - _count);
        }
    }

    /**
	 * Dispose the data source
	 */
    public void dispose() {
        _searchResults.clear();
    }

    /**
	 * Set the value of the field currentSearchType
	 * 
	 * @param currentSearchType the new currentSearchType to set
	 */
    public void setCurrentSearchType(SearchType currentSearchType) {
        synchronized (_currentSearchType) {
            _currentSearchType = currentSearchType;
        }
    }

    /**
	 * Return the value of the field currentSearchType
	 * 
	 * @return the value of currentSearchType
	 */
    public SearchType getCurrentSearchType() {
        synchronized (_currentSearchType) {
            return _currentSearchType;
        }
    }

    /**
	 * Call the given URL and get the String result
	 * 
	 * @param url the URL
	 * @return the String result
	 * @throws IOException
	 */
    public static String call(String url) throws IOException {
        BufferedReader bis = null;
        InputStream is = null;
        try {
            URLConnection connection = new URL(url).openConnection();
            is = connection.getInputStream();
            bis = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line = null;
            StringBuffer result = new StringBuffer();
            while ((line = bis.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
