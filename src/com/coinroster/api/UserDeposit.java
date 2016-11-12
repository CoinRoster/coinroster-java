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

public class UserDeposit extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public UserDeposit(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------

			long transaction_timestamp = System.currentTimeMillis();
			
			double amount_to_deposit = input.getDouble("amount_to_deposit");
			
			String 
			
			user_id = session.user_id(),
			username = session.username(),
			ext_address = input.getString("user_ext_address"),
			internal_cash_register_id = db.get_id_for_username("internal_cash_register"),
			cash_register_address = input.getString("cash_register_address"),					
			created_by = user_id,
			transaction_type = "BTC-DEPOSIT",
			from_account = db.get_id_for_username("internal_btc_liability"),
			to_account = user_id,
			from_currency = "BTC",
			to_currency = "BTC",
			memo = "Method: UserDeposit";
			
			String[] user_xref = db.select_user_xref("id", user_id);
			
			String 
			
			to_address = user_xref[4],
			email_ver_flag = user_xref[6];
			
			int pending_flag = 1;
			
			PreparedStatement new_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, pending_flag, ext_address) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);				
			new_transaction.setLong(1, transaction_timestamp);
			new_transaction.setString(2, created_by);
			new_transaction.setString(3, transaction_type);
			new_transaction.setString(4, from_account);
			new_transaction.setString(5, to_account);
			new_transaction.setDouble(6, amount_to_deposit);
			new_transaction.setString(7, from_currency);
			new_transaction.setString(8, to_currency);
			new_transaction.setString(9, memo);
			new_transaction.setInt(10, pending_flag);
			new_transaction.setString(11, ext_address);
			new_transaction.execute();
			
			ResultSet rs = new_transaction.getGeneratedKeys();
		    rs.next();
		    int transaction_id = rs.getInt(1);
		    
			if (to_address != null && email_ver_flag.equals("1"))
				{
				String
				
				subject = "Deposit instructions", 
				message_body = "";
				
				message_body += "Hi <span style='font-weight:bold'>" + username + "</span>,";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "We have received your request to deposit Bitcoins into your CoinRoster account. Please follow the transfer instructions below. We will credit your account as soon as we receive your deposit.";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Transaction ID: <span style='font-weight:bold'>" + transaction_id + "</span>";
				message_body += "<br/>";
				message_body += "Send from: <span style='font-weight:bold'>" + ext_address + "</span>";
				message_body += "<br/>";
				message_body += "Send to: <span style='font-weight:bold'>" + cash_register_address + "</span>";
				message_body += "<br/>";
				message_body += "Amount: <span style='font-weight:bold'>" + format_btc(amount_to_deposit) + " BTC</span>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "You may view your account <a href='" + Server.host + "/account/'>here</a>.";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Please do not reply to this email.";
				
				Server.send_mail(to_address, username, subject, message_body);
				}
			
			String[] cash_register_xref = db.select_user_xref("id", internal_cash_register_id);
			
			String cash_register_admin_email = cash_register_xref[4];
			
			String
			
			cash_register_admin = "Cash Register Admin",
			subject = "New pending deposit", 
			message_body = "";
			
			message_body += "A pending deposit has been created by <span style='font-weight:bold'>" + username + "</span>";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "Please see admin panel.";
			
			Server.send_mail(cash_register_admin_email, cash_register_admin, subject, message_body);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}