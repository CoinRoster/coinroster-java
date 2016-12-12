package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.UserMail;

public class CreateTransaction extends Utils
	{
	public static String method_level = "admin";
	public CreateTransaction(MethodInstance method) throws Exception 
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
			 
			created_by = session.user_id(),
			transaction_type = input.getString("transaction_type"),

			btc_liability_id = db.get_id_for_username("internal_btc_liability"),
			rc_liability_id = db.get_id_for_username("internal_rc_liability"),
			user_id = no_whitespace(input.getString("user_account")),
			
			from_account = "",
			to_account = "",
			from_currency = "",
			to_currency = "",
			
			memo = input.getString("memo");

			// initial validation:

			if (user_id.length() != 40) break method;
			if (memo.length() > 60) break method;
	 
			Double transaction_amount = null;
			
			try {
				transaction_amount = input.getDouble("amount");
				}
			catch (NumberFormatException e)
				{
				output.put("error", "Number Format Exception for amount");
				break method;
				}
	  
			// start using SQL | lock user table so that balance lookups and adjustments are contiguous:
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write");

			// get records for internal accounts and user account:
			
			JSONObject
			
			btc_liability = db.select_user("id", btc_liability_id),
			rc_liability = db.select_user("id", rc_liability_id),
			user = db.select_user("id", user_id);

			boolean transaction_ok = false;
		
			lock : {

				if (user == null) 
					{
					output.put("error", "Invalid user account");
					break lock;
					}
	
				// get account balances:
			  
				double
				
				btc_liability_balance = btc_liability.getDouble("btc_balance"),
				rc_liability_balance = rc_liability.getDouble("rc_balance"),
	
				user_btc_balance = user.getDouble("btc_balance"),
				user_rc_balance = user.getDouble("rc_balance");
				
				// transaction-specific logic:
				
				switch (transaction_type)
					{
					case "BTC-DEPOSIT":
						{
						// --- FROM --- internal_btc_liability
				
						from_account = btc_liability_id;
						from_currency = "BTC";
						
						// deduct transaction amount from internal_btc_liability:
						
						Double new_btc_liability_balance = btc_liability_balance - transaction_amount;
	
						PreparedStatement update_btc_liability_balance = sql_connection.prepareStatement("update user set btc_balance = ? where id = ?");
						update_btc_liability_balance.setDouble(1, new_btc_liability_balance);
						update_btc_liability_balance.setString(2, btc_liability_id);
						update_btc_liability_balance.executeUpdate();
	
						// --- TO --- user
						
						to_account = user_id;
						to_currency = "BTC";
						
						// add transaction amount to user:
						
						Double new_user_btc_balance = user_btc_balance + transaction_amount;
			 
						PreparedStatement update_user_balance = sql_connection.prepareStatement("update user set btc_balance = ? where id = ?");
						update_user_balance.setDouble(1, new_user_btc_balance);
						update_user_balance.setString(2, user_id);
						update_user_balance.executeUpdate();
						
						break;
						}
					case "BTC-WITHDRAWAL":
						{
						if (transaction_amount > user_btc_balance) 
							{
							output.put("error", "User has insufficient funds");
							break lock;
							}
						
						// --- FROM --- user
			
						from_account = user_id;
						from_currency = "BTC";
						
						// deduct transaction amount from user:
						
						Double new_user_btc_balance = user_btc_balance - transaction_amount;
	
						PreparedStatement update_btc_liability_balance = sql_connection.prepareStatement("update user set btc_balance = ? where id = ?");
						update_btc_liability_balance.setDouble(1, new_user_btc_balance);
						update_btc_liability_balance.setString(2, user_id);
						update_btc_liability_balance.executeUpdate();
	
						// --- TO --- internal_btc_liability
	
						to_account = btc_liability_id;
						to_currency = "BTC";
	
						// add transaction amount to internal_btc_liability:
						
						Double new_btc_liability_balance = btc_liability_balance + transaction_amount;
						
						PreparedStatement update_user_balance = sql_connection.prepareStatement("update user set btc_balance = ? where id = ?");
						update_user_balance.setDouble(1, new_btc_liability_balance);
						update_user_balance.setString(2, btc_liability_id);
						update_user_balance.executeUpdate();
						
						break;
						}
					case "RC-DEPOSIT":
						{
						// --- FROM --- internal_rc_liability
						
						from_account = rc_liability_id;
						from_currency = "RC";
						
						// deduct transaction amount from internal_rc_liability:
						
						Double new_rc_liability_balance = rc_liability_balance - transaction_amount;
	
						PreparedStatement update_rc_liability_balance = sql_connection.prepareStatement("update user set rc_balance = ? where id = ?");
						update_rc_liability_balance.setDouble(1, new_rc_liability_balance);
						update_rc_liability_balance.setString(2, rc_liability_id);
						update_rc_liability_balance.executeUpdate();
	
						// --- TO --- user
						
						to_account = user_id;
						to_currency = "RC";
						
						// add transaction amount to user:
						
						Double new_user_rc_balance = user_rc_balance + transaction_amount;
			 
						PreparedStatement update_user_balance = sql_connection.prepareStatement("update user set rc_balance = ? where id = ?");
						update_user_balance.setDouble(1, new_user_rc_balance);
						update_user_balance.setString(2, user_id);
						update_user_balance.executeUpdate();
						
						break;
						}
					case "RC-WITHDRAWAL":
						{
						if (transaction_amount > user_rc_balance)
							{
							output.put("error", "User has insufficient funds");
							break lock;
							}
						
						// --- FROM --- user
			
						from_account = user_id;
						from_currency = "RC";
						
						// deduct transaction amount from user:
						
						Double new_user_rc_balance = user_rc_balance - transaction_amount;
	
						PreparedStatement update_rc_liability_balance = sql_connection.prepareStatement("update user set rc_balance = ? where id = ?");
						update_rc_liability_balance.setDouble(1, new_user_rc_balance);
						update_rc_liability_balance.setString(2, user_id);
						update_rc_liability_balance.executeUpdate();
	
						// --- TO --- internal_rc_liability
	
						to_account = rc_liability_id;
						to_currency = "RC";
	
						// add transaction amount to internal_rc_liability:
						
						Double new_rc_liability_balance = rc_liability_balance + transaction_amount;
						
						PreparedStatement update_user_balance = sql_connection.prepareStatement("update user set rc_balance = ? where id = ?");
						update_user_balance.setDouble(1, new_rc_liability_balance);
						update_user_balance.setString(2, rc_liability_id);
						update_user_balance.executeUpdate();
						
						break;
						}
					default: 
						{
						output.put("error", "Invalid transaction");
						break lock;
						}
					}
				
				// we only get here if none of the 'break transaction' conditions are triggered
				
				transaction_ok = true;
				}
			
			// immediately after transaction block completes, unlock user table

			statement.execute("unlock tables");
			
			// if transaction completed successfully, record it and send emails
			
			if (transaction_ok)
				{				
				Long transaction_timestamp = System.currentTimeMillis();
				
				PreparedStatement new_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo) values(?, ?, ?, ?, ?, ?, ?, ?, ?)");				
				new_transaction.setLong(1, transaction_timestamp);
				new_transaction.setString(2, created_by);
				new_transaction.setString(3, transaction_type);
				new_transaction.setString(4, from_account);
				new_transaction.setString(5, to_account);
				new_transaction.setDouble(6, transaction_amount);
				new_transaction.setString(7, from_currency);
				new_transaction.setString(8, to_currency);
				new_transaction.setString(9, memo);
				new_transaction.executeUpdate();
			   
				// if user has verified their email, send them a transaction notification:

				DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(transaction_timestamp);
				
				String
	
				subject = "Transaction Notification", 
				message_body = "";
				
				message_body += "Transaction notification for <b><!--USERNAME--></b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Type: <b>" + transaction_type + "</b>";
				message_body += "<br/>";
				message_body += "Date and time: <b>" + formatter.format(calendar.getTime()) + "</b>";
				message_body += "<br/>";
				message_body += "Amount: <b>" + format_btc(transaction_amount) + "</b>";
				message_body += "<br/>";
				message_body += "Currency: <b>" + from_currency + "</b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "You may view your account <a href='" + Server.host + "/account/'>here</a>.";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Please do not reply to this email.";
				
				new UserMail(user, subject, message_body);
					
				output.put("status", "1");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}