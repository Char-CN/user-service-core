package org.blazer.userservice.core.filter;

import java.io.IOException;
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

public class PermissionsFilter implements Filter {

	public static final String SESSION_KEY = "US_SESSION_ID";
	private static String systemName = null;
	private static String serviceUrl = null;
	private static String innerServiceUrl = null;
	private static String noPermissionsPage = null;
	private static HashSet<String> ignoreUrlsSet = null;
	private static Integer cookieSeconds = null;
	private static boolean onOff = false;

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
		String sessionid = getSessionId(request);
		// 访问userservice服务不需要经过权限认证
		if (url.startsWith("/userservice/")) {
			chain.doFilter(req, resp);
			return;
		}
		// web.xml配置的过滤页面以及强制过滤/login.html和pwd.html
		if (ignoreUrlsSet.contains(url)) {
			chain.doFilter(req, resp);
			return;
		}
		try {
			StringBuilder requestUrl = new StringBuilder(innerServiceUrl);
			requestUrl.append("/userservice/checkurl.do?");
			requestUrl.append(SESSION_KEY).append("=").append(sessionid);
			requestUrl.append("&").append("systemName").append("=").append(systemName);
			requestUrl.append("&").append("url").append("=").append(url);
			String content = HttpUtil.executeGet(requestUrl.toString());
			System.out.println("请求checkurl.do返回结果：" + content);
			String[] contents = content.split(",", 3);
			if (contents.length != 3) {
				System.err.println("请求checkurl.do返回：长度不对。");
			}
			delay(request, response, contents[2]);
			// no login
			if ("false".equals(contents[0])) {
				System.err.println("请求checkurl.do返回：没有登录。");
//				System.err.println(serviceUrl + "/tologin.html?url=" + URLEncoder.encode(request.getRequestURL().toString(), "UTF-8"));
				// response.sendRedirect(serviceUrl + "/tologin.html?url=" +
				// URLEncoder.encode(request.getRequestURL().toString(),
				// "UTF-8"));
				// 这样跳转解决了，页面中间嵌套页面的问题。
				System.err.println("<script>window.location.href = '"+serviceUrl+"/tologin.html?url=' + encodeURIComponent(location.href);</script>");
				response.getWriter().println("<script>window.location.href = '"+serviceUrl+"/tologin.html?url=' + encodeURIComponent(location.href);</script>");
				return;
			}
			// no permissions
			if ("false".equals(contents[1])) {
				System.err.println("请求checkurl.do返回：没有权限。");
				if (noPermissionsPage == null) {
					System.err.println("noPermissionsPage没有配置。");
					response.sendRedirect(serviceUrl + "/nopermissions.html");
					return;
				}
				response.sendRedirect(noPermissionsPage);
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

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		systemName = filterConfig.getInitParameter("systemName");
		serviceUrl = filterConfig.getInitParameter("serviceUrl");
		innerServiceUrl = filterConfig.getInitParameter("innerServiceUrl");
		if (innerServiceUrl == null) {
			innerServiceUrl = serviceUrl;
		}
		noPermissionsPage = filterConfig.getInitParameter("noPermissionsPage");
		onOff = "1".equals(filterConfig.getInitParameter("on-off"));
		try {
			cookieSeconds = Integer.parseInt(filterConfig.getInitParameter("cookieSeconds"));
		} catch (Exception e) {
			System.err.println("初始化cookie时间出错。");
		}
		// 过滤的URL
		ignoreUrlsSet = new HashSet<String>();
		// 强制过滤/login.html和/pwd.html
		ignoreUrlsSet.add("/tologin.html");
		ignoreUrlsSet.add("/login.html");
		ignoreUrlsSet.add("/pwd.html");
		String ignoreUrls = filterConfig.getInitParameter("ignoreUrls");
		if (ignoreUrls != null && !"".equals(ignoreUrls)) {
			String[] urls = ignoreUrls.split(",");
			// 过滤url
			for (String url : urls) {
				ignoreUrlsSet.add(url);
			}
		}
		System.out.println("init filter systemName : " + systemName);
		System.out.println("init filter serviceUrl : " + serviceUrl);
		System.out.println("init filter innerServiceUrl : " + innerServiceUrl);
		System.out.println("init filter noPermissionsPage : " + noPermissionsPage);
		System.out.println("init filter cookieSeconds : " + cookieSeconds);
		System.out.println("init filter on-off : " + onOff);
		System.out.println("init filter ignoreUrls : " + ignoreUrls);
		System.out.println("init filter ignoreUrlsMap : " + ignoreUrlsSet);
		System.out.println("初始化权限PermissionsFilter成功，源于 : " + this.getClass().getPackage());
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
			Cookie userNameCn = new Cookie("US_USER_NAME_CN", URLEncoder.encode(model.getUserNameCn(), "UTF-8"));
			userNameCn.setPath("/");
			userNameCn.setDomain(domain);
			userNameCn.setMaxAge(cookieSeconds);
			response.addCookie(userNameCn);
			Cookie userName = new Cookie("US_USER_NAME", URLEncoder.encode(model.getUserName(), "UTF-8"));
			userName.setPath("/");
			userName.setDomain(domain);
			userName.setMaxAge(cookieSeconds);
			response.addCookie(userName);
		}
	}

	public static String getDomain(HttpServletRequest request) {
		return StringUtil.findOneStrByReg(request.getRequestURL().toString(), "[http|https]://.*([.][a-zA-Z0-9]*[.][a-zA-Z0-9]*)/*.*");
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

}
