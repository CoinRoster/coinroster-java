package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class ExpireSettlementWindow extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public ExpireSettlementWindow(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
				int contest_id = input.getInt("contest_id");
				

				JSONObject cash_register = db.select_user("username", "internal_cash_register");	
				
				String

				cash_register_email_address = cash_register.getString("email_address"),
				cash_register_admin = "Cash Register Admin",
				
				subject_admin = "User generated Contest Expired!",
				message_body_admin = "";
				
				message_body_admin += "<br/>";
				message_body_admin += "<br/>";
				message_body_admin += "A contest created by the user <b>" + session.username() + "</b> has expired!";
				message_body_admin += "<br/>";
				message_body_admin += "<br/>";
				message_body_admin += "Contest ID: <b>" + contest_id + "</b>";
				message_body_admin += "<br/>";
				message_body_admin += "<br/>";
				message_body_admin += "Please settle the contest from the admin panel.";
				message_body_admin += "<br/>";

				Server.send_mail(cash_register_email_address, cash_register_admin, subject_admin, message_body_admin);
				
				output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}