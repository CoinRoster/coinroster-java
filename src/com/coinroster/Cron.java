package com.coinroster;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONArray;

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
	private void minute() throws Exception
		{ 
		SessionExpiry();
		if (!Server.dev_server) UpdateBTCUSD();
		}
	
	@SuppressWarnings("unused")
	private void hour() throws Exception
		{
		new CloseContestRegistration();
		if (!Server.dev_server) UpdateCurrencies();
		}
	
	@SuppressWarnings("unused")
	private void day() throws Exception
		{
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
					
					URL obj = new URL("http://finance.yahoo.com/d/quotes.csv?s=" + symbol_string + "&f=l1");
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
		{"Bolivian Bolíviano","BOB","USDBOB=X"},
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
		{"NicaraguaN Cordoba","NIO","USDNIO=X"},
		{"NigeriaN Naira","NGN","USDNGN=X"},
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
		{"United States Dollar","USD","USDUSD=X"},
		{"Uruguayan Peso","UYU","USDUYU=X"},
		{"Uzbekistani Som","UZS","USDUZS=X"},
		{"Venezuelan Bolivar","VEF","USDVEF=X"},
		{"Vietnamese Dong","VND","USDVND=X"},
		{"Yemeni Rial","YER","USDYER=X"}
		
	};
	
	}
