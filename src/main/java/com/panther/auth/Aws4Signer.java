package com.panther.auth;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.panther.model.Aws4Auth;

public class Aws4Signer {

	private static final Logger LOGGER = LoggerFactory.getLogger(Aws4Signer.class);

	private static final String HMAC_ALGORITHM = "AWS4-HMAC-SHA256";
	private static final String AWS4_REQUEST = "aws4_request";
	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	private Aws4Auth aws4Auth;
	private TreeMap<String, String> headers;
	private String signedHeadersString;
	private String xAmzHeaderDate;
	private String todaysDate;

	private Aws4Signer() {
	}

	public Map<String, String> headers(Aws4Auth aws4Auth) {
		if (null == aws4Auth) {
			return null;
		}
		this.aws4Auth = aws4Auth;
		xAmzHeaderDate = getTimeStamp();
		todaysDate = getTodaysDate();
		headers.put("x-amz-date", xAmzHeaderDate);
		String canonicalURL = prepareCanonicalRequest();
		String stringToSign = prepareStringToSign(canonicalURL);
		String signature = calculateSignature(stringToSign);
		if (signature != null) {
			Map<String, String> header = new HashMap<String, String>(0);
			header.put("x-amz-date", xAmzHeaderDate);
			header.put("Authorization", buildAuthorizationString(signature));
			return header;
		}
		return new TreeMap<String, String>();
	}

	private String prepareCanonicalRequest() {
		StringBuilder canonicalURL = new StringBuilder("");
		canonicalURL.append(aws4Auth.getHttpMethod()).append("\n");
		String canonicalURI = aws4Auth.getCanonicalUri();
		canonicalURI = canonicalURI == null || canonicalURI.trim().isEmpty() ? "/" : canonicalURI;
		canonicalURL.append(canonicalURI).append("\n");
		StringBuilder queryString = new StringBuilder("");
		TreeMap<String, String> queryParametes = aws4Auth.getQueryParametes();
		if (queryParametes != null && !queryParametes.isEmpty()) {
			for (Map.Entry<String, String> entrySet : queryParametes.entrySet()) {
				String key = entrySet.getKey();
				String value = entrySet.getValue();
				queryString.append(key).append("=").append(encodeParameter(value)).append("&");
			}
			queryString.deleteCharAt(queryString.lastIndexOf("&"));
			queryString.append("\n");
		} else {
			queryString.append("\n");
		}
		canonicalURL.append(queryString);
		StringBuilder signedHeaders = new StringBuilder("");
		if (headers != null && !headers.isEmpty()) {
			for (Map.Entry<String, String> entrySet : headers.entrySet()) {
				String key = entrySet.getKey();
				String value = entrySet.getValue();
				signedHeaders.append(key).append(";");
				canonicalURL.append(key).append(":").append(value).append("\n");
			}
			canonicalURL.append("\n");
		} else {
			canonicalURL.append("\n");
		}

		signedHeadersString = signedHeaders.substring(0, signedHeaders.length() - 1);
		canonicalURL.append(signedHeadersString).append("\n");
		String payload = aws4Auth.getPayload();
		payload = payload == null ? "" : payload;
		canonicalURL.append(generateHex(payload));
		return canonicalURL.toString();
	}

	private String prepareStringToSign(String canonicalURL) {
		String stringToSign = "";
		stringToSign = HMAC_ALGORITHM + "\n";
		stringToSign += xAmzHeaderDate + "\n";
		stringToSign += todaysDate + "/" + aws4Auth.getRegionName() + "/" + aws4Auth.getServiceName() + "/"
				+ AWS4_REQUEST + "\n";
		stringToSign += generateHex(canonicalURL);
		return stringToSign;
	}

	private String calculateSignature(String stringToSign) {
		try {
			byte[] signatureKey = getSignatureKey(aws4Auth.getSecretKey(), todaysDate, aws4Auth.getRegionName(),
					aws4Auth.getServiceName());
			byte[] signature = HmacSHA256(signatureKey, stringToSign);
			String strHexSignature = bytesToHex(signature);
			return strHexSignature;
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		return null;
	}

	private String buildAuthorizationString(String signatureString) {
		return HMAC_ALGORITHM + " " + "Credential=" + aws4Auth.getAccessKey() + "/" + getTodaysDate() + "/"
				+ aws4Auth.getRegionName() + "/" + aws4Auth.getServiceName() + "/" + AWS4_REQUEST + ","
				+ "SignedHeaders=" + signedHeadersString + "," + "Signature=" + signatureString;
	}

	private String generateHex(String data) {
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
			messageDigest.update(data.getBytes("UTF-8"));
			byte[] digest = messageDigest.digest();
			return String.format("%064x", new java.math.BigInteger(1, digest));
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			LOGGER.error(e.getMessage());
		}
		return null;
	}

	private byte[] HmacSHA256(byte[] key, String data) throws Exception {
		String algorithm = "HmacSHA256";
		Mac mac = Mac.getInstance(algorithm);
		mac.init(new SecretKeySpec(key, algorithm));
		return mac.doFinal(data.getBytes("UTF8"));
	}

	private byte[] getSignatureKey(String secretKey, String date, String regionName, String serviceName)
			throws Exception {
		byte[] kSecret = ("AWS4" + secretKey).getBytes("UTF8");
		byte[] kDate = HmacSHA256(kSecret, date);
		byte[] kRegion = HmacSHA256(kDate, regionName);
		byte[] kService = HmacSHA256(kRegion, serviceName);
		return HmacSHA256(kService, AWS4_REQUEST);
	}

	private String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars).toLowerCase();
	}

	private String getTimeStamp() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(new Date());
	}

	private String getTodaysDate() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(new Date());
	}

	@SuppressWarnings("deprecation")
	private String encodeParameter(String param) {
		try {
			return URLEncoder.encode(param, "UTF-8");
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		return URLEncoder.encode(param);
	}
}
