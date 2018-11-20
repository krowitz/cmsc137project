
import java.io.*;

public class ReceiveThread extends Thread {
	private ChatClient client;
	private DataInputStream inputStream;
	private boolean status;

	public ReceiveThread(DataInputStream inputStream) {
		this.status = true;
		this.inputStream = inputStream;
	}

	public boolean getStatus(){
		return this.status;
	}

	public void stopReceiving(){
		this.status = false;
		this.interrupt();
	}

	public void run() {
		while (true) {
			try {

				byte[] data = new byte[1024];
				inputStream.read(data);

				System.out.println("Acquired from server: " + new String(data));

			} catch (Exception e) {
				System.out.println("Error reading from server");
				e.printStackTrace();
			}
		}
	}
}