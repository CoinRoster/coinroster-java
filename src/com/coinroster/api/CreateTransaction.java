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

			user_id = no_whitespace(input.getString("user_account")),
			
			from_account = "",
			to_account = "",
			from_currency = "",
			to_currency = "",
			
			memo = input.getString("memo");
			
			boolean display_as_adjustment = input.getBoolean("display_as_adjustment");
			
			// initial validation:

			if (user_id.length() != 40) 
				{
				output.put("error", "Invalid user ID");
				break method;
				}
			if (memo.length() > 500) 
				{
				output.put("error", "Memo is too long (max 500 chars)");
				break method;
				}
	 
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

			JSONObject user = null;
			
			boolean 
			
			success = false,
			deposit_bonus_activated = false;
		
			try {
				lock : {
				
					user =  db.select_user("id", user_id);
					
					if (user == null) 
						{
						output.put("error", "Invalid user account");
						break lock;
						}

					JSONObject liability_account = db.select_user("username", "internal_liability");
					String liability_account_id = liability_account.getString("user_id");

					// get account balances:
				  
					double
					
					btc_liability_balance = liability_account.getDouble("btc_balance"),
					rc_liability_balance = liability_account.getDouble("rc_balance"),
		
					user_btc_balance = user.getDouble("btc_balance"),
					user_rc_balance = user.getDouble("rc_balance");
					
					// transaction-specific logic:
					
					switch (transaction_type)
						{
						case "BTC-DEPOSIT":
							{
							// --- FROM --- internal_btc_liability
					
							from_account = liability_account_id;
							from_currency = "BTC";
							
							// deduct transaction amount from internal_btc_liability:
							
							Double new_btc_liability_balance = subtract(btc_liability_balance, transaction_amount, 0);
							
							db.update_btc_balance(liability_account_id, new_btc_liability_balance);
		
							// --- TO --- user
							
							to_account = user_id;
							to_currency = "BTC";
							
							// add transaction amount to user:
							
							Double new_user_btc_balance = add(user_btc_balance, transaction_amount, 0);
		
							db.update_btc_balance(user_id, new_user_btc_balance);

							// activate deposit bonus (if applicable)
							
							deposit_bonus_activated = db.enable_deposit_bonus(user, transaction_amount);
							
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
							
							Double new_user_btc_balance = subtract(user_btc_balance, transaction_amount, 0);
		
							db.update_btc_balance(user_id, new_user_btc_balance);
		
							// --- TO --- internal_btc_liability
		
							to_account = liability_account_id;
							to_currency = "BTC";
		
							// add transaction amount to internal_btc_liability:
							
							Double new_btc_liability_balance = add(btc_liability_balance, transaction_amount, 0);
							
							db.update_btc_balance(liability_account_id, new_btc_liability_balance);
							
							break;
							}
						case "RC-DEPOSIT":
							{
							// --- FROM --- internal_rc_liability
							
							from_account = liability_account_id;
							from_currency = "RC";
							
							// deduct transaction amount from internal_rc_liability:
							
							Double new_rc_liability_balance = subtract(rc_liability_balance, transaction_amount, 0);
		
							db.update_rc_balance(liability_account_id, new_rc_liability_balance);
		
							// --- TO --- user
							
							to_account = user_id;
							to_currency = "RC";
							
							// add transaction amount to user:
							
							Double new_user_rc_balance = add(user_rc_balance, transaction_amount, 0);
		
							db.update_rc_balance(user_id, new_user_rc_balance);
							
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
							
							Double new_user_rc_balance = subtract(user_rc_balance, transaction_amount, 0);
		
							db.update_rc_balance(user_id, new_user_rc_balance);
		
							// --- TO --- internal_rc_liability
		
							to_account = liability_account_id;
							to_currency = "RC";
		
							// add transaction amount to internal_rc_liability:
							
							Double new_rc_liability_balance = add(rc_liability_balance, transaction_amount, 0);
		
							db.update_rc_balance(liability_account_id, new_rc_liability_balance);
							
							break;
							}
						default: 
							{
							output.put("error", "Invalid transaction");
							break lock;
							}
						}
					
					// we only get here if none of the 'break transaction' conditions are triggered
					
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
			
			// if transaction completed successfully, record it and send emails
			
			if (success)
				{				
				if (display_as_adjustment) transaction_type = from_currency + "-ADJUSTMENT"; // to_currency and from_currency are always the same
				
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
				
				subject = "",
				message_body = "";
				
				if (deposit_bonus_activated)
					{
					subject = "Claim your deposit bonus!";
					
					message_body  = "Transaction notification for <b><!--USERNAME--></b>";
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
					message_body += "Memo: <b>" + memo + "</b>";
					message_body += "<br/>";
					message_body += "<br/>";
					message_body += "<a href='" + Server.host + "/account/deposit_bonus.html'>Click here</a> to claim your deposit bonus!";
					}
				else
					{
					subject = "Transaction Notification";
					
					message_body  = "Transaction notification for <b><!--USERNAME--></b>";
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
					message_body += "Memo: <b>" + memo + "</b>";
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