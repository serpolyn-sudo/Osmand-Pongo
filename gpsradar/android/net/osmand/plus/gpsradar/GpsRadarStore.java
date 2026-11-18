package net.osmand.plus.gpsradar;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Local-only database: SQLite index supports millions of camera points. */
public final class GpsRadarStore extends SQLiteOpenHelper {
    private static final int VERSION = 1;
    public GpsRadarStore(Context context) {
        super(context,"gpsradar_osmand.sqlite",null,VERSION);
    }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE points(vehicle TEXT NOT NULL,country TEXT,lat REAL NOT NULL,"
                +"lon REAL NOT NULL,type INTEGER NOT NULL,speed INTEGER NOT NULL,"
                +"dirtype INTEGER NOT NULL,bearing REAL NOT NULL)");
        db.execSQL("CREATE INDEX point_bounds ON points(vehicle,lat,lon)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {
        throw new IllegalStateException("Database schema migration required");
    }
    public synchronized int importArchive(File archive,String vehicle,boolean ukMiles) throws IOException {
        int count=0, files=0;
        SQLiteDatabase db=getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM points WHERE vehicle=?",new Object[]{vehicle});
            android.database.sqlite.SQLiteStatement insert=db.compileStatement(
              "INSERT INTO points(vehicle,country,lat,lon,type,speed,dirtype,bearing)"
              +" VALUES(?,?,?,?,?,?,?,?)");
            try (ZipInputStream zip=new ZipInputStream(new FileInputStream(archive),
                                                       StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry=zip.getNextEntry())!=null) {
                    if(entry.isDirectory())continue;
                    String name=entry.getName();
                    if(name.contains("..")||name.startsWith("/")||name.contains("\\")) {
                        throw new IOException("Bad ZIP path");
                    }
                    String filename=name.substring(name.lastIndexOf('/')+1);
                    if(!filename.toLowerCase(Locale.ROOT).endsWith(".txt"))continue;
                    String country=filename.substring(0,filename.length()-4).toUpperCase(Locale.ROOT);
                    if ((country.endsWith("GBR_MPH")&&!ukMiles)||
                        (country.endsWith("GBR_KMH")&&ukMiles))continue;
                    if(++files>128)throw new IOException("Too many country files");
                    BufferedReader reader=new BufferedReader(
                        new InputStreamReader(zip,StandardCharsets.UTF_8),8192);
                    String line;
                    long fileRead=0;
                    while ((line=reader.readLine())!=null) {
                        fileRead+=line.length()+1;
                        if(fileRead>64_000_000L)throw new IOException("Oversized country");
                        String[] p=line.trim().split("[,;\\t]");
                        if(p.length<3)continue;
                        try {
                            double lon=Double.parseDouble(p[0].trim());
                            double lat=Double.parseDouble(p[1].trim());
                            int type=Integer.parseInt(p[2].trim());
                            if(!Double.isFinite(lat)||!Double.isFinite(lon)
                                    ||Math.abs(lat)>90||Math.abs(lon)>180
                                    ||type<32||type>63)continue;
                            int speed=num(p,3);
                            int dir=num(p,4);
                            double heading=dec(p,5);
                            insert.clearBindings();
                            insert.bindString(1,vehicle);
                            insert.bindString(2,country);
                            insert.bindDouble(3,lat);
                            insert.bindDouble(4,lon);
                            insert.bindLong(5,type);
                            insert.bindLong(6,speed);
                            insert.bindLong(7,dir);
                            insert.bindDouble(8,heading);
                            insert.executeInsert();
                            if(++count>2_500_000)throw new IOException("Too many points");
                        } catch(NumberFormatException ignored) {}
                    }
                    zip.closeEntry();
                }
            }
            if(count==0)throw new IOException("No compatible TYPE 32–63 points in ZIP");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return count;
    }
    private static int num(String[]p,int i) {
        try{return i<p.length?(int)Double.parseDouble(p[i].trim()):0;}
        catch(NumberFormatException ignored){return 0;}
    }
    private static double dec(String[]p,int i) {
        try{return i<p.length?Double.parseDouble(p[i].trim()):0;}
        catch(NumberFormatException ignored){return 0;}
    }

    public synchronized List<GpsRadarPoint> near(String vehicle,double lat,double lon,int radius) {
        double deltaLat=radius/111000d;
        double deltaLon=radius/(111000d*Math.max(0.1,Math.cos(Math.toRadians(lat))));
        List<GpsRadarPoint> result=new ArrayList<>();
        try(Cursor cur=getReadableDatabase().rawQuery(
          "SELECT lat,lon,bearing,type,speed,dirtype,country FROM points WHERE "
          +"vehicle=? AND lat>=? AND lat<=? AND lon>=? AND lon<=? LIMIT 5000",
          new String[]{vehicle,Double.toString(lat-deltaLat),Double.toString(lat+deltaLat),
          Double.toString(lon-deltaLon),Double.toString(lon+deltaLon)})) {
            while(cur.moveToNext()) {
                result.add(new GpsRadarPoint(cur.getDouble(0),cur.getDouble(1),
                    cur.getDouble(2),cur.getInt(3),cur.getInt(4),cur.getInt(5),cur.getString(6)));
            }
        }
        return result;
    }
}
