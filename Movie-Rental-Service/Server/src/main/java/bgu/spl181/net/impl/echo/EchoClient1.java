package bgu.spl181.net.impl.echo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;

public class EchoClient1 {

	public static void main(String[] args) throws IOException {

		if (args.length == 0) {
			args = new String[]{"localhost"};
		}

		//BufferedReader and BufferedWriter automatically using UTF-8 encoding
		try (Socket sock = new Socket(args[0], 7777);
				BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()))) {

			
			System.out.println("sending message to server");
			out.write("hi\n");
			out.newLine();
			out.flush();

			System.out.println("awaiting response");
			
			while(!sock.isClosed()){
				
				String line = in.readLine();
				if(line!=null){
				System.out.println("message from server: " + line);
				}
			}
		
		}
	}
}
