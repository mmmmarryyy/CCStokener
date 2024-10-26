package org.xmlvm.iphone;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.xmlvm.XMLVMIgnore;
import org.xmlvm.XMLVMSkeletonOnly;

@XMLVMSkeletonOnly
public class NSURLConnection extends NSObject {

    @XMLVMIgnore
    private class RunnableInstance implements Runnable {

        private final NSURLConnectionDelegate delegate;

        private final NSMutableURLRequest req;

        public RunnableInstance(NSMutableURLRequest req, NSURLConnectionDelegate delegate) {
            this.req = req;
            this.delegate = delegate;
        }

        @Override
        public void run() {
            NSHTTPURLResponseHolder resp = new NSHTTPURLResponseHolder();
            NSErrorHolder error = new NSErrorHolder();
            NSData data = sendSynchronousRequest(req, resp, error);
            delegate.connectionDidReceiveData(NSURLConnection.this, data);
            delegate.connectionDidFinishLoading(NSURLConnection.this);
        }
    }

    private Thread thread;

    private NSURLConnection(NSMutableURLRequest req, NSURLConnectionDelegate delegate) {
        thread = new Thread(new RunnableInstance(req, delegate));
        thread.start();
    }

    public static NSData sendSynchronousRequest(NSMutableURLRequest req, NSHTTPURLResponseHolder resp, NSErrorHolder error) {
        NSData data = null;
        URL url = req.URL().xmlvmGetURL();
        URLConnection conn;
        try {
            conn = url.openConnection();
            data = new NSData(conn.getInputStream());
        } catch (IOException e) {
        }
        return data;
    }

    public static NSURLConnection connectionWithRequest(NSMutableURLRequest req, NSURLConnectionDelegate delegate) {
        return new NSURLConnection(req, delegate);
    }
}
