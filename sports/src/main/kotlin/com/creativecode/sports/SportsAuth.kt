package com.creativecode.sports

import org.springframework.jdbc.core.RowMapper
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.Timestamp
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class LoginInput(val tenantId: UUID, val username: String, val password: String)
data class UserInput(val username: String, val name: String, val password: String, val role: Role = Role.EMPLOYEE)
data class UserView(val id: UUID, val username: String, val name: String, val role: Role, val active: Boolean)
class SportsAuth(private val store: PostgresSportsStore) {
    private val passwords = BCryptPasswordEncoder(12)
    private val dummyHash = passwords.encode(UUID.randomUUID().toString())
    private val random = SecureRandom()
    private val failures = ConcurrentHashMap<String, Pair<Long, Int>>()
    private val actorMapper = RowMapper { r: java.sql.ResultSet, _: Int -> Actor(UUID.fromString(r.getString("id")), UUID.fromString(r.getString("tenant_id")), r.getString("name"), Role.valueOf(r.getString("role"))) }
    @Synchronized fun login(input: LoginInput, remote: String): Map<String, Any> {
        checkSports(input.username.length in 1..100 && input.password.length in 1..200, "INVALID_LOGIN", "Credenciales inválidas", 401)
        val now = Instant.now(); val window = now.epochSecond / 300
        if (failures.size > 10000) failures.entries.removeIf { it.value.first != window }
        checkSports(failures.size < 20000, "RATE_LIMIT", "Intenta más tarde", 429)
        val key = hash(remote)
        val previous = failures[key]
        val attempts = if (previous?.first == window) previous.second else 0
        checkSports(attempts < 10, "RATE_LIMIT", "Demasiados intentos; espera cinco minutos", 429)
        failures[key] = Pair(window, attempts + 1)
        val users = store.jdbc.queryForList("SELECT id,password_hash,active FROM sports.users WHERE tenant_id=? AND username=?",input.tenantId,input.username.trim().toLowerCase(java.util.Locale.ROOT))
        val user = users.firstOrNull()
        val matches = passwords.matches(input.password, user?.get("password_hash") as? String ?: dummyHash)
        checkSports(matches && user?.get("active") == true, "INVALID_LOGIN", "Credenciales inválidas", 401)
        val bytes = ByteArray(32); random.nextBytes(bytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val expiry = now.plusSeconds(12 * 3600)
        store.jdbc.update("INSERT INTO sports.sessions(token_hash,tenant_id,user_id,expires_at) VALUES (?,?,?,?)", hash(token),input.tenantId,user!!["id"],Timestamp.from(expiry))
        return mapOf("token" to token, "expiresAt" to expiry, "actor" to authenticate("Bearer $token"))
    }
    fun authenticate(header: String?): Actor {
        checkSports(header != null && header.startsWith("Bearer ") && header.length < 200, "UNAUTHORIZED", "Inicia sesión", 401)
        return store.jdbc.query("""SELECT u.id,u.tenant_id,u.name,u.role FROM sports.sessions s JOIN sports.users u ON u.tenant_id=s.tenant_id AND u.id=s.user_id
            WHERE s.token_hash=? AND s.expires_at>now() AND u.active=true""", actorMapper, hash(header!!.substring(7))).firstOrNull() ?: throw SportsError("UNAUTHORIZED", "La sesión expiró; inicia sesión", 401)
    }
    fun logout(header: String) { store.jdbc.update("UPDATE sports.sessions SET expires_at=now() WHERE token_hash=?",hash(header.removePrefix("Bearer "))) }
    fun users(a: Actor): List<UserView> {
        a.admin()
        return store.jdbc.query("SELECT id,username,name,role,active FROM sports.users WHERE tenant_id=? ORDER BY name",RowMapper { r, _ -> UserView(UUID.fromString(r.getString("id")),r.getString("username"),r.getString("name"),Role.valueOf(r.getString("role")),r.getBoolean("active")) },a.tenantId)
    }
    fun create(a: Actor, u: UserInput): UUID = store.transaction {
        a.admin()
        checkSports(u.username.matches(Regex("[a-zA-Z0-9._-]{3,100}")) && u.name.isNotBlank() && u.name.length <= 150 && u.password.length >= 12 && u.password.toByteArray(Charsets.UTF_8).size <= 72,"INVALID_USER","Usuario inválido; contraseña mínimo 12 caracteres y máximo 72 bytes")
        val id=UUID.randomUUID()
        store.jdbc.update("INSERT INTO sports.users(tenant_id,id,username,name,password_hash,role) VALUES (?,?,?,?,?,?)",a.tenantId,id,u.username.toLowerCase(java.util.Locale.ROOT),u.name,passwords.encode(u.password),u.role.name)
        store.audit(a,id,"USER_CREATED",null); id
    }
    fun deactivate(a: Actor, id: UUID) = store.transaction {
        a.admin(); checkSports(id != a.id,"SELF_DEACTIVATION","No puedes desactivar tu propia cuenta")
        checkSports(store.jdbc.update("UPDATE sports.users SET active=false WHERE tenant_id=? AND id=?",a.tenantId,id)==1,"NOT_FOUND","Usuario no encontrado",404)
        store.audit(a,id,"USER_DEACTIVATED",null)
    }
    private fun hash(s: String) = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
