package com.davis.ddf.crs;

import com.davis.ddf.crs.client.TrustingOkHttpClient;
import com.davis.ddf.crs.data.CRSMetacard;
import com.davis.ddf.crs.data.CRSMetacardType;
import com.davis.ddf.crs.data.CRSResponse;
import com.davis.ddf.crs.filter.CRSFilterVisitor;
import com.davis.ddf.crs.service.RestGetService;
import com.davis.ddf.crs.service.SourceService;
import com.davis.ddf.crs.util.SourceUtil;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardTypeRegistry;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.mime.MimeTypeMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringEscapeUtils;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Geometry;
import org.opengis.geometry.primitive.Point;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CRSSource implements ddf.catalog.source.FederatedSource {

  //region Class Variables
  private static final Logger LOGGER = LoggerFactory.getLogger(CRSSource.class);
  private static final String DEFAULT_TYPE = CRSMetacardType.NAME;
  private static final long EXPIRATION_OFFSET = 3600000;
  private static int SAX = 1;
  private static int REST = 2;
  public String ssClientCertPath;
  public String ssClientCertPassword;
  private String contentTypeName = "Sigact";
  /** Spring set variables */
  private String ssWktStringParam;

  private String ssNiirs;
  private String ssContextSearchParam;
  private String ssSpatialSearchParamLat;
  private String ssSpatialSearchParamLong;
  private String ssTemporalSearchParamEnd;
  private String ssTemporalSearchParamStart;
  private String ssReportLink;
  private String ssDisplayTitle;
  private String ssDisplaySerial;
  private String ssSummary;
  private String ssOriginatorUnit;
  private String ssPrimaryEventType;
  private String ssClassification;
  private String ssDateOccurred;
  private String ssLatitude;
  private String ssLongitude;
  private String ssContainerPath;
  private String ssServiceUrl;
  private String ssDescription = "Tfr TfrResponseObjects Federated Source";
  private String ssShortName;
  private boolean ssShouldConvertToBBox;

  /** End Spring set variables */
  private NumberFormat decimalFormatter = new DecimalFormat("#0.0000");
  private static final String OR_CLAUSES_FOR_QUERY = "OrClausesForQuery";
  private String version = "v10";
  private String org = "AFRL";
  private MimeTypeMapper mimeTypeMapper;
  private int mode = REST;
  private Set<ContentType> contentTypes;
  private SimpleDateFormat dateFormat;
  private String DEFAULT_TYPE_VERSION = DEFAULT_TYPE + version;
  private RestGetService restGetService;
  private OkHttpClient client;
  private CatalogFramework catalogFramework;
  private boolean isAvailable;
  private String sourceId = "UniversalFedSource" + ssShortName;
  private MetacardTypeRegistry metacardTypeRegistry;
  //endregion

  //region Constructors

  /** Test Constructor */
  public CRSSource(HashMap<String, String> springVars) {
    setSsShortName(springVars.get("ssShortName"));
    setSsClassification(springVars.get("ssClassification"));
    setSsDateOccurred(springVars.get("ssDateOccurred"));
    setSsDisplaySerial(springVars.get("ssDisplaySerial"));
    setSsLatitude(springVars.get("ssLatitude"));
    setSsLongitude(springVars.get("ssLongitude"));
    setSsDescription(springVars.get("ssDescription"));
    setSsContainerPath(springVars.get("ssContainerPath"));
    setSsContextSearchParam(springVars.get("ssContextSearchParam"));
    setSsServiceUrl(springVars.get("ssServiceUrl"));
    setSsSpatialSearchParamLat(springVars.get("ssSpatialSearchParamLat"));
    setSsSpatialSearchParamLong(springVars.get("ssSpatialSearchParamLong"));
    setSsSummary(springVars.get("ssSummary"));
    setSsTemporalSearchParamEnd(springVars.get("ssTemporalSearchParamEnd"));
    setSsTemporalSearchParamStart(springVars.get("ssTemporalSearchParamStart"));
    setSsOriginatorUnit(springVars.get("ssOriginatorUnit"));
    setSsPrimaryEventType(springVars.get("ssPrimaryEventType"));
    setSsReportLink(springVars.get("ssReportLink"));
    setSsDisplayTitle(springVars.get("ssDisplayTitle"));
    setSsClientCertPath(springVars.get("ssClientCertPath"));
    setSsClientCertPassword(springVars.get("ssClientCertPassword"));
    setSsWktStringParam(springVars.get("ssWktStringParam"));
    mode = REST;
    client = new TrustingOkHttpClient().getUnsafeOkHttpClient(15, 15, null, null);
    try {
      String dateFormatPattern = "yyyy-MM-dd'T'HH:mm:ssZ";
      dateFormat = new SimpleDateFormat(dateFormatPattern);
      isAvailable = true;
      LOGGER.info("Successfully created CRSSource");

    } catch (Exception ex) {
      LOGGER.error("Error  = {}", ex);
    }
  }

  /**
   * Instantiates a new Test federated source.
   *
   * @throws IngestException the ingest exception
   */
  public CRSSource() {
    mode = REST;
    setupOSGIServices();
    try {
      String dateFormatPattern = "yyyy-MM-dd'T'HH:mm:ssZ";
      dateFormat = new SimpleDateFormat(dateFormatPattern);
      isAvailable = true;
      LOGGER.info("Successfully created CRSSource");

    } catch (Exception ex) {
      LOGGER.error("Error  = {}", ex);
    }
    client = new TrustingOkHttpClient().getUnsafeOkHttpClient(15, 15, null, null);
  }

  //endregion

  //region Osgi Service Creation
  private void setupOSGIServices() {
    BundleContext bc = FrameworkUtil.getBundle(CRSSource.class).getBundleContext();
    //Service reference in order to populate our service
    ServiceReference serviceReference = bc.getServiceReference(MimeTypeMapper.class.getName());
    //get the service
    mimeTypeMapper = (MimeTypeMapper) bc.getService(serviceReference);

    //Service reference in order to populate our service
    ServiceReference serviceReference2 =
        bc.getServiceReference(MetacardTypeRegistry.class.getName());
    //get the service
    metacardTypeRegistry = (MetacardTypeRegistry) bc.getService(serviceReference2);
    try {

      metacardTypeRegistry.register(new CRSMetacardType());
    } catch (Exception ex) {
      ex.printStackTrace();
      LOGGER.error(ex.getMessage());
    }
    //Service reference in order to populate our service
    ServiceReference serviceReference3 = bc.getServiceReference(CatalogFramework.class.getName());
    //get the service
    catalogFramework = (CatalogFramework) bc.getService(serviceReference3);
  }
  //endregion

  //region Federated Source Methods

  /**
   * Retrieve resource resource response.
   *
   * @param uri the uri
   * @param stringSerializableMap the string serializable map
   * @return the resource response
   * @throws IOException the io exception
   * @throws ResourceNotFoundException the resource not found exception
   * @throws ResourceNotSupportedException the resource not supported exception
   */
  public ResourceResponse retrieveResource(URI uri, Map<String, Serializable> stringSerializableMap)
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
    return null;
  }

  /**
   * Gets supported schemes.
   *
   * @return the supported schemes
   */
  public Set<String> getSupportedSchemes() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("ENTERING getSupportedSchemes");
    }
    return Collections.emptySet();
  }

  /**
   * Gets options.
   *
   * @param metacard the metacard
   * @return the options
   */
  public Set<String> getOptions(Metacard metacard) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "OpenSearch Source \"{}\" does not support resource retrieval options.",
          this.ssShortName);
    }
    return Collections.emptySet();
  }

  /**
   * Is available boolean.
   *
   * @return the boolean
   */
  public boolean isAvailable() {
    return true;
  }

  /**
   * Is available boolean.
   *
   * @param sourceMonitor the source monitor
   * @return the boolean
   */
  public boolean isAvailable(SourceMonitor sourceMonitor) {
    return true;
  }

  /**
   * Query source response.
   *
   * @param queryRequest the query request
   * @return the source response
   * @throws UnsupportedQueryException the unsupported query exception
   */
  public SourceResponse query(QueryRequest queryRequest) throws UnsupportedQueryException {
    try {
      LOGGER.debug("*******Entering Federated Source Query********");

      Serializable metacardId = queryRequest.getPropertyValue(Metacard.ID);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("METACARDID: " + metacardId);
      }
      Query query = queryRequest.getQuery();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Query = " + query);
      }
      CRSFilterVisitor visitor = new CRSFilterVisitor();
      query.accept(visitor, null);
      TemporalFilter temporalFilter = visitor.getTemporalSearch();
      SpatialFilter spatialFilter = visitor.getSpatialSearch();
      long elapsed = System.currentTimeMillis();
      String searchPhrase = visitor.getSearchPhrase();
      ArrayList<CRSResponse> operationsCRSResponseReports = null;
      List<String> searchPhrases = getSearchClausesFromQueryRequest(queryRequest, searchPhrase);

      if (mode == SAX) {
        LOGGER.debug("SAX mode enabled entering queryWithSax ");
      } else if (mode == REST) {
        LOGGER.debug("REST mode enabled entering queryWithParams ");
        List<CRSResponse> phraseResponse = null;
        operationsCRSResponseReports = new ArrayList<>();
        for(String seachPhrase : searchPhrases){
          phraseResponse =  queryWithParams(query, seachPhrase, temporalFilter, spatialFilter);
          operationsCRSResponseReports.addAll(phraseResponse);
        }
       /* operationsCRSResponseReports =
            queryWithParams(query, searchPhrase, temporalFilter, spatialFilter);*/
        LOGGER.debug("REST :: exiting queryWithParams ");
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Received query: " + query);
      }
      List<Result> results = createResultList(operationsCRSResponseReports);
      elapsed = System.currentTimeMillis() - elapsed;
      LOGGER.debug(
          "query returning " + results.size() + " results in " + elapsed + " milliseconds");
      SourceResponseImpl response = new SourceResponseImpl(queryRequest, results);
      response.setHits(results.size());
      return response;
    } catch (Throwable t) {
      LOGGER.warn("Unable to query source: {}", t);
    }
    LOGGER.info("Returning null for configured source");
    return null;
  }
  /**
   * Gets the OR clauses stored within the query request object if there is any. Takes the search
   * parameter from the query and appends it to the same list.
   *
   * @param queryRequest the query request
   * @param searchPhrase the search phrase
   * @return the search clauses from query request
   */
  public ArrayList<String> getSearchClausesFromQueryRequest(
          QueryRequest queryRequest, String searchPhrase) {

    ArrayList<String> searchClauses = new ArrayList<>();
    if (searchPhrase != null) {
      searchClauses.add(searchPhrase);
    }
    String[] orClausesList = null;
    if (queryRequest.getProperties() != null
            && queryRequest.getProperties().get(OR_CLAUSES_FOR_QUERY) != null) {
      orClausesList = (String[]) queryRequest.getProperties().get(OR_CLAUSES_FOR_QUERY);
      searchClauses.addAll(Arrays.asList(orClausesList));
    }
    return searchClauses;
  }
  /**
   * Gets content types.
   *
   * @return the content types
   */
  /** {@inheritDoc} */
  @Override
  public Set<ContentType> getContentTypes() {
    if (this.contentTypeName != null) {
      if (!this.contentTypeName.equalsIgnoreCase("")) {
        Set<ContentType> contentTypes = new HashSet<ContentType>();
        contentTypes.add(new ContentTypeImpl(this.contentTypeName, getVersion()));
        this.contentTypes = contentTypes;
        return contentTypes;
      } else {
        this.contentTypes = new HashSet<ContentType>();
        return contentTypes;
      }
    } else {
      this.contentTypes = new HashSet<ContentType>();
      return contentTypes;
    }
  }

  /**
   * Gets version.
   *
   * @return the version
   */
  public String getVersion() {
    if (LOGGER.isDebugEnabled()) {
      //  LOGGER.debug("Returning version:" + version);
    }
    return version;
  }

  /**
   * Sets version.
   *
   * @param v the v
   */
  public void setVersion(String v) {
    version = v;
  }

  /**
   * Gets id.
   *
   * @return the id
   */
  public String getId() {

    return ssShortName;
  }

  /**
   * Gets title.
   *
   * @return the title
   */
  public String getTitle() {
    if (LOGGER.isDebugEnabled()) {
      //   LOGGER.debug("Returning tite:" + title);
    }
    return ssShortName;
  }

  /**
   * Gets ssDescription.
   *
   * @return the ssDescription
   */
  public String getDescription() {
    if (LOGGER.isDebugEnabled()) {
      //  LOGGER.debug("Returning ssDescription: " + ssDescription);
    }
    return ssDescription;
  }

  /**
   * Gets organization.
   *
   * @return the organization
   */
  public String getOrganization() {
    if (LOGGER.isDebugEnabled()) {
      //  LOGGER.debug("Returning org:" + org);
    }
    return org;
  }
  //endregion

  /**
   * Sets id.
   *
   * @param id the id
   */
  public void setId(String id) {
    this.sourceId = id;
  }

  //region Http Query
  protected ArrayList<CRSResponse> queryWithParams(
      Query query,
      String contextualSearch,
      TemporalFilter temporalFilter,
      SpatialFilter spatialFilter) {
    if (restGetService == null) {

      restGetService = new RestGetService(this, mode, client);
      LOGGER.debug("Created RestGetService with BaseUrl of {}", ssServiceUrl);
    }

    LOGGER.debug("Inside Query With Params");
    List<CRSResponse> results = new ArrayList<CRSResponse>();
    results =
        getRestResults(
            (ArrayList<CRSResponse>) results,
            restGetService,
            query,
            contextualSearch,
            temporalFilter,
            spatialFilter);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.trace("queryTfr Returning " + results);
    }
    return (ArrayList<CRSResponse>) results;
  }

  /**
   * Create result list.
   *
   * @param CRSRespons the response objects
   * @return the list
   */
  private List<Result> createResultList(ArrayList<CRSResponse> CRSRespons) {

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("CREATING RESULT LIST");
    }
    List<Result> results = new ArrayList<Result>();
    try {

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Obtained " + CRSRespons.size() + " CRSResponse " + "Objects");
      }

      String hitTitle = null;
      for (CRSResponse fedSourceResponse : CRSRespons) {
        try {
          CRSMetacard metaCardData = new CRSMetacard();
          metaCardData.setSourceId(this.ssShortName);
          hitTitle = fedSourceResponse.getDisplayTitle();
          if (hitTitle == null || hitTitle.equals("")) {
            hitTitle = "Unknown";
          }
          Date responseObjectDate = null;
          if (fedSourceResponse.getDateOccurred() != null) {
            responseObjectDate = fedSourceResponse.getDateOccurred();
          }
          metaCardData.setId(fedSourceResponse.getDisplaySerial());

          String metadataString = null;
          if (mode == SAX) {
            metadataString =
                "<metadata>"
                    + StringEscapeUtils.escapeXml11(fedSourceResponse.getMetaData())
                    + "</metadata>";
            metaCardData.setMetadata(
                "<metadata>" + StringEscapeUtils.escapeXml11(metadataString) + "</metadata>");
          } else {

            //metadataString =
            //    "<metadata>"
            //        + StringEscapeUtils.escapeXml11(fedSourceResponse.getMetaData())
            //        + "</metadata>";
            LOGGER.trace(metadataString);

            //metaCardData.setMetadata(metadataString);
            //metaCardData.setMetadata(fedSourceResponse.getMetaData());
            metadataString =
                    "<metadata>"
                            + StringEscapeUtils.escapeXml11(fedSourceResponse.getMetaData())
                            + "</metadata>";
            metaCardData.setMetadata(metadataString);
          }
          Calendar c = Calendar.getInstance();
          long now = System.currentTimeMillis();
          c.setTimeInMillis(now);
          if (responseObjectDate != null) {
            metaCardData.setCreatedDate(responseObjectDate);
            metaCardData.setModifiedDate(responseObjectDate);
            metaCardData.setEffectiveDate(responseObjectDate);
          }

          Calendar expiration = Calendar.getInstance();
          expiration.setTimeInMillis(now + EXPIRATION_OFFSET);
          metaCardData.setExpirationDate(expiration.getTime());
          metaCardData.setTitle(hitTitle);

          if (contentTypeName != null) {
            metaCardData.setContentTypeName(contentTypeName);

          } else {
            metaCardData.setContentTypeName(DEFAULT_TYPE);
          }

          metaCardData.setContentTypeVersion(DEFAULT_TYPE_VERSION);
          if (fedSourceResponse.getClassification() != null) {
            metaCardData.setAttribute(
                CRSMetacardType.CLASSIFICATION, fedSourceResponse.getClassification());
          } else {
            metaCardData.setAttribute(CRSMetacardType.CLASSIFICATION, "Unknown");
          }
          if (fedSourceResponse.getSummary() != null) {
            metaCardData.setAttribute(CRSMetacardType.SUMMARY, fedSourceResponse.getSummary());
          } else {
            metaCardData.setAttribute(CRSMetacardType.SUMMARY, "No ssSummary");
          }
          if (fedSourceResponse.getLatitude() != null) {
            metaCardData.setAttribute(CRSMetacardType.LAT, fedSourceResponse.getLatitude());
          } else {
            metaCardData.setAttribute(CRSMetacardType.LAT, 0.0);
          }
          if (fedSourceResponse.getLongitude() != null) {
            metaCardData.setAttribute(CRSMetacardType.LON, fedSourceResponse.getLongitude());
          } else {
            metaCardData.setAttribute(CRSMetacardType.LON, 0.0);
          }
          if (fedSourceResponse.getOriginatorUnit() != null) {
            metaCardData.setAttribute(CRSMetacardType.UNIT, fedSourceResponse.getOriginatorUnit());
          } else {
            metaCardData.setAttribute(CRSMetacardType.UNIT, "Unknown Unit");
          }
          if (fedSourceResponse.getPrimaryEventType() != null) {
            metaCardData.setAttribute(
                CRSMetacardType.EVENT_TYPE, fedSourceResponse.getPrimaryEventType());
          } else {
            metaCardData.setAttribute(CRSMetacardType.EVENT_TYPE, "Unknown Event Type");
          }
          //  metaCardData.setResourceURI(new URI(CRSResponse.getSsReportLink()));
          if (fedSourceResponse.getReportLink() != null) {
            metaCardData.setAttribute(
                CRSMetacardType.REPORT_LINK, fedSourceResponse.getReportLink());
          } else {
            metaCardData.setAttribute(CRSMetacardType.REPORT_LINK, "No Supplied Link");
          }
          if (fedSourceResponse.getNiirs() != null) {
            metaCardData.setAttribute(CRSMetacardType.NIIRS_RATING, fedSourceResponse.getNiirs());
          } else {
            metaCardData.setAttribute(CRSMetacardType.NIIRS_RATING, 0);
          }
          if (fedSourceResponse.getLocation() != null) {
            metaCardData.setLocation(fedSourceResponse.getLocation());
          } else {
            if (fedSourceResponse.getLongitude() != 0.0 && fedSourceResponse.getLatitude() != 0.0) {
              metaCardData.setLocation(
                  "POINT ("
                      + Double.parseDouble(
                          decimalFormatter.format(fedSourceResponse.getLongitude()))
                      + " "
                      + Double.parseDouble(decimalFormatter.format(fedSourceResponse.getLatitude()))
                      + ")");
            }
          }

          if (LOGGER.isDebugEnabled()) {
            if (metaCardData.getId() != null) {
              LOGGER.trace("Id: " + metaCardData.getId());
            }
            if (metaCardData.getMetadata() != null) {
              LOGGER.trace("Metadata: " + metaCardData.getMetadata());
            }
            if (metaCardData.getContentTypeName() != null) {
              LOGGER.trace("ContentTypeName: " + metaCardData.getContentTypeName());
            }
            if (metaCardData.getContentTypeVersion() != null) {
              LOGGER.trace("ContentTypeVersion: " + metaCardData.getContentTypeVersion());
            }
            if (metaCardData.getTitle() != null) {
              LOGGER.trace("Title: " + metaCardData.getTitle());
            }
            if (metaCardData.getEffectiveDate() != null) {
              LOGGER.trace("Effective: " + metaCardData.getEffectiveDate().toString());
            }
            if (metaCardData.getCreatedDate() != null) {
              LOGGER.trace("Created: " + metaCardData.getCreatedDate().toString());
            }
            if (metaCardData.getModifiedDate() != null) {
              LOGGER.trace("Modified: " + metaCardData.getModifiedDate().toString());
            }
            if (metaCardData.getLocation() != null) {
              LOGGER.trace("Location: " + metaCardData.getLocation());
            }
            if (metaCardData.getAttribute(CRSMetacardType.SUMMARY) != null) {
              LOGGER.trace(
                  "Summary:" + metaCardData.getAttribute(CRSMetacardType.SUMMARY).toString());
            }
          }
          ResultImpl localEntry = new ResultImpl(metaCardData);
          localEntry.setRelevanceScore(Double.valueOf(1.0));
          localEntry.setDistanceInMeters(100.0);
          results.add(localEntry);
        } catch (Exception ex) {
          LOGGER.error("******Error Setting MetaCard Data****************\n" + ex.toString());
          ex.printStackTrace();
        }
      }
      if (results.size() > 0) {
        LOGGER.debug("Returning {} results", results.size());
      } else {
        LOGGER.debug("Returning 0 results");
      }
    } catch (Throwable t) {
      LOGGER.error("Error during metacard creation of results");
      t.printStackTrace();
    }
    return results;
  }
  //endregion

  //region Rest Results
  private ArrayList<CRSResponse> getRestResults(
      ArrayList<CRSResponse> results,
      SourceService service,
      Query query,
      String contextualSearch,
      TemporalFilter temporalFilter,
      SpatialFilter spatialFilter) {
    HttpUrl httpUrl = HttpUrl.parse(ssServiceUrl);

    HttpUrl.Builder httpBuilder = httpUrl.newBuilder();

    //Create array list to hold query params.
    //ArrayList<String> queryParams = new ArrayList<String>();
    String startDate = null;
    String endDate = null;
    StringBuilder topLeftLatLong = null;
    StringBuilder bottomRightLatLong = null;
    try {
      LOGGER.debug("Creating Filters");
      if (contextualSearch != null) {
        LOGGER.debug("Contextual Search = " + contextualSearch);
      }
      if (temporalFilter != null) {
        //sets the date to a specific format "yyyy-MM-dd'T'HH:mm:ssZ"
        startDate = SourceUtil.transformDateTfr(temporalFilter.getStartDate(), dateFormat);
        endDate = SourceUtil.transformDateTfr(temporalFilter.getEndDate(), dateFormat);

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("UI START: " + temporalFilter.getStartDate());
          LOGGER.debug(
              "UI START: "
                  + dateFormat.format(temporalFilter.getStartDate())
                  + "UI END: "
                  + dateFormat.format(temporalFilter.getEndDate()));
        }
        LOGGER.debug("START: " + startDate + " END: " + endDate);
      } else {
        //if temporal filter is null then do the exact same thing
        //NOTE changed this, was unnecessarily checking for null again
        LOGGER.info(
            "Temporal Filter was null setting start date to 1970 and end date to the current day and time");
        Calendar c = Calendar.getInstance();
        c.set(1970, 0, 1);
        startDate = dateFormat.format(c.getTime());
        c.setTimeInMillis(System.currentTimeMillis());
        endDate = dateFormat.format(c.getTime());
      }
      //Add variable to queryParams and append a &
      if (ssTemporalSearchParamStart != null
          && !ssTemporalSearchParamStart.equalsIgnoreCase("null")) {
        httpBuilder.addQueryParameter(ssTemporalSearchParamStart, startDate);
        //queryParams.add(ssTemporalSearchParamStart + "=" + startDate + "&");
      }
      if (ssTemporalSearchParamEnd != null && !ssTemporalSearchParamEnd.equalsIgnoreCase("null")) {
        httpBuilder.addQueryParameter(ssTemporalSearchParamEnd, endDate);
      }
      //if Spatial filter isn't null do all of this.
      if (spatialFilter != null) {
        //get the location string in Well known text
        String wktStr = spatialFilter.getGeometryWkt();
        //Print it to the log.
        LOGGER.trace("wktStr: " + wktStr);
        //if the spatial filter is a Distance Filter do all of this
        if (spatialFilter instanceof SpatialDistanceFilter) {
          try {
            //Create the distance filter
            SpatialDistanceFilter sdf = (SpatialDistanceFilter) spatialFilter;
            //get the geometry object
            Geometry geo = sdf.getGeometry();
            //print it
            LOGGER.trace("GEO: " + geo);
            //get the Point
            Point point = (Point) geo;
            //print it
            LOGGER.trace("POINT: " + point);
            //get the direstPosition
            DirectPosition dp = point.getDirectPosition();
            //print it
            LOGGER.trace("DirectPosition: " + dp);
            //create a coordinates array based on the direct position
            double[] coords = dp.getCoordinate();
            LOGGER.trace(
                "Creating bbox from "
                    + coords[0]
                    + ", "
                    + coords[1]
                    + ", "
                    + sdf.getDistanceInMeters());

            //createBBoxFromPointRadius = minX, minY, maxX, maxY
            double[] bboxCoords =
                SourceUtil.createBBoxFromPointRadius(
                    coords[0], coords[1], sdf.getDistanceInMeters());
            //create string builder for top left
            topLeftLatLong = new StringBuilder();
            //create string builder for bottom right
            bottomRightLatLong = new StringBuilder();
            //appending the Max X variable  //append Space   //then the Min X variable
            topLeftLatLong.append(bboxCoords[3]).append(" ").append(bboxCoords[0]);
            //appending the Max Y variable //append Space  //then the Min Y variable
            bottomRightLatLong.append(bboxCoords[1]).append(" ").append(bboxCoords[2]);
            //Add variable to queryParams and append a &
            if (ssSpatialSearchParamLat != null
                && !ssSpatialSearchParamLat.equalsIgnoreCase("null")) {
              httpBuilder.addQueryParameter(
                  ssSpatialSearchParamLat, String.valueOf(topLeftLatLong));
            }
            if (ssSpatialSearchParamLong != null
                && !ssSpatialSearchParamLong.equalsIgnoreCase("null")) {
              httpBuilder.addQueryParameter(
                  ssSpatialSearchParamLong, String.valueOf(bottomRightLatLong));
            }
          } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
          }
        }
        //if we are not a instance of Spatial Distance Filter
        else {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.trace("WKTSTR: " + wktStr);
          }
          if (wktStr.contains("POLYGON")) {
            String[] polyAry = SourceUtil.createPolyAryFromWKT(wktStr);
            topLeftLatLong = new StringBuilder();
            bottomRightLatLong = new StringBuilder();
            //creating array with bounding box
            //{minX, minY, maxX, maxY};
            double[] bboxCoords = SourceUtil.createBBoxFromPolygon(polyAry);
            //appending the Max X variable  //append Space   //then the Min X variable
            topLeftLatLong.append(bboxCoords[3]).append(" ").append(bboxCoords[0]);
            //appending the Max Y variable //append Space  //then the Min Y variable
            bottomRightLatLong.append(bboxCoords[1]).append(" ").append(bboxCoords[2]);
            if (ssSpatialSearchParamLat != null
                && !ssSpatialSearchParamLat.equalsIgnoreCase("null")) {
              httpBuilder.addQueryParameter(
                  ssSpatialSearchParamLat, String.valueOf(topLeftLatLong));
            }
            if (ssSpatialSearchParamLong != null
                && !ssSpatialSearchParamLong.equalsIgnoreCase("null")) {
              httpBuilder.addQueryParameter(
                  ssSpatialSearchParamLong, String.valueOf(bottomRightLatLong));
            }
          } else {
            LOGGER.trace("WKT ({}) not supported for SPATIAL search, use POLYGON.", wktStr);
          }
        }
      }
      if (ssContextSearchParam != null && !ssContextSearchParam.equalsIgnoreCase("null")) {
        httpBuilder.addQueryParameter(ssContextSearchParam, contextualSearch);
      }



      //Make the call to the service
      LOGGER.debug("CALLING getResultsForQuery(" + httpUrl.url().toString() + ")");

      results = service.getResultsForQuery(httpBuilder.build());
    } catch (Exception ex) {
      //LOGGER.warn(ex.getMessage());
      ex.printStackTrace();
    }
    if (results != null) {
      LOGGER.info("CRSSource  RETURNED {} results", results.size());
    }

    return results;
  }
  //endregion

  //region Spring Setters and Getters
  public String getSsDescription() {
    return ssDescription;
  }

  public void setSsDescription(String ssDescription) {
    LOGGER.debug("Spring setting variable ssDescription to {}", ssDescription);

    this.ssDescription = ssDescription;
  }

  /**
   * Gets ssShortName.
   *
   * @return the ssShortName
   */
  public String getSsShortName() {
    return ssShortName;
  }

  /**
   * Sets ssShortName.
   *
   * @param ssShortName the ssShortName
   */
  public void setSsShortName(String ssShortName) {
    LOGGER.debug("Spring setting variable ssShortName to {}", ssShortName);

    this.ssShortName = ssShortName;
  }

  public String getSsWktStringParam() {
    return ssWktStringParam;
  }

  public void setSsWktStringParam(String ssWktStringParam) {
    LOGGER.debug("Spring setting variable ssWktStringParam to {}", ssWktStringParam);

    this.ssWktStringParam = ssWktStringParam;
  }

  public String getSsClientCertPath() {
    return ssClientCertPath;
  }

  public void setSsClientCertPath(String ssClientCertPath) {
    LOGGER.debug("Spring setting variable ssClientCertPath to {}", ssClientCertPath);

    this.ssClientCertPath = ssClientCertPath;

    if (ssClientCertPassword != null && !ssClientCertPassword.trim().equalsIgnoreCase("")) {
      if (ssClientCertPath != null && !ssClientCertPath.equalsIgnoreCase("")) {
        if (client != null) {
          client = null;
        }
        client =
            new TrustingOkHttpClient()
                .getUnsafeOkHttpClient(15, 15, getSsClientCertPath(), getSsClientCertPassword());
      }
    }
  }

  public String getSsClientCertPassword() {
    return ssClientCertPassword;
  }

  public void setSsClientCertPassword(String ssClientCertPassword) {
    LOGGER.debug("Spring setting variable ssClientCertPassword to {}", ssClientCertPassword);
    this.ssClientCertPassword = ssClientCertPassword;
    if (ssClientCertPassword != null && !ssClientCertPassword.trim().equalsIgnoreCase("")) {
      if (ssClientCertPath != null && !ssClientCertPath.equalsIgnoreCase("")) {
        if (client != null) {
          client = null;
        }
        client =
            new TrustingOkHttpClient()
                .getUnsafeOkHttpClient(15, 15, getSsClientCertPath(), getSsClientCertPassword());
      }
    }
  }

  public String getContentTypeName() {
    return contentTypeName;
  }

  public void setContentTypeName(String contentTypeName) {
    this.contentTypeName = contentTypeName;
  }

  public String getSsContainerPath() {
    return ssContainerPath;
  }

  public void setSsContainerPath(String ssContainerPath) {
    LOGGER.debug("Spring setting variable ssContainerPath to {}", ssContainerPath);

    this.ssContainerPath = ssContainerPath;
  }

  public String getSsContextSearchParam() {
    return ssContextSearchParam;
  }

  public void setSsContextSearchParam(String ssContextSearchParam) {
    LOGGER.debug("Spring setting variable ssContextSearchParam to {}", ssContextSearchParam);

    this.ssContextSearchParam = ssContextSearchParam;
  }

  public String getSsSpatialSearchParamLat() {
    return ssSpatialSearchParamLat;
  }

  public void setSsSpatialSearchParamLat(String ssSpatialSearchParamLat) {
    LOGGER.debug("Spring setting variable ssSpatialSearchParamLat to {}", ssSpatialSearchParamLat);

    this.ssSpatialSearchParamLat = ssSpatialSearchParamLat;
  }

  public String getSsSpatialSearchParamLong() {
    return ssSpatialSearchParamLong;
  }

  public void setSsSpatialSearchParamLong(String ssSpatialSearchParamLong) {
    LOGGER.debug(
        "Spring setting variable ssSpatialSearchParamLong to {}", ssSpatialSearchParamLong);

    this.ssSpatialSearchParamLong = ssSpatialSearchParamLong;
  }

  public String getSsTemporalSearchParamEnd() {
    return ssTemporalSearchParamEnd;
  }

  public void setSsTemporalSearchParamEnd(String ssTemporalSearchParamEnd) {
    LOGGER.debug(
        "Spring setting variable ssTemporalSearchParamEnd to {}", ssTemporalSearchParamEnd);

    this.ssTemporalSearchParamEnd = ssTemporalSearchParamEnd;
  }

  public String getSsTemporalSearchParamStart() {
    return ssTemporalSearchParamStart;
  }

  public void setSsTemporalSearchParamStart(String ssTemporalSearchParamStart) {
    LOGGER.debug(
        "Spring setting variable ssTemporalSearchParamStart to {}", ssTemporalSearchParamStart);

    this.ssTemporalSearchParamStart = ssTemporalSearchParamStart;
  }

  public String getSsReportLink() {
    return ssReportLink;
  }

  public void setSsReportLink(String ssReportLink) {
    LOGGER.debug("Spring setting variable ssReportLink to {}", ssReportLink);

    this.ssReportLink = ssReportLink;
  }

  public String getSsDisplayTitle() {
    return ssDisplayTitle;
  }

  public void setSsDisplayTitle(String ssDisplayTitle) {
    LOGGER.debug("Spring setting variable ssDisplayTitle to {}", ssDisplayTitle);

    this.ssDisplayTitle = ssDisplayTitle;
  }

  public String getSsDisplaySerial() {
    return ssDisplaySerial;
  }

  public void setSsDisplaySerial(String ssDisplaySerial) {
    LOGGER.debug("Spring setting variable ssDisplaySerial to {}", ssDisplaySerial);

    this.ssDisplaySerial = ssDisplaySerial;
  }

  public String getSsSummary() {
    return ssSummary;
  }

  public void setSsSummary(String ssSummary) {
    LOGGER.debug("Spring setting variable ssSummary to {}", ssSummary);

    this.ssSummary = ssSummary;
  }

  public String getSsOriginatorUnit() {
    return ssOriginatorUnit;
  }

  public void setSsOriginatorUnit(String ssOriginatorUnit) {
    LOGGER.debug("Spring setting variable ssOriginatorUnit to {}", ssOriginatorUnit);

    this.ssOriginatorUnit = ssOriginatorUnit;
  }

  public String getSsPrimaryEventType() {
    return ssPrimaryEventType;
  }

  public void setSsPrimaryEventType(String ssPrimaryEventType) {
    LOGGER.debug("Spring setting variable ssPrimaryEventType to {}", ssPrimaryEventType);

    this.ssPrimaryEventType = ssPrimaryEventType;
  }

  public String getSsClassification() {
    return ssClassification;
  }

  public void setSsClassification(String ssClassification) {
    LOGGER.debug("Spring setting variable ssClassification to {}", ssClassification);

    this.ssClassification = ssClassification;
  }

  public String getSsDateOccurred() {
    return ssDateOccurred;
  }

  public void setSsDateOccurred(String ssDateOccurred) {
    LOGGER.debug("Spring setting variable ssDateOccurred to {}", ssDateOccurred);

    this.ssDateOccurred = ssDateOccurred;
  }

  public String getSsLatitude() {
    return ssLatitude;
  }

  public void setSsLatitude(String ssLatitude) {
    LOGGER.debug("Spring setting variable ssLatitude to {}", ssLatitude);

    this.ssLatitude = ssLatitude;
  }

  public String getSsLongitude() {
    return ssLongitude;
  }

  public void setSsLongitude(String ssLongitude) {
    LOGGER.debug("Spring setting variable ssLongitude to {}", ssLongitude);

    this.ssLongitude = ssLongitude;
  }

  public String getSsServiceUrl() {
    return ssServiceUrl;
  }

  public void setSsServiceUrl(String ssServiceUrl) {
    LOGGER.debug("Spring setting variable ssServiceUrl to {}", ssServiceUrl);

    this.ssServiceUrl = ssServiceUrl;
  }

  public String getSsNiirs() {
    return this.ssNiirs;
  }

  public void setSsNiirs(String ssNiirs) {
    LOGGER.debug("Spring setting variable ssNiirs to {}", ssNiirs);
    this.ssNiirs = ssNiirs;
  }

  //endregion
}
