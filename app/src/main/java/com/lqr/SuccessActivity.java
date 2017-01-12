package com.lqr;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class SuccessActivity extends Activity implements OnClickListener {

    private TextView text;//视频保存的路径
    private Button button1;//播放开关
    private Button button2;//暂停开关
    private Button button3;//重新播放开关
    private Button button4;//视频大小开关
    private VideoView videoView1;//视频播放控件
    private String file;//视频路径

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);
        Bundle bundle = getIntent().getExtras();
        file = bundle.getString("text");//获得拍摄的短视频保存地址
        init();
        setValue();
    }

    //初始化
    private void init() {
        text = (TextView) findViewById(R.id.text);
        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
        button3 = (Button) findViewById(R.id.button3);
        button4 = (Button) findViewById(R.id.button4);
        videoView1 = (VideoView) findViewById(R.id.videoView1);
    }

    //设置
    private void setValue() {
        text.setText(file);
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
        button4.setOnClickListener(this);
        videoView1.setVideoPath(file);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button1:
                videoView1.start();
                break;

            case R.id.button2:
                videoView1.pause();
                break;

            case R.id.button3:
                videoView1.resume();
                videoView1.start();
                break;

            case R.id.button4:
                Toast.makeText(this, "视频长度：" + (videoView1.getDuration() / 1024) + "M", Toast.LENGTH_SHORT).show();
                break;

            default:
                break;
        }
    }

}
