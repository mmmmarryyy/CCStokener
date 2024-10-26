package com.limespot.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

final class UserImpl implements User {

    private final Factory factory;

    private final String username;

    UserImpl(Factory factory, String username) {
        this.factory = factory;
        this.username = username;
    }

    public int getId() {
        return getMediaPrefix().id;
    }

    public Collection<User> getContacts() {
        final Map<String, User> res = new HashMap<String, User>();
        parse("users/" + getUsername(), new ValueHandler() {

            void startElementRest(String uri, String localName, String name, Attributes attributes) {
                if (!"a".equals(name)) return;
                String href = attributes.getValue("href");
                if (href == null) return;
                String target = "/users/";
                int itarget = href.indexOf(target);
                if (itarget == -1) return;
                String userName = href.substring(itarget + target.length());
                if (getUsername().equals(userName)) return;
                res.put(userName, factory.newUser(userName));
            }
        });
        return res.values();
    }

    public String getLargeImageURL() {
        return imageURL(300);
    }

    public String getSmallImageURL() {
        return imageURL(30);
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return parse("users/" + getUsername(), new ValueHandler() {

            private boolean get = false;

            void startElementRest(String uri, String localName, String name, Attributes attributes) {
                get = false;
                if (!"h1".equals(name)) return;
                String id = attributes.getValue("id");
                if (!"profile_name".equals(id)) return;
                get = true;
            }

            void charactersRest(char[] ch, int start, int length) {
                if (get) {
                    String value = new String(ch, start, length).trim();
                    if (!"".equals(value)) {
                        done(value);
                    }
                }
            }
        });
    }

    public String getTagline() {
        return parse("users/" + getUsername(), new ValueHandler() {

            private boolean get = false;

            void startElementRest(String uri, String localName, String name, Attributes attributes) {
                get = false;
                if (!"h2".equals(name)) return;
                String id = attributes.getValue("id");
                if (!"profile_tagline".equals(id)) return;
                get = true;
            }

            void charactersRest(char[] ch, int start, int length) {
                if (get) {
                    String value = new String(ch, start, length).trim();
                    if (!"".equals(value)) {
                        done(value);
                    }
                }
            }
        });
    }

    public String getBio() {
        return parse("users/" + getUsername(), new ValueHandler() {

            private boolean inDiv = false;

            private boolean inP = false;

            void startElementRest(String uri, String localName, String name, Attributes ats) {
                if (inDiv) {
                    if ("p".equals(name)) {
                        inP = true;
                        return;
                    }
                }
                inDiv = false;
                inP = false;
                if ("div".equals(name)) {
                    String id = ats.getValue("id");
                    if ("profile_bio".equals(id)) {
                        inDiv = true;
                        return;
                    }
                }
            }

            void charactersRest(char[] ch, int start, int length) {
                if (inP) {
                    String value = new String(ch, start, length).trim();
                    if (!"".equals(value)) {
                        done(value);
                    }
                }
            }
        });
    }

    public String getURL() {
        return parse("users/" + getUsername(), new ValueHandler() {

            private boolean get = false;

            void startElementRest(String uri, String localName, String name, Attributes ats) {
                if (get) {
                    if (!"a".equals(name)) return;
                    String href = ats.getValue("href");
                    if (!"".equals(href)) {
                        done(href);
                    }
                }
                get = false;
                if (!"div".equals(name)) return;
                String id = ats.getValue("id");
                if (!"profile_url_item".equals(id)) return;
                get = true;
            }
        });
    }

    public int compareTo(User that) {
        String n1 = this.getUsername();
        String n2 = that.getUsername();
        if (n1 == null) return -1;
        if (n2 == null) return +1;
        return n1.compareTo(n2);
    }

    public boolean equals(Object o) {
        if (!(o instanceof User)) return false;
        User that = (User) o;
        String n1 = this.getUsername();
        String n2 = that.getUsername();
        if (n1 == null) return n2 == null;
        if (n2 == null) return n1 == null;
        return n1.equals(n2);
    }

    public int hashCode() {
        return getUsername().hashCode() * 33;
    }

    private static final class MediaPrefix {

        final int id;

        final int mediaId;

        MediaPrefix(int id, int mediaId) {
            this.id = id;
            this.mediaId = mediaId;
        }

        public String toString() {
            return id + "/" + mediaId;
        }
    }

    private abstract static class ValueHandler extends DefaultHandler {

        private final Wrapper<String> v = new Wrapper<String>();

        static final class Done extends RuntimeException {
        }

        final String getValue() {
            return v.getValue();
        }

        final void setValue(String s) {
            v.setValue(s);
        }

        final void done(String s) {
            setValue(s);
            throw new Done();
        }

        void startElementRest(String uri, String localName, String name, Attributes attributes) {
        }

        void charactersRest(char[] ch, int start, int length) throws SAXException {
        }

        @Override
        public final void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, name, attributes);
            startElementRest(uri, localName, name, attributes);
        }

        @Override
        public final void characters(char[] ch, int start, int length) throws SAXException {
            super.characters(ch, start, length);
            charactersRest(ch, start, length);
        }
    }

    private String parse(String relativeURL, ValueHandler h) {
        String url = url(relativeURL);
        try {
            SAXParser p = SAXParserFactory.newInstance().newSAXParser();
            p.parse(new URL(url).openStream(), h);
        } catch (ValueHandler.Done d) {
        } catch (ParserConfigurationException e) {
            barf(e);
        } catch (SAXException e) {
            barf(e);
        } catch (MalformedURLException e) {
            barf(e);
        } catch (IOException e) {
            barf(e);
        }
        return h.getValue();
    }

    private void barf(Throwable e) {
        throw new LimeSpotException(e);
    }

    public String toString() {
        return username;
    }

    private String url(String rest) {
        return "http://limespot.com/" + rest;
    }

    private String rootURL() {
        return "http://limespot.com";
    }

    private String imageURL(int size) {
        return rootURL() + "/media/" + getMediaPrefix() + "/422," + size + "," + size + ",s,t.png";
    }

    private MediaPrefix getMediaPrefix() {
        String prefix = parse("users/" + getUsername(), new ValueHandler() {

            void startElementRest(String uri, String localName, String name, Attributes attributes) {
                if (!"img".equals(name)) return;
                String alt = attributes.getValue("alt");
                if (!getUsername().equals(alt)) return;
                String src = attributes.getValue("src");
                String target = "media/";
                int itarget = src.indexOf(target);
                if (itarget == -1) return;
                int icomma = src.indexOf(",", itarget + target.length() + 1);
                if (icomma == -1) return;
                done(src.substring(itarget + target.length(), icomma));
            }
        });
        String[] parts = prefix.split("/");
        return new MediaPrefix(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}
