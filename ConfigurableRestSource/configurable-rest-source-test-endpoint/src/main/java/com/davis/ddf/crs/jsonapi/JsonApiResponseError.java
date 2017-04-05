package com.davis.ddf.crs.jsonapi;

import java.util.List;

/**
 * The type Json api response error.
 */
public class JsonApiResponseError {

  /**
   * The Id.
   */
  private int id;
  /**
   * The Links.
   */
  private List<JsonApiResponseErrorLink> links;
  /**
   * The Status.
   */
  private int status;
  /**
   * The Code.
   */
  private String code;
  /**
   * The Title.
   */
  private String title;
  /**
   * The Detail.
   */
  private String detail;
  /**
   * The Source.
   */
  private JsonApiResponseErrorSource source;
  /**
   * The Meta.
   */
  private JsonApiResponseErrorMeta meta;

  /**
   * Instantiates a new Json api response error.
   */
  public JsonApiResponseError() {
    super();
  }

  /**
   * Instantiates a new Json api response error.
   *
   * @param status the status
   * @param title the title
   * @param detail the detail
   */
  public JsonApiResponseError(int status, String title, String detail) {
    this.status = status;
    this.title = title;
    this.detail = detail;
  }

  /**
   * Gets id.
   *
   * @return the id
   */
  public int getId() {
    return id;
  }

  /**
   * Sets id.
   *
   * @param id the id
   */
  public void setId(int id) {
    this.id = id;
  }

  /**
   * Gets links.
   *
   * @return the links
   */
  public List<JsonApiResponseErrorLink> getLinks() {
    return links;
  }

  /**
   * Sets links.
   *
   * @param links the links
   */
  public void setLinks(List<JsonApiResponseErrorLink> links) {
    this.links = links;
  }

  /**
   * Gets status.
   *
   * @return the status
   */
  public int getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(int status) {
    this.status = status;
  }

  /**
   * Gets code.
   *
   * @return the code
   */
  public String getCode() {
    return code;
  }

  /**
   * Sets code.
   *
   * @param code the code
   */
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * Gets title.
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets title.
   *
   * @param title the title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Gets detail.
   *
   * @return the detail
   */
  public String getDetail() {
    return detail;
  }

  /**
   * Sets detail.
   *
   * @param detail the detail
   */
  public void setDetail(String detail) {
    this.detail = detail;
  }

  /**
   * Gets source.
   *
   * @return the source
   */
  public JsonApiResponseErrorSource getSource() {
    return source;
  }

  /**
   * Sets source.
   *
   * @param source the source
   */
  public void setSource(JsonApiResponseErrorSource source) {
    this.source = source;
  }

  /**
   * Gets meta.
   *
   * @return the meta
   */
  public JsonApiResponseErrorMeta getMeta() {
    return meta;
  }

  /**
   * Sets meta.
   *
   * @param meta the meta
   */
  public void setMeta(JsonApiResponseErrorMeta meta) {
    this.meta = meta;
  }
}