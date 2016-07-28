package test.query;

import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.junit.Assume;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;

import static org.junit.Assert.assertTrue;

/**
 * This software was created for All rights to this software belong to appropriate licenses and
 * restrictions apply. Created by Samuel Davis on 6/25/16. Class Description
 */
public class TestMetadataQuery extends BaseQueryTest {
  public static Map<String, Serializable> properties;

  /**
   * Assuming a valid metadata request from the UI.
   ***/
  @Test
  public void testMetadataQuery() throws IOException, UnsupportedQueryException {

  }

  private String genRandom30CharUUID() {
    String result = UUID.randomUUID().toString();
    result = result.substring(0, 30);
    return result;
  }
}
