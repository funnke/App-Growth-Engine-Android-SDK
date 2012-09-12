package com.hookmobile.age;

import static com.hookmobile.age.AgeConstants.E_SUCCESS;
import static com.hookmobile.age.AgeConstants.P_DESCRIPTION;
import static com.hookmobile.age.AgeConstants.P_STATUS;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

class AgeClient {
	
	private static final int timeoutConnection	= 30000;
	private static final int timeoutSocket		= 30000;
	
	private static HttpParams params;
	private static SchemeRegistry registry;
	
	
	static {
		params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "utf-8");
		HttpProtocolParams.setUseExpectContinue(params, false);
		HttpConnectionParams.setConnectionTimeout(params, timeoutConnection);
		HttpConnectionParams.setSoTimeout(params, timeoutSocket);
		
		registry = new SchemeRegistry();
	    registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	    registry.register(new Scheme("https", new EasySSLSocketFactory(), 443));
	}
	
    static AgeResponse doPost(String url, List<NameValuePair> form) throws ClientProtocolException, IOException, JSONException {
		AgeResponse result = new AgeResponse();
		
		HttpPost post = new HttpPost(url);
		post.setEntity(new UrlEncodedFormEntity(form, HTTP.UTF_8));
		post.addHeader("Content-type", "application/x-www-form-urlencoded");
		
		HttpResponse response = new DefaultHttpClient(new SingleClientConnManager(params, registry), params).execute(post);
		
		if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			String responseText = new String(EntityUtils.toByteArray(response.getEntity()));
			JSONObject json = new JSONObject(responseText);
			result.setJson(json);
			result.setCode(json.isNull(P_STATUS) ? -1 : json.getInt(P_STATUS));
			result.setMessage(json.isNull(P_DESCRIPTION) ? null : json.getString(P_DESCRIPTION));
		}
		else {
			result.setMessage(response.getStatusLine().getReasonPhrase());
		}
		
		return result;
	}
    
    private static class EasyX509TrustManager implements X509TrustManager {  

		private X509TrustManager standardTrustManager = null;
		
		
		public EasyX509TrustManager(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {  
			super();  
			TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			factory.init(keystore);
			TrustManager[] trustmanagers = factory.getTrustManagers();  

			if(trustmanagers.length == 0) {  
				throw new NoSuchAlgorithmException("no trust manager found");  
			}
			this.standardTrustManager = (X509TrustManager)trustmanagers[0];  
		}
		
		public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {  
			standardTrustManager.checkClientTrusted(certificates, authType);  
		}
		
		public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
			int chainLength = certificates.length;
			
			if(certificates.length > 1) {
				int currIndex;
				
				for(currIndex = 0; currIndex < certificates.length; ++currIndex) {
					boolean foundNext = false;
					
					for(int nextIndex = currIndex + 1; nextIndex < certificates.length; ++nextIndex) {
						if(certificates[currIndex].getIssuerDN().equals(certificates[nextIndex].getSubjectDN())) {
							foundNext = true;
							
							if(nextIndex != currIndex + 1) {
								X509Certificate tempCertificate = certificates[nextIndex];
								certificates[nextIndex] = certificates[currIndex + 1];
								certificates[currIndex + 1] = tempCertificate;
							}
							
							break;
						}
					}
					
					if(! foundNext) {
						break;
					}
				}
				
				chainLength = currIndex + 1;
				X509Certificate lastCertificate = certificates[chainLength - 1];
				Date now = new Date();
				
				if(lastCertificate.getSubjectDN().equals(lastCertificate.getIssuerDN()) && now.after(lastCertificate.getNotAfter())) {
					--chainLength;
				}
			}
			
			standardTrustManager.checkServerTrusted(certificates, authType);    
		}  
		
		public X509Certificate[] getAcceptedIssuers() {  
			return this.standardTrustManager.getAcceptedIssuers();  
		}
	}
	
	private static class EasySSLSocketFactory implements SocketFactory, LayeredSocketFactory {  
		
		private SSLContext sslContext = null;  
		
		
		private static SSLContext createEasySSLContext() throws IOException {  
			try {  
				SSLContext context = SSLContext.getInstance("TLS");  
				context.init(null, new TrustManager[] { new EasyX509TrustManager(null) }, null);  
				
				return context;  
			}
			catch(Exception e) {
				throw new IOException(e.getMessage());  
			}  
		}  

		private SSLContext getSSLContext() throws IOException {  
			if(this.sslContext == null) {
				this.sslContext = createEasySSLContext();  
			}
			
			return this.sslContext;  
		}
		
		public Socket connectSocket(
				Socket sock,
				String host,
				int port, 
				InetAddress localAddress,
				int localPort,
				HttpParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
			int connTimeout = HttpConnectionParams.getConnectionTimeout(params);  
			int soTimeout = HttpConnectionParams.getSoTimeout(params);  
			InetSocketAddress remoteAddress = new InetSocketAddress(host, port);  
			SSLSocket sslSock = (SSLSocket) ((sock != null) ? sock : createSocket());  

			if((localAddress != null) || (localPort > 0)) {
				if(localPort < 0) {
					localPort = 0;
				}
				
				InetSocketAddress isa = new InetSocketAddress(localAddress, localPort);  
				sslSock.bind(isa);  
			}
			sslSock.connect(remoteAddress, connTimeout);  
			sslSock.setSoTimeout(soTimeout);  
			
			return sslSock;
		}
		
		public Socket createSocket() throws IOException {  
			return getSSLContext().getSocketFactory().createSocket();  
		}  
		
		public boolean isSecure(Socket socket) throws IllegalArgumentException {  
			return true;  
		}  
		
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
			return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);  
		}  
		
		public boolean equals(Object obj) {  
			return ((obj != null) && obj.getClass().equals(EasySSLSocketFactory.class));  
		}  
		
		public int hashCode() {  
			return EasySSLSocketFactory.class.hashCode();  
		}  
	}
	
    static class AgeResponse {

    	private int code;
    	private String message;
    	private JSONObject json;
    	
    	
    	public int getCode() {
    		return code;
    	}

    	public void setCode(int code) {
    		this.code = code;
    	}

    	public boolean isSuccess() {
    		return code == E_SUCCESS;
    	}
    	
    	public String getMessage() {
    		return message;
    	}

    	public void setMessage(String message) {
    		this.message = message;
    	}

    	public JSONObject getJson() {
    		return json;
    	}

    	public void setJson(JSONObject json) {
    		this.json = json;
    	}
    	
    }
    
}
