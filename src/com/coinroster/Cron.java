package com.coinroster;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;
import com.coinroster.internal.*;

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
	
	protected Cron(String freq, Calendar cal) throws Exception
		{
		year = cal.get(Calendar.YEAR);
		month = cal.get(Calendar.MONTH);
		day_of_month = cal.get(Calendar.DAY_OF_MONTH);
		day_of_week = cal.get(Calendar.DAY_OF_WEEK);
		hour = cal.get(Calendar.HOUR_OF_DAY);
		minute = cal.get(Calendar.MINUTE);
		second = cal.get(Calendar.SECOND);
		
		this.getClass().getDeclaredMethod(freq).invoke(this);
		}

	@SuppressWarnings("unused")
	private void second() throws Exception
		{ 
		
		}
	
	@SuppressWarnings("unused")
	private void minute() throws Exception
		{ 
		SessionExpiry();
		}
	
	@SuppressWarnings("unused")
	private void hour() throws Exception
		{
		new CloseContestRegistration();
		}
	
	@SuppressWarnings("unused")
	private void day() throws Exception
		{
		PurgePasswordResetTable();
		}

//------------------------------------------------------------------------------------

	// kick off stale sessions
	
	private void SessionExpiry()
		{
		try {
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
		catch (Exception e)
			{
			Server.exception(e);
			}
		
		}	
	
//------------------------------------------------------------------------------------

	// purge any password reset keys that have not been used
	
	private void PurgePasswordResetTable()
		{
		try {
			Long expiry_cutoff = System.currentTimeMillis() - Server.hour;
			
			Connection sql_connection = Server.sql_connection();
			
			PreparedStatement delete_password_reset = sql_connection.prepareStatement("delete from password_reset where created < ?");
			delete_password_reset.setLong(1, expiry_cutoff);
			delete_password_reset.executeUpdate();
			
			sql_connection.close();
			}
		catch (Exception e)
			{
			Server.exception(e);
			}
		}	
	
//------------------------------------------------------------------------------------

	}
