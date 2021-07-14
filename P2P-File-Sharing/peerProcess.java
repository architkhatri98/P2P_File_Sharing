import java.io.*;
import java.net.*;
import java.util.*;

public class peerProcess implements RequiredConstants {
	public ServerSocket socketListener = null;
	public int listenerPort;
	public String ipPeer = null;
	public static String pID;
	public int p_index;
	public Thread listener; 
	public static boolean finishedBool = false;
	public static BitField ownBitField = null;
	public static volatile Timer preferredTimer;
	public static volatile Timer unchokeTimer;
	public static Vector<Thread> receiverThread = new Vector<Thread>();
	public static Vector<Thread> thread_send = new Vector<Thread>();
	public static volatile Hashtable<String, RemotePeerInfo> peerInfoHashTable = new Hashtable<String, RemotePeerInfo>();
	public static volatile Hashtable<String, RemotePeerInfo> prefNeighbors = new Hashtable<String, RemotePeerInfo>();
	public static volatile Hashtable<String, RemotePeerInfo> unchoke_neighbor = new Hashtable<String, RemotePeerInfo>();
	public static volatile Queue<Actual_msg_helper> msgQueue = new LinkedList<Actual_msg_helper>();
	public static Hashtable<String, Socket> pIDToSocketMap = new Hashtable<String, Socket>();

	public static Thread Msg_handler;

	public static synchronized void addMsgToQueue(Actual_msg_helper msg) {
		msgQueue.add(msg);
	}

	public static synchronized Actual_msg_helper removeMsgQueue() {
		Actual_msg_helper msg = null;
		if (!msgQueue.isEmpty()) {
			msg = msgQueue.remove();
		}

		return msg;
	}

	public static void getPeerInformationAgain() {
		try {
			String st;
			BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
			while ((st = in.readLine()) != null) {
				String[] args = st.trim().split("\\s+");
				String pID = args[0];
				int is_completed = Integer.parseInt(args[3]);
				if (is_completed == 1) {
					peerInfoHashTable.get(pID).is_completed = 1;
					peerInfoHashTable.get(pID).is_interested = 0;
					peerInfoHashTable.get(pID).is_choked = 0;
				}
			}
			in.close();
		} catch (Exception e) {
			printLogs(pID + e.toString());
		}
	}

	public static class prefNeighbors extends TimerTask {
		public void run() {
			getPeerInformationAgain();
			Enumeration<String> keys = peerInfoHashTable.keys();
			int countInterested = 0;
			String strPref = "";
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				RemotePeerInfo pref = peerInfoHashTable.get(key);
				if (key.equals(pID))
					continue;
				if (pref.is_completed == 0 && pref.handshake_done == 1) {
					countInterested++;
				} else if (pref.is_completed == 1) {
					try {
						prefNeighbors.remove(key);
					} catch (Exception e) {
					}
				}
			}
			if (countInterested > Common_cfg_clone.numOfPreferredNeighbr) {
				boolean flag = prefNeighbors.isEmpty();
				if (!flag)
					prefNeighbors.clear();
				List<RemotePeerInfo> pv = new ArrayList<RemotePeerInfo>(peerInfoHashTable.values());
				Collections.sort(pv, new Comparision_of_peerRate(false));
				int count = 0;
				for (int i = 0; i < pv.size(); i++) {
					if (count > Common_cfg_clone.numOfPreferredNeighbr - 1)
						break;
					if (pv.get(i).handshake_done == 1 && !pv.get(i).pID.equals(pID)
							&& peerInfoHashTable.get(pv.get(i).pID).is_completed == 0) {
						peerInfoHashTable.get(pv.get(i).pID).is_prefferedNeigh = 1;
						prefNeighbors.put(pv.get(i).pID, peerInfoHashTable.get(pv.get(i).pID));

						count++;

						strPref = strPref + pv.get(i).pID + ", ";

						if (peerInfoHashTable.get(pv.get(i).pID).is_choked == 1) {
							send_unchoke(peerProcess.pIDToSocketMap.get(pv.get(i).pID), pv.get(i).pID);
							peerProcess.peerInfoHashTable.get(pv.get(i).pID).is_choked = 0;
							send_have(peerProcess.pIDToSocketMap.get(pv.get(i).pID), pv.get(i).pID);
							peerProcess.peerInfoHashTable.get(pv.get(i).pID).state = 3;
						}

					}
				}
			} else {
				keys = peerInfoHashTable.keys();
				while (keys.hasMoreElements()) {
					String key = (String) keys.nextElement();
					RemotePeerInfo pref = peerInfoHashTable.get(key);
					if (key.equals(pID))
						continue;

					if (pref.is_completed == 0 && pref.handshake_done == 1) {
						if (!prefNeighbors.containsKey(key)) {
							strPref = strPref + key + ", ";
							prefNeighbors.put(key, peerInfoHashTable.get(key));
							peerInfoHashTable.get(key).is_prefferedNeigh = 1;
						}
						if (pref.is_choked == 1) {
							send_unchoke(peerProcess.pIDToSocketMap.get(key), key);
							peerProcess.peerInfoHashTable.get(key).is_choked = 0;
							send_have(peerProcess.pIDToSocketMap.get(key), key);
							peerProcess.peerInfoHashTable.get(key).state = 3;
						}

					}

				}
			}
			if (strPref != "")
				peerProcess.printLogs(peerProcess.pID + " has selected the preferred neighbors - " + strPref);
		}
	}

	private static void send_unchoke(Socket socket, String pID_remote) {
		printLogs(pID + " is sending UNCHOKE message to remote Peer " + pID_remote);
		Actual_msg d = new Actual_msg(data_unchoke_msg);
		byte[] msgByte = Actual_msg.encode_msg(d);
		send_data(socket, msgByte);
	}

	private static void send_have(Socket socket, String pID_remote) {
		byte[] encodedBitField = peerProcess.ownBitField.encode();
		printLogs(pID + " sending HAVE message to Peer " + pID_remote);
		Actual_msg d = new Actual_msg(data_have_msg, encodedBitField);
		send_data(socket, Actual_msg.encode_msg(d));
		encodedBitField = null;
	}

	private static int send_data(Socket socket, byte[] encodedBitField) {
		try {
			OutputStream out = socket.getOutputStream();
			out.write(encodedBitField);
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
		return 1;
	}

	public static class unchoke_neighbor extends TimerTask {

		public void run() {
			getPeerInformationAgain();
			if (!unchoke_neighbor.isEmpty())
				unchoke_neighbor.clear();
			Enumeration<String> keys = peerInfoHashTable.keys();
			Vector<RemotePeerInfo> peers = new Vector<RemotePeerInfo>();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				RemotePeerInfo pref = peerInfoHashTable.get(key);
				if (pref.is_choked == 1 && !key.equals(pID) && pref.is_completed == 0 && pref.handshake_done == 1)
					peers.add(pref);
			}

			if (peers.size() > 0) {
				Collections.shuffle(peers);
				RemotePeerInfo p = peers.firstElement();

				peerInfoHashTable.get(p.pID).is_optimisticNeighUnchoked = 1;
				unchoke_neighbor.put(p.pID, peerInfoHashTable.get(p.pID));
				peerProcess.printLogs(peerProcess.pID + " has the optimistically unchoked neighbor " + p.pID);

				if (peerInfoHashTable.get(p.pID).is_choked == 1) {
					peerProcess.peerInfoHashTable.get(p.pID).is_choked = 0;
					send_unchoke(peerProcess.pIDToSocketMap.get(p.pID), p.pID);
					send_have(peerProcess.pIDToSocketMap.get(p.pID), p.pID);
					peerProcess.peerInfoHashTable.get(p.pID).state = 3;
				}
			}

		}

	}

	public static void unchokeNeighbor_start() {
		preferredTimer = new Timer();
		preferredTimer.schedule(new unchoke_neighbor(), Common_cfg_clone.optUnchokingInterval * 1000 * 0,
				Common_cfg_clone.optUnchokingInterval * 1000);
	}

	public static void unchokeNeighbor_stop() {
		preferredTimer.cancel();
	}

	public static void startPreferred_neighbor() {
		preferredTimer = new Timer();
		preferredTimer.schedule(new prefNeighbors(), Common_cfg_clone.unchokingInterval * 1000 * 0,
				Common_cfg_clone.unchokingInterval * 1000);
	}

	public static void stopPreferred_neighbor() {
		preferredTimer.cancel();
	}

	public static void printLogs(String message) {
		Logger.log_write(Logger.set_time_format() + ": Peer " + message);
		System.out.println(Logger.set_time_format() + ": Peer " + message);
	}

	public static void showProperties() {
		String line;
		try {
			BufferedReader in = new BufferedReader(new FileReader("Common.cfg"));
			while ((line = in.readLine()) != null) {
				String[] tokens = line.split("\\s+");
				if (tokens[0].equalsIgnoreCase("NumberOfPreferredNeighbors")) {
					Common_cfg_clone.numOfPreferredNeighbr = Integer.parseInt(tokens[1]);
				} else if (tokens[0].equalsIgnoreCase("UnchokingInterval")) {
					Common_cfg_clone.unchokingInterval = Integer.parseInt(tokens[1]);
				} else if (tokens[0].equalsIgnoreCase("OptimisticUnchokingInterval")) {
					Common_cfg_clone.optUnchokingInterval = Integer.parseInt(tokens[1]);
				} else if (tokens[0].equalsIgnoreCase("FileName")) {
					Common_cfg_clone.fileName = tokens[1];
				} else if (tokens[0].equalsIgnoreCase("FileSize")) {
					Common_cfg_clone.fileSize = Integer.parseInt(tokens[1]);
				} else if (tokens[0].equalsIgnoreCase("PieceSize")) {
					Common_cfg_clone.pieceSize = Integer.parseInt(tokens[1]);
				}
			}

			in.close();
		} catch (Exception ex) {
			printLogs(pID + ex.toString());
		}
	}

	public static void showPeerInfo() {
		String st;
		try {
			BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
			int i = 0;
			while ((st = in.readLine()) != null) {
				String[] tokens = st.split("\\s+");
				peerInfoHashTable.put(tokens[0],
						new RemotePeerInfo(tokens[0], tokens[1], tokens[2], Integer.parseInt(tokens[3]), i));
				i++;
			}
			in.close();
		} catch (Exception ex) {
			printLogs(pID + ex.toString());
		}
	}

	public static void main(String[] args) {
		peerProcess processP = new peerProcess();
		pID = args[0];

		try {
			Logger.start("log_peer_" + pID + ".log");
			printLogs(pID + " is started");

			showProperties();

			showPeerInfo();

			initializePrefferedNeighbours();

			boolean isItFirstPeer = false;

			Enumeration<String> e = peerInfoHashTable.keys();

			while (e.hasMoreElements()) {
				RemotePeerInfo peerInfo = peerInfoHashTable.get(e.nextElement());
				if (peerInfo.pID.equals(pID)) {
					processP.listenerPort = Integer.parseInt(peerInfo.peer_port);
					processP.p_index = peerInfo.p_index;
					if (peerInfo.get_isFirstP() == 1) {
						isItFirstPeer = true;
						break;
					}
				}
			}

			ownBitField = new BitField();
			ownBitField.initialize_bitfield(pID, isItFirstPeer ? 1 : 0);

			Msg_handler = new Thread(new Msg_handler(pID));
			Msg_handler.start();

			if (isItFirstPeer) {
				try {
					processP.socketListener = new ServerSocket(processP.listenerPort);
					processP.listener = new Thread(new listener(processP.socketListener, pID));
					processP.listener.start();
				} catch (SocketTimeoutException tox) {
					printLogs(pID + " gets time out exception: " + tox.toString());
					Logger.stop();
					System.exit(0);
				} catch (IOException ex) {
					printLogs(pID + " gets exception in Starting Listening thread: " + processP.listenerPort
							+ ex.toString());
					Logger.stop();
					System.exit(0);
				}
			}
			else {
				createEmptyFile();

				e = peerInfoHashTable.keys();
				while (e.hasMoreElements()) {
					RemotePeerInfo peerInfo = peerInfoHashTable.get(e.nextElement());
					if (processP.p_index > peerInfo.p_index) {
						Thread tempThread = new Thread(new RemotePeerHandler(peerInfo.get_pAddress(),
								Integer.parseInt(peerInfo.get_pPort()), 1, pID));
						receiverThread.add(tempThread);
						tempThread.start();
					}
				}
				try {
					processP.socketListener = new ServerSocket(processP.listenerPort);
					processP.listener = new Thread(new listener(processP.socketListener, pID));
					processP.listener.start();
				} catch (SocketTimeoutException tox) {
					printLogs(pID + " gets time out exception in Starting the listening thread: " + tox.toString());
					Logger.stop();
					System.exit(0);
				} catch (IOException ex) {
					printLogs(pID + " gets exception in Starting the listening thread: " + processP.listenerPort + " "
							+ ex.toString());
					Logger.stop();
					System.exit(0);
				}
			}

			startPreferred_neighbor();
			unchokeNeighbor_start();

			while (true) {
				finishedBool = finishedBool();
				if (finishedBool) {
					printLogs("All peers have completed downloading the file.");

					stopPreferred_neighbor();
					unchokeNeighbor_stop();

					try {
						Thread.currentThread();
						Thread.sleep(2000);
					} catch (InterruptedException ex) {
					}

					if (processP.listener.isAlive())
						processP.listener.stop();

					if (Msg_handler.isAlive())
						Msg_handler.stop();

					for (int i = 0; i < receiverThread.size(); i++)
						if (receiverThread.get(i).isAlive())
							receiverThread.get(i).stop();

					for (int i = 0; i < thread_send.size(); i++)
						if (thread_send.get(i).isAlive())
							thread_send.get(i).stop();

					break;
				} else {
					try {
						Thread.currentThread();
						Thread.sleep(5000);
					} catch (InterruptedException ex) {
					}
				}
			}
		} catch (Exception ex) {
			printLogs(pID + " Exception in ending : " + ex.getMessage());
		} finally {
			printLogs(pID + " Peer process is exiting..");
			Logger.stop();
			System.exit(0);
		}
	}

	private static void initializePrefferedNeighbours() {
		Enumeration<String> keys = peerInfoHashTable.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			if (!key.equals(pID)) {
				prefNeighbors.put(key, peerInfoHashTable.get(key));
			}
		}
	}

	public static synchronized boolean finishedBool() {

		String line;
		int hasFileCount = 1;

		try {
			BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));

			while ((line = in.readLine()) != null) {
				hasFileCount = hasFileCount * Integer.parseInt(line.trim().split("\\s+")[3]);
			}
			if (hasFileCount == 0) {
				in.close();
				return false;
			} else {
				in.close();
				return true;
			}

		} catch (Exception e) {
			printLogs(e.toString());
			return false;
		}

	}

	public static void createEmptyFile() {
		try {
			File dir = new File(pID);
			dir.mkdir();

			File newfile = new File(pID, Common_cfg_clone.fileName);
			OutputStream os = new FileOutputStream(newfile, true);
			byte b = 0;

			for (int i = 0; i < Common_cfg_clone.fileSize; i++)
				os.write(b);
			os.close();
		} catch (Exception e) {
			printLogs(pID + " Error while creating the file : " + e.getMessage());
		}

	}

}
