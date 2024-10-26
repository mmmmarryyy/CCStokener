package net.sourceforge.gatherer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.scrollrack.CardInfo;

public class Gatherer {

    private static String[] expansions = { "un=Unlimited", "7e=Seventh Edition", "8e=Eighth Edition", "9e=Ninth Edition", "10e=Tenth Edition", "an=Arabian Nights", "aq=Antiquities", "lg=Legends", "dk=The Dark", "fe=Fallen Empires", "ia=Ice Age", "hl=Homelands", "ai=Alliances", "mr=Mirage", "vi=Visions", "wl=Weatherlight", "tp=Tempest", "sh=Stronghold", "ex=Exodus", "us=Urza's Saga", "ul=Urza's Legacy", "ud=Urza's Destiny", "mm=Mercadian Masques", "ne=Nemesis", "pr=Prophecy", "in=Invasion", "ps=Planeshift", "ap=Apocalypse", "od=Odyssey", "tr=Torment", "ju=Judgment", "on=Onslaught", "le=Legions", "sc=Scourge", "mi=Mirrodin", "ds=Darksteel", "5dn=Fifth Dawn", "chk=Champions of Kamigawa", "bok=Betrayers of Kamigawa", "sok=Saviors of Kamigawa", "rav=Ravnica: City of Guilds", "gp=Guildpact", "di=Dissension", "cs=Coldsnap", "ts=Time Spiral", "tsts=Time Spiral \"Timeshifted\"", "pc=Planar Chaos", "fut=Future Sight", "lw=Lorwyn", "mt=Morningtide", "shm=Shadowmoor", "eve=Eventide", "ala=Shards of Alara", "cfx=Conflux", "arb=Alara Reborn", "zen=Zendikar", "wwk=Worldwake", "roe=Rise of the Eldrazi", "m10=Magic 2010", "m11=Magic 2011" };

    private static String LANGUAGE = "en";

    public static void main(String[] argv) throws Exception {
        new Gatherer().run(argv);
    }

    public void run(String[] argv) throws Exception {
        String filename, expansion, long_name, language, url;
        TextSpoiler textspoiler, partial_text;
        int iii, idx, count;
        CardInfo[] cardbase;
        textspoiler = null;
        for (iii = expansions.length - 1; iii >= 0; iii--) {
            expansion = expansions[iii];
            idx = expansion.indexOf('=');
            long_name = expansion.substring(idx + 1);
            expansion = expansion.substring(0, idx);
            filename = expansion + ".html";
            if (!new File(filename).exists()) {
                while ((idx = long_name.indexOf(' ')) >= 0) {
                    long_name = (long_name.substring(0, idx) + "%20" + long_name.substring(idx + 1));
                }
                url = ("http://gatherer.wizards.com/Pages/Search/Default.aspx?" + "output=spoiler&method=text&" + "set=[%22" + long_name + "%22]");
                download_file(url, filename);
            }
            System.out.println("Processing " + filename + "...");
            partial_text = new TextSpoiler(filename);
            if (textspoiler == null) textspoiler = partial_text; else {
                count = textspoiler.size() + partial_text.size();
                textspoiler.addAll(partial_text);
                count = count - textspoiler.size();
                System.out.println("" + count + " duplicates");
            }
        }
        cardbase = (CardInfo[]) textspoiler.toArray(new CardInfo[0]);
        for (iii = expansions.length - 1; iii >= 0; iii--) {
            expansion = expansions[iii];
            idx = expansion.indexOf('=');
            expansion = expansion.substring(0, idx);
            language = LANGUAGE;
            filename = expansion + "_" + language;
            if (!new File(filename).exists()) {
                url = ("http://magiccards.info/" + expansion + "/" + language + ".html");
                download_file(url, filename);
            }
            read_expansion(cardbase, expansion, language);
        }
        output_cardbase(cardbase);
    }

    private void download_file(String url, String filename) throws Exception {
        URLConnection connection;
        OutputStream fstream;
        InputStream istream;
        byte[] buffer;
        int size;
        System.out.println("Downloading " + url + "...");
        fstream = new FileOutputStream(filename);
        connection = new URL(url).openConnection();
        connection.setDoInput(true);
        connection.setUseCaches(false);
        istream = connection.getInputStream();
        buffer = new byte[8192];
        while ((size = istream.read(buffer)) > 0) {
            fstream.write(buffer, 0, size);
        }
        istream.close();
        fstream.close();
    }

    private void read_expansion(CardInfo[] cardbase, String exp, String lang) throws Exception {
        String filename, card_link, end_href, end_link;
        InputStream istream;
        BufferedReader reader;
        String text, path, name;
        int idx, number;
        CardInfo info;
        filename = exp + "_" + lang;
        System.out.println("Processing " + filename + "...");
        card_link = "<a href=\"/" + exp + "/" + lang + "/";
        end_href = ".html\">";
        end_link = "</a>";
        istream = new FileInputStream(filename);
        reader = new BufferedReader(new InputStreamReader(istream, "UTF-8"));
        while (true) {
            text = reader.readLine();
            if (text == null) break;
            idx = text.indexOf(card_link);
            if (idx < 0) continue;
            path = text.substring(idx + card_link.length());
            idx = path.indexOf(end_href);
            if (idx < 0) continue;
            name = path.substring(idx + end_href.length());
            path = path.substring(0, idx);
            try {
                number = Integer.parseInt(path);
            } catch (NumberFormatException exception) {
                System.out.println(path + ": " + exception);
                continue;
            }
            idx = name.indexOf(end_link);
            if (idx < 0) continue;
            name = name.substring(0, idx);
            if (name.startsWith("<b>")) {
                name = name.substring(3);
                if (name.endsWith("</b>")) name = name.substring(0, name.length() - 4);
            }
            if (is_split_card(name)) {
                continue;
            }
            name = Latinizer.latinize(name);
            if (name != null) {
                idx = find_card(cardbase, name);
                if (idx < 0) {
                    System.out.println("not found: " + name);
                } else {
                    info = cardbase[idx];
                    info.add_print(exp, number);
                }
            }
        }
        reader.close();
    }

    /**
 * Binary search for the given card name.
 * Card names must be sorted (case insensitive).
 */
    public int find_card(CardInfo[] cardbase, String name) {
        int lo, hi, mid, val;
        lo = 0;
        hi = cardbase.length - 1;
        while (lo <= hi) {
            mid = (lo + hi) / 2;
            val = cardbase[mid].name.compareToIgnoreCase(name);
            if (val == 0) return (mid);
            if (val < 0) lo = mid + 1; else hi = mid - 1;
        }
        return (-1);
    }

    private void output_cardbase(CardInfo[] cardbase) throws Exception {
        String filename;
        PrintStream pstream;
        int iii;
        CardInfo info;
        filename = "magicthegathering.txt";
        pstream = new PrintStream(new FileOutputStream(filename));
        for (iii = 0; iii < cardbase.length; iii++) {
            info = cardbase[iii];
            if (info.prints == null) continue;
            pstream.println(info.name);
            if (info.cost != null) pstream.println(info.cost);
            pstream.println(info.cardtype);
            if (info.pow_tgh != null) pstream.println(info.pow_tgh);
            if (info.text != null) pstream.println(info.text);
            pstream.println(prints_to_text(info.prints));
            pstream.println("");
        }
        print_expansion_section(pstream);
        pstream.close();
    }

    private String prints_to_text(List prints) {
        String text;
        Iterator iterator;
        CardInfo.CardPrint print;
        text = "[";
        iterator = prints.iterator();
        while (iterator.hasNext()) {
            print = (CardInfo.CardPrint) iterator.next();
            text += print.exp + " " + print.number;
            if (iterator.hasNext()) text += ", ";
        }
        text += "]";
        return text;
    }

    private static boolean is_split_card(String name) {
        int idx1 = name.indexOf('(');
        int idx2 = name.indexOf('/');
        return ((idx1 > 0) && (idx2 > idx1));
    }

    /**
 * Append expansion and format data to the end of the file.
 */
    private void print_expansion_section(PrintStream pstream) {
        pstream.println("<expansions>");
        for (int iii = 0; iii < expansions.length; iii++) pstream.println(expansions[iii]);
        pstream.println("");
        pstream.println("<formats>");
        pstream.println("Standard: ala cfx arb zen wwk roe m10 m11");
        pstream.println("Extended: 9e 10e m10 m11 mi ds 5dn chk bok sok rav gp di cs ts tsts pc fut lw mt shm eve ala cfx arb zen wwk roe");
        pstream.println("Vintage: all");
    }
}
