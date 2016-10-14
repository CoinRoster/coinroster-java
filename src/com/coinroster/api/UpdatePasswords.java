package com.coinroster.api;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class UpdatePasswords extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public UpdatePasswords(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
			String 
			
			key = input.getString("key"),
			key_hash = Server.SHA1(key);
			
			PreparedStatement get_passwords = sql_connection.prepareStatement("select * from passwords where key_hash = ?");
			get_passwords.setString(1, key_hash);
	    	ResultSet result_set = get_passwords.executeQuery();
	    	
	    	if (result_set.next())
		    	{
	    		String decrypted_blob = input.getString("blob");
	    				
	    		byte[] encrypted_blob = Server.encrypt(decrypted_blob.getBytes("UTF-8"), key);
				
				PreparedStatement update_passwords = sql_connection.prepareStatement("update passwords set data = ?");
				update_passwords.setBinaryStream(1, new ByteArrayInputStream(encrypted_blob), encrypted_blob.length);
				update_passwords.executeUpdate();
				
		        output.put("status", "1");
		    	}
	    	else output.put("error", "Wrong key");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}