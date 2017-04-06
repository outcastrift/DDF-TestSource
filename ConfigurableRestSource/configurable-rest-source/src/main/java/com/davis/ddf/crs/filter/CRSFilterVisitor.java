package com.davis.ddf.crs.filter;

import com.vividsolutions.jts.geom.Coordinate;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.filter.LikeFilterImpl;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.opengis.filter.*;
import org.opengis.filter.expression.Literal;

import org.opengis.filter.spatial.*;
import org.opengis.filter.temporal.*;
import org.opengis.temporal.Period;
import org.opengis.temporal.PeriodDuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;

/** Created by hduser on 6/2/15. */
public class CRSFilterVisitor extends DefaultFilterVisitor {

  //region Class Variables
  private static final String ONLY_AND_MSG = "";
  private static Logger logger = LoggerFactory.getLogger(CRSFilterVisitor.class);
  /** The Any geo. */
  public String anyGeo = null;
  /** The Is or query. */
  private TemporalFilter temporalSearch = null;
  /** The Spatial search. */
  private SpatialFilter spatialSearch = null;

  private List<Filter> filters;
  private Double latitude = null;
  private Double longitude = null;
  private Double radius = null;
  private String metacardId = null;
  private boolean hasSpatial = false;
  private NestedTypes currentNest = null;
  private boolean hasContextual = false;
  //endregion
  private String searchPhrase = null;

  /** Public Constructor * */
  public CRSFilterVisitor() {
    filters = new ArrayList<>();
    temporalSearch = null;
  }

  //region Getters and Setters
  public String getAnyGeo() {
    return anyGeo;
  }

  public void setAnyGeo(String anyGeo) {
    this.anyGeo = anyGeo;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public Double getRadius() {
    return radius;
  }

  public void setRadius(Double radius) {
    this.radius = radius;
  }

  public String getMetacardId() {
    return metacardId;
  }

  public void setMetacardId(String metacardId) {
    this.metacardId = metacardId;
  }

  public boolean isHasSpatial() {
    return hasSpatial;
  }

  public void setHasSpatial(boolean hasSpatial) {
    this.hasSpatial = hasSpatial;
  }

  public NestedTypes getCurrentNest() {
    return currentNest;
  }

  public void setCurrentNest(NestedTypes currentNest) {
    this.currentNest = currentNest;
  }

  public boolean isHasContextual() {
    return hasContextual;
  }

  /**
   * Sets has contextual.
   *
   * @param hasContextual the has contextual
   */
  public void setHasContextual(boolean hasContextual) {
    this.hasContextual = hasContextual;
  }

  public String getSearchPhrase() {
    return searchPhrase;
  }

  public void setSearchPhrase(String searchPhrase) {
    this.searchPhrase = searchPhrase;
  }


    /**
     * Gets filters.
     *
     * @return the filters
     */
    public List<Filter> getFilters() {
        return filters;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    /**
     * Gets temporal search.
     *
     * @return the temporal search
     */
    public TemporalFilter getTemporalSearch() {
        return temporalSearch;
    }

    public void setTemporalSearch(TemporalFilter temporalSearch) {
        this.temporalSearch = temporalSearch;
    }

    /**
     * Gets spatial search.
     *
     * @return the spatial search
     */
    public SpatialFilter getSpatialSearch() {
        return spatialSearch;
    }

    public void setSpatialSearch(SpatialFilter spatialSearch) {
        this.spatialSearch = spatialSearch;
    }

    public String getMetacardIdForTransform() {
        return metacardId;
    }
    //endregion

  // region Filter Visit Methods
  @Override
  public Object visit(ExcludeFilter filter, Object data) {
    filters.add(filter);
    return data;
  }

  public Object visit(IncludeFilter filter, Object data) {
    filters.add(filter);
    return data;
  }

  @Override
  public Object visit(And filter, Object data) {
    Object newData;
    NestedTypes parentNest = currentNest;
    logger.trace("ENTERING: AND filter");
    currentNest = NestedTypes.AND;
    //filters.add(filter);
    newData = super.visit(filter, data);
    currentNest = parentNest;
    logger.trace("EXITING: AND filter");

    return newData;
  }

  /** Added by Sam */
  @Override
  public Object visit(Id filter, Object data) {
    logger.info("ENTERING: Id filter");
    logger.info("Id::Class:" + filter.getClass());
    logger.info("ID::" + filter);
    logger.info("data::Class:" + data);
    logger.info("data::" + data);
    filters.add(filter);

    return super.visit(filter, data);
  }

  @Override
  public Object visit(Not filter, Object data) {
    Object newData;
    NestedTypes parentNest = currentNest;
    logger.trace("ENTERING: NOT filter");
    currentNest = NestedTypes.NOT;
    filters.add(filter);
    newData = super.visit(filter, data);
    currentNest = parentNest;
    logger.trace("EXITING: NOT filter");

    return newData;
  }

  @Override
  public Object visit(Or filter, Object data) {
    Object newData;
    NestedTypes parentNest = currentNest;
    logger.trace("ENTERING: OR filter");
    currentNest = NestedTypes.OR;
    filters.add(filter);
    newData = super.visit(filter, data);
    currentNest = parentNest;
    logger.trace("EXITING: OR filter");

    return newData;
  }

  public Object visit(PropertyIsBetween filter, Object data) {
    data = filter.getLowerBoundary().accept(this, data);
    data = filter.getExpression().accept(this, data);
    data = filter.getUpperBoundary().accept(this, data);
    filters.add(filter);

    return data;
  }

  /** PropertyIsEqualTo filter maps to a Type/Version(s) search criteria. */
  @Override
  public Object visit(PropertyIsEqualTo filter, Object data) {
    logger.debug("ENTERING: PropertyIsEqualTo filter");

    if (filter.getExpression1() != null && filter.getExpression2() != null) {

      if (filter.getExpression2() instanceof Literal) {
        Literal literal = (Literal) filter.getExpression2();
        String exp2 = literal.getValue().toString();
        logger.debug(exp2);
        metacardId = (exp2);
      }
    }

    filters.add(filter);

    logger.debug("EXITING: PropertyIsEqualTo filter");

    return super.visit(filter, data);
  }

  public Object visit(PropertyIsNotEqualTo filter, Object data) {
    data = filter.getExpression1().accept(this, data);
    data = filter.getExpression2().accept(this, data);
    filters.add(filter);

    return data;
  }

  public Object visit(PropertyIsGreaterThan filter, Object data) {
    data = filter.getExpression1().accept(this, data);
    data = filter.getExpression2().accept(this, data);
    filters.add(filter);

    return data;
  }

  public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object data) {
    data = filter.getExpression1().accept(this, data);
    data = filter.getExpression2().accept(this, data);
    filters.add(filter);

    return data;
  }

  public Object visit(PropertyIsLessThan filter, Object data) {
    data = filter.getExpression1().accept(this, data);
    data = filter.getExpression2().accept(this, data);
    filters.add(filter);

    return data;
  }

  public Object visit(PropertyIsLessThanOrEqualTo filter, Object data) {
    data = filter.getExpression1().accept(this, data);
    data = filter.getExpression2().accept(this, data);
    filters.add(filter);

    return data;
  }

  /** PropertyIsLike filter maps to a Contextual search criteria. */
  @Override
  public Object visit(PropertyIsLike filter, Object data) {
    logger.trace("ENTERING: PropertyIsLike filter");

    if (currentNest != NestedTypes.NOT) {
      setHasContextual(true);
      LikeFilterImpl likeFilter = (LikeFilterImpl) filter;
      String searchPhrase = likeFilter.getLiteral();
      setSearchPhrase(searchPhrase);
      filters.add(filter);
    }

    logger.trace("EXITING: PropertyIsLike filter");

    return super.visit(filter, data);
  }

  public Object visit(PropertyIsNull filter, Object data) {
    data = filter.getExpression().accept(this, data);
    filters.add(filter);
    return data;
  }

  public Object visit(PropertyIsNil filter, Object data) {
    data = filter.getExpression().accept(this, data);
    filters.add(filter);
    return data;
  }

  public Object visit(final BBOX filter, Object data) {
    data = filter.getExpression1().accept(this, data);
    data = filter.getExpression2().accept(this, data);
    filters.add(filter);
    return data;
  }

  public Object visit(Beyond filter, Object data) {
    data = filter.getExpression1().accept(this, data);
    data = filter.getExpression2().accept(this, data);
    filters.add(filter);
    return data;
  }

  /**
   * Contains filter maps to a Polygon or BBox Spatial search criteria.
   *
   * @param filter the filter
   * @param data the data
   * @return the object
   */
  @Override
  public Object visit(Contains filter, Object data) {
    //       logger.debug("ENTERING: Contains filter");
    if (currentNest == null || NestedTypes.AND.equals(currentNest)) {
      // The geometric point is wrapped in a <Literal> element, so have to
      // get geometry expression as literal and then evaluate it to get
      // the geometry.
      // Example:
      // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl@64a7c45e</ogc:Literal>
      Literal literalWrapper = (Literal) filter.getExpression2();
      Object geometryExpression = literalWrapper.getValue();

      StringBuffer geometryWkt = new StringBuffer();

      if (geometryExpression instanceof SurfaceImpl) {
        SurfaceImpl polygon = (SurfaceImpl) literalWrapper.evaluate(null);

        Coordinate[] coords = polygon.getJTSGeometry().getCoordinates();
        Point point = polygon.getJTSGeometry().getCentroid();
        geometryWkt.append("POLYGON((");
        for (int i = 0; i < coords.length; i++) {
          geometryWkt.append(coords[i].x);
          geometryWkt.append(" ");
          geometryWkt.append(coords[i].y);

          if (i != (coords.length - 1)) {
            geometryWkt.append(",");
          }
        }
        geometryWkt.append("))");
        this.spatialSearch = new SpatialFilter(geometryWkt.toString());
        longitude = point.getX();

        latitude = point.getY();

        radius = point.getBoundary().getLength() * 10;

        hasSpatial = true;

        filters.add(filter);
        anyGeo = filter.toString();

        //    logger.debug("geometryWkt = [" + geometryWkt.toString() + "]");

        filters.add(filter);

      } else if (geometryExpression instanceof Polygon) {
        Polygon polygon = (Polygon) geometryExpression;
        Point centroid = polygon.getCentroid();
        longitude = centroid.getX();
        latitude = centroid.getY();
        radius = polygon.getBoundary().getLength() * 10;
        hasSpatial = true;

        filters.add(filter);
      } else {
        logger.warn("visit.Contains :: Only POLYGON geometry WKT for Contains filter is supported");
      }
    } else {
      logger.warn(ONLY_AND_MSG);
    }

    //     logger.debug("EXITING: Contains filter");

    return super.visit(filter, data);
  }

  public Object visit(Crosses filter, Object data) {
    filters.add(filter);
    data = filter.getExpression1().accept(this, data);
    data = filter.getExpression2().accept(this, data);
    return data;
  }

  public Object visit(Disjoint filter, Object data) {
    filters.add(filter);
    data = filter.getExpression1().accept(this, data);
    data = filter.getExpression2().accept(this, data);
    return data;
  }

  /**
   * DWithin filter maps to a Point/Radius distance Spatial search criteria.
   *
   * @param filter the filter
   * @param data the data
   * @return the object
   */
  @Override
  public Object visit(DWithin filter, Object data) {
    //      logger.debug("ENTERING: DWithin filter");
    if (currentNest == null || NestedTypes.AND.equals(currentNest)) {
      // The geometric point is wrapped in a <Literal> element, so have to
      // get geometry expression as literal and then evaluate it to get
      // the geometry.
      // Example:
      // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl@dc33f184</ogc:Literal>
      Literal literalWrapper = (Literal) filter.getExpression2();

      // Luckily we know what type the geometry expression should be, so
      // we
      // can cast it
      com.vividsolutions.jts.geom.Point point =
          (com.vividsolutions.jts.geom.Point) literalWrapper.evaluate(null);
      Coordinate[] crds = point.getCoordinates();
      Coordinate coords = point.getCentroid().getCoordinate();
      //            double[] coords = point.getCentroid().getCoordinate();
      double distance = filter.getDistance();

      //     logger.debug("point: coords[0] = " + crds[0].y + ",   coords[1] = " + crds[0].y);
      //      logger.debug("radius = " + distance);

      this.spatialSearch = new SpatialDistanceFilter(crds[0].x, crds[0].y, distance);
      longitude = coords.x;
      latitude = coords.y;
      radius = distance / 1000;
      anyGeo = filter.toString();
      hasSpatial = true;
      filters.add(filter);
    } else {
      logger.warn(ONLY_AND_MSG);
    }

    //      logger.debug("EXITING: DWithin filter");

    return super.visit(filter, data);
  }

  public Object visit(Equals filter, Object data) {
    data = filter.getExpression1().accept(this, data);
    data = filter.getExpression2().accept(this, data);
    filters.add(filter);
    return data;
  }

  /**
   * Intersects filter maps to a Polygon or BBox Spatial search criteria.
   *
   * @param filter the filter
   * @param data the data
   * @return the object
   */
  @Override
  public Object visit(Intersects filter, Object data) {
    logger.debug("ENTERING: Intersects filter");
    if (currentNest == null || NestedTypes.AND.equals(currentNest)) {
      // The geometric point is wrapped in a <Literal> element, so have to
      // get geometry expression as literal and then evaluate it to get
      // the geometry.
      // Example:
      // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl@64a7c45e</ogc:Literal>
      Literal literalWrapper = (Literal) filter.getExpression2();
      Object geometryExpression = literalWrapper.getValue();

      StringBuffer geometryWkt = new StringBuffer();

      //       logger.info("GeometryExpression = " + geometryExpression.getClass());
      if (geometryExpression instanceof SurfaceImpl) {
        SurfaceImpl polygon = (SurfaceImpl) literalWrapper.evaluate(null);

        Coordinate[] coords = polygon.getJTSGeometry().getCoordinates();

        geometryWkt.append("POLYGON((");
        for (int i = 0; i < coords.length; i++) {
          geometryWkt.append(coords[i].x);
          geometryWkt.append(" ");
          geometryWkt.append(coords[i].y);

          if (i != (coords.length - 1)) {
            geometryWkt.append(",");
          }
        }
        geometryWkt.append("))");
        this.spatialSearch = new SpatialFilter(geometryWkt.toString());
        Point point = polygon.getJTSGeometry().getCentroid();

        longitude = point.getX();

        latitude = point.getY();

        radius = point.getBoundary().getLength() * 10;

        hasSpatial = true;
        //     logger.debug("geometryWkt = [" + geometryWkt.toString() + "]");

        filters.add(filter);
      } else if (geometryExpression instanceof com.vividsolutions.jts.geom.Polygon) {

        com.vividsolutions.jts.geom.Polygon polygon =
            (com.vividsolutions.jts.geom.Polygon) geometryExpression;
        //     logger.info("POLYGON::" + polygon);
        Point centroid = polygon.getCentroid();
        this.spatialSearch = new SpatialFilter(polygon.toString());
        longitude = centroid.getX();
        latitude = centroid.getY();
        radius = polygon.getBoundary().getLength() * 10;
        hasSpatial = true;
        anyGeo = filter.toString();
        filters.add(filter);

      } else {
        logger.warn(
            "visit.Intersets :: Only POLYGON geometry WKT for Intersects filter is supported");
      }
    } else {
      logger.warn(ONLY_AND_MSG);
    }

    //    logger.debug("EXITING: Intersects filter");

    return super.visit(filter, data);
  }

  @Override
  public Object visit(Overlaps filter, Object data) {
    data = filter.getExpression1().accept(this, data);
    data = filter.getExpression2().accept(this, data);
    filters.add(filter);
    return data;
  }

  @Override
  public Object visit(Touches filter, Object data) {
    filters.add(filter);
    data = filter.getExpression1().accept(this, data);
    data = filter.getExpression2().accept(this, data);

    return data;
  }

  @Override
  public Object visit(Within filter, Object data) {
    filters.add(filter);
    data = filter.getExpression1().accept(this, data);
    data = filter.getExpression2().accept(this, data);

    return data;
  }

  /**
   * Visit object.
   *
   * @param filter the filter
   * @param data the data
   * @return the object
   */
  @Override
  public Object visit(After filter, Object data) {
    //     logger.debug("ENTERING: After filter");
    if (currentNest == null || NestedTypes.AND.equals(currentNest)) {
      //handleTemporal(filter);
      Literal literalWrapper = (Literal) filter.getExpression2();
      Object literal = literalWrapper.evaluate(null);
      if (literal instanceof Date) {
        if (temporalSearch == null) temporalSearch = new TemporalFilter(0);

        temporalSearch.setStartDate((Date) literal);
      }
    } else {
      logger.warn(ONLY_AND_MSG);
    }
    //    logger.debug("EXITING: After filter");

    return super.visit(filter, data);
  }

  @Override
  public Object visit(AnyInteracts anyInteracts, Object data) {
    filters.add(anyInteracts);
    data = anyInteracts.getExpression1().accept(this, data);
    data = anyInteracts.getExpression2().accept(this, data);
    return data;
  }

  /**
   * Visit object.
   *
   * @param filter the filter
   * @param data the data
   * @return the object
   */
  @Override
  public Object visit(Before filter, Object data) {
    //      logger.debug("ENTERING: Before filter");
    if (currentNest == null || NestedTypes.AND.equals(currentNest)) {
      //handleTemporal(filter);
      Literal literalWrapper = (Literal) filter.getExpression2();
      Object literal = literalWrapper.evaluate(null);
      if (literal instanceof Date) {
        if (temporalSearch == null) temporalSearch = new TemporalFilter(0);

        temporalSearch.setEndDate((Date) literal);
      }
    } else {
      logger.warn(ONLY_AND_MSG);
    }
    //     logger.debug("EXITING: Before filter");

    return super.visit(filter, data);
  }

  /* @Override
  public Object visit(After after, Object data) {
      filters.add(after);
      data = after.getExpression1().accept(this, data);
      data = after.getExpression2().accept(this, data);
      return data;
  }*/

  @Override
  public Object visit(Begins begins, Object data) {
    filters.add(begins);
    data = begins.getExpression1().accept(this, data);
    data = begins.getExpression2().accept(this, data);
    return data;
  }

  /* @Override
  public Object visit(Before before, Object data) {
      filters.add(before);
      data = before.getExpression1().accept(this, data);
      data = before.getExpression2().accept(this, data);
      return data;
  }*/

  @Override
  public Object visit(BegunBy begunBy, Object data) {
    filters.add(begunBy);
    data = begunBy.getExpression1().accept(this, data);
    data = begunBy.getExpression2().accept(this, data);
    return data;
  }

  /**
   * During filter maps to a Temporal (Absolute and Offset) search criteria.
   *
   * @param filter the filter
   * @param data the data
   * @return the object
   */
  @Override
  public Object visit(During filter, Object data) {
    //     logger.debug("ENTERING: TOverlaps filter");
    if (currentNest == null || NestedTypes.AND.equals(currentNest)) {
      handleTemporal(filter);
    } else {
      logger.warn(ONLY_AND_MSG);
    }
    //       logger.debug("EXITING: TOverlaps filter");

    return super.visit(filter, data);
  }

  /*@Override
  public Object visit(During during, Object data) {
      filters.add(during);
      data = during.getExpression1().accept(this, data);
      data = during.getExpression2().accept(this, data);
      return data;
  }*/

  @Override
  public Object visit(EndedBy endedBy, Object data) {
    filters.add(endedBy);
    data = endedBy.getExpression1().accept(this, data);
    data = endedBy.getExpression2().accept(this, data);
    return data;
  }

  @Override
  public Object visit(Ends ends, Object data) {
    filters.add(ends);
    data = ends.getExpression1().accept(this, data);
    data = ends.getExpression2().accept(this, data);
    return data;
  }

  @Override
  public Object visit(Meets meets, Object data) {
    filters.add(meets);
    data = meets.getExpression1().accept(this, data);
    data = meets.getExpression2().accept(this, data);
    return data;
  }

  @Override
  public Object visit(MetBy metBy, Object data) {
    filters.add(metBy);
    data = metBy.getExpression1().accept(this, data);
    data = metBy.getExpression2().accept(this, data);
    return data;
  }

  @Override
  public Object visit(OverlappedBy overlappedBy, Object data) {
    filters.add(overlappedBy);
    data = overlappedBy.getExpression1().accept(this, data);
    data = overlappedBy.getExpression2().accept(this, data);
    return data;
  }

  @Override
  public Object visit(TContains contains, Object data) {
    filters.add(contains);
    data = contains.getExpression1().accept(this, data);
    data = contains.getExpression2().accept(this, data);
    return data;
  }

  @Override
  public Object visit(TEquals equals, Object data) {
    filters.add(equals);
    data = equals.getExpression1().accept(this, data);
    data = equals.getExpression2().accept(this, data);
    return data;
  }

  @Override
  public Object visit(TOverlaps contains, Object data) {
    filters.add(contains);

    data = contains.getExpression1().accept(this, data);
    data = contains.getExpression2().accept(this, data);
    return data;
  }

  //endregion

  /**
   * Handle temporal.
   *
   * @param filter the filter
   */
  private void handleTemporal(BinaryTemporalOperator filter) {

    Literal literalWrapper = (Literal) filter.getExpression2();
    //      logger.info("literalWrapper.getValue() = " + literalWrapper.getValue());

    Object literal = literalWrapper.evaluate(null);
    if (literal instanceof Period) {
      Period period = (Period) literal;

      // Extract the start and end dates from the filter
      Date start = period.getBeginning().getPosition().getDate();
      Date end = period.getEnding().getPosition().getDate();

      temporalSearch = new TemporalFilter(start, end);

      filters.add(filter);
    } else if (literal instanceof PeriodDuration) {

      DefaultPeriodDuration duration = (DefaultPeriodDuration) literal;

      // Extract the start and end dates from the filter
      Date end = Calendar.getInstance().getTime();
      Date start = new Date(end.getTime() - duration.getTimeInMillis());

      temporalSearch = new TemporalFilter(start, end);

      filters.add(filter);
    } else if (literal instanceof Date) {
      //     logger.info("Temporal filters is java.util.Date");
      Date start = (Date) literal;
      logger.info("Filter date = " + start);
      temporalSearch = new TemporalFilter(start, new Date());
    } else {
      //    logger.info("NOT SURE WHAT THE LITERAL IS");
      //   logger.info("Literal::" + literal.getClass());
    }
  }


  private enum NestedTypes {
    /** And nested types. */
    AND,
    /** Or nested types. */
    OR,
    /** Not nested types. */
    NOT
  }
}
