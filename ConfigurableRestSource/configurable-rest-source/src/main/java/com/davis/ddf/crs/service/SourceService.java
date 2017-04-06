package com.davis.ddf.crs.service;

import com.davis.ddf.crs.data.CRSResponse;
import java.util.ArrayList;
import okhttp3.HttpUrl;

/**
 * This software was created for the Open Architecture Distributed Common Ground System
 * Modernization All rights to this software belong to  appropriate licenses and restrictions apply.
 * Created by Samuel Davis on 3/22/16.
 */

public interface SourceService {


/*    public  ArrayList<CRSResponse> getResultsByDate(String module,
                                                                String reportType,
                                                                String format,
                                                                String wsVersion,
                                                                String startDate,
                                                                String endDate,
                                                                String dateType,
                                                                String aorAttr,
                                                                String aorValue,
                                                                String maxReturn,
                                                                String showDep,
                                                                String searchParams);

    public ArrayList<CRSResponse> getTfrObjectsById(String module,
                                                                String reportType,
                                                                String format,
                                                                String wsVersion,
                                                                String reportId,
                                                                String maxReturn,
                                                                String showDep,
                                                                String searchParams);

    public ArrayList<CRSResponse> getResultsByBoundingBox(String module,
                                                                      String reportType,
                                                                      String format,
                                                                      String wsVersion,
                                                                      String startDate,
                                                                      String endDate,
                                                                      String dateType,
                                                                      String topLeftLatLong,
                                                                      String bottomRightLatLong,
                                                                      String maxReturn,
                                                                      String showDep,
                                                                      String searchParams);*/
    public ArrayList<CRSResponse> getResultsForQuery(HttpUrl httpUrl);


}
