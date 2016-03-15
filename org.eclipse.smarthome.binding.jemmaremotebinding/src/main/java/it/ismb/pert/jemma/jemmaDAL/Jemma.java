package it.ismb.pert.jemma.jemmaDAL;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.MethodNotSupportedException;
import org.apache.http.auth.InvalidCredentialsException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Jemma implements Comparable<Jemma>{
    private URI jemmaURI;
    private JemmaAuthenticator jemmaAuthenticator;
    private JemmaEnquirer enquirer = null;
    private Set<JemmaMethod> methods = null;
    private Set<Appliance> appliances = null;
    private boolean connected = false;

    public Jemma(URI jemmaURL) {
        this(jemmaURL, new JemmaAuthenticator());
    }

    public Jemma(URI jemmaURI, JemmaAuthenticator jemmaAuthenticator) {
        this.jemmaURI = jemmaURI;
        this.jemmaAuthenticator = jemmaAuthenticator;
    }

    public void connect() throws InvalidCredentialsException, IOException, MethodNotSupportedException {
        if (enquirer == null || !connected) {
            if (enquirer != null) {
                enquirer.release();
            }
            enquirer = new JemmaEnquirer(jemmaURI);
            enquirer.connect(jemmaAuthenticator);
            methods = getMethods();
        }
        connected = true;
    }
    
    public void disconnect()
    {
        if(connected)
        {
            connected = false;
            if(enquirer != null)
                enquirer.release();
            enquirer = null;
        }
    }

    public Set<Appliance> getAppliances() {
        Set<Appliance> localAppliances = appliances;

        if (appliances == null) {
            try {
                localAppliances = enquirer.getAppliances();
                appliances = localAppliances;
            } catch (IOException | MethodNotSupportedException e) {
                localAppliances = new HashSet<Appliance>();
                e.printStackTrace();
            }
        }

        return localAppliances;
    }
    
    public Appliance getAppliance(String applianceId)
    {
        for(Appliance appliance : appliances)
            if(appliance.getId().equals(applianceId))
                return appliance;
        
        return null;
    }

    public Set<JemmaMethod> getMethods() {
        Set<JemmaMethod> localMethods = methods;
        if (localMethods == null) {
            try {
                JemmaResponse response = enquirer.callJemmaMethod(JemmaMethod.SYSTEM_LIST_METHODS,
                        new ArrayList<Object>());
                methods = localMethods = Jemma.parseMethodsList(response);

            } catch (MethodNotSupportedException | IOException | JSONException e) {
                localMethods = new HashSet<JemmaMethod>();
                e.printStackTrace();
            }
        }

        return localMethods;
    }

    private boolean canDoThis(JemmaMethod method) {
        return methods.contains(method);
    }

    public URI getURI() {
        return jemmaURI;
    }

    private boolean isConnected() {
        return connected;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public static class JemmaAuthenticator {
        public static final String DEFAULT_USERNAME = "admin";
        public static final String DEFAULT_PASSWORD = "Admin";
        String username;
        String password;

        public JemmaAuthenticator() {
            this(DEFAULT_USERNAME, DEFAULT_PASSWORD);
        }

        public JemmaAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    public static class JemmaResponse {
        private JSONObject jsonResponse;

        public JemmaResponse(String responseAsString) {
            try {
                jsonResponse = new JSONObject(responseAsString);
            } catch (JSONException e) {
                jsonResponse = new JSONObject();
            }
        }

        public JemmaResponse(JSONObject response) {
            this.jsonResponse = response;
        }

        public JSONObject getResponseAsJSONObject() {
            return this.jsonResponse;
        }
    }

    enum JemmaMethod {
        SYSTEM_LIST_METHODS("system.listMethods"),
        OSGI_FIND("OSGi.find"),
        OSGI_BIND("OSGi.bind"),
        GET_APPLIANCE_CONFIGURATION_DEMO("getApplianceConfigurationDemo"),
        GET_APPLIANCES_CONFIGURATIONS_DEMO("getAppliancesConfigurationsDemo"),
        // {id:88,method:"159.getDeviceClusters",params:["ah.app.3521399293210526201-8"]}
        GET_DEVICE_CLUSTERS("getDeviceClusters"),
        // {id:158,method:"159.setDeviceState",params:["ah.app.3521399293210526201-8",
        // 0]}
        SET_DEVICE_STATE("setDeviceState");

        String methodName;
        static HashMap<String, JemmaMethod> cam = null;

        static {
            cam = new HashMap<String, JemmaMethod>();
            for (JemmaMethod method : JemmaMethod.values()) {
                cam.put(method.methodName, method);
            }
        }

        JemmaMethod(String methodName) {
            this.methodName = methodName;

        }

        static JemmaMethod parse(String content) {
            return cam.get(content);
        }
    }

    public static Jemma buildJemma() {
        return null;
    }

    public static Set<JemmaMethod> parseMethodsList(JemmaResponse response) throws JSONException {
        JSONObject jsonResponse = response.getResponseAsJSONObject();
        JSONArray results = jsonResponse.getJSONArray(JemmaEnquirer.RESULT_KEY);
        String methodName;
        JemmaMethod supportedMethod;
        Set<JemmaMethod> methods = new HashSet<JemmaMethod>();
        for (int i = 0; i < results.length(); ++i) {
            methodName = results.getString(i).split("[.]")[1];

            if ((supportedMethod = JemmaMethod.parse(methodName)) != null) {
                methods.add(supportedMethod);
            }
        }

        return methods;
    }

    public static class JemmaException extends Exception {

    }

    public static class UnsupportedMethodException extends JemmaException {

    }

    @Override
    public int compareTo(Jemma other) 
    {
        return jemmaURI.compareTo(other.jemmaURI);
    }
}
