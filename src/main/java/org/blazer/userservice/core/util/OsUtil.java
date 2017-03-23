package org.blazer.userservice.core.util;

public class OsUtil {

	private static final String osName = System.getProperty("os.name").toLowerCase();

	private static boolean isWindows = false;

	private static boolean isLinux = false;

	private static boolean isMac = false;

	public static boolean isWindows() {
		return isWindows;
	}

	public static boolean isLinux() {
		return isLinux;
	}

	public static boolean isMac() {
		return isMac;
	}

	static {
		if (osName.indexOf("windows") != -1) {
			isWindows = true;
		}
		if (osName.indexOf("linux") != -1) {
			isLinux = true;
		}
		if (osName.indexOf("mac") != -1) {
			isMac = true;
		}
	}

	public static void main(String[] args) {
		System.out.println(isWindows());
		System.out.println(isLinux());
		System.out.println(isMac());
	}

}
