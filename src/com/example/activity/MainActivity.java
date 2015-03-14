package com.example.activity;


import com.example.chronometertest.R;
import com.example.service.TimerService;
import com.example.util.ConstantUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener{

	public static boolean isDestroy;
	// ���ư����η��ؼ��˳�Ӧ��
	private static boolean isExit = false;
	
	private static final int REQUEST_IMAGE = 1000;
	private static final int DEFAULT_BG = 1001;
	private static final int CHANGE_BG = 1002;
	private static final int DELETE_BG = 1003;
	private static final int SHORT = Toast.LENGTH_SHORT;
	
	private static final String BG_PATH = "yanghp.daojishi.bg_path";

	private Toast toast;
	
	private SharedPreferences preferences = null;
	private SharedPreferences.Editor editor = null;
	
	private RelativeLayout relativeLayout = null;
	private EditText editTime;
	private TextView showView;
	private Button startBtn;
	private Button stopBtn;
	private Button pauseBtn;
	
	private String bgPicturePath = null;
	private int currentProgress = 0;
	private int progress = 0;
	
	private ServiceReceiver receiver;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.activity_main);
        findViewById();
        addListener();
        // ��ȡpreference
        preferences = getPreferences(Context.MODE_PRIVATE);
        isDestroy = false;
        
        bgPicturePath = preferences.getString(BG_PATH, null);
        if (bgPicturePath != null) {
        	BitmapDrawable bd = new BitmapDrawable(getResources(), bgPicturePath);
        	if (bd != null)
        		relativeLayout.setBackground(bd);
        }
        
        currentProgress = preferences.getInt(ConstantUtil.CURRENT_PROGRESS, 0);
        progress = preferences.getInt(ConstantUtil.PROGRESS, 0);
        
        // ��sharedpreference�л�ȡֵ���Ա�ȷ������ʱ�Ƿ����
        SharedPreferences preference1 = getSharedPreferences(ConstantUtil.PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor1 = preference1.edit();
        boolean timeout = preference1.getBoolean(ConstantUtil.STOP, false);
        
        if (timeout) {
        	showView.setText(getResources().getString(R.string.default_text));
//        	isStop = true;
        	currentProgress = 0;
        	editor1.putBoolean(ConstantUtil.STOP, false);
        }
        editor1.commit();
        
        // ����δ����ֹͣ״̬
        if (currentProgress != 0) {
        	showView.setText((progress - currentProgress) + "");
        	editTime.setText(progress + "");
        }
        
        // ��̬ע��receiver
        receiver = new ServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConstantUtil.ALREADY_RUN);
        filter.addAction(ConstantUtil.PAUSE);
        filter.addAction(ConstantUtil.TIMEOUT);
        filter.addAction(ConstantUtil.RUNNING);
        registerReceiver(receiver, filter);
        
        // ���ڱ������Ҵ�ͼ�����Activity��Ӧ�����notification
        Intent intent = new Intent(getApplicationContext(), TimerService.class);
		intent.setAction(ConstantUtil.DESTROY_NOTIFICATION);
		startService(intent);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		if (!isExit) {
    			toast = Toast.makeText(getApplicationContext(), "�ٰ�һ�η��ؼ��л�������", Toast.LENGTH_SHORT);
    			toast.show();
    			isExit = true;
    			MyHandler handler = new MyHandler();
    			handler.sendEmptyMessageDelayed(1, 2000);
    		} else {
    			Intent intent = new Intent(getApplicationContext(), TimerService.class);
    			intent.setAction(ConstantUtil.START_NOTIFICATION);
    			startService(intent);
    			finish();
    		}
    		return true;
    	} else {
    		return super.onKeyDown(keyCode, event);
    	}
    	
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	// ��ȡpreference��editor
    	editor = preferences.edit();
    	
    	// ���汳��ͼƬ��·��
    	editor.putString(BG_PATH, bgPicturePath);
    	
    	// ���浱ǰ��ʱ����״̬
    	editor.putInt(ConstantUtil.CURRENT_PROGRESS, currentProgress);
    	editor.putInt(ConstantUtil.PROGRESS, progress);
    	
    	// �ύ����
    	editor.commit();
    	
    	// ȡ��receiver
    	unregisterReceiver(receiver);
    	
    	// activity������
    	isDestroy = true;
    	
    }
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK && data != null) {
			Uri selectedImage = data.getData();
			String[] filePathColumn = { MediaStore.Images.Media.DATA };
			Cursor cursor = getContentResolver().query(selectedImage,
					filePathColumn, null, null, null);
			cursor.moveToFirst();

			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			// ��ñ���ͼƬ·��
			bgPicturePath = cursor.getString(columnIndex);
			cursor.close();
            
			// ��Bitmap����ת��ΪDrawable����
			BitmapDrawable bd = new BitmapDrawable(getResources(), bgPicturePath);
			relativeLayout.setBackground(bd);
		}
		
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int id = item.getItemId();
		// ��ͼ����ѡȡͼƬ��Ϊ����
		if (id == CHANGE_BG) {
			Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			// ��ͼ��ѡ��һ��ͼƬ�����ؽ��
			startActivityForResult(intent, REQUEST_IMAGE);
		} else if (id == DELETE_BG) {
			bgPicturePath = null;
			relativeLayout.setBackgroundResource(0);
		} else if (id == DEFAULT_BG) {
			relativeLayout.setBackgroundResource(R.drawable.bg);
			bgPicturePath = null;
		}
		return super.onMenuItemSelected(featureId, item);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "�˳�");
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == 0) {
			progress = 0;
			currentProgress = 0;
			System.exit(0);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	private void findViewById() {
		relativeLayout = (RelativeLayout) findViewById(R.id.main);
		editTime = (EditText) findViewById(R.id.edit_time);
		showView = (TextView) findViewById(R.id.show);
		startBtn = (Button) findViewById(R.id.action_start);
		stopBtn = (Button) findViewById(R.id.action_stop);
		pauseBtn = (Button) findViewById(R.id.action_pause);
	}
	
	private void addListener() {
		relativeLayout.setOnCreateContextMenuListener(new CreateContextMenuListener());
		startBtn.setOnClickListener(this);
		stopBtn.setOnClickListener(this);
		pauseBtn.setOnClickListener(this);
		editTime.setOnLongClickListener(new LongClickListener());
	}
	
	private class LongClickListener implements OnLongClickListener {

		@Override
		public boolean onLongClick(View v) {
			editTime.setText(null);
			return true;
		}
		
	}
	
	// �����Ĳ˵�������
	private class CreateContextMenuListener implements OnCreateContextMenuListener {

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo) {
			menu.add(Menu.NONE, DEFAULT_BG, Menu.NONE, "Ĭ�ϱ���");
			menu.add(Menu.NONE, CHANGE_BG, Menu.NONE, "��������");
			menu.add(Menu.NONE, DELETE_BG, Menu.NONE, "ɾ������");
		}
		
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.action_start:
			startTimer();
			break;
		case R.id.action_stop:
			stopTimer();
			break;
		case R.id.action_pause:
			pauseTimer();
			break;
		default:
			break;
		}
	}
	
	private void startTimer() {
		String string = editTime.getText().toString();
		// ���edittext��û������
		if (string  == null || string.equals("")) {
			toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.hint), SHORT);
			toast.show();
			return;
		}
		
		progress = Integer.parseInt(string);
		showView.setText((progress - currentProgress) + "");
		
		// ����������е���ʱ
		Intent intent = new Intent(ConstantUtil.START);
		intent.setClass(getApplicationContext(), TimerService.class);
		intent.putExtra(ConstantUtil.CURRENT_PROGRESS, currentProgress);
		intent.putExtra(ConstantUtil.PROGRESS, progress);
		startService(intent);
		
	}
	
	private void stopTimer() {
		Intent intent = new Intent(ConstantUtil.STOP);
		intent.setClass(getApplicationContext(), TimerService.class);
		startService(intent);
		
		showView.setText(getResources().getString(R.string.default_text));
		// ����currentProgress
		currentProgress = 0;
		
	}
	
	private void pauseTimer() {
		Intent intent = new Intent(ConstantUtil.PAUSE);
		intent.setClass(getApplicationContext(), TimerService.class);
		startService(intent);
	}
	
	private class ServiceReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(ConstantUtil.ALREADY_RUN)) {
				toast = Toast.makeText(getApplicationContext(), "�������ڽ�����...", SHORT);
				toast.show();
			} else if (action.equals(ConstantUtil.PAUSE)) {
				int data = intent.getIntExtra(ConstantUtil.CURRENT_PROGRESS, 0);
				currentProgress = data;
			} else if (action.equals(ConstantUtil.RUNNING)) {
				int data = intent.getIntExtra(ConstantUtil.CURRENT_PROGRESS, 0);
				currentProgress = data;
				showView.setText((progress - currentProgress) + "");
			} else if (action.equals(ConstantUtil.TIMEOUT)) {
				currentProgress = 0;
				showView.setText(getResources().getString(R.string.default_text));
				AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
				.setTitle("Timeout")
				.setMessage("��ϲ��������" + progress + "��")
				.setPositiveButton("�õ�", null)
				.create();
				dialog.show();
			}
		}
	}
	
	private static class MyHandler extends Handler {
    	@Override
    	public void handleMessage(Message msg) {
    		super.handleMessage(msg);
    		isExit = false;
    	}
    }
}
