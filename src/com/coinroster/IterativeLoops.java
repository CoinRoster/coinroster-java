package com.coinroster;

import java.util.Calendar;

public class IterativeLoops
	{
	private static int
	
	last_day = -1,
	last_hour = -1,
	last_minute = -1,
	last_second = -1;
	
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
		    		    minute = cal.get(Calendar.MINUTE),
		    		    second = cal.get(Calendar.SECOND);
					
						try {
							if (second != last_second) new Cron("second", cal);
							}
						catch (Exception e)
							{
							Server.exception(e);
							}
						try {
							if (minute != last_minute) new Cron("minute", cal);
							}
						catch (Exception e)
							{
							Server.exception(e);
							}
						try {
							if (hour != last_hour) new Cron("hour", cal);
							}
						catch (Exception e)
							{
							Server.exception(e);
							}
						try {
							if (day != last_day) new Cron("day", cal);
							}
						catch (Exception e)
							{
							Server.exception(e);
							}
						
						last_day = day;
						last_hour = hour;
						last_minute = minute;
						last_second = second;
						
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