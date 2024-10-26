package com.bigfatgun.fixjures.yaml;

import static com.bigfatgun.fixjures.FixtureException.convert;
import com.bigfatgun.fixjures.FixtureSource;
import com.bigfatgun.fixjures.FixtureType;
import com.google.common.base.Charsets;
import com.google.common.base.Supplier;
import org.ho.yaml.Yaml;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

public class YamlSource extends FixtureSource {

    public static FixtureSource newYamlUrl(String url) throws IOException {
        return new YamlSource(new URL(url).openStream());
    }

    public static FixtureSource newYamlResource(String resourceName) {
        return new YamlSource(YamlSource.class.getClassLoader().getResourceAsStream(resourceName));
    }

    public static FixtureSource newYamlStream(ReadableByteChannel channel) {
        return newYamlStream(Channels.newInputStream(channel));
    }

    public static FixtureSource newYamlStream(InputStream input) {
        return new YamlSource(input);
    }

    public static FixtureSource newYamlString(String yaml) {
        return newYamlString(yaml, Charsets.UTF_8);
    }

    public static FixtureSource newYamlString(String yaml, Charset charset) {
        return new YamlSource(new ByteArrayInputStream(yaml.getBytes(charset)));
    }

    private YamlSource(InputStream input) {
        super(Channels.newChannel(input));
    }

    @Override
    protected Object createFixture(FixtureType type) {
        try {
            Object object = Yaml.load(Channels.newInputStream(getSource()));
            final Supplier<?> provider = findValue(type, object);
            final Object value = provider.get();
            return type.getType().cast(value);
        } catch (Exception e) {
            throw convert(e);
        }
    }
}
