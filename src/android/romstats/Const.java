package android.romstats;

public class Const {

	public static final String TAG = "ResurrectionStats";

	public static final String ANONYMOUS_OPT_IN = "pref_anonymous_opt_in";
	public static final String ANONYMOUS_OPT_OUT_PERSIST = "pref_anonymous_opt_out_persist";
	public static final String ANONYMOUS_FIRST_BOOT = "pref_anonymous_first_boot";
	public static final String ANONYMOUS_LAST_CHECKED = "pref_anonymous_checked_in";
	public static final String ANONYMOUS_LAST_REPORT_VERSION = "pref_anonymous_last_rep_version";
	public static final String ANONYMOUS_NEXT_ALARM = "pref_anonymous_next_alarm";

	public static final int ROMSTATS_REPORTING_MODE_NEW = 0; // new CM10.1: no user prompt, default TRUE, first time after tframe
	public static final int ROMSTATS_REPORTING_MODE_OLD = 1; // old CM10  : user prompt, default FALSE, first time immediately

	public static final String STATS_URL = "http://resurrectionremix.sourceforge.net";
	public static final String RR_VERSION = "5.8.1";
	public static final String ROMNAME = "ResurrectionRemix";
	public static final int TIMEFRAME = 1;
}
