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
	
	public static void main(String[] args) {
		System.out.println(encode(1480061451914L,3,"heyunyang","何云洋","heyunyang@evergrande.com","",1));
		System.out.println(decode("2e722b4e56a82eaf5d51218d182d49a8130eaa98213901a60bd9566da91e6513566e15a6a32710c0af87dc83a25b8c5eef105f41726a06b72c55322dd192f4bf"));
	}

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
//		System.out.println(String.format(FORMAT, expire, id, userName, userNameCn, email, phoneNumber, loginType));
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
		String[] content = StringUtils.splitByWholeSeparatorPreserveAllTokens(sessionStr, SEPARATOR);
//		System.out.println(sessionStr);
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
