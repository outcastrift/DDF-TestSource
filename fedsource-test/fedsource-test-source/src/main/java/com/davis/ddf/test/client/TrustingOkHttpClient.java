package com.davis.ddf.test.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

/**
 * Created by Samuel Davis on 7/26/16.
 */
public class TrustingOkHttpClient {

  public TrustingOkHttpClient(){

  }

  private KeyStore readKeyStore(String keyStorePath,String clientCertPassword){

      KeyStore ks = null;
      InputStream in = null;
      try {
        ks = KeyStore.getInstance("PKCS12");
        //ks = KeyStore.getInstance(KeyStore.getDefaultType());
        // get user password and file input stream
        if(TrustingOkHttpClient.class.getClassLoader().getResourceAsStream(keyStorePath) == null){
          File initialFile = new File(keyStorePath);
          in = new FileInputStream(initialFile);
        }else{
          in = TrustingOkHttpClient.class.getClassLoader().getResourceAsStream(keyStorePath);
        }

        char[] password = clientCertPassword.toCharArray();
        ks.load(in, password);
      } catch (KeyStoreException | NoSuchAlgorithmException | IOException | CertificateException e) {
        e.printStackTrace();
      } finally {
        if (in != null) {
          try {
            in.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
      return ks;
    }


  public OkHttpClient getUnsafeOkHttpClient(int readTimeout,
                                            int connectTimeout,
                                            String clientCertPath,
                                            String certPassword) {
    try {
      // Create a trust manager that does not validate certificate chains
      final TrustManager[] trustAllCerts = new TrustManager[] {
          new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
              return new java.security.cert.X509Certificate[]{};

            }
          }
      };
      KeyStore keyStore = readKeyStore(clientCertPath,certPassword);
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

      HostnameVerifier hostnameVerifier =new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      };
      builder.hostnameVerifier(hostnameVerifier);
      builder.connectTimeout(connectTimeout,TimeUnit.SECONDS);
      builder.readTimeout(readTimeout,TimeUnit.SECONDS);
      return builder.build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
