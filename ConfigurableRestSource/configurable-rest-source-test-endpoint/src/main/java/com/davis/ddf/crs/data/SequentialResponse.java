package com.davis.ddf.crs.data;

import java.util.Map;

/**
 * This software was created for rights to this software belong to appropriate licenses and
 * restrictions apply.
 *
 * @author Samuel Davis created on 9/7/17.
 */
public class SequentialResponse {
  private Map<Integer, GroovyResponseObject> results;
  private Integer startRow;
  private Integer endRow;
  private String queryUri;
  private String requestId;
  private Integer pageSize;
  private Integer totalResults;

  /**
   * Instantiates a new Sequential response.
   *
   * @param startRow the start row
   * @param endRow the end row
   * @param queryUri the query uri
   * @param requestId the request id
   * @param pageSize the page size
   * @param results the results
   */
  public SequentialResponse(
      Integer startRow,
      Integer endRow,
      String queryUri,
      String requestId,
      Integer pageSize,
      Integer totalResults,
      Map<Integer, GroovyResponseObject> results) {
    setResults(results);
    setRequestId(requestId);
    setEndRow(endRow);
    setStartRow(startRow);
    setQueryUri(queryUri);
    setPageSize(pageSize);
    setTotalResults(totalResults);
  }

  public Integer getTotalResults() {
    return totalResults;
  }

  public void setTotalResults(Integer totalResults) {
    this.totalResults = totalResults;
  }

  /**
   * Gets page size.
   *
   * @return the page size
   */
  public Integer getPageSize() {
    return pageSize;
  }

  /**
   * Sets page size.
   *
   * @param pageSize the page size
   */
  public void setPageSize(Integer pageSize) {
    this.pageSize = pageSize;
  }

  /**
   * Gets results.
   *
   * @return the results
   */
  public Map<Integer, GroovyResponseObject> getResults() {
    return results;
  }

  /**
   * Sets results.
   *
   * @param results the results
   */
  public void setResults(Map<Integer, GroovyResponseObject> results) {
    this.results = results;
  }

  /**
   * Gets start row.
   *
   * @return the start row
   */
  public Integer getStartRow() {
    return startRow;
  }

  /**
   * Sets start row.
   *
   * @param startRow the start row
   */
  public void setStartRow(Integer startRow) {
    this.startRow = startRow;
  }

  /**
   * Gets end row.
   *
   * @return the end row
   */
  public Integer getEndRow() {
    return endRow;
  }

  /**
   * Sets end row.
   *
   * @param endRow the end row
   */
  public void setEndRow(Integer endRow) {
    this.endRow = endRow;
  }

  /**
   * Gets query uri.
   *
   * @return the query uri
   */
  public String getQueryUri() {
    return queryUri;
  }

  /**
   * Sets query uri.
   *
   * @param queryUri the query uri
   */
  public void setQueryUri(String queryUri) {
    this.queryUri = queryUri;
  }

  /**
   * Gets request id.
   *
   * @return the request id
   */
  public String getRequestId() {
    return requestId;
  }

  /**
   * Sets request id.
   *
   * @param requestId the request id
   */
  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }
}
