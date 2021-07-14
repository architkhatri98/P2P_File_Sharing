import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class listener implements Runnable {
	private ServerSocket SocketListening;
	private String pID;
	Socket socket_remote;
	Thread thread_send;

	public listener(ServerSocket socket, String pID) {
		this.SocketListening = socket;
		this.pID = pID;
	}

	public void run() {
		while (true) {
			try {
				socket_remote = SocketListening.accept();
				thread_send = new Thread(new RemotePeerHandler(socket_remote, 0, pID));
				peerProcess.printLogs(pID + " Connection established successfully!");
				peerProcess.thread_send.add(thread_send);
				thread_send.start();
			} catch (Exception e) {
				peerProcess.printLogs(this.pID + " Exception found in connection: " + e.toString());
			}
		}
	}

	public void socket_release() {
		try {
			if (!socket_remote.isClosed())
				socket_remote.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
