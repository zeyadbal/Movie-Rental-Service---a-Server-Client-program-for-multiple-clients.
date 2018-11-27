package bgu.spl181.net.impl.echo;

import bgu.spl181.net.api.bidi.Connections;

import java.time.LocalDateTime;

public class EchoProtocol implements bgu.spl181.net.api.bidi.BidiMessagingProtocol<String> {

    private boolean shouldTerminate = false;
    Connections<String> connections;
    int connectionId;
    
    @Override
    public void start(int connectionId, Connections<String> connections) {
    	this.connections=connections;
    	this.connectionId=connectionId;
    }
    
    @Override
    public void process(String msg) {
        shouldTerminate = "bye".equals(msg);
        System.out.println("[" + LocalDateTime.now() + "]: " + msg);
        connections.send(connectionId, msg);
        if("iamadmin".equals(msg)){
        	connections.broadcast("anadminhasentered");
        }
        if("closeme".equals(msg)){
        	connections.disconnect(connectionId);
        }
    }


    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
}
