package test;

import com.davis.ddf.test.client.TrustingOkHttpClient;
import com.jayway.restassured.config.SSLConfig;
import com.jayway.restassured.response.Response;

import ddf.catalog.data.Result;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.junit.BeforeClass;
import org.junit.Test;
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

	private static final Logger logger = LoggerFactory.getLogger(BaseRestTest.class);
	
	protected final static String ENDPOINT_ADDRESS = "https://localhost:8993/services/test/getSourceResults";
	private final static String WADL_ADDRESS = ENDPOINT_ADDRESS + "?_wadl";
	
	protected static boolean serverUp = false;
	protected static SSLConfig sslConfig;
	protected static OkHttpClient client;
	@BeforeClass
	public static void init() {
		client = new TrustingOkHttpClient().getUnsafeOkHttpClient(15,15,"certs/tstark-cert.p12","changeit");

	}
	@Test
	public void test(){
		HttpUrl.Builder builder = new HttpUrl.Builder();
		HttpUrl httpUrl = finalizeUrl(builder);
	}
	private HttpUrl finalizeUrl(HttpUrl.Builder builder){

		int port  =0;
		String ssServiceUrl = "https://localhost:8993/services/test/getSourceResults";
		HttpUrl a =HttpUrl.parse(ssServiceUrl);

		String[] parseUrl = ssServiceUrl.split("/");
		String protocol = parseUrl[0].substring(0,parseUrl[0].length()-1);
		String host =null;
		if(parseUrl[2].contains(":")){
			String[] hostString = parseUrl[2].split(":");
			host = hostString[0];
			port = Integer.valueOf(hostString[1]);
		}else{
			host = parseUrl[2];
		}
		builder.scheme(protocol);
		ArrayList<String> paths = new ArrayList<String>();
		for(int x =3; x < parseUrl.length; x++){
			paths.add(parseUrl[x]);
		}
		builder.host(host);
		if(port > 0){
			builder.port(port);
		}
		for(String s : paths){
			builder.addPathSegment(s);
		}

		return builder.build();
	}

	

        

}
