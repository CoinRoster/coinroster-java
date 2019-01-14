package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.UserMail;

/**
 * Deny a promo request from the admin panel.
 * 
 * @custom.access admin
 *
 */
public class DenyPromoRequest extends Utils
	{
	public static String method_level = "admin";
	
	/**
	 * Deny a promo request from the admin panel.
	 * 
	 * @param method.input.request_id
	 * @param method.input.reason
	 * @throws Exception
	 */
	public DenyPromoRequest(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			int request_id = input.getInt("request_id");
			String reason = input.getString("reason");
			
			if (reason.equals(""))
				{
				output.put("error", "No reason provided");
	            break method;
				}
			if (reason.length() > 200)
				{
				output.put("error", "Reason is too long");
	            break method;
				}
			
			JSONObject promo_request = db.select_promo_request(request_id);
			
			if (promo_request == null)
				{
				output.put("error", "Invalid promo request id");
	            break method;
				}
			
			String user_id = promo_request.getString("created_by");
			
			PreparedStatement deny_promo_request = sql_connection.prepareStatement("update promo_request set denied = 1, denied_by = ?, denied_reason = ? where id = ?");
			deny_promo_request.setString(1, session.user_id());
			deny_promo_request.setString(2, reason);
			deny_promo_request.setInt(3, request_id);
			deny_promo_request.executeUpdate();
			
			JSONObject user = db.select_user("id", user_id);
			
			String subject = "Your affiliate code request has been denied.",
			
			message_body = "Hi <b><!--USERNAME--></b>";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "Your affiliate code request has been denied for the following reason:";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "<b>" + reason + "</b>";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "If you disagree with this action, please reply to this email";
			
			new UserMail(user, subject, message_body);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}