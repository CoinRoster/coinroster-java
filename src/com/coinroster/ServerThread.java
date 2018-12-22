package com.coinroster;

import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * New Server thread used for a server thread
 * 
 * This is where task timeouts happen if workers fail to return by the timeout interval.
 * (this would be a lot more serious than a CronWorker failure)
 */
public class ServerThread extends Thread
	{
    private Socket socket = null;
    
    /**
     * Initialize server thread.
     * @param socket
     */
    public ServerThread(Socket socket) 
    	{
        this.socket = socket;
    	}

    /**
     * Spawns new Server Worker.
     */
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