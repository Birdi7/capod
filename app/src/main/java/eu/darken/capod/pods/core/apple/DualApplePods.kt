package eu.darken.capod.pods.core.apple

import android.content.Context
import android.util.Range
import androidx.annotation.StringRes
import eu.darken.capod.R
import eu.darken.capod.common.debug.logging.log
import eu.darken.capod.common.isBitSet
import eu.darken.capod.common.lowerNibble
import eu.darken.capod.common.upperNibble
import eu.darken.capod.pods.core.HasCase
import eu.darken.capod.pods.core.HasDualPods
import eu.darken.capod.pods.core.HasDualPods.Pod
import eu.darken.capod.pods.core.HasEarDetection

interface DualApplePods : ApplePods, HasDualPods, HasEarDetection, HasCase {

    val microPhonePod: Pod
        get() = when (rawStatus.isBitSet(5)) {
            true -> Pod.LEFT
            false -> Pod.RIGHT
        }

    override val batteryLeftPodPercent: Float?
        get() {
            val value = when (microPhonePod) {
                Pod.LEFT -> rawPodsBattery.lowerNibble.toInt()
                Pod.RIGHT -> rawPodsBattery.upperNibble.toInt()
            }
            return when (value) {
                15 -> null
                else -> if (value > 10) {
                    log { "Left pod: Above 100% battery: $value" }
                    1.0f
                } else {
                    (value / 10f)
                }
            }
        }

    override val batteryRightPodPercent: Float?
        get() {
            val value = when (microPhonePod) {
                Pod.LEFT -> rawPodsBattery.upperNibble.toInt()
                Pod.RIGHT -> rawPodsBattery.lowerNibble.toInt()
            }
            return when (value) {
                15 -> null
                else -> if (value > 10) {
                    log { "Right pod: Above 100% battery: $value" }
                    1.0f
                } else {
                    value / 10f
                }
            }
        }

    val isLeftPodInEar: Boolean
        get() = when (microPhonePod) {
            Pod.LEFT -> rawStatus.isBitSet(1)
            Pod.RIGHT -> rawStatus.isBitSet(3)
        }

    val isRightPodInEar: Boolean
        get() = when (microPhonePod) {
            Pod.LEFT -> rawStatus.isBitSet(3)
            Pod.RIGHT -> rawStatus.isBitSet(1)
        }

    override val isBeingWorn: Boolean
        get() = isLeftPodInEar && isRightPodInEar

    val isLeftPodCharging: Boolean
        get() = when (microPhonePod) {
            Pod.LEFT -> rawCaseBattery.upperNibble.isBitSet(0)
            Pod.RIGHT -> rawCaseBattery.upperNibble.isBitSet(1)
        }

    val isRightPodCharging: Boolean
        get() = when (microPhonePod) {
            Pod.LEFT -> rawCaseBattery.upperNibble.isBitSet(1)
            Pod.RIGHT -> rawCaseBattery.upperNibble.isBitSet(0)
        }

    override val batteryCasePercent: Float?
        get() = when (val value = rawCaseBattery.lowerNibble.toInt()) {
            15 -> null
            else -> if (value > 10) {
                log { "Case: Above 100% battery: $value" }
                1.0f
            } else {
                value / 10f
            }
        }

    override val isCaseCharging: Boolean
        get() = rawCaseBattery.upperNibble.isBitSet(2)

    val caseLidState: LidState
        get() {
            return LidState.values().firstOrNull { it.rawRange.contains(rawCaseLidState) } ?: LidState.UNKNOWN
        }

    /**
     * TODO this is glitchy
     * The counters generally increase if quickly and repeatedly:
     * - open/close
     * - add/remove the last airpod to the case
     * They reset after some time to their start values.
     * The upper limits are not the maximums but are only reached if playing with the case.
     */
    enum class LidState(val rawRange: Range<UByte>) {
        OPEN(0x30, 0x37),
        CLOSED(0x38, 0x3F),
        NOT_IN_CASE(0x00, 0x03),
        UNKNOWN(0xFF, 0xFF);

        constructor(vararg raw: Int) : this(Range(raw[0].toUByte(), (raw[1].toUByte())))
    }

    val deviceColor: DeviceColor
        get() = DeviceColor.values().firstOrNull { it.raw == rawDeviceColor } ?: DeviceColor.UNKNOWN


    fun getDeviceColorLabel(context: Context): String = context.getString(deviceColor.labelRes)

    enum class DeviceColor(val raw: UByte?, @StringRes val labelRes: Int) {

        WHITE(0x00, R.string.pods_device_color_white_label),
        BLACK(0x01, R.string.pods_device_color_black_label),
        RED(0x02, R.string.pods_device_color_red_label),
        BLUE(0x03, R.string.pods_device_color_blue_label),
        PINK(0x04, R.string.pods_device_color_pink_label),
        GRAY(0x05, R.string.pods_device_color_gray_label),
        SILVER(0x06, R.string.pods_device_color_silver_label),
        GOLD(0x07, R.string.pods_device_color_gold_label),
        ROSE_GOLD(0x08, R.string.pods_device_color_rose_gold_label),
        SPACE_GRAY(0x09, R.string.pods_device_color_space_gray_label),
        DARK_BLUE(0x0a, R.string.pods_device_color_dark_blue_label),
        LIGHT_BLUE(0x0b, R.string.pods_device_color_light_blue_label),
        YELLOW(0x0c, R.string.pods_device_color_yellow_label),
        UNKNOWN(null, R.string.general_value_unknown_label);

        constructor(raw: Int, @StringRes labelRes: Int) : this(raw.toUByte(), labelRes)
    }

    val connectionState: ConnectionState
        get() = ConnectionState.values().firstOrNull { rawSuffix == it.raw } ?: ConnectionState.UNKNOWN

    fun getConnectionStateLabel(context: Context): String = context.getString(connectionState.labelRes)

    enum class ConnectionState(val raw: UByte?, @StringRes val labelRes: Int) {
        DISCONNECTED(0x00, R.string.pods_connection_state_disconnected_label),
        IDLE(0x04, R.string.pods_connection_state_idle_label),
        MUSIC(0x05, R.string.pods_connection_state_music_label),
        CALL(0x06, R.string.pods_connection_state_call_label),
        RINGING(0x07, R.string.pods_connection_state_ringing_label),
        HANGING_UP(0x09, R.string.pods_connection_state_hanging_up_label),
        UNKNOWN(null, R.string.general_value_unknown_label);

        constructor(raw: Int, @StringRes labelRes: Int) : this(raw.toUByte(), labelRes)
    }
}