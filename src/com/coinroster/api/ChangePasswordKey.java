package com.coinroster.api;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class ChangePasswordKey extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public ChangePasswordKey(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		method : {
			
//------------------------------------------------------------------------------------
		
			String 
			
			old_key = input.getString("key"),
			old_key_hash = Server.SHA1(old_key),
			
			new_key = input.getString("new_key"),
			new_key_hash = Server.SHA1(new_key);
			
			PreparedStatement get_passwords = sql_connection.prepareStatement("select * from passwords where key_hash = ?");
			get_passwords.setString(1, old_key_hash);
			ResultSet result_set = get_passwords.executeQuery();
			
			if (result_set.next())
				{
				byte[] 

				old_encrypted_blob = Server.inputstream_to_bytearray(result_set.getBinaryStream(2)),
				new_encrypted_blob = Server.encrypt(Server.decrypt(old_encrypted_blob, old_key), new_key);
				
				PreparedStatement update_passwords = sql_connection.prepareStatement("update passwords set key_hash = ?, data = ?");
				update_passwords.setString(1, new_key_hash);
				update_passwords.setBinaryStream(2, new ByteArrayInputStream(new_encrypted_blob), new_encrypted_blob.length);
				update_passwords.executeUpdate();
				
				output.put("status", "1");
				}
			else output.put("error", "Wrong key");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}