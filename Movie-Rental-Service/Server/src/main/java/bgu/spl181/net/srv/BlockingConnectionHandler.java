package bgu.spl181.net.srv;
import bgu.spl181.net.api.MessageEncoderDecoder;
import bgu.spl181.net.api.bidi.Connections;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BlockingConnectionHandler<T> implements Runnable, bgu.spl181.net.srv.bidi.ConnectionHandler<T> {

	private final bgu.spl181.net.api.bidi.BidiMessagingProtocol<T> protocol;
	private final MessageEncoderDecoder<T> encdec;
	private final Socket sock;
	private BufferedInputStream in;
	private BufferedOutputStream out;
	private volatile boolean connected = true;
	Connections<T> CurrentConnections;
	Integer id;

	public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader,
			bgu.spl181.net.api.bidi.BidiMessagingProtocol<T> protocol, Connections<T> CurrentConnections,
			Integer id) {
		this.sock = sock;
		this.encdec = reader;
		this.protocol = protocol;
		this.CurrentConnections= CurrentConnections;
		this.id= id;

		protocol.start(id, CurrentConnections);
		
		
	}

	@Override
	public void run() {
		try (   Socket sock = this.sock; ){ //just for automatic closing

			int read;
			in = new BufferedInputStream(sock.getInputStream());
			out = new BufferedOutputStream(sock.getOutputStream());


			while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
				T nextMessage = encdec.decodeNextByte((byte) read);
				if (nextMessage != null) {
					protocol.process(nextMessage);
				}


			}

		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	@Override
	public void send(T msg) {

		try {
			out.write(encdec.encode(msg));
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	@Override
	public void close() throws IOException {
		System.out.println("closing a socket");
		connected = false;
		sock.close();
	}

}
