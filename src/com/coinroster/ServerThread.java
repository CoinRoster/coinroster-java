package com.coinroster;

import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ServerThread extends Thread
	{
    private Socket socket = null;
    
    public ServerThread(Socket socket) 
    	{
        this.socket = socket;
    	}

    public void run() 
    	{
    	FutureTask<Integer> task = null;
		
		try {
			Callable<Integer> worker = new ServerWorker(socket);
			
			task = new FutureTask<Integer>(worker);
			
		    Server.worker_pool.execute(task);
		    task.get(30, TimeUnit.SECONDS);
			}
		catch (TimeoutException e) // only fires if ServerWorker hasn't yet returned
			{
			task.cancel(true); // interrupt ServerWorker task
			Utils.log("server TASK to string: " + task.toString());
			Utils.log("--------------------TASK TIMEOUT--------------------------");
			Server.exception(e);
			}
		catch (Exception e) 
			{    
			Server.exception(e);
			}
    	}
    
    }