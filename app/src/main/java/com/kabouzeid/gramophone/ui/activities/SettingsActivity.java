package com.kabouzeid.gramophone.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.TwoStatePreference;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.appthemehelper.common.prefs.supportv7.ATEColorPreference;
import com.kabouzeid.appthemehelper.common.prefs.supportv7.ATEPreferenceFragmentCompat;
import com.kabouzeid.appthemehelper.util.ColorUtil;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.appshortcuts.DynamicShortcutManager;
import com.kabouzeid.gramophone.helper.MusicPlayerRemote;
import com.kabouzeid.gramophone.preferences.NowPlayingScreenPreference;
import com.kabouzeid.gramophone.preferences.NowPlayingScreenPreferenceDialog;
import com.kabouzeid.gramophone.service.MusicService;
import com.kabouzeid.gramophone.ui.activities.base.AbsBaseActivity;
import com.kabouzeid.gramophone.util.NavigationUtil;
import com.kabouzeid.gramophone.util.PreferenceUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingsActivity extends AbsBaseActivity implements
        ColorChooserDialog.ColorCallback,
        GoogleApiClient.OnConnectionFailedListener
{
    public static final String TAG = SettingsActivity.class.getSimpleName();

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        setDrawUnderStatusbar(true);
        ButterKnife.bind(this);

        setStatusbarColorAuto();
        setNavigationbarColorAuto();
        setTaskDescriptionColorAuto();

        toolbar.setBackgroundColor(ThemeStore.primaryColor(this));
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Google login
        GoogleSignInOptions opt = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(
                        Drive.SCOPE_FILE,
                        Drive.SCOPE_APPFOLDER,
                        new Scope("https://www.googleapis.com/auth/drive"))
                .build();

        GoogleApiClient mGoogle = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, opt)
                .addApi(Drive.API)
                .addScope(new Scope("https://www.googleapis.com/auth/drive"))
                .build();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new SettingsFragment()).commit();
        } else {
            SettingsFragment frag = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
            if (frag != null) frag.invalidateSettings();

        }

        Intent signIn = Auth.GoogleSignInApi.getSignInIntent(mGoogle);
        startActivityForResult(signIn, 666);


        int permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 666) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

            Log.d(TAG, "handleSignInResult:" + result.isSuccess());
            if (result.isSuccess()) {
                // Signed in successfully, show authenticated UI.
                GoogleSignInAccount acct = result.getSignInAccount();
                Log.d(TAG, "display_name: " + acct.getDisplayName());
                MusicService.getInstance().googleAccount = acct;
//                updateUI(true);
            } else {
                // Signed out, show unauthenticated UI.
//                updateUI(false);
            }
        }
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, @ColorInt int selectedColor) {
        switch (dialog.getTitle()) {
            case R.string.primary_color:
                ThemeStore.editTheme(this)
                        .primaryColor(selectedColor)
                        .commit();
                break;
            case R.string.accent_color:
                ThemeStore.editTheme(this)
                        .accentColor(selectedColor)
                        .commit();
                break;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            new DynamicShortcutManager(this).updateDynamicShortcuts();
        }
        recreate();
    }

    @Override
    public void onColorChooserDismissed(@NonNull ColorChooserDialog dialog) {

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public static class SettingsFragment extends ATEPreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

        private static void setSummary(@NonNull Preference preference) {
            setSummary(preference, PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getString(preference.getKey(), ""));
        }

        private static void setSummary(Preference preference, @NonNull Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else {
                preference.setSummary(stringValue);
            }
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.pref_general);
            addPreferencesFromResource(R.xml.pref_colors);
            addPreferencesFromResource(R.xml.pref_notification);
            addPreferencesFromResource(R.xml.pref_now_playing_screen);
            addPreferencesFromResource(R.xml.pref_images);
            addPreferencesFromResource(R.xml.pref_lockscreen);
            addPreferencesFromResource(R.xml.pref_audio);
            addPreferencesFromResource(R.xml.pref_playlists);
            addPreferencesFromResource(R.xml.pref_sync);
        }

        @Nullable
        @Override
        public DialogFragment onCreatePreferenceDialog(Preference preference) {
            if (preference instanceof NowPlayingScreenPreference) {
                return NowPlayingScreenPreferenceDialog.newInstance();
            }
            return super.onCreatePreferenceDialog(preference);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getListView().setPadding(0, 0, 0, 0);
            invalidateSettings();
            PreferenceUtil.getInstance(getActivity()).registerOnSharedPreferenceChangedListener(this);
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            PreferenceUtil.getInstance(getActivity()).unregisterOnSharedPreferenceChangedListener(this);
        }

        private void invalidateSettings() {
            final Preference defaultStartPage = findPreference("default_start_page");
            setSummary(defaultStartPage);
            defaultStartPage.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, @NonNull Object o) {
                    setSummary(defaultStartPage, o);
                    return true;
                }
            });

            final Preference generalTheme = findPreference("general_theme");
            setSummary(generalTheme);
            generalTheme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, @NonNull Object o) {
                    setSummary(generalTheme, o);
                    ThemeStore.editTheme(getActivity())
                            .activityTheme(PreferenceUtil.getThemeResFromPrefValue((String) o))
                            .commit();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                        //Set the new theme so that updateAppShortcuts can pull it
                        getActivity().setTheme(PreferenceUtil.getThemeResFromPrefValue((String) o));
                        new DynamicShortcutManager(getActivity()).updateDynamicShortcuts();
                    }

                    getActivity().recreate();
                    return true;
                }
            });

            final Preference autoDownloadImagesPolicy = findPreference("auto_download_images_policy");
            setSummary(autoDownloadImagesPolicy);
            autoDownloadImagesPolicy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, @NonNull Object o) {
                    setSummary(autoDownloadImagesPolicy, o);
                    return true;
                }
            });

            final ATEColorPreference primaryColorPref = (ATEColorPreference) findPreference("primary_color");
            final int primaryColor = ThemeStore.primaryColor(getActivity());
            primaryColorPref.setColor(primaryColor, ColorUtil.darkenColor(primaryColor));
            primaryColorPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull Preference preference) {
                    new ColorChooserDialog.Builder(((SettingsActivity) getActivity()), R.string.primary_color)
                            .accentMode(false)
                            .allowUserColorInput(true)
                            .allowUserColorInputAlpha(false)
                            .preselect(primaryColor)
                            .show();
                    return true;
                }
            });

            final ATEColorPreference accentColorPref = (ATEColorPreference) findPreference("accent_color");
            final int accentColor = ThemeStore.accentColor(getActivity());
            accentColorPref.setColor(accentColor, ColorUtil.darkenColor(accentColor));
            accentColorPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull Preference preference) {
                    new ColorChooserDialog.Builder(((SettingsActivity) getActivity()), R.string.accent_color)
                            .accentMode(true)
                            .allowUserColorInput(true)
                            .allowUserColorInputAlpha(false)
                            .preselect(accentColor)
                            .show();
                    return true;
                }
            });

            TwoStatePreference colorNavBar = (TwoStatePreference) findPreference("should_color_navigation_bar");
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                colorNavBar.setEnabled(false);
                colorNavBar.setSummary(R.string.pref_only_lollipop);
            } else {
                colorNavBar.setChecked(ThemeStore.coloredNavigationBar(getActivity()));
                colorNavBar.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        ThemeStore.editTheme(getActivity())
                                .coloredNavigationBar((Boolean) newValue)
                                .commit();
                        getActivity().recreate();
                        return true;
                    }
                });
            }

            final TwoStatePreference classicNotification = (TwoStatePreference) findPreference("classic_notification");
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
                classicNotification.setEnabled(false);
                classicNotification.setSummary(R.string.pref_only_nougat);
            } else {
                classicNotification.setChecked(PreferenceUtil.getInstance(getActivity()).classicNotification());
                classicNotification.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        //Save preference
                        PreferenceUtil.getInstance(getActivity()).setClassicNotification((Boolean)newValue);

                        final MusicService service = MusicPlayerRemote.musicService;
                        if (service != null) {
                            service.initNotification();
                            service.updateNotification();
                        }

                        return true;
                    }
                });
            }

            final TwoStatePreference colorAppShortcuts = (TwoStatePreference) findPreference("should_color_app_shortcuts");
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
                colorAppShortcuts.setEnabled(false);
                colorAppShortcuts.setSummary(R.string.pref_only_nougat_mr1);
            } else {
                colorAppShortcuts.setChecked(PreferenceUtil.getInstance(getActivity()).coloredAppShortcuts());
                colorAppShortcuts.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        //Save preference
                        PreferenceUtil.getInstance(getActivity()).setColoredAppShortcuts((Boolean)newValue);

                        //Update app shortcuts
                        new DynamicShortcutManager(getActivity()).updateDynamicShortcuts();

                        return true;
                    }
                });
            }

            final Preference equalizer = findPreference("equalizer");
            if (!hasEqualizer()) {
                equalizer.setEnabled(false);
                equalizer.setSummary(getResources().getString(R.string.no_equalizer));
            }
            equalizer.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    NavigationUtil.openEqualizer(getActivity());
                    return true;
                }
            });

            updateNowPlayingScreenSummary();
        }

        private boolean hasEqualizer() {
            final Intent effects = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
            PackageManager pm = getActivity().getPackageManager();
            ResolveInfo ri = pm.resolveActivity(effects, 0);
            return ri != null;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch (key) {
                case PreferenceUtil.NOW_PLAYING_SCREEN_ID:
                    updateNowPlayingScreenSummary();
                    break;
            }
        }

        private void updateNowPlayingScreenSummary() {
            findPreference("now_playing_screen_id").setSummary(PreferenceUtil.getInstance(getActivity()).getNowPlayingScreen().titleRes);
        }
    }
}
