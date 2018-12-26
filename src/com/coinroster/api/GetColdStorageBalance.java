package com.coinroster.api;

import java.sql.Connection;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

/**
 * Get the cold storage account's balance.
 * 
 * @custom.access admin
 *
 */
public class GetColdStorageBalance extends Utils{

	public static String method_level = "admin";
	
	/**
	 * Get the cold storage account's balance.
	 * 
	 * @param method
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	public GetColdStorageBalance(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);
		
		method : {
			
//------------------------------------------------------------------------------------
			Double cold_storage_balance = db.get_cold_storage_balance();

			output.put("cold_storage_balance", cold_storage_balance);
			output.put("status", "1");
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
		
}
