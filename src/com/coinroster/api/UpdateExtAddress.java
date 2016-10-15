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
	@SuppressWarnings("unused")
	public UpdateExtAddress(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
			// !! SECURITY !! this method can be called by an admin OR standard user
			
			// an admin can use this method to update the cash register external address
			// a normal user can only change their external address if their BTC balance is 0.0

			// we could consider locking the user_xref record for the balance check and update
			// however, since withdrawals and deposits are manual, there's no real way to exploit
			
			boolean 
			
			update_for_active_user = input.getBoolean("update_for_active_user"),
			is_admin = session.user_level().equals("1");
			
			String 
			
			ext_address = input.getString("ext_address"),
			user_id = null;
	
			boolean do_update = false;
			
			if (update_for_active_user) 
				{
				user_id = session.user_id();
				String[] user_xref = db.select_user_xref("id", user_id);
				
				double user_btc_balance = Double.parseDouble(user_xref[1]);
				
				if (user_btc_balance == 0.0) do_update = true;
				else output.put("balance_not_zero", "1");
				}
			else if (is_admin) 
				{
				user_id = db.get_id_for_username("internal_cash_register");
				do_update = true;
				}
			
			if (do_update)
				{
				PreparedStatement update_ext_address = sql_connection.prepareStatement("update user_xref set ext_address = ? where id = ?");
				update_ext_address.setString(1, ext_address);
				update_ext_address.setString(2, user_id);
				update_ext_address.executeUpdate();
				}
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}