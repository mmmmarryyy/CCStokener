package map;

import game.Game;
import gui.ImageFrame;
import java.io.*;
import java.net.URL;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.*;
import java.util.*;
import objects.creatures.*;
import objects.monster.*;

public class Map extends DefaultHandler {

    public int width, height;

    public int data;

    public String name;

    public MapField content[][];

    public Game game;

    public int start_x, start_y;

    private String aElement;

    private int aId;

    ArrayList<MapField> fieldtype;

    public ArrayList<Creature> creatures;

    int data_read;

    int Ax, Ay, aTemplate;

    String aType;

    private Creature acreature;

    public MapField getField(int i) {
        try {
            return (MapField) fieldtype.get(i);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public MapField createField(String type) {
        try {
            if (type.equals("Normal")) {
                return new NormalField();
            } else if (type.equals("Teleport")) {
                return new TeleportField();
            } else if (type.equals("City")) {
                return new CityField();
            }
        } catch (Exception e) {
            return new MapField();
        }
        return new MapField();
    }

    public Creature getNewCreature(String name) {
        System.out.println("New monster created : " + name);
        return new SimpleMonster();
    }

    public MapField getMap(int x, int y) throws Exception {
        try {
            return content[x][y];
        } catch (Exception e) {
            throw new Exception("Mapfield [" + x + "," + y + "] doesn't exist");
        }
    }

    public void setMapValue(String path, String name, String value) {
        String[] xpath = null;
        xpath = path.split("/");
        try {
            if (xpath[1].equals("map")) {
                if (xpath.length == 2) {
                    if (name.equals("width")) {
                        width = Integer.decode(value);
                    } else if (name.equals("height")) {
                        height = Integer.decode(value);
                    } else if (name.equals("name")) {
                        this.name = new String(value);
                    } else {
                        System.out.println("Unknown attribute \"" + name + "\" = \"" + value + "\"");
                    }
                } else {
                    if (xpath.length > 2 && xpath[2].equals("mapdata")) {
                        if (xpath.length > 3 && xpath[3].equals("player")) {
                            if (name.equals("x")) {
                                start_x = Integer.decode(value);
                            } else if (name.equals("y")) {
                                start_y = Integer.decode(value);
                            }
                        } else if (xpath.length > 3 && xpath[3].equals("fieldtypes")) {
                            if (xpath.length > 4 && xpath[4].equals("fieldtype")) {
                                if (name.equals("id") && xpath.length == 5) {
                                    aId = Integer.decode(value);
                                    fieldtype.add(aId, new MapField());
                                } else if (xpath[5].equals("name")) {
                                    MapField mp = (MapField) fieldtype.get(aId);
                                    mp.name = new String(value);
                                } else if (xpath[5].equals("img")) {
                                    MapField mp = (MapField) fieldtype.get(aId);
                                    if (name.equals("id")) {
                                        ImageFrame ifr = new ImageFrame();
                                        ifr.id = Integer.decode(value);
                                        mp.image.add(ifr.id, ifr);
                                    } else if (name.equals("time")) {
                                        mp.image.get(mp.image.size() - 1).time = Integer.decode(value);
                                    } else {
                                        ImageFrame ifr;
                                        try {
                                            ifr = (ImageFrame) mp.image.get(mp.image.size() - 1);
                                        } catch (Exception e) {
                                            ifr = new ImageFrame();
                                            mp.image.add(ifr);
                                        }
                                        ifr.image = new String(value);
                                        try {
                                            game.gw.render.images.Load(value);
                                        } catch (Exception e) {
                                            System.out.println("Error: " + e.getMessage());
                                        }
                                    }
                                } else if (xpath[5].equals("type")) {
                                    MapField mp = (MapField) fieldtype.get(aId);
                                    mp.type = new String(value);
                                }
                            }
                        } else if (xpath.length > 3 && xpath[3].equals("data")) {
                            if (xpath.length > 4 && xpath[4].equals("field")) {
                                if (content == null) {
                                    try {
                                        content = new MapField[width][height];
                                        for (int z = 0; z < width * height; z++) {
                                            this.content[z % width][z / width] = null;
                                        }
                                    } catch (Exception e) {
                                        throw new Exception("Can't create map");
                                    }
                                }
                                if (xpath.length == 5) {
                                    if (name.equals("x")) {
                                        Ax = Integer.decode(value);
                                    } else if (name.equals("y")) {
                                        Ay = Integer.decode(value);
                                    } else if (name.equals("template")) {
                                        aTemplate = Integer.decode(value);
                                    } else if (name.equals("type")) {
                                        aType = new String(value);
                                        int Tt;
                                        try {
                                            Tt = this.content[Ax][Ay].template;
                                            this.content[Ax][Ay] = createField(value);
                                            this.content[Ax][Ay].template = Tt;
                                            this.content[Ax][Ay].type = new String(value);
                                        } catch (Exception e) {
                                            throw new Exception("Can't set new type");
                                        }
                                    }
                                } else if (xpath[5].equals("fieldinfo")) {
                                    if (xpath[6].equals("name")) {
                                        try {
                                            this.content[Ax][Ay].name = new String(value);
                                        } catch (Exception e) {
                                            throw new Exception("Can't set name");
                                        }
                                    } else if (xpath[6].equals("description")) {
                                        try {
                                            this.content[Ax][Ay].description = new String(value);
                                        } catch (Exception e) {
                                            throw new Exception("Can't set description");
                                        }
                                    }
                                    if (xpath[6].equals("monsters")) {
                                        if (xpath[7].equals("monster")) {
                                            if (xpath.length == 8) {
                                                if (name.equals("type")) {
                                                    try {
                                                        acreature = getNewCreature(value);
                                                        acreature.setpos(Ax, Ay);
                                                        creatures.add(acreature);
                                                        this.content[Ax][Ay].objects.add(acreature);
                                                    } catch (Exception e) {
                                                        throw new Exception("Can't add creature");
                                                    }
                                                }
                                            } else if (xpath[8].equals("name")) {
                                                try {
                                                    ((SimpleMonster) acreature).name = new String(value);
                                                } catch (Exception e) {
                                                    throw new Exception("Can't set name");
                                                }
                                            } else if (xpath[8].equals("health")) {
                                                try {
                                                    ((SimpleMonster) acreature).setHealth(Integer.decode(value));
                                                } catch (Exception e) {
                                                    throw new Exception("Can't set health");
                                                }
                                            } else if (xpath[8].equals("maxhealth")) {
                                                try {
                                                    ((SimpleMonster) acreature).setMaxHealth(Integer.decode(value));
                                                } catch (Exception e) {
                                                    throw new Exception("Can't set max health");
                                                }
                                            } else if (xpath[8].equals("experience")) {
                                                try {
                                                    ((SimpleMonster) acreature).setExperience(Integer.decode(value));
                                                } catch (Exception e) {
                                                    throw new Exception("Can't set experience");
                                                }
                                            } else if (xpath[8].equals("image")) {
                                                try {
                                                    ((SimpleMonster) acreature).setImage(value);
                                                } catch (Exception e) {
                                                    throw new Exception("Can't set image");
                                                }
                                            }
                                        }
                                    } else if (xpath[6].equals("img")) {
                                        MapField mp = (MapField) this.content[Ax][Ay];
                                        if (name.equals("id")) {
                                            ImageFrame ifr = new ImageFrame();
                                            ifr.id = Integer.decode(value);
                                            mp.image.add(ifr.id, ifr);
                                        } else if (name.equals("time")) {
                                            ImageFrame ifr = (ImageFrame) mp.image.get(mp.image.size() - 1);
                                            ifr.time = Integer.decode(value);
                                        } else {
                                            try {
                                                ImageFrame ifr;
                                                try {
                                                    ifr = (ImageFrame) mp.image.get(mp.image.size() - 1);
                                                } catch (Exception e) {
                                                    ifr = new ImageFrame();
                                                    mp.image.add(ifr);
                                                }
                                                ifr.image = new String(value);
                                                try {
                                                    game.gw.render.images.Load(value);
                                                } catch (Exception e) {
                                                    System.out.println("Error: " + e.getMessage());
                                                }
                                            } catch (ArrayIndexOutOfBoundsException e) {
                                            }
                                        }
                                    } else if (xpath[6].equals("tmap")) {
                                        MapField tf = (MapField) this.content[Ax][Ay];
                                        if (name.equals("x")) {
                                            try {
                                                tf.start_x = Integer.decode(value);
                                                throw new Exception("X pos decode error");
                                            } catch (Exception e) {
                                            }
                                        } else if (name.equals("y")) {
                                            try {
                                                tf.start_y = Integer.decode(value);
                                            } catch (Exception e) {
                                                throw new Exception("Y pos decode error");
                                            }
                                        } else {
                                            try {
                                                tf.mapName = new String(value);
                                                throw new Exception("Map name error");
                                            } catch (Exception e) {
                                            }
                                        }
                                    }
                                }
                                if ((Ax >= 0) && (Ay >= 0) && (aTemplate >= 0)) {
                                    try {
                                        this.content[Ax][Ay] = createField(null);
                                        this.content[Ax][Ay].template = aTemplate;
                                        aTemplate = -1;
                                    } catch (Exception e) {
                                        System.out.println("Hmm, couldn't even create mapfield? [" + Ax + "," + Ay + "]" + content[Ax][Ay]);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Exception on XML tag at : " + path + ":" + name + ":" + value + " " + e);
            System.out.println("Exception: " + e.getMessage());
        }
    }

    public void startDocument() throws SAXException {
        System.out.println("--- Map parser v 1.0 ---");
    }

    public void endDocument() throws SAXException {
    }

    public void startElement(String namespaceURI, String lName, String qName, Attributes attrs) throws SAXException {
        aElement = aElement.concat("/" + (("".equals(lName)) ? qName : lName));
        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                String aName = ("".equals(attrs.getLocalName(i))) ? attrs.getQName(i) : attrs.getLocalName(i);
                setMapValue(aElement, aName, attrs.getValue(i));
            }
        }
    }

    public void endElement(String namespaceURI, String sName, String qName) throws SAXException {
        String eName = new String((("".equals(sName)) ? qName : sName));
        if (aElement.endsWith(eName)) {
            String s1 = aElement.substring(0, aElement.length() - (eName.length() + 1));
            if ("".equals(s1) == false) {
                aElement = s1;
            }
        }
    }

    public void characters(char buf[], int offset, int len) throws SAXException {
        String s = new String(buf, offset, len);
        s = s.replaceAll("[\t\n\r]", "");
        if ("".equals(s) == false) {
            setMapValue(aElement, "", s);
        }
    }

    public Map(Game g) {
        width = -1;
        height = -1;
        name = new String("No map loaded");
        System.out.println("Map created");
        content = null;
        fieldtype = new ArrayList<MapField>();
        creatures = new ArrayList<Creature>();
        this.game = g;
    }

    public void Load(String fname) throws Exception {
        File f = null;
        try {
            if ("".equals(fname) || fname == null) throw new Exception();
            System.out.println("Loading mapfile " + fname);
        } catch (Exception e) {
            throw new Exception("File not found");
        }
        aType = null;
        fieldtype.clear();
        creatures.clear();
        aElement = new String("");
        content = null;
        Ax = -1;
        Ay = -1;
        aTemplate = -1;
        SAXParserFactory factory = SAXParserFactory.newInstance();
        data_read = 0;
        URL url = this.game.mainClass.getClassLoader().getResource(fname);
        if (url == null) {
            throw new Exception("Can't load map from : " + fname);
        }
        try {
            SAXParser parser = factory.newSAXParser();
            parser.parse(url.openStream(), this);
        } catch (Exception e) {
            System.out.println("Can't open XML : " + e);
        }
        for (int i = 0; i < fieldtype.size(); i++) {
            System.out.println((MapField) fieldtype.get(i));
        }
        game.player.setpos(start_x, start_y);
        System.out.println("Player starting position set");
        start_x = -1;
        start_y = -1;
        System.out.println("Map \"" + fname + "\" loaded");
    }
}
