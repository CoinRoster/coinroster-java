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

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
			String user_id = session.user_id();
				
			String[] user_xref = db.select_user_xref("id", user_id);

			int 
			
			user_supplied_secure_flag = input.getInt("ext_address_secure_flag"),
			current_secure_flag = Integer.parseInt(user_xref[10]);
			
			if (user_supplied_secure_flag != current_secure_flag)
				{
				// a user can change their ext_address_secure_flag ...
				
				// 1) if their BTC balance is 0.0
				// 2) if ext_address_secure_flag is being changed to 1 (enabled)

				double btc_balance = Double.parseDouble(user_xref[1]);

				if (user_supplied_secure_flag == 1 || btc_balance == 0.0)
					{
					PreparedStatement update_ext_address = sql_connection.prepareStatement("update user_xref set ext_address_secure_flag = ? where id = ?");
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