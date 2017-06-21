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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.ClientProtocolException;
import org.blazer.userservice.core.model.ChangeNode;
import org.blazer.userservice.core.model.CheckUrlStatus;
import org.blazer.userservice.core.model.SessionModel;
import org.blazer.userservice.core.model.UserModel;
import org.blazer.userservice.core.util.HttpUtil;
import org.blazer.userservice.core.util.SessionUtil;
import org.blazer.userservice.core.util.StringUtil;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 需要在web.xml中配置如下字段信息:
 * onOff、systemName、serviceUrl、noPermissionsPage、cookieSeconds、ignoreUrls、
 * templateJs
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
	private static ChangeNode serviceNode = null;
	private static String noPermissionsPage = null;
	private static String templateJs = null;
	private static HashSet<String> ignoreUrlsSet = null;
	private static HashSet<String> ignoreUrlsPrefixSet = null;
	private static Integer cookieSeconds = null;
	private static boolean onOff = false;
	private static String doCheckUrl = null;
	private static String doGetUserAll = null;
	private static String doGetUserByUserIds = null;
	private static int eCount = 0;

	private FilterConfig filterConfig;

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
		log("action url : " + url);
		try {
			CheckUrlStatus cus = checkUrlAndDelay(request, response, url);
			if (cus == CheckUrlStatus.FailToRstLengthError) {
				err("验证提示：服务器返回结果的长度不对。");
				return;
			} else if (cus == CheckUrlStatus.FailToNoLogin) {
				err("验证提示：没有登录。");
				// 这样跳转解决了，页面中间嵌套页面的问题。
				String script = "<script>alert('您的身份已失效，请重新登录!');";
				script += "window.location.href = '" + serviceUrl + "/login.html?url=' + encodeURIComponent(location.href);</script>";
				response.setContentType("text/html;charset=utf-8");
				response.getWriter().println(script);
				return;
			} else if (cus == CheckUrlStatus.FailToNoPermissions) {
				err("验证提示：没有权限。");
				response.sendRedirect(serviceUrl + noPermissionsPage + "?url=" + URLEncoder.encode(request.getRequestURL().toString(), "utf-8"));
				return;
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			++eCount;
			err("验证userservice出现错误。" + eCount);
			// response.sendRedirect(serviceUrl + noPermissionsPage + "?url=" +
			// URLEncoder.encode(request.getRequestURL().toString(), "utf-8"));
			response.setContentType("text/html;charset=utf-8");
			response.getWriter().println(e.getMessage() + "<br>UserService连接出错。");
			return;
		} catch (IOException e) {
			e.printStackTrace();
			++eCount;
			err("验证userservice出现错误。" + eCount);
			// response.sendRedirect(serviceUrl + noPermissionsPage + "?url=" +
			// URLEncoder.encode(request.getRequestURL().toString(), "utf-8"));
			response.setContentType("text/html;charset=utf-8");
			response.getWriter().println(e.getMessage() + "<br>UserService连接出错。");
			return;
		}
		chain.doFilter(req, resp);
	}

	private static CheckUrlStatus checkUrlAndDelay(HttpServletRequest request, HttpServletResponse response, String url) throws ClientProtocolException, IOException {
		String sessionId = getSessionId(request);
		String content = HttpUtil.executeGet(serviceUrl + String.format(doCheckUrl, sessionId, url));
		String[] contents = content.split(",", 3);
		if (contents.length != 3) {
			err(CheckUrlStatus.FailToRstLengthError + " : " + sessionId + " | " + getUserName(request));
			return CheckUrlStatus.FailToRstLengthError;
		}
		delay(request, response, contents[2]);
		if ("false".equals(contents[0])) {
			err(CheckUrlStatus.FailToNoLogin + " : " + sessionId + " | " + getUserName(request));
			return CheckUrlStatus.FailToNoLogin;
		}
		if ("false".equals(contents[1])) {
			err(CheckUrlStatus.FailToNoPermissions + " : " + sessionId + " | " + getUserName(request));
			return CheckUrlStatus.FailToNoPermissions;
		}
		return CheckUrlStatus.Success;
	}

	/**
	 * 查询本系统(当前系统,哪个系统调用的,就属于哪个系统)中的url是否有权限
	 * 
	 * @param sm
	 * @param url
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static CheckUrlStatus checkUrl(SessionModel sm, String url) throws ClientProtocolException, IOException {
		// String sessionid = getSessionId(request);
		String content = HttpUtil.executeGet(serviceUrl + String.format(doCheckUrl, sm.getSessionStr(), url));
		String[] contents = content.split(",", 3);
		if (contents.length != 3) {
			return CheckUrlStatus.FailToRstLengthError;
		}
		// delay(request, response, contents[2]);
		if ("false".equals(contents[0])) {
			return CheckUrlStatus.FailToNoLogin;
		}
		if ("false".equals(contents[1])) {
			return CheckUrlStatus.FailToNoPermissions;
		}
		return CheckUrlStatus.Success;
	}

	/**
	 * 查询本系统(当前系统,哪个系统调用的,就属于哪个系统)中拥有该url权限的所有用户
	 * 
	 * @param systemName
	 * @param url
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static List<UserModel> findAllUserBySystemNameAndUrl(String url) throws JsonParseException, JsonMappingException, ClientProtocolException, IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		JavaType javaType = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, UserModel.class);
		List<UserModel> list = objectMapper.readValue(HttpUtil.executeGet(serviceUrl + String.format(doGetUserAll, url)), javaType);
		return list;
	}

	/**
	 * 根据userids查询所有user
	 * 
	 * @param userIds
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static List<UserModel> findAllUserByUserIds(String userIds) throws JsonParseException, JsonMappingException, ClientProtocolException, IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		JavaType javaType = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, UserModel.class);
		List<UserModel> list = objectMapper.readValue(HttpUtil.executeGet(serviceUrl + String.format(doGetUserByUserIds, userIds)), javaType);
		return list;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
		systemName = filterConfig.getInitParameter("systemName");
		serviceUrl = filterConfig.getInitParameter("serviceUrl");
		// 根据配置的ServiceUrl选择一个host
		serviceNode = new ChangeNode(serviceUrl);
		initServiceUrl();
		noPermissionsPage = filterConfig.getInitParameter("noPermissionsPage");
		try {
			if (noPermissionsPage == null || filterConfig.getServletContext().getResource("/" + noPermissionsPage) == null) {
				err("noPermissionsPage没有配置或找不到该文件。");
				noPermissionsPage = "/nopermissions.html";
			}
		} catch (Exception e) {
			err("初始化noPermissionsPage出错。" + e.getMessage());
		}
		templateJs = filterConfig.getInitParameter("templateJs");
		onOff = "1".equals(filterConfig.getInitParameter("on-off"));
		try {
			cookieSeconds = Integer.parseInt(filterConfig.getInitParameter("cookieSeconds"));
		} catch (Exception e) {
			err("初始化cookie时间出错。");
		}
		// 过滤的URL
		ignoreUrlsSet = new HashSet<String>();
		ignoreUrlsPrefixSet = new HashSet<String>();
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
		doCheckUrl = "/userservice/checkurl.do?systemName=" + systemName + "&" + SESSION_KEY + "=%s&url=%s";
		doGetUserAll = "/userservice/getuserall.do?systemName=" + systemName + "&url=%s";
		doGetUserByUserIds = "/userservice/getuserbyuserids.do?userids=%s";
		log("初始化配置：on-off              : " + onOff);
		log("初始化配置：systemName          : " + systemName);
		log("初始化配置：serviceUrl          : " + serviceUrl);
		log("初始化配置：noPermissionsPage   : " + noPermissionsPage);
		log("初始化配置：cookieSeconds       : " + cookieSeconds);
		log("初始化配置：ignoreUrls          : " + ignoreUrls);
		log("初始化配置：ignoreUrlsSet       : " + ignoreUrlsSet);
		log("初始化配置：ignoreUrlsPrefixSet : " + ignoreUrlsPrefixSet);
		log("初始化配置：doCheckUrl          : " + doCheckUrl);
		if (ignoreUrlsPrefixSet.size() > 10) {
			log("【提示】：ignoreUrlsPrefixSet的值大于10个，将会严重影响系统性能。");
		} else if (ignoreUrlsPrefixSet.size() > 5) {
			log("【提示】：ignoreUrlsPrefixSet的值大于5个，可能会影响系统性能。");
		}
		// 初始化模板
		initTemplate();
		Thread checkConnection = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(60000);
						if (eCount > 10 && !HttpUtil.ping(serviceUrl)) {
							log("检测到异常次数[" + eCount + "]，无法ping通[" + serviceUrl + "]，开始重新初始化serviceUrl配置。");
							String url = serviceUrl;
							log("系统初始化配置：serviceUrl          : " + serviceUrl);
							initServiceUrl();
							log("重新初始化配置：serviceUrl          : " + serviceUrl);
							if (!url.equals(serviceUrl)) {
								initTemplate();
							} else {
								log("重新初始化配置失败，需要管理员查看网络信息。");
							}
							eCount = 0;
						}
					} catch (Exception e) {
					}
				}
			}
		});
		checkConnection.start();
		log("初始化配置：success by source   : " + this.getClass().getPackage());
	}

	private void initServiceUrl() {
		if (serviceNode.size() > 1) {
			for (int i = 0; i < serviceNode.size(); i++) {
				if (HttpUtil.ping(serviceNode.get(i))) {
					serviceUrl = serviceNode.get(i);
					break;
				} else {
					serviceUrl = serviceNode.next(i);
				}
			}
		}
	}

	private void initTemplate() {
		if (templateJs != null && !"".equals(templateJs)) {
			for (String keyValue : templateJs.split(",")) {
				String[] kv = keyValue.split(":");
				if (kv.length != 2) {
					err("过滤错误JS生成配置：" + keyValue);
					continue;
				}
				// 根据模板生成文件
				newTemplate(kv[0], kv[1]);
			}
		} else {
			log("初始化配置：没有需要生成的JS模板");
		}
	}

	private void newTemplate(String template, String js) {
		String filePath = null;
		try {
			filePath = filterConfig.getServletContext().getResource("/").getPath();
			filePath = filePath.substring(0, filePath.length() - 1);
			if (filterConfig.getServletContext().getResource(template) == null) {
				log("初始化配置：not found template  : " + filePath + template);
			} else {
				log("初始化配置：template path       : " + filePath + template);
				log("初始化配置：template child path : " + filePath + js);
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

	public static void delay(HttpServletRequest request, HttpServletResponse response, String newSession) throws UnsupportedEncodingException {
		if ("".equals(newSession)) {
			newSession = null;
		}
		String domain = getDomain(request);
		log("delay ~ [" + domain + "] ~ new session : " + newSession);
		if (domain == null) {
			// log("delay error ~ domain is null ~ new session : " +
			// newSession);
			return;
		}
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

	private static String getRequestCookie(HttpServletRequest request, String key) {
		String sessionValue = request.getParameter(key);
		if (sessionValue != null) {
			log(key + " 从 request 中取值 : " + sessionValue);
			return sessionValue;
		}
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (key.equals(cookie.getName())) {
					log(key + " 从 cookie  中取值 : " + cookie.getValue());
					return cookie.getValue();
				}
			}
		}
		log(key + " 从 cookie 和 request 中取值均为 : null");
		return null;
	}

	private static String getSessionId(HttpServletRequest request) {
		return getRequestCookie(request, SESSION_KEY);
	}

	private static String getUserName(HttpServletRequest request) {
		return getRequestCookie(request, NAME_KEY);
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

	public static String getNoPermissionsPage() {
		return noPermissionsPage;
	}

	public static Integer getCookieSeconds() {
		return cookieSeconds;
	}

	public static boolean isOnOff() {
		return onOff;
	}

	@Override
	public void destroy() {
	}

	private static void log(String msg) {
		System.out.println("[PermissionsFilter] [Info ] - " + msg);
	}

	private static void err(String msg) {
		System.err.println("[PermissionsFilter] [Error] - " + msg);
	}

}
