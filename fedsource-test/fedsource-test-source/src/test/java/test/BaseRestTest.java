package test;


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
  protected static boolean serverUp = false;
  protected static HashMap<String, String> springVars;
  @BeforeClass
  public static void init() {
   springVars.put("ssShortName",);
    springVars.put("ssClassification",);
    springVars.put("ssDateOccurred",);
    springVars.put("ssDisplaySerial",);
    springVars.put("ssLatitude",);
    springVars.put("ssLongitude",);
    springVars.put("ssDescription",);
   springVars.put("ssContainerPath",);
    springVars.put("ssContextSearchParam",);
    springVars.put("ssServiceUrl",);
    springVars.put("ssSpatialSearchParamLat",);
    springVars.put("ssSpatialSearchParamLong",);
    springVars.put("ssSummary",);
   springVars.put("ssTemporalSearchParamEnd",);
    springVars.put("ssTemporalSearchParamStart",);
    springVars.put("ssOriginatorUnit",);
    springVars.put("ssPrimaryEventType",);
    springVars.put("ssReportLink",);
    springVars.put("ssDisplayTitle","SamTest");
    springVars.put("ssClientCertPath",null);
    springVars.put("ssClientCertPassword",null);


    try{

    }catch(Exception e){

    }
  }
}
