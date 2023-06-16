package no.fdk.concept_catalog.security

import no.fdk.concept_catalog.model.User
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

private const val ROLE_ROOT_ADMIN = "system:root:admin"
private fun roleOrgAdmin(orgnr: String) = "organization:$orgnr:admin"
private fun roleOrgWrite(orgnr: String) = "organization:$orgnr:write"
private fun roleOrgRead(orgnr: String) = "organization:$orgnr:read"

@Component
class EndpointPermissions {

    fun getOrgsByPermission(jwt: Jwt, permission: String): Set<String> {
        val authorities: String? = jwt.claims["authorities"] as? String
        val regex = when(permission){
            "read" -> Regex("""[0-9]{9}""")
            else -> Regex("""[0-9]{9}:$permission""")
        }

        return authorities
            ?.let { regex.findAll(it)}
            ?.map { matchResult -> matchResult.value
                .replace(Regex("[A-Za-z:]"), "")}
            ?.toSet()
            ?: emptySet()
    }

    fun hasOrgReadPermission(jwt: Jwt, orgnr: String?): Boolean {
        val authorities: String? = jwt.claims["authorities"] as? String
        return when {
            orgnr == null -> false
            authorities == null -> false
            hasSysAdminPermission(jwt) -> true
            authorities.contains(roleOrgAdmin(orgnr)) -> true
            authorities.contains(roleOrgWrite(orgnr)) -> true
            authorities.contains(roleOrgRead(orgnr)) -> true
            else -> false
        }
    }

    fun hasOrgWritePermission(jwt: Jwt, orgnr: String?): Boolean {
        val authorities: String? = jwt.claims["authorities"] as? String
        return when {
            orgnr == null -> false
            authorities == null -> false
            hasSysAdminPermission(jwt) -> true
            authorities.contains(roleOrgAdmin(orgnr)) -> true
            authorities.contains(roleOrgWrite(orgnr)) -> true
            else -> false
        }
    }

    fun hasSysAdminPermission(jwt: Jwt): Boolean {
        val authorities: String? = jwt.claims["authorities"] as? String

        return authorities?.contains(ROLE_ROOT_ADMIN) ?: false
    }

    fun getUser(jwt: Jwt): User? =
        jwt.let { it.claims["user_name"] as? String }
            ?.let { id ->
                User(
                    id=id,
                    email=jwt.claims["email"] as? String,
                    name=jwt.claims["name"] as? String
                )
            }

}
