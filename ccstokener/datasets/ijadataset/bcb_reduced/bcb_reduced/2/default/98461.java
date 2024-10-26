import java.io.*;
import java.util.*;
import java.net.*;
import javax.swing.*;

public class scribeBinder {

    ArrayList rings = new ArrayList();

    String title = "No crutch.";

    scribeNote tempNote;

    String[] topicList;

    /*************************
  * Make the binder object *
  *************************/
    public scribeBinder() {
    }

    /***********
  * New note *
  ***********/
    public void newNote(String noteTitle, char[] noteBody) {
        rings.add(tempNote = new scribeNote(noteTitle, noteBody));
        updateList();
    }

    /********************************
  * Delete a note from the binder *
  ********************************/
    public void delNote(int toDel) {
        rings.remove(toDel);
        updateList();
    }

    /*****************************
  * Get the size of the binder *
  *****************************/
    public int getSize() {
        return rings.size();
    }

    /******************
  * Read note title *
  ******************/
    public String readNoteTitle(int whichNote) {
        return ((scribeNote) rings.get(whichNote)).getTitle();
    }

    /*****************
  * Read note body *
  *****************/
    public char[] readNoteBody(int whichNote) {
        return ((scribeNote) rings.get(whichNote)).getBody();
    }

    /*****************
  * Set note title *
  *****************/
    public void setNoteTitle(int whichNote, String newTitle) {
        ((scribeNote) rings.get(whichNote)).setTitle(newTitle);
        updateList();
    }

    /****************
  * Set note body *
  ****************/
    public void setNoteBody(int whichNote, char[] newBody) {
        ((scribeNote) rings.get(whichNote)).setBody(newBody);
    }

    /*******************
  * Clean the binder *
  *******************/
    public void clean() {
        rings.clear();
        updateList();
    }

    /***********************
  * Get the binder title *
  ***********************/
    public String getTitle() {
        return title;
    }

    /********************
  * Update topic list *
  ********************/
    public void updateList() {
        String[] topics = new String[getSize()];
        for (int k = 0; k < getSize(); k++) {
            topics[k] = readNoteTitle(k);
        }
        topicList = topics;
    }

    /***************
  * Get the list *
  ***************/
    public String[] getList() {
        return topicList;
    }

    /***********************
  * Set the binder title *
  ***********************/
    public void setTitle(String newTitle) {
        title = newTitle;
    }

    /**************
  * Sort binder ************************
  * This method gets a little nasty... *
  *************************************/
    public void sortBinder() {
        String[] names = new String[rings.size()];
        for (int k = 0; k < names.length; k++) {
            names[k] = readNoteTitle(k);
        }
        int compare = 0;
        String temp;
        for (int d = 0; d < names.length; d++) {
            for (int k = 0; k < names.length; k++) {
                compare = names[k].compareTo(names[d]);
                if (compare >= 1) {
                    temp = names[d];
                    names[d] = names[k];
                    names[k] = temp;
                }
            }
        }
        ArrayList clone = (ArrayList) rings.clone();
        clean();
        for (int d = 0; d < clone.size(); d++) {
            for (int k = 0; k < clone.size(); k++) {
                if (((scribeNote) clone.get(k)).getTitle().equals(names[d])) {
                    rings.add((scribeNote) clone.get(k));
                }
            }
        }
        updateList();
    }

    /***********************
  * Write binder to HTML *
  ***********************/
    public void writeToHtml(String filename) {
        if (filename.endsWith(".html") | filename.endsWith(".HTML")) {
        } else {
            filename = filename + ".html";
        }
        File file = new File(filename);
        try {
            file.createNewFile();
        } catch (IOException e) {
            System.out.println("There was an error: " + e);
        }
        try {
            BufferedWriter pen = new BufferedWriter(new FileWriter(filename));
            pen.write("<html><title>" + title + "</title>");
            pen.newLine();
            pen.write("<h1><b>" + title + "</b></h1>");
            pen.newLine();
            pen.write("<h2>Table Of Contents:</h2><ul>");
            pen.newLine();
            for (int k = 0; k < rings.size(); k++) {
                pen.write("<li><i><a href=\"#");
                pen.write(readNoteTitle(k));
                pen.write("\">");
                pen.write(readNoteTitle(k));
                pen.write("</a></i></li>");
                pen.newLine();
            }
            pen.write("</ul><hr>");
            pen.newLine();
            for (int k = 0; k < rings.size(); k++) {
                pen.write("<h3><b>");
                pen.write("<a name=\"");
                pen.write(readNoteTitle(k));
                pen.write("\">");
                pen.write(readNoteTitle(k) + "</a>");
                pen.write("</b></h3>");
                pen.newLine();
                pen.write("<a>");
                String[] tempText = { new String(readNoteBody(k)) };
                tempText = tempText[0].split("\n");
                for (int p = 0; p < tempText.length; p++) {
                    pen.write(tempText[p] + "<br>");
                }
                pen.write("</a>");
                pen.newLine();
            }
            pen.newLine();
            pen.write("<br><hr><a>This was created using Scribe, a free crutch editor.</a></html>");
            pen.flush();
            pen.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /************************************************
  * Export to WAV method, requires Espeak to work *
  ************************************************/
    public void writeToWav(String filename) {
        writeToTxt(filename);
        String command1 = "espeak -f \"" + filename + ".txt" + "\" -w \"" + filename + ".wav" + "\"";
        String command2 = "rm \"" + filename + ".txt\"";
        String command3 = "rm textToWave";
        File file = new File("textToWave");
        try {
            file.createNewFile();
        } catch (IOException e) {
            System.out.println("There was an error: " + e);
        }
        try {
            BufferedWriter pen = new BufferedWriter(new FileWriter("textToWave"));
            pen.write(command1);
            pen.newLine();
            pen.write(command2);
            pen.newLine();
            pen.write(command3);
            pen.newLine();
            pen.flush();
            pen.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        wizard.runCommand("sh textToWave");
    }

    /*********************************************************
  * Export to MP3 method, requires Espeak and Lame to work *
  *********************************************************/
    public void writeToMp3(String filename) {
        writeToWav(filename);
        String command1 = "lame \"" + filename + ".wav\" \"" + filename + ".mp3\"";
        String command2 = "rm \"" + filename + ".wav\"";
        String command3 = "rm waveToMp3";
        File file = new File("waveToMp3");
        try {
            file.createNewFile();
        } catch (IOException e) {
            System.out.println("There was an error: " + e);
        }
        try {
            BufferedWriter pen = new BufferedWriter(new FileWriter("waveToMp3"));
            pen.write(command1);
            pen.newLine();
            pen.write(command2);
            pen.newLine();
            pen.write(command3);
            pen.newLine();
            pen.flush();
            pen.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        wizard.runCommand("sh waveToMp3");
    }

    /**********************
  * Write to FTP method ***************************************
  * Basically, exports the crutch to HTML and then uploads it *
  ************************************************************/
    public void writeToFtp(String login, String password, String address, String directory, String filename) {
        String newline = System.getProperty("line.separator");
        try {
            URL url = new URL("ftp://" + login + ":" + password + "@ftp." + address + directory + filename + ".html" + ";type=i");
            URLConnection urlConn = url.openConnection();
            urlConn.setDoOutput(true);
            OutputStreamWriter stream = new OutputStreamWriter(urlConn.getOutputStream());
            stream.write("<html><title>" + title + "</title>" + newline);
            stream.write("<h1><b>" + title + "</b></h1>" + newline);
            stream.write("<h2>Table Of Contents:</h2><ul>" + newline);
            for (int k = 0; k < rings.size(); k++) {
                stream.write("<li><i><a href=\"#");
                stream.write(readNoteTitle(k));
                stream.write("\">");
                stream.write(readNoteTitle(k));
                stream.write("</a></i></li>" + newline);
            }
            stream.write("</ul><hr>" + newline + newline);
            for (int k = 0; k < rings.size(); k++) {
                stream.write("<h3><b>");
                stream.write("<a name=\"");
                stream.write(readNoteTitle(k));
                stream.write("\">");
                stream.write(readNoteTitle(k) + "</a>");
                stream.write("</b></h3>" + newline);
                stream.write(readNoteBody(k) + newline);
            }
            stream.write(newline + "<br><hr><a>This was created using Scribe, a free crutch editor.</a></html>");
            stream.close();
        } catch (IOException error) {
            System.out.println("There was an error: " + error);
        }
    }

    /******************
  * Read saved data *
  ******************/
    public void restoreBinder(String filename) {
        File file = new File(filename);
        rings.clear();
        try {
            FileInputStream fileInput = new FileInputStream(file);
            DataInputStream dataIn = new DataInputStream(fileInput);
            title = dataIn.readUTF();
            while (true) {
                try {
                    newNote(dataIn.readUTF(), dataIn.readUTF().toCharArray());
                    updateList();
                } catch (EOFException eof) {
                    break;
                }
            }
        } catch (IOException error) {
            System.out.println("There was an error: " + error);
        }
    }

    /*******************************************************
  * Write binder to a text file, with a ".txt" extension *********
  * Probably more useful for temporary saving than anything else *
  ***************************************************************/
    public void writeToTxt(String filename) {
        if (filename.endsWith(".txt") | filename.endsWith(".TXT")) {
        } else {
            filename = filename + ".txt";
        }
        File file = new File(filename);
        try {
            file.createNewFile();
        } catch (IOException e) {
            System.out.println("There was an error: " + e);
        }
        try {
            BufferedWriter pen = new BufferedWriter(new FileWriter(filename));
            pen.write(title);
            pen.newLine();
            pen.newLine();
            for (int k = 0; k < rings.size(); k++) {
                pen.write(readNoteTitle(k));
                pen.newLine();
                pen.write(readNoteBody(k));
                pen.newLine();
                pen.newLine();
            }
            pen.flush();
            pen.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*****************************
  * Saving the current binder. *
  *****************************/
    public void saveBinder(String filename) {
        if (filename.endsWith(".crutch") | filename.endsWith(".CRUTCH")) {
        } else {
            filename = filename + ".crutch";
        }
        File file = new File(filename);
        try {
            FileOutputStream fileOutput = new FileOutputStream(file);
            DataOutputStream dataOut = new DataOutputStream(fileOutput);
            dataOut.writeUTF(title);
            for (int k = 0; k < rings.size(); k++) {
                dataOut.writeUTF(readNoteTitle(k));
                String tempString = new String(readNoteBody(k));
                dataOut.writeUTF(tempString);
            }
            fileOutput.close();
        } catch (IOException e) {
            System.out.println("There was an error: " + e);
        }
    }

    /********************
 * Import topic list *
 ********************/
    public String[] importList(String filename) {
        rings.clear();
        try {
            BufferedReader glasses = new BufferedReader(new FileReader(filename));
            String temp;
            do {
                temp = glasses.readLine();
                newNote(temp, "".toCharArray());
                updateList();
            } while (temp != null);
        } catch (IOException error) {
            System.out.println("There was an error: " + error);
        }
        return topicList;
    }

    /********************
  * Export topic list *
  ********************/
    public void exportList(String filename) {
        if (filename.endsWith(".list") | filename.endsWith(".LIST")) {
        } else {
            filename = filename + ".list";
        }
        File file = new File(filename);
        try {
            file.createNewFile();
        } catch (IOException e) {
            System.out.println("There was an error: " + e);
        }
        try {
            BufferedWriter pen = new BufferedWriter(new FileWriter(filename));
            for (int k = 0; k < rings.size(); k++) {
                pen.write(readNoteTitle(k));
                pen.newLine();
            }
            pen.flush();
            pen.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
