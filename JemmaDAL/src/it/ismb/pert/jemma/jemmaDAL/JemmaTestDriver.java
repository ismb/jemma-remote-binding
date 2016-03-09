package it.ismb.pert.jemma.jemmaDAL;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
			
			for(Appliance device : manager.getAllDevices())			
				System.out.println(device);			

		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public JemmaAuthenticator submitCredential(URI jemmaURI) {
		// Default authenticator
		return new JemmaAuthenticator();
	}

}
