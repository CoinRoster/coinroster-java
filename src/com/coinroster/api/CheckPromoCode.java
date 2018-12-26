package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

/**
 * Validate a supplied promo code.
 *
 */
public class CheckPromoCode extends Utils
	{
	public static String method_level = "standard";
	
	/**
	 * Validate a supplied promo code.
	 * 
	 * @param method.input.promo_code
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	public CheckPromoCode(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			String promo_code = no_whitespace(input.getString("promo_code"));

			if (db.select_promo(promo_code) != null) 
				{
				output.put("error", "Promo code [" + promo_code + "] already exists");
				break method;
				}
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}