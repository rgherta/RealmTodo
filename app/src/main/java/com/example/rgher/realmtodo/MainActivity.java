package com.example.rgher.realmtodo;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.rgher.realmtodo.data.DatabaseContract;
import com.example.rgher.realmtodo.data.QueryCursorLoader;
import com.example.rgher.realmtodo.data.RealmTask;
import com.example.rgher.realmtodo.data.TaskAdapter;
import com.example.rgher.realmtodo.data.TaskUpdateService;

public class MainActivity extends AppCompatActivity implements
        TaskAdapter.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int ID_LOADER = 30;
    private TaskAdapter mAdapter;
    private LoaderManager loaderManager;
    private LinearLayoutManager layoutManager;
    private Loader<Cursor> asyncTaskLoader;
    private LoaderManager.LoaderCallbacks<Cursor> callback;
    private int currentListPosition=0;

    private String CURRENT_POS_EXTRA;
    private String URI_EXTRA;
    private String SORT_EXTRA;
    private static boolean PREFERENCES_HAVE_BEEN_UPDATED = false;
    private String sortOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        CURRENT_POS_EXTRA = getString(R.string.extra_current_position);
        URI_EXTRA = getString(R.string.extra_uri);
        SORT_EXTRA = getString(R.string.sort_extra);


        //Get sortOrder from preferences: Default: Priority → Date → Completed, Due Date: Date → Priority → Completed
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        String myKey = getString(R.string.pref_sortBy_key);
        String defaultValue = getString(R.string.pref_sortBy_default);
        sortOrder = sharedPreferences.getString(myKey, defaultValue);

        mAdapter = new TaskAdapter(this);
        mAdapter.setOnItemClickListener(this);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAdapter);
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        //Load content
        callback = MainActivity.this;

        Bundle bundle = new Bundle();
        bundle.putString(SORT_EXTRA, sortOrder);

        loaderManager = getSupportLoaderManager();
        asyncTaskLoader = loaderManager.getLoader(ID_LOADER);
        if(asyncTaskLoader == null) {
            mAdapter.swapCursor(null);
            loaderManager.initLoader(ID_LOADER, bundle, callback);
        } else {
            mAdapter.swapCursor(null);
            loaderManager.restartLoader(ID_LOADER, bundle, callback);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* Click events in Floating Action Button */

    public void FabClicked(View view){
        Intent intent = new Intent(this, AddTaskActivity.class);
        startActivity(intent);
    }

    /* Click events in RecyclerView items */
    @Override
    public void onItemClick(View v, int position) {
        //COMPLETE: Handle list item click event
        RealmTask myTask = mAdapter.getItem(position);
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra(URI_EXTRA, myTask);
        startActivity(intent);
    }

    /* Click events on RecyclerView item checkboxes */
    @Override
    public void onItemToggled(boolean active, int position) {
        //COMPLETE: Handle task item checkbox event

        RealmTask myTask = mAdapter.getItem(position);

        Uri uriUpdateTask = DatabaseContract.CONTENT_URI
                .buildUpon()
                .appendPath(String.valueOf(myTask.getTask_id()))
                .build();

        ContentValues values = new ContentValues();
        int isComplete = (active)? 1 : 0;

        values.put(DatabaseContract.TaskColumns.IS_COMPLETE, isComplete);
        TaskUpdateService.updateTask(this, uriUpdateTask,values );

    }


    //Loader Interface Methods
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        QueryCursorLoader result = null;
        String sortOrder = args.getString(SORT_EXTRA);
        String sortDefault = getString(R.string.pref_sortBy_default);
        String sortDate = getString(R.string.pref_sortBy_due);
        switch (id) {
            case ID_LOADER:
                if (sortOrder.equals(sortDefault)) {
                    result = new QueryCursorLoader(this, DatabaseContract.CONTENT_URI, DatabaseContract.DEFAULT_SORT);
                } else if(sortOrder.equals(sortDate)) {
                    result = new QueryCursorLoader(this, DatabaseContract.CONTENT_URI, DatabaseContract.DATE_SORT);
                }
                break;
        }
        return result;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        layoutManager.scrollToPosition(currentListPosition);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }



    //SAVING THE STATE

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        currentListPosition = layoutManager.findFirstVisibleItemPosition();
        outState.putInt(CURRENT_POS_EXTRA, currentListPosition);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        currentListPosition = savedInstanceState.getInt(CURRENT_POS_EXTRA);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(PREFERENCES_HAVE_BEEN_UPDATED){
            String myKey = getString(R.string.pref_sortBy_key);
            String defaultValue = getString(R.string.pref_sortBy_default);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            sortOrder = sharedPreferences.getString(myKey, defaultValue);
        }

        Bundle bundle=new Bundle();
        bundle.putString(SORT_EXTRA, sortOrder);
        loaderManager = getSupportLoaderManager();
        loaderManager.restartLoader(ID_LOADER, bundle, callback);
    }

    //SHAREDPREFERENCES LISTENER AND LIFECYCLE MANAGEMENT

    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        PREFERENCES_HAVE_BEEN_UPDATED = true;
    }
}
