import java.net.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
public class Process_graphs implements Runnable{
	//Constructor
	String ID;
	InetAddress IPAddress;
	int port;
	Hashtable <String,String> active_table; 
	int [][]graph_width;
	int [][]graph_delay;
	DatagramSocket serverSocket;
	int total_switch_nums;
	public Process_graphs(String ID, InetAddress IPAddress,int port, Hashtable <String,String> active_table, int[][]graph_width,int[][]graph_delay,DatagramSocket serverSocket,int total_switch_nums)
	{
		this.active_table = active_table;
		this.graph_width = graph_width;
		this.graph_delay = graph_delay;
		this.ID = ID;
		this.IPAddress = IPAddress;
		this.port = port;
		this.serverSocket = serverSocket;
		this.total_switch_nums = total_switch_nums;
	}
	public void run()
	{
		try
		{
			System.out.println("This is thread"+Thread.currentThread().getName().toString()+", which is processing the graph of switch" + ID);
			//start computing the graph
			int num_active_switch = total_switch_nums;
			/*
			while(i<active_table.size())
			{
				if(active_table.get((String.valueOf(i+1))).trim().equals("active")) num_active_switch++;
				i++;
			}
			*/
			//Compute the paths, and store in the results
			String results = "";
			results = dijkstra(graph_width,graph_delay,Integer.parseInt(ID.trim())-1,num_active_switch);
			System.out.println("topology is " +results);
			//send the new topology to switch
			String outputdata = "ROUTE_UPDATE "+results;
			byte[] sendData = new byte[1024];
			sendData = outputdata.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
        	serverSocket.send(sendPacket);
			//Sending route update is done!!!
			
		}
		catch (Exception e)
		{
			System.err.println(e);
		}
	}
	
	

	
	
	String Find_shortest(StringBuilder widest_paths,int [][] graph_delay)
	{
		//System.out.println(widest_paths.toString());
		Hashtable <Integer,Integer> dist = new Hashtable <Integer,Integer>();
		StringBuilder final_result =new StringBuilder();
		String all_paths = widest_paths.toString();
		String paths[] = all_paths.split("\n");
		String temp_string = "";
		int i = 0;
		int end_node = -1;
		int max_for_same = Integer.MAX_VALUE;
		int max_temp = 0;
		while(i<paths.length)
		{
			max_temp = 0;
			max_for_same = 0;
			String nodes [] = paths[i].split(",");
			//update the end_node
			end_node = Integer.parseInt(nodes[0].trim());
			//found the same destination path
			
			if(dist.containsKey(end_node))
			{
				int last_string_ind = final_result.indexOf(temp_string);
				int j = 0;
				while(j<nodes.length-1)
				{
					max_for_same+=graph_delay[Integer.parseInt((nodes[j]).trim())-1][Integer.parseInt(nodes[j+1].trim())-1];
					j++;
				}
				System.out.println(paths[i].toString()+"has the same width, and its delay is " + max_for_same);
				//if shorter path is found, then replace with last path, and update the table
				if(max_for_same < dist.get(end_node))
				{
					dist.put(end_node, max_for_same);
					final_result.replace(last_string_ind, final_result.length()-1, paths[i].toString());
					temp_string = paths[i].toString();
				}
			}
			//this is the only path
			else
			{
				
				int j = 0;
				while(j<nodes.length-1)
				{
					max_temp+=graph_delay[Integer.parseInt((nodes[j]).trim())-1][Integer.parseInt(nodes[j+1].trim())-1];
					j++;
				}
				//update delay table for each destination
				dist.put(end_node, max_temp);
				System.out.println(paths[i].toString() + "has shortest path at delay "+max_temp);
				temp_string = paths[i].toString();
				final_result.append(paths[i]+"\n");
			}
			
			i++;
		}
		//System.out.println("Final paths are " + final_result.reverse().toString());
		return final_result.reverse().toString();
	}
	
	
	StringBuilder Print_func(List<Vertex> prev,Vertex [] vertices,int src,int V)
	{
		//print all widest path
		System.out.println("Will print all possible widest path first");
		int i = 0;
		StringBuilder results = new StringBuilder();
		StringBuilder output = new StringBuilder();
		for(i=0;i<V;i++)
		{
			if(i != src)
			{
			Iterator <Vertex> Ves = vertices[i].getPrev().iterator();
			while(Ves.hasNext())
			{
				output = new StringBuilder();
				Vertex ves =Ves.next();
				output.append((i+1)+","+(ves.getId()+1));
				while(ves.getPrev()!=null)
				{
					output.append(","+(ves.getPrevious().getId()+1));
					ves = ves.getPrevious();
				}
				output.append("\n");
				//System.out.print(output.reverse().toString());
				results.append(output.toString());
			}
			
			}
			
		}
		return results;
	}
	
	
	
	
	String dijkstra(int graph[][], int graph_delay[][],int src,int V)
    {
    	//initialization for width and previous
		List<Vertex> prev = null;
        Vertex vertices [] = new Vertex [V];
        Queue<Integer> Q = new LinkedList<Integer>();
        int width[] = new int[V];
        int i = 0;
        System.out.println("111111");
        for(i=0;i<V;i++)
        {
        	
        	Vertex temp = new Vertex();
        	temp.setId(i);
        	vertices[i] = temp;
        	vertices[i].setwidth(-Integer.MAX_VALUE);
        	width[i] = -Integer.MAX_VALUE;
        	Q.add(i);
        }
		width[src]=Integer.MAX_VALUE;
		vertices[src].setwidth(Integer.MAX_VALUE);
		System.out.println("222222");
		while(!Q.isEmpty())
		{
			i = 0;
        	//clone a Queue Q_temp of Q to find the largest width in Q
        	Queue<Integer> Q_temp = new LinkedList<Integer>(Q);
        	int temp_max = -1;
        	int u = -1;
        	System.out.println("3333333");
        	while(!Q_temp.isEmpty())
        	{
        		int idx = Q_temp.poll();
        		//find the idx of the largest width in Q
        		if(width[idx]>temp_max)
        		{
        			temp_max = width[idx];
        			u = idx;
        		}
        	}
        	//remove the largest width from Q
        	Q.remove(u);
        	
        	Queue<Integer> Q_temp2 = new LinkedList<Integer>(Q);
        	int new_array [] = new int [V];
        	System.out.println("4444444");
        	while(!Q_temp2.isEmpty())
        	{
        		int temp_array_elem = Q_temp2.poll();
        		new_array[temp_array_elem] = 1;
        	}
        	if(width[u] == -Integer.MAX_VALUE) break;
		
		int v = 0;
		System.out.println("5555555");
		for(v=0;v<V;v++)
		{
			//if v is u's neighbor
			if(graph[u][v] != 0 && new_array[v]==1)
			{
				
				prev = vertices[v].getPrev();
				int alt = Math.max(width[vertices[v].getId()],Math.min(width[u],graph[u][vertices[v].getId()]));
				if(alt>width[vertices[v].getId()])
				{
					Q.remove(v);
					
					width[vertices[v].getId()] = alt;
					vertices[v].setPrevious(vertices[u]);
					prev = new ArrayList<Vertex>();
					prev.add(vertices[u]);
					vertices[v].setPrev(prev);
					Q.add(v);
				}
				else if(alt==width[vertices[v].getId()]&&graph[u][v]>=width[v])
				{
					if (prev != null)
					{
						prev.add(vertices[u]);
					}
					else {
						prev = new ArrayList<Vertex>();
						prev.add(vertices[u]);
						vertices[v].setPrev(prev);
					}
				}
			}
		}
		}
		StringBuilder widest_paths = new StringBuilder();
		//System.out.println("111111");
		widest_paths=Print_func(prev,vertices,src,V);
		//find the shortest path among all possible widest path
		//System.out.println("222222");
		return Find_shortest(widest_paths,graph_delay);
    }
}
