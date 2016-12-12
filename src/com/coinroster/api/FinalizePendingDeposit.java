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
import com.coinroster.internal.UserMail;

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

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			String 
			
			user_id = session.user_id(),
			memo = input.getString("memo");

			int transaction_id = input.getInt("transaction_id");
			
			double received_amount = input.getDouble("received_amount");

			// start using SQL | lock user_xref table so that balance lookups and adjustments are contiguous:
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write");

			// get user_xref records for internal accounts and user account:

			JSONObject 
			
			btc_liability = db.select_user("username", "internal_btc_liability"),
			user = db.select_user("id", user_id);

			String btc_liability_id = btc_liability.getString("user_id");
			
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

			// add received amount to user balance:
			
			Double new_user_btc_balance = user_btc_balance + received_amount;
	
			PreparedStatement update_btc_liability_balance = sql_connection.prepareStatement("update user set btc_balance = ? where id = ?");
			update_btc_liability_balance.setDouble(1, new_user_btc_balance);
			update_btc_liability_balance.setString(2, user_id);
			update_btc_liability_balance.executeUpdate();

			// subtract received amount from internal_btc_liability:
			
			Double new_btc_liability_balance = btc_liability_balance - received_amount;
			
			PreparedStatement update_user_balance = sql_connection.prepareStatement("update user set btc_balance = ? where id = ?");
			update_user_balance.setDouble(1, new_btc_liability_balance);
			update_user_balance.setString(2, btc_liability_id);
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
			
			subject = "Deposit confirmation", 
			
			message_body = "Hi <b><!--USERNAME--></b>,";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "We have received your deposit and have credited your account!";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "Transaction ID: <b>" + transaction_id + "</b>";
			message_body += "<br/>";
			message_body += "Amount received: <b>" + format_btc(received_amount) + " BTC</b>";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "You may view your account <a href='" + Server.host + "/account/'>here</a>.";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "Please do not reply to this email.";
			
			new UserMail(user, subject, message_body);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}