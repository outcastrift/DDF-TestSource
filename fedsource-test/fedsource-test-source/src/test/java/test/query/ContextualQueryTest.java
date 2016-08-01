package test.query;

import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.IOException;
import org.geotools.filter.text.cql2.CQLException;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * This software was created for All rights to this software belong to appropriate licenses and
 * restrictions apply. Created by Samuel Davis on 6/28/16. Class Description
 */
public class ContextualQueryTest extends BaseQueryTest {
    @Test
    public void testContextualQuery() throws CQLException, IOException, UnsupportedQueryException {


        Query stateQuery = new QueryImpl(contextualFilter);
        QueryRequest stateRequest = new QueryRequestImpl(stateQuery);
        SourceResponse sourceResponse = universalFederatedSource.query(stateRequest);

        assertTrue(sourceResponse.getHits() == 15);




    }
}
