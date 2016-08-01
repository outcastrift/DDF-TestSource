package com.davis.ddf.test.fedSource;

import com.davis.ddf.test.client.TrustingOkHttpClient;
import com.davis.ddf.test.fedSource.datamodel.UniversalFederatedSourceResponse;
import com.davis.ddf.test.fedSource.datamodel.metacards.UniversalFederatedSourceMetacard;
import com.davis.ddf.test.fedSource.datamodel.metacards.UniversalFederatedSourceMetacardType;
import com.davis.ddf.test.service.SourceService;
import com.davis.ddf.test.service.restService.RestGetService;

import org.apache.commons.lang3.StringEscapeUtils;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Geometry;
import org.opengis.geometry.primitive.Point;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardTypeRegistry;
import ddf.catalog.data.Result;
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
import ddf.catalog.source.opensearch.ContextualSearch;
import ddf.catalog.source.opensearch.OpenSearchSiteUtil;
import ddf.catalog.util.Describable;
import ddf.mime.MimeTypeMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class UniversalFederatedSource implements ddf.catalog.source.FederatedSource, Describable {

  private static final Logger LOGGER = LoggerFactory.getLogger(UniversalFederatedSource.class);
  private static final String DEFAULT_TYPE = UniversalFederatedSourceMetacardType.NAME;
  private static final long EXPIRATION_OFFSET = 3600000;
  private static int SAX = 1;
  private static int REST = 2;
  public String ssClientCertPath;
  public String ssClientCertPassword;
  /**
   * Spring set variables
   **/
  private String ssWktStringParam;
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

  /**
   * End Spring set variables
   **/
  private NumberFormat decimalFormatter = new DecimalFormat("#0.0000");
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

  /**
   * Test Constructor
   **/
  public UniversalFederatedSource(HashMap<String, String> springVars) {
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
  }

  /**
   * Instantiates a new Test federated source.
   *
   * @throws IngestException the ingest exception
   */
  public UniversalFederatedSource() {
    mode = REST;
    setupOSGIServices();
    try {
      String dateFormatPattern = "yyyy-MM-dd'T'HH:mm:ssZ";
      dateFormat = new SimpleDateFormat(dateFormatPattern);
      isAvailable = true;
      LOGGER.info("Successfully created UniversalFederatedSource");

    } catch (Exception ex) {
      LOGGER.error("Error  = {}",ex);
    }


  }

  private void setupOSGIServices() {
    BundleContext bc = FrameworkUtil.getBundle(UniversalFederatedSource.class).getBundleContext();
    //Service reference in order to populate our service
    ServiceReference serviceReference = bc.getServiceReference(MimeTypeMapper.class.getName());
    //get the service
    mimeTypeMapper = (MimeTypeMapper) bc.getService(serviceReference);

    //Service reference in order to populate our service
    ServiceReference serviceReference2 = bc.getServiceReference(MetacardTypeRegistry.class.getName());
    //get the service
    metacardTypeRegistry = (MetacardTypeRegistry) bc.getService(serviceReference2);
    try {

      metacardTypeRegistry.register(new UniversalFederatedSourceMetacardType());
    } catch (Exception ex) {
      ex.printStackTrace();
      LOGGER.error(ex.getMessage());
    }
    //Service reference in order to populate our service
    ServiceReference serviceReference3 = bc.getServiceReference(CatalogFramework.class.getName());
    //get the service
    catalogFramework = (CatalogFramework) bc.getService(serviceReference3);
  }


  /**
   * Retrieve resource resource response.
   *
   * @param uri                   the uri
   * @param stringSerializableMap the string serializable map
   * @return the resource response
   * @throws IOException                   the io exception
   * @throws ResourceNotFoundException     the resource not found exception
   * @throws ResourceNotSupportedException the resource not supported exception
   */
  public ResourceResponse retrieveResource(URI uri, Map<String, Serializable> stringSerializableMap) throws
          IOException, ResourceNotFoundException, ResourceNotSupportedException {
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
      LOGGER.debug("OpenSearch Source \"{}\" does not support resource retrieval options.", this.ssShortName);
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
      List<Result> results = new ArrayList<Result>();
      LOGGER.debug("*******Entering Federated Source Query********");
      if (LOGGER.isDebugEnabled()) {

        Map<String, Serializable> props = queryRequest.getProperties();
        Iterator<String> keys = props.keySet().iterator();
        while (keys.hasNext()) {
          String key = keys.next();
          LOGGER.debug(key + ", " + props.get(key));
        }
      }
      Serializable metacardId = queryRequest.getPropertyValue(Metacard.ID);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("METACARDID: " + metacardId);
      }
      Query query = queryRequest.getQuery();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Query = " + query);
      }
      UniversalSourceFilterVisitor visitor = new UniversalSourceFilterVisitor();
      query.accept(visitor, null);
      ContextualSearch contextualFilter = visitor.getContextualSearch();
      TemporalFilter temporalFilter = visitor.getTemporalSearch();
      SpatialFilter spatialFilter = visitor.getSpatialSearch();
      long elapsed = System.currentTimeMillis();

      ArrayList<UniversalFederatedSourceResponse> operationsUniversalFederatedSourceResponseReports = null;
      if (mode == SAX) {
        LOGGER.debug("SAX mode enabled entering queryWithSax ");
      } else if (mode == REST) {
        LOGGER.debug("REST mode enabled entering queryWithParams ");
        operationsUniversalFederatedSourceResponseReports = queryWithParams(query, contextualFilter, temporalFilter,
                spatialFilter);
        LOGGER.debug("REST :: exiting queryWithParams ");
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Received query: " + query);
      }
      results = createResultList(operationsUniversalFederatedSourceResponseReports);
      elapsed = System.currentTimeMillis() - elapsed;
      LOGGER.debug("query returning " + results.size() + " results in " + elapsed + " milliseconds");
      SourceResponseImpl response = new SourceResponseImpl(queryRequest, results);
      response.setHits(results.size());
      return response;
    } catch (Throwable t) {
      LOGGER.warn("Unable to query source: {}", t);
    }
    LOGGER.info("Returning null for configured source");
    return null;
  }




  protected ArrayList<UniversalFederatedSourceResponse> queryWithParams(Query query, ContextualSearch
          contextualSearch, TemporalFilter temporalFilter, SpatialFilter spatialFilter) {
    if (restGetService == null) {

      restGetService = new RestGetService(this, mode,client);
      LOGGER.debug("Created RestGetService with BaseUrl of {}", ssServiceUrl);
    }

    LOGGER.debug("Inside Query With Params");
    List<UniversalFederatedSourceResponse> results = new ArrayList<UniversalFederatedSourceResponse>();
    results = getRestResults((ArrayList<UniversalFederatedSourceResponse>) results, restGetService, query,
            contextualSearch, temporalFilter, spatialFilter);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("queryTfr Returning " + results);
    }
    return (ArrayList<UniversalFederatedSourceResponse>) results;
  }

  /**
   * Create result list.
   *
   * @param universalFederatedSourceResponses the response objects
   * @return the list
   */
  private List<Result> createResultList(ArrayList<UniversalFederatedSourceResponse> universalFederatedSourceResponses) {

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("CREATING RESULT LIST");
    }
    List<Result> results = new ArrayList<Result>();
    try {

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Obtained " + universalFederatedSourceResponses.size() + " UniversalFederatedSourceResponse " +
                "Objects");
      }

      String hitTitle = null;
      for (UniversalFederatedSourceResponse fedSourceResponse : universalFederatedSourceResponses) {
        try {
          UniversalFederatedSourceMetacard metaCardData = new UniversalFederatedSourceMetacard();
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
            metadataString = "<metadata>" + StringEscapeUtils.escapeXml11(fedSourceResponse.getMetaData()) +
                    "</metadata>";
            metaCardData.setMetadata("<metadata>" + StringEscapeUtils.escapeXml11(metadataString) + "</metadata>");
          } else {

            metadataString = "<metadata>" + StringEscapeUtils.escapeXml11(fedSourceResponse.getMetaData()) +
                    "</metadata>";
            LOGGER.trace(metadataString);
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

          metaCardData.setContentTypeName(DEFAULT_TYPE);
          metaCardData.setContentTypeVersion(DEFAULT_TYPE_VERSION);
          if (fedSourceResponse.getClassification() != null) {
            metaCardData.setAttribute(UniversalFederatedSourceMetacardType.CLASSIFICATION, fedSourceResponse
                    .getClassification());
          } else {
            metaCardData.setAttribute(UniversalFederatedSourceMetacardType.CLASSIFICATION, "Unknown");
          }
          if (fedSourceResponse.getSummary() != null) {
            metaCardData.setAttribute(UniversalFederatedSourceMetacardType.SUMMARY, fedSourceResponse.getSummary());
          } else {
            metaCardData.setAttribute(UniversalFederatedSourceMetacardType.SUMMARY, "No ssSummary");
          }
          if (fedSourceResponse.getLatitude() != null) {
            metaCardData.setAttribute(UniversalFederatedSourceMetacardType.LAT, fedSourceResponse.getLatitude());
          } else {
            metaCardData.setAttribute(UniversalFederatedSourceMetacardType.LAT, 0.0);
          }
          if (fedSourceResponse.getLongitude() != null) {
            metaCardData.setAttribute(UniversalFederatedSourceMetacardType.LON, fedSourceResponse.getLongitude());
          } else {
            metaCardData.setAttribute(UniversalFederatedSourceMetacardType.LON, 0.0);
          }
          if (fedSourceResponse.getOriginatorUnit() != null) {
            metaCardData.setAttribute(UniversalFederatedSourceMetacardType.UNIT, fedSourceResponse.getOriginatorUnit());
          } else {
            metaCardData.setAttribute(UniversalFederatedSourceMetacardType.UNIT, "Unknown Unit");
          }
          if (fedSourceResponse.getPrimaryEventType() != null) {
            metaCardData.setAttribute(UniversalFederatedSourceMetacardType.EVENT_TYPE, fedSourceResponse
                    .getPrimaryEventType());
          } else {
            metaCardData.setAttribute(UniversalFederatedSourceMetacardType.EVENT_TYPE, "Unknown Event Type");
          }
          //  metaCardData.setResourceURI(new URI(UniversalFederatedSourceResponse.getSsReportLink()));
          if (fedSourceResponse.getReportLink() != null) {
            metaCardData.setAttribute(UniversalFederatedSourceMetacardType.REPORT_LINK, fedSourceResponse
                    .getReportLink());
          } else {
            metaCardData.setAttribute(UniversalFederatedSourceMetacardType.REPORT_LINK, "No Supplied Link");
          }
          if (fedSourceResponse.getLocation() != null) {
            metaCardData.setLocation(fedSourceResponse.getLocation());
          } else {
            if (fedSourceResponse.getLongitude() != 0.0 && fedSourceResponse.getLatitude() != 0.0) {
              metaCardData.setLocation("POINT (" + Double.parseDouble(decimalFormatter.format(fedSourceResponse
                      .getLongitude())) + " " + Double.parseDouble(decimalFormatter.format(fedSourceResponse
                      .getLatitude())) + ")");
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
            if (metaCardData.getAttribute(UniversalFederatedSourceMetacardType.SUMMARY) != null) {
              LOGGER.trace("Summary:" + metaCardData.getAttribute(UniversalFederatedSourceMetacardType.SUMMARY)
                      .toString());
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

  private ArrayList<UniversalFederatedSourceResponse> getRestResults(ArrayList<UniversalFederatedSourceResponse>
                                                                             results, SourceService service, Query
          query, ContextualSearch contextualSearch, TemporalFilter temporalFilter, SpatialFilter spatialFilter) {
    HttpUrl httpUrl = HttpUrl.parse(ssServiceUrl);

    HttpUrl.Builder httpBuilder = httpUrl.newBuilder();

    //Create array list to hold query params.
    //ArrayList<String> queryParams = new ArrayList<String>();
    String searchParams = null;
    String startDate = null;
    String endDate = null;
    StringBuilder topLeftLatLong = null;
    StringBuilder bottomRightLatLong = null;
    try {
      LOGGER.debug("Creating Filters");
      if (contextualSearch != null) {
        LOGGER.debug("Contextual Search = " + contextualSearch.getSelectors() + " search phrase " + contextualSearch
                .getSearchPhrase());
        searchParams = contextualSearch.getSearchPhrase();
      }
      if (temporalFilter != null) {
        //sets the date to a specific format "yyyy-MM-dd'T'HH:mm:ssZ"
        startDate = transformDateTfr(temporalFilter.getStartDate(), dateFormat);
        endDate = transformDateTfr(temporalFilter.getEndDate(), dateFormat);

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("UI START: " + temporalFilter.getStartDate());
          LOGGER.debug("UI START: " + dateFormat.format(temporalFilter.getStartDate()) + "UI END: " + dateFormat
                  .format(temporalFilter.getEndDate()));
        }
        LOGGER.debug("START: " + startDate + " END: " + endDate);
      } else {
        //if temporal filter is null then do the exact same thing
        //NOTE changed this, was unnecessarily checking for null again
        LOGGER.info("Temporal Filter was null setting start date to 1970 and end date to the current day and time");
        Calendar c = Calendar.getInstance();
        c.set(1970, 0, 1);
        startDate = dateFormat.format(c.getTime());
        c.setTimeInMillis(System.currentTimeMillis());
        endDate = dateFormat.format(c.getTime());
      }
      //Add variable to queryParams and append a &
      if (ssTemporalSearchParamStart != null && !ssTemporalSearchParamStart.equalsIgnoreCase("null")) {
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
            LOGGER.trace("Creating bbox from " + coords[0] + ", " + coords[1] + ", " + sdf.getDistanceInMeters());


            //createBBoxFromPointRadius = minX, minY, maxX, maxY
            double[] bboxCoords = OpenSearchSiteUtil.createBBoxFromPointRadius(coords[0], coords[1], sdf
                    .getDistanceInMeters());
            //create string builder for top left
            topLeftLatLong = new StringBuilder();
            //create string builder for bottom right
            bottomRightLatLong = new StringBuilder();
            //appending the Max X variable  //append Space   //then the Min X variable
            topLeftLatLong.append(bboxCoords[3]).append(" ").append(bboxCoords[0]);
            //appending the Max Y variable //append Space  //then the Min Y variable
            bottomRightLatLong.append(bboxCoords[1]).append(" ").append(bboxCoords[2]);
            //Add variable to queryParams and append a &
            if (ssSpatialSearchParamLat != null && !ssSpatialSearchParamLat.equalsIgnoreCase("null")) {
              httpBuilder.addQueryParameter(ssSpatialSearchParamLat, String.valueOf(topLeftLatLong));
            }
            if (ssSpatialSearchParamLong != null && !ssSpatialSearchParamLong.equalsIgnoreCase("null")) {
              httpBuilder.addQueryParameter(ssSpatialSearchParamLong, String.valueOf(bottomRightLatLong));
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
            String[] polyAry = OpenSearchSiteUtil.createPolyAryFromWKT(wktStr);
            topLeftLatLong = new StringBuilder();
            bottomRightLatLong = new StringBuilder();
            //creating array with bounding box
            //{minX, minY, maxX, maxY};
            double[] bboxCoords = OpenSearchSiteUtil.createBBoxFromPolygon(polyAry);
            //appending the Max X variable  //append Space   //then the Min X variable
            topLeftLatLong.append(bboxCoords[3]).append(" ").append(bboxCoords[0]);
            //appending the Max Y variable //append Space  //then the Min Y variable
            bottomRightLatLong.append(bboxCoords[1]).append(" ").append(bboxCoords[2]);
            if (ssSpatialSearchParamLat != null && !ssSpatialSearchParamLat.equalsIgnoreCase("null")) {
              httpBuilder.addQueryParameter(ssSpatialSearchParamLat, String.valueOf(topLeftLatLong));
            }
            if (ssSpatialSearchParamLong != null && !ssSpatialSearchParamLong.equalsIgnoreCase("null")) {
              httpBuilder.addQueryParameter(ssSpatialSearchParamLong, String.valueOf(bottomRightLatLong));
            }
          } else {
            LOGGER.trace("WKT ({}) not supported for SPATIAL search, use POLYGON.", wktStr);
          }
        }
      }
      if (ssContextSearchParam != null && !ssContextSearchParam.equalsIgnoreCase("null")) {
        httpBuilder.addQueryParameter(ssContextSearchParam, searchParams);
      }

      //Make the call to the service
      LOGGER.debug("CALLING getResultsForQuery(" + httpUrl.url().toString() + ")");

      results = service.getResultsForQuery(httpBuilder.build());
    } catch (Exception ex) {
      //LOGGER.warn(ex.getMessage());
      ex.printStackTrace();
    }
    if (results != null) {
      LOGGER.info("UniversalFederatedSource  RETURNED {} results", results.size());
    }
    return results;
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

  /**
   * Gets content types.
   *
   * @return the content types
   */
  public Set<ContentType> getContentTypes() {

    this.contentTypes = new HashSet<ContentType>();

    return contentTypes;
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

  /**
   * Gets id.
   *
   * @return the id
   */
  public String getId() {

    return ssShortName;
  }

  /**
   * Sets id.
   *
   * @param id the id
   */
  public void setId(String id) {
    this.sourceId = id;
  }

  /**
   * Used by DDF to log from here
   */
  private void logElement(Element e) {
    LOGGER.info(e.getTagName() + " " + e.getNodeName() + ": " + e.getTextContent());
    NodeList nl = e.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      logNode(nl.item(i));
    }
  }

  /**
   * Used by DDF to log from here
   */
  private void logNode(Node n) {
    if (n instanceof Element) {
      logElement((Element) n);
    } else {
      LOGGER.info(n.getNodeName() + ":" + n.getNodeValue());
      NodeList nl = n.getChildNodes();
      for (int i = 0; i < nl.getLength(); i++) {
        logNode(nl.item(i));
      }
    }
  }

  public String getSsDescription() {
    return ssDescription;
  }

  public void setSsDescription(String ssDescription) {
    LOGGER.debug("Spring setting variable ssDescription to {}", ssDescription);

    this.ssDescription = ssDescription;
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

      if(ssClientCertPassword != null) {
        client = new TrustingOkHttpClient().getUnsafeOkHttpClient(15, 15, getSsClientCertPath(), getSsClientCertPassword());
      }

  }

  public String getSsClientCertPassword() {
    return ssClientCertPassword;
  }

  public void setSsClientCertPassword(String ssClientCertPassword) {
    LOGGER.debug("Spring setting variable ssClientCertPassword to {}", ssClientCertPassword);
    this.ssClientCertPassword = ssClientCertPassword;

      if(ssClientCertPath != null) {
        client = new TrustingOkHttpClient().getUnsafeOkHttpClient(15, 15, getSsClientCertPath(), getSsClientCertPassword());
      }

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
    LOGGER.debug("Spring setting variable ssSpatialSearchParamLong to {}", ssSpatialSearchParamLong);

    this.ssSpatialSearchParamLong = ssSpatialSearchParamLong;
  }

  public String getSsTemporalSearchParamEnd() {
    return ssTemporalSearchParamEnd;
  }

  public void setSsTemporalSearchParamEnd(String ssTemporalSearchParamEnd) {
    LOGGER.debug("Spring setting variable ssTemporalSearchParamEnd to {}", ssTemporalSearchParamEnd);

    this.ssTemporalSearchParamEnd = ssTemporalSearchParamEnd;
  }

  public String getSsTemporalSearchParamStart() {
    return ssTemporalSearchParamStart;
  }

  public void setSsTemporalSearchParamStart(String ssTemporalSearchParamStart) {
    LOGGER.debug("Spring setting variable ssTemporalSearchParamStart to {}", ssTemporalSearchParamStart);

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
}
