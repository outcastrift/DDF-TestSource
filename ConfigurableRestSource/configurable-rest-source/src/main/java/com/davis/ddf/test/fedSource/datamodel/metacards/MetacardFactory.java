package com.davis.ddf.test.fedSource.datamodel.metacards;

import org.json.JSONObject;

import ddf.catalog.data.impl.MetacardImpl;

/**
 * A Metacard Factory helps us dynamically create Metacards.
 *
 * @author Bob Harrod
 * @version 1.0
 */
public interface MetacardFactory {
  /**
   * Create a Metacard.
   *
   * @return A Metacard.
   */
  MetacardImpl create();

  /**
   * Create a Metacard given a JSON String
   *
   * @param json the json
   * @return metacard
   */
  MetacardImpl createFromJson(String json);

  /**
   * Create a Metacard given a JSON Object
   *
   * @param json the json
   * @return metacard
   */
  MetacardImpl createFromJson(JSONObject json);
}
