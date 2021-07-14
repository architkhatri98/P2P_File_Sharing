import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Date;
import java.util.Enumeration;

public class Msg_handler implements Runnable, RequiredConstants {

	RandomAccessFile rand_file;
	private static boolean running = true;
	private static String pID_this = null;
	public static int state_of_peer = -1;

	// constructor
	public Msg_handler(String peer_ID_this) {
		pID_this = peer_ID_this;
	}

	// constructor
	public Msg_handler() {
		pID_this = null;
	}

	// prints the state and data message type in the log file.
	public void print_state(String dataType, int state) {
		peerProcess.printLogs("Message Processor : msgType = " + dataType + " State = " + state);
	}

	// message exchange main function
	public void run() {
		Actual_msg dm;
		Actual_msg_helper data_wrapper;
		String msgType;
		String receiving_peer;

		while (running) {
			data_wrapper = peerProcess.removeMsgQueue();
			while (data_wrapper == null) {
				Thread.currentThread();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				data_wrapper = peerProcess.removeMsgQueue();
			}

			dm = data_wrapper.data_msg_get();

			// checks if have message is received and if received then is it interested or
			// not.
			msgType = dm.get_msgString();
			receiving_peer = data_wrapper.from_pIDGet();
			int state = peerProcess.peerInfoHashTable.get(receiving_peer).state;
			if (msgType.equals(data_have_msg) && state != 14) {
				peerProcess.printLogs(peerProcess.pID + " receieved HAVE message from Peer " + receiving_peer);
				if (is_interested(dm, receiving_peer)) {
					interested_send(peerProcess.pIDToSocketMap.get(receiving_peer), receiving_peer);
					peerProcess.peerInfoHashTable.get(receiving_peer).state = 9;
				} else {
					not_interested_send(peerProcess.pIDToSocketMap.get(receiving_peer), receiving_peer);
					peerProcess.peerInfoHashTable.get(receiving_peer).state = 13;
				}
			} else {

				switch (state) {

				case 2: // bitfield msg
					if (msgType.equals(data_bitfield_msg)) {
						peerProcess.printLogs(
								peerProcess.pID + " receieved a BITFIELD message from Peer " + receiving_peer);
						bitfield_send(peerProcess.pIDToSocketMap.get(receiving_peer), receiving_peer);
						peerProcess.peerInfoHashTable.get(receiving_peer).state = 3;
					}
					break;

				case 3: // not interested msg or if interested msg then see for choking
					if (msgType.equals(data_notinterested_msg)) {

						peerProcess.printLogs(
								peerProcess.pID + " receieved a NOT INTERESTED message from Peer " + receiving_peer);
						peerProcess.peerInfoHashTable.get(receiving_peer).is_interested = 0;
						peerProcess.peerInfoHashTable.get(receiving_peer).state = 5;
						peerProcess.peerInfoHashTable.get(receiving_peer).handshake_done = 1;
					} else if (msgType.equals(data_interested_msg)) {
						peerProcess.printLogs(
								peerProcess.pID + " receieved an INTERESTED message from Peer " + receiving_peer);
						peerProcess.peerInfoHashTable.get(receiving_peer).is_interested = 1;
						peerProcess.peerInfoHashTable.get(receiving_peer).handshake_done = 1;

						if (!peerProcess.prefNeighbors.containsKey(receiving_peer)
								&& !peerProcess.unchoke_neighbor.containsKey(receiving_peer)) {
							choke_send(peerProcess.pIDToSocketMap.get(receiving_peer), receiving_peer);
							peerProcess.peerInfoHashTable.get(receiving_peer).state = 6;
						} else {
							peerProcess.peerInfoHashTable.get(receiving_peer).is_choked = 0;
							send_unchoke(peerProcess.pIDToSocketMap.get(receiving_peer), receiving_peer);
							peerProcess.peerInfoHashTable.get(receiving_peer).state = 4;
						}
					}
					break;

				case 4: // request msg
					if (msgType.equals(data_request_msg)) {
						peice_send(peerProcess.pIDToSocketMap.get(receiving_peer), dm, receiving_peer);
						if (!peerProcess.prefNeighbors.containsKey(receiving_peer)
								&& !peerProcess.unchoke_neighbor.containsKey(receiving_peer)) {
							choke_send(peerProcess.pIDToSocketMap.get(receiving_peer), receiving_peer);
							peerProcess.peerInfoHashTable.get(receiving_peer).is_choked = 1;
							peerProcess.peerInfoHashTable.get(receiving_peer).state = 6;
						}
					}
					break;

				case 8: // compare bitfield and check for interested or not
					if (msgType.equals(data_bitfield_msg)) {

						if (is_interested(dm, receiving_peer)) {

							interested_send(peerProcess.pIDToSocketMap.get(receiving_peer), receiving_peer);
							peerProcess.peerInfoHashTable.get(receiving_peer).state = 9;
						} else {
							not_interested_send(peerProcess.pIDToSocketMap.get(receiving_peer), receiving_peer);
							peerProcess.peerInfoHashTable.get(receiving_peer).state = 13;
						}
					}
					break;

				case 9: // Checks for choking and unchoking
					if (msgType.equals(data_choke_msg)) {
						peerProcess.printLogs(peerProcess.pID + " is CHOKED by Peer " + receiving_peer);
						peerProcess.peerInfoHashTable.get(receiving_peer).state = 14;
					} else if (msgType.equals(data_unchoke_msg)) {
						peerProcess.printLogs(peerProcess.pID + " is UNCHOKED by Peer " + receiving_peer);
						int firstdiff = peerProcess.ownBitField
								.Difference(peerProcess.peerInfoHashTable.get(receiving_peer).bitField);
						if (firstdiff != -1) {
							request_send(peerProcess.pIDToSocketMap.get(receiving_peer), firstdiff, receiving_peer);
							peerProcess.peerInfoHashTable.get(receiving_peer).state = 11;
							peerProcess.peerInfoHashTable.get(receiving_peer).startTime = new Date();
						} else
							peerProcess.peerInfoHashTable.get(receiving_peer).state = 13;
					}
					break;

				case 11: // checks if data message is same as the message type
					if (msgType.equals(data_piece_msg)) {
						byte[] buffer = dm.payload_get();
						peerProcess.peerInfoHashTable.get(receiving_peer).finishTime = new Date();
						long timeLapse = peerProcess.peerInfoHashTable.get(receiving_peer).finishTime.getTime()
								- peerProcess.peerInfoHashTable.get(receiving_peer).startTime.getTime();

						peerProcess.peerInfoHashTable.get(
								receiving_peer).dataRate = ((double) (buffer.length + data_lenOf_msg + data_type_msg)
										/ (double) timeLapse) * 100;

						Piece p = Piece.piece_decoder(buffer);
						peerProcess.ownBitField.bitfield_update(receiving_peer, p);

						int toGetPeiceIndex = peerProcess.ownBitField
								.Difference(peerProcess.peerInfoHashTable.get(receiving_peer).bitField);
						if (toGetPeiceIndex != -1) {
							request_send(peerProcess.pIDToSocketMap.get(receiving_peer), toGetPeiceIndex,
									receiving_peer);
							peerProcess.peerInfoHashTable.get(receiving_peer).state = 11;
							peerProcess.peerInfoHashTable.get(receiving_peer).startTime = new Date();
						} else
							peerProcess.peerInfoHashTable.get(receiving_peer).state = 13;
						peerProcess.getPeerInformationAgain();

						Enumeration<String> keys = peerProcess.peerInfoHashTable.keys();
						while (keys.hasMoreElements()) {
							String key = (String) keys.nextElement();
							RemotePeerInfo pref = peerProcess.peerInfoHashTable.get(key);

							if (key.equals(peerProcess.pID))
								continue;
							if (pref.is_completed == 0 && pref.is_choked == 0 && pref.handshake_done == 1) {
								send_have(peerProcess.pIDToSocketMap.get(key), key);
								peerProcess.peerInfoHashTable.get(key).state = 3;

							}

						}

						buffer = null;
						dm = null;

					} else if (msgType.equals(data_choke_msg)) {

						peerProcess.printLogs(peerProcess.pID + " is CHOKED by Peer " + receiving_peer);
						peerProcess.peerInfoHashTable.get(receiving_peer).state = 14;
					}
					break;

				case 14: // checks for have-type data message
					if (msgType.equals(data_have_msg)) {

						if (is_interested(dm, receiving_peer)) {
							interested_send(peerProcess.pIDToSocketMap.get(receiving_peer), receiving_peer);
							peerProcess.peerInfoHashTable.get(receiving_peer).state = 9;
						} else {
							not_interested_send(peerProcess.pIDToSocketMap.get(receiving_peer), receiving_peer);
							peerProcess.peerInfoHashTable.get(receiving_peer).state = 13;
						}
					} else if (msgType.equals(data_unchoke_msg)) {
						peerProcess.printLogs(peerProcess.pID + " is UNCHOKED by Peer " + receiving_peer);
						peerProcess.peerInfoHashTable.get(receiving_peer).state = 14;
					}
					break;

				}
			}

		}
	}

	// Converting integer to byte
	private byte[] intTobyte(int b) {
		return new byte[] { (byte) ((b >> 24) & 0xFF), (byte) ((b >> 16) & 0xFF), (byte) ((b >> 8) & 0xFF),
				(byte) (b & 0xFF) };
	}

	// Converting byte to integer
	private int byteToint(byte[] b1) {
		return b1[3] & 0xFF | (b1[2] & 0xFF) << 8 | (b1[1] & 0xFF) << 16 | (b1[0] & 0xFF) << 24;
	}

	// Choke Message sender
	private void choke_send(Socket socket, String pID_remote) {
		peerProcess.printLogs(peerProcess.pID + " sending CHOKE message to Peer " + pID_remote);
		Actual_msg dm = new Actual_msg(data_choke_msg);
		byte[] msgByte = Actual_msg.encode_msg(dm);
		data_send(socket, msgByte);
	}

	// Unchoke Message Sender
	private void send_unchoke(Socket socket, String pID_remote) {
		peerProcess.printLogs(peerProcess.pID + " sending UNCHOKE message to Peer " + pID_remote);
		Actual_msg dm = new Actual_msg(data_unchoke_msg);
		byte[] msgByte = Actual_msg.encode_msg(dm);
		data_send(socket, msgByte);
	}

	// Interested Message sender
	private void interested_send(Socket socket, String pID_remote) {
		peerProcess.printLogs(peerProcess.pID + " sending an INTERESTED message to Peer " + pID_remote);
		Actual_msg dm = new Actual_msg(data_interested_msg);
		byte[] msgByte = Actual_msg.encode_msg(dm);
		data_send(socket, msgByte);
	}

	// returns true after comparision of bitfield if there is any extra data
	private boolean is_interested(Actual_msg dm, String receiving_peer) {
		BitField b = BitField.decode(dm.payload_get());
		peerProcess.peerInfoHashTable.get(receiving_peer).bitField = b;
		if (peerProcess.ownBitField.compare(b))
			return true;
		return false;
	}

	// Not-Interested message sender
	private void not_interested_send(Socket socket, String pID_remote) {
		peerProcess.printLogs(peerProcess.pID + " sending a NOT INTERESTED message to Peer " + pID_remote);
		Actual_msg dm = new Actual_msg(data_notinterested_msg);
		byte[] msgByte = Actual_msg.encode_msg(dm);
		data_send(socket, msgByte);
	}

	// Have Message Sender
	private void send_have(Socket socket, String remote_pID) {
		peerProcess.printLogs(peerProcess.pID + " sending HAVE message to Peer " + remote_pID);
		byte[] encoded_bitField = peerProcess.ownBitField.encode();
		Actual_msg dm = new Actual_msg(data_have_msg, encoded_bitField);
		data_send(socket, Actual_msg.encode_msg(dm));
		encoded_bitField = null;
	}

	// Bitfield Message sender
	private void bitfield_send(Socket socket, String pID_remote) {
		peerProcess.printLogs(peerProcess.pID + " sending BITFIELD message to Peer " + pID_remote);
		byte[] encodedBitField = peerProcess.ownBitField.encode();
		Actual_msg dm = new Actual_msg(data_bitfield_msg, encodedBitField);
		data_send(socket, Actual_msg.encode_msg(dm));
		encodedBitField = null;
	}

	// Datasend;;
	private int data_send(Socket socket, byte[] encoded_bitField) {
		try {
			OutputStream output = socket.getOutputStream();
			output.write(encoded_bitField);
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
		return 1;
	}

	// Request message sender
	private void request_send(Socket socket, int pieceNo, String pID_remote) {
		byte[] piece_byte = new byte[RequiredConstants.len_piece_index];
		for (int i = 0; i < RequiredConstants.len_piece_index; i++) { // Converting byte to int
			piece_byte[i] = 0;
		}
		byte[] pieceIndexByte = Actual_msg_helper.ArrayIntToByte(pieceNo);
		System.arraycopy(pieceIndexByte, 0, piece_byte, 0, pieceIndexByte.length);
		Actual_msg dm = new Actual_msg(data_request_msg, piece_byte);
		byte[] b = Actual_msg.encode_msg(dm);
		data_send(socket, b);
		piece_byte = null;
		pieceIndexByte = null;
		b = null;
		dm = null;
	}

	private void peice_send(Socket socket, Actual_msg dm, String pID_remote) // Actual_msg is basically the request
																				// message
	{
		byte[] bytePieceIndex = dm.payload_get();
		int pieceIndex = Actual_msg_helper.ArrayByteToInt(bytePieceIndex);

		peerProcess.printLogs(
				peerProcess.pID + " sending a PIECE message for piece " + pieceIndex + " to Peer " + pID_remote);

		byte[] byteRead = new byte[Common_cfg_clone.pieceSize];
		int noBytesRead = 0;

		File file = new File(peerProcess.pID, Common_cfg_clone.fileName);
		try {
			rand_file = new RandomAccessFile(file, "r");
			rand_file.seek(pieceIndex * Common_cfg_clone.pieceSize);
			noBytesRead = rand_file.read(byteRead, 0, Common_cfg_clone.pieceSize);
		} catch (IOException e) {
			peerProcess.printLogs(peerProcess.pID + " ERROR in reading the file : " + e.toString());
		}
		if (noBytesRead == 0) {
			peerProcess.printLogs(peerProcess.pID + " ERROR :  Zero bytes read from the file !");
		} else if (noBytesRead < 0) {
			peerProcess.printLogs(peerProcess.pID + " ERROR : File could not be read properly.");
		}

		byte[] buffer = new byte[noBytesRead + RequiredConstants.len_piece_index];
		System.arraycopy(bytePieceIndex, 0, buffer, 0, RequiredConstants.len_piece_index);
		System.arraycopy(byteRead, 0, buffer, RequiredConstants.len_piece_index, noBytesRead);

		Actual_msg sendMessage = new Actual_msg(data_piece_msg, buffer);
		byte[] b = Actual_msg.encode_msg(sendMessage);
		data_send(socket, b);

		byteRead = null;
		b = null;
		bytePieceIndex = null;
		sendMessage = null;

		try {
			rand_file.close();
		} catch (Exception e) {
		}
	}

}