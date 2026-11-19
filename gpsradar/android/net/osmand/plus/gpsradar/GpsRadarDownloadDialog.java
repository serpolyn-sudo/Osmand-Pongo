package net.osmand.plus.gpsradar;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.ViewGroup;
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

    public static void show(Activity activity) { showVehicles(activity); }

    private static void showVehicles(Activity activity) {
        new AlertDialog.Builder(activity)
          .setTitle("Европа")
          .setItems(new String[]{"Грузовик","Легковой"},
              (dialog,index)->confirmDownload(activity,index==0?"truck":"car"))
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
        LinearLayout panel=new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(20*activity.getResources().getDisplayMetrics().density);
        panel.setPadding(pad,pad,pad,pad);
        TextView status=new TextView(activity);
        status.setText("Подключение к серверу…");
        ProgressBar progress=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setIndeterminate(true);
        panel.addView(status);
        panel.addView(progress,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,pad));
        AlertDialog loading=new AlertDialog.Builder(activity)
            .setTitle("Европа • "+(vehicle.equals("truck")?"Грузовик":"Легковой"))
            .setView(panel).setCancelable(false).create();
        loading.show();
        Handler ui=new Handler(Looper.getMainLooper());
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
                    long length=con.getContentLengthLong();
                    ui.post(()->{progress.setIndeterminate(length<=0);
                        status.setText(length>0?"Скачивание: 0%":"Скачивание базы…");});
                    try(InputStream in=con.getInputStream();
                        FileOutputStream out=new FileOutputStream(temp)) {
                        byte[] bytes=new byte[32*1024];long n=0;int c;int last=-1;
                        while((c=in.read(bytes))!=-1) {
                            if((n+=c)>140L*1024*1024)
                                throw new java.io.IOException("ZIP слишком большой");
                            out.write(bytes,0,c);
                            if(length>0) {
                                int percent=(int)Math.min(100,n*100/length);
                                if(percent!=last) {last=percent;int current=percent;
                                    ui.post(()->{progress.setProgress(current);
                                        status.setText("Скачивание: "+current+"%");});}
                            }
                        }
                        out.getFD().sync();
                    }
                } finally {con.disconnect();}
                if(!temp.renameTo(destination))throw new java.io.IOException("Не удалось сохранить");
                ui.post(()->{progress.setIndeterminate(true);
                    status.setText("Импорт объектов в базу…");});
                int count;
                try(GpsRadarStore store=new GpsRadarStore(activity.getApplicationContext())) {
                    count=store.importArchive(destination,vehicle,false);
                }
                int loaded=count;
                ui.post(()->{loading.dismiss();Toast.makeText(activity,
                    "GPSRadar: импортировано "+loaded+" объектов",Toast.LENGTH_LONG).show();});
            } catch(Exception ex) {
                String error=ex.getMessage();
                ui.post(()->{loading.dismiss();Toast.makeText(activity,
                    "Ошибка загрузки GPSRadar: "+error,Toast.LENGTH_LONG).show();});
            } finally {
                if(temp!=null && temp.exists())temp.delete();
                worker.shutdown();
            }
        });
    }
}
