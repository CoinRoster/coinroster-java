package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class UpdateColdStorageBalance extends Utils	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public UpdateColdStorageBalance(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------

			Double amount = input.getDouble("amount");
			
			PreparedStatement update_cold_storage_balance = sql_connection.prepareStatement("update control set value = ? where name = ?");
			update_cold_storage_balance.setDouble(1, amount);
			update_cold_storage_balance.setString(2, "cold_storage_balance");
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}
