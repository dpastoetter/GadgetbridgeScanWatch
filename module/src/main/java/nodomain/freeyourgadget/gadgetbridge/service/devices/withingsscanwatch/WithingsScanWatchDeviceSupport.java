/*  SPDX-License-Identifier: AGPL-3.0-or-later
    Withings ScanWatch add-on for Gadgetbridge (see companion README). */
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingsscanwatch;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;

/**
 * Minimal BLE scaffold: discovers Generic Access and reads the GAP device name. Replace with real
 * Withings ScanWatch protocol handling as you implement it.
 */
public class WithingsScanWatchDeviceSupport extends AbstractBTLESingleDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(WithingsScanWatchDeviceSupport.class);

    public WithingsScanWatchDeviceSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZING);
        getDevice().setFirmwareVersion("n/a");
        builder.read(GattCharacteristic.UUID_CHARACTERISTIC_DEVICE_NAME);
        builder.setDeviceState(GBDevice.State.INITIALIZED);
        return builder;
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public boolean onCharacteristicRead(
            final BluetoothGatt gatt,
            final BluetoothGattCharacteristic characteristic,
            final byte[] value,
            final int status
    ) {
        if (super.onCharacteristicRead(gatt, characteristic, value, status)) {
            return true;
        }
        final UUID uuid = characteristic.getUuid();
        if (GattCharacteristic.UUID_CHARACTERISTIC_DEVICE_NAME.equals(uuid)) {
            if (status == BluetoothGatt.GATT_SUCCESS && value != null) {
                LOG.info("Withings ScanWatch GAP name: {}", new String(value, StandardCharsets.UTF_8));
            }
            return true;
        }
        return false;
    }
}
