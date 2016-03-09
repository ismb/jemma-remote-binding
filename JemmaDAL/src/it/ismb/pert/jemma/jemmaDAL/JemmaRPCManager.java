package it.ismb.pert.jemma.jemmaDAL;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Consts;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.NameValuePair;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecFactory;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.cookie.BasicClientCookie2;
import org.apache.http.impl.cookie.CookieSpecBase;
import org.apache.http.message.BasicNameValuePair;

import it.ismb.pert.jemma.jemmaDAL.Jemma.JemmaAuthenticator;

public class JemmaRPCManager 
{	
	private Map<Jemma,List<Appliance>> devicesMap = new HashMap<Jemma,List<Appliance>>();	
	JemmaCredentialsProvider  credentialsProvider = null;

	public JemmaRPCManager(List<URI> jemmasEndpoints, JemmaCredentialsProvider provider)
	{
		Jemma jemmaInstance;
		JemmaAuthenticator authenticator = null;
		credentialsProvider = provider;
				
		for(URI jemmaURI : jemmasEndpoints)
		{
			if(credentialsProvider != null)
				authenticator = credentialsProvider.submitCredential(jemmaURI);
			jemmaInstance = new Jemma(jemmaURI, ( authenticator == null) ? new JemmaAuthenticator() : authenticator);
			
			try 
			{
				jemmaInstance.connect();
				
			} catch (InvalidCredentialsException | IOException e) 
			{
				e.printStackTrace();
			}
			devicesMap.put(jemmaInstance, new ArrayList<Appliance>());
		}
		
	}


	/**
	 * get devices from each JEMMA instance
	 * 
	 * @return list of all devices configured on each JEMMA instance  
	 */
	public synchronized List<Appliance> getAllDevices()
	{
		List<Appliance> jemmasDevices = new ArrayList<Appliance>();	

		retrieveDevices();

		for(List<Appliance> jemmaDevices : devicesMap.values())
			jemmasDevices.addAll(jemmaDevices);

		return jemmasDevices;
	}
	
	public synchronized List<Appliance> getDevices(Jemma jemmaInstance)
	{
		return devicesMap.get(jemmaInstance);
	}
	

	private  void retrieveDevices()
	{				
		for(Jemma jemmaInstance : devicesMap.keySet())		
		{
			List<Appliance> jemmaDevicesList =  devicesMap.get(jemmaInstance);
			
			try 
			{
				List<Appliance> currentDevices = jemmaInstance.getDevices();
				jemmaDevicesList.clear();
				jemmaDevicesList.addAll(currentDevices);
				
			} catch (MethodNotSupportedException | IOException | InvalidCredentialsException e) {
				e.printStackTrace();
			}					
		}
	}
	
	
	
	
	public interface JemmaCredentialsProvider
	{
		/**
		 * Callback to provide JEMMA authentication credentials
		 * 
		 * @param jemmaURI
		 */
		public JemmaAuthenticator submitCredential(URI jemmaURI);
	}	
}
