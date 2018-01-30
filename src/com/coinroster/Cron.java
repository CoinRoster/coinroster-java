package com.coinroster;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.api.BasketballBot;
import com.coinroster.internal.*;

public class Cron 
	{
	@SuppressWarnings("unused")
	private static int
	
	year,
	month,
	day_of_month,
	day_of_week, 
	hour,
	minute,
	second;

	protected Cron(String freq, Calendar cal) throws Exception
		{
		year = cal.get(Calendar.YEAR);
		month = cal.get(Calendar.MONTH);
		day_of_month = cal.get(Calendar.DAY_OF_MONTH);
		day_of_week = cal.get(Calendar.DAY_OF_WEEK);
		hour = cal.get(Calendar.HOUR_OF_DAY);
		minute = cal.get(Calendar.MINUTE);
		second = cal.get(Calendar.SECOND);
		
		this.getClass().getDeclaredMethod(freq).invoke(this);
		}

	@SuppressWarnings("unused")
	private void second() throws Exception
		{ 
		
		}
	
	@SuppressWarnings("unused")
	private void minute() throws Exception{ 
		SessionExpiry();
		if (!Server.dev_server) UpdateBTCUSD();
		
		if(Server.dev_server){
			if((minute%20)==0){
				//see if ANY basketball contests are in play (status=2)
				Connection sql_connection = Server.sql_connection();
				DB db_connection = new DB(sql_connection);
				ArrayList<Integer> contest_ids = db_connection.check_if_in_play("FANTASYSPORTS", "BASKETBALL", "ROSTER");
				if(!contest_ids.isEmpty()){
					BasketballBot ball_bot = new BasketballBot();
					log("Contest is in play and minute is multiple of 20");
					ArrayList<String> gameIDs = ball_bot.getAllGameIDsDB();
					boolean games_ended;
					games_ended = ball_bot.scrape(gameIDs);
					for(Integer contest_id : contest_ids ){
						ball_bot.updateScores(contest_id, sql_connection);
					}
					if(games_ended){
						log("games have ended");
						for(Integer contest_id : contest_ids){
							ball_bot.settleContest(contest_id, sql_connection);
						}
					}
				}
			}
		}
		
	}
			 
	
	@SuppressWarnings("unused")
	private void hour() throws Exception
	{
		new CloseContestRegistration();
		new ExpirePromos();
//		new CheckPendingWithdrawals();
		if (!Server.dev_server) UpdateCurrencies();
		
		if(Server.dev_server){
			if(hour==6){
				
				System.out.println("6AM player table load");	
				BasketballBot ball_bot = new BasketballBot();
				ball_bot.scrapeGameIDs();
				ball_bot.setup();
				ball_bot.savePlayers();

				// parameters for contest
				String category = "FANTASYSPORTS";
				String contest_type = "ROSTER";
				Long deadline = ball_bot.getEarliestGame();
	            LocalDate date = Instant.ofEpochMilli(deadline).atZone(ZoneId.systemDefault()).toLocalDate();
	            log("deadline for contest: " + date);
				String progressive_code = "";
				String title = "NBA Jackpot Contest | " + date.toString(); 
				String desc = "description";
	            double rake = 5.0;
	            double cost_per_entry = 0.0001;
	            String settlement_type = "JACKPOT";
	            int salary_cap = 1000;
	            int min_users = 3;
	            int max_users = 0; // 0 = unlimited
	            int entries_per_user = 0;
	            int roster_size = 8;
	            String score_header = "Points";
	            String odds_source = "n/a";
	            double[] payouts = {0.6, 0.2, 0.15, 0.05};
	            ResultSet playerIDs = BasketballBot.getAllPlayerIDs();
	            BasketballBot.createContest(category, contest_type, progressive_code, title, desc, rake, cost_per_entry, settlement_type, salary_cap, min_users, max_users, entries_per_user, roster_size, odds_source, score_header, payouts, playerIDs, deadline);
			
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void day() throws Exception
	{
		GenerateAddresses();
		PurgePasswordResetTable();
		TriggerBuildLobby();
		
		if (Server.dev_server)
		{
			UpdateBTCUSD();
			UpdateCurrencies();
			
		}
					
		
	}
	

//------------------------------------------------------------------------------------

	// kick off stale sessions
	
	private void SessionExpiry()
		{
		try {
			for (Entry<String, String[]> entry : Server.session_map.entrySet()) 
				{
				String session_token = entry.getKey();
				
				String[] session_vars = Server.session_map.get(session_token);
				
				String
				
				/*username = session_vars[0],
				user_id = session_vars[1],*/
				user_level = session_vars[2];
				
				if (user_level.equals("1"))
					{
					Long last_active = Long.parseLong(session_vars[3]);
					if (System.currentTimeMillis() - last_active >= Server.admin_timeout) Server.kill_session(session_token);
					}
				}
			}
		catch (Exception e)
			{
			Server.exception(e);
			}
		}	
	
//------------------------------------------------------------------------------------

	// kick off stale sessions
	
	private void GenerateAddresses()
		{
		Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			
			PreparedStatement count_free_addresses = sql_connection.prepareStatement("select count(*) from cgs where cr_account is null");
			ResultSet result_set = count_free_addresses.executeQuery();

			int free_address_count = 0;
			
			if (result_set.next()) free_address_count = result_set.getInt(1);
			
			int address_shortage = Server.free_address_quota - free_address_count;
			
			if (address_shortage == 0) return;

			JSONObject rpc_call = new JSONObject();
			
			rpc_call.put("method", "newAccount");
			
			JSONObject rpc_method_params = new JSONObject();
			
			rpc_method_params.put("type", "btc");
			
			rpc_call.put("params", rpc_method_params);
			
			while (address_shortage-- > 0)
				{
				CallCGS call = new CallCGS(rpc_call);
				
				JSONObject error = call.get_error();
				
				if (error != null) 
					{
					Server.log("ERROR GENERATING ADDRESS");
					Server.log(error.toString());
					}
				
				Thread.sleep(30000);
				}
			} 
		catch (Exception e) 
			{
			Server.exception(e);
			} 
		finally
			{
			if (sql_connection != null)
				{
				try {sql_connection.close();} 
				catch (SQLException ignore) {}
				}
			}
		}	

//------------------------------------------------------------------------------------

	// update BTCUSD price
	
	private void UpdateBTCUSD()
		{
		Server.async_updater.execute(new Runnable() 
			{
		    public void run() 
		    	{
		    	Connection sql_connection = null;
				try {
			    	List<String> Bitfinex_BTCUSD_raw = Utils.get_page("https://api.bitfinex.com/v2/ticker/tBTCUSD");
			    	
			    	// the first line is the entire array:
			    	
			    	JSONArray Bitfinex_BTCUSD_JSON = new JSONArray(Bitfinex_BTCUSD_raw.get(0)); 
			    	
			    	/* [
			    	
			    	     1 : SYMBOL,
			    	     2 : BID, 
			    	     3 : BID_SIZE, 
			    	     4 : ASK, 
			    	     5 : ASK_SIZE, 
			    	     6 : DAILY_CHANGE, 
			    	     7 : DAILY_CHANGE_PERC, 
			    	     8 : LAST_PRICE, 
			    	     9 : VOLUME, 
			    	    10 : HIGH, 
			    	    11 : LOW
			    	    
			    	] */
			    	
			    	double Bitfinex_BTCUSD_last_price = Bitfinex_BTCUSD_JSON.getDouble(8);
					
					sql_connection = Server.sql_connection();
					DB db = new DB(sql_connection);
					db.update_fx("BTCUSD", Bitfinex_BTCUSD_last_price, "Bitfinex", "Bitcoin in US Dollar");
					} 
				catch (Exception e) 
					{
					Server.exception(e);
					} 
				finally
					{
					if (sql_connection != null)
						{
						try {sql_connection.close();} 
						catch (SQLException ignore) {}
						}
					}
		    	}
			});
		}	

//------------------------------------------------------------------------------------

	// update BTCUSD price
	
	private void UpdateCurrencies()
		{
		Server.async_updater.execute(new Runnable() 
			{
		    public void run() 
		    	{
		    	Connection sql_connection = null;
				try {
					StringBuilder builder = new StringBuilder();
					
					for (String[] currency : currencies) builder.append(currency[2] + "+");
					String symbol_string = builder.toString();
					symbol_string = symbol_string.substring(0,symbol_string.length() - 1);
					
					URL obj = new URL("http://download.finance.yahoo.com/d/quotes.csv?s=" + symbol_string + "&f=l1");
					HttpURLConnection con = (HttpURLConnection) obj.openConnection();
					con.setRequestMethod("GET");
					con.setRequestProperty("User-Agent", "CoinRoster.com - We respect your API and make requests once per hour");
				
					BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
					
					String line;
				
					int ctr = 0;

					sql_connection = Server.sql_connection();
					DB db = new DB(sql_connection);
					
					while ((line = in.readLine()) != null)
						{
						if (line.contains("N/A")) continue;
						try {
							String
							
							description = currencies[ctr][0], // e.g. Canadian Dollar
							symbol = currencies[ctr][1]; // e.g. CAD
							
							double 
							
							price = Double.parseDouble(line),
							last_price = db.get_last_price(symbol);
							
							if (price != last_price && price != 0) db.update_fx(symbol, price, "Yahoo Finance", description);
							}
						catch (Exception e)
							{
							Server.exception(e);
							}
						ctr++;
						}
				
					in.close();
					} 
				catch (Exception e) 
					{
					Server.exception(e);
					} 
				finally
					{
					if (sql_connection != null)
						{
						try {sql_connection.close();} 
						catch (SQLException ignore) {}
						}
					}
		    	}
			});
		}	
	
//------------------------------------------------------------------------------------

	// purge any password reset keys that have not been used
	
	private void PurgePasswordResetTable()
		{
    	Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			
			Long expiry_cutoff = System.currentTimeMillis() - Server.hour;
			
			PreparedStatement delete_password_reset = sql_connection.prepareStatement("delete from password_reset where created < ?");
			delete_password_reset.setLong(1, expiry_cutoff);
			delete_password_reset.executeUpdate();
			} 
		catch (Exception e) 
			{
			Server.exception(e);
			} 
		finally
			{
			if (sql_connection != null)
				{
				try {sql_connection.close();} 
				catch (SQLException ignore) {}
				}
			}
		}
	
//------------------------------------------------------------------------------------

	// trigger BuildLobby (called daily)
	
	private void TriggerBuildLobby()
		{
    	Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			new BuildLobby(sql_connection);
			} 
		catch (Exception e) 
			{
			Server.exception(e);
			} 
		finally
			{
			if (sql_connection != null)
				{
				try {sql_connection.close();} 
				catch (SQLException ignore) {}
				}
			}
		}	
	
//------------------------------------------------------------------------------------

	private static String[][] currencies = new String[][]{
		
		{"Albanian Lek","ALL","USDALL=X"},
		{"Afghan Afghani","AFN","USDAFN=X"},
		{"Argentine Peso","ARS","USDARS=X"},
		{"Aruban Guilder","AWG","USDAWG=X"},
		{"Australian Dollar","AUD","USDAUD=X"},
		{"Azerbaijan Manat","AZN","USDAZN=X"},
		{"Bahamian Dollar","BSD","USDBSD=X"},
		{"Barbadian Dollar","BBD","USDBBD=X"},
		{"Belarusian Ruble","BYN","USDBYN=X"},
		{"Belize Dollar","BZD","USDBZD=X"},
		{"Bermudian Dollar","BMD","USDBMD=X"},
		{"Bolivian Bol�viano","BOB","USDBOB=X"},
		{"Bosnia and Herzegovina Convertible Marka","BAM","USDBAM=X"},
		{"Botswana Pula","BWP","USDBWP=X"},
		{"Bulgarian Lev","BGN","USDBGN=X"},
		{"Brazilian Real","BRL","USDBRL=X"},
		{"British Pound","GBP","USDGBP=X"},
		{"Brunei Dollar","BND","USDBND=X"},
		{"Cambodian Riel","KHR","USDKHR=X"},
		{"Canadian Dollar","CAD","USDCAD=X"},
		{"Cayman Islands Dollar","KYD","USDKYD=X"},
		{"Chilean Peso","CLP","USDCLP=X"},
		{"Chinese Yuan Renminbi","CNY","USDCNY=X"},
		{"Colombian Peso","COP","USDCOP=X"},
		{"Costa Rican Colon","CRC","USDCRC=X"},
		{"Croatian Kuna","HRK","USDHRK=X"},
		{"Cuban Peso","CUP","USDCUP=X"},
		{"Czech Koruna","CZK","USDCZK=X"},
		{"Danish Krone","DKK","USDDKK=X"},
		{"Dominican Republic Peso","DOP","USDDOP=X"},
		{"East Caribbean Dollar","XCD","USDXCD=X"},
		{"Egyptian Pound","EGP","USDEGP=X"},
		{"European Euro","EUR","USDEUR=X"},
		{"Falkland Islands Pound","FKP","USDFKP=X"},
		{"Fijian Dollar","FJD","USDFJD=X"},
		{"Ghanaian Cedi","GHS","USDGHS=X"},
		{"Gibraltar Pound","GIP","USDGIP=X"},
		{"Guatemalan Quetzal","GTQ","USDGTQ=X"},
		{"Guyanese Dollar","GYD","USDGYD=X"},
		{"Honduran Lempira","HNL","USDHNL=X"},
		{"Hong Kong Dollar","HKD","USDHKD=X"},
		{"Hungarian Forint","HUF","USDHUF=X"},
		{"Icelandic Krona","ISK","USDISK=X"},
		{"Indian Rupee","INR","USDINR=X"},
		{"Indonesian Rupiah","IDR","USDIDR=X"},
		{"Iranian Rial","IRR","USDIRR=X"},
		{"Israeli Shekel","ILS","USDILS=X"},
		{"Jamaican Dollar","JMD","USDJMD=X"},
		{"Japanese Yen","JPY","USDJPY=X"},
		{"Kazakhstani Tenge","KZT","USDKZT=X"},
		{"Korean Won (North)","KPW","USDKPW=X"},
		{"Korean Won (South)","KRW","USDKRW=X"},
		{"Kyrgyzstani Som","KGS","USDKGS=X"},
		{"Lao Kip","LAK","USDLAK=X"},
		{"Lebanese Pound","LBP","USDLBP=X"},
		{"Liberian Dollar","LRD","USDLRD=X"},
		{"Macedonian Denar","MKD","USDMKD=X"},
		{"Malaysian Ringgit","MYR","USDMYR=X"},
		{"Mauritanian Rupee","MUR","USDMUR=X"},
		{"Mexican Peso","MXN","USDMXN=X"},
		{"Mongolian Tughrik","MNT","USDMNT=X"},
		{"Mozambican Metical","MZN","USDMZN=X"},
		{"Namibian Dollar","NAD","USDNAD=X"},
		{"Nepalese Rupee","NPR","USDNPR=X"},
		{"New Zealand Dollar","NZD","USDNZD=X"},
		{"Nicaraguan Cordoba","NIO","USDNIO=X"},
		{"Nigerian Naira","NGN","USDNGN=X"},
		{"Norwegian Krone","NOK","USDNOK=X"},
		{"Omani Rial","OMR","USDOMR=X"},
		{"Pakistani Rupee","PKR","USDPKR=X"},
		{"Paraguayan Guarani","PYG","USDPYG=X"},
		{"Peruvian Sol","PEN","USDPEN=X"},
		{"Philippine Peso","PHP","USDPHP=X"},
		{"Polish Zloty","PLN","USDPLN=X"},
		{"Qatari Riyal","QAR","USDQAR=X"},
		{"Romanian New Leu","RON","USDRON=X"},
		{"Russian Ruble","RUB","USDRUB=X"},
		{"Saint Helena Pound","SHP","USDSHP=X"},
		{"Saudi Arabian Riyal","SAR","USDSAR=X"},
		{"Serbian Dinar","RSD","USDRSD=X"},
		{"Seychellois Rupee","SCR","USDSCR=X"},
		{"Singapore Dollar","SGD","USDSGD=X"},
		{"Solomon Islands Dollar","SBD","USDSBD=X"},
		{"Somali Shilling","SOS","USDSOS=X"},
		{"South African Rand","ZAR","USDZAR=X"},
		{"Sri Lankan Rupee","LKR","USDLKR=X"},
		{"Swedish Krona","SEK","USDSEK=X"},
		{"Swiss Franc","CHF","USDCHF=X"},
		{"Syrian Pound","SYP","USDSYP=X"},
		{"Taiwanese New Dollar","TWD","USDTWD=X"},
		{"Thai Baht","THB","USDTHB=X"},
		{"Trinidad and Tobago Dollar","TTD","USDTTD=X"},
		{"Turkish Lira","TRY","USDTRY=X"},
		{"Ukrainian Hryvnia","UAH","USDUAH=X"},
		{"US Dollar","USD","USDUSD=X"},
		{"Uruguayan Peso","UYU","USDUYU=X"},
		{"Uzbekistani Som","UZS","USDUZS=X"},
		{"Venezuelan Bolivar","VEF","USDVEF=X"},
		{"Vietnamese Dong","VND","USDVND=X"},
		{"Yemeni Rial","YER","USDYER=X"}
		
	};
	
	
	public static void log(Object msg)
	{
	System.out.println(new SimpleDateFormat("MMM d h:mm:ss a").format(new Date()) + " : " + String.valueOf(msg));
	}

	
	
}
