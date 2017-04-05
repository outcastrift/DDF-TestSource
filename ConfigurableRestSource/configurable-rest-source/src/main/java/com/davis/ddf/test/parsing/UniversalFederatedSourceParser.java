package com.davis.ddf.test.parsing;




import com.davis.ddf.test.fedSource.UniversalFederatedSource;
import com.davis.ddf.test.fedSource.datamodel.UniversalFederatedSourceResponse;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.helpers.DefaultHandler;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class UniversalFederatedSourceParser extends DefaultHandler {
    public static final int JSON = 0;
    public static final int SAX = 1;
    public static final String dateFormatPattern = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final Logger LOGGER = LoggerFactory.getLogger(UniversalFederatedSourceParser.class);

    private static HashMap<String, String> fedResultsMap = new HashMap<String, String>();
    private UniversalFederatedSourceResponse UniversalFederatedSourceResponse;
    private SimpleDateFormat dateFormat;
    private ArrayList<UniversalFederatedSourceResponse> universalFederatedSourceResponses;
    private Document document;
    private DOMImplementation domImpl;
    private Node myCurrentNode;
    private HashMap<String, Document> docs = new HashMap<String, Document>();
    private HashMap<String, String> stringDocs = new HashMap<String, String>();
    private StringBuilder docAsString;
    private StringBuilder buffer = new StringBuilder();
    private int mode;
    private UniversalFederatedSource source;
    public UniversalFederatedSourceParser(int modeForContent, UniversalFederatedSource source) {
        this.source = source;
        universalFederatedSourceResponses = new ArrayList<UniversalFederatedSourceResponse>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        mode = modeForContent;
        if (mode == JSON) {
            LOGGER.debug("Parsing with JSON");
            try {

                DocumentBuilder builder = factory.newDocumentBuilder();
                domImpl = builder.getDOMImplementation();
                if (domImpl != null) {
                    document = domImpl.createDocument(null, null, null);
                    myCurrentNode = document;
                    UniversalFederatedSourceResponse = null;
                    docs = new HashMap<String, Document>();
                }
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
        } else if (mode == SAX) {
            LOGGER.debug("Parsing with CONTENT_AS_STRING");
            stringDocs = new HashMap<String, String>();
            docAsString = new StringBuilder();
        }
        dateFormat = new SimpleDateFormat(dateFormatPattern);
    }

    public static Date convertAfghanDateToLocalDate(String date, SimpleDateFormat dateFormat) {
        String transformedDate = null;
        Date dateAsReported = null;
        try {
            dateAsReported = dateFormat.parse(date);
        } catch (ParseException e) {
            LOGGER.warn("Unable to parse date: " + date);
        }

        Calendar c = Calendar.getInstance();
        TimeZone localTimeZone = c.getTimeZone();
        TimeZone afgTimeZone = TimeZone.getTimeZone("Asia/Kabul");

        //gives you the current offset from UTC for local time zone
        int localOffsetFromUTC = localTimeZone.getRawOffset();
        //gives you the current offset from UTC for afghan time zone
        int afghanOffsetFromUTC = afgTimeZone.getRawOffset();
        LOGGER.debug("local offset is " + localOffsetFromUTC);
        LOGGER.debug("AFG offset is " + afghanOffsetFromUTC);
        //create a new calendar in GMT timezone, set to this date and add the offset
        Calendar localCal = Calendar.getInstance();
        if (dateAsReported != null) {
            localCal.setTimeInMillis(dateAsReported.getTime());
            localCal.add(Calendar.MILLISECOND, (-1 * afghanOffsetFromUTC));
            localCal.add(Calendar.MILLISECOND, localOffsetFromUTC);
            LOGGER.debug("Original AFG Date: " + date);
            LOGGER.debug("Created local date as [" + dateFormat.format(localCal.getTime()) + "]");
            return localCal.getTime();
        } else {
            return null;
        }

    }



    public String getRestResultMetadata(String id) {
        return fedResultsMap.get(id);
    }



    public ArrayList<UniversalFederatedSourceResponse> getUniversalFederatedSourceResponses() {
        return universalFederatedSourceResponses;
    }

    // Add a new text PI in the DOM tree, at the right place.
    public void processingInstruction(String target, String data) {
        try {
            if (mode == JSON) {
                ProcessingInstruction pi = document.createProcessingInstruction(target, data);
                myCurrentNode.appendChild(pi);
            }
        } catch (Exception ex) {
            LOGGER.error(ex.toString());
        }
    }



    public ArrayList<UniversalFederatedSourceResponse> getObjectsFromJson(String filteredJson) {
        //Create the POJOs
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(filteredJson);
       // LOGGER.debug("Document = {}", document);
        ResultHolder responseList = getListsFromDocument(document);
        ArrayList<UniversalFederatedSourceResponse> responses = responseList.buildSourceReponses();
        return responses;

    }




    private ResultHolder getListsFromDocument(Object document) {
        ResultHolder results = new ResultHolder();
        if (source.getSsWktStringParam() != null && !source.getSsWktStringParam().equalsIgnoreCase("null")
                &&!source.getSsWktStringParam().trim().equalsIgnoreCase("")) {
            LOGGER.trace("getSsWktStringParam {}", source.getSsWktStringParam());
            List<String> wktArray = JsonPath.read(document, source.getSsWktStringParam());
            LOGGER.trace("wktArray size =  {}", wktArray.size());

            results.setLocation(wktArray);
        }
        if (source.getSsReportLink() != null && !source.getSsReportLink().equalsIgnoreCase("null")
                &&!source.getSsReportLink().trim().equalsIgnoreCase("")) {
            LOGGER.trace("getSsReportLink {}", source.getSsReportLink());
            List<String> reportArray = JsonPath.read(document, source.getSsReportLink());
            results.setReportLink(reportArray);
        }
        if (source.getSsDisplayTitle() != null && !source.getSsDisplayTitle().equalsIgnoreCase("null")
                &&!source.getSsDisplayTitle().trim().equalsIgnoreCase("")) {
            LOGGER.trace("getSsDisplayTitle {}", source.getSsDisplayTitle());
            List<String> displayTitleArray = JsonPath.read(document, source.getSsDisplayTitle());
            results.setTitle(displayTitleArray);
        }
        if (source.getSsDisplaySerial() != null && !source.getSsDisplaySerial().equalsIgnoreCase("null")
                &&!source.getSsDisplaySerial().trim().equalsIgnoreCase("")) {
            LOGGER.trace("getSsDisplaySerial {}", source.getSsDisplaySerial());
            List<String> displaySerialArray = JsonPath.read(document, source.getSsDisplaySerial());
        results.setSerial(displaySerialArray);

        }
        if (source.getSsSummary() != null && !source.getSsSummary().equalsIgnoreCase("null")
                &&!source.getSsSummary().trim().equalsIgnoreCase("")) {
            LOGGER.trace("getSsSummary {}", source.getSsSummary());
            List<String> summaryArray = JsonPath.read(document, source.getSsSummary());
        results.setSummary(summaryArray);

        }
        if (source.getSsOriginatorUnit() != null && !source.getSsOriginatorUnit().equalsIgnoreCase("null")
                &&!source.getSsOriginatorUnit().trim().equalsIgnoreCase("")) {
            LOGGER.trace("getSsOriginatorUnit {}", source.getSsOriginatorUnit());
            List<String> orginatorArray = JsonPath.read(document, source.getSsOriginatorUnit());
            results.setUnit(orginatorArray);

        }
        if (source.getSsPrimaryEventType() != null && !source.getSsPrimaryEventType().equalsIgnoreCase("null")
                &&!source.getSsPrimaryEventType().trim().equalsIgnoreCase("")) {
            LOGGER.trace("getSsPrimaryEventType {}", source.getSsPrimaryEventType());
            List<String> primaryEventArray = JsonPath.read(document, source.getSsPrimaryEventType());
         results.setPrimaryEvent(primaryEventArray);
        }
        if (source.getSsClassification() != null && !source.getSsClassification().equalsIgnoreCase("null")
                &&!source.getSsClassification().trim().equalsIgnoreCase("")) {
            LOGGER.trace("getSsClassification {}", source.getSsClassification());
            List<String> classificationArray = JsonPath.read(document, source.getSsClassification());
           results.setClassification(classificationArray);

        }
        if (source.getSsDateOccurred() != null && !source.getSsDateOccurred().equalsIgnoreCase("null")
                &&!source.getSsDateOccurred().trim().equalsIgnoreCase("")) {
            LOGGER.trace("getSsDateOccurred {}", source.getSsDateOccurred());
            List<String> dateArray = JsonPath.read(document, source.getSsDateOccurred());
           results.setDate(dateArray);

        }
        if (source.getSsLatitude() != null && !source.getSsLatitude().equalsIgnoreCase("null")
                &&!source.getSsLatitude().trim().equalsIgnoreCase("")) {
            LOGGER.trace("getSsLatitude {}", source.getSsLatitude());
            List<Double> latArray = JsonPath.read(document, source.getSsLatitude());
            results.setLat(latArray);
        }
        if (source.getSsLongitude() != null && !source.getSsLongitude().equalsIgnoreCase("null")
                &&!source.getSsLongitude().trim().equalsIgnoreCase("")) {
            LOGGER.trace("getSsLongitude {}", source.getSsLongitude());
            List<Double> lngArray = JsonPath.read(document, source.getSsLongitude());
           results.setLng(lngArray);
        }

        return results;
    }


    public String extractJSONPath(String jsonString, String jsonPath) throws Exception {
        Object jsonPathResult = JsonPath.read(jsonString, jsonPath);
        if (null == jsonPathResult) {
            LOGGER.debug("Invalid Json Path Provided for Path {}",jsonPath);
            throw new Exception("Invalid JSON path provided!");
        } else if (jsonPathResult instanceof List && ((List<?>) jsonPathResult).isEmpty()) {
            LOGGER.debug("JsonPath result returned a empty list.");
            return "NULL";
        } else {
            LOGGER.debug("JsonPath was successful");
            return jsonPathResult.toString();
        }
    }










}

