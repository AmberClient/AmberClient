package com.amberclient.modules.minigames.murdererfinder

import java.util.regex.Pattern

class Version(version: String) : Comparable<Version> {

    companion object {
        private val SYNTAX = Pattern.compile("^[0-9]+\\.[0-9]+(?:\\.[0-9]+)?(?:pre[0-9]+)?$")
    }

    private var major: Int
    private var minor: Int
    private var patch: Int
    private var preRelease: Int

    init {
        if (!SYNTAX.asPredicate().test(version)) {
            major = -1
            minor = -1
            patch = -1
            preRelease = Int.MAX_VALUE
        }

        val indexOfPre = version.indexOf("pre")

        val parts: Array<String>
        if (indexOfPre == -1) {
            preRelease = Int.MAX_VALUE
            parts = version.split("\\.".toRegex()).toTypedArray()
        } else {
            preRelease = version.substring(indexOfPre + 3).toInt()
            parts = version.substring(0, indexOfPre).split("\\.".toRegex()).toTypedArray()
        }

        major = parts[0].toInt()
        minor = parts[1].toInt()
        patch = if (parts.size == 3) parts[2].toInt() else 0
    }

    override fun hashCode(): Int {
        return major shl 24 or (minor shl 16) or (patch shl 8) or preRelease
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other) || (other is Version && compareTo(other) == 0)
    }

    override fun compareTo(other: Version): Int {
        if (major != other.major)
            return major.compareTo(other.major)

        if (minor != other.minor)
            return minor.compareTo(other.minor)

        if (patch != other.patch)
            return patch.compareTo(other.patch)

        if (preRelease != other.preRelease)
            return preRelease.compareTo(other.preRelease)

        return 0
    }

    fun shouldUpdateTo(other: Version): Boolean {
        return isInvalid() || other.isInvalid() || isLowerThan(other)
    }

    fun isLowerThan(other: Version): Boolean {
        return compareTo(other) < 0
    }

    fun isLowerThan(other: String): Boolean {
        return isLowerThan(Version(other))
    }

    fun isHigherThan(other: Version): Boolean {
        return compareTo(other) > 0
    }

    fun isHigherThan(other: String): Boolean {
        return isHigherThan(Version(other))
    }

    override fun toString(): String {
        if (isInvalid())
            return "(invalid version)"

        var s = "$major.$minor"

        if (patch > 0)
            s += ".$patch"

        if (isPreRelease())
            s += "pre$preRelease"

        return s
    }

    fun isInvalid(): Boolean {
        return major == -1 && minor == -1 && patch == -1
    }

    fun isPreRelease(): Boolean {
        return preRelease != Int.MAX_VALUE
    }
}