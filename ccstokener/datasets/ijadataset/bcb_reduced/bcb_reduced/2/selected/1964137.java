package org.cyberaide.transcript;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import javax.net.ssl.SSLSocketFactory;

public class IBMApi {

    private static String url;

    private static final String login = "login?";

    private static final String addJob = "addJob?";

    private static URL ibmUrl;

    private static URLConnection urlCon;

    private static User loggedinUsr, job;

    static {
        try {
            String configureDirectory = Thread.currentThread().getContextClassLoader().getResource("").getFile();
            String urlTxt = configureDirectory + "../../etc/configuration.properties";
            Scanner sc = new Scanner(new File(urlTxt));
            url = sc.nextLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void login(String... arg) throws IOException {
        try {
            String uri = url + login + "username=" + arg[0] + "&password=" + arg[1];
            getConnection(uri, "Login.txt");
            loggedinUsr = IBMDigester.parse("Login.txt");
        } catch (Exception e) {
            System.out.println("error in IBMApi->login");
        }
    }

    public static void main(String arg[]) {
        try {
            Scanner sc = new Scanner(System.in);
            System.out.print("enter userName:");
            String username = sc.next();
            System.out.println();
            System.out.print("enter pass:");
            String password = sc.next();
            System.out.println();
            url = url + "username=" + username + "&password=" + password;
            URL urlCon = new URL(url);
            URLConnection conn = urlCon.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuffer sb = new StringBuffer();
            File f = new File("Login.txt");
            BufferedWriter bwr = new BufferedWriter(new FileWriter(f));
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
                bwr.write(line + "\n");
            }
            rd.close();
            bwr.close();
            IBMDigester.parse("Login.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addJob(String optionValue) throws Exception {
        try {
            String uri = url + addJob + "userid=" + loggedinUsr.getRetval() + "&url=" + optionValue;
            getConnection(uri, "Job.txt");
            job = (User) IBMDigester.parse("Job.txt");
        } catch (Exception e) {
            System.out.println("erron in IBMApi->addJob");
        }
    }

    public static void getJob(String jobid) {
        try {
            String uri = url + "getJob?userid=" + loggedinUsr.getRetval() + "&jobid=" + jobid;
            getConnection(uri, "JobStatus.txt");
        } catch (Exception e) {
            System.out.println("erron in IBMApi->addJob");
        }
    }

    public static void getJobs() {
        try {
            String uri = url + "getJobs?userid=" + loggedinUsr.getRetval();
            getConnection(uri, "JobList.txt");
        } catch (Exception e) {
            System.out.println("erron in IBMApi->addJob");
        }
    }

    public static void getConnection(String query, String fileName) throws Exception {
        ibmUrl = new URL(query);
        urlCon = ibmUrl.openConnection();
        BufferedReader rd = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));
        StringBuffer sb = new StringBuffer();
        File f = new File(fileName);
        BufferedWriter bwr = new BufferedWriter(new FileWriter(f));
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
            bwr.write(line + "\n");
        }
        rd.close();
        bwr.close();
    }

    public static void removeJob(String optionValue) {
        try {
            String uri = url + "removeJob?userid=" + loggedinUsr.getRetval() + "&jobid=" + optionValue;
            getConnection(uri, "RemoveJob.txt");
        } catch (Exception e) {
            System.out.println("erron in IBMApi->addJob");
        }
    }

    public static void getTranscript(String... args) {
        try {
            String uri = url + "getTranscript?userid=" + loggedinUsr.getRetval() + "&jobid=" + args[0] + "&format=" + args[1];
            getConnection(uri, "JobTranscript.txt");
        } catch (Exception e) {
            System.out.println("erron in IBMApi->getTrans");
        }
    }
}
