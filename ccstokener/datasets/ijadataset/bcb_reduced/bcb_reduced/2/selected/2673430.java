package MiG.oneclick;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.io.ObjectInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import javax.net.ssl.HttpsURLConnection;

public class Exe implements Runnable {

    private final int SLEEPTIME_FACTOR = 1000;

    private String server;

    private String iosessionid;

    private String jobid;

    private String execute_cmd;

    private String[] execute_args;

    private StringBuffer stderr;

    private StringBuffer stdout;

    private Method MiG_main_method;

    private Method getStdout_method;

    private Method getStderr_method;

    private Method setInfo_method;

    private Method restoreCheckpointedFiles_method;

    private URLClassLoader urlClassLoader;

    public Exe(URLClassLoader urlClassLoader, String server, String iosessionid, String jobid, String execute_cmd, String[] execute_args) {
        this.urlClassLoader = urlClassLoader;
        this.server = server;
        this.iosessionid = iosessionid;
        this.jobid = jobid;
        this.execute_cmd = execute_cmd;
        this.execute_args = execute_args;
        this.stderr = new StringBuffer();
        this.stdout = new StringBuffer();
        this.MiG_main_method = null;
        this.getStdout_method = null;
        this.getStderr_method = null;
        this.setInfo_method = null;
    }

    private long checkForSleepJob(String execute_str, String[] execute_args) throws java.lang.Exception {
        long sleeptime = -1;
        if (execute_str.indexOf("sleep ") == 0) {
        }
        return sleeptime;
    }

    private void getJobMethods(Class job_class) throws NoSuchMethodException {
        int mods;
        this.restoreCheckpointedFiles_method = job_class.getMethod("restoreCheckpointedFiles", new Class[] {});
        mods = restoreCheckpointedFiles_method.getModifiers();
        if (this.restoreCheckpointedFiles_method.getReturnType() != boolean.class || Modifier.isStatic(mods) || !Modifier.isPublic(mods)) {
            throw new NoSuchMethodException("StringBuffer boolean restoreCheckpointedFiles()");
        }
        this.MiG_main_method = job_class.getMethod("MiG_main", new Class[] { this.execute_args.getClass() });
        mods = this.MiG_main_method.getModifiers();
        if (this.MiG_main_method.getReturnType() != void.class || Modifier.isStatic(mods) || !Modifier.isPublic(mods)) {
            throw new NoSuchMethodException("MiG_main(String[] argv)");
        }
        this.getStdout_method = job_class.getMethod("getStdout", null);
        mods = this.getStdout_method.getModifiers();
        if (this.getStdout_method.getReturnType() != StringBuffer.class || Modifier.isStatic(mods) || !Modifier.isPublic(mods)) {
            throw new NoSuchMethodException("StringBuffer getStdout()");
        }
        this.getStderr_method = job_class.getMethod("getStderr", null);
        mods = this.getStderr_method.getModifiers();
        if (this.getStderr_method.getReturnType() != StringBuffer.class || Modifier.isStatic(mods) || !Modifier.isPublic(mods)) {
            throw new NoSuchMethodException("StringBuffer getStderr()");
        }
        this.setInfo_method = job_class.getMethod("setInfo", new Class[] { this.server.getClass(), this.iosessionid.getClass(), this.jobid.getClass() });
        mods = this.setInfo_method.getModifiers();
        if (this.setInfo_method.getReturnType() != void.class || Modifier.isStatic(mods) || !Modifier.isPublic(mods)) {
            throw new NoSuchMethodException("void setInfo(String server, String session_id )");
        }
    }

    private Object getCheckpointObject() {
        int checkpoint_nr;
        int mods;
        Boolean filerestore_result;
        String checkpoint_url_str;
        String checkpoint_request_url_str;
        URL checkpoint_obj_url;
        HttpsConnection checkpoint_request_conn;
        HttpsURLConnection httpsUrlConn;
        Class job_class;
        Object job_object;
        Object result;
        ObjectInputStream ois;
        Method restoreCheckpointedFiles_method;
        result = null;
        try {
            checkpoint_url_str = this.server + "/sid_redirect/" + this.iosessionid + "/" + this.jobid + "." + this.execute_cmd + ".checkpoint";
            checkpoint_request_url_str = checkpoint_url_str + ".latest";
            System.out.println("checkpoint_request_url_str: " + checkpoint_request_url_str);
            checkpoint_request_conn = new HttpsConnection(checkpoint_request_url_str);
            checkpoint_request_conn.open();
            checkpoint_nr = -1;
            if (checkpoint_request_conn.getResponseCode() == HttpsConnection.HTTP_OK) {
                checkpoint_nr = Integer.parseInt(checkpoint_request_conn.readLine());
            }
            checkpoint_request_conn.close();
            if (checkpoint_nr != -1) {
                checkpoint_obj_url = new URL(checkpoint_url_str + "." + checkpoint_nr);
                System.out.println("checkpoint obj url: " + checkpoint_obj_url);
                httpsUrlConn = (HttpsURLConnection) checkpoint_obj_url.openConnection();
                httpsUrlConn.connect();
                job_object = (Object) (new ObjectInputStream(httpsUrlConn.getInputStream())).readObject();
                httpsUrlConn.getResponseCode();
                httpsUrlConn.disconnect();
                job_class = job_object.getClass();
                this.getJobMethods(job_class);
                this.setInfo_method.invoke(job_object, new Object[] { this.server, this.iosessionid, this.jobid });
                result = job_object;
                filerestore_result = (Boolean) this.restoreCheckpointedFiles_method.invoke(job_object, new Object[] {});
                if (!filerestore_result.booleanValue()) {
                    result = null;
                }
            }
        } catch (java.lang.Exception e) {
            result = null;
            e.printStackTrace();
        }
        return result;
    }

    public StringBuffer getStdout() {
        return this.stdout;
    }

    public StringBuffer getStderr() {
        return this.stderr;
    }

    public void run() {
        int mods;
        long sleeptime;
        StringBuffer job_stderr;
        StringBuffer job_stdout;
        Class job_class;
        Object job_object;
        try {
            if (this.execute_cmd.compareTo("sleep") == 0) {
                sleeptime = Long.parseLong(execute_args[0]) * SLEEPTIME_FACTOR;
                Thread.sleep(sleeptime);
            } else {
                job_object = this.getCheckpointObject();
                if (job_object == null) {
                    job_class = this.urlClassLoader.loadClass(this.execute_cmd);
                    this.getJobMethods(job_class);
                } else {
                    job_class = job_object.getClass();
                }
                if (job_object == null) {
                    job_object = job_class.newInstance();
                    this.setInfo_method.invoke(job_object, new Object[] { this.server, this.iosessionid, this.jobid });
                }
                this.MiG_main_method.invoke(job_object, new Object[] { execute_args });
                job_stderr = (StringBuffer) this.getStderr_method.invoke(job_object, null);
                this.stderr.append(job_stderr);
                job_stdout = (StringBuffer) this.getStdout_method.invoke(job_object, null);
                this.stdout.append(job_stdout);
            }
        } catch (java.lang.Exception e) {
            e.printStackTrace();
            this.stderr.append(MiG.oneclick.Exception.dumpStackTrace(e));
        }
    }
}
