import conga.Configuration;
import conga.ConfigurationBuilder;
import conga.io.format.ConfigFormats;
import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.net.URL;

/** @author Justin Caballero */
public class NoParameterHierarchy {

    public static void main(String[] args) throws IOException {
        ConfigFormats.setDefaultFormat(ConfigFormats.XML);
        printBad();
        printGood();
    }

    private static void printBad() throws IOException {
        URL url = NoParameterHierarchy.class.getResource("noParamHier-bad.xml");
        Configuration config = ConfigurationBuilder.newUrlConfig(url);
        System.out.println("Original:");
        System.out.println(IOUtils.toString(url.openStream()));
        System.out.println("\nHow conga sees it:");
        config.save(System.out);
        System.out.println("\nOr, as properties:");
        config.save(System.out, ConfigFormats.PROPERTIES);
    }

    private static void printGood() throws IOException {
        URL url = NoParameterHierarchy.class.getResource("noParamHier-good.xml");
        Configuration config = ConfigurationBuilder.newUrlConfig(url);
        System.out.println("\n\nA better way:");
        System.out.println(IOUtils.toString(url.openStream()));
        System.out.println("\nAs properties:");
        config.save(System.out, ConfigFormats.PROPERTIES);
        System.out.println("\nAccess and print:");
        for (String name : config.extend().getList("module.name")) {
            String prefix = "module." + name + ".";
            System.out.println(config.extend().getClass(prefix + "class"));
            System.out.println(config.getInt(prefix + "timeout"));
        }
    }
}
