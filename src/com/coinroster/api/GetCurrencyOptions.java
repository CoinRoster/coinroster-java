package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class GetCurrencyOptions extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public GetCurrencyOptions(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			JSONArray currency_options = new JSONArray();
			
			//only works because the the sql limit 5
			PreparedStatement select_currencies = sql_connection.prepareStatement("select * from fx where not symbol = 'BTCUSD' order by symbol asc limit 5");
			ResultSet result_set = select_currencies.executeQuery();
			
			while (result_set.next())
				{
				String symbol = result_set.getString(1);
				double last_price = result_set.getDouble(3);
				String description = result_set.getString(5);
				
				JSONObject currency = new JSONObject();
				
				currency.put("symbol", symbol);
				currency.put("description", description);
				currency.put("last_price", last_price);
				
				currency_options.put(currency);
				}
			Utils.log("passing currency_options json");
			output.put("currency_options", currency_options);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}