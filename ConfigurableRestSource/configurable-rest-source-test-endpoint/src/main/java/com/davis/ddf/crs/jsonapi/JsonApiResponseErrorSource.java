package com.davis.ddf.crs.jsonapi;

/**
 * The type Json api response error source.
 */
public class JsonApiResponseErrorSource {

  /**
   * The Pointer.
   */
  private String pointer;
  /**
   * The Parameter.
   */
  private String parameter;

  /**
   * Gets pointer.
   *
   * @return the pointer
   */
  public String getPointer() {
    return pointer;
  }

  /**
   * Sets pointer.
   *
   * @param pointer the pointer
   */
  public void setPointer(String pointer) {
    this.pointer = pointer;
  }

  /**
   * Gets parameter.
   *
   * @return the parameter
   */
  public String getParameter() {
    return parameter;
  }

  /**
   * Sets parameter.
   *
   * @param parameter the parameter
   */
  public void setParameter(String parameter) {
    this.parameter = parameter;
  }
}