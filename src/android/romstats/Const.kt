package android.romstats

object Const {
    const val TAG = "ResurrectionStats"

    const val ANONYMOUS_OPT_IN = "pref_anonymous_opt_in"
    const val ANONYMOUS_OPT_OUT_PERSIST = "pref_anonymous_opt_out_persist"
    const val ANONYMOUS_FIRST_BOOT = "pref_anonymous_first_boot"
    const val ANONYMOUS_LAST_CHECKED = "pref_anonymous_checked_in"
    const val ANONYMOUS_LAST_REPORT_VERSION = "pref_anonymous_last_rep_version"
    const val ANONYMOUS_NEXT_ALARM = "pref_anonymous_next_alarm"

    const val STATS_URL = "https://resurrectionremix.sourceforge.io/"
    const val RR_VERSION = "6.0.0"
    const val ROMNAME = "ResurrectionRemix"
    const val TIMEFRAME = 1
    val DEBUG = BuildConfig.DEBUG
}
