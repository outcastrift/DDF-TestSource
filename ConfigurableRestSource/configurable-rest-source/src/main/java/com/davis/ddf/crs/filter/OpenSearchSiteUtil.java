/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package com.davis.ddf.crs.filter;

import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.List;

import ddf.catalog.data.Result;
import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.Query;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * Utility helper class that performs much of the translation logic used in CddaOpenSearchSite.
 */
public final class OpenSearchSiteUtil {

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

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchSiteUtil.class);

    private OpenSearchSiteUtil() {

    }



    public static String translateToOpenSearchSort(SortBy ddfSort) {
        String openSearchSortStr = null;
        String orderType = null;

        if (ddfSort == null || ddfSort.getSortOrder() == null) {
            return openSearchSortStr;
        }

        if (ddfSort.getSortOrder()
                .equals(SortOrder.ASCENDING)) {
            orderType = ORDER_ASCENDING;
        } else {
            orderType = ORDER_DESCENDING;
        }

        // QualifiedString type = ddfSort.getType();
        PropertyName sortByField = ddfSort.getPropertyName();

        if (Result.RELEVANCE.equals(sortByField.getPropertyName())) {
            // asc relevance not supported by spec
            openSearchSortStr = SORT_RELEVANCE + SORT_DELIMITER + ORDER_DESCENDING;
        } else if (Result.TEMPORAL.equals(sortByField.getPropertyName())) {
            openSearchSortStr = SORT_TEMPORAL + SORT_DELIMITER + orderType;
        } else {
            LOGGER.warn(
                    "Couldn't determine sort policy, not adding sorting in request to federated site.");
        }

        return openSearchSortStr;
    }






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



    private static boolean hasParameter(String parameter, List<String> parameters) {
        for (String param : parameters) {
            if (param != null && param.equalsIgnoreCase(parameter)) {
                return true;
            }
        }
        return false;
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
            LOGGER.debug("polyToBBox: lon - {} lat - {}", polyAry[i], polyAry[i + 1]);
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

}
