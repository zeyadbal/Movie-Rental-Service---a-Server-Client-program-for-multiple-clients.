package bgu.spl181.net.impl.movieRental;
import bgu.spl181.net.srv.sharedObject;

import bgu.spl181.net.api.bidi.Connections;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MovieRentalServiceProtocol extends UserServiceTextBasedProtocol {

	String currMsg;
	
	
	/*
	 * 			IN THIS CLASS
	 * 
	 *  description:
	 * 
	 * 
	 */
	
	public MovieRentalServiceProtocol(sharedObject<String> sharedData) {
		super(sharedData);
	}
	
	
	@Override
	public void start(int connectionId, Connections<String> connections) {
		super.start(connectionId, connections);
	}
	
	@Override
	public void process(String msg) {
		
		
		this.currMsg=msg;


		String delimiter = " ";
		String[] substrings = currMsg.split(delimiter);
		String command= substrings[0];
		
		super.process(currMsg);//to handle login/sign out

		switch(command){
		
		case "REGISTER" :
			handleREGISTER(substrings);
			break;
		case "REQUEST" :
			handleREQUEST(substrings);
			break;
		default :
			return;	
		}
	}

	@Override
	public void handleREQUEST(String[]substrings){
		
		if(substrings.length<2){// Illegal command syntax
			String msg="ERROR request failed";
			connections.send(connectionId, msg);
			continueHandling=false;
			return;
		}


		String requestName="";

		if(currMsg.contains("balance info")){
			requestName="balance";
		}
		else if(currMsg.contains("balance add")){
			requestName="balance";
		}
		else if(currMsg.contains("info") && !currMsg.contains("balance info") ){
			requestName="info";
		}
		else if(currMsg.contains("rent")){
			requestName="rent";
		}
		else if(currMsg.contains("return")){
			requestName="return";
		}	
		else if(currMsg.contains("addmovie")){
			requestName="addmovie";
		}
		else if(currMsg.contains("remmovie")){
			requestName="remmovie";
		}
		else if(currMsg.contains("changeprice")){
			requestName="changeprice";
		}

		//Reason to failure: Client isn't logged in

		if(!sharedObject.isOnline_ByUsername(clientName)){
			String msg="ERROR request "+requestName+" failed";
			connections.send(connectionId, msg);
			continueHandling=false;
			return;
		}

		UsersRepresentionAsObject fromJson_Users;
		MoviesRepresentionAsObject fromJson_Movies;
		Gson gson = new GsonBuilder().create();

		
		  //   NORMAL REQUESTS
		 

		//////////////////////////////////////////////////////////////////////////////////////////////////
		
		if(currMsg.contains("balance info")){
			sharedObject.getUsersReadWriteLock().readLock().lock();

			fromJson_Users= getUsersFileAsObject();

			String balance="";
			for(User entry : fromJson_Users.getUsers()){
				if(entry.getUsername().equalsIgnoreCase(clientName)){
					balance= entry.getBalance();
					break;
				}
			}

			sharedObject.getUsersReadWriteLock().readLock().unlock();

			String msg="ACK balance ";
			msg+=balance;
			connections.send(connectionId, msg);
			return;

		}

		//////////////////////////////////////////////////////////////////////////////////////////////////

		if(currMsg.contains("balance add")){

			if(substrings.length!=4){
				String msg="ERROR request balance failed";
				connections.send(connectionId, msg);
				return;
			}

			String amount= substrings[3];

			sharedObject.getUsersReadWriteLock().readLock().lock();


			fromJson_Users= getUsersFileAsObject();

			String balance="";

			for(User entry : fromJson_Users.getUsers()){
				if(entry.getUsername().equalsIgnoreCase(clientName)){
					balance= entry.getBalance();
					break;
				}
			}

			balance= Integer.toString((Integer.parseInt(amount) + Integer.parseInt(balance)));

			sharedObject.getUsersReadWriteLock().readLock().unlock();
			sharedObject.getUsersReadWriteLock().writeLock().lock();


			fromJson_Users= getUsersFileAsObject();

			for(User entry : fromJson_Users.getUsers()){
				if(entry.getUsername().equalsIgnoreCase(clientName)){
					entry.setBalance(balance);
					break;
				}
			}

			try (Writer writer = new FileWriter("Database/Users.json")) {
				gson = new GsonBuilder().create();
				gson.toJson(fromJson_Users, writer);
			}
			catch(IOException ex){} 

			sharedObject.getUsersReadWriteLock().writeLock().unlock();

			String msg="ACK balance ";
			msg+=balance;
			connections.send(connectionId, msg);
			return;

		}

		////////////////////////////////////////////////////////////////////////////////////////////////////////

		if(currMsg.contains("info") && !currMsg.contains("balance info") ){

			String movie="";
			if(substrings.length==1){// illegal syntax
				String msg="ERROR request info failed";
				connections.send(connectionId, msg);
				return;
			}
			if(substrings.length>2){

				for(int i=2; i<substrings.length; i++){
					movie+=substrings[i];
					movie+=" ";
				}
				movie= movie.substring(0, movie.length()-1);//to remove the last space
				movie= movie.substring(1,movie.length()-1);// to remove the " "
			}


			sharedObject.getMoviesReadWriteLock().readLock().lock();		

			fromJson_Movies= getMoviesFileAsObject();

			// does the movie exist
			boolean exist=false;

			if(movie!=""){
				for(Movie entry : fromJson_Movies.getMovies()){
					if(entry.getName().equalsIgnoreCase(movie)){
						exist=true;
						break;
					}
				}
				if(!exist){
					String msg="ERROR request info failed";
					connections.send(connectionId, msg);
					return;
				}
			}

			String msg="ACK info ";

			if(movie==""){
				for(Movie entry : fromJson_Movies.getMovies()){
					msg+= "\""+entry.getName()+"\""+" ";
				}
				msg=msg.substring(0,msg.length()-1);
				connections.send(connectionId, msg);
				sharedObject.getMoviesReadWriteLock().readLock().unlock();
				return;
			}else{
				for(Movie entry : fromJson_Movies.getMovies()){
					if(entry.getName().equalsIgnoreCase(movie)){
						msg+= "\""+movie+"\""+" ";
						msg+= entry.getAvailableAmount()+" ";
						msg+= entry.getPrice()+" ";
						for(int i=0; i<entry.getBannedCountries().size(); i++){
							msg+= "\""+entry.getBannedCountries().get(i)+"\""+" ";
						}
						msg= msg.substring(0,msg.length()-1);
						break;
					}
				}

				connections.send(connectionId, msg);
				sharedObject.getMoviesReadWriteLock().readLock().unlock();		
				return;		
			}

		}

		//////////////////////////////////////////////////////////////////////////////////////////////////////////

		if(currMsg.contains("rent")){

			if(substrings.length<3){// illegal syntax
				String msg="ERROR request rent failed";
				connections.send(connectionId, msg);
				return;
			}

			//rebuild the movie information

			String movieName="";
			for(int i=2; i<substrings.length; i++){
				movieName+=substrings[i];
				movieName+=" ";
			}
			movieName= movieName.substring(0, movieName.length()-1);//to remove the last space
			movieName= movieName.substring(1,movieName.length()-1);// to remove the " "

			sharedObject.getMoviesReadWriteLock().readLock().lock();

			fromJson_Movies= getMoviesFileAsObject();

			//Reason to failure: The movie doesn't exist in the system

			Movie movie=null;
			for(Movie entry : fromJson_Movies.getMovies()){
				if(entry.getName().equalsIgnoreCase(movieName)){
					movie= entry;
					break;
				}
			}
			if(movie==null){
				String msg="ERROR request rent failed";
				connections.send(connectionId, msg);
				sharedObject.getMoviesReadWriteLock().readLock().unlock();
				return;
			}	
			sharedObject.getMoviesReadWriteLock().readLock().unlock();

			//get the user information

			sharedObject.getUsersReadWriteLock().readLock().lock();



			fromJson_Users= getUsersFileAsObject();

			User user=null;
			for(User entry2 : fromJson_Users.getUsers()){
				if(entry2.getUsername().equalsIgnoreCase(clientName)){
					user= entry2;
					break;
				}
			}
			if(user==null){
				String msg="ERROR request rent failed";
				connections.send(connectionId, msg);
				sharedObject.getUsersReadWriteLock().readLock().unlock();
				return;
			}
			sharedObject.getUsersReadWriteLock().readLock().unlock();

			//Reason to failure: The user is already renting the movie

			for(Movie entry3 : user.getMovies()){
				if(entry3.getName().equalsIgnoreCase(movie.getName())){
					String msg="ERROR request rent failed";
					connections.send(connectionId, msg);
					return;
				}
			}

			//Reason to failure: The user doesn't have enough money in their balance


			if(Integer.parseInt(user.getBalance()) - Integer.parseInt(movie.getPrice()) <0 ){
				String msg="ERROR request rent failed";
				connections.send(connectionId, msg);
				return;
			}

			//Reason to failure: There are no more copies of the movie that are available for rental

			if(Integer.parseInt(movie.getAvailableAmount())<1){
				String msg="ERROR request rent failed";
				connections.send(connectionId, msg);
				return;
			}

			//Reason to failure: The movie is banned in the user's country

			if(movie.getBannedCountries().contains(user.getCountry())){
				String msg="ERROR request rent failed";
				connections.send(connectionId, msg);
				return;
			}

			/*
			 * updating the files
			 */

			//update movies file
			sharedObject.getMoviesReadWriteLock().writeLock().lock();


			fromJson_Movies= getMoviesFileAsObject();

			String newAmount="";
			for(Movie entry3 : fromJson_Movies.getMovies()){

				if(entry3.getName().equalsIgnoreCase(movie.getName())){				
					newAmount= Integer.toString(Integer.parseInt(entry3.getAvailableAmount())-1);
					entry3.setAvailableAmount(newAmount);	
					break;
				}
			}
			try (Writer writer = new FileWriter("Database/Movies.json")) {
				gson = new GsonBuilder().create();
				gson.toJson(fromJson_Movies, writer);
			}
			catch(IOException ex){} 

			sharedObject.getMoviesReadWriteLock().writeLock().unlock();	
			//update users file
			sharedObject.getUsersReadWriteLock().writeLock().lock();



			fromJson_Users= getUsersFileAsObject();


			for(User entry4 : fromJson_Users.getUsers()){
				if(entry4.getUsername().equalsIgnoreCase(clientName)){

					Movie newMovie= new Movie();
					newMovie.setName(movie.getName());
					newMovie.setId(movie.getId());
					entry4.addMovieToMovies(newMovie);;

					String newBalance= Integer.toString(
							Integer.parseInt(entry4.getBalance())
							- Integer.parseInt(movie.getPrice())
							);

					entry4.setBalance(newBalance);
					break;
				}
			}

			try (Writer writer = new FileWriter("Database/Users.json")) {
				gson = new GsonBuilder().create();
				gson.toJson(fromJson_Users, writer);
			}
			catch(IOException ex){} 
			sharedObject.getUsersReadWriteLock().writeLock().unlock();



			String msg="ACK rent "+movie.getName()+" success";
			connections.send(connectionId, msg);

			String broadcast="BROADCAST movie "+"\""+movie.getName()+"\""+" "+newAmount+" "+movie.getPrice();
			sharedObject.broadcastToOnlines(broadcast);

			return;

		}

		///////////////////////////////////////////////////////////////////////////////////////////////////////////

		if(currMsg.contains("return")){



			if(substrings.length<3){// illegal syntax
				String msg="ERROR request return failed";
				connections.send(connectionId, msg);
				return;
			}

			//rebuild the movie information

			String movieName="";
			for(int i=2; i<substrings.length; i++){
				movieName+=substrings[i];
				movieName+=" ";
			}
			movieName= movieName.substring(0, movieName.length()-1);//to remove the last space
			movieName= movieName.substring(1,movieName.length()-1);// to remove the " "

			sharedObject.getMoviesReadWriteLock().readLock().lock();

			fromJson_Movies= getMoviesFileAsObject();

			//Reason to failure: The movie doesn't exist in the system

			Movie movie=null;
			for(Movie entry : fromJson_Movies.getMovies()){
				if(entry.getName().equalsIgnoreCase(movieName)){
					movie= entry;
					break;
				}
			}
			if(movie==null){
				String msg="ERROR request return failed";
				connections.send(connectionId, msg);
				sharedObject.getMoviesReadWriteLock().readLock().unlock();
				return;
			}	
			sharedObject.getMoviesReadWriteLock().readLock().unlock();

			//get the user information

			sharedObject.getUsersReadWriteLock().readLock().lock();



			fromJson_Users= getUsersFileAsObject();

			User user=null;
			for(User entry2 : fromJson_Users.getUsers()){
				if(entry2.getUsername().equalsIgnoreCase(clientName)){
					user= entry2;
					break;
				}
			}
			if(user==null){
				String msg="ERROR request return failed";
				connections.send(connectionId, msg);
				sharedObject.getUsersReadWriteLock().readLock().unlock();
				return;
			}
			sharedObject.getUsersReadWriteLock().readLock().unlock();


			//Reason to failure: The user is currently not renting the movie

			boolean exist=false;
			for(Movie entry3 : user.getMovies()){
				if(entry3.getName().equalsIgnoreCase(movie.getName())){
					exist=true;
					break;
				}
			}
			if(!exist){
				String msg="ERROR request return failed";
				connections.send(connectionId, msg);
				return;
			}

			/*
			 * updating the files
			 */

			//update movies file
			sharedObject.getMoviesReadWriteLock().writeLock().lock();


			fromJson_Movies= getMoviesFileAsObject();

			String newAmount="";
			for(Movie entry3 : fromJson_Movies.getMovies()){

				if(entry3.getName().equalsIgnoreCase(movie.getName())){				
					newAmount= Integer.toString(Integer.parseInt(entry3.getAvailableAmount())+1);
					entry3.setAvailableAmount(newAmount);	
					break;
				}
			}
			try (Writer writer = new FileWriter("Database/Movies.json")) {
				gson = new GsonBuilder().create();
				gson.toJson(fromJson_Movies, writer);
			}
			catch(IOException ex){} 

			sharedObject.getMoviesReadWriteLock().writeLock().unlock();	
			//update users file
			sharedObject.getUsersReadWriteLock().writeLock().lock();



			fromJson_Users= getUsersFileAsObject();


			for(User entry4 : fromJson_Users.getUsers()){
				if(entry4.getUsername().equalsIgnoreCase(clientName)){

					entry4.removeMovieFromMovies(movie.getName());
					break;
				}
			}

			try (Writer writer = new FileWriter("Database/Users.json")) {
				gson = new GsonBuilder().create();
				gson.toJson(fromJson_Users, writer);
			}
			catch(IOException ex){} 
			sharedObject.getUsersReadWriteLock().writeLock().unlock();



			String msg="ACK return "+movie.getName()+" success";
			connections.send(connectionId, msg);

			String broadcast="BROADCAST movie "+"\""+movie.getName()+"\""+" "+newAmount+" "+movie.getPrice();
			sharedObject.broadcastToOnlines(broadcast);

			return;

		}

		////////////////////////////////////////////////////////////////////////////////////////////////////		

		/*
		 *     ADMIN REQUESTS
		 */

		//////////////////////////////////////////////////////////////////////////////////////////////////////

		if(currMsg.contains("addmovie")){

			if(!sharedObject.isAdmin(clientName)){
				String msg="ERROR request addmovie failed";
				connections.send(connectionId, msg);
				return;
			}
			
			breakAddMovieCommand breakCmd= new breakAddMovieCommand(currMsg);

			if(!breakCmd.isLegalSyntax() | !sharedObject.isAdmin(clientName)){
				String msg="ERROR request addmovie failed";
				connections.send(connectionId, msg);
				return;
			}
			String movieName= breakCmd.getMovieName();
			ArrayList<String> bannedCountries= breakCmd.getBannedCountries();	
			String amount= breakCmd.getAmount();
			String price= breakCmd.getPrice();
					
			if(!breakCmd.isLegalSyntax()){
				String msg="ERROR request addmovie failed";
				connections.send(connectionId, msg);
				return;
			}
			
			boolean legal_PRICE_AMOUNT=true;
			
			legal_PRICE_AMOUNT= Integer.parseInt(amount)>0 & Integer.parseInt(price)>0;
			
			if(!legal_PRICE_AMOUNT){
				String msg="ERROR request addmovie failed";
				connections.send(connectionId, msg);
				return;
			}

			sharedObject.getMoviesReadWriteLock().readLock().lock();

			fromJson_Movies= getMoviesFileAsObject();

			//Reason to failure: The movie already exists in the system

			int maxId=0;
			
			Movie movie=null;
			for(Movie entry : fromJson_Movies.getMovies()){
				if(Integer.parseInt(entry.getId())>maxId){
					maxId= Integer.parseInt(entry.getId());
				}
				if(entry.getName().equalsIgnoreCase(movieName)){
					movie= entry;
					break;
				}
			}
			if(movie!=null){
				String msg="ERROR request return failed";
				connections.send(connectionId, msg);
				sharedObject.getMoviesReadWriteLock().readLock().unlock();
				return;
			}	
			sharedObject.getMoviesReadWriteLock().readLock().unlock();

			//get the user information

			sharedObject.getMoviesReadWriteLock().writeLock().lock();

			fromJson_Movies= getMoviesFileAsObject();
			
			movie= new Movie();
			movie.setAvailableAmount(amount);
			movie.setBannedCountries(bannedCountries);
			movie.setName(movieName);
			movie.setTotalAmount(amount);
			movie.setPrice(price);
			movie.setId(Integer.toString(maxId+1));
			
			fromJson_Movies.getMovies().add(movie);
			
			try (Writer writer = new FileWriter("Database/Movies.json")) {
				gson = new GsonBuilder().create();
				gson.toJson(fromJson_Movies, writer);
			}
			catch(IOException ex){} 

			sharedObject.getMoviesReadWriteLock().writeLock().unlock();	
			

			String msg="ACK addmovie "+"\""+movie.getName()+"\""+" success";
			connections.send(connectionId, msg);

			String broadcast="BROADCAST movie "+"\""+movie.getName()+"\""+" "+movie.getAvailableAmount()+" "+movie.getPrice();
			sharedObject.broadcastToOnlines(broadcast);

			return;

		}
		
///////////////////////////////////////////////////////////////////////////////////////////////////////////////		
		
		if(currMsg.contains("remmovie")){

			if(!sharedObject.isAdmin(clientName)){
				String msg="ERROR request remmovie failed";
				connections.send(connectionId, msg);
				return;
			}
			
			if(substrings.length<3){// illegal syntax
				String msg="ERROR request remmovie failed";
				connections.send(connectionId, msg);
				return;
			}
			
			String movieName="";
			for(int i=2; i<substrings.length; i++){
				movieName+=substrings[i];
				movieName+=" ";
			}
			movieName= movieName.substring(0, movieName.length()-1);//to remove the last space
			movieName= movieName.substring(1,movieName.length()-1);// to remove the " "

			sharedObject.getMoviesReadWriteLock().readLock().lock();

			fromJson_Movies= getMoviesFileAsObject();

			//Reason to failure: The movie doesn't exists in the system

			Movie movie=null;
			for(Movie entry : fromJson_Movies.getMovies()){
				if(entry.getName().equalsIgnoreCase(movieName)){
					movie= entry;
					break;
				}
			}
			if(movie==null){
				String msg="ERROR request remmovie failed";
				connections.send(connectionId, msg);
				sharedObject.getMoviesReadWriteLock().readLock().unlock();
				return;
			}	
			
			//Reason to failure: There is atleast one copy of the movie that is currently rented by a user
			
			String available= movie.getAvailableAmount();
			String total= movie.getTotalAmount();
			if(Integer.parseInt(available) != Integer.parseInt(total)){
				String msg="ERROR request remmovie failed";
				connections.send(connectionId, msg);
				sharedObject.getMoviesReadWriteLock().readLock().unlock();
				return;
			}
			
			sharedObject.getMoviesReadWriteLock().readLock().unlock();

			sharedObject.getMoviesReadWriteLock().writeLock().lock();

			fromJson_Movies= getMoviesFileAsObject();
			
			fromJson_Movies.removeMovieFromMovies(movieName);
			
			try (Writer writer = new FileWriter("Database/Movies.json")) {
				gson = new GsonBuilder().create();
				gson.toJson(fromJson_Movies, writer);
			}
			catch(IOException ex){} 

			sharedObject.getMoviesReadWriteLock().writeLock().unlock();	
			

			String msg="ACK remmovie "+movie.getName()+" success";
			connections.send(connectionId, msg);

			String broadcast="BROADCAST movie "+"\""+movie.getName()+"\""+" removed";
			sharedObject.broadcastToOnlines(broadcast);

			return;

		}
		
/////////////////////////////////////////////////////////////////////////////////////////////////////////////		
		
		if(currMsg.contains("changeprice")){

			if(!sharedObject.isAdmin(clientName)){
				String msg="ERROR request changeprice failed";
				connections.send(connectionId, msg);
				return;
			}
			
			if(substrings.length<3){// illegal syntax
				String msg="ERROR request changeprice failed";
				connections.send(connectionId, msg);
				return;
			}
			
			String movieName="";
			int i=0;
			for( i=0; i<currMsg.length(); i++){
				if(currMsg.charAt(i)!='\"'){
					i++;
				}else{
					i++;
					break;
				}
			}
			for(int j=i; j<currMsg.length(); j++){
				if(currMsg.charAt(j)!='\"'){
				movieName+= currMsg.charAt(j);
			}else{
				break;
			}
			}

				
				//Reason to failure: Price is smaller than or equals 0
				
				String price= substrings[substrings.length-1];
				
				if(Integer.parseInt(price)<1){// illegal syntax
					String msg="ERROR request changeprice failed";
					connections.send(connectionId, msg);
					return;
				}

			sharedObject.getMoviesReadWriteLock().readLock().lock();

			fromJson_Movies= getMoviesFileAsObject();

			//Reason to failure: The movie doesn't exists in the system

			Movie movie=null;
			for(Movie entry : fromJson_Movies.getMovies()){
				if(entry.getName().equalsIgnoreCase(movieName)){
					movie= entry;
					break;
				}
			}
			if(movie==null){
				String msg="ERROR request changeprice failed";
				connections.send(connectionId, msg);
				sharedObject.getMoviesReadWriteLock().readLock().unlock();
				return;
			}				
			
			sharedObject.getMoviesReadWriteLock().readLock().unlock();

			sharedObject.getMoviesReadWriteLock().writeLock().lock();

			fromJson_Movies= getMoviesFileAsObject();
			
			for(Movie entry : fromJson_Movies.getMovies()){
				if(entry.getName().equalsIgnoreCase(movieName)){
					entry.setPrice(price);
					break;
				}
			}
			
			try (Writer writer = new FileWriter("Database/Movies.json")) {
				gson = new GsonBuilder().create();
				gson.toJson(fromJson_Movies, writer);
			}
			catch(IOException ex){} 

			sharedObject.getMoviesReadWriteLock().writeLock().unlock();	
			

			String msg="ACK changeprice "+movie.getName()+" success";
			connections.send(connectionId, msg);

			String broadcast="BROADCAST movie "+"\""+movie.getName()+"\""+" "+movie.getAvailableAmount()+" "+price;
			sharedObject.broadcastToOnlines(broadcast);

			return;
			}
		}
	
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	

	@Override
	public void handleREGISTER(String[]substrings){
		
		//Reason to failure: The client performing the register call is already logged in

		if(sharedObject.isOnline_ById(connectionId)){
			String msg="ERROR registration failed";
			connections.send(connectionId, msg);
			continueHandling=false;
			return;
		}

		//Reason to failure: Missing info (user name/password)

		if(substrings.length<3){
			String msg="ERROR registration failed";
			connections.send(connectionId, msg);
			continueHandling=false;
			return;
		}
		
		UsersRepresentionAsObject fromJson_Users;
		Gson gson = new GsonBuilder().create();

		//Reason to failure: Data block doesn't fit service requirement

		if(substrings.length>=4){

			String country="";
			for(int i=3; i<substrings.length; i++){
				country+=substrings[i];
				country+=" ";
			}
			country= country.substring(0, country.length()-1);//to remove the last space
			substrings[3]= country;	

			if( !substrings[3].contains("country=")){
				String msg="ERROR registration failed";
				connections.send(connectionId, msg);
				return;
			}
		}


		//Reason to failure: The user name requested is already register in the system

		sharedObject.getUsersReadWriteLock().readLock().lock();

		fromJson_Users= getUsersFileAsObject();

		for(User entry : fromJson_Users.getUsers()){
			if(entry.getUsername().equals(substrings[1])){
				String msg="ERROR registration failed";
				connections.send(connectionId, msg);
				sharedObject.getUsersReadWriteLock().readLock().unlock();
				return;
			}
		}
		sharedObject.getUsersReadWriteLock().readLock().unlock();
		
		

		User newUser= new User();
		newUser.setUsername(substrings[1]);
		newUser.setPassword(substrings[2]);
		newUser.setType("normal");
		newUser.setBalance("0");
		if(substrings.length>=4){
			newUser.setCountry(substrings[3].substring(9,substrings[3].length()-1));
		}

		sharedObject.getUsersReadWriteLock().writeLock().lock();

		fromJson_Users= getUsersFileAsObject();

		fromJson_Users.getUsers().add(newUser);

		try (Writer writer = new FileWriter("Database/Users.json")) {
			gson = new GsonBuilder().create();
			gson.toJson(fromJson_Users, writer);
		}
		catch(IOException ex){} 

		sharedObject.getUsersReadWriteLock().writeLock().unlock();

		
		if(!sharedObject.getClients().containsKey(substrings[1])){
			sharedObject.addClient(substrings[1], substrings[2]);
		}else{
			String msg="ERROR registration failed";
			connections.send(connectionId, msg);
			continueHandling=false;
			return;
		}
		
		
		String msg="ACK registration succeeded";
		connections.send(connectionId, msg);
	}

	private UsersRepresentionAsObject getUsersFileAsObject(){
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

	private MoviesRepresentionAsObject getMoviesFileAsObject(){
		BufferedReader br = null;
		FileReader fr = null;

		try {
			fr = new FileReader("Database/Movies.json");
		} catch (FileNotFoundException e) {}
		br = new BufferedReader(fr);


		Gson gson= new Gson();

		MoviesRepresentionAsObject fromJson= gson.fromJson(br, MoviesRepresentionAsObject.class);

		if(fromJson==null){
			fromJson= new MoviesRepresentionAsObject();
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

	public static class MoviesRepresentionAsObject{

		public MoviesRepresentionAsObject(){
			this.movies= new ArrayList<Movie>();
		}

		@SerializedName("movies")
		@Expose
		private List<Movie> movies = null;

		public List<Movie> getMovies() {
			return movies;
		}

		public void setMovies(List<Movie> movies) {
			this.movies = movies;
		}
		
		public void removeMovieFromMovies(String movieName){
			for(int i=0; i<this.movies.size(); i++){
				if(movies.get(i).getName().equalsIgnoreCase(movieName)){
					this.movies.remove(i);
					break;
				}
			}
		}

	}

	public static class User {
		public User(){
			this.movies= new ArrayList<Movie>();
		}
		@SerializedName("username")
		@Expose
		private String username;
		@SerializedName("type")
		@Expose
		private String type;
		@SerializedName("password")
		@Expose
		private String password;
		@SerializedName("country")
		@Expose
		private String country;
		@SerializedName("movies")
		@Expose
		private List<Movie> movies = null;
		@SerializedName("balance")
		@Expose
		private String balance;

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}

		public List<Movie> getMovies() {
			return movies;
		}

		public void setMovies(List<Movie> movies) {
			this.movies = movies;
		}

		public void addMovieToMovies(Movie movie){
			movie.setBannedCountries(null);
			this.movies.add(movie);
		}

		public void removeMovieFromMovies(String movieName){
			for(int i=0; i<this.movies.size(); i++){
				if(movies.get(i).getName().equalsIgnoreCase(movieName)){
					this.movies.remove(i);
					break;
				}
			}
		}

		public String getBalance() {
			return balance;
		}

		public void setBalance(String balance) {
			this.balance = balance;
		}

	}

	public static class Movie {

		@SerializedName("id")
		@Expose
		private String id;
		@SerializedName("name")
		@Expose
		private String name;
		@SerializedName("price")
		@Expose
		private String price;
		@SerializedName("bannedCountries")
		@Expose
		private List<String> bannedCountries = null;
		@SerializedName("availableAmount")
		@Expose
		private String availableAmount;
		@SerializedName("totalAmount")
		@Expose
		private String totalAmount;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPrice() {
			return price;
		}

		public void setPrice(String price) {
			this.price = price;
		}

		public List<String> getBannedCountries() {
			if(this.bannedCountries==null){
				this.bannedCountries= new ArrayList<String>();
			}
			return bannedCountries;
		}

		public void setBannedCountries(List<String> bannedCountries) {
			this.bannedCountries = bannedCountries;
		}

		public void addCountryToBannedCountries(String country ){
			if(this.bannedCountries==null){
				this.bannedCountries= new ArrayList<String>();
			}
			bannedCountries.add(country);
		}

		public String getAvailableAmount() {
			return availableAmount;
		}

		public void setAvailableAmount(String availableAmount) {
			this.availableAmount = availableAmount;
		}

		public String getTotalAmount() {
			return totalAmount;
		}

		public void setTotalAmount(String totalAmount) {
			this.totalAmount = totalAmount;
		}
	}
	private static class breakAddMovieCommand{
		private String command;
		private String movieName;
		private ArrayList<String> bannedCountries;
		int i=0;
		boolean legalSyntax=true;
		String amount="";
		String price="";

		public breakAddMovieCommand(String command) {
			this.command=command;
			this.movieName="";
			this.bannedCountries= new ArrayList<String>();
			// illegal syntax
			if(command.length()<5){
				legalSyntax=false;
			}
			int n=0;
			for(int j=0;legalSyntax && j<command.length(); j++){
				if(command.charAt(j)=='\"'){
					n++;
				}
			}
			if(legalSyntax){
				legalSyntax= n%2==0 & n>=2;
			}
			if(!legalSyntax){
				return;
			}
			
			getNextCommasIndex();
			i++;
			while(i<command.length()){
				if(command.charAt(i)!='\"'){
					movieName+= command.charAt(i);
					i++;
				}else{
					break;
				}
			}
			
			i++;
			i++;

			while(i<command.length()){
				if(command.charAt(i)!=' '){
					amount+=command.charAt(i);
					i++;
				}
				else{
					break;
				}
			}
			
			i++;
			
			while(i<command.length()){
				if(command.charAt(i)!=' '){
					price+=command.charAt(i);
					i++;
				}
				else{
					break;
				}
			}
			
			
			createBannedCountries();
			
		}

		public void createBannedCountries(){

			String bannedCountry="";

			getNextCommasIndex();
			i++;
			while(i<command.length()){
				if(command.charAt(i)!='\"'){
					bannedCountry+= command.charAt(i);
					i++;
				}else{
					i++;
					bannedCountries.add(bannedCountry);
					createBannedCountries();
				}
			}
		}

		public ArrayList<String> getBannedCountries(){
			return this.bannedCountries;
		}
		private void getNextCommasIndex(){
			while(i<command.length()){
				if(command.charAt(i)=='\"'){
					break;
				}
				else{
					i++;
				}
			}			
		}
		public boolean isLegalSyntax(){
			return legalSyntax;
		}

		public String getAmount(){
			return amount;
		}
		public String getPrice(){
			return price;
		}
		public String getMovieName(){
			return movieName;
		}

	}
}

