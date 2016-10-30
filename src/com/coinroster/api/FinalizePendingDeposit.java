package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class FinalizePendingDeposit extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public FinalizePendingDeposit(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
			int transaction_id = input.getInt("transaction_id");

			PreparedStatement update_transaction = sql_connection.prepareStatement("update pending_deposit set completed = 1 where id = ?");
			update_transaction.setInt(1, transaction_id);
			update_transaction.executeUpdate();
				
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}