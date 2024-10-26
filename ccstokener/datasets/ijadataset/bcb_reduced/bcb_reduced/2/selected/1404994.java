package tr.com.iontek.biotools;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import javax.swing.ImageIcon;
import org.biojava.bio.chromatogram.AbstractChromatogram;
import org.biojava.bio.chromatogram.ChromatogramFactory;
import org.biojava.bio.chromatogram.UnsupportedChromatogramFormatException;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.symbol.Alignment;
import org.biojava.bio.symbol.SymbolList;
import tr.com.iontek.biotools.formats.Fasta;

public class Tools {

    public static Fasta abi2fasta(File abifile) {
        return abi2fasta(abifile, abifile.getName());
    }

    public static Fasta abi2fasta(AbstractChromatogram trace, String header) {
        Fasta fasta = new Fasta();
        Alignment aln = trace.getBaseCalls();
        Iterator itr = aln.symbolListIterator();
        SymbolList symL = (SymbolList) itr.next();
        fasta.seq = symL.seqString().toUpperCase();
        fasta.header = new String(">" + header);
        fasta.fasta = fastaFormat(fasta.header, fasta.seq, Fasta.linewidth);
        return fasta;
    }

    public static Fasta abi2fasta(File file, String header) {
        AbstractChromatogram trace = null;
        try {
            trace = (AbstractChromatogram) ChromatogramFactory.create(file);
        } catch (UnsupportedChromatogramFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return abi2fasta(trace, header);
    }

    public static String fastaFormat(String head, String seq, int width) {
        StringBuffer buffer = new StringBuffer();
        int i, begin, end;
        buffer.append("> " + head + "\n");
        int lineCount = seq.length() / width;
        if (seq.length() % width == 0) {
            lineCount--;
        }
        for (i = 0; i <= lineCount; i++) {
            begin = i * width;
            end = (1 + i) * width;
            if (end > seq.length()) {
                end = seq.length();
            }
            buffer.append(seq.substring(begin, end) + "\n");
        }
        buffer.append("\n");
        return buffer.toString().toUpperCase();
    }

    public static int countOccurrences(String arg1, String arg2) {
        int count = 0;
        int index = 0;
        while ((index = arg1.indexOf(arg2, index)) != -1) {
            ++index;
            ++count;
        }
        return count;
    }

    public static ImageIcon createImageIcon(Object object, String path, String description) {
        java.net.URL imgURL = object.getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    public static boolean ionSendMail(String from, String to, String subject, String message) {
        Socket smtpSocket = null;
        DataOutputStream os = null;
        BufferedReader is = null;
        try {
            smtpSocket = new Socket("217.195.199.115", 25);
            os = new DataOutputStream(smtpSocket.getOutputStream());
            is = new BufferedReader(new InputStreamReader(smtpSocket.getInputStream()));
            if (smtpSocket != null && os != null && is != null) {
                try {
                    os.writeBytes("HELO\r\n");
                    os.writeBytes("MAIL From: <" + from + ">\r\n");
                    os.writeBytes("RCPT To: <" + to + ">\r\n");
                    os.writeBytes("DATA\r\n");
                    os.writeBytes("X-Mailer: Via Java\r\n");
                    os.writeBytes("From: <" + from + ">\r\n");
                    os.writeBytes("To:  <" + to + ">\r\n");
                    os.writeBytes("Subject: " + subject + "\r\n");
                    os.writeBytes(message + "\r\n");
                    os.writeBytes("\r\n.\r\n");
                    os.writeBytes("QUIT\r\n");
                    String responseline;
                    while ((responseline = is.readLine()) != null) {
                        if (responseline.indexOf("Ok") != -1) break;
                    }
                } catch (Exception e) {
                    System.err.println("Cannot send email as an error occurred.");
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("Connection to  failed");
            return false;
        }
        return true;
    }

    public static void download(String address, String localFileName) {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(address);
            out = new BufferedOutputStream(new FileOutputStream(localFileName));
            conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
            System.out.println(localFileName + "\t" + numWritten);
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    public static void download(String address) {
        int lastSlashIndex = address.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < address.length() - 1) {
            download(address, address.substring(lastSlashIndex + 1));
        } else {
            System.err.println("Could not figure out local file name for " + address);
        }
    }

    public static Fasta reverse(Fasta input) {
        Fasta output = new Fasta();
        output.header = new String(input.header + " reverse");
        SymbolList symL;
        try {
            symL = DNATools.createDNA(input.seq);
            symL = DNATools.reverseComplement(symL);
            symL = DNATools.complement(symL);
            output.seq = new String(symL.seqString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    public static Fasta complement(Fasta input) {
        Fasta output = new Fasta();
        output.header = new String(input.header + " complement");
        SymbolList symL;
        try {
            symL = DNATools.createDNA(input.seq);
            symL = DNATools.complement(symL);
            output.seq = new String(symL.seqString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    public static Fasta revcomplement(Fasta input) {
        Fasta output = new Fasta();
        output.header = new String(input.header + " reverse complement");
        SymbolList symL;
        try {
            symL = DNATools.createDNA(input.seq);
            symL = DNATools.reverseComplement(symL);
            output.seq = new String(symL.seqString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }
}
