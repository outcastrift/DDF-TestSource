package com.davis.ddf.crs.service;

/**
 * This software was created for the Open Architecture Distributed Common Ground System
 * Modernization All rights to this software belong to  appropriate licenses and restrictions apply.
 * Created by Samuel Davis on 3/22/16.
 */


import com.davis.ddf.crs.client.TrustingOkHttpClient;
import com.davis.ddf.crs.UniversalFederatedSource;
import com.davis.ddf.crs.data.UniversalFederatedSourceResponse;
import com.davis.ddf.crs.parsing.UniversalFederatedSourceParser;
import com.davis.ddf.crs.service.SourceService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class RestGetService implements SourceService {
  private static final Logger LOGGER = LoggerFactory.getLogger(UniversalFederatedSource.class);
  UniversalFederatedSource source;
  private String url;
  private OkHttpClient client;
  private UniversalFederatedSourceParser parser;


  public RestGetService(UniversalFederatedSource source, int mode,
                        OkHttpClient client) {
    this.source = source;
    this.client = client;
    LOGGER.debug("Client = {}", client);

    this.url = source.getSsServiceUrl();
    LOGGER.debug("Creating REST Service for " + this.url);
    parser = new UniversalFederatedSourceParser(mode, source);

  }





  /**
   * Get results for query
   *
   * @param httpUrl a ArrayList containing all the query params to send to the endpoint. These params must have the
   *                appropriate & symbols added in-between the parameters with the last variable having nothing. The
   *                strings within the array list also need to hold the queryParameter. Ie amount=
   **/
  @Override
  public ArrayList<UniversalFederatedSourceResponse> getResultsForQuery(HttpUrl httpUrl) {
    ArrayList<UniversalFederatedSourceResponse> finalResultsList = null;

    LOGGER.debug("OkHttp URL to Call = {}", httpUrl.url().toString());

    String response = null;
    try {
      response = getResponseString(httpUrl);
    } catch (IOException e) {
      LOGGER.error("The request could not be completed {}",e);
    }
    LOGGER.info("Successfully got response from source.");
    //transform to JSON
    String pathResult = null;
    try {
      pathResult = parser.extractJSONPath(response, source.getSsContainerPath());
      LOGGER.debug("JsonPath result length = " + String.valueOf(pathResult.length()));
    } catch (Exception e) {
      LOGGER.warn("Exception occurred during the extraction of the response data from the RestSource getting errors from response if any.");

      try {
        pathResult = parser.extractJSONPath(response, "$.errors[*]");
        LOGGER.warn("Errors contained in the response = {}", pathResult);
      } catch (Exception e1) {
        //do nothing
      }
      //create empty result list to prevents errors
      finalResultsList = new ArrayList<UniversalFederatedSourceResponse>();
      return finalResultsList;
    }
    //Time to create POJOs
    finalResultsList = parser.getObjectsFromJson(pathResult);
    LOGGER.debug("Finished Printing REST Results Received a Total of " + String.valueOf(finalResultsList.size()) + " results.");
    return finalResultsList;
  }

  public String getResponseString( HttpUrl urlToCall) throws IOException {
    /*URL theURL = UrlBuilder.fromString(urlToCall, "ISO-8859-1")
        .encodeAs("UTF-8").toUrl();*/
    LOGGER.debug("Client = {}", client);

    Request request = new Request.Builder().url(urlToCall).get().build();


    Response response = client.newCall(request).execute();
    if (!response.isSuccessful()) {
      return null;
    }
    return response.body().string();
  }

  private OkHttpClient createNetworkClient(String certPath, String certPassword) {
    if(client != null){
      return client;
    }else{
      TrustingOkHttpClient trustingOkHttpClient = new TrustingOkHttpClient();
      OkHttpClient result = null;

      LOGGER.debug("certPath = {}", certPath);
      LOGGER.debug("certPassword = {}", certPassword);

      if (certPath != null && certPassword != null) {
        if (!certPath.trim().equalsIgnoreCase("") &&
                certPassword.trim().equalsIgnoreCase("")) {

          LOGGER.info("RestGetService is Using the Supplied certificate path of {} ",
                  certPath);

          result = trustingOkHttpClient.getUnsafeOkHttpClient(15, 15,
                  certPath, certPassword);
          client =result;
        }
      } else {
        LOGGER.info("RestGetService was not supplied a certificate path using jar resources ");
        result = trustingOkHttpClient.getUnsafeOkHttpClient(15, 15, null, null);
        client =result;

      }
      if(client != null){
        LOGGER.debug("Successfully created client {}",client);
      }else{
        LOGGER.debug("Unsuccessful in creating client {}",client);
      }
      return result;

    }
  }
}