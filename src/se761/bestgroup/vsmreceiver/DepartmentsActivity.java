package se761.bestgroup.vsmreceiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class DepartmentsActivity extends ListActivity {

	private ArrayAdapter<String> adapter;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// get a list view, give it an adapter and an onclick listener
		final ListView listView = getListView();
		this.adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		listView.setAdapter(this.adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view,
					final int position, final long id) {
				// get the name of the department from the item
				final String department = (String) listView
						.getItemAtPosition(position);

				// give the department to the intent
				final Intent data = new Intent();
				data.putExtra("department", department);
				// setResult for next activity and finish up
				setResult(RESULT_OK, data);
				finish();
			}
		});
		Log.v("Departments", "Starting task");
		new HttpTask().execute();
	}

	/**
	 * Task for getting departments from server
	 * 
	 * @author Jourdan Harvey, Mike Little
	 * 
	 */
	private class HttpTask extends AsyncTask<Void, Void, List<String>> {

		@Override
		protected List<String> doInBackground(final Void... params) {
			final List<String> departments = new ArrayList<String>();
			final HttpGet get = new HttpGet(getStr(R.string.departments_endpoint));
			final HttpClient httpclient = new DefaultHttpClient();
			try {
				// get response and build result
				final HttpResponse response = httpclient.execute(get);
				final InputStream ins = response.getEntity().getContent();
				final BufferedReader buff = new BufferedReader(new InputStreamReader(ins));
				final StringBuilder sb = new StringBuilder();
				String line;
				while ((line = buff.readLine()) != null) {
					sb.append(line);
				}
				// Process the json array
				final JSONArray arr = new JSONArray(sb.toString());
				for (int i = 0; i < arr.length(); i++) {
					Log.v("Departments",
							arr.getJSONObject(i).getString(
									getStr(R.string.department_name)));
					departments.add(arr.getJSONObject(i).getString(
							getStr(R.string.department_name)));
				}
			} catch (final ClientProtocolException e) {
				e.printStackTrace();
			} catch (final IOException e) {
				e.printStackTrace();
			} catch (final JSONException e) {
				e.printStackTrace();
			}
			// return list of departments for onPostExecute
			return departments;
		}

		/**
		 * Add each department in the list to the adapter for the List View
		 */
		@Override
		protected void onPostExecute(final List<String> departments) {
			for (final String s : departments) {
				DepartmentsActivity.this.adapter.add(s);
			}
		}
	}

	/**
	 * Helper method for get strings from the values file
	 * 
	 * @param id of resource
	 * @return value of resource requested
	 */
	private String getStr(final int id) {
		return getResources().getString(id);
	}
}
