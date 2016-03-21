package it.ismb.pert.jemma.jemmaDAL;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.MethodNotSupportedException;
import org.apache.http.auth.InvalidCredentialsException;

import it.ismb.pert.jemma.jemmaDAL.Jemma.Authenticator;

public class JemmaRPCManager {
    private static JemmaRPCManager jemmaManagerInstance = null;
    private Set<Jemma> jemmaInstances = new HashSet<Jemma>();
    JemmaCredentialsProvider credentialsProvider = null;

    private JemmaRPCManager() {
    }

    public static synchronized JemmaRPCManager getInstance() {
        if (jemmaManagerInstance == null) {
            jemmaManagerInstance = new JemmaRPCManager();
        }

        return jemmaManagerInstance;
    }

    public Set<Jemma> addAll(Set<URI> jemmaURIsSet, JemmaCredentialsProvider provider) {
        Set<Jemma> jemmasSet = new HashSet<Jemma>();
        for (URI jemmaURI : jemmaURIsSet) {
            jemmasSet.add(getJemma(jemmaURI, provider));
        }

        return jemmasSet;
    }

    public Jemma getJemma(URI jemmaURI, JemmaCredentialsProvider provider) {
        Jemma jemmaInstance = null;
        Authenticator authenticator = null;

        credentialsProvider = provider;

        if (credentialsProvider != null) {
            authenticator = credentialsProvider.submitCredential(jemmaURI);
        }
        jemmaInstance = new Jemma(jemmaURI, (authenticator == null) ? new Authenticator() : authenticator);

        try {
            jemmaInstance.connect();
            jemmaInstances.add(jemmaInstance);
        } catch (InvalidCredentialsException | IOException | MethodNotSupportedException e) {
            return null;
        }

        return jemmaInstance;
    }

    public void removeJemma(Jemma jemmaInstance) {
        jemmaInstance.disconnect();
        this.jemmaInstances.remove(jemmaInstance);
    }

    /**
     * Get devices from each JEMMA instance
     *
     * @return set of all devices configured on each JEMMA instance
     */
    public Set<Appliance> getAllAppliances() {
        Set<Appliance> allAppliancesList = new HashSet<Appliance>();

        for (Jemma jemmaInstance : jemmaInstances) {
            allAppliancesList.addAll(getAppliances(jemmaInstance));
        }

        return allAppliancesList;
    }

    /**
     * Get JEMMA instances
     * 
     * @return set of JEMMA instances currently registered
     */

    public Set<Jemma> getJemmaInstances() {
        return this.jemmaInstances;
    }

    /**
     * Get devices from a specific JEMMA instance
     *
     * @return list of all devices configured on each JEMMA instance
     */
    public Set<Appliance> getAppliances(Jemma jemmaInstance) {
        return jemmaInstance.getAppliances();
    }

    public interface JemmaCredentialsProvider {
        /**
         * Callback to provide JEMMA authentication credentials
         *
         * @param jemmaURI
         */
        public Authenticator submitCredential(URI jemmaURI);
    }
}
