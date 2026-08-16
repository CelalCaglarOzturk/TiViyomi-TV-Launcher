package dev.mudrock.tiviyomitvlauncher.ui.util

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * A wrapper to make standard Maps stable in the eyes of the Compose compiler.
 * This prevents parent recompositions from forcing child recompositions when
 * the map instance is technically new but contents are unchanged.
 */
@Immutable
data class StableMap<K, V>(val map: Map<K, V> = emptyMap()) : Map<K, V> by map

/**
 * A wrapper to make standard Lists stable.
 */
@Immutable
data class StableList<T>(val list: List<T> = emptyList()) : List<T> by list

/**
 * A wrapper to make standard Sets stable.
 */
@Immutable
data class StableSet<T>(val set: Set<T> = emptySet()) : Set<T> by set

fun <K, V> Map<K, V>.asStable(): StableMap<K, V> = StableMap(this)
fun <T> List<T>.asStable(): StableList<T> = StableList(this)
fun <T> Set<T>.asStable(): StableSet<T> = StableSet(this)

@Stable
class StableRef<T>(var value: T)
