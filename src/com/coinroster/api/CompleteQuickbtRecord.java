package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.UserMail;

/**
 * Get all complete QuickBT records (could be part of DB?).
 * 
 * @custom.access standard
 *
 */
public class CompleteQuickbtRecord extends Utils
	{
	public static String method_level = "standard";
	
	/**
	 * Get all complete QuickBT records (could be part of DB?).
	 * 
	 * @param method.input.passed_id
	 * @throws Exception
	 */
	public CompleteQuickbtRecord(MethodInstance method) throws Exception 
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
			
			passed_id = input.getString("passed_id"),
			user_id = session.user_id();
			
			PreparedStatement test_quickbt_record = sql_connection.prepareStatement("select * from quickbt where completed = 0 and passed_id = ? and user_id = ?");
			test_quickbt_record.setString(1, passed_id);
			test_quickbt_record.setString(2, user_id);
			ResultSet result_set = test_quickbt_record.executeQuery();

			if (!result_set.next()) // quietly exit if user shouldn't have access or record has been completed
				{
				output.put("status", "1");
				break method;
				}
			
			PreparedStatement complete_quickbt_record = sql_connection.prepareStatement("update quickbt set completed = ? where passed_id = ?");
			complete_quickbt_record.setLong(1, System.currentTimeMillis());
			complete_quickbt_record.setString(2, passed_id);
			complete_quickbt_record.executeUpdate();
			
			JSONObject user = db.select_user("id", user_id);
			
			String
			
			subject = "We are processing your deposit", 

			message_body = "Hi <b><!--USERNAME--></b>";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body = "We have received your request to deposit funds to CoinRoster. We will credit your account as soon as the funds arrive. You will receive an email when the deposit has been completed";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "You may view your account <a href='" + Server.host + "/account/'>here</a>.";
			
			new UserMail(user, subject, message_body);

			// send notification to cash register admin
			
			JSONObject cash_register = db.select_user("username", "internal_cash_register");
			
			String 
			
			cash_register_email_address = cash_register.getString("email_address"),
			cash_register_admin = "Cash Register Admin";
			
			subject = "New QuickBT deposit";
			
			message_body = "A QuickBT deposit has been initiated by <b>" + user.getString("username") + "</b>";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "Please see admin panel.";
			
			Server.send_mail(cash_register_email_address, cash_register_admin, subject, message_body);

			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}