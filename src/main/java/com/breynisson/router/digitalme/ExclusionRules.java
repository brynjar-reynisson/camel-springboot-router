package com.breynisson.router.digitalme;

public final class ExclusionRules {

    private ExclusionRules() {
    }

    private static boolean endsWithDomain(String sourceUrl, String domain) {
        return sourceUrl.endsWith(domain) || sourceUrl.endsWith(domain + "/");
    }

    public static boolean isExcluded(String sourceUrl) {
        return sourceUrl == null
                || sourceUrl.contains("localhost:3001")
                || sourceUrl.contains("localhost:8080")
                || sourceUrl.contains("//google.")
                || sourceUrl.contains(".google.")
                || sourceUrl.contains("islandsbanki")
                || endsWithDomain(sourceUrl, "facebook.com")
                || endsWithDomain(sourceUrl, "quora.com")
                || endsWithDomain(sourceUrl, "meta.com/is");
    }
}
