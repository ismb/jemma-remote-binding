package it.ismb.pert.jemma.jemmaDAL;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.NameValuePair;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Jemma 
{
	private URI jemmaURI;	
	private JemmaAuthenticator jemmaAuthenticator;
	private JemmaEnquirer enquirer = null;
	private boolean connected = false;

	public Jemma(URI jemmaURL)
	{
		this(jemmaURL, new JemmaAuthenticator()); 
	}
	public Jemma(URI jemmaURI, JemmaAuthenticator jemmaAuthenticator)
	{
		this.jemmaURI = jemmaURI; 
		this.jemmaAuthenticator = jemmaAuthenticator;
	}

	public void connect() throws InvalidCredentialsException, IOException 
	{
		if(enquirer == null || !connected)
		{
			if(enquirer != null)
				enquirer.release();
			enquirer = new JemmaEnquirer(jemmaURI);
			enquirer.connect(jemmaAuthenticator);
		}
		connected = true;
	}
	private void disconnect()
	{
		if(enquirer != null)
			enquirer.release();
		enquirer = null;
		connected = false;
	}

	public List<Appliance> getDevices() throws IOException, InvalidCredentialsException, MethodNotSupportedException
	{
		List<Appliance> appliancesList;
		if(!isConnected())		
			connect();

		appliancesList = enquirer.getDevices();

		return appliancesList;		
	}

	public URI getURI()
	{
		return jemmaURI;
	}

	private boolean isConnected()
	{
		return connected;
	}

	@Override
	public int hashCode() 
	{
		return super.hashCode();
	}

	public static class JemmaAuthenticator
	{
		public static final String DEFAULT_USERNAME="admin";
		public static final String DEFAULT_PASSWORD="Admin";
		String username;
		String password;

		public JemmaAuthenticator()
		{
			this(DEFAULT_USERNAME, DEFAULT_PASSWORD);
		}

		public JemmaAuthenticator(String username, String password)
		{
			this.username = username;
			this.password = password;
		}
	}
	public static class JemmaResponse
	{
		private JSONObject jsonResponse;

		public JemmaResponse(String responseAsString)
		{
			try
			{
				jsonResponse = new JSONObject(responseAsString);
			}
			catch(JSONException e)
			{
				jsonResponse = new JSONObject();
			}
		}
		public JemmaResponse(JSONObject response)
		{
			this.jsonResponse = response;
		}

		public JSONObject getResponseAsJSONObject()
		{
			return this.jsonResponse;
		}
	}

	enum JemmaMethod
	{
		SYSTEM_LIST_METHODS("system.listMethods"),		
		OSGI_FIND("OSGi.find"),
		OSGI_BIND("OSGi.bind"),
		GET_APPLIANCE_CONFIGURATION_DEMO("getApplianceConfigurationDemo"),		
		GET_APPLIANCES_CONFIGURATIONS_DEMO("getAppliancesConfigurationsDemo"),
		//{id:88,method:"159.getDeviceClusters",params:["ah.app.3521399293210526201-8"]}
		GET_DEVICE_CLUSTERS("getDeviceClusters"),
		//{id:158,method:"159.setDeviceState",params:["ah.app.3521399293210526201-8", 0]}
		SET_DEVICE_STATE("setDeviceState");

		String methodName;
		static HashMap<String, JemmaMethod> cam = null;

		static
		{
			cam = new HashMap<String, JemmaMethod>();
			for(JemmaMethod method : JemmaMethod.values())
				cam.put(method.methodName, method);
		}
		
		JemmaMethod(String methodName)
		{
			this.methodName = methodName;

		}
		static JemmaMethod parse(String content)
		{			
			return cam.get(content);
		}
	}
}
