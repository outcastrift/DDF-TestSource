package com.davis.ddf.crs.utils;

import com.davis.ddf.crs.data.CRSEndpointResponse;
import com.davis.ddf.crs.data.GroovyResponseObject;
import com.davis.ddf.crs.data.InMemoryDataStore;
import com.davis.ddf.crs.data.StoredSequentialQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.davis.ddf.crs.utils.LocUtil.DECIMAL_FORMAT;
import static com.davis.ddf.crs.utils.LocUtil.MAX_LAT;
import static com.davis.ddf.crs.utils.LocUtil.MAX_LON;
import static com.davis.ddf.crs.utils.LocUtil.MIN_LAT;
import static com.davis.ddf.crs.utils.LocUtil.MIN_LON;
import static com.davis.ddf.crs.utils.LocUtil.constructWktString;

/**
 * This software was created for rights to this software belong to appropriate licenses and
 * restrictions apply.
 *
 * @author Samuel Davis created on 9/5/17.
 */
public class RandomUtil {
  private static final Logger logger = LoggerFactory.getLogger(RandomUtil.class.getName());
  private static Date LAST_THREE_YEARS;
  private static Date DEFAULT_START;
  private static Date DEFAULT_END;
  private static SimpleDateFormat dateFormat;
  private static InMemoryDataStore DATA_STORE = new InMemoryDataStore();

  static {
    String dateFormatPattern = "yyyy-MM-dd'T'HH:mm:ssZ";
    dateFormat = new SimpleDateFormat(dateFormatPattern);
    try {
      Calendar c = Calendar.getInstance();
      c.set(1970, 0, 1);
      String s = dateFormat.format(c.getTime());
      DEFAULT_START = dateFormat.parse(s);
      c.setTimeInMillis(System.currentTimeMillis());
      String e = dateFormat.format(c.getTime());
      DEFAULT_END = dateFormat.parse(e);
      Calendar cy = Calendar.getInstance();
      cy.set(2014, 0, 1);
      String lastThreeYears = dateFormat.format(cy.getTime());
      LAST_THREE_YEARS = dateFormat.parse(lastThreeYears);
    } catch (ParseException e) {
      logger.error("Unable to parse");
    }
  }

  private RandomUtil() {}

  public static CRSEndpointResponse createRandomResult(
      InMemoryDataStore dataStore, int itemId, int idOffset) {

    //Works for USA
    double topLeftLat = 47.5531;
    double topLeftLng = -124.3574;
    double bottomRightLat = 29.8240;
    double bottomRightLng = -66.3350;
    /* double topLeftLat = 90;
    double topLeftLng = -180;
    double bottomRightLat = -90 ;
    double bottomRightLng = 180;*/
    CRSEndpointResponse uniResponse = new CRSEndpointResponse();
    int whichGeom = ThreadLocalRandom.current().nextInt(6);
    //logger.info("Array Number = {}", whichGeom);
    String wktString =
        constructWktString(whichGeom, topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
    uniResponse.setLocation(wktString);

    Date sigactDate = generateRandomDate(LAST_THREE_YEARS, DEFAULT_END);
    uniResponse.setClassification("UNCLASSIFIED");
    uniResponse.setDisplayTitle(RandomUtil.generateTitleBasedOnGeometry(wktString, sigactDate));
    uniResponse.setDateOccurred(sigactDate);
    double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
    lat = Double.parseDouble(DECIMAL_FORMAT.format(lat));
    //logger.debug("Entering always successful lng. Origin = {} Bound = {}",topLeftLng,bottomRightLng);
    double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
    lng = Double.parseDouble(DECIMAL_FORMAT.format(lng));
    uniResponse.setDisplaySerial("CRS-" + String.valueOf(itemId + idOffset));
    uniResponse.setLatitude(lat);
    uniResponse.setLongitude(lng);
    uniResponse.setOriginatorUnit(dataStore.getOriginateUnit().get(itemId));
    uniResponse.setPrimaryEventType(getRandomizedField(dataStore.getEventTypes()));
    uniResponse.setReportLink(getRandomizedField(dataStore.getReportLinks()));
    uniResponse.setSummary(dataStore.getSummaries().get(itemId));
    return uniResponse;
  }

  public static String generateTitleBasedOnGeometry(String geom, Date date) {
    String name = null;
    if (geom.contains("MULTIPOINT")) {
      name = "SIGACT-COLLECTION" + date;
    } else if (geom.contains("POINT")) {
      name = "SIGACT-" + date;
    } else if (geom.contains("MULTILINESTRING")) {
      name = "ROUTE-COLLECTION-" + date;
    } else if (geom.contains("LINESTRING")) {
      name = "ROUTE-" + date;

    } else if (geom.contains("MULTIPOLYGON")) {
      name = "NAI-COLLECTION-" + date;

    } else if (geom.contains("POLYGON")) {
      name = "NAI-" + date;
    }
    return name;
  }

  /**
   * Get random element from array list.
   *
   * @param <T> the array list you want a random entry from.
   * @return the random entry.
   */
  public static <T> T getRandomizedField(ArrayList<T> list) {
    Random random = new Random();
    return list.get(random.nextInt(list.size()));
  }

  /**
   * Build Stored Sequential Query
   *
   * @param amount the amount
   * @param pagesize the amount
   * @return a stored query contained X number of results where x is equal to amount * pagesize
   */
  public static StoredSequentialQuery buildStoredSequentialQuery(int amount, int pagesize) {
    StoredSequentialQuery storedSequentialQuery = new StoredSequentialQuery();
    List<GroovyResponseObject> results = new ArrayList<GroovyResponseObject>();
    GroovyResponseObject restResponseObject = null;
    for (int x = 1; x < (amount * pagesize) + 1; x++) {
      restResponseObject = new GroovyResponseObject();
      /*The valid range of latitude in degrees is -90 and +90 for the southern and northern hemisphere respectively.
      Longitude is in the range -180 and +180 specifying the east-west position.*/
      double lat = ThreadLocalRandom.current().nextDouble(MIN_LAT, MAX_LAT);
      double lng = ThreadLocalRandom.current().nextDouble(MIN_LON, MAX_LON);
      //POINT(73.0166738279393 33.6788721326803)
      restResponseObject.setTitle("TestObject" + String.valueOf(amount));
      String id = UUID.randomUUID().toString().replaceAll("-", "");
      restResponseObject.setId(id);
      restResponseObject.setLat(lat);
      restResponseObject.setLng(lng);
      restResponseObject.setLocation("POINT(" + lng + " " + lat + ")");
      results.add(restResponseObject);
      storedSequentialQuery.addObjectToQuery(x, restResponseObject);
    }
    return storedSequentialQuery;
  }

  /**
   * Build objects list.
   *
   * @param amount the amount
   * @return the list
   */
  public static List<GroovyResponseObject> buildRandomGroovyResponses(int amount) {
    List<GroovyResponseObject> results = new ArrayList<GroovyResponseObject>();
    while (amount > 0) {
      GroovyResponseObject restResponseObject = new GroovyResponseObject();

      /*The valid range of latitude in degrees is -90 and +90 for the southern and northern hemisphere respectively.
      Longitude is in the range -180 and +180 specifying the east-west position.*/
      double lat = ThreadLocalRandom.current().nextDouble(MIN_LAT, MAX_LAT);
      double lng = ThreadLocalRandom.current().nextDouble(MIN_LON, MAX_LON);
      //POINT(73.0166738279393 33.6788721326803)
      restResponseObject.setTitle("TestObject" + String.valueOf(amount));
      String id = UUID.randomUUID().toString().replaceAll("-", "");
      restResponseObject.setId(id);
      restResponseObject.setLat(lat);
      restResponseObject.setLng(lng);
      restResponseObject.setLocation("POINT(" + lng + " " + lat + ")");
      results.add(restResponseObject);
      amount = amount - 1;
    }
    return results;
  }

  public static Date generateRandomDate(Date dMin, Date dMax) {
    long MILLIS_PER_DAY = 1000 * 60 * 60 * 24;
    GregorianCalendar s = new GregorianCalendar();
    s.setTimeInMillis(dMin.getTime());
    GregorianCalendar e = new GregorianCalendar();
    e.setTimeInMillis(dMax.getTime());

    // Get difference in milliseconds
    long endL = e.getTimeInMillis() + e.getTimeZone().getOffset(e.getTimeInMillis());
    long startL = s.getTimeInMillis() + s.getTimeZone().getOffset(s.getTimeInMillis());
    long dayDiff = (endL - startL) / MILLIS_PER_DAY;
    //int year  = dMax.getYear() - dMin.getYear();
    Calendar cal = Calendar.getInstance();
    cal.setTime(dMin);
    cal.add(Calendar.DATE, new Random().nextInt((int) dayDiff));
    //int year  = (int) ThreadLocalRandom.current().nextDouble(1970, 2017);
    //cal.add(Calendar.YEAR, year);
    return cal.getTime();
  }

  public static CRSEndpointResponse genRandomResponse(
      double lat,
      double lng,
      Double topLeftLat,
      Double topLeftLng,
      Double bottomRightLat,
      Double bottomRightLng,
      Date start,
      Date end) {
    /*logger.debug("topLeftLat {}",topLeftLat);
    logger.debug("topLeftLng {}",topLeftLng);
    logger.debug("bottomRightLat {}",bottomRightLat);
    logger.debug("bottomRightLng {}",bottomRightLng);*/
    CRSEndpointResponse uniResponse = new CRSEndpointResponse();
    int whichGeom = ThreadLocalRandom.current().nextInt(6);
    //logger.info("Array Number = {}", whichGeom);
    String wktString =
        constructWktString(whichGeom, topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
    uniResponse.setLocation(wktString);
    Date sigactDate = generateRandomDate(start, end);
    uniResponse.setClassification("UNCLASSIFIED");
    int niirsValue = ThreadLocalRandom.current().nextInt(11);
    uniResponse.setNiirs(niirsValue);
    uniResponse.setDisplayTitle(generateTitleBasedOnGeometry(wktString, sigactDate));
    uniResponse.setDateOccurred(sigactDate);
    uniResponse.setDisplaySerial(
        String.valueOf(topLeftLat)
            + String.valueOf(bottomRightLng)
            + String.valueOf(lat)
            + String.valueOf(lng));
    uniResponse.setLatitude(lat);
    uniResponse.setLongitude(lng);
    uniResponse.setOriginatorUnit(getRandomizedField(DATA_STORE.getOriginateUnit()));
    uniResponse.setPrimaryEventType(getRandomizedField(DATA_STORE.getEventTypes()));
    uniResponse.setReportLink(getRandomizedField(DATA_STORE.getReportLinks()));
    uniResponse.setSummary(getRandomizedField(DATA_STORE.getSummaries()));
    return uniResponse;
  }
}
