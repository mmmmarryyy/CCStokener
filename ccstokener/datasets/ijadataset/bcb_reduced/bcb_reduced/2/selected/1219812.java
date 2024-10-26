package com.objectwave.socketClassServer;

import com.objectwave.simpleSockets.*;
import java.io.*;

/**
 *  Send class definitions via sockets. It will only find classes that are
 *  accessible from the classpath. It will not look in ZIP nor JAR files.
 *
 * @author  dhoag
 * @version  $Id: ServeClassRequest.java,v 2.0 2001/06/11 15:54:25 dave_hoag Exp $
 */
public class ServeClassRequest extends ServeClient {

    /**
	 *  Constructor for the ServeClassRequest object
	 *
	 * @param  server
	 * @param  id
	 * @param  t
	 */
    public ServeClassRequest(ObjectServer server, int id, Thread t) {
        super((SimpleServer) server, id, t);
    }

    /**
	 *  Map the package name to the class. Also add .class to the end of the class
	 *  name.
	 *
	 * @param  className
	 * @return
	 */
    protected String formatClassName(String className) {
        return className.replace('.', java.io.File.separatorChar) + ".class";
    }

    /**
	 *  Send a stream of bytes that can be used to define the class.
	 *
	 * @param  fullClassName
	 */
    protected void processClassRequest(String fullClassName) {
        java.io.InputStream inStream = null;
        inStream = new com.objectwave.customClassLoader.FileClassLoader("").locateClass(fullClassName);
        if (inStream == null) {
            try {
                System.out.println("Class " + fullClassName + " not found. Was Looking For: " + formatClassName(fullClassName));
                socket.writeInt(-1);
            } catch (java.io.IOException ex) {
            }
            return;
        }
        byte result[];
        try {
            result = new byte[inStream.available()];
            inStream.read(result);
            inStream.close();
            this.socket.writeFromStream(new java.io.ByteArrayInputStream(result));
            return;
        } catch (Exception e) {
            System.out.println("Failed writing stream. " + e);
        }
        try {
            System.out.println("Class " + fullClassName + " not found");
            socket.writeInt(-1);
        } catch (java.io.IOException ex) {
        }
        return;
    }

    /**
	 *  Important. Entry point for processing Requests from an SocketClassLoader.
	 *
	 * @param  fullClassName
	 * @return
	 */
    protected String processRequest(final String fullClassName) {
        if (fullClassName.equals(SocketClassLoader.contentRequest)) {
            processResourceRequest();
        } else if (fullClassName.equals(SocketClassLoader.inputStreamRequest)) {
            processStreamRequest();
        } else {
            processClassRequest(fullClassName);
        }
        return null;
    }

    /**
	 *  Send a resource that was requested via getContent();
	 */
    protected void processResourceRequest() {
        try {
            java.net.URL url = (java.net.URL) this.socket.readObject();
            url = this.getClass().getResource(url.getFile());
            Object obj = url.getContent();
            System.out.println("Foudn " + obj);
            if (obj instanceof InputStream) {
                this.socket.writeObject(new Integer(1));
                this.socket.writeFromStream((InputStream) obj);
            } else {
                this.socket.writeObject(obj);
            }
        } catch (Exception e) {
            System.out.println("SrvClassRequest>>Error processing resource request " + e);
            e.printStackTrace();
        }
    }

    /**
	 *  Send a resource that was requested via getInputStream()
	 */
    protected void processStreamRequest() {
        try {
            java.net.URL url = (java.net.URL) this.socket.readObject();
            String fileName = url.getFile();
            url = ClassLoader.getSystemResource(fileName);
            InputStream stream;
            if (url == null) {
                stream = new java.io.ByteArrayInputStream(new byte[0]);
            } else {
                stream = url.openStream();
            }
            this.socket.writeFromStream(stream);
        } catch (Exception e) {
            System.out.println("SrvClassRequest>>Error processing stream request " + e);
            e.printStackTrace();
        }
    }
}
