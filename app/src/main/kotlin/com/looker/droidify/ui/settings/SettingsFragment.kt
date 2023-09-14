package com.looker.droidify.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.looker.core.common.SdkCheck
import com.looker.core.common.extension.homeAsUp
import com.looker.core.common.extension.systemBarsPadding
import com.looker.core.common.extension.updateAsMutable
import com.looker.core.datastore.Settings
import com.looker.core.datastore.extension.*
import com.looker.core.datastore.model.*
import com.looker.droidify.BuildConfig
import com.looker.droidify.databinding.EnumTypeBinding
import com.looker.droidify.databinding.SettingsPageBinding
import com.looker.droidify.databinding.SwitchTypeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import com.looker.core.common.BuildConfig as CommonBuildConfig
import com.looker.core.common.R as CommonR

@AndroidEntryPoint
class SettingsFragment : Fragment() {

	companion object {
		fun newInstance() = SettingsFragment()

		private val localeCodesList: List<String> = CommonBuildConfig.DETECTED_LOCALES
			.toList()
			.updateAsMutable { add(0, "system") }
	}

	private val viewModel: SettingsViewModel by viewModels()
	private var _binding: SettingsPageBinding? = null
	private val binding get() = _binding!!

	@SuppressLint("SetTextI18n")
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		_binding = SettingsPageBinding.inflate(inflater, container, false)
		binding.nestedScrollView.systemBarsPadding()
		val toolbar = binding.toolbar
		toolbar.navigationIcon = toolbar.context.homeAsUp
		toolbar.setNavigationOnClickListener { activity?.onBackPressed() }
		toolbar.title = getString(CommonR.string.settings)
		with(binding) {
			dynamicTheme.root.isVisible = SdkCheck.isSnowCake
			dynamicTheme.connect(
				titleText = getString(CommonR.string.material_you),
				contentText = getString(CommonR.string.material_you_desc),
				setting = viewModel.getInitialSetting { dynamicTheme }
			)
			homeScreenSwiping.connect(
				titleText = getString(CommonR.string.home_screen_swiping),
				contentText = getString(CommonR.string.home_screen_swiping_DESC),
				setting = viewModel.getInitialSetting { homeScreenSwiping }
			)
			autoUpdate.connect(
				titleText = getString(CommonR.string.auto_update),
				contentText = getString(CommonR.string.auto_update_apps),
				setting = viewModel.getInitialSetting { autoUpdate }
			)
			notifyUpdates.connect(
				titleText = getString(CommonR.string.notify_about_updates),
				contentText = getString(CommonR.string.notify_about_updates_summary),
				setting = viewModel.getInitialSetting { notifyUpdate }
			)
			unstableUpdates.connect(
				titleText = getString(CommonR.string.unstable_updates),
				contentText = getString(CommonR.string.unstable_updates_summary),
				setting = viewModel.getInitialSetting { unstableUpdate }
			)
			incompatibleUpdates.connect(
				titleText = getString(CommonR.string.incompatible_versions),
				contentText = getString(CommonR.string.incompatible_versions_summary),
				setting = viewModel.getInitialSetting { incompatibleVersions }
			)
			language.connect(
				titleText = getString(CommonR.string.prefs_language_title),
				map = { translateLocale(getLocaleOfCode(it)) },
				setting = viewModel.getSetting { language }
			)
			theme.connect(
				titleText = getString(CommonR.string.theme),
				setting = viewModel.getSetting { theme },
				map = { themeName(it) }
			)
			cleanUp.connect(
				titleText = getString(CommonR.string.cleanup_title),
				setting = viewModel.getSetting { cleanUpInterval },
				map = { toTime(it) }
			)
			autoSync.connect(
				titleText = getString(CommonR.string.sync_repositories_automatically),
				setting = viewModel.getSetting { autoSync },
				map = { autoSyncName(it) }
			)
			installer.connect(
				titleText = getString(CommonR.string.installer),
				setting = viewModel.getSetting { installerType },
				map = { installerName(it) }
			)
			proxyType.connect(
				titleText = getString(CommonR.string.proxy_type),
				setting = viewModel.getSetting { proxy.type },
				map = { proxyName(it) }
			)
			proxyHost.connect(
				titleText = getString(CommonR.string.proxy_host),
				setting = viewModel.getSetting { proxy.host },
				map = { it }
			)
			proxyPort.connect(
				titleText = getString(CommonR.string.proxy_port),
				setting = viewModel.getSetting { proxy.port },
				map = { it.toString() }
			)

			forceCleanUp.title.text = getString(CommonR.string.force_clean_up)
			forceCleanUp.content.text = getString(CommonR.string.force_clean_up_DESC)
			creditFoxy.title.text = getString(CommonR.string.special_credits)
			creditFoxy.content.text = "FoxyDroid"
			droidify.title.text = "Droid-ify"
			droidify.content.text = BuildConfig.VERSION_NAME
		}
		setChangeListener()
		viewLifecycleOwner.lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.RESUMED) {
				launch {
					viewModel.snackbarStringId.collect {
						Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
					}
				}
				launch {
					viewModel.settingsFlow.collect(::updateSettings)
				}
			}
		}
		return binding.root
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setChangeListener() {
		with(binding) {
			dynamicTheme.checked.setOnCheckedChangeListener { _, checked ->
				viewModel.setDynamicTheme(checked)
			}
			homeScreenSwiping.checked.setOnCheckedChangeListener { _, checked ->
				viewModel.setHomeScreenSwiping(checked)
			}
			notifyUpdates.checked.setOnCheckedChangeListener { _, checked ->
				viewModel.setNotifyUpdates(checked)
			}
			autoUpdate.checked.setOnCheckedChangeListener { _, checked ->
				viewModel.setAutoUpdate(checked)
			}
			unstableUpdates.checked.setOnCheckedChangeListener { _, checked ->
				viewModel.setUnstableUpdates(checked)
			}
			incompatibleUpdates.checked.setOnCheckedChangeListener { _, checked ->
				viewModel.setIncompatibleUpdates(checked)
			}
			forceCleanUp.root.setOnClickListener {
				viewModel.forceCleanup(it.context)
			}
			creditFoxy.root.setOnClickListener {
				openLink("https://github.com/kitsunyan/foxy-droid")
			}
			droidify.root.setOnClickListener {
				openLink("https://github.com/Iamlooker/Droid-ify")
			}
		}
	}

	private fun updateSettings(settings: Settings) {
		with(binding) {
			val allowProxies = settings.proxy.type != ProxyType.DIRECT
			proxyHost.root.isVisible = allowProxies
			proxyPort.root.isVisible = allowProxies
			forceCleanUp.root.isVisible = settings.cleanUpInterval == Duration.INFINITE
			language.root.setOnClickListener { view ->
				view.addSingleCorrectDialog(
					initialValue = settings.language,
					values = localeCodesList,
					title = CommonR.string.prefs_language_title,
					iconRes = CommonR.drawable.ic_language,
					onClick = viewModel::setLanguage,
					valueToString = { translateLocale(context?.getLocaleOfCode(it)) }
				).show()
			}
			theme.root.setOnClickListener { view ->
				view.addSingleCorrectDialog(
					initialValue = settings.theme,
					values = Theme.entries,
					title = CommonR.string.theme,
					iconRes = CommonR.drawable.ic_themes,
					onClick = viewModel::setTheme,
					valueToString = view.context::themeName
				).show()
			}
			cleanUp.root.setOnClickListener { view ->
				view.addSingleCorrectDialog(
					initialValue = settings.cleanUpInterval,
					values = cleanUpIntervals,
					valueToString = { context.toTime(it) },
					title = CommonR.string.cleanup_title,
					iconRes = CommonR.drawable.ic_time,
					onClick = viewModel::setCleanUpInterval
				).show()
			}
			autoSync.root.setOnClickListener { view ->
				view.addSingleCorrectDialog(
					initialValue = settings.autoSync,
					values = AutoSync.entries,
					title = CommonR.string.sync_repositories_automatically,
					iconRes = CommonR.drawable.ic_sync,
					onClick = viewModel::setAutoSync,
					valueToString = view.context::autoSyncName
				).show()
			}
			proxyType.root.setOnClickListener { view ->
				view.addSingleCorrectDialog(
					initialValue = settings.proxy.type,
					values = ProxyType.entries,
					title = CommonR.string.proxy_type,
					iconRes = CommonR.drawable.ic_proxy,
					onClick = viewModel::setProxyType,
					valueToString = view.context::proxyName
				).show()
			}
			proxyHost.root.setOnClickListener { view ->
				view.addEditTextDialog(
					initialValue = settings.proxy.host,
					title = CommonR.string.proxy_host,
					onFinish = viewModel::setProxyHost
				).show()
			}
			proxyPort.root.setOnClickListener { view ->
				view.addEditTextDialog(
					initialValue = settings.proxy.port.toString(),
					title = CommonR.string.proxy_host,
					onFinish = viewModel::setProxyPort
				).show()
			}
			installer.root.setOnClickListener { view ->
				view.addSingleCorrectDialog(
					initialValue = settings.installerType,
					values = InstallerType.entries,
					title = CommonR.string.installer,
					iconRes = CommonR.drawable.ic_download,
					onClick = viewModel::setInstaller,
					valueToString = view.context::installerName
				).show()
			}
		}
	}

	private val cleanUpIntervals =
		listOf(6.hours, 12.hours, 18.hours, 1.days, 2.days, Duration.INFINITE)

	private fun translateLocale(locale: Locale?): String {
		val country = locale?.getDisplayCountry(locale)
		val language = locale?.getDisplayLanguage(locale)
		val languageDisplay = if (locale != null) {
			(language?.replaceFirstChar { it.uppercase(Locale.getDefault()) }
					+ (if (country?.isNotEmpty() == true && country.compareTo(
					language.toString(),
					true
				) != 0
			)
				"($country)" else ""))
		} else getString(CommonR.string.system)
		return languageDisplay
	}

	private fun openLink(link: String) {
		try {
			startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	@Suppress("DEPRECATION")
	private fun Context.getLocaleOfCode(localeCode: String): Locale? = when {
		localeCode.isEmpty() -> if (SdkCheck.isNougat) resources.configuration.locales[0]
		else resources.configuration.locale

		localeCode.contains("-r") -> Locale(
			localeCode.substring(0, 2),
			localeCode.substring(4)
		)

		localeCode.contains("_") -> Locale(
			localeCode.substring(0, 2),
			localeCode.substring(3)
		)

		localeCode == "system" -> null
		else -> Locale(localeCode)
	}

	private fun <T> EnumTypeBinding.connect(
		titleText: String,
		setting: Flow<T>,
		map: Context.(T) -> String
	) {
		title.text = titleText
		viewLifecycleOwner.lifecycleScope.launch {
			setting.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
				.collect {
					with(root.context) {
						content.text = map(it)
					}
				}
		}
	}

	private fun SwitchTypeBinding.connect(
		titleText: String,
		contentText: String,
		setting: Flow<Boolean>
	) {
		title.text = titleText
		content.text = contentText
		viewLifecycleOwner.lifecycleScope.launch {
			setting.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
				.collect(checked::setChecked)
		}
	}

	private fun <T> View.addSingleCorrectDialog(
		initialValue: T,
		values: List<T>,
		@StringRes title: Int,
		@DrawableRes iconRes: Int,
		onClick: (T) -> Unit,
		valueToString: (T) -> String
	) = MaterialAlertDialogBuilder(context)
		.setTitle(title)
		.setIcon(iconRes)
		.setSingleChoiceItems(
			values.map(valueToString).toTypedArray(),
			values.indexOf(initialValue)
		) { dialog, newValue ->
			dialog.dismiss()
			post {
				onClick(values.elementAt(newValue))
			}
		}
		.setNegativeButton(CommonR.string.cancel, null)
		.create()

	private fun View.addEditTextDialog(
		initialValue: String,
		@StringRes title: Int,
		onFinish: (String) -> Unit
	): AlertDialog {
		val scroll = NestedScrollView(context)
		val customEditText = TextInputEditText(context)
		customEditText.id = android.R.id.edit
		val paddingValue = context.resources.getDimension(CommonR.dimen.shape_margin_large).toInt()
		scroll.setPadding(paddingValue, 0, paddingValue, 0)
		customEditText.setText(initialValue)
		customEditText.hint = customEditText.text.toString()
		customEditText.text?.let { editable -> customEditText.setSelection(editable.length) }
		customEditText.requestFocus()
		scroll.addView(
			customEditText,
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.WRAP_CONTENT
		)
		return MaterialAlertDialogBuilder(context)
			.setTitle(title)
			.setView(scroll)
			.setPositiveButton(CommonR.string.ok) { _, _ ->
				post { onFinish(customEditText.text.toString()) }
			}
			.setNegativeButton(CommonR.string.cancel, null)
			.create()
			.apply {
				window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
			}
	}
}