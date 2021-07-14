
import java.io.UnsupportedEncodingException;

public class Actual_msg implements RequiredConstants {
	private String msg_type;
	private String msg_length;
	private int data_len = data_type_msg;
	private byte[] type = null;
	private byte[] len = null;
	private byte[] pay_load = null;

	public Actual_msg(String Type) {

		try {

			if (Type == data_choke_msg || Type == data_unchoke_msg || Type == data_interested_msg
					|| Type == data_notinterested_msg) {
				this.set_msg_len(1);
				this.set_msg_type(Type);
				this.pay_load = null;
			} else
				throw new Exception("Incorrect Constructor call");

		} catch (Exception e) {
			peerProcess.printLogs(e.toString());
		}
	}

	public Actual_msg(String Type, byte[] pay_load) {

		try {
			if (pay_load != null) {

				this.set_msg_len(pay_load.length + 1);
				if (this.len.length > data_lenOf_msg)
					throw new Exception("Message too big");

				this.payload_set(pay_load);

			} else {
				if (Type == data_choke_msg || Type == data_unchoke_msg || Type == data_interested_msg
						|| Type == data_notinterested_msg) {
					this.set_msg_len(1);
					this.pay_load = null;
				} else
					throw new Exception("Payload is null");

			}

			this.set_msg_type(Type);
			if (this.get_msg_type().length > data_type_msg)
				throw new Exception("Type is too long");

		} catch (Exception e) {
			peerProcess.printLogs(e.toString());
		}

	}

	public Actual_msg() {

	}

	public void set_msg_len(int msg_length) {
		this.data_len = msg_length;
		this.msg_length = ((Integer) msg_length).toString();
		this.len = Actual_msg_helper.ArrayIntToByte(msg_length);
	}

	public void set_msg_len(byte[] len) {

		Integer l = Actual_msg_helper.ArrayByteToInt(len);
		this.msg_length = l.toString();
		this.len = len;
		this.data_len = l;
	}

	public byte[] get_msg_len() {
		return len;
	}

	public String get_msg_lenString() {
		return msg_length;
	}

	public int get_msg_lenInt() {
		return this.data_len;
	}

	public void set_msg_type(byte[] type) {
		try {
			this.msg_type = new String(type, MSG_CHARSET_NAME);
			this.type = type;
		} catch (UnsupportedEncodingException e) {
			peerProcess.printLogs(e.toString());
		}
	}

	public void set_msg_type(String msg_type) {
		try {
			this.msg_type = msg_type.trim();
			this.type = this.msg_type.getBytes(MSG_CHARSET_NAME);
		} catch (UnsupportedEncodingException e) {
			peerProcess.printLogs(e.toString());
		}
	}

	public byte[] get_msg_type() {
		return type;
	}

	public void payload_set(byte[] pay_load) {
		this.pay_load = pay_load;
	}

	public byte[] payload_get() {
		return pay_load;
	}

	public String get_msgString() {
		return msg_type;
	}

	public String toString() {
		String str = null;
		try {
			str = "[Actual_msg] : Message Length - " + this.msg_length + ", Message Type - " + this.msg_type
					+ ", Data - " + (new String(this.pay_load, MSG_CHARSET_NAME)).toString().trim();
		} catch (UnsupportedEncodingException e) {
			peerProcess.printLogs(e.toString());
		}
		return str;
	}
	// encodes the object Actual_msg to a byte array

	public static byte[] encode_msg(Actual_msg msg) {
		byte[] msgStream = null;
		int msgType;

		try {

			msgType = Integer.parseInt(msg.get_msgString());
			if (msg.get_msg_len().length > data_lenOf_msg)
				throw new Exception("Invalid message length.");
			else if (msgType < 0 || msgType > 7)
				throw new Exception("Invalid message type.");
			else if (msg.get_msg_type() == null)
				throw new Exception("Invalid message type.");
			else if (msg.get_msg_len() == null)
				throw new Exception("Invalid message length.");

			if (msg.payload_get() != null) {
				msgStream = new byte[data_lenOf_msg + data_type_msg + msg.payload_get().length];

				System.arraycopy(msg.get_msg_len(), 0, msgStream, 0, msg.get_msg_len().length);
				System.arraycopy(msg.get_msg_type(), 0, msgStream, data_lenOf_msg, data_type_msg);
				System.arraycopy(msg.payload_get(), 0, msgStream, data_lenOf_msg + data_type_msg,
						msg.payload_get().length);

			} else {
				msgStream = new byte[data_lenOf_msg + data_type_msg];

				System.arraycopy(msg.get_msg_len(), 0, msgStream, 0, msg.get_msg_len().length);
				System.arraycopy(msg.get_msg_type(), 0, msgStream, data_lenOf_msg, data_type_msg);

			}

		} catch (Exception e) {
			peerProcess.printLogs(e.toString());
			msgStream = null;
		}

		return msgStream;
	}

	// decodes the byte array and send it to object Actual_msg

	public static Actual_msg decode_msg(byte[] Message) {

		Actual_msg msg = new Actual_msg();
		byte[] msgLength = new byte[data_lenOf_msg];
		byte[] msgType = new byte[data_type_msg];
		byte[] pay_load = null;
		int len;

		try {

			if (Message == null)
				throw new Exception("Invalid data.");
			else if (Message.length < data_lenOf_msg + data_type_msg)
				throw new Exception("Byte array length is too small...");

			System.arraycopy(Message, 0, msgLength, 0, data_lenOf_msg);
			System.arraycopy(Message, data_lenOf_msg, msgType, 0, data_type_msg);

			msg.set_msg_len(msgLength);
			msg.set_msg_type(msgType);

			len = Actual_msg_helper.ArrayByteToInt(msgLength);

			if (len > 1) {
				pay_load = new byte[len - 1];
				System.arraycopy(Message, data_lenOf_msg + data_type_msg, pay_load, 0,
						Message.length - data_lenOf_msg - data_type_msg);
				msg.payload_set(pay_load);
			}

			pay_load = null;
		} catch (Exception e) {
			peerProcess.printLogs(e.toString());
			msg = null;
		}
		return msg;
	}

}
