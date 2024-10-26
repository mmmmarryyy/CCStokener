package org.frankkie.parcdroidprj;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;

/**
 * ParcDroid, MainActivity.
 * Start de App, init alles.
 * @author frankkie
 * @see MapActivity
 */
public class MainActivity extends MapActivity {

    public static int kind_id = -1;

    MapView mapView;

    public static final int huidigeAppVersie = 1;

    public static boolean route_displayed = false;

    public static LocationManager locationManager;

    public static Location location;

    public static MapController mapController;

    List<Overlay> mapOverlays;

    Drawable drawable;

    OverlayDing itemizedOverlay;

    LocationListener deListener;

    OverlayDing mijnLokatieOverlay;

    OverlayDinges deOverlayDinges;

    public static Context deContext;

    public static boolean fakeLokatieGebruiken = false;

    /**
   * Als je GPS-Positie veranderd,
   * moet de kaart daar dan wel of niet naar toe scrollen.
   */
    public static boolean scrollToMijnLokatieZodraLokatieIsVeranderdEnRouteAanStaat = true;

    public static OverlayDebugInfo debugOverlay;

    public static int fixTime;

    public static boolean ttsIsInitKlaar = false;

    public static android.speech.tts.TextToSpeech ttsF;

    public static int ttsIsInitKlaarNr = 0;

    public static boolean magBellen = true;

    public static String telefoonNummerOuders = "1244";

    public static String waarZijnDeOuders = "Stoomcarousel";

    public static OverlayPunt waarDeOudersZijnPunt = null;

    public static OverlayRoute oRoute;

    public static ArrayList<RouteMelding> deJuisteRoute = new ArrayList<RouteMelding>();

    public static String batteryLevelStr = "?%";

    public static int batteryLevelInt = -1;

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra("level", 0);
                int scale = intent.getIntExtra("scale", 100);
                batteryLevelStr = (String.valueOf(level * 100 / scale) + "%");
                batteryLevelInt = level * 100 / scale;
                System.out.println("MainActivity.mBatteryInfoReceiver.onReceive: BatteryLevel: " + batteryLevelStr);
            }
        }
    };

    public void sensorFunctie() {
        Intent in = new Intent();
        ComponentName component = new ComponentName("org.frankkie.parcdroidprj", "org.frankkie.parcdroidprj.SensorTester");
        in.setAction("android.intent.action.MAIN");
        in.setComponent(component);
        try {
            startActivity(in);
        } catch (Exception e) {
            System.out.println("kompasFunctie: " + e);
        }
    }

    /**
   * Ga naar...
   * Voert de actie uit die hoort bij
   * de menu-optie 'Ga Naar...'.
   * Het opent een submenu met de opties:
   * "GPS-Positie" en "Efteling".
   * De Map wordt verschoven tot dit punt
   * het midden van de kaart staat.
   */
    public void optieCustomToast() {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_layout, (ViewGroup) findViewById(R.id.toast_layout_root));
        ImageView image = (ImageView) layout.findViewById(R.id.image);
        image.setImageResource(R.drawable.androidmarker);
        TextView text = (TextView) layout.findViewById(R.id.text);
        text.setText("Hello! This is a custom toast!");
        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }

    /**
   * Sluit het programma.
   * Voert de actie uit die hoort bij
   * "Finish" uit het Menu.
   */
    public void optieFinish() {
        System.out.println();
        ttsF.shutdown();
        this.finish();
    }

    public void opOudersZoekenGeklikt() {
        if (!OverlayRoute.isRouteLatenZien()) {
            if (waarZijnDeOuders.equals("Stoomcarousel")) {
                waarDeOudersZijnPunt = OverlayDinges.gpsPlekken.get(94);
            } else if (waarZijnDeOuders.equals("Hoofdingang")) {
                waarDeOudersZijnPunt = OverlayDinges.gpsPlekken.get(2);
            } else if (waarZijnDeOuders.equals("Mijn Huis")) {
                for (int a = 0; a < OverlayDinges.gpsPlekken.size(); a++) {
                    if (OverlayDinges.gpsPlekken.get(a).volgnummer == 914) {
                        waarDeOudersZijnPunt = OverlayDinges.gpsPlekken.get(a);
                    }
                }
            }
            deOverlayDinges.berekenAfstandenTotLijnen();
            List<OverlayPunt> puntenLijst = Dijkstra.doeDijkstra(waarDeOudersZijnPunt);
            deJuisteRoute = OverlayRoute.maakRouteMeldingenLijst(puntenLijst);
            Toast.makeText(getApplicationContext(), "Ouders Zoeken...", Toast.LENGTH_SHORT).show();
        }
        OverlayRoute.setRouteLatenZien(!OverlayRoute.isRouteLatenZien());
        oRoute.lokatieIsVeranderd();
        mapController.scrollBy(1, 1);
        mapController.scrollBy(-1, -1);
    }

    /**
   * Zoekt de juiste actie bij een menu-optie
   * @param item
   * @return boolean of de menu-optie is gebruikt.
   */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        System.out.println("Option: " + item.getItemId());
        switch(item.getItemId()) {
            case 99:
                {
                    opOudersZoekenGeklikt();
                    return true;
                }
            case 1:
                {
                    optieCustomToast();
                    return true;
                }
            case 2:
                {
                    optieFinish();
                    return true;
                }
            case 3:
                {
                    System.out.println("Map-animateTo(location)");
                    mapController.setCenter(OverlayDing.getPoint(location.getLatitude(), location.getLongitude()));
                    return true;
                }
            case 4:
                {
                    System.out.println("Map-scrollTo(Efteling)");
                    mapController.setCenter(OverlayDing.getPoint(51.651123, 5.048898));
                    return true;
                }
            case 5:
                {
                    toggleDeFakeLokatie();
                    return true;
                }
            case 6:
                {
                    fakeLokatieInvullen();
                    return false;
                }
            case 7:
                {
                    toggleDebugLayer();
                    return true;
                }
            case 8:
                {
                    showDialog(2);
                    return true;
                }
            case 9:
                {
                    oudersBellen();
                    return true;
                }
            case 10:
                {
                    instellingenDialogLatenZien();
                    return true;
                }
            case 11:
                {
                    sensorFunctie();
                    return true;
                }
            case 12:
                {
                    checkVoorUpdates();
                    return true;
                }
            case 13:
                {
                    doeAboutDialog();
                    return true;
                }
            case 14:
                {
                    startVibrate();
                    return true;
                }
            case 15:
                {
                    stopVibrate();
                    return true;
                }
            case 16:
                {
                    geluidAfspelen();
                    return true;
                }
            case 17:
                {
                    mapView.setSatellite(true);
                    return true;
                }
            case 18:
                {
                    mapView.setSatellite(false);
                    return true;
                }
            case 19:
                {
                    Dijkstra.startDijkstra();
                }
            case 21:
                {
                    doeNotitieDialog();
                }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
   * Een Notitie Maken,
   * die kan worden gelezen via de ParcDroid-website
   */
    public void doeNotitieDialog() {
        showDialog(11);
    }

    public void geluidAfspelen() {
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setDataSource("/sdcard/parcdroid/marioriff.wav");
            mp.prepare();
            mp.start();
        } catch (Exception e) {
        }
    }

    public void startVibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(10 * 1000);
    }

    public void startVibrate(long tijd) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(tijd);
    }

    public void stopVibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.cancel();
    }

    public void doeAboutDialog() {
        showDialog(5);
    }

    public void checkVoorUpdates() {
        String updateMelding = "";
        try {
            updateMelding = haalPaginaOp("www.frankkie.nl", "/parcdroid/update.php?versie=" + huidigeAppVersie);
        } catch (Exception ex) {
            System.out.println("Fout bij: checkVoorUpdates(): " + ex);
            Toast.makeText(getApplicationContext(), "Er is een fout opgetreden, probeer het later opnieuw", Toast.LENGTH_LONG).show();
            return;
        }
        if (!updateMelding.equals("oke")) {
            Toast.makeText(getApplicationContext(), "Er is een Nieuwere versie Beschikbaar !!\n(" + updateMelding + ")", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Dit is de Nieuwste Versie :-) !\n(" + updateMelding + ")", Toast.LENGTH_LONG).show();
        }
    }

    public void instellingenDialogLatenZien() {
        showDialog(3);
    }

    public void oudersBellen() {
        if (magBellen) {
            Intent a = new Intent();
            a.setAction(Intent.ACTION_CALL);
            a.setData(Uri.parse("tel:" + telefoonNummerOuders));
            startActivity(a);
        } else {
            Toast.makeText(getApplicationContext(), "Je mag niet Bellen !", Toast.LENGTH_LONG).show();
        }
    }

    public void toggleDebugLayer() {
        OverlayDebugInfo.drawDebugLayer = !OverlayDebugInfo.drawDebugLayer;
        mapController.scrollBy(1, 1);
        mapController.scrollBy(-1, -1);
    }

    public void toggleDeFakeLokatie() {
        fakeLokatieGebruiken = !fakeLokatieGebruiken;
        if (fakeLokatieGebruiken) {
            locationManager.removeUpdates(deListener);
            Toast.makeText(getApplicationContext(), "Fake Lokatie staat nu AAN", Toast.LENGTH_SHORT).show();
        } else {
            locationManager.removeUpdates(deListener);
            long minTime = 1;
            float minDistance = 1;
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, deListener);
            Toast.makeText(getApplicationContext(), "Fake Lokatie staat nu UIT", Toast.LENGTH_LONG).show();
        }
        mapController.scrollBy(1, 1);
        mapController.scrollBy(-1, -1);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final int fake_location = 1;
        final int speech = 2;
        final int instellingen = 3;
        final int kind_idDialog = 4;
        final int about_dialog = 5;
        final int routeDingesDialog = 10;
        final int notitieDialog = 11;
        final Dialog dialog = new Dialog(this, 0);
        if (id == fake_location) {
            dialog.setContentView(R.layout.fake_location_dialog);
            dialog.setTitle("Fake Location Dialog");
            final EditText latitudeT = (EditText) dialog.findViewById(R.id.fake_location_latitude);
            final EditText longtitudeT = (EditText) dialog.findViewById(R.id.fake_location_longtitude);
            Button buttonT = (Button) dialog.findViewById(R.id.fake_location_button);
            View.OnClickListener aaaa = new View.OnClickListener() {

                public void onClick(View arg0) {
                    if (fakeLokatieGebruiken) {
                        double latii = Double.parseDouble(latitudeT.getText().toString());
                        location.setLatitude(latii);
                        double longg = Double.parseDouble(longtitudeT.getText().toString());
                        location.setLongitude(longg);
                        System.out.println("FAKE LOCATION: " + latii + ", " + longg);
                        deLokatieIsVeranderd(location);
                        Toast.makeText(getApplicationContext(), "Fake Lokatie is Aangepast :P", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "FAKE LOKATIE GEBRUIKEN STAAT UIT !", Toast.LENGTH_LONG).show();
                    }
                    dialog.dismiss();
                }
            };
            buttonT.setOnClickListener(aaaa);
            Button mapCenterKnop = (Button) dialog.findViewById(R.id.fake_location_mapcenter);
            mapCenterKnop.setOnClickListener(new View.OnClickListener() {

                public void onClick(View arg0) {
                    if (fakeLokatieGebruiken) {
                        double latii = 0.0 + (mapView.getMapCenter().getLatitudeE6() / 1E6);
                        location.setLatitude(latii);
                        double longg = 0.0 + (mapView.getMapCenter().getLongitudeE6() / 1E6);
                        location.setLongitude(longg);
                        System.out.println("FAKE LOCATION: " + latii + ", " + longg);
                        deLokatieIsVeranderd(location);
                        Toast.makeText(getApplicationContext(), "Fake Lokatie is Aangepast :P", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "FAKE LOKATIE GEBRUIKEN STAAT UIT !", Toast.LENGTH_LONG).show();
                    }
                    dialog.dismiss();
                }
            });
        }
        if (id == speech) {
            dialog.setContentView(R.layout.speech_dialog);
            dialog.setTitle("Text-To-Speech Dialog");
            final EditText deTekst = (EditText) dialog.findViewById(R.id.speech_text);
            Button praatKnop = (Button) dialog.findViewById(R.id.speech_speak_button);
            Button sluitKnop = (Button) dialog.findViewById(R.id.speech_close_button);
            praatKnop.setOnClickListener(new View.OnClickListener() {

                public void onClick(View arg0) {
                    String aa = deTekst.getText().toString();
                    ttsF.speak(aa, TextToSpeech.QUEUE_ADD, new HashMap<String, String>());
                }
            });
            sluitKnop.setOnClickListener(new View.OnClickListener() {

                public void onClick(View arg0) {
                    dialog.dismiss();
                }
            });
        }
        if (id == instellingen) {
        }
        if (id == kind_idDialog) {
            dialog.setContentView(R.layout.kind_id_layout);
            dialog.setTitle("Belangrijk");
            TextView deTextView = (TextView) dialog.findViewById(R.id.text_van_kind_id_dialog);
            deTextView.setText("Code: " + kind_id + "\nOnthoud deze code !");
            Button deOkKnop = (Button) dialog.findViewById(R.id.button_van_kind_id_dialog);
            deOkKnop.setOnClickListener(new View.OnClickListener() {

                public void onClick(View arg0) {
                    dialog.dismiss();
                }
            });
        }
        if (id == about_dialog) {
            dialog.setContentView(R.layout.about_dialog);
            dialog.setTitle("       About...       ");
        }
        if (id == notitieDialog) {
            dialog.setTitle("     NotitieDialog     ");
            dialog.setContentView(R.layout.notitie_dialog);
            final EditText notitieEditText = (EditText) dialog.findViewById(R.id.notitie_edittext);
            Button deKnop = (Button) dialog.findViewById(R.id.notitie_button);
            deKnop.setOnClickListener(new View.OnClickListener() {

                public void onClick(View arg0) {
                    try {
                        String deStrVanNotitie = notitieEditText.getText().toString();
                        deStrVanNotitie = deStrVanNotitie.replace(" ", "+");
                        String meldResponse = haalPaginaOp("www.frankkie.nl", "/parcdroid/meld.php?a=1&id=" + kind_id + "&lat=" + location.getLatitude() + "&long=" + location.getLongitude() + "&bat=" + batteryLevelInt + "&not=" + deStrVanNotitie);
                        if (!meldResponse.contains("ok\n")) {
                            drukaf("Dr is iets fout bij het melden van de huidige lokatie EN Notitie :'-( \n" + meldResponse);
                        }
                        Toast.makeText(getApplicationContext(), "Notitie Gemaakt\n" + notitieEditText.getText().toString(), Toast.LENGTH_LONG).show();
                    } catch (Exception ex) {
                        Toast.makeText(getApplicationContext(), "Notitie Mislukt...", Toast.LENGTH_LONG).show();
                        ex.printStackTrace();
                    }
                    dialog.dismiss();
                }
            });
        }
        return dialog;
    }

    @Override
    protected void onPrepareDialog(int id, final Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        final int kind_idDialog = 4;
        final int instellingen = 3;
        if (id == instellingen) {
            dialog.setContentView(R.layout.instellingen_dialog);
            dialog.setTitle("              Instellingen              ");
            final Spinner s = (Spinner) dialog.findViewById(R.id.instellingen_waar_spinner);
            try {
                TextView tvtje = (TextView) dialog.findViewById(R.id.instellingen_kind_id);
                tvtje.setText("Kind_Code: " + kind_id);
                ArrayAdapter adapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.planets, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                if (s == null) {
                    System.out.println("(Spinner) s == null !!!");
                }
                if (adapter == null) {
                    System.out.println("adapter == null !!!");
                }
                s.setAdapter(adapter);
                int nr = -1;
                for (int a = 0; a < adapter.getCount(); a++) {
                    if (waarZijnDeOuders.equals((String) adapter.getItem(a))) {
                        nr = a;
                    }
                }
                s.setSelection(nr);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Button opslaanKnop = (Button) dialog.findViewById(R.id.instellingen_save_button);
            Button cancelKnop = (Button) dialog.findViewById(R.id.instellingen_cancel_button);
            final CheckBox magBellenCheckBox = (CheckBox) dialog.findViewById(R.id.instellingen_magbellen);
            final EditText telefoonnr = (EditText) dialog.findViewById(R.id.instellingen_oudersnummer);
            telefoonnr.setText(telefoonNummerOuders);
            magBellenCheckBox.setChecked(magBellen);
            final CheckBox kaartScrollt = (CheckBox) dialog.findViewById(R.id.instellingen_volggpsalsroute);
            kaartScrollt.setChecked(scrollToMijnLokatieZodraLokatieIsVeranderdEnRouteAanStaat);
            final EditText puntenAfstand = (EditText) dialog.findViewById(R.id.instellingen_afstandpunt);
            puntenAfstand.setText(Double.toString(OverlayRoute.deAfstandDieJeVanHetPuntMoetZijnOmNaarHetVolgendePuntTeGaan), TextView.BufferType.NORMAL);
            opslaanKnop.setOnClickListener(new View.OnClickListener() {

                public void onClick(View arg0) {
                    waarZijnDeOuders = (s.getAdapter().getItem(s.getSelectedItemPosition())).toString();
                    if (waarZijnDeOuders.equals("Stoomcarousel")) {
                        waarDeOudersZijnPunt = OverlayDinges.gpsPlekken.get(14);
                    } else if (waarZijnDeOuders.equals("Hoofdingang")) {
                        waarDeOudersZijnPunt = OverlayDinges.gpsPlekken.get(2);
                    } else if (waarZijnDeOuders.equals("Mijn Huis")) {
                        for (int a = 0; a < OverlayDinges.gpsPlekken.size(); a++) {
                            if (OverlayDinges.gpsPlekken.get(a).volgnummer == 915) {
                                waarDeOudersZijnPunt = OverlayDinges.gpsPlekken.get(a);
                            }
                        }
                    }
                    List<OverlayPunt> puntenLijst = Dijkstra.doeDijkstra(waarDeOudersZijnPunt);
                    deJuisteRoute = OverlayRoute.maakRouteMeldingenLijst(puntenLijst);
                    deLokatieIsVeranderd(location);
                    mapController.scrollBy(-1, -1);
                    mapController.scrollBy(1, 1);
                    telefoonNummerOuders = telefoonnr.getText().toString();
                    magBellen = magBellenCheckBox.isChecked();
                    scrollToMijnLokatieZodraLokatieIsVeranderdEnRouteAanStaat = kaartScrollt.isChecked();
                    try {
                        String afstandStr = puntenAfstand.getText().toString();
                        System.out.println("(Instellingen Dialog) Afstand Tot Punt: " + afstandStr);
                        double afstD = Double.parseDouble(afstandStr);
                        OverlayRoute.deAfstandDieJeVanHetPuntMoetZijnOmNaarHetVolgendePuntTeGaan = afstD;
                    } catch (Exception ex) {
                        System.out.println("(Instellingen Dialog) Exception ! puntenafstand");
                        ex.printStackTrace();
                    }
                    dePropertiesBijwerken();
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Instellingen zijn Opgeslagen", Toast.LENGTH_LONG).show();
                }
            });
            cancelKnop.setOnClickListener(new View.OnClickListener() {

                public void onClick(View arg0) {
                    telefoonnr.setText(telefoonNummerOuders);
                    magBellenCheckBox.setChecked(magBellen);
                    dialog.dismiss();
                }
            });
        }
        if (id == kind_idDialog) {
            dialog.setContentView(R.layout.kind_id_layout);
            dialog.setTitle("Belangrijk");
            TextView deTextView = (TextView) dialog.findViewById(R.id.text_van_kind_id_dialog);
            deTextView.setText("Code: " + kind_id + "\nOnthoud deze code !\n\nDeze code is ook te zien bij instellingen");
            Button deOkKnop = (Button) dialog.findViewById(R.id.button_van_kind_id_dialog);
            deOkKnop.setOnClickListener(new View.OnClickListener() {

                public void onClick(View arg0) {
                    dialog.dismiss();
                }
            });
        }
        if (id == 10) {
            Toast.makeText(getApplicationContext(), "Deze Dailog wordt niet meer gebruikt!", Toast.LENGTH_LONG).show();
        }
    }

    public void fakeLokatieInvullen() {
        showDialog(1);
    }

    /**
   * Maakt het menu.
   * Wordt getoont als er op de
   * (Hardware)Menu-knop wordt gedrukt.
   * @param menu
   * @return boolean result = super.onCreateOptionsMenu(menu);
   */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(0, 99, 0, "Naar Ouders Toe!").setIcon(R.drawable.awesome_klein);
        SubMenu menuGaNaar = menu.addSubMenu("Ga naar...").setIcon(android.R.drawable.ic_menu_mylocation);
        menuGaNaar.add(0, 3, 0, "GPS Positie");
        menuGaNaar.add(0, 4, 0, "Efteling");
        menu.add(0, 9, 0, "Ouders Bellen").setIcon(R.drawable.ic_menu_call);
        SubMenu menuKaartSoort = menu.addSubMenu("Kaart Soort...").setIcon(R.drawable.ic_menu_mapmode);
        menuKaartSoort.add(0, 17, 0, "Sataliet");
        menuKaartSoort.add(0, 18, 0, "Kaart");
        menu.add(0, 2, 0, R.string.menu_optie_finish).setIcon(R.drawable.ic_menu_close_clear_cancel);
        menu.add(0, 10, 0, "Instellingen...").setIcon(R.drawable.ic_menu_preferences);
        menu.add(0, 12, 0, "Check voor Updates...");
        menu.add(0, 13, 0, "About...").setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(0, 1, 0, R.string.menu_optie_custom_toast);
        menu.add(0, 7, 0, "Toggle DebugLayer");
        SubMenu menuFakeLokatie = menu.addSubMenu("Fake Lokatie...");
        menuFakeLokatie.add(0, 5, 0, "Toggle");
        menuFakeLokatie.add(0, 6, 0, "Kies Locatie invoeren...");
        menu.add(0, 21, 0, "Notitie Maken");
        menu.add(0, 8, 0, "Speech Dialog");
        menu.add(0, 11, 0, "SensorTester...");
        SubMenu menuVibrate = menu.addSubMenu("Vibrate...");
        menuVibrate.add(0, 14, 0, "Start (stopt na 10 sec)");
        menuVibrate.add(0, 15, 0, "Stop");
        menu.add(0, 16, 0, "Geluid afspelen");
        menu.add(0, 19, 0, "Dijksta (kijk in DDMS!)");
        return result;
    }

    public void maakDeOverlays() {
        GeoPoint stoomCaroeselPoint = OverlayDing.getPoint(51.651123, 5.048898);
        mapOverlays = mapView.getOverlays();
        drawable = this.getResources().getDrawable(R.drawable.demarker50);
        itemizedOverlay = new OverlayDing(drawable, this);
        OverlayItem stoomCaroeselItem = new OverlayItem(stoomCaroeselPoint, "StoomCaroesel", "De StoomCaroesel staat hier");
        itemizedOverlay.addOverlay(stoomCaroeselItem);
        GeoPoint point = new GeoPoint(19240000, -99120000);
        OverlayItem overlayitem = new OverlayItem(point, "Mexico", "Mexico !");
        itemizedOverlay.addOverlay(overlayitem);
        GeoPoint point2 = new GeoPoint(35410000, 139460000);
        OverlayItem overlayitem2 = new OverlayItem(point2, "Japan", "Japan !");
        itemizedOverlay.addOverlay(overlayitem2);
        deOverlayDinges = new OverlayDinges();
        OverlayDinges overlaydinges = deOverlayDinges;
        mapOverlays.add(overlaydinges);
        mapOverlays.add(itemizedOverlay);
        Drawable androidDrawable = this.getResources().getDrawable(R.drawable.androidmarker);
        mijnLokatieOverlay = new OverlayDing(androidDrawable, this);
        if (location == null) {
            location = locationManager.getLastKnownLocation("gps");
            if (location == null) {
                location = new Location("gps");
            }
        }
        if (location == null) {
            System.out.println("Location == null !!!");
        } else {
            System.out.println("Location != null :P");
        }
        deLokatieIsVeranderd(location);
        mapOverlays.add(mijnLokatieOverlay);
        debugOverlay = new OverlayDebugInfo();
        mapOverlays.add(debugOverlay);
        oRoute = new OverlayRoute(this);
        mapOverlays.add(oRoute);
    }

    public void doeLocationManagerMaken() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled("gps")) {
            Toast.makeText(getApplicationContext(), "GPS staat niet aan !!!", Toast.LENGTH_LONG).show();
            this.finish();
        }
        fixTime = locationManager.getGpsStatus(null).getTimeToFirstFix();
        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        deListener = new LocationListener() {

            public void onLocationChanged(Location arg0) {
                System.out.println("onLocationChanged");
                if (!fakeLokatieGebruiken) {
                    deLokatieIsVeranderd(arg0);
                }
            }

            public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
                System.out.println("onStatusChanged");
                GpsStatus huidigeGPS_Status = locationManager.getGpsStatus(null);
                fixTime = huidigeGPS_Status.getTimeToFirstFix();
                System.out.println("Huidige GPS Status: fixTime=" + fixTime + "&maxSatallites=" + huidigeGPS_Status.getMaxSatellites());
            }

            public void onProviderEnabled(String arg0) {
                System.out.println("onProviderEnabled");
            }

            public void onProviderDisabled(String arg0) {
                System.out.println("onProviderDisabled");
            }
        };
        long minTime = 1;
        float minDistance = 1;
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, deListener);
    }

    /**
   *
   *
   * @param a
   */
    @Override
    protected void onCreate(Bundle a) {
        super.onCreate(a);
        System.setErr(new PrintStream(new SystemOut("System.err")));
        System.setOut(new PrintStream(new SystemOut("System.out")));
        System.out.println("ParcDroid.MainActivity.onCreate");
        deContext = getApplicationContext();
        this.setContentView(R.layout.main);
        mapView = (MapView) findViewById(R.id.mapview);
        if (mapView == null) {
            System.out.println("mapView == null !!!!!!!!!!!");
        } else {
            System.out.println("mapView != null :P");
        }
        try {
            mapView.setBuiltInZoomControls(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            mapController = mapView.getController();
        } catch (Exception ex) {
            drukaf("Fout: " + ex);
        }
        GeoPoint stoomCaroeselPoint = OverlayDing.getPoint(51.651123, 5.048898);
        mapController.setCenter(stoomCaroeselPoint);
        mapController.setZoom(16);
        System.out.println("onCreate: mapView.setSatellite(false);");
        mapView.setSatellite(false);
        doeLocationManagerMaken();
        checkOfSoundFileBestaat();
        maakDeOverlays();
        deProperties();
        startTTS();
    }

    public void checkOfSoundFileBestaat() {
        File file = new File("/sdcard/parcdroid/marioriff.wav");
        if (!file.exists()) {
            System.out.println("Sound: /sdcard/parcdroid/marioriff.wav; !exists");
            InputStream fIn = getBaseContext().getResources().openRawResource(R.raw.marioriff);
            try {
                int size = fIn.available();
                byte[] buffer = new byte[size];
                fIn.read(buffer);
                fIn.close();
                File rtNew = new File("/sdcard/parcdroid/marioriff.wav");
                rtNew.createNewFile();
                FileOutputStream rtFOS = new FileOutputStream(rtNew);
                rtFOS.write(buffer);
                rtFOS.flush();
                rtFOS.close();
            } catch (Exception ex) {
                System.out.println("Fout bij: MainActivity.checkOfSoundFileBestaat()");
                ex.printStackTrace();
            }
        }
    }

    public void startTTS() {
        android.speech.tts.TextToSpeech.OnInitListener initListenerF = new OnInitListener() {

            public void onInit(int arg0) {
                ttsIsInitKlaar = true;
                ttsIsInitKlaarNr++;
                mapController.scrollBy(1, 1);
                mapController.scrollBy(-1, -1);
            }
        };
        ttsF = new android.speech.tts.TextToSpeech(getApplicationContext(), initListenerF);
        if (ttsF.isLanguageAvailable(new Locale("nl", "NL")) == TextToSpeech.LANG_AVAILABLE) {
            System.out.println("Lang NL is Avaliable !!");
            ttsF.setLanguage(new Locale("nl", "NL"));
        } else {
            System.out.println("Lang NL is NIET Avaliable !!");
            ttsF.setLanguage(Locale.US);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ttsF.shutdown();
        locationManager.removeUpdates(deListener);
    }

    @Override
    protected boolean isRouteDisplayed() {
        return route_displayed;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBatteryInfoReceiver);
        locationManager.removeUpdates(deListener);
        long minTime = 3000;
        float minDistance = 5;
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, deListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(deListener);
        long minTime = 3000;
        float minDistance = 5;
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, deListener);
        oRoute.deStopVanOverlayRoute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        oRoute.deResumeVanOverlayRoute();
        if (deListener != null) {
            try {
                locationManager.removeUpdates(deListener);
            } catch (Exception e) {
            }
        } else {
            System.out.println("deListener == null ! :( !! ");
        }
        long minTime = 1;
        float minDistance = 1;
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, deListener);
    }

    public void maakTosti(OverlayItem ding) {
        Toast.makeText(getApplicationContext(), "" + ding.getTitle() + "\n" + ding.getSnippet(), Toast.LENGTH_LONG).show();
    }

    public void deLokatieIsVeranderd(Location lo) {
        System.out.println("MainAcitivity.deLokatieIsVeranderd");
        location = lo;
        try {
            mijnLokatieOverlay.wisDeOverlays();
            System.out.println("Lokatie: " + location.getLatitude() + " " + location.getLongitude());
            GeoPoint mijnPoint = new GeoPoint((int) (location.getLatitude() * 1E6), (int) (location.getLongitude() * 1E6));
            OverlayItem mijnLokatieItem = new OverlayItem(mijnPoint, "HIER", "Hier ben je !");
            mijnLokatieOverlay.addOverlay(mijnLokatieItem);
            System.out.println("MainActivity.deLokatieIsVeranderd --> afstand tot lijnen laten berekenen....");
            deOverlayDinges.berekenAfstandenTotLijnen();
            if (oRoute != null) {
                oRoute.lokatieIsVeranderd();
            }
            mapController.scrollBy(1, 1);
            mapController.scrollBy(-1, -1);
            String meldResponse = haalPaginaOp("www.frankkie.nl", "/parcdroid/meld.php?a=1&id=" + kind_id + "&lat=" + lo.getLatitude() + "&long=" + lo.getLongitude() + "&bat=" + batteryLevelInt + "&not=");
            if (!meldResponse.contains("ok\n")) {
                drukaf("Dr is iets fout bij het melden van de huidige lokatie :'-( \n" + meldResponse);
            }
            if (scrollToMijnLokatieZodraLokatieIsVeranderdEnRouteAanStaat && OverlayRoute.isRouteLatenZien()) {
                mapController.setCenter(mijnPoint);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String haalPaginaOp(String domein, String target) throws Exception {
        String ans = "";
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUserAgent(params, "HttpComponents/1.1");
        HttpProtocolParams.setUseExpectContinue(params, true);
        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new RequestContent());
        httpproc.addInterceptor(new RequestTargetHost());
        httpproc.addInterceptor(new RequestConnControl());
        httpproc.addInterceptor(new RequestUserAgent());
        httpproc.addInterceptor(new RequestExpectContinue());
        HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
        BasicHttpContext context = new BasicHttpContext(null);
        HttpHost host = new HttpHost(domein, 80);
        DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
        ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();
        context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);
        try {
            if (!conn.isOpen()) {
                Socket socket = new Socket(host.getHostName(), host.getPort());
                conn.bind(socket, params);
            }
            BasicHttpRequest request = new BasicHttpRequest("GET", target);
            System.out.println(">> Request URI: " + request.getRequestLine().getUri());
            request.setParams(params);
            httpexecutor.preProcess(request, httpproc, context);
            HttpResponse response = httpexecutor.execute(request, conn, context);
            response.setParams(params);
            httpexecutor.postProcess(response, httpproc, context);
            System.out.println("<< Response: " + response.getStatusLine());
            String foobar = (EntityUtils.toString(response.getEntity()));
            ans = foobar;
            System.out.println(foobar);
            System.out.println("ans:\n" + ans);
            System.out.println("==============");
            if (!connStrategy.keepAlive(response, context)) {
                conn.close();
            } else {
            }
        } finally {
            conn.close();
        }
        return ans;
    }

    public static void drukaf(String string) {
        System.out.println(string);
    }

    public void dePropertiesBijwerken() {
        String propertiesFileString = "props.properties";
        String deMapString = "/sdcard/parcdroid";
        File hetPropertiesBestand = new File(deMapString + "/" + propertiesFileString);
        Properties props = new Properties();
        try {
            FileInputStream in = new FileInputStream(hetPropertiesBestand);
            props.load(in);
            in.close();
        } catch (Exception ex) {
            System.out.println("Fout bij het lezen van de Properties-file\n(file: " + propertiesFileString + ")\nFout: " + ex);
        }
        props.setProperty("kind_id", Integer.toString(kind_id));
        props.setProperty("waarzijnouders", waarZijnDeOuders);
        props.setProperty("telefoonnummer", telefoonNummerOuders);
        props.setProperty("puntenafstand", Double.toString(oRoute.afstandTotPunt));
        props.setProperty("scrollKaartAlsRouteAanStaat", Boolean.toString(scrollToMijnLokatieZodraLokatieIsVeranderdEnRouteAanStaat));
        props.setProperty("magbellen", Boolean.toString(magBellen));
        try {
            FileOutputStream file_out = new FileOutputStream(hetPropertiesBestand);
            props.store(file_out, "#ParcDroid");
            file_out.close();
        } catch (IOException ex) {
            drukaf("deProperties fout   ....:" + ex);
        }
    }

    public boolean deProperties() {
        boolean debug = true;
        String propertiesFileString = "props.properties";
        String deMapString = "/sdcard/parcdroid";
        if (debug) {
            drukaf("properties...");
        }
        File deMap = new File(deMapString);
        if (deMap.exists() && deMap.isDirectory()) {
            drukaf("Map Bestaat :-) (" + deMapString + ")");
        } else {
            boolean gelukt = deMap.mkdir();
            if (gelukt) {
                drukaf("Het is gelukt om de map aan te maken (" + deMapString + ")");
            } else {
                drukaf("Het is !NIET! gelukt om de map aan te maken (" + deMapString + ")");
                return false;
            }
        }
        File hetPropertiesBestand = new File(deMapString + "/" + propertiesFileString);
        if (hetPropertiesBestand.exists()) {
            drukaf("het bestand Bestaat ;-) (" + deMapString + "/" + propertiesFileString + ")");
        } else {
            try {
                boolean gelukt = hetPropertiesBestand.createNewFile();
                if (gelukt) {
                    drukaf("Bestand is gemaakt :-) (" + deMapString + "/" + propertiesFileString + ")");
                } else {
                    drukaf("Bestand kon !NIET! worden gemaakt (" + deMapString + "/" + propertiesFileString + ")");
                    return false;
                }
            } catch (IOException ex) {
                drukaf("fout bij: properties: " + ex);
                drukaf("Dus het Bestand kon !NIET! worden gemaakt (" + deMapString + "/" + propertiesFileString + ")");
                return false;
            }
        }
        Properties props = new Properties();
        try {
            FileInputStream in = new FileInputStream(hetPropertiesBestand);
            props.load(in);
            in.close();
        } catch (Exception ex) {
            System.out.println("Fout bij het lezen van de Properties-file\n(file: " + propertiesFileString + ")\nFout: " + ex);
            return false;
        }
        if (props.getProperty("kind_id") != null && isInt(props.getProperty("kind_id"))) {
            kind_id = Integer.parseInt(props.getProperty("kind_id"));
        } else {
            erIsGeenKindId();
        }
        props.setProperty("kind_id", Integer.toString(kind_id));
        if (props.getProperty("waarzijnouders") != null) {
            waarZijnDeOuders = props.getProperty("waarzijnouders");
        } else {
            waarZijnDeOuders = "Stoomcarousel";
        }
        props.setProperty("waarzijnouders", waarZijnDeOuders);
        if (props.getProperty("telefoonnummer") != null) {
            telefoonNummerOuders = props.getProperty("telefoonnummer");
        } else {
            telefoonNummerOuders = "1244";
        }
        props.setProperty("telefoonnummer", telefoonNummerOuders);
        if (props.getProperty("magbellen") != null) {
            magBellen = Boolean.parseBoolean(props.getProperty("magbellen"));
        } else {
            magBellen = true;
        }
        props.setProperty("magbellen", Boolean.toString(magBellen));
        if (props.getProperty("puntenafstand") != null) {
            oRoute.afstandTotPunt = Double.parseDouble(props.getProperty("puntenafstand"));
        } else {
            oRoute.afstandTotPunt = 15;
        }
        props.setProperty("puntenafstand", Double.toString(oRoute.afstandTotPunt));
        if (props.getProperty("scrollKaartAlsRouteAanStaat") != null) {
            scrollToMijnLokatieZodraLokatieIsVeranderdEnRouteAanStaat = Boolean.parseBoolean(props.getProperty("scrollKaartAlsRouteAanStaat"));
        } else {
            scrollToMijnLokatieZodraLokatieIsVeranderdEnRouteAanStaat = true;
        }
        props.setProperty("scrollKaartAlsRouteAanStaat", Boolean.toString(scrollToMijnLokatieZodraLokatieIsVeranderdEnRouteAanStaat));
        try {
            FileOutputStream file_out = new FileOutputStream(hetPropertiesBestand);
            props.store(file_out, "#ParcDroid");
            file_out.close();
        } catch (IOException ex) {
            drukaf("deProperties fout   ....:" + ex);
            return false;
        }
        return true;
    }

    public int erIsGeenKindId() {
        int ans = -1;
        String pag = null;
        try {
            pag = haalPaginaOp("www.frankkie.nl", "/parcdroid/meld.php?a=0");
            ans = Integer.parseInt(pag);
            kind_id = ans;
            showDialog(4);
        } catch (Exception ex) {
            drukaf("fout bij: erIsGeenKindId(): " + ex);
        }
        return ans;
    }

    public static boolean isInt(String getal) {
        boolean debug = true;
        try {
            Integer.parseInt(getal);
        } catch (Exception e) {
            if (debug) {
                drukaf("isInt: " + getal + " is geen int getal !");
            }
            return false;
        }
        return true;
    }
}
