package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class FinalizePendingDeposit extends Utils
	{
	public static String method_level = "admin";
	public FinalizePendingDeposit(MethodInstance method) throws Exception 
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
			
			user_id = session.user_id(),
			internal_btc_liability_id = db.get_id_for_username("internal_btc_liability"),
			memo = input.getString("memo");

			int transaction_id = input.getInt("transaction_id");
			
			double received_amount = input.getDouble("received_amount");

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

			// add received amount to user balance:
			
			Double new_user_btc_balance = user_btc_balance + received_amount;
	
			PreparedStatement update_btc_liability_balance = sql_connection.prepareStatement("update user_xref set btc_balance = ? where id = ?");
			update_btc_liability_balance.setDouble(1, new_user_btc_balance);
			update_btc_liability_balance.setString(2, user_id);
			update_btc_liability_balance.executeUpdate();

			// subtract received amount from internal_btc_liability:
			
			Double new_btc_liability_balance = internal_btc_liability_balance - received_amount;
			
			PreparedStatement update_user_balance = sql_connection.prepareStatement("update user_xref set btc_balance = ? where id = ?");
			update_user_balance.setDouble(1, new_btc_liability_balance);
			update_user_balance.setString(2, internal_btc_liability_id);
			update_user_balance.executeUpdate();

			statement.execute("unlock tables");
			
			// finally, update transaction record:
			
			PreparedStatement update_transaction = sql_connection.prepareStatement("update transaction set pending_flag = 0, amount = ?, memo = ? where id = ?");
			update_transaction.setDouble(1, received_amount);
			update_transaction.setString(2, memo);
			update_transaction.setInt(3, transaction_id);
			update_transaction.executeUpdate();
			
			// communications

			String
			
			to_address = user_xref[4],
			email_ver_flag = user_xref[6],
			to_user = session.username();
			
			// if user has verified their email, send them a transaction notification:
			
			if (to_address != null && email_ver_flag.equals("1"))
				{
				
				String
				
				subject = "Deposit confirmation", 
				message_body = "";
				
				message_body += "Hi <span style='font-weight:bold'>" + to_user + "</span>,";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "We have received your deposit and have credited your account!";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Transaction ID: <span style='font-weight:bold'>" + transaction_id + "</span>";
				message_body += "<br/>";
				message_body += "Amount received: <span style='font-weight:bold'>" + format_btc(received_amount) + " BTC</span>";
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