package com.example.rgher.realmtodo;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rgher.realmtodo.data.DatabaseContract;
import com.example.rgher.realmtodo.data.QueryCursorLoader;
import com.example.rgher.realmtodo.data.RealmTask;
import com.example.rgher.realmtodo.data.TaskUpdateService;
import com.example.rgher.realmtodo.reminders.AlarmScheduler;
import com.example.rgher.realmtodo.views.DatePickerFragment;

import java.util.Calendar;

public class TaskDetailActivity extends AppCompatActivity implements
        DatePickerDialog.OnDateSetListener {

    private String URI_EXTRA;
    private String BUNDLE_EXTRA;
    private String ALARM_ERROR;
    private static final int ID_LOADER_MAIN = 76;

    private LoaderManager loaderManager;
    private Loader<Cursor> asyncTaskLoader;
    private LoaderManager.LoaderCallbacks<Cursor> callback;
    private Uri taskUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        URI_EXTRA = getString(R.string.extra_uri);
        BUNDLE_EXTRA = getString(R.string.my_loader_uri);
        ALARM_ERROR = getString(R.string.alarm_error);


        Intent startIntent = getIntent();
        RealmTask myTask = startIntent.getParcelableExtra(URI_EXTRA);

        //Uri used by Content Provider
        taskUri = DatabaseContract.CONTENT_URI
                .buildUpon()
                .appendPath(String.valueOf(myTask.getTask_id()))
                .build();

        TextView taskDetails = (TextView) findViewById(R.id.task_details);
        ImageView imageStar = (ImageView) findViewById(R.id.task_star);
        TextView dueDateTextView = (TextView) findViewById(R.id.task_due_date);

        taskDetails.setText(myTask.getDescription());

        if(myTask.getIs_priority()==1){
            Drawable drb = getDrawable(R.drawable.ic_priority);
            imageStar.setImageDrawable(drb);
        } else {
            Drawable drb = getDrawable(R.drawable.ic_not_priority);
            imageStar.setImageDrawable(drb);
        }

        if(myTask.getDue_date() != Long.MAX_VALUE) {
            CharSequence stringTaskDate = DateUtils.getRelativeTimeSpanString(myTask.getDue_date());
            dueDateTextView.setText(stringTaskDate);
        } else {
            dueDateTextView.setText(getString(R.string.date_empty));
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_task_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_delete:
                DeleteItem();
                break;
            case R.id.action_reminder:
                DatePickerFragment dialogFragment = new DatePickerFragment();
                dialogFragment.show(getSupportFragmentManager(), "datePicker");
                break;
        }
        return super.onOptionsItemSelected(item);
    }




    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        //COMPLETE: Handle date selection from a DatePickerFragment
        Calendar c = Calendar.getInstance();
        Long now = c.getTimeInMillis();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        Long dateInMillis = c.getTimeInMillis();
        if (dateInMillis < now) {
            Toast.makeText(this, ALARM_ERROR, Toast.LENGTH_SHORT).show();
        } else {
            AlarmScheduler.scheduleAlarm(this,dateInMillis,taskUri);

        }

    }

    private void DeleteItem() {
        TaskUpdateService.deleteTask(this, taskUri);
        finish();
    }

}
