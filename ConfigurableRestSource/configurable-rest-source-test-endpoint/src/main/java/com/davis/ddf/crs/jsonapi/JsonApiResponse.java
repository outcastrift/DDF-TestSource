package com.davis.ddf.crs.jsonapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * This class will encompass the values that should be returned to the calling application. Look at
 *
 * http://jsonapi.org/format
 */
public class JsonApiResponse {
  /**
   * The constant dateFormatPattern.
   */
  public static final String dateFormatPattern = "yyyy-MM-dd'T'HH:mm:ssZ";

  /**
   * The Version.
   */
  private String version;
  /**
   * The Data.
   */
  private Object data;
  /**
   * The Errors.
   */
  private List<JsonApiResponseError> errors;
  /**
   * The Meta.
   */
  private String meta;

  /**
   * Instantiates a new Json api response.
   */
  public JsonApiResponse() {
    errors = new ArrayList<JsonApiResponseError>();
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
   * Gets version.
   *
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Sets version.
   *
   * @param version the version
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Gets data.
   *
   * @return the data
   */
  public Object getData() {
    return data;
  }

  /**
   * Sets data.
   *
   * @param data the data
   */
  public void setData(Object data) {
    this.data = data;
  }

  /**
   * Gets errors.
   *
   * @return the errors
   */
  public List<JsonApiResponseError> getErrors() {
    return errors;
  }

  /**
   * Sets errors.
   *
   * @param errors the errors
   */
  public void setErrors(List<JsonApiResponseError> errors) {
    this.errors = errors;
  }

  /**
   * Add error.
   *
   * @param error the error
   */
  public void addError(JsonApiResponseError error) {
    if (errors == null) {
      setErrors(new ArrayList<JsonApiResponseError>());
    }

    errors.add(error);
  }

  /**
   * Remove error.
   *
   * @param error the error
   */
  public void removeError(JsonApiResponseError error) {
    if (errors != null && errors.contains(error)) {
      errors.remove(error);
    }
  }

  /**
   * Has errors boolean.
   *
   * @return the boolean
   */
  public boolean hasErrors() {
    return errors != null && errors.size() > 0;
  }

  /**
   * Gets meta.
   *
   * @return the meta
   */
  public String getMeta() {
    return meta;
  }

  /**
   * Sets meta.
   *
   * @param meta the meta
   */
  public void setMeta(String meta) {
    this.meta = meta;
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