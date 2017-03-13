package com.coinroster.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.coinroster.Server;
import com.coinroster.Utils;

public class UpdateUserContestStatus extends Utils
	{
	public UpdateUserContestStatus(final String user_id, final int contest_status)
		{
		Server.async_updater.execute(new Runnable() 
			{
		    public void run() 
		    	{
		    	Connection sql_connection = null;
				try {
					sql_connection = Server.sql_connection();
					PreparedStatement update_last_active = sql_connection.prepareStatement("update user set contest_status = ? where id = ?");
					update_last_active.setInt(1, contest_status);
					update_last_active.setString(2, user_id);
					update_last_active.executeUpdate();
					} 
				catch (Exception e) 
					{
					Server.exception(e);
					} 
				finally
					{
					if (sql_connection != null)
						{
						try {sql_connection.close();} 
						catch (SQLException ignore) {}
						}
					}
		    	}
			});
		}
	}
