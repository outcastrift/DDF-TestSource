package com.davis.ddf.test.filter;

import org.opengis.filter.sort.SortBy;

import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;

/**
 * Basic FilterCriteria container.
 *
 * @author Bob Harrod
 * @version 1.0
 */
public class FilterCriteria {
  private ResourceFetch resourceFetch;
  private ContextualSearch contextualSearch;
  private SpatialFilter spatialFilter;
  private TemporalFilter temporalFilter;
  private SortBy sortBy;

  /**
   * Instantiates a new Filter criteria.
   */
  public FilterCriteria() {}

  /**
   * Gets resource fetch.
   *
   * @return the resourceFetch
   */
  public ResourceFetch getResourceFetch() {
    return resourceFetch;
  }

  /**
   * Sets resource fetch.
   *
   * @param resourceFetch the resourceFetch to set
   */
  public void setResourceFetch(ResourceFetch resourceFetch) {
    this.resourceFetch = resourceFetch;
  }

  /**
   * Gets contextual search.
   *
   * @return the contextualSearch
   */
  public ContextualSearch getContextualSearch() {
    return contextualSearch;
  }

  /**
   * Sets contextual search.
   *
   * @param contextualSearch the contextualSearch to set
   */
  public void setContextualSearch(ContextualSearch contextualSearch) {
    this.contextualSearch = contextualSearch;
  }

  /**
   * Gets spatial filter.
   *
   * @return the spatialFilter
   */
  public SpatialFilter getSpatialFilter() {
    return spatialFilter;
  }

  /**
   * Sets spatial filter.
   *
   * @param spatialFilter the spatialFilter to set
   */
  public void setSpatialFilter(SpatialFilter spatialFilter) {
    this.spatialFilter = spatialFilter;

  }

  /**
   * Gets temporal filter.
   *
   * @return the temporalFilter
   */
  public TemporalFilter getTemporalFilter() {
    return temporalFilter;
  }

  /**
   * Sets temporal filter.
   *
   * @param temporalFilter the temporalFilter to set
   */
  public void setTemporalFilter(TemporalFilter temporalFilter) {
    this.temporalFilter = temporalFilter;
  }

  /**
   * Gets sort by.
   *
   * @return the sortBy
   */
  public SortBy getSortBy() {
    return sortBy;
  }

  /**
   * Sets sort by.
   *
   * @param sortBy the sortBy to set
   */
  public void setSortBy(SortBy sortBy) {
    this.sortBy = sortBy;
  }


}
