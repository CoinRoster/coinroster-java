package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

public class UserWithdrawal extends Utils
	{
	public static String method_level = "standard";
	public UserWithdrawal(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			double amount_to_withdraw = input.getDouble("amount_to_withdraw");
			
			if (amount_to_withdraw <= 0)
				{
				output.put("error", "Amount must be positive");
				break method;
				}
			
			String
			 
			user_id = session.user_id(),
			username = session.username(),
			
			btc_liability_id = db.get_id_for_username("internal_liability"),
			
			created_by = user_id,
			transaction_type = "BTC-WITHDRAWAL",
			from_account = user_id,
			to_account = btc_liability_id,
			from_currency = "BTC",
			to_currency = "BTC",
			memo = "User request to withdraw BTC";
			
			int pending_flag = 1;
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write");

			JSONObject user = null;
			
			boolean success = false;
					
			try {
				lock : {
				
					user = db.select_user("id", user_id);
					
					if (user.getInt("withdrawal_locked") == 1)
						{
						output.put("error", "You cannot withdraw funds until you meet a playing requirement");
						break lock;
						}
					
					JSONObject btc_liability = db.select_user("id", btc_liability_id);
					
					// get account balances:
				  
					double
					
					btc_liability_balance = btc_liability.getDouble("btc_balance"),
					user_btc_balance = user.getDouble("btc_balance");
					
					// withdrawal-specific logic:
					
					if (amount_to_withdraw > user_btc_balance) 
						{
						output.put("error", "Insufficient funds");
						break lock;
						}
	
					// deduct withdrawal amount from user:
					
					Double new_btc_balance = subtract(user_btc_balance, amount_to_withdraw, 0);
			
					db.update_btc_balance(user_id, new_btc_balance);
						
					// add withdrawal amount to internal_btc_liability (decreases liability):
					
					Double new_btc_liability_balance = add(btc_liability_balance, amount_to_withdraw, 0);
					
					db.update_btc_balance(btc_liability_id, new_btc_liability_balance);
					
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
				Long transaction_timestamp = System.currentTimeMillis();
				
				String ext_address = user.getString("ext_address");
				
				PreparedStatement new_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, pending_flag, ext_address) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);				
				new_transaction.setLong(1, transaction_timestamp);
				new_transaction.setString(2, created_by);
				new_transaction.setString(3, transaction_type);
				new_transaction.setString(4, from_account);
				new_transaction.setString(5, to_account);
				new_transaction.setDouble(6, amount_to_withdraw);
				new_transaction.setString(7, from_currency);
				new_transaction.setString(8, to_currency);
				new_transaction.setString(9, memo);
				new_transaction.setInt(10, pending_flag);
				new_transaction.setString(11, ext_address);
				new_transaction.execute();
				
				ResultSet rs = new_transaction.getGeneratedKeys();
			    rs.next();
			    int transaction_id = rs.getInt(1);
			    
			    output.put("transaction_id", transaction_id);
				
			    // send withdrawal confirmation
			  			
				DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(transaction_timestamp);
				
				String 
	
				subject = "Withdrawal request confirmation", 
				
				message_body = "Hi <b><!--USERNAME--></b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "We have received your request to withdraw funds. One of our administrators will process your request shortly. You will receive another confirmation email when the funds have been sent";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Transaction ID: <b>" + transaction_id + "</b>";
				message_body += "<br/>";
				message_body += "Type: <b>" + transaction_type + "</b>";
				message_body += "<br/>";
				message_body += "Date and time: <b>" + formatter.format(calendar.getTime()) + "</b>";
				message_body += "<br/>";
				message_body += "Amount: <b>" + format_btc(amount_to_withdraw) + " BTC</b>";
				message_body += "<br/>";
				message_body += "To (wallet on file): <b>" + ext_address + "</b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "You may view your account <a href='" + Server.host + "/account/'>here</a>.";
					
				new UserMail(user, subject, message_body);
	
				// send notification to cash register admin
				
				JSONObject cash_register = db.select_user("username", "internal_cash_register");
				
				String 
				
				cash_register_email_address = cash_register.getString("email_address"),
				cash_register_admin = "CoinRoster Admin";
				
				subject = "New withdrawal request";
				
				message_body = "A withdrawal request has been submitted by <b>" + username + "</b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Please see admin panel.";
				
				Server.send_mail(cash_register_email_address, cash_register_admin, subject, message_body);
				
			    output.put("status", "1");
				}
						
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}