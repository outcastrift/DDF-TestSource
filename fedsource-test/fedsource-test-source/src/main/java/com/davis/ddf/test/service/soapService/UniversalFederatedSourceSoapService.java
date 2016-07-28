package com.davis.ddf.test.service.soapService;

import com.davis.ddf.test.fedSource.datamodel.UniversalFederatedSourceResponse;
import okhttp3.HttpUrl;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.davis.ddf.test.service.SourceService;
import com.davis.ddf.test.parsing.UniversalFederatedSourceParser;


/**
 * Created by hduser on 7/20/15.
 */
public class UniversalFederatedSourceSoapService implements SourceService {
    /**
     * The constant LOGGER.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UniversalFederatedSourceSoapService.class);
    /**
     * The constant MODULE.
     */
    private static final String MODULE = "\\$\\{Module\\}";
    /**
     * The constant REPORT_TYPE.
     */
    private static final String REPORT_TYPE = "\\$\\{ReportType\\}";
    /**
     * The constant FORMAT.
     */
    private static final String FORMAT = "\\$\\{Format\\}";
    /**
     * The constant VERSION.
     */
    private static final String VERSION = "\\$\\{Version\\}";
    /**
     * The constant START_DATE.
     */
    private static final String START_DATE = "\\$\\{StartDate\\}";
    /**
     * The constant END_DATE.
     */
    private static final String END_DATE = "\\$\\{EndDate\\}";
    /**
     * The constant DATE_TYPE.
     */
    private static final String DATE_TYPE = "\\$\\{DateType\\}";
    /**
     * The constant AOR_ATTR.
     */
    private static final String AOR_ATTR = "\\$\\{AORAttr\\}";
    /**
     * The constant AOR_VALUE.
     */
    private static final String AOR_VALUE = "\\$\\{AORValue\\}";
    /**
     * The constant MAX_RETURN.
     */
    private static final String MAX_RETURN = "\\$\\{MaxReturn\\}";
    /**
     * The constant SHOW_DEP.
     */
    private static final String SHOW_DEP = "\\$\\{ShowDeprecated\\}";
    /**
     * The constant ID.
     */
    private static final String ID = "\\$\\{ID\\}";
    /**
     * The constant TOP_LEFT_LAT_LONG.
     */
    private static final String TOP_LEFT_LAT_LONG = "\\$\\{TopLeftLatLong\\}";
    /**
     * The constant BOTTOM_RIGHT_LAT_LONG.
     */
    private static final String BOTTOM_RIGHT_LAT_LONG = "\\$\\{BottomRightLatLong\\}";
    /**
     * The Url.
     */
    private String url;
    /**
     * The Path to trust cert.
     */
    private String pathToTrustCert;
    /**
     * The Parser.
     */
    private UniversalFederatedSourceParser parser;
    /**
     * The Post.
     */
    private HttpPost post;
    /**
     * The Client.
     */
    private CloseableHttpClient client;
    /**
     * The Reports unmarshaller.
     */
    private Unmarshaller reportsUnmarshaller;
    /**
     * The Query by date file.
     */
    private String queryByDateFile;
    /**
     * The Query by box file.
     */
    private String queryByBoxFile;
    /**
     * The Query by id file.
     */
    private String queryByIdFile;
    /**
     * The Reports context.
     */
    private JAXBContext reportsContext = null;
    /**
     * The Mode.
     */
    private int mode;

    /**
     * Instantiates a new Test federated source soap service.
     *
     * @param url  the url
     * @param mode the mode
     */
    public UniversalFederatedSourceSoapService(String url, int mode) {
        this(url, null, mode);


    }

    /**
     * Instantiates a new Test federated source soap service.
     *
     * @param url       the url
     * @param trustCert the trust cert
     * @param mode      the mode
     */
    public UniversalFederatedSourceSoapService(String url, String trustCert, int mode) {
        this.mode = mode;
        pathToTrustCert = trustCert;
        this.url = url;
        LOGGER.debug("Creating TFR SOAP Service for " + this.url);
        String strSoapAction = "";
        parser = new UniversalFederatedSourceParser(mode,null);
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.useSystemProperties();
        LOGGER.debug("Path to Trust: " + pathToTrustCert);
        if (url.startsWith("https") && pathToTrustCert != null) {
            this.configureTrustStore(builder);
        }
        client = builder.build();
        post = new HttpPost(url);
        post.setHeader("SOAPAction", strSoapAction);
        readQueryFile();

    }

    /**
     * Read query file.
     */
    private void readQueryFile() {

        LOGGER.debug("READING XML QUERY FILE");
        String strXMLFilename = "/soapXml/GetTfrReportsById.xml";
        BufferedReader br = null;
        StringBuilder contents = new StringBuilder();
        try {

            String sCurrentLine;

            br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(strXMLFilename)));

            while ((sCurrentLine = br.readLine()) != null) {
                contents.append(sCurrentLine);

            }

        } catch (Exception e) {
            LOGGER.error(e.toString());
            e.printStackTrace();
        }
        securelyCloseStream(br);
        queryByIdFile = contents.toString();
        LOGGER.debug("READING GetTfrReportsByDate.xml QUERY FILE");

        strXMLFilename = "/soapXml/GetTfrReportsByDate.xml";
        br = null;
        contents = new StringBuilder();
        try {

            String sCurrentLine;

            br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(strXMLFilename)));

            while ((sCurrentLine = br.readLine()) != null) {
                contents.append(sCurrentLine);

            }

        } catch (Exception e) {
            LOGGER.error(e.toString());
            e.printStackTrace();
        }
        securelyCloseStream(br);

        queryByDateFile = contents.toString();
        LOGGER.debug("READING GetTfrReportsByBoundingBox.xml QUERY FILE");

        strXMLFilename = "/soapXml/GetTfrReportsByBoundingBox.xml";
        br = null;
        contents = new StringBuilder();
        try {

            String sCurrentLine;

            br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(strXMLFilename)));

            while ((sCurrentLine = br.readLine()) != null) {
                contents.append(sCurrentLine);

            }

        } catch (Exception e) {
            LOGGER.error(e.toString());
            e.printStackTrace();
        }
        securelyCloseStream(br);

        queryByBoxFile = contents.toString();
    }









    /**
     * Gets tfr reports by date.
     *
     * @param module     the module
     * @param reportType the report type
     * @param format     the format
     * @param wsVersion  the ws version
     * @param startDate  the start date
     * @param endDate    the end date
     * @param dateType   the date type
     * @param aorAttr    the aor attr
     * @param aorValue   the aor value
     * @param maxReturn  the max return
     * @param showDep    the show dep
     * @return the tfr reports by date
     */
    public ArrayList<UniversalFederatedSourceResponse> getResultsByDate(String module,
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
                                                               String searchParams) {


        LOGGER.debug("REPLACING QUERY PARAMS");
        String fileString = queryByDateFile;
        //   String fileString = contents.toString();
        fileString = fileString.replaceAll(MODULE, module);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("After replacement " + queryByDateFile);
        fileString = fileString.replaceAll(REPORT_TYPE, reportType);
        fileString = fileString.replaceAll(FORMAT, format);
        fileString = fileString.replaceAll(VERSION, wsVersion);
        fileString = fileString.replaceAll(START_DATE, startDate);
        fileString = fileString.replaceAll(END_DATE, endDate);
        fileString = fileString.replaceAll(DATE_TYPE, dateType);
        fileString = fileString.replaceAll(AOR_ATTR, aorAttr);
        fileString = fileString.replaceAll(AOR_VALUE, aorValue);
        fileString = fileString.replaceAll(MAX_RETURN, maxReturn);
        fileString = fileString.replaceAll(SHOW_DEP, showDep);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Query: " + fileString);
        }

        // Prepare HTTP post
        // Request content will be retrieved directly
        // from the input stream
        HttpEntity entity = new StringEntity(fileString, ContentType.APPLICATION_XML);
        post.setEntity(entity);
        // consult documentation for your web service
        //  builder.setUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko");
        // Execute request
        InputStream stream = null;
        CloseableHttpResponse response = null;
        try {

            long elapsed = System.currentTimeMillis();
            response = client.execute(post);
            elapsed = System.currentTimeMillis() - elapsed;
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("WebService responded in " + elapsed + " milliseconds");
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("RECIEVED RESPONSE: " + response.toString());
            }

            SAXParser saxParser = getSAXParser();
            LOGGER.debug("Parsing");
            stream = response.getEntity().getContent();
            if (saxParser != null) {
                saxParser.parse(stream, parser);
                securelyCloseStream(response);
                return parser.getUniversalFederatedSourceResponses();
            } else {
                securelyCloseStream(response);
                return new ArrayList<UniversalFederatedSourceResponse>();

            }
        } catch (Exception e) {
            LOGGER.error(e.toString());

        } finally {
            if (response != null) {
                securelyCloseStream(response);
            }
        }
        securelyCloseStream(stream);
        return new ArrayList<UniversalFederatedSourceResponse>();
    }

    /**
     * Gets tfr objects by id.
     *
     * @param module     the module
     * @param reportType the report type
     * @param format     the format
     * @param wsVersion  the ws version
     * @param reportId   the report id
     * @param maxReturn  the max return
     * @param showDep    the show dep
     * @return the tfr objects by id
     */
    public ArrayList<UniversalFederatedSourceResponse> getTfrObjectsById(String module,
                                                                String reportType,
                                                                String format,
                                                                String wsVersion,
                                                                String reportId,
                                                                String maxReturn,
                                                                String showDep,
                                                                String searchParams) {


        LOGGER.debug("REPLACING QUERY PARAMS");
        //   String fileString = contents.toString();
        String fileString = queryByIdFile;
        fileString = fileString.replaceAll(MODULE, module);
        fileString = fileString.replaceAll(REPORT_TYPE, reportType);
        fileString = fileString.replaceAll(FORMAT, format);
        fileString = fileString.replaceAll(VERSION, wsVersion);
        fileString = fileString.replaceAll(ID, reportId);
        fileString = fileString.replaceAll(MAX_RETURN, maxReturn);
        fileString = fileString.replaceAll(SHOW_DEP, showDep);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Query: " + fileString);
        }

        // Prepare HTTP post

        // Request content will be retrieved directly
        // from the input stream
        HttpEntity entity = new StringEntity(fileString, ContentType.APPLICATION_XML);
        post.setEntity(entity);
        // consult documentation for your web service

        //  builder.setUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko");

        InputStream stream = null;
        CloseableHttpResponse response = null;

        // Execute request
        try {

            long elapsed = System.currentTimeMillis();
            response = client.execute(post);
            elapsed = System.currentTimeMillis() - elapsed;
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("WebService responded in " + elapsed + " milliseconds");
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("RECIEVED RESPONSE: " + response.toString());
            }

            SAXParser saxParser = getSAXParser();
            if (parser != null) {
                LOGGER.debug("Parsing");
                stream = response.getEntity().getContent();
                saxParser.parse(stream, parser);
                securelyCloseStream(response);
                return parser.getUniversalFederatedSourceResponses();
            } else {

                return new ArrayList<UniversalFederatedSourceResponse>();
            }

            //   System.out.println(post.getResponseBodyAsString());
        } catch (Exception e) {
            LOGGER.error(e.toString());

        } finally {
            // Release current connection to the connection pool once you are done
            securelyCloseStream(response);
            //  if(post !=null)
            //    post.releaseConnection();
        }
        securelyCloseStream(stream);
        return new ArrayList<UniversalFederatedSourceResponse>();
    }

    /**
     * Configure trust store.
     *
     * @param builder the builder
     */
    private void configureTrustStore(HttpClientBuilder builder) {
        LOGGER.debug("CONFIGURING TFR TRUST STORE WITH: " + pathToTrustCert);
        KeyStore truststore = null;
        try {
            truststore = KeyStore.getInstance(KeyStore.getDefaultType());
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        try {
            if (truststore != null) {
                truststore.load(null, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }

        BufferedInputStream bis = null;
        try {

            bis = new BufferedInputStream(getClass().getResourceAsStream(pathToTrustCert));
        } catch (Exception ex) {
            LOGGER.error(ex.toString());

        }


        CertificateFactory cf = null;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            e.printStackTrace();
        }

        try {

            while (bis != null && bis.available() > 0) {
                if (cf != null && truststore != null) {
                    Certificate cert = cf.generateCertificate(bis);
                    truststore.setCertificateEntry("trustedCert", cert);
                    LOGGER.debug("Loaded Trust store cert");
                }
                //   System.out.println(cert.toString());
            }
            securelyCloseStream(bis);
        } catch (IOException e) {
            securelyCloseStream(bis);
            LOGGER.debug("Could not load as resource trying file...");
            File file = new File(pathToTrustCert);


            FileInputStream fis = null;
            //     try {
            try {
                fis = new FileInputStream(file);
                bis = new BufferedInputStream(fis);
                LOGGER.debug("Created BufferedInputStream");
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }

            try {
                while (bis != null && bis.available() > 0) {
                    if (cf != null && truststore != null) {
                        Certificate cert = cf.generateCertificate(bis);
                        truststore.setCertificateEntry("trustedCert", cert);
                        LOGGER.debug("Loaded Trust store cert");
                    }
                    //   System.out.println(cert.toString());
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (CertificateException e1) {
                e1.printStackTrace();
            } catch (KeyStoreException e1) {
                e1.printStackTrace();
            }

            //    } catch (Exception ex) {
            //       LOGGER.error(e.toString());
            //        e.printStackTrace();
            //     }

            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        securelyCloseStream(bis);
        TrustManagerFactory tmf = null;
        try {
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        TrustManager[] tms = null;
        try {
            if (tmf != null) {
                tmf.init(truststore);
                tms = tmf.getTrustManagers();
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }


        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // SSLContext sslContext = SSLContext.getDefault();
        try {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(null, null);
            if (sslContext != null && builder != null) {
                sslContext.init(kmf.getKeyManagers(), tms, new SecureRandom());

                builder.setSSLContext(sslContext);
            }
            LOGGER.debug("Set Truststore");
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        securelyCloseStream(bis);

    }

    /**
     * Gets sax parser.
     *
     * @return the sax parser
     */
    private SAXParser getSAXParser() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            SAXParser saxParser = factory.newSAXParser();
            return saxParser;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXNotRecognizedException e) {
            e.printStackTrace();
        } catch (SAXNotSupportedException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * Gets tfr objects by bounding box.
     *
     * @param module             the module
     * @param reportType         the report type
     * @param format             the format
     * @param wsVersion          the ws version
     * @param startDate          the start date
     * @param endDate            the end date
     * @param dateType           the date type
     * @param topLeftLatLong     the top left lat long
     * @param bottomRightLatLong the bottom right lat long
     * @param maxReturn          the max return
     * @param showDep            the show dep
     * @return the tfr objects by bounding box
     */
    public ArrayList<UniversalFederatedSourceResponse> getResultsByBoundingBox(String module,
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
                                                                      String searchParams) {
        String strSoapAction = "";
        LOGGER.debug("REPLACING QUERY PARAMS");
        String fileString = queryByBoxFile;
        //  String fileString = contents.toString();
        fileString = fileString.replaceAll(MODULE, module);
        fileString = fileString.replaceAll(REPORT_TYPE, reportType);
        fileString = fileString.replaceAll(FORMAT, format);
        LOGGER.debug("Replacing version with " + wsVersion);
        fileString = fileString.replaceAll(VERSION, wsVersion);
        fileString = fileString.replaceAll(START_DATE, startDate);
        fileString = fileString.replaceAll(END_DATE, endDate);
        fileString = fileString.replaceAll(DATE_TYPE, dateType);
        fileString = fileString.replaceAll(TOP_LEFT_LAT_LONG, topLeftLatLong);
        fileString = fileString.replaceAll(BOTTOM_RIGHT_LAT_LONG, bottomRightLatLong);
        fileString = fileString.replaceAll(MAX_RETURN, maxReturn);
        fileString = fileString.replaceAll(SHOW_DEP, showDep);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Query: " + fileString);
        }
        // Prepare HTTP post

        // Request content will be retrieved directly
        // from the input stream
        HttpEntity entity = new StringEntity(fileString, ContentType.APPLICATION_XML);
        post.setEntity(entity);
        InputStream stream = null;
        CloseableHttpResponse response = null;
        try {

            long elapsed = System.currentTimeMillis();
            response = client.execute(post);
            elapsed = System.currentTimeMillis() - elapsed;
            LOGGER.debug("WebService responded in " + elapsed + " milliseconds");
            SAXParser saxParser = getSAXParser();
            LOGGER.debug("Parsing");
            stream = response.getEntity().getContent();
            if (saxParser != null) {
                saxParser.parse(stream, parser);
            }
            securelyCloseStream(stream);
            if (parser != null) {

                return parser.getUniversalFederatedSourceResponses();
            } else {
                return new ArrayList<UniversalFederatedSourceResponse>();
            }
            //   System.out.println(post.getResponseBodyAsString());
        } catch (Exception e) {
            LOGGER.error(e.toString());

        } finally {
            securelyCloseStream(response);
            // Release current connection to the connection pool once you are done
            // post.releaseConnection();
        }
        securelyCloseStream(stream);
        return new ArrayList<UniversalFederatedSourceResponse>();
    }


    //Method only has value to the Rest Services
    @Override
    public ArrayList<UniversalFederatedSourceResponse> getResultsForQuery(HttpUrl queryParams) {
        return null;
    }

    /**
     * Gets path to trust cert.
     *
     * @return the path to trust cert
     */
    public String getPathToTrustCert() {
        return pathToTrustCert;
    }


    /**
     * Sets path to trust cert.
     *
     * @param s the s
     */
    public void setPathToTrustCert(String s) {
        pathToTrustCert = s;
    }

    /**
     * Securely close stream.
     *
     * @param stream the stream
     */
    private void securelyCloseStream(Closeable stream) {
        try {
            if (stream != null) {
                stream.close();
            }
        } catch (IOException ex) {
            LOGGER.debug("Unable to close stream: " + ex);
        }
    }



}
