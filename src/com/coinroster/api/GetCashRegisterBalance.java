package com.coinroster.api;

import java.sql.Connection;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

public class GetCashRegisterBalance {


	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public GetCashRegisterBalance(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);
		
		method : {
			
//------------------------------------------------------------------------------------
			HttpResponse<String> response = Unirest.get("https://api.blockcypher.com/v1/btc/main/addrs/1HjSwXFqL4B2GWB6Umt34PX9R69xvPRzUz")
					  .header("Cache-Control", "no-cache")
					  .asString();

			output.put("response", response.getBody());
			output.put("status", "1");
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
		
}
