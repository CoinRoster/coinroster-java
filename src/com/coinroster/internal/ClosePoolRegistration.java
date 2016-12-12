package com.coinroster.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;

import com.coinroster.DB;
import com.coinroster.Server;

public class ClosePoolRegistration 
	{
	Connection sql_connection;
	
	public ClosePoolRegistration() // called by hourly cron
		{
		try {
			sql_connection = Server.sql_connection();
			
			DB db = new DB(sql_connection);
			
			PreparedStatement select_pool = sql_connection.prepareStatement("select id, registration_deadline, min_users, title from pool where status = 1");
			ResultSet pool = select_pool.executeQuery();
			
			// loop through all pools with open registration

			while (pool.next())
				{
				try {
					Long registration_deadline = pool.getLong(2);
					
					// process any pools where registration should be closed
					
					if (System.currentTimeMillis() >= registration_deadline)
						{
						int 
						
						pool_id = pool.getInt(1),
						min_users = pool.getInt(3);
						
						Server.log("Closing registration for pool " + pool_id);
						
						// first set pool to in-play to lock out additional registrations

						new UpdatePoolStatus(sql_connection, pool_id, 2);
						
						// next loop through users to make sure pool is adequately subscribed
						
						PreparedStatement select_entry = sql_connection.prepareStatement("select user_id from entry where pool_id = ?");
						select_entry.setInt(1, pool_id);
						ResultSet entry = select_entry.executeQuery();
						
						// count number of users
						
						HashSet<String> users = new HashSet<String>();
						
						while (entry.next())
							{
							String user_id = entry.getString(1);
							if (!users.contains(user_id)) users.add(user_id);
							}
						
						String
						
						pool_title = pool.getString(4),
						
						subject = null,
						message_body = null;
												
						if (users.size() >= min_users) // pool is adequately subscribed
							{
							Server.log("Pool " + pool_id + " is in play");
							
							// we will notify users that pool is in play
							
							subject = "Pool " + pool_id + " is in play!";
							
							message_body = "Hello <b><!--USERNAME--></b>,";
							message_body += "<br/>";
							message_body += "<br/>";
							message_body += "Pool " + pool_id + " (" + pool_title + ") is in play!";
							message_body += "<br/>";
							message_body += "<br/>";
							message_body += "You will receive a notification when the pool has been settled.";
							}
						else // pool is under-subscribed
							{
							Server.log("Pool " + pool_id + " is under-subscribed");
							
							new PoolSettlement(sql_connection, pool_id, "UNDER-SUBSCRIBED");
							
							// we will notify users that pool has been closed
							
							subject = "Pool " + pool_id + " is under-subscribed";
							
							message_body = "Hello <b><!--USERNAME--></b>,";
							message_body += "<br/>";
							message_body += "<br/>";
							message_body += "Not enough users entered pool " + pool_id + " (" + pool_title + ").";
							message_body += "<br/>";
							message_body += "<br/>";
							message_body += "The pool has been cancelled and your entry fees have been credited back to your account.";
							}
						
						// send status emails to users
						
						for (String user_id : users) new UserMail(db.select_user("id", user_id), subject, message_body);
						}
					}
				catch (Exception e1)
					{
					Server.exception(e1);
					}
				}
			}
		catch (Exception e)
			{
			Server.exception(e);
			}
		}
	}
