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

public class PromoCodeRequest extends Utils
	{
	public static String method_level = "standard";
	public PromoCodeRequest(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			String requested_code = no_whitespace(input.getString("requested_code"));
		
			if (requested_code.length() > 20)
				{
				output.put("error", "Promo code is too long.");
				break method;
				}
			
			// check for any "open" code requests (not yet approved, not yet denied)
			
			PreparedStatement select_promo_requests = sql_connection.prepareStatement("select * from promo_request where approved = 0 and denied = 0 and created_by = ?");
			select_promo_requests.setString(1, session.user_id());
			ResultSet result_set = select_promo_requests.executeQuery();

			if (result_set.next())
				{
				output.put("error", "We have already received a prior code request from your account. Please be patient, we will contact you soon.");
				break method;
				}
			
			PreparedStatement create_promo_request = sql_connection.prepareStatement("insert into promo_request(created, created_by, requested_code) values(?, ?, ?)");
			create_promo_request.setLong(1, System.currentTimeMillis());
			create_promo_request.setString(2, session.user_id());
			create_promo_request.setString(3, requested_code);
			create_promo_request.executeUpdate();
			
			JSONObject cash_register = db.select_user("username", "internal_cash_register");
			
			String 
			
			cash_register_email_address = cash_register.getString("email_address"),
			cash_register_admin = "CoinRoster Admin",

			subject = "New affiliate promo code request",
			
			message_body = "A promo code request has been submitted by <b>" + session.username() + "</b>";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "Please see admin panel.";
			
			Server.send_mail(cash_register_email_address, cash_register_admin, subject, message_body);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}