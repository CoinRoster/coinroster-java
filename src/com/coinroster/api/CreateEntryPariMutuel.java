package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.UpdateUserContestStatus;
import com.coinroster.internal.UserMail;

public class CreateEntryPariMutuel extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public CreateEntryPariMutuel(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			int contest_id = input.getInt("contest_id");
			boolean use_rc = input.getBoolean("use_rc");
			JSONArray wagers = input.getJSONArray("wagers");
			
			int number_of_user_wagers = wagers.length();
			
			double
			
			rc_transaction_amount = 0,
			btc_transaction_amount = 0;
			
			String 

			user_id = session.user_id(),
			created_by = user_id,
			contest_account_id = null,
			contest_title = null;
			
			// lock it all
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write, contest write, entry write, transaction write");

			JSONObject user = null;
			
			boolean success = false;
						
			try {
				lock : {
				
					// make sure contest id is valid
				
					JSONObject contest = db.select_contest(contest_id);
					
					if (contest == null)
						{
						output.put("error", "Invalid contest ID: " + contest_id);
						break lock;
						}
					
					contest_title = contest.getString("title");
					
					// make sure contest is open for registration
					
					if (contest.getInt("status") != 1)
						{
						output.put("error", "Contest #" + contest_id + " is not open for registration");
						break lock;
						}
					
					// validate wagers
					
					JSONArray option_table = new JSONArray(contest.getString("option_table"));
					
					int number_of_options = option_table.length();
					
					double 
					
					cost_per_entry = contest.getDouble("cost_per_entry"),
					total_entry_fees = 0;
					
					for (int i=0; i<number_of_user_wagers; i++)
						{
						JSONObject wager_item = wagers.getJSONObject(i);
						
						int option_id = wager_item.getInt("id");
						double wager = wager_item.getDouble("wager");
						
						if (option_id > number_of_options)
							{
							output.put("error", "Invalid outcome id: " + option_id);
							break lock;
							}
						if (wager < cost_per_entry)
							{
							output.put("error", "Wager on outcome " + option_id + " is less than the min wager");
							break lock;
							}
						
						total_entry_fees = add(total_entry_fees, wager, 0);
						}
					
					// make sure user can afford entr(ies)
	
					user = db.select_user("id", user_id);
					
					double 
					
					btc_balance = user.getDouble("btc_balance"),
					rc_balance = user.getDouble("rc_balance"),
					available_balance = btc_balance;
									
					if (use_rc) available_balance = add(available_balance, rc_balance, 0);
					
					if (total_entry_fees > available_balance)
						{
						output.put("error", "Insufficient funds");
						break lock;
						}
					
					// --------------------------------------------- //
					// if we get here, the entry has been validated! //
					// --------------------------------------------- //
	
					// calculate user balances and transaction amounts
	
					if (use_rc && rc_balance > 0)
						{
						double temp_rc_balance = subtract(rc_balance, total_entry_fees, 0);
	
						// user has enough RC to cover all entry fees:
						
						if (temp_rc_balance >= 0)
							{
							rc_transaction_amount = total_entry_fees;
							rc_balance = temp_rc_balance;
							}
						
						// user does not have enough RC to cover all entry fees:
						
						else
							{
							// entire RC balance being spent:
							
							rc_transaction_amount = rc_balance;
							rc_balance = 0;
							
							// overflow is removed from BTC balance:
							
							btc_transaction_amount = Math.abs(temp_rc_balance);
							btc_balance = subtract(btc_balance, btc_transaction_amount, 0);
							}
						}
					else 
						{
						btc_balance = subtract(btc_balance, total_entry_fees, 0);
						btc_transaction_amount = total_entry_fees;
						}
					
					// update user's account balances
					
					db.update_btc_balance(user_id, btc_balance);
					db.update_rc_balance(user_id, rc_balance);
	
					JSONObject contest_account = db.select_user("username", "internal_contest_asset");
					
					contest_account_id = contest_account.getString("user_id");
	
					// update RC contest asset account (if applicable)
					
					if (rc_transaction_amount > 0)
						{
						// update account balance:
	
						double rc_contest_account_balance = contest_account.getDouble("rc_balance");
						rc_contest_account_balance = add(rc_contest_account_balance, rc_transaction_amount, 0);

						db.update_rc_balance(contest_account_id, rc_contest_account_balance);
						}
					
					// update BTC contest asset account (if applicable)
					
					if (btc_transaction_amount > 0)
						{
						// update account balance:
	
						double btc_contest_account_balance = contest_account.getDouble("btc_balance");
						btc_contest_account_balance = add (btc_contest_account_balance, btc_transaction_amount, 0);
						
						db.update_btc_balance(contest_account_id, btc_contest_account_balance);
						}
					
					// create entries (one per wager)
					
					for (int i=0; i<wagers.length(); i++)
						{
						JSONObject wager_item = wagers.getJSONObject(i);
						
						int option_id = wager_item.getInt("id");
						double wager = wager_item.getDouble("wager");
					
						PreparedStatement create_entry = sql_connection.prepareStatement("insert into entry(contest_id, user_id, created, amount, entry_data) values(?, ?, ?, ?, ?)");	
						create_entry.setInt(1, contest_id);
						create_entry.setString(2, created_by);			
						create_entry.setLong(3, System.currentTimeMillis());
						create_entry.setDouble(4, wager);
						create_entry.setString(5, Integer.toString(option_id));
						create_entry.executeUpdate();
						}
					
					success = true;
					}
				}
			catch (Exception e)
				{
				Server.exception(e);
				}
			finally
				{
				statement.execute("unlock tables");
				}
		
			if (success)
				{
				// create transaction for RC transfer:
				
				if (rc_transaction_amount > 0)
					{
					String 
					
					transaction_type = "RC-CONTEST-ENTRY",
					from_account = user_id,
					to_account = contest_account_id,
					from_currency = "RC",
					to_currency = "RC",
					memo = "Entry fees (RC) for contest #" + contest_id;
					
					PreparedStatement rc_contest_entry = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
					rc_contest_entry.setLong(1, System.currentTimeMillis());
					rc_contest_entry.setString(2, created_by);
					rc_contest_entry.setString(3, transaction_type);
					rc_contest_entry.setString(4, from_account);
					rc_contest_entry.setString(5, to_account);
					rc_contest_entry.setDouble(6, rc_transaction_amount);
					rc_contest_entry.setString(7, from_currency);
					rc_contest_entry.setString(8, to_currency);
					rc_contest_entry.setString(9, memo);
					rc_contest_entry.setInt(10, contest_id);
					rc_contest_entry.executeUpdate();
					}
				
				// create transaction for BTC transfer:
				
				if (btc_transaction_amount > 0)
					{
					String 
					
					transaction_type = "BTC-CONTEST-ENTRY",
					from_account = user_id,
					to_account = contest_account_id,
					from_currency = "BTC",
					to_currency = "BTC",
					memo = "Entry fees (BTC) for contest #" + contest_id;
					
					PreparedStatement btc_contest_entry = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
					btc_contest_entry.setLong(1, System.currentTimeMillis());
					btc_contest_entry.setString(2, created_by);
					btc_contest_entry.setString(3, transaction_type);
					btc_contest_entry.setString(4, from_account);
					btc_contest_entry.setString(5, to_account);
					btc_contest_entry.setDouble(6, btc_transaction_amount);
					btc_contest_entry.setString(7, from_currency);
					btc_contest_entry.setString(8, to_currency);
					btc_contest_entry.setString(9, memo);
					btc_contest_entry.setInt(10, contest_id);
					btc_contest_entry.executeUpdate();
					}
				
				// send email confirmation
				
				String
				
				subject = "Entry confirmation for contest #" + contest_id, 
				message_body = "";
				
				message_body += "You have successfully entered <b>" + number_of_user_wagers + " wager" + (number_of_user_wagers > 1 ? "s" : "") + "</b> for contest #" + contest_id + " - <b>" + contest_title + "</b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "You may view your wagers <a href='" + Server.host + "/contests/entries.html?contest_id=" + contest_id + "'>here</a>.";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Please do not reply to this email.";
				
				new UserMail(user, subject, message_body);

				new UpdateUserContestStatus(user_id, 1); // front end will redirect to My Contests -> we need to show the Open tab
				
				output.put("status", "1");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}