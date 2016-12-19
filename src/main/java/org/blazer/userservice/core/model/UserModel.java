package org.blazer.userservice.core.model;

/**
 * 非常干净的类，不允许有其他类的引用。
 * 
 * @author hyy
 *
 */
public class UserModel {

	private Integer id;
	private String userName;
	private String password;
	private String userNameCn;
	private String phoneNumber;
	private String email;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
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

	@Override
	public String toString() {
		return "UserModel [id=" + id + ", userName=" + userName + ", password=" + password + ", userNameCn=" + userNameCn + ", phoneNumber=" + phoneNumber
				+ ", email=" + email + "]";
	}

}
