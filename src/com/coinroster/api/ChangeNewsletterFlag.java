package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

/**
 * Flip newsletter flag.
 * 
 * @custom.access standard
 *
 */
public class ChangeNewsletterFlag extends Utils
	{
	public static String method_level = "standard";
	
	/**
	 * Flip newsletter flag.
	 * 
	 * @param method.input.current_flag_state
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	public ChangeNewsletterFlag(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		method : {
			
//------------------------------------------------------------------------------------
	
			int 
			
			current_flag_state = input.getInt("current_flag_state"), // 1 for subscribed, 0 for unsubscribed
			new_flag_state = current_flag_state == 0 ? 1 : 0;

			PreparedStatement set_newsletter_flag = sql_connection.prepareStatement("update user set newsletter_flag = ? where id = ?");
			set_newsletter_flag.setInt(1, new_flag_state);
			set_newsletter_flag.setString(2, session.user_id());
			set_newsletter_flag.executeUpdate();
			
			output.put("newsletter_flag", new_flag_state);

			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}