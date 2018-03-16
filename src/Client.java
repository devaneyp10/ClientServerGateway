import java.net.DatagramSocket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

import tcdIO.*;

/**
 *
 * Client class
 * 
 * An instance accepts user input 
 *
 */
public class Client extends Node 
{
	
	static final int CLIENT_1_PORT = 1;
	static final int GATEWAY_PORT = 8;
	static final int TIMER = 10000000;
	static final String DEFAULT_DST_NODE = "localhost";	
	DatagramPacket prevPacket;
	public int packetNumber=1;
	private int seqNumber=0;
	
	Terminal terminal;
	InetSocketAddress dstAddress;
	
	/**
	 * Constructor
	 * 	 
	 * Attempts to create socket at given port and create an InetSocketAddress for the destinations
	 */
	Client(Terminal terminal, String dstHost, int dstPort, int srcPort) 
	{
		try 
		{
			this.terminal= terminal;
			dstAddress= new InetSocketAddress(dstHost, dstPort);
			socket= new DatagramSocket(srcPort);
			socket.setSoTimeout(TIMER);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	/*
	 * This method receives a packet from the gateway.
	 * It checks for an error before printing the acknowledgement.
	 * If an error has occured in the sequence numbers at the Server
	 * node the client is notified and prints an error message.
	 */
	public synchronized void onReceipt(DatagramPacket packet) 
	{
		int errorCheck = getErrorFlag(packet);
		if (errorCheck==0)
		{
			StringContent content= new StringContent(packet);
			this.notify();
			terminal.println(content.toString());
			terminal.println("----------------------------------------------------------------------------------");
		}
		else
		{
			StringContent content= new StringContent(packet);
			this.notify();
			terminal.println(content.toString());
			terminal.println("There was an error in the sequence \nof the packets at the server end.");
			terminal.println("----------------------------------------------------------------------------------");
		}
	}

	/*
	 * This method takes in a string to be sent to the gateway.
	 * The string is put into a byte array along with the sequence
	 * number of the packet and a flag to tell the gateway that the 
	 * packet is coming from the Client.
	 */
	public synchronized void start() throws Exception, SocketTimeoutException 
	{
		try
		{
			boolean finished = false;
			while(!finished)
			{
			
				DatagramPacket packet= null;
		
				byte[] payload= null;
				byte[] header= null;
				byte[] buffer= null;
				
			
				
				payload= (terminal.readString("String to send: ")).getBytes();
				header= new byte[PacketContent.HEADERLENGTH];
				
				buffer= new byte[header.length + payload.length];
				System.arraycopy(header, 0, buffer, 0, header.length);
				System.arraycopy(payload, 0, buffer, header.length, payload.length);
			
				terminal.println("Sending packet ...");
				
				packet= new DatagramPacket(buffer, buffer.length, dstAddress);
				setClientFlag(packet);
				setSeqNumber(packet);
				
				socket.send(packet);
				prevPacket = packet;
				terminal.println("Packet "+ packetNumber++ +" sent.\n\nWaiting for acknowledgement.\n");
				
				this.wait();
			}
		}catch(SocketTimeoutException s){
			terminal.println("Socket has timed out. Attempting to re-send packet...");
			resend(prevPacket);
	  }
			
	}

	/*
	 * This method re-sends a packet after a socket has timed out.
	 */
	public void resend(DatagramPacket packet) throws IOException
	{
		socket.send(packet);
		terminal.println("Packet re-sent");
	}
	
	/*
	 * This method sets the first byte in the array to a 1 which acts as a flag 
	 * to the gateway that the packet is coming from the client node
	 */
	public void setClientFlag(DatagramPacket packet)
	{
		byte[] data = packet.getData();
		data[0]=(byte)1;
		packet.setData(data);
	}
	
	/*
	 * This method sets the sequence number of the packet in the 2nd byte of the array
	 */
	public void setSeqNumber(DatagramPacket packet)
	{
		byte[] data = packet.getData();
		data[1]=(byte)seqNumber++;
		packet.setData(data);
	}
	
	/*
	 * This method returns the 3rd byte in the buffer of a packet, to indicate whether 
	 * an error had occured in the sequence numbers at the server node.
	 */
	public int getErrorFlag(DatagramPacket packet)
	{
		byte[] data = packet.getData();
		return (int)data[2];
	}
	
	
	public static void main(String[] args) {
		try 
		{					
			Terminal terminal= new Terminal("Client");		
			(new Client(terminal, DEFAULT_DST_NODE, GATEWAY_PORT, CLIENT_1_PORT)).start();
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}