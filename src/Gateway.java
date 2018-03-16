import java.net.DatagramSocket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import tcdIO.Terminal;
import tcdIO.*;

public class Gateway extends Node
{
	InetSocketAddress clientAddress;
	InetSocketAddress serverAddress;	
	static final int CLIENT_1_PORT = 1;
	static final int GATEWAY_PORT = 8;
	static final int SERVER_PORT = 9;
	public int clientNumber;
	public Terminal terminal;
	static final String DEFAULT_DST_NODE = "localhost";	

	Gateway(Terminal terminal, String dstHost, int dstPortServer,  int dstPortClient, int srcPort)
	{
		try {
			
			clientAddress= new InetSocketAddress(dstHost,dstPortClient);
			serverAddress= new InetSocketAddress(dstHost,dstPortServer);
			this.terminal= terminal;
			socket= new DatagramSocket(srcPort);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	/*
	 * This method receives packets from both the Client and Server nodes and
	 * forwards them to the appropriate destination.
	 * The method deals with errors by forwarding error messages back to the 
	 * client when there has been an error in the sequence numbers 
	 * at the Server node.
	 */
	public void onReceipt(DatagramPacket packet)  
	{
		try
		{
			byte[] buffer = packet.getData();
			byte flagBytes = buffer[0];
			int senderFlag = (int)flagBytes;
			int errorCheck = getErrorFlag(packet);
			
			if(errorCheck == 0)
			{
				if(senderFlag==1)
				{
					terminal.print("Packet received from client.\nForwarding packet 1 to server.\n\n");
					packet.setSocketAddress(serverAddress);
				}
				else if (senderFlag==0)
				{
					terminal.println("Packet has been received by the server\nSending acknowledgement back to client");
					terminal.println("----------------------------------------------------------------------------------");

					packet.setSocketAddress(clientAddress);						
				}
			}
			else
			{
				StringContent content= new StringContent(packet);
				terminal.println(content.toString());
				
				terminal.println("\n\nThere has been an error in sequence numbers at the Server end.\nForwarding this information back to the Client");
				terminal.println("-------------------------------------------------------------------------------------");

				packet.setSocketAddress(clientAddress);
			}
			socket.send(packet);
			
		}catch(Exception e) {e.printStackTrace();}
	
	}
	
	/*
	 * This method returns the 3rd bit in the buffer. Indicates whether an error has occured.
	 */
	public int getErrorFlag(DatagramPacket packet)
	{
		byte[] data = packet.getData();
		return (int)data[2];
	}
	
	public synchronized void start() throws Exception {
		terminal.println("Waiting for contact...\n");
		this.wait();
	}
	
	public static void main(String[] args) {
		try 
		{					
			Terminal terminal= new Terminal("Gateway");		
			(new Gateway(terminal, DEFAULT_DST_NODE, SERVER_PORT, CLIENT_1_PORT, GATEWAY_PORT)).start();
			terminal.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}}
	
}