/*  SPDX-License-Identifier: AGPL-3.0-or-later
    Withings ScanWatch add-on for Gadgetbridge (see companion README). */
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingsscanwatch;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.Nullable;

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
 * BLE scaffold aimed at reverse engineering: registers standard services plus the Withings UUIDs
 * used on Steel HR so characteristics appear in Gadgetbridge logs when the watch exposes them.
 */
public class WithingsScanWatchDeviceSupport extends AbstractBTLESingleDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(WithingsScanWatchDeviceSupport.class);

    /** Device Information strings are short ASCII; anything longer is not stored or logged. */
    private static final int MAX_BLE_TEXT_BYTES = 64;
    private static final int MAX_BLE_TEXT_CHARS = 32;

    public WithingsScanWatchDeviceSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(WithingsScanWatchGatt.WITHINGS_SERVICE);
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZING);
        getDevice().setFirmwareVersion("n/a");
        builder.read(GattCharacteristic.UUID_CHARACTERISTIC_DEVICE_NAME);
        if (getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING) != null) {
            builder.read(GattCharacteristic.UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING);
        }
        if (getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL) != null) {
            builder.read(GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL);
        }
        final BluetoothGattCharacteristic withingsWrite =
                getCharacteristic(WithingsScanWatchGatt.WITHINGS_WRITE);
        if (withingsWrite != null
                && (withingsWrite.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            builder.notify(WithingsScanWatchGatt.WITHINGS_WRITE, true);
        }
        builder.requestMtu(512);
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
            if (status == BluetoothGatt.GATT_SUCCESS) {
                final String gapName = sanitizeBleText(value);
                if (gapName != null) {
                    LOG.info("ScanWatch GAP name: {}", gapName);
                } else {
                    LOG.warn("ScanWatch GAP name rejected (len={})", value == null ? 0 : value.length);
                }
            }
            return true;
        }
        if (GattCharacteristic.UUID_CHARACTERISTIC_FIRMWARE_REVISION_STRING.equals(uuid)) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                final String revision = sanitizeBleText(value);
                if (revision != null) {
                    getDevice().setFirmwareVersion(revision);
                    LOG.info("ScanWatch firmware revision (DIS): {}", revision);
                } else {
                    LOG.warn("ScanWatch firmware revision rejected (len={})", value == null ? 0 : value.length);
                }
            }
            return true;
        }
        if (GattCharacteristic.UUID_CHARACTERISTIC_BATTERY_LEVEL.equals(uuid)) {
            if (status == BluetoothGatt.GATT_SUCCESS && value != null && value.length == 1) {
                final int level = value[0] & 0xff;
                if (level <= 100) {
                    getDevice().setBatteryLevel(level);
                    getDevice().setBatteryVoltage(GBDevice.BATTERY_UNKNOWN);
                    LOG.info("ScanWatch battery level (BAS): {}%", level);
                } else {
                    LOG.warn("ScanWatch battery level rejected: {}", level);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onCharacteristicChanged(
            final BluetoothGatt gatt,
            final BluetoothGattCharacteristic characteristic,
            final byte[] value
    ) {
        if (super.onCharacteristicChanged(gatt, characteristic, value)) {
            return true;
        }
        final int length = value == null ? 0 : value.length;
        LOG.debug("ScanWatch notify {} ({} bytes)", characteristic.getUuid(), length);
        return true;
    }

    /**
     * Keep only short printable ASCII. Firmware and GAP strings are persisted and written to
     * logs; a spoofed peripheral can otherwise inject newlines or a very large value.
     */
    @Nullable
    static String sanitizeBleText(final byte[] value) {
        if (value == null || value.length == 0 || value.length > MAX_BLE_TEXT_BYTES) {
            return null;
        }
        final String raw = new String(value, StandardCharsets.UTF_8).trim();
        if (raw.isEmpty() || raw.length() > MAX_BLE_TEXT_CHARS) {
            return null;
        }
        for (int i = 0; i < raw.length(); i++) {
            final char c = raw.charAt(i);
            if (c < 0x20 || c > 0x7e) {
                return null;
            }
        }
        return raw;
    }
}
