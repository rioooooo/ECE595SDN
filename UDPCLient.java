import java.io.*;
import java.net.*;
import java.lang.Thread;
import java.util.Timer;
import java.util.TimerTask;

public class UDPClient {
static int client_id;
static String hostname,clienthostname;
static int port,clientport;


	//declare the struct of client input
UDPClient(int Client_ID,String Hostname,int Port)
{
    client_id = Client_ID;
    hostname = Hostname;
    port = Port;
}


public static void main(String args[]) throws Exception    
{       
		//declare the variable
		//UDPClient client_info = NULL;
		int clientid = 0;
		//try to get the client info from command line
		try{
			UDPClient client_info = new UDPClient(Integer.parseInt(args[0]),args[1],Integer.parseInt(args[2]));
			
			
			System.out.println("client id = " + client_info.client_id);
			System.out.println("host name = " + client_info.hostname);
			System.out.println("port num = " + client_info.port);

			clientid = client_info.client_id;
			clienthostname = client_info.hostname;
			clientport = client_info.port;


		}		
		catch (Exception e)
		{
			System.out.println("Please input the client info as the following example: ");
			System.out.println("./UDPClient [Client_ID] [hostname] [port number]");
		}

		//BufferedReader inFromUser =          
		//		new BufferedReader(new InputStreamReader(System.in));       
		DatagramSocket clientSocket = new DatagramSocket();        //declare the client socket
		InetAddress IPAddress = InetAddress.getByName(clienthostname);    //get the ip address by name

		byte[] sendData = new byte[1024];       
		byte[] receiveData = new byte[1024];  

		int finishflag = 0;
		int[] neighbor_ID;
		String[] neighborID_active;
		int[] neighbor_port = {0};
		int i;
		String[] parts;
		String modifiedSentence = "";
		//DatagramPacket receivePacket;
		//DatagramPacket sendPacket;
		int length_parts = 0;

		try{
			
			//String sentence = inFromUser.readLine();       
			//sendData = sentence.getBytes();       
			String sentence = "";
			String idnum = String.valueOf(clientid);
			sentence = idnum + " " + "REGISTER_REQUEST";
			System.out.println("sentence = " + sentence);
			sendData = sentence.getBytes();  //construct the sentence

			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, clientport);       //compose the packet
			clientSocket.send(sendPacket);        //send the packet
			
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);       
			clientSocket.receive(receivePacket);       //receive the packet from server
			
			modifiedSentence = new String(receivePacket.getData());       //get the data from packet
			System.out.println("FROM SERVER:" + modifiedSentence);  

			parts = modifiedSentence.split(" ");  //split the message from server
			System.out.println(parts[0]);

			length_parts = parts.length;


			if (parts[0].equals("REGISTER_RESPONSE"))
			{
				//int size = Integer.parseInt(parts[1]);   //get the number of neighbors
				System.out.println("number of neighbors: " + (length_parts-1));

				neighbor_ID = new int[length_parts-1];
				neighborID_active = new String[length_parts-1];
				neighbor_port = new int[length_parts-1];

				i = 0;
				while( i < length_parts-1 )
				{	
					String[] pofparts = parts[i + 1].split("_");  //split the 
					neighbor_ID[i] = Integer.parseInt(pofparts[0]); 
					neighborID_active[i] = pofparts[1];
					neighbor_port[i] = Integer.parseInt(pofparts[2]);
					i = i + 1; //update the counter	

				}

				
				for (i = 0; i < length_parts-1; i++)
				{
					System.out.println("neighbor " + i + neighborID_active[i] + neighbor_port[i]);
				}
			}

			finishflag = 1;	
		
			//clientSocket.close(); 
		}
		catch (Exception e)
		{
			System.out.println("Server is not responding. Please try again.");
			System.out.println("Exception caught:" + e);
		   
		} 

		


		String sentence;

		while(true){
			
			if (finishflag == 1)
			{
				//send info to active neighbors
				for (i = 0; i < length_parts-1 ; i ++)
				{
						sentence = "KEEP_ALIVE";
						sendData = sentence.getBytes();
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, neighbor_port[i]);
						clientSocket.send(sendPacket);
						//call timer
					
				}
				
			}
			finishflag = 0;
			


			//try accept data from neighbors
			try{
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);       
				clientSocket.receive(receivePacket);       //receive the packet from server

				String receivedsentence = new String(receivePacket.getData());
				System.out.println("FROM SOMEONE:" + receivedsentence);

				parts = modifiedSentence.split(" ");  //split the message
				System.out.println("FROM SOMEONE" + parts[0]);  
				if (parts[0].equals("KEEP_ALIVE"))
				{

				}

			}
			catch(Exception e){
				System.err.println("Exception in receiving packet ...");

			}
			//reply keepalive

			//update neighbor table , inac ->ac


			//subthread
			//send topology update to controller
			//recv table from controller, print it out
		}   //while loop

}
}


// class MyTimerTask extends TimerTask{
// 	int client_neighbor_id;
// 	MyTimerTask(int passed_neighbor_id)
// 	{
// 		this.client_neighbor_id = passed_neighbor_id;
// 	}

// 	public void run(){

// 	}

// }

// class ClientHandler extends Thread {
// 	int neighbor_id;
// 	int neighbor_port;
// 	String 
// 	Timer timer;
// 	ClientHandler (int neighbor_id, int neighbor_port, Timer timer,){
// 		this.neighbor_id = neighbor_id;
// 		this neighbor_port = neighbor_port;
// 		this.timer = timer;
// 	}

// 	public void run(){
// 		try{	
// 			sentence = "KEEP_ALIVE";
// 			sendData = sentence.getBytes();
// 			sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, neighbor_port);
// 			clientSocket.send(sendPacket);
// 		}



// 	}
// }














