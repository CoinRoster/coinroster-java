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

public class TransferFunds_Admin extends Utils
	{
	public static String method_level = "admin";
	public TransferFunds_Admin(MethodInstance method) throws Exception 
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
			sender_id = no_whitespace(input.getString("sender")),
			btc_rc = input.getString("btc_rc"),
			receiver_id = no_whitespace(input.getString("receiver")),
			memo = input.getString("memo"),
			created_by = session.user_id(),
			from_account = "",
			to_account = "",
			from_currency = "",
			to_currency = "";

			JSONObject sender = db.select_user("id", sender_id);
			JSONObject receiver = db.select_user("id", receiver_id);
			
			// initial validation
			if (sender == null | receiver == null){
				output.put("error", "Something went wrong. Please try again");
				break method;
			}
			if(sender_id.equals(receiver_id)){
				output.put("error", "You cannot transfer to and from the same account");
				break method;
			}
			if (memo.length() > 500){
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
			
			if (transaction_amount <= 0)
				{
				output.put("error", "Transfer amount must be positive");
				break method;
				}
			
			if (sender.getInt("withdrawal_locked") == 1)
				{
				output.put("error", "You cannot transfer funds until you meet a playing requirement");
				break method;
				}

			
			// start using SQL | lock user table so that balance lookups and adjustments are contiguous:
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write");
			
			boolean success = false;
		
			try {
				lock : {

					// get account balances:
					double
					sender_btc_balance = sender.getDouble("btc_balance"),
					sender_rc_balance = sender.getDouble("rc_balance"),
					receiver_btc_balance = receiver.getDouble("btc_balance"),
					receiver_rc_balance = receiver.getDouble("rc_balance");
					
					from_account = sender.getString("user_id");
					to_account = receiver.getString("user_id");
					// transaction-specific logic:
					
					switch (btc_rc)
						{
						
						case "BTC":
						{
							if (transaction_amount > sender_btc_balance) 
								{
								output.put("error", "The sender has insufficient funds to transfer that amount.");
								break lock;
								}
							
							from_currency = "BTC";
							to_currency = "BTC";
							
							// deduct transaction amount from sender:
							Double new_sender_btc_balance = subtract(sender_btc_balance, transaction_amount, 0);
							db.update_btc_balance(from_account, new_sender_btc_balance);
		
							// add transaction amount to receiver
							Double new_receiver_btc_balance = add(receiver_btc_balance, transaction_amount, 0);
							db.update_btc_balance(to_account, new_receiver_btc_balance);
							
							break;
							}
						
						case "RC":
						{
							if (transaction_amount > sender_rc_balance) 
								{
								output.put("error", "The sender has insufficient funds to transfer that amount.");
								break lock;
								}
							
							from_currency = "RC";
							to_currency = "RC";
							
							// deduct transaction amount from sender:
							Double new_sender_rc_balance = subtract(sender_rc_balance, transaction_amount, 0);
							db.update_rc_balance(from_account, new_sender_rc_balance);
		
							// add transaction amount to receiver
							Double new_receiver_rc_balance = add(receiver_rc_balance, transaction_amount, 0);
							db.update_rc_balance(to_account, new_receiver_rc_balance);
							
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
				
				Long transaction_timestamp = System.currentTimeMillis();
				PreparedStatement new_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo) values(?, ?, ?, ?, ?, ?, ?, ?, ?)");				
				new_transaction.setLong(1, transaction_timestamp);
				new_transaction.setString(2, created_by);
				String trans_type = btc_rc + "-USER-TRANSFER";
				new_transaction.setString(3, trans_type);
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
				subject = "Transaction Notification - Funds Transfer Successful",
				
				message_body  = "Transaction notification for <b><!--USERNAME--></b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Type: <b>" + trans_type + "</b>";
				message_body += "<br/>";
				message_body += "Date and time: <b>" + formatter.format(calendar.getTime()) + "</b>";
				message_body += "<br/>";
				message_body += "Amount: <b>" + format_btc(transaction_amount) + "</b>";
				message_body += "<br/>";
				message_body += "To: <b>" + receiver.getString("username") + "</b>";
				message_body += "<br/>";
				message_body += "Currency: <b>" + from_currency + "</b>";
				message_body += "<br/>";
				message_body += "Memo: <b>" + memo + "</b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "You may view your account <a href='" + Server.host + "/account/'>here</a>.";
				
				new UserMail(sender, subject, message_body);
				
				// send email to recipient
				subject = "Funds Deposit - User Transfer";
				
				message_body  = "Transaction notification for <b><!--USERNAME--></b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "You have received a funds transfer from " + sender.getString("username");
				message_body += "<br/>";
				message_body += "Type: <b>" + trans_type + "</b>";
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
				
				new UserMail(receiver, subject, message_body);
					
				output.put("status", "1");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}