package com.davis.ddf.crs.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This software was created for rights to this software belong to appropriate licenses and
 * restrictions apply.
 *
 * @author Samuel Davis created on 9/5/17.
 */
public class LocUtil {
  public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.#####");
  public static final int POLYGON = 0;
  public static final int POINT = 1;
  public static final int MULTIPOINT = 2;
  public static final int LINESTRING = 3;
  public static final int MULTILINESTRING = 4;
  public static final int MULTIPOLYGON = 5;
  public static final Double MAX_LAT = Double.valueOf(90); //Y variable
  public static final Double MIN_LAT = Double.valueOf(-90); //Y variable
  public static final Double MAX_LON = Double.valueOf(180); //X variable
  public static final Double MIN_LON = Double.valueOf(-180); //X Variable
  public static final Logger logger = LoggerFactory.getLogger(LocUtil.class.getName());

  private LocUtil() {}

  public static double genRandomLevel() {
    double result = 0.0;
    int decide = ThreadLocalRandom.current().nextInt(4);
    if (decide == 0) {
      result = 0.27;
    } else if (decide == 1) {
      result = 0.1;
    } else if (decide == 2) {
      result = 0.2;
    } else if (decide == 3) {
      result = 0.3;
    }
    return result;
  }

  public static double generateUpperRight(double lng, double amount) {
    double newLong = lng + amount;
    if (newLong > 180) {
      newLong = 180;
    } else if (newLong < -180) {
      newLong = -180;
    }
    return newLong;
  }

  public static double generateLowerRight(double lat, double amount) {
    double newLat = lat - amount;
    if (newLat > 90) {
      newLat = 90;
    } else if (newLat < -90) {
      newLat = -90;
    }
    return newLat;
  }

  public static double generateLowerLeft(double lat, double amount) {
    double newLat = lat - amount;
    if (newLat > 90) {
      newLat = 90;
    } else if (newLat < -90) {
      newLat = -90;
    }
    return newLat;
  }

  //Verified this method returns valid line.
  public static String createRandomWktLine(
      Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double bottomRightLng) {
    StringBuilder result = new StringBuilder();
    int numberOfPoints = ThreadLocalRandom.current().nextInt(4) + 1;
    double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
    lat = Double.parseDouble(DECIMAL_FORMAT.format(lat));
    double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
    lng = Double.parseDouble(DECIMAL_FORMAT.format(lng));
    result.append("LINESTRING (");
    result.append(lng + " " + lat + ",");
    for (int x = 0; x < numberOfPoints; x++) {
      int upOrDown = ThreadLocalRandom.current().nextInt(50);
      if (upOrDown % 2 == 0) {
        result.append((lng + (lng * 0.3)) + " " + lat + ",");

      } else {
        result.append(lng + " " + (lat + (lat * 0.3)) + ",");
      }
    }
    String fix = result.toString();
    fix = fix.substring(0, fix.length() - 1);
    fix = fix + ")";
    return fix;
  }

  //Verified and tested this method creates valid MultiLineStrings
  public static String createRandomWktMultiLine(
      Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double bottomRightLng) {
    StringBuilder result = new StringBuilder();
    int numberOfLines = ThreadLocalRandom.current().nextInt(4) + 1;
    result.append("MULTILINESTRING (");

    for (int y = 0; y < numberOfLines; y++) {
      StringBuilder innerLoop = new StringBuilder();
      innerLoop.append("(");
      int numberOfPoints = ThreadLocalRandom.current().nextInt(4) + 1;
      double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
      lat = Double.parseDouble(DECIMAL_FORMAT.format(lat));
      double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
      lng = Double.parseDouble(DECIMAL_FORMAT.format(lng));

      innerLoop.append(lng + " " + lat + ", ");
      for (int x = 0; x < numberOfPoints; x++) {
        int upOrDown = ThreadLocalRandom.current().nextInt(50);
        if (upOrDown % 2 == 0) {
          innerLoop.append(lng + (lng * 0.3) + " " + lat + ", ");
        } else {
          innerLoop.append(lng + " " + (lat + (lat * 0.3)) + ", ");
        }
      }
      String fix = innerLoop.toString();
      fix = fix.substring(0, fix.length() - 2);
      fix = fix + "), ";
      result.append(fix);
    }
    String fix = result.toString();
    fix = fix.substring(0, fix.length() - 2);
    fix = fix + ")";
    return fix;
  }

  public static Double genRandomDoubleInRange(Double origin, Double bounds) {
    double result = ThreadLocalRandom.current().nextDouble(origin, bounds);
    result = Double.parseDouble(DECIMAL_FORMAT.format(result));
    return result;
  }
  //Verified and tested this method creates valid WKT MultiPolygons
  public static String createRandomWktMultiPolygon(
      Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double bottomRightLng) {
    StringBuilder result = new StringBuilder();
    int numberOfPolygons = ThreadLocalRandom.current().nextInt(4) + 1;

    result.append("MULTIPOLYGON (");

    for (int y = 0; y < numberOfPolygons; y++) {
      StringBuilder inner = new StringBuilder();
      double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
      lat = Double.parseDouble(DECIMAL_FORMAT.format(lat));
      double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
      lng = Double.parseDouble(DECIMAL_FORMAT.format(lng));
      double levelOf = genRandomLevel();
      inner.append("((");
      //upper left
      inner.append(lng + " " + lat + ", ");
      //upper Right
      inner.append(generateUpperRight(lng, levelOf) + " " + lat + ", ");
      //Lower right
      inner.append(
          generateUpperRight(lng, levelOf) + " " + generateLowerRight(lat, levelOf) + ", ");
      //lower left
      inner.append(lng + " " + generateLowerLeft(lat, levelOf) + ", ");
      inner.append(lng + " " + lat + " )),");
      String wkt = inner.toString();
      result.append(wkt);
    }
    String fix = result.toString();
    fix = fix.substring(0, fix.length() - 1);
    fix = fix + ")";
    return fix;
  }

  //Verified and tested this method produces valid points
  public static String createRandomWktPoint(
      Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double bottomRightLng) {
    String result = null;
    //logger.debug("Entering generate lat. Origin = {} Bound = {}",bottomRightLat,topLeftLat);
    double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
    //logger.debug("Exit generate lat. lat = {} ",lat);
    lat = Double.parseDouble(DECIMAL_FORMAT.format(lat));
    //logger.debug("Lat after decimal format lat = {} ",lat);
    //logger.debug("Entering generate lng. Origin = {} Bound = {}",bottomRightLng,topLeftLng);
    double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
    //logger.debug("Exit generate lng. lng = {} ",lng);
    lng = Double.parseDouble(DECIMAL_FORMAT.format(lng));
    //logger.debug("Lng after decimal format lng = {} ",lat);
    result = "POINT(" + lng + " " + lat + ")";
    return result;
  }

  //Verified this method produces valid multipoints.
  public static String createRandomWktMultiPoint(
      Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double bottomRightLng) {
    StringBuilder result = new StringBuilder();
    int numberOfPoints = ThreadLocalRandom.current().nextInt(20) + 1;
    double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
    lat = Double.parseDouble(DECIMAL_FORMAT.format(lat));
    double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
    lng = Double.parseDouble(DECIMAL_FORMAT.format(lng));
    result.append("MULTIPOINT (");
    result.append("(" + lng + " " + lat + "), ");
    for (int x = 0; x < numberOfPoints; x++) {
      double latInner = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
      latInner = Double.parseDouble(DECIMAL_FORMAT.format(latInner));
      double lngInner = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
      lngInner = Double.parseDouble(DECIMAL_FORMAT.format(lngInner));
      result.append("(" + lngInner + " " + latInner + "), ");
    }
    String fix = result.toString();
    fix = fix.substring(0, fix.length() - 2);
    fix = fix + ")";
    return fix;
  }

  //Verified and tested this method creates valid WKT Polygons
  public static String createRandomWktPolygon(
      Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double bottomRightLng) {
    StringBuilder result = new StringBuilder();
    double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
    lat = Double.parseDouble(DECIMAL_FORMAT.format(lat));
    double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
    lng = Double.parseDouble(DECIMAL_FORMAT.format(lng));
    double decision = genRandomLevel();
    result.append("POLYGON ((");
    //upper left
    result.append(lng + " " + lat + ", ");
    //upper Right
    result.append(generateUpperRight(lng, decision) + " " + lat + ", ");
    //Lower right
    result.append(
        generateUpperRight(lng, decision) + " " + generateLowerRight(lat, decision) + ", ");
    //lower left
    result.append(lng + " " + generateLowerLeft(lat, decision) + ", ");
    result.append(lng + " " + lat + " ))");
    String wkt = result.toString();
    return wkt;
  }

  public static String constructWktString(
      int wktType,
      Double topLeftLat,
      Double topLeftLng,
      Double bottomRightLat,
      Double bottomRightLng) {
    String result = null;
    switch (wktType) {
      case POLYGON:
        {
          try {
            result = createRandomWktPolygon(topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
          } catch (Exception e) {
            logger.error("Wkt Creation Failed For POLYGON {}", e);
          }
        }
        break;
        /*case MULTIPOLYGON:
        {
          try {
            result =
                createRandomWktMultiPolygon(topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
          } catch (Exception e) {
            logger.error("Wkt Creation Failed For MULTIPOLYGON");
          }
        }
        break;*/
      case POINT:
        {
          try {
            result = createRandomWktPoint(topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
          } catch (Exception e) {
            logger.error("Wkt Creation Failed For POINT");
          }
        }
        break;
        /*case MULTIPOINT:
        {
          try {
            result =
                createRandomWktMultiPoint(topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
          } catch (Exception e) {
            logger.error("Wkt Creation Failed For MULTIPOINT");
          }
        }
        break;*/
        //Removed because these lines look like ass on the map, until i can customize better they are staying off.
        /*case LINESTRING: {
          try {
            result = createRandomWktLine(topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
          } catch (Exception e) {
            logger.error("Wkt Creation Failed For LINESTRING");
          }
        }
        break;
        case MULTILINESTRING: {
          try {
            result = createRandomWktMultiLine(topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
          } catch (Exception e) {
            logger.error("Wkt Creation Failed For MULTILINESTRING");
          }
        }
        break;
        */
      default:
        {
          try {
            result = createRandomWktPoint(topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
          } catch (Exception e) {
            logger.error("Wkt Creation Failed For Default Type of Point");
          }
        }
    }
    return result;
  }
}
