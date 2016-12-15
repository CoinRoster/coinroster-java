package com.coinroster.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

import com.coinroster.DB;
import com.coinroster.Server;

public class CloseContestRegistration 
	{
	public CloseContestRegistration() // called by hourly cron
		{
		Connection sql_connection = null;
		
		try {
			sql_connection = Server.sql_connection();
			
			DB db = new DB(sql_connection);
			
			PreparedStatement select_contest = sql_connection.prepareStatement("select id, registration_deadline, min_users, title from contest where status = 1");
			ResultSet contest = select_contest.executeQuery();
			
			// loop through all contests with open registration

			while (contest.next())
				{
				try {
					Long registration_deadline = contest.getLong(2);
					
					// process any contests where registration should be closed
					
					if (System.currentTimeMillis() >= registration_deadline)
						{
						int 
						
						contest_id = contest.getInt(1),
						min_users = contest.getInt(3);
						
						Server.log("Closing registration for contest " + contest_id);
						
						// first set contest to in-play to lock out additional registrations

						new UpdateContestStatus(sql_connection, contest_id, 2);
						
						// next loop through users to make sure contest is adequately subscribed
						
						PreparedStatement select_entry = sql_connection.prepareStatement("select user_id from entry where contest_id = ?");
						select_entry.setInt(1, contest_id);
						ResultSet entry = select_entry.executeQuery();
						
						// count number of users
						
						HashSet<String> users = new HashSet<String>();
						
						while (entry.next())
							{
							String user_id = entry.getString(1);
							if (!users.contains(user_id)) users.add(user_id);
							}
						
						String
						
						contest_title = contest.getString(4),
						
						subject = null,
						message_body = null;
												
						if (users.size() >= min_users) // contest is adequately subscribed
							{
							Server.log("Contest " + contest_id + " is in play");
							
							// we will notify users that contest is in play
							
							subject = "Contest " + contest_id + " is in play!";
							
							message_body = "Hello <b><!--USERNAME--></b>,";
							message_body += "<br/>";
							message_body += "<br/>";
							message_body += "Contest " + contest_id + " (" + contest_title + ") is in play!";
							message_body += "<br/>";
							message_body += "<br/>";
							message_body += "You will receive a notification when the contest has been settled.";
							}
						else // contest is under-subscribed
							{
							Server.log("Contest " + contest_id + " is under-subscribed");
							
							new ContestSettlement(sql_connection, contest_id, "UNDER-SUBSCRIBED");
							
							// we will notify users that contest has been closed
							
							subject = "Contest " + contest_id + " is under-subscribed";
							
							message_body = "Hello <b><!--USERNAME--></b>,";
							message_body += "<br/>";
							message_body += "<br/>";
							message_body += "Not enough users entered contest " + contest_id + " (" + contest_title + ").";
							message_body += "<br/>";
							message_body += "<br/>";
							message_body += "The contest has been cancelled and your entry fees have been credited back to your account.";
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
		finally
			{
			if (sql_connection != null)
				{
				try {sql_connection.close();} 
				catch (SQLException ignore) {}
				}
			}
		}
	}
