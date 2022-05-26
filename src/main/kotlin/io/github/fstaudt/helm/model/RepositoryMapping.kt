package io.github.fstaudt.helm.model

/**
 * Mapping to an external JSON schema repository for validation of values in charts of a Helm repository
 * @param basePath base path to the repository
 *
 *                 must include protocol and host, may include port and path and must not end with "/"
 *
 *                 examples: http://my.charts.repository:1080, https://my.charts.repository/apps
 * @param authorizationHeader optional authorization header for the repository
 *
 *                            examples: Basic dGVzdDp0ZXN0, Bearer xxx.xxxxxx.xxx
 */
data class RepositoryMapping(val basePath: String, val authorizationHeader: String? = null)