
public class Actual_msg_helper {
	Actual_msg dataMsg;
	String pID_from;

	public Actual_msg_helper() {
		dataMsg = new Actual_msg();
		pID_from = null;
	}

	public void pID_from_set(String pID_from) {
		this.pID_from = pID_from;
	}

	public void data_msgSet(Actual_msg dataMsg) {
		this.dataMsg = dataMsg;
	}

	public Actual_msg data_msg_get() {
		return dataMsg;
	}

	public String from_pIDGet() {
		return pID_from;
	}

	public static byte[] ArrayIntToByte(int value) {
		byte[] bArray = new byte[4];
		for (int i = 0; i < 4; i++) {
			int offset = (bArray.length - 1 - i) * 8;
			bArray[i] = (byte) ((value >>> offset) & 0xFF);
		}
		return bArray;
	}

	// ArrayByteToInt
	public static int ArrayByteToInt(byte[] b) {
		return ArrayByteToInt(b, 0);
	}

	// ArrayByteToInt
	public static int ArrayByteToInt(byte[] b, int offset) {
		int value = 0;
		int range = 4;
		for (int i = 0; i < range; i++) {
			int shift = (range - 1 - i) * 8;
			value += (b[i + offset] & 0x000000FF) << shift;
		}
		return value;
	}

	// ArrayByteToHexString
	static String ArrayByteToHexString(byte in[]) {
		byte chr = 0x00;

		int i = 0;

		if (in == null || in.length <= 0)
			return null;
		String hexDigits[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };

		StringBuffer out = new StringBuffer(in.length * 2);

		for (i = 0; i < in.length; i++) {
			chr = (byte) (in[i] & 0xF0);
			chr = (byte) (chr >>> 4);
			chr = (byte) (chr & 0x0F);
			out.append(hexDigits[(int) chr]);
			chr = (byte) (in[i] & 0x0F);
			out.append(hexDigits[(int) chr]);
		}

		String result = new String(out);

		return result;
	}

}
