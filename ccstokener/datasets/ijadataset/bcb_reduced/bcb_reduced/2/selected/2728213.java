/** 
 * OGL Explorer
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.leo.oglexplorer.model.engine.impl;

import java.awt.Container;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.leo.oglexplorer.model.engine.SearchEngine;
import org.leo.oglexplorer.model.result.SearchResult;
import org.leo.oglexplorer.model.result.SearchType;
import org.leo.oglexplorer.model.result.impl.BookSearchResult;
import org.leo.oglexplorer.model.result.impl.VideoSearchResult;
import org.leo.oglexplorer.resources.APIKeyNotFoundException;
import org.leo.oglexplorer.resources.ConfigManager;
import org.leo.oglexplorer.resources.Resources;
import org.leo.oglexplorer.ui.task.CancelMonitor;
import org.leo.oglexplorer.ui.util.HyperlinkLabel;
import org.leo.oglexplorer.util.CustomRunnable;

import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.json.JsonCParser;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.Key;
import com.google.api.services.books.v1.Books;
import com.google.api.services.books.v1.model.Volume;
import com.google.api.services.books.v1.model.VolumeVolumeInfo;
import com.google.api.services.books.v1.model.Volumes;

/**
 * GoogleSearchEngine $Id: GoogleSearchEngine.java 16 2011-06-05 05:58:32Z
 * leolewis $
 * 
 * <pre>
 * Google search engine implementation
 * </pre>
 * 
 * @author Leo Lewis
 */
public class GoogleSearchEngine extends SearchEngine {

	/** Json Factory */
	final static JsonFactory JSON_FACTORY = new JacksonFactory();

	/**
	 * Constructor
	 */
	public GoogleSearchEngine() {
		super();
	}

	public static class VideoFeed {
		@Key List<Video> items;
	}

	public static class Video {
		@Key String title;
		@Key String description;
		@Key Player player;
		@Key Thumbnail thumbnail;
		@Key String[] tags;
	}

	public static class Player {
		@Key("default") String defaultUrl;
	}

	public static class Thumbnail {
		@Key("sqDefault") String lowThumbnailURL;
		@Key("hqDefault") String highThumbnailURL;
	}

	public static class YouTubeUrl extends GenericUrl {
		@Key final String alt = "jsonc";
		@Key String author;
		@Key("q") String words;
		@Key("max-results") Integer maxResults;
		@Key("start-index") Integer startIndex;

		YouTubeUrl(String url) {
			super(url);
		}
	}

	/**
	 * Search books
	 * 
	 * @param words criteria
	 * @param number number of results
	 * @param offset offset
	 * @param cancelMonitor monitor used by the algorithm to see if its needs to
	 *            stop because the user canceled the process
	 * @return page containing the results
	 * @throws APIKeyNotFoundException 
	 */
	protected List<? extends SearchResult> searchBook(String words, int offset, CancelMonitor cancelMonitor) throws APIKeyNotFoundException {
		List<BookSearchResult> resultsList = new ArrayList<>();
		String api = "";
		try {
			api = ConfigManager.getConfigParam("google.api.key");
		} catch (IOException e) {
			throw new APIKeyNotFoundException(getName());
		}
		if (api == null || "".equals(api)) {
			throw new APIKeyNotFoundException(getName());
		}
		try {
			// set page size to 40, this is the maximum authorized by google
			_count = 40;
			// Set up Books client.
			final Books books = new Books(new NetHttpTransport(), JSON_FACTORY);
			books.setApplicationName("OGLExplorer/1.0");
			books.accessKey = api;
			Books.Volumes.List volumesList = books.volumes.list(words);
			volumesList.maxResults = _count;
			volumesList.startIndex = offset;

			// Execute the query.
			Volumes volumes = volumesList.execute();
			if (volumes.totalItems == 0 || volumes.items == null) {
				return resultsList;
			}

			for (int index = 0; index < volumes.items.size() && !cancelMonitor.isCanceled(); index++) {
				Volume volume = volumes.items.get(index);
				BookSearchResult modelResult = new BookSearchResult(index + 1);
				VolumeVolumeInfo volumeInfo = volume.volumeInfo;

				modelResult.setTitle(volumeInfo.title);
				if (volumeInfo.authors != null) {
					modelResult.getAuthors().addAll(volumeInfo.authors);
				}
				modelResult.setDescription(volumeInfo.description);

				if (volumeInfo.ratingsCount != null && volumeInfo.ratingsCount > 0) {
					modelResult.setRating((int) Math.round(volumeInfo.averageRating.doubleValue()));
					modelResult.setRatingCount(volumeInfo.ratingsCount);
				}
				modelResult.setPath(volumeInfo.infoLink);
				resultsList.add(modelResult);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (cancelMonitor.isCanceled()) {
			return null;
		}
		return resultsList;
	}

	/**
	 * Search video
	 * 
	 * @param words criteria
	 * @param number number of results
	 * @param offset offset
	 * @param cancelMonitor monitor used by the algorithm to see if its needs to
	 *            stop because the user canceled the process
	 * @return page containing the results
	 */
	protected List<? extends SearchResult> searchVideo(String words, int number, int offset, CancelMonitor cancelMonitor) {
		List<VideoSearchResult> resultsList = new ArrayList<>();
		try {
			// set up the HTTP request factory
			HttpTransport transport = new NetHttpTransport();
			HttpRequestFactory factory = transport.createRequestFactory(new HttpRequestInitializer() {

				@Override
				public void initialize(HttpRequest request) {
					// set the parser
					JsonCParser parser = new JsonCParser();
					parser.jsonFactory = JSON_FACTORY;
					request.addParser(parser);
					// set up the Google headers
					GoogleHeaders headers = new GoogleHeaders();
					headers.setApplicationName("OGLExplorer/1.0");
					headers.gdataVersion = "2";
					request.headers = headers;
				}
			});
			// build the YouTube URL
			YouTubeUrl url = new YouTubeUrl("https://gdata.youtube.com/feeds/api/videos");
			url.maxResults = number;
			url.words = words;
			url.startIndex = offset + 1;
			// build
			HttpRequest request = factory.buildGetRequest(url);
			// execute
			HttpResponse response = request.execute();
			VideoFeed feed = response.parseAs(VideoFeed.class);
			if (feed.items == null) {
				return null;
			}
			// browse result and convert them to the local generic object model
			for (int i = 0; i < feed.items.size() && !cancelMonitor.isCanceled(); i++) {
				Video result = feed.items.get(i);
				VideoSearchResult modelResult = new VideoSearchResult(offset + i + 1);
				modelResult.setTitle(result.title);
				modelResult.setDescription(result.description);
				modelResult.setThumbnailURL(new URL(result.thumbnail.lowThumbnailURL));
				modelResult.setPath(result.player.defaultUrl);
				resultsList.add(modelResult);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (cancelMonitor.isCanceled()) {
			return null;
		}
		return resultsList;
	}

	/**
	 * @throws APIKeyNotFoundException 
	 * @see org.leo.oglexplorer.model.engine.impl.SearchEngine#searchImpl(java.lang.String,
	 *      int, int, org.leo.oglexplorer.model.result.SearchType)
	 */
	@Override
	protected List<? extends SearchResult> searchImpl(String words, int number, int offset, SearchType type, CancelMonitor cancelMonitor) throws APIKeyNotFoundException {
		switch (type) {
		case VIDEO:
			return searchVideo(words, number, offset, cancelMonitor);
		case BOOK:
			return searchBook(words, offset, cancelMonitor);
		default:
			throw new UnsupportedOperationException("Search type : " + type.getName()
					+ " is not implemented for the engine : " + getName());
		}
	}

	/**
	 * @see org.leo.oglexplorer.model.engine.impl.SearchEngine#getName()
	 */
	@Override
	public String getName() {
		return Resources.getLabel("engine.google");
	}

	/**
	 * @see org.leo.oglexplorer.model.engine.impl.SearchEngine#availableSearchTypes()
	 */
	@Override
	public SearchType[] availableSearchTypes() {
		return new SearchType[] { SearchType.VIDEO, SearchType.BOOK };
	}

	/**
	 * @see org.leo.oglexplorer.model.engine.impl.SearchEngine#getLogo()
	 */
	@Override
	public Icon getLogo() {
		return Resources.getImageIcon("google.png");
	}
	
	/**
	 * @see org.leo.oglexplorer.model.engine.SearchEngine#configPanel(java.awt.Container)
	 */
	@Override
	public CustomRunnable configPanel(Container parent) {
		JPanel keyPanel = new JPanel();
		JLabel label = new JLabel(Resources.getLabel("google.api.key"), Resources.getImageIcon("google_small.png"), JLabel.CENTER);
		final JTextField textField = new JTextField(15);
		try {
			textField.setText(ConfigManager.getConfigParam("google.api.key"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		keyPanel.add(label);
		keyPanel.add(textField);
		try {
			keyPanel.add(new HyperlinkLabel(Resources.getLabel("get.key", Resources.getLabel("google.api.key")), new URL(
					Resources.getLabel("google.api.key.url"))));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		parent.add(keyPanel);
		// code to execute on apply config action
		return new CustomRunnable() {
			@Override
			public void run() throws Exception {
				ConfigManager.setConfigParam("google.api.key", textField.getText());
			}
		};
	}
}
