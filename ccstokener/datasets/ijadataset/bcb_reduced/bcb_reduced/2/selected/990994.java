package uk.ac.ncl.neresc.dynasoar.deployer;

import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.util.Properties;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import uk.ac.ncl.neresc.dynasoar.fault.DeploymentException;

/**
 * ***********************************************************************
 * Created On 18-Jul-2007 at 13:38:31
 * Created by nam48
 * <p/>
 * Copyright (C) 2007  Newcastle University, Newcastle Upon Tyne, UK
 * <p/>
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 * <p/>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 * ***********************************************************************
 */
public class VirtualMachineDeployer extends ServiceDeployer {

    private static Logger mLog = Logger.getLogger(VirtualMachineDeployer.class.getName());

    private String vmName = null;

    private String ret = null;

    private String fault = null;

    private static String vmBaseLocation = null;

    private static String vmwareCmd = null;

    private static final String START_RETURN = "start()";

    private static final String GETSTATE_RETURN = "getstate()";

    private static final String IP_RETURN = "getguestinfo(ip)";

    public VirtualMachineDeployer() throws IOException {
        InputStream inpStream = null;
        Properties hpProp = new Properties();
        try {
            inpStream = this.getClass().getResourceAsStream("/HPConfig.properties");
            mLog.debug("Loading HostProvider properties...");
            if (inpStream != null) {
                hpProp.load(inpStream);
                vmBaseLocation = new String(hpProp.getProperty("vmdisk.location"));
                inpStream.close();
                mLog.debug("The virtual machine base directory is: " + vmBaseLocation);
                vmwareCmd = new String(hpProp.getProperty("vmware.command"));
                mLog.debug("The virtual machine command is: " + vmwareCmd);
            }
        } catch (IOException ix) {
            mLog.error("Error while reading HPConfig.properties file");
            throw ix;
        }
    }

    private String[] getVMFilePaths(String location) {
        mLog.debug("Input location: " + location);
        String[] splitString = location.split(";");
        for (int i = 0; i < splitString.length; i++) {
            if (i == 0) {
                mLog.debug("Config File: " + splitString[i]);
            } else {
                mLog.debug("Hard Disk[" + (i + 1) + "]: " + splitString[i]);
            }
        }
        return splitString;
    }

    private String getFileName(String fileURI) {
        mLog.debug("Input URI: " + fileURI);
        int length = fileURI.length();
        int breakPoint = fileURI.lastIndexOf("/");
        String fileName = fileURI.substring(breakPoint + 1, length);
        mLog.debug("File Name: " + fileName);
        return fileName;
    }

    private void getFile(String fileURI, String targetFileName, String sourceFileName) {
        try {
            System.out.println("Getting " + sourceFileName + " from " + fileURI);
            FileOutputStream out = null;
            URL url = new URL(fileURI);
            File targetFile = new File(targetFileName);
            if (!targetFile.exists()) {
                targetFile.createNewFile();
            }
            InputStream in = null;
            try {
                in = url.openStream();
                out = new FileOutputStream(targetFile);
            } catch (Exception e) {
                e.printStackTrace();
                throw new DeploymentException("couldn't open stream due to: " + e.getMessage());
            }
            URLConnection con = url.openConnection();
            long fileLength = con.getContentLength();
            mLog.debug("File Length: " + fileLength);
            mLog.debug("Max Value for LONG: " + Long.MAX_VALUE);
            mLog.debug("Max Value for Integer: " + Integer.MAX_VALUE);
            ReadableByteChannel channelIn = Channels.newChannel(in);
            FileChannel channelOut = out.getChannel();
            channelOut.transferFrom(channelIn, 0, Integer.MAX_VALUE);
            channelIn.close();
            channelOut.close();
            out.flush();
            out.close();
            in.close();
        } catch (Exception ix) {
            ix.printStackTrace();
        }
    }

    public StringBuffer executeCommand(String command) {
        StringBuffer buff = null;
        Process child = null;
        try {
            Runtime runtime = Runtime.getRuntime();
            mLog.debug("Executing: " + command);
            child = runtime.exec(command, null, new File(System.getenv("CATALINA_HOME")));
            int i = child.waitFor();
            if (i == 0) {
                InputStream in = child.getInputStream();
                int c;
                buff = new StringBuffer();
                while ((c = in.read()) != -1) {
                    System.out.print((char) c);
                    buff.append((char) c);
                }
                in.close();
            }
        } catch (IOException iox) {
            System.out.println("IOException occured");
            iox.printStackTrace();
        } catch (InterruptedException ix) {
            System.out.println("Interrupted");
        }
        return buff;
    }

    private String[] parseReturnValue(StringBuffer sb) {
        System.out.println("Input: " + sb);
        int len = sb.length();
        int eql = sb.indexOf("=");
        String param = (sb.substring(0, eql)).trim();
        String value = (sb.substring(eql + 1, len)).trim();
        System.out.println("Param: " + param);
        System.out.println("Value: " + value);
        String[] returnValues = new String[2];
        returnValues[0] = param;
        returnValues[1] = value;
        return returnValues;
    }

    private String bootVirtualMachine(String configFileName) {
        String ip = null;
        try {
            String cmdLine = "chmod u+x " + configFileName;
            StringBuffer returnBuf = executeCommand(cmdLine);
            cmdLine = vmwareCmd + " " + configFileName + " start";
            returnBuf = executeCommand(cmdLine);
            if (returnBuf == null || returnBuf.length() == 0) {
                mLog.error("Command not executed, WHY???");
            } else {
                mLog.debug("Returned: " + returnBuf);
                String[] retnCmd = parseReturnValue(returnBuf);
                if (retnCmd[1].equals("1")) {
                    mLog.debug("VM starting up...");
                    cmdLine = vmwareCmd + " " + configFileName + " getstate";
                    returnBuf = executeCommand(cmdLine);
                    retnCmd = parseReturnValue(returnBuf);
                    if (retnCmd[1].equals("on")) {
                        mLog.debug("VM status is ON...");
                        boolean booting = true;
                        long startTime = System.currentTimeMillis();
                        System.out.print("Booting...");
                        while (booting) {
                            Thread.sleep(5000);
                            cmdLine = vmwareCmd + " " + configFileName + " getguestinfo ip";
                            returnBuf = executeCommand(cmdLine);
                            if (returnBuf != null && returnBuf.length() > 0) {
                                mLog.debug("IP Address Returned: " + returnBuf);
                                booting = false;
                            } else {
                                System.out.print("...");
                            }
                        }
                        long endTime = System.currentTimeMillis();
                        retnCmd = parseReturnValue(returnBuf);
                        mLog.debug("Total time to boot: " + (endTime - startTime) + "ms");
                        mLog.debug("IP Address: " + retnCmd[1]);
                        ip = retnCmd[1];
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return ip;
    }

    public String installCode(String serviceName, String location) throws DeploymentException {
        return "dummy";
    }

    public String installCode(String serviceName, String location, String vmName, String serviceLocationVM, String port) throws DeploymentException {
        try {
            long mStartTime = System.currentTimeMillis();
            String[] files = getVMFilePaths(location);
            boolean vmBaseExists = new File(vmBaseLocation).isDirectory();
            String vmDir = vmBaseLocation + File.separator + vmName;
            if (vmBaseExists) {
                vmBaseExists = new File(vmDir).mkdir();
            }
            String configFileName = null;
            for (int i = 0; i < files.length; i++) {
                mLog.debug("Downloading " + files[i] + "...");
                String sourceFileName = getFileName(files[i]);
                String targetFileName = vmDir + File.separator + sourceFileName;
                if (i == 0) {
                    configFileName = targetFileName;
                }
                boolean vmPresent = new File(targetFileName).exists();
                if (vmPresent) {
                    break;
                }
                getFile(files[i], targetFileName, sourceFileName);
            }
            long mEndTime = System.currentTimeMillis();
            mLog.debug("Downloaded all VM files in: " + (mEndTime - mStartTime) + "ms");
            String ipAddress = bootVirtualMachine(configFileName);
            if (ipAddress == null) {
                mLog.error("Unable to install virtual machine");
                throw new DeploymentException("Unable to install virtual machine", null);
            }
            String invocationAddress = "http://" + ipAddress + ":" + port + "/" + serviceLocationVM;
            mLog.debug("Service Endpoint: " + invocationAddress);
            long time = System.currentTimeMillis();
            check(invocationAddress, time, STARTCONTROL);
            return invocationAddress;
        } catch (Exception ex) {
            ex.printStackTrace();
            mLog.error("Unable to install virtual machine: " + ex.getMessage());
            throw new DeploymentException(ex.getMessage(), ex.getCause());
        }
    }

    public static void main(String[] args) {
        try {
            VirtualMachineDeployer vmDeployer = new VirtualMachineDeployer();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
