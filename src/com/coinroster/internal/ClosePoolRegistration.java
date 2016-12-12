package com.coinroster.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

import com.coinroster.Server;

public class ClosePoolRegistration 
	{
	Connection sql_connection;
	
	public ClosePoolRegistration()
		{
		try {
			sql_connection = Server.sql_connection();
			
			PreparedStatement select_pool = sql_connection.prepareStatement("select id, registration_deadline, min_users from pool where status = 1");
			ResultSet pool = select_pool.executeQuery();

			while (pool.next())
				{
				Long registration_deadline = pool.getLong(2);
				
				if (System.currentTimeMillis() >= registration_deadline)
					{
					int 
					
					pool_id = pool.getInt(1),
					min_users = pool.getInt(3);
					
					Server.log("Closing registration for pool [" + pool_id + "]");
					
					// first set pool to in-play to lock out additional registrations:

					update_pool_status(pool_id, 2);
					
					// next loop through users to make sure pool is adequately subscribed:
					
					PreparedStatement select_entry = sql_connection.prepareStatement("select user_id from entry where pool_id = ?");
					select_entry.setInt(1, pool_id);
					ResultSet entry = select_entry.executeQuery();
					
					HashSet<String> users = new HashSet<String>();
					
					while (entry.next())
						{
						String user_id = entry.getString(1);
						if (!users.contains(user_id)) users.add(user_id);
						}
					
					if (users.size() < min_users) // pool is under-subscribed; unwind
						{
						Server.log("Pool [" + pool_id + "] is under-subscribed");
						update_pool_status(pool_id, 4);
						
						// credit any entry fees to registered users, send email to registered users
						}
					else
						{
						// notify users that contest is active
						}
					}
				}
			}
		catch (Exception e)
			{
			Server.exception(e);
			}
		}
	
	private void update_pool_status(int pool_id, int status) throws SQLException
		{
		PreparedStatement update_pool_status = sql_connection.prepareStatement("update pool set status = ? where id = ?");
		update_pool_status.setInt(1, status);
		update_pool_status.setInt(2, pool_id);
		update_pool_status.executeUpdate();
		}
	}
