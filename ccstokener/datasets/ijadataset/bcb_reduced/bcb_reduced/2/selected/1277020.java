package org.chemicalcovers.script;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.chemicalcovers.model.CoverInfo;
import org.chemicalcovers.model.ICoverList;
import org.chemicalcovers.search.ISearchEngine;
import org.chemicalcovers.utils.URLExtractor;

public class CoverIsland implements ISearchEngine {

    public String getName() {
        return "Albulm Art Exchange";
    }

    public String getDescription() {
        return getName() + " Search Engine Plugin";
    }

    public void run(String artistName, String albumTitle, ICoverList coverList) throws Exception {
        char firstLetter;
        if (artistName.length() > 0) firstLetter = artistName.charAt(0); else if (albumTitle.length() > 0) firstLetter = albumTitle.charAt(0); else {
            return;
        }
        if (Character.isLetter(firstLetter)) {
        } else if (Character.isDigit(firstLetter)) {
            firstLetter = '0';
        } else {
            return;
        }
        URLExtractor urlExtractor = new URLExtractor();
        URLExtractor imageExtractor = new URLExtractor();
        String artistNameLowerCase = artistName.toLowerCase();
        String albumTitleLowerCase = albumTitle.toLowerCase();
        for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
            StringBuilder buffer = urlExtractor.getPage("http://www.coverisland.com/copertine/Audio/" + firstLetter + pageNumber + ".asp");
            if (buffer == null) break;
            String albumsPage = buffer.toString();
            Pattern albumPattern = Pattern.compile("<option value=\"([^\"]+)\">([^<]+)<");
            Matcher albumsForFirstLetterMatcher = albumPattern.matcher(albumsPage);
            while (albumsForFirstLetterMatcher.find()) {
                String albumOptionValue = albumsForFirstLetterMatcher.group(1).toLowerCase();
                String albumText = albumsForFirstLetterMatcher.group(2).toLowerCase();
                if (albumText.indexOf(artistNameLowerCase) != -1 && albumText.indexOf(albumTitleLowerCase) != -1) {
                    int typePostOption;
                    int signPostOption;
                    String titlePostOption;
                    if (albumOptionValue.startsWith("-")) {
                        typePostOption = Integer.parseInt(albumOptionValue.substring(1, 3).trim());
                        signPostOption = 1;
                        titlePostOption = albumOptionValue.substring(3);
                    } else {
                        typePostOption = Integer.parseInt(albumOptionValue.substring(0, 2).trim());
                        signPostOption = 1;
                        titlePostOption = albumOptionValue.substring(2);
                    }
                    ArrayList<String> imageTypes = new ArrayList<String>();
                    if ((typePostOption & 0x1) == 0x1) imageTypes.add("front");
                    if ((typePostOption & 0x2) == 0x2) imageTypes.add("back");
                    if ((typePostOption & 0x4) == 0x4) imageTypes.add("inside");
                    if ((typePostOption & 0x8) == 0x8) imageTypes.add("inlay");
                    if ((typePostOption & 0x10) == 0x10) imageTypes.add("cd");
                    if ((typePostOption & 0x20) == 0x20) imageTypes.add("cd2");
                    for (int j = 0, size = imageTypes.size(); j < size; j++) {
                        imageExtractor.postPage("http://www.coverisland.com/copertine/down.asp", "tipologia=Audio&title=" + titlePostOption + "&type=-" + imageTypes.get(j) + "&segno=" + signPostOption);
                        ArrayList<String> imageUrl = imageExtractor.extract("http://www.coverforum.net/view.php\\?image=" + titlePostOption + "&ty=Audio&([^']+)");
                        System.out.println("CoverIsland.run() '" + imageUrl.get(0) + "'");
                        if (imageUrl.size() == 1) {
                            System.out.println("CoverIsland.run()2 " + imageExtractor.getPage(imageUrl.get(0)));
                            CoverInfo coverInfo = new CoverInfo();
                            try {
                                coverInfo.setCoverURL(imageUrl.get(0));
                                coverInfo.setAlbum(albumText);
                            } catch (Exception e) {
                                System.err.println(e);
                                e.printStackTrace();
                            }
                            coverList.add(coverInfo);
                        }
                    }
                }
            }
        }
        System.out.println("CoverIsland.run() done");
    }

    @SuppressWarnings("unused")
    private String post(String url, String content) {
        try {
            HttpURLConnection httpConn = (HttpURLConnection) new URL(url).openConnection();
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);
            httpConn.setUseCaches(false);
            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            DataOutputStream outputStream = new DataOutputStream(httpConn.getOutputStream());
            outputStream.writeBytes(content);
            outputStream.close();
            BufferedReader inputStream = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            StringBuilder buffer = new StringBuilder();
            for (String str = inputStream.readLine(); str != null; str = inputStream.readLine()) {
                buffer.append(str);
            }
            inputStream.close();
            return buffer.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
