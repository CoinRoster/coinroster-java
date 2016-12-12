package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class UpdateExtAddress extends Utils
	{
	public static String method_level = "standard";
	public UpdateExtAddress(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			/* !! SECURITY !! this method can be called by an admin OR standard user
			
			an admin can use this method to update the cash register external address
			
			a standard user can only change their external address if their BTC balance is 0.0
			or if they disabled ext_address_secure_flag when they added their wallet

			we could consider locking the user_xref record for the balance check and update
			however, since withdrawals and deposits are manual, there's no real way to exploit

			*/
			
			String user_id = null;

			boolean 
			
			update_for_active_user = input.getBoolean("update_for_active_user"),
			do_update = false;
			
			if (!update_for_active_user) // update for cash register
				{
				if (session.user_level().equals("1")) // must be admin
					{
					user_id = db.get_id_for_username("internal_cash_register");
					do_update = true;
					}
				else break method;
				}
			
			else // update for active user
				{
				user_id = session.user_id();
				
				JSONObject user = db.select_user("id", user_id);
				
				String current_ext_address = user.getString("ext_address");

				int ext_address_secure_flag = user.getInt("ext_address_secure_flag");
				
				double user_btc_balance = user.getDouble("btc_balance");
				
				// a user can change their ext_address ...
				
				// 1) if current_ext_address has never been set before
				// 2) if security has been disabled
				// 3) if btc balance is 0.0
			
				if (current_ext_address == null || ext_address_secure_flag == 0 || user_btc_balance == 0.0) do_update = true;
				}
				
			if (do_update)
				{
				String ext_address = no_whitespace(input.getString("ext_address"));
				
				if (ext_address.length() == 0) break method;
				
				PreparedStatement update_ext_address = sql_connection.prepareStatement("update user set ext_address = ? where id = ?");
				update_ext_address.setString(1, ext_address);
				update_ext_address.setString(2, user_id);
				update_ext_address.executeUpdate();
				}
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}