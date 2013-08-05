package se761.bestgroup.vsmreceiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie2;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class LogActivity extends Activity {

	private ArrayAdapter<String> listAdapter;

	private BasicClientCookie2 _deptCookie;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log);

		NfcAdapter.getDefaultAdapter(this);
		this.listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		final ListView lv = (ListView) findViewById(R.id.logListView);
		lv.setAdapter(this.listAdapter);

		final String savedDepartment = getSharedPreferences("department_cookie", MODE_PRIVATE).getString("department", null);
		this._deptCookie = new BasicClientCookie2("department", savedDepartment != null ? savedDepartment : "Cardiology");
		this._deptCookie.setDomain("vsm.herokuapp.com");
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.log, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
		if (item.getItemId() == R.id.action_departments) {
			startActivityForResult(new Intent(this, DepartmentsActivity.class), 1);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_departments:
				startActivityForResult(new Intent(this, DepartmentsActivity.class), 1);
		}
		return true;
	}

	@Override
	public void onResume() {
		super.onResume();
		// Check to see that the Activity started due to an Android Beam
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			System.out.println("YOUR MUM");
			processIntent(getIntent());
		}
	}

	/**
	 * Get the department cookie from the shared preferences Set it to the cookie field.
	 */
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		final String dept = data.getStringExtra("department");
		final Editor edit = getSharedPreferences("department_cookie", MODE_PRIVATE).edit();
		edit.putString("department", dept);
		edit.commit();
		this._deptCookie = new BasicClientCookie2("department", dept.trim());
		this._deptCookie.setDomain("vsm.herokuapp.com");
		setTitle(dept);
	}

	@Override
	public void onNewIntent(final Intent intent) {
		// onResume gets called after this to handle the intent
		setIntent(intent);
	}

	/**
	 * Get any messages from the intent and upload them to the server using the SubmitVitalStats task
	 * 
	 * @param intent
	 */
	void processIntent(final Intent intent) {

		final Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		// only one message sent during the beam
		final NdefMessage msg = (NdefMessage) rawMsgs[0];
		final String patient = new String(msg.getRecords()[0].getPayload());

		this.listAdapter.add(patient);
		// send the message somewhere
		final SubmitVitalStats vitalStatsUpload = new SubmitVitalStats();
		vitalStatsUpload.execute(patient);
	}

	/**
	 * Async task for network access
	 * 
	 * @author Jourdan Harvey
	 * 
	 */
	private class SubmitVitalStats extends AsyncTask<String, Void, Boolean> {

		private DefaultHttpClient httpclient;

		@Override
		protected Boolean doInBackground(final String... params) {

			// instantiates httpclient to make request
			this.httpclient = new DefaultHttpClient();
			// add our department cookie to the client so that we know where to
			// put our patient
			this.httpclient.getCookieStore().addCookie(LogActivity.this._deptCookie);

			for (final Cookie c : this.httpclient.getCookieStore().getCookies()) {
				System.out.println(c);
			}

			// passes the results to a string builder/entity
			StringEntity patientSE = null;
			String patientString = null;
			try {
				final JSONObject j = new JSONObject(params[0]);
				j.getJSONObject("vitalinfo").put("location", "Starship Childrens' Hospital");
				patientString = j.toString();
				Log.d("json", patientString);
			} catch (final IndexOutOfBoundsException e) {
				// incorrect msg
				return false;
			} catch (final JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				patientSE = new StringEntity(patientString);
			} catch (final UnsupportedEncodingException e) {
				e.printStackTrace();
				return false;
			}

			boolean result = true;
			Log.v("Receiver", "Patients Response");
			// call httpPost method to post the patientData to the server
			result = httpPost(patientSE, new HttpPost(getResources().getString(R.string.patients_endpoint)));
			Log.v("Receiver", "Vitals Response");
			return result;
		}

		/**
		 * Take an HttpPost and execute it
		 * 
		 * @param se
		 * @param httppost
		 * @return
		 */
		private boolean httpPost(final StringEntity se, final HttpPost httppost) {
			// sets the post request as the resulting string
			httppost.setEntity(se);
			// sets a request header so the page receiving the request
			// will know what to do with it
			httppost.setHeader("Accept", "application/json");
			httppost.setHeader("Content-type", "application/json");

			try {
				// execute, get response, read
				final HttpResponse response = this.httpclient.execute(httppost);
				final InputStream content = response.getEntity().getContent();
				final BufferedReader br = new BufferedReader(new InputStreamReader(content));
				String line;
				final StringBuilder sb = new StringBuilder();
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
				Log.d("response", sb.toString());

			} catch (final ClientProtocolException e) {

				e.printStackTrace();
				return false;
			} catch (final IOException e) {
				e.printStackTrace();
				return false;
			}
			// winning
			return true;
		}
	}

}
