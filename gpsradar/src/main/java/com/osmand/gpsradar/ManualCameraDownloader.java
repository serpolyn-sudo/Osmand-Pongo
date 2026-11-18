package com.osmand.gpsradar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Called ONLY by explicit user tap on Europe -> Car/Truck download entry. */
public final class ManualCameraDownloader {
    private static final String URL_BASE="https://slagboom.narod.ru/gpsradar/";
    private static final long MAX_BYTES=140L*1024*1024;
    private ManualCameraDownloader() { }

    /** Invoke from an app IO thread; do not call from Android main thread. */
    public static File downloadOnTap(RadarProfileSettings.Database database,
                                     File storage) throws IOException {
        String name=database == RadarProfileSettings.Database.TRUCK
                ? "truck.zip" : "car.zip";
        if (!storage.isDirectory() && !storage.mkdirs())
            throw new IOException("Cannot create camera storage");
        File temporary=new File(storage,name+".partial");
        File target=new File(storage,name);
        HttpURLConnection connection=(HttpURLConnection)new URL(URL_BASE+name).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(45000);
        connection.setInstanceFollowRedirects(false);
        try {
            if (connection.getResponseCode()!=HttpURLConnection.HTTP_OK)
                throw new IOException("Camera download HTTP "+connection.getResponseCode());
            long length=0;
            try (InputStream input=connection.getInputStream();
                 FileOutputStream out=new FileOutputStream(temporary)) {
                byte[] buffer=new byte[32768]; int count;
                while ((count=input.read(buffer))!=-1) {
                    length+=count;
                    if (length>MAX_BYTES) throw new IOException("Archive exceeds size limit");
                    out.write(buffer,0,count);
                }
                out.getFD().sync();
            }
            if (!temporary.renameTo(target))
                throw new IOException("Failed to save database archive");
            return target;
        } finally {
            connection.disconnect();
            if (temporary.exists()) temporary.delete();
        }
    }
}
