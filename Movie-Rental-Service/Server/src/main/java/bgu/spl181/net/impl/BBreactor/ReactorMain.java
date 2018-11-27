package bgu.spl181.net.impl.BBreactor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import bgu.spl181.net.impl.echo.LineMessageEncoderDecoder;
import bgu.spl181.net.impl.movieRental.MovieRentalServiceProtocol;
import bgu.spl181.net.impl.movieRental.MovieRentalServiceProtocol.User;
import bgu.spl181.net.srv.Reactor;
import bgu.spl181.net.srv.reactorServer;
import bgu.spl181.net.srv.sharedObject;

public class ReactorMain {
	public static void main(String[] args) {
		
		
		sharedObject<String> sharedData= new sharedObject<String>();


		//initialization
		
		UsersRepresentionAsObject fromJson_Users;

		fromJson_Users= getUsersFileAsObject();
		for(User entry : fromJson_Users.getUsers()){
			sharedData.addClient(entry.getUsername(), entry.getPassword());
				if(entry.getType().equals("admin")){
					sharedData.addAdmin(entry.getUsername());
				}
		}
		
		reactorServer Reactor= new reactorServer(
				5,
				Integer.valueOf(args[0]),
				()-> new MovieRentalServiceProtocol(sharedData) ,
				()->new LineMessageEncoderDecoder()
				);
		Reactor.serve();
		 
	}
	
	private static UsersRepresentionAsObject getUsersFileAsObject(){
		BufferedReader br = null;
		FileReader fr = null;

		try {
			fr = new FileReader("Database/Users.json");
		} catch (FileNotFoundException e) {}
		br = new BufferedReader(fr);


		Gson gson= new Gson();

		UsersRepresentionAsObject fromJson= gson.fromJson(br, UsersRepresentionAsObject.class);

		if(fromJson==null){
			fromJson= new UsersRepresentionAsObject();
		}
		return fromJson;
	}
	
	public static class UsersRepresentionAsObject {

		public UsersRepresentionAsObject(){
			this.users= new ArrayList<User>();
		}

		@SerializedName("users")
		@Expose
		private List<User> users = null;

		public List<User> getUsers() {
			return users;
		}

		public void setUsers(List<User> users) {
			this.users = users;
		}

	}
}