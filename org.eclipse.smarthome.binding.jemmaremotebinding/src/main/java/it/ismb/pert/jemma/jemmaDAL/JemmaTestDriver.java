package it.ismb.pert.jemma.jemmaDAL;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import it.ismb.pert.jemma.jemmaDAL.Appliance.ApplianceAction;
import it.ismb.pert.jemma.jemmaDAL.Appliance.UnsupportedActionException;
import it.ismb.pert.jemma.jemmaDAL.Jemma.JemmaAuthenticator;
import it.ismb.pert.jemma.jemmaDAL.JemmaRPCManager.JemmaCredentialsProvider;

public class JemmaTestDriver implements JemmaCredentialsProvider 
{

	public static void main(String[] args)
	{
		URI jemmaURI;
		JemmaTestDriver test = new JemmaTestDriver();
		try 
		{
			jemmaURI = new URI("http://localhost:8080/demo/JSON-RPC");

			List<URI> jemmasList = new ArrayList<URI>();
			jemmasList.add(jemmaURI);
			
			JemmaRPCManager manager = new JemmaRPCManager(jemmasList, test);					
			List<Object> actionParams = new ArrayList<Object>();
			for(Appliance device : manager.getAllAppliances())
			{
				System.out.println(device);
				
				for(int status = 0;; status^=1)
				{
					System.out.println("Status: "+status);
					actionParams.clear();
					actionParams.add(device.getAppliancePid());
					actionParams.add(status);					
					device.performAction(Appliance.ApplianceAction.OnOffServer, actionParams);
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (UnsupportedActionException e) 
		{
			e.printStackTrace();
		}
	}

	@Override
	public JemmaAuthenticator submitCredential(URI jemmaURI) {
		// Default authenticator
		return new JemmaAuthenticator();
	}

}
