package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

/**
 * Cancel a pending deposut
 * @custom.access admin
 */
public class CancelPendingDeposit extends Utils
	{
	public static String method_level = "admin";
	
	/**
	 * Cancel a pending deposit.
	 * 
	 * @param method.input.transaction_id
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	public CancelPendingDeposit(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------

			int transaction_id = input.getInt("transaction_id");

			PreparedStatement update_transaction = sql_connection.prepareStatement("update transaction set cancelled_flag = 1, pending_flag = 0 where id = ?");
			update_transaction.setInt(1, transaction_id);
			update_transaction.executeUpdate();
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}