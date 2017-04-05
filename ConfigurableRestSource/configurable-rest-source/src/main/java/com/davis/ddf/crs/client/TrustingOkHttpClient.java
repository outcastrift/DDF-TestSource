package com.davis.ddf.crs.client;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

/** Created by Samuel Davis on 7/26/16. */
public class TrustingOkHttpClient {
  private static final Logger logger =
      LoggerFactory.getLogger(TrustingOkHttpClient.class.getName());
  private OkHttpClient client;

  public TrustingOkHttpClient() {}

  public OkHttpClient getUnsafeOkHttpClient(
      int readTimeout, int connectTimeout, String clientCertPath, String certPassword) {
    try {
      // Create a trust manager that does not validate certificate chains
      final TrustManager[] trustAllCerts =
          new TrustManager[] {
              new X509TrustManager() {
                @Override
                public void checkClientTrusted(
                    java.security.cert.X509Certificate[] chain, String authType)
                    throws CertificateException {}

                @Override
                public void checkServerTrusted(
                    java.security.cert.X509Certificate[] chain, String authType)
                    throws CertificateException {}

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                  return new java.security.cert.X509Certificate[] {};
                }
              }
          };
      KeyStore keyStore = null;
      if (clientCertPath == null || certPassword == null) {
        clientCertPath = "certs/embedded.p12";
        logger.info(
            "Client cert path or password were null creating keystore from jar resources  {}",
            clientCertPath);

        certPassword = "changeit";
        keyStore = readKeyStoreJarResources(clientCertPath, certPassword);

      } else {
        logger.info("Creating Keystore for Client Cert Path of  {}", clientCertPath);
        keyStore = readKeyStore(clientCertPath, certPassword);
      }

      // Install the all-trusting trust manager
      KeyManagerFactory kmf =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(keyStore, certPassword.toCharArray());
      KeyManager[] keyManagers = kmf.getKeyManagers();
      final SSLContext sslContext = SSLContext.getInstance("TLS");
      //added a Key manager authenticate the Client.
      sslContext.init(keyManagers, trustAllCerts, new java.security.SecureRandom());
      // Create an ssl socket factory with our all-trusting manager
      final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

      OkHttpClient.Builder builder = new OkHttpClient.Builder();

      builder.sslSocketFactory(sslSocketFactory);

      HostnameVerifier hostnameVerifier =
          new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
              return true;
            }
          };
      builder.hostnameVerifier(hostnameVerifier);
      builder.connectTimeout(connectTimeout, TimeUnit.SECONDS);
      builder.readTimeout(readTimeout, TimeUnit.SECONDS);
      logger.info("Successfully created UnsafeOkHttpClient");
      client = builder.build();

      return client;
    } catch (Exception e) {
      logger.error("Exception creating trusting okhttp client {}", e);
      throw new RuntimeException(e);
    }
  }

  private KeyStore readKeyStoreJarResources(String keyStorePath, String clientCertPassword) {
    KeyStore ks = null;
    InputStream in = null;
    try {
      ks = KeyStore.getInstance("PKCS12");
      logger.info("Successfully got instance of keystore PKCS12");
      //ks = KeyStore.getInstance(KeyStore.getDefaultType());
      // get user password and file input stream
      in = TrustingOkHttpClient.class.getClassLoader().getResourceAsStream(keyStorePath);
      OutputStream outputStream = new FileOutputStream("deploy/embedded.p12");
      IOUtils.copy(in, outputStream);
      in.close();
      outputStream.close();
      in=null;
      in = FileUtils.openInputStream(new File("deploy/embedded.p12"));
      logger.info("Successfully read client certificate from JAR resources ");
      char[] password = clientCertPassword.toCharArray();
      ks.load(in, password);
    } catch (KeyStoreException | NoSuchAlgorithmException | IOException | CertificateException e) {
      e.printStackTrace();
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          logger.error("Error loading certificate {}", e);
        }
      }
    }
    return ks;
  }

  private KeyStore readKeyStore(String keyStorePath, String clientCertPassword) {

    KeyStore ks = null;
    InputStream in = null;

    try {
      ks = KeyStore.getInstance("PKCS12");
      logger.info("Successfully got instance of keystore PKCS12");
      //ks = KeyStore.getInstance(KeyStore.getDefaultType());
      // get user password and file input stream

      logger.info("Loading client certificate from path of {}", keyStorePath);

      File initialFile = new File(keyStorePath);
      in = FileUtils.openInputStream(initialFile);
      logger.info("Successfully read client certificate from absolute path and input stream. ");

      char[] password = clientCertPassword.toCharArray();
      ks.load(in, password);
    } catch (KeyStoreException | NoSuchAlgorithmException | IOException | CertificateException e) {
      e.printStackTrace();
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          logger.error("Error loading certificate {}", e);
        }
      }
    }
    return ks;
  }
}
