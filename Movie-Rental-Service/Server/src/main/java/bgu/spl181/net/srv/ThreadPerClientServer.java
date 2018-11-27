package bgu.spl181.net.srv;
import bgu.spl181.net.api.*;

import java.util.function.Supplier;

public class ThreadPerClientServer extends BaseServer<String> {
	 
    public ThreadPerClientServer(
            int port,
            Supplier<bgu.spl181.net.api.bidi.BidiMessagingProtocol<String>> protocolFactory,
            Supplier<MessageEncoderDecoder<String>> encoderDecoderFactory) {
 
        super(port, protocolFactory, encoderDecoderFactory);
    }
 
    @Override
    protected void execute(BlockingConnectionHandler<String> handler) {
        new Thread(handler).start();
    }
 
}