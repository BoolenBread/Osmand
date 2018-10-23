package net.osmand.telegram.ui

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.widget.ListPopupWindow
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import net.osmand.telegram.R
import net.osmand.telegram.TelegramSettings
import net.osmand.telegram.TelegramSettings.DurationPref
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.telegram.utils.AndroidNetworkUtils
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.OsmandApiUtils
import org.json.JSONObject

class SettingsDialogFragment : BaseDialogFragment() {

	private val uiUtils get() = app.uiUtils
	private lateinit var container : ViewGroup

	override fun onCreateView(
		inflater: LayoutInflater,
		parent: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val mainView = inflater.inflate(R.layout.fragement_settings_dialog, parent)
		container = mainView.findViewById<ViewGroup>(R.id.share_as_container);
		val appBarLayout = mainView.findViewById<View>(R.id.app_bar_layout)
		AndroidUtils.addStatusBarPadding19v(context!!, appBarLayout)

		mainView.findViewById<Toolbar>(R.id.toolbar).apply {
			navigationIcon = uiUtils.getThemedIcon(R.drawable.ic_arrow_back)
			setNavigationOnClickListener { dismiss() }
		}

		container = mainView.findViewById<ViewGroup>(R.id.gps_and_loc_container)
		for (pref in settings.gpsAndLocPrefs) {
			inflater.inflate(R.layout.item_with_desc_and_right_value, container, false).apply {
				findViewById<ImageView>(R.id.icon).setImageDrawable(uiUtils.getThemedIcon(pref.iconId))
				findViewById<TextView>(R.id.title).setText(pref.titleId)
				findViewById<TextView>(R.id.description).setText(pref.descriptionId)
				val valueView = findViewById<TextView>(R.id.value)
				valueView.text = pref.getCurrentValue()
				setOnClickListener {
					showPopupMenu(pref, valueView)
				}
				container.addView(this)
			}
		}

		container = mainView.findViewById(R.id.osmand_connect_container)
		for (appConn in TelegramSettings.AppConnect.values()) {
			val pack = appConn.appPackage
			val installed = AndroidUtils.isAppInstalled(context!!, pack)
			if (!installed && appConn.showOnlyInstalled) {
				continue
			}
			inflater.inflate(R.layout.item_with_rb_and_btn, container, false).apply {
				findViewById<ImageView>(R.id.icon).setImageDrawable(uiUtils.getIcon(appConn.iconId))
				findViewById<TextView>(R.id.title).text = appConn.title
				if (installed) {
					findViewById<View>(R.id.primary_btn).visibility = View.GONE
					findViewById<RadioButton>(R.id.radio_button).apply {
						visibility = View.VISIBLE
						isChecked = pack == settings.appToConnectPackage
					}
					setOnClickListener {
						if (settings.appToConnectPackage != appConn.appPackage) {
							settings.updateAppToConnect(appConn.appPackage)
							updateSelectedAppConn()
						}
					}
				} else {
					findViewById<RadioButton>(R.id.radio_button).visibility = View.GONE
					findViewById<TextView>(R.id.primary_btn).apply {
						setText(R.string.shared_string_install)
						setOnClickListener {
							context?.also { ctx ->
								startActivity(AndroidUtils.getPlayMarketIntent(ctx, pack))
							}
						}
					}
					setOnClickListener(null)
					isClickable = false
				}
				tag = pack
				container.addView(this)
			}
		}
		updateSelectedAppConn()

		container = mainView.findViewById(R.id.share_as_container)
		val user = telegramHelper.getCurrentUser()
		if (user != null) {
			addItemToContainer(inflater, container, user.id.toString(),  TelegramUiHelper.getUserName(user))
		}
		settings.shareDevicesIds.forEach {
			addItemToContainer(inflater, container, it.key, it.value)
		}

		if (user != null) {
			TelegramUiHelper.setupPhoto(
				app,
				mainView.findViewById(R.id.user_icon),
				telegramHelper.getUserPhotoPath(user),
				R.drawable.img_user_picture,
				false
			)
			mainView.findViewById<TextView>(R.id.username).text = TelegramUiHelper.getUserName(user)
		} else {
			mainView.findViewById<View>(R.id.user_row).visibility = View.GONE
		}

		mainView.findViewById<View>(R.id.logout_btn).setOnClickListener {
			fragmentManager?.also { fm ->
				LogoutBottomSheet.showInstance(fm, this)
			}

		}

		mainView.findViewById<ImageView>(R.id.help_icon)
			.setImageDrawable(uiUtils.getActiveIcon(R.drawable.ic_action_help))
		mainView.findViewById<View>(R.id.help_row).setOnClickListener {
			DisconnectTelegramBottomSheet.showInstance(childFragmentManager)
		}

		mainView.findViewById<TextView>(R.id.add_new_device_btn).setOnClickListener {
			fragmentManager?.also { fm ->
				AddNewDeviceBottomSheet.showInstance(fm, this)
			}
		}
		return mainView
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		when (requestCode) {
			LogoutBottomSheet.LOGOUT_REQUEST_CODE -> {
				logoutTelegram()
				dismiss()
			}
			AddNewDeviceBottomSheet.NEW_DEVICE_REQUEST_CODE -> {
				OsmandApiUtils.createNewDevice(app, data!!.getStringExtra("deviceName"), 12345678,
					object : AndroidNetworkUtils.OnRequestResultListener {
						override fun onResult(result: String) {
							val jsonResult = JSONObject(result)
							val status = jsonResult.getString("status")
							val deviceBot =
								OsmandApiUtils.parseDeviceBot(jsonResult.getJSONObject("device"))
							val inflater = activity?.layoutInflater

							if (inflater != null) {
								addItemToContainer(inflater, container, deviceBot.externalId, deviceBot.deviceName)
							}
						}
					})
//				dismiss()
			}
		}
	}

	private fun addItemToContainer(inflater: LayoutInflater, container: ViewGroup, tag: String, title: String) {
		inflater.inflate(R.layout.item_with_rb_and_btn, container, false).apply {
			findViewById<TextView>(R.id.title).text = title
			findViewById<View>(R.id.primary_btn).visibility = View.GONE
			findViewById<RadioButton>(R.id.radio_button).apply {
				visibility = View.VISIBLE
				isChecked = tag == settings.currentSharingMode
			}
			setOnClickListener {
				settings.currentSharingMode = tag
				updateSelectedSharingMode()
			}
			this.tag = tag
			container.addView(this)
		}
	}
	
	private fun showPopupMenu(pref: DurationPref, valueView: TextView) {
		val menuList = pref.getMenuItems()
		val ctx = valueView.context
		ListPopupWindow(ctx).apply {
			isModal = true
			anchorView = valueView
			setContentWidth(AndroidUtils.getPopupMenuWidth(ctx, menuList))
			height = AndroidUtils.getPopupMenuHeight(ctx)
			setDropDownGravity(Gravity.END or Gravity.TOP)
			setAdapter(ArrayAdapter(ctx, R.layout.popup_list_text_item, menuList))
			setOnItemClickListener { _, _, position, _ ->
				pref.setCurrentValue(position)
				valueView.text = pref.getCurrentValue()
				dismiss()
			}
			show()
		}
	}

	private fun updateSelectedAppConn() {
		view?.findViewById<ViewGroup>(R.id.osmand_connect_container)?.apply {
			for (i in 0 until childCount) {
				getChildAt(i).apply {
					findViewById<RadioButton>(R.id.radio_button).isChecked =
							tag == settings.appToConnectPackage
				}
			}
		}
	}

	private fun updateSelectedSharingMode() {
		view?.findViewById<ViewGroup>(R.id.share_as_container)?.apply {
			for (i in 0 until childCount) {
				getChildAt(i).apply {
					findViewById<RadioButton>(R.id.radio_button).isChecked =
							tag == settings.currentSharingMode
				}
			}
		}
	}

	private fun logoutTelegram() {
		val act = activity ?: return
		(act as MainActivity).logoutTelegram()
	}

	companion object {

		private const val TAG = "SettingsDialogFragment"

		fun showInstance(fm: FragmentManager): Boolean {
			return try {
				SettingsDialogFragment().show(fm, TAG)
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}
