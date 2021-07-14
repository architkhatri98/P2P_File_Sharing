import java.net.*;
import java.io.*;

public class RemotePeerHandler implements Runnable, RequiredConstants {
	private Socket socket_peer = null;
	private InputStream in;
	private OutputStream out;
	private int type_ofConnection;

	private handshake_msg handshake_msg;

	String peer_id_own, pID_remote;

	final int ACTIVECONN = 1;
	final int PASSIVECONN = 0;

	public void close_open(InputStream i, Socket socket) {
		try {
			i.close();
			i = socket.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public RemotePeerHandler(Socket socket_peer, int type_ofConnection, String peer_id_own) {

		this.socket_peer = socket_peer;
		this.type_ofConnection = type_ofConnection;
		this.peer_id_own = peer_id_own;
		try {
			in = socket_peer.getInputStream();
			out = socket_peer.getOutputStream();
		} catch (Exception ex) {
			peerProcess.printLogs(this.peer_id_own + " Error found : " + ex.getMessage());
		}
	}

	public RemotePeerHandler(String add, int port, int type_ofConnection, String peer_id_own) {
		try {
			this.type_ofConnection = type_ofConnection;
			this.peer_id_own = peer_id_own;
			this.socket_peer = new Socket(add, port);
		} catch (UnknownHostException e) {
			peerProcess.printLogs(peer_id_own + " RemotePeerHandler : " + e.getMessage());
		} catch (IOException e) {
			peerProcess.printLogs(peer_id_own + " RemotePeerHandler : " + e.getMessage());
		}
		this.type_ofConnection = type_ofConnection;

		try {
			in = socket_peer.getInputStream();
			out = socket_peer.getOutputStream();
		} catch (Exception ex) {
			peerProcess.printLogs(peer_id_own + " RemotePeerHandler : " + ex.getMessage());
		}
	}

	public boolean SendHandshake() {
		try {
			out.write(
					handshake_msg.encode_msg(new handshake_msg(RequiredConstants.header_handshake, this.peer_id_own)));
		} catch (IOException e) {
			peerProcess.printLogs(this.peer_id_own + " SendHandshake : " + e.getMessage());
			return false;
		}
		return true;
	}

	public boolean ReceiveHandshake() {
		byte[] receivedHandshakeByte = new byte[32];
		try {
			in.read(receivedHandshakeByte);
			handshake_msg = handshake_msg.decode_msg(receivedHandshakeByte);
			pID_remote = handshake_msg.get_pidString();

			peerProcess.pIDToSocketMap.put(pID_remote, this.socket_peer);
		} catch (IOException e) {
			peerProcess.printLogs(this.peer_id_own + " ReceiveHandshake : " + e.getMessage());
			return false;
		}
		return true;
	}

	public boolean request_send(int index) {
		try {
			out.write(Actual_msg.encode_msg(new Actual_msg(data_request_msg, Actual_msg_helper.ArrayIntToByte(index))));
		} catch (IOException e) {
			peerProcess.printLogs(this.peer_id_own + " request_send : " + e.getMessage());
			return false;
		}
		return true;
	}

	public boolean interested_send() {
		try {
			out.write(Actual_msg.encode_msg(new Actual_msg(data_interested_msg)));
		} catch (IOException e) {
			peerProcess.printLogs(this.peer_id_own + " interested_send : " + e.getMessage());
			return false;
		}
		return true;
	}

	public boolean not_interested_send() {
		try {
			out.write(Actual_msg.encode_msg(new Actual_msg(data_notinterested_msg)));
		} catch (IOException e) {
			peerProcess.printLogs(this.peer_id_own + " not_interested_send : " + e.getMessage());
			return false;
		}

		return true;
	}

	public boolean unchoke_receiver() {
		byte[] unchokeByte_receive = null;

		try {
			in.read(unchokeByte_receive);
		} catch (IOException e) {
			peerProcess.printLogs(this.peer_id_own + " unchoke_receiver : " + e.getMessage());
			return false;
		}

		Actual_msg m = Actual_msg.decode_msg(unchokeByte_receive);
		if (m.get_msgString().equals(data_unchoke_msg)) {
			peerProcess.printLogs(peer_id_own + "is unchoked by " + pID_remote);
			return true;
		} else
			return false;
	}

	public boolean choke_receive() {
		byte[] receiveChokeByte = null;

		try {
			if (in.available() == 0)
				return false;
		} catch (IOException e) {
			peerProcess.printLogs(this.peer_id_own + " choke_receive : " + e.getMessage());
			return false;
		}
		try {
			in.read(receiveChokeByte);
		} catch (IOException e) {
			peerProcess.printLogs(this.peer_id_own + " choke_receive : " + e.getMessage());
			return false;
		}
		Actual_msg m = Actual_msg.decode_msg(receiveChokeByte);
		if (m.get_msgString().equals(data_choke_msg)) {
			peerProcess.printLogs(peer_id_own + " is CHOKED by " + pID_remote);
			return true;
		} else
			return false;
	}

	public boolean piece_receive() {
		byte[] piece_receive = null;

		try {
			in.read(piece_receive);
		} catch (IOException e) {
			peerProcess.printLogs(this.peer_id_own + " piece_receive : " + e.getMessage());
			return false;
		}

		Actual_msg m = Actual_msg.decode_msg(piece_receive);
		if (m.get_msgString().equals(data_unchoke_msg)) {
	
			peerProcess.printLogs(peer_id_own + " is UNCHOKED by " + pID_remote);
			return true;
		} else
			return false;

	}

	public void run() {
		byte[] handshakeBuff = new byte[32];
		byte[] dataBuffWithoutPayload = new byte[data_lenOf_msg + data_type_msg];
		byte[] msgLength;
		byte[] msgType;
		Actual_msg_helper dataMsgWrapper = new Actual_msg_helper();

		try {
			if (this.type_ofConnection == ACTIVECONN) {
				if (!SendHandshake()) {
					peerProcess.printLogs(peer_id_own + " HANDSHAKE sending failed.");
					System.exit(0);
				} else {
					peerProcess.printLogs(peer_id_own + " HANDSHAKE has been sent...");
				}
				while (true) {
					in.read(handshakeBuff);
					handshake_msg = handshake_msg.decode_msg(handshakeBuff);
					if (handshake_msg.header_getString().equals(RequiredConstants.header_handshake)) {

						pID_remote = handshake_msg.get_pidString();

						peerProcess.printLogs(peer_id_own + " makes a connection to Peer " + pID_remote);

						peerProcess.printLogs(peer_id_own + " Received a HANDSHAKE message from Peer " + pID_remote);

						peerProcess.pIDToSocketMap.put(pID_remote, this.socket_peer);
						break;
					} else {
						continue;
					}
				}

				Actual_msg d = new Actual_msg(data_bitfield_msg, peerProcess.ownBitField.encode());
				byte[] b = Actual_msg.encode_msg(d);
				out.write(b);
				peerProcess.peerInfoHashTable.get(pID_remote).state = 8;
			}

			else {
				while (true) {
					in.read(handshakeBuff);
					handshake_msg = handshake_msg.decode_msg(handshakeBuff);
					if (handshake_msg.header_getString().equals(RequiredConstants.header_handshake)) {
						pID_remote = handshake_msg.get_pidString();

						peerProcess.printLogs(peer_id_own + " makes a connection to Peer " + pID_remote);
						peerProcess.printLogs(peer_id_own + " Received a HANDSHAKE message from Peer " + pID_remote);

						peerProcess.pIDToSocketMap.put(pID_remote, this.socket_peer);
						break;
					} else {
						continue;
					}
				}
				if (!SendHandshake()) {
					peerProcess.printLogs(peer_id_own + " HANDSHAKE message sending failed.");
					System.exit(0);
				} else {
					peerProcess.printLogs(peer_id_own + " HANDSHAKE message has been sent successfully.");
				}

				peerProcess.peerInfoHashTable.get(pID_remote).state = 2;
			}
			while (true) {

				int headerBytes = in.read(dataBuffWithoutPayload);

				if (headerBytes == -1)
					break;

				msgLength = new byte[data_lenOf_msg];
				msgType = new byte[data_type_msg];
				System.arraycopy(dataBuffWithoutPayload, 0, msgLength, 0, data_lenOf_msg);
				System.arraycopy(dataBuffWithoutPayload, data_lenOf_msg, msgType, 0, data_type_msg);
				Actual_msg Actual_msg = new Actual_msg();
				Actual_msg.set_msg_len(msgLength);
				Actual_msg.set_msg_type(msgType);
				if (Actual_msg.get_msgString().equals(RequiredConstants.data_choke_msg)
						|| Actual_msg.get_msgString().equals(RequiredConstants.data_unchoke_msg)
						|| Actual_msg.get_msgString().equals(RequiredConstants.data_interested_msg)
						|| Actual_msg.get_msgString().equals(RequiredConstants.data_notinterested_msg)) {
					dataMsgWrapper.dataMsg = Actual_msg;
					dataMsgWrapper.pID_from = this.pID_remote;
					peerProcess.addMsgToQueue(dataMsgWrapper);
				} else {
					int bytesAlreadyRead = 0;
					int bytesRead;
					byte[] dataBuffPayload = new byte[Actual_msg.get_msg_lenInt() - 1];
					while (bytesAlreadyRead < Actual_msg.get_msg_lenInt() - 1) {
						bytesRead = in.read(dataBuffPayload, bytesAlreadyRead,
								Actual_msg.get_msg_lenInt() - 1 - bytesAlreadyRead);
						if (bytesRead == -1)
							return;
						bytesAlreadyRead += bytesRead;
					}

					byte[] dataBuffWithPayload = new byte[Actual_msg.get_msg_lenInt() + data_lenOf_msg];
					System.arraycopy(dataBuffWithoutPayload, 0, dataBuffWithPayload, 0, data_lenOf_msg + data_type_msg);
					System.arraycopy(dataBuffPayload, 0, dataBuffWithPayload, data_lenOf_msg + data_type_msg,
							dataBuffPayload.length);

					Actual_msg dataMsgWithPayload = Actual_msg.decode_msg(dataBuffWithPayload);
					dataMsgWrapper.dataMsg = dataMsgWithPayload;
					dataMsgWrapper.pID_from = pID_remote;
					peerProcess.addMsgToQueue(dataMsgWrapper);
					dataBuffPayload = null;
					dataBuffWithPayload = null;
					bytesAlreadyRead = 0;
					bytesRead = 0;
				}
			}
		} catch (IOException e) {
			peerProcess.printLogs(peer_id_own + " run exception: " + e);
		}

	}

	public void socket_release() {
		try {
			if (this.type_ofConnection == PASSIVECONN && this.socket_peer != null) {
				this.socket_peer.close();
			}
			if (in != null) {
				in.close();
			}
			if (out != null)
				out.close();
		} catch (IOException e) {
			peerProcess.printLogs(peer_id_own + " Release socket IO exception: " + e);
		}
	}
}