package com.coinroster;

import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CronThread extends Thread
	{
    private String freq = null;
    private Calendar cal = null;
    private int timeout = 1800; // seconds (30 min)
    
    public CronThread(String freq, Calendar cal) 
    	{
        this.freq = freq;
        this.cal = cal;
    	}
    
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
			task.cancel(true); // interrupt CronWorker task
			Utils.log("--- TASK TIMEOUT ------------------------------------------------");
			Utils.log(e.getStackTrace().toString());
			}
		catch (Exception e) 
			{
			Server.exception(e);
			}
    	}
    
    }