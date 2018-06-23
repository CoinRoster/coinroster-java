package com.coinroster.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.Server;
import com.coinroster.Utils;

public class CloseContestRegistration extends Utils
	{
	public CloseContestRegistration() // called by hourly cron
		{
		Connection sql_connection = null;
		
		try {
			sql_connection = Server.sql_connection();
			
			DB db = new DB(sql_connection);
			
			PreparedStatement select_contest = sql_connection.prepareStatement("select id from contest where status = 1");
			ResultSet result_set = select_contest.executeQuery();
			
			// loop through all contests with open registration

			while (result_set.next())
				{
				try {
					int contest_id = result_set.getInt(1);
					
					JSONObject contest = db.select_contest(contest_id);
					
					Long registration_deadline = contest.getLong("registration_deadline");
					
					// process any contests where registration should be closed
					
					if (System.currentTimeMillis() >= registration_deadline)
						{
						Server.log("Closing registration for contest #" + contest_id);
						
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
						
						contest_title = contest.getString("title"),
						contest_type = contest.getString("contest_type"),
						
						subject = "",
						message_body = "";

						int min_users = contest.getInt("min_users");
										
						if (users.size() >= min_users && contest.getString("settlement_type").equals("CROWD-SETTLED")) // contest is adequately subscribed
							{
							Server.log("Contest #" + contest_id + " is being crowd-settled");
							
							/* VOTING ROUND CREATION */
							
			            	PreparedStatement create_contest = sql_connection.prepareStatement("insert into contest(category, sub_category, created, contest_type, title, description, registration_deadline, rake, cost_per_entry, settlement_type, option_table, created_by, auto_settle, status) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
							create_contest.setString(1, contest.getString("category"));
							create_contest.setString(2, contest.getString("sub_category"));
							create_contest.setLong(3, System.currentTimeMillis());
							//create_contest.setString(3, contest.getString("progressive_code"));
							create_contest.setString(4, contest.getString("contest_type"));
							create_contest.setString(5, "VOTING ROUND: " + contest.getString("title"));
							create_contest.setString(6, contest.getString("description"));
							create_contest.setLong(7, contest.getLong("settlement_deadline"));
							create_contest.setDouble(8, contest.getDouble("rake"));
							create_contest.setDouble(9, contest.getDouble("cost_per_entry"));
							create_contest.setString(10, contest.getString("settlement_type"));
							create_contest.setString(11, contest.getString("option_table"));
							create_contest.setString(12, "Crowd Contest: " + contest_id);
							create_contest.setInt(13, 0);
							create_contest.setInt(14, 1);
							create_contest.executeUpdate();
							
							new BuildLobby(sql_connection);
							
							// notify users that contest is in play
							
							subject = contest_title + " is now in play!";
							
							message_body  = "Hi <b><!--USERNAME--></b>";
							message_body += "<br/>";
							message_body += "<br/>";
							message_body += "<b>" + contest_title + "</b> is now being Crowd settled! <a href='" + Server.host + "/login.html>Log in now</a> and cast your bet on what you think is the correct outcome.";
							message_body += "<br/>";
							message_body += "<br/>";
							message_body += "<a href='" + Server.host + "/contests/entries.html?contest_id=" + contest_id + "'>Click here</a> to view your wagers.";
							}										
						else if (users.size() >= min_users) // contest is adequately subscribed
							{
							Server.log("Contest #" + contest_id + " is in play");
							
							// notify users that contest is in play
							
							subject = contest_title + " is now in play!";
							
							message_body  = "Hi <b><!--USERNAME--></b>";
							message_body += "<br/>";
							message_body += "<br/>";
							message_body += "<b>" + contest_title + "</b> is now in play!";
							message_body += "<br/>";
							message_body += "<br/>";
							
							if (contest_type.equals("ROSTER")) message_body += "<a href='" + Server.host + "/rosters.html?contest_id=" + contest_id + "'>Click here</a> to view the current Leaderboard";
							else if (contest_type.equals("PARI-MUTUEL")) message_body += "<a href='" + Server.host + "/contests/entries.html?contest_id=" + contest_id + "'>Click here</a> to view your wagers.";
							}
						else // contest is under-subscribed
							{
							Server.log("Contest #" + contest_id + " is under-subscribed");

							new UpdateContestStatus(sql_connection, contest_id, 4);
							
							Statement statement = sql_connection.createStatement();
							statement.execute("lock tables user write, contest write, entry write, transaction write");

							try {
								JSONObject contest_account = db.select_user("username", "internal_contest_asset");
								
								String contest_account_id = contest_account.getString("user_id");
								
								double
								
								contest_account_btc_balance = contest_account.getDouble("btc_balance"),
								contest_account_rc_balance = contest_account.getDouble("rc_balance");
								
								PreparedStatement select_transactions = sql_connection.prepareStatement("select * from transaction where contest_id = ?");
								select_transactions.setInt(1, contest_id);
								ResultSet transaction = select_transactions.executeQuery();
								
								// loop through all contests with open registration

								while (transaction.next())
									{
									String trans_type = transaction.getString(4);
									String from_account = transaction.getString(5);
									String to_account = transaction.getString(6);
									double amount = transaction.getDouble(7);
									String from_currency = transaction.getString(8);
									String to_currency = transaction.getString(9);
									
									String user_id = from_account; // transaction was created by a user - we need to key off their user_id
									from_account = contest_account_id; // now we invert the transaction (to_account is assigned in loop)

									String created_by = contest_account_id;
									String memo = "Not enough users entered: " + contest_title;
									
									if (trans_type.endsWith("CONTEST-ENTRY"))
										{
										// build up unique list of users for communications
										
										if (!users.contains(user_id)) users.add(user_id);
										
										JSONObject user = db.select_user("id", user_id);

										// update balance
										
										if (from_currency.equals("BTC")) 
											{
											double user_btc_balance = user.getDouble("btc_balance");
											
											user_btc_balance = add(user_btc_balance, amount, 0); // credit user
											contest_account_btc_balance = subtract(contest_account_btc_balance, amount, 0); // debit contest account
											
											db.update_btc_balance(user_id, user_btc_balance);
											}
										else if (from_currency.equals("RC")) 
											{
											double user_rc_balance = user.getDouble("rc_balance");
											
											user_rc_balance = add(user_rc_balance, amount, 0); // credit user
											contest_account_rc_balance = subtract(contest_account_rc_balance, amount, 0); // debit contest account
											
											db.update_rc_balance(user_id, user_rc_balance);
											}

										// create reversal transaction

										trans_type = from_currency + "-CONTEST-ENTRY-REVERSAL";
										to_account = user_id;
										
										PreparedStatement create_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
										create_transaction.setLong(1, System.currentTimeMillis());
										create_transaction.setString(2, created_by);
										create_transaction.setString(3, trans_type);
										create_transaction.setString(4, from_account);
										create_transaction.setString(5, to_account);
										create_transaction.setDouble(6, amount);
										create_transaction.setString(7, from_currency);
										create_transaction.setString(8, to_currency);
										create_transaction.setString(9, memo);
										create_transaction.setInt(10, contest_id);
										create_transaction.executeUpdate();
										}
									}
								
								db.update_btc_balance(contest_account_id, contest_account_btc_balance);
								db.update_rc_balance(contest_account_id, contest_account_rc_balance);
								}
							catch (Exception e)
								{
								Server.exception(e);
								}
							finally
								{
								statement.execute("unlock tables");
								}
							
							subject = contest_title + " has been cancelled";
							
							message_body  = "Hi <b><!--USERNAME--></b>";
							message_body += "<br/>";
							message_body += "<br/>";
							message_body += "Not enough users entered: <b>" + contest_title + "</b>";
							message_body += "<br/>";
							message_body += "<br/>";
							message_body += "The contest has been cancelled and your entry fees have been credited back to your account.";
							}
						
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
