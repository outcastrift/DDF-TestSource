println("Entering Results")
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.http.HttpResponse
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.socket.LayeredConnectionSocketFactory
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.json.JSONObject
import org.json.XML
import org.slf4j.LoggerFactory

import javax.net.ssl.*
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

def logger = LoggerFactory.getLogger("NSA-Pulse-Groovy-Result-Script")

CloseableHttpClient getUnsafeClient(
        int readTimeout, int connectTimeout, String clientCertPath, String certPassword) {
    def logger = LoggerFactory.getLogger("NSA-Pulse-Groovy-Result-Script")

    try {

        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = [

                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(
                            X509Certificate[] chain, String authType)
                            throws CertificateException {}

                    @Override
                    public void checkServerTrusted(
                            X509Certificate[] chain, String authType)
                            throws CertificateException {}

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        X509Certificate[] certArray = []
                        return certArray
                    }
                }

        ]
        logger.info("Creating Keystore for Client Cert Path of  {}", clientCertPath)

        KeyStore keyStore = readKeyStore(clientCertPath, certPassword)

        // Install the all-trusting trust manager
        KeyManagerFactory kmf =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())

        kmf.init(keyStore, certPassword.toCharArray())

        KeyManager[] keyManagers = kmf.getKeyManagers()

        final SSLContext sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagers, trustAllCerts, new SecureRandom())

        //Timeout Parameters
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setConnectionRequestTimeout(connectTimeout)
                .setSocketTimeout(readTimeout)
                .build()
        //Start Building Our Client
        HttpClientBuilder builder = HttpClients.custom()
        LayeredConnectionSocketFactory sslSocketFactory =
                new SSLConnectionSocketFactory(sslContext,
                        SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER)

        builder.setSSLSocketFactory(sslSocketFactory)
        builder.setDefaultRequestConfig(config)
        HostnameVerifier hostnameVerifier =
                new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {

                        return true
                    }
                }
        builder.setSSLHostnameVerifier(hostnameVerifier)
        logger.info("Successfully created CloseableHttpClient")
        CloseableHttpClient client = builder.build()
        return client
    } catch (Exception e) {
        logger.error("Exception creating trusting okhttp client {}", e)
        throw new RuntimeException(e)
    }
}

KeyStore readKeyStore(String keyStorePath, String clientCertPassword) {
    def logger = LoggerFactory.getLogger("NSA-Pulse-Groovy-Result-Script")

    KeyStore ks = null
    InputStream inputStream = null
    try {
        ks = KeyStore.getInstance("PKCS12")

        logger.info("Successfully got instance of keystore PKCS12")

        logger.info("Loading client certificate from path of {}", keyStorePath)

        File initialFile = new File(keyStorePath)

        inputStream = FileUtils.openInputStream(initialFile)

        logger.info("Successfully read client certificate from absolute path and input stream. ")

        char[] password = clientCertPassword.toCharArray()

        ks.load(inputStream, password)

    } catch (KeyStoreException | NoSuchAlgorithmException | IOException | CertificateException e) {
        logger.error("Error encountered while attempting to read the keystore specified. {}", e)
    } finally {
        if (inputStream != null) {
            try {
                IOUtils.closeQuietly(inputStream)
            } catch (IOException e) {
                logger.error("Error loading certificate {}", e)
            }
        }
    }
    return ks
}

output = []
def metacard = [:]
def jsonSlurper = new JsonSlurper()
logger.info("Input JSON = {}", input)
def inputObj = jsonSlurper.parseText(input)
logger.info("Groovy Object = {} ", inputObj)
boolean performAdditionalQuery = false
if (inputObj?.data != null) {
    CloseableHttpClient client = getUnsafeClient(30000, 30000, "etc/certs/localhost.p12", "changeit")
    int startIndex =inputObj.data.startRow
    logger.info("startRow = {}", startIndex)
    int endIndex =inputObj.data.endRow
    logger.info("endRow = {}", endIndex)
    int pageSize = inputObj.data.pageSize
    logger.info("pageSize = {}", pageSize)
    int totalResults = inputObj.data.totalResults
    logger.info("totalResults = {}", totalResults)
    String queryUri = inputObj.data.queryUri
    URI uri = new URI(queryUri)
    logger.info("URI = {}", uri)
    //build query URL
    String base = uri.toString()
    String[] baseSplit = base.split("\\?")
    String baseQuery = baseSplit[0]
    String queryString = baseSplit[1]
    String[] queryParts = queryString.split("&")
    boolean firstPart = true
    for (String part : queryParts) {
        if (!part.contains("startRow") && part.contains("endRow")) {
            if (firstPart) {
                baseQuery = baseQuery + "?" + part
                firstPart = false
            } else {
                baseQuery = baseQuery + "&" + part
            }
        }
    }
    //handles initial results returned
    inputObj?.data?.results?.eachWithIndex { queryResult, resultIdx ->
        logger.info("RESULT ${queryResult}")
        metacard = [:]
        metacard['id'] = "testResult_metacard" + queryResult['lat'] + queryResult['lng']
        metacard['title'] = queryResult['title']
        metacard['location'] = queryResult['location']

        metacard['Summary'] = ""
        if (result.title) {
            metacard['Summary'] += "Title: ${queryResult.title}<br/>"
        }
        if (result.lat) {
            metacard['Summary'] += "Latitude: ${queryResult.lat}<br/>"
        }
        if (result.lng) {
            metacard['Summary'] += "Longitude: ${queryResult.lng}<br/>"
        }
        JSONObject wJson = new JSONObject(JsonOutput.toJson(result))
        metacard.metadata = "<metacard><metadata>" + XML.toString(wJson) + "</metadata></metacard>"
        output.push(metacard)
    }

    while (totalResults > metacard.size()) {
        int requestStartRow = startIndex + pageSize
        int requestEndingRow = endIndex + pageSize
        startIndex = requestStartRow
        endIndex = requestEndingRow

        String callUrl = baseQuery + "&startRow=" + requestStartRow + "&endRow=" + requestEndingRow
        logger.info("Additional Query URL to Call = {}", callUrl)
        HttpGet request = new HttpGet(callUrl)
        HttpResponse response = client.execute(request)
        if (response.getStatusLine().getStatusCode() == 200) {

            def getResponse = jsonSlurper.parseText(
                    EntityUtils.toString(
                            response.getEntity(),
                            "UTF-8")
            )
            logger.info("New Query = {} ", getResponse)
            if (getResponse?.data?.results != null) {
                getResponse?.data?.results?.eachWithIndex { result, id ->
                    logger.info("RESULT ${result}")
                    metacard = [:]
                    metacard['id'] = "testResult_metacard" + result['lat'] + result['lng']
                    metacard['title'] = result['title']
                    metacard['location'] = result['location']

                    metacard['Summary'] = ""
                    if (result.title) {
                        metacard['Summary'] += "Title: ${result.title}<br/>"
                    }
                    if (result.lat) {
                        metacard['Summary'] += "Latitude: ${result.lat}<br/>"
                    }
                    if (result.lng) {
                        metacard['Summary'] += "Longitude: ${result.lng}<br/>"
                    }
                    JSONObject wJson = new JSONObject(JsonOutput.toJson(result))
                    metacard.metadata = "<metacard><metadata>" + XML.toString(wJson) + "</metadata></metacard>"
                    output.push(metacard)
                }
            } else {
                logger.info("Http Get response was NULL")
            }
        } else {
            logger.info("Http Get Response failed with error code of {}", response.getStatusLine().getStatusCode())
        }
    }


} else {
    logger.debug("results was null")
}




output = (JsonOutput.toJson(output))
