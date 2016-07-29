package test.query;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.junit.Before;
import org.opengis.filter.Filter;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.UnsupportedQueryException;
import test.BaseRestTest;

/**
 * This software was created for All rights to this software belong to appropriate licenses and
 * restrictions apply. Created by Samuel Davis on 6/22/16. Class Description
 */
public class BaseQueryTest extends BaseRestTest {

  public static Filter statesGeoFilter;
  public static Filter haitiGeoFilter;
  public static Filter timeSourceFilter;
  public static Filter statesContextualFilter;
  public static Filter floridaAndSmallPieceOfHaitiFilter;

  public static Gson gson;

  
  @Before
  public void setup() throws CQLException, ParseException {
    statesGeoFilter = CQL.toFilter("INTERSECTS(anyGeo, POLYGON ((-127.96874999999997 24.52713482259776," + " " +
            "-127.96874999999997 51.17934297928926," + " -57.65625000000003 51.17934297928926," + " " +
            "-57.65625000000003 24.52713482259776," + " -127.96874999999997 24.52713482259776)))");
    haitiGeoFilter = CQL.toFilter("INTERSECTS(anyGeo, POLYGON ((-80.41992187499999 15.36894989653473," + " " +
            "-80.41992187499999 23.241346102386146," + " -64.07226562499999 23.241346102386146," + " " +
            "-64.07226562499999 15.36894989653473," + " -80.41992187499999 15.36894989653473)))");
    floridaAndSmallPieceOfHaitiFilter = CQL.toFilter("INTERSECTS(anyGeo, POLYGON ((-87.23144531249999 " +
            "19.47695020648844, -87.23144531249999 28.033197847676362," + " -68.642578125 28.033197847676362, " +
            "-68.642578125 19.47695020648844, -87.23144531249999 19.47695020648844)))");
    timeSourceFilter = CQL.toFilter("INTERSECTS(anyGeo , POLYGON((-180 -90, -180 90, 180 90, -180 -90)))");
    statesContextualFilter = CQL.toFilter("anyText LIKE '15'");
    gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().create();

  }

  public SourceResponse genResultsForSources(FederatedSource source, QueryRequest queryRequest, String
          outputFilename) throws UnsupportedQueryException, IOException {
    ArrayList<HashMap<String, String>> resultList = new ArrayList<HashMap<String, String>>();
    SourceResponse s = source.query(queryRequest);
    return s;
  }


}
