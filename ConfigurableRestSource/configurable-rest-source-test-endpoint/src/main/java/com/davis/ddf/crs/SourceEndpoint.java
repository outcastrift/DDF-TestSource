package com.davis.ddf.crs;

import com.davis.ddf.crs.data.GroovyResponseObject;
import com.davis.ddf.crs.data.InMemoryDataStore;
import com.davis.ddf.crs.data.CRSEndpointResponse;
import com.davis.ddf.crs.jsonapi.JsonApiResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/** The type Geospatial endpoint. */
@Path("/")
@Consumes("application/json")
public class SourceEndpoint {
  /**
   * An unmodifiable set containing some common English words that are not usually useful for
   * searching.
   */
  public static final List<String> stopWords =
      Arrays.asList(
          "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is",
          "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then", "there",
          "these", "they", "this", "to", "was", "will", "with");

  private static final Double MAX_LAT = Double.valueOf(90); //Y variable
  private static final Double MIN_LAT = Double.valueOf(-90); //Y variable
  private static final Double MAX_LON = Double.valueOf(180); //X variable
  private static final Double MIN_LON = Double.valueOf(-180); //X Variable
  private static final String TOP_LEFT = "90 -180";
  private static final String BOTTOM_RIGHT = "-90 180";
  private static final int POLYGON = 0;
  private static final int MULTIPOLYGON = 1;
  private static final int POINT = 2;
  private static final int MULTIPOINT = 3;
  private static final int LINESTRING = 4;
  private static final int MULTILINESTRING = 5;
  private static final DecimalFormat decFormat = new DecimalFormat("#.#####");
  /** The constant TAG. */
  private static final Logger logger = LoggerFactory.getLogger(SourceEndpoint.class);

  private static final Type RESPONSE_TYPE = new TypeToken<List<CRSEndpointResponse>>() {}.getType();
  private SimpleDateFormat dateFormat;
  private Date DEFAULT_START;
  private Date DEFAULT_END;
  private Date LAST_THREE_YEARS;
  private InMemoryDataStore dataStore;
  private List<CRSEndpointResponse> cannedResponses = new ArrayList<>();

  /** Instantiates a new Geospatial endpoint. */
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
    Calendar cy = Calendar.getInstance();

    cy.set(2014, 0, 1);
    String lastThreeYears = dateFormat.format(cy.getTime());
    try {
      LAST_THREE_YEARS = dateFormat.parse(lastThreeYears);
    } catch (ParseException ey) {
      logger.error("error countered parsing date {}", ey.getMessage());
    }

    dataStore = new InMemoryDataStore();
    try {
      createCannedResults("cannedResults.json");
      createCannedResults("usaResults.json");
    } catch (FileNotFoundException e1) {
      logger.error("Unable to create canned Dataset for endpoint. {}", e);
    }
  }

  /**
   * Transform date string.
   *
   * @param date the date
   * @param dateFormat the date format
   * @return the string
   */
  public static String transformDate(Date date, SimpleDateFormat dateFormat) {
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

  private void createCannedResults(String fileName) throws FileNotFoundException {
    Gson gson = new Gson();
    BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                SourceEndpoint.class.getClassLoader().getResourceAsStream(fileName)));
    JsonReader jsonReader = new JsonReader(reader);
    List<CRSEndpointResponse> data = gson.fromJson(reader, RESPONSE_TYPE);
    cannedResponses.addAll(data);
  }

  public CRSEndpointResponse createCannedResult(
      InMemoryDataStore dataStore, int itemId, int idOffset) {

    //Works for USA
   /* double topLeftLat = 47.5531;
    double topLeftLng = -124.3574;
    double bottomRightLat = -71.6631;
    double bottomRightLng = 31.0422;*/
    double topLeftLat = 90;
    double topLeftLng = -180;
    double bottomRightLat = -90 ;
    double bottomRightLng = 180;
    CRSEndpointResponse uniResponse = new CRSEndpointResponse();
    int whichGeom = ThreadLocalRandom.current().nextInt(6);
    //logger.info("Array Number = {}", whichGeom);
    String wktString =
        constructWktString(whichGeom, topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
    uniResponse.setLocation(wktString);

    Date sigactDate = generateRandomDate(LAST_THREE_YEARS, DEFAULT_END);
    uniResponse.setClassification("UNCLASSIFIED");
    uniResponse.setDisplayTitle(generateTitleBasedOnGeometry(wktString, sigactDate));
    uniResponse.setDateOccurred(sigactDate);
    double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
    lat = Double.parseDouble(decFormat.format(lat));
    //logger.debug("Entering always successful lng. Origin = {} Bound = {}",topLeftLng,bottomRightLng);
    double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
    lng = Double.parseDouble(decFormat.format(lng));
    uniResponse.setDisplaySerial("CRS-" + String.valueOf(itemId + idOffset));
    uniResponse.setLatitude(lat);
    uniResponse.setLongitude(lng);
    uniResponse.setOriginatorUnit(dataStore.getOriginateUnit().get(itemId));
    uniResponse.setPrimaryEventType(getRandomizedField(dataStore.getEventTypes()));
    uniResponse.setReportLink(getRandomizedField(dataStore.getReportLinks()));
    uniResponse.setSummary(dataStore.getSummaries().get(itemId));
    return uniResponse;
  }

  @HEAD
  @Path("/getSourceResults")
  public Response getResultsForSource(@Context UriInfo requestUriInfo) {
    Response.ResponseBuilder builder = null;
    builder = Response.ok();
    return builder.build();
  }

  /**
   * Convert geopoint response.
   *
   * @param requestUriInfo the request uri info
   * @param amount the amount
   * @return the response
   */
  @GET
  @Path("/getGroovyResults")
  public Response getResultsForGroovy(
      @Context UriInfo requestUriInfo, @QueryParam("amount") String amount) {
    int intAmount = 0;
    List<GroovyResponseObject> resultsList = null;

    JsonApiResponse response = new JsonApiResponse();
    Response.ResponseBuilder builder = null;
    if (amount != null) {

      resultsList = buildObjects(Integer.parseInt(amount));
      response.setData(resultsList);
    }

    if (response.getData() != null) {
      builder = Response.ok(response.getSanitizedJson(), MediaType.APPLICATION_JSON);
    } else {
      throw new SourceEndpointException(
          "There was a error handling the request.",
          "There was a error handling the " + "" + "" + "request.",
          Response.Status.INTERNAL_SERVER_ERROR);
    }

    return builder.build();
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
   * @param amount the amount
   * @return the response
   */
  @GET
  @Path("/getSourceResultsRandom")
  public Response getResultsForSourceRandom(
      @Context UriInfo requestUriInfo,
      @QueryParam("startDate") String startDate,
      @QueryParam("endDate") String endDate,
      @QueryParam("topLeftLatLong") String topLeftLatLong,
      @QueryParam("bottomRightLatLong") String bottomRightLatLong,
      @QueryParam("amount") String amount)
      throws UnsupportedEncodingException {

    logger.debug("SourceEndpoint received query");
    int intAmount = 0;
    String topLeft = null;
    String bottomRight = null;
    Date start = null;
    Date end = null;
    ArrayList<CRSEndpointResponse> results = null;
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

    JsonApiResponse response = new JsonApiResponse();
    Response.ResponseBuilder builder = null;
    response.setData(results);
    Response here = null;
    if (response.getData() != null) {
      String responseData = response.getSanitizedJson();
      builder =
          Response.ok(responseData, MediaType.APPLICATION_JSON)
              .header(HttpHeaders.CONTENT_LENGTH, responseData.getBytes("UTF-8").length);
      here = builder.build();
      logger.debug("SourceEndpoint Query Result Code  = {} ", String.valueOf(here.getStatus()));
    } else {
      logger.debug("SourceEndpoint ERROR There was a error handling the request. ");
      throw new SourceEndpointException(
          "There was a error handling the request.",
          "There was a error handling the " + "" + "" + "request.",
          Response.Status.INTERNAL_SERVER_ERROR);
    }

    return here;
  }

  /**
   * Create federated source response.
   *
   * @param requestUriInfo the request uri info
   * @param amount the amount
   * @return the response
   */
  @GET
  @Path("/getSourceResultsCanned")
  public Response getResultsForSourceCanned(
      @Context UriInfo requestUriInfo,
      @QueryParam("startDate") String startDate,
      @QueryParam("endDate") String endDate,
      @QueryParam("topLeftLatLong") String topLeftLatLong,
      @QueryParam("bottomRightLatLong") String bottomRightLatLong,
      @QueryParam("amount") String amount,
      @QueryParam("id") @DefaultValue("-1") Integer id)
      throws UnsupportedEncodingException {

    logger.debug("SourceEndpoint received query");
    int intAmount = 0;
    String topLeft = null;
    String bottomRight = null;
    Date start = null;
    Date end = null;
    List<CRSEndpointResponse> results = null;
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

    results=  checkForResults(start, end, bottomRight, topLeft, amount);


    JsonApiResponse response = new JsonApiResponse();
    Response.ResponseBuilder builder = null;
    response.setData(results);
    Response here = null;
    if (response.getData() != null) {
      String responseData = response.getSanitizedJson();
      builder =
          Response.ok(responseData, MediaType.APPLICATION_JSON)
              .header(HttpHeaders.CONTENT_LENGTH, responseData.getBytes("UTF-8").length);
      here = builder.build();
      logger.debug("SourceEndpoint Query Result Code  = {} ", String.valueOf(here.getStatus()));
    } else {
      logger.debug("SourceEndpoint ERROR There was a error handling the request. ");
      throw new SourceEndpointException(
          "There was a error handling the request.",
          "There was a error handling the " + "" + "" + "request.",
          Response.Status.INTERNAL_SERVER_ERROR);
    }

    return here;
  }

  private List<CRSEndpointResponse> checkForResults(
      Date start, Date end, String bottomRight, String topLeft, String queryString) {
    List<CRSEndpointResponse> result = new ArrayList<>();
    String latSegments[] = StringUtils.split(topLeft);
    String lngSegments[] = StringUtils.split(bottomRight);
    double topLeftLat = Double.parseDouble(latSegments[0]);

    double topLeftLng = Double.parseDouble(latSegments[1]);

    double bottomRightLat = Double.parseDouble(lngSegments[0]);

    double bottomRightLng = Double.parseDouble(lngSegments[1]);

    for (CRSEndpointResponse e : cannedResponses) {
      if (checkResultForMatching(
          topLeftLat, topLeftLng, bottomRightLat, bottomRightLng, queryString, start, end, e)) {
        result.add(e);
      }
    }

    return result;
  }

  private boolean checkResultForMatching(
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

  private boolean checkQueryString(String queryString, CRSEndpointResponse item) {
    boolean result = false;
    if(queryString != null){
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

        if (!stopWords.contains(param)) {
          if (itemRollUp.contains(param)) {
            result = true;
            break;
          }
        }
      }
    }else{
      result = true;
    }


    return result;
  }

  private boolean checkDateInRange(Date from, Date to, CRSEndpointResponse item) {
    boolean result = false;
    Long itemDate = item.getDateOccurred().getTime();

    if (itemDate > from.getTime() && itemDate < to.getTime()) {
      result = true;
    }
    return result;
  }

  private boolean checkDoubleInRange(
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
      double lat = center.getX();
      double lng = center.getY();

      if (lat > bottomRightLat && lat < topLeftLat) {
        if (lng > bottomRightLng && lng < topLeftLng) {
          result = true;
        }
      }

    } catch (com.vividsolutions.jts.io.ParseException e) {
      logger.error("Unable to parse WKT determining the item to be false.");
    }

    return result;
  }

  /**
   * Get integer from string int.
   *
   * @param s the s
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

  private ArrayList<CRSEndpointResponse> generateDataForResponse(
      int amount, Date start, Date end, String topLeft, String bottomRight) {

    String latSegments[] = StringUtils.split(topLeft);
    String lngSegments[] = StringUtils.split(bottomRight);
    double topLeftLat = Double.parseDouble(latSegments[0]);

    double topLeftLng = Double.parseDouble(latSegments[1]);

    double bottomRightLat = Double.parseDouble(lngSegments[0]);

    double bottomRightLng = Double.parseDouble(lngSegments[1]);

    //Y is lat
    //X is lon
    String startDate = transformDate(start, dateFormat);
    String endDate = transformDate(end, dateFormat);
    Date s = null;
    Date e = null;
    try {
      s = dateFormat.parse(startDate);
      e = dateFormat.parse(endDate);
    } catch (Exception ed) {
      logger.error(ed.getMessage());
    }

    ArrayList<CRSEndpointResponse> results =
        buildObjectsForSource(amount, topLeftLat, topLeftLng, bottomRightLat, bottomRightLng, s, e);
    if (results.size() > 0 && results != null) {
      return results;
    } else {
      return null;
    }
  }

  private ArrayList<CRSEndpointResponse> buildObjectsForSource(
      int amount,
      Double topLeftLat,
      Double topLeftLng,
      Double bottomRightLat,
      Double bottomRightLng,
      Date start,
      Date end) {
    ArrayList<CRSEndpointResponse> results = new ArrayList<>();

    while (amount > 0) {
      //logger.debug("Entering always successful lat. Origin = {} Bound = {}",bottomRightLat,topLeftLat);

      double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
      lat = Double.parseDouble(decFormat.format(lat));
      //logger.debug("Entering always successful lng. Origin = {} Bound = {}",topLeftLng,bottomRightLng);

      double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
      lng = Double.parseDouble(decFormat.format(lng));

      CRSEndpointResponse uniResponse =
          generateRandomMetacard(
              lat, lng, topLeftLat, topLeftLng, bottomRightLat, bottomRightLng, start, end);

      results.add(uniResponse);
      amount = amount - 1;
    }
    return results;
  }

  private CRSEndpointResponse generateRandomMetacard(
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
    uniResponse.setDisplayTitle(generateTitleBasedOnGeometry(wktString, sigactDate));
    uniResponse.setDateOccurred(sigactDate);
    uniResponse.setDisplaySerial(
        String.valueOf(topLeftLat)
            + String.valueOf(bottomRightLng)
            + String.valueOf(lat)
            + String.valueOf(lng));
    uniResponse.setLatitude(lat);
    uniResponse.setLongitude(lng);
    uniResponse.setOriginatorUnit(getRandomizedField(dataStore.getOriginateUnit()));
    uniResponse.setPrimaryEventType(getRandomizedField(dataStore.getEventTypes()));
    uniResponse.setReportLink(getRandomizedField(dataStore.getReportLinks()));
    uniResponse.setSummary(getRandomizedField(dataStore.getSummaries()));
    return uniResponse;
  }

  public String constructWktString(
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
      case MULTIPOLYGON:
        {
          try {
            result =
                createRandomWktMultiPolygon(topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
          } catch (Exception e) {
            logger.error("Wkt Creation Failed For MULTIPOLYGON");
          }
        }
        break;
      case POINT:
        {
          try {
            result = createRandomWktPoint(topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
          } catch (Exception e) {
            logger.error("Wkt Creation Failed For POINT");
          }
        }
        break;
      case MULTIPOINT:
        {
          try {
            result =
                createRandomWktMultiPoint(topLeftLat, topLeftLng, bottomRightLat, bottomRightLng);
          } catch (Exception e) {
            logger.error("Wkt Creation Failed For MULTIPOINT");
          }
        }
        break;
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
    //int year  = dMax.getYear() - dMin.getYear();
    Calendar cal = Calendar.getInstance();
    cal.setTime(dMin);
    cal.add(Calendar.DATE, new Random().nextInt((int) dayDiff));
    //int year  = (int) ThreadLocalRandom.current().nextDouble(1970, 2017);
    //cal.add(Calendar.YEAR, year);
    return cal.getTime();
  }

  public String generateTitleBasedOnGeometry(String geom, Date date) {
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

  //Verified and tested this method creates valid WKT Polygons
  private String createRandomWktPolygon(
      Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double bottomRightLng) {
    StringBuilder result = new StringBuilder();
    double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
    lat = Double.parseDouble(decFormat.format(lat));
    double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
    lng = Double.parseDouble(decFormat.format(lng));
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

  //Verified and tested this method creates valid WKT MultiPolygons
  private String createRandomWktMultiPolygon(
      Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double bottomRightLng) {
    StringBuilder result = new StringBuilder();
    int numberOfPolygons = ThreadLocalRandom.current().nextInt(4) + 1;

    result.append("MULTIPOLYGON (");

    for (int y = 0; y < numberOfPolygons; y++) {
      StringBuilder inner = new StringBuilder();
      double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
      lat = Double.parseDouble(decFormat.format(lat));
      double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
      lng = Double.parseDouble(decFormat.format(lng));
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
  private String createRandomWktPoint(
      Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double bottomRightLng) {
    String result = null;
    //logger.debug("Entering generate lat. Origin = {} Bound = {}",bottomRightLat,topLeftLat);
    double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
    //logger.debug("Exit generate lat. lat = {} ",lat);
    lat = Double.parseDouble(decFormat.format(lat));
    //logger.debug("Lat after decimal format lat = {} ",lat);
    //logger.debug("Entering generate lng. Origin = {} Bound = {}",bottomRightLng,topLeftLng);
    double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
    //logger.debug("Exit generate lng. lng = {} ",lng);
    lng = Double.parseDouble(decFormat.format(lng));
    //logger.debug("Lng after decimal format lng = {} ",lat);
    result = "POINT(" + lng + " " + lat + ")";
    return result;
  }

  //Verified this method produces valid multipoints.
  private String createRandomWktMultiPoint(
      Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double bottomRightLng) {
    StringBuilder result = new StringBuilder();
    int numberOfPoints = ThreadLocalRandom.current().nextInt(20) + 1;
    double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
    lat = Double.parseDouble(decFormat.format(lat));
    double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
    lng = Double.parseDouble(decFormat.format(lng));
    result.append("MULTIPOINT (");
    result.append("(" + lng + " " + lat + "), ");
    for (int x = 0; x < numberOfPoints; x++) {
      double latInner = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
      latInner = Double.parseDouble(decFormat.format(latInner));
      double lngInner = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
      lngInner = Double.parseDouble(decFormat.format(lngInner));
      result.append("(" + lngInner + " " + latInner + "), ");
    }
    String fix = result.toString();
    fix = fix.substring(0, fix.length() - 2);
    fix = fix + ")";
    return fix;
  }

  private double genRandomLevel() {
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

  private double generateUpperRight(double lng, double amount) {
    double newLong = lng + amount;
    if (newLong > 180) {
      newLong = 180;
    } else if (newLong < -180) {
      newLong = -180;
    }
    return newLong;
  }

  private double generateLowerRight(double lat, double amount) {
    double newLat = lat - amount;
    if (newLat > 90) {
      newLat = 90;
    } else if (newLat < -90) {
      newLat = -90;
    }
    return newLat;
  }

  private double generateLowerLeft(double lat, double amount) {
    double newLat = lat - amount;
    if (newLat > 90) {
      newLat = 90;
    } else if (newLat < -90) {
      newLat = -90;
    }
    return newLat;
  }

  //Verified this method returns valid line.
  private String createRandomWktLine(
      Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double bottomRightLng) {
    StringBuilder result = new StringBuilder();
    int numberOfPoints = ThreadLocalRandom.current().nextInt(4) + 1;
    double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
    lat = Double.parseDouble(decFormat.format(lat));
    double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
    lng = Double.parseDouble(decFormat.format(lng));
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
  private String createRandomWktMultiLine(
      Double topLeftLat, Double topLeftLng, Double bottomRightLat, Double bottomRightLng) {
    StringBuilder result = new StringBuilder();
    int numberOfLines = ThreadLocalRandom.current().nextInt(4) + 1;
    result.append("MULTILINESTRING (");

    for (int y = 0; y < numberOfLines; y++) {
      StringBuilder innerLoop = new StringBuilder();
      innerLoop.append("(");
      int numberOfPoints = ThreadLocalRandom.current().nextInt(4) + 1;
      double lat = ThreadLocalRandom.current().nextDouble(bottomRightLat, topLeftLat);
      lat = Double.parseDouble(decFormat.format(lat));
      double lng = ThreadLocalRandom.current().nextDouble(topLeftLng, bottomRightLng);
      lng = Double.parseDouble(decFormat.format(lng));

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

  private Double genRandomDoubleInRange(Double origin, Double bounds) {
    double result = ThreadLocalRandom.current().nextDouble(origin, bounds);
    result = Double.parseDouble(decFormat.format(result));
    return result;
  }
}
