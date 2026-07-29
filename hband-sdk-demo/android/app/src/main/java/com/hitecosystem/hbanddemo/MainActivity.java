package com.hitecosystem.hbanddemo;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        registerPlugin(HBandPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
