/*  SPDX-License-Identifier: AGPL-3.0-or-later
    Withings ScanWatch add-on for Gadgetbridge (see companion README). */
package nodomain.freeyourgadget.gadgetbridge.devices.withingsscanwatch;

import androidx.annotation.NonNull;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingsscanwatch.WithingsScanWatchDeviceSupport;

/**
 * Experimental ScanWatch support scaffold. Refine {@link #supports(GBDeviceCandidate)} once BLE
 * advertisement / naming is confirmed for your hardware revision.
 */
public class WithingsScanWatchDeviceCoordinator extends AbstractBLEDeviceCoordinator {

    /** BLE local names are short; longer values are not a ScanWatch advertisement. */
    private static final int MAX_ADVERTISED_NAME_CHARS = 40;

    /**
     * Match ScanWatch names only. The previous {@code withings && scan} rule also selected
     * Withings Body Scan, which is a scale. Names are attacker-controlled advertisements, so
     * control characters and oversized names are rejected before they reach the device list.
     */
    @Override
    public boolean supports(final GBDeviceCandidate candidate) {
        return isScanWatchName(candidate.getName());
    }

    static boolean isScanWatchName(final String name) {
        if (name == null || name.length() > MAX_ADVERTISED_NAME_CHARS) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (c < 0x20 || c > 0x7e) {
                return false;
            }
        }
        final String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("body")) {
            return false;
        }
        return lower.contains("scanwatch") || lower.contains("scan watch");
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_BOND;
    }

    @Override
    public boolean isExperimental() {
        return true;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return WithingsScanWatchDeviceSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_withings_scan_watch;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_watchxplus;
    }

    @Override
    public String getManufacturer() {
        return "Withings";
    }

    @NonNull
    @Override
    public DeviceKind getDeviceKind(@NonNull final GBDevice device) {
        return DeviceKind.WATCH;
    }
}
