# Withings ScanWatch (Gadgetbridge add-on)

Self-contained sources plus a small **integration patch** for [Gadgetbridge](https://codeberg.org/Freeyourgadget/Gadgetbridge). This repository does **not** contain Gadgetbridge; you clone upstream separately and merge this add-on into that checkout.

## Layout

| Path | Purpose |
|------|--------|
| `module/src/main/java/...` | Coordinator, BLE support, and `WithingsScanWatchGatt` UUID constants (Steel-HR family). |
| `integration/gadgetbridge-integration.patch` | Minimal edits to upstream `DeviceType` and `strings`. |
| `integration/apply-integration.sh` | Copies Java sources into upstream and applies the patch. |
| `UPSTREAM_REVISION` | Commit the patch was last validated against (regenerate patch if you rebase). |

Gadgetbridge has no dynamic plugin API; the coordinator and support classes must compile inside the main `app` module. The script **copies** (with `rsync` without `--delete`) the add-on Java tree into `app/src/main/java` and applies a tiny diff for registration. Never use `rsync --delete` here: it would wipe the rest of Gadgetbridge’s sources.

## Prerequisites

- A clean or working [Gadgetbridge](https://codeberg.org/Freeyourgadget/Gadgetbridge.git) clone (any path).
- `bash`, `rsync`, `patch` or `git apply`.

## Apply

```bash
export GADGETBRIDGE_ROOT=/path/to/Gadgetbridge   # optional if you pass it as $1
./integration/apply-integration.sh "$GADGETBRIDGE_ROOT"
```

Then build Gadgetbridge as usual, for example:

```bash
cd "$GADGETBRIDGE_ROOT"
./gradlew :app:assembleDebug
```

## Remove (from a Gadgetbridge tree)

```bash
cd "$GADGETBRIDGE_ROOT"
git restore app/src/main/java/nodomain/freeyourgadget/gadgetbridge/model/DeviceType.java \
  app/src/main/res/values/strings.xml
rm -rf app/src/main/java/nodomain/freeyourgadget/gadgetbridge/devices/withingsscanwatch
rm -rf app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/devices/withingsscanwatch
```

Re-running `./integration/apply-integration.sh` on an **already integrated** tree will usually fail at `git apply` (patch already applied). Remove using the steps above first, or `git checkout` those two files before re-applying.

## Reverse engineering (no special build)

With logging enabled in Gadgetbridge, connection attempts will show GATT services/characteristics for the UUIDs registered in `WithingsScanWatchDeviceSupport` (Generic Access/Attribute, Device Information, Battery, and the Steel-HR-family Withings service `00000020-5749-5448-…`). Unknown notifications are logged with a hex dump. Compare with upstream `WithingsSteelHRDeviceSupport` / `WithingsUUID` in [Gadgetbridge](https://codeberg.org/Freeyourgadget/Gadgetbridge).

Optional: Android **Bluetooth HCI snoop** while using the vendor app, then inspect with Wireshark.

## Publishing on GitHub

Initialize git only inside this add-on directory (or move these files to a new repo root). Do not commit the Gadgetbridge tree. When upstream moves forward, refresh `integration/gadgetbridge-integration.patch` if `DeviceType.java` or nearby context conflicts, and update `UPSTREAM_REVISION`.

## License

Add-on source files are under the GNU Affero General Public License v3 or later, consistent with Gadgetbridge.
