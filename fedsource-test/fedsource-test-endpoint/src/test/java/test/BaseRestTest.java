package test;

import com.jayway.restassured.config.SSLConfig;
import com.jayway.restassured.response.Response;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;

@SuppressWarnings("deprecation")
public class BaseRestTest {

  protected final static String ENDPOINT_ADDRESS =
      "https://localhost:8993/services/test/getSourceResults";
  private static final Logger logger = LoggerFactory.getLogger(BaseRestTest.class);
  private final static String WADL_ADDRESS = ENDPOINT_ADDRESS + "?_wadl";

  protected static boolean serverUp = false;
  protected static SSLConfig sslConfig;

  @BeforeClass
  public static void init() {

    try {
      KeyStore keyStore = KeyStore.getInstance("JKS");
      String password = "changeit";
      File truststore = new File("src/test/resources/tstark.jks");
      keyStore.load(new FileInputStream(truststore), password.toCharArray());
      KeyManagerFactory keyFactory =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyFactory.init(keyStore, password.toCharArray());
      KeyManager[] km = keyFactory.getKeyManagers();

      truststore = new File("src/test/resources/serverTruststore.jks");
      keyStore.load(new FileInputStream(truststore), password.toCharArray());
      TrustManagerFactory trustFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustFactory.init(keyStore);
      TrustManager[] tm = trustFactory.getTrustManagers();

      X509HostnameVerifier hostVerifier = new X509HostnameVerifier() {
        @Override public boolean verify(String arg0, SSLSession arg1) {
          return true;
        }

        @Override public void verify(String arg0, SSLSocket arg1) throws IOException {
        }

        @Override public void verify(String arg0, X509Certificate arg1) throws SSLException {
        }

        @Override public void verify(String arg0, String[] arg1, String[] arg2)
            throws SSLException {
        }
      };

      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(km, tm, new SecureRandom());

      SSLSocketFactory sslSocketFactory = new SSLSocketFactory(ctx);

      sslConfig = new SSLConfig().sslSocketFactory(sslSocketFactory)
          .allowAllHostnames()
          .x509HostnameVerifier(hostVerifier);

      Response checkStatus = given()
          .config(newConfig().sslConfig(sslConfig))
          .get(WADL_ADDRESS);

      serverUp = (checkStatus.getStatusCode() == 200);
    } catch (Exception e) {
      serverUp = false;
      logger.warn("There was a problem connecting to DDF.  " + e.getMessage());
    }
  }
}
