package de.peacei.android.foodwatcher.client.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Vector;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;
import de.peacei.android.foodwatcher.data.Meal;
import de.peacei.android.foodwatcher.data.Mensa;
import de.peacei.android.foodwatcher.data.WeekPlan;
import de.peacei.android.foodwatcher.gui.R;

/**
 * @author peacei
 *
 */
public class JsonFoodSource extends AbstractFoodSource {

    private final String FOOD_TITLE = "title";

    private final String FOOD_DESC = "desc";

    private final String FOOD_TYPE = "type";

    private final String FOOD_EXTRA = "extra";

    private final String FOOD_STUDENT_PRICE = "studentprice";

    private final String FOOD_STAFF_PRICE = "staffprice";

    private final String MENU_FOODS = "foods";

    private final String WEEKPLAN_WEEKNUMBER = "weeknumber";

    private final String WEEKPLAN_MENUES = "menues";

    private final Context cxt;

    public JsonFoodSource(Context cxt, Mensa mensa, byte weekNumber, int year) {
        super(mensa, weekNumber, year);
        this.type = "FOODSUPPLIER";
        this.cxt = cxt;
    }

    @Override
    public void processSource() {
        try {
            URL url = new URL(this.mensa.getJsonUrl(weekNumber));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            StringBuilder agentBuilder = new StringBuilder();
            agentBuilder.append(cxt.getString(R.string.app_name)).append(' ').append(cxt.getString(R.string.app_version)).append('|').append(Build.DISPLAY).append('|').append(VERSION.RELEASE).append('|').append(Build.ID).append('|').append(Build.MODEL).append('|').append(Locale.getDefault().getLanguage()).append('-').append(Locale.getDefault().getCountry());
            connection.setRequestProperty("User-Agent", agentBuilder.toString());
            InputStream inStream = connection.getInputStream();
            String response = getStringFromInputStream(inStream);
            JSONObject weekplanJsonObj = new JSONObject(response);
            this.menues = parseWeekplan(weekplanJsonObj);
            this.valuability = WeekPlan.VALUABLE;
        } catch (IOException ex) {
            this.valuability = WeekPlan.NOCON;
            this.menues = null;
        } catch (JSONException ex) {
            this.valuability = WeekPlan.ERROR;
            this.menues = null;
        }
    }

    private String getStringFromInputStream(final InputStream is) throws IOException {
        String line = "";
        StringBuilder builder = new StringBuilder();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        while ((line = rd.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString();
    }

    private Vector<Meal[]> parseWeekplan(JSONObject weekplan) throws JSONException {
        if (this.weekNumber != weekplan.getInt(WEEKPLAN_WEEKNUMBER)) {
            this.valuability = WeekPlan.OUT_OF_DATE;
            return null;
        }
        Vector<Meal[]> menues = new Vector<Meal[]>(0);
        JSONArray jsonMenues = weekplan.getJSONArray(WEEKPLAN_MENUES);
        for (int menueCount = 0; menueCount < jsonMenues.length(); menueCount++) {
            JSONArray jsonMeals = jsonMenues.getJSONObject(menueCount).getJSONArray(MENU_FOODS);
            Meal[] meals = new Meal[jsonMeals.length()];
            for (int mealCount = 0; mealCount < meals.length; mealCount++) {
                if (menueCount == 0) menues.add(mealCount, new Meal[5]);
                JSONObject jsonMeal = jsonMeals.getJSONObject(mealCount);
                Meal meal = new Meal(jsonMeal.getString(FOOD_TITLE), jsonMeal.getString(FOOD_DESC), (byte) jsonMeal.getInt(FOOD_TYPE), (byte) jsonMeal.getInt(FOOD_EXTRA));
                meal.setStaffPrice(jsonMeal.getString(FOOD_STAFF_PRICE));
                meal.setStudentPrice(jsonMeal.getString(FOOD_STUDENT_PRICE));
                menues.get(mealCount)[menueCount] = meal;
            }
        }
        return menues;
    }
}
