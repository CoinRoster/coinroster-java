package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.UserMail;

public class FinalizeQuickbtDeposit extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public FinalizeQuickbtDeposit(MethodInstance method) throws Exception 
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
			
			int quickbt_record_id = input.getInt("quickbt_record_id");
			
			double received_amount = input.getDouble("received_amount");

			// start using SQL | lock user_xref table so that balance lookups and adjustments are contiguous:
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write");

			JSONObject user = null;

			String btc_liability_id = null;
			
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
					btc_liability_id = liability_account.getString("user_id");
					
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
				PreparedStatement update_quickbt_record = sql_connection.prepareStatement("update quickbt set pending_flag = 0 where trans_id = ?");
				update_quickbt_record.setInt(1, quickbt_record_id);
				update_quickbt_record.executeUpdate();
				
				Long transaction_timestamp = System.currentTimeMillis();
				
				String
				
				created_by = session.user_id(),
				transaction_type = "BTC-DEPOSIT",
				from_account = btc_liability_id,
				to_account = user_account_id,
				from_currency = "BTC",
				to_currency = "BTC";
				
				PreparedStatement new_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo) values(?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);				
				new_transaction.setLong(1, transaction_timestamp);
				new_transaction.setString(2, created_by);
				new_transaction.setString(3, transaction_type);
				new_transaction.setString(4, from_account);
				new_transaction.setString(5, to_account);
				new_transaction.setDouble(6, received_amount);
				new_transaction.setString(7, from_currency);
				new_transaction.setString(8, to_currency);
				new_transaction.setString(9, memo);
				new_transaction.executeUpdate();
				
				ResultSet rs = new_transaction.getGeneratedKeys();
			    rs.next();
			    int transaction_id = rs.getInt(1);
				
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