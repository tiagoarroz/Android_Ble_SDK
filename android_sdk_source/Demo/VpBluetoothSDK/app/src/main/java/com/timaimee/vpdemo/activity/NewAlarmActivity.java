package com.timaimee.vpdemo.activity;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.orhanobut.logger.Logger;
import com.timaimee.vpdemo.R;
import com.timaimee.vpdemo.adapter.NewAlarmAdapter;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.base.IBleWriteResponse;
import com.veepoo.protocol.listener.data.IAlarm2DataListListener;
import com.veepoo.protocol.model.datas.AlarmData2;
import com.veepoo.protocol.model.enums.EMultiAlarmOprate;
import com.veepoo.protocol.model.settings.Alarm2Setting;
import com.yanzhenjie.recyclerview.OnItemMenuClickListener;
import com.yanzhenjie.recyclerview.SwipeMenu;
import com.yanzhenjie.recyclerview.SwipeMenuBridge;
import com.yanzhenjie.recyclerview.SwipeMenuCreator;
import com.yanzhenjie.recyclerview.SwipeMenuItem;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;
import com.yanzhenjie.recyclerview.widget.DefaultItemDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Author: YWX
 * Date: 2021/9/30 16:51
 * Description:
 */
public class NewAlarmActivity extends Activity implements NewAlarmAdapter.OnNewAlarmToggleChangeListener {
    private static final String TAG = NewAlarmActivity.class.getSimpleName();
    SwipeRecyclerView mRecyclerView;
    NewAlarmAdapter mAdapter;
    List<Alarm2Setting> mSettings = new ArrayList<>();
    EditText mEditText, etHour, etMinute;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_alarm);
        initNewAlarmListView();
        readNewAlarm();
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Alarm2Setting setting = getAlarm2Setting();
                VPOperateManager.getInstance().addAlarm2(writeResponse, new IAlarm2DataListListener() {
                    @Override
                    public void onAlarmDataChangeListListener(AlarmData2 alarmData2) {
                        Logger.t(TAG).e("Adicionaralarme --" + alarmData2.toString());
                        EMultiAlarmOprate OPT = alarmData2.getOprate();
                        showMsg("Adicionaralarme --" + (alarmData2.getOprate() == EMultiAlarmOprate.SETTING_SUCCESS ? "" : ""));
                        if (OPT == EMultiAlarmOprate.ALARM_FULL) {
                            showMsg("alarme（Adicionar20）");
                        } else if (OPT == EMultiAlarmOprate.SETTING_SUCCESS) {
                            showMsg("alarmeAdicionar");
                            mSettings.clear();
                            mSettings.addAll(alarmData2.getAlarm2SettingList());
                            mAdapter.notifyDataSetChanged();
                        } else if (OPT == EMultiAlarmOprate.SETTING_FAIL) {
                            showMsg("alarmeAdicionar");
                        }
                    }
                }, setting);
            }
        });
        mEditText = findViewById(R.id.et_content);
        mEditText.setVisibility(View.GONE);

        etHour = findViewById(R.id.et_hour);
        etMinute = findViewById(R.id.et_minute);
    }

    /**
     * ，Item
     */
    private SwipeMenuCreator swipeMenuCreator = new SwipeMenuCreator() {
        @Override
        public void onCreateMenu(SwipeMenu swipeLeftMenu, SwipeMenu swipeRightMenu, int position) {
            int width = getResources().getDimensionPixelSize(R.dimen.dp_70);

            // 1. MATCH_PARENT ，Item;
            // 2. ，80;
            // 3. WRAP_CONTENT，，;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;

            // Adicionar，Adicionar，
            {
                SwipeMenuItem addItem = new SwipeMenuItem(NewAlarmActivity.this)
                        .setBackgroundColor(getResources().getColor(R.color.colorAccent))
                        .setText("Eliminar")
                        .setWidth(width)
                        .setHeight(height);
                swipeRightMenu.addMenuItem(addItem);
            }
        }
    };

    /**
     * RecyclerViewItemMenu
     */
    private OnItemMenuClickListener mMenuItemClickListener = new OnItemMenuClickListener() {
        @Override
        public void onItemClick(SwipeMenuBridge menuBridge, int position) {
            menuBridge.closeMenu();

            int direction = menuBridge.getDirection(); // 
            int menuPosition = menuBridge.getPosition(); // RecyclerViewItemPosition

            if (direction == SwipeRecyclerView.RIGHT_DIRECTION) {
                Toast.makeText(NewAlarmActivity.this, "list" + position + "; " + menuPosition, Toast.LENGTH_SHORT)
                        .show();
                VPOperateManager.getInstance().deleteAlarm2(writeResponse, new IAlarm2DataListListener() {
                    @Override
                    public void onAlarmDataChangeListListener(AlarmData2 alarmData2) {
                        EMultiAlarmOprate OPT = alarmData2.getOprate();
                        if (OPT == EMultiAlarmOprate.CLEAR_SUCCESS) {
                            showMsg("alarmeEliminar");
                            mSettings.clear();
                            mSettings.addAll(alarmData2.getAlarm2SettingList());
                            mAdapter.notifyDataSetChanged();
                        } else {
                            showMsg("Eliminar");
                        }
                    }
                    //String bluetoothAddress, int alarmId, int alarmHour, int alarmMinute, String repeatStatus, int scene, String unRepeatDate, boolean isOpen
                }, mSettings.get(position));

            }
        }
    };

    private Alarm2Setting getAlarm2Setting() {
        String strHour = etHour.getText().toString();
        String strMinute = etMinute.getText().toString();
        Alarm2Setting setting = new Alarm2Setting();
        setting.setOpen(true);
        setting.setRepeatStatus("1111111");
        setting.setUnRepeatDate("0000-00-00");
        setting.setAlarmHour(TextUtils.isEmpty(strHour) ? new Random().nextInt(24) : Integer.parseInt(strHour));
        setting.setAlarmMinute(TextUtils.isEmpty(strMinute) ? new Random().nextInt(60) : Integer.parseInt(strMinute));
        return setting;
    }


    private void initNewAlarmListView() {
        mSettings.clear();
        mRecyclerView = findViewById(R.id.rvTextAlarm);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new NewAlarmAdapter(mSettings, this);
        mRecyclerView.setSwipeMenuCreator(swipeMenuCreator);
        mRecyclerView.addItemDecoration(createItemDecoration());
        mRecyclerView.setOnItemMenuClickListener(mMenuItemClickListener);
        mRecyclerView.setAdapter(mAdapter);
    }

    protected RecyclerView.ItemDecoration createItemDecoration() {
        return new DefaultItemDecoration(ContextCompat.getColor(this, R.color.divider_color));
    }

    private void readNewAlarm() {
        VPOperateManager.getInstance().readAlarm2(writeResponse, new IAlarm2DataListListener() {
            @Override
            public void onAlarmDataChangeListListener(AlarmData2 alarmData2) {
                EMultiAlarmOprate OPT = alarmData2.getOprate();
                boolean isOk = OPT == EMultiAlarmOprate.READ_SUCCESS ||
                        OPT == EMultiAlarmOprate.READ_SUCCESS_SAME_CRC ||
                        OPT == EMultiAlarmOprate.READ_SUCCESS_SAVE;
                if (isOk) {
                    mSettings.clear();
                    mSettings.addAll(alarmData2.getAlarm2SettingList());
                    mAdapter.notifyDataSetChanged();
                }
                showMsg(isOk ? "Leralarme" : "Leralarme");
            }
        });

    }

    private IBleWriteResponse writeResponse = new IBleWriteResponse() {
        @Override
        public void onResponse(int code) {

        }
    };

    private void showMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onToggleChanged(Alarm2Setting setting) {
        VPOperateManager.getInstance().modifyAlarm2(writeResponse, new IAlarm2DataListListener() {
            @Override
            public void onAlarmDataChangeListListener(AlarmData2 alarmData2) {
                showMsg("alarme --" + (alarmData2.getOprate() == EMultiAlarmOprate.SETTING_SUCCESS ? "" : ""));
                mSettings.clear();
                mSettings.addAll(alarmData2.getAlarm2SettingList());
                mAdapter.notifyDataSetChanged();
            }
        }, setting);
    }
}
