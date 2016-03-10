package it.ismb.pert.jemma.jemmaDAL;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.MethodNotSupportedException;
import org.apache.http.auth.InvalidCredentialsException;

import it.ismb.pert.jemma.jemmaDAL.Jemma.JemmaAuthenticator;

public class JemmaRPCManager
{
	private Set<Jemma> jemmaInstances = new HashSet<Jemma>();
	JemmaCredentialsProvider credentialsProvider = null;

	public JemmaRPCManager(List<URI> jemmasEndpoints, JemmaCredentialsProvider provider)
	{
		Jemma jemmaInstance;
		JemmaAuthenticator authenticator = null;
		credentialsProvider = provider;

		for (URI jemmaURI : jemmasEndpoints)
		{
			if (credentialsProvider != null)
				authenticator = credentialsProvider.submitCredential(jemmaURI);
			jemmaInstance = new Jemma(jemmaURI, (authenticator == null) ? new JemmaAuthenticator() : authenticator);

			try
			{
				jemmaInstance.connect();
				jemmaInstances.add(jemmaInstance);

			}
			catch (InvalidCredentialsException | IOException | MethodNotSupportedException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get devices from each JEMMA instance
	 *
	 * @return list of all devices configured on each JEMMA instance
	 */
	public List<Appliance> getAllAppliances()
	{
		List<Appliance> allAppliancesList = new ArrayList<Appliance>();

		for (Jemma jemmaInstance : jemmaInstances)
			allAppliancesList.addAll(getAppliances(jemmaInstance));

		return allAppliancesList;
	}

	/**
	 * Get devices from a specific JEMMA instance
	 *
	 * @return list of all devices configured on each JEMMA instance
	 */
	public List<Appliance> getAppliances(Jemma jemmaInstance)
	{
		return jemmaInstance.getAppliances();
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
