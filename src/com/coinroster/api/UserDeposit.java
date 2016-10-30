package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

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
		
			double amount_to_deposit = input.getDouble("amount_to_deposit");
			
			String 
			
			user_id = session.user_id(),
			username = session.username(),
			ext_address = input.getString("user_ext_address"),
			internal_cash_register_id = db.get_id_for_username("internal_cash_register"),
			cash_register_address = input.getString("cash_register_address");
			
			String[] user_xref = db.select_user_xref("id", user_id);
			
			String 
			
			to_address = user_xref[4],
			email_ver_flag = user_xref[6];
			
			PreparedStatement new_transaction = sql_connection.prepareStatement("insert into pending_deposit(created, created_by, ext_address, cash_register_address, amount) values(?, ?, ?, ?, ?)");				
			new_transaction.setLong(1, System.currentTimeMillis());
			new_transaction.setString(2, user_id);
			new_transaction.setString(3, ext_address);
			new_transaction.setString(4, cash_register_address);
			new_transaction.setDouble(5, amount_to_deposit);
			new_transaction.executeUpdate();

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