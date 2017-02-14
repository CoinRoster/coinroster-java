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
			
			// start using SQL | lock user_xref table so that balance lookups and adjustments are contiguous:
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write");

			// get user_xref records for internal accounts and user account:

			JSONObject
			
			btc_liability = db.select_user("id", btc_liability_id),
			user = db.select_user("id", user_id);
			
			// unlock and break method if user key is invalid:
			
			if (user == null) 
				{
				statement.execute("unlock tables");
				output.put("error", "Invalid user account");
				break method;
				}

			// get account balances:
		  
			double
			
			btc_liability_balance = btc_liability.getDouble("btc_balance"),
			user_btc_balance = user.getDouble("btc_balance");
			
			// withdrawal-specific logic:
			
			if (amount_to_withdraw > user_btc_balance) 
				{
				output.put("error", "User has insufficient funds");
				statement.execute("unlock tables");
				break method;
				}

			// deduct withdrawal amount from user:
			
			Double new_user_btc_balance = user_btc_balance - amount_to_withdraw;
	
			PreparedStatement update_btc_liability_balance = sql_connection.prepareStatement("update user set btc_balance = ? where id = ?");
			update_btc_liability_balance.setDouble(1, new_user_btc_balance);
			update_btc_liability_balance.setString(2, user_id);
			update_btc_liability_balance.executeUpdate();

			// add withdrawal amount to internal_btc_liability:
			
			Double new_btc_liability_balance = btc_liability_balance + amount_to_withdraw;
			
			PreparedStatement update_user_balance = sql_connection.prepareStatement("update user set btc_balance = ? where id = ?");
			update_user_balance.setDouble(1, new_btc_liability_balance);
			update_user_balance.setString(2, btc_liability_id);
			update_user_balance.executeUpdate();

			statement.execute("unlock tables");
			
			// finally, write transaction record:
			
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
		    output.put("status", "1");
			
		    // send withdrawal confirmation
		  			
			DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(transaction_timestamp);
			
			String 

			subject = "Withdrawal request confirmation", 
			
			message_body = "Hi <b>" + username + "</b>,";
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
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "Please do not reply to this email.";
				
			new UserMail(user, subject, message_body);

			// send notification to cash register admin
			
			JSONObject cash_register = db.select_user("username", "internal_cash_register");
			
			String 
			
			cash_register_email_address = cash_register.getString("email_address"),
			cash_register_admin = "Cash Register Admin";
			
			subject = "New withdrawal request";
			
			message_body = "A withdrawal request has been submitted by <b>" + username + "</b>";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "Please see admin panel.";
			
			Server.send_mail(cash_register_email_address, cash_register_admin, subject, message_body);
						
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}