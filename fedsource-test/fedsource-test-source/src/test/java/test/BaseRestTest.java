package test;


import com.davis.ddf.test.fedSource.UniversalFederatedSource;

import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

@SuppressWarnings("deprecation")
public class BaseRestTest {

  protected final static String GET_SOURCE_RESULTS = "getSourceResults";
  protected static final String GET_GROOVY_RESULTS = "getGroovyResults";
  protected static final String SERVICE_ADRESS = "https://localhost:8993/services/test/";
  private static final Logger logger = LoggerFactory.getLogger(BaseRestTest.class);
  private final static String WADL_ADDRESS = GET_SOURCE_RESULTS + "?_wadl";
  protected static  UniversalFederatedSource universalFederatedSource;
  protected static boolean serverUp = false;
  protected static HashMap<String, String> springVars;
  @BeforeClass
  public static void init() {
    springVars = new HashMap<>();
    springVars.put("ssShortName","TestBedFederatedSource");
    springVars.put("ssClassification","$[*].classification");
    springVars.put("ssDateOccurred","$[*].dateOccurred");
    springVars.put("ssDisplaySerial","$[*].displaySerial");
    springVars.put("ssLatitude","$[*].latitude");
    springVars.put("ssLongitude","$[*].longitude");
    springVars.put("ssDescription","SamsSource");
    springVars.put("ssWktStringParam","$[*].location");
    springVars.put("ssContainerPath","$.data[*]");
    springVars.put("ssContextSearchParam","amount");
    springVars.put("ssServiceUrl","https://localhost:8993/services/test/getSourceResults");
    springVars.put("ssSpatialSearchParamLat","topLeftLatLong");
    springVars.put("ssSpatialSearchParamLong","bottomRightLatLong");
    springVars.put("ssSummary","$[*].summary");
    springVars.put("ssTemporalSearchParamEnd","endDate");
    springVars.put("ssTemporalSearchParamStart","startDate");
    springVars.put("ssOriginatorUnit","$[*].originatorUnit");
    springVars.put("ssPrimaryEventType","$[*].primaryEventType");
    springVars.put("ssReportLink","$[*].reportLink");
    springVars.put("ssDisplayTitle","$[*].displayTitle");
    springVars.put("ssClientCertPath",null);
    springVars.put("ssClientCertPassword",null);

    universalFederatedSource = new UniversalFederatedSource(springVars);

  }
}
