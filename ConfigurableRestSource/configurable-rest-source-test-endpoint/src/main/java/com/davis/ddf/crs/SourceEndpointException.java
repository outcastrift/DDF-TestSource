/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package com.davis.ddf.crs;

import com.davis.ddf.crs.jsonapi.JsonApiResponse;
import com.davis.ddf.crs.jsonapi.JsonApiResponseError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang.StringEscapeUtils;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Class that will provide support for returning an HTML response and message.  See:
 *
 * http://docs.oracle.com/javaee/7/api/javax/ws/rs/core/Response.Status.html
 *
 * For a list of proper Status values
 */
public class SourceEndpointException extends WebApplicationException {
  /**
   * The constant dateFormatPattern.
   */
  public static final String dateFormatPattern = "yyyy-MM-dd'T'HH:mm:ssZ";
  /**
   * The constant serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Instantiates a new Source endpoint exception.
   *
   * @param title  the title
   * @param error  the error
   * @param status the status
   */
  public SourceEndpointException(String title, String error, Status status) {

    super(Response.status(status)
        .entity(generateJsonResponseWithError(status.getStatusCode(), title, error))
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        .build());
  }

  /**
   * Since we always want to return the JSON representation of an instance of {@link
   * JsonApiResponse}, then we don't want to use this method since it's designed to just return a
   * string message.
   *
   * @param message the message
   * @param status  the status
   */
  @Deprecated
  public SourceEndpointException(String message, Status status) {
    super(Response.status(status).entity(StringEscapeUtils.escapeHtml(message))
        .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN).build());
  }

  /**
   * Instantiates a new Source endpoint exception.
   *
   * @param cause the cause
   */
  public SourceEndpointException(Throwable cause) {
    super(cause);
  }

  /**
   * Generate json response with error string.
   *
   * @param statusCode the status code
   * @param title      the title
   * @param message    the message
   * @return the string
   */
  private static String generateJsonResponseWithError(int statusCode, String title,
      String message) {

    JsonApiResponse response = new JsonApiResponse();
    JsonApiResponseError error = new JsonApiResponseError(statusCode, title, message);
    response.addError(error);

    return response.getSanitizedJson();
  }

  /**
   * New gson gson.
   *
   * @return the gson
   */
  public static Gson newGson() {
    return new GsonBuilder().setDateFormat(dateFormatPattern).create();
  }

  /**
   * Get sanitized json string.
   *
   * @return the string
   */
  public String getSanitizedJson() {
    Gson gson = newGson();
    return gson.toJson(this);
  }
}