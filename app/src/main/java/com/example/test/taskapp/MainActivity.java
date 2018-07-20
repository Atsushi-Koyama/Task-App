package com.example.test.taskapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

//テキスト欄に入力のイベントの監視
import android.text.TextWatcher;

import java.util.Objects;

import android.util.Log;

public class MainActivity extends AppCompatActivity implements TextWatcher {
    public final static String EXTRA_TASK = "com.example.test.taskapp.TASK";

    private Realm mRealm;
    private RealmChangeListener mRealmListener = new RealmChangeListener() {
        @Override
        public void onChange(Object element) {
            reloadListView();
        }
    };

    private EditText mEditText;
    private ListView mListView;
    private TaskAdapter mTaskAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //MainActivity.this,からInputActivity.classへ画面遷移を行う
                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                //画面遷移の実行
                startActivity(intent);
            }
        });

        // Realmのインスタンス化
        mRealm = Realm.getDefaultInstance();
        //Realmからデータを取得し、ListViewへ代入するまでの処理
        mRealm.addChangeListener(mRealmListener);

        // ListViewの設定
        mTaskAdapter = new TaskAdapter(MainActivity.this);
        mListView = (ListView) findViewById(R.id.listView1);
        mEditText = (EditText) findViewById(R.id.EditText1);
        mEditText.addTextChangedListener(this);

        // ListViewをタップしたときの処理
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            //タップしたところの情報を取得するpositionはタップした行数idもタップした行数??
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 入力・編集する画面に遷移させる
                Task task = (Task) parent.getAdapter().getItem(position);
                Log.d("中身を確認", String.valueOf(id));

                //画面遷移
                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                intent.putExtra(EXTRA_TASK, task.getId());

                startActivity(intent);
            }
        });

        // ListViewを長押ししたときの処理
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                // タスクを削除する

                final Task task = (Task) parent.getAdapter().getItem(position);

                // ダイアログを表示する
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("削除");
                builder.setMessage(task.getTitle() + "を削除しますか");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //realmからtaskのidと同一のものを取得
                        RealmResults<Task> results = mRealm.where(Task.class).equalTo("id", task.getId()).findAll();
                        //realmの更新、削除等の開始
                        mRealm.beginTransaction();
                        //該当のidのインスタンスの削除準備
                        results.deleteAllFromRealm();
                        //削除の実行
                        mRealm.commitTransaction();

                        Intent resultIntent = new Intent(getApplicationContext(), TaskAlarmReceiver.class);
                        PendingIntent resultPendingIntent = PendingIntent.getBroadcast(
                                MainActivity.this,
                                task.getId(),
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.cancel(resultPendingIntent);

                        //listViewの再読み込み
                        reloadListView();
                    }
                });
                //キャンセ流の場合
                builder.setNegativeButton("CANCEL", null);

                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            }
        });

        reloadListView();
    }

    private void reloadListView() {

        String SearchText = mEditText.getText().toString();

        // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
        RealmResults<Task> taskRealmResults;
        if(Objects.equals(SearchText, "")) taskRealmResults = mRealm.where(Task.class).findAll();
        else taskRealmResults = mRealm.where(Task.class).equalTo("category", SearchText).findAll();

        // 上記の結果を、TaskList としてセットする
        mTaskAdapter.setTaskList(mRealm.copyFromRealm(taskRealmResults));
        // TaskのListView用のアダプタに渡す
        mListView.setAdapter(mTaskAdapter);
        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mRealm.close();
    }

    //文字列が修正される前に呼ばれるメソッド。
    // CharSequenceは現在textEditに入力されている文字列。第二引数は新たに追加される文字列のスタート位置
    //第３引数は文字列の中で変更された文字列の総数、第四引数は新規に追加された文字列の数だと思われる
    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }
    //文字を一文字でも変更した際に呼ばれるメソッド
    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }
    //最後に呼ばれるメソッドeditableには変更された文字列が格納されている。
    @Override
    public void afterTextChanged(Editable editable) {
        reloadListView();
    }
}