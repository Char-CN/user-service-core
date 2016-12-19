package org.blazer.userservice.core.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.blazer.userservice.core.model.SessionModel;
import org.blazer.userservice.core.util.HttpUtil;
import org.blazer.userservice.core.util.SessionUtil;
import org.blazer.userservice.core.util.StringUtil;

/**
 * 需要在web.xml中配置如下字段信息:onOff、systemName、serviceUrl、innerServiceUrl、
 * noPermissionsPage、cookieSeconds、ignoreUrls
 * 
 * @author hyy
 *
 */
public class PermissionsFilter implements Filter {

	public static final String SESSION_KEY = "US_SESSION_ID";
	public static final String NAME_KEY = "US_USER_NAME";
	public static final String NAME_CN_KEY = "US_USER_NAME_CN";
	public static final String DOMAIN_REG = "[http|https]://.*([.][a-zA-Z0-9]*[.][a-zA-Z0-9]*)/*.*";
	private static String systemName = null;
	private static String serviceUrl = null;
	private static String innerServiceUrl = null;
	private static String noPermissionsPage = null;
	private static String templateJs = null;
	private static HashSet<String> ignoreUrlsSet = null;
	private static HashSet<String> ignoreUrlsPrefixSet = null;
	private static Integer cookieSeconds = null;
	private static boolean onOff = false;
	private static String doCheckUrl = null;

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		if (!onOff) {
			chain.doFilter(req, resp);
			return;
		}
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		String url = request.getRequestURI();
		if (!"".equals(request.getContextPath())) {
			url = url.replaceFirst(request.getContextPath(), "");
		}
		System.out.println("action url : " + url);
		// 访问userservice服务不需要经过权限认证
		for (String perfix : ignoreUrlsPrefixSet) {
			if (url.startsWith(perfix)) {
				chain.doFilter(req, resp);
				return;
			}
		}
		// web.xml配置的过滤页面以及强制过滤/login.html和pwd.html
		if (ignoreUrlsSet.contains(url)) {
			chain.doFilter(req, resp);
			return;
		}
		try {
			// StringBuilder requestUrl = new StringBuilder(innerServiceUrl);
			// requestUrl.append("/userservice/checkurl.do?");
			// requestUrl.append(SESSION_KEY).append("=").append(sessionid);
			// requestUrl.append("&").append("systemName").append("=").append(systemName);
			// requestUrl.append("&").append("url").append("=").append(url);
//			String sessionid = getSessionId(request);
//			String requestUrl = String.format(doCheckUrl, sessionid, url);
//			String content = HttpUtil.executeGet(requestUrl.toString());
//			System.out.println("验证Url：" + requestUrl);
//			System.out.println("验证结果：" + content);
//			String[] contents = content.split(",", 3);
//			if (contents.length != 3) {
//				System.err.println("验证提示：服务器返回结果的长度不对。");
//			}
//			delay(request, response, contents[2]);
//			// no login
//			if ("false".equals(contents[0])) {
//				System.err.println("验证提示：没有登录。");
//				// 这样跳转解决了，页面中间嵌套页面的问题。
//				// String script = "<script>window.location.href = '" +
//				// serviceUrl + "/tologin.html?url=' +
//				// encodeURIComponent(location.href);</script>";
//				String script = "<script>";
//				script += "alert('您的身份已失效，请重新登录!');";
//				script += "window.location.href = '" + serviceUrl + "/login.html?url=' + encodeURIComponent(location.href);";
//				script += "</script>";
//				System.err.println(script);
//				response.setContentType("text/html;charset=utf-8");
//				response.getWriter().println(script);
//				return;
//			}
//			// no permissions
//			if ("false".equals(contents[1])) {
//				System.err.println("验证提示：没有权限。");
//				response.sendRedirect(noPermissionsPage);
//				return;
//			}
			CheckUrlStatus cus = checkUrl(request, response, url);
			if (cus == CheckUrlStatus.FailToRstLengthError) {
				System.err.println("验证提示：服务器返回结果的长度不对。");
				return;
			} else if (cus == CheckUrlStatus.FailToNoLogin) {
				System.err.println("验证提示：没有登录。");
				return;
			} else if (cus == CheckUrlStatus.FailToNoPermissions) {
				System.err.println("验证提示：没有权限。");
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("验证userservice出现错误。。。");
			response.sendRedirect(noPermissionsPage);
			return;
		}
		chain.doFilter(req, resp);
	}

	public CheckUrlStatus checkUrl(HttpServletRequest request, HttpServletResponse response, String url) throws Exception {
		String sessionid = getSessionId(request);
		String requestUrl = String.format(doCheckUrl, sessionid, url);
		String content = HttpUtil.executeGet(requestUrl.toString());
		String[] contents = content.split(",", 3);
		if (contents.length != 3) {
			return CheckUrlStatus.FailToRstLengthError;
		}
		delay(request, response, contents[2]);
		// no login
		if ("false".equals(contents[0])) {
			// 这样跳转解决了，页面中间嵌套页面的问题。
			String script = "<script>";
			script += "alert('您的身份已失效，请重新登录!');";
			script += "window.location.href = '" + serviceUrl + "/login.html?url=' + encodeURIComponent(location.href);";
			script += "</script>";
			response.setContentType("text/html;charset=utf-8");
			response.getWriter().println(script);
			return CheckUrlStatus.FailToNoLogin;
		}
		// no permissions
		if ("false".equals(contents[1])) {
			response.sendRedirect(noPermissionsPage);
			return CheckUrlStatus.FailToNoPermissions;
		}
		return CheckUrlStatus.SUCCESS;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		systemName = filterConfig.getInitParameter("systemName");
		serviceUrl = filterConfig.getInitParameter("serviceUrl");
		innerServiceUrl = filterConfig.getInitParameter("innerServiceUrl");
		if (innerServiceUrl == null) {
			innerServiceUrl = serviceUrl;
		}
		noPermissionsPage = filterConfig.getInitParameter("noPermissionsPage");
		try {
			if (noPermissionsPage == null || filterConfig.getServletContext().getResource("/" + noPermissionsPage) == null) {
				System.err.println("noPermissionsPage没有配置或找不到该文件。");
				noPermissionsPage = serviceUrl + "/nopermissions.html";
			}
		} catch (Exception e) {
			System.err.println("初始化noPermissionsPage出错。" + e.getMessage());
		}
		templateJs = filterConfig.getInitParameter("templateJs");
		onOff = "1".equals(filterConfig.getInitParameter("on-off"));
		try {
			cookieSeconds = Integer.parseInt(filterConfig.getInitParameter("cookieSeconds"));
		} catch (Exception e) {
			System.err.println("初始化cookie时间出错。");
		}
		// 过滤的URL
		ignoreUrlsSet = new HashSet<String>();
		ignoreUrlsPrefixSet = new HashSet<String>();
		// 强制过滤/login.html和/pwd.html
		// ignoreUrlsSet.add("/tologin.html");
		// ignoreUrlsSet.add("/login.html");
		// ignoreUrlsSet.add("/pwd.html");
		String ignoreUrls = filterConfig.getInitParameter("ignoreUrls");
		if (ignoreUrls != null && !"".equals(ignoreUrls)) {
			String[] urls = ignoreUrls.split(",");
			// 过滤url
			for (String url : urls) {
				// 格式：/userservice/*
				// 如URL：/userservice/checkurl.do
				// 即访问userservice服务不需要经过权限认证
				if (url.endsWith("*")) {
					ignoreUrlsPrefixSet.add(url.replaceAll("[*]", ""));
				} else {
					ignoreUrlsSet.add(url);
				}
			}
		}
		// url 处理
		doCheckUrl = serviceUrl + "/userservice/checkurl.do?systemName=" + systemName + "&" + SESSION_KEY + "=%s&url=%s";
		System.out.println("初始化配置：on-off              : " + onOff);
		System.out.println("初始化配置：systemName          : " + systemName);
		System.out.println("初始化配置：serviceUrl          : " + serviceUrl);
		System.out.println("初始化配置：innerServiceUrl     : " + innerServiceUrl);
		System.out.println("初始化配置：noPermissionsPage   : " + noPermissionsPage);
		System.out.println("初始化配置：cookieSeconds       : " + cookieSeconds);
		System.out.println("初始化配置：ignoreUrls          : " + ignoreUrls);
		System.out.println("初始化配置：ignoreUrlsSet       : " + ignoreUrlsSet);
		System.out.println("初始化配置：ignoreUrlsPrefixSet : " + ignoreUrlsPrefixSet);
		System.out.println("初始化配置：doCheckUrl          : " + doCheckUrl);
		if (ignoreUrlsPrefixSet.size() > 10) {
			System.out.println("【提示】：ignoreUrlsPrefixSet的值大于10个，将会严重影响系统性能。");
		} else if (ignoreUrlsPrefixSet.size() > 5) {
			System.out.println("【提示】：ignoreUrlsPrefixSet的值大于5个，将会影响系统性能。");
		}
		if (templateJs != null && !"".equals(templateJs)) {
			for (String keyValue : templateJs.split(",")) {
				String[] kv = keyValue.split(":");
				if (kv.length != 2) {
					System.err.println("过滤错误JS生成配置：" + keyValue);
					continue;
				}
				// 根据模板生成文件
				newTemplate(filterConfig, kv[0], kv[1]);
			}
		} else {
			System.out.println("初始化配置：没有需要生成的JS模板");
		}
		System.out.println("初始化配置：success by source   : " + this.getClass().getPackage());
	}

	private void newTemplate(FilterConfig filterConfig, String template, String js) {
		String filePath = null;
		try {
			filePath = filterConfig.getServletContext().getResource("/").getPath();
			filePath = filePath.substring(0, filePath.length() - 1);
			if (filterConfig.getServletContext().getResource(template) == null) {
				System.out.println("初始化配置：找不到js模板          : " + filePath + template);
			} else {
				System.out.println("初始化配置：js模板路径            : " + filePath + template);
				System.out.println("初始化配置：新生成js文件          : " + filePath + js);
				BufferedReader br = null;
				OutputStreamWriter osw = null;
				try {
					br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath + template), "UTF-8"));
					osw = new OutputStreamWriter(new FileOutputStream(new File(filePath + js)), "utf-8");
					for (String line = br.readLine(); line != null; line = br.readLine()) {
						// 替换js文件模板内容变量
						line = line.replace("${serviceUrl}", serviceUrl);
						line = line.replace("${SESSION_KEY}", SESSION_KEY);
						line = line.replace("${NAME_KEY}", NAME_KEY);
						line = line.replace("${NAME_CN_KEY}", NAME_CN_KEY);
						line = line.replace("${DOMAIN_REG}", DOMAIN_REG);
						osw.append(line + "\n");
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (br != null) {
						try {
							br.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					if (osw != null) {
						try {
							// fw.close();
							osw.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void destroy() {
	}

	public static void delay(HttpServletRequest request, HttpServletResponse response, String newSession) throws UnsupportedEncodingException {
		if ("".equals(newSession)) {
			newSession = null;
		}
		String domain = getDomain(request);
		if (domain == null) {
			System.out.println("delay error ~ domain is null ~ new session : " + newSession);
			return;
		}
		System.out.println("delay ~ [" + domain + "] ~ new session : " + newSession);
		Cookie key = new Cookie(SESSION_KEY, newSession);
		key.setPath("/");
		key.setDomain(domain);
		key.setMaxAge(cookieSeconds);
		response.addCookie(key);
		SessionModel model = SessionUtil.decode(newSession);
		if (model.isValid()) {
			Cookie userNameCn = new Cookie(NAME_CN_KEY, URLEncoder.encode(model.getUserNameCn(), "UTF-8"));
			userNameCn.setPath("/");
			userNameCn.setDomain(domain);
			userNameCn.setMaxAge(cookieSeconds);
			response.addCookie(userNameCn);
			Cookie userName = new Cookie(NAME_KEY, URLEncoder.encode(model.getUserName(), "UTF-8"));
			userName.setPath("/");
			userName.setDomain(domain);
			userName.setMaxAge(cookieSeconds);
			response.addCookie(userName);
		}
	}

	public static String getDomain(HttpServletRequest request) {
		return StringUtil.findOneStrByReg(request.getRequestURL().toString(), DOMAIN_REG);
	}

	public static String getSessionId(HttpServletRequest request) {
		String sessionValue = request.getParameter(SESSION_KEY);
		if (sessionValue != null) {
			System.out.println(SESSION_KEY + " 从 request 中取值 : " + sessionValue);
			return sessionValue;
		}
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (SESSION_KEY.equals(cookie.getName())) {
					System.out.println(SESSION_KEY + " 从 cookie  中取值 : " + cookie.getValue());
					return cookie.getValue();
				}
			}
		}
		System.out.println(SESSION_KEY + " 从 cookie  中取值 : null");
		return null;
	}

	public static SessionModel getSessionModel(HttpServletRequest request) {
		String sessionStr = getSessionId(request);
		SessionModel sessionModel = SessionUtil.decode(sessionStr);
		return sessionModel;
	}

	public static String getSystemName() {
		return systemName;
	}

	public static String getServiceUrl() {
		return serviceUrl;
	}

	public static String getInnerServiceUrl() {
		return innerServiceUrl;
	}

	public static String getNoPermissionsPage() {
		return noPermissionsPage;
	}

	public static Integer getCookieSeconds() {
		return cookieSeconds;
	}

	public static boolean isOnOff() {
		return onOff;
	}

}
