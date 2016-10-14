package com.coinroster;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;

public class Cron 
	{
	@SuppressWarnings("unused")
	private static int
	
	year,
	month,
	day_of_month,
	day_of_week, 
	hour,
	minute,
	second;
	
	protected Cron(String freq) throws Exception
		{
		Calendar cal = Calendar.getInstance();
   
		year = cal.get(Calendar.YEAR);
		month = cal.get(Calendar.MONTH);
		day_of_month = cal.get(Calendar.DAY_OF_MONTH);
		day_of_week = cal.get(Calendar.DAY_OF_WEEK);
		hour = cal.get(Calendar.HOUR_OF_DAY);
		minute = cal.get(Calendar.MINUTE);
		second = cal.get(Calendar.SECOND);
		
		this.getClass().getDeclaredMethod("_" + freq).invoke(this);
		}
	
	@SuppressWarnings("unused")
	private void _minute() throws Exception
		{ 
		// automatic session expiry:

		for (String key : Server.session_map.keySet()) 
			{
		    String[] session_vars = Server.session_map.get(key);
		    
		    String
		    
		    /*username = session_vars[0],
		    user_id = session_vars[1],*/
		    user_level = session_vars[2];
		    
		    if (user_level.equals("1"))
		    	{
		    	Long last_active = Long.parseLong(session_vars[3]);
		    	if (System.currentTimeMillis() - last_active >= Server.admin_timeout) Server.session_map.remove(key);
		    	}
			}
		}
	
	@SuppressWarnings("unused")
	private void _hour() throws Exception
		{
		if (hour == 3)
			{
			// do backup
			}
		}
	
	@SuppressWarnings("unused")
	private void _day() throws Exception
		{
		// purge password_reset table:
		
		Long expiry_cutoff = System.currentTimeMillis() - Server.hour;
		
		Connection sql_connection = Server.sql_connection();
		
		PreparedStatement delete_password_reset = sql_connection.prepareStatement("delete from password_reset where created < ?");
		delete_password_reset.setLong(1, expiry_cutoff);
		delete_password_reset.executeUpdate();
		
        sql_connection.close();
		}
	}
