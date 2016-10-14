package com.coinroster;

public class IterativeLoops
	{
	protected static void start() 
		{
		// 1-minute loop 
			
		new Thread() 
			{
			public void run() 
				{
				while (Server.listening)
					{
					try {
						Thread.sleep(Server.minute);
						new Cron("minute");
						}
					catch (Exception e) 
						{
						Server.exception(e);
						}
					}
				}
			}.start();	

		// 1-minute loop 
			
		new Thread() 
			{
			public void run() 
				{
				while (Server.listening)
					{
					try {
						Thread.sleep(Server.hour);
						new Cron("hour");
						}
					catch (Exception e) 
						{
						Server.exception(e);
						}
					}
				}
			}.start();	
		
		// 1-day loop 
			
		new Thread() 
			{
			public void run() 
				{
				while (Server.listening)
					{
					try {
						Thread.sleep(Server.day);
						new Cron("day");
						}
					catch (Exception e) 
						{
						Server.exception(e);
						}
					}
				}
			}.start();
		}
	}