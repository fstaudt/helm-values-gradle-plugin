package io.github.fstaudt.helm.exceptions

class RepositoryMappingException(publicationRepository: String?) : RuntimeException(
    """
        Publication repository $publicationRepository not found in repository mappings.
        Please update publicationRepository or correct configuration of helmValues in gradle build.
    """.trimIndent()
)
