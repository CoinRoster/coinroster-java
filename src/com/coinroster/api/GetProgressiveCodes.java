package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class GetProgressiveCodes extends Utils
{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public GetProgressiveCodes(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------

			PreparedStatement select_categories = sql_connection.prepareStatement("select code, balance from progressive");
			ResultSet code_rs = select_categories.executeQuery();
			
			JSONArray codes = new JSONArray();
			JSONArray balances = new JSONArray();
			
			while(code_rs.next())
				{
				codes.put(code_rs.getString(1));
				balances.put(code_rs.getString(2));
				log(code_rs.getString(2));
				}
			
			output.put("codes", codes);
			output.put("balances", balances);
			
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}