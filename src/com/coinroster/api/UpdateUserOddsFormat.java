package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.HashSet;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class UpdateUserOddsFormat extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public UpdateUserOddsFormat(MethodInstance method) throws Exception 
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
			odds_format = input.getString("odds_format");
			
			HashSet<String> odds_formats = new HashSet<String>(Arrays.asList(
				"DECIMAL", 
				"AMERICAN", 
				"FRACTIONAL",
				"PERCENTAGE"
			));
			
			if (!odds_formats.contains(odds_format))
				{
				output.put("error", "Invalid odds format: " + odds_format);
				break method;
				}
			
			PreparedStatement update_currency = sql_connection.prepareStatement("update user set odds_format = ? where id = ?");
			update_currency.setString(1, odds_format);
			update_currency.setString(2, user_id);
			update_currency.executeUpdate();

			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}