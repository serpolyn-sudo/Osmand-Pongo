package com.osmand.gpsradar;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** One imported iGO speed-camera or road-hazard location; X=longitude, Y=latitude. */
public final class RadarCamera {
    public final double latitude;
    public final double longitude;
    public final int type;
    public final int speedLimitKmh;
    public final int directionType;
    public final double bearingDegrees;
    public final String country;

    public RadarCamera(double lat, double lon, int type, int speed, int dirType,
                       double bearing, String country) {
        if (!Double.isFinite(lat) || lat < -90 || lat > 90 ||
            !Double.isFinite(lon) || lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Invalid GPS coordinates");
        }
        if (type < 32 || type > 63) throw new IllegalArgumentException("Unknown TYPE " + type);
        latitude = lat;
        longitude = lon;
        this.type = type;
        speedLimitKmh = speed > 0 && speed <= 250 ? speed : 0;
        directionType = dirType;
        bearingDegrees = Double.isFinite(bearing)
                ? ((bearing % 360) + 360) % 360 : Double.NaN;
        this.country = country == null ? "" : country;
    }

    public boolean hasSpeedLimit() { return speedLimitKmh > 0; }
    public boolean isAverageSpeed() { return type == 35; }
    public String label() { return TYPES.getOrDefault(type, "Объект дороги"); }

    public static final Map<Integer, String> TYPES;
    static {
        Map<Integer, String> m = new HashMap<>();
        m.put(32,"Камера контроля скорости");
        m.put(33,"Возможно засада");
        m.put(34,"Встроенная камера контроля скорости");
        m.put(35,"Камера контроля средней скорости");
        m.put(36,"Контроль проезда на красный");
        m.put(37,"Фото радарный комплекс");
        m.put(38,"Железнодорожный переезд");
        m.put(39,"Камера на полосе общественного транспорта");
        m.put(40,"Аварийно-опасный участок");
        m.put(41,"Школьная территория");
        m.put(42,"Начало населенного пункта");
        m.put(43,"Камера встроенная в светофор");
        m.put(44,"Пункт оплаты");
        m.put(45,"Конец населенного пункта");
        m.put(46,"Камера направлена в спину");
        m.put(47,"Контроль оплаты");
        m.put(48,"Знак уступи дорогу");
        m.put(49,"Возможно тренога");
        m.put(50,"Лежачий полицейский");
        m.put(51,"Участок плохой дороги");
        m.put(52,"Опасный поворот");
        m.put(53,"Тоннель");
        m.put(54,"Объект ПОИ");
        m.put(55,"Пост ДПС");
        m.put(56,"Камера контроля остановки");
        m.put(57,"Возможно тренога в спину");
        m.put(58,"Фото радарный комплекс в спину");
        m.put(59,"Камера контроля скорости в спину");
        m.put(60,"Съезд с автострады");
        m.put(61,"Пешеходный переход");
        m.put(62,"Камера наблюдения");
        m.put(63,"Опасный участок дороги");
        TYPES = Collections.unmodifiableMap(m);
    }
}
