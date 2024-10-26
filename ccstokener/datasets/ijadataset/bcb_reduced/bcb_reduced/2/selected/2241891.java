package com.rubecula.beanpot.serialize;

import java.io.IOException;
import java.io.ObjectStreamConstants;
import java.net.URL;

/**
 * This is just a sample of another source/sink type. It has NOT been tested.
 * 
 * @author Robin Hillyard
 * 
 */
public final class Serializer_Url extends Serializer_ {

    /**
	 * @param url
	 *            the URL to serialize to/from
	 * @param restore
	 *            true for deserializing, false for serializing
	 * @throws IOException
	 */
    Serializer_Url(final URL url, final boolean restore) throws IOException {
        super(createSerializerStream(url, restore));
    }

    /**
	 * @param url
	 * @param restore
	 * @return
	 * @throws IOException
	 */
    private static ObjectStreamConstants createSerializerStream(final URL url, final boolean restore) throws IOException {
        if (restore) return new SerializeInputStream(url.openConnection().getInputStream());
        return new SerializeOutputStream(url.openConnection().getOutputStream());
    }
}
