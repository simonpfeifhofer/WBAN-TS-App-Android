package com.wban_ts;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.UUID;

public class LiveMonitor extends FragmentActivity {

    private final static String TAG = LiveMonitor.class.getSimpleName();

    private static final int REQUEST_ENABLE_BT = 12;

    private DeviceScan mDeviceScan;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;

    private DataUpdateReceiver mReceiver = new DataUpdateReceiver();

    private PreferencesHandler mPreferenceHandler;

    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(BluetoothLeService.ACTION_DATA_AVAILABLE)) {

                String value = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                ((TextView)findViewById(R.id.bpm_label)).setText(
                        String.format(
                                "%s bpm",
                                value
                        )
                );

            }

            if (intent.getAction().equals(GPSService.ACTION_DATA_AVAILABLE)) {

                String value = intent.getStringExtra(GPSService.EXTRA_DATA);
                LatLng latLong = new LatLng(Double.parseDouble(value.split("/")[0]), Double.parseDouble(value.split("/")[1]));
                mMap.clear();
                mMap.addMarker(
                        new MarkerOptions()
                                .position(latLong)
                );
                mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                                latLong,
                                16
                        )
                );

            }

        }
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mBluetoothServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final ServiceConnection mLocationServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.e(TAG, "Location-Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, "Location-Service disconnected");
        }
    };

    private boolean mBluetoothServiceStarted = false;

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    mDeviceAddress = device.getAddress();
                    if(device.getName().contains("HRM") && !mBluetoothServiceStarted) {
                        mBluetoothServiceStarted = true;
                        StartBluetoothService();
                    }
                }
            };

    private void StartBluetoothService(){
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mBluetoothServiceConnection, BIND_AUTO_CREATE);
    }

    private void StartLocationService(){
        Intent locationServiceIntent = new Intent(this, GPSService.class);
        bindService(locationServiceIntent, mLocationServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_monitor);
        setUpMapIfNeeded();

        mPreferenceHandler = new PreferencesHandler(PreferenceManager.getDefaultSharedPreferences(this));
        performActivityVisibilitySettings();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        filter.addAction(GPSService.ACTION_DATA_AVAILABLE);
        registerReceiver(mReceiver, filter);
        Log.i(TAG, "Register DataUpdateReceiver registered");

        final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        mDeviceScan = new DeviceScan(mBluetoothAdapter, mLeScanCallback);
        mDeviceScan.ScanLeDevice(true);

        StartLocationService();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        Log.i(TAG, "Register DataUpdateReceiver unregistered");
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        //centerMapOnMyLocation();
    }

    private void centerMapOnMyLocation() {

        //mMap.setMyLocationEnabled(true);

        Location location = mMap.getMyLocation();

        if (location != null) {
            LatLng latLong = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            latLong,
                            16
                    )
            );
        }

    }

    private void performActivityVisibilitySettings(){

        Button startBtn = (Button) findViewById(R.id.start_activity_button);
        Button stopBtn = (Button) findViewById(R.id.stop_activity_button);

        if(mPreferenceHandler.GetActivityId() == ""){
            stopBtn.setVisibility(View.GONE);
            startBtn.setVisibility(View.VISIBLE);
        }
        else{
            startBtn.setVisibility(View.GONE);
            stopBtn.setVisibility(View.VISIBLE);
        }

    }

    public void startActivity(View view) {

        mPreferenceHandler.SetActivityId(UUID.randomUUID().toString());
        performActivityVisibilitySettings();

    }

    public void stopActivity(View view) {

        mPreferenceHandler.SetActivityId("");
        performActivityVisibilitySettings();

    }

}




