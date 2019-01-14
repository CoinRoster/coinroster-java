package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

/**
 * Cancel QuickBT Deposit
 * 
 * @custom.access admin
 *
 */
public class CancelQuickbtDeposit extends Utils
	{
	public static String method_level = "admin";
	
	/**
	 * Cancel QuickBT deposit.
	 * 
	 * @param method.input.quickbt_record_id
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	public CancelQuickbtDeposit(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------

			int quickbt_record_id = input.getInt("quickbt_record_id");

			PreparedStatement update_transaction = sql_connection.prepareStatement("update quickbt set cancelled_flag = 1, pending_flag = 0 where trans_id = ?");
			update_transaction.setInt(1, quickbt_record_id);
			update_transaction.executeUpdate();
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}