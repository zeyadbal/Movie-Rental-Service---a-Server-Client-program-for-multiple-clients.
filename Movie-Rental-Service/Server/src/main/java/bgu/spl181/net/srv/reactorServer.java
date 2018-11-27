package bgu.spl181.net.srv;
import bgu.spl181.net.api.*;

import java.util.function.Supplier;

public class reactorServer extends Reactor<String> {
	 
    public reactorServer(
    		            int nthreads,
    		            int port,
    		            Supplier<bgu.spl181.net.api.bidi.BidiMessagingProtocol<String>> protocolFactory,
    		            Supplier<MessageEncoderDecoder<String>> encoderDecoderFactory) {
 
        super(nthreads, port, protocolFactory, encoderDecoderFactory);
    }
 
}