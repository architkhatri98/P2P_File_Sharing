import java.util.Comparator;

public class Comparision_of_peerRate implements Comparator<RemotePeerInfo> {

	private boolean constructor1;

	public Comparision_of_peerRate() {
		this.constructor1 = true;
	}

	public Comparision_of_peerRate(boolean constructor) {
		this.constructor1 = constructor;
	}

	public int compare(RemotePeerInfo rpi1, RemotePeerInfo rpi2) {
		if (rpi1 == null && rpi2 == null)
			return 0;

		if (rpi1 == null)
			return 1;

		if (rpi2 == null)
			return -1;

		if (rpi1 instanceof Comparable) {
			if (constructor1) {
				return rpi1.compareTo(rpi2);
			} else {
				return rpi2.compareTo(rpi1);
			}
		} else {
			if (constructor1) {
				return rpi1.toString().compareTo(rpi2.toString());
			} else {
				return rpi2.toString().compareTo(rpi1.toString());
			}
		}
	}

}
