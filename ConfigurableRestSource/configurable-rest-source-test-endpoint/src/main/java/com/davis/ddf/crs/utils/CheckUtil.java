package com.davis.ddf.crs.utils;

import com.davis.ddf.crs.data.CRSEndpointResponse;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This software was created for rights to this software belong to appropriate licenses and
 * restrictions apply.
 *
 * @author Samuel Davis created on 9/5/17.
 */
public class CheckUtil {
  private static final Logger logger = LoggerFactory.getLogger(CheckUtil.class.getName());
    public static final List<String> STOP_WORDS =
            Arrays.asList(
                    "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is",
                    "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then", "there",
                    "these", "they", "this", "to", "was", "will", "with");
  private CheckUtil() {}

  public static boolean checkDoubleInRange(
      double topLeftLat,
      double topLeftLng,
      double bottomRightLat,
      double bottomRightLng,
      CRSEndpointResponse item) {
    boolean result = false;
    String itemLocation = item.getLocation();
    WKTReader reader = new WKTReader();
    try {
      Geometry geometry = reader.read(itemLocation);
      Point center = geometry.getCentroid();
      double lat = item.getLatitude();
      double lng = item.getLongitude();

      if (lat > bottomRightLat && lat < topLeftLat) {
        if (lng < bottomRightLng && lng > topLeftLng) {
          result = true;
        }
      }

    } catch (com.vividsolutions.jts.io.ParseException e) {
      logger.error("Unable to parse WKT determining the item to be false.");
    }

    return result;
  }

  public static boolean checkQueryString(String queryString, CRSEndpointResponse item) {
    boolean result = false;
    if (queryString != null && !queryString.equalsIgnoreCase("%")) {
      String[] queryParams = queryString.split(" ");
      String itemRollUp =
          item.getClassification()
              + " "
              + item.getOriginatorUnit()
              + " "
              + item.getSummary()
              + " "
              + item.getReportLink()
              + " "
              + item.getDisplayTitle();
      for (String param : queryParams) {

        if (!STOP_WORDS.contains(param)) {
          if (itemRollUp.contains(param)) {
            result = true;
            break;
          }
        }
      }
    } else {
      result = true;
    }

    return result;
  }

  public static boolean checkDateInRange(Date from, Date to, CRSEndpointResponse item) {
    boolean result = false;
    Long itemDate = item.getDateOccurred().getTime();

    if (itemDate > from.getTime() && itemDate < to.getTime()) {
      result = true;
    }
    return result;
  }

  public static boolean checkResultForMatching(
      double topLeftLat,
      double topLeftLng,
      double bottomRightLat,
      double bottomRightLng,
      String queryString,
      Date start,
      Date end,
      CRSEndpointResponse e) {
    boolean result = false;
    boolean dateMatched = checkDateInRange(start, end, e);
    if (dateMatched) {
      boolean locationMatched =
          checkDoubleInRange(topLeftLat, topLeftLng, bottomRightLat, bottomRightLng, e);
      if (locationMatched) {
        boolean contextualMatched = checkQueryString(queryString, e);
        if (contextualMatched) {
          result = true;
        }
      }
    }
    return result;
  }

  public static List<CRSEndpointResponse> checkForResults(
      Date start,
      Date end,
      String bottomRight,
      String topLeft,
      String queryString,
      Map<String, CRSEndpointResponse> cannedResponses) {
    List<CRSEndpointResponse> result = new ArrayList<>();
    String latSegments[] = StringUtils.split(topLeft);
    String lngSegments[] = StringUtils.split(bottomRight);
    double topLeftLat = Double.parseDouble(latSegments[0]);

    double topLeftLng = Double.parseDouble(latSegments[1]);

    double bottomRightLat = Double.parseDouble(lngSegments[0]);

    double bottomRightLng = Double.parseDouble(lngSegments[1]);

    for (CRSEndpointResponse e : cannedResponses.values()) {
      if (checkResultForMatching(
          topLeftLat, topLeftLng, bottomRightLat, bottomRightLng, queryString, start, end, e)) {
        result.add(e);
      }
    }

    return result;
  }
}
