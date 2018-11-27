package bgu.spl181.net.impl.echo;
import bgu.spl181.net.api.*;
import bgu.spl181.net.srv.*;

import java.io.IOException;
import java.util.function.Supplier;

public class EchoServer {
	 
	public static void main(String[] args) {
		
		 Supplier<bgu.spl181.net.api.bidi.BidiMessagingProtocol<String>> protocolFactory= ()->	new EchoProtocol(); 
		 Supplier<MessageEncoderDecoder<String>> LineMessageEncoderDecoderFactory= ()->
				new LineMessageEncoderDecoder();

		
		ThreadPerClientServer ThreadPerClientServer= new ThreadPerClientServer
		(Integer.valueOf(args[0]), protocolFactory, LineMessageEncoderDecoderFactory);
		ThreadPerClientServer.serve();
		
	}
	

}
