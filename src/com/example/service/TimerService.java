package com.example.service;

import java.util.Timer;
import java.util.TimerTask;

import com.example.activity.MainActivity;
import com.example.chronometertest.R;
import com.example.util.ConstantUtil;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.Vibrator;

public class TimerService extends Service {

	private int currentProgress;
	private int progress;
	
	// 当退出Activity时，切换成Notification显示
	private boolean startNotification = false;
	
	private Timer mTimer;
	private MyTimerTask mTask;
	
	private SharedPreferences preferences;
	
	private NotificationManager mNotificationManager;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mTimer = new Timer();
		preferences = getSharedPreferences(ConstantUtil.PREFERENCE, Context.MODE_PRIVATE);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mTimer != null) {
			mTimer = null;
		}
		if (mTask != null) {
			mTask.cancel();
			mTask = null;
		}
		if (preferences != null) {
			preferences = null;
		}
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// 必须判断一下，否则可能导致程序无法运行
		if (intent == null) {
			return super.onStartCommand(intent, flags, startId);
		}
		String action = intent.getAction();
		if (action.equals(ConstantUtil.START)) {
			currentProgress = intent.getIntExtra(ConstantUtil.CURRENT_PROGRESS, 0);
			progress = intent.getIntExtra(ConstantUtil.PROGRESS, 0);
			start();
		} else if (action.equals(ConstantUtil.PAUSE)) {
			pause();
		} else if (action.equals(ConstantUtil.STOP)) {
			stop();
		} else if (action.equals(ConstantUtil.START_NOTIFICATION)) {
			startNotification = true;
			createNotification("倒计时中", "还有" + (progress - currentProgress) + "秒~");
		} else if (action.equals(ConstantUtil.DESTROY_NOTIFICATION)) {
			if (mNotificationManager != null) {
				mNotificationManager.cancelAll();
			}
			startNotification = false;
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
	private void start() {
		// 有任务正在进行，发送广播通知Activity
		if (mTask != null) {
			Intent intent = new Intent(ConstantUtil.ALREADY_RUN);
			sendBroadcast(intent);
			return;
		}
		mTask = new MyTimerTask();
		mTimer.schedule(mTask, 1000, 1000);
	}
	
	private void pause() {
		if (mTask != null) {
			mTask.cancel();
			mTask = null;
		}
		// 将当前进度发回activity保存
		Intent intent = new Intent(ConstantUtil.PAUSE);
		intent.putExtra(ConstantUtil.CURRENT_PROGRESS, currentProgress);
		sendBroadcast(intent);
	}
	
	/**
	 * 人为停止计时器
	 */
	private void stop() {
		if (mTask != null) {
			mTask.cancel();
			mTask = null;
		}
	}

	/**
	 * 倒计时完成，停止计时器
	 */
	private void timeout() {
		if (mTask != null) {
			mTask.cancel();
			mTask = null;
		}
		
		Intent intent = new Intent(ConstantUtil.TIMEOUT);
		sendBroadcast(intent);
		
		// 当activity无法收到倒计时完成的消息时，将停止状态保存在sharedpreference中
		// activity再次启动时便可以判断倒计时是否已完成
		Editor editor = preferences.edit();
		editor.putBoolean(ConstantUtil.STOP, true);
		editor.commit();
		
		// 创建通知
		createNotification("Timeout","恭喜你，消耗了" +progress +"秒");
		// 发出系统通知声音和调用震动
		ringAndVibrator();
	}
	
	private void createNotification(String title, String content) {
		Intent contentIntent = new Intent();
		contentIntent.setClass(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, contentIntent, 0);
		if (MainActivity.isDestroy || startNotification) {
			if (mNotificationManager == null) {
				mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			}
			Notification notification = new Notification.Builder(getApplicationContext())
			.setContentTitle(title)
			.setContentText(content)
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentIntent(pendingIntent)
			.setAutoCancel(true)
			.build();
			
			mNotificationManager.notify(0, notification);
		} else {
			if (mNotificationManager != null)
				mNotificationManager.cancelAll();
		}
		
	}

	private void ringAndVibrator() {
		// 控制震动
		Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		long pattern[] = {100, 300, 100, 400};
		vibrator.vibrate(pattern, -1);
		
		// 获取系统通知声音
		Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);  
        MediaPlayer player = new MediaPlayer();  
        try {
			player.setDataSource(this, alert);
		} catch (Exception e) {
			e.printStackTrace();
		} 
        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);  
        if (audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) != 0) {  
        	player.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);  
        	player.setLooping(false);  
        	try {
        		player.prepare();
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        	player.start();  
        }  
	}

	private void running() {
		// 发送广播使activity更新UI
		Intent intent = new Intent(ConstantUtil.RUNNING);
		intent.putExtra(ConstantUtil.CURRENT_PROGRESS, currentProgress);
		sendBroadcast(intent);
	}

	private class MyTimerTask extends TimerTask {

		@Override
		public void run() {
			if (startNotification) {
				createNotification("倒计时中", "还有" + (progress - currentProgress) + "秒~");
			}
			if (++currentProgress >= progress) {
				startNotification = false;
				timeout();
			} else {
				running();
			}
		}
	}
}
