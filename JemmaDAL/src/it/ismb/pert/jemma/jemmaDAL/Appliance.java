package it.ismb.pert.jemma.jemmaDAL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

public class Appliance 
{
	private static final String APPLIANCE_PID_KEY="appliance.pid";

	private String appliancePid;	
	private JSONObject jsonRepr;	
	private Set<ApplianceAction> actionsList;
	private JemmaEnquirer enquirer;

	public Appliance(JSONObject jsonRepr, JSONObject jsonClusters, JemmaEnquirer enquirer) throws JSONException
	{
		this.jsonRepr = jsonRepr; 
		this.actionsList = new HashSet<ApplianceAction>();
		this.enquirer = enquirer;
		buildAppliance(jsonRepr, jsonClusters);
	}

	public String getAppliancePid()
	{
		return this.appliancePid;
	}

	public Set<ApplianceAction> getActions()
	{
		return actionsList;
	}	

	private void buildAppliance(JSONObject jsonRepr, JSONObject jsonClusters) throws JSONException
	{
		appliancePid = getAppliancePid(jsonRepr);
		actionsList = getApplianceActions(jsonClusters);

	}

	public void performAction(ApplianceAction action, List<Object> params)
	{
		if(canDoThis(action))
		{
			
		}		
	}
	
	public boolean canDoThis(ApplianceAction action)
	{
		return actionsList.contains(action);
	}

	public String toString()
	{
		StringBuffer buffer = new StringBuffer("");
		buffer.append("Device PID: "+appliancePid+"\n");
		buffer.append("Device capabilities:\n");
		for(ApplianceAction action: actionsList)
			buffer.append("\t"+action+"\n");
				
		return buffer.toString();
	}	
	
	enum ApplianceAction
	{
		OnOffServer("org.energy_home.jemma.ah.cluster.zigbee.general.OnOffServer"),
		SimpleMeteringServer("org.energy_home.jemma.ah.cluster.zigbee.metering.SimpleMeteringServer");
		
		private String methodName;
		private static Map<String,ApplianceAction> cam = null;
		
		static
		{
			cam = new HashMap<String, ApplianceAction>();
			for(ApplianceAction action : ApplianceAction.values())
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

	//{id:88,method:"159.getDeviceClusters",params:["ah.app.3521399293210526201-8"]}
	
	public static Set<ApplianceAction> getApplianceActions(JSONObject jsonClusters) throws JSONException
	{
		//{"cluster1":"org.energy_home.jemma.ah.cluster.zigbee.general.OnOffServer","cluster0":"org.energy_home.jemma.ah.cluster.ah.ConfigServer","pid":"ah.app.3521399293210526201-8","cluster4":"org.energy_home.jemma.ah.cluster.ah.ConfigServer","cluster3":"org.energy_home.jemma.ah.cluster.zigbee.general.BasicServer","cluster2":"org.energy_home.jemma.ah.cluster.zigbee.metering.SimpleMeteringServer"}
		Set<ApplianceAction> clustersNamesList = new HashSet<ApplianceAction>();
		String[] keys = JSONObject.getNames(jsonClusters);
		ApplianceAction action = null;
		for(String key : keys)		
			if(!key.equals("pid") && (action = ApplianceAction.parse(jsonClusters.getString(key))) != null)				
				clustersNamesList.add(action);
		
		return clustersNamesList;
	}

	public static String getAppliancePid(JSONObject jsonRepr) throws JSONException
	{
		//{"result":{"javaClass":"java.util.ArrayList","list":[{"javaClass":"java.util.Hashtable","map":{"ah.app.eps.types":["ah.ep.common","ah.ep.zigbee.SmartPlug"],"ah.location.pid":"0","appliance.pid":"ah.app.3521399293210526201-8","device_value":{"javaClass":"java.util.LinkedList","list":[]},"availability":0,"category":{"iconName":"other.png","javaClass":"org.energy_home.jemma.ah.hac.lib.ext.Category","name":"Other","pid":"1"},"ah.category.pid":"1","ah.icon":"plug.png","ah.app.type":"org.energy_home.jemma.ah.zigbee.smartplug","ah.app.name":"SmartPlug 1"}}]},"id":4}		
		return jsonRepr.getString(APPLIANCE_PID_KEY);
	}

}

	