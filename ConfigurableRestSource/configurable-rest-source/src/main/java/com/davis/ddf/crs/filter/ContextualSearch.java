package com.davis.ddf.crs.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contextual Search.
 *
 * @author Bob Harrod
 * @version 1.0
 */
public class ContextualSearch {
  private List<String> keywords;

  private String searchPhrase;

  private String selectors;

  private boolean isCaseSensitive;

  /**
   * Default Constructor.
   */
  public ContextualSearch() {
    setKeywords(new ArrayList<String>());
  }

  /**
   * Constructor.
   *
   * @param selectors Selectors for search.
   * @param searchPhrase The Search Phrase.
   * @param isCaseSensitive Whether the search is case sensitive or not.
   */
  public ContextualSearch(String selectors, String searchPhrase, boolean isCaseSensitive) {
    super();
    this.selectors = selectors;
    this.searchPhrase = searchPhrase;
    this.isCaseSensitive = isCaseSensitive;
  }

  /**
   * Get the search phrase.
   *
   * @return The search phrase.
   */
  public String getSearchPhrase() {
    return searchPhrase;
  }

  /**
   * Set the search phrase.
   *
   * @param searchPhrase The search phrase.
   */
  public void setSearchPhrase(String searchPhrase) {
    this.searchPhrase = searchPhrase;
  }

  /**
   * Get Selectors.
   *
   * @return The selectors.
   */
  public String getSelectors() {
    return selectors;
  }

  /**
   * Set Selectors.
   *
   * @param selectors The selectors.
   */
  public void setSelectors(String selectors) {
    this.selectors = selectors;
  }

  /**
   * Is the search case sensitive?
   *
   * @return True or False
   */
  public boolean isCaseSensitive() {
    return isCaseSensitive;
  }

  /**
   * Set the case sensitivity of the search.
   *
   * @param isCaseSensitive True or false
   */
  public void setCaseSensitive(boolean isCaseSensitive) {
    this.isCaseSensitive = isCaseSensitive;
  }

  /**
   * @return the keywords
   */
  private List<String> getKeywords() {
    return keywords;
  }

  /**
   * @param keywords the keywords to set
   */
  private void setKeywords(List<String> keywords) {
    this.keywords = keywords;
  }

  /**
   * Add a keyword.
   *
   * @param keyword A Keyword
   */
  public void addKeyword(String keyword) {
    if (!getKeywords().contains(keyword)) {
      getKeywords().add(keyword);
    }
  }

  /**
   * Remove a keyword.
   *
   * @param keyword A keyword.
   * @return A true or false indicating that the keyword that was removed.
   */
  public boolean removeKeyword(String keyword) {
    boolean isRemoved = false;
    if (getKeywords().contains(keyword)) {
      isRemoved = getKeywords().remove(keyword);
    }
    return isRemoved;
  }

  /**
   * Add a search phrase.
   *
   * @param phrase A Search phrase
   */
  private void addKeywordsFromPhrase(String phrase) {
    String[] terms = null;

    if (phrase != null) {
      terms = phrase.split("\\s*(,|\\s)\\s*");
    }

    if (terms != null) {
      Collections.addAll(getKeywords(), terms);
    }

  }

}

