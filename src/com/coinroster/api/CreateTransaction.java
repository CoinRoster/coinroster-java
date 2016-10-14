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

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
			String
			 
	        created_by = session.user_id(),
	        transaction_type = input.getString("transaction_type"),

	        btc_liability_account = db.get_id_for_username("internal_btc_liability"),
			rc_liability_account = db.get_id_for_username("internal_rc_liability"),
	    			
	        user_account = no_whitespace(input.getString("user_account")),
	        
	        from_account = "",
	        to_account = "",
	        from_currency = "",
	        to_currency = "",
	        
	        memo = input.getString("memo");

	        // initial validation:

	        if (user_account.length() != 40) break method;
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
	  
	    	// start using SQL | lock user_xref table so that balance lookups and adjustments are contiguous:
		
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user_xref write");

			// get user_xref records for internal accounts and user account:

	        String[] 
	        		
	        internal_btc_liability_xref = db.select_user_xref("id", btc_liability_account),
	        internal_rc_liability_xref = db.select_user_xref("id", rc_liability_account),
	        
	        user_xref = db.select_user_xref("id", user_account);
	        
	        // unlock and break method if user key is invalid:
	        
	        if (user_xref == null) 
	        	{
				statement.execute("unlock tables");
				output.put("error", "Invalid user account");
	        	break method;
	        	}

	        // get account balances:
	      
			double
			
	        internal_btc_liability_balance = Double.parseDouble(internal_btc_liability_xref[1]),
	        internal_rc_liability_balance = Double.parseDouble(internal_rc_liability_xref[2]),

	        user_btc_balance = Double.parseDouble(user_xref[1]),
	        user_rc_balance = Double.parseDouble(user_xref[2]);
	        
			// transaction-specific logic:
			
	        switch (transaction_type)
	        	{
	        	case "BTC-DEPOSIT":
	        		{
	        		// --- FROM --- internal_btc_liability
	        
	        		from_account = btc_liability_account;
	        		from_currency = "BTC";
	        		
	        		// deduct transaction amount from internal_btc_liability:
	        		
	            	Double new_btc_liability_balance = internal_btc_liability_balance - transaction_amount;

	            	PreparedStatement update_btc_liability_balance = sql_connection.prepareStatement("update user_xref set btc_balance = ? where id = ?");
	            	update_btc_liability_balance.setDouble(1, new_btc_liability_balance);
	            	update_btc_liability_balance.setString(2, btc_liability_account);
	            	update_btc_liability_balance.executeUpdate();

	        		// --- TO --- user
	            	
	        		to_account = user_account;
	            	to_currency = "BTC";
	            	
	        		// add transaction amount to user:
	        		
	        		Double new_user_btc_balance = user_btc_balance + transaction_amount;
	     
	            	PreparedStatement update_user_balance = sql_connection.prepareStatement("update user_xref set btc_balance = ? where id = ?");
	            	update_user_balance.setDouble(1, new_user_btc_balance);
	            	update_user_balance.setString(2, user_account);
	            	update_user_balance.executeUpdate();
	            	
	        		break;
	        		}
	        	case "BTC-WITHDRAWAL":
	        		{
	            	if (transaction_amount > user_btc_balance) 
	            		{
	        			output.put("error", "User has insufficient funds");
	        			statement.execute("unlock tables");
	            		break method;
	            		}
	            	
	            	// --- FROM --- user
	    
	        		from_account = user_account;
	        		from_currency = "BTC";
	        		
	        		// deduct transaction amount from user:
	        		
	        		Double new_user_btc_balance = user_btc_balance - transaction_amount;

	            	PreparedStatement update_btc_liability_balance = sql_connection.prepareStatement("update user_xref set btc_balance = ? where id = ?");
	            	update_btc_liability_balance.setDouble(1, new_user_btc_balance);
	            	update_btc_liability_balance.setString(2, user_account);
	            	update_btc_liability_balance.executeUpdate();

	        		// --- TO --- internal_btc_liability

	            	to_account = btc_liability_account;
	        		to_currency = "BTC";

	        		// add transaction amount to internal_btc_liability:
	        		
	            	Double new_btc_liability_balance = internal_btc_liability_balance + transaction_amount;
	            	
	            	PreparedStatement update_user_balance = sql_connection.prepareStatement("update user_xref set btc_balance = ? where id = ?");
	            	update_user_balance.setDouble(1, new_btc_liability_balance);
	            	update_user_balance.setString(2, btc_liability_account);
	            	update_user_balance.executeUpdate();
	            	
	        		break;
	        		}
	        	case "RC-DEPOSIT":
	        		{
	    			// --- FROM --- internal_rc_liability
	        		
	        		from_account = rc_liability_account;
	        		from_currency = "RC";
	        		
	        		// deduct transaction amount from internal_rc_liability:
	        		
	            	Double new_rc_liability_balance = internal_rc_liability_balance - transaction_amount;

	            	PreparedStatement update_rc_liability_balance = sql_connection.prepareStatement("update user_xref set rc_balance = ? where id = ?");
	            	update_rc_liability_balance.setDouble(1, new_rc_liability_balance);
	            	update_rc_liability_balance.setString(2, rc_liability_account);
	            	update_rc_liability_balance.executeUpdate();

	        		// --- TO --- user
	            	
	        		to_account = user_account;
	            	to_currency = "RC";
	            	
	        		// add transaction amount to user:
	        		
	        		Double new_user_rc_balance = user_rc_balance + transaction_amount;
	     
	            	PreparedStatement update_user_balance = sql_connection.prepareStatement("update user_xref set rc_balance = ? where id = ?");
	            	update_user_balance.setDouble(1, new_user_rc_balance);
	            	update_user_balance.setString(2, user_account);
	            	update_user_balance.executeUpdate();
	            	
	        		break;
	        		}
	        	case "RC-WITHDRAWAL":
	        		{
	    			if (transaction_amount > user_rc_balance)
	            		{
	        			statement.execute("unlock tables");
	        			output.put("error", "User has insufficient funds");
	            		break method;
	            		}
	            	
	            	// --- FROM --- user
	    
	        		from_account = user_account;
	        		from_currency = "RC";
	        		
	        		// deduct transaction amount from user:
	        		
	        		Double new_user_rc_balance = user_rc_balance - transaction_amount;

	            	PreparedStatement update_rc_liability_balance = sql_connection.prepareStatement("update user_xref set rc_balance = ? where id = ?");
	            	update_rc_liability_balance.setDouble(1, new_user_rc_balance);
	            	update_rc_liability_balance.setString(2, user_account);
	            	update_rc_liability_balance.executeUpdate();

	        		// --- TO --- internal_rc_liability

	            	to_account = rc_liability_account;
	        		to_currency = "RC";

	        		// add transaction amount to internal_rc_liability:
	        		
	            	Double new_rc_liability_balance = internal_rc_liability_balance + transaction_amount;
	            	
	            	PreparedStatement update_user_balance = sql_connection.prepareStatement("update user_xref set rc_balance = ? where id = ?");
	            	update_user_balance.setDouble(1, new_rc_liability_balance);
	            	update_user_balance.setString(2, rc_liability_account);
	            	update_user_balance.executeUpdate();
	            	
	        		break;
	        		}
	        	default: 
	        		{
	    			statement.execute("unlock tables");
	    			output.put("error", "Invalid transaction");
	        		break method;
	        		}
	        	}

			statement.execute("unlock tables");
			
	        // finally, write transaction record:
			
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
	        
	        String
			
			to_address = user_xref[4],
			email_ver_flag = user_xref[6],
			to_user = db.get_username_for_id(user_account);
	        
	        if (to_address != null && email_ver_flag.equals("1"))
	        	{
	            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	            Calendar calendar = Calendar.getInstance();
	            calendar.setTimeInMillis(transaction_timestamp);
	            
	            String
	            
				subject = "Transaction Notification", 
				message_body = "";
	            
				message_body += "Transaction notification for <span style='font-weight:bold'>" + to_user + "</span>";
	            message_body += "<br/>";
	            message_body += "<br/>";
	            message_body += "Type: <span style='font-weight:bold'>" + transaction_type + "</span>";
	            message_body += "<br/>";
	            message_body += "Date and time: <span style='font-weight:bold'>" + formatter.format(calendar.getTime()) + "</span>";
	            message_body += "<br/>";
	            message_body += "Amount: <span style='font-weight:bold'>" + transaction_amount + "</span>";
	            message_body += "<br/>";
	            message_body += "Currency: <span style='font-weight:bold'>" + from_currency + "</span>";
	            message_body += "<br/>";
	            message_body += "<br/>";
	            message_body += "You may view your account <a href='" + Server.host + "/account/'>here</a>.";
	            message_body += "<br/>";
	            message_body += "<br/>";
	            message_body += "<br/>";
	            message_body += "Please do not reply to this email.";
	            
				Server.send_mail(to_address, to_user, subject, message_body);
	            }
	        
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}