package com.davis.crs;

import com.davis.ddf.crs.SourceEndpoint;
import com.davis.ddf.crs.data.SequentialResponse;
import com.davis.ddf.crs.jsonapi.JsonApiResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This software was created for rights to this software belong to appropriate licenses and
 * restrictions apply.
 *
 * @author Samuel Davis created on 9/7/17.
 */
public class SequentialEndpointTest {
  private static final Logger log = LoggerFactory.getLogger(SequentialEndpointTest.class.getName());
  SourceEndpoint sourceEndpoint = new SourceEndpoint();

  @Test
  @Ignore
  public void testEndpoint() throws URISyntaxException {
    Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    Response firstResponse = sourceEndpoint.getResultsSequential(null, "5", null, null, null);

    String jsonString  = gson.toJson(firstResponse);
    JsonApiResponse jsonApiResponse =  gson.fromJson(jsonString, JsonApiResponse.class);
    String seqJson = gson.toJson(jsonApiResponse.getData());
    SequentialResponse sequentialResponse = gson.fromJson(seqJson, SequentialResponse.class);
    URI uri = new URI(sequentialResponse.getQueryUri());
    log.info("URI {}", uri);
    log.info("getPath {}", uri.getPath());
    log.info("getQuery {}", uri.getQuery());
    log.info("getHost {}", uri.getHost());
    log.info("getAuthority {}", uri.getAuthority());
    log.info("getFragment {}", uri.getFragment());
    log.info("getPort {}", uri.getPort());
    log.info("getRawAuthority {}", uri.getRawAuthority());
    log.info("getRawFragment {}", uri.getRawFragment());
    log.info("getRawSchemeSpecificPart {}", uri.getRawSchemeSpecificPart());
    log.info("getScheme {}", uri.getScheme());
    log.info("getSchemeSpecificPart {}", uri.getSchemeSpecificPart());
    log.info("getUserInfo {}", uri.getUserInfo());


    Assert.assertTrue(firstResponse != null);
  }
}
