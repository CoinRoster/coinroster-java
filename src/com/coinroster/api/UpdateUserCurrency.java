package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class UpdateUserCurrency extends Utils
	{
	public static String method_level = "standard";
	public UpdateUserCurrency(MethodInstance method) throws Exception 
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
			symbol = input.getString("symbol");
			
			JSONObject currency = db.select_currency(symbol);
				
			if (currency == null)
				{
				output.put("error", "Invalid currency symbol: " + symbol);
				break method;
				}
		
			PreparedStatement update_currency = sql_connection.prepareStatement("update user set currency = ? where id = ?");
			update_currency.setString(1, symbol);
			update_currency.setString(2, user_id);
			update_currency.executeUpdate();

			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}