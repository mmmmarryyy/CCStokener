package JungLayouts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.List;

public class ProcessData {

    static int counter = 0;

    static String[] defaultPics = { "red.jpg", "green.jpg", "blue.jpg" };

    /*****************************************
	This method is used remove duplicate items
	from the list.
	******************************************/
    public static List<String> newUniquify(List<String> inList) {
        HashSet<String> hash = new HashSet<String>();
        hash.addAll(inList);
        inList.clear();
        inList.addAll(hash);
        return inList;
    }

    /*******************************************
	This method is used to calculate the average
	weight on Node/Edge.
	********************************************/
    public static void getAverageWeight() {
        int count = 0;
        int AverageInWeight = 0;
        int AverageOutWeight = 0;
        int[] In = new int[GraphPlotter.uCombinedList.length];
        int[] Out = new int[GraphPlotter.uCombinedList.length];
        int[] Combined = new int[GraphPlotter.uCombinedList.length];
        for (int i = 0; i < GraphPlotter.uCombinedList.length; i++) {
            count = 0;
            for (int j = 0; j < GraphPlotter.userList.length; j++) {
                if (GraphPlotter.uCombinedList[i].matches(GraphPlotter.userList[j])) {
                    count++;
                }
            }
            In[i] = count;
        }
        for (int i = 0; i < GraphPlotter.uCombinedList.length; i++) {
            count = 0;
            for (int j = 0; j < GraphPlotter.userList.length; j++) {
                if (GraphPlotter.uCombinedList[i].matches(GraphPlotter.toList[j])) {
                    count++;
                }
            }
            Out[i] = count;
        }
    }

    /*****************************************
	This method is used extract user profile 
	image from Url's and saves them in local.
	******************************************/
    public static void getAndsaveImages(String[] imageUrl) throws IOException {
        byte[] b;
        int length;
        URL url;
        InputStream is;
        OutputStream os;
        String destinationFile;
        String fileName;
        File file;
        for (int i = 0; i < imageUrl.length; i++) {
            fileName = imageUrl[i] + ".jpg";
            file = new File(fileName);
            if (file.exists() == false) {
                destinationFile = "images//UsersProfilePics//" + GraphPlotter.quserList[i] + ".jpg";
                url = new URL(imageUrl[i]);
                try {
                    is = url.openStream();
                    os = new FileOutputStream(destinationFile);
                    b = new byte[4096];
                    while ((length = is.read(b)) != -1) {
                        os.write(b, 0, length);
                    }
                    is.close();
                    os.close();
                } catch (IOException e) {
                    System.out.println("***Exception***");
                    FileInputStream fn = new FileInputStream("images//UsersProfilePics//" + defaultPics[randomNumber()]);
                    os = new FileOutputStream(destinationFile);
                    b = new byte[4096];
                    while ((length = fn.read(b)) != -1) {
                        os.write(b, 0, length);
                    }
                    fn.close();
                    os.close();
                }
                System.out.println(GraphPlotter.quserList[i] + "...." + Integer.toString(i));
            }
        }
    }

    /*****************************************
	This method is used to allocate a random 
	default Twitter profile image for receivers.
	******************************************/
    public static void getAndsaveRestImages(String[] imageUrl) throws IOException {
        String fileName;
        File file;
        byte[] b;
        int length;
        OutputStream os;
        String destinationFile;
        for (int i = 0; i < imageUrl.length; i++) {
            fileName = "images//UsersProfilePics//" + imageUrl[i] + ".jpg";
            file = new File(fileName);
            if (file.exists() == false) {
                destinationFile = "images//UsersProfilePics//" + imageUrl[i] + ".jpg";
                FileInputStream fn = new FileInputStream("images//UsersProfilePics//" + defaultPics[randomNumber()]);
                os = new FileOutputStream(destinationFile);
                b = new byte[4096];
                while ((length = fn.read(b)) != -1) {
                    os.write(b, 0, length);
                }
                fn.close();
                os.close();
            }
        }
    }

    /*****************************************
	This method is used to generate a random 
	integer Number [0 to 2] 
	******************************************/
    public static int randomNumber() {
        counter++;
        if (counter == 3) {
            counter = 0;
        }
        return counter;
    }
}
