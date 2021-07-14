import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;

public class BitField implements RequiredConstants {
	public Piece[] pieces; // global variable
	public int size; // global variable

	// Constructor
	public BitField() {
		size = (int) Math.ceil(((double) Common_cfg_clone.fileSize / (double) Common_cfg_clone.pieceSize));
		this.pieces = new Piece[size];
		for (int i = 0; i < this.size; i++)
			this.pieces[i] = new Piece();
	}

	// Returns the size of pieces array
	public int fetch_size() {
		return size;
	}

	// Returns all pieces.
	public Piece[] fetch_pieces() {
		return pieces;
	}

	// set the size
	public void size_set(int size) {
		this.size = size;
	}

	// sets the pieces
	public void pieces_set(Piece[] pieces) {
		this.pieces = pieces;
	}

	// returns byteArray
	public byte[] fetch_bytes() {
		int sz = this.size / 8;
		if (size % 8 != 0)
			sz = sz + 1;
		byte[] iP = new byte[sz];
		int tempInt = 0;
		int count = 0;
		int i;
		for (i = 1; i <= this.size; i++) {
			int tempP = this.pieces[i - 1].is_present;
			tempInt = tempInt << 1;
			if (tempP == 1) {
				tempInt = tempInt + 1;
			} else
				tempInt = tempInt + 0;

			if (i % 8 == 0 && i != 0) {
				iP[count] = (byte) tempInt;
				count++;
				tempInt = 0;
			}

		}
		if ((i - 1) % 8 != 0) {
			int tempShift = ((size) - (size / 8) * 8);
			tempInt = tempInt << (8 - tempShift);
			iP[count] = (byte) tempInt;
		}
		return iP;
	}

	public byte[] encode() {
		return this.fetch_bytes();
	}

	public static BitField decode(byte[] b) {
		BitField bf = new BitField();
		for (int i = 0; i < b.length; i++) {
			int count = 7;
			while (count >= 0) {
				int test = 1 << count;
				if (i * 8 + (8 - count - 1) < bf.size) {
					if ((b[i] & (test)) != 0)
						bf.pieces[i * 8 + (8 - count - 1)].is_present = 1;
					else
						bf.pieces[i * 8 + (8 - count - 1)].is_present = 0;
				}
				count--;
			}
		}

		return bf;
	}

	// Comparing own bitfield with other
	public synchronized boolean compare(BitField bitfield_2) {
		int size_2 = bitfield_2.fetch_size();

		for (int i = 0; i < size_2; i++) {
			if (bitfield_2.fetch_pieces()[i].is_presentGet() == 1 && this.fetch_pieces()[i].is_presentGet() == 0) {
				return true;
			} else
				continue;
		}

		return false;
	}

	// this functions is used to find first bit number one peer has but the other
	// has not.
	public synchronized int Difference(BitField bitfield_2) {
		int size_1 = this.fetch_size();
		int size_2 = bitfield_2.fetch_size();

		if (size_1 >= size_2) {
			for (int i = 0; i < size_2; i++) {
				if (bitfield_2.fetch_pieces()[i].is_presentGet() == 1 && this.fetch_pieces()[i].is_presentGet() == 0) {
					return i;
				}
			}
		} else {
			for (int i = 0; i < size_1; i++) {
				if (bitfield_2.fetch_pieces()[i].is_presentGet() == 1 && this.fetch_pieces()[i].is_presentGet() == 0) {
					return i;
				}
			}
		}

		return -1;
	}

	// converts the byte array to a string(hexadecimal)
	static String Array_byte_to_hex(byte in[]) {
		byte ch = 0x00;

		int i = 0;

		if (in == null || in.length <= 0)
			return null;
		String hex[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };

		StringBuffer out = new StringBuffer(in.length * 2);

		while (i < in.length) {
			ch = (byte) (in[i] & 0xF0);
			ch = (byte) (ch >>> 4);
			ch = (byte) (ch & 0x0F);
			out.append(hex[(int) ch]);
			ch = (byte) (in[i] & 0x0F);
			out.append(hex[(int) ch]);
			i++;
		}

		String rslt = new String(out);

		return rslt;
	}

	public void initialize_bitfield(String peer_id_own, int hasFile) {

		if (hasFile != 1) {// represents that file is not there.
			for (int i = 0; i < this.size; i++) {
				this.pieces[i].is_presentSet(0);
				this.pieces[i].pID_from_set(peer_id_own);
			}

		} else {// File exists here.
			for (int i = 0; i < this.size; i++) {
				this.pieces[i].is_presentSet(1);
				this.pieces[i].pID_from_set(peer_id_own);
			}

		}

	}

	// Updating Bitfield
	// Checks if new pieces are received and then updates in the bitfield section of
	// the peerID.
	public synchronized void bitfield_update(String pID, Piece piece) {
		try {
			if (peerProcess.ownBitField.pieces[piece.pieceIndex].is_present == 1) {
				peerProcess.printLogs(pID + " Piece already received!!");
			} else {
				String fileName = Common_cfg_clone.fileName;
				File file = new File(peerProcess.pID, fileName);
				int off = piece.pieceIndex * Common_cfg_clone.pieceSize;
				RandomAccessFile rand_file = new RandomAccessFile(file, "rw");
				byte[] byteWrite;
				byteWrite = piece.filePiece;

				rand_file.seek(off);
				rand_file.write(byteWrite);

				this.pieces[piece.pieceIndex].is_presentSet(1);
				this.pieces[piece.pieceIndex].pID_from_set(pID);
				rand_file.close();

				peerProcess.printLogs(peerProcess.pID + " has downloaded the PIECE " + piece.pieceIndex + " from Peer "
						+ pID + ". Now the number of pieces it has is " + peerProcess.ownBitField.pieces_of_own());

				if (peerProcess.ownBitField.is_completed()) {
					peerProcess.peerInfoHashTable.get(peerProcess.pID).is_interested = 0;
					peerProcess.peerInfoHashTable.get(peerProcess.pID).is_completed = 1;
					peerProcess.peerInfoHashTable.get(peerProcess.pID).is_choked = 0;
					peerinfo_update(peerProcess.pID, 1);

					peerProcess.printLogs(peerProcess.pID + " has DOWNLOADED the complete file.");
				}
			}

		} catch (Exception e) {
			peerProcess.printLogs(peerProcess.pID + " EROR in updating bitfield " + e.getMessage());
		}

	}

	public int pieces_of_own() {
		int count = 0;
		for (int i = 0; i < this.size; i++)
			if (this.pieces[i].is_present == 1)
				count++;

		return count;
	}

	public boolean is_completed() {

		for (int i = 0; i < this.size; i++) {
			if (this.pieces[i].is_present == 0) {
				return false;
			}
		}
		return true;
	}

	// Updating the PeerInfo.cfg file
	// If download is completed by a peer,then in peerinfo file the corresponding
	// field of hasFile of the same peer is changed from 0 to 1.
	public void peerinfo_update(String clientID, int hasFile) {
		BufferedWriter out = null;
		BufferedReader in = null;

		try {
			in = new BufferedReader(new FileReader("PeerInfo.cfg"));

			String line;
			StringBuffer buffer = new StringBuffer();

			while ((line = in.readLine()) != null) {
				if (line.trim().split("\\s+")[0].equals(clientID)) {
					buffer.append(line.trim().split("\\s+")[0] + " " + line.trim().split("\\s+")[1] + " "
							+ line.trim().split("\\s+")[2] + " " + hasFile);
				} else {
					buffer.append(line);

				}
				buffer.append("\n");
			}

			in.close();

			out = new BufferedWriter(new FileWriter("PeerInfo.cfg"));
			out.write(buffer.toString());

			out.close();
		} catch (Exception e) {
			peerProcess.printLogs(clientID + " Error in updating the PeerInfo.cfg " + e.getMessage());
		}
	}

}
