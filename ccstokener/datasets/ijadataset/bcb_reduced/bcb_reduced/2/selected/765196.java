package com.androidapp.seniorproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import com.androidapp.seniorproject.UnivClassEvent;
import com.androidapp.seniorproject.UniversityClass;
import android.util.Log;

public class ElearningPortal {

    private static final String LOG_CONNECTION = "CONNECTION";

    private static final String LOG_LOGINPROCESS = "LOGIN_PROCESS";

    private static final int LOGIN_ATTEMPTS = 5;

    private String username = "";

    private String password = "";

    public boolean getData() {
        try {
            HttpClient client = null;
            client = this.LoginToElearning(0, client);
            if (client == null) {
                Log.i(LOG_CONNECTION, "Problem logging into eLearning");
                return false;
            }
            HttpGet request5 = new HttpGet("https://elearning.utdallas.edu/webct/urw/lc5122011.tp0/viewCalendar.dowebct");
            Log.i(LOG_CONNECTION, "-- about to make call to open the calendar --");
            HttpResponse response5 = client.execute(request5);
            HttpEntity entity5 = response5.getEntity();
            if (entity5 == null) {
                Log.i(LOG_CONNECTION, "error on fifth page response");
                return false;
            }
            String sHTML1 = "";
            sHTML1 = this.parseHtmlFromEntity(entity5);
            int nIndex = sHTML1.indexOf("Key:");
            if (nIndex < 0 && sHTML1.indexOf("<title>Calendar</title>") > 0) {
                Log.i(LOG_CONNECTION, "error on fifth page response - 2");
                return false;
            }
            Log.i(LOG_CONNECTION, "-- successfully got to the calendar --");
            ArrayList<UniversityClass> listClasses = new ArrayList<UniversityClass>();
            listClasses = this.parseUnivClassesFromText(nIndex, sHTML1);
            String sTextOut = "";
            UniversityDataStorage uds = UniversityDataStorage.getSingleton();
            uds.onCreate(uds.getWritableDatabase());
            for (UniversityClass c : listClasses) {
                sTextOut += String.format("prefix: %s num: %s sec: %s sem: %s title: %s\n", c.Prefix, c.ClassNumber, c.ClassSection, c.Semester, c.Title);
                c.UniversityClassId = uds.SaveUniversityClass(c);
            }
            ArrayList<UnivClassEvent> listEvents = new ArrayList<UnivClassEvent>();
            listEvents = parseEventsFromCalendar(listClasses, sHTML1);
            for (UnivClassEvent uce : listEvents) {
                uce.UnivClassEventId = uds.SaveUnivClassEvent(uce);
            }
            ArrayList<BasicNameValuePair> listParams = new ArrayList<BasicNameValuePair>();
            listParams = this.parseNameValueFromText(0, "startMonth", ">", "selected=\"true\" value=\"", "\"", sHTML1, 1);
            int nMonth = Integer.parseInt(listParams.get(0).getValue());
            nMonth = (++nMonth == 13) ? 1 : nMonth;
            Log.d("CALENDAR", "next Month:  calendar:" + listParams.get(0).getValue() + "  nextValue: " + nMonth);
            listParams = this.parseNameValueFromText(0, "startYear", ">", "selected=\"true\" value=\"", "\"", sHTML1, 1);
            int nYear = Integer.parseInt(listParams.get(0).getValue());
            nYear = (nMonth == 1) ? nYear + 1 : nYear;
            Log.d("CALENDAR", "next Year: calendar:" + listParams.get(0).getValue() + "   nextValue:" + nYear);
            listParams = new ArrayList<BasicNameValuePair>();
            listParams.add(new BasicNameValuePair("startHideDay", "1"));
            listParams.add(new BasicNameValuePair("startAllowBlankSelection", "false"));
            listParams.add(new BasicNameValuePair("startAllowBlankTimeSelection", "false"));
            listParams.add(new BasicNameValuePair("startMonth", String.valueOf(nMonth)));
            listParams.add(new BasicNameValuePair("startYear", String.valueOf(nYear)));
            listParams.add(new BasicNameValuePair("start", nYear + "/" + nMonth + "/1 8:30"));
            for (int i = 0; i < listParams.size(); i++) Log.d("CALENDAR", "name: " + listParams.get(i).getName() + "  value: " + listParams.get(i).getValue());
            HttpPost request6 = new HttpPost("https://elearning.utdallas.edu/webct/urw/lc5122011.tp0/viewMonth.dowebct");
            request6.setEntity(new UrlEncodedFormEntity(listParams, HTTP.UTF_8));
            Log.i(LOG_CONNECTION, "--- about to make call to open a different month on the calendar ---");
            HttpResponse response6 = client.execute(request6);
            HttpEntity entity6 = response6.getEntity();
            if (entity6 == null) {
                Log.i(LOG_CONNECTION, "error on opening different month on calendar");
                return false;
            }
            sHTML1 = this.parseHtmlFromEntity(entity6);
            listEvents = new ArrayList<UnivClassEvent>();
            listEvents = parseEventsFromCalendar(listClasses, sHTML1);
            for (UnivClassEvent uce : listEvents) {
                uce.UnivClassEventId = uds.SaveUnivClassEvent(uce);
            }
            uds.updateLastSync();
            this.LogoutElearning(client);
            return true;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            Log.e("GETDATA", "getData() - client protocol exception: " + e.getMessage());
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("GETDATA", "getData() - IO exception: " + e.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("GETDATA", "getData() - general exception: " + e.getMessage());
            return false;
        }
    }

    private HttpClient LoginToElearning(int nAttemptedLogins, HttpClient client) {
        try {
            if (nAttemptedLogins % 3 == 0) {
                Thread.currentThread();
                Thread.sleep(2000);
            }
            client = new DefaultHttpClient();
            HttpGet request1 = new HttpGet("https://elearning.utdallas.edu/webct/entryPageIns.dowebct");
            Log.i(LOG_CONNECTION, "-- about to execute first request to eLearning --");
            HttpResponse response1 = client.execute(request1);
            HttpEntity entity1 = response1.getEntity();
            if (entity1 == null) {
                Log.e(LOG_LOGINPROCESS, "error in connecting.. 1");
                if (nAttemptedLogins < LOGIN_ATTEMPTS) return LoginToElearning(++nAttemptedLogins, client); else {
                    this.LogoutElearning(client);
                    return null;
                }
            }
            String sHTML1 = this.parseHtmlFromEntity(entity1);
            if (sHTML1.length() == 0) {
                Log.e(LOG_LOGINPROCESS, "error parsing HTML1");
                if (nAttemptedLogins < LOGIN_ATTEMPTS) return LoginToElearning(++nAttemptedLogins, client); else {
                    this.LogoutElearning(client);
                    return null;
                }
            }
            ArrayList<BasicNameValuePair> listParams1 = null;
            int nIndex = sHTML1.indexOf("Academic");
            if (nIndex > 0) {
                listParams1 = this.parseNameValueFromText(nIndex, "name =\"", "\"", "value =\"", "\"", sHTML1, 3);
            }
            if (listParams1 == null) {
                Log.e(LOG_LOGINPROCESS, "error parsing parameters");
                if (nAttemptedLogins < LOGIN_ATTEMPTS) return LoginToElearning(++nAttemptedLogins, client); else {
                    this.LogoutElearning(client);
                    return null;
                }
            }
            for (int i = 0; i < listParams1.size(); i++) Log.d(LOG_CONNECTION, "R1: name: " + listParams1.get(i).getName() + "  value: " + listParams1.get(i).getValue());
            response1 = null;
            sHTML1 = "";
            Log.i(LOG_CONNECTION, " -- About to make second call -- ");
            HttpPost httpost = new HttpPost("https://elearning.utdallas.edu/webct/entryPage.dowebct");
            httpost.setEntity(new UrlEncodedFormEntity(listParams1, HTTP.UTF_8));
            HttpResponse response2 = client.execute(httpost);
            HttpEntity entity2 = response2.getEntity();
            if (entity2 == null) {
                Log.e(LOG_LOGINPROCESS, "error on second page response");
                if (nAttemptedLogins < LOGIN_ATTEMPTS) return LoginToElearning(++nAttemptedLogins, client); else {
                    this.LogoutElearning(client);
                    return null;
                }
            }
            Log.i(LOG_CONNECTION, "-- about to parse response2 to get parameters --");
            sHTML1 = this.parseHtmlFromEntity(entity2);
            listParams1 = new ArrayList<BasicNameValuePair>();
            nIndex = sHTML1.indexOf("function submitLogin()");
            if (nIndex > 0) {
                listParams1 = this.parseNameValueFromText(nIndex, "document.vistaInsEntryForm.", ".", "value = \"", "\"", sHTML1, 4);
            } else {
                Log.e(LOG_LOGINPROCESS, "error making it to the second page, couldn't find the submitLogin func on the html loaded");
                if (nAttemptedLogins < LOGIN_ATTEMPTS) return LoginToElearning(++nAttemptedLogins, client); else {
                    this.LogoutElearning(client);
                    return null;
                }
            }
            if (listParams1 == null) {
                Log.e(LOG_LOGINPROCESS, "error parsing params from second page for third page");
                if (nAttemptedLogins < LOGIN_ATTEMPTS) return LoginToElearning(++nAttemptedLogins, client); else {
                    this.LogoutElearning(client);
                    return null;
                }
            }
            for (int i = 0; i < listParams1.size(); i++) Log.d(LOG_CONNECTION, "R2: name: " + listParams1.get(i).getName() + "  value: " + listParams1.get(i).getValue());
            HttpPost httpost3 = new HttpPost("https://elearning.utdallas.edu/webct/logonDisplay.dowebct");
            Log.i(LOG_CONNECTION, " -- About to make third call -- ");
            httpost3.setEntity(new UrlEncodedFormEntity(listParams1, HTTP.UTF_8));
            HttpResponse response3 = client.execute(httpost3);
            HttpEntity entity3 = response3.getEntity();
            if (entity3 == null) {
                Log.e(LOG_LOGINPROCESS, "error on third page response");
                if (nAttemptedLogins < LOGIN_ATTEMPTS) return LoginToElearning(++nAttemptedLogins, client); else {
                    this.LogoutElearning(client);
                    return null;
                }
            }
            nIndex = this.parseHtmlFromEntity(entity3).indexOf("<title>Log in to eLearning</title>");
            if (nIndex < 0) {
                Log.e(LOG_LOGINPROCESS, "error on third page response - 2");
                if (nAttemptedLogins < LOGIN_ATTEMPTS) return LoginToElearning(++nAttemptedLogins, client); else {
                    this.LogoutElearning(client);
                    return null;
                }
            }
            listParams1.add(new BasicNameValuePair("gotoid", "null"));
            listParams1.add(new BasicNameValuePair("timeZoneOffset", "6"));
            listParams1.add(new BasicNameValuePair("webctid", getUsername()));
            listParams1.add(new BasicNameValuePair("password", getPassword()));
            for (int i = 0; i < listParams1.size(); i++) Log.d(LOG_CONNECTION, "R3: name: " + listParams1.get(i).getName() + "  value: " + listParams1.get(i).getValue());
            Log.i(LOG_CONNECTION, " -- About to make fourth call -- actual login with user/pass --  ");
            HttpPost httpost4 = new HttpPost("https://elearning.utdallas.edu/webct/authenticateUser.dowebct");
            httpost4.setEntity(new UrlEncodedFormEntity(listParams1, HTTP.UTF_8));
            HttpResponse response4 = client.execute(httpost4);
            HttpEntity entity4 = response4.getEntity();
            if (entity4 == null) {
                Log.e(LOG_LOGINPROCESS, "error on fourth page response");
                if (nAttemptedLogins < LOGIN_ATTEMPTS) return LoginToElearning(++nAttemptedLogins, client); else {
                    this.LogoutElearning(client);
                    return null;
                }
            }
            nIndex = this.parseHtmlFromEntity(entity4).indexOf("<title>Blackboard Learning System</title>");
            if (nIndex < 0) {
                Log.e(LOG_LOGINPROCESS, "error on fourth page response - 2");
                if (nAttemptedLogins < LOGIN_ATTEMPTS) return LoginToElearning(++nAttemptedLogins, client); else {
                    this.LogoutElearning(client);
                    return null;
                }
            }
            Log.i(LOG_CONNECTION, "-- successfully logged in to eLearning --");
        } catch (Exception ex) {
            Log.e(LOG_CONNECTION, "LoginToElearning error: " + ex.getMessage());
            return null;
        }
        return client;
    }

    private boolean LogoutElearning(HttpClient client) {
        boolean bSuccess = false;
        try {
            HttpGet requestLO = new HttpGet("https://elearning.utdallas.edu/webct/urw/lc5122011.tp0/logout.dowebct?insId=5122011&insName=Academic&glcid=URN:X-WEBCT-VISTA-V1:ffc0dca0-0ab4-0f47-0134-42f173422d1b");
            Log.i(LOG_CONNECTION, "-- logging out of eLearning --");
            client.execute(requestLO);
            client.getConnectionManager().closeIdleConnections(1, TimeUnit.MILLISECONDS);
            bSuccess = true;
        } catch (Exception ex) {
            Log.e(LOG_LOGINPROCESS, "Error logging out: " + ex.getMessage());
        }
        return bSuccess;
    }

    private ArrayList<BasicNameValuePair> parseNameValueFromText(int nIndex, String sNameTemplate, String sNameEnd, String sValueTemplate, String sValueEnd, String sTextToParse, int nIterations) {
        ArrayList<BasicNameValuePair> listNVP = null;
        try {
            listNVP = new ArrayList<BasicNameValuePair>();
            int nCounter = 0;
            while (nCounter < nIterations) {
                nIndex = sTextToParse.indexOf(sNameTemplate, nIndex);
                nIndex += sNameTemplate.length();
                int nIndexEnd = sTextToParse.indexOf(sNameEnd, nIndex);
                String sName = sTextToParse.substring(nIndex, nIndexEnd);
                if (sName.equalsIgnoreCase("glcid1")) sName = "glcid"; else if (sName.equalsIgnoreCase("insId1")) sName = "insId"; else if (sName.equalsIgnoreCase("insName1")) sName = "insName";
                nIndex = sTextToParse.indexOf(sValueTemplate, nIndexEnd);
                nIndex += sValueTemplate.length();
                nIndexEnd = sTextToParse.indexOf(sValueEnd, nIndex);
                String sValue = sTextToParse.substring(nIndex, nIndexEnd);
                listNVP.add(new BasicNameValuePair(sName, sValue));
                nCounter++;
            }
        } catch (Exception ex) {
            listNVP = null;
        }
        return listNVP;
    }

    private ArrayList<UniversityClass> parseUnivClassesFromText(int nIndex, String sTextToParse) {
        ArrayList<UniversityClass> listClasses = null;
        try {
            listClasses = new ArrayList<UniversityClass>();
            UniversityClass univClass = new UniversityClass();
            nIndex = sTextToParse.indexOf("alt=\"", nIndex);
            while (nIndex > 0) {
                univClass = new UniversityClass();
                nIndex = sTextToParse.indexOf("alt=\"", nIndex);
                nIndex += "alt=\"".length();
                int nIndexEnd = sTextToParse.indexOf("\"", nIndex);
                String sWholeValue = sTextToParse.substring(nIndex, nIndexEnd);
                String[] sValueArray = sWholeValue.split(" ");
                if (sValueArray.length >= 6) {
                    univClass.Prefix = sValueArray[0];
                    univClass.ClassNumber = sValueArray[1];
                    univClass.ClassSection = "." + sValueArray[2].replace(":", "");
                    String sTitle = "";
                    int i = 3;
                    while (!sValueArray[i].equals("-")) sTitle += sValueArray[i++] + " ";
                    univClass.Title = sTitle;
                    univClass.Semester = sValueArray[++i];
                    listClasses.add(univClass);
                } else {
                    Log.e("PARSE_CLASSES", "error parsing class string: " + sWholeValue);
                }
                nIndex = sTextToParse.indexOf("alt=\"", nIndex);
            }
        } catch (Exception ex) {
            Log.e("PARSE_CLASSES", ex.getMessage());
        }
        return listClasses;
    }

    private String parseHtmlFromEntity(HttpEntity entity) {
        String html = "";
        try {
            InputStream in = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder str = new StringBuilder();
            String line = null;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 1000) {
                str.append(line);
                count++;
            }
            in.close();
            html = str.toString();
        } catch (Exception ex) {
            Log.e("PARSE_HTML_ENTITY", ex.getMessage());
            html = "";
        }
        return html;
    }

    private ArrayList<UnivClassEvent> parseEventsFromCalendar(ArrayList<UniversityClass> listClasses, String sHTML) {
        ArrayList<UnivClassEvent> listEvents = new ArrayList<UnivClassEvent>();
        sHTML = sHTML.substring(0, sHTML.indexOf("Key:"));
        for (UniversityClass u : listClasses) {
            int start = sHTML.indexOf(u.Prefix + " " + u.ClassNumber + " " + u.ClassSection.replace(".", ""));
            while (start > 0) {
                try {
                    start = sHTML.indexOf("/>", start);
                    start += 2;
                    int end = sHTML.indexOf("</li>", start);
                    int nDateIndex = start - 330;
                    nDateIndex = sHTML.indexOf("viewDay.dowebct?", nDateIndex);
                    if (nDateIndex > 0) {
                        nDateIndex = sHTML.indexOf("month=", nDateIndex) + 6;
                        String sMonth = sHTML.substring(nDateIndex, sHTML.indexOf("&", nDateIndex));
                        sMonth = String.valueOf(Integer.parseInt(sMonth));
                        nDateIndex = sHTML.indexOf("day=", nDateIndex) + 4;
                        String sDay = sHTML.substring(nDateIndex, sHTML.indexOf("&", nDateIndex));
                        nDateIndex = sHTML.indexOf("year=", nDateIndex) + 5;
                        String sYear = sHTML.substring(nDateIndex, sHTML.indexOf("\"", nDateIndex));
                        int i = sHTML.indexOf("<a", start);
                        if (i > 0 && i < end) {
                            start = sHTML.indexOf(">", i);
                            start++;
                            end = sHTML.indexOf("</a", start);
                        }
                        String eventName = sHTML.substring(start, end).trim();
                        Log.i("PARSE_EVENT", u.Prefix + " " + u.ClassNumber + " " + u.ClassSection + " found event: " + eventName + " on " + sMonth + "/" + sDay + "/" + sYear);
                        UnivClassEvent uce = new UnivClassEvent();
                        Date dueDate = new Date();
                        dueDate.setDate(Integer.parseInt(sDay));
                        dueDate.setMonth(Integer.parseInt(sMonth));
                        dueDate.setYear(Integer.parseInt(sYear));
                        uce.dueDate = dueDate;
                        uce.name = eventName;
                        uce.ParentUnivClassId = u.UniversityClassId;
                        listEvents.add(uce);
                    } else {
                        Log.e("PARSE_EVENT", "Couldn't find/parse the date for: " + u.Prefix + " " + u.ClassNumber + " " + u.ClassSection);
                    }
                    start = sHTML.indexOf(u.Prefix + " " + u.ClassNumber + " " + u.ClassSection, start);
                } catch (Exception e) {
                    Log.e("PARSE_EVENT", "Error parsing event for class " + u.Prefix + " " + u.ClassNumber + " " + u.ClassSection + ": " + e.getMessage());
                    start = -1;
                }
            }
        }
        return listEvents;
    }

    private String getUsername() {
        if (this.username.length() > 0) return this.username; else {
            UniversityDataStorage uds = UniversityDataStorage.getSingleton();
            ArrayList<String> listUP = uds.GetUserPass();
            this.username = listUP.get(0);
            this.password = listUP.get(1);
            return this.username;
        }
    }

    private String getPassword() {
        return this.password;
    }
}
