package com.heisha.simplifieddemo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.heisha.heisha_sdk.Component.AirConditioner.AirConditioner;
import com.heisha.heisha_sdk.Component.AirConditioner.AirConditionerStateCallback;
import com.heisha.heisha_sdk.Component.AirConditioner.AirConditionerWorkingMode;
import com.heisha.heisha_sdk.Component.BaseComponent;
import com.heisha.heisha_sdk.Component.Canopy.Canopy;
import com.heisha.heisha_sdk.Component.Canopy.CanopyState;
import com.heisha.heisha_sdk.Component.Canopy.CanopyStateCallback;
import com.heisha.heisha_sdk.Component.Charger.BatteryDetectState;
import com.heisha.heisha_sdk.Component.Charger.ChargeState;
import com.heisha.heisha_sdk.Component.Charger.Charger;
import com.heisha.heisha_sdk.Component.Charger.ChargerStateCallback;
import com.heisha.heisha_sdk.Component.Charger.DroneSwitchState;
import com.heisha.heisha_sdk.Component.ConnStatus;
import com.heisha.heisha_sdk.Component.ControlCenter.ConfigFailReason;
import com.heisha.heisha_sdk.Component.ControlCenter.ConfigParameter;
import com.heisha.heisha_sdk.Component.ControlCenter.ControlCenter;
import com.heisha.heisha_sdk.Component.ControlCenter.ControlCenterStateCallback;
import com.heisha.heisha_sdk.Component.ControlCenter.ThingLevel;
import com.heisha.heisha_sdk.Component.EdgeComputing.EdgeComputing;
import com.heisha.heisha_sdk.Component.EdgeComputing.EdgeStateCallback;
import com.heisha.heisha_sdk.Component.EdgeComputing.PowerState;
import com.heisha.heisha_sdk.Component.PositionBar.PositionBar;
import com.heisha.heisha_sdk.Component.PositionBar.PositionBarState;
import com.heisha.heisha_sdk.Component.PositionBar.PositionBarStateCallback;
import com.heisha.heisha_sdk.Component.RemoteControl.RemoteControl;
import com.heisha.heisha_sdk.Component.RemoteControl.RemoteControlStateCallback;
import com.heisha.heisha_sdk.Manager.HSSDKManager;
import com.heisha.heisha_sdk.Manager.SDKManagerCallback;
import com.heisha.heisha_sdk.Manager.ServiceCode;
import com.heisha.heisha_sdk.Manager.ServiceResult;
import com.heisha.heisha_sdk.Product.DNEST;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

	private SharedPreferences mPref;
	private static final String TAG = "MainActivity";

	private DNEST mDNEST;
	public Canopy mCanopy;
	public PositionBar mPositionBar;
	public Charger mCharger;
	public EdgeComputing mEdgeComputing;
	public ControlCenter mControlCenter;
	public AirConditioner mAirConditioner;
	public RemoteControl mRemoteControl;

	private FloatingActionButton btnLogin;
	private ScrollView mScrollInfo;
	private TextView mTxtInfo;
	private ImageView btnClear;
	private Button btnEnvironmentalCheck;
	private Button btnReadyToTakeoff;
	private Button btnChargingImmediately;

	private Timer mTimer;

	public int mReadyToTakeoffResult = 0;    //0 Unknown, 1 Successful, 2 Failed
	public int mChargingImmediatelyResult = 0;     //0 Unknown, 1 Successful, 2 Failed
	private int mAlarmCode;
	private float mHeatingTempweature = 0f;
	private float mCoolingTempweature = 0f;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mPref = getSharedPreferences("connection info", MODE_PRIVATE);
		initView();
	}

	private void initView() {
		mScrollInfo = this.findViewById(R.id.scroll_info);
		btnLogin = this.findViewById(R.id.btn_login);
		mTxtInfo = this.findViewById(R.id.txt_info);
		btnClear = this.findViewById(R.id.btn_clear);
		btnEnvironmentalCheck = this.findViewById(R.id.btn_environmental_check);
		btnReadyToTakeoff = this.findViewById(R.id.btn_ready_to_takeoff);
		btnChargingImmediately = this.findViewById(R.id.btn_charging_immediately);
		View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switch(v.getId()) {
					case R.id.btn_login:
						LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
						LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dialog_connect, null);
						final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
						dialog.setTitle("Connect to Device");
						dialog.setView(layout);
						dialog.show();
						final EditText editServerURL = layout.findViewById(R.id.edit_server_address);
						final EditText editDeviceSerial = layout.findViewById(R.id.edit_device_serial);
						Button btnConnect = layout.findViewById(R.id.btn_connect);
						editServerURL.setText(mPref.getString("serverURL", ""));
						editDeviceSerial.setText(mPref.getString("deviceSerial", ""));
						btnConnect.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								String serverURL = editServerURL.getText().toString();
								String deviceSerial = editDeviceSerial.getText().toString();
								registApp(serverURL, deviceSerial);
								SharedPreferences.Editor edit = mPref.edit();
								edit.putString("serverURL", serverURL);
								edit.putString("deviceSerial", deviceSerial);
								edit.apply();
								dialog.dismiss();
							}
						});
						break;

					case R.id.btn_clear:
						mTxtInfo.setText("");
						break;

					case R.id.btn_environmental_check:
						startEnvironmentalCheck();
						break;

					case R.id.btn_ready_to_takeoff:
						readyToTakeoff();
						break;

					case R.id.btn_charging_immediately:
						startChargingImmediately();
						break;
				}
			}
		};
		btnLogin.setOnClickListener(listener);
		btnClear.setOnClickListener(listener);
		btnEnvironmentalCheck.setOnClickListener(listener);
		btnReadyToTakeoff.setOnClickListener(listener);
		btnChargingImmediately.setOnClickListener(listener);
	}

	private void registApp(final String serverURI, String deviceSerial) {
		HSSDKManager.getInstance().registAPP(deviceSerial, serverURI, new SDKManagerCallback() {
			@Override
			public void onRegister() {
				Log.d(TAG, "onRegister: Registeration is complete");
			}

			@Override
			public void onServerConnected(boolean b, String s) {
				Log.d(TAG, "connectComplete: Connecting server success");
				Toast.makeText(MainActivity.this, "Connecting server success", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onServerDisconnected(Throwable throwable) {
				Log.d(TAG, "connectionLost: Server connection lost");
				throwable.printStackTrace();
			}

			@Override
			public void onProductConnected(final String deviceName) {
				Log.d(TAG, "onProductConnected: HeiSha device connection success");
				Toast.makeText(MainActivity.this, "HeiSha device connection success", Toast.LENGTH_SHORT).show();
				initDevice(deviceName);
				initDeviceCallback();
			}

			@Override
			public void onProductDisconnected() {
				Log.d(TAG, "onProductConnected: HeiSha device connection lost");
			}

			@Override
			public void onComponentChanged(BaseComponent baseComponent, ConnStatus connStatus) {
				Log.d(TAG, "onComponentChanged: Component status changed");
			}
		});
	}

	private void initDevice(String productName) {
		mDNEST = (DNEST) HSSDKManager.getInstance().getProduct(productName);
		mCanopy = mDNEST.getCanopy();
		mPositionBar = mDNEST.getPositionBar();
		mCharger = mDNEST.getCharger();
		mEdgeComputing = mDNEST.getEdgeComputing();
		mControlCenter = mDNEST.getControlCenter();
		mAirConditioner = mDNEST.getAirConditioner();
		mRemoteControl = mDNEST.getRemoteControl();
	}

	private void initDeviceCallback() {
		mCanopy.setStateCallback(new CanopyStateCallback() {
			@Override
			public void onUpdate(ConnStatus connStatus, CanopyState canopyState) {
				Log.d(TAG, "canopy: connection state:" + connStatus.toString() + ", state:" + canopyState.toString());
			}

			@Override
			public void onOperateResult(ServiceCode serviceCode, ServiceResult serviceResult) {
				Log.d(TAG, "canopy operate:" + serviceCode.toString() + ", result:" + serviceResult.toString());
			}
		});

		mPositionBar.setStateCallback(new PositionBarStateCallback() {
			@Override
			public void onUpdate(ConnStatus connStatus, PositionBarState positionBarState) {
				Log.d(TAG, "position bar: connection state:" + connStatus.toString() + ", state:" + positionBarState.toString());
			}

			@Override
			public void onOperateResult(ServiceCode serviceCode, ServiceResult serviceResult) {
				Log.d(TAG, "position bar operate:" + serviceCode.toString() + ", result:" + serviceResult.toString());
			}
		});

		mCharger.setStateCallback(new ChargerStateCallback() {
			@Override
			public void onUpdate(ConnStatus connStatus, ChargeState chargeState, BatteryDetectState batteryDetectState, DroneSwitchState droneSwitchState, int voltage, int current) {
				Log.d(TAG, "charger: connection state:" + connStatus.toString() + ", state:" + chargeState.toString() +
						", battery detect:" + batteryDetectState.toString() + ", drone state:" + droneSwitchState.toString());
			}

			@Override
			public void onOperateResult(ServiceCode serviceCode, ServiceResult serviceResult) {
				Log.d(TAG, "charger operate:" + serviceCode.toString() + ", result:" + serviceResult.toString());
			}
		});

		mEdgeComputing.setStateCallback(new EdgeStateCallback() {
			@Override
			public void onUpdate(PowerState androidPowerState, PowerState NVIDIAPowerState) {
				Log.d(TAG, "Edge: android power:" + androidPowerState.toString() + ", NVIDIA power:" + NVIDIAPowerState.toString());
			}

			@Override
			public void onOperateResult(ServiceCode serviceCode, ServiceResult serviceResult) {
				Log.d(TAG, "Edge operate:" + serviceCode.toString() + ", result:" + serviceResult.toString());
			}
		});

		mControlCenter.setStateCallback(new ControlCenterStateCallback() {
			@Override
			public void onUpdate() {
			}

			@Override
			public void onThingsPost(ThingLevel thingLevel, int code) {
				switch(code) {
					case 101:
						mAlarmCode = 0;
						mAlarmCode = mControlCenter.getThing().getParam().getAlarm();
						mReadyToTakeoffResult = 2;
						break;
					case 102:
						mAlarmCode = 0;
						mAlarmCode = mControlCenter.getThing().getParam().getAlarm();
						mChargingImmediatelyResult = 2;
						break;
					case 1:
						mReadyToTakeoffResult = 1;
						break;
					case 2:
						mChargingImmediatelyResult = 1;
						break;
				}
				Log.d(TAG, "onThingsPost: " + thingLevel.toString() + " code" + code);
			}

			@Override
			public void onOperateResult(ServiceCode serviceCode, ServiceResult serviceResult) {
			}

			@Override
			public void onGetConfigVersionInfo(int version, int paramNum) {
			}

			@Override
			public void onGetConfig(ServiceResult result, ConfigParameter paramIndex, int value) {
				if (result == ServiceResult.SUCCESS) {
					switch(paramIndex) {
						case SERVICE_PARAM_AIR_ROOM_MAXTEM:
							mCoolingTempweature = (value - 1000) / 10f;
							break;
						case SERVICE_PARAM_AIR_ROOM_MINTEM:
							mHeatingTempweature = (value - 1000) / 10f;
							break;
					}
				}
			}

			@Override
			public void onSetConfig(ServiceResult serviceResult, ConfigParameter configParameter, ConfigFailReason configFailReason) {
			}
		});

		mAirConditioner.setStateCallback(new AirConditionerStateCallback() {
			@Override
			public void onUpdate(ConnStatus connStatus, AirConditionerWorkingMode airConditionerWorkingMode) {
				Log.d(TAG, "AirConditioner: connection state: " + connStatus.toString() + ", Working Mode:" + airConditionerWorkingMode.toString());
			}

			@Override
			public void onOperateResult(ServiceCode serviceCode, ServiceResult serviceResult) {
				Log.d(TAG, "AirConditioner operate:" + serviceCode.toString() + ", result:" + serviceResult.toString());
			}
		});

		mRemoteControl.setStateCallback(new RemoteControlStateCallback() {
			@Override
			public void onUpdate(ConnStatus connStatus) {
				Log.d(TAG, "RemoteControl connection state: " + connStatus.toString());
			}

			@Override
			public void onOperateResult(ServiceCode serviceCode, ServiceResult serviceResult) {
				Log.d(TAG, "RemoteControl operate:" + serviceCode.toString() + ", result:" + serviceResult.toString());
			}
		});
	}

	private void startEnvironmentalCheck() {
		StringBuilder print = new StringBuilder();
		if (HSSDKManager.getInstance().getMQTTServerConnectionStatus() == ConnStatus.CONNECTED) {
			if (HSSDKManager.getInstance().getDeviceConnectionStatus() == ConnStatus.CONNECTED) {
				byte checkResult = mEdgeComputing.getMeteorologicalStation().environmentalCheck();
				if ((checkResult & 0x01) > 0) {
					if ((checkResult & 0x02) > 0) {
						print.append("Wind meter connected\n");
						print.append("Current wind speed ").
								append(mEdgeComputing.getMeteorologicalStation().getAnemograph().getWindSpeed()).append("m/s\n");
					} else {
						print.append("Wind meter not connected\n");
					}

					if ((checkResult & 0x08) > 0) {
						print.append("Rain meter connected\n");
						print.append("Current cumulative rainfall value ").
								append(mEdgeComputing.getMeteorologicalStation().getRainGauge().getRainfall()).append("mm\n");
					} else {
						print.append("Rain meter not connected\n");
					}

					if ((checkResult & 0x20) > 0) {
						print.append("Hygrothermograph is connected\n");
						print.append("Current temperature ").
								append(mEdgeComputing.getMeteorologicalStation().getHygrothermograph().getTemperature()).append("℃\n");
						print.append("Current humidity ").
								append(mEdgeComputing.getMeteorologicalStation().getHygrothermograph().getHumidity()).append("%\n");
					} else {
						print.append("Hygrothermograph not connected\n");
					}
				} else {
					print.append("Meteorological station not connected, environmental detection failed\n");
				}

				print.append("Summary report：");
				if ((checkResult & 0x01) == 0) {
					print.append("Meteorological station not connected, not recommended for takeoff\n");
				} else if ((checkResult & 0x02) == 0) {
					print.append("Wind speed unknown, not recommended to take off\n");
				} else if ((checkResult & 0x04) > 0) {
					print.append("Wind scale greater than 5, not recommended to take off\n");
				} else if ((checkResult & 0x08) == 0) {
					print.append("Rainfall value unknown, not recommended for takeoff\n");
				} else if ((checkResult & 0x10) > 0) {
					print.append("Raining, not recommended to take off\n");
				} else {
					print.append("Environmental coefficient detection is completed, wind level is less than 5, no rainfall, can take off\n");
				}
			} else {
				print.append("HeiSha device not connected, environment detection failur\n");
			}
		} else {
			print.append("MQTT Server not connected, environment detection failed\n");
		}

		mTxtInfo.setText(print.toString());
		mScrollInfo.fullScroll(View.FOCUS_DOWN);
	}

	private void readyToTakeoff() {
		if (mTimer == null) {
			mTimer = new Timer();
			TimerTask timerTask = new TimerTask() {
				StringBuilder print = new StringBuilder();
				int step = 0;
				int actionResult = 0; //0 Running, 1 Successful, 2 Failed

				@Override
				public void run() {
					switch(step) {
						case 0:
							if (HSSDKManager.getInstance().getMQTTServerConnectionStatus() == ConnStatus.CONNECTED) {
								if (HSSDKManager.getInstance().getDeviceConnectionStatus() == ConnStatus.CONNECTED) {
									mControlCenter.ShortcutReadyFlying();
									print.append("One-click flight preparation starts\n");
									step++;
								} else {
									print.append("The HeiSha device is not connected, One-click flight preparation interrupted\n");
									actionResult = 2;
								}
							} else {
								print.append("MQTT server is not connected,One-click flight preparation interrupted\n");
								actionResult = 2;
							}
							break;
						case 1:
							if (mRemoteControl.getConnectionStatus() == ConnStatus.CONNECTED) {
								if (mRemoteControl.getRCUSBCableStatus() == 1) {
									print.append("The remote control USB cable is inserted\n");
									step++;
								}
							} else {
								print.append("The remote control module is not connected,One-click flight preparation interrupted\n");
								actionResult = 2;
							}
							break;
						case 2:
							if (mCharger.getConnectionStatus() == ConnStatus.CONNECTED) {
								if (mCharger.getBatteryManager().getBatteryDetectStatus() == BatteryDetectState.BATTERY_DETECT_YES) {
									print.append("Drones have been detected\n");
									step++;
								}
							} else {
								print.append("Charging control module not connected,One-click flight preparation interrupted\n");
								actionResult = 2;
							}
							break;
						case 3:
							if (mCharger.getConnectionStatus() == ConnStatus.CONNECTED) {
								if (mCharger.getDroneSwitch().getDroneStatus() == DroneSwitchState.DRONE_STATUS_POWER_ON) {
									print.append("Drone powered on\n");
									step++;
								}
							} else {
								print.append("Charging control module not connected,One-click flight preparation interrupted\n");
								actionResult = 2;
							}
							break;
						case 4:
							if (mRemoteControl.getConnectionStatus() == ConnStatus.CONNECTED) {
								if (mRemoteControl.getRCPowerState() == PowerState.POWER_ON) {
									print.append("Remote control powered on\n");
									step++;
								}
							} else {
								print.append("Remote control module not connected,One-click flight preparation interrupted\n");
								actionResult = 2;
							}
							break;
						case 5:
							if (mPositionBar.getConnectionState() == ConnStatus.CONNECTED) {
								if (mPositionBar.getPositionBarState() == PositionBarState.POSITION_BAR_STATUS_UNLOCKED) {
									print.append("Charge bars released\n");
									step++;
								}
							} else {
								print.append("Charge bars module not connected,One-click flight preparation interrupted\n");
								actionResult = 2;
							}
							break;
						case 6:
							if (mCanopy.getConnectionState() == ConnStatus.CONNECTED) {
								if (mCanopy.getCanopyState() == CanopyState.CANOPY_STATUS_OPEN) {
									print.append("Canopy opened\n");
									step++;
								}
							} else {
								print.append("Canopy control module not connected,One-click flight preparation interrupted\n");
								actionResult = 2;
							}
							break;
						case 7:
							if (mReadyToTakeoffResult == 1) {
								print.append("One-click flight preparation complete\n");
								actionResult = 1;
								mReadyToTakeoffResult = 0;
							} else if (mReadyToTakeoffResult == 2) {
								print.append("One-click flight preparation failure,alarm ").append(mAlarmCode).append("\n");
								actionResult = 2;
								mReadyToTakeoffResult = 0;
							}
							break;
					}

					if (HSSDKManager.getInstance().getMQTTServerConnectionStatus() == ConnStatus.DISCONNECTED) {
						print.append("MQTT server not connected,One-click flight preparation interrupted\n");
						actionResult = 2;
					}
					if (HSSDKManager.getInstance().getDeviceConnectionStatus() == ConnStatus.DISCONNECTED) {
						print.append("HeiSha device not connected,One-click flight preparation interrupted\n");
						actionResult = 2;
					}
					if (mReadyToTakeoffResult == 2) {
						print.append("One-click flight preparation failure,alarm ").append(mAlarmCode).append("\n");
						actionResult = 2;
						mReadyToTakeoffResult = 0;
					}

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mTxtInfo.setText(print.toString());
							mScrollInfo.fullScroll(View.FOCUS_DOWN);
						}
					});

					if (actionResult != 0) {
						this.cancel();
						mTimer.cancel();
						mTimer = null;
					}
				}
			};
			mTimer.schedule(timerTask, 1000, 500);
		}
	}

	private void startChargingImmediately() {
		if (mTimer == null) {
			mTimer = new Timer();
			TimerTask timerTask = new TimerTask() {
				StringBuilder print = new StringBuilder();
				int step = 0;
				int actionResult = 0; //0 Running, 1 Successful, 2 Failed

				@Override
				public void run() {
					switch(step) {
						case 0:
							if (HSSDKManager.getInstance().getMQTTServerConnectionStatus() == ConnStatus.CONNECTED) {
								if (HSSDKManager.getInstance().getDeviceConnectionStatus() == ConnStatus.CONNECTED) {
									mControlCenter.ShortcutChargingEnforcedly();
									print.append("One-click to charge starts\n");
									step++;
								} else {
									print.append("HeiSha device not connected,One-click to charge interrupted\n");
									actionResult = 2;
								}
							} else {
								print.append("MQTT server not connected,One-click to charge interrupted\n");
								actionResult = 2;
							}
							break;
						case 1:
							if (mAirConditioner.getConnectionState() == ConnStatus.CONNECTED) {
								print.append("A/C working mode：").
										append(mAirConditioner.getAirConditionerWorkingMode().toString()).append("\n");
								step++;
							} else {
								print.append("A/C module not connected,One-click to charge interrupted\n");
								actionResult = 2;
							}
							break;
						case 2:
							if (mPositionBar.getConnectionState() == ConnStatus.CONNECTED) {
								if (mPositionBar.getPositionBarState() == PositionBarState.POSITION_BAR_STATUS_LOCKED) {
									print.append("Charge bars locked\n");
									step++;
								}
							} else {
								print.append("Charge bars module not connected,One-click to charge interrupted\n");
								actionResult = 2;
							}
							break;
						case 3:
							if (mCanopy.getConnectionState() == ConnStatus.CONNECTED) {
								if (mCanopy.getCanopyState() == CanopyState.CANOPY_STATUS_CLOSE) {
									print.append("Canopy closed\n");
									step++;
								}
							} else {
								print.append("Canopy module not connected,One-click to charge interrupted\n");
								actionResult = 2;
							}
							break;
						case 4:
							if (mCharger.getConnectionStatus() == ConnStatus.CONNECTED) {
								if (mCharger.getBatteryManager().getBatteryDetectStatus() == BatteryDetectState.BATTERY_DETECT_YES) {
									print.append("Drone detected\n");
									step++;
								}
							} else {
								print.append("Charge control module not connected,One-click to charge interrupted\n");
								actionResult = 2;
							}
							break;
						case 5:
							if (mCharger.getConnectionStatus() == ConnStatus.CONNECTED) {
								if (mCharger.getDroneSwitch().getDroneStatus() == DroneSwitchState.DRONE_STATUS_POWER_OFF) {
									print.append("Drone powered off\n");
									step++;
								}
							} else {
								print.append("Charge control module not connected,One-click to charge interrupted\n");
								actionResult = 2;
							}
							break;
						case 6:
							if (mRemoteControl.getConnectionStatus() == ConnStatus.CONNECTED) {
								if (mRemoteControl.getRCPowerState() == PowerState.POWER_OFF) {
									print.append("Remote control powered off\n");
									step++;
								}
							} else {
								print.append("Remote control module not connected,One-click to charge interrupted\n");
								actionResult = 2;
							}
							break;
						case 7:
							if (mCharger.getConnectionStatus() == ConnStatus.CONNECTED) {
								if (mCharger.getChargingStatus() != ChargeState.CHARGE_STATUS_UNCHARGE &&
										mCharger.getChargingStatus() != ChargeState.CHARGE_STATUS_FAULT) {
									print.append("Charging...\n");
									step++;
								}
							} else {
								print.append("Charge control module not connected,One-click to charge interrupted\n");
								actionResult = 2;
							}
							break;
						case 8:
							if (mChargingImmediatelyResult == 2) {
								print.append("One-click to charge failure,alarm ").append(mAlarmCode).append("\n");
								actionResult = 2;
								mChargingImmediatelyResult = 0;
							} else {
								mControlCenter.getConfigParameter(ConfigParameter.SERVICE_PARAM_AIR_ROOM_MINTEM);
								step++;
							}
							break;
						case 9:
							print.append("Heating starts at ").append(mHeatingTempweature).append("℃\n");
							mControlCenter.getConfigParameter(ConfigParameter.SERVICE_PARAM_AIR_ROOM_MAXTEM);
							step++;
							break;
						case 10:
							print.append("Cooling starts at ").append(mCoolingTempweature).append("℃\n");
							step++;
							break;
						case 11:
							print.append("Internal temperature ").append(mAirConditioner.getCabinTemperature()).append("℃\n");
							step++;
							break;
						case 12:
							print.append("Battery temperature ").append(mCharger.getBatteryManager().getBatteryTemperature()).append("℃\n");
							step++;
							break;
						case 13:
							print.append("A/C working mode：").
									append(mAirConditioner.getAirConditionerWorkingMode().toString()).append("\n");
							step++;
							break;
						case 14:
							if (mChargingImmediatelyResult == 1) {
								print.append("One-click to charge complete\n");
								actionResult = 1;
								mChargingImmediatelyResult = 0;
							}
							break;
					}

					if (HSSDKManager.getInstance().getMQTTServerConnectionStatus() == ConnStatus.DISCONNECTED) {
						print.append("MQTT server not connected,One-click to charge interrupted\n");
						actionResult = 2;
					}
					if (HSSDKManager.getInstance().getDeviceConnectionStatus() == ConnStatus.DISCONNECTED) {
						print.append("HeiSha device not connected,One-click to charge interrupted\n");
						actionResult = 2;
					}
					if (mChargingImmediatelyResult == 2) {
						print.append("One-click to charge failure,alarm ").append(mAlarmCode).append("\n");
						actionResult = 2;
						mChargingImmediatelyResult = 0;
					}

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mTxtInfo.setText(print.toString());
							mScrollInfo.fullScroll(View.FOCUS_DOWN);
						}
					});

					if (actionResult != 0) {
						this.cancel();
						mTimer.cancel();
						mTimer = null;
					}
				}
			};
			mTimer.schedule(timerTask, 1000, 500);
		}
	}
}