package com.pixplaze.util;

import com.pixplaze.exceptions.UnableToDefineLocalAddress;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Optional;
import java.util.regex.Pattern;

public class Inet {
	private static final Pattern IPV4_PATTERN = Pattern.compile(
			"^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

	// TODO: Протестировать работу метода на RedHat Linux
	public static String getLocalAddress() throws UnableToDefineLocalAddress {
		try {
			var interfaces = NetworkInterface.getNetworkInterfaces();

			while (interfaces.hasMoreElements()) {
				var addresses = interfaces.nextElement().getInetAddresses();
				while (addresses.hasMoreElements()) {
					var address = addresses.nextElement();
					if (address.isSiteLocalAddress()) {
						return address.getHostAddress();
					}
				}
			}
		} catch (Throwable e) {
			throw new UnableToDefineLocalAddress("Unable to define local address!", e);
		}
		throw new UnableToDefineLocalAddress("No site local address found!");
	}


	public static String verboseInterfaces() throws SocketException {
		var interfaces = NetworkInterface.getNetworkInterfaces();
		var stringBuilder = new StringBuilder();
		while (interfaces.hasMoreElements()) {
			var addresses = interfaces.nextElement().getInetAddresses();
			while (addresses.hasMoreElements()) {
				var address = addresses.nextElement();
				stringBuilder
						.append(address.getHostAddress())
						.append("SiteLocalAddress: ")
						.append(address.isSiteLocalAddress())
						.append("\n");
			}
		}
		return stringBuilder.toString();
	}

	public static boolean isIpV4Valid(final String ip) {
		return IPV4_PATTERN.matcher(ip).matches();
	}
}
