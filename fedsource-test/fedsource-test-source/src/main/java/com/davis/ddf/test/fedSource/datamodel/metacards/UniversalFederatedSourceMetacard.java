package com.davis.ddf.test.fedSource.datamodel.metacards;

/**
 * Created by hduser on 6/3/15.
 */


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.impl.MetacardImpl;


/**
 * The type Test federated source metacard.
 */
public class UniversalFederatedSourceMetacard extends MetacardImpl {
    /**
     * The constant LOGGER.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UniversalFederatedSourceMetacard.class);
    /**
     * The constant serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
  //  private static String namespace="ddf.davis.com";


    /**
     * Instantiates a new Test federated source metacard.
     */
    public UniversalFederatedSourceMetacard(){
        super(new UniversalFederatedSourceMetacardType());

    }




}