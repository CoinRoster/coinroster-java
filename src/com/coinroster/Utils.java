package com.coinroster;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

public class Utils 
	{
	public static String to_valid_email(String email_address) throws Exception
		{
		email_address = no_whitespace(email_address).toLowerCase();

		if (is_valid_email(email_address)) return email_address;
		
		return null;
		}
	
	public static boolean is_valid_email(String email_address)
		{
		String email_regex = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
		Pattern pattern = Pattern.compile(email_regex);
		Matcher matcher = pattern.matcher(email_address);
		return matcher.matches();
		}
	
	public static boolean is_valid_btc(double amount)
		{
		if (amount == Double.parseDouble(format_btc(amount))) return true;
		return false;
		}
	
	public static String format_btc(double amount)
		{
		DecimalFormat btc_formatter = new DecimalFormat("#######0.00000000");
		return btc_formatter.format(amount);
		}
	
	public static String SHA1(String data) 
		{
		String digest = DigestUtils.sha1Hex(data);
		return digest;
		}

	protected static String no_whitespace(String string) throws Exception
		{
		string = string.replaceAll("\\s","");
		return string;
		}
	
	public static void log(Object msg)
		{
		System.out.println(new SimpleDateFormat("MMM d h:mm:ss a").format(new Date()) + " : " + String.valueOf(msg));
		}

	final static Charset ENCODING = StandardCharsets.UTF_8;
	
	public static List<String> read(String file_path) throws IOException 
		{
		Path path = Paths.get(file_path);
		return Files.readAllLines(path, ENCODING);
		}
	
	protected static void write(String file_path, String[] array) throws IOException 
		{
		Path path = Paths.get(file_path);
		try (BufferedWriter writer = Files.newBufferedWriter(path, ENCODING))
			{
			for(String line : array)
				{
				writer.write(line);
				writer.newLine();
				}
			}
		}

	protected static String read_to_string(String path) throws IOException 
		{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, StandardCharsets.UTF_8);
		}

	protected static void write_to_string(String file_path, String file_string) throws IOException 
		{
		BufferedWriter writer = new BufferedWriter(new FileWriter(file_path));
		writer.write(file_string);
		writer.close();
		}	
	
	protected static void write_bytes(String file_path, byte[] file_data) throws IOException 
		{
		FileOutputStream fos = new FileOutputStream(file_path);
		fos.write(file_data);
		fos.close();
		}
	
	public static byte[] base64_to_bytearray(String data) throws Exception 
		{
		return Base64.decodeBase64(data);
		}
	
	public static byte[] inputstream_to_bytearray(InputStream in) throws Exception
		{
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		byte[] data = new byte[1024];

		for (int n; (n = in.read(data, 0, data.length)) != -1;) buffer.write(data, 0, n);

		buffer.flush();

		return buffer.toByteArray();
		}
	
	public static List<String> get_page(String https_url) 
		{
		URL url;
		
		try {
			url = new URL(https_url);
			HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

			con.setRequestProperty("Accept", "text/html");
			con.setRequestProperty("Accept-Language", "en-US,en;");
			con.setRequestProperty("User-Agent", "CoinRoster.com - We respect your API and make requests once per minute");

			if (con != null) 
				{
				BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
				List<String> page = new ArrayList<String>();

				String line;

				while ((line = br.readLine()) != null) page.add(line);

				br.close();
				
				return page;
				}
			} 
		catch (Exception e) 
			{
			Server.exception(e);
			}
		
		return null;
		}
	
	final static int MAX_PRECISION = 8;
	public static double multiply(double in1, double in2, int max_precision)
		{
		if (max_precision == 0) max_precision = MAX_PRECISION;
		BigDecimal out = BigDecimal.valueOf(in1).multiply(BigDecimal.valueOf(in2));
		out = out.setScale(max_precision, RoundingMode.HALF_UP);
		return out.doubleValue();
		}
	public static double divide(double in1, double in2, int max_precision) // requires rounding in the actual divide function
		{
		if (max_precision == 0) max_precision = MAX_PRECISION;
		BigDecimal out = BigDecimal.valueOf(in1).divide(BigDecimal.valueOf(in2), max_precision, RoundingMode.HALF_UP);
		return out.doubleValue();
		}
	public static double add(double in1, double in2, int max_precision)
		{
		if (max_precision == 0) max_precision = MAX_PRECISION;
		BigDecimal out = BigDecimal.valueOf(in1).add(BigDecimal.valueOf(in2));
		out = out.setScale(max_precision, RoundingMode.HALF_UP);
		return out.doubleValue();
		}
	public static double subtract(double in1, double in2, int max_precision)
		{
		if (max_precision == 0) max_precision = MAX_PRECISION;
		BigDecimal out = BigDecimal.valueOf(in1).subtract(BigDecimal.valueOf(in2));
		out = out.setScale(max_precision, RoundingMode.HALF_UP);
		return out.doubleValue();
		}
	public static double btc_to_satoshi(double btc)
		{
		return multiply(btc, 100000000, 0);
		}
	public static double satoshi_to_btc(double satoshi)
		{
		return divide(satoshi, 100000000, 0);
		}
	}
