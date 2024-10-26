package net.sf.fileexchange.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.fileexchange.api.snapshot.AddressExtractedWithRegExSourceSnapshot;
import net.sf.fileexchange.api.snapshot.AddressSourceSnapshot;
import net.sf.fileexchange.api.snapshot.AddressSourcesSnapshot;
import net.sf.fileexchange.api.snapshot.ConstantAddressSourceSnapshot;
import net.sf.fileexchange.api.snapshot.IPsFromNetworkInterfacesSnapshot;
import net.sf.fileexchange.api.snapshot.events.AddAddressExtractedWithRegExEvent;
import net.sf.fileexchange.api.snapshot.events.AddAddressSourceConstantEvent;
import net.sf.fileexchange.api.snapshot.events.AddIPsFromNetworkInterfacesEvent;
import net.sf.fileexchange.api.snapshot.events.DeleteAddressSourceEvent;
import net.sf.fileexchange.api.snapshot.events.MoveAddressEvent;
import net.sf.fileexchange.api.snapshot.events.StorageEventListener;
import net.sf.fileexchange.api.snapshot.events.StorageEventListeners;

public class AddressSources extends ObservableList<AddressSources.AddressSource> {

    private static Logger logger = Logger.getLogger(AddressSources.class.getCanonicalName());

    private final StorageEventListeners<AddressSourcesSnapshot> storageEventListeners = new StorageEventListeners<AddressSourcesSnapshot>();

    public AddressSources(AddressSourcesSnapshot snapshot) {
        final Iterator<AddressSourceSnapshot> iterator = snapshot.getChilds().iterator();
        while (iterator.hasNext()) {
            final AddressSourceSnapshot sourceSnapshot = iterator.next();
            if (sourceSnapshot instanceof IPsFromNetworkInterfacesSnapshot) {
                add(new IPsFromNetworkInterfaces());
            } else if (sourceSnapshot instanceof ConstantAddressSourceSnapshot) {
                add(new ConstantAddressSource(((ConstantAddressSourceSnapshot) sourceSnapshot).getValue()));
            } else if (sourceSnapshot instanceof AddressExtractedWithRegExSourceSnapshot) {
                final AddressExtractedWithRegExSourceSnapshot castedSnapshot = (AddressExtractedWithRegExSourceSnapshot) sourceSnapshot;
                final String label = castedSnapshot.getLabel();
                final URL url = castedSnapshot.getUrl();
                final Pattern pattern = castedSnapshot.getPattern();
                final int group = castedSnapshot.getGroup();
                if (label == null || url == null || pattern == null) {
                    iterator.remove();
                }
                final AddressExtractedWithRegEx address = new AddressExtractedWithRegEx(label, url, pattern, group);
                add(new AddressExtractedWithRegExSource(address));
            } else {
                throw new RuntimeException("Unhandled case: " + sourceSnapshot.getClass());
            }
        }
        addListener(new ListListener<AddressSource>() {

            @Override
            public void elementGotRemoved(int formerIndex, AddressSource deletedElement) {
                storageEventListeners.fireEvent(new DeleteAddressSourceEvent(formerIndex));
            }

            @Override
            public void elementGotMoved(AddressSource element, int sourceIndex, int destinationIndex) {
                storageEventListeners.fireEvent(new MoveAddressEvent(sourceIndex, destinationIndex));
            }
        });
    }

    private List<Address> createAddressList() {
        List<Address> addressList = new ArrayList<Address>();
        for (AddressSource source : this) {
            addressList.addAll(source.getAddresses());
        }
        return addressList;
    }

    public interface Address {

        /**
		 * 
		 * @return the actual address.
		 * @throws FailedToGetValueOfAddress
		 *             if an exception occurs which should be reported to the
		 *             user.
		 */
        public String getValue() throws FailedToGetValueOfAddress;

        /**
		 * 
		 * @return a short string describing the address, can differ from the
		 *         value returned by {@link #getValue()}.
		 */
        public String toString();
    }

    public static class AddressFromNetworkInterface implements Address, Comparable<AddressFromNetworkInterface> {

        private final String networkName;

        private final boolean networkWasUp;

        private final InetAddress address;

        public String getAddressWithSuffix() {
            return String.format("%s (%s)", getValue(), networkName);
        }

        public String getValue() {
            return address.getHostAddress();
        }

        public AddressFromNetworkInterface(NetworkInterface network, InetAddress address) throws SocketException {
            this.networkName = network.getName();
            this.networkWasUp = network.isUp();
            this.address = address;
        }

        @Override
        public int compareTo(AddressFromNetworkInterface o) {
            if (networkWasUp != o.networkWasUp) {
                return networkWasUp ? -1 : 1;
            }
            if (address.isLoopbackAddress() != o.address.isLoopbackAddress()) {
                return address.isLoopbackAddress() ? 1 : -1;
            }
            if (!networkName.equals(o.networkName)) {
                return networkName.compareTo(o.networkName);
            }
            boolean isIP6 = address instanceof Inet6Address;
            boolean otherIsIP6 = o.address instanceof Inet6Address;
            if (isIP6 != otherIsIP6) {
                return isIP6 ? 1 : -1;
            }
            return address.getHostAddress().compareTo(o.address.getHostAddress());
        }

        @Override
        public String toString() {
            return getAddressWithSuffix();
        }
    }

    public static final class ConstantAddress implements Address {

        private final String value;

        public ConstantAddress(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public Address[] getAddressArray() {
        List<? extends Address> addresses = createAddressList();
        return addresses.toArray(new Address[addresses.size()]);
    }

    /**
	 * 
	 * @return the first address or null if none could be determined.
	 */
    public Address getFirstAddress() {
        List<? extends Address> addresses = createAddressList();
        if (addresses.isEmpty()) return null;
        return addresses.get(0);
    }

    public interface AddressSource {

        public List<? extends Address> getAddresses();
    }

    public static final class IPsFromNetworkInterfaces implements AddressSource {

        @Override
        public List<? extends Address> getAddresses() {
            List<AddressFromNetworkInterface> addressList = new ArrayList<AddressFromNetworkInterface>();
            Enumeration<NetworkInterface> interfaceIterator;
            try {
                interfaceIterator = NetworkInterface.getNetworkInterfaces();
                while (interfaceIterator.hasMoreElements()) {
                    NetworkInterface i = interfaceIterator.nextElement();
                    for (InterfaceAddress address : i.getInterfaceAddresses()) {
                        addressList.add(new AddressFromNetworkInterface(i, address.getAddress()));
                    }
                }
            } catch (SocketException e) {
                logger.log(Level.WARNING, "Catched SocketException while searching the NetworkInterfaces for IP addresses", e);
            }
            Collections.sort(addressList);
            return addressList;
        }

        @Override
        public String toString() {
            return "IPs from network interfaces";
        }
    }

    public static final class AddressExtractedWithRegEx implements Address {

        private final String label;

        private final URL url;

        private final Pattern pattern;

        private final int group;

        public AddressExtractedWithRegEx(String label, URL url, Pattern pattern, int group) {
            this.label = label;
            this.url = url;
            this.pattern = pattern;
            this.group = group;
        }

        @Override
        public String getValue() throws FailedToGetValueOfAddress {
            try {
                final ByteArrayOutputStream bigBuffer = new ByteArrayOutputStream();
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                final InputStream inputStream = connection.getInputStream();
                try {
                    byte[] smallBuffer = new byte[1024 * 32];
                    int readedBytes = inputStream.read(smallBuffer);
                    while (readedBytes != -1) {
                        bigBuffer.write(smallBuffer, 0, readedBytes);
                        readedBytes = inputStream.read(smallBuffer);
                    }
                } finally {
                    inputStream.close();
                }
                final String text = bigBuffer.toString();
                Matcher matcher = pattern.matcher(text);
                if (!matcher.find()) {
                    throw new IOException("Obtained content does not match pattern");
                }
                if (group < 0 || group > matcher.groupCount()) {
                    final String message = String.format("The specified group %d is not in the interval [0,%d].", group, matcher.groupCount());
                    throw new IOException(message);
                }
                return matcher.group(group);
            } catch (IOException e) {
                throw new FailedToGetValueOfAddress(e);
            }
        }

        @Override
        public String toString() {
            return label;
        }

        final String getLabel() {
            return label;
        }

        final URL getUrl() {
            return url;
        }

        final Pattern getPattern() {
            return pattern;
        }

        final int getGroup() {
            return group;
        }
    }

    public static final class AddressExtractedWithRegExSource implements AddressSource {

        private final AddressExtractedWithRegEx address;

        AddressExtractedWithRegExSource(AddressExtractedWithRegEx address) {
            this.address = address;
        }

        @Override
        public List<? extends Address> getAddresses() {
            return Arrays.asList(address);
        }

        @Override
        public String toString() {
            return address.toString();
        }
    }

    public static final class ConstantAddressSource implements AddressSource {

        private final ConstantAddress address;

        public ConstantAddressSource(String value) {
            this.address = new ConstantAddress(value);
        }

        @Override
        public List<? extends Address> getAddresses() {
            return Arrays.asList(address);
        }

        @Override
        public String toString() {
            return address.toString();
        }
    }

    public void addConstant(String address) {
        storageEventListeners.fireEvent(new AddAddressSourceConstantEvent(address));
        add(new ConstantAddressSource(address));
    }

    public void addIPsFromNetworkInterfaces() {
        storageEventListeners.fireEvent(new AddIPsFromNetworkInterfacesEvent());
        add(new IPsFromNetworkInterfaces());
    }

    public void addAddressExtractedWithRegEx(AddressExtractedWithRegEx address) {
        final String label = address.getLabel();
        final URL url = address.getUrl();
        final Pattern pattern = address.getPattern();
        final int group = address.getGroup();
        storageEventListeners.fireEvent(new AddAddressExtractedWithRegExEvent(label, url, pattern, group));
        add(new AddressExtractedWithRegExSource(address));
    }

    public void registerStorageListener(StorageEventListener<AddressSourcesSnapshot> listener) {
        storageEventListeners.registerListener(listener);
    }

    public static final class FailedToGetValueOfAddress extends Exception {

        private static final long serialVersionUID = 1L;

        public FailedToGetValueOfAddress(IOException e) {
            super(e.getMessage(), e);
        }
    }
}
