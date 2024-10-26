package de.grogra.ext.sunshine.spectral.colors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

public class ReadFromURL {

    public static void parseString(String str, String name) {
        BufferedReader reader;
        String zeile = null;
        boolean firstL = true;
        int lambda;
        float intens;
        int l_b = 0;
        int l_e = 0;
        HashMap<Integer, Float> curve = new HashMap<Integer, Float>();
        String[] temp;
        try {
            File f = File.createTempFile("tempFile", null);
            URL url = new URL(str);
            InputStream is = url.openStream();
            FileOutputStream os = new FileOutputStream(f);
            byte[] buffer = new byte[0xFFFF];
            for (int len; (len = is.read(buffer)) != -1; ) os.write(buffer, 0, len);
            is.close();
            os.close();
            reader = new BufferedReader(new FileReader(f));
            zeile = reader.readLine();
            lambda = 0;
            while (zeile != null) {
                if (!(zeile.length() > 0 && zeile.charAt(0) == '#')) {
                    zeile = reader.readLine();
                    break;
                }
                zeile = reader.readLine();
            }
            while (zeile != null) {
                if (zeile.length() > 0) {
                    temp = zeile.split(" ");
                    lambda = Integer.parseInt(temp[0]);
                    intens = Float.parseFloat(temp[1]);
                    if (firstL) {
                        firstL = false;
                        l_b = lambda;
                    }
                    curve.put(lambda, intens);
                }
                zeile = reader.readLine();
            }
            l_e = lambda;
        } catch (IOException e) {
            System.err.println("Error2 :" + e);
        }
        try {
            String tempV;
            File file = new File("C:/spectralColors/" + name + ".sd");
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("# COLOR: " + name + " Auto generated File: 02/09/2009; From " + l_b + " to " + l_e);
            bw.newLine();
            bw.write(l_b + "");
            bw.newLine();
            for (int i = l_b; i <= l_e; i++) {
                if (curve.containsKey(i)) {
                    tempV = i + " " + curve.get(i);
                    bw.write(tempV);
                    bw.newLine();
                }
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] arg) {
        for (int i = 0; i < names.length; i++) {
            parseString("http://www.cs.utah.edu/~bes/graphics/spectra/" + names[i] + ".sd", names[i]);
            System.out.println(i + " of " + names.length);
        }
    }

    public static String getNameForID(int id) {
        if (id < 0 && id >= names.length) throw new IllegalArgumentException();
        return names[id];
    }

    static final String[] names = { "01_DARK_SKIN", "02_LIGHT_SKIN", "03_BLUE_SKY", "04_FOLIAGE", "05_BLUE_FLOWER", "06_BLUISH_GREEN", "07_ORANGE", "08_PURPLISH_BLUE", "09_MODERATE_RED", "10_PURPLE", "11_YELLOW_GREEN", "12_ORANGE_YELLOW", "13_BLUE", "14_GREEN", "15_RED", "16_YELLOW", "17_MAGENTA", "18_CYAN", "19_WHITE", "20_NEUTRAL_8", "21_NEUTRAL_65", "22_NEUTRAL_5", "23_NEUTRAL_35", "24_BLACK", "LIGHT_BUFF", "DARK_BLUE_31", "DARK_BLUE_FLEXACHROM", "RED_3", "TOLEDO_RED", "LIGHT_GREEN_184", "PINK_190", "BLACK_EARTH", "CLAY_LOAM", "DRY_SOIL_A", "DRY_SOIL_B", "DRY_SOIL_C", "LIMESTONE", "SAND", "WET_SOIL_A", "WET_SOIL_B", "WET_SOIL_C", "DRYPRESS_BRICK_1", "DRYPRESS_BRICK_2", "DRYPRESS_BRICK_3", "EXTRUDED_BRICK_1", "EXTRUDED_BRICK_2", "SAND_LIME_BRICK", "SOFT_MUD_BRICK", "BLUE_2239", "DARK_RED_3386", "DARK_RED_841", "CREAM_NUCITE", "CREAM_VITROLITE", "GREEN_NUCITE", "GREEN_VITROLITE", "WHITE_VITROLITE", "BRILLIANT_PURPLE", "BRIL_GREENISH_BLUE", "DARK_BLUE", "DARK_GRAYISH_BLUE", "DEEP_PURPLE", "GRAYISH_BLUE", "GRAYPURPLISH_BLUE", "LIGHT_BLUE", "LIGHT_GREENISH_BLUE", "LIGHT_PURPLISH_BLUE", "LIGHT_VIOLET", "MODERATE_BLUE", "MOD_PURPLISH_BLUE", "PALE_BLUE", "PALE_PURPLE", "STRONG_BLUE", "STRONG_GREENISH_BLUE", "STRONG_PURPLISH_BLUE", "STR_REDDISH_PURPLE", "VERY_LIGHT_PURPLE", "VERY_PALE_BLUE", "BLACK", "BROWNISH_BLACK", "DARK_GRAYISH_BROWN", "DARK_GRAYYLLW_BROWN", "DARK_OLIVE_BROWN", "DEEP_REDDISH_BROWN", "GRAYISH_BROWN", "GRAYREDDISH_BROWN", "GRAYYELLOWISH_BROWN", "GREENISH_BLACK", "LIGHT_BROWN", "LIGHT_GRAYRED_BROWN", "LIGHT_OLIVE_BROWN", "LIGHT_REDDISH_BROWN", "LITE_YELLOWISH_BROWN", "LT_GRAYYELLOW_BROWN", "MODERATE_BROWN", "MODERATE_OLIVE_BROWN", "MOD_REDDISH_BROWN", "MOD_YELLOWISH_BROWN", "STRONG_BROWN", "STR_YELLOWISH_BROWN", "BLACKISH_GREEN", "BRILLIANT_GREEN", "BRIL_BLUISH_GREEN", "DARK_GRAYISH_GREEN", "DARK_GRAYISH_OLIVE", "DARK_GREEN", "GRAYISH_GREEN", "GRAYISH_OLIVE", "GRAYISH_OLIVE_GREEN", "GRAYISH_YELLOW_GREEN", "LIGHT_BLUISH_GREEN", "LIGHT_GREEN", "LIGHT_YELLOW_GREEN", "LITE_YELLOWISH_GREEN", "MODERATE_GREEN", "MODERATE_OLIVE", "MODERATE_OLIVE_GREEN", "MODERATE_YELLOW_GREE", "MOD_BLUISH_GREEN", "MOD_YELLOWISH_GREEN", "PALE_GREEN", "PALE_YELLOW_GREEN", "STRONG_YELLOW_GREEN", "STR_YELLOWISH_GREEN", "VERY_LIGHT_GREEN", "VERY_PALE_GREEN", "V_LITE_BLUISH_GREEN", "BRILLIANT_YELLOW", "DARK_ORANGE_YELLOW", "DEEP_ORANGE", "GRAYISH_YELLOW", "GRAYREDDISH_ORANGE", "LIGHT_ORANGE_YELLOW", "LIGHT_YELLOW", "LITE_GREENISH_YELLOW", "MODERATE_ORANGE", "MODERATE_YELLOW", "MOD_ORANGE_YELLOW", "MOD_REDDISH_ORANGE", "PALE_GREENISH_YELLOW", "PALE_ORANGE_YELLOW", "PALE_YELLOW", "STRONG_ORANGE_YELLOW", "VIVID_GREENSH_YELLOW", "VIVID_ORANGE_YELLOW", "VIVID_YELLOW", "BLACKISH_RED", "DARK_GRAYISH_RED", "DARK_RED", "DEEP_RED", "DEEP_YELLOWISH_PINK", "GRAYISH_PURPLISH_RED", "GRAYISH_RED", "GRAYPURPLISH_PINK", "LIGHT_GRAYISH_RED", "LIGHT_YELLOWISH_PINK", "MODERATE_PINK", "MODERATE_RED", "MOD_PURPLISH_PINK", "MOD_YELLOWISH_PINK", "PALE_PURPLISH_PINK", "PALE_YELLOWISH_PINK", "STRONG_RED", "VIVID_REDDISH_PINK", "BLUISH_GRAY", "BLUISH_WHITE", "BROWNISH_GRAY", "DARK_BLUISH_GRAY", "DARK_GRAY", "DARK_GREENISH_GRAY", "GREENISH_GRAY", "GREENISH_WHITE", "LIGHT_BLUISH_GRAY", "LIGHT_BROWNISH_GRAY", "LIGHT_GRAY", "LIGHT_GREENISH_GRAY", "LIGHT_OLIVE_GRAY", "MEDIUM_GRAY", "OLIVE_GRAY", "YELLOWISH_GRAY", "YELLOWISH_WHITE", "BRIGHT_RED", "DEEP_BLUE", "MEDIUM_GREEN", "PEACH", "BLUEGREEN", "GRAY", "OLIVE", "WALNUT", "DARK_BLUE_31", "DARK_BLUE_FLEXACHROM", "LIGHT_STAGE", "RED_3", "SALMON", "TOLEDO_RED", "3000K_TUNGSTEN_INCAN", "COOL_WHITE", "FLUORESCENT_INCANDES", "NATURAL", "SOURCE_A", "SOURCE_B", "SOURCE_C", "SOURCE_D55", "SOURCE_D65", "SOURCE_D75", "WARM_WHITE", "WHITE", "YELLOW_LED", "GREEN_1705", "STANDARD_BROWN", "GREEN_111", "LIGHT_GREEN_3032", "LIGHT_GREEN_44", "BLOSSOM_PINK", "BURGUNDY", "CACTUS_GREEN", "CANARY_YELLOW", "CREAM_GRAY", "IVORY_TAN", "PALE_JADE", "ROSE_TAN", "SAVELITE_GLOSS", "SEMILUSTER", "TERRA_COTTA", "LIGHT_GRAY_AKOUSTO", "AGCL", "AQUA", "ARMY_OLIVE", "ARSENIC", "BISMUTH_TRIIODIDE", "BLACKBODY", "BLUE_SKY", "BLUE_VELVET_CARPET", "BRASS", "BRASS_2", "BRONZE", "CARBON", "CHINA_CLAY", "COPPER", "COPPER_OXIDE_FILM", "COPPER_UNOXIDIZED", "CREAM", "CYAN", "CYAN_2", "DEFAULT", "DIMERCURY_DIBROMIDE", "DIMERCURY_DIIODIDE", "GLOSS", "GOLD", "GOLD_COATING", "GRAPE_ZINC_SULFIDE", "GREEN", "GREEN_SEA", "HOT_RED", "IRON_OXIDE_POWDER", "KRYPTONITE", "LAVENDER", "LUNAR_DUST", "LUNAR_ROCK", "MAHOGANY", "MATTE", "MERCURY_DIIODIDE", "MIRROR", "NICKEL", "NICKEL_OXIDE_POWDER", "OBSIDIAN", "PINK", "RED", "RHENIUM_TRIOXIDE", "RUBBER", "RUBBER_2", "RUBY", "SEMIGLOSS", "SILICON", "SILICON_MONOCARBIDE", "SILICON_OXIDIZED", "SILICON_POLISHED", "SILVER", "STEEL", "TEFLON_2", "TEFLON_COATING", "TIN", "TIN_OXIDE", "TITANIUM_DIOXIDE", "TUNGSTEN", "VINYL", "WINE", "YELLOW", "YELLOW_2", "ZINC_SULFIDE", "ZINC_SULFIDE_2", "ASBESTOS_CEMENT_SIDI", "BLACK_GRANITE", "BLACK_MARBLE", "CONCRETE", "INDIANA_LIMESTONE", "ROSE_GRANITE", "SANDSTONE", "TRAVERTINE", "VEINED_MARBLE", "WHITE_MARBLE", "ALUMINUM_SHEET", "COPPER_BEARING_STEEL", "GALVANIZED_STEEL", "HARD_LEAD", "MAGNESIUM_ALLOY", "ROLLED_ZINC", "SHEET_COPPER", "STAINLESS_STEEL", "DARK_BROWN", "DARK_REDBROWN", "AVERAGE_CAUCASIAN", "DARK_CAUCASIAN", "HINDU", "JAPANESE", "LIGHT_CAUCASIAN", "MULATTO", "BLUE_QUIETONE", "BUFF_SATINCOTE", "GRAY_SATINCOTE", "GREEN_SATINCOTE", "IVORY_ACOUSTONE", "IVORY_FIRTEX", "NATURAL_ABSORBEX", "NATURAL_ACOUSTEX", "NATURAL_ECONACOUST", "ROSE_QUIETONE", "CANARY", "CAPRI_BLUE", "EMBER_RED", "HARNESS_TAN", "HOLLY_RED", "JONQUIL", "ORANGE", "THISTLE_BLUE", "TROPIC_GREEN", "TWILIGHT_BLUE", "AUTUMN_FOREST", "DORMANT_BERMUDA_GRAS", "DORMANT_BLUE_GRASS", "GREEN_BLUE_GRASS", "IVY_LEAF_BACK", "IVY_LEAF_FRONT", "SUMMER_CONIFEROUS", "SUMMER_DECIDUOUS", "WINTER_CONIFEROUS", "BUFF", "LIGHT_GRAY_893", "ROSE", "LIGHT_GREEN_3032", "BLUE", "BLUE_SUNTILE", "CHESTNUT", "CHOCOLATE", "CINNAMON_TAN", "CORALLIN", "COSSACK_DARK_GREEN", "DARK_PURPLE", "DUBONNET", "IVORY", "JONQUIL_YELLOW", "LIGHT_BLUE_451", "LIGHT_BLUE_S7", "LIGHT_GREEN_184", "LIGHT_GREEN_S23", "LIGHT_GREEN_S6", "LIGHT_JADE_GREEN", "LIGHT_LILAC", "LIGHT_LILAC_841", "LIGHT_ORCHID", "NILE_BLUE", "OPAL_GREEN", "ORCHID", "PALE_NILE_BLUE", "PEACH_BLOOM", "PEARL_GRAY", "PINK_190", "PINK_S2", "PRIMROSE_YELLOW", "ROYAL_BLUE", "SATIN_BLACK", "SATIN_WHITE", "TAN", "TAN_S25", "TAN_SUNTILE", "WHITE_SUNTILE", "FRESH_SNOW", "SNOW_COVERED_WICE", "WATER_REFLECTING_SKY", "AUREOLE", "AUTUMN", "CIRCASSIAN_BROWN", "DUSK", "FOREST", "HUNTER_GREEN", "LAUREL", "MIDDAY", "PARCHMENT", "PLAIN_SILVER", "PONGEE", "RUSSET", "SPRINGLEAF", "SUNRISE", "BROWN", "BROWN_42", "WHITE_OAK", "WHITE_OAK_MEDIUM" };
}
