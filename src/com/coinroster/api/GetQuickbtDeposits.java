package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class GetQuickbtDeposits extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public GetQuickbtDeposits(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		method : {
			
//------------------------------------------------------------------------------------

			PreparedStatement select_transaction  = sql_connection.prepareStatement("select * from quickbt where completed > 0 and pending_flag = 1 and cancelled_flag = 0");

			ResultSet result_set = select_transaction.executeQuery();
			
			JSONArray quickbt_deposits = new JSONArray();
	   
			while (result_set.next())
				{
				String 
		
				transaction_id = result_set.getString(1),
				passed_id = result_set.getString(2),
				user_id = result_set.getString(3);
				
				Long
				
				created = result_set.getLong(4),
				completed = result_set.getLong(5);
		
				JSONObject transaction = new JSONObject();

				transaction.put("transaction_id", transaction_id);
				transaction.put("passed_id", passed_id);
				transaction.put("user_id", user_id);
				transaction.put("created", created);
				transaction.put("completed", completed);
				
				quickbt_deposits.put(transaction);
				}
			
			output.put("quickbt_deposits", quickbt_deposits);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}