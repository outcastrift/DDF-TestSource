package test;


import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
public class BaseRestTest {

  protected final static String GET_SOURCE_RESULTS = "getSourceResults";
  protected static final String GET_GROOVY_RESULTS = "getGroovyResults";
  protected static final String SERVICE_ADRESS = "https://localhost:8993/services/test/";
  private static final Logger logger = LoggerFactory.getLogger(BaseRestTest.class);
  private final static String WADL_ADDRESS = GET_SOURCE_RESULTS + "?_wadl";
  protected static boolean serverUp = false;

  @BeforeClass
  public static void init() {
    try{

    }catch(Exception e){

    }
  }
}
