package org.kamiblue.capeapi

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

val cachedNames = hashMapOf<String, String>() // UUID, name

fun cachedName(uuid: String?): String? {
    if (uuid == null) return null
    if (!uuid.isUUID()) return uuid

    return cachedNames[uuid] ?: run {
        val user = getFromUUID(uuid) ?: return@run null
        cachedNames[user.uuid] = user.currentMojangName.name

        user.currentMojangName.name
    }
}

fun getFromUUID(uuid: String): User? {
    if (!uuid.isUUID()) return null

    val names = getNamesFromUUID(uuid) ?: return null

    cachedNames[uuid] = names.last().name
    return User(uuid, names)
}

fun getFromName(name: String): User? {
    if (name.isUUID()) return getFromUUID(name)

    val profile = getProfileFromName(name) ?: return null
    val names = getNamesFromUUID(profile.uuid) ?: return null

    cachedNames[profile.uuid] = profile.name
    return User(profile.uuid, names)
}

fun getProfileFromName(name: String): MojangProfile? {
    val foundProfile = getProfileFromName0(name) ?: return null
    return MojangProfile(foundProfile.name, foundProfile.uuidWithoutDash) // used because Gson doesn't serialize uuidWithoutDash.org.kamiblue.capeapi.insertDashes()
}

fun getNamesFromUUID(uuid: String): List<MojangName>? {
    if (uuid.fixedUUID() == null) return null

    val url = "https://api.mojang.com/user/profiles/$uuid/names".replace("-", "")
    val response = getRequest(url)

    if (response?.isEmpty() != false) return null // uuid doesn't exist

    // this will just return null if it somehow fails, no exception thrown
    return Gson().fromJson(response, object : TypeToken<List<MojangName>>() {}.type)
}

private fun getProfileFromName0(name: String): MojangProfile? {
    if (name.isUUID()) return null

    val url = "https://api.mojang.com/users/profiles/minecraft/$name"
    val response = getRequest(url)

    if (response?.isEmpty() != false) return null // name doesn't exist

    return Gson().fromJson(response, MojangProfile::class.java)
}

fun getRequest(url: String): String? {
    return try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        connection.doOutput = true
        connection.doInput = true
        connection.requestMethod = "GET"

        // read the response
        val inputStream: InputStream = BufferedInputStream(connection.inputStream)
        val response = convertStreamToString(inputStream)
        inputStream.close()
        connection.disconnect()
        response
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun convertStreamToString(inputStream: InputStream): String {
    val scanner = Scanner(inputStream).useDelimiter("\\A")
    return if (scanner.hasNext()) scanner.next() else "/"
}

fun String.insertDashes() = StringBuilder(this)
    .insert(8, '-')
    .insert(13, '-')
    .insert(18, '-')
    .insert(23, '-')
    .toString()

fun String.isUUID() = Regex("[a-z0-9].{7}-[a-z0-9].{3}-[a-z0-9].{3}-[a-z0-9].{3}-[a-z0-9].{11}").matches(this)

/** @return a properly formatted UUID, null if can't be formatted */
fun String.fixedUUID(): String? {
    if (this.isUUID()) return this
    if (length < 32) return null
    val fixed = this.insertDashes()
    if (fixed.isUUID()) return fixed
    return null
}
