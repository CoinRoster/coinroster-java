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

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
			double amount_to_withdraw = input.getDouble("amount_to_withdraw");
			
			String
			 
			user_id = session.user_id(),
			internal_btc_liability_id = db.get_id_for_username("internal_btc_liability"),
			internal_cash_register_id = db.get_id_for_username("internal_cash_register"),
			
			created_by = user_id,
			transaction_type = "BTC-WITHDRAWAL",
			from_account = user_id,
			to_account = internal_btc_liability_id,
			from_currency = "BTC",
			to_currency = "BTC",
			memo = "Method: UserWithdrawal";
			
			int pending_flag = 1;
			
			// start using SQL | lock user_xref table so that balance lookups and adjustments are contiguous:
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user_xref write");

			// get user_xref records for internal accounts and user account:

			String[] 
					
			internal_btc_liability_xref = db.select_user_xref("id", internal_btc_liability_id),
			user_xref = db.select_user_xref("id", user_id);
			
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
			user_btc_balance = Double.parseDouble(user_xref[1]);
			
			// withdrawal-specific logic:
			
			if (amount_to_withdraw > user_btc_balance) 
				{
				output.put("error", "User has insufficient funds");
				statement.execute("unlock tables");
				break method;
				}

			// deduct withdrawal amount from user:
			
			Double new_user_btc_balance = user_btc_balance - amount_to_withdraw;
	
			PreparedStatement update_btc_liability_balance = sql_connection.prepareStatement("update user_xref set btc_balance = ? where id = ?");
			update_btc_liability_balance.setDouble(1, new_user_btc_balance);
			update_btc_liability_balance.setString(2, user_id);
			update_btc_liability_balance.executeUpdate();

			// add withdrawal amount to internal_btc_liability:
			
			Double new_btc_liability_balance = internal_btc_liability_balance + amount_to_withdraw;
			
			PreparedStatement update_user_balance = sql_connection.prepareStatement("update user_xref set btc_balance = ? where id = ?");
			update_user_balance.setDouble(1, new_btc_liability_balance);
			update_user_balance.setString(2, internal_btc_liability_id);
			update_user_balance.executeUpdate();

			statement.execute("unlock tables");
			
			// finally, write transaction record:
			
			Long transaction_timestamp = System.currentTimeMillis();
			
			String ext_address = user_xref[3];
			
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
			
			// communications
			
			DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(transaction_timestamp);
			
			String
			
			to_address = user_xref[4],
			email_ver_flag = user_xref[6],
			to_user = session.username();
			
			// if user has verified their email, send them a transaction notification:
			
			if (to_address != null && email_ver_flag.equals("1"))
				{
				
				String
				
				subject = "Withdrawal request confirmation", 
				message_body = "";
				
				message_body += "Hi <span style='font-weight:bold'>" + to_user + "</span>,";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "We have received your request to withdraw funds. One of our administrators will process your request shortly. You will receive another confirmation email when the funds have been sent";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Transaction ID: <span style='font-weight:bold'>" + transaction_id + "</span>";
				message_body += "<br/>";
				message_body += "Type: <span style='font-weight:bold'>" + transaction_type + "</span>";
				message_body += "<br/>";
				message_body += "Date and time: <span style='font-weight:bold'>" + formatter.format(calendar.getTime()) + "</span>";
				message_body += "<br/>";
				message_body += "Amount: <span style='font-weight:bold'>" + format_btc(amount_to_withdraw) + " BTC</span>";
				message_body += "<br/>";
				message_body += "To (wallet on file): <span style='font-weight:bold'>" + ext_address + "</span>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "You may view your account <a href='" + Server.host + "/account/'>here</a>.";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Please do not reply to this email.";
				
				Server.send_mail(to_address, to_user, subject, message_body);
				}
			
			String[] cash_register_xref = db.select_user_xref("id", internal_cash_register_id);
			
			String cash_register_admin_email = cash_register_xref[4];
			
			String
			
			cash_register_admin = "Cash Register Admin",
			subject = "New withdrawal request", 
			message_body = "";
			
			message_body += "A withdrawal request has been submitted by <span style='font-weight:bold'>" + to_user + "</span>";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "Please see admin panel.";
			
			Server.send_mail(cash_register_admin_email, cash_register_admin, subject, message_body);
						
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}