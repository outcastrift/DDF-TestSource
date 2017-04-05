package com.davis.ddf.crs.filter;

import org.opengis.filter.sort.SortBy;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Geometry;
import org.opengis.geometry.primitive.Point;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;

/**
 * Created by hduser on 10/19/15.
 */
public class JsonCompatibleFilterCriteria {
  /**
   * The constant SPATIAL_DISTANCE_FILTER.
   */
  public static final String SPATIAL_DISTANCE_FILTER = "SpatialDistanceFilter";
  /**
   * The constant BOUNDING_BOX_FILTER.
   */
  public static final String BOUNDING_BOX_FILTER = "BoundingBoxFilter";
  private static XLogger logger =
      new XLogger(LoggerFactory.getLogger(JsonCompatibleFilterCriteria.class));
  /**
   * The Longitude.
   */
  // todo Change the way this class handles the the spatial filter criteria.
  // todo The 4 doubles above are for bounding box filters while the longitude, latitude and
  // distanceInMeters support SpatialDistanceFilters
  // This was done to avoid having to change the existing scripts which expect north, west south and
  // east to be present
  double longitude;
  /**
   * The Latitude.
   */
  double latitude;
  /**
   * The Distance in meters.
   */
  double distanceInMeters;
  private ResourceFetch resourceFetch;
  private ContextualSearch contextualSearch;
  private double north;
  private double west;
  private double south;
  private double east;
  private String spatialFilterType;
  private TemporalFilter temporalFilter;
  private SortBy sortBy;

  /**
   * Instantiates a new Json compatible filter criteria.
   *
   * @param criteria the criteria
   * @param convertToBBox the convert to b box
   */
  public JsonCompatibleFilterCriteria(FilterCriteria criteria, boolean convertToBBox) {
    // todo add support for other spatial criteria
    this.resourceFetch = criteria.getResourceFetch();
    this.contextualSearch = criteria.getContextualSearch();
    this.temporalFilter = criteria.getTemporalFilter();
    this.sortBy = criteria.getSortBy();
    SpatialFilter spatialFilter = criteria.getSpatialFilter();

    if (spatialFilter != null) {

      if (spatialFilter instanceof SpatialDistanceFilter && !convertToBBox) {
        spatialFilterType = SPATIAL_DISTANCE_FILTER;
        SpatialDistanceFilter sdf = (SpatialDistanceFilter) spatialFilter;
        Geometry geo = sdf.getGeometry();
        logger.debug("GEO: " + geo);
        Point point = (Point) geo;
        logger.debug("POINT: " + point);
        DirectPosition dp = point.getDirectPosition();
        logger.debug("DirectPosition: " + dp);
        double[] coordinates = dp.getCoordinate();
        this.longitude = coordinates[0];
        this.latitude = coordinates[1];
        logger.debug("Distance: " + sdf.getDistanceInMeters());
        this.distanceInMeters = sdf.getDistanceInMeters();
      } else if (spatialFilter instanceof SpatialDistanceFilter) {
        spatialFilterType = BOUNDING_BOX_FILTER;
        SpatialDistanceFilter sdf = (SpatialDistanceFilter) spatialFilter;
        Geometry geo = sdf.getGeometry();
        logger.debug("GEO: " + geo);
        Point point = (Point) geo;
        logger.debug("POINT: " + point);
        DirectPosition dp = point.getDirectPosition();
        logger.debug("DirectPosition: " + dp);
        double[] coordinates = dp.getCoordinate();
        double lon = coordinates[0];
        double lat = coordinates[1];
        logger.debug("Distance: " + sdf.getDistanceInMeters());
        double distance = sdf.getDistanceInMeters();
        double[] bboxCoords = OpenSearchSiteUtil.createBBoxFromPointRadius(lon, lat, distance);
        north = bboxCoords[3];

        west = bboxCoords[0];

        south = bboxCoords[1];

        east = bboxCoords[2];

        if (logger.isDebugEnabled()) {
          logger.debug("north = " + north);
          logger.debug("west = " + west);
          logger.debug("south = " + south);
          logger.debug("east = " + east);
        }
      } else {
        spatialFilterType = BOUNDING_BOX_FILTER;
        String wktStr = criteria.getSpatialFilter().getGeometryWkt();
        String[] polyAry = OpenSearchSiteUtil.createPolyAryFromWKT(wktStr);
        double[] bboxCoords = OpenSearchSiteUtil.createBBoxFromPolygon(polyAry);
        // maxy minx
        north = bboxCoords[3];

        west = bboxCoords[0];

        south = bboxCoords[1];

        east = bboxCoords[2];

        if (logger.isDebugEnabled()) {
          logger.debug("north = " + north);
          logger.debug("west = " + west);
          logger.debug("south = " + south);
          logger.debug("east = " + east);
        }
      }
    }

  }

  /**
   * Gets resource fetch.
   *
   * @return the resource fetch
   */
  public ResourceFetch getResourceFetch() {
    return resourceFetch;
  }

  /**
   * Sets resource fetch.
   *
   * @param resourceFetch the resource fetch
   */
  public void setResourceFetch(ResourceFetch resourceFetch) {
    this.resourceFetch = resourceFetch;
  }

  /**
   * Gets contextual search.
   *
   * @return the contextual search
   */
  public ContextualSearch getContextualSearch() {
    return contextualSearch;
  }

  /**
   * Sets contextual search.
   *
   * @param contextualSearch the contextual search
   */
  public void setContextualSearch(ContextualSearch contextualSearch) {
    this.contextualSearch = contextualSearch;
  }

  /**
   * Gets sort by.
   *
   * @return the sort by
   */
  public SortBy getSortBy() {
    return sortBy;
  }

  /**
   * Sets sort by.
   *
   * @param sortBy the sort by
   */
  public void setSortBy(SortBy sortBy) {
    this.sortBy = sortBy;
  }

  /**
   * Gets temporal filter.
   *
   * @return the temporal filter
   */
  public TemporalFilter getTemporalFilter() {
    return temporalFilter;
  }

  /**
   * Sets temporal filter.
   *
   * @param temporalFilter the temporal filter
   */
  public void setTemporalFilter(TemporalFilter temporalFilter) {
    this.temporalFilter = temporalFilter;
  }

  /**
   * Gets east.
   *
   * @return the east
   */
  public double getEast() {
    return east;
  }

  /**
   * Sets east.
   *
   * @param e the e
   */
  public void setEast(double e) {
    east = e;
  }

  /**
   * Gets west.
   *
   * @return the west
   */
  public double getWest() {
    return west;
  }

  /**
   * Sets west.
   *
   * @param w the w
   */
  public void setWest(double w) {
    this.west = w;
  }

  /**
   * Gets north.
   *
   * @return the north
   */
  public double getNorth() {
    return north;
  }

  /**
   * Sets north.
   *
   * @param n the n
   */
  public void setNorth(double n) {
    this.north = n;
  }

  /**
   * Gets south.
   *
   * @return the south
   */
  public double getSouth() {
    return south;
  }

  /**
   * Sets south.
   *
   * @param s the s
   */
  public void setSouth(double s) {
    this.south = s;
  }
  /*
  
  
   */
}
