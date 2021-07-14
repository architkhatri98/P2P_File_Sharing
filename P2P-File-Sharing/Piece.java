public class Piece {
	public int is_present;
	public String pID_from;
	public byte[] filePiece;
	public int pieceIndex;

	public int is_presentGet() {
		return is_present;
	}

	public void is_presentSet(int is_present) {
		this.is_present = is_present;
	}

	public String from_pIDGet() {
		return pID_from;
	}

	public void pID_from_set(String pID_from) {
		this.pID_from = pID_from;
	}

	public Piece() {
		filePiece = new byte[Common_cfg_clone.pieceSize];
		pieceIndex = -1;
		is_present = 0;
		pID_from = null;
	}

	public static Piece piece_decoder(byte[] pay_load) {
		byte[] byteIndex = new byte[RequiredConstants.len_piece_index];
		Piece piece = new Piece();
		System.arraycopy(pay_load, 0, byteIndex, 0, RequiredConstants.len_piece_index);
		piece.pieceIndex = Actual_msg_helper.ArrayByteToInt(byteIndex);
		piece.filePiece = new byte[pay_load.length - RequiredConstants.len_piece_index];
		System.arraycopy(pay_load, RequiredConstants.len_piece_index, piece.filePiece, 0,
				pay_load.length - RequiredConstants.len_piece_index);
		return piece;
	}
}
