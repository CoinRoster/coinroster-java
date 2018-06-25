package com.coinroster;

import java.util.Calendar;

public class CronDriver
	{
	private static int
	
	last_day = -1,
	last_hour = -1,
	last_minute = -1;
	
	protected static void start() 
		{
		new Thread() 
			{
			public void run() 
				{
				while (Server.listening)
					{
					try {
						Calendar cal = Calendar.getInstance();
						
						int 
						
		    		    day = cal.get(Calendar.DAY_OF_WEEK),
		    		    hour = cal.get(Calendar.HOUR_OF_DAY),
		    		    minute = cal.get(Calendar.MINUTE);
						
						if (minute != last_minute)
							{
							new CronThread("minute", cal).start();
							last_minute = minute;
							}
						
						if (hour != last_hour)
							{
							new CronThread("hour", cal).start();
							last_hour = hour;
							}
						
						if (day != last_day)
							{
							new CronThread("day", cal).start();
							last_day = day;
							}
						
						Thread.sleep(1000);
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