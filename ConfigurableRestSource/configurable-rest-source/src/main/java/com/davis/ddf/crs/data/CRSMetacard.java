package com.davis.ddf.crs.data;

/**
 * Created by hduser on 6/3/15.
 */


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.impl.MetacardImpl;


/**
 * The type Test federated source metacard.
 */
public class CRSMetacard extends MetacardImpl {
    /**
     * The constant LOGGER.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CRSMetacard.class);
    /**
     * The constant serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
  //  private static String namespace="ddf.davis.com";


    /**
     * Instantiates a new Test federated source metacard.
     */
    public CRSMetacard(){
        super(new CRSMetacardType());

    }




}