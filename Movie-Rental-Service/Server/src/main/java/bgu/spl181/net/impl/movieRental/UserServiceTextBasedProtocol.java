package bgu.spl181.net.impl.movieRental;

import bgu.spl181.net.api.bidi.Connections;
import bgu.spl181.net.srv.CurrentConnections;
import bgu.spl181.net.srv.sharedObject;
public abstract class UserServiceTextBasedProtocol implements bgu.spl181.net.api.bidi.BidiMessagingProtocol<String> {

	private boolean shouldTerminate = false;
	Connections<String> connections;
	int connectionId;
	protected sharedObject<String> sharedObject;
	String currMsg;
	String clientName="";
	boolean continueHandling;

/*
	 * 			                       IN THIS CLASS
	 * 
	 * in this implementation of the USTBP, we support the design that make this protocol
	 * as independent as possible. it doesn't rely on the extended protocol (in our case, it 
	 * doesn't rely on the movie service protocol) and it doensn't has any connection to the json's.
	 * its implemented as a generically(simple) user text protocol.
	 *  
	 *  
	 *  <<<LOGIN and SIGNOUT>>>
	 *  
	 *  since in the different extended protocols LOGIN and SIGNOUT commands are the same, we do
	 *  implement this methods here. so the extended protocols doesn't have to implement them again
	 *  since its a general commands which is not relies on the specific extend protocol.
	 *  
	 *  <<<REGISTER and REQUEST>>>
	 *  
	 *  this methods are abstract in our implementation. since the higher protocols (movie rental
	 *  for example) should implement. this is because the USTBP doesn't really know what REQUEST 
	 *  is, and doesn't know what is the [data block] (country=" " in our example) in the register 
	 *  command is. since there is many protocols which may extend the USTBP that don't have 
	 *  the "country" field in their registration. they may have "age" or anything else..
	 *  the same is about REQUEST, there are an infinity number of requests which a protocol can
	 *  support. so the USTBP cannot handle all of these.
	 *  
	 */
	
	public UserServiceTextBasedProtocol(sharedObject<String> sharedData) {
		this.sharedObject=sharedData;
	}
	
	@Override
	public void start(int connectionId, Connections<String> connections) {
		sharedObject.attachConnections((CurrentConnections<String>)connections);
		this.connections=connections;
		this.connectionId=connectionId;
	}

	public void attachSharedObject(sharedObject<String> sharedObject){
		this.sharedObject=sharedObject;
	}

	@Override
	public void process(String msg) {
		
		continueHandling=true;
		this.currMsg=msg;
		

		String delimiter = " ";
		String[] substrings = currMsg.split(delimiter);
		String command= substrings[0];

		switch(command){

		case "LOGIN" :
			handleLOGIN(substrings);
			break;

		case "SIGNOUT" :
			handleSIGNOUT(substrings);
			break;
			
		default :
			return;	
			
		}
	}


	private void handleLOGIN(String[]substrings){

		if(clientName!="" && clientName!=substrings[1]){
			String msg="ERROR login failed";
			connections.send(connectionId, msg);
			return;
		}
		
		if(clientName==""){
			clientName=substrings[1];
		}
		
		//Reason to failure: Client performing LOGIN command already performed successful LOGIN

		if(sharedObject.isOnline_ById(connectionId)){
			String msg="ERROR login failed";
			connections.send(connectionId, msg);
			clientName="";
			return;
		}

		//Reason to failure: User name already logged in

		if(sharedObject.isOnline_ByUsername(clientName)){
			String msg="ERROR login failed";
			connections.send(connectionId, msg);
			clientName="";
			return;
		}

		/*Reason to failure: User name and password combination doesn't fit any user
		 *in the system
		 */

		if(substrings.length<3){// missed user name / password
			String msg="ERROR login failed";
			connections.send(connectionId, msg);
			clientName="";
			return;
		}


		boolean exist=false;
		for(String entry : sharedObject.getClients().keySet()){
			if(entry.equalsIgnoreCase(substrings[1])){
				exist=true;
				if(!sharedObject.getClients().get(entry).equals(substrings[2])){
					exist=false;
					break;
				}
			}
		}

		if(!exist){//wrong user name/password
			String msg="ERROR login failed";
			connections.send(connectionId, msg);
			clientName="";
			return;
		}

		sharedObject.makeOnline_ById(connectionId);
		sharedObject.makeOnline_ByUsername(clientName);
		String msg="ACK login succeeded";
		connections.send(connectionId, msg);
	}

	private void handleSIGNOUT(String[]substrings){

		//Reason to failure: Client not logged in

		if(!sharedObject.isOnline_ById(connectionId)){
			String msg="ERROR signout failed";
			connections.send(connectionId, msg);
			return;
		}

		if(substrings.length>1){// Illegal command syntax
			String msg="ERROR signout failed";
			connections.send(connectionId, msg);
			return;
		}

		
		
		sharedObject.makeOffline_ById(connectionId);
		sharedObject.makeOffline_ByUsername(clientName);

		String msg="ACK signout succeeded";
		
		shouldTerminate=true;
		connections.send(connectionId, msg);
//		connections.disconnect(connectionId);
		
		

	}

	public abstract void handleREGISTER(String[]substrings);
	
	public abstract void handleREQUEST(String[]substrings);

	@Override
	public boolean shouldTerminate() {
		return shouldTerminate;
	}
}

