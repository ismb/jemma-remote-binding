package it.ismb.pert.jemma.jemmaDAL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.MethodNotSupportedException;
import org.json.JSONException;
import org.json.JSONObject;

import it.ismb.pert.jemma.jemmaDAL.Jemma.JemmaException;
import it.ismb.pert.jemma.jemmaDAL.Jemma.JemmaMethod;
import it.ismb.pert.jemma.jemmaDAL.Jemma.JemmaResponse;

public class Appliance
{
	private static final String APPLIANCE_PID_KEY = "appliance.pid";

	private String appliancePid;
	private Set<ApplianceAction> actionsList = null;
	private JemmaEnquirer enquirer;

	private Appliance(JSONObject jsonRepr, JemmaEnquirer enquirer) throws JSONException
	{
		this.appliancePid = Appliance.getAppliancePid(jsonRepr);
		this.enquirer = enquirer;
	}

	public String getAppliancePid()
	{
		return this.appliancePid;
	}

	public Set<ApplianceAction> getActions()
	{
		Set<ApplianceAction> localActions = actionsList;
		JemmaResponse response;
		if (localActions == null)
		{
			List<Object> params = new ArrayList<Object>();
			params.clear();
			params.add(appliancePid);
			// {"result":{"javaClass":"java.util.Hashtable","map":{"cluster1":"org.energy_home.jemma.ah.cluster.zigbee.general.OnOffServer","cluster0":"org.energy_home.jemma.ah.cluster.ah.ConfigServer","pid":"ah.app.3521399293210526201-8","cluster4":"org.energy_home.jemma.ah.cluster.ah.ConfigServer","cluster3":"org.energy_home.jemma.ah.cluster.zigbee.general.BasicServer","cluster2":"org.energy_home.jemma.ah.cluster.zigbee.metering.SimpleMeteringServer"}},"id":686}
			try
			{
				response = enquirer.callJemmaMethod(JemmaMethod.GET_DEVICE_CLUSTERS, params);
				actionsList = localActions = Appliance.getApplianceActions(response.getResponseAsJSONObject()
						.getJSONObject(JemmaEnquirer.RESULT_KEY).getJSONObject("map"));

			}
			catch (MethodNotSupportedException | IOException | JSONException e)
			{
				localActions = new HashSet<ApplianceAction>();
				e.printStackTrace();
			}
		}

		return localActions;
	}

	public static Appliance buildAppliance(JSONObject jsonRepr, JemmaEnquirer enquirer) throws JSONException
	{
		return new Appliance(jsonRepr, enquirer);
	}

	public void performAction(ApplianceAction action, List<Object> params) throws UnsupportedActionException
	{
		if (!canDoThis(action))
			throw new UnsupportedActionException();

		try
		{
			enquirer.callJemmaMethod(JemmaMethod.SET_DEVICE_STATE, params);
		}
		catch (MethodNotSupportedException | IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public boolean canDoThis(ApplianceAction action)
	{
		return getActions().contains(action);
	}

	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer("");
		buffer.append("Device PID: " + appliancePid + "\n");
		buffer.append("Device capabilities:\n");
		for (ApplianceAction action : getActions())
			buffer.append("\t" + action + "\n");

		return buffer.toString();
	}

	public enum ApplianceAction
	{
		OnOffServer("org.energy_home.jemma.ah.cluster.zigbee.general.OnOffServer"), SimpleMeteringServer(
				"org.energy_home.jemma.ah.cluster.zigbee.metering.SimpleMeteringServer");

		private String methodName;
		private static Map<String, ApplianceAction> cam = null;

		static
		{
			cam = new HashMap<String, ApplianceAction>();
			for (ApplianceAction action : ApplianceAction.values())
				cam.put(action.methodName, action);
		}

		ApplianceAction(String methodName)
		{
			this.methodName = methodName;
		}

		public static ApplianceAction parse(String applianceActionName)
		{
			return cam.get(applianceActionName);
		}
	}

	public static class ApplianceException extends JemmaException
	{

	}

	public static class UnsupportedActionException extends ApplianceException
	{

	}

	// {id:88,method:"159.getDeviceClusters",params:["ah.app.3521399293210526201-8"]}

	public static Set<ApplianceAction> getApplianceActions(JSONObject jsonClusters) throws JSONException
	{
		// {"cluster1":"org.energy_home.jemma.ah.cluster.zigbee.general.OnOffServer","cluster0":"org.energy_home.jemma.ah.cluster.ah.ConfigServer","pid":"ah.app.3521399293210526201-8","cluster4":"org.energy_home.jemma.ah.cluster.ah.ConfigServer","cluster3":"org.energy_home.jemma.ah.cluster.zigbee.general.BasicServer","cluster2":"org.energy_home.jemma.ah.cluster.zigbee.metering.SimpleMeteringServer"}
		Set<ApplianceAction> clustersNamesList = new HashSet<ApplianceAction>();
		String[] keys = JSONObject.getNames(jsonClusters);
		ApplianceAction action = null;
		for (String key : keys)
			if (!key.equals("pid") && (action = ApplianceAction.parse(jsonClusters.getString(key))) != null)
				clustersNamesList.add(action);

		return clustersNamesList;
	}

	public static String getAppliancePid(JSONObject jsonRepr) throws JSONException
	{
		// {"result":{"javaClass":"java.util.ArrayList","list":[{"javaClass":"java.util.Hashtable","map":{"ah.app.eps.types":["ah.ep.common","ah.ep.zigbee.SmartPlug"],"ah.location.pid":"0","appliance.pid":"ah.app.3521399293210526201-8","device_value":{"javaClass":"java.util.LinkedList","list":[]},"availability":0,"category":{"iconName":"other.png","javaClass":"org.energy_home.jemma.ah.hac.lib.ext.Category","name":"Other","pid":"1"},"ah.category.pid":"1","ah.icon":"plug.png","ah.app.type":"org.energy_home.jemma.ah.zigbee.smartplug","ah.app.name":"SmartPlug
		// 1"}}]},"id":4}
		return jsonRepr.getString(APPLIANCE_PID_KEY);
	}

}
