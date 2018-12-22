package com.coinroster;

import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * New Cron thread used for a CronWorker
 * 
 * This is where task timeouts happen if Cron workers fail to return by the timeout interval.
 */
public class CronThread extends Thread
	{
    private String freq = null;
    private Calendar cal = null;
    private int timeout = 1800; // seconds (30 min)
    
    /**
     * Creates a new thread instance.
     * 
     * @param freq One of: minute, hour, day
     * @param cal Calendar instance that represents the current time
     */
    public CronThread(String freq, Calendar cal) 
    	{
        this.freq = freq;
        this.cal = cal;
    	}
    
    /**
     * Spawns new worker with properties specified by thread instance.
     * 
     * @throws Exception 
     * @see com.coinroster.CronWorker
     */
    public void run() 
    	{
    	FutureTask<Integer> task = null;
		
		try {
			Callable<Integer> worker = new CronWorker(freq, cal);
			
			task = new FutureTask<Integer>(worker);
		    Server.cron_pool.execute(task);
		    
		    task.get(timeout, TimeUnit.SECONDS);
			}
		catch (TimeoutException e) // only fires if CronWorker hasn't yet returned
			{
			Utils.log("------------------------TASK TIMEOUT-------------------------");
			Utils.log("cron TASK to string: " + task.toString());
			task.cancel(true); // interrupt CronWorker task
			Server.exception(e);
			}
		catch (Exception e) 
			{
			Server.exception(e);
			}
    	}
    
    }