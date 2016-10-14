package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class SendReferral extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public SendReferral(MethodInstance method) throws Exception 
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
			
			referrer_provided_name = input.getString("referrer"),
			referrer_id = session.user_id(),
			referrer_username = db.get_username_for_id(referrer_id),

			referral_key = Server.generate_key(referrer_id),
			referral_program = input.getString("referral_program"),

			// should do a referral program lookup else break method
			
			email_contact_name = "Referral",
			email_address = no_whitespace(input.getString("email_address"));
			
			PreparedStatement create_user = sql_connection.prepareStatement("insert into referral(referral_key, referrer_id, referrer_username, email_address, referral_program, created) values(?, ?, ?, ?, ?, ?)");				
			create_user.setString(1, referral_key);
			create_user.setString(2, referrer_id);
			create_user.setString(3, referrer_username);
			create_user.setString(4, email_address);
			create_user.setInt(5, Integer.parseInt(referral_program));
			create_user.setLong(6, System.currentTimeMillis());
			create_user.executeUpdate();
			
			String

			subject = "CoinRoster invitation from " + referrer_provided_name,
			message_body = "You have been invited by <span style='font-weight:bold'>" + referrer_provided_name + "</span> to join CoinRoster.<br></br><br></br>Please <a href='" + Server.host + "/signup.html?" + referral_key + "'>click here</a> to create an account.";

			Server.send_mail(email_address, email_contact_name, subject, message_body);

			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}