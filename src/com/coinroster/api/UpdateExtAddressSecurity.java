package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class UpdateExtAddressSecurity extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public UpdateExtAddressSecurity(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			String user_id = session.user_id();
				
			JSONObject user = db.select_user("id", user_id);

			int 
			
			user_supplied_secure_flag = input.getInt("ext_address_secure_flag"),
			current_secure_flag = user.getInt("ext_address_secure_flag");
			
			if (user_supplied_secure_flag != current_secure_flag)
				{
				// a user can change their ext_address_secure_flag ...
				
				// 1) if their BTC balance is 0.0
				// 2) if ext_address_secure_flag is being changed to 1 (enabled)

				double btc_balance = user.getDouble("btc_balance");

				if (user_supplied_secure_flag == 1 || btc_balance == 0.0)
					{
					PreparedStatement update_ext_address = sql_connection.prepareStatement("update user set ext_address_secure_flag = ? where id = ?");
					update_ext_address.setInt(1, user_supplied_secure_flag);
					update_ext_address.setString(2, user_id);
					update_ext_address.executeUpdate();
					}
				}

			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}