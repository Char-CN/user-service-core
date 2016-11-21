package org.blazer.userservice.core.model;

/**
 * Session模型，由SessionUtil工具类提供该类的操作
 * 
 * @author hyy
 *
 */
public class SessionModel {

	private LoginType loginType;

	private Integer userId;

	private String userName;

	private String userNameCn;

	private String phoneNumber;

	private String email;

	private Long expireTime;

	private boolean isValid;

	public SessionModel() {
	}

	public LoginType getLoginType() {
		return loginType;
	}

	public void setLoginType(LoginType loginType) {
		this.loginType = loginType;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserNameCn() {
		return userNameCn;
	}

	public void setUserNameCn(String userNameCn) {
		this.userNameCn = userNameCn;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Long getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(Long expireTime) {
		this.expireTime = expireTime;
	}

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	@Override
	public String toString() {
		return "SessionBean [loginType=" + loginType + ", userId=" + userId + ", userName=" + userName + ", userNameCn=" + userNameCn + ", phoneNumber="
				+ phoneNumber + ", email=" + email + ", expireTime=" + expireTime + ", isValid=" + isValid + "]";
	}

}
