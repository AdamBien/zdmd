package airhacks.zdmd.linting.control;

public interface Levenshtein {

    static int distance(String a, String b) {
        var m = a.length();
        var n = b.length();
        var dp = new int[m + 1][n + 1];
        for (var i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (var j = 0; j <= n; j++) {
            dp[0][j] = j;
        }
        for (var i = 1; i <= m; i++) {
            for (var j = 1; j <= n; j++) {
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1]
                        : 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
            }
        }
        return dp[m][n];
    }
}
