public interface RequiredConstants {

	public static final String MSG_CHARSET_NAME = "UTF8"; // Encoding

	public static final String header_handshake = "P2PFILESHARINGPROJ"; // given handshake header name

	public static final int handshake_msg_len = 32; // Length of handshake message

	public static final int handshake_header_len = 18; // Length of handshake header

	public static final int HANDSHAKE_ZEROBITS_LEN = 10; // 10-byte zero bits

	public static final int HANDSHAKE_pID_LEN = 4; // 4-byte peerID

	public static final int len_piece_index = 4; // 4-byte piece index

	public static final int data_lenOf_msg = 4; // Actual message 4-byte message length

	public static final int data_type_msg = 1; // 1-byte message type field

	public static final String data_choke_msg = "0";

	public static final String data_unchoke_msg = "1";

	public static final String data_interested_msg = "2";

	public static final String data_notinterested_msg = "3";

	public static final String data_have_msg = "4";

	public static final String data_bitfield_msg = "5";

	public static final String data_request_msg = "6";

	public static final String data_piece_msg = "7";

}
