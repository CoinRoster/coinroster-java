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
import com.coinroster.internal.UserMail;

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

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			int transaction_id = input.getInt("transaction_id");

			String[] transaction = db.select_transaction_old(transaction_id);
	   
			if (transaction != null)
				{
				String 

				transaction_type = transaction[3],
				user_id = transaction[4],
				amount = transaction[6],
				ext_address = transaction[11];
				
				PreparedStatement update_transaction = sql_connection.prepareStatement("update transaction set pending_flag = 0, created_by = ? where id = ?");
				update_transaction.setString(1, session.user_id());
				update_transaction.setInt(2, transaction_id);
				update_transaction.executeUpdate();
				
				JSONObject user = db.select_user("id", user_id);
		
				DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

				Calendar calendar = Calendar.getInstance();
				
				String
				
				subject = "Your withdrawal has been processed", 
				
				message_body = "Hi <b><!--USERNAME--></b>,";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "One of our administrators has processed your withdrawal. The funds should arrive in your Bitcoin wallet soon.";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Transaction ID: <b>" + transaction_id + "</b>";
				message_body += "<br/>";
				message_body += "Type: <b>" + transaction_type + "</b>";
				message_body += "<br/>";
				message_body += "Date and time: <b>" + formatter.format(calendar.getTime()) + "</b>";
				message_body += "<br/>";
				message_body += "Amount: <b>" + format_btc(Double.parseDouble(amount)) + "</b>";
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
				}
			else output.put("error", "Transaction not found");
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}