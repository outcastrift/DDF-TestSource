package com.davis.ddf.crs.data;

/**
 * This software was created for the Open Architecture Distributed Common Ground System
 * Modernization All rights to this software belong to appropriate licenses and restrictions apply.
 * Created by Samuel Davis on 3/21/16.
 */
public class GroovyResponseObject {
  private String id;
  /** The Title. */
  private String title;
  /** The Lat. */
  private double lat;
  /** The Lng. */
  private double lng;
  /** The Location. */
  private String location;

  /** Instantiates a new Rest response object. */
  public GroovyResponseObject() {}

  /**
   * Gets lat.
   *
   * @return the lat
   */
  public double getLat() {
    return lat;
  }

  /**
   * Sets lat.
   *
   * @param lat the lat
   */
  public void setLat(double lat) {
    this.lat = lat;
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
   * Gets lng.
   *
   * @return the lng
   */
  public double getLng() {
    return lng;
  }

  /**
   * Sets lng.
   *
   * @param lng the lng
   */
  public void setLng(double lng) {
    this.lng = lng;
  }

  /**
   * Gets location.
   *
   * @return the location
   */
  public String getLocation() {
    return String.valueOf(lat) + "," + String.valueOf(lng);
  }

  /**
   * Sets location.
   *
   * @param location the location
   */
  public void setLocation(String location) {
    this.location = location;
  }

  /**
   * Gets id.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets id.
   *
   * @param id the id
   */
  public void setId(String id) {
    this.id = id;
  }
}
