package larpplanner.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@DBTable("fic_character")
public class FicCharacter implements DBObject {

    public static final String ID_COL = "char_id";

    public static final String FREEBASEID_COL = "freebase_id";

    public static final String NAME_COL = "char_name";

    public static final String GENDER_COL = "gender";

    @DBCol(ID_COL)
    protected long id;

    @DBCol(FREEBASEID_COL)
    protected String freebase_id;

    @DBCol(NAME_COL)
    protected String name;

    @DBCol(GENDER_COL)
    protected String gender;

    @DBListRelation(table = "char_power", join_col = ID_COL, val_col = "power_name")
    protected List<String> powers;

    @DBListRelation(table = "char_specie", join_col = ID_COL, val_col = "specie_name")
    protected List<String> species;

    @DBListRelation(table = "char_universe", join_col = ID_COL, val_col = "universe_name")
    protected List<String> universes;

    public FicCharacter() {
        this.freebase_id = "";
    }

    /**
	 * Constructor for new FicCharacter, not from freebase
	 */
    public FicCharacter(String name, String gender, List<String> powers, List<String> species, List<String> universes) {
        this.freebase_id = "";
        this.name = name;
        this.gender = gender;
        this.powers = powers;
        this.species = species;
        this.universes = universes;
    }

    public FicCharacter(String freebaseID, String name, String gender, List<String> powers, List<String> species, List<String> universes) {
        this.freebase_id = freebaseID;
        this.name = name;
        this.gender = gender;
        this.powers = powers;
        this.species = species;
        this.universes = universes;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setFreebaseID(String freebaseID) {
        this.freebase_id = freebaseID;
    }

    public String getFreebaseID() {
        return freebase_id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getGender() {
        return gender;
    }

    public void setPowers(List<String> powers) {
        this.powers = powers;
    }

    public List<String> getPowers() {
        return powers;
    }

    public void setSpecies(List<String> species) {
        this.species = species;
    }

    public List<String> getSpecies() {
        return species;
    }

    public void setUniverses(List<String> universes) {
        this.universes = universes;
    }

    public List<String> getUniverses() {
        return universes;
    }

    /**
	 * Get description of the character from freebase api
	 */
    public String getDescriptionFromWeb() {
        String res = "";
        String line;
        res = "Name: " + name + "\n";
        if (gender != null) res += "Gender: " + gender + "\n";
        if (!universes.isEmpty()) res += "Universes: " + universes.toString().subSequence(1, universes.toString().length() - 1) + "\n";
        if (!species.isEmpty()) res += "Species: " + species.toString().substring(1, species.toString().length() - 1) + "\n";
        if (!powers.isEmpty()) res += "Powers: " + powers.toString().substring(1, powers.toString().length() - 1) + "\n";
        if (freebase_id != "") {
            try {
                URL url = new URL("http://api.freebase.com/api/trans/raw" + freebase_id);
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
                line = reader.readLine();
                if (line != null && line.charAt(0) < 128) {
                    res += "\nDescription:\n";
                    res += removeXMLbrackets(line) + "\n";
                    while ((line = reader.readLine()) != null) {
                        res += removeXMLbrackets(line) + "\n";
                    }
                }
                reader.close();
            } catch (IOException e) {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    /**remove max 2 of <...> from the text.*/
    private String removeXMLbrackets(String text) {
        if (!text.contains("<")) return text;
        String res = text.substring(0, text.indexOf('<')) + text.substring(text.indexOf('>') + 1);
        if (!res.contains("<")) return res;
        res = res.substring(0, res.indexOf('<')) + res.substring(res.indexOf('>') + 1);
        return res;
    }

    public String toString() {
        return name;
    }

    public String getFullRepresentitiveString() {
        String str = "" + id + " " + freebase_id + " " + name + " " + gender + " ~ powers=";
        str += powers + " ~ species=" + species + " ~ universes=" + universes;
        return str;
    }
}
