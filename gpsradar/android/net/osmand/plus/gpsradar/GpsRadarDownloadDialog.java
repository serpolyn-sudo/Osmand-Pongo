package net.osmand.plus.gpsradar;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

/** The only HTTPS entry point; invoked exclusively from Maps & Resources. */
public final class GpsRadarDownloadDialog {
    private static final String ROOT="https://slagboom.narod.ru/gpsradar/";
    private GpsRadarDownloadDialog() {}

    public static void show(Activity activity) {
        new AlertDialog.Builder(activity)
          .setTitle("Камеры контроля скорости")
          .setItems(new String[]{"Европа"},(dialog,region)->showVehicles(activity))
          .setNegativeButton("Назад",null).show();
    }

    private static void showVehicles(Activity activity) {
        new AlertDialog.Builder(activity)
          .setTitle("Европа")
          .setItems(new String[]{"Легковые (авто, мото, автобус)","Грузовые"},
              (dialog,index)->confirmDownload(activity,index==1?"truck":"car"))
          .setNegativeButton("Назад",null).show();
    }

    private static void confirmDownload(Activity activity,String vehicle) {
        String title=vehicle.equals("truck")?"Грузовые":"Легковые";
        new AlertDialog.Builder(activity).setTitle("База GPSRadar: "+title)
          .setMessage("Скачать базу камер с сайта slagboom.narod.ru? "
                +"Автоматическое обновление отключено.")
          .setNegativeButton("Отмена",null)
          .setPositiveButton("Загрузить",(dialog,choice)->download(activity,vehicle))
          .show();
    }

    private static void download(Activity activity,String vehicle) {
        Toast.makeText(activity,"Загрузка базы GPSRadar началась",Toast.LENGTH_LONG).show();
        java.util.concurrent.ExecutorService worker=Executors.newSingleThreadExecutor();
        worker.execute(()->{
            File temp=null;
            try {
                File dir=new File(activity.getFilesDir(),"gpsradar");
                if(!dir.exists()&&!dir.mkdirs())throw new java.io.IOException("Нет папки");
                File destination=new File(dir,vehicle+".zip");
                temp=new File(dir,vehicle+".partial");
                HttpURLConnection con=(HttpURLConnection)
                  new URL(ROOT+vehicle+".zip").openConnection();
                con.setConnectTimeout(15000);con.setReadTimeout(30000);
                con.setInstanceFollowRedirects(false);
                try {
                    if(con.getResponseCode()!=200)
                        throw new java.io.IOException("HTTP "+con.getResponseCode());
                    try(InputStream in=con.getInputStream();
                        FileOutputStream out=new FileOutputStream(temp)) {
                        byte[] bytes=new byte[32*1024];long n=0;int c;
                        while((c=in.read(bytes))!=-1) {
                            if((n+=c)>140L*1024*1024)
                                throw new java.io.IOException("ZIP слишком большой");
                            out.write(bytes,0,c);
                        }
                        out.getFD().sync();
                    }
                } finally {con.disconnect();}
                if(!temp.renameTo(destination))throw new java.io.IOException("Не удалось сохранить");
                int count;
                try(GpsRadarStore store=new GpsRadarStore(activity.getApplicationContext())) {
                    count=store.importArchive(destination,vehicle,false);
                }
                int loaded=count;
                new Handler(Looper.getMainLooper()).post(()->Toast.makeText(activity,
                    "GPSRadar: импортировано "+loaded+" объектов",Toast.LENGTH_LONG).show());
            } catch(Exception ex) {
                String error=ex.getMessage();
                new Handler(Looper.getMainLooper()).post(()->Toast.makeText(activity,
                    "Ошибка загрузки GPSRadar: "+error,Toast.LENGTH_LONG).show());
            } finally {
                if(temp!=null && temp.exists())temp.delete();
                worker.shutdown();
            }
        });
    }
}
