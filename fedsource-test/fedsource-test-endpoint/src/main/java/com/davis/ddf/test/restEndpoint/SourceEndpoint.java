package com.davis.ddf.test.restEndpoint;

import com.davis.ddf.test.fedSource.datamodel.UniversalFederatedSourceResponse;
import com.davis.ddf.test.groovySource.datamodel.GroovyResponseObject;
import com.davis.ddf.test.restEndpoint.jsonapi.JsonApiResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * The type Geospatial endpoint.
 */
@Path("/")
@Consumes("application/json")
public class SourceEndpoint {
  public static final Double MAX_LAT = Double.valueOf(90); //Y variable
  public static final Double MIN_LAT = Double.valueOf(-90); //Y variable
  public static final Double MAX_LON = Double.valueOf(180); //X variable
  public static final Double MIN_LON = Double.valueOf(-180); //X Variable
  public static final String TOP_LEFT = "90 -180";
  public static final String BOTTOM_RIGHT = "-90 180";
  private static final int POLYGON = 0;
  private static final int MULTIPOLYGON = 1;
  private static final int POINT = 2;
  private static final int MULTIPOINT = 3;
  private static final int LINESTRING = 4;
  private static final int MULTILINESTRING = 5;

  private static final DecimalFormat decFormat = new DecimalFormat("#.#####");
  /**
   * The constant TAG.
   */
  private static final Logger logger = LoggerFactory.getLogger(SourceEndpoint.class);
  public static Date DEFAULT_START;
  public static Date DEFAULT_END;
  SimpleDateFormat dateFormat;
  private ArrayList<String> originateUnit;
  private ArrayList<String> reportLinks;
  private ArrayList<String> summaries;
  private ArrayList<String> eventTypes;

  /**
   * Instantiates a new Geospatial endpoint.
   */
  public SourceEndpoint() {
    // String dateFormatPattern = "MM/dd/yyyy HH:mm:ss";
    //NOTE yyyyMMddHHmmss works every time but we use the ISO format so cant use it.
    //String dateFormatPattern = "yyyyMMddHHmmss";
    String dateFormatPattern = "yyyy-MM-dd'T'HH:mm:ssZ";
    dateFormat = new SimpleDateFormat(dateFormatPattern);
    Calendar c = Calendar.getInstance();
    c.set(1970, 0, 1);
    String s = dateFormat.format(c.getTime());
    try {
      DEFAULT_START = dateFormat.parse(s);
    } catch (ParseException e) {
    }
    c.setTimeInMillis(System.currentTimeMillis());
    String e = dateFormat.format(c.getTime());
    try {
      DEFAULT_END = dateFormat.parse(e);
    } catch (ParseException e2) {
    }
    originateUnit = new ArrayList<String>();
    reportLinks = new ArrayList<String>();
    summaries = new ArrayList<String>();
    eventTypes = new ArrayList<String>();
    populateArrays();
    logger.debug("Arrays Size \n orginatingUnits = " + String.valueOf(originateUnit.size()) + " \n summaries = " +
            String.valueOf(summaries.size()) + "  \n eventTypes = " + String.valueOf(eventTypes.size()) + " \n " +
            "reportLinks = " + String.valueOf(reportLinks.size()) + "\n");

  }

  /**
   * Convert geopoint response.
   *
   * @param requestUriInfo the request uri info
   * @param amount         the amount
   * @return the response
   */
  @GET
  @Path("/getGroovyResults")
  public Response getResultsForGroovy(@Context UriInfo requestUriInfo, @QueryParam("amount") String amount) {
    int intAmount = 0;
    List<GroovyResponseObject> resultsList = null;
    try {
      JsonApiResponse response = new JsonApiResponse();
      Response.ResponseBuilder builder = null;
      String date = LocalDateTime.now().toString();
      if (amount != null) {

        resultsList = buildObjects(Integer.valueOf(amount));
        response.setData(resultsList);
      }

      if (response != null) {
        builder = Response.ok(response.getSanitizedJson(), MediaType.APPLICATION_JSON);
      } else {
        throw new SourceEndpointException("There was a error handling the request.", "There was a error handling the " +
                "request.", Response.Status.INTERNAL_SERVER_ERROR);
      }

      return builder.build();
    } catch (Exception e) {
      throw new SourceEndpointException("There was a error handling the request.", "There was a error handling the " +
              "request.", Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Build objects list.
   *
   * @param amount the amount
   * @return the list
   */
  private List<GroovyResponseObject> buildObjects(int amount) {
    List<GroovyResponseObject> results = new ArrayList<GroovyResponseObject>();
    while (amount > 0) {
      GroovyResponseObject restResponseObject = new GroovyResponseObject();

         /*The valid range of latitude in degrees is -90 and +90 for the southern and northern hemisphere respectively.
         Longitude is in the range -180 and +180 specifying the east-west position.*/
      double lat = ThreadLocalRandom.current().nextDouble(MIN_LAT, MAX_LAT);
      double lng = ThreadLocalRandom.current().nextDouble(MIN_LON, MAX_LON);
      //POINT(73.0166738279393 33.6788721326803)
      restResponseObject.setTitle("TestObject" + String.valueOf(amount));
      restResponseObject.setLat(lat);
      restResponseObject.setLng(lng);
      restResponseObject.setLocation("POINT(" + lng + " " + lat + ")");
      // GroovyResponseObject.setSummary("RandomGeneratedPoint");
      results.add(restResponseObject);
      amount = amount - 1;
    }
    return results;
  }

  /**
   * Create federated source response.
   *
   * @param requestUriInfo the request uri info
   * @param amount         the amount
   * @return the response
   */
  @GET
  @Path("/getSourceResults")
  public Response getResultsForSource(@Context UriInfo requestUriInfo, @QueryParam("startDate") String startDate,
                                      @QueryParam("endDate") String endDate, @QueryParam("topLeftLatLong") String
                                                topLeftLatLong, @QueryParam("bottomRightLatLong") String
                                                bottomRightLatLong, @QueryParam("amount") String amount

  ) {

    logger.debug("SourceEndpoint received query");
    int intAmount = 0;
    String topLeft = null;
    String bottomRight = null;
    Date start = null;
    Date end = null;
    ArrayList<UniversalFederatedSourceResponse> results = null;
    if (startDate != null) {
      try {
        start = dateFormat.parse(startDate);
      } catch (ParseException e) {
        e.printStackTrace();
      }
    } else {
      start = DEFAULT_START;
    }
    if (endDate != null) {
      try {
        end = dateFormat.parse(endDate);
      } catch (ParseException e) {
        e.printStackTrace();
      }
    } else {
      end = DEFAULT_END;
    }
    if (topLeftLatLong != null) {
      topLeft = topLeftLatLong;
    } else {
      topLeft = TOP_LEFT;
    }
    if (bottomRightLatLong != null) {
      bottomRight = bottomRightLatLong;
    } else {
      bottomRight = BOTTOM_RIGHT;
    }

    if (amount != null) {
      intAmount = getIntegerFromString(amount, 10);
      results = generateDataForResponse(intAmount, start, end, topLeft, bottomRight);
    }

    try {
      JsonApiResponse response = new JsonApiResponse();
      Response.ResponseBuilder builder = null;
      response.setData(results);
      Response here = null;
      if (response != null) {
        String responseData = response.getSanitizedJson();
        builder = Response.ok(responseData, MediaType.APPLICATION_JSON).header(HttpHeaders.CONTENT_LENGTH,
                responseData.getBytes("UTF-8").length);
        here = builder.build();
        logger.debug("SourceEndpoint Query Result Code  = {} ", String.valueOf(here.getStatus()));
      } else {
        logger.debug("SourceEndpoint ERROR There was a error handling the request. ");
        throw new SourceEndpointException("There was a error handling the request.", "There was a error handling the " +
                "request.", Response.Status.INTERNAL_SERVER_ERROR);
      }

      return here;
    } catch (Exception e) {
      logger.debug("SourceEndpoint ERROR There was a error handling the request. ");

      throw new SourceEndpointException("There was a error handling the request.", "There was a error handling the " +
              "request.", Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Get integer from string int.
   *
   * @param s     the s
   * @param value the value
   * @return the int
   */
  private int getIntegerFromString(String s, int value) {
    try {
      value = Integer.parseInt(s);
    } catch (Exception e) {

    }
    return value;
  }

  private ArrayList<UniversalFederatedSourceResponse> generateDataForResponse(int amount, Date start, Date end,
                                                                              String topLeft, String bottomRight) {


    String latSegments[] = StringUtils.split(topLeft);
    String lngSegments[] = StringUtils.split(bottomRight);
    double topLeftLat = Double.parseDouble(latSegments[0]);

    double topLeftLng = Double.parseDouble(latSegments[1]);

    double bottomRightLat = Double.parseDouble(lngSegments[0]);

    double bottomRightLng = Double.parseDouble(lngSegments[1]);

    //Y is lat
    //X is lon
    ArrayList<UniversalFederatedSourceResponse> results = new ArrayList<UniversalFederatedSourceResponse>();
    String startDate = transformDateTfr(start, dateFormat);
    String endDate = transformDateTfr(end, dateFormat);
    Date s = null;
    Date e = null;
    try {
      s = dateFormat.parse(startDate);
      e = dateFormat.parse(endDate);
    } catch (Exception ed) {
      logger.error(ed.getMessage());
    }

    results = buildObjectsForSource(amount, topLeftLat, topLeftLng, bottomRightLat, bottomRightLng, s, e);
    if (results.size() > 0 && results != null) {
      return results;
    } else {
      return null;
    }


  }

  /**
   * Transform date string.
   *
   * @param date       the date
   * @param dateFormat the date format
   * @return the string
   */
  public static String transformDateTfr(Date date, SimpleDateFormat dateFormat) {
    //  LOGGER.debug("Transforming query dates to AFG time");
    String transformedDate = null;
    Calendar c = Calendar.getInstance();
    TimeZone localTimeZone = c.getTimeZone();
    TimeZone afgTimeZone = TimeZone.getTimeZone("Asia/Kabul");
    //Returns the number of milliseconds since January 1, 1970, 00:00:00 for query date
    long msFromEpochQuery = date.getTime();
    //gives you the current offset in ms from GMT at the current date
    int localOffsetFromUTC = localTimeZone.getRawOffset();
    int afghanOffsetFromUTC = afgTimeZone.getRawOffset();
    //  LOGGER.debug("local offset is " + localOffsetFromUTC);
    //  LOGGER.debug("AFG offset is " + afghanOffsetFromUTC);
    //create a new calendar in GMT timezone, set to this date and add the offset
    Calendar afghanCal = Calendar.getInstance(afgTimeZone);
    afghanCal.setTimeInMillis(date.getTime());
    afghanCal.add(Calendar.MILLISECOND, (-1 * localOffsetFromUTC));
    afghanCal.add(Calendar.MILLISECOND, afghanOffsetFromUTC);
    //  LOGGER.debug("Original Date: " + date);
    //  LOGGER.debug("Created AFG date as [" + dateFormat.format(afghanCal.getTime()) + "]");
    return dateFormat.format(afghanCal.getTime());
  }

  private ArrayList<UniversalFederatedSourceResponse> buildObjectsForSource(int amount,
                                                                            Double topLeftLat,
                                                                            Double  topLeftLng,
                                                                            Double bottomRightLat,
                                                                            Double bottomRightLng,
                                                                            Date start,
                                                                            Date end) {
    ArrayList<UniversalFederatedSourceResponse> results = new ArrayList<>();

    while (amount > 0) {
      logger.debug("Entering always successful lat. Origin = {} Bound = {}",bottomRightLat,topLeftLat);

      double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
      lat = Double.parseDouble(decFormat.format(lat));
      logger.debug("Entering always successful lng. Origin = {} Bound = {}",topLeftLng,bottomRightLng);

      double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
      lng = Double.parseDouble(decFormat.format(lng));

      UniversalFederatedSourceResponse uniResponse = generateRandomMetacard(
              lat,
              lng,
              topLeftLat,
              topLeftLng,
              bottomRightLat,
              bottomRightLng,
              start,
              end);

      results.add(uniResponse);
      amount = amount - 1;
    }
    return results;
  }

  private UniversalFederatedSourceResponse generateRandomMetacard(double lat,
                                                                  double lng,
                                                                  Double topLeftLat,
                                                                  Double topLeftLng,
                                                                  Double bottomRightLat,
                                                                  Double bottomRightLng,
                                                                  Date start,
                                                                  Date end) {
    logger.debug("topLeftLat {}",topLeftLat);
    logger.debug("topLeftLng {}",topLeftLng);
    logger.debug("bottomRightLat {}",bottomRightLat);
    logger.debug("bottomRightLng {}",bottomRightLng);
    UniversalFederatedSourceResponse uniResponse = new UniversalFederatedSourceResponse();
    int whichGeom = ThreadLocalRandom.current().nextInt(5);
    logger.info("Array Number = {}", whichGeom);
    String wktString = constructWktString(whichGeom,
            topLeftLat,
            topLeftLng,
            bottomRightLat,
            bottomRightLng
    );
    uniResponse.setLocation(wktString);
    Date sigactDate = generateRandomDate(start, end);
    uniResponse.setClassification("UNCLASSIFIED");
    uniResponse.setDisplayTitle("SIGACT-" + sigactDate);
    uniResponse.setDateOccurred(sigactDate);
    uniResponse.setDisplaySerial(String.valueOf(topLeftLat) + String.valueOf(bottomRightLng) + String.valueOf(lat) +
            String.valueOf(lng));
    uniResponse.setLatitude(lat);
    uniResponse.setLongitude(lng);
    uniResponse.setOriginatorUnit(getRandomizedField(originateUnit));
    uniResponse.setPrimaryEventType(getRandomizedField(eventTypes));
    uniResponse.setReportLink(getRandomizedField(reportLinks));
    uniResponse.setSummary(getRandomizedField(summaries));

    return uniResponse;
  }

  public String constructWktString(int wktType,
                                   Double topLeftLat,
                                   Double topLeftLng,
                                   Double bottomRightLat,
                                   Double bottomRightLng) {
    String result = null;
    switch (wktType) {
      case POLYGON: {
        try {
          result = createRandomWktPolygon(topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
        } catch (Exception e) {
          logger.debug("Wkt Creation Failed For POLYGON");
        }
      }
      break;
      case MULTIPOLYGON: {
        try {
          result = createRandomWktMultiPolygon(topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
        } catch (Exception e) {
          logger.debug("Wkt Creation Failed For MULTIPOLYGON");
        }
      }
      break;
      case POINT: {
        try {
          result = createRandomWktPoint(topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
        } catch (Exception e) {
          logger.debug("Wkt Creation Failed For POINT");
          logger.warn("Exception = {}",e);

        }
      }
      break;
      case MULTIPOINT: {
        try {
          result = createRandomWktMultiPoint(topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
        } catch (Exception e) {
          logger.debug("Wkt Creation Failed For MULTIPOINT");
        }
      }
      break;
      case LINESTRING: {
        try {
          result = createRandomWktLine(topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
        } catch (Exception e) {
          logger.debug("Wkt Creation Failed For LINESTRING");
        }
      }
      break;
      case MULTILINESTRING: {
        try {
          result = createRandomWktMultiLine(topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
        } catch (Exception e) {
          logger.debug("Wkt Creation Failed For MULTILINESTRING");
        }
      }
      break;
    }
    logger.debug("WKT Result = {}",result);
    return result;
  }

  public Date generateRandomDate(Date dMin, Date dMax) {
    long MILLIS_PER_DAY = 1000 * 60 * 60 * 24;
    GregorianCalendar s = new GregorianCalendar();
    s.setTimeInMillis(dMin.getTime());
    GregorianCalendar e = new GregorianCalendar();
    e.setTimeInMillis(dMax.getTime());

    // Get difference in milliseconds
    long endL = e.getTimeInMillis() + e.getTimeZone().getOffset(e.getTimeInMillis());
    long startL = s.getTimeInMillis() + s.getTimeZone().getOffset(s.getTimeInMillis());
    long dayDiff = (endL - startL) / MILLIS_PER_DAY;

    Calendar cal = Calendar.getInstance();
    cal.setTime(dMin);
    cal.add(Calendar.DATE, new Random().nextInt((int) dayDiff));
    return cal.getTime();
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

  private String createRandomWktPolygon(Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double
          bottomRightLng) {
    StringBuilder result = new StringBuilder();
    int numberOfPoints = ThreadLocalRandom.current().nextInt(80) + 1;
    double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
    lat = Double.parseDouble(decFormat.format(lat));
    double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
    lng = Double.parseDouble(decFormat.format(lng));
    result.append("POLYGON ((");
    result.append(lng + " " + lat + ",");
    for (int x = 0; x < numberOfPoints; x++) {
      if (x % 2 == 0) {

        result.append(lng + x + " " + lat + ",");
      } else {
        result.append(lng + " " + lat + x + ",");
      }
    }
    result.append("))");
    return result.toString();
  }

  private String createRandomWktMultiPolygon(Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double
          bottomRightLng) {
    StringBuilder result = new StringBuilder();
    int numberOfPolygons = ThreadLocalRandom.current().nextInt(4) + 1;

    result.append("MULTIPOLYGON (");
    for (int y = 0; y < numberOfPolygons; y++) {
      double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
      lat = Double.parseDouble(decFormat.format(lat));
      double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
      lng = Double.parseDouble(decFormat.format(lng));
      result.append("(");
      result.append(lng + " " + lat + ",");
      int numberOfPoints = ThreadLocalRandom.current().nextInt(80) + 1;
      for (int x = 0; x < numberOfPoints; x++) {
        if (x % 2 == 0) {
          result.append(lng + x + " " + lat + ",");
        } else {
          result.append(lng + " " + lat + x + ",");
        }
      }
      result.append(lng + " " + lat);
      result.append(")");
    }

    result.append(")");
    return result.toString();
  }

  private String createRandomWktPoint(Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double
          bottomRightLng) {
    String result = null;
    logger.debug("Entering generate lat. Origin = {} Bound = {}",bottomRightLat,topLeftLat);
    double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
    logger.debug("Exit generate lat. lat = {} ",lat);
    lat = Double.parseDouble(decFormat.format(lat));
    logger.debug("Lat after decimal format lat = {} ",lat);
    logger.debug("Entering generate lng. Origin = {} Bound = {}",bottomRightLng,topLeftLng);
    double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
    logger.debug("Exit generate lng. lng = {} ",lng);
    lng = Double.parseDouble(decFormat.format(lng));
    logger.debug("Lng after decimal format lng = {} ",lat);
    result = "POINT(" + lng + " " + lat + ")";
    return result;
  }

  private String createRandomWktMultiPoint(Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double
          bottomRightLng) {
    StringBuilder result = new StringBuilder();
    int numberOfPoints = ThreadLocalRandom.current().nextInt(20) + 1;
    double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
    lat = Double.parseDouble(decFormat.format(lat));
    double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
    lng = Double.parseDouble(decFormat.format(lng));
    result.append("MULTIPOINT (");
    result.append("(" + lng + " " + lat + "),");
    for (int x = 0; x < numberOfPoints; x++) {
      if (x % 2 == 0) {

        result.append("(" + lng + x + " " + lat + "),");
      } else {
        result.append("(" + lng + " " + lat + x + "),");
      }
    }
    String fix = result.toString();
    fix = fix.substring(0, fix.length() - 1);
    fix = fix + ")";
    return fix;
  }

  private String createRandomWktLine(Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double
          bottomRightLng) {
    StringBuilder result = new StringBuilder();
    int numberOfPoints = ThreadLocalRandom.current().nextInt(20) + 1;
    double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
    lat = Double.parseDouble(decFormat.format(lat));
    double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
    lng = Double.parseDouble(decFormat.format(lng));
    result.append("LINESTRING (");
    result.append(lng + " " + lat + ",");
    for (int x = 0; x < numberOfPoints; x++) {
      if (x % 2 == 0) {

        result.append(lng + x + " " + lat + ",");
      } else {
        result.append(lng + " " + lat + x + ",");
      }
    }
    String fix = result.toString();
    fix = fix.substring(0, fix.length() - 1);
    fix = fix + ")";
    return fix;
  }

  private String createRandomWktMultiLine(Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double
          bottomRightLng) {
    StringBuilder result = new StringBuilder();
    int numberOfLines = ThreadLocalRandom.current().nextInt(4) + 1;
    result.append("MULTILINESTRING (");

    for (int y = 0; y < numberOfLines; y++) {
      StringBuilder innerLoop = new StringBuilder();
      innerLoop.append("(");
      int numberOfPoints = ThreadLocalRandom.current().nextInt(20) + 1;
      double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
      lat = Double.parseDouble(decFormat.format(lat));
      double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
      lng = Double.parseDouble(decFormat.format(lng));

      innerLoop.append(lng + " " + lat + ",");
      for (int x = 0; x < numberOfPoints; x++) {
        if (x % 2 == 0) {

          innerLoop.append(lng + x + " " + lat + ",");
        } else {
          innerLoop.append(lng + " " + lat + x + ",");
        }
      }
      String fix = innerLoop.toString();
      fix = fix.substring(0, fix.length() - 1);
      fix = fix + ")";
      result.append(fix);
    }
    result.append(")");
    return result.toString();
  }
  private void populateArrays() {
    //summaries
    summaries.add("crashing of hijacked planes into World Trade Center, New York City, New York, Pentagon in " +
            "Alexandria, Virginia, and site in Pennsylvania, USA");
    summaries.add("armed attack on city by Boko Haram");
    summaries.add("armed attack and arson of villages by Boko Haram");
    summaries.add("multiple car bombings in Al-Adnaniyah and Al-Qataniyah");
    summaries.add("arson of theater");
    summaries.add("hostage taking at school (includes 35 terrorists killed)");
    summaries.add("mid-air bombings of Air India flight off Cork, Ireland, killing 329; bomb intended for second Air " +
            "India flight exploded in Narita Airport, Japan, killing 2 and injuring 4");
    summaries.add("15 bombings throughout city");
    summaries.add("armed attack on market, after which many buildings were set on fire");
    summaries.add("truck bomb explodes outside U.S. embassy in Nairobi, Kenya, destroying adjacent office building; " +
            "244 killed, 4,877 injured; within five minutes,");
    summaries.add("a truck bomb explodes outside U.S. embassy in Dar es Salaam, Tanzania; 12 killed");
    summaries.add("simultaneous truck bombings of U.S. Marine and French barracks");
    summaries.add("armed attack");
    summaries.add("armed attack");
    summaries.add("mid-air bombing of Pan Am flight over Lockerbie, Scotland");
    summaries.add("bombing and sinking of ship carrying Jewish immigrants");
    summaries.add("hostage taking at Grand Mosque (includes 87 terrorists killed)");
    summaries.add("armed attack and arson at refugee camp");
    summaries.add("attacks at Sidi Moussa and Hais Rais");
    summaries.add("apparent mid-air bombing of Russian Airbus over Sinai after departing Sharm el-Sheikh airport; " +
            "plane broke apart in midair");
    summaries.add("armed attacks on villages of Ungwar Sankwai, Ungwar Gata, and Chenshyi");
    summaries.add("intentional crash of Egypt Air flight off Nantucket Island by copilot");
    summaries.add("seven bombings on commuter trains");
    summaries.add("car bombing outside nightclub");
    summaries.add("five car bombings and two mortar attacks in Sadr City, Baghdad");
    summaries.add("multiple bombings");
    summaries.add("bombings of four trains");
    summaries.add("armed attack on village");
    summaries.add("suicide bombings at shrines in Karbala and Kadhimiya");
    summaries.add("Boko Haram attack on village");
    summaries.add("attacks at Had Chekala, Remka, and Ain Tarik, Algeria");
    summaries.add("multiple suicide bombings and shooting attacks");
    summaries.add("multiple suicide truck bombings in Armili and area, Iraq");
    summaries.add("multiple bombings and gun attacks");
    summaries.add("multiple shooting and grenade attacks and hostage takings (includes 9 terrorists killed)");
    summaries.add("mid-air bombing of French UTA flight near Bilma");
    summaries.add("hostage taking and attempted rescue in theater (includes 41 terrorists killed)");
    summaries.add("truck bombing of federal building, causing partial collapse");
    summaries.add("bombing by communist terrorists of Sveta Nedelya Cathedral during funeral; cathedral dome " +
            "collapsed, killing mostly officials attending the funeral");
    summaries.add("two vehicle bombings at government buildings");
    summaries.add("two truck bombings");
    summaries.add("attack on train south of Luanda");
    summaries.add("attack on college campus by multiple gunmen");
    summaries.add("armed attack on crowds");
    summaries.add("sabotage resulting in derailment of Jnaneswari express train and collision with second train");
    summaries.add("shooting and bombing attack on school");
    summaries.add("hostage taking at hospital and two failed rescue attempts");
    summaries.add("ambush of civilian traffic on highway");
    summaries.add("two bombings at each of two mosques");
    summaries.add("bombing of motorcade for former prime minister Bhutto");
    summaries.add("armed attack at two mosques");
    summaries.add("FARC rebels attack police barracks with rockets, killing police officers and 10 civilians");
    summaries.add("shooting and hostage-taking at a theater; shooting attacks on three restaurants; four suicide " +
            "bombings outside a soccer stadium and at a restaurant");
    summaries.add("truck bombing in marketplace");
    summaries.add("two suicide bombings and additional attacks");
    summaries.add("multiple bombings in Baghdad, Khalis, and Mahmudiya");
    summaries.add("car bombing outside medical clinic");
    summaries.add("crash of hijacked PRC airliner");
    summaries.add("bombing of apartment building");
    summaries.add("five car bombings");
    summaries.add("Tamil Tiger roadway ambush of Sinhalese in five vehicles near Alut Oya");
    summaries.add("crash of hijacked Ethiopian Air flight off Comoros during forced landing off shore");
    summaries.add("Mi-26 helicopter carrying Russian troops shot down by terrorist-fired missile shortly before " +
            "landing; 115 killed immediately, 12 died of injuries; 21 survivors included 14 injured");
    summaries.add("car bombing outside mosque");
    summaries.add("bombings in Karbala, Ramadi, and Baghdad");
    summaries.add("armed attack at mosque");
    summaries.add("bombing attack on mosque");
    summaries.add("bombings at security checkpoint, billiards hall, nearby street, and market in Quetta, and bombing " +
            "at mosque in Mingora");
    summaries.add("car bombing at marketplace");
    summaries.add("armed attack on village including bombing of church");
    summaries.add("two car bombings at market and bus station");
    summaries.add("bombing and fire on ferry near Manila");
    summaries.add("attack by 50 gunmen on village");
    summaries.add("multiple bombings in Taji, Mosul, Baghdad, Dhuluiya, Baquba, and Diyala");
    summaries.add("truck bombing in marketplace");
    summaries.add("mid-air bombing of Korean Air flight near Burma");
    summaries.add("bombings in Baghdad, Hilla, Basra, Balad, Jisr Diyala, Samarra, Mosul, Baiji, Rutba, Baquba, and " +
            "Tuz Khormato, and armed attacks in Haditha");
    summaries.add("crash of Gulf Air flight following mid-air bombing over the UAE");
    summaries.add("multiple bombings in Hilla, Basra, al-Suwayra, Baghdad, Tarmiyah, Fallujah, Mosul, and " +
            "Iskandariyah, and additional armed attacks in Baghdad");
    summaries.add("mid-air bombing of Avianca flight in Bogota, killing all 107 aboard plus 3 on the ground");
    summaries.add("truck bombing of anti-Taliban tribal meeting");
    summaries.add("two suicide bombings of political party offices");
    summaries.add("bombings at a mosque, market, and football game spectators in Maiduguri; bombing of market in " +
            "Monguno");
    summaries.add("bombings in Baghdad, Amara, Kirkuk, Taji, Maysan, Tuz Khormato, Nasiriyah, Basra, Tal Afar, " +
            "Hawija, and Ar Riyad, and armed attack on Dujail army base");
    summaries.add("bombings in Baghdad, Tal Afar, Kirkuk, Daquq, Garma, Kut, Husainiya, Tuz Khurmato, and armed " +
            "attacks in Mushahda, Falluja, Al-A'amiriya, and Baaj");
    summaries.add("Tamil Tiger bombing of bus depot");
    summaries.add("two suicide bombings");
    summaries.add("crash of airliner struck by missile");
    summaries.add("suicide bombing at dogfighting festival");
    summaries.add("multiple bombings at government sites");
    summaries.add("bombings in Mosul and Baghdad; armed attacks in Ramadi and Baquba");
    summaries.add("suicide bombing of military convoy near Habarana");
    summaries.add("attack on movie theater and mosque");
    summaries.add("two suicide bombings at political rally");
    summaries.add("hostage taking and army storming of mosque");
    summaries.add("bombings in Baghdad and Baquba");
    summaries.add("bombing of Siguranzia palace in Bolgard (Ograd?) in Bessarabia");
    summaries.add("50 M-19 terrorists seize Palace of Justice in Bogota, Columbia; 12 judges killed, palace is " +
            "stormed, fire results and kills all terrorists and additional hostages");
    summaries.add("bombings and shooting attacks in Baghdad, Beiji, and Mosul");
    summaries.add("suicide bombing in marketplace near gas tanker and mosque");
    summaries.add("bombings and shooting attacks on churches, police headquarters, government buildings, and banks");
    summaries.add("mid-air bombing of Aeroflot airliner over Siberia");
    summaries.add("crash of hijacked Malaysian Boeing 747 airliner in Straits of Johore near Malaysia");
    summaries.add("suicide bombing with truck carrying chlorine tanks");
    summaries.add("bombing at open air market involving chlorine release");
    summaries.add("water poisoning with pesticide at constabulary");
    summaries.add("police officers poisoned, then shot");
    summaries.add("attack with chemical grenades on village of Ormancik");
    summaries.add("suicide bombing with dump truck carrying chlorine tank");
    summaries.add("sarin nerve gas attack in subway");
    summaries.add("bombing of chlorine tanker truck near restaurant");
    summaries.add("poisoning and armed attack on police");
    summaries.add("two suicide bombings in Falluja and one in Ramadi, all trucks carrying chlorine tanks resulting in" +
            " chemical releases");
    summaries.add("possible poisoning of food");
    summaries.add("sarin nerve gas attack, attributed to Aum Shinrikyo");
    summaries.add("explosion of car bomb carrying chlorine tanks");
    summaries.add("explosion of chlorine tanker near restaurant");
    summaries.add("poisoning and armed attack on police outpost");
    summaries.add("anthrax-laced letters mailed to Washington, DC");
    summaries.add("poisoning attack on police");
    summaries.add("poisoning attack on police");
    summaries.add("suicide bombing with car carrying chlorine tanks");
    summaries.add("acid attack on children");
    summaries.add("anthrax-laced letters mailed to West Palm Beach, Florida, USA, and New York City, New York, USA");
    summaries.add("truck bombing involving chlorine");
    summaries.add("cyanide poisoning of bread for SS prisoners in Camp Stalag 13 near Nuremberg");
    summaries.add("salmonella poisoning in restaurants by followers of Bhadwan Shree Rajneesh");
    summaries.add("tear gas attack");
    summaries.add("gas poisoning at girls' school");
    summaries.add("water poisoning at girls' school");
    summaries.add("poisoning attack on girls' school");
    summaries.add("poisoning attack on girls' school");
    summaries.add("gas poisoning at girls' school");
    summaries.add("gas poisoning at girls' school");
    summaries.add("gas poisoning at girls' school");
    summaries.add("gas poisoning at girls' school");
    summaries.add("description");
    summaries.add("crashing of hijacked planes into World Trade Center, New York City, New York, Pentagon in " +
            "Alexandria, Virginia, and site in Pennsylvania, USA");
    summaries.add("sarin nerve gas attack in subway");
    summaries.add("truck bomb explodes outside U.S. embassy in Nairobi, Kenya, destroying adjacent office building; " +
            "244 killed, 4,877 injured; within five minutes, a truck bomb explodes outside U.S. embassy in Dar es " +
            "Salaam, Tanzania; 12 killed");
    summaries.add("cyanide poisoning of bread for SS prisoners in Camp Stalag 13 near Nuremberg");
    summaries.add("bombings of four trains");
    summaries.add("multiple car bombings in Al-Adnaniyah and Al-Qataniyah");
    summaries.add("15 bombings throughout city");
    summaries.add("suicide truck bombing of Central Bank");
    summaries.add("truck bombing in garage of World Trade Center");
    summaries.add("truck bombing at federal police building in Bogota");
    summaries.add("bombings of three subway trains and one bus");
    summaries.add("salmonella poisoning in restaurants by followers of Bhadwan Shree Rajneesh");
    summaries.add("hostage taking at school (includes 35 terrorists killed)");
    summaries.add("two vehicle bombings at government buildings");
    summaries.add("seven bombings on commuter trains");
    summaries.add("truck bombing in public square");
    summaries.add("possible poisoning of food");
    summaries.add("multiple suicide bombings and shooting attacks");
    summaries.add("truck bombing of federal building, causing partial collapse");
    summaries.add("hostage taking and attempted rescue in theater (includes 41 terrorists killed)");
    summaries.add("suicide bombings at shrines in Karbala and Kadhimiya");
    summaries.add("two car bombings outside government house");
    summaries.add("hostage taking at Grand Mosque (includes 87 terrorists killed)");
    summaries.add("bombing of train");
    summaries.add("multiple bombings at government sites");
    summaries.add("bombing of motorcade for former prime minister Bhutto");
    summaries.add("two car bombings, with telephoned warning directing evacuees towards one bomb");
    summaries.add("truck bombing at U.S. military housing complex near Dhahran");
    summaries.add("bombing by communist terrorists of Sveta Nedelya Cathedral during funeral; cathedral dome " +
            "collapsed, killing mostly officials attending the funeral");
    summaries.add("car bombing outside mosque");
    summaries.add("armed attack on town");
    summaries.add("car bombing");
    summaries.add("twelve bombings in Guwahati, Kokrajhar, Barpeta Road, and Bongaigaon");
    summaries.add("five car bombings");
    summaries.add("hostage taking at hospital and two failed rescue attempts");
    summaries.add("multiple bombings:  27 police recruits killed and 26 injured by suicide bombing at a police " +
            "academy; 15 killed and 400 injured (including 15 children injured) by a truck bombing in a residential " +
            "area near an army base");
    summaries.add("bombings in Baghdad, Amara, Kirkuk, Taji, Maysan, Tuz Khormato, Nasiriyah, Basra, Tal Afar, " +
            "Hawija, and Ar Riyad, and armed attack on Dujail army base");
    summaries.add("two suicide bombings at political rally");
    summaries.add("two bombings outside mosques");
    summaries.add("suicide bombings of British consulate and bank");
    summaries.add("multiple bombings in Hilla, Basra, al-Suwayra, Baghdad, Tarmiyah, Fallujah, Mosul, and " +
            "Iskandariyah, and additional armed attacks in Baghdad");
    summaries.add("twin suicide attacks");
    summaries.add("multiple shooting and grenade attacks and hostage takings (includes 9 terrorists killed)");
    summaries.add("two bombings at each of two mosques");
    summaries.add("shooting and hostage-taking at a theater; shooting attacks on three restaurants; four suicide " +
            "bombings outside a soccer stadium and at a restaurant");
    summaries.add("two suicide bombings in Falluja and one in Ramadi, all trucks carrying chlorine tanks resulting in" +
            " chemical releases");
    summaries.add("two truck bombings");
    summaries.add("car bombing outside nightclub");
    summaries.add("bombings in Baghdad, Tal Afar, Kirkuk, Daquq, Garma, Kut, Husainiya, Tuz Khurmato, and armed " +
            "attacks in Mushahda, Falluja, Al-A'amiriya, and Baaj");
    summaries.add("truck bombing in marketplace");
    summaries.add("bombings in Baghdad, Hilla, Basra, Balad, Jisr Diyala, Samarra, Mosul, Baiji, Rutba, Baquba, and " +
            "Tuz Khormato, and armed attacks in Haditha");
    summaries.add("bombings in Mosul and Baghdad; armed attacks in Ramadi and Baquba");
    summaries.add("multiple car bombings in Kirkuk, Tuz Khurmato, Baghdad, and other cities");
    summaries.add("16 car bombings during Shiite pilgrimage");
    summaries.add("suicide bombing by Wehrsportsgruppe Neo-Nazi bomber");
    summaries.add("two suicide bombings and additional attacks");
    summaries.add("armed attack and arson of villages by Boko Haram");
    summaries.add("Armed Revolutionary Nuclei bomb at railway station");
    summaries.add("car bombing by FLP of PLO office");
    summaries.add("twin car bombs and two other bombs in shopping area");
    summaries.add("11 bombings at cafes, markets, and restaurants during religious festival");
    summaries.add("bombing at airport");
    summaries.add("bomb in horse-drawn wagon exploded near Morgan bank in lower Manhattan");
    summaries.add("bombing of bus");
    summaries.add("suicide bombing at Shia mosque");
    summaries.add("President Abraham Lincoln shot by John Wilkes Booth in Washington DC; secretary of state William H" +
            ". Seward injured separately by accomplice Lewis Powell; Lincoln died 15 April");
    summaries.add("President James Garfield shot by Charles J. Guiteau in Washington, DC, died 19 Sep");
    summaries.add("bomb thrown during labor rally at Haymarket Square kills 7 policemen, many injured; police fire " +
            "into crowd, killing 4; 8 anarchists accused");
    summaries.add("Kentucky Representative William Taulbee shot outside U.S. Capitol by Charles Kincaid; Taulbee died" +
            " 11 March");
    summaries.add("President William McKinley shot by Leon Czolgosz in Buffalo, NY, died 14 Sep");
    summaries.add("Frank Steunenberg, former Idaho governor, killed by bomb");
    summaries.add("bombing by labor activists at The Los Angeles Times building caused partial collapse of the " +
            "building; two bombs at other locations were defused");
    summaries.add("former President Theodore Roosevelt shot and injured in attempted assassination outside hotel en " +
            "route to speech");
    summaries.add("shooting attack on financier J. P. Morgan in failed hostage taking");
    summaries.add("bomb in suitcase explodes at Preparedness Day parade");
    summaries.add("bombing at police station");
    summaries.add("3 Chinese assassinated");
    summaries.add("mail bomb sent to home of senator Thomas Hardwick explodes, injuring a housekeeper and the " +
            "senator's wife");
    summaries.add("anarchist bombings kill a night watchman and a terrorist; additional bombings occurred in " +
            "Washington, DC, Philadelphia, PA, Paterson, NJ, Cleveland, OH, and two in Pittsburgh, PA");
    summaries.add("anarchist bombing at the home of a state representative injured one child; a second bombing at the" +
            " house of a judge caused no injuries");
    summaries.add("bomb in horse-drawn wagon exploded near Morgan bank in lower Manhattan");
    summaries.add("shooting by striking workers at labor protest");
    summaries.add("explosion of bomb placed in school, followed by suicide bombing");
    summaries.add("silver nitrate poison mailed to 9 New York City officials");
    summaries.add("attempted assassination of President-elect Franklin Roosevelt by anarchist Joseph Zangara; Chicago" +
            " mayor Anton Cermak shot instead, dying 6 March");
    summaries.add("mid-air bombing destroys a Boeing 247");
    summaries.add("Senator Huey Long shot by Carl Weiss who was shot and killed by bodyguards; Long died 10 Sep");
    summaries.add("plane hijacked from Brooklyn and flown across Atlantic, crashing in Irish Free State; 1 injured");
    summaries.add("police officer shot by Puerto Rican nationalists");
    summaries.add("police officer killed in shooting attack by Puerto Rican nationalists");
    summaries.add("peace advocate drowned trying to bomb Japanese steamer");
    summaries.add("bomb explodes at British Pavilion at the World's Fair, killing 2 police officers and injuring 2");
    summaries.add("shooting by guard at German prisoners in POW camp");
    summaries.add("Stephen J. Supona dropped homemade bomb from airplane over United Nations building; no damage " +
            "caused");
    summaries.add("shooting attack in residential neighborhood");
    summaries.add("in assassination attempt on President Harry Truman, two Puerto Rican nationalists try to shoot " +
            "their way into Blair House; 2 killed, including 1 terrorist");
    summaries.add("NAACP state director Harry Moore and his wife killed in bombing of their house");
    summaries.add("Puerto Rican nationalists fire from gallery of U.S. House of Representatives; 5 Congressman " +
            "injured");
    summaries.add("bomb explodes in seat at Radio City Music Hall during a movie showing");
    summaries.add("United Air Lines DC-8 exploded and crashed near Longmont, CO, destroyed by bomb planted by John " +
            "Graham in insurance plot to kill his mother, a passenger");
    summaries.add("bomb explodes at Paramount Theater");
    summaries.add("Cubana Airlines flight hijacked from Miami by members of 26th of July Movement; plane crashed near" +
            " Punta Tabaio, Cuba, killing 17 of 20 aboard");
    summaries.add("suicide bombing at elementary school");
    summaries.add("National Airlines flight bombed in insurance plot, crashing near Boliva, NC");
    summaries.add("criticality excursion and explosion at SL-1 reactor, apparently due to intentional removal of " +
            "control rod in murder/suicide act");
    summaries.add("Puerto Rican hijacks National Airlines plane to Havana, Cuba");
    summaries.add("Continental flight bombed");
    summaries.add("Medgar Evers, NAACP Mississippi field secretary, shot and killed at his home");
    summaries.add("bomb exploded under the steps of the Sixteenth Street Baptist Church, killing 4 young girls (ages " +
            "11-14) attending Sunday school");
    summaries.add("President John Kennedy shot and killed by Lee Harvey Oswald, himself later fatally shot by Jack " +
            "Rudy before trial");
    summaries.add("Frank Gonzalez, intending to commit suicide, shot pilot of Pacific Air Lines flight, causing plane" +
            " to crash");
    summaries.add("three civil rights workers kidnapped in Mississippi, bodies found 4 Aug, 7 whites convicted of " +
            "murders");
    summaries.add("three individuals, including members of Black Liberation Front and Montreal Separatist Party, " +
            "arrested plotting to bomb Liberty Bell, Statue of Liberty, and Washington Monument");
    summaries.add("Malcolm X fatally shot");
    summaries.add("civil rights protestor killed by Ku Klux Klan");
    summaries.add("black riots in Watts; $20 million in damage");
    summaries.add("firebombing by Ku Klux Klan");
    summaries.add("black riots in Watts");
    summaries.add("sniper shooting from tower on University of Texas campus");
    summaries.add("R. Parks commits suicide in bombing of Las Vegas motel");
    summaries.add("simultaneous bombing of Yugoslav missions in Washington, DC, Chicago, IL, San Francisco, CA, New " +
            "York City, NY, Ottawa, Canada, and Toronto, Canada; the Washington bombing injured two embassy employees");
    summaries.add("26 Black Panthers walk into California State Legislature in Sacramento carrying loaded guns to " +
            "read a political statement");
    summaries.add("black riots");
    summaries.add("black riots");
    summaries.add("Martin Luther King Jr. shot and killed by James Earl Ray");
    summaries.add("Robert Kennedy shot by Jordanian Sinhan Bishara Sirhan, died 6 Jun");
    summaries.add("gunfight at Black Panther Party headquarters, injures 13 policemen");
    summaries.add("bombing of Marine Midland Building");
    summaries.add("three killed, apparently while building bombs for Weathermen");
    summaries.add("Eastern Airlines flight hijacked; copilot shot and killed");
    summaries.add("bombing at Sterling Hall kills scientist at University of Wisconsin; bomb planted in protest of " +
            "Vietnam War");
    summaries.add("US Army learns of Weathermen plot to blackmail homosexual officer at Fort Detrick, MD, to steal a " +
            "biological weapon for use in the water supply of a major city");
    summaries.add("Weather Underground bombing of Senate wing of U.S. Capitol");
    summaries.add("JDL members fire into apartment occupied by members of Soviet UN delegation");
    summaries.add("two teenagers, members of RISE, arrested for plotting to introduce typhoid bacteria into Chicago " +
            "water supply");
    summaries.add("two police officers killed");
    summaries.add("Jewish Defense League firebombs two Sol Hurok offices; one killed at each location, with 13 total " +
            "additional injuries");
    summaries.add("Black Liberation Army kills 2 police officers");
    summaries.add("caller threatens to bomb four TWA airliners, one every 6 hours, unless a $2,000,000 ransom was " +
            "paid; caller stated bomb was aboard TWA Flight 7 en route from New York City to Los Angeles; flight " +
            "returned to New York City and the bomb defused");
    summaries.add("additional attack on TWA airliner; bomb exploded in emptied airliner in Las Vegas following its " +
            "arrival from New York City");
    summaries.add("intentional irradiation of child using radioactive sources");
    summaries.add("Alabama governor George Wallace and 3 others injured, shot by Arthur Bremer");
    summaries.add("shootings by former Black Panther member");
    summaries.add("attempted bombings of three Israeli targets");
    summaries.add("Israeli military attache Colonel Yosef Alon shot outside his home, claimed by PFLP");
    summaries.add("SLA kills Marcus Foster, black school superintendent");
    summaries.add("gunman attempts to hijack plane in Baltimore and fly it into White House to kill President Nixon; " +
            "kills 2 at airport and himself, injures one");
    summaries.add("6 SLA suspects in Hearst kidnapping killed in gun battle with police");
    summaries.add("bomb explodes in locker at airport, later attributed to the Alphabet Bomber");
    summaries.add("Alphabet Bomber, having threatened to kill the President with nerve gas, arrested with all but one" +
            " of necessary components");
    summaries.add("FALN bombing injures police officer");
    summaries.add("FALN bombing of Fraunces Tavern in Wall Street");
    summaries.add("Weather Underground bombing of U.S. State Department");
    summaries.add("Secret Service agent prevents Lynette Fromme, a Charles Manson follower, from shooting President " +
            "Gerald Ford");
    summaries.add("President Gerald Ford unharmed in assassination attempt by Sara Jane Moore, a political activist");
    summaries.add("bomb explodes in locker at La Guardia Airport; Croatian nationalists suspected");
    summaries.add("B. A. Fox threatens to use mail to disperse ticks carrying pathogens");
    summaries.add("gunman scales White House fence, is shot and killed by guards");
    summaries.add("Croatian terrorists hijack TWA jet and have it flown to France; 1 policeman killed by bomb at " +
            "Grand Central Terminal in New York City");
    summaries.add("two Chileans killed in car bombing");
    summaries.add("Hanafi Muslim gunmen seize 3 buildings in Washington, DC, and hold 134 hostages for 39 hours " +
            "before surrendering");
    summaries.add("bombing attributed to Chicano activist");
    summaries.add("bomb explodes in locker at airport");
    summaries.add("FALN bombs two office buildings; 1 killed at Mobil headquarters, 7 injured");
    summaries.add("mail bomb slightly injures campus police officer at Northwestern University");
    summaries.add("extortion attempt threatening release of uranium dioxide");
    summaries.add("FALN bombing at Shubert Theatre");
    summaries.add("bomb slightly injures student at Northwestern University");
    summaries.add("shooting attack at protest");
    summaries.add("bomb ignites on American Airlines flight which lands safely; 12 passengers suffer from smoke " +
            "inhalation");
    summaries.add("Macheteros members ambush Navy bus in Puerto Rico, killing 2 sailors and injuring 10");
    summaries.add("Vernon Jordan Jr., civil rights leader, shot and injured");
    summaries.add("mail bomb injures president of United Airlines");
    summaries.add("Ali Akbar Tabataba'i, former senior officer in Iranian Shah's SAVAK, shot at home by Daoud " +
            "Salahuddin, a radical black Muslim under instructions from Iran");
    summaries.add("Macheteros terrorists bomb 9 Air National Guard jets, causing $40 million in damage");
    summaries.add("Ku Klux Klan attack");
    summaries.add("President Ronald Reagan and 3 others injured in attempted assassination by Hinkley");
    summaries.add("bomb explodes in JFK airport terminal");
    summaries.add("fatal self-inflicted radiation dose using stolen source");
    summaries.add("Weather Underground member Kathy Boudin captured after killing 3");
    summaries.add("UFF members murder New Jersey State Police officer");
    summaries.add("Kemal Arikan, Turkish Consul-General, assassinated by Armenian terrorists");
    summaries.add("Orhan Gunduz, honorary Turkish Consul in Boston, assassinated by Armenian terrorists");
    summaries.add("mail bomb injures secretary at Vanderbilt University");
    summaries.add("shooting attack on navy sailors");
    summaries.add("mail bomb injures professor at University of California");
    summaries.add("shooting attack");
    summaries.add("two bombings in Manhattan and Brooklyn by FALN");
    summaries.add("bomb in car kills Armenian Victor Galustian");
    summaries.add("Ahmadiyya Movement in Islam secretary killed by members of Fuqra, a black Islamic sect; 2 members " +
            "killed setting fire in AMI temple");
    summaries.add("bombing at U.S. Capitol building; later linked to Revolutionary Armed Task Force");
    summaries.add("authorities prevent attempt by pro-Khomeini students to set fire to theater where 500 " +
            "anti-Khomeini Iranians were attending a singing performance");
    summaries.add("FALN bombings at federal and city buildings; 1 policeman injured");
    summaries.add("two Canadians arrested in NY attempting to purchase large amounts of pathogenic bacteria (tetanus " +
            "and botulinal toxin) from a Rockville, MD, firm");
    summaries.add("shooting attack at McDonalds restaurant");
    summaries.add("Alan Berg killed by white supremacists");
    summaries.add("followers of Bhagwan Shree Rajneesh use water to infect two officials with salmonella; both " +
            "sickened, one hospitalized");
    summaries.add("salmonella poisoning in restaurants by followers of Bhadwan Shree Rajneesh");
    summaries.add("Robert Matthews, leader of The Order, a right-wing group, killed in raid by federal agents");
    summaries.add("three abortion clinics bombed");
    summaries.add("letter writer threatens to contaminate New York City's water reservoirs with plutonium unless " +
            "charges against Bernhard Goetz are dropped; testing was announced to have detected femtocurie levels of " +
            "plutonium in the water on 26 July");
    summaries.add("police assault on headquarters of radical black group Move starts fire");
    summaries.add("mail bomb injures student at University of California");
    summaries.add("Tscherim Soobzokov, alleged Nazi war criminal, injured by bombing possibly linked to JDL; died 6 " +
            "Sep");
    summaries.add("Alex Odah, officer of American-Arab Anti-Discrimination Committee, killed by bombing possibly " +
            "linked to JDL");
    summaries.add("bombing injures two");
    summaries.add("Unabomber bomb kills Hugh Scrutton, a computer store owner, with bomb in paper bag behind store");
    summaries.add("two Aryan Nation members take 150 students and teachers hostage at an elementary school; bomb " +
            "accidentally explodes, killing one terrorist and injuring many children; second terrorist commits " +
            "suicide");
    summaries.add("members of Libyan-linked street gang El Rukn arrested attempting to obtain SAM to attack an " +
            "aircraft at O'Hare IAP");
    summaries.add("shooting attack by postal employee at post office");
    summaries.add("tear gas bomb set off 5 minutes before end of Russian dance troupe performance at New York City's " +
            "Metropolitan Opera House by Jewish extremists");
    summaries.add("four bombs explode in Coeur d'Alene, at department store, restaurant, federal building, and armed " +
            "forces recruiting station, set by Bruder Scheigen Strike Force II");
    summaries.add("Macheteros bombings at military facilities");
    summaries.add("6 members of Arizona Patriots indicted for planned bombings of the Phoenix ADL regional office, a " +
            "Phoenix synagogue, the Simon Wiesenthal Center in Los Angeles, and the Ogden Utah IRS facility");
    summaries.add("Dennis Malvasi sets bomb in Planned Parenthood building in Manhattan, leaving rental agent " +
            "handcuffed nearby; bomb fizzles");
    summaries.add("three employees set fire in Dupont Plaza Hotel; most fatalities were in the hotel casino; the " +
            "employees were in a labor dispute with the hotel's management");
    summaries.add("bombing injures computer store owner");
    summaries.add("apparent Islamic terrorist plot to bomb Atlantic City casinos called off due to alerted " +
            "authorities");
    summaries.add("Lebanese national and two others, all members of Syrian Socialist National Party, arrested " +
            "attempting to enter Vermont from Canada with bomb components");
    summaries.add("bomb exploded in parking lot of Sandia National Laboratories");
    summaries.add("Yu Kikumura, member of Japanese Red Army, arrested in New Jersey with bombs to be detonated in " +
            "Manhattan 3 days later");
    summaries.add("animal rights activist arrested leaving pipe bomb at U.S. Surgical Corporation");
    summaries.add("shooting attack on children in playground of elementary school; gunman then fatally shot himself");
    summaries.add("pipe-bomb exploded in van of Sharon Lee Rogers, wife of U.S.S. Vincennes captain, planted by " +
            "pro-Iranian terrorists");
    summaries.add("US FDA inspectors in Philadelphia discover two grapes laced with minimal amounts of cyanide in " +
            "shipment from Chile following warning telephoned to U.S. embassy in Santiago");
    summaries.add("child maimed by bomb in toothpaste tube in K-Mart store; apparent teenage perpetrator commits " +
            "suicide 20 April");
    summaries.add("explosion in gun turrent of battleship U.S.S. Iowa off Puerto Rico kills 47; Navy cites some " +
            "evidence of sabotage");
    summaries.add("gas canister in parcel explodes at NAACP regional office");
    summaries.add("Judge Robert Vance killed by mail bomb, wife injured");
    summaries.add("black civil rights lawyer Robert Robinson killed by mail bomb");
    summaries.add("Rashad Khalifa assassinated");
    summaries.add("arson fire in social club");
    summaries.add("two Earth First members injured in explosion while transporting bomb in car");
    summaries.add("shooting attack at GMAC office");
    summaries.add("Rabbi Meir Kahane assassinated by Al-Sayyid Abdulazziz Nossair");
    summaries.add("Mustafa Shalabi killed in Brooklyn by Islamic group members");
    summaries.add("shooting attack at Luby's restaurant");
    summaries.add("Minnesota Patriots Council plots to assassinate law enforcement officials using ricin");
    summaries.add("Parivash Rafizadeh, wife of former senior officer in Iranian Shah's SAVAK, shot near her home");
    summaries.add("federal marshals in shootout with white supremacist Randy Weaver in Idaho kill his wife and son");
    summaries.add("black riots following not guilty verdict in trial of four policemen for beating black offender");
    summaries.add("shooting attack at high school");
    summaries.add("Mir Amail Kansi, an Afghan Islamist, shot several CIA employees in cars in front of CIA " +
            "headquarters");
    summaries.add("truck bombing in garage of World Trade Center");
    summaries.add("Branch Davidian cult members kill 4 ATF agents, injure 16, when agents raided their compound in " +
            "Waco, TX; 10 cult members killed; compound was sieged until 19 Apr when another raid was attempted and " +
            "the compound burned down");
    summaries.add("abortionist David Gunn shot and killed by abortion opponent");
    summaries.add("bomb injures scientist from University of California");
    summaries.add("bomb injures professor at Yale University");
    summaries.add("Sheikh Omar Abdel Rahman and others arrested for role in World Trade Center bombing, thwarting " +
            "plans to bomb United Nation Headquarters, the Lincoln Tunnel, the Holland Tunnel, the George Washington " +
            "Bridge, and FBI offices in New York City");
    summaries.add("FBI arrests skinheads planning to machine gun worshippers at First African Methodist Episcopal " +
            "Church in Los Angeles in hopes of starting a race war");
    summaries.add("abortionist George Tiller shot and injured at an abortion clinic");
    summaries.add("Colin Ferguson shot and killed 6, injured 17 on Long Island train, professing hatred of whites");
    summaries.add("gunman fires at van of Orthodox Jewish students at the Brooklyn Bridge");
    summaries.add("mail bomb kills man and injures his wife");
    summaries.add("shooting attack at base hospital");
    summaries.add("abortion opponent shot and killed abortionist and his bodyguard and injured abortionist's wife");
    summaries.add("Frank Corder flew Cessna from MD into White House, striking tree near President's bedroom, killing" +
            " himself and causing damage to White House");
    summaries.add("lone gunman with semi-automatic weapon fires shots at White House from sidewalk in front on " +
            "Pennsylvania Avenue");
    summaries.add("Unabomber mail bomb kills New York advertising executive Thomas Mosser");
    summaries.add("gunman kills 2 abortion clinic workers in MA, then drives to Norfolk, VA, and fires on clinic " +
            "before arrest");
    summaries.add("truck bombing of federal building, causing partial collapse");
    summaries.add("Unabomber mail bomb kills Gilbert Murray, president of California Forestry Assn., at office");
    summaries.add("man with unloaded gun scales White House fence; jumper and Secret Service agent shot and injured " +
            "by another guard");
    summaries.add("car bombing at shopping mall, apparent murder plot");
    summaries.add("Amtrak train derailed near Hyder, AZ, by sabotage to tracks with nearby note claiming " +
            "responsibility by Sons of Gestapo, later attributed to railroad employee");
    summaries.add("Thomas Lewis Lavy arrested in Arkansas for possession of ricin, a biotoxin; Lavy commits suicide " +
            "the next day");
    summaries.add("radioactive source theft");
    summaries.add("several individuals arrested in plot to kill Republican officials; seized weapons included " +
            "radioactive materials");
    summaries.add("several individuals arrested in New York planning to kill Republican officials; seized weapons " +
            "included radioactive materials");
    summaries.add("mid-air explosion of TWA 800, attribution to accidental explosion has been disputed; victims " +
            "included 20 children and 38 French citizens");
    summaries.add("pipe bomb explodes in park at night concert at Summer Olympic Games; 1 killed, 1 died nearby of " +
            "heart attack");
    summaries.add("letter bombs received at Egyptian newspaper offices in Washington, DC, New York City, and a prison" +
            " in Leavenworth Kansas; similar device exploded at Egyptian newspaper office in London, UK, injuring 2 " +
            "guards");
    summaries.add("bomb explodes in Atlanta, GA, nightclub frequented by homosexuals; 4 injured");
    summaries.add("lone Palestinian gunman fired on tourists on observation deck of Empire State Building; Danish " +
            "national was killed and other tourists injured before gunman killed himself");
    summaries.add("discovery of mass suicide by 39 members of Heaven's Gate cult, tied by cult members to Comet " +
            "Hale-Bopp");
    summaries.add("James Dalton Bell allegedly investigates toxins for use in assassinating government officials");
    summaries.add("would-be Palestinian suicide bombers are arrested at their apartment while planning to bomb New " +
            "York subways");
    summaries.add("bombing at abortion clinic kills one guard and injures a nurse; Eric Rudolph suspected in case");
    summaries.add("shooting attack at middle school by two students; 4 students and 1 teacher killed, 9 students and " +
            "2 adults injured");
    summaries.add("shooting attacks at residence and high school");
    summaries.add("gunman enters U.S. Capitol building and kills two guards; one tourist and gunman are injured");
    summaries.add("arson attacks by the Earth Liberation Front at Vail ski resort cause $12 million in damages");
    summaries.add("abortionist shot and killed at his home");
    summaries.add("mass shooting at Columbine High School by two students; 12 students and 1 teacher killed, 21 " +
            "students and 2 teachers killed; both gunmen killed themselves");
    summaries.add("shooting attack at Jewish daycare by white supremacist");
    summaries.add("shooting attack at church service");
    summaries.add("intentional crash of Egypt Air flight off Nantucket Island by copilot");
    summaries.add("terrorist arrested crossing from Canada with material to bomb Los Angeles International Airport");
    summaries.add("gunman fires on the White House from outside the perimeter fence; gunman is shot and injured by a " +
            "guard");
    summaries.add("crashing of two hijacked planes into World Trade Center towers, causing fires and collapse");
    summaries.add("crashing of hijacked plane into Pentagon");
    summaries.add("crashing of hijacked plane into rural area of Pennsylvania, following attempt by passengers to " +
            "regain control of aircraft");
    summaries.add("anthrax-laced letters mailed to West Palm Beach, Florida, USA, and New York City, New York, USA");
    summaries.add("anthrax-laced letters mailed to Washington, DC");
    summaries.add("British citizen prevented from igniting shoe bomb on flight from Paris to Miami");
    summaries.add("US citizen arrested for seeking to use dirty bomb in US");
    summaries.add("Egyptian gunman kills two Israelis, injures four at the El Al ticket counter at the Los Angeles " +
            "International Airport");
    summaries.add("owner of Italian restaurant shot in robbery by Beltway snipers");
    summaries.add("6 U.S. citizens arrested for terrorist connections");
    summaries.add("liquor store employees shot in robbery by Beltway snipers");
    summaries.add("1 killed at grocery store by Beltway snipers");
    summaries.add("5 killed in separate shootings by Beltway snipers");
    summaries.add("1 killed at shopping mall by Beltway snipers");
    summaries.add("1 child injured at a middle school by Beltway snipers");
    summaries.add("1 killed at gas station by Beltway snipers");
    summaries.add("1 killed at gas station by Beltway snipers");
    summaries.add("1 killed at shopping mall by Beltway snipers");
    summaries.add("1 killed at restaurant by Beltway snipers");
    summaries.add("1 bus driver killed by Beltway snipers");
    summaries.add("US citizen arrested for planning to sabotage Brooklyn Bridge");
    summaries.add("11 arrested for planning attacks on U.S. servicemen");
    summaries.add("shooting attack at factory");
    summaries.add("arrest of terrorist plotting to bomb shopping mall in Columbus");
    summaries.add("2 arrested plotting assassination of Pakistani diplomat");
    summaries.add("2 arrested planning to bomb Penn Station during Republican National Convention");
    summaries.add("terror cell leader arrested in London for planning attacks on financial centers in the US");
    summaries.add("shooting at Red Lake Indian Reservation school");
    summaries.add("4 arrested plotting attacks on Los Angeles targets");
    summaries.add("4 injured, including several children, by incendiary attacks by suspected animal rights activists");
    summaries.add("1 arrested plotting attacks on refineries in Wyoming and New Jersey and on the transcontinental " +
            "pipeline");
    summaries.add("3 arrested plotting attacks on U.S. military abroad and on domestic targets");
    summaries.add("man drives vehicle into pedestrians at the University of North Carolina");
    summaries.add("2 arrested plotting attacks on U.S. Capitol and World Bank headquarters");
    summaries.add("7 arrested planning to bomb the Sears Tower");
    summaries.add("1 arrested planning to bomb train tunnels");
    summaries.add("gunman fires on women at the Jewish Federation of Greater Seattle");
    summaries.add("British authorities arrest 24 terrorists planning to use liquid explosives on airlines to attack " +
            "US targets");
    summaries.add("hostage taking and shooting attack at high school");
    summaries.add("hostage taking and shooting attack at Amish schoolhouse");
    summaries.add("1 arrested plotting grenade attack on Chicago area shopping mall");
    summaries.add("shooting attack at Virginia Polytechnic Institute");
    summaries.add("6 arrested plotting armed attack on Fort Dix");
    summaries.add("4 arrested in Trinidad plotting to bomb fuel pipelines near JFK airport");
    summaries.add("radioactive source theft");
    summaries.add("animal rights activists attempt home invasion of biomedical researcher, injuring the researcher's " +
            "husband");
    summaries.add("1 arrested plotting attacks on U.S. and European targets");
    summaries.add("gunman fires on congregation at a church");
    summaries.add("multiple shootings at residences and businesses in Samson and Geneva, AL");
    summaries.add("shooting attack at immigrant center");
    summaries.add("4 arrested plotting bombing attacks on New York Jewish centers and attacks against Air National " +
            "Guard aircraft");
    summaries.add("1 doctor killed (George Tiller) in shooting attack at Reformation Lutheran Church");
    summaries.add("1 Army private killed (William Long), second injured in shooting attack at Army Navy Career Center");
    summaries.add("1 guard killed (Stephen Johns) in shooting attack at the Holocaust Museum");
    summaries.add("abortion protester shot and killed outside a school; the gunman also killed an area businessman");
    summaries.add("US citizen arrested plotting to detonate car bomb at the federal building in Springfield, IL");
    summaries.add("terrorist arrested planning to bomb Dallas Fountain Place");
    summaries.add("1 arrested plotting attacks on shopping malls and assassinations of two politicians");
    summaries.add("shooting attack at Soldier Readiness Center at Foot Hood");
    summaries.add("Yemeni terrorist attempts to detonate bomb on flight from Amsterdam to Detroit; bomb only ignites," +
            " and passengers and crew subdue the terrorist");
    summaries.add("suicide crash of small plane into federal office building");
    summaries.add("shooting at gate outside Pentagon; gunman killed");
    summaries.add("failed car bombing in Times Square by Pakistani terrorists");
    summaries.add("2 arrested plotting mail bomb assassinations");
    summaries.add("3 hostages held by gunman at Discovery Communications headquarters; gunman killed by police");
    summaries.add("attempted shooting at Capitol Hill; gunman shot and injured by guards");
    summaries.add("Pakistani-American arrested plotting bombing attack on Washington subway");
    summaries.add("thwarted attempt to bomb multiple US-bound airliners with parcel bombs sent from Yemen");
    summaries.add("1 arrested plotting bombing at Christmas tree lighting ceremony in Portland");
    summaries.add("1 arrested plotting bombing of military recruiting center");
    summaries.add("shooting attack at political event at a supermarket; U.S. District Judge John Roll killed, U.S. " +
            "Representative Gabrielle Giffords injured");
    summaries.add("1 arrested plotting bombings of domestic targets");
    summaries.add("2 arrested plotting attacks on a Manhattan synagogue");
    summaries.add("2 arrested plotting attack on Seattle military recruiting station");
    summaries.add("thwarted attempt to attack restaurant near Fort Hood with bombing and shooting attack; Naser Abdo " +
            "arrested");
    summaries.add("shooting attack at restaurant, killing 4 (2 died immediately, 2 died later of injuries) and " +
            "injuring 7 others; casualties included 3 Nevada National Guard soldiers killed and 2 injured; gunman " +
            "also died of self-inflicted wounds");
    summaries.add("shooting attack at movie theater; suspect was arrested afterwards; suspect had booby-trapped his " +
            "nearby apartment with explosives which were successfully disarmed by police");
    summaries.add("6 killed, 4 injured in shooting attack at a Sikh temple shortly before worship service on Sunday " +
            "morning; one of those injured was a police officer, another was president of the temple; the gunman was " +
            "shot and killed at the scene by police");
    summaries.add("2 police officers killed, 1 injured while investigating attack that injured another officer; 7 " +
            "arrested, 2 of whom were injured in the shootout; several of those arrested had ties to the sovereign " +
            "citizen movement");
    summaries.add("1 guard shot and injured while subduing gunman at Family Research Council offices");
    summaries.add("shooting attack at elementary school kills 20 children and 6 adults; shooter killed himself and " +
            "had killed his mother earlier that day");
    summaries.add("two bombings at Boston Marathon kill 3 (including 1 child) and injure 183 (including 8 children)");
    summaries.add("two letters testing positive for ricin mailed to Mississippi Senator Roger Wicker and President " +
            "Obama are found at mail screening facilities; a third letter to an official in Mississippi was awaiting " +
            "testing; an individual in Mississippi is arrested and charged in the case");
    summaries.add("1 police officer killed, one injured during manhunt for the Boston Marathon bombers; one terrorist" +
            " killed and one injured and captured");
    summaries.add("two gunmen fired on crowds at Mother's Day parade; 19 injured, including 2 children");
    summaries.add("shooting attack at Washington Navy Yard");
    summaries.add("shooting attack at Los Angeles International Airport; 1 TSA officer killed, 2 TSA officers and " +
            "several civilians injured");
    summaries.add("shooting attack on Fort Hood; 3 killed, 16 injured; in addition the gunman killed himself");
    summaries.add("shooting attack at a Jewish community center and Jewish retirement home; 3 killed, including one " +
            "teenager");
    summaries.add("shooting attack killed 1");
    summaries.add("shooting attack near night club killed 2");
    summaries.add("shooting attack at restaurant and store; 3 killed, including 2 police officers; both shooters " +
            "killed themselves");
    summaries.add("shooting attack killed 1 teenager");
    summaries.add("shooting attack on police officers; shooter evaded a manhunt in nearby woods until 30 Oct");
    summaries.add("knife attack at food processing plant killed 1, injured 1; attacker was shot and injured");
    summaries.add("axe attack on police officers injured 2, one severely; police shot and killed the attacker and " +
            "injured one bystander");
    summaries.add("shots fired at Mexican consulate, US courthouse, and police station during early morning hours; " +
            "failed attempt at arson at consulate; attacker was shot at by police");
    summaries.add("shooting attack killed two police officers, gunman shot and killed himself");
    summaries.add("attempted shooting attack at event involving art critical of Islam, one guard shot and injured, " +
            "both attackers shot and killed by police officer serving as guard");
    summaries.add("gunman killed 9 in attack at Emanuel African Methodist Episcopal Church on a Bible study group; " +
            "South Carolina state congressman among those killed; 1 injured");
    summaries.add("gunman killed 4 Marines and injures 1 Navy sailor (who died 18 Jul of injuries), 1 police officer," +
            " and 1 Marine, at two locations; gunman was shot and killed by police");
    summaries.add("student stabbed two students and two staff at the University of California; attacker was shot and " +
            "killed by police");
    summaries.add("gunman killed two civilians and one police officer outside a Planned Parenthood clinic, also " +
            "injuring 4 civilians and 5 police officers");
    summaries.add("two attackers killed 14 and injured 21 at a county employee meeting and Christmas party; both " +
            "attackers were killed hours later in a shootout with police in which 2 police officers were injured");
    summaries.add("gunman shot and injured a police officer; attacker was shot and injured");
    summaries.add("attacker injures 4 in machete attack at a restaurant; attacker was shot and killed by police when " +
            "he attacked police at the end of a car chase");

    //originator unit
    originateUnit.add("1st Human Resources Center");
    originateUnit.add("24 Hour Counseling Hot Line - Army One Source");
    originateUnit.add("1st Armored Division, Fort Bliss, Texas");
    originateUnit.add("1st Battalion, 5th Infantry Regiment, Fort Wainright, Alaska");
    originateUnit.add("1st Battalion 10th Special Forces Group");
    originateUnit.add("1st Brigade Combat Team, 10th Mountain Division");
    originateUnit.add("1st Cavalry Division, Fort Hood, Texas");
    originateUnit.add("1st Infantry Division, Fort Riley, Kan.");
    originateUnit.add("1st Information Operations Command Land");
    originateUnit.add("1st Theater Sustainment Command");
    originateUnit.add("2nd Battalion, 1st Air Defense Artillery Regiment, Korea");
    originateUnit.add("2nd Cavalry Regiment");
    originateUnit.add("2nd Infantry Division, Camp Red Cloud, Korea");
    originateUnit.add("2nd Signal Brigade");
    originateUnit.add("2nd Stryker Brigade Combat Team, 2nd Infantry Division");
    originateUnit.add("3rd Armored Cavalry Regiment, Fort Hood, Texas");
    originateUnit.add("3rd Battlefield Coordination Detachment-Korea");
    originateUnit.add("3rd Stryker Brigade Combat Team, 2nd Infantry Division");
    originateUnit.add("3rd Infantry Division, Fort Stewart, Ga.");
    originateUnit.add("3rd U.S. Infantry Regiment (The Old Guard)");
    originateUnit.add("4th Infantry Division, Fort Carson, Colo.");
    originateUnit.add("4th Stryker Brigade Combat Team, 2nd Infantry Division");
    originateUnit.add("5th Battlefield Coordination Detachment");
    originateUnit.add("5th Signal Command");
    originateUnit.add("6th Medical Logistic Management Center");
    originateUnit.add("7th Army Joint Multinational Training Command");
    originateUnit.add("7th Civil Support Command (CSC)");
    originateUnit.add("7th Infantry Division");
    originateUnit.add("7th Signal Command (Theater)");
    originateUnit.add("7th Theater Tactical Signal Brigade");
    originateUnit.add("8th Theater Sustainment Command, Fort Shafter, Hawaii");
    originateUnit.add("10th Army Air & Missile Defense Command");
    originateUnit.add("10th Mountain Division, Fort Drum, N.Y.");
    originateUnit.add("11th Armored Cavalry Regiment (ACR)");
    originateUnit.add("12th Combat Aviation Brigade");
    originateUnit.add("13th Combat Sustainment Support Battalion, Joint Base Lewis-McChord, Wash.");
    originateUnit.add("13th Sustainment Command (Expeditionary) (Provisional)");
    originateUnit.add("14th Combat Engineer Battalion, Joint Base Lewis-McChord, Wash.");
    originateUnit.add("16th Sustainment Brigade");
    originateUnit.add("16th Combat Aviation Brigade");
    originateUnit.add("17th Combat Sustainment Support Battalion");
    originateUnit.add("17th Field Artillery Brigade");
    originateUnit.add("18th Fires Brigade (Airborne), Fort Bragg, N.C.");
    originateUnit.add("18TH Medical Command (Direct Support)");
    originateUnit.add("18th Military Police Brigade");
    originateUnit.add("19th Battlefield Coordination Detachment");
    originateUnit.add("19th Theater Sustainment Command, Korea");
    originateUnit.add("2nd Brigade Combat Team, 10th Mountain Division");
    originateUnit.add("20th CBRNE Command");
    originateUnit.add("21st Signal Brigade");
    originateUnit.add("21st Theater Sustainment Command");
    originateUnit.add("21st Theater Sustainment Command Special Troops Battalion");
    originateUnit.add("24th Infantry Division, Fort Riley, Kan.");
    originateUnit.add("25th Infantry Division, Schofield Barracks, Hawaii");
    originateUnit.add("27th Infantry Brigade, Syracuse, N.Y.");
    originateUnit.add("28th Infantry Division, Harrisburg, Pa.");
    originateUnit.add("30th Air Defense Artillery Brigade, Fort Sill, Okla.");
    originateUnit.add("30th Medical Brigade");
    originateUnit.add("30th Signal Battalion, Schofield Barracks, Hawaii");
    originateUnit.add("31st Air Defense Artillery Brigade, Fort Sill, Okla.");
    originateUnit.add("35th Air Defense Artillery Brigade, Korea");
    originateUnit.add("35th Infantry Division, Fort Leavenworth, Kan.");
    originateUnit.add("35th Signal Brigade");
    originateUnit.add("38th Infantry Division, Indianapolis, Ind.");
    originateUnit.add("39th Infantry Brigade, Little Rock, Ark.");
    originateUnit.add("39th Signal Battalion");
    originateUnit.add("39th Transportation Battalion");
    originateUnit.add("40th Infantry Division, Los Alamitos, Calif.");
    originateUnit.add("41st Infantry Brigade, Portland, Ore.");
    originateUnit.add("42nd Infantry Division, Troy, N.Y.");
    originateUnit.add("43rd Signal Battalion");
    originateUnit.add("44th Expeditionary Signal Battalion");
    originateUnit.add("45th Infantry Brigade, Edmond, Okla.");
    originateUnit.add("48th Chemical Brigade");
    originateUnit.add("52nd Ordnance Group (EOD)");
    originateUnit.add("52nd Signal Battalion");
    originateUnit.add("56th Signal Battalion");
    originateUnit.add("62nd Medical Brigade");
    originateUnit.add("63rd Regional Readiness Command, Los Alamitos, Calif.");
    originateUnit.add("66th Military Intelligence Brigade");
    originateUnit.add("69th Signal Battalion");
    originateUnit.add("71st Ordnance Group");
    originateUnit.add("72nd Expeditionary Signal Battalion");
    originateUnit.add("81st Infantry Brigade, Seattle, Wash.");
    originateUnit.add("81st Regional Support Command");
    originateUnit.add("82nd Airborne Division, Fort Bragg, N.C.");
    originateUnit.add("88th Regional Readiness Command, Fort Snelling, Minn.");
    originateUnit.add("94th Army Air and Missile Defense Command, Fort Shafter, Hawaii");
    originateUnit.add("99th Regional Support Command");
    originateUnit.add("101st Airborne Division, Fort Campbell, Ky.");
    originateUnit.add("102nd Signal Battalion");
    originateUnit.add("114th Signal Battalion");
    originateUnit.add("116th Cavalry Brigade, Boise, Idaho");
    originateUnit.add("160th Signal Brigade, Camp Arifjan, Kuwait");
    originateUnit.add("173rd Airborne Brigade (SkySoldiers)");
    originateUnit.add("201st Battlefield Surveillance Brigade (BfSB)");
    originateUnit.add("266th Financial Management Support Center");
    originateUnit.add("278th Armored Cavalry Regiment, Knoxville, Tenn.");
    originateUnit.add("302d Signal Battalion");
    originateUnit.add("311th Signal Command (Theater)");
    originateUnit.add("405th Army Field Support Brigade");
    originateUnit.add("409th Contracting Support Br");
    originateUnit.add("428th Field Artillery Brigade, Fort Sill, Okla.");
    originateUnit.add("434th Field Artillery Brigade, Fort Sill, Okla.");
    originateUnit.add("501st Military Intelligence Brigade");
    originateUnit.add("509th Signal Battalion");
    originateUnit.add("513th Military Intelligence Brigade");
    originateUnit.add("555th Engineer Brigade");
    originateUnit.add("593rd Sustainment Command (Expeditionary)");
    originateUnit.add("599th Transportation Group");
    originateUnit.add("6981st Civilian Support Group");
    originateUnit.add("704th Military Intelligence Brigade");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("A. P. Hill, Va.");
    originateUnit.add("Aberdeen Proving Ground (APG), Md.");
    originateUnit.add("Aberdeen Test Center");
    originateUnit.add("Accession Medical Standards Analysis & Research Activity (AMSARA)");
    originateUnit.add("Acronym Finder");
    originateUnit.add("Adjutant General School, Fort Jackson, S.C.");
    originateUnit.add("Administrative Assistant to the Secretary of the Army (AASA)");
    originateUnit.add("AFRICOM (U.S. Africa Command)");
    originateUnit.add("Air Force");
    originateUnit.add("Air Traffic Services Command (ATSCOM)");
    originateUnit.add("Airfields");
    originateUnit.add("Alabama National Guard");
    originateUnit.add("Alaska National Guard");
    originateUnit.add("AMEDD Virtual Library");
    originateUnit.add("American Forces Network-Europe");
    originateUnit.add("American Forces Network-Korea");
    originateUnit.add("Andrew Rader Health Clinic, Fort Myer, Va.");
    originateUnit.add("Anniston Army Depot, Ala.");
    originateUnit.add("Anthrax Vaccine Immunization Program");
    originateUnit.add("Arizona National Guard");
    originateUnit.add("Arkansas National Guard");
    originateUnit.add("Arlington National Cemetery");
    originateUnit.add("Armament Research, Development and Engineering Center (ARDEC)");
    originateUnit.add("Armed Forces Entertainment");
    originateUnit.add("Armed Forces Institute of Regenerative Medicine (AFIRM)");
    originateUnit.add("Armed Forces Medical Examiner System (AFMES)");
    originateUnit.add("Armed Forces Research Institute of Medical Sciences (AFRIMS)");
    originateUnit.add("Armed Forces Retirement Home");
    originateUnit.add("Armed Forces Sports");
    originateUnit.add("Armor School, Fort Benning, Ga.");
    originateUnit.add("Army Acquisition Corps");
    originateUnit.add("Army Air Force Exchange Service (AAFES)");
    originateUnit.add("Army Analysis, Modeling and Simulation Competencey Development");
    originateUnit.add("Army Aeronautical Services Agency");
    originateUnit.add("Army Audit Agency");
    originateUnit.add("Army Auditor General");
    originateUnit.add("Army Aviation and Missile Command (AMCOM) - Redstone Arsenal, Ala.");
    originateUnit.add("Army Bands");
    originateUnit.add("Army Benefits - Military");
    originateUnit.add("Army Benefits Center - Civilian");
    originateUnit.add("Army Cadet Command");
    originateUnit.add("Army Career and Alumni Program");
    originateUnit.add("Army Casualty and Memorial Affairs Operations Center");
    originateUnit.add("Army Center for Health Promotion and Preventive Medicine - Europe");
    originateUnit.add("Army Center for Substance Abuse Programs (ACSAP)");
    originateUnit.add("Army Child and Youth Services");
    originateUnit.add("Army Civilian Jobs");
    originateUnit.add("Army Civilian Personnel");
    originateUnit.add("Army Computer Emergency Response Team - Computer Network Operations (ACERT)");
    originateUnit.add("Army Contracting Command");
    originateUnit.add("Army Contracting Command - Warren (ACC-WRN) Procurement Network (ProcNet)");
    originateUnit.add("Army Correspondence Course Program");
    originateUnit.add("Army Credentialing Opportunities Online (COOL)");
    originateUnit.add("Army Cyber Command");
    originateUnit.add("Army Dental Care");
    originateUnit.add("Army Distributed Learning System");
    originateUnit.add("Army Dragster");
    originateUnit.add("Army Echoes");
    originateUnit.add("Army Education");
    originateUnit.add("Army Electronic Product Support (AEPS)");
    originateUnit.add("Army Emergency Relief");
    originateUnit.add("Army Entertainment");
    originateUnit.add("Army Environmental Command");
    originateUnit.add("Army Family Action Plan");
    originateUnit.add("Army Family and Morale, Welfare and Recreation Command");
    originateUnit.add("Army Family Readiness Groups");
    originateUnit.add("Army Gaming");
    originateUnit.add("Army Geospatial Center (AGC)");
    originateUnit.add("Army Historical Foundation");
    originateUnit.add("Army Housing");
    originateUnit.add("Army Human Resources Command");
    originateUnit.add("Army Images");
    originateUnit.add("Army Inspector General School");
    originateUnit.add("Army Joint Support Team (AJST)");
    originateUnit.add("Army Knowledge Online (AKO)");
    originateUnit.add("Army Leadership");
    originateUnit.add("Army Legal Services Agency");
    originateUnit.add("Army Lodging");
    originateUnit.add("Army Management Staff College (AMSC)");
    originateUnit.add("Army Materiel Command (AMC)");
    originateUnit.add("Army Materiel Systems Analysis Activity");
    originateUnit.add("Army Medicine");
    originateUnit.add("Army Modeling & Simulation Directorate");
    originateUnit.add("Army Morale, Welfare and Recreation (MWR)");
    originateUnit.add("Army Mountain Warfare School");
    originateUnit.add("Army National Guard");
    originateUnit.add("Army Network Enterprise Technology Command (NETCOM)");
    originateUnit.add("Army Officer Candidate School");
    originateUnit.add("Army Operations Research Symposium");
    originateUnit.add("Army Pay charts (1949 to present)");
    originateUnit.add("Army Public Affairs");
    originateUnit.add("Army Public Health Command");
    originateUnit.add("Army Racing");
    originateUnit.add("Army Reprogramming Analysis Team (ARAT)");
    originateUnit.add("Army Research Institute for the Behavioral and Social Sciences");
    originateUnit.add("Army Research Laboratory");
    originateUnit.add("Army Research Laboratory Chemical-Biological Material Effects Database (CMBE)");
    originateUnit.add("Army Research Office");
    originateUnit.add("Army Reserve - Recruiting");
    originateUnit.add("Army Retirement Services");
    originateUnit.add("Army Review Boards Agency");
    originateUnit.add("Army Safety (Combat Readiness Center)");
    originateUnit.add("Army Sexual Harassment/Assault Response & Prevention (SHARP)");
    originateUnit.add("Army Song");
    originateUnit.add("Army Sports");
    originateUnit.add("Army Staff");
    originateUnit.add("Army Suggestion Program");
    originateUnit.add("Army Sustainment Command");
    originateUnit.add("Army Symbols and History");
    originateUnit.add("Aviation Systems Project Management Office, Redstone Arsenal");
    originateUnit.add("Army Ten-Miler - Americas Largest Ten Mile Race");
    originateUnit.add("Army Test and Evaluation Command");
    originateUnit.add("Army Training Network");
    originateUnit.add("Army Training Requirements and Resources System (ATRRS)");
    originateUnit.add("Army Training Support Center (ATSC)");
    originateUnit.add("Army Transformation War Game 2001");
    originateUnit.add("Army University Access Online");
    originateUnit.add("Army Values");
    originateUnit.add("Army War College, Pa.");
    originateUnit.add("Army Wounded Warrior Program (AW2)");
    originateUnit.add("ARNews");
    originateUnit.add("Arsenals");
    originateUnit.add("Assistant Chief of Staff for Installation Management (ACSIM)");
    originateUnit.add("Assistant Secretary of the Army for Acquisitions, Logistics, and Technology (ASA(ALT))");
    originateUnit.add("Assistant Secretary of the Army for Civil Works (ASACW)");
    originateUnit.add("Assistant Secretary of the Army for Financial Management and Comptroller (ASA(FM&C))");
    originateUnit.add("Assistant Secretary of the Army for Installations and Environment (ASAIE)");
    originateUnit.add("Assistant Secretary of the Army for Manpower and Reserve Affairs (ASAMRA)");
    originateUnit.add("Association for Death Education and Counseling");
    originateUnit.add("Association of the United States Army (AUSA)");
    originateUnit.add("Asymmetric Warfare Group (AWG)");
    originateUnit.add("Auditor General (SAAG)");
    originateUnit.add("Aviation Center of Excellence, Fort Rucker, Ala.");
    originateUnit.add("Aviation Logistics School, Fort Eustis, Va. (USAALS)");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("Base Realignment and Closure (BRAC)");
    originateUnit.add("Battle Command Battle Laboratory - Fort Gordon, Ga. (BCBL)");
    originateUnit.add("Bayne-Jones Army Community Hospital, Fort Polk, La.");
    originateUnit.add("Benet Labs");
    originateUnit.add("Better Opportunities for Single Soldiers (BOSS)");
    originateUnit.add("Biggs Army Airfield, Texas");
    originateUnit.add("Black Knights, USMA");
    originateUnit.add("Blanchfield Army Community Hospital, Fort Campbell, Ky.");
    originateUnit.add("Blue Grass Army Depot, Ky.");
    originateUnit.add("Bull Riding");
    originateUnit.add("Battle Command Knowledge System");
    originateUnit.add("Brigade Modernization Command");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("Cadet Command");
    originateUnit.add("California National Guard");
    originateUnit.add("Call to Duty");
    originateUnit.add("Camp Bondsteel, Kosovo");
    originateUnit.add("Camp Zama, Japan");
    originateUnit.add("Camps");
    originateUnit.add("Cargo Helicopters Project Management Office");
    originateUnit.add("Carl R. Darnall Army Medical Center, Fort Hood, Texas");
    originateUnit.add("Carlisle Barracks, Pa.");
    originateUnit.add("Casualty and Mortuary Affairs Operations Center");
    originateUnit.add("Center for Army Analysis (CAA)");
    originateUnit.add("Center for Army Leadership (CAL)");
    originateUnit.add("Center for Army Lessons Learned (CALL)");
    originateUnit.add("Center of Military History (CMH)");
    originateUnit.add("Center for Substance Abuse Programs (ACSAP)");
    originateUnit.add("Center for the Army Profession and Ethic (CAPE)");
    originateUnit.add("Centers");
    originateUnit.add("Central Command");
    originateUnit.add("Central Identification Laboratory Hawaii");
    originateUnit.add("Chaplain School, Fort Jackson, S.C.");
    originateUnit.add("Chaplains, Office of the Chief of Chaplains (OCCH)");
    originateUnit.add("Chemical Materials Agency (CMA)");
    originateUnit.add("Chemical School, Fort Leonard Wood, Mo.");
    originateUnit.add("Chief of Staff of the Army (CSA)");
    originateUnit.add("Clinical and Rehabilitative Medicine Research Program (CRMRP)");
    originateUnit.add("Civilian Pay Chart");
    originateUnit.add("Civilian Personnel Online (CPOL)");
    originateUnit.add("Civilians");
    originateUnit.add("Civilian Training and Leader Development");
    originateUnit.add("Coast Guard");
    originateUnit.add("Coastal and Hydraulics Laboratory (CHL)");
    originateUnit.add("Cold Regions Research and Engineering Laboratory (CRREL)");
    originateUnit.add("Cold Regions Test Center (CRTC)");
    originateUnit.add("Collective Training Directorate (CTD)");
    originateUnit.add("Colorado National Guard");
    originateUnit.add("Combat Readiness Center (Safety)");
    originateUnit.add("Combat Related Special Compensation (CRSC)");
    originateUnit.add("Combat Casualty Care Research Program (CCC)");
    originateUnit.add("Combat Trafficking In Persons");
    originateUnit.add("Combat Training Centers");
    originateUnit.add("Combat Training Center Directorate (CTCD)");
    originateUnit.add("Combined Arms Center (CAC) - Fort Leavenworth, Kan.");
    originateUnit.add("Combined Arms Center - Training");
    originateUnit.add("Combined Arms Doctrine Directorate (CADD)");
    originateUnit.add("Combined Arms Support Command, Ft. Lee, Va.");
    originateUnit.add("Combined Joint Task Force-Horn of Africa");
    originateUnit.add("Command and General Staff College (CGSC)");
    originateUnit.add("Command and General Staff College, Directorate of Educational Technology (CGSC-DOET)");
    originateUnit.add("Commissaries");
    originateUnit.add("Communications-Electronics Command (CECOM)");
    originateUnit.add("Communications Electronics Research Development and Engineering Center (CERDEC)");
    originateUnit.add("Communications Security Logistics Activity (CSLA)");
    originateUnit.add("Computer Emergency Response Team - Computer Network Operations");
    originateUnit.add("Computer Hardware, Enterprise Software and Solutions (CHESS)");
    originateUnit.add("Congressionally Directed Medical Research Programs (CDMRP)");
    originateUnit.add("Connecticut National Guard");
    originateUnit.add("Construction Engineering Research Laboratory");
    originateUnit.add("Contracting Command (ACC)");
    originateUnit.add("Corpus Christi Army Depot (CCAD), Texas");
    originateUnit.add("Correspondence Course Program (ACCP)");
    originateUnit.add("Cost and Economic Analysis Center");
    originateUnit.add("Counseling - Military One Source");
    originateUnit.add("Crane Army Ammunition Activity");
    originateUnit.add("Credentialing Opportunities Online (COOL)");
    originateUnit.add("Criminal Investigation Command (CID)");
    originateUnit.add("Chief Information Officer (CIO/G6)");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("Defense Enrollment Eligibility Reporting System (DEERS)");
    originateUnit.add("Defense Agencies");
    originateUnit.add("Defense Ammunition Center");
    originateUnit.add("Defense Centers of Excellence (DCoE) for Psychological Health and Traumatic Brain Injury");
    originateUnit.add("Defense Finance and Accounting Service (DFAS)");
    originateUnit.add("Defense Human Resource Activity (DHRA)");
    originateUnit.add("Defense Information School");
    originateUnit.add("Defense Information Systems Agency (DISA)");
    originateUnit.add("Defense Language Institute English Language Center, Lackland AFB, Texas");
    originateUnit.add("Defense Language Institute Foreign Language Center, Monterey, CADefense Language Institute " +
            "Foreign Language Center, Monterey, Calif.");
    originateUnit.add("Defense Logistics Information Service (DLIS)");
    originateUnit.add("Defense Media Activity (DMA)");
    originateUnit.add("Defense Medical Research and Development Program (DMRDP)");
    originateUnit.add("Defense Prisoner of War/Missing Personnel Office (DPMO)");
    originateUnit.add("Defense Threat Reduction Agency");
    originateUnit.add("Defense Video & Imagery Distribution System (DVIDS)");
    originateUnit.add("Defense and Veterans Brain Injury Center (DVBIC)");
    originateUnit.add("Delaware National Guard");
    originateUnit.add("Dental Care");
    originateUnit.add("Department of Defense");
    originateUnit.add("Department of Defense Education Activity");
    originateUnit.add("Deployment Well-Being");
    originateUnit.add("Deployment (FHP&R)");
    originateUnit.add("Deployment Health Clinical Center (DHCC)");
    originateUnit.add("Depots");
    originateUnit.add("Deseret Chemical Depot, Utah");
    originateUnit.add("Devens Reserve Forces Training Area, Devens, Mass.");
    originateUnit.add("DeWitt Army Community Hospital, Fort Belvoir, Va.");
    originateUnit.add("DiLorenzo TRICARE Health Clinic, Washington, D.C.");
    originateUnit.add("Directorate of Environmental Integration");
    originateUnit.add("Directorate of Logistics-Washington");
    originateUnit.add("Directorate of Public Works");
    originateUnit.add("Directorate of Training, Doctrine, and Simulation (DOTDS)");
    originateUnit.add("District of Columbia National Guard");
    originateUnit.add("DoD Blast Injury Research Program");
    originateUnit.add("Dugway Proving Ground, Utah");
    originateUnit.add("Dwight D. Eisenhower Army Medical Center, Fort Gordon, Ga.");
    originateUnit.add("Dragon Hill Lodge, Yongsan");
    originateUnit.add("Digital Training Management System (DTMS)");
    originateUnit.add("Deployment Health Assessment Program (DHAP)");
    originateUnit.add("Drill Sergeant School");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("Echoes");
    originateUnit.add("eCYBERMISSION");
    originateUnit.add("Education");
    originateUnit.add("EEO Agency, Washington, D.C.");
    originateUnit.add("EEO Compliance and Complaints Review, Va.");
    originateUnit.add("Eighth U.S. Army, Korea");
    originateUnit.add("Electronic Product Support (AEPS) - AMC Logistics Web Portal");
    originateUnit.add("Electronic Proving Ground, Ariz.");
    originateUnit.add("Emergency Relief");
    originateUnit.add("Engineer Research and Development Center");
    originateUnit.add("Engineer School, Fort Leonard Wood, Mo.");
    originateUnit.add("Enlisted");
    originateUnit.add("Enlisted Management (HRC, formerly PERSCOM)");
    originateUnit.add("Enlisted Records and Evaluation Center");
    originateUnit.add("Enlisted Selections and Promotions");
    originateUnit.add("Environmental Command (USAEC)");
    originateUnit.add("Environmental Laboratory (EL)");
    originateUnit.add("Equal Employment Opportunity & Civil Rights Office");
    originateUnit.add("Equal Opportunity Staff Advisor Course");
    originateUnit.add("European Command");
    originateUnit.add("Evans Army Hospital, Fort Carson, Colo.");
    originateUnit.add("Europe Regional Dental Command");
    originateUnit.add("Europe Regional Medical Command");
    originateUnit.add("Eighth U.S. Army Noncommissioned Officer Academy");
    originateUnit.add("U.S. Army Electronic Proving Ground");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("Families");
    originateUnit.add("Families (National Guard)");
    originateUnit.add("Families (Reserve)");
    originateUnit.add("Field Artillery School, Fort Sill, Okla.");
    originateUnit.add("Fifth U.S. Army");
    originateUnit.add("Finance Command, Va.");
    originateUnit.add("Finance School, Fort Jackson, S.C.");
    originateUnit.add("First U.S. Army");
    originateUnit.add("Fiscal Law Website, JAG");
    originateUnit.add("Florida National Guard");
    originateUnit.add("Forms, Official Department of the Army");
    originateUnit.add("FORSCOM (U.S. Army Forces Command)");
    originateUnit.add("Fort A. P. Hill, Va.");
    originateUnit.add("Fort Belvoir, Va.");
    originateUnit.add("Fort Benning, Ga.");
    originateUnit.add("Fort Bliss, Texas");
    originateUnit.add("Fort Bragg, N.C.");
    originateUnit.add("Fort Buchanan, Puerto Rico");
    originateUnit.add("Fort Campbell, Ky.");
    originateUnit.add("Fort Carson, Colo.");
    originateUnit.add("Fort Detrick, Md.");
    originateUnit.add("Fort Dix, N.J.");
    originateUnit.add("Fort Drum, N.Y.");
    originateUnit.add("Fort Eustis, Va.");
    originateUnit.add("Fort Gordon, Ga.");
    originateUnit.add("Fort Greely, Alaska");
    originateUnit.add("Fort Hamilton, N.Y.");
    originateUnit.add("Fort Hood, Texas");
    originateUnit.add("Fort Huachuca, Ariz.");
    originateUnit.add("Fort Hunter Liggett, Calif.");
    originateUnit.add("Fort Irwin, Calif.");
    originateUnit.add("Fort Jackson, S.C.");
    originateUnit.add("Fort Knox, Ky.");
    originateUnit.add("Fort Leavenworth, Kan.");
    originateUnit.add("Fort Lee, Va.");
    originateUnit.add("Fort Leonard Wood, Mo.");
    originateUnit.add("Fort McClellan, Ala.");
    originateUnit.add("Fort McCoy, Wis.");
    originateUnit.add("Fort McNair, Washington, D.C.");
    originateUnit.add("Fort McPherson, Ga.");
    originateUnit.add("Fort Meade, Md.");
    originateUnit.add("Fort Polk, La.");
    originateUnit.add("Fort Richardson, Alaska");
    originateUnit.add("Fort Riley, Kan.");
    originateUnit.add("Fort Rucker, Ala.");
    originateUnit.add("Fort Sam Houston, Texas");
    originateUnit.add("Fort Shafter, Hawaii");
    originateUnit.add("Fort Sill, Okla.");
    originateUnit.add("Fort Stewart, Ga.");
    originateUnit.add("Fort Story, Va.");
    originateUnit.add("Fort Wainwright, Alaska");
    originateUnit.add("Forts");
    originateUnit.add("Fox Army Health Center, Redstone Arsenal, Ala.");
    originateUnit.add("Freedom of Information Act (FOIA)");
    originateUnit.add("Freedom of Information Act Electronic Reading Room");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("G-1 - Office of the Deputy Chief of Staff for Personnel (ODCSPER)");
    originateUnit.add("G-2 - Office of the Deputy Chief of Staff for Intelligence (ODCSINT)");
    originateUnit.add("G-3/5/7");
    originateUnit.add("G-4 - Office of the Deputy Chief of Staff for Logistics (ODCSLOG)");
    originateUnit.add("G-6 - Office of the Chief Information Officer/G-6 (CIO/SAIS)");
    originateUnit.add("G-8 - Office of the Deputy Chief of Staff for Programs");
    originateUnit.add("General Counsel (OGC)");
    originateUnit.add("General Leonard Wood Army Community Hospital, Fort Leonard Wood, Mo.");
    originateUnit.add("General Officer Management Office (GOMO)");
    originateUnit.add("Georgia National Guard");
    originateUnit.add("Geotechnical and Structures Laboratory");
    originateUnit.add("GI Bill");
    originateUnit.add("Golden Knights");
    originateUnit.add("Guam National Guard");
    originateUnit.add("Government Jobs");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("Hawaii National Guard");
    originateUnit.add("Headquarters, Department of the Army");
    originateUnit.add("Health Promotion and Preventive Medicine");
    originateUnit.add("Heraldry (TIOH)");
    originateUnit.add("History (CMH)");
    originateUnit.add("Hometown News Service");
    originateUnit.add("Housing");
    originateUnit.add("Human Resources Command (HRC)");
    originateUnit.add("Human Resource Service Center");
    originateUnit.add("Hunter Army Airfield, Ga.");
    originateUnit.add("HHSC, 24th Military Intelligence");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("I Corps, Joint Base Lewis-McChord, Wash.");
    originateUnit.add("Idaho National Guard");
    originateUnit.add("III Corps, Fort Hood, Texas");
    originateUnit.add("Illinois National Guard");
    originateUnit.add("Images");
    originateUnit.add("Indiana National Guard");
    originateUnit.add("Infantry Center, Fort Benning, Ga.");
    originateUnit.add("Information Technology Laboratory (ITL)");
    originateUnit.add("Inspector General (OTIG)");
    originateUnit.add("Installation Management Command (IMCOM)");
    originateUnit.add("Installation Management Command - Europe");
    originateUnit.add("Installation Management Command - Pacific Region");
    originateUnit.add("Institute of Heraldry (TIOH)");
    originateUnit.add("Institute for National Strategic Studies");
    originateUnit.add("Institute of Surgical Research - U.S. Army, Fort Sam Houston, Texas");
    originateUnit.add("Integrated Personnel and Pay System-Army (IPPS-A)");
    originateUnit.add("Intelligence and Security Command (INSCOM)");
    originateUnit.add("Intelligence School, Fort Huachuca, Ariz.");
    originateUnit.add("Intelligence, Office of the Deputy Chief of Staff for Intelligence (ODCSINT) - G-2");
    originateUnit.add("International Affairs");
    originateUnit.add("Iowa National Guard");
    originateUnit.add("Ireland Army Community Hospital, Fort Knox, Ky.");
    originateUnit.add("Irwin Army Community Hospital,Fort Riley, Kan.");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("Joint Base Lewis-McChord, Wash.");
    originateUnit.add("Joint Base Myer-Henderson Hall");
    originateUnit.add("Joint Chemical Biological Defense Research Program (JCBDRP), Md.");
    originateUnit.add("Joint Chiefs of Staff");
    originateUnit.add("Joint Forces Command");
    originateUnit.add("Joint Interagency Training and Education (JITEC)");
    originateUnit.add("Joint Multinational Readiness Center, Germany");
    originateUnit.add("Joint Multinational Training Command (JMTC)");
    originateUnit.add("Joint Readiness Training Center (JRTC), La.");
    originateUnit.add("Joint Systems Manufacturing Center - Lima Army Tank Plant");
    originateUnit.add("Joint Personal Property Shipping Office, Joint Base Lewis-McChord");
    originateUnit.add("Joint Prisoners of War, Missing in Action Accounting Command (JPAC)");
    originateUnit.add("Judge Advocate General, Office of The Judge Advocate General (OTJAG)");
    originateUnit.add("Joint Multinational Simulation Center");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("Kansas National Guard");
    originateUnit.add("Keller Army Community Hospital, West Point, N.Y.");
    originateUnit.add("Kenner Army Health Clinic, Fort Lee, Va.");
    originateUnit.add("Kentucky National Guard");
    originateUnit.add("Kimbrough Ambulatory Care Center, Fort Meade, Md.");
    originateUnit.add("Korean Service Corps Battalion");
    originateUnit.add("Korean War");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("Laboratories");
    originateUnit.add("Landstuhl Regional Medical Center, Germany");
    originateUnit.add("Lawson Army Airfield, Ga.");
    originateUnit.add("Lead AMC Integration Support Office (LAISO)");
    originateUnit.add("Legal Services Agency");
    originateUnit.add("Legal Services, Judge Advocate General");
    originateUnit.add("Legislative Liaison, Office of the Chief of Legislative Liaison (OCLL)");
    originateUnit.add("Lessons Learned (CALL)");
    originateUnit.add("Lessons Learned Office - USAREUR");
    originateUnit.add("Letterkenny Army Depot, Pa.");
    originateUnit.add("Libraries");
    originateUnit.add("Library Program (ALP)");
    originateUnit.add("Lodging");
    originateUnit.add("Logistics Civil Augmentation Program (LOGCAP)");
    originateUnit.add("Logistics Management College, Fort Lee, Va.");
    originateUnit.add("Logistics Modernization Program");
    originateUnit.add("Logistics Support Activity (LOGSA)");
    originateUnit.add("Logistics Transformation Agency, Va.");
    originateUnit.add("Louisiana National Guard");
    originateUnit.add("Logistics Innovation Agency, VA");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("Madigan Army Medical Center, Tacoma, Wash.");
    originateUnit.add("Maine National Guard");
    originateUnit.add("Major Commands");
    originateUnit.add("Maneuver Support Battle Lab (MSBL)");
    originateUnit.add("Maneuver Support Center of Excellence");
    originateUnit.add("Marine Artillery Detachment, Fort Sill, Okla.");
    originateUnit.add("Marine Corps");
    originateUnit.add("Martin Army Community Hospital, Fort Benning, Ga.");
    originateUnit.add("Maryland National Guard");
    originateUnit.add("Massachusetts National Guard");
    originateUnit.add("McAlester Army Ammunition Plant");
    originateUnit.add("McDonald Army Community Hospital, Fort Eustis, Va.");
    originateUnit.add("Media Releases");
    originateUnit.add("Medicine, Army (formerly Medical Command or MEDCOM)");
    originateUnit.add("Medical Command (MEDCOM) Office of Equal Employment Opportunity Programs");
    originateUnit.add("Medical Communications for Combat Casualty Care (MC4)");
    originateUnit.add("Medical Department-Alaska (MEDDAC-Alaska)");
    originateUnit.add("Medical Department - Fort Drum");
    originateUnit.add("Medical Regional Commands");
    originateUnit.add("Michigan National Guard");
    originateUnit.add("Mihail Kogalniceanu Air Base Passenger Transit Center");
    originateUnit.add("Military Academy, USMA, West Point, N.Y.");
    originateUnit.add("Military Child Education Coalition (MCEC)");
    originateUnit.add("Military District of Washington (MDW), Washington, D.C.");
    originateUnit.add("Military Funerals");
    originateUnit.add("Military Health Care (TRICARE)");
    originateUnit.add("Military Health System Research Symposium (MHSRS)");
    originateUnit.add("Military Homefront");
    originateUnit.add("Military Impacted School Association (MISA)");
    originateUnit.add("Military Infectious Diseases Research Program (MIDRP)");
    originateUnit.add("Military One Source - Army");
    originateUnit.add("Military Operational Medicine Research Program (MOMRP)");
    originateUnit.add("Military Personnel (MILPER) Messages");
    originateUnit.add("Military Police School, Fort Leonard Wood, Mo.");
    originateUnit.add("Military Postal Service Agency");
    originateUnit.add("Military Surface Deployment and Distribution Command (SDDC)");
    originateUnit.add("Minnesota National Guard");
    originateUnit.add("Mission and Installation Contracting Command");
    originateUnit.add("Mississippi National Guard");
    originateUnit.add("Missouri National Guard");
    originateUnit.add("Moncrief Army Community Hospital, Fort Jackson, S.C.");
    originateUnit.add("Montana National Guard");
    originateUnit.add("Morale, Welfare, and Recreation (MWR or FMWRC)");
    originateUnit.add("Mountain Warfare School, Jericho, Vt.");
    originateUnit.add("Multi-User ECP Automated Review System (MEARS)");
    originateUnit.add("Munson Army Health Center, Fort Leavenworth, Kan.");
    originateUnit.add("MyPay - DFAS");
    originateUnit.add("Mission Command Training Program (MCTP)");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("National Guard Bureau (NGB)");
    originateUnit.add("National Guard Bureau Counterdrug");
    originateUnit.add("National Guard Bureau International Affairs");
    originateUnit.add("National Military Family Association");
    originateUnit.add("National Museum of Health and Medicine (NMHM)");
    originateUnit.add("National Guard Family Support");
    originateUnit.add("National Guard Office of the Chaplain");
    originateUnit.add("National Interagency Confederation for Biological Research (NICBR)");
    originateUnit.add("National Simulation Center (NSC)");
    originateUnit.add("National Training Center (NTC), Calif.");
    originateUnit.add("NATO, U.S. Army");
    originateUnit.add("Navy");
    originateUnit.add("NCO Academies");
    originateUnit.add("NCO Academy Hawaii");
    originateUnit.add("NCO Journal");
    originateUnit.add("Nebraska National Guard");
    originateUnit.add("Network Enterprise Technology Command");
    originateUnit.add("Nevada National Guard");
    originateUnit.add("New Hampshire National Guard");
    originateUnit.add("New Jersey National Guard");
    originateUnit.add("New Mexico National Guard");
    originateUnit.add("New York National Guard");
    originateUnit.add("No Fear Act (Equal Employment Opportunity)");
    originateUnit.add("North Atlantic Medical Command");
    originateUnit.add("North Carolina National Guard");
    originateUnit.add("North Dakota National Guard");
    originateUnit.add("Northern Warfare Training Center (NWTC)");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("Office of the Army G-1");
    originateUnit.add("Office of the Army G-3");
    originateUnit.add("Office of the Army G-4");
    originateUnit.add("Office of the Army General Counsel (OGC)");
    originateUnit.add("Office of the Assistant Secretary of the Army for Acquisitions, Logistics, and Technology (ASA" +
            "(ALT))");
    originateUnit.add("Office of the Assistant Secretary of the Army for Civil Works (ASACW)");
    originateUnit.add("Office of the Assistant Secretary of the Army for Financial Management and Comptroller (ASA" +
            "(FM&C))");
    originateUnit.add("Office of the Assistant Secretary of the Army for Installations and Environment (ASAIE)");
    originateUnit.add("Office of the Assistant Secretary of the Army for Manpower and Reserve Affairs (ASAMRA)");
    originateUnit.add("Office of the Auditor General (SAAG)");
    originateUnit.add("Office of the Chief of Chaplains (OCCH)");
    originateUnit.add("Office of the Chief of Engineers (OCE)");
    originateUnit.add("Office of the Chief of Public Affairs (OCPA)");
    originateUnit.add("Office of the Chief of Staff of the Army (CSA)");
    originateUnit.add("Office of the Deputy Chief of Staff for Intelligence (ODCSINT) - G-2");
    originateUnit.add("Office of the Deputy Chief of Staff for Logistics (ODCSLOG) - G-4");
    originateUnit.add("Office of the Deputy Chief of Staff for Operations & Plans (ODCSOPS) - G-3/5/7");
    originateUnit.add("Office of the Deputy Chief of Staff for Personnel (ODCSPER) - G-1");
    originateUnit.add("Office of Economic Adjustment (OEA)");
    originateUnit.add("Office of the Inspector General (OTIG)");
    originateUnit.add("Office of The Judge Advocate General (OTJAG)");
    originateUnit.add("Office of the Program Manager - Saudi Arabian National Guard (OPM-SANG)");
    originateUnit.add("Office of the Provost Marshal General (OPMG)");
    originateUnit.add("Office of the Secretary of the Army (SECARMY)");
    originateUnit.add("Office of the Surgeon General (DASG)");
    originateUnit.add("Office of the Under Secretary of the Army (U/SECARMY)");
    originateUnit.add("Office of the Vice Chief of Staff of the Army (VCSA)");
    originateUnit.add("Office of Small and Disadvantaged Business Utilization (OSADBU)");
    originateUnit.add("Official Army Publications and Forms");
    originateUnit.add("Officer Candidate School");
    originateUnit.add("Officer Personnel Managers");
    originateUnit.add("Officer Selections and Promotions");
    originateUnit.add("Officers");
    originateUnit.add("Ohio National Guard");
    originateUnit.add("Oklahoma National Guard");
    originateUnit.add("Olympics - Soldier athletes");
    originateUnit.add("One Source, Army");
    originateUnit.add("Operational Support Airlift Agency (OSAA), Fort Belvoir, Va.");
    originateUnit.add("Ordnance School, Fort Lee, Va.");
    originateUnit.add("Oregon National Guard");
    originateUnit.add("Other Facilities");
    originateUnit.add("Outdoor Recreation");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("");
    originateUnit.add("Pacific Medical Command");
    originateUnit.add("Parks Reserve Forces Training Area, Dublin, Calif.");
    originateUnit.add("Peacekeeping & Stability Operations Institute");
    originateUnit.add("PEO Aviation");
    originateUnit.add("Pennsylvania National Guard");
    originateUnit.add("PERSCOM (now Human Resources Command)");
    originateUnit.add("Personnel (MILPER) Messages");
    originateUnit.add("Picatinny Arsenal, N.J.");
    originateUnit.add("Pine Bluff Arsenal, Ark.");
    originateUnit.add("Pittsburg State University ROTC Gorilla Battalion");
    originateUnit.add("Post-Deployment Health Reassessment");
    originateUnit.add("Posture Statement");
    originateUnit.add("Privacy Act");
    originateUnit.add("Product Manager Joint-Automatic Identification Technology (PM J-AIT)");
    originateUnit.add("Product Manager - Sets, Kits, Outfits & Tools");
    originateUnit.add("Product Manager Test, Measurement, and Diagnostic Equipment");
    originateUnit.add("Program Executive Office for Ammunition (PEO Ammo), Picatinny Arsenal");
    originateUnit.add("Program Executive Office for Combat Support and Combat Service Support (CS&CSS)");
    originateUnit.add("Program Executive Office for Enterprise Information Systems (PEO EIS)");
    originateUnit.add("Program Executive Office for Enterprise Information Systems: HR Solutions");
    originateUnit.add("Program Executive Office for Simulation, Training and Instrumentation");
    originateUnit.add("Program Executive Office Soldier");
    originateUnit.add("Product Director Test, Measurement, and Diagnostic Equipment");
    originateUnit.add("Post Traumatic Stress Disorder and Mild Traumatic Brain Injury (PTSD/MTBI) Chain Teaching " +
            "Program");
    originateUnit.add("Program Executive Office for Command, Control and Communications-Tactical (PEO 3CT)");
    originateUnit.add("Program Executive Office for Ground Combat Systems (PEO GCS)");
    originateUnit.add("Proving Grounds");
    originateUnit.add("Public Affairs, Office of the Chief of Public Affairs (OCPA)");
    originateUnit.add("Public Health Command Region-Europe");
    originateUnit.add("Publications, Administrative");
    originateUnit.add("Pueblo Chemical Depot, Colo.");
    originateUnit.add("PX - Army Air Force Exchange Service (AAFES)");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("Quartermaster School, Fort Lee, Va.");
    originateUnit.add("Quartermaster Museum, Fort Lee, Va.");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("R&R Leave Information");
    originateUnit.add("Radio News, Soldiers");
    originateUnit.add("Rank");
    originateUnit.add("Rapid Equipping Force");
    originateUnit.add("Raymond W. Bliss Army Health Clinic, Fort Huachuca, Ariz.");
    originateUnit.add("RDECOM");
    originateUnit.add("Records Information Management");
    originateUnit.add("Records Management and Declassification Agency (RMDA)");
    originateUnit.add("Recruiting");
    originateUnit.add("Recruiting and Retention School, Fort Jackson, S.C.");
    originateUnit.add("Red River Army Depot (RRAD), Texas");
    originateUnit.add("Redstone Arsenal, Ala.");
    originateUnit.add("Redstone Scientific Information Center (RISC)");
    originateUnit.add("Redstone Test Center (RTC)");
    originateUnit.add("Regional Health Command - Atlantic");
    originateUnit.add("Regional Health Command - Central");
    originateUnit.add("Regional Health Command - Pacific");
    originateUnit.add("Regulations, Army Wide");
    originateUnit.add("Rehabilitative Medicine Research Program, Md.");
    originateUnit.add("Reimer Digital Library");
    originateUnit.add("Reserve, Office of the Chief of the Army Reserve (OCAR)");
    originateUnit.add("Retirees");
    originateUnit.add("Retirement Services");
    originateUnit.add("Reynolds Army Community Hospital, Fort Sill, Okla.");
    originateUnit.add("Rhode Island National Guard");
    originateUnit.add("Rock Island Arsenal (RIA), Ill.");
    originateUnit.add("Rock Island Arsenal Joint Manufacturing and Technology Center (JMTC)");
    originateUnit.add("Rodriguez Army Health Clinic, Fort Buchanan, Puerto Rico");
    originateUnit.add("ROTC");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("Safety (Combat Readiness Center)");
    originateUnit.add("San Antonio Military Medical Center");
    originateUnit.add("San Diego Chapter Buffalo Soldiers");
    originateUnit.add("SBBCOM Integrated Materiel Management Center (IMMC)");
    originateUnit.add("Schofield Barracks, Hawaii");
    originateUnit.add("Secretary of the Army");
    originateUnit.add("Sergeant Major of the Army");
    originateUnit.add("Sergeant Major Academy");
    originateUnit.add("Senior Leader Development Office (SLD)");
    originateUnit.add("Sexual Assault Prevention and Response Program");
    originateUnit.add("Sierra Army Depot, Calif.");
    originateUnit.add("Signal Center of Excellence");
    originateUnit.add("Signal Newspaper, Fort Gordon, Ga.");
    originateUnit.add("Signal School, Fort Gordon, Ga.");
    originateUnit.add("Simmons Army Airfield, N.C.");
    originateUnit.add("Small and Disadvantaged Business Utilization (OSADBU)");
    originateUnit.add("Smallpox Vaccination Program (SVP)");
    originateUnit.add("Social Media");
    originateUnit.add("Software Engineering Center-Lee, Fort Lee, Va.");
    originateUnit.add("Software Engineering Center - Ft. Monmouth, N.J.");
    originateUnit.add("Soldier's Creed");
    originateUnit.add("Soldiers Magazine");
    originateUnit.add("Soldier Support Institute, Fort Jackson, S.C.");
    originateUnit.add("Soldier Systems Center, Natick, Mass.");
    originateUnit.add("South Carolina National Guard");
    originateUnit.add("South Dakota National Guard");
    originateUnit.add("Southern European Task Force (Airborne) (SETAF)");
    originateUnit.add("STAND-TO!");
    originateUnit.add("Strategic Studies Institute - Army War College");
    originateUnit.add("Stuttgart, U.S. Army Garrison");
    originateUnit.add("Suicide Prevention - Army G-1 Human Resources");
    originateUnit.add("Surgeon General (DASG)");
    originateUnit.add("Survivor Benefit Plan");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("Tank-automotive & Armaments Command (TACOM) Life Cycle Management Command (LCMC)");
    originateUnit.add("Tank-automotive & Armaments Command (TACOM) Life Cycle Management Command (LCMC) Integrated " +
            "Logistics Support Center");
    originateUnit.add("Tank Automotive Research, Development and Engineering Center (TARDEC)");
    originateUnit.add("Task Force Muleskinner");
    originateUnit.add("TCM Virtual & Gaming");
    originateUnit.add("Tennessee National Guard");
    originateUnit.add("Test and Evaluation Command (ATEC)");
    originateUnit.add("Texas National Guard");
    originateUnit.add("Third U.S. Army");
    originateUnit.add("Thrift Savings Plan");
    originateUnit.add("To Our Soldiers");
    originateUnit.add("Tobyhanna Army Depot, Pa.");
    originateUnit.add("Tooele Army Depot, Utah");
    originateUnit.add("Topographic Engineering Center");
    originateUnit.add("TRADOC Analysis Center (TRAC)");
    originateUnit.add("TRADOC Architecture Integration and Management Directorate");
    originateUnit.add("TRADOC Schools");
    originateUnit.add("Training and Doctrine Command (TRADOC)");
    originateUnit.add("Training - Help Desk");
    originateUnit.add("Transportation Command");
    originateUnit.add("Transportation School, Fort Eustis, Va.");
    originateUnit.add("TRICARE");
    originateUnit.add("Tricare Management Activity (TMA)");
    originateUnit.add("Tripler Army Medical Center, Honolulu, Hawaii");
    originateUnit.add("Twilight Tattoo");
    originateUnit.add("Traumatic Servicemembers' Group Life Insurance (TSGLI)");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("Under Secretary of the Army");
    originateUnit.add("Unified Commands");
    originateUnit.add("USAMRMC Chemical Biological Defense Partnership Support Directorate (PSD)");
    originateUnit.add("USAMRMC Office of Research and Technology Applications (USAMRMC ORTA)");
    originateUnit.add("USAMRMC Program Management Office, Integrated Clinical Systems (PMO ICS)");
    originateUnit.add("USAMRMC Telemedicine & Advanced Technology Research Center (TATRC)");
    originateUnit.add("U.S. Africa Command (AFRICOM)");
    originateUnit.add("U.S. Army Aberdeen Test Center (ATC)");
    originateUnit.add("U.S. Army Acquisition Support Center (USAASC)");
    originateUnit.add("U.S. Army Aeromedical Center");
    originateUnit.add("U.S. Army Aeromedical Research Laboratory (USAARL)");
    originateUnit.add("U.S. Army Aeronautical Services Agency");
    originateUnit.add("U.S. Army Africa (USARAF)");
    originateUnit.add("U.S. Army Alaska");
    originateUnit.add("U.S. Army Aviation and Missile Command (AMCOM) - Redstone Arsenal, Ala.");
    originateUnit.add("U.S. Army Cadet Command");
    originateUnit.add("U.S. Army Center for Environmental Health Research (USACEHR)");
    originateUnit.add("U.S. Army Central (USARCENT)");
    originateUnit.add("U.S. Army Combined Arms Support Command (CASCOM)");
    originateUnit.add("U.S. Army Corps of Engineers (USACE)");
    originateUnit.add("U.S. Army Criminal Investigation Command (USACIDC)");
    originateUnit.add("U.S. Army Cyber Center of Excellence");
    originateUnit.add("U.S. Army Cyber Command");
    originateUnit.add("U.S. Army Developmental Test Command (DTC)");
    originateUnit.add("U.S. Disciplinary Barracks, Fort Leavenworth, Kan.");
    originateUnit.add("U.S. Army Europe (USAREUR), Germany");
    originateUnit.add("U.S. Army Europe NATO Brigade");
    originateUnit.add("U.S. Army Europe Regional Veterinary Command");
    originateUnit.add("U.S. Army Environmental Command");
    originateUnit.add("U.S. Army Environmental Policy Institute");
    originateUnit.add("U.S. Army Force Management Support Agency (USAFMSC)");
    originateUnit.add("U.S. Army Forces Command (FORSCOM)");
    originateUnit.add("U.S. Army Garrison - Ansbach");
    originateUnit.add("U.S. Army Garrison - Bavaria");
    originateUnit.add("U.S. Army Garrison - Benelux");
    originateUnit.add("U.S. Army Garrison - Camp Parks");
    originateUnit.add("U.S. Army Garrison - Daegu");
    originateUnit.add("U.S. Army Garrison - Detroit Arsenal, Warren, Mich.");
    originateUnit.add("U.S. Army Garrison - Garmisch");
    originateUnit.add("U.S. Army Garrison - Hawaii");
    originateUnit.add("U.S. Army Garrison - Humphereys");
    originateUnit.add("U.S. Army Garrison - Japan");
    originateUnit.add("U.S. Army Garrison Kwajalein-Atoll");
    originateUnit.add("U.S. Army Garrison - Livorno");
    originateUnit.add("U.S. Army Garrison - Okinawa");
    originateUnit.add("U.S. Army Garrison - Presidio of Monterey");
    originateUnit.add("U.S. Army Garrison - Red Cloud Camp Casey");
    originateUnit.add("U.S. Army Garrison Rheinland-Pfalz");
    originateUnit.add("U.S. Army Garrison - Stuttgart");
    originateUnit.add("U.S. Army Garrison - Yongsan");
    originateUnit.add("U.S. Army Headquarters Services (AHS)");
    originateUnit.add("U.S. Army Information Technology Agency");
    originateUnit.add("U.S. Army Installation Management Command");
    originateUnit.add("U.S. Army Institute of Surgical Research (USAISR)");
    originateUnit.add("U.S. Army Intelligence and Security Command (INSCOM)");
    originateUnit.add("U.S. Army Japan");
    originateUnit.add("U.S. Army Judge Advocate General's Legal Center and School (LCS)");
    originateUnit.add("U.S. Army Legal Services Agency");
    originateUnit.add("U.S. Army Materiel Command (AMC)");
    originateUnit.add("U.S. Army Materiel Systems Analysis Activity");
    originateUnit.add("U.S. Army Medical Command (MEDCOM)");
    originateUnit.add("U.S. Army Medical Department Center & School Portal");
    originateUnit.add("U.S. Army Medical Materiel Agency (USAMMA)");
    originateUnit.add("U.S. Army Medical Materiel Center Europe");
    originateUnit.add("U.S. Army Medical Materiel Development Activity (USAMMDA)");
    originateUnit.add("U.S. Army Medical Research Acquisition Activity (USAMRAA)");
    originateUnit.add("U.S. Army Medical Research Detachment (USAMRD)");
    originateUnit.add("U.S. Army Medical Research Institute of Chemical Defense (USAMRICD)");
    originateUnit.add("U.S. Army Medical Research Unit - Europe (USAMRU-E)");
    originateUnit.add("U.S. Army Medical Research Unit - Kenya (USAMRU-K)");
    originateUnit.add("U.S. Army Medical Research Institute of Infectious Diseases (USAMRIID)");
    originateUnit.add("U.S. Army Medical Research and Materiel Command (USAMRMC)");
    originateUnit.add("U.S. Army Military District of Washington (MDW)");
    originateUnit.add("U.S. Army Museum System");
    originateUnit.add("U.S. Army, NATO");
    originateUnit.add("U.S. Army Network Enterprise Technology Command (NETCOM)");
    originateUnit.add("U.S. Army North (USARNORTH)");
    originateUnit.add("U.S. Army Operational Test Command (OTC)");
    originateUnit.add("U.S. Army Pacific (USARPAC)");
    originateUnit.add("U.S. Army Publishing Directorate (APD)");
    originateUnit.add("U.S. Army Records Management and Declassification Agency (RMDA)");
    originateUnit.add("U.S. Army Recruiting and Retention School");
    originateUnit.add("U.S. Army Research Institute of Environmental Medicine (USARIEM)");
    originateUnit.add("U.S. Army Reserve Command (USARC)");
    originateUnit.add("U.S. Army Security Assistance Command (USASAC)");
    originateUnit.add("U.S. Army South (USARSO/ARSOUTH)");
    originateUnit.add("U.S. Army Space and Missile Defense Command (SMDC)");
    originateUnit.add("U.S. Army Special Operations Command (USASOC)");
    originateUnit.add("U.S. Army Symbols and History");
    originateUnit.add("U.S. Army TACOM LCMC Cost & Systems Analysis");
    originateUnit.add("U.S. Army Test and Evaluation Command (ATEC)");
    originateUnit.add("U.S. Military Academy at West Point, N.Y. (USMA / West Point)");
    originateUnit.add("U.S. Army Human Resource Service Center");
    originateUnit.add("U.S. Army Resources and Programs Agency (RPA)");
    originateUnit.add("U.S. Army Southern Regional Dental Command");
    originateUnit.add("U.S. Army Training and Doctrine Command (TRADOC)");
    originateUnit.add("U.S. Army Women's Museum");
    originateUnit.add("Umatilla Chemical Depot, OR");
    originateUnit.add("United Service Organizations (USO)");
    originateUnit.add("United States Army Medical Materiel Center - Korea (USAMMC-K)");
    originateUnit.add("United States Central Command");
    originateUnit.add("United States European Command (USEUCOM)");
    originateUnit.add("United States Joint Forces Command (USJFCOM)");
    originateUnit.add("United States Northern Command (USNORTHCOM)");
    originateUnit.add("United States Pacific Command (USPACOM)");
    originateUnit.add("United States Southern Command (USSOUTHCOM)");
    originateUnit.add("United States Special Operations Command (USSOCOM)");
    originateUnit.add("United States Strategic Command (USSTRATCOM)");
    originateUnit.add("United States Transportation Command (USTRANSCOM)");
    originateUnit.add("University of Arkansas ROTC");
    originateUnit.add("University of Colorado - Boulder Army ROTC");
    originateUnit.add("University of Colorado - Colorado Springs ROTC");
    originateUnit.add("University of Hawaii, ROTC Warrior Battalion");
    originateUnit.add("University of Illinois at Chicago ROTC");
    originateUnit.add("University of New Mexico Army ROTC");
    originateUnit.add("University of Oregon Army ROTC");
    originateUnit.add("U.S. Office of the Army G-4");
    originateUnit.add("USAMRMC New Products and Idea (NPI)");
    originateUnit.add("USAMRMC Office of Small Business Programs");
    originateUnit.add("USAMRMC Personnel Demonstration Project (PDP)");
    originateUnit.add("USAMRMC Science, Technology, Engineering and Mathematics (STEM) Outreach");
    originateUnit.add("Utah National Guard");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("V Corps");
    originateUnit.add("Vaccines - Military Vaccine (MILVAX) Agency");
    originateUnit.add("Values");
    originateUnit.add("Vermont National Guard");
    originateUnit.add("Veterans");
    originateUnit.add("Veterans Affairs (VA)");
    originateUnit.add("Vice Chief of Staff");
    originateUnit.add("Virginia National Guard");
    originateUnit.add("Voting Assistance Program");
    originateUnit.add("Return to TOP of main content");
    originateUnit.add("Walter Reed Army Institute of Research (WRAIR)");
    originateUnit.add("Walter Reed Army Institute of Research,Human Subjects Protection Branch (HSPB)");
    originateUnit.add("Walter Reed Army Institute of Research Clinical Trials Center (WRAIR-CTC)");
    originateUnit.add("Walter Reed Medical Center, Washington, D.C.");
    originateUnit.add("Walter Reed Biosystematics Unit (WRBU)");
    originateUnit.add("Warrant Officer Career College");
    originateUnit.add("Warrant Officer Recruiting");
    originateUnit.add("Warrant Officer Selections and Promotions");
    originateUnit.add("Warrant Officers");
    originateUnit.add("Washington Headquarters Services (WHS)");
    originateUnit.add("Washington National Guard");
    originateUnit.add("Watervliet Arsenal, N.Y.");
    originateUnit.add("Weed Army Community Hospital, Fort Irwin, Calif.");
    originateUnit.add("Well-Being");
    originateUnit.add("West Point, USMA");
    originateUnit.add("Western Hemisphere Institute for Security Cooperation (WHINSEC)");
    originateUnit.add("Western Regional Medical Command (WRMC)");
    originateUnit.add("West Virginia National Guard");
    originateUnit.add("Wheeler Army Airfield, HI");
    originateUnit.add("White Sands Missile Range (WSMR)");
    originateUnit.add("William Beaumont Army Medical Center, Fort Bliss, Texas");
    originateUnit.add("Winn Army Community Hospital, Fort Stewart, Ga.");
    originateUnit.add("Wisconsin National Guard");
    originateUnit.add("Womack Army Medical Center, Fort Bragg, N.C.");
    originateUnit.add("World Class Athlete Program (WCAP)");
    originateUnit.add("Wyoming National Guard");
    originateUnit.add("White House Communications Agency (WHCA)");

    //event types
    eventTypes.add("Terrorist Activity");
    eventTypes.add("Traitor");
    eventTypes.add("Extremism:Race");
    eventTypes.add("Extremism:Religion");
    eventTypes.add("Extremism:Sexism");
    eventTypes.add("InsiderThreat");
    eventTypes.add("Criminal");

    //report links
    reportLinks.add("www.itdoesntexist.com");
    reportLinks.add("www.thisisnthere.com");
    reportLinks.add("www.dontbothertrying.org");
    reportLinks.add("www.yahoowontanswer.net");
  }
}