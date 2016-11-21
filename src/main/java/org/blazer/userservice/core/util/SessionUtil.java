package org.blazer.userservice.core.util;

import org.apache.commons.lang3.StringUtils;
import org.blazer.userservice.core.model.LoginType;
import org.blazer.userservice.core.model.SessionModel;

/**
 * SessionModel加密与解密工具类
 * 
 * @author hyy
 *
 */
public class SessionUtil {

	/**
	 * 分隔符
	 */
	public static final String SEPARATOR = ",";

	/**
	 * 每位的意义：ExpireTime,UserId,UserName,UserNameCn,Email,PhoneNumber,LoginType
	 */
	public static final String FORMAT = "%s" + SEPARATOR + "%s" + SEPARATOR + "%s" + SEPARATOR + "%s" + SEPARATOR + "%s" + SEPARATOR + "%s" + SEPARATOR + "%s";

	/**
	 * 加密session
	 * 
	 * @param expire
	 * @param id
	 * @param userName
	 * @param userNameCn
	 * @param email
	 * @param phoneNumber
	 * @param loginType
	 * @return
	 */
	public static String encode(Long expire, Integer id, String userName, String userNameCn, String email, String phoneNumber, Integer loginType) {
		String sessionId = DesUtil.encrypt(String.format(FORMAT, expire, id, userName, userNameCn, email, phoneNumber, loginType));
		return sessionId;
	}

	/**
	 * 解密session
	 * 
	 * @param sessionStr
	 * @return
	 */
	public static SessionModel decode(String sessionStr) {
		sessionStr = DesUtil.decrypt(sessionStr);
		SessionModel bean = new SessionModel();
		if (StringUtils.isBlank(sessionStr)) {
			bean.setLoginType(null);
			bean.setUserId(null);
			bean.setUserName(null);
			bean.setUserNameCn(null);
			bean.setEmail(null);
			bean.setPhoneNumber(null);
			bean.setExpireTime(null);
			bean.setValid(false);
			return bean;
		}
		String[] content = StringUtils.splitByWholeSeparator(sessionStr, SEPARATOR);
		if (content.length != 7) {
			bean.setLoginType(null);
			bean.setUserId(null);
			bean.setUserName(null);
			bean.setUserNameCn(null);
			bean.setEmail(null);
			bean.setPhoneNumber(null);
			bean.setExpireTime(null);
			bean.setValid(false);
			return bean;
		}
		// ExpireTime,UserId,UserName,UserNameCn,Email,PhoneNumber,LoginType
		bean.setExpireTime(LongUtil.getLong(content[0]));
		bean.setUserId(IntegerUtil.getInt0(content[1]));
		bean.setUserName(StringUtil.getStrEmpty(content[2]));
		bean.setUserNameCn(StringUtil.getStr(content[3]));
		bean.setEmail(StringUtil.getStrEmpty(content[4]));
		bean.setPhoneNumber(StringUtil.getStrEmpty(content[5]));
		bean.setLoginType(LoginType.valueOf(IntegerUtil.getInt0(content[6])));
		bean.setValid(true);
		return bean;
	}

}
