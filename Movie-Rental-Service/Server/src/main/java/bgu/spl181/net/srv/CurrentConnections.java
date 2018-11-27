package bgu.spl181.net.srv;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author Zeyad-baraa
 *
 * in this implementation, we hold all the connections which the server has accept.
 * for each connection, there is an unique id which in turn used to identify the specific 
 * connection handler for each client.
 * 
 *  NOTICE: this implementation is generic. any protocol should work with this implementation 
 *  since the class type is <T> , and all the main methods (the interface methods and add_connection
 *  method) are not depending on the protocol and the type we work with.
 *  
 *  send method:
 *  
 *  broadcast method:
 *  
 *  disconnect method:
 *    
 * @param <T>
 */
public class CurrentConnections<T> implements bgu.spl181.net.api.bidi.Connections<T>{

	ConcurrentHashMap<Integer, bgu.spl181.net.srv.bidi.ConnectionHandler<T>> activeClientsbidiCH;

	public CurrentConnections() {
		this.activeClientsbidiCH= new ConcurrentHashMap<Integer, bgu.spl181.net.srv.bidi.ConnectionHandler<T>>();
	}

	@Override
	public boolean send(int connectionId, T msg) {

		if(!activeClientsbidiCH.containsKey(new Integer(connectionId))){
			return false;
		}	
		bgu.spl181.net.srv.bidi.ConnectionHandler<T> bidiCH= activeClientsbidiCH.get(new Integer(connectionId));
		bidiCH.send(msg);
		return true;

	}

	@Override
	public void broadcast(T msg) {	
		for(bgu.spl181.net.srv.bidi.ConnectionHandler<T> entry : activeClientsbidiCH.values()){
			entry.send(msg);
		}
	}

	@Override
	public void disconnect(int connectionId) {
		activeClientsbidiCH.remove(connectionId);
		/*
		try {
			activeClientsbidiCH.get(new Integer(connectionId)).close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}
	
	public void addConnection(Integer id, bgu.spl181.net.srv.bidi.ConnectionHandler<T> bidiCH){
		activeClientsbidiCH.putIfAbsent(id, bidiCH);
	}
}
