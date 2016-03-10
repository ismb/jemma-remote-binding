package it.ismb.pert.jemma.jemmaDAL;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.cache.HttpCacheContext;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import it.ismb.pert.jemma.jemmaDAL.Jemma.JemmaAuthenticator;
import it.ismb.pert.jemma.jemmaDAL.Jemma.JemmaMethod;
import it.ismb.pert.jemma.jemmaDAL.Jemma.JemmaResponse;

public class JemmaEnquirer
{
	private static final String LOGIN = "http://localhost:8080/demo/conf/login.html?username=admin&password=Admin&submit=Entra";
	private static final String QUERY = "{id:177,method:\"163.getAppliancesConfigurationsDemo\",params:[]}";
	private static final String LISTMETHODS = "{id:1,method:\"system.listMethods\",params:[]}";
	private static final String OSGIFIND = "{id:2,method:\"OSGi.find\",params:[\"org.energy_home.jemma.ah.greenathome.GreenAtHomeApplianceService\"]}";
	private static final String OSGIBIND = "{id:3,method:\"OSGi.bind\",params:[{\"javaClass\": \"java.util.HashMap\", \"map\": {\"service.id\": 164, \"interface.name\": \"org.energy_home.jemma.ah.greenathome.GreenAtHomeApplianceService\"}}]}";
	private static final String USERNAME_KEY = "username";
	private static final String PASSWORD_KEY = "password";
	private static final String ID_KEY = "id";
	private static final String SERVICE_ID_KEY = "service.id";
	private static final String METHOD_KEY = "method";
	private static final String PARAMS_KEY = "params";
	private static final String INTERFACE_NAME_KEY = "interface.name";
	public static final String RESULT_KEY = "result";

	private int id = 1;
	private int serviceId;
	private String interfaceName;
	private URI jemmaURI;
	private URI jsonRPCURI = null;
	private JemmaAuthenticator jemmaAuthenticator;
	private HttpCacheContext jemmaHttpContext;

	private Set<JemmaMethod> defaultMethods = new HashSet<JemmaMethod>();

	public JemmaEnquirer(URI jemmaURI)
	{
		this.jemmaURI = jemmaURI;
		defaultMethods.add(JemmaMethod.OSGI_FIND);
		defaultMethods.add(JemmaMethod.OSGI_BIND);
	}

	public void connect(JemmaAuthenticator jemmaAuthenticator) throws InvalidCredentialsException, IOException
	{
		this.jemmaHttpContext = new HttpCacheContext();
		this.jemmaAuthenticator = jemmaAuthenticator;

		List<Object> emptyList = new ArrayList<Object>();
		login();
		try
		{
			callJemmaMethod(JemmaMethod.OSGI_FIND, emptyList);
			callJemmaMethod(JemmaMethod.OSGI_BIND, emptyList);
		}
		catch (MethodNotSupportedException e)
		{
			// These methods must always exist
		}
	}

	public List<Appliance> getAppliances() throws MethodNotSupportedException, IOException
	{

		List<Appliance> appliancesList = new ArrayList<Appliance>();
		List<Object> params = new ArrayList<Object>();
		JemmaResponse response = callJemmaMethod(JemmaMethod.GET_APPLIANCES_CONFIGURATIONS_DEMO, params);
		JSONObject jsonApplianceMap = null;
		// {"result":{"javaClass":"java.util.ArrayList","list":[{"javaClass":"java.util.Hashtable","map":{"ah.app.eps.types":["ah.ep.common","ah.ep.zigbee.SmartPlug"],"ah.location.pid":"0","appliance.pid":"ah.app.3521399293210526201-8","device_value":{"javaClass":"java.util.LinkedList","list":[]},"availability":0,"category":{"iconName":"other.png","javaClass":"org.energy_home.jemma.ah.hac.lib.ext.Category","name":"Other","pid":"1"},"ah.category.pid":"1","ah.icon":"plug.png","ah.app.type":"org.energy_home.jemma.ah.zigbee.smartplug","ah.app.name":"SmartPlug
		// 1"}}]},"id":4}

		try
		{
			JSONArray jsonAppliancesList = response.getResponseAsJSONObject().getJSONObject(RESULT_KEY)
					.getJSONArray("list");
			for (int i = 0; i < jsonAppliancesList.length(); ++i)
				try
				{
					jsonApplianceMap = jsonAppliancesList.getJSONObject(i).getJSONObject("map");
					Appliance appliance = Appliance.buildAppliance(jsonApplianceMap, this);
					appliancesList.add(appliance);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return appliancesList;
	}

	public JemmaResponse callJemmaMethod(JemmaMethod jemmaMethod, List<Object> params)
			throws MethodNotSupportedException, IOException
	{
		URI jsonRPCURI = getJSONRPCURI();
		HttpEntity entity = forgeHttpEntity(jemmaMethod, params);

		HttpResponse response = performHttpPost(jsonRPCURI, entity);
		if (!isValidResponse(response))
			throw new HttpResponseException(response.getStatusLine().getStatusCode(), "Malformed response");

		String responseAsString = getResponseAsString(response.getEntity());
		System.out.println("RECEIVED RESPONSE - " + responseAsString);

		JemmaResponse jemmaResponse = new JemmaResponse(responseAsString);
		processJemmaResponse(jemmaMethod, jemmaResponse);

		return jemmaResponse;
	}

	private void processJemmaResponse(JemmaMethod calleeMethod, JemmaResponse response)
	{
		try
		{
			switch (calleeMethod)
			{
			case OSGI_FIND:
			{
				// {"result":{"javaClass":"java.util.ArrayList","list":[{"javaClass":"java.util.HashMap","map":{"service.id":164,"interface.name":"org.energy_home.jemma.ah.greenathome.GreenAtHomeApplianceService"}}]},"id":2}'

				JSONObject map = response.getResponseAsJSONObject().getJSONObject("result").getJSONArray("list")
						.getJSONObject(0).getJSONObject("map");
				serviceId = map.getInt(SERVICE_ID_KEY);
				interfaceName = map.getString(INTERFACE_NAME_KEY);

				break;
			}
			case OSGI_BIND:
				break;
			default:
				break;
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private HttpEntity forgeHttpEntity(JemmaMethod jemmaMethod, List<Object> params)
	{
		HttpEntity httpEntity = null;
		JSONObject jsonEntity = new JSONObject();
		try
		{
			jsonEntity.put(ID_KEY, getTransactionId());
			String serviceIdString = "";

			JSONArray jsonParams = new JSONArray();

			switch (jemmaMethod)
			{
			case SYSTEM_LIST_METHODS:
				break;
			case OSGI_FIND:
				jsonParams.put("org.energy_home.jemma.ah.greenathome.GreenAtHomeApplianceService");
				break;
			case OSGI_BIND:
			{
				JSONObject jsonMap = new JSONObject();
				jsonMap.put(SERVICE_ID_KEY, serviceId);
				jsonMap.put(INTERFACE_NAME_KEY, interfaceName);

				JSONObject bindDetails = new JSONObject();
				bindDetails.put("javaClass", "java.util.HashMap");
				bindDetails.put("map", jsonMap);
				jsonParams.put(bindDetails);
				break;
			}
			case GET_APPLIANCE_CONFIGURATION_DEMO:
			case GET_APPLIANCES_CONFIGURATIONS_DEMO:
				serviceIdString = String.valueOf(serviceId);
				break;
			case GET_DEVICE_CLUSTERS:
				serviceIdString = String.valueOf(serviceId);
				break;
			case SET_DEVICE_STATE:
				serviceIdString = String.valueOf(serviceId);
				break;
			}

			jsonEntity.put(METHOD_KEY,
					serviceIdString + (serviceIdString.length() > 0 ? "." : "") + jemmaMethod.methodName);
			for (Object o : params)
				jsonParams.put(o);
			jsonEntity.put(PARAMS_KEY, jsonParams);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		httpEntity = new StringEntity(jsonEntity.toString(), Consts.UTF_8);

		return httpEntity;
	}

	private String getResponseAsString(HttpEntity responseEntity)
	{
		StringBuffer buffer = new StringBuffer("");
		InputStreamReader isr = null;
		char[] chars = new char[1024];
		int readBytes;
		try
		{
			isr = new InputStreamReader(responseEntity.getContent());
			while ((readBytes = isr.read(chars)) > 0)
				buffer.append(new String(chars, 0, readBytes));

		}
		catch (UnsupportedOperationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (isr != null)
					isr.close();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return buffer.toString();
	}

	private int getTransactionId()
	{
		return id++;
	}

	private HttpResponse performHttpPost(URI uri, HttpEntity postEnitty) throws IOException
	{
		HttpResponse response = null;

		HttpPost post = new HttpPost(uri);
		post.setEntity(postEnitty);
		CloseableHttpClient client = HttpClients.createDefault();
		response = client.execute(post, jemmaHttpContext);
		return response;
	}

	private void login() throws InvalidCredentialsException, IOException
	{
		URI loginURI;

		try
		{

			loginURI = new URI("http://" + jemmaURI.getHost() + ":" + jemmaURI.getPort() + "/demo/conf/login.html?"
					+ USERNAME_KEY + "=" + jemmaAuthenticator.username + "&" + PASSWORD_KEY + "="
					+ jemmaAuthenticator.password + "&submit=Entra");

			HttpGet getLogin = new HttpGet(loginURI);
			CloseableHttpClient clientHttp = HttpClients.createDefault();

			HttpResponse responseLogin = clientHttp.execute(getLogin);
			if (!isValidResponse(responseLogin))
				throw new InvalidCredentialsException();

		}
		catch (URISyntaxException e)
		{
			e.printStackTrace();
		}

	}

	private URI getJSONRPCURI()
	{
		if (jsonRPCURI == null)
		{
			URIBuilder uriBuilder = new URIBuilder();
			uriBuilder.setScheme("http");
			uriBuilder.setHost(jemmaURI.getHost());
			uriBuilder.setPort(jemmaURI.getPort());
			uriBuilder.setPath("/demo/JSON-RPC");

			try
			{
				jsonRPCURI = uriBuilder.build();
			}
			catch (URISyntaxException e)
			{
				e.printStackTrace(); // cannot fail
			}
		}

		return jsonRPCURI;
	}

	private static boolean isValidResponse(HttpResponse response)
	{
		return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
	}

	public void release()
	{
		this.id = 1;
		this.jemmaHttpContext = null;

	}
}
