package com.coinroster.internal;

import java.sql.Connection;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.Server;

public class NotifyAdmin {

	public NotifyAdmin (Connection sql_connection, String subject, String message) {
		DB db = new DB(sql_connection);
		JSONObject cash_register;
		try {
			cash_register = db.select_user("username", "internal_cash_register");

			String 
			
			cash_register_email_address = cash_register.getString("email_address"),
			cash_register_admin = "Cash Register Admin";
	
			Server.send_mail(cash_register_email_address, cash_register_admin, subject, message);
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}
}
