/*
 *  Copyright (c) 2009, Steven Wang
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 *  twitterSina at http://twitterSina.appspot.com
 *  twitterSina code at http://twitterSina.googlecode.com
 * 	
 */
package twitterSina.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import twitterSina.Account;

/**
 * http公共处理类
 * @author Steven Wang <http://steven-wang.appspot.com>
 */
public class HttpHelp 
{
	public static String readBufferedContent(BufferedReader bufferedReader) 
	{   
		if (bufferedReader == null)   
			return null;   
		StringBuffer result = new StringBuffer();   
		String line = null;   
		try 
		{   
			while ((line = bufferedReader.readLine()) != null) 
			{
				result.append(line);   
			}   
		} 
		catch (IOException e)
		{   
			return null;   
		}   
		return result.toString();   
	}
	
	/**
	* 验证用户是否登录
	* @param HttpServletRequest，request对象
	* @return，是否验证成功
	*/
	@SuppressWarnings("unchecked")
	public static boolean checkLogin(HttpServletRequest request)
	{
		//首先从Session中判断
		if( request.getSession().getAttribute("accountList") != null)
		{
			return true;
		}
		else   //然后从cookie中判断
		{
			try
			{
				Cookie[] cookies = request.getCookies();
				for(Cookie cookie : cookies)
				{
					if(cookie.getName().equals("accountList"))
					{
						String value = URLDecoder.decode(cookie.getValue(), "utf-8") ;
						List<Account> accountList = (List<Account>)
						JsonSerializer.deserialize(value, Account.class);
						request.getSession().setAttribute("accountList",accountList);
						return true;
					}
				}
			}
			catch(Exception e)
			{
			}
		}
		request.getSession().setAttribute("accountList", null);
		return false;
	}
	
	/**
	* 对发布的消息进行处理(目前主要为处理消息中的短地址)
	* @param publishContent，发布的消息
	* @return，处理后的消息
	*/
	public static String processContent(String publishContent)
	{
		Pattern pattern = Pattern.compile("(((www\\.)|(http://)|(ftp://))[A-Z,a-z,0-9,\\p{Punct},\\+]+)\\s", Pattern.MULTILINE | Pattern.DOTALL);  
		Matcher matcher = pattern.matcher(publishContent);
		String url = "";
		while(matcher.find())
		{
			url += matcher.group();
		}
		url = url.trim();
		if(url.length() > 0)
		{
			String tinyurl = tinyUrl(url);
			publishContent = replace(publishContent, url, tinyurl);
		}
		return publishContent;
	}
	
	/**
	* 利用短地址服务，处理URL地址
	* @param url，原始地址
	* @return，处理后的短地址
	*/
	private static String tinyUrl(String url)
	{
		HttpURLConnection httpURLConnection = null;
		OutputStream httpOutputStream = null;
		String responseStr = null;
		try
		{
			URLConnection con = new URL("http://is.gd/api.php?longurl=" + url).openConnection();
			if(con != null)
			{
				httpURLConnection = (HttpURLConnection)con;
			}
			else
			{
				return url;
			}
			httpURLConnection.setRequestMethod("get");
			httpURLConnection.setDoOutput(true);
			httpOutputStream = httpURLConnection.getOutputStream();
	
			BufferedReader httpBufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));   
			responseStr = HttpHelp.readBufferedContent(httpBufferedReader);
	
			if (responseStr != null && responseStr.length() > 0 && responseStr.indexOf("http") != -1)
			{
				return responseStr;
			}
		}
		catch(Exception e)
		{
		}
		finally
		{
			try
			{
				httpOutputStream.close();
				httpURLConnection.disconnect();
			}
			catch(Exception e)
			{
			}
		}
		return url;
	}
	
	/**
	* 字符串替换函数
	* @param line，需要替换的字符串
	* @param oldString，替换前的字符串
	* @param newString，替换后的字符串
	* @return，处理后的字符串
	*/
	private static String replace(String line, String oldString, String newString)
	{
		if (line == null) 
		{
			return null;
		}
		int i = 0;
		if ((i = line.indexOf(oldString, i)) >= 0) 
		{
			char[] line2 = line.toCharArray();
			char[] newString2 = newString.toCharArray();
			int oLength = oldString.length();
			StringBuffer buf = new StringBuffer(line2.length);
			buf.append(line2, 0, i).append(newString2);
			i += oLength;
			int j = i;
			while ((i = line.indexOf(oldString, i)) > 0) 
			{
				buf.append(line2, j, i - j).append(newString2);
				i += oLength;
				j = i;
			}
			buf.append(line2, j, line2.length - j);
			return buf.toString();
		}
		return line;
	}
}
