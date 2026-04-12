/*  SPDX-License-Identifier: AGPL-3.0-or-later
    Withings ScanWatch add-on for Gadgetbridge (see companion README).

    Values mirror Gadgetbridge’s Withings Steel HR stack (WithingsUUID) for interoperability
    research. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingsscanwatch;

import java.util.UUID;

public final class WithingsScanWatchGatt {

    public static final UUID WITHINGS_SERVICE = UUID.fromString("00000020-5749-5448-0037-000000000000");
    public static final UUID WITHINGS_WRITE = UUID.fromString("00000024-5749-5448-0037-000000000000");
    /** Often used for app channel traffic on Steel HR (characteristic, not a service). */
    public static final UUID WITHINGS_APP = UUID.fromString("10000059-5749-5448-0037-000000000000");
    public static final UUID WITHINGS_APP2 = UUID.fromString("10000028-5749-5448-0037-000000000000");

    private WithingsScanWatchGatt() {
    }
}
