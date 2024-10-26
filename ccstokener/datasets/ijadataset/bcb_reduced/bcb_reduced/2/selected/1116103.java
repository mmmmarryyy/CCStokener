package resultviewer;

import java.awt.Color;
import java.awt.Graphics;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import resultviewer.xml.classes.ClassResult;
import resultviewer.xml.classes.CountryId;
import resultviewer.xml.classes.Nationality;
import resultviewer.xml.classes.PersonName;
import resultviewer.xml.classes.PersonResult;
import resultviewer.xml.classes.RaceResult;
import resultviewer.xml.classes.ResultList;

public class ResultBuilderOLA {

    private Graphics graphic;

    private int resultTimePositionOne = 0;

    private String timeOfLastResultModified = "";

    private String dateOfLastResultModified = "";

    private ResultBuilderParameters params;

    public ResultBuilderOLA(ResultBuilderParameters newParams, Graphics g) {
        params = newParams;
        graphic = g;
    }

    private ClassResult getClassResultFromResultList(ResultList resultList) {
        List<ClassResult> classResultList = resultList.getClassResult();
        for (ClassResult classResult : classResultList) {
            if (classResult.getClassShortName().getContent().equals(params.className)) {
                return classResult;
            }
        }
        return null;
    }

    private ClassResult getClassResult() {
        JAXBContext jc = null;
        Unmarshaller u = null;
        ClassResult classResult = null;
        try {
            jc = JAXBContext.newInstance("resultviewer.xml.classes");
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        try {
            u = jc.createUnmarshaller();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        try {
            if (params.url.startsWith("http")) {
                Object res = u.unmarshal(sendGetRequest(params.url));
                if (res instanceof ClassResult) {
                    classResult = (ClassResult) res;
                } else {
                    classResult = getClassResultFromResultList((ResultList) res);
                }
            } else {
                Object res = u.unmarshal(new FileInputStream(params.url));
                if (res instanceof ClassResult) {
                    classResult = (ClassResult) res;
                } else {
                    classResult = getClassResultFromResultList((ResultList) res);
                }
            }
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("Current working directory is: " + System.getProperty("user.dir"));
        }
        return classResult;
    }

    @SuppressWarnings("unchecked")
    public ResultArea getResult(ResultArea areaClass) {
        areaClass.clear();
        if (areaClass instanceof TimeWindow) {
            ((TimeWindow) areaClass).setTimeLength(params.timeWindowDelay);
        }
        ResultArea areaFixedResults = new ResultArea(graphic);
        areaFixedResults.setHeightLayout(LayoutTypes.FitContent);
        if (params.showClassHeader) {
            ResultClassHeader rch = new ResultClassHeader(params.title);
            areaFixedResults.addComponent(rch);
            areaFixedResults.addComponent(new NewLineMark());
        }
        ResultArea areaSlidingResults = new ResultArea(graphic);
        areaSlidingResults.setYStepSize(params.yStepSize);
        ClassResult classResult = getClassResult();
        int sequenceNo = 1;
        Object resObject = null;
        if (classResult != null) {
            resObject = classResult.getPersonResultOrTeamResult();
        } else {
            areaFixedResults.addComponent(new ResultStrip("-", "Class not found", "", "", "", "", "", false, false, false, false, new Color(0, 0, 0), "", new Color(255, 255, 255)));
            areaFixedResults.addComponent(new NewLineMark());
        }
        String latestLastModifiedDate = "";
        String latestLastModifiedTime = "";
        if (resObject instanceof ArrayList) {
            ArrayList<PersonResult> personResults = (ArrayList<PersonResult>) resObject;
            for (PersonResult personResult : personResults) {
                String position = "";
                String status = getCompetitorStatus(personResult);
                boolean runnerStatusOK = status.equalsIgnoreCase("OK");
                if (runnerStatusOK) {
                    position = getResultPosition(personResult);
                }
                if (position.equals("1")) {
                    resultTimePositionOne = getTimeAsSeconds(getTimeFormat(personResult), getTime(personResult));
                }
                String time = "";
                String timeDiff = "";
                if (runnerStatusOK && params.showTime) {
                    time = getTime(personResult);
                }
                if (runnerStatusOK && params.showDiffTime) {
                    String timeFormat = getTimeFormat(personResult);
                    int timeDiffSec = (getTimeAsSeconds(timeFormat, time) - resultTimePositionOne);
                    timeDiff = "(+" + getTimeAsString("MM:SS", timeDiffSec) + ")";
                }
                if (!runnerStatusOK) {
                    timeDiff = status;
                }
                String timeTwo = "";
                if (params.useMainResult) {
                    RaceResult res = getRaceResult(personResult);
                    if (res != null) {
                        timeTwo = getRaceTime(res);
                    } else {
                        timeTwo = "";
                    }
                } else {
                    timeTwo = getMainTime(personResult);
                }
                String nationality = getPersonNationality(personResult);
                if (nationality == null || nationality.isEmpty()) {
                    nationality = getClubNationality(personResult);
                }
                boolean isNewResult = false;
                String timeModified = getTimeLastModified(personResult);
                String dateModified = getDateLastModified(personResult);
                if (dateModified.compareTo(latestLastModifiedDate) > 0 || (dateModified.compareTo(latestLastModifiedDate) >= 0 && timeModified.compareTo(latestLastModifiedTime) > 0)) {
                    latestLastModifiedDate = dateModified;
                    latestLastModifiedTime = timeModified;
                }
                if (dateModified.compareTo(dateOfLastResultModified) > 0 || (dateModified.compareTo(dateOfLastResultModified) >= 0 && timeModified.compareTo(timeOfLastResultModified) > 0)) {
                    isNewResult = true;
                }
                String clubName = params.useShortClubName ? getShortClubName(personResult) : getClubName(personResult);
                ResultStrip rs = new ResultStrip(params.showPosition ? position : "", getPersonName(personResult), clubName, time, timeDiff, nationality, timeTwo, params.showFlagBeforePerson, params.showFlagBeforeClub, params.showTotalTime, isNewResult, params.newResultColor, params.dontPrintFlagOfCountry, params.newBackgroundResultColor);
                if (sequenceNo <= params.fixedResultsUpToPosition) {
                    areaFixedResults.addComponent(rs);
                    areaFixedResults.addComponent(new NewLineMark());
                    if (sequenceNo == params.fixedResultsUpToPosition) {
                        LineComponent line = new LineComponent();
                        line.setHeightValue(3);
                        areaFixedResults.addComponent(line);
                        areaFixedResults.addComponent(new NewLineMark());
                    } else {
                        if (sequenceNo % params.printLineEveryXLine == 0) {
                            areaFixedResults.addComponent(new LineComponent());
                            areaFixedResults.addComponent(new NewLineMark());
                        }
                    }
                } else {
                    areaSlidingResults.addComponent(rs);
                    areaSlidingResults.addComponent(new NewLineMark());
                    if (sequenceNo % params.printLineEveryXLine == 0) {
                        areaSlidingResults.addComponent(new LineComponent());
                        areaSlidingResults.addComponent(new NewLineMark());
                    }
                }
                if (sequenceNo == params.printNumberOfPersons) {
                    break;
                }
                sequenceNo++;
            }
            LineComponent line = new LineComponent();
            line.setHeightValue(3);
            areaSlidingResults.addComponent(line);
            areaSlidingResults.addComponent(new NewLineMark());
            areaSlidingResults.addComponent(new ResultStrip("", "", "", "", "", "", "", false, false, false, false, new Color(0, 0, 0), "", new Color(255, 255, 255)));
            areaSlidingResults.addComponent(new NewLineMark());
            LineComponent line2 = new LineComponent();
            line2.setHeightValue(3);
            areaSlidingResults.addComponent(line2);
            areaSlidingResults.addComponent(new NewLineMark());
        }
        timeOfLastResultModified = latestLastModifiedTime;
        dateOfLastResultModified = latestLastModifiedDate;
        areaClass.addComponent(areaFixedResults);
        areaClass.addComponent(new NewLineMark());
        areaClass.addComponent(areaSlidingResults);
        areaClass.addComponent(new NewLineMark());
        areaClass.addComponent(new NewColumnMark());
        return areaClass;
    }

    /**
         * Sends an HTTP GET request to a url
         * 
         * @param urlString -
         *                The URL of the server. (Example: "
         *                http://www.yahoo.com/search")
         * @return - a bufferedReader
         */
    public static BufferedReader sendGetRequest(String urlString) {
        BufferedReader rd = null;
        if (urlString.startsWith("http://")) {
            do {
                try {
                    URL url = new URL(urlString);
                    URLConnection conn = url.openConnection();
                    rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), Charset.forName("UTF-8")));
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ex) {
                    }
                    ;
                    rd = null;
                }
            } while (rd == null);
        }
        return rd;
    }

    private String getPersonName(PersonResult personResult) {
        try {
            PersonName personName = personResult.getPerson().getPersonName();
            String familyName = personName.getFamily().getContent();
            String firstGiven = personName.getGiven().get(0).getContent();
            return firstGiven + " " + familyName;
        } catch (Exception e) {
            System.err.println("Error getting name");
            e.printStackTrace();
            return "Error getting name";
        }
    }

    private String getClubName(PersonResult personResult) {
        try {
            return personResult.getClub().getName().getContent();
        } catch (Exception e) {
            System.err.println("Error getting club name");
            e.printStackTrace();
            return "Error getting club name";
        }
    }

    private String getShortClubName(PersonResult personResult) {
        try {
            return personResult.getClub().getShortName().getContent();
        } catch (Exception e) {
            System.err.println("Error getting club name");
            e.printStackTrace();
            return "Error getting club name";
        }
    }

    private String getClubNationality(PersonResult personResult) {
        try {
            CountryId theClubCountry = personResult.getClub().getCountryId();
            if (theClubCountry == null) {
                return "";
            } else {
                return theClubCountry.getValue();
            }
        } catch (Exception e) {
            System.err.println("Error getting club nationality");
            e.printStackTrace();
            return "Error getting club nationality";
        }
    }

    private String getPersonNationality(PersonResult personResult) {
        try {
            Nationality thePersonNationality = personResult.getPerson().getNationality();
            if (thePersonNationality == null) {
                return "";
            } else {
                return thePersonNationality.getCountryId().getValue();
            }
        } catch (Exception e) {
            System.err.println("Error getting person nationality");
            e.printStackTrace();
            return "Error getting person nationality";
        }
    }

    private String getTime(PersonResult personResult) {
        try {
            RaceResult res = getRaceResult(personResult);
            if (res == null || params.useMainResult) {
                return getMainTime(personResult);
            } else {
                return getRaceTime(res);
            }
        } catch (Exception e) {
            System.err.println("Error getting time");
            e.printStackTrace();
            return "Error getting time";
        }
    }

    private String getMainTime(PersonResult personResult) {
        try {
            if (personResult.getResult().getTime() == null) {
                return "";
            } else {
                return personResult.getResult().getTime().getContent();
            }
        } catch (Exception e) {
            System.err.println("Error getting main time");
            e.printStackTrace();
            return "Error getting main time";
        }
    }

    private String getRaceTime(RaceResult raceResult) {
        try {
            return raceResult.getResult().getTime().getContent();
        } catch (Exception e) {
            System.err.println("Error getting race time");
            e.printStackTrace();
            return "Error getting race time";
        }
    }

    private String getTimeFormat(PersonResult personResult) {
        try {
            return personResult.getResult().getTime().getTimeFormat();
        } catch (Exception e) {
            System.err.println("Error getting time format");
            e.printStackTrace();
            return "Error getting time format";
        }
    }

    private int getTimeAsSeconds(String timeFormat, String time) {
        if (timeFormat.equals("HH:MM:SS")) {
            String[] timeArray = time.split(":");
            int seconds = 0;
            if (timeArray.length == 3) {
                Integer.parseInt(timeArray[2]);
                seconds += 60 * Integer.parseInt(timeArray[1]);
                seconds += 3600 * Integer.parseInt(timeArray[0]);
            }
            return seconds;
        } else if (timeFormat.equals("MM:SS")) {
            String[] timeArray = time.split(":");
            int seconds = 0;
            if (timeArray.length == 2) {
                seconds = Integer.parseInt(timeArray[1]);
                seconds += 60 * Integer.parseInt(timeArray[0]);
            }
            return seconds;
        }
        return -1;
    }

    private String right(String theString, int noOfCharacters) {
        return theString.substring(theString.length() - noOfCharacters, theString.length());
    }

    private String getTimeAsString(String timeFormat, int seconds) {
        if (timeFormat.equals("HH:MM:SS")) {
            int hours = seconds / 3600;
            int minutes = (seconds - 60 * hours) / 60;
            seconds = seconds - 3600 * hours - 60 * minutes;
            String time = right("0" + hours, 2) + ":" + right("0" + minutes, 2) + ":" + right("0" + seconds, 2);
            return time;
        } else if (timeFormat.equals("MM:SS")) {
            int minutes = seconds / 60;
            seconds = seconds - 60 * minutes;
            String time = minutes + ":" + right("0" + seconds, 2);
            return time;
        }
        return "";
    }

    private String getResultPosition(PersonResult personResult) {
        try {
            RaceResult res = getRaceResult(personResult);
            if (res == null || params.useMainResult) {
                return getMainResultPosition(personResult);
            } else {
                return getRaceResultPosition(res);
            }
        } catch (Exception e) {
            System.err.println("Error getting result position");
            e.printStackTrace();
            return "Error getting result position";
        }
    }

    private String getMainResultPosition(PersonResult personResult) {
        try {
            return personResult.getResult().getResultPosition().getContent();
        } catch (Exception e) {
            System.err.println("Error getting main result position");
            e.printStackTrace();
            return "Error getting main result position";
        }
    }

    private String getRaceResultPosition(RaceResult raceResult) {
        try {
            return raceResult.getResult().getResultPosition().getContent();
        } catch (Exception e) {
            System.err.println("Error getting race result position");
            e.printStackTrace();
            return "Error getting race result position";
        }
    }

    private String getCompetitorStatus(PersonResult personResult) {
        try {
            RaceResult res = getRaceResult(personResult);
            if (res == null || params.useMainResult) {
                return getMainCompetitorStatus(personResult);
            } else {
                return getRaceCompetitorStatus(res);
            }
        } catch (Exception e) {
            System.err.println("Error getting competitor status");
            e.printStackTrace();
            return "Error getting competitor status";
        }
    }

    private String getMainCompetitorStatus(PersonResult personResult) {
        try {
            return personResult.getResult().getCompetitorStatus().getValue();
        } catch (Exception e) {
            System.err.println("Error getting competitor main status");
            e.printStackTrace();
            return "Error getting competitor main status";
        }
    }

    private String getRaceCompetitorStatus(RaceResult raceResult) {
        try {
            return raceResult.getResult().getCompetitorStatus().getValue();
        } catch (Exception e) {
            System.err.println("Error getting competitor race status");
            e.printStackTrace();
            return "Error getting race competitor race status";
        }
    }

    private RaceResult getRaceResult(PersonResult personResult) {
        try {
            List<RaceResult> results = personResult.getRaceResult();
            for (RaceResult raceResult : results) {
                if (raceResult.getEventRaceId().getContent().equalsIgnoreCase(params.eventRaceId)) {
                    return raceResult;
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error getting raceresult");
            e.printStackTrace();
            return null;
        }
    }

    private String getTimeLastModified(PersonResult personResult) {
        try {
            RaceResult res = getRaceResult(personResult);
            if (res == null || params.useMainResult) {
                return getMainTimeLastModified(personResult);
            } else {
                return getRaceTimeLastModified(res);
            }
        } catch (Exception e) {
            System.err.println("Error getting competitor time last modified");
            e.printStackTrace();
            return "Error getting competitor time last modified";
        }
    }

    private String getMainTimeLastModified(PersonResult personResult) {
        try {
            return personResult.getResult().getModifyDate().getClock().getContent();
        } catch (Exception e) {
            System.err.println("Error getting competitor main time last modified");
            e.printStackTrace();
            return "Error getting competitor main time last modified";
        }
    }

    private String getRaceTimeLastModified(RaceResult raceResult) {
        try {
            return raceResult.getResult().getModifyDate().getClock().getContent();
        } catch (Exception e) {
            System.err.println("Error getting competitor race time last modified");
            e.printStackTrace();
            return "Error getting race competitor race time last modified";
        }
    }

    private String getDateLastModified(PersonResult personResult) {
        try {
            RaceResult res = getRaceResult(personResult);
            if (res == null) {
                return getMainDateLastModified(personResult);
            } else {
                return getRaceDateLastModified(res);
            }
        } catch (Exception e) {
            System.err.println("Error getting competitor date last modified");
            e.printStackTrace();
            return "Error getting competitor date last modified";
        }
    }

    private String getMainDateLastModified(PersonResult personResult) {
        try {
            return personResult.getResult().getModifyDate().getDate().getContent();
        } catch (Exception e) {
            System.err.println("Error getting competitor main date last modified");
            e.printStackTrace();
            return "Error getting competitor main date last modified";
        }
    }

    private String getRaceDateLastModified(RaceResult raceResult) {
        try {
            return raceResult.getResult().getModifyDate().getDate().getContent();
        } catch (Exception e) {
            System.err.println("Error getting competitor race date last modified");
            e.printStackTrace();
            return "Error getting race competitor race date last modified";
        }
    }
}
