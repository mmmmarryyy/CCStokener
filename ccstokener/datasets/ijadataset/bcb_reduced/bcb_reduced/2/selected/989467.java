package org.amse.grigory.dchess.dchess;

import org.amse.grigory.dchess.rules.*;
import java.io.*;
import java.net.URL;
import org.amse.grigory.dchess.kernel.*;
import org.amse.grigory.dchess.view.*;

/**
 *
 * @author grigory
 */
public class DChess {

    private DChess() {
    }

    private static void prn(String s) {
        System.out.println("\n****************************");
        System.out.println(s + "Use -h parameter to get help.");
        System.out.println("****************************\n");
    }

    private static void printHelp() {
        Reader inp;
        try {
            URL url = DChess.class.getClassLoader().getResource("org/amse/grigory/dchess/dchess/man");
            inp = new BufferedReader(new InputStreamReader(url.openStream()));
            char[] temp = new char[1];
            int c = 0;
            c = inp.read(temp);
            while (c > 0) {
                System.out.print(temp);
                c = inp.read(temp);
            }
        } catch (IOException e) {
            System.out.println(e);
        }
        System.out.println("");
    }

    private static void prnNotSupport() {
        System.out.println("\nSorry. This option isn't supported yet.\n");
    }

    public static void main(String args[]) {
        if (args.length == 0) {
            runX();
        } else {
            boolean par = false;
            if ((args[0].equals("-h")) || (args[0].equals("--help"))) {
                printHelp();
                par = true;
            }
            if ((args[0].equals("-v")) || (args[0].equals("--version"))) {
                System.out.println("Current version is 0.23");
                par = true;
            }
            if (!par) {
                prn("Wrong parameter! ");
            }
        }
    }

    private static void runConsole(String rulesfile) {
        Model model = new Model(rulesfile);
        try {
            model.newGame();
        } catch (Exception e) {
            e.printStackTrace();
        }
        model.start();
        IView view = new ConsoleView(model);
        while ((model.isWin() == Rules.NOBODY_WIN) && !(view.getExit())) {
            view.show();
            view.getMove();
        }
    }

    private static void runX() {
        new DChessFrame();
    }
}
