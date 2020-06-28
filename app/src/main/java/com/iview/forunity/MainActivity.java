package com.iview.forunity;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.iview.an4unity.JsonHelper;
import com.iview.commonclient.CommonManager;
import com.iview.commonclient.OnCommonListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public final static String TAG = "MainActivity";

    Button button1;
    Button button2;
    Button button3;
    Button button4;
    Button button5;

    Button projectorSwitchButton;


    EditText hStepsEdit;
    EditText vStepsEdit;
    EditText hDirEdit;
    EditText vDirEdit;
    EditText delayEdit;
    Button multiConfirmButton;

    HandlerThread mHandlerThread;
    Handler mHandler;

    List<MotorData> mMotorDataList = new ArrayList<>();

    public static final int HMotor = 1;
    public static final int VMotor = 2;
    public static final int MultiMotor = 10;

    public static final int HEncoder = 1;
    public static final int VEncoder = 0;

    public static final int HMotorLeftDirection = 0;
    public static final int HMotorRightDirection = 1;

    public static final int VMotorUpDirection = 0;
    public static final int VMotorDownDirection = 1;

    public final static int MSG_CONTROL_H_MOTOR = 0;
    public final static int MSG_CONTROL_D_MOTOR = 1;
    public final static int MSG_STOP_D_MOTOR = 2;

    int[] delayTime = {1000, 1000, 3000, 3000};
    int cmdIndex = 0;

    private boolean bServiceConnect;
    private CommonManager commonManager;
    private CommonListener commonListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bServiceConnect = false;
        commonManager = CommonManager.getInstance(this);
        commonListener = new CommonListener();
        commonManager.regisiterOnCommonListener(commonListener);
        commonManager.connect();

        initMotorData();

        initView();
        initData();
    }

    @Override
    protected void onPause() {
        super.onPause();

        bServiceConnect = false;
        if (commonManager != null) {
            commonManager.unregisiterOnCommonListener(commonListener);

            commonManager.stopMotorRunning(CommonManager.HMotor);
            commonManager.stopMotorRunning(CommonManager.VMotor);

            commonManager.disconnect();
            commonManager = null;
        }

    }

    private void initView() {
        button1 = findViewById(R.id.button1);
        button1.setOnClickListener(this);

        button2 = findViewById(R.id.button2);
        button2.setOnClickListener(this);

        button3 = findViewById(R.id.button3);
        button3.setOnClickListener(this);

        button4 = findViewById(R.id.button4);
        button4.setOnClickListener(this);

        button5 = findViewById(R.id.button5);
        button5.setOnClickListener(this);

        projectorSwitchButton = findViewById(R.id.projectorSwitch);
        projectorSwitchButton.setOnClickListener(this);

        vStepsEdit = findViewById(R.id.vStepEdit);
        hStepsEdit = findViewById(R.id.hStepEdit);
        hDirEdit = findViewById(R.id.hDirEdit);
        vDirEdit = findViewById(R.id.vDirEdit);
        delayEdit = findViewById(R.id.delayEdit);

        multiConfirmButton = findViewById(R.id.multiConfirm);
        multiConfirmButton.setOnClickListener(this);
    }

    private void initMotorData() {
        mMotorDataList.clear();

        mMotorDataList.add(new MotorData(500, 0, 500, 0, 500, false));
        mMotorDataList.add(new MotorData(500, 0, 500, 0, 500, false));
        mMotorDataList.add(new MotorData(500, 0, 500, 0, 500, false));
        mMotorDataList.add(new MotorData(500, 0, 500, 0, 500, false));
        mMotorDataList.add(new MotorData(500, 0, 500, 0, 500, false));

        mMotorDataList.add(new MotorData(500, 1, 500, 1, 500, false));
        mMotorDataList.add(new MotorData(500, 1, 500, 1, 500, false));
        mMotorDataList.add(new MotorData(500, 1, 500, 1, 500, false));
        mMotorDataList.add(new MotorData(500, 1, 500, 1, 500, false));
        mMotorDataList.add(new MotorData(500, 1, 500, 1, 500, false));


    }



    @Override
    public void onClick(View view) {
        if (!bServiceConnect) {
            return;
        }
        switch (view.getId()) {
            case R.id.button1:
                controlMotor(commonManager.HMotor, 10000, commonManager.HMotorLeftDirection, 500);
                break;
            case R.id.button2:
                controlMotor(commonManager.HMotor,10000, commonManager.HMotorRightDirection, 500);
                break;
            case R.id.button3:
                controlMotor(commonManager.VMotor,10000, commonManager.VMotorUpDirection, 1000 );
                break;
            case R.id.button4:
                controlMotor(commonManager.VMotor,10000, commonManager.VMotorDownDirection, 1000 );
                break;
            case R.id.button5:

                cmdIndex = 0;


                mHandler.sendEmptyMessageDelayed(MSG_CONTROL_D_MOTOR, 0);

                break;

            case R.id.projectorSwitch:
                switchProjector(0);

                mHandler.postDelayed((new Runnable() {
                    @Override
                    public void run() {
                        switchProjector(1);
                    }
                }), 4000);
                break;

            case R.id.multiConfirm:
                int hSteps = Integer.parseInt(hStepsEdit.getText().toString());
                int vSteps = Integer.parseInt(vStepsEdit.getText().toString());
                int hDir = Integer.parseInt(hDirEdit.getText().toString());
                int vDir = Integer.parseInt(vDirEdit.getText().toString());
                int delay = Integer.parseInt(delayEdit.getText().toString());

              //  controlMultiMotor2(hSteps, vSteps, hDir, vDir, delay);
                break;
        }
    }

    private void initData() {
        mHandlerThread = new HandlerThread("UnityMotor Thread");
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                if (!bServiceConnect) {
                    return;
                }
                switch (msg.what) {
                    case MSG_CONTROL_D_MOTOR:
                        if (mMotorDataList.isEmpty() || cmdIndex > mMotorDataList.size() - 1) {
                            commonManager.stopMotorRunning(commonManager.HMotor);
                            commonManager.stopMotorRunning(commonManager.VMotor);
                            sendStopMotorList();
                            return;
                        }

                        Log.e(TAG, "execute cmdindex :" + cmdIndex);
                        Log.e(TAG, "execute hSpeed:" + mMotorDataList.get(cmdIndex).hSpeed +
                                ", hDirection: " + mMotorDataList.get(cmdIndex).hDirection +
                                ", vSpeed:" + mMotorDataList.get(cmdIndex).vSpeed +
                                ", vDirection: " + mMotorDataList.get(cmdIndex).vDirection +
                                ", running time:" + mMotorDataList.get(cmdIndex).runningTime);

                        commonManager.setMotorSpeed(commonManager.HMotor, mMotorDataList.get(cmdIndex).hSpeed);
                        commonManager.setMotorDirection(commonManager.HMotor, mMotorDataList.get(cmdIndex).hDirection);

                        final boolean bCheckLimitSwitch = mMotorDataList.get(cmdIndex).bCheckLimitSwitch;

                        if (mMotorDataList.get(cmdIndex).hSpeed != 0) {
                            commonManager.startMotorRunning(commonManager.HMotor, bCheckLimitSwitch);

                        } else if (mMotorDataList.get(cmdIndex).hSpeed == 0) {
                            commonManager.stopMotorRunning(commonManager.HMotor);
                        }

                        commonManager.setMotorSpeed(commonManager.VMotor, mMotorDataList.get(cmdIndex).vSpeed);
                        commonManager.setMotorDirection(commonManager.VMotor, mMotorDataList.get(cmdIndex).vDirection);

                        if (mMotorDataList.get(cmdIndex).vSpeed != 0) {
                            commonManager.startMotorRunning(commonManager.VMotor, bCheckLimitSwitch);
                        } else if (mMotorDataList.get(cmdIndex).vSpeed == 0) {
                            commonManager.stopMotorRunning(commonManager.VMotor);
                        }

                        mHandler.sendEmptyMessageDelayed(MSG_CONTROL_D_MOTOR, mMotorDataList.get(cmdIndex).runningTime);
                        sendStartMotorList(cmdIndex);
                        cmdIndex++;
                        break;
                    case MSG_STOP_D_MOTOR:
                        commonManager.stopMotorRunning(commonManager.HMotor);
                        commonManager.stopMotorRunning(commonManager.VMotor);

                        sendMotorStopMsg(MultiMotor);
                        break;

                }
            }
        };
    }

    /**
     * 设置unity回调信息， android会通过 UnitySendMessage(String var0, String var1, String var2) 回调unity接口
     * @param unityPacakage : var0
     * @param unityMethod : var1
     */
    public void setUnityCallBack(String unityPacakage, String unityMethod) {
//        this.unityPacakage = unityPacakage;
//        this.unityMethod = unityMethod;
    }



    /**
     * 控制马达转动， 转动结束会通过 UnitySendMessage 通知unity
     * @param motorId: 马达id
    {
    @link HMotor 水平方向马达
    @link VMotor 垂直方向马达
    }
     * @param steps : PWM脉冲数量
     * @param dir : 转动方向
     * {
    @link    HMotorLeftDirection
    @link    HMotorRightDirection
    @link    VMotorUpDirection
    @link    VMotorDownDirection
     * }
     * @param delay 马达脉冲持续时间，单位us， 数值越小速度越快
     */
    public int controlMotor(final int motorId, final int steps, final int dir, final int delay) {
        controlMotor(motorId, steps, dir, delay, true);

        return 0;
    }

    /**
     * 控制马达转动， 转动结束会通过 UnitySendMessage 通知unity
     * @param motorId: 马达id
    {
    @link HMotor 水平方向马达
    @link VMotor 垂直方向马达
    }
     * @param steps : PWM脉冲数量
     * @param dir : 转动方向
     * {
    @link    HMotorLeftDirection
    @link    HMotorRightDirection
    @link    VMotorUpDirection
    @link    VMotorDownDirection
     * }
     * @param delay 马达脉冲持续时间，单位us， 数值越小速度越快
     * @param bCheckLimitSwitch: 是否判断限位  true 判断， false 不判断
     */
    public int controlMotor(final int motorId, final int steps, final int dir, final int delay, final boolean bCheckLimitSwitch) {
        if (bServiceConnect) {
            if (steps != 0 && delay != 0) {
                commonManager.controlMotorAsync(motorId, steps, dir, delay, bCheckLimitSwitch);
            }
        }

        return 0;
    }

    /**
     * 同时控制两个马达转动， 转动结束会通过 UnitySendMessage 通知unity
     * @param hDir : 水平马达转动方向
     * {
    @link    HMotorLeftDirection
    @link    HMotorRightDirection
     * }
     * @param hDelay: 水平马达脉冲持续时间， 单位us， 数值越小速度越快
     * @param vDir ： 垂直马达转动方向
     * {
    @link    VMotorUpDirection
    @link    VMotorDownDirection
     * }
     * @param vDelay 垂直马达脉冲持续时间，单位us， 数值越小速度越快
     * @param duration 马达转动时间， 单位ms
     */
    public int controlMultiMotor(final int hDir, final int hDelay, final int vDir, final int vDelay, final int duration) {

        controlMultiMotor(hDir, hDelay, vDir, vDelay, duration, true);

        return 0;
    }

    /**
     * 同时控制两个马达转动， 转动结束会通过 UnitySendMessage 通知unity
     * @param hDir : 水平马达转动方向
     * {
    @link    HMotorLeftDirection
    @link    HMotorRightDirection
     * }
     * @param hDelay: 水平马达脉冲持续时间， 单位us， 数值越小速度越快
     * @param vDir ： 垂直马达转动方向
     * {
    @link    VMotorUpDirection
    @link    VMotorDownDirection
     * }
     * @param vDelay 垂直马达脉冲持续时间，单位us， 数值越小速度越快
     * @param duration 马达转动时间， 单位ms
     * @param bCheckLimitSwitch: 是否判断限位  true 判断， false 不判断
     */
    public int controlMultiMotor(final int hDir, final int hDelay, final int vDir, final int vDelay, final int duration, final boolean bCheckLimitSwitch) {

        if (bServiceConnect) {
            commonManager.setMotorSpeed(commonManager.HMotor, hDelay);
            commonManager.setMotorDirection(commonManager.HMotor, hDir);
            if (hDelay != 0) {
                commonManager.startMotorRunning(commonManager.HMotor, bCheckLimitSwitch);
            }

            commonManager.setMotorSpeed(commonManager.VMotor, vDelay);
            commonManager.setMotorDirection(commonManager.VMotor, vDir);
            if (vDelay != 0) {
                commonManager.startMotorRunning(commonManager.VMotor, bCheckLimitSwitch);
            }

            mHandler.sendEmptyMessageDelayed(MSG_STOP_D_MOTOR, duration);
        }

        return 0;
    }

    /**
     * 同时控制两个马达转动， 转动结束会通过 UnitySendMessage 通知unity
     * @param hAngle : 水平马达转动角度
     * @param vAngle ： 垂直马达转动角度
     * @param duration 马达转动时间， 单位ms
     */
    public int controlMultiMotor(float hAngle, float vAngle, final int duration) {
        return 0;
    }

    /**
     * 停止马达转动
     * @param motorId
     * {
     *     @link HMotor 水平方向马达
     *     @link VMotor 垂直方向马达
     * }
     */
    public void stopMotor(int motorId) {
        if (bServiceConnect) {
            commonManager.stopMotorRunning(motorId);
        }

    }

    /**
     * 清空马达转动指令列表
     */
    public void clearMotorDataList() {
        mMotorDataList.clear();
    }

    /**
     * 添加马达转动指令
     * @param runningTime ：马达转动时间
     * @param hDirection ： 水平马达转动方向
     * @param hDelay ： 水平马达转动速度
     * @param vDirection ： 垂直马达转动方向
     * @param vDelay ： 垂直马达转动速度
     */
    public void addMotorData(int runningTime, int hDirection, int hDelay, int vDirection, int vDelay) {
        addMotorData(runningTime, hDirection, hDelay, vDirection, vDelay, true);
    }

    /**
     * 添加马达转动指令
     * @param runningTime ：马达转动时间
     * @param hDirection ： 水平马达转动方向
     * @param hDelay ： 水平马达转动速度
     * @param vDirection ： 垂直马达转动方向
     * @param vDelay ： 垂直马达转动速度
     * @param bCheckLimitSwitch: 是否判断限位  true 判断， false 不判断
     */
    public void addMotorData(int runningTime, int hDirection, int hDelay, int vDirection, int vDelay, boolean bCheckLimitSwitch) {
        mMotorDataList.add(new MotorData(runningTime, hDirection, hDelay, vDirection, vDelay, bCheckLimitSwitch));
    }

    /**
     * 让马达按照设置的直接开始运行，每开始执行一条指令会通过接口通知unity
     */
    public void startMotorRunning() {
        cmdIndex = 0;
        mHandler.sendEmptyMessageDelayed(MSG_CONTROL_D_MOTOR, 0);
    }

    public class MotorData {
        public int runningTime;
        public int hDirection;
        public int hSpeed;
        public int vDirection;
        public int vSpeed;
        public boolean bCheckLimitSwitch;

        MotorData(int runningTime, int hDirection, int hSpeed, int vDirection, int vSpeed, boolean bCheckLimitSwitch) {
            this.runningTime = runningTime;
            this.hDirection = hDirection;
            this.hSpeed = hSpeed;
            this.vDirection = vDirection;
            this.vSpeed = vSpeed;
            this.bCheckLimitSwitch = bCheckLimitSwitch;
        }
    }

    /**
     * 同时控制两个马达转动， 参数选择不合适两个马达可能无法同时停止， 转动结束会通过 UnitySendMessage 通知unity
     * @param hSteps 水平马达转动的脉冲数
     * @param vSteps 垂直马达转动的脉冲数
     * @param hDir : 水平马达转动方向
     * {
    @link    HMotorLeftDirection
    @link    HMotorRightDirection
     * }
     * @param vDir ： 垂直马达转动方向
     * {
    @link    VMotorUpDirection
    @link    VMotorDownDirection
     * }
     * @param duration 马达运行时间，单位us
     * @param bCheckLimitSwitch: 是否判断限位  true 判断， false 不判断
     */
    public void controlMotors(final int hSteps, final int vSteps, final int hDir, final int vDir, final int duartion, final boolean bCheckLimitSwitch) {

        if (bServiceConnect) {
            if (hSteps != 0) {
                int delay = duartion / hSteps;
                if (delay != 0) {
                    commonManager.controlMotorAsync(HMotor, hSteps, hDir, delay, bCheckLimitSwitch);
                }
            }

            if (vSteps != 0) {
                int delay = duartion / vSteps;
                if (delay != 0) {
                    commonManager.controlMotorAsync(VMotor, vSteps, vDir, delay, bCheckLimitSwitch);
                }
            }
        }
    }

    /**
     * 开关投影灯
     * @param enable : 0：关  1：开
     */
    public void switchProjector(int enable) {
        if (bServiceConnect) {
            commonManager.switchProjector(enable);
        }

    }


    private void sendMotorStopMsg(int motorId) {
        String motor = null;
        switch(motorId) {
            case HMotor:
                motor = "horizotalMotor";
                break;
            case VMotor:
                motor = "verticalMotor";
                break;
            case MultiMotor:
                motor = "multiMotor";
                break;
        }

        JSONObject argJson = new JSONObject();
        try {
            argJson.put("motor", motor);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject notifyJson = JsonHelper.generateNotifyMsg("callback", "onMotorStop", argJson);
   //     UnityPlayer.UnitySendMessage(unityPacakage, unityMethod, notifyJson.toString());
    }

    private void sendStartMotorList(int index) {
        JSONObject argJson = new JSONObject();
        try {
            argJson.put("index", index);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject notifyJson = JsonHelper.generateNotifyMsg("callback", "startMotorList", argJson);
    //    UnityPlayer.UnitySendMessage(unityPacakage, unityMethod, notifyJson.toString());
    }

    private void sendStopMotorList() {

        JSONObject notifyJson = JsonHelper.generateNotifyMsg("callback", "stopMotorList", null);
    //    UnityPlayer.UnitySendMessage(unityPacakage, unityMethod, notifyJson.toString());
    }


    private class CommonListener implements OnCommonListener {

        @Override
        public void onServiceConnect() {
            bServiceConnect = true;
            Log.e(TAG, "onServiceConnect");
        }

        @Override
        public void onMotorStop(int i) {
            Log.e(TAG, "onMotorStop");
        //    sendMotorStopMsg(i);
        }
    }
}
