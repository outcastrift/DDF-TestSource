package com.davis.ddf.crs.data;

import com.google.gson.annotations.Expose;

import java.util.Date;

/** Created by hduser on 7/17/15. */
public class CRSEndpointResponse {

  private Integer niirs;
  private String location;
  /** The Report link. */
  private String reportLink;

  private String displayTitle;
  @Expose private String displaySerial;
  @Expose private String summary;
  @Expose private String originatorUnit;
  /** The Primary event type. */
  @Expose private String primaryEventType;

  @Expose private String classification;
  @Expose private Date dateOccurred;
  @Expose private double latitude;
  @Expose private double longitude;

  /**
   * Gets report link.
   *
   * @return the report link
   */
  public String getReportLink() {
    return reportLink;
  }

  /**
   * Sets report link.
   *
   * @param reportLink the report link
   */
  public void setReportLink(String reportLink) {
    this.reportLink = reportLink;
  }

  /**
   * Gets display title.
   *
   * @return the display title
   */
  public String getDisplayTitle() {
    return displayTitle;
  }

  /**
   * Sets display title.
   *
   * @param displayTitle the display title
   */
  public void setDisplayTitle(String displayTitle) {
    this.displayTitle = displayTitle;
  }

  /**
   * Gets display serial.
   *
   * @return the display serial
   */
  public String getDisplaySerial() {
    return displaySerial;
  }

  /**
   * Sets display serial.
   *
   * @param displaySerial the display serial
   */
  public void setDisplaySerial(String displaySerial) {
    this.displaySerial = displaySerial;
  }

  /**
   * Gets classification.
   *
   * @return the classification
   */
  public String getClassification() {
    return classification;
  }

  /**
   * Sets classification.
   *
   * @param classification the classification
   */
  public void setClassification(String classification) {
    this.classification = classification;
  }

  /**
   * Gets summary.
   *
   * @return the summary
   */
  public String getSummary() {
    return summary;
  }

  /**
   * Sets summary.
   *
   * @param summary the summary
   */
  public void setSummary(String summary) {
    this.summary = summary;
  }

  /**
   * Gets originator unit.
   *
   * @return the originator unit
   */
  public String getOriginatorUnit() {
    return originatorUnit;
  }

  /**
   * Sets originator unit.
   *
   * @param originalUnit the original unit
   */
  public void setOriginatorUnit(String originalUnit) {
    this.originatorUnit = originalUnit;
  }

  /**
   * Gets primary event type.
   *
   * @return the primary event type
   */
  public String getPrimaryEventType() {
    return primaryEventType;
  }

  /**
   * Sets primary event type.
   *
   * @param primaryEventType the primary event type
   */
  public void setPrimaryEventType(String primaryEventType) {
    this.primaryEventType = primaryEventType;
  }

  /**
   * Gets date occurred.
   *
   * @return the date occurred
   */
  public Date getDateOccurred() {
    return dateOccurred;
  }

  /**
   * Sets date occurred.
   *
   * @param dateOccurred the date occurred
   */
  public void setDateOccurred(Date dateOccurred) {
    this.dateOccurred = dateOccurred;
  }

  /**
   * Gets latitude.
   *
   * @return the latitude
   */
  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  /**
   * Sets latitude.
   *
   * @param latitude the latitude
   */
  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  /**
   * Gets longitude.
   *
   * @return the longitude
   */
  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  /**
   * Sets longitude.
   *
   * @param longitude the longitude
   */
  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public String getLocation() {
    return location;
  }

  /*public void setMetaData(String metaData) {
      this.metaData = metaData;
  }*/

  public void setLocation(String location) {
    this.location = location;
  }

  /**
   * To string string.
   *
   * @return the string
   */
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("DisplayTitle:");
    builder.append(this.getDisplayTitle());
    return builder.toString();
  }

  public Integer getNiirs() {
    return niirs;
  }

  public void setNiirs(Integer niirs) {
    this.niirs = niirs;
  }

  public String getMetaData() {
    return addToMetadataResultMap();
  }

  private String addToMetadataResultMap() {
    StringBuilder stringBuilder = null;
    stringBuilder = new StringBuilder();

    stringBuilder.append("<Result " + String.valueOf(this.getDisplaySerial()) + ">");
    if (this.getDisplaySerial() != null) {
      stringBuilder.append("<displaySerial>" + this.getDisplaySerial() + "</displaySerial>");
    }
    if (this.getReportLink() != null) {
      stringBuilder.append("<reportLink>" + this.getReportLink() + "</reportLink>");
    }
    if (this.getSummary() != null) {
      stringBuilder.append("<summary>" + this.getSummary() + "</summary>");
    }
    if (this.getDisplayTitle() != null) {
      stringBuilder.append("<displayTitle>" + this.getDisplayTitle() + "</displayTitle>");
    }
    if (this.getOriginatorUnit() != null) {
      stringBuilder.append("<originatorUnit>" + this.getOriginatorUnit() + "</originatorUnit>");
    }
    if (this.getPrimaryEventType() != null) {
      stringBuilder.append(
          "<primaryEventType>" + this.getPrimaryEventType() + "</primaryEventType>");
    }
    if (this.getClassification() != null) {
      stringBuilder.append("<classification>" + this.getClassification() + "</classification>");
    }
    if (this.getNiirs() != null) {
      stringBuilder.append("<niirs>" + this.getNiirs() + "</niirs>");
    }
    if (this.getDateOccurred() != null) {
      stringBuilder.append(
          "<dateOccurred>" + this.getDateOccurred().toString() + "</dateOccurred>");
    }
    if (this.getLatitude() != null) {
      stringBuilder.append("<latitude>" + String.valueOf(this.getLatitude()) + "</latitude>");
    }
    if (this.getLongitude() != null) {
      stringBuilder.append("<longitude>" + String.valueOf(this.getLongitude()) + "</longitude>");
    }
    if (this.getLocation() != null) {
      stringBuilder.append("<location>" + String.valueOf(this.getLocation()) + "</location>");
    }

    stringBuilder.append("</Result>");
    String loopResult = stringBuilder.toString();
    return loopResult;
  }
}
