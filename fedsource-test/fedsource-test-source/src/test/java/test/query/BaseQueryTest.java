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

  public static Filter contextualFilter;
  public static Gson gson;


  @Before
  public void setup() throws CQLException, ParseException {
    contextualFilter = CQL.toFilter("anyText LIKE '15'");
    gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().create();

  }

  public SourceResponse genResultsForSources(FederatedSource source, QueryRequest queryRequest, String
          outputFilename) throws UnsupportedQueryException, IOException {
    ArrayList<HashMap<String, String>> resultList = new ArrayList<HashMap<String, String>>();
    SourceResponse s = source.query(queryRequest);
    return s;
  }


}
