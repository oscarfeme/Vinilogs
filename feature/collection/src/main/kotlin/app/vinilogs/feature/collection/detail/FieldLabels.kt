package app.vinilogs.feature.collection.detail

import app.vinilogs.core.model.Condition
import app.vinilogs.core.model.Format
import app.vinilogs.core.model.Speed

/**
 * Display labels for the enum fields on [app.vinilogs.core.model.Record]. Duplicated from
 * T-17's copy in `feature/collection/addedit` -- independent branches off the same base can't
 * share a commit. See that task's PR notes for the same duplication.
 */
internal fun Format.displayLabel(): String =
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
        Speed.RPM33 -> "33 RPM"
        Speed.RPM45 -> "45 RPM"
        Speed.RPM78 -> "78 RPM"
    }

/** Glossary abbreviation (00-README.md): M/NM/VG+/VG/G/F/P, best to worst. */
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
