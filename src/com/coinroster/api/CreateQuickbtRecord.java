package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

/**
 * Create a new QuickBT record.
 * 
 * @deprecated now just sends amount to bitcoin address with blockcypher API
 *
 */
public class CreateQuickbtRecord extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public CreateQuickbtRecord(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			String
			
			user_id = session.user_id(),
			passed_id = Server.generate_key(user_id);
			
			PreparedStatement create_quickbt_record = sql_connection.prepareStatement("insert into quickbt(passed_id, user_id, created) values(?, ?, ?)");
			create_quickbt_record.setString(1, passed_id);
			create_quickbt_record.setString(2, user_id);
			create_quickbt_record.setLong(3, System.currentTimeMillis());
			create_quickbt_record.executeUpdate();

			output.put("redirect", Server.host + "/account/deposit_rcv.html");
			output.put("passed_id", passed_id);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}