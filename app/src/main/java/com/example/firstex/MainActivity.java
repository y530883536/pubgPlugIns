package com.example.firstex;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class MainActivity extends AppCompatActivity {

    private Button ipButton;
    private TextView ipTextView;

    private Button gButton;
    private TextView gTextView;

    private PrintWriter cmdPrintWriter = null;
    private BufferedReader cmdBufferedReader = null;

    private FileWriter logFileWriter = null;

    Process process = null;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            cmdPrintWriter.close();
            cmdBufferedReader.close();
            logFileWriter.close();
            process.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //创建日志文件
        String logPath = Environment.getExternalStorageDirectory() + "/qwer";
        File file = new File(logPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        File logFile = new File(logPath + File.separator + "ycfLog.txt");
        MediaScannerConnection.scanFile(this, new String[] { file.getAbsolutePath() }, null, null);
        MediaScannerConnection.scanFile(this, new String[] { logFile.getAbsolutePath() }, null, null);
        //日他妈的，这句话不加的话日志不会出现
        //logFile.setReadable(true);
        //logFile.setWritable(true);

        try {
            process = Runtime.getRuntime().exec("su");
            cmdPrintWriter = new PrintWriter(process.getOutputStream());
            cmdBufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            logFileWriter = new FileWriter(logFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //创建一个新线程，监听cmd的输出结果，并把输出结果写到日志文件中
        Thread logThread = new Thread(() -> {
            String msg;
            try {
                while ((msg = cmdBufferedReader.readLine()) != null) {
                    logFileWriter.write(msg);
                    logFileWriter.write("\n");
                    logFileWriter.flush();
                    MediaScannerConnection.scanFile(this, new String[] { logFile.getAbsolutePath() }, null, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        logThread.start();

        //todo 测试，之后要去掉
        Thread test = new Thread(() -> {
            String msg;
            try {
                //这里是可以拿到文件大小的，说明可以读取到文件，但是为什么下面的语句不生效呢？
                /*while(true) {
                    Thread.sleep(1000);
                    System.out.println(logFile.length());
                }*/
                BufferedReader br = new BufferedReader(new FileReader(logFile));
                while ((msg = br.readLine()) != null) {
                    System.out.println("test:" + msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        test.start();

        ipButton = findViewById(R.id.ipButton);
        ipTextView = findViewById(R.id.ipTextView);

        gButton = findViewById(R.id.gButton);
        gTextView = findViewById(R.id.gTextView);

        ipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder sb = new StringBuilder("本机IP：");
                //获取系统的连接服务
                try{
                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if(wifiInfo.getIpAddress() == 0) {
                        sb.append("wifi未连接");
                    } else {
                        sb.append(int2ip(wifiInfo.getIpAddress()));
                    }
                } catch(Exception e) {
                    sb.append("网络连接失败，请设置正确的网络连接");
                }
                ipTextView.setText(sb.toString());
                //todo 下面的代码后面要去掉的
                gButton.setEnabled(true);
            }
        });

        gButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //点击这个按钮之后按钮不可用，避免手贱重复点击
                gButton.setEnabled(false);
                gTextView.setTextColor(Color.RED);
                try{
                    gTextView.setText("程序正在启动中，请稍等······");
                    commandInstruct("./data/ycf/helloworld");
                } catch (Exception e) {
                    e.printStackTrace();
                    gTextView.setText("程序启用出现问题，请重试" + e.getMessage());
                }
            }
        });
    }

    private String int2ip(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }

    //运行指令的函数
    private void commandInstruct(String instruct) {
        //todo 这里执行指令（新建文件夹，复制文件，chmod 777等）
        cmdPrintWriter.println(instruct);
        cmdPrintWriter.flush();
    }
}
