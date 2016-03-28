package it.ismb.pert.jemma.jemmaDAL;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.MethodNotSupportedException;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.types.State;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.ismb.pert.jemma.jemmaDAL.Jemma.JemmaException;
import it.ismb.pert.jemma.jemmaDAL.Jemma.JemmaMethod;
import it.ismb.pert.jemma.jemmaDAL.Jemma.JemmaResponse;

public class Appliance implements Comparable<Appliance> {
    private static final String APPLIANCE_PID_KEY = "appliance.pid";
    private static final String APPLIANCE_TYPES_KEY = "ah.app.eps.types";

    private Logger logger = LoggerFactory.getLogger(Appliance.class);
    private String applianceId;
    private ApplianceType type;
    private String modelId;
    Set<ApplianceAction> actionsList = null;
    private JemmaEnquirer enquirer;
    private volatile Set<OnApplianceStatusUpdatedListener> onApplianceStatusUpdatedListeners = new HashSet<>();

    private Appliance(JSONObject jsonRepr, JemmaEnquirer enquirer) throws JSONException {
        this.applianceId = Appliance.getIdFromJson(jsonRepr);
        this.type = Appliance.getModelIdFromJson(jsonRepr);
        this.enquirer = enquirer;
    }

    public String getId() {
        return this.applianceId;
    }

    public String getName() {
        // TODO
        return this.applianceId;
    }

    public String getModelId() {
        // TODO
        return this.modelId;
    }

    public ApplianceType getType() {
        return this.type;
    }

    public Set<ApplianceAction> getApplianceActions() {
        Set<ApplianceAction> localActions = actionsList;
        JemmaResponse response;
        if (localActions == null) {
            List<Object> params = new ArrayList<Object>();
            params.clear();
            params.add(applianceId);
            // {"result":{"javaClass":"java.util.Hashtable","map":{"cluster1":"org.energy_home.jemma.ah.cluster.zigbee.general.OnOffServer","cluster0":"org.energy_home.jemma.ah.cluster.ah.ConfigServer","pid":"ah.app.3521399293210526201-8","cluster4":"org.energy_home.jemma.ah.cluster.ah.ConfigServer","cluster3":"org.energy_home.jemma.ah.cluster.zigbee.general.BasicServer","cluster2":"org.energy_home.jemma.ah.cluster.zigbee.metering.SimpleMeteringServer"}},"id":686}
            try {
                response = enquirer.callJemmaMethod(JemmaMethod.GET_DEVICE_CLUSTERS, params);
                localActions = Appliance.getApplianceActionsFromJson(response.getResponseAsJSONObject()
                        .getJSONObject(JemmaEnquirer.RESULT_KEY).getJSONObject("map"));
                actionsList = localActions;

            } catch (MethodNotSupportedException | IOException | JSONException e) {
                localActions = new HashSet<ApplianceAction>();
                e.printStackTrace();
            }
        }

        return localActions;
    }

    public static Appliance buildAppliance(JSONObject jsonRepr, JemmaEnquirer enquirer) throws JSONException {
        return new Appliance(jsonRepr, enquirer);
    }

    public void performAction(ApplianceAction action, List<Object> params) throws UnsupportedActionException {
        if (!canDoThis(action)) {
            throw new UnsupportedActionException();
        }
        try {
            enquirer.callJemmaMethod(JemmaMethod.SET_DEVICE_STATE, params);
        } catch (MethodNotSupportedException | IOException e) {
            e.printStackTrace();
        }
    }

    public boolean canDoThis(ApplianceAction action) {
        return getApplianceActions().contains(action);
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer("");
        buffer.append("Device PID: " + applianceId + "\n");
        buffer.append("Device capabilities:\n");
        for (ApplianceAction action : getApplianceActions()) {
            buffer.append("\t" + action + "\n");
        }

        return buffer.toString();
    }

    public static class ApplianceException extends JemmaException {

    }

    public static class UnsupportedActionException extends ApplianceException {

    }

    // {id:88,method:"159.getDeviceClusters",params:["ah.app.3521399293210526201-8"]}

    public static Set<ApplianceAction> getApplianceActionsFromJson(JSONObject jsonClusters) throws JSONException {
        // {"cluster1":"org.energy_home.jemma.ah.cluster.zigbee.general.OnOffServer","cluster0":"org.energy_home.jemma.ah.cluster.ah.ConfigServer","pid":"ah.app.3521399293210526201-8","cluster4":"org.energy_home.jemma.ah.cluster.ah.ConfigServer","cluster3":"org.energy_home.jemma.ah.cluster.zigbee.general.BasicServer","cluster2":"org.energy_home.jemma.ah.cluster.zigbee.metering.SimpleMeteringServer"}
        Set<ApplianceAction> clustersNamesList = new HashSet<ApplianceAction>();
        String[] keys = JSONObject.getNames(jsonClusters);
        ApplianceAction action = null;
        for (String key : keys) {
            if (!key.equals("pid") && (action = ApplianceAction.parse(jsonClusters.getString(key))) != null) {
                clustersNamesList.add(action);
            }
        }

        return clustersNamesList;
    }

    public static String getIdFromJson(JSONObject jsonRepr) throws JSONException {
        // {"ah.app.eps.types":["ah.ep.common","ah.ep.zigbee.SmartPlug"],"ah.location.pid":"0","appliance.pid":"ah.app.3521399293210526201-8","device_value":{"javaClass":"java.util.LinkedList","list":[]},"availability":0,"category":{"iconName":"other.png","javaClass":"org.energy_home.jemma.ah.hac.lib.ext.Category","name":"Other","pid":"1"},"ah.category.pid":"1","ah.icon":"plug.png","ah.app.type":"org.energy_home.jemma.ah.zigbee.smartplug","ah.app.name":"SmartPlug
        // 1"}
        return jsonRepr.getString(APPLIANCE_PID_KEY);
    }

    public static ApplianceType getModelIdFromJson(JSONObject jsonRepr) throws JSONException {
        // TODO provide a reliable way to infer appliance type
        String modelId;
        ApplianceType type = ApplianceType.UNKNOWN_APPLIANCE_TYPE;
        ApplianceType workType = null;

        JSONArray types = jsonRepr.getJSONArray(APPLIANCE_TYPES_KEY);

        for (int i = 0; i < types.length(); ++i) {
            if ((workType = ApplianceType.parse(types.getString(i))) != null
                    && workType != ApplianceType.UNKNOWN_APPLIANCE_TYPE) {
                type = workType;
                break;
            }
        }

        return type;
    }

    public synchronized void sync() {
        JemmaResponse response;
        logger.info("Synchronizing appliance {}", applianceId);
        try {
            response = enquirer.callJemmaMethod(JemmaMethod.GET_APPLIANCES_CONFIGURATIONS_DEMO, new ArrayList<>());
            JSONArray jarray = response.getResponseAsJSONObject().getJSONObject(JemmaEnquirer.RESULT_KEY)
                    .getJSONArray(JemmaEnquirer.LIST_KEY);

            JSONObject jAppliance = null;
            JSONObject localJAppliance = null;
            for (int i = 0; i < jarray.length(); ++i) {
                if ((localJAppliance = jarray.getJSONObject(i).getJSONObject(JemmaEnquirer.MAP_KEY))
                        .getString(APPLIANCE_PID_KEY).equals(applianceId)) {
                    jAppliance = localJAppliance;
                    break;
                }
            }

            if (jAppliance != null) {
                for (ApplianceAction action : getApplianceActions()) {
                    switch (action) {
                        case OnOffServer:
                            broadcastApplianceStatusUpdated(action,
                                    getOnOffStateFromJson(jAppliance) ? OnOffType.ON : OnOffType.OFF);
                            break;
                        case SimpleMeteringServer:
                            // TODO
                            break;
                    }
                }

            }
        } catch (MethodNotSupportedException | IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void broadcastApplianceStatusUpdated(ApplianceAction action, State state) {
        try {
            Method method = null;
            switch (action) {
                case OnOffServer:
                    method = OnApplianceStatusUpdatedListener.class
                            .getMethod(OnApplianceStatusUpdatedListener.ON_OFF_STATE_UPDATED, State.class);
                    break;
                case SimpleMeteringServer:
                    // TODO
                    break;
            }

            if (method != null) {
                synchronized (onApplianceStatusUpdatedListeners) {
                    for (OnApplianceStatusUpdatedListener listener : onApplianceStatusUpdatedListeners) {
                        logger.info("Broadcasting appliance status");
                        method.invoke(listener, state);
                    }
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {

        }
    }

    public static boolean getOnOffStateFromJson(JSONObject obj) throws JSONException {
        JSONArray jValues = obj.getJSONObject(JemmaEnquirer.DEVICE_VALUE_KEY).getJSONArray(JemmaEnquirer.LIST_KEY);
        JSONObject jValue = null;
        JSONObject tmpJValue = null;
        for (int i = 0; i < jValues.length(); ++i) {
            if ((tmpJValue = jValues.getJSONObject(i)).getString(JemmaEnquirer.DEVICE_VALUE_NAME_KEY)
                    .equals("OnOffState")) {
                jValue = tmpJValue;
            }
        }
        if (jValue != null) {
            return jValue.getJSONObject(JemmaEnquirer.VALUE_KEY).getBoolean(JemmaEnquirer.VALUE_KEY);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return applianceId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return applianceId.equals(((Appliance) obj).applianceId);
    }

    @Override
    public int compareTo(Appliance o) {
        return applianceId.compareTo(o.applianceId);
    }

    public enum ApplianceAction {
        OnOffServer("org.energy_home.jemma.ah.cluster.zigbee.general.OnOffServer"),
        SimpleMeteringServer("org.energy_home.jemma.ah.cluster.zigbee.metering.SimpleMeteringServer");

        private String methodName;
        private static Map<String, ApplianceAction> cam = null;

        static {
            cam = new HashMap<String, ApplianceAction>();
            for (ApplianceAction action : ApplianceAction.values()) {
                cam.put(action.methodName, action);
            }
        }

        ApplianceAction(String methodName) {
            this.methodName = methodName;
        }

        public static ApplianceAction parse(String applianceActionName) {
            return cam.get(applianceActionName);
        }
    }

    public enum ApplianceType {
        AH_EP_ZIGBEE_SMARTPLUG("ah.ep.zigbee.SmartPlug"),
        UNKNOWN_APPLIANCE_TYPE("unknownType");

        private String applianceTypeString;
        private static Map<String, ApplianceType> cam;

        static {
            cam = new HashMap<>();
            for (ApplianceType type : ApplianceType.values()) {
                cam.put(type.applianceTypeString, type);
            }
        }

        ApplianceType(String applianceType) {
            this.applianceTypeString = applianceType;
        }

        public String asString() {
            return applianceTypeString;
        }

        public static ApplianceType parse(String applianceTypeString) {
            ApplianceType type = cam.get(applianceTypeString);
            if (type != null) {
                return type;
            }
            return ApplianceType.UNKNOWN_APPLIANCE_TYPE;
        }
    }

    public boolean registerOnApplianceStatusUpdatedListener(OnApplianceStatusUpdatedListener listener) {
        if (listener == null) {
            throw new NullPointerException("Null event handler not valid");
        }
        boolean result = false;
        synchronized (onApplianceStatusUpdatedListeners) {
            result = onApplianceStatusUpdatedListeners.add(listener);
        }

        return result;
    }

    public void unregisterOnApplianceStatusUpdatedListener(OnApplianceStatusUpdatedListener listener) {
        synchronized (onApplianceStatusUpdatedListeners) {
            onApplianceStatusUpdatedListeners.remove(listener);
        }
    }

    public interface OnApplianceStatusUpdatedListener {
        public static final String ON_OFF_STATE_UPDATED = "onOffStateUpdated";

        public void onOffStateUpdated(State state);
    }

}
