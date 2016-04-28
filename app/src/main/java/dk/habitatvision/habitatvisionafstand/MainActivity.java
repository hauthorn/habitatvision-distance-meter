package dk.habitatvision.habitatvisionafstand;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.kontakt.sdk.android.ble.configuration.ActivityCheckConfiguration;
import com.kontakt.sdk.android.ble.configuration.ForceScanConfiguration;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.configuration.scan.IBeaconScanContext;
import com.kontakt.sdk.android.ble.configuration.scan.ScanContext;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.device.BeaconRegion;
import com.kontakt.sdk.android.ble.discovery.BluetoothDeviceEvent;
import com.kontakt.sdk.android.ble.discovery.EventType;
import com.kontakt.sdk.android.ble.filter.ibeacon.IBeaconFilters;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerContract;
import com.kontakt.sdk.android.ble.rssi.RssiCalculators;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.RemoteBluetoothDevice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements ProximityManager.ProximityListener {
    private ProximityManagerContract proximityManager;
    private ScanContext scanContext;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        proximityManager = new ProximityManager(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Turn on bluetooth
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }

        // Start scanning for beacons when we have connected
        proximityManager.initializeScan(getScanContext(), new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                proximityManager.attachListener(MainActivity.this);
            }
            @Override
            public void onConnectionFailure() {
                Log.e(TAG, "Failed to connect to proximitymanager");
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        proximityManager.detachListener(this); // We must detach to avoid memory leaks
        proximityManager.disconnect();
    }

    private ScanContext getScanContext() {
        if (scanContext != null) return scanContext;

        // Beacons to care about
        BeaconRegion region = new BeaconRegion.Builder()
                .setProximity(UUID.fromString("f7826da6-4fa2-4e98-8024-bc5b71e0893e"))
                .setMajor(47445)
                .setMinor(8289)
                .build();
        Collection<IBeaconRegion> regions = new ArrayList<>();
        regions.add(region);

        scanContext = new ScanContext.Builder()
                .setScanPeriod(new ScanPeriod(TimeUnit.SECONDS.toMillis(3), TimeUnit.SECONDS.toMillis(2)))
                .setScanMode(ProximityManager.SCAN_MODE_BALANCED)
                .setActivityCheckConfiguration(ActivityCheckConfiguration.MINIMAL)
                .setForceScanConfiguration(ForceScanConfiguration.MINIMAL)
                .setIBeaconScanContext(new IBeaconScanContext.Builder()
                        .setRssiCalculator(RssiCalculators.newLimitedMeanRssiCalculator(3)) // Even out measurements
                        .setEventTypes(Arrays.asList( // What events are we interested in
                                EventType.DEVICE_DISCOVERED,
                                EventType.DEVICES_UPDATE,
                                EventType.DEVICE_LOST
                        ))
                        .setIBeaconRegions(regions)
                        .build())
                .setForceScanConfiguration(ForceScanConfiguration.MINIMAL)
                .build();

        return scanContext;
    }

    // Callbacks from the beacon scanning
    @Override
    public void onScanStart() {
        Log.d(TAG, "scan started");
    }

    @Override
    public void onScanStop() {
        Log.d(TAG, "scan stopped");
    }

    @Override
    public void onEvent(BluetoothDeviceEvent event) {
        // This is where the magic happens, and we get updates from bluetooth
        List<? extends RemoteBluetoothDevice> devices = event.getDeviceList();
        IBeaconDevice device = null;
        if (devices.size() > 0) {
            device = (IBeaconDevice) devices.get(0);
        }
        
        switch (event.getEventType()) {
            case DEVICE_DISCOVERED:
                Log.d(TAG, "found new beacon, major-minor: " + device.getMajor() + " " + device.getMinor());
                Log.d(TAG, "Distance-Rssi" + device.getDistance() + " " + device.getRssi());
                break;
            case DEVICES_UPDATE:
                Log.d(TAG, "updated beacon, major-minor: " + device.getMajor() + " " + device.getMinor());
                Log.d(TAG, "Distance-Rssi" + device.getDistance() + " " + device.getRssi());
                break;
            case DEVICE_LOST:
                Log.d(TAG, "lost beacon, major-minor: " + device.getMajor() + " " + device.getMinor());
                break;
        }
    }

    private static class NearbyBeacons {

    }
}
