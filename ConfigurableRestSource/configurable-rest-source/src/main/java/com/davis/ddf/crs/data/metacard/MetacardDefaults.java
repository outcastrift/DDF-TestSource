package com.davis.ddf.crs.data.metacard;

import java.util.Date;

/**
 * Metacard defaults for JSON / XML to MetacardImpl.
 *
 * @author Bob Harrod
 * @version 1.0
 */
public class MetacardDefaults {
  /**
   * The constant TIMEOUT.
   */
  private static final long TIMEOUT = 60 * 60 * 1000;
  /**
   * The Default metacard type.
   */
  private static final String DEFAULT_METACARD_TYPE = "string";
  /**
   * The Default metacard version.
   */
  private static final String DEFAULT_METACARD_VERSION = "1.0";
  /**
   * The Default metacard metadata.
   */
  private static final String DEFAULT_METACARD_METADATA = "<metadata></metadata>";
  /**
   * The Default metacard resource size.
   */
  private static final String DEFAULT_METACARD_RESOURCE_SIZE = "100";

  /**
   * The Created date.
   */
  private static Date createdDate;
  /**
   * The Modified date.
   */
  private static Date modifiedDate;
  /**
   * The Expiration date.
   */
  private static Date expirationDate;
  /**
   * The Effective date.
   */
  private static Date effectiveDate;

  /**
   * Instantiates a new Metacard defaults.
   */
  public MetacardDefaults(){
    setCreatedDate( new Date() );
    setModifiedDate( new Date( System.currentTimeMillis() ) );
    setEffectiveDate( new Date() );
    setExpirationDate( new Date( System.currentTimeMillis() + TIMEOUT ) );    
  }

  /**
   * Gets metadata.
   *
   * @return the metadata
   */
  public String getMetadata() {
    return DEFAULT_METACARD_METADATA;
  }

  /**
   * Gets created date.
   *
   * @return the createdDate
   */
  public Date getCreatedDate() {
    return createdDate;
  }

  /**
   * Sets created date.
   *
   * @param createdDate the createdDate to set
   */
  private void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  /**
   * Gets modified date.
   *
   * @return the modifiedDate
   */
  public Date getModifiedDate() {
    return modifiedDate;
  }

  /**
   * Sets modified date.
   *
   * @param modifiedDate the modifiedDate to set
   */
  private void setModifiedDate(Date modifiedDate) {
    this.modifiedDate = modifiedDate;
  }

  /**
   * Gets expiration date.
   *
   * @return the expirationDate
   */
  public Date getExpirationDate() {
    return expirationDate;
  }

  /**
   * Sets expiration date.
   *
   * @param expirationDate the expirationDate to set
   */
  private void setExpirationDate(Date expirationDate) {
    this.expirationDate = expirationDate;
  }

  /**
   * Gets effective date.
   *
   * @return the effectiveDate
   */
  public Date getEffectiveDate() {
    return effectiveDate;
  }

  /**
   * Sets effective date.
   *
   * @param effectiveDate the effectiveDate to set
   */
  private void setEffectiveDate(Date effectiveDate) {
    this.effectiveDate = effectiveDate;
  }

  /**
   * Gets content type name.
   *
   * @return the contentTypeName
   */
  public String getContentTypeName() {
    return DEFAULT_METACARD_TYPE;
  }

  /**
   * Gets content type version.
   *
   * @return the contentTypeVersion
   */
  public String getContentTypeVersion() {
    return DEFAULT_METACARD_VERSION;
  }

  /**
   * Gets resource size.
   *
   * @return the resourceSize
   */
  public String getResourceSize() {
    return DEFAULT_METACARD_RESOURCE_SIZE;
  }
  
  
}