package com.yusufteker.healthtrack.core.utils

/**
 * Parses a numeric string entered by the user, normalizing Turkish/European
 * comma decimal separators (',') to '.' before parsing as a [Double].
 */
fun String.toLocaleDoubleOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()
