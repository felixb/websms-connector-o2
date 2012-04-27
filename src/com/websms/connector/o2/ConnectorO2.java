/*
 * Copyright (C) 2010 Felix Bechstein, 2011 Lorenz Bauer
 * 
 * This file is part of WebSMS.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.websms.connector.o2;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import de.ub0r.android.websms.connector.common.CharacterTable;
import de.ub0r.android.websms.connector.common.CharacterTableSMSLengthCalculator;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;
import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.Utils.HttpOptions;
import de.ub0r.android.websms.connector.common.WebSMSException;

/**
 * AsyncTask to manage IO to O2 API.
 * 
 * @author flx
 */
public class ConnectorO2 extends Connector {
	/** Tag for output. */
	private static final String TAG = "o2";

	/** Google's ad unit id. */
	private static final String AD_UNITID = "a14dd555326b466";

	/** Used encoding. */
	private static final String ENCODING = "ISO-8859-15";

	/** Custom Dateformater. */
	private static final String DATEFORMAT = "yyyy,MM,dd,kk,mm,00";

	/** URL before login. */
	private static final String URL_PRELOGIN = "https://login.o2online.de/auth/login?scheme=https&port=443&server=email.o2online.de&url=%2Fssomanager.osp%3FAPIID%3DAUTH-WEBSSO";
	/** URL for login. */
	private static final String URL_LOGIN = "https://login.o2online.de/auth/?wicket:interface=:0:loginForm::IFormSubmitListener::";
	/** URL of captcha. */
	private static final String URL_CAPTCHA = "https://login.o2online.de/auth/?wicket:interface=:0:loginForm:captchaPanel:captchaImage::IResourceListener::";
	/** URL for sms center. */
	private static final String URL_SMSCENTER = "https://email.o2online.de/ssomanager.osp?APIID=AUTH-WEBSSO&TargetApp=/smscenter_new.osp%3f&o2_type=url&o2_label=web2sms-o2online";
	/** URL before sending. */
	private static final String URL_PRESEND = "https://email.o2online.de"
			+ "/smscenter_new.osp?Autocompletion=1&MsgContentID=-1";
	/** URL for sending. */
	private static final String URL_SEND = "https://email.o2online.de"
			+ "/smscenter_send.osp";
	/** URL for sending later. */
	private static final String URL_SCHEDULE = "https://email.o2online.de"
			+ "/smscenter_schedule.osp";

	/** Check for free sms. */
	private static final String CHECK_FREESMS = "Frei-SMS: ";
	/** Check for web2sms. */
	private static final String CHECK_WEB2SMS = "Web2SMS";
	/** Check if message was sent. */
	private static final String CHECK_SENT = // .
	"Ihre SMS wurde erfolgreich versendet.";

	/** Check if message was scheduled. */
	private static final String CHECK_SCHED = "Ihre Web2SMS ist geplant";

	/** HTTP Useragent. */
	private static final String TARGET_AGENT = "Mozilla/5.0 (Windows; U;"
			+ " Windows NT 5.1; de; rv:1.9.0.9) Gecko/2009040821"
			+ " Firefox/3.0.9 (.NET CLR 3.5.30729)";

	/** Solved Captcha. */
	private static String captchaSolve = null;
	/** Object to sync with. */
	private static final Object CAPTCHA_SYNC = new Object();
	/** Timeout for entering the captcha. */
	private static final long CAPTCHA_TIMEOUT = 60000;

	/**
	 * The current fingerprints of the SSL-certificate used by the https-sites.
	 */
	private static final String[] O2_SSL_FINGERPRINTS = {
			// login.o2online.de (older but still used)
			"2c:b4:86:a8:da:87:77:3f:e4:b2:9d:26:6e:11:9e:00:3d:db:85:55",
			// login.o2online.de (2011-11-01)
			"09:37:b0:df:67:b6:01:dd:2a:b6:0b:b1:f9:24:0f:3c:3f:77:77:2f",
			// email.o2online.de (2011-04-14)
			"b0:36:f6:fd:0b:6f:28:75:ca:3b:5d:4a:91:07:ce:db:d0:0d:71:b0",
			// email.o2online.de (2012-04-10)
			"a8:d1:74:21:71:61:d5:e7:d0:6f:ee:4b:ea:f0:ee:4e:0a:09:04:83" };

	/** (Setting) Ignore invalid SSL certificates */
	protected boolean mIgnoreCerts = false;

	/** Mapping. */
	private static final Map<String, String> MAP = new HashMap<String, String>(
			512);

	static {
		MAP.put("\r", "");
		// MAP.put("Â´", "'");

		// turkish
		MAP.put("\u00F7", "%"); // Ã·
		MAP.put("\u0130", "I"); // Ä°
		MAP.put("\u0131", "i"); // Ä±
		MAP.put("\u015E", "S"); // Åž
		MAP.put("\u015F", "s"); // ÅŸ
		MAP.put("\u00C7", "C"); // Ã‡
		MAP.put("\u00E7", "c"); // Ã§
		MAP.put("\u011E", "G"); // Äž
		MAP.put("\u011F", "g"); // ÄŸ

		// polish
		MAP.put("\u0104", "A"); // Ä„
		MAP.put("\u0105", "a"); // Ä…
		MAP.put("\u0106", "C"); // Ä†
		MAP.put("\u0107", "c"); // Ä‡
		MAP.put("\u0118", "E"); // Ä˜
		MAP.put("\u0119", "e"); // Ä™
		MAP.put("\u0141", "L"); // Å�
		MAP.put("\u0142", "l"); // Å‚
		MAP.put("\u0143", "N"); // Åƒ
		MAP.put("\u0144", "n"); // Å„
		MAP.put("\u00D3", "O"); // Ã“
		MAP.put("\u015A", "S"); // Åš
		MAP.put("\u015B", "s"); // Å›
		MAP.put("\u0179", "Z"); // Å¹
		MAP.put("\u017A", "z"); // Åº
		MAP.put("\u017B", "Z"); // Å»
		MAP.put("\u017C", "z"); // Å¼
		MAP.put("\u00F3", "o"); // Ã³
	}

	/** GMX's {@link CharacterTable}. */
	private static final CharacterTable REPLACE = new CharacterTable(MAP);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_o2_name);
		ConnectorSpec c = new ConnectorSpec(name);
		c.setAuthor(context.getString(R.string.connector_o2_author));
		c.setAdUnitId(AD_UNITID);
		c.setBalance(null);
		c.setSMSLengthCalculator(new CharacterTableSMSLengthCalculator(REPLACE));
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND
				| ConnectorSpec.CAPABILITIES_PREFS);
		c.addSubConnector(TAG, c.getName(),
				SubConnectorSpec.FEATURE_CUSTOMSENDER
						| SubConnectorSpec.FEATURE_SENDLATER
						| SubConnectorSpec.FEATURE_SENDLATER_QUARTERS
						| SubConnectorSpec.FEATURE_FLASHSMS);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec updateSpec(final Context context,
			final ConnectorSpec connectorSpec) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (p.getBoolean(Preferences.PREFS_ENABLED, false)) {
			if (p.getString(Preferences.PREFS_PASSWORD, "").length() > 0) {
				connectorSpec.setReady();
			} else {
				connectorSpec.setStatus(ConnectorSpec.STATUS_ENABLED);
			}
		} else {
			connectorSpec.setStatus(ConnectorSpec.STATUS_INACTIVE);
		}

		boolean oldIgnoreCerts = this.mIgnoreCerts;
		this.mIgnoreCerts = p.getBoolean(Preferences.PREFS_IGNORE_CERTS, false);
		Log.d(TAG, "Ignoring SSL certs = " + this.mIgnoreCerts);

		if (oldIgnoreCerts != this.mIgnoreCerts) {
			// the setting changed, we have to reset the client or it will
			// not use the new setting
			Utils.resetHttpClient();
		}

		return connectorSpec;
	}

	/**
	 * Load captcha and wait for user input to solve it.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return true if captcha was solved
	 * @throws IOException
	 *             IOException
	 */
	private String solveCaptcha(final Context context) throws IOException {
		HttpResponse response = this.getHttpClient(context, URL_CAPTCHA, null,
				URL_LOGIN);
		int resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(context, R.string.error_http, "" + resp);
		}
		BitmapDrawable captcha = new BitmapDrawable(response.getEntity()
				.getContent());
		final Intent intent = new Intent(Connector.ACTION_CAPTCHA_REQUEST);
		intent.putExtra(Connector.EXTRA_CAPTCHA_DRAWABLE, captcha.getBitmap());
		captcha = null;
		this.getSpec(context).setToIntent(intent);
		context.sendBroadcast(intent);
		try {
			synchronized (CAPTCHA_SYNC) {
				CAPTCHA_SYNC.wait(CAPTCHA_TIMEOUT);
			}
		} catch (InterruptedException e) {
			Log.e(TAG, null, e);
			return null;
		}
		if (captchaSolve == null) {
			return null;
		}
		// got user response, try to solve captcha
		Log.d(TAG, "got solved captcha: " + captchaSolve);
		return captchaSolve;
	}

	/**
	 * Login to O2.
	 * 
	 * @param context
	 *            Context
	 * @param command
	 *            ConnectorCommand
	 * @return true if logged in
	 * @throws IOException
	 *             IOException
	 */
	private boolean login(final Context context,
			final ConnectorCommand command, final String captcha)
			throws IOException {
		// post data
		final ArrayList<BasicNameValuePair> postData = // .
		new ArrayList<BasicNameValuePair>(3);

		if (captcha != null) {
			Log.d(TAG, "Using captcha: " + captcha);
			postData.add(new BasicNameValuePair(
					"captchaPanel:captchaInput:response", captcha));
		}

		postData.add(new BasicNameValuePair("loginName:loginName", Utils
				.international2national(command.getDefPrefix(),
						Utils.getSenderNumber(context, command.getDefSender()))));
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		postData.add(new BasicNameValuePair("password:password", p.getString(
				Preferences.PREFS_PASSWORD, "")));

		int ccount = Utils.getCookieCount();
		HttpResponse response = this.getHttpClient(context, URL_LOGIN,
				postData, URL_PRELOGIN);
		int resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(context, R.string.error_http, "" + resp);
		}
		if (ccount == Utils.getCookieCount()) {
			Log.i(TAG, "cookie count: " + ccount);
			Log.d(TAG, Utils.getCookiesAsString());

			String htmlText = null;
			htmlText = Utils.stream2str(response.getEntity().getContent());

			response = null;
			if (htmlText != null && htmlText.indexOf("captchaPanel") > 0) {
				htmlText = null;
				String new_captcha;
				if ((new_captcha = this.solveCaptcha(context)) == null) {
					throw new WebSMSException(context,
							R.string.error_wrongcaptcha);
				}

				this.login(context, command, new_captcha);
			} else {
				Log.d(TAG, htmlText);
				throw new WebSMSException(context, R.string.error_pw);
			}
		}
		return true;
	}

	/**
	 * Format values from calendar to minimum 2 digits.
	 * 
	 * @param cal
	 *            calendar
	 * @param f
	 *            field
	 * @return value as string
	 */
	private static String getTwoDigitsFromCal(final Calendar cal, final int f) {
		int r = cal.get(f);
		if (f == Calendar.MONTH) {
			++r;
		}
		if (r < 10) {
			return "0" + r;
		} else {
			return "" + r;
		}
	}

	/**
	 * Send SMS.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param command
	 *            {@link ConnectorCommand}
	 * @param htmlText
	 *            html source of previous site
	 * @throws IOException
	 *             IOException
	 */
	private void sendToO2(final Context context,
			final ConnectorCommand command, final String htmlText)
			throws IOException {
		ArrayList<BasicNameValuePair> postData = // .
		new ArrayList<BasicNameValuePair>();
		postData.add(new BasicNameValuePair("SMSTo", Utils
				.national2international(command.getDefPrefix(),
						Utils.getRecipientsNumber(command.getRecipients()[0]))));
		postData.add(new BasicNameValuePair("SMSText", REPLACE
				.encodeString(command.getText())));
		String customSender = command.getCustomSender();
		if (customSender == null) {
			final String sn = Utils.getSenderNumber(context,
					command.getDefSender());
			final String s = Utils.getSender(context, command.getDefSender());
			if (s != null && !s.equals(sn)) {
				customSender = s;
			}
		}
		if (customSender != null) {
			postData.add(new BasicNameValuePair("SMSFrom", customSender));
			if (customSender.length() == 0) {
				postData.add(new BasicNameValuePair("FlagAnonymous", "1"));
			} else {
				postData.add(new BasicNameValuePair("FlagAnonymous", "0"));
				postData.add(new BasicNameValuePair("FlagDefSender", "1"));
			}
			postData.add(new BasicNameValuePair("FlagDefSender", "0"));
		} else {
			postData.add(new BasicNameValuePair("SMSFrom", ""));
			postData.add(new BasicNameValuePair("FlagDefSender", "1"));
		}
		postData.add(new BasicNameValuePair("Frequency", "5"));
		if (command.getFlashSMS()) {
			postData.add(new BasicNameValuePair("FlagFlash", "1"));
		} else {
			postData.add(new BasicNameValuePair("FlagFlash", "0"));
		}
		String url = URL_SEND;
		final long sendLater = command.getSendLater();
		if (sendLater > 0) {
			url = URL_SCHEDULE;
			final Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(sendLater);
			postData.add(new BasicNameValuePair("StartDateDay",
					getTwoDigitsFromCal(cal, Calendar.DAY_OF_MONTH)));
			postData.add(new BasicNameValuePair("StartDateMonth",
					getTwoDigitsFromCal(cal, Calendar.MONTH)));
			postData.add(new BasicNameValuePair("StartDateYear",
					getTwoDigitsFromCal(cal, Calendar.YEAR)));
			postData.add(new BasicNameValuePair("StartDateHour",
					getTwoDigitsFromCal(cal, Calendar.HOUR_OF_DAY)));
			postData.add(new BasicNameValuePair("StartDateMin",
					getTwoDigitsFromCal(cal, Calendar.MINUTE)));
			postData.add(new BasicNameValuePair("EndDateDay",
					getTwoDigitsFromCal(cal, Calendar.DAY_OF_MONTH)));
			postData.add(new BasicNameValuePair("EndDateMonth",
					getTwoDigitsFromCal(cal, Calendar.MONTH)));
			postData.add(new BasicNameValuePair("EndDateYear",
					getTwoDigitsFromCal(cal, Calendar.YEAR)));
			postData.add(new BasicNameValuePair("EndDateHour",
					getTwoDigitsFromCal(cal, Calendar.HOUR_OF_DAY)));
			postData.add(new BasicNameValuePair("EndDateMin",
					getTwoDigitsFromCal(cal, Calendar.MINUTE)));
			final String s = DateFormat.format(DATEFORMAT, cal).toString();
			postData.add(new BasicNameValuePair("RepeatStartDate", s));
			postData.add(new BasicNameValuePair("RepeatEndDate", s));
			postData.add(new BasicNameValuePair("RepeatType", "5"));
			postData.add(new BasicNameValuePair("RepeatEndType", "0"));
		}
		String[] st = htmlText.split("<input type=\"Hidden\" ");
		for (String s : st) {
			if (s.startsWith("name=")) {
				String[] subst = s.split("\"", 5);
				if (subst.length >= 4) {
					if (sendLater > 0 && subst[1].startsWith("Repeat")) {
						continue;
					}
					postData.add(new BasicNameValuePair(subst[1], subst[3]));
				}
			}
		}
		st = null;

		HttpResponse response = this.getHttpClient(context, url, postData,
				URL_PRESEND);
		postData = null;
		int resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			throw new WebSMSException(context, R.string.error_http, "" + resp);
		}
		String check = CHECK_SENT;
		if (sendLater > 0) {
			check = CHECK_SCHED;
		}
		String htmlText1 = null;
		htmlText1 = Utils.stream2str(response.getEntity().getContent(), 0,
				Utils.ONLY_MATCHING_LINE, check);

		if (htmlText1 == null) {
			throw new WebSMSException("error parsing website");
		} else if (htmlText1.indexOf(check) < 0) {
			// check output html for success message
			Log.w(TAG, htmlText1);
			throw new WebSMSException("error parsing website");
		}
	}

	/**
	 * Send data.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param command
	 *            {@link ConnectorCommand}
	 * @param reuseSession
	 *            try to reuse existing session
	 * @throws IOException
	 *             IOException
	 */
	private void sendData(final Context context,
			final ConnectorCommand command, final boolean reuseSession)
			throws IOException {
		Log.d(TAG, "sendData(" + reuseSession + ")");

		// get Connection
		HttpResponse response;
		int resp;
		if (!reuseSession) {
			// clear session data
			Utils.clearCookies();
		}
		if (Utils.getCookieCount() == 0) {
			Log.d(TAG, "init session");
			// pre-login

			response = this.getHttpClient(context, URL_PRELOGIN, null, null);

			resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				throw new WebSMSException(context, R.string.error_http, ""
						+ resp);
			}

			// login
			if (!this.login(context, command, null)) {
				throw new WebSMSException(context, R.string.error);
			}

			// sms-center
			response = this.getHttpClient(context, URL_SMSCENTER, null,
					URL_LOGIN);
			resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				if (reuseSession) {
					// try again with clear session
					this.sendData(context, command, false);
					return;
				}
				throw new WebSMSException(context, R.string.error_http, ""
						+ resp);
			}
		}

		// pre-send
		response = this
				.getHttpClient(context, URL_PRESEND, null, URL_SMSCENTER);
		resp = response.getStatusLine().getStatusCode();
		if (resp != HttpURLConnection.HTTP_OK) {
			if (reuseSession) {
				// try again with clear session
				this.sendData(context, command, false);
				return;
			}
			throw new WebSMSException(context, R.string.error_http, "" + resp);
		}
		String htmlText = null;

		htmlText = Utils.stream2str(response.getEntity().getContent(), 0, -1,
				CHECK_FREESMS);

		if (htmlText == null) {
			if (reuseSession) {
				this.sendData(context, command, false);
				return;
			} else {
				throw new WebSMSException(context, // .
						R.string.missing_freesms);
			}
		}
		int i = htmlText.indexOf(CHECK_FREESMS);
		if (i > 0) {
			int j = htmlText.indexOf(CHECK_WEB2SMS, i);
			if (j > 0) {
				ConnectorSpec c = this.getSpec(context);
				c.setBalance(htmlText.substring(i + 9, j).trim().split(" ", 2)[0]);
				Log.d(TAG, "balance: " + c.getBalance());
			} else if (reuseSession) {
				// try again with clear session
				this.sendData(context, command, false);
				return;
			} else {
				Log.d(TAG, htmlText);
				throw new WebSMSException(context, // .
						R.string.missing_freesms);
			}
		} else {
			Log.d(TAG, htmlText);
			throw new WebSMSException(context, R.string.missing_freesms);
		}

		// send
		final String text = command.getText();
		if (text != null && text.length() > 0) {
			this.sendToO2(context, command, htmlText);
		}
		htmlText = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doUpdate(final Context context, final Intent intent)
			throws IOException {
		this.sendData(context, new ConnectorCommand(intent), true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doSend(final Context context, final Intent intent)
			throws IOException {
		this.sendData(context, new ConnectorCommand(intent), true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void gotSolvedCaptcha(final Context context,
			final String solvedCaptcha) {
		captchaSolve = solvedCaptcha;
		synchronized (CAPTCHA_SYNC) {
			CAPTCHA_SYNC.notify();
		}
	}

	protected HttpResponse getHttpClient(final Context context,
			final String url, final ArrayList<BasicNameValuePair> postData,
			final String referer) {
		try {
			HttpOptions options = new HttpOptions(ENCODING);
			options.url = url;
			options.userAgent = TARGET_AGENT;
			options.referer = referer;
			options.trustAll = this.mIgnoreCerts;
			options.knownFingerprints = O2_SSL_FINGERPRINTS.clone();
			options.addFormParameter(postData);

			return Utils.getHttpClient(options);
		} catch (javax.net.ssl.SSLException e) {
			throw new WebSMSException(context, R.string.error_invalid_cert);
		} catch (IOException e) {
			throw new WebSMSException(e);
		}
	}
}
