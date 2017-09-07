package com.davis.ddf.crs;

import com.davis.ddf.crs.data.CRSEndpointResponse;
import com.davis.ddf.crs.data.GroovyResponseObject;
import com.davis.ddf.crs.data.SequentialResponse;
import com.davis.ddf.crs.data.SourceEndpointException;
import com.davis.ddf.crs.data.StoredSequentialQuery;
import com.davis.ddf.crs.jsonapi.JsonApiResponse;
import com.davis.ddf.crs.utils.RandomUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LegacyDoubleField;
import org.apache.lucene.document.LegacyIntField;
import org.apache.lucene.document.LegacyLongField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.davis.ddf.crs.utils.RandomUtil.genRandomResponse;
import static com.davis.ddf.crs.utils.Utils.transformDate;

@Path("/")
@Consumes("application/json")
public class SourceEndpoint {

  private static final String NIIRS = "niirs";
  private static final String LOCATION = "location";
  private static final String REPORT_LINK = "reportLink";
  private static final String DISPLAY_TITLE = "displayTitle";
  private static final String DISPLAY_SERIAL = "displaySerial";
  private static final String SUMMARY = "summary";
  private static final String ORIGINATOR_UNIT = "originatorUnit";
  private static final String PRIMARY_EVENT_TYPE = "primaryEventType";
  private static final String CLASSIFICATION = "classification";
  private static final String DATE_OCCURRED = "dateOccurred";
  private static final String LATITUDE = "latitude";
  private static final String LONGITUDE = "longitude";
  private static final String TOP_LEFT = "90 -180";
  private static final String BOTTOM_RIGHT = "-90 180";
  private static final DecimalFormat decFormat = new DecimalFormat("#.#####");
  private static final Logger logger = LoggerFactory.getLogger(SourceEndpoint.class);

  private static final Type RESPONSE_TYPE = new TypeToken<List<CRSEndpointResponse>>() {}.getType();
  private static final int PAGE_SIZE = 10;
  private static final String ERROR_STRING = "There was a error handling the request.";
  private SimpleDateFormat dateFormat;
  private Date DEFAULT_START;
  private Date DEFAULT_END;
  private Date LAST_THREE_YEARS;
  //private List<CRSEndpointResponse> cannedResponses = new ArrayList<>();
  private TreeMap<String, CRSEndpointResponse> cannedResponses = new TreeMap<>();
  private List<Document> cannedDocuments = new ArrayList<>();
  private MultiFieldQueryParser queryParser;
  private Directory ramDirectory;
  private Analyzer analyzer;
  private Map<String, StoredSequentialQuery> queryMap = new HashMap<>();

  /** Instantiates a new Geospatial endpoint. */
  public SourceEndpoint() {}

  public void init() {
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

    try {
      createCannedResults("cannedResults.json");
    } catch (FileNotFoundException e1) {
      logger.error("Unable to create canned Dataset for endpoint. {}", e1);
    }
    createSearchIndexer(false);
  }

  public void shutdown() {
    try {
      ramDirectory.close();
    } catch (Exception e) {
      logger.error("Unable to close ramdirectory {}", e);
    }
    analyzer.close();
  }

  private void createCannedResults(String fileName) throws FileNotFoundException {
    Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();
    BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                SourceEndpoint.class.getClassLoader().getResourceAsStream(fileName)));
    JsonReader jsonReader = new JsonReader(reader);

    List<CRSEndpointResponse> data = gson.fromJson(jsonReader, RESPONSE_TYPE);
    for (CRSEndpointResponse crsEndpointResponse : data) {
      int niirsValue = ThreadLocalRandom.current().nextInt(11);
      crsEndpointResponse.setNiirs(niirsValue);
      cannedResponses.put(crsEndpointResponse.getDisplaySerial(), crsEndpointResponse);
      //cannedResponses.add(crsEndpointResponse);
      cannedDocuments.add(buildDoc(crsEndpointResponse));
    }
  }

  @HEAD
  @Path("/getSourceResults")
  public Response getResultsForSource(@Context UriInfo requestUriInfo) {
    Response.ResponseBuilder builder = null;
    builder = Response.ok();
    return builder.build();
  }

  @GET
  @Path("/getResultsSequential")
  public Response getResultsSequential(
      @Context UriInfo requestUriInfo,
      @QueryParam("amount") String amount,
      @QueryParam("requestId") String requestId,
      @QueryParam("startRow") Integer startRow,
      @QueryParam("endRow") Integer endRow) {
    String queryUri = null;
    if (requestUriInfo == null) {
      queryUri = "https://localhost:8993/services/test/getResultsSequential?amount=5&startRow=1&endRow=10&requestId=4b196d6717e148c9998d0d75cfa9525c";
    } else {
      queryUri = requestUriInfo.getRequestUri().toASCIIString();
    }

    JsonApiResponse response = new JsonApiResponse();
    StoredSequentialQuery storedSequentialQuery = null;
   List<GroovyResponseObject> results = new ArrayList<>();
    Response.ResponseBuilder builder = null;
    SequentialResponse sequentialResponse = null;
    boolean allPagesHaveBeenRead = false;
    if (requestId != null && !requestId.trim().equalsIgnoreCase("")) {
      storedSequentialQuery = queryMap.get(requestId);
      if (storedSequentialQuery != null) {
        if (startRow != null && endRow != null && startRow > 0 && endRow > 0) {
          if (startRow > endRow) {
            throw new SourceEndpointException(
                ERROR_STRING,
                "StartRow cannot be greater than EndRow ",
                Response.Status.BAD_REQUEST);
          } else {
            if (endRow.equals(storedSequentialQuery.getQuery().size())) {
              allPagesHaveBeenRead = true;
            }

            GroovyResponseObject object = null;
            for (int x = startRow; x < endRow; x++) {
              object = storedSequentialQuery.getObjectInStoredQuery(x);
              results.add(object);
            }
            object = storedSequentialQuery.getObjectInStoredQuery(endRow);
            results.add(object);
            sequentialResponse =
                new SequentialResponse(
                    startRow,
                    endRow,
                    queryUri,
                    requestId,
                    PAGE_SIZE,
                    storedSequentialQuery.getQuery().size(),
                    results);
            response.setData(sequentialResponse);
            if (allPagesHaveBeenRead) {
              queryMap.remove(requestId);
            }
          }

        } else {
          throw new SourceEndpointException(
              ERROR_STRING,
              "StartRow and EndRow cannot be Null, and they must be greater than 0.",
              Response.Status.BAD_REQUEST);
        }
      } else {
        throw new SourceEndpointException(
            ERROR_STRING,
            "You supplied a Bad RequestID to the endpoint",
            Response.Status.BAD_REQUEST);
      }

    } else {
      Integer returnStartRow = 1;
      Integer returnEndRow = 10;
      String returnRequestId = UUID.randomUUID().toString().replaceAll("-", "");
      Integer intAmount = null;

      try {
        intAmount = Integer.parseInt(amount);
      } catch (NumberFormatException e) {
        logger.error(
            "Amount specified to the sequential endpoint was not a valid integer values must be a whole digit integer."
                + " Value specified was  {} Error report was {}",
            amount,
            e);
      }
      if (intAmount == null || intAmount <= 0) {
        intAmount = 10;
      }
      storedSequentialQuery = RandomUtil.buildStoredSequentialQuery(intAmount, PAGE_SIZE);
      queryMap.put(returnRequestId, storedSequentialQuery);
      GroovyResponseObject object = null;
      for (int x = 1; x < returnEndRow; x++) {
        object = storedSequentialQuery.getObjectInStoredQuery(x);
        results.add(object);
      }
      object = storedSequentialQuery.getObjectInStoredQuery(returnEndRow);
      results.add(object);
      String modQueryUri =
          queryUri
              + "&requestId="
              + returnRequestId
              + "&startRow="
              + returnStartRow
              + "&endRow="
              + returnEndRow;
      sequentialResponse =
          new SequentialResponse(
              returnStartRow,
              returnEndRow,
              modQueryUri,
              returnRequestId,
              PAGE_SIZE,
              storedSequentialQuery.getQuery().size(),
              results);
      response.setData(sequentialResponse);
    }

    if (response.getData() != null) {
      builder = Response.ok(response.getSanitizedJson(), MediaType.APPLICATION_JSON);
    } else {
      throw new SourceEndpointException(
          ERROR_STRING, "Something went Wrong", Response.Status.INTERNAL_SERVER_ERROR);
    }

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

      resultsList = RandomUtil.buildRandomGroovyResponses(Integer.parseInt(amount));
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
      results = generateDataForRandomResponse(intAmount, start, end, topLeft, bottomRight);
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
    results = searchCannedForResult(amount, null);
    List<CRSEndpointResponse> test = searchCannedForResult(amount, buildTextQuery(amount));
    //results = checkForResults(start, end, bottomRight, topLeft, amount);

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

  private ArrayList<CRSEndpointResponse> generateDataForRandomResponse(
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
        buildObjectsForRandomResponse(
            amount, topLeftLat, topLeftLng, bottomRightLat, bottomRightLng, s, e);
    if (results.size() > 0 && results != null) {
      return results;
    } else {
      return null;
    }
  }

  private ArrayList<CRSEndpointResponse> buildObjectsForRandomResponse(
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
          genRandomResponse(
              lat, lng, topLeftLat, topLeftLng, bottomRightLat, bottomRightLng, start, end);

      results.add(uniResponse);
      amount = amount - 1;
    }
    return results;
  }

  private boolean createSearchIndexer(boolean caseSensitive) {
    boolean result = true;

    try {
      //Required otherwise lucene wont work, gotta love OSGI classloaders.
      Thread.currentThread().setContextClassLoader(SourceEndpoint.class.getClassLoader());

      if (caseSensitive) {

        analyzer = new StandardAnalyzer();
        //analyzer = new CaseSensitiveStandardAnalyzer();
      } else {
        analyzer = new StandardAnalyzer();
      }

      ramDirectory = new RAMDirectory();
      IndexWriterConfig config = new IndexWriterConfig(analyzer);
      IndexWriter indexWriter = null;
      List<IndexableField> fields = new ArrayList<>();

      //they all have the same fields so it doesnt matter if i only do this once
      for (IndexableField field : cannedDocuments.get(0).getFields()) {
        fields.add(field);
      }
      logger.debug("LUCENE SEARCH :: Creating index...");
      indexWriter = new IndexWriter(ramDirectory, config);
      for (Document metaDoc : cannedDocuments) {
        indexWriter.addDocument(metaDoc);
        indexWriter.flush();
      }
      //close the index writer all documents have been added.
      indexWriter.commit();
      indexWriter.close();
      logger.debug("LUCENE SEARCH :: Index created.");

      //query logic
      List<String> fieldNames =
          fields.stream().map(field -> field.name()).collect(Collectors.toList());
      //Create the String[] requirement for the MultiFieldQueryParser
      String[] allFields = (String[]) fieldNames.toArray(new String[fieldNames.size()]);
      //Create a new multifield query parser.
      queryParser = new MultiFieldQueryParser(allFields, analyzer);
      //Set allow leading wildcard to true.
      queryParser.setAllowLeadingWildcard(true);
      //Set the default operator to AND
      //queryParser.setDefaultOperator(QueryParser.Operator.OR);
      queryParser.setDefaultOperator(QueryParser.Operator.AND);
    } catch (Exception e) {
      //catching everything
      logger.error("Unable to create the lucene in memory config for canned respones. {}", e);
      result = false;
    }
    return result;
  }

  private BooleanQuery buildTextQuery(String query) {
    QueryParser summParser = new QueryParser(SUMMARY, analyzer);
    QueryParser primEventParser = new QueryParser(PRIMARY_EVENT_TYPE, analyzer);
    QueryParser classParser = new QueryParser(CLASSIFICATION, analyzer);
    QueryParser displayParser = new QueryParser(DISPLAY_TITLE, analyzer);
    QueryParser reportParser = new QueryParser(REPORT_LINK, analyzer);
    Query summQ = null;
    Query primQ = null;
    Query classQ = null;
    Query titleQ = null;
    Query reportQ = null;
    List<BooleanClause> clauses = new ArrayList<>();
    boolean allNots = false;
    String[] queryArray = query.split(" ");
    int notCount = 0;
    for (String term : queryArray) {
      if (term.contains("!")) {
        notCount = notCount + 1;
      }
    }
    if (notCount >= queryArray.length) {
      allNots = true;
    }
    if (!allNots) {
      for (String term : queryArray) {
        BooleanClause.Occur whatType = null;
        if (term.contains("!")) {
          whatType = BooleanClause.Occur.MUST_NOT;
        } else {
          whatType = BooleanClause.Occur.MUST;
        }
        try {
          summQ = summParser.parse(term);
          primQ = primEventParser.parse(term);
          classQ = classParser.parse(term);
          titleQ = displayParser.parse(term);
          reportQ = reportParser.parse(term);
          clauses.add(new BooleanClause(summQ, whatType));
          clauses.add(new BooleanClause(primQ, whatType));
          clauses.add(new BooleanClause(classQ, whatType));
          clauses.add(new BooleanClause(titleQ, whatType));
          clauses.add(new BooleanClause(reportQ, whatType));
        } catch (org.apache.lucene.queryparser.classic.ParseException e) {
          logger.error("Error in parsing the supplied query {}", e);
        }
      }
      BooleanQuery.Builder booleanBuilder = new BooleanQuery.Builder();
      for (BooleanClause clause : clauses) {
        booleanBuilder.add(clause);
      }
      return booleanBuilder.build();
    } else {
      return null;
    }
  }

  /*public void searchIndexWithQueryParser(String whichField, String searchString)
      throws IOException, ParseException {
    System.out.println("\nSearching for '" + searchString + "' using QueryParser");
    IndexReader indexReader = DirectoryReader.open(ramDirectory);
    IndexSearcher indexSearcher = new IndexSearcher(indexReader);
    int hitsPerPage = 10000;
    QueryParser queryParser = new QueryParser(whichField, analyzer);
    Query query = null;
    try {
      query = queryParser.parse(searchString);
    } catch (org.apache.lucene.queryparser.classic.ParseException e) {
      logger.error(
          "Error in parsing query for field of {} with query String of {} reason given was {}",
          whichField,
          searchString,
          e);
    }
    System.out.println("Type of query: " + query.getClass().getSimpleName());
    TopDocs hits = indexSearcher.search(query, hitsPerPage);
  }*/

  /*public void searchIndexWithRangeQuery(
      String whichField, String start, String end, boolean inclusive)
      throws IOException, ParseException {
    System.out.println("\nSearching for range '" + start + " to " + end + "' using RangeQuery");
    IndexReader indexReader = DirectoryReader.open(ramDirectory);
    int hitsPerPage = 10000;
    IndexSearcher indexSearcher = new IndexSearcher(indexReader);
    Term startTerm = new Term(whichField, start);
    Term endTerm = new Term(whichField, end);
    Query query = new TermRangeQuery(startTerm, endTerm, inclusive);
    TopDocs hits = indexSearcher.search(query);
    displayHits(hits);
  }*/

  private Query createQueryForParams(
      String textQuery, Date dateStart, Date dateEnd, Double lat, Double lon) {

    return null;
  }

  public List<CRSEndpointResponse> searchCannedForResult(
      String queryString, BooleanQuery booleanQuery) {
    IndexReader indexReader = null;
    IndexSearcher indexSearcher = null;
    TopDocs searchResultsFromLucene = null;
    ScoreDoc[] topResultsFromLucene = null;
    List<CRSEndpointResponse> returnResults = new ArrayList<>();
    TreeMap<String, CRSEndpointResponse> luceneResultMap = new TreeMap<>();
    //Create our query string for lucene
    logger.debug("LUCENE SEARCH :: Creating parser for query [{}]", queryString);
    Query q = null;
    try {
      if (booleanQuery != null) {
        q = booleanQuery;
      } else {
        q = queryParser.parse(queryString);
      }

      //Set our hits per page to a very large number
      int hitsPerPage = 10000;
      //Create our index reader for us to search
      logger.debug("LUCENE SEARCH :: Opening index...");
      try {
        indexReader = DirectoryReader.open(ramDirectory);
        logger.debug("LUCENE SEARCH :: Index contains {} documents", indexReader.numDocs());
        //Create our index searcher to actually search the index
        indexSearcher = new IndexSearcher(indexReader);
        //Represents hits returned by the search
        logger.debug("LUCENE SEARCH :: Searching index...");
        try {
          searchResultsFromLucene = indexSearcher.search(q, hitsPerPage);
          logger.debug(
              "LUCENE SEARCH :: Search complete. {} total hits.",
              searchResultsFromLucene.totalHits);
          //The top results from the search.
          topResultsFromLucene = searchResultsFromLucene.scoreDocs;

          int hits = 0;
          for (ScoreDoc result : topResultsFromLucene) {
            //Retrieve the document from the indexSearcher
            Document d = null;
            try {
              d = indexSearcher.doc(result.doc);
            } catch (IOException e) {
              logger.error("Unable to retrieve document for result {} error was {}", result.doc, e);
              continue;
            }
            if (d != null && d.getField(DISPLAY_SERIAL) != null) {
              String metacardId = d.getField(DISPLAY_SERIAL).stringValue();
              if (luceneResultMap.get(metacardId) == null) {
                luceneResultMap.put(metacardId, cannedResponses.get(metacardId));
                logger.trace(
                    "Found a result within a metacard with ID of {} adding to result set .... .",
                    metacardId);
                logger.trace("Document matched Lucene criteria, adding to result set ... ");
                hits++;
              }
            }
          }
          returnResults.addAll(luceneResultMap.values());
          // We no longer need to access the documents so we close the indexReader
          // This can only be done after all reasons for accessing the documents is gone, if you attempt otherwise
          // Exceptions will occur.
          try {
            indexReader.close();
          } catch (IOException e) {
            logger.error("Unable to close the Index reader {}", e);
          }
          logger.debug("LUCENE SEARCH :: Added {} documents to the results.", hits);
        } catch (IOException e) {
          logger.error("Unable to search the index {}", e);
        }
      } catch (IOException e) {
        logger.error("Index reader was unable to open the ram directory. {}", e);
      }

    } catch (org.apache.lucene.queryparser.classic.ParseException e) {
      logger.error("Unable to parse the query string supplied to the endpoint. {}", e);
    }
    if (returnResults == null) {
      returnResults = new ArrayList<>();
    }
    return returnResults;
  }

  /**
   * Construct document from result document.
   *
   * @param r the r
   * @return the document
   */
  public Document buildDoc(CRSEndpointResponse r) {

    Document doc = new Document();
    if (r.getDisplayTitle() != null) {
      doc.add(
          new org.apache.lucene.document.TextField(
              DISPLAY_TITLE, String.valueOf(r.getDisplayTitle()), Field.Store.YES));
    }
    if (r.getNiirs() != null) {
      doc.add(new LegacyIntField(NIIRS, r.getNiirs(), Field.Store.YES));
      /* doc.add(
      new org.apache.lucene.document.TextField(
              NIIRS,
              String.valueOf(r.getNiirs()),
              Field.Store.YES));*/
    }
    if (r.getSummary() != null) {
      doc.add(
          new org.apache.lucene.document.TextField(
              SUMMARY, String.valueOf(r.getSummary()), Field.Store.YES));
    }
    if (r.getLatitude() != null && r.getLongitude() != null) {
      doc.add(new LegacyDoubleField(LATITUDE, r.getLatitude(), Field.Store.YES));
      doc.add(new LegacyDoubleField(LONGITUDE, r.getLongitude(), Field.Store.YES));
      /*
      doc.add(
              new LatLonDocValuesField(LATITUDE, r.getLatitude(), r.getLongitude()));
      doc.add(
              new LatLonDocValuesField(LONGITUDE, r.getLatitude(), r.getLongitude()));*/

    }
    if (r.getLocation() != null) {
      doc.add(
          new org.apache.lucene.document.TextField(
              LOCATION, String.valueOf(r.getLocation()), Field.Store.YES));
    }
    if (r.getClassification() != null) {
      doc.add(
          new org.apache.lucene.document.TextField(
              CLASSIFICATION, String.valueOf(r.getClassification()), Field.Store.YES));
    }
    if (r.getOriginatorUnit() != null) {
      doc.add(
          new org.apache.lucene.document.TextField(
              ORIGINATOR_UNIT, String.valueOf(r.getOriginatorUnit()), Field.Store.YES));
    }
    if (r.getDisplaySerial() != null) {
      doc.add(
          new org.apache.lucene.document.TextField(
              DISPLAY_SERIAL, String.valueOf(r.getDisplaySerial()), Field.Store.YES));
    }
    if (r.getPrimaryEventType() != null) {
      doc.add(
          new org.apache.lucene.document.TextField(
              PRIMARY_EVENT_TYPE, String.valueOf(r.getPrimaryEventType()), Field.Store.YES));
    }
    if (r.getReportLink() != null) {
      doc.add(
          new org.apache.lucene.document.TextField(
              REPORT_LINK, String.valueOf(r.getReportLink()), Field.Store.YES));
    }
    if (r.getDateOccurred() != null) {
      doc.add(new LegacyLongField(DATE_OCCURRED, r.getDateOccurred().getTime(), Field.Store.YES));
    }
    return doc;
  }

  /*static final class CaseSensitiveStandardAnalyzer extends StopwordAnalyzerBase {
    @Override
    protected TokenStreamComponents createComponents(String string) {
      final StandardTokenizer src;
      TokenStream tok;

      try (StandardAnalyzer standardAnalyzer = new StandardAnalyzer()) {
        src = new StandardTokenizer();
        src.setMaxTokenLength(standardAnalyzer.getMaxTokenLength());
        tok = new StandardFilter(src);
        tok = new StopFilter(tok, stopwords);
      }
      return new TokenStreamComponents(src, tok);
    }
  }*/
}
