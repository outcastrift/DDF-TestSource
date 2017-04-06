package com.davis.ddf.crs.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * This software was created for
 * rights to this software belong to
 * appropriate licenses and restrictions apply.
 *
 * @author Samuel Davis created on 4/6/17.
 */
public class SourceUtil {
    private static final Logger logger = LoggerFactory.getLogger(SourceUtil.class);

    // OpenSearch defined parameters
    public static final String SEARCH_TERMS = "q";

    // temporal
    public static final String TIME_START = "dtstart";

    public static final String TIME_END = "dtend";

    public static final String TIME_NAME = "dateName";

    // geospatial
    public static final String GEO_LAT = "lat";

    public static final String GEO_LON = "lon";

    public static final String GEO_RADIUS = "radius";

    public static final String GEO_POLY = "polygon";

    public static final String GEO_BBOX = "bbox";

    // general options
    public static final String SRC = "src";

    public static final String MAX_RESULTS = "mr";

    public static final String COUNT = "count";

    public static final String MAX_TIMEOUT = "mt";

    public static final String USER_DN = "dn";

    public static final String SORT = "sort";

    public static final String FILTER = "filter";

    // only for async searches
    public static final String START_INDEX = "start";

    // geospatial constants
    public static final double LAT_DEGREE_M = 111325;

    public static final Integer DEFAULT_TOTAL_MAX = 1000;

    public static final Integer MAX_LAT = 90;

    public static final Integer MIN_LAT = -90;

    public static final Integer MAX_LON = 180;

    public static final Integer MIN_LON = -180;

    public static final Integer MAX_ROTATION = 360;

    public static final Integer MAX_BBOX_POINTS = 4;

    public static final String ORDER_ASCENDING = "asc";

    public static final String ORDER_DESCENDING = "desc";

    public static final String SORT_DELIMITER = ":";

    public static final String SORT_RELEVANCE = "relevance";

    public static final String SORT_TEMPORAL = "date";



    /**
     * Parses a WKT polygon string and returns a string array containing the lon and lat.
     *
     * @param wkt WKT String in the form of POLYGON((Lon Lat, Lon Lat...))
     * @return Lon on even # and Lat on odd #
     */
    public static String[] createPolyAryFromWKT(String wkt) {
        String lonLat = wkt.substring(wkt.indexOf("((") + 2, wkt.indexOf("))"));
        return lonLat.split(" |,\\p{Space}?");
    }

    /**
     * Parses a WKT Point string and returns a string array containing the lon and lat.
     *
     * @param wkt WKT String in the form of POINT( Lon Lat)
     * @return Lon at position 0, Lat at position 1
     */
    public static String[] createLatLonAryFromWKT(String wkt) {
        String lonLat = wkt.substring(wkt.indexOf('(') + 1, wkt.indexOf(')'));
        return lonLat.split(" ");
    }

    /**
     * Takes in a point radius search and converts it to a (rough approximation) bounding box.
     *
     * @param lon    latitude in decimal degrees (WGS-84)
     * @param lat    longitude in decimal degrees (WGS-84)
     * @param radius radius, in meters
     * @return Array of bounding box coordinates in the following order: West South East North. Also
     * described as minX, minY, maxX, maxY (where longitude is the X-axis, and latitude is
     * the Y-axis).
     */
    public static double[] createBBoxFromPointRadius(double lon, double lat, double radius) {
        double minX;
        double minY;
        double maxX;
        double maxY;

        double lonDifference = radius / (LAT_DEGREE_M * Math.cos(lat));
        double latDifference = radius / LAT_DEGREE_M;
        minX = lon - lonDifference;
        if (minX < MIN_LON) {
            minX += MAX_ROTATION;
        }
        maxX = lon + lonDifference;
        if (maxX > MAX_LON) {
            maxX -= MAX_ROTATION;
        }
        minY = lat - latDifference;
        if (minY < MIN_LAT) {
            minY = Math.abs(minY + MAX_LAT) - MAX_LAT;
        }
        maxY = lat + latDifference;
        if (maxY > MAX_LAT) {
            maxY = MAX_LAT - (maxY - MAX_LAT);
        }

        return new double[] {minX, minY, maxX, maxY};
    }

    /**
     * Takes in an array of coordinates and converts it to a (rough approximation) bounding box.
     * <p/>
     * Note: Searches being performed where the polygon goes through the international date line may
     * return a bad bounding box.
     *
     * @param polyAry array of coordinates (lon,lat,lon,lat,lon,lat..etc)
     * @return Array of bounding box coordinates in the following order: West South East North. Also
     * described as minX, minY, maxX, maxY (where longitude is the X-axis, and latitude is
     * the Y-axis).
     */
    public static double[] createBBoxFromPolygon(String[] polyAry) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        double curX, curY;
        for (int i = 0; i < polyAry.length - 1; i += 2) {
            logger.debug("polyToBBox: lon - {} lat - {}", polyAry[i], polyAry[i + 1]);
            curX = Double.parseDouble(polyAry[i]);
            curY = Double.parseDouble(polyAry[i + 1]);
            if (curX < minX) {
                minX = curX;
            }
            if (curX > maxX) {
                maxX = curX;
            }
            if (curY < minY) {
                minY = curY;
            }
            if (curY > maxY) {
                maxY = curY;
            }
        }
        return new double[] {minX, minY, maxX, maxY};
    }
    /**
     * Transform date string.
     *
     * @param date       the date
     * @param dateFormat the date format
     * @return the string
     */
    public static String transformDateTfr(Date date, SimpleDateFormat dateFormat) {
        Calendar c = Calendar.getInstance();
        TimeZone localTimeZone = c.getTimeZone();
        TimeZone afgTimeZone = TimeZone.getTimeZone("Asia/Kabul");
        int localOffsetFromUTC = localTimeZone.getRawOffset();
        int afghanOffsetFromUTC = afgTimeZone.getRawOffset();
        Calendar afghanCal = Calendar.getInstance(afgTimeZone);
        afghanCal.setTimeInMillis(date.getTime());
        afghanCal.add(Calendar.MILLISECOND, (-1 * localOffsetFromUTC));
        afghanCal.add(Calendar.MILLISECOND, afghanOffsetFromUTC);
        return dateFormat.format(afghanCal.getTime());
    }
}
