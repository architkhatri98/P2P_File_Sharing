import java.io.*;

public class handshake_msg implements RequiredConstants {
	// Attributes
	private byte[] header = new byte[handshake_header_len];
	private String msgheader;
	private String msgPID;
	private byte[] pID = new byte[HANDSHAKE_pID_LEN];
	private byte[] zeroBits = new byte[HANDSHAKE_ZEROBITS_LEN];

	public handshake_msg() {

	}

	public handshake_msg(String header, String pID) {

		try {
			this.msgheader = header;
			this.header = header.getBytes(MSG_CHARSET_NAME);
			if (this.header.length > handshake_header_len)
				throw new Exception("header size more than expected!");

			this.msgPID = pID;
			this.pID = pID.getBytes(MSG_CHARSET_NAME);
			if (this.pID.length > handshake_header_len)
				throw new Exception("Peer ID size longer than expected");

			this.zeroBits = "0000000000".getBytes(MSG_CHARSET_NAME);
		} catch (Exception e) {
			peerProcess.printLogs(e.toString());
		}

	}

	public void header_set(byte[] handShakeheader) {
		try {
			this.msgheader = (new String(handShakeheader, MSG_CHARSET_NAME)).toString().trim();
			this.header = this.msgheader.getBytes();
		} catch (UnsupportedEncodingException e) {
			peerProcess.printLogs(e.toString());
		}
	}

	public void set_pid(byte[] pID) {
		try {
			this.msgPID = (new String(pID, MSG_CHARSET_NAME)).toString().trim();
			this.pID = this.msgPID.getBytes();

		} catch (UnsupportedEncodingException e) {
			peerProcess.printLogs(e.toString());
		}
	}

	public void set_pid(String msgPID) {
		try {
			this.msgPID = msgPID;
			this.pID = msgPID.getBytes(MSG_CHARSET_NAME);
		} catch (UnsupportedEncodingException e) {
			peerProcess.printLogs(e.toString());
		}
	}

	public void header_set(String msgheader) {
		try {
			this.msgheader = msgheader;
			this.header = msgheader.getBytes(MSG_CHARSET_NAME);
		} catch (UnsupportedEncodingException e) {
			peerProcess.printLogs(e.toString());
		}
	}

	public byte[] header_get() {
		return header;
	}

	public byte[] get_pid() {
		return pID;
	}

	public void make_zeroBits(byte[] zeroBits) {
		this.zeroBits = zeroBits;
	}

	public byte[] zero_bits_get() {
		return zeroBits;
	}

	public String header_getString() {
		return msgheader;
	}

	public String get_pidString() {
		return msgPID;
	}

	public String toString() {
		return ("[handshake_msg] : Peer Id - " + this.msgPID + ", header - " + this.msgheader);
	}

	public static handshake_msg decode_msg(byte[] receivedMessage) {

		handshake_msg handshake_msg = null;
		byte[] msgheader = null;
		byte[] msgpID = null;

		try {
			if (receivedMessage.length != handshake_msg_len)
				throw new Exception("Byte length array size doesn't match with the handshake length");

			handshake_msg = new handshake_msg();
			msgheader = new byte[handshake_header_len];
			msgpID = new byte[HANDSHAKE_pID_LEN];
			System.arraycopy(receivedMessage, 0, msgheader, 0, handshake_header_len);
			System.arraycopy(receivedMessage, handshake_header_len + HANDSHAKE_ZEROBITS_LEN, msgpID, 0,
					HANDSHAKE_pID_LEN);
			handshake_msg.header_set(msgheader);
			handshake_msg.set_pid(msgpID);

		} catch (Exception e) {
			peerProcess.printLogs(e.toString());
			handshake_msg = null;
		}
		return handshake_msg;
	}

	public static byte[] encode_msg(handshake_msg handshake_msg) {

		byte[] sendMessage = new byte[handshake_msg_len];

		try {
			if (handshake_msg.header_get() == null) {
				throw new Exception("Invalid header.");
			}
			if (handshake_msg.header_get().length > handshake_header_len || handshake_msg.header_get().length == 0) {
				throw new Exception("Invalid header.");
			} else {
				System.arraycopy(handshake_msg.header_get(), 0, sendMessage, 0, handshake_msg.header_get().length);
			}

			if (handshake_msg.zero_bits_get() == null) {
				throw new Exception("Invalid zero bits field.");
			}
			if (handshake_msg.zero_bits_get().length > HANDSHAKE_ZEROBITS_LEN
					|| handshake_msg.zero_bits_get().length == 0) {
				throw new Exception("Invalid zero bits field.");
			} else {
				System.arraycopy(handshake_msg.zero_bits_get(), 0, sendMessage, handshake_header_len,
						HANDSHAKE_ZEROBITS_LEN - 1);
			}
			
			if (handshake_msg.get_pid() == null) {
				throw new Exception("Invalid peer id.");
			} else if (handshake_msg.get_pid().length > HANDSHAKE_pID_LEN || handshake_msg.get_pid().length == 0) {
				throw new Exception("Invalid peer id.");
			} else {
				System.arraycopy(handshake_msg.get_pid(), 0, sendMessage, handshake_header_len + HANDSHAKE_ZEROBITS_LEN,
						handshake_msg.get_pid().length);
			}

		} catch (Exception e) {
			peerProcess.printLogs(e.toString());
			sendMessage = null;
		}

		return sendMessage;
	}
}
