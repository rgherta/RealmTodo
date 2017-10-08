package com.example.rgher.realmtodo.data;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;

import io.realm.DynamicRealm;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;
import io.realm.RealmResults;
import io.realm.RealmSchema;

import com.example.rgher.realmtodo.data.DatabaseContract.TaskColumns;

public class TaskProvider extends ContentProvider {
    private static final String TAG = TaskProvider.class.getSimpleName();

    private static final int CLEANUP_JOB_ID = 43;
    private static final int TASKS = 100;
    private static final int TASKS_WITH_ID = 101;

    private Realm realm;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        // content://com.example.rgher.realmtodo/tasks
        sUriMatcher.addURI(DatabaseContract.CONTENT_AUTHORITY,
                DatabaseContract.TABLE_TASKS,
                TASKS);

        // content://com.example.rgher.realmtodo/tasks/id
        sUriMatcher.addURI(DatabaseContract.CONTENT_AUTHORITY,
                DatabaseContract.TABLE_TASKS + "/#",
                TASKS_WITH_ID);
    }

    @Override
    public boolean onCreate() {

        //Innitializing RealmDB
        Realm.init(getContext());
        RealmConfiguration config = new RealmConfiguration.Builder()
                .schemaVersion(1)
                .migration(new MyRealmMigration())
                .build();
        Realm.setDefaultConfiguration(config);

        manageCleanupJob();

        return true;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null; /* Not used */
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        int match = sUriMatcher.match(uri);

        //Get Realm Instance
        realm = Realm.getDefaultInstance();
        MatrixCursor myCursor = new MatrixCursor( new String[]{TaskColumns._ID, TaskColumns.DESCRIPTION
                , TaskColumns.IS_COMPLETE, TaskColumns.IS_PRIORITY
                , TaskColumns.DUE_DATE
        });

        switch (match) {
            //Expected "query all" Uri: content://com.example.rgher.realmtodo/tasks

            case TASKS:
                RealmResults<RealmTask> tasksRealmResults = realm.where(RealmTask.class).findAll();
                for(RealmTask myTask : tasksRealmResults) {
                    Object[] rowData = new Object[] {myTask.getTask_id(), myTask.getDescription(), myTask.getIs_complete(), myTask.getIs_priority(), myTask.getDue_date()};
                    myCursor.addRow(rowData);
                    Log.v("RealmDB", myTask.toString());
                }
                break;


            //Expected "query one" Uri: content://com.example.rgher.realmtodo/tasks/{id}
            case TASKS_WITH_ID:
                Integer id = Integer.parseInt(uri.getPathSegments().get(1));
                RealmTask myTask = realm.where(RealmTask.class).equalTo("task_id", id).findFirst();
                myCursor.addRow(new Object[] {myTask.getTask_id(), myTask.getDescription(), myTask.getIs_complete(), myTask.getIs_priority(), myTask.getDue_date()});
                Log.v("RealmDB", myTask.toString());
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        myCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return myCursor;

    }

    @Nullable
    @Override
    public Uri insert(Uri uri, final ContentValues contentValues) {
        //COMPLETE: Expected Uri: content://com.example.rgher.realmtodo/tasks

        //final SQLiteDatabase taskDb = mDbHelper.getReadableDatabase();
        int match = sUriMatcher.match(uri);
        Uri returnUri;

        //Get Realm Instance
        realm = Realm.getDefaultInstance();

        switch (match){
            case TASKS:
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {

                        Number currId = realm.where(RealmTask.class).max(TaskColumns._ID);
                        Integer nextId = (currId == null)? 1 : currId.intValue() + 1;

                        RealmTask myNewTask = realm.createObject(RealmTask.class, nextId);
                        myNewTask.setDescription(contentValues.get(TaskColumns.DESCRIPTION).toString());
                        myNewTask.setIs_complete((Integer) contentValues.get(TaskColumns.IS_COMPLETE));
                        myNewTask.setIs_priority((Integer) contentValues.get(TaskColumns.IS_PRIORITY));
                        myNewTask.setDue_date((Long) contentValues.get(TaskColumns.DUE_DATE));
                    }
                });
                returnUri = ContentUris.withAppendedId(DatabaseContract.CONTENT_URI, '1');
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        //Expected Uri: content://com.example.rgher.realmtodo/tasks/{id}
        realm = Realm.getDefaultInstance();

        int match = sUriMatcher.match(uri);
        int nrUpdated=0;

        switch (match){
            case TASKS_WITH_ID:
                Integer id = Integer.parseInt(uri.getPathSegments().get(1));
                RealmTask myTask = realm.where(RealmTask.class).equalTo("task_id", id).findFirst();
                realm.beginTransaction();
                myTask.setIs_complete(Integer.parseInt(values.get(TaskColumns.IS_COMPLETE).toString()));
                if(values.get(TaskColumns.DUE_DATE) != null) {
                    myTask.setDue_date(Long.valueOf(values.get(TaskColumns.DUE_DATE).toString()));
                }
                nrUpdated++;
                realm.commitTransaction();
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (nrUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return nrUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        realm = Realm.getDefaultInstance();

        switch (sUriMatcher.match(uri)) {
            case TASKS:
                selection = (selection == null) ? "1" : selection;
                RealmResults<RealmTask> tasksRealmResults = realm.where(RealmTask.class).equalTo(selection, Integer.parseInt(selectionArgs[0])).findAll();
                realm.beginTransaction();
                tasksRealmResults.deleteAllFromRealm();
                count++;
                realm.commitTransaction();
                break;
            case TASKS_WITH_ID:
                Integer id = Integer.parseInt(String.valueOf(ContentUris.parseId(uri)));
                RealmTask myTask = realm.where(RealmTask.class).equalTo("task_id", id).findFirst();
                realm.beginTransaction();
                myTask.deleteFromRealm();
                count++;
                realm.commitTransaction();
                break;
            default:
                throw new IllegalArgumentException("Illegal delete URI");
        }

        if (count > 0) {
            //Notify observers of the change
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    /* Initiate a periodic job to clear out completed items */
    private void manageCleanupJob() {
        Log.d(TAG, "Scheduling cleanup job");
        JobScheduler jobScheduler = (JobScheduler) getContext()
                .getSystemService(Context.JOB_SCHEDULER_SERVICE);

        //Run the job approximately every hour
        long jobInterval = DateUtils.HOUR_IN_MILLIS;

        ComponentName jobService = new ComponentName(getContext(), CleanupJobService.class);
        JobInfo task = new JobInfo.Builder(CLEANUP_JOB_ID, jobService)
                .setPeriodic(jobInterval)
                .setPersisted(true)
                .build();

        if (jobScheduler.schedule(task) != JobScheduler.RESULT_SUCCESS) {
            Log.w(TAG, "Unable to schedule cleanup job");
        }
    }
}

// Example of REALM migration
class MyRealmMigration implements RealmMigration {
    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {

        RealmSchema schema = realm.getSchema();

        if (oldVersion != 0) {
            schema.create(DatabaseContract.TABLE_TASKS)
                    .addField(DatabaseContract.TaskColumns._ID, Integer.class)
                    .addField(DatabaseContract.TaskColumns.DESCRIPTION, String.class)
                    .addField(DatabaseContract.TaskColumns.IS_COMPLETE, Integer.class)
                    .addField(DatabaseContract.TaskColumns.IS_PRIORITY, Integer.class);
            oldVersion++;
        }

    }
}
