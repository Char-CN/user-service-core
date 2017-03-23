package org.blazer.userservice.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class HttpUtil {

	public static final String HOST_REG = "[http|https]://([.a-zA-Z0-9]*)/*.*";

	public static void main(String[] args) {
		ping("http://bigdata.blazer.org");
		ping("http://bigdata.blazer.org", 1);
	}

	public static String getHost(String url) {
		String host = StringUtil.findOneStrByReg(url, HOST_REG);
		if (host == null) {
			return url;
		}
		return host;
	}

	/**
	 * 默认 5s
	 * @param url
	 * @return
	 */
	public static boolean ping(String url) {
		return ping(url, 5);
	}

	/**
	 * Ping命令使用
	 * @param url
	 * @param timeout 单位秒
	 * @return
	 */
	public static boolean ping(String url, int timeout) {
		String ip = getHost(url);
		System.out.print("ping [" + ip + "]");
		boolean rst = false;
		try {
			Runtime runtime = Runtime.getRuntime();
			Process process = null;
			String line = null;
			InputStream is = null;
			InputStreamReader isr = null;
			BufferedReader br = null;
			if (OsUtil.isLinux()) {
				process = runtime.exec("ping -w " + timeout + " " + ip);
			} else if (OsUtil.isMac()) {
				process = runtime.exec("ping -W " + timeout + " " + ip);
			} else {
				process = runtime.exec("ping -w " + timeout + " " + ip);
			}
			is = process.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			int count = 1;
			while ((line = br.readLine()) != null) {
				if (line.contains("unknown") || line.contains("UNKNOWN")) {
					rst = false;
					break;
				} else if (line.contains("timeout") || line.contains("TIMEOUT")) {
					rst = false;
					break;
				} else if (line.contains("ttl") || line.contains("TTL")) {
					rst = true;
					break;
				}
				if (count == 5) {
					rst = false;
					break;
				}
				count++;
			}
			is.close();
			isr.close();
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(" return : " + rst);
		return rst;
	}

	public static String executeGet(String url) throws ClientProtocolException, IOException {
		BufferedReader in = null;
		String content = null;
		try {
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpGet httpGet = new HttpGet(url);
			CloseableHttpResponse response = httpclient.execute(httpGet);
			in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				if (sb.length() != 0) {
					sb.append(NL);
				}
				sb.append(line);
			}
			in.close();
			content = sb.toString();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return content;
	}

}
