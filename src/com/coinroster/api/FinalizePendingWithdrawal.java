package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class FinalizePendingWithdrawal extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public FinalizePendingWithdrawal(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
			int transaction_id = input.getInt("transaction_id");

			String[] transaction = db.select_transaction(transaction_id);
	   
			if (transaction != null)
				{
				String 

				transaction_type = transaction[3],
				user_account = transaction[4],
				amount = transaction[6],
				ext_address = transaction[11];
				
				PreparedStatement update_transaction = sql_connection.prepareStatement("update transaction set pending_flag = 0, created_by = ? where id = ?");
				update_transaction.setString(1, session.user_id());
				update_transaction.setDouble(2, transaction_id);
				update_transaction.executeUpdate();
				
				String[] user_xref = db.select_user_xref("id", user_account);
				
				String
				
				to_address = user_xref[4],
				email_ver_flag = user_xref[6],
				to_user = db.get_username_for_id(user_account);
				
				if (to_address != null && email_ver_flag.equals("1"))
					{
					DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

					Calendar calendar = Calendar.getInstance();
					
					String
					
					subject = "Your withdrawal has been processed", 
					message_body = "";
					
					message_body += "Hi <span style='font-weight:bold'>" + to_user + "</span>,";
					message_body += "<br/>";
					message_body += "<br/>";
					message_body += "One of our administrators has processed your withdrawal. The funds should arrive in your Bitcoin wallet soon.";
					message_body += "<br/>";
					message_body += "<br/>";
					message_body += "Transaction ID: <span style='font-weight:bold'>" + transaction_id + "</span>";
					message_body += "<br/>";
					message_body += "Type: <span style='font-weight:bold'>" + transaction_type + "</span>";
					message_body += "<br/>";
					message_body += "Date and time: <span style='font-weight:bold'>" + formatter.format(calendar.getTime()) + "</span>";
					message_body += "<br/>";
					message_body += "Amount: <span style='font-weight:bold'>" + format_btc(Double.parseDouble(amount)) + "</span>";
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
				}
			else output.put("error", "Transaction not found");
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}