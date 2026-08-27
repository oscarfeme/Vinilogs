package app.vinilogs.feature.collection.addedit

import app.vinilogs.core.model.Condition
import app.vinilogs.core.model.Format
import app.vinilogs.core.model.Speed

/**
 * Chip labels for the manual-entry form's format/speed/condition rows. Split out of
 * AddEditRecordForm.kt to stay under detekt's per-file function threshold.
 */
internal fun Format.label(): String =
    when (this) {
        Format.LP -> "LP"
        Format.EP -> "EP"
        Format.SEVEN -> "7\""
        Format.TEN -> "10\""
        Format.TWELVE -> "12\""
        Format.BOX -> "Box"
    }

internal fun Speed.rpmLabel(): String =
    when (this) {
        Speed.RPM33 -> "33"
        Speed.RPM45 -> "45"
        Speed.RPM78 -> "78"
    }

internal fun Condition.abbreviation(): String =
    when (this) {
        Condition.MINT -> "M"
        Condition.NEAR_MINT -> "NM"
        Condition.VERY_GOOD_PLUS -> "VG+"
        Condition.VERY_GOOD -> "VG"
        Condition.GOOD -> "G"
        Condition.FAIR -> "F"
        Condition.POOR -> "P"
    }
