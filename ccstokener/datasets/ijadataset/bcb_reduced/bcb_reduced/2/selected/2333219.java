package console;

import java.io.*;
import java.net.URL;

class CommandoCommand {

    String name;

    URL url;

    String path;

    String propertyPrefix;

    CommandoCommand(String name, URL url) {
        this.name = name;
        this.url = url;
        this.propertyPrefix = "commando." + name.replace(' ', '_') + '.';
    }

    CommandoCommand(String name, String path) {
        this.name = name;
        this.path = path;
        this.propertyPrefix = "commando." + name.replace(' ', '_') + '.';
    }

    Reader openStream() throws IOException {
        if (url != null) {
            return new BufferedReader(new InputStreamReader(url.openStream()));
        } else {
            return new BufferedReader(new FileReader(path));
        }
    }

    public String toString() {
        return name;
    }
}
