package com.davis.ddf.test.parsing;

import com.davis.ddf.test.fedSource.datamodel.UniversalFederatedSourceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This software was created for the Open Architecture Distributed Common Ground System
 * Modernization All rights to this software belong to  appropriate licenses and restrictions apply.
 * Created by Samuel Davis on 5/5/16.
 */

public class ResultHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultHolder.class);

    public static final String dateFormatPattern = "yyyy-MM-dd'T'HH:mm:ssZ";

    private SimpleDateFormat dateFormat;
    private List<String> title;
    private List<String> serial;
    private List<String> summary;
    private List<String> reportLink;
    private List<String> date;
    private List<String> classification;
    private List<String> unit;
    private List<Double> lat;
    private List<Double> lng;
    private List<String> primaryEvent;
    private List<String> location;
    public ResultHolder(List<String> title,
        List<String> serial,
        List<String> summary,
        List<String> reportLink,
        List<String> date,
        List<String> classification,
        List<String> unit,
        List<Double> lat,
        List<Double> lng,
        List<String> primaryEvent,
        List<String> location) {
        this.title = title;
        this.serial = serial;
        this.summary = summary;
        this.reportLink = reportLink;
        this.date = date;
        this.classification = classification;
        this.unit = unit;
        this.primaryEvent = primaryEvent;
        this.location = location;
        this.lat = lat;
        this.lng = lng;
        dateFormat = new SimpleDateFormat(dateFormatPattern);

    }

    public ResultHolder() {
        dateFormat = new SimpleDateFormat(dateFormatPattern);

    }

    public int getListSize() {
        int result = 0;
        if (title != null) {
            if (result < title.size()) {
                result = title.size();
            }
        }
        if (serial != null) {
            if (result < serial.size()) {
                result = serial.size();
            }
        }
        if (summary != null) {
            if (result < summary.size()) {
                result = summary.size();
            }
        }
        if (unit != null) {
            if (result < unit.size()) {
                result = unit.size();
            }
        }
        if (primaryEvent != null) {
            if (result < primaryEvent.size()) {
                result = primaryEvent.size();
            }
        }
        if (classification != null) {
            if (result < classification.size()) {
                result = classification.size();
            }
        }
        if (date != null) {
            if (result < date.size()) {
                result = date.size();
            }
        }
        if (lat != null) {
            if (result < lat.size()) {
                result = lat.size();
            }
        }
        if (lng != null) {
            if (result < lng.size()) {
                result = lng.size();
            }
        }
        if (reportLink != null) {
            if (result < reportLink.size()) {
                result = reportLink.size();
            }
        }
        if(location != null){
            if (result < location.size()) {
                result = location.size();
            }
        }
        return result;
    }


    public ArrayList<UniversalFederatedSourceResponse> buildSourceReponses() {
        ArrayList<UniversalFederatedSourceResponse> responseList = new ArrayList<UniversalFederatedSourceResponse>();

        int maxListSize = getListSize();
        int position =0;
        while (position < maxListSize) {
            responseList.add(buildResponse(position));
            position = position + 1;
        }
        if (serial == null) {
            for (UniversalFederatedSourceResponse fedResponse : responseList) {
                double one = ThreadLocalRandom.current().nextDouble(0, 9999999);
                double two = ThreadLocalRandom.current().nextDouble(0, 9999999);
                double three = ThreadLocalRandom.current().nextDouble(0, 9999999);
                String randomSerial = "RandomSerial-";
                fedResponse.setDisplaySerial(randomSerial + String.valueOf(one) + String.valueOf(two) + String.valueOf(three));
            }

        }

        return responseList;
    }


    private UniversalFederatedSourceResponse buildResponse(int position) {
        UniversalFederatedSourceResponse response = new UniversalFederatedSourceResponse();
        if (title != null ) {
            response.setDisplayTitle(title.get(position));
        }
        if (serial != null ) {
            response.setDisplaySerial(serial.get(position));

        }
        if (summary != null ) {
            response.setSummary(summary.get(position));

        }
        if (unit != null ) {
            response.setOriginatorUnit(unit.get(position));

        }
        if (primaryEvent != null ) {
            response.setPrimaryEventType(primaryEvent.get(position));

        }
        if (classification != null ) {
            response.setClassification(classification.get(position));

        }
        if (date != null ) {
            Date theDate = null;
            try {
                theDate = dateFormat.parse(date.get(position));
                response.setDateOccurred(theDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            response.setDateOccurred(theDate);
        }
        if (lat != null ) {
            LOGGER.debug("Lat = {}",lat.get(position));
            response.setLatitude(lat.get(position));
        }
        if (lng != null ) {
            LOGGER.debug("Lng = {}",lng.get(position));

            response.setLongitude(lng.get(position));
        }
        if (reportLink != null ) {
            response.setReportLink(reportLink.get(position));
        }
        return response;
    }


    public List<String> getTitle() {
        return this.title;
    }

    public void setTitle(List<String> title) {
        this.title = title;
    }

    public List<String> getSerial() {
        return serial;
    }

    public void setSerial(List<String> serial) {
        this.serial = serial;
    }

    public List<String> getSummary() {
        return summary;
    }

    public void setSummary(List<String> summary) {
        this.summary = summary;
    }

    public List<String> getReportLink() {
        return reportLink;
    }

    public void setReportLink(List<String> reportLink) {
        this.reportLink = reportLink;
    }

    public List<String> getDate() {
        return date;
    }

    public void setDate(List<String> date) {
        this.date = date;
    }

    public List<String> getClassification() {
        return classification;
    }

    public void setClassification(List<String> classification) {
        this.classification = classification;
    }

    public List<String> getUnit() {
        return unit;
    }

    public void setUnit(List<String> unit) {
        this.unit = unit;
    }

    public List<Double> getLat() {
        return lat;
    }

    public void setLat(List<Double> lat) {
        this.lat = lat;
    }

    public List<Double> getLng() {
        return lng;
    }

    public void setLng(List<Double> lng) {
        this.lng = lng;
    }

    public List<String> getPrimaryEvent() {
        return primaryEvent;
    }

    public void setPrimaryEvent(List<String> primaryEvent) {
        this.primaryEvent = primaryEvent;
    }

    public List<String> getLocation() {
        return location;
    }

    public void setLocation(List<String> location) {
        this.location = location;
    }
}

