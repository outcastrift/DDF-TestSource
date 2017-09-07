package com.davis.ddf.crs.data;

import java.util.TreeMap;

/**
 * This software was created for rights to this software belong to appropriate licenses and
 * restrictions apply.
 *
 * @author Samuel Davis created on 9/7/17.
 */
public class StoredSequentialQuery {

  private TreeMap<Integer, GroovyResponseObject> query;

  /** Instantiates a new Stored sequential query. */
  public StoredSequentialQuery() {
    query = new TreeMap<>();
  }

  public GroovyResponseObject getObjectInStoredQuery(Integer objectId) {
    return query.get(objectId);
  }

  public Boolean addObjectToQuery(Integer objectId, GroovyResponseObject object) {
    boolean result = false;
    if (object != null) {
      query.put(objectId, object);
      result = true;
    }
    return result;
  }
  /**
   * Gets query.
   *
   * @return the query
   */
  public TreeMap<Integer, GroovyResponseObject> getQuery() {
    return query;
  }

  /**
   * Sets query.
   *
   * @param query the query
   */
  public void setQuery(TreeMap<Integer, GroovyResponseObject> query) {
    this.query = query;
  }
}
