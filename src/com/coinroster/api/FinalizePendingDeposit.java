package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.UserMail;

public class FinalizePendingDeposit extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public FinalizePendingDeposit(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			String 
			
			user_account_id = input.getString("user_account_id"),
			memo = input.getString("memo");

			int transaction_id = input.getInt("transaction_id");
			
			double received_amount = input.getDouble("received_amount");

			// start using SQL | lock user_xref table so that balance lookups and adjustments are contiguous:
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write");

			JSONObject user = null;
					
			boolean 
			
			success = false,
			deposit_bonus_activated = false;
			
			try {
				lock : {
				
					user = db.select_user("id", user_account_id);

					// unlock and break method if user key is invalid:
					
					if (user == null) 
						{
						output.put("error", "Invalid user account");
						break lock;
						}
	
					JSONObject liability_account = db.select_user("username", "internal_liability");
					String btc_liability_id = liability_account.getString("user_id");
					
					// get account balances:
				  
					double
					
					btc_liability_balance = liability_account.getDouble("btc_balance"),
					user_btc_balance = user.getDouble("btc_balance");
	
					// add received amount to user balance:
					
					double new_btc_balance = add(user_btc_balance, received_amount, 0);
					
					db.update_btc_balance(user_account_id, new_btc_balance);
			
					// subtract received amount from internal_btc_liability:
					
					Double new_btc_liability_balance = subtract(btc_liability_balance, received_amount, 0);
					
					db.update_btc_balance(btc_liability_id, new_btc_liability_balance);
					
					// activate deposit bonus (if applicable)
					
					deposit_bonus_activated = db.enable_deposit_bonus(user, received_amount);

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
				PreparedStatement update_transaction = sql_connection.prepareStatement("update transaction set pending_flag = 0, amount = ?, memo = ? where id = ?");
				update_transaction.setDouble(1, received_amount);
				update_transaction.setString(2, memo);
				update_transaction.setInt(3, transaction_id);
				update_transaction.executeUpdate();
				
				// communications
				
				String
				
				subject = null,
				message_body = null;
				
				if (deposit_bonus_activated)
					{
					subject = "Claim your Deposit Bonus!";
					
					message_body = "Hi <b><!--USERNAME--></b>";
					message_body += "<br/>";
					message_body += "<br/>";
					message_body += "We have received your first deposit and have credited to your account.";
					message_body += "<br/>";
					message_body += "<br/>";
					message_body += "Transaction ID: <b>" + transaction_id + "</b>";
					message_body += "<br/>";
					message_body += "Amount received: <b>" + format_btc(received_amount) + " BTC</b>";
					message_body += "<br/>";
					message_body += "<br/>";
					message_body += "<a href='" + Server.host + "/account/deposit_bonus.html'>Click here</a> to claim your deposit bonus!";
					}
				else
					{
					subject = "Deposit confirmation";
					
					message_body = "Hi <b><!--USERNAME--></b>";
					message_body += "<br/>";
					message_body += "<br/>";
					message_body += "We have received your deposit and have credited to your account.";
					message_body += "<br/>";
					message_body += "<br/>";
					message_body += "Transaction ID: <b>" + transaction_id + "</b>";
					message_body += "<br/>";
					message_body += "Amount received: <b>" + format_btc(received_amount) + " BTC</b>";
					message_body += "<br/>";
					message_body += "<br/>";
					message_body += "You may view your account <a href='" + Server.host + "/account/'>here</a>.";
					}
				
				new UserMail(user, subject, message_body);
				
				output.put("status", "1");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}