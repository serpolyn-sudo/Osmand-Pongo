package net.osmand.plus.gpsradar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.ArrayList;
import java.util.List;

/** Every value is isolated by current OsmAnd ApplicationMode key. */
public final class GpsRadarSettingsDialog {
    private GpsRadarSettingsDialog() {}
    public static void show(Activity activity, OsmandApplication app) {
        show(activity,app,app.getSettings().getApplicationMode());
    }
    public static void show(Activity activity, OsmandApplication app, ApplicationMode mode) {
        String profile=mode.getStringKey();
        SharedPreferences pref=app.getSharedPreferences(
                "gpsradar_profile_"+profile,Context.MODE_PRIVATE);
        String vehicle=mode.isDerivedRoutingFrom(ApplicationMode.TRUCK)?"Грузовые":"Легковые";
        String[] actions={"Режим предупреждений: "+(pref.getBoolean("extended",false)?
                    "Расширенный":"Штатный"),
                "Показывать все объекты: "+(pref.getBoolean("markers",true)?"Да":"Нет"),
                "Пороги предупреждений: "+pref.getString("thresholds","700,300,150")+" м",
                "Допуск превышения: "+pref.getInt("tolerance",0)+" км/ч",
                "Типы предупреждений (32–63)"};
        new AlertDialog.Builder(activity)
                .setTitle("GPSRadar • "+profile+" • "+vehicle)
                .setItems(actions,(dialog,index)->{
                    switch(index) {
                        case 0 -> modes(activity,app,mode,pref);
                        case 1 -> {
                            pref.edit().putBoolean("markers",!pref.getBoolean("markers",true)).apply();
                            show(activity,app,mode);
                        }
                        case 2 -> editText(activity,app,mode,pref,true);
                        case 3 -> editText(activity,app,mode,pref,false);
                        case 4 -> types(activity,app,mode,pref);
                    }
                }).setNegativeButton("Закрыть",null).show();
    }
    private static void modes(Activity activity,OsmandApplication app,
                              ApplicationMode mode,SharedPreferences pref) {
        boolean oldExtended=pref.getBoolean("extended",false);
        new AlertDialog.Builder(activity).setTitle("Предупреждения GPSRadar")
          .setSingleChoiceItems(new String[]{"Штатные OsmAnd","Расширенные GPSRadar"},
                oldExtended?1:0,(dialog,selection)->{
                boolean extended=selection==1;
                // Prevent duplicate stock camera speech in the enhanced mode.
                if(extended&&!oldExtended) {
                    boolean nativeVoice=app.getSettings().SPEAK_SPEED_CAMERA.getModeValue(mode);
                    pref.edit().putBoolean("saved_native_voice",nativeVoice).apply();
                    app.getSettings().SPEAK_SPEED_CAMERA.setModeValue(mode,false);
                } else if(!extended&&oldExtended) {
                    app.getSettings().SPEAK_SPEED_CAMERA.setModeValue(mode,
                        pref.getBoolean("saved_native_voice",false));
                }
                pref.edit().putBoolean("extended",extended).apply();
                dialog.dismiss();show(activity,app,mode);
          }).setNegativeButton("Отмена",null).show();
    }
    private static void editText(Activity activity,OsmandApplication app,
                      ApplicationMode mode,SharedPreferences pref,boolean triggers) {
        EditText field=new EditText(activity);
        field.setSingleLine(true);
        field.setText(triggers?pref.getString("thresholds","700,300,150"):
                             String.valueOf(pref.getInt("tolerance",0)));
        int padding=(int)(16*activity.getResources().getDisplayMetrics().density);
        field.setPadding(padding,padding,padding,padding);
        new AlertDialog.Builder(activity)
          .setTitle(triggers?"Расстояния в метрах, через запятую":"Допуск превышения, км/ч")
          .setView(field)
          .setNegativeButton("Отмена",null)
          .setPositiveButton("Сохранить",(d,button)->{
              try {
                  if(triggers) {
                      String[] pieces=field.getText().toString().trim().split(",");
                      if(pieces.length==0||pieces.length>8)throw new Exception();
                      StringBuilder joined=new StringBuilder();
                      for(String item:pieces) {
                          int val=Integer.parseInt(item.trim());
                          if(val<10||val>3000)throw new Exception();
                          if(joined.length()>0)joined.append(',');
                          joined.append(val);
                      }
                      pref.edit().putString("thresholds",joined.toString()).apply();
                  } else {
                      int val=Integer.parseInt(field.getText().toString().trim());
                      if(val<0||val>40)throw new Exception();
                      pref.edit().putInt("tolerance",val).apply();
                  }
              } catch(Exception ex) {
                  Toast.makeText(activity,"Недопустимое значение",Toast.LENGTH_LONG).show();
              }
          }).show();
    }
    private static void types(Activity activity,OsmandApplication app,
                              ApplicationMode mode,SharedPreferences pref) {
        String[] labels=new String[32];
        boolean[] enabled=new boolean[32];
        for(int i=0;i<32;i++) {
            int type=32+i;
            labels[i]=type+" — "+GpsRadarEngine.cameraLabel(type);
            enabled[i]=pref.getBoolean("type_"+type,true);
        }
        new AlertDialog.Builder(activity)
           .setTitle("Типы GPSRadar — "+mode.getStringKey())
           .setMultiChoiceItems(labels,enabled,(dialog,index,value)->enabled[index]=value)
           .setNegativeButton("Отмена",null)
           .setPositiveButton("Применить",(dialog,button)->{
               SharedPreferences.Editor editor=pref.edit();
               for(int i=0;i<32;i++)editor.putBoolean("type_"+(i+32),enabled[i]);
               editor.apply();
           }).show();
    }
}
