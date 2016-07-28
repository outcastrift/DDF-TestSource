package test.query;

import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.temporal.AfterImpl;
import org.geotools.filter.temporal.BeforeImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.junit.Assume;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import static org.junit.Assert.assertEquals;

/**
 * This software was created for All rights to this software belong to appropriate licenses and
 * restrictions apply. Created by Samuel Davis on 6/28/16.
 * Class Description
 */
public class TemporalQueryTest extends BaseQueryTest {
    @Test
    public void testQueryWithTemporal() throws CQLException, IOException, UnsupportedQueryException {

        Query testQuery = new QueryImpl(timeSourceFilter);
        QueryRequest testRequest = new QueryRequestImpl(testQuery);


    }
}
