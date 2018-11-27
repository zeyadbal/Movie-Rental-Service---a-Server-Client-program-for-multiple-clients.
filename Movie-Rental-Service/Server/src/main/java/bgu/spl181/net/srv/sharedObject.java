package bgu.spl181.net.srv;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class sharedObject<T> {
	
	volatile ReadWriteLock Users_readWriteLock = new ReentrantReadWriteLock(); 
	volatile ReadWriteLock Movies_readWriteLock = new ReentrantReadWriteLock(); 
    ConcurrentLinkedQueue<String> onlineUsers = new ConcurrentLinkedQueue<String>();
    ConcurrentLinkedQueue<Integer> onlineClients = new ConcurrentLinkedQueue<Integer>();
    CurrentConnections<T> CurrentConnections;
    ConcurrentHashMap<String,String>Clients= new ConcurrentHashMap<String,String>();
    ConcurrentLinkedQueue<String>admins= new ConcurrentLinkedQueue<String>();


    public void attachConnections(CurrentConnections<T> CurrentConnections) {
		this.CurrentConnections= CurrentConnections;
	}
    
    public void addClient(String name, String password){
    	Clients.putIfAbsent(name, password);
    }
    
    public void makeOnline_ById(Integer id){
		onlineClients.add(id);
	}
	
	public void makeOnline_ByUsername(String userName){
		onlineUsers.add(userName);
	}
	
	public void makeOffline_ById(Integer id){
		onlineClients.remove(id);
	}
	
	public void makeOffline_ByUsername(String userName){
		onlineUsers.remove(userName);
	}
	
	public boolean isOnline_ById(Integer id){
		if(onlineClients.contains(id)){
			return true;
		}
		else{
			return false;
		}
	}
	
	public boolean isOnline_ByUsername(String userName){
		if(onlineUsers.contains(userName)){
			return true;
		}
		else{
			return false;
		}
	}

	public ConcurrentLinkedQueue<String> getOnlineUsers(){
		return onlineUsers;
	}
	
	public ReadWriteLock getUsersReadWriteLock(){
		return Users_readWriteLock;
	}
	
	public ReadWriteLock getMoviesReadWriteLock(){
		return Movies_readWriteLock;
	}
	
	public void broadcastToOnlines(T msg){
		for(Integer entry : onlineClients){
			CurrentConnections.send(entry, msg);
		}
	}
	public ConcurrentHashMap<String,String> getClients(){
		return Clients;
	}
	public void addAdmin(String adminName){
		this.admins.add(adminName);
	}
	public boolean isAdmin(String name){
		return admins.contains(name);
	}
}