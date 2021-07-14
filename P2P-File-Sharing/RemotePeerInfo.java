import java.util.Date;

public class RemotePeerInfo implements Comparable<RemotePeerInfo>
{
	public String pID;
	public String peer_addresss;
	public String peer_port;
	public int isItFirstPeer;
	public double dataRate = 0;
	public int is_interested = 1;
	public int is_prefferedNeigh = 0;
	public int is_optimisticNeighUnchoked = 0;
	public int is_choked = 1;
	public BitField bitField;
	public int state = -1;
	public int p_index;
	public int is_completed = 0;
	public int handshake_done = 0;
	public Date startTime;
	public Date finishTime;
	
	public RemotePeerInfo(String pId, String pAddress, String pPort, int pIndex)
	{
		pID = pId;
		peer_addresss = pAddress;
		peer_port = pPort;
		bitField = new BitField();
		p_index = pIndex;
	}
	public RemotePeerInfo(String pId, String pAddress, String pPort, int pIsFirstPeer, int pIndex)
	{
		pID = pId;
		peer_addresss = pAddress;
		peer_port = pPort;
		isItFirstPeer = pIsFirstPeer;
		bitField = new BitField();
		p_index = pIndex;
	}
	public String get_pid() {
		return pID;
	}
	public void set_pid(String pID) {
		this.pID = pID;
	}
	public String get_pAddress() {
		return peer_addresss;
	}
	public void set_pAddress(String peer_addresss) {
		this.peer_addresss = peer_addresss;
	}
	public String get_pPort() {
		return peer_port;
	}
	public void set_pPort(String peer_port) {
		this.peer_port = peer_port;
	}
	public int get_isFirstP() {
		return isItFirstPeer;
	}
	public void set_isFirstP(int isItFirstPeer) {
		this.isItFirstPeer = isItFirstPeer;
	}
	public int compareTo(RemotePeerInfo o1) {
		
		if (this.dataRate > o1.dataRate) 
			return 1;
		else if (this.dataRate == o1.dataRate) 
			return 0;
		else 
			return -1;
	}

}
