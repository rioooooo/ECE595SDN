import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.net.*;
import java.util.Hashtable;

//do we need to replace the active_table to concurrent_active_table
public class Controller {
	public static void main(String args[]) throws Exception
	{
		StringBuilder fileresult = new StringBuilder();
		int graph_width[][] = null;
		int graph_delay[][] = null;

		
		//for debug using!!!!!
		
		//print out the configuration graph
		/*
		int ci,cj;
		for(ci=0;ci<6;ci++)
		{
			System.out.print("{");
			for(cj=0;cj<6;cj++)
			{
				System.out.print(graph_delay[ci][cj] + ",");
			}
			System.out.print("},");
		}
		*/
		//Open UDP server port
		DatagramSocket serverSocket = new DatagramSocket(5555);
		System.out.println("UDP server running at port 5555, and localhost");
		byte[] receiveData = new byte[1024];
        byte[] sendData = new byte[1024];
        //waiting for all switches to register into the network
        int count = 0;
        int total_switch_nums = -1;
        //neighbor table
        Hashtable <String,String> neighbor_table = new Hashtable <String,String> ();
        //active table
        Hashtable <String,String> active_table = new Hashtable <String,String> ();
        //port table
        Hashtable <String,String> port_table = new Hashtable <String,String> ();
        //IP table
        Hashtable <String,String> IP_table = new Hashtable <String,String> ();
        int port = 0;
        
        
		//try open the configuration file and get the graph
		graph_width = openthefile(fileresult,args[0],graph_width);
		graph_delay = openthefile2(args[0],graph_delay,neighbor_table);
		
		
		//System.out.println(neighbor_table);
        while (count != total_switch_nums)
        {
        	//rcv data from switches
            receiveData = new byte[1024];
            sendData = new byte[1024];
        	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        	serverSocket.receive(receivePacket);
        	String sentence = new String( receivePacket.getData());
        	System.out.println("RECEIVED: " + sentence);
        	InetAddress IPAddress = receivePacket.getAddress();
        	port = receivePacket.getPort();
        	StringBuilder neighbors = new StringBuilder();
        	//get neighbors information and send back with REGISTER_RESPONSE
        	String ID = "";
        	if(sentence.contains("REGISTER_REQUEST"))
        	{
        		//get this switch ID
        		ID = sentence.split(" ")[0];
        		//if the switch is already processed, then we do nothing
        		if(active_table.contains(ID)) continue;
        		//update table
        		active_table.put(ID, "active");
        		port_table.put(ID, Integer.toString(port));
        		IP_table.put(ID,IPAddress.toString());
        		//ini the neighbors
        		neighbors.append("");
        		
        		//find the neighbors
        		total_switch_nums = create_neighbor_table(neighbors,ID,fileresult,total_switch_nums,active_table,port_table,neighbor_table,IP_table);
        		neighbors.insert(0, "REGISTER_RESPONSE");
        	
        	//send data back to switches
        	//What I send is 
        	System.out.println("What I send to switch "+ID+" is " + neighbors.toString());
        	sendData = neighbors.toString().getBytes();
        	DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
        	serverSocket.send(sendPacket);
        	count = count +1;
        	}
        }
        //register done!!!
        System.out.println("Switches registeration has been done");
        
        
        
        
        int graph_width_temp[][] = null;
		int graph_delay_temp[][] = null;
		//use temp to update, keep the original graph to save data in order to retrieve data when some switches are a live again
		graph_width_temp = graph_width;
		graph_delay_temp = graph_delay;
        //start to use main thread to receive and send info to switches, and use subthread to calculate graph
        while (true)
        {
        	
        	
        	/*
        	 *when receive neighbor active info from switches, update neighbor table, update graph,
        	 *after then, check neighbor table, if neighbor_table.get(ID) == null, then it means that 
        	 *this ID switch is dead, remove it from the active table, update the graph.
        	 *Another case is that, if ID have not sent topo_update to me in 10 secs, then I will remove 
        	 *this ID switch from active table.
        	 */
        	
        	receiveData = new byte[1024];
            sendData = new byte[1024];
        	
        	//always listen for requests from switches
        	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        	serverSocket.receive(receivePacket);
        	String sentence = new String( receivePacket.getData());
        	System.out.println("RECEIVED: " + sentence);
        	InetAddress IPAddress = receivePacket.getAddress();
        	port = receivePacket.getPort();
        	//split the incoming message, and update the active table, then pass info to subthread to calculate graph
        	String [] new_live_neighbors = sentence.split(" ");
        	//Sample incoming data: TOPOLOGY_UPDATE ID 1_active 2_inactive ...
        	if(new_live_neighbors[0].trim().equals("TOPOLOGY_UPDATE"))
        	{
        		String ID = "";
        		ID = new_live_neighbors[1];
        		int count_for_new_active = 2;
        		//because last element is null (space)
        		while(count_for_new_active < new_live_neighbors.length-1)
        		{
        			//update the neighbor table
        			String [] temp = new_live_neighbors[count_for_new_active].split("_");
        			/*
        			 * failure handle: if link is broken, and retrieved switch
        			 */
        			//2[0] inactive[1] ID 1
        			if((temp[1].trim()).equals("inactive"))
        			{
        				System.out.println("Process " + temp[0] + "with inactive");
        				//check the incoming inactive is in active or not????
        				//if all connection to temp[0] is disconnected, update active_table and graph
        				if(neighbor_table.get(temp[0].trim()).equals(""))
        				{
        					System.out.println("Prcess dead switch graph, ID is " + temp[0]);
        					active_table.put(temp[0].trim(), "inactive");
        					int temp_graph_counteri = 0;
        					int temp_graph_counterj = 0;
        					//update graph
        					for(temp_graph_counteri= 0; temp_graph_counteri <total_switch_nums;temp_graph_counteri++)
        					{
        						for(temp_graph_counterj= 0; temp_graph_counterj <total_switch_nums;temp_graph_counterj++)
        						{
        							if((temp_graph_counteri == (Integer.parseInt(temp[0].trim()))-1) || (temp_graph_counterj == (Integer.parseInt(temp[0].trim())-1)))
        							{
        								//if the index is found, then update the graph
        								graph_width_temp[temp_graph_counteri][temp_graph_counterj] = 0;
        								graph_delay_temp[temp_graph_counteri][temp_graph_counterj] = 0;
        							}
        						}
        					}
        				}
        				
        				
        				System.out.println("update broken neighbors links, which ID is" + ID);
        				
        			
        				//if inactive, disconnect these two connection, and update graph & neighbor table
        				String [] temp1 = null;
        				temp1 = neighbor_table.get(ID).split(" ");
        				int neighbor_counter = 0;
        				String temp3 = null;
        				//temp1 2 4 6
        				while(neighbor_counter<temp1.length)
        				{
        					//update ID's neighbor first
        					if(!(temp1[neighbor_counter].trim()).equals((temp[0].trim())))
        					{
        						//if the ID is not the inactive neighbor, and append to temp3
        						temp3 = temp1[neighbor_counter].trim()+" ";
        					}
        					neighbor_counter++;
        				}
        				//delete the last space 
        				if(temp3!=null) temp3 = temp3.trim();
        				//update the ID's neighbor table
        				neighbor_table.put(ID, temp3);
        				
        				
        				
        				
        				System.out.println("update the broken neighbors links, which ID is " + temp[0]);
        				//update the incoming inactive switch's neighbor table
        				temp1 = null;
        				temp1 = neighbor_table.get(temp[0].trim()).split(" ");
        				//2's neighbors: 1 3 5
        				neighbor_counter = 0;
        				temp3 = "";
        				//temp1 1 3 5
        				while(neighbor_counter<temp1.length)
        				{
        					//update ID's neighbor first
        					if(!(temp1[neighbor_counter].trim()).equals((ID.trim())))
        					{
        						//if the ID is not the inactive neighbor, and append to temp3
        						temp3 = temp1[neighbor_counter].trim()+" ";
        					}
        					neighbor_counter++;
        				}
        				//delete the last space
        				if(temp3!="") temp3 = temp3.trim();
        				//update the incoming inactive switch's neighbor table
        				neighbor_table.put(temp[0].trim(), temp3);
        				
        				
        				
        				
        				System.out.println("Update graph...");
        				//update the graph
        				int temp_graph_counteri = 0;
    					int temp_graph_counterj = 0;
    					//update graph
    					for(temp_graph_counteri= 0; temp_graph_counteri <total_switch_nums;temp_graph_counteri++)
    					{
    						for(temp_graph_counterj= 0; temp_graph_counterj <total_switch_nums;temp_graph_counterj++)
    						{
    							//set graph[ID-1][temp[0]-1]  to 0
    							if((temp_graph_counteri == (Integer.parseInt(ID.trim()))-1) && (temp_graph_counterj == (Integer.parseInt(temp[0].trim())-1)))
    							{
    								//if the index is found, then update the graph
    								graph_width_temp[temp_graph_counteri][temp_graph_counterj] = 0;
    								graph_delay_temp[temp_graph_counteri][temp_graph_counterj] = 0;
    							}
    							//set graph[temp[0]-1][ID]  to 0
    							if((temp_graph_counteri == (Integer.parseInt(temp[0].trim()))-1) && (temp_graph_counterj == (Integer.parseInt(ID.trim())-1)))
    							{
    								//if the index is found, then update the graph
    								graph_width_temp[temp_graph_counteri][temp_graph_counterj] = 0;
    								graph_delay_temp[temp_graph_counteri][temp_graph_counterj] = 0;
    							}
    						}
    					}
    					System.out.println("new neighbor_table is " + neighbor_table + " \nGraphs are also updated by disconnecting the pair of connection");
    					//end update neighbor_table and graph
        				
        				
        			}//end with inactive
        			
        			
        			//start with active!!!!check for returned switches
        			else if ((temp[1].trim()).equals("active"))
        			{
        				//firstly check the active table, if this switch is inactive in the active table,
        				//mark it to active back
        				if(active_table.get(temp[0].trim()).equals("inactive"))
        				{
        					System.out.println("Process returned dead switch");
        					active_table.put(temp[0].trim(), "active");
        					System.out.println(temp[0]+" is reactived, and the graph has been updated");
        					//update the graph, retrieve info from original table
        					int temp_graph_counteri = 0;
        					int temp_graph_counterj = 0;
        					//update graph
        					for(temp_graph_counteri= 0; temp_graph_counteri <total_switch_nums;temp_graph_counteri++)
        					{
        						for(temp_graph_counterj= 0; temp_graph_counterj <total_switch_nums;temp_graph_counterj++)
        						{
        							if((temp_graph_counteri == (Integer.parseInt(temp[0].trim()))-1) || (temp_graph_counterj == (Integer.parseInt(temp[0].trim())-1)))
        							{
        								//if the index is found, then update the graph
        								graph_width_temp[temp_graph_counteri][temp_graph_counterj] = graph_width[temp_graph_counteri][temp_graph_counterj];
        								graph_delay_temp[temp_graph_counteri][temp_graph_counterj] = graph_delay[temp_graph_counteri][temp_graph_counterj];
        							}
        						}
        					}
        				}
        				//this switch is active, but the link between these two are broken before,
        				//we need to return all info for these two switch
        				/*
        				Used small tricky way here, no matter the link between these two are broken before or not,
        				treat this as broken link
        				Because if inactive, the neighbor will be corrected
        				*/
        				else
        				{
        					System.out.println("Process broken link if link is broken, else do nothing, which ID is "+temp[0]);
        					//update ID's neighbor table
        					String temp_retrieve = "";
        					temp_retrieve = neighbor_table.get((ID.trim())).trim() + " " + temp[0].trim();
        					neighbor_table.put(ID.trim(), temp_retrieve.trim());
        					//update incoming switch neighbor table
        					temp_retrieve = "";
        					temp_retrieve = neighbor_table.get((temp[0].trim())).trim() + " " + ID.trim();
        					neighbor_table.put(temp[0].trim(), temp_retrieve.trim());
        					//update the graph
        					int temp_graph_counteri = 0;
        					int temp_graph_counterj = 0;
        					//update graph
        					for(temp_graph_counteri= 0; temp_graph_counteri <total_switch_nums;temp_graph_counteri++)
        					{
        						for(temp_graph_counterj= 0; temp_graph_counterj <total_switch_nums;temp_graph_counterj++)
        						{
        							if((temp_graph_counteri == (Integer.parseInt(ID.trim()))-1) && (temp_graph_counterj == (Integer.parseInt(temp[0].trim())-1)))
        							{
        								//if the index is found, then update the graph
        								graph_width_temp[temp_graph_counteri][temp_graph_counterj] = graph_width[temp_graph_counteri][temp_graph_counterj];
        								graph_delay_temp[temp_graph_counteri][temp_graph_counterj] = graph_delay[temp_graph_counteri][temp_graph_counterj];
        							}
        							if((temp_graph_counteri == (Integer.parseInt(temp[0].trim()))-1) && (temp_graph_counterj == (Integer.parseInt(ID.trim())-1)))
        							{
        								//if the index is found, then update the graph
        								graph_width_temp[temp_graph_counteri][temp_graph_counterj] = graph_width[temp_graph_counteri][temp_graph_counterj];
        								graph_delay_temp[temp_graph_counteri][temp_graph_counterj] = graph_delay[temp_graph_counteri][temp_graph_counterj];
        							}
        						}
        					}
        				}
        				//else is end
        			}//end with active
                    count_for_new_active++;
        		}//end while read from the switch active & inactive info
        		System.out.println("Failure handle finished, will print the route table for this ID "+ID);
        		//after update the active table, pass the required info to subthread to calculate graph, and then send back to corresponding ID switch
        		Process_graphs prc = null;
        		Thread t1 = null;
        		prc = new Process_graphs(ID,IPAddress,port,active_table,graph_width_temp,graph_delay_temp,serverSocket,total_switch_nums);
        		t1 = new Thread(prc);
        		t1.start();
        		//subthread is in processing
        		System.out.println("The route table for ID " + ID + " is in processing");
        	}
        	//continue listen to other switches
        	//end while
        }
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//create the neighbor table
	private static int create_neighbor_table(StringBuilder neighbors,String ID,StringBuilder fileresult, int total_switch_nums, Hashtable <String,String> active_table,Hashtable <String,String> port_table,Hashtable <String,String> neighbor_table,Hashtable <String,String> IP_table)
	{
		StringBuilder temp1 = new StringBuilder(fileresult);
		String temp2 = temp1.toString();
		String [] parts = temp2.split("\n");
		total_switch_nums = Integer.parseInt(parts[0]);
		int num_lines = parts.length;
		//start construct the neighbor table
		int i = 1;
		while(i<num_lines)
		{
			String [] switch_parts = parts[i].toString().split(" ");
			if(switch_parts[0].equals(ID))
			{
				//update neighbors
				//active table & IP table
				if(active_table.containsKey(switch_parts[1]))
				{
					neighbors.append(" "+switch_parts[1]+"_"+active_table.get(switch_parts[1])+"_"+IP_table.get(switch_parts[1]));
				}
				else
				{
					neighbors.append(" "+switch_parts[1]+"_"+"inactive"+"_unknownIP");
				}
				//port table
				if(port_table.containsKey(switch_parts[1]))
				{
					neighbors.append("_"+port_table.get(switch_parts[1]));
				}
				else
				{
					neighbors.append("_"+"unknownport");
				}
				
			}
			else if (switch_parts[1].equals(ID))
			{
				//update neighbors
				if(active_table.containsKey(switch_parts[0]))
				{
					neighbors.append(" "+switch_parts[0]+"_"+active_table.get(switch_parts[0])+"_"+IP_table.get(switch_parts[0]));
				}
				else
				{
					neighbors.append(" "+switch_parts[0]+"_"+"inactive"+"_unknownIP");
				}
				//port table
				if(port_table.containsKey(switch_parts[0]))
				{
					neighbors.append("_"+port_table.get(switch_parts[0]));
				}
				else
				{
					neighbors.append("_"+"unknownport");
				}
			}
			i = i+1;
		}
		//update neighbor_table
		//neighbor_table.put(ID, neighbors.toString());
		return total_switch_nums;
	}
	
	
	
	//open the file class
	private static int[][] openthefile(StringBuilder fileresult,String inputfile,int [][] graph_width)
	{
		String filename = "";
		int flag = 0;
		
		try
		{
			filename = inputfile;
			FileReader filereader = new FileReader(filename);
			 // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader =  new BufferedReader(filereader);
            
            String line="";
            while((line = bufferedReader.readLine()) != null) {
            	
            	if(flag == 1)
            	{
            		
            		String [] idx;
            		idx = line.split(" ");
            		
            		graph_width[Integer.parseInt(idx[0].trim())-1][Integer.parseInt(idx[1].trim())-1]=Integer.parseInt(idx[2].trim());
            		graph_width[Integer.parseInt(idx[1].trim())-1][Integer.parseInt(idx[0].trim())-1]=Integer.parseInt(idx[2].trim());
            		
            	}
            	//first line is total number
            	if(flag == 0)
            	{
            		//create 2-dimensional array of the graph based on the configuration file
                    graph_width= new int [Integer.parseInt(line.trim())][Integer.parseInt(line.trim())];
                   
                    
                    flag = 1;
            	}
            	fileresult.append(line+'\n');
            	
            }   

            // Always close files.
            bufferedReader.close();         
        }
        catch(FileNotFoundException ex) {
            System.out.println(
                "Unable to open file '" + 
                filename + "'");                
        }
        catch(IOException ex) {
            System.out.println(
                "Error reading file '" 
                + filename + "'");                  
            // Or we could just do this: 
            // ex.printStackTrace();
        }
		return graph_width;
	}
	
	
	//to get the delay graph
	
	private static int[][] openthefile2(String inputfile,int [][] graph_delay, Hashtable <String,String> neighbor_table)
	{
		String filename = "";
		int flag = 0;
		
		try
		{
			filename = inputfile;
			FileReader filereader = new FileReader(filename);
			 // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader =  new BufferedReader(filereader);
            
            String line="";
            while((line = bufferedReader.readLine()) != null) {
            	
            	if(flag == 1)
            	{
            		
            		String [] idx;
            		idx = line.split(" ");
            		graph_delay[Integer.parseInt(idx[0].trim())-1][Integer.parseInt(idx[1].trim())-1]=Integer.parseInt(idx[3].trim());
            		graph_delay[Integer.parseInt(idx[1].trim())-1][Integer.parseInt(idx[0].trim())-1]=Integer.parseInt(idx[3].trim());
            		//update the neighbor_table
            		//if ID is in the table
            		if(neighbor_table.containsKey(idx[0]))
            		{
            			//update the neighbor table, which separate with space " "
            			String temp = neighbor_table.get(idx[0]) + " "+idx[1];
            			neighbor_table.put(idx[0], temp);
            			if(neighbor_table.containsKey(idx[1]))
            			{
            				String temp2 = neighbor_table.get(idx[1]) + " "+idx[0];
            				neighbor_table.put(idx[1],temp2);
            			}
            			else
            			{
            				neighbor_table.put(idx[1],idx[0]);
            			}
            		}
            		else
            		{
            			//ID is not in the table
            			neighbor_table.put(idx[0], idx[1]);
            			neighbor_table.put(idx[1], idx[0]);
            		}
            		
            	}
            	//first line is total number
            	if(flag == 0)
            	{
            		//create 2-dimensional array of the graph based on the configuration file
                    graph_delay= new int [Integer.parseInt(line.trim())][Integer.parseInt(line.trim())];
                   
                    
                    flag = 1;
            	}
            	
            	
            }   

            // Always close files.
            bufferedReader.close();         
        }
        catch(FileNotFoundException ex) {
            System.out.println(
                "Unable to open file '" + 
                filename + "'");                
        }
        catch(IOException ex) {
            System.out.println(
                "Error reading file '" 
                + filename + "'");                  
            // Or we could just do this: 
            // ex.printStackTrace();
        }
		return graph_delay;
	}
}
