package orbgate;

import java.io.*;
import java.net.*;

/**
 * OrbGate HTTP request
 */
public final class ORBCall {

    /**
     * Perform ORB method call
     */
    private static final int TARGET_ORB = 0;

    /**
     * Perform object method call
     */
    private static final int TARGET_OBJECT = 1;

    /**
     * Status: call succeeded
     */
    public static final int STATUS_SUCCESS = 0;

    /**
     * Status: system exception raised
     */
    public static final int STATUS_SYSTEM_EXCEPTION = 1;

    /**
     * Status: user exception raised
     */
    public static final int STATUS_USER_EXCEPTION = 2;

    /**
     * Status: resend the request with another byte order, etc.
     */
    public static final int STATUS_RESEND_REQUEST = 3;

    private static final int HREQ_BYTE_ORDER = 0;

    private static final int HREQ_TARGET = 1;

    private static final int HREQ_HDRLEN = 4;

    private static final int HREQ_ALOFF_2 = 8;

    private static final int HREQ_ALOFF_4 = 12;

    private static final int HREQ_ALOFF_8 = 16;

    private static final int HREP_BYTE_ORDER = 0;

    private static final int HREP_CALL_STATUS = 1;

    private static final int HREP_PADDING = 2;

    private static final int HREP_REQ_BYTE_ORDER = 3;

    private static final int HDR_SIZE_REQUEST = 20;

    private static final int HDR_SIZE_REPLY = 4;

    /**
     * Internal state
     */
    private ORBImpl orb_;

    private URL url_;

    private byte[] requestHdr;

    private CDROutputStream outputCDR;

    protected ORBCall(ORBImpl orb, URL url) {
        requestHdr = new byte[HDR_SIZE_REQUEST];
        orb_ = orb;
        url_ = url;
    }

    public final synchronized void clear() {
        try {
            close();
        } finally {
            url_ = null;
            orb_ = null;
        }
    }

    public static ORBCall create(ORBImpl orb, URL url) {
        return new ORBCall(orb, url);
    }

    public static void dismiss(ORBCall call) {
        if (call != null) call.clear();
    }

    private final CDROutputStream open_request(int target) {
        CDROutputStream os = (CDROutputStream) orb_.create_output_stream();
        os.set_call_context(this);
        requestHdr[HREQ_TARGET] = (byte) target;
        requestHdr[HREQ_BYTE_ORDER] = (byte) os.get_byte_order();
        outputCDR = os;
        return os;
    }

    private final CDROutputStream complete_header(CDROutputStream os) {
        os.align(8);
        int hdr_size = os.total_size();
        CDR.write4b(requestHdr, HREQ_HDRLEN, hdr_size);
        os.reset_alignment();
        return os;
    }

    public final synchronized CDROutputStream orb_request(String opname) {
        CDROutputStream os = open_request(TARGET_ORB);
        os.write_string(opname);
        return complete_header(os);
    }

    public final synchronized CDROutputStream request(ObjectRef objref, String opname) {
        CDROutputStream os = open_request(TARGET_OBJECT);
        ObjectRef.write(os, objref);
        os.write_string(opname);
        return complete_header(os);
    }

    public final synchronized CDRInputStream invoke() throws org.omg.CORBA.portable.ApplicationException, org.omg.CORBA.portable.RemarshalException {
        java.io.OutputStream http_out = null;
        java.io.InputStream http_in = null;
        try {
            CDROutputStream cdr_out = outputCDR;
            int[] align_offts = cdr_out.get_align_offsets();
            CDR.write4b(requestHdr, HREQ_ALOFF_2, align_offts[0]);
            CDR.write4b(requestHdr, HREQ_ALOFF_4, align_offts[1]);
            CDR.write4b(requestHdr, HREQ_ALOFF_8, align_offts[2]);
            URLConnection conn = url_.openConnection();
            conn.setAllowUserInteraction(false);
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-orbgate-cdr");
            http_out = conn.getOutputStream();
            http_out.write(requestHdr, 0, HDR_SIZE_REQUEST);
            cdr_out.write_to(http_out);
            http_out.close();
            http_out = null;
            http_in = conn.getInputStream();
            int reply_size = conn.getContentLength();
            MsgBlk reply_blk = new MsgBlk(reply_size);
            byte[] reply_buf = reply_blk.data;
            for (int nrd = 0; nrd < reply_size; ) {
                int n = http_in.read(reply_buf, nrd, reply_size - nrd);
                if (n <= 0) throw new java.io.EOFException();
                nrd += n;
            }
            reply_blk.used = reply_size;
            http_in.close();
            http_in = null;
            int byte_order = reply_buf[HREP_BYTE_ORDER];
            int call_status = reply_buf[HREP_CALL_STATUS];
            int padding = reply_buf[HREP_PADDING];
            orb_.set_preferred_byte_order(reply_buf[HREP_REQ_BYTE_ORDER]);
            CDRInputStream cdr_in = (CDRInputStream) cdr_out.create_input_stream();
            cdr_in.open(reply_blk, HDR_SIZE_REPLY, reply_size - HDR_SIZE_REPLY);
            cdr_in.set_byte_order(byte_order);
            cdr_in.set_stream_pos(padding);
            switch(call_status) {
                case STATUS_SUCCESS:
                    return cdr_in;
                case STATUS_SYSTEM_EXCEPTION:
                    {
                        String id = cdr_in.read_string();
                        int minor = cdr_in.read_long();
                        org.omg.CORBA.CompletionStatus cmpl = org.omg.CORBA.CompletionStatus.from_int(cdr_in.read_long());
                        throw orb_.create_system_exception(id, minor, cmpl);
                    }
                case STATUS_USER_EXCEPTION:
                    {
                        String id = cdr_in.read_string();
                        cdr_in.open(reply_blk, HDR_SIZE_REPLY, reply_size - HDR_SIZE_REPLY);
                        cdr_in.set_byte_order(byte_order);
                        cdr_in.set_stream_pos(padding);
                        throw new org.omg.CORBA.portable.ApplicationException(id, cdr_in);
                    }
                case STATUS_RESEND_REQUEST:
                    throw new org.omg.CORBA.portable.RemarshalException();
            }
            throw new org.omg.CORBA.UNKNOWN("Unknown status code: " + call_status, 0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);
        } catch (UnknownHostException ex) {
            throw new org.omg.CORBA.COMM_FAILURE(ex.toString(), 0, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
        } catch (NoRouteToHostException ex) {
            throw new org.omg.CORBA.COMM_FAILURE(ex.toString(), 0, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
        } catch (ConnectException ex) {
            throw new org.omg.CORBA.COMM_FAILURE(ex.toString(), 0, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
        } catch (IOException ex) {
            throw new org.omg.CORBA.COMM_FAILURE(ex.toString(), 0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);
        } finally {
            if (http_out != null) {
                try {
                    http_out.close();
                } catch (IOException ex) {
                }
            }
            if (http_in != null) {
                try {
                    http_in.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    public synchronized void close() {
    }
}
