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

public class GetPendingDeposits extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public GetPendingDeposits(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------

			PreparedStatement select_transaction  = sql_connection.prepareStatement("select * from transaction where trans_type = 'BTC-DEPOSIT' and pending_flag = 1");

			ResultSet result_set = select_transaction.executeQuery();
			
			JSONArray pending_deposits = new JSONArray();
	   
			while (result_set.next())
				{
				String 
		
				transaction_id = result_set.getString(1),
				created = result_set.getString(2),
				created_by = result_set.getString(3),
				trans_type = result_set.getString(4),
				to_account = result_set.getString(6),
				amount = result_set.getString(7),
				to_currency = result_set.getString(9),
				ext_address = result_set.getString(12);
		
				JSONObject transaction = new JSONObject();

				transaction.put("transaction_id", transaction_id);
				transaction.put("created", created);
				transaction.put("trans_type", trans_type);
				transaction.put("to_account", to_account);
				transaction.put("amount", amount);
				transaction.put("to_currency", to_currency);
				transaction.put("ext_address", ext_address);
				
				pending_deposits.put(transaction);
				}
			
			output.put("pending_deposits", pending_deposits);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}