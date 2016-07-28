package com.davis.ddf.test.fedSource;

import com.vividsolutions.jts.geom.Coordinate;

import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.LikeFilterImpl;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.TContains;
import org.opengis.filter.temporal.TOverlaps;
import org.opengis.temporal.Period;
import org.opengis.temporal.PeriodDuration;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.source.opensearch.ContextualSearch;

/**
 * Created by hduser on 6/2/15.
 */
public class UniversalSourceFilterVisitor extends DefaultFilterVisitor {
    /**
     * The constant logger.
     */
    private static XLogger logger = new XLogger(
                LoggerFactory.getLogger(UniversalSourceFilterVisitor.class));

    /**
     * The constant ONLY_AND_MSG.
     */
    private static final String ONLY_AND_MSG = "Opensearch only supports AND operations for non-contextual criteria.";

    /**
     * The enum Nested types.
     */
    private enum NestedTypes {
        /**
         * And nested types.
         */
        AND, /**
         * Or nested types.
         */
        OR, /**
         * Not nested types.
         */
        NOT
        }

    /**
     * The Filters.
     */
    private List<Filter> filters;

    /**
     * The Contextual search.
     */
// Can only have one each of each type of filter in an OpenSearch query
        private ContextualSearch contextualSearch;

    /**
     * The Temporal search.
     */
    private TemporalFilter temporalSearch;

    /**
     * The Spatial search.
     */
    private SpatialFilter spatialSearch;

    /**
     * The Cur nest.
     */
    private NestedTypes curNest = null;

    /**
     * Instantiates a new Test source filter visitor.
     */
    public UniversalSourceFilterVisitor() {

            super();

            filters = new ArrayList<Filter>();

            contextualSearch = null;
            temporalSearch = null;
            spatialSearch = null;
        }

    /**
     * Visit object.
     *
     * @param filter the filter
     * @param data   the data
     * @return the object
     */
    @Override
        public Object visit(Not filter, Object data) {
            Object newData;
            NestedTypes parentNest = curNest;
            logger.debug("ENTERING: NOT filter");
            curNest = NestedTypes.NOT;
            filters.add(filter);
            newData = super.visit(filter, data);
            curNest = parentNest;
            logger.debug("EXITING: NOT filter");

            return newData;
        }

    /**
     * Visit object.
     *
     * @param filter the filter
     * @param data   the data
     * @return the object
     */
    @Override
        public Object visit(Or filter, Object data) {
            Object newData;
            NestedTypes parentNest = curNest;
         //   logger.debug("ENTERING: OR filter");
            curNest = NestedTypes.OR;
            filters.add(filter);
            newData = super.visit(filter, data);
            curNest = parentNest;
        //    logger.debug("EXITING: OR filter");

            return newData;
        }

    /**
     * Visit object.
     *
     * @param filter the filter
     * @param data   the data
     * @return the object
     */
    @Override
        public Object visit(And filter, Object data) {
            Object newData;
            NestedTypes parentNest = curNest;
       //     logger.debug("ENTERING: AND filter");
            curNest = NestedTypes.AND;
            filters.add(filter);
            newData = super.visit(filter, data);
            curNest = parentNest;
       //     logger.debug("EXITING: AND filter");

            return newData;
        }

    /**
     * DWithin filter maps to a Point/Radius distance Spatial search criteria.
     *
     * @param filter the filter
     * @param data   the data
     * @return the object
     */
    @Override
        public Object visit(DWithin filter, Object data) {
      //      logger.debug("ENTERING: DWithin filter");
            if (curNest == null || NestedTypes.AND.equals(curNest)) {
                // The geometric point is wrapped in a <Literal> element, so have to
                // get geometry expression as literal and then evaluate it to get
                // the geometry.
                // Example:
                // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl@dc33f184</ogc:Literal>
                Literal literalWrapper = (Literal) filter.getExpression2();

                // Luckily we know what type the geometry expression should be, so
                // we
                // can cast it
                com.vividsolutions.jts.geom.Point point = (com.vividsolutions.jts.geom.Point) literalWrapper.evaluate(null);
                Coordinate[] crds = point.getCoordinates();
//            double[] coords = point.getCentroid().getCoordinate();
                double distance = filter.getDistance();

           //     logger.debug("point: coords[0] = " + crds[0].y + ",   coords[1] = " + crds[0].y);
          //      logger.debug("radius = " + distance);

                this.spatialSearch = new SpatialDistanceFilter(crds[0].x, crds[0].y, distance);

                filters.add(filter);
            } else {
                logger.warn(ONLY_AND_MSG);
            }

      //      logger.debug("EXITING: DWithin filter");

            return super.visit(filter, data);
        }

    /**
     * Contains filter maps to a Polygon or BBox Spatial search criteria.
     *
     * @param filter the filter
     * @param data   the data
     * @return the object
     */
    @Override
        public Object visit(Contains filter, Object data) {
     //       logger.debug("ENTERING: Contains filter");
            if (curNest == null || NestedTypes.AND.equals(curNest)) {
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

                //    logger.debug("geometryWkt = [" + geometryWkt.toString() + "]");

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

    /**
     * Intersects filter maps to a Polygon or BBox Spatial search criteria.
     *
     * @param filter the filter
     * @param data   the data
     * @return the object
     */
    @Override
        public Object visit(Intersects filter, Object data) {
            logger.debug("ENTERING: Intersects filter");
            if (curNest == null || NestedTypes.AND.equals(curNest)) {
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

               //     logger.debug("geometryWkt = [" + geometryWkt.toString() + "]");

                    filters.add(filter);
                } else if(geometryExpression instanceof com.vividsolutions.jts.geom.Polygon){

                    com.vividsolutions.jts.geom.Polygon polygon = (com.vividsolutions.jts.geom.Polygon)geometryExpression;
               //     logger.info("POLYGON::" + polygon);
                    this.spatialSearch = new SpatialFilter(polygon.toString());
                    filters.add(filter);

                } else {
                    logger.warn("visit.Intersets :: Only POLYGON geometry WKT for Intersects filter is supported");
                }
            } else {
                logger.warn(ONLY_AND_MSG);
            }

        //    logger.debug("EXITING: Intersects filter");

            return super.visit(filter, data);
        }

    /**
     * TOverlaps filter maps to a Temporal (Absolute and Offset) search criteria.
     *
     * @param filter the filter
     * @param data   the data
     * @return the object
     */
    @Override
        public Object visit(TOverlaps filter, Object data) {
        //    logger.info("ENTERING: TOverlaps filter");
            if (curNest == null || NestedTypes.AND.equals(curNest)) {
                handleTemporal(filter);
            } else {
                logger.warn(ONLY_AND_MSG);
            }
       //     logger.debug("EXITING: TOverlaps filter");

            return super.visit(filter, data);
        }

    /**
     * Visit object.
     *
     * @param filter the filter
     * @param data   the data
     * @return the object
     */
    @Override
        public Object visit(TContains filter, Object data) {
        //    logger.debug("ENTERING: TContains filter");
            if (curNest == null || NestedTypes.AND.equals(curNest)) {
                handleTemporal(filter);
            } else {
                logger.warn(ONLY_AND_MSG);
            }
        //    logger.debug("EXITING: TContains filter");

            return super.visit(filter, data);
        }

    /**
     * Visit object.
     *
     * @param filter the filter
     * @param data   the data
     * @return the object
     */
    @Override
        public Object visit(Before filter, Object data) {
      //      logger.debug("ENTERING: Before filter");
            if (curNest == null || NestedTypes.AND.equals(curNest)) {
                //handleTemporal(filter);
                Literal literalWrapper = (Literal) filter.getExpression2();
                Object literal = literalWrapper.evaluate(null);
                if( literal instanceof Date ){
                    if( temporalSearch == null )
                        temporalSearch = new TemporalFilter(0);

                    temporalSearch.setEndDate((Date)literal);
                }
            } else {
                logger.warn(ONLY_AND_MSG);
            }
       //     logger.debug("EXITING: Before filter");

            return super.visit(filter, data);
        }

    /**
     * Visit object.
     *
     * @param filter the filter
     * @param data   the data
     * @return the object
     */
    @Override
        public Object visit(After filter, Object data) {
       //     logger.debug("ENTERING: After filter");
            if (curNest == null || NestedTypes.AND.equals(curNest)) {
                //handleTemporal(filter);
                Literal literalWrapper = (Literal) filter.getExpression2();
                Object literal = literalWrapper.evaluate(null);
                if( literal instanceof Date ){
                    if( temporalSearch == null )
                        temporalSearch = new TemporalFilter(0);

                    temporalSearch.setStartDate((Date)literal);
                }
            } else {
                logger.warn(ONLY_AND_MSG);
            }
        //    logger.debug("EXITING: After filter");

            return super.visit(filter, data);
        }

    /**
     * During filter maps to a Temporal (Absolute and Offset) search criteria.
     *
     * @param filter the filter
     * @param data   the data
     * @return the object
     */
    @Override
        public Object visit(During filter, Object data) {
       //     logger.debug("ENTERING: TOverlaps filter");
            if (curNest == null || NestedTypes.AND.equals(curNest)) {
                handleTemporal(filter);
            } else {
                logger.warn(ONLY_AND_MSG);
            }
     //       logger.debug("EXITING: TOverlaps filter");

            return super.visit(filter, data);
        }

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
            } else if( literal instanceof Date) {
           //     logger.info("Temporal filters is java.util.Date");
                Date start = (Date)literal;
                logger.info("Filter date = " + start);
                temporalSearch = new TemporalFilter(start, new Date());
            } else {
            //    logger.info("NOT SURE WHAT THE LITERAL IS");
             //   logger.info("Literal::" + literal.getClass());
            }
        }

    /**
     * PropertyIsEqualTo filter maps to a Type/Version(s) search criteria.
     *
     * @param filter the filter
     * @param data   the data
     * @return the object
     */
    @Override
        public Object visit(PropertyIsEqualTo filter, Object data) {
      //      logger.debug("ENTERING: PropertyIsEqualTo filter");

            filters.add(filter);

     //       logger.debug("EXITING: PropertyIsEqualTo filter");

            return super.visit(filter, data);
        }

    /**
     * PropertyIsLike filter maps to a Contextual search criteria.
     *
     * @param filter the filter
     * @param data   the data
     * @return the object
     */
    @Override
        public Object visit(PropertyIsLike filter, Object data) {
       //     logger.debug("ENTERING: PropertyIsLike filter");

            LikeFilterImpl likeFilter = (LikeFilterImpl) filter;

            AttributeExpressionImpl expression = (AttributeExpressionImpl) likeFilter.getExpression();
            String selectors = expression.getPropertyName();
        //    logger.debug("selectors = " + selectors);

            String searchPhrase = likeFilter.getLiteral();
       //     logger.debug("searchPhrase = [" + searchPhrase + "]");
            if (contextualSearch != null) {
                contextualSearch.setSearchPhrase(contextualSearch.getSearchPhrase() + " "
                        + curNest.toString() + " " + searchPhrase);
            } else {
                contextualSearch = new ContextualSearch(selectors, searchPhrase,
                        likeFilter.isMatchingCase());
            }

       //     logger.debug("EXITING: PropertyIsLike filter");

            return super.visit(filter, data);
        }

    /**
     * Visit object.
     *
     * @param expression the expression
     * @param data       the data
     * @return the object
     */
    @Override
        public Object visit(PropertyName expression, Object data) {
        //    logger.debug("ENTERING: PropertyName expression");

            // countOccurrence( expression );

         //   logger.debug("EXITING: PropertyName expression");

            return data;
        }

    /**
     * Visit object.
     *
     * @param expression the expression
     * @param data       the data
     * @return the object
     */
    @Override
        public Object visit(Literal expression, Object data) {
        //    logger.debug("ENTERING: Literal expression");

            // countOccurrence( expression );

        //    logger.debug("EXITING: Literal expression");

            return data;
        }

    /**
     * Gets filters.
     *
     * @return the filters
     */
    public List<Filter> getFilters() {
            return filters;
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
     * Gets temporal search.
     *
     * @return the temporal search
     */
    public TemporalFilter getTemporalSearch() {
            return temporalSearch;
        }

    /**
     * Gets spatial search.
     *
     * @return the spatial search
     */
    public SpatialFilter getSpatialSearch() {
            return spatialSearch;
        }

    }


