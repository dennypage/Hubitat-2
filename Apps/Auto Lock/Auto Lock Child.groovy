/* 
 *   Hubitat Import URL: https://raw.githubusercontent.com/heidrickla/Hubitat/Apps/Auto%20Lock/Auto%20Lock%20Child.groovy
 *
 *   Author Chris Sader, modified by Lewis Heidrick with permission from Chris to takeover the project.
 *   
 *   Pending - 
 */
import groovy.transform.Field
import hubitat.helper.RMUtils

def setVersion() {
    state.name = "Auto Lock"
	state.version = "1.1.52"
}

definition(
    name: "Auto Lock Child",
    namespace: "heidrickla",
    author: "Lewis Heidrick",
    description: "Automatically locks a specific door after X minutes/seconds when closed and unlocks it when open.",
    category: "Convenience",
    parent: "heidrickla:Auto Lock",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/heidrickla/Hubitat/Apps/Auto%20Lock/Auto%20Lock%20Child.groovy")

preferences {
    page(name: "mainPage")
    page(name: "timeIntervalInput", title: "Only during a certain time") {
        section {
            input "starting", "time", title: "Starting", required: false
            input "ending", "time", title: "Ending", required: false
        }
    }
}

def mainPage() {
    dynamicPage(name: "mainPage", install: true, uninstall: true, refreshInterval:0) {
        ifTrace("mainPage")
        turnOffLoggingTogglesIn30()
        setPauseButtonName()

    section("") {
      input name: "Pause", type: "button", title: state.pauseButtonName, submitOnChange:true
      input "detailedInstructions", "bool", title: "Enable detailed instructions?", submitOnChange: true, required: false, defaultValue: false
      input name: "Lock", type: "button", title: "Lock", submitOnChange:true
      input name: "Unlock", type: "button", title: "Unlock", submitOnChange:true
    }
    section("") {
        if ((state.thisName == null) || (state.thisName == "null <span style=color:white> </span>")) {state.thisName = "Enter a name for this app."}
        input name: "thisName", type: "text", title: "", required:true, submitOnChange:true, defaultValue: "Enter a name for this app."
        state.thisName = thisName
        updateLabel()
    }
    section("") {
        if (detailedInstructions == true) {paragraph "This option performs an immediate update to the current status of the Lock, Contact Sensor, Presence Sensor, and Status of the application.  It will automatically reset back to off after activated."}
        input "refresh", "bool", title: "Click here to refresh the device status", submitOnChange: true, required: false
        app.updateSetting("refresh",[value:"false",type:"bool"])
        if (detailedInstructions == true) {paragraph "This is the lock that all actions will activate against. The app watches for locked or unlocked status sent from the device.  If it cannot determine the current status, the last known status of the lock will be used.  If there is not a last status available and State sync fix is enabled it will attempt to determine its' state, otherwise it will default to a space. Once a device is selected, the current status will appear on the device.  The status can be updated by refreshing the page or clicking the refresh status toggle."}
        input "lock1", "capability.lock", title: "Lock: ${state.lock1LockStatus} ${state.lock1BatteryStatus}", submitOnChange: true, required: true
    }
    section(title: "Locking Options:", hideable: true, hidden: hideLockOptionsSection()) {
        if (detailedInstructions == true) {paragraph "This is the contact sensor that will be used to determine if the door is open.  The lock will not lock while the door is open.  If it does become locked and Bolt/Frame strike protection is enabled, it will immediately try to unlock to keep from hitting the bolt against the frame. If you are having issues with your contact sensor or do not use one, it is recommended to disable Bolt/frame strike protection as it will interfere with the operation of the lock."}
        if (settings.whenToLock?.contains("1")) {input "contact", "capability.contactSensor", title: "Door: ${state.contactContactStatus} ${state.contactBatteryStatus}", submitOnChange: true, required: false}
        if (detailedInstructions == true) {paragraph "This is the presence sensor that will be used to lock when presence is not present.  If using the combined presence feature then the lock will lock once all sensors have departed."}
        if (settings.whenToLock?.contains("2")) {input "lockPresence", "capability.presenceSensor", title: "Presence: ${state.lockPresenceStatus} ${state.lockPresenceBatteryStatus}", submitOnChange: true, required: false, multiple: true}
        if (settings.whenToLock?.contains("4")) {input "deviceActivationSwitch", "capability.switch", title: "Switch Triggered Action: ${state.deviceActivationSwitchStatus}", submitOnChange: true, required: false, multiple: false}
        if (settings.whenToLock?.contains("4")) {input "deviceActivationToggle", "bool", title: "Invert Switch Triggered Action: ", submitOnChange: true, required: false, multiple: false, defaultValue: false}
        input "whenToLock", "enum", title: "When to lock?  Default: '(Lock when lock unlocks)'", options: whenToLockOptions, defaultValue: ["0"], required: true, multiple: true, submitOnChange:true
        if (detailedInstructions == true) {paragraph "Use seconds instead changes the timer used in the application to determine if the delay before performing locking actions will be based on minutes or seconds.  This will update the label on the next option to show its' setting."}
        if (settings.whenToLock?.contains("7")) {input name: "modesLockStatus", type: "mode", title: "Lock when entering these modes",required: false, multiple: true, submitOnChange: true}
        if (settings.whenToLock?.contains("7")) {input name: "enablePerModeLockDelay", type: "bool", title: "Enable per mode lock delay",required: false, defaultValue: false, submitOnChange: true
            if (enablePerModeLockDelay == true) {input "minSecLock", "bool", title: "Use seconds instead?", submitOnChange:true, required: true, defaultValue: false
                perModeLockDelay()
            } else if (enablePerModeLockDelay != true) {input "minSecLock", "bool", title: "Use seconds instead?", submitOnChange:true, required: true, defaultValue: false
                if (minSecLock == false) {input "durationLock", "number", title: "Lock it how many minutes later?", submitOnChange: false, required: true, defaultValue: 2, range: "1..84600"}
                if (minSecLock == true) {input "durationLock", "number", title: "Lock it how many seconds later?", submitOnChange: false, required: true, defaultValue: 5, range: "1..84600"}
            }
        }
        if (!settings.whenToLock?.contains("6")) {input "minSecLock", "bool", title: "Use seconds instead?", submitOnChange:true, required: true, defaultValue: false}
        if (!settings.whenToLock?.contains("6") && (detailedInstructions == true)) {paragraph "This value is used to determine the delay before locking actions occur. The minutes/seconds are determined by the Use seconds instead toggle."}
        if (!settings.whenToLock?.contains("6") && (minSecLock == false)) {input "durationLock", "number", title: "Lock it how many minutes later?", required: true, submitOnChange: true, defaultValue: 10, range: "1..84600"}
        if (!settings.whenToLock?.contains("6") && (minSecLock == true)) {input "durationLock", "number", title: "Lock it how many seconds later?", required: true, submitOnChange: true, defaultValue: 10, range: "1..84600"}
        if ((retryLock == true) && (detailedInstructions == true)) {paragraph "Enable retries if lock fails to change state enables all actions that try to lock the door up to the maximum number of retries.  If all retry attempts fail, a failure notice will appear in the logs.  Turning this toggle off causes any value in the Maximum number of retries to be ignored."}
        input "retryLock", "bool", title: "Enable retries if lock fails to change state.", required: false, submitOnChange: true, defaultValue: true
        if (detailedInstructions == true) {paragraph "Maximum number of retries is used to determine the limit of times that a locking action can attempt to perform an action.  This option is to prevent the lock from attempting over and over until the batteries are drained."}
        if (retryLock == true) {input "maxRetriesLock", "number", title: "Maximum number of retries?", required: false, submitOnChange: false, defaultValue: 3, range: "1..99"}
        if ((retryLock == true) && (detailedInstructions == true)) {paragraph "Delay between retries in second(s) provides the lock enough time to perform the locking action.  If you set this too low  and it send commands to the lock before it completes its' action, the commands will be ignored.  Three to five seconds is usually enough time for the lock to perform any actions and report back its' status."}
        if (retryLock == true) {input "delayBetweenRetriesLock", "number", title: "Delay between retries in second(s)?", require: false, submitOnChange: false, defaultValue: 5, range: "1..84600"}
        if (settings.whenToLock?.contains("5")) {input "hsmLockStatus","enum", title: "Lock when HSM enters these modes",required: false, multiple: true, submitOnChange: false, options: hsmStateOptions}
        if (enableHSMToggle == true) {input "hsmCommandsLock","enum", title: "Set HSM status when Locked?",required: false, multiple: false, submitOnChange: false, options: hsmCommandOptions}
    }
    section(title: "Unlocking Options:", hideable: true, hidden: hideUnlockOptionsSection()) {
        if (detailedInstructions == true) {if (settings.whenToUnlock?.contains("2")) {paragraph "This sensor is used for presence unlock triggers."}}
        if (settings.whenToUnlock?.contains("2")) {input "unlockPresence", "capability.presenceSensor", title: "Presence: ${state.unlockPresenceStatus} ${state.unlockPresenceBatteryStatus}", submitOnChange: true, required: false, multiple: true}
        if (settings.whenToUnlock?.contains("3")) {input "fireMedical", "capability.smokeDetector", title: "Fire/Medical: ${state.fireMedicalStatus} ${state.fireMedicalBatteryStatus}", submitOnChange: true, required: false, multiple: false}
        if (settings.whenToUnlock?.contains("4")) {input "deviceActivationSwitch", "capability.switch", title: "Switch Triggered Action: ${state.deviceActivationSwitchStatus}", submitOnChange: true, required: false, multiple: false}
        if (settings.whenToUnlock?.contains("4")) {input "deviceActivationToggle", "bool", title: "Invert Switch Triggered Action: ", submitOnChange: true, required: false, multiple: false, defaultValue: false}
        if (detailedInstructions == true) {paragraph "Bolt/Frame strike protection detects when the lock is locked and the door is open and immediately unlocks it to prevent it striking the frame.  This special case uses a modified delay timer that ignores the Unlock it how many minutes/seconds later and Delay between retries option.  It does obey the Maximum number of retries though."}
        if (detailedInstructions == true) {paragraph "Presence detection uses the selected presence device(s) and on arrival will unlock the door.  It is recommended to use a combined presence app to prevent false triggers.  I recommend Presence Plus and Life360 with States by BPTWorld, and the iPhone Presence driver (it works on android too).  You might need to mess around with battery optimization options to get presence apps to work reliably on your phone though."}
        if (detailedInstructions == true) {paragraph "Fire/Medical panic unlock will unlock the door whenever a specific sensor is opened.  I have zones on my alarm that trip open if one of these alarms are triggered and use an Envisalink 4 to bring over the zones into Hubitat. They show up as contact sensors.  If you have wired smoke detectors to your alarm panel, these are typically on zone 1.  You could use any sensor though to trigger."}
        if (detailedInstructions == true) {paragraph "Switch triggered unlock lets you trigger a lock or an unlock with a switch.  You can use the Invert Switch Triggered Action to flip the trigger logic to when the switch is on or off. This is different from the Switch to enable and disable option as it is used to lock and unlock the door."}
        if (detailedInstructions == true) {paragraph "State sync fix is used when the lock is locked but the door becomes opened.  Since this shouldn't happen it immediately unlocks the lock and tries to refresh the lock if successful it updates the app status.  If the unlock attempt fails, it then will attempt to retry and follows any unlock delays or retry restrictions.  This option allows you to use the lock and unlock functionality and still be able to use the app when you experience sensor problems by disabling this option."}
        if (detailedInstructions == true) {paragraph "Prevent unlocking under any circumstances is used when you want to disable all unlock functionality in the app. It overrides all unlock settings including Fire/Medical panic unlock."}
        input "whenToUnlock", "enum", title: "When to unlock?  Default: '(Prevent unlocking under any circumstances)'", options: whenToUnlockOptions, defaultValue: ["6"], required: true, multiple: true, submitOnChange:true
        if (!settings.whenToUnlock?.contains("7")) {
        if (detailedInstructions == true) {paragraph "Use seconds instead changes the timer used in the application to determine if the delay before performing unlocking actions will be based on minutes or seconds. This will update the label on the next option to show its' setting."}
        if (settings.whenToUnlock?.contains("7")) {input name: "modesUnlockStatus", type: "mode", title: "Unlock when entering these modes",required: false, multiple: true, submitOnChange: true}
        if (settings.whenToUnlock?.contains("7")) {input name: "enablePerModeUnlockDelay", type: "bool", title: "Enable per mode unlock delay",required: false, defaultValue: false, submitOnChange: true
            if (enablePerModeUnlockDelay == true) {input "minSecUnlock", "bool", title: "Use seconds instead?", submitOnChange:true, required: true, defaultValue: false
                perModeUnlockDelay()
            } else if (enablePerModeUnlockDelay != true) {input "minSecUnlock", "bool", title: "Use seconds instead?", submitOnChange:true, required: true, defaultValue: false
                configureDelayUnlock()
                if (minSecUnlock == false) {input "durationUnlock", "number", title: "Unlock it how many minutes later?", submitOnChange: false, required: true, defaultValue: 1, range: "1..84600"}
                if (minSecUnlock == true) {input "durationUnlock", "number", title: "Unlock it how many seconds later?", submitOnChange: false, required: true, defaultValue: 1, range: "1..84600"}
            }
        }
        if (!settings.whenToUnlock?.contains("6")) {input "minSecUnlock", "bool", title: "Use seconds instead?", submitOnChange:true, required: true, defaultValue: true}
        if (!settings.whenToUnlock?.contains("6") && (detailedInstructions == true)) {paragraph "This value is used to determine the delay before unlocking actions occur. The minutes/seconds are determined by the Use seconds instead toggle."}
        if (!settings.whenToUnlock?.contains("6") && (minSecUnlock == false)) {input "durationUnlock", "number", title: "Unlock it how many minutes later?", submitOnChange: false, required: true, defaultValue: 1, range: "1..84600"}
        if (!settings.whenToUnlock?.contains("6") && (minSecUnlock == true)) {input "durationUnlock", "number", title: "Unlock it how many seconds later?", submitOnChange: false, required: true, defaultValue: 1, range: "1..84600"}
        if (!settings.whenToUnlock?.contains("6") && (retryUnlock == true) && (detailedInstructions == true)) {paragraph "Enable retries if unlock fails to change state enables all actions that try to unlock the door up to the maximum number of retries.  If all retry attempts fail, a failure notice will appear in the logs.  Turning this toggle off causes any value in the Maximum number of retries to be ignored."}
        if (!settings.whenToUnlock?.contains("6")) {input "retryUnlock", "bool", title: "Enable retries if unlock fails to change state.", submitOnChange: true, require: false, defaultValue: true}
        if (!settings.whenToUnlock?.contains("6") && (retryUnlock == true) && (detailedInstructions == true)) {paragraph "Maximum number of retries is used to determine the limit of times that an unlocking action can attempt to perform an action.  This option is to prevent the lock from attempting over and over until the batteries are drained."}
        if (!settings.whenToUnlock?.contains("6") && (retryUnlock == true)) {input "maxRetriesUnlock", "number", title: "Maximum number of retries? While door is open it will wait for it to close.", submitOnChange: false, required: false, defaultValue: 3, range: "1..99"}
        if (!settings.whenToUnlock?.contains("6") && (retryUnlock == true) && (detailedInstructions == true)) {paragraph "Delay between retries in second(s) provides the lock enough time to perform the unlocking action.  If you set this too low and it send commands to the lock before it completes its' action, the commands will be ignored.  Three to five seconds is usually enough time for the lock to perform any actions and report back its' status."}
        if (!settings.whenToUnlock?.contains("6") && (retryUnlock == true)) {input "delayBetweenRetriesUnlock", "number", title: "Delay between retries in second(s)?", submitOnChange: false, require: false, defaultValue: 5, range: "1..84600"}
        if (!settings.whenToUnlock?.contains("6") && settings.whenToUnlock?.contains("0")) {input "hsmUnlockStatus","enum", title: "Unlock when HSM enters these modes",required: false, multiple: true, submitOnChange: false, options: hsmStateOptions}
        if (!settings.whenToUnlock?.contains("6") && (enableHSMToggle == true)) {input "hsmCommandsUnlock","enum", title: "Set HSM when Unlocked?",required: false, multiple: false, submitOnChange: false, options: hsmCommandOptions}
        }
    }
    section(title: "Only Run When:", hideable: true, hidden: hideOptionsSection()) {
        def timeLabel = timeIntervalLabel()
        if (detailedInstructions == true) {paragraph "Switch to Enable and Disable this app prevents the app from performing any actions other than status updates for the lock and contact sensor state and battery state on the app page."}
        input "disabledSwitch", "capability.switch", title: "Switch to Enable and Disable this app ${state.disabledSwitchStatus}", submitOnChange: false, required: false, multiple: false
        if (detailedInstructions == true) {paragraph "Only during a certain time is used to restrict the app to running outside of the assigned times. You can use this to prevent false presence triggers while your sleeping from unlocking the door."}
        href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
        if (detailedInstructions == true) {paragraph "Only on certain days of the week restricts the app from running outside of the assigned days. Useful if you work around the yard frequently on the weekends and want to keep your door unlocked and just want the app during the week."}
        input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false, submitOnChange: false, options: daysOptions
        if (detailedInstructions == true) {paragraph "Only when mode is allows you to prevent the app from running outside of the specified modes. This is useful if you have a party mode and want the lock from re-locking on you while company is over.  This could also be used like the Only during a certain time mode to prevent faluse triggers at night for instance."}
        input "modes", "mode", title: "Only when mode is", multiple: true, required: false, submitOnChange:false
        input "enableHSMToggle", "bool", title: "Enable HSM Actions", required:false, submitOnChange: true, defaultValue: false
        input "enableHSMSwitch", "capability.switch", title: "Switch to Enable HSM Actions: (Optional) ${state.enableHSMSwitchStatus}", required: false, multiple: false, submitOnChange: true
        if (enableHSMToggle == true) {input "whenToLockHSM", "enum", title: "Only lock when HSM status is?", options: hsmStateOptions, required: false, multiple: true, submitOnChange:false}
        if (enableHSMToggle == true) {input "whenToUnlockHSM", "enum", title: "Only unlock when HSM status is?", options: hsmStateOptions, required: false, multiple: true, submitOnChange:false}
    }
    section (title: "Notification Options:", hideable: true, hidden: hideNotificationSection()) {
        input "notifyOnEvent", "bool", title: "Enable Event Notifications?", submitOnChange: true, required:false, defaultValue: false
        if (notifyOnEvent == true) {input "eventNotificationDevices", "capability.notification", title: "Event Notification Devices:", submitOnChange: false, multiple: true, required: true}
        if (notifyOnEvent == true) {input "eventNotifications", "enum", title: "Select the types of events that you want to get notifications for:", required: true, multiple: true, submitOnChange: false, options: eventNotificationOptions}
        input "notifyOnLowBattery", "bool", title: "Enable Low Battery Notifications?", submitOnChange: true, required:false, defaultValue: false
        if (notifyOnLowBattery == true) {input "lowBatteryNotificationDevices", "capability.notification", title: "Low Battery Notification Devices:", submitOnChange: false, multiple: true, required: true}
        if (notifyOnLowBattery == true) {input "lowBatteryDevicesToNotifyFor", "enum", title: "Select devices that you want to get notifications for:", required: true, multiple: true, submitOnChange: false, options: lowBatteryNotificationOptions}
        if (notifyOnLowBattery == true) {input "lowBatteryAlertThreshold", "number", title: "Below what percentage do you want to be notified?", submitOnChange: false, required:true, defaultValue: 30}
        input "notifyOnFailure", "bool", title: "Enable Failure Notifications?", submitOnChange: true, required: false, defaultValue: false
        if (notifyOnFailure == true) {input "failureNotificationDevices", "capability.notification", title: "Failure Notification Devices:", submitOnChange: false, multiple: true, required: true}
        if (notifyOnFailure == true) {input "failureNotifications", "enum", title: "Failure Notifications:", required: true, multiple: true, submitOnChange: false, options: failureNotificationOptions}
    }
    section(title: "Logging Options:", hideable: true, hidden: hideLoggingSection()) {
        if (detailedInstructions == true) {paragraph "Enable Info logging for 30 minutes will enable info logs to show up in the Hubitat logs for 30 minutes after which it will turn them off. Useful for checking if the app is performing actions as expected."}
        input "isInfo", "bool", title: "Enable Info logging for 30 minutes", submitOnChange: false, required:false, defaultValue: false
        if (detailedInstructions == true) {paragraph "Enable Debug logging for 30 minutes will enable debug logs to show up in the Hubitat logs for 30 minutes after which it will turn them off. Useful for troubleshooting problems."}
        input "isDebug", "bool", title: "Enable debug logging for 30 minutes", submitOnChange: false, required:false, defaultValue: false
        if (detailedInstructions == true) {paragraph "Enable Trace logging for 30 minutes will enable trace logs to show up in the Hubitat logs for 30 minutes after which it will turn them off. Useful for following the logic inside the application but usually not neccesary."}
        input "isTrace", "bool", title: "Enable Trace logging for 30 minutes", submitOnChange: false, required:false, defaultValue: false
        if (detailedInstructions == true) {paragraph "Logging level is used to permanantly set your logging level for the application.  If it is set higher than any temporary logging options you enable, it will override them.  If it is set lower than temporary logging options, they will take priority until their timer expires.  This is useful if you prefer you logging set to a low level and then can use the logging toggles for specific use cases so you dont have to remember to go back in and change them later.  It's also useful if you are experiencing issues and need higher logging enabled for longer than 30 minutes."}
        input "ifLevel","enum", title: "Logging level", required: false, multiple: true, submitOnChange: false, options: logLevelOptions
        if (enableHSMToggle == true) {input "isHSM", "bool", title: "Enable HSM logging", submitOnChange: true, required:true, defaultValue: false}
        if ((enableHSMToggle == true) && (isHSM == true)) {input "hsmLogLevel","enum", title: "Show HSM Alerts in log as?", required: false, multiple: false, options: logLevelOptions}
    }
    displayFooter()
    }
}

// Application settings and startup
@Field static List<Map<String,String>> whenToLockOptions = [
    ["0": "Lock when lock unlocks"],
    ["1": "Lock when contact closes"],
    ["2": "Presence departure lock"],
    ["7": "Lock with Modes"],
    ["4": "Switch triggered lock"],
    ["5": "Lock with HSM"],
    ["6": "Prevent locking under any circumstances"]
]

@Field static List<Map<String,String>> whenToUnlockOptions = [
    ["1": "Bolt/frame strike protection"],
    ["8": "Attempt to clear lock jams"],
    ["2": "Presence unlock"],
    ["3": "Fire/medical panic unlock"],
    ["7": "Unlock with Modes"],
    ["4": "Switch triggered unlock"],
    ["5": "State sync fix"],
    ["0": "Unlock with HSM"],
    ["6": "Prevent unlocking under any circumstances"]
]

@Field static List<Map<String>> daysOptions = ["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"]

@Field static List<Map<String,String>> logLevelOptions = [
    ["0": "None"],
    ["1": "Info"],
    ["2": "Debug"],
    ["3": "Trace"]
]

@Field static List<Map<String,String>> hsmCommandOptions = [
    ["armAway": "Arm Away"],
    ["armHome": "Arm Home"],
    ["armNight": "Arm Night"],
    ["disarm": "Disarm"],
    ["armRules": "Arm Rules"],
    ["disarmRules": "Disarm Rules"],
    ["disarmAll": "Disarm All"],
    ["armAll": "Arm All"],
    ["cancelAlerts": "Cancel Alerts"]
]

@Field static List<Map<String,String>> hsmStateOptions = [
    ["armedAway": "Armed Away"],
    ["armedHome": "Armed Home"],
    ["armedNight": "Armed Night"],
    ["disarmed": "Disarmed"]
]

@Field static List<Map<String,String>> eventNotificationOptions = [
    ["1": "Lock Physical"],
    ["2": "Unlock Physical"],
    ["3": "Lock Digital"],
    ["4": "Unlock Digital"],
    ["5": "Fire/Medical Panic Triggered"],
    ["6": "Presence Arrival Unlock"],
    ["7": "Presence Departure Lock"],
    ["8": "Switch Triggered Lock"],
    ["9": "Switch Triggered Unlock"]
]

@Field static List<Map<String,String>> lowBatteryNotificationOptions = [
    ["1": "Lock"],
    ["2": "Contact Sensor"],
    ["3": "Smoke Detector"]
//WIP    ["4": "Lock Presence Sensor"],
//WIP    ["5": "Unlock Presence Sensor"]
]

@Field static List<Map<String,String>> failureNotificationOptions = [
    ["0": "Lock Jammed"],
    ["1": "Max lock retries exceeded"],
    ["2": "Max unlock retries exceeded"],
    ["3": "Fire/Medical panic triggered unlock but application is paused or disabled"],
    ["4": "Switch triggered lock/unlock but application is paused or disabled"],
    ["5": "State sync fix triggered"],
    ["6": "Unlock triggered while Preventing unlock under any circumstances was enabled"]
]

def installed() {
    ifTrace("installed")
    ifDebug("Auto Lock Door installed.")
    state.installed = true
    if (state.lock1LockStatus == null) {state.lock1LockStatus = " "}
    if (state.lock1BatteryStatus == null) {state.lock1BatteryStatus = " "}
    if (state.contactContactStatus == null) {state.contactContactStatus = " "}
    if (state.contactBatteryStatus == null) {state.contactBatteryStatus = " "}
    if (state.lockPresenceStatus == null) {state.lockPresenceStatus = " "}
    if (state.lockPresenceBatteryStatus == null) {state.lockPresenceBatteryStatus = " "}
    if (state.unlockPresenceStatus == null) {state.unlockPresenceStatus = " "}
    if (state.unlockPresenceBatteryStatus == null) {state.unlockPresenceBatteryStatus = " "}
    if (state.fireMedicalStatus == null) {state.fireMedicalStatus = " "}
    if (state.fireMedicalBatteryStatus == null) {state.fireMedicalBatteryStatus = " "}
    if (state.deviceActivationSwitchStatus == null) {state.deviceActivationSwitchStatus = " "}
    if (state.disabledSwitchStatus == null) {state.disabledSwitchStatus = " "}
    if (state.enableHSMSwitchStatus == null) {state.enableHSMSwitchStatus = " "}
    initialize()
}

def updated() {
    ifTrace("updated")
    ifDebug("Settings: ${settings}")
    if (state?.installed == null) {
		state.installed = true
	}
    if (state.lock1LockStatus == null) {state.lock1LockStatus = " "}
    if (state.lock1BatteryStatus == null) {state.lock1BatteryStatus = " "}
    if (state.contactContactStatus == null) {state.contactContactStatus = " "}
    if (state.contactBatteryStatus == null) {state.contactBatteryStatus = " "}
    if (state.lockPresenceStatus == null) {state.lockPresenceStatus = " "}
    if (state.lockPresenceBatteryStatus == null) {state.lockPresenceBatteryStatus = " "}
    if (state.unlockPresenceStatus == null) {state.unlockPresenceStatus = " "}
    if (state.unlockPresenceBatteryStatus == null) {state.unlockPresenceBatteryStatus = " "}
    if (state.fireMedicalStatus == null) {state.fireMedicalStatus = " "}
    if (state.fireMedicalBatteryStatus == null) {state.fireMedicalBatteryStatus = " "}
    if (state.deviceActivationSwitchStatus == null) {state.deviceActivationSwitchStatus = " "}
    if (disabledSwitch?.currentValue("switch") == null) {state.disabledSwitchStatus = " "}
    if (state.disabledSwitchStatus == null) {state.disabledSwitchStatus = " "}
    if (state.enableHSMSwitchStatus == null) {state.enableHSMSwitchStatus = " "}
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    ifTrace("initialize")
    ifDebug("Settings: ${settings}")
    if (!settings.whenToLock?.contains("6")) {configureCountLock()}
    if (!settings.whenToUnlock?.contains("6")) {configureCountUnlock()}
    if (!settings.whenToLock?.contains("6")) {configureDelayLock()}
    if (settings.whenToUnlock?.contains("6")) {configureDelayUnlock()}
    if (maxRetriesLock != null) {atomicState.countLock = maxRetriesLock} else {(atomicState.countLock = 0)}
	if (maxRetriesUnlock != null) {atomicState.countUnlock = maxRetriesUnlock} else {(atomicState.countUnlock = 0)}
    if ((atomicState.countUnlock == -99) && (maxRetriesUnlock != null)) {atomicState.countUnlock = maxRetriesUnlock}
    subscribe(disabledSwitch, "switch.on", disabledHandler)
    subscribe(disabledSwitch, "switch.off", disabledHandler)
    subscribe(deviceActivationSwitch, "switch.on", deviceActivationSwitchHandler)
    subscribe(deviceActivationSwitch, "switch.off", deviceActivationSwitchHandler)
    subscribe(deviceActivationToggle, "switch", deviceActivationToggleHandler)
    if (settings.whenToUnlock?.contains("3")) {subscribe(fireMedical, "contact.open", fireMedicalHandler)}
    if (settings.whenToUnlock?.contains("3")) {subscribe(fireMedical, "contact.closed", fireMedicalHandler)}
    if (settings.whenToUnlock?.contains("3")) {subscribe(fireMedical, "battery", fireMedicalBatteryHandler)}
    subscribe(lock1, "lock.locked", lock1LockHandler)
    subscribe(lock1, "lock.unlocked", lock1UnlockHandler)
    subscribe(lock1, "battery", lock1BatteryHandler)
    subscribe(lock1, "lock.unknown", lock1JammedHandler)
    subscribe(contact, "contact.open", contactOpenHandler)
    subscribe(contact, "contact.closed", contactClosedHandler)
    subscribe(contact, "battery", contactBatteryHandler)
    if (settings.whenToLock?.contains("2")) {subscribe(lockPresence, "presence.not present", lockPresenceHandler)}
    if (settings.whenToLock?.contains("2")) {subscribe(lockPresence, "battery", lockPresenceBatteryHandler)}
    if (settings.whenToUnlock?.contains("2")) {subscribe(unlockPresence, "presence.present", unlockPresenceHandler)}
    if (settings.whenToUnlock?.contains("2")) {subscribe(unlockPresence, "battery", unlockPresenceBatteryHandler)}
    subscribe(enableHSMSwitch, "switch", enableHSMHandler)
    if (settings.whenToLock?.contains("7") || settings.whenToUnlock?.contains("7")) {subscribe(location, "mode", modeHandler)}
    if (((whenToLockHSM || whenToUnlockHSM) && enableHSMActions) || (settings.whenToLock?.contains("5") && (hsmLockStatus)) || (settings.whenToUnlock?.contains("0") && (hsmUnlockStatus))) {subscribe(location, "hsmStatus", hsmStatusHandler)}   //For app to subscribe to HSM status
    if ((whenToLockHSM || whenToUnlockHSM) && enableHSMActions) {subscribe(location, "hsmAlerts", hsmAlertHandler)}    //For app to subscribe to HSM alerts
    turnOffLoggingTogglesIn30()
    getAllOk()
}

// Device Handlers
def modeHandler(evt) {
    // HSM Status Handler Action
    ifTrace("modeHandler: ${evt.value}")
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {
        ifTrace("modeHandler: Application is paused or disabled.")
    } else if (!settings.whenToLock?.contains("6") && settings.whenToLock?.contains("7") && modesLockStatus?.contains(location.mode) && (lock1?.currentValue("lock") == "unlocked") && ((contact?.currentValue("contact") == "closed") || (contact == null))) {
        if (settings.whenToLock?.contains("7") && (enablePerModeLockDelay == true)) {
            modesLockStatus.each { it ->
                if (it == location.mode) {
                    app.updateSetting("durationLock",[value: "${it}",type: "number"])
                }
            }
        }
        lockDoor()
    } else if (!settings.whenToUnlock?.contains("6") && settings.whenToUnlock?.contains("7") && modesUnlockStatus?.contains(location.mode) && (lock1?.currentValue("lock") == "locked")) {
        unlockDoor()
    }
}


def hsmAlertHandler(evt) {
    // HSM Alert Handler Action
    ifTrace("hsmAlertHandler: ${evt.value}")
    if (hsmLogLevel == "1") {log.info "HSM Alert: $evt.value" + (evt.value == "rule" ? ",  $evt.descriptionText" : "")}
    else if (hsmLogLevel == "2") {log.debug "HSM Alert: $evt.value" + (evt.value == "rule" ? ",  $evt.descriptionText" : "")}
    else if (hsmLogLevel == "3") {log.trace "HSM Alert: $evt.value" + (evt.value == "rule" ? ",  $evt.descriptionText" : "")}
    else {}
//    only has descriptionText for rule alert
}

def hsmStatusHandler(evt) {
    // HSM Status Handler Action
    ifTrace("hsmStatusHandler: ${evt.value}")
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {ifTrace("hsmStatusHandler: Application is paused or disabled.")
    } else if (!settings.whenToLock?.contains("6") && settings.whenToLock?.contains("5") && hsmLockStatus?.contains(location.hsmStatus) && (lock1?.currentValue("lock") == "unlocked") && ((contact?.currentValue("contact") == "closed") || (contact == null))) {lockDoor()
    } else if (!settings.whenToUnlock?.contains("6") && settings.whenToUnlock?.contains("0") && hsmUnlockStatus?.contains(location.hsmStatus) && (lock1?.currentValue("lock") == "locked")) {unlockDoor()}
    if (((enableHSMToggle == true) && (enableHSMSwitch?.currentValue("switch") == "on")) && (hsmCommandsLock != null)) {sendLocationEvent(name: "hsmSetArm", value: "${hsmCommandsLock}")}
}

def enableHSMHandler(evt) {
    // Device Status
    ifTrace("enableHSMHandler: ${evt.value}")
    if (evt.value == "on") {app.updateSetting("enableHSMToggle",[value:"true",type:"bool"])
    } else if (evt.value == "off") {app.updateSetting("enableHSMToggle",[value:"false",type:"bool"])}

    // Device Handler Action
    
}

def lock1LockHandler(evt) {
    // Device Status
    ifTrace("lock1LockHandler: ${evt.value}")
    if (evt.value) {state.lock1LockStatus = "[${evt.value}]"
    } else if (lock1?.currentValue("lock") != null) {state.lock1LockStatus = "[${lock1.currentValue("lock")}]"
    } else if (lock1?.latestValue("lock") != null) {state.lock1LockStatus = "[${lock1.latestValue("lock")}]"
    } else if (state?.lock1LockStatus == null) {state.lock1LockStatus = " "
        log.warn "${evt.value}"
    }
    
    // Send Lock Notification
    ifDebug("${evt.descriptionText}")
    if ((evt.type == 'physical') && evt.descriptionText.contains(' locked') && settings.eventNotifications?.contains("1")) {sendEventNotification("${evt.descriptionText}")
    } else if ((evt.type == 'digital') && evt.descriptionText.contains(' locked') && settings.eventNotifications?.contains("3")) {sendEventNotification("${evt.descriptionText}")
    } else if (evt.descriptionText.contains('physical') && evt.descriptionText.contains(' locked') && settings.eventNotifications?.contains("1")) {sendEventNotification("${evt.descriptionText}")
    } else if (evt.descriptionText.contains('digital') && evt.descriptionText.contains(' locked') && settings.eventNotifications?.contains("3")) {sendEventNotification("${evt.descriptionText}")}
    updateLabel()
    
    // Device Handler Action
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {
        ifTrace("lock1LockHandler: Application is paused or disabled.")
    } else if (settings.whenToUnlock?.contains("6")) {
    // Unlocking is disabled. Doing nothing.
    } else if (settings.whenToUnlock?.contains("1") && (contact?.currentValue("contact") == "open")) {
        ifDebug("lock1LockHandler:  Lock was locked while Door was open. Performing a fast unlock to prevent hitting the bolt against the frame.")
        unscheduleLockCommands()
        lock1Unlock()
        state.delayUnlock = 1
        runIn(state.delayUnlock, unlockDoor, [data: maxRetriesUnlock])
    } else if (settings.whenToUnlock?.contains("3") && (fireMedical?.currentValue("smokeSensor") == "detected")) {
        ifDebug("lock1LockHandler: Lock was locked while the Fire/Medical Sensor detected smoke. Performing a fast unlock.")
        unscheduleLockCommands()
        lock1Unlock()
        state.delayUnlock = 1
        runIn(state.delayUnlock, unlockDoor, [data: maxRetriesUnlock])
    } else if ((lock1?.currentValue("lock") == "locked") && (contact?.currentValue("contact") == "closed") || (contact == null)) {
        unscheduleLockCommands()                  // ...we don't need to lock it later.
    }
}

def lock1UnlockHandler(evt) {
    // Device Status
    ifTrace("lock1UnlockHandler: ${evt.value}")
    if (evt.value) {state.lock1LockStatus = "[${evt.value}]"
    } else if (lock1?.currentValue("lock") != null) {state.lock1LockStatus = "[${lock1.currentValue("lock")}]"
    } else if (lock1?.latestValue("lock") != null) {state.lock1LockStatus = "[${lock1.latestValue("lock")}]"
    } else if (state?.lock1LockStatus == null) {state.lock1LockStatus = " "
        log.warn "${evt.value}"
    }
    
    // Send Unlock Notification
    ifDebug("${evt.descriptionText}")
    if ((evt.type == 'physical') && evt.descriptionText.contains('unlocked') && settings.eventNotifications?.contains("2")) {sendEventNotification("${evt.descriptionText}")
    } else if ((evt.type == 'digital') && evt.descriptionText.contains('unlocked') && settings.eventNotifications?.contains("4")) {sendEventNotification("${evt.descriptionText}")
    } else if (evt.descriptionText.contains('physical') && evt.descriptionText.contains('unlocked') && settings.eventNotifications?.contains("2")) {sendEventNotification("${evt.descriptionText}")
    } else if (evt.descriptionText.contains('digital') && evt.descriptionText.contains('unlocked') && settings.eventNotifications?.contains("4")) {sendEventNotification("${evt.descriptionText}")}
    updateLabel()
    
    // Device Handler Action
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {
        ifTrace("lock1UnlockHandler: Application is paused or disabled.")
    } else if (settings.whenToLock?.contains("6")) {
        // Locking is disabled. Doing nothing.
    } else if (settings.whenToUnlock?.contains("3") && (fireMedical?.currentValue("smokeSensor") == "detected")) {
        // Keeping door unlocked until the sensor clears.
        unscheduleLockCommands()
    } else if (settings.whenToLock?.contains("0") && (contact?.currentValue("contact") == "closed") || (contact == null)) {
        unscheduleLockCommands()
        if (maxRetriesLock != null) {atomicState.countLock = maxRetriesLock} else {(atomicState.countLock = 0)}
        lockDoor(maxRetriesLock)
    }
}

def lock1BatteryHandler(evt) {
    // Device Status
    ifTrace("lock1BatteryHandler: Battery: [${evt.value}%]")
    if (evt.value) {state.lock1BatteryStatus = "Battery: [${evt.value}%]"
    } else if (lock1?.currentBattery != null) {state.lock1BatteryStatus = "Battery: [${lock1.currentBattery}%]"
    } else if (lock1?.currentValue("battery") != null) {state.lock1BatteryStatus = "Battery: [${lock1.currentValue("battery")}%]"
    } else if (lock1?.latestValue("battery") != null) {state.lock1BatteryStatus = "Battery: [${lock1.latestValue("battery")}%]"
    } else if (state?.lock1BatteryStatus == null) {state.lock1BatteryStatus = " "
        log.warn "${evt.value}"
    }
    if (lowBatteryDevicesToNotifyFor?.contains("1") && (notifyOnLowBattery == true) && (lowBatteryAlertThreshold) && (evt.value.toDouble() < lowBatteryAlertThreshold.toDouble())) {sendEventNotification("${lock1} battery is ${evt.value}.")}

    // Device Handler Action
}

def lock1JammedHandler(evt) {
    if (evt.value) {
	if (settings.failureNotifications?.contains("0") && (lock1.currentValue("lock") == "unknown")) {sendEventNotification("Lock is possibly jammed.")}
        if (!settings.whenToUnlock?.contains("6") && settings.whenToUnlock?.contains("8") && (retryLock == true)) {unlockDoor()}
        if (!settings.whenToLock?.contains("6") && (retryLock == true)) {runIn(3, lockDoor)}
    }
}

def contactOpenHandler(evt) {
    // Device Status
    ifTrace("contactOpenHandler: ${evt.value}")
    if (evt.value) {state.contactContactStatus = "[${evt.value}]"
    } else if (contact?.currentValue("contact") != null) {state.contactContactStatus = "[${contact.currentValue("contact")}]"
    } else if (contact?.latestValue("contact") != null) {state.contactContactStatus = "[${contact.latestValue("contact")}]"
    } else if (state?.contactContactStatus == null) {state.contactContactStatus = " "
        log.warn "${evt.value}"
    }
    
    // Device Handler Action
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {
        ifTrace("contactOpenHandler: Application is paused or disabled.")
    } else if (!settings.whenToUnlock?.contains("6") && settings.whenToUnlock?.contains("3") && (fireMedical?.currentValue("smokeSensor") == "detected")) {
        unscheduleLockCommands()
        // Doing nothing until the sensor clears.
    } else if (!settings.whenToUnlock?.contains("6") && settings.whenToUnlock?.contains("5") && (lock1.currentValue("lock") == "locked")) {
        unscheduleLockCommands()
        ifDebug("Door was opened while lock was locked. Performing a fast unlock and device refresh to get current state.")
        ifTrace("contactOpenHandler: Door was opened while lock was locked. Performing a fast unlock in case and device refresh to get current state.")
        if (settings.failureNotifications?.contains("5")) {sendFailureNotification("State sync fix triggered")}
        lock1Unlock()
        if (maxRetriesLock != null) {atomicState.countLock = maxRetriesLock} else {(atomicState.countLock = 0)}
        runIn(1, unlockDoor, [data: maxRetriesUnlock])
        lock1.refresh()
    } else if (!settings.whenToLock?.contains("6") && settings.whenToLock?.contains("0") && settings.whenToLock?.contains("1")) {unscheduleLockCommands()}
}

def contactClosedHandler(evt) {
    // Device Status
    ifTrace("contactClosedHandler: ${evt.value}")
    if (evt.value) {state.contactContactStatus = "[${evt.value}]"
    } else if (contact?.currentValue("contact") != null) {state.contactContactStatus = "[${contact.currentValue("contact")}]"
    } else if (contact?.latestValue("contact") != null) {state.contactContactStatus = "[${contact.latestValue("contact")}]"
    } else if (state?.contactContactStatus == null) {state.contactContactStatus = " "
        log.warn "${evt.value}"
    }
    
    // Device Handler Action
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {
        ifTrace("contactContactHandler: Application is paused or disabled.")
    } else if (!settings.whenToUnlock?.contains("6") && settings.whenToUnlock?.contains("3") && (fireMedical?.currentValue("smokeSensor") == "detected")) {
        // Doing nothing until the sensor clears.
    } else if (!settings.whenToLock?.contains("6") && settings.whenToLock?.contains("1") && (lock1?.currentValue("lock") == "unlocked") && (contact?.currentValue("contact") == "closed")) {
        unscheduleLockCommands()
        if (maxRetriesLock != null) {atomicState.countLock = maxRetriesLock} else {(atomicState.countLock = 0)}
        lockDoor(maxRetriesLock)
    }
}

def contactBatteryHandler(evt) {
    // Device Status
    ifTrace("contactBatterytHandler: Battery: [${evt.value}%]")
    if (evt.value) {state.contactBatteryStatus = "Battery: [${evt.value}%]"
    } else if (contact?.currentBattery != null) {state.contactBatteryStatus = "Battery: [${contact.currentBattery}%]"
    } else if (contact?.currentValue("battery") != null) {state.contactBatteryStatus = "Battery: [${contact.currentValue("battery")}%]"
    } else if (contact?.latestValue("battery") != null) {state.contactBatteryStatus = "Battery: [${contact.latestValue("battery")}%]"
    } else if (state?.contactBatteryStatus == null) {state.contactBatteryStatus = " "
        log.warn "${evt.value}"
    }
    if (lowBatteryDevicesToNotifyFor?.contains("2") && (notifyOnLowBattery == true) && (lowBatteryAlertThreshold) && (evt.value.toDouble() < lowBatteryAlertThreshold.toDouble())) {sendEventNotification("${contact} battery is ${evt.value}.")}

    // Device Handler Action
}

def lockPresenceHandler(evt) {
    // Device Status
    ifTrace("lockPresenceHandler: ${evt.value}")
    if (evt.value) {state.unlockPresenceStatus = "[${evt.value}]"
    } else if (state?.unlockPresenceStatus == null) {(state?.lockPresenceStatus = " ")
        log.warn "${evt.value}"
    }
    
    // Device Handler Action
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {ifTrace("lockPresenceHanlder: Application is paused or disabled.")
    } else if (!settings.whenToLock?.contains("6") && settings.whenToLock?.contains("2") && (evt.value == "not present")) {
        if (settings.eventNotifications?.contains("7")) {sendEventNotification("Presence Departure Lock")}
        unscheduleLockCommands()
        if (maxRetriesLock != null) {atomicState.countLock = maxRetriesLock} else {(atomicState.countLock = 0)}
        lockDoor(maxRetriesLock)
    }
}

def lockPresenceBatteryHandler(evt) {
    // Device Status
    ifTrace("lockPresenceBatteryHandler: Battery: [${evt.value}%]")
    if (evt.value) {state.unlockPresenceBatteryStatus = "Battery: [${evt.value}%]"
    } else if (lockPresence?.currentBattery != null) {state.lockPresenceBatteryStatus = "Battery: [${lockPresence.currentBattery}]"
    } else if (state?.unlockPresenceBatteryStatus == null) {(state.unlockPresenceBatteryStatus = " ")
        log.warn "${evt.value}"
    }

    // Device Handler Action
//    if (lowBatteryDevicesToNotifyFor?.contains("1") && (notifyOnLowBattery == true) && (lowBatteryAlertThreshold) && (evt.value.toDouble() < lowBatteryAlertThreshold.toDouble())) {sendEventNotification("${lock1} battery is ${evt.value}.")}
}

def unlockPresenceHandler(evt) {
    // Device Status
    ifTrace("unlockPresenceHandler: ${evt.value}")
    if (evt.value) {state.unlockPresenceStatus = "[${evt.value}]"
    } else if (lockPresence?.currentValue("presence") != null) {state.lockPresenceStatus = " [${lockPresence.currentValue("presence")}]"
    } else if (state?.unlockPresenceStatus == null) {(state.lockPresenceStatus = " ")
        log.warn "${evt.value}"
    }
    
    // Device Handler Action
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {ifTrace("unlockPresenceHanlder: Application is paused or disabled.")
    } else if (!settings.whenToUnlock?.contains("6") && settings.whenToUnlock?.contains("2") && (evt.value == "present")) {
        if (settings.eventNotifications?.contains("6")) {sendEventNotification("Presence Arrival Unlock")}
        unscheduleLockCommands()
        if (maxRetriesUnlock != null) {atomicState.countUnlock = maxRetriesUnlock} else {(atomicState.countUnlock = 0)}
        unlockDoor(maxRetriesUnlock)
    }
}

def unlockPresenceBatteryHandler(evt) {
    // Device Status
    ifTrace("unlockPresenceBatteryHandler: Battery: [${evt.value}%]")
    if (evt.value) {state.unlockPresenceBatteryStatus = "Battery: [${evt.value}%]"    
    } else if (unlockPresence?.currentBattery != null) {state.unlockPresenceBatteryStatus = "Battery: [${unlockPresence.currentBattery}]"
    } else if (state?.unlockPresenceBatteryStatus == null) {(state.unlockPresenceBatteryStatus = " ")
        log.warn "${evt.value}"
    }
    // Device Handler Action
//    if (lowBatteryDevicesToNotifyFor?.contains("1") && (notifyOnLowBattery == true) && (lowBatteryAlertThreshold) && (evt.value.toDouble() < lowBatteryAlertThreshold.toDouble())) {sendEventNotification("${lock1} battery is ${evt.value}.")}
}

def fireMedicalHandler(evt) {
    // Device Status
    ifTrace("fireMedicalHandler: ${evt.value}")
    if (evt.value) {state.fireMedicalStatus = "[${evt.value}]"
    } else if (fireMedical?.currentValue("contact") != null) {state.fireMedicalStatus = "[${fireMedical.currentValue("contact")}]"
    } else if (fireMedical?.latestValue("contact") != null) {state.fireMedicalStatus = "[${fireMedical.latestValue("contact")}]"
    } else if (state?.fireMedicalStatus == null) {(state.fireMedicalStatus = " ")
        log.warn "${evt.value}"
    }
    
    // Device Handler Action
    if (evt.value != null) {ifTrace("fireMedicalHandler: ${evt.value}")}
    updateLabel()
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {
        ifTrace("fireMedicalHandler: Application is paused or disabled.")
        if (settings.failureNotifications?.contains("3")) {sendFailureNotification("Fire/Medical panic triggered unlock but application is paused or disabled")}
    } else if (settings.whenToUnlock?.contains("3") && !settings.whenToUnlock?.contains("6") && (fireMedical?.currentValue("contact") == "open")) {
        ifDebug("fireMedicalHandler:  Fast unlocking because of an emergency.")
        if (settings.eventNotifications?.contains("5")) {sendEventNotification("Fire/Medical Panic Triggered.")}
        unscheduleLockCommands()
        if (maxRetriesUnlock != null) {atomicState.countUnlock = maxRetriesUnlock} else {(atomicState.countUnlock = 0)}
        lock1Unlock()
        runIn(1, unlockDoor, [data: maxRetriesUnlock])
    } else if (lock1?.currentValue("lock") == "unlocked") {ifTrace("The door is open and the lock is unlocked. Nothing to do.")}
}

def fireMedicalBatteryHandler(evt) {
    // Device Status
    ifTrace("fireMedicalBatteryHandler: Battery: [${evt.value}%]")
    if (evt.value) {state.fireMedicalBatteryStatus = "[${evt.value}%]"
    } else if (fireMedical?.currentBattery != null) {state.fireMedicalBatteryStatus = "Battery: [${fireMedical.currentBattery}%]"
    } else if (fireMedical?.currentValue("battery") != null) {state.fireMedicalBatteryStatus = "Battery: [${fireMedical.currentValue("battery")}%]"
    } else if (fireMedical?.latestValue("battery") != null) {state.fireMedicalBatteryStatus = "Battery: [${fireMedical.latestValue("battery")}%]"
    } else if (state?.fireMedicalBatteryStatus == null) {(state.fireMedicalBatteryStatus = " ")
        log.warn "${evt.value}"
    }
    if (lowBatteryDevicesToNotifyFor?.contains("3") && (notifyOnLowBattery == true) && (lowBatteryAlertThreshold) && (evt.value.toDouble < lowBatteryAlertThreshold.toDouble())) {sendEventNotification("${fireMedical} battery is ${evt.value}.")}
    // Device Handler Action
}

def deviceActivationSwitchHandler(evt) {
    // Device Status
    ifTrace("deviceActivationSwitchHandler: ${evt.value}")
    if (evt.value) {state.deviceActivationSwitchStatus = "[${evt.value}]"
    } else if (deviceActivationSwitch?.currentValue("switch") != null) {state.deviceActivationSwitchStatus = "[${deviceActivationSwitch.currentValue("switch")}]"
    } else if (deviceActivationSwitch?.latestValue("switch") != null) {state.deviceActivationSwitchStatus = "[${deviceActivationSwitch.latestValue("switch")}]"
    } else if (state?.deviceActivationSwitchStatus == null) {(state.deviceActivationSwitchStatus = " ")}

    // Device Handler Action
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {
        ifTrace("deviceActivationSwitchHandler: Application is paused or disabled.")
        if (settings.failureNotifications?.contains("4")) {sendFailureNotification("Switch triggered lock/unlock but application is paused or disabled.")}
    } else if (deviceActivationSwitch) {
        deviceActivationSwitch.each { it ->
            state.deviceActivationSwitchState = it.currentValue("switch")
        }
        if (state.deviceActivationSwitchState == "on") {
            if ((deviceActivationToggle == true) && !settings.whenToUnlock?.contains("6") && settings.whenToUnlock?.contains("4")) {
                ifDebug("deviceActivationSwitchHandler: Unlocking the door")
                if (settings.eventNotifications?.contains("9")) {sendEventNotification("Switch Triggered Unlock")}
                unscheduleLockCommands()
                if (maxRetriesUnlock != null) {atomicState.countUnlock = maxRetriesUnlock} else {(atomicState.countUnlock = 0)}
                unlockDoor(maxRetriesUnlock)
            } else if (!settings.whenToLock?.contains("6") && settings.whenToLock?.contains("4")) {
                ifDebug("deviceActivationSwitchHandler: Locking the door")
                if (settings.eventNotifications?.contains("8")) {sendEventNotification("Switch Triggered Lock")}
                unscheduleLockCommands()
                if (maxRetriesLock != null) {atomicState.countLock = maxRetriesLock} else {(atomicState.countLock = 0)}
                lockDoor(maxRetriesLock)
            }
        } else if (state.deviceActivationSwitchState == "off") {
            if ((deviceActivationToggle == true) && !settings.whenToLock?.contains("6") && settings.whenToLock?.contains("4")){
                lockDoor()
            } else if (!settings.whenToUnlock?.contains("6") && settings.whenToUnlock?.contains("4")) {
                ifDebug("deviceActivationSwitchHandler: Unlocking the door now")
                if (settings.eventNotifications?.contains("9")) {sendEventNotification("Switch Triggered Unlock")}
                unscheduleLockCommands()
                if (maxRetriesUnlock != null) {atomicState.countUnlock = maxRetriesUnlock} else {(atomicState.countUnlock = 0)}
                unlockDoor(maxRetriesUnlock)
            }
        }
    }
}

def deviceActivationToggleHandler(evt) {
    // Toggle Status
    ifTrace("Action Toggled: ${evt.value}")
}

def disabledHandler(evt) {
    // Device Status
    ifTrace("disabledHandler: ${evt.value}")
    if (evt.value) {state.disabledSwitchStatus = "[${evt.value}]"
    } else if (disabledSwitch?.currentValue("switch") != null) {state.disabledSwitchStatus = "[${disabledSwitch.currentValue("switch")}]"
    } else if (disabledSwitch?.latestValue("switch") != null) {state.disabledSwitchStatus = "[${disabledSwitch.latestValue("switch")}]"
    } else if (state?.disabledSwitchStatus == null) {(state.disabledSwitchStatus = " ")}
    
    // Device Handler Action
    if (disabledSwitch) {
        disabledSwitch.each { it ->
        state.disabledSwitchState = it.currentValue("switch")
            if (state.disabledSwitchState == "on") {
                ifTrace("disabledHandler: Enabled by switch")
                state.paused = false
                state.disabled = false
                state.pausedOrDisabled = false
                if (!settings.whenToLock?.contains("6") && (lock1?.currentValue("lock") == "unlocked") && ((contact?.currentValue("contact") == "closed") || (contact == null))) {
                    if (maxRetriesLock != null) {atomicState.countLock = maxRetriesLock} else {(atomicState.countLock = 0)}
                    lockDoor()
                }
            } else if (state.disabledSwitchState == "off") {
                unscheduleLockCommands()
                state.pauseButtonName = "Disabled by Switch"
                state.status = "(Disabled)"
                ifTrace("disabledHandler: (Disabled)")
                state.disabled = true
            }
        }
    }
    updateLabel()
}

def enableHSMSwitchHandler(evt) {
    // Device Status
    ifTrace("enableHSMSwitchHandler: ${evt.value}")
    if (evt.value) {state.enableHSMSwitchStatus = "[${evt.value}]"
    } else if (enableHSMSwitch?.currentValue("switch") != null) {state.enableHSMSwitchStatus = "[${enableHSMSwitch.currentValue("switch")}]"
    } else if (enableHSMSwitch?.latestValue("switch") != null) {state.enableHSMSwitchStatus = "[${enableHSMSwitch.latestValue("switch")}]"
    } else if (state?.enableHSMSwitchStatus == null) {(state.enableHSMSwitchStatus = " ")}
    
    // Device Handler Action
    if (((enableHSMToggle == true) || (enableHSMSwitch?.currentValue("switch") == "on")) && (hsmCommandsLock != null)) {sendLocationEvent(name: "hsmSetArm", value: "${hsmCommandsLock}")}
}

// Application Functions
def lockDoor(countLock) {
    ifTrace("lockDoor")
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {ifTrace("lockDoor: Application is paused or disabled.")
    } else if (getHSMLockOk() == false) {ifDebug("Unable to lock the door. HSM Status is ${location.HSMStatus}.")
    } else if (settings.whenToLock?.contains("6")) {ifDebug("Prevent locking under any circumstances is enabled.")
    } else if (lock1?.currentValue("lock") == "locked") {
        state.status = "(Locked)"
        unscheduleLockCommands()
        ifTrace("lockDoor: The door was locked successfully")
        if (((enableHSMToggle == true) || (enableHSMSwitch?.currentValue("switch") == "on")) && (hsmCommandsLock != null)) {sendLocationEvent(name: "hsmSetArm", value: "${hsmCommandsLock}")}
        if (maxRetriesLock != null) {atomicState.countLock = maxRetriesLock} else {(atomicState.countLock = 0)}
        updateLabel()
    } else {
        if (!settings.whenToUnlock?.contains("6") && settings.whenToUnlock?.contains("1") && ((contact?.currentValue("contact") == "open") || (contact?.currentValue("contact") == "opened")) && (lock1?.currentValue("lock") == "locked")) {
            ifTrace("lockDoor: Lock was locked while Door was open. Performing a fast unlock to prevent hitting the bolt against the frame.")
            unscheduleLockCommands()
            lock1Unlock()
            lock1.refresh()
        } else if (settings.whenToLock?.contains("1") && (contact?.currentValue("contact") == "open") && (lock1?.currentValue("lock") == "unlocked")) {
            ifTrace("lockDoor: Door is open. Waiting for door to close before locking.")
            unscheduleLockCommands()
        } else if ((contact?.currentValue("contact") == "closed") || (contact == null)) {
            if (state?.delayLock == 0) {ifDebug("Locking door now.")} else if (minSecLock == true) {ifDebug("Locking door in ${durationLock} seconds.")} else {ifDebug("Locking door in ${durationLock} minutes.")}
            configureDelayLock()
            if ((retryLock == false) && (lock1?.currentValue("lock") != "locked")) {
                runIn(state.delayLock, lock1Lock)
            } else if ((retryLock == true) && (atomicState.countLock > -1) && (atomicState.countLock != maxRetriesUnlock) && (lock1?.currentValue("lock") != "locked")) {
                i = (delayBetweenRetriesLock + state.delayLock)
                runIn(i, lock1Lock)
                runIn(i+i, lockDoor)
                ifTrace("Next retry in ${delayBetweenRetriesLock} seconds. Retries remaining: = ${atomicState.countLock}")
                atomicState.countLock = (atomicState.countLock - 1)
            } else if ((retryLock == true) && (atomicState.countLock == maxRetriesUnlock) && (lock1?.currentValue("lock") != "locked")) {
                runIn(state.delayLock, lock1Lock)
                atomicState.countLock = (atomicState.countLock - 1)
                i = (delayBetweenRetriesLock + state.delayLock)
                runIn(i, lockDoor)
            } else if ((retryLock == true) && (atomicState.countLock < 0) && (lock1?.currentValue("lock") != "locked")) {
                if (settings.eventNotifications?.contains("1")) {sendEventNotification("Maximum Lock Retries Exceeded.")}
                if (maxRetriesLock != null) {atomicState.countLock = maxRetriesLock} else {(atomicState.countLock = 0)}
            }
        } else {
            ifTrace("lockDoor: Unhandled exception")
        }
    }
    if (settings.failureNotifications?.contains("0") && (lock1.currentValue("lock") == "unknown")) {sendEventNotification("Lock is possibly jammed.")}
}

def lock1Lock() {
    ifTrace("lock1Lock")
    lock1.lock()
    ifDebug("Lock command sent")
    updateLabel()
}

def unlockDoor(countUnlock) {
    ifTrace("unlockDoor")
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {ifTrace("unlockDoor: Application is paused or disabled.")
    } else if (getHSMUnlockOk() == false) {ifDebug("Unable to unlock the door. HSM Status is ${location.HSMStatus}.")
    } else if (settings.whenToUnlock?.contains("6")) {ifDebug("Prevent unlocking under any circumstances is enabled.")
    } else if (lock1?.currentValue("lock") == "unlocked") {
        state.status = "(Unlocked)"
        unscheduleLockCommands()
        ifTrace("unlockDoor: The door was unlocked successfully")
        if (((enableHSMToggle == true) || (enableHSMSwitch?.currentValue("switch") == "on")) && (hsmCommandsUnlock != null)) {sendLocationEvent(name: "hsmSetArm", value: "${hsmCommandsUnlock}")}
        if (maxRetriesUnlock != null) {atomicState.countUnlock = maxRetriesUnlock} else {(atomicState.countUnlock = 0)}
        updateLabel()
    } else {
        ifTrace("state.delayUnlock = ${state.delayUnlock} durationUnlock = ${durationUnlock}")
        if (state.delayUnlock == 0) {ifDebug("Unlocking door now.")
        } else if (minSecUnlock) {ifDebug("Unlocking door in ${durationUnlock} seconds.")
        } else {ifDebug("Unlocking door in ${durationUnlock} minutes.")
        }
        configureDelayUnlock()
        if (retryUnlock == false) {
            runIn(state.delayUnlock, lock1Unlock)
        } else if ((retryUnlock == true) && (atomicState.countUnlock > -1) && (atomicState.countUnlock != maxRetriesUnlock) && (lock1?.currentValue("lock") != "unlocked")) {
            runIn(delayBetweenRetriesUnlock, lock1Unlock)
            atomicState.countUnlock = (atomicState.countUnlock - 1)
            ifTrace("Next retry in ${delayBetweenRetriesUnlock} seconds. Retries remaining: = ${atomicState.countUnlock}")
            runIn(delayBetweenRetriesUnlock, unlockDoor)
        } else if ((retryUnlock == true) && (atomicState.countUnlock == maxRetriesUnlock)) {
            runIn(state.delayUnlock, lock1Unlock)
            atomicState.countUnlock = (atomicState.countUnlock - 1)
            i = (delayBetweenRetriesLock + state.delayLock)
            runIn(i, unlockDoor)
        } else if ((retryUnlock == true) && (atomicState.countUnlock < 0) && (lock1?.currentValue("lock") != "unlocked")) {
            if (settings.eventNotifications?.contains("1")) {sendEventNotification("Maximum Unlock Retries Exceeded.")}
            unscheduleLockCommands()
            if (maxRetriesUnlock != null) {atomicState.countUnlock = maxRetriesUnlock} else {(atomicState.countUnlock = 0)}
        }
    }
    if (settings.failureNotifications?.contains("0") && (lock1.currentValue("lock") == "unknown")) {sendEventNotification("Lock is possibly jammed.")}
}

def lock1Unlock() {
    ifTrace("lock1Unlock")
    lock1.unlock()
    ifDebug("Unlock command sent")
    updateLabel()
}

def unscheduleLockCommands() {
    unschedule(unlockDoor)
    unschedule(lock1Unlock)
    unschedule(lockDoor)
    unschedule(lock1Lock)
}

def sendEventNotification(msg) {
    if ((notifyOnEvent == true) && eventNotificationDevices) {
        ifTrace("Sending Event Notification: ${msg}")
        eventNotificationDevices.deviceNotification(msg)
    }
}

def sendFailureNotification(msg) {
    if ((notifyOnFailure == true) && failureNotificationDevices) {
        ifTrace("Sending Failure Notification: ${msg}")
        failureNotificationDevices.deviceNotification(msg)
    }
}

//Label Updates
void updateLabel() {
    unschedule(updateLabel)
    runIn(1800, updateLabel)
    ifTrace("updateLabel")
//    getVariableInfo()
    if (getAllOk() == false) {
        if ((state?.paused == true) || (state?.disabled == true)) {state.pausedOrDisabled = true} else {state.pausedOrDisabled = false}
        state.status = "(Disabled by Time, Day, or Mode)"
        appStatus = "<span style=color:brown>(Disabled by Time, Day, or Mode)</span>"
    } else if (state?.disabled == true) {
        state.status = "(Disabled)"
        appStatus = "<span style=color:red>(Disabled)</span>"
    } else if (state?.paused == true) {
        state.status = "(Paused)"
        appStatus = "<span style=color:red>(Paused)</span>"
    } else if (lock1?.currentValue("lock") == "locked") {
        state.status = "(Locked)"
        appStatus = "<span style=color:green>(Locked)</span>"
    } else if (lock1?.currentValue("lock") == "unlocked") {
        state.status = "(Unlocked)"
        appStatus = "<span style=color:orange>(Unlocked)</span>"
    } else {
        initialize()
        state.pausedOrDisabled = false
        state.status = " "
        appStatus = "<span style=color:white> </span>"
    }
    if ((state?.paused == true) || (state?.disabled == true)) {state.pausedOrDisabled = true} else {state.pausedOrDisabled = false}
    app.updateLabel("${state.thisName} ${appStatus}")
}

//Enable, Resume, Pause button
def appButtonHandler(btn) {
    ifTrace("appButtonHandler")
    if (btn == "Lock") {
        lock1.lock()
        ifDebug("Lock command sent")
	runIn(5, updateLabel)
    } else if (btn == "Unlock") {
        lock1.unlock()
        ifDebug("Unlock command sent")
        runIn(5, updateLabel)
    } else if (btn == "Disabled by Switch") {
        state.disabled = false
        unsubscribe()
        unschedule()
        subscribe(disabledSwitch, "switch.on", disabledHandler)
        subscribe(disabledSwitch, "switch.off", disabledHandler)
        updateLabel()
    } else if (btn == "Resume") {
        state.disabled = false
        state.paused = !state.paused
        subscribe(disabledSwitch, "switch.on", disabledHandler)
        subscribe(disabledSwitch, "switch.off", disabledHandler)
        initialize()
    } else if (btn == "Pause") {
        state.paused = !state.paused
        if (state?.paused) {
            unschedule()
            unsubscribe()
            subscribe(disabledSwitch, "switch.on", disabledHandler)
            subscribe(disabledSwitch, "switch.off", disabledHandler)
            updated()
        } else {
            initialize()
            state.pausedOrDisabled = false
            if (lock1?.currentValue("lock") == "unlocked") {
                ifTrace("appButtonHandler: App was enabled or unpaused and lock was unlocked. Locking the door.")
                lockDoor(maxRetriesLock)
            }
        }
    }
    updateLabel()
}

def setPauseButtonName() {
    if (state?.disabled == true) {
        state.pauseButtonName = "Disabled by Switch"
        unsubscribe()
        unschedule()
		subscribe(disabledSwitch, "switch.on", disabledHandler)
        subscribe(disabledSwitch, "switch.off", disabledHandler)
        updateLabel()
    } else if (state?.paused == true) {
        state.pauseButtonName = "Resume"
        updated()
        unschedule()
		subscribe(disabledSwitch, "switch.on", disabledHandler)
        subscribe(disabledSwitch, "switch.off", disabledHandler)
    } else {
        state.pauseButtonName = "Pause"
        updated()
    }
}

// Application Page settings
def configureCountLock() {
    if (atomicState.countLock == null) {atomicState.countLock = 0}
}

def configureCountUnlock() {
    if (atomicState.countUnlock == null) {atomicState.countUnlock = 0}
}

def configureDelayLock() {
    if ((minSecLock != true) && (durationLock != null)) {state.delayLock = (durationLock * 60)} else {state.delayLock = durationLock}
}

def configureDelayUnlock() {
    if ((minSecUnlock != true) && (durationUnlock != null)) {state.delayUnlock = (durationUnlock * 60)} else {state.delayUnlock = durationUnlock}
}

def perModeLockDelay() {
    if (settings.whenToLock?.contains("7") && (enablePerModeLockDelay == true) && modesLockStatus) {
        modesLockStatus.each { it ->
            variableName = ("modeDurationLock"+"${it}")
            input "${variableName}", "number", title: "${it} mode lock delay:", required: false, defaultValue: 10, submitOnChange: true, range: "1..84600"
            if (it == location.mode) {
                log.debug "${variableName}"
                variableValue = app.getSetting("${variableName}")
                app.updateSetting("durationLock",[value: variableValue, type: "number"])}
        }
    }
}

def perModeUnlockDelay() {
    if (settings.whenToUnlock?.contains("7") && (enablePerModeUnlockDelay == true) && modesUnlockStatus) {
        modesUnlockStatus.each { it ->
            variableName = ("modeDurationUnlock"+"${it}")
            input "${variableName}", "number", title: "${it} mode unlock delay:", required: false, defaultValue: 10, submitOnChange: true, range: "1..84600"
            if (it == location.mode) {
                log.debug "${variableName}"
                variableValue = app.getSetting("${variableName}")
                app.updateSetting("durationUnlock",[value: variableValue, type: "number"])}
        }
    }
}

def sendHsmCommandsLock() {sendLocationEvent(name: "hsmSetArm", value: "${hsmCommandsLock.collect { it.keySet()[0] }}")}    //Send HSM Commands When Locking
def sendHsmCommandsUnlock() {sendLocationEvent(name: "hsmSetArm", value: "${hsmCommandsLock.collect { it.keySet()[0] }}")}    //Send HSM Commands When Unlocking
private hideLockOptionsSection() {(minSecLock || durationLock || retryLock || maxRetriesLock || delayBetweenRetriesLock) ? false : true}
private hideUnlockOptionsSection() {(minSecUnlock || durationUnlock || retryUnlock || maxRetriesUnlock || delayBetweenRetriesUnlock) ? false : true}
private hideHSMSection() {(whenToLockHSM || whenToUnlockHSM || isHSM || hsmLogLevel) ? false : true}
private hideNotificationSection() {(notifyOnLowBattery || lowBatteryNotificationDevices || lowBatteryDevicesToNotifyFor || lowBatteryAlertThreshold || notifyOnFailure || failureNotificationDevices || failureNotifications) ? false : true}
private hideLoggingSection() {(isInfo || isDebug || isTrace || ifLevel) ? false : true}
private hideOptionsSection() {(starting || ending || days || modes || manualCount) ? false : true}
def getAllOk() {if ((modeOk && daysOk && timeOk) == true) {return true} else {return false}}

private getHSMLockOk() {
    def result = (!whenToLockHSM || whenToUnlockHSM.contains(location.hsmStatus))
    result
}

private getHSMUnlockOk() {
    def result = (!whenToUnlockHSM || whenToUnlockHSM.contains(location.hsmStatus))
    result
}

private getModeOk() {
	def result = (!modes || modes.contains(location.mode))
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	result
}

private getTimeOk() {
	def result = true
	if ((starting != null) && (ending != null)) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	result
}

private hhmm(time, fmt = "h:mm a") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private timeIntervalLabel() {(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""}

def turnOffLoggingTogglesIn30() {
    ifTrace("turnOffLoggingTogglesIn30")
    if (!isInfo) {app.updateSetting("isInfo",[value:"false",type:"bool"])}
    if (!isDebug) {app.updateSetting("isDebug",[value:"false",type:"bool"])}
    if (!isTrace) {app.updateSetting("isTrace",[value:"false",type:"bool"])}
    if (isInfo == true) {runIn(1800, infoOff)}
    if (isDebug == true) {runIn(1800, debugOff)}
    if (isTrace == true) {runIn(1800, traceOff)}
}

def infoOff() {
    log.info "${state.thisName}: Info logging disabled."
    app.updateSetting("isInfo",[value:"false",type:"bool"])
}

def debugOff() {
    log.info "${state.thisName}: Debug logging disabled."
    app.updateSetting("isDebug",[value:"false",type:"bool"])
}

def traceOff() {
    log.trace "${state.thisName}: Trace logging disabled."
    app.updateSetting("isTrace",[value:"false",type:"bool"])
}

def hsmOff() {
    log.info "${state.thisName}: HSM logging disabled."
    app.updateSetting("isHSM",[value:"false",type:"bool"])
}

def disableInfoIn30() {
    if (isInfo == true) {
        runIn(1800, infoOff)
        log.info "Info logging disabling in 30 minutes."
    }
}

def disableDebugIn30() {
    if (isDebug == true) {
        runIn(1800, debugOff)
        log.debug "Debug logging disabling in 30 minutes."
    }
}

def disableTraceIn30() {
    if (isTrace == true) {
        runIn(1800, traceOff)
        log.trace "Trace logging disabling in 30 minutes."
    }
}

def disableHSMIn30() {
    if (isHSM == true) {
        runIn(1800, hsmOff)
        log.info "HSM logging disabling in 30 minutes."
    }
}

def ifWarn(msg) {log.warn "${state.thisName}: ${msg}"}

def ifInfo(msg) {
    if (!settings.ifLevel?.contains("1") && (isInfo != true)) {return}//bail
    else if (settings.ifLevel?.contains("1") || (isInfo == true)) {log.info "${state.thisName}: ${msg}"}
}

def ifDebug(msg) {
    if (!settings.ifLevel?.contains("2") && (isDebug != true)) {return}//bail
    else if (settings.ifLevel?.contains("2") || (isDebug == true)) {log.debug "${state.thisName}: ${msg}"}
}

def ifTrace(msg) {
    if (!settings.ifLevel?.contains("3") && (isTrace != true)) {return}//bail
    else if (settings.ifLevel?.contains("3") || (isTrace == true)) {log.trace "${state.thisName}: ${msg}"}
}

def displayFooter(){
	section() {
		paragraph "<div style='color:#1A77C9;text-align:center'>Auto Lock<br><a href='https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=3MPZ3GU5XL8RS&item_name=Hubitat+Development&currency_code=USD' target='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo'></a><br>Buy me a beer!</div>"
	}       
}
